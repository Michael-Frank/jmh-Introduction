package de.frank.jmh.basic;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.concurrent.TimeUnit;


/*--
 * Further benchmarks: HashMaps of various libraries including JDK
 * http://java-performance.info/hashmap-overview-jdk-fastutil-goldman-sachs-hppc-koloboke-trove-january-2015/
 *
 * ==== DANGER - SUB-NANOSECONDS BENCHMARK ===
 * ====       !!!Use with caution!!!       ===
 * Before telling you what you can learn from this benchmark, let me tell you what you CANT.
 * Such a types of "nano-benchmark", and this one in particular, have many
 * issues and restrictions.
 *   + Dont use the absolute ns/op values as a base for decisions! only look at
 *     the rough relations between individual tests. => "A is roughly x times faster then B"
 *   + can be heavily skewed and flawed (reported execution times != real execution times)
 *   + is very dependent on your hardware (like most benchmarks)
 *   + depends on Java version (e.g. in case of higher level classes:
 *     BigInteger/BigDecimal implementation may change)
 *   + performance is not additive - operations perform very different when mixed and in sequence with other operations,
 *     like they are ALWAYS in real programs -> hint: cpu pipelining
 *   + operations perform very different inside loops  (cpu pipelining and SIMD instructions
 *   + Your cpu has shared resources between cores (and especially between virtual
 *     threads like "hyper-threads")
 *       => one thread reaching 100Million op/s does NOT imply that two threads will reach
 *          200Million op/s
 *   + The Thermal design limit and dynamic Overclocking/Throttling will distort your performance numbers when scaling
 *     number of threads - scalup today is mostly not linear.
 *   + Single Threaded benchmarking leaves room for a lot of other things to run fast in parallel like:
 *     + Java c1 & c2 code compiler (optimized code may be available earlier)
 *     + Multiple concurrent garbage collection threads
 *     + thermal limit in your CPU (dynamic overclocking)
 *     + Available (shared) cache in your cpu
 *   + Multi-threaded benchmarking - saturating your machine with 'your' work leaves
 *     less room for the background tasks like:
 *     + Compiler: it will take longer for the java JIT compiler to complete its work
 *       as its competing with 'your' work tasks for cpu time
 *     + Garbage collection: you will allocate a lot more memory with more threads,
 *       but at the same time you will have less cpu resources for the garbage collector
 *       to do its work => Allocation pressure rises, "full gc -stop the world" events
 *       become more common => application throughput drops rapidly.
 *     + A cpu may run hot and throttle down.
 *
 * ###Caution!!##
 * +  Dont trust this benchmark, dont trust any benchmark.
 * + And if a subsecond benchmark is not done with JMH it is probably totally wrong.
 * + Always wear your "bullshit-googles"
 * + Be cautious, ask WHY.
 * + Read about benchmarking,performance and the JVM, JVM optmizations, JVM memory model.. if done, read more.
 * + Question these numbers!
 *     e.g. i should get ~0.33 ns/op for 'add' op on my machine as an Add takes one cycle and it's a 3GHz CPU with theoretical 3*10^9 OPs/s => 0.33   ns/op
 *     but we are measuring about ~2.8 ns/op here... (e.g. you have to "return the result" which will cost you => see "noOpReturnCostBaseline" benchmark)
 *
 * + Dont loop inside of your benchmark code! I did it on purpose here to show the effect in the "*Loop_100000" benchmarks.
 *   Look at this line:
 *     ArithmeticOpsBenchmark.int_SumLoop_100000  2402,361 ns/op=> 0,024 ns/op(for single iteration)
 *   Remember: my machine can do max 0.33 ns/op but we measured a result 10 times faster then theoretically possible.
 *   The JVM is very smart in optimizing loops, folding variables and doing entirely different things then what you "programmed" in your code.
 *   In this case the JVM can predict the loop and may transform the 100000 'add' iterations into:  "y*100000" or employs other optimizations like using SIMD instructions.
 *   If you really must know what the jvm is doing, you have to install the 'hdis' (hotspot disassembler) binary in your jdk to get the real assembly code generated by the JVM.
 *   I leave that as an exercise for your (output is strongly machine and JVM dependent)
 *
 * What you !!can't!! learn from this ns/op numbers:
 *   + how fast things will be in your! program/algorithm
 *   + how many int ops your! cpu can do
 *   + predictions: "my algorithm has two integer mult's and three add's, so it will take 2*6.3ns+3*6.3ns=31.6ns" That approach of thinking does not work!
 *
 *
 * So what might you learn then from these numbers then?
 *   + very little  :)
 *   + that its hard to do (nano) benchmarks
 *   + basic/primitive operations are very fast - they are translated into single cpu instruction machine code after all.
 *   + int, long or double ... does not matter on a modern 64bit cpu. all are (equally) fast
 *       => dont try to "optimize" by restricting your data type to int/float. Just go with long/double for everything.
 *   + Div (/) and mod (%) are still more expensive then the other operations but in the greater picture mostly still negligible.
 *   + BigInteger / BigDecimal is not as expensive as you would think, still expensive though.
 *     But thanks many jvm tricks like escape analysis (Object-on-stack-allocation),
 *     very fast algorithms (e.g. multiplication) and even intrinsics for them, it is quite impressive.
 *   + BigInteger / BigDecimal get slower the "bigger" the numbers get.
 *     Most of the time you need only slightly more bits then long/double => so its "fast"

=================================================
What you should really take form this benchmark
- Computation is fast
 - raw add/mul computation power is almost all cases not the limiting factor - your data is!
- Most code and algorithm may appear CPU-Bound, but in reality is memory bound
  - most obviously, allocating objects
  - not so obviously: getting the data you wish to process from RAM into CPU and keeping it there
    - mostly your CPU is waiting for data to be loaded from RAM or speculating about it
    - optimize your data layout!
    - keep memory access predictable (linear array strides instead of random access)
    - processing sorted data is faster - a lot - cpu can speculate on result with branch prediction
    - threads - more threads -> more context switching -> cpu has to load data for other work
  - if you can use a sorted array of primitives (instead of lists or whatever) DO IT
    arrays are underused today



Latency comparison numbers every programmer should know - form https://gist.github.com/jboner/2841832
--------------------------
L1 cache reference                           0.5 ns
Branch mispredict                            5   ns
L2 cache reference                           7   ns                      14x L1 cache
Mutex lock/unlock                           25   ns
Main memory reference                      100   ns                      20x L2 cache, 200x L1 cache
Compress 1K bytes with Zippy             3,000   ns        3 us
Send 1K bytes over 1 Gbps network       10,000   ns       10 us
Read 4K randomly from SSD*             150,000   ns      150 us          ~1GB/sec SSD
Read 1 MB sequentially from memory     250,000   ns      250 us
Round trip within same datacenter      500,000   ns      500 us
Read 1 MB sequentially from SSD*     1,000,000   ns    1,000 us    1 ms  ~1GB/sec SSD, 4X memory
Disk seek                           10,000,000   ns   10,000 us   10 ms  20x datacenter roundtrip
Read 1 MB sequentially from disk    20,000,000   ns   20,000 us   20 ms  80x memory, 20X SSD
Send packet CA->Netherlands->CA    150,000,000   ns  150,000 us  150 ms


##################################################################################
PRIMITIVES
##################################################################################
noOpBaseline             0,316  <-- doing absolutely nothing
noOpReturnCostBaseline   2,714  <-- overhead of benchmarking a single operation (every method has a return)
##################################################################################
Measurements in nanoseconds per Operation ( ns/Op )
Values in this table contain the cost for the basic operation as well as the cost for calling the benchmark method.
Calling the benchmark method is about: noOpReturnCostBaseline   2,714 ns/Ops.
See below for a table which is normalized by the overhead.
##################################################################################
|                        byte     short    char     int     long    float   double
Arithmetic
increment                 3,03     3,04     2,97    2,97    2,96     3,39     3,48
Add                       3,17     3,25     3,23    2,77    2,79     3,19     4,06
Substract                 3,20     3,26     3,18    2,77    2,79     3,17     3,20
Mult                      3,59     3,21     3,62    2,83    2,81     3,32     3,29
Div                       5,43     5,38     5,80    5,14   12,75     3,56     4,47
Rem                       5,39     5,37     5,37    5,09   12,29    28,69    26,34

Bitwise
AND                       3,17     3,20     3,21    2,76    2,78  see int see long
OR                        3,19     3,17     3,26    2,77    2,78  see int see long
XOR                       3,32     3,26     3,26    2,81    2,84  see int see long
ShiftL                    3,06     3,04     3,04    2,90    2,91  see int see long
ShiftRight                3,09     3,06     3,01    2,91    2,93  see int see long
ShiftRightUnsigned        3,04     3,05     3,03    2,95    2,92  see int see long

Misc
equals                    3,41     3,33     3,18    3,22    3,21     3,27     3,33
compare                   3,14     3,12     3,22    3,37    3,46     4,15     3,47
Conversion
toString                 24,51    30,79     8,62   40,65   48,09    78,23   294,87
toHexString                                        45,10   61,45      n/a      n/a
toWrapperTypeCached       3,04     3,50     3,14    3,67    3,80      n/a      n/a (int->Integer, long->Long, ...)
toWrapperTypeUncached      n/a     4,92     4,83    4,98    5,30     4,79     5,43 (int->Integer, long->Long, ...)

Loop
SumLoop_100000          62.988	 62.562	  62.604   2.410  31.187   93.759   93.657
MultLoop_100000        125.043  125.387  125.732  94.144  93.821  155.834  156.272
SumLoop per inter.        0,63     0,63     0,63    0,02    0,31     0,94     0,94
MultLoop per inter.       1,25     1,25     1,26    0,94    0,94     1,56     1,56

More bit stuff
bitCount                                            2,82    3,05
leadingZeros                                        3,45    3,44
rotateLeft                                          2,89    2,91
rotateRight                                         2,90    2,94

##################################################################################
PRIMITIVES
##################################################################################
Measurements in nanoseconds per Operation ( ns/Op )
Same as above but normalized by costs of  -"noOpReturnCostBaseline" time
##################################################################################
|                         byte     short    char     int    long    float   double
Arithmetic
increment                 0,32     0,33     0,25    0,26    0,24     0,68     0,77
Add                       0,46     0,54     0,52    0,06    0,07     0,47     1,34 (see int & long -- thats why you dont trust absolute values of nanobenchmarks -- one op on this machine @ 3GHZ takes 0.33ns. 0.06 is not possible
Substract                 0,49     0,55     0,46    0,06    0,08     0,46     0,49
Mult                      0,88     0,50     0,90    0,11    0,10     0,61     0,57
Div                       2,72     2,66     3,08    2,43   10,03     0,84     1,76
Rem                       2,68     2,66     2,66    2,37    9,57    25,98    23,63

Bitwise
AND                       0,45     0,49     0,50    0,04    0,06  see int see long
OR                        0,47     0,45     0,55    0,06    0,07  see int see long
XOR                       0,60     0,55     0,54    0,09    0,12  see int see long
ShiftL                    0,34     0,33     0,33    0,18    0,19  see int see long
ShiftRight                0,37     0,35     0,30    0,19    0,22  see int see long
ShiftRightUnsigned        0,32     0,33     0,31    0,24    0,20  see int see long

Misc
equals                    0,69     0,62     0,47    0,51    0,49     0,55     0,62
compare                   0,43     0,41     0,51    0,65    0,74     1,43     0,76

Conversion
toString                 21,79    28,08     5,91   37,93   45,38    75,51   292,15
toHexString                                        42,38   58,73      n/a      n/a
toWrapperTypeCached       0,32     0,79     0,43    0,95    1,08      n/a      n/a (int->Integer, long->Long, ...)
toWrapperTypeUncached      n/a     2,21     2,12    2,27    2,58     2,07     2,71 (int->Integer, long->Long, ...)

Loop
SumLoop_100000          62.988	 62.562	  62.604   2.410  31.187   93.759   93.657
MultLoop_100000        125.043  125.387  125.732  94.144  93.821  155.834  156.272
SumLoop per inter.        0,63     0,63     0,63    0,02    0,31     0,94     0,94
MultLoop per inter.       1,25     1,25     1,26    0,94    0,94     1,56     1,56

More bit stuff
bitCount                                            0,11    0,33
leadingZeros                                        0,74    0,72
rotateLeft                                          0,18    0,20
rotateRight                                         0,19    0,23

##################################################################################
BigInteger/BigDecimal tests -  every value in ns/Op
##################################################################################
|            bigInteger		           |   bigDecimal
|            small  LongMax  RealyBig  |   small   LongMax  RealyBig
Add          22,33    22,84     26,92  |    5,61     19,33     38,12
Substract    24,72    25,83     29,18  |    5,74      5,87     36,08
Mult         30,84    24,92     57,19  |    5,76     34,77     59,80
Rem          39,74    76,04    237,88  |  108,26    772,88  1.131,89
shiftL       20,76    24,34     27,89  |
shiftR       23,15    24,83     30,93  |
Mod          34,21    68,93    220,98  |
Div          31,73    66,48    189,58  |
Div128                                 | 2.573,52 1.559,17    369,22
Div64                                  |    74,55    39,41    322,91
toString    102,18   375,07    687,10  |     0,42     0,40      0,27  <-- BigDecimal has StringCache
XOR          29,04    32,95     42,70  |
SumLoop100000               2.067.472  |     485.869
MultLoop100000            480.779.051  | 508.446.604
SumLoop per interation          20,67  |        4,85
MultLoop per interation     48.077,90  |    5.084,46


##################################################################################
RAW RESULTS
##################################################################################
# Run complete. Total time: 00:38:12

Benchmark                        Mode Cnt       Score ±     Error  Unit       Score     Loop per
                                                                         Normalized   invocation
noOpBaseline                     avgt   5        0,31 ±      0,01 ns/op
noOpReturnCostBaseline           avgt   5        2,69 ±      0,17 ns/op  <== normalize by: (score-ReturnCostBaseLine)

Math_max_double                  avgt   5        4,59 ±      0,07 ns/op
Math_max_int                     avgt   5        3,33 ±      0,18 ns/op
Math_max_long                    avgt   5        3,07 ±      0,07 ns/op
Math_min_double                  avgt   5        4,62 ±      0,07 ns/op
Math_min_int                     avgt   5        3,06 ±      0,14 ns/op
Math_min_long                    avgt   5        3,09 ±      0,10 ns/op
Math_abs                         avgt   5        3,25 ±      0,34 ns/op
Math_abs_double                  avgt   5        3,30 ±      0,78 ns/op
Math_round                       avgt   5        5,65 ±      0,10 ns/op
Math_ceil                        avgt   5        4,77 ±      0,14 ns/op
Math_sin                         avgt   5       39,09 ±      0,59 ns/op
Math_tan                         avgt   5       49,93 ±      1,47 ns/op
Math_cos                         avgt   5       47,64 ±      0,39 ns/op
Math_exp                         avgt   5       56,37 ±      0,44 ns/op
Math_log                         avgt   5       22,08 ±      0,36 ns/op
Math_log10                       avgt   5       21,91 ±      1,04 ns/op
Math_SQRT                        avgt   5        6,27 ±      0,12 ns/op
Math_pow0_5                      avgt   5       74,36 ±      1,40 ns/op
Math_pow1_5                      avgt   5       73,10 ±      2,76 ns/op
Math_pow2                        avgt   5        3,22 ±      0,09 ns/op
Math_powX                        avgt   5       74,58 ±      1,14 ns/op
Math_addExact_int                avgt   5        3,95 ±      0,20 ns/op
Math_muliplyExact_int            avgt   5        4,12 ±      0,16 ns/op
Math_muliplyExact_long           avgt   5        4,10 ±      0,58 ns/op

Math_EXTRA_invSQRT 1/Math.sqrt(x)avgt   5       12,50 ±      0,44 ns/op
Math_EXTRA_invSQRT_cust_double   avgt   5        5,34 ±      0,04 ns/op
Math_EXTRA_invSQRT_cust_double2  avgt   5       11,43 ±      0,16 ns/op
Math_EXTRA_invSQRT_cust_float    avgt   5        5,58 ±      0,20 ns/op

instanceOfFalse                  avgt   5        2,85 ±      0,06 ns/op        0,16
instanceOfTrue                   avgt   5        2,90 ±      0,13 ns/op        0,21
methodCallOverhead_0Args         avgt   5        2,18 ±      0,06 ns/op       -0,51
methodCallOverhead_2Args         avgt   5        2,38 ±      0,08 ns/op       -0,31
nullCheckFalse                   avgt   5        2,90 ±      0,17 ns/op        0,21
nullCheckTrue                    avgt   5        2,93 ±      0,04 ns/op        0,25
lookupSwitch                     avgt   5        3,43 ±      0,10 ns/op        0,75
tableSwitch                      avgt   5        2,79 ±      0,16 ns/op        0,10
tableSwitchWithHoles             avgt   5        3,45 ±      0,11 ns/op        0,76
ifElseCascade                    avgt   5        3,14 ±      0,11 ns/op        0,46
createNewException               avgt   5    1.160,03 ±     37,78 ns/op    1.157,34
createNewExceptionWithoutStackTr avgt   5       21,48 ±      0,14 ns/op       18,80
catchException                   avgt   5    1.160,69 ±     13,52 ns/op    1.158,01
catchExceptionWithoutStacktrace  avgt   5       21,61 ±      0,28 ns/op       18,92

byte_AND                         avgt   5        3,23 ±      0,16 ns/op        0,55
byte_Add                         avgt   5        3,16 ±      0,10 ns/op        0,47
byte_Div                         avgt   5        5,38 ±      0,06 ns/op        2,69
byte_Mult                        avgt   5        3,26 ±      0,11 ns/op        0,57
byte_OR                          avgt   5        3,17 ±      0,05 ns/op        0,48
byte_Rem                         avgt   5        5,34 ±      0,13 ns/op        2,66
byte_ShiftL                      avgt   5        3,01 ±      0,09 ns/op        0,32
byte_ShiftR                      avgt   5        3,01 ±      0,12 ns/op        0,32
byte_Substract                   avgt   5        3,22 ±      0,15 ns/op        0,53
byte_UShiftR                     avgt   5        3,03 ±      0,18 ns/op        0,34
byte_XOR                         avgt   5        3,24 ±      0,53 ns/op        0,55
byte_compare                     avgt   5        3,20 ±      0,08 ns/op        0,52
byte_equals                      avgt   5        3,23 ±      0,12 ns/op        0,54
byte_incr                        avgt   5        3,08 ±      0,50 ns/op        0,39
byte_toString                    avgt   5       25,53 ±      0,61 ns/op       22,85
byte_toWrapperCached             avgt   5        3,08 ±      0,14 ns/op        0,39
byte_SumLoop100000               avgt   5      62.297 ±     1.518 ns/op      62.294      0,62
byte_MultLoop100000              avgt   5     124.208 ±     2.466 ns/op     124.205      1,24

short_AND                        avgt   5        3,19 ±      0,14 ns/op        0,51
short_Add                        avgt   5        3,18 ±      0,13 ns/op        0,49
short_Div                        avgt   5        5,38 ±      0,05 ns/op        2,70
short_Mult                       avgt   5        3,38 ±      0,07 ns/op        0,69
short_OR                         avgt   5        3,24 ±      0,13 ns/op        0,55
short_Rem                        avgt   5        5,79 ±      0,12 ns/op        3,10
short_ShiftL                     avgt   5        3,00 ±      0,08 ns/op        0,32
short_ShiftR                     avgt   5        3,00 ±      0,07 ns/op        0,31
short_Substract                  avgt   5        3,19 ±      0,25 ns/op        0,50
short_UShiftR                    avgt   5        3,01 ±      0,12 ns/op        0,32
short_XOR                        avgt   5        3,16 ±      0,16 ns/op        0,47
short_compare                    avgt   5        3,16 ±      0,09 ns/op        0,47
short_equals                     avgt   5        3,17 ±      0,16 ns/op        0,49
short_incr                       avgt   5        3,20 ±      0,09 ns/op        0,51
short_toString                   avgt   5       30,76 ±      0,54 ns/op       28,07
short_toWrapperCached            avgt   5        3,50 ±      0,12 ns/op        0,81
short_toWrapperUnCached          avgt   5        4,87 ±      0,05 ns/op        2,18
short_SumLoop100000              avgt   5      62.240 ±     1.074 ns/op      62.237      0,62
short_MultLoop100000             avgt   5     124.399 ±     3.368 ns/op     124.396      1,24

char_AND                         avgt   5        3,20 ±      0,14 ns/op        0,52
char_Add                         avgt   5        3,16 ±      0,07 ns/op        0,47
char_Div                         avgt   5        5,38 ±      0,14 ns/op        2,69
char_Mult                        avgt   5        3,33 ±      0,05 ns/op        0,64
char_OR                          avgt   5        3,25 ±      0,21 ns/op        0,57
char_Rem                         avgt   5        5,40 ±      0,29 ns/op        2,72
char_ShiftL                      avgt   5        3,04 ±      0,14 ns/op        0,36
char_ShiftR                      avgt   5        3,02 ±      0,16 ns/op        0,33
char_Substract                   avgt   5        3,23 ±      0,14 ns/op        0,55
char_UShiftR                     avgt   5        3,08 ±      0,14 ns/op        0,40
char_XOR                         avgt   5        3,16 ±      0,09 ns/op        0,47
char_compare                     avgt   5        3,06 ±      0,11 ns/op        0,38
char_equals                      avgt   5        3,18 ±      0,10 ns/op        0,50
char_incr                        avgt   5        3,09 ±      0,08 ns/op        0,40
char_toString                    avgt   5        8,59 ±      0,11 ns/op        5,90
char_toWrapperCached             avgt   5        3,12 ±      0,10 ns/op        0,43
char_toWrapperUnCached           avgt   5        4,83 ±      0,06 ns/op        2,14
char_SumLoop100000               avgt   5      62.436 ±     1.896 ns/op      62.433      0,62
char_MultLoop100000              avgt   5     124.209 ±     1.925 ns/op     124.206      1,24

int_AND                          avgt   5        2,77 ±      0,24 ns/op        0,08
int_Add                          avgt   5        2,78 ±      0,10 ns/op        0,09
int_Div                          avgt   5        5,06 ±      0,19 ns/op        2,37
int_Mult                         avgt   5        2,80 ±      0,06 ns/op        0,11
int_OR                           avgt   5        2,76 ±      0,18 ns/op        0,07
int_Rem                          avgt   5        5,04 ±      0,11 ns/op        2,36
int_ShiftL                       avgt   5        2,98 ±      0,78 ns/op        0,29
int_ShiftR                       avgt   5        2,87 ±      0,09 ns/op        0,18
int_Substract                    avgt   5        2,76 ±      0,06 ns/op        0,07
int_UShiftR                      avgt   5        2,91 ±      0,11 ns/op        0,22
int_XOR                          avgt   5        2,78 ±      0,16 ns/op        0,09
int_bitCount                     avgt   5        2,84 ±      0,13 ns/op        0,15
int_compare                      avgt   5        3,35 ±      0,08 ns/op        0,66
int_equals                       avgt   5        3,19 ±      0,07 ns/op        0,51
int_incr                         avgt   5        2,94 ±      0,10 ns/op        0,25
int_leadingZeros                 avgt   5        3,44 ±      0,05 ns/op        0,76
int_rotateLeft                   avgt   5        2,90 ±      0,11 ns/op        0,22
int_rotateRight                  avgt   5        2,91 ±      0,19 ns/op        0,23
int_toHexString                  avgt   5       43,85 ±      0,40 ns/op       41,16
int_toString                     avgt   5       40,61 ±      0,61 ns/op       37,92
int_toWrapperCached              avgt   5        3,51 ±      0,12 ns/op        0,82
int_toWrapperUnCached            avgt   5        4,95 ±      0,16 ns/op        2,26
int_SumLoop100000                avgt   5       2.259 ±       229 ns/op       2.257      0,02
int_MultLoop100000               avgt   5      93.411 ±     2.746 ns/op      93.409      0,93

long_AND                         avgt   5        2,76 ±      0,16 ns/op        0,07
long_Add                         avgt   5        2,80 ±      0,15 ns/op        0,12
long_Div                         avgt   5       12,58 ±      0,51 ns/op        9,90
long_Mult                        avgt   5        2,83 ±      0,15 ns/op        0,14
long_OR                          avgt   5        2,77 ±      0,16 ns/op        0,09
long_Rem                         avgt   5       12,57 ±      0,40 ns/op        9,88
long_ShiftL                      avgt   5        3,02 ±      0,08 ns/op        0,33
long_ShiftR                      avgt   5        2,98 ±      0,21 ns/op        0,29
long_Substract                   avgt   5        2,78 ±      0,09 ns/op        0,09
long_UShiftR                     avgt   5        2,89 ±      0,22 ns/op        0,20
long_XOR                         avgt   5        2,79 ±      0,04 ns/op        0,10
long_bitCount                    avgt   5        3,03 ±      0,11 ns/op        0,34
long_compare                     avgt   5        3,41 ±      0,11 ns/op        0,72
long_equals                      avgt   5        3,18 ±      0,06 ns/op        0,49
long_incr                        avgt   5        3,93 ±      0,16 ns/op        1,25
long_leadingZeros                avgt   5        3,38 ±      0,15 ns/op        0,69
long_rotateLeft                  avgt   5        2,90 ±      0,12 ns/op        0,21
long_rotateRight                 avgt   5        2,87 ±      0,11 ns/op        0,18
long_toHexString                 avgt   5       61,63 ±      1,28 ns/op       58,94
long_toString                    avgt   5       48,61 ±      1,72 ns/op       45,92
long_toWrapperCached             avgt   5        3,80 ±      0,10 ns/op        1,11
long_toWrapperUnCached           avgt   5        5,27 ±      0,14 ns/op        2,58
long_SumLoop100000               avgt   5      31.421 ±       369 ns/op      31.418      0,31
long_multLoop100000              avgt   5      93.604 ±     2.278 ns/op      93.601      0,94

float_Add                        avgt   5        3,17 ±      0,04 ns/op        0,48
float_Div                        avgt   5        3,58 ±      0,16 ns/op        0,89
float_Mod                        avgt   5       28,53 ±      1,08 ns/op       25,84
float_Mult                       avgt   5        3,31 ±      0,20 ns/op        0,62
float_Substract                  avgt   5        3,14 ±      0,10 ns/op        0,45
float_compare                    avgt   5        3,53 ±      0,19 ns/op        0,85
float_equals                     avgt   5        3,25 ±      0,09 ns/op        0,57
float_incr                       avgt   5        3,37 ±      0,17 ns/op        0,68
float_toString                   avgt   5       78,67 ±      0,39 ns/op       75,98
float_toWrapper                  avgt   5        4,90 ±      0,08 ns/op        2,21
float_SumLoop100000              avgt   5      92.922 ±     2.482 ns/op      92.919      0,93
float_multLoop100000             avgt   5     155.345 ±     4.411 ns/op     155.342      1,55

double_Add                       avgt   5        3,22 ±      0,17 ns/op        0,53
double_Div                       avgt   5        4,38 ±      0,11 ns/op        1,69
double_Mod                       avgt   5       26,17 ±      2,26 ns/op       23,49
double_Mult                      avgt   5        3,45 ±      0,09 ns/op        0,76
double_Substract                 avgt   5        3,36 ±      0,08 ns/op        0,67
double_compare                   avgt   5        3,46 ±      0,07 ns/op        0,77
double_equals                    avgt   5        3,31 ±      0,12 ns/op        0,62
double_incr                      avgt   5        3,56 ±      1,13 ns/op        0,87
double_toString                  avgt   5      294,02 ±      2,57 ns/op      291,34
double_toWrapper                 avgt   5        5,11 ±      0,04 ns/op        2,43
double_SumLoop100000             avgt   5      93.768 ±     2.477 ns/op      93.765      0,94
double_multLoop100000            avgt   5     156.000 ±     4.911 ns/op     155.997      1,56
Math:
double_SQRT         		     avgt    5       6,23 ±     0,14 ns/op
double_pow0_5         		     avgt    5      73,93 ±     5,45 ns/op
double_pow1_5         		     avgt    5      72,74 ±     1,64 ns/op
double_pow2           		     avgt    5       3,35 ±     0,94 ns/op
double_powX           		     avgt    5      73,81 ±     2,21 ns/op


bigDecimal_LongMax_Add           avgt   5       22,02 ±      0,20 ns/op       19,33
bigDecimal_LongMax_Mult          avgt   5       37,46 ±      0,22 ns/op       34,77
bigDecimal_LongMax_Rem           avgt   5      775,57 ±     16,21 ns/op      772,88
bigDecimal_LongMax_Substract     avgt   5        8,56 ±      0,27 ns/op        5,87
bigDecimal_LongMax_Div128        avgt   5    1.561,86 ±     27,72 ns/op    1.559,17
bigDecimal_LongMax_Div64         avgt   5       42,10 ±      0,16 ns/op       39,41
bigDecimal_LongMax_toString      avgt   5        3,09 ±      0,11 ns/op        0,40

bigDecimal_RealyBig_Add          avgt   5       40,80 ±      8,55 ns/op       38,12
bigDecimal_RealyBig_Div128       avgt   5      371,90 ±     14,43 ns/op      369,22
bigDecimal_RealyBig_Div64        avgt   5      325,59 ±      3,14 ns/op      322,91
bigDecimal_RealyBig_Mult         avgt   5       62,49 ±      1,45 ns/op       59,80
bigDecimal_RealyBig_Rem          avgt   5    1.134,57 ±      7,50 ns/op    1.131,89
bigDecimal_RealyBig_Substract    avgt   5       38,77 ±      0,86 ns/op       36,08
bigDecimal_RealyBig_toString     avgt   5        2,96 ±      0,09 ns/op        0,27

bigDecimal_small_Add             avgt   5        8,30 ±      0,15 ns/op        5,61
bigDecimal_small_Div128          avgt   5    2.576,21 ±     46,31 ns/op    2.573,52
bigDecimal_small_Div64           avgt   5       77,24 ±      1,30 ns/op       74,55
bigDecimal_small_Mult            avgt   5        8,45 ±      0,23 ns/op        5,76
bigDecimal_small_Rem             avgt   5      110,95 ±      0,61 ns/op      108,26
bigDecimal_small_Substract       avgt   5        8,42 ±      0,09 ns/op        5,74
bigDecimal_small_toString        avgt   5        3,10 ±      0,71 ns/op        0,42
bigDecimal_small_SumLoop100000   avgt   5     485.872 ±     9.213 ns/op     485.869      4,86
bigDecimal_small_MultLoop100000  avgt   5 508.446.607 ± 5.532.820 ns/op 508.446.604  5.084,47 <--number gets really big (no overflow as wit int/long)

bigInteger_LongMax_Add           avgt   5       25,53 ±      0,13 ns/op       22,84
bigInteger_LongMax_Mod           avgt   5       71,62 ±      0,84 ns/op       68,93
bigInteger_LongMax_Mult          avgt   5       27,61 ±      0,67 ns/op       24,92
bigInteger_LongMax_Rem           avgt   5       78,73 ±      1,72 ns/op       76,04
bigInteger_LongMax_Substract     avgt   5       28,52 ±      0,33 ns/op       25,83
bigInteger_LongMax_XOR           avgt   5       35,64 ±      0,55 ns/op       32,95
bigInteger_LongMax_Div           avgt   5       69,17 ±      1,08 ns/op       66,48
bigInteger_LongMax_shiftL        avgt   5       27,03 ±      0,42 ns/op       24,34
bigInteger_LongMax_shiftR        avgt   5       27,52 ±      0,28 ns/op       24,83
bigInteger_LongMax_toString      avgt   5      377,76 ±      6,87 ns/op      375,07

bigInteger_RealyBig_Add          avgt   5       29,60 ±      0,09 ns/op       26,92
bigInteger_RealyBig_Div          avgt   5      192,27 ±      0,81 ns/op      189,58
bigInteger_RealyBig_Mod          avgt   5      223,67 ±      2,74 ns/op      220,98
bigInteger_RealyBig_Mult         avgt   5       59,88 ±      0,98 ns/op       57,19
bigInteger_RealyBig_Rem          avgt   5      240,56 ±      7,09 ns/op      237,88
bigInteger_RealyBig_Substract    avgt   5       31,86 ±      0,32 ns/op       29,18
bigInteger_RealyBig_XOR          avgt   5       45,38 ±      0,64 ns/op       42,70
bigInteger_RealyBig_shiftL       avgt   5       30,57 ±      0,60 ns/op       27,89
bigInteger_RealyBig_shiftR       avgt   5       33,62 ±      0,45 ns/op       30,93
bigInteger_RealyBig_toString     avgt   5      689,79 ±      6,59 ns/op      687,10

bigInteger_small_Add             avgt   5       25,02 ±      0,28 ns/op       22,33
bigInteger_small_Div             avgt   5       34,42 ±      0,29 ns/op       31,73
bigInteger_small_Mod             avgt   5       36,90 ±      0,92 ns/op       34,21
bigInteger_small_Mult            avgt   5       33,53 ±      0,50 ns/op       30,84
bigInteger_small_Rem             avgt   5       42,42 ±      0,71 ns/op       39,74
bigInteger_small_Substract       avgt   5       27,41 ±      0,74 ns/op       24,72
bigInteger_small_XOR             avgt   5       31,72 ±      0,47 ns/op       29,04
bigInteger_small_shiftL          avgt   5       23,45 ±      0,36 ns/op       20,76
bigInteger_small_shiftR          avgt   5       25,84 ±      0,38 ns/op       23,15
bigInteger_small_toString        avgt   5      104,87 ±      1,03 ns/op      102,18
bigInteger_small_SumLoop100000   avgt   5   2.067.475 ±    20.538 ns/op   2.067.472     20,67
bigInteger_small_MultLoop100000  avgt   5 480.779.054 ± 1.986.572 ns/op 480.779.052  4.807,79 <--number gets really big (no overflow as wit int/long)
*/
/**
 * @author Michael Frank
 * @version 1.0 05.12.2016
 */
