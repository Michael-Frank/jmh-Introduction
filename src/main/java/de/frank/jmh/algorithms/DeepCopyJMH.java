package de.frank.jmh.algorithms;

import com.esotericsoftware.kryo.*;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.*;
import com.fasterxml.jackson.databind.util.*;
import com.fasterxml.jackson.datatype.jsr310.*;
import com.google.gson.*;
import com.rits.cloning.*;
import com.yevdo.jwildcard.*;
import de.frank.impl.jaxb.*;
import de.frank.jmh.model.*;
import de.javakaffee.kryoserializers.*;
import lombok.*;
import org.apache.commons.lang3.*;
import org.mapstruct.*;
import org.mapstruct.control.*;
import org.mapstruct.factory.*;
import org.modelmapper.*;
import org.objenesis.strategy.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.results.*;
import org.openjdk.jmh.results.format.*;
import org.openjdk.jmh.runner.*;
import org.openjdk.jmh.runner.options.*;

import java.io.*;
import java.nio.charset.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;

/*--


Uncontended results (forks 1, threads 1)
----------------------------------------
Benchmark                                                       Mode  Cnt     Score     Error  Units
DeepCopyJMH.commonsLangClone                                   avgt   10  1424,957 ±  27,038  us/op
DeepCopyJMH.copyConstructor                                    avgt   10    15,745 ±   0,170  us/op
DeepCopyJMH.gson_cached                                        avgt   10  4901,697 ±  91,668  us/op
DeepCopyJMH.gson_threadLocal                                   avgt   10  4999,078 ± 373,655  us/op
DeepCopyJMH.jackson_cached2_generic_toTokenBuffer              avgt   10   587,379 ±  20,881  us/op
DeepCopyJMH.jackson_cachedThreadLocal_generic_toTokenBuffer    avgt   10   572,950 ±  18,529  us/op
DeepCopyJMH.jackson_cached_generic_toBytes                     avgt   10  1248,203 ±  13,937  us/op
DeepCopyJMH.jackson_cached_generic_toTokenBuffer               avgt   10   569,766 ±  29,556  us/op
DeepCopyJMH.jackson_cached_typed_toBytes                       avgt   10  1192,116 ±  14,194  us/op
DeepCopyJMH.jackson_cached_typed_toString                      avgt   10  1328,386 ±  12,972  us/op
DeepCopyJMH.jackson_cached_typed_toTokenBuffer                 avgt   10   576,335 ±   9,028  us/op
DeepCopyJMH.jackson_new2_typed_toString                        avgt   10  1624,295 ±  91,926  us/op
DeepCopyJMH.jackson_new_generic_toString                       avgt   10  1596,581 ±  34,901  us/op
DeepCopyJMH.jackson_new_typed_toString                         avgt   10  1606,599 ±  52,431  us/op
DeepCopyJMH.jackson_threadLocalEverything_typed_toTokenBuffer  avgt   10   603,741 ±  16,318  us/op
DeepCopyJMH.jaxbMoxy_cached                                    avgt   10  7630,968 ± 172,118  us/op
DeepCopyJMH.kostaskougiosCloningLib_cached                     avgt   10   684,948 ±  23,559  us/op
DeepCopyJMH.kostaskougiosCloningLib_threadLocal                avgt   10   684,416 ±  11,755  us/op
DeepCopyJMH.kryo_cached                                        avgt   10   156,190 ±  17,673  us/op
DeepCopyJMH.modelMapper_cached                                 avgt   10     0,102 ±   0,001  us/op
DeepCopyJMH.modelMapper_threadLocal                            avgt   10     0,098 ±   0,001  us/op
DeepCopyJMH.springSerialize                                    avgt   10  1360,370 ±  16,726  us/op

Contended results (forks 1, threads 32)
----------------------------------------
Benchmark                                                       Mode  Cnt      Score       Error  Units
DeepCopyJMH.copyConstructor                                    avgt   10    199,353 ±     5,579  us/op
DeepCopyJMH.jackson_cached_typed_toTokenBuffer                 avgt   10   5634,017 ±   219,688  us/op
DeepCopyJMH.jackson_cached2_generic_toTokenBuffer              avgt   10   5675,313 ±   111,679  us/op
DeepCopyJMH.jackson_cached_generic_toTokenBuffer               avgt   10   6062,692 ±   316,370  us/op
DeepCopyJMH.jackson_cachedThreadLocal_generic_toTokenBuffer    avgt   10   5812,336 ±   228,990  us/op
DeepCopyJMH.jackson_threadLocalEverything_typed_toTokenBuffer  avgt   10   6165,216 ±   420,225  us/op
DeepCopyJMH.jackson_cached_generic_toBytes                     avgt   10  10542,001 ±   338,734  us/op
DeepCopyJMH.jackson_cached_typed_toBytes                       avgt   10  10776,638 ±   182,298  us/op
DeepCopyJMH.jackson_cached_typed_toString                      avgt   10  10999,531 ±   440,923  us/op
DeepCopyJMH.jackson_new2_typed_toString                        avgt   10  24647,483 ±  4457,954  us/op
DeepCopyJMH.jackson_new_generic_toString                       avgt   10  29448,229 ± 14704,570  us/op
DeepCopyJMH.jackson_new_typed_toString                         avgt   10  20355,571 ±  7231,071  us/op
DeepCopyJMH.commonsLangClone                                   avgt   10  10557,899 ±   984,620  us/op
DeepCopyJMH.gson_cached                                        avgt   10  36897,026 ±  2286,447  us/op
DeepCopyJMH.gson_threadLocal                                   avgt   10  35640,850 ±  2265,355  us/op
DeepCopyJMH.jaxbMoxy_cached                                    avgt   10  66656,486 ±  2086,503  us/op
DeepCopyJMH.kryo_cached                                        avgt   10   1195,145 ±    67,907  us/op
DeepCopyJMH.kostaskougiosCloningLib_cached                     avgt   10   5170,461 ±   155,091  us/op
DeepCopyJMH.kostaskougiosCloningLib_threadLocal                avgt   10   5341,027 ±   280,713  us/op
DeepCopyJMH.modelMapper_cached                                 avgt   10      1,547 ±     0,011  us/op
DeepCopyJMH.modelMapper_threadLocal                            avgt   10      1,593 ±     0,020  us/op
DeepCopyJMH.springSerialize                                    avgt   10  11236,535 ±  1207,929  us/op

     */
