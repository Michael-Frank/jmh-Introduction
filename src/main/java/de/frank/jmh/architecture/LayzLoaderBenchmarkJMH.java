package de.frank.jmh.architecture;

import com.google.common.base.Suppliers;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/*--
Result: use whatever fancies your needs. The variants are almost identically good.
If guava is fine - use it.

Cold (singleShot) !very! inaccurate for such short runtime's!
Benchmark              Mode     Cnt   Score   Error  Units
doubleCheckedLocking     ss  600000  82,358 ± 0,976  ns/op
doubleCheckedLockingBool ss  600000  81,273 ± 1,093  ns/op
atomicReference          ss  600000  74,027 ± 1,059  ns/op
guava                    ss  600000  57,973 ± 0,836  ns/op //impl is same as doubleCheckedLockingBool


1 thread (un-contended)
Benchmark                 Mode  Cnt            Score
apache                   thrpt   30  432.232.705,281  ops/s
guava                    thrpt   30  400.486.228,714  ops/s
doubleCheckedLockingBool thrpt   30  393.490.297,020  ops/s //impl is same as doubleCheckedLockingBool
doubleCheckedLocking     thrpt   30  393.088.054,796  ops/s
atomicReference          thrpt   30  392.140.813,525  ops/s

16 threads (contended)
Benchmark                 Mode  Cnt          Score
apache                   thrpt   30  1.590.075.016  ops/s
guava                    thrpt   30  1.639.424.605  ops/s
doubleCheckedLocking     thrpt   30  1.617.764.807  ops/s
doubleCheckedLockingBool thrpt   30  1.609.340.012  ops/s //impl is same as doubleCheckedLockingBool
atomicReference          thrpt   30  1.545.984.032  ops/s

 */

/**
 * @author Michael Frank
 * @version 1.0 13.07.2018
 */
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(3)
@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark) // Important to be Scope.Benchmark
@Threads(16)
public class LayzLoaderBenchmarkJMH {

    @State(Scope.Benchmark)
    public static class MyState {
        Supplier<String> doubleCheckedLocking = DCLLazyLoader.of(MyState::expensiveOperation);
        Supplier<String> doubleCheckedLockingBool = DCLLazyLoaderBool.of(MyState::expensiveOperation);
        Supplier<String> atomicReference = AtomicLazyLoader.of(MyState::expensiveOperation);
        com.google.common.base.Supplier<String> guava = Suppliers.memoize(MyState::expensiveOperation); //same as doubleCheckedLockingBool
        LazyInitializer<String> apache = new LazyInitializer<String>() {
            @Override
            protected String initialize() {
                return expensiveOperation();
            }
        };


        private static String expensiveOperation() {
            return String.format("foo %s %s", "foo", "bar");
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()//
                .include(".*" + LayzLoaderBenchmarkJMH.class.getSimpleName() + ".*")//
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
    public String apache(MyState state) throws ConcurrentException {
        return state.apache.get();
    }

    @Benchmark
    public String atomicReference(MyState state) {
        return state.atomicReference.get();
    }


    /**
     * Thread safe lazily loads and caches the value provided by a supplier.
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
     * business method, but only once, cache the result and in a thread safe manner.
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

    public static class AtomicLazyLoader<T> implements Supplier<T> {

        private Supplier<T> supplier;
        private AtomicReference<T> object = new AtomicReference<>();

        private AtomicLazyLoader(Supplier<T> supplier) {
            this.supplier = Objects.requireNonNull(supplier);
        }

        public static <T> Supplier<T> of(Supplier<T> supplier) {
            return new AtomicLazyLoader<>(supplier);
        }

        public T get() {
            T t = object.get();// fast path
            if (t == null) {
                // slow path
                t = object.updateAndGet(objec -> objec != null ? objec : supplier.get());
            }
            return t;
        }
    }

}
