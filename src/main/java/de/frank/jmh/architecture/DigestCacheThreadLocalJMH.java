package de.frank.jmh.architecture;

import org.apache.commons.lang.RandomStringUtils;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/*--
The MessageDigest.getInstance("SHA-256") calls are surprisingly and allocate an reasonable amount of memory.
The benefits of caching the MessageDigest instance are mostly visible in two cases
- Optimizing Latency: less garbage create -> reduced GC times and GC Pauses  (only really shows when hashing small data chunks very frequently)
- hashing LOTS of very small data blocks

Throughput ops/s         20        200     2000  #<-dataLen
newEachTime       4.126.463  2.282.067  330.163  #
threadLocal       8.620.123  2.542.658  332.177  # much faster for small data - benefits diminish when hashing bigger junks.

Allocations per Invocation in B/op:
                         20        200     2000  #<-dataLen
newEachTime.gc.Norm     648        653      584  #
threadLocal.gc.Norm      20         48       48  # but way more GC friendly (reduced GC times and GC Pauses!)

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
                .include(DigestCacheThreadLocalJMH.class.getName())//
                .result(String.format("%s_%s.json",
                        DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                        DigestCacheThreadLocalJMH.class.getSimpleName()))
                .addProfiler(GCProfiler.class).build();
        new Runner(opt).run();
    }
}
