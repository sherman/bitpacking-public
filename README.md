# BitPacking Java

BitPacking Java provides a static API to compress blocks of 128 unsigned 32-bit
integers. It uses a sixteen-stream format with bit widths from 1 to 32. Each
block takes `16 * bits` bytes.

You need Java 25 and `--add-modules=jdk.incubator.vector` to run it.

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

## Structure

| Class | Responsibility |
|---|---|
| `BitPacker16x` | Public API, argument validation, and backend selection |
| `ScalarKernels` | Scalar width detection, packing, and unpacking |
| `Vector512Kernels` | The same operations using generated 512-bit SIMD kernels |

Both backends have `numBits`, `pack`, and `unpack` methods. The scalar backend
uses the same packing and unpacking loops for every width, including 32. The
vector backend has a kernel for each width. The public API checks the bit width
and input/output ranges before writing to either array.

| JVM configuration | Backend for every operation |
|---|---|
| Preferred vector size at least 512 bits | `Vector512Kernels` |
| Preferred vector size below 512 bits | `ScalarKernels` |
| `-Dbitpacking.scalar=true` | `ScalarKernels` |

`BitPacker16x` selects a backend once, when the class is initialized. Both
backends produce identical bytes.

## Operation families

| Methods | Input | Encoded value | Convenience initial value |
|---|---|---|---|
| `numBits`, `compress`, `decompress` | Arbitrary unsigned integers | Value itself | None |
| `*Sorted` | Unsigned nondecreasing values | `value - previous` | `0` |
| `*StrictlySorted` | Unsigned strictly increasing values | `value - previous - 1` | `-1` (no preceding value) |

When chaining sorted blocks, pass the last value of the previous block as the
initial value. Arithmetic wraps modulo 2^32. Make sure the values or deltas fit
the supplied width and the input is in the required order. Compression does
not check these conditions.

`numBits*` always returns at least `1`, so a block of zeros takes 16 bytes.
The same applies to constant sorted values with a matching initial value and
consecutive strictly sorted values: each uses one bit per value. Passing width
`0` throws `IllegalArgumentException`.

## Compatibility

The static method signatures and byte format for widths 1 to 32 are unchanged.
Replace `BitPackingCodec.BITPACKER16X.method(...)` calls with
`BitPacker16x.method(...)`. The enum facade and zero-width decoding have been
removed. Migrate any stored zero-width blocks before using this version.

## Build and test

```sh
mvn verify
mvn -Dbitpacking.scalar=true test
mvn -Pbenchmarks verify
```

## Benchmarks

The benchmark profile produces `target/bitpacking-java-0.1.0-SNAPSHOT-benchmarks.jar`.
Run it with `java --add-modules=jdk.incubator.vector -jar <jar>`. Each operation
processes the whole working set. To convert ns/op to ns/block, divide the result
by `blocks`.

The two benchmark classes are standalone, with their own setup and benchmark
methods. They use identical inputs, parameters, and workloads, and call their
kernels directly with the same range checks:

| Suite | Backend |
|---|---|
| `ScalarCodecBenchmark` | Always `ScalarKernels` |
| `VectorCodecBenchmark` | Always `Vector512Kernels`; requires preferred vector size ≥512 bits |

Select a suite by name. Both have `detect`, `compress`, `decompress`, and
`detectAndCompress` operations. The old `backend` parameter is no longer supported.

| Parameter | Meaning |
|---|---|
| `bits` | Width 1 to 32; `-1` cycles through 1, 3, 8, 13, 16, 24, 31, 32 across blocks |
| `mode` | `0`: plain; `1`: sorted; `2`: strictly sorted |
| `blocks` | Number of 128-integer blocks processed by one benchmark operation |

For example:

```sh
JAR=target/bitpacking-java-0.1.0-SNAPSHOT-benchmarks.jar
java --add-modules=jdk.incubator.vector -jar "$JAR" \
  'ScalarCodecBenchmark.*' -p bits=13 -p mode=0 -p blocks=1 -f 1
java --add-modules=jdk.incubator.vector -jar "$JAR" \
  'VectorCodecBenchmark.*' -p bits=13 -p mode=0 -p blocks=1 -f 1
```

The default is zero forks. Use `-f 1`, as in the examples, to measure in a fresh
JVM. These benchmarks call the kernels directly, so public API backend selection
and `bitpacking.scalar` have no effect.
