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
 * @see <a href="https://macromates.com/textmate/manual/references#scope-selector">TextMate Scope Selector syntax</a>
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/matcher.ts#L10">vscode-textmate/src/matcher.ts</a>
 */
@FunctionalInterface
public interface Matcher<T> {

    boolean matches(T t);

    //*************************************************************************

    static List<MatcherWithPriority<List<String>>> createMatchers(String selector) {
        return createMatchers(selector, NameMatcher.DEFAULT);
    }

    static List<MatcherWithPriority<List<String>>> createMatchers(String selector,
                                                                  NameMatcher<List<String>> matchesName) {
        return new MatcherBuilder<>(selector, matchesName).results();
    }
}
