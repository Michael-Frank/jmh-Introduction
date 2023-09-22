package de.frank.jmh.architecture;

import com.google.common.base.Suppliers;
import lombok.Getter;
import org.apache.commons.lang3.concurrent.AtomicInitializer;
import org.apache.commons.lang3.concurrent.AtomicSafeInitializer;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/*--
Result: use whatever fancies your needs. The variants are all almost identically good.

Tl;dr:
if (You are writing a lib) {
   if ( using lombok ) {  use lombok's  @Getter(lazy = true)
   } else if ( apache in class path ) {  use apache LazyInitializer
   } else { copy paste DCLLazyLoader }
   //you should not use guava in a library - guava versions are not very backward compatible!
} else { //assuming you are writing something that has its own main()
  if ( using lombok ) {  use lombok's  @Getter(lazy = true)
  } else if ( guava in classpath ){  use  Suppliers.memoize(this::expensiveOperation); its nicer to use then apache LazyInitializer }
  } else if ( apache in class path ) {  use apache LazyInitializer
  } else { copy paste DCLLazyLoader }
}



JDK 1.8.0_212, OpenJDK 64-Bit Server VM, 25.212-b04

Cold (singleShot) !very! inaccurate for such short runtime's!
Benchmark                 Mode    Cnt    Score    Error  Units
apacheAtomic                ss  30000  271,409 ±  9,808  ns/op
apacheAtomicSafe            ss  30000  233,134 ±  9,521  ns/op
apacheLazy                  ss  30000  231,441 ±  9,358  ns/op
atomicSafeReference         ss  30000  249,565 ± 10,432  ns/op
doubleCheckedLocking        ss  30000  251,627 ± 11,524  ns/op
doubleCheckedLockingBool    ss  30000  243,617 ±  9,706  ns/op
guava                       ss  30000  246,742 ±  9,783  ns/op
lombokLazyGetter            ss  30000  260,602 ± 10,576  ns/op


1 thread (un-contended)
Benchmark                  Mode  Cnt        Score        Error  Units
apacheLazy                thrpt   15  419.549.359 ± 23.140.506  ops/s
apacheAtomic              thrpt   15  440.323.357 ± 21.325.226  ops/s
apacheAtomicSafe          thrpt   15  424.683.310 ± 19.432.998  ops/s
guava                     thrpt   15  400.282.683 ± 10.714.310  ops/s
lombokLazyGetter          thrpt   15  403.419.628 ± 21.783.503  ops/s
atomicSafeReference       thrpt   15  393.224.676 ± 12.942.598  ops/s
doubleCheckedLocking      thrpt   15  392.044.732 ± 17.580.392  ops/s
doubleCheckedLockingBool  thrpt   15  403.658.959 ±  4.608.701  ops/s

16 threads (contended)
Benchmark                  Mode  Cnt          Score        Error  Units
apacheLazy                thrpt   15  1.909.452.011 ± 69.983.943  ops/s #nothing wrong with apache, the abstract class extension is a bit old-school though
apacheAtomic              thrpt   15  1.774.326.440 ± 83.061.960  ops/s
apacheAtomicSafe          thrpt   15  1.669.911.920 ± 60.636.695  ops/s
guava                     thrpt   15  1.719.092.137 ± 49.238.983  ops/s #guava is fine to
lombokLazyGetter          thrpt   15  1.762.643.349 ± 15.398.775  ops/s #if lombok is used - this one is quite fancy
atomicSafeReference       thrpt   15  1.714.699.352 ± 66.808.984  ops/s #or just use atomicSafeReference..
doubleCheckedLocking      thrpt   15  1.648.277.279 ± 78.491.368  ops/s
doubleCheckedLockingBool  thrpt   15  1.683.437.848 ± 90.864.384  ops/s

 */

/**
 * @author Michael Frank
 * @version 1.1 20.11.2019
 */
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(3)
@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark) // Important to be Scope.Benchmark
@Threads(16)
public class LazyLoaderBenchmarkJMH {

    @State(Scope.Benchmark)
    public static class MyState {
        private String var1 = "a";
        private String var2 = "b";

