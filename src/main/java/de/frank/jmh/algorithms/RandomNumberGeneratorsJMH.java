package de.frank.jmh.algorithms;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well44497b;
import org.apache.commons.math3.random.Well512a;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
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
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;
import java.util.SplittableRandom;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/*--

Disclaimer:
This benchmark settles for "drop in" replacement's for java.util.Random and its interface.
Some generators have special methods to generate bounded doubles, floats, ints and longs correct and efficiently but theses are not included in this benchmark - only measuring methods in common.

@ VM version: JDK 1.8.0_161, VM 25.161-b12
@ Single Threaded
                                                 double       double          int          int        long
                                      bool      bounded    unbounded      bounded    unbounded    unbounded        Average  Comment
JDK.Random                      84.530.322   40.726.804   41.015.961   71.972.563   83.968.432   41.933.529    60.691.269 # DONT EVER USE! - this is just plain bad. Use ThreadLocalRandom as dropin replacement.
JDK.SplitableRandom            222.596.847  171.889.636  180.398.036  111.568.686  251.920.298  240.194.560   196.428.010 # splitMix64 - but lacks the jdk.Random "interface" and has a few shortcomings
JDK.ThreadLocalRandom          228.848.173  192.477.756  202.704.545  115.587.554  245.179.387  240.212.027   204.168.240 # splitMix64 - VERY good - Prefer this one if you don't have special requirements.
JDK.SecureRandom_SHA1           24.040.454    4.631.773    4.581.726    8.863.876    9.117.086    4.644.490     9.313.234 # way to go for normal secure stuff (crypto keys, salts, ....)
JDK.SecureRandom_STRONG             12.492        6.760        6.585       13.609      13.598         6.651         9.949 # ULTRA slow - no reallife benefit over SecureRandom_SHA1 - only viable for you CA Master Private Key

commons.MerseneTwister         130.504.838   69.272.325   77.490.124   77.615.564  134.250.753   92.389.524    96.920.521 # well known but special - long period
commons.Well44497b              77.605.088   34.802.570   35.024.216   55.943.941   81.154.884   36.773.720    53.550.737 # well known but special - very long period
commons.Well512a               105.534.028   59.444.862   59.526.684   66.699.357  107.752.658   57.129.972    76.014.594 # well known but special - long period
dsi.XoRoShiRo128PlusRandom*    229.216.318  181.556.846  201.642.720   56.639.801  240.527.720  261.998.544   195.263.658 # current state of the art in super fast medium period PRNG's
dsi.XorShift1024StarPhiRandom  184.317.442  150.684.543  159.733.854   51.764.245  195.200.861  206.921.179   158.103.688 # current state of the art in super fast high period PRNG's
dsi.XorShift1024StarRandom*    131.331.083  104.839.167  118.124.601   44.632.823  147.003.966  132.848.090   113.129.955 # *DEPRECATED* essentially xorshift64* wich a much larger period.
dsi.XorShift64StarRandom*      173.209.529  131.579.301  160.454.976   56.002.236  189.603.417  195.882.252   151.121.952 # *DEPRECATED* super simplistic and fast, but splitMix64 and XoRoShiRo128Plus are superior. based on George Marsaglia's Xorshift generators.

Remarks: Interestingly all PRNG's have performance issues generating bounded integers

@ VM version: JDK 10, VM 10+46
@ Single Threaded
                                                 double       double          int          int        long
                                      bool      bounded    unbounded      bounded    unbounded    unbounded        Average  Comment
JDK.Random                      84.688.418   42.648.737   37.877.992   69.664.586   75.352.850   42.498.463     58.788.508
JDK.SplitableRandom            219.565.640  166.783.275  142.240.708  102.144.697  185.627.930  231.352.310    174.619.093
JDK.ThreadLocalRandom          227.544.151  194.500.916  160.835.113   87.871.808  180.509.087  227.958.946    179.870.003
JDK.SecureRandom_SHA1           32.716.090    4.432.020    4.649.020    9.601.989    9.065.637    5.512.551     10.996.218
JDK.SecureRandom_STRONG             12.746        4.766        5.588       12.005       10.605        6.627          8.723

commons.MerseneTwister         130.539.263   63.523.568   64.603.732   65.960.251  129.267.386   89.266.765     90.526.828
commons.Well44497b              83.922.486   30.177.240   31.733.013   53.928.039   70.456.861   36.337.797     51.092.573
commons.Well512a               103.263.483   51.662.151   52.029.243   67.210.906   86.372.272   64.045.734     70.763.965
dsi.XoRoShiRo128PlusRandom*    228.149.480  162.502.615  168.092.234   55.604.014  227.019.846  253.364.841    182.455.505
dsi.XorShift1024StarPhiRandom  192.542.221  119.665.989  130.277.979   49.274.933  183.984.705  202.520.136    146.377.660


RAW:(JDK 1.8)
# Run complete. Total time: 00:25:40
Benchmark                                                      (implName)  (int_bound)   Mode  Cnt          Score          Error  Units
RandomNumberGeneratorsJMH.bool                                 JDK.Random           10  thrpt   15   84530321,930 ±  1259183,160  ops/s
RandomNumberGeneratorsJMH.bool                      JDK.ThreadLocalRandom           10  thrpt   15  228848173,206 ±  2963717,834  ops/s
RandomNumberGeneratorsJMH.bool                        JDK.SplitableRandom           10  thrpt   15  222596846,618 ±  4913162,692  ops/s
RandomNumberGeneratorsJMH.bool                      JDK.SecureRandom_SHA1           10  thrpt   15   24040453,871 ±   765259,933  ops/s
RandomNumberGeneratorsJMH.bool                    JDK.SecureRandom_STRONG           10  thrpt   15      12491,848 ±      761,944  ops/s
RandomNumberGeneratorsJMH.bool                           commons.Well512a           10  thrpt   15  105534028,160 ±  4723896,920  ops/s
RandomNumberGeneratorsJMH.bool                         commons.Well44497b           10  thrpt   15   77605087,900 ±  4105039,665  ops/s
RandomNumberGeneratorsJMH.bool                     commons.MerseneTwister           10  thrpt   15  130504838,124 ±  1276116,633  ops/s
RandomNumberGeneratorsJMH.bool                dsi.XoRoShiRo128PlusRandom*           10  thrpt   15  229216318,163 ±  4306766,764  ops/s
RandomNumberGeneratorsJMH.bool              dsi.XorShift1024StarPhiRandom           10  thrpt   15  184317442,245 ±  6279707,864  ops/s
RandomNumberGeneratorsJMH.double_bounded                       JDK.Random           10  thrpt   15   40726804,238 ±   647290,391  ops/s
RandomNumberGeneratorsJMH.double_bounded            JDK.ThreadLocalRandom           10  thrpt   15  192477756,329 ±  2819895,076  ops/s
RandomNumberGeneratorsJMH.double_bounded              JDK.SplitableRandom           10  thrpt   15  171889635,557 ±  3748437,737  ops/s
RandomNumberGeneratorsJMH.double_bounded            JDK.SecureRandom_SHA1           10  thrpt   15    4631773,440 ±    77834,590  ops/s
RandomNumberGeneratorsJMH.double_bounded          JDK.SecureRandom_STRONG           10  thrpt   15       6760,164 ±      108,313  ops/s
RandomNumberGeneratorsJMH.double_bounded                 commons.Well512a           10  thrpt   15   59444861,550 ±   462262,088  ops/s
RandomNumberGeneratorsJMH.double_bounded               commons.Well44497b           10  thrpt   15   34802569,937 ±   315795,138  ops/s
RandomNumberGeneratorsJMH.double_bounded           commons.MerseneTwister           10  thrpt   15   69272325,102 ±  7407738,863  ops/s
RandomNumberGeneratorsJMH.double_bounded      dsi.XoRoShiRo128PlusRandom*           10  thrpt   15  181556846,490 ± 25388869,124  ops/s
RandomNumberGeneratorsJMH.double_bounded    dsi.XorShift1024StarPhiRandom           10  thrpt   15  150684542,976 ±  7637349,086  ops/s
RandomNumberGeneratorsJMH.double_unbounded                     JDK.Random           10  thrpt   15   41015960,570 ±   883273,499  ops/s
RandomNumberGeneratorsJMH.double_unbounded          JDK.ThreadLocalRandom           10  thrpt   15  202704544,683 ±  2530864,180  ops/s
RandomNumberGeneratorsJMH.double_unbounded            JDK.SplitableRandom           10  thrpt   15  180398036,024 ±  3028758,387  ops/s
RandomNumberGeneratorsJMH.double_unbounded          JDK.SecureRandom_SHA1           10  thrpt   15    4581726,087 ±    66989,349  ops/s
RandomNumberGeneratorsJMH.double_unbounded        JDK.SecureRandom_STRONG           10  thrpt   15       6585,264 ±      272,004  ops/s
RandomNumberGeneratorsJMH.double_unbounded               commons.Well512a           10  thrpt   15   59526684,460 ±   888350,929  ops/s
RandomNumberGeneratorsJMH.double_unbounded             commons.Well44497b           10  thrpt   15   35024216,344 ±   728957,927  ops/s
RandomNumberGeneratorsJMH.double_unbounded         commons.MerseneTwister           10  thrpt   15   77490123,772 ±  2431870,691  ops/s
RandomNumberGeneratorsJMH.double_unbounded    dsi.XoRoShiRo128PlusRandom*           10  thrpt   15  201642719,842 ±  3242054,069  ops/s
RandomNumberGeneratorsJMH.double_unbounded  dsi.XorShift1024StarPhiRandom           10  thrpt   15  159733854,496 ±  3148857,493  ops/s
RandomNumberGeneratorsJMH.int_bounded                          JDK.Random           10  thrpt   15   71972563,335 ±  2151758,764  ops/s
RandomNumberGeneratorsJMH.int_bounded               JDK.ThreadLocalRandom           10  thrpt   15  115587553,587 ±  6126867,436  ops/s
RandomNumberGeneratorsJMH.int_bounded                 JDK.SplitableRandom           10  thrpt   15  111568685,548 ±  2029976,932  ops/s
RandomNumberGeneratorsJMH.int_bounded               JDK.SecureRandom_SHA1           10  thrpt   15    8863875,778 ±   210576,759  ops/s
RandomNumberGeneratorsJMH.int_bounded             JDK.SecureRandom_STRONG           10  thrpt   15      13609,023 ±      267,450  ops/s
RandomNumberGeneratorsJMH.int_bounded                    commons.Well512a           10  thrpt   15   66699357,308 ±  1694638,692  ops/s
RandomNumberGeneratorsJMH.int_bounded                  commons.Well44497b           10  thrpt   15   55943941,002 ±  2548130,585  ops/s
RandomNumberGeneratorsJMH.int_bounded              commons.MerseneTwister           10  thrpt   15   77615564,482 ±  6215931,883  ops/s
RandomNumberGeneratorsJMH.int_bounded         dsi.XoRoShiRo128PlusRandom*           10  thrpt   15   56639801,218 ±   704995,679  ops/s
RandomNumberGeneratorsJMH.int_bounded       dsi.XorShift1024StarPhiRandom           10  thrpt   15   51764245,388 ±   637474,089  ops/s
RandomNumberGeneratorsJMH.int_unbounded                        JDK.Random           10  thrpt   15   83968432,107 ±   852532,062  ops/s
RandomNumberGeneratorsJMH.int_unbounded             JDK.ThreadLocalRandom           10  thrpt   15  245179386,903 ±  3045760,552  ops/s
RandomNumberGeneratorsJMH.int_unbounded               JDK.SplitableRandom           10  thrpt   15  251920298,465 ±  2706561,525  ops/s
RandomNumberGeneratorsJMH.int_unbounded             JDK.SecureRandom_SHA1           10  thrpt   15    9117086,336 ±   248403,660  ops/s
RandomNumberGeneratorsJMH.int_unbounded           JDK.SecureRandom_STRONG           10  thrpt   15      13598,291 ±      589,703  ops/s
RandomNumberGeneratorsJMH.int_unbounded                  commons.Well512a           10  thrpt   15  107752658,276 ±  1246113,538  ops/s
RandomNumberGeneratorsJMH.int_unbounded                commons.Well44497b           10  thrpt   15   81154884,463 ±  1101715,892  ops/s
RandomNumberGeneratorsJMH.int_unbounded            commons.MerseneTwister           10  thrpt   15  134250752,699 ±  1704839,715  ops/s
RandomNumberGeneratorsJMH.int_unbounded       dsi.XoRoShiRo128PlusRandom*           10  thrpt   15  240527720,327 ±  2023892,899  ops/s
RandomNumberGeneratorsJMH.int_unbounded     dsi.XorShift1024StarPhiRandom           10  thrpt   15  195200860,821 ± 15416344,814  ops/s
RandomNumberGeneratorsJMH.long_unbounded                       JDK.Random           10  thrpt   15   41933529,122 ±   529920,067  ops/s
RandomNumberGeneratorsJMH.long_unbounded            JDK.ThreadLocalRandom           10  thrpt   15  240212026,832 ± 11457298,216  ops/s
RandomNumberGeneratorsJMH.long_unbounded              JDK.SplitableRandom           10  thrpt   15  240194559,658 ±  2757983,315  ops/s
RandomNumberGeneratorsJMH.long_unbounded            JDK.SecureRandom_SHA1           10  thrpt   15    4644490,243 ±    72634,790  ops/s
RandomNumberGeneratorsJMH.long_unbounded          JDK.SecureRandom_STRONG           10  thrpt   15       6651,056 ±       71,984  ops/s
RandomNumberGeneratorsJMH.long_unbounded                 commons.Well512a           10  thrpt   15   57129972,399 ±  3696156,473  ops/s
RandomNumberGeneratorsJMH.long_unbounded               commons.Well44497b           10  thrpt   15   36773719,706 ±   511968,695  ops/s
RandomNumberGeneratorsJMH.long_unbounded           commons.MerseneTwister           10  thrpt   15   92389524,415 ±   895551,536  ops/s
RandomNumberGeneratorsJMH.long_unbounded      dsi.XoRoShiRo128PlusRandom*           10  thrpt   15  261998543,635 ±  3258166,001  ops/s
RandomNumberGeneratorsJMH.long_unbounded    dsi.XorShift1024StarPhiRandom           10  thrpt   15  206921179,168 ±  4039445,073  ops/s

RAW JDK10
Benchmark                                                      (implName)  (int_bound)   Mode  Cnt          Score          Error  Units
RandomNumberGeneratorsJMH.bool                                 JDK.Random           10  thrpt   15   84688418,405 ±  1259515,276  ops/s
RandomNumberGeneratorsJMH.bool                      JDK.ThreadLocalRandom           10  thrpt   15  227544150,529 ±  2476219,786  ops/s
RandomNumberGeneratorsJMH.bool                        JDK.SplitableRandom           10  thrpt   15  219565640,085 ±  1121089,511  ops/s
RandomNumberGeneratorsJMH.bool                      JDK.SecureRandom_SHA1           10  thrpt   15   32716090,160 ±   516714,051  ops/s
RandomNumberGeneratorsJMH.bool                    JDK.SecureRandom_STRONG           10  thrpt   15      12746,197 ±      963,469  ops/s
RandomNumberGeneratorsJMH.bool                           commons.Well512a           10  thrpt   15  103263482,995 ±  1079972,765  ops/s
RandomNumberGeneratorsJMH.bool                         commons.Well44497b           10  thrpt   15   83922485,683 ±  1064346,966  ops/s
RandomNumberGeneratorsJMH.bool                     commons.MerseneTwister           10  thrpt   15  130539262,518 ±  2131901,376  ops/s
RandomNumberGeneratorsJMH.bool                dsi.XoRoShiRo128PlusRandom*           10  thrpt   15  228149479,925 ±  3028435,682  ops/s
RandomNumberGeneratorsJMH.bool              dsi.XorShift1024StarPhiRandom           10  thrpt   15  192542220,967 ±  1366178,306  ops/s
RandomNumberGeneratorsJMH.double_bounded                       JDK.Random           10  thrpt   15   42648737,256 ±   202742,077  ops/s
RandomNumberGeneratorsJMH.double_bounded            JDK.ThreadLocalRandom           10  thrpt   15  194500915,861 ±  3469885,315  ops/s
RandomNumberGeneratorsJMH.double_bounded              JDK.SplitableRandom           10  thrpt   15  166783275,470 ±  1125519,430  ops/s
RandomNumberGeneratorsJMH.double_bounded            JDK.SecureRandom_SHA1           10  thrpt   15    4432019,661 ±   845434,217  ops/s
RandomNumberGeneratorsJMH.double_bounded          JDK.SecureRandom_STRONG           10  thrpt   15       4766,083 ±      278,191  ops/s
RandomNumberGeneratorsJMH.double_bounded                 commons.Well512a           10  thrpt   15   51662151,253 ±  1346180,132  ops/s
RandomNumberGeneratorsJMH.double_bounded               commons.Well44497b           10  thrpt   15   30177240,248 ±  1062890,123  ops/s
RandomNumberGeneratorsJMH.double_bounded           commons.MerseneTwister           10  thrpt   15   63523568,452 ±  2086853,508  ops/s
RandomNumberGeneratorsJMH.double_bounded      dsi.XoRoShiRo128PlusRandom*           10  thrpt   15  162502614,924 ±  6262641,274  ops/s
RandomNumberGeneratorsJMH.double_bounded    dsi.XorShift1024StarPhiRandom           10  thrpt   15  119665988,859 ± 10407465,486  ops/s
RandomNumberGeneratorsJMH.double_unbounded                     JDK.Random           10  thrpt   15   37877992,305 ±   357423,840  ops/s
RandomNumberGeneratorsJMH.double_unbounded          JDK.ThreadLocalRandom           10  thrpt   15  160835112,640 ± 14856506,791  ops/s
RandomNumberGeneratorsJMH.double_unbounded            JDK.SplitableRandom           10  thrpt   15  142240707,507 ±  5389029,455  ops/s
RandomNumberGeneratorsJMH.double_unbounded          JDK.SecureRandom_SHA1           10  thrpt   15    4649019,784 ±   134935,873  ops/s
RandomNumberGeneratorsJMH.double_unbounded        JDK.SecureRandom_STRONG           10  thrpt   15       5588,318 ±      237,901  ops/s
RandomNumberGeneratorsJMH.double_unbounded               commons.Well512a           10  thrpt   15   52029242,883 ±  1488419,031  ops/s
RandomNumberGeneratorsJMH.double_unbounded             commons.Well44497b           10  thrpt   15   31733013,348 ±  3835925,140  ops/s
RandomNumberGeneratorsJMH.double_unbounded         commons.MerseneTwister           10  thrpt   15   64603732,148 ±  1890482,187  ops/s
RandomNumberGeneratorsJMH.double_unbounded    dsi.XoRoShiRo128PlusRandom*           10  thrpt   15  168092234,284 ±  5234738,240  ops/s
RandomNumberGeneratorsJMH.double_unbounded  dsi.XorShift1024StarPhiRandom           10  thrpt   15  130277979,344 ±  8719801,074  ops/s
RandomNumberGeneratorsJMH.int_bounded                          JDK.Random           10  thrpt   15   69664586,104 ±  1291495,501  ops/s
RandomNumberGeneratorsJMH.int_bounded               JDK.ThreadLocalRandom           10  thrpt   15   87871807,516 ± 33148639,137  ops/s
RandomNumberGeneratorsJMH.int_bounded                 JDK.SplitableRandom           10  thrpt   15  102144696,824 ±  2530162,006  ops/s
RandomNumberGeneratorsJMH.int_bounded               JDK.SecureRandom_SHA1           10  thrpt   15    9601989,219 ±   222378,458  ops/s
RandomNumberGeneratorsJMH.int_bounded             JDK.SecureRandom_STRONG           10  thrpt   15      12004,951 ±      634,369  ops/s
RandomNumberGeneratorsJMH.int_bounded                    commons.Well512a           10  thrpt   15   67210906,459 ±  1625147,443  ops/s
RandomNumberGeneratorsJMH.int_bounded                  commons.Well44497b           10  thrpt   15   53928038,689 ±  3986968,234  ops/s
RandomNumberGeneratorsJMH.int_bounded              commons.MerseneTwister           10  thrpt   15   65960251,367 ±  2646366,719  ops/s
RandomNumberGeneratorsJMH.int_bounded         dsi.XoRoShiRo128PlusRandom*           10  thrpt   15   55604014,493 ±   787242,771  ops/s
RandomNumberGeneratorsJMH.int_bounded       dsi.XorShift1024StarPhiRandom           10  thrpt   15   49274932,783 ±  1986648,298  ops/s
RandomNumberGeneratorsJMH.int_unbounded                        JDK.Random           10  thrpt   15   75352850,156 ±   855876,300  ops/s
RandomNumberGeneratorsJMH.int_unbounded             JDK.ThreadLocalRandom           10  thrpt   15  180509086,747 ±  5895813,516  ops/s
RandomNumberGeneratorsJMH.int_unbounded               JDK.SplitableRandom           10  thrpt   15  185627929,902 ±  6468386,010  ops/s
RandomNumberGeneratorsJMH.int_unbounded             JDK.SecureRandom_SHA1           10  thrpt   15    9065636,899 ±   253827,760  ops/s
RandomNumberGeneratorsJMH.int_unbounded           JDK.SecureRandom_STRONG           10  thrpt   15      10604,615 ±      406,969  ops/s
RandomNumberGeneratorsJMH.int_unbounded                  commons.Well512a           10  thrpt   15   86372271,895 ±  3120914,507  ops/s
RandomNumberGeneratorsJMH.int_unbounded                commons.Well44497b           10  thrpt   15   70456860,637 ±  3459014,876  ops/s
RandomNumberGeneratorsJMH.int_unbounded            commons.MerseneTwister           10  thrpt   15  129267385,921 ±  7169834,133  ops/s
RandomNumberGeneratorsJMH.int_unbounded       dsi.XoRoShiRo128PlusRandom*           10  thrpt   15  227019845,665 ± 16848263,330  ops/s
RandomNumberGeneratorsJMH.int_unbounded     dsi.XorShift1024StarPhiRandom           10  thrpt   15  183984704,672 ± 13563924,949  ops/s
RandomNumberGeneratorsJMH.long_unbounded                       JDK.Random           10  thrpt   15   42498463,041 ±   619625,625  ops/s
RandomNumberGeneratorsJMH.long_unbounded            JDK.ThreadLocalRandom           10  thrpt   15  227958945,876 ±  1948362,032  ops/s
RandomNumberGeneratorsJMH.long_unbounded              JDK.SplitableRandom           10  thrpt   15  231352310,088 ±  2779635,121  ops/s
RandomNumberGeneratorsJMH.long_unbounded            JDK.SecureRandom_SHA1           10  thrpt   15    5512550,821 ±    86196,964  ops/s
RandomNumberGeneratorsJMH.long_unbounded          JDK.SecureRandom_STRONG           10  thrpt   15       6627,395 ±       56,817  ops/s
RandomNumberGeneratorsJMH.long_unbounded                 commons.Well512a           10  thrpt   15   64045733,540 ±   933420,070  ops/s
RandomNumberGeneratorsJMH.long_unbounded               commons.Well44497b           10  thrpt   15   36337796,719 ±   540372,961  ops/s
RandomNumberGeneratorsJMH.long_unbounded           commons.MerseneTwister           10  thrpt   15   89266765,322 ±   971573,029  ops/s
RandomNumberGeneratorsJMH.long_unbounded      dsi.XoRoShiRo128PlusRandom*           10  thrpt   15  253364840,628 ±  4085596,678  ops/s
RandomNumberGeneratorsJMH.long_unbounded    dsi.XorShift1024StarPhiRandom           10  thrpt   15  202520136,135 ±  3526411,477  ops/s
*/
/**
 * @author Michael Frank
 * @version 1.0 21.11.2017
 */
