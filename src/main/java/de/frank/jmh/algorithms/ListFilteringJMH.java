package de.frank.jmh.algorithms;

import com.yevdo.jwildcard.JWildcard;
import org.jetbrains.annotations.NotNull;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/*--
 Task: ListFiltering - remove unwanted stuff.
 For the sake of fairness, each impl has to regard the input list as immutable.
 Returning the original list is an allowed optimization in case of no element requires removal.

WINNER: removeIf() - (internally makes 2 passes - first: scan and mark in bitset, second: shift elements)

enchmark                                              Mode  Cnt         Score        Error  Units
ListFilteringJMH.MixedWorkload.removeIfArrayList      thrpt   30  10897305,211 ± 398831,925  ops/s
ListFilteringJMH.MixedWorkload.removeByIterArrayList  thrpt   30   9855625,240 ±  93163,750  ops/s
ListFilteringJMH.MixedWorkload.removeIfLinkedList     thrpt   30   6924750,856 ±  76589,106  ops/s
ListFilteringJMH.MixedWorkload.filterGeneric          thrpt   30   6853871,520 ± 358069,635  ops/s
ListFilteringJMH.MixedWorkload.filterLoopLazy         thrpt   30   6153274,329 ± 173342,126  ops/s
ListFilteringJMH.MixedWorkload.filterLoop             thrpt   30   5151096,522 ±  58566,226  ops/s
ListFilteringJMH.MixedWorkload.filterStream           thrpt   30   4289866,516 ±  34129,039  ops/s
ListFilteringJMH.MixedWorkload.filterStreamGeneric    thrpt   30   4232627,574 ±  87847,311  ops/s


             filterGeneric    filterLoop filterLoopLazy  filterStream filterStream   removeByIter      removeIf      removeIf
numNulls: 0                                                                Generic      ArrayList     ArrayList    LinkedList   WINNERS
 listSize
    1         (123.343.014)   54.976.781   (126.419.575)   22.421.074    22.143.186    88.485.593    88.330.607    67.086.182   1.filterLoopLazy        2.filterGeneric
    2         (109.386.578    39.297.103   (111.267.614)   19.254.821    19.512.210    79.408.442    80.990.485    57.376.596   1.filterLoopLazy        2.filterGeneric
    5          (80.237.927    22.508.517    (87.375.847)   13.989.751    13.772.237    64.090.167    63.739.841    36.373.785   1.filterLoopLazy        2.filterGeneric
    10         (63.829.938    13.825.645    (65.289.471)    9.680.141     9.698.369    52.758.096    52.475.515    22.065.070   1.filterLoopLazy        2.filterGeneric
    100         11.398.053     1.463.538     11.749.767     1.370.274     1.368.561   (13.131.446)  (13.512.725)    2.304.099   1.removeByIterArrayList 2.removeIfArrayList
numNulls: 1
    1           53.515.991  (110.910.376)    52.797.657    29.353.725    30.154.890   (81.855.627)   69.439.425    57.414.456   1.filterLoop            2.removeByIterArrayList
    2           39.853.378    46.239.701     40.157.211    21.868.056    21.932.644   (56.311.155)  (52.266.527)   43.326.068   1.removeByIterArrayList 2.removeIfArrayList
    5           15.877.149    23.505.541     27.529.150    14.884.244    14.882.027   (25.755.882)  (35.196.643)   28.993.810   1.removeByIterArrayList 2.removeIfArrayList
    10          13.750.640    14.305.541     16.980.799     9.944.951     9.975.849   (20.018.075)  (29.321.648)   19.433.617   1.removeIfArrayList     2.removeByIterArrayList
    100          1.844.838     1.532.534      2.163.198     1.252.671     1.349.534   ( 4.478.253)  ( 5.073.527)    2.537.378   1.removeIfArrayList     2.removeByIterArrayList
numNulls: 5
    1           50.472.609  (110.627.334)    54.068.412    30.093.318    29.917.096   (88.441.343)  (69.679.036)   63.250.020   1.filterLoop            2.removeByIterArrayList
    2           46.674.173  ( 97.787.601)    45.193.980    28.424.279    29.241.035   (32.791.381)  (55.159.221)   47.351.453   1.filterLoop            2.removeIfArrayList
    5           32.730.414  ( 81.598.490)    32.358.935    28.089.096    28.229.792   (11.672.600)  (31.875.412)   23.782.321   1.filterLoop            2.other filter* and removeIfArrayList
    10          12.470.438  ( 17.748.809)    12.162.672    13.278.261    12.219.762   ( 8.545.862)  (22.933.482)   14.955.217   1.removeIfArrayList     2.filterLoop
    100          1.712.707     1.509.697      1.553.243     1.290.276     1.287.498   ( 2.646.446)  ( 3.059.207)    2.236.076   1.removeIfArrayList     2.removeByIterArrayList

Gesamtergebnis: 43.806.523    42.522.481     45.804.502    16.346.329    16.378.979    42.026.025    44.870.220    32.565.743


Benchmark  (numNullValues)  (size) Mode  Cnt       Score        Error  Units
filterGeneric          0       1  thrpt   30  123.343.014 ±  1464.466  ops/s
filterGeneric          0       2  thrpt   30  109.386.578 ±  1066.290  ops/s
filterGeneric          0       5  thrpt   30   80.237.927 ±   788.621  ops/s
filterGeneric          0      10  thrpt   30   63.829.938 ±   492.637  ops/s
filterGeneric          0     100  thrpt   30   11.398.053 ±   188.714  ops/s
filterGeneric          1       1  thrpt   30   53.515.991 ±  1455.258  ops/s
filterGeneric          1       2  thrpt   30   39.853.378 ±  2755.486  ops/s
filterGeneric          1       5  thrpt   30   15.877.149 ±  1277.475  ops/s
filterGeneric          1      10  thrpt   30   13.750.640 ±  1247.961  ops/s
filterGeneric          1     100  thrpt   30    1.844.838 ±   298.451  ops/s
filterGeneric          5       1  thrpt   30   50.472.609 ±   542.149  ops/s
filterGeneric          5       2  thrpt   30   46.674.173 ±  1644.575  ops/s
filterGeneric          5       5  thrpt   30   32.730.414 ±   323.436  ops/s
filterGeneric          5      10  thrpt   30   12.470.438 ±  1756.047  ops/s
filterGeneric          5     100  thrpt   30    1.712.707 ±   137.423  ops/s
filterLoop             0       1  thrpt   30   54.976.781 ±   354.458  ops/s
filterLoop             0       2  thrpt   30   39.297.103 ±   202.287  ops/s
filterLoop             0       5  thrpt   30   22.508.517 ±   198.557  ops/s
filterLoop             0      10  thrpt   30   13.825.645 ±    89.605  ops/s
filterLoop             0     100  thrpt   30    1.463.538 ±    33.139  ops/s
filterLoop             1       1  thrpt   30  110.910.376 ±   781.290  ops/s
filterLoop             1       2  thrpt   30   46.239.701 ±   368.502  ops/s
filterLoop             1       5  thrpt   30   23.505.541 ±   733.851  ops/s
filterLoop             1      10  thrpt   30   14.305.541 ±   302.717  ops/s
filterLoop             1     100  thrpt   30    1.532.534 ±    15.084  ops/s
filterLoop             5       1  thrpt   30  110.627.334 ±  2186.774  ops/s
filterLoop             5       2  thrpt   30   97.787.601 ±   776.641  ops/s
filterLoop             5       5  thrpt   30   81.598.490 ±  1589.057  ops/s
filterLoop             5      10  thrpt   30   17.748.809 ±   957.113  ops/s
filterLoop             5     100  thrpt   30    1.509.697 ±     7.835  ops/s
filterLoopLazy         0       1  thrpt   30  126.419.575 ±   867.678  ops/s
filterLoopLazy         0       2  thrpt   30  111.267.614 ±   983.592  ops/s
filterLoopLazy         0       5  thrpt   30   87.375.847 ±   690.335  ops/s
filterLoopLazy         0      10  thrpt   30   65.289.471 ±   624.479  ops/s
filterLoopLazy         0     100  thrpt   30   11.749.767 ±    84.739  ops/s
filterLoopLazy         1       1  thrpt   30   52.797.657 ±  1646.371  ops/s
filterLoopLazy         1       2  thrpt   30   40.157.211 ±  2290.610  ops/s
filterLoopLazy         1       5  thrpt   30   27.529.150 ±  4395.423  ops/s
filterLoopLazy         1      10  thrpt   30   16.980.799 ±   553.261  ops/s
filterLoopLazy         1     100  thrpt   30    2.163.198 ±   260.727  ops/s
filterLoopLazy         5       1  thrpt   30   54.068.412 ±   992.366  ops/s
filterLoopLazy         5       2  thrpt   30   45.193.980 ±   911.497  ops/s
filterLoopLazy         5       5  thrpt   30   32.358.935 ±   311.500  ops/s
filterLoopLazy         5      10  thrpt   30   12.162.672 ±   665.194  ops/s
filterLoopLazy         5     100  thrpt   30    1.553.243 ±   113.395  ops/s
filterStream           0       1  thrpt   30   22.421.074 ±   310.945  ops/s
filterStream           0       2  thrpt   30   19.254.821 ±   140.084  ops/s
filterStream           0       5  thrpt   30   13.989.751 ±   106.591  ops/s
filterStream           0      10  thrpt   30    9.680.141 ±    57.215  ops/s
filterStream           0     100  thrpt   30    1.370.274 ±    12.268  ops/s
filterStream           1       1  thrpt   30   29.353.725 ±   654.962  ops/s
filterStream           1       2  thrpt   30   21.868.056 ±   260.203  ops/s
filterStream           1       5  thrpt   30   14.884.244 ±   122.560  ops/s
filterStream           1      10  thrpt   30    9.944.951 ±   115.530  ops/s
filterStream           1     100  thrpt   30    1.252.671 ±     8.675  ops/s
filterStream           5       1  thrpt   30   30.093.318 ±   295.390  ops/s
filterStream           5       2  thrpt   30   28.424.279 ±   474.535  ops/s
filterStream           5       5  thrpt   30   28.089.096 ±   842.750  ops/s
filterStream           5      10  thrpt   30   13.278.261 ±   126.550  ops/s
filterStream           5     100  thrpt   30    1.290.276 ±     8.459  ops/s
filterStreamGeneric    0       1  thrpt   30   22.143.186 ±   316.290  ops/s
filterStreamGeneric    0       2  thrpt   30   19.512.210 ±   155.963  ops/s
filterStreamGeneric    0       5  thrpt   30   13.772.237 ±   164.431  ops/s
filterStreamGeneric    0      10  thrpt   30    9.698.369 ±   105.148  ops/s
filterStreamGeneric    0     100  thrpt   30    1.368.561 ±     9.893  ops/s
filterStreamGeneric    1       1  thrpt   30   30.154.890 ±   276.670  ops/s
filterStreamGeneric    1       2  thrpt   30   21.932.644 ±   203.286  ops/s
filterStreamGeneric    1       5  thrpt   30   14.882.027 ±   152.118  ops/s
filterStreamGeneric    1      10  thrpt   30    9.975.849 ±    75.812  ops/s
filterStreamGeneric    1     100  thrpt   30    1.349.534 ±    46.314  ops/s
filterStreamGeneric    5       1  thrpt   30   29.917.096 ±   700.597  ops/s
filterStreamGeneric    5       2  thrpt   30   29.241.035 ±   326.105  ops/s
filterStreamGeneric    5       5  thrpt   30   28.229.792 ±   297.339  ops/s
filterStreamGeneric    5      10  thrpt   30   12.219.762 ±   509.241  ops/s
filterStreamGeneric    5     100  thrpt   30    1.287.498 ±    11.408  ops/s
removeByIterArrayList  0       1  thrpt   30   88.485.593 ±   613.609  ops/s
removeByIterArrayList  0       2  thrpt   30   79.408.442 ±  1401.144  ops/s
removeByIterArrayList  0       5  thrpt   30   64.090.167 ±   542.611  ops/s
removeByIterArrayList  0      10  thrpt   30   52.758.096 ±  1537.740  ops/s
removeByIterArrayList  0     100  thrpt   30   13.131.446 ±   193.638  ops/s
removeByIterArrayList  1       1  thrpt   30   81.855.627 ±  3289.064  ops/s
removeByIterArrayList  1       2  thrpt   30   56.311.155 ± 12552.315  ops/s
removeByIterArrayList  1       5  thrpt   30   25.755.882 ±   343.797  ops/s
removeByIterArrayList  1      10  thrpt   30   20.018.075 ±   300.343  ops/s
removeByIterArrayList  1     100  thrpt   30    4.478.253 ±   660.027  ops/s
removeByIterArrayList  5       1  thrpt   30   88.441.343 ±   761.723  ops/s
removeByIterArrayList  5       2  thrpt   30   32.791.381 ±   733.355  ops/s
removeByIterArrayList  5       5  thrpt   30   11.672.600 ±    84.032  ops/s
removeByIterArrayList  5      10  thrpt   30    8.545.862 ±   549.014  ops/s
removeByIterArrayList  5     100  thrpt   30    2.646.446 ±    35.670  ops/s
removeIfArrayList      0       1  thrpt   30   88.330.607 ±   835.110  ops/s
removeIfArrayList      0       2  thrpt   30   80.990.485 ±   516.788  ops/s
removeIfArrayList      0       5  thrpt   30   63.739.841 ±   525.635  ops/s
removeIfArrayList      0      10  thrpt   30   52.475.515 ±  1384.263  ops/s
removeIfArrayList      0     100  thrpt   30   13.512.725 ±   279.749  ops/s
removeIfArrayList      1       1  thrpt   30   69.439.425 ±   766.693  ops/s
removeIfArrayList      1       2  thrpt   30   52.266.527 ±  3477.160  ops/s
removeIfArrayList      1       5  thrpt   30   35.196.643 ±  2746.287  ops/s
removeIfArrayList      1      10  thrpt   30   29.321.648 ±  2218.475  ops/s
removeIfArrayList      1     100  thrpt   30    5.073.527 ±   915.562  ops/s
removeIfArrayList      5       1  thrpt   30   69.679.036 ±   635.452  ops/s
removeIfArrayList      5       2  thrpt   30   55.159.221 ±   723.086  ops/s
removeIfArrayList      5       5  thrpt   30   31.875.412 ±   504.180  ops/s
removeIfArrayList      5      10  thrpt   30   22.933.482 ±   257.315  ops/s
removeIfArrayList      5     100  thrpt   30    3.059.207 ±   113.410  ops/s
removeIfLinkedList     0       1  thrpt   30   67.086.182 ±   499.010  ops/s
removeIfLinkedList     0       2  thrpt   30   57.376.596 ±   558.958  ops/s
removeIfLinkedList     0       5  thrpt   30   36.373.785 ±   331.332  ops/s
removeIfLinkedList     0      10  thrpt   30   22.065.070 ±   208.834  ops/s
removeIfLinkedList     0     100  thrpt   30    2.304.099 ±    16.273  ops/s
removeIfLinkedList     1       1  thrpt   30   57.414.456 ±  1248.184  ops/s
removeIfLinkedList     1       2  thrpt   30   43.326.068 ±   332.509  ops/s
removeIfLinkedList     1       5  thrpt   30   28.993.810 ±   777.350  ops/s
removeIfLinkedList     1      10  thrpt   30   19.433.617 ±   328.237  ops/s
removeIfLinkedList     1     100  thrpt   30    2.537.378 ±    22.587  ops/s
removeIfLinkedList     5       1  thrpt   30   63.250.020 ±   272.572  ops/s
removeIfLinkedList     5       2  thrpt   30   47.351.453 ±   341.986  ops/s
removeIfLinkedList     5       5  thrpt   30   23.782.321 ±   236.774  ops/s
removeIfLinkedList     5      10  thrpt   30   14.955.217 ±   188.485  ops/s
removeIfLinkedList     5     100  thrpt   30    2.236.076 ±   106.983  ops/s


 */
