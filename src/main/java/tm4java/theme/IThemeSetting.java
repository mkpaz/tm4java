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

/**
 * Represents the style properties (font style, foreground, and background colors) contained
 * within a theme rule.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/theme.ts#L10">
 * vscode-textmate/src/theme.ts#L10</a>
 */
public interface IThemeSetting {

    /**
     * Returns space-separated font style modifiers or CSS font style declarations.
     *
     * <p>Expected values include combinations of:
     * <ul>
     * <li>{@code "italic"}</li>
     * <li>{@code "bold"}</li>
     * <li>{@code "underline"}</li>
     * <li>{@code "strikethrough"}</li>
     * </ul>
     *
     * @return the font style string, or {@code null} if unset
     */
    @Nullable String getFontStyle();

    /**
     * Returns the background color string in hexadecimal format
     * (e.g. {@code "#282C34"} or {@code "#282C34FF"}).
     *
     * @return the background color hex string, or {@code null} if unset
     */
    @Nullable String getBackground();

    /**
     * Returns the foreground color string in hexadecimal format
     * (e.g. {@code "#ABB2BF"} or {@code "#ABB2BFFF"}).
     *
     * @return the foreground color hex string, or {@code null} if unset
     */
    @Nullable String getForeground();
}