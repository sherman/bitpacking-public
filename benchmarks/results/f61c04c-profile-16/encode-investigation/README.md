# Why plain 16-bit encode executes more instructions

On Oracle HotSpot 25.0.1+8 and an AMD Ryzen 9 9900X3D, commit `f61c04c2` pushed the unchanged 16-bit packing method past C2’s size limit for hot inlining. C2 emitted a separate call for every 128 integers. The extra call and its surrounding code account for 208 instructions per JMH operation. The original hardware-counter measurement found 206.7 extra instructions per operation, a +10.46% change (95% CI +9.88% to +11.02%). [Original profile](../README.md)

## What changed in the archived JARs

The source for `pack0_16` is identical in both revisions. Other kernels in the same class changed the constant pool. Before the entry for the integer mask `65535`, the commit has 30 entries unique to its pool and the parent has 18 unique to its pool. That net shift of 12 moved the mask from index 253 to 265. The eight loads of the mask therefore need a two-byte pool index. The saved disassembly includes every affected offset and the constant-pool entries behind the shift. [Bytecode and compiler evidence](data/compiler-evidence.txt)

| | Parent `476bd9f4` | Commit `f61c04c2` |
|---|---:|---:|
| Mask load, repeated eight times | `ldc #253` | `ldc_w #265` |
| Bytecode instructions in `pack0_16` | 157 | 157 |
| Bytecode size of `pack0_16` | 325 bytes | 333 bytes |
| C2 decision at `pack0 → pack0_16` | `inline (hot)` | `hot method too big` |
| Native instructions per codec block on the hot path | 60 | 15 in the caller + 58 in the callee = 73 |

On this JVM, `FreqInlineSize` defaults to 325 bytes. C2 accepts a hot method at the limit and rejects an oversized one at this call site. Inlining put the vector operations directly into the parent’s dispatcher. The commit executes the same sequence of vector operations with an extra stack frame, argument move, call and return handling. The instruction counts follow the ordinary width-16 path through each method’s first return. The benchmark encodes 16 blocks per JMH operation, so the predicted increase is (73 − 60) × 16 = 208. The original profiler’s arithmetic means were 1,976.3 and 2,183.0 instructions/op. [JVM constant-load formats](https://docs.oracle.com/javase/specs/jvms/se25/html/jvms-6.html#jvms-6.5.ldc_w) · [C2 size check](https://github.com/openjdk/jdk25u/blob/jdk-25.0.1%2B8/src/hotspot/share/opto/bytecodeInfo.cpp#L155-L179)

## Reversing the decision

Each comparison used the same JAR on both sides. A compiler directive changed only whether C2 inlines `pack0_16` while compiling `pack0`. Both comparisons used eight randomly ordered ABBA or BAAB blocks on CPU 2, for 64 fresh JVM forks in total. All 128 governor checks read `performance`. Four separate default diagnostic forks and one per override captured C2’s decisions and assembly. Both overrides produced the opposite revision’s native instruction count per block. [Frozen plan](data/plan.json) · [C2 log excerpts and assembly](data/compiler-evidence.txt)

| JAR and intervention | Default instructions/op | With intervention | Paired change | Individual 95% CI | Holm p |
|---|---:|---:|---:|---:|---:|
| Parent, forbid inlining | 1,974.0 | 2,191.5 | +11.02% | +10.43% to +11.47% | 0.02344 |
| Commit, force inlining | 2,181.4 | 1,980.3 | −9.22% | −9.67% to −8.73% | 0.02344 |

The absolute columns are arithmetic means of 16 forks per condition. The percentage and interval use the harness’s paired block log ratios. The raw two-sided sign-flip p-value was 0.0078125 for instructions in each comparison. Holm adjustment covers the three profiled metrics within each comparison. All three counters and their intervals were independently verified from the raw fork records. [Counter results](data/analysis.json) · [Raw forks](data/forks.jsonl) · [JMH console log](data/console.txt.gz)

## Reproduce

From this directory, run `python3 -B analyze.py` to audit the saved data. On the original measurement host, `python3 -B collect.py /tmp/encode16-repeat` repeats the intervention campaign in a new directory. Use `python3 -B analyze.py /tmp/encode16-repeat` to analyze that run. The archived JARs and their hashes are recorded in the plan.

To inspect the compiler decision on an individual fork, use the Java command recorded in its JSON row with a fresh JMH output path. Add the following JVM options before `-jar`. These enable compilation logging and disassembly. The saved diagnostic captures used the Intel disassembler and also printed the enclosing benchmark methods.

```
-XX:+LogCompilation
-XX:LogFile=/tmp/encode16-hotspot-%p.xml
-XX:PrintAssemblyOptions=intel
-XX:CompileCommand=print,io.github.sherman.bitpacking.Vector512Kernels::pack0
-XX:CompileCommand=print,io.github.sherman.bitpacking.Vector512Kernels::pack0_16
```

Read the C2 compilation of `pack0` at bytecode offset 342 in the fork JVM. The C2 compile ID in the disassembly identifies the matching XML task. The original benchmark, the recorded JAR hashes and the directives are sufficient to repeat both interventions. [Forbid inlining](noinline.json) · [Force inlining](inline.json)
