package io.github.sherman.bitpacking;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class BitPackingCodecTest {
    private static final int CANARY = 0x76543210;

    @ParameterizedTest
    @EnumSource(BitPackingCodec.class)
    void selectedFormatMatchesSpecificationAtEveryWidthAndMode(BitPackingCodec codec) {
        int streams = 16;
        Random random = new Random(419373L);
        for (int bits = 0; bits <= 32; bits++) {
            int mask = (int) ((1L << bits) - 1);
            for (int mode = 0; mode <= 3; mode++) {
                int initial = mode == 3 ? -1 : 37;
                int[] input = new int[3 + 128];
                Arrays.fill(input, CANARY);
                int[] deltas = new int[128];
                int previous = initial;
                for (int i = 0; i < 128; i++) {
                    // Force exactly the requested width while keeping sorted input unsigned-monotonic.
                    int delta = i == 0 ? (bits == 0 ? 0 : 1 << (bits - 1)) : random.nextInt() & mask & 1023;
                    deltas[i] = delta;
                    input[3 + i] = mode == 0 ? delta : previous + delta + (mode >= 2 ? 1 : 0);
                    previous = input[3 + i];
                }
                int[] saved = input.clone();
                assertEquals(bits, width(codec, mode, initial, input, 3));
                byte[] expected = reference(deltas, bits, streams);
                assertEquals(expected.length, codec.compressedBlockSize(bits));
                byte[] packed = new byte[5 + expected.length];
                Arrays.fill(packed, (byte) 0x5a);
                assertEquals(expected.length, pack(codec, mode, initial, input, 3, packed, 5, bits));
                assertArrayEquals(expected, Arrays.copyOfRange(packed, 5, packed.length), codec + ", bits=" + bits + ", mode=" + mode);
                for (int i = 0; i < 5; i++) {
                    assertEquals((byte) 0x5a, packed[i]);
                }

                // Decode specification bytes independently of the selected encoder.
                int[] output = new int[7 + 128];
                Arrays.fill(output, CANARY);
                assertEquals(expected.length, unpack(codec, mode, initial, expected, 0, output, 7, bits));
                assertArrayEquals(Arrays.copyOfRange(input, 3, input.length), Arrays.copyOfRange(output, 7, output.length));
                for (int i = 0; i < 7; i++) {
                    assertEquals(CANARY, output[i]);
                }
                assertArrayEquals(saved, input);
            }
        }
    }

    @Test
    void chainedBlocksPreserveInitialValues() {
        BitPackingCodec[] codecs = {BitPackingCodec.BITPACKER16X, BitPackingCodec.BITPACKER16X, BitPackingCodec.BITPACKER16X,
                BitPackingCodec.BITPACKER16X};
        int[] input = new int[codecs.length * 128];
        int value = 0;
        for (int i = 0; i < input.length; i++) {
            if (i != 0) {
                value += 1 + ((i * 17 + (i >>> 2)) & 31);
            }
            input[i] = value;
        }
        int[] widths = new int[codecs.length];
        int[] offsets = new int[codecs.length];
        byte[] packed = new byte[codecs.length * BitPackingCodec.MAX_COMPRESSED_BYTES];
        int position = 0;
        for (int block = 0; block < codecs.length; block++) {
            int start = block * 128;
            int initial = block == 0 ? -1 : input[start - 1];
            widths[block] = codecs[block].numBitsStrictlySorted(initial, input, start);
            offsets[block] = position;
            position += codecs[block].compressStrictlySorted(initial, input, start, packed, position, widths[block]);
        }
        int[] output = new int[input.length];
        for (int block = 0; block < codecs.length; block++) {
            int start = block * 128;
            int initial = block == 0 ? -1 : output[start - 1];
            assertEquals(codecs[block].compressedBlockSize(widths[block]),
                    codecs[block].decompressStrictlySorted(initial, packed, offsets[block], output, start, widths[block]));
        }
        assertArrayEquals(input, output);
    }

    @ParameterizedTest
    @EnumSource(BitPackingCodec.class)
    void invalidArgumentsLeaveBuffersUntouched(BitPackingCodec codec) {
        int[] input = new int[128];
        byte[] packed = new byte[512];
        Arrays.fill(input, CANARY);
        Arrays.fill(packed, (byte) 0x5a);
        int[] savedInput = input.clone();
        byte[] savedPacked = packed.clone();
        for (int mode = 0; mode <= 3; mode++) {
            int m = mode;
            for (int bits : new int[]{-1, 33}) {
                assertThrows(IllegalArgumentException.class, () -> pack(codec, m, 0, input, 0, packed, 0, bits));
                assertThrows(IllegalArgumentException.class, () -> unpack(codec, m, 0, packed, 0, input, 0, bits));
            }
            assertThrows(IndexOutOfBoundsException.class, () -> width(codec, m, 0, input, 1));
            assertThrows(IndexOutOfBoundsException.class, () -> pack(codec, m, 0, input, 1, packed, 0, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> unpack(codec, m, 0, packed, 0, input, 1, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> pack(codec, m, 0, input, 0, packed, 1, 32));
            assertThrows(IndexOutOfBoundsException.class, () -> unpack(codec, m, 0, packed, 1, input, 0, 32));
            assertThrows(NullPointerException.class, () -> width(codec, m, 0, null, 0));
            assertThrows(NullPointerException.class, () -> pack(codec, m, 0, input, 0, null, 0, 0));
            assertThrows(NullPointerException.class, () -> unpack(codec, m, 0, null, 0, input, 0, 0));
        }
        assertArrayEquals(savedInput, input);
        assertArrayEquals(savedPacked, packed);
    }

    private static byte[] reference(int[] deltas, int bits, int streams) {
        int streamBytes = deltas.length / streams * bits / 8;
        byte[] expected = new byte[bits * 16];
        for (int lane = 0; lane < streams; lane++) {
            BigInteger stream = BigInteger.ZERO;
            for (int row = 0; row < deltas.length / streams; row++) {
                stream = stream.or(BigInteger.valueOf(Integer.toUnsignedLong(deltas[lane + row * streams])).shiftLeft(row * bits));
            }
            for (int b = 0; b < streamBytes; b++) {
                int group = b / 4 * 4;
                int bytes = Math.min(4, streamBytes - group);
                expected[group * streams + lane * bytes + b % 4] = stream.shiftRight(b * 8).byteValue();
            }
        }
        return expected;
    }

    private static int width(BitPackingCodec codec, int mode, int initial, int[] input, int offset) {
        return switch (mode) {
            case 0 -> codec.numBits(input, offset);
            case 1 -> codec.numBitsSorted(initial, input, offset);
            case 2 -> codec.numBitsStrictlySorted(initial, input, offset);
            case 3 -> codec.numBitsStrictlySorted(input, offset);
            default -> throw new AssertionError();
        };
    }

    private static int pack(BitPackingCodec codec, int mode, int initial, int[] input, int ip, byte[] output, int op, int bits) {
        return switch (mode) {
            case 0 -> codec.compress(input, ip, output, op, bits);
            case 1 -> codec.compressSorted(initial, input, ip, output, op, bits);
            case 2 -> codec.compressStrictlySorted(initial, input, ip, output, op, bits);
            case 3 -> codec.compressStrictlySorted(input, ip, output, op, bits);
            default -> throw new AssertionError();
        };
    }

    private static int unpack(BitPackingCodec codec, int mode, int initial, byte[] input, int ip, int[] output, int op, int bits) {
        return switch (mode) {
            case 0 -> codec.decompress(input, ip, output, op, bits);
            case 1 -> codec.decompressSorted(initial, input, ip, output, op, bits);
            case 2 -> codec.decompressStrictlySorted(initial, input, ip, output, op, bits);
            case 3 -> codec.decompressStrictlySorted(input, ip, output, op, bits);
            default -> throw new AssertionError();
        };
    }
}
