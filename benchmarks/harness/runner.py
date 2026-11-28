"""Schedule randomized four-fork blocks, run JMH, and freeze the A/A forecast."""

from collections import defaultdict
from contextlib import contextmanager
from dataclasses import dataclass
from datetime import datetime, timezone
import gzip
import itertools
import json
import math
from pathlib import Path
import random
import re
import shutil
import tempfile
import time

from .analysis import COUNTERS, PROFILE_SHIFTS, log_effect, minimum_detectable_effects, profile_sensitivity
from .cpu_policy import run_checked_fork


def utc():
    return datetime.now(timezone.utc).isoformat()


def make_schedule(cases, *, seed, calibration_blocks, timing_blocks, profile_blocks=8, profile_calibration_blocks=0):
    """Shuffle cases per round and choose ABBA or BAAB independently per block."""
    cases = list(cases)
    if not cases or calibration_blocks < 1 or timing_blocks < 1 or profile_blocks < 0:
        raise ValueError("Expected cases, positive calibration and timing budgets, and a nonnegative profile budget")
    if profile_calibration_blocks < 0 or (profile_calibration_blocks and not 1 <= profile_blocks <= 20):
        raise ValueError("Profile calibration needs a nonnegative calibration count and an exact final budget of 1–20 blocks")
    rng = random.Random(seed)
    plan = []
    for stage, count in (("calibration", calibration_blocks), ("timing", timing_blocks),
                         ("profile_aa", profile_calibration_blocks), ("profile", profile_blocks)):
        for block in range(1, count + 1):
            order = cases.copy()
            rng.shuffle(order)
            for case in order:
                plan.append(dict(sequence=len(plan) + 1, stage=stage, block=block, **case,
                                 order=rng.choice(("ABBA", "BAAB"))))
    return plan


@dataclass(frozen=True)
class ForkSettings:
    cpu: int
    java: str
    jvm_args: tuple[str, ...]
    warmup_iterations: int
    warmup_time: str
    measurement_iterations: int
    measurement_time: str


def measure_fork(settings, jar, benchmark, params, profiled, result_path, console, start):
    cmd = ["taskset", "-c", str(settings.cpu), settings.java, *settings.jvm_args, "-jar", str(jar),
           "^" + re.escape(benchmark) + "$"]
    for name, value in params.items():
        cmd.extend(["-p", f"{name}={value}"])
    cmd.extend(["-t", "1", "-f", "1", "-wi", str(settings.warmup_iterations), "-w", settings.warmup_time,
                "-i", str(settings.measurement_iterations), "-r", settings.measurement_time,
                "-foe", "true", "-rf", "json", "-rff", str(result_path)])
    if profiled:
        cmd.extend(["-prof", "perfnorm:events=cycles,instructions", "-prof", "gc"])
    tick = time.monotonic()
    result_path.unlink(missing_ok=True)
    before, after = run_checked_fork(cmd, console, settings.cpu)
    elapsed = time.monotonic() - tick
    row, = json.loads(result_path.read_text())
    primary = row["primaryMetric"]
    if (row["benchmark"] != benchmark or row["forks"] != 1 or row["threads"] != 1 or row.get("params", {}) != params
            or primary["scoreUnit"] != "ns/op" or len(primary["rawData"]) != 1
            or len(primary["rawData"][0]) != settings.measurement_iterations
            or not math.isfinite(primary["score"]) or primary["score"] <= 0):
        raise ValueError(f"Unexpected JMH result for {benchmark} {params}")
    if profiled:
        for key in COUNTERS:
            metric = row["secondaryMetrics"][key]
            valid_unit = "B/op" if key == "gc.alloc.rate.norm" else "#/op"
            score = metric["score"]
            if (metric["scoreUnit"] != valid_unit or not math.isfinite(score)
                    or score < 0 or (score == 0 and valid_unit == "#/op")):
                raise ValueError(f"Invalid {key} result for {benchmark} {params}")
    elif row["secondaryMetrics"]:
        raise ValueError(f"Unexpected secondary metrics for {benchmark} {params}")
    return dict(start=start, end=utc(), seconds=elapsed, before=before["scaling_governor"],
                after=after["scaling_governor"], command=cmd, jmh=row)


@contextmanager
def fork_recorder(output):
    """Stream completed forks and console output, including a rejected fork's console."""
    with (output / "forks.jsonl").open("x", buffering=1) as raw, gzip.open(output / "console.txt.gz", "xt") as logs, \
            tempfile.TemporaryDirectory(prefix="jmh-fork-") as tmp:
        result_path = Path(tmp) / "jmh.json"

        def record_block(entry, settings, forks, *, profiled):
            rows = []
            for slot, (label, spec) in enumerate(zip(entry["order"], forks, strict=True), 1):
                build, jar, benchmark, params = spec
                with (Path(tmp) / "console").open("w+") as console:
                    start = utc()
                    logs.write(f"\n===== block {entry['sequence']} slot {slot} {start} =====\n")
                    try:
                        result = measure_fork(settings, jar, benchmark, params, profiled, result_path, console, start)
                    finally:
                        console.seek(0)
                        shutil.copyfileobj(console, logs)
                row = dict(**entry, slot=slot, label=label, build=build, **result)
                raw.write(json.dumps(row, separators=(",", ":")) + "\n")
                rows.append(row)
            return rows

        yield record_block


def run_schedule(output, plan, jars, settings, describe_case, *, profile_grid=PROFILE_SHIFTS):
    """Run the saved plan. describe_case maps each entry to a case key, JMH method and string parameters."""
    calibration = defaultdict(list)
    profile_aa = defaultdict(list)
    timing_blocks = max(e["block"] for e in plan if e["stage"] == "timing")
    case_count = sum(e["stage"] == "timing" and e["block"] == 1 for e in plan)
    with fork_recorder(output) as record_block:
        for stage, entries in itertools.groupby(plan, key=lambda e: e["stage"]):
            for entry in entries:
                case_id, benchmark, params = describe_case(entry)
                builds = "AAAA" if stage in ("calibration", "profile_aa") else entry["order"]
                forks = [(build, jars[build], benchmark, params) for build in builds]
                rows = record_block(entry, settings, forks, profiled=stage in ("profile_aa", "profile"))
                if stage == "calibration":
                    scores = [row["jmh"]["primaryMetric"]["score"] for row in rows]
                    calibration[case_id].append(abs(log_effect(scores, entry["order"])))
                if stage == "profile_aa":
                    profile_aa[case_id].append(rows)
                if entry["sequence"] % case_count == 0:
                    print(f"{utc()} {stage} round {entry['block']} complete "
                          f"({4 * entry['sequence']}/{4 * len(plan)} JVM forks)", flush=True)
            if stage == "calibration":
                forecasts = {k: minimum_detectable_effects(values, [timing_blocks])[timing_blocks]
                             for k, values in calibration.items()}
                (output / "calibration.json").write_text(json.dumps(dict(frozen=utc(), cases=forecasts), indent=2) + "\n")
            if stage == "profile_aa":
                budget = max(e["block"] for e in plan if e["stage"] == "profile")
                forecasts = {k: profile_sensitivity(groups, budget, profile_grid) for k, groups in profile_aa.items()}
                (output / "profile-calibration.json").write_text(json.dumps(dict(frozen=utc(), cases=forecasts), indent=2) + "\n")
    print(f"{utc()} Collection complete", flush=True)
