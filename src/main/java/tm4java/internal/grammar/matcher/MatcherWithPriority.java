/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.grammar.matcher;

/**
 * @param priority the matcher priority, one of -1 | 0 | 1
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/matcher.ts#L5">
 * vscode-textmate/src/matcher.ts</a>
 */
public record MatcherWithPriority<T>(Matcher<T> matcher, int priority) {
}