@State(Scope.Benchmark)
public class DeepCopyJMH {
    static final Class<HierarchicalMockModel> MODEL_CLASS = HierarchicalMockModel.class;

    @State(Scope.Thread)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MyState {
        HierarchicalMockModel model = HierarchicalMockModel.newInstance(10, 10, 10);
    }

    ObjectMapper cachedGenericMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
    ObjectReader cachedGenericReader = cachedGenericMapper.reader();
    ObjectWriter cachedGenericWriter = cachedGenericMapper.writer();
    ObjectReader cachedTypedReader = cachedGenericMapper.readerFor(MODEL_CLASS);
    ObjectWriter cachedTypedWriter = cachedGenericMapper.writerFor(MODEL_CLASS);


    ObjectMapper cached2Mapper = new JsonMapper().registerModule(new JavaTimeModule());
    ObjectReader cached2TypedReader = cached2Mapper.readerFor(MODEL_CLASS);
    ObjectWriter cached2TypedWriter = cached2Mapper.writerFor(MODEL_CLASS);


    ThreadLocal<ObjectReader> threadLocalReader =
            ThreadLocal.withInitial(() -> cachedGenericMapper.readerFor(MODEL_CLASS));
    ThreadLocal<ObjectWriter> threadLocalWriter =
            ThreadLocal.withInitial(() -> cachedGenericMapper.writerFor(MODEL_CLASS));

    ThreadLocal<ObjectMapperInstances> threadLocalEverything =
            ThreadLocal.withInitial(() -> new ObjectMapperInstances(MODEL_CLASS));

    Gson gson = new Gson();
    ThreadLocal<Gson> threadLocalGson = ThreadLocal.withInitial(Gson::new);

    Cloner kostaskougiosCloningLib = new Cloner();
    ThreadLocal<Cloner> threadLocalKostaskougiosCloningLib = ThreadLocal.withInitial(Cloner::new);

    ModelMapper modelMapper = new ModelMapper();
    ThreadLocal<ModelMapper> threadLocalModelMapper = ThreadLocal.withInitial(ModelMapper::new);

    //kryo is not thread safe https://github.com/EsotericSoftware/kryo#thread-safety
    ThreadLocal<Kryo> kryo = ThreadLocal.withInitial(DeepCopyJMH::newKryo);

