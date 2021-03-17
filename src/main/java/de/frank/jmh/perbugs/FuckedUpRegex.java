package de.frank.jmh.perbugs;

import org.apache.commons.lang3.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.*;
import org.openjdk.jmh.runner.options.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;


/*--
Cold-singleshot times
Benchmark                                    Mode  Cnt  Score   Error  Units
matchesAndGroup_base            ss  100  4,286 ± 0,695  ms/op
matchesAndGroup_fixedPattern    ss  100  4,422 ± 0,541  ms/op

#Single Threaded
Benchmark                      Mode  Cnt      Score    Error  Units
matchesAndGroup_base          thrpt   30     79.215 ±   1812  ops/s # baseline
matchesAndGroup_fixedPattern  thrpt   30    128.104 ±   2145  ops/s # one simple non-required A-Z boi gone ... stonks!
matchesAndSplit_v1            thrpt   30    130.866 ±   1882  ops/s # ..can do slightly better (regex still sucks)
matchesAndSplit_v2            thrpt   30    133.811 ±   2744  ops/s # ..and more (regex still sucks)
split_customVerify            thrpt   30    182.452 ±   5860  ops/s # even a dead simple unoptimized looping verifier is faster...
split_noVerify                thrpt   30 10.687.486 ± 219064  ops/s # or just ditch that useless regex...profit

#Contented 16 threads
Benchmark                      Mode  Cnt         Score       Error  Units
matchesAndGroup_base          thrpt   30    373.733 ±  8470,758  ops/s
matchesAndGroup_fixedPattern  thrpt   30    579.725 ± 17590,397  ops/s
matchesAndSplit_v1            thrpt   30    598.046 ±  7313,426  ops/s
matchesAndSplit_v2            thrpt   30    570.064 ± 11290,081  ops/s
split_customVerify            thrpt   30    751.407 ± 33392,378  ops/s # even a dead simple unoptimized looping verifier is faster...
split_noVerify                thrpt   30 11.968.489 ± 96009,280  ops/s # hitting gc saturation?

 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Threads(1)
@Fork(3)
@Warmup(iterations = 4, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Thread)
public class FuckedUpRegex {
    private static final String BEARER = "bearer ";

    private static final Pattern authorizationPattern = Pattern.compile(
            "^Bearer (?<token>[a-zA-Z0-9-._~+/]+=*)$",
            Pattern.CASE_INSENSITIVE);

    //remove the redundant uppercase class A-Z, as we compare CASE_INSENSITIVE anyway
    private static final Pattern authorizationPatternFixed = Pattern.compile(
            "^Bearer (?<token>[a-z0-9-._~+/]+=*)$",
            Pattern.CASE_INSENSITIVE);

    //we dont use .group("token") with this pattern. After matches() we just use the substring after "Bearer " as the token.
    private static final Pattern authorizationPatternOptimized = Pattern.compile(
            "^Bearer [a-z0-9-._~+/]+=*$",
            Pattern.CASE_INSENSITIVE);

    //just verify the token. Other chores done by substring and equals checks.
    private static final Pattern authorizationPatternOptimized2 = Pattern.compile(
            "[a-z0-9-._~+/]+=*",
            Pattern.CASE_INSENSITIVE);

    private static final String EXPECTED =
            "eyJhbGciOiJSUzI1NiIsImtpZCI6IjljZTVlNmY1MzBiNDkwMTFiYjg0YzhmYWExZWM1NGM1MTc1N2I2NTgiLCJ0eXAiOiJKV1QifQ.eyJhdXRob3JpdGllcyI6WyJTWVNURU0iLCJERVNJR05FUiIsIlRFU1RFUiIsIkFUVEVOREVFIiwiVklQX0FUVEVOREVFIl0sImlzcyI6Imh0dHBzOi8vc2VjdXJldG9rZW4uZ29vZ2xlLmNvbS92dmVudWUtYzJhNjdjNGI1Mzg1YzQ1NSIsImF1ZCI6InZ2ZW51ZS1jMmE2N2M0YjUzODVjNDU1IiwiYXV0aF90aW1lIjoxNjEyMzkxNjgwLCJ1c2VyX2lkIjoidGVzdC1kZXNpZ25lckB2aXJ0dWFsdmVudWUuY29tIiwic3ViIjoidGVzdC1kZXNpZ25lckB2aXJ0dWFsdmVudWUuY29tIiwiaWF0IjoxNjEyMzkxNjgwLCJleHAiOjE2MTIzOTUyODAsImVtYWlsIjoidGVzdC1kZXNpZ25lckB2aXJ0dWFsdmVudWUuY29tIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsImZpcmViYXNlIjp7ImlkZW50aXRpZXMiOnsiZW1haWwiOlsidGVzdC1kZXNpZ25lckB2aXJ0dWFsdmVudWUuY29tIl19LCJzaWduX2luX3Byb3ZpZGVyIjoicGFzc3dvcmQifX0.nmoV5qicmjree4EUHWXUUEZwvakbRQDokfMK-DygSh5zZlyyYsjyVbEpqRDLL4yEesRdxnsJyKoghspCf_DkEbhCUEol0hUezkGh68TnKNI0CHvW2z62p1AJYMIbl4lOIImV16NXPdwzXpETx0X8SvlTOoZVtS5P2WxI2kgsBPP6KQe2QzEhxvpWUwIV7oafu1K6Cbbc6XYc8v_19M1pX8MuW_aXAAw16w7_fp5lQSLc7uCqwJiK1YHOloE2raUniuedtPdvHc9hpHQDq2aVc53ts3UN5351S-rNErsOBcWFrG0G_iGIjPKaIdL6vXYSBPIx1cb7Ub8aRc617bM-PA";


    String input = "Bearer " + EXPECTED;


    @Benchmark
    public String matchesAndGroup_base() {
        var matcher = authorizationPattern.matcher(input);
        return matcher.matches() ? matcher.group("token") : null;
    }

    @Benchmark
    public String matchesAndGroup_fixedPattern() {
        var matcher = authorizationPatternFixed.matcher(input);
        return matcher.matches() ? matcher.group("token") : null;
    }

    @Benchmark
    public String matchesAndSplit_v1() {
        var matcher = authorizationPatternOptimized.matcher(input);
        return matcher.matches() ? getBearerToken(input) : null;
    }


    @Benchmark
    public String matchesAndSplit_v2() {
        if (startsWithBearerIgnoreCase(input)) {
            String token = getBearerToken(input);
            boolean validToken = authorizationPatternOptimized2.matcher(token).matches();
            return validToken ? token : null;
        }
        return null;
    }


    @Benchmark
    public String split_noVerify() {
        if (startsWithBearerIgnoreCase(input)) {
            return getBearerToken(input);
        }
        return null;
    }

    private static final char[] ALLOWED = "abcdefghijklmnopqrstuvwxyz1234567890=.-_~" .toCharArray();

    @Benchmark
    public String split_customVerify() {
        if (startsWithBearerIgnoreCase(input)) {
            String token = getBearerToken(input);
            if (containsOnlyAllowedChars(token, true, ALLOWED)) {
                return token;
            }
        }
        return null;
    }

    public boolean containsOnlyAllowedChars(String input, boolean lowercase, char[] allowed) {
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (lowercase) {
                c = Character.toLowerCase(c);
            }
            boolean matched = false;
            for (char allow : allowed) {
                if (c == allow) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return false;
            }
        }
        return true;
    }

    private static String getBearerToken(String input) {
        return input.substring(BEARER.length());
    }

    private static boolean startsWithBearerIgnoreCase(String input) {
        boolean hasMinLength = input != null && input.length() >= (BEARER.length() + 1);
        return hasMinLength && BEARER.equalsIgnoreCase(input.substring(0, BEARER.length()));
    }


    public static void main(String[] args) throws RunnerException {
        verifyAllImpls(EXPECTED);

        //System.setProperty("jmh.perfasm.xperf.dir", "C:\\Program Files (x86)\\Windows Kits\\10\\Windows Performance Toolkit");
        new Runner(new OptionsBuilder()
                           .include(FuckedUpRegex.class.getName() + ".*")
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

    public static void verifyAllImpls(String expected) {
        FuckedUpRegex instance = new FuckedUpRegex();
        Arrays.stream(FuckedUpRegex.class.getDeclaredMethods())
              .filter(m -> m.getDeclaredAnnotation(Benchmark.class) != null)
              .forEach(m -> {
                  try {
                      String result = String.valueOf(m.invoke(instance));
                      System.out.println(
                              m.getName() + " -> '" + StringUtils.substring(result, 0, 10) + "...(truncated)'");
                      if (!expected.equals(result)) {
                          throw new RuntimeException(
                                  "benchmark method '" + m.getName() + "'did not produce the expected result! : " +
                                  result);
                      }

                  } catch (Exception e) {
                      throw new RuntimeException(e);
                  }
              });
    }
}
