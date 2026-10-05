package io.github.sherman.bitpacking;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Measures the 512-bit vector backend directly, with API-equivalent range checks.
 * Results are ns per entire working set; divide by blocks to obtain ns/block.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 0, jvmArgsAppend = {"--add-modules=jdk.incubator.vector", "-Xms512m", "-Xmx1g"})
public class VectorCodecBenchmark {

    @Param({"1", "3", "8", "13", "16", "24", "31", "32", "-1"})
    public int bits;

    @Param({"0", "1", "2"})
    public int mode;

    @Param({"1", "64", "262144"})
    public int blocks;

    private int[] values;
    private int[] decoded;
    private byte[] compressed;
    private int[] widths;
    private int[] offsets;

    @Setup(Level.Trial)
    public void setup() {
        if (!System.getProperty("bitpacking.benchmark.storage", "byte").equals("byte")) {
            throw new IllegalArgumentException("Word-storage experiments were removed; use their archived benchmark JARs");
        }

        if (!Boolean.parseBoolean(System.getProperty("bitpacking.benchmark.codec16", "true"))) {
            throw new IllegalArgumentException("Four-stream benchmarks were removed; use their archived JARs");
        }
        values = new int[blocks * BitPacker16x.BLOCK_LEN];
        decoded = new int[values.length];
        compressed = new byte[blocks * BitPacker16x.MAX_COMPRESSED_BYTES];
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
            for (int i = 0; i < BitPacker16x.BLOCK_LEN; i++) {
                state ^= state << 13;
                state ^= state >>> 17;
                state ^= state << 5;
                int d = state & mask;
                if (mode != 0) {
                    d = i == 0 ? (1 << (b - 1)) : d & 1023;
                }
                if (mode == 0 && i == 0) {
                    d = mask;
                }
                int value = mode == 0 ? d : previous + d + (mode == 2 ? 1 : 0);
                values[block * BitPacker16x.BLOCK_LEN + i] = value;
                previous = value;
            }
            compressBlock(block, b);
            decompressBlock(block, b);
            if (width(block) != b) {
                throw new AssertionError("Invalid benchmark bit width");
            }
            for (int i = 0; i < BitPacker16x.BLOCK_LEN; i++) {
                if (values[block * BitPacker16x.BLOCK_LEN + i] != decoded[block * BitPacker16x.BLOCK_LEN + i]) {
                    throw new AssertionError("Invalid benchmark data");
                }
            }
            offset += BitPacker16x.compressedBlockSize(b);
        }
    }

    @Benchmark
    public int detect() {
        int sum = 0;
        for (int block = 0; block < blocks; block++) {
            sum += width(block);
        }
        return sum;
    }

    @Benchmark
    public void compress(Blackhole blackhole) {
        for (int block = 0; block < blocks; block++) {
            compressBlock(block, widths[block]);
        }
        blackhole.consume(compressed);
    }

    @Benchmark
    public void decompress(Blackhole blackhole) {
        for (int block = 0; block < blocks; block++) {
            decompressBlock(block, widths[block]);
        }
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
        int offset = block * BitPacker16x.BLOCK_LEN;
        Objects.checkFromIndexSize(offset, BitPacker16x.BLOCK_LEN, values.length);
        return Vector512Kernels.numBits(values, offset, mode, 0);
    }

    private void compressBlock(int block, int bits) {
        int offset = block * BitPacker16x.BLOCK_LEN;
        int dest = offsets[block];
        int size = BitPacker16x.compressedBlockSize(bits);
        Objects.checkFromIndexSize(offset, BitPacker16x.BLOCK_LEN, values.length);
        Objects.checkFromIndexSize(dest, size, compressed.length);
        Vector512Kernels.pack(values, offset, compressed, dest, bits, mode, 0);
    }

    private void decompressBlock(int block, int bits) {
        int offset = offsets[block];
        int dest = block * BitPacker16x.BLOCK_LEN;
        int size = BitPacker16x.compressedBlockSize(bits);
        Objects.checkFromIndexSize(offset, size, compressed.length);
        Objects.checkFromIndexSize(dest, BitPacker16x.BLOCK_LEN, decoded.length);
        Vector512Kernels.unpack(compressed, offset, decoded, dest, bits, mode, 0);
    }
}
