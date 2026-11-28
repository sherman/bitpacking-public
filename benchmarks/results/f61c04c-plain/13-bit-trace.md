# 13-bit plain encode and decode execution log

Campaign started 2026-11-28T16:47:58.075793+00:00 (UTC). This log follows all 192 JVM forks for the two 13-bit cases in execution order. The global block number includes measurements at the six other widths.

## 1. Build the parent and candidate

The two source trees were extracted separately with `git archive`. Each ran `mvn -o -Pbenchmarks verify` on the same JDK. Both passed all 41 tests. Of the 1,713 compiled classes in each JAR, only Vector512Kernels.class differed.

| JVM build | Git revision | Benchmark JAR SHA-256 |
|---|---|---|
| A | `476bd9f401116d85083ca93178c5f364640046b8` | `b3507c4f5c5ee1ee7a5eb3635bd2fd47fdf04ec7a5678b781ba75df0a154fdd8` |
| B | `f61c04c2141fe70571af97a8ae3dedebe7d924a1` | `1f40cef38603cdb18c095e8ff499b810cbadd6419157a6750c450ee7dc401ef5` |

## 2. Set up the measurement

Selected AMD Ryzen 9 9900X3D 12-Core Processor, logical CPU 2, SMT siblings 2,14. The kernel governor at startup was `performance`. Schedule seed 2611281947. Each round shuffled all width/operation cases and independently chose ABBA or BAAB for each case.

Each slot ran a fresh JVM pinned with taskset. JMH regenerated deterministic 13-bit inputs before each iteration and verified the encoded data round trip outside the timed operation. The measured work was either 16 plain encodes or 16 plain decodes of 128 integers per codec block. Each fork score averages three measurement iterations after two warmups.

The following tables use unrounded scores for analysis and display fork times to four decimals. The clock column is UTC. A/B label gives the randomized experimental slot. JAR identifies the binary actually run, including during A/A. Every row passed both boundary governor checks and the live guard.

## 3. Measure baseline against itself and freeze the forecast

Both labels use the parent JAR. The timing protocol is identical to the later unprofiled comparison.

Actual command for the first slot in this stage

```sh
taskset -c 2 /home/sherman/.sdkman/candidates/java/25.0.1-oracle/bin/java --add-modules=jdk.incubator.vector -jar /home/sherman/work/ai/bitpacking-java-public/benchmarks/results/f61c04c-plain/data/builds/baseline.jar 'io.github.sherman.bitpacking.VectorCodecBenchmark.decompress$' -p bits=13 -p mode=0 -p blocks=16 -t 1 -f 1 -wi 2 -w 500ms -i 3 -r 250ms -foe true -rf json -rff /tmp/jmh-f61c-fork-buxmm4he/jmh.json
```

