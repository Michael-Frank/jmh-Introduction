package de.frank.jmh.basic;

import com.sun.istack.internal.NotNull;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/*--
execution time in MICROSECONDS/ops - lower is Better
singleT_atomicReference                  4204 us/op
singleT_atomicReferenceFieldUpdater      3939 us/op

gc.alloc.rate.normalize B/ops - lower is Better
singleT_atomicReference:·             6291141 B/op
singleT_atomicReferenceFieldUpdater   5847456 B/op



Benchmark                                                                                         Mode  Cnt        Score         Error   Units
AtomicReferenceFieldUpdater.singleT_atomicReference                                               avgt   10     4204,741 ±     185,755   us/op
AtomicReferenceFieldUpdater.singleT_atomicReference:·gc.alloc.rate                                avgt   10     3812,255 ±     140,341  MB/sec
AtomicReferenceFieldUpdater.singleT_atomicReference:·gc.alloc.rate.norm                           avgt   10  6291141,677 ±    3795,721    B/op
AtomicReferenceFieldUpdater.singleT_atomicReference:·gc.churn.PS_Eden_Space                       avgt   10     3789,629 ±     651,778  MB/sec
AtomicReferenceFieldUpdater.singleT_atomicReference:·gc.churn.PS_Eden_Space.norm                  avgt   10  6252094,492 ± 1019694,053    B/op
AtomicReferenceFieldUpdater.singleT_atomicReference:·gc.churn.PS_Survivor_Space                   avgt   10        4,129 ±       3,883  MB/sec
AtomicReferenceFieldUpdater.singleT_atomicReference:·gc.churn.PS_Survivor_Space.norm              avgt   10     6826,102 ±    6343,364    B/op
AtomicReferenceFieldUpdater.singleT_atomicReference:·gc.count                                     avgt   10       44,000                counts
AtomicReferenceFieldUpdater.singleT_atomicReference:·gc.time                                      avgt   10      292,000                    ms
AtomicReferenceFieldUpdater.singleT_atomicReferenceFieldUpdater                                   avgt   10     3939,800 ±      84,068   us/op
AtomicReferenceFieldUpdater.singleT_atomicReferenceFieldUpdater:·gc.alloc.rate                    avgt   10     3791,473 ±      77,942  MB/sec
AtomicReferenceFieldUpdater.singleT_atomicReferenceFieldUpdater:·gc.alloc.rate.norm               avgt   10  5847456,716 ±    3042,556    B/op
AtomicReferenceFieldUpdater.singleT_atomicReferenceFieldUpdater:·gc.churn.PS_Eden_Space           avgt   10     3791,846 ±     671,601  MB/sec
AtomicReferenceFieldUpdater.singleT_atomicReferenceFieldUpdater:·gc.churn.PS_Eden_Space.norm      avgt   10  5850311,467 ± 1061732,279    B/op
AtomicReferenceFieldUpdater.singleT_atomicReferenceFieldUpdater:·gc.churn.PS_Survivor_Space       avgt   10        4,454 ±       2,937  MB/sec
AtomicReferenceFieldUpdater.singleT_atomicReferenceFieldUpdater:·gc.churn.PS_Survivor_Space.norm  avgt   10     6867,186 ±    4532,727    B/op
AtomicReferenceFieldUpdater.singleT_atomicReferenceFieldUpdater:·gc.count                         avgt   10       44,000                counts
AtomicReferenceFieldUpdater.singleT_atomicReferenceFieldUpdater:·gc.time                          avgt   10      198,000                    ms
 */
@State(Scope.Thread)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Threads(4)
public class AtomicReferenceFieldUpdater {

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()//
                .include(".*" + AtomicReferenceFieldUpdater.class.getSimpleName() + ".*")//
                .addProfiler(GCProfiler.class)//
//                .param("dataSize", DATA_SIZE_PARAMS)
//                .resultFormat(ResultFormatType.JSON)
//                .result(format("%s_%s.json",//
//                        new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(System.currentTimeMillis()),//
//                        ListToArrayJMH.class.getSimpleName()))
                .build();
        new Runner(opt).run();
    }

    @State(Scope.Benchmark)
    public static class AtomicFieldUpdaterState {
       final SampleNode atomicFiledUpdaterSampleNode = new SampleNode("ROOT");
    }

    @State(Scope.Benchmark)
    public static class AtomicRefState {
        final AtomicRefSampleNode atomicRefSampleNode = new AtomicRefSampleNode("ROOT");
    }

    @Benchmark
    public SampleNodeIF singleT_atomicReferenceFieldUpdater() {
        return generateGraph(new SampleNode("ROOT"));
    }

    @Benchmark
    public SampleNodeIF singleT_atomicReference() {
        return generateGraph(new AtomicRefSampleNode("ROOT"));
    }

