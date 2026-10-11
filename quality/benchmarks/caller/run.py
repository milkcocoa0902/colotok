#!/usr/bin/env python3
"""Run paired JMH JVMs. First run the Gradle preparation command in README.md."""
import argparse
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import platform
import subprocess
import zipfile


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--java-home", required=True, type=Path)
    parser.add_argument("--forks", type=int, default=3)
    parser.add_argument("--warmup", type=int, default=3)
    parser.add_argument("--iterations", type=int, default=5)
    parser.add_argument("--seconds", type=int, default=1)
    parser.add_argument("--label", default="jdk11")
    args = parser.parse_args()
    here = Path(__file__).resolve().parent
    root = here.parents[2]
    build = here / "build"
    classes = build / "classes"
    classes.mkdir(parents=True, exist_ok=True)
    results = here / "results" / args.label
    results.mkdir(parents=True, exist_ok=True)
    java = str(args.java_home / "bin/java")
    javac = str(args.java_home / "bin/javac")
    core = Path((build / "core-jar.txt").read_text())
    tools = (build / "tools-classpath.txt").read_text()
    runtime = (build / "runtime-classpath.txt").read_text()
    export = "java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED"
    subprocess.run([javac, "--add-exports", export, "-d", str(classes), str(here / "PatchCaller.java")], check=True)
    omitted = build / "colotok-no-caller.jar"
    subprocess.run([java, "--add-exports", export, "-cp", str(classes),
                    "quality.benchmarks.caller.PatchCaller", str(core), str(omitted)], check=True)
    with zipfile.ZipFile(core) as original, zipfile.ZipFile(omitted) as patched:
        assert original.namelist() == patched.namelist()
        changed = [name for name in original.namelist() if original.read(name) != patched.read(name)]
        assert changed == ["com/milkcocoa/info/colotok/core/logger/LogEventMetadata$Companion.class"], changed
    classpath = os.pathsep.join([str(classes), str(core), runtime, tools])
    subprocess.run([javac, "-cp", classpath, "-processorpath", tools, "-d", str(classes),
                    str(here / "CallerBenchmark.java")], check=True)
    metadata = {
        "started_at_utc": datetime.now(timezone.utc).isoformat(),
        "commit": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=root, text=True).strip(),
        "java": subprocess.run([java, "-version"], capture_output=True, text=True, check=True).stderr,
        "platform": platform.platform(),
        "cpu": subprocess.check_output(["lscpu"], text=True),
        "core_sha256": hashlib.sha256(core.read_bytes()).hexdigest(),
        "omitted_sha256": hashlib.sha256(omitted.read_bytes()).hexdigest(),
        "changed_entries": changed,
        "parameters": vars(args) | {"java_home": str(args.java_home)},
        "commands": [],
    }
    for fork in range(1, args.forks + 1):
        variants = ["current", "no-caller"] if fork % 2 else ["no-caller", "current"]
        for variant in variants:
            jar = core if variant == "current" else omitted
            cp = os.pathsep.join([str(classes), str(jar), runtime, tools])
            command = [java, "-cp", cp, "org.openjdk.jmh.Main", "CallerBenchmark", "-f", "1",
                       "-wi", str(args.warmup), "-i", str(args.iterations),
                       "-w", f"{args.seconds}s", "-r", f"{args.seconds}s", "-prof", "gc",
                       "-jvmArgsAppend", "-Xms512m -Xmx512m -XX:+UseG1GC "
                       f"-Dcaller.omitted={str(variant == 'no-caller').lower()}",
                       "-rf", "json", "-rff", str(results / f"{variant}-{fork}.json"), "-foe", "true"]
            metadata["commands"].append(command)
            (results / "environment.json").write_text(json.dumps(metadata, indent=2) + "\n")
            print(f"Running {variant}, fork {fork}/{args.forks}", flush=True)
            with (results / f"{variant}-{fork}.txt").open("w") as output:
                subprocess.run(command, stdout=output, stderr=subprocess.STDOUT, check=True)
    print(f"Results: {results}", flush=True)


if __name__ == "__main__":
    main()