@SuppressWarnings("UnnecessaryBoxing")
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
public class BasicJavaOpsJMH {


	public byte incrementB;
	public short incrementS;
	public char incrementC;
	public int incrementI;
	public long incrementL;
	public float incrementF;
	public double incrementD;

	public byte xB, yB;
	public short xS, yS, xSBig;
	public char xC, yC, xCBig;
	public int xI, yI, xIBig;
	public long xL, yL, xLBig;
	public float xF, yF, xFBig;
	public double xD, yD, xDBig;

	public BigInteger xBigI, yBigI;
	public BigDecimal xBigD, yBigD;
	public BigInteger xBigI_LongMax;
	public BigInteger yBigI_LongMax;
	public BigDecimal xBigD_LongMax;
	public BigDecimal yBigD_LongMax;

	public BigInteger xBigI_RealyBig_;
	public BigInteger yBigI_RealyBig_;
	public BigDecimal xBigD_RealyBig;
	public BigDecimal yBigD_RealyBig;

	int loopIterations;
	Object someInteger;
	Object isNull;
	Object isNotNull;

	@Setup
	public void setup() {
		incrementB = 0;
		incrementS = 0;
		incrementC = 0;
		incrementI = 0;
		incrementL = 0;
		incrementF = 0;
		incrementD = 0;
		xB = 103;// must be < 128
		yB = 5; // at least < 8 but lower x
		xS = 103; // must be < 128
		yS = 5; // at least < 16 but lower x
		xSBig = Short.MAX_VALUE - 10;
		xC = 103;// must be < 128
		yC = 5;
		xCBig = Character.MAX_VALUE - 10;
		xI = 103;// must be < 128
		yI = 5; // at least < 8 (used as shift for byte,short,char,int,long)
		xIBig = Integer.MAX_VALUE - 10;
		xL = 103L;// must be < 128
		yL = 5L;
		xLBig = Long.MAX_VALUE - 10;
		xF = 103.0F;
		yF = 5.0F;
		xFBig = xIBig;
		xD = 103.0D;
		yD = 5.0D;
		xDBig = xLBig;
		xBigI = BigInteger.valueOf(103);
		yBigI = BigInteger.valueOf(5);
		xBigD = BigDecimal.valueOf(103);
		yBigD = BigDecimal.valueOf(5);
		xBigI_LongMax = BigInteger.valueOf(Long.MAX_VALUE);
		yBigI_LongMax = BigInteger.valueOf(5);
		xBigD_LongMax = BigDecimal.valueOf(Long.MAX_VALUE);
		yBigD_LongMax = BigDecimal.valueOf(5);
		// x > LongMax
		xBigI_RealyBig_ = new BigInteger("892233720368547758079223372036854775807");
		yBigI_RealyBig_ = new BigInteger("19223372036854775807");
		xBigD_RealyBig = new BigDecimal("892233720368547758079223372036854775807");
		yBigD_RealyBig = new BigDecimal("19223372036854775807");

		loopIterations = 100000;

		someInteger = Integer.valueOf(23452345);
		isNull = null;
		isNotNull = new Object();
	}

