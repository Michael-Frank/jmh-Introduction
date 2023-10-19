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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.TimeUnit;

/**
 * --
 * Benchmark                                                   Mode  Cnt   Score   Error  Units
 * MathMultiplyHighVsBigIntJmh.mathMultiplyHighToCustomInt128  avgt   20   5,045 ± 0,303  ns/op #IF long high/low parts are the desired result, use Math.multiplyHigh/unsignedMultiplyHigh
 * MathMultiplyHighVsBigIntJmh.unsignedMulToCustomInt128       avgt   20   4,832 ± 0,267  ns/op
 * MathMultiplyHighVsBigIntJmh.bigIntMultiplyToBigInt          avgt   20  20,457 ± 1,317  ns/op #IF BigInteger is desired result -> use BigInteger.multiply
 * MathMultiplyHighVsBigIntJmh.mathMultiplyHighToBigInt        avgt   20  25,981 ± 2,073  ns/op
 * MathMultiplyHighVsBigIntJmh.bigIntMultiplyToCustomInt128    avgt   20  43,130 ± 1,707  ns/op # worst of both worlds
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 2)
@State(Scope.Benchmark)
public class MathMultiplyHighVsBigIntJmh {


    public static void main(String[] args) throws Throwable {

        System.out.println(new MathMultiplyHighVsBigIntJmh().mathMultiplyHighToBigInt());
        System.out.println(new MathMultiplyHighVsBigIntJmh().bigIntMultiplyToBigInt());
        System.out.println(new MathMultiplyHighVsBigIntJmh().mathMultiplyHighToCustomInt128());
        System.out.println(new MathMultiplyHighVsBigIntJmh().bigIntMultiplyToCustomInt128());
        System.out.println(new MathMultiplyHighVsBigIntJmh().unsignedMulToCustomInt128());

        Options opt = new OptionsBuilder()//
                .include(MathMultiplyHighVsBigIntJmh.class.getName() + ".*")//
                //.addProfiler(GCProfiler.class)//
                .build();
        new Runner(opt).run();
    }


    // two long values
    //long a = 45267356745l, b = 45676556735l;
    long a = Long.MAX_VALUE, b = 100;


    @Benchmark
    public BigInteger mathMultiplyHighToBigInt() {
        long high = Math.multiplyHigh(a, b);
        long low = a * b;
        return toBigInt(high, low);
    }


    @Benchmark
    public Int128 mathMultiplyHighToCustomInt128() {
        long high = Math.multiplyHigh(a, b);
        long low = a * b; //Low bits?
        return new Int128(high, low);
    }

    @Benchmark
    public Int128 unsignedMulToCustomInt128() {
        return unsignedMul64to128(a, b);
    }

    @Benchmark
    public BigInteger bigIntMultiplyToBigInt() {
        return BigInteger.valueOf(a).multiply(BigInteger.valueOf(b));
    }

    @Benchmark
    public Int128 bigIntMultiplyToCustomInt128() {
        var r = BigInteger.valueOf(a).multiply(BigInteger.valueOf(b));
        return new Int128(r.shiftRight(Long.SIZE).longValue(), r.longValue());
    }


    public static Int128 unsignedMul64to128(long a, long b) {
        final long low = a * b;
        long high = Math.unsignedMultiplyHigh(a, b);
        return new Int128(high, low);
    }

    record Int128(long high, long low) {
        public String toString() {
            return "Int128[high=" + high + ", low=" + low + " string=" + toBigInteger() + "]";
        }

        public BigInteger toBigInteger() {
            var bb = ByteBuffer.allocate(Long.BYTES * 2);
            bb.order(ByteOrder.BIG_ENDIAN);
            bb.putLong(high);
            bb.putLong(low);
            //big int constructor expects big endian
            return new BigInteger(bb.array());
        }
    }

    private static BigInteger toBigInt(long high, long low) {
        var bb = ByteBuffer.allocate(Long.BYTES * 2);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.putLong(high);
        bb.putLong(low);
        //big int constructor expects big endian
        return new BigInteger(bb.array());
    }


}
