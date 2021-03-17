package de.frank.jmh.algorithms;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.*;
import com.fasterxml.jackson.databind.util.*;
import com.fasterxml.jackson.datatype.jsr310.*;
import de.frank.impl.jaxb.*;
import de.frank.jmh.model.*;
import lombok.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.*;
import org.openjdk.jmh.runner.options.*;

import java.io.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;

@State(Scope.Benchmark)
public class DeepCloneJMH {
    static final Class<HierarchicalMockModel> MODEL_CLASS = HierarchicalMockModel.class;

    @State(Scope.Thread)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MyState {
        HierarchicalMockModel model = HierarchicalMockModel.newInstance(10, 10, 100);
    }

    ObjectMapper cachedCustMapper = JsonMapper.builder().build();
    ObjectReader cachedCustTypedReader = cachedCustMapper.readerFor(MODEL_CLASS);
    ObjectWriter cachedCustTypedWriter = cachedCustMapper.writerFor(MODEL_CLASS);
    ObjectReader cachedCustGenericReader = cachedCustMapper.readerFor(MODEL_CLASS);
    ObjectWriter cachedCustGenericWriter = cachedCustMapper.writerFor(MODEL_CLASS);


    ObjectMapper cachedDefMapper =
            new com.fasterxml.jackson.databind.json.JsonMapper().registerModule(new JavaTimeModule());
    ObjectReader cachedDefGenericReader = cachedDefMapper.readerFor(MODEL_CLASS);
    ObjectWriter cachedDefGenericWriter = cachedDefMapper.writerFor(MODEL_CLASS);

    /*--

    Benchmark winner:
    Benchmark                                                 Mode  Cnt    Score   Error  Units
    jackson_Def_Generic_Cached_TokenBuffer  avgt   30  183,680 ± 5,404  us/op

    @1 Thread  - lower score is better
    Benchmark                                Mode  Cnt      Score      Error  Units
    jackson_Def_Generic_Cached_TokenBuffer   avgt   10    200,775 ±   27,891  us/op
    jackson_Cust_Typed_Cached_TokenBuffer    avgt   10    220,953 ±   32,675  us/op
    jackson_Cust_Generic_Cached_TokenBuffer  avgt   10    238,411 ±   63,346  us/op
    jackson_Cust_Generic_Cached_Bytes        avgt   10    369,848 ±   14,546  us/op
    jackson_Cust_Typed_Cached_Bytes          avgt   10    379,287 ±   38,683  us/op
    jackson_Cust_Typed_Cached_String         avgt   10    418,160 ±   65,668  us/op
    jackson_Cust                             avgt   10   3289,476 ± 1129,961  us/op
    jackson_Def                              avgt   10   3416,246 ±  636,904  us/op
    jaxbMoxy                                 avgt   10  14465,160 ± 2820,086  us/op


    @32 Threads ("contended") - lower score is better
    Benchmark                                Mode  Cnt       Score       Error  Units
    jackson_Def_Generic_Cached_TokenBuffer   avgt   10    2643,035 ±    53,679  us/op
    jackson_Cust_Typed_Cached_TokenBuffer    avgt   10    2879,103 ±    99,434  us/op
    jackson_Cust_Generic_Cached_TokenBuffer  avgt   10    3181,553 ±   144,256  us/op
    jackson_Cust_Typed_Cached_String         avgt   10    3685,436 ±   273,136  us/op
    jackson_Cust_Generic_Cached_Bytes        avgt   10    4034,739 ±   619,449  us/op
    jackson_Cust_Typed_Cached_Bytes          avgt   10    4028,791 ±   937,570  us/op
    jackson_Def                              avgt   10   49079,140 ±  8602,176  us/op
    jackson_Cust                             avgt   10   49790,036 ±  5904,977  us/op
    jaxbMoxy                                 avgt   10  155902,466 ± 39316,123  us/op

     */

    public static void main(String[] args) throws Exception {
        DeepCloneJMH bench = new DeepCloneJMH();
        //quick overview of winning algorithm
        var state = new MyState(HierarchicalMockModel.newInstance(10, 10, 10));

        simpleBench(15_000, 1000, () -> bench.jackson_Def_Generic_Cached_TokenBuffer(state));

        bench.benchmarkAllDeepCopyApproaches();
        bench.benchmarkWinnerMoreDeeply();
    }

    public void benchmarkWinnerMoreDeeply() throws Exception {
        //test impls if deep copy produces the same result as the original model
        var state = new MyState();
        assertThat(jackson_Def_Generic_Cached_TokenBuffer(state)).isEqualTo(state.model);

        Options opt = new OptionsBuilder()
                .include(this.getClass().getName() + ".jackson_Def_Generic_Cached_TokenBuffer")
                .mode(Mode.AverageTime)
                .timeUnit(TimeUnit.MICROSECONDS)
                .warmupTime(TimeValue.seconds(1))
                .warmupIterations(5)
                .measurementTime(TimeValue.seconds(1))
                .measurementIterations(10)
                .threads(1)//contended
                .forks(3)
                .shouldFailOnError(true)
                .build();

        new Runner(opt).run();
    }


