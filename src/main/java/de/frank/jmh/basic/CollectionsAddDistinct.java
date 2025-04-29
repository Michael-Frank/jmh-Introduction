package de.frank.jmh.basic;

import de.frank.jmh.util.BenchmarkFormatter;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/*--
We test: ArrayList vs HashSets "addIfNotContains" usecase
Text book says: Lists are O(n) and therefore are inferior to Sets when a "contains()" is needed, or even worse, an "addIfNotContains" is required.

However:
  - SMALL Lists are awesome and FASTER than Set's if we stay <=10 elements,
  - equal till 20 elements
  - arguably acceptable till 100 elements depending on perf requirements (but at 100 you will have a 5x perf hit)
Small lists are also very common in reality - not every data structure has a Lists with hundreds or thousands of elements.

Conclusion: it depends on your specify data size if a ArrayList is preferable over a HashSet for a "contains" or even a "addIfNotContains" usecase.

LOWER IS BETTER
Units: ns/op |  list/set_size->    2   10   30   100  1000
arrayList_AddExisting              4    8   20    50   631
arrayList_AddNonExisting           7   13   30   117  1401
hashSet_AddExisting               16   16   17    17    18
hashSet_AddNonExisting            13   10   12    12    12
linkedHashSet_AddExisting         14   10   15     9    11
linkedHashSet_AddNonExisting      10   11   11    12    13

Benchmark                                            (size)  Mode  Cnt     Score     Error  Units
CollectionsAddDistinct.arrayList_AddExisting              2  avgt   10     3,635 ±   0,822  ns/op
CollectionsAddDistinct.arrayList_AddExisting             10  avgt   10     8,482 ±   2,447  ns/op
CollectionsAddDistinct.arrayList_AddExisting             30  avgt   10    20,125 ±   4,148  ns/op
CollectionsAddDistinct.arrayList_AddExisting            100  avgt   10    50,381 ±  10,618  ns/op
CollectionsAddDistinct.arrayList_AddExisting           1000  avgt   10   630,911 ± 310,855  ns/op
CollectionsAddDistinct.arrayList_AddNonExisting           2  avgt   10     7,092 ±   1,139  ns/op
CollectionsAddDistinct.arrayList_AddNonExisting          10  avgt   10    13,084 ±   0,477  ns/op
CollectionsAddDistinct.arrayList_AddNonExisting          30  avgt   10    29,620 ±   2,749  ns/op
CollectionsAddDistinct.arrayList_AddNonExisting         100  avgt   10   116,978 ±  14,567  ns/op
CollectionsAddDistinct.arrayList_AddNonExisting        1000  avgt   10  1400,730 ± 332,343  ns/op
CollectionsAddDistinct.hashSet_AddExisting                2  avgt   10    16,135 ±   0,495  ns/op
CollectionsAddDistinct.hashSet_AddExisting               10  avgt   10    16,440 ±   0,920  ns/op
CollectionsAddDistinct.hashSet_AddExisting               30  avgt   10    16,737 ±   0,714  ns/op
CollectionsAddDistinct.hashSet_AddExisting              100  avgt   10    16,821 ±   1,227  ns/op
CollectionsAddDistinct.hashSet_AddExisting             1000  avgt   10    17,907 ±   0,579  ns/op
CollectionsAddDistinct.hashSet_AddNonExisting             2  avgt   10    13,148 ±   3,878  ns/op
CollectionsAddDistinct.hashSet_AddNonExisting            10  avgt   10     9,652 ±   1,048  ns/op
CollectionsAddDistinct.hashSet_AddNonExisting            30  avgt   10    11,603 ±   3,280  ns/op
CollectionsAddDistinct.hashSet_AddNonExisting           100  avgt   10    12,134 ±   7,965  ns/op
CollectionsAddDistinct.hashSet_AddNonExisting          1000  avgt   10    11,615 ±   2,007  ns/op
CollectionsAddDistinct.linkedHashSet_AddExisting          2  avgt   10    14,052 ±   0,709  ns/op
CollectionsAddDistinct.linkedHashSet_AddExisting         10  avgt   10     9,528 ±   0,711  ns/op
CollectionsAddDistinct.linkedHashSet_AddExisting         30  avgt   10    14,590 ±   1,265  ns/op
CollectionsAddDistinct.linkedHashSet_AddExisting        100  avgt   10     9,261 ±   0,360  ns/op
CollectionsAddDistinct.linkedHashSet_AddExisting       1000  avgt   10    10,766 ±   0,356  ns/op
CollectionsAddDistinct.linkedHashSet_AddNonExisting       2  avgt   10    10,110 ±   0,810  ns/op
CollectionsAddDistinct.linkedHashSet_AddNonExisting      10  avgt   10    11,086 ±   0,441  ns/op
CollectionsAddDistinct.linkedHashSet_AddNonExisting      30  avgt   10    11,240 ±   1,868  ns/op
CollectionsAddDistinct.linkedHashSet_AddNonExisting     100  avgt   10    11,726 ±   0,506  ns/op
CollectionsAddDistinct.linkedHashSet_AddNonExisting    1000  avgt   10    13,098 ±   0,439  ns/op


 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
public class CollectionsAddDistinct {


    @State(Scope.Thread)
    public static class MyState {
        @Param(value = {"2", "10", "30", "100", "1000"})
        public long size = 10000;

        private final Set<Employee> employeeSet = new HashSet<>();
        private final Set<Employee> employeeLinkedSet = new LinkedHashSet<>();
        private final List<Employee> employeeList = new ArrayList<>();


        private Employee employeeInList = new Employee(1L, "Nobody");
        private Employee employeeNotInList = new Employee(2L, "Nobody");

        @Setup(Level.Trial)
        public void setUp() {
            long breakpoint = size / 2;
            for (long i = 0; i < breakpoint; i++) {
                Employee e = new Employee(i + 3, "John");
                employeeSet.add(e);
                employeeList.add(e);
                employeeLinkedSet.add(e);
            }
            employeeList.add(employeeInList);
            employeeSet.add(employeeInList);
            employeeLinkedSet.add(employeeInList);
            for (long i = breakpoint; i < size; i++) {
                Employee e = new Employee(i + 3, "John");
                employeeSet.add(e);
                employeeList.add(e);
                employeeLinkedSet.add(e);
            }

        }

        @Setup(Level.Iteration)
        public void reset() {
            employeeList.remove(employeeNotInList);
            employeeSet.remove(employeeNotInList);
            employeeLinkedSet.remove(employeeNotInList);
        }

    }


    @Benchmark
    public boolean arrayList_AddExisting(MyState state) {
        if (!state.employeeList.contains(state.employeeInList)) {
            return state.employeeList.add(state.employeeInList);
        }
        return false;
    }

    @Benchmark
    public boolean arrayList_AddNonExisting(MyState state) {
        if (!state.employeeList.contains(state.employeeNotInList)) {
            return state.employeeList.add(state.employeeNotInList);
        }
        return false;
    }


    @Benchmark
    public boolean hashSet_AddExisting(MyState state) {
        return state.employeeSet.add(state.employeeInList);
    }

    @Benchmark
    public boolean hashSet_AddNonExisting(MyState state) {
        return state.employeeSet.add(state.employeeNotInList);
    }

    @Benchmark
    public boolean linkedHashSet_AddExisting(MyState state) {
        return state.employeeSet.add(state.employeeInList);
    }

    @Benchmark
    public boolean linkedHashSet_AddNonExisting(MyState state) {
        return state.employeeSet.add(state.employeeNotInList);
    }

    public static void main(String[] args) throws Exception {
        Options options = new OptionsBuilder()
                .include(CollectionsAddDistinct.class.getSimpleName())
                .threads(1)
                .forks(1)
                .shouldFailOnError(true)
                .shouldDoGC(true)
                .jvmArgs("-server")
                .build();
        var res = new Runner(options).run();
        BenchmarkFormatter.displayAsMatrix(res, "size");
    }

    public static class Employee {

        private Long id;
        private String name;

        public Employee(Long id, String name) {
            this.name = name;
            this.id = id;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Employee employee = (Employee) o;

            if (!id.equals(employee.id)) return false;
            return name.equals(employee.name);

        }

        @Override
        public int hashCode() {
            int result = id.hashCode();
            result = 31 * result + name.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "Employee{" +
                   "id=" + id +
                   ", name='" + name + '\'' +
                   '}';
        }
    }
}