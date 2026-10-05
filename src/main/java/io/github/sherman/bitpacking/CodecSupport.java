package io.github.sherman.bitpacking;

import java.util.Objects;

/**
 * Block sizes, width detection, and boundary-width bridges.
 */
final class CodecSupport {
    static final int BLOCK_LEN = 128;
    static final int MAX_COMPRESSED_BYTES = 512;

    // Read once, not on each call. The override also permits fallback verification.
    static final boolean VECTOR = VectorSupport.PREFERRED_BITS >= 128 && !Boolean.getBoolean("bitpacking.scalar");

    private CodecSupport() {}

    static int compressedBlockSize(int bits) {
        if (bits < 0 || bits > 32) {
            throw new IllegalArgumentException("bits must be in [0, 32]: " + bits);
        }
        return bits * 16;
    }

    static int numBits(int[] input, int offset, int mode, int initial) {
        Objects.checkFromIndexSize(offset, BLOCK_LEN, input.length);
        return VECTOR ? VectorSupport.numBits(input, offset, mode, initial) : ScalarSupport.numBits(input, offset, mode, initial);
    }

    // Widths 0 and 32 need no bit packing. Keep the mode-specific
    // bridges: forwarding a variable mode changed inlining and slowed zero-bit decode.
    static int compressCommonWidth(int[] input, int inOffset, byte[] output, int outOffset, int bits) {
        return BoundaryCodec.pack(input, inOffset, output, outOffset, bits, 0, 0);
    }

    static int compressSortedCommonWidth(int initial, int[] input, int inOffset, byte[] output, int outOffset, int bits) {
        return BoundaryCodec.pack(input, inOffset, output, outOffset, bits, 1, initial);
    }

    static int compressStrictlySortedCommonWidth(int initial, int[] input, int inOffset, byte[] output, int outOffset, int bits) {
        return BoundaryCodec.pack(input, inOffset, output, outOffset, bits, 2, initial);
    }

    static int decompressCommonWidth(byte[] input, int inOffset, int[] output, int outOffset, int bits) {
        return BoundaryCodec.unpack(input, inOffset, output, outOffset, bits, 0, 0);
    }

    static int decompressSortedCommonWidth(int initial, byte[] input, int inOffset, int[] output, int outOffset, int bits) {
        return BoundaryCodec.unpack(input, inOffset, output, outOffset, bits, 1, initial);
    }

    static int decompressStrictlySortedCommonWidth(int initial, byte[] input, int inOffset, int[] output, int outOffset, int bits) {
        return BoundaryCodec.unpack(input, inOffset, output, outOffset, bits, 2, initial);
    }
}
