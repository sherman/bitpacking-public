"""Fresh eight-block profiling study of the 16-bit plain codec and independent controls."""

import argparse
from collections import defaultdict
from dataclasses import asdict, replace
import hashlib
import json
from pathlib import Path
import random
import shutil
import subprocess
import sys
import tempfile

BENCHMARKS = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(BENCHMARKS))
from harness.analysis import ALPHA, profile_sensitivity
from harness.cpu_policy import start_run
from harness.runner import ForkSettings, fork_recorder, utc

ORIGINAL = BENCHMARKS / "results/f61c04c-plain/data"
FIXTURE = BENCHMARKS / "harness/tests/fixtures/IndependentBenchmark.java"
SEED = 2611282113
BLOCKS = 8
DRAWS = 2000
GRID = {"cycles": [1, 2, 5, 10, 20], "instructions": [1, 2, 5, 10, 20],
        "gc.alloc.rate.norm": [0.001, 0.01, 0.1, 1, 16, 256]}
CODECS = {"encode": "compress", "decode": "decompress"}
CONTROLS = {"cpu": ("arithmetic", "rounds", 64, 128), "allocation": ("allocate", "bytes", 16, 272)}


def make_plan():
    rng = random.Random(SEED)
    plan = []
    for stage, cases in (("profile_aa", CODECS), ("profile_ab", CODECS), ("controls", CONTROLS)):
        for block in range(1, BLOCKS + 1):
            order = list(cases)
            rng.shuffle(order)
            for case in order:
                plan.append(dict(sequence=len(plan)+1, stage=stage, case=case, block=block,
                                 order=rng.choice(("ABBA", "BAAB"))))
    return plan


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--processor", required=True, type=Path, help="JMH 1.37 annotation processor JAR")
    args = parser.parse_args()
    old = json.loads((ORIGINAL / "plan.json").read_text())
    cpu = old["cpu"]
    policy, power = start_run(cpu)
    settings = ForkSettings(cpu, old["java"], ("--add-modules=jdk.incubator.vector",), 2, "500ms", 3, "250ms")
    control_settings = replace(settings, jvm_args=("-Xms256m", "-Xmx256m"))
    jars = {side: ORIGINAL / build["jar"] for side, build in old["builds"].items()}
    for side, jar in jars.items():
        if sha(jar) != old["builds"][side]["sha256"]:
            raise ValueError(f"Archived {side} JAR hash differs from its manifest")
    plan = make_plan()
    out = args.output.resolve()
    out.mkdir(parents=True, exist_ok=False)
    with tempfile.TemporaryDirectory(prefix="jmh-profile-control-") as temp:
        classes = Path(temp) / "classes"
        classes.mkdir()
        subprocess.run(["javac", "-cp", str(jars["A"]), "-processorpath", f"{args.processor}:{jars['A']}",
                        "-processor", "org.openjdk.jmh.generators.BenchmarkProcessor", "-d", str(classes),
                        str(FIXTURE)], check=True)
        control = out / "controls.jar"
        shutil.copyfile(jars["A"], control)
        subprocess.run(["jar", "uf", str(control), "-C", str(classes), "."], check=True)
        manifest = dict(started=utc(), seed=SEED, cpu=cpu, policy=policy, power_profile=power,
                        builds=old["builds"], java_version=subprocess.check_output([settings.java, "-version"],
                        stderr=subprocess.STDOUT, text=True), codec_settings=asdict(settings),
                        control_settings=asdict(control_settings), fixture_sha256=sha(FIXTURE),
                        processor_sha256=sha(args.processor), control_jar_sha256=sha(control),
                        blocks=BLOCKS, alpha=ALPHA, sensitivity_grid=GRID, draws=DRAWS, plan=plan)
        (out / "plan.json").write_text(json.dumps(manifest, indent=2) + "\n")
        aa = defaultdict(list)
        with fork_recorder(out) as record:
            for e in plan:
                stage, case = e["stage"], e["case"]
                if stage == "controls":
                    method, param, a, b = CONTROLS[case]
                    forks = [("fixture", control, "calibration.controls.IndependentBenchmark."+method,
                              {param: str(a if side == "A" else b)}) for side in e["order"]]
                    rows = record(e, control_settings, forks, profiled=True)
                else:
                    method = "io.github.sherman.bitpacking.VectorCodecBenchmark." + CODECS[case]
                    params = dict(bits="16", mode="0", blocks="16")
                    builds = "AAAA" if stage == "profile_aa" else e["order"]
                    rows = record(e, settings, [(side, jars[side], method, params) for side in builds], profiled=True)
                if stage == "profile_aa":
                    aa[case].append(rows)
                    if all(len(aa[name]) == BLOCKS for name in CODECS):
                        forecasts = {name: profile_sensitivity(groups, BLOCKS, GRID, draws=DRAWS, seed=SEED + 1) for name, groups in aa.items()}
                        (out / "calibration.json").write_text(json.dumps(dict(frozen=utc(), cases=forecasts), indent=2)+"\n")
                if e["sequence"] % 2 == 0:
                    print(f"{utc()} {stage} round {e['block']} complete ({4*e['sequence']}/{4*len(plan)} forks)", flush=True)


if __name__ == "__main__":
    main()
