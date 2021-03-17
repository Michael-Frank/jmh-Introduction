package de.frank.jmh.architecture;


import de.frank.impl.jaxb.*;
import de.frank.jmh.model.*;
import org.eclipse.persistence.jaxb.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.results.*;
import org.openjdk.jmh.results.format.*;
import org.openjdk.jmh.runner.*;
import org.openjdk.jmh.runner.options.*;
import org.xml.sax.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.*;
import javax.xml.validation.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

/*--
Questions:
- Should i create a new JaxbContext and Marshaller/UnMarshaller for each operation?
- Are there benefits Marshalling/Unmarschalling to/from String vs to/from byte[]?
- Are the the JAXB instances thread safe?

Results:
- Caching? YES - its worth caching the JaxbContext AND its Marshaller/Unmarschaller
 - Cache the JaxbContext takes 81ms - caching it is ~34x faster then creating a new one for each operation
 - Caching a marshaller is ~1.24x faster then creating a new one for each operation
 - Caching a unmarshaller is ~5.8x faster then creating a new one for each operation
- Are there benefits Marshalling/Unmarschalling to/from to/from byte[] instead of String? YES
 - using to/from bytes ist around 1,35x faster then the string api.
- Are the instances thread safe? NO!
 - JaxbContexts are specific to their registered class(es) (Map<Class, Marshaller>)
 - and the Marshaller/Unmarschallers should be cached per thread.


Single shot (e.g. "cold load" or load exactly once at startup) (lower is better)
Benchmark                                          Mode  Cnt   Score    Error  Units
Only getting the context:
  newContext                                          ss    3   0,018 ±  0,062   s/op
  cachedContext                                       ss    3  ≈ 10⁻⁵            s/op
Getting the context AND Marshall/Unmarschall:
  marshall_newContext_newMarshaller_string            ss    3   0,015 ±  0,019   s/op
  marshall_cachedContext_newMarshaller_string         ss    3   0,002 ±  0,006   s/op
  marshall_cachedContext_cachedMarshaller_string      ss    3   0,002 ±  0,001   s/op
  marshall_cachedContext_cachedMarshaller_bytes       ss    3   0,002 ±  0,004   s/op
  unMarshall_newContext_newMarshaller_string          ss    3   0,041 ±  0,045   s/op
  unMarshall_cachedContext_newMarshaller_string       ss    3   0,029 ±  0,072   s/op
  unMarshall_cachedContext_cachedMarshaller_string    ss    3   0,018 ±  0,283   s/op
  unMarshall_cachedContext_cachedMarshaller_bytes     ss    3   0,022 ±  0,324   s/op


Single thread: lower is better
Benchmark                                           Mode  Cnt      Score      Error  Units
Only getting the context:
  newContext                                        avgt   10   4009,766 ±  896,085  us/op
  cachedContext                                     avgt   10      0,009 ±    0,001  us/op
Getting the context AND Marshall/Unmarschall:
  marshall_newContext_newMarshaller_string          avgt   10   3839,991 ±  517,039  us/op
  marshall_cachedContext_newMarshaller_string       avgt   10    338,241 ±   46,644  us/op
  marshall_cachedContext_cachedMarshaller_string    avgt   10    308,393 ±   17,081  us/op
  marshall_cachedContext_cachedMarshaller_bytes     avgt   10    259,110 ±    3,472  us/op
  unMarshall_newContext_newMarshaller_string        avgt   10  12652,639 ± 1448,577  us/op
  unMarshall_cachedContext_newMarshaller_string     avgt   10   8031,984 ± 1164,141  us/op
  unMarshall_cachedContext_cachedMarshaller_string  avgt   10   1043,850 ±   46,593  us/op
  unMarshall_cachedContext_cachedMarshaller_bytes   avgt   10   1062,012 ±   43,085  us/op


Contended (32 Threads) lower is better
Benchmark                                         Mode  Cnt       Score        Error  Units
Only getting the context:
  newContext                                        avgt    5   40668,693 ±  24168,175  us/op
  cachedContext                                     avgt    5       0,066 ±      0,025  us/op
Getting the context AND Marshall/Unmarschall:
  marshall_newContext_newMarshaller_string          avgt    5   44425,458 ±  26075,485  us/op
  marshall_cachedContext_newMarshaller_string       avgt    5    2286,723 ±    262,975  us/op
  marshall_cachedContext_cachedMarshaller_string    avgt    5    2159,866 ±    127,643  us/op
  marshall_cachedContext_cachedMarshaller_bytes     avgt    5    1697,794 ±     28,240  us/op
  unMarshall_newContext_newMarshaller_string        avgt    5  165321,542 ± 110283,364  us/op
  unMarshall_cachedContext_newMarshaller_string     avgt    5  125892,729 ±  83419,342  us/op
  unMarshall_cachedContext_cachedMarshaller_string  avgt    5   26249,094 ±  58037,208  us/op
  unMarshall_cachedContext_cachedMarshaller_bytes   avgt    5   19314,365 ±  30359,857  us/op

 */
