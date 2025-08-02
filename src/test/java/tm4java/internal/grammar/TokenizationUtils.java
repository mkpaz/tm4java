/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.grammar;

import tm4java.grammar.IGrammar;
import tm4java.grammar.IStateStack;
import tm4java.grammar.IToken;
import tm4java.grammar.ITokenizeLineResult;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class TokenizationUtils {

    public static final Pattern LINE_SPLIT_PATTERN = Pattern.compile("\\r?\\n");

    /**
     * Lazy tokenizes the given text.
     * Returns the stream of {@link ITokenizeLineResult}, each item covering 1 line of the text.
     */
    public static Stream<ITokenizeLineResult<IToken[]>> tokenizeText(CharSequence text, IGrammar grammar) {
        if (text.isEmpty()) {
            return Stream.empty();
        }

        var prevStack = new AtomicReference<IStateStack>();
        return LINE_SPLIT_PATTERN.splitAsStream(text).map(line -> {
            var tokenized = grammar.tokenizeLine(line, prevStack.get(), null);
            prevStack.set(tokenized.ruleStack());
            return tokenized;
        });
    }

    /**
     * Lazy tokenizes the text provided by the given input stream.
     * Returns the stream of {@link ITokenizeLineResult}, each item covering 1 line of the text.
     */
    public static Stream<ITokenizeLineResult<IToken[]>> tokenizeText(InputStream text, IGrammar grammar) {
        var reader = new BufferedReader(new InputStreamReader(text));

        var prevStack = new AtomicReference<IStateStack>();
        return reader.lines().map(line -> {
            var tokenized = grammar.tokenizeLine(line, prevStack.get(), null);
            prevStack.set(tokenized.ruleStack());
            return tokenized;
        });
    }
}
