# JMH performance harness worklog

## Goal and ownership

The user chose the protocol, the plain codec workloads and a local Git commit containing the harness and the completed experiment. Codex owns the implementation, cleanup and verification.

## Protocol decisions

- Use one fresh, single-threaded JVM per slot. Within each stage, shuffle cases each round and independently draw ABBA or BAAB for each four-fork block. Pin every launcher and its child JVM to the selected logical CPU. Require its kernel `scaling_governor` to be `performance` at startup, at both fork boundaries, and when polled every 500 ms during a fork. A failed check stops collection and excludes the interrupted fork.
- Compare the baseline with itself during A/A calibration, using the same JMH settings as the later A/B timing comparison. Freeze the per-case forecast before launching the first A/B fork. Report the smallest slowdown on a 0.1-percentage-point grid detected in at least 80% of 2,000 seeded resamples of the A/A block magnitudes at the planned timing budget.
- Use one prespecified final timing check per case. Give each block equal weight and compute its candidate-to-baseline log ratio. Transform the mean back to a percentage. Use a two-sided sign-flip test and a per-case 95% confidence interval obtained by inverting that test for a common multiplicative change. Enumerate assignments through 20 blocks and sample signs with a fixed seed above that. A wholly negative interval means IMPROVED, a wholly positive interval means REGRESSED, and an interval containing zero means INCONCLUSIVE.
- Collect cycles/op, instructions/op and allocated Java heap B/op together in separate profiled forks. Report paired geometric changes for the CPU counters and an arithmetic difference in B/op for allocation. The timing verdict comes from the unprofiled forks.

## Completed plain codec commit comparison

The user requested separate plain encode and decode measurements at 1, 3, 8, 13, 16, 25 and 31 bits for commit `f61c04c2` against its parent `476bd9f4`. Codex completed 1,344 guarded JVM forks in 52.4 minutes. Eight A/A, twelve unprofiled timing and four profiler blocks per case used the commit study's 16 codec blocks of 128 integers per JMH operation. All 2,688 boundary readings confirmed the performance governor on CPU 2. Both builds passed 41 tests and only `Vector512Kernels.class` differed among 1,713 JAR class entries. The 14 per-case A/A forecasts were frozen before timing; the estimated 80%-power slowdown MDEs ranged from 2.9% to 10.8%.

Encode and decode improved at 1, 13 and 25 bits. Encode regressed at 16 bits (+10.74%, 95% CI +6.56% to +15.05%). Decode improved at 31 bits (−3.13%, 95% CI −5.51% to −0.72%). The corresponding width-specific method bodies at 16 and 31 are unchanged, and the mechanism remains unresolved. The remaining six per-case verdicts were inconclusive. An eight-block retrospective timing replay retained the six gains at the changed widths and the 16-bit encode regression. The twelve-block budget additionally resolved 31-bit decode.

Codex verified every timing interval and p-value, the fourteen MDE forecasts, all forty-two counter changes, and every row of the requested 13-bit execution log. [Final English reports](benchmarks/results/f61c04c-plain/index.html) · [Step-by-step log for all 192 13-bit forks](benchmarks/results/f61c04c-plain/13-bit-trace.md) · [Audit](benchmarks/results/f61c04c-plain/data/audit.json)

## Finalization

The reusable runner, CPU guard, analysis, report card and protocol tests live in [benchmarks/harness](benchmarks/harness/README.md). The commit-specific build checks, reporting and independent audit live beside the measured experiment. The requested cleanup covers development pilots, synthetic campaigns and exploratory reports. The older `structure-simplification` archive and local IDE file are outside this scope.

Removed the development pilots after retaining the independent protocol tests. The extracted collector completed a fresh twelve-fork 13-bit decode check across all three stages. Its seeded scheduler reproduced all 336 original block entries. Regenerating the report and running the independent audit reproduced the stored outputs byte for byte. The local commit includes the measured JARs, both build logs and all fork records.
