package de.frank.jmh.basic;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/*--
 DANGER! this benchmark is heavily flawed!
 Measuring simple looping is skewed - re test with your own application code inside the loops.


 All numbers in: nanoseconds per operation

 Benchmark in ns/op      /ListSize->    10  1.000   10.0000  1.000.000    Average
 forI_complexList                     0,14  15,24  2.365,01  31.435,36   8.453,94
 forI_complexArray                    0,12  13,63  2.368,53  31.311,63   8.423,48
 forEach_complexList                  0,15  16,56  2.450,98  32.373,94   8.710,41
 forEach_complexArray                 0,14  14,75  2.440,92  32.145,45   8.650,31
 stream_complexList_map               0,21  17,05  2.462,19  31.589,82   8.517,31
 stream_complexList_foreach           0,17  15,06  2.336,50  30.482,10   8.208,47
 stream_complexArray_map              0,21  17,21  2.462,45  31.645,96   8.531,46
 stream_complexArray_foreach          0,16  15,08  2.324,87  30.511,15   8.212,82


 ##DANGER## this only shows how well the JVM can optimize very! simplistic loops
 # note the 0.01ns which is not possible.
 forI_List                            0,01   0,71     87,01   1.893,46     495,30
 forI_intArray                        0,01   0,31     30,33     313,31      85,99
 forEach_List                         0,02   0,78     89,79   1.939,27     507,46
 forEach_intArray                     0,01   0,31     30,44     313,21      85,99


 stream_List_sumCollector             0,04   1,31    112,30   3.638,44     938,02
 stream_List_mapToIntSum              0,05   0,88    592,99   6.896,60   1.872,63
 stream_List_mapToIntSum_parallel     7,63  15,46    138,48   1.220,34     345,48
 stream_List_mapToIntReduce           0,05   0,88    106,80   1.964,14     517,97
 stream_List_mapToIntReduce_parallel  8,08  15,28    138,84   1.216,95     344,79
 stream_intArray_sum                  0,04   2,31     30,25     312,93      86,38
 stream_intArray_sum_parallel         7,96  13,89     21,16      84,47      31,87
 stream_intArray_reduce               0,04   2,62     30,51     316,12      87,32
 stream_intArray_reduce_parallel      7,41  13,89     21,10      84,56      31,74

 idioticLoop                          0,01   0,31     30,49     314,25      86,27

*/

/**
 * @author Michael Frank
 * @version 1.0 21.01.2017
 */
@Fork(3)
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
public class LoopingAndStreamsJMH {
    /**
     * --
     * ######### WARNING ##############
     * Do not confuse this benchmark with the anti pattern in JMHSample_11_Loops
     * This benchmark should show you on purpose how you how well (very simple) loops are optimized by the jvm
     * <data>
     * You will get totally different results with complex operations arrays/lists of objects
     */
    @Param({"10", "1000", "100000", "1000000"})
    public int size;

    private int[] data;
    private List<Integer> dataAsList;
    private ComplexData[] complexData;
    private List<ComplexData> complexDataList;


    private String key = "1";

    static class ComplexData {
        private static final AtomicInteger INSTANCES = new AtomicInteger();
        //just a map of {"0":"0", "1":"1", "2":"2"};
        private static final Map<String, String> COMMON_DATA = IntStream.rangeClosed(0, 3).mapToObj(Integer::toString).collect(Collectors.toMap(x -> x, x -> x));

        Map<String, String> data;

        public ComplexData() {
            data = new HashMap<>(COMMON_DATA);
            String unique = Integer.toString(INSTANCES.incrementAndGet());
            data.put(unique, unique);
        }

    }

    @Setup
    public void setup() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        data = new int[size];
        for (int i = 0; i < data.length; i++) {
            data[i] = r.nextInt(size);
        }
        dataAsList = Arrays.stream(data).boxed().collect(Collectors.toList());

