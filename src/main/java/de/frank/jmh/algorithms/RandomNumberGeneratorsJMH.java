package de.frank.jmh.algorithms;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Random;
import java.util.SplittableRandom;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/*--

Disclaimer:
This benchmark settles for "drop in" replacement's for java.util.Random and its interface.
Some generators have special methods to generate bounded doubles, floats, ints, longs correct and efficiently, which
are not included in this benchmark.


                                                 double       double         int          int       long
                                      bool      bounded    unbounded     bounded    unbounded   unbounded        Average   Comment
dsi.SplitMix64Random*          164.888.581  128.711.540  139.440.578  47.260.915  191.480.592  177.678.470    141.576.779  # essentially same as JDKSplitableRandom again. But nextInt(bound) calls nextLong(bound) instead of using custom code for bounded integers
dsi.XoRoShiRo128PlusRandom*    176.258.478  134.030.350  158.181.138  47.812.503  197.133.471  187.870.341    150.214.380  # current state of the art in super fast medium period PRNG's
dsi.XorShift1024StarPhiRandom  134.219.586  103.387.804  116.970.967  43.576.749  128.155.110  135.300.280    110.268.416  # current state of the art in super fast high period PRNG's
dsi.XorShift1024StarRandom*    131.331.083  104.839.167  118.124.601  44.632.823  147.003.966  132.848.090    113.129.955  # *DEPRECATED* essentially xorshift64* wich a much larger period.
dsi.XorShift64StarRandom*      173.209.529  131.579.301  160.454.976  56.002.236  189.603.417  195.882.252    151.121.952  # *DEPRECATED* super simplistic and fast, but splitMix64 and XoRoShiRo128Plus are superior. based on George Marsaglia's Xorshift generators.
JDK.Random                      78.244.683   39.934.486   39.382.213  72.609.536   78.327.461   36.250.151     57.458.088  # DONT EVER USE! - this is just plain bad.. Use ThreadLocalRandom as dropin replacement.
JDK.SplitableRandom            155.059.804  117.591.638  126.638.107  80.447.532  176.014.167  157.978.204    135.621.575  # splitMix64 - but lacks the "Random" interface and a few shortcomings
JDK.ThreadLocalRandom          182.488.055  144.093.446  146.105.065  99.245.675  199.520.206  185.023.505    159.412.659  # splitMix64 - VERY good - Prefer this one if you don't have special requirements.

Remarks: Interestingly all generators have performance issues generating bounded integers

RAW:

# Run complete. Total time: 00:50:16

Benchmark                                                      (implName)  (int_bound)   Mode  Cnt          Score          Error  Units
RandomNumberGeneratorsJMH.bool                                 JDK.Random           10  thrpt   30   78244683,108 ±  2158618,983  ops/s
RandomNumberGeneratorsJMH.bool                      JDK.ThreadLocalRandom           10  thrpt   30  182488055,505 ± 11715208,580  ops/s
RandomNumberGeneratorsJMH.bool                        JDK.SplitableRandom           10  thrpt   30  155059804,091 ±  8151985,342  ops/s
RandomNumberGeneratorsJMH.bool                      dsi.SplitMix64Random*           10  thrpt   30  164888581,129 ±  9220232,520  ops/s
RandomNumberGeneratorsJMH.bool                dsi.XoRoShiRo128PlusRandom*           10  thrpt   30  176258478,132 ±  8306857,287  ops/s
RandomNumberGeneratorsJMH.bool                dsi.XorShift1024StarRandom*           10  thrpt   30  131331083,558 ±  6920937,421  ops/s
RandomNumberGeneratorsJMH.bool                  dsi.XorShift64StarRandom*           10  thrpt   30  173209529,348 ±  8504247,608  ops/s
RandomNumberGeneratorsJMH.bool              dsi.XorShift1024StarPhiRandom           10  thrpt   30  134219586,305 ±  7128745,356  ops/s
RandomNumberGeneratorsJMH.double_bounded                       JDK.Random           10  thrpt   30   39934486,831 ±   846338,441  ops/s
RandomNumberGeneratorsJMH.double_bounded            JDK.ThreadLocalRandom           10  thrpt   30  144093446,491 ±  6621125,541  ops/s
RandomNumberGeneratorsJMH.double_bounded              JDK.SplitableRandom           10  thrpt   30  117591638,140 ±  5692094,165  ops/s
RandomNumberGeneratorsJMH.double_bounded            dsi.SplitMix64Random*           10  thrpt   30  128711540,138 ±  6971430,363  ops/s
RandomNumberGeneratorsJMH.double_bounded      dsi.XoRoShiRo128PlusRandom*           10  thrpt   30  134030350,150 ±  6622808,888  ops/s
RandomNumberGeneratorsJMH.double_bounded      dsi.XorShift1024StarRandom*           10  thrpt   30  104839167,034 ±  3312865,173  ops/s
RandomNumberGeneratorsJMH.double_bounded        dsi.XorShift64StarRandom*           10  thrpt   30  131579301,030 ±  6589608,981  ops/s
RandomNumberGeneratorsJMH.double_bounded    dsi.XorShift1024StarPhiRandom           10  thrpt   30  103387804,275 ±  3663758,037  ops/s
RandomNumberGeneratorsJMH.double_unbounded                     JDK.Random           10  thrpt   30   39382213,121 ±   864120,886  ops/s
RandomNumberGeneratorsJMH.double_unbounded          JDK.ThreadLocalRandom           10  thrpt   30  146105065,561 ±  6961960,880  ops/s
RandomNumberGeneratorsJMH.double_unbounded            JDK.SplitableRandom           10  thrpt   30  126638107,563 ±  5559039,579  ops/s
RandomNumberGeneratorsJMH.double_unbounded          dsi.SplitMix64Random*           10  thrpt   30  139440578,089 ±  4519926,540  ops/s
RandomNumberGeneratorsJMH.double_unbounded    dsi.XoRoShiRo128PlusRandom*           10  thrpt   30  158181138,471 ±  4817878,978  ops/s
RandomNumberGeneratorsJMH.double_unbounded    dsi.XorShift1024StarRandom*           10  thrpt   30  118124601,729 ±  3067640,932  ops/s
RandomNumberGeneratorsJMH.double_unbounded      dsi.XorShift64StarRandom*           10  thrpt   30  160454976,486 ±  4397409,318  ops/s
RandomNumberGeneratorsJMH.double_unbounded  dsi.XorShift1024StarPhiRandom           10  thrpt   30  116970967,722 ±  3936908,904  ops/s
RandomNumberGeneratorsJMH.int_bounded                          JDK.Random           10  thrpt   30   72609536,024 ±  2274311,723  ops/s
RandomNumberGeneratorsJMH.int_bounded               JDK.ThreadLocalRandom           10  thrpt   30   99245675,103 ±  3008113,121  ops/s
RandomNumberGeneratorsJMH.int_bounded                 JDK.SplitableRandom           10  thrpt   30   80447532,687 ±  2118089,565  ops/s
RandomNumberGeneratorsJMH.int_bounded               dsi.SplitMix64Random*           10  thrpt   30   47260915,525 ±  1090401,094  ops/s
RandomNumberGeneratorsJMH.int_bounded         dsi.XoRoShiRo128PlusRandom*           10  thrpt   30   47812503,103 ±  1438190,697  ops/s
RandomNumberGeneratorsJMH.int_bounded         dsi.XorShift1024StarRandom*           10  thrpt   30   44632823,349 ±  1103301,789  ops/s
RandomNumberGeneratorsJMH.int_bounded           dsi.XorShift64StarRandom*           10  thrpt   30   56002236,665 ±  1713122,187  ops/s
RandomNumberGeneratorsJMH.int_bounded       dsi.XorShift1024StarPhiRandom           10  thrpt   30   43576749,927 ±  1215665,401  ops/s
RandomNumberGeneratorsJMH.int_unbounded                        JDK.Random           10  thrpt   30   78327461,968 ±  2592253,172  ops/s
RandomNumberGeneratorsJMH.int_unbounded             JDK.ThreadLocalRandom           10  thrpt   30  199520206,349 ±  9336314,788  ops/s
RandomNumberGeneratorsJMH.int_unbounded               JDK.SplitableRandom           10  thrpt   30  176014167,639 ±  4572440,136  ops/s
RandomNumberGeneratorsJMH.int_unbounded             dsi.SplitMix64Random*           10  thrpt   30  191480592,551 ±  6251214,130  ops/s
RandomNumberGeneratorsJMH.int_unbounded       dsi.XoRoShiRo128PlusRandom*           10  thrpt   30  197133471,222 ±  5989748,531  ops/s
RandomNumberGeneratorsJMH.int_unbounded       dsi.XorShift1024StarRandom*           10  thrpt   30  147003966,548 ±  4683523,514  ops/s
RandomNumberGeneratorsJMH.int_unbounded         dsi.XorShift64StarRandom*           10  thrpt   30  189603417,322 ±  8979207,101  ops/s
RandomNumberGeneratorsJMH.int_unbounded     dsi.XorShift1024StarPhiRandom           10  thrpt   30  128155110,989 ±  6174017,095  ops/s
RandomNumberGeneratorsJMH.long_unbounded                       JDK.Random           10  thrpt   30   36250151,779 ±  1448637,952  ops/s
RandomNumberGeneratorsJMH.long_unbounded            JDK.ThreadLocalRandom           10  thrpt   30  185023505,407 ± 10741070,960  ops/s
RandomNumberGeneratorsJMH.long_unbounded              JDK.SplitableRandom           10  thrpt   30  157978204,982 ±  7184045,227  ops/s
RandomNumberGeneratorsJMH.long_unbounded            dsi.SplitMix64Random*           10  thrpt   30  177678470,309 ±  6489445,101  ops/s
RandomNumberGeneratorsJMH.long_unbounded      dsi.XoRoShiRo128PlusRandom*           10  thrpt   30  187870341,860 ±  8856774,485  ops/s
RandomNumberGeneratorsJMH.long_unbounded      dsi.XorShift1024StarRandom*           10  thrpt   30  132848090,879 ±  8271107,425  ops/s
RandomNumberGeneratorsJMH.long_unbounded        dsi.XorShift64StarRandom*           10  thrpt   30  195882252,044 ±  7492601,865  ops/s
RandomNumberGeneratorsJMH.long_unbounded    dsi.XorShift1024StarPhiRandom           10  thrpt   30  135300280,368 ±  5671421,310  ops/s
*/

/**
 * @author Michael Frank
 * @version 1.0 21.11.2017
 */
@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(3)
@State(Scope.Thread)
public class RandomNumberGeneratorsJMH {

    @Param({
            "JDK.Random",
            "JDK.ThreadLocalRandom",
            "JDK.SplitableRandom",
            "dsi.SplitMix64Random*",
            "dsi.XoRoShiRo128PlusRandom*",
            "dsi.XorShift1024StarRandom*",
            "dsi.XorShift64StarRandom*",
            "dsi.XorShift1024StarPhiRandom"
    })
    private String implName;


    @Param({"10"})
    private int int_bound;

    private long long_bound;
    private double double_bound;
    private Random r;

    @Setup
    public void setup() {
        long_bound = int_bound;
        double_bound = int_bound;

        switch (implName) {
            case "JDK.Random":
                r = new Random();
                break;
            case "JDK.ThreadLocalRandom":
                r = ThreadLocalRandom.current();
                break;
            case "JDK.SplitableRandom":
                r = new JDKSplitableRandomWrapper(new SplittableRandom());
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

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(RandomNumberGeneratorsJMH.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }


}
