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
 * Immutable container associating a scope selector {@link Matcher} with its priority weight.
 *
 * <p>Priority levels are used in TextMate theme matching and scope injection rules to resolve precedence
 * conflicts when multiple rules match the same scope stack.
 *
 * @param <T>      the candidate object type evaluated by the matcher predicate
 * @param matcher  the predicate logic evaluating whether a scope stack matches
 * @param priority the matcher priority weight, typically one of
 * {@code -1} (Low / Left), {@code 0} (Normal), or {@code 1} (High / Right)
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/matcher.ts#L5">
 * vscode-textmate/src/matcher.ts</a>
 */
public record MatcherWithPriority<T>(Matcher<T> matcher, int priority) { }