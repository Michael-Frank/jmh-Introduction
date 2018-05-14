package de.frank.jmh.algorithms;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/*--
 DISCLAIMER: This UUID version ist NOT a drop in replacement for jdk UUID (or any other) see description javadoc @ customUUID class

 Oracle JDK 1.8.0_161
 Run complete. Total time: 00:13:52
 Benchmark               Mode  Cnt         Score        Error  Units
 JDKrandomUUID          thrpt   30   1387488,927 ±  24324,277  ops/s #uses SecureRandom
 JDKnewUUIDFrom2Longs   thrpt   30   2682555,545 ±  61595,885  ops/s #still bad
 customUUID             thrpt   30  14895978,462 ± 350588,907  ops/s
 customUUIDFrom2Longs   thrpt   30  14821319,244 ± 256184,576  ops/s
 customUUIDProvidedRNG  thrpt   30  14964418,485 ± 428048,918  ops/s
 customUUIDReuseBuffer  thrpt   30  24378762,601 ± 719431,119  ops/s #if possible (re-)use a buffer - then this version runs with ZERO-Allocation
 */
/**
 * @author Michael Frank
 * @version 1.0 05.02.2018
 */
@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(3)
@State(Scope.Thread)
public class FastUUIDImplJMH {



    private ThreadLocalRandom r;
    private char[] buffer = new char[UUID.UUID_STRING_LEN];

    @Setup
    public void setup() {
        r = ThreadLocalRandom.current();
    }

    @Benchmark
    public String JDKrandomUUID() {
        return java.util.UUID.randomUUID().toString();
    }

    @Benchmark
    public String JDKnewUUIDFrom2Longs() {
        return new java.util.UUID(r.nextLong(), r.nextLong()).toString();
    }

    @Benchmark
    public String customUUID() {
        return UUID.randomUUID();
    }

    @Benchmark
    public String customUUIDProvidedRNG() {
        return UUID.randomUUID(r);
    }

    @Benchmark
    public String customUUIDFrom2Longs() {
        return UUID.toUUID(r.nextLong(), r.nextLong());
    }

    @Benchmark
    public void customUUIDReuseBuffer(Blackhole bh) {
        UUID.randomUUID(r, buffer);
        bh.consume(buffer);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(FastUUIDImplJMH.class.getName())
                .build();
        new Runner(opt).run();
    }

    /**
     * Disclaimer:
     * This Implementation is for generating Random (Version 4) UUID's very fast.
     * Its main purpose is for test data generation.
     * This version does not guarantee uniqueness or good randomness.
     *
     * Version 4 UUIDs are defined by RFC 4122 ("Leach-Salz"). <br>
     * These UUIDs depend primarily on random numbers. This algorithm sets the
     * version number (4 bits) as well as two reserved bits. All other bits (the
     * remaining 122 bits) are set using a random or pseudorandom data source.
     * Version 4 UUIDs have the form xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx where
     * x is any hexadecimal digit and y is one of 8, 9, a, or b (e.g.,
     * f47ac10b-58cc-4372-a567-0e02b2c3d479). <br>
     * <br>
     * What is different from the JDK java.util.UUID version? => performance!
     * <p>
     * <pre>
     * Benchmark               Mode  Cnt         Score        Error  Units
     * JDKrandomUUID          thrpt   30   1387488,927 ±  24324,277  ops/s #uses SecureRandom
     * JDKnewUUIDFrom2Longs   thrpt   30   2682555,545 ±  61595,885  ops/s #still bad
     * customUUID             thrpt   30  14895978,462 ± 350588,907  ops/s
     * customUUIDFrom2Longs   thrpt   30  14821319,244 ± 256184,576  ops/s
     * customUUIDProvidedRNG  thrpt   30  14964418,485 ± 428048,918  ops/s
     * customUUIDReuseBuffer  thrpt   30  24378762,601 ± 719431,119  ops/s #if possible (re-)use a buffer - then this version runs with ZERO-Allocation
     * </pre>
     *
     * @author Michael Frank
     * @version 1.0 22.11.2016
     */
    public static class UUID {
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
            charPos = digits(msl >> 32, 8, chars, charPos);
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
