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
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/grammar/grammar.ts#L71">
 * vscode-textmate/src/grammar/grammar.ts</a>
 */
public interface NameMatcher<T> {

    boolean matches(Collection<String> names, T scopes);

    //*************************************************************************

    NameMatcher<List<String>> DEFAULT = new NameMatcher<>() {

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
