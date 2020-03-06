package de.frank.jmh.algorithms;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.commons.lang3.text.translate.LookupTranslator;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/*--
Compare common approaches and libraries to "sanitize" strings - commonly by replacing and removing stuff from the input.

Result:
- For most cases the Apache's StringUtils .replace() and .replaceEach() functions are the best choices and have a performance advantage of 2-15x!
Notable exceptions:
 - If a simple char->char substitution is enough String.replace('a','b') is good.
 - If most strings wont need "sanitize" actions (NO_MATCH) and the input tends to be long (>64 chars), then java's Pattern Matcher is a good choice. (ideally cached and reused in a ThreadLocal)

WARNING:
 - in hot code NEVER use someString.replace("toFind", "replacement") or .replaceAll(). Use Apache String Utils or at least cache the search pattern with Pattern.compile("toFind") and use the Matcher
 - be especially careful with multi-search-replace chains like: someInput.replace("\\r", "\r").replace("\\n", "\n").replace("\\t", "\t") - THEY SUCK!

ops/s (higher=better)                    MATCH     MATCH     NO_MATCH     NO_MATCH   #
                                        size=1  size=100       size=1     size=100   #
replaceSingle
   String_replace                   29.081.704   659.296    79.766.497      979.583   #1 if simple one char substitution (rarely)
remove
   ApacheStringUtils_replace         6.110.383    74.383    69.723.789    1.099.984   #1 winner, except for no-match case
   CachedMatcher_replaceAll          3.559.446    49.570    32.831.458    1.878.575   #2 ThreadLocal cache+reset the matcher offers a slight gain
   Matcher_replaceAll                3.359.963    46.703    20.333.294    1.726.379   #3 (replace=literal) cached Pre-Complied patterns is better then String.replace
   Matcher_replaceAll_literal        2.883.205    49.537    20.408.383    1.734.805   #4 (replaceAll=regex) cached Pre-Complied patterns is better then String.replace
   String_replace                    2.722.391    45.639     9.084.557    1.600.579   #5 (replace=literal)
   String_replaceAll                 2.486.311    45.281     6.770.305    1.442.005   #6 (replaceAll=regex)
   StringSplitJoin                   2.112.012    37.090     5.127.103      595.641   #7 worst
replace
   ApacheStringUtils_replace         5.777.618    68.727    73.260.772    1.143.783   #1 winner
   CachedMatcher_replaceAll          3.004.263    42.008    30.133.940    1.771.834   #2 ThreadLocal cache+reset the matcher offers a slight gain
   Matcher_replaceAll                2.831.040    42.963    20.353.358    1.735.522   #3 cached Pre-Complied patterns is better then String.replace
   Matcher_replaceAll_literal        2.832.252    44.235    21.114.823    1.716.508   #4 cached Pre-Complied patterns is better then String.replace
   String_replace                    2.327.316    42.515     9.324.876    1.625.840   #5 (replace=literal)
   String_replaceAll                 2.075.712    36.065     7.148.350    1.399.649   #6 (replaceAll=regex)
   String_splitJoin                  1.889.062    33.033     5.297.719      575.762   #7 worst - please dont use
multiReplace
   ApacheStringUtils_replaceEach     1.392.566    14.949    22.872.595      501.608   #1 winner
   ApacheLookupTranslator            1.258.075    12.815     4.282.744       32.684   #2 has issues in no-macht case
   Matcher_replaceALLChain             815.940    10.813     6.883.631      575.779   #3 ok-ish
   Matcher_replaceALLChain_literal     802.164    11.412     6.738.441      545.819   #4 literal is somehow worse...
   String_replaceAllChain              622.036    10.579     2.421.741      479.579   #5 please cache the compiled patterns!
   String_replaceChain                 707.538    11.509     3.052.788      518.901   #6 sucks
   Matcher_group                       771.062     8.558     4.237.626       61.175   #7 sucks

RAW data
Benchmark                                                             (size)    (type)   Mode  Cnt          Score         Error  Units
StringReplacePatternJMH.multiReplace_ApacheLookupTranslator                1     MATCH  thrpt    5    1174893,297 ±   66669,224  ops/s
StringReplacePatternJMH.multiReplace_ApacheLookupTranslator                1  NO_MATCH  thrpt    5    3359133,218 ±  144785,099  ops/s
StringReplacePatternJMH.multiReplace_ApacheLookupTranslator              100     MATCH  thrpt    5      11792,863 ±     647,766  ops/s
StringReplacePatternJMH.multiReplace_ApacheLookupTranslator              100  NO_MATCH  thrpt    5      31443,519 ±    1668,508  ops/s
StringReplacePatternJMH.multiReplace_ApacheStringUtils_replaceEach         1     MATCH  thrpt    5    2163980,015 ±   96520,474  ops/s
StringReplacePatternJMH.multiReplace_ApacheStringUtils_replaceEach         1  NO_MATCH  thrpt    5   36349929,753 ± 1206648,822  ops/s
StringReplacePatternJMH.multiReplace_ApacheStringUtils_replaceEach       100     MATCH  thrpt    5      22481,360 ±     557,486  ops/s
StringReplacePatternJMH.multiReplace_ApacheStringUtils_replaceEach       100  NO_MATCH  thrpt    5    1014395,104 ±   56530,250  ops/s
StringReplacePatternJMH.multiReplace_Matcher_replaceALLChain               1     MATCH  thrpt    5     953243,110 ±   38603,957  ops/s
StringReplacePatternJMH.multiReplace_Matcher_replaceALLChain               1  NO_MATCH  thrpt    5    6128579,278 ±  418458,120  ops/s
StringReplacePatternJMH.multiReplace_Matcher_replaceALLChain             100     MATCH  thrpt    5      13163,698 ±     443,166  ops/s
StringReplacePatternJMH.multiReplace_Matcher_replaceALLChain             100  NO_MATCH  thrpt    5     320350,307 ±   14386,240  ops/s
StringReplacePatternJMH.multiReplace_Matcher_replaceALLChain_literal       1     MATCH  thrpt    5     925307,018 ±   69990,787  ops/s
StringReplacePatternJMH.multiReplace_Matcher_replaceALLChain_literal       1  NO_MATCH  thrpt    5    6128673,149 ±  117417,119  ops/s
StringReplacePatternJMH.multiReplace_Matcher_replaceALLChain_literal     100     MATCH  thrpt    5      13198,349 ±     473,631  ops/s
StringReplacePatternJMH.multiReplace_Matcher_replaceALLChain_literal     100  NO_MATCH  thrpt    5     322375,918 ±    4647,191  ops/s
StringReplacePatternJMH.multiReplace_String_replaceAllChain                1     MATCH  thrpt    5     753489,084 ±   22861,173  ops/s
StringReplacePatternJMH.multiReplace_String_replaceAllChain                1  NO_MATCH  thrpt    5    2476621,994 ±  176473,267  ops/s
StringReplacePatternJMH.multiReplace_String_replaceAllChain              100     MATCH  thrpt    5      12869,966 ±    1076,312  ops/s
StringReplacePatternJMH.multiReplace_String_replaceAllChain              100  NO_MATCH  thrpt    5     300959,498 ±   16291,268  ops/s
StringReplacePatternJMH.multiReplace_String_replaceChain                   1     MATCH  thrpt    5    3935187,870 ±  139367,473  ops/s
StringReplacePatternJMH.multiReplace_String_replaceChain                   1  NO_MATCH  thrpt    5   51586669,814 ±  686150,236  ops/s
StringReplacePatternJMH.multiReplace_String_replaceChain                 100     MATCH  thrpt    5      42397,600 ±     726,578  ops/s
StringReplacePatternJMH.multiReplace_String_replaceChain                 100  NO_MATCH  thrpt    5    1047002,032 ±   43734,603  ops/s
StringReplacePatternJMH.multiReplace_matcher_groups                        1     MATCH  thrpt    5     725200,052 ±   64361,567  ops/s
StringReplacePatternJMH.multiReplace_matcher_groups                        1  NO_MATCH  thrpt    5    5335040,162 ±  169230,701  ops/s
StringReplacePatternJMH.multiReplace_matcher_groups                      100     MATCH  thrpt    5       8157,445 ±     142,574  ops/s
StringReplacePatternJMH.multiReplace_matcher_groups                      100  NO_MATCH  thrpt    5      80981,874 ±    2591,455  ops/s
StringReplacePatternJMH.remove_ApacheStringUtils_replace                   1     MATCH  thrpt    5    7190760,705 ±  357349,610  ops/s
StringReplacePatternJMH.remove_ApacheStringUtils_replace                   1  NO_MATCH  thrpt    5  124573547,692 ± 2059549,834  ops/s
StringReplacePatternJMH.remove_ApacheStringUtils_replace                 100     MATCH  thrpt    5      88081,733 ±    7571,966  ops/s
StringReplacePatternJMH.remove_ApacheStringUtils_replace                 100  NO_MATCH  thrpt    5    3112813,752 ±  168213,882  ops/s
StringReplacePatternJMH.remove_CachedMatcher_replaceAll                    1     MATCH  thrpt    5    4407317,344 ±  114857,734  ops/s
StringReplacePatternJMH.remove_CachedMatcher_replaceAll                    1  NO_MATCH  thrpt    5   36671637,969 ± 1122913,678  ops/s
StringReplacePatternJMH.remove_CachedMatcher_replaceAll                  100     MATCH  thrpt    5      54658,694 ±    2195,754  ops/s
StringReplacePatternJMH.remove_CachedMatcher_replaceAll                  100  NO_MATCH  thrpt    5    1901348,087 ±   63881,473  ops/s
StringReplacePatternJMH.remove_Matcher_replaceAll                          1     MATCH  thrpt    5    3822951,624 ±  442014,344  ops/s
StringReplacePatternJMH.remove_Matcher_replaceAll                          1  NO_MATCH  thrpt    5   19684398,239 ±  435267,129  ops/s
StringReplacePatternJMH.remove_Matcher_replaceAll                        100     MATCH  thrpt    5      53354,381 ±    1393,411  ops/s
StringReplacePatternJMH.remove_Matcher_replaceAll                        100  NO_MATCH  thrpt    5    1778652,102 ±   98849,722  ops/s
StringReplacePatternJMH.remove_Matcher_replaceAll_literal                  1     MATCH  thrpt    5    3863165,933 ±  425840,091  ops/s
StringReplacePatternJMH.remove_Matcher_replaceAll_literal                  1  NO_MATCH  thrpt    5   19813316,672 ±  225284,924  ops/s
StringReplacePatternJMH.remove_Matcher_replaceAll_literal                100     MATCH  thrpt    5      52138,289 ±    3875,268  ops/s
StringReplacePatternJMH.remove_Matcher_replaceAll_literal                100  NO_MATCH  thrpt    5    1801581,855 ±   31109,099  ops/s
StringReplacePatternJMH.remove_StringSplitJoin                             1     MATCH  thrpt    5    2229081,633 ±   83850,573  ops/s
StringReplacePatternJMH.remove_StringSplitJoin                             1  NO_MATCH  thrpt    5    7890298,801 ±  449592,996  ops/s
StringReplacePatternJMH.remove_StringSplitJoin                           100     MATCH  thrpt    5      43046,966 ±    3437,789  ops/s
StringReplacePatternJMH.remove_StringSplitJoin                           100  NO_MATCH  thrpt    5    1551898,073 ±   37926,761  ops/s
StringReplacePatternJMH.remove_String_replace                              1     MATCH  thrpt    5   12947956,900 ±  509334,721  ops/s
StringReplacePatternJMH.remove_String_replace                              1  NO_MATCH  thrpt    5  117995283,604 ± 3797631,479  ops/s
StringReplacePatternJMH.remove_String_replace                            100     MATCH  thrpt    5     161033,821 ±    5093,829  ops/s
StringReplacePatternJMH.remove_String_replace                            100  NO_MATCH  thrpt    5    3163937,242 ±   55225,776  ops/s
StringReplacePatternJMH.remove_String_replaceAll                           1     MATCH  thrpt    5    2760152,836 ±  134226,942  ops/s
StringReplacePatternJMH.remove_String_replaceAll                           1  NO_MATCH  thrpt    5    7576787,423 ±  456531,123  ops/s
StringReplacePatternJMH.remove_String_replaceAll                         100     MATCH  thrpt    5      52374,906 ±    5161,184  ops/s
StringReplacePatternJMH.remove_String_replaceAll                         100  NO_MATCH  thrpt    5    1545120,826 ±   35394,915  ops/s
StringReplacePatternJMH.remove_custStringBuilder                           1     MATCH  thrpt    5    9668023,947 ±  737376,904  ops/s
StringReplacePatternJMH.remove_custStringBuilder                           1  NO_MATCH  thrpt    5   34197608,846 ±  414874,769  ops/s
StringReplacePatternJMH.remove_custStringBuilder                         100     MATCH  thrpt    5     130516,306 ±    5463,337  ops/s
StringReplacePatternJMH.remove_custStringBuilder                         100  NO_MATCH  thrpt    5    1306516,560 ±   37249,281  ops/s
StringReplacePatternJMH.remove_custStringBuilderSized                      1     MATCH  thrpt    5   10108069,748 ±  498807,154  ops/s
StringReplacePatternJMH.remove_custStringBuilderSized                      1  NO_MATCH  thrpt    5   40466670,011 ±  969800,946  ops/s
StringReplacePatternJMH.remove_custStringBuilderSized                    100     MATCH  thrpt    5     138798,789 ±    3187,572  ops/s
StringReplacePatternJMH.remove_custStringBuilderSized                    100  NO_MATCH  thrpt    5    1333599,274 ±   17757,205  ops/s
StringReplacePatternJMH.remove_custStringBuilderSizedLazy                  1     MATCH  thrpt    5    8989505,543 ±  521022,300  ops/s
StringReplacePatternJMH.remove_custStringBuilderSizedLazy                100     MATCH  thrpt    5     120596,523 ±    2531,830  ops/s
StringReplacePatternJMH.replaceSingle_String_replace                       1     MATCH  thrpt    5   51353935,385 ± 3114441,719  ops/s
StringReplacePatternJMH.replaceSingle_String_replace                       1  NO_MATCH  thrpt    5   86539395,740 ± 4544361,879  ops/s
StringReplacePatternJMH.replaceSingle_String_replace                     100     MATCH  thrpt    5    1012090,804 ±   55048,353  ops/s
StringReplacePatternJMH.replaceSingle_String_replace                     100  NO_MATCH  thrpt    5    1181905,932 ±   11441,198  ops/s
StringReplacePatternJMH.replaceSingle_custStringBuilder                    1     MATCH  thrpt    5    7558342,350 ±  250298,963  ops/s
StringReplacePatternJMH.replaceSingle_custStringBuilder                    1  NO_MATCH  thrpt    5    9976447,329 ±  187176,863  ops/s
StringReplacePatternJMH.replaceSingle_custStringBuilder                  100     MATCH  thrpt    5      88524,022 ±    5780,632  ops/s
StringReplacePatternJMH.replaceSingle_custStringBuilder                  100  NO_MATCH  thrpt    5     118606,228 ±    2657,495  ops/s
StringReplacePatternJMH.replaceSingle_custStringBuilderLazy                1     MATCH  thrpt    5    7627417,044 ±  268791,097  ops/s
StringReplacePatternJMH.replaceSingle_custStringBuilderLazy                1  NO_MATCH  thrpt    5    9837968,423 ±  590613,050  ops/s
StringReplacePatternJMH.replaceSingle_custStringBuilderLazy              100     MATCH  thrpt    5      89227,554 ±    1084,012  ops/s
StringReplacePatternJMH.replaceSingle_custStringBuilderLazy              100  NO_MATCH  thrpt    5     118528,621 ±    4070,807  ops/s
StringReplacePatternJMH.replaceSingle_custStringBuilderPreSized            1     MATCH  thrpt    5    8591129,793 ±  391379,711  ops/s
StringReplacePatternJMH.replaceSingle_custStringBuilderPreSized            1  NO_MATCH  thrpt    5   11190901,678 ±  631024,587  ops/s
StringReplacePatternJMH.replaceSingle_custStringBuilderPreSized          100     MATCH  thrpt    5      92872,588 ±    2095,003  ops/s
StringReplacePatternJMH.replaceSingle_custStringBuilderPreSized          100  NO_MATCH  thrpt    5     117874,213 ±   28461,894  ops/s
StringReplacePatternJMH.replace_ApacheStringUtils_replace                  1     MATCH  thrpt    5    6282825,825 ±  360401,823  ops/s
StringReplacePatternJMH.replace_ApacheStringUtils_replace                  1  NO_MATCH  thrpt    5  116589488,722 ± 7546508,278  ops/s
StringReplacePatternJMH.replace_ApacheStringUtils_replace                100     MATCH  thrpt    5      68947,119 ±   32955,972  ops/s
StringReplacePatternJMH.replace_ApacheStringUtils_replace                100  NO_MATCH  thrpt    5    2938693,245 ±  582599,449  ops/s
StringReplacePatternJMH.replace_CachedMatcher_replaceAll                   1     MATCH  thrpt    5    2196369,226 ± 2226512,027  ops/s
StringReplacePatternJMH.replace_CachedMatcher_replaceAll                   1  NO_MATCH  thrpt    5   31319166,162 ± 7197907,626  ops/s
StringReplacePatternJMH.replace_CachedMatcher_replaceAll                 100     MATCH  thrpt    5      41301,628 ±    9529,627  ops/s
StringReplacePatternJMH.replace_CachedMatcher_replaceAll                 100  NO_MATCH  thrpt    5    1841703,989 ±   81480,405  ops/s
StringReplacePatternJMH.replace_Matcher_replaceAll                         1     MATCH  thrpt    5    3254571,417 ±  273946,454  ops/s
StringReplacePatternJMH.replace_Matcher_replaceAll                         1  NO_MATCH  thrpt    5   19387347,704 ±  611802,201  ops/s
StringReplacePatternJMH.replace_Matcher_replaceAll                       100     MATCH  thrpt    5      43422,573 ±    2618,298  ops/s
StringReplacePatternJMH.replace_Matcher_replaceAll                       100  NO_MATCH  thrpt    5    1763230,148 ±  146019,427  ops/s
StringReplacePatternJMH.replace_Matcher_replaceAll_literal                 1     MATCH  thrpt    5    3194866,523 ±  183433,180  ops/s
StringReplacePatternJMH.replace_Matcher_replaceAll_literal                 1  NO_MATCH  thrpt    5   19356621,330 ±  530307,377  ops/s
StringReplacePatternJMH.replace_Matcher_replaceAll_literal               100     MATCH  thrpt    5      45140,871 ±    1463,453  ops/s
StringReplacePatternJMH.replace_Matcher_replaceAll_literal               100  NO_MATCH  thrpt    5    1745128,056 ±   82372,351  ops/s
StringReplacePatternJMH.replace_String_replace                             1     MATCH  thrpt    5   12594245,222 ±  865871,828  ops/s
StringReplacePatternJMH.replace_String_replace                             1  NO_MATCH  thrpt    5  113080599,404 ± 6331318,621  ops/s
StringReplacePatternJMH.replace_String_replace                           100     MATCH  thrpt    5     135034,382 ±    4889,919  ops/s
StringReplacePatternJMH.replace_String_replace                           100  NO_MATCH  thrpt    5    3049872,181 ±  124225,007  ops/s
StringReplacePatternJMH.replace_String_replaceAll                          1     MATCH  thrpt    5    2375530,553 ±   65730,431  ops/s
StringReplacePatternJMH.replace_String_replaceAll                          1  NO_MATCH  thrpt    5    7257898,645 ±  703910,020  ops/s
StringReplacePatternJMH.replace_String_replaceAll                        100     MATCH  thrpt    5      41877,301 ±    6342,250  ops/s
StringReplacePatternJMH.replace_String_replaceAll                        100  NO_MATCH  thrpt    5    1501877,012 ±   52744,133  ops/s
StringReplacePatternJMH.replace_String_splitJoin                           1     MATCH  thrpt    5    2131198,070 ±   97586,442  ops/s
StringReplacePatternJMH.replace_String_splitJoin                           1  NO_MATCH  thrpt    5    7766509,869 ±  243295,267  ops/s
StringReplacePatternJMH.replace_String_splitJoin                         100     MATCH  thrpt    5      42095,091 ±    1288,925  ops/s
StringReplacePatternJMH.replace_String_splitJoin                         100  NO_MATCH  thrpt    5    1530114,740 ±   48840,826  ops/s
StringReplacePatternJMH.replace_custStringBuilder                          1     MATCH  thrpt    5    8198167,446 ±  269751,619  ops/s
StringReplacePatternJMH.replace_custStringBuilder                          1  NO_MATCH  thrpt    5   33673830,996 ± 1628934,436  ops/s
StringReplacePatternJMH.replace_custStringBuilder                        100     MATCH  thrpt    5     107063,943 ±    1668,004  ops/s
StringReplacePatternJMH.replace_custStringBuilder                        100  NO_MATCH  thrpt    5    1268025,655 ±  108279,851  ops/s
StringReplacePatternJMH.replace_custStringBuilderSized                     1     MATCH  thrpt    5    8867633,097 ±  769408,865  ops/s
StringReplacePatternJMH.replace_custStringBuilderSized                     1  NO_MATCH  thrpt    5   40437690,688 ± 1664271,238  ops/s
StringReplacePatternJMH.replace_custStringBuilderSized                   100     MATCH  thrpt    5     111656,091 ±    3354,577  ops/s
StringReplacePatternJMH.replace_custStringBuilderSized                   100  NO_MATCH  thrpt    5    1278432,198 ±   83827,162  ops/s
StringReplacePatternJMH.replace_custStringBuilderSizedLazy                 1     MATCH  thrpt    5    8034274,647 ±  394530,283  ops/s
StringReplacePatternJMH.replace_custStringBuilderSizedLazy               100     MATCH  thrpt    5     100184,390 ±    1941,733  ops/s
 * @author Michael Frank
 * @version 1.0 05.12.2019
 */
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread) // Important to be Scope.Benchmark
@Fork(1)
@Threads(1)
public class StringReplacePatternJMH {

