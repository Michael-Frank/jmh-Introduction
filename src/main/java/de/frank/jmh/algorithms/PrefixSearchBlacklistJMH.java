package de.frank.jmh.algorithms;

import de.frank.jmh.util.BenchmarkFormatter;
import de.frank.jmh.util.RandomUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/*--
Task:
given a list of prefixes, we want to efficiently check strings if they start with any of the prefixes (Black-/Whitelisting)

There are many efficient algorithms, most importantly any type of search graph (like Trie's), or a  Bloomfilters...
of.
Special case: if you can predetermine the exact prefix boundaries, a simple HashSet  or sorted List is sufficient and way faster (hash&equals instead of "startsWith")

TL:Dr.:
Problem class:
- "Exact match" (so its essentially no longer a "prefix" match...):  HashSet
- "startsWith"
    - number of prefixes in list <= ~200:  A simple list iterator algo is sufficient: for(prefix:prefixes) toTest.startWith(prefix)
    - large number of prefixes: a Trie, as it scales better and offers a predictable performance and memory characteristic.
       Depending on your Trie-implementation and similarity of your prefixes, the exact "use trie if > x prefixes, may differ.

Q: Whats up with the other implementations like SortedFlatList and searchTrie_flatMemory ?
A: Memory density, layout and predictability often matters. They implement a custom-memory management (just like a arena-allocator would, or like a struct in c languages). Think of it as: store as "serialized" format and run algorithms directly on top of the serialized data.
   Q: Does it help in this case?
   A: according to the benchmark data: NO! (remember: good idea/intention != faster)
   Q: Then why are they here?
   A: To show you good idea/intention != faster - and so you dont have to trie (phun intended) your similar ideas.



Benchmark                                (prefixesCount)  Mode  Cnt         Score         Error  Units
baseline_getRandomString                             20  avgt   10        15,248 ±       0,525  ns/op
exact_hashSet_exact_match                            20  avgt   10        23,433 ±       3,825  ns/op
exact_sortedList_binarySearch                        20  avgt   10        34,335 ±       1,357  ns/op
startsWith_sortedList                                20  avgt   10        90,860 ±       1,241  ns/op
startsWith_naiveList                                 20  avgt   10       101,943 ±      43,199  ns/op
startsWith_sortedFlatListPrefixMatcher               20  avgt   10       216,045 ±      18,833  ns/op
startsWith_searchTrie_flatMemory                     20  avgt   10       248,062 ±       2,780  ns/op
startsWith_regex_singlePatternForAll                 20  avgt   10       296,458 ±       9,461  ns/op
startsWith_searchTrie                                20  avgt   10       453,011 ±      27,364  ns/op
startsWith_searchTrie_flatMemoryBytes                20  avgt   10       563,610 ±     221,592  ns/op
startsWith_regex_patternPerPrefix                    20  avgt   10      1061,493 ±     450,221  ns/op
baseline_getRandomString                            200  avgt   10        14,980 ±       0,557  ns/op
exact_hashSet_exact_match                           200  avgt   10        18,560 ±       1,830  ns/op
exact_sortedList_binarySearch                       200  avgt   10        42,465 ±       2,224  ns/op
startsWith_searchTrie_flatMemory                    200  avgt   10       455,416 ±     179,689  ns/op
startsWith_searchTrie_flatMemoryBytes               200  avgt   10       530,615 ±      11,573  ns/op
startsWith_searchTrie                               200  avgt   10       589,921 ±     269,936  ns/op
startsWith_regex_singlePatternForAll                200  avgt   10       769,245 ±      21,280  ns/op
startsWith_sortedList                               200  avgt   10       844,259 ±     377,054  ns/op
startsWith_sortedFlatListPrefixMatcher              200  avgt   10       878,498 ±      26,655  ns/op
startsWith_naiveList                                200  avgt   10      1012,019 ±     669,991  ns/op
startsWith_regex_patternPerPrefix                   200  avgt   10      8899,942 ±     535,399  ns/op
baseline_getRandomString                           2000  avgt   10        15,865 ±       0,994  ns/op
exact_hashSet_exact_match                          2000  avgt   10        26,531 ±       9,607  ns/op
exact_sortedList_binarySearch                      2000  avgt   10        50,805 ±      18,379  ns/op
startsWith_searchTrie_flatMemory                   2000  avgt   10       331,527 ±      33,690  ns/op
startsWith_searchTrie_flatMemoryBytes              2000  avgt   10       544,657 ±      15,338  ns/op
startsWith_searchTrie                              2000  avgt   10       574,396 ±     230,360  ns/op
startsWith_sortedList                              2000  avgt   10      5720,545 ±     100,816  ns/op
startsWith_naiveList                               2000  avgt   10      6989,621 ±    4057,306  ns/op
startsWith_sortedFlatListPrefixMatcher             2000  avgt   10     15490,024 ±    5730,777  ns/op
startsWith_regex_singlePatternForAll               2000  avgt   10     15617,403 ±    6415,207  ns/op
startsWith_regex_patternPerPrefix                  2000  avgt   10     94147,151 ±    6256,534  ns/op
exact_hashSet_exact_match                        200000  avgt   10        20,245 ±       3,517  ns/op
baseline_getRandomString                         200000  avgt   10        29,318 ±      44,592  ns/op
exact_sortedList_binarySearch                    200000  avgt   10       108,245 ±      24,213  ns/op
startsWith_searchTrie_flatMemory                 200000  avgt   10       326,953 ±      33,065  ns/op
startsWith_searchTrie                            200000  avgt   10       433,008 ±       6,085  ns/op
startsWith_searchTrie_flatMemoryBytes            200000  avgt   10       676,589 ±      22,957  ns/op
startsWith_sortedFlatListPrefixMatcher           200000  avgt   10    796491,375 ±   15278,104  ns/op
startsWith_regex_singlePatternForAll             200000  avgt   10   1762045,600 ±  280655,518  ns/op
startsWith_naiveList                             200000  avgt   10   1992584,482 ±  290072,757  ns/op
startsWith_sortedList                            200000  avgt   10   3464193,774 ±   56440,174  ns/op
startsWith_regex_patternPerPrefix                200000  avgt   10  16015211,520 ± 5787761,317  ns/op

Units: ns/op                                  20       200      2000       200000
baseline_getRandomString                      15        15        16           29
exact_hashSet_exact_match                     23        19        27           20
exact_sortedList_binarySearch                 34        42        51          108
startsWith_searchTrie_flatMemory             248       455       332          327
startsWith_searchTrie_flatMemoryBytes        564       531       545          677
startsWith_searchTrie                        453       590       574          433
startsWith_regex_singlePatternForAll         296       769    15.617    1.762.046
startsWith_sortedList                         91       844     5.721    3.464.194
startsWith_sortedFlatListPrefixMatcher       216       878    15.490      796.491
startsWith_naiveList                         102     1.012     6.990    1.992.584
startsWith_regex_patternPerPrefix          1.061     8.900    94.147   16.015.212

Units: ns/op                                  20       200      2000       200000
startsWith_naiveList                         102     1.012     6.990    1.992.584
startsWith_sortedList                         91       844     5.721    3.464.194
startsWith_searchTrie                        453       590       574          433
startsWith_searchTrie_flatMemory             248       455       332          327
startsWith_regex_singlePatternForAll         296       769    15.617    1.762.046
startsWith_regex_patternPerPrefix          1.061     8.900    94.147   16.015.212
exact_hashSet_exact_match                     23        19        27           20

Process finished with exit code 0


Units: ns/op                                  20    200   2.000     200.000
baseline_getRandomString                      15     15      15          15  # sanity check
exact_hashSet_exact_match                     23     17      19          38  
exact_sortedList_binarySearch                 34     38      47         166
startsWith_naiveList                         129    525   4.810   9.284.089  # WINNER for small number of prefixes  - easy and simple
startsWith_sortedList                        128    617   5.583  16.181.950  # (Analysis: sorting shifted the matching pattern towards the end of the list, "unsorted": pattern is exactly in the middle. So we are seeing O(n/2) vs O(n))
startsWith_searchTrie                        455    456     418         948  # WINNER for lager number of prefixes
startsWith_searchTrie_flatMemory             258    328     477        1274  # memory efficient but no gain in perf - if mem efficiency is not paramount, dont use it.
startsWith_eclipseJettyArrayTernaryTrie      265    631     722     (error)  # surprisingly bad - the naive textbook trie implementation outperforms it. 
startsWith_sortedFlatListPrefixMatcher       207    957  12.635   2.530.310  # good intention - bad outcome



Benchmark                                  (prefixesCount)  Mode  Cnt      Score      Error  Units
baseline_getRandomString                                20  avgt   10     15,141 ±    0,387  ns/op
exact_hashSet_exact_match                               20  avgt   10     22,751 ±    8,787  ns/op
exact_sortedList_binarySearch                           20  avgt   10     33,512 ±    1,997  ns/op
startsWith_sortedList                                   20  avgt   10    127,997 ±   76,126  ns/op # WINNER for small number of prefixes  - easy and simple
startsWith_naiveList                                    20  avgt   10    128,951 ±   50,368  ns/op
startsWith_sortedFlatListPrefixMatcher                  20  avgt   10    207,299 ±   15,243  ns/op # good intention - bad outcome -> jvm has optimized string compare intrinsics, which we break by this "optimization"
startsWith_searchTrie_flatMemory                        20  avgt   10    258,326 ±    4,415  ns/op
startsWith_eclipseJettyArrayTernaryTrie                 20  avgt   10    265,294 ±   24,391  ns/op # its "pre-made", so nothing you have to code/test/maintain but its surprisingly bad and looses against the naive textbook trie implementation on higher prefix counts
startsWith_searchTrie                                   20  avgt   10    454,521 ±    8,163  ns/op

baseline_getRandomString                               200  avgt   10     15,498 ±    1,439  ns/op
exact_hashSet_exact_match                              200  avgt   10     17,300 ±    0,419  ns/op
exact_sortedList_binarySearch                          200  avgt   10     37,645 ±    2,121  ns/op
startsWith_searchTrie_flatMemory                       200  avgt   10    328,183 ±   33,577  ns/op # good intention - bad outcome - dont use (to complicated for marginal/no perf benefits)
startsWith_searchTrie                                  200  avgt   10    455,674 ±    4,819  ns/op # WINNER for lager number of prefixes
startsWith_naiveList                                   200  avgt   10    525,358 ±    3,986  ns/op
startsWith_sortedList                                  200  avgt   10    617,149 ±    6,523  ns/op
startsWith_eclipseJettyArrayTernaryTrie                200  avgt   10    631,176 ±  230,177  ns/op # its "pre-made", so nothing you have to code/test/maintain. But its surprisingly bad and looses against the naive textbook trie implementation on higher prefix counts
startsWith_sortedFlatListPrefixMatcher                 200  avgt   10    956,909 ±   10,492  ns/op

baseline_getRandomString                              2000  avgt   10     14,972 ±    0,037  ns/op
exact_hashSet_exact_match                             2000  avgt   10     18,898 ±    0,942  ns/op
exact_sortedList_binarySearch                         2000  avgt   10     46,605 ±    0,971  ns/op
startsWith_searchTrie                                 2000  avgt   10    418,372 ±    2,889  ns/op # WINNER for lager number of prefixes
startsWith_searchTrie_flatMemory                      2000  avgt   10    476,511 ±  200,052  ns/op # good intention - bad outcome - dont use (to complicated for marginal/no perf benefits)
startsWith_eclipseJettyArrayTernaryTrie               2000  avgt   10    721,776 ±   36,279  ns/op # its "pre-made", so nothing you have to code/test/maintain. But its surprisingly bad and looses against the naive textbook trie implementation on higher prefix counts
startsWith_naiveList                                  2000  avgt   10   4809,833 ±   45,281  ns/op
startsWith_sortedList                                 2000  avgt   10   5583,300 ±  210,685  ns/op
startsWith_sortedFlatListPrefixMatcher                2000  avgt   10  12634,780 ± 5686,339  ns/op  # good intention - bad outcome ->

baseline_getRandomString                            200000  avgt   10        32,523 ±       1,647  ns/op
exact_hashSet_exact_match                           200000  avgt   10        37,931 ±       4,340  ns/op
exact_sortedList_binarySearch                       200000  avgt   10       165,704 ±       9,452  ns/op
startsWith_searchTrie                               200000  avgt   10       947,674 ±     336,935  ns/op # now trie's are really shining
startsWith_searchTrie_flatMemory                    200000  avgt   10      1273,593 ±     417,205  ns/op
startsWith_sortedFlatListPrefixMatcher              200000  avgt   10   2530309,848 ±  485285,833  ns/op # and the "flatList" is picking up as well - but still the wrong tool for the job
startsWith_naiveList                                200000  avgt   10   9284088,519 ± 9301048,621  ns/op # welp, we constructed the initial test set so, that the search string is placed exactly in the middle -> so we are certain to hit it in O(n/2)
startsWith_sortedList                               200000  avgt   10  16181949,527 ± 6841858,186  ns/op # somehow sorting shifted matching pattern towards the end, and we are seeing the worst case O(n)
startsWith_eclipseJettyArrayTernaryTrie             --unable to start, takes forever to initialize --

 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Threads(1)
@Fork(1) //if you promise to not use this machine during benchmarking->1 is ok. Watching youtube?-> set to 3
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)//read @Fork comment - consider setting to 10
@State(Scope.Thread)
public class PrefixSearchBlacklistJMH {


    String aExactMatchString = "foo.bar.foobar.askjldfasflasdfasfasdfasd.fasdfasdfasfasfsaf.MATCHING";
    String aContainsString = "foo.bar.foobar.askjldfasflasdfasfasdfasd.fasdfasdfasfasfsaf.MATCHING.someOtherPackages.whatever.orAClass";
    String almostMatching = "foo.bar.foobar.askjldfasflasdfasfasdfasd.fasdfasdfasfasfsaf.NOT_MATCHING.someOtherPackages.whatever.orAClass";
    String notMatching = "xxxxdifferentStart.foo.bar.foobar.askjldfasflasdfasfasdfasd.fasdfasdfasfasfsaf.NOT_MATCHING";

    @Param({
            "20",
            "200",
            "2000",
            "200000" //at this size, consider employing a probabilistic pre-filter (e.g. bloomfilter) to reduce problem domain
    })
    int prefixesCount = 200;

    // @Param({
    //         "0.0",
    //         "0.5",
    //         "1.0"
    // })
    double isAMatchChance = 0.5;


    //@Param({
    //        "0.0",
    //        "0.5",
    //        "1.0"
    //})
    double exactMatchChance = 0.5; //1== the prefix matches exactly (prefix.equals(toCheck)), 0== the prefix is contained toCheck.contains(prefix)


    // @Param({
    //         "0.0",
    //         "0.5",
    //         "1.0"
    // })
    //most of the prefix matches (so a search graph cannot terminate on the first node)
    double almostMatchChance = 0.5;


    private NaiveListPrefixMatcher naiveListPrefixMatcher;
    private SortedListPrefixMatcher sortedListPrefixMatcher;
    private SortedListBinarySearchExactMatchPrefixMatcher sortedListBinarySearchExactMatchPrefixMatcher;
    private HashSetExactMatch hashSetExactMatch;
    private SimpleTriePrefixMatch simpleTriePrefixMatch;
    private EclipseJettyArrayTernaryTrie eclipseJettyArrayTernaryTrie;
    private SortedFlattenedListPrefixMatcher sortedFlatListPrefixMatcher;

    private RuntimeTrie runtimeTrie;

    private static final char[] ALPHABET = "abcdefghijklmnopqrstuvwxyzABCEDFGHIJKLMNOPQRSTUVWXYZ01234567890.".toCharArray();
    private RuntimeTrieBytes runtimeTrieBytes;
    private RegexPatternsMatcher2 regexMatcher_singlePatternForAll;
    private RegexPatternsMatcher regexMatcher_patternPerPrefix;

    @Setup
    public void setup() {
        StopWatch s = new StopWatch();
        s.start();
        ArrayList<String> prefixes = new ArrayList<>();

        //fill up prefix filter list;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < prefixesCount; i++) {
            if (i == prefixesCount / 2) {
                //strategically place the only matching prefix pattern into the middle of the list - some naive implementations (like unsorted list search) are sensitive regrading the position.
                prefixes.add("foo.bar.foobar.askjldfasflasdfasfasdfasd.fasdfasdfasfasfsaf.MATCHING");
            } else {
                prefixes.add(RandomUtils.randomString(r.nextInt(10, 50), r, ALPHABET).toString());
            }
        }
        s.stop();
        System.out.println("generated prefixes: " + s);
        this.naiveListPrefixMatcher = new NaiveListPrefixMatcher(prefixes); //"unsorted"
        this.sortedListPrefixMatcher = new SortedListPrefixMatcher(prefixes);
        this.sortedFlatListPrefixMatcher = new SortedFlattenedListPrefixMatcher(prefixes); //is shit!
        this.sortedListBinarySearchExactMatchPrefixMatcher = new SortedListBinarySearchExactMatchPrefixMatcher(prefixes);
        this.hashSetExactMatch = new HashSetExactMatch(prefixes);
        this.simpleTriePrefixMatch = stopWatch("new SimpleTriePrefixMatch", () -> new SimpleTriePrefixMatch(prefixes));
        this.runtimeTrie = stopWatch("new RuntimeTrie", () -> RuntimeTrie.constructFrom(simpleTriePrefixMatch.root));
        this.runtimeTrieBytes = stopWatch("new runtimeTrieBytes", () -> RuntimeTrieBytes.constructFrom(simpleTriePrefixMatch.root));
        this.regexMatcher_patternPerPrefix = new RegexPatternsMatcher(prefixes); //"unsorted"
        this.regexMatcher_singlePatternForAll = new RegexPatternsMatcher2(prefixes); //"unsorted"

        //    this.eclipseJettyArrayTernaryTrie = stopWatch(" newEclipseJettyArrayTeneraryTrie",()-> newEclipseJettyArrayTeneraryTrie(prefixes));
    }


    private <T> T stopWatch(String info, Supplier<T> o) {
        StopWatch s = new StopWatch();
        s.start();
        T r = o.get();
        s.stop();
        System.out.println(info + " took:" + s);
        return r;
    }

    private static EclipseJettyArrayTernaryTrie newEclipseJettyArrayTeneraryTrie(ArrayList<String> prefixes) {
        BiFunction<List<String>, Integer, EclipseJettyArrayTernaryTrie<String>> supplier = (prefixesList, cap) -> {
            EclipseJettyArrayTernaryTrie newTrie = new EclipseJettyArrayTernaryTrie<String>(false, cap);
            for (int i = 0; i < prefixesList.size(); i++) {
                String p = prefixesList.get(i);
                if (!newTrie.put(p, p)) {

                    System.err.println("Trie with capacity " + cap + " is to small, failed to add prefix  " + i + " of " + prefixes.size() + "  uniqueChars: " + prefixes.stream().flatMapToInt(String::chars).distinct().count() + " sumLenAllPrefixes(worst case upper bound): " + prefixes.stream().mapToInt(String::length).sum());
                    return null;
                }

            }
            return newTrie;
        };


        int capacity = prefixes.stream().mapToInt(String::length).sum();
        capacity += (int) Math.max(capacity * 0.05, 15);//for whatever reason, the "worst case upperbound" is still exceed - add a bit of wiggle room on top

        while (true) { //i am to lazy to write a better size estimation - "re-grow on error" will do for this test.
            EclipseJettyArrayTernaryTrie newTrie = supplier.apply(prefixes, capacity);
            if (newTrie == null) {
                capacity += 10; //very conservative grow! in production use capacity *=2
            } else {
                return newTrie;
            }
        }
    }

    String getRandomTestString() {
        final ThreadLocalRandom r = ThreadLocalRandom.current();
        return r.nextDouble() < isAMatchChance
                //match
                ? (r.nextDouble() < exactMatchChance ? aExactMatchString : aContainsString)
                //non match
                : (r.nextDouble() < almostMatchChance ? almostMatching : notMatching);
    }


    @Benchmark
    public String baseline_getRandomString() {
        return getRandomTestString();
    }

    @Benchmark
    public boolean startsWith_naiveList() {
        String in = getRandomTestString();
        return naiveListPrefixMatcher.test(in);
    }

     @Benchmark
    public boolean startsWith_regex_patternPerPrefix() {
        String in = getRandomTestString();
        return regexMatcher_patternPerPrefix.test(in);
    }
    @Benchmark
    public boolean startsWith_regex_singlePatternForAll() {
        String in = getRandomTestString();
        return regexMatcher_singlePatternForAll.test(in);
    }

    @Benchmark
    public boolean startsWith_sortedList() {
        String in = getRandomTestString();
        return sortedListPrefixMatcher.test(in);
    }

    @Benchmark
    public boolean startsWith_sortedFlatListPrefixMatcher() {
        String in = getRandomTestString();
        return sortedFlatListPrefixMatcher.test(in);
    }

    @Benchmark
    public boolean exact_sortedList_binarySearch() {
        String in = getRandomTestString();
        return sortedListBinarySearchExactMatchPrefixMatcher.test(in);
    }

    @Benchmark
    public boolean startsWith_searchTrie() {
        String in = getRandomTestString();
        return simpleTriePrefixMatch.test(in);
    }

    @Benchmark
    public boolean startsWith_searchTrie_flatMemory() {
        String in = getRandomTestString();
        return runtimeTrie.startsWith(in);
    }
    @Benchmark
    public boolean startsWith_searchTrie_flatMemoryBytes() {
        String in = getRandomTestString();
        return runtimeTrieBytes.startsWith(in);
    }
    @Benchmark
    public boolean exact_hashSet_exact_match() {
        String in = getRandomTestString();
        return hashSetExactMatch.test(in);
    }

    //@Benchmark
    //public boolean startsWith_eclipseJettyArrayTernaryTrie() {
    //    String in = getRandomTestString();
    //    return eclipseJettyArrayTernaryTrie.get(in) != null;
    //}


    public interface PrefixMatcher extends Predicate<String> {

    }


    public static class NaiveListPrefixMatcher implements PrefixMatcher {

        private final ArrayList<String> prefixes;

        public NaiveListPrefixMatcher(List<String> prefixes) {
            this.prefixes = new ArrayList<>(prefixes);//defensive copy
        }

        @Override
        public boolean test(String toTest) {
            for (String prefix : prefixes) {
                if (toTest.startsWith(prefix)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static class RegexPatternsMatcher implements PrefixMatcher {

        private final List<Pattern> prefixes;

        public RegexPatternsMatcher(List<String> prefixes) {
            this.prefixes = prefixes.stream().map(RegexPatternsMatcher::startsWithToRegex).collect(Collectors.toList());
        }

        @Override
        public boolean test(String toTest) {
            for (Pattern prefix : prefixes) {
                if (prefix.matcher(toTest).matches()) {
                    return true;
                }
            }
            return false;
        }


        private static Pattern startsWithToRegex(String s) {
            return Pattern.compile("^" + Pattern.quote(s)  + ".*");
        }

    }

    public static class RegexPatternsMatcher2 implements PrefixMatcher {

        private final Pattern prefixes;

        public RegexPatternsMatcher2(List<String> prefixes) {
                    String regex= prefixes.stream()
                            .map(Pattern::quote)
                            .collect(Collectors.joining("|","^(" ,").*"));
            this.prefixes  = Pattern.compile(regex);
        }

        @Override
        public boolean test(String toTest) {
            return prefixes.matcher(toTest).matches();
        }

    }



    public static class SortedListPrefixMatcher implements PrefixMatcher {

        private final ArrayList<String> prefixes;

        public SortedListPrefixMatcher(Collection<String> input) {
            this.prefixes = new ArrayList<>(input); //defensive copy
            this.prefixes.sort(Comparator.naturalOrder());
        }

        @Override
        public boolean test(String toTest) {
            for (String prefix : prefixes) {
                if (toTest.startsWith(prefix)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static class SortedListBinarySearchExactMatchPrefixMatcher implements PrefixMatcher {

        private final ArrayList<String> prefixes;

        public SortedListBinarySearchExactMatchPrefixMatcher(List<String> prefixes) {
            this.prefixes = new ArrayList<>(prefixes);
            prefixes.sort(Comparator.naturalOrder());
        }

        @Override
        public boolean test(String toTest) {
            //positive values: the found index, negative values: the -<insertionIndex>
            //Excerpt: "[..] the return value will be >= 0 if and only if the key is found.
            return Collections.binarySearch(prefixes, toTest, Comparator.naturalOrder()) >= 0;
        }
    }

    public static class HashSetExactMatch implements PrefixMatcher {

        private final HashSet<String> prefixes;

        public HashSetExactMatch(List<String> prefixes) {
            this.prefixes = new HashSet<>(prefixes);
        }

        @Override
        public boolean test(String toTest) {
            return prefixes.contains(toTest);
        }
    }

    public static class SortedFlattenedListPrefixMatcher implements PrefixMatcher {

        private final char[] prefixes; //flattened 2d char array char[prefixes][prefix]

        public SortedFlattenedListPrefixMatcher(Collection<String> input) {
            ArrayList<String> tmp = new ArrayList<>(input);//defensive copy
            tmp.sort(Comparator.naturalOrder());
            int len = tmp.stream().mapToInt(s -> s.length() + 1).sum();
            this.prefixes = new char[len];
            int insertPos = 0;
            for (String prefix : tmp) {
                this.prefixes[insertPos++] = (char) prefix.length(); //jep, thats dirty
                for (int i = 0; i < prefix.length(); i++) {
                    this.prefixes[insertPos++] = prefix.charAt(i);
                }

            }
        }

        @Override
        public boolean test(String toTest) {
            int curPrefix = 0;
            //e.g. 2 prefixes: "foo" and "test";
            //IDX:      0,1,2,3,4,5,6,7,8
            //prefixes: 3,f,o,o,4,T,e,s,t
            while (curPrefix < prefixes.length) {
                //len of first stored prefix
                int lenPrefix = prefixes[curPrefix];
                int curPrefixStart = curPrefix + 1;
                //test prefix
                if (_tokenStartsWithCurrentPrefix(toTest, curPrefixStart, lenPrefix)) {
                    return true;
                } else {
                    curPrefix = curPrefix + 1 /*sizeStore*/ + lenPrefix;//next prefix store position
                }
            }
            return false;
        }

        private boolean _tokenStartsWithCurrentPrefix(String toTest, int start, int len) {
            if (toTest.length() < len) {
                return false;
            }

            //FIXME performance penalty! suffers heavily from char-by-char comparison. The JVM usually has a native optimized drop-in replacement with SIMD instructions for string compare e.g. "startsWith"
            for (int i = 0; i < len; i++) {
                if (prefixes[start + i] != toTest.charAt(i)) {
                    return false; //no match
                }
            }
            return true; //starts with prefix
        }
    }


    /**
     * Simple text book trie implementation
     */
    public static class SimpleTriePrefixMatch implements PrefixMatcher {

        static class TrieNode {

            private final Map<Character, TrieNode> children = new HashMap<>();
            private boolean endOfWord;

            Map<Character, TrieNode> getChildren() {
                return children;
            }

            boolean isEndOfWord() {
                return endOfWord;
            }

            void setEndOfWord(boolean endOfWord) {
                this.endOfWord = endOfWord;
            }
        }


        private final TrieNode root;

        public SimpleTriePrefixMatch(Collection<String> input) {
            root = new TrieNode();
            for (String token : input) {
                insert(token);
            }
        }


        @Override
        public boolean test(String toTest) {
            return startsWith(toTest);
        }

        public boolean startsWith(String word) {
            TrieNode current = root;

            for (int i = 0; i < word.length(); i++) {
                char ch = word.charAt(i);
                TrieNode node = current.getChildren().get(ch);
                if (node == null) {
                    return false;
                } else if (node.isEndOfWord()) {
                    return true;
                }
                current = node;
            }
            return current.isEndOfWord();
        }


        public boolean containsExactly(String word) {
            TrieNode current = root;

            for (int i = 0; i < word.length(); i++) {
                char ch = word.charAt(i);
                TrieNode node = current.getChildren().get(ch);
                if (node == null) {
                    return false;
                }
                current = node;
            }
            return current.isEndOfWord();
        }

        void insert(String word) {
            TrieNode current = root;

            for (char l : word.toCharArray()) {
                current = current.getChildren().computeIfAbsent(l, c -> new TrieNode());
            }
            current.setEndOfWord(true);
        }

        boolean delete(String word) {
            return delete(root, word, 0);
        }

        boolean isEmpty() {
            return root == null;
        }

        private boolean delete(TrieNode current, String word, int index) {
            if (index == word.length()) {
                if (!current.isEndOfWord()) {
                    return false;
                }
                current.setEndOfWord(false);
                return current.getChildren().isEmpty();
            }
            char ch = word.charAt(index);
            TrieNode node = current.getChildren().get(ch);
            if (node == null) {
                return false;
            }
            boolean shouldDeleteCurrentNode = delete(node, word, index + 1) && !node.isEndOfWord();

            if (shouldDeleteCurrentNode) {
                current.getChildren().remove(ch);
                return current.getChildren().isEmpty();
            }
            return false;
        }
    }

    /**
     * Compresses a Trie into a flat int[] in an effort to safe memory AND optimize runtime performance
     * Think of it as "serialize a trie into a int/byte array" - but we can directly operate on this "serialized" state without de-serializing it.
     * This impl loosely follows the "Flyweight" pattern.
     */
    static class RuntimeTrie {

        //flat version of  private final Map<Character, TrieNode> children = new HashMap<>();
        //layout: [TrieNode,TrieNode,TrieNode] TrieNode: [lenChildren, isTerminal, [transitionChars,...],[nextNodeAddress,...]]
        //example: "ab", "ac", "a,b,c"  (ASCII: a=97 b=98, c=99)
        // [0,  1,  2,  3,   4,  5,  6,  7,  8,  9,    10,  11,  12,    13, 14]
        // [1,  0, 97,  4], [2,  0, 98, 99, 10, 13],   [1,   1,  13],   [0,  1]
        // [root->a]        [a->b,c]                     [b->c,term], [c->term]
        private static final int OFFSET_CHILDREN_LENGTH = 0;
        private static final int OFFSET_IS_TERMINAL = 1;
        private static final int OFFSET_TRANSITION_CHARS = 2;
        private static final int OFFSET_NEXTADRESS_CHARS = 2; //relative!  OFFSET_NEXTADRESS_CHARS + lenChildren

        private final int[] memoryPool;
        private int currentNodeAddress;

        RuntimeTrie(int requiredMemSize) {
            this(0, new int[requiredMemSize]);
        }

        RuntimeTrie(int currentNodeAddress, int[] memoryPool) {
            this.currentNodeAddress = currentNodeAddress;
            this.memoryPool = memoryPool;
        }

        public int numChildren() {
            return getInt(currentNodeAddress + OFFSET_CHILDREN_LENGTH);
        }

        public boolean isTerminal() {
            return getInt(currentNodeAddress + OFFSET_IS_TERMINAL) != 0;
        }

        public int getChildChar(int idx) {
            return getInt(currentNodeAddress + OFFSET_TRANSITION_CHARS + idx);
        }

        public int getChildCharNodeAddress(int idx) {
            return getInt(currentNodeAddress + OFFSET_NEXTADRESS_CHARS + numChildren() + idx);
        }

        private int getInt(int i) {
            return memoryPool[i];
        }

        //internal

        private void setNumChildren(int value) {
            setInt(currentNodeAddress + OFFSET_CHILDREN_LENGTH, value);
        }

        private void setIsTerminal(boolean isTerminal) {
            setInt(currentNodeAddress + OFFSET_IS_TERMINAL, isTerminal ? 1 : 0);
        }

        private void setChildChar(int idx, char value) {
            setInt(currentNodeAddress + OFFSET_TRANSITION_CHARS + idx, value);
        }

        private void setChildCharNodeAddress(int idx, int jumpAddress) {
            setInt(currentNodeAddress + OFFSET_NEXTADRESS_CHARS + numChildren() + idx, jumpAddress);
        }

        private void directAddress(int currentNodeAddress) {
            this.currentNodeAddress = currentNodeAddress;
        }

        private int setInt(int address, int value) {
            return memoryPool[address] = value;
        }

        private void advanceToChildNodeAddress(int childNodeIndex) {
            currentNodeAddress = getChildCharNodeAddress(childNodeIndex);
        }

        private void resetToRoot() {
            currentNodeAddress = 0;
        }

        public boolean startsWith(String word) {
            resetToRoot();

            for (int i = 0; i < word.length(); i++) {
                char ch = word.charAt(i);
                int idx = getChildTransitionIdxForCharacter(ch);
                if (idx < 0) {
                    return false; //no match in trie
                }
                //match, advance to next node
                advanceToChildNodeAddress(idx);
                if (isTerminal()) {//current node is terminal. as we perform "startsWith" wea re done here
                    return true;
                }

            }
            return false;
        }

        private int getChildTransitionIdxForCharacter(char ch) {
            int numChildren = numChildren();
            for (int childIDX = 0; childIDX < numChildren; childIDX++) {
                if (ch == getChildChar(childIDX)) {
                    return childIDX;
                }
            }
            return -1;
        }

        /**
         * Converts an existing trie into an optimized trie.
         *
         * @param root
         * @return the optimized trie
         */
        public static RuntimeTrie constructFrom(PrefixSearchBlacklistJMH.SimpleTriePrefixMatch.TrieNode root) {
            HashMap<SimpleTriePrefixMatch.TrieNode, Integer> knownNodeAddresses = new HashMap<>();

            //1st pass: cacl memory requirement for each node, and pre-calc and store flat-memory-address
            int nextWritePointer = 0;//0==root

            //depth first traversal of a graph without recursion
            Deque<SimpleTriePrefixMatch.TrieNode> todoStack = new ArrayDeque<>(); //Stack is based on Vector, an Vector sucks.
            todoStack.push(root);
            while (!todoStack.isEmpty()) {
                SimpleTriePrefixMatch.TrieNode curNode = todoStack.pop();
                if (knownNodeAddresses.containsKey(curNode)) {
                    continue; //we are a graph, so to prevent cycles, we need to skip known nodes
                }

                knownNodeAddresses.put(curNode, nextWritePointer);
                int requiredAdditionalSize = calculateRequiredSize(curNode);
                nextWritePointer += requiredAdditionalSize;

                for (Map.Entry<Character, SimpleTriePrefixMatch.TrieNode> entry : curNode.getChildren().entrySet()) {
                    SimpleTriePrefixMatch.TrieNode child = entry.getValue();
                    if (knownNodeAddresses.containsKey(child)) {
                        continue; //we are a graph, so to prevent cycles, we need to skip known nodes
                    }
                    todoStack.add(child);
                }
            }
            todoStack = null; //cleanup and free references


            //2nd pass serialize nodes into allocated flat memory
            //and we know our final memory size and can allocate it in one junk
            RuntimeTrie node = new RuntimeTrie(nextWritePointer);
            for (Map.Entry<SimpleTriePrefixMatch.TrieNode, Integer> entry : knownNodeAddresses.entrySet()) {

                node.directAddress(entry.getValue()); //directly set the address of current node

                //now serialize the node into flat memory, starting at addressOffset
                SimpleTriePrefixMatch.TrieNode sourceNode = entry.getKey();
                node.setIsTerminal(sourceNode.isEndOfWord());
                node.setNumChildren(sourceNode.getChildren().size());
                int childIDX = 0;
                for (Map.Entry<Character, SimpleTriePrefixMatch.TrieNode> childDetails : sourceNode.getChildren().entrySet()) {
                    node.setChildChar(childIDX, childDetails.getKey());
                    node.setChildCharNodeAddress(childIDX, knownNodeAddresses.get(childDetails.getValue()));
                    childIDX++;
                }
            }
            node.resetToRoot();
            return node;
        }

        private static int[] growIfRequired(int[] arena, int memoryPointer, int requiredAdditionalSize) {
            if (memoryPointer + requiredAdditionalSize >= arena.length) {
                int newSize = Math.max(memoryPointer + requiredAdditionalSize, (arena.length + (arena.length /* >> 1*/)));
                return Arrays.copyOf(arena, newSize);
            }

            return arena;
        }

        private static int calculateRequiredSize(PrefixSearchBlacklistJMH.SimpleTriePrefixMatch.TrieNode curNode) {
            return 2 + curNode.children.size() * 2;
        }
    }

    static class RuntimeTrieBytes {

        //flat version of  private final Map<Character, TrieNode> children = new HashMap<>();
        //layout: [TrieNode,TrieNode,TrieNode] TrieNode: [lenChildren, isTerminal, [transitionChars,...],[nextNodeAddress,...]]
        //example: "ab", "ac", "a,b,c"  (ASCII: a=97 b=98, c=99)
        // [0,  1,  2,  3,   4,  5,  6,  7,  8,  9,    10,  11,  12,    13, 14]
        // [1,  0, 97,  4], [2,  0, 98, 99, 10, 13],   [1,   1,  13],   [0,  1]
        // [root->a]        [a->b,c]                     [b->c,term], [c->term]
        private static final int OFFSET_CHILDREN_LENGTH = 0;
        private static final int OFFSET_IS_TERMINAL = OFFSET_CHILDREN_LENGTH + 1 * Integer.BYTES /*+length == 4Bytes*/;
        private static final int OFFSET_TRANSITION_CHARS = OFFSET_IS_TERMINAL + 1 /*Terminal == 1 Byte)*/;


        private static final int OFFSET_NEXTADRESS_CHARS = OFFSET_TRANSITION_CHARS; //relative!  OFFSET_NEXTADRESS_CHARS + lenChildren * Charater.Bytes

        private final byte[] memoryPool;
        private int currentNodeAddress;

        RuntimeTrieBytes(int requiredMemSize) {
            this(0, new byte[requiredMemSize]);
        }

        RuntimeTrieBytes(int currentNodeAddress, byte[] memoryPool) {
            this.currentNodeAddress = currentNodeAddress;
            this.memoryPool = memoryPool;
        }

        public int numChildren() {
            return getInt(currentNodeAddress + OFFSET_CHILDREN_LENGTH);
        }

        private void setNumChildren(int value) {
            setInt(currentNodeAddress + OFFSET_CHILDREN_LENGTH, value);
        }

        public boolean isTerminal() {
            return getBoolean(currentNodeAddress + OFFSET_IS_TERMINAL);
        }

        private void setIsTerminal(boolean isTerminal) {
            setBoolean(currentNodeAddress + OFFSET_IS_TERMINAL, isTerminal);
        }


        public int getChildChar(int idx) {
            return getChar(currentNodeAddress + OFFSET_TRANSITION_CHARS + idx * Character.BYTES);
        }

        private void setChildChar(int idx, char value) {
            setChar(currentNodeAddress + OFFSET_TRANSITION_CHARS + idx * Character.BYTES, value);
        }

        public int getChildCharNodeAddress(int idx) {
            return getInt(currentNodeAddress + OFFSET_NEXTADRESS_CHARS + numChildren() * Character.BYTES + idx * Integer.BYTES);
        }

        private void setChildCharNodeAddress(int idx, int jumpAddress) {
            setInt(currentNodeAddress + OFFSET_NEXTADRESS_CHARS + numChildren() * Character.BYTES + idx * Integer.BYTES, jumpAddress);
        }


        private void directAddress(int currentNodeAddress) {
            this.currentNodeAddress = currentNodeAddress;
        }


        private int getInt(int offset) {
            return ((memoryPool[offset] & 0xFF) << 24) |
                    ((memoryPool[offset + 1] & 0xFF) << 16) |
                    ((memoryPool[offset + 2] & 0xFF) << 8) |
                    ((memoryPool[offset + 3] & 0xFF) << 0);
        }

        private void setInt(int offset, int value) {
            memoryPool[offset] = (byte) (value >> 24);
            memoryPool[offset + 1] = (byte) (value >> 16);
            memoryPool[offset + 2] = (byte) (value >> 8);
            memoryPool[offset + 3] = (byte) value;
        }

        ;


        private int getChar(int i) {
            return ((memoryPool[i] & 0xFF) << 8) |
                    ((memoryPool[i + 1] & 0xFF) << 0);
        }

        private void setChar(int offset, char value) {
            memoryPool[offset + 0] = (byte) (value >> 8);
            memoryPool[offset + 1] = (byte) value;
        }

        private boolean getBoolean(int i) {
            return memoryPool[i] != 0x0;
        }

        private void setBoolean(int offset, boolean value) {
            memoryPool[offset] = value ? (byte) 0xff : (byte) 0x0;
        }


        private void advanceToChildNodeAddress(int childNodeIndex) {
            currentNodeAddress = getChildCharNodeAddress(childNodeIndex);
        }

        private void resetToRoot() {
            currentNodeAddress = 0;
        }

        public boolean startsWith(String word) {
            resetToRoot();

            for (int i = 0; i < word.length(); i++) {
                char ch = word.charAt(i);
                int idx = getChildTransitionIdxForCharacter(ch);
                if (idx < 0) {
                    return false; //no match in trie
                }
                //match, advance to next node
                advanceToChildNodeAddress(idx);
                if (isTerminal()) {//current node is terminal. as we perform "startsWith" wea re done here
                    return true;
                }

            }
            return false;
        }

        private int getChildTransitionIdxForCharacter(char ch) {
            int numChildren = numChildren();
            for (int childIDX = 0; childIDX < numChildren; childIDX++) {
                if (ch == getChildChar(childIDX)) {
                    return childIDX;
                }
            }
            return -1;
        }

        /**
         * Converts an existing trie into an optimized trie.
         *
         * @param root
         * @return the optimized trie
         */
        public static RuntimeTrieBytes constructFrom(PrefixSearchBlacklistJMH.SimpleTriePrefixMatch.TrieNode root) {
            HashMap<SimpleTriePrefixMatch.TrieNode, Integer> knownNodeAddresses = new HashMap<>();

            //1st pass: cacl memory requirement for each node, and pre-calc and store flat-memory-address
            int nextWritePointer = 0;//0==root

            //depth first traversal of a graph without recursion
            Deque<SimpleTriePrefixMatch.TrieNode> todoStack = new ArrayDeque<>(); //Stack is based on Vector, an Vector sucks.
            todoStack.push(root);
            while (!todoStack.isEmpty()) {
                SimpleTriePrefixMatch.TrieNode curNode = todoStack.pop();
                if (knownNodeAddresses.containsKey(curNode)) {
                    continue; //we are a graph, so to prevent cycles, we need to skip known nodes
                }

                knownNodeAddresses.put(curNode, nextWritePointer);
                int requiredAdditionalSize = calculateRequiredSize(curNode);
                nextWritePointer += requiredAdditionalSize;

                for (Map.Entry<Character, SimpleTriePrefixMatch.TrieNode> entry : curNode.getChildren().entrySet()) {
                    SimpleTriePrefixMatch.TrieNode child = entry.getValue();
                    if (knownNodeAddresses.containsKey(child)) {
                        continue; //we are a graph, so to prevent cycles, we need to skip known nodes
                    }
                    todoStack.add(child);
                }
            }
            todoStack = null; //cleanup and free references


            //2nd pass serialize nodes into allocated flat memory
            //and we know our final memory size and can allocate it in one junk
            RuntimeTrieBytes node = new RuntimeTrieBytes(nextWritePointer);
            for (Map.Entry<SimpleTriePrefixMatch.TrieNode, Integer> entry : knownNodeAddresses.entrySet()) {

                node.directAddress(entry.getValue()); //directly set the address of current node

                //now serialize the node into flat memory, starting at addressOffset
                SimpleTriePrefixMatch.TrieNode sourceNode = entry.getKey();
                node.setNumChildren(sourceNode.getChildren().size());
                node.setIsTerminal(sourceNode.isEndOfWord());
                int childIDX = 0;
                for (Map.Entry<Character, SimpleTriePrefixMatch.TrieNode> childDetails : sourceNode.getChildren().entrySet()) {
                    node.setChildChar(childIDX, childDetails.getKey());
                    node.setChildCharNodeAddress(childIDX, knownNodeAddresses.get(childDetails.getValue()));
                    childIDX++;
                }
            }
            node.resetToRoot();
            return node;
        }


        private static int calculateRequiredSize(PrefixSearchBlacklistJMH.SimpleTriePrefixMatch.TrieNode curNode) {
            return 1 * Integer.BYTES /*len of transitions in int 4 bytes*/
                    + 1 /*boolean 1 byte*/
                    + curNode.children.size() * Integer.BYTES/* jump indexes in integer*/
                    + curNode.children.size() * Character.BYTES/*transition chars*/
                    ;
        }
    }

    // i was to lazy to add jetty as a dependency. As licence permits it: here is a copy paste
    //  ========================================================================
    //  Copyright (c) 1995-2016 Mort Bay Consulting Pty. Ltd.
    //  ------------------------------------------------------------------------
    //  All rights reserved. This program and the accompanying materials
    //  are made available under the terms of the Eclipse Public License v1.0
    //  and Apache License v2.0 which accompanies this distribution.
    //
    //      The Eclipse Public License is available at
    //      http://www.eclipse.org/legal/epl-v10.html
    //
    //      The Apache License v2.0 is available at
    //      http://www.opensource.org/licenses/apache2.0.php
    //
    //  You may elect to redistribute this code under either of these licenses.
    //  ========================================================================
    //
    public static class EclipseJettyArrayTernaryTrie<V> {
        private static int LO = 1;
        private static int EQ = 2;
        private static int HI = 3;

        /**
         * The Size of a Trie row is the char, and the low, equal and high
         * child pointers
         */
        private static final int ROW_SIZE = 4;

        /**
         * The Trie rows in a single array which allows a lookup of row,character
         * to the next row in the Trie.  This is actually a 2 dimensional
         * array that has been flattened to achieve locality of reference.
         */
        private final char[] _tree;

        /**
         * The key (if any) for a Trie row.
         * A row may be a leaf, a node or both in the Trie tree.
         */
        private final String[] _key;

        /**
         * The value (if any) for a Trie row.
         * A row may be a leaf, a node or both in the Trie tree.
         */
        private final V[] _value;

        /**
         * The number of rows allocated
         */
        private char _rows;

        final boolean _caseInsensitive;

        /* ------------------------------------------------------------ */

        /**
         * Create a case insensitive Trie of default capacity.
         */
        public EclipseJettyArrayTernaryTrie() {
            this(128);
        }

        /* ------------------------------------------------------------ */

        /**
         * Create a Trie of default capacity
         *
         * @param insensitive true if the Trie is insensitive to the case of the key.
         */
        public EclipseJettyArrayTernaryTrie(boolean insensitive) {
            this(insensitive, 128);
        }

        /* ------------------------------------------------------------ */

        /**
         * Create a case insensitive Trie
         *
         * @param capacity The capacity of the Trie, which is in the worst case
         *                 is the total number of characters of all keys stored in the Trie.
         *                 The capacity needed is dependent of the shared prefixes of the keys.
         *                 For example, a capacity of 6 nodes is required to store keys "foo"
         *                 and "bar", but a capacity of only 4 is required to
         *                 store "bar" and "bat".
         */
        public EclipseJettyArrayTernaryTrie(int capacity) {
            this(true, capacity);
        }

        /* ------------------------------------------------------------ */

        /**
         * Create a Trie
         *
         * @param insensitive true if the Trie is insensitive to the case of the key.
         * @param capacity    The capacity of the Trie, which is in the worst case
         *                    is the total number of characters of all keys stored in the Trie.
         *                    The capacity needed is dependent of the shared prefixes of the keys.
         *                    For example, a capacity of 6 nodes is required to store keys "foo"
         *                    and "bar", but a capacity of only 4 is required to
         *                    store "bar" and "bat".
         */
        public EclipseJettyArrayTernaryTrie(boolean insensitive, int capacity) {
            this._caseInsensitive = insensitive;
            _value = (V[]) new Object[capacity];
            _tree = new char[capacity * ROW_SIZE];
            _key = new String[capacity];
        }

        /* ------------------------------------------------------------ */

        /**
         * Copy Trie and change capacity by a factor
         *
         * @param trie   the trie to copy from
         * @param factor the factor to grow the capacity by
         */
        public EclipseJettyArrayTernaryTrie(EclipseJettyArrayTernaryTrie<V> trie, double factor) {
            this._caseInsensitive = trie.isCaseInsensitive();
            int capacity = (int) (trie._value.length * factor);
            _rows = trie._rows;
            _value = Arrays.copyOf(trie._value, capacity);
            _tree = Arrays.copyOf(trie._tree, capacity * ROW_SIZE);
            _key = Arrays.copyOf(trie._key, capacity);
        }

        /* ------------------------------------------------------------ */
        public void clear() {
            _rows = 0;
            Arrays.fill(_value, null);
            Arrays.fill(_tree, (char) 0);
            Arrays.fill(_key, null);
        }

        /* ------------------------------------------------------------ */
        public boolean put(String s, V v) {
            int t = 0;
            int limit = s.length();
            int last = 0;
            for (int k = 0; k < limit; k++) {
                char c = s.charAt(k);
                if (isCaseInsensitive() && c < 128)
                    c = Character.toLowerCase(c);

                while (true) {
                    int row = ROW_SIZE * t;

                    // Do we need to create the new row?
                    if (t == _rows) {
                        _rows++;
                        if (_rows >= _key.length) {
                            _rows--;
                            return false;
                        }
                        _tree[row] = c;
                    }

                    char n = _tree[row];
                    int diff = n - c;
                    if (diff == 0)
                        t = _tree[last = (row + EQ)];
                    else if (diff < 0)
                        t = _tree[last = (row + LO)];
                    else
                        t = _tree[last = (row + HI)];

                    // do we need a new row?
                    if (t == 0) {
                        t = _rows;
                        _tree[last] = (char) t;
                    }

                    if (diff == 0)
                        break;
                }
            }

            // Do we need to create the new row?
            if (t == _rows) {
                _rows++;
                if (_rows >= _key.length) {
                    _rows--;
                    return false;
                }
            }

            // Put the key and value
            _key[t] = v == null ? null : s;
            _value[t] = v;

            return true;
        }


        /* ------------------------------------------------------------ */
        public V get(String s, int offset, int len) {
            int t = 0;
            for (int i = 0; i < len; ) {
                char c = s.charAt(offset + i++);
                if (isCaseInsensitive() && c < 128)
                    c = Character.toLowerCase(c);

                while (true) {
                    int row = ROW_SIZE * t;
                    char n = _tree[row];
                    int diff = n - c;

                    if (diff == 0) {
                        t = _tree[row + EQ];
                        if (t == 0)
                            return null;
                        break;
                    }

                    t = _tree[row + hilo(diff)];
                    if (t == 0)
                        return null;
                }
            }

            return _value[t];
        }


        public V get(ByteBuffer b, int offset, int len) {
            int t = 0;
            offset += b.position();

            for (int i = 0; i < len; ) {
                byte c = (byte) (b.get(offset + i++) & 0x7f);
                if (isCaseInsensitive())
                    c = (byte) Character.toLowerCase((char) c);

                while (true) {
                    int row = ROW_SIZE * t;
                    char n = _tree[row];
                    int diff = n - c;

                    if (diff == 0) {
                        t = _tree[row + EQ];
                        if (t == 0)
                            return null;
                        break;
                    }

                    t = _tree[row + hilo(diff)];
                    if (t == 0)
                        return null;
                }
            }

            return (V) _value[t];
        }

        /* ------------------------------------------------------------ */
        public V getBest(String s) {
            return getBest(0, s, 0, s.length());
        }

        /* ------------------------------------------------------------ */
        public V getBest(String s, int offset, int length) {
            return getBest(0, s, offset, length);
        }

        /* ------------------------------------------------------------ */
        private V getBest(int t, String s, int offset, int len) {
            int node = t;
            int end = offset + len;
            loop:
            while (offset < end) {
                char c = s.charAt(offset++);
                len--;
                if (isCaseInsensitive() && c < 128)
                    c = Character.toLowerCase(c);

                while (true) {
                    int row = ROW_SIZE * t;
                    char n = _tree[row];
                    int diff = n - c;

                    if (diff == 0) {
                        t = _tree[row + EQ];
                        if (t == 0)
                            break loop;

                        // if this node is a match, recurse to remember
                        if (_key[t] != null) {
                            node = t;
                            V better = getBest(t, s, offset, len);
                            if (better != null)
                                return better;
                        }
                        break;
                    }

                    t = _tree[row + hilo(diff)];
                    if (t == 0)
                        break loop;
                }
            }
            return (V) _value[node];
        }


        /* ------------------------------------------------------------ */
        public V getBest(ByteBuffer b, int offset, int len) {
            if (b.hasArray())
                return getBest(0, b.array(), b.arrayOffset() + b.position() + offset, len);
            return getBest(0, b, offset, len);
        }

        /* ------------------------------------------------------------ */
        private V getBest(int t, byte[] b, int offset, int len) {
            int node = t;
            int end = offset + len;
            loop:
            while (offset < end) {
                byte c = (byte) (b[offset++] & 0x7f);
                len--;
                if (isCaseInsensitive())
                    c = (byte) Character.toLowerCase((char) c);

                while (true) {
                    int row = ROW_SIZE * t;
                    char n = _tree[row];
                    int diff = n - c;

                    if (diff == 0) {
                        t = _tree[row + EQ];
                        if (t == 0)
                            break loop;

                        // if this node is a match, recurse to remember
                        if (_key[t] != null) {
                            node = t;
                            V better = getBest(t, b, offset, len);
                            if (better != null)
                                return better;
                        }
                        break;
                    }

                    t = _tree[row + hilo(diff)];
                    if (t == 0)
                        break loop;
                }
            }
            return (V) _value[node];
        }

        /* ------------------------------------------------------------ */
        private V getBest(int t, ByteBuffer b, int offset, int len) {
            int node = t;
            int o = offset + b.position();

            loop:
            for (int i = 0; i < len; i++) {
                byte c = (byte) (b.get(o + i) & 0x7f);
                if (isCaseInsensitive())
                    c = (byte) Character.toLowerCase((char) c);

                while (true) {
                    int row = ROW_SIZE * t;
                    char n = _tree[row];
                    int diff = n - c;

                    if (diff == 0) {
                        t = _tree[row + EQ];
                        if (t == 0)
                            break loop;

                        // if this node is a match, recurse to remember
                        if (_key[t] != null) {
                            node = t;
                            V best = getBest(t, b, offset + i + 1, len - i - 1);
                            if (best != null)
                                return best;
                        }
                        break;
                    }

                    t = _tree[row + hilo(diff)];
                    if (t == 0)
                        break loop;
                }
            }
            return (V) _value[node];
        }

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder();
            for (int r = 0; r <= _rows; r++) {
                if (_key[r] != null && _value[r] != null) {
                    buf.append(',');
                    buf.append(_key[r]);
                    buf.append('=');
                    buf.append(_value[r].toString());
                }
            }
            if (buf.length() == 0)
                return "{}";

            buf.setCharAt(0, '{');
            buf.append('}');
            return buf.toString();
        }


        public Set<String> keySet() {
            Set<String> keys = new HashSet<>();

            for (int r = 0; r <= _rows; r++) {
                if (_key[r] != null && _value[r] != null)
                    keys.add(_key[r]);
            }
            return keys;
        }

        public boolean isFull() {
            return _rows + 1 == _key.length;
        }

        public static int hilo(int diff) {
            // branchless equivalent to return ((diff<0)?LO:HI);
            // return 3+2*((diff&Integer.MIN_VALUE)>>Integer.SIZE-1);
            return 1 + (diff | Integer.MAX_VALUE) / (Integer.MAX_VALUE / 2);
        }

        public void dump() {
            for (int r = 0; r < _rows; r++) {
                char c = _tree[r * ROW_SIZE + 0];
                System.err.printf("%4d [%s,%d,%d,%d] '%s':%s%n",
                        r,
                        (c < ' ' || c > 127) ? ("" + (int) c) : "'" + c + "'",
                        (int) _tree[r * ROW_SIZE + LO],
                        (int) _tree[r * ROW_SIZE + EQ],
                        (int) _tree[r * ROW_SIZE + HI],
                        _key[r],
                        _value[r]);
            }

        }


        public boolean put(V v) {
            return put(v.toString(), v);
        }

        public V remove(String s) {
            V o = get(s);
            put(s, null);
            return o;
        }


        public V get(String s) {
            return get(s, 0, s.length());
        }


        public V get(ByteBuffer b) {
            return get(b, 0, b.remaining());
        }


        public V getBest(byte[] b, int offset, int len) {
            return getBest(new String(b, offset, len, StandardCharsets.ISO_8859_1));
        }


        public boolean isCaseInsensitive() {
            return _caseInsensitive;
        }
    }


    public static void main(String[] args) throws Exception {
        SimpleTriePrefixMatch simpleTrie = new SimpleTriePrefixMatch(List.of("a.b", "a.c", "d.e"));
        RuntimeTrie runtimeTrie = RuntimeTrie.constructFrom(simpleTrie.root);
        RuntimeTrieBytes runtimeTrieB = RuntimeTrieBytes.constructFrom(simpleTrie.root);
        Consumer<String> testme = in -> {
            System.out.printf("%15s : %s %s %n", in, simpleTrie.startsWith(in), runtimeTrie.startsWith(in));
            System.out.printf("%15s : %s %s %n", in, runtimeTrieB.startsWith(in), runtimeTrieB.startsWith(in));
        };

        testme.accept("a");
        testme.accept("a.b");
        testme.accept("a.b.c"); //expected true
        testme.accept("a.d"); //expected false


        System.out.println("NO-Match");
        System.out.println("=============");
        execAllTestMethods(() -> {
            PrefixSearchBlacklistJMH instance = new PrefixSearchBlacklistJMH();
            instance.isAMatchChance = 0;
            instance.almostMatchChance = 0;
            instance.setup();
            System.out.println("example: " + instance.getRandomTestString());
            return instance;
        });
        System.out.println("Exact-Match");
        System.out.println("=============");
        execAllTestMethods(() -> {
            PrefixSearchBlacklistJMH instance = new PrefixSearchBlacklistJMH();
            instance.isAMatchChance = 100;
            instance.exactMatchChance = 100;
            instance.setup();
            System.out.println("example: " + instance.getRandomTestString());

            return instance;
        });
        System.out.println("Contains-Match");
        System.out.println("=============");
        execAllTestMethods(() -> {
            PrefixSearchBlacklistJMH instance = new PrefixSearchBlacklistJMH();
            instance.isAMatchChance = 100;
            instance.exactMatchChance = 0;
            instance.setup();
            System.out.println("example: " + instance.getRandomTestString());
            return instance;
        });


        Options benchOptions = new OptionsBuilder()
                .include(PrefixSearchBlacklistJMH.class.getName() + ".*")
                //##########
                // Profilers
                //############
                //commonly used profilers:
                //.addProfiler(GCProfiler.class)
                //.addProfiler(StackProfiler.class)
                //.addProfiler(HotspotRuntimeProfiler.class)
                //.addProfiler(HotspotMemoryProfiler.class)
                //.addProfiler(HotspotCompilationProfiler.class)
                //
                // full list of built in profilers:
                //("cl",       ClassloaderProfiler.class);
                //("comp",     CompilerProfiler.class);
                //("gc",       GCProfiler.class);
                //("hs_cl",    HotspotClassloadingProfiler.class);
                //("hs_comp",  HotspotCompilationProfiler.class);
                //("hs_gc",    HotspotMemoryProfiler.class);
                //("hs_rt",    HotspotRuntimeProfiler.class);
                //("hs_thr",   HotspotThreadProfiler.class);
                //("stack",    StackProfiler.class);
                //("perf",     LinuxPerfProfiler.class);
                //("perfnorm", LinuxPerfNormProfiler.class);
                //("perfasm",  LinuxPerfAsmProfiler.class);
                //("xperfasm", WinPerfAsmProfiler.class);
                //("dtraceasm", DTraceAsmProfiler.class);
                //("pauses",   PausesProfiler.class);
                //("safepoints", SafepointsProfiler.class);
                //
                //ASM-level profilers - require -XX:+PrintAssembly
                //----------
                // this in turn requires hsdis (hotspot disassembler) binaries to be copied into e.g C:\Program Files\Java\jdk1.8.0_161\jre\bin\server
                // For Windows you can download pre-compiled hsdis module from http://fcml-lib.com/download.html
                //.jvmArgsAppend("-XX:+PrintAssembly") //requires hsdis binary in jdk - enable if you use the perf or winperf profiler
                ///required for external profilers like "perf" to show java frames in their traces
                //.jvmArgsAppend("-XX:+PreserveFramePointer")
                //XPERF  - windows xperf must be installed - this is included in WPT (windows performance toolkit) wich in turn is windows ADK included in https://developer.microsoft.com/en-us/windows/hardware/windows-assessment-deployment-kit
                //WARNING - MUST RUN WITH ADMINISTRATIVE PRIVILEGES (must start your console or your IDE with admin rights!
                //WARNING - first ever run of xperf takes VERY VERY long (1h+) because it has to download and process symbols
                //.addProfiler(WinPerfAsmProfiler.class)
                //.addProfiler(LinuxPerfProfiler.class)
                //.addProfiler(LinuxPerfNormProfiler.class)
                //.addProfiler(LinuxPerfAsmProfiler.class)
                //
                // #########
                // More Profling jvm options
                // #########
                // .jvmArgsAppend("-XX:+UnlockCommercialFeatures")
                // .jvmArgsAppend("-XX:+FlightRecorder")
                // .jvmArgsAppend("-XX:+UnlockDiagnosticVMOptions")
                // .jvmArgsAppend("-XX:+PrintSafepointStatistics")
                // .jvmArgsAppend("-XX:+DebugNonSafepoints")
                //
                // required for external profilers like "perf" to show java
                // frames in their traces
                // .jvmArgsAppend("-XX:+PreserveFramePointer")
                //
                // #########
                // COMPILER
                // #########
                // make sure we dont see compiling of our benchmark code during
                // measurement.
                // if you see compiling => more warmup
                .jvmArgsAppend("-XX:+UnlockDiagnosticVMOptions")
                //.jvmArgsAppend("-XX:+PrintCompilation")
                // .jvmArgsAppend("-XX:+PrintInlining")
                // .jvmArgsAppend("-XX:+PrintAssembly") //requires hsdis binary in jdk - enable if you use the perf or winperf profiler
                // .jvmArgsAppend("-XX:+PrintOptoAssembly") //c2 compiler only
                // More compiler prints:
                // .jvmArgsAppend("-XX:+PrintInterpreter")
                // .jvmArgsAppend("-XX:+PrintNMethods")
                // .jvmArgsAppend("-XX:+PrintNativeNMethods")
                // .jvmArgsAppend("-XX:+PrintSignatureHandlers")
                // .jvmArgsAppend("-XX:+PrintAdapterHandlers")
                // .jvmArgsAppend("-XX:+PrintStubCode")
                // .jvmArgsAppend("-XX:+PrintCompilation")
                // .jvmArgsAppend("-XX:+PrintInlining")
                // .jvmArgsAppend("-XX:+TraceClassLoading")
                // .jvmArgsAppend("-XX:PrintAssemblyOptions=syntax")
                .build();

        Collection<RunResult> results = new Runner(benchOptions).run();
        BenchmarkFormatter.displayAsMatrix(results, "prefixesCount");

    }

    private static void execAllTestMethods(Supplier<PrefixSearchBlacklistJMH> instanceSupplier) {
        PrefixSearchBlacklistJMH instance = instanceSupplier.get();
        Arrays.stream(PrefixSearchBlacklistJMH.class.getMethods())
                .filter(m -> m.getAnnotation(Benchmark.class) != null)
                .forEach(method -> {
                    try {
                        System.out.println(method.getName() + " : " + method.invoke(instance));
                    } catch (ReflectiveOperationException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

}