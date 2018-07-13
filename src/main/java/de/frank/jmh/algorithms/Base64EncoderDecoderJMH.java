package de.frank.jmh.algorithms;

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
import org.springframework.util.Base64Utils;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/*--
 * ============================
 * 1 Thread benchmark
 * ============================
 * Result: 1 threaded - sun.misc en/decoders are ~15-20x slower then java.utilBase64
 * (win7-openjdk-1.8.0_121)
 *
 *                                      alloc     alloc      Eden      Eden Survivor Survivor       gc      gc                #
 *                                       rate rate.norm     total      norm    total     norm       gc    time     throughput #
 *                             unit->  MB/sec      B/op    MB/sec      B/op   MB/sec     B/op   counts      ms          ops/s #Comment
 * java8util_decode                     900,8     240,0     890,5     237,0     0,09     0,02    262,0   234,0    5.903.631,9 #winner if >= java8
 * java8util_encode                   1.173,3     192,0   1.181,7     193,4     0,08     0,01    211,0   218,0    9.612.305,5 #winner if >= java8
 * javax_xml_Datatyeconverter_decode    350,5      72,0     360,3      74,3     0,03     0,01     58,0    49,0    7.659.230,5 #winner if < java8
 * javax_xml_Datatyeconverter_encode  1.351,2     232,0   1.357,7     233,4     0,09     0,02    274,0   291,0    9.158.803,2 #winner if < java8
 * springBase64Utils_decode           1.188,8     368,0   1.193,0     369,0     0,07     0,02    160,0   214,0    5.080.874,8 #ok - but no point in using it - slower and more memory then javax or java8
 * springBase64Utils_encode           1.103,4     232,0   1.126,1     236,7     0,06     0,01    184,0   152,0    7.481.073,9 #ok - but no point in using it - slower and more memory then javax or java8
 * apacheCommonsBase64_decode         6.494,6   8.680,0   6.471,4   8.646,7     0,08     0,11    326,0   342,0    1.176.408,7 #BAD!
 * apacheCommonsBase64_encode         6.244,4   8.544,0   6.280,6   8.595,7     0,09     0,12    332,0   480,0    1.149.444,8 #BAD!
 * sunMisc_decode_New                   247,2     824,0     247,6     826,2     0,06     0,19    161,0   190,0      471.740,2 #BAD!
 * sunMisc_decode_Shared                227,1     784,0     224,2     774,6     0,08     0,27    206,0   189,0      455.591,5 #BAD!
 * sunMisc_encode_New                 6.728,4  25.336,0   6.753,5  25.419,1     0,10     0,37    277,0   476,0      417.641,1 #BAD!
 * sunMisc_encode_Shared              6.912,2  25.320,0   6.918,9  25.337,9     0,11     0,39    320,0   374,0      429.409,1 #BAD!
 *
 * ============================
 * 16 Thread benchmark
 * ============================
 * Result: under contention with 16 threads - sun.misc en/decoders are ~12-70x slower then java.utilBase64
 *
 * Benchmark                           Mode  Cnt     Score     Error  Units
 * java8util_decode                   thrpt   30  24901327 ±  732628  ops/s
 * java8util_encode                   thrpt   30  42275097 ± 1445483  ops/s
 * javax_xml_Datatyeconverter_decode  thrpt   30  31974184 ± 1620874  ops/s
 * javax_xml_Datatyeconverter_encode  thrpt   30  37667396 ± 1040602  ops/s
 * sunMisc_decode_New                 thrpt   30   1968543 ±   32656  ops/s
 * sunMisc_decode_Shared              thrpt   30   1790385 ±   22195  ops/s
 * sunMisc_encode_New                 thrpt   30    599063 ±    7482  ops/s
 * sunMisc_encode_Shared              thrpt   30    589989 ±   11087  ops/s
 *
 */
/**
 * @author Michael Frank
 * @version 1.0 13.05.2018
 */
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(3)
@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark) // Important to be Scope.Benchmark
@Threads(1)
public class Base64EncoderDecoderJMH {

	public BASE64Encoder sunEncoder = new BASE64Encoder();
	public BASE64Decoder sunDecoder = new BASE64Decoder();

	@State(Scope.Benchmark)
	public static class MyState {

		public static final SecureRandom RANDOM = getSecureRandom();

		public byte[] rawData = newRandomByteArray(32);
		public String base64 = DatatypeConverter.printBase64Binary(rawData);


		private static byte[] newRandomByteArray(int i) {
			byte[] data = new byte[i];
			RANDOM.nextBytes(data);
			return data;
		}

		private static SecureRandom getSecureRandom() {
			try {
				return SecureRandom.getInstance("SHA1PRNG");
			} catch (NoSuchAlgorithmException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()//
				.include(".*" + Base64EncoderDecoderJMH.class.getSimpleName() + ".*")//
				.addProfiler(GCProfiler.class)//
				.build();
		new Runner(opt).run();
	}

	@Benchmark
	public String sunMisc_encode_New(MyState state) {
		return new BASE64Encoder().encode(state.rawData);
	}

	@Benchmark
	public String sunMisc_encode_Shared(MyState state) {
		return sunEncoder.encode(state.rawData);
	}

	@Benchmark
	public byte[] sunMisc_decode_New(MyState state) throws IOException {
		return new BASE64Decoder().decodeBuffer(state.base64);
	}

	@Benchmark
	public byte[] sunMisc_decode_Shared(MyState state) throws IOException {
		return sunDecoder.decodeBuffer(state.base64);
	}


	@Benchmark
	public String javax_xml_Datatyeconverter_encode(MyState state) {
		return DatatypeConverter.printBase64Binary(state.rawData);
	}

	@Benchmark
	public byte[] javax_xml_Datatyeconverter_decode(MyState state) {
		return DatatypeConverter.parseBase64Binary(state.base64);
	}

	@Benchmark
	public String java8util_encode(MyState state) {
		return Base64.getEncoder().encodeToString(state.rawData);
	}

	@Benchmark
	public byte[] java8util_decode(MyState state) {
		return Base64.getDecoder().decode(state.base64);
	}

	@Benchmark
	public String springBase64Utils_encode(MyState state) {
		return Base64Utils.encodeToString(state.rawData);
	}

	@Benchmark
	public byte[] springBase64Utils_decode(MyState state) {
		return Base64Utils.decodeFromString(state.base64);
	}

	@Benchmark
	public String apacheCommonsBase64_encode(MyState state) {
		return org.apache.commons.codec.binary.Base64.encodeBase64String(state.rawData);
	}

	@Benchmark
	public byte[] apacheCommonsBase64_decode(MyState state) {
		return org.apache.commons.codec.binary.Base64.decodeBase64(state.base64);
	}


}
