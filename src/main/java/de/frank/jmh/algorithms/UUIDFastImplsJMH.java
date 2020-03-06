package de.frank.jmh.algorithms;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/*--
 DISCLAIMER: This UUID version ist NOT always a drop in replacement for jdk UUID (or any other) see description javadoc @ customUUID class


 Oracle JDK 1.8.0_161
 Run complete. Total time: 00:13:52
 Benchmark               Mode  Cnt       Score     Error  Units # Comment
 JDK_RandomUUID          thrpt   30   1.387.488 ±  24.324  ops/s # uses SecureRandom internally
 JDK_newUUIDFrom2Longs   thrpt   30   2.682.555 ±  61.595  ops/s # own random but is still bad
 customUUID              thrpt   30  14.895.978 ± 350.588  ops/s # far less overhead and faster
 customUUID_from2Longs   thrpt   30  14.821.319 ± 256.184  ops/s # use-case specific variant
 customUUID_providedRNG  thrpt   30  14.964.418 ± 428.048  ops/s # use-case specific variant
 customUUID_reuseBuffer  thrpt   30  24.378.762 ± 719.431  ops/s # if possible (re-)use a buffer - then this version runs with ZERO GC-Allocations
 */

/**
 * @author Michael Frank
 * @version 1.0 05.02.2018
 */
@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(3)
@State(Scope.Thread)
public class UUIDFastImplsJMH {


    private ThreadLocalRandom r;
    private char[] buffer = new char[FastUUID.UUID_STRING_LEN];

    @Setup
    public void setup() {
        r = ThreadLocalRandom.current();
    }

    @Benchmark
    public String JDK_RandomUUID() {
        return java.util.UUID.randomUUID().toString();
    }

    @Benchmark
    public String JDK_newUUIDFrom2Longs() {
        return new java.util.UUID(r.nextLong(), r.nextLong()).toString();
    }

    @Benchmark
    public String customUUID() {
        return FastUUID.randomUUID();
    }

    @Benchmark
    public String customUUID_providedRNG() {
        return FastUUID.randomUUID(r);
    }

    @Benchmark
    public String customUUID_from2Longs() {
        return FastUUID.toUUID(r.nextLong(), r.nextLong());
    }

    @Benchmark
    public void customUUID_reuseBuffer(Blackhole bh) {
        FastUUID.randomUUID(r, buffer);
        bh.consume(buffer);
    }


    public static void main(String[] args) throws Throwable {
        Options opt = new OptionsBuilder()
                .include(UUIDFastImplsJMH.class.getName())
                .result(String.format("%s_%s.json",
                        DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                        UUIDFastImplsJMH.class.getSimpleName()))
                .build();
        new Runner(opt).run();
    }


    /**
     * Disclaimer:
     * This Implementation is for generating Random (Version 4) UUID's very fast.
     * Its main purpose is for test data generation.
     * This version does not guarantee uniqueness or good randomness.
     * <p>
     * Version 4 UUIDs are defined by RFC 4122 ("Leach-Salz"). <br>
     * These UUIDs depend primarily on random numbers. This algorithm sets the
     * version number (4 bits) as well as two reserved bits. All other bits (the
     * remaining 122 bits) are set using a random or pseudorandom data source.
     * Version 4 UUIDs have the form xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx where
     * x is any hexadecimal digit and y is one of 8, 9, a, or b (e.g.,
     * f47ac10b-58cc-4372-a567-0e02b2c3d479). <br>
     * <br>
     * What is different from the JDK java.util.UUID version? => performance!
     *
     * <pre>
     * Benchmark               Mode  Cnt         Score        Error  Units
     * JDK_RandomUUID          thrpt   30   1387488,927 ±  24324,277  ops/s #uses SecureRandom
     * JDK_newUUIDFrom2Longs   thrpt   30   2682555,545 ±  61595,885  ops/s #still bad
     * customUUID             thrpt   30  14895978,462 ± 350588,907  ops/s
     * customUUID_from2Longs   thrpt   30  14821319,244 ± 256184,576  ops/s
     * customUUID_providedRNG  thrpt   30  14964418,485 ± 428048,918  ops/s
     * customUUID_reuseBuffer  thrpt   30  24378762,601 ± 719431,119  ops/s #if possible (re-)use a buffer - then this version runs with ZERO-Allocation
     * </pre>
     *
     * @author Michael Frank
     * @version 1.0 22.11.2016
     */
    public static class FastUUID {
        public static final int UUID_STRING_LEN = 36;
        private final static char[] digitsONE = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd',
                'e', 'f',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', '0', '1', '2', '3', '4',
                '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'a', 'b', 'c', 'd', 'e', 'f', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e',
                'f', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', '0', '1', '2', '3',
                '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', '0', '1', '2', '3', '4', '5', '6', '7', '8',
                '9', 'a', 'b', 'c', 'd', 'e', 'f', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd',
                'e', 'f', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', '0', '1', '2',
                '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', '0', '1', '2', '3', '4', '5', '6', '7',
                '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c',
                'd', 'e', 'f', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', '0', '1',
                '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', '0', '1', '2', '3', '4', '5', '6',
                '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

