package de.frank.jmh.algorithms;

import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.commons.lang3.RandomStringUtils;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/*--
single threaded @ 1000 prefixes and a 25% match rate in 25000 tokens
Benchmark     Mode  Cnt      Score      Error  Units
patriciaTrie thrpt   30  8.416.393 ±  106.310  ops/s
sortedList   thrpt   30    364.990 ±   42.173  ops/s
treeMap      thrpt   30     70.211 ±    7.369  ops/s
treeSet      thrpt   30     89.214 ±    5.346  ops/s

//single threaded  @ 5 prefixes and a  38% match rate
Benchmark     Mode  Cnt       Score       Error  Units
patriciaTrie thrpt   30  19.801.164 ±   256.800  ops/s
sortedList   thrpt   30  19.423.753 ± 1.469.290  ops/s
treeMap      thrpt   30  10.711.479 ±   280.535  ops/s
treeSet      thrpt   30  10.092.850 ± 1.005.189  ops/s
 */
@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 4, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(3)
@State(Scope.Benchmark)
public class LongestPrefixMatcherWithSubstitutionBenchmark {
    static Map<String, String> PREFIXES = new HashMap<>();
    public static List<String> INPUT_TOKENS = new ArrayList<>();

    static {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < 1000; i++) {
            String prefix = RandomStringUtils.random(r.nextInt(2, 8), true, false);
            PREFIXES.put(prefix, String.valueOf(i));

        }
        for (int i = 0; i < 10000; i++) {
            String token = RandomStringUtils.random(r.nextInt(1, 8), true, false);
            INPUT_TOKENS.add(token);
        }
        for (int i = 0; i < 5000; i++) {
            String token = RandomStringUtils.random(r.nextInt(8, 15), true, false);
            INPUT_TOKENS.add(token);
        }
        PatriciaTriePrefixMatcher<String> patricia = new PatriciaTriePrefixMatcher<>(PREFIXES);
        long matching = INPUT_TOKENS.stream().filter(i -> patricia.getLongestMatchingPrefix(i) != null).count();