@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(3)
@State(Scope.Thread)
public class RandomNumberGeneratorsJMH {

    @Param({
            "JDK.Random",
            "JDK.ThreadLocalRandom",
            "JDK.SplitableRandom",
            "JDK.SecureRandom_SHA1",
            "JDK.SecureRandom_STRONG",
            "commons.Well512a",
            "commons.Well44497b",
            "commons.MerseneTwister",
//            "dsi.SplitMix64Random*",
            "dsi.XoRoShiRo128PlusRandom*",
//            "dsi.XorShift1024StarRandom*",
//            "dsi.XorShift64StarRandom*",
            "dsi.XorShift1024StarPhiRandom"
    })
    private String implName;


    @Param({"10"})
    private int int_bound;

    private long long_bound;
    private double double_bound;
    private Random r;

    @Setup
    public void setup() throws NoSuchAlgorithmException {
        long_bound = int_bound;
        double_bound = int_bound;

        switch (implName) {
            case "JDK.Random":
                r = new Random();
                break;
            case "JDK.SecureRandom_SHA1":
                r = SecureRandom.getInstance("SHA1PRNG");
                break;
            case "JDK.SecureRandom_STRONG":
                r = SecureRandom.getInstanceStrong();
                break;
            case "JDK.ThreadLocalRandom":
                r = ThreadLocalRandom.current();
                break;
            case "JDK.SplitableRandom":
                r = new JDKSplitableRandomWrapper(new SplittableRandom());
                break;
            case "commons.MerseneTwister":
                r=new JDKRandomWrapper(new MersenneTwister());
                break;
            case "commons.Well512a":
                r=new JDKRandomWrapper(new  Well512a());
                break;
            case "commons.Well44497b":
                r=new JDKRandomWrapper(new Well44497b());
                break;
            case "dsi.SplitMix64Random*":
                r = new it.unimi.dsi.util.SplitMix64Random();
                break;
            case "dsi.XoRoShiRo128PlusRandom*":
                r = new it.unimi.dsi.util.XoRoShiRo128PlusRandom();
                break;
            case "dsi.XorShift1024StarRandom*":
                r = new it.unimi.dsi.util.XorShift1024StarRandom();
                break;
            case "dsi.XorShift64StarRandom*":
                r = new it.unimi.dsi.util.XorShift64StarRandom();
                break;
            case "dsi.XorShift1024StarPhiRandom":
                r = new it.unimi.dsi.util.XorShift1024StarPhiRandom();
                break;
            default:
                throw new IllegalArgumentException(implName);
        }

    }


