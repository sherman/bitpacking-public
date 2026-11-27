# BitPacking Java

BitPacking Java provides a static API to compress blocks of 128 unsigned 32-bit
integers. It uses a sixteen-stream format with bit widths from 1 to 32. Each
block takes `16 * bits` bytes.

Use JDK 25 and launch your application with `--add-modules=jdk.incubator.vector`.

```java
int bits = BitPacker16x.numBits(values, 0);
byte[] packed = new byte[BitPacker16x.compressedBlockSize(bits)];
BitPacker16x.compress(values, 0, packed, 0, bits);

int[] decoded = new int[BitPacker16x.BLOCK_LEN];
BitPacker16x.decompress(packed, 0, decoded, 0, bits);
```

Import `io.github.sherman.bitpacking.BitPacker16x` to use the example above.
Offsets count elements in the corresponding array. Compression returns the
number of bytes written, and decompression returns the number of bytes read.
Store the bit width and any initial value separately.

## Operation families

| Methods | Input | Encoded value | Convenience initial value |
|---|---|---|---|
| `numBits`, `compress`, `decompress` | Arbitrary unsigned integers | Value itself | None |
| `*Sorted` | Unsigned nondecreasing values | `value - previous` | `0` |
| `*StrictlySorted` | Unsigned strictly increasing values | `value - previous - 1` | `-1` (no preceding value) |

When chaining sorted blocks, pass the last value of the previous block as the
initial value for width detection, compression, and decompression. Arithmetic
wraps modulo 2^32. The API checks the width and buffer ranges before writing.
The caller must ensure the values or deltas fit that width and the input is in
the required order.

`numBits*` always returns at least `1`, so a block of zeros takes 16 bytes.
Constant sorted values also take 16 bytes when the initial value equals the
first value. Consecutive strictly sorted values take 16 bytes when the initial
value immediately precedes the first. Passing width `0` throws
`IllegalArgumentException`.

## Backend selection

`BitPacker16x` selects its backend at class initialization. It uses 512-bit
vector kernels when the JVM's preferred integer vector size is at least 512
bits and scalar kernels otherwise. Set `-Dbitpacking.scalar=true` at startup
to force the scalar backend. Both backends produce identical bytes.

## Build and test

```sh
mvn verify
mvn -Dbitpacking.scalar=true test
mvn -Pbenchmarks verify
```

## Benchmarks

Hardware: AMD Ryzen 9 9900X3D (12 cores / 24 threads, AVX-512,
128 MiB L3 cache), 64 GB RAM.

Results below measure the vector kernels with one 128-integer block,
averaged equally across widths 1, 3, 8, 13, 16, 24, and 31.
Positive random inputs are regenerated before each iteration, outside timing;
buffers are reused. Throughput is `128 × 10⁹ / average ns/op`.

### Plain mode

| Operation | Average ns/op | Billion integers/s |
|---|---:|---:|
| Compress | 5.251474 | 24.374108 |
| Decompress | 4.586427 | 27.908435 |

### Sorted mode (deltas)

| Operation | Average ns/op | Billion integers/s |
|---|---:|---:|
| Compress | 5.754198 | 22.244628 |
| Decompress | 9.886175 | 12.947374 |

All suites call kernels directly with range checks and support `detect`,
`compress`, `decompress`, and `detectAndCompress`.

| Suite | Backend |
|---|---|
| `ScalarCodecBenchmark` | Always `ScalarKernels` |
| `VectorCodecBenchmark` | Always `Vector512Kernels`; requires preferred vector size ≥512 bits |

| Parameter | Meaning |
|---|---|
| `bits` | Width 1 to 31; `-1` cycles through 1, 3, 8, 13, 16, 24, 31 across blocks |
| `mode` | `0`: plain; `1`: sorted; `2`: strictly sorted |
| `blocks` | Number of 128-integer blocks processed by one benchmark operation |

Build with `mvn -Pbenchmarks verify`, then run:

```sh
JAR=target/bitpacking-java-0.1.0-SNAPSHOT-benchmarks.jar
java --add-modules=jdk.incubator.vector -jar "$JAR" \
  'VectorCodecBenchmark.(compress|decompress)$' \
  -p bits=1,3,8,13,16,24,31 -p mode=0,1 -p blocks=1 -f 10
```
