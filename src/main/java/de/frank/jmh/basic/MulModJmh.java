package de.frank.jmh.basic;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

/**
 * Since i first implemented this mulMod stuff almost 11 years ago, BigInteger got a lot better.
 * As of today there is no much difference between mulModSplit and BigInteger based implementation. (11 years ago there was a 3x gap)
 * The "Double" variant and of course the baseline  are a lot faster than BigInt
 * Best approach might be to write a new "multiAlgoMulMod" consisting of
 * - baseline (if a*b does not overflow)
 * - doublevariant (if m<=2^62 and a*b/m < 2^53)
 * - bigInt fallback
 * <p>
 * <p>
 * Bummer: as of today, we still have no direct mulMod or mulModAdd support in java.lang.Math or a 128Bit datatype.
 * LOWER IS BETTER
 * Benchmark                            Mode  Cnt    Score    Error  Units
 * MulModJmh.baseline_OverflowsAndWrong avgt   20    8,303 ±  0,544  ns/op
 * MulModJmh.bigInt                     avgt   20   47,244 ±  3,583  ns/op # since last time i did this test bigInt has come a long way and is much faster. mulModMultiAlg was almost 3x faster 6-8 or so years ago .
 * MulModJmh.bigIntPartialStatic        avgt   20   50,200 ±  1,749  ns/op # strange
 * MulModJmh.mulModMultiAlg             avgt   20   30,857 ±  1,157  ns/op # winner, if computation might overflow and the whole Long.MAX_VALUE range is required for all parameters.
 * MulModJmh.doubleMulMod               avgt   20   27,864 ±  0,993  ns/op # nice but requires fallback to split if supported range is exceeded:  m<=2^62 and a*b/m < 2^53
 * MulModJmh.mulModSplit                avgt   20   45,141 ±  1,336  ns/op # double is faster, but does not support full 63 bit range
 * MulModJmh.mulModWhile                avgt   20  504,374 ± 14,797  ns/op # abysmal bad
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 2)
@State(Scope.Benchmark)
public class MulModJmh {


    public static void main(String[] args) throws Throwable {

        System.out.println(new MulModJmh().baselineOverflowsAndWrong());
        System.out.println(new MulModJmh().bigInt());
        System.out.println(new MulModJmh().bigIntPartialStatic());
        System.out.println(new MulModJmh().mulmodWhile());
        System.out.println(new MulModJmh().doubleMulMod());
        System.out.println(new MulModJmh().mulModSplit());
        System.out.println(new MulModJmh().mulModMultiAlg());

        Options opt = new OptionsBuilder()//
                .include(MulModJmh.class.getName() + ".*")//
                //.addProfiler(GCProfiler.class)//
                .build();
        new Runner(opt).run();
    }

    long high = Integer.MAX_VALUE;
    BigInteger highB = BigInteger.valueOf(Integer.MAX_VALUE);
    long low = Integer.MAX_VALUE - 1L;
    long mod = 41;
    BigInteger modB = BigInteger.valueOf(mod);

    @Benchmark
    public long baselineOverflowsAndWrong() {
        return high * low % mod;
    }

    @Benchmark
    public long bigInt() {
        return BigInteger.valueOf(high).multiply(BigInteger.valueOf(low)).mod(BigInteger.valueOf(mod)).longValue();
    }

    @Benchmark
    public long bigIntPartialStatic() {
        return highB.multiply(BigInteger.valueOf(low)).mod(modB).longValue();
    }

    @Benchmark
    public long mulmodWhile() {
        return mulmodWhile1(low, high, mod);
    }


    @Benchmark
    public long mulModSplit() {
        return mulModSplit(high, low, mod);
    }

    @Benchmark
    public long mulModMultiAlg() {
        return mulModMultiAlg(high, low, mod);
    }

    @Benchmark
    public long doubleMulMod() {
        return doubleMulMod(high, low, mod);
    }


    static long mulmodWhile1(long a, long b, long mod) { // returns (a * b) % c, and minimize overflow
        long res = 0;
        a = a % mod;
        while (b > 0) {
            // If b is odd, add 'a' to result
            if (b % 2 == 1) {
                res = (res + a) % mod;
            }
            // Multiply 'a' with 2
            a = (a * 2) % mod;
            // Divide b by 2
            b /= 2;
        }
        return res % mod;
    }


    public long mulModMultiAlg(long a, long b, long m) {
        if (a > m)
            a = a % m;
        if (b > m)
            b = b % m;

        if (b != 0 && a < Long.MAX_VALUE / b) {
            // a*b will not overflow Long.MAX_VALUE
            return (a * b) % m;
        } else {
            return _unsafeDoubleMulMod(a, b, m);
        }
    }

    /**
     * 64bit calculation of (a * b) mod m with multiplication overflow
     * consciousness.
     * <br>
     * Naive version "return (a * b) % m is ~3.2 times faster then this
     * algorithm
     * but will fail if a*b > Long.MAX_VALUE
     *
     * @param a
     * @param b
     * @param m
     * @return (a * b) mod m
     */
    public static long mulModSplit(long a, long b, long m) {
        // algorithm fails if a >= m, we restrict a
        if (a > m)
            a = a % m;
        if (b > m)
            b = b % m;

        return _unsafeSplitMulMod(a, b, m);
    }

    private static final long DOUBLE_MULMOD_THREASHOLD = 1L << 52; // theoretical
    // <<53

    /**
     * 64bit calculation of (a * b) mod m with multiplication overflow
     * consciousness.
     * <br>
     * Naive version "return (a * b) % m ~1.6 times faster then this algorithm
     * but will fail if a*b > Long.MAX_VALUE
     *
     * @param a
     * @param b
     * @param m
     * @return
     */
    public long doubleMulMod(long a, long b, long m) {
        return _unsafeDoubleMulMod(a, b, m);
    }

    /**
     * 64bit calculation of (a * b) mod m with multiplication overflow
     * consciousness.
     * <br>
     * Naive version "return (a * b) % m ~1.6 times faster then this algorithm
     * but will fail if a*b > Long.MAX_VALUE
     *
     * @param a < m
     * @param b < m
     * @param m
     * @return (a * b) mod m
     */
    private long _unsafeDoubleMulMod(long a, long b, long m) {
        // check if we can reduce size of parameters because a*b mod m ==(a mod
        // m) * (b mod m) mod m
        // splitMulMod will fail if we dont reduce!
        if (a > m) {
            a = a % m;
        }
        if (b > m) {
            b = b % m;
        }

        // verify conditions
        if (m > 1L << 62) {// >2^62
            return _unsafeSplitMulMod(a, b, m);
        }
        double inv_m = 1.0d / m;
        long q = (long) (inv_m * a * b);
        if (q >= DOUBLE_MULMOD_THREASHOLD) {
            return _unsafeSplitMulMod(a, b, m);
        }
        // following code only correct if m<=2^62 and a*b/m < 2^53 (double
        // mantissa)
        long r = a * b - q * m;
        return r < 0 ? r + m : r % m;
    }


    /**
     * 64bit calculation of (a * b) mod m with multiplication overflow
     * consciousness.
     * <br>
     * Naive version "return (a * b) % m is ~3.2 times faster then this
     * algorithm
     * but will fail if a*b > Long.MAX_VALUE
     *
     * @param a < m
     * @param b < m
     * @param m
     * @return (a * b) mod m
     */
    private static long _unsafeSplitMulMod(long a, long b, long m) {
        long l4;
        long l14;
        if (a < 0x80000000L) {
            l4 = a;
            l14 = 0L;
        } else {
            long l5 = a / 0x80000000L;
            l4 = a - 0x80000000L * l5;
            long l8 = m / 0x80000000L;
            long l9 = m - 0x80000000L * l8;
            if (l5 >= 0x80000000L) {
                l5 -= 0x80000000L;
                long l10 = b / l8;
                l14 = 0x80000000L * (b - l10 * l8) - l10 * l9;
                if (l14 < 0L)
                    l14 = ((l14 + 1L) % m + m) - 1L;
            } else {
                l14 = 0L;
            }
            if (l5 != 0L) {
                long l6 = m / l5;
                long l11 = b / l6;
                l14 -= l11 * (m - l5 * l6);
                if (l14 > 0L)
                    l14 -= m;
                l14 += l5 * (b - l11 * l6);
                if (l14 < 0L)
                    l14 = ((l14 + 1L) % m + m) - 1L;
            }
            long l12 = l14 / l8;
            l14 = 0x80000000L * (l14 - l12 * l8) - l12 * l9;
            if (l14 < 0L)
                l14 = ((l14 + 1L) % m + m) - 1L;
        }
        if (l4 != 0L) {
            long l7 = m / l4;
            long l13 = b / l7;
            l14 -= l13 * (m - l4 * l7);
            if (l14 > 0L)
                l14 -= m;
            l14 += l4 * (b - l13 * l7);
            if (l14 < 0L)
                l14 = ((l14 + 1L) % m + m) - 1L;
        }
        return l14;
    }


}
