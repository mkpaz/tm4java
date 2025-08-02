/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.grammar;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import tm4java.Resources;
import tm4java.grammar.IGrammar;
import tm4java.grammar.IGrammarSource;
import tm4java.grammar.IStateStack;
import tm4java.grammar.IToken;
import tm4java.parser.ContentType;
import tm4java.registry.Registry;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static tm4java.grammar.IGrammarSource.fromString;

@TestMethodOrder(MethodOrderer.DisplayName.class)
public class GrammarTest {

    static final String[] EXPECTED_SINGLE_LINE_TOKENS = {
        "Token from 0 to 8 with scopes [source.js, meta.function.js, storage.type.function.js]",
        "Token from 8 to 9 with scopes [source.js, meta.function.js]",
        "Token from 9 to 12 with scopes [source.js, meta.function.js, entity.name.function.js]",
        "Token from 12 to 13 with scopes [source.js, meta.function.js, meta.function.type.parameter.js, meta.brace.round.js]",
        "Token from 13 to 14 with scopes [source.js, meta.function.js, meta.function.type.parameter.js, parameter.name.js, variable.parameter.js]",
        "Token from 14 to 15 with scopes [source.js, meta.function.js, meta.function.type.parameter.js]",
        "Token from 15 to 16 with scopes [source.js, meta.function.js, meta.function.type.parameter.js, parameter.name.js, variable.parameter.js]",
        "Token from 16 to 17 with scopes [source.js, meta.function.js, meta.function.type.parameter.js, meta.brace.round.js]",
        "Token from 17 to 18 with scopes [source.js, meta.function.js]",
        "Token from 18 to 19 with scopes [source.js, meta.function.js, meta.decl.block.js, meta.brace.curly.js]",
        "Token from 19 to 20 with scopes [source.js, meta.function.js, meta.decl.block.js]",
        "Token from 20 to 26 with scopes [source.js, meta.function.js, meta.decl.block.js, keyword.control.js]",
        "Token from 26 to 28 with scopes [source.js, meta.function.js, meta.decl.block.js]",
        "Token from 28 to 29 with scopes [source.js, meta.function.js, meta.decl.block.js, keyword.operator.arithmetic.js]",
        "Token from 29 to 32 with scopes [source.js, meta.function.js, meta.decl.block.js]",
        "Token from 32 to 33 with scopes [source.js, meta.function.js, meta.decl.block.js, meta.brace.curly.js]"
    };

    static final String[] EXPECTED_MULTI_LINE_TOKENS = {
        "Token from 0 to 8 with scopes [source.js, meta.function.js, storage.type.function.js]",
        "Token from 8 to 9 with scopes [source.js, meta.function.js]",
        "Token from 9 to 12 with scopes [source.js, meta.function.js, entity.name.function.js]",
        "Token from 12 to 13 with scopes [source.js, meta.function.js, meta.function.type.parameter.js, meta.brace.round.js]",
        "Token from 13 to 14 with scopes [source.js, meta.function.js, meta.function.type.parameter.js, parameter.name.js, variable.parameter.js]",
        "Token from 14 to 15 with scopes [source.js, meta.function.js, meta.function.type.parameter.js]",
        "Token from 15 to 16 with scopes [source.js, meta.function.js, meta.function.type.parameter.js, parameter.name.js, variable.parameter.js]",
        "Token from 16 to 17 with scopes [source.js, meta.function.js, meta.function.type.parameter.js, meta.brace.round.js]",
        "Token from 0 to 1 with scopes [source.js, meta.function.js, meta.decl.block.js, meta.brace.curly.js]",
        "Token from 1 to 2 with scopes [source.js, meta.function.js, meta.decl.block.js]",
        "Token from 2 to 8 with scopes [source.js, meta.function.js, meta.decl.block.js, keyword.control.js]",
        "Token from 8 to 10 with scopes [source.js, meta.function.js, meta.decl.block.js]",
        "Token from 10 to 11 with scopes [source.js, meta.function.js, meta.decl.block.js, keyword.operator.arithmetic.js]",
        "Token from 11 to 14 with scopes [source.js, meta.function.js, meta.decl.block.js]",
        "Token from 14 to 15 with scopes [source.js, meta.function.js, meta.decl.block.js, meta.brace.curly.js]"
    };