public class JaxbContextAndMarshallerCaching {


    @State(Scope.Benchmark)
    public static class MyState {
        Class<HierarchicalMockModel> modelType = HierarchicalMockModel.class;
        //Number of objects in model = 10 *10 *100 = 10_000
        HierarchicalMockModel model = HierarchicalMockModel.newInstance(10, 10, 100);

        String modelAsXmlString = CachedJaxbXmlMapper.toXMLString(model);
        byte[] modelAsBytes = CachedJaxbXmlMapper.toXMLBytes(model);
    }

    public static void main(String[] args) throws Exception {
        JaxbContextAndMarshallerCaching bench = new JaxbContextAndMarshallerCaching();
        //test if all impls work before starting a long running benchmark
        bench.testAllImplsWorkCorrectly();
        bench.runAllBenchmarks(); //that will take some time... fetch a coffee
    }


    public void runAllBenchmarks() throws Exception {
        Options singleShotBench = new OptionsBuilder()
                .include(this.getClass().getName() + ".*")
                .mode(Mode.SingleShotTime)
                .warmupIterations(0)
                .measurementIterations(3)
                .forks(0)
                .threads(1)
                .shouldFailOnError(true)
                .build();

        Options singleThreadedBench = new OptionsBuilder()
                .include(this.getClass().getName() + ".*")
                .mode(Mode.AverageTime)
                .timeUnit(TimeUnit.MICROSECONDS)
                .warmupTime(TimeValue.seconds(1))
                .warmupIterations(3)
                .measurementTime(TimeValue.seconds(1))
                .measurementIterations(5)
                .threads(1)//uncontended
                .forks(1)
                .shouldFailOnError(true)
                .build();

        Options contentedBench = new OptionsBuilder()
                .include(this.getClass().getName() + ".*")
                .mode(Mode.AverageTime)
                .timeUnit(TimeUnit.MICROSECONDS)
                .warmupTime(TimeValue.seconds(1))
                .warmupIterations(3)
                .measurementTime(TimeValue.seconds(1))
                .measurementIterations(5)
                .threads(32)//contended - set to 2x your cores!
                .forks(1)
                .shouldFailOnError(true)
                .build();

        Map<String, Collection<RunResult>> results = new HashMap<>();
        results.put("Single Shot benchmark", new Runner(singleShotBench).run());
        results.put("Single thread results avg time", new Runner(singleThreadedBench).run());
        results.put("MultiThreaded contented avg time benchmark", new Runner(contentedBench).run());

        //print all results aggregated at the end of the benchmark
        ResultFormat format = ResultFormatFactory.getInstance(ResultFormatType.TEXT, System.out);
        for (Map.Entry<String, Collection<RunResult>> entry : results.entrySet()) {
            System.out.println(entry.getKey());
            format.writeOut(entry.getValue());
        }

    }

    public void testAllImplsWorkCorrectly() {
        MyState s = new MyState();
        Arrays.stream(this.getClass().getDeclaredMethods())
              .filter(m -> m.getAnnotation(Benchmark.class) != null)
              .filter(m -> m.getParameters().length == 1 && m.getParameters()[0].getType() == MyState.class)
              .forEach(m -> {
                  try {
                      System.out.println(m);
                      //exec twice
                      m.invoke(this, s);
                      m.invoke(this, s);
                  } catch (IllegalAccessException | InvocationTargetException e) {
                      e.printStackTrace();
                  }
              });
        System.out.println("OK - All impls work as expected");
        System.out.println(new MyState().modelAsXmlString);
    }

