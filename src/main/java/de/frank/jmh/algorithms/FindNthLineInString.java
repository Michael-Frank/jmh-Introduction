package de.frank.jmh.algorithms;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/*--
Usecase: a Parser takes a (very huge) program code as String and on pars error returns a "Location" object.
In error case, we want to conveniently provide the user with the matching line in addition to the Location, to speed up debugging.

As we parse thousands of program code strings and want to (batch) review the errors later, this debug functionality should not slow down the process.

Result:
========
Does the speed matter for even a 1000 errors: NO

However, stringSplit is not that much easier to read than indexOfLoop .... so use whatever you like

Note:
======
There are certainly many other approaches like, e.g. scanner, tokenizer, ... and what not. Feel free to add.

Number of lines:
- VERY_LONG(10_000)
- LONG(1_000)
- SHORT(15)

Benchmark                                            (numLines)  (matchRate)  Mode  Cnt        Score         Error  Units
FindNthLineInString.baseline                              SHORT          1.0  avgt    5       25,707 ±       4,617  ns/op
FindNthLineInString.multi_stringSplit                     SHORT          1.0  avgt    5      712,077 ±     262,475  ns/op #if we KNOW that we  have to search for multiple hits, split&cache is better
FindNthLineInString.multi_indexOfLoop                     SHORT          1.0  avgt    5     1489,061 ±     540,280  ns/op
FindNthLineInString.multi_bufferedReader                  SHORT          1.0  avgt    5    29662,778 ±    8237,780  ns/op

FindNthLineInString.single_indexOfLoop                    SHORT          1.0  avgt    5      156,574 ±      28,522  ns/op # winner - avoids unnecessary substrings except for the searched one#wins
FindNthLineInString.single_stringSplit                    SHORT          1.0  avgt    5      648,959 ±     147,680  ns/op
FindNthLineInString.single_apacheCommonsSplit             SHORT          1.0  avgt    5      735,089 ±      88,198  ns/op
FindNthLineInString.single_apacheIoUtilsLineIterator      SHORT          1.0  avgt    5     2752,420 ±    1115,887  ns/op  # LineIterator == BufferedReader internally
FindNthLineInString.single_bufferedReader                 SHORT          1.0  avgt    5     2792,161 ±     602,648  ns/op  # good ol' buffered reader
FindNthLineInString.single_apacheIoUtilsReadLines         SHORT          1.0  avgt    5     3421,074 ±    2270,597  ns/op

FindNthLineInString.baseline                               LONG          1.0  avgt    5       23,746 ±       5,695  ns/op
FindNthLineInString.multi_indexOfLoop                      LONG          1.0  avgt    5    95273,273 ±   35779,312  ns/op
FindNthLineInString.multi_stringSplit                      LONG          1.0  avgt    5    50330,825 ±   16486,966  ns/op #if we KNOW that we  have to search for multiple hits, split&cache is better
FindNthLineInString.multi_bufferedReader                   LONG          1.0  avgt    5   400996,174 ±  114214,666  ns/op

FindNthLineInString.single_indexOfLoop                     LONG          1.0  avgt    5     9255,972 ±     899,068  ns/op # winner - avoids unnecessary substrings except for the searched one#wins
FindNthLineInString.single_apacheIoUtilsLineIterator       LONG          1.0  avgt    5    36931,410 ±    3629,897  ns/op # LineIterator == BufferedReader internally
FindNthLineInString.single_bufferedReader                  LONG          1.0  avgt    5    38722,763 ±    6951,236  ns/op # good ol' buffered reader
FindNthLineInString.single_stringSplit                     LONG          1.0  avgt    5    48141,969 ±    3916,079  ns/op
FindNthLineInString.single_apacheCommonsSplit              LONG          1.0  avgt    5    55340,895 ±    2261,303  ns/op # apache stuff really falls behind in speed over compared to jdk defaults in latest java lang/Jvm versions
FindNthLineInString.single_apacheIoUtilsReadLines          LONG          1.0  avgt    5    81937,881 ±    5832,133  ns/op # apache stuff really falls behind in speed over compared to jdk defaults in latest java lang/Jvm versions


FindNthLineInString.baseline                          VERY_LONG          1.0  avgt    5       22,697 ±       2,674  ns/op
FindNthLineInString.multi_stringSplit                 VERY_LONG          1.0  avgt    5   613049,942 ±   67941,025  ns/op
FindNthLineInString.multi_indexOfLoop                 VERY_LONG          1.0  avgt    5  1030290,917 ±  145772,879  ns/op
FindNthLineInString.multi_bufferedReader              VERY_LONG          1.0  avgt    5  3928724,216 ± 1424575,036  ns/op

FindNthLineInString.single_indexOfLoop                VERY_LONG          1.0  avgt    5   114718,403 ±   31217,153  ns/op # winner - avoids unnecessary substrings except for the searched one
FindNthLineInString.single_bufferedReader             VERY_LONG          1.0  avgt    5   388395,267 ±   82951,943  ns/op # good ol' buffered reader
FindNthLineInString.single_apacheIoUtilsLineIterator  VERY_LONG          1.0  avgt    5   395083,770 ±   50790,812  ns/op # LineIterator == BufferedReader internally
FindNthLineInString.single_stringSplit                VERY_LONG          1.0  avgt    5   582293,640 ±   31299,491  ns/op
FindNthLineInString.single_apacheCommonsSplit         VERY_LONG          1.0  avgt    5   679038,062 ±  204971,264  ns/op # apache stuff really falls behind in speed over compared to jdk defaults in latest java lang/Jvm versions
FindNthLineInString.single_apacheIoUtilsReadLines     VERY_LONG          1.0  avgt    5   757308,975 ±   25583,379  ns/op # apache stuff really falls behind in speed over compared to jdk defaults in latest java lang/Jvm versions


 */
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1
//        ,jvmArgsAppend = {"-XX:+UseParallelGC", "-Xms1g", "-Xmx1g"}
)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class FindNthLineInString {

    public static void main(String[] args) throws RunnerException {

        System.out.printf("Expected: null       Actual: '%s'%n", indexOfLoop(null, 2));
        System.out.printf("Expected: ''         Actual: '%s'%n", indexOfLoop("", 0));
        System.out.printf("Expected: null       Actual: '%s'%n", indexOfLoop("", 1));
        System.out.printf("Expected: null       Actual: '%s'%n", indexOfLoop("", 2));
        System.out.printf("Expected: 'foo'      Actual: '%s'%n", indexOfLoop("foo", 0));
        System.out.printf("Expected: null       Actual: '%s'%n", indexOfLoop("foo", 1));
        System.out.printf("Expected: null       Actual: '%s'%n", indexOfLoop("foo", 2));
        System.out.printf("Expected: 'foo'      Actual: '%s'%n", indexOfLoop("foo\n", 0));
        System.out.printf("Expected: ''         Actual: '%s'%n", indexOfLoop("foo\n", 1));
        System.out.printf("Expected: null       Actual: '%s'%n", indexOfLoop("foo\n", 2));
        System.out.printf("Expected: 'foo'      Actual: '%s'%n", indexOfLoop("foo\nbar", 0));
        System.out.printf("Expected: 'bar'      Actual: '%s'%n", indexOfLoop("foo\nbar", 1));
        System.out.printf("Expected: null       Actual: '%s'%n", indexOfLoop("foo\nbar", 2));

        Options opt = new OptionsBuilder()//
                .include(FindNthLineInString.class.getName() + ".*")//
                // .result(String.format("%s_%s.json",
                //         DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                //         FindNthLineInString.class.getSimpleName()))
                //   .addProfiler(GCProfiler.class)//
                .build();
        new Runner(opt).run();
    }


    @State(Scope.Thread)
    public static class InputString {


        public enum Match {
            VERY_LONG(10_000),
            LONG(1_000),
            SHORT(15);
            int lines;

            Match(int lines) {
                this.lines = lines;
            }

        }

        @Param({"VERY_LONG", "LONG", "SHORT"})
        public Match match = Match.SHORT;

        // @Param({"0.0", "1.0", "0.5", "0.8"})
        @Param({"1.0"}) //in my special usecase a no-match is virtually non existent
        public double matchRate = 1.0;


        public String textToSearch;
        public int singleLineNo;
        private ArrayList<Integer> multipleLineNo = new ArrayList<>();

        @Setup(Level.Iteration) //our usecase: input is probably different every time
        public void setupStringToSearch() {
            ThreadLocalRandom r = ThreadLocalRandom.current();
            this.textToSearch = randomMultilineText(r, match.lines, 30, 80/*lineLenght*/);
        }


        @Setup(Level.Invocation) //our usecase: input is probably different every time
        public void setupSearchIndexes() {
            ThreadLocalRandom r = ThreadLocalRandom.current();
            this.singleLineNo = randomLine(r, match.lines, matchRate);
            ArrayList<Integer> multi = new ArrayList<>(10);
            for (int i = 0; i < 10; i++) {
                multi.add(randomLine(r, match.lines, matchRate));
            }
            this.multipleLineNo = multi;
        }

        @NotNull
        private String randomMultilineText(ThreadLocalRandom r, int lines, int lineLenMin, int lineLenMax) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < lines; i++) {
                String line = RandomStringUtils.random(r.nextInt(lineLenMin, lineLenMax/*lineLenght*/));
                sb.append(line).append("\n");
            }
            return sb.toString();
        }

        private static int randomLine(ThreadLocalRandom r, int lineCount, double matchRate) {
            var singleLineNo = Integer.MAX_VALUE; //no match
            if (r.nextDouble() <= matchRate) {
                singleLineNo = r.nextInt(lineCount);
            }
            return singleLineNo;
        }
    }

    @Benchmark
    public String baseline(InputString s) {
        return s.textToSearch;
    }

    @Benchmark
    public String single_apacheIoUtilsLineIterator(InputString s) {
        return apacheIoUtilsLineIterator(s.textToSearch, s.singleLineNo);
    }

    @Benchmark
    public String single_apacheIoUtilsReadLines(InputString s) {
        return apacheIoUtilsReadLines(s.textToSearch, s.singleLineNo);
    }

    @Benchmark
    public String single_apacheCommonsSplit(InputString s) {
        return apacheCommonsSplit(s.textToSearch, s.singleLineNo);
    }

    @Benchmark
    public String single_stringSplit(InputString s) {
        return stringSplit(s.textToSearch, s.singleLineNo);
    }

    @Benchmark
    public static void multi_stringSplit(InputString s, Blackhole b) {
        if (s.textToSearch == null) {
            b.consume(null);
            return;
        }
        String[] lines = s.textToSearch.split("\n");
        for (int lineNo : s.multipleLineNo) {
            String r = getNthLineOrNull(lines, lineNo);
            b.consume(r);
        }
    }

    @Benchmark
    public String single_bufferedReader(InputString s) {
        return bufferedReader(s.textToSearch, s.singleLineNo);
    }

    @Benchmark
    public static void multi_bufferedReader(InputString s, Blackhole b) {
        for (int lineNo : s.multipleLineNo) {
            String r = bufferedReader(s.textToSearch, lineNo);
            b.consume(r);
        }
    }

    @Benchmark
    public static String single_indexOfLoop(InputString s) {
        return indexOfLoop(s.textToSearch, s.singleLineNo);
    }

    @Benchmark
    public static void multi_indexOfLoop(InputString s, Blackhole b) {
        for (int lineNo : s.multipleLineNo) {
            String r = indexOfLoop(s.textToSearch, lineNo);
            b.consume(r);
        }
    }


    public static String stringSplit(String input, int n) {
        if (input != null) {
            String[] lines = input.split("\n");
            if (n < lines.length) {
                return lines[n];
            }
        }
        return null;
    }

    public static String getNthLineOrNull(String input[], int n) {
        if (input != null && (n < input.length)) {
            return input[n];
        }
        return null;
    }

    public static String bufferedReader(String input, int n) {
        BufferedReader br = new BufferedReader(new StringReader(input));
        String line;
        int counter = 0;
        try {
            while (((line = br.readLine()) != null)) {
                if (counter++ == n) {
                    return line;
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return null;
    }


    public static String indexOfLoop(String str, int lineNumber) {
        if (str == null) {
            return null;
        }

        int lineStartIndex = 0;
        for (int i = 0; i < lineNumber; i++) {
            lineStartIndex = str.indexOf("\n", lineStartIndex); //find new line symbol from
            if (lineStartIndex < 0) {
                return null; //not found
            }
            //specified index
            lineStartIndex++; //increase the index by 1 so the to skip newLine Symbol on
            //next search or substring method
        }
        int nextLine = str.indexOf("\n", lineStartIndex); //end of line 7
        if (nextLine < 0) {//special: searched line is last line or only ony line in string.
            nextLine = str.length();
        }
        return str.substring(lineStartIndex, nextLine);
    }

    public static String apacheIoUtilsLineIterator(String input, int n) {
        LineIterator it = IOUtils.lineIterator(new StringReader(input));
        int count = 0;
        String line = null;

        while (it.hasNext()) {
            count++;
            line = it.nextLine();

            if (count == n) {
                break;
            }
        }

        return count == n ? line : null;
    }

    public static String apacheIoUtilsReadLines(String input, int n) {
        if (input != null) {
            List<String> lines = IOUtils.readLines(new StringReader(input));
            if (n < lines.size()) {
                return lines.get(0);
            }
        }
        return null;
    }

    public static String apacheCommonsSplit(String input, int n) {
        if (input != null) {
            var lines = StringUtils.split(input, '\n');
            if (n < lines.length) {
                return lines[n];
            }
        }
        return null;
    }

}
