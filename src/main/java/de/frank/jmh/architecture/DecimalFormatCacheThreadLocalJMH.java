package de.frank.jmh.architecture;

import com.yevdo.jwildcard.JWildcard;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/*--
Test: Create new DecimalFormat pattern each time vs ThreadLocal cache them (Pattern: "0.000")
Result: yes, its much better to cache them, offering a 2-6x improvement, depending on the formatting overhead itself.


Benchmark                      (data)   Mode         Score   Units   gc.alloc.rate.norm
newEachTime                      20.0  thrpt     1.316.115   ops/s   1840 B/op
newEachTime   314159265358979323846.0  thrpt       940.076   ops/s   2008 B/op
newEachTime    3141592653.58979323846  thrpt     1.158.990   ops/s   1928 B/op
newEachTime    3.14159265358979323846  thrpt       996.516   ops/s   1920 B/op
threadLocal                      20.0  thrpt     7.844.241   ops/s    160 B/op
threadLocal   314159265358979323846.0  thrpt     2.215.016   ops/s    256 B/op
threadLocal    3141592653.58979323846  thrpt     2.443.725   ops/s    184 B/op
threadLocal    3.14159265358979323846  thrpt     2.991.874   ops/s    176 B/op

*/

/**
 * @author Michael Frank
 * @version 1.0 20.01.2020
 */
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(3)
@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark) // Important to be Scope.Benchmark
@Threads(16)
public class DecimalFormatCacheThreadLocalJMH {

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()//
                .include(JWildcard.wildcardToRegex(DecimalFormatCacheThreadLocalJMH.class.getName() + "*"))//
                .result(String.format("%s_%s.json",
                        DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                        DecimalFormatCacheThreadLocalJMH.class.getSimpleName()))
                .addProfiler(GCProfiler.class).build();
        new Runner(opt).run();
    }

    public static class DecimalFormatJMH {
        private static final ThreadLocal<DecimalFormat> FORMATTERS = ThreadLocal.withInitial(DecimalFormatJMH::getDecimalFormat);

        private static DecimalFormat getDecimalFormat() {
            return new DecimalFormat("0.000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        }

        @Benchmark
        public DecimalFormat newEachTime() {
            return getDecimalFormat();
        }

        @Benchmark
        public DecimalFormat threadLocal() {
            return FORMATTERS.get();
        }
    }

    public static class DoubleDecimalFormatJMH {

        private static final ThreadLocal<DecimalFormat> FORMATTERS = ThreadLocal.withInitial(DoubleDecimalFormatJMH::getDecimalFormat);

        private static DecimalFormat getDecimalFormat() {
            return new DecimalFormat("0.000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        }

        @State(Scope.Benchmark)
        public static class MyState {
            @Param({"20.0", "314159265358979323846.0", "3141592653.58979323846", "3.14159265358979323846"})
            public double aDouble;
        }

        @Benchmark
        public String double_newEachTime(MyState s) {
            return getDecimalFormat().format(s.aDouble);
        }

        @Benchmark
        public String double_threadLocal(MyState s) {
            return FORMATTERS.get().format(s.aDouble);
        }
    }

    public static class FloatDecimalFormatJMH {
        private static final ThreadLocal<DecimalFormat> FORMATTERS = ThreadLocal.withInitial(FloatDecimalFormatJMH::getDecimalFormat);

        private static DecimalFormat getDecimalFormat() {
            return new DecimalFormat("0.000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        }

        @State(Scope.Benchmark)
        public static class MyState {
            @Param({"20.0", "314159265358979323846.0", "3141592653.58979323846", "3.14159265358979323846"})
            public float aFloat;
        }


        @Benchmark
        public String float_newEachTime(MyState s) {
            return getDecimalFormat().format(s.aFloat);
        }

        @Benchmark
        public String float_threadLocal(MyState s) {
            return FORMATTERS.get().format(s.aFloat);
        }


    }
}
