package de.frank.jmh.algorithms;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/*--
Evaluation of a code piece that looked allot like premature optimization. Original Author had good intents but could have
done better and probably did not have JMH to verify his optimizations.

#######################
Evaluation of results #
#######################
=> all values in ns/operation (lower is better)
================================================
                                    indentation(number of spaces to the left)
                                    0   2   8   16   31   32   64    128
indentOriginal_                     35  30  31  188  327  362  640  1205  original - complex and but still not good

Refactorings - keeping the original idea
indentRefactoredStringBuilderFixed_ 29  36  35  119  157  187  291   545  still with builder, but a lot better by eliminating the obvious bugs
indentRefactoredCharArray_          24  29  30   83   88  122  148   257  **winner/generalPurpose** how it should have been (if you want to keep the switch)
indentRefactoredFillCharArray_      24  29  30   84   90  118  140   243  use Arrays.fill instead of loop (not much difference :-( )
indentRefactoredStaticCharArray_    29  34  35   77   79  106  129   219  has limitation on max indentation - currently no fallback

Redo from scratch
indentCharArrayFill_                48  52  57   77   84  104  129   235  super simple and scales well - but penalty for small values
indentStaticCharArray_              26  40  41   62   66   90  111   195  has limitation on max indentation - currently no fallback for big values
indentStringBuilderLoop_            17  32  49  103  153  188  323   599  **winner/simplicity**  very simple and ok-ish performance
indentStringBuilderStaticCharArray  27  32  32   56   61   88  106   174  has limitation on max indentation - currently no fallback
indentConcurrentHashMapCache        44  48  49   71   71   97  111   174  DONT! Use- A great waste of memory - just for comparision what can be done in a time memory tradeoff
                                                                          bad performance for low values it the caches overhead. uses arrays.fill internally.


Conclusion:
The original version tried to optimize the "common case" of indenting 1-8 spaces. Depending on the usecase it may be worthwhile.#
But the uncommon case has obvious performance issues. The number of LOC for such a simple task seams to be off.
If the usecase is really this performance critical, there are better options- which we have explored during this test.
First of all, challenging the overall design decision: when working with indentation, probably a StringBuilder or stream is used somewhere higher up in the call chain.
So instead of generating intermediate Strings, why not write the ' ' characters directly into this buffer instead of creating and returning a new String?
To avoid the looping, an static char [] filled with spaces can be used as bulk buffer. This gives the overall best performance at the cost of some length restrictions.
*/

