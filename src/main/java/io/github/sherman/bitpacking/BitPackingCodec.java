package io.github.sherman.bitpacking;

/**
 * Configurable handle for the sixteen-stream codec.
 *
 * <p>Operations have the same validation, byte format, and allocation behavior as
 * {@link BitPacker16x}. Widths and delta initial values remain external metadata.
 */
public enum BitPackingCodec {
    /**
     * Sixteen-stream format with 512-bit SIMD and a portable scalar fallback.
     */
    BITPACKER16X;

    public static final int BLOCK_LEN = CodecSupport.BLOCK_LEN;
    public static final int MAX_COMPRESSED_BYTES = CodecSupport.MAX_COMPRESSED_BYTES;

    /**
     * Returns the encoded block length; rejects widths outside [0, 32].
     */
    public int compressedBlockSize(int bits) {
        return CodecSupport.compressedBlockSize(bits);
    }

    /**
     * Minimum width for unsigned values in one block, independent of layout.
     */
    public int numBits(int[] input, int offset) {
        return CodecSupport.numBits(input, offset, 0, 0);
    }

    /**
     * Minimum width for consecutive differences, starting at {@code initial}.
     */
    public int numBitsSorted(int initial, int[] input, int offset) {
        return CodecSupport.numBits(input, offset, 1, initial);
    }

    /**
     * Minimum width for consecutive differences minus one.
     */
    public int numBitsStrictlySorted(int initial, int[] input, int offset) {
        return CodecSupport.numBits(input, offset, 2, initial);
    }

    /**
     * Minimum strict-delta width with no preceding value.
     */
    public int numBitsStrictlySorted(int[] input, int offset) {
        return numBitsStrictlySorted(-1, input, offset);
    }

    /**
     * Packs one block using this format; returns bytes written.
     */
    public int compress(int[] input, int inOffset, byte[] output, int outOffset, int bits) {
        return BitPacker16x.compress(input, inOffset, output, outOffset, bits);
    }

    /**
     * Packs consecutive differences; {@code initial} must unsigned-compare <= the first value.
     */
    public int compressSorted(int initial, int[] input, int inOffset, byte[] output, int outOffset, int bits) {
        return BitPacker16x.compressSorted(initial, input, inOffset, output, outOffset, bits);
    }

    /**
     * Packs differences minus one; {@code initial} must unsigned-compare < the first value.
     */
    public int compressStrictlySorted(int initial, int[] input, int inOffset, byte[] output, int outOffset, int bits) {
        return BitPacker16x.compressStrictlySorted(initial, input, inOffset, output, outOffset, bits);
    }

    /**
     * Packs strict deltas with no preceding value, allowing a first value of zero.
     */
    public int compressStrictlySorted(int[] input, int inOffset, byte[] output, int outOffset, int bits) {
        return compressStrictlySorted(-1, input, inOffset, output, outOffset, bits);
    }

    /**
     * Unpacks one block using this format; returns bytes consumed.
     */
    public int decompress(byte[] input, int inOffset, int[] output, int outOffset, int bits) {
        return BitPacker16x.decompress(input, inOffset, output, outOffset, bits);
    }

    /**
     * Unpacks deltas using the same initial value used for compression.
     */
    public int decompressSorted(int initial, byte[] input, int inOffset, int[] output, int outOffset, int bits) {
        return BitPacker16x.decompressSorted(initial, input, inOffset, output, outOffset, bits);
    }

    /**
     * Unpacks deltas plus one using the compression initial value.
     */
    public int decompressStrictlySorted(int initial, byte[] input, int inOffset, int[] output, int outOffset, int bits) {
        return BitPacker16x.decompressStrictlySorted(initial, input, inOffset, output, outOffset, bits);
    }

    /**
     * Unpacks strict deltas with no preceding value.
     */
    public int decompressStrictlySorted(byte[] input, int inOffset, int[] output, int outOffset, int bits) {
        return decompressStrictlySorted(-1, input, inOffset, output, outOffset, bits);
    }
}
