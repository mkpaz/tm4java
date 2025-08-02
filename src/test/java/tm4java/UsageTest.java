/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java;

import java.time.Duration;
import java.util.Arrays;
import java.util.function.BiConsumer;
import tm4java.grammar.IGrammar;
import tm4java.grammar.IGrammarSource;
import tm4java.grammar.IStateStack;
import tm4java.grammar.IToken;
import tm4java.grammar.ITokenizeLineResult;
import tm4java.registry.Registry;
import tm4java.theme.ITheme;
import tm4java.theme.IThemeSource;
import tm4java.theme.StyleAttributes;

public class UsageTest {

    public static void main(String[] args) {
        var registry = new Registry();

        IGrammar grammar = registry.addGrammar(IGrammarSource.fromFile(
            Resources.getFile("/tm4java/language_pack/java/java.tmLanguage.json")
        ));

        ITheme theme = registry.setTheme(IThemeSource.fromFile(
            Resources.getFile("/tm4java/test-cases/themes/light_vs.json")
        ));

        String text = """
            public static void main(String[] args) {
                System.out.println("Hello World!");
            }
            """;

        BiConsumer<String, IToken> printer = (line, token) -> {
            System.out.printf("Token:\t'%s'%n", line.substring(token.getStartIndex(), token.getEndIndex()));
            System.out.printf("Scopes:\t%s%n",token.getScopes());

            for (int i = token.getScopes().size() - 1; i >= 0; i--) {
                String scope = token.getScopes().get(i);
                StyleAttributes style = theme.match(scope);
                if (style != null && !StyleAttributes.NO_STYLE.equals(style)) {
                    System.out.println("Style:\t" + style);
                }
            }
        };

        IStateStack state = null;
        for (var line : text.split("\r?\n")) {
            ITokenizeLineResult<IToken[]> result = grammar.tokenizeLine(line, state, Duration.ofSeconds(1));
            state = result.ruleStack();
            Arrays.stream(result.tokens()).forEach(token -> printer.accept(line, token));
        }
    }
}
