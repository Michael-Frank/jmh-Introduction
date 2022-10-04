package de.frank.jmh.algorithms;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.ahocorasick.trie.Trie;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/*--
Task:
find and redact a token in Log-Messages. Tokens:
- are NOT delimited - so a token may be embedded into the text: "foo<Token>bar". "foo <Token>, bar" or it might be in parentheses: "foo (<TOKEN>) " or, whatever the developer thought of how to concatenate the token into a log message "theToken=<TOKEN>"
- we know they start with a known prefix (100 to 1000 prefixes exist)
- but we know they are 17 chars long
- we know the MUST contain at least on Digit AND at least one Letter!

Simple naive approach: sliding window of 17 chars across input and test each window if it fulfills the conditions above.
ahoCorasickSearch: build a search trie from all known prefixes and use ahoCorasickSearch to find prefixes indicating the start token candidates. Then just expand 17-prefix.len chars beyond the prefix and test for the known conditions, just like the simple approach.
Regex: you just had to, didnt you? But why? WHY? This is mostly a "MultiString-Prefix-SEARCH Problem". Use a search algorithm FFS!

TL;DR:
- Regex sucks performance wise and its a absolute nightmare to get right! And even if it is "right" it will probably match more then you thought! So you probably end up verifying the found tokens again with java code.
- The simple approach is already 23x more performant, was written in <10minutes and can be understood by everyone.
- The optimized version, using aho-corasick search, is even simpler to understand, was written in another 10minutes and is >=135x faster!

Benchmark                                        (hasTokenChance)  (prefixesCount)  (stringLength)  Mode  Cnt     Score      Error  Units
SearchAndRedactLogMessagesJMH.simpleIteration               0.0               10             100  avgt    5     4,2 ±    2,1 us/op
SearchAndRedactLogMessagesJMH.simpleIteration               0.5               10             100  avgt    5     3,6 ±    1,3 us/op
SearchAndRedactLogMessagesJMH.simpleIteration               1.0               10             100  avgt    5     3,6 ±    1,3 us/op
SearchAndRedactLogMessagesJMH.simpleIteration               0.0              100             100  avgt    5     3,6 ±    1,5 us/op
SearchAndRedactLogMessagesJMH.simpleIteration               0.5              100             100  avgt    5     3,7 ±    1,0 us/op
SearchAndRedactLogMessagesJMH.simpleIteration               1.0              100             100  avgt    5     3,5 ±    2,3 us/op
SearchAndRedactLogMessagesJMH.simpleIteration               0.0               10          100000  avgt    5  4345,6 ± 1016,8 us/op
SearchAndRedactLogMessagesJMH.simpleIteration               0.5               10          100000  avgt    5  4247,9 ± 1634,2 us/op
SearchAndRedactLogMessagesJMH.simpleIteration               1.0               10          100000  avgt    5  4243,3 ± 1972,5 us/op
SearchAndRedactLogMessagesJMH.simpleIteration               0.0              100          100000  avgt    5  3843,8 ±  149,9 us/op
SearchAndRedactLogMessagesJMH.simpleIteration               0.5              100          100000  avgt    5  4333,8 ± 1912,7 us/op
SearchAndRedactLogMessagesJMH.simpleIteration               1.0              100          100000  avgt    5  4796,2 ± 1959,1 us/op
SearchAndRedactLogMessagesJMH.ahoCorasickSearch             0.0               10             100  avgt    5     0,5 ±    0,0 us/op
SearchAndRedactLogMessagesJMH.ahoCorasickSearch             0.5               10             100  avgt    5     0,5 ±    0,1 us/op
SearchAndRedactLogMessagesJMH.ahoCorasickSearch             1.0               10             100  avgt    5     0,4 ±    0,0 us/op
SearchAndRedactLogMessagesJMH.ahoCorasickSearch             0.0              100             100  avgt    5     0,6 ±    0,0 us/op
SearchAndRedactLogMessagesJMH.ahoCorasickSearch             0.5              100             100  avgt    5     0,5 ±    0,1 us/op
SearchAndRedactLogMessagesJMH.ahoCorasickSearch             1.0              100             100  avgt    5     0,4 ±    0,0 us/op
SearchAndRedactLogMessagesJMH.ahoCorasickSearch             0.0               10          100000  avgt    5  1109,3 ±  167,9 us/op
SearchAndRedactLogMessagesJMH.ahoCorasickSearch             0.5               10          100000  avgt    5  1142,0 ±  230,6 us/op
SearchAndRedactLogMessagesJMH.ahoCorasickSearch             1.0               10          100000  avgt    5  1124,2 ±   59,8 us/op
SearchAndRedactLogMessagesJMH.ahoCorasickSearch             0.0              100          100000  avgt    5  1078,5 ±  469,7 us/op
SearchAndRedactLogMessagesJMH.ahoCorasickSearch             0.5              100          100000  avgt    5   902,9 ±  165,7 us/op
SearchAndRedactLogMessagesJMH.ahoCorasickSearch             1.0              100          100000  avgt    5   865,7 ±  155,4 us/op
SearchAndRedactLogMessagesJMH.regex                         0.0               10             100  avgt    5    66,2 ±    1,6 us/op
SearchAndRedactLogMessagesJMH.regex                         0.5               10             100  avgt    5    66,0 ±    4,5 us/op
SearchAndRedactLogMessagesJMH.regex                         1.0               10             100  avgt    5    85,4 ±    3,4 us/op
SearchAndRedactLogMessagesJMH.regex                         0.0              100             100  avgt    5    59,0 ±    2,1 us/op
SearchAndRedactLogMessagesJMH.regex                         1.0              100             100  avgt    5    81,0 ±   14,4 us/op

 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Threads(1)
@Fork(1) //if you promise to not use this machine during benchmarking, 1 is ok. Watching youtube? set to 3
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)//read @Fork comment - consider setting to 10
@State(Scope.Thread)
public class SearchAndRedactLogMessagesJMH {

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

    private TokenFinder_Simple tokenFinder_Simple;
    private TokenFinder_ahoCorasick tokenFinder_ahoCorasick;
    private TokenFinder_regex tokenFinder_regex;

    @Setup
    public void setup() {
        Set<String> randTokenPrefixes = randomUniqueStrings(prefixesCount, PREFIX_LEN, PREFIX_ALPHABET);
        this.stringWithToken = randomStringWithToken(stringLength, generateValidToken(randTokenPrefixes));
        this.stringWithoutToken = randomStringWithToken(stringLength, "");
        this.tokenFinder_Simple = new TokenFinder_Simple(randTokenPrefixes);
        this.tokenFinder_ahoCorasick = new TokenFinder_ahoCorasick(randTokenPrefixes);
        this.tokenFinder_regex = new TokenFinder_regex(randTokenPrefixes);
    }


    @Benchmark
    public List<Match> simpleIteration() {
        String in = ThreadLocalRandom.current().nextDouble() >= hasTokenChance ? stringWithToken : stringWithoutToken;
        return tokenFinder_Simple.findTokens(in);
        //omitted replaceFoundTokensWith("<<REDACTED_TOKEN>>", input, tokens); code as it is redundant for all tests
    }


    @Benchmark
    public List<Match> ahoCorasickSearch() {
        String in = ThreadLocalRandom.current().nextDouble() >= hasTokenChance ? stringWithToken : stringWithoutToken;
        return tokenFinder_ahoCorasick.findTokens(in);
        //omitted replaceFoundTokensWith("<<REDACTED_TOKEN>>", input, tokens); code as it is redundant for all tests
    }

    @Benchmark
    public List<Match> regex() {
        String in = ThreadLocalRandom.current().nextDouble() >= hasTokenChance ? stringWithToken : stringWithoutToken;
        return tokenFinder_regex.findTokens(in);
        //omitted replaceFoundTokensWith("<<REDACTED_TOKEN>>", input, tokens); code as it is redundant for all tests

    }

    private static String replaceFoundTokensWith(String replacement, String input, List<Match> found) {
        if (found == null || found.isEmpty()) {
            return input;
        }
        //this could probably be optimized
        StringBuilder buf = new StringBuilder(replacement);
        found.forEach(match -> buf.replace(match.getStart(), match.getEnd(), replacement));
        return buf.toString();
    }

    public static class TokenFinder_Simple {
        private static final int TOKEN_MAX_LEN = 17;
        private static final int TOKEN_PREFIX_LEN = 3;
        private final Set<String> validTokenPrefixes;

        public TokenFinder_Simple(Set<String> validTokenPrefixes) {
            this.validTokenPrefixes = validTokenPrefixes;
        }

        public List<Match> findTokens(String in) {
            ArrayList<Match> r = new ArrayList<>();
            // Plain simple algorithm:
            // tokenize the whole input into 17 chars long substrings and test them each for "isValidToken"
            // Optimizations: check for known prefixes BEFORE tokenization with a search lib.

            for (int pos = 0; pos < in.length() - TOKEN_MAX_LEN; pos++) {
                String token = in.substring(pos, pos + TOKEN_MAX_LEN);
                if (isValidToken(token)) {
                    r.add(new Match(pos, pos + token.length(), token));
                }
            }
            return r;
        }

        private boolean isValidToken(String token) {
            if (token.length() != TOKEN_MAX_LEN) {
                throw new IllegalArgumentException("Invalid token candidate - a token must be exactly " + TOKEN_MAX_LEN + " chars");
            }

            String prefix = token.substring(0, TOKEN_PREFIX_LEN);
            if (!validTokenPrefixes.contains(prefix)) {
                //not a token
                return false;
            }

            return satiesfiesTokenRules(token);
        }

        public static boolean satiesfiesTokenRules(String token) {
            boolean hasLetter = false;
            boolean hasNumber = false;

            for (int i = TOKEN_PREFIX_LEN; i < TOKEN_MAX_LEN; i++) {
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

    public static class TokenFinder_ahoCorasick {
        private static final int TOKEN_MAX_LEN = 17;
        private static final int TOKEN_PREFIX_LEN = 3;
        private final Trie validTokenPrefixes;

        public TokenFinder_ahoCorasick(Set<String> validTokenPrefixes) {
            this.validTokenPrefixes = Trie.builder().addKeywords(validTokenPrefixes).build();
        }

        public List<Match> findTokens(String in) {
            ArrayList<Match> foundTokens = new ArrayList<>();

            //optimized search for list of known token prefixes
            validTokenPrefixes.parseText(in, foundPrefix -> {
                //so we found a prefix - now check the next 17 chars beginning from match.start if its a valid token
                String token = in.substring(foundPrefix.getStart(), foundPrefix.getStart() + TOKEN_MAX_LEN);
                if (isValidToken(token)) {
                    foundTokens.add(new Match(foundPrefix.getStart(), foundPrefix.getStart() + token.length(), token));
                    return true;
                }
                return false;
            });

            return foundTokens;
        }

        private static boolean isValidToken(String token) {
            if (token.length() != TOKEN_MAX_LEN) {
                throw new IllegalArgumentException("Invalid token candidate - a token must be exactly " + TOKEN_MAX_LEN + " chars");
            }
            //we can commit the prefix check, as finding the prefix is already part of the aho-corasik search!
            return satisfiesTokenCharacterRules(token);
        }

        private static boolean satisfiesTokenCharacterRules(String token) {
            boolean hasLetter = false;
            boolean hasNumber = false;

            //valid tokens must have
            // - at least one letter and
            // - at least one digit and
            // - Must-Not contain any other character then digits and letters a-Z
            for (int i = TOKEN_PREFIX_LEN; i < TOKEN_MAX_LEN; i++) {
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

    public static class TokenFinder_regex {
        private static final int TOKEN_MAX_LEN = 17;
        private static final int TOKEN_PREFIX_LEN = 3;
        private final Set<String> validTokenPrefixes;
        private final Pattern searchPattern;

        public TokenFinder_regex(Set<String> validTokenPrefixes) {
            this.validTokenPrefixes = validTokenPrefixes;
            // ATTENTION
            // this regex is wrong! It does not find overlaps - but for the sake of this example, it is sufficient. (and i am running out of time trying to get this garbage regex to work)
            // A more correct match would only make the regex more complicated, but not simpler.
            // You still need lookaheads and backtracking - which make this regex suck balls
            // - performance wise
            // - and you need ages to figure out what it does (hint: comments may lie. if i haven't stated it is wrong, you would never know!)
            // - ... Oh,and good luck trying to add the >=1000 known prefixes into this regex

            // Why does this/all regexes suck?
            //  - Ratio of learning curve vs debuggability vs performance alone seldom justify using a regex and the overlap with "right-tool-for-the-job" reduces applicable use-cases to almost 0.
            //  - compared to other regex engines, the java engine cannot recognize that this is a "search string task" and just optimize the problem into a string search algo in the background.
            //    There are regex engines that do pull that optimization - (ok, its no longer a regex then... and that's exactly why its fast)
            //  - "BUUUUT my professor said..." - shhhhh, if your regex engine impl would adhere to the formal chomsky hierarchy of a "RegularExpressionLanguage" it would be a DFA, and it would be fast and simple.
            //     But there is no mainstream engine that simple.
            //     Most engines support advanced stuff like backtracking and lookarounds, which is expensive. Supporting lookaround/ahead/behind makes the engine no longer "regular" but a "Context-sensitive grammar" (just one step below turing complete)
            //     In my opinion, such Regexengines just re-invent the wheel. You are mostly using them from a turing complete language like java - but you decide to limit yourself to an un-debuggable un-readable mess of characters (trying) to express your intent as regex, while wasting yours and everybody else's time reading your mess, instead of writing proper code.
            final String regex = "(?=(?:.*+){0,17}\\d)(?=(?:.*+){0,17}[a-zA-Z])(?:[a-zA-Z\\d]*){17}";
            this.searchPattern = Pattern.compile(regex);
        }

        public List<Match> findTokens(String in) {
            ArrayList<Match> foundTokens = new ArrayList<>();

            Matcher matcher = searchPattern.matcher(in); //only finds strings of matching length, hopefully satisfying the "At least one char AND at least one digit" rule
            while (matcher.find()) {
                // Get the matching string
                String match = matcher.group();
                if (isValidToken(match)) {
                    foundTokens.add(new Match(matcher.start(), matcher.end(), match));
                }
            }

            return foundTokens;
        }

        private boolean isValidToken(String token) {
            if (token.length() != TOKEN_MAX_LEN) {
                throw new IllegalArgumentException("Invalid token candidate - a token must be exactly " + TOKEN_MAX_LEN + " chars: " + token);
            }

            String prefix = token.substring(0, TOKEN_PREFIX_LEN);
            if (!validTokenPrefixes.contains(prefix)) {
                //not a token
                return false;
            }

            return satisfiesTokenCharacterRules(token);
        }

        public static boolean satisfiesTokenCharacterRules(String token) {
            boolean hasLetter = false;
            boolean hasNumber = false;

            for (int i = TOKEN_PREFIX_LEN; i < TOKEN_MAX_LEN; i++) {
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


    @Data
    @AllArgsConstructor
    public static class Match {
        int start;
        int end;
        String token;
    }


    private static String generateValidToken(Set<String> prefixes) {
        Random r = ThreadLocalRandom.current();
        String prefix = (String) prefixes.toArray()[r.nextInt(prefixes.size())];//choose a random prefix
        String token;
        do {
            token = prefix + RandomStringUtils.randomAlphanumeric(TOKEN_LENGTH - prefix.length());
            //simple rejection sampling
        } while (!TokenFinder_Simple.satiesfiesTokenRules(token));

        return token;
    }

    private static String randomStringWithToken(int stringLength, String token) {
        Random r = ThreadLocalRandom.current();
        StringBuilder result = randomString(stringLength, r);
        if (StringUtils.isNotEmpty(token)) {
            int replacePos = r.nextInt(stringLength - token.length());
            result.replace(replacePos, replacePos + token.length(), token);
        }
        return result.toString();

    }


    private static StringBuilder randomString(int stringLength, Random r) {
        char[] ALPHABET = "abcdefghijklmnopqrstuvwxyzABCEDFGHIJKLMNOPQRSTUVWXYZ 01234567890!\"§$%&/()=?ÜÖÄ;:_'*,.-#+'".toCharArray();
        StringBuilder withToken = new StringBuilder();
        for (int i = 0; i < stringLength; i++) {
            withToken.append(ALPHABET[r.nextInt(ALPHABET.length)]);
        }
        return withToken;
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

    public static void main(String[] args) throws Exception {

        System.exit(0);
        Set<String> prefixes = randomUniqueStrings(10, PREFIX_LEN, PREFIX_ALPHABET);
        System.out.println("Prefixes: " + prefixes);
        String tokenToFind = generateValidToken(prefixes);
        String logLineWithoutToken = randomStringWithToken(100, "");
        String logLineWithToken = randomStringWithToken(100, tokenToFind);
        System.out.println("tokenToFind:         " + tokenToFind);
        System.out.println("logLineWithoutToken: " + logLineWithoutToken);
        System.out.println("logLineWithToken:    " + logLineWithToken);
        //Verify implementation:
        System.out.println("SimpleIteration:");
        System.out.println("No-token:  " + new TokenFinder_Simple(prefixes).findTokens(logLineWithoutToken));
        System.out.println("one token: " + new TokenFinder_Simple(prefixes).findTokens(logLineWithToken));
        System.out.println("AhoCorasick:");
        System.out.println("No-token:  " + new TokenFinder_ahoCorasick(prefixes).findTokens(logLineWithoutToken));
        System.out.println("one token: " + new TokenFinder_ahoCorasick(prefixes).findTokens(logLineWithToken));
        System.out.println("Regex:");
        System.out.println("No-token:  " + new TokenFinder_regex(prefixes).findTokens(logLineWithoutToken));
        System.out.println("one token: " + new TokenFinder_regex(prefixes).findTokens(logLineWithToken));


        new Runner(new OptionsBuilder()
                .include(SearchAndRedactLogMessagesJMH.class.getName() + ".regex")
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
                .build())
                .run();
    }

}