/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.grammar.matcher;

import java.util.List;

/**
 * Functional interface for matching objects against criteria or TextMate scope selectors.
 *
 * @param <T> the type of candidate object being matched (typically a list or stack of scope names)
 * @see <a href="https://macromates.com/textmate/manual/references#scope-selector">
 * TextMate Scope Selector syntax</a>
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/matcher.ts#L10">
 * vscode-textmate/src/matcher.ts</a>
 */
@FunctionalInterface
public interface Matcher<T> {

    /**
     * Evaluates whether the given object satisfies the matcher's predicate logic.
     *
     * @param t the object to test
     * @return {@code true} if the object matches; {@code false} otherwise
     */
    boolean matches(T t);

    //*************************************************************************

    /**
     * Creates a list of prioritized matchers from a TextMate scope selector string using
     * default scope matching rules.
     *
     * @param selector the raw TextMate scope selector expression string (e.g. {@code "source.java comment.line"})
     * @return a list of {@link MatcherWithPriority} instances representing the parsed expression
     */
    static List<MatcherWithPriority<List<String>>> createMatchers(String selector) {
        return createMatchers(selector, NameMatcher.DEFAULT);
    }

    /**
     * Creates a list of prioritized matchers from a TextMate scope selector string using
     * a custom scope name matcher.
     *
     * @param selector    the raw TextMate scope selector expression string
     * @param matchesName the custom {@link NameMatcher} logic to match scope names against scope lists
     * @return a list of {@link MatcherWithPriority} instances representing the parsed expression
     */
    static List<MatcherWithPriority<List<String>>> createMatchers(String selector,
                                                                  NameMatcher<List<String>> matchesName) {
        return new MatcherBuilder<>(selector, matchesName).results();
    }
}