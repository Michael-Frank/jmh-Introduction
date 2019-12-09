package de.frank.jmh.basic;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/*--
 * Benchmark               Mode  Cnt    Score    Error  Units
 * classGetName            avgt    5    4,059 ±  0,687  ns/op
 * staticName              avgt    5    4,617 ±  0,353  ns/op
 * classGetSimpleName      avgt    5  102,377 ± 18,462  ns/op *wow expensive!
 * classGetSimpleNameThis  avgt    5   96,000 ±  0,715  ns/op *wow expensive!
 */

/**
 * @author Michael Frank
 * @version 1.0 13.05.2018
 */
@State(Scope.Thread)
@Warmup(iterations = 3, time = 3, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class ClassGetSimpleNameIsSlowJMH {


    public Object obj = new ClassGetSimpleNameIsSlowJMH();

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()//
                .include(ClassGetSimpleNameIsSlowJMH.class.getName() + ".*")//
                .result(String.format("%s_%s.json",
                        DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                        ClassGetSimpleNameIsSlowJMH.class.getSimpleName()))
                .build();
        new Runner(opt).run();
    }

    @Benchmark
    public void noop() {

    }

    @Benchmark
    public String staticName() {
        return "ClassGetSimpleNameIsSlowJMH";
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

}
