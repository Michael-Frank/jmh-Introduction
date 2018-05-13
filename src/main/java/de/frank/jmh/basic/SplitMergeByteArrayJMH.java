package de.frank.jmh.basic;

import org.apache.commons.lang.ArrayUtils;
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
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**--
 * RAW
 * Benchmark                                                     Mode  Cnt         Score         Error   Units
 * join_ByteArrayStream                                         thrpt   30  18565976,719 ±  466973,000   ops/s
 * join_ByteArrayStream:·gc.alloc.rate                          thrpt   30      2455,633 ±      61,638  MB/sec
 * join_ByteArrayStream:·gc.alloc.rate.norm                     thrpt   30       208,000 ±       0,001    B/op
 * join_ByteArrayStream:·gc.churn.PS_Eden_Space                 thrpt   30      2457,825 ±      94,855  MB/sec
 * join_ByteArrayStream:·gc.churn.PS_Eden_Space.norm            thrpt   30       208,301 ±       7,600    B/op
 * join_ByteArrayStream:·gc.churn.PS_Survivor_Space             thrpt   30         0,094 ±       0,024  MB/sec
 * join_ByteArrayStream:·gc.churn.PS_Survivor_Space.norm        thrpt   30         0,008 ±       0,002    B/op
 * join_ByteArrayStream:·gc.count                               thrpt   30       319,000                counts
 * join_ByteArrayStream:·gc.time                                thrpt   30       239,000                    ms
 * join_arraysCopyOf                                            thrpt   30  58165499,912 ± 1156239,879   ops/s
 * join_arraysCopyOf:·gc.alloc.rate                             thrpt   30      1774,954 ±      35,290  MB/sec
 * join_arraysCopyOf:·gc.alloc.rate.norm                        thrpt   30        48,000 ±       0,001    B/op
 * join_arraysCopyOf:·gc.churn.PS_Eden_Space                    thrpt   30      1774,120 ±     106,497  MB/sec
 * join_arraysCopyOf:·gc.churn.PS_Eden_Space.norm               thrpt   30        47,949 ±       2,496    B/op
 * join_arraysCopyOf:·gc.churn.PS_Survivor_Space                thrpt   30         0,079 ±       0,028  MB/sec
 * join_arraysCopyOf:·gc.churn.PS_Survivor_Space.norm           thrpt   30         0,002 ±       0,001    B/op
 * join_arraysCopyOf:·gc.count                                  thrpt   30       329,000                counts
 * join_arraysCopyOf:·gc.time                                   thrpt   30       249,000                    ms
 * join_commonsArrayUtils                                       thrpt   30  30516040,325 ±  731117,638   ops/s
 * join_commonsArrayUtils:·gc.alloc.rate                        thrpt   30      1552,091 ±      37,235  MB/sec
 * join_commonsArrayUtils:·gc.alloc.rate.norm                   thrpt   30        80,000 ±       0,001    B/op
 * join_commonsArrayUtils:·gc.churn.PS_Eden_Space               thrpt   30      1546,641 ±      49,645  MB/sec
 * join_commonsArrayUtils:·gc.churn.PS_Eden_Space.norm          thrpt   30        79,714 ±       1,610    B/op
 * join_commonsArrayUtils:·gc.churn.PS_Survivor_Space           thrpt   30         0,071 ±       0,022  MB/sec
 * join_commonsArrayUtils:·gc.churn.PS_Survivor_Space.norm      thrpt   30         0,004 ±       0,001    B/op
 * join_commonsArrayUtils:·gc.count                             thrpt   30       388,000                counts
 * join_commonsArrayUtils:·gc.time                              thrpt   30       299,000                    ms
 * join_systemArrayCopy                                         thrpt   30  50576451,371 ± 2115135,542   ops/s
 * join_systemArrayCopy:·gc.alloc.rate                          thrpt   30      1542,669 ±      64,911  MB/sec
 * join_systemArrayCopy:·gc.alloc.rate.norm                     thrpt   30        48,000 ±       0,001    B/op
 * join_systemArrayCopy:·gc.churn.PS_Eden_Space                 thrpt   30      1524,019 ±     144,938  MB/sec
 * join_systemArrayCopy:·gc.churn.PS_Eden_Space.norm            thrpt   30        47,461 ±       4,487    B/op
 * join_systemArrayCopy:·gc.churn.PS_Survivor_Space             thrpt   30         0,060 ±       0,022  MB/sec
 * join_systemArrayCopy:·gc.churn.PS_Survivor_Space.norm        thrpt   30         0,002 ±       0,001    B/op
 * join_systemArrayCopy:·gc.count                               thrpt   30       168,000                counts
 * join_systemArrayCopy:·gc.time                                thrpt   30       334,000                    ms
 * split_arraysCopyOf                                           thrpt   30  49391116,090 ± 3237547,069   ops/s
 * split_arraysCopyOf:·gc.alloc.rate                            thrpt   30      2009,306 ±     131,860  MB/sec
 * split_arraysCopyOf:·gc.alloc.rate.norm                       thrpt   30        64,000 ±       0,001    B/op
 * split_arraysCopyOf:·gc.churn.PS_Eden_Space                   thrpt   30      1995,312 ±     223,191  MB/sec
 * split_arraysCopyOf:·gc.churn.PS_Eden_Space.norm              thrpt   30        63,521 ±       5,910    B/op
 * split_arraysCopyOf:·gc.churn.PS_Survivor_Space               thrpt   30         0,071 ±       0,026  MB/sec
 * split_arraysCopyOf:·gc.churn.PS_Survivor_Space.norm          thrpt   30         0,002 ±       0,001    B/op
 * split_arraysCopyOf:·gc.count                                 thrpt   30       206,000                counts
 * split_arraysCopyOf:·gc.time                                  thrpt   30       308,000                    ms
 * split_byteArrayInputStream                                   thrpt   30  38444425,700 ± 1023273,536   ops/s
 * split_byteArrayInputStream:·gc.alloc.rate                    thrpt   30      1564,210 ±      41,648  MB/sec
 * split_byteArrayInputStream:·gc.alloc.rate.norm               thrpt   30        64,000 ±       0,001    B/op
 * split_byteArrayInputStream:·gc.churn.PS_Eden_Space           thrpt   30      1555,022 ±      80,154  MB/sec
 * split_byteArrayInputStream:·gc.churn.PS_Eden_Space.norm      thrpt   30        63,595 ±       2,567    B/op
 * split_byteArrayInputStream:·gc.churn.PS_Survivor_Space       thrpt   30         0,097 ±       0,031  MB/sec
 * split_byteArrayInputStream:·gc.churn.PS_Survivor_Space.norm  thrpt   30         0,004 ±       0,001    B/op
 * split_byteArrayInputStream:·gc.count                         thrpt   30       313,000                counts
 * split_byteArrayInputStream:·gc.time                          thrpt   30       249,000                    ms
 * split_byteBuffer                                             thrpt   30  42570929,071 ± 1086202,979   ops/s
 * split_byteBuffer:·gc.alloc.rate                              thrpt   30      1732,173 ±      44,267  MB/sec
 * split_byteBuffer:·gc.alloc.rate.norm                         thrpt   30        64,000 ±       0,001    B/op
 * split_byteBuffer:·gc.churn.PS_Eden_Space                     thrpt   30      1729,955 ±      78,268  MB/sec
 * split_byteBuffer:·gc.churn.PS_Eden_Space.norm                thrpt   30        63,968 ±       2,897    B/op
 * split_byteBuffer:·gc.churn.PS_Survivor_Space                 thrpt   30         0,082 ±       0,024  MB/sec
 * split_byteBuffer:·gc.churn.PS_Survivor_Space.norm            thrpt   30         0,003 ±       0,001    B/op
 * split_byteBuffer:·gc.count                                   thrpt   30       279,000                counts
 * split_byteBuffer:·gc.time                                    thrpt   30       223,000                    ms
 * split_commonsArrayUtils                                      thrpt   30  57953003,513 ± 1562229,555   ops/s
 * split_commonsArrayUtils:·gc.alloc.rate                       thrpt   30      2357,943 ±      63,582  MB/sec
 * split_commonsArrayUtils:·gc.alloc.rate.norm                  thrpt   30        64,000 ±       0,001    B/op
 * split_commonsArrayUtils:·gc.churn.PS_Eden_Space              thrpt   30      2363,225 ±     151,557  MB/sec
 * split_commonsArrayUtils:·gc.churn.PS_Eden_Space.norm         thrpt   30        64,135 ±       3,646    B/op
 * split_commonsArrayUtils:·gc.churn.PS_Survivor_Space          thrpt   30         0,087 ±       0,022  MB/sec
 * split_commonsArrayUtils:·gc.churn.PS_Survivor_Space.norm     thrpt   30         0,002 ±       0,001    B/op
 * split_commonsArrayUtils:·gc.count                            thrpt   30       288,000                counts
 * split_commonsArrayUtils:·gc.time                             thrpt   30       206,000                    ms
 * split_systemArrayCopy                                        thrpt   30  44296562,328 ± 3074640,627   ops/s
 * split_systemArrayCopy:·gc.alloc.rate                         thrpt   30      1802,654 ±     124,517  MB/sec
 * split_systemArrayCopy:·gc.alloc.rate.norm                    thrpt   30        64,000 ±       0,001    B/op
 * split_systemArrayCopy:·gc.churn.PS_Eden_Space                thrpt   30      1837,164 ±     240,725  MB/sec
 * split_systemArrayCopy:·gc.churn.PS_Eden_Space.norm           thrpt   30        65,281 ±       8,015    B/op
 * split_systemArrayCopy:·gc.churn.PS_Survivor_Space            thrpt   30         0,053 ±       0,023  MB/sec
 * split_systemArrayCopy:·gc.churn.PS_Survivor_Space.norm       thrpt   30         0,002 ±       0,001    B/op
 * split_systemArrayCopy:·gc.count                              thrpt   30       115,000                counts
 * split_systemArrayCopy:·gc.time                               thrpt   30       273,000                    ms
 *
 * @author Michael Frank
 * @version 1.0 13.05.2018
 */
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(3)
@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark) // Important to be Scope.Benchmark
@Threads(1)
public class SplitMergeByteArrayJMH {

