package tm4java.benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import tm4java.grammar.IGrammar;
import tm4java.grammar.IGrammarSource;
import tm4java.grammar.IStateStack;
import tm4java.internal.utils.Resources;
import tm4java.registry.Registry;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(value = 2)
public class GrammarBenchmark {

    private IGrammar grammar;
    private String[] sourceCode;

    @Setup
    public void setup() throws Exception {
        grammar = new Registry().addGrammar(
            IGrammarSource.fromResource(GrammarBenchmark.class, "GrammarBenchmark.Java.tmLanguage.json")
        );

        try (var sourceFileReader =
                 Resources.getReader(GrammarBenchmark.class, "GrammarBenchmark.JavaFile.txt")) {
            sourceCode = sourceFileReader.lines().toArray(String[]::new);
        }

        System.out.printf("Source Code chars: %,d%n", Arrays.stream(sourceCode).mapToInt(String::length).sum());
        System.out.printf("Source Code lines: %,d%n", sourceCode.length);
    }

    /** Tokenize all lines of the preloaded source file. */
    @Benchmark
    public IStateStack tokenizeGrammar() {
        IStateStack state = null;
        for (var line : sourceCode) {
            state = grammar.tokenizeLine(line, state, null).ruleStack();
        }
        return state;
    }

    static void main(String[] args) throws Exception {
        var opt = new OptionsBuilder()
            .include(GrammarBenchmark.class.getSimpleName())
            .addProfiler("gc")
            .build();

        new Runner(opt).run();
    }
}