	// @Group("baseline")
	@Benchmark
	public void noOpBaseline() {
		// this method was intentionally left blank.
	}

	// @Group("baseline")
	@Benchmark
	public int noOpReturnCostBaseline() {
		return xI;
	}

	// @Group("byte")
	@Benchmark
	public byte byte_incr() {
		return incrementB++;
	}

	// @Group("byte")
	@Benchmark
	public boolean byte_equals() {
		return xB == yB;
	}

	// @Group("byte")
	@Benchmark
	public int byte_compare() {
		return Byte.compare(xB, yB);
	}

	// @Group("byte")
	@Benchmark
	public Byte byte_toWrapperCached() {
		return Byte.valueOf(xB);
	}

	// @Group("byte")
	@Benchmark
	public String byte_toString() {
		return Byte.toString(xB);
	}

	// @Group("byte")
	@Benchmark
	public byte byte_Add() {
		return (byte) (xB + yB);
	}

	// @Group("byte")
	@Benchmark
	public byte byte_Substract() {
		return (byte) (xB - yB);
	}

	// @Group("byte")
	@Benchmark
	public byte byte_Mult() {
		return (byte) (xB * yB);
	}

	// @Group("byte")
	@Benchmark
	public byte byte_Div() {
		return (byte) (xB / yB);
	}

