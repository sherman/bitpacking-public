"""Rebuild and audit the fresh profiling report, with timing from the original fixed endpoint."""

import argparse
from collections import defaultdict
from datetime import datetime
import gzip
import itertools
import json
from pathlib import Path

from collect import BLOCKS, CODECS, CONTROLS, DRAWS, FIXTURE, GRID, ORIGINAL, SEED, make_plan, sha
from harness.analysis import COUNTERS, profile_sensitivity, profile_summary
from harness.report_card import signed_pct, write_timing_card
from harness.tests.counter_oracle import verify_profile


def load_groups(data, plan):
    groups = defaultdict(list)
    expected = make_plan()
    assert expected == plan["plan"]
    archive = json.loads((ORIGINAL / "plan.json").read_text())
    assert plan["builds"] == archive["builds"] and plan["cpu"] == archive["cpu"]
    for build in plan["builds"].values():
        assert sha(ORIGINAL / build["jar"]) == build["sha256"]
    assert sha(data / "controls.jar") == plan["control_jar_sha256"] and sha(FIXTURE) == plan["fixture_sha256"]
    assert plan["blocks"] == BLOCKS and plan["alpha"] == .05 and plan["draws"] == DRAWS and plan["sensitivity_grid"] == GRID
    previous_end = datetime.fromisoformat(plan["started"])
    with (data / "forks.jsonl").open() as raw, gzip.open(data / "console.txt.gz", "rt") as console:
        headers = (line.strip() for line in console if line.startswith("===== block "))
        by_sequence = itertools.groupby(map(json.loads, raw), key=lambda r: r["sequence"])
        for entry, (seq, slots) in zip(expected, by_sequence, strict=True):
            rows = list(slots)
            assert seq == entry["sequence"] and [r["slot"] for r in rows] == [1, 2, 3, 4]
            stage, case = entry["stage"], entry["case"]
            for row, side in zip(rows, entry["order"], strict=True):
                assert all(row[k] == v for k, v in entry.items())
                assert row["before"] == row["after"] == "performance" and row["label"] == side
                start, end = [datetime.fromisoformat(row[k]) for k in ("start", "end")]
                assert previous_end <= start < end
                previous_end = end
                assert next(headers) == f"===== block {seq} slot {row['slot']} {row['start']} ====="
                cmd, jmh = row["command"], row["jmh"]
                assert cmd[:3] == ["taskset", "-c", str(plan["cpu"])]
                assert [cmd[i+1] for i, x in enumerate(cmd) if x == "-prof"] == ["perfnorm:events=cycles,instructions", "gc"]
                assert jmh["forks"] == jmh["threads"] == 1 and jmh["primaryMetric"]["scoreUnit"] == "ns/op"
                assert jmh["warmupIterations"] == 2 and jmh["measurementIterations"] == 3
                assert jmh["warmupTime"] == "500 ms" and jmh["measurementTime"] == "250 ms"
                for k in COUNTERS:
                    assert jmh["secondaryMetrics"][k]["scoreUnit"] == ("B/op" if k == "gc.alloc.rate.norm" else "#/op")
                if stage == "controls":
                    method, param, a, b = CONTROLS[case]
                    assert row["build"] == "fixture" and jmh["params"] == {param: str(a if side == "A" else b)}
                    assert jmh["benchmark"] == "calibration.controls.IndependentBenchmark." + method
                    jar = "controls.jar"
                else:
                    build = "A" if stage == "profile_aa" else side
                    assert row["build"] == build and jmh["params"] == {"bits": "16", "blocks": "16", "mode": "0"}
                    assert jmh["benchmark"] == "io.github.sherman.bitpacking.VectorCodecBenchmark." + CODECS[case]
                    jar = Path(plan["builds"][build]["jar"]).name
                assert Path(cmd[cmd.index("-jar")+1]).name == jar
            groups[stage, case].append(rows)
        assert next(headers, None) is None
    return groups


def checked_profile(groups):
    summary = profile_summary(groups)
    verify_profile(groups, summary)
    return summary


def measure_text(value, unit):
    return signed_pct(value) if unit == "%" else f"{value:+.4g} B/op"


def table_rows(label, profiled):
    names = ("Cycles/op", "Instructions/op", "Allocated heap")
    lines = []
    for name, key in zip(names, COUNTERS):
        m = profiled["counters"][key]
        unit, ci = m["unit"], m["ci"]
        bounds = f"{measure_text(ci['lower'], unit)} to {measure_text(ci['upper'], unit)}"
        lines.append(f"| {label} | {name} | {measure_text(m['change'], unit)} | {bounds} | {m['p']:.5g} | "
                     f"{m['p_holm']:.5g} | {m['direction'] or 'Inconclusive'} |")
    return lines


