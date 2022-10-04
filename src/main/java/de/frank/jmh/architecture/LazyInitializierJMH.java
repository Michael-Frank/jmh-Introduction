package de.frank.jmh.architecture;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Supplier;


/**
 * Somewhat strange usecase found in a project with heavy javax dependency injection. The typical pattern is reverse.
 * In Object instance field we have the "lookup" value, while in the to be cached "getFromLookup(SourceForLook s) method
 * we are provided with the object we need to create the lookup table from (weird, i know)
 * Therefore we cannot use the initializers form Guava (Suppliers.memoize()) or Apache commons (LazyInitializer)
 * <p>
 * HOT code (all lazy loaded caches are warm)
 * dclLazyLoader_inline_refCache                       thrpt  30  1048.601.519 ± 17.428.786 ops/s #1st - perf. winner but most complex
 * dclLazyLoader_inline_refCache:·gc.alloc.rate.norm   thrpt  30        ≈ 10⁻⁵              B/op
 * dclLazyLoader_inline                                thrpt  30  1001.813.857 ± 14.303.987 ops/s #2nd - but still complex
 * dclLazyLoader_inline:·gc.alloc.rate.norm            thrpt  30        ≈ 10⁻⁵              B/op
 * dclLazyLoader                                       thrpt  30   958.420.441 ± 18.329.592 ops/s #3rd - nice but not much benefit over AtomicReference
 * dclLazyLoader:·gc.alloc.rate.norm                   thrpt  30        ≈ 10⁻⁵              B/op
 * atomicReference_inline                              thrpt  30   951.313.180 ± 19.286.816 ops/s #4rd - simple and good perf
 * atomicReference_inline:·gc.alloc.rate.norm          thrpt  30        ≈ 10⁻⁵              B/op
 * atomicReferenceFieldLazyLoader                      thrpt  30   951.980.431 ± 32.283.514 ops/s #5rd - good perf  and mid complexity
 * atomicReferenceFieldLazyLoader:·gc.alloc.rate.norm  thrpt  30        ≈ 10⁻⁵              B/op
 * atomicReferenceLazyLoader                           thrpt  30   852.729.858 ± 22.291.655 ops/s #6th - simple and still reasonable perf
 * atomicReferenceLazyLoader:·gc.alloc.rate.norm       thrpt  30        ≈ 10⁻⁴              B/op
 * <p>
 * COLD - first call (single shot time)  lower is better)
 * Benchmark                                           Mode  Cnt Score   Error Units
 * dclLazyLoader_inline_refCache                         ss   15  2389 ±  545  ns/op #1st - winner, but most complex
 * dclLazyLoader_inline_refCache:·gc.alloc.rate.norm     ss   15   509 ±    5   B/op
 * dclLazyLoader_inline                                  ss   15  2467 ±  321  ns/op #2nd - good perf but still complex
 * dclLazyLoader_inline:·gc.alloc.rate.norm              ss   15   506 ±    5   B/op
 * atomicReference_inline                                ss   30  3062 ±  518  ns/op #3rd - simple and reasonable perf
 * atomicReference_inline:·gc.alloc.rate.norm            ss   30   507 ±    2   B/op
 * atomicReferenceLazyLoader                             ss   15  4650 ± 1326  ns/op #4th - simple and reasonable perf
 * atomicReferenceLazyLoader:·gc.alloc.rate.norm         ss   15   523 ±    4   B/op
 * atomicReferenceFieldLazyLoader                        ss   15  5256 ± 1207  ns/op #6th - only worth if memory requirements ar tight
 * atomicReferenceFieldLazyLoader:·gc.alloc.rate.norm    ss   15   524 ±    6   B/op
 * dclLazyLoader                                         ss   15  4565 ± 1100  ns/op #5th - probably better of with AtomicReference
 * dclLazyLoader:·gc.alloc.rate.norm                     ss   15   525 ±    6   B/op
 *
 * @author Michael Frank
 * @version 1.0 13.07.2018
 */
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(3)
@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark) // Important to be Scope.Benchmark
@Threads(16)
public class LazyInitializierJMH {

    private static Map<String, SomeType> createLookup(List<SomeType> relevantTypes) {
        Map<String, SomeType> tmp = new HashMap<>();
        for (SomeType t : relevantTypes) {
            tmp.put(t.letter, t);
        }
        return Collections.unmodifiableMap(tmp);
    }


    @State(Scope.Benchmark)
    public static class MyState {

        List<SomeType> param = Arrays.asList(SomeType.values());
        String findThis = "s";