        //My own impls
        Supplier<String> doubleCheckedLocking = DCLLazyLoader.of(this::expensiveOperation);
        Supplier<String> doubleCheckedLockingBool = DCLLazyLoaderBool.of(this::expensiveOperation);
        Supplier<String> atomicSafeReference = AtomicSafeLazyLoader.of(this::expensiveOperation);

        //3rd party library impls

        //Lombok - most convenient to use and generates a classic doubleCheckedLocking pattern in the background
        @Getter(lazy = true)
        private final String lombokLazyGetter = expensiveOperation();

        //guava - same as doubleCheckedLockingBool - it has a nicer usage pattern then apaches Lazy.. abstract class extensions
        com.google.common.base.Supplier<String> guava = Suppliers.memoize(this::expensiveOperation);


        //Uses classic doubleCheckedLocking - initialize() is guaranteed to be called only once
        LazyInitializer<String> apacheLazy = new LazyInitializer<String>() {
            @Override
            protected String initialize() {
                return expensiveOperation();
            }
        };
        //Uses AtomicReference internally - initialize() my be called multiple times - no real benefit over LazyInitializer
        // at the end, all fast paths still must read the initialized value from a volatile.
        AtomicInitializer<String> apacheAtomicInitializer = new AtomicInitializer<String>() {
            @Override
            protected String initialize() {
                return expensiveOperation();
            }
        };
        //Uses AtomicReference internally - initialize() only called once - much more complicated and still real benefit over LazyInitializer except some corner cases
        // at the end, all fast paths still must read the initialized value from a volatile.
        AtomicSafeInitializer<String> apacheAtomicSafeInitializer = new AtomicSafeInitializer<String>() {
            @Override
            protected String initialize() {
                return expensiveOperation();
            }
        };

        private String expensiveOperation() {
            return String.format("foo %s %s", var1, var2);
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()//
                .include(LazyLoaderBenchmarkJMH.class.getName())//
                .result(String.format("%s_%s.json",
                        DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                        LazyLoaderBenchmarkJMH.class.getSimpleName()))
                .addProfiler(GCProfiler.class)//
                .build();
        new Runner(opt).run();
    }

    @Benchmark
    public String doubleCheckedLocking(MyState state) {
        return state.doubleCheckedLocking.get();
    }

    @Benchmark
    public String doubleCheckedLockingBool(MyState state) {
        return state.doubleCheckedLockingBool.get();
    }

    @Benchmark
    public String guava(MyState state) {
        return state.guava.get();
    }

    @Benchmark
    public String apacheLazy(MyState state) throws ConcurrentException {
        return state.apacheLazy.get();
    }
    @Benchmark
    public String apacheAtomic(MyState state) throws ConcurrentException {
        return state.apacheAtomicInitializer.get();
    }

    @Benchmark
    public String apacheAtomicSafe(MyState state) throws ConcurrentException {
        return state.apacheAtomicSafeInitializer.get();
    }

    @Benchmark
    public String atomicSafeReference(MyState state) {
        return state.atomicSafeReference.get();
    }

    @Benchmark
    public String lombokLazyGetter(MyState state) {
        return state.getLombokLazyGetter();
    }


    /**
     * Thread safe lazily loads and caches the value provided by dclLazyLoader supplier.
     * Multiple Executions of {@link DCLLazyLoader#get()} are guaranteed to always
     * return the same object. It is guaranteed that {@link Supplier#get()} of the
     * provided supplier is called only once. <br/>
     * <br/>
     * Usage Example 1:
     *
     * <pre>
     *  public SomeClass{
     *    private Supplier<ExpensiveObject> lazyExpensiveObject = LazyLoader.of(SomeClass::createExpensiveObject);
     *
     *    public void someMethod(){
     *       ExpensiveObject obj = lazyExpensiveObject.get();
     *    }
     *
     *    public ExpensiveObject createExpensiveObject(){
     *       //you initialization code
     *    }
     *  }
     * </pre>
     * <p>
     * Usage Example 2:
     * <pre>
     * Supplier<String> anotherExpensiveOp = LazyLoader.of(() -> "ExpensiveDebugString" + someObject.toString());
     * </pre>
     * <p>
     * When would you use this:<br/>
     * deferred execution of e.g. expensive JNDI lookups. The JNDI might not be
     * ready/initialized while you create an instance of your class - so you cannot
     * do the lookup in your constructor. At the same time executing the lookup on
     * every invocation of your business method(e.g. every request) is to expensive.
     * So you want to do the JNDI lookup lazily on the first invocation of your
     * business method, but only once, cache the result and in dclLazyLoader thread safe manner.
     */
    public static class DCLLazyLoader<T> implements Supplier<T> {

