package io.github.sherman.bitpacking;

import java.util.Arrays;

/**
 * Portable fallback for the sixteen-stream representation. Ranges are checked by the API.
 */
final class Scalar16Kernels {
    private Scalar16Kernels() {}

    private static int byteIndex(int bits, int lane, int position) {
        int word = position >>> 5;
        int bytes = word < bits / 4 ? 4 : bits % 4;
        return word * 64 + lane * bytes + ((position & 31) >>> 3);
    }

    static void pack(int[] in, int ip, byte[] out, int op, int bits, int mode, int initial) {
        Arrays.fill(out, op, op + bits * 16, (byte) 0);
        int previous = initial;
        for (int i = 0; i < 128; i++) {
            int current = in[ip + i];
            int value = mode == 0 ? current : current - previous - (mode == 2 ? 1 : 0);
            previous = current;
            for (int b = 0; b < bits; b++) {
                int position = (i >>> 4) * bits + b;
                out[op + byteIndex(bits, i & 15, position)] |= (byte) (((value >>> b) & 1) << (position & 7));
            }
        }
    }

    static void unpack(byte[] in, int ip, int[] out, int op, int bits, int mode, int initial) {
        int previous = initial;
        for (int i = 0; i < 128; i++) {
            int value = 0;
            for (int b = 0; b < bits; b++) {
                int position = (i >>> 4) * bits + b;
                int packed = in[ip + byteIndex(bits, i & 15, position)] & 255;
                value |= ((packed >>> (position & 7)) & 1) << b;
            }
            if (mode != 0) {
                value += previous + (mode == 2 ? 1 : 0);
            }
            out[op + i] = value;
            previous = value;
        }
    }
}
