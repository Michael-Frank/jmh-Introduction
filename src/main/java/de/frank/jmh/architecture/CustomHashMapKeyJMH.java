package de.frank.jmh.architecture;

import lombok.Builder;
import lombok.Data;
import lombok.Value;
import org.apache.commons.lang3.RandomStringUtils;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;


/*--

Benchmark                       Score Units
badCache_containsAndPut       1160035 ops/s
badCache_getAndPut            3400767 ops/s
betterCache_getAndPut        58577668 ops/s
betterCache_putIfAbsent      36324723 ops/s
betterCache_computeIfAbsent  46825796 ops/s

Benchmark                                   Mode  Cnt         Score         Error  Units
badCache_containsAndPut                    thrpt   30   1160035,092 ±   56390,945  ops/s
badCache_getAndPut                         thrpt   30   3400767,115 ±  110719,629  ops/s
betterCache_getAndPut                      thrpt   30  58577668,167 ± 2241150,405  ops/s
betterCache_putIfAbsent                    thrpt   30  36324723,639 ± 1701861,795  ops/s
betterCache_computeIfAbsent                thrpt   30  46825796,505 ± 2503221,561  ops/s
goodIntentionsBadResult_computeIfAbsent    thrpt   30      1298,560 ±     151,183  ops/s

 */

@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(3)
@State(Scope.Benchmark)
public class CustomHashMapKeyJMH {

    public static void main(String[] args) throws RunnerException {

        Options opt = new OptionsBuilder()
                .include(CustomHashMapKeyJMH.class.getName())
                .result(String.format("%s_%s.json",
                        DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                        CustomHashMapKeyJMH.class.getSimpleName()))
                .build();
        new Runner(opt).run();
    }


    @Data
    @Builder
    private static class SomeKeyPart {
        int a, b;
        String c, d;
    }

    //This map is accessed with Key= SomeKeyPartA.toString() + SomeKeyPartB.toString()
    private ConcurrentHashMap<String, Object> badCache = new ConcurrentHashMap<>();

    @Value
    private static class JoinedKey {
        SomeKeyPart a, b;
    }

    private ConcurrentHashMap<JoinedKey, Object> betterCache = new ConcurrentHashMap<>();


    @Value
    private static class GoodIntentionsBadResult {
        private final SomeKeyPart a;
        private final SomeKeyPart b;
        private final int hashCode;

        public GoodIntentionsBadResult(SomeKeyPart a, SomeKeyPart b) {
            this.a = a;
            this.b = b;
            this.hashCode = Objects.hash(a, b);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }

    private ConcurrentHashMap<GoodIntentionsBadResult, Object> goodIntentionsBadResultCache = new ConcurrentHashMap<>();


    SomeKeyPart aKey;
    SomeKeyPart bKey;
    int elementsInCache = 100;

    @Setup(Level.Trial)
    public void setup() {
        int counter = 0;
        for (int i = 0; i < elementsInCache; i++) {
            SomeKeyPart aKey = SomeKeyPart.builder()
                    .a(counter++)
                    .b(counter++)
                    .c(RandomStringUtils.randomAlphanumeric(16))
                    .d(RandomStringUtils.randomAlphanumeric(16))
                    .build();
            SomeKeyPart bKey = SomeKeyPart.builder()
                    .a(counter++)
                    .b(counter++)
                    .c(RandomStringUtils.randomAlphanumeric(16))
                    .d(RandomStringUtils.randomAlphanumeric(16))
                    .build();

            Object val = new Object();
            badCache.put(aKey.toString() + bKey.toString(), val);
            betterCache.put(new JoinedKey(aKey, bKey), val);
            goodIntentionsBadResultCache.put(new GoodIntentionsBadResult(aKey, bKey), val);
            if (i == 0) {
                this.aKey = aKey;
                this.bKey = bKey;
            }
        }
    }

    @Benchmark
    public Object badCache_containsAndPut() {
        String key = aKey.toString() + bKey.toString();
        if (!badCache.contains(key)) {
            badCache.put(key, newCachedObject());
        }
        return badCache.get(key);
    }


    @Benchmark
    public Object badCache_getAndPut() {
        String key = aKey.toString() + bKey.toString();
        Object val;
        if ((val = badCache.get(key)) == null) {
            val = newCachedObject();
            badCache.put(key, val);
        }
        return val;
    }

    @Benchmark
    public Object betterCache_getAndPut() {
        JoinedKey key = new JoinedKey(aKey, bKey);
        Object val;
        if ((val = betterCache.get(key)) == null) {
            val = newCachedObject();
            betterCache.put(key, val);
        }
        return val;
    }


    @Benchmark
    public Object betterCache_putIfAbsent() {
        return betterCache.putIfAbsent(new JoinedKey(aKey, bKey), newCachedObject());
    }

    @Benchmark
    public Object betterCache_computeIfAbsent() {
        return betterCache.computeIfAbsent(new JoinedKey(aKey, bKey), (k) -> newCachedObject());
    }


    @Benchmark
    public Object goodIntentionsBadResult_computeIfAbsent() {
        return goodIntentionsBadResultCache.computeIfAbsent(new GoodIntentionsBadResult(aKey, bKey), (k) -> newCachedObject());
    }


    public Object newCachedObject() {
        Blackhole.consumeCPU(1);
        return new Object();
    }

}