	@State(Scope.Thread)
	public static class MyState {

		public static final SecureRandom RANDOM = getSecureRandom();

		public byte[] data32 = newRandomByteArray(32);
		public byte[] data16_a = newRandomByteArray(16);
		public byte[] data16_b = newRandomByteArray(16);


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
				.include(".*" + SplitMergeByteArrayJMH.class.getSimpleName() + ".*")//
				//.addProfiler(GCProfiler.class)//
				.build();
		new Runner(opt).run();
	}

	@Benchmark
	public void split_byteBuffer(Blackhole bh, MyState state) {
		byte[] l = new byte[16];
		byte[] r = new byte[16];
		ByteBuffer bb = ByteBuffer.wrap(state.data32);
		bb.get(l);
		bb.get(r);
		bh.consume(l);
		bh.consume(r);
	}

	@Benchmark
	public void split_arraysCopyOf(Blackhole bh, MyState state) {
		byte[] l = Arrays.copyOfRange(state.data32, 0, 16);
		byte[] r = Arrays.copyOfRange(state.data32, 16, 32);

		bh.consume(l);
		bh.consume(r);
	}

	@Benchmark
	public void split_systemArrayCopy(Blackhole bh, MyState state) {
		byte[] l = new byte[16];
		byte[] r = new byte[16];
		System.arraycopy(state.data32, 0, l, 0, 16);
		System.arraycopy(state.data32, 16, r, 0, 16);

		bh.consume(l);
		bh.consume(r);
	}