| Global block.slot | Start UTC | Operation | Case block | Order | Label | JAR | ns/op |
|---:|---|---|---:|---|---|---|---:|
| 6.1 | 16:48:44.293 | decode | 1 | BAAB | B | A | 82.8369 |
| 6.2 | 16:48:46.608 | decode | 1 | BAAB | A | A | 79.1908 |
| 6.3 | 16:48:48.923 | decode | 1 | BAAB | A | A | 80.7948 |
| 6.4 | 16:48:51.239 | decode | 1 | BAAB | B | A | 92.2788 |
| 11.1 | 16:49:30.599 | encode | 1 | BAAB | B | A | 88.9755 |
| 11.2 | 16:49:32.914 | encode | 1 | BAAB | A | A | 82.0691 |
| 11.3 | 16:49:35.229 | encode | 1 | BAAB | A | A | 92.3748 |
| 11.4 | 16:49:37.545 | encode | 1 | BAAB | B | A | 93.6972 |
| 18.1 | 16:50:35.431 | decode | 2 | BAAB | B | A | 86.6246 |
| 18.2 | 16:50:37.746 | decode | 2 | BAAB | A | A | 84.1850 |
| 18.3 | 16:50:40.112 | decode | 2 | BAAB | A | A | 77.9460 |
| 18.4 | 16:50:42.427 | decode | 2 | BAAB | B | A | 85.7670 |
| 20.1 | 16:50:54.004 | encode | 2 | BAAB | B | A | 76.9840 |
| 20.2 | 16:50:56.319 | encode | 2 | BAAB | A | A | 96.2353 |
| 20.3 | 16:50:58.634 | encode | 2 | BAAB | A | A | 93.1026 |
| 20.4 | 16:51:00.949 | encode | 2 | BAAB | B | A | 92.5995 |
| 30.1 | 16:52:26.515 | decode | 3 | ABBA | A | A | 84.9442 |
| 30.2 | 16:52:28.830 | decode | 3 | ABBA | B | A | 85.2224 |
| 30.3 | 16:52:31.145 | decode | 3 | ABBA | B | A | 83.3558 |
| 30.4 | 16:52:33.461 | decode | 3 | ABBA | A | A | 82.2205 |
| 33.1 | 16:52:54.250 | encode | 3 | ABBA | A | A | 108.7419 |
| 33.2 | 16:52:56.565 | encode | 3 | ABBA | B | A | 77.5018 |
| 33.3 | 16:52:58.880 | encode | 3 | ABBA | B | A | 95.4888 |
| 33.4 | 16:53:01.195 | encode | 3 | ABBA | A | A | 92.9860 |
| 54.1 | 16:56:08.636 | decode | 4 | ABBA | A | A | 80.3895 |
| 54.2 | 16:56:10.951 | decode | 4 | ABBA | B | A | 82.5188 |
| 54.3 | 16:56:13.266 | decode | 4 | ABBA | B | A | 80.8728 |
| 54.4 | 16:56:15.581 | decode | 4 | ABBA | A | A | 83.7163 |
| 55.1 | 16:56:17.896 | encode | 4 | ABBA | A | A | 83.3101 |
| 55.2 | 16:56:20.211 | encode | 4 | ABBA | B | A | 100.0474 |
| 55.3 | 16:56:22.526 | encode | 4 | ABBA | B | A | 91.3339 |
| 55.4 | 16:56:24.841 | encode | 4 | ABBA | A | A | 78.8009 |
| 58.1 | 16:56:45.679 | decode | 5 | ABBA | A | A | 77.7885 |
| 58.2 | 16:56:47.994 | decode | 5 | ABBA | B | A | 88.0607 |
| 58.3 | 16:56:50.309 | decode | 5 | ABBA | B | A | 82.4705 |
| 58.4 | 16:56:52.624 | decode | 5 | ABBA | A | A | 81.3941 |
| 62.1 | 16:57:22.726 | encode | 5 | BAAB | B | A | 90.4177 |
| 62.2 | 16:57:25.043 | encode | 5 | BAAB | A | A | 77.1527 |
| 62.3 | 16:57:27.358 | encode | 5 | BAAB | A | A | 79.4245 |
| 62.4 | 16:57:29.674 | encode | 5 | BAAB | B | A | 96.8731 |
| 72.1 | 16:58:55.289 | decode | 6 | ABBA | A | A | 81.2442 |
| 72.2 | 16:58:57.603 | decode | 6 | ABBA | B | A | 85.4402 |
| 72.3 | 16:58:59.918 | decode | 6 | ABBA | B | A | 91.2086 |
| 72.4 | 16:59:02.234 | decode | 6 | ABBA | A | A | 81.9503 |
| 78.1 | 16:59:50.953 | encode | 6 | ABBA | A | A | 93.1439 |
| 78.2 | 16:59:53.268 | encode | 6 | ABBA | B | A | 95.6105 |
| 78.3 | 16:59:55.584 | encode | 6 | ABBA | B | A | 88.5959 |
| 78.4 | 16:59:57.899 | encode | 6 | ABBA | A | A | 95.3196 |
| 91.1 | 17:01:51.443 | encode | 7 | ABBA | A | A | 79.1674 |
| 91.2 | 17:01:53.759 | encode | 7 | ABBA | B | A | 88.6238 |
| 91.3 | 17:01:56.074 | encode | 7 | ABBA | B | A | 79.6813 |
| 91.4 | 17:01:58.389 | encode | 7 | ABBA | A | A | 92.3200 |
| 93.1 | 17:02:09.966 | decode | 7 | BAAB | B | A | 84.0026 |
| 93.2 | 17:02:12.281 | decode | 7 | BAAB | A | A | 86.4545 |
| 93.3 | 17:02:14.595 | decode | 7 | BAAB | A | A | 83.2694 |
| 93.4 | 17:02:16.910 | decode | 7 | BAAB | B | A | 80.1961 |
| 101.1 | 17:03:24.056 | decode | 8 | ABBA | A | A | 80.4150 |
| 101.2 | 17:03:26.371 | decode | 8 | ABBA | B | A | 83.4004 |
| 101.3 | 17:03:28.687 | decode | 8 | ABBA | B | A | 82.3049 |
| 101.4 | 17:03:31.003 | decode | 8 | ABBA | A | A | 83.3673 |
| 102.1 | 17:03:33.320 | encode | 8 | BAAB | B | A | 97.8065 |
| 102.2 | 17:03:35.635 | encode | 8 | BAAB | A | A | 77.4209 |
| 102.3 | 17:03:37.950 | encode | 8 | BAAB | A | A | 101.9866 |
| 102.4 | 17:03:40.265 | encode | 8 | BAAB | B | A | 88.0618 |

