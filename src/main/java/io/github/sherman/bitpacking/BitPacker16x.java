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
 * satisfy unsigned ordering. Arithmetic wraps modulo 2^32. Widths and array
 * ranges are validated before writing; values and ordering are not checked.
 * Null buffers are rejected even at width zero. Instances and per-call scratch
 * storage are unnecessary; callers coordinate writes to shared buffers.
 *
 * <p>Every operation processes one complete block. Array offsets count elements
 * of the corresponding array. Compression and decompression return the number
 * of bytes written or consumed. Sorted convenience overloads use an initial
 * value of zero; strictly sorted convenience overloads use {@code -1} to
 * indicate no preceding value, allowing a first value of zero.
 *
 * <p>512-bit vectors implement widths 1-31 when supported; other hosts use a
 * scalar fallback. Launch with {@code --add-modules=jdk.incubator.vector}.
 */
public final class BitPacker16x {
    /** Number of integers in one block. */
    public static final int BLOCK_LEN = CodecSupport.BLOCK_LEN;

    /** Maximum encoded size of one block, in bytes. */
    public static final int MAX_COMPRESSED_BYTES = CodecSupport.MAX_COMPRESSED_BYTES;

    private static final boolean VECTOR = VectorSupport.PREFERRED_BITS >= 512 && CodecSupport.VECTOR;

    private BitPacker16x() {}

    /**
     * Returns the encoded block size in bytes.
     *
     * @param bits bit width in [0, 32]
     * @return encoded size, {@code 16 * bits} bytes
     * @throws IllegalArgumentException if {@code bits} is outside [0, 32]
     */
    public static int compressedBlockSize(int bits) {
        return CodecSupport.compressedBlockSize(bits);
    }

    /**
     * Returns the minimum width for unsigned values in one block.
     *
     * @param input block values
     * @param offset index of the first value
     * @return required bit width in [0, 32]
     */
    public static int numBits(int[] input, int offset) {
        return CodecSupport.numBits(input, offset, 0, 0);
    }

    /**
     * Returns the minimum width for consecutive differences in one block.
     *
     * @param initial preceding value, less than or equal to the first value in unsigned order
     * @param input block values in unsigned nondecreasing order
     * @param offset index of the first value
     * @return required bit width in [0, 32]
     */
    public static int numBitsSorted(int initial, int[] input, int offset) {
        return CodecSupport.numBits(input, offset, 1, initial);
    }

    /**
     * Returns the minimum delta width with an initial value of zero.
     *
     * @param input block values in unsigned nondecreasing order
     * @param offset index of the first value
     * @return required bit width in [0, 32]
     * @see #numBitsSorted(int, int[], int)
     */
    public static int numBitsSorted(int[] input, int offset) {
        return numBitsSorted(0, input, offset);
    }

    /**
     * Returns the minimum width for consecutive differences minus one in one block.
     *
     * @param initial preceding value, less than the first value in unsigned order,
     *                or {@code -1} to indicate no preceding value
     * @param input block values in unsigned strictly increasing order
     * @param offset index of the first value
     * @return required bit width in [0, 32]
     */
    public static int numBitsStrictlySorted(int initial, int[] input, int offset) {
        return CodecSupport.numBits(input, offset, 2, initial);
    }

    /**
     * Returns the minimum strict-delta width with no preceding value.
     * Uses an initial value of {@code -1}, allowing a first value of zero.
     *
     * @param input block values in unsigned strictly increasing order
     * @param offset index of the first value
     * @return required bit width in [0, 32]
     * @see #numBitsStrictlySorted(int, int[], int)
     */
    public static int numBitsStrictlySorted(int[] input, int offset) {
        return numBitsStrictlySorted(-1, input, offset);
    }

    /**
     * Packs one block of unsigned values.
     *
     * @param input block values that fit {@code bits}
     * @param inOffset index of the first value
     * @param output destination buffer
     * @param outOffset index of the first destination byte
     * @param bits bit width in [0, 32]
     * @return bytes written, equal to {@link #compressedBlockSize(int)}
     */
    public static int compress(int[] input, int inOffset, byte[] output, int outOffset, int bits) {
        return pack(input, inOffset, output, outOffset, bits, 0, 0);
    }

    /**
     * Packs consecutive differences for one block.
     *
     * @param initial preceding value, less than or equal to the first value in unsigned order
     * @param input block values in unsigned nondecreasing order, with deltas that fit {@code bits}
     * @param inOffset index of the first value
     * @param output destination buffer
     * @param outOffset index of the first destination byte
     * @param bits bit width in [0, 32]
     * @return bytes written, equal to {@link #compressedBlockSize(int)}
     */
    public static int compressSorted(int initial, int[] input, int inOffset, byte[] output, int outOffset, int bits) {
        return pack(input, inOffset, output, outOffset, bits, 1, initial);
    }

