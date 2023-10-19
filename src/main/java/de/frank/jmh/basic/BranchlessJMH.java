package de.frank.jmh.basic;

import org.apache.commons.lang3.RandomStringUtils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/*--
DISCLAIMER
These kind of low level hacks are usually know to the compiler anyway and the compiler will perform it for you.
On top, the CPU's branch predictor will most likely optimize away any left over perf-penalty of branching.
There MIGHT be some corner case, where the compiler chickens out and you MIGHT save some cycles by doing such hacks yourself (But usually you wont)

There are times and cases when branching is actually faster.
e.g. on ARM architecture, nearly all instructions can have a conditional flag, and reducing branches will slow down your code.
https://developer.arm.com/documentation/den0013/d/ARM-Thumb-Unified-Assembly-Language-Instructions/Instruction-set-basics/Conditional-execution?lang=en
What you actually want to optimize in most cases is your code size to maximize use of the instruction cache (and, well, others).

So this benchmark is mostly for shits and giggles and to see how far CPU's and compilers have come.

Disclaimer 2)
------------
The custom impls are a very simplified US-ASCII only version of the JDK impls!
The JDK impls are much more complex and handle all kinds of edge cases and other encodings.

Result interpretation:
---------------------
- Branching vs Branchless makes almost no difference (you are doing the compilers and branch-predictors work)
- The hand crafted variants are faster but are a very simplified US-ASCII only version of the JDK impls - so dont use them in production probably.
- The byte variant plain sucks
- The JDK variant is slower, but it handles all kinds of edge cases and other encodings.


Benchmark                                                        Mode  Cnt    Score    Error  Units
BranchlessJMH.string_toLowerCase                                 avgt    5   80,332 ±  8,671  ns/op # Baseline
BranchlessJMH.branching_toLowerCaseAsciiBytes                    avgt    5  136,683 ±  5,169  ns/op
BranchlessJMH.branching_toLowerCaseAsciiChar                     avgt    5   43,090 ±  2,898  ns/op
BranchlessJMH.branching_toLowerCaseAsciiCharArrayUninitialized   avgt    5   54,289 ± 25,125  ns/op
BranchlessJMH.branchless_toLowerCaseAsciiBytes                   avgt    5  101,002 ± 13,457  ns/op
BranchlessJMH.branchless_toLowerCaseAsciiChar                    avgt    5   48,520 ±  7,554  ns/op
BranchlessJMH.branchless_toLowerCaseAsciiCharArrayUninitialized  avgt    5   56,525 ±  9,719  ns/op

#special inplace variant
BranchlessJMH.charArray_toLowerCaseInplace                       avgt    5   60,964 ± 16,392  ns/op #baseline
BranchlessJMH.branching_toLowerCaseInplace                       avgt    5   34,105 ±  2,234  ns/op
BranchlessJMH.branchless_toLowerCaseInplace                      avgt    5   34,791 ±  2,703  ns/op

#upper
BranchlessJMH.string_toUpperCase                                 avgt    5  126,721 ± 34,646  ns/op

 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1,
        jvmArgsAppend = {
                "--add-opens"
                , "java.base/jdk.internal.misc=ALL-UNNAMED"
        })
public class BranchlessJMH {

    public static void main(String[] args) throws Throwable {
        System.out.println(BranchlessStringUtils.toLowerCaseAsciiCharArrayUninitialized("aAbBcCzZ"));
        System.out.println(BranchlessStringUtils.toLowerCaseAsciiChar("aAbBcCzZ"));
        System.out.println(StringUtils.toLowerCaseAsciiCharArrayUninitialized("aAbBcCzZ"));
        System.out.println(StringUtils.toLowerCaseAsciiChar("aAbBcCzZ"));

        Options opt = new OptionsBuilder()//
                .include(BranchlessJMH.class.getName() + ".*")//
                //.addProfiler(GCProfiler.class)//
                .build();
        new Runner(opt).run();
    }

    @State(Scope.Thread)
    public static class MyState {

        String value;
        char[] valueChars;

        @Setup(Level.Invocation)
        public void setup() {
            value = RandomStringUtils.randomAlphanumeric(15);
            valueChars = value.toCharArray();
        }
    }

    @Benchmark
    public String string_toLowerCase(MyState s) {
        return s.value.toLowerCase();
    }

    @Benchmark
    public String string_toUpperCase(MyState s) {
        return s.value.toUpperCase();
    }

    @Benchmark
    public char[] charArray_toLowerCaseInplace(MyState s) {
        for (int i = 0; i < s.valueChars.length; i++) {
            s.valueChars[i] = Character.toLowerCase(s.valueChars[i]);
        }
        return s.valueChars;
    }

    @Benchmark
    public char[] branchless_toLowerCaseInplace(MyState s) {
        BranchlessStringUtils.toLowerCaseAscii(s.valueChars);
        return s.valueChars;
    }

    @Benchmark
    public char[] branching_toLowerCaseInplace(MyState s) {
        StringUtils.toLowerCaseAscii(s.valueChars);
        return s.valueChars;
    }

    @Benchmark
    public String branchless_toLowerCaseAsciiBytes(MyState s) {
        return BranchlessStringUtils.toLowerCaseAsciiBytes(s.value);
    }

    @Benchmark
    public String branchless_toLowerCaseAsciiChar(MyState s) {
        return BranchlessStringUtils.toLowerCaseAsciiChar(s.value);
    }

    @Benchmark
    public char[] branchless_toLowerCaseAsciiCharArrayUninitialized(MyState s) {
        return BranchlessStringUtils.toLowerCaseAsciiCharArrayUninitialized(s.value);
    }


    @Benchmark
    public String branching_toLowerCaseAsciiBytes(MyState s) {
        return StringUtils.toLowerCaseAsciiBytes(s.value);
    }

    @Benchmark
    public String branching_toLowerCaseAsciiChar(MyState s) {
        return StringUtils.toLowerCaseAsciiChar(s.value);
    }

    @Benchmark
    public char[] branching_toLowerCaseAsciiCharArrayUninitialized(MyState s) {
        return StringUtils.toLowerCaseAsciiCharArrayUninitialized(s.value);
    }


    public static class BranchlessStringUtils {


        public static String toLowerCaseAsciiBytes(String in) {
            byte[] newX = in.getBytes();
            toLowerCaseAscii(newX);
            try {
                return new String(newX, StandardCharsets.US_ASCII.name());
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("should never happen", e);
            }
        }

        public static String toLowerCaseAsciiChar(String in) {
            char[] newX = in.toCharArray();
            toLowerCaseAscii(newX);
            return new String(newX);
        }


        public static char[] toLowerCaseAsciiCharArrayUninitialized(String in) {
            char[] out = (char[]) UnsafeAccess.allocateUninitializedArray(char.class, in.length());
            for (int i = 0; i < in.length(); i++) {
                out[i] = toLowerAsciiInvariant(in.charAt(i));
            }
            return out;
        }


        public static void toLowerCaseAscii(char[] in) {
            for (int i = 0; i < in.length; i++) {
                in[i] = toLowerAsciiInvariant(in[i]);
            }
        }


        public static void toLowerCaseAscii(byte[] in) {
            for (int i = 0; i < in.length; i++) {
                in[i] = toLowerAsciiInvariant(in[i]);
            }
        }

        public static String toUpperCaseAsciiBytes(String in) {
            byte[] newX = in.getBytes();
            toUpperCaseAscii(newX);
            try {
                return new String(newX, StandardCharsets.US_ASCII.name());
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("should never happen", e);
            }
        }

        public static String toUpperCaseAsciiChar(String in) {
            char[] newX = in.toCharArray();
            toUpperCaseAscii(newX);
            return new String(newX);
        }

        public static char[] toUpperCaseAsciiCharArrayUninitialized(String in) {
            char[] out = (char[]) UnsafeAccess.allocateUninitializedArray(char.class, in.length());
            for (int i = 0; i < in.length(); i++) {
                out[i] = toUpperAsciiInvariant(in.charAt(i));
            }
            return out;
        }

        public static void toUpperCaseAscii(char[] in) {
            for (int i = 0; i < in.length; i++) {
                in[i] = toUpperAsciiInvariant(in[i]);
            }
        }

        public static void toUpperCaseAscii(byte[] in) {
            for (int i = 0; i < in.length; i++) {
                in[i] = toUpperAsciiInvariant(in[i]);
            }
        }


        public static char toUpperAsciiInvariant(char c) {
            int maskA = 'a' - c - 1;
            int maskB = c - 'z' - 1;
            int mask = (maskA & maskB) >> 16;
            return (char) (c ^ (mask & 0x20));
        }


        public static byte toUpperAsciiInvariant(byte c) {
            int maskA = 'a' - c - 1;
            int maskB = c - 'z' - 1;
            int mask = (maskA & maskB) >> 16;
            return (byte) (c ^ (mask & 0x20));

        }


        public static byte toLowerAsciiInvariant(byte c) {
            int maskA = 'A' - c - 1;
            int maskZ = c - 'Z' - 1;
            int mask = (maskA & maskZ) >> 16;
            return (byte) (c ^ (mask & 0x20));
        }


        public static char toLowerAsciiInvariant(char c) {
            int maskA = 'A' - c - 1;
            int maskZ = c - 'Z' - 1;
            int mask = (maskA & maskZ) >> 16;
            return (char) (c ^ (mask & 0x20));
        }
    }

    public static class StringUtils {

        public static String toLowerCaseAsciiBytes(String in) {
            byte[] newX = in.getBytes();
            toLowerCaseAscii(newX);
            try {
                return new String(newX, StandardCharsets.US_ASCII.name());
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("should never happen", e);
            }
        }

        public static String toLowerCaseAsciiChar(String in) {
            char[] newX = in.toCharArray();
            toLowerCaseAscii(newX);
            return new String(newX);
        }


        public static char[] toLowerCaseAsciiCharArrayUninitialized(String in) {
            char[] out = (char[]) UnsafeAccess.allocateUninitializedArray(char.class, in.length());
            for (int i = 0; i < in.length(); i++) {
                out[i] = toLowerAsciiInvariant(in.charAt(i));
            }
            return out;
        }


        public static void toLowerCaseAscii(char[] in) {
            for (int i = 0; i < in.length; i++) {
                in[i] = toLowerAsciiInvariant(in[i]);
            }
        }


        public static void toLowerCaseAscii(byte[] in) {
            for (int i = 0; i < in.length; i++) {
                in[i] = toLowerAsciiInvariant(in[i]);
            }
        }

        public static String toUpperCaseAsciiBytes(String in) {
            byte[] newX = in.getBytes();
            toUpperCaseAscii(newX);
            try {
                return new String(newX, StandardCharsets.US_ASCII.name());
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("should never happen", e);
            }
        }

        public static String toUpperCaseAsciiChar(String in) {
            char[] newX = in.toCharArray();
            toUpperCaseAscii(newX);
            return new String(newX);
        }

        public static char[] toUpperCaseAsciiCharArrayUninitialized(String in) {
            char[] out = (char[]) UnsafeAccess.allocateUninitializedArray(char.class, in.length());
            for (int i = 0; i < in.length(); i++) {
                out[i] = toUpperAsciiInvariant(in.charAt(i));
            }
            return out;
        }

        public static void toUpperCaseAscii(char[] in) {
            for (int i = 0; i < in.length; i++) {
                in[i] = toUpperAsciiInvariant(in[i]);
            }
        }

        public static void toUpperCaseAscii(byte[] in) {
            for (int i = 0; i < in.length; i++) {
                in[i] = toUpperAsciiInvariant(in[i]);
            }
        }

        public static byte toLowerAsciiInvariant(byte c) {
            if (c - 'A' <= 'Z' - 'A') {
                c = (byte) (c | 0x20);
            }
            return c;
        }

        public static char toLowerAsciiInvariant(char c) {
            if (c - 'A' <= 'Z' - 'A') {
                c = (char) (c | 0x20);
            }
            return c;
        }

        public static char toUpperAsciiInvariant(char c) {
            if ((c - 'a') <= ('z' - 'a')) {
                c = (char) (c & ~0x20);
            }
            return c;
        }

        public static byte toUpperAsciiInvariant(byte c) {
            if ((c - 'a') <= ('z' - 'a')) {
                c = (byte) (c & ~0x20);
            }
            return c;
        }

        public static boolean isAscii(char c) {
            return c < 0x80;
        }
    }


    static class UnsafeAccess {
        private static final MethodHandle ALLOCATE_UNINITIALIZED_ARRAY = mkHandleAllocateUninitializedArray();


        public static Object allocateUninitializedArray(Class<?> type, int len) {
            try {
                return UnsafeAccess.ALLOCATE_UNINITIALIZED_ARRAY.invoke(type, len);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }


        private static MethodHandle mkHandleAllocateUninitializedArray() {
            MethodHandles.Lookup lookup = MethodHandles.lookup();

            try {
                Class<?> unsafeClass = lookup.findClass("jdk.internal.misc.Unsafe");
                Object theUnsafe = lookup.findStatic(unsafeClass, "getUnsafe", MethodType.methodType(unsafeClass)).invoke();
                return lookup.findVirtual(unsafeClass, "allocateUninitializedArray", MethodType.methodType(Object.class, Class.class, int.class)).bindTo(theUnsafe);
            } catch (NoSuchMethodException | ClassNotFoundException | IllegalAccessException e1) {
                throw new ExceptionInInitializerError(e1);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }

        }
    }

}
