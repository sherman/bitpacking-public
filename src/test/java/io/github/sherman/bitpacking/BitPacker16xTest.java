package io.github.sherman.bitpacking;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BitPacker16xTest {
    private static final int CANARY = 0x76543210;

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31,
        32})
    void allWidthsModesOffsetsAndExactBuffers(int bits) {
        Random random = new Random(871223L + bits);
        int mask = (int) ((1L << bits) - 1);
        for (int mode = 0; mode < 5; mode++) {
            int kernelMode = mode == 3 ? 2 : mode == 4 ? 1 : mode;
            for (int trial = 0; trial < 12; trial++) {
                int ip = trial % 5, bp = trial % 7, op = trial % 3;
                int initial = mode == 3 ? -1 : mode == 4 ? 0 : trial == 0 ? -1 : random.nextInt();
                int[] values = new int[ip + 128];
                int[] deltas = new int[128];
                Arrays.fill(values, CANARY);
                int previous = initial;
                for (int i = 0; i < 128; i++) {
                    int d = trial == 0 ? 0 : trial == 1 ? mask : random.nextInt() & mask;
                    deltas[i] = d;
                    values[ip + i] = kernelMode == 0 ? d : previous + d + (kernelMode == 2 ? 1 : 0);
                    previous = values[ip + i];
                }
                int[] saved = values.clone();
                byte[] expected = streamReference(deltas, bits);
                byte[] packed = new byte[bp + bits * 16];
                Arrays.fill(packed, (byte) 0x5a);
                assertEquals(bits * 16, pack(mode, initial, values, ip, packed, bp, bits));
                assertArrayEquals(expected, Arrays.copyOfRange(packed, bp, packed.length));
                for (int i = 0; i < bp; i++) {
                    assertEquals((byte) 0x5a, packed[i]);
                }
                int[] decoded = new int[op + 128];
                Arrays.fill(decoded, CANARY);
                assertEquals(bits * 16, unpack(mode, initial, packed, bp, decoded, op, bits));
                assertArrayEquals(Arrays.copyOfRange(values, ip, values.length), Arrays.copyOfRange(decoded, op, decoded.length));
                for (int i = 0; i < op; i++) {
                    assertEquals(CANARY, decoded[i]);
                }
                byte[] scalar = new byte[expected.length];
                ScalarKernels.pack(values, ip, scalar, 0, bits, kernelMode, initial);
                assertArrayEquals(expected, scalar);
                int[] scalarOutput = new int[128];
                ScalarKernels.unpack(packed, bp, scalarOutput, 0, bits, kernelMode, initial);
                assertArrayEquals(Arrays.copyOfRange(values, ip, values.length), scalarOutput);
                assertArrayEquals(saved, values);

                // Also detect writes beyond a block when larger caller buffers are supplied.
                byte[] padded = Arrays.copyOf(packed, packed.length + 17);
                Arrays.fill(padded, packed.length, padded.length, (byte) 0x5a);
                pack(mode, initial, values, ip, padded, bp, bits);
                assertArrayEquals(packed, Arrays.copyOf(padded, packed.length));
                for (int i = packed.length; i < padded.length; i++) {
                    assertEquals((byte) 0x5a, padded[i]);
                }
                int[] extra = Arrays.copyOf(decoded, decoded.length + 17);
                Arrays.fill(extra, decoded.length, extra.length, CANARY);
                unpack(mode, initial, packed, bp, extra, op, bits);
                assertArrayEquals(decoded, Arrays.copyOf(extra, decoded.length));
                for (int i = decoded.length; i < extra.length; i++) {
                    assertEquals(CANARY, extra[i]);
                }
            }
        }
    }

    @Test
    void zeroValuesAndDeltasUseOneBit() {
        for (int mode = 0; mode < 5; mode++) {
            int initial = mode == 3 ? -1 : mode == 4 ? 0 : 37;
            int[] input = new int[128];
            for (int i = 0; i < input.length; i++) {
                input[i] = switch (mode) {
                    case 0 -> 0;
                    case 1, 4 -> initial;
                    case 2, 3 -> initial + i + 1;
                    default -> throw new AssertionError();
                };
            }
            assertEquals(1, width(mode, initial, input, 0));
            assertEquals(16, BitPacker16x.compressedBlockSize(1));
            byte[] packed = new byte[16];
            Arrays.fill(packed, (byte) 0x5a);
            assertEquals(16, pack(mode, initial, input, 0, packed, 0, 1));
            assertArrayEquals(new byte[16], packed);
            int[] output = new int[128];
            Arrays.fill(output, CANARY);
            assertEquals(16, unpack(mode, initial, packed, 0, output, 0, 1));
            assertArrayEquals(input, output);
        }
    }

    @Test
    void rejectsZeroWidthBeforeAccessingBuffers() {
        assertThrows(IllegalArgumentException.class, () -> BitPacker16x.compressedBlockSize(0));
        for (int mode = 0; mode < 5; mode++) {
            int m = mode;
            assertThrows(IllegalArgumentException.class, () -> pack(m, 0, null, 0, null, 0, 0));
            assertThrows(IllegalArgumentException.class, () -> unpack(m, 0, null, 0, null, 0, 0));
        }
    }

    @Test
    void everyBitInEveryLane() {
        int[] input = new int[128];
        for (int bit = 0; bit < 32; bit++) {
            for (int position = 0; position < 128; position++) {
                input[position] = 1 << bit;
                int bits = bit + 1;
                assertEquals(bits, BitPacker16x.numBits(input, 0));
                byte[] compressed = new byte[16 * bits];
                BitPacker16x.compress(input, 0, compressed, 0, bits);
                assertArrayEquals(streamReference(input, bits), compressed);
                int[] decoded = new int[128];
                BitPacker16x.decompress(compressed, 0, decoded, 0, bits);
                assertArrayEquals(input, decoded);
                input[position] = 0;
            }
        }
    }

    @Test
    void strictNoInitialAndChainedBlocks() {
        int[] input = new int[256];
        for (int i = 0; i < input.length; i++) {
            input[i] = i;
        }
        assertEquals(1, BitPacker16x.numBitsStrictlySorted(input, 0));
        assertEquals(1, BitPacker16x.numBitsStrictlySorted(127, input, 128));
        byte[] packed = new byte[32];
        assertEquals(16, BitPacker16x.compressStrictlySorted(input, 0, packed, 0, 1));
        assertEquals(16, BitPacker16x.compressStrictlySorted(127, input, 128, packed, 16, 1));
        int[] result = new int[256];
        assertEquals(16, BitPacker16x.decompressStrictlySorted(packed, 0, result, 0, 1));
        assertEquals(16, BitPacker16x.decompressStrictlySorted(result[127], packed, 16, result, 128, 1));
        assertArrayEquals(input, result);
    }

    @Test
    void strictNoInitialAndUnsignedBoundary() {
        int[] values = new int[128];
        for (int i = 0; i < 128; i++) {
            values[i] = i * 8192;
        }
        int bits = BitPacker16x.numBitsStrictlySorted(values, 0);
        assertEquals(13, bits);
        byte[] packed = new byte[BitPacker16x.compressedBlockSize(bits)];
        BitPacker16x.compressStrictlySorted(values, 0, packed, 0, bits);
        int[] decoded = new int[128];
        BitPacker16x.decompressStrictlySorted(packed, 0, decoded, 0, bits);
        assertArrayEquals(values, decoded);
        for (int i = 0; i < 128; i++) {
            values[i] = Integer.MAX_VALUE - 63 + i;
        }
        assertEquals(1, BitPacker16x.numBitsStrictlySorted(values[0] - 1, values, 0));
        assertEquals(1, BitPacker16x.numBitsSorted(values[0] - 1, values, 0));
        assertEquals(32, BitPacker16x.numBits(values, 0));
    }

    @Test
    void rejectsInvalidArgumentsBeforeWriting() {
        int[] values = new int[128];
        byte[] packed = new byte[512];
        Arrays.fill(values, CANARY);
        Arrays.fill(packed, (byte) 0x5a);
        int[] savedValues = values.clone();
        byte[] savedPacked = packed.clone();
        for (int mode = 0; mode < 5; mode++) {
            final int m = mode;
            for (int bad : new int[]{-1, 0, 33, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
                assertThrows(IllegalArgumentException.class, () -> pack(m, 0, values, 0, packed, 0, bad));
                assertThrows(IllegalArgumentException.class, () -> unpack(m, 0, packed, 0, values, 0, bad));
            }
            for (int bad : new int[]{-1, 1, Integer.MAX_VALUE}) {
                assertThrows(IndexOutOfBoundsException.class, () -> pack(m, 0, values, bad, packed, 0, 32));
                assertThrows(IndexOutOfBoundsException.class, () -> pack(m, 0, values, 0, packed, bad, 32));
                assertThrows(IndexOutOfBoundsException.class, () -> unpack(m, 0, packed, bad, values, 0, 32));
                assertThrows(IndexOutOfBoundsException.class, () -> unpack(m, 0, packed, 0, values, bad, 32));
            }
            assertThrows(IndexOutOfBoundsException.class, () -> pack(m, 0, new int[127], 0, packed, 0, 13));
            assertThrows(IndexOutOfBoundsException.class, () -> unpack(m, 0, new byte[207], 0, values, 0, 13));
            assertThrows(IndexOutOfBoundsException.class, () -> unpack(m, 0, packed, 0, new int[127], 0, 13));
            assertThrows(NullPointerException.class, () -> pack(m, 0, values, 0, null, 0, 13));
            assertThrows(NullPointerException.class, () -> unpack(m, 0, null, 0, values, 0, 13));
        }
        assertArrayEquals(savedValues, values);
        assertArrayEquals(savedPacked, packed);
    }

    private static byte[] streamReference(int[] values, int bits) {
        byte[][] streams = new byte[16][bits];
        for (int lane = 0; lane < 16; lane++) {
            BigInteger stream = BigInteger.ZERO;
            for (int row = 0; row < 8; row++) {
                stream = stream.or(BigInteger.valueOf(Integer.toUnsignedLong(values[row * 16 + lane])).shiftLeft(row * bits));
            }
            for (int b = 0; b < bits; b++) {
                streams[lane][b] = stream.shiftRight(b * 8).byteValue();
            }
        }
        byte[] packed = new byte[bits * 16];
        int offset = 0;
        for (int first = 0; first < bits; first += 4) {
            int bytes = Math.min(4, bits - first);
            for (byte[] stream : streams) {
                System.arraycopy(stream, first, packed, offset, bytes);
                offset += bytes;
            }
        }
        assertEquals(packed.length, offset);
        return packed;
    }

    private static int pack(int mode, int initial, int[] in, int ip, byte[] out, int op, int bits) {
        return switch (mode) {
            case 0 -> BitPacker16x.compress(in, ip, out, op, bits);
            case 1 -> BitPacker16x.compressSorted(initial, in, ip, out, op, bits);
            case 2 -> BitPacker16x.compressStrictlySorted(initial, in, ip, out, op, bits);
            case 3 -> BitPacker16x.compressStrictlySorted(in, ip, out, op, bits);
            case 4 -> BitPacker16x.compressSorted(in, ip, out, op, bits);
            default -> throw new AssertionError();
        };
    }

    private static int unpack(int mode, int initial, byte[] in, int ip, int[] out, int op, int bits) {
        return switch (mode) {
            case 0 -> BitPacker16x.decompress(in, ip, out, op, bits);
            case 1 -> BitPacker16x.decompressSorted(initial, in, ip, out, op, bits);
            case 2 -> BitPacker16x.decompressStrictlySorted(initial, in, ip, out, op, bits);
            case 3 -> BitPacker16x.decompressStrictlySorted(in, ip, out, op, bits);
            case 4 -> BitPacker16x.decompressSorted(in, ip, out, op, bits);
            default -> throw new AssertionError();
        };
    }

    @Test
    void selectedFormatMatchesSpecificationAtEveryWidthAndMode() {
        Random random = new Random(419373L);
        for (int bits = 1; bits <= 32; bits++) {
            int mask = (int) ((1L << bits) - 1);
            for (int mode = 0; mode <= 4; mode++) {
                int initial = mode == 3 ? -1 : mode == 4 ? 0 : 37;
                boolean strict = mode == 2 || mode == 3;
                int[] input = new int[3 + 128];
                Arrays.fill(input, CANARY);
                int[] deltas = new int[128];
                int previous = initial;
                for (int i = 0; i < 128; i++) {
                    // Force exactly the requested width while keeping sorted input unsigned-monotonic.
                    int delta = i == 0 ? (1 << (bits - 1)) : random.nextInt() & mask & 1023;
                    deltas[i] = delta;
                    input[3 + i] = mode == 0 ? delta : previous + delta + (strict ? 1 : 0);
                    previous = input[3 + i];
                }
                int[] saved = input.clone();
                assertEquals(bits, width(mode, initial, input, 3));
                byte[] expected = streamReference(deltas, bits);
                assertEquals(expected.length, BitPacker16x.compressedBlockSize(bits));
                byte[] packed = new byte[5 + expected.length];
                Arrays.fill(packed, (byte) 0x5a);
                assertEquals(expected.length, pack(mode, initial, input, 3, packed, 5, bits));
                assertArrayEquals(expected, Arrays.copyOfRange(packed, 5, packed.length), "bits=" + bits + ", mode=" + mode);
                for (int i = 0; i < 5; i++) {
                    assertEquals((byte) 0x5a, packed[i]);
                }

                // Decode specification bytes independently of the selected encoder.
                int[] output = new int[7 + 128];
                Arrays.fill(output, CANARY);
                assertEquals(expected.length, unpack(mode, initial, expected, 0, output, 7, bits));
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
        int blocks = 4;
        int[] input = new int[blocks * BitPacker16x.BLOCK_LEN];
        int value = 0;
        for (int i = 0; i < input.length; i++) {
            if (i != 0) {
                value += 1 + ((i * 17 + (i >>> 2)) & 31);
            }
            input[i] = value;
        }
        int[] widths = new int[blocks];
        int[] offsets = new int[blocks];
        byte[] packed = new byte[blocks * BitPacker16x.MAX_COMPRESSED_BYTES];
        int position = 0;
        for (int block = 0; block < blocks; block++) {
            int start = block * BitPacker16x.BLOCK_LEN;
            int initial = block == 0 ? -1 : input[start - 1];
            widths[block] = BitPacker16x.numBitsStrictlySorted(initial, input, start);
            offsets[block] = position;
            position += BitPacker16x.compressStrictlySorted(initial, input, start, packed, position, widths[block]);
        }
        int[] output = new int[input.length];
        for (int block = 0; block < blocks; block++) {
            int start = block * BitPacker16x.BLOCK_LEN;
            int initial = block == 0 ? -1 : output[start - 1];
            assertEquals(BitPacker16x.compressedBlockSize(widths[block]),
                BitPacker16x.decompressStrictlySorted(initial, packed, offsets[block], output, start, widths[block]));
        }
        assertArrayEquals(input, output);
    }

    @Test
    void invalidArgumentsLeaveBuffersUntouched() {
        int[] input = new int[128];
        byte[] packed = new byte[512];
        Arrays.fill(input, CANARY);
        Arrays.fill(packed, (byte) 0x5a);
        int[] savedInput = input.clone();
        byte[] savedPacked = packed.clone();
        for (int mode = 0; mode <= 4; mode++) {
            int m = mode;
            for (int bits : new int[]{-1, 0, 33}) {
                assertThrows(IllegalArgumentException.class, () -> pack(m, 0, input, 0, packed, 0, bits));
                assertThrows(IllegalArgumentException.class, () -> unpack(m, 0, packed, 0, input, 0, bits));
            }
            assertThrows(IndexOutOfBoundsException.class, () -> width(m, 0, input, 1));
            assertThrows(IndexOutOfBoundsException.class, () -> pack(m, 0, input, 1, packed, 0, 1));
            assertThrows(IndexOutOfBoundsException.class, () -> unpack(m, 0, packed, 0, input, 1, 1));
            assertThrows(IndexOutOfBoundsException.class, () -> pack(m, 0, input, 0, packed, 1, 32));
            assertThrows(IndexOutOfBoundsException.class, () -> unpack(m, 0, packed, 1, input, 0, 32));
            assertThrows(NullPointerException.class, () -> width(m, 0, null, 0));
            assertThrows(NullPointerException.class, () -> pack(m, 0, input, 0, null, 0, 1));
            assertThrows(NullPointerException.class, () -> unpack(m, 0, null, 0, input, 0, 1));
        }
        assertArrayEquals(savedInput, input);
        assertArrayEquals(savedPacked, packed);
    }

    private static int width(int mode, int initial, int[] input, int offset) {
        return switch (mode) {
            case 0 -> BitPacker16x.numBits(input, offset);
            case 1 -> BitPacker16x.numBitsSorted(initial, input, offset);
            case 2 -> BitPacker16x.numBitsStrictlySorted(initial, input, offset);
            case 3 -> BitPacker16x.numBitsStrictlySorted(input, offset);
            case 4 -> BitPacker16x.numBitsSorted(input, offset);
            default -> throw new AssertionError();
        };
    }
}
