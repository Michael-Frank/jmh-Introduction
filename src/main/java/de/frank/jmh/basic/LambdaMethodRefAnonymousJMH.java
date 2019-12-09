package de.frank.jmh.basic;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.IntConsumer;



/*--
Point is to show that the different ways to "implement an interface" are not just synthetic sugar
Each of them emits different bytecode with different performance characteristics - and they are optimized pretty darn good
Variants:
 - simple lambda: x->foo(x)
 - multi-line lambda: x->{foo(x);}
 - MethodReferences: this::foo
 - anonymous classes: new Consumer<T>(){ public void accept(T x){ foo(x); };
 - concrete implementations fooConsumer.accept(foo);


JDK 1.8.0_212, OpenJDK 64-Bit Server VM, 25.212-b04e


                   primitive  primBoxed     string
                      ns/op       ns/op      ns/op
lambda                 1,73        2,47       2,17
lambdaMultiline        1,74        2,44       2,15
methodReference        1,80        2,54       2,16  # Method reference is slightly slower
anonymous              1,85        2,51       2,15
concreteImpl           4,38        5,44       4,89  #jvm forced to crate a new instance -> gc.alloc.rate.norm: 24,000 B/op
concreteImplCached     1.96        2,75       2,34
direct


Raw results:
Benchmark                        Mode  Cnt   Score    Error   Units  gc.alloc.rate.norm
anonymous_primitive              avgt   30   1,857 ±  0,074   ns/op  ≈ 10⁻⁶ B/op
anonymous_primitiveBoxed         avgt   30   2,515 ±  0,059   ns/op  ≈ 10⁻⁶ B/op
anonymous_string                 avgt   30   2,156 ±  0,029   ns/op  ≈ 10⁻⁶ B/op
lambdaMultiline_primitive        avgt   30   1,742 ±  0,018   ns/op  ≈ 10⁻⁶ B/op
lambdaMultiline_primitiveBoxed   avgt   30   2,446 ±  0,041   ns/op  ≈ 10⁻⁶ B/op
lambdaMultiline_string           avgt   30   2,154 ±  0,024   ns/op  ≈ 10⁻⁶ B/op
lambda_primitive                 avgt   30   1,729 ±  0,022   ns/op  ≈ 10⁻⁶ B/op
lambda_primitiveBoxed            avgt   30   2,474 ±  0,086   ns/op  ≈ 10⁻⁶ B/op
lambda_string                    avgt   30   2,174 ±  0,052   ns/op  ≈ 10⁻⁶ B/op
methodReference_primitive        avgt   30   1,800 ±  0,056   ns/op  ≈ 10⁻⁶ B/op
methodReference_primitiveBoxed   avgt   30   2,536 ±  0,075   ns/op  ≈ 10⁻⁶ B/op
methodReference_string           avgt   30   2,165 ±  0,083   ns/op  ≈ 10⁻⁶ B/op
concreteImplCached_primitive     avgt   30   1,963 ±  0,018   ns/op  ≈ 10⁻⁶ B/op
concreteImplCached_primBoxed     avgt   30   2,775 ±  0,030   ns/op  ≈ 10⁻⁶ B/op
concreteImplCached_string        avgt   30   2,334 ±  0,039   ns/op  ≈ 10⁻⁶ B/op
concreteImpl_primitive           avgt   30   4,381 ±  0,073   ns/op  24,000 B/op
concreteImpl_primitiveBoxed      avgt   30   5,448 ±  0,218   ns/op  24,000 B/op
concreteImpl_string              avgt   30   4,905 ±  0,229   ns/op  24,000 B/op
direct_primitive                 avgt   30   3,437 ±  0,090   ns/op  ≈ 10⁻⁶ B/op
direct_primitiveBoxed            avgt   30   3,501 ±  0,050   ns/op  ≈ 10⁻⁶ B/op
direct_string                    avgt   30   3,222 ±  0,035   ns/op  ≈ 10⁻⁶ B/op

 */

