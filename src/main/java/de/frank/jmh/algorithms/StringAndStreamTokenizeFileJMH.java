package de.frank.jmh.algorithms;


import com.yevdo.jwildcard.JWildcard;
import org.apache.commons.io.output.CountingOutputStream;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormat;
import org.openjdk.jmh.results.format.ResultFormatFactory;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/*--

Condensed results:

HOT SINGLE THREAD WINNERS
    customIndexOfTokenizer_stream_filesLines_iso                   avgt    5  157 ±   9  ms/op
  **stringTokenizer_line_Stream_ForEach_defaultFileReader          avgt    5  183 ±  11  ms/op
    stringSplit_stream_FlatMap_FilesLines                          avgt    5  202 ±   6  ms/op
    stringSplit_loops_defaultFileReader                            avgt    5  215 ± 127  ms/op
    scanner_while_defaultFileReader                                avgt    5  230 ±  82  ms/op
    streamTokenizer_AllReader_defaultFileReader                    avgt    5  250 ±   9  ms/op

SINGLE SHOT SINGLE THREADED BEST OF EACH
    customIndexOfTokenizer_stream_filesNewBufReader_ISO              ss   30  367 ±   23  ms/op
  **stringTokenizer_line_Stream_ForEach_defaultFileReader            ss   30  380 ±   37  ms/op
    stringSplit_stream_foreach_defaultFileReader                     ss   30  426 ±   15  ms/op
    scanner_while_defaultFileReader                                  ss   30  431 ±   22  ms/op
    stringSplit_stream_FlatMap_defaultFileReader                     ss   30  434 ±   23  ms/op
    customStreamTokenizer_consumer_defaultFileReader                 ss   30  569 ±  108  ms/op
    stringTokenizer_AllReader_defaultFileReader                      ss   30  645 ±  086  ms/op



DETAILS

SingleShot 30 forks (cold time)
Benchmark                                                          Mode  Cnt  Score   Error  Units
#Parallel ( stream().parallel())
    customIndexOfTokenizer_streamParallel_defaultFileReader          ss   30  0,182 ± 0,015   s/op
    stringTokenizer_line_StreamParallel_FlatMap_defaultFileReader    ss   30  0,240 ± 0,023   s/op #StringTokenizer is pretty good
    stringSplit_streamParallel_FlatMap_defaultFileReader             ss   30  0,336 ± 0,034   s/op
    stringSplit_streamParallel_FlatMap_channel_iso                   ss   30  0,345 ± 0,041   s/op
    customIndexOfTokenizer_streamParallel_filesLines                 ss   30  0,362 ± 0,037   s/op
    customIndexOfTokenizer_streamParallel_filesLines_iso             ss   30  0,373 ± 0,032   s/op
    stringSplit_streamParallel_FlatMap_FilesLines_ISO88591           ss   30  0,464 ± 0,058   s/op #string.split sucks
#Single Thread
    customIndexOfTokenizer_whileLines_defaultFileReader              ss   30  0,308 ± 0,011   s/op
    customIndexOfTokenizer_stream_defaultFileReader                  ss   30  0,324 ± 0,014   s/op
    customIndexOfTokenizer_stream_filesLines                         ss   30  0,345 ± 0,019   s/op
    customIndexOfTokenizer_stream_filesLines_iso                     ss   30  0,355 ± 0,017   s/op
    customIndexOfTokenizer_stream_filesNewBufReader                  ss   30  0,345 ± 0,020   s/op
    customIndexOfTokenizer_stream_filesNewBufReader_ISO              ss   30  0,367 ± 0,023   s/op
    stringTokenizer_line_Stream_ForEach_defaultFileReader            ss   30  0,380 ± 0,037   s/op
    stringSplit_stream_foreach_defaultFileReader                     ss   30  0,426 ± 0,015   s/op
    scanner_while_defaultFileReader                                  ss   30  0,431 ± 0,022   s/op
    stringSplit_stream_FlatMap_defaultFileReader                     ss   30  0,434 ± 0,023   s/op
    stringTokenizer_line_Stream_FlatMap_defaultFileReader            ss   30  0,467 ± 0,033   s/op
    stringSplit_loops_defaultFileReader                              ss   30  0,447 ± 0,023   s/op
    stringSplit_stream_FlatMap_FilesLines_ASCII                      ss   30  0,479 ± 0,034   s/op
    stringSplit_stream_FlatMap_FilesLines_ISO88591                   ss   30  0,479 ± 0,034   s/op
    stringSplit_stream_FlatMap_channel_iso                           ss   30  0,487 ± 0,050   s/op
    stringSplit_stream_FlatMap_FilesLines                            ss   30  0,490 ± 0,032   s/op
    customStreamTokenizer_consumer_defaultFileReader                 ss   30  0,569 ± 0,108   s/op
    stringTokenizer_AllReader_defaultFileReader                      ss   30  0,645 ± 0,086   s/op
    customStreamTokenizer_stream_defaultFileReader                   ss   30  0,722 ± 0,023   s/op

============================================
Hot time results
============================================
Benchmark                                                      Mode  Cnt    Score     Error  Units
#stram.parallel
    customIndexOfTokenizer_streamParallel_defaultFileReader        avgt    5  106,935 ±   4,955  ms/op
    customIndexOfTokenizer_streamParallel_filesLines               avgt    5   92,319 ±   3,588  ms/op
    customIndexOfTokenizer_streamParallel_filesLines_iso           avgt    5   92,781 ±   7,635  ms/op
    stringSplit_streamParallel_FlatMap_FilesLines_ISO88591         avgt    5   77,600 ±   3,503  ms/op
    stringSplit_streamParallel_FlatMap_channel_iso                 avgt    5   68,656 ±   1,523  ms/op
    stringSplit_streamParallel_FlatMap_defaultFileReader           avgt    5   73,862 ±   2,989  ms/op
    stringTokenizer_line_StreamParallel_FlatMap_defaultFileReader  avgt    5   87,491 ±   4,366  ms/op
#single threaded
    customIndexOfTokenizer_stream_defaultFileReader                avgt    5  172,122 ±   8,338  ms/op
    customIndexOfTokenizer_stream_filesLines                       avgt    5  160,586 ±   9,996  ms/op
    customIndexOfTokenizer_stream_filesNewBufReader                avgt    5  159,556 ±   7,527  ms/op
    customIndexOfTokenizer_stream_filesNewBufReader_ISO            avgt    5  159,324 ±   4,275  ms/op
    customIndexOfTokenizer_whileLines_defaultFileReader            avgt    5  186,958 ±  56,381  ms/op
   *customIndexOfTokenizer_stream_filesLines_iso                   avgt    5  157,324 ±   9,744  ms/op
   *scanner_while_defaultFileReader                                avgt    5  230,894 ±  82,213  ms/op
   *streamTokenizer_AllReader_defaultFileReader                    avgt    5  250,012 ±   9,480  ms/op
   *stringSplit_loops_defaultFileReader                            avgt    5  215,688 ± 127,871  ms/op
   *stringSplit_stream_FlatMap_FilesLines                          avgt    5  202,226 ±   6,495  ms/op
    stringSplit_stream_FlatMap_FilesLines_ASCII                    avgt    5  199,370 ±   4,988  ms/op
    stringSplit_stream_FlatMap_FilesLines_ISO88591                 avgt    5  194,254 ±   5,611  ms/op
    stringSplit_stream_FlatMap_channel_iso                         avgt    5  207,237 ±  95,190  ms/op
    stringSplit_stream_FlatMap_defaultFileReader                   avgt    5  203,938 ±  11,836  ms/op
    stringSplit_stream_foreach_defaultFileReader                   avgt    5  190,484 ±   2,583  ms/op
    stringTokenizer_line_Stream_FlatMap_defaultFileReader          avgt    5  231,321 ±   7,075  ms/op
 ***stringTokenizer_line_Stream_ForEach_defaultFileReader          avgt    5  183,195 ±  11,788  ms/op
    customStreamTokenizer_consumer_defaultFileReader               avgt    5  209,359 ±  40,431  ms/op
    customStreamTokenizer_stream_defaultFileReader                 avgt    5  582,099 ±  19,962  ms/op


 */
