package de.frank.jmh.basic;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.IntConsumer;



/*--
Point is to show that the different ways to "implement an interface" are not just synthetic sugar
Each of them emits different bytecode with different performance characteristics.
Variants:
 - simple lambda: x->foo(x)
 - multi-line lambda: x->{foo(x);}
 - MethodReferences: this::foo
 - anonymous classes: new Consumer<T>(){ public void accept(T x){ foo(x); };
 - concrete implementations fooConsumer.accept(foo);


There are no real difference. Maybe due to heavily optimized inlining effects?
TODO: inverstigate
                      string  primitive  primBoxed
                      ns/op       ns/op      ns/op
lambda                  3,3         2,7        4,2  # simple lambdas are the easiest for the JVM to optimize
lambdaMultiline         3,7         2,6        4,3  # multiline may introduce a slight overhead
methodReference         3,2         2,6        4,1  # Method reference do not show differences in *this case*
anonymous               3,2         2,6        4,1
concreteImpl            4,1         3,1        4,8
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
                .include(".*" + LambdaMethodRefAnonymousJMH.class.getSimpleName() + ".*")//
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
    public void concreteImpl_primitive() {
        acceptPrimitive(consumer);
    }

    @Benchmark
    public void concreteImpl_primitiveBoxed() {
        acceptPrimitiveBoxed(consumer);
    }

    @Benchmark
    public void concreteImpl_string() {
        acceptString(consumer);
    }


    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void acceptPrimitiveBoxed(Consumer<Integer> consume) {
        consume.accept(primitive);
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void acceptPrimitive(IntConsumer consume) {
        consume.accept(primitive);
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private void acceptString(Consumer<String> consume) {
        consume.accept(string);
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
