# jmh-benchmarks collection
Started out with an introduction talk to JMH and ended up writing and collecting many interesting benchmarks.
Partly from real life projects and partly of personal interest.

The benchmarks are roughly organized into 4 different packages
- basic:  comparing Java language  and JVM features
- algorithms: comparing different algorithms (e.g. bas64 encoders, uuid implementations, String-Replace/Tokenize/Concat patterns, ..)
- architecture: mostly "good patterns" e.g. lazyInitialization and stuff worth (ThreadLocal) caching
- intro: Examples form my basic introduction to JMH and why __you__ should start using JMH.

## Requirements
- java >= 11
- maven
 
## Run the Benchmarks 
Run the `main` method in each benchmark class: 
  - A) its easiest to run the benchmarks from IDE starting the main method
    - for intellij you can install the JMH plugin
  - B) or run from console 
      ```bash
      java -cp target/benchmarks.jar de.frank.jmh.basic.<benchmarkClass> -p <paramName>=<paramValue>
      ```

(JMH recommended way is `java -jar benchmarks.jar <benchmarksToRunRegex` but often den benchmark classes use some kind of automation inside the `main`s  )

## JMH
Please read the project page of  [JMH - Java Microbenchmarking Harness](http://openjdk.java.net/projects/code-tools/jmh/)

If you want to learn how to work with JMH - work through all of the [official JMH-Examples](http://hg.openjdk.java.net/code-tools/jmh/file/tip/jmh-samples/src/main/java/org/openjdk/jmh/samples/) first!

The preferred JMH way is to create a own benchmarking project. Of course you can add JMH to any existing project as well.
To initialize a new JMH benchmark project you can used the provided maven archetype:
Unix:
```
 mvn archetype:generate \
          -DinteractiveMode=false \
          -DarchetypeGroupId=org.openjdk.jmh \
          -DarchetypeArtifactId=jmh-java-benchmark-archetype \
          -DgroupId=org.sample \
          -DartifactId=test \
          -Dversion=1.0
```

Win
```
 mvn archetype:generate ^
          -DinteractiveMode=false ^
          -DarchetypeGroupId=org.openjdk.jmh ^
          -DarchetypeArtifactId=jmh-java-benchmark-archetype ^
          -DgroupId=org.sample ^
          -DartifactId=test ^
          -Dversion=1.0
```

Building the benchmarks. After the project is generated, you can build it with the following Maven command:
```
 cd test/
 mvn clean install
```
Running the benchmarks. After the build is done, you will get the self-contained executable JAR, which holds your benchmark, and all essential JMH infrastructure code:
 `java -jar target/benchmarks.jar`

