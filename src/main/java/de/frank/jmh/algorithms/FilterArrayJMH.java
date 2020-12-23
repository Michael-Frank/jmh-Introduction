package de.frank.jmh.algorithms;

import org.apache.commons.lang3.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.*;
import org.openjdk.jmh.runner.options.*;

import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.*;

/*--
The use case:
===============
A csv may contain empty lines (all columns are empty e.g: ';;;/n' or a totally empty line '/n' which have to be removed.
The csv framework at hand offers a .filter(Predicate<String[]>) option to check & reject parsed lines.
Test different approaches to check the columns for emptiness (streams, forEach, for-i)

Environment
===============
Mac OSX 10.15.3 catalina| 6-Core Intel Core i9| JMH version: 1.23| OpenJDK 64-Bit Server VM, 15.0.1+8

Result
========
(mode throughput @ 1000 lines per test sample with number of columns and emptyLinePercentage% chances as listed)
higher is better


       |              columns:       3         3        3         3          3 |      10        10        10       10      10 |     100      100      100      100      100
       |  emptyLinePercentage:      0%       10%       50%       90%      100% |      0%       10%       50%      90%    100% |      0%      10%      50%      90%     100%   Average
winner |filterForEach          617.090   279.489   203.423   186.438   189.555 | 547.058   212.248   142.220   93.073  85.826 | 312.168   80.421   21.382   14.710   13.450   199.903
second |filterForI             579.445   275.337   218.496   184.190   186.178 | 561.799   209.771   135.435   96.672  89.057 | 309.089   86.272   21.923   14.302   13.779   198.783
looser |filterStreamAnyMatch    58.919    42.157    37.901    40.564    34.428 |  58.961    45.842    32.022   29.105  31.226 |  53.793   24.686    8.391    5.254    5.495    33.916

*) As all checks short-circuit on the first found non-empty column, a empty line processes SLOWER then a non-empty line, as all columns had to be checked. So a higher number of empty lines decreases the throughput.

Raw result
================
Benchmark               columns    emptyLinePercentage   Mode  Cnt   Score   Error  Units
filterForEach                 3                      0  thrpt   15  617090 ± 37964  ops/s
filterForEach                 3                   0.10  thrpt   15  279489 ± 23485  ops/s
filterForEach                 3                   0.50  thrpt   15  203423 ± 15555  ops/s
filterForEach                 3                    0.9  thrpt   15  186438 ± 11845  ops/s
filterForEach                 3                    1.0  thrpt   15  189555 ± 11957  ops/s
filterForEach                10                      0  thrpt   15  547058 ± 58510  ops/s
filterForEach                10                   0.10  thrpt   15  212248 ±  9352  ops/s
filterForEach                10                   0.50  thrpt   15  142220 ±  7786  ops/s
filterForEach                10                    0.9  thrpt   15   93073 ±  8335  ops/s
filterForEach                10                    1.0  thrpt   15   85826 ±  9884  ops/s
filterForEach               100                      0  thrpt   15  312168 ± 32717  ops/s
filterForEach               100                   0.10  thrpt   15   80421 ±  8799  ops/s
filterForEach               100                   0.50  thrpt   15   21382 ±  2032  ops/s
filterForEach               100                    0.9  thrpt   15   14710 ±  1259  ops/s
filterForEach               100                    1.0  thrpt   15   13450 ±   508  ops/s
filterForI                    3                      0  thrpt   15  579445 ± 25226  ops/s
filterForI                    3                   0.10  thrpt   15  275337 ± 27130  ops/s
filterForI                    3                   0.50  thrpt   15  218496 ±  8615  ops/s
filterForI                    3                    0.9  thrpt   15  184190 ± 13517  ops/s
filterForI                    3                    1.0  thrpt   15  186178 ±  8042  ops/s
filterForI                   10                      0  thrpt   15  561799 ± 34393  ops/s
filterForI                   10                   0.10  thrpt   15  209771 ± 23325  ops/s
filterForI                   10                   0.50  thrpt   15  135435 ± 10164  ops/s
filterForI                   10                    0.9  thrpt   15   96672 ±  3542  ops/s
filterForI                   10                    1.0  thrpt   15   89057 ±  2764  ops/s
filterForI                  100                      0  thrpt   15  309089 ± 31244  ops/s
filterForI                  100                   0.10  thrpt   15   86272 ±  7833  ops/s
filterForI                  100                   0.50  thrpt   15   21923 ±  1010  ops/s
filterForI                  100                    0.9  thrpt   15   14302 ±   666  ops/s
filterForI                  100                    1.0  thrpt   15   13779 ±   369  ops/s
filterStreamAnyMatch          3                      0  thrpt   15   58919 ±  2421  ops/s
filterStreamAnyMatch          3                   0.10  thrpt   15   42157 ±  6583  ops/s
filterStreamAnyMatch          3                   0.50  thrpt   15   37901 ±  2815  ops/s
filterStreamAnyMatch          3                    0.9  thrpt   15   40564 ±  2914  ops/s
filterStreamAnyMatch          3                    1.0  thrpt   15   34428 ±  1795  ops/s
filterStreamAnyMatch         10                      0  thrpt   15   58961 ±  2148  ops/s
filterStreamAnyMatch         10                   0.10  thrpt   15   45842 ±  1908  ops/s
filterStreamAnyMatch         10                   0.50  thrpt   15   32022 ±   815  ops/s
filterStreamAnyMatch         10                    0.9  thrpt   15   29105 ±   629  ops/s
filterStreamAnyMatch         10                    1.0  thrpt   15   31226 ±   385  ops/s
filterStreamAnyMatch        100                      0  thrpt   15   53793 ±   641  ops/s
filterStreamAnyMatch        100                   0.10  thrpt   15   24686 ±  1123  ops/s
filterStreamAnyMatch        100                   0.50  thrpt   15    8391 ±   216  ops/s
filterStreamAnyMatch        100                    0.9  thrpt   15    5254 ±    73  ops/s
filterStreamAnyMatch        100                    1.0  thrpt   15    5495 ±   196  ops/s
 */
