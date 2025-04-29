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

/**
 * We test: ArrayList vs HashSets "addIfNotContains" usecase
 * Text book says: Lists are O(n) and therefore are inferior to Sets when a "contains()" is needed, or even worse, an "addIfNotContains" is required.
 * <p>
 * For very small lists they are as fast as Sets.
 * However, seldomly a pure "contains()" is required. maybe see {@linkplain CollectionsAddDistinct} for a more complex usecase.
 */
/*--
LOWER IS BETTER
Units: ns/op  | list/set-size-->  2   10  100   1000  10000
arrayListContainsMedian           4    7   43    506   9734
arrayListContainsWorst            5   12   80    975  20007
hashSetContains                   3    4    3      3      5
linkedHashSetContains             3    4    3      3      5

Benchmark                                    (size)  Mode  Cnt      Score      Error  Units
CollectionsContains.arrayListContainsMedian       2  avgt   10      3,649 ±    2,383  ns/op
CollectionsContains.arrayListContainsMedian      10  avgt   10      6,680 ±    0,732  ns/op
CollectionsContains.arrayListContainsMedian     100  avgt   10     43,350 ±    3,647  ns/op
CollectionsContains.arrayListContainsMedian    1000  avgt   10    506,004 ±   37,858  ns/op
CollectionsContains.arrayListContainsMedian   10000  avgt   10   9733,570 ±  649,353  ns/op
CollectionsContains.arrayListContainsWorst        2  avgt   10      5,362 ±    0,675  ns/op
CollectionsContains.arrayListContainsWorst       10  avgt   10     11,561 ±    1,414  ns/op
CollectionsContains.arrayListContainsWorst      100  avgt   10     80,252 ±    8,125  ns/op
CollectionsContains.arrayListContainsWorst     1000  avgt   10    974,959 ±   56,290  ns/op
CollectionsContains.arrayListContainsWorst    10000  avgt   10  20006,730 ± 1089,205  ns/op
CollectionsContains.hashSetContains               2  avgt   10      2,770 ±    0,263  ns/op
CollectionsContains.hashSetContains              10  avgt   10      3,932 ±    0,500  ns/op
CollectionsContains.hashSetContains             100  avgt   10      2,847 ±    0,226  ns/op
CollectionsContains.hashSetContains            1000  avgt   10      2,769 ±    0,182  ns/op
CollectionsContains.hashSetContains           10000  avgt   10      4,738 ±    0,459  ns/op
CollectionsContains.linkedHashSetContains         2  avgt   10      2,868 ±    0,258  ns/op
CollectionsContains.linkedHashSetContains        10  avgt   10      3,922 ±    0,253  ns/op
CollectionsContains.linkedHashSetContains       100  avgt   10      2,881 ±    0,201  ns/op
CollectionsContains.linkedHashSetContains      1000  avgt   10      2,875 ±    0,269  ns/op
CollectionsContains.linkedHashSetContains     10000  avgt   10      4,677 ±    0,475  ns/op


*/
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
public class CollectionsContains {


    @State(Scope.Thread)
    public static class MyState {
        @Param(value = {"2", "10", "100", "1000", "10000"})
        public long size = 10000;

        private final Set<Employee> employeeSet = new HashSet<>();
        private final Set<Employee> employeeLinkedSet = new LinkedHashSet<>();
        private final List<Employee> employeeList = new ArrayList<>();

        private Employee employeeWorstCase = new Employee(1L, "Harry");
        private Employee employeeWorstMedianCase = new Employee(2L, "Sepp");

        @Setup(Level.Trial)
        public void setUp() {
            long breakpoint = size / 2;
            for (long i = 0; i < breakpoint; i++) {
                Employee e = new Employee(i + 3, "John");
                employeeSet.add(e);
                employeeList.add(e);
                employeeLinkedSet.add(e);
            }
            employeeList.add(employeeWorstMedianCase);
            employeeSet.add(employeeWorstMedianCase);
            employeeLinkedSet.add(employeeWorstMedianCase);
            for (long i = breakpoint; i < size; i++) {
                Employee e = new Employee(i + 3, "John");
                employeeSet.add(e);
                employeeList.add(e);
                employeeLinkedSet.add(e);
            }
            employeeList.add(employeeWorstCase);
            employeeSet.add(employeeWorstCase);
            employeeLinkedSet.add(employeeWorstCase);
        }

    }

    @Benchmark
    public boolean arrayListContainsWorst(MyState state) {
        return state.employeeList.contains(state.employeeWorstCase);
    }

    @Benchmark
    public boolean arrayListContainsMedian(MyState state) {
        return state.employeeList.contains(state.employeeWorstMedianCase);
    }

    @Benchmark
    public boolean hashSetContains(MyState state) {
        return state.employeeSet.contains(state.employeeWorstMedianCase);
    }

    @Benchmark
    public boolean linkedHashSetContains(MyState state) {
        return state.employeeSet.contains(state.employeeWorstMedianCase);
    }


    public static void main(String[] args) throws Exception {
        Options options = new OptionsBuilder()
                .include(CollectionsContains.class.getSimpleName())
                .threads(1)
                .forks(1)
                .shouldFailOnError(true)
                .shouldDoGC(true)
                .jvmArgs("-server").build();
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