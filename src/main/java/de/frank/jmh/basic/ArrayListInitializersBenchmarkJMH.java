package de.frank.jmh.basic;

import com.google.common.collect.Lists;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*--
 * This benchmark is biased and only valid if:
 *   - initialize ArrayList with a SINGLE item
 *   - AND there is never a second "add()" sometimes later.
 *
 * If there is more the one initial value, Arrays.asList might win.
 * If there is a second "add()" sometimes later we can expect most variants to behave similarly, except for simpleAddWithInitialCapacity1 and an higher capacity
 *
 * Benchmark                      Mode  Cnt   Score   Error  Units
 * baseline                       avgt    5   0,302 ± 0,048  ns/op NO-OP
 * baselineNewArrayList           avgt    5   4,943 ± 0,326  ns/op will contain an static empty object array - final capacity is 0
 * baselineNewArrayList1          avgt    5   8,076 ± 0,182  ns/op
 * arraysAsList                   avgt    5  17,455 ± 0,780  ns/op will copy data32 TWICE- final capacity is 1
 * collectionsSingleton           avgt    5   9,126 ± 1,038  ns/op will copy data32 Once - final capacity is 1
 * collectionsSingletonList       avgt    5   9,199 ± 1,368  ns/op will copy data32 Once - final capacity is 1
 * initializerAdd                 avgt    5  11,640 ± 1,313  ns/op will copy data32 Once - final capacity is 1
 * simpleAdd                      avgt    5  10,482 ± 0,694  ns/op issues an ArrayCopy with defaultSize 10 on first add()- final capacity is 10
 * simpleAddWithInitialCapacity1  avgt    5   9,543 ± 0,441  ns/op initialize with size 1
 * guava                          avgt    5  10,883 ± 1,890  ns/op internal: new ArrayList<>();Collections.addAll
 * java8Stream                    avgt    5  52,589 ± 1,167  ns/op as expected: sucks
 * java8Stream2                   avgt    5  44,616 ± 0,479  ns/op as expected: sucks
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
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class ArrayListInitializersBenchmarkJMH {


    @Benchmark
    public void baseline() {
        //baseline
    }

    @Benchmark
    public ArrayList<String> baselineNewArrayList() {
        return new ArrayList<>();
    }

    @Benchmark
    public ArrayList<String> baselineNewArrayList1() {
        return new ArrayList<>(1);
    }

    @Benchmark
    public ArrayList<String> arraysAsList() {
        return new ArrayList<>(Arrays.asList("someValue"));
    }

    @Benchmark
    public ArrayList<String> collectionsSingletonList() {
        return new ArrayList<>(Collections.singletonList("someValue"));
    }

    @Benchmark
    public ArrayList<String> collectionsSingleton() {
        return new ArrayList<>(Collections.singleton("someValue"));
    }

    @Benchmark
    public ArrayList<String> simpleAdd() {
        ArrayList a = new ArrayList<>();
        a.add("someValue");
        return a;
    }

    @Benchmark
    public ArrayList<String> simpleAddWithInitialCapacity1() {
        ArrayList a = new ArrayList<>(1);
        a.add("someValue");
        return a;
    }

    @Benchmark
    public ArrayList<String> initializerAdd() {
        return new ArrayList<String>() {
            {
                add("someValue");
            }
        };
    }

    @Benchmark
    public ArrayList<String> java8Stream() {
        return Arrays.stream(new String[]{"someValue"}).collect(Collectors.toCollection(ArrayList::new));
    }

    @Benchmark
    public List<String> java8Stream_v2() {
        return Stream.of("someValue").collect(Collectors.toCollection(ArrayList::new));
    }

    @Benchmark
    public ArrayList<String> guava() {
        return Lists.newArrayList("someValue");
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()//
                .include(ArrayListInitializersBenchmarkJMH.class.getName())
                .result(String.format("%s_%s.json",
                        DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                        ArrayListInitializersBenchmarkJMH.class.getSimpleName()))
                .forks(1)
                .build();
        new Runner(opt).run();
    }
}
