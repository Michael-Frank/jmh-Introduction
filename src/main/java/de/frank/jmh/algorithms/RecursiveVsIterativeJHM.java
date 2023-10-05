package de.frank.jmh.algorithms;

import com.yevdo.jwildcard.JWildcard;
import lombok.AllArgsConstructor;
import lombok.Data;
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
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.profile.ProfilerException;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormat;
import org.openjdk.jmh.results.format.ResultFormatFactory;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.AbstractList;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;


/*--
 * --
 * Problem Statement:
 * -----------------------------------
 * I a very hot & time critical code path we repeatably must find/checking elements in a large nested data structure (n-order tree).
 * Which algorithm is best to iterate through the nodes: recursive vs iterative?
 * <p>
 * Implementation:
 * -----------------------------------
 * As an approximation we use a perfectly balanced n-order Tree with a certain String to find placed strategically.
 * As the recursive method traverses depth-first the iterative version does so as well.
 * Benchmark dimensions:
 * - recursive VS. iterative (3 different ways of using ArrayDeque: add-/poll-first VS. add-/poll-last VS. addAll-pollLast)
 * - occurrences of search string:
 *   - never - must check all nodes)
 *   - on first node - can skip n-1 nodes
 *   - in the middle  - must check n/2 nodes
 * - number of levels in hierarchy
 * - number of nodes/leafs per level
 *
 * Result:
 * -----------------------------------

                   iterative     iterative     iterative     iterative     iterative     recursive
                   pollFirst      pollLast      pollLast      pollLast      pollLast
                    AddFirst       AddLast        AddAll        AddAll       AddLast
                                                                  lazy  shortCircuit
FIRST
    Depth_1
        Nodes_2         0,03          0,02          0,02          0,01          0,02          0,02   - iter_pollLast_addAll_lazy
        Nodes_5         0,03          0,02          0,02          0,01          0,02          0,02   - iter_pollLast_addAll_lazy
        Nodes_10        0,03          0,03          0,02          0,01          0,02          0,02   - iter_pollLast_addAll_lazy
        Nodes_50        0,03          0,03          0,02          0,01          0,03          0,02   - iter_pollLast_addAll_lazy
    Depth_2
        Nodes_2         0,08          0,07          0,09          0,08          0,06          0,04   - recursive
        Nodes_5         0,10          0,11          0,19          0,17          0,12          0,09   - recursive
        Nodes_10        0,17          0,18          0,39          0,35          0,16          0,19   - iter_pollLast_AddLast_shortCircuit
        Nodes_50        0,99          0,85          1,71          1,22          0,99          0,68   - recursive
    Depth_5
        Nodes_2         0,52          0,55          1,05          0,68          0,48          0,44   - recursive
        Nodes_5        14,00         13,52         24,71         13,99         13,19         10,73   - recursive
        Nodes_10      195,42        174,58        341,39        183,89        174,86        173,15   - recursive, iter_pollLast_addLast, iter_pollLast_addAll_lazy
        Nodes_50   157378,61     133818,81     215765,48     135662,80     152145,20     169945,73   - iter_pollLast_addLast,  iter_pollLast_addAll_lazy
IN_THE_MIDDLE
    Depth_1
        Nodes_2         0,03          0,02          0,02          0,01          0,02          0,02   - iter_pollLast_addAll_lazy
        Nodes_5         0,03          0,02          0,02          0,01          0,02          0,02   - iter_pollLast_addAll_lazy
        Nodes_10        0,03          0,02          0,02          0,01          0,03          0,02   - iter_pollLast_addAll_lazy
        Nodes_50        0,03          0,02          0,02          0,01          0,03          0,02   - iter_pollLast_addAll_lazy
    Depth_2
        Nodes_2         0,08          0,06          0,09          0,08          0,06          0,04   - recursive
        Nodes_5         0,10          0,09          0,04          0,04          0,09          0,04   - recursive
        Nodes_10        0,16          0,14          0,07          0,06          0,15          0,06   - recursive
        Nodes_50        0,71          0,50          0,32          0,32          0,65          0,30   - recursive, iter_pollLast_addAll, iter_pollLast_addAll_lazy
    Depth_5
        Nodes_2         0,33          0,34          0,68          0,48          0,37          0,22   - recursive
        Nodes_5        13,89         12,47         24,75         13,59         12,60         10,76   - recursive
        Nodes_10       96,23         95,37        199,34        109,84         88,67         84,07   - recursive, iter_pollLast_AddLast_shortCircuit
        Nodes_50    79445,05      73607,01     112531,46      73361,85      81110,83      89021,15   - iter_pollLast_addAll_lazy, iter_pollLast_addLast
NONE
    Depth_1
        Nodes_2         0,03          0,02          0,02          0,01          0,02          0,02   - iter_pollLast_addAll_lazy
        Nodes_5         0,03          0,02          0,02          0,01          0,02          0,02   - iter_pollLast_addAll_lazy
        Nodes_10        0,03          0,02          0,02          0,01          0,02          0,02   - iter_pollLast_addAll_lazy
        Nodes_50        0,03          0,02          0,02          0,01          0,02          0,02   - iter_pollLast_addAll_lazy
    Depth_2
        Nodes_2         0,09          0,06          0,09          0,09          0,07          0,04   - recursive,
        Nodes_5         0,10          0,12          0,19          0,16          0,11          0,09   - recursive, iter_pollFirst_addFirst
        Nodes_10        0,19          0,18          0,36          0,35          0,17          0,17   - recursive, iter_pollLast_AddLast_shortCircuit
        Nodes_50        1,10          0,86          1,71          1,25          0,99          1,00   - iter_pollLast_AddLast
    Depth_5
        Nodes_2         0,50          0,75          1,05          0,68          0,53          0,45   - recursive
        Nodes_5        14,15         13,46         24,51         13,55         13,32         11,73   - recursive
        Nodes_10      208,23        166,02        334,02        184,42        180,26        152,81   - recursive, iter_pollLast_AddLast
        Nodes_50   158081,06     142908,52     229222,19     138082,40     156180,85     173930,85   - iter_pollLast_addAll_lazy, iter_pollLast_AddLast_shortCircuit

Benchmark                                    (childrenPerNode)  (depth)  (terminalConditionPlace)  Mode  Cnt       Score       Error  Units
iterative_DF_R_pollFirst_AddFirst                            2        1                      NONE  avgt    5       0,027 ±     0,001  us/op
iterative_DF_R_pollFirst_AddFirst                            2        1             IN_THE_MIDDLE  avgt    5       0,032 ±     0,016  us/op
iterative_DF_R_pollFirst_AddFirst                            2        1                     FIRST  avgt    5       0,034 ±     0,009  us/op
iterative_DF_R_pollFirst_AddFirst                            2        2                      NONE  avgt    5       0,088 ±     0,031  us/op
iterative_DF_R_pollFirst_AddFirst                            2        2             IN_THE_MIDDLE  avgt    5       0,075 ±     0,004  us/op
iterative_DF_R_pollFirst_AddFirst                            2        2                     FIRST  avgt    5       0,077 ±     0,001  us/op
iterative_DF_R_pollFirst_AddFirst                            2        5                      NONE  avgt    5       0,496 ±     0,014  us/op
iterative_DF_R_pollFirst_AddFirst                            2        5             IN_THE_MIDDLE  avgt    5       0,333 ±     0,019  us/op
iterative_DF_R_pollFirst_AddFirst                            2        5                     FIRST  avgt    5       0,522 ±     0,111  us/op
iterative_DF_R_pollFirst_AddFirst                            5        1                      NONE  avgt    5       0,027 ±     0,001  us/op
iterative_DF_R_pollFirst_AddFirst                            5        1             IN_THE_MIDDLE  avgt    5       0,027 ±     0,001  us/op
iterative_DF_R_pollFirst_AddFirst                            5        1                     FIRST  avgt    5       0,027 ±     0,001  us/op
iterative_DF_R_pollFirst_AddFirst                            5        2                      NONE  avgt    5       0,096 ±     0,002  us/op
iterative_DF_R_pollFirst_AddFirst                            5        2             IN_THE_MIDDLE  avgt    5       0,101 ±     0,010  us/op
iterative_DF_R_pollFirst_AddFirst                            5        2                     FIRST  avgt    5       0,099 ±     0,024  us/op
iterative_DF_R_pollFirst_AddFirst                            5        5                      NONE  avgt    5      14,154 ±     0,755  us/op
iterative_DF_R_pollFirst_AddFirst                            5        5             IN_THE_MIDDLE  avgt    5      13,887 ±     0,705  us/op
iterative_DF_R_pollFirst_AddFirst                            5        5                     FIRST  avgt    5      13,995 ±     0,361  us/op
iterative_DF_R_pollFirst_AddFirst                           10        1                      NONE  avgt    5       0,026 ±     0,001  us/op
iterative_DF_R_pollFirst_AddFirst                           10        1             IN_THE_MIDDLE  avgt    5       0,028 ±     0,006  us/op
iterative_DF_R_pollFirst_AddFirst                           10        1                     FIRST  avgt    5       0,027 ±     0,001  us/op
iterative_DF_R_pollFirst_AddFirst                           10        2                      NONE  avgt    5       0,186 ±     0,006  us/op
iterative_DF_R_pollFirst_AddFirst                           10        2             IN_THE_MIDDLE  avgt    5       0,164 ±     0,013  us/op
iterative_DF_R_pollFirst_AddFirst                           10        2                     FIRST  avgt    5       0,171 ±     0,006  us/op
iterative_DF_R_pollFirst_AddFirst                           10        5                      NONE  avgt    5     208,228 ±    10,614  us/op
iterative_DF_R_pollFirst_AddFirst                           10        5             IN_THE_MIDDLE  avgt    5      96,227 ±    16,247  us/op
iterative_DF_R_pollFirst_AddFirst                           10        5                     FIRST  avgt    5     195,420 ±     6,087  us/op
iterative_DF_R_pollFirst_AddFirst                           50        1                      NONE  avgt    5       0,027 ±     0,002  us/op
iterative_DF_R_pollFirst_AddFirst                           50        1             IN_THE_MIDDLE  avgt    5       0,027 ±     0,001  us/op
iterative_DF_R_pollFirst_AddFirst                           50        1                     FIRST  avgt    5       0,026 ±     0,001  us/op
iterative_DF_R_pollFirst_AddFirst                           50        2                      NONE  avgt    5       1,095 ±     0,046  us/op
iterative_DF_R_pollFirst_AddFirst                           50        2             IN_THE_MIDDLE  avgt    5       0,712 ±     0,046  us/op
iterative_DF_R_pollFirst_AddFirst                           50        2                     FIRST  avgt    5       0,992 ±     0,032  us/op
iterative_DF_R_pollFirst_AddFirst                           50        5                      NONE  avgt    5  158081,060 ±  9954,917  us/op
iterative_DF_R_pollFirst_AddFirst                           50        5             IN_THE_MIDDLE  avgt    5   79445,047 ±  5996,489  us/op
iterative_DF_R_pollFirst_AddFirst                           50        5                     FIRST  avgt    5  157378,614 ±  5267,872  us/op
iterative_DF_R_pollLast_AddAll_lazy                          2        1                      NONE  avgt    5       0,015 ±     0,001  us/op
iterative_DF_R_pollLast_AddAll_lazy                          2        1             IN_THE_MIDDLE  avgt    5       0,016 ±     0,001  us/op
iterative_DF_R_pollLast_AddAll_lazy                          2        1                     FIRST  avgt    5       0,015 ±     0,001  us/op
iterative_DF_R_pollLast_AddAll_lazy                          2        2                      NONE  avgt    5       0,090 ±     0,008  us/op
iterative_DF_R_pollLast_AddAll_lazy                          2        2             IN_THE_MIDDLE  avgt    5       0,089 ±     0,003  us/op
iterative_DF_R_pollLast_AddAll_lazy                          2        2                     FIRST  avgt    5       0,090 ±     0,005  us/op
iterative_DF_R_pollLast_AddAll_lazy                          2        5                      NONE  avgt    5       1,045 ±     0,055  us/op
iterative_DF_R_pollLast_AddAll_lazy                          2        5             IN_THE_MIDDLE  avgt    5       0,678 ±     0,014  us/op
iterative_DF_R_pollLast_AddAll_lazy                          2        5                     FIRST  avgt    5       1,047 ±     0,032  us/op
iterative_DF_R_pollLast_AddAll_lazy                          5        1                      NONE  avgt    5       0,016 ±     0,001  us/op
iterative_DF_R_pollLast_AddAll_lazy                          5        1             IN_THE_MIDDLE  avgt    5       0,016 ±     0,001  us/op
iterative_DF_R_pollLast_AddAll_lazy                          5        1                     FIRST  avgt    5       0,016 ±     0,001  us/op
iterative_DF_R_pollLast_AddAll_lazy                          5        2                      NONE  avgt    5       0,191 ±     0,008  us/op
iterative_DF_R_pollLast_AddAll_lazy                          5        2             IN_THE_MIDDLE  avgt    5       0,037 ±     0,002  us/op
iterative_DF_R_pollLast_AddAll_lazy                          5        2                     FIRST  avgt    5       0,190 ±     0,005  us/op
iterative_DF_R_pollLast_AddAll_lazy                          5        5                      NONE  avgt    5      24,513 ±     0,601  us/op
iterative_DF_R_pollLast_AddAll_lazy                          5        5             IN_THE_MIDDLE  avgt    5      24,750 ±     1,459  us/op
iterative_DF_R_pollLast_AddAll_lazy                          5        5                     FIRST  avgt    5      24,705 ±     0,834  us/op
iterative_DF_R_pollLast_AddAll_lazy                         10        1                      NONE  avgt    5       0,016 ±     0,001  us/op
iterative_DF_R_pollLast_AddAll_lazy                         10        1             IN_THE_MIDDLE  avgt    5       0,016 ±     0,001  us/op
iterative_DF_R_pollLast_AddAll_lazy                         10        1                     FIRST  avgt    5       0,016 ±     0,001  us/op
iterative_DF_R_pollLast_AddAll_lazy                         10        2                      NONE  avgt    5       0,359 ±     0,014  us/op
iterative_DF_R_pollLast_AddAll_lazy                         10        2             IN_THE_MIDDLE  avgt    5       0,071 ±     0,004  us/op
iterative_DF_R_pollLast_AddAll_lazy                         10        2                     FIRST  avgt    5       0,385 ±     0,018  us/op
iterative_DF_R_pollLast_AddAll_lazy                         10        5                      NONE  avgt    5     334,024 ±    28,366  us/op
iterative_DF_R_pollLast_AddAll_lazy                         10        5             IN_THE_MIDDLE  avgt    5     199,341 ±     5,121  us/op
iterative_DF_R_pollLast_AddAll_lazy                         10        5                     FIRST  avgt    5     341,390 ±     8,855  us/op
iterative_DF_R_pollLast_AddAll_lazy                         50        1                      NONE  avgt    5       0,016 ±     0,001  us/op
iterative_DF_R_pollLast_AddAll_lazy                         50        1             IN_THE_MIDDLE  avgt    5       0,016 ±     0,003  us/op
iterative_DF_R_pollLast_AddAll_lazy                         50        1                     FIRST  avgt    5       0,015 ±     0,001  us/op
iterative_DF_R_pollLast_AddAll_lazy                         50        2                      NONE  avgt    5       1,707 ±     0,069  us/op
iterative_DF_R_pollLast_AddAll_lazy                         50        2             IN_THE_MIDDLE  avgt    5       0,321 ±     0,008  us/op
iterative_DF_R_pollLast_AddAll_lazy                         50        2                     FIRST  avgt    5       1,709 ±     0,082  us/op
iterative_DF_R_pollLast_AddAll_lazy                         50        5                      NONE  avgt    5  229222,187 ± 17661,945  us/op
iterative_DF_R_pollLast_AddAll_lazy                         50        5             IN_THE_MIDDLE  avgt    5  112531,460 ±  5978,104  us/op
iterative_DF_R_pollLast_AddAll_lazy                         50        5                     FIRST  avgt    5  215765,482 ± 25835,045  us/op
iterative_DF_R_pollLast_AddLast                              2        1                      NONE  avgt    5       0,024 ±     0,001  us/op
iterative_DF_R_pollLast_AddLast                              2        1             IN_THE_MIDDLE  avgt    5       0,024 ±     0,001  us/op
iterative_DF_R_pollLast_AddLast                              2        1                     FIRST  avgt    5       0,024 ±     0,001  us/op
iterative_DF_R_pollLast_AddLast                              2        2                      NONE  avgt    5       0,074 ±     0,003  us/op
iterative_DF_R_pollLast_AddLast                              2        2             IN_THE_MIDDLE  avgt    5       0,062 ±     0,001  us/op
iterative_DF_R_pollLast_AddLast                              2        2                     FIRST  avgt    5       0,062 ±     0,003  us/op
iterative_DF_R_pollLast_AddLast                              2        5                      NONE  avgt    5       0,533 ±     0,007  us/op
iterative_DF_R_pollLast_AddLast                              2        5             IN_THE_MIDDLE  avgt    5       0,374 ±     0,004  us/op
iterative_DF_R_pollLast_AddLast                              2        5                     FIRST  avgt    5       0,484 ±     0,010  us/op
iterative_DF_R_pollLast_AddLast                              5        1                      NONE  avgt    5       0,024 ±     0,001  us/op
iterative_DF_R_pollLast_AddLast                              5        1             IN_THE_MIDDLE  avgt    5       0,024 ±     0,002  us/op
iterative_DF_R_pollLast_AddLast                              5        1                     FIRST  avgt    5       0,024 ±     0,001  us/op
iterative_DF_R_pollLast_AddLast                              5        2                      NONE  avgt    5       0,110 ±     0,006  us/op
iterative_DF_R_pollLast_AddLast                              5        2             IN_THE_MIDDLE  avgt    5       0,087 ±     0,004  us/op
iterative_DF_R_pollLast_AddLast                              5        2                     FIRST  avgt    5       0,124 ±     0,035  us/op
iterative_DF_R_pollLast_AddLast                              5        5                      NONE  avgt    5      13,317 ±     0,332  us/op
iterative_DF_R_pollLast_AddLast                              5        5             IN_THE_MIDDLE  avgt    5      12,599 ±     0,264  us/op
iterative_DF_R_pollLast_AddLast                              5        5                     FIRST  avgt    5      13,194 ±     0,386  us/op
iterative_DF_R_pollLast_AddLast                             10        1                      NONE  avgt    5       0,024 ±     0,001  us/op
iterative_DF_R_pollLast_AddLast                             10        1             IN_THE_MIDDLE  avgt    5       0,028 ±     0,001  us/op
iterative_DF_R_pollLast_AddLast                             10        1                     FIRST  avgt    5       0,024 ±     0,001  us/op
iterative_DF_R_pollLast_AddLast                             10        2                      NONE  avgt    5       0,168 ±     0,010  us/op
iterative_DF_R_pollLast_AddLast                             10        2             IN_THE_MIDDLE  avgt    5       0,154 ±     0,006  us/op
iterative_DF_R_pollLast_AddLast                             10        2                     FIRST  avgt    5       0,158 ±     0,007  us/op
iterative_DF_R_pollLast_AddLast                             10        5                      NONE  avgt    5     180,263 ±    14,746  us/op
iterative_DF_R_pollLast_AddLast                             10        5             IN_THE_MIDDLE  avgt    5      88,665 ±     5,864  us/op
iterative_DF_R_pollLast_AddLast                             10        5                     FIRST  avgt    5     174,855 ±     3,780  us/op
iterative_DF_R_pollLast_AddLast                             50        1                      NONE  avgt    5       0,024 ±     0,001  us/op
iterative_DF_R_pollLast_AddLast                             50        1             IN_THE_MIDDLE  avgt    5       0,025 ±     0,006  us/op
iterative_DF_R_pollLast_AddLast                             50        1                     FIRST  avgt    5       0,028 ±     0,001  us/op
iterative_DF_R_pollLast_AddLast                             50        2                      NONE  avgt    5       0,987 ±     0,020  us/op
iterative_DF_R_pollLast_AddLast                             50        2             IN_THE_MIDDLE  avgt    5       0,650 ±     0,020  us/op
iterative_DF_R_pollLast_AddLast                             50        2                     FIRST  avgt    5       0,992 ±     0,021  us/op
iterative_DF_R_pollLast_AddLast                             50        5                      NONE  avgt    5  156180,847 ±  6314,546  us/op
iterative_DF_R_pollLast_AddLast                             50        5             IN_THE_MIDDLE  avgt    5   81110,825 ±  1460,350  us/op
iterative_DF_R_pollLast_AddLast                             50        5                     FIRST  avgt    5  152145,197 ±  6044,705  us/op
iterative_DF_R_pollLast_addAll                               2        1                      NONE  avgt    5       0,023 ±     0,001  us/op
iterative_DF_R_pollLast_addAll                               2        1             IN_THE_MIDDLE  avgt    5       0,024 ±     0,007  us/op
iterative_DF_R_pollLast_addAll                               2        1                     FIRST  avgt    5       0,024 ±     0,001  us/op
iterative_DF_R_pollLast_addAll                               2        2                      NONE  avgt    5       0,062 ±     0,003  us/op
iterative_DF_R_pollLast_addAll                               2        2             IN_THE_MIDDLE  avgt    5       0,062 ±     0,001  us/op
iterative_DF_R_pollLast_addAll                               2        2                     FIRST  avgt    5       0,066 ±     0,021  us/op
iterative_DF_R_pollLast_addAll                               2        5                      NONE  avgt    5       0,745 ±     0,716  us/op
iterative_DF_R_pollLast_addAll                               2        5             IN_THE_MIDDLE  avgt    5       0,338 ±     0,015  us/op
iterative_DF_R_pollLast_addAll                               2        5                     FIRST  avgt    5       0,551 ±     0,013  us/op
iterative_DF_R_pollLast_addAll                               5        1                      NONE  avgt    5       0,024 ±     0,001  us/op
iterative_DF_R_pollLast_addAll                               5        1             IN_THE_MIDDLE  avgt    5       0,024 ±     0,001  us/op
iterative_DF_R_pollLast_addAll                               5        1                     FIRST  avgt    5       0,024 ±     0,001  us/op
iterative_DF_R_pollLast_addAll                               5        2                      NONE  avgt    5       0,116 ±     0,005  us/op
iterative_DF_R_pollLast_addAll                               5        2             IN_THE_MIDDLE  avgt    5       0,092 ±     0,003  us/op
iterative_DF_R_pollLast_addAll                               5        2                     FIRST  avgt    5       0,106 ±     0,004  us/op
iterative_DF_R_pollLast_addAll                               5        5                      NONE  avgt    5      13,461 ±     0,449  us/op
iterative_DF_R_pollLast_addAll                               5        5             IN_THE_MIDDLE  avgt    5      12,468 ±     0,305  us/op
iterative_DF_R_pollLast_addAll                               5        5                     FIRST  avgt    5      13,522 ±     0,409  us/op
iterative_DF_R_pollLast_addAll                              10        1                      NONE  avgt    5       0,024 ±     0,001  us/op
iterative_DF_R_pollLast_addAll                              10        1             IN_THE_MIDDLE  avgt    5       0,024 ±     0,002  us/op
iterative_DF_R_pollLast_addAll                              10        1                     FIRST  avgt    5       0,026 ±     0,001  us/op
iterative_DF_R_pollLast_addAll                              10        2                      NONE  avgt    5       0,182 ±     0,025  us/op
iterative_DF_R_pollLast_addAll                              10        2             IN_THE_MIDDLE  avgt    5       0,144 ±     0,006  us/op
iterative_DF_R_pollLast_addAll                              10        2                     FIRST  avgt    5       0,178 ±     0,005  us/op
iterative_DF_R_pollLast_addAll                              10        5                      NONE  avgt    5     166,023 ±    11,626  us/op
iterative_DF_R_pollLast_addAll                              10        5             IN_THE_MIDDLE  avgt    5      95,373 ±     4,435  us/op
iterative_DF_R_pollLast_addAll                              10        5                     FIRST  avgt    5     174,583 ±     2,809  us/op
iterative_DF_R_pollLast_addAll                              50        1                      NONE  avgt    5       0,024 ±     0,001  us/op
iterative_DF_R_pollLast_addAll                              50        1             IN_THE_MIDDLE  avgt    5       0,024 ±     0,001  us/op
iterative_DF_R_pollLast_addAll                              50        1                     FIRST  avgt    5       0,026 ±     0,001  us/op
iterative_DF_R_pollLast_addAll                              50        2                      NONE  avgt    5       0,859 ±     0,032  us/op
iterative_DF_R_pollLast_addAll                              50        2             IN_THE_MIDDLE  avgt    5       0,500 ±     0,010  us/op
iterative_DF_R_pollLast_addAll                              50        2                     FIRST  avgt    5       0,853 ±     0,054  us/op
iterative_DF_R_pollLast_addAll                              50        5                      NONE  avgt    5  142908,521 ± 35947,573  us/op
iterative_DF_R_pollLast_addAll                              50        5             IN_THE_MIDDLE  avgt    5   73607,007 ±  7480,791  us/op
iterative_DF_R_pollLast_addAll                              50        5                     FIRST  avgt    5  133818,812 ±  7832,901  us/op
iterative_DF_R_pollLast_addAll_shortCircuit                  2        1                      NONE  avgt    5       0,014 ±     0,001  us/op
iterative_DF_R_pollLast_addAll_shortCircuit                  2        1             IN_THE_MIDDLE  avgt    5       0,014 ±     0,001  us/op
iterative_DF_R_pollLast_addAll_shortCircuit                  2        1                     FIRST  avgt    5       0,014 ±     0,001  us/op
iterative_DF_R_pollLast_addAll_shortCircuit                  2        2                      NONE  avgt    5       0,085 ±     0,011  us/op
iterative_DF_R_pollLast_addAll_shortCircuit                  2        2             IN_THE_MIDDLE  avgt    5       0,084 ±     0,002  us/op
iterative_DF_R_pollLast_addAll_shortCircuit                  2        2                     FIRST  avgt    5       0,084 ±     0,005  us/op
iterative_DF_R_pollLast_addAll_shortCircuit                  2        5                      NONE  avgt    5       0,682 ±     0,044  us/op
iterative_DF_R_pollLast_addAll_shortCircuit                  2        5             IN_THE_MIDDLE  avgt    5       0,475 ±     0,025  us/op
iterative_DF_R_pollLast_addAll_shortCircuit                  2        5                     FIRST  avgt    5       0,680 ±     0,042  us/op
iterative_DF_R_pollLast_addAll_shortCircuit                  5        1                      NONE  avgt    5       0,014 ±     0,001  us/op
iterative_DF_R_pollLast_addAll_shortCircuit                  5        1             IN_THE_MIDDLE  avgt    5       0,014 ±     0,001  us/op
iterative_DF_R_pollLast_addAll_shortCircuit                  5        1                     FIRST  avgt    5       0,014 ±     0,001  us/op
iterative_DF_R_pollLast_addAll_shortCircuit                  5        2                      NONE  avgt    5       0,164 ±     0,008  us/op
iterative_DF_R_pollLast_addAll_shortCircuit                  5        2             IN_THE_MIDDLE  avgt    5       0,037 ±     0,003  us/op
iterative_DF_R_pollLast_addAll_shortCircuit                  5        2                     FIRST  avgt    5       0,166 ±     0,007  us/op
iterative_DF_R_pollLast_addAll_shortCircuit                  5        5                      NONE  avgt    5      13,550 ±     0,383  us/op
iterative_DF_R_pollLast_addAll_shortCircuit                  5        5             IN_THE_MIDDLE  avgt    5      13,588 ±     0,619  us/op
iterative_DF_R_pollLast_addAll_shortCircuit                  5        5                     FIRST  avgt    5      13,993 ±     3,630  us/op
iterative_DF_R_pollLast_addAll_shortCircuit                 10        1                      NONE  avgt    5       0,014 ±     0,001  us/op
iterative_DF_R_pollLast_addAll_shortCircuit                 10        1             IN_THE_MIDDLE  avgt    5       0,014 ±     0,001  us/op
iterative_DF_R_pollLast_addAll_shortCircuit                 10        1                     FIRST  avgt    5       0,014 ±     0,001  us/op
iterative_DF_R_pollLast_addAll_shortCircuit                 10        2                      NONE  avgt    5       0,350 ±     0,014  us/op
iterative_DF_R_pollLast_addAll_shortCircuit                 10        2             IN_THE_MIDDLE  avgt    5       0,063 ±     0,018  us/op
iterative_DF_R_pollLast_addAll_shortCircuit                 10        2                     FIRST  avgt    5       0,350 ±     0,017  us/op
iterative_DF_R_pollLast_addAll_shortCircuit                 10        5                      NONE  avgt    5     184,424 ±    11,490  us/op
iterative_DF_R_pollLast_addAll_shortCircuit                 10        5             IN_THE_MIDDLE  avgt    5     109,838 ±     4,833  us/op
iterative_DF_R_pollLast_addAll_shortCircuit                 10        5                     FIRST  avgt    5     183,885 ±     8,667  us/op
iterative_DF_R_pollLast_addAll_shortCircuit                 50        1                      NONE  avgt    5       0,014 ±     0,001  us/op
iterative_DF_R_pollLast_addAll_shortCircuit                 50        1             IN_THE_MIDDLE  avgt    5       0,014 ±     0,001  us/op
iterative_DF_R_pollLast_addAll_shortCircuit                 50        1                     FIRST  avgt    5       0,014 ±     0,001  us/op
iterative_DF_R_pollLast_addAll_shortCircuit                 50        2                      NONE  avgt    5       1,245 ±     0,091  us/op
iterative_DF_R_pollLast_addAll_shortCircuit                 50        2             IN_THE_MIDDLE  avgt    5       0,320 ±     0,002  us/op
iterative_DF_R_pollLast_addAll_shortCircuit                 50        2                     FIRST  avgt    5       1,224 ±     0,070  us/op
iterative_DF_R_pollLast_addAll_shortCircuit                 50        5                      NONE  avgt    5  138082,399 ± 13770,782  us/op
iterative_DF_R_pollLast_addAll_shortCircuit                 50        5             IN_THE_MIDDLE  avgt    5   73361,848 ±  4269,237  us/op
iterative_DF_R_pollLast_addAll_shortCircuit                 50        5                     FIRST  avgt    5  135662,796 ±  8513,434  us/op
recursive                                                    2        1                      NONE  avgt    5       0,015 ±     0,001  us/op
recursive                                                    2        1             IN_THE_MIDDLE  avgt    5       0,015 ±     0,001  us/op
recursive                                                    2        1                     FIRST  avgt    5       0,015 ±     0,001  us/op
recursive                                                    2        2                      NONE  avgt    5       0,042 ±     0,007  us/op
recursive                                                    2        2             IN_THE_MIDDLE  avgt    5       0,040 ±     0,002  us/op
recursive                                                    2        2                     FIRST  avgt    5       0,041 ±     0,002  us/op
recursive                                                    2        5                      NONE  avgt    5       0,447 ±     0,009  us/op
recursive                                                    2        5             IN_THE_MIDDLE  avgt    5       0,215 ±     0,019  us/op
recursive                                                    2        5                     FIRST  avgt    5       0,444 ±     0,007  us/op
recursive                                                    5        1                      NONE  avgt    5       0,016 ±     0,003  us/op
recursive                                                    5        1             IN_THE_MIDDLE  avgt    5       0,015 ±     0,001  us/op
recursive                                                    5        1                     FIRST  avgt    5       0,015 ±     0,001  us/op
recursive                                                    5        2                      NONE  avgt    5       0,091 ±     0,002  us/op
recursive                                                    5        2             IN_THE_MIDDLE  avgt    5       0,035 ±     0,001  us/op
recursive                                                    5        2                     FIRST  avgt    5       0,092 ±     0,006  us/op
recursive                                                    5        5                      NONE  avgt    5      11,730 ±     0,303  us/op
recursive                                                    5        5             IN_THE_MIDDLE  avgt    5      10,756 ±     0,244  us/op
recursive                                                    5        5                     FIRST  avgt    5      10,731 ±     0,210  us/op
recursive                                                   10        1                      NONE  avgt    5       0,015 ±     0,001  us/op
recursive                                                   10        1             IN_THE_MIDDLE  avgt    5       0,015 ±     0,001  us/op
recursive                                                   10        1                     FIRST  avgt    5       0,016 ±     0,001  us/op
recursive                                                   10        2                      NONE  avgt    5       0,165 ±     0,010  us/op
recursive                                                   10        2             IN_THE_MIDDLE  avgt    5       0,062 ±     0,001  us/op
recursive                                                   10        2                     FIRST  avgt    5       0,193 ±     0,004  us/op
recursive                                                   10        5                      NONE  avgt    5     152,813 ±     7,440  us/op
recursive                                                   10        5             IN_THE_MIDDLE  avgt    5      84,069 ±     2,630  us/op
recursive                                                   10        5                     FIRST  avgt    5     173,147 ±     4,784  us/op
recursive                                                   50        1                      NONE  avgt    5       0,015 ±     0,001  us/op
recursive                                                   50        1             IN_THE_MIDDLE  avgt    5       0,016 ±     0,001  us/op
recursive                                                   50        1                     FIRST  avgt    5       0,016 ±     0,002  us/op
recursive                                                   50        2                      NONE  avgt    5       0,997 ±     0,039  us/op
recursive                                                   50        2             IN_THE_MIDDLE  avgt    5       0,299 ±     0,013  us/op
recursive                                                   50        2                     FIRST  avgt    5       0,682 ±     0,033  us/op
recursive                                                   50        5                      NONE  avgt    5  173930,853 ± 22207,802  us/op
recursive                                                   50        5             IN_THE_MIDDLE  avgt    5   89021,151 ± 16371,360  us/op
recursive                                                   50        5                     FIRST  avgt    5  169945,729 ± 12050,466  us/op

 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class RecursiveVsIterativeJHM {
    @Param({"1", "2", "5"})
    public int depth;

    @Param({"2", "5", "10", "50"})
    public int childrenPerNode;

    @Param({"NONE", "IN_THE_MIDDLE", "FIRST"})
    public TerminalConditionPlace terminalConditionPlace;

    public enum TerminalConditionPlace {NONE, IN_THE_MIDDLE, FIRST}

    List<String> terminalConditions = Arrays.asList("Foo1", "FOO2", "FIND_ME", "BAR1", "BAR2");
    Node root;

    @Setup(Level.Trial)
    public void setup() {
        long totalNodes = getTotalNodesInNTree(childrenPerNode, depth);
        AtomicInteger createdNodes = new AtomicInteger();
        this.root = generateNTree(
                new TreeMetaData(depth, childrenPerNode, totalNodes, terminalConditionPlace, createdNodes));

        System.out.println("total nodes(@depth=" + depth + ", childrenPerNode=" + childrenPerNode + ")=" + totalNodes);
        System.out.println("newNodes:" + createdNodes.get());
        if (createdNodes.get() != totalNodes) {
            throw new IllegalArgumentException(
                    "Expected vs create node count does not math! Expected: " + totalNodes + " Created: " +
                    createdNodes);
        }
    }


    public static void main(String[] args) throws RunnerException {
        testTreeGenerationCorrectness();

        Collection<RunResult> hotResults = new Runner(
                new OptionsBuilder()
                        .include(JWildcard.wildcardToRegex(RecursiveVsIterativeJHM.class.getName() + ".*"))//
                        .resultFormat(ResultFormatType.JSON)
                        .result(String.format("%s_%s_hot.json",
                                              DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                                              RecursiveVsIterativeJHM.class.getSimpleName()))
                        //.addProfiler(NormalizedGcProfiler.class)
                        .build()).run();

        ResultFormat format = ResultFormatFactory.getInstance(ResultFormatType.TEXT, System.out);
        System.out.println("hot (avg time):");
        format.writeOut(hotResults);
    }


    //depthFirst starting form "right", as most right (higher list index) is added last to the stack
    @Benchmark
    public boolean iterative_DF_R_pollFirst_AddFirst() {
        ArrayDeque<Node> stack = new ArrayDeque<>();
        stack.push(root);

        Node cur;
        while ((cur = stack.pollFirst()) != null) {
            if (isHit(cur)) {
                return true;
            }
            if (cur.children != null) {
                for (Node child : cur.children) {
                    stack.addFirst(child);
                }
            }
        }
        return false;
    }


    //depthFirst starting form "right", as most right (higher list index) is added last to the stack
    @Benchmark
    public boolean iterative_DF_R_pollLast_AddLast() {
        ArrayDeque<Node> stack = new ArrayDeque<>();
        stack.push(root);
        Node cur;
        while ((cur = stack.pollLast()) != null) {
            if (isHit(cur)) {
                return true;
            }
            if (cur.children != null) {
                for (Node child : cur.children) {
                    stack.addLast(child);
                }
            }
        }
        return false;
    }

    @Benchmark
    public boolean iterative_DF_R_pollLast_AddAll_lazy() {
        ArrayDeque<Node> stack = null;//defer init till needed (after first level and its children are checked)
        Node cur = root;
        do {
            if (isHit(cur)) {
                return true;
            }
            if (cur.children != null) {
                for (Node child : cur.children) {
                    if (isHit(child)) {
                        return true;
                    }
                }
                //no luck, need to check next level(s)
                if (stack == null) {
                    stack = new ArrayDeque<>();
                }
                stack.addAll(cur.children);
            }
        } while (stack != null && ((cur = stack.pollLast()) != null));
        return false;
    }


    //depthFirst starting form "right", as most right (higher list index) is added last to the stack
    @Benchmark
    public boolean iterative_DF_R_pollLast_addAll() {
        ArrayDeque<Node> stack = new ArrayDeque<>();
        stack.push(root);

        Node cur;
        while ((cur = stack.pollLast()) != null) {
            if (isHit(cur)) {
                return true;
            }
            if (cur.children != null) {
                stack.addAll(cur.children);
            }
        }
        return false;
    }

    //depthFirst starting form "left", as most lest (lower list index) is added last to the stack
//    @Benchmark
//    public boolean iterative_DF_Left_pollLast_addAllInverse() {
//        ArrayDeque<Node> stack = new ArrayDeque<>();
//        stack.push(root);
//
//        Node cur;
//        while ((cur = stack.pollLast()) != null) {
//            if (isHit(cur)) {
//                return true;
//            }
//            if (cur.children != null) {
//                stack.addAll(ReverseListView.of(cur.children));
//            }
//        }
//        return false;
//    }

    @Benchmark
    public boolean iterative_DF_R_pollLast_addAll_shortCircuit() {
        //short circuit optimization - peel first two layers in n-ary tree
        if (isHit(root)) {
            return true;
        }
        if (root.children == null) {
            return false;
        } else {
            for (Node c : root.children) {
                if (isHit(c)) {
                    return true;
                }
            }
        }

        //.. ok so we need to decent further down
        ArrayDeque<Node> stack = new ArrayDeque<>(root.children);

        Node cur;
        while ((cur = stack.pollLast()) != null) {
            if (isHit(cur)) {
                return true;
            }
            if (cur.children != null) {
                stack.addAll(cur.children);
            }
        }
        return false;
    }


    //depthFirst starting form "left", as most left (lower list index) is decended into first before moving to next list index.
    @Benchmark
    public boolean recursive() {
        return recursive(root);
    }


    boolean recursive(Node b) {
        if (isHit(b)) {
            return true;
        }
        if (b.children != null) {
            for (Node child : b.children) {
                if (recursive(child)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isHit(Node b) {
        return terminalConditions.contains(b.content);
    }

    private static long getTotalNodesInNTree(int childrenPerNode, int depth) {
        //totalNodes=(L^D -1) / (L-1)    L=leavesPerNode, D=depth
        return (long) ((Math.pow(childrenPerNode, depth) - 1) / (childrenPerNode - 1));
    }

    private static Node generateNTree(TreeMetaData m) {
        return generateNTree(m.depth, m);
    }

    private static Node generateNTree(int curDepth, TreeMetaData m) {
        Node n = new Node();
        int nodeNumber = m.nodeNumber.incrementAndGet();
        n.setContent("DUMMY_" + nodeNumber);
        if (curDepth == 1) {
            if (m.terminalConditionPlace == TerminalConditionPlace.FIRST && nodeNumber == 0
                ||
                (m.terminalConditionPlace == TerminalConditionPlace.IN_THE_MIDDLE && nodeNumber == m.totalNodes / 2)) {
                n.setContent("FIND_ME");
            }
            return n;
        }

        n.children = new ArrayList<>(m.childrenPerNode);
        for (int i = 0; i < m.childrenPerNode; i++) {
            n.children.add(generateNTree(curDepth - 1, m));
        }
        return n;
    }

    private static void testTreeGenerationCorrectness() {
        boolean allCorrect = true;
        for (int depth : Arrays.asList(1, 2, 5)) {
            for (int leafs : Arrays.asList(2, 5, 10, 50)) {
                long expected = getTotalNodesInNTree(leafs, depth);
                AtomicInteger createdNodes = new AtomicInteger();
                generateNTree(new TreeMetaData(depth, leafs, expected, TerminalConditionPlace.NONE, createdNodes));
                if (expected != createdNodes.get()) {
                    allCorrect = false;
                }
                System.out.println("totalNodes(@depth=" + depth + ", childrenPerNode=" + leafs + "=" + expected +
                                   " == actual=" + createdNodes.get() + " ? " + (expected == createdNodes.get()));

            }
        }
        if (!allCorrect) {
            System.err.println("Tree generation error");
            System.exit(1);
        }
    }

    @Data
    public static class Node {
        List<Node> children;
        String content;
    }

    @Data
    @AllArgsConstructor
    private static class TreeMetaData {
        int depth;
        int childrenPerNode;
        long totalNodes;
        TerminalConditionPlace terminalConditionPlace;
        AtomicInteger nodeNumber;
    }

    public static class ReverseListView<E> extends AbstractList<E> {

        private final int lastIdx;
        private final List<E> backingList;

        private ReverseListView(List<E> backingList) {
            this.backingList = backingList;
            this.lastIdx = backingList.size() - 1;
        }

        public static <E> List<E> of(List<E> list) {
            return new ReverseListView<>(list);
        }

        @Override
        public E get(int i) {
            return backingList.get(lastIdx - i);
        }

        @Override
        public int size() {
            return lastIdx + 1;
        }

        public void forEach(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            for (int i = lastIdx; i >= 0; i--) {
                action.accept(backingList.get(i));
            }
        }
    }

    public static class NormalizedGcProfiler extends GCProfiler {
        public NormalizedGcProfiler(String initLine) throws ProfilerException {
            super(initLine);
        }

        @Override
        public Collection<? extends Result> afterIteration(BenchmarkParams benchmarkParams,
                                                           IterationParams iterationParams, IterationResult iResult) {
            Collection<? extends Result> results = super.afterIteration(benchmarkParams, iterationParams, iResult);
            return results.stream().filter(r -> r.getLabel().contains(".norm")).collect(Collectors.toList());
        }
    }
}
