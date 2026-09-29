/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.grammar;

import tm4java.internal.grammar.matcher.Matcher;

import java.util.List;

/**
 * Associates a token match predicate with its target standard token type.
 *
 * @param matcher the predicate matcher operating on list of scope names
 * @param type    the target standard token type (see {@code StandardTokenType})
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/grammar/grammar.ts#L893">
 * vscode-textmate/src/grammar/grammar.ts#L893</a>
 */
record TokenTypeMatcher(Matcher<List<String>> matcher, int type /*StandardTokenType*/) { }