	// @Group("byte")
	@Benchmark
	public byte byte_Rem() {
		return (byte) (xB % yB);
	}

	// @Group("byte")
	@Benchmark
	public byte byte_XOR() {
		return (byte) (xB ^ yB);
	}

	// @Group("byte")
	@Benchmark
	public byte byte_AND() {
		return (byte) (xB & yB);
	}

	// @Group("byte")
	@Benchmark
	public byte byte_OR() {
		return (byte) (xB | yB);
	}

	// @Group("byte")
	@Benchmark
	public byte byte_ShiftL() {
		return (byte) (xB << yI);
	}

	// @Group("byte")
	@Benchmark
	public byte byte_ShiftR() {
		return (byte) (xB >> yI);
	}

	// @Group("byte")
	@Benchmark
	public byte byte_UShiftR() {
		return (byte) (xB >>> yI);
	}

	// JVM magic will optimize this loop.
	// @Group("byte")
	@Benchmark
	public byte byte_SumLoop_100000() {
		byte result = 0;
		for (int i = 0; i < loopIterations; i++) {
			result += yB;
		}
		return result;
	}

	// JVM magic will optimize this loop.
	// @Group("byte")
	@Benchmark
	public byte byte_MultLoop_100000() {
		byte result = 1;
		for (int i = 0; i < loopIterations; i++) {
			result *= yB;
		}
		return result;
	}

