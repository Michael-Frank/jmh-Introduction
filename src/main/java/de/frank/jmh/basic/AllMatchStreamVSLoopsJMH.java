package de.frank.jmh.basic;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;


/*--
forEach is slightly faster and has only 1/3 allocation pressure

Benchmark                         Score  gc.alloc.rate.norm
allMatch_forEach               52 ns/op    48 B/op
allMatch_forEach_specialized   47 ns/op    32 B/op
allMatch_stream                71 ns/op   152 B/op

*/
/**
 * @author Michael Frank
 * @version 1.0 13.05.2018
 */
@State(Scope.Thread)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class AllMatchStreamVSLoopsJMH {

	@org.openjdk.jmh.annotations.State(Scope.Thread)
	public static class State {
		Map<String, String> toVerify;
		List<String> required;

		public State() {
			Map<String, String> m = new HashMap<>();
			m.put("aaa", "abc");
			m.put("bbb", "abc");
			m.put("ccc", "abc");
			m.put("ddd", "abc");
			m.put("eee", "abc");
			m.put("fff", "abc");
			m.put("gggg", "abc");
			m.put("hhh", "abc");
			this.toVerify = m;
			this.required = Arrays.asList("bbb", "ccc", "ddd");
		}

	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()//
				.include(".*" + AllMatchStreamVSLoopsJMH.class.getSimpleName() + ".*")//
				.addProfiler(GCProfiler.class)//
				.build();
		new Runner(opt).run();
	}

	@Benchmark
	public boolean allMatch_stream(State state) {
		return state.required.stream() //
				.allMatch(credential -> StringUtils.hasText(state.toVerify.get(credential)));
	}
	
	@Benchmark
	public boolean allMatch_forEach(State state) {
		return allMatch(state.required, credential -> StringUtils.hasText(state.toVerify.get(credential)));
	}
	
	private static <T> boolean allMatch(List<T> required, Function<T, Boolean> matcher) {
		for (T t : required) {
			if (!matcher.apply(t)) {
				return false;
			}
		}
		return true; // all matched or empty list
	}

	@Benchmark
	public boolean allMatch_forEach_specialized(State state) {
		return mapContainsRequiredKeysValuesNotEmpty(state.required,state.toVerify);
	}

	private static  boolean mapContainsRequiredKeysValuesNotEmpty(List<String> requiredKeys, Map<String,String> map) {
		for (String key : requiredKeys) {
			if (!StringUtils.hasText(map.get(key))) {
				return false;
			}
		}
		return true; // all matched or empty list
	}

}