@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 4, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(3)
@State(Scope.Benchmark)
public class ListFilteringJMH {

    public static void main(String[] args) throws RunnerException {
        List<String> in = Arrays.asList("foo", "bar", "", "a", null, "b", "c", "", "", "d", null, null);

        //remove by actually removing
        System.out.println(removeIfArrayList(in));
        System.out.println(removeIfLinkedList(in));
        System.out.println(removeByIterArrayList(in));
        //"remove" by filter&collect
        System.out.println(filterLoop(in));
        System.out.println(filterStream(in));
        System.out.println(filterStreamGeneric(in, e -> e != null && !e.isEmpty()));
        System.out.println(filterLoopLazy(in));
        System.out.println(filterGeneric(in, e -> e != null && !e.isEmpty()));


        Options opt = new OptionsBuilder()
                .include(ListFilteringJMH.class.getName())
                .result(String.format("%s_%s.json",
                        DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                        ListFilteringJMH.class.getSimpleName()))
                .build();
        new Runner(opt).run();
    }


    @Param({"1", "2", "5", "10", "100"})
    int size;

    @Param({"0", "1", "5"})
    int numNullValues;

    private List<String> in;


    @Setup(Level.Trial)
    public void setup() {
        this.in = generateList(size, numNullValues);
    }

