//package de.frank.jmh.architecture;
//
//import net.openhft.chronicle.bytes.Byteable;
//import net.openhft.chronicle.bytes.BytesStore;
//import net.openhft.chronicle.bytes.NativeBytesStore;
//import net.openhft.chronicle.core.Memory;
//import net.openhft.chronicle.core.OS;
//import net.openhft.chronicle.values.Array;
//import net.openhft.chronicle.values.Values;
//import org.apacheLazy.commons.lang3.mutable.MutableDouble;
//import org.jetbrains.annotations.NotNull;
//import org.openjdk.jmh.annotations.*;
//import org.openjdk.jmh.infra.Blackhole;
//import org.openjdk.jmh.profile.GCProfiler;
//import org.openjdk.jmh.results.RunResult;
//import org.openjdk.jmh.results.format.ResultFormatFactory;
//import org.openjdk.jmh.results.format.ResultFormatType;
//import org.openjdk.jmh.runner.Runner;
//import org.openjdk.jmh.runner.RunnerException;
//import org.openjdk.jmh.runner.options.OptionsBuilder;
//
//import java.io.ByteArrayOutputStream;
//import java.io.PrintStream;
//import java.lang.ref.Cleaner;
//import java.nio.ByteBuffer;
//import java.nio.ByteOrder;
//import java.nio.DoubleBuffer;
//import java.util.AbstractMap.SimpleEntry;
//import java.util.Arrays;
//import java.util.Collection;
//import java.util.Map;
//import java.util.concurrent.TimeUnit;
//import java.util.function.Consumer;
//import java.util.stream.Collectors;
//import java.util.stream.Stream;
//
//public class FlyweightStructJMH {
//
//
//    private static final int SIZE = 100_000;//Vect3[SIZE]
//
//    //Benchmark control
//    private static final int WARMUP_ITERATIONS = 5;
//    private static final int MEASUREMENT_ITERATIONS = 5;
//    private static final int FORKS = 1;
//
//    private static final Memory MEM = OS.memory();
//
//    public static void main(String[] args) throws RunnerException, InterruptedException {
//        //System.setProperty("chronicle.values.dumpCode", "true");
//
//        UnsafeVect3 vect = UnsafeVect3.newVectors(SIZE);
//
//        System.out.println("Native Byte order: " + ByteOrder.nativeOrder());
//        //System.out.println("pageSize:" +MEM.UNSAFE.pageSize());
//
//        System.out.println("====================");
//        System.out.println("uninitialized vect[] (mem garbage) ");
//        System.out.println("====================");
//        System.out.println("dump first 5 vect's:");
//        dumpMemory(vect.memoryBaseAddr, Double.BYTES * 3 * 5);
//        System.out.println("..");
//        System.out.println("vect[0]:");
//        System.out.printf("%016x: %016x => %.1f%n", vect.xAddr(), MEM.readLong(vect.xAddr()), vect.getX());
//        System.out.printf("%016x: %016x => %.1f%n", vect.yAddr(), MEM.readLong(vect.yAddr()), vect.getY());
//        System.out.printf("%016x: %016x => %.1f%n", vect.zAddr(), MEM.readLong(vect.zAddr()), vect.getZ());
//
//        init(vect);
//        System.out.println();
//        System.out.println("====================");
//        System.out.println("initialized vect[] ");
//        System.out.println("====================");
//        System.out.println("dump first 5 vect's:");
//        dumpMemory(vect.memoryBaseAddr, Double.BYTES * 3 * 5);
//        System.out.println("..");
//        System.out.println("initialized vect[0]:");
//        System.out.printf("%016x: %016x => %.1f%n", vect.xAddr(), MEM.readLong(vect.xAddr()), vect.getX());
//        System.out.printf("%016x: %016x => %.1f%n", vect.yAddr(), MEM.readLong(vect.yAddr()), vect.getY());
//        System.out.printf("%016x: %016x => %.1f%n", vect.zAddr(), MEM.readLong(vect.zAddr()), vect.getZ());
//
//        System.out.println();
//        System.out.println("====================");
//        System.out.println("modify vect[0] ");
//        System.out.println("====================");
//        vect.setZ(5d);
//        OS.memory().writeDouble(vect.yAddr(), 123.456d);
//        OS.memory().writeLong(vect.zAddr(), 876543210987654321L);
//        System.out.println();
//        System.out.println("set vect[0].x=5.0");
//        System.out.println("Direct write: vect[0].y=123.456");
//        System.out.println("Direct write: vect[0].z=876543210987654321L (as long)");
//        dumpMemory(vect.memoryBaseAddr, Double.BYTES * 3 * 1);
//        System.out.println("..");
//        System.out.printf("%016x: %016x => %.1f%n", vect.xAddr(), MEM.readLong(vect.xAddr()), vect.getX());
//        System.out.printf("%016x: %016x => %.1f%n", vect.yAddr(), MEM.readLong(vect.yAddr()), vect.getY());
//        System.out.printf("%016x: %016x => %.1f %d%n", vect.zAddr(), MEM.readLong(vect.zAddr()), vect.getZ(), MEM.readLong(vect.zAddr()));
//
//
//        vect.movePtr(SIZE - 1);
//        System.out.println();
//        System.out.println("====================");
//        System.out.println("vect[SIZE-1]:");
//        System.out.println("====================");
//        System.out.printf("%016x: %016x => %.1f%n", vect.xAddr(), MEM.readLong(vect.xAddr()), vect.getX());
//        System.out.printf("%016x: %016x => %.1f%n", vect.yAddr(), MEM.readLong(vect.yAddr()), vect.getY());
//        System.out.printf("%016x: %016x => %.1f%n", vect.zAddr(), MEM.readLong(vect.zAddr()), vect.getZ());
//
//        //vect.movePtr(SIZE );//IndexOutOfBoundsException: index 100 exceeds bound: 100
//
//
//        String condensedShortResults = Stream.of(//
//                //Individual benchmark's
//                BenchAllocation.class//
//                , BenchInitialization.class//
//                , BenchStride.class//
//                , BenchRandomAccess.class
//        )//
//                //Exec the benchmark
//                .map(bench -> new SimpleEntry<Class, Collection<RunResult>>(bench, benchRun(bench)))
//                .map(FlyweightStructJMH::toShortResult)
//                .peek(System.out::println)//print intermediate
//                //gather all bench results to print later in one piece
//                .collect(Collectors.joining("\n"));
//
//
//        System.out.println(condensedShortResults);
//
//    }
//
//
//    private static void dumpMemory(long memAddr, long size) {
//        System.out.print("bytes: " + size);
//        for (long i = 0; i < size; i += Integer.BYTES) {
//            if (i % (Integer.BYTES * 6) == 0) {
//                System.out.println();
//                System.out.printf("%016x: ", memAddr + i);
//            }
//            int b1 = 0xff & MEM.readByte(memAddr + i);
//            int b2 = 0xff & MEM.readByte(memAddr + i + 1);
//            int b3 = 0xff & MEM.readByte(memAddr + i + 2);
//            int b4 = 0xff & MEM.readByte(memAddr + i + 3);
//
//
//            System.out.printf("%02x%02x %02x%02x ", b1, b2, b3, b4);
//        }
//        System.out.println();
//    }
//
//
//    private static Collection<RunResult> benchRun(Class<?> bench) {
//
//        try {
//            return new Runner(new OptionsBuilder()//
//                    .include(".*" + bench.getSimpleName() + ".*")//
//                    .addProfiler(GCProfiler.class)//
//                    .build()).run();
//        } catch (RunnerException e) {
//            throw new RuntimeException(e);
//        }
//
//    }
//
//    private static String toShortResult(Map.Entry<Class, Collection<RunResult>> result) {
//        return toShortResult(result.getKey().getSimpleName(), result.getValue());
//    }
//
//    private static String toShortResult(String benchName, Collection<RunResult> result) {
//
//        ByteArrayOutputStream os = new ByteArrayOutputStream();
//        PrintStream ps = new PrintStream(os);
//        ResultFormatFactory.getInstance(ResultFormatType.TEXT, ps).writeOut(result);
//
//        StringBuilder resultString = new StringBuilder(benchName).append(":\n");
//
//        String prefix = FlyweightStructJMH.class.getSimpleName();
//
//        Arrays.asList(os.toString().split("\n")).stream()
//                .filter(x -> !x.contains(":") || x.contains("gc.alloc.rate.norm"))
//                .map(line -> line.startsWith(prefix) ? line.substring(prefix.length() + 1) : line)
//                .forEach(line -> resultString.append(line).append('\n'));
//
//        return resultString.toString();
//    }
//
//
//    /*--
//    Benchmark                                                   Score  Unit  # Comment
//    BenchAllocation.base                                        19638 us/op  # base
//    BenchAllocation.base:·gc.alloc.rate.norm                 44000023  B/op  #
//    BenchAllocation.array                                        1378 us/op  # we save dclLazyLoader lot of allocations compared to base
//    BenchAllocation.array:·gc.alloc.rate.norm                24000040  B/op  #
//    BenchAllocation.byteBuffHeap                                 1264 us/op  # ByteBuffer.asDoubleBuffer() is internally the same as "array"
//    BenchAllocation.byteBuffHeap:·gc.alloc.rate.norm         24000088  B/op  #
//    BenchAllocation.byteBuffNative                               8659 us/op  #
//    BenchAllocation.byteBuffNative:·gc.alloc.rate.norm            219  B/op  #
//    BenchAllocation.chronical                                   32373 us/op  # requires dclLazyLoader new Object per "Vect3d" resulting in bad perf.
//    BenchAllocation.chronical:·gc.alloc.rate.norm            52000276  B/op  #
//    BenchAllocation.chronicalNested                              1442 us/op  # most similar to byteBuffNative - but fixed size only :(
//    BenchAllocation.chronicalNested:·gc.alloc.rate.norm      24000304  B/op  #
//    BenchAllocation.unsafe                                         14 us/op  # Suspiciously low!!! - uninitialized allocation, automatic (GC) reference cleanup and auto de-allocation is wonky :(
//    BenchAllocation.unsafe:·gc.alloc.rate.norm                     72  B/op  #
//
//    BenchAllocation:
//    Benchmark                                                       Mode  Cnt        Score         Error   Units
//    BenchAllocation.base                                            avgt    5      360,959 ±      21,371   us/op
//    BenchAllocation.base:·gc.alloc.rate.norm                        avgt    5  4400016,156 ±       0,020    B/op
//    BenchAllocation.array                                           avgt    5      151,606 ±      57,536   us/op
//    BenchAllocation.array:·gc.alloc.rate.norm                       avgt    5  2400040,063 ±       0,024    B/op
//    BenchAllocation.byteBuffHeap                                    avgt    5      133,946 ±      24,948   us/op
//    BenchAllocation.byteBuffHeap:·gc.alloc.rate.norm                avgt    5  2400088,059 ±       0,034    B/op
//    BenchAllocation.byteBuffNative                                  avgt    5      813,623 ±    1239,554   us/op
//    BenchAllocation.byteBuffNative:·gc.alloc.rate.norm              avgt    5      216,333 ±       0,488    B/op
//    BenchAllocation.chronical                                       avgt    5     1195,292 ±     525,168   us/op
//    BenchAllocation.chronical:·gc.alloc.rate.norm                   avgt    5  2800264,207 ±     129,676    B/op
//    BenchAllocation.chronicalNested                                 avgt    5       11,631 ±      33,454   us/op
//    BenchAllocation.chronicalNested:·gc.alloc.rate.norm             avgt    5      296,431 ±     141,422    B/op
//    BenchAllocation.managedUnsafe                                   avgt    5        8,487 ±      39,190   us/op
//    BenchAllocation.managedUnsafe:·gc.alloc.rate.norm               avgt    5      344,742 ±     764,078    B/op
//    BenchAllocation.unsafe                                          avgt    5        1,056 ±       0,088   us/op
//    BenchAllocation.unsafe:·gc.alloc.rate.norm                      avgt    5       32,000 ±       0,001    B/op
//     */
//    @BenchmarkMode({Mode.AverageTime})
//    @OutputTimeUnit(TimeUnit.MICROSECONDS)
//    @Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
//    @Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
//    @Fork(1)
//    @State(Scope.Thread)
//    public static class BenchAllocation {
//        @Benchmark
//        public void base(Blackhole b) {
//            //zero's the memory
//            BaseVect3[] vec = newBaseVect3(SIZE);
//            b.consume(vec);
//        }
//
//        @Benchmark
//        public void array(Blackhole b) {
//            ArrayVect3 vec = ArrayVect3.newVectors(SIZE);
//            b.consume(vec);
//        }
//
//        @Benchmark
//        public void byteBuffHeap(Blackhole b) {
//            //zero's the memory
//            ByteBuffVect3 vec = ByteBuffVect3.newVectors(SIZE, false);
//            b.consume(vec);
//        }
//
//        @Benchmark
//        public void byteBuffNative(Blackhole b) {
//            //zero's the memory  :-(
//            ByteBuffVect3 vec = ByteBuffVect3.newVectors(SIZE, true);
//            b.consume(vec);
//        }
//
//        @Benchmark
//        public void unsafe(Blackhole b) {
//            //does not zero the memory! :-)
//            UnsafeVect3 vec = UnsafeVect3.newVectors(SIZE);
//            b.consume(vec);
//            //vec.free();//manual de-allocator
//        }
//
//        @Benchmark
//        public void managedUnsafe(Blackhole b) {
//            //does not zero the memory! :-)
//            ManagedUnsafeVect3 vec = ManagedUnsafeVect3.newVectors(SIZE);
//            b.consume(vec);
//            //auto de-allocator :)
//        }
//
//        @Benchmark
//        public void chronical(Blackhole b) {
//            //does not zero the memory! :-)
//            Vect3[] vec = newChronicalVec3Native(SIZE);
//            b.consume(vec);
//        }
//
//        @Benchmark
//        public void chronicalNested(Blackhole b) {
//            //does not zero the memory! :-)
//            ChronicalVec3Nested vec = newChronicalVec3Nested();//Fixed Size :(
//            b.consume(vec);
//        }
//
//    }
//
//
//    /*--
//
//    Benchmark                                                Mode  Cnt    Score    Error   Units
//    BenchInitialization.base                                 avgt    5  141,346 ± 35,589   us/op
//    BenchInitialization.base:·gc.alloc.rate.norm             avgt    5   88,289 ±  1,986    B/op
//    BenchInitialization.array                                avgt    5   63,694 ± 22,081   us/op
//    BenchInitialization.array:·gc.alloc.rate.norm            avgt    5   10,141 ± 48,919    B/op
//    BenchInitialization.byteBuffHeap                         avgt    5   84,381 ± 19,843   us/op
//    BenchInitialization.byteBuffHeap:·gc.alloc.rate.norm     avgt    5   19,679 ± 37,543    B/op
//    BenchInitialization.byteBuffNative                       avgt    5   81,508 ± 61,337   us/op
//    BenchInitialization.byteBuffNative:·gc.alloc.rate.norm   avgt    5   15,618 ± 44,914    B/op
//    BenchInitialization.chronical                            avgt    5  182,843 ± 32,252   us/op
//    BenchInitialization.chronical:·gc.alloc.rate.norm        avgt    5   88,398 ±  2,801    B/op
//    BenchInitialization.chronicalNested                      avgt    5  274,794 ± 24,981   us/op
//    BenchInitialization.chronicalNested:·gc.alloc.rate.norm  avgt    5    0,558 ±  3,821    B/op
//    BenchInitialization.managedUnsafe                        avgt    5  120,582 ± 47,980   us/op
//    BenchInitialization.managedUnsafe:·gc.alloc.rate.norm    avgt    5   27,147 ± 44,506    B/op
//    BenchInitialization.unsafe                               avgt    5  118,573 ± 49,411   us/op
//    BenchInitialization.unsafe:·gc.alloc.rate.norm           avgt    5   27,095 ± 44,881    B/op
//     */
//
//    @BenchmarkMode({Mode.AverageTime})
//    @OutputTimeUnit(TimeUnit.MICROSECONDS)
//    @Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
//    @Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
//    @Fork(1)
//    @State(Scope.Thread)
//    public static class BenchInitialization {
//
//        @State(Scope.Thread)
//        public static class StateObj {
//            BaseVect3[] baseVect3s = newBaseVect3(SIZE);
//            ArrayVect3 arrayVect3 = ArrayVect3.newVectors(SIZE);
//            ByteBuffVect3 byteBuffVect3 = ByteBuffVect3.newVectors(SIZE, false);
//            ByteBuffVect3 byteBuffVect3Direct = ByteBuffVect3.newVectors(SIZE, true);
//            UnsafeVect3 unsafeVect3Heap = UnsafeVect3.newVectors(SIZE);
//            ManagedUnsafeVect3 managedUnsafeVect3 = ManagedUnsafeVect3.newVectors(SIZE);
//            ChronicalVec3Nested chronicalVec3Nested = newChronicalVec3Nested();//Fixed Size :(
//            Vect3[] chronicalVec3 = newChronicalVec3Native(SIZE);
//
//            @TearDown
//            public void free() {
//                unsafeVect3Heap.free();
//                chronicalVec3Nested.bytesStore().release();
//                //sufficient to release only first - all entries in chronicalVec3[] share the same underlying bytesStore
//                ((Byteable) chronicalVec3[0]).bytesStore().release();
//            }
//        }
//
//        @Benchmark
//        public void base(Blackhole b, StateObj s) {
//            init(s.baseVect3s);
//            b.consume(s.baseVect3s);
//        }
//
//        @Benchmark
//        public void array(Blackhole b, StateObj s) {
//            init(s.arrayVect3);
//            b.consume(s.arrayVect3);
//        }
//
//        @Benchmark
//        public void byteBuffHeap(Blackhole b, StateObj s) {
//            init(s.byteBuffVect3);
//            b.consume(s.byteBuffVect3);
//        }
//
//        @Benchmark
//        public void byteBuffNative(Blackhole b, StateObj s) {
//            init(s.byteBuffVect3Direct);
//            b.consume(s.byteBuffVect3Direct);
//        }
//
//        @Benchmark
//        public void unsafe(Blackhole b, StateObj s) {
//            init(s.unsafeVect3Heap);
//            b.consume(s.unsafeVect3Heap);
//        }
//
//        @Benchmark
//        public void managedUnsafe(Blackhole b, StateObj s) {
//            init(s.managedUnsafeVect3);
//            b.consume(s.managedUnsafeVect3);
//        }
//
//        @Benchmark
//        public void chronical(Blackhole b, StateObj s) {
//            init(s.chronicalVec3);
//            b.consume(s.chronicalVec3);
//        }
//
//        @Benchmark
//        public void chronicalNested(Blackhole b, StateObj s) {
//            init(s.chronicalVec3Nested);
//            b.consume(s.chronicalVec3Nested);
//        }
//
//
//    }
//
//    /*--
//    BenchStride:
//    Benchmark                                        Mode  Cnt     Score    Error   Units
//    BenchStride.base                                 avgt    5   126,786 ± 21,523   us/op
//    BenchStride.base:·gc.alloc.rate.norm             avgt    5    32,986 ± 32,388    B/op
//    BenchStride.array                                avgt    5   347,545 ± 31,922   us/op
//    BenchStride.array:·gc.alloc.rate.norm            avgt    5    48,698 ±  4,735    B/op
//    BenchStride.byteBuffHeap                         avgt    5   436,654 ± 18,851   us/op
//    BenchStride.byteBuffHeap:·gc.alloc.rate.norm     avgt    5    48,896 ±  6,151    B/op
//    BenchStride.byteBuffNative                       avgt    5   415,386 ±  9,646   us/op
//    BenchStride.byteBuffNative:·gc.alloc.rate.norm   avgt    5    48,855 ±  5,877    B/op
//    BenchStride.chronical                            avgt    5   210,140 ± 43,853   us/op
//    BenchStride.chronical:·gc.alloc.rate.norm        avgt    5    24,422 ±  2,875    B/op
//    BenchStride.chronicalNested                      avgt    5  1424,979 ± 58,069   us/op
//    BenchStride.chronicalNested:·gc.alloc.rate.norm  avgt    5    36,295 ± 26,053    B/op
//    BenchStride.managedUnsafe                        avgt    5   361,042 ± 29,052   us/op
//    BenchStride.managedUnsafe:·gc.alloc.rate.norm    avgt    5    56,733 ±  5,022    B/op
//    BenchStride.unsafe                               avgt    5   294,391 ± 83,653   us/op
//    BenchStride.unsafe:·gc.alloc.rate.norm           avgt    5    56,595 ±  4,067    B/op
//     */
//    @BenchmarkMode({Mode.AverageTime})
//    @OutputTimeUnit(TimeUnit.MICROSECONDS)
//    @Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
//    @Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
//    @Fork(1)
//    @State(Scope.Thread)
//    public static class BenchStride {
//
//        @State(Scope.Thread)
//        public static class StateObj {
//            BaseVect3[] baseVect3s = init(newBaseVect3(SIZE));
//            ArrayVect3 arrayVect3 = init(ArrayVect3.newVectors(SIZE));
//            ByteBuffVect3 byteBuffVect3 = init(ByteBuffVect3.newVectors(SIZE, false));
//            ByteBuffVect3 byteBuffVect3Direct = init(ByteBuffVect3.newVectors(SIZE, true));
//            UnsafeVect3 unsafeVect3Heap = init(UnsafeVect3.newVectors(SIZE));
//            ManagedUnsafeVect3 managedUnsafeVect3 = init(ManagedUnsafeVect3.newVectors(SIZE));
//            ChronicalVec3Nested chronicalVec3Nested = init(newChronicalVec3Nested());//Fixed Size :(
//            Vect3[] chronicalVec3 = init(newChronicalVec3Native(SIZE));
//
//            @TearDown
//            public void free() {
//                unsafeVect3Heap.free();
//                chronicalVec3Nested.bytesStore().release();
//                //sufficient to release only first - all entries in chronicalVec3[] share the same underlying bytesStore
//                ((Byteable) chronicalVec3[0]).bytesStore().release();
//            }
//        }
//
//
//        @Benchmark
//        public void base(Blackhole b, StateObj s) {
//            MutableDouble acc = new MutableDouble();
//            forEach(s.baseVect3s, vec -> acc.add(sum(vec)));
//            b.consume(acc);
//        }
//
//
//        @Benchmark
//        public void array(Blackhole b, StateObj s) {
//            MutableDouble acc = new MutableDouble();
//            s.arrayVect3.forEach(vec -> acc.add(sum(vec)));
//            b.consume(acc);
//        }
//
//        @Benchmark
//        public void byteBuffHeap(Blackhole b, StateObj s) {
//            MutableDouble acc = new MutableDouble();
//            s.byteBuffVect3.forEach(vec -> acc.add(sum(vec)));
//            b.consume(acc);
//        }
//
//        @Benchmark
//        public void byteBuffNative(Blackhole b, StateObj s) {
//            MutableDouble acc = new MutableDouble();
//            s.byteBuffVect3Direct.forEach(vec -> acc.add(sum(vec)));
//            b.consume(acc);
//        }
//
//        @Benchmark
//        public void unsafe(Blackhole b, StateObj s) {
//            MutableDouble acc = new MutableDouble();
//            s.unsafeVect3Heap.forEach(vec -> acc.add(sum(vec)));
//            b.consume(acc);
//        }
//
//        @Benchmark
//        public void managedUnsafe(Blackhole b, StateObj s) {
//            MutableDouble acc = new MutableDouble();
//            s.managedUnsafeVect3.forEach(vec -> acc.add(sum(vec)));
//            b.consume(acc);
//        }
//
//        @Benchmark
//        public void chronical(Blackhole b, StateObj s) {
//            MutableDouble acc = new MutableDouble();
//            forEach(s.chronicalVec3, vec -> acc.add(sum(vec)));
//            b.consume(acc);
//        }
//
//        @Benchmark
//        public void chronicalNested(Blackhole b, StateObj s) {
//            MutableDouble acc = new MutableDouble();
//            s.chronicalVec3Nested.forEach(vec -> acc.add(sum(vec)));
//            b.consume(acc);
//        }
//
//    }
//
//    /*--
//    BenchRandomAccess:
//    Benchmark                                              Mode  Cnt    Score     Error   Units
//    BenchRandomAccess.base                                 avgt    5  570,219 ± 377,047   us/op
//    BenchRandomAccess.base:·gc.alloc.rate.norm             avgt    5   25,069 ±   7,105    B/op
//    BenchRandomAccess.array                                avgt    5  347,962 ±  77,102   us/op
//    BenchRandomAccess.array:·gc.alloc.rate.norm            avgt    5   24,692 ±   4,731    B/op
//    BenchRandomAccess.byteBuffHeap                         avgt    5  457,904 ±  16,704   us/op
//    BenchRandomAccess.byteBuffHeap:·gc.alloc.rate.norm     avgt    5   24,947 ±   6,524    B/op
//    BenchRandomAccess.byteBuffNative                       avgt    5  467,718 ±  22,873   us/op
//    BenchRandomAccess.byteBuffNative:·gc.alloc.rate.norm   avgt    5   24,976 ±   6,739    B/op
//    BenchRandomAccess.chronical                            avgt    5  919,837 ±  82,300   us/op
//    BenchRandomAccess.chronical:·gc.alloc.rate.norm        avgt    5   25,849 ±  12,611    B/op
//    BenchRandomAccess.chronicalNested                      avgt    5  620,139 ±  15,669   us/op
//    BenchRandomAccess.chronicalNested:·gc.alloc.rate.norm  avgt    5   25,266 ±   8,682    B/op
//    BenchRandomAccess.managedUnsafe                        avgt    5  303,058 ±  19,042   us/op
//    BenchRandomAccess.managedUnsafe:·gc.alloc.rate.norm    avgt    5   24,636 ±   4,399    B/op
//    BenchRandomAccess.unsafe                               avgt    5  300,604 ±  13,215   us/op
//    BenchRandomAccess.unsafe:·gc.alloc.rate.norm           avgt    5   24,617 ±   4,239    B/op
//     */
//    @BenchmarkMode({Mode.AverageTime})
//    @OutputTimeUnit(TimeUnit.MICROSECONDS)
//    @Warmup(iterations = WARMUP_ITERATIONS, time = 1, timeUnit = TimeUnit.SECONDS)
//    @Measurement(iterations = MEASUREMENT_ITERATIONS, time = 1, timeUnit = TimeUnit.SECONDS)
//    @Fork(FORKS)
//    @State(Scope.Thread)
//    public static class BenchRandomAccess {
//        @State(Scope.Thread)
//        public static class StateObj {
//            BaseVect3[] baseVect3s = init(newBaseVect3(SIZE));
//            ArrayVect3 arrayVect3 = init(ArrayVect3.newVectors(SIZE));
//            ByteBuffVect3 byteBuffVect3 = init(ByteBuffVect3.newVectors(SIZE, false));
//            ByteBuffVect3 byteBuffVect3Direct = init(ByteBuffVect3.newVectors(SIZE, true));
//            UnsafeVect3 unsafeVect3OffHeap = init(UnsafeVect3.newVectors(SIZE));
//            ManagedUnsafeVect3 managedUnsafeVect3 = init(ManagedUnsafeVect3.newVectors(SIZE));
//            ChronicalVec3Nested chronicalVec3Nested = init(newChronicalVec3Nested());//Fixed Size :(
//            Vect3[] chronicalVec3 = init(newChronicalVec3Native(SIZE));
//
//            @TearDown
//            public void free() {
//                unsafeVect3OffHeap.free();
//                chronicalVec3Nested.bytesStore().release();
//                //sufficient to release only first - all entries in chronicalVec3[] share the same underlying bytesStore
//                ((Byteable) chronicalVec3[0]).bytesStore().release();
//            }
//        }
//
//        private static final long SEED = 9223372036854775783L % SIZE;
//
//        private static long getSimplePermutation(long value, long maxValue) {
//            return ((SEED + value) * Integer.MAX_VALUE/*isPrime*/) % maxValue;
//        }
//
//        private interface VectForIndex {
//            Vect3 getVectForIndex(int i);
//        }
//
//        private MutableDouble randStride(VectForIndex vfi) {
//            MutableDouble acc = new MutableDouble();
//            for (int i = 0; i < SIZE; i++) {
//                int randIndex = (int) getSimplePermutation(i, SIZE);
//                acc.add(sum(vfi.getVectForIndex(randIndex)));
//            }
//            return acc;
//        }
//
//        @Benchmark
//        public void base(Blackhole b, StateObj s) {
//            b.consume(randStride(index -> s.baseVect3s[index]));
//        }
//
//        @Benchmark
//        public void array(Blackhole b, StateObj s) {
//            b.consume(randStride(index -> s.arrayVect3.movePtr(index)));
//        }
//
//        @Benchmark
//        public void byteBuffHeap(Blackhole b, StateObj s) {
//            b.consume(randStride(index -> s.byteBuffVect3.movePtr(index)));
//        }
//
//        @Benchmark
//        public void byteBuffNative(Blackhole b, StateObj s) {
//            b.consume(randStride(index -> s.byteBuffVect3Direct.movePtr(index)));
//        }
//
//        @Benchmark
//        public void unsafe(Blackhole b, StateObj s) {
//            b.consume(randStride(index -> s.unsafeVect3OffHeap.movePtr(index)));
//        }
//
//        @Benchmark
//        public void managedUnsafe(Blackhole b, StateObj s) {
//            b.consume(randStride(index -> s.managedUnsafeVect3.movePtr(index)));
//        }
//
//        @Benchmark
//        public void chronical(Blackhole b, StateObj s) {
//            b.consume(randStride(index -> s.chronicalVec3[index]));
//        }
//
//        @Benchmark
//        public void chronicalNested(Blackhole b, StateObj s) {
//            b.consume(randStride(index -> s.chronicalVec3Nested.getVectAt(index)));
//        }
//
//    }
//
//    //##############################
//    // Helpers for init, iterations and "workload"
//    //##############################
//
//    public static double sum(Vect3 v) {
//        return v.getX() + v.getY() + v.getZ();
//    }
//
//
//    public static void initVect3(Vect3 v) {
//        v.set(1, 2, 3);
//    }
//
//
//    public static <T extends InPlaceVect3> T init(T v) {
//        v.forEach(FlyweightStructJMH::initVect3);
//        return v;
//    }
//
//    public static <T extends Vect3> T[] init(T[] v) {
//        Arrays.stream(v).forEach(FlyweightStructJMH::initVect3);
//        return v;
//    }
//
//    public static ChronicalVec3Nested init(ChronicalVec3Nested v) {
//        v.forEach(FlyweightStructJMH::initVect3);
//        return v;
//    }
//
//
//    public static void forEach(Vect3[] x, Consumer<Vect3> c) {
//        for (Vect3 v : x) {
//            c.accept(v);
//        }
//    }
//
//
//    /**
//     * Not so much better then dclLazyLoader normal java heap Vect3[]  :-(
//     *
//     * @param size
//     * @return
//     */
//    public static Vect3[] newChronicalVec3Native(int size) {
//        Vect3 first = Values.newNativeReference(Vect3.class);
//
//        long objSize = ((Byteable) first).maxSize();
//        int memSize = (int) (objSize * size);
//        BytesStore memoryChunk = NativeBytesStore.lazyNativeBytesStoreWithFixedCapacity(memSize);
//
//        ((Byteable) first).bytesStore(memoryChunk, 0, objSize);
//
//        Vect3[] vects = new Vect3[size];
//        vects[0] = first;
//        for (int i = 1; i < size; i++) {
//            Vect3 next = Values.newNativeReference(Vect3.class);
//            ((Byteable) next).bytesStore(memoryChunk, objSize * i, objSize);
//            vects[i] = next;
//        }
//
//        return vects;
//    }
//
//    //Fixed Size :(
//    public static ChronicalVec3Nested newChronicalVec3Nested() {
//        ChronicalVec3Nested vec = Values.newNativeReference(ChronicalVec3Nested.class);
//        long objSize = vec.maxSize();
//        BytesStore memoryChunk = NativeBytesStore.lazyNativeBytesStoreWithFixedCapacity(objSize);
//
//        vec.bytesStore(memoryChunk, 0, objSize);
//        return vec;
//    }
//
//    public static BaseVect3[] newBaseVect3(int size) {
//        BaseVect3[] vect = new BaseVect3[size];
//        for (int i = 0; i < vect.length; i++) {
//            vect[i] = new BaseVect3();
//        }
//        return vect;
//    }
//
//
//    //##############################
//    // Implementations
//    //##############################
//
//
//    interface Vect3 {
//
//        default void set(double x, double y, double z) {
//            setX(x);
//            setY(y);
//            setZ(z);
//        }
//
//        double getX();
//
//        void setX(double x);
//
//        double getY();
//
//        void setY(double y);
//
//        double getZ();
//
//        void setZ(double z);
//    }
//
//    public static class BaseVect3 implements Vect3 {
//
//        private double x, y, z;
//
//
//        public double getX() {
//            return x;
//        }
//
//        public void setX(double x) {
//            this.x = x;
//        }
//
//        public double getY() {
//            return y;
//        }
//
//        public void setY(double y) {
//            this.y = y;
//        }
//
//        public double getZ() {
//            return z;
//        }
//
//        public void setZ(double z) {
//            this.z = z;
//        }
//    }
//
//
//    interface InPlaceVect3 extends Vect3 {
//
//        int getSize();
//
//        InPlaceVect3 movePtr(int index);
//
//        InPlaceVect3 getInstanceAt(int index);
//
//        default void forEach(Consumer<InPlaceVect3> consumer) {
//            InPlaceVect3 v = this.getInstanceAt(0);//local copy
//            for (int i = 0; i < v.getSize(); i++) {
//                v.movePtr(i);
//                consumer.accept(v);
//            }
//        }
//    }
//
//    public static class ArrayVect3 implements InPlaceVect3 {
//
//        //Offsets
//        private static final int X = 0;
//        private static final int Y = 1;
//        private static final int Z = 2;
//
//        private double[] memory; //ptr to memory region
//
//        private final int size;
//        private int offset;
//
//        private ArrayVect3(int size) {
//            this(new double[size * 3], size, 0);
//        }
//
//        private ArrayVect3(double[] memory, int size, int index) {
//            this.memory = memory;
//            this.size = size;
//            this.offset = index;
//        }
//
//        public static ArrayVect3 newVectors(int size) {
//            return new ArrayVect3(size);
//        }
//
//        @Override
//        public int getSize() {
//            return size;
//        }
//
//        public ArrayVect3 movePtr(int index) {
//            if (index >= size)
//                throw new IndexOutOfBoundsException("index " + index + " exceeds bound: " + size);
//            this.offset = index;
//            return this;
//        }
//
//        public ArrayVect3 getInstanceAt(int index) {
//            return new ArrayVect3(memory, size, index);
//        }
//
//        public double getX() {
//            return memory[offset + X];
//        }
//
//        public void setX(double x) {
//            memory[offset + X] = x;
//        }
//
//        public double getY() {
//            return memory[offset + Y];
//        }
//
//        public void setY(double y) {
//            memory[offset + X] = y;
//        }
//
//        public double getZ() {
//            return memory[offset + Z];
//        }
//
//        public void setZ(double z) {
//            memory[offset + Z] = z;
//        }
//    }
//
//    public static class UninitializedArrayVect3 implements InPlaceVect3 {
//
//        //Offsets
//        private static final int X = 0;
//        private static final int Y = 1;
//        private static final int Z = 2;
//
//        private double[] memory; //ptr to memory region
//
//        private final int size;
//        private int offset;
//
//        private UninitializedArrayVect3(int size) {
//            this(new double[size * 3], size, 0);
//        }
//
//        private UninitializedArrayVect3(double[] memory, int size, int index) {
//            this.memory = memory;
//            this.size = size;
//            this.offset = index;
//        }
//
//        public static UninitializedArrayVect3 newVectors(int size) {
//            return new UninitializedArrayVect3(size);
//        }
//
//        @Override
//        public int getSize() {
//            return size;
//        }
//
//        public UninitializedArrayVect3 movePtr(int index) {
//            if (index >= size)
//                throw new IndexOutOfBoundsException("index " + index + " exceeds bound: " + size);
//            this.offset = index;
//            return this;
//        }
//
//        public UninitializedArrayVect3 getInstanceAt(int index) {
//            return new UninitializedArrayVect3(memory, size, index);
//        }
//
//        public double getX() {
//            return memory[offset + X];
//        }
//
//        public void setX(double x) {
//            memory[offset + X] = x;
//        }
//
//        public double getY() {
//            return memory[offset + Y];
//        }
//
//        public void setY(double y) {
//            memory[offset + X] = y;
//        }
//
//        public double getZ() {
//            return memory[offset + Z];
//        }
//
//        public void setZ(double z) {
//            memory[offset + Z] = z;
//        }
//    }
//
//
//    public static class ByteBuffVect3 implements InPlaceVect3 {
//        //Offsets
//        private static final int X = 0;
//        private static final int Y = 1;
//        private static final int Z = 2;
//
//        private DoubleBuffer memory;
//        private int size;
//        private int offset;
//
//        private ByteBuffVect3(DoubleBuffer memory, int size, int index) {
//            this.memory = memory;
//            this.size = size;
//            this.offset = index;
//        }
//
//        public ByteBuffVect3(int size, boolean direct) {
//            this(allocateMemory(size, direct), size, 0);
//        }
//
//        @NotNull
//        private static DoubleBuffer allocateMemory(int size, boolean direct) {
//            if (direct) {
//                return ByteBuffer.allocateDirect(size * 3 * Double.BYTES)
//                        .order(ByteOrder.nativeOrder())
//                        .asDoubleBuffer();
//            } else {
//                return DoubleBuffer.allocate(size * 3);
//            }
//        }
//
//        public static ByteBuffVect3 newVectors(int size, boolean direct) {
//            return new ByteBuffVect3(size, direct);
//        }
//
//        @Override
//        public int getSize() {
//            return size;
//        }
//
//        public ByteBuffVect3 movePtr(int index) {
//            if (index >= size)
//                throw new IndexOutOfBoundsException("index " + index + " exceeds bound: " + size);
//            this.offset = index;
//            return this;
//        }
//
//        public ByteBuffVect3 getInstanceAt(int index) {
//            return new ByteBuffVect3(memory, size, index);
//        }
//
//        @Override
//        public double getX() {
//            return memory.get(offset + X);
//        }
//
//        @Override
//        public void setX(double x) {
//            memory.put(offset + X, x);
//        }
//
//        @Override
//        public double getY() {
//            return memory.get(offset + Y);
//        }
//
//        @Override
//        public void setY(double y) {
//            memory.put(offset + Y, y);
//        }
//
//        @Override
//        public double getZ() {
//            return memory.get(offset + Z);
//        }
//
//        @Override
//        public void setZ(double z) {
//            memory.put(offset + Z, z);
//        }
//    }
//
//    /**
//     * Allocates native memory WITHOUT zeroing the memory!
//     */
//    public static class UnsafeVect3 implements InPlaceVect3 {
//
//        //Offsets
//        private static final long OBJ_SIZE = Double.BYTES * 3L;
//        private static final int X = 0;
//        private static final int Y = X + Double.BYTES;
//        private static final int Z = Y + Double.BYTES;
//
//        //Mem Access helper
//        private static final Memory MEM = OS.memory();
//
//        //ptr's
//        private final long memoryBaseAddr;
//        private final int size;
//        private long objOffset;
//        private final Cleaner.Cleanable cleanable;
//
//
//        private UnsafeVect3(int size) {
//            this(MEM.allocate(size * OBJ_SIZE), size, 0);
//        }
//
//        public UnsafeVect3(long baseAddr, int csize, long offset) {
//            this.memoryBaseAddr = baseAddr;
//            this.size = csize;
//            this.objOffset = offset;
//
//            //be careful not to capture any! instance field or method, or this object wont become phantom reachable
//            // and thus cleaner will not work (and you created dclLazyLoader memory leak)
//            Cleaner cleaner = Cleaner.create();
//            cleanable = cleaner.register(this, () -> { MEM.freeMemory(baseAddr, csize * OBJ_SIZE); System.out.println("cleaned: " + baseAddr);});
//
//        }
//
//        public static UnsafeVect3 newVectors(int size) {
//            return new UnsafeVect3(size);
//        }
//
//
//        private void free() {
//            cleanable.clean();
//        }
//
//        @Override
//        public int getSize() {
//            return size;
//        }
//
//        public UnsafeVect3 movePtr(int index) {
//            this.objOffset = offsetFor(index);
//            return this;
//        }
//
//        private long offsetFor(int index) {
//            if (index >= size)
//                throw new IndexOutOfBoundsException("index " + index + " exceeds bound: " + size);
//            return OBJ_SIZE * index;
//        }
//
//        public UnsafeVect3 getInstanceAt(int index) {
//            return new UnsafeVect3(memoryBaseAddr, size, offsetFor(index));
//        }
//
//        private long objAddr() {
//            return memoryBaseAddr + objOffset;
//        }
//
//        private long xAddr() {
//            return objAddr() + X;
//        }
//
//        private long yAddr() {
//            return objAddr() + Y;
//        }
//
//        private long zAddr() {
//            return objAddr() + Z;
//        }
//
//        @Override
//        public double getX() {
//            return MEM.readDouble(xAddr());
//        }
//
//        @Override
//        public void setX(double x) {
//            MEM.writeDouble(xAddr(), x);
//        }
//
//        @Override
//        public double getY() {
//            return MEM.readDouble(yAddr());
//        }
//
//        @Override
//        public void setY(double y) {
//            MEM.writeDouble(yAddr(), y);
//        }
//
//        @Override
//        public double getZ() {
//            return MEM.readDouble(zAddr());
//        }
//
//        @Override
//        public void setZ(double z) {
//            MEM.writeDouble(zAddr(), z);
//        }
//    }
//
//    /**
//     * Allocates native memory WITHOUT zeroing the memory, but provides automatic "free" of memory with an internal deAllocator
//     */
//    public static class ManagedUnsafeVect3 implements InPlaceVect3 {
//
//        //Offsets
//        private static final long OBJ_SIZE = Double.BYTES * 3L;
//        private static final int X = 0;
//        private static final int Y = X + Double.BYTES;
//        private static final int Z = Y + Double.BYTES;
//
//
//        //ptr's
//        private final NativeBytesStore memory;
//        private final int size;
//        private long objOffset;
//
//
//        private ManagedUnsafeVect3(int size) {
//            this(NativeBytesStore.lazyNativeBytesStoreWithFixedCapacity(size * OBJ_SIZE), size, 0);
//        }
//
//        public ManagedUnsafeVect3(NativeBytesStore memory, int size, long offset) {
//            this.memory = memory;
//            this.size = size;
//            this.objOffset = offset;
//        }
//
//        public static ManagedUnsafeVect3 newVectors(int size) {
//            return new ManagedUnsafeVect3(size);
//        }
//
//
//        private void free() {
//            memory.release();
//        }
//
//        @Override
//        public int getSize() {
//            return size;
//        }
//
//        public ManagedUnsafeVect3 movePtr(int index) {
//            this.objOffset = offsetFor(index);
//            return this;
//        }
//
//        private long offsetFor(int index) {
//            if (index >= size)
//                throw new IndexOutOfBoundsException("index " + index + " exceeds bound: " + size);
//            return OBJ_SIZE * index;
//        }
//
//        public ManagedUnsafeVect3 getInstanceAt(int index) {
//            return new ManagedUnsafeVect3(memory, size, offsetFor(index));
//        }
//
//
//        @Override
//        public double getX() {
//            return memory.readDouble(objOffset + X);
//        }
//
//        @Override
//        public void setX(double x) {
//            memory.writeDouble(objOffset + X, x);
//        }
//
//        @Override
//        public double getY() {
//            return memory.readDouble(objOffset + Y);
//        }
//
//        @Override
//        public void setY(double y) {
//            memory.writeDouble(objOffset + Y, y);
//        }
//
//        @Override
//        public double getZ() {
//            return memory.readDouble(objOffset + Z);
//        }
//
//        @Override
//        public void setZ(double z) {
//            memory.writeDouble(objOffset + Z, z);
//        }
//    }
//
//    /**
//     * No configurable sizing :(
//     */
//    public interface ChronicalVec3Nested extends Byteable {
//
//        @Array(length = SIZE)
//        Vect3 getVectAt(int x);
//
//        void setVectAt(int x, Vect3 v);
//
//
//        default void forEach(Consumer<Vect3> consumer) {
//            for (int i = 0; i < SIZE; i++) {
//                consumer.accept(getVectAt(i));
//            }
//        }
//    }
//
//}
