package de.frank.jmh.architecture;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/*--

Throughput benchmark          Score Units
threadLocal               1.957.664 ops/s  # winner - A Per-thread cached SecureRandom is much faster then sharing a single cached instance
shared                      363.403 ops/s  # the shared version suffers from lock contention in public synchronized void engineNextBytes(byte[] result)
getInstance               1.111.351 ops/s  # creating a new SecureRandom.instance() each time is surprisingly fast


gc.alloc.rate.norm benchmark  Score Units
threadLocal                     400 B/op  # winner
shared                          400 B/op
getInstance                   1.385 B/op


 */

/**
 * @author Michael Frank
 * @version 1.0 13.07.2018
 */
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(3)
@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark) // Important to be Scope.Benchmark
@Threads(16)// Important to be high - we want to measure the lock-pressure on the internal  method in SecureRandomSpi "synchronized engineNextBytes"
public class SecureRandomThreadLocalJMH {

    @State(Scope.Benchmark)
    public static class RandomHolder {
        public static final SecureRandom ONE_FOR_ALL_SECURE_RANDOM = getSecureRandom();
        public static final ThreadLocal<SecureRandom> TL_SECURE_RANDOM = ThreadLocal.withInitial(() -> getSecureRandom());
    }

    public static SecureRandom getSecureRandom() {
        try {
            return SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()//
                .include(".*" + SecureRandomThreadLocalJMH.class.getSimpleName() + ".*")//
                .addProfiler(GCProfiler.class).build();
        new Runner(opt).run();
    }


    @Benchmark
    public byte[] getInstance(RandomHolder state) {
        try {
            byte[] r = new byte[128];
            SecureRandom.getInstance("SHA1PRNG").nextBytes(r);
            return r;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

    }

    @Benchmark
    public byte[] shared(RandomHolder state) {
        byte[] r = new byte[128];
        state.ONE_FOR_ALL_SECURE_RANDOM.nextBytes(r);
        return r;
    }

    @Benchmark
    public byte[] threadLocal(RandomHolder state) {
        byte[] r = new byte[128];
        state.TL_SECURE_RANDOM.get().nextBytes(r);
        return r;
    }

}
