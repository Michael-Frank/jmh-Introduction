package de.frank.jmh.algorithms;

import org.apache.commons.lang3.RandomStringUtils;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*--
Statemachine: easy to write, easy to maintain and extend,fast and constant linear performance for arbitrary large texts.

Regex, as always, fails at rather trivial tasks.
Hard to write correctly, hard to read and debug, horrible runtime performance (even worse at "no-match"))
and catastrophic failures at rather small inputs.

- removeCommentsFixedStateMachine: whole parser code is in approx 40 lines
- removeCommentsExtensibleStateMachine: state machine logic is encapsulated, and custom "removers" can be easily written and added without manipulating the existing state machine. However, it is slightly slower than the fixed state machine.
- regex: WHY!!!!! even the "good-ones" suck.... In less time i spent googling/debugging the regex, i have implemented the state machine.

Benchmark                                                         (commentRate)  (numLines)  Mode  Cnt      Score       Error Units
removeCommentsExtensibleStateMachine            0.0         100  avgt   18     42.435 ±     3.367 ns/op
removeCommentsExtensibleStateMachine            0.0        1000  avgt   18    329.324 ±    21.762 ns/op
removeCommentsExtensibleStateMachine            0.0       10000  avgt   18  4.209.228 ± 1.110.051 ns/op
removeCommentsExtensibleStateMachine            0.0      100000  avgt   18 41.729.614 ± 3.661.110 ns/op
removeCommentsExtensibleStateMachine            0.1         100  avgt   18     56.519 ±    10.517 ns/op
removeCommentsExtensibleStateMachine            0.1        1000  avgt   18    553.379 ±    77.029 ns/op
removeCommentsExtensibleStateMachine            0.1       10000  avgt   18  4.785.951 ±    86.831 ns/op
removeCommentsExtensibleStateMachine            0.1      100000  avgt   18 47.327.133 ± 1.333.205 ns/op

removeCommentsFixedStateMachine                 0.0         100  avgt   18     30.238 ±     7.085 ns/op
removeCommentsFixedStateMachine                 0.0        1000  avgt   18    320.616 ±    67.027 ns/op
removeCommentsFixedStateMachine                 0.0       10000  avgt   18  3.143.853 ±  .196.212 ns/op
removeCommentsFixedStateMachine                 0.0      100000  avgt   18 38.678.668 ± 5.064.362 ns/op
removeCommentsFixedStateMachine                 0.1         100  avgt   18     40.454 ±     8.001 ns/op
removeCommentsFixedStateMachine                 0.1        1000  avgt   18    419.827 ±   169.381 ns/op
removeCommentsFixedStateMachine                 0.1       10000  avgt   18  3.493.247 ±    98.450 ns/op
removeCommentsFixedStateMachine                 0.1      100000  avgt   18 38.263.863 ± 5.400.565 ns/op

removeCommentsRegex                             0.0         100  avgt   12    105.699 ±     6.472 ns/op
removeCommentsRegex                             0.1         100  avgt   18    124.663 ±     8.344 ns/op
removeCommentsRegex                             0.1        1000  avgt   18  1.122.075 ±    94.503 ns/op
!!! removeCommentsRegex   fails with stackoverflow for comment rate 0.0 and more than 100 lines and for comment rate > 0.1 and more than 1000 lines!

