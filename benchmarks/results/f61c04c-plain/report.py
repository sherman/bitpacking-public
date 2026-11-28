"""Analyze the frozen two-revision run and generate the English reports."""

import argparse
from collections import defaultdict
from datetime import datetime
import itertools
import json
import math
from pathlib import Path
import shlex
from statistics import stdev

from collect import METHODS, WIDTHS
from harness.analysis import COUNTERS, metric_score, paired_change, percent, profile_summary, timing_summary
from harness.report_card import signed_pct, write_timing_card


def evidence(data, plan):
    grouped = defaultdict(list)
    with (data / "forks.jsonl").open() as f:
        records = itertools.groupby(map(json.loads, f), key=lambda r: r["sequence"])
        for entry, (seq, group) in zip(plan["plan"], records, strict=True):
            rows = list(group)
            assert entry["sequence"] == seq and [r["slot"] for r in rows] == [1, 2, 3, 4]
            assert "".join(r["label"] for r in rows) == entry["order"]
            for row in rows:
                assert all(row[k] == v for k, v in entry.items())
                assert row["build"] == ("A" if entry["stage"] == "calibration" else row["label"])
                assert row["before"] == row["after"] == "performance"
                assert row["command"][:3] == ["taskset", "-c", str(plan["cpu"])]
                assert Path(row["command"][row["command"].index("-jar") + 1]).name == Path(plan["builds"][row["build"]]["jar"]).name
                assert row["jmh"]["params"] == {"mode": "0", "bits": str(entry["bits"]), "blocks": "16"}
                assert row["jmh"]["benchmark"].endswith("." + METHODS[row["operation"]])
                assert row["jmh"]["primaryMetric"]["scoreUnit"] == "ns/op"
                assert len(row["jmh"]["primaryMetric"]["rawData"][0]) == row["jmh"]["measurementIterations"] == 3
                assert row["jmh"]["warmupIterations"] == 2
                assert row["jmh"]["threads"] == row["jmh"]["forks"] == 1
                assert ("-prof" in row["command"]) == (entry["stage"] == "profile")
            grouped[(entry["operation"], entry["bits"], entry["stage"])].append(rows)
    return grouped


def codec_timing_summary(groups):
    # Convert the absolute times from JMH's 16 codec blocks per operation.
    timing = {}
    for key, value in timing_summary(groups).items():
        if key.endswith("_ns_per_op"):
            timing[key.removesuffix("_op") + "_codec_block"] = value / 16
        else:
            timing[key] = value
    return timing


