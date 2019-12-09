package de.frank.jmh.algorithms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/*--
 It is thread-safe and saleable to reuse the  ObjectReader reader = new ObjectMapper().readerFor(Map.class);
 Higher is Better
 Throughput in  ops/s          1         2          4          8         32 #numThreads
 newMapperPerCall         24.845    44.459     79.350    114.334    101.316
 sharedObjectMapper      429.138   821.334  1.402.759  1.622.020  1.610.938
 sharedObjectReader      434.012   804.545  1.379.722  1.716.694  1.739.768 #winner

 Lower is Better
 Per call in  us/op            1         2          4          8         32 #numThreads
 newMapperPerCall           53,7      54,2       63,7       90,0      386,7
 sharedObjectMapper          2,9       3,0        3,7        5,8       25,1
 sharedObjectReader          2,8       2,9        4,1        6,0       26,4 #winner

*/

/**
 * @author Michael Frank
 * @version 1.0 13.05.2018
 */
@Fork(1)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class JacksonReaderBenchmarkJMH {

    public static void main(String[] args) throws RunnerException, IOException {
        System.out.println((Map<String, String>) new SharedOjectReaderHolder().reader.readValue(new InputHolder().input));

        Options opt = new OptionsBuilder().include(JacksonReaderBenchmarkJMH.class.getName())//
                .result(String.format("%s_%s.json",
                        DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                        JacksonReaderBenchmarkJMH.class.getSimpleName()))
                .forks(1)//
                .build();//
        new Runner(opt).run();
    }


    @State(Scope.Thread)
    public static class InputHolder {
        String input = "{\"my_token\":\"sdfdhDFGtrz546SDFG54768SDFG657SDH4576SDH4576SGH7SghSDFG456SGfgjdfgjhsdgfd5SDFGfsgdfg45645tzgdfd4tFGFGF.sdfdhDFGtrz546SDFG54768SDFG657SDH4576SDH4576SGH7SghSDFG456SGfgjdfgjhsdgfd5SDFGfsgdfg45645tzgdfd4tFGFGsdfdhDFGtrz546SDFG54768SDFG657SDH4576SDH4576SGH7SghSDFG456SGfgjdfgjhsdgfd5SDFGfsgdfg45645tzgdfd4tFGFGsdfdhDFGtrz546SDFG54768SDFG657SDH4576SDH4576SGH7SghSDFG456SGfgjdfgjh.sdfdhDFGtrz546SDFG54768SDFG657SDH4576SDH4576SGH7SghSDFG45-6S-Gfgjdfgj-h-sdgfd5SD_FGfsgdfg45-6_45tzgdfd4dgsdfg456sgdfgerttFGFGsdfdh-DFGtrz546SDFG54-768SDFG657SDH4576SD_H4576SGH7SghSDFG456SGfgjdfgjhsd-g_fd5SDFGfsgdfg45645tzgdfd4tFG_F-G-sdfdhDFG_trz546SDFG547_68SDFG657SDH45_76SDH457_6SGH7SghSDFG-456SGfgjd-fgjhsdgfd5S-DFGfsgdfg4564-5tzgdfd4tFGF\",\"token_type\":\"bearer\",\"expires_in\":2699,\"sub\":{\"misc:anon\":\"\"},\"aud\":[\"*:*\"],\"iat\":\"1507456456\",\"asc\":\"someValue\",\"exp\":1507466456,\"jti\":\"67845b7f-45c1-8671-945d-91daed3a7620\",\"iss\":\"some:issuer:foo:bar\",\"rqi\":\"i-am-the-requestor\",\"stg\":\"foobar\"}";
    }

    @State(Scope.Benchmark)
    public static class SharedOjectReaderHolder {
        final ObjectReader reader = new ObjectMapper().readerFor(Map.class);
    }

    @State(Scope.Benchmark)
    public static class SharedObjectMapper {
        final ObjectMapper mapper = new ObjectMapper();
    }


    //#########################
    //##### BENCHMARK
    //#########################

    @Benchmark
    @Threads(1)
    public String sharedObjectReader_1(SharedOjectReaderHolder reader, InputHolder input) throws IOException {
        Map<String, String> map = reader.reader.readValue(input.input);
        return map.get("my_token");
    }

    @Benchmark
    @Threads(2)
    public String sharedObjectReader_2(SharedOjectReaderHolder reader, InputHolder input) throws IOException {
        Map<String, String> map = reader.reader.readValue(input.input);
        return map.get("my_token");
    }

    @Benchmark
    @Threads(4)
    public String sharedObjectReader_4(SharedOjectReaderHolder reader, InputHolder input) throws IOException {
        Map<String, String> map = reader.reader.readValue(input.input);
        return map.get("my_token");
    }

    @Benchmark
    @Threads(8)
    public String sharedObjectReader_8(SharedOjectReaderHolder reader, InputHolder input) throws IOException {
        Map<String, String> map = reader.reader.readValue(input.input);
        return map.get("my_token");
    }

    @Benchmark
    @Threads(32)
    public String sharedObjectReader_32(SharedOjectReaderHolder reader, InputHolder input) throws IOException {
        Map<String, String> map = reader.reader.readValue(input.input);
        return map.get("my_token");
    }


    @Benchmark
    @Threads(1)
    public String sharedObjectMapper_1(SharedObjectMapper mapper, InputHolder input) throws IOException {
        Map<String, String> map = mapper.mapper.readValue(input.input, Map.class);
        return map.get("my_token");
    }

    @Benchmark
    @Threads(2)
    public String sharedObjectMapper_2(SharedObjectMapper mapper, InputHolder input) throws IOException {
        Map<String, String> map = mapper.mapper.readValue(input.input, Map.class);
        return map.get("my_token");
    }

    @Benchmark
    @Threads(4)
    public String sharedObjectMapper_4(SharedObjectMapper mapper, InputHolder input) throws IOException {
        Map<String, String> map = mapper.mapper.readValue(input.input, Map.class);
        return map.get("my_token");
    }

    @Benchmark
    @Threads(8)
    public String sharedObjectMapper_8(SharedObjectMapper mapper, InputHolder input) throws IOException {
        Map<String, String> map = mapper.mapper.readValue(input.input, Map.class);
        return map.get("my_token");
    }

    @Benchmark
    @Threads(32)
    public String sharedObjectMapper_32(SharedObjectMapper mapper, InputHolder input) throws IOException {
        Map<String, String> map = mapper.mapper.readValue(input.input, Map.class);
        return map.get("my_token");
    }


    @Benchmark
    @Threads(1)
    public String sharedMapperReadTree_1(SharedObjectMapper mapper, InputHolder input) throws IOException {
        JsonNode actualObj = mapper.mapper.readTree(input.input);
        JsonNode jsonNode1 = actualObj.get("my_token");
        return jsonNode1.textValue();
    }

    @Benchmark
    @Threads(2)
    public String sharedMapperReadTree_2(SharedObjectMapper mapper, InputHolder input) throws IOException {
        JsonNode actualObj = mapper.mapper.readTree(input.input);
        JsonNode jsonNode1 = actualObj.get("my_token");
        return jsonNode1.textValue();
    }

    @Benchmark
    @Threads(4)
    public String sharedMapperReadTree_4(SharedObjectMapper mapper, InputHolder input) throws IOException {
        JsonNode actualObj = mapper.mapper.readTree(input.input);
        JsonNode jsonNode1 = actualObj.get("my_token");
        return jsonNode1.textValue();
    }

    @Benchmark
    @Threads(8)
    public String sharedMapperReadTree_8(SharedObjectMapper mapper, InputHolder input) throws IOException {
        JsonNode actualObj = mapper.mapper.readTree(input.input);
        JsonNode jsonNode1 = actualObj.get("my_token");
        return jsonNode1.textValue();
    }

    @Benchmark
    @Threads(32)
    public String sharedMapperReadTree_32(SharedObjectMapper mapper, InputHolder input) throws IOException {
        JsonNode actualObj = mapper.mapper.readTree(input.input);
        JsonNode jsonNode1 = actualObj.get("my_token");
        return jsonNode1.textValue();
    }


    @Benchmark
    @Threads(1)
    public String newMapperPerCall_1(InputHolder input) throws IOException {
        Map<String, String> map = new ObjectMapper().readValue(input.input, Map.class);
        return map.get("my_token");
    }

    @Benchmark
    @Threads(2)
    public String newMapperPerCall_2(InputHolder input) throws IOException {
        Map<String, String> map = new ObjectMapper().readValue(input.input, Map.class);
        return map.get("my_token");
    }

    @Benchmark
    @Threads(4)
    public String newMapperPerCall_4(InputHolder input) throws IOException {
        Map<String, String> map = new ObjectMapper().readValue(input.input, Map.class);
        return map.get("my_token");
    }

    @Benchmark
    @Threads(8)
    public String newMapperPerCall_8(InputHolder input) throws IOException {
        Map<String, String> map = new ObjectMapper().readValue(input.input, Map.class);
        return map.get("my_token");
    }

    @Benchmark
    @Threads(32)
    public String newMapperPerCall_32(InputHolder input) throws IOException {
        Map<String, String> map = new ObjectMapper().readValue(input.input, Map.class);
        return map.get("my_token");
    }
}