The harness saved the A/A forecast at 2026-11-28T17:05:17.604791+00:00. It resampled whole A/A block magnitudes, assigned random signs, added candidate slowdowns and replayed the same fixed two-sided timing test. The first 0.1-percentage-point grid step reaching 80% detection in 2,000 draws is the reported MDE. This is a forecast over repeated runs. The A/B verdict uses its own observed block effects and p-value.

| Operation | A/A log SD × 100 | Slowdown MDE at 12 blocks |
|---|---:|---:|
| encode | 11.937% | 10.8% |
| decode | 4.490% | 5.1% |

## 4. Measure the commit against its parent

Each A slot uses the parent and each B slot uses the commit. All twelve blocks per operation enter the one prespecified timing check.

Actual command for the first slot in this stage

```sh
taskset -c 2 /home/sherman/.sdkman/candidates/java/25.0.1-oracle/bin/java --add-modules=jdk.incubator.vector -jar /home/sherman/work/ai/bitpacking-java-public/benchmarks/results/f61c04c-plain/data/builds/baseline.jar 'io.github.sherman.bitpacking.VectorCodecBenchmark.compress$' -p bits=13 -p mode=0 -p blocks=16 -t 1 -f 1 -wi 2 -w 500ms -i 3 -r 250ms -foe true -rf json -rff /tmp/jmh-f61c-fork-buxmm4he/jmh.json
```

