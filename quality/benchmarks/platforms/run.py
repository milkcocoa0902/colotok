#!/usr/bin/env python3
"""Run standalone, paired benchmarks. Android requires an already connected device."""
import argparse
import csv
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import platform
import shlex
import shutil
from statistics import mean
import subprocess

HERE = Path(__file__).resolve().parent


def prepare_android(sdk, build_tools, d8_jar):
    output = HERE / "build/dex"
    output.mkdir(parents=True, exist_ok=True)
    java = shutil.which("java")
    for variant in ("current", "noCaller"):
        project = HERE / "build/projects" / variant
        jars = list((project / "build/libs").glob("*-jvm.jar"))
        assert len(jars) == 1, jars
        dependencies = (project / "build/android-runtime.txt").read_text().split(os.pathsep)
        jar = d8_jar or sdk / f"build-tools/{build_tools}/lib/d8.jar"
        command = [java, "-cp", str(jar), "com.android.tools.r8.D8",
                   "--release", "--min-api", "26", "--lib", str(sdk / "platforms/android-34/android.jar"),
                   "--output", str(output / f"{variant}.zip"), str(jars[0]), *dependencies]
        with (output / f"{variant}-d8.txt").open("w") as log:
            subprocess.run(command, stdout=log, stderr=subprocess.STDOUT, check=True)
        log_text = (output / f"{variant}-d8.txt").read_text()
        if "malformed kotlin.Metadata" in log_text or "Unexpected error" in log_text:
            raise RuntimeError("Incompatible D8; use --d8-jar with a compatible version. See the D8 log.")
        print(f"D8 log: {output / (variant + '-d8.txt')}", flush=True)


def summarize(directory, forks, case_count):
    groups = {}
    for file in sorted(directory.glob("*-*.jsonl")):
        for line in file.read_text().splitlines():
            if not line.startswith('{"'): continue
            row = json.loads(line)
            if row["type"] != "measurement": continue
            groups.setdefault((row["variant"], row["case"]), {}).setdefault(file.name, []).append(row)
    assert len(groups) == case_count * 2, (len(groups), case_count)
    summary = []
    for (variant, case), processes in sorted(groups.items()):
        assert len(processes) == forks, (variant, case, len(processes))
        assert all(len(samples) == 5 for samples in processes.values())
        samples = [r for rows in processes.values() for r in rows]
        fork_means = [mean(r["nsPerUnit"] for r in rows) for rows in processes.values()]
        ns = mean(r["nsPerUnit"] for r in samples)
        alloc = [r["allocatedBytesPerUnit"] for r in samples if r["allocatedBytesPerUnit"] >= 0]
        summary.append(dict(variant=variant, case=case, ns_per_unit=ns, equivalent_units_per_s=1e9/ns,
                            bytes_per_unit=mean(alloc) if alloc else "unavailable", forks=forks,
                            samples=len(samples), fork_mean_min_ns=min(fork_means), fork_mean_max_ns=max(fork_means)))
    with (directory / "summary.csv").open("w") as output:
        writer = csv.DictWriter(output, fieldnames=summary[0].keys())
        writer.writeheader()
        writer.writerows(summary)
    print(json.dumps(summary, indent=2))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--platform", choices=["android", "js", "linuxX64"], required=True)
    parser.add_argument("--sdk", type=Path, default=Path(os.environ.get("ANDROID_HOME", str(Path.home() / "Android/Sdk"))))
    parser.add_argument("--serial", default="emulator-5554")
    parser.add_argument("--build-tools", default="36.1.0")
    parser.add_argument("--d8-jar", type=Path)
    parser.add_argument("--forks", type=int, default=3)
    parser.add_argument("--window-ms", type=int, default=400)
    parser.add_argument("--case", default="all")
    parser.add_argument("--label", default="reference")
    parser.add_argument("--prepare-android", action="store_true")
    args = parser.parse_args()
    if args.prepare_android:
        prepare_android(args.sdk, args.build_tools, args.d8_jar)
    directory = HERE / "results" / args.label / args.platform
    directory.mkdir(parents=True, exist_ok=True)
    metadata = {
        "started_at_utc": datetime.now(timezone.utc).isoformat(),
        "host": platform.platform(),
        "source_manifest": json.loads((HERE / "build/source-manifest.json").read_text()),
        "parameters": vars(args) | {"sdk": str(args.sdk), "d8_jar": str(args.d8_jar) if args.d8_jar else None},
        "commands": [],
        "artifacts": {},
    }
    artifacts = {}
    if args.platform == "android":
        adb = str(args.sdk / "platform-tools/adb")
        metadata["android_properties"] = subprocess.check_output([adb, "-s", args.serial, "shell", "getprop"], text=True)
        subprocess.run([adb, "-s", args.serial, "shell", "mkdir", "-p", "/data/local/tmp/colotok-benchmark"], check=True)
        for variant in ("current", "noCaller"):
            artifact = HERE / f"build/dex/{variant}.zip"
            artifacts[variant] = artifact
            subprocess.run([adb, "-s", args.serial, "push", str(artifact), f"/data/local/tmp/colotok-benchmark/{variant}.zip"], check=True)
    elif args.platform == "linuxX64":
        for variant in ("current", "noCaller"):
            binaries = list((HERE / f"build/projects/{variant}/build/bin/linuxX64/releaseExecutable").glob("*.kexe"))
            assert len(binaries) == 1, binaries
            artifacts[variant] = binaries[0]
    else:
        metadata["node"] = subprocess.check_output(["node", "--version"], text=True).strip()
        for variant in ("current", "noCaller"):
            candidates = list((HERE / f"build/projects/{variant}/build/compileSync/js/main/productionExecutable/kotlin").glob(f"*{variant}.js"))
            assert len(candidates) == 1, candidates
            artifacts[variant] = candidates[0]
    for variant, artifact in artifacts.items():
        metadata["artifacts"][variant] = {"path": str(artifact), "sha256": hashlib.sha256(artifact.read_bytes()).hexdigest()}
    for fork in range(1, args.forks + 1):
        for variant in (["current", "noCaller"] if fork % 2 else ["noCaller", "current"]):
            if args.platform == "android":
                remote = f"CLASSPATH=/data/local/tmp/colotok-benchmark/{variant}.zip app_process -Xms64m -Xmx256m /system/bin quality.benchmarks.platforms.BenchKt {shlex.quote(variant)} {shlex.quote(args.case)} {args.window_ms} 1>/dev/null"
                command = [adb, "-s", args.serial, "shell", "-T", remote]
            elif args.platform == "js":
                command = ["node", str(artifacts[variant]), variant, args.case, str(args.window_ms)]
            else:
                command = [str(artifacts[variant]), variant, args.case, str(args.window_ms)]
            metadata["commands"].append(command)
            (directory / "environment.json").write_text(json.dumps(metadata, indent=2) + "\n")
            print(f"{args.platform}: {variant}, process {fork}/{args.forks}", flush=True)
            # stdout is an actual /dev/null file, not a pipe; stderr contains JSONL results.
            with (directory / f"{variant}-{fork}.jsonl").open("w") as output:
                subprocess.run(command, stdout=subprocess.DEVNULL, stderr=output, check=True)
            lines = (directory / f"{variant}-{fork}.jsonl").read_text().splitlines()
            environment = [json.loads(line) for line in lines if line.startswith('{"') and json.loads(line).get("type") == "environment"]
            assert len(environment) == 1 and environment[0]["variant"] == variant, lines[:10]
    summarize(directory, args.forks, 9 if args.case == "all" else 1)


if __name__ == "__main__":
    main()
