/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.theme;


import org.jspecify.annotations.Nullable;
import tm4java.internal.theme.FontStyle;

import java.util.Objects;

/**
 * Represents styling metadata applied to a token, including font style bitmasks and color
 * palette identifiers.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/theme.ts#L210">
 * vscode-textmate/src/theme.ts#L210</a>
 */
public final class StyleAttributes {

    /**
     * Represents the absence of any style (no font styles and default colors).
     */
    public static final StyleAttributes NO_STYLE = new StyleAttributes(-1, 0, 0);

    private final int fontStyle;
    private final int foregroundId;
    private final int backgroundId;

    /**
     * Creates a new style attribute.
     *
     * @param fontStyle    the font style mask
     * @param foregroundId the foreground color id
     * @param backgroundId the background color id
     * @see FontStyle
     */
    private StyleAttributes(int fontStyle, int foregroundId, int backgroundId) {
        this.fontStyle = fontStyle;
        this.foregroundId = foregroundId;
        this.backgroundId = backgroundId;
    }

    /**
     * Returns the font style bitmask for this attribute set.
     *
     * @return the font style bitmask (e.g. bold, italic, underline, strikethrough)
     * @see FontStyle
     */
    public int fontStyle() {
        return fontStyle;
    }

    /**
     * Returns the foreground color identifier from the theme palette.
     *
     * @return the foreground color ID
     */
    public int foregroundId() {
        return foregroundId;
    }

    /**
     * Returns the background color identifier from the theme palette.
     *
     * @return the background color ID
     */
    public int backgroundId() {
        return backgroundId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(backgroundId, fontStyle, foregroundId);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof StyleAttributes other) {
            return backgroundId == other.backgroundId
                && fontStyle == other.fontStyle
                && foregroundId == other.foregroundId;
        }

        return false;
    }

    @Override
    public String toString() {
        return "StyleAttributes{"
            + "fontStyle=" + fontStyle
            + ", foregroundId=" + foregroundId
            + ", backgroundId=" + backgroundId
            + '}';
    }

    /**
     * Determines whether the style attributes contain a bold font style.
     *
     * @return {@code true} if bold style is set; {@code false} otherwise
     */
    public boolean isBold() { // not from the upstream
        return FontStyle.isBold(fontStyle);
    }

    /**
     * Determines whether the style attributes contain an italic font style.
     *
     * @return {@code true} if italic style is set; {@code false} otherwise
     */
    public boolean isItalic() { // not from the upstream
        return FontStyle.isItalic(fontStyle);
    }

    /**
     * Determines whether the style attributes contain an underline font style.
     *
     * @return {@code true} if underline style is set; {@code false} otherwise
     */
    public boolean isUnderline() { // not from the upstream
        return FontStyle.isUnderline(fontStyle);
    }

    /**
     * Determines whether the style attributes contain a strikethrough font style.
     *
     * @return {@code true} if strikethrough style is set; {@code false} otherwise
     */
    public boolean isStrikethrough() { // not from the upstream
        return FontStyle.isStrikethrough(fontStyle);
    }

    //*************************************************************************

    /**
     * Factory method to create a {@link StyleAttributes} instance or return {@link #NO_STYLE}.
     *
     * @param fontStyle    the font style mask
     * @param foregroundId the foreground color identifier
     * @param backgroundId the background color identifier
     * @return a {@link StyleAttributes} instance matching the specified attributes,
     * or {@link #NO_STYLE} if default/unset values are provided
     */
    public static StyleAttributes of(int fontStyle, int foregroundId, int backgroundId) {
        if (fontStyle == -1 && foregroundId == 0 && backgroundId == 0) {
            return NO_STYLE;
        }
        return new StyleAttributes(fontStyle, foregroundId, backgroundId);
    }
}