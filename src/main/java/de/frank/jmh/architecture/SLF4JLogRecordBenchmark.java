package de.frank.jmh.architecture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.OutputStreamAppender;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import static java.lang.String.format;

/**
 * --
 * <p>
 * What we measure:
 * -------------------
 * Benchmark primarily compares "lazy" logging idioms.
 * Generally we try to defer the costs of constructing log messages till we are sure we need them, AKA the loglevel is enabled.
 * We aim to ideally have zero overhead for disabled log levels, especially for DEBUG and TRACE log statements.
 * Secondly we compare using log4j2 directly vs using log4j over slf4j bridge.
 * <p>
 * There are many benchmarks on this topic but..
 * SLF4j 2.0.0-alpha Nov2019 finally introduced support for lazy lambda style logging e.g. LOGGER.atDebug().log(()-> "expensive: " + expensive.toString());
 * so its worth to benchmark again as most folks use slf4j and therefore did not have access to lambda style logging till now.
 * <p>
 * What we compare:
 * ------------------
 * - the current default best practice of using pattern style e.g. LOGGER.log("param1: {}, param2: {}", p1, p2)
 * - the new lambda way (new in SLF4j 2.0.0-alpha since Nov2019). Exists for quite some while now when using Log4j directly LOGGER.atDebug().log(()-> "param1: "+ p1 +", param2: "+ p2 +"")
 * Three bad and discouraged patterns:
 * - String concatenation (unless guarded by if(..) or wrapped in lambda).
 * - and String.format are considered bad practice, as they both eagerly construct the log messages, regardless of the message being used.
 * - the old pattern of wrapping expensive log statements in a if(isDebugEnabled()){LOGGER.debug(..)} - discouraged as it clutters the code
 * <p>
 * TL,DR Conclusion
 * ------------------
 * Good patterns:
 * - use latest dependencies and latest java versions
 * - Use the lambda style logging with plain string concatenation for overall good perf
 * - parametrized logging is well optimized, but inferior to lambda style in all cases
 * - if performance and 0-allocation is paramount - you MAY consider using the if(isDebugEnabled()) guard + string concat in critical sections
 * - Slf4j almost adds no overhead compared to using Log4j2 directly (Except for lambda, but lambda support is still alpha)
 * Bad patterns:
 * - NEVER use String.format()
 * - do not use String concatenation unless combined with lambdas or the if(isDebugEnabled()) guard
 * <p>
 * Bonus:
 * Newer Java versions (11,12,13...) provide new improved ways of optimizing + string concatenations:
 * Google: "Indify string concatenation" -Djava.lang.invoke.stringConcat=MH_INLINE_SIZED_EXACT
 *
 * @ OpenJDK 64-Bit Server VM 1.8.0_212 25.212-b04
 * Benchmark                         (LogLevel) Mode  Cnt   Score     Error   Units      gc.alloc.rate.norm # Comment
 * <p>
 * ## in "info"-level nothing is actually logged and every time and memory spent is overhead
 * log4j2_guarded_parametrized            INFO  avgt  5     0.933 ±   0.020   ns/op     0 ±   0        B/op # super perf but ugly - the if(isDebugEnabled()) is what we aim to avoid by using parametrization or lambdas
 * log4j2_guarded_stringConcatenation     INFO  avgt  5     1.039 ±   0.348   ns/op     0 ±   0        B/op # super perf but ugly - the if(isDebugEnabled()) is what we aim to avoid by using parametrization or lambdas
 * log4j2_lambda                          INFO  avgt  5     1.006 ±   0.307   ns/op     0 ±   0        B/op # WINNER - same perf as guarded but less ugly
 * log4j2_unguarded_parametrized          INFO  avgt  5     3.638 ±   0.087   ns/op    16 ±   0.001    B/op # 2nd Place but introduces VarArgs allocation overhead
 * log4j2_unguarded_stringConcatenation   INFO  avgt  5   136.739 ±   3.522   ns/op   440 ±   0.001    B/op # unnecessary string concat and allot of allocations
 * log4j2_string_format                   INFO  avgt  5  1499.139 ±  39.438   ns/op  2559 ±   2.805    B/op # string.format is always a bad idea - all hope is lost
 * <p>
 * slf4j_guarded_parametrized             INFO  avgt  5     1.260 ±   0.026   ns/op     0 ±   0        B/op # slf4j introduces only a minor overhead compared to using log4j2 directly
 * slf4j_guarded_stringConcatenation      INFO  avgt  5     1.250 ±   0.009   ns/op     0 ±   0        B/op
 * slf4j_lambda                           INFO  avgt  5     3.433 ±   0.046   ns/op    16 ±   0.001    B/op # but slf4j's lambda implementation still sucks compared to log4j where it is 100% for free
 * slf4j_unguarded_parametrized           INFO  avgt  5     4.994 ±   0.153   ns/op    32 ±   0.001    B/op
 * slf4j_unguarded_stringConcatenation    INFO  avgt  5   135.125 ±   1.611   ns/op   440 ±   0.001    B/op
 * slf4j_string_format                    INFO  avgt  5  1489.964 ±  22.067   ns/op  2559 ±   2.509    B/op # string.format is always a bad idea
 * <p>
 * <p>
 * ## in "debug"-level the log messages have to be actually constructed and written to file/stdout - lets see which one is fastest in concatenating
 * log4j2_guarded_parametrized           DEBUG  avgt  5   717.084 ± 366.217   ns/op   176 ±   0.001    B/op # parametrization inferior to lambdas string concat
 * log4j2_guarded_stringConcatenation    DEBUG  avgt  5   459.560 ±  10.272   ns/op   632 ±   0.001    B/op # super perf but ugly - the if(isDebugEnabled()) is what we aim to avoid by using parametrization or lambdas
 * log4j2_lambda                         DEBUG  avgt  5   482.827 ±  18.744   ns/op   624 ±   0.001    B/op # WINNER! again the simple lazy lambda + string concat winns
 * log4j2_unguarded_parametrized         DEBUG  avgt  5   639.383 ±  25.319   ns/op   200 ±   0.001    B/op
 * log4j2_unguarded_stringConcatenation  DEBUG  avgt  5   450.932 ±   9.981   ns/op   632 ±   0.001    B/op # fast here but big perf hit if the log message is not needed (see "INFO")
 * log4j2_string_format                  DEBUG  avgt  5  1862.951 ±  22.574   ns/op  2725 ±  13.473    B/op # string.format is always a bad idea
 * <p>
 * slf4j_guarded_parametrized            DEBUG  avgt  5   652.549 ±  10.777   ns/op   216 ±   0.001    B/op
 * slf4j_guarded_stringConcatenation     DEBUG  avgt  5   505.402 ±  11.918   ns/op   632 ±   0.001    B/op
 * slf4j_lambda                          DEBUG  avgt  5   509.408 ±   8.747   ns/op   728 ±   0.001    B/op
 * slf4j_unguarded_parametrized          DEBUG  avgt  5   646.308 ±  18.692   ns/op   240 ±   0.001    B/op
 * slf4j_unguarded_stringConcatenation   DEBUG  avgt  5   453.085 ±   3.658   ns/op   608 ±   0.001    B/op
 * slf4j_string_format                   DEBUG  avgt  5  1907.335 ±  22.879   ns/op  2661 ±  13.946    B/op # string.format is always a bad idea
 * @ OpenJDK 12.0.1, OpenJDK 64-Bit Server VM, 12.0.1+12
 * Benchmark                        (theLevel)  Mode Cnt  Score    Error   Units      gc.alloc.rate.norm # Comment
 * log4j2_guarded_parametrized            INFO  avgt  5     1,2 ±    0,3   ns/op     0 ±   0        B/op # see jdk1.8 benchmark
 * log4j2_guarded_stringConcatenation     INFO  avgt  5     1,1 ±    0,2   ns/op     0 ±   0        B/op # see jdk1.8 benchmark
 * log4j2_lambda                          INFO  avgt  5     1,1 ±    0,2   ns/op     0 ±   0        B/op # see jdk1.8 benchmark
 * log4j2_unguarded_parametrized          INFO  avgt  5     3,7 ±    0,3   ns/op    16 ±   0,001    B/op # see jdk1.8 benchmark
 * log4j2_unguarded_stringConcatenation   INFO  avgt  5   112,9 ±   11,1   ns/op   272 ±   0,001    B/op # string concatenation got better in post jdk 1.8! (StringMeta
 * log4j2_string_format                   INFO  avgt  5  1626,7 ±  146,9   ns/op  1583 ±   8,169    B/op # interestingly lower allocations - (score+error within same range as jdk 1.8)
 * <p>
 * slf4j_guarded_parametrized             INFO  avgt  5     1,4 ±    0,1   ns/op     0 ±   0        B/op # see jdk1.8 benchmark
 * slf4j_guarded_stringConcatenation      INFO  avgt  5     1,4 ±    0,0   ns/op     0 ±   0        B/op # see jdk1.8 benchmark
 * slf4j_lambda                           INFO  avgt  5     4,3 ±    3,4   ns/op    16 ±   0,001    B/op # see jdk1.8 benchmark
 * slf4j_unguarded_parametrized           INFO  avgt  5     5,2 ±    0,4   ns/op    32 ±   0,001    B/op # see jdk1.8 benchmark
 * slf4j_unguarded_stringConcatenation    INFO  avgt  5   112,3 ±   10,1   ns/op   272 ±   0,001    B/op # string concatenation got better in post jdk 1.8! (StringMeta
 * slf4j_string_format                    INFO  avgt  5  1606,1 ±  340,5   ns/op  1655 ±   6,276    B/op # interestingly lower allocations - (score+error within same range as jdk 1.8)
 * <p>
 * log4j2_guarded_parametrized           DEBUG  avgt  5   724,4 ±   11,1   ns/op   112 ±   0,001    B/op # see jdk1.8 benchmark
 * log4j2_guarded_stringConcatenation    DEBUG  avgt  5   578,6 ±  356,6   ns/op   400 ±   0,001    B/op # see jdk1.8 benchmark
 * log4j2_lambda                         DEBUG  avgt  5   561,0 ±  144,6   ns/op   416 ±   0,001    B/op # see jdk1.8 benchmark
 * log4j2_unguarded_parametrized         DEBUG  avgt  5   801,3 ±  100,5   ns/op   136 ±   0,001    B/op # see jdk1.8 benchmark
 * log4j2_unguarded_stringConcatenation  DEBUG  avgt  5   517,8 ±   42,8   ns/op   400 ±   0,001    B/op # see jdk1.8 benchmark
 * log4j2_string_format                  DEBUG  avgt  5  2050,3 ±  326,6   ns/op  1780 ±  16,177    B/op # see jdk1.8 benchmark
 * <p>
 * slf4j_guarded_parametrized            DEBUG  avgt  5  1216,7 ±  795,2   ns/op   176 ±   0,001    B/op # heavy outliner with heavy error - something messed up the test? reado?
 * slf4j_guarded_stringConcatenation     DEBUG  avgt  5   564,3 ±   36,2   ns/op   400 ±   0,001    B/op # see jdk1.8 benchmark
 * slf4j_lambda                          DEBUG  avgt  5   710,6 ±   65,6   ns/op   472 ±   0,001    B/op # see jdk1.8 benchmark
 * slf4j_unguarded_parametrized          DEBUG  avgt  5   782,3 ±   52,0   ns/op   200 ±   0,001    B/op # see jdk1.8 benchmark
 * slf4j_unguarded_stringConcatenation   DEBUG  avgt  5   501,7 ±   13,2   ns/op   400 ±   0,001    B/op # see jdk1.8 benchmark
 * slf4j_string_format                   DEBUG  avgt  5  1988,2 ±  123,5   ns/op  1780 ±  16,188    B/op # see jdk1.8 benchmark
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 15, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 3)
@State(Scope.Benchmark)
public class SLF4JLogRecordBenchmark {

    private static final Logger LOG4j2_LOGGER = LogManager.getLogger(SLF4JLogRecordBenchmark.class);
    private static final org.slf4j.Logger SLF4J_LOGGER = org.slf4j.LoggerFactory.getLogger(SLF4JLogRecordBenchmark.class);
    private static final java.util.logging.Logger JUL_LOGGER = java.util.logging.Logger.getLogger(SLF4JLogRecordBenchmark.class.getName());

    private String aString = "P1";
    private int anInt = 42;
    private float aFloat = 0.42f;
    private boolean aBoolean = true;
    private char aChar = '!';


    @Param({"INFO", "DEBUG"})
    private static String theLevel;

    public static void main(String[] args) throws RunnerException {

        Options opt = new OptionsBuilder()
                .include(SLF4JLogRecordBenchmark.class.getName())
                .resultFormat(ResultFormatType.JSON)
                .result(String.format("%s_%s.json",
                        DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                        SLF4JLogRecordBenchmark.class.getSimpleName()))
                .addProfiler(GCProfiler.class)
                .build();
        new Runner(opt).run();
    }


    @Setup
    public void setUp(final Blackhole blackhole) {

        ByteArrayOutputStream blackHoleOutStream = new ByteArrayOutputStream() {
            @Override
            public void write(int b) {
                blackhole.consume(b);
            }

            @Override
            public void write(byte[] b, int off, int len) {
                blackhole.consume(b);
            }

            @Override
            public void write(byte[] b) {
                blackhole.consume(b);
            }
        };

        setupLog4j(blackHoleOutStream);
        setupJUL(blackHoleOutStream);
    }

    private void setupJUL(ByteArrayOutputStream blackHoleOutStream) {
        Level level = "INFO".equals(theLevel) ? Level.INFO : Level.FINE;
        java.util.logging.LogManager.getLogManager().reset(); //remove all loggers except the root logger
        java.util.logging.Logger rootLogger = java.util.logging.LogManager.getLogManager().getLogger("");// ""==rootLogger
        //set desired level to test
        rootLogger.setLevel(level);
        //remove all handlers and add only
        for (Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }
        StreamHandler bhHandler = new StreamHandler(blackHoleOutStream, new SimpleFormatter());
        bhHandler.setLevel(Level.FINE);
        rootLogger.addHandler(bhHandler);

    }

    private void setupLog4j(ByteArrayOutputStream blackHoleOutStream) {
        LoggerContext context = LoggerContext.getContext(false);
        org.apache.logging.log4j.core.Logger logger = context.getLogger(SLF4J_LOGGER.getName());
        logger.getAppenders().forEach((name, appender) -> logger.removeAppender(appender));
        logger.setAdditive(false);
        logger.addAppender(OutputStreamAppender.createAppender(null, null, blackHoleOutStream, "blackholeAppender", false, true));
        org.apache.logging.log4j.Level logLevel = "INFO".equals(theLevel) ? org.apache.logging.log4j.Level.INFO : org.apache.logging.log4j.Level.DEBUG;
        logger.setLevel(logLevel);
    }

    @Benchmark
    public void slf4j_string_format() {
        SLF4J_LOGGER.debug(
                format("Result [%s], [%s], [%s], [%s], [%s]",
                        aString, ++anInt, aBoolean, aFloat++, aChar));
    }

    @Benchmark
    public void slf4j_lambda() {
        SLF4J_LOGGER.atDebug().log(
                () -> ("Result [" + aString + "], [" + (++anInt) + "], [" +
                        aBoolean + "], [" + (aFloat++) + "], [" + aChar + "]"));
    }

    @Benchmark
    public void slf4j_unguarded_parametrized() {
        SLF4J_LOGGER.debug("Result [{}], [{}], [{}], [{}], [{}]",
                aString, ++anInt, aBoolean, aFloat++, aChar);
    }

    @Benchmark
    public void slf4j_guarded_parametrized() {
        if (SLF4J_LOGGER.isDebugEnabled()) {
            SLF4J_LOGGER.debug("Result [{}], [{}], [{}], [{}], [{}]",
                    aString, ++anInt, aBoolean, aFloat++, aChar);
        }
    }

    @Benchmark
    public void slf4j_unguarded_stringConcatenation() {
        SLF4J_LOGGER.debug("Result [" + aString + "], [" + (++anInt) + "], [" +
                aBoolean + "], [" + (aFloat++) + "], [" + aChar + "]");
    }

    @Benchmark
    public void slf4j_guarded_stringConcatenation() {
        if (SLF4J_LOGGER.isDebugEnabled()) {
            SLF4J_LOGGER.debug("Result [" + aString + "], [" + (++anInt) + "], [" +
                    aBoolean + "], [" + (aFloat++) + "], [" + aChar + "]");
        }
    }


    //log4j2
    @Benchmark
    public void log4j2_string_format() {
        LOG4j2_LOGGER.debug(
                format("Result [%s], [%s], [%s], [%s], [%s]",
                        aString, ++anInt, aBoolean, aFloat++, aChar));
    }

    @Benchmark
    public void log4j2_lambda() {
        LOG4j2_LOGGER.debug(
                () -> ("Result [" + aString + "], [" + (++anInt) + "], [" +
                        aBoolean + "], [" + (aFloat++) + "], [" + aChar + "]"));
    }

    @Benchmark
    public void log4j2_unguarded_parametrized() {
        LOG4j2_LOGGER.debug("Result [{}], [{}], [{}], [{}], [{}]",
                aString, ++anInt, aBoolean, aFloat++, aChar);
    }

    @Benchmark
    public void log4j2_guarded_parametrized() {
        if (LOG4j2_LOGGER.isDebugEnabled()) {
            LOG4j2_LOGGER.debug("Result [{}], [{}], [{}], [{}], [{}]",
                    aString, ++anInt, aBoolean, aFloat++, aChar);
        }
    }

    @Benchmark
    public void log4j2_unguarded_stringConcatenation() {
        LOG4j2_LOGGER.debug("Result [" + aString + "], [" + (++anInt) + "], [" +
                aBoolean + "], [" + (aFloat++) + "], [" + aChar + "]");
    }

    @Benchmark
    public void log4j2_guarded_stringConcatenation() {
        if (LOG4j2_LOGGER.isDebugEnabled()) {
            LOG4j2_LOGGER.debug("Result [" + aString + "], [" + (++anInt) + "], [" +
                    aBoolean + "], [" + (aFloat++) + "], [" + aChar + "]");
        }
    }

}
