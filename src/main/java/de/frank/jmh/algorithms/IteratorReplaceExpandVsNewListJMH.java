package de.frank.jmh.algorithms;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/*--
Function: "normalize" a list by replacing each occurrence of an empty string or null by adding two marker tokens
This benchmark started out comparing the efficiency of using iterator.remove followed by two iterator.add's on an ArrayList


       genericReplace   genericRepl2 genericReplStream   iterator    lazyNewList       newList
0 element to replace
    1     237.161.800    245.751.279     22.741.287    91.629.471    243.977.570    68.911.389
    2     211.801.997    218.294.824     13.855.984    83.458.452    221.240.756    56.080.006
    5     123.961.345    125.323.864     10.526.907    65.266.279    127.579.389    37.201.943
    10     89.310.054     89.006.233      8.425.757    54.162.297     90.783.623    24.094.835
    100    16.357.651     16.416.794      1.404.231    13.191.760     16.768.300     2.246.739
1 element to replace
    1      25.939.691     28.966.471     11.108.582    37.619.000     72.954.704    65.255.569
    2      17.444.098     23.192.638     10.301.058    12.489.889     37.131.533    50.784.165
    5      14.266.265     15.029.137      8.709.508    17.772.550     24.025.230    28.123.608
    10     11.555.856      9.911.106      6.713.600    10.232.036     16.249.082    17.859.809
    100     2.549.991      1.542.945      1.339.331     3.128.609      2.838.479     2.229.730
5 elements to replace
    1      25.278.715     29.750.388     11.135.076    38.484.317     72.818.010    65.744.091
    2      12.264.099     14.741.202      8.467.454    10.276.870     37.270.993    53.152.766
    5       5.719.368      6.596.237      5.199.266     3.712.124     20.987.386    33.070.574
    10      5.606.644      5.676.090      4.572.758     4.025.489     14.371.538    17.329.080
    100     1.563.279      1.526.189      1.376.326     1.763.063      2.443.524     2.189.252

Average:   53.385.390     55.448.360      8.391.808    29.814.147     66.762.674    34.951.570

Benchmark             (numNullValues)  (size)   Mode  Cnt       Score       Error  Units
genericReplace                      0       1  thrpt   15  237.161.800 ±  3726307  ops/s
genericReplace                      0       2  thrpt   15  211.801.997 ±  2564162  ops/s
genericReplace                      0       5  thrpt   15  123.961.345 ±  1435258  ops/s
genericReplace                      0      10  thrpt   15   89.310.054 ±   881202  ops/s
genericReplace                      0     100  thrpt   15   16.357.651 ±   187411  ops/s
genericReplace                      1       1  thrpt   15   25.939.691 ±   364249  ops/s
genericReplace                      1       2  thrpt   15   17.444.098 ±  3262281  ops/s
genericReplace                      1       5  thrpt   15   14.266.265 ±  1388889  ops/s
genericReplace                      1      10  thrpt   15   11.555.856 ±   187813  ops/s
genericReplace                      1     100  thrpt   15    2.549.991 ±   266180  ops/s
genericReplace                      5       1  thrpt   15   25.278.715 ±  1160270  ops/s
genericReplace                      5       2  thrpt   15   12.264.099 ±   118245  ops/s
genericReplace                      5       5  thrpt   15    5.719.368 ±    39860  ops/s
genericReplace                      5      10  thrpt   15    5.606.644 ±   438926  ops/s
genericReplace                      5     100  thrpt   15    1.563.279 ±   108979  ops/s
genericReplace2                     0       1  thrpt   15  245.751.279 ±  7666147  ops/s
genericReplace2                     0       2  thrpt   15  218.294.824 ±  1190874  ops/s
genericReplace2                     0       5  thrpt   15  125.323.864 ±  1731415  ops/s
genericReplace2                     0      10  thrpt   15   89.006.233 ±   937782  ops/s
genericReplace2                     0     100  thrpt   15   16.416.794 ±   222957  ops/s
genericReplace2                     1       1  thrpt   15   28.966.471 ±   756707  ops/s
genericReplace2                     1       2  thrpt   15   23.192.638 ±  1339773  ops/s
genericReplace2                     1       5  thrpt   15   15.029.137 ±   405216  ops/s
genericReplace2                     1      10  thrpt   15    9.911.106 ±   295614  ops/s
genericReplace2                     1     100  thrpt   15    1.542.945 ±   221727  ops/s
genericReplace2                     5       1  thrpt   15   29.750.388 ±   273103  ops/s
genericReplace2                     5       2  thrpt   15   14.741.202 ±   170871  ops/s
genericReplace2                     5       5  thrpt   15    6.596.237 ±    87445  ops/s
genericReplace2                     5      10  thrpt   15    5.676.090 ±    71656  ops/s
genericReplace2                     5     100  thrpt   15    1.526.189 ±    54468  ops/s
genericReplaceStream                0       1  thrpt   15   22.741.287 ± 11903961  ops/s
genericReplaceStream                0       2  thrpt   15   13.855.984 ±   185669  ops/s
genericReplaceStream                0       5  thrpt   15   10.526.907 ±   149771  ops/s
genericReplaceStream                0      10  thrpt   15    8.425.757 ±   139172  ops/s
genericReplaceStream                0     100  thrpt   15    1.404.231 ±    22934  ops/s
genericReplaceStream                1       1  thrpt   15   11.108.582 ±   192618  ops/s
genericReplaceStream                1       2  thrpt   15   10.301.058 ±   212807  ops/s
genericReplaceStream                1       5  thrpt   15    8.709.508 ±    80376  ops/s
genericReplaceStream                1      10  thrpt   15    6.713.600 ±    97093  ops/s
genericReplaceStream                1     100  thrpt   15    1.339.331 ±   140551  ops/s
genericReplaceStream                5       1  thrpt   15   11.135.076 ±   217055  ops/s
genericReplaceStream                5       2  thrpt   15    8.467.454 ±   209291  ops/s
genericReplaceStream                5       5  thrpt   15    5.199.266 ±   176931  ops/s
genericReplaceStream                5      10  thrpt   15    4.572.758 ±   189290  ops/s
genericReplaceStream                5     100  thrpt   15    1.376.326 ±    27452  ops/s
iterator                            0       1  thrpt   15   91.629.471 ±   885535  ops/s
iterator                            0       2  thrpt   15   83.458.452 ±   934283  ops/s
iterator                            0       5  thrpt   15   65.266.279 ±  1176662  ops/s
iterator                            0      10  thrpt   15   54.162.297 ±  1891374  ops/s
iterator                            0     100  thrpt   15   13.191.760 ±   265976  ops/s
iterator                            1       1  thrpt   15   37.619.000 ±   900211  ops/s
iterator                            1       2  thrpt   15   12.489.889 ±   164100  ops/s
iterator                            1       5  thrpt   15   17.772.550 ±  9094546  ops/s
iterator                            1      10  thrpt   15   10.232.036 ±   218287  ops/s
iterator                            1     100  thrpt   15    3.128.609 ±   343238  ops/s
iterator                            5       1  thrpt   15   38.484.317 ±   797952  ops/s
iterator                            5       2  thrpt   15   10.276.870 ±   556828  ops/s
iterator                            5       5  thrpt   15    3.712.124 ±    46239  ops/s
iterator                            5      10  thrpt   15    4.025.489 ±   515237  ops/s
iterator                            5     100  thrpt   15    1.763.063 ±    35314  ops/s
lazyNewList                         0       1  thrpt   15  243.977.570 ±  3332510  ops/s
lazyNewList                         0       2  thrpt   15  221.240.756 ±  2646434  ops/s
lazyNewList                         0       5  thrpt   15  127.579.389 ±  1639168  ops/s
lazyNewList                         0      10  thrpt   15   90.783.623 ±   654788  ops/s
lazyNewList                         0     100  thrpt   15   16.768.300 ±   221874  ops/s
lazyNewList                         1       1  thrpt   15   72.954.704 ±  1308590  ops/s
lazyNewList                         1       2  thrpt   15   37.131.533 ± 14922330  ops/s
lazyNewList                         1       5  thrpt   15   24.025.230 ±   239409  ops/s
lazyNewList                         1      10  thrpt   15   16.249.082 ±  1102815  ops/s
lazyNewList                         1     100  thrpt   15    2.838.479 ±   316885  ops/s
lazyNewList                         5       1  thrpt   15   72.818.010 ±  1004144  ops/s
lazyNewList                         5       2  thrpt   15   37.270.993 ±   674163  ops/s
lazyNewList                         5       5  thrpt   15   20.987.386 ±   274332  ops/s
lazyNewList                         5      10  thrpt   15   14.371.538 ±  2346718  ops/s
lazyNewList                         5     100  thrpt   15    2.443.524 ±    66537  ops/s
newList                             0       1  thrpt   15   68.911.389 ±   897357  ops/s
newList                             0       2  thrpt   15   56.080.006 ±   794508  ops/s
newList                             0       5  thrpt   15   37.201.943 ±   665492  ops/s
newList                             0      10  thrpt   15   24.094.835 ±   338634  ops/s
newList                             0     100  thrpt   15    2.246.739 ±    23698  ops/s
newList                             1       1  thrpt   15   65.255.569 ±  1029956  ops/s
newList                             1       2  thrpt   15   50.784.165 ±  2171120  ops/s
newList                             1       5  thrpt   15   28.123.608 ±   755535  ops/s
newList                             1      10  thrpt   15   17.859.809 ±   203881  ops/s
newList                             1     100  thrpt   15    2.229.730 ±    23111  ops/s
newList                             5       1  thrpt   15   65.744.091 ±   637210  ops/s
newList                             5       2  thrpt   15   53.152.766 ±   661240  ops/s
newList                             5       5  thrpt   15   33.070.574 ±   550394  ops/s
newList                             5      10  thrpt   15   17.329.080 ±   287474  ops/s
newList                             5     100  thrpt   15    2.189.252 ±    27416  ops/s

Benchmark                                  (size)   Mode  Cnt         Score          Error  Units
IteratorRemoveVsNewListJMH.iterator             1  thrpt   15  55.210.337 ± 27.408.858 ops/s
IteratorRemoveVsNewListJMH.iterator             2  thrpt   15  34.380.066 ± 33.683.991 ops/s
IteratorRemoveVsNewListJMH.iterator             5  thrpt   15   7.231.161 ±  3.138.801 ops/s
IteratorRemoveVsNewListJMH.iterator            10  thrpt   15   3.180.528 ±    521.162 ops/s
IteratorRemoveVsNewListJMH.iterator           100  thrpt   15     312.665 ±     22.357 ops/s

IteratorRemoveVsNewListJMH.newList              1  thrpt   15  76.547.357 ±  2.389.726 ops/s
IteratorRemoveVsNewListJMH.newList              2  thrpt   15  59.964.646 ±  4.647.361 ops/s
IteratorRemoveVsNewListJMH.newList              5  thrpt   15  29.345.098 ±    654.206 ops/s
IteratorRemoveVsNewListJMH.newList             10  thrpt   15  11.926.369 ±  1.168.577 ops/s
IteratorRemoveVsNewListJMH.newList            100  thrpt   15   1.371.805 ±     75.341 ops/s

IteratorRemoveVsNewListJMH.lazyNewList          1  thrpt   15  69.963.248 ±    731.003 ops/s
IteratorRemoveVsNewListJMH.lazyNewList          2  thrpt   15  41.876.033 ± 10.722.200 ops/s
IteratorRemoveVsNewListJMH.lazyNewList          5  thrpt   15  25.808.699 ±    698.680 ops/s
IteratorRemoveVsNewListJMH.lazyNewList         10  thrpt   15  14.992.308 ±  1.402.390 ops/s
IteratorRemoveVsNewListJMH.lazyNewList        100  thrpt   15   1.813.444 ±     47.123 ops/s

IteratorRemoveVsNewListJMH.genericReplace       1  thrpt   15  30.121.285 ±  1.231.187 ops/s
IteratorRemoveVsNewListJMH.genericReplace       2  thrpt   15  17.438.221 ±  5.583.484 ops/s
IteratorRemoveVsNewListJMH.genericReplace       5  thrpt   15   8.939.138 ±  2.214.927 ops/s
IteratorRemoveVsNewListJMH.genericReplace      10  thrpt   15   5.623.359 ±    932.872 ops/s
IteratorRemoveVsNewListJMH.genericReplace     100  thrpt   15     668.867 ±     38.883 ops/s
 */
