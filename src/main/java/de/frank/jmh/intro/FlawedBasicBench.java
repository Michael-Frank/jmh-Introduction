package de.frank.jmh.intro;

import java.util.concurrent.TimeUnit;

/**
 * This demonstrates HOW NOT! TO BENCHMARK in multiple steps and the "improvements" one could think of to make it "right" (but will not work)
 *
 * This is from the live-demo part of the Slides
 * Walk through of some issues (optimizations the jvm does - which is good) while benchmarking Java.
 *   * compile threshold c1/c2
 *   * dead-code elimination
 *   * constants
 *   * statics
 *
 * @author Michael Frank
 * @version 1.0 05.12.2016
 */
public class FlawedBasicBench {

    interface Algo {
        int doWork(int x);
    }

    public static void main(String[] args) {
        int x = 0;
        Algo[] implementations = new Algo[]{new AlgoImpl1(), new AlgoImpl2(), new AlgoImpl3()};
        for (Algo algo : implementations) {
            //HOWTO: run each variant at its own (jvm instance)!
            singleBenchRun_v1(algo, 1_000);
//            singleBenchRun_v1(algo, 10_000);
//            singleBenchRun_v1(algo, 100_000);
//            singleBenchRun_v1(algo, 1_000_000);
//            singleBenchRun_v1(algo, 200_000_000);
//            x += singleBenchRun_v2(algo, 200_000_000);
//            x += singleBenchRun_v3(algo, 200_000_000);
//            x += singleBenchRun_v4(algo, 200_000_000);
        }
        System.out.println(x);
    }


    private static void singleBenchRun_v1(Algo algo, int invocations) {
        long start = System.nanoTime();
        for (int i = 0; i < invocations; i++) {
            algo.doWork(42);
        }
        long duration = System.nanoTime() - start;
        double nsPerOp = (double) duration / invocations;
        double scale = (double) TimeUnit.SECONDS.toNanos(1) / duration;
        double throughputPerS = invocations * scale;
        System.out.printf("%s %9.3fns/op %.0fops/s%n", algo.getClass().getSimpleName(), nsPerOp, throughputPerS);
    }

    // use result, prevent deadcode eliminations ... still sucks
    private static int singleBenchRun_v2(Algo algo, int invocations) {
        long start = System.nanoTime();
        int x = 0;
        for (int i = 0; i < invocations; i++) {
            x += algo.doWork(42);
        }
        printBenchmarkReport(algo, invocations, start);
        return x;
    }


    // dont use constant variables ... still sucks (and still constant)
    public static int input3 = 27;
    private static int singleBenchRun_v3(Algo algo, int invocations) {
        long start = System.nanoTime();
        int x = 0;
        for (int i = 0; i < invocations; i++) {
            x += algo.doWork(input3);
        }
        printBenchmarkReport(algo, invocations, start);
        return x;
    }

    // now we got it, eh? ... nope, still sucks (you can continue this for weeks... now switch to JMH already)
    public static volatile int input4 = 27;
    private static int singleBenchRun_v4(Algo algo, int invocations) {
        long start = System.nanoTime();
        int x = 0;
        for (int i = 0; i < invocations; i++) {
            x += algo.doWork(input4);
        }
        printBenchmarkReport(algo, invocations, start);
        return x;
    }


    private static void printBenchmarkReport(Algo algo, int invocations, long start) {
        long duration = System.nanoTime() - start;
        double nsPerOp = (double) duration / invocations;
        double scale = (double) TimeUnit.SECONDS.toNanos(1) / duration;
        double throughputPerS = invocations * scale;
        System.out.printf("%s %.3fns/op %.0fops/s%n", algo.getClass().getSimpleName(), nsPerOp, throughputPerS);
    }











    static class AlgoImpl1 implements Algo {
        @Override
        public int doWork(int x) {
            return x * x;
        }
    }

    static class AlgoImpl2 implements Algo {
        @Override
        public int doWork(int x) {
            return x * x;
        }
    }

    static class AlgoImpl3 implements Algo {
        @Override
        public int doWork(int x) {
            return x * x;
        }
    }
}