    public void benchmarkAllDeepCopyApproaches() throws Exception {
        //test all impls if they produces the same result as the original model
        var state = new MyState();
        Arrays.stream(this.getClass().getDeclaredMethods())
              .filter(m -> m.isAnnotationPresent(Benchmark.class))
              //.filter(m -> m.getName().startsWith(prefix))
              .forEach(m -> {
                  try {
                      assertThat(m.invoke(this, state)).isEqualTo(state.model);
                  } catch (ReflectiveOperationException e) {
                      throw new RuntimeException(e);
                  }
              });

        Options opt = new OptionsBuilder()
                .include(this.getClass().getName() + ".*")
                .mode(Mode.AverageTime)
                .timeUnit(TimeUnit.MICROSECONDS)
                .warmupTime(TimeValue.seconds(1))
                .warmupIterations(5)
                .measurementTime(TimeValue.seconds(1))
                .measurementIterations(10)
                .threads(32)//contended
                .forks(1)
                .shouldFailOnError(true)
                .build();

        new Runner(opt).run();
    }


    @Benchmark
    public HierarchicalMockModel jaxbMoxy(MyState state) {
        return CachedJaxbXmlMapper.fromXML(CachedJaxbXmlMapper.toXMLString(state.model), state.model.getClass());
    }

    @Benchmark
    public HierarchicalMockModel jackson_Def(MyState state) throws JsonProcessingException {
        ObjectMapper mapper = new com.fasterxml.jackson.databind.json.JsonMapper().registerModule(new JavaTimeModule());
        String asJson = mapper.writeValueAsString(state.model);
        return mapper.readerFor(state.model.getClass()).readValue(asJson);
    }


    @Benchmark
    public HierarchicalMockModel jackson_Cust(MyState state) throws JsonProcessingException {
        ObjectMapper mapper = JsonMapper.builder().build();
        String asJson = mapper.writeValueAsString(state.model);
        return mapper.readerFor(state.model.getClass()).readValue(asJson);
    }


    @Benchmark
    public HierarchicalMockModel jackson_Cust_Typed_Cached_String(MyState state) throws IOException {
        var serialized = cachedCustTypedWriter.writeValueAsString(state.model);
        return cachedCustTypedReader.readValue(serialized, MODEL_CLASS);
    }

    @Benchmark
    public HierarchicalMockModel jackson_Cust_Typed_Cached_Bytes(MyState state) throws IOException {
        var serialized = cachedCustTypedWriter.writeValueAsBytes(state.model);
        return cachedCustTypedReader.readValue(serialized, MODEL_CLASS);
    }

    @Benchmark
    public HierarchicalMockModel jackson_Cust_Generic_Cached_Bytes(MyState state) throws IOException {
        var serialized = cachedCustGenericWriter.writeValueAsBytes(state.model);
        return cachedCustGenericReader.readValue(serialized, MODEL_CLASS);
    }


    @Benchmark
    public HierarchicalMockModel jackson_Cust_Typed_Cached_TokenBuffer(MyState state) throws IOException {
        TokenBuffer tokens = new TokenBuffer(cachedCustMapper, false);
        cachedCustTypedWriter.writeValue(tokens, state.model);
        return cachedCustTypedReader.readValue(tokens.asParser(), MODEL_CLASS);
    }


    @Benchmark
    public HierarchicalMockModel jackson_Cust_Generic_Cached_TokenBuffer(MyState state) throws IOException {
        TokenBuffer tokens = new TokenBuffer(cachedCustMapper, false);
        cachedCustGenericWriter.writeValue(tokens, state.model);
        return cachedCustGenericReader.readValue(tokens.asParser(), MODEL_CLASS);
    }


    @Benchmark
    public HierarchicalMockModel jackson_Def_Generic_Cached_TokenBuffer(MyState state) throws IOException {
        TokenBuffer tokens = new TokenBuffer(cachedDefMapper, false);
        cachedDefGenericWriter.writeValue(tokens, state.model);
        return cachedDefGenericReader.readValue(tokens.asParser(), MODEL_CLASS);
    }


    /**
     * A VERY CRUDE benchmark to provide a quick glance of coldstart/warmup effects.
     * <p>
     * For a realistic figures - use the numbers from JHM and respective "SingleShot"  Times or or AverageTime mode with long running benchmarks with sufficient warmups.
     *
     * @param totalIterations should be 10k to 100k as jvm will typically start compiling code with c2 after around 10k invocations
     * @param sampleInterval  should be around 5-10k, as jvm will typically start compiling code with c2 after around 10k invocations
     * @param c               the code to test
     * @param <V>             the generic result type
     * @throws Exception on error
     */
    private static <V> void simpleBench(int totalIterations, int sampleInterval, Callable<V> c) throws Exception {
        System.out.println("SimpleBenchmark for a quick glance of coldstart/warmup effects");
        System.out.println("--------------------------------");
        System.out.println("- First iteration: cold start lazy init time jackson");
        System.out.println("- Second to 5th iteration: jackson initialized - but code still cold");
        System.out.println("- >10k'th iteration: decently warm code (should be ~10% in line with what jmh produces");

        int v = Integer.MAX_VALUE;//largest prime - very simple blackhole to prevent JVM's dead code elimination
        for (int i = 0; i <= totalIterations; i++) {
            if (i < 5 || i % sampleInterval == 0) {
                System.out.print(i);
                System.out.print(": ");
                v ^= timed(c).hashCode();
            } else {
                v ^= c.call().hashCode();
            }
        }
        System.out.println("Ignore me: " + v);
    }

    @lombok.SneakyThrows
    private static <V> V timed(Callable<V> c) {
        Instant now = Instant.now();
        V v = c.call();
        System.out.println(Duration.between(now, Instant.now()));
        return v;
    }

}
