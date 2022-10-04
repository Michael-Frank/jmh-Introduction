package de.frank.jmh.algorithms;

import com.esotericsoftware.kryo.Kryo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.Gson;
import com.rits.cloning.Cloner;
import com.yevdo.jwildcard.JWildcard;
import de.frank.impl.jaxb.CachedJaxbXmlMapper;
import de.frank.jmh.model.HierarchicalMockModel;
import de.javakaffee.kryoserializers.*;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Value;
import org.apache.commons.lang3.SerializationUtils;
import org.jetbrains.annotations.NotNull;
import org.mapstruct.Mapper;
import org.mapstruct.control.DeepClone;
import org.mapstruct.factory.Mappers;
import org.modelmapper.ModelMapper;
import org.modelmapper.module.jsr310.Jsr310Module;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormatFactory;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

// Results:
/*--
Q: How do i run this benchmark?
A: please run the main method included in this class. It will configure and run a battery of tests.

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
@ JMH version: 1.28, JDK 15.0.2, OpenJDK 64-Bit Server VM 15.0.2+7, MacOS  Intel i7-6700HQ@2,7GHz
----------------------------------------
Uncontended results (forks 1, threads 1)
----------------------------------------
Benchmark                                                      Mode  Cnt     Score      Error  Units
DeepCopyJMH.copyConstructor                                   avgt 30 10,156   ± 0,091    us/op
DeepCopyJMH.kryo_cached                                       avgt 30 165,828  ± 6,139    us/op
DeepCopyJMH.kostaskougiosCloningLib_threadLocal               avgt 30 331,250  ± 9,536    us/op
DeepCopyJMH.kostaskougiosCloningLib_cached                    avgt 30 340,124  ± 11,277   us/op
DeepCopyJMH.jackson_cachedThreadLocal_generic_toTokenBuffer   avgt 30 564,739  ± 4,185    us/op
DeepCopyJMH.jackson_cached2_generic_toTokenBuffer             avgt 30 569,348  ± 3,350    us/op
DeepCopyJMH.jackson_cached_typed_toTokenBuffer                avgt 30 573,985  ± 8,429    us/op
DeepCopyJMH.jackson_cached_generic_toTokenBuffer              avgt 30 578,445  ± 9,753    us/op
DeepCopyJMH.protostuff_copy_reuse_TL_schema_buffer            avgt 30 579,898  ± 8,042    us/op
DeepCopyJMH.protostuff_cachedSchema                           avgt 30 595,741  ± 10,816   us/op
DeepCopyJMH.protostuff_static                                 avgt 30 596,880  ± 13,978   us/op
DeepCopyJMH.protostuff_copy_reuse_runtimeschema_buffer        avgt 30 603,951  ± 38,564   us/op
DeepCopyJMH.jackson_threadLocalEverything_typed_toTokenBuffer avgt 30 795,476  ± 68,194   us/op
DeepCopyJMH.commonsLangClone                                  avgt 30 997,923  ± 15,329   us/op
DeepCopyJMH.jackson_cached_generic_toBytes                    avgt 30 1109,778 ± 10,032   us/op
DeepCopyJMH.jackson_cached_typed_toBytes                      avgt 30 1116,167 ± 12,105   us/op
DeepCopyJMH.jackson_cached_typed_toString                     avgt 30 1191,119 ± 7,674    us/op
DeepCopyJMH.jackson_new2_typed_toString                       avgt 30 1381,921 ± 6,235    us/op
DeepCopyJMH.springSerialize                                   avgt 30 1569,072 ± 237,495  us/op
DeepCopyJMH.jackson_new_generic_toString                      avgt 30 1621,853 ± 164,792  us/op
DeepCopyJMH.jackson_new_typed_toString                        avgt 30 3196,530 ± 1307,020 us/op
DeepCopyJMH.modelMapper_cached                                avgt 30 3828,482 ± 123,046  us/op
DeepCopyJMH.modelMapper_threadLocal                           avgt 30 3940,539 ± 178,672  us/op
DeepCopyJMH.gson_cached                                       avgt 30 4183,107 ± 26,918   us/op
DeepCopyJMH.gson_threadLocal                                  avgt 30 4202,343 ± 20,727   us/op
DeepCopyJMH.jaxbMoxy_cached                                   avgt 30 8824,832 ± 270,818  us/op


Contended results (forks 1, threads 32)
----------------------------------------
Benchmark                                                      Mode  Cnt       Score      Error  Units
DeepCopyJMH.copyConstructor                                   avgt 30 207,898    ± 6,459    us/op
DeepCopyJMH.kryo_cached                                       avgt 30 2105,272   ± 81,623   us/op
DeepCopyJMH.kostaskougiosCloningLib_threadLocal               avgt 30 4016,538   ± 132,252  us/op
DeepCopyJMH.kostaskougiosCloningLib_cached                    avgt 30 4367,219   ± 156,462  us/op
DeepCopyJMH.protostuff_cachedSchema                           avgt 30 7645,974   ± 246,463  us/op
DeepCopyJMH.protostuff_copy_reuse_runtimeschema_buffer        avgt 30 7649,374   ± 259,132  us/op
DeepCopyJMH.protostuff_static                                 avgt 30 7678,049   ± 203,398  us/op
DeepCopyJMH.protostuff_copy_reuse_TL_schema_buffer            avgt 30 7883,275   ± 205,596  us/op
DeepCopyJMH.jackson_cached2_generic_toTokenBuffer             avgt 30 10324,883  ± 265,273  us/op
DeepCopyJMH.jackson_cached_typed_toTokenBuffer                avgt 30 10572,224  ± 358,393  us/op
DeepCopyJMH.jackson_cachedThreadLocal_generic_toTokenBuffer   avgt 30 10665,744  ± 276,739  us/op
DeepCopyJMH.jackson_threadLocalEverything_typed_toTokenBuffer avgt 30 10694,957  ± 297,900  us/op
DeepCopyJMH.jackson_cached_generic_toTokenBuffer              avgt 30 11134,311  ± 418,073  us/op
DeepCopyJMH.springSerialize                                   avgt 30 15840,783  ± 471,570  us/op
DeepCopyJMH.commonsLangClone                                  avgt 30 16338,170  ± 402,404  us/op
DeepCopyJMH.jackson_cached_typed_toBytes                      avgt 30 19942,008  ± 534,589  us/op
DeepCopyJMH.jackson_cached_generic_toBytes                    avgt 30 20319,409  ± 724,155  us/op
DeepCopyJMH.jackson_cached_typed_toString                     avgt 30 21247,110  ± 642,285  us/op
DeepCopyJMH.jackson_new_generic_toString                      avgt 30 24279,864  ± 925,031  us/op
DeepCopyJMH.jackson_new_typed_toString                        avgt 30 25754,157  ± 760,301  us/op
DeepCopyJMH.jackson_new2_typed_toString                       avgt 30 26371,262  ± 4683,951 us/op
DeepCopyJMH.modelMapper_threadLocal                           avgt 30 48496,786  ± 1367,234 us/op
DeepCopyJMH.gson_threadLocal                                  avgt 30 64827,040  ± 1746,201 us/op
DeepCopyJMH.gson_cached                                       avgt 30 69650,841  ± 2303,813 us/op
DeepCopyJMH.modelMapper_cached                                avgt 30 91016,509  ± 5439,031 us/op
DeepCopyJMH.jaxbMoxy_cached                                   avgt 30 116948,379 ± 3665,064 us/op
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
    Cloner kostaskougiosCloningLib = getCloner();

    @NotNull
    private Cloner getCloner() {
        var c = new Cloner();
        // immutable classes are not cloned
        c.registerImmutable(OffsetDateTime.class);
        c.registerImmutable(LocalDateTime.class);
        c.registerImmutable(LocalTime.class);
        c.registerImmutable(OffsetTime.class);
        c.registerImmutable(ZonedDateTime.class);
        return c;
    }

    ThreadLocal<Cloner> threadLocalKostaskougiosCloningLib = ThreadLocal.withInitial(this::getCloner);

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

        //more forks: more reliable result but multiplies benchmark execution time
        var uncontenedResult = bench.benchmarkAllDeepCopyApproaches(3, 1);
        var contendResult = bench.benchmarkAllDeepCopyApproaches(3, 32);
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

                  } catch (Exception e) {
                      throw new RuntimeException("Failed to instantiate/verify implementation: " + m.getName(), e);
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
