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
 * Tokens are one or more characters that belong to the same program element. For example,
 * tokens include operators such as {@code +} and {@code *}, variable names like {@code myVar},
 * or strings such as {@code "my string"}.
 *
 * <p>Each token is associated with a scope that defines its context. A scope is a dot-separated
 * list of identifiers specifying the context of the current token. For instance, the {@code +}
 * operation in JavaScript has the scope {@code keyword.operator.arithmetic.js}. Scopes can nest,
 * so each token is also associated with a list of parent scopes. The {@link #getScopes()}
 * method returns the list of scopes the token is part of.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/main.ts#L249">vscode-textmate/src/main.ts#L249</a>
 */
public interface IToken {

    /**
     * Returns the 0-based token start index (inclusive).
     */
    int getStartIndex();

    /**
     * Returns the 0-based token end index (inclusive).
     */
    int getEndIndex();

    /**
     * Returns the list of scopes associated with the token, from the least
     * specific to the most specific scope.
     */
    List<String> getScopes();
}
