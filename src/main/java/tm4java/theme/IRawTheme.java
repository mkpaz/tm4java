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

import java.util.Collection;
import java.util.Map;


/**
 * Represents a raw TextMate theme.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/theme.ts#L91">
 * vscode-textmate/src/theme.ts#L9</a>
 */
public interface IRawTheme {

    /**
     * Returns the value of the {@code name} property of the theme.
     */
    @Nullable
    String getName();

    /**
     * Returns the value of the {@code settings} property of the grammar.
     *
     * <p>This is the top-level dictionary containing all theme rules.
     * A theme rule without a scope represents the default settings.
     *
     * <pre>{@code
     * "settings": [
     *     {
     *         "settings": {
     *             "foreground": "#D4D4D4",
     *             "background": "#1E1E1E"
     *         }
     *     },
     *     {
     *         "scope": "emphasis",
     *         "settings": {
     *             "fontStyle": "italic"
     *         }
     *     }
     * }
     * }</pre>
     */
    @Nullable
    Collection<IRawThemeSetting> getSettings();

    /**
     * Returns the editor colors for this theme.
     * <p>
     * Custom code, not from the upstream.
     */
    Map<String, String> getEditorColors();
}