java.lang.StackOverflowError
    at java.base/java.lang.Character.codePointAt(Character.java:8910)
    at java.base/java.util.regex.Pattern$CharProperty.match(Pattern.java:3927)
    at java.base/java.util.regex.Pattern$Branch.match(Pattern.java:4734)
    at java.base/java.util.regex.Pattern$GroupHead.match(Pattern.java:4789)
    at java.base/java.util.regex.Pattern$Loop.match(Pattern.java:4898)
    at java.base/java.util.regex.Pattern$GroupTail.match(Pattern.java:4820)
    at java.base/java.util.regex.Pattern$BranchConn.match(Pattern.java:4698)
    at java.base/java.util.regex.Pattern$CharProperty.match(Pattern.java:3931)
    at java.base/java.util.regex.Pattern$Branch.match(Pattern.java:4734)
    at java.base/java.util.regex.Pattern$GroupHead.match(Pattern.java:4789)
    at java.base/java.util.regex.Pattern$Loop.match(Pattern.java:4898)

 */
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 6, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 3
//        ,jvmArgsAppend = {"-XX:+UseParallelGC", "-Xms1g", "-Xmx1g"}
)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class RemoveCommentsFromStringJMH {

    public static void main(String[] args) throws RunnerException {

        InputString i = new InputString();
        i.numLines = 100;
        i.commentRate = 0.1;
        i.generateString();
        System.out.println(i.lines);

        Options opt = new OptionsBuilder()//
                .include(RemoveCommentsFromStringJMH.class.getName() + ".*")//
                // .result(String.format("%s_%s.json",
                //         DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                //         FindNthLineInString.class.getSimpleName()))
                //   .addProfiler(GCProfiler.class)//
                .build();
        new Runner(opt).run();
        System.out.println(Instant.now());
    }


    @org.openjdk.jmh.annotations.State(Scope.Thread)
    public static class InputString {

        @Param({
                "100",
                "1000",
                "10000",
                "100000"
        })
        public int numLines = 10;

        @Param({
                "0.0",
                "0.1"
        })
        public double commentRate = 1.0;


        private String lines;

        @Setup
        public void generateString() {
            ThreadLocalRandom r = ThreadLocalRandom.current();

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < numLines; i++) {
                String line = RandomStringUtils.random(r.nextInt(30, 80/*lineLenght*/), "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijkklmnopqrstuvwxyz,;.:-_1234567890ß!§$%&/()=?*'')\"   ");
                sb.append(line).append("\n");
            }

            //inject comments by randomly comment out content
            int comments = (int) Math.ceil(numLines * commentRate);
            //insert line comment
            for (int i = 0; i < comments; i++) {
                int insertPos = r.nextInt(sb.length() - 1);
                sb.replace(insertPos, insertPos + 1, "//");
            }
            //insert block comment
            for (int i = 0; i < comments; i++) {
                int insertPos = r.nextInt(sb.length() - 100);
                int distance = r.nextInt(4, 100);

                sb.replace(insertPos, insertPos + 1, "/*");
                sb.replace(insertPos + distance, insertPos + distance, "*/");
            }
            this.lines = sb.toString();
        }

        public String getLines() {
            return lines;
        }
    }


    @Benchmark
    public String removeCommentsFixedStateMachine(InputString s) {
        return removeCommentsFixedStateMachine(s.getLines(), true, true, false);
    }

    @Benchmark
    public String removeCommentsExtensibleStateMachine(InputString s) {
        return TextCommentFilter.LINE_AND_BLOCK_COMMENTS.removeComments(s.getLines());
    }

    @Benchmark
    public String removeCommentsRegex(InputString s) {
        return removeCommentsRegex(s.getLines());
    }


    /**
     * Statemachine based comment remove, which is fast enough for our purpose
     *
     * @param inputText
     * @return text without comments like:
     */
    private static final Pattern COMMENT_REGEX_MULTI_AND_SINGLE_LINE_PATTERN = Pattern.compile("(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)");

    public static String removeCommentsRegex(String inputText) {
        if (inputText == null || inputText.isEmpty()) {
            return inputText;
        }
        Matcher matcher = COMMENT_REGEX_MULTI_AND_SINGLE_LINE_PATTERN.matcher(inputText);
        return matcher.replaceAll("");
    }


    enum State {
        outsideComment,
        insideLineComment,
        insideBlockComment,
        insideIbmICLComment, insideBlockComment_noNewLineYet // we want to have at least one new line in the result if the block is not inline.
    }


    public static String removeCommentsFixedStateMachine(String inputText, boolean stripLineComments, boolean stripBlockComments,
                                                         boolean stripIbmICLComment) {

        if (inputText == null || inputText.isEmpty()) {
            return inputText;
        }

        if (stripIbmICLComment) {
            stripBlockComments = false; //IbmCL BlockComments are a special case
        }
        StringBuilder endResult = new StringBuilder();

        TextCommentFilter.CharIterator iter = new TextCommentFilter.CharIterator(inputText);
        State currentState = State.outsideComment;
        while (iter.hasNext()) {

            char curChar = iter.next();
            switch (currentState) {
                case outsideComment:

                    if (stripIbmICLComment && curChar == '/' && iter.peekNext(0) == '*' //find: '/*
                        // IBM ICL block comments must be followed by '*' or a space to be valid!: e.g.:'/**' or '/* '
                        // '/*FOO' is NOT a valid comment!
                        && (iter.peekNext(1) == '*' || iter.peekNext(1) == ' ')) {
                        iter.next();//skip the peeked value
                        currentState = State.insideIbmICLComment;
                    } else if (stripBlockComments && curChar == '/' && iter.peekNext() == '*') { //find: /*/
                        iter.next();//skip the peeked value
                        currentState = State.insideBlockComment;
                    } else if (stripLineComments && curChar == '/' && iter.peekNext() == '/') { //find: //
                        iter.next();//skip the peeked value
                        currentState = State.insideLineComment;
                    } else {
                        endResult.append(curChar);
                    }
                    break;
                case insideLineComment:
                    //Stay in this state till the end of line - line comments do not span multiple lines
                    if (curChar == '\n') {
                        currentState = State.outsideComment;
                        //preserve line breaks - a line comment could be at the end of line like: "foo //somComment \n"
                        //however this produces blank lines, if the whole line is a line comment "//someComment\n"
                        //don't want that: we would need another state/marker to check if the entire line is a comment.
                        endResult.append("\n");
                    }
                    break;
                case insideBlockComment, insideIbmICLComment:
                    //stay in this state till we find a '*/'
                    if (curChar == '*' && iter.peekNext() == '/') {
                        iter.next(); //skip the peeked value
                        currentState = State.outsideComment;
                        break;
                    }
                    break;
                default:
                    throw new IllegalStateException("Unexpected state: " + currentState);
            }
        }

        return endResult.toString();
    }

    public static class TextCommentFilter {

        public static final TextCommentFilter LINE_AND_BLOCK_COMMENTS = new TextCommentFilter(LineCommentDetector.INSTANCE, BlockCommentDetector.INSTANCE);
        public static final TextCommentFilter IBM_ICL = new TextCommentFilter(LineCommentDetector.INSTANCE, IbmICLBlockCommentDetector.INSTANCE);

        private final CommentDetector[] detectors;

        public TextCommentFilter(List<CommentDetector> detectors) {
            this(detectors.toArray(CommentDetector[]::new));
        }

        private TextCommentFilter(CommentDetector... detectors) {
            this.detectors = detectors;
        }

        public static TextCommentFilter newDetector(boolean stripLineComments, boolean stripBlockComments, boolean stripIbmICLComment) {

            List<CommentDetector> detectors = new ArrayList<>();
            if (stripLineComments) {
                detectors.add(new LineCommentDetector());
            }
            if (stripIbmICLComment) {
                stripBlockComments = false; //IbmCL BlockComments are a special case
                detectors.add(new IbmICLBlockCommentDetector());
            }
            if (stripBlockComments) {
                detectors.add(new BlockCommentDetector());
            }
            return new TextCommentFilter(detectors);

        }


        public String removeComments(String inputText) {
            if (inputText == null || inputText.isEmpty()) {
                return inputText;
            }

            StringBuilder endResult = new StringBuilder();

            CommentDetector insideCommentState = null; //start state
            CharIterator iter = new CharIterator(inputText);
            while (iter.hasNext()) {
                char curChar = iter.next();
                if (insideCommentState == null) {
                    for (CommentDetector candidate : detectors) {
                        if (candidate.isCommentStart(curChar, iter)) {
                            insideCommentState = candidate;
                            break;
                        }
                    }
                    if (insideCommentState == null) { //current pos in text is not a startComment marker
                        //not in comment
                        endResult.append(curChar);
                    }
                } else {
                    //inside a comment
                    if (!insideCommentState.stillInsideComment(curChar, iter)) {
                        insideCommentState = null; //no longer inside comment
                    }
                }

            }
            return endResult.toString();
        }

        public interface CommentDetector {

            boolean isCommentStart(char current, Inspecting inspectNext);

            boolean stillInsideComment(char current, Inspecting inspectNext);

        }

        public static class LineCommentDetector implements CommentDetector {
            public static final LineCommentDetector INSTANCE = new LineCommentDetector();

            public boolean isCommentStart(char current, Inspecting inspectNext) {
                if (current == '/' && inspectNext.peekNext() == '/') { //find: //
                    inspectNext.skip(1);//skip the peeked value
                    return true;
                }
                return false;
            }

            public boolean stillInsideComment(char current, Inspecting inspectNext) {
                // /n is the "end of comment marker" but we want to still include it - so we need to look ahead
                return inspectNext.peekNext() != '\n';
            }

        }

        public static class BlockCommentDetector implements CommentDetector {
            public static final BlockCommentDetector INSTANCE = new BlockCommentDetector();

            public boolean isCommentStart(char current, Inspecting inspectNext) {
                if (current == '/' && inspectNext.peekNext() == '*') { //find: /*/
                    inspectNext.skip(1);//skip the peeked value
                    return true;
                }
                return false;
            }

            public boolean stillInsideComment(char current, Inspecting inspectNext) {
                if (current == '*' && inspectNext.hasNext() && inspectNext.peekNext() == '/') {
                    inspectNext.skip(1); //skip the peeked value
                    return false;
                }
                return true;
            }

        }

        public static class IbmICLBlockCommentDetector implements CommentDetector {
            public static final IbmICLBlockCommentDetector INSTANCE = new IbmICLBlockCommentDetector();

            public boolean isCommentStart(char current, Inspecting inspectNext) {
                if (current == '/' && inspectNext.peekNext(0) == '*' //find: '/*
                    // IBM ICL block comments must be followed by '*' or a space to be valid!: e.g.:'/**' or '/* '
                    // '/*FOO' is NOT a valid comment!
                    && (inspectNext.peekNext(1) == '*' || inspectNext.peekNext(1) == ' ')) {
                    inspectNext.skip(1);//skip the peeked value
                    return true;
                }
                return false;
            }

            public boolean stillInsideComment(char current, Inspecting inspectNext) {
                if (current == '*' && inspectNext.hasNext() && inspectNext.peekNext() == '/') {
                    inspectNext.skip(1); //skip the peeked value
                    return false;
                }
                return true;
            }

        }

        public interface Inspecting {
            void skip(int i);

            boolean hasNext();

            boolean hasNext(int i);

            int peekNext();

            int peekNext(int i);
        }

        public static class CharIterator implements Inspecting {

            private final String str;
            private int pos = 0;

            public CharIterator(String str) {
                this.str = str;
            }

            @Override
            public void skip(int i) {
                pos = pos + i;
            }

            public boolean hasNext() {
                return pos < str.length();
            }

            @Override
            public boolean hasNext(int i) {
                int idx = pos + i;
                return idx >= 0 && idx < str.length();
            }

            public char next() {
                return str.charAt(pos++);
            }

            public int peekNext() {
                return peekNext(0);
            }

            /**
             * @param i
             * @return -1 of out of bounds or the char at curPos+i
             */
            public int peekNext(int i) {
                if (!hasNext(i)) {
                    return -1;
                }
                return str.charAt(pos + i);
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }

        }
    }


}