/**
 * @author Michael Frank
 * @version 1.0 13.05.2018
 */
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(3)
@BenchmarkMode({Mode.AverageTime})
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Threads(1)
@State(Scope.Benchmark)
public class LambdaMethodRefAnonymousJMH {

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()//
                .include(LambdaMethodRefAnonymousJMH.class.getName() + ".*")//
                .result(String.format("%s_%s.json",
                        DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                        LambdaMethodRefAnonymousJMH.class.getSimpleName()))
                .addProfiler(GCProfiler.class)//
                .build();
        new Runner(opt).run();
    }


    private int primitive = 1;
    private String string = "f";


    private BlackholeConsumer consumer;

    @Setup
    public void setup(Blackhole bh) {
        this.consumer = new BlackholeConsumer(bh);
    }

    @Benchmark
    public void methodReference_primitive(Blackhole bh) {
        acceptPrimitive(bh::consume);
    }

    @Benchmark
    public void methodReference_primitiveBoxed(Blackhole bh) {
        acceptPrimitiveBoxed(bh::consume);
    }

    @Benchmark
    public void methodReference_string(Blackhole bh) {
        acceptString(bh::consume);
    }


    @Benchmark
    public void lambda_primitive(Blackhole bh) {
        acceptPrimitive(x -> bh.consume(x));
    }

    @Benchmark
    public void lambda_primitiveBoxed(Blackhole bh) {
        acceptPrimitiveBoxed(x -> bh.consume(x));
    }

    @Benchmark
    public void lambda_string(Blackhole bh) {
        acceptString(x -> bh.consume(x));
    }


    @Benchmark
    public void lambdaMultiline_primitive(Blackhole bh) {
        acceptPrimitive(x -> {
            bh.consume(x);
        });
    }

    @Benchmark
    public void lambdaMultiline_primitiveBoxed(Blackhole bh) {
        acceptPrimitiveBoxed(x -> {
            bh.consume(x);
        });
    }

    @Benchmark
    public void lambdaMultiline_string(Blackhole bh) {
        acceptString(x -> {
            bh.consume(x);
        });
    }

    @Benchmark
    public void anonymous_primitive(Blackhole bh) {
        acceptPrimitive(new IntConsumer() {
            @Override
            public void accept(int value) {
                bh.consume(value);
            }
        });
    }

    @Benchmark
    public void anonymous_primitiveBoxed(Blackhole bh) {
        acceptPrimitiveBoxed(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) {
                bh.consume(integer);
            }
        });
    }

    @Benchmark
    public void anonymous_string(Blackhole bh) {
        acceptString(new Consumer<String>() {
            @Override
            public void accept(String s) {
                bh.consume(s);
            }
        });
    }


    @Benchmark
    public void concreteImpl_primitive(Blackhole bh) {
        acceptPrimitiveBoxed(new BlackholeConsumer(bh));

    }

    @Benchmark
    public void concreteImpl_primitiveBoxed(Blackhole bh) {
        acceptPrimitiveBoxed(new BlackholeConsumer(bh));
    }

    @Benchmark
    public void concreteImpl_string(Blackhole bh) {
        acceptString(new BlackholeConsumer(bh));
    }

    @Benchmark
    public void concreteImplCached_primitive() {
        acceptPrimitive(consumer);
    }

    @Benchmark
    public void concreteImplCached_primitiveBoxed() {
        acceptPrimitiveBoxed(consumer);
    }

    @Benchmark
    public void concreteImplCached_string() {
        acceptString(consumer);
    }


    @Benchmark
    public void direct_primitive(Blackhole bh) {
        acceptPrimitiveBoxed(bh);
    }

    @Benchmark
    public void direct_primitiveBoxed(Blackhole bh) {
        acceptPrimitiveBoxed(bh);
    }

    @Benchmark
    public void direct_string(Blackhole bh) {
        acceptString(bh);
    }


    //@CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void acceptPrimitiveBoxed(Consumer<Integer> consume) {
        consume.accept(primitive);
    }

    //@CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void acceptPrimitive(IntConsumer consume) {
        consume.accept(primitive);
    }

    //@CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void acceptString(Consumer<String> consume) {
        consume.accept(string);
    }

    //@CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void acceptPrimitiveBoxed(Blackhole bh) {
        bh.consume((Integer) primitive);
    }

    //@CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void acceptPrimitive(Blackhole bh) {
        bh.consume(primitive);
    }

    //@CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void acceptString(Blackhole bh) {
        bh.consume(string);
    }



    public class BlackholeConsumer<T> implements Consumer<T>, IntConsumer {
        private final Blackhole bh;

        public BlackholeConsumer(Blackhole bh) {
            this.bh = bh;
        }

        @Override
        public void accept(T t) {
            bh.consume(t);
        }

        @Override
        public void accept(int value) {
            bh.consume(value);
        }
    }

}