//    @Benchmark
//    public SampleNodeIF multiT_atomicReferenceFieldUpdater(AtomicFieldUpdaterState s) {
//        ThreadLocalRandom r = ThreadLocalRandom.current();
//        appendToGraph(generateBranch(r), s.atomicFiledUpdaterSampleNode);
//        return s.atomicFiledUpdaterSampleNode;
//
//    }
//
//    @Benchmark
//    public SampleNodeIF multiT_atomicReference(AtomicRefState s) {
//        ThreadLocalRandom r = ThreadLocalRandom.current();
//        appendToGraph(generateBranch(r), s.atomicRefSampleNode);
//
//        return s.atomicRefSampleNode;
//    }

    public <S extends SampleNodeIF> S generateGraph(S root) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < 1000; i++) {
            String[] branch = generateBranch(r);
            appendToGraph(branch, root);
        }
        return root;
    }

    public static final String [] BRANCHNAMES = new String[20];
    static{
        for (int i = 0; i <BRANCHNAMES.length; i++) {
            BRANCHNAMES[i]=i+"";
        }
    }

    private String[] generateBranch(ThreadLocalRandom r) {
        String[] branch = new String[r.nextInt(10, 50)];
        for (int i = 0; i < branch.length; i++) {
            branch[i] = BRANCHNAMES[r.nextInt(BRANCHNAMES.length)];
        }
        return branch;
    }


    public <S extends SampleNodeIF> void  appendToGraph(String[] oneBranch, S root) {
        SampleNodeIF current = root;
        for (String nodeName : oneBranch) {
            current = current.getOrCreateChildNode(nodeName);
            current.incrementAndGetInvocationCount();
        }
    }


    public interface SampleNodeIF<N extends SampleNodeIF> {
        String getName();

        Map<String, N> getChildren();

        long getInvocationCount();

        long incrementAndGetInvocationCount();

        N getOrCreateChildNode(String name);
    }

    public static class SampleNode implements SampleNodeIF<SampleNode> {

        @NotNull
        private final String name; // FCQN.methodName - is unique for each node
        private AtomicLong invocations = new AtomicLong();
        // save memory on leaf nodes by not creating a children map until required
        private volatile ConcurrentHashMap<String, SampleNode> children = null;

        private static final java.util.concurrent.atomic.AtomicReferenceFieldUpdater<SampleNode, ConcurrentHashMap> CHILDREN_REF_UPDATER
                = java.util.concurrent.atomic.AtomicReferenceFieldUpdater.newUpdater(SampleNode.class, ConcurrentHashMap.class, "children");

        /**
         * FCQN.methodName - must be unique for each node!
         *
         * @param name FCQN.methodName
         */
        SampleNode(String name) {
            Objects.requireNonNull(name, "name is required");
            this.name = name;
        }


        /**
         * Name = fcqn.methodName - must be unique for each node!
         *
         * @return name
         */
        @Override
        public String getName() {
            return name;
        }

        /**
         * child nodes or null if this is a leaf node
         *
         * @return child nodes or null if this is a leaf node
         */
        @Override
        public Map<String, SampleNode> getChildren() {
            return children;
        }

        /**
         * number of recorded invocations for this sample
         *
         * @return number of recorded invocations for this sample
         */
        @Override
        public long getInvocationCount() {
            return invocations.get();
        }

        /**
         * increment and get the invocation counter
         *
         * @return current invocation count
         */
        @Override
        public long incrementAndGetInvocationCount() {
            return invocations.incrementAndGet();
        }

        /**
         * Will get a ChildNode with provided name. If this parent node not yet has this
         * child node, it will be crated
         *
         * @param name
         * @return
         */
        @Override
        public SampleNode getOrCreateChildNode(String name) {
            ConcurrentHashMap<String, SampleNode> theChildren = getOrInitChildren();
            return theChildren.computeIfAbsent(name, SampleNode::new);
        }

        /**
         * ConcurrentHashMap has high memory consumption - this code delays the
         * initialization of the 'children' filed. Leafs don't have children mapping -
         * if that changes we need to initialize On NodeCreation we assume we are a Leaf
         *
         * @return
         */
        private ConcurrentHashMap<String, SampleNode> getOrInitChildren() {
            ConcurrentHashMap<String, SampleNode> theChildren = this.children;

            if (theChildren == null) {

                // optimistically assume we have not that many children and usually only ONE
                // thread modifying the tree structure
                theChildren = new ConcurrentHashMap<>(2, 0.7f, 1);
                if (!CHILDREN_REF_UPDATER.compareAndSet(this, null, theChildren)) {
                    // if we fail to update the reference, another thread was faster -> use his
                    // instance
                    theChildren = this.children;
                }
            }
            return theChildren;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            SampleNode node = (SampleNode) o;
            // children and samples must NOT be considered
            // name is unique for each node
            return Objects.equals(name, node.name);
        }

        @Override
        public int hashCode() {
            // children and samples must NOT be considered
            // name is unique for each node
            return name.hashCode();
        }

        @Override
        public String toString() {
            return "SampleNode{" + "name='" + name + '\'' + ", invocations=" + invocations + ", children=" + children + '}';
        }

    }


    public static class AtomicRefSampleNode implements SampleNodeIF<AtomicRefSampleNode> {

        @NotNull
        private final String name; // FCQN.methodName - is unique for each node
        private AtomicLong invocations = new AtomicLong();
        // save memory on leaf nodes by not creating a children map until required
        private AtomicReference<ConcurrentHashMap<String, AtomicRefSampleNode>> children = new AtomicReference<>();


        /**
         * FCQN.methodName - must be unique for each node!
         *
         * @param name FCQN.methodName
         */
        public AtomicRefSampleNode(String name) {
            Objects.requireNonNull(name, "name is required");
            this.name = name;
        }

        /**
         * Name = fcqn.methodName - must be unique for each node!
         *
         * @return name
         */
        public String getName() {
            return name;
        }

        /**
         * child nodes or null if this is a leaf node
         *
         * @return child nodes or null if this is a leaf node
         */
        public Map<String, AtomicRefSampleNode> getChildren() {
            return children.get();
        }

        /**
         * number of recorded invocations for this sample
         *
         * @return number of recorded invocations for this sample
         */
        public long getInvocationCount() {
            return invocations.get();
        }

        /**
         * increment and get the invocation counter
         *
         * @return current invocation count
         */
        public long incrementAndGetInvocationCount() {
            return invocations.incrementAndGet();
        }

        /**
         * Will get a ChildNode with provided name. If this parent node not yet has this
         * child node, it will be crated
         *
         * @param name
         * @return
         */
        public AtomicRefSampleNode getOrCreateChildNode(String name) {
            ConcurrentHashMap<String, AtomicRefSampleNode> theChildren = getOrInitChildren();
            return theChildren.computeIfAbsent(name, AtomicRefSampleNode::new);
        }

        /**
         * ConcurrentHashMap has high memory consumption - this code delays the
         * initialization of the 'children' filed. Leafs don't have children mapping -
         * if that changes we need to initialize On NodeCreation we assume we are a Leaf
         *
         * @return
         */
        private ConcurrentHashMap<String, AtomicRefSampleNode> getOrInitChildren() {
            ConcurrentHashMap<String, AtomicRefSampleNode> theChildren = this.children.get();

            if (theChildren == null) {

                // optimistically assume we have not that many children and usually only ONE
                // thread modifying the tree structure
                theChildren = new ConcurrentHashMap<>(2, 0.7f, 1);
                if (!children.compareAndSet(null, theChildren)) {
                    // if we fail to update the reference, another thread was faster -> use his
                    // instance
                    theChildren = this.children.get();
                }
            }
            return theChildren;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            SampleNode node = (SampleNode) o;
            // children and samples must NOT be considered
            // name is unique for each node
            return Objects.equals(name, node.name);
        }

        @Override
        public int hashCode() {
            // children and samples must NOT be considered
            // name is unique for each node
            return name.hashCode();
        }

        @Override
        public String toString() {
            return "SampleNode{" + "name='" + name + '\'' + ", invocations=" + invocations + ", children=" + children + '}';
        }

    }

}
