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
import io.protostuff.*;
import io.protostuff.runtime.*;
import lombok.*;
import org.apache.commons.lang3.*;
import org.jetbrains.annotations.*;
import org.mapstruct.*;
import org.mapstruct.control.*;
import org.mapstruct.factory.*;
import org.modelmapper.*;
import org.modelmapper.module.jsr310.*;
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

// Results:
//@ JMH version: 1.2, JDK 15.0.2, OpenJDK 64-Bit Server VM 15.0.2+7, MacOS  Intel i7-6700HQ@2,7GHz
/*--
Q: Why would it want a deep copy?
A: e.g. for a cache, or for defensive programming (e.g. a in-memory model layer that shares a mutable copy to be used by downstream layers)

Q: what is the fastest (more most feasible) way of creating a DeepCopy of a provided Object or Object graph?
A:Undoubtedly, writing hand crafted copy constructors for all your model files is fastest.
However, that is sometimes overkill, not maintainable or simply impossible due to classes outside your control.
In such cases a generic tool is wanted.
Some of the analysed tools are pure deepCopiers but most of them are some kind of generic serialization/deserialization libraries.
Many of the evaluated tools work quite well or "good-enough", so we can take into account which dependencies we want (or are allowed to) use to do the job.
Read the results!

Q: are these all the tools?
A: hell no, aint nobody got time for that. They are the fastest, most common or easiest to work with i found. Feel free to contribute.

WARNING: keep in mind that
- you clone tool is most likely to be used in a concurrent context, and not all of them are thread safe or event concurrent
- threadSafe != concurrent - the former only ensure nothing breaks (and could potentially use a global lock) while the second may also consider performance implications
- sometimes just allocating on demand is faster than caching or threadLocal caching - ThreadLocal is not the holy grail
- data matters! its a huge difference if you serialize deeply nested big graphs, or just a single simple pojo!
=> Adapt this benchmark to your case - preferably switch the HierarchicalMockModel to a model you use in production!!


Conclusion:
- Handcrafted copy constructor is the fastest but the most complicated and the most error prone
- Kryo is sometimes a pain to set up, but delivers amazing results
- protobuf is a nice candidate and easy to make a maintainable generic deepClone util out of. Especially if you program is already doing protobuf. Protostuff is the best protobuf implementation (IMHO)
- jackson databind does reasonably well, and is readily available in most projects - use the intermediate "TokenBuffer" instead of to/from json. Easy and fast.
- If you want a dedicated cloning library that "just works" -> kostaskougios CloningLib is a solid choice. But wrap it in a ThreadLocal as it suffers from thread contention.
- stuck in legacy land? apache commonsLang SerializationUtils might help. However, it relies on java Serialization interface and thus requires ALL your model files to implement Serializable
- never ever use gson or jaxb/moxy



Uncontended results (forks 1, threads 1) - lower is better
----------------------------------------
Benchmark                                                      Mode  Cnt     Score      Error  Units
DeepCopyJMH.copyConstructor                                   avgt 10 13,102   ± 2,016    us/op
DeepCopyJMH.kryo_cached                                       avgt 10 149,329  ± 13,284   us/op
DeepCopyJMH.protostuff_static                                 avgt 10 544,208  ± 67,374   us/op
DeepCopyJMH.protostuff_copy_reuse_runtimeschema_buffer        avgt 10 581,715  ± 90,344   us/op
DeepCopyJMH.protostuff_copy_reuse_TL_schema_buffer            avgt 10 596,357  ± 173,892  us/op
DeepCopyJMH.protostuff_cachedSchema                           avgt 10 623,088  ± 113,602  us/op
DeepCopyJMH.jackson_cachedThreadLocal_generic_toTokenBuffer   avgt 10 664,305  ± 40,786   us/op
DeepCopyJMH.jackson_cached_typed_toTokenBuffer                avgt 10 668,782  ± 55,507   us/op
DeepCopyJMH.jackson_cached_generic_toTokenBuffer              avgt 10 671,478  ± 41,879   us/op
DeepCopyJMH.jackson_cached2_generic_toTokenBuffer             avgt 10 703,605  ± 111,487  us/op
DeepCopyJMH.kostaskougiosCloningLib_threadLocal               avgt 10 706,779  ± 44,579   us/op
DeepCopyJMH.kostaskougiosCloningLib_cached                    avgt 10 803,779  ± 198,025  us/op
DeepCopyJMH.jackson_threadLocalEverything_typed_toTokenBuffer avgt 10 912,996  ± 381,484  us/op
DeepCopyJMH.commonsLangClone                                  avgt 10 1170,309 ± 44,064   us/op
DeepCopyJMH.jackson_cached_generic_toBytes                    avgt 10 1230,111 ± 74,252   us/op
DeepCopyJMH.jackson_cached_typed_toBytes                      avgt 10 1272,306 ± 139,190  us/op
DeepCopyJMH.jackson_cached_typed_toString                     avgt 10 1379,892 ± 268,460  us/op
DeepCopyJMH.springSerialize                                   avgt 10 1402,167 ± 245,269  us/op
DeepCopyJMH.jackson_new_typed_toString                        avgt 10 1738,089 ± 310,950  us/op
DeepCopyJMH.jackson_new_generic_toString                      avgt 10 1863,538 ± 770,218  us/op
DeepCopyJMH.jackson_new2_typed_toString                       avgt 10 2165,512 ± 891,361  us/op
DeepCopyJMH.modelMapper_threadLocal                           avgt 10 3420,660 ± 510,550  us/op
DeepCopyJMH.modelMapper_cached                                avgt 10 3918,275 ± 961,079  us/op
DeepCopyJMH.gson_threadLocal                                  avgt 10 4878,577 ± 199,474  us/op
DeepCopyJMH.gson_cached                                       avgt 10 4992,306 ± 449,850  us/op
DeepCopyJMH.jaxbMoxy_cached                                   avgt 10 8709,603 ± 2726,208 us/op


Contended results (forks 1, threads 32) - lower is better
----------------------------------------
Benchmark                                                      Mode  Cnt       Score       Error  Units
DeepCopyJMH.copyConstructor                                   avgt 10 148,843    ± 20,652    us/op
DeepCopyJMH.kryo_cached                                       avgt 10 1326,178   ± 232,830   us/op
DeepCopyJMH.protostuff_static                                 avgt 10 5115,453   ± 231,857   us/op
DeepCopyJMH.kostaskougiosCloningLib_threadLocal               avgt 10 5468,118   ± 903,604   us/op
DeepCopyJMH.protostuff_copy_reuse_runtimeschema_buffer        avgt 10 5873,032   ± 1146,258  us/op
DeepCopyJMH.kostaskougiosCloningLib_cached                    avgt 10 6101,636   ± 906,655   us/op
DeepCopyJMH.protostuff_copy_reuse_TL_schema_buffer            avgt 10 6257,055   ± 1465,978  us/op
DeepCopyJMH.jackson_cached_generic_toTokenBuffer              avgt 10 6416,757   ± 626,858   us/op
DeepCopyJMH.jackson_cachedThreadLocal_generic_toTokenBuffer   avgt 10 6825,192   ± 1398,428  us/op
DeepCopyJMH.protostuff_cachedSchema                           avgt 10 7092,253   ± 1729,510  us/op
DeepCopyJMH.jackson_threadLocalEverything_typed_toTokenBuffer avgt 10 7115,618   ± 1489,128  us/op
DeepCopyJMH.springSerialize                                   avgt 10 9206,732   ± 1015,981  us/op
DeepCopyJMH.commonsLangClone                                  avgt 10 9552,800   ± 2292,853  us/op
DeepCopyJMH.jackson_cached_generic_toBytes                    avgt 10 11375,475  ± 2187,300  us/op
DeepCopyJMH.jackson_cached_typed_toTokenBuffer                avgt 10 12486,088  ± 3082,981  us/op
DeepCopyJMH.jackson_cached_typed_toBytes                      avgt 10 12805,771  ± 2048,417  us/op
DeepCopyJMH.jackson_new2_typed_toString                       avgt 10 14663,639  ± 2838,194  us/op
DeepCopyJMH.jackson_cached2_generic_toTokenBuffer             avgt 10 15606,528  ± 9205,637  us/op
DeepCopyJMH.jackson_cached_typed_toString                     avgt 10 17733,439  ± 7297,026  us/op
DeepCopyJMH.jackson_new_typed_toString                        avgt 10 21040,426  ± 10049,313 us/op
DeepCopyJMH.jackson_new_generic_toString                      avgt 10 21080,263  ± 10247,715 us/op
DeepCopyJMH.modelMapper_cached                                avgt 10 26129,203  ± 4693,008  us/op
DeepCopyJMH.modelMapper_threadLocal                           avgt 10 42265,292  ± 33937,149 us/op
DeepCopyJMH.gson_cached                                       avgt 10 46997,629  ± 7301,741  us/op
DeepCopyJMH.jaxbMoxy_cached                                   avgt 10 75382,409  ± 11357,673 us/op
DeepCopyJMH.gson_threadLocal                                  avgt 10 153278,350 ± 80752,593 us/op





*/
@State(Scope.Benchmark)
public class DeepCopyJMH {