    private static Kryo newKryo() {
        Kryo kryo = new Kryo();
        kryo.setRegistrationRequired(false); //serialize EVERYTHING
        //kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());

        //fix npe Cannot read the array length because "elementData" is null
        kryo.register( Arrays.asList( "" ).getClass(), new ArraysAsListSerializer() );

        kryo.register(Collections.EMPTY_LIST.getClass(), new CollectionsEmptyListSerializer());
        kryo.register(Collections.EMPTY_MAP.getClass(), new CollectionsEmptyMapSerializer());
        kryo.register(Collections.EMPTY_SET.getClass(), new CollectionsEmptySetSerializer());
        kryo.register(Collections.singletonList("").getClass(), new CollectionsSingletonListSerializer());
        kryo.register(Collections.singleton("").getClass(), new CollectionsSingletonSetSerializer());
        kryo.register(Collections.singletonMap("", "").getClass(), new CollectionsSingletonMapSerializer());
        UnmodifiableCollectionsSerializer.registerSerializers(kryo);
        SynchronizedCollectionsSerializer.registerSerializers(kryo);
//        kryo.register( GregorianCalendar.class, new GregorianCalendarSerializer() );
//        kryo.register( InvocationHandler.class, new JdkProxySerializer() );
        return kryo;
    }


    @Mapper(mappingControl = DeepClone.class)
    public interface MapStructCloneMapper {
        MapStructCloneMapper INSTANCE = Mappers.getMapper(MapStructCloneMapper.class);

        HierarchicalMockModel clone(HierarchicalMockModel model);
    }

    @Getter
    private static class ObjectMapperInstances {
        private final ObjectMapper mapper;
        private final ObjectReader reader;
        private final ObjectWriter writer;

        public ObjectMapperInstances(Class z) {
            this.mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
            this.reader = mapper.readerFor(z);
            this.writer = mapper.writerFor(z);
        }
    }


    @Benchmark
    public HierarchicalMockModel copyConstructor(MyState state) {
        //HINT: deep-copy! constructors are a pain to get working with complex models and a pain to keep them functioning over the life time of a project
        //lists and maps are a pain, and if the elements in the lists use any inheritance and are mutable it gets weird fast.
        return new HierarchicalMockModel(state.model);
    }

    @Benchmark
    public HierarchicalMockModel commonsLangClone(MyState state) {
        //requires all model classes to implement Serializable and uses javas ObjectOutputStream
        return SerializationUtils.clone(state.model);
    }

    @Benchmark
    public HierarchicalMockModel springSerialize(MyState state) {
        //requires all model classes to implement Serializable and uses javas ObjectOutputStream
        return (HierarchicalMockModel) org.springframework.util.SerializationUtils
                .deserialize(org.springframework.util.SerializationUtils.serialize(state.model));
    }

//    @Benchmark
//    public HierarchicalMockModel mapstruct(MyState state) {
//        //Annotation processor based generation of a "bean to bean" mapper. In this case src and target bean are the same class.
//
//        //TODO: currently has issues serializing the date stuff
//        return MapStructCloneMapper.INSTANCE.clone(state.model);
//    }

    @Benchmark
    public HierarchicalMockModel modelMapper_cached(MyState state) {
        return modelMapper.map(state.model, MODEL_CLASS);
    }

    @Benchmark
    public HierarchicalMockModel modelMapper_threadLocal(MyState state) {
        return threadLocalModelMapper.get().map(state.model, MODEL_CLASS);
    }

    @Benchmark
    public HierarchicalMockModel kostaskougiosCloningLib_cached(MyState state) {
        //reflection based
        return kostaskougiosCloningLib.deepClone(state.model);
    }

    @Benchmark
    public HierarchicalMockModel kostaskougiosCloningLib_threadLocal(MyState state) {
        //reflection based
        return threadLocalKostaskougiosCloningLib.get().deepClone(state.model);
    }

    @Benchmark
    public HierarchicalMockModel kryo_cached(MyState state) {
        return kryo.get().copy(state.model);
    }

    @Benchmark
    public HierarchicalMockModel gson_cached(MyState state) {
        return gson.fromJson(gson.toJson(state.model), MODEL_CLASS);
    }

    @Benchmark
    public HierarchicalMockModel gson_threadLocal(MyState state) {
        var lgson = threadLocalGson.get();
        return lgson.fromJson(lgson.toJson(state.model), MODEL_CLASS);
    }

    @Benchmark
    public HierarchicalMockModel jaxbMoxy_cached(MyState state) {
        //no point in benchmarking un-cached moxy, its even slower than the cached one
        return CachedJaxbXmlMapper.fromXML(CachedJaxbXmlMapper.toXMLString(state.model), state.model.getClass());
    }


