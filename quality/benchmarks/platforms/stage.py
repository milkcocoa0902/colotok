#!/usr/bin/env python3
"""Snapshot the actual library sources, changing only caller capture in the control."""
import hashlib
import json
from pathlib import Path
import subprocess

here = Path(__file__).resolve().parent
root = here.parents[2]
manifest = {"commit": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=root, text=True).strip(), "sources": {}}
for variant in ("current", "noCaller"):
    project = here / "build/projects" / variant
    project.mkdir(parents=True, exist_ok=True)
    (project / "build.gradle.kts").write_text((here / "variant.gradle.kts.template").read_text())
    for source_set in ("commonMain", "androidMain", "jsMain", "nativeMain"):
        for source in (root / "colotok/src" / source_set).rglob("*.kt"):
            relative = source.relative_to(root / "colotok")
            target = project / relative
            target.parent.mkdir(parents=True, exist_ok=True)
            text = source.read_text()
            manifest["sources"][str(relative)] = hashlib.sha256(source.read_bytes()).hexdigest()
            if variant == "noCaller" and source.name == "LogRecord.kt":
                old = "caller = ThreadWrapper.traceCallPoint(),"
                assert text.count(old) == 1
                text = text.replace(old, 'caller = "",')
            target.write_text(text)
    original = project / "src/commonMain/kotlin/com/milkcocoa/info/colotok/core/logger/LogRecord.kt"
    assert ('caller = "",' in original.read_text()) == (variant == "noCaller")
(here / "build/source-manifest.json").write_text(json.dumps(manifest, indent=2) + "\n")
print("Snapshots ready: current and noCaller; only capture's caller expression differs.")
