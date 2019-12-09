package de.frank.jmh.algorithms;

import org.jetbrains.annotations.NotNull;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


/*--
Conclusion:
- if you are forced to stay e.g. at JDK 8, consider fastUUID
- JDK >=12 has many very nice String concatenation optimizations, rendering fastuuui obsolete
- ... but java.util.UUID still has a single shared SecureRandom instance, suffering form heavy lock contention in SecureRandomSpi#engineNextBytes, which kills the performance!
-  Make sure to use a ThreadLocal<SecureRandom> SECURE_RAND to generate UUID's!
- As we cannot change libraries to use ThreadLocal's, it is still god practice to start the jvm with: -Djava.security.egd=file:/dev/./urandom" to speed up the creation of new SecureRandom instances

Openjdk 13
Benchmark                                                  Mode  Cnt  Score   Error  Units  gc.alloc.rate.norm
jdkUUID_default                                            avgt   10   1346 ±   167  ns/op   176 ±  0,042 B/op #sucks - should not be THAT bad in single thread mode.. investigate?
jdkUUID_egd_random                                         avgt   10   1341 ±    80  ns/op   176 ±  0,039 B/op #sucks - should not be THAT bad in single thread mode.. investigate?
jdkUUID_egd_urandom                                        avgt   10   1373 ±   282  ns/op   176 ±  0,042 B/op #sucks - should not be THAT bad in single thread mode.. investigate?
jdkUUID_threadLocalSecRand                                 avgt   10    312 ±    10  ns/op   208 ±  0,001 B/op #2nd
jdkUUID_threadLocalSecRand_egd_urand                       avgt   10    311 ±    16  ns/op   208 ±  0,001 B/op #winner - JDK 13 string concatenation optimizations in java.util.UUID)
fastUuid_threadLocalSecRand                                avgt   10    340 ±    18  ns/op   296 ±  0,001 B/op #fast uuid is becoming obsolete with new JDK's
fastUuid_threadLocalSecRand_egd_urand                      avgt   10    333 ±    14  ns/op   296 ±  0,001 B/op

jdkUUID_default_contended                                  avgt   10  26981 ±   769  ns/op   176 ±  0,048 B/op #heavy lock contention
jdkUUID_egd_random_contended                               avgt   10  28752 ±  1165  ns/op   176 ±  0,054 B/op #heavy lock contention
jdkUUID_egd_urandom_contended                              avgt   10  27067 ±   965  ns/op   208 ±  0,050 B/op #heavy lock contention
jdkUUID_threadLocalSecRand_contended                       avgt   10   1070 ±    23  ns/op   208 ±  0,001 B/op #2nd - ThreadLocal SecureRandom eliminates the lock contention
jdkUUID_threadLocalSecRand_egd_urand_contended             avgt   10    957 ±    13  ns/op   208 ±  0,001 B/op #winner (JDK 13 string concatenation optimizations in java.util.UUID)
fastUuid_threadLocalSecRand_contended                      avgt   10   1137 ±    40  ns/op   296 ±  0,001 B/op #fast uuid is becoming obsolete with new JDK's
fastUuid_threadLocalSecRand_egd_urand_contended            avgt   10   1112 ±    10  ns/op   296 ±  0,001 B/op


# VM version: JDK 1.8.0_232, OpenJDK 64-Bit Server VM, 25.232-b09
Benchmark                                                 Mode  Cnt   Score     Error Units
jdkUUID_default                                           avgt   10    1473 ±     47  ns/op    864 ±   0 B/op #sucks - should not be THAT bad in single thread mode.. investigate?
jdkUUID_egd_random                                        avgt   10    1472 ±     55  ns/op    864 ±   0 B/op #sucks - should not be THAT bad in single thread mode.. investigate?
jdkUUID_egd_urandom                                       avgt   10     501 ±     27  ns/op    832 ±   0 B/op #ok-ish if used rarely, but suffers from lock contention
jdkUUID_threadLocalSecRand                                avgt   10     568 ±     25  ns/op    864 ±   0 B/op
jdkUUID_threadLocalSecRand_egd_urand                      avgt   10     573 ±     25  ns/op    864 ±   0 B/op
fastUuid_threadLocalSecRand                               avgt   10     340 ±     24  ns/op    328 ±   0 B/op #winner
fastUuid_threadLocalSecRand_egd_urand                     avgt   10     342 ±      9  ns/op    328 ±   0 B/op #winner

jdkUUID_default_contended                                 avgt   10   28595 ±    840  ns/op    864 ±   0 B/op #heavy lock contention
jdkUUID_egd_random_contended                              avgt   10   29501 ±    865  ns/op    864 ±   0 B/op #heavy lock contention
jdkUUID_egd_urandom_contended                             avgt   10    9899 ±    356  ns/op    832 ±   0 B/op #heavy lock contention
jdkUUID_threadLocalSecRand_contended                      avgt   10    1866 ±    243  ns/op    864 ±   0 B/op
jdkUUID_threadLocalSecRand_egd_urand_contended            avgt   10    1726 ±    155  ns/op    864 ±   0 B/op #2n place
fastUuid_threadLocalSecRand_contended                     avgt   10    1257 ±    155  ns/op    328 ±   0 B/op
fastUuid_threadLocalSecRand_egd_urand_contended           avgt   10    1184 ±     98  ns/op    328 ±   0 B/op #winner

jdkUUID_default                                             ss   10  100144 ±  61242  ns/op   1598 ±  26 B/op
jdkUUID_egd_random                                          ss   10   96281 ±  25614  ns/op   1595 ±  31 B/op
jdkUUID_egd_urandom                                         ss   10   44735 ±  37694  ns/op   1563 ±  31 B/op
jdkUUID_threadLocalSecRand                                  ss   10   49981 ±  29470  ns/op   1627 ±  31 B/op
jdkUUID_threadLocalSecRand_egd_urand                        ss   10   46567 ±  27075  ns/op   1627 ±  31 B/op
fastUuid_threadLocalSecRand_egd_urand                       ss   10   36592 ±  18037  ns/op    795 ±  31 B/op #winner
fastUuid_threadLocalSecRand                                 ss   10   40278 ±  22625  ns/op    795 ±  31 B/op

jdkUUID_egd_random_contended                                ss   10   91891 ±  41399  ns/op   1638 ±   8 B/op
jdkUUID_egd_urandom_contended                               ss   10   37107 ±  30350  ns/op   1604 ±   5 B/op
jdkUUID_threadLocalSecRand_contended                        ss   10   23838 ±  14686  ns/op   1669 ±  27 B/op
jdkUUID_threadLocalSecRand_egd_urand_contended              ss   10   25789 ±  17010  ns/op   1667 ±  30 B/op
fastUuid_threadLocalSecRand_contended                       ss   10   20754 ±  14081  ns/op    835 ±  28 B/op +winner
fastUuid_threadLocalSecRand_egd_urand_contended             ss   10   21358 ±  13548  ns/op    835 ±  27 B/op


 */