        System.out.println("Setup complete: " + matching + "/" + INPUT_TOKENS.size() + "(" + (((double) INPUT_TOKENS.size()) / matching) + "%) tokens will match a prefix");
    }

    @State(Scope.Thread)
    public static class TestData {
        String param;

        @Setup(Level.Invocation)
        public void doSetup() {
            //each benchmark loop invocation gets its own random value
            //Reasons:
            // - our usecase is to tokenize a file and longestPrefixMatch each token
            // - it is discouraged to write loops into @Benchmark code
            this.param = INPUT_TOKENS.get(ThreadLocalRandom.current().nextInt(INPUT_TOKENS.size()));
        }

        public String getArgument() {
            return param;
        }
    }

    SortedListLongestPrefixMapper<String> sortedList = new SortedListLongestPrefixMapper(PREFIXES);
    TreeSetLongestPrefixMatcher treeSet = new TreeSetLongestPrefixMatcher(PREFIXES.keySet());
    TreeMapLongestPrefixMapper<String> treeMap = new TreeMapLongestPrefixMapper<>(PREFIXES);
    PatriciaTriePrefixMatcher<String> patriciaTrie = new PatriciaTriePrefixMatcher<>(PREFIXES);

    public static void main(String[] args) throws RunnerException {
        testAlgos();

        Options opt = new OptionsBuilder()
                .include(LongestPrefixMatcherWithSubstitutionBenchmark.class.getName())
                // .result(String.format("%s_%s.json",
                //         DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                //         LongestPrefixMatcherBenchmark.class.getSimpleName()))
                .build();
        new Runner(opt).run();
    }


    @Benchmark
    public String sortedList(TestData args) {
        return sortedList.getLongestMatchingPrefix(args.getArgument());
    }

    @Benchmark
    public String treeSet(TestData args) {
        return treeSet.getLongestMatchingPrefixValue(args.getArgument());
    }

    @Benchmark
    public String treeMap(TestData args) {
        return treeMap.getLongestMatchingPrefixValue(args.getArgument());
    }

    @Benchmark
    public String patriciaTrie(TestData args) {
        return patriciaTrie.getLongestMatchingPrefix(args.getArgument());
    }

    public static class SortedListLongestPrefixMapper<T> {
        private final Map<String, T> map;
        private final ArrayList<String> list;

        public SortedListLongestPrefixMapper(Map<String, T> prefixes) {
            this.map = new TreeMap<>(prefixes);
            this.list = new ArrayList<>(prefixes.keySet());
            list.sort((s1, s2) -> {
                if (s1.length() != s2.length()) {
                    return s2.length() - s1.length(); // descending order
                }
                return s1.compareTo(s2); // ascending order
            });
        }

        public T getLongestMatchingPrefixValue(String input) {
            String key = getLongestMatchingPrefix(input);
            return key == null ? null : map.get(key);
        }

        public String getLongestMatchingPrefix(String input) {
            for (String key : list) {
                if (input.startsWith(key)) {
                    return key;
                }
            }
            return null;
        }
    }


    public static class PatriciaTriePrefixMatcher<T> {

        private final PatriciaTrie<T> trie;

        public PatriciaTriePrefixMatcher(Map<String, T> in) {
            this.trie = new PatriciaTrie<>(in);
        }

        public T getLongestMatchingPrefixValue(String input) {
            String longestMatchingKey = trie.selectKey(input);
            if (!input.startsWith(longestMatchingKey)) {
                return null;//reject partial matches
            }
            return trie.selectValue(longestMatchingKey);
        }

        public String getLongestMatchingPrefix(String input) {
            String longestMatchingKey = trie.selectKey(input);
            return input.startsWith(longestMatchingKey)
                    ? longestMatchingKey
                    : null;//reject partial matches

        }
    }

    public static class TreeMapLongestPrefixMapper<T> {
        private final TreeMap<String, T> prefixes;

        public TreeMapLongestPrefixMapper(Map<String, T> prefixes) {
            this.prefixes = new TreeMap<>(prefixes);
        }

        private String getLongestMatchingPrefixKey(String in) {
            Map.Entry<String, T> prefix = getLongestMatchingPrefixEntry(in);
            return prefix == null ? null : prefix.getKey();
        }

        private T getLongestMatchingPrefixValue(String in) {
            Map.Entry<String, T> prefix = getLongestMatchingPrefixEntry(in);
            return prefix == null ? null : prefix.getValue();
        }

        private Map.Entry<String, T> getLongestMatchingPrefixEntry(String in) {
            Map.Entry<String, T> prefix = prefixes.floorEntry(in);
            while (prefix != null && !in.startsWith(prefix.getKey())) {
                prefix = prefixes.floorEntry(prefix.getKey().substring(0, prefix.getKey().length() - 1));
            }
            return prefix;
        }
    }

    public static class TreeSetLongestPrefixMatcher {
        private final TreeSet<String> prefixSet;

        public TreeSetLongestPrefixMatcher(Collection<String> prefixes) {
            this.prefixSet = new TreeSet<>(prefixes);
        }

        private String getLongestMatchingPrefixValue(String in) {
            String prefix = prefixSet.floor(in);
            while (prefix != null && !in.startsWith(prefix)) {
                prefix = prefixSet.floor(prefix.substring(0, prefix.length() - 1));
            }
            return prefix;
        }

    }


    private static void testAlgos() {
        Map<String, String> FEW_PREFIXES = Map.of(
                "1234", "a",
                "123", "b",
                "12", "c",
                "555", "d",
                "556", "e"
        );

        List<String> INPUT_TOKENS = List.of(
                //matches
                "12345",
                "123",
                "5555",
                "556789123",
                //*we expect a lot of  "non-matches"

                "55",
                "a",
                "1",
                "556789",
                "asdfasfdasdfasdff",
                " ",
                ",",
                "yyyyyyyyyyyy",

                "lkasdfjalsöfaslfasdfsafasdfaslökflasflkasflöasflasflkaslasködflkasföasdöfl");

        SortedListLongestPrefixMapper<String> sortedList = new SortedListLongestPrefixMapper(FEW_PREFIXES);
        TreeSetLongestPrefixMatcher treeSet = new TreeSetLongestPrefixMatcher(FEW_PREFIXES.keySet());
        TreeMapLongestPrefixMapper<String> treeMap = new TreeMapLongestPrefixMapper<>(FEW_PREFIXES);
        PatriciaTriePrefixMatcher<String> patricia = new PatriciaTriePrefixMatcher<>(FEW_PREFIXES);

        INPUT_TOKENS.forEach(n -> System.out.println(n + "-> "
                                                     + " TreeSet: " + treeSet.getLongestMatchingPrefixValue(n)
                                                     + ", TreeMap: " + treeMap.getLongestMatchingPrefixValue(n)
                                                     + ", SortedList: " + sortedList.getLongestMatchingPrefixValue(n)
                                                     + ", Patricia: " + patricia.getLongestMatchingPrefixValue(n)));
    }


}
