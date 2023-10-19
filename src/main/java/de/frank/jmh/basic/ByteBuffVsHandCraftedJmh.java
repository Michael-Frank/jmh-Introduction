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
 * If we only have to convert two longs to a BigInteger, we can do it by hand or with ByteBuffer.
 * Lets see which is faster.
 * <p>
 * JMH version: 1.37 VM version: JDK 21, OpenJDK 64-Bit Server VM, 21+35-2513 Windows 10  Intel Core i9-10885H
 * <p>
 * <p>
 * ByteBuffVsHandCraftedJmh.toBigIntByteBuf     avgt   20  23,814 ± 0,421  ns/op //ByteBuffer winns
 * ByteBuffVsHandCraftedJmh.toBigIntLongBuf     avgt   20  25,397 ± 1,585  ns/op //not worth it
 * ByteBuffVsHandCraftedJmh.toBigIntHandRolled  avgt   20  28,799 ± 1,082  ns/op //not worth it
 * ByteBuffVsHandCraftedJmh.toBigIntShift       avgt   20  89,541 ± 7,773  ns/op //surprisingly bad
 * ByteBuffVsHandCraftedJmh.toBigIntShift2      avgt   20  96,135 ± 2,760  ns/op //surprisingly bad
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 2)
@State(Scope.Benchmark)
public class ByteBuffVsHandCraftedJmh {

    private static final BigInteger UNSIGNED_LONG_MASK = BigInteger.ONE.shiftLeft(Long.SIZE).subtract(BigInteger.ONE);

    public static void main(String[] args) throws Throwable {

        System.out.println(new ByteBuffVsHandCraftedJmh().toBigIntHandRolled());
        System.out.println(new ByteBuffVsHandCraftedJmh().toBigIntByteBuf());
        System.out.println(new ByteBuffVsHandCraftedJmh().toBigIntLongBuf());
        System.out.println(new ByteBuffVsHandCraftedJmh().toBigIntShift());

        Options opt = new OptionsBuilder()//
                .include(ByteBuffVsHandCraftedJmh.class.getName() + ".*")//
                //.addProfiler(GCProfiler.class)//
                .build();
        new Runner(opt).run();
    }

    long high = 49;
    long low = -100;

    @Benchmark
    public BigInteger toBigIntByteBuf() {
        return toBigIntByteBuf(high, low);
    }

    @Benchmark
    public BigInteger toBigIntHandRolled() {
        return toBigIntHandRolled(high, low);
    }

    @Benchmark
    public BigInteger toBigIntShift() {
        return toBigIntShift(high, low);
    }

    @Benchmark
    public BigInteger toBigIntShift2() {
        return toBigIntShift2(high, low);
    }

    @Benchmark
    public BigInteger toBigIntLongBuf() {
        return toBigIntLongBuf(high, low);
    }

    private static BigInteger toBigIntShift(long high, long low) {
        //code below is not right
        var shifted = BigInteger.valueOf(high).shiftLeft(Long.SIZE);
        return shifted.or(BigInteger.valueOf(low).and(UNSIGNED_LONG_MASK));
    }

    private static BigInteger toBigIntShift2(long high, long low) {
        //code below is not right
        var shifted = BigInteger.valueOf(high).shiftLeft(Long.SIZE);
        return shifted.and(BigInteger.valueOf(low).and(UNSIGNED_LONG_MASK));
    }

    private static BigInteger toBigIntByteBuf(long high, long low) {
        var bb = ByteBuffer.allocate(Long.BYTES * 2);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.putLong(high);
        bb.putLong(low);
        //big int constructor expects big endian
        return new BigInteger(bb.array());
    }

    private static BigInteger toBigIntLongBuf(long high, long low) {
        var bb = ByteBuffer.allocate(Long.BYTES * 2);
        bb.order(ByteOrder.BIG_ENDIAN);
        var lb = bb.asLongBuffer();
        lb.put(high);
        lb.put(low);

        //big int constructor expects big endian
        return new BigInteger(bb.array());
    }

    private static BigInteger toBigIntHandRolled(long high, long low) {
        return new BigInteger(new byte[]{
                // Big-Endian - Most significant byte first
                (byte) (high >>> 56 & 0xFF),
                (byte) (high >>> 48 & 0xFF),
                (byte) (high >>> 40 & 0xFF),
                (byte) (high >>> 32 & 0xFF),
                (byte) (high >>> 24 & 0xFF),
                (byte) (high >>> 16 & 0xFF),
                (byte) (high >>> 8 & 0xFF),
                (byte) (high & 0xFF),
                (byte) (low >>> 56 & 0xFF),
                (byte) (low >>> 48 & 0xFF),
                (byte) (low >>> 40 & 0xFF),
                (byte) (low >>> 32 & 0xFF),
                (byte) (low >>> 24 & 0xFF),
                (byte) (low >>> 16 & 0xFF),
                (byte) (low >>> 8 & 0xFF),
                (byte) (low & 0xFF),
        });
    }
}