    @NotNull
    public static List<String> generateList(int size, int numNullValues) {
        int nullsToInsert = Math.min(size, numNullValues);
        List<String> in;
        if (nullsToInsert == size) {//all values are null/empty
            in = IntStream.rangeClosed(1, size).mapToObj(i -> "").collect(Collectors.toList());
        } else {
            in = IntStream.rangeClosed(1, size).mapToObj(Integer::toString).collect(Collectors.toList());
        }

        ThreadLocalRandom r = ThreadLocalRandom.current();
        while (nullsToInsert > 0) {
            int idx = r.nextInt(size);
            String v = in.get(idx);
            if (v != null) {
                in.set(idx, r.nextBoolean() ? "" : null);
                --nullsToInsert;
            }
        }
        return Collections.unmodifiableList(in);
    }


    @Benchmark
    public List<String> removeIfArrayList() {
        return removeIfArrayList(in);
    }

    @Benchmark
    public List<String> removeIfLinkedList() {
        return removeIfLinkedList(in);
    }

    @Benchmark
    public List<String> removeByIterArrayList() {
        return removeByIterArrayList(in);
    }

    @Benchmark
    public List<String> filterLoop() {
        return filterLoop(in);
    }

    @Benchmark
    public List<String> filterStream() {
        return filterStream(in);
    }

