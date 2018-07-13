package de.frank.jmh.basic;

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

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/*--
 *What this is about:
 *
 * String (from/to bytes) has an internal ThreadLocal cache for charset encoders/decoders.
 * But ONLY IF you call it with the charset name:
 * <ul>
 *   <li>new String(bytes, "UTF-8")</li>
 *   <li>"foo".getBytes("UTF-8")</li>
 * </ul>
 *
 * The alternative interfaces using the Charset Instance will not! benefit from this cache!
 * <ul>
 *   <li>new String(bytes, StandardCharsets.UTF_8)//No cache benefit!</li>
 *   <li>"foo".getBytes(StandardCharsets.UTF_8)//No cache benefit!</li>
 * </ul>


 Results:

 bytesFromString_CharsetInstance                                   avgt   30   173,612 ±   3,247   ns/op
 bytesFromString_CharsetInstance:·gc.alloc.rate.norm               avgt   30   576,000 ±   0,001    B/op
 bytesFromString_CharsetName                                       avgt   30   178,571 ±  10,577   ns/op <-faster but more important: less allocation per invoc.
 bytesFromString_CharsetName:·gc.alloc.rate.norm                   avgt   30   472,000 ±   0,001    B/op

 stringFromBytes_CharsetInstance                                   avgt   30   194,999 ±  10,838   ns/op
 stringFromBytes_CharsetInstance:·gc.alloc.rate.norm               avgt   30   552,000 ±   0,001    B/op
 stringFromBytes_CharsetName                                       avgt   30   198,187 ±  17,974   ns/op <-faster but more important: less allocation per invoc.
 stringFromBytes_CharsetName:·gc.alloc.rate.norm                   avgt   30   512,000 ±   0,001    B/op



*/

/**
 * @author Michael Frank
 * @version 1.0 13.05.2018
 */
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(3)
@BenchmarkMode({Mode.AverageTime})
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Threads(16)//important to be high to put pressure on the cache
public class StringGetBytesCharsetEncoderCacheJMH {

    @State(Scope.Thread)
    public static class ThreadState {
        private String stringData = "löko3lö3laöskfjölaw3kr4j21öl5kjrfölskjfö2lqk3jrlkasjföl2k3jröl2kj5ölksdjfs23234l21l3j4lkjflksjlökjcv23lk4j";
        private byte[] byteData = stringData
                .getBytes(StandardCharsets.UTF_8);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()//
                .include(".*" + StringGetBytesCharsetEncoderCacheJMH.class.getSimpleName() + ".*")//
                .addProfiler(GCProfiler.class)//
                .jvmArgs("-Xmx128m")
                .build();

        new Runner(opt).run();
    }

    @Benchmark
    public String stringFromBytes_CharsetInstance(ThreadState s) {
        return new String(s.byteData, StandardCharsets.UTF_8);
    }

    @Benchmark
    public String stringFromBytes_CharsetName(ThreadState s) throws UnsupportedEncodingException {
        return new String(s.byteData, StandardCharsets.UTF_8.name());
    }

    @Benchmark
    public byte[] bytesFromString_CharsetInstance(ThreadState s) {
        return s.stringData.getBytes(StandardCharsets.UTF_8);
    }

    @Benchmark
    public byte[] bytesFromString_CharsetName(ThreadState s) throws UnsupportedEncodingException {
        return s.stringData.getBytes(StandardCharsets.UTF_8.name());
    }
}
