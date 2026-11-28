# Paired JMH performance harness

Use this Python harness to compare two benchmark JARs on the same logical CPU. The timing verdict uses one prespecified endpoint per benchmark case. The [plain codec experiment](../results/f61c04c-plain/README.md) includes the collection command, build inputs, published reports and raw JMH results for a complete run.

## Replay the saved experiment

From the repository root, regenerate the HTML reports and the 13-bit execution log, then independently verify the estimates against the fork records and Git revisions. Replay uses Python 3.10 or newer and Git. It does not launch a JVM.

```sh
study=benchmarks/results/f61c04c-plain
python3 "$study/report.py" "$study"
python3 "$study/verify.py" "$study"
```

Run the harness tests from the same directory. They cover the governor guard, randomized schedule, interval coverage, verdicts, allocation units and calibration against independent generated fork timings.

```sh
PYTHONPATH=benchmarks python3 -m unittest discover -s benchmarks/harness/tests
```

## Run a comparison

The [plain codec collector](../results/f61c04c-plain/collect.py) is a working example. A new experiment supplies its JARs, cases and budgets to [runner.py](runner.py).

1. Select a logical CPU and call `start_run(cpu)` to check its kernel governor. Fresh measurements require Linux, `taskset`, the JDK for the benchmark JARs, and permission to read hardware performance counters for profiling. Both JARs must expose the same benchmark and parameters and report time in ns/op.
2. Call `make_schedule` with case dictionaries, a seed and block counts for calibration, timing and profiling. Save the plan, JAR hashes, CPU, JVM and iteration settings before collection. Cases are shuffled each round. Each four-fork block draws ABBA or BAAB independently.
3. Call `run_schedule(output, plan, jars, settings, describe_case)` in a fresh output directory. Supply JAR paths keyed by `A` and `B`, a `ForkSettings` instance, and a function mapping a plan entry to a unique case key, the fully qualified JMH method and a dictionary of string parameters. Each slot launches one pinned, single-threaded JVM. A governor change stops the run. The output includes a JSON record per completed fork, a compressed console log and the frozen calibration forecast.
4. Group the raw records by case, stage and four-fork block, in slot order. Use `timing_summary` on the unprofiled A/B blocks and `profile_summary` on the profiled blocks. The [timing card renderer](report_card.py) combines those summaries with the A/A forecast for the case.

## Read the result

| Stage | Builds and measurement | Result |
| --- | --- | --- |
| A/A calibration | Both randomized labels use the baseline JAR with the timing settings. | Smallest slowdown predicted to trigger the final test in at least 80% of 2,000 seeded resamples, on a 0.1-percentage-point grid. The forecast is saved before A/B starts. |
| A/B timing | Each label runs its own JAR without a profiler. | Change in ns/op and its per-case 95% confidence interval. A wholly negative interval gives IMPROVED, a wholly positive interval gives REGRESSED, and an interval containing zero gives INCONCLUSIVE. |
| A/B profiling | Separate forks run JMH `perfnorm` and `gc` together. | Relative changes in cycles/op and instructions/op, plus the candidate-minus-baseline difference in allocated heap B/op. |

Timing and CPU counters use the average of the block log ratios, converted back to percent. Each complete block has the same weight. The timing interval inverts a two-sided sign-flip test under a common multiplicative change. The final test enumerates sign assignments through 20 blocks and uses seeded sampling for larger budgets. The archived experiment used 12 timing blocks per case.
