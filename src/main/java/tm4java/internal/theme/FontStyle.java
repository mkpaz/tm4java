/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.theme;

/**
 * Font style definitions and bitmask constants.
 *
 * <p>Defines bit-flag constants representing text decorations and font variants
 * (e.g. bold, italic, underline, strikethrough) used by TextMate themes.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/theme.ts#L326">
 * vscode-textmate/theme.ts#L326</a>
 */
public final class FontStyle {

    //*************************************************************************
    // NOTE: these are bit-flags, so it can be `Italic | Bold`
    //*************************************************************************

    /** Represents regular text formatting without any extra font style modifiers (bitmask {@code 0}). */
    public static final int NONE = 0;

    /** Bitflag for italic font style (bitmask {@code 0x1}). */
    public static final int ITALIC = 1;

    /** Bitflag for bold font weight (bitmask {@code 0x2}). */
    public static final int BOLD = 2;

    /** Bitflag for underline text decoration (bitmask {@code 0x4}). */
    public static final int UNDERLINE = 4;

    /** Bitflag for strikethrough text decoration (bitmask {@code 0x8}). */
    public static final int STRIKETHROUGH = 8;

    /** Sentinel value indicating that font style settings were not specified (value {@code -1}). */
    public static final int NOT_SET = -1;

    private FontStyle() {
        // utility class
    }

    /**
     * Determines whether the specified font style (mask) matches the bold style.
     *
     * @param fontStyle the font style bitmask to evaluate
     * @return {@code true} if the bold bit is set in the mask; {@code false} otherwise
     */
    public static boolean isBold(int fontStyle) {
        return (fontStyle & BOLD) == BOLD;
    }

    /**
     * Determines whether the specified font style (mask) matches the italic style.
     *
     * @param fontStyle the font style bitmask to evaluate
     * @return {@code true} if the italic bit is set in the mask; {@code false} otherwise
     */
    public static boolean isItalic(int fontStyle) {
        return (fontStyle & ITALIC) == ITALIC;
    }

    /**
     * Determines whether the specified font style (mask) matches the underline style.
     *
     * @param fontStyle the font style bitmask to evaluate
     * @return {@code true} if the underline bit is set in the mask; {@code false} otherwise
     */
    public static boolean isUnderline(int fontStyle) {
        return (fontStyle & UNDERLINE) == UNDERLINE;
    }

    /**
     * Determines whether the specified font style (mask) matches the strikethrough style.
     *
     * @param fontStyle the font style bitmask to evaluate
     * @return {@code true} if the strikethrough bit is set in the mask; {@code false} otherwise
     */
    public static boolean isStrikethrough(int fontStyle) {
        return (fontStyle & STRIKETHROUGH) == STRIKETHROUGH;
    }
}