    @Benchmark
    public List<String> filterStreamGeneric() {
        return filterStreamGeneric(in, e -> e != null && !e.isEmpty());
    }

    @Benchmark
    public List<String> filterLoopLazy() {
        return filterLoopLazy(in);
    }

    @Benchmark
    public List<String> filterGeneric() {
        return filterGeneric(in, e -> e != null && !e.isEmpty());
    }


    public static List<String> removeIfArrayList(List<String> in) {
        ArrayList<String> result = new ArrayList<>(in);
        result.removeIf(x -> x == null || x.isEmpty());
        return result;
    }

    public static List<String> removeIfLinkedList(List<String> in) {
        LinkedList<String> result = new LinkedList<>(in);
        result.removeIf(x -> x == null || x.isEmpty());
        return result;
    }

    //should be equal to removeIfArrayList
    public static List<String> removeByIterArrayList(List<String> searchTerms) {
        List<String> result = new ArrayList<>(searchTerms);
        Iterator<String> iter = result.iterator();
        while (iter.hasNext()) {
            String e = iter.next();
            if (e == null || e.isEmpty()) {
                iter.remove();
            }
        }
        return result;
    }

    //removeByCollectLoop
    public static List<String> filterLoop(List<String> searchTerms) {
        List<String> result = new ArrayList<>();
        for (String e : searchTerms) {
            if (!(e == null || e.isEmpty())) {
                result.add(e);
            }
        }
        return result;
    }

