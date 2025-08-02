/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.registry;

import org.jspecify.annotations.Nullable;
import tm4java.TMException;
import tm4java.grammar.*;
import tm4java.internal.grammar.dependencies.ScopeDependencyProcessor;
import tm4java.internal.grammar.raw.RawGrammar;
import tm4java.internal.registry.SyncRegistry;
import tm4java.internal.theme.RawTheme;
import tm4java.internal.theme.Theme;
import tm4java.theme.ITheme;
import tm4java.theme.IThemeSource;

import java.lang.System.Logger;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;

import static java.lang.System.Logger.Level.WARNING;
import static tm4java.internal.utils.NullSafetyHelper.castNonNull;

/**
 * The registry is the main entry point for working with {@code tm4java}.
 * <p>
 * It holds all the grammars and is linked to the current theme.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/main.ts#L54">
 * vscode-textmate/src/main.ts#L54</a>
 */
public final class Registry {

    private static final Logger LOGGER = System.getLogger(Registry.class.getName());

    private final IRegistryOptions options;
    private final SyncRegistry syncRegistry;
    private final Map<String, Boolean> ensureGrammarCache = new HashMap<>();
    private final List<Consumer<ITheme>> themeCallbacks = new ArrayList<>(); // not from upstream
    private final List<Consumer<IGrammar>> grammarCallbacks = new ArrayList<>(); // not from upstream

    public Registry() {
        this(IRegistryOptions.createDefault());
    }

    public Registry(IRegistryOptions options) {
        this.options = options;
        this.syncRegistry = new SyncRegistry(Theme.createFromRawTheme(
            options.getTheme(),
            options.getColorMap()
        ));
    }

    /**
     * Sets the current theme.
     * <p>
     * Once called, no previous rule {@link IStateStack} in{@link IGrammar#tokenizeLine(String, IStateStack, Duration)}
     * should be used.
     */
    public ITheme setTheme(IThemeSource source) throws TMException {
        try {
            var theme = Theme.createFromRawTheme(RawTheme.read(source), options.getColorMap());
            syncRegistry.setTheme(theme);
            themeCallbacks.forEach(c -> c.accept(theme));
            return theme;
        } catch (Exception e) {
            throw new TMException("Loading theme from '" + source.getURI() + "' failed: " + e.getMessage(), e);
        }
    }

    /**
     * Returns a lookup array for theme colors.
     * <p>
     * It contains all the colors specified by the current theme.
     */
    public List<String> getColorMap() {
        return syncRegistry.getColorMap();
    }

    /**
     * Reads the specified grammar from the given source and loads it into the repository.
     * <p>
     * See {@link #addGrammar(IGrammarSource, List, Integer, Map)}.
     */
    public IGrammar addGrammar(IGrammarSource source) throws TMException {
        return addGrammar(source, null, null, null);
    }

    /**
     * Reads the specified grammar from the given source and loads it into the repository.
     * <p>
     * You can load an already added grammar with different options via
     * {@link #loadGrammarWithConfiguration(String, int, IGrammarConfiguration)}.
     *
     * @param source            the input source of the grammar
     * @param injections        optional list of injected grammars,
     *                          see {@link IRegistryOptions#getInjections(String)}
     * @param initialLanguage   optional initial language identifier
     * @param embeddedLanguages optional map of embedded languages,
     *                          see {@link IGrammarConfiguration#getEmbeddedLanguages()}
     * @return the loaded grammar
     */
    public IGrammar addGrammar(IGrammarSource source,
                               @Nullable List<String> injections,
                               @Nullable Integer initialLanguage,
                               @Nullable Map<String, Integer> embeddedLanguages) throws TMException {
        try {
            var rawGrammar = RawGrammar.from(source);
            syncRegistry.addGrammar(
                rawGrammar,
                injections == null || injections.isEmpty()
                    ? options.getInjections(rawGrammar.getScopeName())
                    : injections);

            var grammar = castNonNull(grammarForScopeName(
                rawGrammar.getScopeName(), initialLanguage, embeddedLanguages, null, null
            ));
            grammarCallbacks.forEach(c -> c.accept(grammar));

            return grammar;
        } catch (Exception e) {
            throw new TMException("Loading grammar from [" + source.getURI() + "] failed: " + e.getMessage(), e);
        }
    }

