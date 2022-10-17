package de.frank.jmh.algorithms;

import de.frank.jmh.BenchmarkFormatter;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.ahocorasick.trie.Trie;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;


/*--
Task:
find and redact sensitive token's in Log-Messages.

Challenges:
 - Tokes are NOT clearly delimited - so a token might be
   - embedded into the text: "foo <Token> bar"  "foo <Token>. Bar"
   - prefixed: "foo<Token>, bar"  "theToken=<TOKEN>" bar"
   - or, part of a uri /foo/bar/<TOKEN>
 - however, we are relatively certain, that our log messages are well structured and a token is followed by a delimiter. e.g.: space . , # !

Tokens are:
 - know to start with a known prefix (100 to 1000 prefixes exist)
 - are either 7 or 17 chars long
 - MUST contain at least on Digit AND at least one Letter!

Approaches:
- Simple naive approach: sliding window across input and test each a window of 7 and a window of 17 if it fulfills the token conditions above.
  - withPrefix: additionally check if the token starts with one of the known prefixes (eliminates a bunch of false positives like: "foo RandStr17CharLong, bar")
- AhoCorasickSearch: build a search trie from all known prefixes and use ahoCorasickSearch to find prefixes indicating the start token candidates. Then just expand 17-prefix.len chars beyond the prefix and test for the known conditions, just like the simple approach.
- Regex: multiple complex regexes for each length and circumstance a token might occur (e.g. prefixed, not prefixed , len 7, len17)
  -  f you haven't guessed by now, i am strongly against regexes.  This is mostly a "MultiString-Prefix-SEARCH Problem". Use a search algorithm FFS!
  - However this is a real life example, where this was the implemented solution
- RegexSplit: just tokenize the log message by "Delimiter" boundaries and then check each token if it fulfills our rules and needs redaction.

TL;DR:
- Regex sucks in all dimensions.
   - They waste everyone's time: The computers (performance), yours (writing them, as they are a absolute nightmare to get right!), and most importantly the time of your colleagues/maintainers (learning,understanding and debugging them)
   - And even if it is "right" they will almost certainly match more then you thought!
   - To get it right, you probably end up verifying the found tokens again with java code, coding the rules (just like in the other solutions) -> you loose the ability to use capture groups and "replace()" and  the "shortness" of the solution (albeit "short" was never a good reason to do it)
   - When they do not work, you have no good tools to debug them, as they are essentially a (shitty) little program inside your full fledged programming language
- The simple approach is already 6x more performant, was written in way less time than the regex's, and can be understood & extended by everyone.
- The optimized version, using aho-corasick search, is even simpler to understand, was written as fast as the simple approach and is >=10x faster!

Regex are only good for use in bash scripting, where you typically dont have a full fledged programming language and an open IDE.


hasTokenChance=0.5      stringLength:   100       100       100000       100000
                       prefixesCount:    10       100           10          100
ahoCorasickSearch                     2,882     1,618    2.577,740    2.274,599 us/op
simpleSlidingWindow                   1,672     2,077    4.731,582    4.601,062 us/op
simpleSlidingWindowWithPrefix         4,305    10,788    4.834,113    5.290,023 us/op
regexSplitWord                        7,731     6,202    6.785,387   10.734,895 us/op
regexFromProject                     16,440    16,112   34.452,572   22.196,037 us/op

Benchmark                      (hasTokenChance)  (prefixesCount)  (stringLength)  Mode  Cnt      Score       Error  Units
baseline_randomInput                        0.0               10             100  avgt   10      0,007 ±     0,001  us/op
baseline_randomInput                        0.0              100             100  avgt   10      0,012 ±     0,004  us/op
baseline_randomInput                        0.5               10             100  avgt   10      0,006 ±     0,001  us/op
baseline_randomInput                        0.5              100             100  avgt   10      0,006 ±     0,001  us/op
baseline_randomInput                        1.0               10             100  avgt   10      0,007 ±     0,001  us/op
baseline_randomInput                        1.0              100             100  avgt   10      0,007 ±     0,001  us/op
baseline_randomInput                        0.0               10          100000  avgt   10      0,007 ±     0,001  us/op
baseline_randomInput                        0.0              100          100000  avgt   10      0,007 ±     0,001  us/op
baseline_randomInput                        0.5               10          100000  avgt   10      0,009 ±     0,004  us/op
baseline_randomInput                        0.5              100          100000  avgt   10      0,006 ±     0,001  us/op
baseline_randomInput                        1.0               10          100000  avgt   10      0,009 ±     0,004  us/op
baseline_randomInput                        1.0              100          100000  avgt   10      0,007 ±     0,001  us/op
baseline_replacer                           0.0               10             100  avgt   10      0,903 ±     0,331  us/op
baseline_replacer                           0.0              100             100  avgt   10      0,620 ±     0,091  us/op
baseline_replacer                           0.5               10             100  avgt   10      0,521 ±     0,071  us/op
baseline_replacer                           0.5              100             100  avgt   10      0,504 ±     0,049  us/op
baseline_replacer                           1.0               10             100  avgt   10      0,478 ±     0,008  us/op
baseline_replacer                           1.0              100             100  avgt   10      0,651 ±     0,327  us/op
baseline_replacer                           0.0               10          100000  avgt   10    612,942 ±    47,805  us/op
baseline_replacer                           0.0              100          100000  avgt   10    510,505 ±   152,289  us/op
baseline_replacer                           0.5               10          100000  avgt   10    383,762 ±    13,197  us/op
baseline_replacer                           0.5              100          100000  avgt   10    544,462 ±   110,781  us/op
baseline_replacer                           1.0               10          100000  avgt   10    380,612 ±     3,954  us/op
baseline_replacer                           1.0              100          100000  avgt   10    399,410 ±    15,760  us/op

ahoCorasickSearch                           0.0               10             100  avgt   10      2,137 ±     0,156  us/op
ahoCorasickSearch                           0.0              100             100  avgt   10      2,009 ±     0,060  us/op
ahoCorasickSearch                           0.5               10             100  avgt   10      2,882 ±     0,502  us/op
ahoCorasickSearch                           0.5              100             100  avgt   10      1,618 ±     0,074  us/op
ahoCorasickSearch                           1.0               10             100  avgt   10      2,261 ±     0,834  us/op
ahoCorasickSearch                           1.0              100             100  avgt   10      1,772 ±     0,236  us/op
ahoCorasickSearch                           0.0               10          100000  avgt   10   2395,044 ±   105,181  us/op
ahoCorasickSearch                           0.0              100          100000  avgt   10   1940,604 ±    41,871  us/op
ahoCorasickSearch                           0.5               10          100000  avgt   10   2577,740 ±   315,649  us/op
ahoCorasickSearch                           0.5              100          100000  avgt   10   2274,599 ±   885,508  us/op
ahoCorasickSearch                           1.0               10          100000  avgt   10   2488,694 ±   326,540  us/op
ahoCorasickSearch                           1.0              100          100000  avgt   10   1994,650 ±    34,044  us/op
regexFromProject                            0.0               10             100  avgt   10     16,855 ±     0,474  us/op
regexFromProject                            0.0              100             100  avgt   10     21,480 ±     7,522  us/op
regexFromProject                            0.5               10             100  avgt   10     16,440 ±     2,129  us/op
regexFromProject                            0.5              100             100  avgt   10     16,112 ±     0,559  us/op
regexFromProject                            1.0               10             100  avgt   10     14,503 ±     2,688  us/op
regexFromProject                            1.0              100             100  avgt   10     15,217 ±     0,660  us/op
regexFromProject                            0.0               10          100000  avgt   10  24629,393 ±  3894,399  us/op
regexFromProject                            0.0              100          100000  avgt   10  22807,831 ±   972,165  us/op
regexFromProject                            0.5               10          100000  avgt   10  34452,572 ± 13692,761  us/op
regexFromProject                            0.5              100          100000  avgt   10  22196,037 ±   723,825  us/op
regexFromProject                            1.0               10          100000  avgt   10  31493,049 ± 10893,137  us/op
regexFromProject                            1.0              100          100000  avgt   10  22849,905 ±  1624,013  us/op
regexSplitWord                              0.0               10             100  avgt   10      9,395 ±     1,629  us/op
regexSplitWord                              0.0              100             100  avgt   10      4,949 ±     0,164  us/op
regexSplitWord                              0.5               10             100  avgt   10      7,731 ±     2,852  us/op
regexSplitWord                              0.5              100             100  avgt   10      6,202 ±     0,546  us/op
regexSplitWord                              1.0               10             100  avgt   10      5,521 ±     0,553  us/op
regexSplitWord                              1.0              100             100  avgt   10      6,127 ±     1,929  us/op
regexSplitWord                              0.0               10          100000  avgt   10   6776,026 ±   126,941  us/op
regexSplitWord                              0.0              100          100000  avgt   10   7604,997 ±  2954,973  us/op
regexSplitWord                              0.5               10          100000  avgt   10   6785,387 ±   250,963  us/op
regexSplitWord                              0.5              100          100000  avgt   10  10734,895 ±  2678,102  us/op
regexSplitWord                              1.0               10          100000  avgt   10   6845,431 ±   354,994  us/op
regexSplitWord                              1.0              100          100000  avgt   10   7935,046 ±  3072,609  us/op
simpleSlidingWindow                         0.0               10             100  avgt   10      1,732 ±     0,059  us/op
simpleSlidingWindow                         0.0              100             100  avgt   10      2,856 ±     0,825  us/op
simpleSlidingWindow                         0.5               10             100  avgt   10      1,672 ±     0,063  us/op
simpleSlidingWindow                         0.5              100             100  avgt   10      2,077 ±     0,666  us/op
simpleSlidingWindow                         1.0               10             100  avgt   10      1,643 ±     0,109  us/op
simpleSlidingWindow                         1.0              100             100  avgt   10      1,936 ±     0,060  us/op
simpleSlidingWindow                         0.0               10          100000  avgt   10   4688,097 ±   349,794  us/op
simpleSlidingWindow                         0.0              100          100000  avgt   10   4573,873 ±    83,595  us/op
simpleSlidingWindow                         0.5               10          100000  avgt   10   4731,582 ±   751,199  us/op
simpleSlidingWindow                         0.5              100          100000  avgt   10   4601,062 ±   201,362  us/op
simpleSlidingWindow                         1.0               10          100000  avgt   10   6286,443 ±  1359,125  us/op
simpleSlidingWindow                         1.0              100          100000  avgt   10   4559,116 ±    98,008  us/op
simpleSlidingWindowWithPrefix               0.0               10             100  avgt   10      3,224 ±     0,334  us/op
simpleSlidingWindowWithPrefix               0.0              100             100  avgt   10      3,694 ±     0,079  us/op
simpleSlidingWindowWithPrefix               0.5               10             100  avgt   10      4,305 ±     1,189  us/op
simpleSlidingWindowWithPrefix               0.5              100             100  avgt   10     10,788 ±    10,889  us/op
simpleSlidingWindowWithPrefix               1.0               10             100  avgt   10      3,278 ±     0,064  us/op
simpleSlidingWindowWithPrefix               1.0              100             100  avgt   10      5,031 ±     0,993  us/op
simpleSlidingWindowWithPrefix               0.0               10          100000  avgt   10   5406,434 ±  1757,716  us/op
simpleSlidingWindowWithPrefix               0.0              100          100000  avgt   10   4065,479 ±   155,825  us/op
simpleSlidingWindowWithPrefix               0.5               10          100000  avgt   10   4834,113 ±   184,786  us/op
simpleSlidingWindowWithPrefix               0.5              100          100000  avgt   10   5290,023 ±   993,641  us/op
simpleSlidingWindowWithPrefix               1.0               10          100000  avgt   10   4230,822 ±   195,416  us/op
simpleSlidingWindowWithPrefix               1.0              100          100000  avgt   10   4106,702 ±   404,745  us/op



 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Threads(1)
@Fork(1) //if you promise to not use this machine during benchmarking->1 is ok. Watching youtube?-> set to 3
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)//read @Fork comment - consider setting to 10
@State(Scope.Thread)
public class SearchAndRedactLogMessages2JMH {

    private static final char[] PREFIX_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final int PREFIX_LEN = 3;
    private static final int TOKEN_LENGTH = 17;


    @Param({
            "10",
            "100"
    })
    int prefixesCount;

    @Param({
            "100",
            "100000"
    })
    int stringLength;

    @Param({
            "0.0",
            "0.5",
            "1.0"
    })
    double hasTokenChance;

    private String stringWithoutToken;
    private String stringWithToken;

    private RedactingFinder_SimpleSlidingWindow tokenFinder_SimpleSlidingWindow;
    private RedactingFinder_ahoCorasick tokenFinder_ahoCorasick;
    private StringRedactor_regexFromProject tokenFinder_regex_from_project;
    private StringRedactor_RegexWordTokenizer regexSplitWord;
    private RedactingFinder_SimpleSlidingWindowWithPrefix tokenFinder_simpleSlidingWindowWithPrefix;

    @Setup
    public void setup() {
        //generate test input
        Set<String> randTokenPrefixes = randomUniqueStrings(prefixesCount, PREFIX_LEN, PREFIX_ALPHABET);
        StringBuilder result = randomString(stringLength, ThreadLocalRandom.current());
        this.stringWithToken = injectTokensIntoString(result, generateValidToken(randTokenPrefixes));
        this.stringWithoutToken = injectTokensIntoString(result, "");

        //setup benchmark candidates
        this.tokenFinder_SimpleSlidingWindow = new RedactingFinder_SimpleSlidingWindow();
        this.tokenFinder_simpleSlidingWindowWithPrefix = new RedactingFinder_SimpleSlidingWindowWithPrefix(randTokenPrefixes);
        this.tokenFinder_ahoCorasick = new RedactingFinder_ahoCorasick(randTokenPrefixes);
        this.tokenFinder_regex_from_project = new StringRedactor_regexFromProject(/* only knows its hardcoded pattern*/);
        this.regexSplitWord = new StringRedactor_RegexWordTokenizer(randTokenPrefixes);
    }

    @Benchmark
    public String baseline_randomInput() {
        return generateTestInput();
    }

    @Benchmark
    public String baseline_replacer() {
        String in = generateTestInput();
        return StringReplacer.forInput(in, TOKEN_REDACTOR).replaceAll(List.of(new Match(in.length() / 2, in.length() / 2 + 7))).build();
    }

    @Benchmark
    public String simpleSlidingWindow() {
        String in = generateTestInput();
        return tokenFinder_SimpleSlidingWindow.redact(in);
    }

    @Benchmark
    public String simpleSlidingWindowWithPrefix() {
        String in = generateTestInput();
        return tokenFinder_simpleSlidingWindowWithPrefix.redact(in);
    }

    @Benchmark
    public String ahoCorasickSearch() {
        String in = generateTestInput();
        return tokenFinder_ahoCorasick.redact(in);

    }

    @Benchmark
    public String regexSplitWord() {
        String in = generateTestInput();
        return regexSplitWord.redact(in);
    }


    @Benchmark
    public String regexFromProject() {
        String in = generateTestInput();
        return tokenFinder_regex_from_project.redact(in);
    }

    public String generateTestInput() {
        return ThreadLocalRandom.current().nextDouble() >= hasTokenChance ? stringWithToken : stringWithoutToken;
    }


    @Data
    @AllArgsConstructor
    public static class Match {
        int start;
        int end;
    }

    public interface TokenFinder {
        List<Match> findTokens(String input);
    }

    public interface StringRedactor {
        String redact(String input);
    }

    public static abstract class RedactingFinder implements TokenFinder, StringRedactor {
        private final StringReplacer.ReplacementProvider replacementProvider;

        public RedactingFinder(StringReplacer.ReplacementProvider replacementProvider) {
            this.replacementProvider = replacementProvider;
        }

        @Override
        public String redact(String input) {
            List<Match> matchPositions = findTokens(input); //matches may overlap.

            return StringReplacer.forInput(input, replacementProvider).replaceAll(matchPositions).build();
        }
    }

    public static class StringReplacer {
        private StringBuilder buffer = null;
        private final String input;
        private int pos = 0;

        private final ReplacementProvider replacer;

        public interface ReplacementProvider {
            void accept(String source, Match pos, StringBuilder result);

        }

        public StringReplacer(String source, ReplacementProvider replacer) {
            this.input = source;
            this.replacer = replacer;
        }

        public static StringReplacer forInput(String in, ReplacementProvider replacer) {
            return new StringReplacer(in, replacer);
        }

        private StringReplacer replaceAll(Collection<Match> matches) {
            matches.forEach(this::replace);
            return this;
        }

        private StringReplacer replace(Match m) {
            if (pos >= input.length()) {
                throw new IllegalArgumentException("Redaction already finished and last token was appended.");
            }
            if (buffer == null) {
                buffer = new StringBuilder(input.length());
            }
            buffer.append(input, pos, m.getStart());
            pos = m.getEnd();
            replacer.accept(input, m, buffer);
            return this;
        }

        /**
         * terminal operation. (Appends remaining dangling token)
         *
         * @return the finished string
         */
        public String build() {
            if (buffer == null) {
                return input;
            } else {
                if (pos < input.length()) { //append last token
                    buffer.append(input, pos, input.length());
                }
                return buffer.toString();
            }
        }

        public String toString() {
            if (pos < input.length()) {
                throw new IllegalArgumentException("Redaction not finished yet. Call build() as a terminal operation");
            }
            return buffer.toString();
        }
    }


    private static final StringReplacer.ReplacementProvider TOKEN_REDACTOR = (source, /*Match*/ match, /*StringBuilder*/ result) -> {
        String rightmost4Chars = source.substring(match.getEnd() - 4, match.getEnd());
        int tokenLen = match.getEnd() - match.getStart();
        result.append(tokenLen <= 7 ? "TKN_7_MASKED(" : "TKN_17_MASKED(")
                .append(rightmost4Chars)
                .append(")");
    };


    //#######################################################################
    // Implementations to test against each other
    //#######################################################################


    /**
     * As project "solution" is a domain-specialized implementation, we will go the same route with the alternative implementations, instead of creating a generic implementation
     */
    public static class RedactingFinder_SimpleSlidingWindow extends RedactingFinder {

        public RedactingFinder_SimpleSlidingWindow() {
            super(TOKEN_REDACTOR);
        }

        public List<Match> findTokens(String in) {
            ArrayList<Match> r = new ArrayList<>();
            // Plain simple algorithm:
            // sliding window over input, look ahead the next 7a and 17 chars and test them for isValidToken
            // Obvious optimizations: check for known prefixes BEFORE tokenization with a search lib. (See other impls)

            for (int pos = 0; pos < in.length() - TokenValidation.TOKEN_MAX_LEN_7; pos++) {

                //we can have two possible lengths..
                //..7 chars
                if (TokenValidation.isValidToken(in, pos, TokenValidation.TOKEN_MAX_LEN_7)) {
                    r.add(new Match(pos, pos + TokenValidation.TOKEN_MAX_LEN_7));
                    //we dont want overlaps -> skip ahead
                    pos += TokenValidation.TOKEN_MAX_LEN_7;
                    continue; //if len 7 matches, 17 must not be matched
                }
                //..and 17 chars
                if (TokenValidation.isValidToken(in, pos, TokenValidation.TOKEN_MAX_LEN_17)) {
                    r.add(new Match(pos, pos + TokenValidation.TOKEN_MAX_LEN_17));
                    //we dont want overlaps -> skip ahead
                    pos += TokenValidation.TOKEN_MAX_LEN_17;
                }

            }
            return r;
        }
    }

    public static class RedactingFinder_SimpleSlidingWindowWithPrefix extends RedactingFinder {

        private final Set<String> validTokenPrefixes;

        public RedactingFinder_SimpleSlidingWindowWithPrefix(Set<String> validTokenPrefixes) {
            super(TOKEN_REDACTOR);
            this.validTokenPrefixes = validTokenPrefixes;
        }

        public List<Match> findTokens(String in) {
            ArrayList<Match> r = new ArrayList<>();
            // Plain simple algorithm:
            // sliding window over input, look ahead the next 7a and 17 chars and test them for isValidToken
            // Obvious optimizations: check for known prefixes BEFORE tokenization with a search lib. (See other impls)

            for (int pos = 0; pos < in.length() - TokenValidation.TOKEN_MAX_LEN_7; pos++) {
                //we know a valid token must start with a known prefix
                if (!validTokenPrefixes.contains(in.substring(pos, pos + TokenValidation.TOKEN_PREFIX_LEN))) {
                    continue;
                }
                //we can have two possible lengths..
                //..7 chars
                if (TokenValidation.isValidToken(in, pos, TokenValidation.TOKEN_MAX_LEN_7)) {
                    r.add(new Match(pos, pos + TokenValidation.TOKEN_MAX_LEN_7));
                    //we dont want overlaps -> skip ahead
                    pos += TokenValidation.TOKEN_MAX_LEN_7;
                    continue; //if len 7 matches, 17 must not be matched
                }
                //..and 17 chars
                if (TokenValidation.isValidToken(in, pos, TokenValidation.TOKEN_MAX_LEN_17)) {
                    r.add(new Match(pos, pos + TokenValidation.TOKEN_MAX_LEN_17));
                    //we dont want overlaps -> skip ahead
                    pos += TokenValidation.TOKEN_MAX_LEN_17;
                }

            }
            return r;
        }
    }


    /**
     * Splits the problem into "search" and "replace" parts.
     * We KNOW that we have a set of knwon prefixes, which the original implementation did not consider using.
     * By searching for the known prefix start points in search text, we can reduce the problem complexity to "string search"
     * Then we check the other "is token of interest" rules.
     * <p>
     * This implementation will ignore word boundaries!
     */
    public static class RedactingFinder_ahoCorasick extends RedactingFinder {

        private final Trie validTokenPrefixes;

        public RedactingFinder_ahoCorasick(Set<String> validTokenPrefixes) {
            super(TOKEN_REDACTOR);
            this.validTokenPrefixes = Trie.builder().addKeywords(validTokenPrefixes).build();
        }

        public List<Match> findTokens(String in) {
            final ArrayList<Match> foundTokens = new ArrayList<>();

            //the trie finds overlapping matches -> we want to skip over these overlapping matches

            final AtomicInteger lastHitEnd = new AtomicInteger(0);
            //optimized search for list of known token prefixes
            validTokenPrefixes.parseText(in, foundPrefix -> {

                //skip overlapping matches
                if (foundPrefix.overlapsWith(lastHitEnd.get())) {
                    return false; //if searchTrie.isStopOnHit && return true  from here -> end search here
                }

                //thecheck for a valid prefix, if the following chars satisfy the token rules.
                //we have two possibilities, tokens of len 7 and 17.
                // If a token satisfies the rules for 7, it cannot be 17 long as well.
                if (TokenValidation.isValidToken(in, foundPrefix.getStart(), TokenValidation.TOKEN_MAX_LEN_7)) {
                    foundTokens.add(new Match(foundPrefix.getStart(), foundPrefix.getStart() + TokenValidation.TOKEN_MAX_LEN_7));
                    lastHitEnd.set(foundPrefix.getEnd());
                    return true;  //if searchTrie.isStopOnHit && return true  from here -> end search here
                }

                if (TokenValidation.isValidToken(in, foundPrefix.getStart(), TokenValidation.TOKEN_MAX_LEN_17)) {
                    foundTokens.add(new Match(foundPrefix.getStart(), foundPrefix.getStart() + TokenValidation.TOKEN_MAX_LEN_17));
                    lastHitEnd.set(foundPrefix.getEnd());
                    return true;  //if searchTrie.isStopOnHit && return true  from here -> end search here
                }

                return false; //if searchTrie.isStopOnHit && return true  from here -> end search here
            });

            return foundTokens;
        }
    }

    /**
     * As project "solution" is a domain-specialized implementation, we will go the same route with the alternative implementations, instead of creating a generic implementation
     */
    public static class StringRedactor_RegexWordTokenizer implements StringRedactor {
        private static final int TOKEN_PREFIX_LEN = 3;
        private static final int TOKEN_MAX_LEN_7 = 7;
        private static final int TOKEN_MAX_LEN_17 = 17;


        //Attention! if your split char is not a regex e.g.: "foo,bar".split(",") is faster, as it has a fastpath for non-regex stuff.
        //split on word boundaries \b but keep the delimiter
        //e.g.: "Hello-World:How\nAre You&doing"
        //OUTPUT:
        //a[0] = "Hello"
        //a[1] = "-"
        //a[2] = "World"
        //a[3] = ":"
        //a[4] = "How"
        //a[5] = "
        //"
        //a[6] = "Are"
        //a[7] = " "
        //a[8] = "You"
        //a[9] = "&"
        //a[10] = "doing"
        private static final Pattern SPLIT_ON_WORD = Pattern.compile("(?!^)\\b");

        private final Set<String> validTokenPrefixes;

        public StringRedactor_RegexWordTokenizer(Set<String> validTokenPrefixes) {
            this.validTokenPrefixes = validTokenPrefixes;
        }

        public String redact(String in) {
            StringBuilder sb = new StringBuilder();
            // tokens might be prefixed with something but are known to be never postfixed! so we can just split of the tokens starting from the end
            for (String curToken : SPLIT_ON_WORD.split(in)) {
                if (curToken.length() < TOKEN_MAX_LEN_7) {
                    sb.append(curToken);  // nothing to redact - to short
                    continue;//cannot be a valid token
                }

                if (curToken.length() >= TOKEN_MAX_LEN_17) {
                    String vin17Candidate = curToken.substring(curToken.length() - TOKEN_MAX_LEN_17);
                    if (TokenValidation.isValidToken(vin17Candidate, validTokenPrefixes)) {
                        redact(curToken, vin17Candidate, sb);
                        continue;
                    }
                }

                String vin7Candidate = curToken.substring(curToken.length() - TOKEN_MAX_LEN_7);
                if (TokenValidation.isValidToken(vin7Candidate, validTokenPrefixes)) {
                    redact(curToken, vin7Candidate, sb);
                    continue;
                }
                // nothing to redact - neither token7 nor token17 - append as is.
                sb.append(curToken);

            }
            return sb.toString();
        }


        private void redact(String splitterToken, String matchingTokenPart, StringBuilder buf) {
            //chars left of actual token
            buf.append(splitterToken, 0, splitterToken.length() - matchingTokenPart.length());
            redactMatchingTokenItself(matchingTokenPart, buf);
        }

        protected void redactMatchingTokenItself(String matchingTokenPart, StringBuilder buf) {
            String rightmost4Chars = StringUtils.right(matchingTokenPart, 4);//keep the rightmost 4 chars of the token we vant to redact
            buf.append(matchingTokenPart.length() <= 7 ? "VIN_7_MASKED(" : "VIN_17_MASKED(")
                    .append(rightmost4Chars)
                    .append(")");
        }
    }


    /**
     * The "offender" - as my personal opinion is: regex is never the answer.
     */
    private static class StringRedactor_regexFromProject implements StringRedactor {
        // Search for:
        //   - token as part of a path:  /foo/bar/{tokenToSearchFor}
        //     e.g.: "/foo/bar/VIN123a"
        //   - and replace to:  "/foo/bar/VIN_7_MASKED(123a)"
        // Regex checks for:
        //   - a 7-character alphanumeric string
        //     - which is preceded by a "/"
        //     - contains at least one number [a-zA-Z0-9]
        //   - and is followed by a word boundary.
        // The lookahead part of the regex - i.e. the block "(?![a-zA-Z]{7)" - is used to ignore all strings that do not contain a number but would otherwise match.
        // The VIN part is divided into two capturing groups ([a-zA-Z0-9]{3}) and ([a-zA-Z0-9]{4}) so that the second part (4 characters) can be kept for search purposes.
        private static final String VIN7_IN_PATH_SEARCH = "/(?![a-zA-Z]{7})([a-zA-Z0-9]{3})([a-zA-Z0-9]{4})\\b";
        private static final String VIN7_IN_PATH_REPLACEMENT = "/VIN_7_MASKED($2)";

        //Search for:
        //   - token as part of a path:  /foo/bar/{tokenToSearchFor}
        //     e.g.: "/foo/bar/VIN123abcdefghijk"
        //   - and replace to:  "/foo/bar/VIN_17_MASKED(hijk)"
        // Regex checks for:
        //   - Checks for a 17-character alphanumeric string
        //     - which is preceded by a "/"
        //     - which contains at least one number
        //   - and is followed by a word boundary. \b

        // The lookahead part of the regex - i.e. the block "(?![a-zA-Z]{17)" - is used to ignore all strings that do not contain a number but would otherwise match.
        // The VIN part is divided into two capturing groups ([a-zA-Z0-9]{3}) and ([a-zA-Z0-9]{4}) so that the second part (4 characters) can be kept for search purposes.
        private static final String VIN17_IN_PATH_SEARCH = "/(?![a-zA-Z]{17})([a-zA-Z0-9]{13})([a-zA-Z0-9]{4})\\b";
        private static final String VIN17_IN_PATH_REPLACEMENT = "/VIN_17_MASKED($2)";

        // Explanation:
        //   - Checks for "<whatever> <someVariableName>={tokenToSearchFor} <whatever>"
        //     e.g. "foo someVar=VIN123a, bar" and
        //   - replace to: "foo someVar=VIN_7_MASKED(123a), bar"
        // Regex checks for:
        //   - a word boundary: \b
        //   - followed by a random variable name: (\w*)
        //       - plus one separation char (space or =): ([=\s])
        //   - followed by a 7-character alphanumeric string
        //      - which contains at least one number
        //   - and followed by a word boundary: \b
        // The lookahead part of the regex - i.e. the block "(?![a-zA-Z]{7)" - is used to ignore all strings that do not contain a number but would otherwise match.
        // The VIN part is divided into two capturing groups ([a-zA-Z0-9]{3}) and ([a-zA-Z0-9]{4}) so that the second part (4 characters) can be kept for search purposes.
        private static final String VIN7_IN_KEY_VALUE_PAIR_SEARCH = "\\b(\\w*)([=\\s])(?![a-zA-Z]{7})([a-zA-Z0-9]{3})([a-zA-Z0-9]{4})";
        private static final String VIN7_IN_KEY_VALUE_PAIR_REPLACEMENT = "$1$2VIN_7_MASKED($4)";

        // Explanation:
        //   - Checks for "<whatever> <someVariableName>={tokenToSearchFor} <whatever>"  e.g. "foo someVar=VIN123a, bar" and
        //   - replace to: "foo someVar=VIN_7_MASKED(123a), bar"
        // Regex checks for:
        //   - Checks for a word boundary  \b
        //   - followed by a 17-character alphanumeric string
        //     - which is preceded by a random variable name plus one separation char (space or =): (\w*)([=\s])(
        //     - which contains at least one number
        //   - and followed by a word boundary. \b
        // The lookahead part of the regex - i.e. the block "(?![a-zA-Z]{17)" - is used to ignore all strings that do not contain a number but would otherwise match.
        // The VIN part is divided into two capturing groups so that the second part (4 characters) can be kept for search purposes.
        private static final String VIN17_IN_KEY_VALUE_PAIR_SEARCH = "\\b(\\w*)([=\\s])(?![a-zA-Z]{17})([a-zA-Z0-9]{13})([a-zA-Z0-9]{4})\\b";
        private static final String VIN17_IN_KEY_VALUE_PAIR_REPLACEMENT = "$1$2VIN_17_MASKED($4)";


        private static final List<Pair<Pattern, String>> COMPILED_PATTERNS = List.of(
                Pair.of(Pattern.compile(VIN7_IN_PATH_SEARCH), VIN7_IN_PATH_REPLACEMENT),
                Pair.of(Pattern.compile(VIN17_IN_PATH_SEARCH), VIN17_IN_PATH_REPLACEMENT),
                Pair.of(Pattern.compile(VIN7_IN_KEY_VALUE_PAIR_SEARCH), VIN7_IN_KEY_VALUE_PAIR_REPLACEMENT),
                Pair.of(Pattern.compile(VIN17_IN_KEY_VALUE_PAIR_SEARCH), VIN17_IN_KEY_VALUE_PAIR_REPLACEMENT)
        );

        public String redact(String stringToRewrite) {
            for (Pair<Pattern, String> replacementPattern : COMPILED_PATTERNS) {
                Pattern pattern = replacementPattern.getLeft();
                String replacement = replacementPattern.getRight();
                stringToRewrite = rewriteString(stringToRewrite, pattern, replacement);
            }
            return stringToRewrite;
        }

        private static String rewriteString(String formattedMessage, Pattern pattern, String replacement) {
            return pattern.matcher(formattedMessage).replaceAll(replacement);
        }
    }


    private static String generateValidToken(Set<String> prefixes) {
        Random r = ThreadLocalRandom.current();
        String prefix = (String) prefixes.toArray()[r.nextInt(prefixes.size())];//choose a random prefix
        String token;
        do {
            int tokenLen = r.nextBoolean() ? TokenValidation.TOKEN_MAX_LEN_7 : TokenValidation.TOKEN_MAX_LEN_17;
            token = prefix + RandomStringUtils.randomAlphanumeric(tokenLen - prefix.length());
            //simple rejection sampling
        } while (!TokenValidation.satisfiesTokenCharacterRules(token, 0, token.length()));

        return token + " ";//tokens are postfixed by a delimiter - we just statically use a space
    }

    private static String injectTokensIntoString(StringBuilder input, String... tokens) {
        Random r = ThreadLocalRandom.current();
        int stringLength = input.length();
        for (String token : tokens) {
            if (StringUtils.isNotEmpty(token)) {
                int replacePos = r.nextInt(stringLength - token.length());
                input.replace(replacePos, replacePos + token.length(), token);
            }
        }
        return input.toString();

    }


    private static StringBuilder randomString(int stringLength, Random r) {
        char[] ALPHABET = "abcdefghijklmnopqrstuvwxyzABCEDFGHIJKLMNOPQRSTUVWXYZ01234567890!\"§$%&/()=?ÜÖÄ;:_'*,.-#+'".toCharArray();
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < stringLength; i++) {
            //mimic text by inserting a few strategic spaces
            if (r.nextDouble() < 1.0 / 8) {//every 8th char is a space
                buf.append(' ');
            } else {
                buf.append(ALPHABET[r.nextInt(ALPHABET.length)]);
            }
        }
        return buf;
    }

    private static Set<String> randomUniqueStrings(int count, int len, char[] alphabet) {
        final long possibilities = (long) Math.pow(alphabet.length, len);

        Set<String> r = new HashSet<>();
        for (int i = 0; i < count; i++) {
            //gnerate random UNIQUE strings!
            //- we just create a simple permuation of the index within the solution space
            //simple permuation to make tokens more random
            long sample = (i * (Integer.MAX_VALUE - 1L/*a prime*/) + 41L) % possibilities;

            char[] result = new char[len];
            numberToString(sample, alphabet, result);
            r.add(new String(result));
        }

        return r;
    }

    private static void numberToString(long generatedNumber, char[] charset, final char[] result) {
        final int charsetLen = charset.length;
        int pos = result.length - 1;
        while (generatedNumber > 0 && pos >= 0) {
            result[pos--] = charset[(int) (generatedNumber % charsetLen)];
            generatedNumber /= charsetLen;
        }
        // padding
        while (pos >= 0) {
            result[pos--] = charset[0];
        }
    }


    static class TokenValidation {
        public static final int TOKEN_PREFIX_LEN = 3;
        public static final int TOKEN_MAX_LEN_7 = 7;
        public static final int TOKEN_MAX_LEN_17 = 17;

        private static boolean isValidToken(String in, int startPos, int tokenLength) {
            int tokenEnd = startPos + tokenLength; //exclusive
            if (tokenEnd > in.length()) {
                //token would not fit in string boundaries, we exceed the input string boundary!
                return false;
            }

            //we know that valid tokens are
            // - either 7 or 17 chars long (consisting of  a-zA-Z0-9)
            // - AND are followed by a delimiter OR end-of-input
            //so we can reject all token candidates, if the char preceding the token is neither a delimiter nor EOL;
            if (tokenEnd == in.length()) {//we are exactly at the end of the input string: "foo <token>"
                return satisfiesTokenCharacterRules(in, startPos, tokenEnd);
            }

            //at this point we know charAt tokenEnd, will definitely not exceed input string boundaries
            char charAfterTokenCandidate = in.charAt(tokenEnd);
            return isDelimiter(charAfterTokenCandidate) && satisfiesTokenCharacterRules(in, startPos, tokenEnd);
        }


        private static boolean satisfiesTokenCharacterRules(String input, int tokenStart, int tokenEnd) {
            boolean hasLetter = false;
            boolean hasNumber = false;

            //valid tokens must have
            // - at least one letter and
            // - at least one digit and
            // - Must-Not contain any other character then digits and letters a-Z
            for (int i = tokenStart; i < tokenEnd; i++) {
                char toTest = input.charAt(i);
                if (Character.isLetter(toTest)) {
                    hasLetter = true;
                } else if (Character.isDigit(toTest)) {
                    hasNumber = true;
                } else {
                    return false;//negative match: contains forbidden char - so cannot be the search string.
                }
            }

            return hasLetter && hasNumber;
        }

        private static boolean isDelimiter(char in) {
            //delimiter=true if neither digit or letter
            return !(Character.isDigit(in) || Character.isLetter(in));
        }

        private static boolean isValidToken(String token, Set<String> prefixes) {
            String prefix = token.substring(0, TOKEN_PREFIX_LEN);
            if (!prefixes.contains(prefix)) {
                //not a valid token
                return false;
            }
            return hasLettersAndNumbers(token);
        }

        public static boolean hasLettersAndNumbers(String token) {
            boolean hasLetter = false;
            boolean hasNumber = false;

            for (int i = 0; i < token.length(); i++) {
                char toTest = token.charAt(i);
                if (Character.isLetter(toTest)) {
                    hasLetter = true;
                } else if (Character.isDigit(toTest)) {
                    hasNumber = true;
                } else {
                    return false;
                }
            }

            return hasLetter && hasNumber;
        }

    }

    public static void main(String[] args) throws Exception {
        System.out.println(StringReplacer.forInput("aaaabbbbbccccddddeeee", (source, pos, result) -> result.append("XX")).replaceAll(List.of(new Match(0, 4), new Match(5, 9))).build());
        System.out.println(" 1: " + TokenValidation.isValidToken("0123456 89", 0, 7));//@start - invalid - only numbers
        System.out.println(" 3: " + TokenValidation.isValidToken("012345A 89", 0, 7));//@start - valid
        System.out.println(" 4: " + TokenValidation.isValidToken("0123456A 9", 1, 7));//@mid  valid
        System.out.println(" 5: " + TokenValidation.isValidToken("0123456AAA", 2, 7));//@mid  invalid - no delimiter
        System.out.println(" 6: " + TokenValidation.isValidToken("012345678A", 3, 7));//@end - valid
        System.out.println(" 7: " + TokenValidation.isValidToken("012345678A", 4, 7));//invalid - exceeds boundaries
        System.out.println(" 9: " + TokenValidation.isValidToken("012345678A", 5, 7));//invalid - exceeds boundaries
        System.out.println(" 9: " + TokenValidation.isValidToken("012345678A", 6, 7));//invalid - exceeds boundaries
        System.out.println("10: " + TokenValidation.isValidToken("012345678A", 7, 7));//invalid - exceeds boundaries
        System.out.println("11: " + TokenValidation.isValidToken("012345678A", 8, 7));//invalid - exceeds boundaries
        System.out.println("12: " + TokenValidation.isValidToken("012345678A", 9, 7));//invalid - exceeds boundaries
        System.out.println("13: " + TokenValidation.isValidToken("012345678A", 10, 7));//invalid - exceeds boundaries
        System.out.println("14: " + TokenValidation.isValidToken("012345678A", 11, 7));//invalid - exceeds boundaries


        Set<String> prefixes = randomUniqueStrings(10, PREFIX_LEN, PREFIX_ALPHABET);
        System.out.println("Prefixes: " + prefixes);
        String tokenToFind = generateValidToken(prefixes);
        StringBuilder base = randomString(100, ThreadLocalRandom.current());
        String logLineWithoutToken = base.toString();
        String logLineWithToken = injectTokensIntoString(base, tokenToFind);
        System.out.println("tokenToFind:         " + tokenToFind);
        System.out.println("logLineWithoutToken: " + logLineWithoutToken);
        System.out.println("logLineWithToken:    " + logLineWithToken);
        //Verify implementation:
        System.out.println("SimpleIteration:");
        System.out.println("  No-token:  " + new RedactingFinder_SimpleSlidingWindow().findTokens(logLineWithoutToken));
        System.out.println("  one token: " + new RedactingFinder_SimpleSlidingWindow().findTokens(logLineWithToken));
        System.out.println("  replace one token: " + new RedactingFinder_SimpleSlidingWindow().redact(logLineWithToken));
        System.out.println("SimpleIterationWithPrefix:");
        System.out.println("  No-token:  " + new RedactingFinder_SimpleSlidingWindowWithPrefix(prefixes).findTokens(logLineWithoutToken));
        System.out.println("  one token: " + new RedactingFinder_SimpleSlidingWindowWithPrefix(prefixes).findTokens(logLineWithToken));
        System.out.println("  replace one token: " + new RedactingFinder_SimpleSlidingWindowWithPrefix(prefixes).redact(logLineWithToken));
        if (!new RedactingFinder_SimpleSlidingWindowWithPrefix(prefixes).redact(logLineWithToken).equals(new RedactingFinder_ahoCorasick(prefixes).redact(logLineWithToken))) {
            System.out.println("bug in ahoCorasik");
        }
        System.out.println("AhoCorasick:");
        System.out.println("  No-token:  " + new RedactingFinder_ahoCorasick(prefixes).findTokens(logLineWithoutToken));
        System.out.println("  one token: " + new RedactingFinder_ahoCorasick(prefixes).findTokens(logLineWithToken));
        System.out.println("  replace one token: " + new RedactingFinder_ahoCorasick(prefixes).redact(logLineWithToken));
        System.out.println("RegexTokenizer:");
        System.out.println("  replace NO token: " + new StringRedactor_RegexWordTokenizer(prefixes).redact(logLineWithoutToken));
        System.out.println("  replace one token: " + new StringRedactor_RegexWordTokenizer(prefixes).redact(logLineWithToken));
        System.out.println("Regex_original:");
        System.out.println("  replace NO token: " + new StringRedactor_regexFromProject().redact(logLineWithoutToken));
        System.out.println("  replace one token: " + new StringRedactor_regexFromProject().redact(logLineWithToken));


        Options benchOptions = new OptionsBuilder()
                .include(SearchAndRedactLogMessages2JMH.class.getName() + ".")
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
        BenchmarkFormatter.displayAsMatrix(results, "stringLength");

    }

}