package de.frank.jmh.algorithms;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.RandomStringUtils;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
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

Benchmark                             (match)  (matchRate)  Mode  Cnt       Score        Error  Units
FindNthLineInString.indexOfLoop     VERY_LONG          1.0  avgt    5   94055,430 ±  28740,595  ns/op
FindNthLineInString.ioUtils         VERY_LONG          1.0  avgt    5  244810,938 ±   9062,228  ns/op
FindNthLineInString.stringSplit     VERY_LONG          1.0  avgt    5  803189,442 ±  27064,930  ns/op
FindNthLineInString.bufferedReader  VERY_LONG          1.0  avgt    5  912003,162 ± 178233,162  ns/op

FindNthLineInString.indexOfLoop          LONG          1.0  avgt    5    6042,659 ±   4206,065  ns/op
FindNthLineInString.bufferedReader       LONG          1.0  avgt    5    8521,408 ±   3901,423  ns/op
FindNthLineInString.stringSplit          LONG          1.0  avgt    5   64262,164 ±   3641,117  ns/op
FindNthLineInString.ioUtils              LONG          1.0  avgt    5   70808,625 ±   7224,122  ns/op

FindNthLineInString.indexOfLoop         SHORT          1.0  avgt    5     321,004 ±    167,260  ns/op
FindNthLineInString.stringSplit         SHORT          1.0  avgt    5     854,795 ±     25,756  ns/op
FindNthLineInString.bufferedReader      SHORT          1.0  avgt    5    4349,251 ±    744,567  ns/op
FindNthLineInString.ioUtils             SHORT          1.0  avgt    5    4423,357 ±     40,905  ns/op

Process finished with exit code 0

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


        public String lines;
        public int lineNo;

        @Setup
        public void generateString() {
            ThreadLocalRandom r = ThreadLocalRandom.current();

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < match.lines; i++) {
                String line = RandomStringUtils.random(r.nextInt(30, 80/*lineLenght*/));
                sb.append(line).append("\n");
            }
            this.lines = sb.toString();
            this.lineNo = Integer.MAX_VALUE; //no match
            if (r.nextDouble() <= matchRate) {
                this.lineNo = r.nextInt(match.lines);
            }

        }
    }


    @Benchmark
    public String ioUtils(InputString s) {
        return ioUtilsNthLine(s.lines, s.lineNo);
    }

    @Benchmark
    public String stringSplit(InputString s) {
        return stringSplit(s.lines, s.lineNo);
    }

    @Benchmark
    public String bufferedReader(InputString s) {
        return bufferedReader(s.lines, s.lineNo);
    }

    @Benchmark
    public static String indexOfLoop(InputString s) {
        return indexOfLoop(s.lines, s.lineNo);
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

    public static String ioUtilsNthLine(String input, int n) {
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

}
