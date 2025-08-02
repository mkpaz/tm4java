/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.benchmark;

import java.util.Locale;
import tm4java.TMException;

public final class BenchmarkRunner {

    static final Runtime RUNTIME = Runtime.getRuntime();
    static final String SEPARATOR = "--------------------------------";

    private BenchmarkRunner() {
        // utility class
    }

    public static void run(int warmUpRounds,
                           int benchmarkRounds,
                           int opsPerBenchmarkRound,
                           Runnable benchmark) throws InterruptedException {
        Locale.setDefault(Locale.ENGLISH);

        System.out.println("JVM Vendor: " + System.getProperty("java.vendor"));
        System.out.println("JVM Version: " + System.getProperty("java.version"));
        System.out.printf("JVM Initial Heap: %.2f MB%n", RUNTIME.maxMemory() / (float) 1024 / 1024);
        System.out.printf("JVM Maximum Heap: %.2f MB%n", RUNTIME.totalMemory() / (float) 1024 / 1024);
        System.out.println(SEPARATOR);

        System.out.println("Warmup Rounds: " + warmUpRounds);
        System.out.println("Benchmark Rounds: " + warmUpRounds);
        System.out.println("Operations per Benchmark Round: " + opsPerBenchmarkRound);
        System.out.println(SEPARATOR);

        // warmup
        for (int r = 1; r <= warmUpRounds; r++) {
            runRound(
                "warm-up",
                r,
                warmUpRounds,
                5 * opsPerBenchmarkRound, // use more iterations to force JIT to kick in
                false, // don't try to measure heap usage as warm-up rounds will trigger GC
                benchmark
            );
        }

        System.out.println(SEPARATOR);

        // benchmark
        for (int r = 1; r <= benchmarkRounds; r++) {
            runRound("benchmark", r, benchmarkRounds, opsPerBenchmarkRound, true, benchmark);
        }
        System.out.println("DONE.");
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    static void runRound(String label,
                         int round,
                         int totalRounds,
                         int iterations,
                         boolean measureHeapUsage,
                         Runnable benchmark) throws InterruptedException {
        // spinning up a thread so the JVM hopefully runs the benchmark on different cores each round
        var t = new Thread(() -> {
            System.out.println(label + " " + round + "/" + totalRounds + "...");
            if (measureHeapUsage) {
                try {
                    System.gc();
                    Thread.sleep(1_000);
                    System.gc();
                    Thread.sleep(1_000);
                } catch (InterruptedException e) {
                    Thread.interrupted();
                    throw new TMException(e.getMessage(), e);
                }
            }

            var startFreeMem = RUNTIME.freeMemory();
            var startAt = System.currentTimeMillis();

            for (int i = 0; i < iterations; i++) {
                benchmark.run();
            }

            var durationMS = System.currentTimeMillis() - startAt;
            var durationMSPerIteration = durationMS / (float) iterations;
            var iterationsPerSecond = 60_000 / durationMSPerIteration;

            if (measureHeapUsage) {
                var heapBytesPerIteration = (startFreeMem - RUNTIME.freeMemory()) / (float) iterations;
                System.out.printf(
                    " -> result: %,5d ms/round | %,7.2f ops/s | %,5.2f ms/op | %,6.3f MB/op%n",
                    durationMS,
                    iterationsPerSecond,
                    durationMSPerIteration,
                    heapBytesPerIteration / 1024 / 1024
                );
            } else {
                System.out.printf(
                    " -> result: %,5d ms/round | %,7.2f ops/s | %,5.2f ms/op%n",
                    durationMS,
                    iterationsPerSecond,
                    durationMSPerIteration
                );
            }
        });

        t.setPriority(Thread.MAX_PRIORITY);
        t.start();
        t.join();
    }
}
