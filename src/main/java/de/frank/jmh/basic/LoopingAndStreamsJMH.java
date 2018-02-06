
package de.frank.jmh.basic;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**--
 Benchmark               /ListSize->    10   1000    100000    1000000    Average
 stream_List_sumCollector             0,04   1,31    112,30   3.638,44     938,02
 stream_List_mapToIntSum_parallel     7,63  15,46    138,48   1.220,34     345,48
 stream_List_mapToIntSum              0,05   0,88    592,99   6.896,60   1.872,63
 stream_List_mapToIntReduce_parallel  8,08  15,28    138,84   1.216,95     344,79
 stream_List_mapToIntReduce           0,05   0,88    106,80   1.964,14     517,97
 stream_intArray_sum_parallel         7,96  13,89     21,16      84,47      31,87
 stream_intArray_sum                  0,04   2,31     30,25     312,93      86,38
 stream_intArray_reduce_parallel      7,41  13,89     21,10      84,56      31,74
 stream_intArray_reduce               0,04   2,62     30,51     316,12      87,32
 stream_complexList_map               0,21  17,05  2.462,19  31.589,82   8.517,31
 stream_complexList_foreach           0,17  15,06  2.336,5   30.482,10   8.208,47
 stream_complexArray_map              0,21  17,21  2.462,45  31.645,96   8.531,46
 stream_complexArray_foreach          0,16  15,08  2.324,87  30.511,15   8.212,82
 idioticLoop                          0,01   0,31     30,49     314,25      86,27
 forI_List                            0,01   0,71     87,01   1.893,46     495,30
 forI_intArray                        0,01   0,31     30,33     313,31      85,99
 forI_complexList                     0,14  15,24  2.365,01  31.435,36   8.453,94
 forI_complexArray                    0,12  13,63  2.368,53  31.311,63   8.423,48
 forEach_List                         0,02   0,78     89,79   1.939,27     507,46
 forEach_intArray                     0,01   0,31     30,44     313,21      85,99
 forEach_complexList                  0,15  16,56  2.450,98  32.373,94   8.710,41
 forEach_complexArray                 0,14  14,75  2.440,92  32.145,45   8.650,31

 Benchmark                             (size)  Mode  Cnt      Score      Error  Units
 forEach_List                              10  avgt   10      0,016 ±    0,001  us/op
 forEach_List                            1000  avgt   10      0,779 ±    0,024  us/op
 forEach_List                          100000  avgt   10     89,794 ±    1,259  us/op
 forEach_List                         1000000  avgt   10   1939,268 ±   97,095  us/op
 forEach_complexArray                      10  avgt   10      0,143 ±    0,004  us/op
 forEach_complexArray                    1000  avgt   10     14,749 ±    0,291  us/op
 forEach_complexArray                  100000  avgt   10   2440,916 ±  235,483  us/op
 forEach_complexArray                 1000000  avgt   10  32145,448 ± 1191,279  us/op
 forEach_complexList                       10  avgt   10      0,154 ±    0,009  us/op
 forEach_complexList                     1000  avgt   10     16,559 ±    0,647  us/op
 forEach_complexList                   100000  avgt   10   2450,981 ±   89,490  us/op
 forEach_complexList                  1000000  avgt   10  32373,937 ± 1496,225  us/op
 forEach_intArray                          10  avgt   10      0,007 ±    0,001  us/op
 forEach_intArray                        1000  avgt   10      0,308 ±    0,003  us/op
 forEach_intArray                      100000  avgt   10     30,435 ±    0,390  us/op
 forEach_intArray                     1000000  avgt   10    313,212 ±    4,696  us/op
 forI_List                                 10  avgt   10      0,014 ±    0,001  us/op
 forI_List                               1000  avgt   10      0,714 ±    0,007  us/op
 forI_List                             100000  avgt   10     87,006 ±    0,983  us/op
 forI_List                            1000000  avgt   10   1893,455 ±   13,293  us/op
 forI_complexArray                         10  avgt   10      0,123 ±    0,004  us/op
 forI_complexArray                       1000  avgt   10     13,631 ±    0,210  us/op
 forI_complexArray                     100000  avgt   10   2368,528 ±   17,609  us/op
 forI_complexArray                    1000000  avgt   10  31311,627 ±  450,944  us/op
 forI_complexList                          10  avgt   10      0,140 ±    0,004  us/op
 forI_complexList                        1000  avgt   10     15,244 ±    0,151  us/op
 forI_complexList                      100000  avgt   10   2365,007 ±   19,434  us/op
 forI_complexList                     1000000  avgt   10  31435,364 ±  364,273  us/op
 forI_intArray                             10  avgt   10      0,007 ±    0,001  us/op
 forI_intArray                           1000  avgt   10      0,309 ±    0,004  us/op
 forI_intArray                         100000  avgt   10     30,329 ±    0,117  us/op
 forI_intArray                        1000000  avgt   10    313,311 ±    5,146  us/op
 idioticLoop                               10  avgt   10      0,008 ±    0,001  us/op
 idioticLoop                             1000  avgt   10      0,309 ±    0,003  us/op
 idioticLoop                           100000  avgt   10     30,493 ±    0,210  us/op
 idioticLoop                          1000000  avgt   10    314,252 ±    5,094  us/op
 stream_List_mapToIntReduce                10  avgt   10      0,052 ±    0,001  us/op
 stream_List_mapToIntReduce              1000  avgt   10      0,880 ±    0,016  us/op
 stream_List_mapToIntReduce            100000  avgt   10    106,804 ±    1,088  us/op
 stream_List_mapToIntReduce           1000000  avgt   10   1964,138 ±   30,768  us/op
 stream_List_mapToIntReduce_parallel       10  avgt   10      8,082 ±    0,170  us/op
 stream_List_mapToIntReduce_parallel     1000  avgt   10     15,277 ±    0,135  us/op
 stream_List_mapToIntReduce_parallel   100000  avgt   10    138,838 ±    0,791  us/op
 stream_List_mapToIntReduce_parallel  1000000  avgt   10   1216,951 ±  267,542  us/op
 stream_List_mapToIntSum                   10  avgt   10      0,052 ±    0,001  us/op
 stream_List_mapToIntSum                 1000  avgt   10      0,882 ±    0,009  us/op
 stream_List_mapToIntSum               100000  avgt   10    592,990 ±    4,905  us/op
 stream_List_mapToIntSum              1000000  avgt   10   6896,603 ±   44,491  us/op
 stream_List_mapToIntSum_parallel          10  avgt   10      7,626 ±    0,160  us/op
 stream_List_mapToIntSum_parallel        1000  avgt   10     15,459 ±    0,040  us/op
 stream_List_mapToIntSum_parallel      100000  avgt   10    138,479 ±    0,784  us/op
 stream_List_mapToIntSum_parallel     1000000  avgt   10   1220,337 ±  233,975  us/op
 stream_List_sumCollector                  10  avgt   10      0,041 ±    0,001  us/op
 stream_List_sumCollector                1000  avgt   10      1,308 ±    0,018  us/op
 stream_List_sumCollector              100000  avgt   10    112,295 ±    8,113  us/op
 stream_List_sumCollector             1000000  avgt   10   3638,444 ± 1782,402  us/op
 stream_complexArray_foreach               10  avgt   10      0,162 ±    0,002  us/op
 stream_complexArray_foreach             1000  avgt   10     15,080 ±    0,311  us/op
 stream_complexArray_foreach           100000  avgt   10   2324,873 ±   59,349  us/op
 stream_complexArray_foreach          1000000  avgt   10  30511,147 ±  205,739  us/op
 stream_complexArray_map                   10  avgt   10      0,213 ±    0,007  us/op
 stream_complexArray_map                 1000  avgt   10     17,206 ±    0,196  us/op
 stream_complexArray_map               100000  avgt   10   2462,452 ±   44,033  us/op
 stream_complexArray_map              1000000  avgt   10  31645,963 ±  366,983  us/op
 stream_complexList_foreach                10  avgt   10      0,165 ±    0,003  us/op
 stream_complexList_foreach              1000  avgt   10     15,060 ±    0,262  us/op
 stream_complexList_foreach            100000  avgt   10   2336,542 ±   47,220  us/op
 stream_complexList_foreach           1000000  avgt   10  30482,101 ±  253,728  us/op
 stream_complexList_map                    10  avgt   10      0,206 ±    0,013  us/op
 stream_complexList_map                  1000  avgt   10     17,046 ±    0,198  us/op
 stream_complexList_map                100000  avgt   10   2462,185 ±   25,568  us/op
 stream_complexList_map               1000000  avgt   10  31589,821 ±  220,494  us/op
 stream_intArray_reduce                    10  avgt   10      0,042 ±    0,001  us/op
 stream_intArray_reduce                  1000  avgt   10      2,616 ±    0,021  us/op
 stream_intArray_reduce                100000  avgt   10     30,505 ±    0,267  us/op
 stream_intArray_reduce               1000000  avgt   10    316,119 ±    2,321  us/op
 stream_intArray_reduce_parallel           10  avgt   10      7,410 ±    0,017  us/op
 stream_intArray_reduce_parallel         1000  avgt   10     13,889 ±    0,200  us/op
 stream_intArray_reduce_parallel       100000  avgt   10     21,098 ±    0,192  us/op
 stream_intArray_reduce_parallel      1000000  avgt   10     84,555 ±    0,314  us/op
 stream_intArray_sum                       10  avgt   10      0,040 ±    0,001  us/op
 stream_intArray_sum                     1000  avgt   10      2,308 ±    0,015  us/op
 stream_intArray_sum                   100000  avgt   10     30,250 ±    0,190  us/op
 stream_intArray_sum                  1000000  avgt   10    312,934 ±    3,177  us/op
 stream_intArray_sum_parallel              10  avgt   10      7,960 ±    0,613  us/op
 stream_intArray_sum_parallel            1000  avgt   10     13,890 ±    0,218  us/op
 stream_intArray_sum_parallel          100000  avgt   10     21,155 ±    0,403  us/op
 stream_intArray_sum_parallel         1000000  avgt   10     84,472 ±    0,439  us/op

 * @author Michael Frank
 * @version 1.0 21.01.2017
 */