    private static final String[] MULTI_SEARCH = {"\\r", "\\n", "\\t"};
    private static final String[] MULTI_REPLACE = {"\r", "\n", "\t"};
    private static final Pattern MULTI_SEARCH_REGEX = Pattern.compile("\\\\r|\\\\n|\\\\t");

    private static final HashMap<String, String> REPLACE_MAP;

    static {
        HashMap<String, String> replaceMap = new HashMap<>();
        for (int i = 0; i < MULTI_SEARCH.length; i++) {
            replaceMap.put(MULTI_SEARCH[i], MULTI_REPLACE[i]);
        }
        REPLACE_MAP = replaceMap;
    }

    private static final StrSubstitutor APACHE_STRSUBSTITUTOR = new StrSubstitutor(REPLACE_MAP);
    private static final LookupTranslator APACHE_LOOKUPTRANSLATOR = new LookupTranslator(new String[][]{
            {"\\r", "\r"},
            {"\\n", "\n"},
            {"\\t", "\t"},
    });

    private static final Pattern ESCAPED_NEWLINE_LITERAL = Pattern.compile("\\n", Pattern.LITERAL);
    private static final Pattern ESCAPED_RETURN_LITERAL = Pattern.compile("\\r", Pattern.LITERAL);
    private static final Pattern ESCAPED_TAB_LITERAL = Pattern.compile("\\t", Pattern.LITERAL);
    private static final Pattern ESCAPED_NEWLINE = Pattern.compile("\\\\n");
    private static final Pattern ESCAPED_RETURN = Pattern.compile("\\\\r");
    private static final Pattern ESCAPED_TAB = Pattern.compile("\\\\t");

