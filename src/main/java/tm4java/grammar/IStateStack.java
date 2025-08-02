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
 * Represents a "pushed" state on the stack (as a linked list element).
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/main.ts#L258">vscode-textmate/src/main.ts#L258</a>
 */
public interface IStateStack {

    /**
     * Returns the depth of the stack.
     */
    int getDepth();
}
