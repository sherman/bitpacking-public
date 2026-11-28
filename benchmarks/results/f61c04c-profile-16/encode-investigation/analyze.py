"""Verify the fixed intervention schedule and summarize effects from saved JMH records."""

import argparse
from collections import defaultdict
import gzip
import hashlib
import json
from pathlib import Path
from statistics import mean

from collect import DATA, HERE, METHOD, TREATMENT, make_plan
from harness.analysis import metric_score, profile_summary
from harness.tests.counter_oracle import verify_profile


def analyze(directory):
    plan = json.loads((directory / "plan.json").read_text())
    groups = defaultdict(list)
    assert plan["primary_metric"] == "instructions"
    assert plan["plan"] == make_plan() and plan["treatment"] == TREATMENT
    for build in plan["builds"].values():
        assert hashlib.sha256((DATA / build["jar"]).read_bytes()).hexdigest() == build["sha256"]
    with (directory / "forks.jsonl").open() as raw, gzip.open(directory / "console.txt.gz", "rt") as log:
        rows = map(json.loads, raw)
        headers = (line.strip() for line in log if line.startswith("===== block "))
        for sequence, entry in enumerate(plan["plan"], 1):
            build = entry["build"]
            assert entry["sequence"] == sequence and entry["order"] in ("ABBA", "BAAB")
            assert entry["block"] == len(groups[build]) + 1
            group = []
            for slot, label in enumerate(entry["order"], 1):
                row = next(rows)
                assert all(row[key] == value for key, value in entry.items())
                assert row["slot"] == slot and row["label"] == label
                assert next(headers) == f"===== block {sequence} slot {slot} {row['start']} ====="
                assert row["before"] == row["after"] == "performance"
                assert row["command"][:3] == ["taskset", "-c", str(plan["cpu"])]
                jar = row["command"][row["command"].index("-jar") + 1]
                assert Path(jar).name == Path(plan["builds"][build]["jar"]).name
                jmh = row["jmh"]
                assert jmh["benchmark"] == METHOD and jmh["params"] == {"bits": "16", "mode": "0", "blocks": "16"}
                assert jmh["forks"] == jmh["threads"] == 1
                assert (jmh["warmupIterations"], jmh["warmupTime"]) == (2, "500 ms")
                assert (jmh["measurementIterations"], jmh["measurementTime"]) == (3, "250 ms")
                setting = "default" if label == "A" else plan["treatment"][build]
                assert row["setting"] == setting
                directives = [Path(arg.split("=", 1)[1]).name for arg in jmh["jvmArgs"]
                              if arg.startswith("-XX:CompilerDirectivesFile=")]
                assert directives == ([] if label == "A" else [setting + ".json"])
                group.append(row)
            groups[build].append(group)
        assert next(rows, None) is None and next(headers, None) is None

    summaries = {}
    for build in ("A", "B"):
        blocks = groups[build]
        assert len(blocks) == 8
        summary = profile_summary(blocks)
        verify_profile(blocks, summary)
        summary["mean_instructions_per_op"] = {
            setting: mean(metric_score(row, "instructions") for block in blocks for row in block
                          if row["setting"] == setting)
            for setting in ("default", plan["treatment"][build])
        }
        summaries[build] = summary
    return summaries


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("data", type=Path, default=HERE / "data", nargs="?")
    print(json.dumps(analyze(parser.parse_args().data), indent=2))
