/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.grammar;

import tm4java.grammar.ITokenizeLineResult;

/**
 * Result of the line tokenization implementation.
 *
 * @param <T>          the concrete type of token sequence returned
 * @param tokens       the collection of tokens generated for the line
 * @param ruleStack    the state stack representing the grammar rules active at the end of the line
 * @param stoppedEarly {@code true} if tokenization was halted before reaching the end of the line
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/main.ts#L219">
 * vscode-textmate/src/main.ts#L219</a>
 */
record TokenizeLineResult<T>(T tokens, StateStack ruleStack, boolean stoppedEarly)
    implements ITokenizeLineResult<T> { }