    /**
     * Loads the specified existing (read) grammar and all referenced included grammars
     * from the registry for the given initial scope name with the specified configuration options.
     * <p>
     * See {@link #loadGrammar(String, int, Map, Map, BalancedBracketSelectors)}.
     * <p>
     * The language ID 0 is reserved for the root grammar; do not use it.
     */
    public @Nullable IGrammar loadGrammarWithConfiguration(String initialScopeName,
                                                           int initialLanguage,
                                                           IGrammarConfiguration configuration) {
        return loadGrammar(
            initialScopeName,
            initialLanguage,
            configuration.getEmbeddedLanguages(),
            configuration.getTokenTypes(),
            new BalancedBracketSelectors(
                Objects.requireNonNullElse(configuration.getBalancedBracketSelectors(), List.of()),
                Objects.requireNonNullElse(configuration.getUnbalancedBracketSelectors(), List.of())
            )
        );
    }

    /**
     * Loads the specified existing (read) grammar and all referenced included grammars
     * from the registry for the given initial scope name with the specified configuration options.
     * <p>
     * The language ID 0 is reserved for the root grammar; do not use it.
     */
    public @Nullable IGrammar loadGrammar(String initialScopeName) {
        return loadGrammar(initialScopeName, 0, null, null, null);
    }

    private @Nullable IGrammar loadGrammar(String initialScopeName,
                                           int initialLanguage,
                                           @Nullable Map<String, Integer> embeddedLanguages,
                                           @Nullable Map<String, Integer> tokenTypes,
                                           @Nullable BalancedBracketSelectors balancedBracketSelectors) {

        if (!loadSingleGrammar(initialScopeName)) {
            return null;
        }

        var dependencyProcessor = new ScopeDependencyProcessor(syncRegistry, initialScopeName);
        while (!dependencyProcessor.getQueue().isEmpty()) {
            dependencyProcessor.getQueue().forEach(
                ruleReference -> loadSingleGrammar(ruleReference.getScopeName())
            );
            dependencyProcessor.processQueue();
        }

        return grammarForScopeName(
            initialScopeName,
            initialLanguage,
            embeddedLanguages,
            tokenTypes,
            balancedBracketSelectors
        );
    }

    /**
     * Adds a new callback that will be invoked when a new grammar is added to the registry.
     */
    public void addGrammarCallback(Consumer<IGrammar> callback) { // not from upstream
        grammarCallbacks.add(callback);
    }

    /**
     * Removes an existing grammar callback.
     * See {@link #addGrammarCallback(Consumer)}.
     */
    public void removeGrammarCallback(Consumer<IGrammar> callback) { // not from upstream
        grammarCallbacks.remove(callback);
    }

    /**
     * Adds a new callback that will be invoked when the theme changes.
     */
    public void addThemeCallback(Consumer<ITheme> callback) { // not from upstream
        themeCallbacks.add(callback);
    }

    /**
     * Removes an existing theme callback.
     * See {@link #addThemeCallback(Consumer)}.
     */
    public void removeThemeCallback(Consumer<ITheme> callback) { // not from upstream
        themeCallbacks.remove(callback);
    }

    /**
     * Looks up an existing grammar by the specified scope name.
     * <p>
     * The grammar must first be registered via {@link #loadGrammar(String)} or {@link #addGrammar(IGrammarSource)}.
     */
    public @Nullable IGrammar grammarForScopeName(String scopeName) {
        return grammarForScopeName(scopeName, null, null, null, null);
    }

    //*************************************************************************

    private @Nullable IGrammar grammarForScopeName(String scopeName,
                                                   @Nullable Integer initialLanguage,
                                                   @Nullable Map<String, Integer> embeddedLanguages,
                                                   @Nullable Map<String, Integer> tokenTypes,
                                                   @Nullable BalancedBracketSelectors balancedBracketSelectors) {
        return syncRegistry.grammarForScopeName(
            scopeName,
            Objects.requireNonNullElse(initialLanguage, 0),
            embeddedLanguages,
            tokenTypes,
            balancedBracketSelectors
        );
    }

    private boolean loadSingleGrammar(String scopeName) {
        return ensureGrammarCache.computeIfAbsent(scopeName, this::doLoadSingleGrammar);
    }

    private boolean doLoadSingleGrammar(String scopeName) {
        var grammarSource = options.getGrammarSource(scopeName);
        if (grammarSource == null) {
            LOGGER.log(WARNING, "No grammar source for scope [{0}]", scopeName);
            return false;
        }

        try {
            var grammar = RawGrammar.from(grammarSource);
            syncRegistry.addGrammar(grammar, options.getInjections(scopeName));
        } catch (Exception e) {
            throw new TMException("Loading grammar for scope [" + scopeName + "] from ["
                + grammarSource.getURI() + "] failed: " + e.getMessage(), e);
        }
        return true;
    }
}
