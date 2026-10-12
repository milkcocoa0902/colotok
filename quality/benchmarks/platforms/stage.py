#!/usr/bin/env python3
"""Snapshot the actual library sources, changing only caller capture in the control."""
import hashlib
import json
from pathlib import Path
import subprocess
import argparse
import shutil

here = Path(__file__).resolve().parent
root = here.parents[2]
parser = argparse.ArgumentParser()
parser.add_argument("--caller-baseline-ref", help="Compare baseline ThreadWrapper sources against working-tree optimization")
args = parser.parse_args()
manifest = {"commit": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=root, text=True).strip(), "sources": {}}
manifest["mode"] = "caller-implementation" if args.caller_baseline_ref else "caller-omission"
manifest["baseline_ref"] = args.caller_baseline_ref
manifest["variants"] = {}
for variant in ("current", "noCaller"):
    hashes = {}
    project = here / "build/projects" / variant
    project.mkdir(parents=True, exist_ok=True)
    # Only this generated source tree is replaced; never touch the library source tree.
    if (project / "src").exists():
        shutil.rmtree(project / "src")
    template = (here / "variant.gradle.kts.template").read_text()
    if args.caller_baseline_ref:
        template = template.replace(
            "// Caller snapshot runtime tests are added by stage.py only for implementation comparisons.",
            'nativeTest { kotlin.srcDir("../../../src/nativeTest/kotlin") }'
        )
    (project / "build.gradle.kts").write_text(template)
    for source_set in ("commonMain", "androidMain", "jsMain", "nativeMain"):
        for source in (root / "colotok/src" / source_set).rglob("*.kt"):
            relative = source.relative_to(root / "colotok")
            target = project / relative
            target.parent.mkdir(parents=True, exist_ok=True)
            text = source.read_text()
            manifest["sources"][str(relative)] = hashlib.sha256(source.read_bytes()).hexdigest()
            if args.caller_baseline_ref and variant == "current" and source.name.startswith("ThreadWrapper."):
                text = subprocess.check_output(
                    ["git", "show", f"{args.caller_baseline_ref}:{source.relative_to(root)}"], cwd=root, text=True
                )
            if not args.caller_baseline_ref and variant == "noCaller" and source.name == "LogRecord.kt":
                old = "caller = ThreadWrapper.traceCallPoint(),"
                assert text.count(old) == 1
                text = text.replace(old, 'caller = "",')
            target.write_text(text)
            hashes[str(relative)] = hashlib.sha256(text.encode()).hexdigest()
    original = project / "src/commonMain/kotlin/com/milkcocoa/info/colotok/core/logger/LogRecord.kt"
    assert ('caller = "",' in original.read_text()) == (variant == "noCaller" and not args.caller_baseline_ref)
    manifest["variants"][variant] = hashes
    if args.caller_baseline_ref:
        test = root / "colotok/src/commonTest/kotlin/com/milkcocoa/info/colotok/util/CallerFrameUtilsTest.kt"
        target = project / test.relative_to(root / "colotok")
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(test.read_text())
(here / "build/source-manifest.json").write_text(json.dumps(manifest, indent=2) + "\n")
print("Snapshots ready:", manifest["mode"], "baseline:", args.caller_baseline_ref)