def report_markdown(cases, controls, forecasts):
    lines = ["# 16-bit plain encode and decode", "",
             "The parent (476bd9f4) and commit (f61c04c2) ran the same JMH method and processed 16 codec blocks "
             "of 128 integers per operation. The follow-up used fresh profiled forks on CPU 2 with the performance "
             "governor. Every case had eight independently randomized ABBA or BAAB blocks. "
             "The original unprofiled timing endpoint used twelve blocks per case.", "",
             "| Operation | Original time/op change | 95% timing interval | Timing verdict |",
             "|---|---:|---:|---|"]
    for name, case in cases.items():
        t = case["timing"]
        ci = t["timing_ci"]
        lines.append(f"| [{name.capitalize()}]({name}.html) | {signed_pct(t['effect_pct'])} | "
                     f"{signed_pct(ci['lower_pct'])} to {signed_pct(ci['upper_pct'])} | {t['verdict']} |")
    columns = ["| Case | Profiled metric | Change | Individual 95% interval | Raw p | Holm p | Direction at 5% |",
               "|---|---|---:|---:|---:|---:|---|"]
    lines += ["", "## Fresh profiler results", "", *columns]
    for name, case in cases.items():
        lines.extend(table_rows(name.capitalize(), case["profile"]))
    lines += ["", "Each counter uses a two-sided sign-flip test. Holm adjustment covers three counters within "
              "the same operation. CPU intervals use a common multiplicative change, and allocation intervals "
              "use an additive change in B/op. A positive estimate indicates a larger value on the commit. The width-specific "
              "16-bit plain methods have identical source in both revisions. The measurements compare the complete JARs.", "",
              "## Sensitivity at eight blocks", "",
              "Before the first candidate fork, the controller measured eight profiled parent-versus-parent blocks "
              "for each operation. It resampled complete three-counter block vectors 2,000 times. Each trial "
              "injected an increase into one counter and tested whether Holm identified it in the correct direction. "
              "The table gives the first tested grid point reaching 80% detection. Estimates depend on this A/A sample.", "",
              "| Operation | Cycles/op | Instructions/op | Allocated heap | Any false flag with zero shift |",
              "|---|---:|---:|---:|---:|"]
    for name in cases:
        f = forecasts[name]
        thresholds = []
        for key in COUNTERS:
            cross = next((x for x in GRID[key] if f["detection"][key][str(x)] >= .8), None)
            thresholds.append("Above tested grid" if cross is None else
                              f"{cross:g}" + (" B/op" if key == "gc.alloc.rate.norm" else "%"))
        lines.append(f"| {name.capitalize()} | " + " | ".join(thresholds) +
                     f" | {100*f['family_false_call_rate']:.2f}% |")
    lines += ["", "## Independent JMH controls", "",
              "The arithmetic fixture increased dependent rounds from 64 to 128. The allocation fixture increased "
              "the size of an escaping array from 16 to 272 bytes, a known change of +256 B/op.", "", *columns]
    for name, profile in controls.items():
        lines.extend(table_rows("CPU" if name == "cpu" else name.capitalize(), profile))
    overhead = controls["cpu"]["counters"]["gc.alloc.rate.norm"]["change"]
    lines += ["", f"The arithmetic loop creates no objects in its Java body, yet its measured heap difference "
              f"was {measure_text(overhead, 'B/op')}. JMH normalized heap bytes include activity outside "
              "the benchmark method. A small B/op change alone cannot identify an allocation site."]
    lines += ["", "## Evidence and reproduction", "",
              "The follow-up comprises 64 profiled A/A forks, 64 codec A/B forks and 64 positive-control forks. "
              "The console and raw records contain the command for every slot. Replaying the report checks the "
              "frozen plan, fork order, governor boundaries, JAR hashes, and forecasts, and independently "
              "enumerates signs to audit all counter p-values and interval boundaries.", "",
              "[Original timing study](../f61c04c-plain/README.md) · [Plan](data/plan.json) · "
              "[Frozen sensitivity](data/calibration.json) · [Raw fork records](data/forks.jsonl) · "
              "[Console](data/console.txt.gz) · [Case and control estimates](data/analysis.json) · "
              "[Measured control JAR](data/controls.jar)", "",
              "From the repository root, replay the saved study with", "",
              "```sh", "study=benchmarks/results/f61c04c-profile-16", 'python3 "$study/report.py" "$study"', "```", "",
              "For a fresh run, use a new output directory, the archived codec JARs and a JMH 1.37 annotation processor.", "",
              "```sh", 'repeat=$(mktemp -d)', 'python3 "$study/collect.py" --output "$repeat/data" --processor "$JMH_PROCESSOR"',
              'python3 "$study/report.py" "$repeat"', "```"]
    return "\n".join(lines)+"\n"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("root", type=Path)
    root = parser.parse_args().root
    data = root / "data"
    plan = json.loads((data / "plan.json").read_text())
    groups = load_groups(data, plan)
    frozen = json.loads((data / "calibration.json").read_text())
    old = json.loads((ORIGINAL / "analysis.json").read_text())
    cases = {}
    for name, method in CODECS.items():
        aa, ab = groups["profile_aa", name], groups["profile_ab", name]
        assert aa[-1][-1]["end"] < frozen["frozen"] < ab[0][0]["start"]
        assert profile_sensitivity(aa, BLOCKS, GRID, draws=DRAWS, seed=SEED+1) == frozen["cases"][name]
        historical = old[name+"-16"]
        profile = checked_profile(ab)
        cases[name] = dict(timing=historical["timing"], profile=profile, profile_aa=checked_profile(aa))
        write_timing_card(historical["timing"], profile, historical["calibration"]["mde_slowdown_pct"],
                          data / "analysis.json", root / (name+".html"), cpu=plan["cpu"],
                          title=f"VectorCodecBenchmark.{method}",
                          context=f"16-bit plain {name} · Original timing and fresh eight-block counters · 476bd9f4 → f61c04c2")
    controls = {k: checked_profile(groups["controls", k]) for k in CONTROLS}
    (data / "analysis.json").write_text(json.dumps(dict(cases=cases, controls=controls), indent=2)+"\n")
    (root / "README.md").write_text(report_markdown(cases, controls, frozen["cases"]))
    print("Verified 192 fork records and console headers, two frozen forecasts and 18 counter comparisons.")


if __name__ == "__main__":
    main()
