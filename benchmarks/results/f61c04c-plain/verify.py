"""Audit the published estimates directly from the recorded JVM scores."""

import argparse
from collections import defaultdict
from bisect import bisect_left
import gzip
import hashlib
import itertools
import json
import math
from pathlib import Path
import re
import random
from statistics import geometric_mean, mean
import subprocess
import sys
import zipfile


sys.path.insert(0, str(Path(__file__).resolve().parents[2]))
from harness.tests.counter_oracle import verify_profile


def exact_p(ds, h=0.0):
    centered = [d - h for d in ds]
    totals = [0.0]
    for d in centered:
        totals = [v + d for v in totals] + [v - d for v in totals]
    observed = abs(math.fsum(centered))
    return sum(abs(v) >= observed - 1e-13 for v in totals) / len(totals)


def parts(rows):
    return ([r for r in rows if r["label"] == "A"], [r for r in rows if r["label"] == "B"])


def check_forecast(groups, forecast):
    # Directly evaluate the two adjacent grid points around the published MDE.
    # Use half sums to count sign flips without the forecast's threshold search.
    magnitudes = []
    for group in groups:
        a,b = parts(group)
        def geom(rows):
            return geometric_mean(r["jmh"]["primaryMetric"]["score"] for r in rows)
        magnitudes.append(abs(math.log(geom(b) / geom(a))))
    mde = forecast["mde_slowdown_pct"]
    shifts = [math.log1p(x/100) for x in [mde - .1, mde]]
    hits = [0, 0]
    rng = random.Random(405811)
    def sums(xs):
        values = [0.0]
        for x in xs:
            values = [s+x for s in values] + [s-x for s in values]
        return values
    for _ in range(forecast["draws"]):
        ds = [rng.choice(magnitudes) * rng.choice((-1,1)) for _ in range(12)]
        for i, h in enumerate(shifts):
            shifted = [d+h for d in ds]
            threshold = abs(math.fsum(shifted)) - 1e-13
            right = sorted(sums(shifted[6:]))
            tail = sum(len(right)-bisect_left(right,threshold-x) for x in sums(shifted[:6]))
            hits[i] += 2*tail/4096 <= .05
    rates = [x/forecast["draws"] for x in hits]
    assert rates[0] < .8 <= rates[1] and rates[1] == forecast["estimated_power"], (mde, rates)


