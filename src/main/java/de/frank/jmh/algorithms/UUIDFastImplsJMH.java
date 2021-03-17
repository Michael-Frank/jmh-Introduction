package de.frank.jmh.algorithms;

import com.fasterxml.uuid.*;
import com.fasterxml.uuid.impl.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.*;
import org.openjdk.jmh.runner.*;
import org.openjdk.jmh.runner.options.*;

import java.security.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.concurrent.*;

/*--
 DISCLAIMER: This UUID version ist NOT always a drop in replacement for jdk UUID (or any other) see description javadoc @ customUUID class


16 Threads contented - higher is better
JDK 15.0.1, OpenJDK 64-Bit Server VM, 15.0.1+8


Benchmark                                                        Mode  Cnt       Score        Error  Units
#insecure:
JDK_insecureRandom                                              thrpt   30  175.848.726 ±  193.7981  ops/s #jdk >=10 has javalangaccess.uuidToString which is ALOT faster
customFastUUID_external_ThreadLocal_insecureRandom              thrpt   30   82.263.435 ±   84.5977  ops/s #jdk >=10 has javalangaccess.uuidToString which is ALOT faster
customFastUUID_external_ThreadLocal_insecureRandom_from2Longs   thrpt   30   75.624.005 ±  210.2183  ops/s #jdk >=10 has javalangaccess.uuidToString which is ALOT faster
customFastUUID_external_ThreadLocal_insecureRandom_reuseBuffer  thrpt   30  163.230.177 ± 1763.3310  ops/s #jdk >=10 has javalangaccess.uuidToString which is ALOT faster
customFastUUID_insecureRandom                                   thrpt   30   82.975.445 ±   80.3699  ops/s #jdk >=10 has javalangaccess.uuidToString which is ALOT faster

#secure:
JDK_default_SecureRandom                                        thrpt   30    1.517.948 ±     8.116  ops/s #baseline-suffers from lock-contention
JDK_simpleThreadLocalSecureRandom                               thrpt   30   13.759.435 ±   342.514  ops/s #simple, but does not set variant
JDK_correctThreadLocalSecureRandom                              thrpt   30   16.869.144 ±   712.113  ops/s #my own
fasterxml_base                                                  thrpt   30    1.582.350 ±    27.774  ops/s # suffers from lock-contention
fasterxml_shared_secureRandom                                   thrpt   30    1.935.938 ±    81.416  ops/s # suffers from lock-contention
fasterxml_threadlocal_secureRandom                              thrpt   30   16.912.282 ±   230.095  ops/s
fasterxml_threadlocal_secureRandom_wrapper                      thrpt   30   17.024.619 ±   219.728  ops/s
customFastUUID_external_ThreadLocal_SecureRandom                thrpt   30   12.494.613 ±   304.758  ops/s #jdk >=10 has javalangaccess.uuidToString which is ALOT faster



Benchmark                          Mode  Cnt        Score       Error  Units
JDK_RandomUUID                    thrpt   30    1.545.375 ±    18.720  ops/s - baseline
JDK_newUUIDFrom2Longs             thrpt   30  175.339.754 ± 1.668.722  ops/s - non cryptographic prng is very fast
customUUID                        thrpt   30   80.404.259 ±   381.588  ops/s
customUUID_from2Longs             thrpt   30   79.870.090 ±   689.152  ops/s
customUUID_providedRNG            thrpt   30   80.464.137 ±   422.563  ops/s
customUUID_reuseBuffer            thrpt   30  167.034.018 ±15.577.678  ops/s - non cryptographic prng is very fast -
customUUID_providedRNG_tl_CSPRNG  thrpt   30   12.639.388 ±   391.240  ops/s - threadLocal elevates contention
fasterxml_uuid_                   thrpt   30    1.564.053 ±    12.850  ops/s - same as jdk uuid
fasterxml_uuid_csprng             thrpt   30    1.936.401 ±    51.039  ops/s
fasterxml_uuid_rand               thrpt   30    1.580.995 ±    13.483  ops/s
fasterxml_uuid_tl_csprng          thrpt   30   17.098.631 ±   347.760  ops/s - threadLocal elevates contention
fasterxml_uuid_wrapped_tl_csprng  thrpt   30   16.695.283 ±   189.538  ops/s - threadLocal elevates contention

//Outdated:
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
@Threads(16)
@State(Scope.Thread)
public class UUIDFastImplsJMH {


    private ThreadLocalRandom threadLocalRandom;
    private char[] buffer = new char[FastUUID.UUID_STRING_LEN];

    static final ThreadLocal<SecureRandom> THREAD_LOCAL_SECURE_RANDS =
            ThreadLocal.withInitial(UUIDFastImplsJMH::sh1csprng);

    private static SecureRandom sh1csprng() {
        try {
            return SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);//SHA1PRNG is always available
        }
    }

    @Setup
    public void setup() {
        threadLocalRandom = ThreadLocalRandom.current();
    }

    @Benchmark
    public String JDK_default_SecureRandom() {
        return java.util.UUID.randomUUID().toString();
    }

    @Benchmark
    public String JDK_insecureRandom() {
        //this variant is not "valid" is it does not set the appropriate variant bit flags
        return new java.util.UUID(threadLocalRandom.nextLong(), threadLocalRandom.nextLong()).toString();
    }


    @Benchmark
    public String JDK_simpleThreadLocalSecureRandom() {
        SecureRandom secrand = THREAD_LOCAL_SECURE_RANDS.get();
        long msb = secrand.nextLong();
        long lsb = secrand.nextLong();
        return new java.util.UUID(msb, lsb).toString();
    }

    @Benchmark
    public String JDK_correctThreadLocalSecureRandom() {
        return ThreadLocalUUID.randomUUID().toString();
    }


    @Benchmark
    public String customFastUUID_insecureRandom() {
        return FastUUID.randomUUID();
    }

    @Benchmark
    public String customFastUUID_external_ThreadLocal_insecureRandom() {
        return FastUUID.randomUUID(threadLocalRandom);
    }

    @Benchmark
    public String customFastUUID_external_ThreadLocal_SecureRandom() {
        return FastUUID.randomUUID(THREAD_LOCAL_SECURE_RANDS.get());
    }

    @Benchmark
    public String customFastUUID_external_ThreadLocal_insecureRandom_from2Longs() {
        return FastUUID.toUUID(threadLocalRandom.nextLong(), threadLocalRandom.nextLong());
    }

    @Benchmark
    public void customFastUUID_external_ThreadLocal_insecureRandom_reuseBuffer(Blackhole bh) {
        FastUUID.randomUUID(threadLocalRandom, buffer);
        bh.consume(buffer);
    }


    @Benchmark
    public String fasterxml_base(Blackhole bh) {
        return Generators.randomBasedGenerator().generate().toString();
    }


    public static final RandomBasedGenerator UUID_GENERATOR = Generators.randomBasedGenerator(sh1csprng());

    @Benchmark
    public String fasterxml_shared_secureRandom() {
        return UUID_GENERATOR.generate().toString();
    }


    @Benchmark
    public String fasterxml_threadlocal_secureRandom() {
        return Generators.randomBasedGenerator(THREAD_LOCAL_SECURE_RANDS.get()).generate().toString();
    }

    public static final RandomBasedGenerator UUID_GENERATOR2 =
            Generators.randomBasedGenerator(new ThreadLocalSecureRandom("SHA1PRNG"));

    @Benchmark
    public String fasterxml_threadlocal_secureRandom_wrapper() {
        return UUID_GENERATOR2.generate().toString();
    }

    static class ThreadLocalSecureRandom extends SecureRandom {
        private final ThreadLocal<SecureRandom> threadLocalRandom;

        public ThreadLocalSecureRandom(String algo) {
            this.threadLocalRandom = ThreadLocal.withInitial(() -> {
                try {
                    return SecureRandom.getInstance(algo);
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);//SHA1PRNG is always available
                }
            });
        }

        @Override
        public void nextBytes(byte[] bytes) {
            threadLocalRandom.get().nextBytes(bytes);
        }

        @Override
        public void nextBytes(byte[] bytes, SecureRandomParameters params) {
            threadLocalRandom.get().nextBytes(bytes, params);
        }

        @Override
        public long nextLong() {
            return threadLocalRandom.get().nextLong();
        }

        @Override
        public int nextInt() {
            return threadLocalRandom.get().nextInt();
        }
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


    /**
     * Generates a random UUID, just like {@link UUID#randomUUID()}.
     * Uses a ThreadLocal SecureRandom and thus does not suffer from lock-contention on {@link
     * java.security.SecureRandomSpi#engineNextBytes(byte[])} unlike the implementation of the JDK's {@link
     * UUID#randomUUID()}
     *
     * @return a (secure) random based UUID
     */
    public static class ThreadLocalUUID {
        //"ThreadLocals should be cleaned" no, we want the cache to live as long as the entire thread is living.
        @SuppressWarnings("java:S5164")
        private static final ThreadLocal<SecureRandom> RNG = ThreadLocal.withInitial(() -> newSecureRandom("SHA1PRNG"));

        private static SecureRandom newSecureRandom(String algo) {
            try {
                return SecureRandom.getInstance(algo);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Static factory to retrieve a type 4 (pseudo randomly generated) UUID.
         * <p>
         * The {@code UUID} is generated using a cryptographically strong pseudo
         * random number generator.
         *
         * @return A randomly generated {@code UUID}
         */
        public static UUID randomUUID() {
            return randomUUID(RNG.get());
        }

        /**
         * Static factory to retrieve a type 4 (pseudo randomly generated) UUID.
         * <p>
         * The {@code UUID} is generated using the provided random number generator.
         *
         * @return A randomly generated {@code UUID}
         */
        @SuppressWarnings("java:S109")
        public static UUID randomUUID(Random r) {
            final byte[] data = new byte[16];
            r.nextBytes(data);
            data[6] &= 0x0f;  /* clear version        */
            data[6] |= 0x40;  /* set to version 4     */
            data[8] &= 0x3f;  /* clear variant        */
            data[8] |= 0x80;  /* set to IETF variant  */

            long msb = toLong(data, 0);
            long lsb = toLong(data, Long.BYTES);

            return new UUID(msb, lsb);
        }

        public static long toLong(byte[] data, int offset) {
            long l = 0;
            for (int i = offset; i < offset + Long.BYTES; i++) {
                l = (l << 8) | (data[i] & 0xff);
            }
            return l;
        }
    }

}
