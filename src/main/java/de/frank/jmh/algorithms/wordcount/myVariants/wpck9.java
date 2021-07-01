package de.frank.jmh.algorithms.wordcount.myVariants;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class wpck9 {


    public static void main(final String[] args) throws Exception {

        final ConcurrentHashMap<String, AtomicInteger> map = new ConcurrentHashMap<>(1048576);

        (args.length > 0 ? Files.lines(Paths.get(args[0])) : new BufferedReader(new InputStreamReader(System.in)).lines())
                .parallel()
                .map(line -> Arrays.asList(line.trim().split(" ")))
                .forEach(words->words.forEach(
                        word -> map.computeIfAbsent(word, k -> new AtomicInteger()).incrementAndGet()));


        final StringBuilder sb = new StringBuilder();
        map.entrySet().stream()
                .sorted((a, b) -> b.getValue().get() - a.getValue().get())
                .forEach(e -> sb.append(e.getValue().get()).append(' ').append(e.getKey()).append('\n'));
        System.out.println(sb.toString());

        // System.err.println("time: " + (System.currentTimeMillis() - timestamp));
    }
}