| Global block.slot | Start UTC | Operation | Case block | Order | Label | JAR | ns/op |
|---:|---|---|---:|---|---|---|---:|
| 116.1 | 17:05:45.388 | encode | 1 | ABBA | A | A | 91.1472 |
| 116.2 | 17:05:47.706 | encode | 1 | ABBA | B | B | 81.8004 |
| 116.3 | 17:05:50.021 | encode | 1 | ABBA | B | B | 87.9233 |
| 116.4 | 17:05:52.338 | encode | 1 | ABBA | A | A | 92.0046 |
| 123.1 | 17:06:50.118 | decode | 1 | ABBA | A | A | 91.5892 |
| 123.2 | 17:06:52.433 | decode | 1 | ABBA | B | B | 83.1123 |
| 123.3 | 17:06:54.748 | decode | 1 | ABBA | B | B | 72.7260 |
| 123.4 | 17:06:57.063 | decode | 1 | ABBA | A | A | 84.2890 |
| 135.1 | 17:08:41.056 | encode | 2 | ABBA | A | A | 77.2992 |
| 135.2 | 17:08:43.371 | encode | 2 | ABBA | B | B | 81.8401 |
| 135.3 | 17:08:45.687 | encode | 2 | ABBA | B | B | 67.8458 |
| 135.4 | 17:08:48.002 | encode | 2 | ABBA | A | A | 91.6609 |
| 139.1 | 17:09:18.051 | decode | 2 | BAAB | B | B | 69.1960 |
| 139.2 | 17:09:20.367 | decode | 2 | BAAB | A | A | 77.7160 |
| 139.3 | 17:09:22.683 | decode | 2 | BAAB | A | A | 90.7326 |
| 139.4 | 17:09:24.998 | decode | 2 | BAAB | B | B | 63.5550 |
| 147.1 | 17:10:32.140 | decode | 3 | ABBA | A | A | 84.5805 |
| 147.2 | 17:10:34.455 | decode | 3 | ABBA | B | B | 70.4359 |
| 147.3 | 17:10:36.770 | decode | 3 | ABBA | B | B | 68.5620 |
| 147.4 | 17:10:39.085 | decode | 3 | ABBA | A | A | 90.8017 |
| 149.1 | 17:10:50.661 | encode | 3 | ABBA | A | A | 78.9416 |
| 149.2 | 17:10:52.977 | encode | 3 | ABBA | B | B | 82.9967 |
| 149.3 | 17:10:55.292 | encode | 3 | ABBA | B | B | 82.9119 |
| 149.4 | 17:10:57.607 | encode | 3 | ABBA | A | A | 90.7098 |
| 156.1 | 17:11:55.336 | encode | 4 | BAAB | B | B | 83.2095 |
| 156.2 | 17:11:57.652 | encode | 4 | BAAB | A | A | 92.7678 |
| 156.3 | 17:11:59.967 | encode | 4 | BAAB | A | A | 92.1294 |
| 156.4 | 17:12:02.282 | encode | 4 | BAAB | B | B | 82.4478 |
| 163.1 | 17:13:00.060 | decode | 4 | ABBA | A | A | 82.4098 |
| 163.2 | 17:13:02.375 | decode | 4 | ABBA | B | B | 69.1185 |
| 163.3 | 17:13:04.690 | decode | 4 | ABBA | B | B | 63.5707 |
| 163.4 | 17:13:07.006 | decode | 4 | ABBA | A | A | 82.0042 |
| 171.1 | 17:14:14.048 | encode | 5 | BAAB | B | B | 82.8354 |
| 171.2 | 17:14:16.363 | encode | 5 | BAAB | A | A | 94.3284 |
| 171.3 | 17:14:18.679 | encode | 5 | BAAB | A | A | 92.0375 |
| 171.4 | 17:14:20.994 | encode | 5 | BAAB | B | B | 82.4006 |
| 178.1 | 17:15:18.873 | decode | 5 | BAAB | B | B | 67.7610 |
| 178.2 | 17:15:21.188 | decode | 5 | BAAB | A | A | 75.6578 |
| 178.3 | 17:15:23.503 | decode | 5 | BAAB | A | A | 86.0755 |
| 178.4 | 17:15:25.818 | decode | 5 | BAAB | B | B | 69.4587 |
| 184.1 | 17:16:14.239 | encode | 6 | ABBA | A | A | 98.0989 |
| 184.2 | 17:16:16.555 | encode | 6 | ABBA | B | B | 82.3074 |
| 184.3 | 17:16:18.870 | encode | 6 | ABBA | B | B | 83.0227 |
| 184.4 | 17:16:21.185 | encode | 6 | ABBA | A | A | 99.9607 |
| 192.1 | 17:17:28.328 | decode | 6 | BAAB | B | B | 69.9991 |
| 192.2 | 17:17:30.643 | decode | 6 | BAAB | A | A | 79.9821 |
| 192.3 | 17:17:32.959 | decode | 6 | BAAB | A | A | 83.9344 |
| 192.4 | 17:17:35.274 | decode | 6 | BAAB | B | B | 64.7919 |
| 198.1 | 17:18:23.744 | encode | 7 | BAAB | B | B | 79.1545 |
| 198.2 | 17:18:26.059 | encode | 7 | BAAB | A | A | 90.2576 |
| 198.3 | 17:18:28.374 | encode | 7 | BAAB | A | A | 91.4008 |
| 198.4 | 17:18:30.689 | encode | 7 | BAAB | B | B | 84.0220 |
| 199.1 | 17:18:33.005 | decode | 7 | ABBA | A | A | 82.0055 |
| 199.2 | 17:18:35.320 | decode | 7 | ABBA | B | B | 69.7720 |
| 199.3 | 17:18:37.636 | decode | 7 | ABBA | B | B | 65.5123 |
| 199.4 | 17:18:39.951 | decode | 7 | ABBA | A | A | 84.6923 |
| 216.1 | 17:21:10.249 | decode | 8 | BAAB | B | B | 69.1183 |
| 216.2 | 17:21:12.564 | decode | 8 | BAAB | A | A | 80.5919 |
| 216.3 | 17:21:14.879 | decode | 8 | BAAB | A | A | 85.3236 |
| 216.4 | 17:21:17.194 | decode | 8 | BAAB | B | B | 62.7511 |
| 221.1 | 17:21:56.554 | encode | 8 | ABBA | A | A | 96.8708 |
| 221.2 | 17:21:58.869 | encode | 8 | ABBA | B | B | 82.8560 |
| 221.3 | 17:22:01.185 | encode | 8 | ABBA | B | B | 80.4294 |
| 221.4 | 17:22:03.500 | encode | 8 | ABBA | A | A | 94.1320 |
| 228.1 | 17:23:01.284 | decode | 9 | ABBA | A | A | 87.1330 |
| 228.2 | 17:23:03.599 | decode | 9 | ABBA | B | B | 67.6115 |
| 228.3 | 17:23:05.914 | decode | 9 | ABBA | B | B | 67.7906 |
| 228.4 | 17:23:08.229 | decode | 9 | ABBA | A | A | 79.4443 |
| 236.1 | 17:24:15.321 | encode | 9 | ABBA | A | A | 95.1057 |
| 236.2 | 17:24:17.637 | encode | 9 | ABBA | B | B | 83.2224 |
| 236.3 | 17:24:19.953 | encode | 9 | ABBA | B | B | 80.5252 |
| 236.4 | 17:24:22.268 | encode | 9 | ABBA | A | A | 91.1298 |
| 243.1 | 17:25:20.003 | decode | 10 | ABBA | A | A | 82.5895 |
| 243.2 | 17:25:22.318 | decode | 10 | ABBA | B | B | 68.2639 |
| 243.3 | 17:25:24.633 | decode | 10 | ABBA | B | B | 69.1485 |
| 243.4 | 17:25:26.949 | decode | 10 | ABBA | A | A | 83.1895 |
| 247.1 | 17:25:57.047 | encode | 10 | BAAB | B | B | 82.1318 |
| 247.2 | 17:25:59.362 | encode | 10 | BAAB | A | A | 103.4672 |
| 247.3 | 17:26:01.677 | encode | 10 | BAAB | A | A | 90.0083 |
| 247.4 | 17:26:03.992 | encode | 10 | BAAB | B | B | 83.1115 |
| 255.1 | 17:27:11.035 | encode | 11 | ABBA | A | A | 80.3699 |
| 255.2 | 17:27:13.401 | encode | 11 | ABBA | B | B | 82.0304 |
| 255.3 | 17:27:15.716 | encode | 11 | ABBA | B | B | 82.0838 |
| 255.4 | 17:27:18.031 | encode | 11 | ABBA | A | A | 96.7884 |
| 256.1 | 17:27:20.346 | decode | 11 | ABBA | A | A | 81.4250 |
| 256.2 | 17:27:22.661 | decode | 11 | ABBA | B | B | 70.9843 |
| 256.3 | 17:27:24.976 | decode | 11 | ABBA | B | B | 77.5563 |
| 256.4 | 17:27:27.292 | decode | 11 | ABBA | A | A | 84.3293 |
| 270.1 | 17:29:29.905 | decode | 12 | ABBA | A | A | 80.5660 |
| 270.2 | 17:29:32.220 | decode | 12 | ABBA | B | B | 72.0791 |
| 270.3 | 17:29:34.535 | decode | 12 | ABBA | B | B | 70.5620 |
| 270.4 | 17:29:36.850 | decode | 12 | ABBA | A | A | 82.4328 |
| 276.1 | 17:30:25.424 | encode | 12 | ABBA | A | A | 89.7406 |
| 276.2 | 17:30:27.739 | encode | 12 | ABBA | B | B | 82.1778 |
| 276.3 | 17:30:30.055 | encode | 12 | ABBA | B | B | 84.2149 |
| 276.4 | 17:30:32.370 | encode | 12 | ABBA | A | A | 89.9323 |

