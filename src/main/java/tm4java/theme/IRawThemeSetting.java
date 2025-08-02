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
 * Represents a single theme setting.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/theme.ts#L91">
 * vscode-textmate/src/theme.ts#L91</a>
 */
public interface IRawThemeSetting {

    /**
     * Returns the name of the theme setting.
     */
    @Nullable
    String getName();

    /**
     * Returns the scope of this theme setting, which may be a single string
     * or a list of strings.
     */
    @Nullable
    Object getScope();

    /**
     * Returns the style information for this theme setting.
     */
    @Nullable
    IThemeSetting getSetting();
}