    public static List<String> filterStream(List<String> searchTerms) {
        return searchTerms.stream()
                .filter(x -> x != null && !x.isEmpty())
                .collect(Collectors.toList());
    }

    public static <T> List<T> filterStreamGeneric(Collection<T> searchTerms, Predicate<T> filter) {
        return searchTerms.stream()
                .filter(filter)
                .collect(Collectors.toList());
    }

    public static List<String> filterLoopLazy(List<String> input) {
        List<String> result = null;
        int numScanned = 0;
        for (String e : input) {

            if (e == null || e.isEmpty()) {
                if (result == null) {
                    //first time we discovered anything that has to be removed
                    result = new ArrayList<>(input.subList(0, numScanned)); //catch up
                }
            } else {
                if (result != null) {
                    result.add(e);
                }
            }
            ++numScanned;
        }
        return result == null
                ? input//nothing needed to be replaced
                : result;
    }

    //filter
    public static <T> List<T> filterGeneric(List<T> input, Predicate<T> trueToKeep) {
        List<T> result = null;
        int numScanned = 0;
        for (T e : input) {
            if (trueToKeep.test(e)) {
                //defer adding until we found something we want to remove for the first time
                if (result != null) {
                    result.add(e);
                }
            } else if (result == null) {
                //first time we discovered anything that has to be removed
                result = new ArrayList<>(input.subList(0, numScanned)); //catch up

            }//else: skip
            ++numScanned;
        }
        return result == null
                ? input//nothing needed to be removed, return original list
                : result;
    }