    @Benchmark
    public void int_unbounded(Blackhole bh) {
        bh.consume(r.nextInt());
    }

    @Benchmark
    public void int_bounded(Blackhole bh) {
        bh.consume(r.nextInt(int_bound));
    }

    @Benchmark
    public void long_unbounded(Blackhole bh) {
        bh.consume(r.nextLong());
    }


    @Benchmark
    public void double_unbounded(Blackhole bh) {
        bh.consume(r.nextDouble());
    }

    @Benchmark
    public void double_bounded(Blackhole bh) {
        //not quite right but ok for this benchmark
        double result = r.nextDouble() * double_bound;

        //correct bounded double:
//		result= (result < double_bound) ?  result : // correct for rounding
//				Double.longBitsToDouble(Double.doubleToLongBits(double_bound) - 1);

        bh.consume(result);
    }

    @Benchmark
    public void bool(Blackhole bh) {
        bh.consume(r.nextBoolean());
    }


    /**
     * Splitable random does not extend java.util.Random -> make a wrapper
     */
    public static class JDKSplitableRandomWrapper extends Random {

        private final SplittableRandom r;

        public JDKSplitableRandomWrapper(SplittableRandom splittableRandom) {
            this.r = splittableRandom;
        }

        public long nextLong(long n) { // Byte code:
            return r.nextLong(n);
        }

