package de.frank.jmh.algorithms;

import org.apache.commons.lang3.Validate;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/*--

Task:
=======
Escape certain chars in a string IF such chars are present (input is a Regex stored into a system where we need to escape the / char into \\.
Problem: we always copy the string into a StringBuilder - even if we do not escape anything (NO_MATCH_*) and thus input==output
Benchmark: try to avoid the StringBuilder or at least the resizing of the builder
  - original - always allocate new StringBuilder()
  - preSized - at least preSize the StringBuilder(original.size())
  - lazy - lazily allocate the StringBuilder at first match and at end: return builder==null?original:builder.toString();

Result:
======
best approach depends on expected "hit-rate" and input length
 - original performs best for string sizes < 16 - except if it does not need to escape anything -> lazy
 - preSize is nice for longer strings but has a penalty for short strings
 - Lazy is best if most of the time we do not need to escape anything, especially if we mostly have large input

if( (avgStringLength < 16 && hitRate < 50%)  //short and at least half of the input does not require escaping
  || avgStringLength > 16 && hitRate < 80%)) //or long input and at most 80% requires escaping
    => use lazy as default
}else{ avgStringLength < 16 && hitRate > 50)  == mostly short regexes and mostly in need of escaping
     => use original
}else if(avgStringLength > 16 ){
     => use preSized
}



Lower is better
Benchmark                                                    (match)  Mode  Cnt     Score     Error   Units
LazyEscape.original                                       MATCH_LONG  avgt   10   200.358 ±   8.151   ns/op   784.000 ±   0.001    B/op
LazyEscape.preSized                                       MATCH_LONG  avgt   10   169.780 ±   5.436   ns/op   624.000 ±   0.001    B/op #good
LazyEscape.lazy                                           MATCH_LONG  avgt   10   204.977 ±   4.938   ns/op   656.000 ±   0.001    B/op #cost of catching up and lazy init

LazyEscape.original                                    NO_MATCH_LONG  avgt   10   153.555 ±   3.616   ns/op   464.000 ±   0.001    B/op
LazyEscape.preSized                                    NO_MATCH_LONG  avgt   10   133.365 ±   3.901   ns/op   312.000 ±   0.001    B/op #good
LazyEscape.lazy                                        NO_MATCH_LONG  avgt   10    16.395 ±   1.072   ns/op    ≈ 10⁻⁵              B/op #zero-copy if nothing is matched

LazyEscape.original                                      MATCH_SHORT  avgt   10    29.836 ±   0.529   ns/op   128.000 ±   0.001    B/op
LazyEscape.preSized                                      MATCH_SHORT  avgt   10    42.338 ±   1.592   ns/op   128.000 ±   0.001    B/op #strange...
LazyEscape.lazy                                          MATCH_SHORT  avgt   10    47.190 ±   1.345   ns/op   152.000 ±   0.001    B/op #cost of catching up and lazy init

LazyEscape.original                                   NO_MATCH_SHORT  avgt   10    27.877 ±   0.515   ns/op   120.000 ±   0.001    B/op
LazyEscape.preSized                                   NO_MATCH_SHORT  avgt   10    41.396 ±   1.815   ns/op   120.000 ±   0.001    B/op #strange...
LazyEscape.lazy                                       NO_MATCH_SHORT  avgt   10     6.838 ±   0.261   ns/op    ≈ 10⁻⁶              B/op #zero-copy if nothing is matched


Benchmark                                                           (match)  (matchRate)  Mode  Cnt     Score     Error   Units
LazyInitStringBuilderJMH.lazy                                          LONG          0.0  avgt    5    16,759 ±   1,308   ns/op
LazyInitStringBuilderJMH.lazy:·gc.alloc.rate                           LONG          0.0  avgt    5    ≈ 10⁻⁴            MB/sec
LazyInitStringBuilderJMH.lazy:·gc.alloc.rate.norm                      LONG          0.0  avgt    5    ≈ 10⁻⁵              B/op
LazyInitStringBuilderJMH.lazy:·gc.count                                LONG          0.0  avgt    5       ≈ 0            counts
LazyInitStringBuilderJMH.lazy                                          LONG          1.0  avgt    5   217,752 ±   9,841   ns/op
LazyInitStringBuilderJMH.lazy:·gc.alloc.rate                           LONG          1.0  avgt    5  1119,823 ±  49,644  MB/sec
LazyInitStringBuilderJMH.lazy:·gc.alloc.rate.norm                      LONG          1.0  avgt    5   384,000 ±   0,001    B/op
LazyInitStringBuilderJMH.lazy:·gc.churn.PS_Eden_Space                  LONG          1.0  avgt    5  1161,163 ± 354,791  MB/sec
LazyInitStringBuilderJMH.lazy:·gc.churn.PS_Eden_Space.norm             LONG          1.0  avgt    5   398,262 ± 125,677    B/op
LazyInitStringBuilderJMH.lazy:·gc.churn.PS_Survivor_Space              LONG          1.0  avgt    5     0,046 ±   0,104  MB/sec
LazyInitStringBuilderJMH.lazy:·gc.churn.PS_Survivor_Space.norm         LONG          1.0  avgt    5     0,016 ±   0,036    B/op
LazyInitStringBuilderJMH.lazy:·gc.count                                LONG          1.0  avgt    5    26,000            counts
LazyInitStringBuilderJMH.lazy:·gc.time                                 LONG          1.0  avgt    5    26,000                ms
LazyInitStringBuilderJMH.lazy                                          LONG          0.5  avgt    5   171,477 ±   9,599   ns/op
LazyInitStringBuilderJMH.lazy:·gc.alloc.rate                           LONG          0.5  avgt    5   711,229 ±  41,144  MB/sec
LazyInitStringBuilderJMH.lazy:·gc.alloc.rate.norm                      LONG          0.5  avgt    5   192,009 ±   0,338    B/op
LazyInitStringBuilderJMH.lazy:·gc.churn.PS_Eden_Space                  LONG          0.5  avgt    5   702,959 ± 379,725  MB/sec
LazyInitStringBuilderJMH.lazy:·gc.churn.PS_Eden_Space.norm             LONG          0.5  avgt    5   190,084 ± 113,742    B/op
LazyInitStringBuilderJMH.lazy:·gc.churn.PS_Survivor_Space              LONG          0.5  avgt    5     0,033 ±   0,091  MB/sec
LazyInitStringBuilderJMH.lazy:·gc.churn.PS_Survivor_Space.norm         LONG          0.5  avgt    5     0,009 ±   0,024    B/op
LazyInitStringBuilderJMH.lazy:·gc.count                                LONG          0.5  avgt    5    16,000            counts
LazyInitStringBuilderJMH.lazy:·gc.time                                 LONG          0.5  avgt    5    14,000                ms
LazyInitStringBuilderJMH.lazy                                          LONG          0.8  avgt    5   208,878 ±  13,823   ns/op
LazyInitStringBuilderJMH.lazy:·gc.alloc.rate                           LONG          0.8  avgt    5   933,462 ±  58,586  MB/sec
LazyInitStringBuilderJMH.lazy:·gc.alloc.rate.norm                      LONG          0.8  avgt    5   307,179 ±   0,184    B/op
LazyInitStringBuilderJMH.lazy:·gc.churn.PS_Eden_Space                  LONG          0.8  avgt    5   933,859 ± 382,849  MB/sec
LazyInitStringBuilderJMH.lazy:·gc.churn.PS_Eden_Space.norm             LONG          0.8  avgt    5   307,071 ± 111,004    B/op
LazyInitStringBuilderJMH.lazy:·gc.churn.PS_Survivor_Space              LONG          0.8  avgt    5     0,042 ±   0,057  MB/sec
LazyInitStringBuilderJMH.lazy:·gc.churn.PS_Survivor_Space.norm         LONG          0.8  avgt    5     0,014 ±   0,019    B/op
LazyInitStringBuilderJMH.lazy:·gc.count                                LONG          0.8  avgt    5    21,000            counts
LazyInitStringBuilderJMH.lazy:·gc.time                                 LONG          0.8  avgt    5    28,000                ms
LazyInitStringBuilderJMH.lazy                                         SHORT          0.0  avgt    5     7,569 ±   0,284   ns/op
LazyInitStringBuilderJMH.lazy:·gc.alloc.rate                          SHORT          0.0  avgt    5    ≈ 10⁻⁴            MB/sec
LazyInitStringBuilderJMH.lazy:·gc.alloc.rate.norm                     SHORT          0.0  avgt    5    ≈ 10⁻⁶              B/op
LazyInitStringBuilderJMH.lazy:·gc.count                               SHORT          0.0  avgt    5       ≈ 0            counts
LazyInitStringBuilderJMH.lazy                                         SHORT          1.0  avgt    5    43,605 ±   3,016   ns/op
LazyInitStringBuilderJMH.lazy:·gc.alloc.rate                          SHORT          1.0  avgt    5  1865,708 ± 128,206  MB/sec
LazyInitStringBuilderJMH.lazy:·gc.alloc.rate.norm                     SHORT          1.0  avgt    5   128,000 ±   0,001    B/op
LazyInitStringBuilderJMH.lazy:·gc.churn.PS_Eden_Space                 SHORT          1.0  avgt    5  1895,186 ± 479,652  MB/sec
LazyInitStringBuilderJMH.lazy:·gc.churn.PS_Eden_Space.norm            SHORT          1.0  avgt    5   130,091 ±  36,783    B/op
LazyInitStringBuilderJMH.lazy:·gc.churn.PS_Survivor_Space             SHORT          1.0  avgt    5     0,071 ±   0,134  MB/sec
LazyInitStringBuilderJMH.lazy:·gc.churn.PS_Survivor_Space.norm        SHORT          1.0  avgt    5     0,005 ±   0,009    B/op
LazyInitStringBuilderJMH.lazy:·gc.count                               SHORT          1.0  avgt    5    42,000            counts
LazyInitStringBuilderJMH.lazy:·gc.time                                SHORT          1.0  avgt    5    40,000                ms
LazyInitStringBuilderJMH.lazy                                         SHORT          0.5  avgt    5    37,358 ±   1,112   ns/op
LazyInitStringBuilderJMH.lazy:·gc.alloc.rate                          SHORT          0.5  avgt    5  1224,201 ±  32,149  MB/sec
LazyInitStringBuilderJMH.lazy:·gc.alloc.rate.norm                     SHORT          0.5  avgt    5    72,002 ±   0,068    B/op
LazyInitStringBuilderJMH.lazy:·gc.churn.PS_Eden_Space                 SHORT          0.5  avgt    5  1253,977 ± 467,118  MB/sec
LazyInitStringBuilderJMH.lazy:·gc.churn.PS_Eden_Space.norm            SHORT          0.5  avgt    5    73,742 ±  26,858    B/op
LazyInitStringBuilderJMH.lazy:·gc.churn.PS_Survivor_Space             SHORT          0.5  avgt    5     0,066 ±   0,067  MB/sec
LazyInitStringBuilderJMH.lazy:·gc.churn.PS_Survivor_Space.norm        SHORT          0.5  avgt    5     0,004 ±   0,004    B/op
LazyInitStringBuilderJMH.lazy:·gc.count                               SHORT          0.5  avgt    5    28,000            counts
LazyInitStringBuilderJMH.lazy:·gc.time                                SHORT          0.5  avgt    5    30,000                ms
LazyInitStringBuilderJMH.lazy                                         SHORT          0.8  avgt    5    40,655 ±   0,819   ns/op
LazyInitStringBuilderJMH.lazy:·gc.alloc.rate                          SHORT          0.8  avgt    5  1649,241 ±  31,022  MB/sec
LazyInitStringBuilderJMH.lazy:·gc.alloc.rate.norm                     SHORT          0.8  avgt    5   105,599 ±   0,023    B/op
LazyInitStringBuilderJMH.lazy:·gc.churn.PS_Eden_Space                 SHORT          0.8  avgt    5  1666,226 ± 479,562  MB/sec
LazyInitStringBuilderJMH.lazy:·gc.churn.PS_Eden_Space.norm            SHORT          0.8  avgt    5   106,685 ±  30,557    B/op
LazyInitStringBuilderJMH.lazy:·gc.churn.PS_Survivor_Space             SHORT          0.8  avgt    5     0,058 ±   0,067  MB/sec
LazyInitStringBuilderJMH.lazy:·gc.churn.PS_Survivor_Space.norm        SHORT          0.8  avgt    5     0,004 ±   0,004    B/op
LazyInitStringBuilderJMH.lazy:·gc.count                               SHORT          0.8  avgt    5    37,000            counts
LazyInitStringBuilderJMH.lazy:·gc.time                                SHORT          0.8  avgt    5    39,000                ms
LazyInitStringBuilderJMH.original                                      LONG          0.0  avgt    5   180,438 ±  69,727   ns/op
LazyInitStringBuilderJMH.original:·gc.alloc.rate                       LONG          0.0  avgt    5   992,878 ± 336,470  MB/sec
LazyInitStringBuilderJMH.original:·gc.alloc.rate.norm                  LONG          0.0  avgt    5   280,000 ±   0,001    B/op
LazyInitStringBuilderJMH.original:·gc.churn.PS_Eden_Space              LONG          0.0  avgt    5   979,708 ± 460,852  MB/sec
LazyInitStringBuilderJMH.original:·gc.churn.PS_Eden_Space.norm         LONG          0.0  avgt    5   277,208 ± 126,093    B/op
LazyInitStringBuilderJMH.original:·gc.churn.PS_Survivor_Space          LONG          0.0  avgt    5     0,037 ±   0,067  MB/sec
LazyInitStringBuilderJMH.original:·gc.churn.PS_Survivor_Space.norm     LONG          0.0  avgt    5     0,011 ±   0,019    B/op
LazyInitStringBuilderJMH.original:·gc.count                            LONG          0.0  avgt    5    22,000            counts
LazyInitStringBuilderJMH.original:·gc.time                             LONG          0.0  avgt    5    24,000                ms
LazyInitStringBuilderJMH.original                                      LONG          1.0  avgt    5   199,549 ±  19,157   ns/op
LazyInitStringBuilderJMH.original:·gc.alloc.rate                       LONG          1.0  avgt    5  1426,352 ± 140,814  MB/sec
LazyInitStringBuilderJMH.original:·gc.alloc.rate.norm                  LONG          1.0  avgt    5   448,000 ±   0,001    B/op
LazyInitStringBuilderJMH.original:·gc.churn.PS_Eden_Space              LONG          1.0  avgt    5  1435,371 ± 462,001  MB/sec
LazyInitStringBuilderJMH.original:·gc.churn.PS_Eden_Space.norm         LONG          1.0  avgt    5   450,548 ± 119,475    B/op
LazyInitStringBuilderJMH.original:·gc.churn.PS_Survivor_Space          LONG          1.0  avgt    5     0,046 ±   0,036  MB/sec
LazyInitStringBuilderJMH.original:·gc.churn.PS_Survivor_Space.norm     LONG          1.0  avgt    5     0,014 ±   0,011    B/op
LazyInitStringBuilderJMH.original:·gc.count                            LONG          1.0  avgt    5    32,000            counts
LazyInitStringBuilderJMH.original:·gc.time                             LONG          1.0  avgt    5    30,000                ms
LazyInitStringBuilderJMH.original                                      LONG          0.5  avgt    5   195,375 ±  51,857   ns/op
LazyInitStringBuilderJMH.original:·gc.alloc.rate                       LONG          0.5  avgt    5  1187,018 ± 294,824  MB/sec
LazyInitStringBuilderJMH.original:·gc.alloc.rate.norm                  LONG          0.5  avgt    5   363,996 ±   0,165    B/op
LazyInitStringBuilderJMH.original:·gc.churn.PS_Eden_Space              LONG          0.5  avgt    5  1208,034 ± 480,214  MB/sec
LazyInitStringBuilderJMH.original:·gc.churn.PS_Eden_Space.norm         LONG          0.5  avgt    5   370,305 ± 102,356    B/op
LazyInitStringBuilderJMH.original:·gc.churn.PS_Survivor_Space          LONG          0.5  avgt    5     0,071 ±   0,107  MB/sec
LazyInitStringBuilderJMH.original:·gc.churn.PS_Survivor_Space.norm     LONG          0.5  avgt    5     0,022 ±   0,035    B/op
LazyInitStringBuilderJMH.original:·gc.count                            LONG          0.5  avgt    5    27,000            counts
LazyInitStringBuilderJMH.original:·gc.time                             LONG          0.5  avgt    5    26,000                ms
LazyInitStringBuilderJMH.original                                      LONG          0.8  avgt    5   197,163 ±  48,980   ns/op
LazyInitStringBuilderJMH.original:·gc.alloc.rate                       LONG          0.8  avgt    5  1339,122 ± 320,802  MB/sec
LazyInitStringBuilderJMH.original:·gc.alloc.rate.norm                  LONG          0.8  avgt    5   414,383 ±   0,064    B/op
LazyInitStringBuilderJMH.original:·gc.churn.PS_Eden_Space              LONG          0.8  avgt    5  1303,623 ± 381,595  MB/sec
LazyInitStringBuilderJMH.original:·gc.churn.PS_Eden_Space.norm         LONG          0.8  avgt    5   403,354 ±  69,400    B/op
LazyInitStringBuilderJMH.original:·gc.churn.PS_Survivor_Space          LONG          0.8  avgt    5     0,054 ±   0,044  MB/sec
LazyInitStringBuilderJMH.original:·gc.churn.PS_Survivor_Space.norm     LONG          0.8  avgt    5     0,017 ±   0,012    B/op
LazyInitStringBuilderJMH.original:·gc.count                            LONG          0.8  avgt    5    29,000            counts
LazyInitStringBuilderJMH.original:·gc.time                             LONG          0.8  avgt    5    29,000                ms
LazyInitStringBuilderJMH.original                                     SHORT          0.0  avgt    5    34,719 ±  10,971   ns/op
LazyInitStringBuilderJMH.original:·gc.alloc.rate                      SHORT          0.0  avgt    5  1765,384 ± 546,872  MB/sec
LazyInitStringBuilderJMH.original:·gc.alloc.rate.norm                 SHORT          0.0  avgt    5    96,000 ±   0,001    B/op
LazyInitStringBuilderJMH.original:·gc.churn.PS_Eden_Space             SHORT          0.0  avgt    5  1804,951 ± 603,940  MB/sec
LazyInitStringBuilderJMH.original:·gc.churn.PS_Eden_Space.norm        SHORT          0.0  avgt    5    98,182 ±  16,194    B/op
LazyInitStringBuilderJMH.original:·gc.churn.PS_Survivor_Space         SHORT          0.0  avgt    5     0,075 ±   0,122  MB/sec
LazyInitStringBuilderJMH.original:·gc.churn.PS_Survivor_Space.norm    SHORT          0.0  avgt    5     0,004 ±   0,006    B/op
LazyInitStringBuilderJMH.original:·gc.count                           SHORT          0.0  avgt    5    40,000            counts
LazyInitStringBuilderJMH.original:·gc.time                            SHORT          0.0  avgt    5    35,000                ms
LazyInitStringBuilderJMH.original                                     SHORT          1.0  avgt    5    37,234 ±   3,773   ns/op
LazyInitStringBuilderJMH.original:·gc.alloc.rate                      SHORT          1.0  avgt    5  1774,521 ± 171,277  MB/sec
LazyInitStringBuilderJMH.original:·gc.alloc.rate.norm                 SHORT          1.0  avgt    5   104,000 ±   0,001    B/op
LazyInitStringBuilderJMH.original:·gc.churn.PS_Eden_Space             SHORT          1.0  avgt    5  1802,999 ±  17,332  MB/sec
LazyInitStringBuilderJMH.original:·gc.churn.PS_Eden_Space.norm        SHORT          1.0  avgt    5   105,723 ±  10,503    B/op
LazyInitStringBuilderJMH.original:·gc.churn.PS_Survivor_Space         SHORT          1.0  avgt    5     0,079 ±   0,104  MB/sec
LazyInitStringBuilderJMH.original:·gc.churn.PS_Survivor_Space.norm    SHORT          1.0  avgt    5     0,005 ±   0,006    B/op
LazyInitStringBuilderJMH.original:·gc.count                           SHORT          1.0  avgt    5    40,000            counts
LazyInitStringBuilderJMH.original:·gc.time                            SHORT          1.0  avgt    5    38,000                ms
LazyInitStringBuilderJMH.original                                     SHORT          0.5  avgt    5    40,794 ±   1,891   ns/op
LazyInitStringBuilderJMH.original:·gc.alloc.rate                      SHORT          0.5  avgt    5  1558,063 ±  69,060  MB/sec
LazyInitStringBuilderJMH.original:·gc.alloc.rate.norm                 SHORT          0.5  avgt    5   100,000 ±   0,007    B/op
LazyInitStringBuilderJMH.original:·gc.churn.PS_Eden_Space             SHORT          0.5  avgt    5  1572,695 ±  25,979  MB/sec
LazyInitStringBuilderJMH.original:·gc.churn.PS_Eden_Space.norm        SHORT          0.5  avgt    5   100,953 ±   6,066    B/op
LazyInitStringBuilderJMH.original:·gc.churn.PS_Survivor_Space         SHORT          0.5  avgt    5     0,062 ±   0,080  MB/sec
LazyInitStringBuilderJMH.original:·gc.churn.PS_Survivor_Space.norm    SHORT          0.5  avgt    5     0,004 ±   0,005    B/op
LazyInitStringBuilderJMH.original:·gc.count                           SHORT          0.5  avgt    5    35,000            counts
LazyInitStringBuilderJMH.original:·gc.time                            SHORT          0.5  avgt    5    38,000                ms
LazyInitStringBuilderJMH.original                                     SHORT          0.8  avgt    5    39,977 ±  10,643   ns/op
LazyInitStringBuilderJMH.original:·gc.alloc.rate                      SHORT          0.8  avgt    5  1632,267 ± 425,359  MB/sec
LazyInitStringBuilderJMH.original:·gc.alloc.rate.norm                 SHORT          0.8  avgt    5   102,400 ±   0,003    B/op
LazyInitStringBuilderJMH.original:·gc.churn.PS_Eden_Space             SHORT          0.8  avgt    5  1619,653 ± 373,184  MB/sec
LazyInitStringBuilderJMH.original:·gc.churn.PS_Eden_Space.norm        SHORT          0.8  avgt    5   101,858 ±  27,102    B/op
LazyInitStringBuilderJMH.original:·gc.churn.PS_Survivor_Space         SHORT          0.8  avgt    5     0,050 ±   0,044  MB/sec
LazyInitStringBuilderJMH.original:·gc.churn.PS_Survivor_Space.norm    SHORT          0.8  avgt    5     0,003 ±   0,002    B/op
LazyInitStringBuilderJMH.original:·gc.count                           SHORT          0.8  avgt    5    36,000            counts
LazyInitStringBuilderJMH.original:·gc.time                            SHORT          0.8  avgt    5    37,000                ms
LazyInitStringBuilderJMH.preSized                                      LONG          0.0  avgt    5   165,075 ±  29,282   ns/op
LazyInitStringBuilderJMH.preSized:·gc.alloc.rate                       LONG          0.0  avgt    5   709,270 ± 129,189  MB/sec
LazyInitStringBuilderJMH.preSized:·gc.alloc.rate.norm                  LONG          0.0  avgt    5   184,000 ±   0,001    B/op
LazyInitStringBuilderJMH.preSized:·gc.churn.PS_Eden_Space              LONG          0.0  avgt    5   702,261 ± 375,980  MB/sec
LazyInitStringBuilderJMH.preSized:·gc.churn.PS_Eden_Space.norm         LONG          0.0  avgt    5   182,395 ±  98,740    B/op
LazyInitStringBuilderJMH.preSized:·gc.churn.PS_Survivor_Space          LONG          0.0  avgt    5     0,037 ±   0,104  MB/sec
LazyInitStringBuilderJMH.preSized:·gc.churn.PS_Survivor_Space.norm     LONG          0.0  avgt    5     0,010 ±   0,029    B/op
LazyInitStringBuilderJMH.preSized:·gc.count                            LONG          0.0  avgt    5    16,000            counts
LazyInitStringBuilderJMH.preSized:·gc.time                             LONG          0.0  avgt    5    16,000                ms
LazyInitStringBuilderJMH.preSized                                      LONG          1.0  avgt    5   203,783 ±  24,448   ns/op
LazyInitStringBuilderJMH.preSized:·gc.alloc.rate                       LONG          1.0  avgt    5  1097,859 ± 129,781  MB/sec
LazyInitStringBuilderJMH.preSized:·gc.alloc.rate.norm                  LONG          1.0  avgt    5   352,000 ±   0,001    B/op
LazyInitStringBuilderJMH.preSized:·gc.churn.PS_Eden_Space              LONG          1.0  avgt    5  1115,913 ±  34,460  MB/sec
LazyInitStringBuilderJMH.preSized:·gc.churn.PS_Eden_Space.norm         LONG          1.0  avgt    5   358,048 ±  42,348    B/op
LazyInitStringBuilderJMH.preSized:·gc.churn.PS_Survivor_Space          LONG          1.0  avgt    5     0,054 ±   0,134  MB/sec
LazyInitStringBuilderJMH.preSized:·gc.churn.PS_Survivor_Space.norm     LONG          1.0  avgt    5     0,017 ±   0,043    B/op
LazyInitStringBuilderJMH.preSized:·gc.count                            LONG          1.0  avgt    5    25,000            counts
LazyInitStringBuilderJMH.preSized:·gc.time                             LONG          1.0  avgt    5    35,000                ms
LazyInitStringBuilderJMH.preSized                                      LONG          0.5  avgt    5   180,962 ±  34,517   ns/op
LazyInitStringBuilderJMH.preSized:·gc.alloc.rate                       LONG          0.5  avgt    5   941,549 ± 170,244  MB/sec
LazyInitStringBuilderJMH.preSized:·gc.alloc.rate.norm                  LONG          0.5  avgt    5   267,984 ±   0,187    B/op
LazyInitStringBuilderJMH.preSized:·gc.churn.PS_Eden_Space              LONG          0.5  avgt    5   934,113 ± 400,311  MB/sec
LazyInitStringBuilderJMH.preSized:·gc.churn.PS_Eden_Space.norm         LONG          0.5  avgt    5   265,759 ±  93,910    B/op
LazyInitStringBuilderJMH.preSized:·gc.churn.PS_Survivor_Space          LONG          0.5  avgt    5     0,041 ±   0,126  MB/sec
LazyInitStringBuilderJMH.preSized:·gc.churn.PS_Survivor_Space.norm     LONG          0.5  avgt    5     0,012 ±   0,036    B/op
LazyInitStringBuilderJMH.preSized:·gc.count                            LONG          0.5  avgt    5    21,000            counts
LazyInitStringBuilderJMH.preSized:·gc.time                             LONG          0.5  avgt    5    20,000                ms
LazyInitStringBuilderJMH.preSized                                      LONG          0.8  avgt    5   187,120 ±  27,450   ns/op
LazyInitStringBuilderJMH.preSized:·gc.alloc.rate                       LONG          0.8  avgt    5  1082,004 ± 158,992  MB/sec
LazyInitStringBuilderJMH.preSized:·gc.alloc.rate.norm                  LONG          0.8  avgt    5   318,405 ±   0,093    B/op
LazyInitStringBuilderJMH.preSized:·gc.churn.PS_Eden_Space              LONG          0.8  avgt    5  1070,507 ± 379,903  MB/sec
LazyInitStringBuilderJMH.preSized:·gc.churn.PS_Eden_Space.norm         LONG          0.8  avgt    5   315,484 ± 125,960    B/op
LazyInitStringBuilderJMH.preSized:·gc.churn.PS_Survivor_Space          LONG          0.8  avgt    5     0,046 ±   0,067  MB/sec
LazyInitStringBuilderJMH.preSized:·gc.churn.PS_Survivor_Space.norm     LONG          0.8  avgt    5     0,013 ±   0,018    B/op
LazyInitStringBuilderJMH.preSized:·gc.count                            LONG          0.8  avgt    5    24,000            counts
LazyInitStringBuilderJMH.preSized:·gc.time                             LONG          0.8  avgt    5    25,000                ms
LazyInitStringBuilderJMH.preSized                                     SHORT          0.0  avgt    5    36,598 ±   5,793   ns/op
LazyInitStringBuilderJMH.preSized:·gc.alloc.rate                      SHORT          0.0  avgt    5  1668,893 ± 255,265  MB/sec
LazyInitStringBuilderJMH.preSized:·gc.alloc.rate.norm                 SHORT          0.0  avgt    5    96,000 ±   0,001    B/op
LazyInitStringBuilderJMH.preSized:·gc.churn.PS_Eden_Space             SHORT          0.0  avgt    5  1666,360 ± 469,911  MB/sec
LazyInitStringBuilderJMH.preSized:·gc.churn.PS_Eden_Space.norm        SHORT          0.0  avgt    5    95,822 ±  19,478    B/op
LazyInitStringBuilderJMH.preSized:·gc.churn.PS_Survivor_Space         SHORT          0.0  avgt    5     0,062 ±   0,098  MB/sec
LazyInitStringBuilderJMH.preSized:·gc.churn.PS_Survivor_Space.norm    SHORT          0.0  avgt    5     0,004 ±   0,005    B/op
LazyInitStringBuilderJMH.preSized:·gc.count                           SHORT          0.0  avgt    5    37,000            counts
LazyInitStringBuilderJMH.preSized:·gc.time                            SHORT          0.0  avgt    5    44,000                ms
LazyInitStringBuilderJMH.preSized                                     SHORT          1.0  avgt    5    40,762 ±  13,797   ns/op
LazyInitStringBuilderJMH.preSized:·gc.alloc.rate                      SHORT          1.0  avgt    5  1630,723 ± 523,415  MB/sec
LazyInitStringBuilderJMH.preSized:·gc.alloc.rate.norm                 SHORT          1.0  avgt    5   104,000 ±   0,001    B/op
LazyInitStringBuilderJMH.preSized:·gc.churn.PS_Eden_Space             SHORT          1.0  avgt    5  1667,648 ± 767,278  MB/sec
LazyInitStringBuilderJMH.preSized:·gc.churn.PS_Eden_Space.norm        SHORT          1.0  avgt    5   106,099 ±  18,570    B/op
LazyInitStringBuilderJMH.preSized:·gc.churn.PS_Survivor_Space         SHORT          1.0  avgt    5     0,071 ±   0,072  MB/sec
LazyInitStringBuilderJMH.preSized:·gc.churn.PS_Survivor_Space.norm    SHORT          1.0  avgt    5     0,005 ±   0,005    B/op
LazyInitStringBuilderJMH.preSized:·gc.count                           SHORT          1.0  avgt    5    37,000            counts
LazyInitStringBuilderJMH.preSized:·gc.time                            SHORT          1.0  avgt    5    37,000                ms
LazyInitStringBuilderJMH.preSized                                     SHORT          0.5  avgt    5    43,315 ±   5,483   ns/op
LazyInitStringBuilderJMH.preSized:·gc.alloc.rate                      SHORT          0.5  avgt    5  1467,530 ± 191,988  MB/sec
LazyInitStringBuilderJMH.preSized:·gc.alloc.rate.norm                 SHORT          0.5  avgt    5   100,000 ±   0,003    B/op
LazyInitStringBuilderJMH.preSized:·gc.churn.PS_Eden_Space             SHORT          0.5  avgt    5  1439,134 ± 479,752  MB/sec
LazyInitStringBuilderJMH.preSized:·gc.churn.PS_Eden_Space.norm        SHORT          0.5  avgt    5    98,055 ±  29,091    B/op
LazyInitStringBuilderJMH.preSized:·gc.churn.PS_Survivor_Space         SHORT          0.5  avgt    5     0,062 ±   0,113  MB/sec
LazyInitStringBuilderJMH.preSized:·gc.churn.PS_Survivor_Space.norm    SHORT          0.5  avgt    5     0,004 ±   0,008    B/op
LazyInitStringBuilderJMH.preSized:·gc.count                           SHORT          0.5  avgt    5    32,000            counts
LazyInitStringBuilderJMH.preSized:·gc.time                            SHORT          0.5  avgt    5    31,000                ms
LazyInitStringBuilderJMH.preSized                                     SHORT          0.8  avgt    5    39,441 ±   1,844   ns/op
LazyInitStringBuilderJMH.preSized:·gc.alloc.rate                      SHORT          0.8  avgt    5  1646,441 ±  77,881  MB/sec
LazyInitStringBuilderJMH.preSized:·gc.alloc.rate.norm                 SHORT          0.8  avgt    5   102,400 ±   0,004    B/op
LazyInitStringBuilderJMH.preSized:·gc.churn.PS_Eden_Space             SHORT          0.8  avgt    5  1666,422 ± 482,228  MB/sec
LazyInitStringBuilderJMH.preSized:·gc.churn.PS_Eden_Space.norm        SHORT          0.8  avgt    5   103,665 ±  31,033    B/op
LazyInitStringBuilderJMH.preSized:·gc.churn.PS_Survivor_Space         SHORT          0.8  avgt    5     0,058 ±   0,088  MB/sec
LazyInitStringBuilderJMH.preSized:·gc.churn.PS_Survivor_Space.norm    SHORT          0.8  avgt    5     0,004 ±   0,005    B/op
LazyInitStringBuilderJMH.preSized:·gc.count                           SHORT          0.8  avgt    5    37,000            counts
LazyInitStringBuilderJMH.preSized:·gc.time                            SHORT          0.8  avgt    5    33,000                ms

 */
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1, jvmArgsAppend = {"-XX:+UseParallelGC", "-Xms1g", "-Xmx1g"})
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class StringReplaceLazyStringBuilderJMH {

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()//
                .include(StringReplaceLazyStringBuilderJMH.class.getName() + ".*")//
                .result(String.format("%s_%s.json",
                        DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                        StringReplaceLazyStringBuilderJMH.class.getSimpleName()))
                .addProfiler(GCProfiler.class)//
                .build();
        new Runner(opt).run();
    }


    @State(Scope.Thread)
    public static class RegexStringHolder {
        public enum Match {
            LONG("asfla/ksfasdfxv/yxvsdfasdfasdfasf/safascyxcyxccvydsfs/dfsadfxvcyxcv", "asflaksfasdfxvyxvsdfasdfasdfasfsafascyxcyxccvydsfsdfsadfxvcyxcv"),
            SHORT("asfla/ksf", "asflaksf");

            private final String match, noMatch;

            Match(String match, String noMatch) {
                this.match = match;
                this.noMatch = noMatch;
            }
        }

        @Param({"LONG", "SHORT"})
        public Match match = Match.SHORT;

        @Param({"0.0", "1.0", "0.5", "0.8"})
        public double matchRate = 0.0;


        public String getRegexString() {
            return ThreadLocalRandom.current().nextDouble() <= matchRate ? match.match : match.noMatch;
        }
    }


    @Benchmark
    public String original(RegexStringHolder s) {
        return escapeRegex_orig(s.getRegexString());
    }

    @Benchmark
    public String preSized(RegexStringHolder s) {
        return escapeRegex_preSized(s.getRegexString());
    }

    @Benchmark
    public String lazy(RegexStringHolder s) {
        return escapeRegex_lazy(s.getRegexString());
    }


    static String escapeRegex_preSized(String javaRegex) {
        Validate.notEmpty(javaRegex);

        StringBuilder stringBuilder = new StringBuilder(Math.max(16, javaRegex.length()));
        for (int index = 0; index < javaRegex.length(); index++) {
            char character = javaRegex.charAt(index);

            //The Regex will be embedded into '/' therefore we need to escape them.
            if (character == '/') {
                stringBuilder.append('\\');
            }
            stringBuilder.append(character);
        }
        return stringBuilder.toString();
    }

    static String escapeRegex_orig(String javaRegex) {
        Validate.notEmpty(javaRegex);

        StringBuilder stringBuilder = new StringBuilder();
        for (int index = 0; index < javaRegex.length(); index++) {
            char character = javaRegex.charAt(index);

            //The Regex will be embedded into '/' therefore we need to escape them.
            if (character == '/') {
                stringBuilder.append('\\');
            }
            stringBuilder.append(character);
        }
        return stringBuilder.toString();
    }

    static String escapeRegex_lazy(String javaRegex) {
        Validate.notEmpty(javaRegex);

        StringBuilder stringBuilder = null;
        for (int index = 0; index < javaRegex.length(); index++) {
            char character = javaRegex.charAt(index);

            //The Regex will be embedded into '/' therefore we need to escape them.
            if (character == '/') {
                if (stringBuilder == null) {
                    stringBuilder = new StringBuilder(Math.max(16, javaRegex.length() + 1));
                    stringBuilder.append(javaRegex, 0, Math.max(0, index - 1));
                }
                stringBuilder.append('\\');
            }
            if (stringBuilder != null)
                stringBuilder.append(character);
        }
        return stringBuilder == null ? javaRegex : stringBuilder.toString();
    }


}
