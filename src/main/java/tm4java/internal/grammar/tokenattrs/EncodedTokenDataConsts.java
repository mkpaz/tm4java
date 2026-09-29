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
 * Helpers to manage the "collapsed" metadata of an entire StackElement stack.
 *
 * <p>The following assumptions have been made:
 * <ul>
 * <li>languageId &lt; 256 =&gt; needs 8 bits</li>
 * <li>unique color count &lt; 512 =&gt; needs 9 bits</li>
 * </ul>
 *
 * <p>The binary format is:
 * <pre>
 * ---------------------------------------
 * 3322 2222 2222 1111 1111 1100 0000 0000
 * 1098 7654 3210 9876 5432 1098 7654 3210
 * ---------------------------------------
 * xxxx xxxx xxxx xxxx xxxx xxxx xxxx xxxx
 * bbbb bbbb ffff ffff fFFF FBTT LLLL LLLL
 * ---------------------------------------
 * </pre>
 *
 * <ul>
 * <li>L = LanguageId (8 bits)</li>
 * <li>T = StandardTokenType (2 bits)</li>
 * <li>B = Balanced bracket (1 bit)</li>
 * <li>F = FontStyle (4 bits)</li>
 * <li>f = foreground color (9 bits)</li>
 * <li>b = background color (9 bits)</li>
 * </ul>
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/encodedTokenAttributes.ts#L143">
 * vscode-textmate/src/encodedTokenAttributes.ts#L143</a>
 */
public final class EncodedTokenDataConsts {

    /** Bitmask for extracting the 8-bit language ID (bits 0–7). */
    public static final int LANGUAGE_ID_MASK = 0b00000000_00000000_00000000_11111111;

    /** Bitmask for extracting the 2-bit standard token type (bits 8–9). */
    public static final int TOKEN_TYPE_MASK = 0b00000000_00000000_00000011_00000000;

    /** Bitmask for extracting the 1-bit balanced bracket flag (bit 10). */
    public static final int BALANCED_BRACKETS_MASK = 0b00000000_00000000_00000100_00000000;

    /** Bitmask for extracting the 4-bit font style flags (bits 11–14). */
    public static final int FONT_STYLE_MASK = 0b00000000_00000000_01111000_00000000;

    /** Bitmask for extracting the 9-bit foreground color ID (bits 15–23). */
    public static final int FOREGROUND_MASK = 0b00000000_11111111_10000000_00000000;

    /** Bitmask for extracting the 9-bit background color ID (bits 24–32). */
    public static final int BACKGROUND_MASK = 0b11111111_00000000_00000000_00000000;

    /** Bit offset position for language ID (0). */
    public static final int LANGUAGE_ID_OFFSET = 0;

    /** Bit offset position for token type (8). */
    public static final int TOKEN_TYPE_OFFSET = 8;

    /** Bit offset position for balanced brackets flag (10). */
    public static final int BALANCED_BRACKETS_OFFSET = 10;

    /** Bit offset position for font style flags (11). */
    public static final int FONT_STYLE_OFFSET = 11;

    /** Bit offset position for foreground color ID (15). */
    public static final int FOREGROUND_OFFSET = 15;

    /** Bit offset position for background color ID (24). */
    public static final int BACKGROUND_OFFSET = 24;

    private EncodedTokenDataConsts() {
        // utility class
    }
}