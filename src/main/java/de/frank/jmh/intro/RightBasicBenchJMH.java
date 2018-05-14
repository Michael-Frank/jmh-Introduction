package de.frank.jmh.intro;

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

import java.util.concurrent.TimeUnit;


/*--
 Benchmark                    (algoName)  Mode  Cnt  Score   Error  Units
 RightBasicBenchJMH.testAlgo       algo1  avgt    5  2,444 ± 0,286  ns/op
 RightBasicBenchJMH.testAlgo       algo2  avgt    5  2,399 ± 0,033  ns/op
 RightBasicBenchJMH.testAlgo       algo3  avgt    5  2,426 ± 0,123  ns/op
 */
/**
 * @author Michael Frank
 * @version 1.0 13.05.2018
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Threads(1)
@Fork(1)//demo normally run with fork's >= 3
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
        System.setProperty("jmh.perfasm.xperf.dir","C:\\Program Files (x86)\\Windows Kits\\10\\Windows Performance Toolkit");
        new Runner(new OptionsBuilder()
                .include(RightBasicBenchJMH.class.getName() + ".*")
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
                //.jvmArgsAppend("-XX:+PerserveFramePointer")
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
                // .jvmArgsAppend("-XX:+PerserveFramePointer")
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
    /*--
Bonus: generated assembly with profiling information   .addProfiler(WinPerfAsmProfiler.class)
Hottest code regions (>10,00% "SampledProfile" events):

....[Hottest Region 1]..............................................................................
C2, level 4, de.frank.jmh.intro.generated.RightBasicBenchJMH_testAlgo_jmhTest::testAlgo_avgt_jmhStub, version 571 (63 bytes)

           0x0000000002a2b6b4: mov     r10,qword ptr [rsp+60h]
           0x0000000002a2b6b9: movzx   r11d,byte ptr [r10+94h]  ;*getfield isDone
                                                         ; - de.frank.jmh.intro.generated.RightBasicBenchJMH_testAlgo_jmhTest::testAlgo_avgt_jmhStub@30 (line 188)
                                                         ; implicit exception: dispatches to 0x0000000002a2b7c5
           0x0000000002a2b6c1: mov     ebp,1h
           0x0000000002a2b6c6: test    r11d,r11d
           0x0000000002a2b6c9: jne     2a2b714h          ;*ifeq
                                                         ; - de.frank.jmh.intro.generated.RightBasicBenchJMH_testAlgo_jmhTest::testAlgo_avgt_jmhStub@33 (line 188)
           0x0000000002a2b6cb: nop     dword ptr [rax+rax+0h]  ;*aload
                                                         ; - de.frank.jmh.intro.generated.RightBasicBenchJMH_testAlgo_jmhTest::testAlgo_avgt_jmhStub@13 (line 186)
  0,04%    0x0000000002a2b6d0: mov     r10,qword ptr [rsp+70h]
  3,62%    0x0000000002a2b6d5: mov     r8d,dword ptr [r10+0ch]  ;*getfield value
                                                         ; - de.frank.jmh.intro.RightBasicBenchJMH::testAlgo@5 (line 54)
                                                         ; - de.frank.jmh.intro.generated.RightBasicBenchJMH_testAlgo_jmhTest::testAlgo_avgt_jmhStub@17 (line 186)
  0,35%    0x0000000002a2b6d9: mov     r10d,dword ptr [r10+14h]
                                                         ;*getfield algo
                                                         ; - de.frank.jmh.intro.RightBasicBenchJMH::testAlgo@1 (line 54)
                                                         ; - de.frank.jmh.intro.generated.RightBasicBenchJMH_testAlgo_jmhTest::testAlgo_avgt_jmhStub@17 (line 186)
  6,72%    0x0000000002a2b6dd: mov     r9d,dword ptr [r10+8h]  ; implicit exception: dispatches to 0x0000000002a2b785
  5,88%    0x0000000002a2b6e1: cmp     r9d,20019d3fh     ;   {metadata(&apos;de/frank/jmh/intro/RightBasicBenchJMH$AlgoImpl1&apos;)}
           0x0000000002a2b6e8: jne     2a2b73eh          ;*invokeinterface doWork
                                                         ; - de.frank.jmh.intro.RightBasicBenchJMH::testAlgo@8 (line 54)
                                                         ; - de.frank.jmh.intro.generated.RightBasicBenchJMH_testAlgo_jmhTest::testAlgo_avgt_jmhStub@17 (line 186)
  7,46%    0x0000000002a2b6ea: imul    r8d,r8d           ;*imul
                                                         ; - de.frank.jmh.intro.RightBasicBenchJMH$AlgoImpl1::doWork@2 (line 64)
                                                         ; - de.frank.jmh.intro.RightBasicBenchJMH::testAlgo@8 (line 54)
                                                         ; - de.frank.jmh.intro.generated.RightBasicBenchJMH_testAlgo_jmhTest::testAlgo_avgt_jmhStub@17 (line 186)
  0,11%    0x0000000002a2b6ee: mov     rdx,qword ptr [rsp+20h]
  3,64%    0x0000000002a2b6f3: call    28161a0h          ; OopMap{[96]=Oop [104]=Oop [112]=Oop [32]=Oop off=184}
                                                         ;*invokevirtual consume
                                                         ; - de.frank.jmh.intro.generated.RightBasicBenchJMH_testAlgo_jmhTest::testAlgo_avgt_jmhStub@20 (line 186)
                                                         ;   {optimized virtual_call}
  0,86%    0x0000000002a2b6f8: mov     r10,qword ptr [rsp+60h]
  7,62%    0x0000000002a2b6fd: movzx   r10d,byte ptr [r10+94h]  ;*getfield isDone
                                                         ; - de.frank.jmh.intro.generated.RightBasicBenchJMH_testAlgo_jmhTest::testAlgo_avgt_jmhStub@30 (line 188)
  3,73%    0x0000000002a2b705: add     rbp,1h            ; OopMap{[96]=Oop [104]=Oop [112]=Oop [32]=Oop off=201}
                                                         ;*ifeq
                                                         ; - de.frank.jmh.intro.generated.RightBasicBenchJMH_testAlgo_jmhTest::testAlgo_avgt_jmhStub@33 (line 188)
  0,13%    0x0000000002a2b709: test    dword ptr [0d00000h],eax
                                                         ;   {poll}
  7,31%    0x0000000002a2b70f: test    r10d,r10d
           0x0000000002a2b712: je      2a2b6d0h          ;*aload_1
                                                         ; - de.frank.jmh.intro.generated.RightBasicBenchJMH_testAlgo_jmhTest::testAlgo_avgt_jmhStub@36 (line 189)
           0x0000000002a2b714: mov     r10,53d56b80h
           0x0000000002a2b71e: call indirect r10         ;*invokestatic nanoTime
                                                         ; - de.frank.jmh.intro.generated.RightBasicBenchJMH_testAlgo_jmhTest::testAlgo_avgt_jmhStub@37 (line 189)
           0x0000000002a2b721: mov     r10,qword ptr [rsp+68h]
           0x0000000002a2b726: mov     qword ptr [r10+18h],rbp  ;*putfield measuredOps
                                                         ; - de.frank.jmh.intro.generated.RightBasicBenchJMH_testAlgo_jmhTest::testAlgo_avgt_jmhStub@52 (line 191)
           0x0000000002a2b72a: mov     qword ptr [r10+30h],rax  ;*putfield stopTime
....................................................................................................
 47,47%  <total for region 1>

     */
}
