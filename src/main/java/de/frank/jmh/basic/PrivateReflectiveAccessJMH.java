package de.frank.jmh.basic;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

/*--

This tests tries to access the private No-Copy new String(char [], shared=true) Constructor.

But it basically tests (private) various access methods: copying newString vs ReflectionAPI vs MethodHandlerAPI vs Lamda(via LamdaMetaFactory)
Lamda and MethodHandlerAPI are essentially the same and superior to ReflectionAPI calls.
Still, the ReflectionAPI benefits greatly form the MethodHandlerAPI and got a hell of a lot faster since java 8!



Result:                                                               gc.alloc.
                               (stringLen)  Mode  Cnt        Score    rate.norm
newStringConstructor                   10  avgt   10    13,2 ns/op   64,0 B/op create new String(char [] buf) which will copy internally.
newStringConstructor                  100  avgt   10    26,1 ns/op  240,0 B/op
newStringConstructor                 1000  avgt   10   245,2 ns/op 2040,0 B/op

usingStringBuilder                     10  avgt   10    14,5 ns/op   64,0 B/op  StringBuilder copies the String :(
usingStringBuilder                    100  avgt   10    26,4 ns/op  240,0 B/op
usingStringBuilder                   1000  avgt   10   236,9 ns/op 2040,0 B/op

newStringSharedConstructor_lambda       10  avgt   10     5,4 ns/op   24,0 B/op using private String(char [] buf, boolean shared) constructor
newStringSharedConstructor_lambda      100  avgt   10     5,6 ns/op   24,0 B/op lambdas are basically MethodHandles with a nicer signature
newStringSharedConstructor_lambda     1000  avgt   10     5,5 ns/op   24,0 B/op

newStringSharedConstructor_handler     10  avgt   10     5,7 ns/op   24,0 B/op using private String(char [] buf, boolean shared) constructor
newStringSharedConstructor_handler    100  avgt   10     5,5 ns/op   24,0 B/op
newStringSharedConstructor_handler   1000  avgt   10     5,5 ns/op   24,0 B/op

newStringSharedConstructor_reflect     10  avgt   10    10,9 ns/op   48,0 B/op using private String(char [] buf, boolean shared) constructor
newStringSharedConstructor_reflect    100  avgt   10    10,7 ns/op   48,0 B/op
newStringSharedConstructor_reflect   1000  avgt   10    10,8 ns/op   48,0 B/op
*/

/**
 * @author Michael Frank
 * @version 1.0 13.07.2018
 */