    static final Class<HierarchicalMockModel> MODEL_CLASS = HierarchicalMockModel.class;

    //The model under test
    @State(Scope.Thread)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MyState {
        HierarchicalMockModel model = HierarchicalMockModel.newInstance(10, 10, 10);
    }


    // https://github.com/google/gson
    Gson gson = new Gson();
    ThreadLocal<Gson> threadLocalGson = ThreadLocal.withInitial(Gson::new);

    // https://github.com/kostaskougios/cloning
    Cloner kostaskougiosCloningLib = new Cloner();
    ThreadLocal<Cloner> threadLocalKostaskougiosCloningLib = ThreadLocal.withInitial(Cloner::new);

    // http://modelmapper.org/
    ModelMapper modelMapper = getModelMapper();
    ThreadLocal<ModelMapper> threadLocalModelMapper = ThreadLocal.withInitial(DeepCopyJMH::getModelMapper);

    @NotNull
    private static ModelMapper getModelMapper() {
        var mm = new ModelMapper();
        mm.registerModule(new Jsr310Module());
        mm.getConfiguration().setDeepCopyEnabled(true);
        return mm;
    }


    //kryo is not thread safe https://github.com/EsotericSoftware/kryo#thread-safety
    ThreadLocal<Kryo> kryo = ThreadLocal.withInitial(DeepCopyJMH::newKryo);

