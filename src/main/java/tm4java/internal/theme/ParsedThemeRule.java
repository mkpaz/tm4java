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
 * @param scope        the scope of the rule
 * @param parentScopes the parent scopes of the rule
 * @param index        the index of the rule
 * @param fontStyle    a mask of FontStyle or -1 if not set
 * @param foreground   the foreground color
 * @param background   the background color
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/theme.ts#L294">
 * vscode-textmate/theme.ts#L294</a>
 */
public record ParsedThemeRule(String scope,
                              @Nullable List<String> parentScopes,
                              int index,
                              int fontStyle,
                              @Nullable String foreground,
                              @Nullable String background) {
}
