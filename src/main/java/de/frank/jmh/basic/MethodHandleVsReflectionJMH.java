package de.frank.jmh.basic;


import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

/*--
Benchmark: Reflective vs MethodHandle API access to Fields - additionally "dynamic" vs "static final" handlers.
Note: depending on JVM vendor and versions, JVM can fold certain checks if VarHandle/MethodHandles are stored as static
final fields.


# VM version: JDK 13.0.1, OpenJDK 64-Bit Server VM, 13.0.1+9
Benchmark                                                              Mode  Cnt  Score   Error  Units
MethodHandleVsReflectionJMH.intVal_base                                avgt   10  1,891 ± 0,028  ns/op #baseline - directly invoke
MethodHandleVsReflectionJMH.intVal_handle_invoke                       avgt   10  5,134 ± 0,455  ns/op
MethodHandleVsReflectionJMH.intVal_handle_invokeExact                  avgt   10  5,205 ± 0,726  ns/op
MethodHandleVsReflectionJMH.intVal_handle_unreflect_invoke             avgt   10  5,444 ± 0,733  ns/op
MethodHandleVsReflectionJMH.intVal_handle_unreflect_invokeExact        avgt   10  4,859 ± 0,484  ns/op
MethodHandleVsReflectionJMH.intVal_staticHandle_inline_invoke          avgt   10  1,826 ± 0,040  ns/op #same perf as base! - static handles can fold most runtime checks!
MethodHandleVsReflectionJMH.intVal_staticHandle_invokeExact            avgt   10  1,819 ± 0,052  ns/op #same perf as base! - static handles can fold most runtime checks!
MethodHandleVsReflectionJMH.intVal_staticHandle_unreflect_invoke       avgt   10  1,841 ± 0,040  ns/op #same perf as base! - static handles can fold most runtime checks!
MethodHandleVsReflectionJMH.intVal_staticHandle_unreflect_invokeExact  avgt   10  1,806 ± 0,041  ns/op #same perf as base! - static handles can fold most runtime checks!
MethodHandleVsReflectionJMH.intVal_reflect_accessible_get              avgt   10  5,056 ± 0,407  ns/op
MethodHandleVsReflectionJMH.intVal_reflect_accessible_getInt           avgt   10  4,084 ± 0,080  ns/op #
MethodHandleVsReflectionJMH.intVal_reflect_get                         avgt   10  5,276 ± 0,165  ns/op
MethodHandleVsReflectionJMH.intVal_reflect_getInt                      avgt   10  4,974 ± 0,094  ns/op
MethodHandleVsReflectionJMH.intVal_staticReflect_accessible_get        avgt   10  3,667 ± 0,091  ns/op #static reflect can fold some checks
MethodHandleVsReflectionJMH.intVal_staticReflect_accessible_getInt     avgt   10  3,300 ± 0,076  ns/op #static reflect can fold some checks
MethodHandleVsReflectionJMH.intVal_staticReflect_get                   avgt   10  4,348 ± 0,084  ns/op #static reflect can fold some checks
MethodHandleVsReflectionJMH.intVal_staticReflect_getInt                avgt   10  4,126 ± 0,106  ns/op #static reflect can fold some checks

MethodHandleVsReflectionJMH.stringVal_base                             avgt   10  2,206 ± 0,047  ns/op #baseline
MethodHandleVsReflectionJMH.stringVal_handle_invoke                    avgt   10  5,605 ± 0,135  ns/op
MethodHandleVsReflectionJMH.stringVal_handle_unreflect_invoke          avgt   10  5,553 ± 0,106  ns/op
MethodHandleVsReflectionJMH.stringVal_staticHandle_inline_invoke       avgt   10  4,451 ± 0,129  ns/op
MethodHandleVsReflectionJMH.stringVal_staticHandle_unreflect_invoke    avgt   10  4,502 ± 0,158  ns/op
MethodHandleVsReflectionJMH.stringVal_reflect_accessible_get           avgt   10  4,294 ± 0,081  ns/op
MethodHandleVsReflectionJMH.stringVal_reflect_get                      avgt   10  4,636 ± 0,102  ns/op
MethodHandleVsReflectionJMH.stringVal_staticReflect_get                avgt   10  4,286 ± 0,061  ns/op

MethodHandleVsReflectionJMH.private_base                               avgt   10  2,231 ± 0,051  ns/op
MethodHandleVsReflectionJMH.private_reflect_accessbile_get             avgt   10  4,060 ± 0,051  ns/op
MethodHandleVsReflectionJMH.private_staticReflect_accessbile_get       avgt   10  3,238 ± 0,088  ns/op
 */
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode({Mode.AverageTime})
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread) // Important to be Scope.Benchmark
@Fork(1)
@Threads(1)
public class MethodHandleVsReflectionJMH {

