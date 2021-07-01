package de.frank.jmh.algorithms.wordcount.myVariants;


import java.io.*;
import java.nio.channels.Channels;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Counts words (whitespace and newline tokenizer) provided via stdin or provided filename argument and outputs the
 * word frequency matrix
 * <p>
 * Implementation notice: implementation is parallel, has a limit of 2^31-1 occurrences of words und uses a 1MB buffer
 *
 * @author Michael Frank
 */
public class wp_my_winner_finalist_bytes {
    private static final byte NEWLINE = (byte) '\n';
    private static final byte SPACE = (byte) ' ';


    public static void main(String[] args) throws Exception {
        //aggregator map shared over threads - avoids map merge
        long start = System.currentTimeMillis();

        Map<Word, AtomicInteger> wordFreq = new ConcurrentHashMap<>(1 << 16); //65535

        InputStream input = (args.length > 0 ? new FileInputStream(args[0]) : System.in);

        ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        new ChunkedInputStream(input, 1 * 1024 * 1024, SPACE).forEach(chunk ->
                //  tokenizer(chunk, word -> wordFreq.computeIfAbsent(word, w -> new AtomicInteger()).incrementAndGet()));
                pool.submit(() -> tokenizer(chunk, word -> wordFreq.computeIfAbsent(word, w -> new AtomicInteger()).incrementAndGet())));
        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.DAYS);


        printResultBytes(wordFreq, System.out);
        System.out.println(System.currentTimeMillis() - start);

    }


    public static void tokenizer(byte[] input, Consumer<Word> wordConsumer) {
        int tokenStart = 0;
        for (int i = 0; i < input.length; i++) {
            byte bi = input[i];
            if (bi == SPACE || bi == NEWLINE) {

                if (tokenStart < i) {
                    //flush word
                    Word w = new Word(input, tokenStart, i);
                    wordConsumer.accept(w);
                }
                tokenStart = i + 1;
            }
        }
        //last token , end of line
        if (tokenStart < input.length) {
            //flush word
            Word w = new Word(input, tokenStart, input.length);
            wordConsumer.accept(w);
        }
    }


    private static void printResultBytes(Map<Word, AtomicInteger> wordFreq, OutputStream target) {
        BufferedOutputStream out = new BufferedOutputStream(target, 8192 * 4);
        wordFreq.entrySet().stream()
                .sorted((a, b) ->b.getValue().intValue()- a.getValue().intValue())
                .forEach(entry -> writeEntry(out, entry));
        try {
            out.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void writeEntry(OutputStream bos, Map.Entry<Word, ? extends Number> entry) {
        try {
            bos.write(Long.toString(entry.getValue().longValue()).getBytes(StandardCharsets.UTF_8));
            bos.write(SPACE);
            bos.write(entry.getKey().word);
            bos.write(NEWLINE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void testImpl() {
        byte[] in = ("a\n" +
                "b c\n" +
                "dd  ee\n" +
                "fff ggg\n" +
                "hhhh iiii\n\n" +
                "jjjjj kkkk\n" +
                "jjjjj kkkk\n" +
                "jjjjj   kkkk\n" +
                "jjjjj kkkk\n" +
                "jjjjj   kkkk \n" +
                "llllllllllllllll").getBytes(StandardCharsets.UTF_8);

        ChunkedInputStream cis = new ChunkedInputStream(new ByteArrayInputStream(in), 3, (byte) '\n');

        for (byte[] chunk : cis) {
            System.out.println("Chunk: " + new String(chunk, StandardCharsets.UTF_8));
            tokenizer(chunk, w -> System.out.println(" Word: '" + w + "'"));
        }
    }

    public static class Word {
        byte[] word;
        transient int hash;


        public Word(byte[] in, int start, int end) {
            this.word = Arrays.copyOfRange(in, start, end);
            this.hash = Arrays.hashCode(this.word);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Word word1 = (Word) o;
            return Arrays.equals(word, word1.word);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public String toString() {
            return new String(word, StandardCharsets.UTF_8);
        }

    }

    static class ChunkedInputStream implements Iterable<byte[]> {
        private final byte chunkMarker;
        private final InputStream in;
        private byte[] buf;

        private int bufMark;
        private int bufEnd;

        public ChunkedInputStream(InputStream in, int buffSize, byte chunkMarker) {
            this.in = in;
            this.chunkMarker = chunkMarker;
            this.buf = new byte[buffSize];
        }

        public byte[] readChunk() throws IOException {
            while (true) {
                fillBuffer();

                //detect EOF
                if (bufEnd == -1) {
                    if (bufMark > 0) {
                        byte[] r = Arrays.copyOfRange(buf, 0, bufMark);
                        bufMark = 0;
                        return r;
                    }
                    return null;
                }

                //search a chunk Mark from end
                for (int i = bufEnd - 1; i >= 0; i--) {
                    if (buf[i] == chunkMarker) {
                        byte[] r = Arrays.copyOfRange(buf, 0, i);
                        //Copy residual buf content to start of buffer
                        int residualSize = bufEnd - (i + 1);//+1 to omit marker
                        System.arraycopy(buf, i + 1, buf, 0, residualSize);
                        bufMark = residualSize;
                        return r;
                    }
                }

                //no chunk marker found, grow buff and fill
                int newLen = newLength(buf.length, 1, buf.length << 1);
                buf = Arrays.copyOf(buf, newLen);
                bufMark = bufEnd;
            }
        }

        /**
         * The maximum length of array to allocate (unless necessary).
         * Some VMs reserve some header words in an array.
         * Attempts to allocate larger arrays may result in
         * {@code OutOfMemoryError: Requested array size exceeds VM limit}
         */
        public static final int MAX_ARRAY_LENGTH = Integer.MAX_VALUE - 8;

        public static int newLength(int oldLength, int minGrowth, int prefGrowth) {
            // assert oldLength >= 0
            // assert minGrowth > 0

            int newLength = Math.max(minGrowth, prefGrowth) + oldLength;
            if (newLength - MAX_ARRAY_LENGTH <= 0) {
                return newLength;
            }
            return hugeLength(oldLength, minGrowth);
        }

        private static int hugeLength(int oldLength, int minGrowth) {
            int minLength = oldLength + minGrowth;
            if (minLength < 0) { // overflow
                throw new OutOfMemoryError("Required array length too large");
            }
            if (minLength <= MAX_ARRAY_LENGTH) {
                return MAX_ARRAY_LENGTH;
            }
            return Integer.MAX_VALUE;
        }

        private void fillBuffer() throws IOException {
            int offset = bufMark;
            int read;
            do {
                read = in.read(buf, offset, buf.length - offset);
            } while (read == 0);

            if (read > 0) {
                bufEnd = offset + read;
            } else {
                bufEnd = -1;//EOF
            }

        }

        public Stream<byte[]> chunks() {
            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                    iterator(), Spliterator.CONCURRENT | Spliterator.NONNULL), false);
        }

        public Iterator<byte[]> iterator() {
            return new Iterator<byte[]>() {
                byte[] nextChunk = null;

                @Override
                public boolean hasNext() {
                    if (nextChunk != null) {
                        return true;
                    } else {
                        try {
                            nextChunk = readChunk();
                            return (nextChunk != null);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }
                }

                @Override
                public byte[] next() {
                    if (nextChunk != null || hasNext()) {
                        byte[] chunk = nextChunk;
                        nextChunk = null;
                        return chunk;
                    } else {
                        throw new NoSuchElementException();
                    }
                }
            };
        }
    }

}
