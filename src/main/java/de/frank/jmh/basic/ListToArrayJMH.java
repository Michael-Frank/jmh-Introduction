package de.frank.jmh.basic;

import de.frank.jmh.BenchmarkFormatter;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.profile.DTraceAsmProfiler;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

/*--
Outdated - Just read the perfect article from Alexey: https://shipilev.net/blog/2016/arrays-wisdom-ancients/#_conclusion

 VM version: JDK 1.8.0_161-1-redhat, VM 25.161-b14
 Benchmark
                               1        100       100.000 <-ListSize
 toArrayStringUnsized   11 ns/op   48 ns/op  57.479 ns/op  # ALWAYS use un-sized & always use typed! the others are not worth it
 toArrayStringSized     24 ns/op   50 ns/op  59.266 ns/op  # sized is worse then un-sized. Corner usecase: you need a bigger result array then the source list
 toArray                12 ns/op   48 ns/op  50.376 ns/op  # Object[] - not typed
 toArrayObjectSized     25 ns/op   50 ns/op  59.644 ns/op  # Object[] - not typed
 toArrayObjectUnsized   10 ns/op   44 ns/op  61.779 ns/op  # Object[] - not typed
 toArrayStream          22 ns/op  217 ns/op 186.404 ns/op  # Object[] - not typed - sucks because of stream
 toArrayStreamString    36 ns/op  248 ns/op 346.462 ns/op  # sucks because of stream
 */
@State(Scope.Thread)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1, jvmArgsAppend = {"-XX:+UseParallelGC", "-Xms1g", "-Xmx1g"})
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class ListToArrayJMH {

    private static final String[] DATA_SIZE_PARAMS = {"1"};//, "100", "100000"};

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()//
                .include(ListToArrayJMH.class.getName() + ".*")//
//                .addProfiler(GCProfiler.class)//
//                .addProfiler(LinuxPerfNormProfiler.class)//Linux
//                .addProfiler(LinuxPerfAsmProfiler.class)//Linux
//                .addProfiler(WinPerfAsmProfiler.cl ass)//WIN
                .addProfiler(DTraceAsmProfiler.class)//OSX
                // to run Dtras run: sudo java -cp benchmarks.jar de.frank.jmh.basic.ListToArrayJMH -p dataSize=10

                //make sure we dont see compiling of our benchmark code during
                //measurement. if you see compiling => more warmup
                .jvmArgsAppend(
                        "-XX:+UnlockDiagnosticVMOptions",
                        "-XX:+PrintCompilation",
                        //"-XX:+PrintInlining", //
                        "-XX:+PrintAssembly", //requires hsdis binary in jdk - enable if you use the perf or winperf profiler
                        // "-XX:+PrintOptoAssembly", //c2 compiler only
                        //"-XX:+DebugNonSafepoints",
                        //required for external profilers like "perf" to show java
                        //frames in their traces
                        "-XX:+PreserveFramePointer"
                )
                .param("dataSize", DATA_SIZE_PARAMS)
                .resultFormat(ResultFormatType.JSON)
                .result(String.format("%s_%s.json",
                        DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                        ListToArrayJMH.class.getSimpleName()))
                .build();
        BenchmarkFormatter.displayAsMatrix(new Runner(opt).run(), "dataSize");
    }


    List<String> data;

    @Param({"1", "100", "100000"})
    private int dataSize;


    @Setup
    public void setup() {
        String[] tmp = new String[dataSize];
        for (int i = 0; i < tmp.length; i++) {
            tmp[i] = "foo" + i;
        }
        this.data = Arrays.asList(tmp); //toArray implementation in openjdk 1.8 is the same as java.util.ArrayList
    }


    @Benchmark
    public Object[] toArray() {
        //flawed, as Object[] does not require "instanceof String" checks on assignment and we probably allays want a typed array.
        return data.toArray();
    }


    @Benchmark
    public String[] toArrayStringUnsized() {
        return data.toArray(new String[0]);
    }

    @Benchmark
    public String[] toArrayStringSized() {
        return data.toArray(new String[data.size()]);
    }


    @Benchmark
    public Object[] toArrayObjectUnsized() {
        //flawed, as Object[] does not require "instanceof String" checks on assignment
        return data.toArray(new Object[0]);
    }

    @Benchmark
    public Object[] toArrayObjectSized() {
        //flawed, as Object[] does not require "instanceof String" checks on assignment
        return data.toArray(new Object[data.size()]);
    }


    @Benchmark
    public Object[] toArrayStream() {
        return data.stream().toArray();
    }

    @Benchmark
    public String[] toArrayStreamString() {
        return data.stream().toArray(String[]::new);
    }




}