    private static final ThreadLocal<Matcher> CACHED_ESCAPED_NEWLINE_MATCHER = ThreadLocal.withInitial(() -> ESCAPED_NEWLINE.matcher(""));

    public enum Match {
        MATCH("the\nTypical\nNew\nLine\nUnescape\nReplace",//
                "the\\r\\n\\tTypical\\r\\n\\tNew\\r\\n\\tLine\\r\\n\\tUnescape\\r\\n\\tReplace",//
                "the\\nTypical\\nNew\\nLine\\nUnescape\\nReplace"//
        ),
        NO_MATCH("the_Typical_New_Line_replace_Replace",//
                "the_Typical_New_Line_replace_Replace",//
                "the_Typical_New_Line_replace_Replace"//
        );

        final String replaceSingleChar;
        final String escapedString;
        final String multiEscapedString;

        Match(String replaceSingleChar, String multiEscapedString, String escapedString) {
            this.replaceSingleChar = replaceSingleChar;
            this.multiEscapedString = multiEscapedString;
            this.escapedString = escapedString;
        }
    }

    @State(Scope.Thread)
    public static class MyState {

        @Param({"1", "100"})
        int size = 1;

        @Param({"MATCH", "NO_MATCH"})
        Match type;

        String escapedString;
        String replaceSingleChar;
        String multiEscapedString;