        complexData = new ComplexData[size];
        for (int i = 0; i < data.length; i++) {
            complexData[i] = new ComplexData();
        }
        complexDataList = Arrays.asList(complexData);
    }

    @SuppressWarnings("ForLoopReplaceableByForEach")

    @Benchmark
    public void baseline_singleOp(Blackhole b) {
        b.consume(complexData[0].data.get(key));
    }

    @Benchmark
    public void forI_complexArray(Blackhole b) {
        for (int i = 0; i < complexData.length; i++) {
            b.consume(complexData[i].data.get(key));
        }
    }

    @Benchmark
    public void forEach_complexArray(Blackhole b) {
        for (ComplexData value : complexData) {
            b.consume(value.data.get(key));
        }
    }


    @SuppressWarnings("ForLoopReplaceableByForEach")
    @Benchmark
    public void forI_complexList(Blackhole b) {
        for (int i = 0; i < complexDataList.size(); i++) {
            b.consume(complexDataList.get(i).data.get(key));
        }
    }

    @Benchmark
    public void forEach_complexList(Blackhole b) {
        for (ComplexData value : complexDataList) {
            b.consume(value.data.get(key));
        }
    }

    @Benchmark
    public void stream_complexArray_foreach(Blackhole b) {
        Arrays.stream(complexData).forEach(x -> b.consume(x.data.get(key)));
    }

    @Benchmark
    public void stream_complexList_foreach(Blackhole b) {
        complexDataList.stream().forEach(x -> b.consume(x.data.get(key)));
    }

    @Benchmark
    public void stream_complexArray_map(Blackhole b) {
        Arrays.stream(complexData).map(x -> x.data.get(key)).forEach(b::consume);
    }

    @Benchmark
    public void stream_complexList_map(Blackhole b) {
        complexDataList.stream().map(x -> x.data.get(key)).forEach(b::consume);
    }


    @SuppressWarnings("ForLoopReplaceableByForEach")
    @Benchmark
    public int forI_intArray() {
        int sum = 0;
        for (int i = 0; i < data.length; i++) {
            sum += data[i];
        }
        return sum;
    }

    @Benchmark
    public int forEach_intArray() {
        int sum = 0;
        for (int value : data) {
            sum += value;
        }
        return sum;
    }


    @Benchmark
    public int forI_List() {
        int sum = 0;
        for (int i = 0; i < dataAsList.size(); i++) {
            sum += dataAsList.get(i);
        }
        return sum;
    }

    @Benchmark
    public int forEach_List() {
        int sum = 0;
        for (int value : dataAsList) {
            sum += value;
        }
        return sum;
    }

    @Benchmark
    public int stream_intArray_sum() {
        return Arrays.stream(data).sum();
    }

    @Benchmark
    public int stream_intArray_sum_parallel() {
        return Arrays.stream(data).parallel().sum();
    }

    @Benchmark
    public int stream_intArray_reduce() {
        return Arrays.stream(data).reduce(0, Integer::sum);
    }

    @Benchmark
    public int stream_intArray_reduce_parallel() {
        return Arrays.stream(data).parallel().reduce(0, Integer::sum);
    }


    @Benchmark
    public int stream_List_mapToIntSum() {
        return dataAsList.stream().mapToInt(Integer::intValue).sum();
    }

    @Benchmark
    public int stream_List_sumCollector() {
        //intentionally NOT ,mapToInt(Integer::intValue).sum()  see stream_List_mapToIntSum
        return dataAsList.stream().collect(Collectors.summingInt(Integer::intValue));
    }

    @Benchmark
    public int stream_List_mapToIntSum_parallel() {
        return dataAsList.parallelStream().parallel().mapToInt(Integer::intValue).sum();
    }

    @Benchmark
    public int stream_List_mapToIntReduce() {
        return dataAsList.stream().mapToInt(Integer::intValue).reduce(0, Integer::sum);
    }

    @Benchmark
    public int stream_List_mapToIntReduce_parallel() {
        return dataAsList.parallelStream().mapToInt(Integer::intValue).reduce(0, Integer::sum);
    }


    @Benchmark
    public int idioticLoop() {
        // seriously, this is fucking stupid.

        //This "habit" can sometimes be found in very old code
        // The reasoning was: "java performs expensive array bounds checks for i in each iteration"
        //
        // This is INVALID today as the compiler will perform "bounds check elimination" for you (if applicable)
        // In contrast: Throwing a AIOOB exception is very expensive - especially the "fillInStackTrace" part
        // But the JVM is smart again and just throws out your bad "exception" code
        // and inserts a bounds check "if i < size" itself
        // so while not bad for your performance - the code is just ridiculous.

        int sum = 0;
        try {
            for (int i = 0; ; i++) {
                sum += data[i];
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            // end of loop
        }
        return sum;
    }


    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                // runs all tests
                .include(LoopingAndStreamsJMH.class.getName())
                // DEFAULT
                .jvmArgsAppend("-XX:+UnlockDiagnosticVMOptions")
                // #########
                // COMPILER
                // #########
                // make sure we dont see compiling of our benchmark code during
                // measurement.
                // if you see compiling => more warmup
                // .jvmArgsAppend("-XX:+PrintCompilation")
                //
                // .jvmArgsAppend("-XX:+PrintInlining")
                // .jvmArgsAppend("-XX:+PrintAssembly")
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
                //
                // .jvmArgsAppend("-XX:+UnlockCommercialFeatures")
                // .jvmArgsAppend("-XX:+FlightRecorder")
                //
                // .jvmArgsAppend("-XX:+UnlockDiagnosticVMOptions")
                // .jvmArgsAppend("-XX:+PrintSafepointStatistics")
                // .jvmArgsAppend("-XX:+DebugNonSafepoints")
                //
                // required for external profilers like "perf" to show java
                // frames in their traces
                // .jvmArgsAppend("-XX:+PreserveFramePointer")
                .build();
        new Runner(opt).run();

    }
}
