package de.frank.jmh.intro;


import java.util.Arrays;


/**
 * How NOT to benchmark
 * <p>
 * Initial Example from the slides
 * Just everything about this example is wrong.
 * Jet this and even much simpler benchmark then this are used daily by people to "justify" decisions and execute tests.
 * <p>
 * I bet you have done it too! We all have at some point .. :-(
 *
 * @author Michael Frank
 * @version 1.0 05.12.2016
 */
public class BadIntroExample {

    //keep increasing this till compile kicks in and performs dead code optimizations and folding
    private static final int TEST_ITERATIONS = 150000;
    private static final boolean sampling = false;

    public static void main(String[] args) {
        //timer resolution currentTimeMillis depending on OS and kernel settings! May be as coarse as 33ms!
        long t0 = System.currentTimeMillis();

        //######## setup ##############
        // test data, test data size, and benchmark iterations have a HUGE impact on the results you get!

        // TODO: to show the all the flaws, ->YOU<- have to adjust the data size so, that  each run executes at around ~1.8ms at YOUR machine
        final int[] data = new int[5_000_000];
        Arrays.fill(data, 42);
        long t1 = System.currentTimeMillis();

        //######## the benchmark "harness" ##############
        long execTimes = 0;
        for (int i = 0; i < TEST_ITERATIONS; i++) {

            //######## code to benchmark ##############
            long start = System.currentTimeMillis();
            sum(data);
            long duration = System.currentTimeMillis() - start;

            execTimes += duration;

            if (sampling && showSample(i))
                System.out.printf("%2d: Took: %d ms%n", i, duration);
        }

        //######## print results ##############
        System.out.printf("Average exec time1:    %dms (wrong!)%n", (execTimes / TEST_ITERATIONS));
        System.out.printf("Average exec time2: %.2fms (still wrong!)%n", ((double) execTimes / TEST_ITERATIONS));

        //######## code to show some of the issues - Still all wrong! ##############
        long tx = System.currentTimeMillis();
        System.out.println("A little correction (but the benchmark itself is still totally wrong!):");
        System.out.printf("Total=%dms fill=%dms sum=%dms sumAvg=%.2fms%n", (tx - t0), (t1 - t0), (tx - t1), (double) (tx - t1) / TEST_ITERATIONS);
    }


    public static int sum(int[] arr) {
        int sum = 0;
        for (int i : arr) {
            sum += i;
        }
        return sum;
    }


    /**
     * only show a sample for the first 5 of each 10^x power
     * <pre>
     * e.g.:
     * 0,1,2,3,4,5
     * 10,11,12,13,14
     * 100,101,102,103,104
     * 1001, .., 1004
     * 10000, .., 10004
     * ...
     * </pre>
     *
     * @param i
     * @return
     */
    private static boolean showSample(int i) {
        int log10Pow = (int) Math.pow(10, ((int) Math.log10(i)));
        return i >= log10Pow && i < log10Pow + 5;
    }

}