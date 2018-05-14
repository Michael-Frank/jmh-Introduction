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
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

/*--
 * Benchmark               Mode  Cnt    Score    Error  Units
 * classGetName            avgt    5    4,059 ±  0,687  ns/op
 * classGetSimpleName      avgt    5  102,377 ± 18,462  ns/op *wow expensive!
 * classGetSimpleNameThis  avgt    5   96,000 ±  0,715  ns/op *wow expensive!
 * staticName              avgt    5    4,617 ±  0,353  ns/op
 */
/**
* @author Michael Frank
* @version 1.0 13.05.2018
*/
@State(Scope.Thread)
@Warmup(iterations = 3, time = 3, timeUnit = TimeUnit.NANOSECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.NANOSECONDS)
@Fork(1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class ClassNameJMH {


	public Object obj = new ClassNameJMH();


	@Benchmark
	public void noop() {

	}

	@Benchmark
	public String staticName() {
		return "ClassNameJMH";
	}

	@Benchmark
	public String classGetSimpleName() {
		return obj.getClass().getSimpleName();
	}

	@Benchmark
	public String classGetName() {
		return obj.getClass().getName();
	}

	@Benchmark
	public String classGetSimpleNameThis() {
		return this.getClass().getSimpleName();
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()//
				.include(".*" + ClassNameJMH.class.getSimpleName() + ".*")//
				.build();
		new Runner(opt).run();
	}
}