    @Benchmark
    public JAXBContext newContext(MyState s) {
        return newJaxbContextFor(s.modelType);
    }

    @Benchmark
    public JAXBContext cachedContext(MyState s) {
        return CachedJaxbXmlMapperContext.cachedContextFor(s.modelType);
    }

    @Benchmark
    public String marshall_newContext_newMarshaller_string(MyState s) throws JAXBException {
        JAXBContext ctx = newJaxbContextFor(s.modelType);
        Marshaller marshaller = newMarshaller(ctx);
        StringWriter sw = new StringWriter();
        marshaller.marshal(s.model, sw);
        return sw.toString();
    }

    @Benchmark
    public HierarchicalMockModel unMarshall_newContext_newMarshaller_string(MyState s) throws JAXBException {
        JAXBContext ctx = newJaxbContextFor(s.modelType);
        Unmarshaller unmarshaller = newUnmarshaller(ctx, s.modelType);
        return (HierarchicalMockModel) unmarshaller.unmarshal(new StringReader(s.modelAsXmlString));
    }

    @Benchmark
    public String marshall_cachedContext_newMarshaller_string(MyState s) throws JAXBException {
        JAXBContext ctx = CachedJaxbXmlMapperContext.cachedContextFor(s.modelType);
        Marshaller marshaller = newMarshaller(ctx);
        StringWriter sw = new StringWriter();
        marshaller.marshal(s.model, sw);
        return sw.toString();
    }

    @Benchmark
    public HierarchicalMockModel unMarshall_cachedContext_newMarshaller_string(MyState s) throws JAXBException {
        JAXBContext ctx = CachedJaxbXmlMapperContext.cachedContextFor(s.modelType);
        Unmarshaller unmarshaller = newUnmarshaller(ctx, s.modelType);
        return (HierarchicalMockModel) unmarshaller.unmarshal(new StringReader(s.modelAsXmlString));
    }

    @Benchmark
    public String marshall_cachedContext_cachedMarshaller_string(MyState s) {
        return CachedJaxbXmlMapper.toXMLString(s.model);
    }

    @Benchmark
    public HierarchicalMockModel unMarshall_cachedContext_cachedMarshaller_string(MyState s) {
        return CachedJaxbXmlMapper.fromXML(s.modelAsXmlString, s.modelType);
    }

    @Benchmark
    public byte[] marshall_cachedContext_cachedMarshaller_bytes(MyState s) {
        return CachedJaxbXmlMapper.toXMLBytes(s.model);
    }

    @Benchmark
    public HierarchicalMockModel unMarshall_cachedContext_cachedMarshaller_bytes(MyState s) {
        return CachedJaxbXmlMapper.formXml(s.modelAsBytes, s.modelType);
    }


    public static JAXBContext newJaxbContextFor(Class<?> type) {
        Objects.requireNonNull(type);
        try {
            return JAXBContext.newInstance(type);
        } catch (JAXBException e) {
            throw new UncheckedXMLException(e);
        }
    }

    public static Marshaller newMarshaller(JAXBContext jaxbContext) {
        Objects.requireNonNull(jaxbContext);

        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(MarshallerProperties.JSON_MARSHAL_EMPTY_COLLECTIONS, true);
            return marshaller;
        } catch (JAXBException e) {
            throw new UncheckedXMLException(e);
        }
    }

    public static Unmarshaller newUnmarshaller(JAXBContext jaxbContext, Class<?> type) {
        Objects.requireNonNull(jaxbContext);
        Objects.requireNonNull(type);
        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            Schema schema = SchemaNodeGenerator.generateSchemaFor(type);
            unmarshaller.setSchema(schema);
            return unmarshaller;
        } catch (JAXBException | SAXException e) {
            throw new UncheckedXMLException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static class CachedJaxbXmlMapperContext {

        private static final ConcurrentHashMap<Class<?>, JAXBContext> JAXB_CONTEXTS = new ConcurrentHashMap<>();

        public static JAXBContext cachedContextFor(Class<?> type) {
            Objects.requireNonNull(type);
            return JAXB_CONTEXTS.computeIfAbsent(type, JaxbContextAndMarshallerCaching::newJaxbContextFor);
        }

    }


    private static class UncheckedXMLException extends RuntimeException {
        public UncheckedXMLException(Exception e) {
            super(e);
        }
    }
}