        private final static char[] digitsTEN = {'0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
                '0', '0',
                '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '2', '2', '2', '2', '2',
                '2', '2', '2', '2', '2', '2', '2', '2', '2', '2', '2', '3', '3', '3', '3', '3', '3', '3', '3', '3', '3',
                '3', '3', '3', '3', '3', '3', '4', '4', '4', '4', '4', '4', '4', '4', '4', '4', '4', '4', '4', '4', '4',
                '4', '5', '5', '5', '5', '5', '5', '5', '5', '5', '5', '5', '5', '5', '5', '5', '5', '6', '6', '6', '6',
                '6', '6', '6', '6', '6', '6', '6', '6', '6', '6', '6', '6', '7', '7', '7', '7', '7', '7', '7', '7', '7',
                '7', '7', '7', '7', '7', '7', '7', '8', '8', '8', '8', '8', '8', '8', '8', '8', '8', '8', '8', '8', '8',
                '8', '8', '9', '9', '9', '9', '9', '9', '9', '9', '9', '9', '9', '9', '9', '9', '9', '9', 'a', 'a', 'a',
                'a', 'a', 'a', 'a', 'a', 'a', 'a', 'a', 'a', 'a', 'a', 'a', 'a', 'b', 'b', 'b', 'b', 'b', 'b', 'b', 'b',
                'b', 'b', 'b', 'b', 'b', 'b', 'b', 'b', 'c', 'c', 'c', 'c', 'c', 'c', 'c', 'c', 'c', 'c', 'c', 'c', 'c',
                'c', 'c', 'c', 'd', 'd', 'd', 'd', 'd', 'd', 'd', 'd', 'd', 'd', 'd', 'd', 'd', 'd', 'd', 'd', 'e', 'e',
                'e', 'e', 'e', 'e', 'e', 'e', 'e', 'e', 'e', 'e', 'e', 'e', 'e', 'e', 'f', 'f', 'f', 'f', 'f', 'f', 'f',
                'f', 'f', 'f', 'f', 'f', 'f', 'f', 'f', 'f'};


        /**
         * Generates a generic UUID.<br>
         *
         * @return
         */
        public static String randomUUID() {
            return randomUUID(ThreadLocalRandom.current());
        }

        /**
         * Generates a generic UUID.<br>
         *
         * @param r
         * @return
         */
        public static String randomUUID(Random r) {
            char[] buf = new char[UUID_STRING_LEN];
            randomUUID(r, buf);
            return new String(buf, 0, UUID_STRING_LEN);
        }


        /**
         * Generates a generic UUID.<br>
         * The UUID is stored in the provided buffer. <br>
         * Buffer must have a size of at least 36!
         *
         * @param r
         * @param buf with size >= 36
         */
        public static void randomUUID(Random r, char[] buf) {
            long msl = r.nextLong();
            long lsl = r.nextLong();
            toUUID(msl, lsl, buf, 0);
        }

        /**
         * Generates a generic UUID from two provided longs.<br>
         * The UUID is stored in the provided buffer. <br>
         * Buffer must have a size of at least 36!
         *
         * @param mostSigBits
         * @param leastSigBits
         */
        public static String toUUID(long mostSigBits, long leastSigBits) {
            char[] buf = new char[UUID_STRING_LEN];
            toUUID(mostSigBits, leastSigBits, buf, 0);
            return new String(buf, 0, UUID_STRING_LEN);
        }

        public static String randomUUIDBuf(Random r) {
            StringBuilder buf = new StringBuilder(UUID_STRING_LEN);
            randomUUID(r, buf);
            return buf.toString();
        }

        public static void randomUUIDBuf(Random r, StringBuilder buf) {
            randomUUID(r, buf);
        }

        public static void randomUUID(Random r, StringBuilder buf) {
            long msl = r.nextLong();
            long lsl = r.nextLong();
            toUUID(msl, lsl, buf, 0);
        }

