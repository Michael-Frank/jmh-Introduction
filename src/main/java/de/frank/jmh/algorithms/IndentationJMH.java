package de.frank.jmh.algorithms;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/*--
##########################
Evaluation Results
#########################
nanoseconds/operation (lower is better)
                                   indentation(number of spaces to the left)
                                    0   2   8   16   31   32   64   128
indentOriginal_                     35  30  31  188  327  362  640  1205  complex and still not good
indentRefactoredStringBuilderFixed_ 29  36  35  119  157  187  291   545  still with builder but got a lot better by eliminating the obvious bugs
indentRefactoredCharArray_          24  29  30   83   88  122  148   257  *winner?* how it should have been (if you want to keep the switch)
indentRefactoredFillCharArray_      24  29  30   84   90  118  140   243  use Arrays.fill instead of loop (not much difference :-( )
indentRefactoredStaticCharArray_    29  34  35   77   79  106  129   219  has limitation on max indentation - currently no fallback
indentCharArrayFill_                48  52  57   77   84  104  129   235  super simple and scales well - but penatly for small values
indentStaticCharArray_              26  40  41   62   66   90  111   195  has limitation on max indentation - currently no fallback
indentStringBuilderLoop_            17  32  49  103  153  188  323   599  *winer @ simple* very simple and ok-ish performance
indentStringBuilderStaticCharArray  27  32  32   56   61   88  106   174  has limitation on max indentation - currently no fallback
indentConcurrentHashMapCache        44  48  49   71   71   97  111   174  DONT! Use- A great waste of memory - just for comparision what can be done in a time memory tradeoff
                                                                         bad performance for low values it the caches overhead. uses arrays.fill internally.


Conclusion:
The original version tried to optimize the "common case" of indenting 1-8 spaces. Depending on the usecase it may be worthwhile. But the uncommon case has obvious performance issues. The number of LOC for such a simple task seams to be off.
If the usecase is really this performance critical, there are better options we now explore.
First of all, when working with indentation, somwhere a StringBuilder or a stream is probably used.
Instead of generating intermediate Strings, why not write ' ' characters directly into the buffer?
To avoid the looping an static char [] filled with spaces can be used as bulk buffer. This gives the overall best performance at the cost of some length restrictions.

##########################
RAW
#########################
# Run complete. Total time: 00:17:40

Benchmark             (indentationDepth)  Mode Cnt    Score      Error  Units
indentCharArrayFill_                  0  avgt  10    48,112  ±   1,871  ns/op
indentCharArrayFill_                  2  avgt  10    51,824  ±   0,713  ns/op
indentCharArrayFill_                  8  avgt  10    56,672  ±   2,919  ns/op
indentCharArrayFill_                 16  avgt  10    76,998  ±   1,984  ns/op
indentCharArrayFill_                 31  avgt  10    84,406  ±   1,800  ns/op
indentCharArrayFill_                 32  avgt  10   104,197  ±   1,290  ns/op
indentCharArrayFill_                 64  avgt  10   129,068  ±   1,936  ns/op
indentCharArrayFill_                128  avgt  10   234,668  ±   2,413  ns/op
indentOriginal_                       0  avgt  10    34,775  ±   1,901  ns/op
indentOriginal_                       2  avgt  10    30,003  ±   0,695  ns/op
indentOriginal_                       8  avgt  10    30,727  ±   0,471  ns/op
indentOriginal_                      16  avgt  10   188,390  ±   2,667  ns/op
indentOriginal_                      31  avgt  10   326,603  ±   4,714  ns/op
indentOriginal_                      32  avgt  10   361,949  ±   9,425  ns/op
indentOriginal_                      64  avgt  10   640,489  ±  20,858  ns/op
indentOriginal_                     128  avgt  10  1204,863  ±  22,185  ns/op
indentRefactoredCharArray_            0  avgt  10    24,069  ±   0,545  ns/op
indentRefactoredCharArray_            2  avgt  10    28,932  ±   0,493  ns/op
indentRefactoredCharArray_            8  avgt  10    30,041  ±   0,432  ns/op
indentRefactoredCharArray_           16  avgt  10    82,778  ±   2,336  ns/op
indentRefactoredCharArray_           31  avgt  10    87,527  ±   2,877  ns/op
indentRefactoredCharArray_           32  avgt  10   122,213  ±   5,625  ns/op
indentRefactoredCharArray_           64  avgt  10   148,352  ±   3,245  ns/op
indentRefactoredCharArray_          128  avgt  10   257,385  ±   4,772  ns/op
indentRefactoredStaticCharArray_      0  avgt  10    28,617  ±   0,807  ns/op
indentRefactoredStaticCharArray_      2  avgt  10    34,135  ±   1,228  ns/op
indentRefactoredStaticCharArray_      8  avgt  10    34,598  ±   1,354  ns/op
indentRefactoredStaticCharArray_     16  avgt  10    77,378  ±   2,835  ns/op
indentRefactoredStaticCharArray_     31  avgt  10    78,719  ±   2,616  ns/op
indentRefactoredStaticCharArray_     32  avgt  10   106,221  ±   3,104  ns/op
indentRefactoredStaticCharArray_     64  avgt  10   129,300  ±   3,799  ns/op
indentRefactoredStaticCharArray_    128  avgt  10   218,597  ±   4,211  ns/op
indentRefactoredStringBuilderFixed_   0  avgt  10    29,342  ±   1,075  ns/op
indentRefactoredStringBuilderFixed_   2  avgt  10    35,583  ±   1,176  ns/op
indentRefactoredStringBuilderFixed_   8  avgt  10    35,339  ±   1,043  ns/op
indentRefactoredStringBuilderFixed_  16  avgt  10   119,247  ±   2,947  ns/op
indentRefactoredStringBuilderFixed_  31  avgt  10   156,887  ±   5,036  ns/op
indentRefactoredStringBuilderFixed_  32  avgt  10   187,133  ±   4,185  ns/op
indentRefactoredStringBuilderFixed_  64  avgt  10   291,118  ±   4,603  ns/op
indentRefactoredStringBuilderFixed_ 128  avgt  10   544,848  ±  10,653  ns/op
indentStaticCharArray_                0  avgt  10    25,879  ±   0,424  ns/op
indentStaticCharArray_                2  avgt  10    39,534  ±   1,114  ns/op
indentStaticCharArray_                8  avgt  10    40,563  ±   4,220  ns/op
indentStaticCharArray_               16  avgt  10    62,170  ±   0,961  ns/op
indentStaticCharArray_               31  avgt  10    65,582  ±   1,222  ns/op
indentStaticCharArray_               32  avgt  10    89,649  ±   1,536  ns/op
indentStaticCharArray_               64  avgt  10   111,215  ±   2,409  ns/op
indentStaticCharArray_              128  avgt  10   194,910  ±   3,876  ns/op
indentStringBuilderLoop_              0  avgt  10    16,883  ±   0,171  ns/op
indentStringBuilderLoop_              2  avgt  10    31,855  ±   0,954  ns/op
indentStringBuilderLoop_              8  avgt  10    49,352  ±   2,071  ns/op
indentStringBuilderLoop_             16  avgt  10   102,611  ±   1,122  ns/op
indentStringBuilderLoop_             31  avgt  10   152,506  ±   4,483  ns/op
indentStringBuilderLoop_             32  avgt  10   187,892  ±   5,390  ns/op
indentStringBuilderLoop_             64  avgt  10   323,382  ±   8,365  ns/op
indentStringBuilderLoop_            128  avgt  10   599,360  ±  21,093  ns/op
indentStringBuilderStaticCharArray    0  avgt  10    26,958  ±   1,271  ns/op
indentStringBuilderStaticCharArray    2  avgt  10    31,553  ±   1,037  ns/op
indentStringBuilderStaticCharArray    8  avgt  10    32,202  ±   0,878  ns/op
indentStringBuilderStaticCharArray   16  avgt  10    56,315  ±   1,737  ns/op
indentStringBuilderStaticCharArray   31  avgt  10    61,386  ±   2,453  ns/op
indentStringBuilderStaticCharArray   32  avgt  10    88,027  ±   2,256  ns/op
indentStringBuilderStaticCharArray   64  avgt  10   105,705  ±   2,011  ns/op
indentStringBuilderStaticCharArray  128  avgt  10   174,350  ±   3,626  ns/op
indentRefactoredFillCharArray_        0  avgt  10    23,940  ±   1,357  ns/op
indentRefactoredFillCharArray_        2  avgt  10    28,836  ±   0,330  ns/op
indentRefactoredFillCharArray_        8  avgt  10    29,781  ±   0,651  ns/op
indentRefactoredFillCharArray_       16  avgt  10    84,230  ±   1,238  ns/op
indentRefactoredFillCharArray_       31  avgt  10    90,114  ±   1,572  ns/op
indentRefactoredFillCharArray_       32  avgt  10   117,664  ±   3,740  ns/op
indentRefactoredFillCharArray_       64  avgt  10   139,515  ±   3,906  ns/op
indentRefactoredFillCharArray_      128  avgt  10   242,605  ±   2,125  ns/op
indentConcurrentHashMapCache          0  avgt  10    43,567  ±   2,075  ns/op
indentConcurrentHashMapCache          2  avgt  10    47,920  ±   1,098  ns/op
indentConcurrentHashMapCache          8  avgt  10    49,064  ±   2,745  ns/op
indentConcurrentHashMapCache         16  avgt  10    70,904  ±   5,373  ns/op
indentConcurrentHashMapCache         31  avgt  10    70,638  ±   1,675  ns/op
indentConcurrentHashMapCache         32  avgt  10    96,865  ±   2,568  ns/op
indentConcurrentHashMapCache         64  avgt  10   111,348  ±   2,058  ns/op
indentConcurrentHashMapCache        128  avgt  10   173,846  ±   2,002  ns/op

 */


@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(2)
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
                .forks(2)
                // #########
                // COMPILER
                // #########
                // make sure we dont see compiling of our benchmark code during
                // measurement.
                // if you see compiling => more warmup
                //.jvmArgsAppend("-XX:+UnlockDiagnosticVMOptions")
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
                // .jvmArgsAppend("-XX:+PerserveFramePointer")
                .build();
        new Runner(opt).run();

    }

}