	// @Group("byte")
	@Benchmark
	public short short_incr() {
		return incrementS++;
	}

	// @Group("short")
	@Benchmark
	public boolean short_equals() {
		return xS == yS;
	}

	// @Group("short")
	@Benchmark
	public int short_compare() {
		return Short.compare(xS, yS);
	}

	// @Group("short")
	@Benchmark
	public Short short_toWrapperCached() {
		return Short.valueOf(xS);
	}

	// @Group("short")
	@Benchmark
	public Short short_toWrapperUnCached() {
		return Short.valueOf(xSBig);
	}

	// @Group("short")
	@Benchmark
	public String short_toString() {
		return Short.toString(xSBig);
	}

	// @Group("short")
	@Benchmark
	public short short_Add() {
		return (short) (xS + yS);
	}

	// @Group("short")
	@Benchmark
	public short short_Substract() {
		return (short) (xS - yS);
	}

	// @Group("short")
	@Benchmark
	public short short_Mult() {
		return (short) (xS * yS);
	}

	// @Group("short")
	@Benchmark
	public short short_Div() {
		return (short) (xS / yS);
	}

	// @Group("short")
	@Benchmark
	public short short_Rem() {
		return (short) (xS % yS);
	}

	// @Group("short")
	@Benchmark
	public short short_XOR() {
		return (short) (xS ^ yS);
	}

	// @Group("short")
	@Benchmark
	public short short_AND() {
		return (short) (xS & yS);
	}

	// @Group("short")
	@Benchmark
	public short short_OR() {
		return (short) (xS | yS);
	}

	// @Group("short")
	@Benchmark
	public short short_ShiftL() {
		return (short) (xS << yI);
	}

	// @Group("short")
	@Benchmark
	public short short_ShiftR() {
		return (short) (xS >> yI);
	}

	// @Group("short")
	@Benchmark
	public short short_UShiftR() {
		return (short) (xS >>> yI);
	}

	// JVM magic will optimize this loop.
	// @Group("short")
	@Benchmark
	public short short_SumLoop_100000() {
		short result = 0;
		for (int i = 0; i < loopIterations; i++) {
			result += yS;
		}
		return result;
	}

	// JVM magic will optimize this loop.
	// @Group("short")
	@Benchmark
	public short short_MultLoop_100000() {
		short result = 1;
		for (int i = 0; i < loopIterations; i++) {
			result *= yS;
		}
		return result;
	}

	// @Group("byte")
	@Benchmark
	public char char_incr() {
		return incrementC++;
	}

	// @Group("char")
	@Benchmark
	public boolean char_equals() {
		return xC == yC;
	}

	// @Group("char")
	@Benchmark
	public int char_compare() {
		return Character.compare(xC, yC);
	}

	// @Group("char")
	@Benchmark
	public Character char_toWrapperCached() {
		return Character.valueOf(xC);
	}

	// @Group("char")
	@Benchmark
	public Character char_toWrapperUnCached() {
		return Character.valueOf(xCBig);
	}

	// @Group("char")
	@Benchmark
	public String char_toString() {
		return Character.toString(xCBig);
	}

	// @Group("char")
	@Benchmark
	public char char_Add() {
		return (char) (xC + yC);
	}

	// @Group("char")
	@Benchmark
	public char char_Substract() {
		return (char) (xC - yC);
	}

	// @Group("char")
	@Benchmark
	public char char_Mult() {
		return (char) (xC * yC);
	}

	// @Group("char")
	@Benchmark
	public char char_Div() {
		return (char) (xC / yC);
	}

	// @Group("char")
	@Benchmark
	public char char_Rem() {
		return (char) (xC % yC);
	}

	// @Group("char")
	@Benchmark
	public char char_XOR() {
		return (char) (xC ^ yC);
	}

	// @Group("char")
	@Benchmark
	public char char_AND() {
		return (char) (xC & yC);
	}

	// @Group("char")
	@Benchmark
	public char char_OR() {
		return (char) (xC | yC);
	}

	// @Group("char")
	@Benchmark
	public char char_ShiftL() {
		return (char) (xC << yI);
	}

	// @Group("char")
	@Benchmark
	public char char_ShiftR() {
		return (char) (xC >> yI);
	}

	// @Group("char")
	@Benchmark
	public char char_UShiftR() {
		return (char) (xC >>> yI);
	}

	// JVM magic will optimize this loop.
	// @Group("char")
	@Benchmark
	public char char_SumLoop_100000() {
		char result = 0;
		for (int i = 0; i < loopIterations; i++) {
			result += yC;
		}
		return result;
	}

	// JVM magic will optimize this loop.
	// @Group("char")
	@Benchmark
	public char char_MultLoop_100000() {
		char result = 1;
		for (int i = 0; i < loopIterations; i++) {
			result *= yC;
		}
		return result;
	}

	// @Group("int")
	@Benchmark
	public int int_incr() {
		return incrementI++;
	}

	// @Group("int")
	@Benchmark
	public boolean int_equals() {
		return xI == yI;
	}

	// @Group("int")
	@Benchmark
	public int int_compare() {
		return Integer.compare(xI, yI);
	}