        public MyState() {
            //For jmh
        }

        /**
         * For manual correctness tests
         *
         * @param size
         * @param type
         */
        public MyState(int size, Match type) {
            this.size = size;
            this.type = type;
            doSetup();
        }


        @Setup(Level.Trial)
        public void doSetup() {
            escapedString = StringUtils.repeat(type.escapedString, size);
            replaceSingleChar = StringUtils.repeat(type.replaceSingleChar, size);
            multiEscapedString = StringUtils.repeat(type.multiEscapedString, size);
        }

    }

    @Benchmark
    public String replaceSingle_String_replace(MyState s) {
        return s.replaceSingleChar.replace('\n', '_');
    }


    @Benchmark
    public String replaceSingle_custStringBuilder(MyState s) {
        return replace(s.replaceSingleChar, '\n', '_');
    }

    @Benchmark
    public String replaceSingle_custStringBuilderPreSized(MyState s) {
        return replacePreSized(s.replaceSingleChar, '\n', '_');
    }

    @Benchmark
    public String replaceSingle_custStringBuilderLazy(MyState s) {
        return replace(s.replaceSingleChar, '\n', '_');
    }

    @Benchmark
    public String replace_String_splitJoin(MyState s) {
        return String.join("\n", s.escapedString.split("\\\\n"));
    }

