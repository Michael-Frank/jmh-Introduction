package de.frank.jmh.algorithms.wordcount.myVariants;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class wp_my_winner {

    public static void main(String[] args) throws Exception {
        Map<String, AtomicInteger> wordFreq = new ConcurrentHashMap<>(1 << 16); //65535
        ReadableByteChannel source = args.length > 0 ? new FileInputStream(args[0]).getChannel() : Channels.newChannel(System.in);
        BufferedReader input = new BufferedReader(Channels.newReader(source, StandardCharsets.ISO_8859_1.newDecoder(), 1048576), 262144);

        ExecutorService pool = Executors.newFixedThreadPool(8);
        Future<?> async = pool.submit(() ->
                input.lines()
                        .parallel()
                        .forEach(line -> {
                                    StringTokenizer st = new StringTokenizer(line, " ");
                                    while (st.hasMoreTokens()) {
                                        wordFreq.computeIfAbsent(st.nextToken(), w -> new AtomicInteger()).incrementAndGet();
                                    }
                                }
                        )
        );
        pool.shutdown();
        async.get();//blocking await result
        printResult(wordFreq);
    }

    private static void printResult(Map<String, AtomicInteger> wordFreq) {
        StringBuilder res = wordFreq.entrySet().stream()
                .sorted((a, b) -> b.getValue().get() - a.getValue().get())
                .collect(StringBuilder::new,
                        (sb, entry) -> sb.append(entry.getValue().get()).append(' ').append(entry.getKey()).append('\n'),
                        StringBuilder::append);
        System.out.println(res);
    }
}
