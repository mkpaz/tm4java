/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.grammar.dependencies;

/**
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/grammar/grammarDependencies.ts#L10">
 * vscode-textmate/src/grammar/grammarDependencies.ts#L10</a>
 */
public abstract sealed class AbsoluteRuleReference permits
    AbsoluteRuleReference.TopLevelRepositoryRuleReference,
    AbsoluteRuleReference.TopLevelRuleReference {

    protected final String scopeName;

    private AbsoluteRuleReference(String scopeName) {
        this.scopeName = scopeName;
    }

    public String toKey() {
        return scopeName;
    }

    public String getScopeName() {
        return scopeName;
    }

    //*************************************************************************

    /**
     * References the top level rule of a grammar with the given scope name.
     */
    static final class TopLevelRuleReference extends AbsoluteRuleReference {
        TopLevelRuleReference(String scopeName) {
            super(scopeName);
        }
    }

    /**
     * References a rule of a grammar in the top level repository section with the given name.
     */
    static final class TopLevelRepositoryRuleReference extends AbsoluteRuleReference {

        private final String ruleName;

        TopLevelRepositoryRuleReference(String scopeName, String ruleName) {
            super(scopeName);
            this.ruleName = ruleName;
        }

        public String getRuleName() {
            return ruleName;
        }

        @Override
        public String toKey() {
            return getScopeName() + '#' + ruleName;
        }
    }
}
