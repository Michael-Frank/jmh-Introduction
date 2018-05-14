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
 * <p>
 * alloc     alloc      Eden      Eden Survivor Survivor       gc      gc                #
 * rate rate.norm     total      norm    total     norm       gc    time     throughput #
 * unit->  MB/sec     B/op    MB/sec      B/op   MB/sec     B/op   counts      ms          ops/s #Comment
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
 * <p>
 * RAW
 * # Run complete. Total time: 00:18:49
 * Benchmark                                                                                    Mode  Cnt        Score        Error   Units
 * apacheCommonsBase64_decode                                          thrpt   30  1176408,738 ±  30131,885   ops/s
 * apacheCommonsBase64_decode:·gc.alloc.rate                           thrpt   30     6494,603 ±    166,293  MB/sec
 * apacheCommonsBase64_decode:·gc.alloc.rate.norm                      thrpt   30     8680,001 ±      0,002    B/op
 * apacheCommonsBase64_decode:·gc.churn.PS_Eden_Space                  thrpt   30     6471,399 ±    287,409  MB/sec
 * apacheCommonsBase64_decode:·gc.churn.PS_Eden_Space.norm             thrpt   30     8646,726 ±    285,878    B/op
 * apacheCommonsBase64_decode:·gc.churn.PS_Survivor_Space              thrpt   30        0,084 ±      0,021  MB/sec
 * apacheCommonsBase64_decode:·gc.churn.PS_Survivor_Space.norm         thrpt   30        0,112 ±      0,028    B/op
 * apacheCommonsBase64_decode:·gc.count                                thrpt   30      326,000               counts
 * apacheCommonsBase64_decode:·gc.time                                 thrpt   30      342,000                   ms
 * apacheCommonsBase64_encode                                          thrpt   30  1149444,784 ±  37045,422   ops/s
 * apacheCommonsBase64_encode:·gc.alloc.rate                           thrpt   30     6244,400 ±    200,707  MB/sec
 * apacheCommonsBase64_encode:·gc.alloc.rate.norm                      thrpt   30     8544,002 ±      0,002    B/op
 * apacheCommonsBase64_encode:·gc.churn.PS_Eden_Space                  thrpt   30     6280,619 ±    301,006  MB/sec
 * apacheCommonsBase64_encode:·gc.churn.PS_Eden_Space.norm             thrpt   30     8595,675 ±    336,061    B/op
 * apacheCommonsBase64_encode:·gc.churn.PS_Survivor_Space              thrpt   30        0,090 ±      0,032  MB/sec
 * apacheCommonsBase64_encode:·gc.churn.PS_Survivor_Space.norm         thrpt   30        0,122 ±      0,043    B/op
 * apacheCommonsBase64_encode:·gc.count                                thrpt   30      332,000               counts
 * apacheCommonsBase64_encode:·gc.time                                 thrpt   30      480,000                   ms
 * java8util_decode                                                    thrpt   30  5903631,886 ± 124839,248   ops/s
 * java8util_decode:·gc.alloc.rate                                     thrpt   30      900,784 ±     19,064  MB/sec
 * java8util_decode:·gc.alloc.rate.norm                                thrpt   30      240,000 ±      0,001    B/op
 * java8util_decode:·gc.churn.PS_Eden_Space                            thrpt   30      890,534 ±     60,436  MB/sec
 * java8util_decode:·gc.churn.PS_Eden_Space.norm                       thrpt   30      237,024 ±     13,794    B/op
 * java8util_decode:·gc.churn.PS_Survivor_Space                        thrpt   30        0,089 ±      0,033  MB/sec
 * java8util_decode:·gc.churn.PS_Survivor_Space.norm                   thrpt   30        0,024 ±      0,009    B/op
 * java8util_decode:·gc.count                                          thrpt   30      262,000               counts
 * java8util_decode:·gc.time                                           thrpt   30      234,000                   ms
 * java8util_encode                                                    thrpt   30  9612305,476 ± 220981,958   ops/s
 * java8util_encode:·gc.alloc.rate                                     thrpt   30     1173,273 ±     26,984  MB/sec
 * java8util_encode:·gc.alloc.rate.norm                                thrpt   30      192,000 ±      0,001    B/op
 * java8util_encode:·gc.churn.PS_Eden_Space                            thrpt   30     1181,711 ±     81,771  MB/sec
 * java8util_encode:·gc.churn.PS_Eden_Space.norm                       thrpt   30      193,404 ±     12,663    B/op
 * java8util_encode:·gc.churn.PS_Survivor_Space                        thrpt   30        0,075 ±      0,029  MB/sec
 * java8util_encode:·gc.churn.PS_Survivor_Space.norm                   thrpt   30        0,012 ±      0,005    B/op
 * java8util_encode:·gc.count                                          thrpt   30      211,000               counts
 * java8util_encode:·gc.time                                           thrpt   30      218,000                   ms
 * javax_xml_Datatyeconverter_decode                                   thrpt   30  7659230,514 ± 471490,259   ops/s
 * javax_xml_Datatyeconverter_decode:·gc.alloc.rate                    thrpt   30      350,477 ±     21,749  MB/sec
 * javax_xml_Datatyeconverter_decode:·gc.alloc.rate.norm               thrpt   30       72,000 ±      0,001    B/op
 * javax_xml_Datatyeconverter_decode:·gc.churn.PS_Eden_Space           thrpt   30      360,302 ±     59,795  MB/sec
 * javax_xml_Datatyeconverter_decode:·gc.churn.PS_Eden_Space.norm      thrpt   30       74,257 ±     11,737    B/op
 * javax_xml_Datatyeconverter_decode:·gc.churn.PS_Survivor_Space       thrpt   30        0,032 ±      0,017  MB/sec
 * javax_xml_Datatyeconverter_decode:·gc.churn.PS_Survivor_Space.norm  thrpt   30        0,007 ±      0,003    B/op
 * javax_xml_Datatyeconverter_decode:·gc.count                         thrpt   30       58,000               counts
 * javax_xml_Datatyeconverter_decode:·gc.time                          thrpt   30       49,000                   ms
 * javax_xml_Datatyeconverter_encode                                   thrpt   30  9158803,189 ± 495140,896   ops/s
 * javax_xml_Datatyeconverter_encode:·gc.alloc.rate                    thrpt   30     1351,231 ±     73,306  MB/sec
 * javax_xml_Datatyeconverter_encode:·gc.alloc.rate.norm               thrpt   30      232,000 ±      0,001    B/op
 * javax_xml_Datatyeconverter_encode:·gc.churn.PS_Eden_Space           thrpt   30     1357,677 ±     72,786  MB/sec
 * javax_xml_Datatyeconverter_encode:·gc.churn.PS_Eden_Space.norm      thrpt   30      233,407 ±      7,243    B/op
 * javax_xml_Datatyeconverter_encode:·gc.churn.PS_Survivor_Space       thrpt   30        0,086 ±      0,025  MB/sec
 * javax_xml_Datatyeconverter_encode:·gc.churn.PS_Survivor_Space.norm  thrpt   30        0,015 ±      0,004    B/op
 * javax_xml_Datatyeconverter_encode:·gc.count                         thrpt   30      274,000               counts
 * javax_xml_Datatyeconverter_encode:·gc.time                          thrpt   30      291,000                   ms
 * springBase64Utils_decode                                            thrpt   30  5080874,829 ± 271818,921   ops/s
 * springBase64Utils_decode:·gc.alloc.rate                             thrpt   30     1188,790 ±     63,622  MB/sec
 * springBase64Utils_decode:·gc.alloc.rate.norm                        thrpt   30      368,000 ±      0,001    B/op
 * springBase64Utils_decode:·gc.churn.PS_Eden_Space                    thrpt   30     1192,999 ±    140,973  MB/sec
 * springBase64Utils_decode:·gc.churn.PS_Eden_Space.norm               thrpt   30      369,042 ±     37,058    B/op
 * springBase64Utils_decode:·gc.churn.PS_Survivor_Space                thrpt   30        0,066 ±      0,020  MB/sec
 * springBase64Utils_decode:·gc.churn.PS_Survivor_Space.norm           thrpt   30        0,021 ±      0,006    B/op
 * springBase64Utils_decode:·gc.count                                  thrpt   30      160,000               counts
 * springBase64Utils_decode:·gc.time                                   thrpt   30      214,000                   ms
 * springBase64Utils_encode                                            thrpt   30  7481073,916 ± 174164,409   ops/s
 * springBase64Utils_encode:·gc.alloc.rate                             thrpt   30     1103,437 ±     25,724  MB/sec
 * springBase64Utils_encode:·gc.alloc.rate.norm                        thrpt   30      232,000 ±      0,001    B/op
 * springBase64Utils_encode:·gc.churn.PS_Eden_Space                    thrpt   30     1126,065 ±     78,470  MB/sec
 * springBase64Utils_encode:·gc.churn.PS_Eden_Space.norm               thrpt   30      236,731 ±     15,253    B/op
 * springBase64Utils_encode:·gc.churn.PS_Survivor_Space                thrpt   30        0,062 ±      0,026  MB/sec
 * springBase64Utils_encode:·gc.churn.PS_Survivor_Space.norm           thrpt   30        0,013 ±      0,005    B/op
 * springBase64Utils_encode:·gc.count                                  thrpt   30      184,000               counts
 * springBase64Utils_encode:·gc.time                                   thrpt   30      152,000                   ms
 * sunMisc_decode_New                                                  thrpt   30   471740,209 ±  20009,663   ops/s
 * sunMisc_decode_New:·gc.alloc.rate                                   thrpt   30      247,190 ±     10,511  MB/sec
 * sunMisc_decode_New:·gc.alloc.rate.norm                              thrpt   30      824,004 ±      0,006    B/op
 * sunMisc_decode_New:·gc.churn.PS_Eden_Space                          thrpt   30      247,632 ±     31,771  MB/sec
 * sunMisc_decode_New:·gc.churn.PS_Eden_Space.norm                     thrpt   30      826,186 ±    101,835    B/op
 * sunMisc_decode_New:·gc.churn.PS_Survivor_Space                      thrpt   30        0,058 ±      0,024  MB/sec
 * sunMisc_decode_New:·gc.churn.PS_Survivor_Space.norm                 thrpt   30        0,193 ±      0,081    B/op
 * sunMisc_decode_New:·gc.count                                        thrpt   30      161,000               counts
 * sunMisc_decode_New:·gc.time                                         thrpt   30      190,000                   ms
 * sunMisc_decode_Shared                                               thrpt   30   455591,541 ±  13323,007   ops/s
 * sunMisc_decode_Shared:·gc.alloc.rate                                thrpt   30      227,081 ±      6,631  MB/sec
 * sunMisc_decode_Shared:·gc.alloc.rate.norm                           thrpt   30      784,004 ±      0,006    B/op
 * sunMisc_decode_Shared:·gc.churn.PS_Eden_Space                       thrpt   30      224,158 ±     12,031  MB/sec
 * sunMisc_decode_Shared:·gc.churn.PS_Eden_Space.norm                  thrpt   30      774,566 ±     41,097    B/op
 * sunMisc_decode_Shared:·gc.churn.PS_Survivor_Space                   thrpt   30        0,078 ±      0,019  MB/sec
 * sunMisc_decode_Shared:·gc.churn.PS_Survivor_Space.norm              thrpt   30        0,271 ±      0,065    B/op
 * sunMisc_decode_Shared:·gc.count                                     thrpt   30      206,000               counts
 * sunMisc_decode_Shared:·gc.time                                      thrpt   30      189,000                   ms
 * sunMisc_encode_New                                                  thrpt   30   417641,108 ±  14716,609   ops/s
 * sunMisc_encode_New:·gc.alloc.rate                                   thrpt   30     6728,447 ±    238,234  MB/sec
 * sunMisc_encode_New:·gc.alloc.rate.norm                              thrpt   30    25336,004 ±      0,006    B/op
 * sunMisc_encode_New:·gc.churn.PS_Eden_Space                          thrpt   30     6753,520 ±    330,963  MB/sec
 * sunMisc_encode_New:·gc.churn.PS_Eden_Space.norm                     thrpt   30    25419,090 ±    706,806    B/op
 * sunMisc_encode_New:·gc.churn.PS_Survivor_Space                      thrpt   30        0,097 ±      0,034  MB/sec
 * sunMisc_encode_New:·gc.churn.PS_Survivor_Space.norm                 thrpt   30        0,368 ±      0,127    B/op
 * sunMisc_encode_New:·gc.count                                        thrpt   30      277,000               counts
 * sunMisc_encode_New:·gc.time                                         thrpt   30      476,000                   ms
 * sunMisc_encode_Shared                                               thrpt   30   429409,128 ±  14102,286   ops/s
 * sunMisc_encode_Shared:·gc.alloc.rate                                thrpt   30     6912,155 ±    227,193  MB/sec
 * sunMisc_encode_Shared:·gc.alloc.rate.norm                           thrpt   30    25320,004 ±      0,006    B/op
 * sunMisc_encode_Shared:·gc.churn.PS_Eden_Space                       thrpt   30     6918,856 ±    296,661  MB/sec
 * sunMisc_encode_Shared:·gc.churn.PS_Eden_Space.norm                  thrpt   30    25337,894 ±    566,604    B/op
 * sunMisc_encode_Shared:·gc.churn.PS_Survivor_Space                   thrpt   30        0,105 ±      0,025  MB/sec
 * sunMisc_encode_Shared:·gc.churn.PS_Survivor_Space.norm              thrpt   30        0,385 ±      0,092    B/op
 * sunMisc_encode_Shared:·gc.count                                     thrpt   30      320,000               counts
 * sunMisc_encode_Shared:·gc.time                                      thrpt   30      374,000                   ms
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
