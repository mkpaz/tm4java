/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.grammar;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Represents optional configuration options applied when instantiating or loading a TextMate grammar.
 *
 * <p>Allows customizing embedded language mapping, overriding standard token types, and configuring
 * bracket-matching scope selectors.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/blob/v9.2.0/src/main.ts#L44">
 * vscode-textmate/src/main.ts#L44</a>
 */
public interface IGrammarConfiguration {

    /**
     * Returns a map overriding the target language ID for embedded scopes.
     *
     * <p>Maps a scope selector (e.g. {@code "source.js"}) to a numerical language ID, which is then encoded
     * into {@link EncodedTokenAttributes} during binary tokenization.
     *
     * @return a map where keys are scope selectors and values are language IDs,
     *         or {@code null} if no custom mappings are defined
     * @see <a href="https://github.com/microsoft/vscode-textmate/issues/216">vscode-textmate Issue #216</a>
     */
    default @Nullable Map<String, Integer> getEmbeddedLanguages() {
        return null;
    }

    /**
     * Returns a map of scope selectors used to override the default token types
     * (e.g. Comment, String, RegEx).
     *
     * <p>Overridden token types are encoded as part of {@link EncodedTokenAttributes}.
     *
     * @return a map where keys are scope selectors and values are values from
     * {@code OptionalStandardTokenType}, or {@code null} if no token type overrides are defined
     */
    default @Nullable Map<String, Integer> getTokenTypes() {
        return null;
    }

    /**
     * Returns a list of scope selectors in which bracket pairing/balancing is explicitly enabled.
     *
     * <p>Passing {@code "*"} enables bracket balancing across all scopes by default.
     * This metadata is exposed via {@link EncodedTokenAttributes#containsBalancedBrackets(int)}.
     *
     * @return a list of scope selector strings where bracket balancing is active,
     *         or {@code null} if not configured
     * @see #getUnbalancedBracketSelectors()
     */
    default @Nullable List<String> getBalancedBracketSelectors() {
        return null;
    }

    /**
     * Returns a list of scope selectors in which bracket pairing/balancing must be disabled.
     *
     * <p>Unbalanced bracket selectors take precedence over {@link #getBalancedBracketSelectors()}.
     * This allows enabling balanced brackets globally while excluding specific sub-scopes (such as strings
     * or comments).
     *
     * @return a list of scope selector strings where bracket balancing is disabled,
     *         or {@code null} if not configured
     * @see #getBalancedBracketSelectors()
     */
    default @Nullable List<String> getUnbalancedBracketSelectors() {
        return null;
    }
}
