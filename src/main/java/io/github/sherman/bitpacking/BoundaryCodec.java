package io.github.sherman.bitpacking;

import java.util.Objects;

/**
 * Internal dispatcher for zero-bit and full-width blocks.
 */
final class BoundaryCodec {
    private static final boolean VECTOR = CodecSupport.VECTOR;

    private BoundaryCodec() {}

    static int pack(int[] input, int inOffset, byte[] output, int outOffset, int bits, int mode, int initial) {
        int size = CodecSupport.compressedBlockSize(bits);
        Objects.checkFromIndexSize(inOffset, CodecSupport.BLOCK_LEN, input.length);
        Objects.checkFromIndexSize(outOffset, size, output.length);
        if (VECTOR) {
            if (bits == 32 && VectorSupport.PREFERRED_BITS >= 512) {
                WideVectorKernels512.pack32(input, inOffset, output, outOffset, mode, initial);
            } else if (bits == 32 && VectorSupport.PREFERRED_BITS >= 256) {
                ScalarSupport.pack(input, inOffset, output, outOffset, bits, mode, initial);
            } else {
                VectorKernels.pack(input, inOffset, output, outOffset, bits, mode, initial);
            }
        } else {
            ScalarSupport.pack(input, inOffset, output, outOffset, bits, mode, initial);
        }
        return size;
    }

    static int unpack(byte[] input, int inOffset, int[] output, int outOffset, int bits, int mode, int initial) {
        int size = CodecSupport.compressedBlockSize(bits);
        Objects.checkFromIndexSize(inOffset, size, input.length);
        Objects.checkFromIndexSize(outOffset, CodecSupport.BLOCK_LEN, output.length);
        if (VECTOR) {
            if (bits == 32 && VectorSupport.PREFERRED_BITS >= 512) {
                WideVectorKernels512.unpack32(input, inOffset, output, outOffset, mode, initial);
            } else if (bits == 32 && VectorSupport.PREFERRED_BITS >= 256) {
                ScalarSupport.unpack(input, inOffset, output, outOffset, bits, mode, initial);
            } else {
                VectorKernels.unpack(input, inOffset, output, outOffset, bits, mode, initial);
            }
        } else {
            ScalarSupport.unpack(input, inOffset, output, outOffset, bits, mode, initial);
        }
        return size;
    }
}
