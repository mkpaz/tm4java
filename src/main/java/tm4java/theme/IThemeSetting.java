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
 * Represents style information for a theme setting.
 */
public interface IThemeSetting {

    /**
     * Returns the font style. Possible values include:
     * <ul>
     *   <li>{@code "italic"}</li>
     *   <li>{@code "bold"}</li>
     *   <li>{@code "underline"}</li>
     *   <li>{@code "strikethrough"}</li>
     * </ul>
     */
    @Nullable
    String getFontStyle();

    /**
     * Returns the background color in HEX format.
     */
    @Nullable
    String getBackground();

    /**
     * Returns the foreground color in HEX format.
     */
    @Nullable
    String getForeground();
}
