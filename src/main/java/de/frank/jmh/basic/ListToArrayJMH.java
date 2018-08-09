package de.frank.jmh.basic;

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
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static java.lang.String.format;

/*--

 VM version: JDK 1.8.0_161-1-redhat, VM 25.161-b14
 Benchmark
                               1        100       100.000 <-ListSize
 toArrayStringUnsized   11 ns/op   48 ns/op  57.479 ns/op  # ALWAYS use un-sized & always use typed! the others are not worth it
 toArrayStringSized     24 ns/op   50 ns/op  59.266 ns/op  # sized is worse then un-sized. Corner usecase: you need a bigger result array then the source list
 toArray                12 ns/op   48 ns/op  50.376 ns/op  # Object[] - not typed
 toArrayObjectSized     25 ns/op   50 ns/op  59.644 ns/op  # Object[] - not typed
 toArrayObjectUnsized   10 ns/op   44 ns/op  61.779 ns/op  # Object[] - not typed
 toArrayStream          22 ns/op  217 ns/op 186.404 ns/op  # Object[] - not typed - sucks because of stream
 toArrayStreamString    36 ns/op  248 ns/op 346.462 ns/op  # sucks because of stream
 */
@State(Scope.Thread)
@Warmup(iterations = 10, time = 7, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class ListToArrayJMH {

    private static final String[] DATA_SIZE_PARAMS = {"1", "100", "100000"};

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()//
                .include(".*" + ListToArrayJMH.class.getSimpleName() + ".*")//
                //.addProfiler(GCProfiler.class)//
                .param("dataSize" , DATA_SIZE_PARAMS)
                .resultFormat(ResultFormatType.JSON)
                .result(format("%s_%s.json",//
                        new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(System.currentTimeMillis()),//
                        ListToArrayJMH.class.getSimpleName()))
                .build();
        displayAsMatrix("dataSize", new Runner(opt).run());
    }

    private static void displayAsMatrix(String groupByThisParam, Collection<RunResult> results) {
        Optional<RunResult> resultWithMoreThenOneParam = results.stream().filter(x -> x.getParams().getParamsKeys().size() > 1).findFirst();
        if(resultWithMoreThenOneParam.isPresent()){
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


    List<String> data;

    @Param({"1", "100", "100000"})
    private int dataSize;


    @Setup
    public void setup() {
        String[] tmp = new String[dataSize];
        for (int i = 0; i < tmp.length; i++) {
            tmp[i] = "" + i;
        }
        this.data = Arrays.asList(tmp); //toArray implementation in openjdk 1.8 is the same as java.util.ArrayList
    }


    @Benchmark
    public Object[] toArray() {
        //flawed, as Object[] does not require "instanceof String" checks on assignment
        return data.toArray();
    }

    @Benchmark
    public Object[] toArrayObjectUnsized() {
        //flawed, as Object[] does not require "instanceof String" checks on assignment
        return data.toArray(new Object[0]);
    }

    @Benchmark
    public Object[] toArrayObjectSized() {
        //flawed, as Object[] does not require "instanceof String" checks on assignment
        return data.toArray(new Object[data.size()]);
    }

    @Benchmark
    public String[] toArrayStringUnsized() {
        return data.toArray(new String[0]);
    }

    @Benchmark
    public String[] toArrayStringSized() {
        return data.toArray(new String[data.size()]);
    }

    @Benchmark
    public Object[] toArrayStream() {
        return data.stream().toArray();
    }

    @Benchmark
    public String[] toArrayStreamString() {
        return data.stream().toArray(String[]::new);
    }
}