@State(Scope.Thread)
public class StringAndStreamTokenizeFileJMH {

    @Param("file")
    public String file;


    public static void main(String[] args) throws Exception {
        File f = generateRandomTestFile(160 * 1024 * 1024);

        Map<String, Collection<RunResult>> results = new HashMap<>();
        String include = JWildcard.wildcardToRegex(StringAndStreamTokenizeFileJMH.class.getName() + ".*");
        Options singleShotOpts = new OptionsBuilder()
                .include(include)
                .result(String.format("%s_%s.json",
                        DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                        StringAndStreamTokenizeFileJMH.class.getSimpleName()))
                .mode(Mode.SingleShotTime)
                .timeUnit(TimeUnit.SECONDS)
                .warmupTime(TimeValue.seconds(1))
                .warmupIterations(1)
                .measurementTime(TimeValue.seconds(1))
                .measurementIterations(1)
                .forks(30)//measure cold time only in own vm instances (like when invoked as command line program)
                .param("file", f.getAbsolutePath())
                .build();
        results.put("SingleShot time results", new Runner(singleShotOpts).run());

        Options hotCodeOpts = new OptionsBuilder()
                .include(include)
                .result(String.format("%s_%s.json",
                        DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                        StringAndStreamTokenizeFileJMH.class.getSimpleName()))
                .mode(Mode.AverageTime)
                .timeUnit(TimeUnit.MILLISECONDS)
                .warmupTime(TimeValue.seconds(1))
                .warmupIterations(5)
                .measurementTime(TimeValue.seconds(1))
                .measurementIterations(5)
                .forks(1)//measure cold time only in own vm instances (like when invoked as command line program)
                .param("file", f.getAbsolutePath())
                .build();
        results.put("Hot time results", new Runner(hotCodeOpts).run());


        ResultFormat textSoutFormater = ResultFormatFactory.getInstance(ResultFormatType.TEXT, System.out);
        for (Map.Entry<String, Collection<RunResult>> res : results.entrySet()) {
            System.out.println("");
            System.out.println("============================================");
            System.out.println(res.getKey());
            System.out.println("============================================");
            textSoutFormater.writeOut(res.getValue());
        }
    }

