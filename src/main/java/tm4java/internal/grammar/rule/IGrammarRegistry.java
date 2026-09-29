/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.grammar.rule;

import org.jspecify.annotations.Nullable;
import tm4java.internal.grammar.raw.IRawGrammar;
import tm4java.internal.grammar.raw.IRawRepository;

/**
 * A registry for managing and retrieving grammars.
 *
 * <p>Provides lookups for external TextMate grammars by scope name and repository.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/rule.ts#L36">
 * vscode-textmate/src/rule.ts#L36</a>
 */
interface IGrammarRegistry {

    /**
     * Retrieves an external raw grammar for the given scope name and repository.
     *
     * @param scopeName  the scope name of the grammar
     * @param repository the repository to search in
     * @return the external raw grammar, or null if not found
     */
    @Nullable
    IRawGrammar getExternalGrammar(String scopeName, IRawRepository repository);
}