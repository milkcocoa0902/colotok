#!/usr/bin/env python3
"""Compare public JVM stack acquisition APIs in unpublished library adapters."""
import argparse
import csv
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import platform
from statistics import mean
import subprocess
import zipfile

VARIANTS = ["current", "thread-array", "throwable-array", "walker-stream", "walker-iterator", "walker-classvalue"]
HERE = Path(__file__).resolve().parent


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--java-home", type=Path, required=True)
    parser.add_argument("--label", default="jvm-strategies-2026-10-11")
    parser.add_argument("--forks", type=int, default=3)
    parser.add_argument("--warmup", type=int, default=3)
    parser.add_argument("--iterations", type=int, default=5)
    parser.add_argument("--seconds", type=int, default=1)
    parser.add_argument("--validate-only", action="store_true")
    args = parser.parse_args()
    build = HERE / "build"
    classes = build / "strategy-classes"
    classes.mkdir(parents=True, exist_ok=True)
    results = HERE / "results" / args.label
    results.mkdir(parents=True, exist_ok=True)
    core = Path((build / "core-jar.txt").read_text())
    tools = (build / "tools-classpath.txt").read_text()
    runtime = (build / "runtime-classpath.txt").read_text()
    java, javac = [str(args.java_home / "bin" / executable) for executable in ("java", "javac")]
    export = "java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED"
    subprocess.run([javac, "--add-exports", export, "-d", str(classes), str(HERE / "PatchCallerStrategy.java")], check=True)
    cp = os.pathsep.join([str(classes), str(core), runtime, tools])
    source_paths = [HERE / name for name in ("JvmCallerStrategies.java", "CallerStrategiesFixture.java", "CallerBenchmark.java")]
    source_paths += list((HERE / "fixtures").rglob("*.java"))
    subprocess.run([javac, "-cp", cp, "-processorpath", tools, "-d", str(classes), *map(str, source_paths)], check=True)
    adapter = build / "colotok-strategy-adapter.jar"
    subprocess.run([java, "--add-exports", export, "-cp", str(classes),
                    "quality.benchmarks.caller.PatchCallerStrategy", str(core), str(adapter)], check=True)
    with zipfile.ZipFile(core) as original, zipfile.ZipFile(adapter) as patched:
        assert original.namelist() == patched.namelist()
        changed = [name for name in original.namelist() if original.read(name) != patched.read(name)]
        assert changed == ["com/milkcocoa/info/colotok/util/ThreadWrapper.class"], changed
    metadata = {
        "started_at_utc": datetime.now(timezone.utc).isoformat(),
        "commit": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=HERE.parents[2], text=True).strip(),
        "platform": platform.platform(),
        "java": subprocess.run([java, "-version"], capture_output=True, text=True, check=True).stderr,
        "parameters": vars(args) | {"java_home": str(args.java_home)},
        "jar_sha256": {"current": hashlib.sha256(core.read_bytes()).hexdigest(),
                       "adapter": hashlib.sha256(adapter.read_bytes()).hexdigest()},
        "source_sha256": {str(p.relative_to(HERE)): hashlib.sha256(p.read_bytes()).hexdigest() for p in source_paths},
        "changed_entries": changed,
        "fixture_results": {},
        "commands": [],
    }
    baseline = None
    for variant in VARIANTS:
        jar = core if variant == "current" else adapter
        variant_cp = os.pathsep.join([str(classes), str(jar), runtime, tools])
        command = [java, f"-Dcaller.strategy={variant}", "-cp", variant_cp,
                   "quality.benchmarks.caller.CallerStrategiesFixture"]
        output = subprocess.run(command, capture_output=True, text=True, check=True)
        (results / f"validation-{variant}.txt").write_text(output.stdout + output.stderr)
        fixtures = dict(line.split("\t", 1) for line in output.stdout.splitlines())
        assert len(fixtures) == 6, fixtures
        if baseline is None: baseline = fixtures
        assert fixtures == baseline, {"variant": variant, "current": baseline, "actual": fixtures}
        metadata["fixture_results"][variant] = fixtures
    (results / "environment.json").write_text(json.dumps(metadata, indent=2) + "\n")
    print("Verified caller equivalence: direct/deep/reflection/MethodHandle/SLF4J bridge/worker, all six strategies.", flush=True)
    if args.validate_only: return
    for fork in range(1, args.forks + 1):
        order = VARIANTS if fork == 1 else list(reversed(VARIANTS)) if fork == 2 else VARIANTS[2:] + VARIANTS[:2]
        for variant in order:
            jar = core if variant == "current" else adapter
            variant_cp = os.pathsep.join([str(classes), str(jar), runtime, tools])
            command = [java, "-cp", variant_cp, "org.openjdk.jmh.Main", "CallerBenchmark.plainRecord$",
                       "-f", "1", "-wi", str(args.warmup), "-i", str(args.iterations),
                       "-w", f"{args.seconds}s", "-r", f"{args.seconds}s", "-prof", "gc",
                       "-jvmArgsAppend", f"-Xms512m -Xmx512m -XX:+UseG1GC -Dcaller.omitted=false -Dcaller.strategy={variant}",
                       "-rf", "json", "-rff", str(results / f"{variant}-{fork}.json"), "-foe", "true"]
            metadata["commands"].append(command)
            (results / "environment.json").write_text(json.dumps(metadata, indent=2) + "\n")
            print(f"{variant}, process {fork}/{args.forks}", flush=True)
            with (results / f"{variant}-{fork}.txt").open("w") as output:
                subprocess.run(command, stdout=output, stderr=subprocess.STDOUT, check=True)
    rows = []
    for variant in VARIANTS:
        for depth in ("0", "16"):
            forks = [next(r for r in json.loads((results / f"{variant}-{i}.json").read_text())
                          if r["params"]["depth"] == depth) for i in range(1, args.forks + 1)]
            assert all(r["primaryMetric"]["scoreUnit"] == "ns/op" for r in forks)
            samples = [sample for r in forks for run in r["primaryMetric"]["rawData"] for sample in run]
            assert len(samples) == args.forks * args.iterations
            ns = mean(samples)
            rows.append(dict(variant=variant, extra_frames=depth, ns_per_record=ns, equivalent_records_per_s=1e9/ns,
                             bytes_per_record=mean(r["secondaryMetrics"]["gc.alloc.rate.norm"]["score"] for r in forks),
                             measurements=len(samples), forks=args.forks,
                             fork_mean_min_ns=min(r["primaryMetric"]["score"] for r in forks),
                             fork_mean_max_ns=max(r["primaryMetric"]["score"] for r in forks)))
    with (results / "summary.csv").open("w") as output:
        writer = csv.DictWriter(output, fieldnames=rows[0].keys(), lineterminator="\n")
        writer.writeheader()
        writer.writerows(rows)
    print(json.dumps(rows, indent=2))


if __name__ == "__main__":
    main()
