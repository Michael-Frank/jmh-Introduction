package de.frank.jmh.basic;

import org.apache.commons.lang3.RandomStringUtils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/*--
Intro
=====
This is not a rant or a case against streams.
It is an reminder that it is an additional tool in your box, not the only one, not an replacement and maybe not even your first choice.
Know the implications and carefully weight them in your specific context.
  Run once glue-code -> streams are fine.
  Performance critical tight inner loop -> classic loops
  Dont require a .filter() or any other fancy option? just one simple .map()? -> maybe stick to loops


Disclaimer:
==========
classic, stream and parallelStream are taken form http://50226.de/benchmark-performance-von-streams-in-java-8.html
"_fixed"  are versions added by myself.

What to be learned:
=================
Stream, classic or whatever is irrelevant if you write bade code. Neither will rescue you.
Eager filtering and thus "Dont do unnecessary work" are the easiest principles but still often overlooked.
Beware of hidden costs of seemingly simple methods, like in this case with .toLowercase() which has to look at each
char in the String O(n) and also has to create a copy of the String.

First the numbers:
Single-threaded THROUGHPUT in ops/s - higher is better
                          100  10000 100000 #<-list size
classic                70.254    350      3
stream                 68.652    352      3
parallelStream         28.709  1.259     13
--
classic_fixed         832.263  2.213     16
stream_fixed          496.389  2.082     17
parallelStream_fixed   40.561  3.985     39


Nevertheless lets look at stream vs classic:
Streams haven on overhead. But depending on the complexity of the operations on the stream elements, it might not be visible.
Compare the numbers for differnt length's of the list. Stream ned setup time and alot of compiler love and optimizations.
So for small number of elements in your list a loop run faster, alot.

But all of this is meaningless if you mess up the overall logic. See non-fixed with fixed versions. You get a 10X improvment.

What about parallel?
Parallel streams have a very high startup overhead.
Are you sure you stream is not already executed multiple times?
If this code runs in an Webserver context it may already be executed at each request (in parallel).
Dont use Parallel!
Still not convinced? Sure your application is "single threaded" sure this is the "slow part"? sure you cant fix it with caching?
Look again at the results, parallelStreams have significant overhead. Sure your data is big enough and the operation on it
complex enough that the overhead is worth it? Sure you have CPU resources to spare?
-> You have to measure it with YOUR REAL DATA in your real application.


For a better understanding of the issues with parallelStreams look at the multithreaded numbers

Multi-threaded THROUGHPUT in ops/s - higher is better
                            100     10000  100000 #<-list size
classic                 286.829     1.883      15
stream                  297.455     1.805      17
parallelStream          145.954     1.791      17
stream_fixed          1.569.788    10.913      51
classic_fixed         1.626.222    10.983      47
parallelStream_fixed    287.844     8.079      46

*/

/**
 * @author Michael Frank
 * @version 1.0 13.05.2018
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Threads(1)
@Fork(1)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class StreamExampleJMH {

    @Param({"100", "10000", "1000000"})
    private int len;

    private List<String> strings;

    @Setup
    public void setup() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        String[] strings = new String[len];
        for (int i = 0; i < strings.length; i++) {
            strings[i] = RandomStringUtils.random(32, 0, 0, true, true, null, r);
        }
        this.strings = Arrays.asList(strings);
    }


    @Benchmark
    public String stream() {
        return strings.stream()
                .map(String::toLowerCase)
                .filter(string -> string.startsWith("a"))
                .map(string -> string.replaceAll("a", "z"))
                .sorted()
                .collect(Collectors.joining(","));
    }


    @Benchmark
    public String stream_fixed() {
        return strings.stream()
                //more intelligent filter
                .filter(string -> Character.toLowerCase(string.charAt(0)) == 'a')
                .map(string -> string.toLowerCase().replaceAll("a", "z"))
                .sorted()
                .collect(Collectors.joining(","));
    }


    @Benchmark
    public String parallelStream() {
        return strings.parallelStream()
                .map(String::toLowerCase)
                .filter(string -> string.startsWith("a"))
                .map(string -> string.replaceAll("a", "z"))
                .sorted()
                .collect(Collectors.joining(","));
    }

    @Benchmark
    public String parallelStream_fixed() {
        return strings.parallelStream()
                //more intelligent filter
                .filter(string -> Character.toLowerCase(string.charAt(0)) == 'a')
                .map(string -> string.toLowerCase().replaceAll("a", "z"))
                .sorted()
                .collect(Collectors.joining(","));
    }

    @Benchmark
    public String classic() {
        List<String> filtered = new ArrayList<>(strings.size());

        for (String string : strings) {
            if (string.toLowerCase().startsWith("a")) {
                filtered.add(string.replaceAll("a", "z"));
            }
        }

        Collections.sort(filtered);

        StringBuilder builder = new StringBuilder();
        builder.append(filtered.get(0));

        for (int i = 1; i < filtered.size(); i++) {
            builder.append(",");
            builder.append(filtered.get(i));
        }

        return builder.toString();
    }


    @Benchmark
    public String classic_fixed() {
        List<String> filtered = new ArrayList<>(strings.size());

        for (String string : strings) {
            //more intelligent filter
            if ((Character.toLowerCase(string.charAt(0)) == 'a')) {
                filtered.add(string.toLowerCase().replaceAll("a", "z"));
            }
        }
        Collections.sort(filtered);

        StringBuilder builder = new StringBuilder();
        for (String s : filtered) {
            builder.append(s);
            builder.append(',');
        }
        builder.setLength(builder.length() - 1);//delete last ','
        return builder.toString();
    }


    public static void main(String[] args) throws RunnerException {

        System.setProperty("jmh.perfasm.xperf.dir", "C:\\Program Files (x86)\\Windows Kits\\10\\Windows Performance Toolkit");
        new Runner(new OptionsBuilder()
                .include(StreamExampleJMH.class.getName() + ".*")
                //##########
                // Profilers
                //############
                //commonly used profilers:
                //.addProfiler(GCProfiler.class)
                //.addProfiler(StackProfiler.class)
                //.addProfiler(HotspotRuntimeProfiler.class)
                //.addProfiler(HotspotMemoryProfiler.class)
                //.addProfiler(HotspotCompilationProfiler.class)
                //.addProfiler(WinPerfAsmProfiler.class)

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
                // .jvmArgsAppend("-XX:+PrintCompilation")
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

}