	@Benchmark
	public void split_systemArrayCopy_dynSized(Blackhole bh, MyState state) {
		byte[] l = new byte[16];
		byte[] r = new byte[16];
		System.arraycopy(state.data32, 0, l, 0, l.length);
		System.arraycopy(state.data32, l.length, r, 0, r.length);

		bh.consume(l);
		bh.consume(r);
	}

	@Benchmark
	public void split_byteArrayInputStream(Blackhole bh, MyState state) throws IOException {
		ByteArrayInputStream in = new ByteArrayInputStream(state.data32);
		byte[] l = new byte[16];
		byte[] r = new byte[16];
		in.read(l);
		in.read(r);
		bh.consume(l);
		bh.consume(r);
	}

	@Benchmark
	public void split_commonsArrayUtils(Blackhole bh, MyState state) {
		byte[] l = ArrayUtils.subarray(state.data32, 0, 16);
		byte[] r = ArrayUtils.subarray(state.data32, 16, 32);

		bh.consume(l);
		bh.consume(r);
	}


	@Benchmark
	public void join_byteBuffer(Blackhole bh, MyState state) {
		ByteBuffer bb = ByteBuffer.allocate(32);
		bb.put(state.data16_a);
		bb.put(state.data16_b);
		bh.consume(bb.array());
	}


	@Benchmark
	public void join_byteBuffer_dynSized(Blackhole bh, MyState state) {
		ByteBuffer bb = ByteBuffer.allocate(state.data16_a.length + state.data16_b.length);
		bb.put(state.data16_a);
		bb.put(state.data16_b);
		bh.consume(bb.array());
	}

	@Benchmark
	public void join_arraysCopyOf(Blackhole bh, MyState state) {
		byte[] res = Arrays.copyOfRange(state.data16_a, 0, 32);
		System.arraycopy(state.data16_b, 0, res, 16, 16);
		bh.consume(res);
	}

	@Benchmark
	public void join_systemArrayCopy(Blackhole bh, MyState state) {
		byte[] res = new byte[32];
		System.arraycopy(state.data16_a, 0, res, 0, 16);
		System.arraycopy(state.data16_b, 0, res, 16, 16);
		bh.consume(res);
	}


	@Benchmark
	public void join_systemArrayCopy_dynSized(Blackhole bh, MyState state) {
		byte[] res = new byte[state.data16_a.length + state.data16_b.length];
		System.arraycopy(state.data16_a, 0, res, 0, state.data16_a.length);
		System.arraycopy(state.data16_b, 0, res, state.data16_a.length, state.data16_b.length);
		bh.consume(res);
	}

	@Benchmark
	public void join_byteArrayOutputStream(Blackhole bh, MyState state) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream(32);
		bos.write(state.data16_a);
		bos.write(state.data16_a);
		bh.consume(bos.toByteArray());
	}

	@Benchmark
	public void join_commonsArrayUtils(Blackhole bh, MyState state) {
		bh.consume(ArrayUtils.addAll(state.data16_a, state.data16_b));
	}
}
