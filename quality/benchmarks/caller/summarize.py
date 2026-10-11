#!/usr/bin/env python3
"""Aggregate equal-length forks; report the range of fork means, not a fake CI."""
import argparse
import csv
import json
from pathlib import Path
from statistics import mean


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("results", type=Path)
    parser.add_argument("--expected-forks", type=int, default=3)
    args = parser.parse_args()
    groups = {}
    for variant in ("current", "no-caller"):
        for file in sorted(args.results.glob(f"{variant}-*.json")):
            for result in json.loads(file.read_text()):
                assert result["primaryMetric"]["scoreUnit"] == "ns/op"
                assert result["secondaryMetrics"]["gc.alloc.rate.norm"]["scoreUnit"] == "B/op"
                key = (result["benchmark"].split(".")[-1], int(result["params"]["depth"]), variant)
                groups.setdefault(key, []).append(result)
    rows = []
    for key, forks in sorted(groups.items()):
        if len(forks) != args.expected_forks:
            raise SystemExit(f"Incomplete results for {key}: {len(forks)} forks")
        samples = [sample for fork in forks for run in fork["primaryMetric"]["rawData"] for sample in run]
        fork_means = [fork["primaryMetric"]["score"] for fork in forks]
        ns = mean(samples)
        rows.append(dict(benchmark=key[0], extra_frames=key[1], variant=key[2], forks=len(forks),
                         measurements=len(samples), ns_per_op=ns, equivalent_ops_per_s=1e9 / ns,
                         bytes_per_op=mean(f["secondaryMetrics"]["gc.alloc.rate.norm"]["score"] for f in forks),
                         fork_mean_min_ns=min(fork_means), fork_mean_max_ns=max(fork_means)))
    if not rows:
        raise SystemExit("No JMH results found")
    if len(rows) != 12:
        raise SystemExit(f"Expected 12 case/variant combinations, got {len(rows)}")
    with (args.results / "summary.csv").open("w") as output:
        writer = csv.DictWriter(output, fieldnames=rows[0].keys())
        writer.writeheader()
        writer.writerows(rows)
    print("| Case | Extra frames | Variant | ns/op | Equivalent ops/s | B/op | Fork mean range (ns) |")
    print("|---|---:|---|---:|---:|---:|---:|")
    for row in rows:
        print(f"| {row['benchmark']} | {row['extra_frames']} | {row['variant']} | "
              f"{row['ns_per_op']:.1f} | {row['equivalent_ops_per_s']:,.0f} | "
              f"{row['bytes_per_op']:.1f} | {row['fork_mean_min_ns']:.1f}–{row['fork_mean_max_ns']:.1f} |")


if __name__ == "__main__":
    main()
