package de.frank.jmh.algorithms;

import org.apache.commons.math3.random.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.*;
import org.openjdk.jmh.runner.*;
import org.openjdk.jmh.runner.options.*;

import java.security.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.concurrent.*;

/*--

Disclaimer:
This benchmark settles for "drop in" replacement's for java.util.Random and its interface.
Some generators have special methods to generate bounded doubles, floats, ints and longs correct and efficiently but theses are not included in this benchmark - only measuring methods in common.

@ VM version: JDK 1.8.0_161, VM 25.161-b12
@ Single Threaded
                                                 double       double          int          int        long
                                      bool      bounded    unbounded      bounded    unbounded    unbounded       Average # Comment
JDK.Random                      84.530.322   40.726.804   41.015.961   71.972.563   83.968.432   41.933.529    60.691.269 # DONT EVER USE! - this is just plain bad. Use ThreadLocalRandom as dropin replacement.
JDK.SplitableRandom            222.596.847  171.889.636  180.398.036  111.568.686  251.920.298  240.194.560   196.428.010 # splitMix64 - but lacks the jdk.Random "interface" and has a few shortcomings
JDK.ThreadLocalRandom          228.848.173  192.477.756  202.704.545  115.587.554  245.179.387  240.212.027   204.168.240 # splitMix64 - VERY good - Prefer this one if you don't have special requirements.
JDK.SecureRandom_SHA1           24.040.454    4.631.773    4.581.726    8.863.876    9.117.086    4.644.490     9.313.234 # way to go for normal secure stuff (crypto keys, salts, ....)
JDK.SecureRandom_STRONG             12.492        6.760        6.585       13.609      13.598         6.651         9.949 # ULTRA slow - no reallife benefit over SecureRandom_SHA1 - only viable for you CA Master Private Key

commons.MersenneTwister        130.504.838   69.272.325   77.490.124   77.615.564  134.250.753   92.389.524    96.920.521 # well known but special - long period
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

commons.MersenneTwister        130.539.263   63.523.568   64.603.732   65.960.251  129.267.386   89.266.765     90.526.828
commons.Well44497b              83.922.486   30.177.240   31.733.013   53.928.039   70.456.861   36.337.797     51.092.573
commons.Well512a               103.263.483   51.662.151   52.029.243   67.210.906   86.372.272   64.045.734     70.763.965
dsi.XoRoShiRo128PlusRandom*    228.149.480  162.502.615  168.092.234   55.604.014  227.019.846  253.364.841    182.455.505
dsi.XorShift1024StarPhiRandom  192.542.221  119.665.989  130.277.979   49.274.933  183.984.705  202.520.136    146.377.660

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
            "commons.MersenneTwister",
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
            case "commons.MersenneTwister":
                r = new JDKRandomWrapper(new MersenneTwister());
                break;
            case "commons.Well512a":
                r = new JDKRandomWrapper(new Well512a());
                break;
            case "commons.Well44497b":
                r = new JDKRandomWrapper(new Well44497b());
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
        //        result= (result < double_bound) ?  result : // correct for rounding
        //        Double.longBitsToDouble(Double.doubleToLongBits(double_bound) - 1);

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

        public JDKRandomWrapper(RandomGenerator r) {
            this.r = r;
        }

        @Override
        public void setSeed(int var1) {
            this.r.setSeed(var1);
        }

        @Override
        public void setSeed(int[] var1) {
            this.r.setSeed(var1);
        }

        @Override
        public void setSeed(long var1) {
            if (r == null) {
                return;
            }
            this.r.setSeed(var1);
        }

        @Override
        public void nextBytes(byte[] var1) {
            this.r.nextBytes(var1);
        }

        @Override
        public int nextInt() {
            return this.r.nextInt();
        }

        @Override
        public int nextInt(int var1) {
            return this.r.nextInt(var1);
        }

        @Override
        public long nextLong() {
            return this.r.nextLong();
        }

        @Override
        public boolean nextBoolean() {
            return this.r.nextBoolean();
        }

        @Override
        public float nextFloat() {
            return this.r.nextFloat();
        }

        @Override
        public double nextDouble() {
            return this.r.nextDouble();
        }

        @Override
        public double nextGaussian() {
            return this.r.nextGaussian();
        }
    }

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(RandomNumberGeneratorsJMH.class.getName())
                .result(String.format("%s_%s.json",
                        DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                        RandomNumberGeneratorsJMH.class.getSimpleName()))
                .build();
        new Runner(opt).run();
    }


}