@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 4, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(3)
@State(Scope.Thread)
public class IteratorReplaceExpandVsNewListJMH {

    public static void main(String[] args) throws RunnerException {
        List<String> in = Arrays.asList("foo", "bar", "", "a", null, "b", "c", "", "", "d", null, null);
        System.out.println(normalizeEmptySearchTerms(in));
        System.out.println(normalizeEmptySearchTerms2(in));
        System.out.println(lazyNormalizeEmptySearchTerms(in));
        System.out.println(genericReplace(in,
                //if..
                e -> e == null || e.isEmpty(),
                //replace with
                e -> REPLACEMENT));


        Options opt = new OptionsBuilder()
                .include(IteratorReplaceExpandVsNewListJMH.class.getName())
                .result(String.format("%s_%s.json",
                        DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                        IteratorReplaceExpandVsNewListJMH.class.getSimpleName()))
                .build();
        new Runner(opt).run();
    }

    private static final List<String> REPLACEMENT = Arrays.asList("", null);

    @Param({"1", "2", "5", "10", "100"})
    int size;

    @Param({"0", "1", "5"})
    int numNullValues;

    List<String> in;

    @Setup
    public void setup() {
        int nullsToInsert = Math.min(size, numNullValues);

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
    }


    @Benchmark
    public List<String> iterator() {
        return normalizeEmptySearchTerms(in);
    }

