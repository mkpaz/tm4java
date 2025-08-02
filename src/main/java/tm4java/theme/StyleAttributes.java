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
 * Represents style attributes for a theme.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/theme.ts#L190">
 * vscode-textmate/src/theme.ts#L190</a>
 */
public final class StyleAttributes {

    /**
     * A style attribute with no style.
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

    public int fontStyle() {
        return fontStyle;
    }

    public int foregroundId() {
        return foregroundId;
    }

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
     */
    public boolean isBold() { // not from upstream
        return FontStyle.isBold(fontStyle);
    }

    /**
     * Determines whether the style attributes contain an italic font style.
     */
    public boolean isItalic() { // not from upstream
        return FontStyle.isItalic(fontStyle);
    }

    /**
     * Determines whether the style attributes contain an underline font style.
     */
    public boolean isUnderline() { // not from upstream
        return FontStyle.isUnderline(fontStyle);
    }

    /**
     * Determines whether the style attributes contain a strikethrough font style.
     */
    public boolean isStrikethrough() { // not from upstream
        return FontStyle.isStrikethrough(fontStyle);
    }

    //*************************************************************************

    public static StyleAttributes of(int fontStyle, int foregroundId, int backgroundId) {
        if (fontStyle == -1 && foregroundId == 0 && backgroundId == 0) {
            return NO_STYLE;
        }
        return new StyleAttributes(fontStyle, foregroundId, backgroundId);
    }
}
