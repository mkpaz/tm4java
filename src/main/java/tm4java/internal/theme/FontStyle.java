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
 * Font style definitions.
 * <p>
 * See <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/theme.ts#L306">
 * vscode-textmate/theme.ts#L306</a>
 */
public final class FontStyle {

    // these are bit-flags, so it can be `Italic | Bold`
    public static final int NONE = 0;
    public static final int ITALIC = 1;
    public static final int BOLD = 2;
    public static final int UNDERLINE = 4;
    public static final int STRIKETHROUGH = 8;
    public static final int NOT_SET = -1;

    private FontStyle() {
        // utility class
    }

    /**
     * Determines whether the specified font style (mask) matches the bold style.
     */
    public static boolean isBold(int fontStyle) {
        return (fontStyle & BOLD) == BOLD;
    }

    /**
     * Determines whether the specified font style (mask) matches the italic style.
     */
    public static boolean isItalic(int fontStyle) {
        return (fontStyle & ITALIC) == ITALIC;
    }

    /**
     * Determines whether the specified font style (mask) matches the underline style.
     */
    public static boolean isUnderline(int fontStyle) {
        return (fontStyle & UNDERLINE) == UNDERLINE;
    }

    /**
     * Determines whether the specified font style (mask) matches the strikethrough style.
     */
    public static boolean isStrikethrough(int fontStyle) {
        return (fontStyle & STRIKETHROUGH) == STRIKETHROUGH;
    }
}
