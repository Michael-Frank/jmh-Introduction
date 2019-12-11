package de.frank.jmh.algorithms;

import de.frank.jmh.algorithms.UUIDFastImplsJMH.FastUUID;
import org.jetbrains.annotations.NotNull;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


/*-
Generating many secure UUID's fast has some pitfalls. The most common way is to simply call: java.util.UUID.randomUUID()


Conclusion:
- creation of new SecureRandom instances is expensive - Make sure to use a ThreadLocal<SecureRandom> to generate UUID's and other crypto stuff, especially in a webserver context!
- Try to avoid plain java.util.UUID.randomUUID() - always inject a ThreadLocal<SecureRandom> instance
- As we cannot change libraries to use ThreadLocal's, it is still god practice to start the jvm with: -Djava.security.egd=file:/dev/./urandom"
  - to speed up the creation of new SecureRandom instances
  - to avoid blocking if the random pool of the operating system is exhausted, as the default settings use the blocking /dev/random
- if you are forced to stay at JDK 8 or lower consider using fastUUID
- JDK >=12 has very nice String concatenation optimizations, rendering fastUUID obsolete
    -  but JDK12 java.util.UUID still has the single shared SecureRandom instance, and makes it suffer form the lock contention in SecureRandomSpi#engineNextBytes, killing performance!

Benchmark                                                    Mode  Cnt  Score   Error  Units  gc.alloc.rate.norm
Contended
    default java.util.UUID.randomUUID()
        jdkUUID_default_egdDefault_16Threads                 avgt   10   28104 ±   1519 ns/op    176 B/op #heavy lock contention in SecureRandomSpi#engineNextBytes
        jdkUUID_default_egdRandom_16Threads                  avgt   10   28411 ±   1717 ns/op    176 B/op #heavy lock contention in SecureRandomSpi#engineNextBytes
        jdkUUID_default_egdUrandom_16Threads                 avgt   10   28248 ±   2012 ns/op    176 B/op #heavy lock contention in SecureRandomSpi#engineNextBytes
    threadLocal of SHA1PRNG
        jdkUUID_threadLocalSHA1_egdDefault_16Threads         avgt   10     974 ±     16 ns/op    208 B/op #winner by 29x speedup- ThreadLocal<SecureRandom> eliminates the lock contention and SecureRandom "SHA1PRNG" does not need syscalls to /dev/urandom except for seeding
        jdkUUID_threadLocalSHA1_egdUrand_16Threads           avgt   10    1071 ±     19 ns/op    208 B/op #2nd - ThreadLocal<SecureRandom> eliminates the lock contention
        fastUuid_threadLocalSHA1_egdDefault_16Threads        avgt   10    1208 ±     97 ns/op    296 B/op #fastUuid obsolete in jdk13 because of JDK 13 string concatenation optimizations in java.util.UUID
        fastUuid_threadLocalSHA1_egdUrand_16Threads          avgt   10    1151 ±     49 ns/op    296 B/op #fastUuid obsolete in jdk13 because of JDK 13 string concatenation optimizations in java.util.UUID
    threadLocal of default new SecureRandom()
        jdkUUID_threadLocalDefault_egdDefault_16Threads      avgt   10   41542 ±   4099 ns/op    304 B/op #default new SecureRandom() algo is very slow
        jdkUUID_threadLocalDefault_egdUrand_16Threads        avgt   10   41676 ±   4826 ns/op    304 B/op #default new SecureRandom() algo is very slow
        fastUuid_threadLocalDefault_egdDefault_16Threads     avgt   10   41114 ±   1381 ns/op    392 B/op #fastUuid obsolete in jdk13 because of JDK 13 string concatenation optimizations in java.util.UUID - default new SecureRandom() algo is very slow
        fastUuid_threadLocalDefault_egdUrand_16Threads       avgt   10   43666 ±   5049 ns/op    392 B/op #fastUuid obsolete in jdk13 because of JDK 13 string concatenation optimizations in java.util.UUID- default new SecureRandom() algo is very slow
Single Thread
    default java.util.UUID.randomUUID()
        jdkUUID_default_egdDefault_1Thread                   avgt   10    1311 ±    140 ns/op    176 B/op
        jdkUUID_default_egdRandom_1Thread                    avgt   10    1274 ±     60 ns/op    176 B/op
        jdkUUID_default_egdUrandom_1Thread                   avgt   10    1259 ±     17 ns/op    176 B/op
    threadLocal of SHA1PRNG
        jdkUUID_threadLocalSHA1_egdDefault_1Thread           avgt   10     313 ±     10 ns/op    208 B/op #winner
        jdkUUID_threadLocalSHA1_egdUrand_1Thread             avgt   10     312 ±      9 ns/op    208 B/op #winner
        fastUuid_threadLocalSHA1_egdDefault_1Thread          avgt   10     341 ±      9 ns/op    296 B/op
        fastUuid_threadLocalSHA1_egdUrand_1Thread            avgt   10     353 ±     28 ns/op    296 B/op
    threadLocal of default new SecureRandom()
        jdkUUID_threadLocalDefault_egdDefault_1Thread        avgt   10    1465 ±     61 ns/op    304 B/op
        jdkUUID_threadLocalDefault_egdUrand_1Thread          avgt   10    1442 ±     45 ns/op    304 B/op
        fastUuid_threadLocalDefault_egdDefault_1Thread       avgt   10    1516 ±     84 ns/op    392 B/op #fastUuid obsolete in jdk13 because of JDK 13 string concatenation optimizations in java.util.UUID
        fastUuid_threadLocalDefault_egdUrand_1Thread         avgt   10    1506 ±    139 ns/op    392 B/op #fastUuid obsolete in jdk13 because of JDK 13 string concatenation optimizations in java.util.UUID


Single Shot Times (take with caution and see Error ± ns/ops!)
Benchmark                                                    Mode  Cnt  Score   Error  Units  gc.alloc.rate.norm
Contented
    default java.util.UUID.randomUUID()
        jdkUUID_default_egdRandom_16Threads                    ss   10  145004 ±  84358 ns/op    697 B/op
        jdkUUID_default_egdUrandom_16Threads                   ss   10  119047 ±  70141 ns/op    695 B/op
        jdkUUID_default_egdDefault_16Threads                   ss   10  122152 ± 100788 ns/op    698 B/op
    threadLocal of SHA1PRNG
        jdkUUID_threadLocalSHA1_egdUrand_16Threads             ss   10   36048 ±  74992 ns/op    730 B/op
        jdkUUID_threadLocalSHA1_egdDefault_16Threads           ss   10   21510 ±  12884 ns/op    729 B/op #winner
        fastUuid_threadLocalSHA1_egdDefault_16Threads          ss   10   22538 ±  11399 ns/op    783 B/op
        fastUuid_threadLocalSHA1_egdUrand_16Threads            ss   10   24795 ±  17610 ns/op    786 B/op #fast uuid is becoming obsolete with new JDK's
    threadLocal of default new SecureRandom()
        jdkUUID_threadLocalDefault_egdDefault_16Threads        ss   10  266387 ± 253382 ns/op    824 B/op #default new SecureRandom() algo is very slow
        jdkUUID_threadLocalDefault_egdUrand_16Threads          ss   10  243048 ± 206100 ns/op    824 B/op #default new SecureRandom() algo is very slow
        fastUuid_threadLocalDefault_egdDefault_16Threads       ss   10  264334 ± 160103 ns/op    883 B/op #fast uuid is becoming obsolete with new JDK's
        fastUuid_threadLocalDefault_egdUrand_16Threads         ss   10  238833 ± 173709 ns/op    879 B/op #fast uuid is becoming obsolete with new JDK's
Single Thread
    default java.util.UUID.randomUUID()
        jdkUUID_default_egdRandom_1Thread                      ss   10   74492 ±  17443 ns/op    651 B/op
        jdkUUID_default_egdUrandom_1Thread                     ss   10   76133 ±  26322 ns/op    651 B/op
        jdkUUID_default_egdDefault_1Thread                     ss   10   75001 ±  29578 ns/op    651 B/op
    threadLocal of SHA1PRNG
        jdkUUID_threadLocalSHA1_egdUrand_1Thread               ss   10   32760 ±  15820 ns/op    683 B/op
        jdkUUID_threadLocalSHA1_egdDefault_1Thread             ss   10   37208 ±  32426 ns/op    683 B/op
        fastUuid_threadLocalSHA1_egdDefault_1Thread            ss   10   33382 ±  18155 ns/op    739 B/op
        fastUuid_threadLocalSHA1_egdUrand_1Thread              ss   10   32976 ±  15704 ns/op    742 B/op
    threadLocal of default new SecureRandom()
        jdkUUID_threadLocalDefault_egdUrand_1Thread            ss   10   86627 ±  27819 ns/op    779 B/op #default new SecureRandom() algo is very slow
        jdkUUID_threadLocalDefault_egdDefault_1Thread          ss   10   79746 ±  23491 ns/op    782 B/op #default new SecureRandom() algo is very slow
        fastUuid_threadLocalDefault_egdDefault_1Thread         ss   10   71999 ±  23583 ns/op    835 B/op #default new SecureRandom() algo is very slow
        fastUuid_threadLocalDefault_egdUrand_1Thread           ss   10   76244 ±  22388 ns/op    835 B/op #default new SecureRandom() algo is very slow



# VM version: JDK 1.8.0_232, OpenJDK 64-Bit Server VM, 25.232-b09
There are a few differences between jdk13 and jdk1.8
- setting egd to /dev/urandom really helps in 1.8 but not in 13
- 13 has far better java.util.UUID.toString
- in JDK13 changing the egd file to /dev/urandom appears to no longer work/benefits new SecureRandom()

Benchmark                                                       Mode  Cnt   Score    Error Units  gc.alloc.rate.norm
Contented
    default java.util.UUID.randomUUID()
        jdkUUID_default_egdDefault_16Threads                    avgt   10   27775 ±   1545 ns/op   864 B/op #heavy lock contention in SecureRandomSpi#engineNextBytes
        jdkUUID_default_egdRandom_16Threads                     avgt   10   27877 ±   1432 ns/op   864 B/op #heavy lock contention in SecureRandomSpi#engineNextBytes
        jdkUUID_default_egdUrandom_16Threads                    avgt   10    9785 ±    150 ns/op   832 B/op # -||- but egd /dev/urandom really helps
    threadLocal of SHA1PRNG
        jdkUUID_threadLocalSHA1_egdDefault_16Threads            avgt   10    1675 ±     19 ns/op   895 B/op #as we are using sha1prng and ThreadLocal the egd changes have no real impact
        jdkUUID_threadLocalSHA1_egdUrand_16Threads              avgt   10    1732 ±    157 ns/op   896 B/op
        fastUuid_threadLocalSHA1_egdDefault_16Threads           avgt   10    1116 ±     19 ns/op   328 B/op #
        fastUuid_threadLocalSHA1_egdUrand_16Threads             avgt   10    1110 ±     16 ns/op   328 B/op
    threadLocal of default new SecureRandom() - defaults to NativePRNG which should be in "MIXED" mode but appears to be in "BLOCKING" mode
        jdkUUID_threadLocalDefault_egdDefault_16Threads         avgt   10   44714 ±   1764 ns/op   960 B/op #
        jdkUUID_threadLocalDefault_egdUrand_16Threads           avgt   10    1670 ±    110 ns/op   864 B/op
        fastUuid_threadLocalDefault_egdDefault_16Threads        avgt   10   45233 ±   2459 ns/op   424 B/op
        fastUuid_threadLocalDefault_egdUrand_16Threads          avgt   10    1047 ±     26 ns/op   328 B/op
Single Thread
    default java.util.UUID.randomUUID()
        jdkUUID_default_egdDefault_1Thread                      avgt   10    1405 ±     38 ns/op   864 B/op
        jdkUUID_default_egdRandom_1Thread                       avgt   10    1423 ±     50 ns/op   864 B/op
        jdkUUID_default_egdUrandom_1Thread                      avgt   10     485 ±     26 ns/op   832 B/op
    threadLocal of SHA1PRNG
        jdkUUID_threadLocalSHA1_egdDefault_1Thread              avgt   10     511 ±     24 ns/op   864 B/op
        jdkUUID_threadLocalSHA1_egdUrand_1Thread                avgt   10     508 ±      5 ns/op   864 B/op
        fastUuid_threadLocalSHA1_egdDefault_1Thread             avgt   10     317 ±      3 ns/op   328 B/op
        fastUuid_threadLocalSHA1_egdUrand_1Thread               avgt   10     346 ±      2 ns/op   328 B/op
    threadLocal of default new SecureRandom()
        jdkUUID_threadLocalDefault_egdDefault_1Thread           avgt   10    1674 ±     70 ns/op   960 B/op
        jdkUUID_threadLocalDefault_egdUrand_1Thread             avgt   10     558 ±     32 ns/op   864 B/op
        fastUuid_threadLocalDefault_egdDefault_1Thread          avgt   10    1459 ±    151 ns/op   424 B/op
        fastUuid_threadLocalDefault_egdUrand_1Thread            avgt   10     322 ±      3 ns/op   328 B/op

Single Shot Times (take with caution and see Error ± ns/ops!)
Contented
    default java.util.UUID.randomUUID()
        jdkUUID_default_egdDefault_16Threads                      ss   10  112142 ±  50139 ns/op  1636 B/op
        jdkUUID_default_egdRandom_16Threads                       ss   10   79360 ±  52899 ns/op  1634 B/op
        jdkUUID_default_egdUrandom_16Threads                      ss   10   41418 ±  26702 ns/op  1602 B/op
     threadLocal of SHA1PRNG
        jdkUUID_threadLocalSHA1_egdDefault_16Threads              ss   10   24562 ±  13889 ns/op  1665 B/op
        jdkUUID_threadLocalSHA1_egdUrand_16Threads                ss   10   26363 ±  21808 ns/op  1667 B/op
        fastUuid_threadLocalDefault_egdDefault_16Threads          ss   10  213622 ± 131631 ns/op   930 B/op
        fastUuid_threadLocalDefault_egdUrand_16Threads            ss   10   21596 ±  14276 ns/op   835 B/op
    threadLocal of default new SecureRandom()
        jdkUUID_threadLocalDefault_egdDefault_16Threads           ss   10  195319 ± 152402 ns/op  1762 B/op
        jdkUUID_threadLocalDefault_egdUrand_16Threads             ss   10   25829 ±  16283 ns/op  1667 B/op
        fastUuid_threadLocalSHA1_egdDefault_16Threads             ss   10   19171 ±  12634 ns/op   834 B/op
        fastUuid_threadLocalSHA1_egdUrand_16Threads               ss   10   22373 ±  24764 ns/op   833 B/op

Single Thread
    default java.util.UUID.randomUUID()
        jdkUUID_default_egdDefault_1Thread                        ss   10  100311 ±  29318 ns/op  1595 B/op
        jdkUUID_default_egdRandom_1Thread                         ss   10  113475 ±  61072 ns/op  1595 B/op
        jdkUUID_default_egdUrandom_1Thread                        ss   10   44194 ±  22689 ns/op  1566 B/op
    threadLocal of SHA1PRNG
        jdkUUID_threadLocalSHA1_egdDefault_1Thread                ss   10   53086 ±  25865 ns/op  1630 B/op
        jdkUUID_threadLocalSHA1_egdUrand_1Thread                  ss   10   49497 ±  23356 ns/op  1627 B/op
        fastUuid_threadLocalSHA1_egdDefault_1Thread               ss   10   40451 ±  23212 ns/op   795 B/op
        fastUuid_threadLocalSHA1_egdUrand_1Thread                 ss   10   35669 ±  19707 ns/op   798 B/op
    threadLocal of default new SecureRandom()
        jdkUUID_threadLocalDefault_egdDefault_1Thread             ss   10  126737 ±  46842 ns/op  1723 B/op
        jdkUUID_threadLocalDefault_egdUrand_1Thread               ss   10   48443 ±  26510 ns/op  1627 B/op
        fastUuid_threadLocalDefault_egdDefault_1Thread            ss   10  112593 ±  38390 ns/op   891 B/op
        fastUuid_threadLocalDefault_egdUrand_1Thread              ss   10   36503 ±  15230 ns/op   795 B/op

 */
