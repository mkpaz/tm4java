/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.utils;

/**
 * A utility class for handling regular expressions.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/utils.ts#L59">
 * vscode-textmate/src/utils.ts#L59</a>
 */
@SuppressWarnings("RegExpRedundantEscape")
public final class RegexUtils {

    private RegexUtils() {
        // utility class
    }

    /**
     * Escapes/prefixes RegExp meta characters with a backslash in the given string.
     * <p>
     * It is a non-regex based faster alternative to the
     * <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/utils.ts#L159">
     * TypeScript implementation</a>
     *
     * <pre>{@code
     * function escapeRegExpCharacters(value: string): string {
     *     return value.replace(/[\-\\\{\}\*\+\?\|\^\$\.\,\[\]\(\)\#\s]/g, '\\$&');
     * }
     * }</pre>
     *
     * @return a string with the RegExp meta characters escaped
     */
    public static String escapeRegExpCharacters(CharSequence value) {
        int valueLen = value.length();
        var sb = new StringBuilder(valueLen);

        for (int i = 0; i < valueLen; i++) {
            final char ch = value.charAt(i);
            switch (ch) {
                case '-', '\\', '{', '}', '*', '+', '?', '|', '^', '$', '.', ',', '[', ']', '(', ')', '#':
                    // escaping white space chars is actually not necessary:
                    // ' ', '\t', '\n', '\f', '\r',
                    // 0x0B: // vertical tab \v
                    sb.append('\\');
            }
            sb.append(ch);
        }
        return sb.toString();
    }
}