    @Benchmark
    public List<String> newList() {
        return normalizeEmptySearchTerms2(in);
    }

    @Benchmark
    public List<String> lazyNewList() {
        return lazyNormalizeEmptySearchTerms(in);
    }


    @Benchmark
    public List<String> genericReplace() {
        return genericReplace(in,
                //if..
                e -> e == null || e.isEmpty(),
                //replace with
                e -> REPLACEMENT);

    }

    @Benchmark
    public List<String> genericReplace2() {
        return genericReplace2(in,
                //if..
                e -> e == null || e.isEmpty(),
                //replace with
                e -> REPLACEMENT);

    }

    @Benchmark
    public List<String> genericReplace3() {
        return genericReplace3(in,
                //if..
                e -> e == null || e.isEmpty(),
                //replace with
                e -> REPLACEMENT);

    }

    @Benchmark
    public List<String> genericReplaceStream() {
        return genericReplaceStream(in,
                //if..
                e -> e == null || e.isEmpty(),
                //replace with
                e -> REPLACEMENT);

    }


    private static List<String> normalizeEmptySearchTerms(List<String> searchTerms) {
        List<String> allSearchTerms = new ArrayList<>(searchTerms);
        for (ListIterator<String> iter = allSearchTerms.listIterator(); iter.hasNext(); ) {
            String curr = iter.next();
            if (curr == null || curr.isEmpty()) {
                iter.remove();  // before the adds!!!
                iter.add("");
                iter.add(null);
            }
        }
        return allSearchTerms;
    }

