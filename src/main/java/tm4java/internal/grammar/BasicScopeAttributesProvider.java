/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.grammar;

import org.jspecify.annotations.Nullable;
import tm4java.TMException;
import tm4java.internal.grammar.tokenattrs.OptionalStandardTokenType;
import tm4java.internal.utils.Patterns;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

/**
 * Provides basic scope attributes (such as language ID and standard token type) for TextMate
 * scope names.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/grammar/basicScopesAttributeProvider.ts#L18">
 * vscode-textmate/src/basicScopesAttributeProvider.ts#L18</a>
 */
final class BasicScopeAttributesProvider {

    private static final Pattern STANDARD_TOKEN_TYPE_REGEXP =
        Pattern.compile("\\b(comment|string|regex|meta\\.embedded)\\b");

    private static final BasicScopeAttributes NULL_SCOPE_METADATA =
        new BasicScopeAttributes(0, 0);

    private final BasicScopeAttributes defaultAttributes;
    private final ScopeMatcher<Integer/*languageId*/> embeddedLanguagesMatcher;
    private final ConcurrentMap<String/*scopeName*/, BasicScopeAttributes> cache = new ConcurrentHashMap<>();

    /**
     * Constructs a new {@link BasicScopeAttributesProvider}.
     *
     * @param initialLanguage   the default language ID
     * @param embeddedLanguages mapping of scope names to embedded language IDs, or {@code null}
     */
    BasicScopeAttributesProvider(int initialLanguage,
                                 @Nullable Map<String, Integer> embeddedLanguages) {
        this.defaultAttributes = new BasicScopeAttributes(
            initialLanguage,
            OptionalStandardTokenType.NOT_SET
        );
        this.embeddedLanguagesMatcher = new ScopeMatcher<>(
            Objects.requireNonNullElse(embeddedLanguages, Collections.emptyMap())
        );
    }

    /**
     * Returns the default attributes used when no specific scope matches apply.
     *
     * @return the default basic scope attributes
     */
    BasicScopeAttributes getDefaultAttributes() {
        return defaultAttributes;
    }

    /**
     * Resolves the basic scope attributes for the specified scope name.
     *
     * @param scopeName the scope name to resolve, or {@code null}
     * @return the resolved {@link BasicScopeAttributes}
     */
    BasicScopeAttributes getBasicScopeAttributes(@Nullable String scopeName) {
        if (scopeName == null) {
            return BasicScopeAttributesProvider.NULL_SCOPE_METADATA;
        }

        return cache.computeIfAbsent(scopeName, scopeName_ -> {
            var languageId = scopeToLanguage(scopeName);
            var standardTokenType = toStandardTokenType(scopeName);
            return new BasicScopeAttributes(languageId, standardTokenType);
        });
    }

    /**
     * Given a produced TM scope, return the language that token describes or null if unknown.
     * e.g. source.html => html, source.css.embedded.html => css, punctuation.definition.tag.html => null
     */
    private int scopeToLanguage(String scopeName) {
        return Objects.requireNonNullElse(embeddedLanguagesMatcher.match(scopeName), 0);
    }

    /**
     * Determines the standard token type classification for a given scope name.
     *
     * @param scopeName the scope name to analyze
     * @return the standard token type constant
     */
    private int toStandardTokenType(String scopeName) {
        var matcher = STANDARD_TOKEN_TYPE_REGEXP.matcher(scopeName);
        if (!matcher.find()) {
            return OptionalStandardTokenType.NOT_SET;
        }

        String group = matcher.group(1);
        return switch (group) {
            case "comment" -> OptionalStandardTokenType.COMMENT;
            case "string" -> OptionalStandardTokenType.STRING;
            case "regex" -> OptionalStandardTokenType.REGEX;
            case "meta.embedded" -> OptionalStandardTokenType.OTHER;
            default -> throw new TMException("Unexpected match for standard token type: " + group);
        };
    }

    //*************************************************************************

    /**
     * Internal helper that uses regex matching to map scope name prefixes to target values.
     *
     * @param <T> the type of value associated with scope prefixes
     */
    private static final class ScopeMatcher<T> {

        private final Map<String, T> values;
        private final @Nullable Pattern scopesRegExp;

        /**
         * Constructs a new {@link ScopeMatcher}.
         *
         * @param values map of target scope strings to associated values
         */
        ScopeMatcher(Map<String, T> values) {
            if (values.isEmpty()) {
                this.values = Collections.emptyMap();
                this.scopesRegExp = null;
            } else {
                this.values = new HashMap<>(values);

                // create the regex
                var escapedScopes = values.keySet().stream()
                    .map(Patterns::escapeRegexCharacters)
                    .sorted(Collections.reverseOrder()) // longest scope first
                    .toArray(String[]::new);

                scopesRegExp = Pattern.compile("^((" + String.join(")|(", escapedScopes) + "))($|\\.)");
            }
        }

        /**
         * Matches a scope name against configured scope prefix patterns.
         *
         * @param scopeName the scope name to match
         * @return the matching value, or {@code null} if no match is found
         */
        @Nullable
        T match(String scopeName) {
            if (scopesRegExp == null) {
                return null;
            }

            var matcher = scopesRegExp.matcher(scopeName);
            if (!matcher.find()) {
                // no scopes matched
                return null;
            }

            return values.get(matcher.group(1));
        }
    }
}
