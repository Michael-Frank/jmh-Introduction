package de.frank.jmh.intro;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Threads(1)
@Fork(1)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Thread)
public class RightBasicBenchJMH {

    @Param({"algo1", "algo2", "algo3"})
    private String algoName;

    private Algo algo;

    private int value = 42;//jmh magic will prevent optimizing

    @Setup
    public void init() {
        switch (algoName) {
            case "algo1":
                algo = new AlgoImpl1();
                break;
            case "algo2":
                algo = new AlgoImpl2();
                break;
            case "algo3":
                algo = new AlgoImpl3();
                break;
            default:
                throw new IllegalArgumentException(algoName);
        }
    }

    @Benchmark
    public int testAlgo() {
        return algo.doWork(value);
    }

    interface Algo {
        int doWork(int x);
    }

    static class AlgoImpl1 implements Algo {
        @Override
        public int doWork(int x) {
            return x * x;
        }
    }

    static class AlgoImpl2 implements Algo {
        @Override
        public int doWork(int x) {
            return x * x;
        }
    }

    static class AlgoImpl3 implements Algo {
        @Override
        public int doWork(int x) {
            return x * x;
        }
    }


    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(RightBasicBenchJMH.class.getName() + ".*")
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

                // #########
                // Profling
                // #########
                // .jvmArgsAppend("-XX:+UnlockCommercialFeatures")
                // .jvmArgsAppend("-XX:+FlightRecorder")
                // .jvmArgsAppend("-XX:+UnlockDiagnosticVMOptions")
                // .jvmArgsAppend("-XX:+PrintSafepointStatistics")
                // .jvmArgsAppend("-XX:+DebugNonSafepoints")
                //
                // required for external profilers like "perf" to show java
                // frames in their traces
                // .jvmArgsAppend("-XX:+PerserveFramePointer")
                .build();
        new Runner(opt).run();

    }
}
