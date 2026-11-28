"""Run the fixed plain-codec comparison for the specified pair of verified builds."""

import argparse
import hashlib
import itertools
import json
import os
from pathlib import Path
import platform
import shutil
import subprocess
import sys
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parents[2]))
from harness.cpu_policy import start_run
from harness.runner import ForkSettings, make_schedule, run_schedule, utc

VERSIONS = {"A": "476bd9f401116d85083ca93178c5f364640046b8", "B": "f61c04c2141fe70571af97a8ae3dedebe7d924a1"}
WIDTHS = (1, 3, 8, 13, 16, 25, 31)
METHODS = {"encode": "compress", "decode": "decompress"}
SEED = 2611281947


def schedule():
    cases = [dict(bits=bits, operation=op) for bits, op in itertools.product(WIDTHS, METHODS)]
    return make_schedule(cases, seed=SEED, calibration_blocks=8, timing_blocks=12, profile_blocks=4)


def describe_case(entry):
    bits, op = entry["bits"], entry["operation"]
    benchmark = "io.github.sherman.bitpacking.VectorCodecBenchmark." + METHODS[op]
    return f"{op}-{bits}", benchmark, dict(bits=str(bits), mode="0", blocks="16")


def main():
    cli = argparse.ArgumentParser(description=__doc__)
    cli.add_argument("--builds", type=Path, required=True, help="Contains baseline and candidate source/build trees")
    cli.add_argument("--output", type=Path, required=True)
    args = cli.parse_args()
    cpu = 2
    if cpu not in os.sched_getaffinity(0):
        cli.error("CPU 2 is unavailable")
    policy, power = start_run(cpu)
    out = args.output.resolve()
    out.mkdir(parents=True, exist_ok=False)
    artifacts = out / "builds"
    artifacts.mkdir()
    builds, jars = {}, {}
    for side, name in [("A", "baseline"), ("B", "candidate")]:
        build = args.builds / name
        jar, = (build / "target").glob("*-benchmarks.jar")
        jars[side] = artifacts / f"{name}.jar"
        shutil.copyfile(jar, jars[side])
        log = (build / "build.log").read_text()
        if "BUILD SUCCESS" not in log or "Tests run: 41, Failures: 0, Errors: 0, Skipped: 0" not in log:
            raise ValueError(f"Unverified Maven build: {build}")
        (artifacts / (name + "-maven.txt")).write_text(log)
        suite = "io/github/sherman/bitpacking/"
        with zipfile.ZipFile(jar) as z:
            def class_hash(base):
                return hashlib.sha256(z.read(suite + base + ".class")).hexdigest()
            builds[side] = dict(revision=VERSIONS[side], jar=str(jars[side].relative_to(out)),
                                sha256=hashlib.sha256(jar.read_bytes()).hexdigest(),
                                benchmark_sha256=class_hash("VectorCodecBenchmark"),
                                kernel_sha256=class_hash("Vector512Kernels"))
    assert builds["A"]["benchmark_sha256"] == builds["B"]["benchmark_sha256"]
    assert builds["A"]["kernel_sha256"] != builds["B"]["kernel_sha256"]
    java = str(Path(shutil.which("java")).resolve())
    plan = schedule()
    metadata = dict(started=utc(), seed=SEED, cpu=cpu, policy=policy, power_profile=power,
                    kernel=platform.release(), cpu_model=next(x.split(":", 1)[1].strip() for x in
                    Path("/proc/cpuinfo").read_text().splitlines() if x.startswith("model name")),
                    smt_siblings=Path(f"/sys/devices/system/cpu/cpu{cpu}/topology/thread_siblings_list").read_text().strip(),
                    java=java, java_version=subprocess.run([java,"-version"], capture_output=True, text=True).stderr,
                    builds=builds, codec_blocks_per_op=16, integers_per_block=128,
                    warmup="2 x 500 ms", measurement="3 x 250 ms", alpha=0.05, mde_power=0.8, plan=plan)
    (out / "plan.json").write_text(json.dumps(metadata, indent=2) + "\n")
    settings = ForkSettings(cpu=cpu, java=java, jvm_args=("--add-modules=jdk.incubator.vector",),
                            warmup_iterations=2, warmup_time="500ms", measurement_iterations=3, measurement_time="250ms")
    run_schedule(out, plan, jars, settings, describe_case)


if __name__ == "__main__":
    main()
