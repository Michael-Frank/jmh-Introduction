package de.frank.jmh.algorithms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
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

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 *
 And the winner is: sharedObjectReader!
 It is totally thread-safe and saleable to reuse the  ObjectReader reader = new ObjectMapper().readerFor(Map.class);

 Benchmark                       Mode  Cnt        Score        Error  Units
 my_tokennewMapperPerCall_1     thrpt    5    24845,747 ±    401,588  ops/s
 my_tokennewMapperPerCall_2     thrpt    5    44459,373 ±   4541,548  ops/s
 my_tokennewMapperPerCall_4     thrpt    5    79350,459 ±  11168,382  ops/s
 my_tokennewMapperPerCall_8     thrpt    5   114334,386 ±   7930,404  ops/s
 my_tokennewMapperPerCall_32    thrpt    5   101316,231 ±  13915,002  ops/s

 my_tokensharedObjectMapper_1   thrpt    5   429138,455 ±  15425,506  ops/s
 my_tokensharedObjectMapper_2   thrpt    5   821334,492 ±  41812,944  ops/s
 my_tokensharedObjectMapper_4   thrpt    5  1402759,756 ±  92106,132  ops/s
 my_tokensharedObjectMapper_8   thrpt    5  1622020,315 ± 131445,036  ops/s
 my_tokensharedObjectMapper_32  thrpt    5  1610938,419 ± 230044,029  ops/s

 my_tokensharedObjectReader_1   thrpt    5   434012,998 ±  26670,798  ops/s
 my_tokensharedObjectReader_2   thrpt    5   804545,269 ±  29523,579  ops/s
 my_tokensharedObjectReader_4   thrpt    5  1379722,464 ± 123574,988  ops/s
 my_tokensharedObjectReader_8   thrpt    5  1716694,389 ± 157594,614  ops/s
 my_tokensharedObjectReader_32  thrpt    5  1739768,637 ±  44723,808  ops/s



 Benchmark                      Mode  Cnt    Score    Error  Units
 my_tokennewMapperPerCall_1     avgt    5   53,725 ±  3,958  us/op
 my_tokennewMapperPerCall_2     avgt    5   54,223 ±  3,276  us/op
 my_tokennewMapperPerCall_4     avgt    5   63,746 ±  3,588  us/op
 my_tokennewMapperPerCall_8     avgt    5   90,052 ±  1,339  us/op
 my_tokennewMapperPerCall_32    avgt    5  386,733 ± 16,266  us/op

 my_tokensharedObjectMapper_1   avgt    5    2,969 ±  0,048  us/op
 my_tokensharedObjectMapper_2   avgt    5    3,083 ±  0,295  us/op
 my_tokensharedObjectMapper_4   avgt    5    3,731 ±  0,251  us/op
 my_tokensharedObjectMapper_8   avgt    5    5,866 ±  0,292  us/op
 my_tokensharedObjectMapper_32  avgt    5   25,153 ±  1,266  us/op

 my_tokensharedObjectReader_1   avgt    5    2,875 ±  0,008  us/op
 my_tokensharedObjectReader_2   avgt    5    2,932 ±  0,067  us/op
 my_tokensharedObjectReader_4   avgt    5    4,179 ±  0,256  us/op
 my_tokensharedObjectReader_8   avgt    5    6,083 ±  0,216  us/op
 my_tokensharedObjectReader_32  avgt    5   26,405 ±  1,320  us/op

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
		System.out.println((Map<String,String>) new SharedOjectReaderHolder().reader.readValue(new InputHolder().input));

		Options opt = new OptionsBuilder().include(".*" + JacksonReaderBenchmarkJMH.class.getSimpleName() + ".*")
				.forks(1).build();
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
