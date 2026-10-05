package io.github.sherman.bitpacking;

import java.util.Objects;

/**
 * Stateless, thread-safe codec for 128 unsigned integers in sixteen interleaved streams.
 *
 * <p>A block uses exactly {@code 16 * bits} bytes. Stream {@code lane} contains
 * values {@code lane, lane + 16, ..., lane + 112}, packed least-significant bit
 * first. Full 32-bit words are interleaved across the sixteen streams in little
 * endian order. The remaining one, two, or three bytes of each stream are stored
 * consecutively in a compact final group, without padding.
 *
 * <p>Widths and delta initial values remain external metadata. The caller must
 * ensure values or adjacent deltas fit the supplied width and sorted inputs
 * satisfy unsigned ordering. Arithmetic wraps modulo 2^32. All widths and array
 * ranges are validated before writing. Instances and per-call scratch storage
 * are unnecessary; callers coordinate writes to shared buffers.
 *
 * <p>512-bit vectors implement widths 1-31 when supported; other hosts use a
 * scalar fallback. Launch with {@code --add-modules=jdk.incubator.vector}.
 */
public final class BitPacker16x {
    public static final int BLOCK_LEN = CodecSupport.BLOCK_LEN;
    public static final int MAX_COMPRESSED_BYTES = CodecSupport.MAX_COMPRESSED_BYTES;
    private static final boolean VECTOR = VectorSupport.PREFERRED_BITS >= 512 && CodecSupport.VECTOR;

    private BitPacker16x() {}

    public static int compressedBlockSize(int bits) {
        return CodecSupport.compressedBlockSize(bits);
    }

    public static int numBits(int[] input, int offset) {
        return CodecSupport.numBits(input, offset, 0, 0);
    }

    public static int numBitsSorted(int initial, int[] input, int offset) {
        return CodecSupport.numBits(input, offset, 1, initial);
    }

    public static int numBitsStrictlySorted(int initial, int[] input, int offset) {
        return CodecSupport.numBits(input, offset, 2, initial);
    }

    public static int numBitsStrictlySorted(int[] input, int offset) {
        return numBitsStrictlySorted(-1, input, offset);
    }

    public static int compress(int[] input, int inOffset, byte[] output, int outOffset, int bits) {
        return pack(input, inOffset, output, outOffset, bits, 0, 0);
    }

    public static int compressSorted(int initial, int[] input, int inOffset, byte[] output, int outOffset, int bits) {
        return pack(input, inOffset, output, outOffset, bits, 1, initial);
    }

    public static int compressStrictlySorted(int initial, int[] input, int inOffset, byte[] output, int outOffset, int bits) {
        return pack(input, inOffset, output, outOffset, bits, 2, initial);
    }

    public static int compressStrictlySorted(int[] input, int inOffset, byte[] output, int outOffset, int bits) {
        return compressStrictlySorted(-1, input, inOffset, output, outOffset, bits);
    }

    public static int decompress(byte[] input, int inOffset, int[] output, int outOffset, int bits) {
        return unpack(input, inOffset, output, outOffset, bits, 0, 0);
    }

    public static int decompressSorted(int initial, byte[] input, int inOffset, int[] output, int outOffset, int bits) {
        return unpack(input, inOffset, output, outOffset, bits, 1, initial);
    }

    public static int decompressStrictlySorted(int initial, byte[] input, int inOffset, int[] output, int outOffset, int bits) {
        return unpack(input, inOffset, output, outOffset, bits, 2, initial);
    }

    public static int decompressStrictlySorted(byte[] input, int inOffset, int[] output, int outOffset, int bits) {
        return decompressStrictlySorted(-1, input, inOffset, output, outOffset, bits);
    }

    private static int pack(int[] input, int inOffset, byte[] output, int outOffset, int bits, int mode, int initial) {
        if (bits == 0 || bits == 32) {
            return switch (mode) {
                case 0 -> CodecSupport.compressCommonWidth(input, inOffset, output, outOffset, bits);
                case 1 -> CodecSupport.compressSortedCommonWidth(initial, input, inOffset, output, outOffset, bits);
                case 2 -> CodecSupport.compressStrictlySortedCommonWidth(initial, input, inOffset, output, outOffset, bits);
                default -> throw new AssertionError();
            };
        }
        int size = compressedBlockSize(bits);
        Objects.checkFromIndexSize(inOffset, BLOCK_LEN, input.length);
        Objects.checkFromIndexSize(outOffset, size, output.length);
        if (VECTOR) {
            Vector16Kernels.pack(input, inOffset, output, outOffset, bits, mode, initial);
        } else {
            Scalar16Kernels.pack(input, inOffset, output, outOffset, bits, mode, initial);
        }
        return size;
    }

    private static int unpack(byte[] input, int inOffset, int[] output, int outOffset, int bits, int mode, int initial) {
        if (bits == 0 || bits == 32) {
            return switch (mode) {
                case 0 -> CodecSupport.decompressCommonWidth(input, inOffset, output, outOffset, bits);
                case 1 -> CodecSupport.decompressSortedCommonWidth(initial, input, inOffset, output, outOffset, bits);
                case 2 -> CodecSupport.decompressStrictlySortedCommonWidth(initial, input, inOffset, output, outOffset, bits);
                default -> throw new AssertionError();
            };
        }
        int size = compressedBlockSize(bits);
        Objects.checkFromIndexSize(inOffset, size, input.length);
        Objects.checkFromIndexSize(outOffset, BLOCK_LEN, output.length);
        if (VECTOR) {
            Vector16Kernels.unpack(input, inOffset, output, outOffset, bits, mode, initial);
        } else {
            Scalar16Kernels.unpack(input, inOffset, output, outOffset, bits, mode, initial);
        }
        return size;
    }
}
