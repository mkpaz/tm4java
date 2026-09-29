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
 * Represents a raw TextMate theme structure loaded directly from a JSON or Plist source.
 *
 * <p>A raw theme consists of a collection of setting rules (global defaults and scope-specific styles)
 * and an optional map of workbench editor color customizations.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/theme.ts#L83">
 * vscode-textmate/src/theme.ts#L83</a>
 */
public interface IRawTheme {

    /**
     * Returns the human-readable name of the theme.
     *
     * @return the theme name, or {@code null} if unset
     */
    @Nullable
    String getName();

    /**
     * Returns the list of theme rules defined in the {@code settings} array.
     *
     * <p>An entry without a scope specifies the default theme attributes
     * (e.g. general editor background/foreground).
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
     * ]
     * }</pre>
     *
     * @return a collection of {@link IRawThemeSetting} entries, or {@code null} if missing
     */
    @Nullable
    Collection<IRawThemeSetting> getSettings();

    /**
     * Returns the workbench editor UI color map defined in the theme (e.g., UI token keys mapped to HEX values).
     *
     * <p>Custom extension method, not present in the upstream library.
     *
     * @return a map of UI color key-value pairs
     */
    Map<String, String> getEditorColors();
}
