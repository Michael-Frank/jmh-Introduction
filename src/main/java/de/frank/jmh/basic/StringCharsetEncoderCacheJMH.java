package de.frank.jmh.basic;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

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

/**--
 *What this is about:
 *
 * String (from/to bytes) has an internal threadLocal cache for charset encoders/decoders.
 * But ONLY IF you call it with the charset name, not the Object like:
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


 Benchmark                                                         Mode  Cnt     Score     Error   Units
 bytesFromString_CharsetInstance                                   avgt   30   173,612 ±   3,247   ns/op
 bytesFromString_CharsetInstance:·gc.alloc.rate                    avgt   30  2111,085 ±  38,578  MB/sec
 bytesFromString_CharsetInstance:·gc.alloc.rate.norm               avgt   30   576,000 ±   0,001    B/op
 bytesFromString_CharsetInstance:·gc.churn.PS_Eden_Space           avgt   30  2110,936 ± 143,776  MB/sec
 bytesFromString_CharsetInstance:·gc.churn.PS_Eden_Space.norm      avgt   30   576,213 ±  40,116    B/op
 bytesFromString_CharsetInstance:·gc.churn.PS_Survivor_Space       avgt   30     0,082 ±   0,031  MB/sec
 bytesFromString_CharsetInstance:·gc.churn.PS_Survivor_Space.norm  avgt   30     0,022 ±   0,008    B/op
 bytesFromString_CharsetInstance:·gc.count                         avgt   30   219,000            counts
 bytesFromString_CharsetInstance:·gc.time                          avgt   30   210,000                ms
 bytesFromString_CharsetName                                       avgt   30   178,571 ±  10,577   ns/op
 bytesFromString_CharsetName:·gc.alloc.rate                        avgt   30  1693,559 ±  91,789  MB/sec
 bytesFromString_CharsetName:·gc.alloc.rate.norm                   avgt   30   472,000 ±   0,001    B/op
 bytesFromString_CharsetName:·gc.churn.PS_Eden_Space               avgt   30  1685,081 ± 192,440  MB/sec
 bytesFromString_CharsetName:·gc.churn.PS_Eden_Space.norm          avgt   30   468,166 ±  45,187    B/op
 bytesFromString_CharsetName:·gc.churn.PS_Survivor_Space           avgt   30     0,071 ±   0,026  MB/sec
 bytesFromString_CharsetName:·gc.churn.PS_Survivor_Space.norm      avgt   30     0,020 ±   0,007    B/op
 bytesFromString_CharsetName:·gc.count                             avgt   30   176,000            counts
 bytesFromString_CharsetName:·gc.time                              avgt   30   382,000                ms
 stringFromBytes_CharsetInstance                                   avgt   30   194,999 ±  10,838   ns/op
 stringFromBytes_CharsetInstance:·gc.alloc.rate                    avgt   30  1812,845 ±  90,329  MB/sec
 stringFromBytes_CharsetInstance:·gc.alloc.rate.norm               avgt   30   552,000 ±   0,001    B/op
 stringFromBytes_CharsetInstance:·gc.churn.PS_Eden_Space           avgt   30  1809,980 ± 225,956  MB/sec
 stringFromBytes_CharsetInstance:·gc.churn.PS_Eden_Space.norm      avgt   30   548,936 ±  57,991    B/op
 stringFromBytes_CharsetInstance:·gc.churn.PS_Survivor_Space       avgt   30     0,052 ±   0,020  MB/sec
 stringFromBytes_CharsetInstance:·gc.churn.PS_Survivor_Space.norm  avgt   30     0,016 ±   0,006    B/op
 stringFromBytes_CharsetInstance:·gc.count                         avgt   30   134,000            counts
 stringFromBytes_CharsetInstance:·gc.time                          avgt   30   267,000                ms
 stringFromBytes_CharsetName                                       avgt   30   198,187 ±  17,974   ns/op
 stringFromBytes_CharsetName:·gc.alloc.rate                        avgt   30  1666,327 ± 121,618  MB/sec
 stringFromBytes_CharsetName:·gc.alloc.rate.norm                   avgt   30   512,000 ±   0,001    B/op
 stringFromBytes_CharsetName:·gc.churn.PS_Eden_Space               avgt   30  1652,091 ± 189,687  MB/sec
 stringFromBytes_CharsetName:·gc.churn.PS_Eden_Space.norm          avgt   30   506,604 ±  42,841    B/op
 stringFromBytes_CharsetName:·gc.churn.PS_Survivor_Space           avgt   30     0,055 ±   0,021  MB/sec
 stringFromBytes_CharsetName:·gc.churn.PS_Survivor_Space.norm      avgt   30     0,017 ±   0,007    B/op
 stringFromBytes_CharsetName:·gc.count                             avgt   30   127,000            counts
 stringFromBytes_CharsetName:·gc.time                              avgt   30   217,000                ms

 * @author Michael Frank
 * @version 1.0 13.05.2018
 */

@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(3)
@BenchmarkMode({ Mode.AverageTime })
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Threads(16)
public class StringCharsetEncoderCacheJMH {

	@State(Scope.Thread)
	public static class ThreadState {
		private String stringData="löko3lö3laöskfjölaw3kr4j21öl5kjrfölskjfö2lqk3jrlkasjföl2k3jröl2kj5ölksdjfs23234l21l3j4lkjflksjlökjcv23lk4j";
		private byte[] byteData = stringData
				.getBytes(StandardCharsets.UTF_8);
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()//
				.include(".*" + StringCharsetEncoderCacheJMH.class.getSimpleName() + ".*")//
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
		return s.stringData.getBytes(StandardCharsets.UTF_8);	}

	@Benchmark
	public byte[] bytesFromString_CharsetName(ThreadState s) throws UnsupportedEncodingException {
		return s.stringData.getBytes(StandardCharsets.UTF_8.name());
	}
}
