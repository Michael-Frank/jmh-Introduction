package de.frank.jmh.algorithms.wordcount.myVariants;

import java.io.*;
import java.util.Arrays;
import java.util.Map;

import static java.util.Comparator.reverseOrder;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

public class wp_my_stream {
    public static void main(String[] args) throws FileNotFoundException {
        Reader in = args.length > 0 ? new FileReader(args[0]) : new InputStreamReader(System.in);

        new BufferedReader(in).lines().
                flatMap(line -> Arrays.stream(line.trim().split(" ")))//attention! if you modify the split() to e.g. split("\\s")
                .filter(word -> word.length() > 0)
                .collect(groupingBy(identity(), counting()))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByValue(reverseOrder()))
                .forEach(e -> System.out.println(e.getValue() + " " + e.getKey()));

    }
}