/**
 * @author Michael Frank
 * @version 1.0 13.05.2018
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(3)
public class IndentationJMH {

    /**
     * Bonus: dont use values for your benchmark that are 2^n (power of two's)
     * You will get skewed results as there are special optimizations in the JVM for such values
     * You can observe such a skew if you compare the results for 31,32,33..
     */
    @Param({"0", "2", "5", "10", "31", "32", "33", "50", "100"})
    int indentationDepth;

    private ConcurrentHashMap<Integer, String> buffer;

    private StringBuilder sharedBuilder;

    private static final char[] SPACES = new char[256];

    static {
        Arrays.fill(SPACES, ' ');
    }

    @Setup
    public void init() {

        buffer = new ConcurrentHashMap<>(16);
        sharedBuilder = new StringBuilder();
    }


    //Disclaimer:
    //All benchmarks do an additional + "foo" - as this was the way it was originally used.
    //Bonus: this shows
    @Benchmark
    public String original() {
        return indentOriginal(indentationDepth) + "foo";
    }


    @Benchmark
    public String switch_and_StringBuilder_FixedSize() {
        return indentRefactoredStringBuilderFixedSize(indentationDepth) + "foo";
    }

    @Benchmark
    public String switch_and_charArray() {
        return indentRefactoredCharArray(indentationDepth) + "foo";
    }

    @Benchmark
    public String switch_and_copyOfStaticCharArray() {
        return indentRefactoredCopyOfStaticCharArray(indentationDepth) + "foo";
    }

    @Benchmark
    public String swith_and__fillCharArray() {
        return indentRefactoredCharArrayFill(indentationDepth) + "foo";
    }

    @Benchmark
    public String fillCharArray() {
        return indentFillCharArray(indentationDepth) + "foo";
    }

    @Benchmark
    public String copyOfStaticCharArray() {
        return indentCopyOfStaticCharArray(indentationDepth) + "foo";
    }

    @Benchmark
    public String stringBuilder_Loop() {
        StringBuilder sb = new StringBuilder();
        indentStringBuilderLoop(indentationDepth, sb);
        return sb.append("foo").toString();
    }

    //chances are, if you need indentation you probably doing a lot of string concat.
    // => use a StringBuilder, dont return new Strings
    @Benchmark
    public String stringBuilder_staticCharArray() {
        StringBuilder sb = new StringBuilder();
        indentStringBuilderStaticCharArray(indentationDepth, sb);
        return sb.append("foo").toString();
    }

    //chances are, if you need indentation you probably doing a lot of string concat.
    // => use a StringBuilder, dont return new Strings
    // and while you are at it, you might call indent() multiple times per builder
    @Benchmark
    public String stringBuilder_staticCharArray_reuseBuilder() {
        sharedBuilder.setLength(0);
        indentStringBuilderStaticCharArray(indentationDepth, sharedBuilder);
        return sharedBuilder.append("foo").toString();
    }

    @Benchmark
    public String concurrentMapCache_fillCharArray() {
        //yes this is stupid - its just there to show you that it is indeed a stupid idea
        return buffer.computeIfAbsent(indentationDepth, IndentationJMH::indentFillCharArray) + "foo";
    }

    /*
    This "gem" was found in customer code
     */
    private static String indentOriginal(int indentationIndex) {
        switch (indentationIndex) {
            //yup... the ZERO case was missing
            case 1:
                return " ";
            case 2:
                return "  ";
            case 3:
                return "   ";
            case 4:
                return "    ";
            case 5:
                return "     ";
            case 6:
                return "      ";
            case 7:
                return "       ";
            case 8:
                return "        ";
            default:
                StringBuffer buf = new StringBuffer();
                for (int i = 0; i < indentationIndex; ++i) {
                    buf.append(" ");
                }
                return buf.toString();
        }
    }


    private static String indentRefactoredStringBuilderFixedSize(int indentationIndex) {
        switch (indentationIndex) {
            case 0:
                return "";
            case 1:
                return " ";
            case 2:
                return "  ";
            case 3:
                return "   ";
            case 4:
                return "    ";
            case 5:
                return "     ";
            case 6:
                return "      ";
            case 7:
                return "       ";
            case 8:
                return "        ";
            default:
                StringBuilder buf = new StringBuilder(indentationIndex);
                for (int i = 0; i < indentationIndex; i++) {
                    buf.append(' ');
                }
                return buf.toString();
        }
    }

    private static String indentRefactoredCharArray(int indentationIndex) {
        switch (indentationIndex) {
            case 0:
                return "";
            case 1:
                return " ";
            case 2:
                return "  ";
            case 3:
                return "   ";
            case 4:
                return "    ";
            case 5:
                return "     ";
            case 6:
                return "      ";
            case 7:
                return "       ";
            case 8:
                return "        ";
            default:
                char[] buf = new char[indentationIndex];
                for (int i = 0; i < indentationIndex; i++) {
                    buf[i] = ' ';
                }
                return new String(buf);
        }
    }

    private static String indentRefactoredCharArrayFill(int indentationIndex) {
        switch (indentationIndex) {
            case 0:
                return "";
            case 1:
                return " ";
            case 2:
                return "  ";
            case 3:
                return "   ";
            case 4:
                return "    ";
            case 5:
                return "     ";
            case 6:
                return "      ";
            case 7:
                return "       ";
            case 8:
                return "        ";
            default:
                char[] tmp = new char[indentationIndex];
                Arrays.fill(tmp, ' ');
                return new String(tmp);
        }
    }

    private static String indentRefactoredCopyOfStaticCharArray(int indentationIndex) {
        switch (indentationIndex) {
            case 0:
                return "";
            case 1:
                return " ";
            case 2:
                return "  ";
            case 3:
                return "   ";
            case 4:
                return "    ";
            case 5:
                return "     ";
            case 6:
                return "      ";
            case 7:
                return "       ";
            case 8:
                return "        ";
            default:
                return new String(SPACES, 0, indentationIndex);
        }
    }

    private static String indentCopyOfStaticCharArray(int indentationIndex) {
        return new String(SPACES, 0, indentationIndex);
    }

    private static String indentFillCharArray(int indentationIndex) {
        char[] tmp = new char[indentationIndex];
        Arrays.fill(tmp, ' ');
        return new String(tmp);
    }


    private static void indentStringBuilderLoop(int indentationIndex, StringBuilder buf) {
        for (int i = 0; i < indentationIndex; i++) {
            buf.append(' ');
        }
    }

    private static void indentStringBuilderStaticCharArray(int indentationIndex, StringBuilder buf) {
        buf.append(SPACES, 0, indentationIndex);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(IndentationJMH.class.getName())
                .result(String.format("%s_%s.json",
                        DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                        IndentationJMH.class.getSimpleName()))
                // #########
                // COMPILER
                // #########
                // make sure we dont see compiling of our benchmark code during
                // measurement.
                // if you see compiling => more warmup
                .jvmArgsAppend("-XX:+UnlockDiagnosticVMOptions")
                //.jvmArgsAppend("-XX:+PrintCompilation")
                // .jvmArgsAppend("-XX:+PrintInlining")
                // .jvmArgsAppend("-XX:+PrintAssembly")
                // .jvmArgsAppend("-XX:+PrintOptoAssembly") //c2 compiler only
                // More compiler prints:
                // .jvmArgsAppend("-XX:+PrintInterpreter")
                // .jvmArgsAppend("-XX:+PrintNMethods")
                // .jvmArgsAppend("-XX:+PrintNativeNMethods")
                // .jvmArgsAppend("-XX:+PrintSignatureHandlers")
                // .jvmArgsAppend("-XX:+PrintAdapterHandlers")
                // .jvmArgsAppend("-XX:+PrintStubCode")
                // .jvmArgsAppend("-XX:+PrintCompilation")
                // .jvmArgsAppend("-XX:+PrintInlining")
                // .jvmArgsAppend("-XX:+TraceClassLoading")
                // .jvmArgsAppend("-XX:PrintAssemblyOptions=syntax")

                // #########
                // Profling
                // #########
                //
                // .jvmArgsAppend("-XX:+UnlockCommercialFeatures")
                // .jvmArgsAppend("-XX:+FlightRecorder")
                //
                // .jvmArgsAppend("-XX:+UnlockDiagnosticVMOptions")
                // .jvmArgsAppend("-XX:+PrintSafepointStatistics")
                // .jvmArgsAppend("-XX:+DebugNonSafepoints")
                //
                // required for external profilers like "perf" to show java
                // frames in their traces
                .jvmArgsAppend("-XX:+PerserveFramePointer")
                .build();
        new Runner(opt).run();

    }

}
