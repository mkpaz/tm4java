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

import java.util.List;
import java.util.stream.Stream;
import tm4java.internal.grammar.matcher.MatcherWithPriority;

/**
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/grammar/grammar.ts#L898">
 * vscode-textmate/src/grammar/grammar.ts#L898</a>
 */
public final class BalancedBracketSelectors {

    private final Matcher<List<String>>[] balancedBracketScopes;
    private final Matcher<List<String>>[] unbalancedBracketScopes;
    private boolean allowAny = false;

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

    public boolean matchesAlways() {
        return allowAny && unbalancedBracketScopes.length == 0;
    }

    public boolean matchesNever() {
        return !allowAny && balancedBracketScopes.length == 0;
    }

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

    private Stream<? extends Matcher<List<String>>> createBalancedMatchers(String selector) {
        if ("*".equals(selector)) {
            allowAny = true;
            return Stream.empty();
        }
        return Matcher.createMatchers(selector).stream().map(MatcherWithPriority::matcher);
    }

    private Stream<? extends Matcher<List<String>>> createUnbalancedMatchers(String selector) {
        return Matcher.createMatchers(selector)
                   .stream()
                   .map(MatcherWithPriority::matcher);
    }
}
