/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.grammar.matcher;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Functional interface for testing whether a set of scope identifiers matches a given sequence
 * of active scope names.
 *
 * <p>Evaluates scope selector hierarchies (e.g., matching requested scope paths like
 * {@code ["source.java", "comment"]} against active scope stacks).
 *
 * @param <T> the type representing the stack or collection of active scope names (e.g. {@code List<String>})
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/grammar/grammar.ts#L71">
 * vscode-textmate/src/grammar/grammar.ts</a>
 */
public interface NameMatcher<T> {

    /**
     * Tests whether the target scope stack satisfies the provided list of scope identifiers in order.
     *
     * @param names  the ordered collection of scope identifier patterns to match
     * @param scopes the current active scope stack/context to evaluate against
     * @return {@code true} if all target identifiers match the scope context in sequence;{@code false} otherwise
     */
    boolean matches(Collection<String> names, T scopes);

    //*************************************************************************

    /**
     * The default implementation of {@link NameMatcher} for testing a list of active scope strings.
     *
     * <p>Implements TextMate hierarchical prefix matching (e.g., selector {@code "source"} matches
     * active scope {@code "source.java"}) while guaranteeing sequential left-to-right matching order.
     */
    NameMatcher<List<String>> DEFAULT = new NameMatcher<>() {

        /**
         * {@inheritDoc}
         *
         * @param identifiers the required scope pattern identifiers
         * @param scopes      the active stack of scope names
         * @return {@code true} if every identifier matches a scope in {@code scopes} sequentially
         */
        @Override
        public boolean matches(Collection<String> identifiers,
                               List<String> scopes) {
            if (scopes.size() < identifiers.size()) {
                return false;
            }
            int[] lastIndex = {0};
            return identifiers.stream().allMatch(identifier -> {
                for (int i = lastIndex[0]; i < scopes.size(); i++) {
                    if (scopesAreMatching(scopes.get(i), identifier)) {
                        lastIndex[0]++;
                        return true;
                    }
                }
                return false;
            });
        }

        /**
         * Determines if an active scope string matches a target scope selector pattern.
         *
         * <p>Performs exact matching or hierarchical prefix matching delimited by dot ({@code .}).
         * For example, scope {@code "entity.name.function"} matches patterns {@code "entity"},
         * {@code "entity.name"}, and {@code "entity.name.function"}.
         *
         * @param thisScopeName the candidate active scope string from the stack
         * @param scopeName     the selector pattern string being searched
         * @return {@code true} if the scope matches or starts with the pattern followed by a dot
         */
        private boolean scopesAreMatching(@Nullable String thisScopeName,
                                          String scopeName) {
            if (thisScopeName == null) {
                return false;
            }
            if (thisScopeName.equals(scopeName)) {
                return true;
            }

            int scopeLength = scopeName.length();
            return thisScopeName.length() > scopeLength
                && thisScopeName.substring(0, scopeLength).equals(scopeName)
                && thisScopeName.charAt(scopeLength) == '.';
        }
    };
}