@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(3)
@State(Scope.Benchmark)
public class FilterArrayJMH {

    @State(Scope.Thread)
    public static class TestData {
        //@Param({"1000"})
        int lines = 1000;

        @Param({"3", "10", "100"})
        int columns;

        @Param({"0", //never
                "0.10", //almost never empty
                "0.50", //50/50
                "0.9",//almost all are empty
                "1.0"//all are empty
        })
        double emptyLinePercentage;

        private List<String[]> rows;


        public TestData() {
            //For jmh
        }

        /**
         * For manual correctness tests
         */
        public TestData(int lines, int columns, double nullLinePercentage) {
            this.lines = lines;
            this.columns = columns;
            this.emptyLinePercentage = nullLinePercentage;
            doSetup();
        }


        @Setup(Level.Iteration)
        public void doSetup() {
            this.rows = generateList(lines, columns, emptyLinePercentage);
        }

    }


    @Benchmark
    public int filterStreamAnyMatch(TestData data) {
        //LOSER - FACTOR 10! (90%) slower then the loops
        return filterTest(data, columns -> Arrays.stream(columns).anyMatch(StringUtils::isNotEmpty));
    }

    @Benchmark
    public int filterForEach(TestData data) {
        //! WINNER
        return filterTest(data, columns -> {
            for (String column : columns) {
                if (StringUtils.isNotEmpty(column)) {
                    return true;
                }
            }
            return false;
        });
    }

    @Benchmark
    public int filterForI(TestData data) {
        // 2nd
        return filterTest(data, columns -> {
            for (int i = 0; i < columns.length; i++) {
                if (StringUtils.isNotEmpty(columns[i])) {
                    return true;
                }
            }
            return false;
        });
    }

    private static int filterTest(TestData data, Predicate<String[]> predicate) {
        int i = 0;
        for (String[] row : data.rows) {
            if (predicate.test(row)) {
                i++;
            }
        }
        return i;
    }


    public static void main(String[] args) throws RunnerException {
        TestData in = new TestData(3, 3, 0.50);
        IntStream.rangeClosed(1, 5)
                 .mapToObj(i -> new TestData(3, 3, 0.50))
                 .forEach(d -> d.rows.forEach(r -> System.out.println(String.join(";", r))));

        System.out.println(new FilterArrayJMH().filterForEach(in));
        System.out.println(new FilterArrayJMH().filterForI(in));
        System.out.println(new FilterArrayJMH().filterStreamAnyMatch(in));

        Options opt = new OptionsBuilder()
                .include(FilterArrayJMH.class.getName())
                .result(String.format("%s_%s.json",
                                      DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                                      FilterArrayJMH.class.getSimpleName()))
                .build();
        new Runner(opt).run();
    }


    public static List<String[]> generateList(int lines, int columnCount, double emptyLinePercentage) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        List<String[]> result = new ArrayList<>(lines);
        for (int i = 0; i < lines; i++) {

            String[] columns = new String[columnCount];
            if (rnd.nextDouble() <= emptyLinePercentage) {
                Arrays.fill(columns, "");//empty
            } else {
                Arrays.fill(columns, RandomStringUtils.randomAlphanumeric(10));//non empty
            }
            result.add(columns);
        }
        return result;
    }

}
