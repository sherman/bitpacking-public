package io.github.sherman.bitpacking;

import jdk.incubator.vector.IntVector;

final class VectorSupport {
    static final int PREFERRED_BITS = IntVector.SPECIES_PREFERRED.vectorBitSize();

    private VectorSupport() {}

    static int numBits(int[] input, int offset, int mode, int initial) {
        if (PREFERRED_BITS >= 512) {
            return WideVectorKernels512.numBits(input, offset, mode, initial);
        }
        return numBits128(input, offset, mode, initial);
    }

    static int numBits128(int[] input, int offset, int mode, int initial) {
        return VectorKernels.numBits(input, offset, mode, initial);
    }
}
