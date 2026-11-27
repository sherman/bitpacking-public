// Generated SIMD kernels for the sixteen-stream format.
// Derived SIMD helpers: Copyright (c) 2016 Paul Masurel, MIT license.
package io.github.sherman.bitpacking;

import static jdk.incubator.vector.VectorOperators.*;

import jdk.incubator.vector.*;

/**
 * 512-bit width detection and sixteen-stream packing for widths 1–32.
 * Modes are 0 (plain), 1 (sorted), and 2 (strictly sorted). Ranges are checked by the API.
 */
final class Vector512Kernels {
    private static final VectorSpecies<Integer> S = IntVector.SPECIES_512;
    private static final VectorSpecies<Byte> B = ByteVector.SPECIES_512;
    private static final VectorSpecies<Byte> B128 = ByteVector.SPECIES_128;
    private static final VectorSpecies<Byte> B256 = ByteVector.SPECIES_256;
    private static final VectorSpecies<Short> SH256 = ShortVector.SPECIES_256;
    private static final IntVector ONE = IntVector.broadcast(S, 1);
    // selectFrom(previous, current) yields previous[15], current[0..14] (one vpermi2d).
    private static final IntVector PREVIOUS_INDEX = IntVector.zero(S).addIndex(1).add(15);
    private static final VectorMask<Byte> BYTE_TAIL3 = VectorMask.fromLong(B, 0xffffffffffffL);
    private static final VectorMask<Byte> BYTE_WORD3 = VectorMask.fromLong(B, 0x7777777777777777L);
    private static final VectorShuffle<Byte> BYTE_EXPAND3 = VectorShuffle.fromOp(B, i -> i % 4 < 3 ? i / 4 * 3 + i % 4 : 0);
    private static final VectorShuffle<Byte> BYTE_COMPACT3 = VectorShuffle.fromOp(B, i -> i < 48 ? i / 3 * 4 + i % 3 : 0);

    // Prefix sums: a qword shift sums lane pairs, then each step adds the total of the pair 1, 2, or 4 pairs back.
    private static final VectorShuffle<Integer> INT_LAST = VectorShuffle.fromOp(S, i -> 15);
    private static final VectorShuffle<Integer> PAIR_SHIFT1 = VectorShuffle.fromOp(S, i -> Math.max(0, (i / 2 - 1) * 2 + 1));
    private static final VectorMask<Integer> PAIR_MASK1 = VectorMask.fromLong(S, -1L << 2);
    private static final VectorShuffle<Integer> PAIR_SHIFT2 = VectorShuffle.fromOp(S, i -> Math.max(0, (i / 2 - 2) * 2 + 1));
    private static final VectorMask<Integer> PAIR_MASK2 = VectorMask.fromLong(S, -1L << 4);
    private static final VectorShuffle<Integer> PAIR_SHIFT4 = VectorShuffle.fromOp(S, i -> Math.max(0, (i / 2 - 4) * 2 + 1));
    private static final VectorMask<Integer> PAIR_MASK4 = VectorMask.fromLong(S, -1L << 8);

    private Vector512Kernels() {}

    static int numBits(int[] input, int offset, int mode, int initial) {
        return switch (mode) {
            case 0 -> numBits0(input, offset, initial);
            case 1 -> numBits1(input, offset, initial);
            case 2 -> numBits2(input, offset, initial);
            default -> throw new IllegalArgumentException("Unknown mode");
        };
    }

    static void pack(int[] in, int ip, byte[] out, int op, int bits, int mode, int initial) {
        switch (mode) {
            case 0 -> pack0(in, ip, out, op, bits, initial);
            case 1 -> pack1(in, ip, out, op, bits, initial);
            case 2 -> pack2(in, ip, out, op, bits, initial);
            default -> throw new IllegalArgumentException("Invalid mode");
        }
    }

    static void unpack(byte[] in, int ip, int[] out, int op, int bits, int mode, int initial) {
        switch (mode) {
            case 0 -> unpack0(in, ip, out, op, bits, initial);
            case 1 -> unpack1(in, ip, out, op, bits, initial);
            case 2 -> unpack2(in, ip, out, op, bits, initial);
            default -> throw new IllegalArgumentException("Invalid mode");
        }
    }

    private static int numBits0(int[] input, int offset, int initial) {
        IntVector union = IntVector.zero(S);
        for (int i = 0; i < 128; i += 16) {
            IntVector current = IntVector.fromArray(S, input, offset + i);
            union = union.or(current);
        }
        return 32 - Integer.numberOfLeadingZeros(union.reduceLanes(OR) | 1);
    }

    private static int numBits1(int[] input, int offset, int initial) {
        // Stay a loop: an unrolled body exceeds FreqInlineSize and stops inlining into callers.
        IntVector union = IntVector.zero(S);
        IntVector previous = IntVector.broadcast(S, initial);
        for (int i = 0; i < 128; i += 16) {
            IntVector current = IntVector.fromArray(S, input, offset + i);
            IntVector value = current.sub(PREVIOUS_INDEX.selectFrom(previous, current));
            previous = current;
            union = union.or(value);
        }
        return 32 - Integer.numberOfLeadingZeros(union.reduceLanes(OR) | 1);
    }

    private static int numBits2(int[] input, int offset, int initial) {
        // Stay a loop: an unrolled body exceeds FreqInlineSize and stops inlining into callers.
        IntVector union = IntVector.zero(S);
        IntVector previous = IntVector.broadcast(S, initial);
        for (int i = 0; i < 128; i += 16) {
            IntVector current = IntVector.fromArray(S, input, offset + i);
            IntVector value = current.sub(PREVIOUS_INDEX.selectFrom(previous, current));
            value = value.sub(ONE);
            previous = current;
            union = union.or(value);
        }
        return 32 - Integer.numberOfLeadingZeros(union.reduceLanes(OR) | 1);
    }

    private static void pack0(int[] in, int ip, byte[] out, int op, int bits, int initial) {
        switch (bits) {
            case 32 -> pack0_32(in, ip, out, op, initial);
            case 1 -> pack0_1(in, ip, out, op, initial);
            case 2 -> pack0_2(in, ip, out, op, initial);
            case 3 -> pack0_3(in, ip, out, op, initial);
            case 4 -> pack0_4(in, ip, out, op, initial);
            case 5 -> pack0_5(in, ip, out, op, initial);
            case 6 -> pack0_6(in, ip, out, op, initial);
            case 7 -> pack0_7(in, ip, out, op, initial);
            case 8 -> pack0_8(in, ip, out, op, initial);
            case 9 -> pack0_9(in, ip, out, op, initial);
            case 10 -> pack0_10(in, ip, out, op, initial);
            case 11 -> pack0_11(in, ip, out, op, initial);
            case 12 -> pack0_12(in, ip, out, op, initial);
            case 13 -> pack0_13(in, ip, out, op, initial);
            case 14 -> pack0_14(in, ip, out, op, initial);
            case 15 -> pack0_15(in, ip, out, op, initial);
            case 16 -> pack0_16(in, ip, out, op, initial);
            case 17 -> pack0_17(in, ip, out, op, initial);
            case 18 -> pack0_18(in, ip, out, op, initial);
            case 19 -> pack0_19(in, ip, out, op, initial);
            case 20 -> pack0_20(in, ip, out, op, initial);
            case 21 -> pack0_21(in, ip, out, op, initial);
            case 22 -> pack0_22(in, ip, out, op, initial);
            case 23 -> pack0_23(in, ip, out, op, initial);
            case 24 -> pack0_24(in, ip, out, op, initial);
            case 25 -> pack0_25(in, ip, out, op, initial);
            case 26 -> pack0_26(in, ip, out, op, initial);
            case 27 -> pack0_27(in, ip, out, op, initial);
            case 28 -> pack0_28(in, ip, out, op, initial);
            case 29 -> pack0_29(in, ip, out, op, initial);
            case 30 -> pack0_30(in, ip, out, op, initial);
            case 31 -> pack0_31(in, ip, out, op, initial);
            default -> throw new IllegalArgumentException("Unsupported width");
        }
    }

