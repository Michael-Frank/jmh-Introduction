package de.frank.jmh.architecture;

import org.apache.commons.lang.RandomStringUtils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

/*--
Througput  ops/s         20        200     2000  #<-dataLen
newEachTime       4.126.463  2.282.067  330.163  #
threadLocal       8.620.123  2.542.658  332.177  # caching is not much faster..

newEachTime.gc.Norm        648 B/op           653 B/op       584 B/op  #
threadLocal.gc.Norm         20 B/op            48 B/op        48 B/op  # but way more GC friendly (reduced GC times and GC Pauses!)

*/

/**
 * @author Michael Frank
 * @version 1.0 13.07.2018
 */
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(3)
@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark) // Important to be Scope.Benchmark
@Threads(16)
public class DigestCacheThreadLocalJMH {


    public static final ThreadLocal<MessageDigest> DIGESTS = ThreadLocal.withInitial(() -> newDigest());

    @Param({"20", "200", "2000"})
    public int dataLen;
    public byte[] data;

    @Setup
    public void setup() {
        data = RandomStringUtils.randomAlphanumeric(dataLen).getBytes();
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Benchmark
    public byte[] newEachTime() {
        MessageDigest digest = newDigest();
        return digest.digest(data);
    }

    @Benchmark
    public byte[] threadLocal() {
        MessageDigest digest = DIGESTS.get();
        digest.reset();
        return digest.digest(data);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()//
                .include(".*" + DigestCacheThreadLocalJMH.class.getSimpleName() + ".*")//
                .addProfiler(GCProfiler.class).build();
        new Runner(opt).run();
    }
}
