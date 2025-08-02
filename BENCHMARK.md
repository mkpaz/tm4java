## Benchmark

The [GrammarBenchmark](src/test/java/tm4java/benchmark/GrammarBenchmark.java) measures how long it takes to tokenize
a given source file using the [Grammar](/src/main/java/tm4java/internal/grammar/Grammar.java)`#tokenizeLine()` method
and how much memory is allocated on the JVM heap to do so. As test source file the
[GrammarBenchmark.JavaFile.txt](src/test/resources/tm4java/benchmark/GrammarBenchmark.JavaFile.txt) is used.

The benchmark executes multiple rounds. Each round is executed sequentially in a new thread. This gives the OS the
opportunity, that in case for the first round an over-utilized core was chosen, to select a better core to execute
the new thread of the next benchmark round.

The output will look something like this:

```txt
Source Code chars: 36.387
Source Code lines: 901
JVM Vendor: Oracle Corporation
JVM Version: 24
JVM Initial Heap: 2048.00 MB
JVM Maximum Heap: 2048.00 MB
--------------------------------
Warmup Rounds: 3
Benchmark Rounds: 3
Operations per Benchmark Round: 50
--------------------------------
warm-up 1/3...
 -> result: 29,444 ms/round |  509.44 ops/s | 117.78 ms/op
warm-up 2/3...
 -> result: 28,278 ms/round |  530.45 ops/s | 113.11 ms/op
warm-up 3/3...
 -> result: 27,945 ms/round |  536.77 ops/s | 111.78 ms/op
--------------------------------
benchmark 1/3...
 -> result: 5,691 ms/round |  527.15 ops/s | 113.82 ms/op | 17.550 MB/op
benchmark 2/3...
 -> result: 5,602 ms/round |  535.52 ops/s | 112.04 ms/op | 17.550 MB/op
benchmark 3/3...
 -> result: 5,626 ms/round |  533.24 ops/s | 112.52 ms/op | 17.550 MB/op
```

The results of the three benchmark rounds show that it was possible to parse the whole source file
`533.24` times per second. One time parsing of the source file took `112.04` ms and allocated `17.55MB`
of temporary objects on the JVM heap.

To run the benchmark:

```txt
JAVA_HOME=path/to/jdk mvn verify -DskipTests -P benchmark
```