    private static void pack0_1(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.and(1);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.and(1);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.and(1);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.and(1);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.and(1);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.and(1);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.and(1);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.and(1);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 1)).or(v2.lanewise(LSHL, 2)).or(v3.lanewise(LSHL, 3)).or(v4.lanewise(LSHL, 4))
            .or(v5.lanewise(LSHL, 5)).or(v6.lanewise(LSHL, 6)).or(v7.lanewise(LSHL, 7));
        ((ByteVector) w0.convertShape(I2B, B128, 0)).intoArray(out, op + 0);
    }

    private static void pack0_2(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.and(3);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.and(3);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.and(3);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.and(3);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.and(3);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.and(3);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.and(3);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.and(3);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 2)).or(v2.lanewise(LSHL, 4)).or(v3.lanewise(LSHL, 6)).or(v4.lanewise(LSHL, 8))
            .or(v5.lanewise(LSHL, 10)).or(v6.lanewise(LSHL, 12)).or(v7.lanewise(LSHL, 14));
        ((ShortVector) w0.convertShape(I2S, SH256, 0)).reinterpretAsBytes().intoArray(out, op + 0);
    }

    private static void pack0_3(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.and(7);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.and(7);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.and(7);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.and(7);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.and(7);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.and(7);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.and(7);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.and(7);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 3)).or(v2.lanewise(LSHL, 6)).or(v3.lanewise(LSHL, 9)).or(v4.lanewise(LSHL, 12))
            .or(v5.lanewise(LSHL, 15)).or(v6.lanewise(LSHL, 18)).or(v7.lanewise(LSHL, 21));
        ((ByteVector) w0.reinterpretShape(B, 0)).rearrange(BYTE_COMPACT3).intoArray(out, op + 0, BYTE_TAIL3);
    }

    private static void pack0_4(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.and(15);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.and(15);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.and(15);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.and(15);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.and(15);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.and(15);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.and(15);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.and(15);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 4)).or(v2.lanewise(LSHL, 8)).or(v3.lanewise(LSHL, 12)).or(v4.lanewise(LSHL, 16))
            .or(v5.lanewise(LSHL, 20)).or(v6.lanewise(LSHL, 24)).or(v7.lanewise(LSHL, 28));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
    }

    private static void pack0_5(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.and(31);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.and(31);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.and(31);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.and(31);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.and(31);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.and(31);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.and(31);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.and(31);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 5)).or(v2.lanewise(LSHL, 10)).or(v3.lanewise(LSHL, 15)).or(v4.lanewise(LSHL, 20))
            .or(v5.lanewise(LSHL, 25)).or(v6.lanewise(LSHL, 30));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v6.lanewise(LSHR, 2).or(v7.lanewise(LSHL, 3));
        ((ByteVector) w1.convertShape(I2B, B128, 0)).intoArray(out, op + 64);
    }

    private static void pack0_6(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.and(63);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.and(63);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.and(63);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.and(63);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.and(63);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.and(63);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.and(63);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.and(63);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 6)).or(v2.lanewise(LSHL, 12)).or(v3.lanewise(LSHL, 18)).or(v4.lanewise(LSHL, 24))
            .or(v5.lanewise(LSHL, 30));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v5.lanewise(LSHR, 2).or(v6.lanewise(LSHL, 4)).or(v7.lanewise(LSHL, 10));
        ((ShortVector) w1.convertShape(I2S, SH256, 0)).reinterpretAsBytes().intoArray(out, op + 64);
    }

    private static void pack0_7(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.and(127);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.and(127);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.and(127);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.and(127);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.and(127);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.and(127);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.and(127);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.and(127);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 7)).or(v2.lanewise(LSHL, 14)).or(v3.lanewise(LSHL, 21)).or(v4.lanewise(LSHL, 28));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v4.lanewise(LSHR, 4).or(v5.lanewise(LSHL, 3)).or(v6.lanewise(LSHL, 10)).or(v7.lanewise(LSHL, 17));
        ((ByteVector) w1.reinterpretShape(B, 0)).rearrange(BYTE_COMPACT3).intoArray(out, op + 64, BYTE_TAIL3);
    }

    private static void pack0_8(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.and(255);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.and(255);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.and(255);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.and(255);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.and(255);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.and(255);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.and(255);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.and(255);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 8)).or(v2.lanewise(LSHL, 16)).or(v3.lanewise(LSHL, 24));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v4.or(v5.lanewise(LSHL, 8)).or(v6.lanewise(LSHL, 16)).or(v7.lanewise(LSHL, 24));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
    }

    private static void pack0_9(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.and(511);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.and(511);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.and(511);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.and(511);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.and(511);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.and(511);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.and(511);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.and(511);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 9)).or(v2.lanewise(LSHL, 18)).or(v3.lanewise(LSHL, 27));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v3.lanewise(LSHR, 5).or(v4.lanewise(LSHL, 4)).or(v5.lanewise(LSHL, 13)).or(v6.lanewise(LSHL, 22))
            .or(v7.lanewise(LSHL, 31));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v7.lanewise(LSHR, 1);
        ((ByteVector) w2.convertShape(I2B, B128, 0)).intoArray(out, op + 128);
    }

    private static void pack0_10(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.and(1023);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.and(1023);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.and(1023);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.and(1023);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.and(1023);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.and(1023);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.and(1023);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.and(1023);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 10)).or(v2.lanewise(LSHL, 20)).or(v3.lanewise(LSHL, 30));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v3.lanewise(LSHR, 2).or(v4.lanewise(LSHL, 8)).or(v5.lanewise(LSHL, 18)).or(v6.lanewise(LSHL, 28));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v6.lanewise(LSHR, 4).or(v7.lanewise(LSHL, 6));
        ((ShortVector) w2.convertShape(I2S, SH256, 0)).reinterpretAsBytes().intoArray(out, op + 128);
    }

    private static void pack0_11(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.and(2047);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.and(2047);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.and(2047);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.and(2047);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.and(2047);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.and(2047);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.and(2047);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.and(2047);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 11)).or(v2.lanewise(LSHL, 22));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v2.lanewise(LSHR, 10).or(v3.lanewise(LSHL, 1)).or(v4.lanewise(LSHL, 12)).or(v5.lanewise(LSHL, 23));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v5.lanewise(LSHR, 9).or(v6.lanewise(LSHL, 2)).or(v7.lanewise(LSHL, 13));
        ((ByteVector) w2.reinterpretShape(B, 0)).rearrange(BYTE_COMPACT3).intoArray(out, op + 128, BYTE_TAIL3);
    }

    private static void pack0_12(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.and(4095);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.and(4095);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.and(4095);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.and(4095);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.and(4095);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.and(4095);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.and(4095);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.and(4095);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 12)).or(v2.lanewise(LSHL, 24));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v2.lanewise(LSHR, 8).or(v3.lanewise(LSHL, 4)).or(v4.lanewise(LSHL, 16)).or(v5.lanewise(LSHL, 28));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v5.lanewise(LSHR, 4).or(v6.lanewise(LSHL, 8)).or(v7.lanewise(LSHL, 20));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
    }

    private static void pack0_13(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.and(8191);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.and(8191);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.and(8191);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.and(8191);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.and(8191);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.and(8191);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.and(8191);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.and(8191);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 13)).or(v2.lanewise(LSHL, 26));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v2.lanewise(LSHR, 6).or(v3.lanewise(LSHL, 7)).or(v4.lanewise(LSHL, 20));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v4.lanewise(LSHR, 12).or(v5.lanewise(LSHL, 1)).or(v6.lanewise(LSHL, 14)).or(v7.lanewise(LSHL, 27));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v7.lanewise(LSHR, 5);
        ((ByteVector) w3.convertShape(I2B, B128, 0)).intoArray(out, op + 192);
    }

    private static void pack0_14(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.and(16383);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.and(16383);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.and(16383);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.and(16383);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.and(16383);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.and(16383);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.and(16383);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.and(16383);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 14)).or(v2.lanewise(LSHL, 28));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v2.lanewise(LSHR, 4).or(v3.lanewise(LSHL, 10)).or(v4.lanewise(LSHL, 24));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v4.lanewise(LSHR, 8).or(v5.lanewise(LSHL, 6)).or(v6.lanewise(LSHL, 20));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v6.lanewise(LSHR, 12).or(v7.lanewise(LSHL, 2));
        ((ShortVector) w3.convertShape(I2S, SH256, 0)).reinterpretAsBytes().intoArray(out, op + 192);
    }

    private static void pack0_15(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.and(32767);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.and(32767);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.and(32767);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.and(32767);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.and(32767);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.and(32767);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.and(32767);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.and(32767);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 15)).or(v2.lanewise(LSHL, 30));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v2.lanewise(LSHR, 2).or(v3.lanewise(LSHL, 13)).or(v4.lanewise(LSHL, 28));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v4.lanewise(LSHR, 4).or(v5.lanewise(LSHL, 11)).or(v6.lanewise(LSHL, 26));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v6.lanewise(LSHR, 6).or(v7.lanewise(LSHL, 9));
        ((ByteVector) w3.reinterpretShape(B, 0)).rearrange(BYTE_COMPACT3).intoArray(out, op + 192, BYTE_TAIL3);
    }

    private static void pack0_16(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.and(65535);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.and(65535);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.and(65535);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.and(65535);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.and(65535);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.and(65535);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.and(65535);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.and(65535);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 16));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v2.or(v3.lanewise(LSHL, 16));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v4.or(v5.lanewise(LSHL, 16));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v6.or(v7.lanewise(LSHL, 16));
        ((ByteVector) w3.reinterpretShape(B, 0)).intoArray(out, op + 192);
    }

    private static void pack0_17(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.and(131071);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.and(131071);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.and(131071);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.and(131071);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.and(131071);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.and(131071);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.and(131071);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.and(131071);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 17));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v1.lanewise(LSHR, 15).or(v2.lanewise(LSHL, 2)).or(v3.lanewise(LSHL, 19));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v3.lanewise(LSHR, 13).or(v4.lanewise(LSHL, 4)).or(v5.lanewise(LSHL, 21));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v5.lanewise(LSHR, 11).or(v6.lanewise(LSHL, 6)).or(v7.lanewise(LSHL, 23));
        ((ByteVector) w3.reinterpretShape(B, 0)).intoArray(out, op + 192);
        IntVector w4 = v7.lanewise(LSHR, 9);
        ((ByteVector) w4.convertShape(I2B, B128, 0)).intoArray(out, op + 256);
    }

    private static void pack0_18(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.and(262143);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.and(262143);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.and(262143);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.and(262143);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.and(262143);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.and(262143);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.and(262143);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.and(262143);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 18));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v1.lanewise(LSHR, 14).or(v2.lanewise(LSHL, 4)).or(v3.lanewise(LSHL, 22));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v3.lanewise(LSHR, 10).or(v4.lanewise(LSHL, 8)).or(v5.lanewise(LSHL, 26));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v5.lanewise(LSHR, 6).or(v6.lanewise(LSHL, 12)).or(v7.lanewise(LSHL, 30));
        ((ByteVector) w3.reinterpretShape(B, 0)).intoArray(out, op + 192);
        IntVector w4 = v7.lanewise(LSHR, 2);
        ((ShortVector) w4.convertShape(I2S, SH256, 0)).reinterpretAsBytes().intoArray(out, op + 256);
    }

    private static void pack0_19(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.and(524287);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.and(524287);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.and(524287);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.and(524287);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.and(524287);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.and(524287);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.and(524287);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.and(524287);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 19));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v1.lanewise(LSHR, 13).or(v2.lanewise(LSHL, 6)).or(v3.lanewise(LSHL, 25));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v3.lanewise(LSHR, 7).or(v4.lanewise(LSHL, 12)).or(v5.lanewise(LSHL, 31));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v5.lanewise(LSHR, 1).or(v6.lanewise(LSHL, 18));
        ((ByteVector) w3.reinterpretShape(B, 0)).intoArray(out, op + 192);
        IntVector w4 = v6.lanewise(LSHR, 14).or(v7.lanewise(LSHL, 5));
        ((ByteVector) w4.reinterpretShape(B, 0)).rearrange(BYTE_COMPACT3).intoArray(out, op + 256, BYTE_TAIL3);
    }

    private static void pack0_20(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.and(1048575);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.and(1048575);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.and(1048575);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.and(1048575);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.and(1048575);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.and(1048575);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.and(1048575);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.and(1048575);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 20));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v1.lanewise(LSHR, 12).or(v2.lanewise(LSHL, 8)).or(v3.lanewise(LSHL, 28));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v3.lanewise(LSHR, 4).or(v4.lanewise(LSHL, 16));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v4.lanewise(LSHR, 16).or(v5.lanewise(LSHL, 4)).or(v6.lanewise(LSHL, 24));
        ((ByteVector) w3.reinterpretShape(B, 0)).intoArray(out, op + 192);
        IntVector w4 = v6.lanewise(LSHR, 8).or(v7.lanewise(LSHL, 12));
        ((ByteVector) w4.reinterpretShape(B, 0)).intoArray(out, op + 256);
    }

    private static void pack0_21(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.and(2097151);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.and(2097151);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.and(2097151);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.and(2097151);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.and(2097151);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.and(2097151);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.and(2097151);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.and(2097151);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 21));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v1.lanewise(LSHR, 11).or(v2.lanewise(LSHL, 10)).or(v3.lanewise(LSHL, 31));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v3.lanewise(LSHR, 1).or(v4.lanewise(LSHL, 20));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v4.lanewise(LSHR, 12).or(v5.lanewise(LSHL, 9)).or(v6.lanewise(LSHL, 30));
        ((ByteVector) w3.reinterpretShape(B, 0)).intoArray(out, op + 192);
        IntVector w4 = v6.lanewise(LSHR, 2).or(v7.lanewise(LSHL, 19));
        ((ByteVector) w4.reinterpretShape(B, 0)).intoArray(out, op + 256);
        IntVector w5 = v7.lanewise(LSHR, 13);
        ((ByteVector) w5.convertShape(I2B, B128, 0)).intoArray(out, op + 320);
    }

    private static void pack0_22(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.and(4194303);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.and(4194303);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.and(4194303);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.and(4194303);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.and(4194303);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.and(4194303);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.and(4194303);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.and(4194303);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 22));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v1.lanewise(LSHR, 10).or(v2.lanewise(LSHL, 12));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v2.lanewise(LSHR, 20).or(v3.lanewise(LSHL, 2)).or(v4.lanewise(LSHL, 24));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v4.lanewise(LSHR, 8).or(v5.lanewise(LSHL, 14));
        ((ByteVector) w3.reinterpretShape(B, 0)).intoArray(out, op + 192);
        IntVector w4 = v5.lanewise(LSHR, 18).or(v6.lanewise(LSHL, 4)).or(v7.lanewise(LSHL, 26));
        ((ByteVector) w4.reinterpretShape(B, 0)).intoArray(out, op + 256);
        IntVector w5 = v7.lanewise(LSHR, 6);
        ((ShortVector) w5.convertShape(I2S, SH256, 0)).reinterpretAsBytes().intoArray(out, op + 320);
    }

    private static void pack0_23(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.and(8388607);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.and(8388607);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.and(8388607);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.and(8388607);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.and(8388607);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.and(8388607);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.and(8388607);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.and(8388607);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 23));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v1.lanewise(LSHR, 9).or(v2.lanewise(LSHL, 14));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v2.lanewise(LSHR, 18).or(v3.lanewise(LSHL, 5)).or(v4.lanewise(LSHL, 28));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v4.lanewise(LSHR, 4).or(v5.lanewise(LSHL, 19));
        ((ByteVector) w3.reinterpretShape(B, 0)).intoArray(out, op + 192);
        IntVector w4 = v5.lanewise(LSHR, 13).or(v6.lanewise(LSHL, 10));
        ((ByteVector) w4.reinterpretShape(B, 0)).intoArray(out, op + 256);
        IntVector w5 = v6.lanewise(LSHR, 22).or(v7.lanewise(LSHL, 1));
        ((ByteVector) w5.reinterpretShape(B, 0)).rearrange(BYTE_COMPACT3).intoArray(out, op + 320, BYTE_TAIL3);
    }

    private static void pack0_24(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.and(16777215);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.and(16777215);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.and(16777215);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.and(16777215);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.and(16777215);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.and(16777215);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.and(16777215);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.and(16777215);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 24));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v1.lanewise(LSHR, 8).or(v2.lanewise(LSHL, 16));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v2.lanewise(LSHR, 16).or(v3.lanewise(LSHL, 8));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v4.or(v5.lanewise(LSHL, 24));
        ((ByteVector) w3.reinterpretShape(B, 0)).intoArray(out, op + 192);
        IntVector w4 = v5.lanewise(LSHR, 8).or(v6.lanewise(LSHL, 16));
        ((ByteVector) w4.reinterpretShape(B, 0)).intoArray(out, op + 256);
        IntVector w5 = v6.lanewise(LSHR, 16).or(v7.lanewise(LSHL, 8));
        ((ByteVector) w5.reinterpretShape(B, 0)).intoArray(out, op + 320);
    }

    private static void pack0_25(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.and(33554431);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.and(33554431);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.and(33554431);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.and(33554431);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.and(33554431);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.and(33554431);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.and(33554431);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.and(33554431);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 25));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v1.lanewise(LSHR, 7).or(v2.lanewise(LSHL, 18));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v2.lanewise(LSHR, 14).or(v3.lanewise(LSHL, 11));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v3.lanewise(LSHR, 21).or(v4.lanewise(LSHL, 4)).or(v5.lanewise(LSHL, 29));
        ((ByteVector) w3.reinterpretShape(B, 0)).intoArray(out, op + 192);
        IntVector w4 = v5.lanewise(LSHR, 3).or(v6.lanewise(LSHL, 22));
        ((ByteVector) w4.reinterpretShape(B, 0)).intoArray(out, op + 256);
        IntVector w5 = v6.lanewise(LSHR, 10).or(v7.lanewise(LSHL, 15));
        ((ByteVector) w5.reinterpretShape(B, 0)).intoArray(out, op + 320);
        IntVector w6 = v7.lanewise(LSHR, 17);
        ((ByteVector) w6.convertShape(I2B, B128, 0)).intoArray(out, op + 384);
    }

    private static void pack0_26(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.and(67108863);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.and(67108863);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.and(67108863);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.and(67108863);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.and(67108863);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.and(67108863);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.and(67108863);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.and(67108863);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 26));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v1.lanewise(LSHR, 6).or(v2.lanewise(LSHL, 20));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v2.lanewise(LSHR, 12).or(v3.lanewise(LSHL, 14));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v3.lanewise(LSHR, 18).or(v4.lanewise(LSHL, 8));
        ((ByteVector) w3.reinterpretShape(B, 0)).intoArray(out, op + 192);
        IntVector w4 = v4.lanewise(LSHR, 24).or(v5.lanewise(LSHL, 2)).or(v6.lanewise(LSHL, 28));
        ((ByteVector) w4.reinterpretShape(B, 0)).intoArray(out, op + 256);
        IntVector w5 = v6.lanewise(LSHR, 4).or(v7.lanewise(LSHL, 22));
        ((ByteVector) w5.reinterpretShape(B, 0)).intoArray(out, op + 320);
        IntVector w6 = v7.lanewise(LSHR, 10);
        ((ShortVector) w6.convertShape(I2S, SH256, 0)).reinterpretAsBytes().intoArray(out, op + 384);
    }

    private static void pack0_27(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.and(134217727);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.and(134217727);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.and(134217727);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.and(134217727);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.and(134217727);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.and(134217727);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.and(134217727);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.and(134217727);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 27));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v1.lanewise(LSHR, 5).or(v2.lanewise(LSHL, 22));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v2.lanewise(LSHR, 10).or(v3.lanewise(LSHL, 17));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v3.lanewise(LSHR, 15).or(v4.lanewise(LSHL, 12));
        ((ByteVector) w3.reinterpretShape(B, 0)).intoArray(out, op + 192);
        IntVector w4 = v4.lanewise(LSHR, 20).or(v5.lanewise(LSHL, 7));
        ((ByteVector) w4.reinterpretShape(B, 0)).intoArray(out, op + 256);
        IntVector w5 = v5.lanewise(LSHR, 25).or(v6.lanewise(LSHL, 2)).or(v7.lanewise(LSHL, 29));
        ((ByteVector) w5.reinterpretShape(B, 0)).intoArray(out, op + 320);
        IntVector w6 = v7.lanewise(LSHR, 3);
        ((ByteVector) w6.reinterpretShape(B, 0)).rearrange(BYTE_COMPACT3).intoArray(out, op + 384, BYTE_TAIL3);
    }

    private static void pack0_28(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.and(268435455);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.and(268435455);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.and(268435455);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.and(268435455);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.and(268435455);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.and(268435455);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.and(268435455);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.and(268435455);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 28));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v1.lanewise(LSHR, 4).or(v2.lanewise(LSHL, 24));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v2.lanewise(LSHR, 8).or(v3.lanewise(LSHL, 20));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v3.lanewise(LSHR, 12).or(v4.lanewise(LSHL, 16));
        ((ByteVector) w3.reinterpretShape(B, 0)).intoArray(out, op + 192);
        IntVector w4 = v4.lanewise(LSHR, 16).or(v5.lanewise(LSHL, 12));
        ((ByteVector) w4.reinterpretShape(B, 0)).intoArray(out, op + 256);
        IntVector w5 = v5.lanewise(LSHR, 20).or(v6.lanewise(LSHL, 8));
        ((ByteVector) w5.reinterpretShape(B, 0)).intoArray(out, op + 320);
        IntVector w6 = v6.lanewise(LSHR, 24).or(v7.lanewise(LSHL, 4));
        ((ByteVector) w6.reinterpretShape(B, 0)).intoArray(out, op + 384);
    }

    private static void pack0_29(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.and(536870911);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.and(536870911);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.and(536870911);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.and(536870911);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.and(536870911);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.and(536870911);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.and(536870911);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.and(536870911);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 29));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v1.lanewise(LSHR, 3).or(v2.lanewise(LSHL, 26));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v2.lanewise(LSHR, 6).or(v3.lanewise(LSHL, 23));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v3.lanewise(LSHR, 9).or(v4.lanewise(LSHL, 20));
        ((ByteVector) w3.reinterpretShape(B, 0)).intoArray(out, op + 192);
        IntVector w4 = v4.lanewise(LSHR, 12).or(v5.lanewise(LSHL, 17));
        ((ByteVector) w4.reinterpretShape(B, 0)).intoArray(out, op + 256);
        IntVector w5 = v5.lanewise(LSHR, 15).or(v6.lanewise(LSHL, 14));
        ((ByteVector) w5.reinterpretShape(B, 0)).intoArray(out, op + 320);
        IntVector w6 = v6.lanewise(LSHR, 18).or(v7.lanewise(LSHL, 11));
        ((ByteVector) w6.reinterpretShape(B, 0)).intoArray(out, op + 384);
        IntVector w7 = v7.lanewise(LSHR, 21);
        ((ByteVector) w7.convertShape(I2B, B128, 0)).intoArray(out, op + 448);
    }

    private static void pack0_30(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.and(1073741823);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.and(1073741823);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.and(1073741823);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.and(1073741823);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.and(1073741823);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.and(1073741823);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.and(1073741823);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.and(1073741823);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 30));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v1.lanewise(LSHR, 2).or(v2.lanewise(LSHL, 28));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v2.lanewise(LSHR, 4).or(v3.lanewise(LSHL, 26));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v3.lanewise(LSHR, 6).or(v4.lanewise(LSHL, 24));
        ((ByteVector) w3.reinterpretShape(B, 0)).intoArray(out, op + 192);
        IntVector w4 = v4.lanewise(LSHR, 8).or(v5.lanewise(LSHL, 22));
        ((ByteVector) w4.reinterpretShape(B, 0)).intoArray(out, op + 256);
        IntVector w5 = v5.lanewise(LSHR, 10).or(v6.lanewise(LSHL, 20));
        ((ByteVector) w5.reinterpretShape(B, 0)).intoArray(out, op + 320);
        IntVector w6 = v6.lanewise(LSHR, 12).or(v7.lanewise(LSHL, 18));
        ((ByteVector) w6.reinterpretShape(B, 0)).intoArray(out, op + 384);
        IntVector w7 = v7.lanewise(LSHR, 14);
        ((ShortVector) w7.convertShape(I2S, SH256, 0)).reinterpretAsBytes().intoArray(out, op + 448);
    }

    private static void pack0_31(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.and(2147483647);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.and(2147483647);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.and(2147483647);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.and(2147483647);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.and(2147483647);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.and(2147483647);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.and(2147483647);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.and(2147483647);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 31));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v1.lanewise(LSHR, 1).or(v2.lanewise(LSHL, 30));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v2.lanewise(LSHR, 2).or(v3.lanewise(LSHL, 29));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v3.lanewise(LSHR, 3).or(v4.lanewise(LSHL, 28));
        ((ByteVector) w3.reinterpretShape(B, 0)).intoArray(out, op + 192);
        IntVector w4 = v4.lanewise(LSHR, 4).or(v5.lanewise(LSHL, 27));
        ((ByteVector) w4.reinterpretShape(B, 0)).intoArray(out, op + 256);
        IntVector w5 = v5.lanewise(LSHR, 5).or(v6.lanewise(LSHL, 26));
        ((ByteVector) w5.reinterpretShape(B, 0)).intoArray(out, op + 320);
        IntVector w6 = v6.lanewise(LSHR, 6).or(v7.lanewise(LSHL, 25));
        ((ByteVector) w6.reinterpretShape(B, 0)).intoArray(out, op + 384);
        IntVector w7 = v7.lanewise(LSHR, 7);
        ((ByteVector) w7.reinterpretShape(B, 0)).rearrange(BYTE_COMPACT3).intoArray(out, op + 448, BYTE_TAIL3);
    }

    private static void pack1(int[] in, int ip, byte[] out, int op, int bits, int initial) {
        switch (bits) {
            case 32 -> pack1_32(in, ip, out, op, initial);
            case 1 -> pack1_1(in, ip, out, op, initial);
            case 2 -> pack1_2(in, ip, out, op, initial);
            case 3 -> pack1_3(in, ip, out, op, initial);
            case 4 -> pack1_4(in, ip, out, op, initial);
            case 5 -> pack1_5(in, ip, out, op, initial);
            case 6 -> pack1_6(in, ip, out, op, initial);
            case 7 -> pack1_7(in, ip, out, op, initial);
            case 8 -> pack1_8(in, ip, out, op, initial);
            case 9 -> pack1_9(in, ip, out, op, initial);
            case 10 -> pack1_10(in, ip, out, op, initial);
            case 11 -> pack1_11(in, ip, out, op, initial);
            case 12 -> pack1_12(in, ip, out, op, initial);
            case 13 -> pack1_13(in, ip, out, op, initial);
            case 14 -> pack1_14(in, ip, out, op, initial);
            case 15 -> pack1_15(in, ip, out, op, initial);
            case 16 -> pack1_16(in, ip, out, op, initial);
            case 17 -> pack1_17(in, ip, out, op, initial);
            case 18 -> pack1_18(in, ip, out, op, initial);
            case 19 -> pack1_19(in, ip, out, op, initial);
            case 20 -> pack1_20(in, ip, out, op, initial);
            case 21 -> pack1_21(in, ip, out, op, initial);
            case 22 -> pack1_22(in, ip, out, op, initial);
            case 23 -> pack1_23(in, ip, out, op, initial);
            case 24 -> pack1_24(in, ip, out, op, initial);
            case 25 -> pack1_25(in, ip, out, op, initial);
            case 26 -> pack1_26(in, ip, out, op, initial);
            case 27 -> pack1_27(in, ip, out, op, initial);
            case 28 -> pack1_28(in, ip, out, op, initial);
            case 29 -> pack1_29(in, ip, out, op, initial);
            case 30 -> pack1_30(in, ip, out, op, initial);
            case 31 -> pack1_31(in, ip, out, op, initial);
            default -> throw new IllegalArgumentException("Unsupported width");
        }
    }

    private static void pack1_1(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).and(1);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).and(1);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).and(1);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).and(1);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).and(1);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).and(1);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).and(1);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).and(1);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 1)).or(v2.lanewise(LSHL, 2)).or(v3.lanewise(LSHL, 3)).or(v4.lanewise(LSHL, 4))
            .or(v5.lanewise(LSHL, 5)).or(v6.lanewise(LSHL, 6)).or(v7.lanewise(LSHL, 7));
        ((ByteVector) w0.convertShape(I2B, B128, 0)).intoArray(out, op + 0);
    }

    private static void pack1_2(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).and(3);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).and(3);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).and(3);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).and(3);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).and(3);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).and(3);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).and(3);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).and(3);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 2)).or(v2.lanewise(LSHL, 4)).or(v3.lanewise(LSHL, 6)).or(v4.lanewise(LSHL, 8))
            .or(v5.lanewise(LSHL, 10)).or(v6.lanewise(LSHL, 12)).or(v7.lanewise(LSHL, 14));
        ((ShortVector) w0.convertShape(I2S, SH256, 0)).reinterpretAsBytes().intoArray(out, op + 0);
    }

    private static void pack1_3(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).and(7);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).and(7);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).and(7);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).and(7);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).and(7);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).and(7);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).and(7);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).and(7);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 3)).or(v2.lanewise(LSHL, 6)).or(v3.lanewise(LSHL, 9)).or(v4.lanewise(LSHL, 12))
            .or(v5.lanewise(LSHL, 15)).or(v6.lanewise(LSHL, 18)).or(v7.lanewise(LSHL, 21));
        ((ByteVector) w0.reinterpretShape(B, 0)).rearrange(BYTE_COMPACT3).intoArray(out, op + 0, BYTE_TAIL3);
    }

    private static void pack1_4(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).and(15);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).and(15);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).and(15);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).and(15);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).and(15);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).and(15);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).and(15);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).and(15);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 4)).or(v2.lanewise(LSHL, 8)).or(v3.lanewise(LSHL, 12)).or(v4.lanewise(LSHL, 16))
            .or(v5.lanewise(LSHL, 20)).or(v6.lanewise(LSHL, 24)).or(v7.lanewise(LSHL, 28));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
    }

    private static void pack1_5(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).and(31);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).and(31);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).and(31);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).and(31);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).and(31);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).and(31);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).and(31);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).and(31);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 5)).or(v2.lanewise(LSHL, 10)).or(v3.lanewise(LSHL, 15)).or(v4.lanewise(LSHL, 20))
            .or(v5.lanewise(LSHL, 25)).or(v6.lanewise(LSHL, 30));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v6.lanewise(LSHR, 2).or(v7.lanewise(LSHL, 3));
        ((ByteVector) w1.convertShape(I2B, B128, 0)).intoArray(out, op + 64);
    }

    private static void pack1_6(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).and(63);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).and(63);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).and(63);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).and(63);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).and(63);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).and(63);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).and(63);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).and(63);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 6)).or(v2.lanewise(LSHL, 12)).or(v3.lanewise(LSHL, 18)).or(v4.lanewise(LSHL, 24))
            .or(v5.lanewise(LSHL, 30));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v5.lanewise(LSHR, 2).or(v6.lanewise(LSHL, 4)).or(v7.lanewise(LSHL, 10));
        ((ShortVector) w1.convertShape(I2S, SH256, 0)).reinterpretAsBytes().intoArray(out, op + 64);
    }

    private static void pack1_7(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).and(127);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).and(127);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).and(127);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).and(127);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).and(127);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).and(127);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).and(127);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).and(127);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 7)).or(v2.lanewise(LSHL, 14)).or(v3.lanewise(LSHL, 21)).or(v4.lanewise(LSHL, 28));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v4.lanewise(LSHR, 4).or(v5.lanewise(LSHL, 3)).or(v6.lanewise(LSHL, 10)).or(v7.lanewise(LSHL, 17));
        ((ByteVector) w1.reinterpretShape(B, 0)).rearrange(BYTE_COMPACT3).intoArray(out, op + 64, BYTE_TAIL3);
    }

    private static void pack1_8(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).and(255);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).and(255);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).and(255);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).and(255);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).and(255);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).and(255);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).and(255);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).and(255);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 8)).or(v2.lanewise(LSHL, 16)).or(v3.lanewise(LSHL, 24));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v4.or(v5.lanewise(LSHL, 8)).or(v6.lanewise(LSHL, 16)).or(v7.lanewise(LSHL, 24));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
    }

    private static void pack1_9(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).and(511);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).and(511);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).and(511);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).and(511);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).and(511);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).and(511);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).and(511);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).and(511);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 9)).or(v2.lanewise(LSHL, 18)).or(v3.lanewise(LSHL, 27));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v3.lanewise(LSHR, 5).or(v4.lanewise(LSHL, 4)).or(v5.lanewise(LSHL, 13)).or(v6.lanewise(LSHL, 22))
            .or(v7.lanewise(LSHL, 31));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v7.lanewise(LSHR, 1);
        ((ByteVector) w2.convertShape(I2B, B128, 0)).intoArray(out, op + 128);
    }

    private static void pack1_10(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).and(1023);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).and(1023);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).and(1023);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).and(1023);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).and(1023);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).and(1023);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).and(1023);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).and(1023);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 10)).or(v2.lanewise(LSHL, 20)).or(v3.lanewise(LSHL, 30));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v3.lanewise(LSHR, 2).or(v4.lanewise(LSHL, 8)).or(v5.lanewise(LSHL, 18)).or(v6.lanewise(LSHL, 28));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v6.lanewise(LSHR, 4).or(v7.lanewise(LSHL, 6));
        ((ShortVector) w2.convertShape(I2S, SH256, 0)).reinterpretAsBytes().intoArray(out, op + 128);
    }

    private static void pack1_11(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).and(2047);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).and(2047);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).and(2047);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).and(2047);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).and(2047);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).and(2047);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).and(2047);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).and(2047);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 11)).or(v2.lanewise(LSHL, 22));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v2.lanewise(LSHR, 10).or(v3.lanewise(LSHL, 1)).or(v4.lanewise(LSHL, 12)).or(v5.lanewise(LSHL, 23));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v5.lanewise(LSHR, 9).or(v6.lanewise(LSHL, 2)).or(v7.lanewise(LSHL, 13));
        ((ByteVector) w2.reinterpretShape(B, 0)).rearrange(BYTE_COMPACT3).intoArray(out, op + 128, BYTE_TAIL3);
    }

    private static void pack1_12(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).and(4095);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).and(4095);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).and(4095);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).and(4095);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).and(4095);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).and(4095);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).and(4095);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).and(4095);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 12)).or(v2.lanewise(LSHL, 24));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v2.lanewise(LSHR, 8).or(v3.lanewise(LSHL, 4)).or(v4.lanewise(LSHL, 16)).or(v5.lanewise(LSHL, 28));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v5.lanewise(LSHR, 4).or(v6.lanewise(LSHL, 8)).or(v7.lanewise(LSHL, 20));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
    }

    private static void pack1_13(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).and(8191);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).and(8191);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).and(8191);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).and(8191);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).and(8191);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).and(8191);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).and(8191);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).and(8191);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 13)).or(v2.lanewise(LSHL, 26));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v2.lanewise(LSHR, 6).or(v3.lanewise(LSHL, 7)).or(v4.lanewise(LSHL, 20));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v4.lanewise(LSHR, 12).or(v5.lanewise(LSHL, 1)).or(v6.lanewise(LSHL, 14)).or(v7.lanewise(LSHL, 27));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v7.lanewise(LSHR, 5);
        ((ByteVector) w3.convertShape(I2B, B128, 0)).intoArray(out, op + 192);
    }

    private static void pack1_14(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).and(16383);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).and(16383);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).and(16383);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).and(16383);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).and(16383);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).and(16383);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).and(16383);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).and(16383);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 14)).or(v2.lanewise(LSHL, 28));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v2.lanewise(LSHR, 4).or(v3.lanewise(LSHL, 10)).or(v4.lanewise(LSHL, 24));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v4.lanewise(LSHR, 8).or(v5.lanewise(LSHL, 6)).or(v6.lanewise(LSHL, 20));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v6.lanewise(LSHR, 12).or(v7.lanewise(LSHL, 2));
        ((ShortVector) w3.convertShape(I2S, SH256, 0)).reinterpretAsBytes().intoArray(out, op + 192);
    }

    private static void pack1_15(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).and(32767);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).and(32767);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).and(32767);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).and(32767);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).and(32767);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).and(32767);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).and(32767);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).and(32767);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 15)).or(v2.lanewise(LSHL, 30));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v2.lanewise(LSHR, 2).or(v3.lanewise(LSHL, 13)).or(v4.lanewise(LSHL, 28));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v4.lanewise(LSHR, 4).or(v5.lanewise(LSHL, 11)).or(v6.lanewise(LSHL, 26));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v6.lanewise(LSHR, 6).or(v7.lanewise(LSHL, 9));
        ((ByteVector) w3.reinterpretShape(B, 0)).rearrange(BYTE_COMPACT3).intoArray(out, op + 192, BYTE_TAIL3);
    }

    private static void pack1_16(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).and(65535);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).and(65535);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).and(65535);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).and(65535);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).and(65535);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).and(65535);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).and(65535);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).and(65535);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 16));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v2.or(v3.lanewise(LSHL, 16));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v4.or(v5.lanewise(LSHL, 16));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v6.or(v7.lanewise(LSHL, 16));
        ((ByteVector) w3.reinterpretShape(B, 0)).intoArray(out, op + 192);
    }

    private static void pack1_17(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).and(131071);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).and(131071);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).and(131071);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).and(131071);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).and(131071);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).and(131071);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).and(131071);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).and(131071);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 17));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v1.lanewise(LSHR, 15).or(v2.lanewise(LSHL, 2)).or(v3.lanewise(LSHL, 19));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v3.lanewise(LSHR, 13).or(v4.lanewise(LSHL, 4)).or(v5.lanewise(LSHL, 21));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v5.lanewise(LSHR, 11).or(v6.lanewise(LSHL, 6)).or(v7.lanewise(LSHL, 23));
        ((ByteVector) w3.reinterpretShape(B, 0)).intoArray(out, op + 192);
        IntVector w4 = v7.lanewise(LSHR, 9);
        ((ByteVector) w4.convertShape(I2B, B128, 0)).intoArray(out, op + 256);
    }

    private static void pack1_18(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).and(262143);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).and(262143);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).and(262143);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).and(262143);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).and(262143);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).and(262143);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).and(262143);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).and(262143);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 18));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v1.lanewise(LSHR, 14).or(v2.lanewise(LSHL, 4)).or(v3.lanewise(LSHL, 22));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v3.lanewise(LSHR, 10).or(v4.lanewise(LSHL, 8)).or(v5.lanewise(LSHL, 26));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v5.lanewise(LSHR, 6).or(v6.lanewise(LSHL, 12)).or(v7.lanewise(LSHL, 30));
        ((ByteVector) w3.reinterpretShape(B, 0)).intoArray(out, op + 192);
        IntVector w4 = v7.lanewise(LSHR, 2);
        ((ShortVector) w4.convertShape(I2S, SH256, 0)).reinterpretAsBytes().intoArray(out, op + 256);
    }

    private static void pack1_19(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).and(524287);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).and(524287);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).and(524287);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).and(524287);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).and(524287);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).and(524287);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).and(524287);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).and(524287);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 19));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v1.lanewise(LSHR, 13).or(v2.lanewise(LSHL, 6)).or(v3.lanewise(LSHL, 25));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v3.lanewise(LSHR, 7).or(v4.lanewise(LSHL, 12)).or(v5.lanewise(LSHL, 31));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v5.lanewise(LSHR, 1).or(v6.lanewise(LSHL, 18));
        ((ByteVector) w3.reinterpretShape(B, 0)).intoArray(out, op + 192);
        IntVector w4 = v6.lanewise(LSHR, 14).or(v7.lanewise(LSHL, 5));
        ((ByteVector) w4.reinterpretShape(B, 0)).rearrange(BYTE_COMPACT3).intoArray(out, op + 256, BYTE_TAIL3);
    }

    private static void pack1_20(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).and(1048575);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).and(1048575);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).and(1048575);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).and(1048575);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).and(1048575);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).and(1048575);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).and(1048575);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).and(1048575);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 20));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v1.lanewise(LSHR, 12).or(v2.lanewise(LSHL, 8)).or(v3.lanewise(LSHL, 28));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v3.lanewise(LSHR, 4).or(v4.lanewise(LSHL, 16));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v4.lanewise(LSHR, 16).or(v5.lanewise(LSHL, 4)).or(v6.lanewise(LSHL, 24));
        ((ByteVector) w3.reinterpretShape(B, 0)).intoArray(out, op + 192);
        IntVector w4 = v6.lanewise(LSHR, 8).or(v7.lanewise(LSHL, 12));
        ((ByteVector) w4.reinterpretShape(B, 0)).intoArray(out, op + 256);
    }

    private static void pack1_21(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).and(2097151);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).and(2097151);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).and(2097151);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).and(2097151);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).and(2097151);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).and(2097151);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).and(2097151);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).and(2097151);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 21));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v1.lanewise(LSHR, 11).or(v2.lanewise(LSHL, 10)).or(v3.lanewise(LSHL, 31));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v3.lanewise(LSHR, 1).or(v4.lanewise(LSHL, 20));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v4.lanewise(LSHR, 12).or(v5.lanewise(LSHL, 9)).or(v6.lanewise(LSHL, 30));
        ((ByteVector) w3.reinterpretShape(B, 0)).intoArray(out, op + 192);
        IntVector w4 = v6.lanewise(LSHR, 2).or(v7.lanewise(LSHL, 19));
        ((ByteVector) w4.reinterpretShape(B, 0)).intoArray(out, op + 256);
        IntVector w5 = v7.lanewise(LSHR, 13);
        ((ByteVector) w5.convertShape(I2B, B128, 0)).intoArray(out, op + 320);
    }

    private static void pack1_22(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).and(4194303);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).and(4194303);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).and(4194303);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).and(4194303);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).and(4194303);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).and(4194303);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).and(4194303);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).and(4194303);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 22));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v1.lanewise(LSHR, 10).or(v2.lanewise(LSHL, 12));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v2.lanewise(LSHR, 20).or(v3.lanewise(LSHL, 2)).or(v4.lanewise(LSHL, 24));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v4.lanewise(LSHR, 8).or(v5.lanewise(LSHL, 14));
        ((ByteVector) w3.reinterpretShape(B, 0)).intoArray(out, op + 192);
        IntVector w4 = v5.lanewise(LSHR, 18).or(v6.lanewise(LSHL, 4)).or(v7.lanewise(LSHL, 26));
        ((ByteVector) w4.reinterpretShape(B, 0)).intoArray(out, op + 256);
        IntVector w5 = v7.lanewise(LSHR, 6);
        ((ShortVector) w5.convertShape(I2S, SH256, 0)).reinterpretAsBytes().intoArray(out, op + 320);
    }

    private static void pack1_23(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).and(8388607);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).and(8388607);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).and(8388607);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).and(8388607);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).and(8388607);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).and(8388607);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).and(8388607);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).and(8388607);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 23));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v1.lanewise(LSHR, 9).or(v2.lanewise(LSHL, 14));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v2.lanewise(LSHR, 18).or(v3.lanewise(LSHL, 5)).or(v4.lanewise(LSHL, 28));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v4.lanewise(LSHR, 4).or(v5.lanewise(LSHL, 19));
        ((ByteVector) w3.reinterpretShape(B, 0)).intoArray(out, op + 192);
        IntVector w4 = v5.lanewise(LSHR, 13).or(v6.lanewise(LSHL, 10));
        ((ByteVector) w4.reinterpretShape(B, 0)).intoArray(out, op + 256);
        IntVector w5 = v6.lanewise(LSHR, 22).or(v7.lanewise(LSHL, 1));
        ((ByteVector) w5.reinterpretShape(B, 0)).rearrange(BYTE_COMPACT3).intoArray(out, op + 320, BYTE_TAIL3);
    }

    private static void pack1_24(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).and(16777215);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).and(16777215);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).and(16777215);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).and(16777215);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).and(16777215);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).and(16777215);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).and(16777215);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).and(16777215);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 24));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v1.lanewise(LSHR, 8).or(v2.lanewise(LSHL, 16));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v2.lanewise(LSHR, 16).or(v3.lanewise(LSHL, 8));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v4.or(v5.lanewise(LSHL, 24));
        ((ByteVector) w3.reinterpretShape(B, 0)).intoArray(out, op + 192);
        IntVector w4 = v5.lanewise(LSHR, 8).or(v6.lanewise(LSHL, 16));
        ((ByteVector) w4.reinterpretShape(B, 0)).intoArray(out, op + 256);
        IntVector w5 = v6.lanewise(LSHR, 16).or(v7.lanewise(LSHL, 8));
        ((ByteVector) w5.reinterpretShape(B, 0)).intoArray(out, op + 320);
    }

    private static void pack1_25(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).and(33554431);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).and(33554431);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).and(33554431);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).and(33554431);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).and(33554431);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).and(33554431);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).and(33554431);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).and(33554431);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 25));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v1.lanewise(LSHR, 7).or(v2.lanewise(LSHL, 18));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v2.lanewise(LSHR, 14).or(v3.lanewise(LSHL, 11));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v3.lanewise(LSHR, 21).or(v4.lanewise(LSHL, 4)).or(v5.lanewise(LSHL, 29));
        ((ByteVector) w3.reinterpretShape(B, 0)).intoArray(out, op + 192);
        IntVector w4 = v5.lanewise(LSHR, 3).or(v6.lanewise(LSHL, 22));
        ((ByteVector) w4.reinterpretShape(B, 0)).intoArray(out, op + 256);
        IntVector w5 = v6.lanewise(LSHR, 10).or(v7.lanewise(LSHL, 15));
        ((ByteVector) w5.reinterpretShape(B, 0)).intoArray(out, op + 320);
        IntVector w6 = v7.lanewise(LSHR, 17);
        ((ByteVector) w6.convertShape(I2B, B128, 0)).intoArray(out, op + 384);
    }

    private static void pack1_26(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).and(67108863);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).and(67108863);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).and(67108863);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).and(67108863);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).and(67108863);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).and(67108863);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).and(67108863);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).and(67108863);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 26));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v1.lanewise(LSHR, 6).or(v2.lanewise(LSHL, 20));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v2.lanewise(LSHR, 12).or(v3.lanewise(LSHL, 14));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v3.lanewise(LSHR, 18).or(v4.lanewise(LSHL, 8));
        ((ByteVector) w3.reinterpretShape(B, 0)).intoArray(out, op + 192);
        IntVector w4 = v4.lanewise(LSHR, 24).or(v5.lanewise(LSHL, 2)).or(v6.lanewise(LSHL, 28));
        ((ByteVector) w4.reinterpretShape(B, 0)).intoArray(out, op + 256);
        IntVector w5 = v6.lanewise(LSHR, 4).or(v7.lanewise(LSHL, 22));
        ((ByteVector) w5.reinterpretShape(B, 0)).intoArray(out, op + 320);
        IntVector w6 = v7.lanewise(LSHR, 10);
        ((ShortVector) w6.convertShape(I2S, SH256, 0)).reinterpretAsBytes().intoArray(out, op + 384);
    }

    private static void pack1_27(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).and(134217727);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).and(134217727);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).and(134217727);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).and(134217727);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).and(134217727);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).and(134217727);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).and(134217727);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).and(134217727);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 27));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v1.lanewise(LSHR, 5).or(v2.lanewise(LSHL, 22));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v2.lanewise(LSHR, 10).or(v3.lanewise(LSHL, 17));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v3.lanewise(LSHR, 15).or(v4.lanewise(LSHL, 12));
        ((ByteVector) w3.reinterpretShape(B, 0)).intoArray(out, op + 192);
        IntVector w4 = v4.lanewise(LSHR, 20).or(v5.lanewise(LSHL, 7));
        ((ByteVector) w4.reinterpretShape(B, 0)).intoArray(out, op + 256);
        IntVector w5 = v5.lanewise(LSHR, 25).or(v6.lanewise(LSHL, 2)).or(v7.lanewise(LSHL, 29));
        ((ByteVector) w5.reinterpretShape(B, 0)).intoArray(out, op + 320);
        IntVector w6 = v7.lanewise(LSHR, 3);
        ((ByteVector) w6.reinterpretShape(B, 0)).rearrange(BYTE_COMPACT3).intoArray(out, op + 384, BYTE_TAIL3);
    }

    private static void pack1_28(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).and(268435455);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).and(268435455);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).and(268435455);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).and(268435455);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).and(268435455);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).and(268435455);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).and(268435455);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).and(268435455);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 28));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v1.lanewise(LSHR, 4).or(v2.lanewise(LSHL, 24));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v2.lanewise(LSHR, 8).or(v3.lanewise(LSHL, 20));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v3.lanewise(LSHR, 12).or(v4.lanewise(LSHL, 16));
        ((ByteVector) w3.reinterpretShape(B, 0)).intoArray(out, op + 192);
        IntVector w4 = v4.lanewise(LSHR, 16).or(v5.lanewise(LSHL, 12));
        ((ByteVector) w4.reinterpretShape(B, 0)).intoArray(out, op + 256);
        IntVector w5 = v5.lanewise(LSHR, 20).or(v6.lanewise(LSHL, 8));
        ((ByteVector) w5.reinterpretShape(B, 0)).intoArray(out, op + 320);
        IntVector w6 = v6.lanewise(LSHR, 24).or(v7.lanewise(LSHL, 4));
        ((ByteVector) w6.reinterpretShape(B, 0)).intoArray(out, op + 384);
    }

    private static void pack1_29(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).and(536870911);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).and(536870911);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).and(536870911);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).and(536870911);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).and(536870911);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).and(536870911);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).and(536870911);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).and(536870911);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 29));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v1.lanewise(LSHR, 3).or(v2.lanewise(LSHL, 26));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v2.lanewise(LSHR, 6).or(v3.lanewise(LSHL, 23));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v3.lanewise(LSHR, 9).or(v4.lanewise(LSHL, 20));
        ((ByteVector) w3.reinterpretShape(B, 0)).intoArray(out, op + 192);
        IntVector w4 = v4.lanewise(LSHR, 12).or(v5.lanewise(LSHL, 17));
        ((ByteVector) w4.reinterpretShape(B, 0)).intoArray(out, op + 256);
        IntVector w5 = v5.lanewise(LSHR, 15).or(v6.lanewise(LSHL, 14));
        ((ByteVector) w5.reinterpretShape(B, 0)).intoArray(out, op + 320);
        IntVector w6 = v6.lanewise(LSHR, 18).or(v7.lanewise(LSHL, 11));
        ((ByteVector) w6.reinterpretShape(B, 0)).intoArray(out, op + 384);
        IntVector w7 = v7.lanewise(LSHR, 21);
        ((ByteVector) w7.convertShape(I2B, B128, 0)).intoArray(out, op + 448);
    }

    private static void pack1_30(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).and(1073741823);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).and(1073741823);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).and(1073741823);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).and(1073741823);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).and(1073741823);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).and(1073741823);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).and(1073741823);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).and(1073741823);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 30));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v1.lanewise(LSHR, 2).or(v2.lanewise(LSHL, 28));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v2.lanewise(LSHR, 4).or(v3.lanewise(LSHL, 26));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v3.lanewise(LSHR, 6).or(v4.lanewise(LSHL, 24));
        ((ByteVector) w3.reinterpretShape(B, 0)).intoArray(out, op + 192);
        IntVector w4 = v4.lanewise(LSHR, 8).or(v5.lanewise(LSHL, 22));
        ((ByteVector) w4.reinterpretShape(B, 0)).intoArray(out, op + 256);
        IntVector w5 = v5.lanewise(LSHR, 10).or(v6.lanewise(LSHL, 20));
        ((ByteVector) w5.reinterpretShape(B, 0)).intoArray(out, op + 320);
        IntVector w6 = v6.lanewise(LSHR, 12).or(v7.lanewise(LSHL, 18));
        ((ByteVector) w6.reinterpretShape(B, 0)).intoArray(out, op + 384);
        IntVector w7 = v7.lanewise(LSHR, 14);
        ((ShortVector) w7.convertShape(I2S, SH256, 0)).reinterpretAsBytes().intoArray(out, op + 448);
    }

    private static void pack1_31(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).and(2147483647);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).and(2147483647);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).and(2147483647);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).and(2147483647);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).and(2147483647);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).and(2147483647);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).and(2147483647);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).and(2147483647);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 31));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v1.lanewise(LSHR, 1).or(v2.lanewise(LSHL, 30));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v2.lanewise(LSHR, 2).or(v3.lanewise(LSHL, 29));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v3.lanewise(LSHR, 3).or(v4.lanewise(LSHL, 28));
        ((ByteVector) w3.reinterpretShape(B, 0)).intoArray(out, op + 192);
        IntVector w4 = v4.lanewise(LSHR, 4).or(v5.lanewise(LSHL, 27));
        ((ByteVector) w4.reinterpretShape(B, 0)).intoArray(out, op + 256);
        IntVector w5 = v5.lanewise(LSHR, 5).or(v6.lanewise(LSHL, 26));
        ((ByteVector) w5.reinterpretShape(B, 0)).intoArray(out, op + 320);
        IntVector w6 = v6.lanewise(LSHR, 6).or(v7.lanewise(LSHL, 25));
        ((ByteVector) w6.reinterpretShape(B, 0)).intoArray(out, op + 384);
        IntVector w7 = v7.lanewise(LSHR, 7);
        ((ByteVector) w7.reinterpretShape(B, 0)).rearrange(BYTE_COMPACT3).intoArray(out, op + 448, BYTE_TAIL3);
    }

    private static void pack2(int[] in, int ip, byte[] out, int op, int bits, int initial) {
        switch (bits) {
            case 32 -> pack2_32(in, ip, out, op, initial);
            case 1 -> pack2_1(in, ip, out, op, initial);
            case 2 -> pack2_2(in, ip, out, op, initial);
            case 3 -> pack2_3(in, ip, out, op, initial);
            case 4 -> pack2_4(in, ip, out, op, initial);
            case 5 -> pack2_5(in, ip, out, op, initial);
            case 6 -> pack2_6(in, ip, out, op, initial);
            case 7 -> pack2_7(in, ip, out, op, initial);
            case 8 -> pack2_8(in, ip, out, op, initial);
            case 9 -> pack2_9(in, ip, out, op, initial);
            case 10 -> pack2_10(in, ip, out, op, initial);
            case 11 -> pack2_11(in, ip, out, op, initial);
            case 12 -> pack2_12(in, ip, out, op, initial);
            case 13 -> pack2_13(in, ip, out, op, initial);
            case 14 -> pack2_14(in, ip, out, op, initial);
            case 15 -> pack2_15(in, ip, out, op, initial);
            case 16 -> pack2_16(in, ip, out, op, initial);
            case 17 -> pack2_17(in, ip, out, op, initial);
            case 18 -> pack2_18(in, ip, out, op, initial);
            case 19 -> pack2_19(in, ip, out, op, initial);
            case 20 -> pack2_20(in, ip, out, op, initial);
            case 21 -> pack2_21(in, ip, out, op, initial);
            case 22 -> pack2_22(in, ip, out, op, initial);
            case 23 -> pack2_23(in, ip, out, op, initial);
            case 24 -> pack2_24(in, ip, out, op, initial);
            case 25 -> pack2_25(in, ip, out, op, initial);
            case 26 -> pack2_26(in, ip, out, op, initial);
            case 27 -> pack2_27(in, ip, out, op, initial);
            case 28 -> pack2_28(in, ip, out, op, initial);
            case 29 -> pack2_29(in, ip, out, op, initial);
            case 30 -> pack2_30(in, ip, out, op, initial);
            case 31 -> pack2_31(in, ip, out, op, initial);
            default -> throw new IllegalArgumentException("Unsupported width");
        }
    }

    private static void pack2_1(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).sub(ONE).and(1);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).sub(ONE).and(1);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).sub(ONE).and(1);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).sub(ONE).and(1);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).sub(ONE).and(1);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).sub(ONE).and(1);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).sub(ONE).and(1);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).sub(ONE).and(1);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 1)).or(v2.lanewise(LSHL, 2)).or(v3.lanewise(LSHL, 3)).or(v4.lanewise(LSHL, 4))
            .or(v5.lanewise(LSHL, 5)).or(v6.lanewise(LSHL, 6)).or(v7.lanewise(LSHL, 7));
        ((ByteVector) w0.convertShape(I2B, B128, 0)).intoArray(out, op + 0);
    }

    private static void pack2_2(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).sub(ONE).and(3);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).sub(ONE).and(3);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).sub(ONE).and(3);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).sub(ONE).and(3);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).sub(ONE).and(3);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).sub(ONE).and(3);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).sub(ONE).and(3);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).sub(ONE).and(3);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 2)).or(v2.lanewise(LSHL, 4)).or(v3.lanewise(LSHL, 6)).or(v4.lanewise(LSHL, 8))
            .or(v5.lanewise(LSHL, 10)).or(v6.lanewise(LSHL, 12)).or(v7.lanewise(LSHL, 14));
        ((ShortVector) w0.convertShape(I2S, SH256, 0)).reinterpretAsBytes().intoArray(out, op + 0);
    }

    private static void pack2_3(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).sub(ONE).and(7);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).sub(ONE).and(7);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).sub(ONE).and(7);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).sub(ONE).and(7);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).sub(ONE).and(7);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).sub(ONE).and(7);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).sub(ONE).and(7);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).sub(ONE).and(7);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 3)).or(v2.lanewise(LSHL, 6)).or(v3.lanewise(LSHL, 9)).or(v4.lanewise(LSHL, 12))
            .or(v5.lanewise(LSHL, 15)).or(v6.lanewise(LSHL, 18)).or(v7.lanewise(LSHL, 21));
        ((ByteVector) w0.reinterpretShape(B, 0)).rearrange(BYTE_COMPACT3).intoArray(out, op + 0, BYTE_TAIL3);
    }

    private static void pack2_4(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).sub(ONE).and(15);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).sub(ONE).and(15);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).sub(ONE).and(15);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).sub(ONE).and(15);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).sub(ONE).and(15);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).sub(ONE).and(15);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).sub(ONE).and(15);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).sub(ONE).and(15);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 4)).or(v2.lanewise(LSHL, 8)).or(v3.lanewise(LSHL, 12)).or(v4.lanewise(LSHL, 16))
            .or(v5.lanewise(LSHL, 20)).or(v6.lanewise(LSHL, 24)).or(v7.lanewise(LSHL, 28));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
    }

    private static void pack2_5(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).sub(ONE).and(31);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).sub(ONE).and(31);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).sub(ONE).and(31);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).sub(ONE).and(31);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).sub(ONE).and(31);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).sub(ONE).and(31);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).sub(ONE).and(31);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).sub(ONE).and(31);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 5)).or(v2.lanewise(LSHL, 10)).or(v3.lanewise(LSHL, 15)).or(v4.lanewise(LSHL, 20))
            .or(v5.lanewise(LSHL, 25)).or(v6.lanewise(LSHL, 30));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v6.lanewise(LSHR, 2).or(v7.lanewise(LSHL, 3));
        ((ByteVector) w1.convertShape(I2B, B128, 0)).intoArray(out, op + 64);
    }

    private static void pack2_6(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).sub(ONE).and(63);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).sub(ONE).and(63);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).sub(ONE).and(63);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).sub(ONE).and(63);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).sub(ONE).and(63);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).sub(ONE).and(63);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).sub(ONE).and(63);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).sub(ONE).and(63);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 6)).or(v2.lanewise(LSHL, 12)).or(v3.lanewise(LSHL, 18)).or(v4.lanewise(LSHL, 24))
            .or(v5.lanewise(LSHL, 30));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v5.lanewise(LSHR, 2).or(v6.lanewise(LSHL, 4)).or(v7.lanewise(LSHL, 10));
        ((ShortVector) w1.convertShape(I2S, SH256, 0)).reinterpretAsBytes().intoArray(out, op + 64);
    }

    private static void pack2_7(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).sub(ONE).and(127);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).sub(ONE).and(127);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).sub(ONE).and(127);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).sub(ONE).and(127);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).sub(ONE).and(127);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).sub(ONE).and(127);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).sub(ONE).and(127);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).sub(ONE).and(127);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 7)).or(v2.lanewise(LSHL, 14)).or(v3.lanewise(LSHL, 21)).or(v4.lanewise(LSHL, 28));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v4.lanewise(LSHR, 4).or(v5.lanewise(LSHL, 3)).or(v6.lanewise(LSHL, 10)).or(v7.lanewise(LSHL, 17));
        ((ByteVector) w1.reinterpretShape(B, 0)).rearrange(BYTE_COMPACT3).intoArray(out, op + 64, BYTE_TAIL3);
    }

    private static void pack2_8(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).sub(ONE).and(255);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).sub(ONE).and(255);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).sub(ONE).and(255);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).sub(ONE).and(255);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).sub(ONE).and(255);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).sub(ONE).and(255);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).sub(ONE).and(255);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).sub(ONE).and(255);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 8)).or(v2.lanewise(LSHL, 16)).or(v3.lanewise(LSHL, 24));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v4.or(v5.lanewise(LSHL, 8)).or(v6.lanewise(LSHL, 16)).or(v7.lanewise(LSHL, 24));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
    }

    private static void pack2_9(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).sub(ONE).and(511);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).sub(ONE).and(511);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).sub(ONE).and(511);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).sub(ONE).and(511);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).sub(ONE).and(511);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).sub(ONE).and(511);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).sub(ONE).and(511);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).sub(ONE).and(511);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 9)).or(v2.lanewise(LSHL, 18)).or(v3.lanewise(LSHL, 27));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v3.lanewise(LSHR, 5).or(v4.lanewise(LSHL, 4)).or(v5.lanewise(LSHL, 13)).or(v6.lanewise(LSHL, 22))
            .or(v7.lanewise(LSHL, 31));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v7.lanewise(LSHR, 1);
        ((ByteVector) w2.convertShape(I2B, B128, 0)).intoArray(out, op + 128);
    }

    private static void pack2_10(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).sub(ONE).and(1023);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).sub(ONE).and(1023);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).sub(ONE).and(1023);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).sub(ONE).and(1023);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).sub(ONE).and(1023);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).sub(ONE).and(1023);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).sub(ONE).and(1023);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).sub(ONE).and(1023);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 10)).or(v2.lanewise(LSHL, 20)).or(v3.lanewise(LSHL, 30));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v3.lanewise(LSHR, 2).or(v4.lanewise(LSHL, 8)).or(v5.lanewise(LSHL, 18)).or(v6.lanewise(LSHL, 28));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v6.lanewise(LSHR, 4).or(v7.lanewise(LSHL, 6));
        ((ShortVector) w2.convertShape(I2S, SH256, 0)).reinterpretAsBytes().intoArray(out, op + 128);
    }

    private static void pack2_11(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).sub(ONE).and(2047);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).sub(ONE).and(2047);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).sub(ONE).and(2047);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).sub(ONE).and(2047);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).sub(ONE).and(2047);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).sub(ONE).and(2047);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).sub(ONE).and(2047);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).sub(ONE).and(2047);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 11)).or(v2.lanewise(LSHL, 22));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v2.lanewise(LSHR, 10).or(v3.lanewise(LSHL, 1)).or(v4.lanewise(LSHL, 12)).or(v5.lanewise(LSHL, 23));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v5.lanewise(LSHR, 9).or(v6.lanewise(LSHL, 2)).or(v7.lanewise(LSHL, 13));
        ((ByteVector) w2.reinterpretShape(B, 0)).rearrange(BYTE_COMPACT3).intoArray(out, op + 128, BYTE_TAIL3);
    }

    private static void pack2_12(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).sub(ONE).and(4095);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).sub(ONE).and(4095);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).sub(ONE).and(4095);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).sub(ONE).and(4095);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).sub(ONE).and(4095);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).sub(ONE).and(4095);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).sub(ONE).and(4095);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).sub(ONE).and(4095);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 12)).or(v2.lanewise(LSHL, 24));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v2.lanewise(LSHR, 8).or(v3.lanewise(LSHL, 4)).or(v4.lanewise(LSHL, 16)).or(v5.lanewise(LSHL, 28));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v5.lanewise(LSHR, 4).or(v6.lanewise(LSHL, 8)).or(v7.lanewise(LSHL, 20));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
    }

    private static void pack2_13(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).sub(ONE).and(8191);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).sub(ONE).and(8191);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).sub(ONE).and(8191);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).sub(ONE).and(8191);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).sub(ONE).and(8191);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).sub(ONE).and(8191);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).sub(ONE).and(8191);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).sub(ONE).and(8191);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 13)).or(v2.lanewise(LSHL, 26));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v2.lanewise(LSHR, 6).or(v3.lanewise(LSHL, 7)).or(v4.lanewise(LSHL, 20));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v4.lanewise(LSHR, 12).or(v5.lanewise(LSHL, 1)).or(v6.lanewise(LSHL, 14)).or(v7.lanewise(LSHL, 27));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v7.lanewise(LSHR, 5);
        ((ByteVector) w3.convertShape(I2B, B128, 0)).intoArray(out, op + 192);
    }

    private static void pack2_14(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).sub(ONE).and(16383);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).sub(ONE).and(16383);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).sub(ONE).and(16383);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).sub(ONE).and(16383);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).sub(ONE).and(16383);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).sub(ONE).and(16383);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).sub(ONE).and(16383);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).sub(ONE).and(16383);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 14)).or(v2.lanewise(LSHL, 28));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v2.lanewise(LSHR, 4).or(v3.lanewise(LSHL, 10)).or(v4.lanewise(LSHL, 24));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v4.lanewise(LSHR, 8).or(v5.lanewise(LSHL, 6)).or(v6.lanewise(LSHL, 20));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v6.lanewise(LSHR, 12).or(v7.lanewise(LSHL, 2));
        ((ShortVector) w3.convertShape(I2S, SH256, 0)).reinterpretAsBytes().intoArray(out, op + 192);
    }

    private static void pack2_15(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).sub(ONE).and(32767);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).sub(ONE).and(32767);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).sub(ONE).and(32767);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).sub(ONE).and(32767);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).sub(ONE).and(32767);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).sub(ONE).and(32767);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).sub(ONE).and(32767);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).sub(ONE).and(32767);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 15)).or(v2.lanewise(LSHL, 30));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v2.lanewise(LSHR, 2).or(v3.lanewise(LSHL, 13)).or(v4.lanewise(LSHL, 28));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v4.lanewise(LSHR, 4).or(v5.lanewise(LSHL, 11)).or(v6.lanewise(LSHL, 26));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v6.lanewise(LSHR, 6).or(v7.lanewise(LSHL, 9));
        ((ByteVector) w3.reinterpretShape(B, 0)).rearrange(BYTE_COMPACT3).intoArray(out, op + 192, BYTE_TAIL3);
    }

    private static void pack2_16(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).sub(ONE).and(65535);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).sub(ONE).and(65535);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).sub(ONE).and(65535);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).sub(ONE).and(65535);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).sub(ONE).and(65535);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).sub(ONE).and(65535);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).sub(ONE).and(65535);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).sub(ONE).and(65535);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 16));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v2.or(v3.lanewise(LSHL, 16));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v4.or(v5.lanewise(LSHL, 16));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v6.or(v7.lanewise(LSHL, 16));
        ((ByteVector) w3.reinterpretShape(B, 0)).intoArray(out, op + 192);
    }

    private static void pack2_17(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).sub(ONE).and(131071);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).sub(ONE).and(131071);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).sub(ONE).and(131071);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).sub(ONE).and(131071);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).sub(ONE).and(131071);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).sub(ONE).and(131071);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).sub(ONE).and(131071);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).sub(ONE).and(131071);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 17));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v1.lanewise(LSHR, 15).or(v2.lanewise(LSHL, 2)).or(v3.lanewise(LSHL, 19));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v3.lanewise(LSHR, 13).or(v4.lanewise(LSHL, 4)).or(v5.lanewise(LSHL, 21));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v5.lanewise(LSHR, 11).or(v6.lanewise(LSHL, 6)).or(v7.lanewise(LSHL, 23));
        ((ByteVector) w3.reinterpretShape(B, 0)).intoArray(out, op + 192);
        IntVector w4 = v7.lanewise(LSHR, 9);
        ((ByteVector) w4.convertShape(I2B, B128, 0)).intoArray(out, op + 256);
    }

    private static void pack2_18(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).sub(ONE).and(262143);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).sub(ONE).and(262143);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).sub(ONE).and(262143);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).sub(ONE).and(262143);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).sub(ONE).and(262143);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).sub(ONE).and(262143);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).sub(ONE).and(262143);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).sub(ONE).and(262143);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 18));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v1.lanewise(LSHR, 14).or(v2.lanewise(LSHL, 4)).or(v3.lanewise(LSHL, 22));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v3.lanewise(LSHR, 10).or(v4.lanewise(LSHL, 8)).or(v5.lanewise(LSHL, 26));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v5.lanewise(LSHR, 6).or(v6.lanewise(LSHL, 12)).or(v7.lanewise(LSHL, 30));
        ((ByteVector) w3.reinterpretShape(B, 0)).intoArray(out, op + 192);
        IntVector w4 = v7.lanewise(LSHR, 2);
        ((ShortVector) w4.convertShape(I2S, SH256, 0)).reinterpretAsBytes().intoArray(out, op + 256);
    }

    private static void pack2_19(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).sub(ONE).and(524287);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).sub(ONE).and(524287);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).sub(ONE).and(524287);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).sub(ONE).and(524287);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).sub(ONE).and(524287);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).sub(ONE).and(524287);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).sub(ONE).and(524287);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).sub(ONE).and(524287);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 19));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v1.lanewise(LSHR, 13).or(v2.lanewise(LSHL, 6)).or(v3.lanewise(LSHL, 25));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v3.lanewise(LSHR, 7).or(v4.lanewise(LSHL, 12)).or(v5.lanewise(LSHL, 31));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v5.lanewise(LSHR, 1).or(v6.lanewise(LSHL, 18));
        ((ByteVector) w3.reinterpretShape(B, 0)).intoArray(out, op + 192);
        IntVector w4 = v6.lanewise(LSHR, 14).or(v7.lanewise(LSHL, 5));
        ((ByteVector) w4.reinterpretShape(B, 0)).rearrange(BYTE_COMPACT3).intoArray(out, op + 256, BYTE_TAIL3);
    }

    private static void pack2_20(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).sub(ONE).and(1048575);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).sub(ONE).and(1048575);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).sub(ONE).and(1048575);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).sub(ONE).and(1048575);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).sub(ONE).and(1048575);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).sub(ONE).and(1048575);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).sub(ONE).and(1048575);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).sub(ONE).and(1048575);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 20));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v1.lanewise(LSHR, 12).or(v2.lanewise(LSHL, 8)).or(v3.lanewise(LSHL, 28));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v3.lanewise(LSHR, 4).or(v4.lanewise(LSHL, 16));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v4.lanewise(LSHR, 16).or(v5.lanewise(LSHL, 4)).or(v6.lanewise(LSHL, 24));
        ((ByteVector) w3.reinterpretShape(B, 0)).intoArray(out, op + 192);
        IntVector w4 = v6.lanewise(LSHR, 8).or(v7.lanewise(LSHL, 12));
        ((ByteVector) w4.reinterpretShape(B, 0)).intoArray(out, op + 256);
    }

    private static void pack2_21(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).sub(ONE).and(2097151);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).sub(ONE).and(2097151);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).sub(ONE).and(2097151);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).sub(ONE).and(2097151);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).sub(ONE).and(2097151);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).sub(ONE).and(2097151);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).sub(ONE).and(2097151);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).sub(ONE).and(2097151);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 21));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v1.lanewise(LSHR, 11).or(v2.lanewise(LSHL, 10)).or(v3.lanewise(LSHL, 31));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v3.lanewise(LSHR, 1).or(v4.lanewise(LSHL, 20));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v4.lanewise(LSHR, 12).or(v5.lanewise(LSHL, 9)).or(v6.lanewise(LSHL, 30));
        ((ByteVector) w3.reinterpretShape(B, 0)).intoArray(out, op + 192);
        IntVector w4 = v6.lanewise(LSHR, 2).or(v7.lanewise(LSHL, 19));
        ((ByteVector) w4.reinterpretShape(B, 0)).intoArray(out, op + 256);
        IntVector w5 = v7.lanewise(LSHR, 13);
        ((ByteVector) w5.convertShape(I2B, B128, 0)).intoArray(out, op + 320);
    }

    private static void pack2_22(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).sub(ONE).and(4194303);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).sub(ONE).and(4194303);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).sub(ONE).and(4194303);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).sub(ONE).and(4194303);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).sub(ONE).and(4194303);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).sub(ONE).and(4194303);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).sub(ONE).and(4194303);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).sub(ONE).and(4194303);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 22));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v1.lanewise(LSHR, 10).or(v2.lanewise(LSHL, 12));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v2.lanewise(LSHR, 20).or(v3.lanewise(LSHL, 2)).or(v4.lanewise(LSHL, 24));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v4.lanewise(LSHR, 8).or(v5.lanewise(LSHL, 14));
        ((ByteVector) w3.reinterpretShape(B, 0)).intoArray(out, op + 192);
        IntVector w4 = v5.lanewise(LSHR, 18).or(v6.lanewise(LSHL, 4)).or(v7.lanewise(LSHL, 26));
        ((ByteVector) w4.reinterpretShape(B, 0)).intoArray(out, op + 256);
        IntVector w5 = v7.lanewise(LSHR, 6);
        ((ShortVector) w5.convertShape(I2S, SH256, 0)).reinterpretAsBytes().intoArray(out, op + 320);
    }

    private static void pack2_23(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).sub(ONE).and(8388607);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).sub(ONE).and(8388607);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).sub(ONE).and(8388607);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).sub(ONE).and(8388607);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).sub(ONE).and(8388607);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).sub(ONE).and(8388607);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).sub(ONE).and(8388607);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).sub(ONE).and(8388607);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 23));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v1.lanewise(LSHR, 9).or(v2.lanewise(LSHL, 14));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v2.lanewise(LSHR, 18).or(v3.lanewise(LSHL, 5)).or(v4.lanewise(LSHL, 28));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v4.lanewise(LSHR, 4).or(v5.lanewise(LSHL, 19));
        ((ByteVector) w3.reinterpretShape(B, 0)).intoArray(out, op + 192);
        IntVector w4 = v5.lanewise(LSHR, 13).or(v6.lanewise(LSHL, 10));
        ((ByteVector) w4.reinterpretShape(B, 0)).intoArray(out, op + 256);
        IntVector w5 = v6.lanewise(LSHR, 22).or(v7.lanewise(LSHL, 1));
        ((ByteVector) w5.reinterpretShape(B, 0)).rearrange(BYTE_COMPACT3).intoArray(out, op + 320, BYTE_TAIL3);
    }

    private static void pack2_24(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).sub(ONE).and(16777215);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).sub(ONE).and(16777215);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).sub(ONE).and(16777215);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).sub(ONE).and(16777215);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).sub(ONE).and(16777215);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).sub(ONE).and(16777215);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).sub(ONE).and(16777215);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).sub(ONE).and(16777215);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 24));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v1.lanewise(LSHR, 8).or(v2.lanewise(LSHL, 16));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v2.lanewise(LSHR, 16).or(v3.lanewise(LSHL, 8));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v4.or(v5.lanewise(LSHL, 24));
        ((ByteVector) w3.reinterpretShape(B, 0)).intoArray(out, op + 192);
        IntVector w4 = v5.lanewise(LSHR, 8).or(v6.lanewise(LSHL, 16));
        ((ByteVector) w4.reinterpretShape(B, 0)).intoArray(out, op + 256);
        IntVector w5 = v6.lanewise(LSHR, 16).or(v7.lanewise(LSHL, 8));
        ((ByteVector) w5.reinterpretShape(B, 0)).intoArray(out, op + 320);
    }

    private static void pack2_25(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).sub(ONE).and(33554431);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).sub(ONE).and(33554431);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).sub(ONE).and(33554431);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).sub(ONE).and(33554431);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).sub(ONE).and(33554431);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).sub(ONE).and(33554431);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).sub(ONE).and(33554431);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).sub(ONE).and(33554431);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 25));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v1.lanewise(LSHR, 7).or(v2.lanewise(LSHL, 18));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v2.lanewise(LSHR, 14).or(v3.lanewise(LSHL, 11));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v3.lanewise(LSHR, 21).or(v4.lanewise(LSHL, 4)).or(v5.lanewise(LSHL, 29));
        ((ByteVector) w3.reinterpretShape(B, 0)).intoArray(out, op + 192);
        IntVector w4 = v5.lanewise(LSHR, 3).or(v6.lanewise(LSHL, 22));
        ((ByteVector) w4.reinterpretShape(B, 0)).intoArray(out, op + 256);
        IntVector w5 = v6.lanewise(LSHR, 10).or(v7.lanewise(LSHL, 15));
        ((ByteVector) w5.reinterpretShape(B, 0)).intoArray(out, op + 320);
        IntVector w6 = v7.lanewise(LSHR, 17);
        ((ByteVector) w6.convertShape(I2B, B128, 0)).intoArray(out, op + 384);
    }

    private static void pack2_26(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).sub(ONE).and(67108863);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).sub(ONE).and(67108863);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).sub(ONE).and(67108863);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).sub(ONE).and(67108863);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).sub(ONE).and(67108863);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).sub(ONE).and(67108863);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).sub(ONE).and(67108863);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).sub(ONE).and(67108863);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 26));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v1.lanewise(LSHR, 6).or(v2.lanewise(LSHL, 20));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v2.lanewise(LSHR, 12).or(v3.lanewise(LSHL, 14));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v3.lanewise(LSHR, 18).or(v4.lanewise(LSHL, 8));
        ((ByteVector) w3.reinterpretShape(B, 0)).intoArray(out, op + 192);
        IntVector w4 = v4.lanewise(LSHR, 24).or(v5.lanewise(LSHL, 2)).or(v6.lanewise(LSHL, 28));
        ((ByteVector) w4.reinterpretShape(B, 0)).intoArray(out, op + 256);
        IntVector w5 = v6.lanewise(LSHR, 4).or(v7.lanewise(LSHL, 22));
        ((ByteVector) w5.reinterpretShape(B, 0)).intoArray(out, op + 320);
        IntVector w6 = v7.lanewise(LSHR, 10);
        ((ShortVector) w6.convertShape(I2S, SH256, 0)).reinterpretAsBytes().intoArray(out, op + 384);
    }

    private static void pack2_27(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).sub(ONE).and(134217727);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).sub(ONE).and(134217727);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).sub(ONE).and(134217727);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).sub(ONE).and(134217727);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).sub(ONE).and(134217727);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).sub(ONE).and(134217727);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).sub(ONE).and(134217727);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).sub(ONE).and(134217727);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 27));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v1.lanewise(LSHR, 5).or(v2.lanewise(LSHL, 22));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v2.lanewise(LSHR, 10).or(v3.lanewise(LSHL, 17));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v3.lanewise(LSHR, 15).or(v4.lanewise(LSHL, 12));
        ((ByteVector) w3.reinterpretShape(B, 0)).intoArray(out, op + 192);
        IntVector w4 = v4.lanewise(LSHR, 20).or(v5.lanewise(LSHL, 7));
        ((ByteVector) w4.reinterpretShape(B, 0)).intoArray(out, op + 256);
        IntVector w5 = v5.lanewise(LSHR, 25).or(v6.lanewise(LSHL, 2)).or(v7.lanewise(LSHL, 29));
        ((ByteVector) w5.reinterpretShape(B, 0)).intoArray(out, op + 320);
        IntVector w6 = v7.lanewise(LSHR, 3);
        ((ByteVector) w6.reinterpretShape(B, 0)).rearrange(BYTE_COMPACT3).intoArray(out, op + 384, BYTE_TAIL3);
    }

    private static void pack2_28(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).sub(ONE).and(268435455);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).sub(ONE).and(268435455);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).sub(ONE).and(268435455);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).sub(ONE).and(268435455);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).sub(ONE).and(268435455);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).sub(ONE).and(268435455);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).sub(ONE).and(268435455);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).sub(ONE).and(268435455);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 28));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v1.lanewise(LSHR, 4).or(v2.lanewise(LSHL, 24));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v2.lanewise(LSHR, 8).or(v3.lanewise(LSHL, 20));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v3.lanewise(LSHR, 12).or(v4.lanewise(LSHL, 16));
        ((ByteVector) w3.reinterpretShape(B, 0)).intoArray(out, op + 192);
        IntVector w4 = v4.lanewise(LSHR, 16).or(v5.lanewise(LSHL, 12));
        ((ByteVector) w4.reinterpretShape(B, 0)).intoArray(out, op + 256);
        IntVector w5 = v5.lanewise(LSHR, 20).or(v6.lanewise(LSHL, 8));
        ((ByteVector) w5.reinterpretShape(B, 0)).intoArray(out, op + 320);
        IntVector w6 = v6.lanewise(LSHR, 24).or(v7.lanewise(LSHL, 4));
        ((ByteVector) w6.reinterpretShape(B, 0)).intoArray(out, op + 384);
    }

    private static void pack2_29(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).sub(ONE).and(536870911);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).sub(ONE).and(536870911);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).sub(ONE).and(536870911);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).sub(ONE).and(536870911);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).sub(ONE).and(536870911);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).sub(ONE).and(536870911);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).sub(ONE).and(536870911);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).sub(ONE).and(536870911);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 29));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v1.lanewise(LSHR, 3).or(v2.lanewise(LSHL, 26));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v2.lanewise(LSHR, 6).or(v3.lanewise(LSHL, 23));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v3.lanewise(LSHR, 9).or(v4.lanewise(LSHL, 20));
        ((ByteVector) w3.reinterpretShape(B, 0)).intoArray(out, op + 192);
        IntVector w4 = v4.lanewise(LSHR, 12).or(v5.lanewise(LSHL, 17));
        ((ByteVector) w4.reinterpretShape(B, 0)).intoArray(out, op + 256);
        IntVector w5 = v5.lanewise(LSHR, 15).or(v6.lanewise(LSHL, 14));
        ((ByteVector) w5.reinterpretShape(B, 0)).intoArray(out, op + 320);
        IntVector w6 = v6.lanewise(LSHR, 18).or(v7.lanewise(LSHL, 11));
        ((ByteVector) w6.reinterpretShape(B, 0)).intoArray(out, op + 384);
        IntVector w7 = v7.lanewise(LSHR, 21);
        ((ByteVector) w7.convertShape(I2B, B128, 0)).intoArray(out, op + 448);
    }

    private static void pack2_30(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).sub(ONE).and(1073741823);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).sub(ONE).and(1073741823);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).sub(ONE).and(1073741823);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).sub(ONE).and(1073741823);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).sub(ONE).and(1073741823);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).sub(ONE).and(1073741823);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).sub(ONE).and(1073741823);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).sub(ONE).and(1073741823);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 30));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v1.lanewise(LSHR, 2).or(v2.lanewise(LSHL, 28));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v2.lanewise(LSHR, 4).or(v3.lanewise(LSHL, 26));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v3.lanewise(LSHR, 6).or(v4.lanewise(LSHL, 24));
        ((ByteVector) w3.reinterpretShape(B, 0)).intoArray(out, op + 192);
        IntVector w4 = v4.lanewise(LSHR, 8).or(v5.lanewise(LSHL, 22));
        ((ByteVector) w4.reinterpretShape(B, 0)).intoArray(out, op + 256);
        IntVector w5 = v5.lanewise(LSHR, 10).or(v6.lanewise(LSHL, 20));
        ((ByteVector) w5.reinterpretShape(B, 0)).intoArray(out, op + 320);
        IntVector w6 = v6.lanewise(LSHR, 12).or(v7.lanewise(LSHL, 18));
        ((ByteVector) w6.reinterpretShape(B, 0)).intoArray(out, op + 384);
        IntVector w7 = v7.lanewise(LSHR, 14);
        ((ShortVector) w7.convertShape(I2S, SH256, 0)).reinterpretAsBytes().intoArray(out, op + 448);
    }

    private static void pack2_31(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector initialVector = IntVector.broadcast(S, initial);
        IntVector raw0 = IntVector.fromArray(S, in, ip + 0);
        IntVector v0 = raw0.sub(PREVIOUS_INDEX.selectFrom(initialVector, raw0)).sub(ONE).and(2147483647);
        IntVector raw1 = IntVector.fromArray(S, in, ip + 16);
        IntVector v1 = raw1.sub(PREVIOUS_INDEX.selectFrom(raw0, raw1)).sub(ONE).and(2147483647);
        IntVector raw2 = IntVector.fromArray(S, in, ip + 32);
        IntVector v2 = raw2.sub(PREVIOUS_INDEX.selectFrom(raw1, raw2)).sub(ONE).and(2147483647);
        IntVector raw3 = IntVector.fromArray(S, in, ip + 48);
        IntVector v3 = raw3.sub(PREVIOUS_INDEX.selectFrom(raw2, raw3)).sub(ONE).and(2147483647);
        IntVector raw4 = IntVector.fromArray(S, in, ip + 64);
        IntVector v4 = raw4.sub(PREVIOUS_INDEX.selectFrom(raw3, raw4)).sub(ONE).and(2147483647);
        IntVector raw5 = IntVector.fromArray(S, in, ip + 80);
        IntVector v5 = raw5.sub(PREVIOUS_INDEX.selectFrom(raw4, raw5)).sub(ONE).and(2147483647);
        IntVector raw6 = IntVector.fromArray(S, in, ip + 96);
        IntVector v6 = raw6.sub(PREVIOUS_INDEX.selectFrom(raw5, raw6)).sub(ONE).and(2147483647);
        IntVector raw7 = IntVector.fromArray(S, in, ip + 112);
        IntVector v7 = raw7.sub(PREVIOUS_INDEX.selectFrom(raw6, raw7)).sub(ONE).and(2147483647);
        IntVector w0 = v0.or(v1.lanewise(LSHL, 31));
        ((ByteVector) w0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector w1 = v1.lanewise(LSHR, 1).or(v2.lanewise(LSHL, 30));
        ((ByteVector) w1.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector w2 = v2.lanewise(LSHR, 2).or(v3.lanewise(LSHL, 29));
        ((ByteVector) w2.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector w3 = v3.lanewise(LSHR, 3).or(v4.lanewise(LSHL, 28));
        ((ByteVector) w3.reinterpretShape(B, 0)).intoArray(out, op + 192);
        IntVector w4 = v4.lanewise(LSHR, 4).or(v5.lanewise(LSHL, 27));
        ((ByteVector) w4.reinterpretShape(B, 0)).intoArray(out, op + 256);
        IntVector w5 = v5.lanewise(LSHR, 5).or(v6.lanewise(LSHL, 26));
        ((ByteVector) w5.reinterpretShape(B, 0)).intoArray(out, op + 320);
        IntVector w6 = v6.lanewise(LSHR, 6).or(v7.lanewise(LSHL, 25));
        ((ByteVector) w6.reinterpretShape(B, 0)).intoArray(out, op + 384);
        IntVector w7 = v7.lanewise(LSHR, 7);
        ((ByteVector) w7.reinterpretShape(B, 0)).rearrange(BYTE_COMPACT3).intoArray(out, op + 448, BYTE_TAIL3);
    }

    private static void unpack0(byte[] in, int ip, int[] out, int op, int bits, int initial) {
        switch (bits) {
            case 32 -> unpack0_32(in, ip, out, op, initial);
            case 1 -> unpack0_1(in, ip, out, op, initial);
            case 2 -> unpack0_2(in, ip, out, op, initial);
            case 3 -> unpack0_3(in, ip, out, op, initial);
            case 4 -> unpack0_4(in, ip, out, op, initial);
            case 5 -> unpack0_5(in, ip, out, op, initial);
            case 6 -> unpack0_6(in, ip, out, op, initial);
            case 7 -> unpack0_7(in, ip, out, op, initial);
            case 8 -> unpack0_8(in, ip, out, op, initial);
            case 9 -> unpack0_9(in, ip, out, op, initial);
            case 10 -> unpack0_10(in, ip, out, op, initial);
            case 11 -> unpack0_11(in, ip, out, op, initial);
            case 12 -> unpack0_12(in, ip, out, op, initial);
            case 13 -> unpack0_13(in, ip, out, op, initial);
            case 14 -> unpack0_14(in, ip, out, op, initial);
            case 15 -> unpack0_15(in, ip, out, op, initial);
            case 16 -> unpack0_16(in, ip, out, op, initial);
            case 17 -> unpack0_17(in, ip, out, op, initial);
            case 18 -> unpack0_18(in, ip, out, op, initial);
            case 19 -> unpack0_19(in, ip, out, op, initial);
            case 20 -> unpack0_20(in, ip, out, op, initial);
            case 21 -> unpack0_21(in, ip, out, op, initial);
            case 22 -> unpack0_22(in, ip, out, op, initial);
            case 23 -> unpack0_23(in, ip, out, op, initial);
            case 24 -> unpack0_24(in, ip, out, op, initial);
            case 25 -> unpack0_25(in, ip, out, op, initial);
            case 26 -> unpack0_26(in, ip, out, op, initial);
            case 27 -> unpack0_27(in, ip, out, op, initial);
            case 28 -> unpack0_28(in, ip, out, op, initial);
            case 29 -> unpack0_29(in, ip, out, op, initial);
            case 30 -> unpack0_30(in, ip, out, op, initial);
            case 31 -> unpack0_31(in, ip, out, op, initial);
            default -> throw new IllegalArgumentException("Unsupported width");
        }
    }

    private static void unpack0_1(byte[] in, int ip, int[] out, int op, int initial) {
        unpack0_1Part0(in, ip, out, op, initial);
        unpack0_1Part1(in, ip, out, op, initial);
    }

    private static void unpack0_1Part0(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B128, in, ip + 0).convertShape(ZERO_EXTEND_B2I, S, 0);
        IntVector v0 = w0.and(1);
        v0.intoArray(out, op + 0);
        IntVector v1 = w0.lanewise(LSHR, 1).and(1);
        v1.intoArray(out, op + 16);
        IntVector v2 = w0.lanewise(LSHR, 2).and(1);
        v2.intoArray(out, op + 32);
        IntVector v3 = w0.lanewise(LSHR, 3).and(1);
        v3.intoArray(out, op + 48);
    }

    private static void unpack0_1Part1(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B128, in, ip + 0).convertShape(ZERO_EXTEND_B2I, S, 0);
        IntVector v4 = w0.lanewise(LSHR, 4).and(1);
        v4.intoArray(out, op + 64);
        IntVector v5 = w0.lanewise(LSHR, 5).and(1);
        v5.intoArray(out, op + 80);
        IntVector v6 = w0.lanewise(LSHR, 6).and(1);
        v6.intoArray(out, op + 96);
        IntVector v7 = w0.lanewise(LSHR, 7).and(1);
        v7.intoArray(out, op + 112);
    }

    private static void unpack0_2(byte[] in, int ip, int[] out, int op, int initial) {
        unpack0_2Part0(in, ip, out, op, initial);
        unpack0_2Part1(in, ip, out, op, initial);
    }

    private static void unpack0_2Part0(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B256, in, ip + 0).reinterpretAsShorts().convertShape(ZERO_EXTEND_S2I, S, 0);
        IntVector v0 = w0.and(3);
        v0.intoArray(out, op + 0);
        IntVector v1 = w0.lanewise(LSHR, 2).and(3);
        v1.intoArray(out, op + 16);
        IntVector v2 = w0.lanewise(LSHR, 4).and(3);
        v2.intoArray(out, op + 32);
        IntVector v3 = w0.lanewise(LSHR, 6).and(3);
        v3.intoArray(out, op + 48);
    }

    private static void unpack0_2Part1(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B256, in, ip + 0).reinterpretAsShorts().convertShape(ZERO_EXTEND_S2I, S, 0);
        IntVector v4 = w0.lanewise(LSHR, 8).and(3);
        v4.intoArray(out, op + 64);
        IntVector v5 = w0.lanewise(LSHR, 10).and(3);
        v5.intoArray(out, op + 80);
        IntVector v6 = w0.lanewise(LSHR, 12).and(3);
        v6.intoArray(out, op + 96);
        IntVector v7 = w0.lanewise(LSHR, 14).and(3);
        v7.intoArray(out, op + 112);
    }

    private static void unpack0_3(byte[] in, int ip, int[] out, int op, int initial) {
        unpack0_3Part0(in, ip, out, op, initial);
        unpack0_3Part1(in, ip, out, op, initial);
    }

    private static void unpack0_3Part0(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0, BYTE_TAIL3).rearrange(BYTE_EXPAND3, BYTE_WORD3).reinterpretShape(S,
            0);
        IntVector v0 = w0.and(7);
        v0.intoArray(out, op + 0);
        IntVector v1 = w0.lanewise(LSHR, 3).and(7);
        v1.intoArray(out, op + 16);
        IntVector v2 = w0.lanewise(LSHR, 6).and(7);
        v2.intoArray(out, op + 32);
        IntVector v3 = w0.lanewise(LSHR, 9).and(7);
        v3.intoArray(out, op + 48);
    }

    private static void unpack0_3Part1(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0, BYTE_TAIL3).rearrange(BYTE_EXPAND3, BYTE_WORD3).reinterpretShape(S,
            0);
        IntVector v4 = w0.lanewise(LSHR, 12).and(7);
        v4.intoArray(out, op + 64);
        IntVector v5 = w0.lanewise(LSHR, 15).and(7);
        v5.intoArray(out, op + 80);
        IntVector v6 = w0.lanewise(LSHR, 18).and(7);
        v6.intoArray(out, op + 96);
        IntVector v7 = w0.lanewise(LSHR, 21).and(7);
        v7.intoArray(out, op + 112);
    }

    private static void unpack0_4(byte[] in, int ip, int[] out, int op, int initial) {
        unpack0_4Part0(in, ip, out, op, initial);
        unpack0_4Part1(in, ip, out, op, initial);
    }

    private static void unpack0_4Part0(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector v0 = w0.and(15);
        v0.intoArray(out, op + 0);
        IntVector v1 = w0.lanewise(LSHR, 4).and(15);
        v1.intoArray(out, op + 16);
        IntVector v2 = w0.lanewise(LSHR, 8).and(15);
        v2.intoArray(out, op + 32);
        IntVector v3 = w0.lanewise(LSHR, 12).and(15);
        v3.intoArray(out, op + 48);
    }

    private static void unpack0_4Part1(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector v4 = w0.lanewise(LSHR, 16).and(15);
        v4.intoArray(out, op + 64);
        IntVector v5 = w0.lanewise(LSHR, 20).and(15);
        v5.intoArray(out, op + 80);
        IntVector v6 = w0.lanewise(LSHR, 24).and(15);
        v6.intoArray(out, op + 96);
        IntVector v7 = w0.lanewise(LSHR, 28).and(15);
        v7.intoArray(out, op + 112);
    }

    private static void unpack0_5(byte[] in, int ip, int[] out, int op, int initial) {
        unpack0_5Part0(in, ip, out, op, initial);
        unpack0_5Part1(in, ip, out, op, initial);
    }

    private static void unpack0_5Part0(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector v0 = w0.and(31);
        v0.intoArray(out, op + 0);
        IntVector v1 = w0.lanewise(LSHR, 5).and(31);
        v1.intoArray(out, op + 16);
        IntVector v2 = w0.lanewise(LSHR, 10).and(31);
        v2.intoArray(out, op + 32);
        IntVector v3 = w0.lanewise(LSHR, 15).and(31);
        v3.intoArray(out, op + 48);
    }

    private static void unpack0_5Part1(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B128, in, ip + 64).convertShape(ZERO_EXTEND_B2I, S, 0);
        IntVector v4 = w0.lanewise(LSHR, 20).and(31);
        v4.intoArray(out, op + 64);
        IntVector v5 = w0.lanewise(LSHR, 25).and(31);
        v5.intoArray(out, op + 80);
        IntVector v6 = w0.lanewise(LSHR, 30).or(w1.lanewise(LSHL, 2)).and(31);
        v6.intoArray(out, op + 96);
        IntVector v7 = w1.lanewise(LSHR, 3).and(31);
        v7.intoArray(out, op + 112);
    }

    private static void unpack0_6(byte[] in, int ip, int[] out, int op, int initial) {
        unpack0_6Part0(in, ip, out, op, initial);
        unpack0_6Part1(in, ip, out, op, initial);
    }

    private static void unpack0_6Part0(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector v0 = w0.and(63);
        v0.intoArray(out, op + 0);
        IntVector v1 = w0.lanewise(LSHR, 6).and(63);
        v1.intoArray(out, op + 16);
        IntVector v2 = w0.lanewise(LSHR, 12).and(63);
        v2.intoArray(out, op + 32);
        IntVector v3 = w0.lanewise(LSHR, 18).and(63);
        v3.intoArray(out, op + 48);
    }

    private static void unpack0_6Part1(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B256, in, ip + 64).reinterpretAsShorts().convertShape(ZERO_EXTEND_S2I, S, 0);
        IntVector v4 = w0.lanewise(LSHR, 24).and(63);
        v4.intoArray(out, op + 64);
        IntVector v5 = w0.lanewise(LSHR, 30).or(w1.lanewise(LSHL, 2)).and(63);
        v5.intoArray(out, op + 80);
        IntVector v6 = w1.lanewise(LSHR, 4).and(63);
        v6.intoArray(out, op + 96);
        IntVector v7 = w1.lanewise(LSHR, 10).and(63);
        v7.intoArray(out, op + 112);
    }

    private static void unpack0_7(byte[] in, int ip, int[] out, int op, int initial) {
        unpack0_7Part0(in, ip, out, op, initial);
        unpack0_7Part1(in, ip, out, op, initial);
    }

    private static void unpack0_7Part0(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector v0 = w0.and(127);
        v0.intoArray(out, op + 0);
        IntVector v1 = w0.lanewise(LSHR, 7).and(127);
        v1.intoArray(out, op + 16);
        IntVector v2 = w0.lanewise(LSHR, 14).and(127);
        v2.intoArray(out, op + 32);
        IntVector v3 = w0.lanewise(LSHR, 21).and(127);
        v3.intoArray(out, op + 48);
    }

    private static void unpack0_7Part1(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64, BYTE_TAIL3).rearrange(BYTE_EXPAND3, BYTE_WORD3).reinterpretShape(S,
            0);
        IntVector v4 = w0.lanewise(LSHR, 28).or(w1.lanewise(LSHL, 4)).and(127);
        v4.intoArray(out, op + 64);
        IntVector v5 = w1.lanewise(LSHR, 3).and(127);
        v5.intoArray(out, op + 80);
        IntVector v6 = w1.lanewise(LSHR, 10).and(127);
        v6.intoArray(out, op + 96);
        IntVector v7 = w1.lanewise(LSHR, 17).and(127);
        v7.intoArray(out, op + 112);
    }

    private static void unpack0_8(byte[] in, int ip, int[] out, int op, int initial) {
        unpack0_8Part0(in, ip, out, op, initial);
        unpack0_8Part1(in, ip, out, op, initial);
    }

    private static void unpack0_8Part0(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector v0 = w0.and(255);
        v0.intoArray(out, op + 0);
        IntVector v1 = w0.lanewise(LSHR, 8).and(255);
        v1.intoArray(out, op + 16);
        IntVector v2 = w0.lanewise(LSHR, 16).and(255);
        v2.intoArray(out, op + 32);
        IntVector v3 = w0.lanewise(LSHR, 24).and(255);
        v3.intoArray(out, op + 48);
    }

    private static void unpack0_8Part1(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector v4 = w1.and(255);
        v4.intoArray(out, op + 64);
        IntVector v5 = w1.lanewise(LSHR, 8).and(255);
        v5.intoArray(out, op + 80);
        IntVector v6 = w1.lanewise(LSHR, 16).and(255);
        v6.intoArray(out, op + 96);
        IntVector v7 = w1.lanewise(LSHR, 24).and(255);
        v7.intoArray(out, op + 112);
    }

    private static void unpack0_9(byte[] in, int ip, int[] out, int op, int initial) {
        unpack0_9Part0(in, ip, out, op, initial);
        unpack0_9Part1(in, ip, out, op, initial);
    }

    private static void unpack0_9Part0(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector v0 = w0.and(511);
        v0.intoArray(out, op + 0);
        IntVector v1 = w0.lanewise(LSHR, 9).and(511);
        v1.intoArray(out, op + 16);
        IntVector v2 = w0.lanewise(LSHR, 18).and(511);
        v2.intoArray(out, op + 32);
        IntVector v3 = w0.lanewise(LSHR, 27).or(w1.lanewise(LSHL, 5)).and(511);
        v3.intoArray(out, op + 48);
    }

    private static void unpack0_9Part1(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B128, in, ip + 128).convertShape(ZERO_EXTEND_B2I, S, 0);
        IntVector v4 = w1.lanewise(LSHR, 4).and(511);
        v4.intoArray(out, op + 64);
        IntVector v5 = w1.lanewise(LSHR, 13).and(511);
        v5.intoArray(out, op + 80);
        IntVector v6 = w1.lanewise(LSHR, 22).and(511);
        v6.intoArray(out, op + 96);
        IntVector v7 = w1.lanewise(LSHR, 31).or(w2.lanewise(LSHL, 1)).and(511);
        v7.intoArray(out, op + 112);
    }

    private static void unpack0_10(byte[] in, int ip, int[] out, int op, int initial) {
        unpack0_10Part0(in, ip, out, op, initial);
        unpack0_10Part1(in, ip, out, op, initial);
    }

    private static void unpack0_10Part0(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector v0 = w0.and(1023);
        v0.intoArray(out, op + 0);
        IntVector v1 = w0.lanewise(LSHR, 10).and(1023);
        v1.intoArray(out, op + 16);
        IntVector v2 = w0.lanewise(LSHR, 20).and(1023);
        v2.intoArray(out, op + 32);
        IntVector v3 = w0.lanewise(LSHR, 30).or(w1.lanewise(LSHL, 2)).and(1023);
        v3.intoArray(out, op + 48);
    }

    private static void unpack0_10Part1(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B256, in, ip + 128).reinterpretAsShorts().convertShape(ZERO_EXTEND_S2I, S, 0);
        IntVector v4 = w1.lanewise(LSHR, 8).and(1023);
        v4.intoArray(out, op + 64);
        IntVector v5 = w1.lanewise(LSHR, 18).and(1023);
        v5.intoArray(out, op + 80);
        IntVector v6 = w1.lanewise(LSHR, 28).or(w2.lanewise(LSHL, 4)).and(1023);
        v6.intoArray(out, op + 96);
        IntVector v7 = w2.lanewise(LSHR, 6).and(1023);
        v7.intoArray(out, op + 112);
    }

    private static void unpack0_11(byte[] in, int ip, int[] out, int op, int initial) {
        unpack0_11Part0(in, ip, out, op, initial);
        unpack0_11Part1(in, ip, out, op, initial);
    }

    private static void unpack0_11Part0(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector v0 = w0.and(2047);
        v0.intoArray(out, op + 0);
        IntVector v1 = w0.lanewise(LSHR, 11).and(2047);
        v1.intoArray(out, op + 16);
        IntVector v2 = w0.lanewise(LSHR, 22).or(w1.lanewise(LSHL, 10)).and(2047);
        v2.intoArray(out, op + 32);
        IntVector v3 = w1.lanewise(LSHR, 1).and(2047);
        v3.intoArray(out, op + 48);
    }

    private static void unpack0_11Part1(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128, BYTE_TAIL3).rearrange(BYTE_EXPAND3, BYTE_WORD3).reinterpretShape(S,
            0);
        IntVector v4 = w1.lanewise(LSHR, 12).and(2047);
        v4.intoArray(out, op + 64);
        IntVector v5 = w1.lanewise(LSHR, 23).or(w2.lanewise(LSHL, 9)).and(2047);
        v5.intoArray(out, op + 80);
        IntVector v6 = w2.lanewise(LSHR, 2).and(2047);
        v6.intoArray(out, op + 96);
        IntVector v7 = w2.lanewise(LSHR, 13).and(2047);
        v7.intoArray(out, op + 112);
    }

    private static void unpack0_12(byte[] in, int ip, int[] out, int op, int initial) {
        unpack0_12Part0(in, ip, out, op, initial);
        unpack0_12Part1(in, ip, out, op, initial);
    }

    private static void unpack0_12Part0(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector v0 = w0.and(4095);
        v0.intoArray(out, op + 0);
        IntVector v1 = w0.lanewise(LSHR, 12).and(4095);
        v1.intoArray(out, op + 16);
        IntVector v2 = w0.lanewise(LSHR, 24).or(w1.lanewise(LSHL, 8)).and(4095);
        v2.intoArray(out, op + 32);
        IntVector v3 = w1.lanewise(LSHR, 4).and(4095);
        v3.intoArray(out, op + 48);
    }

    private static void unpack0_12Part1(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector v4 = w1.lanewise(LSHR, 16).and(4095);
        v4.intoArray(out, op + 64);
        IntVector v5 = w1.lanewise(LSHR, 28).or(w2.lanewise(LSHL, 4)).and(4095);
        v5.intoArray(out, op + 80);
        IntVector v6 = w2.lanewise(LSHR, 8).and(4095);
        v6.intoArray(out, op + 96);
        IntVector v7 = w2.lanewise(LSHR, 20).and(4095);
        v7.intoArray(out, op + 112);
    }

    private static void unpack0_13(byte[] in, int ip, int[] out, int op, int initial) {
        unpack0_13Part0(in, ip, out, op, initial);
        unpack0_13Part1(in, ip, out, op, initial);
    }

    private static void unpack0_13Part0(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector v0 = w0.and(8191);
        v0.intoArray(out, op + 0);
        IntVector v1 = w0.lanewise(LSHR, 13).and(8191);
        v1.intoArray(out, op + 16);
        IntVector v2 = w0.lanewise(LSHR, 26).or(w1.lanewise(LSHL, 6)).and(8191);
        v2.intoArray(out, op + 32);
        IntVector v3 = w1.lanewise(LSHR, 7).and(8191);
        v3.intoArray(out, op + 48);
    }

    private static void unpack0_13Part1(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B128, in, ip + 192).convertShape(ZERO_EXTEND_B2I, S, 0);
        IntVector v4 = w1.lanewise(LSHR, 20).or(w2.lanewise(LSHL, 12)).and(8191);
        v4.intoArray(out, op + 64);
        IntVector v5 = w2.lanewise(LSHR, 1).and(8191);
        v5.intoArray(out, op + 80);
        IntVector v6 = w2.lanewise(LSHR, 14).and(8191);
        v6.intoArray(out, op + 96);
        IntVector v7 = w2.lanewise(LSHR, 27).or(w3.lanewise(LSHL, 5)).and(8191);
        v7.intoArray(out, op + 112);
    }

    private static void unpack0_14(byte[] in, int ip, int[] out, int op, int initial) {
        unpack0_14Part0(in, ip, out, op, initial);
        unpack0_14Part1(in, ip, out, op, initial);
    }

    private static void unpack0_14Part0(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector v0 = w0.and(16383);
        v0.intoArray(out, op + 0);
        IntVector v1 = w0.lanewise(LSHR, 14).and(16383);
        v1.intoArray(out, op + 16);
        IntVector v2 = w0.lanewise(LSHR, 28).or(w1.lanewise(LSHL, 4)).and(16383);
        v2.intoArray(out, op + 32);
        IntVector v3 = w1.lanewise(LSHR, 10).and(16383);
        v3.intoArray(out, op + 48);
    }

    private static void unpack0_14Part1(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B256, in, ip + 192).reinterpretAsShorts().convertShape(ZERO_EXTEND_S2I, S, 0);
        IntVector v4 = w1.lanewise(LSHR, 24).or(w2.lanewise(LSHL, 8)).and(16383);
        v4.intoArray(out, op + 64);
        IntVector v5 = w2.lanewise(LSHR, 6).and(16383);
        v5.intoArray(out, op + 80);
        IntVector v6 = w2.lanewise(LSHR, 20).or(w3.lanewise(LSHL, 12)).and(16383);
        v6.intoArray(out, op + 96);
        IntVector v7 = w3.lanewise(LSHR, 2).and(16383);
        v7.intoArray(out, op + 112);
    }

    private static void unpack0_15(byte[] in, int ip, int[] out, int op, int initial) {
        unpack0_15Part0(in, ip, out, op, initial);
        unpack0_15Part1(in, ip, out, op, initial);
    }

    private static void unpack0_15Part0(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector v0 = w0.and(32767);
        v0.intoArray(out, op + 0);
        IntVector v1 = w0.lanewise(LSHR, 15).and(32767);
        v1.intoArray(out, op + 16);
        IntVector v2 = w0.lanewise(LSHR, 30).or(w1.lanewise(LSHL, 2)).and(32767);
        v2.intoArray(out, op + 32);
        IntVector v3 = w1.lanewise(LSHR, 13).and(32767);
        v3.intoArray(out, op + 48);
    }

    private static void unpack0_15Part1(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192, BYTE_TAIL3).rearrange(BYTE_EXPAND3, BYTE_WORD3).reinterpretShape(S,
            0);
        IntVector v4 = w1.lanewise(LSHR, 28).or(w2.lanewise(LSHL, 4)).and(32767);
        v4.intoArray(out, op + 64);
        IntVector v5 = w2.lanewise(LSHR, 11).and(32767);
        v5.intoArray(out, op + 80);
        IntVector v6 = w2.lanewise(LSHR, 26).or(w3.lanewise(LSHL, 6)).and(32767);
        v6.intoArray(out, op + 96);
        IntVector v7 = w3.lanewise(LSHR, 9).and(32767);
        v7.intoArray(out, op + 112);
    }

    private static void unpack0_16(byte[] in, int ip, int[] out, int op, int initial) {
        unpack0_16Part0(in, ip, out, op, initial);
        unpack0_16Part1(in, ip, out, op, initial);
    }

    private static void unpack0_16Part0(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector v0 = w0.and(65535);
        v0.intoArray(out, op + 0);
        IntVector v1 = w0.lanewise(LSHR, 16).and(65535);
        v1.intoArray(out, op + 16);
        IntVector v2 = w1.and(65535);
        v2.intoArray(out, op + 32);
        IntVector v3 = w1.lanewise(LSHR, 16).and(65535);
        v3.intoArray(out, op + 48);
    }

    private static void unpack0_16Part1(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector v4 = w2.and(65535);
        v4.intoArray(out, op + 64);
        IntVector v5 = w2.lanewise(LSHR, 16).and(65535);
        v5.intoArray(out, op + 80);
        IntVector v6 = w3.and(65535);
        v6.intoArray(out, op + 96);
        IntVector v7 = w3.lanewise(LSHR, 16).and(65535);
        v7.intoArray(out, op + 112);
    }

    private static void unpack0_17(byte[] in, int ip, int[] out, int op, int initial) {
        unpack0_17Part0(in, ip, out, op, initial);
        unpack0_17Part1(in, ip, out, op, initial);
    }

    private static void unpack0_17Part0(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector v0 = w0.and(131071);
        v0.intoArray(out, op + 0);
        IntVector v1 = w0.lanewise(LSHR, 17).or(w1.lanewise(LSHL, 15)).and(131071);
        v1.intoArray(out, op + 16);
        IntVector v2 = w1.lanewise(LSHR, 2).and(131071);
        v2.intoArray(out, op + 32);
        IntVector v3 = w1.lanewise(LSHR, 19).or(w2.lanewise(LSHL, 13)).and(131071);
        v3.intoArray(out, op + 48);
    }

    private static void unpack0_17Part1(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector w4 = (IntVector) ByteVector.fromArray(B128, in, ip + 256).convertShape(ZERO_EXTEND_B2I, S, 0);
        IntVector v4 = w2.lanewise(LSHR, 4).and(131071);
        v4.intoArray(out, op + 64);
        IntVector v5 = w2.lanewise(LSHR, 21).or(w3.lanewise(LSHL, 11)).and(131071);
        v5.intoArray(out, op + 80);
        IntVector v6 = w3.lanewise(LSHR, 6).and(131071);
        v6.intoArray(out, op + 96);
        IntVector v7 = w3.lanewise(LSHR, 23).or(w4.lanewise(LSHL, 9)).and(131071);
        v7.intoArray(out, op + 112);
    }

    private static void unpack0_18(byte[] in, int ip, int[] out, int op, int initial) {
        unpack0_18Part0(in, ip, out, op, initial);
        unpack0_18Part1(in, ip, out, op, initial);
    }

    private static void unpack0_18Part0(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector v0 = w0.and(262143);
        v0.intoArray(out, op + 0);
        IntVector v1 = w0.lanewise(LSHR, 18).or(w1.lanewise(LSHL, 14)).and(262143);
        v1.intoArray(out, op + 16);
        IntVector v2 = w1.lanewise(LSHR, 4).and(262143);
        v2.intoArray(out, op + 32);
        IntVector v3 = w1.lanewise(LSHR, 22).or(w2.lanewise(LSHL, 10)).and(262143);
        v3.intoArray(out, op + 48);
    }

    private static void unpack0_18Part1(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector w4 = (IntVector) ByteVector.fromArray(B256, in, ip + 256).reinterpretAsShorts().convertShape(ZERO_EXTEND_S2I, S, 0);
        IntVector v4 = w2.lanewise(LSHR, 8).and(262143);
        v4.intoArray(out, op + 64);
        IntVector v5 = w2.lanewise(LSHR, 26).or(w3.lanewise(LSHL, 6)).and(262143);
        v5.intoArray(out, op + 80);
        IntVector v6 = w3.lanewise(LSHR, 12).and(262143);
        v6.intoArray(out, op + 96);
        IntVector v7 = w3.lanewise(LSHR, 30).or(w4.lanewise(LSHL, 2)).and(262143);
        v7.intoArray(out, op + 112);
    }

    private static void unpack0_19(byte[] in, int ip, int[] out, int op, int initial) {
        unpack0_19Part0(in, ip, out, op, initial);
        unpack0_19Part1(in, ip, out, op, initial);
    }

    private static void unpack0_19Part0(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector v0 = w0.and(524287);
        v0.intoArray(out, op + 0);
        IntVector v1 = w0.lanewise(LSHR, 19).or(w1.lanewise(LSHL, 13)).and(524287);
        v1.intoArray(out, op + 16);
        IntVector v2 = w1.lanewise(LSHR, 6).and(524287);
        v2.intoArray(out, op + 32);
        IntVector v3 = w1.lanewise(LSHR, 25).or(w2.lanewise(LSHL, 7)).and(524287);
        v3.intoArray(out, op + 48);
    }

    private static void unpack0_19Part1(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector w4 = (IntVector) ByteVector.fromArray(B, in, ip + 256, BYTE_TAIL3).rearrange(BYTE_EXPAND3, BYTE_WORD3).reinterpretShape(S,
            0);
        IntVector v4 = w2.lanewise(LSHR, 12).and(524287);
        v4.intoArray(out, op + 64);
        IntVector v5 = w2.lanewise(LSHR, 31).or(w3.lanewise(LSHL, 1)).and(524287);
        v5.intoArray(out, op + 80);
        IntVector v6 = w3.lanewise(LSHR, 18).or(w4.lanewise(LSHL, 14)).and(524287);
        v6.intoArray(out, op + 96);
        IntVector v7 = w4.lanewise(LSHR, 5).and(524287);
        v7.intoArray(out, op + 112);
    }

    private static void unpack0_20(byte[] in, int ip, int[] out, int op, int initial) {
        unpack0_20Part0(in, ip, out, op, initial);
        unpack0_20Part1(in, ip, out, op, initial);
    }

    private static void unpack0_20Part0(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector v0 = w0.and(1048575);
        v0.intoArray(out, op + 0);
        IntVector v1 = w0.lanewise(LSHR, 20).or(w1.lanewise(LSHL, 12)).and(1048575);
        v1.intoArray(out, op + 16);
        IntVector v2 = w1.lanewise(LSHR, 8).and(1048575);
        v2.intoArray(out, op + 32);
        IntVector v3 = w1.lanewise(LSHR, 28).or(w2.lanewise(LSHL, 4)).and(1048575);
        v3.intoArray(out, op + 48);
    }

    private static void unpack0_20Part1(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector w4 = (IntVector) ByteVector.fromArray(B, in, ip + 256).reinterpretShape(S, 0);
        IntVector v4 = w2.lanewise(LSHR, 16).or(w3.lanewise(LSHL, 16)).and(1048575);
        v4.intoArray(out, op + 64);
        IntVector v5 = w3.lanewise(LSHR, 4).and(1048575);
        v5.intoArray(out, op + 80);
        IntVector v6 = w3.lanewise(LSHR, 24).or(w4.lanewise(LSHL, 8)).and(1048575);
        v6.intoArray(out, op + 96);
        IntVector v7 = w4.lanewise(LSHR, 12).and(1048575);
        v7.intoArray(out, op + 112);
    }

    private static void unpack0_21(byte[] in, int ip, int[] out, int op, int initial) {
        unpack0_21Part0(in, ip, out, op, initial);
        unpack0_21Part1(in, ip, out, op, initial);
    }

    private static void unpack0_21Part0(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector v0 = w0.and(2097151);
        v0.intoArray(out, op + 0);
        IntVector v1 = w0.lanewise(LSHR, 21).or(w1.lanewise(LSHL, 11)).and(2097151);
        v1.intoArray(out, op + 16);
        IntVector v2 = w1.lanewise(LSHR, 10).and(2097151);
        v2.intoArray(out, op + 32);
        IntVector v3 = w1.lanewise(LSHR, 31).or(w2.lanewise(LSHL, 1)).and(2097151);
        v3.intoArray(out, op + 48);
    }

    private static void unpack0_21Part1(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector w4 = (IntVector) ByteVector.fromArray(B, in, ip + 256).reinterpretShape(S, 0);
        IntVector w5 = (IntVector) ByteVector.fromArray(B128, in, ip + 320).convertShape(ZERO_EXTEND_B2I, S, 0);
        IntVector v4 = w2.lanewise(LSHR, 20).or(w3.lanewise(LSHL, 12)).and(2097151);
        v4.intoArray(out, op + 64);
        IntVector v5 = w3.lanewise(LSHR, 9).and(2097151);
        v5.intoArray(out, op + 80);
        IntVector v6 = w3.lanewise(LSHR, 30).or(w4.lanewise(LSHL, 2)).and(2097151);
        v6.intoArray(out, op + 96);
        IntVector v7 = w4.lanewise(LSHR, 19).or(w5.lanewise(LSHL, 13)).and(2097151);
        v7.intoArray(out, op + 112);
    }

    private static void unpack0_22(byte[] in, int ip, int[] out, int op, int initial) {
        unpack0_22Part0(in, ip, out, op, initial);
        unpack0_22Part1(in, ip, out, op, initial);
    }

    private static void unpack0_22Part0(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector v0 = w0.and(4194303);
        v0.intoArray(out, op + 0);
        IntVector v1 = w0.lanewise(LSHR, 22).or(w1.lanewise(LSHL, 10)).and(4194303);
        v1.intoArray(out, op + 16);
        IntVector v2 = w1.lanewise(LSHR, 12).or(w2.lanewise(LSHL, 20)).and(4194303);
        v2.intoArray(out, op + 32);
        IntVector v3 = w2.lanewise(LSHR, 2).and(4194303);
        v3.intoArray(out, op + 48);
    }

    private static void unpack0_22Part1(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector w4 = (IntVector) ByteVector.fromArray(B, in, ip + 256).reinterpretShape(S, 0);
        IntVector w5 = (IntVector) ByteVector.fromArray(B256, in, ip + 320).reinterpretAsShorts().convertShape(ZERO_EXTEND_S2I, S, 0);
        IntVector v4 = w2.lanewise(LSHR, 24).or(w3.lanewise(LSHL, 8)).and(4194303);
        v4.intoArray(out, op + 64);
        IntVector v5 = w3.lanewise(LSHR, 14).or(w4.lanewise(LSHL, 18)).and(4194303);
        v5.intoArray(out, op + 80);
        IntVector v6 = w4.lanewise(LSHR, 4).and(4194303);
        v6.intoArray(out, op + 96);
        IntVector v7 = w4.lanewise(LSHR, 26).or(w5.lanewise(LSHL, 6)).and(4194303);
        v7.intoArray(out, op + 112);
    }

    private static void unpack0_23(byte[] in, int ip, int[] out, int op, int initial) {
        unpack0_23Part0(in, ip, out, op, initial);
        unpack0_23Part1(in, ip, out, op, initial);
    }

    private static void unpack0_23Part0(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector v0 = w0.and(8388607);
        v0.intoArray(out, op + 0);
        IntVector v1 = w0.lanewise(LSHR, 23).or(w1.lanewise(LSHL, 9)).and(8388607);
        v1.intoArray(out, op + 16);
        IntVector v2 = w1.lanewise(LSHR, 14).or(w2.lanewise(LSHL, 18)).and(8388607);
        v2.intoArray(out, op + 32);
        IntVector v3 = w2.lanewise(LSHR, 5).and(8388607);
        v3.intoArray(out, op + 48);
    }

    private static void unpack0_23Part1(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector w4 = (IntVector) ByteVector.fromArray(B, in, ip + 256).reinterpretShape(S, 0);
        IntVector w5 = (IntVector) ByteVector.fromArray(B, in, ip + 320, BYTE_TAIL3).rearrange(BYTE_EXPAND3, BYTE_WORD3).reinterpretShape(S,
            0);
        IntVector v4 = w2.lanewise(LSHR, 28).or(w3.lanewise(LSHL, 4)).and(8388607);
        v4.intoArray(out, op + 64);
        IntVector v5 = w3.lanewise(LSHR, 19).or(w4.lanewise(LSHL, 13)).and(8388607);
        v5.intoArray(out, op + 80);
        IntVector v6 = w4.lanewise(LSHR, 10).or(w5.lanewise(LSHL, 22)).and(8388607);
        v6.intoArray(out, op + 96);
        IntVector v7 = w5.lanewise(LSHR, 1).and(8388607);
        v7.intoArray(out, op + 112);
    }

    private static void unpack0_24(byte[] in, int ip, int[] out, int op, int initial) {
        unpack0_24Part0(in, ip, out, op, initial);
        unpack0_24Part1(in, ip, out, op, initial);
    }

    private static void unpack0_24Part0(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector v0 = w0.and(16777215);
        v0.intoArray(out, op + 0);
        IntVector v1 = w0.lanewise(LSHR, 24).or(w1.lanewise(LSHL, 8)).and(16777215);
        v1.intoArray(out, op + 16);
        IntVector v2 = w1.lanewise(LSHR, 16).or(w2.lanewise(LSHL, 16)).and(16777215);
        v2.intoArray(out, op + 32);
        IntVector v3 = w2.lanewise(LSHR, 8).and(16777215);
        v3.intoArray(out, op + 48);
    }

    private static void unpack0_24Part1(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector w4 = (IntVector) ByteVector.fromArray(B, in, ip + 256).reinterpretShape(S, 0);
        IntVector w5 = (IntVector) ByteVector.fromArray(B, in, ip + 320).reinterpretShape(S, 0);
        IntVector v4 = w3.and(16777215);
        v4.intoArray(out, op + 64);
        IntVector v5 = w3.lanewise(LSHR, 24).or(w4.lanewise(LSHL, 8)).and(16777215);
        v5.intoArray(out, op + 80);
        IntVector v6 = w4.lanewise(LSHR, 16).or(w5.lanewise(LSHL, 16)).and(16777215);
        v6.intoArray(out, op + 96);
        IntVector v7 = w5.lanewise(LSHR, 8).and(16777215);
        v7.intoArray(out, op + 112);
    }

    private static void unpack0_25(byte[] in, int ip, int[] out, int op, int initial) {
        unpack0_25Part0(in, ip, out, op, initial);
        unpack0_25Part1(in, ip, out, op, initial);
    }

    private static void unpack0_25Part0(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector v0 = w0.and(33554431);
        v0.intoArray(out, op + 0);
        IntVector v1 = w0.lanewise(LSHR, 25).or(w1.lanewise(LSHL, 7)).and(33554431);
        v1.intoArray(out, op + 16);
        IntVector v2 = w1.lanewise(LSHR, 18).or(w2.lanewise(LSHL, 14)).and(33554431);
        v2.intoArray(out, op + 32);
        IntVector v3 = w2.lanewise(LSHR, 11).or(w3.lanewise(LSHL, 21)).and(33554431);
        v3.intoArray(out, op + 48);
    }

    private static void unpack0_25Part1(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector w4 = (IntVector) ByteVector.fromArray(B, in, ip + 256).reinterpretShape(S, 0);
        IntVector w5 = (IntVector) ByteVector.fromArray(B, in, ip + 320).reinterpretShape(S, 0);
        IntVector w6 = (IntVector) ByteVector.fromArray(B128, in, ip + 384).convertShape(ZERO_EXTEND_B2I, S, 0);
        IntVector v4 = w3.lanewise(LSHR, 4).and(33554431);
        v4.intoArray(out, op + 64);
        IntVector v5 = w3.lanewise(LSHR, 29).or(w4.lanewise(LSHL, 3)).and(33554431);
        v5.intoArray(out, op + 80);
        IntVector v6 = w4.lanewise(LSHR, 22).or(w5.lanewise(LSHL, 10)).and(33554431);
        v6.intoArray(out, op + 96);
        IntVector v7 = w5.lanewise(LSHR, 15).or(w6.lanewise(LSHL, 17)).and(33554431);
        v7.intoArray(out, op + 112);
    }

    private static void unpack0_26(byte[] in, int ip, int[] out, int op, int initial) {
        unpack0_26Part0(in, ip, out, op, initial);
        unpack0_26Part1(in, ip, out, op, initial);
    }

    private static void unpack0_26Part0(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector v0 = w0.and(67108863);
        v0.intoArray(out, op + 0);
        IntVector v1 = w0.lanewise(LSHR, 26).or(w1.lanewise(LSHL, 6)).and(67108863);
        v1.intoArray(out, op + 16);
        IntVector v2 = w1.lanewise(LSHR, 20).or(w2.lanewise(LSHL, 12)).and(67108863);
        v2.intoArray(out, op + 32);
        IntVector v3 = w2.lanewise(LSHR, 14).or(w3.lanewise(LSHL, 18)).and(67108863);
        v3.intoArray(out, op + 48);
    }

    private static void unpack0_26Part1(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector w4 = (IntVector) ByteVector.fromArray(B, in, ip + 256).reinterpretShape(S, 0);
        IntVector w5 = (IntVector) ByteVector.fromArray(B, in, ip + 320).reinterpretShape(S, 0);
        IntVector w6 = (IntVector) ByteVector.fromArray(B256, in, ip + 384).reinterpretAsShorts().convertShape(ZERO_EXTEND_S2I, S, 0);
        IntVector v4 = w3.lanewise(LSHR, 8).or(w4.lanewise(LSHL, 24)).and(67108863);
        v4.intoArray(out, op + 64);
        IntVector v5 = w4.lanewise(LSHR, 2).and(67108863);
        v5.intoArray(out, op + 80);
        IntVector v6 = w4.lanewise(LSHR, 28).or(w5.lanewise(LSHL, 4)).and(67108863);
        v6.intoArray(out, op + 96);
        IntVector v7 = w5.lanewise(LSHR, 22).or(w6.lanewise(LSHL, 10)).and(67108863);
        v7.intoArray(out, op + 112);
    }

    private static void unpack0_27(byte[] in, int ip, int[] out, int op, int initial) {
        unpack0_27Part0(in, ip, out, op, initial);
        unpack0_27Part1(in, ip, out, op, initial);
    }

    private static void unpack0_27Part0(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector v0 = w0.and(134217727);
        v0.intoArray(out, op + 0);
        IntVector v1 = w0.lanewise(LSHR, 27).or(w1.lanewise(LSHL, 5)).and(134217727);
        v1.intoArray(out, op + 16);
        IntVector v2 = w1.lanewise(LSHR, 22).or(w2.lanewise(LSHL, 10)).and(134217727);
        v2.intoArray(out, op + 32);
        IntVector v3 = w2.lanewise(LSHR, 17).or(w3.lanewise(LSHL, 15)).and(134217727);
        v3.intoArray(out, op + 48);
    }

    private static void unpack0_27Part1(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector w4 = (IntVector) ByteVector.fromArray(B, in, ip + 256).reinterpretShape(S, 0);
        IntVector w5 = (IntVector) ByteVector.fromArray(B, in, ip + 320).reinterpretShape(S, 0);
        IntVector w6 = (IntVector) ByteVector.fromArray(B, in, ip + 384, BYTE_TAIL3).rearrange(BYTE_EXPAND3, BYTE_WORD3).reinterpretShape(S,
            0);
        IntVector v4 = w3.lanewise(LSHR, 12).or(w4.lanewise(LSHL, 20)).and(134217727);
        v4.intoArray(out, op + 64);
        IntVector v5 = w4.lanewise(LSHR, 7).or(w5.lanewise(LSHL, 25)).and(134217727);
        v5.intoArray(out, op + 80);
        IntVector v6 = w5.lanewise(LSHR, 2).and(134217727);
        v6.intoArray(out, op + 96);
        IntVector v7 = w5.lanewise(LSHR, 29).or(w6.lanewise(LSHL, 3)).and(134217727);
        v7.intoArray(out, op + 112);
    }

    private static void unpack0_28(byte[] in, int ip, int[] out, int op, int initial) {
        unpack0_28Part0(in, ip, out, op, initial);
        unpack0_28Part1(in, ip, out, op, initial);
    }

    private static void unpack0_28Part0(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector v0 = w0.and(268435455);
        v0.intoArray(out, op + 0);
        IntVector v1 = w0.lanewise(LSHR, 28).or(w1.lanewise(LSHL, 4)).and(268435455);
        v1.intoArray(out, op + 16);
        IntVector v2 = w1.lanewise(LSHR, 24).or(w2.lanewise(LSHL, 8)).and(268435455);
        v2.intoArray(out, op + 32);
        IntVector v3 = w2.lanewise(LSHR, 20).or(w3.lanewise(LSHL, 12)).and(268435455);
        v3.intoArray(out, op + 48);
    }

    private static void unpack0_28Part1(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector w4 = (IntVector) ByteVector.fromArray(B, in, ip + 256).reinterpretShape(S, 0);
        IntVector w5 = (IntVector) ByteVector.fromArray(B, in, ip + 320).reinterpretShape(S, 0);
        IntVector w6 = (IntVector) ByteVector.fromArray(B, in, ip + 384).reinterpretShape(S, 0);
        IntVector v4 = w3.lanewise(LSHR, 16).or(w4.lanewise(LSHL, 16)).and(268435455);
        v4.intoArray(out, op + 64);
        IntVector v5 = w4.lanewise(LSHR, 12).or(w5.lanewise(LSHL, 20)).and(268435455);
        v5.intoArray(out, op + 80);
        IntVector v6 = w5.lanewise(LSHR, 8).or(w6.lanewise(LSHL, 24)).and(268435455);
        v6.intoArray(out, op + 96);
        IntVector v7 = w6.lanewise(LSHR, 4).and(268435455);
        v7.intoArray(out, op + 112);
    }

    private static void unpack0_29(byte[] in, int ip, int[] out, int op, int initial) {
        unpack0_29Part0(in, ip, out, op, initial);
        unpack0_29Part1(in, ip, out, op, initial);
    }

    private static void unpack0_29Part0(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector v0 = w0.and(536870911);
        v0.intoArray(out, op + 0);
        IntVector v1 = w0.lanewise(LSHR, 29).or(w1.lanewise(LSHL, 3)).and(536870911);
        v1.intoArray(out, op + 16);
        IntVector v2 = w1.lanewise(LSHR, 26).or(w2.lanewise(LSHL, 6)).and(536870911);
        v2.intoArray(out, op + 32);
        IntVector v3 = w2.lanewise(LSHR, 23).or(w3.lanewise(LSHL, 9)).and(536870911);
        v3.intoArray(out, op + 48);
    }

    private static void unpack0_29Part1(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector w4 = (IntVector) ByteVector.fromArray(B, in, ip + 256).reinterpretShape(S, 0);
        IntVector w5 = (IntVector) ByteVector.fromArray(B, in, ip + 320).reinterpretShape(S, 0);
        IntVector w6 = (IntVector) ByteVector.fromArray(B, in, ip + 384).reinterpretShape(S, 0);
        IntVector w7 = (IntVector) ByteVector.fromArray(B128, in, ip + 448).convertShape(ZERO_EXTEND_B2I, S, 0);
        IntVector v4 = w3.lanewise(LSHR, 20).or(w4.lanewise(LSHL, 12)).and(536870911);
        v4.intoArray(out, op + 64);
        IntVector v5 = w4.lanewise(LSHR, 17).or(w5.lanewise(LSHL, 15)).and(536870911);
        v5.intoArray(out, op + 80);
        IntVector v6 = w5.lanewise(LSHR, 14).or(w6.lanewise(LSHL, 18)).and(536870911);
        v6.intoArray(out, op + 96);
        IntVector v7 = w6.lanewise(LSHR, 11).or(w7.lanewise(LSHL, 21)).and(536870911);
        v7.intoArray(out, op + 112);
    }

    private static void unpack0_30(byte[] in, int ip, int[] out, int op, int initial) {
        unpack0_30Part0(in, ip, out, op, initial);
        unpack0_30Part1(in, ip, out, op, initial);
    }

    private static void unpack0_30Part0(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector v0 = w0.and(1073741823);
        v0.intoArray(out, op + 0);
        IntVector v1 = w0.lanewise(LSHR, 30).or(w1.lanewise(LSHL, 2)).and(1073741823);
        v1.intoArray(out, op + 16);
        IntVector v2 = w1.lanewise(LSHR, 28).or(w2.lanewise(LSHL, 4)).and(1073741823);
        v2.intoArray(out, op + 32);
        IntVector v3 = w2.lanewise(LSHR, 26).or(w3.lanewise(LSHL, 6)).and(1073741823);
        v3.intoArray(out, op + 48);
    }

    private static void unpack0_30Part1(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector w4 = (IntVector) ByteVector.fromArray(B, in, ip + 256).reinterpretShape(S, 0);
        IntVector w5 = (IntVector) ByteVector.fromArray(B, in, ip + 320).reinterpretShape(S, 0);
        IntVector w6 = (IntVector) ByteVector.fromArray(B, in, ip + 384).reinterpretShape(S, 0);
        IntVector w7 = (IntVector) ByteVector.fromArray(B256, in, ip + 448).reinterpretAsShorts().convertShape(ZERO_EXTEND_S2I, S, 0);
        IntVector v4 = w3.lanewise(LSHR, 24).or(w4.lanewise(LSHL, 8)).and(1073741823);
        v4.intoArray(out, op + 64);
        IntVector v5 = w4.lanewise(LSHR, 22).or(w5.lanewise(LSHL, 10)).and(1073741823);
        v5.intoArray(out, op + 80);
        IntVector v6 = w5.lanewise(LSHR, 20).or(w6.lanewise(LSHL, 12)).and(1073741823);
        v6.intoArray(out, op + 96);
        IntVector v7 = w6.lanewise(LSHR, 18).or(w7.lanewise(LSHL, 14)).and(1073741823);
        v7.intoArray(out, op + 112);
    }

    private static void unpack0_31(byte[] in, int ip, int[] out, int op, int initial) {
        unpack0_31Part0(in, ip, out, op, initial);
        unpack0_31Part1(in, ip, out, op, initial);
    }

    private static void unpack0_31Part0(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector v0 = w0.and(2147483647);
        v0.intoArray(out, op + 0);
        IntVector v1 = w0.lanewise(LSHR, 31).or(w1.lanewise(LSHL, 1)).and(2147483647);
        v1.intoArray(out, op + 16);
        IntVector v2 = w1.lanewise(LSHR, 30).or(w2.lanewise(LSHL, 2)).and(2147483647);
        v2.intoArray(out, op + 32);
        IntVector v3 = w2.lanewise(LSHR, 29).or(w3.lanewise(LSHL, 3)).and(2147483647);
        v3.intoArray(out, op + 48);
    }

    private static void unpack0_31Part1(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector w4 = (IntVector) ByteVector.fromArray(B, in, ip + 256).reinterpretShape(S, 0);
        IntVector w5 = (IntVector) ByteVector.fromArray(B, in, ip + 320).reinterpretShape(S, 0);
        IntVector w6 = (IntVector) ByteVector.fromArray(B, in, ip + 384).reinterpretShape(S, 0);
        IntVector w7 = (IntVector) ByteVector.fromArray(B, in, ip + 448, BYTE_TAIL3).rearrange(BYTE_EXPAND3, BYTE_WORD3).reinterpretShape(S,
            0);
        IntVector v4 = w3.lanewise(LSHR, 28).or(w4.lanewise(LSHL, 4)).and(2147483647);
        v4.intoArray(out, op + 64);
        IntVector v5 = w4.lanewise(LSHR, 27).or(w5.lanewise(LSHL, 5)).and(2147483647);
        v5.intoArray(out, op + 80);
        IntVector v6 = w5.lanewise(LSHR, 26).or(w6.lanewise(LSHL, 6)).and(2147483647);
        v6.intoArray(out, op + 96);
        IntVector v7 = w6.lanewise(LSHR, 25).or(w7.lanewise(LSHL, 7)).and(2147483647);
        v7.intoArray(out, op + 112);
    }

    private static void unpack1(byte[] in, int ip, int[] out, int op, int bits, int initial) {
        switch (bits) {
            case 32 -> unpack1_32(in, ip, out, op, initial);
            case 1 -> unpack1_1(in, ip, out, op, initial);
            case 2 -> unpack1_2(in, ip, out, op, initial);
            case 3 -> unpack1_3(in, ip, out, op, initial);
            case 4 -> unpack1_4(in, ip, out, op, initial);
            case 5 -> unpack1_5(in, ip, out, op, initial);
            case 6 -> unpack1_6(in, ip, out, op, initial);
            case 7 -> unpack1_7(in, ip, out, op, initial);
            case 8 -> unpack1_8(in, ip, out, op, initial);
            case 9 -> unpack1_9(in, ip, out, op, initial);
            case 10 -> unpack1_10(in, ip, out, op, initial);
            case 11 -> unpack1_11(in, ip, out, op, initial);
            case 12 -> unpack1_12(in, ip, out, op, initial);
            case 13 -> unpack1_13(in, ip, out, op, initial);
            case 14 -> unpack1_14(in, ip, out, op, initial);
            case 15 -> unpack1_15(in, ip, out, op, initial);
            case 16 -> unpack1_16(in, ip, out, op, initial);
            case 17 -> unpack1_17(in, ip, out, op, initial);
            case 18 -> unpack1_18(in, ip, out, op, initial);
            case 19 -> unpack1_19(in, ip, out, op, initial);
            case 20 -> unpack1_20(in, ip, out, op, initial);
            case 21 -> unpack1_21(in, ip, out, op, initial);
            case 22 -> unpack1_22(in, ip, out, op, initial);
            case 23 -> unpack1_23(in, ip, out, op, initial);
            case 24 -> unpack1_24(in, ip, out, op, initial);
            case 25 -> unpack1_25(in, ip, out, op, initial);
            case 26 -> unpack1_26(in, ip, out, op, initial);
            case 27 -> unpack1_27(in, ip, out, op, initial);
            case 28 -> unpack1_28(in, ip, out, op, initial);
            case 29 -> unpack1_29(in, ip, out, op, initial);
            case 30 -> unpack1_30(in, ip, out, op, initial);
            case 31 -> unpack1_31(in, ip, out, op, initial);
            default -> throw new IllegalArgumentException("Unsupported width");
        }
    }

    private static void unpack1_1(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B128, in, ip + 0).convertShape(ZERO_EXTEND_B2I, S, 0);
        IntVector v0 = w0.and(1);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 1).and(1);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w0.lanewise(LSHR, 2).and(1);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w0.lanewise(LSHR, 3).and(1);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w0.lanewise(LSHR, 4).and(1);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w0.lanewise(LSHR, 5).and(1);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w0.lanewise(LSHR, 6).and(1);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w0.lanewise(LSHR, 7);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack1_2(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B256, in, ip + 0).reinterpretAsShorts().convertShape(ZERO_EXTEND_S2I, S, 0);
        IntVector v0 = w0.and(3);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 2).and(3);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w0.lanewise(LSHR, 4).and(3);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w0.lanewise(LSHR, 6).and(3);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w0.lanewise(LSHR, 8).and(3);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w0.lanewise(LSHR, 10).and(3);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w0.lanewise(LSHR, 12).and(3);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w0.lanewise(LSHR, 14);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack1_3(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0, BYTE_TAIL3).rearrange(BYTE_EXPAND3, BYTE_WORD3).reinterpretShape(S,
            0);
        IntVector v0 = w0.and(7);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 3).and(7);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w0.lanewise(LSHR, 6).and(7);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w0.lanewise(LSHR, 9).and(7);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w0.lanewise(LSHR, 12).and(7);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w0.lanewise(LSHR, 15).and(7);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w0.lanewise(LSHR, 18).and(7);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w0.lanewise(LSHR, 21);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack1_4(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector v0 = w0.and(15);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 4).and(15);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w0.lanewise(LSHR, 8).and(15);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w0.lanewise(LSHR, 12).and(15);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w0.lanewise(LSHR, 16).and(15);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w0.lanewise(LSHR, 20).and(15);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w0.lanewise(LSHR, 24).and(15);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w0.lanewise(LSHR, 28);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack1_5(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B128, in, ip + 64).convertShape(ZERO_EXTEND_B2I, S, 0);
        IntVector v0 = w0.and(31);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 5).and(31);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w0.lanewise(LSHR, 10).and(31);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w0.lanewise(LSHR, 15).and(31);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w0.lanewise(LSHR, 20).and(31);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w0.lanewise(LSHR, 25).and(31);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w0.lanewise(LSHR, 30).or(w1.lanewise(LSHL, 2)).and(31);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w1.lanewise(LSHR, 3);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack1_6(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B256, in, ip + 64).reinterpretAsShorts().convertShape(ZERO_EXTEND_S2I, S, 0);
        IntVector v0 = w0.and(63);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 6).and(63);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w0.lanewise(LSHR, 12).and(63);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w0.lanewise(LSHR, 18).and(63);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w0.lanewise(LSHR, 24).and(63);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w0.lanewise(LSHR, 30).or(w1.lanewise(LSHL, 2)).and(63);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w1.lanewise(LSHR, 4).and(63);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w1.lanewise(LSHR, 10);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack1_7(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64, BYTE_TAIL3).rearrange(BYTE_EXPAND3, BYTE_WORD3).reinterpretShape(S,
            0);
        IntVector v0 = w0.and(127);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 7).and(127);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w0.lanewise(LSHR, 14).and(127);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w0.lanewise(LSHR, 21).and(127);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w0.lanewise(LSHR, 28).or(w1.lanewise(LSHL, 4)).and(127);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w1.lanewise(LSHR, 3).and(127);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w1.lanewise(LSHR, 10).and(127);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w1.lanewise(LSHR, 17);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack1_8(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector v0 = w0.and(255);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 8).and(255);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w0.lanewise(LSHR, 16).and(255);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w0.lanewise(LSHR, 24).and(255);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w1.and(255);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w1.lanewise(LSHR, 8).and(255);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w1.lanewise(LSHR, 16).and(255);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w1.lanewise(LSHR, 24);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack1_9(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B128, in, ip + 128).convertShape(ZERO_EXTEND_B2I, S, 0);
        IntVector v0 = w0.and(511);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 9).and(511);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w0.lanewise(LSHR, 18).and(511);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w0.lanewise(LSHR, 27).or(w1.lanewise(LSHL, 5)).and(511);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w1.lanewise(LSHR, 4).and(511);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w1.lanewise(LSHR, 13).and(511);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w1.lanewise(LSHR, 22).and(511);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w1.lanewise(LSHR, 31).or(w2.lanewise(LSHL, 1));
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack1_10(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B256, in, ip + 128).reinterpretAsShorts().convertShape(ZERO_EXTEND_S2I, S, 0);
        IntVector v0 = w0.and(1023);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 10).and(1023);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w0.lanewise(LSHR, 20).and(1023);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w0.lanewise(LSHR, 30).or(w1.lanewise(LSHL, 2)).and(1023);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w1.lanewise(LSHR, 8).and(1023);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w1.lanewise(LSHR, 18).and(1023);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w1.lanewise(LSHR, 28).or(w2.lanewise(LSHL, 4)).and(1023);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w2.lanewise(LSHR, 6);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack1_11(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128, BYTE_TAIL3).rearrange(BYTE_EXPAND3, BYTE_WORD3).reinterpretShape(S,
            0);
        IntVector v0 = w0.and(2047);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 11).and(2047);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w0.lanewise(LSHR, 22).or(w1.lanewise(LSHL, 10)).and(2047);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w1.lanewise(LSHR, 1).and(2047);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w1.lanewise(LSHR, 12).and(2047);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w1.lanewise(LSHR, 23).or(w2.lanewise(LSHL, 9)).and(2047);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w2.lanewise(LSHR, 2).and(2047);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w2.lanewise(LSHR, 13);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack1_12(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector v0 = w0.and(4095);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 12).and(4095);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w0.lanewise(LSHR, 24).or(w1.lanewise(LSHL, 8)).and(4095);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w1.lanewise(LSHR, 4).and(4095);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w1.lanewise(LSHR, 16).and(4095);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w1.lanewise(LSHR, 28).or(w2.lanewise(LSHL, 4)).and(4095);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w2.lanewise(LSHR, 8).and(4095);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w2.lanewise(LSHR, 20);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack1_13(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B128, in, ip + 192).convertShape(ZERO_EXTEND_B2I, S, 0);
        IntVector v0 = w0.and(8191);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 13).and(8191);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w0.lanewise(LSHR, 26).or(w1.lanewise(LSHL, 6)).and(8191);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w1.lanewise(LSHR, 7).and(8191);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w1.lanewise(LSHR, 20).or(w2.lanewise(LSHL, 12)).and(8191);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w2.lanewise(LSHR, 1).and(8191);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w2.lanewise(LSHR, 14).and(8191);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w2.lanewise(LSHR, 27).or(w3.lanewise(LSHL, 5));
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack1_14(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B256, in, ip + 192).reinterpretAsShorts().convertShape(ZERO_EXTEND_S2I, S, 0);
        IntVector v0 = w0.and(16383);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 14).and(16383);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w0.lanewise(LSHR, 28).or(w1.lanewise(LSHL, 4)).and(16383);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w1.lanewise(LSHR, 10).and(16383);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w1.lanewise(LSHR, 24).or(w2.lanewise(LSHL, 8)).and(16383);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w2.lanewise(LSHR, 6).and(16383);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w2.lanewise(LSHR, 20).or(w3.lanewise(LSHL, 12)).and(16383);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w3.lanewise(LSHR, 2);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack1_15(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192, BYTE_TAIL3).rearrange(BYTE_EXPAND3, BYTE_WORD3).reinterpretShape(S,
            0);
        IntVector v0 = w0.and(32767);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 15).and(32767);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w0.lanewise(LSHR, 30).or(w1.lanewise(LSHL, 2)).and(32767);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w1.lanewise(LSHR, 13).and(32767);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w1.lanewise(LSHR, 28).or(w2.lanewise(LSHL, 4)).and(32767);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w2.lanewise(LSHR, 11).and(32767);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w2.lanewise(LSHR, 26).or(w3.lanewise(LSHL, 6)).and(32767);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w3.lanewise(LSHR, 9);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack1_16(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector v0 = w0.and(65535);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 16).and(65535);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w1.and(65535);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w1.lanewise(LSHR, 16).and(65535);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w2.and(65535);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w2.lanewise(LSHR, 16).and(65535);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w3.and(65535);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w3.lanewise(LSHR, 16);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack1_17(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector w4 = (IntVector) ByteVector.fromArray(B128, in, ip + 256).convertShape(ZERO_EXTEND_B2I, S, 0);
        IntVector v0 = w0.and(131071);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 17).or(w1.lanewise(LSHL, 15)).and(131071);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w1.lanewise(LSHR, 2).and(131071);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w1.lanewise(LSHR, 19).or(w2.lanewise(LSHL, 13)).and(131071);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w2.lanewise(LSHR, 4).and(131071);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w2.lanewise(LSHR, 21).or(w3.lanewise(LSHL, 11)).and(131071);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w3.lanewise(LSHR, 6).and(131071);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w3.lanewise(LSHR, 23).or(w4.lanewise(LSHL, 9));
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack1_18(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector w4 = (IntVector) ByteVector.fromArray(B256, in, ip + 256).reinterpretAsShorts().convertShape(ZERO_EXTEND_S2I, S, 0);
        IntVector v0 = w0.and(262143);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 18).or(w1.lanewise(LSHL, 14)).and(262143);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w1.lanewise(LSHR, 4).and(262143);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w1.lanewise(LSHR, 22).or(w2.lanewise(LSHL, 10)).and(262143);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w2.lanewise(LSHR, 8).and(262143);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w2.lanewise(LSHR, 26).or(w3.lanewise(LSHL, 6)).and(262143);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w3.lanewise(LSHR, 12).and(262143);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w3.lanewise(LSHR, 30).or(w4.lanewise(LSHL, 2));
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack1_19(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector w4 = (IntVector) ByteVector.fromArray(B, in, ip + 256, BYTE_TAIL3).rearrange(BYTE_EXPAND3, BYTE_WORD3).reinterpretShape(S,
            0);
        IntVector v0 = w0.and(524287);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 19).or(w1.lanewise(LSHL, 13)).and(524287);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w1.lanewise(LSHR, 6).and(524287);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w1.lanewise(LSHR, 25).or(w2.lanewise(LSHL, 7)).and(524287);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w2.lanewise(LSHR, 12).and(524287);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w2.lanewise(LSHR, 31).or(w3.lanewise(LSHL, 1)).and(524287);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w3.lanewise(LSHR, 18).or(w4.lanewise(LSHL, 14)).and(524287);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w4.lanewise(LSHR, 5);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack1_20(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector w4 = (IntVector) ByteVector.fromArray(B, in, ip + 256).reinterpretShape(S, 0);
        IntVector v0 = w0.and(1048575);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 20).or(w1.lanewise(LSHL, 12)).and(1048575);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w1.lanewise(LSHR, 8).and(1048575);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w1.lanewise(LSHR, 28).or(w2.lanewise(LSHL, 4)).and(1048575);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w2.lanewise(LSHR, 16).or(w3.lanewise(LSHL, 16)).and(1048575);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w3.lanewise(LSHR, 4).and(1048575);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w3.lanewise(LSHR, 24).or(w4.lanewise(LSHL, 8)).and(1048575);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w4.lanewise(LSHR, 12);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack1_21(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector w4 = (IntVector) ByteVector.fromArray(B, in, ip + 256).reinterpretShape(S, 0);
        IntVector w5 = (IntVector) ByteVector.fromArray(B128, in, ip + 320).convertShape(ZERO_EXTEND_B2I, S, 0);
        IntVector v0 = w0.and(2097151);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 21).or(w1.lanewise(LSHL, 11)).and(2097151);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w1.lanewise(LSHR, 10).and(2097151);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w1.lanewise(LSHR, 31).or(w2.lanewise(LSHL, 1)).and(2097151);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w2.lanewise(LSHR, 20).or(w3.lanewise(LSHL, 12)).and(2097151);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w3.lanewise(LSHR, 9).and(2097151);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w3.lanewise(LSHR, 30).or(w4.lanewise(LSHL, 2)).and(2097151);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w4.lanewise(LSHR, 19).or(w5.lanewise(LSHL, 13));
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack1_22(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector w4 = (IntVector) ByteVector.fromArray(B, in, ip + 256).reinterpretShape(S, 0);
        IntVector w5 = (IntVector) ByteVector.fromArray(B256, in, ip + 320).reinterpretAsShorts().convertShape(ZERO_EXTEND_S2I, S, 0);
        IntVector v0 = w0.and(4194303);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 22).or(w1.lanewise(LSHL, 10)).and(4194303);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w1.lanewise(LSHR, 12).or(w2.lanewise(LSHL, 20)).and(4194303);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w2.lanewise(LSHR, 2).and(4194303);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w2.lanewise(LSHR, 24).or(w3.lanewise(LSHL, 8)).and(4194303);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w3.lanewise(LSHR, 14).or(w4.lanewise(LSHL, 18)).and(4194303);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w4.lanewise(LSHR, 4).and(4194303);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w4.lanewise(LSHR, 26).or(w5.lanewise(LSHL, 6));
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack1_23(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector w4 = (IntVector) ByteVector.fromArray(B, in, ip + 256).reinterpretShape(S, 0);
        IntVector w5 = (IntVector) ByteVector.fromArray(B, in, ip + 320, BYTE_TAIL3).rearrange(BYTE_EXPAND3, BYTE_WORD3).reinterpretShape(S,
            0);
        IntVector v0 = w0.and(8388607);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 23).or(w1.lanewise(LSHL, 9)).and(8388607);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w1.lanewise(LSHR, 14).or(w2.lanewise(LSHL, 18)).and(8388607);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w2.lanewise(LSHR, 5).and(8388607);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w2.lanewise(LSHR, 28).or(w3.lanewise(LSHL, 4)).and(8388607);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w3.lanewise(LSHR, 19).or(w4.lanewise(LSHL, 13)).and(8388607);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w4.lanewise(LSHR, 10).or(w5.lanewise(LSHL, 22)).and(8388607);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w5.lanewise(LSHR, 1);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack1_24(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector w4 = (IntVector) ByteVector.fromArray(B, in, ip + 256).reinterpretShape(S, 0);
        IntVector w5 = (IntVector) ByteVector.fromArray(B, in, ip + 320).reinterpretShape(S, 0);
        IntVector v0 = w0.and(16777215);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 24).or(w1.lanewise(LSHL, 8)).and(16777215);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w1.lanewise(LSHR, 16).or(w2.lanewise(LSHL, 16)).and(16777215);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w2.lanewise(LSHR, 8).and(16777215);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w3.and(16777215);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w3.lanewise(LSHR, 24).or(w4.lanewise(LSHL, 8)).and(16777215);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w4.lanewise(LSHR, 16).or(w5.lanewise(LSHL, 16)).and(16777215);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w5.lanewise(LSHR, 8);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack1_25(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector w4 = (IntVector) ByteVector.fromArray(B, in, ip + 256).reinterpretShape(S, 0);
        IntVector w5 = (IntVector) ByteVector.fromArray(B, in, ip + 320).reinterpretShape(S, 0);
        IntVector w6 = (IntVector) ByteVector.fromArray(B128, in, ip + 384).convertShape(ZERO_EXTEND_B2I, S, 0);
        IntVector v0 = w0.and(33554431);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 25).or(w1.lanewise(LSHL, 7)).and(33554431);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w1.lanewise(LSHR, 18).or(w2.lanewise(LSHL, 14)).and(33554431);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w2.lanewise(LSHR, 11).or(w3.lanewise(LSHL, 21)).and(33554431);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w3.lanewise(LSHR, 4).and(33554431);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w3.lanewise(LSHR, 29).or(w4.lanewise(LSHL, 3)).and(33554431);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w4.lanewise(LSHR, 22).or(w5.lanewise(LSHL, 10)).and(33554431);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w5.lanewise(LSHR, 15).or(w6.lanewise(LSHL, 17));
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack1_26(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector w4 = (IntVector) ByteVector.fromArray(B, in, ip + 256).reinterpretShape(S, 0);
        IntVector w5 = (IntVector) ByteVector.fromArray(B, in, ip + 320).reinterpretShape(S, 0);
        IntVector w6 = (IntVector) ByteVector.fromArray(B256, in, ip + 384).reinterpretAsShorts().convertShape(ZERO_EXTEND_S2I, S, 0);
        IntVector v0 = w0.and(67108863);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 26).or(w1.lanewise(LSHL, 6)).and(67108863);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w1.lanewise(LSHR, 20).or(w2.lanewise(LSHL, 12)).and(67108863);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w2.lanewise(LSHR, 14).or(w3.lanewise(LSHL, 18)).and(67108863);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w3.lanewise(LSHR, 8).or(w4.lanewise(LSHL, 24)).and(67108863);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w4.lanewise(LSHR, 2).and(67108863);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w4.lanewise(LSHR, 28).or(w5.lanewise(LSHL, 4)).and(67108863);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w5.lanewise(LSHR, 22).or(w6.lanewise(LSHL, 10));
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack1_27(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector w4 = (IntVector) ByteVector.fromArray(B, in, ip + 256).reinterpretShape(S, 0);
        IntVector w5 = (IntVector) ByteVector.fromArray(B, in, ip + 320).reinterpretShape(S, 0);
        IntVector w6 = (IntVector) ByteVector.fromArray(B, in, ip + 384, BYTE_TAIL3).rearrange(BYTE_EXPAND3, BYTE_WORD3).reinterpretShape(S,
            0);
        IntVector v0 = w0.and(134217727);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 27).or(w1.lanewise(LSHL, 5)).and(134217727);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w1.lanewise(LSHR, 22).or(w2.lanewise(LSHL, 10)).and(134217727);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w2.lanewise(LSHR, 17).or(w3.lanewise(LSHL, 15)).and(134217727);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w3.lanewise(LSHR, 12).or(w4.lanewise(LSHL, 20)).and(134217727);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w4.lanewise(LSHR, 7).or(w5.lanewise(LSHL, 25)).and(134217727);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w5.lanewise(LSHR, 2).and(134217727);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w5.lanewise(LSHR, 29).or(w6.lanewise(LSHL, 3));
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack1_28(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector w4 = (IntVector) ByteVector.fromArray(B, in, ip + 256).reinterpretShape(S, 0);
        IntVector w5 = (IntVector) ByteVector.fromArray(B, in, ip + 320).reinterpretShape(S, 0);
        IntVector w6 = (IntVector) ByteVector.fromArray(B, in, ip + 384).reinterpretShape(S, 0);
        IntVector v0 = w0.and(268435455);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 28).or(w1.lanewise(LSHL, 4)).and(268435455);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w1.lanewise(LSHR, 24).or(w2.lanewise(LSHL, 8)).and(268435455);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w2.lanewise(LSHR, 20).or(w3.lanewise(LSHL, 12)).and(268435455);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w3.lanewise(LSHR, 16).or(w4.lanewise(LSHL, 16)).and(268435455);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w4.lanewise(LSHR, 12).or(w5.lanewise(LSHL, 20)).and(268435455);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w5.lanewise(LSHR, 8).or(w6.lanewise(LSHL, 24)).and(268435455);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w6.lanewise(LSHR, 4);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack1_29(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector w4 = (IntVector) ByteVector.fromArray(B, in, ip + 256).reinterpretShape(S, 0);
        IntVector w5 = (IntVector) ByteVector.fromArray(B, in, ip + 320).reinterpretShape(S, 0);
        IntVector w6 = (IntVector) ByteVector.fromArray(B, in, ip + 384).reinterpretShape(S, 0);
        IntVector w7 = (IntVector) ByteVector.fromArray(B128, in, ip + 448).convertShape(ZERO_EXTEND_B2I, S, 0);
        IntVector v0 = w0.and(536870911);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 29).or(w1.lanewise(LSHL, 3)).and(536870911);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w1.lanewise(LSHR, 26).or(w2.lanewise(LSHL, 6)).and(536870911);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w2.lanewise(LSHR, 23).or(w3.lanewise(LSHL, 9)).and(536870911);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w3.lanewise(LSHR, 20).or(w4.lanewise(LSHL, 12)).and(536870911);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w4.lanewise(LSHR, 17).or(w5.lanewise(LSHL, 15)).and(536870911);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w5.lanewise(LSHR, 14).or(w6.lanewise(LSHL, 18)).and(536870911);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w6.lanewise(LSHR, 11).or(w7.lanewise(LSHL, 21));
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack1_30(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector w4 = (IntVector) ByteVector.fromArray(B, in, ip + 256).reinterpretShape(S, 0);
        IntVector w5 = (IntVector) ByteVector.fromArray(B, in, ip + 320).reinterpretShape(S, 0);
        IntVector w6 = (IntVector) ByteVector.fromArray(B, in, ip + 384).reinterpretShape(S, 0);
        IntVector w7 = (IntVector) ByteVector.fromArray(B256, in, ip + 448).reinterpretAsShorts().convertShape(ZERO_EXTEND_S2I, S, 0);
        IntVector v0 = w0.and(1073741823);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 30).or(w1.lanewise(LSHL, 2)).and(1073741823);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w1.lanewise(LSHR, 28).or(w2.lanewise(LSHL, 4)).and(1073741823);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w2.lanewise(LSHR, 26).or(w3.lanewise(LSHL, 6)).and(1073741823);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w3.lanewise(LSHR, 24).or(w4.lanewise(LSHL, 8)).and(1073741823);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w4.lanewise(LSHR, 22).or(w5.lanewise(LSHL, 10)).and(1073741823);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w5.lanewise(LSHR, 20).or(w6.lanewise(LSHL, 12)).and(1073741823);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w6.lanewise(LSHR, 18).or(w7.lanewise(LSHL, 14));
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack1_31(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector w4 = (IntVector) ByteVector.fromArray(B, in, ip + 256).reinterpretShape(S, 0);
        IntVector w5 = (IntVector) ByteVector.fromArray(B, in, ip + 320).reinterpretShape(S, 0);
        IntVector w6 = (IntVector) ByteVector.fromArray(B, in, ip + 384).reinterpretShape(S, 0);
        IntVector w7 = (IntVector) ByteVector.fromArray(B, in, ip + 448, BYTE_TAIL3).rearrange(BYTE_EXPAND3, BYTE_WORD3).reinterpretShape(S,
            0);
        IntVector v0 = w0.and(2147483647);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 31).or(w1.lanewise(LSHL, 1)).and(2147483647);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w1.lanewise(LSHR, 30).or(w2.lanewise(LSHL, 2)).and(2147483647);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w2.lanewise(LSHR, 29).or(w3.lanewise(LSHL, 3)).and(2147483647);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w3.lanewise(LSHR, 28).or(w4.lanewise(LSHL, 4)).and(2147483647);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w4.lanewise(LSHR, 27).or(w5.lanewise(LSHL, 5)).and(2147483647);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w5.lanewise(LSHR, 26).or(w6.lanewise(LSHL, 6)).and(2147483647);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w6.lanewise(LSHR, 25).or(w7.lanewise(LSHL, 7));
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack2(byte[] in, int ip, int[] out, int op, int bits, int initial) {
        switch (bits) {
            case 32 -> unpack2_32(in, ip, out, op, initial);
            case 1 -> unpack2_1(in, ip, out, op, initial);
            case 2 -> unpack2_2(in, ip, out, op, initial);
            case 3 -> unpack2_3(in, ip, out, op, initial);
            case 4 -> unpack2_4(in, ip, out, op, initial);
            case 5 -> unpack2_5(in, ip, out, op, initial);
            case 6 -> unpack2_6(in, ip, out, op, initial);
            case 7 -> unpack2_7(in, ip, out, op, initial);
            case 8 -> unpack2_8(in, ip, out, op, initial);
            case 9 -> unpack2_9(in, ip, out, op, initial);
            case 10 -> unpack2_10(in, ip, out, op, initial);
            case 11 -> unpack2_11(in, ip, out, op, initial);
            case 12 -> unpack2_12(in, ip, out, op, initial);
            case 13 -> unpack2_13(in, ip, out, op, initial);
            case 14 -> unpack2_14(in, ip, out, op, initial);
            case 15 -> unpack2_15(in, ip, out, op, initial);
            case 16 -> unpack2_16(in, ip, out, op, initial);
            case 17 -> unpack2_17(in, ip, out, op, initial);
            case 18 -> unpack2_18(in, ip, out, op, initial);
            case 19 -> unpack2_19(in, ip, out, op, initial);
            case 20 -> unpack2_20(in, ip, out, op, initial);
            case 21 -> unpack2_21(in, ip, out, op, initial);
            case 22 -> unpack2_22(in, ip, out, op, initial);
            case 23 -> unpack2_23(in, ip, out, op, initial);
            case 24 -> unpack2_24(in, ip, out, op, initial);
            case 25 -> unpack2_25(in, ip, out, op, initial);
            case 26 -> unpack2_26(in, ip, out, op, initial);
            case 27 -> unpack2_27(in, ip, out, op, initial);
            case 28 -> unpack2_28(in, ip, out, op, initial);
            case 29 -> unpack2_29(in, ip, out, op, initial);
            case 30 -> unpack2_30(in, ip, out, op, initial);
            case 31 -> unpack2_31(in, ip, out, op, initial);
            default -> throw new IllegalArgumentException("Unsupported width");
        }
    }

    private static void unpack2_1(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B128, in, ip + 0).convertShape(ZERO_EXTEND_B2I, S, 0);
        IntVector v0 = w0.and(1);
        v0 = v0.add(ONE);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 1).and(1);
        v1 = v1.add(ONE);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w0.lanewise(LSHR, 2).and(1);
        v2 = v2.add(ONE);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w0.lanewise(LSHR, 3).and(1);
        v3 = v3.add(ONE);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w0.lanewise(LSHR, 4).and(1);
        v4 = v4.add(ONE);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w0.lanewise(LSHR, 5).and(1);
        v5 = v5.add(ONE);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w0.lanewise(LSHR, 6).and(1);
        v6 = v6.add(ONE);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w0.lanewise(LSHR, 7);
        v7 = v7.add(ONE);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack2_2(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B256, in, ip + 0).reinterpretAsShorts().convertShape(ZERO_EXTEND_S2I, S, 0);
        IntVector v0 = w0.and(3);
        v0 = v0.add(ONE);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 2).and(3);
        v1 = v1.add(ONE);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w0.lanewise(LSHR, 4).and(3);
        v2 = v2.add(ONE);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w0.lanewise(LSHR, 6).and(3);
        v3 = v3.add(ONE);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w0.lanewise(LSHR, 8).and(3);
        v4 = v4.add(ONE);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w0.lanewise(LSHR, 10).and(3);
        v5 = v5.add(ONE);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w0.lanewise(LSHR, 12).and(3);
        v6 = v6.add(ONE);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w0.lanewise(LSHR, 14);
        v7 = v7.add(ONE);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack2_3(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0, BYTE_TAIL3).rearrange(BYTE_EXPAND3, BYTE_WORD3).reinterpretShape(S,
            0);
        IntVector v0 = w0.and(7);
        v0 = v0.add(ONE);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 3).and(7);
        v1 = v1.add(ONE);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w0.lanewise(LSHR, 6).and(7);
        v2 = v2.add(ONE);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w0.lanewise(LSHR, 9).and(7);
        v3 = v3.add(ONE);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w0.lanewise(LSHR, 12).and(7);
        v4 = v4.add(ONE);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w0.lanewise(LSHR, 15).and(7);
        v5 = v5.add(ONE);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w0.lanewise(LSHR, 18).and(7);
        v6 = v6.add(ONE);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w0.lanewise(LSHR, 21);
        v7 = v7.add(ONE);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack2_4(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector v0 = w0.and(15);
        v0 = v0.add(ONE);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 4).and(15);
        v1 = v1.add(ONE);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w0.lanewise(LSHR, 8).and(15);
        v2 = v2.add(ONE);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w0.lanewise(LSHR, 12).and(15);
        v3 = v3.add(ONE);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w0.lanewise(LSHR, 16).and(15);
        v4 = v4.add(ONE);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w0.lanewise(LSHR, 20).and(15);
        v5 = v5.add(ONE);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w0.lanewise(LSHR, 24).and(15);
        v6 = v6.add(ONE);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w0.lanewise(LSHR, 28);
        v7 = v7.add(ONE);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack2_5(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B128, in, ip + 64).convertShape(ZERO_EXTEND_B2I, S, 0);
        IntVector v0 = w0.and(31);
        v0 = v0.add(ONE);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 5).and(31);
        v1 = v1.add(ONE);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w0.lanewise(LSHR, 10).and(31);
        v2 = v2.add(ONE);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w0.lanewise(LSHR, 15).and(31);
        v3 = v3.add(ONE);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w0.lanewise(LSHR, 20).and(31);
        v4 = v4.add(ONE);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w0.lanewise(LSHR, 25).and(31);
        v5 = v5.add(ONE);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w0.lanewise(LSHR, 30).or(w1.lanewise(LSHL, 2)).and(31);
        v6 = v6.add(ONE);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w1.lanewise(LSHR, 3);
        v7 = v7.add(ONE);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack2_6(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B256, in, ip + 64).reinterpretAsShorts().convertShape(ZERO_EXTEND_S2I, S, 0);
        IntVector v0 = w0.and(63);
        v0 = v0.add(ONE);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 6).and(63);
        v1 = v1.add(ONE);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w0.lanewise(LSHR, 12).and(63);
        v2 = v2.add(ONE);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w0.lanewise(LSHR, 18).and(63);
        v3 = v3.add(ONE);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w0.lanewise(LSHR, 24).and(63);
        v4 = v4.add(ONE);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w0.lanewise(LSHR, 30).or(w1.lanewise(LSHL, 2)).and(63);
        v5 = v5.add(ONE);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w1.lanewise(LSHR, 4).and(63);
        v6 = v6.add(ONE);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w1.lanewise(LSHR, 10);
        v7 = v7.add(ONE);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack2_7(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64, BYTE_TAIL3).rearrange(BYTE_EXPAND3, BYTE_WORD3).reinterpretShape(S,
            0);
        IntVector v0 = w0.and(127);
        v0 = v0.add(ONE);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 7).and(127);
        v1 = v1.add(ONE);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w0.lanewise(LSHR, 14).and(127);
        v2 = v2.add(ONE);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w0.lanewise(LSHR, 21).and(127);
        v3 = v3.add(ONE);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w0.lanewise(LSHR, 28).or(w1.lanewise(LSHL, 4)).and(127);
        v4 = v4.add(ONE);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w1.lanewise(LSHR, 3).and(127);
        v5 = v5.add(ONE);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w1.lanewise(LSHR, 10).and(127);
        v6 = v6.add(ONE);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w1.lanewise(LSHR, 17);
        v7 = v7.add(ONE);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack2_8(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector v0 = w0.and(255);
        v0 = v0.add(ONE);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 8).and(255);
        v1 = v1.add(ONE);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w0.lanewise(LSHR, 16).and(255);
        v2 = v2.add(ONE);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w0.lanewise(LSHR, 24).and(255);
        v3 = v3.add(ONE);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w1.and(255);
        v4 = v4.add(ONE);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w1.lanewise(LSHR, 8).and(255);
        v5 = v5.add(ONE);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w1.lanewise(LSHR, 16).and(255);
        v6 = v6.add(ONE);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w1.lanewise(LSHR, 24);
        v7 = v7.add(ONE);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack2_9(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B128, in, ip + 128).convertShape(ZERO_EXTEND_B2I, S, 0);
        IntVector v0 = w0.and(511);
        v0 = v0.add(ONE);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 9).and(511);
        v1 = v1.add(ONE);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w0.lanewise(LSHR, 18).and(511);
        v2 = v2.add(ONE);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w0.lanewise(LSHR, 27).or(w1.lanewise(LSHL, 5)).and(511);
        v3 = v3.add(ONE);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w1.lanewise(LSHR, 4).and(511);
        v4 = v4.add(ONE);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w1.lanewise(LSHR, 13).and(511);
        v5 = v5.add(ONE);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w1.lanewise(LSHR, 22).and(511);
        v6 = v6.add(ONE);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w1.lanewise(LSHR, 31).or(w2.lanewise(LSHL, 1));
        v7 = v7.add(ONE);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack2_10(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B256, in, ip + 128).reinterpretAsShorts().convertShape(ZERO_EXTEND_S2I, S, 0);
        IntVector v0 = w0.and(1023);
        v0 = v0.add(ONE);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 10).and(1023);
        v1 = v1.add(ONE);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w0.lanewise(LSHR, 20).and(1023);
        v2 = v2.add(ONE);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w0.lanewise(LSHR, 30).or(w1.lanewise(LSHL, 2)).and(1023);
        v3 = v3.add(ONE);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w1.lanewise(LSHR, 8).and(1023);
        v4 = v4.add(ONE);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w1.lanewise(LSHR, 18).and(1023);
        v5 = v5.add(ONE);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w1.lanewise(LSHR, 28).or(w2.lanewise(LSHL, 4)).and(1023);
        v6 = v6.add(ONE);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w2.lanewise(LSHR, 6);
        v7 = v7.add(ONE);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack2_11(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128, BYTE_TAIL3).rearrange(BYTE_EXPAND3, BYTE_WORD3).reinterpretShape(S,
            0);
        IntVector v0 = w0.and(2047);
        v0 = v0.add(ONE);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 11).and(2047);
        v1 = v1.add(ONE);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w0.lanewise(LSHR, 22).or(w1.lanewise(LSHL, 10)).and(2047);
        v2 = v2.add(ONE);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w1.lanewise(LSHR, 1).and(2047);
        v3 = v3.add(ONE);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w1.lanewise(LSHR, 12).and(2047);
        v4 = v4.add(ONE);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w1.lanewise(LSHR, 23).or(w2.lanewise(LSHL, 9)).and(2047);
        v5 = v5.add(ONE);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w2.lanewise(LSHR, 2).and(2047);
        v6 = v6.add(ONE);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w2.lanewise(LSHR, 13);
        v7 = v7.add(ONE);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack2_12(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector v0 = w0.and(4095);
        v0 = v0.add(ONE);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 12).and(4095);
        v1 = v1.add(ONE);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w0.lanewise(LSHR, 24).or(w1.lanewise(LSHL, 8)).and(4095);
        v2 = v2.add(ONE);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w1.lanewise(LSHR, 4).and(4095);
        v3 = v3.add(ONE);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w1.lanewise(LSHR, 16).and(4095);
        v4 = v4.add(ONE);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w1.lanewise(LSHR, 28).or(w2.lanewise(LSHL, 4)).and(4095);
        v5 = v5.add(ONE);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w2.lanewise(LSHR, 8).and(4095);
        v6 = v6.add(ONE);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w2.lanewise(LSHR, 20);
        v7 = v7.add(ONE);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack2_13(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B128, in, ip + 192).convertShape(ZERO_EXTEND_B2I, S, 0);
        IntVector v0 = w0.and(8191);
        v0 = v0.add(ONE);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 13).and(8191);
        v1 = v1.add(ONE);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w0.lanewise(LSHR, 26).or(w1.lanewise(LSHL, 6)).and(8191);
        v2 = v2.add(ONE);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w1.lanewise(LSHR, 7).and(8191);
        v3 = v3.add(ONE);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w1.lanewise(LSHR, 20).or(w2.lanewise(LSHL, 12)).and(8191);
        v4 = v4.add(ONE);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w2.lanewise(LSHR, 1).and(8191);
        v5 = v5.add(ONE);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w2.lanewise(LSHR, 14).and(8191);
        v6 = v6.add(ONE);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w2.lanewise(LSHR, 27).or(w3.lanewise(LSHL, 5));
        v7 = v7.add(ONE);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack2_14(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B256, in, ip + 192).reinterpretAsShorts().convertShape(ZERO_EXTEND_S2I, S, 0);
        IntVector v0 = w0.and(16383);
        v0 = v0.add(ONE);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 14).and(16383);
        v1 = v1.add(ONE);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w0.lanewise(LSHR, 28).or(w1.lanewise(LSHL, 4)).and(16383);
        v2 = v2.add(ONE);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w1.lanewise(LSHR, 10).and(16383);
        v3 = v3.add(ONE);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w1.lanewise(LSHR, 24).or(w2.lanewise(LSHL, 8)).and(16383);
        v4 = v4.add(ONE);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w2.lanewise(LSHR, 6).and(16383);
        v5 = v5.add(ONE);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w2.lanewise(LSHR, 20).or(w3.lanewise(LSHL, 12)).and(16383);
        v6 = v6.add(ONE);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w3.lanewise(LSHR, 2);
        v7 = v7.add(ONE);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack2_15(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192, BYTE_TAIL3).rearrange(BYTE_EXPAND3, BYTE_WORD3).reinterpretShape(S,
            0);
        IntVector v0 = w0.and(32767);
        v0 = v0.add(ONE);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 15).and(32767);
        v1 = v1.add(ONE);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w0.lanewise(LSHR, 30).or(w1.lanewise(LSHL, 2)).and(32767);
        v2 = v2.add(ONE);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w1.lanewise(LSHR, 13).and(32767);
        v3 = v3.add(ONE);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w1.lanewise(LSHR, 28).or(w2.lanewise(LSHL, 4)).and(32767);
        v4 = v4.add(ONE);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w2.lanewise(LSHR, 11).and(32767);
        v5 = v5.add(ONE);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w2.lanewise(LSHR, 26).or(w3.lanewise(LSHL, 6)).and(32767);
        v6 = v6.add(ONE);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w3.lanewise(LSHR, 9);
        v7 = v7.add(ONE);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack2_16(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector v0 = w0.and(65535);
        v0 = v0.add(ONE);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 16).and(65535);
        v1 = v1.add(ONE);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w1.and(65535);
        v2 = v2.add(ONE);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w1.lanewise(LSHR, 16).and(65535);
        v3 = v3.add(ONE);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w2.and(65535);
        v4 = v4.add(ONE);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w2.lanewise(LSHR, 16).and(65535);
        v5 = v5.add(ONE);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w3.and(65535);
        v6 = v6.add(ONE);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w3.lanewise(LSHR, 16);
        v7 = v7.add(ONE);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack2_17(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector w4 = (IntVector) ByteVector.fromArray(B128, in, ip + 256).convertShape(ZERO_EXTEND_B2I, S, 0);
        IntVector v0 = w0.and(131071);
        v0 = v0.add(ONE);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 17).or(w1.lanewise(LSHL, 15)).and(131071);
        v1 = v1.add(ONE);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w1.lanewise(LSHR, 2).and(131071);
        v2 = v2.add(ONE);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w1.lanewise(LSHR, 19).or(w2.lanewise(LSHL, 13)).and(131071);
        v3 = v3.add(ONE);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w2.lanewise(LSHR, 4).and(131071);
        v4 = v4.add(ONE);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w2.lanewise(LSHR, 21).or(w3.lanewise(LSHL, 11)).and(131071);
        v5 = v5.add(ONE);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w3.lanewise(LSHR, 6).and(131071);
        v6 = v6.add(ONE);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w3.lanewise(LSHR, 23).or(w4.lanewise(LSHL, 9));
        v7 = v7.add(ONE);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack2_18(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector w4 = (IntVector) ByteVector.fromArray(B256, in, ip + 256).reinterpretAsShorts().convertShape(ZERO_EXTEND_S2I, S, 0);
        IntVector v0 = w0.and(262143);
        v0 = v0.add(ONE);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 18).or(w1.lanewise(LSHL, 14)).and(262143);
        v1 = v1.add(ONE);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w1.lanewise(LSHR, 4).and(262143);
        v2 = v2.add(ONE);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w1.lanewise(LSHR, 22).or(w2.lanewise(LSHL, 10)).and(262143);
        v3 = v3.add(ONE);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w2.lanewise(LSHR, 8).and(262143);
        v4 = v4.add(ONE);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w2.lanewise(LSHR, 26).or(w3.lanewise(LSHL, 6)).and(262143);
        v5 = v5.add(ONE);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w3.lanewise(LSHR, 12).and(262143);
        v6 = v6.add(ONE);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w3.lanewise(LSHR, 30).or(w4.lanewise(LSHL, 2));
        v7 = v7.add(ONE);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack2_19(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector w4 = (IntVector) ByteVector.fromArray(B, in, ip + 256, BYTE_TAIL3).rearrange(BYTE_EXPAND3, BYTE_WORD3).reinterpretShape(S,
            0);
        IntVector v0 = w0.and(524287);
        v0 = v0.add(ONE);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 19).or(w1.lanewise(LSHL, 13)).and(524287);
        v1 = v1.add(ONE);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w1.lanewise(LSHR, 6).and(524287);
        v2 = v2.add(ONE);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w1.lanewise(LSHR, 25).or(w2.lanewise(LSHL, 7)).and(524287);
        v3 = v3.add(ONE);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w2.lanewise(LSHR, 12).and(524287);
        v4 = v4.add(ONE);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w2.lanewise(LSHR, 31).or(w3.lanewise(LSHL, 1)).and(524287);
        v5 = v5.add(ONE);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w3.lanewise(LSHR, 18).or(w4.lanewise(LSHL, 14)).and(524287);
        v6 = v6.add(ONE);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w4.lanewise(LSHR, 5);
        v7 = v7.add(ONE);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack2_20(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector w4 = (IntVector) ByteVector.fromArray(B, in, ip + 256).reinterpretShape(S, 0);
        IntVector v0 = w0.and(1048575);
        v0 = v0.add(ONE);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 20).or(w1.lanewise(LSHL, 12)).and(1048575);
        v1 = v1.add(ONE);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w1.lanewise(LSHR, 8).and(1048575);
        v2 = v2.add(ONE);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w1.lanewise(LSHR, 28).or(w2.lanewise(LSHL, 4)).and(1048575);
        v3 = v3.add(ONE);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w2.lanewise(LSHR, 16).or(w3.lanewise(LSHL, 16)).and(1048575);
        v4 = v4.add(ONE);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w3.lanewise(LSHR, 4).and(1048575);
        v5 = v5.add(ONE);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w3.lanewise(LSHR, 24).or(w4.lanewise(LSHL, 8)).and(1048575);
        v6 = v6.add(ONE);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w4.lanewise(LSHR, 12);
        v7 = v7.add(ONE);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack2_21(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector w4 = (IntVector) ByteVector.fromArray(B, in, ip + 256).reinterpretShape(S, 0);
        IntVector w5 = (IntVector) ByteVector.fromArray(B128, in, ip + 320).convertShape(ZERO_EXTEND_B2I, S, 0);
        IntVector v0 = w0.and(2097151);
        v0 = v0.add(ONE);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 21).or(w1.lanewise(LSHL, 11)).and(2097151);
        v1 = v1.add(ONE);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w1.lanewise(LSHR, 10).and(2097151);
        v2 = v2.add(ONE);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w1.lanewise(LSHR, 31).or(w2.lanewise(LSHL, 1)).and(2097151);
        v3 = v3.add(ONE);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w2.lanewise(LSHR, 20).or(w3.lanewise(LSHL, 12)).and(2097151);
        v4 = v4.add(ONE);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w3.lanewise(LSHR, 9).and(2097151);
        v5 = v5.add(ONE);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w3.lanewise(LSHR, 30).or(w4.lanewise(LSHL, 2)).and(2097151);
        v6 = v6.add(ONE);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w4.lanewise(LSHR, 19).or(w5.lanewise(LSHL, 13));
        v7 = v7.add(ONE);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack2_22(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector w4 = (IntVector) ByteVector.fromArray(B, in, ip + 256).reinterpretShape(S, 0);
        IntVector w5 = (IntVector) ByteVector.fromArray(B256, in, ip + 320).reinterpretAsShorts().convertShape(ZERO_EXTEND_S2I, S, 0);
        IntVector v0 = w0.and(4194303);
        v0 = v0.add(ONE);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 22).or(w1.lanewise(LSHL, 10)).and(4194303);
        v1 = v1.add(ONE);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w1.lanewise(LSHR, 12).or(w2.lanewise(LSHL, 20)).and(4194303);
        v2 = v2.add(ONE);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w2.lanewise(LSHR, 2).and(4194303);
        v3 = v3.add(ONE);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w2.lanewise(LSHR, 24).or(w3.lanewise(LSHL, 8)).and(4194303);
        v4 = v4.add(ONE);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w3.lanewise(LSHR, 14).or(w4.lanewise(LSHL, 18)).and(4194303);
        v5 = v5.add(ONE);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w4.lanewise(LSHR, 4).and(4194303);
        v6 = v6.add(ONE);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w4.lanewise(LSHR, 26).or(w5.lanewise(LSHL, 6));
        v7 = v7.add(ONE);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack2_23(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector w4 = (IntVector) ByteVector.fromArray(B, in, ip + 256).reinterpretShape(S, 0);
        IntVector w5 = (IntVector) ByteVector.fromArray(B, in, ip + 320, BYTE_TAIL3).rearrange(BYTE_EXPAND3, BYTE_WORD3).reinterpretShape(S,
            0);
        IntVector v0 = w0.and(8388607);
        v0 = v0.add(ONE);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 23).or(w1.lanewise(LSHL, 9)).and(8388607);
        v1 = v1.add(ONE);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w1.lanewise(LSHR, 14).or(w2.lanewise(LSHL, 18)).and(8388607);
        v2 = v2.add(ONE);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w2.lanewise(LSHR, 5).and(8388607);
        v3 = v3.add(ONE);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w2.lanewise(LSHR, 28).or(w3.lanewise(LSHL, 4)).and(8388607);
        v4 = v4.add(ONE);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w3.lanewise(LSHR, 19).or(w4.lanewise(LSHL, 13)).and(8388607);
        v5 = v5.add(ONE);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w4.lanewise(LSHR, 10).or(w5.lanewise(LSHL, 22)).and(8388607);
        v6 = v6.add(ONE);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w5.lanewise(LSHR, 1);
        v7 = v7.add(ONE);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack2_24(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector w4 = (IntVector) ByteVector.fromArray(B, in, ip + 256).reinterpretShape(S, 0);
        IntVector w5 = (IntVector) ByteVector.fromArray(B, in, ip + 320).reinterpretShape(S, 0);
        IntVector v0 = w0.and(16777215);
        v0 = v0.add(ONE);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 24).or(w1.lanewise(LSHL, 8)).and(16777215);
        v1 = v1.add(ONE);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w1.lanewise(LSHR, 16).or(w2.lanewise(LSHL, 16)).and(16777215);
        v2 = v2.add(ONE);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w2.lanewise(LSHR, 8).and(16777215);
        v3 = v3.add(ONE);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w3.and(16777215);
        v4 = v4.add(ONE);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w3.lanewise(LSHR, 24).or(w4.lanewise(LSHL, 8)).and(16777215);
        v5 = v5.add(ONE);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w4.lanewise(LSHR, 16).or(w5.lanewise(LSHL, 16)).and(16777215);
        v6 = v6.add(ONE);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w5.lanewise(LSHR, 8);
        v7 = v7.add(ONE);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack2_25(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector w4 = (IntVector) ByteVector.fromArray(B, in, ip + 256).reinterpretShape(S, 0);
        IntVector w5 = (IntVector) ByteVector.fromArray(B, in, ip + 320).reinterpretShape(S, 0);
        IntVector w6 = (IntVector) ByteVector.fromArray(B128, in, ip + 384).convertShape(ZERO_EXTEND_B2I, S, 0);
        IntVector v0 = w0.and(33554431);
        v0 = v0.add(ONE);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 25).or(w1.lanewise(LSHL, 7)).and(33554431);
        v1 = v1.add(ONE);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w1.lanewise(LSHR, 18).or(w2.lanewise(LSHL, 14)).and(33554431);
        v2 = v2.add(ONE);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w2.lanewise(LSHR, 11).or(w3.lanewise(LSHL, 21)).and(33554431);
        v3 = v3.add(ONE);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w3.lanewise(LSHR, 4).and(33554431);
        v4 = v4.add(ONE);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w3.lanewise(LSHR, 29).or(w4.lanewise(LSHL, 3)).and(33554431);
        v5 = v5.add(ONE);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w4.lanewise(LSHR, 22).or(w5.lanewise(LSHL, 10)).and(33554431);
        v6 = v6.add(ONE);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w5.lanewise(LSHR, 15).or(w6.lanewise(LSHL, 17));
        v7 = v7.add(ONE);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack2_26(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector w4 = (IntVector) ByteVector.fromArray(B, in, ip + 256).reinterpretShape(S, 0);
        IntVector w5 = (IntVector) ByteVector.fromArray(B, in, ip + 320).reinterpretShape(S, 0);
        IntVector w6 = (IntVector) ByteVector.fromArray(B256, in, ip + 384).reinterpretAsShorts().convertShape(ZERO_EXTEND_S2I, S, 0);
        IntVector v0 = w0.and(67108863);
        v0 = v0.add(ONE);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 26).or(w1.lanewise(LSHL, 6)).and(67108863);
        v1 = v1.add(ONE);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w1.lanewise(LSHR, 20).or(w2.lanewise(LSHL, 12)).and(67108863);
        v2 = v2.add(ONE);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w2.lanewise(LSHR, 14).or(w3.lanewise(LSHL, 18)).and(67108863);
        v3 = v3.add(ONE);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w3.lanewise(LSHR, 8).or(w4.lanewise(LSHL, 24)).and(67108863);
        v4 = v4.add(ONE);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w4.lanewise(LSHR, 2).and(67108863);
        v5 = v5.add(ONE);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w4.lanewise(LSHR, 28).or(w5.lanewise(LSHL, 4)).and(67108863);
        v6 = v6.add(ONE);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w5.lanewise(LSHR, 22).or(w6.lanewise(LSHL, 10));
        v7 = v7.add(ONE);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack2_27(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector w4 = (IntVector) ByteVector.fromArray(B, in, ip + 256).reinterpretShape(S, 0);
        IntVector w5 = (IntVector) ByteVector.fromArray(B, in, ip + 320).reinterpretShape(S, 0);
        IntVector w6 = (IntVector) ByteVector.fromArray(B, in, ip + 384, BYTE_TAIL3).rearrange(BYTE_EXPAND3, BYTE_WORD3).reinterpretShape(S,
            0);
        IntVector v0 = w0.and(134217727);
        v0 = v0.add(ONE);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 27).or(w1.lanewise(LSHL, 5)).and(134217727);
        v1 = v1.add(ONE);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w1.lanewise(LSHR, 22).or(w2.lanewise(LSHL, 10)).and(134217727);
        v2 = v2.add(ONE);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w2.lanewise(LSHR, 17).or(w3.lanewise(LSHL, 15)).and(134217727);
        v3 = v3.add(ONE);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w3.lanewise(LSHR, 12).or(w4.lanewise(LSHL, 20)).and(134217727);
        v4 = v4.add(ONE);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w4.lanewise(LSHR, 7).or(w5.lanewise(LSHL, 25)).and(134217727);
        v5 = v5.add(ONE);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w5.lanewise(LSHR, 2).and(134217727);
        v6 = v6.add(ONE);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w5.lanewise(LSHR, 29).or(w6.lanewise(LSHL, 3));
        v7 = v7.add(ONE);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack2_28(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector w4 = (IntVector) ByteVector.fromArray(B, in, ip + 256).reinterpretShape(S, 0);
        IntVector w5 = (IntVector) ByteVector.fromArray(B, in, ip + 320).reinterpretShape(S, 0);
        IntVector w6 = (IntVector) ByteVector.fromArray(B, in, ip + 384).reinterpretShape(S, 0);
        IntVector v0 = w0.and(268435455);
        v0 = v0.add(ONE);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 28).or(w1.lanewise(LSHL, 4)).and(268435455);
        v1 = v1.add(ONE);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w1.lanewise(LSHR, 24).or(w2.lanewise(LSHL, 8)).and(268435455);
        v2 = v2.add(ONE);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w2.lanewise(LSHR, 20).or(w3.lanewise(LSHL, 12)).and(268435455);
        v3 = v3.add(ONE);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w3.lanewise(LSHR, 16).or(w4.lanewise(LSHL, 16)).and(268435455);
        v4 = v4.add(ONE);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w4.lanewise(LSHR, 12).or(w5.lanewise(LSHL, 20)).and(268435455);
        v5 = v5.add(ONE);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w5.lanewise(LSHR, 8).or(w6.lanewise(LSHL, 24)).and(268435455);
        v6 = v6.add(ONE);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w6.lanewise(LSHR, 4);
        v7 = v7.add(ONE);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack2_29(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector w4 = (IntVector) ByteVector.fromArray(B, in, ip + 256).reinterpretShape(S, 0);
        IntVector w5 = (IntVector) ByteVector.fromArray(B, in, ip + 320).reinterpretShape(S, 0);
        IntVector w6 = (IntVector) ByteVector.fromArray(B, in, ip + 384).reinterpretShape(S, 0);
        IntVector w7 = (IntVector) ByteVector.fromArray(B128, in, ip + 448).convertShape(ZERO_EXTEND_B2I, S, 0);
        IntVector v0 = w0.and(536870911);
        v0 = v0.add(ONE);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 29).or(w1.lanewise(LSHL, 3)).and(536870911);
        v1 = v1.add(ONE);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w1.lanewise(LSHR, 26).or(w2.lanewise(LSHL, 6)).and(536870911);
        v2 = v2.add(ONE);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w2.lanewise(LSHR, 23).or(w3.lanewise(LSHL, 9)).and(536870911);
        v3 = v3.add(ONE);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w3.lanewise(LSHR, 20).or(w4.lanewise(LSHL, 12)).and(536870911);
        v4 = v4.add(ONE);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w4.lanewise(LSHR, 17).or(w5.lanewise(LSHL, 15)).and(536870911);
        v5 = v5.add(ONE);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w5.lanewise(LSHR, 14).or(w6.lanewise(LSHL, 18)).and(536870911);
        v6 = v6.add(ONE);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w6.lanewise(LSHR, 11).or(w7.lanewise(LSHL, 21));
        v7 = v7.add(ONE);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack2_30(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector w4 = (IntVector) ByteVector.fromArray(B, in, ip + 256).reinterpretShape(S, 0);
        IntVector w5 = (IntVector) ByteVector.fromArray(B, in, ip + 320).reinterpretShape(S, 0);
        IntVector w6 = (IntVector) ByteVector.fromArray(B, in, ip + 384).reinterpretShape(S, 0);
        IntVector w7 = (IntVector) ByteVector.fromArray(B256, in, ip + 448).reinterpretAsShorts().convertShape(ZERO_EXTEND_S2I, S, 0);
        IntVector v0 = w0.and(1073741823);
        v0 = v0.add(ONE);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 30).or(w1.lanewise(LSHL, 2)).and(1073741823);
        v1 = v1.add(ONE);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w1.lanewise(LSHR, 28).or(w2.lanewise(LSHL, 4)).and(1073741823);
        v2 = v2.add(ONE);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w2.lanewise(LSHR, 26).or(w3.lanewise(LSHL, 6)).and(1073741823);
        v3 = v3.add(ONE);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w3.lanewise(LSHR, 24).or(w4.lanewise(LSHL, 8)).and(1073741823);
        v4 = v4.add(ONE);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w4.lanewise(LSHR, 22).or(w5.lanewise(LSHL, 10)).and(1073741823);
        v5 = v5.add(ONE);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w5.lanewise(LSHR, 20).or(w6.lanewise(LSHL, 12)).and(1073741823);
        v6 = v6.add(ONE);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w6.lanewise(LSHR, 18).or(w7.lanewise(LSHL, 14));
        v7 = v7.add(ONE);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack2_31(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector w4 = (IntVector) ByteVector.fromArray(B, in, ip + 256).reinterpretShape(S, 0);
        IntVector w5 = (IntVector) ByteVector.fromArray(B, in, ip + 320).reinterpretShape(S, 0);
        IntVector w6 = (IntVector) ByteVector.fromArray(B, in, ip + 384).reinterpretShape(S, 0);
        IntVector w7 = (IntVector) ByteVector.fromArray(B, in, ip + 448, BYTE_TAIL3).rearrange(BYTE_EXPAND3, BYTE_WORD3).reinterpretShape(S,
            0);
        IntVector v0 = w0.and(2147483647);
        v0 = v0.add(ONE);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w0.lanewise(LSHR, 31).or(w1.lanewise(LSHL, 1)).and(2147483647);
        v1 = v1.add(ONE);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w1.lanewise(LSHR, 30).or(w2.lanewise(LSHL, 2)).and(2147483647);
        v2 = v2.add(ONE);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w2.lanewise(LSHR, 29).or(w3.lanewise(LSHL, 3)).and(2147483647);
        v3 = v3.add(ONE);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w3.lanewise(LSHR, 28).or(w4.lanewise(LSHL, 4)).and(2147483647);
        v4 = v4.add(ONE);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w4.lanewise(LSHR, 27).or(w5.lanewise(LSHL, 5)).and(2147483647);
        v5 = v5.add(ONE);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w5.lanewise(LSHR, 26).or(w6.lanewise(LSHL, 6)).and(2147483647);
        v6 = v6.add(ONE);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w6.lanewise(LSHR, 25).or(w7.lanewise(LSHL, 7));
        v7 = v7.add(ONE);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void pack0_32(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector v0 = IntVector.fromArray(S, in, ip + 0);
        ((ByteVector) v0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector v16 = IntVector.fromArray(S, in, ip + 16);
        ((ByteVector) v16.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector v32 = IntVector.fromArray(S, in, ip + 32);
        ((ByteVector) v32.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector v48 = IntVector.fromArray(S, in, ip + 48);
        ((ByteVector) v48.reinterpretShape(B, 0)).intoArray(out, op + 192);
        IntVector v64 = IntVector.fromArray(S, in, ip + 64);
        ((ByteVector) v64.reinterpretShape(B, 0)).intoArray(out, op + 256);
        IntVector v80 = IntVector.fromArray(S, in, ip + 80);
        ((ByteVector) v80.reinterpretShape(B, 0)).intoArray(out, op + 320);
        IntVector v96 = IntVector.fromArray(S, in, ip + 96);
        ((ByteVector) v96.reinterpretShape(B, 0)).intoArray(out, op + 384);
        IntVector v112 = IntVector.fromArray(S, in, ip + 112);
        ((ByteVector) v112.reinterpretShape(B, 0)).intoArray(out, op + 448);
    }

    private static void pack1_32(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector previous = IntVector.broadcast(S, initial);
        IntVector v0 = IntVector.fromArray(S, in, ip + 0);
        IntVector neighbor0 = PREVIOUS_INDEX.selectFrom(previous, v0);
        IntVector d0 = v0.sub(neighbor0);
        previous = v0;
        ((ByteVector) d0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector v16 = IntVector.fromArray(S, in, ip + 16);
        IntVector neighbor16 = PREVIOUS_INDEX.selectFrom(previous, v16);
        IntVector d16 = v16.sub(neighbor16);
        previous = v16;
        ((ByteVector) d16.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector v32 = IntVector.fromArray(S, in, ip + 32);
        IntVector neighbor32 = PREVIOUS_INDEX.selectFrom(previous, v32);
        IntVector d32 = v32.sub(neighbor32);
        previous = v32;
        ((ByteVector) d32.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector v48 = IntVector.fromArray(S, in, ip + 48);
        IntVector neighbor48 = PREVIOUS_INDEX.selectFrom(previous, v48);
        IntVector d48 = v48.sub(neighbor48);
        previous = v48;
        ((ByteVector) d48.reinterpretShape(B, 0)).intoArray(out, op + 192);
        IntVector v64 = IntVector.fromArray(S, in, ip + 64);
        IntVector neighbor64 = PREVIOUS_INDEX.selectFrom(previous, v64);
        IntVector d64 = v64.sub(neighbor64);
        previous = v64;
        ((ByteVector) d64.reinterpretShape(B, 0)).intoArray(out, op + 256);
        IntVector v80 = IntVector.fromArray(S, in, ip + 80);
        IntVector neighbor80 = PREVIOUS_INDEX.selectFrom(previous, v80);
        IntVector d80 = v80.sub(neighbor80);
        previous = v80;
        ((ByteVector) d80.reinterpretShape(B, 0)).intoArray(out, op + 320);
        IntVector v96 = IntVector.fromArray(S, in, ip + 96);
        IntVector neighbor96 = PREVIOUS_INDEX.selectFrom(previous, v96);
        IntVector d96 = v96.sub(neighbor96);
        previous = v96;
        ((ByteVector) d96.reinterpretShape(B, 0)).intoArray(out, op + 384);
        IntVector v112 = IntVector.fromArray(S, in, ip + 112);
        IntVector neighbor112 = PREVIOUS_INDEX.selectFrom(previous, v112);
        IntVector d112 = v112.sub(neighbor112);
        previous = v112;
        ((ByteVector) d112.reinterpretShape(B, 0)).intoArray(out, op + 448);
    }

    private static void pack2_32(int[] in, int ip, byte[] out, int op, int initial) {
        IntVector previous = IntVector.broadcast(S, initial);
        IntVector v0 = IntVector.fromArray(S, in, ip + 0);
        IntVector neighbor0 = PREVIOUS_INDEX.selectFrom(previous, v0);
        IntVector d0 = v0.sub(neighbor0);
        d0 = d0.sub(ONE);
        previous = v0;
        ((ByteVector) d0.reinterpretShape(B, 0)).intoArray(out, op + 0);
        IntVector v16 = IntVector.fromArray(S, in, ip + 16);
        IntVector neighbor16 = PREVIOUS_INDEX.selectFrom(previous, v16);
        IntVector d16 = v16.sub(neighbor16);
        d16 = d16.sub(ONE);
        previous = v16;
        ((ByteVector) d16.reinterpretShape(B, 0)).intoArray(out, op + 64);
        IntVector v32 = IntVector.fromArray(S, in, ip + 32);
        IntVector neighbor32 = PREVIOUS_INDEX.selectFrom(previous, v32);
        IntVector d32 = v32.sub(neighbor32);
        d32 = d32.sub(ONE);
        previous = v32;
        ((ByteVector) d32.reinterpretShape(B, 0)).intoArray(out, op + 128);
        IntVector v48 = IntVector.fromArray(S, in, ip + 48);
        IntVector neighbor48 = PREVIOUS_INDEX.selectFrom(previous, v48);
        IntVector d48 = v48.sub(neighbor48);
        d48 = d48.sub(ONE);
        previous = v48;
        ((ByteVector) d48.reinterpretShape(B, 0)).intoArray(out, op + 192);
        IntVector v64 = IntVector.fromArray(S, in, ip + 64);
        IntVector neighbor64 = PREVIOUS_INDEX.selectFrom(previous, v64);
        IntVector d64 = v64.sub(neighbor64);
        d64 = d64.sub(ONE);
        previous = v64;
        ((ByteVector) d64.reinterpretShape(B, 0)).intoArray(out, op + 256);
        IntVector v80 = IntVector.fromArray(S, in, ip + 80);
        IntVector neighbor80 = PREVIOUS_INDEX.selectFrom(previous, v80);
        IntVector d80 = v80.sub(neighbor80);
        d80 = d80.sub(ONE);
        previous = v80;
        ((ByteVector) d80.reinterpretShape(B, 0)).intoArray(out, op + 320);
        IntVector v96 = IntVector.fromArray(S, in, ip + 96);
        IntVector neighbor96 = PREVIOUS_INDEX.selectFrom(previous, v96);
        IntVector d96 = v96.sub(neighbor96);
        d96 = d96.sub(ONE);
        previous = v96;
        ((ByteVector) d96.reinterpretShape(B, 0)).intoArray(out, op + 384);
        IntVector v112 = IntVector.fromArray(S, in, ip + 112);
        IntVector neighbor112 = PREVIOUS_INDEX.selectFrom(previous, v112);
        IntVector d112 = v112.sub(neighbor112);
        d112 = d112.sub(ONE);
        previous = v112;
        ((ByteVector) d112.reinterpretShape(B, 0)).intoArray(out, op + 448);
    }

    private static void unpack0_32(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector v0 = ByteVector.fromArray(B, in, ip + 0).reinterpretAsInts();
        v0.intoArray(out, op + 0);
        IntVector v16 = ByteVector.fromArray(B, in, ip + 64).reinterpretAsInts();
        v16.intoArray(out, op + 16);
        IntVector v32 = ByteVector.fromArray(B, in, ip + 128).reinterpretAsInts();
        v32.intoArray(out, op + 32);
        IntVector v48 = ByteVector.fromArray(B, in, ip + 192).reinterpretAsInts();
        v48.intoArray(out, op + 48);
        IntVector v64 = ByteVector.fromArray(B, in, ip + 256).reinterpretAsInts();
        v64.intoArray(out, op + 64);
        IntVector v80 = ByteVector.fromArray(B, in, ip + 320).reinterpretAsInts();
        v80.intoArray(out, op + 80);
        IntVector v96 = ByteVector.fromArray(B, in, ip + 384).reinterpretAsInts();
        v96.intoArray(out, op + 96);
        IntVector v112 = ByteVector.fromArray(B, in, ip + 448).reinterpretAsInts();
        v112.intoArray(out, op + 112);
    }

    private static void unpack1_32(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector w4 = (IntVector) ByteVector.fromArray(B, in, ip + 256).reinterpretShape(S, 0);
        IntVector w5 = (IntVector) ByteVector.fromArray(B, in, ip + 320).reinterpretShape(S, 0);
        IntVector w6 = (IntVector) ByteVector.fromArray(B, in, ip + 384).reinterpretShape(S, 0);
        IntVector w7 = (IntVector) ByteVector.fromArray(B, in, ip + 448).reinterpretShape(S, 0);
        IntVector v0 = w0;
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w1;
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w2;
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w3;
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w4;
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w5;
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w6;
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w7;
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }

    private static void unpack2_32(byte[] in, int ip, int[] out, int op, int initial) {
        IntVector w0 = (IntVector) ByteVector.fromArray(B, in, ip + 0).reinterpretShape(S, 0);
        IntVector w1 = (IntVector) ByteVector.fromArray(B, in, ip + 64).reinterpretShape(S, 0);
        IntVector w2 = (IntVector) ByteVector.fromArray(B, in, ip + 128).reinterpretShape(S, 0);
        IntVector w3 = (IntVector) ByteVector.fromArray(B, in, ip + 192).reinterpretShape(S, 0);
        IntVector w4 = (IntVector) ByteVector.fromArray(B, in, ip + 256).reinterpretShape(S, 0);
        IntVector w5 = (IntVector) ByteVector.fromArray(B, in, ip + 320).reinterpretShape(S, 0);
        IntVector w6 = (IntVector) ByteVector.fromArray(B, in, ip + 384).reinterpretShape(S, 0);
        IntVector w7 = (IntVector) ByteVector.fromArray(B, in, ip + 448).reinterpretShape(S, 0);
        IntVector v0 = w0;
        v0 = v0.add(ONE);
        v0 = v0.add(v0.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v0 = v0.add(v0.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v0 = v0.add(v0.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v1 = w1;
        v1 = v1.add(ONE);
        v1 = v1.add(v1.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v1 = v1.add(v1.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v1 = v1.add(v1.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v2 = w2;
        v2 = v2.add(ONE);
        v2 = v2.add(v2.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v2 = v2.add(v2.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v2 = v2.add(v2.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v3 = w3;
        v3 = v3.add(ONE);
        v3 = v3.add(v3.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v3 = v3.add(v3.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v3 = v3.add(v3.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v4 = w4;
        v4 = v4.add(ONE);
        v4 = v4.add(v4.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v4 = v4.add(v4.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v4 = v4.add(v4.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v5 = w5;
        v5 = v5.add(ONE);
        v5 = v5.add(v5.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v5 = v5.add(v5.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v5 = v5.add(v5.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v6 = w6;
        v6 = v6.add(ONE);
        v6 = v6.add(v6.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v6 = v6.add(v6.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v6 = v6.add(v6.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector v7 = w7;
        v7 = v7.add(ONE);
        v7 = v7.add(v7.reinterpretAsLongs().lanewise(LSHL, 32).reinterpretAsInts());
        v7 = v7.add(v7.rearrange(PAIR_SHIFT1, PAIR_MASK1));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT2, PAIR_MASK2));
        v7 = v7.add(v7.rearrange(PAIR_SHIFT4, PAIR_MASK4));
        IntVector carry = IntVector.broadcast(S, initial);
        v0.add(carry).intoArray(out, op + 0);
        carry = carry.add(v0.rearrange(INT_LAST));
        v1.add(carry).intoArray(out, op + 16);
        carry = carry.add(v1.rearrange(INT_LAST));
        v2.add(carry).intoArray(out, op + 32);
        carry = carry.add(v2.rearrange(INT_LAST));
        v3.add(carry).intoArray(out, op + 48);
        carry = carry.add(v3.rearrange(INT_LAST));
        v4.add(carry).intoArray(out, op + 64);
        carry = carry.add(v4.rearrange(INT_LAST));
        v5.add(carry).intoArray(out, op + 80);
        carry = carry.add(v5.rearrange(INT_LAST));
        v6.add(carry).intoArray(out, op + 96);
        carry = carry.add(v6.rearrange(INT_LAST));
        v7.add(carry).intoArray(out, op + 112);
    }
}
