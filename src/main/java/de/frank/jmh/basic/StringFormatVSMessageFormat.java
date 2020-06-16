package de.frank.jmh.basic;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/*--
	Compares the performance of various approaches of String concatenation and String-/Message-format implementations

    Learning's:
    - String-/Message-formatter's are fairly expensive! Don't use them mindlessly in hot code paths.
    - String.format is the most expensive but also the most powerful. Use wisely.
    - Use + based String concatenation pattern wherever possible. e.g: String msg  = "hello" + name + "!";

	###Disclaimer#######################################
	all of this obviously only matters in HOT code paths
	####################################################

	Lower is better- SINGLE THREADED  @ JDK 1.8.0_212, OpenJDK 64-Bit Server VM, 25.212-b04
    Benchmark                                   Mode  Cnt     Score     Error   Units  gc.alloc.rate.norm
    concatString_4Strings                       avgt   15    47,300 ±   7,376  ns/op   392,000 ± 0,001 B/op # clear winner in this case - simple and fast
    stringBuilder_4Strings                      avgt   15    43,847 ±   4,372  ns/op   392,000 ± 0,001 B/op # same ase concatString but less readable
    slf4jMessageFormatter_4Strings              avgt   15   181,240 ±  17,598  ns/op  1136,000 ± 0,001 B/op # only supports simple '{}' placeholders, but very! fast for a generic formatter
    messageFormat_4Strings                      avgt   15   728,452 ±  12,632  ns/op  3752,000 ± 0,001 B/op # Message format is a nice trade-off - speed is ok-ish  but only knows "strings". Bonus: The numbered patterns make it easy to read: {1}
      messageFormatExternalPattern_4Strings     avgt   15   797,308 ± 106,936  ns/op  3752,000 ± 0,001 B/op   # storing the pattern string inside a static final field does not matter(from a performance point of view)
      messageFormatCached_4Strings              avgt   15   288,953 ±  44,721  ns/op  1960,000 ± 0,001 B/op   # but caching the pre-parsed MessageFormat pattern as a field is a good idea!
    stringFormat_4Strings                       avgt   15  1100,079 ±  72,907  ns/op  3136,000 ± 0,001 B/op # very powerful, but at a very high price. Be careful using this in hot code.


    some VERY special cases, showing what is possible API wise but which probably are not worth it.
    Please don't use that in production code :-)
    concatManualPreCalculated_sharedBuffer      avgt   15    39,709 ±   4,088  ns/op   392,000 ± 0,001 B/op # best you could do manually, but extremely unlikely corner case
    preCalcBufferAndCopyInto                    avgt   15    82,515 ±   5,713  ns/op   632,000 ± 0,001 B/op #  lamdaMetaFactory can automatically do this trick for you - but 3x better in terms of performance

    AntiPatterns - dont do them
    plusEqualsConcatAntiPattern_4Strings        avgt   15    47,774 ±   6,798  ns/op   392,000 ± 0,001 B/op # yes it work well in simple cases..
    plusEqualsConcatAntiPatternLoop_4Strings    avgt   15   148,897 ±   6,044  ns/op  1880,000 ± 0,001 B/op # ..but add a loop and suffer!

	*/

/**
 * @author Michael Frank
 * @version 1.0 13.05.2018
 */
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(3)
@BenchmarkMode({Mode.AverageTime})
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Threads(1)
public class StringFormatVSMessageFormat {

    private String param0 = "x-application/json";
    private String param1 = UUID.randomUUID().toString();
    private String param2 = UUID.randomUUID().toString();
    private String param3 = UUID.randomUUID().toString();

    private static final String MESSAGE_FORMAT_TEMPLATE =
            "SomeToStringClass:\nparam0 {0}\nparam1 {1}\nparam2 {2}\nparam3 {3}";
    private static final MessageFormat MESSAGE_FORMAT = new MessageFormat(MESSAGE_FORMAT_TEMPLATE);

    public static void main(String[] args) throws RunnerException {
        verifyMethodsProduceSameResults();
        Options opt = new OptionsBuilder()
                .include(StringFormatVSMessageFormat.class.getName() + ".*")//
                .addProfiler(GCProfiler.class)
                .build();
        new Runner(opt).run();
    }


    @Benchmark
    public String concatString_4Strings() {
        return "SomeToStringClass:\nparam0 " + param0//
               + "\nparam1 " + param1 //
               + "\nparam2 " + param3//
               + "\nparam3 " + param2;//
    }

