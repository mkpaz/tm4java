/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.theme;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A parsed theme rule.
 *
 * <p>Represents an intermediate rule representation extracted directly from raw theme settings
 * (e.g., TextMate theme JSON/Plist rules) before being compiled into the trie index ({@link ThemeTrieElement}).
 *
 * @param scope        the target scope selector of the rule (e.g. {@code "entity.name.function"})
 * @param parentScopes the optional list of parent scope selectors for contextual matching
 * @param index        the zero-based declaration index of the rule in the raw theme settings
 * @param fontStyle    a mask of {@link FontStyle} flags, or {@link FontStyle#NOT_SET} (-1) if unspecified
 * @param foreground   the raw HEX foreground color string (e.g. {@code "#FF0000"}), or {@code null} if inherited
 * @param background   the raw HEX background color string (e.g. {@code "#000000"}), or {@code null} if inherited
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/theme.ts#L314">
 * vscode-textmate/theme.ts#L314</a>
 */
public record ParsedThemeRule(String scope,
                              @Nullable List<String> parentScopes,
                              int index,
                              int fontStyle,
                              @Nullable String foreground,
                              @Nullable String background) { }