	// @Group("int")
	@Benchmark
	public Integer int_toWrapperCached() {
		return Integer.valueOf(xI);
	}

	// @Group("int")
	@Benchmark
	public Integer int_toWrapperUnCached() {
		return Integer.valueOf(xIBig);
	}

	// @Group("int")
	@Benchmark
	public String int_toString() {
		return Integer.toString(xIBig);
	}

	// @Group("int")
	@Benchmark
	public String int_toHexString() {
		return Integer.toString(xIBig, 16);
	}

	// @Group("int")
	@Benchmark
	public int int_Add() {
		return xI + yI;
	}

	// @Group("int")
	@Benchmark
	public int int_Substract() {
		return xI - yI;
	}

	// @Group("int")
	@Benchmark
	public int int_Mult() {
		return xI * yI;
	}

	// @Group("int")
	@Benchmark
	public int int_Div() {
		return xI / yI;
	}

	// @Group("int")
	@Benchmark
	public int int_Rem() {
		return xI % yI;
	}

	// @Group("int")
	@Benchmark
	public int int_XOR() {
		return xI ^ yI;
	}

	// @Group("int")
	@Benchmark
	public int int_AND() {
		return xI & yI;
	}

	// @Group("int")
	@Benchmark
	public int int_OR() {
		return xI | yI;
	}

	// @Group("int")
	@Benchmark
	public int int_ShiftL() {
		return xI << yI;
	}

	// @Group("int")
	@Benchmark
	public int int_ShiftR() {
		return xI >> yI;
	}

	// @Group("int")
	@Benchmark
	public int int_UShiftR() {
		return xI >>> yI;
	}

	// @Group("int")
	@Benchmark
	public int int_rotateLeft() {
		return Integer.rotateLeft(xI, yI);
	}

	// @Group("int")
	@Benchmark
	public int int_rotateRight() {
		return Integer.rotateRight(xI, yI);
	}

	// @Group("int")
	@Benchmark
	public int int_bitCount() {
		return Integer.bitCount(xI);
	}

	// @Group("int")
	@Benchmark
	public int int_leadingZeros() {
		return Integer.numberOfLeadingZeros(xI);
	}

	// JVM magic will optimize this loop.
	// @Group("int")
	@Benchmark
	public int int_SumLoop_100000() {
		int result = 0;
		for (int i = 0; i < loopIterations; i++) {
			result += yI;
		}
		return result;
	}

	// JVM magic will optimize this loop.
	// @Group("int")
	@Benchmark
	public int int_MultLoop_100000() {
		int result = 1;
		for (int i = 0; i < loopIterations; i++) {
			result *= yI;
		}
		return result;
	}

	// @Group("byte")
	@Benchmark
	public long long_incr() {
		return incrementL++;
	}

	// @Group("long")
	@Benchmark
	public boolean long_equals() {
		return xI == yI;
	}

	// @Group("long")
	@Benchmark
	public int long_compare() {
		return Long.compare(xL, yL);
	}

	// @Group("long")
	@Benchmark
	public Long long_toWrapperCached() {
		return Long.valueOf(xL);
	}

	// @Group("long")
	@Benchmark
	public Long long_toWrapperUnCached() {
		return Long.valueOf(xLBig);
	}

	// @Group("long")
	@Benchmark
	public String long_toString() {
		return Long.toString(xLBig);
	}

	// @Group("long")
	@Benchmark
	public String long_toHexString() {
		return Long.toString(xLBig, 16);
	}

	// @Group("long")
	@Benchmark
	public long long_Add() {
		return xL + yL;
	}

	// @Group("long")
	@Benchmark
	public long long_Substract() {
		return xL - yL;
	}

	// @Group("long")
	@Benchmark
	public long long_Mult() {
		return xL * yL;
	}

	// @Group("long")
	@Benchmark
	public long long_Div() {
		return xL / yL;
	}

	// @Group("long")
	@Benchmark
	public long long_Rem() {
		return xL % yL;
	}

	// @Group("long")
	@Benchmark
	public long long_XOR() {
		return xL ^ yL;
	}

	// @Group("long")
	@Benchmark
	public long long_AND() {
		return xL & yL;
	}

	// @Group("long")
	@Benchmark
	public long long_OR() {
		return xL | yL;
	}

	// @Group("long")
	@Benchmark
	public long long_ShiftL() {
		return xL << yI;
	}

	// @Group("long")
	@Benchmark
	public long long_ShiftR() {
		return xL >> yI;
	}

	// @Group("long")
	@Benchmark
	public long long_UShiftR() {
		return xL >>> yI;
	}

	// @Group("long")
	@Benchmark
	public long long_rotateLeft() {
		return Long.rotateLeft(xL, yI);
	}

	// @Group("long")
	@Benchmark
	public long long_rotateRight() {
		return Long.rotateRight(xL, yI);
	}

	// @Group("long")
	@Benchmark
	public long long_bitCount() {
		return Long.bitCount(xL);
	}

	// @Group("long")
	@Benchmark
	public long long_leadingZeros() {
		return Long.numberOfLeadingZeros(xL);
	}

	// JVM magic will optimize this loop.
	// @Group("int")
	@Benchmark
	public long long_SumLoop_100000() {
		long result = 0;
		for (int i = 0; i < loopIterations; i++) {
			result += yL;
		}
		return result;
	}

	// JVM magic will optimize this loop.
	// @Group("int")
	@Benchmark
	public long long_multLoop_100000() {
		long result = 1;
		for (int i = 0; i < loopIterations; i++) {
			result *= yL;
		}
		return result;
	}

	// @Group("float")
	@Benchmark
	public float float_incr() {
		return incrementF++;
	}

	// @Group("float")
	@Benchmark
	public boolean float_equals() {
		return xF == yF; // BAD!!!! never do this
	}

	// @Group("float")
	@Benchmark
	public int float_compare() {
		return Float.compare(xF, yF);
	}

	// @Group("float")
	@Benchmark
	public Float float_toWrapper() {
		return Float.valueOf(xFBig);
	}

	// @Group("float")
	@Benchmark
	public String float_toString() {
		return Float.toString(xFBig);
	}

	// @Group("float")
	@Benchmark
	public float float_Add() {
		return xF + yF;
	}

	// @Group("float")
	@Benchmark
	public float float_Substract() {
		return xF - yF;
	}

	// @Group("float")
	@Benchmark
	public float float_Mult() {
		return xF * yF;
	}

	// @Group("float")
	@Benchmark
	public float float_Div() {
		return xF / yF;
	}

	// @Group("float")
	@Benchmark
	public float float_Mod() {
		return xF % yF;
	}

	// JVM magic will optimize this loop.
	// @Group("int")
	@Benchmark
	public float float_SumLoop_100000() {
		float result = 0;
		for (int i = 0; i < loopIterations; i++) {
			result += yF;
		}
		return result;
	}

	// JVM magic will optimize this loop.
	// @Group("int")
	@Benchmark
	public float float_multLoop_100000() {
		float result = 1;
		for (int i = 0; i < loopIterations; i++) {
			result *= yF;
		}
		return result;
	}

	// @Group("double")
	@Benchmark
	public double double_incr() {
		return incrementD++;
	}

	// @Group("double")
	@Benchmark
	public boolean double_equals() {
		return xD == yD; // BAD!!!! never do this
	}

	// @Group("double")
	@Benchmark
	public int double_compare() {
		return Double.compare(xD, yD);
	}

	// @Group("double")
	@Benchmark
	public Double double_toWrapper() {
		return Double.valueOf(xDBig);
	}

	// @Group("double")
	@Benchmark
	public String double_toString() {
		return Double.toString(xDBig);
	}

	// @Group("double")
	@Benchmark
	public double double_Add() {
		return xD + yD;
	}

	// @Group("double")
	@Benchmark
	public double double_Substract() {
		return xD - yD;
	}

	// @Group("double")
	@Benchmark
	public double double_Mult() {
		return xD * yD;
	}

	// @Group("double")
	@Benchmark
	public double double_Div() {
		return xD / yD;
	}

	// @Group("double")
	@Benchmark
	public double double_Mod() {
		return xD % yD;
	}

	// JVM magic will optimize this loop.
	// @Group("bigInteger")
	@Benchmark
	public double double_SumLoop_100000() {
		double result = 0;
		for (int i = 0; i < loopIterations; i++) {
			result += yD;
		}
		return result;
	}

	// JVM magic will optimize this loop.
	// @Group("bigInteger")
	@Benchmark
	public double double_multLoop_100000() {
		double result = 1;
		for (int i = 0; i < loopIterations; i++) {
			result *= yD;
		}
		return result;
	}

	// @Group("bigInteger")
	@Benchmark
	public String bigInteger_small_toString() {
		return xBigI.toString(10);
	}

	// @Group("bigInteger")
	@Benchmark
	public BigInteger bigInteger_small_Add() {
		return xBigI.add(yBigI);
	}

	// @Group("bigInteger")
	@Benchmark
	public BigInteger bigInteger_small_Substract() {
		return xBigI.subtract(yBigI);
	}

