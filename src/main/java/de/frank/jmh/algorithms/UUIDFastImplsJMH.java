package de.frank.jmh.algorithms;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.RandomBasedGenerator;
import com.yevdo.jwildcard.JWildcard;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.SecureRandomParameters;
import java.util.Collection;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/*--
 DISCLAIMER: Not all UUID implementations presented here are drop in replacement for jdk UUID (or any other) see description javadoc @ customUUID class
UUID's are not just random bits. Some octets are "overwritten" to encode the version and variant of the UUID ("how was it derived and what specification it follows)

 tl;Dr:
  - correctness + maximum speed + SecureRandom not required:  FastUUID_ThreadLocalRandom
  - correctness + minimal custom code + general purpose SecureRandom UUID's:  fasterxml_threadlocal_secureRandom
  - correctness not that important +  minimal code:  JDK_ThreadLocalSecureRandom_notCorrect or JDK_ThreadLocalRandom_notCorrect

 Do i need SecureRandom?
 -----------------------
 Is it a possible scenario an Attacker can do harm by guessing the next UUID?
 - YES: use SecureRandom. (e.g. SessionID's, Accesstokens ,...)
 - NO: use ThreadLocalRandom

 Can i use other Random Generators? Yes, but why? ThreadLocalRandom is a pretty darn good PRNG and has ThreadLocal built in.

16 Threads contented ~12 min runtime
JMH version: 1.37
VM version: JDK 24, OpenJDK 64-Bit Server VM, 24+36
Result: OPS/s - higher is better
Benchmark                                                  Mode  Cnt        Score       Error  Units
#Baseline - correct
JDK_default_SecureRandom                                  thrpt   30      651.986 ±     62.373  ops/s # baseline-suffers heavily from lock-contention in shared SecureRandom instance

# Simple variants - but not correct, as UUID(long,long) constructor does not set the specification's "Schema" and "Version" bits
JDK_ThreadLocalSecureRandom_notCorrect                    thrpt   30   15.017.272 ±    948.434  ops/s # a SecureRandom instance per thread solves the lock-contention, but SecureRandom is still comparatively slow
JDK_ThreadLocalRandom_notCorrect                          thrpt   30   85.020.152 ±  6.392.238  ops/s # not correct but very fast as jdk >=10 has javalangaccess.uuidToString which is ALOT faster
JDK_CachedThreadLocalRandom_notCorrect                    thrpt   30   88.288.940 ±  3.626.376  ops/s # no point in using - just to show it is an stupid idea to cache the ThreadLocal instance

# Recommendation - correct "Schema" and "Version" bits and uses jdk UUID's very fast toString() for speed
FastUUID_SecureRandom                                     thrpt   30   22.057.529 ±   470.543  ops/s # Correct, Secure and fast enough. Can serve as drop-in replacement for JDK UUID
FastUUID_ThreadLocalRandom                                thrpt   30   92.296.121 ± 3.056.958  ops/s # Correct, Not for "secure" stuff, but very fast
FastUUID_externalThreadLocalRandom                        thrpt   30   95.565.940 ± 1.031.001  ops/s # Correct and same as FastUUID_ThreadLocalRandom - see "Error" column jitter

# Obsolete - customToString was faster before jdk 10 but jdk.UUID.toString() is a lot faster since jdk>=10
FastUUIDWithCustomToString_extThreadLocalSecureRandom     thrpt   30   14.945.681 ±    978.049 ops/s # obsolete, since jdk10 the UUID toString got heavily optimized and better than this hand rolled toString
FastUUIDWithCustomToString_ThreadLocalRandom              thrpt   30   39.865.143 ±  3.403.124 ops/s # obsolete, since jdk10 the UUID toString got heavily optimized and better than this hand rolled toString
FastUUIDWithCustomToString_ThreadLocalRandom_reuseBuffer  thrpt   30  162.274.187 ± 23.853.280 ops/s # SPECIAL: if you can reuse a buffer char[] this variant is ZERO-ALLOCATION (in some rare cases this is very nice)

# Alternative with library
fasterxml_base                                            thrpt   30      729.543 ±     49.150  ops/s # same issue as jdk UUID
fasterxml_shared_secureRandom                             thrpt   30    2.429.346 ±    115.506  ops/s # can serve as drop in replacement for JDK UUID.
fasterxml_threadlocal_secureRandom                        thrpt   30   19.880.718 ±  1.276.301  ops/s # recommendation if you need as little as possible custom code and general purpose secure UUID's
fasterxml_threadlocal_secureRandom_wrapper                thrpt   30   18.854.731 ±  1.469.807  ops/s # not worth the extra code compared to fasterxml_threadlocal_secureRandom
 */