    @Benchmark
    public String replace_String_replace(MyState s) {
        return s.escapedString.replace("\\n", "\n"); //literal replace
    }

    @Benchmark
    public String replace_String_replaceAll(MyState s) {
        return s.escapedString.replaceAll("\\\\n", "\n"); //non-literal - must escape regex
    }

    @Benchmark
    public String replace_Matcher_replaceAll(MyState s) {
        return ESCAPED_NEWLINE.matcher(s.escapedString).replaceAll("\n");
    }

    @Benchmark
    public String replace_CachedMatcher_replaceAll(MyState s) {
        return CACHED_ESCAPED_NEWLINE_MATCHER.get().reset(s.escapedString).replaceAll("\n");
    }

    @Benchmark
    public String replace_Matcher_replaceAll_literal(MyState s) {
        return ESCAPED_NEWLINE_LITERAL.matcher(s.escapedString).replaceAll("\n");
    }

    @Benchmark
    public String replace_ApacheStringUtils_replace(MyState s) {
        return StringUtils.replace(s.escapedString, "\\n", "\n");
    }

    @Benchmark
    public String replace_custStringBuilder(MyState s) {
        return replace(s.escapedString, "\\n", "\n");
    }

    @Benchmark
    public String replace_custStringBuilderSized(MyState s) {
        return replacePreSized(s.escapedString, "\\n", "\n");
    }

