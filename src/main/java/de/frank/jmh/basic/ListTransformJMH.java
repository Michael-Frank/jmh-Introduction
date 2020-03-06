package de.frank.jmh.basic;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.RandomStringUtils;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/*--
What: Transform a List<Foo> to a List<Bar> of same size

Result:
There is not much difference between implementations.
- if you can - always use the latest jvm available to you
- Base and base_sized are repetitive coding and an abstraction might be nice.
- Out of the box stream.toArray is very nice IF! the final size of the list/stream is known in advance
 - else the stream must allocate an growable List and copy to array in the final step
- the customer mapper is very short and precise as well and might be a good choice.

# VM version: JDK 1.8.0_232, OpenJDK 64-Bit Server VM, 25.232-b09
                                          score ns/op                  |            gc.alloc.rate.norm
               List-Size: 1       10       100        1000   avgScore  |      1       10       100        1000  avgAllocRate
objectTransform                                                        |
 base                    48      267     2.651      32.309      8.819  |    216    1.440    15.000     151.024      41.920 #surprisingly good
 baseSized               38      232     2.494      34.396      9.290  |    184    1.440    14.040     140.040      38.926 #sucks for some reason
 customList              41      259     2.845      32.850      8.999  |    184    1.440    14.040     140.040      38.926 #overal good perf and ease of use
 customArray             40      265     3.158      34.589      9.513  |    184    1.440    14.040     140.040      38.926 #sucks for some reason
 stream                  95      264     2.589      28.683      7.908  |    528    1.680    15.272     151.296      42.194 #stream sucks for small lists
 streamArray             63      258     2.256      25.644      7.055  |    360    1.616    14.216     140.216      39.102 #but stream.toArray is surprisingly good
 streamArrayAsList       67      263     2.225      28.775      7.833  |    384    1.640    14.240     140.240      39.126 #even with a Arrays.asList wrapper Stream.toArray is overall best
toString
 base                 1.137   10.927   114.161   1.262.425    347.163  |  3.904   38.312   376.704   3.775.168   1.048.522 #surprisingly good
 baseSized            1.127   11.531   124.295   1.206.804    335.939  |  3.864   34.776   378.016   3.762.080   1.044.684 #funny, in this case sized performs as expected
 customList           1.231   14.168   123.984   1.355.067    373.613  |  3.880   38.568   373.584   3.773.344   1.047.344 #now this one sucks for some reason???
 customArray          1.318   12.392   116.020   1.161.271    322.750  |  3.872   37.136   377.096   3.768.704   1.046.702 #funny - in this case and big lists Mapper2 does well
 stream               1.296   12.361   120.104   1.217.292    337.763  |  4.144   38.544   377.192   3.777.288   1.049.292 #plain stream is ok-ish,except for small lists
 streamArray          1.100   11.529   118.676   1.176.112    326.854  |  2.872   37.328   377.024   3.759.424   1.044.162 #surprisingly good
 streamArrayAsList    1.214   11.239   115.170   1.167.039    323.666  |  4.072   38.488   379.368   3.763.040   1.046.242 #even with a Arrays.asList wrapper Stream.toArray is overall bes

# VM version: JDK 13.0.1, OpenJDK 64-Bit Server VM, 13.0.1+9
                                          score ns/op                  |            gc.alloc.rate.norm
               List-Size: 1       10       100        1000   avgScore  |      1       10       100        1000  avgAllocRate
objectTransform                                                        |
 base                    32      224     2.603      30.969      8.457  |    216    1.440    15.000     151.024      41.920
 baseSized               27      222     2.336      31.058      8.411  |    184    1.440    14.040     140.040      38.926
 customList              28      263     2.474      28.337      7.776  |    184    1.440    14.040     140.040      38.926
 customArray             29      209     2.093      25.570      6.975  |    184    1.440    14.040     140.040      38.926 #in jdk13 mMyMapper2 does really well
 stream                  82      261     2.282      26.188      7.203  |    560    1.712    15.272     151.296      42.210 #plain stream is ok-ish,except for small lists
 streamArray             53      247     2.300      23.510      6.528  |    360    1.616    14.216     140.216      39.102 #surprisingly good
 streamArrayAsList       57      249     2.112      26.414      7.208  |    384    1.640    14.240     140.240      39.126
toString
 base                   941   10.307   106.229   1.107.182    306.165  |  1.536   20.616   205.656   2.052.120     569.982 #surprisingly good
 baseSized            1.001   10.181   111.876   1.086.606    302.416  |  2.096   20.632   201.912   2.022.208     561.712
 customList           1.033   10.399   110.598   1.125.831    311.965  |  2.104   20.608   204.832   2.024.208     562.938
 customArray          1.026   10.344   105.701   1.068.397    296.367  |  2.104   20.608   203.504   2.023.888     562.526 #in jdk13 mMyMapper2 does really well
 stream               1.030   10.827   114.082   1.095.126    305.266  |  2.408   19.712   202.848   2.036.032     565.250 #plain stream is really nice in this case
 streamArray          1.196   10.144   107.899   1.148.226    316.866  |  2.272   19.600   204.272   2.028.504     563.662
 streamArrayAsList    1.025   10.444   111.707   1.141.799    316.244  |  2.296   20.840   202.000   2.025.336     562.618

Raw: # VM version: JDK 1.8.0_232, OpenJDK 64-Bit Server VM, 25.232-b09
Benchmark             (size)  Mode  Cnt    Score  Error   Units gc.rate.norm Units
obj_CustomList             1  avgt   10       41      2   ns/op       184   B/op
obj_CustomList            10  avgt   10      259     21   ns/op      1440   B/op
obj_CustomList           100  avgt   10     2845    288   ns/op     14040   B/op
obj_CustomList          1000  avgt   10    32850   1531   ns/op    140040   B/op
obj_CustomArray            1  avgt   10       40      2   ns/op       184   B/op
obj_CustomArray           10  avgt   10      265     10   ns/op      1440   B/op
obj_CustomArray          100  avgt   10     3158     92   ns/op     14040   B/op
obj_CustomArray         1000  avgt   10    34589   1398   ns/op    140040   B/op
obj_base                   1  avgt   10       48      1   ns/op       216   B/op
obj_base                  10  avgt   10      267     25   ns/op      1440   B/op
obj_base                 100  avgt   10     2651    139   ns/op     15000   B/op
obj_base                1000  avgt   10    32309   1080   ns/op    151024   B/op
obj_sized                  1  avgt   10       38      1   ns/op       184   B/op
obj_sized                 10  avgt   10      232     12   ns/op      1440   B/op
obj_sized                100  avgt   10     2494    106   ns/op     14040   B/op
obj_sized               1000  avgt   10    34396   2030   ns/op    140040   B/op
obj_stream                 1  avgt   10       95      5   ns/op       528   B/op
obj_stream                10  avgt   10      264     18   ns/op      1680   B/op
obj_stream               100  avgt   10     2589    267   ns/op     15272   B/op
obj_stream              1000  avgt   10    28683   1694   ns/op    151296   B/op
obj_streamArray            1  avgt   10       63      4   ns/op       360   B/op
obj_streamArray           10  avgt   10      258     15   ns/op      1616   B/op
obj_streamArray          100  avgt   10     2256     65   ns/op     14216   B/op
obj_streamArray         1000  avgt   10    25644   1584   ns/op    140216   B/op
obj_streamArrayAsList      1  avgt   10       67      3   ns/op       384   B/op
obj_streamArrayAsList     10  avgt   10      263      4   ns/op      1640   B/op
obj_streamArrayAsList    100  avgt   10     2225    116   ns/op     14240   B/op
obj_streamArrayAsList   1000  avgt   10    28775   4594   ns/op    140240   B/op
toString_CustomList        1  avgt   10     1231     85   ns/op      3880   B/op
toString_CustomList       10  avgt   10    14168   1672   ns/op     38568   B/op
toString_CustomList      100  avgt   10   123984   7801   ns/op    373584   B/op
toString_CustomList     1000  avgt   10  1355067 202586   ns/op   3773344   B/op
toString_CustomArray       1  avgt   10     1318    165   ns/op      3872   B/op
toString_CustomArray      10  avgt   10    12392   1532   ns/op     37136   B/op
toString_CustomArray     100  avgt   10   116020   2608   ns/op    377096   B/op
toString_CustomArray    1000  avgt   10  1161271  34341   ns/op   3768704   B/op
toString_base              1  avgt   10     1137     26   ns/op      3904   B/op
toString_base             10  avgt   10    10927    231   ns/op     38312   B/op
toString_base            100  avgt   10   114161   3715   ns/op    376704   B/op
toString_base           1000  avgt   10  1262425  98267   ns/op   3775168   B/op
toString_sized             1  avgt   10     1127    111   ns/op      3864   B/op
toString_sized            10  avgt   10    11531    904   ns/op     34776   B/op
toString_sized           100  avgt   10   124295   6227   ns/op    378016   B/op
toString_sized          1000  avgt   10  1206804  90273   ns/op   3762080   B/op
toString_stream            1  avgt   10     1296     66   ns/op      4144   B/op
toString_stream           10  avgt   10    12361    647   ns/op     38544   B/op
toString_stream          100  avgt   10   120104   7598   ns/op    377192   B/op
toString_stream         1000  avgt   10  1217292  56182   ns/op   3777288   B/op
toString_streamArray       1  avgt   10     1100     31   ns/op      2872   B/op
toString_streamArray      10  avgt   10    11529    710   ns/op     37328   B/op
toString_streamArray     100  avgt   10   118676  13410   ns/op    377024   B/op
toString_streamArray    1000  avgt   10  1176112  22789   ns/op   3759424   B/op
toString_streamArrayAsL    1  avgt   10     1214     16   ns/op      4072   B/op
toString_streamArrayAsL   10  avgt   10    11239    156   ns/op     38488   B/op
toString_streamArrayAsL  100  avgt   10   115170   1400   ns/op    379368   B/op
toString_streamArrayAsL 1000  avgt   10  1167039  21375   ns/op   3763040   B/op

# VM version: JDK 13.0.1, OpenJDK 64-Bit Server VM, 13.0.1+9
Benchmark                  (size)  Mode  Cnt    Score  Units gc.alloc.norm
obj_CustomList                  1  avgt   10       28  ns/op       184   B/op
obj_CustomList                 10  avgt   10      263  ns/op      1440   B/op
obj_CustomList                100  avgt   10     2474  ns/op     14040   B/op
obj_CustomList               1000  avgt   10    28337  ns/op    140040   B/op
obj_CustomArray                 1  avgt   10       29  ns/op       184   B/op
obj_CustomArray                10  avgt   10      209  ns/op      1440   B/op
obj_CustomArray               100  avgt   10     2093  ns/op     14040   B/op
obj_CustomArray              1000  avgt   10    25570  ns/op    140040   B/op
obj_base                        1  avgt   10       32  ns/op       216   B/op
obj_base                       10  avgt   10      224  ns/op      1440   B/op
obj_base                      100  avgt   10     2603  ns/op     15000   B/op
obj_base                     1000  avgt   10    30969  ns/op    151024   B/op
obj_baseSized                   1  avgt   10       27  ns/op       184   B/op
obj_baseSized                  10  avgt   10      222  ns/op      1440   B/op
obj_baseSized                 100  avgt   10     2336  ns/op     14040   B/op
obj_baseSized                1000  avgt   10    31058  ns/op    140040   B/op
obj_stream                      1  avgt   10       82  ns/op       560   B/op
obj_stream                     10  avgt   10      261  ns/op      1712   B/op
obj_stream                    100  avgt   10     2282  ns/op     15272   B/op
obj_stream                   1000  avgt   10    26188  ns/op    151296   B/op
obj_streamArray                 1  avgt   10       53  ns/op       360   B/op
obj_streamArray                10  avgt   10      247  ns/op      1616   B/op
obj_streamArray               100  avgt   10     2300  ns/op     14216   B/op
obj_streamArray              1000  avgt   10    23510  ns/op    140216   B/op
obj_streamArrayAsList           1  avgt   10       57  ns/op       384   B/op
obj_streamArrayAsList          10  avgt   10      249  ns/op      1640   B/op
obj_streamArrayAsList         100  avgt   10     2112  ns/op     14240   B/op
obj_streamArrayAsList        1000  avgt   10    26414  ns/op    140240   B/op
toString_CustomList             1  avgt   10     1033  ns/op      2104   B/op
toString_CustomList            10  avgt   10    10399  ns/op     20608   B/op
toString_CustomList           100  avgt   10   110598  ns/op    204832   B/op
toString_CustomList          1000  avgt   10  1125831  ns/op   2024208   B/op
toString_CustomArray            1  avgt   10     1026  ns/op      2104   B/op
toString_CustomArray           10  avgt   10    10344  ns/op     20608   B/op
toString_CustomArray          100  avgt   10   105701  ns/op    203504   B/op
toString_CustomArray         1000  avgt   10  1068397  ns/op   2023888   B/op
toString_base                   1  avgt   10      941  ns/op      1536   B/op
toString_base                  10  avgt   10    10307  ns/op     20616   B/op
toString_base                 100  avgt   10   106229  ns/op    205656   B/op
toString_base                1000  avgt   10  1107182  ns/op   2052120   B/op
toString_baseSized              1  avgt   10     1001  ns/op      2096   B/op
toString_baseSized             10  avgt   10    10181  ns/op     20632   B/op
toString_baseSized            100  avgt   10   111876  ns/op    201912   B/op
toString_baseSized           1000  avgt   10  1086606  ns/op   2022208   B/op
toString_stream                 1  avgt   10     1030  ns/op      2408   B/op
toString_stream                10  avgt   10    10827  ns/op     19712   B/op
toString_stream               100  avgt   10   114082  ns/op    202848   B/op
toString_stream              1000  avgt   10  1095126  ns/op   2036032   B/op
toString_streamArray            1  avgt   10     1196  ns/op      2272   B/op
toString_streamArray           10  avgt   10    10144  ns/op     19600   B/op
toString_streamArray          100  avgt   10   107899  ns/op    204272   B/op
toString_streamArray         1000  avgt   10  1148226  ns/op   2028504   B/op
toString_streamArrayAsList      1  avgt   10     1025  ns/op      2296   B/op
toString_streamArrayAsList     10  avgt   10    10444  ns/op     20840   B/op
toString_streamArrayAsList    100  avgt   10   111707  ns/op    202000   B/op
toString_streamArrayAsList   1000  avgt   10  1141799  ns/op   2025336   B/op
 */
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1, jvmArgsAppend = {"-XX:+UseParallelGC", "-Xms1g", "-Xmx1g"})
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class ListTransformJMH {

    @State(Scope.Thread)
    public static class MyState {
        @Param({"0", "1", "10", "100", "1000"})
        int size;

        List<From> from;

        public MyState() {
        }

        public MyState(int size) {
            this.size = size;
            doSetup();
        }


        @Setup(Level.Trial)
        public void doSetup() {
            ArrayList<From> list = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                list.add(From.randomObj());
            }
            this.from = list;
        }
    }


    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()//
                .include(ListTransformJMH.class.getName() + ".*")//
                .addProfiler(GCProfiler.class)//
                .build();
        new Runner(opt).run();
    }

    @Benchmark
    public List<String> map_toString_base(MyState s) {
        List<String> copy = new ArrayList<>();
        for (From f : s.from) {
            copy.add(f.toString());
        }
        return copy;
    }

    @Benchmark
    public List<String> map_toString_baseSized(MyState s) {
        List<String> copy = new ArrayList<>(s.size);
        for (From f : s.from) {
            copy.add(f.toString());
        }
        return copy;
    }

    @Benchmark
    public List<String> map_toString_stream(MyState s) {
        return s.from.stream().map(From::toString).collect(Collectors.toList());
    }

    @Benchmark
    public String[] map_toString_streamArray(MyState s) {
        return s.from.stream().map(From::toString).toArray(String[]::new);
    }

    @Benchmark
    public List<String> map_toString_streamArrayAsList(MyState s) {
        return Arrays.asList(s.from.stream().map(From::toString).toArray(String[]::new));
    }

    @Benchmark
    public List<String> map_toString_CustomList(MyState s) {
        return transfrom_sizedList(s.from, From::toString);
    }

    @Benchmark
    public List<String> map_toString_CustomArray(MyState s) {
        return transfrom_array(s.from, From::toString, String[]::new);
    }


    ///ToObject


    @Benchmark
    public List<To> map_obj_base(MyState s) {
        List<To> copy = new ArrayList<>();
        for (From f : s.from) {
            copy.add(transfromObj(f));
        }
        return copy;
    }

    @Benchmark
    public List<To> map_obj_baseSized(MyState s) {
        List<To> copy = new ArrayList<>(s.size);
        for (From f : s.from) {
            copy.add(transfromObj(f));
        }
        return copy;
    }

    @Benchmark
    public List<To> map_obj_stream(MyState s) {
        return s.from.stream().map(ListTransformJMH::transfromObj).collect(Collectors.toList());
    }

    @Benchmark
    public To[] map_obj_streamArray(MyState s) {
        return s.from.stream().map(ListTransformJMH::transfromObj).toArray(To[]::new);
    }

    @Benchmark
    public List<To> map_obj_streamArrayAsList(MyState s) {
        return Arrays.asList(s.from.stream().map(ListTransformJMH::transfromObj).toArray(To[]::new));
    }

    @Benchmark
    public List<To> map_obj_CustomList(MyState s) {
        return transfrom_sizedList(s.from, ListTransformJMH::transfromObj);
    }

    @Benchmark
    public List<To> map_obj_CustomArray(MyState s) {
        return transfrom_array(s.from, ListTransformJMH::transfromObj, To[]::new);
    }


    public static <F, T> List<T> transfrom_sizedList(List<F> from, Function<F, T> mapper) {
        ArrayList<T> to = new ArrayList<>(from.size());
        for (F f : from) {
            to.add(mapper.apply(f));
        }
        return to;
    }

    public static <F, T> List<T> transfrom_array(List<F> from, Function<F, T> mapper, IntFunction<T[]> generator) {
        T[] to = generator.apply(from.size());
        for (int i = 0; i < from.size(); i++) {
            to[i] = mapper.apply(from.get(i));
        }
        return Arrays.asList(to);
    }

    public static To transfromObj(From f) {
        return To.builder()
                .a(f.a())
                .b(f.c())
                .c(f.c())
                .d(f.d())
                .e(f.e())
                .f(f.f())
                .g(f.g())
                .h(f.h())
                .i(f.i())
                .j(f.j())
                .k(new ArrayList<>(f.k()))
                .build();
    }


    @Data
    @Accessors(chain = true, fluent = true)
    @Builder
    public static class From {
        int a, b, c, d;
        String e, f, g;
        double h, i, j;
        List<String> k;

        public static From randomObj() {
            Random r = ThreadLocalRandom.current();

            return From.builder()
                    .a(r.nextInt())
                    .b(r.nextInt())
                    .c(r.nextInt())
                    .d(r.nextInt())
                    .e(RandomStringUtils.random(15, 0, 0, true, true, null, r))
                    .f(RandomStringUtils.random(15, 0, 0, true, true, null, r))
                    .g(RandomStringUtils.random(15, 0, 0, true, true, null, r))
                    .h(r.nextDouble())
                    .i(r.nextDouble())
                    .j(r.nextDouble())
                    .k(IntStream.range(0, 5)
                            .mapToObj(idx ->
                                    RandomStringUtils.random(15, 0, 0, true, true, null, r))
                            .collect(Collectors.toList()))
                    .build();

        }
    }

    @Data
    @Accessors(chain = true, fluent = true)
    @Builder
    public static class To {
        int a, b, c, d;
        String e, f, g;
        double h, i, j;
        List<String> k;
    }
}
