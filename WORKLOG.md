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

## Counter uncertainty and 16-bit follow-up

The user approved marginal 95% intervals and two-sided p-values for the three profiled counters, Holm-adjusted highlights within each benchmark case, and checking an eight-block profiling budget using profiled A/A and positive controls. Codex owns implementation and verification. The follow-up will profile both 16-bit operations at a fresh, fixed eight-block endpoint and report them alongside their original unprofiled timing results. The historical four-block profiler records remain a separate experiment.

Implemented per-counter inference and individual 95% intervals on the metric's analysis scale, with Holm-adjusted directions for the three counters per case. Replayed the fourteen original cases, preserving their timing verdicts, forecasts, measured counter estimates and raw evidence. The new shared profiler calibration resamples whole three-counter A/A block vectors and freezes the sensitivity forecast before candidate profiling.

Completed 192 fresh profiled forks on CPU 2. The 16-bit encode run measured +13.81% cycles/op, +10.46% instructions/op and +0.003462 B/op, each with adjusted p = 0.0234375. All three 16-bit decode results were inconclusive. Both profiled A/A cases had no significant counter. Doubling dependent arithmetic measured +99.97% cycles/op. The known 256-byte array increase measured +256 B/op. The saved report independently audits 18 counter comparisons across A/A, A/B and the two controls. [16-bit follow-up](benchmarks/results/f61c04c-profile-16/README.md)

## 16-bit encode instruction investigation

Codex traced the +10.46% instructions/op to C2 inlining on the measured JDK. A constant-pool shift widened eight mask loads in the unchanged 16-bit kernel. Its bytecode grew from 325 to 333 bytes, above the 325-byte hot inlining limit. Six compiler probes verified the default and reversed decisions. The extra 13 native instructions per codec block predict +208/op against the original measured +206.7/op. In 64 guarded profiler forks, forbidding this inline in the parent raised instructions/op by 11.02%, and forcing it in the commit lowered them by 9.22%. The investigation and independent counter audit are complete. [Report, confidence intervals and assembly](benchmarks/results/f61c04c-profile-16/encode-investigation/README.md)

## Plain 16-bit encode fix

The user confirmed that the vector kernel is handwritten. Codex removed the four masks made redundant by the 16-bit left shift. All 41 project tests and 4,096 additional direct byte comparisons passed. The fixed method occupies 275 bytecode bytes and C2 inlines it by default. The 112-fork campaign on CPU 2 measured −10.45% time/op (95% CI −13.53% to −7.25%) and −9.35% instructions/op (95% CI −9.74% to −8.90%) against the regressed JAR. The schedule, raw forks and intervals were verified. [16-bit source fix report](benchmarks/results/f61c04c-profile-16/encode-investigation/source-fix/encode.html)
