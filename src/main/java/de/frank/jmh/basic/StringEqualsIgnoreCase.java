package de.frank.jmh.basic;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.results.format.*;
import org.openjdk.jmh.runner.*;
import org.openjdk.jmh.runner.options.*;

import java.time.*;
import java.time.format.*;
import java.util.concurrent.*;

/*--
 * Is there a difference between
 * - "fooo".equalsIgnoreCase("FoOo");
 * - "FOOO".equalsIgnoreCase("FoOo");
 * - "fOOo".equalsIgnoreCase("FoOo");
 * => NO!
 *
 * Benchmark                           Mode  Cnt  Score   Error  Units
 * StringEqualsIgnoreCase.lower_mixed  avgt   15  7,978 ± 0,166  ns/op
 * StringEqualsIgnoreCase.mixed_mixed  avgt   15  7,938 ± 0,068  ns/op
 * StringEqualsIgnoreCase.upper_mixed  avgt   15  7,977 ± 0,180  ns/op
 *
 * BUT.. if your right side has a preference to be mostly one specific case, chose the same for the left side.
 */

/**
 * @author Michael Frank
 * @version 1.0 13.05.2018
 */
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(3)
@BenchmarkMode({Mode.AverageTime})
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Threads(1)
public class StringEqualsIgnoreCase {


    String mixed = "01234567890abcdefghijklmnopqrstuvwxyzäöüABCEDFGHIJKLMNOPQRSTUVWXYZÄÖÜ";
    String lower = mixed.toLowerCase();
    String upper = mixed.toUpperCase();

    //make sure to get a new string references or else the equalsIgnoreCase will short circuit on a==b without actually comparing
    String mixed2 = new String(mixed);
    String lower2 = new String(lower);
    String upper2 = new String(upper);


    String mixedFoo = "FoOo";

    @Benchmark
    public boolean foo_lower_mixed() {
        return "fooo" .equalsIgnoreCase(mixedFoo);
    }

    @Benchmark
    public boolean foo_upper_mixed() {
        return "FOOO" .equalsIgnoreCase(mixedFoo);
    }

    @Benchmark
    public boolean foo_mixed_mixed() {
        return "fOOo" .equalsIgnoreCase(mixedFoo);
    }

    @Benchmark
    public boolean mixed_mixed() {
        return mixed.equalsIgnoreCase(mixed2);
    }

    @Benchmark
    public boolean lower_lower() {
        return lower.equalsIgnoreCase(lower2);
    }

    @Benchmark
    public boolean upper_upper() {
        return upper.equalsIgnoreCase(upper2);
    }


    @Benchmark
    public boolean upper_lower() {
        return upper.equalsIgnoreCase(lower2);
    }

    @Benchmark
    public boolean lower_upper() {
        return lower.equalsIgnoreCase(upper2);
    }

    @Benchmark
    public boolean upper_mixed() {
        return upper.equalsIgnoreCase(mixed2);
    }

    @Benchmark
    public boolean lower_mixed() {
        return lower.equalsIgnoreCase(mixed2);
    }

    public static void main(String[] args) throws RunnerException {
        new Runner(new OptionsBuilder()//
                                       .include(StringEqualsIgnoreCase.class.getName() + ".*")//
                                       //.addProfiler(GCProfiler.class)//
                                       .resultFormat(ResultFormatType.JSON)
                                       .result(String.format("%s_%s.json",
                                                             DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                                                             StringEqualsIgnoreCase.class.getSimpleName()))
                                       .build()).run();
    }

}
