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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


/*--



Benchmark                                         Mode  Cnt      Score       Error  Units
SecureUUID.uuid_default                                          avgt   10   1445,617 ±   108,574  ns/op
SecureUUID.uuid_egd_random                                       avgt   10   1414,562 ±    23,163  ns/op
SecureUUID.uuid_egd_urandom                                      avgt   10    460,143 ±     4,907  ns/op !#2 winner drop in

SecureUUID.uuid_threadLocalSecRand                               avgt   10    542,071 ±    11,152  ns/op
SecureUUID.uuid_threadLocalSecRand_egd_urand                     avgt   10    532,944 ±     6,838  ns/op

SecureUUID.uuid_threadLocalSecRand_fastUUID                      avgt   10    342,636 ±     5,004  ns/op
SecureUUID.uuid_threadLocalSecRand_fastUUID_egd_urand            avgt   10    350,249 ±     4,879  ns/op


SecureUUID.uuid_default_contended                                avgt   10  27898,119 ±  1393,915  ns/op
SecureUUID.uuid_egd_random_contended                             avgt   10  28218,308 ±  1868,255  ns/op
SecureUUID.uuid_egd_urandom_contended                            avgt   10   9811,368 ±   478,106  ns/op

SecureUUID.uuid_threadLocalSecRand_contended                     avgt   10   1862,830 ±   121,306  ns/op
SecureUUID.uuid_threadLocalSecRand_egd_urand_contended           avgt   10   1860,359 ±    93,490  ns/op

SecureUUID.uuid_threadLocalSecRand_fastUUID_contended            avgt   10   1130,345 ±    62,964  ns/op
SecureUUID.uuid_threadLocalSecRand_fastUUID_egd_urand_contended  avgt   10   1224,151 ±    66,684  ns/op

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
                .include(SecureUUID.class.getSimpleName())
                .result(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss_").format(LocalDateTime.now()) + SecureUUID.class.getSimpleName())
                .resultFormat(ResultFormatType.JSON)
                .addProfiler(GCProfiler.class)
                .build();
        new Runner(opt).run();
    }


    @Benchmark
    @Fork(value = 1)
    public String uuid_default() {
        return UUID.randomUUID().toString();
    }

    @Benchmark
    @Fork(value = 1)
    @Threads(16)
    public String uuid_default_contended() {
        return UUID.randomUUID().toString();
    }

    @Benchmark
    @Fork(value = 1, jvmArgsAppend = "-Djava.security.egd=file:/dev/./urandom")
    public String uuid_egd_urandom() {
        return UUID.randomUUID().toString();
    }

    @Benchmark
    @Fork(value = 1, jvmArgsAppend = "-Djava.security.egd=file:/dev/./urandom")
    @Threads(16)
    public String uuid_egd_urandom_contended() {
        return UUID.randomUUID().toString();
    }


    @Benchmark
    @Fork(value = 1, jvmArgsAppend = "-Djava.security.egd=file:/dev/random")
    public String uuid_egd_random() {
        return UUID.randomUUID().toString();
    }

    @Benchmark
    @Fork(value = 1, jvmArgsAppend = "-Djava.security.egd=file:/dev/random")
    @Threads(16)
    public String uuid_egd_random_contended() {
        return UUID.randomUUID().toString();
    }


    @Benchmark
    @Fork(value = 1)
    public String uuid_threadLocalSecRand_fastUUID() {
        return FastUUIDImplJMH.UUID.randomUUID(SECURE_RAND.get());
    }

    @Benchmark
    @Fork(value = 1)
    @Threads(16)
    public String uuid_threadLocalSecRand_fastUUID_contended() {
        return FastUUIDImplJMH.UUID.randomUUID(SECURE_RAND.get());
    }


    @Benchmark
    @Fork(value = 1, jvmArgsAppend = "-Djava.security.egd=file:/dev/./urandom")
    public String uuid_threadLocalSecRand_fastUUID_egd_urand() {
        return FastUUIDImplJMH.UUID.randomUUID(SECURE_RAND.get());
    }

    @Benchmark
    @Fork(value = 1, jvmArgsAppend = "-Djava.security.egd=file:/dev/./urandom")
    @Threads(16)
    public String uuid_threadLocalSecRand_fastUUID_egd_urand_contended() {
        return FastUUIDImplJMH.UUID.randomUUID(SECURE_RAND.get());
    }


    @Benchmark
    @Fork(value = 1)
    public String uuid_threadLocalSecRand() {
        return threadLocalSecRandUUID();
    }

    @Benchmark
    @Fork(value = 1)
    @Threads(16)
    public String uuid_threadLocalSecRand_contended() {
        return threadLocalSecRandUUID();
    }


    @Benchmark
    @Fork(value = 1, jvmArgsAppend = "-Djava.security.egd=file:/dev/./urandom")
    public String uuid_threadLocalSecRand_egd_urand() {
        return threadLocalSecRandUUID();
    }

    @Benchmark
    @Fork(value = 1, jvmArgsAppend = "-Djava.security.egd=file:/dev/./urandom")
    @Threads(16)
    public String uuid_threadLocalSecRand_egd_urand_contended() {
        return threadLocalSecRandUUID();
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
