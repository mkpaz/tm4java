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

import java.util.Collection;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import tm4java.grammar.IGrammar;
import tm4java.grammar.IGrammarSource;
import tm4java.grammar.IToken;
import tm4java.registry.IRegistryOptions;
import tm4java.registry.Registry;
import tm4java.Resources;

@NullMarked
public class GrammarInjectionTest {

    static final String[] EXPECTED_TOKENS = {
        "Token from 0 to 1 with scopes [source.ts, meta.decorator.ts, punctuation.decorator.ts]",
        "Token from 1 to 10 with scopes [source.ts, meta.decorator.ts, entity.name.function.ts]",
        "Token from 10 to 11 with scopes [source.ts, meta.decorator.ts, meta.brace.round.ts]",
        "Token from 11 to 12 with scopes [source.ts, meta.decorator.ts, meta.object-literal.ts, punctuation.definition.block.ts]",
        "Token from 12 to 20 with scopes [source.ts, meta.decorator.ts, meta.object-literal.ts, meta.object.member.ts, meta.object-literal.key.ts]",
        "Token from 20 to 21 with scopes [source.ts, meta.decorator.ts, meta.object-literal.ts, meta.object.member.ts, meta.object-literal.key.ts, punctuation.separator.key-value.ts]",
        "Token from 21 to 22 with scopes [source.ts, meta.decorator.ts, meta.object-literal.ts, meta.object.member.ts, string.template.ts, punctuation.definition.string.template.begin.ts]",
        "Token from 22 to 38 with scopes [source.ts, meta.decorator.ts, meta.object-literal.ts, meta.object.member.ts, string.template.ts]",
        "Token from 38 to 39 with scopes [source.ts, meta.decorator.ts, meta.object-literal.ts, meta.object.member.ts, string.template.ts, punctuation.definition.string.template.end.ts]",
        "Token from 39 to 40 with scopes [source.ts, meta.decorator.ts, meta.object-literal.ts, punctuation.definition.block.ts]",
        "Token from 40 to 41 with scopes [source.ts, meta.decorator.ts, meta.brace.round.ts]"
    };

    @Test
    public void angular2TokenizeLine() throws Exception {
        var registry = new Registry(new IRegistryOptions() {

            @Override
            public Collection<String> getInjections(@Nullable String scopeName) {
                return List.of("template.ng", "styles.ng");
            }

            @Override
            public @Nullable IGrammarSource getGrammarSource(@NonNull String scopeName) {
                return switch (scopeName) {
                    case "source.css" -> IGrammarSource.fromResource(Resources.class, "css.json");
                    case "source.js" -> IGrammarSource.fromResource(Resources.class, "JavaScript.tmLanguage.json");
                    case "styles.ng" -> IGrammarSource.fromResource(Resources.class, "styles.ng.json");
                    case "template.ng" -> IGrammarSource.fromResource(Resources.class, "template.ng.json");
                    case "text.html.basic" -> IGrammarSource.fromResource(Resources.class, "html.json");
                    default -> null;
                };
            }
        });

        IGrammar grammar = registry.addGrammar(
            IGrammarSource.fromResource(Resources.class, "TypeScript.tmLanguage.json")
        );

        var lineTokens = grammar.tokenizeLine("@Component({template:`<a href='' ></a>`})");
        for (int i = 0; i < lineTokens.tokens().length; i++) {
            IToken token = lineTokens.tokens()[i];
            var s = "Token from "
                    + token.getStartIndex()
                    + " to " + token.getEndIndex()
                    + " with scopes " + token.getScopes();

            System.out.println(s);
            assertThat(s).isEqualTo(EXPECTED_TOKENS[i]);
        }
    }
}