    @Benchmark
    public String replace_custStringBuilderSizedLazy(MyState s) {
        return replaceLazy(s.escapedString, "\\n", "\n");
    }

    //
    @Benchmark
    public String remove_String_replace(MyState s) {
        return s.escapedString.replace("\\n", "");//literal replace
    }

    @Benchmark
    public String remove_String_replaceAll(MyState s) {
        return s.escapedString.replaceAll("\\\\n", "");//non-literal - must escape regex
    }

    @Benchmark
    public String remove_Matcher_replaceAll(MyState s) {
        return ESCAPED_NEWLINE.matcher(s.escapedString).replaceAll("");
    }

    @Benchmark
    public String remove_Matcher_replaceAll_literal(MyState s) {
        return ESCAPED_NEWLINE_LITERAL.matcher(s.escapedString).replaceAll("");
    }

    @Benchmark
    public String remove_CachedMatcher_replaceAll(MyState s) {
        return CACHED_ESCAPED_NEWLINE_MATCHER.get().reset(s.escapedString).replaceAll("");
    }

    @Benchmark
    public String remove_custStringBuilder(MyState s) {
        return replace(s.escapedString, "\\n", "");
    }

    @Benchmark
    public String remove_custStringBuilderSized(MyState s) {
        return replacePreSized(s.escapedString, "\\n", "");
    }

