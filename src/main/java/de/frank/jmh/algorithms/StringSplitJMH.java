package de.frank.jmh.algorithms;

import com.google.common.base.Splitter;
import org.apache.commons.lang3.StringUtils;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/*--
Results in ns/op - lower is better
               len->     2     10      100
oneChar
    cust_charAtLoop     33    146    1.466   # winner - custom impl is best
    cust_indexOfLoop    33    138    1.532   # .. as fast as charAt and a bit simpler algo
    StringTokenizer     42    194    1.779   # StringTokenizer is best for ease of use and generic application (internal impl basically is a charAt-loop)
    StringSplit         55    187    2.092   # java's String.split has a fast path for single char splits, not using regex
    apacheStringUtils   58    231    2.410   # inferior to other options
    guava               75    268    2.695   # guava sucks
twoChars
    StringTokenizer     36    161    2.912   # StringTokenizer is best for ease of use and generic application
    PatternCompiled    106    347    3.412   # pre-compiled Pattern is ok and the most powerful solution
    apacheStringUtils   67    315    3.523   # inferior to other options
    guava               73    320    3.844   # guava sucks
    StringSplit        202    459    3.634   # suffers heavily from constant pattern recompilations, especially for short input length


Benchmark                                  (listLenght)  Mode  Cnt     Score     Error  Units
StringSplitJMH.oneChar_StringSplit                    2  avgt   15    55,477 ±   4,776  ns/op
StringSplitJMH.oneChar_StringSplit                   10  avgt   15   187,395 ±  29,673  ns/op
StringSplitJMH.oneChar_StringSplit                  100  avgt   15  2091,684 ± 476,517  ns/op
StringSplitJMH.oneChar_StringTokenizer                2  avgt   15    41,616 ±   6,339  ns/op
StringSplitJMH.oneChar_StringTokenizer               10  avgt   15   193,658 ±  32,147  ns/op
StringSplitJMH.oneChar_StringTokenizer              100  avgt   15  1778,897 ± 215,105  ns/op
StringSplitJMH.oneChar_apacheStringUtils              2  avgt   15    57,906 ±   1,257  ns/op
StringSplitJMH.oneChar_apacheStringUtils             10  avgt   15   231,359 ±   5,176  ns/op
StringSplitJMH.oneChar_apacheStringUtils            100  avgt   15  2410,349 ±  44,517  ns/op
StringSplitJMH.oneChar_charAtLoop                     2  avgt   15    33,080 ±   1,072  ns/op
StringSplitJMH.oneChar_charAtLoop                    10  avgt   15   146,035 ±   3,571  ns/op
StringSplitJMH.oneChar_charAtLoop                   100  avgt   15  1465,883 ±  25,358  ns/op
StringSplitJMH.oneChar_guava                          2  avgt   15    74,525 ±  14,335  ns/op
StringSplitJMH.oneChar_guava                         10  avgt   15   267,859 ±   7,719  ns/op
StringSplitJMH.oneChar_guava                        100  avgt   15  2694,929 ±  53,819  ns/op
StringSplitJMH.oneChar_indexOfLoop                    2  avgt   15    32,735 ±   0,494  ns/op
StringSplitJMH.oneChar_indexOfLoop                   10  avgt   15   138,286 ±   2,689  ns/op
StringSplitJMH.oneChar_indexOfLoop                  100  avgt   15  1532,366 ±  18,651  ns/op
StringSplitJMH.towChars_StringSplit                   2  avgt   15   202,361 ±   3,112  ns/op
StringSplitJMH.towChars_StringSplit                  10  avgt   15   459,219 ±  17,114  ns/op
StringSplitJMH.towChars_StringSplit                 100  avgt   15  3634,344 ± 104,228  ns/op
StringSplitJMH.twoChars_PatternCompiled               2  avgt   15   106,372 ±   2,990  ns/op
StringSplitJMH.twoChars_PatternCompiled              10  avgt   15   346,978 ±  19,339  ns/op
StringSplitJMH.twoChars_PatternCompiled             100  avgt   15  3412,068 ±  43,922  ns/op
StringSplitJMH.twoChars_StringTokenizer               2  avgt   15    35,546 ±   0,403  ns/op
StringSplitJMH.twoChars_StringTokenizer              10  avgt   15   161,248 ±   2,274  ns/op
StringSplitJMH.twoChars_StringTokenizer             100  avgt   15  2912,123 ±  43,327  ns/op
StringSplitJMH.twoChars_apacheStringUtils             2  avgt   15    66,648 ±   1,493  ns/op
StringSplitJMH.twoChars_apacheStringUtils            10  avgt   15   315,180 ±   9,063  ns/op
StringSplitJMH.twoChars_apacheStringUtils           100  avgt   15  3522,887 ±  41,564  ns/op
StringSplitJMH.twoChars_guava                         2  avgt   15    73,272 ±   1,361  ns/op
StringSplitJMH.twoChars_guava                        10  avgt   15   319,869 ±   7,161  ns/op
StringSplitJMH.twoChars_guava                       100  avgt   15  3843,823 ± 711,762  ns/op
*/

