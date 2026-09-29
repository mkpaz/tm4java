/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.grammar;

import tm4java.internal.grammar.matcher.Matcher;
import tm4java.internal.grammar.matcher.MatcherWithPriority;

import java.util.List;
import java.util.stream.Stream;

/**
 * Evaluates whether bracket matching should be enabled or disabled for a given set of TextMate scopes.
 *
 * <p>This selector holds matching logic for balanced (inclusive) and unbalanced (exclusive) bracket scope
 * selectors, allowing VSCode's TextMate engine to determine if auto-closing or pair matching applies in
 * the current context (e.g., enabling brackets in code, but disabling them inside comments or strings).
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/grammar/grammar.ts#L898">
 * vscode-textmate/src/grammar/grammar.ts#L898</a>
 */
public final class BalancedBracketSelectors {

    private final Matcher<List<String>>[] balancedBracketScopes;
    private final Matcher<List<String>>[] unbalancedBracketScopes;
    private boolean allowAny = false;

    /**
     * Constructs a new {@code BalancedBracketSelectors} instance by parsing the provided
     * scope selector strings into matchers.
     *
     * @param balancedBracketScopes   a list of scope selectors where brackets should be balanced;
     *                                passing {@code "*"} enables matching for all scopes by default
     * @param unbalancedBracketScopes a list of scope selectors where bracket balancing must be
     *                                explicitly disabled
     */
    @SuppressWarnings("unchecked")
    public BalancedBracketSelectors(List<String> balancedBracketScopes,
                                    List<String> unbalancedBracketScopes) {
        this.balancedBracketScopes = balancedBracketScopes.stream()
            .flatMap(this::createBalancedMatchers)
            .toArray(Matcher[]::new);

        this.unbalancedBracketScopes = unbalancedBracketScopes.stream()
            .flatMap(this::createUnbalancedMatchers)
            .toArray(Matcher[]::new);
    }

    /**
     * Checks if this selector matches unconditionally for any scope stack.
     *
     * @return {@code true} if wildcard matching ({@code "*"}) is enabled and no exclusion
     *      scope rules are defined; {@code false} otherwise
     */
    public boolean matchesAlways() {
        return allowAny && unbalancedBracketScopes.length == 0;
    }

    /**
     * Checks if this selector will never match any scope stack.
     *
     * @return {@code true} if wildcard matching is disabled and there are no balanced bracket
     *         matchers defined; {@code false} otherwise
     */
    public boolean matchesNever() {
        return !allowAny && balancedBracketScopes.length == 0;
    }

    /**
     * Determines whether the specified scope stack satisfies the balanced bracket criteria.
     *
     * <p>The evaluation order is as follows:
     * <ol>
     * <li>If any selector in {@code unbalancedBracketScopes} matches, returns {@code false}.</li>
     * <li>If any selector in {@code balancedBracketScopes} matches, returns {@code true}.</li>
     * <li>Otherwise, returns the fallback value ({@code true} if a wildcard {@code "*"}
     * selector was present, {@code false} otherwise).</li>
     * </ol>
     *
     * @param scopes the current stack of TextMate scopes to evaluate
     * @return {@code true} if bracket matching is allowed for the given scope stack; {@code false} otherwise
     */
    public boolean match(List<String> scopes) {
        for (var exclude : unbalancedBracketScopes) {
            if (exclude.matches(scopes)) {
                return false;
            }
        }

        for (var include : balancedBracketScopes) {
            if (include.matches(scopes)) {
                return true;
            }
        }

        return allowAny;
    }

    //*************************************************************************

    /**
     * Creates matchers for balanced scope selectors. If the wildcard selector {@code "*"}
     * is encountered, sets the internal flag to allow any scope by default.
     *
     * @param selector the scope selector string to parse
     * @return a stream of parsed matchers for balanced scope filtering
     */
    private Stream<? extends Matcher<List<String>>> createBalancedMatchers(String selector) {
        if ("*".equals(selector)) {
            allowAny = true;
            return Stream.empty();
        }
        return Matcher.createMatchers(selector).stream().map(MatcherWithPriority::matcher);
    }

    /**
     * Creates matchers for unbalanced scope selectors (exclusion rules).
     *
     * @param selector the scope selector string to parse
     * @return a stream of parsed matchers for unbalanced scope filtering
     */
    private Stream<? extends Matcher<List<String>>> createUnbalancedMatchers(String selector) {
        return Matcher.createMatchers(selector)
            .stream()
            .map(MatcherWithPriority::matcher);
    }
}