    @Benchmark
    public String remove_custStringBuilderSizedLazy(MyState s) {
        return replaceLazy(s.escapedString, "\\n", "");
    }

    @Benchmark
    public String remove_ApacheStringUtils_replace(MyState s) {
        return StringUtils.remove(s.escapedString, "\\n");
    }

    @Benchmark
    public String remove_StringSplitJoin(MyState s) {
        return String.join("", s.escapedString.split("\\\\n"));
    }


    //MULTI REPLACE
    @Benchmark
    public String multiReplace_String_replaceAllChain(MyState s) {
        return s.multiEscapedString//
                .replaceAll("\\\\r", "\r")//
                .replaceAll("\\\\n", "\n")//
                .replaceAll("\\\\t", "\t");
    }

    //MULTI REPLACE
    @Benchmark
    public String multiReplace_String_replaceChain(MyState s) {
        return s.multiEscapedString//
                .replace("\\r", "\r")//
                .replace("\\n", "\n")//
                .replace("\\t", "\t");
    }

    @Benchmark
    public String multiReplace_Matcher_replaceALLChain_literal(MyState s) {
        String stage1 = ESCAPED_RETURN_LITERAL.matcher(s.multiEscapedString).replaceAll("\r");
        String stage2 = ESCAPED_NEWLINE_LITERAL.matcher(stage1).replaceAll("\n");
        return ESCAPED_TAB_LITERAL.matcher(stage2).replaceAll("\t");
    }

    @Benchmark
    public String multiReplace_Matcher_replaceALLChain(MyState s) {
        String stage1 = ESCAPED_RETURN.matcher(s.multiEscapedString).replaceAll("\r");
        String stage2 = ESCAPED_NEWLINE.matcher(stage1).replaceAll("\n");
        return ESCAPED_TAB.matcher(stage2).replaceAll("\t");
    }


    @Benchmark
    public String multiReplace_ApacheStringUtils_replaceEach(MyState s) {
        return StringUtils.replaceEach(s.multiEscapedString, MULTI_SEARCH, MULTI_REPLACE);
    }


    @Benchmark
    public String multiReplace_ApacheLookupTranslator(MyState s) {
        return APACHE_LOOKUPTRANSLATOR.translate(s.multiEscapedString);
    }

//    @Benchmark
//    public String multiReplace_ApacheStrSubstitutor(MyState s) {
//        return APACHE_STRSUBSTITUTOR.replace(s.multiEscapedString);
//    }

    @Benchmark
    public String multiReplace_matcher_groups(MyState s) {
        StringBuffer sb = new StringBuffer();
        Matcher matcher = MULTI_SEARCH_REGEX.matcher(s.multiEscapedString);
        while (matcher.find()) {
            String group = matcher.group(0);
            String replacement = REPLACE_MAP.get(group);
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    static String replace(String input, String toFind, String replacement) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder stringBuilder = new StringBuilder();
        int tokenStart = 0;
        int search;
        while ((search = input.indexOf(toFind, tokenStart)) > 0) {
            stringBuilder.append(input, tokenStart, search);//append token
            stringBuilder.append(replacement);//append replacement
            tokenStart = search + toFind.length();
        }

        if (tokenStart < input.length()) {//last token
            stringBuilder.append(input.substring(tokenStart));
        }

        return stringBuilder.toString();
    }

    static String replacePreSized(String input, String toFind, String replacement) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder stringBuilder = new StringBuilder(Math.max(16, input.length() + 1));
        int tokenStart = 0;
        int search;
        while ((search = input.indexOf(toFind, tokenStart)) > 0) {
            stringBuilder.append(input, tokenStart, search);//append token
            stringBuilder.append(replacement);//append replacement
            tokenStart = search + toFind.length();
        }

        if (tokenStart < input.length()) {//last token
            stringBuilder.append(input.substring(tokenStart));
        }

        return stringBuilder.toString();
    }