@Fork(3)
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
public class LoopingAndStreamsJMH {
    /**--
     * ######### WARNING ##############
     * Do not confuse this benchmark with JMHSample_11_Loops
     * This benchmark will foremost show you how well (very simple) loops are optimized by the jvm and how bad streams are.
     *
     * You will get totally different results with complex operations arrays/lists of objects
     */
    @Param({"10", "1000", "100000", "1000000"})
    public int size;

    private int[] data;
    private List<Integer> dataAsList;
    private ComplexData[] complexData;
    private List<ComplexData> complexDataList;

    private String key="1";

    static class ComplexData{
        private static final Map<String,String> DEF=new HashMap<>();
        private static final AtomicInteger INSTANCES=new AtomicInteger();
        static{
            for (int i = 0; i < 3; i++) {
                String k = Integer.toString(i);
                DEF.put(k,k);
            }
        }

        Map<String,String> p;
        public ComplexData(){
           p=new HashMap<String,String>(DEF);
           String unique=Integer.toString(INSTANCES.incrementAndGet());
           p.put(unique,unique);
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
        complexDataList  = Arrays.asList(complexData);
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

    @SuppressWarnings("ForLoopReplaceableByForEach")
    @Benchmark
    public void forI_complexArray(Blackhole b) {
        for (int i = 0; i < complexData.length; i++) {
            b.consume(complexData[i].p.get(key));
        }
    }

    @Benchmark
    public void forEach_complexArray(Blackhole b) {
        for (ComplexData value : complexData) {
            b.consume(value.p.get(key));
        }
    }

    @SuppressWarnings("ForLoopReplaceableByForEach")
    @Benchmark
    public void forI_complexList(Blackhole b) {
        for (int i = 0; i < complexDataList.size(); i++) {
            b.consume(complexDataList.get(i).p.get(key));
        }
    }

    @Benchmark
    public void forEach_complexList(Blackhole b) {
        for (ComplexData value : complexDataList) {
            b.consume(value.p.get(key));
        }
    }

    @Benchmark
    public void stream_complexArray_foreach(Blackhole b) {
        Arrays.stream(complexData).forEach(x->b.consume(x.p.get(key)));
    }

    @Benchmark
    public void stream_complexList_foreach(Blackhole b) {
       complexDataList.stream().forEach(x->b.consume(x.p.get(key)));
    }

    @Benchmark
    public void stream_complexArray_map(Blackhole b) {
        Arrays.stream(complexData).map(x->x.p.get(key)).forEach(b::consume);
    }

    @Benchmark
    public void stream_complexList_map(Blackhole b) {
       complexDataList.stream().map(x->x.p.get(key)).forEach(b::consume);
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
    public int stream_List_mapToIntSum() {
        return dataAsList.stream().mapToInt(Integer::intValue).sum();
    }

    @Benchmark
    public int stream_List_sumCollector() {
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

        //This "habbit" can sometimes be found in very old code
        // -> the reasoning was: "java performs array bounds checks for i in each iteration
        // This is INVALID today.
        //
        // The compiler will perform "bounds check elimination" for you (if applicable)
        // Iin contrast:
        // Throwing a AIOOB exception is very expensive - especially the
        // "fillInStackTrace" part
        // But the jvm is smart and just throws out your bad "exception" code
        // and inserts a bounds check "if i < size" itself

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
                // .jvmArgsAppend("-XX:+PerserveFramePointer")
                .build();
        new Runner(opt).run();

    }
}