    /**
     * Packs consecutive differences with an initial value of zero.
     *
     * @param input block values in unsigned nondecreasing order, with deltas that fit {@code bits}
     * @param inOffset index of the first value
     * @param output destination buffer
     * @param outOffset index of the first destination byte
     * @param bits bit width in [0, 32]
     * @return bytes written, equal to {@link #compressedBlockSize(int)}
     * @see #compressSorted(int, int[], int, byte[], int, int)
     */
    public static int compressSorted(int[] input, int inOffset, byte[] output, int outOffset, int bits) {
        return compressSorted(0, input, inOffset, output, outOffset, bits);
    }

    /**
     * Packs consecutive differences minus one for one block.
     *
     * @param initial preceding value, less than the first value in unsigned order,
     *                or {@code -1} to indicate no preceding value
     * @param input block values in unsigned strictly increasing order, with strict deltas that fit {@code bits}
     * @param inOffset index of the first value
     * @param output destination buffer
     * @param outOffset index of the first destination byte
     * @param bits bit width in [0, 32]
     * @return bytes written, equal to {@link #compressedBlockSize(int)}
     */
    public static int compressStrictlySorted(int initial, int[] input, int inOffset, byte[] output, int outOffset, int bits) {
        return pack(input, inOffset, output, outOffset, bits, 2, initial);
    }

    /**
     * Packs strict deltas with no preceding value.
     * Uses an initial value of {@code -1}, allowing a first value of zero.
     *
     * @param input block values in unsigned strictly increasing order, with strict deltas that fit {@code bits}
     * @param inOffset index of the first value
     * @param output destination buffer
     * @param outOffset index of the first destination byte
     * @param bits bit width in [0, 32]
     * @return bytes written, equal to {@link #compressedBlockSize(int)}
     * @see #compressStrictlySorted(int, int[], int, byte[], int, int)
     */
    public static int compressStrictlySorted(int[] input, int inOffset, byte[] output, int outOffset, int bits) {
        return compressStrictlySorted(-1, input, inOffset, output, outOffset, bits);
    }

    /**
     * Unpacks one block of unsigned values.
     *
     * @param input encoded block
     * @param inOffset index of the first encoded byte
     * @param output destination buffer
     * @param outOffset index of the first destination value
     * @param bits bit width used for compression
     * @return bytes consumed, equal to {@link #compressedBlockSize(int)}
     */
    public static int decompress(byte[] input, int inOffset, int[] output, int outOffset, int bits) {
        return unpack(input, inOffset, output, outOffset, bits, 0, 0);
    }

    /**
     * Unpacks consecutive differences for one block.
     *
     * @param initial initial value used for compression
     * @param input encoded block
     * @param inOffset index of the first encoded byte
     * @param output destination buffer
     * @param outOffset index of the first destination value
     * @param bits bit width used for compression
     * @return bytes consumed, equal to {@link #compressedBlockSize(int)}
     */
    public static int decompressSorted(int initial, byte[] input, int inOffset, int[] output, int outOffset, int bits) {
        return unpack(input, inOffset, output, outOffset, bits, 1, initial);
    }

    /**
     * Unpacks consecutive differences with an initial value of zero.
     *
     * @param input encoded block
     * @param inOffset index of the first encoded byte
     * @param output destination buffer
     * @param outOffset index of the first destination value
     * @param bits bit width used for compression
     * @return bytes consumed, equal to {@link #compressedBlockSize(int)}
     * @see #decompressSorted(int, byte[], int, int[], int, int)
     */
    public static int decompressSorted(byte[] input, int inOffset, int[] output, int outOffset, int bits) {
        return decompressSorted(0, input, inOffset, output, outOffset, bits);
    }

    /**
     * Unpacks consecutive differences plus one for one block.
     *
     * @param initial initial value used for compression
     * @param input encoded block
     * @param inOffset index of the first encoded byte
     * @param output destination buffer
     * @param outOffset index of the first destination value
     * @param bits bit width used for compression
     * @return bytes consumed, equal to {@link #compressedBlockSize(int)}
     */
    public static int decompressStrictlySorted(int initial, byte[] input, int inOffset, int[] output, int outOffset, int bits) {
        return unpack(input, inOffset, output, outOffset, bits, 2, initial);
    }

    /**
     * Unpacks strict deltas with no preceding value.
     * Uses an initial value of {@code -1}.
     *
     * @param input encoded block
     * @param inOffset index of the first encoded byte
     * @param output destination buffer
     * @param outOffset index of the first destination value
     * @param bits bit width used for compression
     * @return bytes consumed, equal to {@link #compressedBlockSize(int)}
     * @see #decompressStrictlySorted(int, byte[], int, int[], int, int)
     */
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
