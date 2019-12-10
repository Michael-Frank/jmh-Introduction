package de.frank.jmh.algorithms;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormat;
import org.openjdk.jmh.results.format.ResultFormatFactory;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/*--

# MacOS VM version: JDK 13.0.1, OpenJDK 64-Bit Server VM, 13.0.1+9

Single-Shot Times us/op  (== cold jvm == no compiler optimization) - lower is better
                   fib(n):    30       92      1.000       10.000    100.000    1.000.000
RECURSIVE                16.524
RECURSIVE63               1.994
BINET63                       3        10
BINET                       106       170      3.612       37.254
DYNAMIC                      14        13        125        2.142
MATRIX                       22        28         58          701      8.261
DOUBLING                     16        17         26          134      2.520       27.000
DOUBLINGHYBRID               49        51         52          169      2.567       26.519
DOUBLINGRECURSIVE            17        28        151         1393      7.136       60.842
DOUBLINGRECURSIVEHYBRID      47        49         64          161      2.576       24.415


HOT-Code us/op - lower is better
                   fib(n):    30       92      1.000       10.000    100.000    1.000.000
RECURSIVE              7.318,674
RECURSIVE63            1.970,760
BINET63                    0,049    0,048
BINET                      3,408   12,477    855,779   30.853,861
DYNAMIC                    0,291    1,069     25,563    1.177,631
MATRIX                     1,261    1,754      4,530      112,789  4.904,023
DOUBLING                   0,301    0,472      1,219       14,157    659,424   24.078,775
DOUBLINGHYBRID             0,012    0,016      0,021       12,487    699,252   22.682,791
DOUBLINGRECURSIVE          1,158    3,266     34,706       382,86  4.350,171   57.118,853
DOUBLINGRECURSIVEHYBRID    0,003    0,003      0,003       11,381    541,571   21.851,834


RAW VALUES
=================
SINGLE-SHOT (== cold jvm == no compiler optimization)
Benchmark                                  (test)  Mode  Cnt      Score      Error  Units
FibonacciJMH.run                     RECURSIVE_30    ss  200  16524,505 ± 3100,713  us/op #already sucks at fib(30)
FibonacciJMH.run                   RECURSIVE63_30    ss  200   1994,264 ±   47,288  us/op #already sucks at fib(30)
FibonacciJMH.run                       DYNAMIC_30    ss  200     14,684 ±   25,563  us/op
FibonacciJMH.run                       DYNAMIC_92    ss  200     13,638 ±    2,327  us/op
FibonacciJMH.run                     DYNAMIC_1000    ss  200    125,715 ±  120,669  us/op
FibonacciJMH.run                    DYNAMIC_10000    ss  200   2142,813 ±  352,553  us/op
FibonacciJMH.run                        MATRIX_30    ss  200     22,099 ±    4,545  us/op
FibonacciJMH.run                        MATRIX_92    ss  200     28,860 ±    6,193  us/op
FibonacciJMH.run                      MATRIX_1000    ss  200     58,500 ±   24,248  us/op
FibonacciJMH.run                     MATRIX_10000    ss  200    701,467 ±  177,337  us/op
FibonacciJMH.run                    MATRIX_100000    ss  200   8261,439 ± 1621,868  us/op
FibonacciJMH.run                       BINET63_30    ss  200      3,761 ±    0,783  us/op
FibonacciJMH.run                       BINET63_92    ss  200     10,772 ±   23,128  us/op #fastest for values <=fib(92)
FibonacciJMH.run                         BINET_30    ss  200    106,518 ±   30,437  us/op
FibonacciJMH.run                         BINET_92    ss  200    170,384 ±   39,346  us/op
FibonacciJMH.run                       BINET_1000    ss  200   3612,149 ±  388,769  us/op
FibonacciJMH.run                      BINET_10000    ss  200  37254,568 ± 4184,686  us/op #big performance penalty from BigDecimal
FibonacciJMH.run                      DOUBLING_30    ss  200     16,898 ±    2,784  us/op
FibonacciJMH.run                      DOUBLING_92    ss  200     17,556 ±    3,450  us/op
FibonacciJMH.run                    DOUBLING_1000    ss  200     26,214 ±    5,246  us/op
FibonacciJMH.run                   DOUBLING_10000    ss  200    134,190 ±   30,648  us/op
FibonacciJMH.run                  DOUBLING_100000    ss  200   2520,146 ±  292,479  us/op
FibonacciJMH.run                 DOUBLING_1000000    ss  200  27000,213 ± 2981,988  us/op #WINNER
FibonacciJMH.run                DOUBLINGHYBRID_30    ss  200     49,509 ±  138,939  us/op #22us overhead of initializing the lookup table - lookup not worth in singeShot
FibonacciJMH.run                DOUBLINGHYBRID_92    ss  200     51,421 ±  142,913  us/op #22us overhead of initializing the lookup table - lookup not worth in singeShot
FibonacciJMH.run              DOUBLINGHYBRID_1000    ss  200     52,241 ±  144,660  us/op #22us overhead of initializing the lookup table - lookup not worth in singeShot
FibonacciJMH.run             DOUBLINGHYBRID_10000    ss  200    169,630 ±  162,110  us/op #22us overhead of initializing the lookup table - lookup not worth in singeShot
FibonacciJMH.run            DOUBLINGHYBRID_100000    ss  200   2567,754 ±  386,170  us/op #22us overhead of initializing the lookup table - lookup not worth in singeShot
FibonacciJMH.run           DOUBLINGHYBRID_1000000    ss  200  26519,118 ± 3067,384  us/op #22us overhead of initializing the lookup table - lookup not worth in singeShot
FibonacciJMH.run             DOUBLINGRECURSIVE_30    ss  200     17,521 ±    4,194  us/op #rescurive is worse then iterative
FibonacciJMH.run             DOUBLINGRECURSIVE_92    ss  200     28,250 ±    7,128  us/op #rescurive is worse then iterative
FibonacciJMH.run           DOUBLINGRECURSIVE_1000    ss  200    151,003 ±  119,869  us/op #rescurive is worse then iterative
FibonacciJMH.run          DOUBLINGRECURSIVE_10000    ss  200   1393,939 ±  160,679  us/op #rescurive is worse then iterative
FibonacciJMH.run         DOUBLINGRECURSIVE_100000    ss  200   7136,734 ± 1501,185  us/op #rescurive is worse then iterative
FibonacciJMH.run        DOUBLINGRECURSIVE_1000000    ss  200  60842,967 ± 3854,978  us/op #rescurive is worse then iterative
FibonacciJMH.run       DOUBLINGRECURSIVEHYBRID_30    ss  200     47,236 ±  135,909  us/op #22us overhead of initializing the lookup table - lookup not worth in singeShot
FibonacciJMH.run       DOUBLINGRECURSIVEHYBRID_92    ss  200     49,771 ±  162,101  us/op #22us overhead of initializing the lookup table - lookup not worth in singeShot
FibonacciJMH.run     DOUBLINGRECURSIVEHYBRID_1000    ss  200     64,854 ±  189,976  us/op #22us overhead of initializing the lookup table - lookup not worth in singeShot
FibonacciJMH.run    DOUBLINGRECURSIVEHYBRID_10000    ss  200    161,395 ±  156,253  us/op #22us overhead amortizes for larger values - iterative doubling is still the overall winner
FibonacciJMH.run   DOUBLINGRECURSIVEHYBRID_100000    ss  200   2576,350 ±  389,616  us/op #22us overhead amortizes for larger values - iterative doubling is still the overall winner
FibonacciJMH.run  DOUBLINGRECURSIVEHYBRID_1000000    ss  200  24415,162 ± 2802,508  us/op #22us overhead amortizes for larger values - iterative doubling is still the overall winner

HOT code (compiled) avg time:
Benchmark                                  (test)  Mode  Cnt      Score      Error  Units
FibonacciJMH.run                     RECURSIVE_30  avgt    5   7318,674 ± 2661,933  us/op #already sucks at fib(30)
FibonacciJMH.run                   RECURSIVE63_30  avgt    5   1970,760 ±  328,046  us/op #already sucks at fib(30)
FibonacciJMH.run                       DYNAMIC_30  avgt    5      0,291 ±    0,094  us/op
FibonacciJMH.run                       DYNAMIC_92  avgt    5      1,069 ±    0,276  us/op
FibonacciJMH.run                     DYNAMIC_1000  avgt    5     25,563 ±   10,671  us/op
FibonacciJMH.run                    DYNAMIC_10000  avgt    5   1177,631 ±   40,495  us/op
FibonacciJMH.run                        MATRIX_30  avgt    5      1,261 ±    0,054  us/op
FibonacciJMH.run                        MATRIX_92  avgt    5      1,754 ±    0,060  us/op
FibonacciJMH.run                      MATRIX_1000  avgt    5      4,530 ±    1,050  us/op
FibonacciJMH.run                     MATRIX_10000  avgt    5    112,789 ±    8,329  us/op
FibonacciJMH.run                    MATRIX_100000  avgt    5   4904,023 ±  450,171  us/op
FibonacciJMH.run                       BINET63_30  avgt    5      0,049 ±    0,011  us/op
FibonacciJMH.run                       BINET63_92  avgt    5      0,048 ±    0,003  us/op
FibonacciJMH.run                         BINET_30  avgt    5      3,408 ±    0,201  us/op
FibonacciJMH.run                         BINET_92  avgt    5     12,477 ±    2,339  us/op
FibonacciJMH.run                       BINET_1000  avgt    5    855,779 ±  271,974  us/op
FibonacciJMH.run                      BINET_10000  avgt    5  30853,861 ± 1149,083  us/op
FibonacciJMH.run                      DOUBLING_30  avgt    5      0,301 ±    0,025  us/op #iterative doubling has nice performance
FibonacciJMH.run                      DOUBLING_92  avgt    5      0,472 ±    0,042  us/op
FibonacciJMH.run                    DOUBLING_1000  avgt    5      1,219 ±    0,421  us/op
FibonacciJMH.run                   DOUBLING_10000  avgt    5     14,157 ±    1,602  us/op
FibonacciJMH.run                  DOUBLING_100000  avgt    5    659,424 ±   37,425  us/op
FibonacciJMH.run                 DOUBLING_1000000  avgt    5  24078,775 ± 3800,707  us/op
FibonacciJMH.run                DOUBLINGHYBRID_30  avgt    5      0,012 ±    0,001  us/op #hybrid shines for values <=fib(1023)
FibonacciJMH.run                DOUBLINGHYBRID_92  avgt    5      0,016 ±    0,009  us/op #  -||-
FibonacciJMH.run              DOUBLINGHYBRID_1000  avgt    5      0,021 ±    0,010  us/op #  -||-
FibonacciJMH.run             DOUBLINGHYBRID_10000  avgt    5     12,487 ±    1,628  us/op #  and still has better perf for bigger values
FibonacciJMH.run            DOUBLINGHYBRID_100000  avgt    5    699,252 ±  277,086  us/op #  -||-
FibonacciJMH.run           DOUBLINGHYBRID_1000000  avgt    5  22682,791 ± 1820,359  us/op #  -||-
FibonacciJMH.run             DOUBLINGRECURSIVE_30  avgt    5      1,158 ±    0,743  us/op #plain recursive Doubling is inferior to iterative Doubling
FibonacciJMH.run             DOUBLINGRECURSIVE_92  avgt    5      3,266 ±    1,587  us/op #  -||-
FibonacciJMH.run           DOUBLINGRECURSIVE_1000  avgt    5     34,706 ±    4,230  us/op #  -||-
FibonacciJMH.run          DOUBLINGRECURSIVE_10000  avgt    5    382,860 ±  197,535  us/op #  -||-
FibonacciJMH.run         DOUBLINGRECURSIVE_100000  avgt    5   4350,171 ±  613,642  us/op #  -||-
FibonacciJMH.run        DOUBLINGRECURSIVE_1000000  avgt    5  57118,853 ± 4105,049  us/op #  -||-
FibonacciJMH.run       DOUBLINGRECURSIVEHYBRID_30  avgt    5      0,003 ±    0,001  us/op # but recursive doubling + lookup has really good performance
FibonacciJMH.run       DOUBLINGRECURSIVEHYBRID_92  avgt    5      0,003 ±    0,001  us/op #  especially for values <=fib(1023)
FibonacciJMH.run     DOUBLINGRECURSIVEHYBRID_1000  avgt    5      0,003 ±    0,001  us/op #  especially for values <=fib(1023)
FibonacciJMH.run    DOUBLINGRECURSIVEHYBRID_10000  avgt    5     11,381 ±   10,064  us/op #  and in recursive case also for bigger values
FibonacciJMH.run   DOUBLINGRECURSIVEHYBRID_100000  avgt    5    541,571 ±   53,782  us/op #  and in recursive case also for bigger values
FibonacciJMH.run  DOUBLINGRECURSIVEHYBRID_1000000  avgt    5  21851,834 ± 3664,835  us/op #  and in recursive case also for bigger values
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class FibonacciJMH {
    @Param({
            "RECURSIVE_30",//no point in testing more - already awfully slow at fib(30)
            //"RECURSIVE_40"//to slow already:  885519,886 ± 17265,483  us/op

            "RECURSIVE63_30",//no point in testing more - already awfully slow at fib(30)

            "DYNAMIC_30",
            "DYNAMIC_92",//highest to fit into 2^63-1 (a java long)
            "DYNAMIC_1000",
            "DYNAMIC_10000",
            //"DYNAMIC_100000",//to slow already:  105616,622 ±  2611,484  us/op

            "MATRIX_30",
            "MATRIX_92",//highest to fit into 2^63-1 (a java long)
            "MATRIX_1000",
            "MATRIX_10000",
            "MATRIX_100000",

            //binet closed formula variant
            //long based variant
            "BINET63_30",
            "BINET63_92",//highest to fit into 2^63-1 (a java long)

            //big-int variant
            "BINET_30",
            "BINET_92",//highest to fit into 2^63-1 (a java long)
            "BINET_1000",
            "BINET_10000",
            //"BINET_100000", //to slow already:  779722,371 ±  4566,813  us/op #


            //iterative variant of "doubling" method
            "DOUBLING_30",
            "DOUBLING_92",//highest to fit into 2^63-1 (a java long)
            "DOUBLING_1000",
            "DOUBLING_10000",
            "DOUBLING_100000",
            "DOUBLING_1000000",

            "DOUBLINGHYBRID_30",
            "DOUBLINGHYBRID_92",//highest to fit into 2^63-1 (a java long)
            "DOUBLINGHYBRID_1000",
            "DOUBLINGHYBRID_10000",
            "DOUBLINGHYBRID_100000",
            "DOUBLINGHYBRID_1000000",


            //recursive variant of "doubling" method
            "DOUBLINGRECURSIVE_30",
            "DOUBLINGRECURSIVE_92", //highest to fit into 2^63-1 (a java long)
            "DOUBLINGRECURSIVE_1000",
            "DOUBLINGRECURSIVE_10000",
            "DOUBLINGRECURSIVE_100000",
            "DOUBLINGRECURSIVE_1000000",

            "DOUBLINGRECURSIVEHYBRID_30",
            "DOUBLINGRECURSIVEHYBRID_92", //highest to fit into 2^63-1 (a java long)
            "DOUBLINGRECURSIVEHYBRID_1000",
            "DOUBLINGRECURSIVEHYBRID_10000",
            "DOUBLINGRECURSIVEHYBRID_100000",
            "DOUBLINGRECURSIVEHYBRID_1000000",
    })
    public String test;

    private FibBig impl;
    private int size;


    @Setup(Level.Trial)
    public void setup() {
        String[] params = test.split("_");
        this.impl = FibBig.valueOf(params[0]);
        this.size = Integer.parseInt(params[1]);

    }

    public static void main(String[] args) throws RunnerException {
        generateBigIntLookup(1024);
        System.out.println("Max fib(n) to fit long: " + (FibBig.LazyFibHolder.FIB.length - 1));
        //generate JMH parameters for impls
        generateJMHParams(
                30, //max for naive RECURSIVE - already awfully slow at fib(30)
                92,  //max n of fib(n) to fit into a java long of 2^63-1
                1000,
                10000,
                100000
        );

        //single shot
        Collection<RunResult> singleShotResult = new Runner(new OptionsBuilder()
                .include(FibonacciJMH.class.getName())
                .resultFormat(ResultFormatType.JSON)
                .result(String.format("%s_%s_singleShot.json",
                        DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                        FibonacciJMH.class.getSimpleName()))
                .forks(1)
                .mode(Mode.SingleShotTime)
                .warmupIterations(0)
                .measurementIterations(200)
                .param("test",
                        "RECURSIVE_30",//no point in testing more - already awfully slow at fib(30)
                        //"RECURSIVE_40"//to slow already:  885519,886 ± 17265,483  us/op

                        "RECURSIVE63_30",//no point in testing more - already awfully slow at fib(30)

                        "DYNAMIC_30",
                        "DYNAMIC_92",//highest to fit into 2^63-1 (a java long)
                        "DYNAMIC_1000",
                        "DYNAMIC_10000",
                        //"DYNAMIC_100000",//to slow already:  105616,622 ±  2611,484  us/op

                        "MATRIX_30",
                        "MATRIX_92",//highest to fit into 2^63-1 (a java long)
                        "MATRIX_1000",
                        "MATRIX_10000",
                        "MATRIX_100000",

                        //binet closed formula variant
                        //long based variant
                        "BINET63_30",
                        "BINET63_92",//highest to fit into 2^63-1 (a java long)

                        //big-int variant
                        "BINET_30",
                        "BINET_92",//highest to fit into 2^63-1 (a java long)
                        "BINET_1000",
                        "BINET_10000",
                        //"BINET_100000", //to slow already:  779722,371 ±  4566,813  us/op #


                        //iterative variant of "doubling" method
                        "DOUBLING_30",
                        "DOUBLING_92",//highest to fit into 2^63-1 (a java long)
                        "DOUBLING_1000",
                        "DOUBLING_10000",
                        "DOUBLING_100000",
                        "DOUBLING_1000000",


                        "DOUBLINGHYBRID_30",
                        "DOUBLINGHYBRID_92",//highest to fit into 2^63-1 (a java long)
                        "DOUBLINGHYBRID_1000",
                        "DOUBLINGHYBRID_10000",
                        "DOUBLINGHYBRID_100000",
                        "DOUBLINGHYBRID_1000000",


                        //recursive variant of "doubling" method
                        "DOUBLINGRECURSIVE_30",
                        "DOUBLINGRECURSIVE_92", //highest to fit into 2^63-1 (a java long)
                        "DOUBLINGRECURSIVE_1000",
                        "DOUBLINGRECURSIVE_10000",
                        "DOUBLINGRECURSIVE_100000",
                        "DOUBLINGRECURSIVE_1000000",

                        "DOUBLINGRECURSIVEHYBRID_30",
                        "DOUBLINGRECURSIVEHYBRID_92", //highest to fit into 2^63-1 (a java long)
                        "DOUBLINGRECURSIVEHYBRID_1000",
                        "DOUBLINGRECURSIVEHYBRID_10000",
                        "DOUBLINGRECURSIVEHYBRID_100000",
                        "DOUBLINGRECURSIVEHYBRID_1000000"
                )
                .build()).run();

        Collection<RunResult> hotResults = new Runner(new OptionsBuilder()
                .include(FibonacciJMH.class.getName())
                .resultFormat(ResultFormatType.JSON)
                .result(String.format("%s_%s_hot.json",
                        DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                        FibonacciJMH.class.getSimpleName()))
                .forks(1)
                .mode(Mode.AverageTime)
                .timeUnit(TimeUnit.MICROSECONDS)
                .warmupIterations(5)
                .warmupTime(TimeValue.seconds(1))
                .measurementTime(TimeValue.seconds(1))
                .measurementIterations(5)
                .build()).run();


        ResultFormat format = ResultFormatFactory.getInstance(ResultFormatType.TEXT, System.out);
        System.out.println("Single shot:");
        format.writeOut(singleShotResult);
        System.out.println("hot (avg time):");
        format.writeOut(hotResults);


    }

    private static void generateJMHParams(int... sizes) {
        System.out.println("@Param({");
        for (FibBig impl : FibBig.values()) {
            for (int size : sizes) {
                System.out.println("\"" + impl.name() + "_" + size + "\"");
            }
        }
        System.out.println("}");

    }

    private static void generateBigIntLookup(int max) {
        System.out.println("private static final BigInteger [] FIB_B = {");
        for (int n = 0; n <= max; n++) {
            BigInteger fib = FibBig.DOUBLING.calculate(n);
            if (n == 0) {
                System.out.print("BigInteger.ZERO");
            } else if (n == 1 || n == 2) {
                System.out.print("BigInteger.ONE");
            } else if (fib.bitLength() < 63) {
                System.out.print("BigInteger.valueOf(" + fib.longValue() + "L)");
            } else {
                System.out.print("new BigInteger(\"" + fib.toString(16) + "\",16)");
            }
            if (n <= max) {//print array delim, except for last
                System.out.print(',');
            }
            System.out.println("//fib(" + n + ")");
        }
        System.out.println("};");

    }


    @Benchmark
    public BigInteger run() {
        return impl.calculate(size);
    }


    // Algorithm implementations
    public enum FibBig {
        RECURSIVE {
            @Override
            public String toString() {
                return "Textbook recursive (extremely slow)";
            }

            @Override
            public BigInteger calculate(int n) {
                return naiveRecursiveBigInt(n);
            }

            private BigInteger naiveRecursiveBigInt(int n) {
                if (n == 0) {
                    return BigInteger.ZERO;
                } else if (n <= 2) {
                    return BigInteger.ONE;
                } else {
                    return naiveRecursiveBigInt(n - 1).add(naiveRecursiveBigInt(n - 2));
                }
            }
        },

        RECURSIVE63 {
            @Override
            public String toString() {
                return "Textbook recursive (extremely slow)";
            }

            @Override
            public BigInteger calculate(int n) {
                if (n > 92) {
                    throw new IllegalArgumentException("this implementation operates on 2^63 ints and thus only supports fib(0) to fib(92): " + n);
                }
                return BigInteger.valueOf(naiveRecursive(n));
            }

            private long naiveRecursive(int n) {
                if (n == 0) {
                    return 0L;
                } else if (n <= 2) {
                    return 1L;
                } else {
                    return naiveRecursive(n - 1) + naiveRecursive(n - 2);
                }
            }

        },

        DYNAMIC {
            @Override
            public String toString() {
                return "Dynamic programming";
            }

            @Override
            public BigInteger calculate(int n) {
                BigInteger a = BigInteger.ZERO;
                BigInteger b = BigInteger.ONE;
                for (int i = 0; i < n; i++) {
                    BigInteger c = a.add(b);
                    a = b;
                    b = c;
                }
                return a;
            }

        },

        MATRIX {
            @Override
            public String toString() {
                return "Matrix exponentiation";
            }

            @Override
            public BigInteger calculate(int n) {
                return fastFibonacciMatrix(n);
            }

            /*
             * Fast matrix method. Easy to describe, but has a constant factor slowdown compared to doubling method.
             * [1 1]^n   [F(n+1) F(n)  ]
             * [1 0]   = [F(n)   F(n-1)].
             */
            private BigInteger fastFibonacciMatrix(int n) {
                BigInteger[] matrix = {BigInteger.ONE, BigInteger.ONE, BigInteger.ONE, BigInteger.ZERO};
                return matrixPow(matrix, n)[1];
            }

            // Computes the power of a matrix. The matrix is packed in row-major order.
            private BigInteger[] matrixPow(BigInteger[] matrix, int n) {
                if (n < 0)
                    throw new IllegalArgumentException();
                BigInteger[] result = {BigInteger.ONE, BigInteger.ZERO, BigInteger.ZERO, BigInteger.ONE};
                while (n != 0) {  // Exponentiation by squaring
                    if (n % 2 != 0)
                        result = matrixMultiply(result, matrix);
                    n /= 2;
                    matrix = matrixMultiply(matrix, matrix);
                }
                return result;
            }

            // Multiplies two matrices.
            private BigInteger[] matrixMultiply(BigInteger[] x, BigInteger[] y) {
                return new BigInteger[]{
                        multiply(x[0], y[0]).add(multiply(x[1], y[2])),
                        multiply(x[0], y[1]).add(multiply(x[1], y[3])),
                        multiply(x[2], y[0]).add(multiply(x[3], y[2])),
                        multiply(x[2], y[1]).add(multiply(x[3], y[3]))
                };
            }

            private BigInteger multiply(BigInteger x, BigInteger y) {
                return x.multiply(y);
            }


        },

        DOUBLING {
            @Override
            public String toString() {
                return "Fast doubling - iterative";
            }

            @Override
            public BigInteger calculate(int n) {
                BigInteger a = BigInteger.ZERO;
                BigInteger b = BigInteger.ONE;
                for (int bit = Integer.highestOneBit(n); bit != 0; bit >>>= 1) {
                    // Double it
                    BigInteger d = a.multiply(b.shiftLeft(1).subtract(a));
                    BigInteger e = a.multiply(a).add(b.multiply(b));
                    a = d;
                    b = e;

                    // Advance by one conditionally
                    if ((n & bit) != 0) {
                        BigInteger c = a.add(b);
                        a = b;
                        b = c;
                    }
                }
                return a;
            }


        },


        DOUBLINGHYBRID {
            @Override
            public String toString() {
                return "Fast doubling - iterative with hybrid lookup";
            }

            @Override
            public BigInteger calculate(int n) {
                BigInteger a = BigInteger.ZERO;
                BigInteger b = BigInteger.ONE;
                int m = 0;
                for (int bit = Integer.highestOneBit(n); bit != 0; bit >>>= 1) {
                    // Loop invariant: a = F(m), b = F(m+1)
                    // assert a.equals(slowFibonacci(m));
                    // assert b.equals(slowFibonacci(m+1));

                    // Double it

                    m *= 2;
                    //fib(m) in cache?
                    if ((m + 1) < LazyFibHolder.FIB_B.length) {
                        a = LazyFibHolder.FIB_B[m];
                        b = LazyFibHolder.FIB_B[m + 1];
                    } else {//cache miss
                        BigInteger d = a.multiply(b.shiftLeft(1).subtract(a));
                        BigInteger e = a.multiply(a).add(b.multiply(b));
                        a = d;
                        b = e;
                    }
                    //assert a.equals(slowFibonacci(m));
                    //assert b.equals(slowFibonacci(m+1));

                    // Advance by one conditionally
                    if ((n & bit) != 0) {
                        m++;
                        //fib(m) in cache?
                        if ((m + 1) < LazyFibHolder.FIB_B.length) {
                            a = LazyFibHolder.FIB_B[m];
                            b = LazyFibHolder.FIB_B[m + 1];
                        } else {//cache miss
                            BigInteger c = a.add(b);
                            a = b;
                            b = c;
                        }
                        //assert a.equals(slowFibonacci(m));
                        //assert b.equals(slowFibonacci(m+1));
                    }
                }
                return a;
            }

        },
        DOUBLINGRECURSIVE {
            @Override
            public String toString() {
                return "Fast doubling recursive";
            }

            @Override
            public BigInteger calculate(int n) {
                return fastFibonacciDoubling_recursive(n);
            }


            private BigInteger fastFibonacciDoubling_recursive(int index) {
                if (index == 0) return BigInteger.ZERO;
                if (index == 1 || index == 2) return BigInteger.ONE;
                if (index % 2 == 0) {
                    int k = index / 2;
                    BigInteger fk = fastFibonacciDoubling_recursive(k);
                    BigInteger fk1 = fastFibonacciDoubling_recursive(k + 1);
                    return fk.multiply(fk1.shiftLeft(1).subtract(fk));
                } else {
                    int k = (index - 1) / 2;
                    BigInteger fk = fastFibonacciDoubling_recursive(k);
                    BigInteger fk1 = fastFibonacciDoubling_recursive(k + 1);
                    return fk.multiply(fk).add(fk1.multiply(fk1));
                }
            }
        },
        DOUBLINGRECURSIVEHYBRID {
            @Override
            public String toString() {
                return "recursive doubling with lookup table";
            }

            @Override
            public BigInteger calculate(int n) {
                return fastFibonacciDoubling_recursiveHybrid(n);
            }

            private BigInteger fastFibonacciDoubling_recursiveHybrid(int index) {
                if (index < LazyFibHolder.FIB_B.length) {
                    return LazyFibHolder.FIB_B[index];
                }

                if (index % 2 == 0) {
                    int k = index / 2;
                    BigInteger fk = fastFibonacciDoubling_recursiveHybrid(k);
                    BigInteger fk1 = fastFibonacciDoubling_recursiveHybrid(k + 1);
                    return fk.multiply(fk1.shiftLeft(1).subtract(fk));
                } else {
                    int k = (index - 1) / 2;
                    BigInteger fk = fastFibonacciDoubling_recursiveHybrid(k);
                    BigInteger fk1 = fastFibonacciDoubling_recursiveHybrid(k + 1);
                    return fk.multiply(fk).add(fk1.multiply(fk1));
                }

            }

        },
        BINET {

            /**
             * Another possible implementation using Binet's formula.
             * The code is shorter, but involves time consuming BigDecimal computations.
             * Using this method instead of the original forIndex will lead to situations where running in parallel takes longer than running sequentially.
             * https://en.wikipedia.org/wiki/Fibonacci_number#Binet's_formula
             */
            private final BigDecimal PHI = BigDecimal.valueOf((1 + Math.sqrt(5)) / 2);
            private final BigDecimal PSI = BigDecimal.valueOf((1 - Math.sqrt(5)) / 2);
            private final BigDecimal SQRT5 = BigDecimal.valueOf(Math.sqrt(5));

            @Override
            public BigInteger calculate(int n) {
                return PHI.pow(n).subtract(PSI.pow(n)).divide(SQRT5, RoundingMode.HALF_UP).toBigInteger();
            }

            @Override
            public String toString() {
                return "binet closed formula";
            }
        },
        BINET63 {

            /**
             * Another possible implementation using Binet's formula.
             * The code is shorter, but involves time consuming BigDecimal computations.
             * Using this method instead of the original forIndex will lead to situations where running in parallel takes longer than running sequentially.
             * https://en.wikipedia.org/wiki/Fibonacci_number#Binet's_formula
             */
            private final double SQRT5 = Math.sqrt(5);
            private final double PHI = 1 + Math.sqrt(5) / 2;
            private final double PSI = (1 - Math.sqrt(5)) / 2;

            @Override
            public BigInteger calculate(int n) {
                if (n > 92) {
                    throw new IllegalArgumentException("this implementation operates on 2^63 ints and thus only supports fib(0) to fib(92): " + n);
                }
                return BigInteger.valueOf(binet63(n));
            }

            public long binet63(long n) {
                return Math.round(((Math.pow(PHI, n) - Math.pow(PSI, n)) / SQRT5));
            }

            @Override
            public String toString() {
                return "binet closed formula";
            }
        };


        public abstract BigInteger calculate(int n);


        //Lazy holder to prevent performance penalty at class loading time for other algorithm implementations not requiring this pre-computed values
        private static class LazyFibHolder {
            public static final long[] FIB = new long[]{
                    0L, 1L, 1L, 2L, 3L, 5L, 8L, 13L, 21L, 34L, 55L, 89L, 144L, 233L, 377L, 610L, 987L, 1597L,
                    2584L, 4181L, 6765L, 10946L, 17711L, 28657L, 46368L, 75025L, 121393L, 196418L,
                    317811L, 514229L, 832040L, 1346269L, 2178309L, 3524578L, 5702887L, 9227465L,
                    14930352L, 24157817L, 39088169L, 63245986L, 102334155L, 165580141L, 267914296L,
                    433494437L, 701408733L, 1134903170L, 1836311903L, 2971215073L, 4807526976L,
                    7778742049L, 12586269025L, 20365011074L, 32951280099L, 53316291173L,
                    86267571272L, 139583862445L, 225851433717L, 365435296162L, 591286729879L,
                    956722026041L, 1548008755920L, 2504730781961L, 4052739537881L, 6557470319842L,
                    10610209857723L, 17167680177565L, 27777890035288L, 44945570212853L,
                    72723460248141L, 117669030460994L, 190392490709135L, 308061521170129L,
                    498454011879264L, 806515533049393L, 1304969544928657L, 2111485077978050L,
                    3416454622906707L, 5527939700884757L, 8944394323791464L, 14472334024676221L,
                    23416728348467685L, 37889062373143906L, 61305790721611591L, 99194853094755497L,
                    160500643816367088L, 259695496911122585L, 420196140727489673L,
                    679891637638612258L, 1100087778366101931L, 1779979416004714189L,
                    2880067194370816120L, 4660046610375530309L, 7540113804746346429L
            };

            public static final BigInteger[] FIB_B = {
                    BigInteger.ZERO,//fib(0)
                    BigInteger.ONE,//fib(1)
                    BigInteger.ONE,//fib(2)
                    BigInteger.valueOf(2L),//fib(3)
                    BigInteger.valueOf(3L),//fib(4)
                    BigInteger.valueOf(5L),//fib(5)
                    BigInteger.valueOf(8L),//fib(6)
                    BigInteger.valueOf(13L),//fib(7)
                    BigInteger.valueOf(21L),//fib(8)
                    BigInteger.valueOf(34L),//fib(9)
                    BigInteger.valueOf(55L),//fib(10)
                    BigInteger.valueOf(89L),//fib(11)
                    BigInteger.valueOf(144L),//fib(12)
                    BigInteger.valueOf(233L),//fib(13)
                    BigInteger.valueOf(377L),//fib(14)
                    BigInteger.valueOf(610L),//fib(15)
                    BigInteger.valueOf(987L),//fib(16)
                    BigInteger.valueOf(1597L),//fib(17)
                    BigInteger.valueOf(2584L),//fib(18)
                    BigInteger.valueOf(4181L),//fib(19)
                    BigInteger.valueOf(6765L),//fib(20)
                    BigInteger.valueOf(10946L),//fib(21)
                    BigInteger.valueOf(17711L),//fib(22)
                    BigInteger.valueOf(28657L),//fib(23)
                    BigInteger.valueOf(46368L),//fib(24)
                    BigInteger.valueOf(75025L),//fib(25)
                    BigInteger.valueOf(121393L),//fib(26)
                    BigInteger.valueOf(196418L),//fib(27)
                    BigInteger.valueOf(317811L),//fib(28)
                    BigInteger.valueOf(514229L),//fib(29)
                    BigInteger.valueOf(832040L),//fib(30)
                    BigInteger.valueOf(1346269L),//fib(31)
                    BigInteger.valueOf(2178309L),//fib(32)
                    BigInteger.valueOf(3524578L),//fib(33)
                    BigInteger.valueOf(5702887L),//fib(34)
                    BigInteger.valueOf(9227465L),//fib(35)
                    BigInteger.valueOf(14930352L),//fib(36)
                    BigInteger.valueOf(24157817L),//fib(37)
                    BigInteger.valueOf(39088169L),//fib(38)
                    BigInteger.valueOf(63245986L),//fib(39)
                    BigInteger.valueOf(102334155L),//fib(40)
                    BigInteger.valueOf(165580141L),//fib(41)
                    BigInteger.valueOf(267914296L),//fib(42)
                    BigInteger.valueOf(433494437L),//fib(43)
                    BigInteger.valueOf(701408733L),//fib(44)
                    BigInteger.valueOf(1134903170L),//fib(45)
                    BigInteger.valueOf(1836311903L),//fib(46)
                    BigInteger.valueOf(2971215073L),//fib(47)
                    BigInteger.valueOf(4807526976L),//fib(48)
                    BigInteger.valueOf(7778742049L),//fib(49)
                    BigInteger.valueOf(12586269025L),//fib(50)
                    BigInteger.valueOf(20365011074L),//fib(51)
                    BigInteger.valueOf(32951280099L),//fib(52)
                    BigInteger.valueOf(53316291173L),//fib(53)
                    BigInteger.valueOf(86267571272L),//fib(54)
                    BigInteger.valueOf(139583862445L),//fib(55)
                    BigInteger.valueOf(225851433717L),//fib(56)
                    BigInteger.valueOf(365435296162L),//fib(57)
                    BigInteger.valueOf(591286729879L),//fib(58)
                    BigInteger.valueOf(956722026041L),//fib(59)
                    BigInteger.valueOf(1548008755920L),//fib(60)
                    BigInteger.valueOf(2504730781961L),//fib(61)
                    BigInteger.valueOf(4052739537881L),//fib(62)
                    BigInteger.valueOf(6557470319842L),//fib(63)
                    BigInteger.valueOf(10610209857723L),//fib(64)
                    BigInteger.valueOf(17167680177565L),//fib(65)
                    BigInteger.valueOf(27777890035288L),//fib(66)
                    BigInteger.valueOf(44945570212853L),//fib(67)
                    BigInteger.valueOf(72723460248141L),//fib(68)
                    BigInteger.valueOf(117669030460994L),//fib(69)
                    BigInteger.valueOf(190392490709135L),//fib(70)
                    BigInteger.valueOf(308061521170129L),//fib(71)
                    BigInteger.valueOf(498454011879264L),//fib(72)
                    BigInteger.valueOf(806515533049393L),//fib(73)
                    BigInteger.valueOf(1304969544928657L),//fib(74)
                    BigInteger.valueOf(2111485077978050L),//fib(75)
                    BigInteger.valueOf(3416454622906707L),//fib(76)
                    BigInteger.valueOf(5527939700884757L),//fib(77)
                    BigInteger.valueOf(8944394323791464L),//fib(78)
                    BigInteger.valueOf(14472334024676221L),//fib(79)
                    BigInteger.valueOf(23416728348467685L),//fib(80)
                    BigInteger.valueOf(37889062373143906L),//fib(81)
                    BigInteger.valueOf(61305790721611591L),//fib(82)
                    BigInteger.valueOf(99194853094755497L),//fib(83)
                    BigInteger.valueOf(160500643816367088L),//fib(84)
                    BigInteger.valueOf(259695496911122585L),//fib(85)
                    BigInteger.valueOf(420196140727489673L),//fib(86)
                    BigInteger.valueOf(679891637638612258L),//fib(87)
                    BigInteger.valueOf(1100087778366101931L),//fib(88)
                    BigInteger.valueOf(1779979416004714189L),//fib(89)
                    BigInteger.valueOf(2880067194370816120L),//fib(90)
                    new BigInteger("40abcfb3c0325745", 16),//fib(91)
                    new BigInteger("68a3dd8e61eccfbd", 16),//fib(92)
                    new BigInteger("a94fad42221f2702", 16),//fib(93)
                    new BigInteger("111f38ad0840bf6bf", 16),//fib(94)
                    new BigInteger("1bb433812a62b1dc1", 16),//fib(95)
                    new BigInteger("2cd36c2e32a371480", 16),//fib(96)
                    new BigInteger("48879faf5d0623241", 16),//fib(97)
                    new BigInteger("755b0bdd8fa9946c1", 16),//fib(98)
                    new BigInteger("bde2ab8cecafb7902", 16),//fib(99)
                    new BigInteger("1333db76a7c594bfc3", 16),//fib(100)
                    new BigInteger("1f12062f76909038c5", 16),//fib(101)
                    new BigInteger("3245e1a61e5624f888", 16),//fib(102)
                    new BigInteger("5157e7d594e6b5314d", 16),//fib(103)
                    new BigInteger("839dc97bb33cda29d5", 16),//fib(104)
                    new BigInteger("d4f5b15148238f5b22", 16),//fib(105)
                    new BigInteger("158937accfb606984f7", 16),//fib(106)
                    new BigInteger("22d892c1e4383f8e019", 16),//fib(107)
                    new BigInteger("3861ca6eb3ee4626510", 16),//fib(108)
                    new BigInteger("5b3a5d30982685b4529", 16),//fib(109)
                    new BigInteger("939c279f4c14cbdaa39", 16),//fib(110)
                    new BigInteger("eed684cfe43b518ef62", 16),//fib(111)
                    new BigInteger("18272ac6f30501d6999b", 16),//fib(112)
                    new BigInteger("27149313f148b6ef88fd", 16),//fib(113)
                    new BigInteger("3f3bbddae44db8c62298", 16),//fib(114)
                    new BigInteger("665050eed5966fb5ab95", 16),//fib(115)
                    new BigInteger("a58c0ec9b9e4287bce2d", 16),//fib(116)
                    new BigInteger("10bdc5fb88f7a983179c2", 16),//fib(117)
                    new BigInteger("1b1686e82495ec0ad47ef", 16),//fib(118)
                    new BigInteger("2bd44ce3ad8d958dec1b1", 16),//fib(119)
                    new BigInteger("46ead3cbd2238198c09a0", 16),//fib(120)
                    new BigInteger("72bf20af7fb11726acb51", 16),//fib(121)
                    new BigInteger("b9a9f47b51d498bf6d4f1", 16),//fib(122)
                    new BigInteger("12c69152ad185afe61a042", 16),//fib(123)
                    new BigInteger("1e61309a6235a48a587533", 16),//fib(124)
                    new BigInteger("3127c1ed0f4dff88ba1575", 16),//fib(125)
                    new BigInteger("4f88f2877183a413128aa8", 16),//fib(126)
                    new BigInteger("80b0b47480d1a39bcca01d", 16),//fib(127)
                    new BigInteger("d039a6fbf25547aedf2ac5", 16),//fib(128)
                    new BigInteger("150ea5b707326eb4aabcae2", 16),//fib(129)
                    new BigInteger("22124026c657c32f98af5a7", 16),//fib(130)
                    new BigInteger("3720e5ddcd8a31e4436c089", 16),//fib(131)
                    new BigInteger("5933260493e1f513dc1b630", 16),//fib(132)
                    new BigInteger("90540be2616c26f81f876b9", 16),//fib(133)
                    new BigInteger("e98731e6f54e1c0bfba2ce9", 16),//fib(134)
                    new BigInteger("179db3dc956ba43041b2a3a2", 16),//fib(135)
                    new BigInteger("263626fb04c085f1016cd08b", 16),//fib(136)
                    new BigInteger("3dd3dad79a2c2a21431f742d", 16),//fib(137)
                    new BigInteger("640a01d29eecb012448c44b8", 16),//fib(138)
                    new BigInteger("a1dddcaa3918da3387abb8e5", 16),//fib(139)
                    new BigInteger("105e7de7cd8058a45cc37fd9d", 16),//fib(140)
                    new BigInteger("1a7c5bb27111e647953e3b682", 16),//fib(141)
                    new BigInteger("2adad99a3e923eebf201bb41f", 16),//fib(142)
                    new BigInteger("4557354cafa42533873ff6aa1", 16),//fib(143)
                    new BigInteger("70320ee6ee36641f7941b1ec0", 16),//fib(144)
                    new BigInteger("b58944339dda89530081a8961", 16),//fib(145)
                    new BigInteger("125bb531a8c10ed7279c35a821", 16),//fib(146)
                    new BigInteger("1db44974e29eb76c57a4503182", 16),//fib(147)
                    new BigInteger("300ffea68b5fc6437f4085d9a3", 16),//fib(148)
                    new BigInteger("4dc4481b6dfe7dafd6e4d60b25", 16),//fib(149)
                    new BigInteger("7dd446c1f95e43f356255be4c8", 16),//fib(150)
                    new BigInteger("cb988edd675cc1a32d0a31efed", 16),//fib(151)
                    new BigInteger("1496cd59f60bb0596832f8dd4b5", 16),//fib(152)
                    new BigInteger("21505647cc817c739b039bfc4a2", 16),//fib(153)
                    new BigInteger("35e723a1c28d2ccd033694d9957", 16),//fib(154)
                    new BigInteger("573779e98f0ea9409e3a30d5df9", 16),//fib(155)
                    new BigInteger("8d1e9d8b519bd60da170c5af750", 16),//fib(156)
                    new BigInteger("e4561774e0aa7f4e3faaf685549", 16),//fib(157)
                    new BigInteger("17174b5003246555be11bbc34c99", 16),//fib(158)
                    new BigInteger("255cacc7512f0d4aa20c6b2ba1e2", 16),//fib(159)
                    new BigInteger("3c73f817545372a0601e26eeee7b", 16),//fib(160)
                    new BigInteger("61d0a4dea5827feb022a921a905d", 16),//fib(161)
                    new BigInteger("9e449cf5f9d5f28b6248b9097ed8", 16),//fib(162)
                    new BigInteger("1001541d49f58727664734b240f35", 16),//fib(163)
                    new BigInteger("19e59deca992e6501c6bc042d8e0d", 16),//fib(164)
                    new BigInteger("29e6f209f3886d7782b2f4f519d42", 16),//fib(165)
                    new BigInteger("43cc8ff69d1b53c79f1eb537f2b4f", 16),//fib(166)
                    new BigInteger("6db3820090a3c13f21d1aa2d0c891", 16),//fib(167)
                    new BigInteger("b18011f72dbf1506c0f05f64ff3e0", 16),//fib(168)
                    new BigInteger("11f3393f7be62d645e2c209920bc71", 16),//fib(169)
                    new BigInteger("1d0b3a5eeec21eb4ca3b268f70b051", 16),//fib(170)
                    new BigInteger("2efe739e6aa84c1928674728916cc2", 16),//fib(171)
                    new BigInteger("4c09adfd596a6acdf2a26db8021d13", 16),//fib(172)
                    new BigInteger("7b08219bc412b6e71b09b4e09389d5", 16),//fib(173)
                    new BigInteger("c711cf991d7d21b50dac229895a6e8", 16),//fib(174)
                    new BigInteger("14219f134e18fd89c28b5d7792930bd", 16),//fib(175)
                    new BigInteger("2092bc0cdff0cfa513661fa11bed7a5", 16),//fib(176)
                    new BigInteger("34b45b202e09cd2ed5f17d18ae80862", 16),//fib(177)
                    new BigInteger("5547172d0dfa9cd3e9579cb9ca6e007", 16),//fib(178)
                    new BigInteger("89fb724d3c046a02bf4919d278ee869", 16),//fib(179)
                    new BigInteger("df42897a49ff06d6a8a0b68c435c870", 16),//fib(180)
                    new BigInteger("1693dfbc7860370d967e9d05ebc4b0d9", 16),//fib(181)
                    new BigInteger("248808541d00277b0108a86eaffa7949", 16),//fib(182)
                    new BigInteger("3b1be81095605e88978745749bbf2a22", 16),//fib(183)
                    new BigInteger("5fa3f064b2608603988fede34bb9a36b", 16),//fib(184)
                    new BigInteger("9abfd87547c0e48c30173357e778cd8d", 16),//fib(185)
                    new BigInteger("fa63c8d9fa216a8fc8a7213b333270f8", 16),//fib(186)
                    new BigInteger("19523a14f41e24f1bf8be54931aab3e85", 16),//fib(187)
                    new BigInteger("28f876a293c03b9abc16575ce4dddaf7d", 16),//fib(188)
                    new BigInteger("424ab0b787de608c7ba23ca616888ee02", 16),//fib(189)
                    new BigInteger("6b43275a1b9e9c2737b89402fb6669d7f", 16),//fib(190)
                    new BigInteger("ad8dd811a37cfcb3b35ad0a911eef8b81", 16),//fib(191)
                    new BigInteger("118d0ff6bbf1b98daeb1364ac0d5562900", 16),//fib(192)
                    new BigInteger("1c65ed77d6298958e9e6e35551f445b481", 16),//fib(193)
                    new BigInteger("2df2fd6e921b42e6989819a012c99bdd81", 16),//fib(194)
                    new BigInteger("4a58eae66844cc3f827efcf564bde19202", 16),//fib(195)
                    new BigInteger("784be854fa600f261b17169577877d6f83", 16),//fib(196)
                    new BigInteger("c2a4d33b62a4db659d96138adc455f0185", 16),//fib(197)
                    new BigInteger("13af0bb905d04ea8bb8ad2a2053ccdc7108", 16),//fib(198)
                    new BigInteger("1fd958ecbbfa9c5f156433dab30123b728d", 16),//fib(199)
                    new BigInteger("338864a5c1caeb07d0ef067cb83df17e395", 16),//fib(200)
                    new BigInteger("5361bd927dc58766e6533a576b3f1535622", 16),//fib(201)
                    new BigInteger("86ea22383f90726eb74240d4237d06b39b7", 16),//fib(202)
                    new BigInteger("da4bdfcabd55f9d59d957b2b8ebc1be8fd9", 16),//fib(203)
                    new BigInteger("161360202fce66c4454d7bbffb239229c990", 16),//fib(204)
                    new BigInteger("23b81e1cdba3c6619f26d372b40f53e85969", 16),//fib(205)
                    new BigInteger("39cb7e3d0b722d25e4744f32af32e61222f9", 16),//fib(206)
                    new BigInteger("5d839c59e715f387839b22a5634239fa7c62", 16),//fib(207)
                    new BigInteger("974f1a96f28820ad680f71d81275200c9f5b", 16),//fib(208)
                    new BigInteger("f4d2b6f0d99e1434ebaa947d75b75a071bbd", 16),//fib(209)
                    new BigInteger("18c21d187cc2634e253ba0655882c7a13bb18", 16),//fib(210)
                    new BigInteger("280f48878a5c449173f649ad2fde3d41ad6d5", 16),//fib(211)
                    new BigInteger("40d165a0071ea7df9931ea12886104e2e91ed", 16),//fib(212)
                    new BigInteger("68e0ae27917aec710d2833bfb83f4224968c2", 16),//fib(213)
                    new BigInteger("a9b213c798999450a65a1dd240a047077faaf", 16),//fib(214)
                    new BigInteger("11292c1ef2a1480c1b3825191f8df892c16371", 16),//fib(215)
                    new BigInteger("1bc44d5b6c2ae151259dc6f64397fd03395e20", 16),//fib(216)
                    new BigInteger("2ced797a5ecc295d40d5ec0f6325f595fac191", 16),//fib(217)
                    new BigInteger("48b1c6d5caf70aae6673b305a6bdf299341fb1", 16),//fib(218)
                    new BigInteger("759f405029c3340ba7499f1509e3e82f2ee142", 16),//fib(219)
                    new BigInteger("be510725f4ba3eba0dbd521ab0a1dac86300f3", 16),//fib(220)
                    new BigInteger("133f047761e7d72c5b506f12fba85c2f791e235", 16),//fib(221)
                    new BigInteger("1f2414e9c1337b17fc2c4434a6b279dbff4e328", 16),//fib(222)
                    new BigInteger("32631961231b5244577cb347a25ad60b786c55d", 16),//fib(223)
                    new BigInteger("51872e4ae44ecd5c53a8f77c490d4fe777ba885", 16),//fib(224)
                    new BigInteger("83ea47ac076a1fa0ab25aac3eb6825f2f026de2", 16),//fib(225)
                    new BigInteger("d57175f6ebb8ecfcfecea240347575da67e1667", 16),//fib(226)
                    new BigInteger("1595bbda2f3230c9da9f44d041fdd9bcd5808449", 16),//fib(227)
                    new BigInteger("22ecd3399dedbf99aa8c2ef44545311a7bfe9ab0", 16),//fib(228)
                    new BigInteger("38828f13cd1ff063852b73c487430ad7517f1ef9", 16),//fib(229)
                    new BigInteger("5b6f624d6b0daffd2fb7a2b8cc883bf1cd7db9a9", 16),//fib(230)
                    new BigInteger("93f1f161382da060b4e3167d53cb46c91efcd8a2", 16),//fib(231)
                    new BigInteger("ef6153aea33b505de49ab936205382baec7a924b", 16),//fib(232)
                    new BigInteger("18353450fdb68f0be997dcfb3741ec9840b776aed", 16),//fib(233)
                    new BigInteger("272b498be7ea4411c7e1888e994724c3ef7f1fd38", 16),//fib(234)
                    new BigInteger("3f607ddce5a0d31db1796589d089115c303696825", 16),//fib(235)
                    new BigInteger("668bc768cd8b172f795aee1869d036201fb5b655d", 16),//fib(236)
                    new BigInteger("a5ec4545b32bea4d2ad453a23a59477c4fec4cd82", 16),//fib(237)
                    new BigInteger("10c780cae80b7017ca42f41baa4297d9c6fa2032df", 16),//fib(238)
                    new BigInteger("1b26451f433e2ebc9cf03955cde82c518bf8e50061", 16),//fib(239)
                    new BigInteger("2bedc5ea2b499ed467332d71782ac42b52f3053340", 16),//fib(240)
                    new BigInteger("47140b096e87cd91042366c74612f07cdeebea33a1", 16),//fib(241)
                    new BigInteger("7301d0f399d16c656b569438be3db4a831deef66e1", 16),//fib(242)
                    new BigInteger("ba15dbfd085939f66f79fb000450a52510cad99a82", 16),//fib(243)
                    new BigInteger("12d17acf0a22aa65bdad08f38c28e59cd42a9c90163", 16),//fib(244)
                    new BigInteger("1e72d88edaa83e0524a4a8a38c6defef25374a29be5", 16),//fib(245)
                    new BigInteger("3144535de4cae86ae251b1971896d58bf961e6b9d48", 16),//fib(246)
                    new BigInteger("4fb72becbf73267006f65a3aa504c57b1e9930e392d", 16),//fib(247)
                    new BigInteger("80fb7f4aa43e0edae9480bd1bd9b9b0717fb179d675", 16),//fib(248)
                    new BigInteger("d0b2ab3763b1354af03e660c62a0608236944880fa2", 16),//fib(249)
                    new BigInteger("151ae2a8207ef4425d98671de203bfb894e8f601e617", 16),//fib(250)
                    new BigInteger("22260d5b96ba07970c9c4d7ea82dc5c0b8523a89f5b9", 16),//fib(251)
                    new BigInteger("3740f003b738fbd96a34b49c8a3185794d3b308bdbd0", 16),//fib(252)
                    new BigInteger("5966fd5f4df3037076d1021b325f4b3a058d6b15d189", 16),//fib(253)
                    new BigInteger("90a7ed63052bff49e105b6b7bc90d0b352c89ba1ad59", 16),//fib(254)
                    new BigInteger("ea0eeac2531f02ba57d6b8d2eef01bed585606b77ee2", 16),//fib(255)
                    new BigInteger("17ab6d825584b020438dc6f8aab80eca0ab1ea2592c3b", 16),//fib(256)
                    new BigInteger("264c5c2e7ab6a04be90b3285d9a71088e0374a910ab1d", 16),//fib(257)
                    new BigInteger("3df7c9b0d03b506c2c98f97e845f1f52eae934b69d758", 16),//fib(258)
                    new BigInteger("644425df4af1f0b815a42c045e062fdbcb207f47a8275", 16),//fib(259)
                    new BigInteger("a23bef901b2d4124423d2582e2654f2eb609b3fe459cd", 16),//fib(260)
                    new BigInteger("10680156f661f31dc57e15187406b7f0a812a3345edc42", 16),//fib(261)
                    new BigInteger("1a8bc04ff814c73009a1e770a22d0ce393733e7443360f", 16),//fib(262)
                    new BigInteger("2af3c1a6ee76ba4dcf1ffc891633c4d43b85e1a8a21251", 16),//fib(263)
                    new BigInteger("457f81f6e68b817dd8c1e3f9b860d1b7cef9201ce54860", 16),//fib(264)
                    new BigInteger("7073439dd5023bcba7e1e082ce94968c0a7f01c5875ab1", 16),//fib(265)
                    new BigInteger("b5f2c594bb8dbd4980a3c47c86f56843d97821e26ca311", 16),//fib(266)
                    new BigInteger("126660932908ff9152885a4ff5589fecfe3f723a7f3fdc2", 16),//fib(267)
                    new BigInteger("1dc58cec74c1db65ea929697bdc7f6713bd6f458a60a0d3", 16),//fib(268)
                    new BigInteger("302bed7f9dcadaf73d1af0e7b320965e3a1666932549e95", 16),//fib(269)
                    new BigInteger("4df17a6c128cb65d27ad877f70e88ccf75ed5aebcb53f68", 16),//fib(270)
                    new BigInteger("7e1d67ebb057915464c878672409232db003c17ef09ddfd", 16),//fib(271)
                    new BigInteger("cc0ee257c2e447b18c75ffe694f1affd25f11c6abbf1d65", 16),//fib(272)
                    new BigInteger("14a2c4a43733bd905f13e784db8fad32ad5f4dde9ac8fb62", 16),//fib(273)
                    new BigInteger("2163b2c9b362020b77db478344dec8327fbe5fa5468818c7", 16),//fib(274)
                    new BigInteger("3606776dea95bf9bd6ef2f08206e75652d1dad83e1511429", 16),//fib(275)
                    new BigInteger("576a2a379df7c1a74eca768b654d3d97acdc0d2927d92cf0", 16),//fib(276)
                    new BigInteger("8d70a1a5888d814325b9a59385bbb2fcd9f9baad092a4119", 16),//fib(277)
                    new BigInteger("e4dacbdd268542ea74841c1eeb08f09486d5c7d631036e09", 16),//fib(278)
                    new BigInteger("1724b6d82af12c42d9a3dc1b270c4a39160cf82833a2daf22", 16),//fib(279)
                    new BigInteger("25726395fd59807180ec1ddd15bcd9425e7a54a596b311d2b", 16),//fib(280)
                    new BigInteger("3c971a6e284aacb45a8ff9f83cc9237b74874ccdca55ecc4d", 16),//fib(281)
                    new BigInteger("62097e0425a42d25db7c17d55285fcbdd301a1736108fe978", 16),//fib(282)
                    new BigInteger("9ea098724deed9da360c11cd8f4f20394788ee412b5eeb5c5", 16),//fib(283)
                    new BigInteger("100aa167673930700118829a2e1d51cf71a8a8fb48c67e9f3d", 16),//fib(284)
                    new BigInteger("19f4aaee8c181e0da47943b7071243d3062137df5b7c6d5502", 16),//fib(285)
                    new BigInteger("29ff4c55f3514e7da591c651352f95a277c9e0daa442ebf43f", 16),//fib(286)
                    new BigInteger("43f3f7447f696c8b4a0b0a083c41d9757deb18b9ffbf594941", 16),//fib(287)
                    new BigInteger("6df3439a72babb08ef9cd05971716f17f5b4f994a402453d80", 16),//fib(288)
                    new BigInteger("b1e73adef224279439a7da61adb3488d73a0124ea3c19e86c1", 16),//fib(289)
                    new BigInteger("11fda7e7964dee29d2944aabb1f24b7a569550be347c3e3c441", 16),//fib(290)
                    new BigInteger("1d1c1b95857030a3162ec851cccd80032dcf51e31eb85824b02", 16),//fib(291)
                    new BigInteger("2f19c37d1bbe1ecce8c312fd7ebfcb7d8464a2a153349660f43", 16),//fib(292)
                    new BigInteger("4c35df12a12e4f6ffef1db4f4b8d4b80b233f48471ecee85a45", 16),//fib(293)
                    new BigInteger("7b4fa28fbcec6e3ce7b4ee4cca4d16fe36989725c52184e6988", 16),//fib(294)
                    new BigInteger("c78581a25e1abdace6a6c99c15da627ee8cc8baa370e736c3cd", 16),//fib(295)
                    new BigInteger("142d524321b072be9ce5bb7e8e027797d1f6522cffc2ff852d55", 16),//fib(296)
                    new BigInteger("20a5aa5d47921e996b5028184f601dbfc0831ae7a333e6bbf122", 16),//fib(297)
                    new BigInteger("34d2fca0694291580835e396dd62955792796d14a2f6e6411e77", 16),//fib(298)
                    new BigInteger("5578a6fdb0d4aff173860baf2cc2b31752fc87fc462accfd0f99", 16),//fib(299)
                    new BigInteger("8a4ba39e1a1741497bbbef460a25486ee575f510e921b33e2e10", 16),//fib(300)
                    new BigInteger("dfc44a9bcaebf13aef41faf536e7fb8638727d0d2f4c803b3da9", 16),//fib(301)
                    new BigInteger("16a0fee39e50332846afdea3b410d43f51de8721e186e33796bb9", 16),//fib(302)
                    new BigInteger("249d438d5afef23bf5a3fe53077f53f7b565aef2b47bab3b4a962", 16),//fib(303)
                    new BigInteger("3b3e4270f94f25643c53dcf6bb9028370744361496028e72e151b", 16),//fib(304)
                    new BigInteger("5fdb85fe544e17a031f7db49c30f7c2ebca9e5074a7e39ae2be7d", 16),//fib(305)
                    new BigInteger("9b19c86f4d9d3d046e4bb8407e9fa465c3ee1b1be080c8210d398", 16),//fib(306)
                    new BigInteger("faf54e6da1eb54a4a043938a41af2094809800232aff01cf39215", 16),//fib(307)
                    new BigInteger("1960f16dcef8891a90e8f4bcac04ec4fa44861b3f0b7fc9f0465ad", 16),//fib(308)
                    new BigInteger("29104654a9173e64daed2df5501fde58ec51e1b62367ecbbf7f7c2", 16),//fib(309)
                    new BigInteger("427137c2780fc77f6bd622b1fc24caa8909a436a141fe95afc5d6f", 16),//fib(310)
                    new BigInteger("6b817e17212705e446c350a74c44a9017cec25203787d616f45531", 16),//fib(311)
                    new BigInteger("adf2b5d99936cd63b2997359486973aa0d86688a4ba7bf71f0b2a0", 16),//fib(312)
                    new BigInteger("1197433f0ba5dd347f95cc40094ae1cab8a728daa832f9588e507d1", 16),//fib(313)
                    new BigInteger("1c766e9ca5394a0ababf63759dd17905597f8f634ced754fad5ba71", 16),//fib(314)
                    new BigInteger("2e0db1dbb0df273f3a552fb5a71c5ad01226b83df5206ea83bac242", 16),//fib(315)
                    new BigInteger("4a84207856187149f514932b44edd3d56ba647a1420de3f7e907cb3", 16),//fib(316)
                    new BigInteger("7891d25406f798892f69c2e0ec0a2ea57dccffdf372e52a024b3ef5", 16),//fib(317)
                    new BigInteger("c315f2cc5d1009d3247e560c30f8027ae9734780793c36980dbbba8", 16),//fib(318)
                    new BigInteger("13ba7c5206407a25c53e818ed1d0231206740475fb06a8938326fa9d", 16),//fib(319)
                    new BigInteger("1febdb7ecc117ac2f78666ef94dfa339b50b38ee029a6bfd0402b645", 16),//fib(320)
                    new BigInteger("33a657d0d251f4e8bcc4e87e66afc64bbb7f3d63fda114908729b0e2", 16),//fib(321)
                    new BigInteger("5392334f9e636fabb44b4f6dfb8f6985708a7652003b808d8b2c6727", 16),//fib(322)
                    new BigInteger("87388b2070b56494711037ec623f2fd12c09b3b5fddc951e12561809", 16),//fib(323)
                    new BigInteger("dacabe700f18d440255b875a5dce99569c942a07fe1815ab9d827f30", 16),//fib(324)
                    new BigInteger("1620349907fce38d4966bbf46c00dc927c89dddbdfbf4aac9afd89739", 16),//fib(325)
                    new BigInteger("23cce08008ee70d14bbc746a11ddc627e653207c5fa0cc0754d5b1669", 16),//fib(326)
                    new BigInteger("39ed151910eb545e9523305e7ddea2ba62dcfe583f6016b3efd33ada2", 16),//fib(327)
                    new BigInteger("5db9f59919d9c52fe0dfa4c88fbc68e249301ed49f00e2bb44a8ec40b", 16),//fib(328)
                    new BigInteger("97a70ab22ac5198e7602d5270d9b0b9cac0d1d2cde60f96f347c271ad", 16),//fib(329)
                    new BigInteger("f561004b449edebe56e279ef9d57747ef53d3c017d61dc2a7925135b8", 16),//fib(330)
                    new BigInteger("18d080afd6f63f84ccce54f16aaf2801ba14a592e5bc2d599ada13a765", 16),//fib(331)
                    new BigInteger("282690b48b402d70b23c7c9064849f49a9687952fd924b1c426c64dd1d", 16),//fib(332)
                    new BigInteger("40f7116462366cf57f0ad181cf33c74b637d1ee5e34e7875dd46788482", 16),//fib(333)
                    new BigInteger("691da218ed769a6631474e1233b866950ce59838e0e0c3921fb2dd619f", 16),//fib(334)
                    new BigInteger("aa14b37d4fad075bb0521f9402ec2de07062b71ec42f3c07fcf955e621", 16),//fib(335)
                    new BigInteger("1133255963d23a1c1e1996da636a494757d484f57a50fff9a1cac3347c0", 16),//fib(336)
                    new BigInteger("1bd4709138cd0a91d91eb8d3a3990c255edab0676693f3ba219a5892de1", 16),//fib(337)
                    new BigInteger("2d0795ea9c9f44adf7384fae0703556cb6af355ce0e4f3b3c3651bc75a1", 16),//fib(338)
                    new BigInteger("48dc067bd56c4f3fd0570881aa9c61921589e5c44778e76de4ff745a382", 16),//fib(339)
                    new BigInteger("75e39c66720b93edc78f582fb19fb6fecc391b21285ddb21a8649021923", 16),//fib(340)
                    new BigInteger("bebfa2e24777e32d97e660b15c3c1890e1c300e56fd6c28f8d64047bca5", 16),//fib(341)
                    new BigInteger("134a33f48b983771b5f75b8e10ddbcf8fadfc1c0698349db135c8949d5c8", 16),//fib(342)
                    new BigInteger("1f362e22b00fb5a48f75c19926a17e8208fbf1cec080b6040c32c991926d", 16),//fib(343)
                    new BigInteger("328062173ba7ed16456d1d27377f3b7b03dbb38f2a03ffdf1f8f52db6835", 16),//fib(344)
                    new BigInteger("51b69039ebb7a2bad4e2dec05e20b9fd0cd7a55dea84b5e32bc21c6cfaa2", 16),//fib(345)
                    new BigInteger("8436f251275f8fd11a4ffbe7959ff57810b358ed1488b5c24b516f4862d7", 16),//fib(346)
                    new BigInteger("d5ed828b1317328bef32daa7f3c0af751d8afe4aff0d6ba577138bb55d79", 16),//fib(347)
                    new BigInteger("15a2474dc3a76c25d0982d68f8960a4ed2e3e573813962167c264fafdc050", 16),//fib(348)
                    new BigInteger("23011f7674d8df4e8f8b5b1377d2154624bc9558312a38d0d397886b31dc9", 16),//fib(349)
                    new BigInteger("38a366c438804b746023887c70681f94f7a07acbb2639ae74fbdd81b0de19", 16),//fib(350)
                    new BigInteger("5ba4863aad592ac2efaee38fe83a34db1c5d1023e38dd3b8235560863fbe2", 16),//fib(351)
                    new BigInteger("9447ecfee5d976374fd26c0c58a2547013fd8aef95f16e9f731338a14d9fb", 16),//fib(352)
                    new BigInteger("efec73399332a0fa3f814f9c40dc894b305a9b13797f4257966899278d5dd", 16),//fib(353)
                    new BigInteger("184346038790c17318f53bba8997eddbb445826030f70b0f7097bd1c8dafd8", 16),//fib(354)
                    new BigInteger("27420d3720c3eb82bced50b44da5b670674b2c11688eff34e9fe46af0685b5", 16),//fib(355)
                    new BigInteger("3f85533aa854acf5d5e28c6ed73da44c1b90ae7199860a445a9603cb94358d", 16),//fib(356)
                    new BigInteger("66c76071c918987892cfdd2324e35abc82dbda830215097944944a7a9abb42", 16),//fib(357)
                    new BigInteger("a64cb3ac716d456e68b26991fc20ff089e6c88f49b9b13bd9f2a4e462ef0cf", 16),//fib(358)
                    new BigInteger("10d14141e3a85dde6fb8246b5210459c5214863779db01d36e3be98c0c9ac11", 16),//fib(359)
                    new BigInteger("1b360c7caabf323556434b0471d2558cdbfb4ec6c394b30f482e8e706f89ce0", 16),//fib(360)
                    new BigInteger("2c074dbe8e679013c5fb6f6fc3e29b292e0fd4fe3d6fb4e2b66a77fc7c248f1", 16),//fib(361)
                    new BigInteger("473d5a3b3926c2491c3eba7435b4f0b60a0b23c5010467f1fe99066cebae5d1", 16),//fib(362)
                    new BigInteger("7344a7f9c78e525ce23a29e3f9978bdf381af8c33e741cd4b5037e6967d2ec2", 16),//fib(363)
                    new BigInteger("ba82023500b514a5fe78e4582f4c7c9542261c883f7884c6b39c84d65381493", 16),//fib(364)
                    new BigInteger("12dc6aa2ec8436702e0b30e3c28e408747a41154b7deca19b68a0033fbb54355", 16),//fib(365)
                    new BigInteger("1e848ac63c8f87ba8df2bf29458308509bc6731d3bd6526621c3c88160ed57e8", 16),//fib(366)
                    new BigInteger("3160f5692913be2abbfdf00d081148d7e36a8471f3b51c7fd84dc8b55ca29b3d", 16),//fib(367)
                    new BigInteger("4fe5802f65a345e549f0af364d9451287f30f78f2f8b6ee5fa119136bd8ff325", 16),//fib(368)
                    new BigInteger("814675988eb7041005ee9f4355a59a00629b7c0123408b65d25f59ec1a328e62", 16),//fib(369)
                    new BigInteger("d12bf5c7f45a49f54fdf4e79a339eb28e1cc739052cbfa4bcc70eb22d7c28187", 16),//fib(370)
                    new BigInteger("152726b6083114e0555cdedbcf8df85294467ef91760c85b19ed0450ef1f50fe9", 16),//fib(371)
                    new BigInteger("2239e6128776b97faa5ad3c369c19705226346321c8d87ffd6b413031c9b79170", 16),//fib(372)
                    new BigInteger("37610cc88fa7ce5fffb7b29f394f8f57b6a9c52b33ee505af0a117540bbaca159", 16),//fib(373)
                    new BigInteger("599af2db171e87dfaa128662a311265cd90d0b5d507bd85ac7552a572856432c9", 16),//fib(374)
                    new BigInteger("90fbffa3a6c6563fa9ca3901dc60b5b48fb6d088846a28b5b7f641ab34110d422", 16),//fib(375)
                    new BigInteger("ea96f27ebde4de1f53dcbf647f71dc1168c3dbe5d4e601107f4b6c025c67506eb", 16),//fib(376)
                    new BigInteger("17b92f22264ab345efda6f8665bd291c5f87aac6e595029c63741adad90785db0d", 16),//fib(377)
                    new BigInteger("26629e4a12290127e5183b7cadb446dd7613e88542e362ad6b68d19afecdfae1f8", 16),//fib(378)
                    new BigInteger("3e1bcd6c3873b46dd4f2ab0313716ff9d59b934c28786549cedcec75d7d580bd05", 16),//fib(379)
                    new BigInteger("647e6bb64a9cb595ba0ae67fc125b6d74baf7bd16b5bc7f73a45be10d6a37b9efd", 16),//fib(380)
                    new BigInteger("a29a392283106a038efd9182d49726d1214b0f1d93d42d410922aa86ae78fc5c02", 16),//fib(381)
                    new BigInteger("10718a4d8cdad1f994908780295bcdda86cfa8aeeff2ff53843686897851c77faff", 16),//fib(382)
                    new BigInteger("1a9b2ddfb50bd899cd80609856a5404798e459a0c930422794c8b131e3395745701", 16),//fib(383)
                    new BigInteger("2b0cb82d41e6aa936210e81880010e221fb4024fb923417b18ff37bb5b8b1ec5200", 16),//fib(384)
                    new BigInteger("45a7e60cf6f2832d2f9148b0d6a64e69b8985bf0825383a2adc7e8ed3ec4760a901", 16),//fib(385)
                    new BigInteger("70b49e3a38d92dc091a230c956a75c8bd84c5e403b76c51dc6c720a89a4f94cfb01", 16),//fib(386)
                    new BigInteger("b65c84472fcbb0edc133797a2d4daaf590e4ba30bdca48c0748f0995d9140ada402", 16),//fib(387)
                    new BigInteger("12711228168a4deae52d5aa4383f5078169311870f9410dde3b562a3e73639fa9f03", 16),//fib(388)
                    new BigInteger("1dd6da6c898708f9c140923bdb142b276fa15d2a1b70b569eafe533d44c77aa84305", 16),//fib(389)
                    new BigInteger("3047ec94a01156e4a66dece013537b9f86346eb12b04c647ceb3b5e12bfdb4a2e208", 16),//fib(390)
                    new BigInteger("4e1ec70129985fde67ae7f1bee67a6c6f5d5cbdb46757bb1b9b2091e70c52f4b250d", 16),//fib(391)
                    new BigInteger("7e66b395c9a9b6c30e1c6bfc01bb22667c0a3a8c717a41f98865beff9cc2e3ee0715", 16),//fib(392)
                    new BigInteger("cc857a96f34216a175caeb17f022c92d71e00667b7efbdab4217c81e0d8813392c22", 16),//fib(393)
                    new BigInteger("14aec2e2cbcebcd6483e75713f1ddeb93edea40f42969ffa4ca7d871daa4af7273337", 16),//fib(394)
                    new BigInteger("21771a8c3b02de405f9b2422be200b4c15fca475be159bd500c954f3bb7d30a605f59", 16),//fib(395)
                    new BigInteger("3625dd6f06d19b16a7d99993fd3dea0554db488500ac3bcf4d712d659621e01879290", 16),//fib(396)
                    new BigInteger("579cf7fb41d479570774bdb6bb5df5516ad7ecfabec1d7a44e3a8259519f10be7f1e9", 16),//fib(397)
                    new BigInteger("8dc2d56a48a6146daf4e574ab89bdf56bfb3357fbf6e13739babafbee7c0f0d6f8479", 16),//fib(398)
                    new BigInteger("e55fcd658a7a8dc4b6c3150173f9d4a82a8b227a7e2feb17e9e632183960019577662", 16),//fib(399)
                    new BigInteger("17322a2cfd320a23266116c4c2c95b3feea3e57fa3d9dfe8b8591e1d72120f26c6fadb", 16),//fib(400)
                    new BigInteger("2588270355d9b2ff71cd4814da08f88a714c97a74bbcde9a36f7813ef5a80f401e713d", 16),//fib(401)
                    new BigInteger("3cba5130530bbd22982e5ed99cd253ca5ff07d26ef96be82ef509f5c67ba1e66e56c18", 16),//fib(402)
                    new BigInteger("62427833a8e5702209fba6ee76db4c54d13d14ce3b539d1d2648209b5d622da703dd55", 16),//fib(403)
                    new BigInteger("9efcc963fbf12d44a22a05c813ada01f312d91f52aea5ba01598bff7c51c4c0de9496d", 16),//fib(404)
                    new BigInteger("1013f4197a4d69d66ac25acb68a88ec74026aa6c3663df8bd3be0e093227e79b4ed26c2", 16),//fib(405)
                    new BigInteger("1a03c0afba0c7caab4e4fb27e9e368c93339838b89128545d5179a08ae79ac5c2d6702f", 16),//fib(406)
                    new BigInteger("2a17b4c93459e6811fa755f3528bf79073602df7bf7664d1a8d5a811e0a193f77c396f1", 16),//fib(407)
                    new BigInteger("441b7578ee66632bd48c511b3c6f6059a699b1834888ea177ded421a8f1b4053a9a0720", 16),//fib(408)
                    new BigInteger("6e332a4222c049acf433a70e8efb57ea19f9df7b07ff4ee926c2ea2c6fbcd44b25d9e11", 16),//fib(409)
                    new BigInteger("b24e9fbb1126acd8c8bff829cb6ab843c09390fe50883900a4b02c46fed8149ecf7a531", 16),//fib(410)
                    new BigInteger("12081c9fd33e6f685bcf39f385a66102dda8d7079588787e9cb7316736e94e8e9f554342", 16),//fib(411)
                    new BigInteger("1d2d069b8450da35e85b3976225d0c8719b210177a90fc0ea702342ba6d6cfd88c4ce873", 16),//fib(412)
                    new BigInteger("2f35233b578f499e442a7369a8036d89f75ae71f1019748d43b96592ddc01e672ba22bb5", 16),//fib(413)
                    new BigInteger("4c6229d6dbe023d42c85acdfca607a11110cf7368aaa709beabb99be8496ee3fb7ef1428", 16),//fib(414)
                    new BigInteger("7b974d12336f6d7270b020497263e79b0867de559ac3e5292e74ff5162570ca6e3913fdd", 16),//fib(415)
                    new BigInteger("c7f976e90f4f91469d35cd293cc461ac1974d58c256e55c51930990fe6edfae69b805405", 16),//fib(416)
                    new BigInteger("14390c3fb42befeb90de5ed72af28494721dcb3e1c0323aee47a598614945078d7f1193e2", 16),//fib(417)
                    new BigInteger("20b8a3ae4520e8fffab1bba9bebecaaf33b51896de5a090b360d63171303302741a91e7e7", 16),//fib(418)
                    new BigInteger("34f1afedf94cd8eb8b901a80e9b14f43a5d2e3d4fa5d2cba1a87bc9d279780a0199a37bc9", 16),//fib(419)
                    new BigInteger("55aa539c3e6dc1eb8641d62aa87019f2d987fc6bd8b735c550951fb43a9ab0c75b43563b0", 16),//fib(420)
                    new BigInteger("8a9c038a37ba9ad711d1f0ab922169367f5ae040d314627f6b1cdc516232316774dd8df79", 16),//fib(421)
                    new BigInteger("e046572676285cc29813c6d63a91832958e2dcacabcb9844bbb1fc059ccce22ed020e4329", 16),//fib(422)
                    new BigInteger("16ae25ab0ade2f799a9e5b781ccb2ec5fd83dbced7edffac426ced856feff139644fe722a2", 16),//fib(423)
                    new BigInteger("24b28b1d7240b545c41f97e5807446f893120999a2aab9308e280d45c9bcbf5c5151f565cb", 16),//fib(424)
                    new BigInteger("3b60b0c87d1ee4bf5ebdf35d9d3f75be9095e5687a98b8dcd094facb39acb095b5a1dc886d", 16),//fib(425)
                    new BigInteger("60133be5ef5f9a0522dd8b431db3bcb723a7ef021d43720d5ebd081103696ff206f3d1ee38", 16),//fib(426)
                    new BigInteger("9b73ecae6c7e7ec4819b7ea0baf33275b43dd46a97dc2aea2f5202dc3d162087bc95ae76a5", 16),//fib(427)
                    new BigInteger("fb8728945bde18c9a47909e3d8a6ef2cd7e5c36cb51f9cf78e0f0aed407f9079c3898064dd", 16),//fib(428)
                    new BigInteger("196fb1542c85c978e26148884939a21a28c2397d74cfbc7e1bd610dc97d95b101801f2edb82", 16),//fib(429)
                    new BigInteger("292823dd7243ab057ca8d92686c4110cf64095b44021b64d94b7018b6be15417b43a8af405f", 16),//fib(430)
                    new BigInteger("4297d5319ec9747e5f0a21aecffdb3271f02cf31b4f172cbb08d126803baaf27cc3c7de1be1", 16),//fib(431)
                    new BigInteger("6bbff90f110d1f83dbb2fad556c1c434154364e5f5132919454413f36f9c033f807708d5c40", 16),//fib(432)
                    new BigInteger("ae57ce40afd694023abd1c8426bf775b34463417aa049be4f5d1265b7356b2674cb386b7821", 16),//fib(433)
                    new BigInteger("11a17c74fc0e3b386167017597d813b8f498998fd9f17c4fe3b153a4ee2f2b5a6cd2a8f8d461", 16),//fib(434)
                    new BigInteger("1c86f959070ba4788512d33dda440b2ea7dcfcd15491c60e330e660aa5649680e19de1644c82", 16),//fib(435)
                    new BigInteger("2e2875ce0319dfb0e679d4b3721c1ee79c7596612e83425e16bfb9af9393c1db4e708a5d20e3", 16),//fib(436)
                    new BigInteger("4aaf6f270a2584296b8ca7f14c602a16445293328315086c49ce1fba38f8585c300e6bc16d65", 16),//fib(437)
                    new BigInteger("78d7e4f50d3f63da52067ca4be7c48fde0c82993b1984aca608dd969cc8c1a377e7ef61e8e48", 16),//fib(438)
                    new BigInteger("c387541c1764e803bd9324960adc7314251abcc634ad5336aa5bf92405847293ae8d61dffbad", 16),//fib(439)
                    new BigInteger("13c5f391124a44bde0f99a13ac958bc1205e2e659e6459e010ae9d28dd2108ccb2d0c57fe89f5", 16),//fib(440)
                    new BigInteger("1ffe68d2d3c0933e1cd2cc5d0d4352f262afda3201af2f137b545cbb1d794ff5edb99b9de85a2", 16),//fib(441)
                    new BigInteger("33c45c63e60ad7fbfdcc6670b9d8deb3830e0897a01388f38c02f9e3fa9a58c2a08a611dd0f97", 16),//fib(442)
                    new BigInteger("53c2c536b9cb6b3a1a9f32cdc71c31a5e5bde2c9a1c2b8070757569f1813a8b88e43fcbbb9539", 16),//fib(443)
                    new BigInteger("8787219a9fd64336186b993e80f5105968cbeb6141d640fa935a508312ae017b2ece5dd98a4d0", 16),//fib(444)
                    new BigInteger("db49e6d159a1ae70330acc0c481141ff4e89ce2ae398f9019ab1a7222ac1aa33bd125a9543a09", 16),//fib(445)
                    new BigInteger("162d1086bf977f1a64b76654ac9065258b755b98c256f39fc2e0bf7a53d6fabaeebe0b86ecded9", 16),//fib(446)
                    new BigInteger("23e1aef3d5319a0167e8131571117945805df87b7090832fdc8bd9ec7683155e2a8f31304118e2", 16),//fib(447)
                    new BigInteger("3a0ebf7a94c9191bcc9f796a1da1de6b0bd3541432e776cf9f6c9966ca5a1019194d3cb72df7bb", 16),//fib(448)
                    new BigInteger("5df06e6e69fab31d34878c7f8eb357b08c314c8fa377f9ff7bf8735340dd257743dc6de76f109d", 16),//fib(449)
                    new BigInteger("97ff2de8fec3cc39012705e9ac55361b9804a0a3d65f70cf1b650cba0b3735905d29aa9e9d0858", 16),//fib(450)
                    new BigInteger("f5ef9c5768be7f5635ae92693b088dcc2435ed3379d76ace975d800d4c145b07a10618860c18f5", 16),//fib(451)
                    new BigInteger("18deeca4067824b8f36d59852e75dc3e7bc3a8dd75036db9db2c28cc7574b9097fe2fc324a9214d", 16),//fib(452)
                    new BigInteger("283de6697d040cae56c842abc226651b3e0707b0aca0e466c4a200cd4a35feb9f9f35dbaab53a42", 16),//fib(453)
                    new BigInteger("411cd30d837c31674a359c30f09c4159b9cab08e21a452209fce2999bfaab7c379d659ecf5e5b8f", 16),//fib(454)
                    new BigInteger("695ab97700803e15a0fddedcb2c2a674f7d1b83ece45368764702a6709e0b67d73c9b7a7a1395d1", 16),//fib(455)
                    new BigInteger("aa778c8483fc6f7ceb337b0da35ee7ceb19c68ccefe988a8043e5400c98b6e40eda01194971f160", 16),//fib(456)
                    new BigInteger("113d245fb847cad928c3159ea56218e43a96e210bbe2ebf2f68ae7e67d36c24be6169c93c3858731", 16),//fib(457)
                    new BigInteger("1be49d28008791d0f7764d4f7f98076125b0a89d8ae1847d76cecd2689cf792ff4f09dad0cf77891", 16),//fib(458)
                    new BigInteger("2d21c187b8cf5caa203962ee24fa204560478aae46c470706d59b50d07063b7bdb073a40d07cffc2", 16),//fib(459)
                    new BigInteger("49065eafb956ee7b17afb03da49227a685f8334bd1a5f4ede428823390d5b4abcff7d7eddd747853", 16),//fib(460)
                    new BigInteger("7628203772264b2537e9132bc98c47ebe63fbdfa186a655e5182374097dbf027aaff122eadf17815", 16),//fib(461)
                    new BigInteger("bf2e7ee72b7d39a04f98c3696e1e6f926c37f145ea105a4c35aab97428b1a4d37af6ea1c8b65f068", 16),//fib(462)
                    new BigInteger("135569f1e9da384c58781d69537aab77e5277af40027abfaa872cf0b4c08d94fb25f5fc4b3957687d", 16),//fib(463)
                    new BigInteger("1f4851e05c920be65d71a99fea5c92710beafa085ec8b19f6bcd7aa28e93f39cea0ece667c4bd58e5", 16),//fib(464)
                    new BigInteger("329dbbd2466c4432b5e9c7093dd73de8f11274fc5ef05d9a144049adda9cccec9c6e2e2b2fe14c162", 16),//fib(465)
                    new BigInteger("51e60db2a2fe5019135b70a92833d059fcfd6f04bdb90f39800dc4506930c089867cfc91ac2d21a47", 16),//fib(466)
                    new BigInteger("8483c984e96a944bc94537b2660b0e42ee0fe4011ca96cd3944e0dfe43cd8d7622eb2abcdc0e6dba9", 16),//fib(467)
                    new BigInteger("d669d7378c68e464dca0a85b8e3ede9ceb0d5305da627c0d145bd24eacfe4dffa968274e883b8f5f0", 16),//fib(468)
                    new BigInteger("15aeda0bc75d378b0a5e5e00df449ecdfd91d3706f70be8e0a8a9e04cf0cbdb75cc53520b6449fd199", 16),//fib(469)
                    new BigInteger("2315777f4023c5d15828688698288cb7cc42a8a0cd16e64edbd05b29b9dca297575bb7959ec858c789", 16),//fib(470)
                    new BigInteger("38c4518b0780fd5c6286c687776d2b85c9d47c113c87a4dce65af92e88e9604eb420ecb6550cf89922", 16),//fib(471)
                    new BigInteger("5bd9c90a47a4c32dbaaf2f0e0f95b83d961724b2099e8b2bc22b545842c602e60b7ca44bf3d55160ab", 16),//fib(472)
                    new BigInteger("949e1a954f25c08a1d35f5958702e3c35feba0c346263008a8864d86cbaf6334bf9d910248e249f9cd", 16),//fib(473)
                    new BigInteger("f077e39f96ca83b7d7e524a396989c00f602c5754fc4bb346ab1a1df0e75661acb1a354e3cb79b5a78", 16),//fib(474)
                    new BigInteger("18515fe34e5f04441f51b1a391d9b7fc455ee663895eaeb3d1337ef65da24c94f8ab7c6508599e55445", 16),//fib(475)
                    new BigInteger("2758de1d47cbac7f9cd003edcb4341bc54bf12bade5afa6717de99144e89a2f6a55d1fb9ec25180aebd", 16),//fib(476)
                    new BigInteger("3faa3e00962ab0c3bc21b5915d1cf9b89a1df91e67b9a91ae912180aac2bef8b9e089c1ef47eb660302", 16),//fib(477)
                    new BigInteger("67031c1dddf65d4358f1b97f28603b74eedd0bd94614a38200f0b11efab592824365bbd8e0a3ce6b1bf", 16),//fib(478)
                    new BigInteger("a6ad5a1e74210e0715136f10857d352d88fb04f7adce4c9cea02c929a6e1820de16e57f7d52284cb4c1", 16),//fib(479)
                    new BigInteger("10db0763c52176b4a6e05288faddd70a277d810d0f3e2f01eeaf37a48a197149024d413d0b5c65336680", 16),//fib(480)
                    new BigInteger("1b45dd05ac6387951831897a0335aa5d000d315c8a1b13cbbd4f643724878969e06426bc88ae8d801b41", 16),//fib(481)
                    new BigInteger("2c20e4697184fe49bf11dc02fe138167278ab269995942cdabfe9bdbaea0fab2e2b167f9940af2b381c1", 16),//fib(482)
                    new BigInteger("4766c16f1de885ded743657d01492bc42797e3c623745699694e0012d328841cc3158eb61cb980339d02", 16),//fib(483)
                    new BigInteger("7387a5d88f6d84289655417fff5cad2b4f22962fbccd9967154c9bee81c97ecfa5c6f6afb0c472e71ec3", 16),//fib(484)
                    new BigInteger("baee6747ad560a076d98a6fd00a5d8ef76ba79f5e041f0007e9a9c0154f202ec68dc8565cd7df31abbc5", 16),//fib(485)
                    new BigInteger("12e760d203cc38e3003ede87d0002861ac5dd10259d0f896793e737efd6bb81bc0ea37c157e426601da88", 16),//fib(486)
                    new BigInteger("1e9647467ea19983771868f7a00a85f0a3c978a1b7d5179681281d3f12bad84a87780017b4bc0591c964d", 16),//fib(487)
                    new BigInteger("317da818826dd2667757477f700aae52502749a411a6102cfa6690be10269066486237d90ca02bf1e70d5", 16),//fib(488)
                    new BigInteger("5013ef5f010f6be9ee6fb07710153442f3f0c245c97b27c37b8eadfd22e168b0cfda37f0c15c3183b0722", 16),//fib(489)
                    new BigInteger("81919777837d3e5065c6f7f6801fe29544180be9db2137f075f53ebb3307f917183c6fc9cdfc5d75977f7", 16),//fib(490)
                    new BigInteger("d1a586d6848caa3a5436a86d903516d83808ce2fa49c5fb3f183ecb855e961c7e816a7ba8f588ef947f19", 16),//fib(491)
                    new BigInteger("153371e4e0809e88ab9fda0641054f96d7c20da197fbd97a467792b7388f15adf005317845d54ec6edf710", 16),//fib(492)
                    new BigInteger("224dca5248c9692c50e3448d1a08a1045b429a8492459f75858fd182bdedabca6e869bf3eecad7b6827629", 16),//fib(493)
                    new BigInteger("37813c37294a07b4fc831e935b0df09b3304a8262a4178efcc076439f67cc1785e8bcd6c34a0267d706d39", 16),//fib(494)
                    new BigInteger("59cf0689721370e14d6663207516919f8e4742aabc871865519735bcb46a6d42cd126960236afe33f2e362", 16),//fib(495)
                    new BigInteger("915042c09b5d789649e981b3d024823ac14bead0e6c891551d9e99f6aae72ebb2b9e36cc580b24b163509b", 16),//fib(496)
                    new BigInteger("eb1f494a0d70e977974fe4d4453b13da4f932d7ba34fa9ba6f35cfb35f519bfdf8b0a02c7b7622e55633fd", 16),//fib(497)
                    new BigInteger("17c6f8c0aa8ce620de1396688155f961510df184c8a183b0f8cd469aa0a38cab9244ed6f8d3814796b98498", 16),//fib(498)
                    new BigInteger("2678ed554b63f4b8578894b5c5a9aa9ef607245c82d67e4c9fc0a395d698a66b71cff77254ef76a7c0fb895", 16),//fib(499)
                    new BigInteger("3e3fe615f5f0dad9359c2b1e46ffa400471515e14b7801fd988dea30773c33170414e4e1e2278b212c93d2d", 16),//fib(500)
                    new BigInteger("64b8d36b4154cf918d24bfd40ca94e9f3d1c3a3dce4e804a384e8dc64dd4d98275e4dc54371701c8ed8f5c2", 16),//fib(501)
                    new BigInteger("a2f8b9813745aa6ac2c0eaf253a8f29f8431501f19c68247d0dc77f6c5110c9979f9c136193e8cea1a232ef", 16),//fib(502)
                    new BigInteger("107b18cec789a79fc4fe5aac66052413ec14d8a5ce8150292092b05bd12e5e61befde9d8a50558eb307b28b1", 16),//fib(503)
                    new BigInteger("1aaaa466dafe0246712a695b8b3fb33de457eda7c01db84d9da077db3d7f6f2b569d85ec069941b9d21d5ba0", 16),//fib(504)
                    new BigInteger("2b25bd35a287a9e63628c407f144d751d06cc64d8e9f0876be3328370eadcd8d159b6fc4ab9e9aa502988451", 16),//fib(505)
                    new BigInteger("45d0619c7d85ac2ca7532d637c848a8fb4c4b3f54ebcc0c45bd3a0124c2d3cb86c38f5b0b237dc5ed4b5dff1", 16),//fib(506)
                    new BigInteger("70f61ed2200d5612dd7bf16b6dc961e185317a42dd5bc93b1a06c8495adb0a4581d465755dd67703d74e6442", 16),//fib(507)
                    new BigInteger("b6c6806e9d93023f84cf1eceea4dec7139f62e382c1889ff75da685ba70846fdee0d5b26100e5362ac044433", 16),//fib(508)
                    new BigInteger("127bc9f40bda05852624b103a58174e52bf27a87b0974533a8fe130a501e351436fe1c09b6de4ca668352a875", 16),//fib(509)
                    new BigInteger("1de831faf5b335a91e71a2f0942653ac3f91dd6b3358cdd3a05bb9900a8eb98415def1bc17df31dc92f56eca8", 16),//fib(510)
                    new BigInteger("3063fbef018d3b2e449653f439a7c8916b8457f2e3f013074959cc9a5aacee984cdd0dc5cebd7e82fb2a9951d", 16),//fib(511)
                    new BigInteger("4e4c2de9f74070d76307f6e4cdce1c3dab16355e1748e0dae9b5862a653ba81c62bbff81e69cb05f8e20081c5", 16),//fib(512)
                    new BigInteger("7eb029d8f8cdac05a79e4ad90775e4cf169a8d50fb38f3e2330f52c4bfe896b4af990d47b55a2ee2894aa16e2", 16),//fib(513)
                    new BigInteger("ccfc57c2f00e1cdd0aa641bdd544010cc1b0c2af1281d4bd1cc4d8ef25243ed112550cc99bf6df42176aa98a7", 16),//fib(514)
                    new BigInteger("14bac819be8dbc8e2b2448c96dcb9e5dbd84b50000dbac89f4fd42bb3e50cd585c1ee1a1151510e24a0b54af89", 16),//fib(515)
                    new BigInteger("218a8d95ed8e9e5bfbceace54b1fde6e899fc12af203c9d5c6c9904a30a311456d44326daed47ed66b81ff4830", 16),//fib(516)
                    new BigInteger("364555afac1c5aea26f2f5aeb8eb7ccc4724762af2df765fbbc6d3056ef3de9dc963140ec3e98fb8b58d53f7b9", 16),//fib(517)
                    new BigInteger("57cfe34599aaf94622c1a294040b5b3ad0c43755e4e340358290634f9f96efe336a7467c72be0e8f210f533fe9", 16),//fib(518)
                    new BigInteger("8e1538f545c7543049b49842bcf6d80717e8ad80d7c2b6953e5736550e8ace81000a5a8b36a79e47d69ca737a2", 16),//fib(519)
                    new BigInteger("e5e51c3adf724d766c763ad6c1023341e8ace4d6bca5f6cac0e799a4ae21be6436b1a107a965acd6f7abfa778b", 16),//fib(520)
                    new BigInteger("173fa55302539a1a6b62ad3197df90b49009592579468ad5fff3ecff9bcac8ce536bbfb92e00d4b1ece48a1af2d", 16),//fib(521)
                    new BigInteger("259df716b04abef1d22a10df03efb3e8ae942772e510ea42ac026699e6ace4b496d6d9c9a8972f7f5c5f49c26b8", 16),//fib(522)
                    new BigInteger("3cdd9c69b29e590c3d8cbe109bcf449d3e9d80985e577518abf653998277ad82ea429982d69804314943d3dd5e5", 16),//fib(523)
                    new BigInteger("627b938062e917fe0fb6ceef9fbef885ed31a80b43685f5b57f8ba33692492378119734c7f2f33b0a5a31d9fc9d", 16),//fib(524)
                    new BigInteger("9f592fea1587710a4d438d003b8e3d232bcf28a3a1bfd47403ef0dcceb9c3fba6b5c0ccf55c737e1eee6f17d282", 16),//fib(525)
                    new BigInteger("101d4c36a787089085cfa5befdb4d35a91900d0aee52833cf5be7c80054c0d1f1ec75801bd4f66b92948a0f1cf1f", 16),//fib(526)
                    new BigInteger("1a12df3548df7fa12aa3de8f016db72cc44cff95286e808435fd6d5cd405d11ac57d18ceb2abda3748371009a1a1", 16),//fib(527)
                    new BigInteger("2a302b6bf0668831b073844dff228a8755dd0ca016c103c12bbbe9dcd951de39e44470d06ffb40f0717fb0fb70c0", 16),//fib(528)
                    new BigInteger("44430aa1394607d2db1762dd009041b41a2a0c353f2f844561b95739ad57af54a9c1899f22a71b27b9b6c1051261", 16),//fib(529)
                    new BigInteger("6e73360d29ac90048b8ae72affb2cc3b700718d555f088068d75411686a98d8e8e05fa6f92a25c182b3672008321", 16),//fib(530)
                    new BigInteger("b2b640ae62f297d766a24a0800430def8a31250a95200c4bef2e985034013ce337c7840eb549773fe4ed33059582", 16),//fib(531)
                    new BigInteger("1212976bb8c9f27dbf22d3132fff5da2afa383ddfeb1094527ca3d966baaaca71c5cd7e7e47ebd3581023a50618a3", 16),//fib(532)
                    new BigInteger("1d3dfb769ef91bfb358cf7b3b0038e81a846962ea8030a09e6bd271b6eeac0754fd95028cfd354a97f510d80bae25", 16),//fib(533)
                    new BigInteger("2f5092e257c30e78f4afcac6e002ec2457ea1a0ca6b4134f0e8764b1da956d1c6c362810b45211df005347d11c6c8", 16),//fib(534)
                    new BigInteger("4c8e8e58f6bc2a742a3cc27a90067aa60030b03b4eb71d58f5448bcd49802d91bc0f7839842566887fa45551d74ed", 16),//fib(535)
                    new BigInteger("7bdf213b4e7f38ed1eec8d41700966ca581aca47f56b30a803cbf07f24159aae2845a04a387778677ff79d22f3bb5", 16),//fib(536)
                    new BigInteger("c86daf94453b636149294fbc000fe170584b7a8344224e00f9107c4c6d95c83fe4551883bc9cdeefff9bf274cb0a2", 16),//fib(537)
                    new BigInteger("1444cd0cf93ba9c4e6815dcfd7019483ab06644cb398d7ea8fcdc6ccb91ab62ee0c9ab8cdf51457577f938f97bec57", 16),//fib(538)
                    new BigInteger("20cba8063d8f5ffafb13f2cb9702929ab08b1bf4e7dafcca9f5ece917ff412b2df0efd151b1b136477f2f820c89cf9", 16),//fib(539)
                    new BigInteger("3510751336cb09bfe195509b6e04271e5b9180419b73d4b52f2c955e390ec8e1bfd8a8a1fa6c58d9efec311a448950", 16),//fib(540)
                    new BigInteger("55dc1d19745a69badca943670506b9b90c1c9c36834ed17fce8b63efb902db949ee7a5b715876c3e67df293b0d2649", 16),//fib(541)
                    new BigInteger("8aec922cab25737abe3e9402730ae0d767ae1c781ec2a634fdb7f94df211a4765ec04e590ff3c51857cb5a5551af99", 16),//fib(542)
                    new BigInteger("e0c8af461f7fdd359ae7d76978119a9073cab8aea21177b4cc435d3dab14800afda7f410257b3156bfaa83905ed5e2", 16),//fib(543)
                    new BigInteger("16bb54172caa550b059266b6beb1c7b67db78d526c0d41de9c9fb568b9d2624815c684269356ef66f1775dde5b0857b", 16),//fib(544)
                    new BigInteger("24c7df0b8ea252de5f40e42d5632e15f84f438dd562e5959e963eb3c9483aa48c5a1036795aea27c5d72061760f5b5d", 16),//fib(545)
                    new BigInteger("3b833322bb4ca7e964d34ae414e4a91602abc62fc23b9b388603a0a54e560c90db67878e290591e34ee963f5bbfe0d8", 16),//fib(546)
                    new BigInteger("604b122e49eefac7c4142f116b178a75879fff0d1869f4926f678be1e2d9b6d9a1088af5beb4345fac5b6a0d1cf3c35", 16),//fib(547)
                    new BigInteger("9bce4551053ba2b128e779f57ffc338b8a4bc53cdaa58fcaf56b2c87312fc36a7c701283e7b9c642fb44ce02d8f1d0d", 16),//fib(548)
                    new BigInteger("fc19577f4f2a9d78ecfba906eb13be0111ebc449f30f845d64d2b86914097a441d789d79a66dfaa2a7a0380ff5e5942", 16),//fib(549)
                    new BigInteger("197e79cd05466402a15e322fc6b0ff18c9c378986cdb514285a3de4f045393dae99e8affd8e27c0e5a2e50612ced764f", 16),//fib(550)
                    new BigInteger("29400f44fa390dda302decc035623af8dae234dd0c0c49885bf109d595942b7f2b7614d773495bb884a853e22c4bcf91", 16),//fib(551)
                    new BigInteger("42be8911ff7f71dcd18c1eeffc133a11a4a5ad7578e79acae194e82499e7bf5a15149fd74c2bd7c6ded6a443593945e0", 16),//fib(552)
                    new BigInteger("6bfe9856f9b87fb701ba0bb03175750a7f87e25284f3e4533d85f1fa2f7bead9408ab4aebf75337f637ef82585851571", 16),//fib(553)
                    new BigInteger("aebd2168f937f193d3462aa02d88af1c242d8fc7fddb7f1e1f1ada1ec963aa33559f54860ba10b4642559c68debe5b51", 16),//fib(554)
                    new BigInteger("11abbb9bff2f0714ad50036505efe2426a3b5721a82cf63715ca0cc18f8df950c962a0934cb163ec5a5d4948e644370c2", 16),//fib(555)
                    new BigInteger("1c978db28ec2862dea84660f08c86d342c7e301e280aae28f7bbba637c2433f3febc95dbad6b74a0be82a30f74301cc13", 16),//fib(556)
                    new BigInteger("2e43494e8df18d4297d469740eb84f7696b9873fd037a4600d85c7250bb22d44c81f366efa1cd88d18dfec585a7453cd5", 16),//fib(557)
                    new BigInteger("4adad7011cb413708258cf831780bcaac337b75df84252890541818887d66138c6dbcc4aa7884d2dd7628f67cea4708e8", 16),//fib(558)
                    new BigInteger("791e204faaa5a0b31a2d38f726390c2159f13e9dc879f6e912c748ad93888e7d8efb02b9a1a525baf0427bc02918c45bd", 16),//fib(559)
                    new BigInteger("c3f8f750c759b4239c86087a3db9c8cc1d28f5fbc0bc49721808ca361b5eefb655d6cf04492d72e8c7a50b27f7bd34ea5", 16),//fib(560)
                    new BigInteger("13d1717a071ff54d6b6b3417163f2d4ed771a34998936405b2ad012e3aee77e33e4d1d1bdead298a3b7e786e820d5f9462", 16),//fib(561)
                    new BigInteger("201100ef1395908fa533949eba1ac9db994432a9549f289cd42d8dd19ca466dea3aa8a0c234000b8c7f8c921018932e307", 16),//fib(562)
                    new BigInteger("33e272691ab585dd109ec8b5d059f72a70b5d5f2ed328ca286da8effd792dec1e1f7a72801ed2a430377418f8396927769", 16),//fib(563)
                    new BigInteger("53f373582e4b166cb5d25d548a74c10609fa089c41d1b53f5b081cd1743745a085a23134252d2afbcb700ab0851fc55a70", 16),//fib(564)
                    new BigInteger("87d5e5c149009c49c671260a5aceb8307aafde8f2f0441e1e1e2abd14bca24626799d85c271a553ecee74c4008b657d1d9", 16),//fib(565)
                    new BigInteger("dbc95919774bb2b67c43835ee543793684a9e72b70d5f7213ceac8a2c0016a02ed3c09904c47803a9a5756f08dd61d2c49", 16),//fib(566)
                    new BigInteger("1639f3edac04c4f0042b4a96940123166ff59c5ba9fda39031ecd74740bcb8e6554d5e1ec7361d579693ea330968c74fe22", 16),//fib(567)
                    new BigInteger("23f6897f4379801b6bef82cc82555aa9d8403ace610b030245bb83d16cbccf8684211eb7cbfa955b40395fa212462922a6b", 16),//fib(568)
                    new BigInteger("3a307d6cef7e450b701acd6316567dc04835d72a0b08a69277a85b18ad79886cd96e7cd69330b2b2d6cd49d51baef07288d", 16),//fib(569)
                    new BigInteger("5e2706ec32f7c526dc0a502f98abd86a207611f86c13a994bd63deea1a3657f35d8f9b8e5f2b480e1706a9772df519952f8", 16),//fib(570)
                    new BigInteger("9857845922760a324c251d92af02562a68abe922771c5027350c3a02c7afe06036fe1864f25bfac0edd3f34c49a40a07b85", 16),//fib(571)
                    new BigInteger("f67e8b45556dcf59282f6dc247ae2e948921fb1ae32ff9bbf27018ece1e63853948db3f3518742cf04da9cc37799239ce7d", 16),//fib(572)
                    new BigInteger("18ed60f9e77e3d98b74548b54f6b084bef1cde43d5a4c49e3277c52efa99618b3cb8bcc5843e33d8ff2ae900fc13d2da4a02", 16),//fib(573)
                    new BigInteger("285549ae3cd51a8e49c83f9173e5eb3537aefdf583d7c439f19ec6bdc8b7c51076019804b956a805ef7892cd338d6514187f", 16),//fib(574)
                    new BigInteger("4142aaa824535827010d8846c350f38126cbdc39597c88d824168becc351269bb2ba54ca3d94dbdeeea37bce2fa137ee6281", 16),//fib(575)
                    new BigInteger("6997f456612872b54ad5c7d83736deb65e7ada2edd544d1215b552aa8c08ebac28bbeccef6eb83e4de1c0e9b632e9d027b00", 16),//fib(576)
                    new BigInteger("aada9efe857bcadc4be3501efa87d2378546b66836d0d5ea39cbde974f5a1247db76419934805fc3ccbf8a6992cfd4f0dd81", 16),//fib(577)
                    new BigInteger("114729354e6a43d9196b917f731beb0ede3c19097142522fc4f813141db62fdf404322e682b6be3a8aadb9904f5fe71f35881", 16),//fib(578)
                    new BigInteger("1bf4d32536c20086de29c68162c468325690846ff4af5f8e6894d0fd92abd103bdfa870015fec436c779b236e88ce46e43602", 16),//fib(579)
                    new BigInteger("2d3bfc5a852c445ff7955800d5e0534134cc9d7965f1b1be2d8ce411b06200e2fe3da9e698b5827152276bc737eccb8d78e83", 16),//fib(580)
                    new BigInteger("4930cf7fbbee44e6d5bf1e8238a4bb738b5d21e95aa1114c9621b50f430dd1e6bc3830e6aeb446a819a11dfe2079affbbc485", 16),//fib(581)
                    new BigInteger("766ccbda411a8946cd5476830e850eb4c029bf62c092c30ac3ae9920f36fd2c9ba75dacd4769c9196bc889c558667b8935308", 16),//fib(582)
                    new BigInteger("bf9d9b59fd08ce2da31395054729ca284b86e14c1b33d45759d04e30367da4b076ae0bb3f61e0fc18569a7c378e02b84f178d", 16),//fib(583)
                    new BigInteger("1360a67343e23577470680b8855aed8dd0bb0a0aedbc697621d7ee75129ed777a3123e6813d87d8daf1323188d146a70e26a95", 16),//fib(584)
                    new BigInteger("1f5a8028e3b2c25a2137ba08d9cd8a305573781faf6fa6bb9774f3581606b1c2aa7d1f23533a5e89c769bd94c4a26d29318222", 16),//fib(585)
                    new BigInteger("32bb269c2794f7d1683e3ac15f2877be262e822a9d2c1031b94ce1cd28a5893a4d8f5d8b6712dc17767ce0ad51b6d79a13ecb7", 16),//fib(586)
                    new BigInteger("5215a6c50b47ba2b8975f4ca38f601ee7ba1fa4a4c9bb6ed50c1d5253eac3afcf80c7caeba4d3aa13de69e42165944c3456ed9", 16),//fib(587)
                    new BigInteger("84d0cd6132dcb1fcf1b42f8b981e79aca1d07c74e9c7c71f0a0eb6f26751c437459bda3a216016b8b4637eef68101c5d595b90", 16),//fib(588)
                    new BigInteger("d6e674263e246c287b2a2455d1147b9b1d7276bf36637e0c5ad08c17a5fdff343da856e8dbad5159f24a1d317e6961209eca69", 16),//fib(589)
                    new BigInteger("15bb7418771011e256cde53e16932f547bf42f334202b452b64df430a0d4fc36b83443122fd0d6812a6ad9c20e6797d7df825f9", 16),//fib(590)
                    new BigInteger("2329db5adaf258a4de80878373a4770e2dcb569f3568ec337bfafcf21b34dc29fc0ec880bd8bab96c98f7b95264e2de9e96f062", 16),//fib(591)
                    new BigInteger("38e54f7352026a87354e6cc18a37a662a9bf85d2776ba0863248f122bc09d860b4430b92ed5c8217f3fa555734b5c5c1c8f165b", 16),//fib(592)
                    new BigInteger("5c0f2ace2cf4c32c13cef444fddc1d70d78adc71acd48cb9ae43ee14d73eb48ab051d413aae82daebd89d0ec5b03f3abb2606bd", 16),//fib(593)
                    new BigInteger("94f47a417ef72db3491d61068813c3d3814a624424402d3fe08cdf3793488ceb6494dfa69844afc6b18426438fb9b96d7b51d18", 16),//fib(594)
                    new BigInteger("f103a50fabebf0df5cec554b85efe14458d53eb5d114b9f98ed0cd4c6a87417614e6b3ba432cdd756f0df72feabdad192db23d5", 16),//fib(595)
                    new BigInteger("185f81f512ae31e92a609b6520e03a517da1fa0f9f554e7396f5dac83fdcfce61797b9360db718d3c20921d737a776686a9040ed", 16),//fib(596)
                    new BigInteger("276fbc460d6cf0f7202f60b9d93f3865c32f4dfafc669a132fe2e79d068570fd78e62471b1e9e6ab18fa014a36535139fd6b64c2", 16),//fib(597)
                    new BigInteger("3fcf3e3b201b22e04a8ffc1efa1f72b740d1480a9bbbe886c6d8c26546626de3907ddda7bfa0ff7edb0323216dfac7a267fba5af", 16),//fib(598)
                    new BigInteger("673efa812d8813d76abf5cd8d35eab1d0400960598228299f6bbaa024ce7dee109640219718ae629f3fd246ba44e18dc65670a71", 16),//fib(599)
                    new BigInteger("a70e38bc4da336b7b54f58f7cd7e1dd444d1de1033de6b20bd946c67934a4cc499e1dfc1312be5a8cf00478d1248e07ecd62b020", 16),//fib(600)
                    new BigInteger("10e4d333d7b2b4a8f200eb5d0a0dcc8f148d27415cc00edbab4501669e0322ba5a345e1daa2b6cbd2c2fd6bf8b696f95b32c9ba91", 16),//fib(601)
                    new BigInteger("1b55b6bf9c8ce8146d55e0ec86e5ae6c58da45225ffdf58db71e482d1737c786a3d27c19bd3e2b17b91fdb385c8dfd9da002c6ab1", 16),//fib(602)
                    new BigInteger("2c3a89f3743f9cbd5f56cc4990f37afb6d676c63bcbe046962634993b53aea40fe06da37676997d4e54fb1f7e7f76d33532f62542", 16),//fib(603)
                    new BigInteger("479040b310cc84d1ccacad3617d92967c641b1861cbbf9f7198191c0cc72b1c7a1d9565124a7c2ec9e6f8d3044856ad0f33228ff3", 16),//fib(604)
                    new BigInteger("73cacaa6850c218f2c03797fa8cca46333a91de9d979fe607be4db5481ad9c089fe030888c115ac183bf3f282c7cd80446618b535", 16),//fib(605)
                    new BigInteger("bb5b0b5995d8a660f8b026b5c0a5cdcaf9eacf6ff635f85795666d154e204dd041b986d9b0b91dae222ecc58710242d53993b4528", 16),//fib(606)
                    new BigInteger("12f25d6001ae4c7f024b3a0356972722e2d93ed59cfaff6b8114b4869cfcde9d8e199b7623cca786fa5ee0b809d7f1ad97ff53fa5d", 16),//fib(607)
                    new BigInteger("1ea80e159b0bd6e511d63c6eb2a183ff9277ebcc9c5e5ef0fa6b1b57f1dee37a923533e3bed83961dc81cd7d90e815daeb988f3f85", 16),//fib(608)
                    new BigInteger("319a6b759cba2364142176720938ab2275512aa239595e5c7b7fcfde8edbc218204ecf59e2a4e0e8d6e0ae359ac007888397e339e2", 16),//fib(609)
                    new BigInteger("5042798b37c5fa4925f7b2e0bbda2f2207c9166ed5b7bd4d75eaeb3680baa592b284033da17d1a4ab3627bb32ba81d636f30727967", 16),//fib(610)
                    new BigInteger("81dce500d4801dad3a192952c512da447d1a41110f111ba9f16abb150f9667aad2d2d2978421fb338a4329e8c66824ebf2c855b349", 16),//fib(611)
                    new BigInteger("d21f5e8c0c4617f66010dc3380ed096684e3577fe4c8d8f76755a64b90510d3d8556d5d5259f157e3da5a59bf210424f61f8c82cb0", 16),//fib(612)
                    new BigInteger("153fc438ce0c635a39a2a058645ffe3ab01fd9890f3d9f4a158c061609fe774e85829a86ca9c110b1c7e8cf84b878673b54c11ddff9", 16),//fib(613)
                    new BigInteger("2261ba218ed0c4d99fa3ae1b9c6eced1186e0f010d8a2cd98c01607ac30388225dd807e41cf602630058e7520aa88a98ab6b9e60ca9", 16),//fib(614)
                    new BigInteger("37a17e5a5cdd2833d9464e7400cecd0bc88de88a1cc7cc23a18d6690cd01ff70e35aa26ae792136e1cd7744a5630110c60b7b03eca2", 16),//fib(615)
                    new BigInteger("5a03387bebaded0d78e9fc8f9d3d9bdce0fbf78b2a51f8fd2d8ec70b900587934132aa4f048815d11d305b9c60d89ba50c234e9f94b", 16),//fib(616)
                    new BigInteger("91a4b6d6488b154152304b039e0c68e8a989e0154719c520cf1c2d9c5d078704248d4cb9ec1a293f3a07cfe6b708acb16cdafede5ed", 16),//fib(617)
                    new BigInteger("eba7ef523439024ecb1a47933b4a04c58a85d7a0716bbe1dfcaaf4a7ed0d0e9765bff708f0a23f1057382b8317e1485678fe4d7df38", 16),//fib(618)
                    new BigInteger("17d4ca6287cc417901d4a9296d9566dae340fb7b5b885833ecbc722444a14959b8a4d43c2dcbc684f913ffb69cee9f507e5d94c5c525", 16),//fib(619)
                    new BigInteger("268f4957ab0fd19dee864da2a14a07273be958f5629f1415cc87216ec3721a432f00d3acbcd5ea75fe87826ece6cb3d5e5ed799da45d", 16),//fib(620)
                    new BigInteger("3e6413ba32dc1316f05af6cc0edf6e021f2a5470be276c49b94393930813639ce7a5a7e8eaa1b0faf79b82256b5b5326644b0e636982", 16),//fib(621)
                    new BigInteger("64f35d11ddebe4b4dee1446eb02975295b13ad6620c6805f85cab501cb857de016a67b95a7779b70f623049439c806fc4a3888010ddf", 16),//fib(622)
                    new BigInteger("a35770cc10c7f7cbcf3c3b3abf08e32b7a3e01d6deedeca93f0e4894d398e17cfe4c237e92194c6bedbe86b9a5235a22ae8396647761", 16),//fib(623)
                    new BigInteger("1084acdddeeb3dc80ae1d7fa96f325854d551af3cffb46d08c4d8fd969f1e5f5d14f29f143990e7dce3e18b4ddeeb611ef8bc1e658540", 16),//fib(624)
                    new BigInteger("1aba23ea9ff7bd44c7d59bae42e3b3b804f8fb113dea259b203e7462b72b740da133ec292cbaa3448d1a01207840ebb41a73fb4c9fca1", 16),//fib(625)
                    new BigInteger("2b3ed0c87ee2fb0cd2b773a8d9d6d93d524e16050de56c6bac8c043c211d5a037283161a7053b1c25b5819d5562fa1c609ffbd32f81e1", 16),//fib(626)
                    new BigInteger("45f8f4b31edab8519a8d0f571cba8cf5574711164bcf9206ccca789ed848ce1113b702439d0e5506e8721af5ce708d7a2473b87f97e82", 16),//fib(627)
                    new BigInteger("7137c57b9dbdb35e6d4482fff6916632a995271b59b4fe7279567cdaf9662814863a185e0d6206c943ca34cb24a02f402e7375b290063", 16),//fib(628)
                    new BigInteger("b730ba2ebc986bb007d19257134bf32800dc3831a58490794620f579d1aef62599f11aa1aa705bd02c3c4fc0f310bcba52e72e3227ee5", 16),//fib(629)
                    new BigInteger("128687faa5a561f0e7516155709dd595aaa715f4cff398eebbf777254cb151e3a202b32ffb7d262997006848c17b0ebfa815aa3e4b7f48", 16),//fib(630)
                    new BigInteger("1df9939d916ee8abe7ce7a7ae1d294c82ab4d977ea4be1f65059867ce9cc4145fba1c4da16242be699c42d44d0ac1a8b4d441d216dfe2d", 16),//fib(631)
                    new BigInteger("30801b9837144a9ccf1fdbd052706a5dd55bef6cba3f7ae50c50fda2367d93299da4780a11a1521030c4958d9227294af559c75fb97d75", 16),//fib(632)
                    new BigInteger("4e79af35c8833348b6ee564b3442ff260010c8e4a48b5cdb5caa841f2049d46f99463ce427c57df6ca88c2d262d343d6429de481277ba2", 16),//fib(633)
                    new BigInteger("7ef9cacdff977de5860e321b86b36983d56cb8515ecad7c068fb81c156c7679936eab4ee3966d006fb4d585ff4fa6d2137f7abe0e0f917", 16),//fib(634)
                    new BigInteger("cd737a03c81ab12e3cfc8866baf668a9d57d81360356349bc5a605e077113c08d030f1d2612c4dfdc5d61b3257cdb0f77a9590620874b9", 16),//fib(635)
                    new BigInteger("14c6d44d1c7b22f13c30aba8241a9d22daaea398762210c5c2ea187a1cdd8a3a2071ba6c09a931e04c12373924cc81e18b28d3c42e96dd0", 16),//fib(636)
                    new BigInteger("219e0bed58fcce042000742e8fca03ad78067babd657740f7f4478d8244e9dfaad74c9892fbbf6c0286f98ec4a495cf102d22cca4f1e289", 16),//fib(637)
                    new BigInteger("3664e03a7577f0f55c311fd6b3e4a0d052b51f444c7984d5422e9152412c2834cde683f5396528a07481d0256f15ded28dfb008e7db5059", 16),//fib(638)
                    new BigInteger("5802ec27ce74bef97c31940543aea47dcabb9af022d0f8e4c1730a2a657ac62f7b5b4d7e69211f609cf16911b95f3bc390cd2d58ccd32e2", 16),//fib(639)
                    new BigInteger("8e67cc6243ecafeed862b3dbf793454e1d70ba346f4a7dba03a19b7ca6a6ee644941d173a28648011173393728751a961ec82de74a8833b", 16),//fib(640)
                    new BigInteger("e66ab88a12616ee8549447e13b41e9cbe82c5524921b769ec514a5a70c21b493c49d1ef20ba76761ae64a248e1d45659af955b40175b61d", 16),//fib(641)
                    new BigInteger("174d284ec564e1ed72cf6fbbd32d52f1a059d0f590165f458c8b64123b2c8a2f80ddef065ae2daf62bfd7db800a4970efce5d892761e3958", 16),//fib(642)
                    new BigInteger("25b3d3d7668af8dbf818b439e6e1718e5edc9647d93816af78dcae6cabeea578bd27c0f57b9d516c46e3c7dc8ec1dc7497df2e467793ef75", 16),//fib(643)
                    new BigInteger("3d00fc262befdac96ae823f5ba0ec47fff36673d694e75f50568127ee71b2fa83e05affbd6802c6272e145948f66738394c506d8edb228cd", 16),//fib(644)
                    new BigInteger("62b4cffd927ad3a56300d82fa0f0360e5e12fd8542868ca47e44c0eb9309d520fb2d70f1521d7dceb9c50d711e284ff82ca4351f65461842", 16),//fib(645)
                    new BigInteger("9fb5cc23be6aae6ecde8fc255afefa8e5d4964c2abd5029983acd36a7a2504c9393320ed289daa312ca65305ad8ec37bc1693bf852f8410f", 16),//fib(646)
                    new BigInteger("1026a9c2150e5821430e9d454fbef309cbb5c6247ee5b8f3e01f194560d2ed9ea346091de7abb27ffe66b6076cbb71373ee0d7117b83e5951", 16),//fib(647)
                    new BigInteger("1a22068450f503082fed2d07a56ee2b2b18a5c70a9a3091d7859e67c08753deb36d93b2cba358d2311311b37c7945d6efaf76ad100b369a60", 16),//fib(648)
                    new BigInteger("2a48b04666035b2972fbca4cf52dd5bc7d4022952888c2115878ffc169482b89da1f444aa1e13fa30f97d13f344fcea639d841e27c374f3b1", 16),//fib(649)
                    new BigInteger("446ab6cab6f85e31a2e8f7549a9cb86f2eca7f05d22bcb2ed0d2e63d71bd697510f87f775c16ccc620c8ec76fbe42c1534cfacb37ceab8e11", 16),//fib(650)
                    new BigInteger("6eb367111cfbb95b15e4c1a18fca8e2bac0aa19afab48d40294be5fedb0594feeb17c3c1fdf80c693060bdb63033fabb6ea7ee95f922081c2", 16),//fib(651)
                    new BigInteger("b31e1ddbd3f4178cb8cdb8f62a67469adad520a0cce0586efa1ecc3c4cc2fe73fc1043395a0ed92f5129aa2d2c1826d0a3779b49760cc0fd3", 16),//fib(652)
                    new BigInteger("121d184ecf0efd0e7ceb27a97ba31d4c686dfc23bc794e5af236ab23b27c89372e72806fb5806e598818a67e35c4c218c121f89df6f2ec9195", 16),//fib(653)
                    new BigInteger("1d4efa2c8c4e3e8748780338de4991b6161b4e2dc94753e1e1d897e77748b91e6e3384a34b215bec7d2b412108864485cb5972528e53b8a168", 16),//fib(654)
                    new BigInteger("2f6c127b5b5d3b95c5632ae259ecaf027e894a5185c0a23cd40f430b29c542559ca6051300a1ca460543e79f3e4b069e8c7b6af08546a532fd", 16),//fib(655)
                    new BigInteger("4cbb0ca7e7ab7a1d0ddb2e1b383640b894a4987f4f07f61eb5e7daf2a10dfb740ad989b64bc32632826f28c046d14b2457d4dd43139a5dd465", 16),//fib(656)
                    new BigInteger("7c271f234308b5b2d33e58fd9222efbb132de2d0d4c8985b89f71dfdcad33dc9a77f8ec94c64f07887b3105f851c51c2e450483398e1030762", 16),//fib(657)
                    new BigInteger("c8e22bcb2ab42fcfe1198718ca593073a7d27b5023d08e7a3fdef8f06be1393db259187f982816ab0a22391fcbed9ce73c252576ac7b60dbc7", 16),//fib(658)
                    new BigInteger("145094aee6dbce582b457e0165c7c202ebb005e20f89926d5c9d616ee36b4770759d8a748e48d072391d5497f5109eeaa20756daa455c63e329", 16),//fib(659)
                    new BigInteger("20deb76b9987115529571672f26d550a262d2d9711c69b55009b50fdea295b0450c31bfc87cb51dce9bf7829f1cf78b915c9a9320f1d7c4bef0", 16),//fib(660)
                    new BigInteger("352f4c1a8062dfad549c94745835170d11dd337921502dc25d38b26ccd94a274c660a6711614224f22dcccc1e6e017a3b7d1000cb373428a219", 16),//fib(661)
                    new BigInteger("560e038619e9f1027df3aae74aa26c17380a61103316c9175dd4036ab7bdfd791723c26d9ddf742c0c9c44ebd8af905ccd9aa93ec290bed6109", 16),//fib(662)
                    new BigInteger("8b3d4fa09a4cd0afd2903f5ba2d7832449e794895466f6d9bb0cb5d785529feddd8468deb3f3967b2f7911adbf8fa800856ba94b76040160322", 16),//fib(663)
                    new BigInteger("e14b5326b436c1b25083ea42ed79ef3b81f1f599877dbff118e0b9423d109d66f4a82b4c51d30aa73c155699983f385d5306528a3894c03642b", 16),//fib(664)
                    new BigInteger("16c88a2c74e8392622314299e9051725fcbd98a22dbe4b6cad3ed6f19c2633d54d22c942b05c6a1226b8e684757cee05dd871fbd5ae98c19674d", 16),//fib(665)
                    new BigInteger("24dd3f5ee02ba5414739813e17dcb619b4dcb7fbc636276bbecce285bff73dabbc6d4bf775799abc9a7a3bee0f00e18bb2b784e5fe72d81ccb78", 16),//fib(666)
                    new BigInteger("3ba5c98b5513de67696ac3d800e1cd3fb19a509df3f472d86c0bb9775c1d71810990153a25d604cec1332272847dcf91903ea4a3595c643632c5", 16),//fib(667)
                    new BigInteger("608308ea353f83a8b0a4451618be835966770899ba2a9a442ad89bfd1c14af2cc5fd61319b4f9f8b5bad5e60937eb11d42f6298957cf3c52fe3d", 16),//fib(668)
                    new BigInteger("9c28d2758a5362101a0f08ee19a0509918115937ae1f0d1c96e45574783220adcf8d766bc125a45a1ce080d317fc80aed334ce2cb12ba0893102", 16),//fib(669)
                    new BigInteger("fcabdb5fbf92e5b8cab34e04325ed3f27e8861d16849a760c1bcf1719446cfda958ad79d5c7543e5788ddf33ab7b31cc162af7b608fadcdc2f3f", 16),//fib(670)
                    new BigInteger("198d4add549e647c8e4c256f24bff248b9699bb091668b47d58a146e60c78f08865184e091d9ae83f956e6006c377b27ae95fc5e2ba267d656041", 16),//fib(671)
                    new BigInteger("29580893509792d81af75a4f67e5df87e15221cda7eb25bde1a5e3857a0bfc062faa325a67a102c250dfc3f3a6ef2e446ff8abd98c3215a418f80", 16),//fib(672)
                    new BigInteger("42e55370a535f754a9437fbe8ca5d1d09abbbd7e3951b105b72ff7f3dad38b0eb5fbb73af97ab1464a36a9f41326a96c1e8ea837b7d47d7a6efc1", 16),//fib(673)
                    new BigInteger("6c3d5c03f5cd8a2cc43ada0df48bb1587c0ddf4be13cd6c398d5db7954df8714e5a5e995611bb4089b166de7ba15d7b08e8754114406931e87f41", 16),//fib(674)
                    new BigInteger("af22af749b0381816d7e59cc8131832916c99cca1a8e87c95005d36d2fb312239ba1a0d05a96654ee54d17dbcd3c811cad15fc48fbdb1098f6f02", 16),//fib(675)
                    new BigInteger("11b600b7890d10bae31b933da75bd348192d77c15fbcb5e8ce8dbaee68492993881478a65bbb21957806385c3875258cd3b9d505a3fe1a3b77ee43", 16),//fib(676)
                    new BigInteger("1ca82baed2bd48d2f9f378da6f6eeb7aaa9a118e01659e65638e18253b445ab5c1ce92b3616487ea665b09d9f548ed9e9e8b34ca33bbcb45075d45", 16),//fib(677)
                    new BigInteger("2e5e2c665bca598ddd0f0c1816cabec2c3c7894f6122544e321bd313a38d844949e30b59bd1fa97fde6142362dbe132b724509cfd7b9e5807f4b88", 16),//fib(678)
                    new BigInteger("4b0658152e87a260d70284f28639aa3d6e619add6287f2b395a9eb38ded1deff0bb19e0d1e84316a44bc4c10230700ca10d03e9a0b75b0c586a8cd", 16),//fib(679)
                    new BigInteger("7964847b8a51fbeeb411910a9d0469003229242cc3aa4701c7c5be4c825f63485594a966dba3daea231d8e4650c513f583154869e32f964605f455", 16),//fib(680)
                    new BigInteger("c46adc90b8d99e4f8b1415fd233e133da08abf0a263239b55d6fa9856131424761464773fa280c5467d9da5673cc14bf93e58703eea5470b8c9d22", 16),//fib(681)
                    new BigInteger("13dcf610c432b9a3e3f25a707c0427c3dd2b3e336e9dc80b7253567d1e390a58fb6daf0dad5cbe73e8af7689cc49128b516facf6dd1d4dd51929177", 16),//fib(682)
                    new BigInteger("2023a3d9cfc05388dca39bd04e3808f7b733ea241100eba6c82a5115744c1e7d71821384ecff3f392f2d142f3385d3d74aae05671c07a245d1f2e99", 16),//fib(683)
                    new BigInteger("340099ea93f30d2cc095f640ca3c30bb945f28577f9eb3b23a7da792928528d66cefc2929a5bfdad17dc8ab8ffcee6629c1db25df924f01aeb1c010", 16),//fib(684)
                    new BigInteger("54243dc463b360b59d399211187439b34b93127b909f9f5902a7f8a806d14753de71d617875b3ce647099ee83354ba39e6cbb7c5152c9260bd0eea9", 16),//fib(685)
                    new BigInteger("8824d7aef7a66de25dcf8851e2b06a6edff23ad3103e530b3d25a03a9956702a4b6198aa21b73a935ee629a13323a09c82e96a230e51827ba82aeb9", 16),//fib(686)
                    new BigInteger("dc4915735b59ce97fb091a62fb24a4222b854d4ea0ddf2643fcd98e2a027b77e29d36ec1a9127779a5efc88966785ad669b521e8237e14dc6539d62", 16),//fib(687)
                    new BigInteger("1646ded2253003c7a58d8a2b4ddd50e910b778821b11c456f7cf3391d397e27a87535076bcac9b20d04d5f22a999bfb72ec9e8c0b31cf97580d64c1b", 16),//fib(688)
                    new BigInteger("240b70295ae5a0b1253e1bd17d8f9b2b336fcd57051fa37d3bcc0d1ffd9a5df269f08762d73dc2986aac5bab4001456495653adf3554dac34729e97d", 16),//fib(689)
                    new BigInteger("3a524efb8015a478cacba5fccb6cec14442745d9203167d4339b40b1d132406cf143d7d993ea5db93af9bacde99b051bc42f239fe871d438c8003598", 16),//fib(690)
                    new BigInteger("5e5dbf24dafb4529f009c1ce48fc873f7797133025510b516f674dd1cecc9e5f5b345f3c6b282051a5a61679299c4a8059945e7f1dc6aefc0f2a1f15", 16),//fib(691)
                    new BigInteger("98b00e205b10e9a2bad567cb14697353bbbe590945827325a3028e839ffedecc4c783715ff127e0ae09fd14713374f9c1dc3821f06388334d72a54ad", 16),//fib(692)
                    new BigInteger("f70dcd45360c2eccaadf29995d65fa9333556c396ad37e771269dc556ecb7d2ba7ac96526a3a9e5c8645e7c03cd39a1c7757e09e23ff3230e65473c2", 16),//fib(693)
                    new BigInteger("18fbddb65911d186f65b4916471cf6de6ef13c542b055f19cb56c6ad90eca5bf7f424cd68694d1c6766e5b907500ae9b8951b62bd2a37b565bd7ec86f", 16),//fib(694)
                    new BigInteger("286cba8aac729473c1093bafdcf35687a2269317c1b297013c7d6472e7d95d9239bd163bad387bac3ed2ba0c78cde83d50c73435b4e36e796a3d33c31", 16),//fib(695)
                    new BigInteger("41689841058465fab76484c624104d661117cf6becb7f61b07d42b2078c60351b8ff631233cd4d72b541159cedce96d8da18ea618786e9cfc615204a0", 16),//fib(696)
                    new BigInteger("69d552cbb1f6fa6e786dc0760103a3edb33e6283ae6a8d1c44518f93609f60e3f2bc794de105c91ef413cfa9669c7f162ae01e973c6a58493052540d1", 16),//fib(697)
                    new BigInteger("ab3deb0cb77b60692fd2453c2513f153c45631ef9b2283374c25bab3d9656435abbbdc6014d31691a954e546546b15ef04f908f8c3f14218f66774571", 16),//fib(698)
                    new BigInteger("115133dd869725ad7a84005b22617954177949473498d105390774a473a04c5199e7855adf5d8dfb09d68b4efbb0795052fd92790005b9a6226b9c8642", 16),//fib(699)
                    new BigInteger("1c05128e520edbb40d8124aee4b2b86953beac662e4af938adc9d04fb136a294f4a34320e0aabf64246bd9a360f72aaf434d23088c44cdc7b1d213cbb3", 16),//fib(700)
                    new BigInteger("2d56466bd8a601618805250a071431bd6b37f5ad62e3ca3de6d144f424d6eee68e8ac87bc0084d5f2e4264f25ca7a3ff964ab5818c4a876dd43db051f5", 16),//fib(701)
                    new BigInteger("495b58fa2ab4dd15958649b8ebc6ea26bef6a213912ec376949b1543d60d917b832e0b9ca0b30cc352ae3e95bd9eceaed997d88a188f5535860fc41da8", 16),//fib(702)
                    new BigInteger("76b19f66035ade771d8b6ec2f2db1be42a2e97c0f4128db47b6c5a37fae4806211b8d41860bb5a2280f0a3881a4672ae6fe28e0ba4d9dca35a4d746f9d", 16),//fib(703)
                    new BigInteger("c00cf8602e0fbb8cb311b87bdea2060ae92539d48541512b10076f7bd0f211dd94e6dfb5016e66e5d39ee21dd7e5415d497a6695bd6931d8e05d388d45", 16),//fib(704)
                    new BigInteger("136be97c6316a9a03d09d273ed17d21ef1353d1957953dedf8b73c9b3cbd6923fa69fb3cd6229c108548f85a5f22bb40bb95cf4a162430e7c3aaaacfce2", 16),//fib(705)
                    new BigInteger("1f6cb90265f7a559083aedfbab01f27f9fc790b69fe95300a9b7b392f9cc8a41d3b869382639827ee282e67c3ca10f56902d75b371fac40551b07e58a27", 16),//fib(706)
                    new BigInteger("32d8a27ec90e4ef94544c06f9819c49e90fccdcff77e90eea26ef02e3689f365ce226474fc5c1e8f67cbded69bc3ca974bc344fd881ef4ed155b2928709", 16),//fib(707)
                    new BigInteger("52455b812f05f4524d7fae6b431bb71e30c45e869767e3ef4c26a3c130567da7a1dacdad2295a10e4a4ec552d864d9eddbf0bab0fa19b8f2670ba781130", 16),//fib(708)
                    new BigInteger("851dfdfff814434b92c46edadb357bbcc1c12c568ee674ddee9593ef66e0710d6ffd32221ef1bf9db21aa4297428a48527b3ffae8238addf7c66d0a9839", 16),//fib(709)
                    new BigInteger("d7635981271a379de0441d461e5132daf2858add264e58cd3abc37b09736eeb511d7ffcf418760abfc69697c4c8d7e7303a4ba5f7c5266d1e372782a969", 16),//fib(710)
                    new BigInteger("15c8157811f2e7ae973088c20f986ae97b446b733b534cdab2951cb9ffe175fc281d531f160792049ae840da5c0b622f82b58ba0dfe8b14b15fd948d41a2", 16),//fib(711)
                    new BigInteger("233e4b1024648b287534ca96717d7e172a6cc4210db832678640e0350954e4e7793ad31c0a20080f5aaed77220d43a16b2efd746d7add7b83434bc0feb0b", 16),//fib(712)
                    new BigInteger("39066088365772d70c6553588115e900a5b12f94490b7f4238d5fcef09365ae3a158263b20279a13f597184c7cdf9c4635a562e7b79689034a32509d2cad", 16),//fib(713)
                    new BigInteger("5c44ab985abbfdff819a1deef2936717d01df3b556c3b1a9bf16dd24128b3fcb1a92f9572a47a2235045efbe9db3d65ce8953a2e8f4460bb7e670cad17b8", 16),//fib(714)
                    new BigInteger("954b0c20911370d68dff714773a9501875cf23499fcf30ebf7ecda131bc19aaebbeb1f924a6f3c3745dd080b1a9372a31e3a9d1646dae9bec8995d4a4465", 16),//fib(715)
                    new BigInteger("f18fb7b8ebcf6ed60f998f36663cb73045ed16fef692e295b703b7372e4cda79d67e18e974b6de5a9622f7c9b847490006cfd744d61f4a7a470069f75c1d", 16),//fib(716)
                    new BigInteger("186dac3d97ce2dfac9d99007dd9e60748bbbc3a4896621381aef0914a4a0e75289269387bbf261a91dbffffd4d2dabba3250a745b1cfa34390f99c741a082", 16),//fib(717)
                    new BigInteger("2786a7b9268b24e82ad328fb44022be7901a951478cf4f61765f44881785b4fa268e7516533dcf8ec7222f79e8b2204a32bda4b9ff3197eb3569a3138fc9f", 16),//fib(718)
                    new BigInteger("3ff453f6be5952e2f4acb90321a08c5c1bd658b902357099914e4d9cbc269c4cafb5089e0f303137e4e22f7735dfcc04650e4bffb1013b2ec6633f87a9d21", 16),//fib(719)
                    new BigInteger("677afbafe4e477cb1f7fe1fe65a2b843abf0edcd7b04bffb07ad9224d3ac5146d6437db4626e00c6ac045ef11e91ec4e97cbf0b9b032d319fbcce29b399c0", 16),//fib(720)
                    new BigInteger("a76f4fa6a33dcaae142c9b018743449fc7c746867d3a309498fbdfc18fd2ed9385f88652719e31fe90e68e685471b852fcda3cb961340e48c2302222e36e1", 16),//fib(721)
                    new BigInteger("10eea4b568822427933ac7cffece5fce373b83453f83ef08fa0a971e6637f3eda5c3c0406d40c32c53ceaed597303a4a194a62d731166e162bdfd04be1d0a1", 16),//fib(722)
                    new BigInteger("1b6599afd2b600d2747d91801742941833b7f7ada7579212439a551a7f3522c6de2348a5945aa64c3cdd17bc1c7755cf491806a2c729aefab802d26e100782", 16),//fib(723)
                    new BigInteger("2c543e653b3824fa07b859501610f3e66af37af2e6db811b3da4ec38e56d16b483e708e6019b697890abc691b3a7901962626979f8401d10e3e2a2b9f1d823", 16),//fib(724)
                    new BigInteger("47b9d8150dee25cc7c35ead02d5387fe9eab72a08e33132d813f415364a2397b620a518b95f60fc4cd88de4dd01ee5e8ab7a701cbf69cc0b9be5752801dfa5", 16),//fib(725)
                    new BigInteger("740e167a49264ac683ee442043647be5099eed93750e9448bee42d8c4a0f502fe5f15a719791793d5e34a4df83c676020ddcd996b7a9e91c7fc817e1f3b7c8", 16),//fib(726)
                    new BigInteger("bbc7ee8f5714709300242ef070b803e3a84a60340341a77640236edfaeb189ab47fbabfd2d8789022bbd832d53e55beab95749b37713b5281bad8d09f5976d", 16),//fib(727)
                    new BigInteger("12fd60509a03abb5984127310b41c7fc8b1e94dc778503bbeff079c6bf8c0d9db2ded066ec519023f89f2280cd7abd1ecc734234a2ebd9e449b75a4ebe94f35", 16),//fib(728)
                    new BigInteger("1eb9df398f74f2bec8436a20124d483ac5a33adfb7b91e3353f2b0b4ba772638675e8b26bf2a08b41b5afab3a2b912dd7808b6cfda5d1536cb72331f5dee6a2", 16),//fib(729)
                    new BigInteger("31b73f8a29789e74608491511d8f103750c1cfbc2f3e21ef43e32a7b7a0333d61a3d5b8dab7b98d813fa1d347033cffc447bf9047d48ef1b15298d6e1c835d7", 16),//fib(730)
                    new BigInteger("50711ec3b8ed913328c7fb712fdc587216650a9be6f7402297d5db30347a5a0e819be6b46aa5a18c2f5517e812ece2d9bc84afd457a60451e09bc08d7a71c79", 16),//fib(731)
                    new BigInteger("82285e4de2662fa7894c8cc24d6b68a96726da5816356211dbb905abae7d8de49bd9424216213a64434f351c8320b2d60100a8d8d4eef36cf5c54dfb96f5250", 16),//fib(732)
                    new BigInteger("d2997d119b53c0dab21488337d47c11b7d8be4f3fd2ca234738ee0dbe2f7e7f31d7528f680c6dbf072a44d04960d95afbd8558ad2c94f7bed6610e891166ec9", 16),//fib(733)
                    new BigInteger("154c1db5f7db9f0823b6114f5cab329c4e4b2bf4c136204464f47e687917575d7b94e6b3896e81654b5f38221192e4885be8601860183eb2bcc265c84a85c119", 16),//fib(734)
                    new BigInteger("2275b5871190db15ced759d2947faeae0623ea440108ea67ac2d6c763746d5dcad6c3942f17aef2452897cf25af3bde357c0b5a332e18e2eaa2876b0db9c2fe2", 16),//fib(735)
                    new BigInteger("37c1d33d096c7a1df28d6b21f12ae14a546f1638c23f0aac1121eadeb05e2d3a29011ff67ae970899de8b5146c86a26bb3a915bb92f9cce166eadc792621f0fb", 16),//fib(736)
                    new BigInteger("5a3788c41afd5533c164c4f485aa8ff85a93007cc347f513bd4f5754e7a50316d66d59396c645fadf0723206c77a604f0b69cb5ec5db5b101113532a01be20dd", 16),//fib(737)
                    new BigInteger("91f95c012469cf51b3f2301676d57142af0216b58586ffbfce71423398033050ff6e792fe74dd0378e5ae71b340102babf12e11a58d527f177fe2fa327e011d8", 16),//fib(738)
                    new BigInteger("ec30e4c53f6724857556f50afc80013b0995173248cef4d38bc099887fa83367d5dbd26953b22fe57ecd1921fb7b6309ca7cac791eb08301891182cd299e32b5", 16),//fib(739)
                    new BigInteger("17e2a40c663d0f3d7294925217355727db8972de7ce55f4935a31dbbc17ab63b8d54a4b993b00001d0d28003d2f7c65c4898f8d937785aaf3010fb270517e448d", 16),//fib(740)
                    new BigInteger("26a5b258ba338185c9ea01a2c6fd573b8c22c451a1724e966e5f2754497539720ab261e028eb230028bf5195f2af7c8ce540c3a0c96362df48a21353d7b1c7742", 16),//fib(741)
                    new BigInteger("3e885665207090c33c7e93f4de32ae6367ac37301e57addfa40245100aefefad98070699bc9b2301f991d199c5a742e92dd9bc7a00dbbd8e78b30e7adcc9abbcf", 16),//fib(742)
                    new BigInteger("652e08bddaa4124906689597a530059ef3cefb81bfc9fc7612616c645465291fa2b96879e58646022251232fb856bf76131a801aca3f206dc15521ceb47b73311", 16),//fib(743)
                    new BigInteger("a3b65f22fb14a30c42e7298c8362b4025b7b32b1de21aa55b663b1745f5518cd3ac06f13a22169041be2f4c97dfe025f40f43c94cb1addfc3a08304991451eee0", 16),//fib(744)
                    new BigInteger("108e467e0d5b8b555494fbf242892b9a14f4a2e339deba6cbc8c51dd8b3ba41ecdd79d78d87a7af063e3417f93654c1d5540ebcaf9559fe69fb5d521845c0921f1", 16),//fib(745)
                    new BigInteger("1ac9ac703d0cd58618c36e8b0abf56da3aac560e57c0d51217f28cf4d130f5aba183a46a129c9180a5a170cc2b452c4349502f9446074dc6635658261d705b10d1", 16),//fib(746)
                    new BigInteger("2b57f2ee4a6860db6d586a7d4d4882744fa0f8f1919f8f7ed47eded25c6c99ca6f5b41e2eb170c710984b24bbeaa78609e911b5f3f5cedad030c2d47a1cc6432c2", 16),//fib(747)
                    new BigInteger("46219f5e87753661861bd9085807d94e8a4d4effe9606490ec716bc72d9d8f7610dee64cfdb39df1af262317e9efa4a3e7e14af385643b736662856dbf3cbf4393", 16),//fib(748)
                    new BigInteger("7179924cd1dd973cf3744385a5505bc2d9ee47f17afff40fc0f04a998a0a2940803a282fe8caaa62b8aad563a89a1d0486726652c4c12920696eb2b56109237655", 16),//fib(749)
                    new BigInteger("b79b31ab5952cd9e79901c8dfd583511643b96f1646058a0ad61b660b7a7b8b691190e7ce67e485467d0f87b9289c1a86e53b1464a256493cfd138232045e2b9e8", 16),//fib(750)
                    new BigInteger("12914c3f82b3064db6d046013a2a890d43e29dee2df604cb06e5200fa41b1e1f7115336accf48f2b7207bcddf3b23deacf4c617990ee68db4393fead8814f06303d", 16),//fib(751)
                    new BigInteger("1e0aff5a384833279e6947ca1a000c5e5a26575d443c0a5511bb3b75af9599aada26c4529b5c73b0b884cc65acdada0556319c8df590bf248091122fba194e8ea25", 16),//fib(752)
                    new BigInteger("309c4b99bafb397555398dcb542a956b9e08f54b72320f2018a05b8553b0b7ca4b3bf7bd685102dc2a8c8943a08d17f0257dfe07867f27ffc42510dd422e3ef1a62", 16),//fib(753)
                    new BigInteger("4ea74af3f3436c9cf3a2d5956e2aa1c9f82f4ca8b66e19752a5b96fb034651752562bc1003ad768ce31155a94d67f1f57baf9a957c0fe72444b6230cfc478d80487", 16),//fib(754)
                    new BigInteger("7f43968dae3ea61248dc6360c2553735963841f428a0289542fbf28056f7093f709eb3cd6bfe79690d9ddeecedf509e5a12d989d028f0f2408db33ea3e75cc71ee9", 16),//fib(755)
                    new BigInteger("cdeae181a18212af3c7f38f6307fd8ff8e678e9cdf0e420a6d57897b5a3d5ab496016fdd6fabeff5f0af34963b5cfbdb1cdd33327e9ef6484d9156f73abd59f2370", 16),//fib(756)
                    new BigInteger("14d2e780f4fc0b8c1855b9c56f2d51035249fd09107ae6a9fb0537bfbb13463f406a023aadbaa695efe4d1383295205c0be0acbcf812e056c566c8ae179332664259", 16),//fib(757)
                    new BigInteger("21b195990f142cb70c1dad54d2354e934b3075f2de6bcacaa1dab05770b71bea89ca193884b565954eefc481964af019bdae7ff01ffccfbb4a3fde1d8b3f080565c9", 16),//fib(758)
                    new BigInteger("36847d1a041038432473671a41629f969d7a72fbeee6b1749cdfe8172bca6229ca341b7332700c2b3ed495b9c8e01075c98f2cad180fb0120fa6a6cba2d23a6ba822", 16),//fib(759)
                    new BigInteger("583612b3132464fa3091146f1397ee29e8aae8eecd527c3f3eba986e9c817e1453fe34abb72571c08dc45a3b5f2b008f873dac9d380c7fcd59e684e92e1142710deb", 16),//fib(760)
                    new BigInteger("8eba8fcd17349d3d55047b8954fa8dc086255beabc392db3db9a8085c84be03e1e32501ee9957debcc98eff5280b110550ccd94a501c2fdf698d2bb4d0e37cdcb60d", 16),//fib(761)
                    new BigInteger("e6f0a2802a59023785958ff868927bea6ed044d9898ba9f31a5518f464cd5e52723084caa0baefac5a5d4a3087361194d80a85e78828afacc373b09dfef4bf4dc3f8", 16),//fib(762)
                    new BigInteger("175ab324d418d9f74da9a0b81bd8d09aaf4f5a0c445c4d7a6f5ef997a2d193e909062d4e98a506d9826f63a25af41229a28d75f31d844df8c2d00dc52cfd83c2a7a05", 16),//fib(763)
                    new BigInteger("25c9bd4cd6be6a1ac602f9b7a261f859563c5e59dcf50819a1044b26e91e69ce3029359b42b0b5d44815384563677342f00e1e519606d8f38f0748cf0ceccfb783dfd", 16),//fib(764)
                    new BigInteger("3d247071aad7441213ac9a6fbe3ac8f4058bb86621515594106344be8beffdb7392f62e9db55bcadca849be7be5b856c929b9444b38b26ec51d7569439ea537a2b802", 16),//fib(765)
                    new BigInteger("62ee2dbe8195ae2cd9af9427609cc14d5bc816bffe465dadb1678fe5750e6785695898851e0672821299d42d21c2f8af82a9b2964991ffdfe0de9f6346d72331af5ff", 16),//fib(766)
                    new BigInteger("a0129e302c6cf23eed5c2e971ed78a416153cf261f97b341c1cad4a400fe653ca287fb6ef95c2f2fdd1e7014e01e7e1c154546dafd1d26cc32b5f5f780c176abdae01", 16),//fib(767)
                    new BigInteger("10300cbeeae02a06bc70bc2be7f744b8ebd1be5e61dde10ef73326489760cccc20be093f41762a1b1efb8444201e176cb97eef97146af26ac1394955ac79899dd8a400", 16),//fib(768)
                    new BigInteger("1a3136a1eda6f92aab467f1559e4bd5d01e6fb50c3d75c43134fd392d770b31feae688f6310bed0e1ccd6b456e1fff4e7ad34404c43cc4d78464a8b52485a108965201", 16),//fib(769)
                    new BigInteger("2a614360d887233167b73b4141dc0215edb8b9af25b53d520a82f9db6ed17fec0ba49235728217293bc8ef898e3e16bb3452339bd8a7b742459df20ad0ff2aa66ef601", 16),//fib(770)
                    new BigInteger("44927a02c62e1c5c12fdba569bc0bf72ef9fb4ffe98c99951dd2cd6e4642330bf68b1b2ba38e043758965acefc5e1609af2577a09ce47c19ca029abff584cbaf054802", 16),//fib(771)
                    new BigInteger("6ef3bd639eb53f8d7ab4f597dd9cc188dd586eaf0f41d6e72855c749b513b2f8022fad6116101b60945f4a588a9c2cc4e377ab3c758c335c0fa08ccac683f655743e03", 16),//fib(772)
                    new BigInteger("b386376664e35be98db2afee795d80fbccf823aef8ce707c462894b7fb55e603f8bac88cb99e1f97ecf5a52786fa42ce929d22dd1270af75d9a3278abc08c204798605", 16),//fib(773)
                    new BigInteger("12279f4ca03989b770867a58656fa4284aa50925e081047636e7e5c01b06998fbfaea75edcfae3af88154ef8011966f937614ce1987fce2d1e943b455828cb859edc408", 16),//fib(774)
                    new BigInteger("1d6002c30687bf760961a5574d057c3807748b60d00deb7dfb4a6f0b9abbf7efff3a53e7a894c5a906e4a94a79890b26208b1f0f69a6d9247c2e6dbe03e957a5e674a0d", 16),//fib(775)
                    new BigInteger("2f87a20fa6c1492d79e81fafb275206052199486b08eeff4323254cbb5c2917fbee8fb46858fa9588ef9f8427aa2721f57ec6bf10226a7519ac2a9035c12232b8550e15", 16),//fib(776)
                    new BigInteger("4ce7a4d2ad4908a38349c506ff7a9c98598e1fe7809cdb722d7cc3d7507e896fbe234f2e2e246f0195dea18cf42b7d4578778b006bcd807616f116c15ffb7ad16bc5822", 16),//fib(777)
                    new BigInteger("7c6f46e2540a51d0fd31e4b6b1efbcf8aba7b46e312bcb665faf18a306411aef7d0c4a74b3b4185a24d899cf6ecdef64d063f6f16df427c7b1b3bfc4bc0d9dfcf116637", 16),//fib(778)
                    new BigInteger("c956ebb501535a74807ba9bdb16a59910535d455b1c8a6d88d2bdc7a56bfa45f3b2f99a2e1d8875bbab73b5c62f96caa48db81f1d9c1a83dc8a4d6861c0918ce5cdbe59", 16),//fib(779)
                    new BigInteger("145c63297555dac457dad8e74635a1689b0dd88c3e2f4723eecdaf51d5d00bf4eb83be417958c9fb5df8fd52bd1c75c0f193f78e347b5d0057a58964ad816b6cb4df2490", 16),//fib(780)
                    new BigInteger("20f1d1e4c56b106b9fe29383214c4701ab6135d1994bd19177a06d197b3c063adf36b7dba776527119a47108834c0c8b9621afad52177784342fd6cd0f41fcf99aace2e9", 16),//fib(781)
                    new BigInteger("354e350e3ac0eb2ff7bd6c6a6781e86a466f0e5dd77b18b5666e1c6b510c122fcaba761d20cf1c6c779d6e5b4068824c87b5a73b8692d4848bd56031bcc368664f8c0779", 16),//fib(782)
                    new BigInteger("564006f3002bfb9b979fffed88ce2f6bf1d0442f70c6ea46de0e8984cc48186aa9f12df8c8456edd9141df63c3b48ed81dd756e8d8aa4c08c00536fecc05655fea38ea62", 16),//fib(783)
                    new BigInteger("8b8e3c013aece6cb8f5d6c57f05017d6383f528d484202fc447ca5f01d542a9a74aba415e9148b4a08df4dbf041d1124a58cfe245f3d208d4bda973088c8cdc639c4f1db", 16),//fib(784)
                    new BigInteger("e1ce42f43b18e26726fd6c45791e47422a0f96bcb908ed43228b2f74e99c43051e9cd20eb159fa279a212d22c7d19ffcc364550d37e76c960bdfce2f54ce332623fddc3d", 16),//fib(785)
                    new BigInteger("16d5c7ef57605c932b65ad89d696e5f18624ee94a014af03f6707d56506f06d9f934876249a6e8571a3007ae1cbeeb12168f1533197248d2357ba655fdd9700ec5dc2ce18", 16),//fib(786)
                    new BigInteger("24f2ac1e9b11eab99dd5844e2e28ca65a8c5e8006ba53dd82899304d9f08cb0a4b1e548334bc87f993d21a80493c0511e2c55a83ecf0bf9b9639a338f3265341281c0aa55", 16),//fib(787)
                    new BigInteger("3bc8740df272474cc93b31d804bfb0572eead6950bb9ecdc1f09ada3ef77d1e44452dbe57e637050ae02222e65faf023f9546fb70663086dcbb5498ef0ffc34fedf83786d", 16),//fib(788)
                    new BigInteger("60bb202c8d8432066710b62632e87abcd7b0be95775f2ab447a2ddf18e809cee8f713068b31ff84a41d43caeaf36f535dc19ca3af353c80961eeecc7e42616911614422c2", 16),//fib(789)
                    new BigInteger("9c83943a7ff67953304be7fe37a82b14069b952a8319179066ac8b957df86ed2d3c40c4e3183689aefd65edd1531e559d56e39f1f9b6d0772da43656d525d9e1040c79b2f", 16),//fib(790)
                    new BigInteger("fd3eb4670d7aab59975c9e246a90a5d0de4c53bffa784244ae4f69870c790bc163353cb6e4a360e531aa9b8bc468da8fb188042ced0a98808f93231eb94bf0721a20bbdf1", 16),//fib(791)
                    new BigInteger("199c248a18d7124acc7a88622a238d0e4e4e7e8ea7d9159d514fbf51c8a717a9436f949051626c9802180fa68d99abfe986f63e1ee6c168f7bd3759758e71ca531e2d35920", 16),//fib(792)
                    new BigInteger("29700fd089aebd0065f0524470cc976b5c3343caa78099c19c34b5ea396ea86559a2e85bbfaca2a65532b95f49e039a79387e424bd3cc01784cca7c9447bdbac5384df1711", 16),//fib(793)
                    new BigInteger("430c345aa285cf4b326adaa69af02479aa81c2594f59af5eed84753c0215c00e9d127cec110f0f3e574ac905d779e5a62bf74806aba8d6a700a01d609d62f8518567b27031", 16),//fib(794)
                    new BigInteger("6c7c442b2c348c4b985b2ceb0bbcbbe506b50623f6da492089b92b263b846873f6b56547d0bbb1e4ac7d8265215a1f4dbf7f2c2b68e596be856cc529e1ded3fdd8ec918742", 16),//fib(795)
                    new BigInteger("af887885ceba5b96cac60791a6ace05eb136c87d4633f87f773da0623d9a288293c7e233e1cac12303c84b6af8d404f3eb767432148e6d65860ce28a7f41cc4f5e5443f773", 16),//fib(796)
                    new BigInteger("11c04bcb0faeee7e26321347cb2699c43b7ebcea13d0e41a000f6cb88791e90f68a7d477bb2867307b045cdd01a2e2441aaf5a05d7d7404240b79a7b46120a04d3740d57eb5", 16),//fib(797)
                    new BigInteger("1cb8d3536c9a943792de73c0e59167ca26922971e83423a1f78346beab6b8b9791e4529af9451342ab40e193b13022935966c148f9202718991868a3ee0626c9c9595197628", 16),//fib(798)
                    new BigInteger("2e791f1e7c4982b5b9108708b0b8018e6210e65bfc0507bbf792b37732fd74a6fa8c2712b46d7a7326453e70b2d304d774161b4ed0f7675ad9d0031f341830ce9ccd5eef4dd", 16),//fib(799)
                    new BigInteger("4b31f271e8e416ed4beefac99649695888a30fcde4392b5def15fa35de69003e8c7079adadb28db5d18620046403276acd7cdc97ca178e7372e86bc3221e57986626b086b05", 16),//fib(800)
                    new BigInteger("79ab1190652d99a304ff81d247016ae6eab3f629e03e3319e6a8adad116674e586fca0c062200828f7cb5e7516d62c424192f7e69b0ef5ce4cb86ee25636886702f40f75fe2", 16),//fib(801)
                    new BigInteger("c4dd04024e11b09050ee7c9bdd4ad43f735705f7c4775e77d5bea7e2efcf7524136d1a6e0fd295dec9517e797ad953ad0f0fd47e65268441bfa0daa57854dfff691abffcae7", 16),//fib(802)
                    new BigInteger("13e881592b33f4a3355edfe6e244c3f265e0afc21a4b59191bc6755900135ea099a69bb2e71f29e07c11cdcee91af7fef50a2cc6500357a100c594987ce8b68666c0ecf72ac9", 16),//fib(803)
                    new BigInteger("2036519950150fac3a6dc7b0a01971365d1620219692cf0099225fd72f1055f2dadd6d59c81c533e68a6e5b680c88d39c5fb2a0e3655bfe51cbfa242d46e04865d5298f6f5b0", 16),//fib(804)
                    new BigInteger("341ed2f27b49044f6fcca797825e3528c2f6cfe3b0de2819b4e8d5302f23b4937484090caf3b7d1ee4b8b38569e38538bb0556d4865917861d8536db5156bb0cc41385ee2079", 16),//fib(805)
                    new BigInteger("5455248bcb5e13fbaa3a6f482277a65f200cf0054770f71a4e0b35075e340a864f6176667757d05d4d5f993beaac1272810080e2bcaed76b3a44d91e25c4bf9321661ee51629", 16),//fib(806)
                    new BigInteger("8873f77e46a7184b1a0716dfa4d5db87e303bfe8f84f1f3402f40a378d57bf19c3e57f7326934d7c32184cc1548f97ab3c05d7b74307eef157ca0ff9771b7a9fe579a4d336a2", 16),//fib(807)
                    new BigInteger("dcc91c0a12052c46c4418627c74d81e70310afee3fc0164e50ff3f3eeb8bc9a01346f5d99deb1dd97f77e5fd3f3baa1dbd065899ffb6c65c920ee9179ce03a3306dfc3b84ccb", 16),//fib(808)
                    new BigInteger("1653d138858ac4491de489d076c235d6ee6146fd7380f358253f3497678e388b9d72c754cc47e6b55b19032be93cb41c8f90c305142beb54de9d8f91113fbb4d2ec59688b836d", 16),//fib(809)
                    new BigInteger("242062f926ab170d8a28a232f3370df55e9251fc577cf4bd0a4f288b5646f5259ea736b266269892f310818bbd306ebe6b61288eb42757baa7be7e228b0dbef05f3392c43d038", 16),//fib(810)
                    new BigInteger("3a743431ac35db56a80d2c0369f943cc4cf398f9cafde8152f8e5d22bdd52db13c19fe07326e7f484e2984b7a66d22dafaf1eb93c853430f865c0db39c4d7a3d8df9294cf53a5", 16),//fib(811)
                    new BigInteger("5e94972ad2e0f2643235ce365d3051c1ab85eaf6227adcd239dd85ae141c22d6dac134b9989517db413a0643639d9199665314227c7a9aca2e1a8bd6275b392ded2cbc11323dd", 16),//fib(812)
                    new BigInteger("9908cb5c7f16cdbada42fa39c729958df87983efed78c4e7696be2d0d1f1508816db32c0cb0397238f638afb0a0ab4746144ffb644cdddd9b4769989c3a8b36b7b25e55e27782", 16),//fib(813)
                    new BigInteger("f79d628751f7c01f0c78c8702459e74fa3ff6ee60ff3a1b9a349687ee60d735ef19c677a6398aefed09d913e6da8460dc79813d8c14878a3e291255feb03ec996852a16f59b5f", 16),//fib(814)
                    new BigInteger("190a62de3d10e8dd9e6bbc2a9eb837cdd9c78f2d5fd6c66a10cb54b4fb7fec3e708779a3b2e9c462260011c3977b2fa8228dd138f0616567d9707bee9aeaca004e37886cd812e1", 16),//fib(815)
                    new BigInteger("28843906b23064df8f3348b1a0fdd642d407861bc0d60085aaffeb3ce9e0c3745fa1401b59234f521309ead77e55b408ff0752767c75ecf217998e44999b08c9e4bcb283cdae40", 16),//fib(816)
                    new BigInteger("418e9be4ef414dbd2d9f04dc3fb60e10adcf154920acc6efbbcb3ff1e560afb2d028b9bf0c0d13b43909fc9b15d0e3b1219523af6cd75259f10a0a333485d2ca32f43af0a5c121", 16),//fib(817)
                    new BigInteger("6a12d4eba171b29cbcd24d8de0b3e45381d69b64e182c77566cb2b2ecf4173272fc9f9da653063064c13e772942697ba209c7625e94d3f4c08a39877ce20db9417b0ed74736f61", 16),//fib(818)
                    new BigInteger("aba170d090b30059ea71526a2069f2642fa5b0ae022f8e6522966b20b4a222d9fff2b399713d76ba851de40da9f77b6b423199d5562491a5f9ada2ab02a6ae5e4aa52865193082", 16),//fib(819)
                    new BigInteger("115b445bc3224b2f6a7439ff8011dd6b7b17c4c12e3b255da8961964f83e396012fbcad73d66dd9c0d131cb803e1e132562ce0ffb3f71d0f202513b22d0c789f2625615d98c9fe3", 16),//fib(820)
                    new BigInteger("1c155b68cc2d7b35091b4f2622187c91be121fcc0e5e1e43fabf801703885b8db2faf610d47ab507b564faf8de8158e90a4ffa9d095966297fbfeddcdd36e3850acfb3e3ea5d065", 16),//fib(821)
                    new BigInteger("2d709fc48f4fc664738f8925a22a59fd3929e48d3c9943a1a355997bfbc694edc5f6c0e811e192a3c27817b0e2633a1b607cdb9cbd5083389fe5018f0a435c2430f515418327048", 16),//fib(822)
                    new BigInteger("4985fb2d5b7d41997caad84bc442d68ef73c04594af761e59e151992ff4ef07b78f1b6f8e65c47ab77dd12a9c0e493046accd639c6a9e9621fa4ef6be77a3fa93bc4c9256d840ad", 16),//fib(823)
                    new BigInteger("76f69af1eacd07fdf03a6171666d308c3065e8e68790a587416ab30efb1585693ee877e0f83dda4f3a552a5aa347cd1fcb49b1d683fa6c9abf89f0faf1bd9bcd6cb9de66f0ab0f5", 16),//fib(824)
                    new BigInteger("c07c961f464a49976ce539bd2ab0071b27a1ed3fd288076cdf7fcca1fa6475e4b7da2ed9de9a21fab2323d04642c6024361688104aa455fcdf2ee066d937db76a87ea78c5e2f1a2", 16),//fib(825)
                    new BigInteger("137733111311751955d1f9b2e911d37a75807d6265a18acf420ea7fb0f579fb4df6c2a6bad6d7fc49ec87675f07742d44016039e6ce9ec2979eb8d161caf57744153885f34eda297", 16),//fib(826)
                    new BigInteger("1f7efc73077619b2cca04d4ebbbcd3ec27fa9c3662ca0b461006a4c52efde7132ae9cd594b5721e449eb9a4636ba08d683776c1f7194318947de7b1c8a42d52babdb72d7fad09439", 16),//fib(827)
                    new BigInteger("32f62f841a878ecc22724701a4cea7669d7b1998c86b961552154cc03e5586c80a55f7c4f8c4a1a8e8b410bc27314baac38d6fbdde7e1db2c1ca0832a6f22c9fed2efb372fbe36d0", 16),//fib(828)
                    new BigInteger("52752bf721fda87eef129450608b7b52c575b5cf2b35a15b621bf1856d536ddb353fc51e441bc38d329fab025deb54814704dbdd50124f3c09a8834f313501cb990a6e0f2a8ecb09", 16),//fib(829)
                    new BigInteger("856b5b7b3c85374b1184db52055a22b962f0cf67f3a13770b4313e45aba8f4a33f95bce33ce065361b53bbbe851ca02c0a924b9b2e906ceecb728b81d8272e6b863969465a4d01d9", 16),//fib(830)
                    new BigInteger("d7e087725e82dfca00976fa265e59e0c286685371ed6d8cc164d2fcb18fc627e74d5820180fc28c34df366c0e307f4ad519727787ea2bc2ad51b0ed1095c30371f43d75584dbcce2", 16),//fib(831)
                    new BigInteger("15d4be2ed9b081715121c4af46b3fc0c58b57549f1278103cca7e6e10c4a55721b46b3ee4bddc8df96947227f682494d95c297313ad332919a08d9a52e1835ea2a57d409bdf28cebb", 16),//fib(832)
                    new BigInteger("2352c6a5ff98af6df12b3ba96d1255ed1b3bdd9d6314ee908e0cb9ddbdda1b9a02940c0e63ed8b6bcb73a89404b2c8986adc09a8c2bd5e54475a8a923eadf8ed9c4c117f164049b9d", 16),//fib(833)
                    new BigInteger("392784d4d94930df424d0058b3c651f973f152e7543c6f945ab4a0beca24710c1ddabffcafcb544b62081abbfb3511e6009ea0d9fd9090e5e16364376cc62ed7c6a3e588d432d6a58", 16),//fib(834)
                    new BigInteger("5c7a4b7ad8e1e04d33783c0220d8a7e68f2d3084b7515e24e8c15a9c87fe8ca6206ecc0b13b8dfb72d7bc34fffe7da7e6b7aaa82c04def3a28bdeec9ab7427c562eff707ea73205f5", 16),//fib(835)
                    new BigInteger("95a1d04fb22b112c75c53c5ad49ef9e0031e836c0b8dcdb94375fb5b5222fdb23e498c07c38434028f83de0bfb1cec646c194b5cbdde80200a215301183a569d2993dc90bea5f704d", 16),//fib(836)
                    new BigInteger("f21c1bca8b0cf179a93d785cf577a1c6924bb3f0c2df2bde2c3755f7da218a585eb85812d73d13b9bcffa15bfb04c6e2d793f5df7e2c6f5a32df41cac3ae7e628c83d398a91917642", 16),//fib(837)
                    new BigInteger("187bdec1a3d3802a61f02b4b7ca169ba6956a375cce6cf9976fad51532c44880a9d01e41a9ac147bc4c837f67f621b34743ad413c3c0aef7a3d0094cbdbe8d4ffb617b02967bf0e68f", 16),//fib(838)
                    new BigInteger("279da07e4c844f41fc8402d14bf8e3d6d27b5eb4d914c25759be4a74b06661262fbba3c2d71fe5b76098320c3f1267a2a1b41371bba375ed46fdfd6969f975362429b83c210d825cd1", 16),//fib(839)
                    new BigInteger("40197f3ff057cf6c5e742e1cc89a4d913bd2022aa5fb91f0d0b91f89e32aa9a6d98bc20480cbfa3325606a02be7482d715eee7857f6424e4eace06b627b802861f8b333eb789734360", 16),//fib(840)
                    new BigInteger("67b71fbe3cdc1eae5af830ee149331680e4d60df7f1054482a7769fe93910acd094765c757ebdfea85f89c0efd86ea79b7a2faf73b079ad231cc041f91b177bc43b4eb7ad896f5a031", 16),//fib(841)
                    new BigInteger("a7d09efe2d33ee1ab96c5f0add2d7ef94a1f630a250be638fb30898876bbb473e2d327cbd8b7da1dab590611bbfb6d50cd91e27cba6bbfb71c9a0ad5b9697a4263401eb9902068e391", 16),//fib(842)
                    new BigInteger("10f87bebc6a100cc914648ff8f1c0b061586cc3e9a41c3a8125a7f3870a4cbf40ec1a8d9330a3ba083151a220b98257ca8534dd73f5735a894e660ef54b1af1fea6f50a3468b75e83c2", 16),//fib(843)
                    new BigInteger("1b7585dba9743fae3cdd0ef03ceee2f5aa28c26f3c92820ba20d87d0f810873b4ceedb55f095b9425dcaaa832757dc51b52c6bff0afdf1a406b0019cb04846c410a3528edf8d7c76753", 16),//fib(844)
                    new BigInteger("2c6e01c77015407ace2357efcc0aedfbbfaf8eadd6d445b3b468070968b5532f5bb0842f239ff4e2e0dfc4a532f001ce5d7fb9d64a55274c9b96628c04f9f5e3fb12a3322618f25eb15", 16),//fib(845)
                    new BigInteger("47e387a3198980290b0066e008f9d0f169d8511d1366c7bf56758eda60c5da6aa89f5f851435ae253eaa6f285a47de2012ac25d5555318f0a2466428b5423ca80bb5f5c105a66ed5268", 16),//fib(846)
                    new BigInteger("7451896a899ec0a3d923becfd504beed2987dfcaea3b0d730add95e3c97b2d9a044fe3b437d5a3081f8a33cd8d37dfee702bdfab9fa8403d3ddcc6b4ba3c328c06c898f32bbf6133d7d", 16),//fib(847)
                    new BigInteger("bc35110da32840cce42425afddfe8fde936030e7fda1d532615324be2a410804acef43394c0b512d5e34a2f5e77fbe0e82d80580f4fb592de0232add6f7e6f34127e8eb43165d008fe5", 16),//fib(848)
                    new BigInteger("130869a782cc70170bd47e47fb3034ecbbce810b2e7dce2a56c30baa1f3bc359eb13f26ed83e0f4357dbed6c374b79dfcf303e52c94a3996b1dfff19229baa1c0194727a75d25313cd62", 16),//fib(849)
                    new BigInteger("1ecbbab85cfef423da16c0a2f9101deaa5048419ae57eb7d7cd83df601dfd3da35e2e6a26cfec4562dbf379b95c375c0b75dbeaad899ef298fe231c6f993910f42bc5b65b8e8b0145d47", 16),//fib(850)
                    new BigInteger("31d4245fdfcb643ae5eb3eeaf44052d760d30524dcd5b9a7d39b49a0211b973420f6d911453cd399859b2507cd0eefa0868dfcfda1e428c041c230e01c2f3b2b4450cde02ebb03282aa9", 16),//fib(851)
                    new BigInteger("509fdf183cca585ec001ff8ded5070c205d7893e8b2da5255073879622fb6b0e56d9bfb3b23b97efb35a5ca362d265613debbba87a7e17e9d1a462a715c2cc3a870d2945e7a3b33c87f0", 16),//fib(852)
                    new BigInteger("827403781c95bc99a5ed3e78e190c39966aa8e6368035ecd240ed1364417024277d098c4f7786b8938f581ab2fe15501c479b8a61c6240aa1366938731f20765cb5df726165eb664b299", 16),//fib(853)
                    new BigInteger("d313e290596014f865ef3e06cee1345b6c8217a1f33103f2748258cc67126d50ceaa5878a9b40378ec4fde4e92b3ba630265744e96e05893e50af62e47b4d3a0526b206bfe0269a13a89", 16),//fib(854)
                    new BigInteger("15587e60875f5d1920bdc7c7fb071f7f4d32ca6055b3462bf98912a02ab296f93467af13da12c6f0225455ff9c2950f64c6df2cf4b342993df87189b579a6db061dc9179214612005ed22", 16),//fib(855)
                    new BigInteger("2289bc898cf55e68a71cbba867f532c503faebda74e6566b20d1382cf123bdce4152549b64ae0727b11953e485548c9c7c944a1434a22f1d1dd7c7fe3c15baea6703437fe126389a727ab", 16),//fib(856)
                    new BigInteger("37e23aea1454bb81c7da837062fc5244512db63aca999c971a5a4acd1bd654c775ba03af3ec0ce17d36da9e4217ddd92c9023ce37fd658b0fd5ee09993b0289ac8dfd4f9026c4a9ad14cd", 16),//fib(857)
                    new BigInteger("5a6bf773a14a19ea6ef73f18caf185095528a2153f7ff3023b2b82fa0cfa1295b70c584aa36ed53f8486fdc8a6d26a2f459686f7b47887ce1b36a897cfc5e3852fe31878e392833543c78", 16),//fib(858)
                    new BigInteger("924e325db59ed56c36d1c2892dedd74da65658500a198f995585cdc728d0675d2cc65bf9e22fa35757f4a7acc85047c20e98c3db344ee07f1895893163760c1ff8c2ed71e5fecdd015145", 16),//fib(859)
                    new BigInteger("ecba29d156e8ef56a5c901a1f8df5c56fb7efa654999829b90b150c135ca79f2e3d2b444859e7896dc7ba5756f22b1f1542f4ad2e8c7684d33cc31c9333befa528a605eac991510558dbd", 16),//fib(860)
                    new BigInteger("17f085c2f0c87c4c2dc9ac42b26cd33a4a1d552b553b31234e6371e885e9ae1501099103e67ce1bee34704d223772f9b362c80eae1d1648cc4c61bafa96b1fbc52168f35caf901ed56df02", 16),//fib(861)
                    new BigInteger("26bc286006370b4198263c5cd1fac8ffb9d544d1a9d4c94d076e86f4994655b42f46bc482ed6c948510ebf297a695aba4b6f7598105ddb119802decc3c9edeb6a4a0ef94779216fdac6cbf", 16),//fib(862)
                    new BigInteger("3eacae22f6ff878dc5efe89f84679c3a03f299fcff0ffa7055d1f8dd1f3003c930504d4c1553ab073455c3fb9de08a55819bf682f22f3f9e5cc8fa7be609fe72f6b77eca428b18eb034bc1", 16),//fib(863)
                    new BigInteger("6568d682fd3692cf5e1624fc56626539bdc7decea8e4c3bd5d407fd1b876597d5f970994442a744f856483251849e50fcd0b6c1b028d1aaff4cbd94822a8dd299b586e5eba1d2fe8afb880", 16),//fib(864)
                    new BigInteger("a41584a5f4361a5d24060d9bdaca0173c1ba78cba7f4be2db31278aed7a65d468fe756e0597e1f56b9ba4720b62a6f654ea7629df4bc5a4e5194d3c408b2db9c920fed28fca848d3b30441", 16),//fib(865)
                    new BigInteger("1097e5b28f16cad2c821c3298312c66ad7f82579a50d981eb1052f880901cb6c3ef7e60749da893a63f1eca45ce7454751bb2ceb8f74974fe4660ad0c2b5bb8c62d685b87b6c578bc62bcc1", 16),//fib(866)
                    new BigInteger("1ad93dfcee5a2c789a62240340bf66821413cd065f8ce4018c365712f67c3140a7f65b754f726b2fcf8d91166849ec3da6a5a3156ec05cf4c97f580d0340e9462bf7848b0b36dc19015c102", 16),//fib(867)
                    new BigInteger("2b7123af7d70f74b6283e72cc3d22cecec0bf280049a7c203d3b869aff7dfcace6ee417c994cf46a337f7dbac5313184f860d000fe34f444ade562ddc5f6a4d28ece0a4386a333a4c787dc3", 16),//fib(868)
                    new BigInteger("464a61ac6bcb23c3fce60b300491936f001fbf8664276021c971ddadf5fa2ded8ee49cf1e8bf5f9a030d0ed12d7b1dc29f0673166cf551397764baeac9378e18bac58ece91da0fbdc8e3ec5", 16),//fib(869)
                    new BigInteger("71bb855be93c1b0f5f69f25cc863c05bec2bb20668c1dc4206ad6448f5782a9a75d2de6e820c5404368c8c8bf2ac4f47976743176b2a457e254a1dc88f2e32eb49939912187d4362906bc88", 16),//fib(870)
                    new BigInteger("b805e70855073ed35c4ffd8cccf553caec4b718ccce93c63d01f41f6eb72588804b77b606acbb39e39999b5d20276d0a366db62dd81f96b79caed8b35865c104045927e0aa575320594fb4d", 16),//fib(871)
                    new BigInteger("129c16c643e4359e2bbb9efe995591426d877239335ab18a5d6cca63fe0ea83227a8a59ceecd807a2702627e912d3bc51cdd4f9454349dc35c1f8f67be793f3ef4decc0f2c2d49682e9bb7d5", 16),//fib(872)
                    new BigInteger("1e1c7536c934a98b61809ed76624e67f1c4c2952002945509a6ebe836cc5cdbaa7f41d52f57a3bb40a9bfc34632fb295c0442af731b6972ed5ea7cf2f3ff9b4f35245e8d36d2be9a3430b322", 16),//fib(873)
                    new BigInteger("30b88bfd0d18df298d3c3dd5ff7a77c189d39b8b3383f6daf7db88e76ad475eccf9cc2efe447bc2e319e5eb2f45cee5add217a8b85eb34f2320a0c5ab278da8e2a032a9c6300080262cc6af7", 16),//fib(874)
                    new BigInteger("4ed50133d64d88b4eebcdcad659f5e40a61fc4dd33ad3c2b924a476ad79a43a77790e042d9c1f7e23c3a5ae7578ca0f09d65a582b7a1cc2107f4894da67875dd5f27892999d2c69c96fd1e19", 16),//fib(875)
                    new BigInteger("7f8d8d30e36667de7bf91a836519d6022ff36068673133068a25d052426eb994472da332be09b4106dd8b99a4be98f4b7a87200e3d8d011339fe95a858f1506b892ab3c5fcd2ce9ef9c98910", 16),//fib(876)
                    new BigInteger("ce628e64b9b3f0936ab5f730cab93442d61325459ade6f321c7017bd1a08fd3bbebe837597cbabf2aa131481a376303c17ecc590f52ecd3441f31ef5ff69c648e8523cef96a5953b90c6a729", 16),//fib(877)
                    new BigInteger("14df01b959d1a5871e6af11b42fd30a45060685ae020fa238a695e80f5c77b6d005ec26a855d5600317ebce1bef5fbf879273e59f32bbce477bf1b49e585b16b4717cf0b5937863da8a903039", 16),//fib(878)
                    new BigInteger("21c52a9fa56ce4905516508e4fa8c3e87dc19aaf39cee116ac305ffcc7680b40bc4aaaa1deda10bf5c1fee29d92d5efc3aa60ab3027ea9b7bbde4d39457c4dcfd59cf2da52a1df9161b56d762", 16),//fib(879)
                    new BigInteger("36a42c58ff3e8a17738141a992a5f48cce22030a19efdb3a3699be7dbd2f86adbca96d0c643766bf8d9eab0b98235af4b3cd490cf5aa669c339d68832b01ff3b1cb4c1e5abd965cf0a5e7079b", 16),//fib(880)
                    new BigInteger("586956f8a4ab6ea7c8979237e24eb8754be39db953bebc50e2ca1e7a849791ee78f417ae4311777ee9be99357150b9f0ee7353bff8291053ef7bb5bc707e4d0af251b4bffe7b45606c13ddefd", 16),//fib(881)
                    new BigInteger("8f0d8351a3e9f8bf3c18d3e174f4ad021a05a0c36dae978b1963dcf841c7189c359d84baa748de3e775d4441097414e5a2409cccedd376f023191e3f9b804c460f0676a5aa54ab2f76724e698", 16),//fib(882)
                    new BigInteger("e776da4a4895676704b066195743657765e93e7cc16d53dbfc2dfb72c65eaa8aae919c68ea5a55bd611bdd767ac4ced690b3f08ce5fc87441294d3fc0bfe995101582b65a8cff08fe2862c595", 16),//fib(883)
                    new BigInteger("176845d9bec7f602640c939facc3812797feedf402f1beb671591d86b0825c326e42f212391a333fbd87921b78438e3bc32f48d59d3cffe3435adf23ba77ee597105ea20b53249bbf58f87ac2d", 16),//fib(884)
                    new BigInteger("25dfb37e63514c78d4579a014237b77f0e5d81dbcf0893f4311bfd3ddce846db192c0bd8c7bfd89b93994ff2dfefdb292c3a87de6b9cc85784842c637b37d7ee811b6cd70fbf48c4f3b7ea71c2", 16),//fib(885)
                    new BigInteger("3d47f9582219427b38642da0eefb38a6a65c6fcfd1fa52aaa2751ac48d6aa30d876efdeb00da0bdb5120e20e58336964ef69d0b408d9c83ac7df0b8735afc647f22156f7c4f19280e947721def", 16),//fib(886)
                    new BigInteger("6327acd6856a8ef40cbbc7a23132f025b4b9f1aba102e69ed39118026a52e9e8a09b09c3c899e476e4ba32013823448e1ba45892747690924c6337eab0e79e36733cc3ced4b0db45dcff5c8fb1", 16),//fib(887)
                    new BigInteger("a06fa62ea783d16f451ff543202e28cc5b16617b72fd3949760632c6f7bd8cf6280a07aec973f05235db140f9056adf30b0e29467d5058cd14424371e697647e655e1ac699a26dc6c646ceada0", 16),//fib(888)
                    new BigInteger("1039753052cee606351dbbce5516118f20fd0532714001fe849974ac9621076dec8a51172920dd4c91a954610c879f28126b281d8f1c6e95f60a57b5c977f02b4d89ade956e53490ca3462b3d51", 16),//fib(889)
                    new BigInteger("1a406f933d47231d296fbb228718f41be6ae6b4a286fd5931bf9d7d9059ce03d4f0af19215b81c51b50705a2058d0a07431c0ab1f6f17422c74e7bece7e1667333df8f95c07f5b6d3698cf9eaf1", 16),//fib(890)
                    new BigInteger("2a79e4c3901609235e8d76f0dc2f05ab07ab707c99afd791a0934c859bbde7ab3b9542a93ed8f99e46b05a031214a92f558732cf860de2b8bd58d3a2b159569e81693d7f17648ffe00cd3252842", 16),//fib(891)
                    new BigInteger("44ba5456cd5d2c4087fd32136347f9c6ee59dbc6c21fad24bc8d245ea15ac7e88aa0343b549115effbb75fa517a1b33698a33d817cff56db84a74f8f993abd11b548cd14d7e3eb6b376601f1333", 16),//fib(892)
                    new BigInteger("6f34391a5d733563e68aa9043f76ff71f6054c435bcf84b65d2070e43d18af93c63576e4936a0f8e4267b9a829b65c65ee2a7051030d3994420023324a9413b036b20a93ef487b6938333443b75", 16),//fib(893)
                    new BigInteger("b3ee8d712ad061a46e87db17a2bef938e45f280a1def31db19ad9542de73777c50d5ab1fe7fb257e3e1f194d41580f9c86cdadd2800c906fc6a772c1e3ced0c1ebfad7a8c72c66d46f993634ea8", 16),//fib(894)
                    new BigInteger("12322c68b884397085512841be235f8aada64744d79beb69176ce06271b8c2710170b22047b65350c8086d2f56b0e6c0274f81e238319ca0408a795f42e62e47222ace23cb674e23da7cc6a78a1d", 16),//fib(895)
                    new BigInteger("1d71153fcb313f8acc39a5f3384f4f1e3bec39c5797ade86c907b9b69f9ff9e8c67e0cd2463605a8abea5ec42ac667b9efbc5cbf603265a73cf4f08b61231b5340ea7b9e57da149121765a0ad8c5", 16),//fib(896)
                    new BigInteger("2fa341a883b578fb518ace34f672aea8e992810a5116c9efe0749a191158bc59c7eebef28dec58f973f2cbf381774e7a170bdea1986402477d7f69eaa409499a631549c2234162b4fbf320b262e2", 16),//fib(897)
                    new BigInteger("4d1456e84ee6b8861dc474282ec1fdc7257ebacfca91a876a97c53cfb0f8b6428e6ccbc4d4225ea21fdd2ab7ac3db63406c83b60f89667eeba745a76052c64eda3ffc5607b1b77461d697abd3ba7", 16),//fib(898)
                    new BigInteger("7cb79890d29c31816f4f425d2534ac700f113bda1ba8726689f0ede8c251729c565b8ab7620eb79b93cff6ab2db504ae1dd41a0290fa6a3637f3c460a935ae8807150f229e5cd9fb195c9b6f9e89", 16),//fib(899)
                    new BigInteger("c9cbef792182ea078d13b68553f6aa37348ff6a9e63a1add336d41b8734a28dee4c8567c3631163db3ad2162d9f2bae2249c55638990d224f2681ed6ae621375ab14d4831978514136c6162cda30", 16),//fib(900)
                    new BigInteger("146838809f41f1b88fc62f8e2792b56a743a1328401e28d43bd5e2fa1359b9b7b3b23e133983fcdd9477d180e07a7bf9042706f661a8b3c5b2a5be3375797c1fdb229e3a5b7d52b3c5022b19c78b9", 16),//fib(901)
                    new BigInteger("2104f778315a205908976af67cd2200de7831292de81ca820f0cb7159a8e5c45a1fec37afce70e416fb2a3970e19a7a72670cc4c9a41c0e801cc4020e05f9d5735d3eb828d14d7c7d86e8c7c952e9", 16),//fib(902)
                    new BigInteger("356d2ff8d09c1211985d9a84a464d5785bbd25bb1e9ff3564ae29a0fade815fd55b1018e366b0b1f042a7517ee9423a02a97d342fbea74adb471fe5455d9197710f689bce8922a7b9d70b7965cba2", 16),//fib(903)
                    new BigInteger("5672277101f6326aa0f5057b2136f5864340384dfd21bdd859ef512548767242f7afc5093352196073dd18aefcadcb4751089f8f962c3595b63e3e753638b6ce46ca753f75a7024375df4412f1e8b", 16),//fib(904)
                    new BigInteger("8bdf5769d292447c39529fffc59bcafe9efd5e091bc1b12ea4d1eb34f65e88404d60c69769bd247f78078dc6eb41eee77ba072d29216aa436ab03cc98c11d04557c0fefc5e392cbf134ffba94ea2d", 16),//fib(905)
                    new BigInteger("e2517edad48876e6da47a57ae6d2c084e23d965718e36f06fec13c5a3ed4fa8345108ba09d0f3ddfebe4a675e7efba2ecca912622842dfd920ee7b3ec24a87139e8b743bd3e02f02892f3fbc408b8", 16),//fib(906)
                    new BigInteger("16e30d644a71abb63139a457aac6e8b83813af46034a52035a393278f353382c39271523806cc625f63ec343cd331a91648498534ba598a1c8b9eb8084e5c5758f64c733832195bc19c7f3b658f2e5", 16),//fib(907)
                    new BigInteger("25082551f7ba33249ede1eaf593414c0863788ab74d888f3ca25463e974087d46d781ddd8a3dba03f4fd0dab2bb21634514f29796e29c69f5ac8d334710a6de6c94d7e77405f98ac425ae7b21cfb9d", 16),//fib(908)
                    new BigInteger("3beb32b6422bdedad017c30703fafd78be4b37f17822daf7245e78b78a93c000a69f33010aaa8029eb3bd0eef8e530c5b5d3c1ccb9cf5f412382beb4f5f0335c58b245aac3812e685c22db6875ee82", 16),//fib(909)
                    new BigInteger("60f3580839e611ff6ef5e1b65d2f12394482c09cecfb63eaee83bef621d447d5141750de94e83a2de038de9a249746fa0722eb4627f925e07e4b91e966faa14321ffc42203e0c7149e7dc31a92ea1f", 16),//fib(910)
                    new BigInteger("9cde8abe7c11f0da3f0da4bd612a0fb202cdf88e651e3ee212e237adac6807d5bab683df9f92ba57cb74af891d7c77bfbcf6ad12e1c88521a1ce509e5cead49f7ab209ccc761f57cfaa09e8308d8a1", 16),//fib(911)
                    new BigInteger("fdd1e2c6b5f802d9ae038673be5921eb4750b92b5219a2cd0165f6a3ce3c4faacecdd4be347af485abad8e234213beb9c419985909c1ab022019e287c3e575e29cb1cdeecb42bc91991e619d9bc2c0", 16),//fib(912)
                    new BigInteger("19ab06d853209f3b3ed112b311f83319d4a1eb1b9b737e1af14482e517aa457808984589dd40daedd77223dac5f9036798110456beb8a3023c1e8332620d04a821763d7bb92a4b20e93bf0020a49b61", 16),//fib(913)
                    new BigInteger("29882504be801f68d9b14b1a4dddc5388916f6ae50951847c15ae24f548e0a72b58522d5c0888a36322cfcbcfa1a3f5334529ddc4f54bdb25e20215ade4b5c064b415a5aa5de76ea02cdd61be405e21", 16),//fib(914)
                    new BigInteger("43332bdd11a0bea418825dcd5fd5f8525db8e1c9ec089662b29f65346c384feabe1d685f9dc96524099f2097c01342bacc63a2330e0d60b49a3ea48d405860ae6cb797d65f08c20aec09c61dee4f982", 16),//fib(915)
                    new BigInteger("6cbb50e1d020de0cf233a8e7adb3bd8ae6cfd8783c9daeaa73fa4783c0c65a5d73a28b355e51ef5a3bcc1d54ba2d820e00b6400f5d621e66f85ec5e81ea3bcb4b7f8f23104e738f4eed79c39d2557a3", 16),//fib(916)
                    new BigInteger("afee7cbee1c19cb10ab606b50d89b5dd4488ba4228a6450d2699acb82cfeaa4831bff394fc1b547e456b3dec7a40c4c8cd19e2426b6f7f1b929d6a755efc1d6324b08a0763effaffdae16257c0a5125", 16),//fib(917)
                    new BigInteger("11ca9cda0b1e27abdfce9af9cbb3d73682b5892ba6543f3b79a93f43bedc504a5a5627eca5a6d43d881375b41346e46d6cdd02251c8d19d828afc305d7d9fda17dca97c3868d733f4c9b8fe9192fa8c8", 16),//fib(918)
                    new BigInteger("1cc984a5f93a4176f079fb651c8c729456fe14cfc8dea38c4c12da0f41ac3aeedd722725f56889856c6a2992daeaf0b9f9aea049434411c9e1d999ad2dc9bf77b015a063fccc72ef4a49a60e9539f9ed", 16),//fib(919)
                    new BigInteger("2e94218004586922d048965ee84049cad9b39dfb6f32e2c7c5bc195300888b3937c84f129b0f5dc2f47d9f46ee31d527668ba26e5fd12ba20a895cb305a3bd192de038278359e62e96e535f7ae69a2b5", 16),//fib(920)
                    new BigInteger("4b5da625fd92aa99c0c291c404ccbc5f30b1b2cb3811865411cef3624234c628153a76389077e74860e7c8d9c91cc5e1603a42b7a3153d6bec62f660336d7c90ddf5d88b8026591de12edc0643a39ca2", 16),//fib(921)
                    new BigInteger("79f1c7a601eb13bc910b2822ed0d062a0a6550c6a744691bd78b0cb542bd51614d02c54b2b87450b55656820b74e9b08c6c5e52602e6690df6ec5313391139aa0bd610b303803f4c781411fdf20d3f57", 16),//fib(922)
                    new BigInteger("c54f6dcbff7dbe5651cdb9e6f1d9c2893b170391df55ef6fe95a001784f21789623d3b83bbff2c53b64d30fa806b60ea270027dda5fba679e34f49736c7eb63ae9cbe93e83a6986a5942ee0435b0dbf9", 16),//fib(923)
                    new BigInteger("13f4135720168d212e2d8e209dee6c8b3457c5458869a588bc0e50cccc7af68eaaf4000cee786715f0bb2991b37b9fbf2edc60d03a8e20f87da3b9c86a58fefe4f5a1f9f18726d7b6d157000227be1b50", 16),//fib(924)
                    new BigInteger("20490a33e00e6906934a69bf0d0c08b3c809357ea65f047fbaa3f0ce44ca18074117d3c52a3859db2c1ffca15b8255cdd14c634e14eddb601bd8ae5fa120ea61fdf6de3300acd70212a99ee065d6ef749", 16),//fib(925)
                    new BigInteger("343d1d8b0024f627c177f7dfaafa753efc60fac42ec8aa0876b2419b11450e95ec0bd3d218b0c0f11cdb26330efdf58d0028c41e4f7bfc58997c68280b79e9604d50fdd2191f447d7fbf0ee08852d1299", 16),//fib(926)
                    new BigInteger("548627bee0335f2e54c2619eb8067df2c46a3042d527ae8831563269560f269d2d23a79742e91acc48fb22d46a804b5ad175276c6469d7b8b5551687ac9ad3c24b47dc0519cc1b7f9268adc0ee29c09e2", 16),//fib(927)
                    new BigInteger("88c34549e0585556163a597e6300f331c0cb2b0703f05890a808740467543533192f7b695b99dbbd65d64907797e40e7d19deb8ab3e5d4114ed17eafb814bd229898d9d732eb5ffd1227bca1767c91c7b", 16),//fib(928)
                    new BigInteger("dd496d08c08bb4846afcbb1d1b07712485355b49d9180718d95ea66dbd635bd0465323009e82f689aed16bdbe3fe8c42a31312f7184fabca0426953764af90e4e3e0b5dc4cb77b7ca4906a6264a65265d", 16),//fib(929)
                    new BigInteger("1660cb252a0e409da8137149b7e08645646008650dd085fa981671a7224b791035f829e69fa1cd24714a7b4e35d7ccd2a74b0fe81cc357fdb52f813e71cc44e077c798fb37fa2db79b6b82703db22e42d8", 16),//fib(930)
                    new BigInteger("243561f5b616fbe5eec33cfb8990fd57acb35e19ab62066c25ac5c0dfe21aecd3a5d5c16a989fc8d0c37920bf417b596d17c41178e4852ba5571ea91e8173deec605a458fcc5a56f65b4891663fc936935", 16),//fib(931)
                    new BigInteger("3a962d1ae0253c8396d6ae454171839d1113667eb9328c66bdc2cdb5206d27dd705585fd492bc9b17d820d5a29ef826978c750ffab0baab80aa16bd059e382cf3dcd3d5434bfd32701200b86a1aec1ac0d", 16),//fib(932)
                    new BigInteger("5ecb8f10963c38698599eb40cb0280f4bdc6c498649492d2e36f29c31e8ed6aaaab2e213f2b5c63e89b99f661e0738004a4392173953fd726013566241fac0be03d2e1ad3185789666d4949d05ab551542", 16),//fib(933)
                    new BigInteger("9961bc2b766174ed1c7099860c740491ceda2b171dc71f39a131f7783efbfe881b0868113be18ff0073bacc047f6ba69c30ae316e45fa82a6ab4c2329bde438d41a01f0166454bbd67f4a023a75a16c14f", 16),//fib(934)
                    new BigInteger("f82d4b3c0c9dad56a20a84c6d77685868ca0efaf825bb20c84a1213b5d8ad532c5bb4a252e97562e90f54c2665fdf26a0d4e752e1db3a59ccac81894ddd9044b457300ae97cac453cec934c0ad056bd691", 16),//fib(935)
                    new BigInteger("1918f076782ff2243be7b1e4ce3ea8a185b7b1ac6a022d14625d318b39c86d3bae0c3b2366a78e61e9830f8e6adf4acd3d059584502134dc7357cdac779b747d887131faffe10101136bdd4e4545f8297e0", 16),//fib(936)
                    new BigInteger("289bc52a38f9ccf9a6085a313bb610f9ee81c0a76227e8352aa7439eefa11a8eda67efc5b99103c4d2926450d13f29f3ddda7cd731fc6f3640044f35c57904c23cc86205e95dad465058709a50164ee6e71", 16),//fib(937)
                    new BigInteger("41b4b5a0b129bf1de1f00c1609f4b99b74397253cc2a15498d04752a296987ca88742ae920389226bc1573df3c1e74c11ae0125b821da412b35c1ce23d14793fc5399400e93eae4763c44de8955c4710651", 16),//fib(938)
                    new BigInteger("6a507acaea238c1787f8664745aaca9562bb32fb2e51fd7eb7abb8c9190aa25962dc1aaed9c995eb8ea7d8300d5d9eb4f8ba8f32b41a1348f3606c18028d7e020201f606d29c5b8db41cbe82e57295f74c2", 16),//fib(939)
                    new BigInteger("ac05306b9b4d4b3569e8725d4f9f8430d6f4a54efa7c12c844b02df342742a23eb504597fa0228124abd4c0f497c1376139aa18e3637b75ba6bc88fa3fa1f741c73b8a07bbdb09d517e10c6b7acedd07b13", 16),//fib(940)
                    new BigInteger("11655ab368570d74cf1e0d8a4954a4ec639afd84a28ce1046fc5be6bc5b7ecc7d4e2c6046d3cbbdfdd965243f56d9b22b0c5530c0ea51caa49a1cf512422f7543c93d800e8e776562cbfdcaee604172fefd5", 16),//fib(941)
                    new BigInteger("1c25adba220be22825bc94b01e4e9d2f710a47d99234a230f410c14af9df2f6a1397ca5decdcde6102422704ea055c5a11fefd24f2089820040d97e0c81d16c8590790a164a526f37e3ded759db105006ae8", 16),//fib(942)
                    new BigInteger("2d8b086d8a62ef9cf4daa23a67a3421bd4a5455e34c1833563d67fb6bf971c31e87a90625a199a40dfd87948df72f77cc2c4503100adb4ca4daf6731ec400e1c959b68a24d8c9d49aafdca2483b51c305abd", 16),//fib(943)
                    new BigInteger("49b0b627ac6ed1c51a9736ea85f1df4b45af8d37c6f6256657e74101b9764b9bfc125ac046f678a1e21aa04dc97853d6d4c34d55f2b64cea51bcff12b45d24e4eea2f943b231c43d293bb79a21662130c5a5", 16),//fib(944)
                    new BigInteger("773bbe9536d1c1620f71d924ed9521671a54d295fbb7a89bbbbdc0b8790d67cde48ceb22a11012e2c1f31996a8eb4b5397879d86f36401b49f6c6644a09d3301843e61e5ffbe6186d43981bea51b3d612062", 16),//fib(945)
                    new BigInteger("c0ec74bce34093272a09100f738700b260045fcdc2adce0213a501ba3283b369e09f45e2e8068b84a40db9e472639f2a6c4aeadce61a4e9ef129655754fa57e672e15b29b1f025c3fd753958c6815e91e607", 16),//fib(946)
                    new BigInteger("1382833521a125489397ae934611c22197a593263be65769dcf62c272ab911b37c52c310589169e676600d37b1b4eea7e03d28863d97e50539095cb9bf5978ae7f71fbd0fb1ae874ad1aebb176b9c9bf30669", 16),//fib(947)
                    new BigInteger("1f914a80efd52e7b06383f943d4a322cbda5d92318113449fe307c42cde14cea1a5cb76e8711d29ec0a0e8d5f8db289a8701d7340bf989ef281bf30f34a91e2ce6a011839639ead0ecf23f470321dfa84ec70", 16),//fib(948)
                    new BigInteger("3313cdb6117653c399cfee27835bf44e554b6c4953f78bb3db26a869f89a5e9d96af7a7edfa33c853700f60daa901742673effba49916ef461254fc8f40296db66120d549154d3459a0d2af879dba9677f2d9", 16),//fib(949)
                    new BigInteger("52a51837014b823ea0082dbbc0a6267b12f1456c6c08bffdd95724acc67bab87b10c31ed66b50f23f7a1dee3a36b3fdcee40d6ee558af8e3894142d828abb5084cb21ed8278ebe1686ff6a3f7cfd890fcdf49", 16),//fib(950)
                    new BigInteger("85b8e5ed12c1d60239d81be344021ac9683cb1b5c0004bb1b47dcd16bf160a2547bbac6c46584ba92ea2d4f14dfb571f557fd6a89f1c67d7ea6692a11cae4be3b2c42c2cb8e3915c210c9537f6d932774d222", 16),//fib(951)
                    new BigInteger("d85dfe24140d5840d9e0499f04a841447b2df7222c090baf8dd4f1c38591b5acf8c7de59ad0d5acd2644b3d4f16696fc43c0ad96f4a760bb73a7d579455a00ebff764b04e0724f72a80bff7773d6bb871b16b", 16),//fib(952)
                    new BigInteger("15e16e41126cf2e4313b8658248aa5c0de36aa8d7ec0957614252beda44a7bfd240838ac5f365a67654e788c63f61ee1b9940843f93c3c8935e0e681a62084ccfb23a77319955e0cec91894af6aafedfe6838d", 16),//fib(953)
                    new BigInteger("23674e2353adc8683ed98af214d529d525e989ffa18126310d027b09dca39757f394b691fa07301437b2c3c9b30c88517dd0131d6886b294ed1b63d93a7624dbbb1b0c23679c8304171249426de86a985834f8", 16),//fib(954)
                    new BigInteger("3948bc64661abb4c7015114a395fcf960420348d2041bba72127a6f780ee1355179cef3e593d8a7b9d013c561702a73337641b6161c2ef1e22fc4a5ae096a9a8b63eb3968131e11103a3d28d649369783eb885", 16),//fib(955)
                    new BigInteger("5cb00a87b9c883b4aeee9c3c4e34f96b2a09be8cc1c2e1d82e2a22015d91aaad0b31a5d05344ba8fd4b4001fca0f2f84b5342e7eca49a1b31017ae341b0cce847159bfb9e8ce64151ab61bcfd27bd41096ed7d", 16),//fib(956)
                    new BigInteger("95f8c6ec1fe33f011f03ad868794c9012e29f319e2049d7f4f51c8f8de7fbe0222ce950eac82450b71b53c75e111d6b7ec9849e02c0c90d13313f88efba3782d279873506a0045261e59ee5d370f3d88d5a602", 16),//fib(957)
                    new BigInteger("f2a8d173d9abc2b5cdf249c2d5c9c26c5833b1a6a3c77f577d7beafa3c1168af2e003adeffc6ff9b46693c95ab21063ca1cc785ef6563284432ba6c316b046b198f2330a52cea93b39100a2d098b11996c937f", 16),//fib(958)
                    new BigInteger("188a1985ff98f01b6ecf5f7495d5e8b6d865da4c085cc1cd6cccdb3f31a9126b150cecfedac4944a6b81e790b8c32dcf48e64c23f2262c355763f9f521253bedec08aa65abcceee615769f88a409a4f22423981", 16),//fib(959)
                    new BigInteger("27b4a69d3d33ac46cbae8410c33284dd9de91566729939c2e4a499eed56a28f607ecf0accac104441fe87b5a13753e33130313a9e18b8f5d9b96b461529040590597cd9650f9d979c907a02b74a2560bbaecd00", 16),//fib(960)
                    new BigInteger("403ec0233ccc9c623a7de38559086d94764eefb27af5fb905171752e07133b611cf9ddaba585988e8b6a62eacc386c025be95fcdd3b1bb92f2faae5673b57c46f1a077fbfcc6c85fde7e3fb418abfafddf10681", 16),//fib(961)
                    new BigInteger("67f366c07a0048a9062c67961c3af27214380518ed8f355336160f1cdc7d645724e6ce5870469cd2ab52de44dfadaa356eec7377b53d4af08e9162b7c645bc9ff73845924dc0a1d9a785dfdf8d4e510999fd381", 16),//fib(962)
                    new BigInteger("a83226e3b6cce50b40aa4b1b754360068a86f4cb688530e38787844ae3909fb841e0ac0415cc356136bd412fabe61637cad5d34588ef0683818c110e39fb38e6e8d8bd8e4a876a3986041f93a5fa4c07790da02", 16),//fib(963)
                    new BigInteger("110258da430cd2db446d6b2b1917e52789ebef9e456146636bd9d9367c00e040f66c77a5c8612d233e2101f748b93c06d39c246bd3e2c5174101d73c60040f586e011032098480c132d89ff7333489d11130ad83", 16),//fib(964)
                    new BigInteger("1b857b487e79a12bf8780fdcd06c1b27f2945eeafbe99971a452517b2a39ea3c7a8a826609bdf079518cd60a43779d6a504981a02c71b57f791a984d43a3c2e6dc8e9c0aee2cf764cb38e1f06d942e9188c18785", 16),//fib(965)
                    new BigInteger("2c87d422c18674073ce57b07e984004f7c804e89414adfd5102c2ab1a63aca7d70f6fa0bd21f1d9c8fadd8018c30d97123e5a60c00547a96ba1c6f89a3a7d23f4a8fac3cf7b17825fe1181e7a0c8b86299f23508", 16),//fib(966)
                    new BigInteger("480d4f6b40001533355d8ae4b9f01b776f14ad743d347946b47e7c2cd074b4b9eb817c71dbdd0e15e13aae0bcfa876db742f27ac2cc63016333707d6e74b9526271e4847e5de6f8ac94a63d80e5ce6f422b3bc8d", 16),//fib(967)
                    new BigInteger("7495238e0186893a724305eca3741bc6eb94fbfd7e7f591bc4aaa6de76af7f375c78767dadfc2bb270e8860d5bd9504c9814cdb82d1aaaaced5377608af3676571adf484dd8fe7b0c75be5bfaf259f56bca5f195", 16),//fib(968)
                    new BigInteger("bca272f941869e6da7a090d15d64373e5aa9a971bbb3d2627929230b472433f147f9f2ef89d939c8522334192b81c7280c43f56459e0dac3208a7f37723efc8b98cc3cccc36e573b90a64997bd82864adf59ae22", 16),//fib(969)
                    new BigInteger("131379687430d27a819e396be00d85305463ea56f3a332b7e3dd3c9e9bdd3b328a472696d37d5657ac30bba26875b1774a458c31c86fb85700dddf697fd3263f10a7a3151a0fe3eec58022f576ca825a19bff9fb7", 16),//fib(970)
                    new BigInteger("1edda09808493c615c184278f5e3c8a43a0e84ee0f5e6fde0b6fcecf504f7e719ec6c5c5cc1ae9f43152eee3fb2dcde9cb09cb880e0dc60332e6875cf6f71607ca3466e1e646c9627e8a878ef2a2aabec7b594dd9", 16),//fib(971)
                    new BigInteger("31f11a007c7a0edbddb67be4d5f14dd48e726f450301a295ef4d0b6dec2cb9a4290dec5c9f98404bdd83aa8663a37f61154f57b9d67d7e5a33c466c676ca3c46dadc09f70056ad51440aaa84696d2d18e1758ed90", 16),//fib(972)
                    new BigInteger("50ceba9884c34b3d39cebe5dcbd51678c880f43312601273fabcda3d3c7c3815c7d4b2226bb32a400ed6996a5ed14d4ae0592341e48b445d66aaee236dc1524ea51070d8e69d76b3c29532135c0fd7d7a92b23b69", 16),//fib(973)
                    new BigInteger("82bfd499013d5a1917853a42a1c6644d56f363781561b509ea09e5ab28a8f1b9f0e29e7f0b4b6a8bec5a43f0c274ccabf5a87afbbb08c2b79a6f54e9e48b8e957fec7acfe6f42405069fdc97c57d04f08aa0b28f9", 16),//fib(974)
                    new BigInteger("d38e8f318600a5565153f8a06d9b7ac61f7457ab27c1c77de4c6bfe8652529cfb8b750a176fe94cbfb30dd5b214619f6d6019e3d9f940715011a430d524ce0e424fceba8cd919ab8c9350eab218cdcc833cbd6462", 16),//fib(975)
                    new BigInteger("1564e63ca873dff6f68d932e30f61df137667bb233d237c87ced0a5938dce1b89a999ef208249ff57e78b214be3bae6a2cbaa19395a9cc9cc9b8997f736d86f79a4e96678b485bebdcfd4eb42e709e1b8be6c88d5b", 16),//fib(976)
                    new BigInteger("229dcf2fc0d3ea4c5ba2d2b837cfd59d995dc12ce64e54405b397657bf2f3455962513fc1f9489423e2bbfea705010099a1abb776fa30d0e19ca3db048925505dc9e65221821759769909f9ee0896be80f2385f1bd", 16),//fib(977)
                    new BigInteger("3802b56c6947ca43523065e668c5f38ed0c43cdf1a208c08d82680b0f80c160e30beb2ee27b92937bca471ff2e8bbe73c6d55d0b054cd9aae382d72fbbffdbfd76ecfb89a369d183468dee530efa0a039b0a4e7f18", 16),//fib(978)
                    new BigInteger("5aa0849c2a1bb48fadd3389ea095c92c6a21fe0c006ee049335ff708b73b4a63c6e3c6ea474db279fad031e99edbce7d60f0188274efe6b8fd4d14e004923103538b60abbb8b471ab01e8df1ef8375ebaa2dd470d5", 16),//fib(979)
                    new BigInteger("92a33a0893637ed300039e85095bbcbb3ae63aeb1a8f6c520b8677b9af476071f7a279d86f06dbb1b774a3e8cd678cf127c5758d7a3cc063e0cfec0fc0920d00ca785c355ef5189df6ac7c44fe7d7fef453822efed", 16),//fib(980)
                    new BigInteger("ed43bea4bd7f3362add6d723a9f185e7a50838f71afe4c9b3ee66ec26682aad5be8640c2b6548e2bb244d5d26c435b6e88b58e0fef2ca71cde1d00efc5243e041e03bce11a805fb8a6cb0a36ee00f5daef65f760c2", 16),//fib(981)
                    new BigInteger("17fe6f8ad50e2b235adda75a8b34d42a2dfee73e2358db8ed4a6ce67c15ca0b47b628ba9b255b69dd69b979bb39aae85fb07b039d69696780beececff85b64b04e87c1916797578569d77867bec7e75ca349e1a50af", 16),//fib(982)
                    new BigInteger("26d2ab7520e61e5985bb14ccc5d3ec88a84f6acd9508c05888953553e7c4cb61d74aefb5ddbaff8091bfe4f8da5ee43ce393091ad58960e9d9d09edef4ada8909067fd5f793f5d80f444290b2da7f6ba5240411b171", 16),//fib(983)
                    new BigInteger("3ed11afff5f4497ce098bc275108c0b2d64e520bb8619be75d3c03bba9216c1652ad7b5f9010b61e685b7c948df992c2de9ab954ac1ff761e5bf6daeed090d40deefbef0e0d6b5065e1ba172ec6fde16f58a22c0220", 16),//fib(984)
                    new BigInteger("65a3c67516da67d66653d0f416dcad3b7e9dbcd94d6a5c3fe5d1390f90e6377829f86b156dcbb59efa1b618d685876ffc22dc26f81a9584bbf900c8de1b6b5d16f57bc505a161287525fca7e1a17d4d147ca63db391", 16),//fib(985)
                    new BigInteger("a474e1750cceb15346ec8d1b67e56dee54ec0ee505cbf827430d3ccb3a07a38e7ca5e674fddc6bbd6276de21f65209c2a0c87bc42dc94fada54f7a3ccebfc3124e477b413aecc78db07b6bf10687b2e83d54869b5b1", 16),//fib(986)
                    new BigInteger("10a18a7ea23a91929ad405e0f7ec21b29d389cbbe5336546728de75dacaeddb06a69e518a6ba8215c5c923faf5eaa80c262f63e33af72a7f964df86cab07678e3bd9f37919502da1502db366f209f87b9851eea76942", 16),//fib(987)
                    new BigInteger("1ae8d895f3077ca7cf42ceb2ae6a789182875daa359024c8e6bebb2a604f57e95234437ff69848d19bf091dd154fc8a8503beb9f7dd3bf7a70a2f01077f363bf60be6b2d2cfefa1a2b356a26027273aa1c2737111ef3", 16),//fib(988)
                    new BigInteger("2b8a631495420e3a6a16d493a6569a441fbffa661ac38a0f594ca2880cfe3599bc9e28989d52cae761b9b5d80b3a70b4766b4f82b8cae9fa06f0e87d22facb4d9c985ea6464f27bb7b631d8cf47c6c25b47925b88835", 16),//fib(989)
                    new BigInteger("46733baa88498ae23959a34654c112d5a24758105053aed8400b5db26d4d8d830ed26c1893eb13b8fdaa47b5208a395cc6a73b22369ea9747793d88d9aee2f0cfd56c9d3734e21d5a69887b2f6eedfcfd0a05cc9a728", 16),//fib(990)
                    new BigInteger("71fd9ebf1d8b991ca37077d9fb17ad19c20752766b1738e79958003a7a4bc31ccb7094b1313ddea05f63fd8d2bc4aa113d128aa4ef69936e7e84c10abde8fa5a99ef2879b99d499121fba53feb6b4bf5851982822f5d", 16),//fib(991)
                    new BigInteger("b870da69a5d523fedcca1b204fd8bfef644eaa86bb6ae7bfd9635dece799509fda4300c9c528f2595d0e45424c4ee36e03b9c5c726083ce2f618999858d729679745f24d2ceb6b66c8942cf2e25a2bc555b9df4bd685", 16),//fib(992)
                    new BigInteger("12a6e7928c360bd1b803a92fa4af06d092655fcfd268220a772bb5e2761e513bca5b3957af666d0f9bc7242cf78138d7f40cc506c1571d051749d5aa316c023c231351ac6e688b4f7ea8fd232cdc577badad361ce05e2", 16),//fib(993)
                    new BigInteger("1e2df53926935e11a5d04ae1a9ac92cf88aa4a783e1ed08674c1ebc14497e645c7ff69644bb8fc35319808811c46270ed448616333b7a0d346ab5f43b6f974d29c87b0d141374205eb323ff25b01fa380308d4119dc67", 16),//fib(994)
                    new BigInteger("30d4dccbb2c969e35dd3f4114e5b99a01b0faa481086f290ebeda1a3bab63781925aa2bbfb1f6944cd5f2cae13c75fe6c8552669f50ebdd85df534ede865770ebf9b027daf9fcd5569db3d1587de51b3b0b60a2e7e249", 16),//fib(995)
                    new BigInteger("4f02d204d95cc7f503a43ef2f8082c6fa3b9f4c04ea5c31760af8d64ff4e1dc75a5a0c2046d86579fef7352f300d86f59c9d87cd28c65eaba4a094319f5eebe15c22b34ef0d70f5b550d7d07e2e04bebb3bede401beb0", 16),//fib(996)
                    new BigInteger("7fd7aed08c2631d8617833044663c60fbec99f085f2cb5a84c9d2f08ba045548ecb4aedc41f7cebecc5661dd43d4e6dc64f2ae371dd51c840295c91f87c462f01bbdb5cca076dcb0bee8ba1d6abe9d9f6474e86e9a0f9", 16),//fib(997)
                    new BigInteger("ceda80d56582f9cd651c71f73e6bf27f628393c8add278bfad4cbc6db9527310470ebafc88d03438cb4d970c73e26dd201903604469b7b2fa7365d5127234ed177e0691b914dec0c13f637254d9ee98b1833c6aeb5fa9", 16),//fib(998)
                    new BigInteger("14eb22fa5f1a92ba5c694a4fb84cfb88f214d32d10cff2e67f9e9eb767356c85933c369d8cac802f797a3f8e9b7b754ae6682e43b647097b3a9cc2670aee7b1c1939e1ee831c4c8bcd2def142b85d872a7ca8af1d500a2", 16),//fib(999)
                    new BigInteger("21d8cb07b572c25732bb116f2c33bab0e83d0c699bad1a727a736a7e42ca93b697ad224d55398373062f18ff62b99c28068131a3fab0c12e3510283c1d60b00930b7e8803c312b4c8e6d5286805fc70b594dc75cc0604b", 16),//fib(1000)
                    new BigInteger("36c3ee02148d55118f245bbee480b639da51df96ac7d0d58fa120935aa00003c2ae958eae1e603a27fa9588dfe351172ece95fe7b0f7caa96faceaa3284f2b2549f1ca6ebf4d77d85b9b419aabe59f7e0118524e9560ed", 16),//fib(1001)
                    new BigInteger("589cb909ca001768c1df6d2e10b470eac28eec00482a27cb748573b3ecca93f2c2967b38371f871585d8718d60eead9af36a918baba88bd7a4bd12df45afdb2e7aa9b2eefb7ea324ea0894212c4566895a6619ab55c138", 16),//fib(1002)
                    new BigInteger("8f60a70bde8d6c7a5103c8ecf53527249ce0cb96f4a735246e977ce996ca942eed7fd42319058ab80581ca1b5f23bf0de053f1735ca056811469fd826dff0653c49b7d5dbacc1afd45a3d5bbd82b06075b7e6bf9eb2225", 16),//fib(1003)
                    new BigInteger("e7fd6015a88d83e312e3361b05e9980f5f6fb7973cd15cefe31cf09d83952821b0164f5b502511cd8b5a3ba8c0126ca8d3be82ff0848e258b9271061b3aee1823f45304cb64abe222fac69dd04706c90b5e485a540e35d", 16),//fib(1004)
                    new BigInteger("1775e0721871af05d63e6ff07fb1ebf33fc50832e3178921451b46d871a5fbc509d96237e692a9c8590dc05c41f362bb6b412747264e938d9cd910de421ade7d603e0adaa7116d91f75503f98dc9b72981162f19f2c0582", 16),//fib(1005)
                    new BigInteger("25f5b67372fa8744076ca3523010857435bc03ac56e49ef0434d15e249df4e4724dac72d9b94fae531c36416cdf48985f87d0f7716d321b3286b81e45d55cc9584325ddf727619741a4fca975e10bdf28c74777446ce8df", 16),//fib(1006)
                    new BigInteger("3d6b96e58b6c3649ddab1342afc2716775810bdf39fc281188685cbabb854a0c2eb429658227a4ad8ad124730fe7ec4163be36be3d21b540c54492c29f70ab12e47068ba1987870611a4ce90ebda751c0d8aa68e398ee61", 16),//fib(1007)
                    new BigInteger("63614d58fe66bd8de517b694dfd2f6dbab3d0f8b90e0c701cbb5729d05649853538ef0931dbc9f92bc948889dddc75c75c3b463553f4d6f3edb014a6fcc677a868a2c6998bfda07a2bf4992849eb330e99ff1e02805d740", 16),//fib(1008)
                    new BigInteger("a0cce43e89d2f3d7c2c2c9d78f95684320be1b6acadcef13541dcf57c0e9e25f824319f89fe444404765acfcedc46208bff97cf391168c34b2f4a7699c3722bb4d132f53a58527803d9967b935c5a82aa789c490b9ec5a1", 16),//fib(1009)
                    new BigInteger("1042e31978839b165a7da806c6f685f1ecbfb2af65bbdb6151fd341f4c64e7ab2d5d20a8bbda0e3d303fa3586cba0d7d01c34c328e50b6328a0a4bc1098fd9a63b5b5f5ed3182c7fa698e00e17fb0db394188e2933a49ce1", 16),//fib(1010)
                    new BigInteger("1a4fb15d6120ca53d6a9d4a43fefdc761ecb94661269aa52873f1114c87385d12581524845d8528134b5fe283b96539d8dc2e401c7621ef5d5399637a3534bd1f02c92540d707ef7aa727689ab5768363e912a723f436282", 16),//fib(1011)
                    new BigInteger("2a929476d9a4656a31277cab06e662680b8b4715782585b3d93c453414d86d7c52de72f101b260be64f5a180a850611a8f86303455b2d5285f43e1f8ace325782b87f1b2e088ab77510b5697c35275e9d2a9b89b72e7ff63", 16),//fib(1012)
                    new BigInteger("44e245d43ac52fbe07d1514f46d63ede2a56db7b8a8f3006607b5648dd4bf34d785fc539478ab33f99ab9fa8e3e6b4b81d4914361d14f41e347d78305036714a1bb48406edf92a6efb7dcd216ea9de20113ae30db22b61e5", 16),//fib(1013)
                    new BigInteger("6f74da4b1469952838f8cdfa4dbca14635e2229102b4b5ba39b79b7cf22460c9cb3e382a493d13fdfea141298c3715d2accf446a72c7c94693c15a28fd1996c2473c75b9ce81d5e64c8923b931fc5409e3e49ba925136148", 16),//fib(1014)
                    new BigInteger("b457201f4f2ec4e640ca1f499492e0246038fe0c8d43e5c09a32f1c5cf705417439dfd6390c7c73d984ce0d2701dca8aca1858a08fdcbd64c83ed2594d50080c62f0f9c0bc7b00554806f0daa0a63229f51f7eb6d73ec32d", 16),//fib(1015)
                    new BigInteger("123cbfa6a63985a0e79c2ed43e24f816a961b209d8ff89b7ad3ea8d42c194b4e10edc358dda04db3b96ee21fbfc54e05d76e79d0b02a486ab5c002c824a699eceaa2d6f7a8afcd63b94901493d2a28633d9041a5ffc522475", 16),//fib(1016)
                    new BigInteger("1d8231a89b2c71ef4ba8d0c8d76e2618ef6541eaa1d3c813b6e1d7f08910508f8527a32f16acca2792f3b02ce6c72aae840fff5ab92814410243efedb97b9a6db0d1e693b4777d690dc97056e7348b85dce239916d390e7a2", 16),//fib(1017)
                    new BigInteger("2fbef14f4165f7903344ff9d15931e2f98c6f3f47ad351cb642080c4b5299bdd96156687f44d17db4c62924ca68c78b45b7e792b69525cabb803f2b5de22345a9b74bd8b5d274accc71271a0245eb3e91a727b376cfe30c17", 16),//fib(1018)
                    new BigInteger("4d4122f7dc92697f7eedd065ed014448882c35df1ca719df1b0258b53e39ec6d1b3d09b70af9e202df5642798d53a362df8e7886227a70ecba47e2a3979dcec84c46a41f119ec835d4dbe1f70b933f6ef754b4c8da373f3b9", 16),//fib(1019)
                    new BigInteger("7d0014471df8610fb232d0030294627820f329d3977a6baa7f22d979f363884ab152703eff46f9de2bb8d4c633e01c173b0cf1b18bcccd98724bd55975c00322e7bb61aa6ec613029bee53972ff1f35811c7300047356ffd0", 16),//fib(1020)
                    new BigInteger("ca41373efa8aca8f3120a068ef95a6c0a91f5fb2b42185899a25322f319d74b7cc8f79f60a40dbe10b0f173fc133bf7a1a9b6a37ae473e852c93b7fd0d5dd1eb340205c98064db3870ca358e3b8532c7091be4c9216caf389", 16),//fib(1021)
                    new BigInteger("147414b8618832b9ee353706bf22a0938ca1289864b9bf13419480ba92500fd027de1ea350987d5bf36c7ec05f513db9155a85be93a140c1d9edf8d56831dd50e1bbd6773ef2aee3b0cb889256b77261f1ae314c968a21f359", 16),//fib(1022)
                    new BigInteger("2118282c5130df62e147410d4e1bfaff97331e938ffbd76bdb36d3dd8569e71ba4a71642b13c8b1a041d70345b6479b0b7043c620e85b4aa2cb734553907ba6f94fbf6d3d6f8fc9737d82beb3a6fc58e623fef9928a0ece6e2", 16)//fib(1023)
            };
        }

    }
}
