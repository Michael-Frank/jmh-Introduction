package de.frank.jmh.basic;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

/*--

Results in ns/op        |    999 999999 Int.Max Long.max # <-inputSize
customLog10                  2,1    2,3     2,9          # may look ugly but fast :)
customLog10pow               2,1    2,3     2,8          # may look ugly but very fast :)
listCustomLog10              2,3    2,8     3,7          # list version is a tick slower - for a native long impl it could be better
mathLog10                   21,2   21,3    20,7          # precession (double) is expensive
mathLog10Pow                23,2   72,2    71,1          # two expensive opts add up
mathLogDivideLog10          24,6   24,8    23,9          # not surprisingly, log(x)/log(10) is a little bit slower then the dedicated log10(x) function
mathLogDivideLog10Cached    24,0   24,2    24,2          # java can figure this out by itself - no need to introduce a constant
multLoopCustomLog10          2,1    2,4     4,9          #
------------------------------------------------------------------------------
longListCustomLog10MaxL      2,1    2,4     2,8      3,7 #
longMultLoopCustomLog10MaxL  2,6    2,3     4,9      4,9 #

 */
@State(Scope.Thread)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class IntegerPow10JMH {

    //small, medium, large
    //   @Param(value = {/*"999", "999999",*/ "" + Integer.MAX_VALUE})
    int max;

    @Param(value = {"999", "999999", "" + Integer.MAX_VALUE, "" + Long.MAX_VALUE})
    long maxL;

    private static final double LOG10 = Math.log(10);

    @Benchmark
    public int b_mathLog10() {
        return (int) Math.log10(max);
    }

    @Benchmark
    public int b_mathLogDivideLog10() {
        return mathLogDivideLog10(max);
    }

    public static int mathLogDivideLog10(int i) {
        return (int) (Math.log(i) / Math.log(10));
    }

    @Benchmark
    public int b_mathLogDivideLog10Cached() {
        return mathLogDivideLog10Cached(max);
    }

    public static int mathLogDivideLog10Cached(int max) {
        return (int) (Math.log(max) / LOG10);
    }

    @Benchmark
    public int b_mathLog10Pow() {
        return mathLog10Pow(max);
    }

    public int mathLog10Pow(int max) {
        int log10 = ((int) Math.log10(max));
        return (int) Math.pow(10, log10);
    }

    @Benchmark
    public int b_customLog10() {
        return customLog10(max);
    }

    public static int customLog10(int i) {
        if (i < 10)
            return 1;
        else if (i < 100) return 2;
        else if (i < 1000) return 3;
        else if (i < 10000) return 4;
        else if (i < 100000) return 5;
        else if (i < 1000000) return 6;
        else if (i < 10000000) return 7;
        else if (i < 100000000) return 8;
        else if (i < 1000000000) return 9;
        else return 10;
    }

    @Benchmark
    public int b_customLog10pow() {
        return customLog10Pow(max);
    }

    public static int customLog10Pow(int i) {
        if (i < 10)
            return 1;
        else if (i < 100) return 10;
        else if (i < 1000) return 100;
        else if (i < 10000) return 1000;
        else if (i < 100000) return 10000;
        else if (i < 1000000) return 100000;
        else if (i < 10000000) return 1000000;
        else if (i < 100000000) return 10000000;
        else if (i < 1000000000) return 100000000;
        else return 1000000000;
    }


    @Benchmark
    public int b_listCustomLog10() {
        return listCustomLog10(max);
    }

    private static final int[] sizeTable = {9, 99, 999, 9999, 99999, 999999, 9999999, 99999999, 999999999, Integer.MAX_VALUE};

    public static int listCustomLog10(int x) {
        for (int i = 0; ; i++)
            if (x <= sizeTable[i])
                return i + 1;
    }


    @Benchmark
    public int b_multLoopCustomLog10() {
        return multLoopCustomLog10(max);
    }

    static int multLoopCustomLog10(int x) {
        int p = 10;
        for (int i = 1; i < 19; i++) {
            if (x < p)
                return i;
            p = 10 * p;
        }
        return 10;
    }

    @Benchmark
    public int b_longMultLoopCustomLog10() {
        return multLoopCustomLog10(max);
    }

    @Benchmark
    public int b_longMultLoopCustomLog10MaxL() {
        return multLoopCustomLog10(maxL);
    }

    static int multLoopCustomLog10(long x) {
        int p = 10;
        for (int i = 1; i < 19; i++) {
            if (x < p)
                return i;
            p = 10 * p;
        }
        return 19;
    }

    @Benchmark
    public int b_longListCustomLog10MaxL() {
        return longListCustomLog10(maxL);
    }

    private static final long[] sizeTableL = {9, 99, 999, 9999, 99999, 999999, 9999999, 99999999, 999999999,
            9999999999L, 99999999999L, 999999999999L, 9999999999999L, 99999999999999L, 99999999999999L,
            9999999999999999L, 99999999999999999L, 999999999999999999L, Long.MAX_VALUE};

    public static int longListCustomLog10(long x) {
        for (int i = 0; ; i++)
            if (x <= sizeTableL[i])
                return i + 1;
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()//
                .include(".*" + IntegerPow10JMH.class.getSimpleName() + ".*MaxL")//
                .build();

        new Runner(opt).run();
    }

}