    public static File generateRandomTestFile(int fileSize) throws IOException {
        Instant start = Instant.now();
        File f = File.createTempFile("StringAndStreamsWordList", ".txt");
        f.deleteOnExit();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        try (CountingOutputStream count = new CountingOutputStream(new FileOutputStream(f));
             BufferedWriter w = new BufferedWriter(new OutputStreamWriter(count));) {

            do {//VERY CRUDE random text file generator, generating 2-10 words per line till target file size is reached
                //gen random sentence/line of words
                int words = r.nextInt(2, 10);
                for (int i = 0; i < words; i++) {
                    //gen rand word
                    int wordLen = r.nextInt(2, 20);
                    for (int j = 0; j < wordLen; j++) {
                        w.write(r.nextInt('A', 'z'));
                    }
                    if (r.nextDouble() < 0.2) {//after ever 5th word a ','
                        w.write(',');
                    }
                    w.write(' ');
                }
                w.write('\n');

            } while (count.getByteCount() < fileSize);
            System.out.println(Duration.between(start, Instant.now()) + " created tmp file " + count.getByteCount() + " " + f);
        }
        return f;
    }


    @Benchmark
    public void streamTokenizer_AllReader_defaultFileReader(Blackhole bh) throws IOException {

        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            StreamTokenizer st = new StreamTokenizer(in);
            st.resetSyntax();
            st.wordChars(33, 126);
            st.whitespaceChars(1, 32);
            while (st.nextToken() != StreamTokenizer.TT_EOF) {
                if (st.ttype == StreamTokenizer.TT_WORD) {
                    final String token = st.sval;
                    bh.consume(token);
                }
            }
        }
    }


    @Benchmark
    public void stringTokenizer_line_Stream_FlatMap_defaultFileReader(Blackhole bh) throws IOException {
        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            in.lines()
                    .flatMap(line -> Collections.list(new StringTokenizer(line, " ")).stream())
                    .map(o -> (String) o)
                    .forEach(bh::consume);
        }
    }

    @Benchmark
    public void stringTokenizer_line_StreamParallel_FlatMap_defaultFileReader(Blackhole bh) throws IOException {
        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            in.lines().parallel()
                    .flatMap(line -> Collections.list(new StringTokenizer(line, " ")).stream())
                    .map(o -> (String) o)
                    .forEach(bh::consume);
        }
    }

    @Benchmark
    public void stringTokenizer_line_Stream_ForEach_defaultFileReader(Blackhole bh) throws IOException {
        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            in.lines()
                    .map(line -> new StringTokenizer(line, " "))
                    .forEach(st -> {
                        while (st.hasMoreTokens()) {
                            bh.consume(st.nextToken());
                        }
                    });
        }
    }


    @Benchmark
    public void scanner_while_defaultFileReader(Blackhole bh) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(file));
        try (Scanner s = new Scanner(in).useDelimiter("\\r?\\n\\W+")) {
            while (s.hasNext()) {
                String token = s.next();
                bh.consume(token);
            }
        }

    }

    @Benchmark
    public void stringSplit_loops_defaultFileReader(Blackhole bh) throws IOException {
        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = in.readLine()) != null) {
                for (String word : line.trim().split(" ")) {
                    bh.consume(word);
                }
            }
        }
    }

    @Benchmark
    public void stringSplit_stream_FlatMap_defaultFileReader(Blackhole bh) throws IOException {
        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            in.lines()
                    .flatMap(line -> Arrays.stream(line.trim().split(" ")))
                    .forEach(bh::consume);
        }
    }

    @Benchmark
    public void stringSplit_streamParallel_FlatMap_defaultFileReader(Blackhole bh) throws IOException {
        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            in.lines().parallel()
                    .flatMap(line -> Arrays.stream(line.trim().split(" ")))
                    .forEach(bh::consume);
        }
    }

    @Benchmark
    public void stringSplit_stream_foreach_defaultFileReader(Blackhole bh) throws IOException {
        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            in.lines()
                    .map(line -> line.trim().split(" "))
                    .forEach(words -> {
                        for (String word : words) {
                            bh.consume(word);
                        }
                    });
        }
    }

    @Benchmark
    public void stringSplit_stream_FlatMap_FilesLines(Blackhole bh) throws IOException {
        Files.lines(Paths.get(file))
                .flatMap(line -> Arrays.stream(line.trim().split(" ")))
                .forEach(bh::consume);

    }

    @Benchmark
    public void stringSplit_stream_FlatMap_channel_iso(Blackhole bh) throws IOException {
        new BufferedReader(Channels.newReader(new RandomAccessFile(file, "r").getChannel(),
                StandardCharsets.ISO_8859_1.newDecoder(), 1048576), 262144).
                lines()
                .flatMap(line -> Arrays.stream(line.trim().split(" ")))
                .forEach(bh::consume);

    }

    @Benchmark
    public void stringSplit_streamParallel_FlatMap_channel_iso(Blackhole bh) throws IOException {
        new BufferedReader(Channels.newReader(new RandomAccessFile(file, "r").getChannel(),
                StandardCharsets.ISO_8859_1.newDecoder(), 1048576), 262144).
                lines().parallel()
                .flatMap(line -> Arrays.stream(line.trim().split(" ")))
                .forEach(bh::consume);

    }

    @Benchmark
    public void stringSplit_stream_FlatMap_FilesLines_ASCII(Blackhole bh) throws IOException {
        Files.lines(Paths.get(file), StandardCharsets.US_ASCII)
                .flatMap(line -> Arrays.stream(line.trim().split(" ")))
                .forEach(bh::consume);

    }

    @Benchmark
    public void stringSplit_stream_FlatMap_FilesLines_ISO88591(Blackhole bh) throws IOException {
        Files.lines(Paths.get(file), StandardCharsets.ISO_8859_1)
                .flatMap(line -> Arrays.stream(line.trim().split(" ")))
                .forEach(bh::consume);

    }

    @Benchmark
    public void stringSplit_streamParallel_FlatMap_FilesLines_ISO88591(Blackhole bh) throws IOException {
        Files.lines(Paths.get(file), StandardCharsets.ISO_8859_1)
                .parallel()
                .flatMap(line -> Arrays.stream(line.trim().split(" ")))
                .forEach(bh::consume);

    }


    @Benchmark
    public void customIndexOfTokenizer_whileLines_defaultFileReader(Blackhole bh) throws IOException {
        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            IndexOfWhitespaceTokenizer.tokenize(in, bh::consume);
        }
    }


    @Benchmark
    public void customIndexOfTokenizer_stream_defaultFileReader(Blackhole bh) throws IOException {
        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            in.lines().forEach(line -> IndexOfWhitespaceTokenizer.tokenize(line, bh::consume));
        }
    }

    @Benchmark
    public void customIndexOfTokenizer_streamParallel_defaultFileReader(Blackhole bh) throws IOException {
        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            in.lines()
                    .parallel()
                    .forEach(line -> IndexOfWhitespaceTokenizer.tokenize(line, bh::consume));
        }
    }

    @Benchmark
    public void customIndexOfTokenizer_streamParallel_filesLines(Blackhole bh) throws IOException {
        Files.lines(Paths.get(file))
                .parallel()
                .forEach(line -> IndexOfWhitespaceTokenizer.tokenize(line, bh::consume));

    }

    @Benchmark
    public void customIndexOfTokenizer_streamParallel_filesLines_iso(Blackhole bh) throws IOException {
        Files.lines(Paths.get(file), StandardCharsets.ISO_8859_1)
                .parallel()
                .forEach(line -> IndexOfWhitespaceTokenizer.tokenize(line, bh::consume));

    }

    @Benchmark
    public void customIndexOfTokenizer_stream_filesLines(Blackhole bh) throws IOException {
        Files.lines(Paths.get(file))
                .forEach(line -> IndexOfWhitespaceTokenizer.tokenize(line, bh::consume));

    }

    @Benchmark
    public void customIndexOfTokenizer_stream_filesLines_iso(Blackhole bh) throws IOException {
        Files.lines(Paths.get(file), StandardCharsets.ISO_8859_1)
                .forEach(line -> IndexOfWhitespaceTokenizer.tokenize(line, bh::consume));

    }

    @Benchmark
    public void customIndexOfTokenizer_stream_filesNewBufReader(Blackhole bh) throws IOException {
        Files.newBufferedReader(Paths.get(file))
                .lines()
                .forEach(line -> IndexOfWhitespaceTokenizer.tokenize(line, bh::consume));
    }

    @Benchmark
    public void customIndexOfTokenizer_stream_filesNewBufReader_ISO(Blackhole bh) throws IOException {
        Files.newBufferedReader(Paths.get(file), StandardCharsets.ISO_8859_1)
                .lines()
                .forEach(line -> IndexOfWhitespaceTokenizer.tokenize(line, bh::consume));
    }


    @Benchmark
    public void customStreamTokenizer_consumer_defaultFileReader(Blackhole bh) throws IOException {
        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            CustomWhitespaceStreamTokenizer.tokenize(in, bh::consume);
        }
    }

    @Benchmark
    public void customStreamTokenizer_stream_defaultFileReader(Blackhole bh) throws IOException {
        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            CustomWhitespaceStreamTokenizer.asStream(in).forEach(bh::consume);
        }
    }


    private static class CustomWhitespaceStreamTokenizer implements Iterator<String> {

        private String nextWord;
        private final Reader in;

        public CustomWhitespaceStreamTokenizer(Reader in) {
            this.in = in;
        }

        public static void tokenize(Reader in, Consumer<String> consumer) throws IOException {
            int c;
            int wordLen = 0;
            char[] wordBuffer = new char[64];
            while ((c = in.read()) != -1) {
                //if we find a stop word, skip it and flush so far accumulated word chars
                if ((c == ' ') || (c == '\n') || (c == '\r')) {
                    if (wordLen > 0) {
                        //flush word;
                        String word = new String(wordBuffer, 0, wordLen);
                        wordLen = 0;
                        consumer.accept(word);
                    }
                } else {
                    //grow buffer if required
                    if (wordLen >= wordBuffer.length) {
                        wordBuffer = Arrays.copyOfRange(wordBuffer, 0, wordBuffer.length * 2);
                    }
                    wordBuffer[wordLen++] = (char) c;
                }
            }
        }

        public boolean hasNext() {
            if (nextWord != null) {
                return true;
            } else {
                try {
                    nextWord = readWord();
                    return (nextWord != null);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }

        public String next() {
            if (nextWord != null || hasNext()) {
                String line = nextWord;
                nextWord = null;
                return line;
            } else {
                throw new NoSuchElementException();
            }
        }

        int wordLen = 0;
        char[] wordBuffer = new char[64];

        private String readWord() throws IOException {
            while (true) {
                int c = in.read();
                //if we find a stop word, skip it and flush word buffer
                // if we reach EOF, flush wordbuffer

                // and flush so far accumulated word chars
                if ((c == ' ') || (c == '\n') || (c == '\r') || (c == -1)) {
                    if (wordLen > 0) {
                        //flush word;
                        String word = new String(wordBuffer, 0, wordLen);
                        wordLen = 0;
                        return word;
                    } else if (c == -1) {
                        //not a word but still EOF
                        return null;
                    }
                } else {
                    //grow buffer if required
                    if (wordLen >= wordBuffer.length) {
                        wordBuffer = Arrays.copyOfRange(wordBuffer, 0, wordBuffer.length * 2);
                    }
                    wordBuffer[wordLen++] = (char) c;
                }
            }
        }

        public static Stream<String> asStream(Reader in) {
            return new CustomWhitespaceStreamTokenizer(in).stream();
        }

        public Stream<String> stream() {
            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(this,
                    Spliterator.ORDERED | Spliterator.NONNULL), true);
        }

    }

    private static class IndexOfWhitespaceTokenizer {

        public static void tokenize(BufferedReader in, Consumer<String> consumer) throws IOException {
            String line;
            while ((line = in.readLine()) != null) {
                tokenize(line, consumer);
            }
        }

        public static void tokenize(String line, Consumer<String> consumer) {
            int pos = 0;
            int idx;
            while ((idx = line.indexOf(' ', pos)) >= 0) {
                consumeToken(consumer, line, pos, idx);
                pos = idx + 1;
            }
            //last token , end of line
            consumeToken(consumer, line, pos, line.length());
        }

        private static void consumeToken(Consumer<String> wordConsumer, String line, int pos, int idx) {
            if (pos != idx) {
                String token = line.substring(pos, idx);
                wordConsumer.accept(token);
            }
        }
    }
}
