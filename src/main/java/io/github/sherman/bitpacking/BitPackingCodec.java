package io.github.sherman.bitpacking;

/**
 * Instance facade for the stateless codec API.
 *
 * <p>Every operation delegates to {@link BitPacker16x}, which defines validation,
 * byte format, return values, and delta initial values. Both APIs provide the
 * same operations and convenience overloads.
 */
public enum BitPackingCodec {
    /**
     * Sixteen-stream format with 512-bit SIMD and a portable scalar fallback.
     */
    BITPACKER16X;

    /** Number of integers in one block. */
    public static final int BLOCK_LEN = BitPacker16x.BLOCK_LEN;

    /** Maximum encoded size of one block, in bytes. */
    public static final int MAX_COMPRESSED_BYTES = BitPacker16x.MAX_COMPRESSED_BYTES;

    /**
     * Returns the encoded block size in bytes; rejects widths outside [0, 32].
     * @see BitPacker16x#compressedBlockSize(int)
     */
    public int compressedBlockSize(int bits) {
        return BitPacker16x.compressedBlockSize(bits);
    }

    /**
     * Returns the minimum width for unsigned values in one block.
     * @see BitPacker16x#numBits(int[], int)
     */
    public int numBits(int[] input, int offset) {
        return BitPacker16x.numBits(input, offset);
    }

    /**
     * Returns the minimum width for consecutive differences, starting at {@code initial}.
     * @see BitPacker16x#numBitsSorted(int, int[], int)
     */
    public int numBitsSorted(int initial, int[] input, int offset) {
        return BitPacker16x.numBitsSorted(initial, input, offset);
    }

    /**
     * Returns the minimum delta width with an initial value of zero.
     * @see BitPacker16x#numBitsSorted(int[], int)
     */
    public int numBitsSorted(int[] input, int offset) {
        return BitPacker16x.numBitsSorted(input, offset);
    }

    /**
     * Returns the minimum width for consecutive differences minus one, starting at {@code initial}.
     * @see BitPacker16x#numBitsStrictlySorted(int, int[], int)
     */
    public int numBitsStrictlySorted(int initial, int[] input, int offset) {
        return BitPacker16x.numBitsStrictlySorted(initial, input, offset);
    }

    /**
     * Returns the minimum strict-delta width with no preceding value.
     * @see BitPacker16x#numBitsStrictlySorted(int[], int)
     */
    public int numBitsStrictlySorted(int[] input, int offset) {
        return BitPacker16x.numBitsStrictlySorted(input, offset);
    }

    /**
     * Packs one block using this format; returns bytes written.
     * @see BitPacker16x#compress(int[], int, byte[], int, int)
     */
    public int compress(int[] input, int inOffset, byte[] output, int outOffset, int bits) {
        return BitPacker16x.compress(input, inOffset, output, outOffset, bits);
    }

    /**
     * Packs consecutive differences; returns bytes written.
     * @see BitPacker16x#compressSorted(int, int[], int, byte[], int, int)
     */
    public int compressSorted(int initial, int[] input, int inOffset, byte[] output, int outOffset, int bits) {
        return BitPacker16x.compressSorted(initial, input, inOffset, output, outOffset, bits);
    }

    /**
     * Packs consecutive differences with an initial value of zero; returns bytes written.
     * @see BitPacker16x#compressSorted(int[], int, byte[], int, int)
     */
    public int compressSorted(int[] input, int inOffset, byte[] output, int outOffset, int bits) {
        return BitPacker16x.compressSorted(input, inOffset, output, outOffset, bits);
    }

    /**
     * Packs consecutive differences minus one; returns bytes written.
     * @see BitPacker16x#compressStrictlySorted(int, int[], int, byte[], int, int)
     */
    public int compressStrictlySorted(int initial, int[] input, int inOffset, byte[] output, int outOffset, int bits) {
        return BitPacker16x.compressStrictlySorted(initial, input, inOffset, output, outOffset, bits);
    }

    /**
     * Packs strict deltas with no preceding value, allowing a first value of zero; returns bytes written.
     * @see BitPacker16x#compressStrictlySorted(int[], int, byte[], int, int)
     */
    public int compressStrictlySorted(int[] input, int inOffset, byte[] output, int outOffset, int bits) {
        return BitPacker16x.compressStrictlySorted(input, inOffset, output, outOffset, bits);
    }

    /**
     * Unpacks one block using this format; returns bytes consumed.
     * @see BitPacker16x#decompress(byte[], int, int[], int, int)
     */
    public int decompress(byte[] input, int inOffset, int[] output, int outOffset, int bits) {
        return BitPacker16x.decompress(input, inOffset, output, outOffset, bits);
    }

    /**
     * Unpacks deltas using the compression initial value; returns bytes consumed.
     * @see BitPacker16x#decompressSorted(int, byte[], int, int[], int, int)
     */
    public int decompressSorted(int initial, byte[] input, int inOffset, int[] output, int outOffset, int bits) {
        return BitPacker16x.decompressSorted(initial, input, inOffset, output, outOffset, bits);
    }

    /**
     * Unpacks deltas with an initial value of zero; returns bytes consumed.
     * @see BitPacker16x#decompressSorted(byte[], int, int[], int, int)
     */
    public int decompressSorted(byte[] input, int inOffset, int[] output, int outOffset, int bits) {
        return BitPacker16x.decompressSorted(input, inOffset, output, outOffset, bits);
    }

    /**
     * Unpacks deltas plus one using the compression initial value; returns bytes consumed.
     * @see BitPacker16x#decompressStrictlySorted(int, byte[], int, int[], int, int)
     */
    public int decompressStrictlySorted(int initial, byte[] input, int inOffset, int[] output, int outOffset, int bits) {
        return BitPacker16x.decompressStrictlySorted(initial, input, inOffset, output, outOffset, bits);
    }

    /**
     * Unpacks strict deltas with no preceding value; returns bytes consumed.
     * @see BitPacker16x#decompressStrictlySorted(byte[], int, int[], int, int)
     */
    public int decompressStrictlySorted(byte[] input, int inOffset, int[] output, int outOffset, int bits) {
        return BitPacker16x.decompressStrictlySorted(input, inOffset, output, outOffset, bits);
    }
}
