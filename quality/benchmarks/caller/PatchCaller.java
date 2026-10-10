package quality.benchmarks.caller;

import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.nio.file.Files;
import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;

/** Experimental control: change only capture's traceCallPoint() result to "". */
public final class PatchCaller {
    public static void main(String[] args) throws Exception {
        int[] replacements = {0};
        try (JarFile source = new JarFile(args[0]);
             JarOutputStream target = new JarOutputStream(Files.newOutputStream(Path.of(args[1])))) {
            var entries = source.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                byte[] bytes = source.getInputStream(entry).readAllBytes();
                if (entry.getName().equals("com/milkcocoa/info/colotok/core/logger/LogEventMetadata$Companion.class")) {
                    ClassReader reader = new ClassReader(bytes);
                    ClassWriter writer = new ClassWriter(0);
                    reader.accept(new ClassVisitor(Opcodes.ASM6, writer) {
                        @Override
                        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                         String signature, String[] exceptions) {
                            MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
                            if (!name.equals("capture")) return delegate;
                            return new MethodVisitor(Opcodes.ASM6, delegate) {
                                @Override
                                public void visitMethodInsn(int opcode, String owner, String name,
                                                            String descriptor, boolean isInterface) {
                                    if (opcode == Opcodes.INVOKEVIRTUAL &&
                                        owner.equals("com/milkcocoa/info/colotok/util/ThreadWrapper") &&
                                        name.equals("traceCallPoint") && descriptor.equals("()Ljava/lang/String;")) {
                                        super.visitInsn(Opcodes.POP); // Discard the singleton receiver.
                                        super.visitLdcInsn("");
                                        replacements[0]++;
                                    } else {
                                        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                                    }
                                }
                            };
                        }
                    }, 0);
                    bytes = writer.toByteArray();
                }
                target.putNextEntry(new JarEntry(entry.getName()));
                target.write(bytes);
                target.closeEntry();
            }
        }
        if (replacements[0] != 1) throw new IllegalStateException("Expected one caller capture, got " + replacements[0]);
        System.out.println("Verified: exactly one traceCallPoint invocation replaced in capture().");
    }
}
