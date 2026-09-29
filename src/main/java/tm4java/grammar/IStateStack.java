/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.grammar;

/**
 * Represents an immutable node in a linked-stack tracking the active TextMate grammar states.
 *
 * <p>TextMate tokenization operates line-by-line. An {@code IStateStack} instance carries the exact
 * grammar context (such as open block comments, string literals, or embedded languages) from the end
 * of one line to the beginning of the next.
 *
 * <p>Because state stacks are persistent and immutable single-linked structures, two line states can be
 * efficiently compared using {@link Object#equals(Object)} to determine if incremental re-tokenization
 * can be safely stopped early.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/blob/v9.2.0/src/main.ts#L258">
 * vscode-textmate/src/main.ts#L258</a>
 */
public interface IStateStack {

    /**
     * Returns the total depth of state rules currently pushed on this stack.
     *
     * <p>A depth of 1 represents the root grammar scope context.
     *
     * @return the depth of the state stack (always {@code >= 1})
     */
    int getDepth();
}