/**
 * @author Michael Frank
 * @version 1.0 13.05.2018
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Threads(1)
@Fork(3)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Thread)
public class StringSplitJMH {


    private static final Pattern PATTERN_SPLIT_TWO = Pattern.compile(",\\.");
    private static final Splitter GUAVA_SPLIT_TWO = Splitter.on(",.");
    private static final Splitter GUAVA_SPLIT_ONE = Splitter.on(',');


    @State(Scope.Thread)
    public static class MyState {

        @Param({"2", "10", "100"})
        private int listLenght;

        private String commaString; // @stringLen10: "1,2,3,4,5,6,7,8,9,10";
        private String commaDotString;//@stringLen10 "1,.2,.3,.4,.5,.6,.7,.8,.9,.10";


        public MyState() {
            //For jmh
        }

        /**
         * For manual correctness tests
         *
         * @param listLenght
         */
        public MyState(int listLenght) {
            this.listLenght = listLenght;
            doSetup();
        }


        @Setup(Level.Trial)
        public void doSetup() {
            commaString = IntStream.rangeClosed(1, listLenght).mapToObj(Integer::toString).collect(Collectors.joining(","));
            commaDotString = IntStream.rangeClosed(1, listLenght).mapToObj(Integer::toString).collect(Collectors.joining(",."));
        }
    }


    @Benchmark
    public List<String> oneChar_StringTokenizer(MyState s) {
        ArrayList<String> tokens = new ArrayList<>();
        StringTokenizer st = new StringTokenizer(s.commaString, ",");
        while (st.hasMoreTokens()) {
            tokens.add(st.nextToken());
        }
        return tokens;
    }

    @Benchmark
    public List<String> twoChars_StringTokenizer(MyState s) {
        ArrayList<String> tokens = new ArrayList<>();
        StringTokenizer st = new StringTokenizer(s.commaDotString, ",.");
        while (st.hasMoreTokens()) {
            tokens.add(st.nextToken());
        }
        return tokens;
    }

    @Benchmark
    public List<String> oneChar_indexOfLoop(MyState s) {
        final String intput = s.commaString;

        ArrayList<String> tokens = new ArrayList<>();

        int pos = 0, end;
        while ((end = intput.indexOf(',', pos)) >= 0) {
            tokens.add(intput.substring(pos, end));
            pos = end + 1;
        }
        if (pos < intput.length()) {//last token
            tokens.add(intput.substring(pos));

        }
        return tokens;
    }

    @Benchmark
    public List<String> oneChar_charAtLoop(MyState s) {
        final String intput = s.commaString;
        ArrayList<String> tokens = new ArrayList<>();

        //does impl not support surrogates!
        int wordStart = -1;
        for (int i = 0; i < intput.length(); i++) {
            char cpAti = intput.charAt(i);
            if (cpAti == ',') {
                tokens.add(intput.substring(wordStart, i));
                wordStart = -1;
            } else if (wordStart == -1) {
                wordStart = i;
            }
        }
        if (wordStart > -1) {//last token
            tokens.add(intput.substring(wordStart));
        }

        return tokens;
    }


    @Benchmark
    public String[] oneChar_StringSplit(MyState s) {
        return s.commaString.split(",");
    }

    @Benchmark
    public String[] towChars_StringSplit(MyState s) {
        return s.commaDotString.split(",\\.");
    }

    @Benchmark
    public String[] twoChars_PatternCompiled(MyState s) {
        return PATTERN_SPLIT_TWO.split(s.commaDotString);
    }

    @Benchmark
    public String[] oneChar_apacheStringUtils(MyState s) {
        return org.apache.commons.lang.StringUtils.split(s.commaString, ",");
    }

    @Benchmark
    public String[] twoChars_apacheStringUtils(MyState s) {
        return org.apache.commons.lang.StringUtils.split(s.commaDotString, ",.");
    }

    @Benchmark
    public List<String> oneChar_guava(MyState s) {
        return GUAVA_SPLIT_ONE.splitToList(s.commaString);
    }

    @Benchmark
    public List<String> twoChars_guava(MyState s) {
        return GUAVA_SPLIT_TWO.splitToList(s.commaDotString);
    }


    public static void main(String[] args) throws RunnerException {
        MyState state = new MyState(3);

        Predicate<String> arrayProducing = method -> StringUtils.containsAny(method, "StringSplit", "PatternCompile", "_apacheStringUtils");
        String[] expected = {"1", "2", "3"};
        testImplementations(arrayProducing, o -> Arrays.equals(expected, (String[]) o), state);
        testImplementations(arrayProducing.negate(), o -> Arrays.asList(expected).equals(o), state);


        System.setProperty("jmh.perfasm.xperf.dir", "C:\\Program Files (x86)\\Windows Kits\\10\\Windows Performance Toolkit");
        new Runner(new OptionsBuilder()
                .include(StringSplitJMH.class.getName() + ".*")
                //##########
                // Profilers
                //############
                //commonly used profilers:
                //.addProfiler(GCProfiler.class)
                //.addProfiler(StackProfiler.class)
                //.addProfiler(HotspotRuntimeProfiler.class)
                //.addProfiler(HotspotMemoryProfiler.class)
                //.addProfiler(HotspotCompilationProfiler.class)
                //
                // full list of built in profilers:
                //("cl",       ClassloaderProfiler.class);
                //("comp",     CompilerProfiler.class);
                //("gc",       GCProfiler.class);
                //("hs_cl",    HotspotClassloadingProfiler.class);
                //("hs_comp",  HotspotCompilationProfiler.class);
                //("hs_gc",    HotspotMemoryProfiler.class);
                //("hs_rt",    HotspotRuntimeProfiler.class);
                //("hs_thr",   HotspotThreadProfiler.class);
                //("stack",    StackProfiler.class);
                //("perf",     LinuxPerfProfiler.class);
                //("perfnorm", LinuxPerfNormProfiler.class);
                //("perfasm",  LinuxPerfAsmProfiler.class);
                //("xperfasm", WinPerfAsmProfiler.class);
                //("dtraceasm", DTraceAsmProfiler.class);
                //("pauses",   PausesProfiler.class);
                //("safepoints", SafepointsProfiler.class);
                //
                //ASM-level profilers - require -XX:+PrintAssembly
                //----------
                // this in turn requires hsdis (hotspot disassembler) binaries to be copied into e.g C:\Program Files\Java\jdk1.8.0_161\jre\bin\server
                // For Windows you can download pre-compiled hsdis module from http://fcml-lib.com/download.html
                //.jvmArgsAppend("-XX:+PrintAssembly") //requires hsdis binary in jdk - enable if you use the perf or winperf profiler
                ///required for external profilers like "perf" to show java frames in their traces
                //.jvmArgsAppend("-XX:+PreserveFramePointer")
                //XPERF  - windows xperf must be installed - this is included in WPT (windows performance toolkit) wich in turn is windows ADK included in https://developer.microsoft.com/en-us/windows/hardware/windows-assessment-deployment-kit
                //WARNING - MUST RUN WITH ADMINISTRATIVE PRIVILEGES (must start your console or your IDE with admin rights!
                //WARNING - first ever run of xperf takes VERY VERY long (1h+) because it has to download and process symbols
                //.addProfiler(WinPerfAsmProfiler.class)
                //.addProfiler(LinuxPerfProfiler.class)
                //.addProfiler(LinuxPerfNormProfiler.class)
                //.addProfiler(LinuxPerfAsmProfiler.class)
                //
                // #########
                // More Profling jvm options
                // #########
                // .jvmArgsAppend("-XX:+UnlockCommercialFeatures")
                // .jvmArgsAppend("-XX:+FlightRecorder")
                // .jvmArgsAppend("-XX:+UnlockDiagnosticVMOptions")
                // .jvmArgsAppend("-XX:+PrintSafepointStatistics")
                // .jvmArgsAppend("-XX:+DebugNonSafepoints")
                //
                // required for external profilers like "perf" to show java
                // frames in their traces
                // .jvmArgsAppend("-XX:+PreserveFramePointer")
                //
                // #########
                // COMPILER
                // #########
                // make sure we dont see compiling of our benchmark code during
                // measurement.
                // if you see compiling => more warmup
                .jvmArgsAppend("-XX:+UnlockDiagnosticVMOptions")
                //.jvmArgsAppend("-XX:+PrintCompilation")
                // .jvmArgsAppend("-XX:+PrintInlining")
                // .jvmArgsAppend("-XX:+PrintAssembly") //requires hsdis binary in jdk - enable if you use the perf or winperf profiler
                // .jvmArgsAppend("-XX:+PrintOptoAssembly") //c2 compiler only
                // More compiler prints:
                // .jvmArgsAppend("-XX:+PrintInterpreter")
                // .jvmArgsAppend("-XX:+PrintNMethods")
                // .jvmArgsAppend("-XX:+PrintNativeNMethods")
                // .jvmArgsAppend("-XX:+PrintSignatureHandlers")
                // .jvmArgsAppend("-XX:+PrintAdapterHandlers")
                // .jvmArgsAppend("-XX:+PrintStubCode")
                // .jvmArgsAppend("-XX:+PrintCompilation")
                // .jvmArgsAppend("-XX:+PrintInlining")
                // .jvmArgsAppend("-XX:+TraceClassLoading")
                // .jvmArgsAppend("-XX:PrintAssemblyOptions=syntax")
                .build())
                .run();
    }

    private static void testImplementations(Predicate<String> methodNameFilter, Predicate<Object> verifyExpected, MyState state) {

        List<Method> toTest = Arrays.stream(StringSplitJMH.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(Benchmark.class))
                .filter(m -> methodNameFilter.test(m.getName()))
                .collect(Collectors.toList());

        System.out.println("Verifying: " + toTest.size() + "  implementations : " + toTest);
        toTest.forEach(m -> verifyExpectedResult(m, state, verifyExpected));
    }

    private static void verifyExpectedResult(Method m, MyState state, Predicate<Object> verifyExpected) {
        StringSplitJMH s = new StringSplitJMH();
        RuntimeException err = null;
        try {
            Object actual = m.invoke(s, state);


            if (!verifyExpected.test(actual)) {
                err = new RuntimeException(m.getName() + " did not produce expected result.'\nActual:\n'" + actual + "'");
            }
        } catch (ReflectiveOperationException e) {
            err = new RuntimeException("Error invoking method: " + m.getName(), e);
        }

        if (err == null) {
            System.out.println(" OK " + m.getName());
        } else {
            System.out.println("NOK " + m.getName());
            throw err;
        }

    }

}