        // "cachedObject" is not required to be volatile - guarded by volatile read of
        // "supplier"
        private T cachedObject;
        private volatile Supplier<T> supplier;

        private DCLLazyLoader(Supplier<T> supplier) {
            this.supplier = Objects.requireNonNull(supplier);
        }

        public static <T> Supplier<T> of(Supplier<T> supplier) {
            return new DCLLazyLoader<>(supplier);
        }

        public T get() {
            // This DCL idiom requires Java >=1.5 and volatile to work but then it is
            // guaranteed to be correct
            // On doubt read:
            // https://docs.oracle.com/javase/specs/jls/se7/html/jls-17.html#jls-17.4.5
            // https://shipilev.net/blog/2014/safe-public-construction/#_safe_publication
            // Still doubt? Guavas MemoizingSupplier employs the same logic.
            if (supplier != null) {
                synchronized (this) {
                    if (supplier != null) {
                        cachedObject = supplier.get();
                        supplier = null;
                    }
                }
            }
            return cachedObject;
        }
    }

    public static class DCLLazyLoader2<T> implements Supplier<T> {


        private volatile T cachedObject;
        private Supplier<T> supplier;

        private DCLLazyLoader2(Supplier<T> supplier) {
            this.supplier = Objects.requireNonNull(supplier);
        }

        public static <T> Supplier<T> of(Supplier<T> supplier) {
            return new DCLLazyLoader2<>(supplier);
        }

        public T get() {
            // This DCL idiom requires Java >=1.5 and volatile to work but then it is
            // guaranteed to be correct
            // On doubt read:
            // https://docs.oracle.com/javase/specs/jls/se7/html/jls-17.html#jls-17.4.5
            // https://shipilev.net/blog/2014/safe-public-construction/#_safe_publication

            T cachedRef = cachedObject; //minimize volatile reads by local caching
            if (cachedRef == null) {
                synchronized (this) {
                    cachedRef = cachedObject; //MUST re-read after synchronized
                    if (cachedRef == null) {
                        cachedObject = cachedRef = supplier.get();
                        supplier = null; //we no longer need it
                    }
                }
            }

            return cachedRef;
        }
    }


    public static class DCLLazyLoaderBool<T> implements Supplier<T> {

        // "cachedObject" is not required to be volatile - guarded by volatile read of
        // "loaded"
        private volatile boolean loaded = false;
        private Supplier<T> supplier;
        private T object;

        private DCLLazyLoaderBool(Supplier<T> supplier) {
            this.supplier = Objects.requireNonNull(supplier);
        }

        public static <T> Supplier<T> of(Supplier<T> supplier) {
            return new DCLLazyLoaderBool<>(supplier);
        }

        public T get() {
            if (!loaded) {
                synchronized (this) {
                    if (!loaded) {
                        T t = supplier.get();
                        object = t;
                        loaded = true;
                        supplier = null;
                        return t;
                    }
                }
            }
            return object;
        }
    }

    public static class AtomicSafeLazyLoader<T> implements Supplier<T> {

        private Supplier<T> supplier;
        private AtomicReference<T> object = new AtomicReference<>();

        private AtomicSafeLazyLoader(Supplier<T> supplier) {
            this.supplier = Objects.requireNonNull(supplier);
        }

        public static <T> Supplier<T> of(Supplier<T> supplier) {
            return new AtomicSafeLazyLoader<>(supplier);
        }

        public T get() {
            T t = object.get();// fast path
            if (t == null) {
                // slow path
                t = object.updateAndGet(o -> o != null ? o : supplier.get());
            }
            return t;
        }
    }


}