@BenchmarkMode({Mode.AverageTime, Mode.SingleShotTime})
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class UUIDSecureRandomJVMSwitchesJMH {

    private static final ThreadLocal<SecureRandom> SECURE_RAND_DEF = ThreadLocal.withInitial(SecureRandom::new);
    private static final ThreadLocal<SecureRandom> SECURE_RAND_SHA1 = ThreadLocal.withInitial(() -> {
        try {
            return SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    });


    public static void main(String[] args) throws RunnerException, IOException {
        //System.out.println( new URL("file:/dev/urandom").openConnection().getInputStream().read());
        System.out.println(Optional.of(new SecureRandom()).map(r -> "new SecureRandom() =>  '" + r.getProvider() + "' " + r.getAlgorithm()).orElse("-no instance--"));
        printSecureRandomProviders();
        //-Djava.security.debug=provider
        Options opt = new OptionsBuilder()
                .include(UUIDSecureRandomJVMSwitchesJMH.class.getName())
                .resultFormat(ResultFormatType.JSON)
                .result(String.format("%s_%s.json",
                        DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                        UUIDSecureRandomJVMSwitchesJMH.class.getSimpleName()))
                .addProfiler(GCProfiler.class)
                .build();
        Runner r = new Runner(opt);
        r.list();
        Collection<RunResult> results = r.run();
    }

    private static void printSecureRandomProviders() {
        System.out.println("SecureRandom providers");
        System.out.printf("%-10s %-8s %-23s %s%n", "ThreadSafe", "Provider", "AlgoName", "Class");
        for (Provider p : Security.getProviders()) {
            for (Provider.Service s : p.getServices()) {
                if (s.getType().equals("SecureRandom")) {
                    System.out.printf("%-10b %-8s %-23s %s%n", getThreadSafe(s.getAlgorithm(), p), p.getName(), s.getAlgorithm(), s.getClassName());
                }
            }
        }
    }

    private static boolean getThreadSafe(String algorithm, Provider provider) {
        if (provider == null || algorithm == null) {
            return false;
        } else {
            return Boolean.parseBoolean(provider.getProperty(
                    "SecureRandom." + algorithm + " ThreadSafe", "false"));
        }
    }



    @Benchmark
    @Fork(value = 1)
    public String jdkUUID_default_egdDefault_1Thread() {
        return UUID.randomUUID().toString();
    }
    @Benchmark
    @Fork(value = 1)
    @Threads(16)
    public String jdkUUID_default_egdDefault_16Threads() {
        return UUID.randomUUID().toString();
    }

    @Benchmark
    @Fork(value = 1, jvmArgsAppend = "-Djava.security.egd=file:/dev/./urandom")
    public String jdkUUID_default_egdUrandom_1Thread() {
        return UUID.randomUUID().toString();
    }
    @Benchmark
    @Fork(value = 1, jvmArgsAppend = "-Djava.security.egd=file:/dev/./urandom")
    @Threads(16)
    public String jdkUUID_default_egdUrandom_16Threads() {
        return UUID.randomUUID().toString();
    }


    @Benchmark
    @Fork(value = 1, jvmArgsAppend = "-Djava.security.egd=file:/dev/random")
    public String jdkUUID_default_egdRandom_1Thread() {
        return UUID.randomUUID().toString();
    }
    @Benchmark
    @Fork(value = 1, jvmArgsAppend = "-Djava.security.egd=file:/dev/random")
    @Threads(16)
    public String jdkUUID_default_egdRandom_16Threads() {
        return UUID.randomUUID().toString();
    }


    @Benchmark
    @Fork(value = 1)
    public String jdkUUID_threadLocalSHA1_egdDefault_1Thread() {
        return randJDKUUID(SECURE_RAND_SHA1.get());
    }

    @Benchmark
    @Fork(value = 1)
    @Threads(16)
    public String jdkUUID_threadLocalSHA1_egdDefault_16Threads() {
        return randJDKUUID(SECURE_RAND_SHA1.get());
    }

    @Benchmark
    @Fork(value = 1)
    public String jdkUUID_threadLocalDefault_egdDefault_1Thread() {
        return randJDKUUID(SECURE_RAND_DEF.get());
    }
    @Benchmark
    @Fork(value = 1)
    @Threads(16)
    public String jdkUUID_threadLocalDefault_egdDefault_16Threads() {
        return randJDKUUID(SECURE_RAND_DEF.get());
    }


    @Benchmark
    @Fork(value = 1, jvmArgsAppend = "-Djava.security.egd=file:/dev/./urandom")
    public String jdkUUID_threadLocalSHA1_egdUrand_1Thread() {
        return randJDKUUID(SECURE_RAND_SHA1.get());
    }

    @Benchmark
    @Fork(value = 1, jvmArgsAppend = "-Djava.security.egd=file:/dev/./urandom")
    @Threads(16)
    public String jdkUUID_threadLocalSHA1_egdUrand_16Threads() {
        return randJDKUUID(SECURE_RAND_SHA1.get());
    }

    @Benchmark
    @Fork(value = 1, jvmArgsAppend = "-Djava.security.egd=file:/dev/./urandom")
    public String jdkUUID_threadLocalDefault_egdUrand_1Thread() {
        return randJDKUUID(SECURE_RAND_DEF.get());
    }
    @Benchmark
    @Fork(value = 1, jvmArgsAppend = "-Djava.security.egd=file:/dev/./urandom")
    @Threads(16)
    public String jdkUUID_threadLocalDefault_egdUrand_16Threads() {
        return randJDKUUID(SECURE_RAND_DEF.get());
    }

    @Benchmark
    @Fork(value = 1)
    public String fastUuid_threadLocalSHA1_egdDefault_1Thread() {
        return FastUUID.randomUUID(SECURE_RAND_SHA1.get());
    }

    @Benchmark
    @Fork(value = 1)
    @Threads(16)
    public String fastUuid_threadLocalSHA1_egdDefault_16Threads() {
        return FastUUID.randomUUID(SECURE_RAND_SHA1.get());
    }

    @Benchmark
    @Fork(value = 1)
    public String fastUuid_threadLocalDefault_egdDefault_1Thread() {
        return FastUUID.randomUUID(SECURE_RAND_DEF.get());
    }
    @Benchmark
    @Fork(value = 1)
    @Threads(16)
    public String fastUuid_threadLocalDefault_egdDefault_16Threads() {
        return FastUUID.randomUUID(SECURE_RAND_DEF.get());
    }

    @Benchmark
    @Fork(value = 1, jvmArgsAppend = "-Djava.security.egd=file:/dev/./urandom")
    public String fastUuid_threadLocalSHA1_egdUrand_1Thread() {
        return FastUUID.randomUUID(SECURE_RAND_SHA1.get());
    }
    @Benchmark
    @Fork(value = 1, jvmArgsAppend = "-Djava.security.egd=file:/dev/./urandom")
    @Threads(16)
    public String fastUuid_threadLocalSHA1_egdUrand_16Threads() {
        return FastUUID.randomUUID(SECURE_RAND_SHA1.get());
    }

    @Benchmark
    @Fork(value = 1, jvmArgsAppend = "-Djava.security.egd=file:/dev/./urandom")
    public String fastUuid_threadLocalDefault_egdUrand_1Thread() {
        return FastUUID.randomUUID(SECURE_RAND_DEF.get());
    }

    @Benchmark
    @Fork(value = 1, jvmArgsAppend = "-Djava.security.egd=file:/dev/./urandom")
    @Threads(16)
    public String fastUuid_threadLocalDefault_egdUrand_16Threads() {
        return FastUUID.randomUUID(SECURE_RAND_DEF.get());
    }


    @NotNull
    private static String randJDKUUID(SecureRandom r) {
        long msl = r.nextLong();
        long lsl = r.nextLong();
        msl &= ~0xF000L;/* clear version */
        msl |= 0x4000;/* set to version 4 */
        lsl &= 0x3fFFFFFFFFFFFFFFL; /* clear variant */
        lsl |= 0x8000000000000000L; /* set to IETF variant */
        return new UUID(msl, lsl).toString();
    }

}