    @Benchmark
    //Essentially equal to concatString();
    public String stringBuilder_4Strings() {
        return new StringBuilder()//
                                  .append("SomeToStringClass:\nparam0 ").append(param0)//
                                  .append("\nparam1 ").append(param1)//
                                  .append("\nparam2 ").append(param3)//
                                  .append("\nparam3 ").append(param2)//
                                  .toString();//
    }

    @Benchmark
    public String messageFormat_4Strings() {
        return MessageFormat.format("SomeToStringClass:\nparam0 {0}\nparam1 {1}\nparam2 {2}\nparam3 {3}"
                , param0, param1, param3, param2);
    }

    @Benchmark
    public String messageFormatExternalPattern_4Strings() {
        return MessageFormat.format(MESSAGE_FORMAT_TEMPLATE, param0, param1, param3, param2);
    }

    @Benchmark
    public String messageFormatCached_4Strings() {
        return MESSAGE_FORMAT.format(new Object[]{param0, param1, param3, param2});
    }


    @Benchmark
    public String slf4jMessageFormatter_4Strings() {
        return org.slf4j.helpers.MessageFormatter
                .arrayFormat("SomeToStringClass:\nparam0 {}\nparam1 {}\nparam2 {}\nparam3 {}",
                             new Object[]{param0, param1, param3, param2})
                .getMessage();
    }

    @Benchmark
    public String stringFormat_4Strings() {
        return String.format("SomeToStringClass:\nparam0 %s\nparam1 %s\nparam2 %s\nparam3 %s", param0, param1, param3,
                             param2);
    }


    // yes - it can just be as fast as concat string in this simple case, because the
    // compiler can optimize it.
    // But this is deadly in loops! ( and its a plain ugly coding style)
    @Benchmark
    public String plusEqualsConcatAntiPattern_4Strings() {
        // DONT YOU EVER DO THIS!
        String r = "SomeToStringClass:\nparam0 ";
        r += param0;
        r += "\nparam1 ";
        r += param1;
        r += "\nparam2 ";
        r += param3;
        r += "\nparam3 ";
        r += param2;
        return r;
    }

    //just to show how bad this is...
    @Benchmark
    public String plusEqualsConcatAntiPatternLoop_4Strings() {
        String[] params = {"SomeToStringClass:\nparam0 ", param0, "\nparam1 ", param1, "\nparam2 ",
                param3, "\nparam3 ", param2};
        String r = "";
        for (String param : params) {
            r += param;
        }

        return r;
    }


    @Benchmark
    public String preCalcBufferAndCopyInto() {
        //basically what the LamdaMetaFactory can do automatically for you 3x better.
        // we do not have its access to the cool jdk internal 0-Copy new String access paths :(
        String[] params = {"SomeToStringClass:\nparam0 ", param0, "\nparam1 ", param1, "\nparam2 ",
                param3, "\nparam3 ", param2};
        int len = 0;
        for (String s : params) {
            len += s.length();
        }
        int pos = 0;
        char[] buf = new char[len];

        for (String s : params) {
            int count = s.length();
            s.getChars(0, count, buf, pos);
            pos += count;
        }
        return new String(buf);
    }


    // *insert into pre-filled* variant
    // This variant only works if the length of the tokens never changes!!!
    private int[] insertPos = {26, 52, 96, 140};
    private char[] buffer = concatString_4Strings().toCharArray();
    String[] tokens = {param0, param1, param3, param2};

    @Benchmark
    public String concatManualPreCalculated_sharedBuffer() {
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            token.getChars(0, token.length(), buffer, insertPos[i]);
        }
        return new String(buffer);
    }

    // VERIFICATION

    private static void verifyMethodsProduceSameResults() {
        StringFormatVSMessageFormat b = new StringFormatVSMessageFormat();
        String expected = b.concatString_4Strings();
        System.out.println("Expected: " + expected);
        Arrays.stream(StringFormatVSMessageFormat.class.getMethods())
              .filter(m -> m.getAnnotation(Benchmark.class) != null)
              .forEach(method -> b.methodProducesExpectedResult(method, expected));
    }

    private void methodProducesExpectedResult(Method method, String expected) {
        try {
            String r = (String) method.invoke(this, null);
            if (!r.equals(expected)) {
                throw new VerifyError(
                        "ERROR: method '" + method.getName() + "' does not produce expected result. but returned: \n" +
                        r + "\nExpected:\n" + expected);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
