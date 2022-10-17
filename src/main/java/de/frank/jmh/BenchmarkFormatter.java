package de.frank.jmh;

import org.openjdk.jmh.results.RunResult;

import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class BenchmarkFormatter {
    public static void displayAsMatrix(Collection<RunResult> results, String groupByThisParam) {
        Optional<RunResult> resultWithMoreThenOneParam = results.stream().filter(x -> x.getParams().getParamsKeys().size() > 1).findFirst();
        if (resultWithMoreThenOneParam.isPresent()) {
            System.out.println("ERROR! Cannot display as Matrix!");
            return;
        }
        //group by benchmark name
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