	// @Group("bigInteger")
	@Benchmark
	public BigInteger bigInteger_small_Mult() {
		return xBigI.multiply(yBigI);
	}

	// @Group("bigInteger")
	@Benchmark
	public BigInteger bigInteger_small_Div() {
		return xBigI.divide(yBigI);
	}

	// @Group("bigInteger")
	@Benchmark
	public BigInteger bigInteger_small_Rem() {
		return xBigI.divideAndRemainder(yBigI)[1];
	}

	// @Group("bigInteger")
	@Benchmark
	public BigInteger bigInteger_small_Mod() {
		return xBigI.mod(yBigI);
	}

	// @Group("bigInteger")
	@Benchmark
	public BigInteger bigInteger_small_XOR() {
		return xBigI.xor(yBigI);
	}

	// @Group("bigInteger")
	@Benchmark
	public BigInteger bigInteger_small_shiftL() {
		return xBigI.shiftLeft(yI);
	}

	// @Group("bigInteger")
	@Benchmark
	public BigInteger bigInteger_small_shiftR() {
		return xBigI.shiftRight(yI);
	}

	// @Group("bigIntegerLongMax")
	@Benchmark
	public String bigInteger_LongMax_toString() {
		return xBigI_LongMax.toString(10);
	}

	// @Group("bigIntegerLongMax")
	@Benchmark
	public BigInteger bigInteger_LongMax_Add() {
		return xBigI_LongMax.add(yBigI_LongMax);
	}

	// @Group("bigIntegerLongMax")
	@Benchmark
	public BigInteger bigInteger_LongMax_Substract() {
		return xBigI_LongMax.subtract(yBigI_LongMax);
	}

	// JVM magic will optimize this loop.
	// @Group("bigIntegerLongMax")
	@Benchmark
	public BigInteger bigInteger_SumLoop_100000() {
		BigInteger result = BigInteger.ONE;
		for (int i = 0; i < loopIterations; i++) {
			result = result.add(yBigI);
		}
		return result;
	}

	// JVM magic will optimize this loop.
	// @Group("bigIntegerLongMax")
	@Benchmark
	public BigInteger bigInteger_MultLoop_100000() {
		BigInteger result = BigInteger.ONE;
		for (int i = 0; i < loopIterations; i++) {
			result = result.multiply(yBigI);
		}
		return result;
	}

	// @Group("bigIntegerLongMax")
	@Benchmark
	public BigInteger bigInteger_LongMax_Mult() {
		return xBigI_LongMax.multiply(yBigI_LongMax);
	}

	// @Group("bigIntegerLongMax")
	@Benchmark
	public BigInteger bigInteger_LongMax_Div() {
		return xBigI_LongMax.divide(yBigI_LongMax);
	}

	// @Group("bigIntegerLongMax_")
	@Benchmark
	public BigInteger bigInteger_LongMax_Rem() {
		return xBigI_LongMax.divideAndRemainder(yBigI_LongMax)[1];
	}

	// @Group("bigIntegerLongMax")
	@Benchmark
	public BigInteger bigInteger_LongMax_Mod() {
		return xBigI_LongMax.mod(yBigI_LongMax);
	}

	// @Group("bigIntegerLongMax")
	@Benchmark
	public BigInteger bigInteger_LongMax_XOR() {
		return xBigI_LongMax.xor(yBigI_LongMax);
	}

	// @Group("bigIntegerLongMax")
	@Benchmark
	public BigInteger bigInteger_LongMax_shiftL() {
		return xBigI_LongMax.shiftLeft(yI);
	}

	// @Group("bigIntegerLongMax")
	@Benchmark
	public BigInteger bigInteger_LongMax_shiftR() {
		return xBigI_LongMax.shiftRight(yI);
	}

	// @Group("bigIntegerRealyBig")
	@Benchmark
	public String bigInteger_RealyBig_toString() {
		return xBigI_RealyBig_.toString(10);
	}

	// @Group("bigIntegerRealyBig")
	@Benchmark
	public BigInteger bigInteger_RealyBig_Add() {
		return xBigI_RealyBig_.add(yBigI_RealyBig_);
	}

	// @Group("bigIntegerRealyBig")
	@Benchmark
	public BigInteger bigInteger_RealyBig_Substract() {
		return xBigI_RealyBig_.subtract(yBigI_RealyBig_);
	}

	// @Group("bigIntegerRealyBig")
	@Benchmark
	public BigInteger bigInteger_RealyBig_Mult() {
		return xBigI_RealyBig_.multiply(yBigI_RealyBig_);
	}

	// @Group("bigIntegerRealyBig")
	@Benchmark
	public BigInteger bigInteger_RealyBig_Div() {
		return xBigI_RealyBig_.divide(yBigI_RealyBig_);
	}

	// @Group("bigIntegerRealyBig")
	@Benchmark
	public BigInteger bigInteger_RealyBig_Rem() {
		return xBigI_RealyBig_.divideAndRemainder(yBigI_RealyBig_)[1];
	}

	// @Group("bigIntegerRealyBig")
	@Benchmark
	public BigInteger bigInteger_RealyBig_Mod() {
		return xBigI_RealyBig_.mod(yBigI_RealyBig_);
	}

	// @Group("bigIntegerRealyBig")
	@Benchmark
	public BigInteger bigInteger_RealyBig_XOR() {
		return xBigI_RealyBig_.xor(yBigI_RealyBig_);
	}

	// @Group("bigIntegerRealyBig")
	@Benchmark
	public BigInteger bigInteger_RealyBig_shiftL() {
		return xBigI_RealyBig_.shiftLeft(yI);
	}

	// @Group("bigIntegerRealyBig")
	@Benchmark
	public BigInteger bigInteger_RealyBig_shiftR() {
		return xBigI_RealyBig_.shiftRight(yI);
	}

	// @Group("bigDecimal")
	@Benchmark
	public String bigDecimal_small_toString() {
		return xBigD.toString();
	}

	// @Group("bigDecimal")
	@Benchmark
	public BigDecimal bigDecimal_small_Add() {
		return xBigD.add(yBigD);
	}

	// @Group("bigDecimal")
	@Benchmark
	public BigDecimal bigDecimal_small_Substract() {
		return xBigD.subtract(yBigD);
	}

	// @Group("bigDecimal")
	@Benchmark
	public BigDecimal bigDecimal_small_Mult() {
		return xBigD.multiply(yBigD);
	}

	// @Group("bigDecimal")
	@Benchmark
	public BigDecimal bigDecimal_small_Div128() {
		return xBigD.divide(yBigD, MathContext.DECIMAL128);
	}

	// @Group("bigDecimal")
	@Benchmark
	public BigDecimal bigDecimal_small_Div64() {
		return xBigD.divide(yBigD, MathContext.DECIMAL64);
	}

	// @Group("bigDecimal")
	@Benchmark
	public BigDecimal bigDecimal_small_Rem() {
		return xBigD.divideAndRemainder(yBigD)[1];
	}

	// JVM magic will optimize this loop.
	// @Group("bigDecimalLongMax")
	@Benchmark
	public BigDecimal bigDecimal_SumLoop_100000() {
		BigDecimal result = BigDecimal.ONE;
		for (int i = 0; i < loopIterations; i++) {
			result = result.add(yBigD);
		}
		return result;
	}

	// JVM magic will optimize this loop.
	// @Group("bigDecimalLongMax")
	@Benchmark
	public BigDecimal bigDecimal_MultLoop_100000() {
		BigDecimal result = BigDecimal.ONE;
		for (int i = 0; i < loopIterations; i++) {
			result = result.multiply(yBigD);
		}
		return result;
	}

	// @Group("bigDecimalLongMax")
	@Benchmark
	public String bigDecimal_LongMax_toString() {
		return xBigD_LongMax.toString();
	}

	// @Group("bigDecimalLongMax")
	@Benchmark
	public BigDecimal bigDecimal_LongMax_Add() {
		return xBigD_LongMax.add(yBigD_LongMax);
	}

	// @Group("bigDecimalLongMax")
	@Benchmark
	public BigDecimal bigDecimal_LongMax_Substract() {
		return xBigD_LongMax.subtract(yBigD_LongMax);
	}

	// @Group("bigDecimalLongMax")
	@Benchmark
	public BigDecimal bigDecimal_LongMax_Mult() {
		return xBigD_LongMax.multiply(yBigD_LongMax);
	}

	// @Group("bigDecimalLongMax")
	@Benchmark
	public BigDecimal bigDecimal_LongMax_Div64() {
		return xBigD_LongMax.divide(yBigD_LongMax, MathContext.DECIMAL64);
	}

	// @Group("bigDecimalLongMax")
	@Benchmark
	public BigDecimal bigDecimal_LongMax_Div128() {
		return xBigD_LongMax.divide(yBigD_LongMax, MathContext.DECIMAL128);
	}

	// @Group("bigDecimalLongMax")
	@Benchmark
	public BigDecimal bigDecimal_LongMax_Rem() {
		return xBigD_LongMax.divideAndRemainder(yBigD_LongMax)[1];
	}