def index_html(cases):
    out = ['''<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Plain bitpacking · Commit f61c04c</title><style>
:root{color-scheme:dark;font:16px/1.5 Inter,system-ui,sans-serif;background:#111312;color:#edf1ee}
body{max-width:1050px;margin:36px auto;padding:0 20px}h1{line-height:1.15}h2{font-size:1.25rem;margin:28px 0 8px}
p{max-width:85ch}a{color:#a1d7c6}.scroll{overflow-x:auto}table{border-collapse:collapse;width:100%;background:#202321}
th,td{text-align:right;padding:8px 12px;border-bottom:1px solid #424943;white-space:nowrap;font-variant-numeric:tabular-nums}
th:first-child,td:first-child{text-align:left}th{font-size:.85rem;color:#d1d8d2}.improved{color:#77d6a9}.regressed{color:#ffac9f}.inconclusive{color:#efd097}
code{color:#d4dfd7}footer{margin:26px 0;color:#c1cbc4}</style><main>
<h1>Plain bitpacking performance</h1>
<p><code>476bd9f4</code> → <code>f61c04c2</code> · 512-bit vector kernels · CPU 2 · Performance governor</p>
<p>Each operation processes 16 codec blocks of 128 integers. Times below are geometric means in nanoseconds per codec block. Negative changes mean faster. Each width opens a report card with its confidence interval, counters, allocations and calibrated sensitivity.</p>''']
    for op in METHODS:
        out.append(f'<h2>Plain {op}</h2><div class="scroll"><table><thead><tr><th>Bits</th><th>Parent ns/block</th>'
                   '<th>Commit ns/block</th><th>Time change</th><th>95% CI</th><th>p</th><th>Verdict</th></tr></thead><tbody>')
        for bits in WIDTHS:
            case = cases[f"{op}-{bits}"]
            t, ci = case["timing"], case["timing"]["timing_ci"]
            verdict = t["verdict"]
            out.append(f'<tr><td><a href="cards/{op}-{bits}.html">{bits} bits</a></td>'
                       f'<td>{t["baseline_ns_per_codec_block"]:.3f}</td><td>{t["candidate_ns_per_codec_block"]:.3f}</td>'
                       f'<td>{signed_pct(t["effect_pct"])}</td><td>{signed_pct(ci["lower_pct"])} to {signed_pct(ci["upper_pct"])}</td>'
                       f'<td>{t["p"]:.4g}</td><td class="{verdict.lower()}">{verdict}</td></tr>')
        out.append('</tbody></table></div>')
    out.append('<h2>What did the extra forks buy?</h2><p>The full run used 672 timing forks, 448 separate A/A calibration '
               'forks and 224 profiler forks. This retrospective check reuses only the first 6, 8 or 12 timing blocks of each case. '
               'The published verdicts use the prespecified 12-block endpoint.</p>'
               '<div class="scroll"><table><thead><tr><th>Timing blocks per case</th><th>Total timing forks</th>'
               '<th>Improved</th><th>Regressed</th><th>Inconclusive</th></tr></thead><tbody>')
    for blocks in (6, 8, 12):
        verdicts = [c["timing_budget_replay"][blocks] for c in cases.values()]
        out.append(f'<tr><td>{blocks}</td><td>{len(cases)*4*blocks}</td><td>{verdicts.count("IMPROVED")}</td>'
                   f'<td>{verdicts.count("REGRESSED")}</td><td>{verdicts.count("INCONCLUSIVE")}</td></tr>')
    out.append('</tbody></table></div>')
    out.append('''<h2>Measurement protocol</h2><p>Single-threaded JMH 1.37 on Oracle JDK 25.0.1.
Each fresh JVM runs two 500 ms warmups and three 250 ms measurements. Each randomized ABBA or BAAB block contains four JVMs.
Every case has eight parent-versus-parent calibration blocks, twelve unprofiled parent-versus-commit timing blocks and four separately profiled blocks.
The timing verdict uses one prespecified two-sided test per case. Confidence intervals are per case.
All completed forks are included. The governor is checked before, during and after each fork.</p>
<p>Calibration estimates the slowdown detectable with 80% power at twelve timing blocks. Profiled cycle and instruction changes use paired log ratios.
Allocation is the candidate-minus-parent difference in Java heap bytes per 16-block JMH operation. These counters are diagnostic.</p>
<p>The commit changes the width-specific plain pack or unpack bodies at 1, 13 and 25 bits in this set. The corresponding bodies at
3, 8, 16 and 31 bits match between revisions. The measurements compare the two compiled JARs, including their JIT and code layout.</p>
<footer><a href="13-bit-trace.md">Step-by-step 13-bit execution log</a> · <a href="data/analysis.json">Analysis</a> ·
<a href="data/plan.json">Plan and build hashes</a> · <a href="data/calibration.json">Frozen A/A estimates</a> ·
<a href="data/forks.jsonl">Raw JMH forks</a> · <a href="data/console.txt.gz">JMH console output</a></footer></main></html>''')
    return "\n".join(out) + "\n"


