package io.github.sherman.bitpacking;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Results are ns per entire working set; divide by blocks to obtain ns/block.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 3, jvmArgsAppend = {"--add-modules=jdk.incubator.vector", "-Xms512m", "-Xmx1g"})
public class CodecBenchmark {

    @Param({"0", "1", "3", "8", "13", "16", "24", "31", "32", "-1"})
    public int bits;

    @Param({"0", "1", "2"})
    public int mode;

    @Param({"1", "64", "262144"})
    public int blocks;

    @Param({"vector", "scalar"})
    public String backend;

    private int[] values;
    private int[] decoded;
    private byte[] compressed;
    private int[] widths;
    private int[] offsets;
    private boolean scalar;

    @Setup(Level.Trial)
    public void setup() {
        if (!System.getProperty("bitpacking.benchmark.storage", "byte").equals("byte")) {
            throw new IllegalArgumentException("Word-storage experiments were removed; use their archived benchmark JARs");
        }
        scalar = backend.equals("scalar");
        if (!backend.equals("vector") && !scalar) {
            throw new IllegalArgumentException("Unknown backend: " + backend);
        }
        if (!Boolean.parseBoolean(System.getProperty("bitpacking.benchmark.codec16", "true"))) {
            throw new IllegalArgumentException("Four-stream benchmarks were removed; use their archived JARs");
        }
        values = new int[blocks * 128];
        decoded = new int[values.length];
        compressed = new byte[blocks * 512];
        widths = new int[blocks];
        offsets = new int[blocks];
        int[] mixed = {1, 3, 8, 13, 16, 24, 31, 32};
        int state = 0x12345678;
        int offset = 0;
        for (int block = 0; block < blocks; block++) {
            int b = bits == -1 ? mixed[block % mixed.length] : bits;
            widths[block] = b;
            offsets[block] = offset;
            int mask = (int) ((1L << b) - 1);
            int previous = 0;
            for (int i = 0; i < 128; i++) {
                state ^= state << 13;
                state ^= state >>> 17;
                state ^= state << 5;
                int d = state & mask;
                if (mode != 0)
                    d = i == 0 ? (b == 0 ? 0 : 1 << (b - 1)) : d & 1023;
                if (mode == 0 && i == 0)
                    d = mask;
                int value = mode == 0 ? d : previous + d + (mode == 2 ? 1 : 0);
                values[block * 128 + i] = value;
                previous = value;
            }
            compressBlock(block, b);
            decompressBlock(block, b);
            if (width(block) != b)
                throw new AssertionError("Invalid benchmark bit width");
            for (int i = 0; i < 128; i++) {
                if (values[block * 128 + i] != decoded[block * 128 + i]) {
                    throw new AssertionError("Invalid benchmark data");
                }
            }
            offset += b * 16;
        }
    }

    @Benchmark
    public int detect() {
        int sum = 0;
        for (int block = 0; block < blocks; block++)
            sum += width(block);
        return sum;
    }

    @Benchmark
    public void compress(Blackhole blackhole) {
        for (int block = 0; block < blocks; block++)
            compressBlock(block, widths[block]);
        blackhole.consume(compressed);
    }

    @Benchmark
    public void decompress(Blackhole blackhole) {
        for (int block = 0; block < blocks; block++)
            decompressBlock(block, widths[block]);
        blackhole.consume(decoded);
    }

    @Benchmark
    public void detectAndCompress(Blackhole blackhole) {
        for (int block = 0; block < blocks; block++) {
            compressBlock(block, width(block));
        }
        blackhole.consume(compressed);
    }

    private int width(int block) {
        int offset = block * 128;
        if (scalar) {
            Objects.checkFromIndexSize(offset, 128, values.length);
            return ScalarSupport.numBits(values, offset, mode, 0);
        }
        return switch (mode) {
            case 0 -> BitPacker16x.numBits(values, offset);
            case 1 -> BitPacker16x.numBitsSorted(0, values, offset);
            case 2 -> BitPacker16x.numBitsStrictlySorted(0, values, offset);
            default -> throw new AssertionError();
        };
    }

    private void compressBlock(int block, int b) {
        int offset = block * 128;
        int dest = offsets[block];
        if (scalar) {
            Objects.checkFromIndexSize(offset, 128, values.length);
            Objects.checkFromIndexSize(dest, BitPacker16x.compressedBlockSize(b), compressed.length);
            Scalar16Kernels.pack(values, offset, compressed, dest, b, mode, 0);
        } else {
            switch (mode) {
                case 0 -> BitPacker16x.compress(values, offset, compressed, dest, b);
                case 1 -> BitPacker16x.compressSorted(0, values, offset, compressed, dest, b);
                case 2 -> BitPacker16x.compressStrictlySorted(0, values, offset, compressed, dest, b);
                default -> throw new AssertionError();
            }
        }
    }

    private void decompressBlock(int block, int b) {
        int offset = offsets[block];
        int dest = block * 128;
        if (scalar) {
            Objects.checkFromIndexSize(offset, BitPacker16x.compressedBlockSize(b), compressed.length);
            Objects.checkFromIndexSize(dest, 128, decoded.length);
            Scalar16Kernels.unpack(compressed, offset, decoded, dest, b, mode, 0);
        } else {
            switch (mode) {
                case 0 -> BitPacker16x.decompress(compressed, offset, decoded, dest, b);
                case 1 -> BitPacker16x.decompressSorted(0, compressed, offset, decoded, dest, b);
                case 2 -> BitPacker16x.decompressStrictlySorted(0, compressed, offset, decoded, dest, b);
                default -> throw new AssertionError();
            }
        }
    }
}
