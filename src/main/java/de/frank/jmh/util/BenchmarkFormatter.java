package de.frank.jmh.util;

import org.openjdk.jmh.results.RunResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class BenchmarkFormatter {

    /**
     * Transforms JMH default output of  a flat list of "Benchmarks x Parameter values" results into a matrix with
     * - benchmark methods as lines and
     * - benchmark results per paramter as columns
     * <p>
     * e.g.
     * Produces this:
     * Units: ns/op           size->      2   10   30   100  1000
     * arrayList_AddExisting              4    8   20    50   631
     * arrayList_AddNonExisting           7   13   30   117  1401
     * hashSet_AddExisting               16   16   17    17    18
     * hashSet_AddNonExisting            13   10   12    12    12
     * linkedHashSet_AddExisting         14   10   15     9    11
     * linkedHashSet_AddNonExisting      10   11   11    12    13
     * <p>
     * For this default format:
     * Benchmark                                            (size)  Mode  Cnt     Score     Error  Units
     * CollectionsAddDistinct.arrayList_AddExisting              2  avgt   10     3,635 ±   0,822  ns/op
     * CollectionsAddDistinct.arrayList_AddExisting             10  avgt   10     8,482 ±   2,447  ns/op
     * CollectionsAddDistinct.arrayList_AddExisting             30  avgt   10    20,125 ±   4,148  ns/op
     * CollectionsAddDistinct.arrayList_AddExisting            100  avgt   10    50,381 ±  10,618  ns/op
     * CollectionsAddDistinct.arrayList_AddExisting           1000  avgt   10   630,911 ± 310,855  ns/op
     * CollectionsAddDistinct.arrayList_AddNonExisting           2  avgt   10     7,092 ±   1,139  ns/op
     * CollectionsAddDistinct.arrayList_AddNonExisting          10  avgt   10    13,084 ±   0,477  ns/op
     * CollectionsAddDistinct.arrayList_AddNonExisting          30  avgt   10    29,620 ±   2,749  ns/op
     * CollectionsAddDistinct.arrayList_AddNonExisting         100  avgt   10   116,978 ±  14,567  ns/op
     * CollectionsAddDistinct.arrayList_AddNonExisting        1000  avgt   10  1400,730 ± 332,343  ns/op
     * CollectionsAddDistinct.hashSet_AddExisting                2  avgt   10    16,135 ±   0,495  ns/op
     * CollectionsAddDistinct.hashSet_AddExisting               10  avgt   10    16,440 ±   0,920  ns/op
     * CollectionsAddDistinct.hashSet_AddExisting               30  avgt   10    16,737 ±   0,714  ns/op
     * CollectionsAddDistinct.hashSet_AddExisting              100  avgt   10    16,821 ±   1,227  ns/op
     * CollectionsAddDistinct.hashSet_AddExisting             1000  avgt   10    17,907 ±   0,579  ns/op
     * CollectionsAddDistinct.hashSet_AddNonExisting             2  avgt   10    13,148 ±   3,878  ns/op
     * CollectionsAddDistinct.hashSet_AddNonExisting            10  avgt   10     9,652 ±   1,048  ns/op
     * CollectionsAddDistinct.hashSet_AddNonExisting            30  avgt   10    11,603 ±   3,280  ns/op
     * CollectionsAddDistinct.hashSet_AddNonExisting           100  avgt   10    12,134 ±   7,965  ns/op
     * CollectionsAddDistinct.hashSet_AddNonExisting          1000  avgt   10    11,615 ±   2,007  ns/op
     * CollectionsAddDistinct.linkedHashSet_AddExisting          2  avgt   10    14,052 ±   0,709  ns/op
     * CollectionsAddDistinct.linkedHashSet_AddExisting         10  avgt   10     9,528 ±   0,711  ns/op
     * CollectionsAddDistinct.linkedHashSet_AddExisting         30  avgt   10    14,590 ±   1,265  ns/op
     * CollectionsAddDistinct.linkedHashSet_AddExisting        100  avgt   10     9,261 ±   0,360  ns/op
     * CollectionsAddDistinct.linkedHashSet_AddExisting       1000  avgt   10    10,766 ±   0,356  ns/op
     * CollectionsAddDistinct.linkedHashSet_AddNonExisting       2  avgt   10    10,110 ±   0,810  ns/op
     * CollectionsAddDistinct.linkedHashSet_AddNonExisting      10  avgt   10    11,086 ±   0,441  ns/op
     * CollectionsAddDistinct.linkedHashSet_AddNonExisting      30  avgt   10    11,240 ±   1,868  ns/op
     * CollectionsAddDistinct.linkedHashSet_AddNonExisting     100  avgt   10    11,726 ±   0,506  ns/op
     * CollectionsAddDistinct.linkedHashSet_AddNonExisting    1000  avgt   10    13,098 ±   0,439  ns/op
     *
     * @param results
     * @param groupByThisParam
     */
    public static void displayAsMatrix(Collection<RunResult> results, String groupByThisParam) {
        Optional<RunResult> resultWithMoreThenOneParam = results.stream().filter(x -> x.getParams().getParamsKeys().size() > 1).findFirst();
        if (resultWithMoreThenOneParam.isPresent()) {
            System.out.println("ERROR! Cannot display as Matrix! Only one param is allowed");
            return;
        }
        //group by benchmark name, so that we can print all the param results as columns
        Map<String, List<RunResult>> grouped = results.stream().collect(Collectors.groupingBy(x -> x.getParams().getBenchmark(), toSortedList(compareByParam(groupByThisParam))));

        //Table boundaries
        int columns = grouped.values().stream().mapToInt(List::size).max().orElse(3);
        int benchmarkNameLen = results.stream().mapToInt(x -> x.getParams().getBenchmark().length()).max().orElse(15);
        int paramNameLen = results.stream().mapToInt(x -> x.getParams().getParam(groupByThisParam).length()).max().orElse(5);
        int resultValueLen = results.stream().mapToInt(x -> Long.toString(Math.round(x.getPrimaryResult().getScore())).length()).max().orElse(5);
        int colLen = Math.max(paramNameLen, resultValueLen);

        //create format strings matching boundaries
        String nameColFormat = "%-" + benchmarkNameLen + "s";
        String valueColFormat = "";
        for (int i = 0; i < columns; i++) {
            valueColFormat += "  %" + colLen + "s";
        }

        Map.Entry<String, List<RunResult>> firstLine = grouped.entrySet().iterator().next();
        String[] colNames = firstLine.getValue().stream().map(x -> x.getParams().getParam(groupByThisParam)).toArray(String[]::new);

        StringBuilder sb = new StringBuilder();
        //write header
        sb.append(format(nameColFormat, "Units: " + firstLine.getValue().get(0).getPrimaryResult().getScoreUnit()));
        sb.append(format(valueColFormat, colNames));
        sb.append('\n');

        //write values
        for (Map.Entry<String, List<RunResult>> line : grouped.entrySet()) {
            String[] resultValues = line.getValue().stream().map(x -> Long.toString(Math.round(x.getPrimaryResult().getScore()))).toArray(String[]::new);

            sb.append(format(nameColFormat, line.getKey()));
            sb.append(format(valueColFormat, resultValues));
            sb.append('\n');
        }
        System.out.println(sb.toString());
    }


    private static Comparator<RunResult> compareByParam(String paramName) {
        return Comparator.comparing(x -> x.getParams().getParam(paramName));
    }

    static <T> Collector<T, ?, List<T>> toSortedList(Comparator<? super T> c) {
        return Collectors.collectingAndThen(
                Collectors.toCollection(ArrayList::new), l -> {
                    l.sort(c);
                    return l;
                });
    }
}