def trace_13(grouped, cases, frozen, plan):
    def selected(stage):
        return sorted((g for op in METHODS for g in grouped[op, 13, stage]), key=lambda g: g[0]["sequence"])

    lines = ["# 13-bit plain encode and decode execution log", "",
             f"Campaign started {plan['started']} (UTC). This log follows all 192 JVM forks for the two 13-bit cases in execution order. "
             "The global block number includes measurements at the six other widths.", "",
             "## 1. Build the parent and candidate", "",
             "The two source trees were extracted separately with `git archive`. Each ran `mvn -o -Pbenchmarks verify` on the same JDK. "
             "Both passed all 41 tests. Of the 1,713 compiled classes in each JAR, only Vector512Kernels.class differed.", "",
             "| JVM build | Git revision | Benchmark JAR SHA-256 |", "|---|---|---|"]
    for side, build in plan["builds"].items():
        lines.append(f"| {side} | `{build['revision']}` | `{build['sha256']}` |")
    lines += ["", "## 2. Set up the measurement", "",
              f"Selected {plan['cpu_model']}, logical CPU {plan['cpu']}, SMT siblings {plan['smt_siblings']}. "
              f"The kernel governor at startup was `{plan['policy']['scaling_governor']}`. Schedule seed {plan['seed']}. "
              "Each round shuffled all width/operation cases and independently chose ABBA or BAAB for each case.", "",
              "Each slot ran a fresh JVM pinned with taskset. JMH regenerated deterministic 13-bit inputs before each iteration "
              "and verified the encoded data round trip outside the timed operation. The measured work was either 16 plain encodes "
              "or 16 plain decodes of 128 integers per codec block. Each fork score averages three measurement iterations after two warmups.", "",
              "The following tables use unrounded scores for analysis and display fork times to four decimals. The clock column is UTC. "
              "A/B label gives the randomized experimental slot. JAR identifies the binary actually run, including during A/A. "
              "Every row passed both boundary governor checks and the live guard.", ""]
    for num, stage, title, note in [
            (3, "calibration", "Measure baseline against itself and freeze the forecast",
             "Both labels use the parent JAR. The timing protocol is identical to the later unprofiled comparison."),
            (4, "timing", "Measure the commit against its parent",
             "Each A slot uses the parent and each B slot uses the commit. All twelve blocks per operation enter the one prespecified timing check."),
            (5, "profile", "Collect CPU counters and allocation in separate forks",
             "The runner adds the perfnorm and gc profilers. Cycles, instructions and allocated bytes are per 16-block JMH operation.")]:
        groups = selected(stage)
        lines += [f"## {num}. {title}", "", note, "", "Actual command for the first slot in this stage", "",
                  "```sh", shlex.join(groups[0][0]["command"]), "```", ""]
        header = "| Global block.slot | Start UTC | Operation | Case block | Order | Label | JAR | ns/op |"
        sep = "|---:|---|---|---:|---|---|---|---:|"
        if stage == "profile":
            header += " Cycles/op | Instructions/op | Allocated B/op |"
            sep += "---:|---:|---:|"
        lines += [header, sep]
        for group in groups:
            for row in group:
                time = metric_score(row, "time")
                line = (f"| {row['sequence']}.{row['slot']} | {row['start'][11:23]} | {row['operation']} | "
                        f"{row['block']} | {row['order']} | {row['label']} | {row['build']} | {time:.4f} |")
                if stage == "profile":
                    line += " " + " | ".join(f"{metric_score(row,k):.5f}" for k in COUNTERS) + " |"
                lines.append(line)
        lines.append("")
        if stage == "calibration":
            lines += [f"The harness saved the A/A forecast at {frozen['frozen']}. It resampled whole A/A block magnitudes, "
                      "assigned random signs, added candidate slowdowns and replayed the same fixed two-sided timing test. "
                      "The first 0.1-percentage-point grid step reaching 80% detection in 2,000 draws is the reported MDE. "
                      "This is a forecast over repeated runs. The A/B verdict uses its own observed block effects and p-value.", "",
                      "| Operation | A/A log SD × 100 | Slowdown MDE at 12 blocks |", "|---|---:|---:|"]
            for op in METHODS:
                cal = cases[f"{op}-13"]["calibration"]
                lines.append(f"| {op} | {cal['sd_log_pct']:.3f}% | {cal['mde_slowdown_pct']:.1f}% |")
            lines.append("")
    lines += ["## 6. Recompute the timing verdicts", "",
              "For each four-fork block the harness computed `d = (ln B1 + ln B2 − ln A1 − ln A2) / 2`. "
              "The change in percent is `100 × expm1(mean(d))`. Each block has equal weight. "
              "The two-sided p-value compares the absolute observed sum with all 4,096 sign assignments of the twelve block effects. "
              "Inverting that test gives the 95% interval for a common multiplicative timing change.", ""]
    for op in METHODS:
        group = grouped[op, 13, "timing"][0]
        a = [metric_score(row, "time") for row in group if row["label"] == "A"]
        b = [metric_score(row, "time") for row in group if row["label"] == "B"]
        d = paired_change(group, "time")
        lines += [f"The first {op} block was global block {group[0]['sequence']} ({group[0]['order']}). "
                  f"The parent forks took {a[0]:.4f} and {a[1]:.4f} ns/op. The commit forks took {b[0]:.4f} and "
                  f"{b[1]:.4f} ns/op. The ratio of the two geometric means was {math.exp(d):.6f}, giving "
                  f"a block change of {signed_pct(percent(d))}.", ""]
    lines += ["| Operation | Global block | Case block | Order | Block log ratio | Block time change |",
              "|---|---:|---:|---|---:|---:|"]
    for group in selected("timing"):
        first = group[0]
        d = paired_change(group,"time")
        lines.append(f"| {first['operation']} | {first['sequence']} | {first['block']} | {first['order']} | {d:+.8f} | {signed_pct(percent(d))} |")
    for op in METHODS:
        c=cases[f"{op}-13"]
        t, p = c["timing"], c["profile"]["counters"]
        ci=t["timing_ci"]
        lines += ["", f"**{op.capitalize()}** · {t['verdict']}. Parent {t['baseline_ns_per_codec_block']:.3f} ns/codec block, "
                  f"commit {t['candidate_ns_per_codec_block']:.3f}. The time change was {signed_pct(t['effect_pct'])}, "
                  f"95% CI {signed_pct(ci['lower_pct'])} to {signed_pct(ci['upper_pct'])}. "
                  f"{round(t['p'] * 4096)} of 4,096 assignments were at least as extreme (p = {t['p']:.6f}). "
                  f"Cycles/op changed {signed_pct(p['cycles']['change'])}, instructions/op {signed_pct(p['instructions']['change'])}, "
                  f"and allocation {p['gc.alloc.rate.norm']['change']:+.5f} B/op."]
    lines += ["","[All-width report](index.html) · [Schedule](data/plan.json) · [All raw fork results](data/forks.jsonl) · "
              "[Full JMH console logs](data/console.txt.gz)"]
    return "\n".join(lines)+"\n"


