package de.frank.jmh.algorithms;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;


/**
 * Demonstrator - Naive implementation of a MarkovChain with two basic optimizations to show some more features of JMH
 *
 * @author Michael Frank
 * @version 1.0 13.05.2018
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Threads(1)
@Fork(3)//demo=1; normally run with fork's >= 3
@Warmup(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@State(Scope.Thread)//<-very important this time
public class SimpleMarkovChainBenchmarkJMH {

    @Param({"10", "1000", "10000"})
    private int numberOfWords;

    //shared buffer - we dont want to measure the noise of crating new buffers
    private StringBuilder sb = new StringBuilder(1014);

    private static MarkovChain markov = MarkovChain.fromInput(
            "A rainbow is a meteorological phenomenon that is caused by reflection, refraction and dispersion of light in water droplets resulting in a spectrum of light appearing in the sky. It takes the form of a multicoloured arc. Rainbows caused by sunlight always appear in the section of sky directly opposite the sun.\n" +
                    "Rainbows can be full circles; however, the average observer sees only an arc formed by illuminated droplets above the ground,[1] and centred on a line from the sun to the observer's eye.\n" +
                    "In a primary rainbow, the arc shows red on the outer part and violet on the inner side. This rainbow is caused by light being refracted when entering a droplet of water, then reflected inside on the back of the droplet and refracted again when leaving it.\n" +
                    "In a double rainbow, a second arc is seen outside the primary arc, and has the order of its colours reversed, with red on the inner side of the arc.\n" +
                    "A rainbow is not located at a specific distance from the observer, but comes from an optical illusion caused by any water droplets viewed from a certain angle relative to a light source. Thus, a rainbow is not an object and cannot be physically approached. Indeed, it is impossible for an observer to see a rainbow from water droplets at any angle other than the customary one of 42 degrees from the direction opposite the light source. Even if an observer sees another observer who seems \"under\" or \"at the end of\" a rainbow, the second observer will see a different rainbow—farther off—at the same angle as seen by the first observer.\n" +
                    "Rainbows span a continuous spectrum of colours. Any distinct bands perceived are an artefact of human colour vision, and no banding of any type is seen in a black-and-white photo of a rainbow, only a smooth gradation of intensity to a maximum, then fading towards the other side. For colours seen by the human eye, the most commonly cited and remembered sequence is Newton's sevenfold red, orange, yellow, green, blue, indigo and violet,[2][3] remembered by the mnemonic, Richard Of York Gave Battle In Vain (ROYGBIV).\n" +
                    "Rainbows can be caused by many forms of airborne water. These include not only rain, but also mist, spray, and airborne dew.\n" +
                    "Rainbows can be observed whenever there are water drops in the air and sunlight shining from behind the observer at a low altitude angle. Because of this, rainbows are usually seen in the western sky during the morning and in the eastern sky during the early evening. The most spectacular rainbow displays happen when half the sky is still dark with raining clouds and the observer is at a spot with clear sky in the direction of the sun. The result is a luminous rainbow that contrasts with the darkened background. During such good visibility conditions, the larger but fainter secondary rainbow is often visible. It appears about 10° outside of the primary rainbow, with inverse order of colours.\n" +
                    "The rainbow effect is also commonly seen near waterfalls or fountains. In addition, the effect can be artificially created by dispersing water droplets into the air during a sunny day. Rarely, a moonbow, lunar rainbow or nighttime rainbow, can be seen on strongly moonlit nights. As human visual perception for colour is poor in low light, moonbows are often perceived to be white.[4]\n" +
                    "It is difficult to photograph the complete semicircle of a rainbow in one frame, as this would require an angle of view of 84°. For a 35 mm camera, a wide-angle lens with a focal length of 19 mm or less would be required. Now that software for stitching several images into a panorama is available, images of the entire arc and even secondary arcs can be created fairly easily from a series of overlapping frames.\n" +
                    "From above the earth such as in an airplane, it is sometimes possible to see a rainbow as a full circle. This phenomenon can be confused with the glory phenomenon, but a glory is usually much smaller, covering only 5–20°.\n" +
                    "The sky inside a primary rainbow is brighter than the sky outside of the bow. This is because each raindrop is a sphere and it scatters light over an entire circular disc in the sky. The radius of the disc depends on the wavelength of light, with red light being scattered over a larger angle than blue light. Over most of the disc, scattered light at all wavelengths overlaps, resulting in white light which brightens the sky. At the edge, the wavelength dependence of the scattering gives rise to the rainbow.\n" +
                    "Light of primary rainbow arc is 96% polarised tangential to the arch.[6] Light of second arc is 90% polarised.\n" +
                    "When sunlight encounters a raindrop, part is reflected but part enters, being refracted at the surface of the raindrop. When this light hits the back of the drop, some of it is reflected off the back. When the internally reflected light reaches the surface again, once more some is internally reflected and some is refracted as it exits the drop. (The light that reflects off the drop, exits from the back, or continues to bounce around inside the drop after the second encounter with the surface, is not relevant to the formation of the primary rainbow.) The overall effect is that part of the incoming light is reflected back over the range of 0° to 42°, with the most intense light at 42°.[18] This angle is independent of the size of the drop, but does depend on its refractive index. Seawater has a higher refractive index than rain water, so the radius of a \"rainbow\" in sea spray is smaller than a true rainbow. This is visible to the naked eye by a misalignment of these bows.[19]\n" +
                    "The reason the returning light is most intense at about 42° is that this is a turning point – light hitting the outermost ring of the drop gets returned at less than 42°, as does the light hitting the drop nearer to its centre. There is a circular band of light that all gets returned right around 42°. If the sun were a laser emitting parallel, monochromatic rays, then the luminance (brightness) of the bow would tend toward infinity at this angle (ignoring interference effects). (See Caustic (optics).) But since the sun's luminance is finite and its rays are not all parallel (it covers about half a degree of the sky) the luminance does not go to infinity. Furthermore, the amount by which light is refracted depends upon its wavelength, and hence its colour. This effect is called dispersion. Blue light (shorter wavelength) is refracted at a greater angle than red light, but due to the reflection of light rays from the back of the droplet, the blue light emerges from the droplet at a smaller angle to the original incident white light ray than the red light. Due to this angle, blue is seen on the inside of the arc of the primary rainbow, and red on the outside. The result of this is not only to give different colours to different parts of the rainbow, but also to diminish the brightness. (A \"rainbow\" formed by droplets of a liquid with no dispersion would be white, but brighter than a normal rainbow.)\n" +
                    "The light at the back of the raindrop does not undergo total internal reflection, and some light does emerge from the back. However, light coming out the back of the raindrop does not create a rainbow between the observer and the sun because spectra emitted from the back of the raindrop do not have a maximum of intensity, as the other visible rainbows do, and thus the colours blend together rather than forming a rainbow.\n" +
                    "A rainbow does not exist at one particular location. Many rainbows exist; however, only one can be seen depending on the particular observer's viewpoint as droplets of light illuminated by the sun. All raindrops refract and reflect the sunlight in the same way, but only the light from some raindrops reaches the observer's eye. This light is what constitutes the rainbow for that observer. The whole system composed by the sun's rays, the observer's head, and the (spherical) water drops has an axial symmetry around the axis through the observer's head and parallel to the sun's rays. The rainbow is curved because the set of all the raindrops that have the right angle between the observer, the drop, and the sun, lie on a cone pointing at the sun with the observer at the tip. The base of the cone forms a circle at an angle of 40–42° to the line between the observer's head and their shadow but 50% or more of the circle is below the horizon, unless the observer is sufficiently far above the earth's surface to see it all, for example in an aeroplane (see above).[21][22] Alternatively, an observer with the right vantage point may see the full circle in a fountain or waterfall spray.\n");

    @Benchmark
    public void base(Blackhole b) {
        sb.setLength(0);//reset
        markov.generate(sb, numberOfWords);
        b.consume(sb);
    }

    @Benchmark
    public void twoLoops_newTempArray(Blackhole b) {
        sb.setLength(0);//reset
        markov.generateTwoLoops(sb, numberOfWords);
        b.consume(sb);
    }

    @Benchmark
    public void twoLoops_cachedTemArray(Blackhole b) {
        sb.setLength(0);//reset
        markov.generateTwoLoopsCached(sb, numberOfWords);
        b.consume(sb);
    }

    static class MarkovChain {
        private static final int NON_WORD = -1;
        private static final String WHITE_SPACE = " ";
        //BigGram: <wordDictIDX, wordDictIDX>  List<WordDictIDX>
        private Map<BiGram, List<Integer>> stateTrans;
        private List<String> dict;

        public MarkovChain(Map<BiGram, List<Integer>> lookup, List<String> dict) {
            this.stateTrans = lookup;
            this.dict = dict;
        }


        public static MarkovChain fromInput(String s) {

            Map<BiGram, List<Integer>> stateTrans = new HashMap<>();
            List<String> dict = new ArrayList<>();
            Map<String, Integer> wordLookup = new HashMap<>();

            //ultra simple tokenizer
            for (String line : s.split("\n")) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                BiGram mutableState = new BiGram(NON_WORD, NON_WORD);//start state
                for (String word : line.split(WHITE_SPACE)) {
                    String cleanedWord = word.trim().toLowerCase();

                    //Treat all non letter and digt as tokens
                    if (!Character.isLetterOrDigit(cleanedWord.charAt(cleanedWord.length() - 1))) {
                        String extraToken = Character.toString(cleanedWord.charAt(cleanedWord.length() - 1));
                        cleanedWord = cleanedWord.substring(0, cleanedWord.length() - 1);
                        Integer wordIDX = wordLookup.computeIfAbsent(extraToken, x -> insertToDict(dict, x));
                        addSample(stateTrans, mutableState, wordIDX);
                    }
                    //Lookup word IDX
                    Integer wordIDX = wordLookup.computeIfAbsent(cleanedWord, x -> insertToDict(dict, x));
                    addSample(stateTrans, mutableState, wordIDX);
                }
                addSample(stateTrans, mutableState, NON_WORD);//terminal state

            }

            return new MarkovChain(stateTrans, dict);
        }

        private static void addSample(Map<BiGram, List<Integer>> stateTrans, BiGram mutableState, Integer wordIDX) {
            //get current state
            List<Integer> transitions = stateTrans.get(mutableState);
            if (transitions == null) {
                transitions = new ArrayList<>();
                stateTrans.put(mutableState.copy(), transitions);
            }

            transitions.add(wordIDX);

            //next state
            mutableState.nextMutable(wordIDX);
        }

        static int insertToDict(List<String> dict, String word) {
            dict.add(word);
            return dict.size() - 1;
        }

        public void generate(StringBuilder sb, int numberOfWords) {
            BiGram state = new BiGram(NON_WORD, NON_WORD);
            ThreadLocalRandom r = ThreadLocalRandom.current();
            for (int i = 0; i < numberOfWords; i++) {
                List<Integer> trans = stateTrans.get(state);
                int nextIDX = trans.get(r.nextInt(trans.size()));

                if (nextIDX == NON_WORD) {
                    break;
                }
                state.nextMutable(nextIDX);

                appendWord(sb, dict.get(nextIDX));
            }
        }

        public void generateTwoLoops(StringBuilder sb, int numberOfWords) {
            int[] words = new int[numberOfWords];
            numberOfWords = generateWordsIndexs(numberOfWords, words);
            for (int i = 0; i < numberOfWords; i++) {
                appendWord(sb, dict.get(words[i]));
            }
        }


        public void generateTwoLoopsCached(StringBuilder sb, int numberOfWords) {
            int[] words = getWordCache(numberOfWords);
            numberOfWords = generateWordsIndexs(numberOfWords, words);

            for (int i = 0; i < numberOfWords; i++) {
                appendWord(sb, dict.get(words[i]));
            }

        }

        private static final ThreadLocal<int[]> cache = new ThreadLocal<>();

        private int[] getWordCache(int numberOfWords) {
            int[] words = cache.get();
            if (words == null || words.length < numberOfWords) {
                words = new int[numberOfWords];
                cache.set(words);
            }
            return words;
        }

        private int generateWordsIndexs(int numberOfWords, int[] words) {
            BiGram state = new BiGram(NON_WORD, NON_WORD);
            ThreadLocalRandom r = ThreadLocalRandom.current();
            for (int i = 0; i < numberOfWords; i++) {
                List<Integer> trans = stateTrans.get(state);
                int nextIDX = trans.get(r.nextInt(trans.size()));

                if (nextIDX == NON_WORD) {
                    numberOfWords = i;
                    break;
                }
                state.nextMutable(nextIDX);
                words[i] = nextIDX;

            }
            return numberOfWords;
        }


        private void appendWord(StringBuilder sb, String word) {
            //single token like ,.?!" - remove previous whitespae
            if (word.length() == 1) {
                sb.setLength(Math.max(0, sb.length() - 1));
            }
            sb.append(word);
            sb.append(' ');
        }
    }

    static class BiGram {
        private int first;
        private int second;

        public BiGram(int first, int second) {
            this.first = first;
            this.second = second;
        }

        public void nextMutable(int next) {
            this.first = this.second;
            this.second = next;
        }

        public BiGram copy() {
            return new BiGram(first, second);
        }

        @Override
        public String toString() {
            return "[" + first + "," + second + "]";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            BiGram biGram = (BiGram) o;

            if (first != biGram.first) return false;
            return second == biGram.second;
        }

        @Override
        public int hashCode() {
            int result = first;
            result = 31 * result + second;
            return result;
        }
    }

    public static void main(String[] args) throws RunnerException {
        //for xperfas/WinPerfAsmProfiler
        System.setProperty("jmh.perfasm.xperf.dir", "C:\\Program Files (x86)\\Windows Kits\\10\\Windows Performance Toolkit");

        //Print statistics and samples
        System.out.printf("ChainStatistics:  states: %d  dictEntries: %d%n", markov.stateTrans.size(), markov.dict.size());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.setLength(0);
            markov.generate(sb, 1000);
            System.out.println(sb);
        }

        new Runner(new OptionsBuilder()
                .include(SimpleMarkovChainBenchmarkJMH.class.getName() + ".*")
                //##########
                // Profilers
                //############
                //commonly used profilers:
                .addProfiler(GCProfiler.class)
                //.addProfiler(StackProfiler.class)
                //.addProfiler(HotspotRuntimeProfiler.class)
                //.addProfiler(HotspotMemoryProfiler.class)
                //.addProfiler(HotspotCompilationProfiler.class)
                //
                // full list of built in profilers:
                //("cl",       ClassloaderProfiler.class);
                //("comp",     CompilerProfiler.class);
                //("gc",       GCProfiler.class);
                //("hs_cl",    HotspotClassloadingProfiler.class);
                //("hs_comp",  HotspotCompilationProfiler.class);
                //("hs_gc",    HotspotMemoryProfiler.class);
                //("hs_rt",    HotspotRuntimeProfiler.class);
                //("hs_thr",   HotspotThreadProfiler.class);
                //("stack",    StackProfiler.class);
                //("perf",     LinuxPerfProfiler.class);
                //("perfnorm", LinuxPerfNormProfiler.class);
                //("perfasm",  LinuxPerfAsmProfiler.class);
                //("xperfasm", WinPerfAsmProfiler.class);
                //("dtraceasm", DTraceAsmProfiler.class);
                //("pauses",   PausesProfiler.class);
                //("safepoints", SafepointsProfiler.class);
                //
                //ASM-level profilers - require -XX:+PrintAssembly
                //----------
                // this in turn requires hsdis (hotspot disassembler) binaries to be copied into e.g C:\Program Files\Java\jdk1.8.0_161\jre\bin\server
                // For Windows you can download pre-compiled hsdis module from http://fcml-lib.com/download.html
                //.jvmArgsAppend("-XX:+PrintAssembly") //requires hsdis binary in jdk - enable if you use the perf or winperf profiler
                ///required for external profilers like "perf" to show java frames in their traces
                //.jvmArgsAppend("-XX:+PerserveFramePointer")
                //XPERF  - windows xperf must be installed - this is included in WPT (windows performance toolkit) wich in turn is windows ADK included in https://developer.microsoft.com/en-us/windows/hardware/windows-assessment-deployment-kit
                //WARNING - MUST RUN WITH ADMINISTRATIVE PRIVILEGES (must start your console or your IDE with admin rights!
                //WARNING - first ever run of xperf takes VERY VERY long (1h+) because it has to download and process symbols
                //.addProfiler(WinPerfAsmProfiler.class)
                //.addProfiler(LinuxPerfProfiler.class)
                //.addProfiler(LinuxPerfNormProfiler.class)
                //.addProfiler(LinuxPerfAsmProfiler.class)
                //
                // #########
                // More Profling jvm options
                // #########
                // .jvmArgsAppend("-XX:+UnlockCommercialFeatures")
                // .jvmArgsAppend("-XX:+FlightRecorder")
                // .jvmArgsAppend("-XX:+UnlockDiagnosticVMOptions")
                // .jvmArgsAppend("-XX:+PrintSafepointStatistics")
                // .jvmArgsAppend("-XX:+DebugNonSafepoints")
                //
                // required for external profilers like "perf" to show java
                // frames in their traces
                // .jvmArgsAppend("-XX:+PerserveFramePointer")
                //
                // #########
                // COMPILER
                // #########
                // make sure we dont see compiling of our benchmark code during
                // measurement.
                // if you see compiling => more warmup
                .jvmArgsAppend("-XX:+UnlockDiagnosticVMOptions")
                //.jvmArgsAppend("-XX:+PrintCompilation")
                // .jvmArgsAppend("-XX:+PrintInlining")
                // .jvmArgsAppend("-XX:+PrintAssembly") //requires hsdis binary in jdk - enable if you use the perf or winperf profiler
                // .jvmArgsAppend("-XX:+PrintOptoAssembly") //c2 compiler only
                // More compiler prints:
                // .jvmArgsAppend("-XX:+PrintInterpreter")
                // .jvmArgsAppend("-XX:+PrintNMethods")
                // .jvmArgsAppend("-XX:+PrintNativeNMethods")
                // .jvmArgsAppend("-XX:+PrintSignatureHandlers")
                // .jvmArgsAppend("-XX:+PrintAdapterHandlers")
                // .jvmArgsAppend("-XX:+PrintStubCode")
                // .jvmArgsAppend("-XX:+PrintCompilation")
                // .jvmArgsAppend("-XX:+PrintInlining")
                // .jvmArgsAppend("-XX:+TraceClassLoading")
                // .jvmArgsAppend("-XX:PrintAssemblyOptions=syntax")
                .build())
                .run();

    }
}
