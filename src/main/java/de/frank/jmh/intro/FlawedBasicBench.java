package de.frank.jmh.intro;

import java.util.concurrent.TimeUnit;

public class FlawedBasicBench {

    interface Algo {
        int doWork(int x);
    }

    public static void main(String[] args) {
        int x = 0;
        Algo[] implementations = new Algo[]{new AlgoImpl1(), new AlgoImpl2(), new AlgoImpl3()};
        for (Algo algo : implementations) {
            x += singleBenchRun3(algo, 200_000_000);
        }
        System.out.println(x);
    }


    private static int singleBenchRun(Algo algo, int invocations) {
        long start = System.nanoTime();
        for (int i = 0; i < invocations; i++) {
            algo.doWork(42);
        }
        long duration = System.nanoTime() - start;
        double nsPerOp = (double) duration / invocations;
        double scale = (double) TimeUnit.SECONDS.toNanos(1) / duration;
        double throughputPerS = invocations * scale;
        System.out.printf("%s %9.3fns/op %.0fops/s%n", algo.getClass().getSimpleName(), nsPerOp, throughputPerS);
        return 0;
    }




    private static int singleBenchRun2 (Algo algo, int invocations) {
        long start = System.nanoTime();
        int x = 0;
        for (int i = 0; i < invocations; i++) {
            x += algo.doWork(42);
        }
        long duration = System.nanoTime() - start;
        double nsPerOp = (double) duration / invocations;
        double scale = (double) TimeUnit.SECONDS.toNanos(1) / duration;
        double throughputPerS = invocations * scale;
        System.out.printf("%s %.3fns/op %.0fops/s%n", algo.getClass().getSimpleName(), nsPerOp, throughputPerS);
        return x;
    }




    public static volatile int input = 27;

    private static int singleBenchRun3(Algo algo, int invocations) {
        long start = System.nanoTime();
        int x = 0;
        for (int i = 0; i < invocations; i++) {
            x += algo.doWork(input);
        }
        long duration = System.nanoTime() - start;
        double nsPerOp = (double) duration / invocations;
        double scale = (double) TimeUnit.SECONDS.toNanos(1) / duration;
        double throughputPerS = invocations * scale;
        System.out.printf("%s %.3fns/op %.0fops/s%n", algo.getClass().getSimpleName(), nsPerOp, throughputPerS);
        return x;
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