    static String replaceLazy(String input, String toFind, String replacement) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder stringBuilder = null;
        int tokenStart = 0;
        int search;
        while ((search = input.indexOf(toFind, tokenStart)) > 0) {
            if (stringBuilder == null) {
                stringBuilder = new StringBuilder(Math.max(16, input.length() + 1));
            }
            stringBuilder.append(input, tokenStart, search);//append token
            stringBuilder.append(replacement);//append replacement
            tokenStart = search + toFind.length();
        }

        if (stringBuilder != null && tokenStart < input.length()) {//last token
            stringBuilder.append(input.substring(tokenStart));
        }

        return stringBuilder == null ? input : stringBuilder.toString();
    }

    static String replace(String input, char find, char replace) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder stringBuilder = new StringBuilder();
        for (int index = 0; index < input.length(); index++) {
            char character = input.charAt(index);

            //The Regex will be embedded into '/' therefore we need to escape them.
            if (character == find) {
                stringBuilder.append(replace);
                continue;
            }
            stringBuilder.append(character);
        }
        return stringBuilder.toString();
    }

    static String replacePreSized(String input, char find, char replace) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        StringBuilder stringBuilder = new StringBuilder(Math.max(16, input.length()));
        for (int index = 0; index < input.length(); index++) {
            char character = input.charAt(index);

            //The Regex will be embedded into '/' therefore we need to escape them.
            if (character == find) {
                stringBuilder.append(replace);
                continue;
            }
            stringBuilder.append(character);
        }
        return stringBuilder.toString();
    }


    static String replaceLazy(String input, char find, char replace) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder stringBuilder = null;
        for (int index = 0; index < input.length(); index++) {
            char character = input.charAt(index);

            //The Regex will be embedded into '/' therefore we need to escape them.
            if (character == find) {
                if (stringBuilder == null) {
                    stringBuilder = new StringBuilder(Math.max(16, input.length() + 1));
                    stringBuilder.append(input, 0, Math.max(0, index - 1));
                }
                stringBuilder.append(replace);
                continue;
            }
            if (stringBuilder != null)
                stringBuilder.append(character);
        }
        return stringBuilder == null ? input : stringBuilder.toString();
    }


    public static void main(String[] args) throws RunnerException {
        verify();

        new Runner(new OptionsBuilder()//
                .include(StringReplacePatternJMH.class.getName() + ".*")//
                //.addProfiler(GCProfiler.class)//
                .resultFormat(ResultFormatType.JSON)
                .result(String.format("%s_%s.json",
                        DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                        StringReplacePatternJMH.class.getSimpleName()))
                .build()).run();
    }

    private static void verify() {
        MyState state = new MyState(1, Match.MATCH);

        testImplementations("replaceSingle_", "the_Typical_New_Line_Unescape_Replace", state);
        testImplementations("remove_", "theTypicalNewLineUnescapeReplace", state);
        testImplementations("replace_", "the\nTypical\nNew\nLine\nUnescape\nReplace", state);
        testImplementations("multiReplace_", "the\r\n\tTypical\r\n\tNew\r\n\tLine\r\n\tUnescape\r\n\tReplace", state);
    }

    private static void testImplementations(String prefix, String expected, MyState state) {

        List<Method> toTest = Arrays.stream(StringReplacePatternJMH.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(Benchmark.class))
                .filter(m -> m.getName().startsWith(prefix))
                .collect(Collectors.toList());

        System.out.println("Verifying: " + toTest.size() + " '" + prefix + "'*  implementations" + toTest.size());
        toTest.forEach(m -> verifyExpectedResult(expected, state, m));
    }

    private static void verifyExpectedResult(String expected, MyState state, Method m) {
        StringReplacePatternJMH s = new StringReplacePatternJMH();
        RuntimeException err = null;
        try {
            Object o = m.invoke(s, state);

            if (!(o instanceof String)) {
                err = new RuntimeException(m.getName() + " did not produce expected result. Result is null or of wrong type: " + (o == null ? "-null-" : o.getClass()));
            }
            if (!expected.equals(o)) {
                err = new RuntimeException(m.getName() + " did not produce expected result.\nExpected:\n'" + expected + "'\nActual:\n'" + o + "'");
            }
        } catch (ReflectiveOperationException e) {
            err = new RuntimeException("Error invoking method: " + m.getName(), e);
        }

        if (err == null) {
            System.out.println(" OK " + m.getName());
        } else {
            System.out.println("NOK " + m.getName());
            throw err;
        }

    }
}
