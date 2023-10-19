package de.frank.jmh.algorithms;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/*-
DISCLAIMER
These kind of low level hacks are usually know to the compiler anyway and the compiler will perform it for you.
On top, the CPU's branch predictor will most likely optimize away any left over perf-penalty of branching.
There MIGHT be some corner case, where the compiler chickens out and you MIGHT save some cycles by doing such hacks yourself (But usually you wont)

There are times and cases when branching is actually faster.
e.g. on ARM architecture, nearly all instructions can have a conditional flag, and reducing branches will slow down your code.
https://developer.arm.com/documentation/den0013/d/ARM-Thumb-Unified-Assembly-Language-Instructions/Instruction-set-basics/Conditional-execution?lang=en
What you actually want to optimize in most cases is your code size to maximize use of the instruction cache (and, well, others).

So this benchmark is mostly for shits and giggles and to see how far CPU's and compilers have come.

What is "chance"?
- 0.0 always choice a) - never b) - CPU branch predictor likes that
- 0.5 equal chance for a) and b)  - CPU branch predictor hates that
- 1.0 never choice a) - always b) - CPU branch predictor likes that
- anything in between: the JVM and CPU branch predictor will usually figure out the "most likely case" and optimize for that.
The less likely code path gets a penalty.

"baseline" = branching = has an if statement
Benchmark                                                        (chance)  Mode  Cnt  Score   Error  Units
#Branchless slightly faster than branching
BranchlessDontDoItJMH.a_less_than_zero_return_b_else_a                0.0  avgt   20  1,912 ± 0,037  ns/op
BranchlessDontDoItJMH.a_less_than_zero_return_b_else_a_baseline       0.0  avgt   20  2,022 ± 0,020  ns/op
BranchlessDontDoItJMH.a_less_than_zero_return_b_else_a                0.5  avgt   20  1,899 ± 0,013  ns/op
BranchlessDontDoItJMH.a_less_than_zero_return_b_else_a_baseline       0.5  avgt   20  2,040 ± 0,015  ns/op
BranchlessDontDoItJMH.a_less_than_zero_return_b_else_a                  1  avgt   20  1,924 ± 0,054  ns/op
BranchlessDontDoItJMH.a_less_than_zero_return_b_else_a_baseline         1  avgt   20  2,052 ± 0,029  ns/op

#Branchless slightly faster than branching
BranchlessDontDoItJMH.abs_baseline                                    0.0  avgt   20  1,928 ± 0,026  ns/op
BranchlessDontDoItJMH.abs_branchless                                  0.0  avgt   20  1,877 ± 0,020  ns/op
BranchlessDontDoItJMH.abs_baseline                                    0.5  avgt   20  2,070 ± 0,126  ns/op
BranchlessDontDoItJMH.abs_branchless                                  0.5  avgt   20  1,903 ± 0,070  ns/op
BranchlessDontDoItJMH.abs_baseline                                      1  avgt   20  2,095 ± 0,052  ns/op
BranchlessDontDoItJMH.abs_branchless                                    1  avgt   20  1,920 ± 0,055  ns/op

#Use JDK function Math.max instead of your own stupid idea!
BranchlessDontDoItJMH.max_baselineMathMAx                             0.0  avgt   20  0,754 ± 0,088  ns/op #VERY clear winner
BranchlessDontDoItJMH.max_baselineIf                                  0.0  avgt   20  2,283 ± 0,106  ns/op
BranchlessDontDoItJMH.max_branchless                                  0.0  avgt   20  2,267 ± 0,092  ns/op
BranchlessDontDoItJMH.max_baselineMathMAx                             0.5  avgt   20  0,706 ± 0,084  ns/op
BranchlessDontDoItJMH.max_baselineIf                                  0.5  avgt   20  2,247 ± 0,056  ns/op
BranchlessDontDoItJMH.max_branchless                                  0.5  avgt   20  2,269 ± 0,140  ns/op
BranchlessDontDoItJMH.max_baselineMathMAx                               1  avgt   20  0,763 ± 0,142  ns/op
BranchlessDontDoItJMH.max_baselineIf                                    1  avgt   20  2,288 ± 0,113  ns/op
BranchlessDontDoItJMH.max_branchless                                    1  avgt   20  2,363 ± 0,120  ns/op

#branch wins over branchless
BranchlessDontDoItJMH.x_equals_y_return_a_else_b                      0.0  avgt   20  2,401 ± 0,024  ns/op
BranchlessDontDoItJMH.x_equals_y_return_a_else_b_baseline             0.0  avgt   20  2,039 ± 0,016  ns/op
BranchlessDontDoItJMH.x_equals_y_return_a_else_b                      0.5  avgt   20  2,405 ± 0,021  ns/op
BranchlessDontDoItJMH.x_equals_y_return_a_else_b_baseline             0.5  avgt   20  2,031 ± 0,019  ns/op
BranchlessDontDoItJMH.x_equals_y_return_a_else_b                        1  avgt   20  2,395 ± 0,015  ns/op
BranchlessDontDoItJMH.x_equals_y_return_a_else_b_baseline               1  avgt   20  2,035 ± 0,015  ns/op


 * References:
 * - code examples by Nathan Tippy (Twitter: @NathanTippy)
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 2)
@State(Scope.Benchmark)
public class BranchlessDontDoItJMH {
    public static class BaseState {
        @Param({"0.0", "0.5", "1"})
        double chance;
    }

    @org.openjdk.jmh.annotations.State(Scope.Thread)
    public static class IsEqualState extends BaseState {
        private int toCompareA, toCompareB;
        private static final int HALF_INT = Integer.MAX_VALUE / 2;

        public IsEqualState() {
            //For jmh
        }

        @Setup(Level.Trial)
        public void doSetup() {
            var r = ThreadLocalRandom.current();
            if (chance == 1.0d || r.nextDouble() < chance) { //always similar or isSimilar chance hit
                toCompareA = toCompareB = r.nextInt();
            } else { //dissimilar
                toCompareA = r.nextInt(0, HALF_INT);
                toCompareB = r.nextInt(HALF_INT, Integer.MAX_VALUE);
            }
        }
    }

    @org.openjdk.jmh.annotations.State(Scope.Thread)
    public static class IsLessThanState extends BaseState {
        private int compareMe, returnIfLessThan;

        public IsLessThanState() {
            //For jmh
        }

        @Setup(Level.Trial)
        public void doSetup() {
            var r = ThreadLocalRandom.current();
            compareMe = r.nextInt();
            if (chance == 1.0d || r.nextDouble() < chance) { //always "lessThan" or isLessThan chance hit
                compareMe = -compareMe;
            }
            returnIfLessThan = compareMe - 1;

        }
    }

    @Benchmark
    public int x_equals_y_return_a_else_b(IsEqualState s) {
        return xEqualsYReturnAElseB(s.toCompareA, s.toCompareB, s.toCompareA, s.toCompareB);
    }

    @Benchmark
    public int x_equals_y_return_a_else_b_baseline(IsEqualState s) {
        return xEqualsYReturnAElseBBaseline(s.toCompareA, s.toCompareB, s.toCompareA, s.toCompareB);
    }

    @Benchmark
    public int a_less_than_zero_return_b_else_a(IsLessThanState s) {
        return aLessThanZeroReturnBElseA(s.compareMe, s.returnIfLessThan);
    }


    @Benchmark
    public int a_less_than_zero_return_b_else_a_baseline(IsLessThanState s) {
        return aLessThanZeroReturnBElseABaseline(s.compareMe, s.returnIfLessThan);
    }

    @Benchmark
    public int max_baselineIf(IsLessThanState s) {
        return maxIF(s.compareMe, s.returnIfLessThan);
    }

    @Benchmark
    public int max_baselineMathMAx(IsLessThanState s) {
        return Math.max(s.compareMe, s.returnIfLessThan); //probably uses an Instrinsic on our CPU instead of "if"
    }

    @Benchmark
    public int max_branchless(IsLessThanState s) {
        return maxBranchless(s.compareMe, s.returnIfLessThan);
    }

    @Benchmark
    public int abs_baseline(IsLessThanState s) {
        return abs(s.compareMe);
    }

    @Benchmark
    public int abs_branchless(IsLessThanState s) {
        return absBranchless(s.compareMe);
    }

    // return x == y ? a : b
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private int xEqualsYReturnAElseB(int x, int y, int a, int b) {
        int tmp = ((x - y) - 1) >> 31;

        int mask = (((x - y) >> 31) ^ tmp) & tmp;

        return (a & mask) | (b & (~mask));
    }

    // return x == y ? a : b
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private int xEqualsYReturnAElseBBaseline(int x, int y, int a, int b) {
        if (x == y) {
            return a;
        } else {
            return b;
        }
    }

    // return a < 0 ? b : a;
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private int aLessThanZeroReturnBElseA(int a, int b) {
        int mask = a >> 31;
        return (b & mask) | ((~mask) & a);
    }

    // return a < 0 ? b : a;
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private int aLessThanZeroReturnBElseABaseline(int a, int b) {
        if (a < 0) {
            return b;
        } else {
            return a;
        }
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private int maxIF(int a, int b) {
        if (a > b) {
            return a;
        } else {
            return b;
        }
    }


    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private int maxBranchless(int a, int b) {
        int diff = a - b;
        int dsgn = diff >> 31;
        return a - (diff & dsgn);
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private int abs(int a) {
        if (a >= 0)
            return a;
        else
            return -a;
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private int absBranchless(int a) {
        int mask = a >> 31;
        a ^= mask;
        a -= mask;
        return a;
    }

}