@BenchmarkMode({Mode.AverageTime, Mode.SingleShotTime})
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class SecureUUID {

    private static final ThreadLocal<SecureRandom> SECURE_RAND = ThreadLocal.withInitial(() -> {
        try {
            return SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    });

    public static void main(String[] args) throws RunnerException {

        Options opt = new OptionsBuilder()
                .include(SecureUUID.class.getName())
                .resultFormat(ResultFormatType.JSON)
                .result(String.format("%s_%s.json",
                        DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                        SecureUUID.class.getSimpleName()))
                .addProfiler(GCProfiler.class)
                .build();
        new Runner(opt).run();
    }


    @Benchmark
    @Fork(value = 1)
    public String jdkUUID() {
        return UUID.randomUUID().toString();
    }

    @Benchmark
    @Fork(value = 1)
    @Threads(16)
    public String uuid_jdk_contended() {
        return UUID.randomUUID().toString();
    }

    @Benchmark
    @Fork(value = 1, jvmArgsAppend = "-Djava.security.egd=file:/dev/./urandom")
    public String jdkUUID_egd_urandom() {
        return UUID.randomUUID().toString();
    }

    @Benchmark
    @Fork(value = 1, jvmArgsAppend = "-Djava.security.egd=file:/dev/./urandom")
    @Threads(16)
    public String jdkUUID_egd_urandom_contended() {
        return UUID.randomUUID().toString();
    }


    @Benchmark
    @Fork(value = 1, jvmArgsAppend = "-Djava.security.egd=file:/dev/random")
    public String jdkUUID_egd_random() {
        return UUID.randomUUID().toString();
    }

    @Benchmark
    @Fork(value = 1, jvmArgsAppend = "-Djava.security.egd=file:/dev/random")
    @Threads(16)
    public String jdkUUID_egd_random_contended() {
        return UUID.randomUUID().toString();
    }


    @Benchmark
    @Fork(value = 1)
    public String jdkUUID_threadLocalSecRand() {
        return threadLocalSecRandUUID();
    }

    @Benchmark
    @Fork(value = 1)
    @Threads(16)
    public String jdkUUID_threadLocalSecRand_contended() {
        return threadLocalSecRandUUID();
    }


    @Benchmark
    @Fork(value = 1, jvmArgsAppend = "-Djava.security.egd=file:/dev/./urandom")
    public String jdkUUID_threadLocalSecRand_egd_urand() {
        return threadLocalSecRandUUID();
    }

    @Benchmark
    @Fork(value = 1, jvmArgsAppend = "-Djava.security.egd=file:/dev/./urandom")
    @Threads(16)
    public String jdkUUID_threadLocalSecRand_egd_urand_contended() {
        return threadLocalSecRandUUID();
    }

    @Benchmark
    @Fork(value = 1)
    public String fastUuid_threadLocalSecRand() {
        return FastUUIDImplJMH.UUID.randomUUID(SECURE_RAND.get());
    }

    @Benchmark
    @Fork(value = 1)
    @Threads(16)
    public String fastUuid_threadLocalSecRand_contended() {
        return FastUUIDImplJMH.UUID.randomUUID(SECURE_RAND.get());
    }


    @Benchmark
    @Fork(value = 1, jvmArgsAppend = "-Djava.security.egd=file:/dev/./urandom")
    public String fastUuid_threadLocalSecRand_egd_urand() {
        return FastUUIDImplJMH.UUID.randomUUID(SECURE_RAND.get());
    }

    @Benchmark
    @Fork(value = 1, jvmArgsAppend = "-Djava.security.egd=file:/dev/./urandom")
    @Threads(16)
    public String fastUuid_threadLocalSecRand_egd_urandcontended() {
        return FastUUIDImplJMH.UUID.randomUUID(SECURE_RAND.get());
    }


    @NotNull
    private String threadLocalSecRandUUID() {
        SecureRandom r = SECURE_RAND.get();
        long msl = r.nextLong();
        long lsl = r.nextLong();
        msl &= ~0xF000L;/* clear version */
        msl |= 0x4000;/* set to version 4 */
        lsl &= 0x3fFFFFFFFFFFFFFFL; /* clear variant */
        lsl |= 0x8000000000000000L; /* set to IETF variant */
        return new UUID(msl, lsl).toString();
    }

}
