package de.frank.jmh.basic;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;



/*--
Point is to show that the different ways to "implement an interface"
 - simple lambda: x->foo(x)
 - mulitline lambda: x->{foo(x);}
 - MethodReferences: this::foo
 - anonymous classes: new Consumer<T>(){ public void accept(T x){ foo(x); };
 - concrete implementations fooConsumer.accept(foo);
are not just synthetic sugar
Each of them emits different bytecode with different performance characteristics.



################################################################
#WARNING! Benchmark heavily skewed by stream creation overhead!
################################################################

# VM version: JDK 1.8.0_161-1-redhat, VM 25.161-b14

1 Element in List
AverageTime in  ns/op    objects  primitive  boxedPrim  <-datatypes
lambda                      17,3       15,9       22,3
lambdaMultiline             17,5       16,4       19,2
methodReference             17,7       15,9       18,9
anonymous                   17,2       15,7       18,9
concreteImpl                16,9       16,3       18,9

10 Elements in List
AverageTime in  ns/op    objects  primitive  boxedPrim  <-datatypes
lambda                      47,0       40,0       66,2
lambdaMultiline             47,6       40,4       66,4
methodReference             47,2       40,0       66,4
anonymous                   47,9       39,4       69,1
concreteImpl                50,5       44,3       68,4


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
public class LambdaMethodRefAnonymousStreamJMH {

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()//
                .include(".*"+LambdaMethodRefAnonymousStreamJMH.class.getSimpleName() + ".*")//
                .addProfiler(GCProfiler.class)//
                .build();
        new Runner(opt).run();
    }



    int[] primitive = {1,2,3,4,5,6,7,8,9,10};
    List<Integer> boxedPrimitive = Arrays.stream(primitive).boxed().collect(Collectors.toList());
    List<String> objects = Arrays.asList("1","2","3","4","5","6","7","8","9","10");

    private BlackholeConsumer consumer;


    @Setup
    public void setup(Blackhole blackHole) {
        consumer = new BlackholeConsumer(blackHole);
    }

    @Benchmark
    public void methodReference_primitive(Blackhole blackHole) {
        Arrays.stream(primitive).forEach(blackHole::consume);
    }

    @Benchmark
    public void methodReference_boxedPrimitive(Blackhole blackHole) {
        boxedPrimitive.stream().forEach(blackHole::consume);
    }

    @Benchmark
    public void methodReference_objects(Blackhole blackHole) {
        objects.stream().forEach(blackHole::consume);
    }


    @Benchmark
    public void lambda_primitive(Blackhole blackHole) {
        Arrays.stream(primitive).forEach(x -> blackHole.consume(x));
    }

    @Benchmark
    public void lambda_boxedPrimitive(Blackhole blackHole) {
       boxedPrimitive.stream().forEach(x -> blackHole.consume(x));
    }

    @Benchmark
    public void lambda_objects(Blackhole blackHole) {
        objects.stream().forEach(x -> blackHole.consume(x));
    }


    @Benchmark
    public void lambdaMultiline_primitive(Blackhole blackHole) {
        Arrays.stream(primitive).forEach(x -> {
            blackHole.consume(x);
        });
    }

    @Benchmark
    public void lambdaMultiline_boxedPrimitive(Blackhole blackHole) {
        boxedPrimitive.stream().forEach(x -> {
            blackHole.consume(x);
        });
    }

    @Benchmark
    public void lambdaMultiline_objects(Blackhole blackHole) {
       objects.stream().forEach(x -> {
           blackHole.consume(x);
        });
    }


    @Benchmark
    public void anonymous_primitive(Blackhole blackHole) {
        Arrays.stream(primitive).forEach(new IntConsumer() {
            @Override
            public void accept(int x) {
                blackHole.consume(x);
            }
        });
    }

    @Benchmark
    public void anonymous_boxedPrimitive(Blackhole blackHole) {
        boxedPrimitive.stream().forEach(new Consumer<Integer>() {
            @Override
            public void accept(Integer x) {
                blackHole.consume(x);
            }
        });
    }


    @Benchmark
    public void anonymous_objects(Blackhole blackHole) {
       objects.stream().forEach(new Consumer<String>() {
            @Override
            public void accept(String x) {
                blackHole.consume(x);
            }
        });
    }


    @Benchmark
    public void concreteImpl_primitive(Blackhole blackHole) {
        Arrays.stream(primitive).forEach(consumer);
    }

    @Benchmark
    public void concreteImpl_boxedPrimitive(Blackhole blackHole) {
        boxedPrimitive.stream().forEach(consumer);
    }

    @Benchmark
    public void concreteImpl_objects(Blackhole blackHole) {
        objects.stream().forEach(consumer);
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
