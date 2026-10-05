package io.github.sherman.bitpacking;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Scalar width detection and zero-bit/full-width block operations.
 */
final class ScalarSupport {
    private static final VarHandle WORD = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);

    private ScalarSupport() {}

    static int numBits(int[] input, int offset, int mode, int initial) {
        int union = 0;
        int previous = initial;
        for (int i = 0; i < 128; i++) {
            int value = input[offset + i];
            union |= mode == 0 ? value : value - previous - (mode == 2 ? 1 : 0);
            previous = value;
        }
        return 32 - Integer.numberOfLeadingZeros(union);
    }

    static void pack(int[] input, int offset, byte[] output, int outOffset, int bits, int mode, int initial) {
        if (bits == 0) {
            return;
        }
        int previous = initial;
        for (int i = 0; i < 128; i++) {
            int value = input[offset + i];
            int delta = mode == 0 ? value : value - previous - (mode == 2 ? 1 : 0);
            WORD.set(output, outOffset + i * 4, delta);
            previous = value;
        }
    }

    static void unpack(byte[] input, int offset, int[] output, int outOffset, int bits, int mode, int initial) {
        if (bits == 0 && mode < 2) {
            Arrays.fill(output, outOffset, outOffset + 128, mode == 0 ? 0 : initial);
            return;
        }
        int previous = initial;
        for (int i = 0; i < 128; i++) {
            int value = bits == 0 ? 0 : (int) WORD.get(input, offset + i * 4);
            if (mode != 0) {
                value += previous + (mode == 2 ? 1 : 0);
                previous = value;
            }
            output[outOffset + i] = value;
        }
    }
}