        private static void toUUID(long msl, long lsl, StringBuilder chars, int offset) {

            msl &= ~0xF000L;/* clear version */
            msl |= 0x4000;/* set to version 4 */
            lsl &= 0x3fFFFFFFFFFFFFFFL; /* clear variant */
            lsl |= 0x8000000000000000L; /* set to IETF variant */

            // fill array backwards <-
            int charPos = offset + UUID_STRING_LEN;
            charPos = digits(lsl, 12, chars, charPos);
            chars.setCharAt(--charPos, '-');
            charPos = digits(lsl >> 48, 4, chars, charPos);
            chars.setCharAt(--charPos, '-');
            charPos = digits(msl, 4, chars, charPos);
            chars.setCharAt(-charPos, '-');
            charPos = digits(msl >> 16, 4, chars, charPos);
            chars.setCharAt(--charPos, '-');
            digits(msl >> 32, 8, chars, charPos);
        }

        private static int digits(long val, int digits, StringBuilder chars, int charPos) {
            long hi = 1L << (digits * 4);
            return toHex(hi | (val & (hi - 1)), digits, chars, charPos);
        }

        private static int toHex(long value, int numDigits, StringBuilder buf, int charPos) {

            do {
                int aByte = (int) (value & 0xFF);
                buf.setCharAt(--charPos, digitsONE[aByte]);
                buf.setCharAt(--charPos, digitsTEN[aByte]);
                value >>>= 8; // next 8 bits
                numDigits -= 2;
            } while (value != 0 && numDigits > 0);
            return charPos;
        }
        /**
         * Generates a generic UUID from two provided longs.<br>
         * The UUID is stored in the provided buffer. <br>
         * Buffer must have a size of at least 36!
         *
         * @param msl
         * @param lsl
         * @param chars
         */
        private static void toUUID(long msl, long lsl, char[] chars, int offset) {
            // The most significant long consists of the following unsigned
            // fields:<br/>
            // <pre><br/>
            // 0xFFFFFFFF00000000 time_low
            // 0x00000000FFFF0000 time_mid
            // 0x000000000000F000 version
            // 0x0000000000000FFF time_hi
            // </pre><br/>
            // The least significant long consists of the following unsigned
            // fields:<br/>
            // <pre><br/>
            // 0xC000000000000000 variant
            // 0x3FFF000000000000 clock_seq
            // 0x0000FFFFFFFFFFFF node


            msl &= ~0xF000L;/* clear version */
            msl |= 0x4000;/* set to version 4 */
            lsl &= 0x3fFFFFFFFFFFFFFFL; /* clear variant */
            lsl |= 0x8000000000000000L; /* set to IETF variant */

            // fill array backwards <-
            int charPos = offset + UUID_STRING_LEN;
            charPos = digits(lsl, 12, chars, charPos);
            chars[--charPos] = '-';
            charPos = digits(lsl >> 48, 4, chars, charPos);
            chars[--charPos] = '-';
            charPos = digits(msl, 4, chars, charPos);
            chars[--charPos] = '-';
            charPos = digits(msl >> 16, 4, chars, charPos);
            chars[--charPos] = '-';
            digits(msl >> 32, 8, chars, charPos);
        }

        /**
         * Returns val represented by the specified number of hex digits.
         */
        private static int digits(long val, int digits, char[] chars, int charPos) {
            long hi = 1L << (digits * 4);
            return toHex(hi | (val & (hi - 1)), digits, chars, charPos);
        }

        /**
         * !!! numDigits MUST BE an even number (unchecked)! as this algorithms
         * is
         * optimized to process two digits at a time.<br>
         * Stores 'numDigits' of provided number 'value' as hex in the provided
         * 'buffer' starting at position 'charPos'
         *
         * @param value     source of hex digits (number to format as hex)
         * @param numDigits if value as hex: '123456789ABCD' and numDigits 6, result:
         *                  '89ABCD'
         * @param buf       where to store result
         * @param charPos   position in buffer to start (buffer is traversed in
         *                  reverse
         *                  order with: while(numDigits-- > 0){ buf[--charPos] =
         *                  //hex; }
         * @return
         */
        private static int toHex(long value, int numDigits, char[] buf, int charPos) {
            // convert to hex starting at the least significant digit ("fill
            // from the back")
            do {
                int aByte = (int) (value & 0xFF);
                buf[--charPos] = digitsONE[aByte];
                buf[--charPos] = digitsTEN[aByte];
                value >>>= 8; // next 8 bits
                numDigits -= 2;
            } while (value != 0 && numDigits > 0);
            return charPos;
        }

    }

}