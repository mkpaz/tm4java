/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.grammar;

import org.jspecify.annotations.Nullable;
import tm4java.internal.grammar.tokenattrs.EncodedTokenDataConsts;
import tm4java.internal.grammar.tokenattrs.OptionalStandardTokenType;
import tm4java.internal.theme.FontStyle;

/**
 * Utility class providing static methods to manipulate and retrieve encoded token attributes
 * packed into a single 32-bit integer metadata field.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/blob/v9.2.0/src/encodedTokenAttributes.ts#L9">
 * vscode-textmate/src/encodedTokenAttributes.ts#L9</a>
 */
public final class EncodedTokenAttributes {

    private EncodedTokenAttributes() {
        // utility class
    }

    /**
     * Updates the fields in {@code metadata}. A value of {@code 0}, {@code NOT_SET}, or {@code null}
     * indicates that the corresponding field should be left as is.
     *
     * @param metadata                 the current metadata to be updated
     * @param languageId               the language identifier, or {@code 0} to leave unchanged
     * @param tokenType                the standard token type, or {@code OptionalStandardTokenType.NOT_SET}
     *                                 to leave unchanged
     * @param containsBalancedBrackets whether balanced brackets are present, or {@code null} to leave unchanged
     * @param fontStyle                the font style mask, or {@code FontStyle.NOT_SET} to leave unchanged
     * @param foreground               the foreground color ID, or {@code 0} to leave unchanged
     * @param background               the background color ID, or {@code 0} to leave unchanged
     * @return the updated metadata integer with the new values applied
     */
    public static int set(int metadata,
                          int languageId, // OptionalStandardTokenType
                          int tokenType,
                          @Nullable Boolean containsBalancedBrackets,
                          int fontStyle,
                          int foreground,
                          int background
    ) {
        var languageId_ = languageId == 0 ? getLanguageId(metadata) : languageId;
        var tokenType_ = tokenType == OptionalStandardTokenType.NOT_SET ? getTokenType(metadata) : tokenType;
        var containsBalancedBracketsBit_ = (containsBalancedBrackets == null
            ? containsBalancedBrackets(metadata)
            : containsBalancedBrackets) ? 1 : 0;
        var fontStyle_ = fontStyle == FontStyle.NOT_SET ? getFontStyle(metadata) : fontStyle;
        var foreground_ = foreground == 0 ? getForeground(metadata) : foreground;
        var background_ = background == 0 ? getBackground(metadata) : background;

        return languageId_ << EncodedTokenDataConsts.LANGUAGE_ID_OFFSET
            | tokenType_ << EncodedTokenDataConsts.TOKEN_TYPE_OFFSET
            | containsBalancedBracketsBit_ << EncodedTokenDataConsts.BALANCED_BRACKETS_OFFSET
            | fontStyle_ << EncodedTokenDataConsts.FONT_STYLE_OFFSET
            | foreground_ << EncodedTokenDataConsts.FOREGROUND_OFFSET
            | background_ << EncodedTokenDataConsts.BACKGROUND_OFFSET;
    }

    /**
     * Extracts the language identifier from the token metadata.
     *
     * @param metadata the bit-packed token metadata integer
     * @return the extracted language identifier
     */
    public static int getLanguageId(int metadata) {
        return (metadata & EncodedTokenDataConsts.LANGUAGE_ID_MASK) >>> EncodedTokenDataConsts.LANGUAGE_ID_OFFSET;
    }

    /**
     * Extracts the token type from the token metadata.
     *
     * @param metadata the bit-packed token metadata integer
     * @return the extracted token type
     */
    public static int getTokenType(int metadata) {
        return (metadata & EncodedTokenDataConsts.TOKEN_TYPE_MASK) >>> EncodedTokenDataConsts.TOKEN_TYPE_OFFSET;
    }

