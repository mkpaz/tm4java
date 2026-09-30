/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static tm4java.internal.utils.NullSafety.castNonNull;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import tm4java.registry.Registry;
import tm4java.TMException;
import tm4java.grammar.IGrammarSource;
import tm4java.internal.grammar.oniguruma.OnigRegExp;
import tm4java.internal.grammar.raw.IRawRule;
import tm4java.internal.grammar.raw.RawGrammar;
import tm4java.internal.utils.Resources;

@NullMarked
@TestMethodOrder(MethodOrderer.MethodName.class)
public class TMParserTest {

    @Test
    public void testParseCapturesJSON() {
        // test capture defined as JSON map
        validateCaptures(TMParserNanoJson.instance().parse(new StringReader("""
            {"patterns": [{
            	"name": "THE_PATTERN",
            	"captures": {
            		"0": { "name": "THE_CAPTURE" }
            	},
            	"begin": "BEGIN_PATTERN",
            	"end": "END_PATTERN"
            }]}"""), RawGrammar.OBJECT_FACTORY));

        // test capture defined as JSON array
        validateCaptures(TMParserNanoJson.instance().parse(new StringReader("""
            {"patterns": [{
            	"name": "THE_PATTERN",
            	"captures": [
            		{ "name": "THE_CAPTURE" }
            	],
            	"begin": "BEGIN_PATTERN",
            	"end": "END_PATTERN"
            }]}"""), RawGrammar.OBJECT_FACTORY));
    }

    @Test
    public void testParseCapturesPList() throws Exception {
        // test capture defined as Plist dict
        validateCaptures(TMParserPList.instance().parse(new StringReader("""
            <plist version="1.0">
            <dict>
            	<key>patterns</key>
            	<array>
            		<dict>
            			<key>name</key>
            			<string>THE_PATTERN</string>
            			<key>captures</key>
            			<dict>
            				<key>0</key>
            				<dict>
            					<key>name</key>
            					<string>THE_CAPTURE</string>
            				</dict>
            			</dict>
            			<key>begin</key>
            			<string>BEGIN_PATTERN</string>
            			<key>end</key>
            			<string>END_PATTERN</string>
            		</dict>
            	</array>
            </dict>
            </plist>"""), RawGrammar.OBJECT_FACTORY));

        // test capture defined as Plist array
        validateCaptures(TMParserPList.instance().parse(new StringReader("""
            <plist version="1.0">
            <dict>
            	<key>patterns</key>
            	<array>
            		<dict>
            			<key>name</key>
            			<string>THE_PATTERN</string>
            			<key>captures</key>
            			<array>
            				<dict>
            					<key>name</key>
            					<string>THE_CAPTURE</string>
            				</dict>
            			</array>
            			<key>begin</key>
            			<string>BEGIN_PATTERN</string>
            			<key>end</key>
            			<string>END_PATTERN</string>
            		</dict>
            	</array>
            </dict>
            </plist>"""), RawGrammar.OBJECT_FACTORY));
    }

    @Test
    public void testParseJSON() throws Exception {
        try (var reader = Resources.getReader(tm4java.Resources.class, "csharp.json")) {
            var grammar = TMParserNanoJson.instance().parse(reader, RawGrammar.OBJECT_FACTORY);
            assertThat(grammar.getRepository()).isNotNull();
            assertThat(grammar.getFileTypes()).isNotEmpty();
            assertThat(grammar.getFileTypes()).containsExactly("cs");
            assertThat(grammar.getName()).isEqualTo("C#");
            assertThat(grammar.getScopeName()).isEqualTo("source.cs");
            assertThat(grammar.keySet()).isEqualTo(Set.of(
                "fileTypes", "foldingStartMarker", "foldingStopMarker", "name", "patterns", "repository", "scopeName"
            ));
        }
    }