    @Test
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void testTokenizeConcurrent() throws Exception {
        var registry = new Registry();
        var file = Resources.getFile("/tm4java/language_pack/xml/xml.tmLanguage.json");
        var grammar = registry.addGrammar(IGrammarSource.fromFile(file));
        String content = Files.readString(file);

        int numThreads = 4;
        int numIterations = 10;

        try (var executor = Executors.newFixedThreadPool(numThreads)) {
            Runnable tokenizationTask = () -> {
                for (int i = 0; i < numIterations; i++) {
                    var result = TokenizationUtils.tokenizeText(content, grammar);
                    assertThat(result.count()).isGreaterThan(10);
                }
            };

            var futures = new ArrayList<Future<?>>();
            for (int i = 0; i < numThreads; i++) {
                futures.add(executor.submit(tokenizationTask));
            }

            for (Future<?> future : futures) {
                future.get();
            }

            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.MINUTES);
        }
    }

    @Test
    public void testTokenizeSingleLineExpression() {
        var registry = new Registry();
        IGrammar grammar = registry.addGrammar(
            IGrammarSource.fromResource(Resources.class, "JavaScript.tmLanguage")
        );

        var lineTokens = grammar.tokenizeLine("function add(a,b) { return a+b; }");
        assertThat(lineTokens.stoppedEarly()).isFalse();

        for (int i = 0; i < lineTokens.tokens().length; i++) {
            IToken token = lineTokens.tokens()[i];
            String s = "Token from "
                       + token.getStartIndex()
                       + " to " + token.getEndIndex()
                       + " with scopes " + token.getScopes();
            assertThat(s).isEqualTo(EXPECTED_SINGLE_LINE_TOKENS[i]);
        }
    }

    @Test
    public void testTokenizeMultilineExpression() {
        var registry = new Registry();
        IGrammar grammar = registry.addGrammar(
            IGrammarSource.fromResource(Resources.class, "JavaScript.tmLanguage")
        );

        IStateStack ruleStack = null;
        int i, j = 0;
        String[] lines = {"function add(a,b)", "{ return a+b; }"};

        for (String line : lines) {
            var lineTokens = grammar.tokenizeLine(line, ruleStack, null);
            assertThat(lineTokens.stoppedEarly()).isFalse();

            ruleStack = lineTokens.ruleStack();

            for (i = 0; i < lineTokens.tokens().length; i++) {
                IToken token = lineTokens.tokens()[i];
                String s = "Token from "
                           + token.getStartIndex()
                           + " to " + token.getEndIndex()
                           + " with scopes " + token.getScopes();
                assertThat(s).isEqualTo(EXPECTED_MULTI_LINE_TOKENS[i + j]);
            }

            j = i;
        }
    }

    @Test
    public void testTokenize0Tokens() {
        var registry = new Registry();
        IGrammar grammar = registry.addGrammar(
            IGrammarSource.fromResource(Resources.class, "JavaScript.tmLanguage")
        );
        String lineText = "";

        var lineTokens = grammar.tokenizeLine(lineText);
        assertThat(lineTokens.stoppedEarly()).isFalse();

        var endIndexOffset = 1; // end-indexes are exclusive

        assertThat(lineTokens.tokens())
            .hasOnlyOneElementSatisfying(token -> {
                assertThat(token.getStartIndex()).isZero();
                assertThat(token.getEndIndex()).isEqualTo(endIndexOffset);
            });
    }

    @Test
    public void testTokenize1Token() {
        var registry = new Registry();
        IGrammar grammar = registry.addGrammar(
            IGrammarSource.fromResource(Resources.class, "JavaScript.tmLanguage")
        );
        String lineText = "true";

        var lineTokens = grammar.tokenizeLine(lineText);
        assertThat(lineTokens.stoppedEarly()).isFalse();

        var endIndexOffset = 1; // end-indexes are exclusive

        assertThat(lineTokens.tokens())
            .hasOnlyOneElementSatisfying(token -> {
                assertThat(token.getStartIndex()).isZero();
                assertThat(token.getEndIndex()).isEqualTo(3 + endIndexOffset);
            });
    }

    @Test
    public void testTokenize1TokenWithNewLine() {
        var registry = new Registry();
        IGrammar grammar = registry.addGrammar(
            IGrammarSource.fromResource(Resources.class, "JavaScript.tmLanguage")
        );
        String lineText = "true\n";

        var lineTokens = grammar.tokenizeLine(lineText);
        assertThat(lineTokens.stoppedEarly()).isFalse();

        var endIndexOffset = 1; // end-indexes are exclusive

        System.out.println(Arrays.toString(lineTokens.tokens()));

        assertThat(lineTokens.tokens())
            .hasOnlyOneElementSatisfying(token -> {
                assertThat(token.getStartIndex()).isZero();
                assertThat(token.getEndIndex()).isEqualTo(3 + endIndexOffset);
            });
    }

    @Test
    public void testTokenize1IllegalToken() {
        var registry = new Registry();
        IGrammar grammar = registry.addGrammar(
            IGrammarSource.fromResource(Resources.class, "JavaScript.tmLanguage")
        );
        String lineText = "@"; // uncaught SyntaxError: illegal character U+0040

        var lineTokens = grammar.tokenizeLine(lineText);
        assertThat(lineTokens.stoppedEarly()).isFalse();

        var endIndexOffset = 1; // end-indexes are exclusive

        assertThat(lineTokens.tokens()).hasOnlyOneElementSatisfying(token -> {
            assertThat(token.getStartIndex()).isZero();
            assertThat(token.getEndIndex()).isEqualTo(endIndexOffset);
        });
    }

    @Test
    public void testTokenize2Tokens() {
        var registry = new Registry();
        IGrammar grammar = registry.addGrammar(
            IGrammarSource.fromResource(Resources.class, "JavaScript.tmLanguage")
        );
        String lineText = "{}";

        var lineTokens = grammar.tokenizeLine(lineText);
        assertThat(lineTokens.stoppedEarly()).isFalse();

        var endIndexOffset = 1; // end-indexes are exclusive

        assertThat(lineTokens.tokens())
            .hasSize(2)
            .satisfiesExactly(
                first -> {
                    assertThat(first.getStartIndex()).isZero();
                    assertThat(first.getEndIndex()).isEqualTo(endIndexOffset);
                },
                second -> {
                    assertThat(second.getStartIndex()).isEqualTo(1);
                    assertThat(second.getEndIndex()).isEqualTo(1 + endIndexOffset);
                });
    }

    @Test
    public void testTokenizeMultilineYaml() {
        var registry = new Registry();
        var grammar = registry.addGrammar(
            IGrammarSource.fromResource(Resources.class, "yaml.tmLanguage.json")
        );

        var lines = ">\n should.be.string.unquoted.block.yaml\n should.also.be.string.unquoted.block.yaml";
        var result = TokenizationUtils.tokenizeText(lines, grammar).iterator();

        Assertions.assertThat(result.next().tokens())
            .anyMatch(t -> t.getScopes().contains("keyword.control.flow.block-scalar.folded.yaml"));
        Assertions.assertThat(result.next().tokens())
            .anyMatch(t -> t.getScopes().contains("string.unquoted.block.yaml"));
        Assertions.assertThat(result.next().tokens())
            .anyMatch(t -> t.getScopes().contains("string.unquoted.block.yaml"));
    }

    @Test
    public void testTokenizeTypeScriptFile() throws Exception {
        var grammar = new Registry().addGrammar(
            IGrammarSource.fromResource(Resources.class, "TypeScript.tmLanguage.json")
        );

        List<String> expectedTokens;
        try (var reader = Resources.getReader("raytracer_tokens.txt")) {
            expectedTokens = reader.lines().toList();
        }

        IStateStack stateStack = null;
        int tokenIndex = -1;

        try (var reader = Resources.getReader("raytracer.ts")) {
            String line;
            while ((line = reader.readLine()) != null) {
                var lineTokens = grammar.tokenizeLine(line, stateStack, null);
                stateStack = lineTokens.ruleStack();
                for (int i = 0; i < lineTokens.tokens().length; i++) {
                    tokenIndex++;
                    var token = lineTokens.tokens()[i];
                    assertThat("Token from "
                               + token.getStartIndex()
                               + " to " + token.getEndIndex()
                               + " with scopes " + token.getScopes()
                    ).isEqualTo(expectedTokens.get(tokenIndex));
                }
            }
        }
    }

    @Test
    public void testTokenizeWithTimeout() throws IOException {
        var grammar = new Registry().addGrammar(
            IGrammarSource.fromResource(Resources.class, "TypeScript.tmLanguage.json")
        );

        try (var reader = Resources.getReader("raytracer.ts")) {
            String veryLongLine = reader.lines().collect(Collectors.joining());
            var result1 = grammar.tokenizeLine(veryLongLine);
            assertThat(result1.stoppedEarly()).isFalse();

            var lastToken1 = result1.tokens()[result1.tokens().length - 1];

            var result2 = grammar.tokenizeLine(veryLongLine, null, Duration.ofMillis(10));
            assertThat(result2.stoppedEarly()).isTrue();
            assertThat(result2.tokens()).hasSizeLessThan(result1.tokens().length);
            assertThat(result1.tokens()).contains(result2.tokens());

            var lastToken2 = result2.tokens()[result2.tokens().length - 1];
            assertThat(lastToken2.getEndIndex()).isLessThan(lastToken1.getEndIndex());
        }
    }

    // TODO (upstream): Absolute rule reference uses local scope
    // https://github.com/microsoft/vscode-textmate/issues/173
    @Disabled
    @Test
    public void testShadowedRulesAreResolvedCorrectly() {
        var registry = new Registry();
        var grammar = registry.addGrammar(fromString(ContentType.JSON, """
            {
            	"scopeName": "source.test",
            	"repository": {
            		"foo": {
            			"include": "#bar"
            		},
            		"bar": {
            			"match": "bar1",
            			"name": "outer"
            		}
            	},
            	"patterns": [{
            			"patterns": [{
            				"include": "#foo"
            			}],
            			"repository": {
            				"bar": {
            					"match": "bar1",
            					"name": "inner"
            				}
            			}
            		},
            		{
            			"begin": "begin",
            			"patterns": [{
            				"include": "#foo"
            			}],
            			"end": "end"
            		}
            	]
            }
            """));

        var lineTokens = grammar.tokenizeLine("bar1");
        assertThat(lineTokens.stoppedEarly()).isFalse();

        assertThat(lineTokens.tokens()).hasOnlyOneElementSatisfying(token -> {
            assertThat(token.getStartIndex()).isZero();
            assertThat(token.getEndIndex()).isEqualTo(4);
            assertThat(token.getScopes()).containsExactly("source.test", "outer");
        });
    }
}