    private static List<String> normalizeEmptySearchTerms2(List<String> searchTerms) {
        List<String> allSearchTerms = new ArrayList<>();
        for (String searchTerm : searchTerms) {
            if (searchTerm == null || searchTerm.isEmpty()) {
                // normalize 'empty' search terms into a 'empty string' + 'null' search term
                allSearchTerms.add("");
                allSearchTerms.add(null);
            } else {
                allSearchTerms.add(searchTerm);
            }
        }
        return allSearchTerms;
    }

    private static List<String> lazyNormalizeEmptySearchTerms(List<String> searchTerms) {
        //lazy init result list - if there is nothing to normalize, return original list
        List<String> normalizedTerms = null;
        for (int i = 0; i < searchTerms.size(); i++) {
            String searchTerm = searchTerms.get(i);

            if (searchTerm == null || searchTerm.isEmpty()) {
                if (normalizedTerms == null) {//lazy init
                    normalizedTerms = new ArrayList<>(searchTerms.size() + 1);
                    normalizedTerms.addAll(searchTerms.subList(0, i));//catch up
                }
                // normalize 'empty' search terms into a 'empty string' + 'null' search term
                normalizedTerms.add("");
                normalizedTerms.add(null);
            } else if (normalizedTerms != null) {
                normalizedTerms.add(searchTerm);
            }
        }
        return normalizedTerms != null
                ? normalizedTerms
                : searchTerms;
    }

