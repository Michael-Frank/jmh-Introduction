package de.frank.jmh.algorithms.wordcount.myVariants;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class wpck11 {


    public static void main(final String[] args) throws Exception {

        ConcurrentMap<String, Long> frequencyMap = (args.length > 0 ? Files.lines(Paths.get(args[0])) : new BufferedReader(new InputStreamReader(System.in)).lines())
                .parallel()
                .flatMap(line -> Arrays.stream(line.trim().split(" ")))
                .collect(Collectors.groupingByConcurrent(Function.identity(), Collectors.counting()));


        final StringBuilder sb = new StringBuilder();
        frequencyMap.entrySet().stream()
                .sorted((a, b) -> (int)(b.getValue() - a.getValue()))
                .forEach(e -> sb.append(e.getValue()).append(' ').append(e.getKey()).append('\n'));
        System.out.println(sb.toString());

    }
}