    @Benchmark
    public HierarchicalMockModel jackson_new_generic_toString(MyState state) throws IOException {
        ObjectMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
        String asJson = mapper.writeValueAsString(state.model);
        return mapper.reader().readValue(asJson, state.model.getClass());
    }

    @Benchmark
    public HierarchicalMockModel jackson_new_typed_toString(MyState state) throws JsonProcessingException {
        ObjectMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
        String asJson = mapper.writerFor(MODEL_CLASS).writeValueAsString(state.model);
        return mapper.readerFor(state.model.getClass()).readValue(asJson);
    }

    @Benchmark
    public HierarchicalMockModel jackson_new2_typed_toString(MyState state) throws JsonProcessingException {
        //should not! make any difference compared to JsonMapper.builder().addModule(new JavaTimeModule()).build();
        ObjectMapper mapper = new JsonMapper().registerModule(new JavaTimeModule());
        String asJson = mapper.writerFor(MODEL_CLASS).writeValueAsString(state.model);
        return mapper.readerFor(state.model.getClass()).readValue(asJson);
    }

    @Benchmark
    public HierarchicalMockModel jackson_cached2_generic_toTokenBuffer(MyState state) throws IOException {
        TokenBuffer tokens = new TokenBuffer(cached2Mapper, false);
        cached2TypedWriter.writeValue(tokens, state.model);
        return cached2TypedReader.readValue(tokens.asParser(), MODEL_CLASS);
    }

    @Benchmark
    public HierarchicalMockModel jackson_cached_generic_toTokenBuffer(MyState state) throws IOException {
        TokenBuffer tokens = new TokenBuffer(cachedGenericMapper, false);
        cachedGenericWriter.writeValue(tokens, state.model);
        return cachedGenericReader.readValue(tokens.asParser(), MODEL_CLASS);
    }


    @Benchmark
    public HierarchicalMockModel jackson_cached_generic_toBytes(MyState state) throws IOException {
        var serialized = cachedGenericWriter.writeValueAsBytes(state.model);
        return cachedGenericReader.readValue(serialized, MODEL_CLASS);
    }


    @Benchmark
    public HierarchicalMockModel jackson_cached_typed_toString(MyState state) throws IOException {
        var serialized = cachedTypedWriter.writeValueAsString(state.model);
        return cachedTypedReader.readValue(serialized, MODEL_CLASS);
    }

    @Benchmark
    public HierarchicalMockModel jackson_cached_typed_toBytes(MyState state) throws IOException {
        var serialized = cachedTypedWriter.writeValueAsBytes(state.model);
        return cachedTypedReader.readValue(serialized, MODEL_CLASS);
    }


    @Benchmark
    public HierarchicalMockModel jackson_cached_typed_toTokenBuffer(MyState state) throws IOException {
        TokenBuffer tokens = new TokenBuffer(cachedGenericMapper, false);
        cachedTypedWriter.writeValue(tokens, state.model);
        return cachedTypedReader.readValue(tokens.asParser(), MODEL_CLASS);
    }

    @Benchmark
    public HierarchicalMockModel jackson_cachedThreadLocal_generic_toTokenBuffer(MyState state) throws IOException {
        TokenBuffer tokens = new TokenBuffer(cachedGenericMapper, false);
        threadLocalWriter.get().writeValue(tokens, state.model);
        return threadLocalReader.get().readValue(tokens.asParser(), MODEL_CLASS);
    }

    @Benchmark
    public HierarchicalMockModel jackson_threadLocalEverything_typed_toTokenBuffer(MyState state) throws IOException {
        var local = threadLocalEverything.get();
        TokenBuffer tokens = new TokenBuffer(local.getMapper(), false);
        local.getWriter().writeValue(tokens, state.model);
        return local.getReader().readValue(tokens.asParser(), MODEL_CLASS);
    }


