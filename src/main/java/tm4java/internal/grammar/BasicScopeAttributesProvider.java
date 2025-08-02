/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.grammar;

import java.util.Objects;
import org.jspecify.annotations.Nullable;
import tm4java.TMException;
import tm4java.internal.grammar.tokenattrs.OptionalStandardTokenType;
import tm4java.internal.utils.RegexUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

/**
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/grammar/basicScopesAttributeProvider.ts#L18">
 * vscode-textmate/src/basicScopesAttributeProvider.ts#L18</a>
 */
final class BasicScopeAttributesProvider {

    private static final Pattern STANDARD_TOKEN_TYPE_REGEXP =
        Pattern.compile("\\b(comment|string|regex|meta\\.embedded)\\b");

    private static final BasicScopeAttributes NULL_SCOPE_METADATA =
        new BasicScopeAttributes(0, 0);

    private final BasicScopeAttributes defaultAttributes;
    private final ScopeMatcher<Integer /*languageId*/> embeddedLanguagesMatcher;
    private final ConcurrentMap<String /*scopeName*/, BasicScopeAttributes> cache = new ConcurrentHashMap<>();

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

    BasicScopeAttributes getDefaultAttributes() {
        return defaultAttributes;
    }

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

    private static final class ScopeMatcher<T> {

        private final Map<String, T> values;
        private final @Nullable Pattern scopesRegExp;

        ScopeMatcher(Map<String, T> values) {
            if (values.isEmpty()) {
                this.values = Collections.emptyMap();
                this.scopesRegExp = null;
            } else {
                this.values = new HashMap<>(values);

                // create the regex
                var escapedScopes = values.keySet().stream()
                    .map(RegexUtils::escapeRegExpCharacters)
                    .sorted(Collections.reverseOrder()) // longest scope first
                    .toArray(String[]::new);

                scopesRegExp = Pattern.compile("^((" + String.join(")|(", escapedScopes) + "))($|\\.)");
            }
        }

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