## 5. Collect CPU counters and allocation in separate forks

The runner adds the perfnorm and gc profilers. Cycles, instructions and allocated bytes are per 16-block JMH operation.

Actual command for the first slot in this stage

```sh
taskset -c 2 /home/sherman/.sdkman/candidates/java/25.0.1-oracle/bin/java --add-modules=jdk.incubator.vector -jar /home/sherman/work/ai/bitpacking-java-public/benchmarks/results/f61c04c-plain/data/builds/candidate.jar 'io.github.sherman.bitpacking.VectorCodecBenchmark.decompress$' -p bits=13 -p mode=0 -p blocks=16 -t 1 -f 1 -wi 2 -w 500ms -i 3 -r 250ms -foe true -rf json -rff /tmp/jmh-f61c-fork-buxmm4he/jmh.json -prof perfnorm:events=cycles,instructions -prof gc
```

| Global block.slot | Start UTC | Operation | Case block | Order | Label | JAR | ns/op | Cycles/op | Instructions/op | Allocated B/op |
|---:|---|---|---:|---|---|---|---:|---:|---:|---:|
| 288.1 | 17:32:20.231 | decode | 1 | BAAB | B | B | 69.2306 | 375.17288 | 2216.20760 | 0.03046 |
| 288.2 | 17:32:22.646 | decode | 1 | BAAB | A | A | 84.1404 | 455.94074 | 2431.31692 | 0.03367 |
| 288.3 | 17:32:25.111 | decode | 1 | BAAB | A | A | 84.9705 | 458.72676 | 2432.18100 | 0.03801 |
| 288.4 | 17:32:27.578 | decode | 1 | BAAB | B | B | 64.7787 | 351.02523 | 2218.35384 | 0.02881 |
| 291.1 | 17:32:49.417 | encode | 1 | BAAB | B | B | 73.1442 | 395.87329 | 2325.23369 | 0.02913 |
| 291.2 | 17:32:51.842 | encode | 1 | BAAB | A | A | 80.2101 | 430.67384 | 2492.94280 | 0.03426 |
| 291.3 | 17:32:54.258 | encode | 1 | BAAB | A | A | 89.5098 | 484.54041 | 2500.94343 | 0.03633 |
| 291.4 | 17:32:56.723 | encode | 1 | BAAB | B | B | 82.9481 | 448.27514 | 2323.02880 | 0.03570 |
| 295.1 | 17:33:28.588 | decode | 2 | ABBA | A | A | 79.5161 | 428.97551 | 2444.06506 | 0.03778 |
| 295.2 | 17:33:31.054 | decode | 2 | ABBA | B | B | 70.0286 | 375.94244 | 2229.17283 | 0.03050 |
| 295.3 | 17:33:33.520 | decode | 2 | ABBA | B | B | 69.4904 | 373.52460 | 2207.25794 | 0.02962 |
| 295.4 | 17:33:35.935 | decode | 2 | ABBA | A | A | 96.2586 | 519.13984 | 2430.30809 | 0.04321 |
| 301.1 | 17:34:27.363 | encode | 2 | BAAB | B | B | 83.2938 | 461.70474 | 2326.06238 | 0.03408 |
| 301.2 | 17:34:29.778 | encode | 2 | BAAB | A | A | 108.0427 | 587.96617 | 2527.41955 | 0.04075 |
| 301.3 | 17:34:32.244 | encode | 2 | BAAB | A | A | 98.2862 | 533.53624 | 2550.81335 | 0.04188 |
| 301.4 | 17:34:34.710 | encode | 2 | BAAB | B | B | 82.7649 | 450.59041 | 2309.21647 | 0.03559 |
| 314.1 | 17:36:34.494 | decode | 3 | BAAB | B | B | 69.6586 | 375.89744 | 2205.82453 | 0.02768 |
| 314.2 | 17:36:36.910 | decode | 3 | BAAB | A | A | 83.9831 | 450.68294 | 2426.59896 | 0.03755 |
| 314.3 | 17:36:39.325 | decode | 3 | BAAB | A | A | 92.2059 | 500.94394 | 2436.64183 | 0.03879 |
| 314.4 | 17:36:41.790 | decode | 3 | BAAB | B | B | 68.6982 | 371.68738 | 2216.42474 | 0.02849 |
| 319.1 | 17:37:23.556 | encode | 3 | ABBA | A | A | 95.7790 | 516.07950 | 2485.88893 | 0.03871 |
| 319.2 | 17:37:25.972 | encode | 3 | ABBA | B | B | 85.6139 | 464.43789 | 2340.30677 | 0.03510 |
| 319.3 | 17:37:28.437 | encode | 3 | ABBA | B | B | 81.9508 | 446.99093 | 2318.07731 | 0.03553 |
| 319.4 | 17:37:30.903 | encode | 3 | ABBA | A | A | 82.9358 | 445.57232 | 2503.64226 | 0.03485 |
| 325.1 | 17:38:22.279 | encode | 4 | ABBA | A | A | 90.8438 | 489.97142 | 2500.90328 | 0.03841 |
| 325.2 | 17:38:24.695 | encode | 4 | ABBA | B | B | 84.4186 | 451.98618 | 2302.36981 | 0.03233 |
| 325.3 | 17:38:27.160 | encode | 4 | ABBA | B | B | 85.0057 | 456.21003 | 2299.54846 | 0.03658 |
| 325.4 | 17:38:29.626 | encode | 4 | ABBA | A | A | 89.9634 | 488.46102 | 2505.30958 | 0.03747 |
| 332.1 | 17:39:30.848 | decode | 4 | BAAB | B | B | 66.1381 | 357.27392 | 2194.47823 | 0.02868 |
| 332.2 | 17:39:33.264 | decode | 4 | BAAB | A | A | 84.7802 | 462.77275 | 2465.48664 | 0.03633 |
| 332.3 | 17:39:35.729 | decode | 4 | BAAB | A | A | 86.3736 | 467.54716 | 2427.40926 | 0.04083 |
| 332.4 | 17:39:38.145 | decode | 4 | BAAB | B | B | 68.1249 | 367.10136 | 2200.65472 | 0.02883 |

