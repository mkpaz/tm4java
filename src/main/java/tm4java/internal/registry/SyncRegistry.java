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
import tm4java.grammar.BalancedBracketSelectors;
import tm4java.grammar.IGrammar;
import tm4java.grammar.IGrammarSource;
import tm4java.internal.grammar.Grammar;
import tm4java.internal.grammar.ScopeStack;
import tm4java.internal.grammar.raw.IRawGrammar;
import tm4java.internal.theme.Theme;
import tm4java.registry.Registry;
import tm4java.theme.IThemeSource;
import tm4java.theme.StyleAttributes;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The default implementation for {@link IGrammarRepository}, {@link IThemeProvider},
 * and the delegate for the {@link Registry}.
 *
 * <p>Manages synchronous registration, lookup, and instantiation of TextMate grammars and themes.
 * Acts as the centralized cache and resolver linking parsed {@link IRawGrammar} definitions to active
 * compiled {@link Grammar} instances.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/registry.ts#L11">
 * vscode-textmate/src/registry.ts#L11</a>
 */
public class SyncRegistry implements IGrammarRepository, IThemeProvider {

    /** Cache mapping scope names to compiled active {@link Grammar} instances. */
    private final Map<String, Grammar> grammars = new HashMap<>();

    /** Map storing raw parsed grammar definitions indexed by scope name. */
    private final Map<String, @Nullable IRawGrammar> rawGrammars = new HashMap<>();

    /** Map storing injection scope names targeted at specific base scope names. */
    private final Map<String, Collection<String>> injectionGrammars = new HashMap<>();

    /** The active TextMate theme used for scope style matching and palette resolution. */
    private Theme theme;

    /**
     * Constructs a new {@code SyncRegistry} with the initial active theme.
     *
     * @param theme the initial {@link Theme} instance
     */
    public SyncRegistry(Theme theme) {
        this.theme = theme;
    }

    //*************************************************************************
    // IGrammarRepository
    //*************************************************************************

    /**
     * {@inheritDoc}
     *
     * @param scopeName the scope name of the grammar to look up (e.g. {@code "source.java"})
     * @return the raw grammar matching the scope name, or {@code null} if not registered
     */
    @Override
    public @Nullable IRawGrammar lookup(String scopeName) {
        return rawGrammars.get(scopeName);
    }

    /**
     * {@inheritDoc}
     *
     * @param targetScope the target scope name for which injection scopes are requested
     * @return a collection of scope names for injections targeting {@code targetScope}, or {@code null}
     */
    @Override
    public @Nullable Collection<String> injections(String targetScope) {
        return injectionGrammars.get(targetScope);
    }

    //*************************************************************************
    // IThemeProvider
    //*************************************************************************

    /**
     * {@inheritDoc}
     *
     * @return the default style attributes configured in the active theme
     */
    @Override
    public StyleAttributes getDefaults() {
        return theme.getDefaults();
    }

    /**
     * {@inheritDoc}
     *
     * @param scopePath the stack of active scope selectors to match against the theme
     * @return the resolved {@link StyleAttributes} for the scope path, or {@code null} if unmatched
     */
    @Override
    public @Nullable StyleAttributes themeMatch(ScopeStack scopePath) {
        return theme.match(scopePath);
    }

    //*************************************************************************

    /**
     * Updates the active theme used by this registry.
     *
     * @param theme the new {@link Theme} to set
     * @see Registry#setTheme(IThemeSource)
     */
    public void setTheme(Theme theme) {
        this.theme = theme;
    }

    /**
     * Returns the color palette list from the active theme.
     *
     * @return the unmodifiable list of color strings
     * @see Registry#getColorMap()
     */
    public List<String> getColorMap() {
        return theme.getColorMap();
    }

    /**
     * Registers a raw grammar and optional injection scope bindings into the repository.
     *
     * <p>Clears any previously cached compiled {@link Grammar} instance for the grammar's scope name.
     *
     * @param grammar             the raw grammar instance to register
     * @param injectionScopeNames optional collection of injection scope names targeting this grammar
     * @see Registry#addGrammar(IGrammarSource, List, Integer, Map)
     */
    public void addGrammar(IRawGrammar grammar, @Nullable Collection<String> injectionScopeNames) {
        rawGrammars.put(grammar.getScopeName(), grammar);

        // Not from the upstream:
        // if an IRawGrammar is re-registered under the same scope name,
        // clear any cached grammar so it will be rebuilt with the new definition
        grammars.remove(grammar.getScopeName());

        if (injectionScopeNames != null) {
            injectionGrammars.put(grammar.getScopeName(), injectionScopeNames);
        }
    }

    /**
     * Obtains or creates a compiled {@link IGrammar} instance for the specified scope name.
     *
     * @param scopeName                the target scope name to compile
     * @param initialLanguage          the initial numeric language ID
     * @param embeddedLanguages        optional mapping of scope selectors to embedded language IDs
     * @param tokenTypes               optional mapping of scope selectors to standard token types
     * @param balancedBracketSelectors optional selector configuration for balanced bracket matching
     * @return a compiled {@link IGrammar} instance, or {@code null} if the raw grammar is not found
     */
    public @Nullable IGrammar grammarForScopeName(String scopeName,
                                                  int initialLanguage,
                                                  @Nullable Map<String, Integer> embeddedLanguages,
                                                  @Nullable Map<String, Integer> tokenTypes,
                                                  @Nullable BalancedBracketSelectors balancedBracketSelectors) {
        return grammars.computeIfAbsent(scopeName, scopeName_ -> {
            var rawGrammar = lookup(scopeName_);
            if (rawGrammar == null) {
                return null;
            }

            return new Grammar(
                scopeName_,
                rawGrammar,
                initialLanguage,
                embeddedLanguages,
                tokenTypes,
                balancedBracketSelectors,
                this,
                this
            );
        });
    }
}