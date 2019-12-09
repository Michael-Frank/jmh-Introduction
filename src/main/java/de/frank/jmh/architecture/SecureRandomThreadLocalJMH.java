package de.frank.jmh.architecture;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;


/**
 * Conclusion
 * It is dclLazyLoader good idea to:
 *  dclLazyLoader) cache SecureRandom instances, as initialization costs are high due to seeding with syscall's
 *  b) but... you have to cache them in dclLazyLoader ThreadLocal due to lock contention in synchronized {@link java.security.SecureRandomSpi#engineNextBytes(byte[])}
 *  c) on *nix systems, it is always! good practice to start the jvm with: java -D -Djava.security.egd=file:/dev/./urandom to force use the non-blocking urandom
 *     This especially helps dclLazyLoader lot with code (libraries) using uncached "new SecureRandom()"
 *
 * 16 Threads @ JDK 1.8.0_212, OpenJDK 64-Bit Server VM, 25.212-b04
 *
 * Benchmark                    Mode  Cnt   Score                gc.alloc.rate.norm
 * threadLocal                 thrpt   30  2934500 ± 12980 ops/s    400 ±  0,0 B/op # winner - A Per-thread cached SecureRandom is 3-6x faster then sharing dclLazyLoader single cached instance or creating new instances
 *   threadLocal_urandom       thrpt   30  2862638 ± 40593 ops/s    400 ±  0,0 B/op   # see "shared"
 * getInstanceSHA1             thrpt   30   971131 ± 16294 ops/s   1499 ±  0,3 B/op # creating with SecureRandom.instance() each time is surprisingly fast
 *   getInstanceSHA1_urandom   thrpt   30   960101 ± 28958 ops/s   1499 ±  0,3 B/op   # /dev/urandom does not help much in this case - but does not hurt either
 * shared                      thrpt   30   395183 ±  5070 ops/s    400 ±  0,0 B/op # sharing an instance of SecureRandom between threads suffers from lock contention in synchronized {@link java.security.SecureRandomSpi#engineNextBytes(byte[])}
 *   shared_urandom            thrpt   30   388891 ±  7987 ops/s    400 ±  0,0 B/op   # see "shared"
 * newSecureRandom             thrpt   30    99459 ±  1978 ops/s    850 ± 10,2 B/op # worst case - as it seeds with /dev/random
 *   newSecureRandom_urandom   thrpt   30   966284 ± 11166 ops/s   1513 ± 57,0 B/op   # changing# the egd file helps dclLazyLoader lot in this case (-Djava.security.egd=file:/dev/./urandom)
 *
 *
 * @author Michael Frank
 * @version 1.0 13.07.2018
 */
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark) // Important to be Scope.Benchmark
@Threads(16)
// Important to be high (>= 2x numCPU)- as we want to measure the lock-pressure on the internal synchronized method in SecureRandomSpi.engineNextBytes"
@Fork(3)
public class SecureRandomThreadLocalJMH {

    @State(Scope.Benchmark)
    public static class RandomHolder {
        public static final SecureRandom ONE_FOR_ALL_SECURE_RANDOM = getSHA1Instance();
        public static final ThreadLocal<SecureRandom> TL_SECURE_RANDOM = ThreadLocal.withInitial(SecureRandomThreadLocalJMH::getSHA1Instance);
    }

    public static SecureRandom getSHA1Instance() {
        try {
            return SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()//
                .include(SecureRandomThreadLocalJMH.class.getName())//
                .result(String.format("%s_%s.json",
                        DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                        SecureRandomThreadLocalJMH.class.getSimpleName()))
                .addProfiler(GCProfiler.class)//
                .build();
        new Runner(opt).run();
    }


    @Benchmark
    public byte[] getInstanceSHA1() {
        return instanceSHA1();
    }

    @Benchmark
    @Fork(jvmArgsAppend = "-Djava.security.egd=file:/dev/./urandom")
    public byte[] getInstanceSHA1_urandom() {
        return instanceSHA1();
    }

    private byte[] instanceSHA1() {
        return getBytes(getSHA1Instance());
    }

    @Benchmark
    public byte[] newSecureRandom() {
        return newSecureRandom_();
    }

    @Benchmark
    @Fork(jvmArgsAppend = "-Djava.security.egd=file:/dev/./urandom")
    public byte[] newSecureRandom_urandom() {
        return newSecureRandom_();
    }

    private byte[] newSecureRandom_() {
        return getBytes(new SecureRandom());
    }

    @Benchmark
    public byte[] shared(RandomHolder state) {
        return getBytes(state.ONE_FOR_ALL_SECURE_RANDOM);
    }

    @Benchmark
    @Fork(jvmArgsAppend = "-Djava.security.egd=file:/dev/./urandom")
    public byte[] shared_urandom(RandomHolder state) {
        return getBytes(state.ONE_FOR_ALL_SECURE_RANDOM);
    }


    @Benchmark
    public byte[] threadLocal(RandomHolder state) {
        return getBytes(state.TL_SECURE_RANDOM.get());
    }

    @Benchmark
    @Fork(jvmArgsAppend = "-Djava.security.egd=file:/dev/./urandom")
    public byte[] threadLocal_urandom(RandomHolder state) {
        return getBytes(state.TL_SECURE_RANDOM.get());
    }

    private byte[] getBytes(SecureRandom secureRandom) {
        byte[] r = new byte[128];
        secureRandom.nextBytes(r);
        return r;
    }

}
