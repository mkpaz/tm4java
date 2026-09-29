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

import java.util.List;

/**
 * Represents a raw entry inside a TextMate theme definition (an element of the {@code settings} array).
 *
 * <p>A theme setting maps one or more scope selectors to a set of visual style attributes.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/theme.ts#L91">
 * vscode-textmate/src/theme.ts#L91</a>
 */
public interface IRawThemeSetting {

    /**
     * Returns the human-readable name of this theme setting rule.
     *
     * @return the setting name, or {@code null} if unnamed
     */
    @Nullable
    String getName();

    /**
     * Returns the scope selector or list of scope selectors to which this setting applies.
     *
     * <p>The return value can be a single {@link String} or a {@link List} of strings.
     *
     * @return the scope selector(s), or {@code null} if this setting applies globally
     */
    @Nullable
    Object getScope();

    /**
     * Returns the style attributes (font style, foreground, background) associated with this setting.
     *
     * @return the {@link IThemeSetting} object containing style properties, or {@code null} if missing
     */
    @Nullable
    IThemeSetting getSetting();
}