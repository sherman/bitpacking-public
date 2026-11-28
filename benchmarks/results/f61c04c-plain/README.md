# Plain encode and decode at f61c04c

[All widths and individual English report cards](index.html) · [13-bit chronological execution log](13-bit-trace.md) · [Independent audit](data/audit.json)

This is a fixed-budget comparison of `f61c04c2141fe70571af97a8ae3dedebe7d924a1` with its direct parent `476bd9f401116d85083ca93178c5f364640046b8`. The workloads are the repository's `VectorCodecBenchmark.compress` and `VectorCodecBenchmark.decompress` with mode 0, separately at 1, 3, 8, 13, 16, 25 and 31 bits. Each operation processes sixteen 128-integer codec blocks with a known width. The same compiled benchmark runs against both vector kernels.

The commit's kernel study also used sixteen codec blocks per operation. Its original measurements used a different fork and iteration protocol. This run uses separate revision-specific JVMs, randomizes ABBA/BAAB by four-fork block and takes every completed fork into the analysis. The timing and profiler runs have independent schedules. The case-specific A/A estimates use the parent on both sides. Each per-case 95% interval assumes a common multiplicative timing shift. The fixed design makes 14 individual comparisons. [Original workload description](../../../docs/vector-kernels.md)

## Measured changes

Both encode and decode took less time at 1, 13 and 25 bits. Encode time fell 11.64%, 10.47% and 13.29%, respectively. Decode time fell 17.21%, 17.04% and 15.01%. These are the three requested widths with changes to the width-specific plain kernel methods.

At 16 bits, encode rose from 4.541 to 5.029 ns per codec block. The commit was slower in 11 of 12 four-fork blocks. The measured increase was 10.74%, with a 95% interval of 6.56% to 15.05%. Its separate profiling run measured 9.98% more cycles/op and 11.02% more instructions/op. At 31 bits decode was faster in 11 of 12 blocks, with an estimated 3.13% reduction and a 95% interval of 0.72% to 5.51% less time. The corresponding width-specific method bodies match between revisions, so the cause of these whole-JAR differences remains open. The other six timing verdicts were inconclusive.

The first eight timing blocks per case recovered the six improvements at 1, 13 and 25 bits and the 16-bit encode regression. The last four blocks changed the 31-bit decode verdict. This retrospective comparison used 448 versus 672 timing forks. The reported confidence intervals and verdicts use the planned twelve-block endpoint.

## Evidence

- [Frozen schedule, JVM, CPU, kernel and JAR hashes](data/plan.json)
- [Baseline and candidate JARs and Maven build logs](data/builds)
- [A/A sensitivity forecast saved before the first A/B fork](data/calibration.json)
- [Complete JMH JSON and command per fork](data/forks.jsonl) and [JMH console output](data/console.txt.gz)
- [Machine-readable case estimates](data/analysis.json) and [independent verification](data/audit.json)

The console headers use the same global block and slot numbers as the 13-bit execution log. A profiler's B/op result is per entire JMH operation. The absolute times in the summary tables are per codec block.

## Reproduce

From the repository root, create fresh build and results directories. The collector requires CPU 2 to use the performance governor. It polls during every fork and stops the run on a mismatch. Use the same JDK and cached Maven dependencies to reproduce the recorded setup.

```sh
campaign=benchmarks/results/f61c04c-plain
builds=$(mktemp -d)
repeat=$(mktemp -d)
mkdir "$builds/baseline" "$builds/candidate"
git archive 476bd9f401116d85083ca93178c5f364640046b8 | tar -x -C "$builds/baseline"
git archive f61c04c2141fe70571af97a8ae3dedebe7d924a1 | tar -x -C "$builds/candidate"
(cd "$builds/baseline" && mvn -o -Pbenchmarks verify > build.log 2>&1)
(cd "$builds/candidate" && mvn -o -Pbenchmarks verify > build.log 2>&1)
python3 "$campaign/collect.py" --builds "$builds" --output "$repeat/data"
python3 "$campaign/report.py" "$repeat"
python3 "$campaign/verify.py" "$repeat"
```

To regenerate the published report from the saved measurements, run only the final two scripts with `"$campaign"` as their directory argument.