    private static Kryo newKryo() {
        Kryo kryo = new Kryo();
        kryo.setRegistrationRequired(false); //serialize EVERYTHING
        //kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());

        //fix npe Cannot read the array length because "elementData" is null
        kryo.register(Arrays.asList("").getClass(), new ArraysAsListSerializer());

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


    // https://mapstruct.org/
    @Mapper(mappingControl = DeepClone.class)
    public interface MapStructCloneMapper {
        MapStructCloneMapper INSTANCE = Mappers.getMapper(MapStructCloneMapper.class);

        HierarchicalMockModel clone(HierarchicalMockModel model);
    }

    // com.fasterxml.jackson.databind
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

    ProtostuffDeepCopy<HierarchicalMockModel> protostuffDeepCopy =
            new ProtostuffDeepCopy<>(HierarchicalMockModel.class);


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

    //==========================================
    // Benchmarks
    //==========================================


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

    @Benchmark
    public HierarchicalMockModel protostuff_static(MyState state) {
        return ProtostuffDeepCopy.copy(state.model);
    }

    @Benchmark
    public HierarchicalMockModel protostuff_cachedSchema(MyState state) {
        return protostuffDeepCopy.copy_cachedSchema_newBuffer(state.model);
    }

    @Benchmark
    public HierarchicalMockModel protostuff_copy_reuse_TL_schema_buffer(MyState state) {
        return protostuffDeepCopy.copy_threadLocalSchemaAndBuffer(state.model);
    }

    @Benchmark
    public HierarchicalMockModel protostuff_copy_reuse_runtimeschema_buffer(MyState state) {
        return protostuffDeepCopy.copy_cachedRuntimeSchema_cachedBuffer(state.model);
    }

    //==========================================
    // Support stuff
    //==========================================

    public static void main(String[] args) throws Exception {
        DeepCopyJMH bench = new DeepCopyJMH();

        //test all impls if they produces the same result as the original model
        bench.verifyImplementationCorrectness();

        //String winnerMethod = "jackson_cached_genericWithTime_toTokenBuffer";
        //var state = new MyState(HierarchicalMockModel.newInstance(2, 2, 3));
        //simpleBench(15_000, 1000, () -> bench.jackson_cached_generic_toTokenBuffer(state));

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
        var modified = HierarchicalMockModel.newInstance("TAMPERED_", 2, 3, 1).getSubTypes();
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
                              .withFailMessage(
                                      "Detected shared reference of mutable instance! '%s' produces wrong result. Expecting:\n"
                                      + " %s%n"
                                      + "to be NOT equal to:%n"
                                      + " %s%n", m.getName(), state.model, result)
                              .isNotEqualTo(state.model);
                      //revert modification
                      state.model.setSubTypes(bak);

                      //check if list contents are all deep clones
                      var bak2 = state.model.getSubTypes().get(0);
                      state.model.getSubTypes().set(0, modified.get(0));
                      assertThat(result)
                              .withFailMessage(
                                      "Detected shared reference of mutable instance! '%s' produces wrong result. Expecting:\n"
                                      + " %s%n"
                                      + "to be NOT equal to:%n"
                                      + " %s%n", m.getName(), state.model, result)
                              .isNotEqualTo(state.model);
                      //revert modification
                      state.model.getSubTypes().set(0, bak2);

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


    public static class ProtostuffDeepCopy<T> {
        private final Schema<T> schema;
        private final RuntimeSchema<T> sharedRTSchema;

        ThreadLocal<CachedStuff<T>> tlCache;

        @Value
        private static class CachedStuff<T> {
            Schema schema;
            LinkedBuffer buffer;
        }


        public ProtostuffDeepCopy(Class<T> t) {
            this.schema = RuntimeSchema.getSchema(t);
            this.sharedRTSchema = RuntimeSchema.createFrom(t);

            this.tlCache = ThreadLocal
                    .withInitial(() -> new CachedStuff<T>(RuntimeSchema.getSchema(t), LinkedBuffer.allocate(1024)));
        }


        public T copy_cachedSchema_newBuffer(T t) {
            LinkedBuffer buffer = LinkedBuffer.allocate(1024);
            return copy(t, schema, buffer);
        }


        public T copy_cachedRuntimeSchema_cachedBuffer(T t) {
            LinkedBuffer buffer = LinkedBuffer.allocate(1024);
            return copy(t, sharedRTSchema, buffer);
        }

        public T copy_threadLocalSchemaAndBuffer(T t) {
            var cache = tlCache.get();
            final Schema<T> schema = cache.getSchema();
            final LinkedBuffer buffer = cache.getBuffer();
            return copy(t, schema, buffer);
        }

        public static <V> V copy(V t) {
            Schema<V> schema = (Schema<V>) RuntimeSchema.getSchema(t.getClass());
            // Re-use (manage) this buffer to avoid allocating on every serialization
            LinkedBuffer buffer = LinkedBuffer.allocate(1024);
            return copy(t, schema, buffer);
        }

        private static <T> T copy(T t, Schema<T> schema, LinkedBuffer buffer) {
            // ser
            final byte[] serialized;
            try {
                serialized = ProtostuffIOUtil.toByteArray(t, schema, buffer);
            } finally {
                buffer.clear();
            }
            // deser
            T parsed = schema.newMessage();
            ProtostuffIOUtil.mergeFrom(serialized, parsed, schema);
            return parsed;
        }
    }
}
