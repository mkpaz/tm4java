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
 * Extends and exposes constants for token types.
 *
 * <p>See {@link StandardTokenType}.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/encodedTokenAttributes.ts#L181">
 * vscode-textmate/src/encodedTokenAttributes.ts#L181</a>
 */
public final class OptionalStandardTokenType {

    /**
     * Indicates that no token type is set.
     */
    public static final int NOT_SET = 8;

    //*************************************************************************
    // Must have the same values as `StandardTokenType`!
    //*************************************************************************

    /**
     * Constant representing an uncategorized standard token type, aliasing {@link StandardTokenType#OTHER}.
     */
    public static final int OTHER = StandardTokenType.OTHER;

    /**
     * Constant representing a comment token type, aliasing {@link StandardTokenType#COMMENT}.
     */
    public static final int COMMENT = StandardTokenType.COMMENT;

    /**
     * Constant representing a literal string token type, aliasing {@link StandardTokenType#STRING}.
     */
    public static final int STRING = StandardTokenType.STRING;

    /**
     * Constant representing a regex token type, aliasing {@link StandardTokenType#REGEX}.
     */
    public static final int REGEX = StandardTokenType.REGEX;

    private OptionalStandardTokenType() {
        // utility class
    }
}
