package quality.benchmarks.caller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;

/** Redirects only ThreadWrapper.traceCallPoint() in an unpublished comparison jar. */
public final class PatchCallerStrategy {
    public static void main(String[] args) throws Exception {
        int[] replacements = {0};
        try (JarFile source = new JarFile(args[0]);
             JarOutputStream target = new JarOutputStream(Files.newOutputStream(Path.of(args[1])))) {
            var entries = source.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                byte[] bytes = source.getInputStream(entry).readAllBytes();
                if (entry.getName().equals("com/milkcocoa/info/colotok/util/ThreadWrapper.class")) {
                    ClassReader reader = new ClassReader(bytes);
                    ClassWriter writer = new ClassWriter(0);
                    reader.accept(new ClassVisitor(Opcodes.ASM6, writer) {
                        @Override
                        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                         String signature, String[] exceptions) {
                            MethodVisitor method = super.visitMethod(access, name, descriptor, signature, exceptions);
                            if (!name.equals("traceCallPoint") || !descriptor.equals("()Ljava/lang/String;")) return method;
                            method.visitCode();
                            method.visitMethodInsn(Opcodes.INVOKESTATIC, "quality/benchmarks/caller/JvmCallerStrategies",
                                "capture", "()Ljava/lang/String;", false);
                            method.visitInsn(Opcodes.ARETURN);
                            method.visitMaxs(1, 1);
                            method.visitEnd();
                            replacements[0]++;
                            return null;
                        }
                    }, 0);
                    bytes = writer.toByteArray();
                }
                target.putNextEntry(new JarEntry(entry.getName()));
                target.write(bytes);
                target.closeEntry();
            }
        }
        if (replacements[0] != 1) throw new IllegalStateException("Expected one trace method, got " + replacements[0]);
    }
}
