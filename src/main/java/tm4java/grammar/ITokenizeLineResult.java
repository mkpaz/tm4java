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
 * Represents the result of tokenizing a single line of text.
 *
 * <p>Contains the extracted line tokens, the updated grammar state stack to be passed to the next line,
 * and an execution flag indicating whether tokenization completed or was aborted early due to a time limit.
 *
 * @param <T> the representation type of tokens:
 *            {@code IToken[]} for standard tokenization via {@link IGrammar#tokenizeLine(String)}, or
 *            {@code int[]} for binary-encoded tokenization via {@link IGrammar#tokenizeLine2(String)}
 * @see <a href="https://github.com/microsoft/vscode-textmate/blob/v9.2.0/src/main.ts#L219">
 * vscode-textmate/src/main.ts#L219</a>
 * @see IGrammar#tokenizeLine(String, IStateStack, Duration)
 * @see IGrammar#tokenizeLine2(String, IStateStack, Duration)
 */
public interface ITokenizeLineResult<T> {

    /**
     * Returns the parsed tokens for the processed line.
     *
     * <p>For standard tokenization, this returns an array of {@link IToken} instances.
     * For encoded tokenization, this returns a flat {@code int[]} array of bit-packed token metadata.
     *
     * @return the tokenization payload ({@code IToken[]} or {@code int[]})
     */
    T tokens();

    /**
     * Returns the grammar state stack captured at the end of the line.
     *
     * <p>This stack MUST be passed as the {@code prevState} argument when tokenizing the subsequent line
     * to maintain context across line boundaries.
     *
     * @return the resulting line state stack
     */
    IStateStack ruleStack();

    /**
     * Indicates whether tokenization was prematurely aborted due to exceeding the maximum allowed duration.
     *
     * <p>If {@code true}, the returned tokens only represent the portion of the line processed before
     * reaching the timeout.
     *
     * @return {@code true} if tokenization hit the time limit and stopped early; {@code false} if
     * the entire line was tokenized
     */
    boolean stoppedEarly();
}
