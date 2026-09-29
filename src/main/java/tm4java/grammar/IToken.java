/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.grammar;

import java.util.List;

/**
 * Represents a token produced during standard TextMate line tokenization.
 *
 * <p>Tokens group one or more characters that belong to the same syntax element (such as keywords,
 * operators, comments, or string literals).
 *
 * <p>Each token carries a full hierarchy of TextMate scope selectors defining its context.
 * A scope is a dot-separated identifier (e.g. {@code keyword.operator.arithmetic.js}).
 * Scope lists are ordered from the outermost parent scope down to the most specific leaf scope.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/blob/v9.2.0/src/main.ts#L249">
 * vscode-textmate/src/main.ts#L249</a>
 * @see ITokenizeLineResult
 */
public interface IToken {

    /**
     * Returns the 0-based character offset where this token begins (inclusive).
     *
     * @return the inclusive start character index
     */
    int getStartIndex();

    /**
     * Returns the 0-based character offset where this token ends (exclusive).
     *
     * <p>The length of the token can be computed as {@code getEndIndex() - getStartIndex()}.
     *
     * @return the exclusive end character index
     */
    int getEndIndex();

    /**
     * Returns the hierarchy of scope selectors applied to this token.
     *
     * <p>The list is ordered from the least specific scope (e.g. {@code "source.java"}) at index {@code 0}
     * to the most specific leaf scope (e.g. {@code "entity.name.function.java"}) at the last index.
     *
     * @return a list of scope names associated with this token
     */
    List<String> getScopes();
}