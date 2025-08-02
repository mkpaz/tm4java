/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.grammar;

import static org.assertj.core.api.Assertions.assertThat;
import static tm4java.internal.utils.NullSafetyHelper.castNonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import tm4java.registry.IRegistryOptions;
import tm4java.registry.Registry;
import tm4java.Resources;
import tm4java.grammar.EncodedTokenAttributes;
import tm4java.grammar.IGrammarConfiguration;
import tm4java.grammar.IGrammarSource;
import tm4java.internal.grammar.tokenattrs.OptionalStandardTokenType;

@NullMarked
public class TokenizeLineTest {

    @Test
    public void testTokenizeLine() {
        var grammar = new Registry().addGrammar(
            IGrammarSource.fromResource(Resources.class, "JavaScript.tmLanguage")
        );

        var expected = """
            Token from 0 to 8 with scopes [source.js, meta.function.js, storage.type.function.js]
            Token from 8 to 9 with scopes [source.js, meta.function.js]
            Token from 9 to 12 with scopes [source.js, meta.function.js, entity.name.function.js]
            Token from 12 to 13 with scopes [source.js, meta.function.js, meta.function.type.parameter.js, meta.brace.round.js]
            Token from 13 to 14 with scopes [source.js, meta.function.js, meta.function.type.parameter.js, parameter.name.js, variable.parameter.js]
            Token from 14 to 15 with scopes [source.js, meta.function.js, meta.function.type.parameter.js]
            Token from 15 to 16 with scopes [source.js, meta.function.js, meta.function.type.parameter.js, parameter.name.js, variable.parameter.js]
            Token from 16 to 17 with scopes [source.js, meta.function.js, meta.function.type.parameter.js, meta.brace.round.js]
            Token from 17 to 18 with scopes [source.js, meta.function.js]
            Token from 18 to 19 with scopes [source.js, meta.function.js, meta.decl.block.js, meta.brace.curly.js]
            Token from 19 to 20 with scopes [source.js, meta.function.js, meta.decl.block.js]
            Token from 20 to 26 with scopes [source.js, meta.function.js, meta.decl.block.js, keyword.control.js]
            Token from 26 to 28 with scopes [source.js, meta.function.js, meta.decl.block.js]
            Token from 28 to 29 with scopes [source.js, meta.function.js, meta.decl.block.js, keyword.operator.arithmetic.js]
            Token from 29 to 32 with scopes [source.js, meta.function.js, meta.decl.block.js]
            Token from 32 to 33 with scopes [source.js, meta.function.js, meta.decl.block.js, meta.brace.curly.js]
            """
                           .split("\n");

        var lineText = "function add(a,b) { return a+b; }";
        var lineTokens = grammar.tokenizeLine(lineText);
        for (int i = 0; i < lineTokens.tokens().length; i++) {
            var token = lineTokens.tokens()[i];
            assertThat(token.getStartIndex()).isGreaterThanOrEqualTo(0);
            assertThat(token.getStartIndex()).isLessThanOrEqualTo(lineText.length());
            assertThat(token.getEndIndex()).isGreaterThanOrEqualTo(0);
            assertThat(token.getEndIndex()).isLessThanOrEqualTo(lineText.length());
            assertThat("Token from " + token.getStartIndex()
                       + " to " + token.getEndIndex() + " with scopes " + token.getScopes()
            ).isEqualTo(expected[i]);
        }
    }

