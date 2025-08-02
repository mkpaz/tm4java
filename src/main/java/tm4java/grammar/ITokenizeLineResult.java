/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.grammar;

import java.time.Duration;

/**
 * Represents the result of line tokenization.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/main.ts#L219">vscode-textmate/src/main.ts#L219</a>
 */
public interface ITokenizeLineResult<T> {

    /**
     * Returns the result of line tokenization as an array of tokens {@code IToken[]}.
     * See {@link IToken}.
     */
    T tokens();

    /**
     * Returns the {@code prevState} to be passed to the next line tokenization.
     * See {@link IGrammar#tokenizeLine(String, IStateStack, Duration)}.
     */
    IStateStack ruleStack();

    /**
     * Returns whether the tokenization stopped early due to reaching the time limit.
     * See {@link IGrammar#tokenizeLine(String, IStateStack, Duration)}.
     */
    boolean stoppedEarly();
}
