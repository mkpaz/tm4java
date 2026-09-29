/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.grammar.tokenattrs;

/**
 * Contains constants for standard TextMate token types.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/encodedTokenAttributes.ts#L159">
 * vscode-textmate/src/encodedTokenAttributes.ts#L159</a>
 */
final class StandardTokenType {

    /** Constant value representing an uncategorized standard token type. */
    static final int OTHER = 0;

    /** Constant value representing a comment token type. */
    static final int COMMENT = 1;

    /** Constant value representing a string literal token type. */
    static final int STRING = 2;

    /** Constant value representing a regex token type. */
    static final int REGEX = 3;

    private StandardTokenType() {
        // utility class
    }
}
