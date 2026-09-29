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

/**
 * Provides theme style attributes (such as colors and font styles) for token scopes.
 *
 * <p>The theme provider resolves scope hierarchy against active theme rules to calculate
 * the effective visual styling for tokenized source text.
 *
 * @see StyleAttributes
 * @see ScopeStack
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/grammar/grammar.ts#L39">
 * vscode-textmate/src/grammar/grammar.ts#L39</a>
 */
public interface IThemeProvider {

    /**
     * Resolves and returns the most specific theme style attributes for the given scope stack.
     *
     * <p>This method evaluates active theme matching rules against the provided {@link ScopeStack}
     * hierarchy, adhering to TextMate scope selector specificity rules.
     *
     * @param scopePath the current scope stack representing the token context
     * @return the resolved {@link StyleAttributes} for the matching rule, or {@code null}
     *         if no specific theme rules match the scope stack
     */
    @Nullable
    StyleAttributes themeMatch(ScopeStack scopePath);

    /**
     * Returns the default theme style attributes applied when no scope-specific rules match.
     *
     * <p>Default attributes represent global theme settings (e.g., global foreground/background colors),
     * corresponding to a theme rule with an empty scope selector.
     *
     * <pre>{@code
     * <key>settings</key>
     * <dict>
     *     <key>foreground</key>
     *     <string>#000000</string>
     *     <key>background</key>
     *     <string>#FDF6E3</string>
     * </dict>
     * }</pre>
     *
     * @return the default fallback {@link StyleAttributes} for the active theme
     */
    StyleAttributes getDefaults();
}