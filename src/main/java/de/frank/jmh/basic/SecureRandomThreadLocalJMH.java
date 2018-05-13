package de.frank.jmh.basic;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
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
 * @author Michael Frank
 * @version 1.0 13.05.2018
 */
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(3)
@BenchmarkMode({ Mode.Throughput })
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark) // Important to be Scope.Benchmark
@Threads(16)
public class SecureRandomThreadLocalJMH {

	@State(Scope.Benchmark)
	public static class MyState {

		public static final SecureRandom ONE_FOR_ALL_SECURE_RANDOM = getSecureRandom();
		public static final ThreadLocal<SecureRandom> TL_SECURE_RANDOM = new ThreadLocal<SecureRandom>() {
			@Override
			protected SecureRandom initialValue() {
				return getSecureRandom();
			}

		};


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
				.include(".*" + SecureRandomThreadLocalJMH.class.getSimpleName() + ".*")//
				.addProfiler(GCProfiler.class).build();
		new Runner(opt).run();
	}

	@Benchmark
	public byte[] shared(MyState state) {
		byte[] r = new byte[128];
		state.ONE_FOR_ALL_SECURE_RANDOM.nextBytes(r);
		return r;
	}

	@Benchmark
	public byte[] threadLocal(MyState state) {
		byte[] r = new byte[128];
		state.TL_SECURE_RANDOM.get().nextBytes(r);
		return r;
	}

}
