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
 * Utility class providing methods to manipulate and retrieve encoded token attributes (metadata).
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/encodedTokenAttributes.ts#L9">
 * vscode-textmate/src/encodedTokenAttributes.ts#L9</a>
 */
public final class EncodedTokenAttributes {

    private EncodedTokenAttributes() {
        // utility class
    }

    /**
     * Updates the fields in {@code metadata}. A value of `0`, `NOT_SET` or `null`
     * indicates that the corresponding field should be left as is.
     *
     * @param metadata                 the current metadata to be updated
     * @param languageId               the language identifier
     * @param tokenType                the type of token
     * @param containsBalancedBrackets indicates if balanced brackets are present
     * @param fontStyle                the font style
     * @param foreground               the foreground color
     * @param background               the background color
     * @return the updated metadata with the new values applied
     */
    public static int set(int metadata,
                          int languageId, // OptionalStandardTokenType
                          int tokenType,
                          @Nullable Boolean containsBalancedBrackets,
                          int fontStyle,
                          int foreground,
                          int background) {
        var languageId_ = languageId == 0 ? getLanguageId(metadata) : languageId;
        var tokenType_ = tokenType == OptionalStandardTokenType.NOT_SET ? getTokenType(metadata) : tokenType;
        var containsBalancedBracketsBit_ =
            (containsBalancedBrackets == null ? containsBalancedBrackets(metadata) : containsBalancedBrackets) ? 1 : 0;
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
     * Retrieves the language identifier from the metadata.
     *
     * @param metadata the metadata from which to extract the language id
     */
    public static int getLanguageId(int metadata) {
        return (metadata & EncodedTokenDataConsts.LANGUAGE_ID_MASK) >>> EncodedTokenDataConsts.LANGUAGE_ID_OFFSET;
    }

    /**
     * Retrieves the token type from the metadata.
     *
     * @param metadata the metadata from which to extract the token type
     */
    public static int getTokenType(int metadata) {
        return (metadata & EncodedTokenDataConsts.TOKEN_TYPE_MASK) >>> EncodedTokenDataConsts.TOKEN_TYPE_OFFSET;
    }

    /**
     * Checks if the metadata indicates the presence of balanced brackets.
     *
     * @param metadata the metadata to check
     * @return true if balanced brackets are present, false otherwise
     */
    public static boolean containsBalancedBrackets(int metadata) {
        return (metadata & EncodedTokenDataConsts.BALANCED_BRACKETS_MASK) != 0;
    }

    /**
     * Retrieves the font style from the metadata.
     * <p>
     * The return value can be tested like:
     *
     * <pre>{@code
     * if(EncodedTokenAttributes.isBold(EncodedTokenAttributes.getFontStyle(metadata))){
     *     ...
     * }
     * }</pre>
     *
     * @param metadata the metadata from which to extract the font style
     */
    public static int getFontStyle(int metadata) {
        return (metadata & EncodedTokenDataConsts.FONT_STYLE_MASK) >>> EncodedTokenDataConsts.FONT_STYLE_OFFSET;
    }

    /**
     * Determines whether the specified font style (mask) matches the bold style.
     */
    public static boolean isBold(int fontStyle) { // not from upstream
        return FontStyle.isBold(fontStyle);
    }

    /**
     * Determines whether the specified font style (mask) matches the italic style.
     */
    public static boolean isItalic(int fontStyle) { // not from upstream
        return FontStyle.isItalic(fontStyle);
    }

    /**
     * Determines whether the specified font style (mask) matches the underline style.
     */
    public static boolean isUnderline(int fontStyle) { // not from upstream
        return FontStyle.isUnderline(fontStyle);
    }

    /**
     * Determines whether the specified font style (mask) matches the strikethrough style.
     */
    public static boolean isStrikethrough(int fontStyle) { // not from upstream
        return FontStyle.isStrikethrough(fontStyle);
    }

    /**
     * Retrieves the foreground color from the metadata.
     *
     * @param metadata the metadata from which to extract the foreground color
     */
    public static int getForeground(int metadata) {
        return (metadata & EncodedTokenDataConsts.FOREGROUND_MASK) >>> EncodedTokenDataConsts.FOREGROUND_OFFSET;
    }

    /**
     * Retrieves the background color from the metadata.
     *
     * @param metadata the metadata from which to extract the background color
     */
    public static int getBackground(int metadata) {
        return (metadata & EncodedTokenDataConsts.BACKGROUND_MASK) >>> EncodedTokenDataConsts.BACKGROUND_OFFSET;
    }

    /**
     * Converts the metadata to a binary string representation.
     *
     * @param metadata the metadata to convert
     */
    public static String toBinaryStr(int metadata) {
        return new StringBuilder(Integer.toBinaryString(metadata))
            .insert(0, "0".repeat(Integer.numberOfLeadingZeros(metadata)))
            .toString();
    }

    /**
     * Converts the metadata to a string representation.
     *
     * @param metadata the metadata to convert
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
