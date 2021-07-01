package de.frank.jmh.algorithms.wordcount.count;


import com.yevdo.jwildcard.JWildcard;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormat;
import org.openjdk.jmh.results.format.ResultFormatFactory;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.util.NullOutputStream;

import java.io.PrintStream;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/*--
The problem is this:
  read stdin, tokenize into words
  for each word count how often it occurs
  output words and counts, sorted in descending order by count
The idea is to measure in each language how well it performs this basic
problem, how much memory it takes, and how elegant the solution looks.
(Source: https://ptrace.fefe.de/wp/README.txt)
Implementations: https://ptrace.fefe.de/wp/

The 3 submitted java implementations are not optimal and offer room for improvement - "wpck.java" was already pretty close.

Result
==========
found a ~1,35x faster solution then previous best

# VM version: JDK 13.0.1, OpenJDK 64-Bit Server VM, 13.0.1+9
Test '.*Finalists*_100_[3Mb]':
===============================
Benchmark                    (testFile)  Mode  Cnt  Score   Error  Units
wp_myVariant                        3Mb    ss  100  0,135 ± 0,002   s/op  # 1,25x speedup vs wpck, but suffers from parallelisation overhead
wpck                                3Mb    ss  100  0,174 ± 0,003   s/op  # base - parallel (previous fastest)  suffers from parallelisation overhead
wp2                                 3Mb    ss   30  0,099 ± 0,014   s/op  # base - iterative - winner for small files
wp                                  3Mb    ss   30  0,141 ± 0,008   s/op  # base - iterative
Test '.*Finalists*_100_[32Mb]':
===============================
Benchmark                    (testFile)  Mode  Cnt  Score   Error  Units
wp_myVariant                       32Mb    ss  100  0,246 ± 0,005   s/op  # 1,41x speedup vs wpck
wpck                               32Mb    ss  100  0,348 ± 0,008   s/op  # base - parallel (previous fastest)
wp2                                32Mb    ss   30  0,580 ± 0,060   s/op  # base - iterative
wp                                 32Mb    ss   30  0,919 ± 0,095   s/op  # base - iterative
Test '.*Finalists*_100_[160Mb]':
===============================
Benchmark                    (testFile)  Mode  Cnt  Score   Error  Units
wp_myVariant                      160Mb    ss  100  0,625 ± 0,011   s/op  # 1,35x speedup vs wpck
wpck                              160Mb    ss  100  0,841 ± 0,009   s/op  # base - parallel (previous fastest)
wp2                               160Mb    ss   30  2,573 ± 0,280   s/op  # base - iterative
wp                                160Mb    ss   30  3,566 ± 0,535   s/op  # base - iterative




Benchmark                                          (testFile)  Mode  Cnt  Score   Error  Units
WordCountJMH.Finalists.wp2_singleThreaded                 3Mb    ss   30  0,099 ± 0,007   s/op
WordCountJMH.Finalists.wp_myVariantSingle                 3Mb    ss   30  0,115 ± 0,003   s/op
WordCountJMH.Finalists.wp_myVariantSingle2                3Mb    ss   30  0,114 ± 0,003   s/op
WordCountJMH.Finalists.wp_myVariantSingle3                3Mb    ss   30  0,120 ± 0,005   s/op
WordCountJMH.Finalists.wp_myVariantSingle4                3Mb    ss   30  0,126 ± 0,003   s/op
WordCountJMH.Finalists.wp_myVariantSingle4_2              3Mb    ss   30  0,123 ± 0,007   s/op
WordCountJMH.Finalists.wp_myVariant_multiThreaded         3Mb    ss   30  0,147 ± 0,007   s/op
WordCountJMH.Finalists.wpck_multiThreaded                 3Mb    ss   30  0,207 ± 0,017   s/op

*/
@BenchmarkMode({Mode.SingleShotTime})
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 0, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(30)//measure cold time only in own vm instances (simulate as if each benchmark is invoked as command line program)

@State(Scope.Thread)
public class WordCountJMH {

    public static void main(String[] args) throws Exception {
        String[] allFiles = {"3Mb", "32Mb", "160Mb"};
        List<Map.Entry<String, Collection<RunResult>>> results = new ArrayList<>();
       // results.add(runBench(30, ".*Finalists*",  "160Mb"));
        results.add(runBench(30, ".*Finalists*",  "32Mb"));
        //results.add(runBench(30, ".*Finalists*",  "3Mb"));


        ResultFormat textSoutFormater = ResultFormatFactory.getInstance(ResultFormatType.TEXT, System.out);
        results.forEach(e -> {
            System.out.println("\nTest '" + e.getKey() + "':\n===============================");
            textSoutFormater.writeOut(e.getValue());
        });
    }

    public static Map.Entry<String, Collection<RunResult>> runBench(int forks, String includeWildcard, String... testFiles) throws RunnerException {

        Options opt = new OptionsBuilder()
                .include(JWildcard.wildcardToRegex(WordCountJMH.class.getName() + includeWildcard))
                //measure cold time only in own vm instances (simulate as if each benchmark is invoked as command line program)
                .mode(Mode.SingleShotTime)
                .warmupIterations(0)
                .measurementIterations(1)
                .param("testFile", testFiles)
                .forks(forks)
                .result(String.format("%s_%s.json",
                        DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                        WordCountJMH.class.getSimpleName()))
                .build();
        Collection<RunResult> res = new Runner(opt).run();
        return new AbstractMap.SimpleEntry<>(includeWildcard + "_" + forks + "_" + Arrays.toString(testFiles), res);
    }


    @State(Scope.Benchmark)
    public static class Finalists {

        @Param({"3Mb", "32Mb", "160Mb"})
        private String testFile;

        private String[] args;

        @Setup
        public void setup() {
            System.setOut(new PrintStream(new NullOutputStream()));
            String path = String.format("/Users/od.michael.frank/Downloads/fefe wp bench/words%s.txt", testFile);
            args = new String[]{path};//30Mb
        }

        //baseline - previously best impl
        //@Benchmark
        public void wp_singleThreaded() throws Exception {
            wp.main(args);
        }

        //baseline - previously best impl
        @Benchmark
        public void wp2_singleThreaded() throws Exception {
            wp2.main(args);
        }


        //baseline - previously best impl
        @Benchmark
        public void wpck_multiThreaded() throws Exception {
            wpck.main(args);
        }


        //cleand up version of matrix benchmark winner - ready for submission
        @Benchmark
        public void wp_myVariant_multiThreaded() throws Exception {
            wp_myVariant.main(args);
        }

        @Benchmark
        public void wp_myVariantSingle() throws Exception {
            wp_myVariantSingle.main(args);
        }
        @Benchmark
        public void wp_myVariantSingle2() throws Exception {
            wp_myVariantSingle2.main(args);
        }
        @Benchmark
        public void wp_myVariantSingle3() throws Exception {
            wp_myVariantSingle3.main(args);
        }
        @Benchmark
        public void wp_myVariantSingle4() throws Exception {
            wp_myVariantSingle4.main(args);
        }
        @Benchmark
        public void wp_myVariantSingle4_2() throws Exception {
            wp_myVariantSingle4_2.main(args);
        }

    }

}