    public static void main(String[] args) throws Exception {
        DeepCopyJMH bench = new DeepCopyJMH();
        //test all impls if they produces the same result as the original model
        bench.verifyImplementationCorrectness();

        //quick overview to filter out extremely bad candidates
        var state = new MyState(HierarchicalMockModel.newInstance(2, 2, 3));
        simpleBench(15_000, 1000, () -> bench.jackson_cached_generic_toTokenBuffer(state));

        String winnerMethod = "jackson_cached_genericWithTime_toTokenBuffer";
        var uncontenedResult = bench.benchmarkAllDeepCopyApproaches(1, 1);
        var contendResult = bench.benchmarkAllDeepCopyApproaches(1, 32);
        //var winnerInDepthResult = bench.benchmarkWinnerMoreDeeply(winnerMethod);

        //format results
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             PrintStream out = new PrintStream(outputStream, true, StandardCharsets.UTF_8)) {
            var resultFormatter = ResultFormatFactory.getInstance(ResultFormatType.TEXT, out);
            out.println("SUMMARY");
            out.println("Uncontended results (forks 1, threads 1)");
            out.println("----------------------------------------");
            resultFormatter.writeOut(uncontenedResult);
            out.println();
            out.println("Contended results (forks 1, threads 32)");
            out.println("----------------------------------------");
            resultFormatter.writeOut(contendResult);
            out.println();
            //out.println("Benchmark winner in depth: " + winnerMethod);
            //out.println("----------------------------------------");
            //resultFormatter.writeOut(winnerInDepthResult);
            System.out.println(outputStream.toString(StandardCharsets.UTF_8));
        }
    }

    public Collection<RunResult> benchmarkWinnerMoreDeeply(String winnerMethod) throws Exception {
        //test impls if deep copy produces the same result as the original model
        var state = new MyState();
        assertThat(jackson_cached_generic_toTokenBuffer(state)).isEqualTo(state.model);

        String include = JWildcard.wildcardToRegex(this.getClass().getName() + "." + winnerMethod, true);
        System.out.println(include);
        Options opt = new OptionsBuilder()
                .include(include)
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

        return new Runner(opt).run();
    }


    public Collection<RunResult> benchmarkAllDeepCopyApproaches(int forks, int threads) throws Exception {
        String include = JWildcard.wildcardToRegex(this.getClass().getName() + ".*");

        var opt = new OptionsBuilder()
                .include(include)
                .mode(Mode.AverageTime)
                .timeUnit(TimeUnit.MICROSECONDS)
                .warmupTime(TimeValue.seconds(1))
                .warmupIterations(5)
                .measurementTime(TimeValue.seconds(1))
                .measurementIterations(10)
                .threads(threads)//contended
                .forks(forks)
                .shouldFailOnError(true);

        return new Runner(opt.build()).run();
    }

    private void verifyImplementationCorrectness() {
        var state = new MyState(HierarchicalMockModel.newInstance(2, 2, 2));
        var modified = HierarchicalMockModel.newInstance("TAMPERED_",2, 3, 1).getSubTypes();
        Arrays.stream(this.getClass().getDeclaredMethods())
              .filter(m -> m.isAnnotationPresent(Benchmark.class))
              //.filter(m -> m.getName().startsWith(prefix))
              .forEach(m -> {
                  try {
                      var result = m.invoke(this, state);
                      assertThat(result)
                              .withFailMessage("Implementation '%s' produces wrong result. Expecting:\n"
                                               + " %s%n"
                                               + "to be equal to:%n"
                                               + " %s%n", m.getName(), state.model, result)
                              .isEqualTo(state.model);
                      System.out.println("Implementation produces expected results: " + m.getName());

                      //check if list reference is shared
                      var bak = state.model.getSubTypes();
                      state.model.setSubTypes(modified);
                      assertThat(result)
                              .withFailMessage("Detected shared reference of mutable instance! '%s' produces wrong result. Expecting:\n"
                                               + " %s%n"
                                               + "to be NOT equal to:%n"
                                               + " %s%n", m.getName(), state.model, result)
                              .isNotEqualTo(state.model);
                      //revert modification
                      state.model.setSubTypes(bak);

                      //check if list contents are all deep clones
                      var bak2 = state.model.getSubTypes().get(0);
                      state.model.getSubTypes().set(0,modified.get(0));
                      assertThat(result)
                              .withFailMessage("Detected shared reference of mutable instance! '%s' produces wrong result. Expecting:\n"
                                               + " %s%n"
                                               + "to be NOT equal to:%n"
                                               + " %s%n", m.getName(), state.model, result)
                              .isNotEqualTo(state.model);
                      //revert modification
                      state.model.getSubTypes().set(0,bak2);

                  } catch (ReflectiveOperationException e) {
                      throw new RuntimeException(e);
                  }
              });
        System.out.println("All Implementations are OK and produces expected results.");
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
        System.out.println("- >10k'th iteration: decently warm code (should be ~10% in line with what jmh produces)");

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
