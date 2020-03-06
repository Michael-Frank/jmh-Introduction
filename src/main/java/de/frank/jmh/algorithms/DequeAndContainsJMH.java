package de.frank.jmh.algorithms;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(3)
@State(Scope.Thread)
public class DequeAndContainsJMH {

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(DequeAndContainsJMH.class.getName())
                .result(String.format("%s_%s.json",
                        DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                        DequeAndContainsJMH.class.getSimpleName()))
                .build();
        new Runner(opt).run();
    }

    List<String> in = Arrays.asList("foo", "bar", "", "a", null, "b", "c", "", "", "d", null, null);
    Deque<String> deque = new ArrayDeque<>(in);


}
