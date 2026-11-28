# 16-bit plain encode and decode

The parent (476bd9f4) and commit (f61c04c2) ran the same JMH method and processed 16 codec blocks of 128 integers per operation. The follow-up used fresh profiled forks on CPU 2 with the performance governor. Every case had eight independently randomized ABBA or BAAB blocks. The original unprofiled timing endpoint used twelve blocks per case.

| Operation | Original time/op change | 95% timing interval | Timing verdict |
|---|---:|---:|---|
| [Encode](encode.html) | +10.74% | +6.56% to +15.05% | REGRESSED |
| [Decode](decode.html) | +1.41% | −1.17% to +4.04% | INCONCLUSIVE |

## Fresh profiler results

| Case | Profiled metric | Change | Individual 95% interval | Raw p | Holm p | Direction at 5% |
|---|---|---:|---:|---:|---:|---|
| Encode | Cycles/op | +13.81% | +6.98% to +21.48% | 0.0078125 | 0.023438 | increase |
| Encode | Instructions/op | +10.46% | +9.88% to +11.02% | 0.0078125 | 0.023438 | increase |
| Encode | Allocated heap | +0.003462 B/op | +0.002067 B/op to +0.004933 B/op | 0.0078125 | 0.023438 | increase |
| Decode | Cycles/op | +3.05% | −2.55% to +10.43% | 0.33594 | 1 | Inconclusive |
| Decode | Instructions/op | +0.21% | −0.41% to +0.89% | 0.45312 | 1 | Inconclusive |
| Decode | Allocated heap | +0.0007296 B/op | -0.0009946 B/op to +0.002642 B/op | 0.35938 | 1 | Inconclusive |

Each counter uses a two-sided sign-flip test. Holm adjustment covers three counters within the same operation. CPU intervals use a common multiplicative change, and allocation intervals use an additive change in B/op. A positive estimate indicates a larger value on the commit. The width-specific 16-bit plain methods have identical source in both revisions. The measurements compare the complete JARs.

## Sensitivity at eight blocks

Before the first candidate fork, the controller measured eight profiled parent-versus-parent blocks for each operation. It resampled complete three-counter block vectors 2,000 times. Each trial injected an increase into one counter and tested whether Holm identified it in the correct direction. The table gives the first tested grid point reaching 80% detection. Estimates depend on this A/A sample.

| Operation | Cycles/op | Instructions/op | Allocated heap | Any false flag with zero shift |
|---|---:|---:|---:|---:|
| Encode | 20% | 2% | 0.01 B/op | 2.80% |
| Decode | 10% | 2% | 0.01 B/op | 2.95% |

## Independent JMH controls

The arithmetic fixture increased dependent rounds from 64 to 128. The allocation fixture increased the size of an escaping array from 16 to 272 bytes, a known change of +256 B/op.

| Case | Profiled metric | Change | Individual 95% interval | Raw p | Holm p | Direction at 5% |
|---|---|---:|---:|---:|---:|---|
| CPU | Cycles/op | +99.97% | +99.81% to +100.17% | 0.0078125 | 0.023438 | increase |
| CPU | Instructions/op | +94.41% | +94.26% to +94.57% | 0.0078125 | 0.023438 | increase |
| CPU | Allocated heap | +0.001665 B/op | +0.001654 B/op to +0.001675 B/op | 0.0078125 | 0.023438 | increase |
| Allocation | Cycles/op | +26.50% | +25.89% to +27.12% | 0.0078125 | 0.023438 | increase |
| Allocation | Instructions/op | +47.45% | +46.98% to +47.88% | 0.0078125 | 0.023438 | increase |
| Allocation | Allocated heap | +256 B/op | +256 B/op to +256 B/op | 0.0078125 | 0.023438 | increase |

The arithmetic loop creates no objects in its Java body, yet its measured heap difference was +0.001665 B/op. JMH normalized heap bytes include activity outside the benchmark method. A small B/op change alone cannot identify an allocation site.

## Evidence and reproduction

The follow-up comprises 64 profiled A/A forks, 64 codec A/B forks and 64 positive-control forks. The console and raw records contain the command for every slot. Replaying the report checks the frozen plan, fork order, governor boundaries, JAR hashes, and forecasts, and independently enumerates signs to audit all counter p-values and interval boundaries.

[Original timing study](../f61c04c-plain/README.md) · [Plan](data/plan.json) · [Frozen sensitivity](data/calibration.json) · [Raw fork records](data/forks.jsonl) · [Console](data/console.txt.gz) · [Case and control estimates](data/analysis.json) · [Measured control JAR](data/controls.jar)

From the repository root, replay the saved study with

```sh
study=benchmarks/results/f61c04c-profile-16
python3 "$study/report.py" "$study"
```

For a fresh run, use a new output directory, the archived codec JARs and a JMH 1.37 annotation processor.

```sh
repeat=$(mktemp -d)
python3 "$study/collect.py" --output "$repeat/data" --processor "$JMH_PROCESSOR"
python3 "$study/report.py" "$repeat"
```
