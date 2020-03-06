package de.frank.jmh.basic;

import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

public class ExceptionSafePoint {
    private interface CPUConsumer {
        long consume(int workload);
    }

    public static void main(String[] args) throws InterruptedException, IOException {


        int workload = 10_000_000; //~32ms exec time on MY! machine
        Thread worker1 = new Thread(() -> simulateWorkload("counted  ", workload, consumeCPU_counted));
        Thread worker2 = new Thread(() -> simulateWorkload("unCounted", workload, consumeCPU_unCounted));


        Thread errorProducer = new Thread(() -> {
            System.out.println(System.currentTimeMillis() + " producing exceptions");
            while (true) {
                stackTraceByException(0L, 128, () -> new Exception().getStackTrace());
            }
        }
        );
        Thread errorProducer2 = new Thread(() -> {
            System.out.println(System.currentTimeMillis() + " producing exceptions");
            while (true) {
                stackTraceByException(0L, 128, () -> Thread.currentThread().getStackTrace());
            }
        }
        );


        worker1.start();
        worker2.start();
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(2));
        errorProducer.start();
        System.out.println(System.currentTimeMillis() + " started error producer1 =================");

        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(2));
        errorProducer2.start();
        System.out.println(System.currentTimeMillis() + " started error producer1 =================");

        // worker1.join();


    }

    private static void simulateWorkload(String type, int workload, CPUConsumer cpu) {
        long res;
        long counter = 0;
        while (true) {
            long start = System.nanoTime();
            res = cpu.consume(workload);
            System.out.println(System.currentTimeMillis() + " " + type + " " + (System.nanoTime() - start) + "ns " + ++counter);
        }
    }

    private static CPUConsumer consumeCPU_counted = n -> {
        long res = n;
        for (int i = 0; i < n; i++) {
            consumeCPU(1);
        }
        return res;
    };

    private static CPUConsumer consumeCPU_unCounted = n -> {
        long res = n;
        for (long i = 0; i < n; i++) {
            //will add a safepoint call here
            consumeCPU(1);
        }
        return res;
    };


    private static void stackTraceByException(long waitTime, int stackDepth, Supplier<StackTraceElement[]> stackSupplier) {
        decendStack(() -> consumeStack(stackSupplier), stackDepth);
        if (waitTime > 0)
            LockSupport.parkNanos(waitTime);
    }

    private static final Blackhole BH = new Blackhole("Today's password is swordfish. I understand instantiating Blackholes directly is dangerous.");

    private static void consumeStack(Supplier<StackTraceElement[]> stackTraces) {
        StackTraceElement[] st = stackTraces.get();
        BH.consume(st);
        BH.consume(st[0].getMethodName());
    }

    private static void decendStack(Runnable r, int depth) {
        if (depth >= 0) {
            decendStack(r, depth - 1);
        } else {
            r.run();
        }
    }

    private static volatile long consumedCPU = System.nanoTime();

    /**
     * Consume some amount of time tokens.
     * <p>
     * This method does the CPU work almost linear to the number of tokens.
     * The token cost may vary from system to system, and may change in
     * future. (Translation: it is as reliable as we can get, but not absolutely
     * reliable).
     * <p>
     * See JMH samples for the complete demo, and core benchmarks for
     * the performance assessments.
     *
     * @param tokens CPU tokens to consume
     */
    public static void consumeCPU(long tokens) {
        // If you are looking at this code trying to understand
        // the non-linearity on low token counts, know this:
        // we are pretty sure the generated assembly for almost all
        // cases is the same, and the only explanation for the
        // performance difference is hardware-specific effects.
        // Be wary to waste more time on this. If you know more
        // advanced and clever option to implement consumeCPU, let us
        // know.

        // Randomize start so that JIT could not memoize; this helps
        // to break the loop optimizations if the method is called
        // from the external loop body.
        long t = consumedCPU;

        // One of the rare cases when counting backwards is meaningful:
        // for the forward loop HotSpot/x86 generates "cmp" with immediate
        // on the hot path, while the backward loop tests against zero
        // with "test". The immediate can have different lengths, which
        // attribute to different machine code for different cases. We
        // counter that with always counting backwards. We also mix the
        // induction variable in, so that reversing the loop is the
        // non-trivial optimization.
        for (long i = tokens; i > 0; i--) {
            t += (t * 0x5DEECE66DL + 0xBL + i) & (0xFFFFFFFFFFFFL);
        }

        // Need to guarantee side-effect on the result, but can't afford
        // contention; make sure we update the shared state only in the
        // unlikely case, so not to do the furious writes, but still
        // dodge DCE.
        if (t == 42) {
            consumedCPU += t;
        }
    }
}