    @Test
    public void testParsePlist() throws Exception {
        try (var reader = Resources.getReader(tm4java.Resources.class, "JavaScript.tmLanguage")) {
            var grammar = TMParserPList.instance().parse(reader, RawGrammar.OBJECT_FACTORY);
            assertThat(grammar).isNotNull();
            assertThat(grammar.getRepository()).isNotNull();
            assertThat(grammar.getFileTypes()).isNotEmpty();
            assertThat(grammar.getFileTypes()).containsExactly("js", "jsx");
            assertThat(grammar.getName()).isEqualTo("JavaScript (with React support)");
            assertThat(grammar.getScopeName()).isEqualTo("source.js");
            assertThat(grammar.keySet()).isEqualTo(Set.of(
                "fileTypes", "name", "patterns", "repository", "scopeName", "uuid"
            ));
        }
    }

    @Test
    @SuppressWarnings("null")
    public void testLanguagePackGrammars() throws IOException {
        var count = new AtomicInteger();
        Files.walkFileTree(tm4java.Resources.getDirectory("/tm4java/language_pack"), new SimpleFileVisitor<>() {
            @Override
            public @NonNull FileVisitResult visitFile(@NonNull Path file, @NonNull BasicFileAttributes attrs) {
                var fileName = file.getFileName().toString().toLowerCase();
                if (fileName.endsWith("tmlanguage.yaml")
                    || fileName.endsWith("tmlanguage.json")
                    || fileName.endsWith("plist")
                    || fileName.endsWith("tmlanguage")) {

                    System.out.println("Parsing [" + file + "]...");
                    try {
                        var rawGrammar = RawGrammar.from(IGrammarSource.fromFile(file));
                        count.incrementAndGet();
                        assertThat(rawGrammar.getScopeName()).isNotBlank();
                        assertThat(rawGrammar.getFileTypes()).isNotNull();
                        assertThat(rawGrammar.getRepository()).isNotNull();

                        var patterns = castNonNull(rawGrammar.getPatterns());
                        assertThat(patterns).isNotEmpty();
                        assertParseablePatterns(patterns);

                        var reg = new Registry();
                        var grammar = reg.addGrammar(IGrammarSource.fromFile(file));
                        assertThat(grammar.getName()).isEqualTo(rawGrammar.getName());
                        assertThat(grammar.getScopeName()).isEqualTo(rawGrammar.getScopeName());
                        assertThat(grammar.getFileTypes()).isEqualTo(rawGrammar.getFileTypes());
                        grammar.tokenizeLine("");
                    } catch (Exception e) {
                        throw new TMException(e.getMessage(), e);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
        System.out.println("Successfully parsed " + count.intValue() + " grammars.");
        assertThat(count.intValue())
            .withFailMessage("Only %d grammars found, expected more than 10!", count.intValue())
            .isGreaterThan(10);
    }

    //*************************************************************************

    void validateCaptures(RawGrammar grammar) {
        assertThat(grammar.getPatterns()).isNotNull();
        assertThat(castNonNull(grammar.getPatterns()).size()).isEqualTo(1);

        var pattern = castNonNull(grammar.getPatterns()).iterator().next();
        assertThat(pattern.getName()).isEqualTo("THE_PATTERN");
        assertThat(pattern.getBegin()).isEqualTo("BEGIN_PATTERN");
        assertThat(pattern.getEnd()).isEqualTo("END_PATTERN");

        var captures = castNonNull(pattern.getCaptures());
        assertThat(castNonNull(captures.getCapture("0")).getName()).isEqualTo("THE_CAPTURE");
    }

    void assertParseablePattern(@Nullable String pattern) {
        if (pattern == null) {
            return;
        }
        try {
            assertThat(new OnigRegExp(pattern)).isNotNull();
        } catch (RuntimeException e) {
            var msg = e.getMessage();
            if (msg == null || !msg.contains("invalid backref number/name")) {
                throw e;
            }
        }
    }

    void assertParseablePatterns(@Nullable Collection<IRawRule> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            return;
        }

        for (var rule : patterns) {
            assertParseablePattern(rule.getBegin());
            assertParseablePattern(rule.getEnd());
            assertParseablePattern(rule.getMatch());
            assertParseablePattern(rule.getWhile());
            assertParseablePatterns(rule.getPatterns());
        }
    }
}