        @Override
        public long nextLong() {
            return r.nextLong();
        }

        @Override
        public int nextInt() {
            return r.nextInt();
        }

        @Override
        public int nextInt(int n) {
            return r.nextInt(n);
        }

        @Override
        public double nextDouble() {
            return r.nextDouble();
        }

        @Override
        public float nextFloat() {
            return Float.intBitsToFloat((int) (r.nextLong() >>> 41) | 0x3F8 << 20) - 1.0f;
        }

        @Override
        public boolean nextBoolean() {
            return r.nextBoolean();
        }

        @Override
        protected int next(int bits) {
            return (int) (r.nextLong() >>> (64 - bits));
        }

    }



    public class JDKRandomWrapper extends Random implements RandomGenerator {
        private final RandomGenerator r;

        public JDKRandomWrapper(RandomGenerator r){
            this.r=r;
        }

        @Override
        public void setSeed(int var1){
         this.r.setSeed(var1);
        }

        @Override
        public void setSeed(int[] var1){
            this.r.setSeed(var1);
        }

        @Override
        public void setSeed(long var1){
            if(r==null){
                return;
            }
            this.r.setSeed(var1);
        }

        @Override
        public void nextBytes(byte[] var1){
            this.r.nextBytes(var1);
        }

        @Override
        public int nextInt(){
            return this.r.nextInt();
        }

        @Override
        public int nextInt(int var1){
            return this.r.nextInt(var1);
        }
        @Override
        public long nextLong(){
            return this.r.nextLong();
        }

        @Override
        public boolean nextBoolean(){
            return this.r.nextBoolean();
        }

        @Override
        public float nextFloat(){
            return this.r.nextFloat();
        }

        @Override
        public double nextDouble(){
            return this.r.nextDouble();
        }

        @Override
        public double nextGaussian(){
            return this.r.nextGaussian();
        }
    }

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(RandomNumberGeneratorsJMH.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }


}