def main():
    cli = argparse.ArgumentParser(description=__doc__)
    cli.add_argument("root", type=Path, help="Campaign directory containing data/")
    root = cli.parse_args().root
    data = root / "data"
    plan = json.loads((data / "plan.json").read_text())
    frozen = json.loads((data / "calibration.json").read_text())
    grouped = evidence(data, plan)
    cases = {}
    (root / "cards").mkdir(exist_ok=True)
    analysis_path = data / "analysis.json"
    for bits, op in itertools.product(WIDTHS, METHODS):
        key=f"{op}-{bits}"
        aa, timing, profile = (grouped[op,bits,stage] for stage in ("calibration","timing","profile"))
        assert [len(aa),len(timing),len(profile)] == [8,12,4]
        assert datetime.fromisoformat(aa[-1][-1]["end"]) < datetime.fromisoformat(frozen["frozen"]) < datetime.fromisoformat(timing[0][0]["start"])
        forecast = frozen["cases"][key]
        calibration = dict(blocks=len(aa), forks=len(aa)*4, mde_slowdown_pct=forecast["mde_slowdown_pct"],
                           sd_log_pct=100 * stdev(paired_change(g,"time") for g in aa))
        t, p = codec_timing_summary(timing), profile_summary(profile)
        prefix_verdicts = {n: timing_summary(timing[:n])["verdict"] for n in (6, 8)}
        prefix_verdicts[12] = t["verdict"]
        cases[key] = dict(operation=op, bits=bits, calibration=calibration, timing=t, profile=p,
                          timing_budget_replay=prefix_verdicts)
        write_timing_card(t, p, calibration["mde_slowdown_pct"], analysis_path, root/"cards"/f"{key}.html",
                          title=f"VectorCodecBenchmark.{METHODS[op]}", cpu=plan["cpu"],
                          context=f"{bits}-bit plain {op} · 16 codec blocks per operation · 476bd9f4 → f61c04c2")
    analysis_path.write_text(json.dumps(cases, indent=2)+"\n")
    (root/"index.html").write_text(index_html(cases))
    (root/"13-bit-trace.md").write_text(trace_13(grouped,cases,frozen,plan))


if __name__ == "__main__":
    main()