def main():
    cli = argparse.ArgumentParser(description=__doc__)
    cli.add_argument("root", type=Path)
    root = cli.parse_args().root
    data = root / "data"
    plan, results = [json.loads((data / f"{f}.json").read_text()) for f in ("plan", "analysis")]
    forecasts = json.loads((data / "calibration.json").read_text())["cases"]
    rows = list(map(json.loads,(data / "forks.jsonl").read_text().splitlines()))
    assert len(rows) == 1344
    assert all(row["before"] == row["after"] == "performance" for row in rows)
    assert all(a["end"] <= b["start"] for a,b in itertools.pairwise(rows))
    assert len(set((row["sequence"],row["slot"]) for row in rows)) == len(rows)
    with gzip.open(data / "console.txt.gz", "rt") as f:
        logged = [tuple(map(int,re.match(r"===== block (\d+) slot (\d+)", line).groups()))
                  for line in f if line.startswith("===== block ")]
    assert logged == [(r["sequence"],r["slot"]) for r in rows]
    for build in plan["builds"].values():
        jar = data / build["jar"]
        assert hashlib.sha256(jar.read_bytes()).hexdigest() == build["sha256"]
        with zipfile.ZipFile(jar) as z:
            for name, key in [("Vector512Kernels","kernel_sha256"),("VectorCodecBenchmark","benchmark_sha256")]:
                assert hashlib.sha256(z.read("io/github/sherman/bitpacking/" + name + ".class")).hexdigest() == build[key]
    assert plan["builds"]["A"]["revision"] == "476bd9f401116d85083ca93178c5f364640046b8"
    assert plan["builds"]["B"]["revision"] == "f61c04c2141fe70571af97a8ae3dedebe7d924a1"
    with zipfile.ZipFile(data / plan["builds"]["A"]["jar"]) as a, zipfile.ZipFile(data / plan["builds"]["B"]["jar"]) as b:
        classes = {k for k in a.namelist() if k.endswith(".class")}
        assert classes == {k for k in b.namelist() if k.endswith(".class")}
        different_classes = sorted(k for k in classes if a.read(k) != b.read(k))
        assert different_classes == ["io/github/sherman/bitpacking/Vector512Kernels.class"]

    repo = Path(__file__).resolve().parents[3]
    def source(side, path):
        revision = plan["builds"][side]["revision"]
        return subprocess.check_output(["git", "show", f"{revision}:{path}"], cwd=repo, text=True)
    suite = "src/jmh/java/io/github/sherman/bitpacking/VectorCodecBenchmark.java"
    kernel = "src/main/java/io/github/sherman/bitpacking/Vector512Kernels.java"
    assert source("A", suite) == source("B", suite)
    pattern = r"private static void ((?:un)?pack0_\d+(?:Part[0-9]+)?)\(.*?\) \{(.*?)\n    \}"
    methods = {side: dict(re.findall(pattern, source(side, kernel), re.S)) for side in ("A","B")}
    changed_methods = {}
    for case in results.values():
        bits, operation = case["bits"], case["operation"]
        prefix = ("pack0_" if operation == "encode" else "unpack0_") + str(bits)
        names = [k for k in methods["A"] if k == prefix or k.startswith(prefix+"Part")]
        assert names
        changed_methods[f"{operation}-{bits}"] = [k for k in names if methods["A"][k] != methods["B"][k]]
    assert {k for k,v in changed_methods.items() if v} == {
        f"{op}-{bits}" for op in ("encode","decode") for bits in (1,13,25)}

    cases = defaultdict(list)
    for _, it in itertools.groupby(rows,key=lambda r:r["sequence"]):
        group = list(it)
        start = group[0]
        assert [r["slot"] for r in group] == [1,2,3,4]
        assert "".join(r["label"] for r in group) in ("ABBA","BAAB")
        for r in group:
            expected = "A" if r["stage"] == "calibration" else r["label"]
            assert r["build"] == expected
            assert Path(r["command"][r["command"].index("-jar")+1]).name == Path(plan["builds"][expected]["jar"]).name
            assert r["jmh"]["params"] == {"bits":str(start["bits"]),"mode":"0","blocks":"16"}
        cases[start["operation"],start["bits"],start["stage"]].append(group)
    trace = (root / "13-bit-trace.md").read_text()
    trace_ids = re.findall(r"^\| (\d+)\.(\d+) \|", trace, re.M)
    assert trace_ids == [(str(r["sequence"]),str(r["slot"])) for r in rows if r["bits"]==13]
    assert len(trace_ids)==192
    drift = {}
    def near(a,b):
        assert math.isclose(a,b,rel_tol=1e-10,abs_tol=1e-10), (a,b)
    for key,result in results.items():
        op, bits=result["operation"],result["bits"]
        cal,timing,profile = (cases[op,bits,stage] for stage in ["calibration","timing","profile"])
        assert [len(x) for x in [cal,timing,profile]] == [8,12,4]
        check_forecast(cal, forecasts[key])
        t = result["timing"]
        def score(row):
            return row["jmh"]["primaryMetric"]["score"]
        ds=[]
        for g in timing:
            a,b=parts(g)
            ds.append(math.log(geometric_mean(map(score,b))/geometric_mean(map(score,a))))
        near(100*math.expm1(mean(ds)),t["effect_pct"])
        assert exact_p(ds)==t["p"]
        for col, side in (("baseline","A"),("candidate","B")):
            near(geometric_mean(score(r)/16 for g in timing for r in g if r["label"]==side),
                 t[f"{col}_ns_per_codec_block"])
        ci = t["timing_ci"]
        for bound, direction in [(ci["lower_pct"],1),(ci["upper_pct"],-1)]:
            h = math.log1p(bound/100)
            assert exact_p(ds,h + direction*1e-8)>.05 and exact_p(ds,h - direction*1e-8)<=.05
        assert t["verdict"] == ("INCONCLUSIVE" if t["p"]>.05 else ("IMPROVED" if mean(ds)<0 else "REGRESSED"))
        verify_profile(profile, result["profile"])
        # Descriptive within-fork check: first to last measured iteration per build.
        drift[key]={}
        for side in ("A","B"):
            changes=[]
            for g in timing:
                for r in g:
                    if r["build"]==side:
                        xs,=r["jmh"]["primaryMetric"]["rawData"]
                        changes.append(xs[-1]/xs[0])
            drift[key][side]=100*(geometric_mean(changes)-1)
    assert len(results)==14
    summary=dict(complete_forks=1344, governor_boundary_readings=2688, verified_console_records=len(logged),
                 verified_timing_intervals_and_p_values=len(results), verified_counter_changes=3*len(results),
                 verified_counter_intervals_and_raw_and_holm_p_values=3*len(results),
                 verified_calibration_forecasts=len(forecasts),
                 verified_13_bit_trace_rows=len(trace_ids), class_entries_compared=len(classes),
                 different_classes=different_classes, changed_width_specific_methods=changed_methods,
                 last_vs_first_measured_iteration_pct=drift)
    (data/"audit.json").write_text(json.dumps(summary,indent=2)+"\n")
    print("Verified all 14 timing intervals and p-values, 42 counter changes, 1,344 fork logs, and 192 trace rows.")


if __name__=="__main__":
    main()
