package de.frank.jmh.basic;

import org.jetbrains.annotations.NotNull;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
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
import java.util.concurrent.TimeUnit;


/*-
# VM version: JDK 13.0.1, OpenJDK 64-Bit Server VM, 13.0.1+9

 */
@BenchmarkMode({Mode.AverageTime, Mode.SingleShotTime})
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class SecureRandomJVMSwitchesJMH {

    public enum SecurePRNG {
        NEW_DEFAULT {
            public SecureRandom get() {
                return new SecureRandom();
            }
        },
        NEW_SHA1 {
            public SecureRandom get() {
                return newSHA1PRNGInstance();
            }
        },
        THREADLOCAL_DEFAULT {
            private final ThreadLocal<SecureRandom> SECURE_RAND_DEF = ThreadLocal.withInitial(SecureRandom::new);

            public SecureRandom get() {
                return SECURE_RAND_DEF.get();
            }
        },
        THREADLOCAL_SHA1 {
            private final ThreadLocal<SecureRandom> SECURE_RAND_SHA1 = ThreadLocal.withInitial(SecurePRNG::newSHA1PRNGInstance);

            public SecureRandom get() {
                return SECURE_RAND_SHA1.get();
            }
        };


        public abstract SecureRandom get();

        @NotNull
        public static SecureRandom newSHA1PRNGInstance() {
            try {
                return SecureRandom.getInstance("SHA1PRNG");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public enum Workload {
        ONE_LONG {
            @Override
            public void consume(Blackhole bh, SecureRandom r) {
                bh.consume(r.nextLong());
            }
        },
        BYTE_16 {
            @Override
            public void consume(Blackhole bh, SecureRandom r) {
                byte[] bytes = new byte[16];
                r.nextBytes(bytes);
                bh.consume(bytes);
            }
        };

        public abstract void consume(Blackhole bh, SecureRandom r);
    }


    @Param({"NEW_DEFAULT", "NEW_SHA1", "THREADLOCAL_DEFAULT", "THREADLOCAL_SHA1"})
    public SecurePRNG prng;

    @Param({"ONE_LONG", "BYTE_16"})
    public Workload workload;

    public static void main(String[] args) throws RunnerException, IOException {
        //System.out.println( new URL("file:/dev/urandom").openConnection().getInputStream().read());
        System.out.println(Optional.of(new SecureRandom()).map(r -> "new SecureRandom() =>  '" + r.getProvider() + "' " + r.getAlgorithm()).orElse("-no instance--"));
        printSecureRandomProviders();
        //-Djava.security.debug=provider
        Options opt = new OptionsBuilder()
                .include(SecureRandomJVMSwitchesJMH.class.getName())
                .resultFormat(ResultFormatType.JSON)
                .result(String.format("%s_%s.json",
                        DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                        SecureRandomJVMSwitchesJMH.class.getSimpleName()))
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
    public void egdDefault_1Thread(Blackhole bh) {
        workload.consume(bh, prng.get());
    }

    @Benchmark
    @Fork(value = 1)
    @Threads(16)
    public void egdDefault_16Threads(Blackhole bh) {
        workload.consume(bh, prng.get());
    }

    @Benchmark
    @Fork(value = 1, jvmArgsAppend = "-Djava.security.egd=file:/dev/./urandom")
    @Threads(16)
    public void egdUrandom_16Threads(Blackhole bh) {
        workload.consume(bh, prng.get());
    }

    @Benchmark
    @Fork(value = 1, jvmArgsAppend = "-Djava.security.egd=file:/dev/./urandom")
    public void egdUrandom_1Thread(Blackhole bh) {
        workload.consume(bh, prng.get());
    }


}
