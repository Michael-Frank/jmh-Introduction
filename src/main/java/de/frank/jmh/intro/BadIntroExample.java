package de.frank.jmh.intro;


import java.util.Arrays;


/**
 * How NOT to benchmark
 *
 * Initial Example from the slides
 *
 * @author Michael Frank
 * @version 1.0 05.12.2016
 */
public class BadIntroExample {
    public static void main(String[] args) {
        final int[] data = new int[10_000_000];
        Arrays.fill(data, 42);
        for (int i = 0; i < 10; i++) {
            long start = System.currentTimeMillis();
            sum(data);
            long duration = System.currentTimeMillis() - start;
            System.out.println("Took: " + duration + "ms");
        }
    }


    public static int sum(int[] arr) {
        int sum = 0;
        for (int i : arr) {
            sum += i;
        }
        return sum;
    }
}