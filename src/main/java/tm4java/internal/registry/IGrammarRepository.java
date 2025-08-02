/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.registry;

import org.jspecify.annotations.Nullable;
import tm4java.internal.grammar.raw.IRawGrammar;

import java.util.Collection;

/**
 * Represents the grammar repository.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/grammar/grammar.ts#L44">
 * vscode-textmate/src/grammar/grammar.ts#L44</a>
 */
public interface IGrammarRepository {

    /**
     * Looks up a raw grammar with the specified scope name in the repository.
     *
     * @param scopeName the name of the scope for the grammar to be looked up
     */
    @Nullable
    IRawGrammar lookup(String scopeName);

    /**
     * Returns the injections for the given grammar.
     *
     * @param targetScope the scope of the grammar for which to retrieve injections
     */
    @Nullable
    Collection<String> injections(String targetScope);
}