## 6. Recompute the timing verdicts

For each four-fork block the harness computed `d = (ln B1 + ln B2 − ln A1 − ln A2) / 2`. The change in percent is `100 × expm1(mean(d))`. Each block has equal weight. The two-sided p-value compares the absolute observed sum with all 4,096 sign assignments of the twelve block effects. Inverting that test gives the 95% interval for a common multiplicative timing change.

The first encode block was global block 116 (ABBA). The parent forks took 91.1472 and 92.0046 ns/op. The commit forks took 81.8004 and 87.9233 ns/op. The ratio of the two geometric means was 0.926090, giving a block change of −7.39%.

The first decode block was global block 123 (ABBA). The parent forks took 91.5892 and 84.2890 ns/op. The commit forks took 83.1123 and 72.7260 ns/op. The ratio of the two geometric means was 0.884850, giving a block change of −11.51%.

| Operation | Global block | Case block | Order | Block log ratio | Block time change |
|---|---:|---:|---|---:|---:|
| encode | 116 | 1 | ABBA | -0.07678403 | −7.39% |
| decode | 123 | 1 | ABBA | -0.12233682 | −11.51% |
| encode | 135 | 2 | ABBA | -0.12188703 | −11.48% |
| decode | 139 | 2 | BAAB | -0.23606490 | −21.03% |
| decode | 147 | 3 | ABBA | -0.23197003 | −20.70% |
| encode | 149 | 3 | ABBA | -0.01989709 | −1.97% |
| encode | 156 | 4 | BAAB | -0.10988352 | −10.41% |
| decode | 163 | 4 | ABBA | -0.21524956 | −19.37% |
| encode | 171 | 5 | BAAB | -0.12026465 | −11.33% |
| decode | 178 | 5 | BAAB | -0.16236292 | −14.99% |
| encode | 184 | 6 | ABBA | -0.18058858 | −16.52% |
| decode | 192 | 6 | BAAB | -0.19608807 | −17.81% |
| encode | 198 | 7 | BAAB | -0.10772044 | −10.21% |
| decode | 199 | 7 | ABBA | -0.20916974 | −18.87% |
| decode | 216 | 8 | BAAB | -0.23042644 | −20.58% |
| encode | 221 | 8 | ABBA | -0.15679532 | −14.51% |
| decode | 228 | 9 | ABBA | -0.20614514 | −18.63% |
| encode | 236 | 9 | ABBA | -0.12859286 | −12.07% |
| decode | 243 | 10 | ABBA | -0.18768351 | −17.11% |
| encode | 247 | 10 | BAAB | -0.15532386 | −14.39% |
| encode | 255 | 11 | ABBA | -0.07216775 | −6.96% |
| decode | 256 | 11 | ABBA | -0.11047396 | −10.46% |
| decode | 270 | 12 | ABBA | -0.13340197 | −12.49% |
| encode | 276 | 12 | ABBA | -0.07686230 | −7.40% |

**Encode** · IMPROVED. Parent 5.718 ns/codec block, commit 5.119. The time change was −10.47%, 95% CI −12.93% to −7.85%. 2 of 4,096 assignments were at least as extreme (p = 0.000488). Cycles/op changed −9.79%, instructions/op −7.59%, and allocation -0.00358 B/op.

**Decode** · IMPROVED. Parent 5.212 ns/codec block, commit 4.324. The time change was −17.04%, 95% CI −19.41% to −14.63%. 2 of 4,096 assignments were at least as extreme (p = 0.000488). Cycles/op changed −21.18%, instructions/op −9.26%, and allocation -0.00914 B/op.

[All-width report](index.html) · [Schedule](data/plan.json) · [All raw fork results](data/forks.jsonl) · [Full JMH console logs](data/console.txt.gz)
