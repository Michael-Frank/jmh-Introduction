package de.frank.jmh.algorithms.de.frank.jmh.architecture;

import com.google.common.base.Suppliers;
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
doubleCheckedLocking2    ss  600000  64,803 ± 0,909  ns/op
doubleCheckedLockingBool ss  600000  81,273 ± 1,093  ns/op
atomicReference          ss  600000  74,027 ± 1,059  ns/op
guava                    ss  600000  57,973 ± 0,836  ns/op //impl is same as doubleCheckedLockingBool


1 thread (un-contended)
Benchmark                 Mode  Cnt      Score
doubleCheckedLocking     thrpt   30  250.866.385 ops/s
doubleCheckedLocking2    thrpt   30  252.989.272 ops/s
doubleCheckedLockingBool thrpt   30  247.712.652 ops/s
atomicReference          thrpt   30  242.032.148 ops/s
guava                    thrpt   30  247.541.635 ops/s //impl is same as doubleCheckedLockingBool

16 threads (contended)
Benchmark                 Mode  Cnt      Score
doubleCheckedLocking     thrpt   30  999.919.860 ops/s
doubleCheckedLocking2    thrpt   30  993.081.667 ops/s
doubleCheckedLockingBool thrpt   30  969.773.974 ops/s
atomicReference          thrpt   30  968.192.148 ops/s
guava                    thrpt   30  981.892.277 ops/s //impl is same as doubleCheckedLockingBool

 */

/**
 * @author Michael Frank
 * @version 1.0 13.07.2018
 */
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(3)
@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark) // Important to be Scope.Benchmark
@Threads(1)
public class LayzLoaderBenchmarkJMH {

    @State(Scope.Benchmark)
    public static class MyState {
        Supplier<String> doubleCheckedLocking = DCLLazyLoader.of(MyState::expensiveOperation);
        Supplier<String> doubleCheckedLocking2 = DCLLazyLoader2.of(MyState::expensiveOperation);
        Supplier<String> doubleCheckedLockingBool = DCLLazyLoaderBool.of(MyState::expensiveOperation);
        Supplier<String> atomicReference = AtomicLazyLoader.of(MyState::expensiveOperation);
        com.google.common.base.Supplier<String> guava = Suppliers.memoize(MyState::expensiveOperation); //same as doubleCheckedLockingBool

        private static String expensiveOperation() {
            return String.format("foo %s %s", "foo", "bar");
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()//
                .include(".*" + LayzLoaderBenchmarkJMH.class.getSimpleName() + ".*")//
                // .addProfiler(GCProfiler.class)//
                .build();
        new Runner(opt).run();
    }

    @Benchmark
    public String doubleCheckedLocking(MyState state) {
        return state.doubleCheckedLocking.get();
    }

    @Benchmark
    public String bool(MyState state) {
        return state.doubleCheckedLockingBool.get();
    }

    @Benchmark
    public String doubleCheckedLocking2(MyState state) {
        return state.doubleCheckedLocking2.get();
    }

    @Benchmark
    public String guava(MyState state) {
        return state.guava.get();
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

    public static class DCLLazyLoader2<T> implements Supplier<T> {

        // "cachedObject" is not required to be volatile - guarded by volatile read of
        // "supplier"
        private volatile Supplier<T> supplier;
        private T object;

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
            // Still doubt? Guavas MemoizingSupplier employs the same logic.
            if (supplier != null) {
                synchronized (this) {
                    if (supplier != null) {
                        T object = supplier.get();
                        supplier = null;
                        this.object = object;
                        return object;
                    }
                }
            }
            return object;
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
