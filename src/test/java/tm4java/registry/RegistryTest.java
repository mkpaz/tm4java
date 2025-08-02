/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.registry;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicInteger;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import tm4java.Resources;
import tm4java.grammar.IGrammarSource;

@NullMarked
public class RegistryTest {

    @Test
    public void testLoadingLanguagePackGrammars() throws Exception {
        var registry = new Registry();
        var count = new AtomicInteger();

        Files.walkFileTree(Resources.getDirectory("/tm4java/language_pack/"), new SimpleFileVisitor<>() {
            @Override
            public @NonNull FileVisitResult visitFile(@NonNull Path file,
                                                      @Nullable BasicFileAttributes attrs) {
                if (file.getFileName().toString().endsWith("tmLanguage.json")) {
                    System.out.println("Parsing [" + file + "]");
                    var grammar = registry.addGrammar(IGrammarSource.fromFile(file));
                    count.incrementAndGet();
                    assertThat(grammar.getScopeName()).isNotBlank();
                    assertThat(grammar.getFileTypes()).isNotNull();
                }

                return FileVisitResult.CONTINUE;
            }
        });

        System.out.println("Successfully parsed " + count.intValue() + " grammars");
        assertThat(count).as("Number of grammars found").hasValueGreaterThan(10);
    }

    @Test
    @SuppressWarnings("DataFlowIssue")
    public void testLoadingUnknownGrammar() {
        var registry = new Registry();
        assertThat(registry.grammarForScopeName("undefined")).isNull();
        assertThat(registry.loadGrammar("undefined")).isNull();
    }
}