    @BenchmarkMode({Mode.Throughput})
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Warmup(iterations = 4, time = 1, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
    @Fork(3)
    @State(Scope.Benchmark)
    public static class MixedWorkload {


        public static void main(String[] args) throws RunnerException {
            String includ = JWildcard.wildcardToRegex("*" + MixedWorkload.class.getSimpleName() + "*");
            System.out.println(includ);
            Options opt = new OptionsBuilder()
                    .include(includ)
                    .result(String.format("%s_%s.json",
                            DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                            ListFilteringJMH.class.getSimpleName()))
                    .build();
            new Runner(opt).run();
        }

        List<List<String>> inputs;

        @Setup
        public void setup() {
            inputs = new ArrayList<>();
            List<Integer> sizes = Arrays.asList(1, 2, 5, 10, 100);
            List<Integer> numNulls = Arrays.asList(0, 1, 5);

            for (int numNull : numNulls) {
                for (int size : sizes) {
                    inputs.add(ListFilteringJMH.generateList(size, numNull));
                }
            }
        }

        public List<String> in() {
            return inputs.get(ThreadLocalRandom.current().nextInt(inputs.size()));
        }

        @Benchmark
        public List<String> removeIfArrayList() {
            return ListFilteringJMH.removeIfArrayList(in());
        }

        @Benchmark
        public List<String> removeIfLinkedList() {
            return ListFilteringJMH.removeIfLinkedList(in());
        }

        @Benchmark
        public List<String> removeByIterArrayList() {
            return ListFilteringJMH.removeByIterArrayList(in());
        }

        @Benchmark
        public List<String> filterLoop() {
            return ListFilteringJMH.filterLoop(in());
        }

        @Benchmark
        public List<String> filterStream() {
            return ListFilteringJMH.filterStream(in());
        }

        @Benchmark
        public List<String> filterStreamGeneric() {
            return ListFilteringJMH.filterStreamGeneric(in(), e -> e != null && !e.isEmpty());
        }

        @Benchmark
        public List<String> filterLoopLazy() {
            return ListFilteringJMH.filterLoopLazy(in());
        }

        @Benchmark
        public List<String> filterGeneric() {
            return ListFilteringJMH.filterGeneric(in(), e -> e != null && !e.isEmpty());
        }
    }

}