    @Test
    public void testTokenizeLine2() throws Exception {
        var JS_SCOPE = "source.js";

        var languageMap = new HashMap<String, Integer>();
        languageMap.put(JS_SCOPE, 1);

        var tokenMap = new HashMap<String, Integer>();
        tokenMap.put("comment.block.js", OptionalStandardTokenType.COMMENT);
        tokenMap.put("string.single.js", OptionalStandardTokenType.STRING);

        var registry = new Registry(new IRegistryOptions() {
            @Override
            public @Nullable IGrammarSource getGrammarSource(@NonNull String scopeName) {
                if (scopeName.equals(JS_SCOPE)) {
                    return IGrammarSource.fromResource(Resources.class, "JavaScript.tmLanguage.json");
                }
                return null;
            }
        });

        var grammar = castNonNull(registry.loadGrammarWithConfiguration(
            JS_SCOPE, castNonNull(languageMap.get(JS_SCOPE)), new IGrammarConfiguration() {
                @Override
                public Map<String, Integer> getTokenTypes() {
                    return tokenMap;
                }
            }
        ));

        var lineText = "console.log('hi'); /*comment*/}";
        var lineTokens2 = grammar.tokenizeLine2(lineText);
        int[] encodedTokens = lineTokens2.tokens();

        var lineTokens = new ArrayList<String>();
        for (int i = 0; i < encodedTokens.length; i += 2) {
            int meta = encodedTokens[i + 1];

            int languageId = EncodedTokenAttributes.getLanguageId(meta);
            assertThat(languageId).isEqualTo(languageMap.get(JS_SCOPE));

            int start = encodedTokens[i];
            int end = i + 2 < encodedTokens.length ? encodedTokens[i + 2] : lineText.length();
            int tokenType = EncodedTokenAttributes.getTokenType(meta);
            lineTokens.add("Token [start=" + start + ", end=" + end + ", tokenType=" + tokenType + "]");
        }

        assertThat(lineTokens).containsExactly(
            "Token [start=0, end=12, tokenType=" + OptionalStandardTokenType.OTHER + "]",
            "Token [start=12, end=16, tokenType=" + OptionalStandardTokenType.STRING + "]",
            "Token [start=16, end=19, tokenType=" + OptionalStandardTokenType.OTHER + "]",
            "Token [start=19, end=30, tokenType=" + OptionalStandardTokenType.COMMENT + "]",
            "Token [start=30, end=31, tokenType=" + OptionalStandardTokenType.OTHER + "]");
    }

    @Test
    public void testTokenizeMultiByteLine() {
        var grammar = new Registry().addGrammar(IGrammarSource.fromResource(Resources.class, "c.tmLanguage.json"));

        var expected = """
            Token from 0 to 4 with scopes [source.c, storage.type.built-in.primitive.c]
            Token from 4 to 8 with scopes [source.c]
            Token from 8 to 10 with scopes [source.c, storage.modifier.array.bracket.square.c]
            Token from 10 to 11 with scopes [source.c]
            Token from 11 to 12 with scopes [source.c, keyword.operator.assignment.c]
            Token from 12 to 13 with scopes [source.c]
            Token from 13 to 14 with scopes [source.c, meta.block.c, punctuation.section.block.begin.bracket.curly.c]
            Token from 14 to 15 with scopes [source.c, meta.block.c, string.quoted.double.c, punctuation.definition.string.begin.c]
            Token from 15 to 20 with scopes [source.c, meta.block.c, string.quoted.double.c]
            Token from 20 to 21 with scopes [source.c, meta.block.c, string.quoted.double.c, punctuation.definition.string.end.c]
            Token from 21 to 22 with scopes [source.c, meta.block.c, punctuation.section.block.end.bracket.curly.c]
            Token from 22 to 23 with scopes [source.c, punctuation.terminator.statement.c]
            Token from 23 to 24 with scopes [source.c]
            Token from 24 to 28 with scopes [source.c, storage.type.built-in.primitive.c]
            Token from 28 to 35 with scopes [source.c]
            Token from 35 to 36 with scopes [source.c, keyword.operator.assignment.c]
            Token from 36 to 37 with scopes [source.c]
            Token from 37 to 38 with scopes [source.c, keyword.operator.c]
            Token from 38 to 39 with scopes [source.c, constant.numeric.decimal.c]
            Token from 39 to 40 with scopes [source.c, punctuation.terminator.statement.c]
            """.split("\n");

        var lineText = "char cat[] = {\"кошка\"}; char mouse = -1;\n";
        var lineTokens = grammar.tokenizeLine(lineText);
        for (int i = 0; i < lineTokens.tokens().length; i++) {
            var token = lineTokens.tokens()[i];

            assertThat(token.getStartIndex()).isGreaterThanOrEqualTo(0);
            assertThat(token.getStartIndex()).isLessThanOrEqualTo(lineText.length());
            assertThat(token.getEndIndex()).isGreaterThanOrEqualTo(0);
            assertThat(token.getEndIndex()).isLessThanOrEqualTo(lineText.length());
            assertThat("Token from " + token.getStartIndex() + " to " + token.getEndIndex() + " with scopes " +
                       token.getScopes())
                .isEqualTo(expected[i]);
        }
    }
}
