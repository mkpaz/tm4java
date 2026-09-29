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
import tm4java.grammar.IGrammar;
import tm4java.internal.grammar.raw.IRawGrammar;

import java.util.Collection;

/**
 * Repository interface for resolving raw TextMate grammars and their cross-grammar injections.
 *
 * <p>Acts as a primary entry point or delegate for loading raw grammar definitions
 * (typically parsed from JSON or Plist format) before they are compiled into full {@link IGrammar}
 * instances.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/grammar/grammar.ts#L44">
 * vscode-textmate/src/grammar/grammar.ts#L44</a>
 */
public interface IGrammarRepository {

    /**
     * Looks up a raw grammar definition associated with the given scope name.
     *
     * @param scopeName the scope name of the grammar to retrieve (e.g., {@code "source.java"})
     * @return the corresponding {@link IRawGrammar} instance, or {@code null} if no grammar
     *         is registered for the specified scope name
     */
    @Nullable IRawGrammar lookup(String scopeName);

    /**
     * Retrieves a collection of injection scope names applicable to the specified target scope.
     *
     * <p>Injections allow external grammars to inject additional tokenization rules into
     * specific matching scopes without modifying the target grammar directly.
     *
     * @param targetScope the scope name to check for registered injections (e.g., {@code "source.js"})
     * @return a collection of scope names for grammars that inject into the target scope,
     *         or {@code null} (or an empty collection) if no injections are defined
     */
    @Nullable Collection<String> injections(String targetScope);
}