    public static void main(String[] args) throws Throwable {
        Options opt = new OptionsBuilder()//
                .include(MethodHandleVsReflectionJMH.class.getName() + ".private.*")//
                //.addProfiler(GCProfiler.class)//
                .build();
        new Runner(opt).run();
    }

    private String stringVal = "foo";

    private int intVal = 42;

    enum Access {

        INSTANCE;

        private String privateVal = "bar";
    }

    private static final MethodHandle HANDLE_STRINGVAL_GETTER, HANDLE_STRINGVAL_GETTER_UNREFLECT, HANDLE_INTVAL_GETTER, HANDLE_INTVAL_GETTER_UNREFLECT, HANDLE_PRIVATEVAL_GETTER_UNREFLECT;

    private static final Field REFLECT_STRINGVAL, REFLECT_STRINGVAL_ACCESSBILE, REFLECT_PRIVATE_ACCESSBILE, REFLECT_INTVAL, REFLECT_INTVAL_ACCESSBILE;

    static {
        try {
            REFLECT_STRINGVAL = MethodHandleVsReflectionJMH.class.getDeclaredField("stringVal");
            REFLECT_STRINGVAL_ACCESSBILE = MethodHandleVsReflectionJMH.class.getDeclaredField("stringVal");
            REFLECT_STRINGVAL_ACCESSBILE.setAccessible(true);

            REFLECT_INTVAL = MethodHandleVsReflectionJMH.class.getDeclaredField("intVal");
            REFLECT_INTVAL_ACCESSBILE = MethodHandleVsReflectionJMH.class.getDeclaredField("intVal");
            REFLECT_INTVAL_ACCESSBILE.setAccessible(true);

            REFLECT_PRIVATE_ACCESSBILE = Access.class.getDeclaredField("privateVal");
            REFLECT_PRIVATE_ACCESSBILE.setAccessible(true);

            HANDLE_STRINGVAL_GETTER = MethodHandles.lookup().findGetter(MethodHandleVsReflectionJMH.class, "stringVal", String.class);
            HANDLE_STRINGVAL_GETTER_UNREFLECT = MethodHandles.lookup().unreflectGetter(MethodHandleVsReflectionJMH.class.getDeclaredField("stringVal"));

            HANDLE_INTVAL_GETTER = MethodHandles.lookup().findGetter(MethodHandleVsReflectionJMH.class, "intVal", int.class);
            HANDLE_INTVAL_GETTER_UNREFLECT = MethodHandles.lookup().unreflectGetter(MethodHandleVsReflectionJMH.class.getDeclaredField("intVal"));

            HANDLE_PRIVATEVAL_GETTER_UNREFLECT = MethodHandles.lookup().unreflectGetter(REFLECT_PRIVATE_ACCESSBILE);

        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }


    private Field reflect_StringVal, reflect_StringVal_accessible, reflect_intVal, reflect_intVal_accessbile, reflect_privateVal_accessbile;

    private MethodHandle handle_StringVal_getter, handle_StringVal_getter_unreflect, handle_intVal_getter, handle_intVal_getter_unreflect, handle_privateVal_getter_unreflect;

    private MethodHandle boundHandle_StringVal_getter;

    @Setup
    public void setup() throws Exception {
        reflect_StringVal = MethodHandleVsReflectionJMH.class.getDeclaredField("stringVal");
        reflect_StringVal_accessible = MethodHandleVsReflectionJMH.class.getDeclaredField("stringVal");
        reflect_StringVal_accessible.setAccessible(true);
        reflect_intVal = MethodHandleVsReflectionJMH.class.getDeclaredField("intVal");
        reflect_intVal_accessbile = MethodHandleVsReflectionJMH.class.getDeclaredField("intVal");
        reflect_intVal_accessbile.setAccessible(true);
        handle_StringVal_getter = MethodHandles.lookup().findGetter(MethodHandleVsReflectionJMH.class, "stringVal", String.class);
        handle_StringVal_getter_unreflect = MethodHandles.lookup().unreflectGetter(reflect_StringVal);
        handle_intVal_getter = MethodHandles.lookup().findGetter(MethodHandleVsReflectionJMH.class, "intVal", int.class);
        handle_intVal_getter_unreflect = MethodHandles.lookup().unreflectGetter(reflect_intVal);

        reflect_privateVal_accessbile = Access.class.getDeclaredField("privateVal");
        reflect_privateVal_accessbile.setAccessible(true);
        // handle_privateVal_getter_unreflect = MethodHandles.lookup().unreflectGetter(reflect_privateVal_accessbile);

        boundHandle_StringVal_getter = HANDLE_STRINGVAL_GETTER.bindTo(this);
    }

    @Benchmark
    public Object stringVal_base() {
        return stringVal;
    }

    @Benchmark
    public Object stringVal_reflect_get() throws IllegalAccessException {
        return reflect_StringVal.get(this);
    }

    @Benchmark
    public Object stringVal_reflect_accessible_get() throws IllegalAccessException {
        return reflect_StringVal_accessible.get(this);
    }

    @Benchmark
    public Object stringVal_handle_invoke() throws Throwable {
        return handle_StringVal_getter.invoke(this);
    }

    @Benchmark
    public Object stringVal_handle_invokeExact() throws Throwable {
        return handle_StringVal_getter.invokeExact(this);
    }

    @Benchmark
    public Object stringVal_handle_unreflect_invoke() throws Throwable {
        return handle_StringVal_getter_unreflect.invoke(this);
    }

    @Benchmark
    public Object stringVal_handle_unreflect_invokeExact() throws Throwable {
        return handle_StringVal_getter_unreflect.invokeExact(this);
    }

    @Benchmark
    public Object stringVal_boundHandle_invoke() throws Throwable {
        return boundHandle_StringVal_getter.invoke();
    }

    @Benchmark
    public Object stringVal_boundHandle_invokeExact() throws Throwable {
        return boundHandle_StringVal_getter.invokeExact();
    }


    @Benchmark
    public Object stringVal_staticReflect_get() throws IllegalAccessException {
        return REFLECT_STRINGVAL.get(this);
    }

    @Benchmark
    public Object stringVal_staticReflect_accessible_get() throws IllegalAccessException {
        return REFLECT_PRIVATE_ACCESSBILE.get(this);
    }

    @Benchmark
    public Object stringVal_staticHandle_inline_invoke() throws Throwable {
        return HANDLE_STRINGVAL_GETTER.invoke(this);
    }

    @Benchmark
    public Object stringVal_staticHhandle_inline_invokeExact() throws Throwable {
        return HANDLE_STRINGVAL_GETTER.invokeExact(this);
    }

    @Benchmark
    public Object stringVal_staticHandle_unreflect_invoke() throws Throwable {
        return HANDLE_STRINGVAL_GETTER_UNREFLECT.invoke(this);
    }

    @Benchmark
    public Object stringVal_staticHandle_unreflectInline_invokeExact() throws Throwable {
        return HANDLE_STRINGVAL_GETTER_UNREFLECT.invokeExact(this);
    }


    @Benchmark
    public int intVal_base() {
        return intVal;
    }

    @Benchmark
    public int intVal_reflect_get() throws IllegalAccessException {
        return (int) reflect_intVal.get(this);
    }

    @Benchmark
    public int intVal_reflect_accessible_get() throws IllegalAccessException {
        return (int) reflect_intVal_accessbile.get(this);
    }

    @Benchmark
    public int intVal_reflect_getInt() throws IllegalAccessException {
        return reflect_intVal.getInt(this);
    }

    @Benchmark
    public int intVal_reflect_accessible_getInt() throws IllegalAccessException {
        return reflect_intVal_accessbile.getInt(this);
    }

    @Benchmark
    public int intVal_handle_invoke() throws Throwable {
        return (int) handle_intVal_getter.invoke(this);
    }

    @Benchmark
    public int intVal_handle_invokeExact() throws Throwable {
        return (int) handle_intVal_getter.invokeExact(this);
    }

    @Benchmark
    public int intVal_handle_unreflect_invoke() throws Throwable {
        return (int) handle_intVal_getter_unreflect.invoke(this);
    }

    @Benchmark
    public int intVal_handle_unreflect_invokeExact() throws Throwable {
        return (int) handle_intVal_getter_unreflect.invokeExact(this);
    }


    @Benchmark
    public int intVal_staticReflect_get() throws IllegalAccessException {
        return (int) REFLECT_INTVAL.get(this);
    }

    @Benchmark
    public int intVal_staticReflect_accessible_get() throws IllegalAccessException {
        return (int) REFLECT_INTVAL_ACCESSBILE.get(this);
    }

    @Benchmark
    public int intVal_staticReflect_getInt() throws IllegalAccessException {
        return REFLECT_INTVAL.getInt(this);
    }

    @Benchmark
    public int intVal_staticReflect_accessible_getInt() throws IllegalAccessException {
        return REFLECT_INTVAL_ACCESSBILE.getInt(this);
    }

    @Benchmark
    public int intVal_staticHandle_inline_invoke() throws Throwable {
        return (int) HANDLE_INTVAL_GETTER.invoke(this);
    }

    @Benchmark
    public int intVal_staticHandle_invokeExact() throws Throwable {
        return (int) HANDLE_INTVAL_GETTER.invokeExact(this);
    }

    @Benchmark
    public int intVal_staticHandle_unreflect_invoke() throws Throwable {
        return (int) HANDLE_INTVAL_GETTER_UNREFLECT.invoke(this);
    }

    @Benchmark
    public int intVal_staticHandle_unreflect_invokeExact() throws Throwable {
        return (int) HANDLE_INTVAL_GETTER_UNREFLECT.invokeExact(this);
    }


    @Benchmark
    public String private_base() {
        return Access.INSTANCE.privateVal; // accessor method
    }

    @Benchmark
    public Object private_reflect_accessbile_get() throws Exception {
        return reflect_privateVal_accessbile.get(Access.INSTANCE);
    }

    //java.lang.invoke.WrongMethodTypeException: expected (Access)String but found (Access)Object
//    @Benchmark
//    public Object private_handle_unreflect_invokeExact() throws Throwable {
//        return handle_privateVal_getter_unreflect.invokeExact(Access.INSTANCE);
//    }

    @Benchmark
    public Object private_staticReflect_accessbile_get() throws Exception {
        return REFLECT_PRIVATE_ACCESSBILE.get(Access.INSTANCE);
    }


    @Benchmark
    public Object private_staticHandle_unreflect_invokeExact() throws Throwable {
        return HANDLE_PRIVATEVAL_GETTER_UNREFLECT.invokeExact(Access.INSTANCE);
    }


}
