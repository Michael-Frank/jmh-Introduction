package de.frank.jmh.basic;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/*--
 *What this is about:
 *
 * String (from/to bytes) has an internal ThreadLocal cache for charset encoders/decoders.
 * But ONLY IF:
 *  <ul>
 *   <li>you call it with the charset name as String</li>
 *   <li>the calling thread does not alternate between charsets</li>
 * </ul>
 *
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
 *



Results:

Benchmark                                                        Mode  Cnt     Score     Error   Units
bytesFromString_CharsetInstance                                  avgt   30   736,417 ±  20,538   ns/op
bytesFromString_CharsetInstance:·gc.alloc.rate.norm              avgt   30   576,000 ±   0,001    B/op
bytesFromString_CharsetName                                      avgt   30   632,758 ±   8,125   ns/op
bytesFromString_CharsetName:·gc.alloc.rate.norm                  avgt   30   472,000 ±   0,001    B/op <-name is faster ,but more important: less allocation per invoc.

stringFromBytes_CharsetInstance                                  avgt   30   742,519 ±   5,112   ns/op
stringFromBytes_CharsetInstance:·gc.alloc.rate.norm              avgt   30   552,000 ±   0,001    B/op
stringFromBytes_CharsetName                                      avgt   30   719,842 ±  33,788   ns/op
stringFromBytes_CharsetName:·gc.alloc.rate.norm                  avgt   30   512,000 ±   0,001    B/op <-name is faster, but more important: less allocation per invoc.

stringFromBytes_CharsetName_toggleBaseLine                       avgt   30   712,564 ±  19,960   ns/op
stringFromBytes_CharsetName_toggleBaseLine:·gc.alloc.rate.norm   avgt   30   512,000 ±   0,001    B/op
stringFromBytes_CharsetName_toggling                             avgt   30  2153,854 ±  19,096   ns/op <- the "cached" part takes a serious perf hit if we constantly invalidate the cache by alternating between encodings (not very realistic, but keep in mind the cache is global per thread...)
stringFromBytes_CharsetName_toggling:·gc.alloc.rate.norm         avgt   30   545,376 ±   2,468    B/op
stringFromBytes_CharsetInstance_toggling                         avgt   30   595,438 ±  15,470   ns/op
stringFromBytes_CharsetInstance_toggling:·gc.alloc.rate.norm     avgt   30   436,000 ±   0,001    B/op
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
        private boolean toggleState = true;

        Charset charset() {
            return StandardCharsets.UTF_8;
        }

        String charsetName() {
            return StandardCharsets.UTF_8.name();
        }

        Charset charsetToggle() {
            return toggle() ? StandardCharsets.UTF_8 : StandardCharsets.ISO_8859_1;
        }

        String charsetNameToggle() {
            return toggle() ? StandardCharsets.UTF_8.name() : StandardCharsets.ISO_8859_1.name();
        }

        boolean toggle() {
            return toggleState ^= true;
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()//
                .include(StringGetBytesCharsetEncoderCacheJMH.class.getName() + ".*")//
                .addProfiler(GCProfiler.class)//
                .jvmArgs("-Xmx128m")
                .build();

        new Runner(opt).run();
    }


    @Benchmark
    public byte[] bytesFromString_CharsetInstance(ThreadState s) {
        return s.stringData.getBytes(s.charset());
    }

    @Benchmark
    public byte[] bytesFromString_CharsetName(ThreadState s) throws UnsupportedEncodingException {
        return s.stringData.getBytes(s.charsetName());
    }


    @Benchmark
    public String stringFromBytes_CharsetInstance(ThreadState s) {
        return new String(s.byteData, s.charset());
    }

    @Benchmark
    public String stringFromBytes_CharsetName(ThreadState s) throws UnsupportedEncodingException {
        return new String(s.byteData, s.charsetName());
    }


    @Benchmark
    public String stringFromBytes_CharsetName_toggleBaseLine(ThreadState s) throws UnsupportedEncodingException {
        return new String(s.byteData, s.toggle() ? s.charsetName() : s.charsetName());
    }

    @Benchmark
    public String stringFromBytes_CharsetName_toggling(ThreadState s) throws UnsupportedEncodingException {
        return new String(s.byteData, s.charsetNameToggle());
    }

    @Benchmark
    public String stringFromBytes_CharsetInstance_toggling(ThreadState s) {
        return new String(s.byteData, s.charsetToggle());
    }


}