    /**
     * Checks whether the token metadata indicates the presence of balanced brackets.
     *
     * @param metadata the bit-packed token metadata integer
     * @return {@code true} if balanced brackets are present; {@code false} otherwise
     */
    public static boolean containsBalancedBrackets(int metadata) {
        return (metadata & EncodedTokenDataConsts.BALANCED_BRACKETS_MASK) != 0;
    }

    /**
     * Extracts the font style bitmask from the token metadata.
     *
     * <p>Example usage:
     * <pre>{@code
     * int fontStyle = EncodedTokenAttributes.getFontStyle(metadata);
     * if (EncodedTokenAttributes.isBold(fontStyle)) {
     *     // handle bold style
     * }
     * }</pre>
     *
     * @param metadata the bit-packed token metadata integer
     * @return the extracted font style bitmask
     */
    public static int getFontStyle(int metadata) {
        return (metadata & EncodedTokenDataConsts.FONT_STYLE_MASK) >>> EncodedTokenDataConsts.FONT_STYLE_OFFSET;
    }

    /**
     * Checks whether the specified font style bitmask includes the bold style.
     *
     * @param fontStyle the font style bitmask
     * @return {@code true} if bold style is set; {@code false} otherwise
     */
    public static boolean isBold(int fontStyle) {
        return FontStyle.isBold(fontStyle);
    }

    /**
     * Checks whether the specified font style bitmask includes the italic style.
     *
     * @param fontStyle the font style bitmask
     * @return {@code true} if italic style is set; {@code false} otherwise
     */
    public static boolean isItalic(int fontStyle) {
        return FontStyle.isItalic(fontStyle);
    }

    /**
     * Checks whether the specified font style bitmask includes the underline style.
     *
     * @param fontStyle the font style bitmask
     * @return {@code true} if underline style is set; {@code false} otherwise
     */
    public static boolean isUnderline(int fontStyle) {
        return FontStyle.isUnderline(fontStyle);
    }

    /**
     * Checks whether the specified font style bitmask includes the strikethrough style.
     *
     * @param fontStyle the font style bitmask
     * @return {@code true} if strikethrough style is set; {@code false} otherwise
     */
    public static boolean isStrikethrough(int fontStyle) {
        return FontStyle.isStrikethrough(fontStyle);
    }

    /**
     * Extracts the foreground color identifier from the token metadata.
     *
     * @param metadata the bit-packed token metadata integer
     * @return the extracted foreground color ID
     */
    public static int getForeground(int metadata) {
        return (metadata & EncodedTokenDataConsts.FOREGROUND_MASK) >>> EncodedTokenDataConsts.FOREGROUND_OFFSET;
    }

    /**
     * Extracts the background color identifier from the token metadata.
     *
     * @param metadata the bit-packed token metadata integer
     * @return the extracted background color ID
     */
    public static int getBackground(int metadata) {
        return (metadata & EncodedTokenDataConsts.BACKGROUND_MASK) >>> EncodedTokenDataConsts.BACKGROUND_OFFSET;
    }

    /**
     * Converts the 32-bit token metadata integer to a padded binary string representation.
     *
     * @param metadata the bit-packed token metadata integer
     * @return a 32-character binary string representing the metadata bits
     */
    public static String toBinaryStr(int metadata) {
        return new StringBuilder(Integer.toBinaryString(metadata))
            .insert(0, "0".repeat(Integer.numberOfLeadingZeros(metadata)))
            .toString();
    }

    /**
     * Returns a human-readable string representation of all token attributes encoded within the metadata.
     *
     * @param metadata the bit-packed token metadata integer
     * @return a formatted string listing all decoded metadata fields
     */
    public static String toString(int metadata) {
        return "{"
            + "languageId: '" + getLanguageId(metadata) + "', "
            + "tokenType: '" + getTokenType(metadata) + "', "
            + "fontStyle: '" + getFontStyle(metadata) + "', "
            + "foreground: '" + getForeground(metadata) + "', "
            + "background: '" + getBackground(metadata) + "', "
            + "containsBalancedBrackets: '" + containsBalancedBrackets(metadata) + "'"
            + "}";
    }
}