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
import tm4java.internal.grammar.tokenattrs.OptionalStandardTokenType;

import java.util.List;
import java.util.Map;

/**
 * Represents a configuration for TextMate grammar.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/main.ts#L44">vscode-textmate/src/main.ts#L44</a>
 */
public interface IGrammarConfiguration {

    /**
     * Returns a map of embedded languages.
     * <p>
     * This maps an embedded language scope to a language ID, which is then returned
     * as part of {@link EncodedTokenAttributes}.
     * <p>
     * See <a href="https://github.com/microsoft/vscode-textmate/issues/216">vscode-textmate/issues/216</a>.
     * <p>
     * The key is the scope, and the value is the integer language ID.
     */
    default @Nullable Map<String, Integer> getEmbeddedLanguages() {
        return null;
    }

    /**
     * Returns a map of token types to override {@link OptionalStandardTokenType}
     * for a specific scope.
     * <p>
     * This information is also returned as part of {@link EncodedTokenAttributes}.
     * <p>
     * The key is a scope, and the value is the corresponding integer token type.
     */
    default @Nullable Map<String, Integer> getTokenTypes() {
        return null;
    }

    /**
     * Returns a list of scopes (selectors) that use balanced bracket pairs.
     * <p>
     * This information is not used for tokenization but is returned as part of
     * {@link EncodedTokenAttributes} for client use.
     * <p>
     * See also {@link #getUnbalancedBracketSelectors()}.
     */
    default @Nullable List<String> getBalancedBracketSelectors() {
        return null;
    }

    /**
     * Returns a list of scopes (languages) that use unbalanced bracket pairs.
     * <p>
     * This information is not used for tokenization but is returned as part of
     * {@link EncodedTokenAttributes} for client use.
     * <p>
     * This list takes precedence over {@link #getBalancedBracketSelectors()}.
     * A broader scope can use balanced brackets while a more specific scope can
     * use unbalanced ones.
     * <p>
     * Also see {@link #getBalancedBracketSelectors()}.
     */
    default @Nullable List<String> getUnbalancedBracketSelectors() {
        return null;
    }
}