@State(Scope.Thread)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class PrivateReflectiveAccessJMH {

    private static final BiFunction<char[], Boolean, String> NEW_STRING_SHARED_lambda = createSharedNewStringAccessorlambda();
    private static final MethodHandle NEW_STRING_SHARED_HANDLE = createSharedNewStringAccessorHandle();
    private static final Constructor<String> NEW_STRING_SHARED_REFLECTIVE = createSharedNewStringAccessorReflective();


    public static void main(String[] args) throws Throwable {

        //Function check
        PrivateReflectiveAccessJMH test = new PrivateReflectiveAccessJMH();
        test.setup();
        System.out.println("newString: " + test.newStringConstructor());
        System.out.println("handler:   " + test.newStringSharedConstructor_handler());
        System.out.println("lambda:     " + test.newStringSharedConstructor_lambda());
        System.out.println("reflect:   " + test.newStringSharedConstructor_reflect());


        Options opt = new OptionsBuilder()//
                .include(".*" + PrivateReflectiveAccessJMH.class.getSimpleName() + ".*")//
                .addProfiler(GCProfiler.class)//
                .build();
        new Runner(opt).run();
    }

    private static BiFunction<char[], Boolean, String> createSharedNewStringAccessorlambda() {
        try {
            return PrivateAccessFactory.createConstructorlambda(BiFunction.class, String.class, char[].class, boolean.class);
        } catch (Throwable t) {
            new RuntimeException(t);
        }
        return null;
    }

    private static Constructor<String> createSharedNewStringAccessorReflective() {
        try {
            return PrivateAccessFactory.getConstructor(String.class, char[].class, boolean.class);
        } catch (Throwable t) {
            new RuntimeException(t);
        }
        return null;
    }

    private static MethodHandle createSharedNewStringAccessorHandle() {
        try {
            return PrivateAccessFactory.getConstructorHandle(String.class, char[].class, boolean.class);
        } catch (Throwable t) {
            new RuntimeException(t);
        }
        return null;
    }

    @Param({"10", "100", "1000"})
    int stringLen = 10;

    char[] buffer;
    StringBuilder builder;

    @Setup
    public void setup() {
        buffer = new char[stringLen];
        Arrays.fill(buffer, 'a');
        builder = new StringBuilder(buffer.length);
        builder.append(buffer);
    }

    @Benchmark
    public String newStringConstructor() {
        return new String(buffer);
    }

    @Benchmark
    public String usingStringBuilder() {
        return builder.toString();
    }

    @Benchmark
    public String newStringSharedConstructor_lambda() {
        return NEW_STRING_SHARED_lambda.apply(buffer, true);
    }

    @Benchmark
    public String newStringSharedConstructor_reflect() {
        try {
            return NEW_STRING_SHARED_REFLECTIVE.newInstance(buffer, true);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Benchmark
    public String newStringSharedConstructor_handler() {
        try {
            return (String) NEW_STRING_SHARED_HANDLE.invokeExact(buffer, true);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }


    public static class PrivateAccessFactory {

        private static final MethodHandles.Lookup LOOKUP = privateAccessLookup();

        private static MethodHandles.Lookup privateAccessLookup() {
            //Normally we cannot create lambdas for private methods/constructors
            //Dirty hack to gain access to a "Lookup" implementation which is permitted to create private accessors with LambdaMetafactory
            try {
                final MethodHandles.Lookup original = MethodHandles.lookup();
                final Field internal = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
                internal.setAccessible(true);
                return (MethodHandles.Lookup) internal.get(original);
            } catch (Exception ex) {
                throw new RuntimeException("failed to get private IMPL_LOOKUP implementation", ex);
            }
        }


        public static <S, T> T createConstructorlambda(Class<T> targetFunction, Class<S> theClazzToReflect, Class... constructorArgs) throws Throwable {
            return createConstructorlambda(targetFunction, getConstructorHandle(theClazzToReflect, constructorArgs));
        }

        public static <T> MethodHandle getConstructorHandle(Class<T> target, Class... constructorArgs) throws NoSuchMethodException, IllegalAccessException {
            Constructor<T> c = getConstructor(target, constructorArgs);
            return PrivateAccessFactory.LOOKUP.unreflectConstructor(c);
        }

        public static <T> Constructor<T> getConstructor(Class<T> target, Class... constructorArgs) throws NoSuchMethodException {
            Constructor<T> c = target.getDeclaredConstructor(constructorArgs);
            c.setAccessible(true);
            return c;
        }

        public static <T> T createConstructorlambda(Class<T> targetFunctionalInterface, MethodHandle toInvoke) {
            try {
                String targetMethodName = targetFunctionalInterface.getDeclaredMethods()[0].getName();
                MethodType targetMethodType = MethodType.methodType(targetFunctionalInterface);

                CallSite callSite = LambdaMetafactory.metafactory(//
                        LOOKUP,//
                        targetMethodName,//
                        targetMethodType,
                        toInvoke.type().generic(),//
                        toInvoke, //
                        toInvoke.type());

                return (T) callSite.getTarget().invoke();
            } catch (Throwable t) {
                throw new RuntimeException("Unable to create function '" + targetFunctionalInterface.getName() + "' for constructor of " + toInvoke.toString(), t);
            }
        }


    }
}
