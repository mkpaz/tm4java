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
import tm4java.grammar.IGrammarSource;
import tm4java.internal.theme.Theme;
import tm4java.theme.IRawTheme;

import java.util.Collection;
import java.util.List;

/**
 * Represents a set of options passed to the default {@link Registry} to configure
 * theme initialization, grammar retrieval, and scope injections.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/main.ts#L22">
 * vscode-textmate/src/main.ts#L22</a>
 */
public interface IRegistryOptions {

    /**
     * Returns the raw theme used to create the default registry theme.
     *
     * <p>See {@link Theme#createFromRawTheme(IRawTheme, List)}
     *
     * @return the initial {@link IRawTheme}, or {@code null} if no theme is configured
     */
    default @Nullable IRawTheme getTheme() {
        return null;
    }

    /**
     * Returns the color map used to create the default registry theme.
     *
     * <p>See {@link Theme#createFromRawTheme(IRawTheme, List)}
     *
     * @return an initial list of HEX color strings for the color map palette, or {@code null}
     */
    default @Nullable List<String> getColorMap() {
        return null;
    }

    /**
     * Creates the grammar source used to load grammar by a specified scope name.
     *
     * <pre>{@code
     * public IGrammarSource getGrammarSource(String scopeName) {
     *      return switch (scopeName) {
     *          case "source.css" -> IGrammarSource.fromResource("css.json");
     *          case "source.js" -> IGrammarSource.fromResource("JavaScript.tmLanguage.json");
     *          default -> null;
     *      };
     * }
     * }</pre>
     *
     * @param scopeName the TextMate target scope name (e.g. {@code "source.java"})
     * @return the {@link IGrammarSource} for the requested scope, or {@code null} if not found
     */
    default @Nullable IGrammarSource getGrammarSource(String scopeName) {
        return null;
    }

    /**
     * Returns the list of injected grammars for the specified scope name.
     *
     * <p>For example, Angular template-ng files can contain CSS and TypeScript
     * injections covered by other grammars.
     *
     * @param scopeName the TextMate scope name to query for injections
     * @return a collection of grammar scope names to inject, or {@code null} if none
     */
    default @Nullable Collection<String> getInjections(String scopeName) {
        return null;
    }

    //*************************************************************************

    /**
     * Creates default registry options with empty/null implementations.
     *
     * @return a default instance of {@link IRegistryOptions}
     */
    static IRegistryOptions createDefault() {
        return new IRegistryOptions() { };
    }
}