/**
 * @author Michael Frank
 * @version 2.0 29.04.2025
 */
@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(3)
@Threads(16)
@State(Scope.Thread)
public class UUIDFastImplsJMH {

    public static void main(String[] args) throws Throwable {
        Options opt = new OptionsBuilder()
                .include(JWildcard.wildcardToRegex(UUIDFastImplsJMH.class.getName() + ".FastUUID_*"))
                //.resultFormat(ResultFormatType.JSON)
                //.result(String.format("%s_%s.json",
                //        DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                //        UUIDFastImplsJMH.class.getSimpleName()))
                //.addProfiler(GCProfiler.class)
                .build();

        Runner r = new Runner(opt);
        r.list();
        Collection<RunResult> results = r.run();
    }


    static final ThreadLocal<SecureRandom> THREAD_LOCAL_SECURE_RANDS =
            ThreadLocal.withInitial(UUIDFastImplsJMH::sha1csprng);

    private static SecureRandom sha1csprng() {
        try {
            return SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);//SHA1PRNG is always available
        }
    }

    private ThreadLocalRandom threadLocalRandom; //caching that is a very stupid idea, and its only here to show you

    @State(Scope.Thread)
    public static class MyContext {
        public char[] buffer = new char[FastUUIDWithCustomToString.UUID_STRING_LEN];
    }

    @Setup
    public void setup() {
        threadLocalRandom = ThreadLocalRandom.current(); //caching that is a very stupid idea, and its only here to show you
    }

    @Benchmark
    public String JDK_default_SecureRandom() {
        return java.util.UUID.randomUUID().toString();
    }

    @Benchmark
    public String JDK_ThreadLocalRandom_notCorrect() {
        //this variant is not "correct/valid" is it does not set the appropriate UUID spec variant and version bit flags
        var r = ThreadLocalRandom.current();
        return new java.util.UUID(r.nextLong(), r.nextLong()).toString();
    }

    @Benchmark
    public String JDK_CachedThreadLocalRandom_notCorrect() {
        //this variant is not "correct/valid" is it does not set the appropriate UUID spec variant and version bit flags

        //caching threadLocalRandom is a very stupid idea, and its only here to show you
        return new java.util.UUID(threadLocalRandom.nextLong(), threadLocalRandom.nextLong()).toString();
    }


    @Benchmark
    public String JDK_ThreadLocalSecureRandom_notCorrect() {
        //this variant is not "correct/valid" is it does not set the appropriate UUID spec variant and version bit flags
        SecureRandom secrand = THREAD_LOCAL_SECURE_RANDS.get();
        long msb = secrand.nextLong();
        long lsb = secrand.nextLong();
        return new java.util.UUID(msb, lsb).toString();
    }


    @Benchmark
    public String FastUUID_SecureRandom() {
        //sets correct "version 4" and variant flags
        return FastUUID.secureRandomUUID().toString();
    }

    @Benchmark
    public String FastUUID_ThreadLocalRandom() {
        //sets correct "version 4" and variant flags
        return FastUUID.randomUUID().toString();
    }

    @Benchmark
    public String FastUUID_externalThreadLocalRandom() {
        //sets correct "version 4" and variant flags
        return FastUUID.randomUUID(ThreadLocalRandom.current()).toString();
    }


    @Benchmark
    public String FastUUIDWithCustomToString_ThreadLocalRandom() {
        //sets correct "version 4" and variant flags
        return FastUUIDWithCustomToString.randomUUID();
    }

    @Benchmark
    public String FastUUIDWithCustomToString_extThreadLocalSecureRandom() {
        //sets correct "version 4" and variant flags
        return FastUUIDWithCustomToString.secureRandomUUID();
    }

    @Benchmark
    public String FastUUIDWithCustomToString_externalThreadLocalRandoms() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        return FastUUIDWithCustomToString.toUUID(r.nextLong(), r.nextLong());
    }

    @Benchmark
    public void FastUUIDWithCustomToString_ThreadLocalRandom_reuseBuffer(Blackhole bh, MyContext c) {
        FastUUIDWithCustomToString.randomUUID(ThreadLocalRandom.current(), c.buffer);
        bh.consume(c.buffer);
    }


    @Benchmark
    public String fasterxml_base() {
        return Generators.randomBasedGenerator().generate().toString();
    }


    public static final RandomBasedGenerator UUID_GENERATOR = Generators.randomBasedGenerator(sha1csprng());

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
        //The code overhead is not worth this variant - use fasterxml_threadlocal_secureRandom instead
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


    /**
     * Generates a random UUID, just like {@link UUID#randomUUID()}.
     * Uses a ThreadLocal SecureRandom and thus does not suffer from lock-contention on {@link
     * java.security.SecureRandomSpi#engineNextBytes(byte[])} unlike the implementation of the JDK's {@link
     * UUID#randomUUID()}
     *
     * @author Michael Frank
     */
    public static class FastUUID {
        //"ThreadLocals should be cleaned" no, we want the cache to live as long as the entire thread is living.
        @SuppressWarnings("java:S5164")
        private static final ThreadLocal<SecureRandom> SECURE_RANDOM = ThreadLocal.withInitial(() -> newSecureRandom("SHA1PRNG"));

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
        public static UUID secureRandomUUID() {
            return randomUUIDFromBytes(SECURE_RANDOM.get());
        }

        /**
         * Static factory to retrieve a type 4 (pseudo randomly generated) UUID.
         * <p>
         * The {@code UUID} is generated using a normal Random number generator. (ThreadLocalRandom)
         *
         * @return A randomly generated {@code UUID}
         */
        public static UUID randomUUID() {
            return randomUUIDFromLongs(ThreadLocalRandom.current());
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
            if (r instanceof SecureRandom) {
                return randomUUIDFromBytes(r);
            }
            return randomUUIDFromLongs(r);
        }

        private static UUID randomUUIDFromBytes(Random r) {

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

        private static UUID randomUUIDFromLongs(Random r) {
            var msl = r.nextLong();
            var lsl = r.nextLong();
            msl &= ~0xF000L;/* clear version */
            msl |= 0x4000;/* set to version 4 */
            lsl &= 0x3fFFFFFFFFFFFFFFL; /* clear variant */
            lsl |= 0x8000000000000000L; /* set to IETF variant */
            return new UUID(msl, lsl);
        }

        public static long toLong(byte[] data, int offset) {
            long l = 0;
            for (int i = offset; i < offset + Long.BYTES; i++) {
                l = (l << 8) | (data[i] & 0xff);
            }
            return l;
        }
    }


    /**
     * ######################################################################################################
     * Disclaimer:   !!!!!!  This version does not guarantee uniqueness or strong randomness !!!!
     * ######################################################################################################
     * Justification for this class:
     * - non-security sensitive very fast generation of Version4 Random UUID's for test data generation
     * - at a time (jdk8,9) where optimized UUID.toString() was not yet implemented in the JDK ( since jdk10)
     * - NOT FOR SECURITY related stuff like: SESSION ID's or guaranteed uniqueness.
     * <p>
     * Please use {@linkplain FastUUID} instead
     *
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
     * <pre>
     * Benchmark                      Mode  Cnt         Score     Error  Units
     * JDK_RandomUUID                  thrpt   30   1.387.488  ±  24324  ops/s # uses SecureRandom and suffers from lock contention on load
     * JDK_newUUID(long, long)_jdk<10  thrpt   30   2.682.555  ±  61595  ops/s # still bad as toString implemented badly in JDK versions before 10
     * JDK_newUUID(long, long)_jdk>=10 thrpt   30  12.584.350  ± 258916  ops/s # very good, but not correct (version&variant bits not set)
     * thisClass_toUUID(l,l)           thrpt   30  14.821.319  ± 256184  ops/s
     * thisClass.randomUUID()          thrpt   30  14.895.978  ± 350588  ops/s
     * thisClass_randomUUID(rng)       thrpt   30  14.964.418  ± 428048  ops/s
     * thisClass_randomUUID(rng,buf)   thrpt   30  24.378.762  ± 719431  ops/s #if possible (re-)use a buffer - then this version runs with ZERO-Allocation
     * </pre>
     *
     * @author Michael Frank
     * @version 1.0 22.11.2016
     */
    public static class FastUUIDWithCustomToString {
        public static final int UUID_STRING_LEN = 36;
        private static final char[] digitsONE = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd',
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

        private static final char[] digitsTEN = {'0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
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

        private static final ThreadLocal<SecureRandom> RNG = ThreadLocal.withInitial(() -> newSecureRandom("SHA1PRNG"));

        private static SecureRandom newSecureRandom(String algo) {
            try {
                return SecureRandom.getInstance(algo);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Generates a generic UUID.<br>
         *
         * @return a uuid
         */
        public static String randomUUID() {
            return randomUUID(ThreadLocalRandom.current());
        }

        /**
         * Generates a generic UUID.<br>
         *
         * @return a uuid
         */
        public static String secureRandomUUID() {
            return randomUUID(RNG.get());
        }


        /**
         * Generates a generic UUID.<br>
         *
         * @param r provided UUID
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
