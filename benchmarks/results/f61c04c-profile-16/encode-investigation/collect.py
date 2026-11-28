"""Reverse one C2 inlining decision in each of the two measured JARs."""

import argparse
import gzip
import json
from pathlib import Path
import random
import shutil
import sys
import tempfile

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parents[2]))
from harness.cpu_policy import start_run
from harness.runner import ForkSettings, measure_fork, utc

DATA = HERE.parents[1] / "f61c04c-plain/data"
SEED = 1616333
METHOD = "io.github.sherman.bitpacking.VectorCodecBenchmark.compress"
PARAMS = dict(bits="16", mode="0", blocks="16")
TREATMENT = {"A": "noinline", "B": "inline"}


def make_plan():
    rng = random.Random(SEED)
    plan = []
    for block in range(1, 9):
        builds = ["A", "B"]
        rng.shuffle(builds)
        for build in builds:
            plan.append(dict(sequence=len(plan) + 1, build=build, block=block, order=rng.choice(("ABBA", "BAAB"))))
    return plan


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("output", type=Path)
    out = parser.parse_args().output.resolve()
    original = json.loads((DATA / "plan.json").read_text())
    cpu = original["cpu"]
    policy, power = start_run(cpu)
    out.mkdir(parents=True, exist_ok=False)
    plan = make_plan()
    manifest = dict(plan=plan, seed=SEED, started=utc(), cpu=cpu, policy=policy, power_profile=power,
                    builds=original["builds"], primary_metric="instructions", treatment=TREATMENT)
    (out / "plan.json").write_text(json.dumps(manifest, indent=2) + "\n")
    with (
        (out / "forks.jsonl").open("x", buffering=1) as raw,
        gzip.open(out / "console.txt.gz", "xt") as logs,
        tempfile.TemporaryDirectory(prefix="encode16-inline-") as tmp,
    ):
        for entry in plan:
            build = entry["build"]
            jar = DATA / original["builds"][build]["jar"]
            for slot, label in enumerate(entry["order"], 1):
                setting = "default" if label == "A" else TREATMENT[build]
                opts = ["--add-modules=jdk.incubator.vector", "-XX:+UnlockDiagnosticVMOptions"]
                if setting != "default":
                    opts.append(f"-XX:CompilerDirectivesFile={HERE / (setting + '.json')}")
                settings = ForkSettings(cpu, original["java"], tuple(opts), 2, "500ms", 3, "250ms")
                start = utc()
                with (Path(tmp) / "console.txt").open("w+") as console:
                    logs.write(f"\n===== block {entry['sequence']} slot {slot} {start} =====\n")
                    try:
                        row = measure_fork(settings, jar, METHOD, PARAMS, True, Path(tmp) / "jmh.json", console, start)
                    finally:
                        console.seek(0)
                        shutil.copyfileobj(console, logs)
                record = dict(**entry, slot=slot, label=label, setting=setting, **row)
                raw.write(json.dumps(record, separators=(",", ":")) + "\n")
            if entry["sequence"] % 2 == 0:
                print(f"{utc()} round {entry['block']}/8 complete", flush=True)


if __name__ == "__main__":
    main()
