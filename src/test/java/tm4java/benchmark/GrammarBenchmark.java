/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.benchmark;

import java.util.Arrays;
import tm4java.grammar.IGrammar;
import tm4java.grammar.IGrammarSource;
import tm4java.grammar.IStateStack;
import tm4java.internal.utils.Resources;
import tm4java.registry.Registry;

public final class GrammarBenchmark implements Runnable {

    static final int WARMUP_ROUNDS = 3;
    static final int BENCHMARK_ROUNDS = 3;
    static final int OPS_PER_BENCHMARK_ROUND = 50;

    final IGrammar grammar;
    final String[] sourceCode;

    GrammarBenchmark() throws Exception {
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

    /**
     * Tokenize all lines of the preloaded source file.
     */
    @Override
    public void run() {
        IStateStack state = null;
        for (var line : sourceCode) {
            state = grammar.tokenizeLine(line, state, null).ruleStack();
        }
    }

    //*************************************************************************

    public static void main(String... args) throws Exception {
        BenchmarkRunner.run(WARMUP_ROUNDS, BENCHMARK_ROUNDS, OPS_PER_BENCHMARK_ROUND, new GrammarBenchmark());
    }
}