        volatile Map<String, SomeType> cache;
        private AtomicReference<Map<String, SomeType>> reference = new AtomicReference<>();
        DCLLazyLoader<Map<String, SomeType>> dclLazyLoader = new DCLLazyLoader<>();
        AtomicReferenceLazyLoader<Map<String, SomeType>> atomicReferenceLazyLoader = new AtomicReferenceLazyLoader<>();
        AtomicReferenceFieldLazyLoader<Map<String, SomeType>> atomicReferenceFieldLazyLoader = new AtomicReferenceFieldLazyLoader<>();

    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()//
                .include(LazyInitializierJMH.class.getName())//
                .result(String.format("%s_%s.json",
                        DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                        LazyInitializierJMH.class.getSimpleName()))
                .addProfiler(GCProfiler.class)//
                .build();
        new Runner(opt).run();
    }


    @Benchmark
    public SomeType dclLazyLoader_inline(MyState state) {

        if (state.cache == null) {//fastpath if !null
            synchronized (this) {
                //slow path - requires fresh volatile read
                if (state.cache == null) {

                    //only set fully initialized objects atomically visible when using DCL idiom (must be last line in
                    // this block), or else the fast-path read outside the synchronized block may see an empty map.
                    state.cache = createLookup(state.param);
                }
            }
        }
        return state.cache.get(state.findThis);
    }

    @Benchmark
    public SomeType dclLazyLoader_inline_refCache(MyState state) {

        Map<String, SomeType> tmp = state.cache;

        if (tmp == null) {//fastpath if !null
            synchronized (this) {
                //slow path - requires fresh volatile read
                if ((tmp = state.cache) == null) {
                    tmp = createLookup(state.param);
                    //only set fully initialized objects atomically visible when using DCL idiom (must be last line in
                    // this block), or else the fast-path read outside the synchronized block may see an empty map.
                    state.cache = tmp;
                }
            }
        }

        return tmp.get(state.findThis);
    }


    @Benchmark
    public SomeType atomicReference_inline(MyState state) {

        Map<String, SomeType> t = state.reference.get();// fast path
        if (t == null) {
            // slow path
            t = state.reference.updateAndGet(objec -> objec != null ? objec : createLookup(state.param));
        }
        return t.get(state.findThis);

    }


    @Benchmark
    public SomeType dclLazyLoader(MyState state) {
        return state.dclLazyLoader.get(() -> createLookup(state.param)).get(state.findThis);
    }


    @Benchmark
    public SomeType atomicReferenceLazyLoader(MyState state) {
        return state.atomicReferenceLazyLoader.get(() -> createLookup(state.param)).get(state.findThis);
    }


    @Benchmark
    public SomeType atomicReferenceFieldLazyLoader(MyState state) {
        return state.atomicReferenceFieldLazyLoader.get(() -> createLookup(state.param)).get(state.findThis);
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
    public static class DCLLazyLoader<T> {

        // "cachedObject" is not required to be volatile - guarded by volatile read of
        // "supplier"
        private volatile T cachedObject;

        public T get(Supplier<T> supplier) {
            if (cachedObject == null) {
                synchronized (this) {
                    if (cachedObject == null) {
                        T t = supplier.get();
                        cachedObject = t;
                        return t;
                    }
                }
            }
            return cachedObject;
        }
    }


    public static class AtomicReferenceLazyLoader<T> {

        private AtomicReference<T> object = new AtomicReference<>();

        public T get(Supplier<T> supplier) {
            T t = object.get();// fast path
            if (t == null) {
                // slow path
                t = object.updateAndGet(objec -> objec != null ? objec : supplier.get());
            }
            return t;
        }
    }

    /**
     * Benefits of this {@link AtomicReferenceFieldUpdater} vs {@link AtomicReferenceLazyLoader}: memory efficiency.
     * It requires one less object indirection (the AtomicReference wrapper)
     * The AtomicReferenceFieldUpdater itself is static.
     *
     * @param <T>
     */
    public static class AtomicReferenceFieldLazyLoader<T> {

        private static final AtomicReferenceFieldUpdater<AtomicReferenceFieldLazyLoader, Object> REF_UPDATER = AtomicReferenceFieldUpdater.newUpdater(AtomicReferenceFieldLazyLoader.class, Object.class, "cachedObj");

        private volatile T cachedObj = null; //must be volatile

        public T get(Supplier<T> supplier) {
            T t = cachedObj;// fast path
            if (t == null) {
                // slow path
                t = (T) REF_UPDATER.updateAndGet(this, o -> o != null ? o : supplier.get());
            }
            return t;
        }
    }


    enum SomeType {
        a("z"),
        b("y"),
        c("x"),
        d("w"),
        e("v"),
        f("u"),
        g("t"),
        h("s"),
        i("r"),
        j("q"),
        k("p"),
        l("o"),
        m("n"),
        n("m"),
        o("l");

        private final String letter;

        SomeType(String u) {
            this.letter = u;
        }
    }
}