	// @Group("bigDecimalRealyBig")
	@Benchmark
	public String bigDecimal_RealyBig_toString() {
		return xBigD_RealyBig.toString();
	}

	// @Group("bigDecimalRealyBig")
	@Benchmark
	public BigDecimal bigDecimal_RealyBig_Add() {
		return xBigD_RealyBig.add(yBigD_RealyBig);
	}

	// @Group("bigDecimalRealyBig")
	@Benchmark
	public BigDecimal bigDecimal_RealyBig_Substract() {
		return xBigD_RealyBig.subtract(yBigD_RealyBig);
	}

	// @Group("bigDecimalRealyBig")
	@Benchmark
	public BigDecimal bigDecimal_RealyBig_Mult() {
		return xBigD_RealyBig.multiply(yBigD_RealyBig);
	}

	// @Group("bigDecimalRealyBig")
	@Benchmark
	public BigDecimal bigDecimal_RealyBig_Div64() {
		return xBigD_RealyBig.divide(yBigD_RealyBig, MathContext.DECIMAL64);
	}

	// @Group("bigDecimalRealyBig")
	@Benchmark
	public BigDecimal bigDecimal_RealyBig_Div128() {
		return xBigD_RealyBig.divide(yBigD_RealyBig, MathContext.DECIMAL128);
	}

	// @Group("bigDecimalRealyBig")
	@Benchmark
	public BigDecimal bigDecimal_RealyBig_Rem() {
		return xBigD_RealyBig.divideAndRemainder(yBigD_RealyBig)[1];
	}

	@Benchmark
	public boolean instanceOfFalse() {
		return (someInteger instanceof String);
	}

	@Benchmark
	public boolean instanceOfTrue() {
		return (someInteger instanceof Integer);
	}

	@Benchmark
	public boolean nullCheckFalse() {
		return (isNotNull == null);
	}

	@Benchmark
	public boolean nullCheckTrue() {
		return (isNull == null);
	}

	@Benchmark
	public void methodCallOverhead_0Args() {
		callMe();
	}

	@CompilerControl(CompilerControl.Mode.DONT_INLINE)
	private void callMe() {
		//baseline measure
	}

	@Benchmark
	public void methodCallOverhead_2Args() {
		callMe2(xI, yI);
	}

	@CompilerControl(CompilerControl.Mode.DONT_INLINE)
	private void callMe2(int xI2, int yI2) {
		//baseline measure

	}

	@Benchmark
	public int Math_max_int() {
		return Math.max(xI, yI);
	}

	@Benchmark
	public long Math_max_long() {
		return Math.max(xL, yL);
	}

	@Benchmark
	public double Math_max_double() {
		return Math.max(xD, yD);
	}

	@Benchmark
	public int Math_min_int() {
		return Math.min(xI, yI);
	}

	@Benchmark
	public long Math_min_long() {
		return Math.min(xL, yL);
	}

	@Benchmark
	public double Math_min_double() {
		return Math.min(xD, yD);
	}

	@Benchmark
	public double Math_sin() {
		return Math.sin(xD);
	}

	@Benchmark
	public double Math_cos() {
		return Math.cos(xD);
	}

	@Benchmark
	public double Math_tan() {
		return Math.tan(xD);
	}

	@Benchmark
	public double Math_log() {
		return Math.log(xD);
	}

	@Benchmark
	public double Math_log10() {
		return Math.log10(xD);
	}

	@Benchmark
	public double Math_exp() {
		return Math.exp(xD);
	}

	@Benchmark
	public double Math_SQRT() {
		return Math.sqrt(xD);
	}

	@Benchmark
	public double Math_pow2() {
		return Math.pow(xD, 2.0d);
	}

	@Benchmark
	public double Math_pow1_5() {
		return Math.pow(xD, 1.5d);
	}

	@Benchmark
	public double Math_pow0_5() {
		return Math.pow(xD, 0.5d);
	}

	@Benchmark
	public double Math_powX() {
		return Math.pow(xD, yD);
	}

	@Benchmark
	public float Math_abs_float() {
		return Math.abs(xF);
	}

	@Benchmark
	public double Math_abs_double() {
		return Math.abs(xD);
	}

	@Benchmark
	public double Math_ceil() {
		return Math.ceil(xD);
	}
	@Benchmark
	public double Math_floor() {
		return Math.floor(xD);
	}
	@Benchmark
	public double Math_round() {
		return Math.round(xD);
	}

	@Benchmark
	public double Math_muliplyExact_int() {
		return Math.multiplyExact(xI, yI);
	}

	@Benchmark
	public double Math_addExact_int() {
		return Math.addExact(xI, yI);
	}

	@Benchmark
	public double Math_muliplyExact_long() {
		return Math.multiplyExact(xL, yL);
	}

	@Benchmark
	public double Math_EXTRA_invSQRT() {
		return 1.0d/Math.sqrt(xD);
	}



	@Benchmark
	public float Math_EXTRA_invSQRT_cust_float() {
		float x=xF;
	    float xhalf = 0.5f * x;
	    int i = Float.floatToIntBits(x);
	    i = 0x5f3759df - (i >> 1);
	    x = Float.intBitsToFloat(i);
	    x *= (1.5f - xhalf * x * x);
	    return x;
	}

	@Benchmark
	public double Math_EXTRA_invSQRT_cust_double() {
		double x=xD;
	    double xhalf = 0.5d * x;
	    long i = Double.doubleToLongBits(x);
	    i = 0x5fe6ec85e7de30daL - (i >> 1);
	    x = Double.longBitsToDouble(i);
	    x *= (1.5d - xhalf * x * x);
	    return x;
	}

	@Benchmark
	public double Math_EXTRA_invSQRT_cust_double2(){
	    double x = xD;
	    double xhalf = 0.5d*x;
	    long i = Double.doubleToLongBits(x);
	    i = 0x5fe6ec85e7de30daL - (i>>1);
	    x = Double.longBitsToDouble(i);
	    for(int it = 0; it < 4; it++){
	        x = x*(1.5d - xhalf*x*x);
	    }
	    x *= xD;
	    return x;
	}

	@Benchmark
	public int lookupSwitch() {
		switch (yI) {// yI=5
		case 1:
			return 1;
		case 5:
			return 2;
		case 10:
			return 3;
		case 100:
			return 4;
		case 1000:
			return 5;
		case 10000:
			return 6;
		case 100000:
			return 7;
		case 1000000:
			return 8;
		case 10000000:
			return 9;
		}
		return -1;
	}

	@Benchmark
	public int tableSwitch() {
		switch (yI) {// yI=5
		case 1:
			return 1;
		case 2:
			return 2;
		case 3:
			return 3;
		case 4:
			return 4;
		case 5:
			return 5;
		case 6:
			return 6;
		case 7:
			return 7;
		case 8:
			return 8;
		case 9:
			return 9;
		}
		return -1;
	}

	// still a table switch - javac will fill in the "holes" with "fake" switch
	// cases.
	@Benchmark
	public int tableSwitchWithHoles() {
		switch (yI) {// yI=5
		case 1:
			return 1;
		case 5:
			return 3;
		case 9:
			return 5;
		case 11:
			return 6;
		case 13:
			return 7;
		case 15:
			return 8;
		case 17:
			return 9;
		case 19:
			return 9;
		}
		return -1;
	}

	// the medium case is 5. However the longer and the chain and the faster
	// down the target is, the more expensive the ifElse cascade becomes. =>
	// USE SWITCH
	@Benchmark
	public int ifElseCascade() {
		if (1 == yI) {// yI ==5
			return 1;
		} else if (2 == yI) {
			return 2;
		} else if (3 == yI) {
			return 3;
		} else if (4 == yI) {
			return 4;
		} else if (5 == yI) {
			return 5;
		} else if (6 == yI) {
			return 6;
		} else if (7 == yI) {
			return 7;
		} else if (8 == yI) {
			return 8;
		} else if (9 == yI) {
			return 9;
		}
		return -1;
	}

	@Benchmark
	public Exception createNewException() {
		return new Exception();
	}

	@Benchmark
	public Exception createNewExceptionWithoutStackTrace() {
		return new ExceptionWithoutStacktrace();
	}

	@Benchmark
	public void catchExceptionWithoutStacktrace(Blackhole b) {
		try {
			throw new ExceptionWithoutStacktrace();
		} catch (Exception e) {
			b.consume(e);
		}
	}

	@Benchmark
	public void catchException(Blackhole b) {
		try {
			throw new Exception();
		} catch (Exception e) {
			b.consume(e);
		}
	}

	public static class ExceptionWithoutStacktrace extends Exception {
		@Override
		public synchronized Throwable fillInStackTrace() {
			return this;
		}
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(".*" + BasicJavaOpsJMH.class.getSimpleName() + ".Math_EXTRA*")
				.forks(1)
				// #########
				// COMPILER
				// #########
				// make sure we dont see compiling of our benchmark code during
				// measurement.
				// if you see compiling => more warmup
				.jvmArgsAppend("-XX:+UnlockDiagnosticVMOptions")
				// .jvmArgsAppend("-XX:+PrintCompilation")
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