    private static <T> List<T> genericReplace(List<T> searchTerms, Predicate<T> match, Function<T, List<T>> replacements) {
        //lazy init result list - if there is nothing to normalize, return original list
        List<T> normalizedTerms = null;
        for (int i = 0; i < searchTerms.size(); i++) {
            T searchTerm = searchTerms.get(i);

            if (match.test(searchTerm)) {
                if (normalizedTerms == null) {//lazy init
                    normalizedTerms = new ArrayList<>(searchTerms.size() + 1);
                    normalizedTerms.addAll(searchTerms.subList(0, i));//catch up
                }
                // normalize 'empty' search terms into a 'empty string' + 'null' search term
                normalizedTerms.addAll(replacements.apply(searchTerm));
            } else if (normalizedTerms != null) {
                normalizedTerms.add(searchTerm);
            }
        }
        return normalizedTerms != null
                ? normalizedTerms
                : searchTerms;
    }

    private static <T> List<T> genericReplace3(List<T> searchTerms, Predicate<T> match, Function<T, List<T>> replacements) {
        //lazy init result list - if there is nothing to normalize, return original list
        List<T> normalizedTerms = null;
        for (int i = 0; i < searchTerms.size(); i++) {
            T searchTerm = searchTerms.get(i);

            if (match.test(searchTerm)) {
                if (normalizedTerms == null) {//lazy init
                    normalizedTerms = new ArrayList<>(searchTerms.subList(0, i));
                }
                // normalize 'empty' search terms into a 'empty string' + 'null' search term
                normalizedTerms.addAll(replacements.apply(searchTerm));
            } else if (normalizedTerms != null) {
                normalizedTerms.add(searchTerm);
            }
        }
        return normalizedTerms != null
                ? normalizedTerms
                : searchTerms;
    }

    private static <T> List<T> genericReplace2(List<T> searchTerms, Predicate<T> match, Function<T, List<T>> replacements) {
        //lazy init result list - if there is nothing to normalize, return original list
        List<T> normalizedTerms = null;
        for (int i = 0; i < searchTerms.size(); i++) {
            T searchTerm = searchTerms.get(i);

            if (match.test(searchTerm)) {
                if (normalizedTerms == null) {//lazy init
                    normalizedTerms = new ArrayList<>(searchTerms.size() + 1);
                    searchTerms.subList(0, i).forEach(normalizedTerms::add);//catch up
                }
                // normalize 'empty' search terms into a 'empty string' + 'null' search term
                normalizedTerms.addAll(replacements.apply(searchTerm));
            } else if (normalizedTerms != null) {
                normalizedTerms.add(searchTerm);
            }
        }
        return normalizedTerms != null
                ? normalizedTerms
                : searchTerms;
    }

    private static <T> List<T> genericReplaceStream(List<T> searchTerms, Predicate<T> match, Function<T, List<T>> replacements) {
        return searchTerms.stream().collect(
                ArrayList::new,
                (list, val) -> {
                    if (match.test(val)) {
                        list.addAll(replacements.apply(val));
                    } else {
                        list.add(val);
                    }
                },
                ArrayList::addAll
        );
    }

}
