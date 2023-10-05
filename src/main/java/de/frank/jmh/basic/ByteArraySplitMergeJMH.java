package de.frank.jmh.basic;

import org.apache.commons.lang3.ArrayUtils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/*--
Split and merge a byte array. Often required in assembling/parsing binary messages from a socket or splitting a crypto
envelope into is parts (e.g. get/join SEED, HMAC, length and payload of an encrypted message)

Arrays.copyOfRange() appears to be the winner.

 * Benchmark                    Mode  Cnt             Score  gc.alloc.rate.norm
 * join_arraysCopyOf           thrpt   30  58.165.499 ops/s   48 B/op # winner
 * join_systemArrayCopy        thrpt   30  50.576.451 ops/s   48 B/op
 * join_commonsArrayUtils      thrpt   30  30.516.040 ops/s   80 B/op
 * join_ByteArrayOutputStream  thrpt   30  18.565.976 ops/s  208 B/op # sucks

 * split_arraysCopyOf          thrpt   30  49.391.116 ops/s   64 B/op #winner
 * split_systemArrayCopy       thrpt   30  44.296.562 ops/s   64 B/op
 * split_byteBuffer            thrpt   30  42.570.929 ops/s   64 B/op
 * split_byteArrayInputStream  thrpt   30  38.444.425 ops/s   64 B/op
 *
 */

/**
 * @author Michael Frank
 * @version 1.0 13.05.2018
 */
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(3)
@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark) // Important to be Scope.Benchmark
@Threads(1)
public class ByteArraySplitMergeJMH {

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()//
                .include(ByteArraySplitMergeJMH.class.getName() + ".*")//
                .result(String.format("%s_%s.json",
                        DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                        ByteArraySplitMergeJMH.class.getSimpleName()))
                //.addProfiler(GCProfiler.class)//
                .build();
        new Runner(opt).run();
    }

    @Benchmark
    public void split_byteBuffer(Blackhole bh, MyState state) {
        byte[] l = new byte[16];
        byte[] r = new byte[16];
        ByteBuffer bb = ByteBuffer.wrap(state.data32);
        bb.get(l);
        bb.get(r);
        bh.consume(l);
        bh.consume(r);
    }

    @Benchmark
    public void split_arraysCopyOf(Blackhole bh, MyState state) {
        byte[] l = Arrays.copyOfRange(state.data32, 0, 16);
        byte[] r = Arrays.copyOfRange(state.data32, 16, 32);

        bh.consume(l);
        bh.consume(r);
    }

    @Benchmark
    public void split_systemArrayCopy(Blackhole bh, MyState state) {
        byte[] l = new byte[16];
        byte[] r = new byte[16];
        System.arraycopy(state.data32, 0, l, 0, 16);
        System.arraycopy(state.data32, 16, r, 0, 16);

        bh.consume(l);
        bh.consume(r);
    }

    @Benchmark
    public void split_systemArrayCopy_dynSized(Blackhole bh, MyState state) {
        byte[] l = new byte[16];
        byte[] r = new byte[16];
        System.arraycopy(state.data32, 0, l, 0, l.length);
        System.arraycopy(state.data32, l.length, r, 0, r.length);

        bh.consume(l);
        bh.consume(r);
    }

    @Benchmark
    public void split_byteArrayInputStream(Blackhole bh, MyState state) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(state.data32);
        byte[] l = new byte[16];
        byte[] r = new byte[16];
        in.read(l);
        in.read(r);
        bh.consume(l);
        bh.consume(r);
    }

    @Benchmark
    public void split_commonsArrayUtils(Blackhole bh, MyState state) {
        byte[] l = ArrayUtils.subarray(state.data32, 0, 16);
        byte[] r = ArrayUtils.subarray(state.data32, 16, 32);

        bh.consume(l);
        bh.consume(r);
    }


    @Benchmark
    public void join_byteBuffer(Blackhole bh, MyState state) {
        ByteBuffer bb = ByteBuffer.allocate(32);
        bb.put(state.data16_a);
        bb.put(state.data16_b);
        bh.consume(bb.array());
    }


    @Benchmark
    public void join_byteBuffer_dynSized(Blackhole bh, MyState state) {
        ByteBuffer bb = ByteBuffer.allocate(state.data16_a.length + state.data16_b.length);
        bb.put(state.data16_a);
        bb.put(state.data16_b);
        bh.consume(bb.array());
    }

    @Benchmark
    public void join_arraysCopyOf(Blackhole bh, MyState state) {
        byte[] res = Arrays.copyOfRange(state.data16_a, 0, 32);
        System.arraycopy(state.data16_b, 0, res, 16, 16);
        bh.consume(res);
    }

    @Benchmark
    public void join_systemArrayCopy(Blackhole bh, MyState state) {
        byte[] res = new byte[32];
        System.arraycopy(state.data16_a, 0, res, 0, 16);
        System.arraycopy(state.data16_b, 0, res, 16, 16);
        bh.consume(res);
    }


    @Benchmark
    public void join_systemArrayCopy_dynSized(Blackhole bh, MyState state) {
        byte[] res = new byte[state.data16_a.length + state.data16_b.length];
        System.arraycopy(state.data16_a, 0, res, 0, state.data16_a.length);
        System.arraycopy(state.data16_b, 0, res, state.data16_a.length, state.data16_b.length);
        bh.consume(res);
    }

    @Benchmark
    public void join_byteArrayOutputStream(Blackhole bh, MyState state) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(32);
        bos.write(state.data16_a);
        bos.write(state.data16_a);
        bh.consume(bos.toByteArray());
    }

    @Benchmark
    public void join_commonsArrayUtils(Blackhole bh, MyState state) {
        bh.consume(ArrayUtils.addAll(state.data16_a, state.data16_b));
    }

    @State(Scope.Thread)
    public static class MyState {


        public static final SecureRandom RANDOM = getSecureRandom();
        public byte[] data32 = newRandomByteArray(32);
        public byte[] data16_a = newRandomByteArray(16);
        public byte[] data16_b = newRandomByteArray(16);


        private static byte[] newRandomByteArray(int i) {
            byte[] data = new byte[i];
            RANDOM.nextBytes(data);
            return data;
        }
        private static SecureRandom getSecureRandom() {
            try {
                return SecureRandom.getInstance("SHA1PRNG");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }

    }
}
