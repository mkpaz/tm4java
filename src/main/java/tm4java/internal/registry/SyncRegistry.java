/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.registry;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import tm4java.grammar.IGrammar;
import tm4java.grammar.BalancedBracketSelectors;
import tm4java.internal.grammar.Grammar;
import tm4java.internal.grammar.ScopeStack;
import tm4java.internal.grammar.raw.IRawGrammar;
import tm4java.theme.StyleAttributes;
import tm4java.internal.theme.Theme;
import tm4java.grammar.IGrammarSource;
import tm4java.theme.IThemeSource;
import tm4java.registry.Registry;

/**
 * The default implementation for {@link IGrammarRepository}, {@link IThemeProvider}.
 * and the delegate for the {@link Registry}.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/registry.ts#L11">
 * vscode-textmate/src/registry.ts#L11</a>
 */
public class SyncRegistry implements IGrammarRepository, IThemeProvider {

    private final Map<String, Grammar> grammars = new HashMap<>();
    private final Map<String, @Nullable IRawGrammar> rawGrammars = new HashMap<>();
    private final Map<String, Collection<String>> injectionGrammars = new HashMap<>();
    private Theme theme;

    public SyncRegistry(Theme theme) {
        this.theme = theme;
    }

    //*************************************************************************
    // IGrammarRepository
    //*************************************************************************

    @Override
    public @Nullable IRawGrammar lookup(String scopeName) {
        return rawGrammars.get(scopeName);
    }

    @Override
    public @Nullable Collection<String> injections(String targetScope) {
        return injectionGrammars.get(targetScope);
    }

    //*************************************************************************
    // IThemeProvider
    //*************************************************************************

    @Override
    public StyleAttributes getDefaults() {
        return theme.getDefaults();
    }

    @Override
    public @Nullable StyleAttributes themeMatch(ScopeStack scopePath) {
        return theme.match(scopePath);
    }

    //*************************************************************************

    /**
     * See {@link Registry#setTheme(IThemeSource)}.
     */
    public void setTheme(Theme theme) {
        this.theme = theme;
    }

    /**
     * See {@link Registry#getColorMap()}.
     */
    public List<String> getColorMap() {
        return theme.getColorMap();
    }

    /**
     * See {@link Registry#addGrammar(IGrammarSource, List, Integer, Map)}.
     */
    public void addGrammar(IRawGrammar grammar, @Nullable Collection<String> injectionScopeNames) {
        rawGrammars.put(grammar.getScopeName(), grammar);

        // not from upstream:
        // if an IRawGrammar is re-registered under the same scope name,
        // clear any cached grammar so it will be rebuilt with the new definition
        grammars.remove(grammar.getScopeName());

        if (injectionScopeNames != null) {
            injectionGrammars.put(grammar.getScopeName(), injectionScopeNames);
        }
    }

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
