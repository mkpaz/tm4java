/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.registry;

import org.jspecify.annotations.Nullable;
import tm4java.internal.grammar.ScopeStack;
import tm4java.theme.StyleAttributes;
import tm4java.internal.theme.Theme;

/**
 * The theme provider exposes an API for obtaining theme styles.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/grammar/grammar.ts#L39">
 * vscode-textmate/src/grammar/grammar.ts#L39</a>
 */
public interface IThemeProvider {

    /**
     * Returns the theme style attributes for a given scope.
     * <p>
     * This method looks up the most specific theme rule for the given scope
     * and returns the style attributes set for this rule if any.
     *
     * @param scopePath the scope stack used to determine the most specific theme rule
     */
    @Nullable
    StyleAttributes themeMatch(ScopeStack scopePath);

    /**
     * Returns the default theme style attributes that are applied to a
     * scope unless overridden by more specific theme rules.
     * <p>
     * See {@link Theme#getDefaults()}.
     * <p>
     * Theme defaults is the "settings" key with an empty scope.
     * <pre>{@code
     * <key>settings</key>
     * <dict>
     *     <key>background</key>
     *     <string>#FDF6E3</string>
     *     ...
     * </dict>
     * }</pre>
     */
    StyleAttributes getDefaults();
}
