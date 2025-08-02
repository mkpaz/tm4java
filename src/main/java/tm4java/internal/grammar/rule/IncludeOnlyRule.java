/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.grammar.rule;

import org.jspecify.annotations.Nullable;

/**
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/rule.ts#L166">
 * vscode-textmate/src/rule.ts#L166</a>
 */
final class IncludeOnlyRule extends Rule {

    final boolean hasMissingPatterns;
    final RuleId[] patterns;

    private @Nullable RegExpSourceList cachedCompiledPatterns;

    IncludeOnlyRule(RuleId id,
                    @Nullable String name,
                    @Nullable String contentName,
                    CompilePatternsResult patterns) {
        super(id, name, contentName);
        this.patterns = patterns.patterns();
        this.hasMissingPatterns = patterns.hasMissingPatterns();
    }

    @Override
    public void collectPatterns(IRuleRegistry grammar, RegExpSourceList out) {
        for (RuleId pattern : patterns) {
            Rule rule = grammar.getRule(pattern);
            rule.collectPatterns(grammar, out);
        }
    }

    @Override
    public CompiledRule compile(IRuleRegistry grammar, @Nullable String endRegexSource) {
        return getCachedCompiledPatterns(grammar).compile();
    }

    @Override
    public CompiledRule compileAG(IRuleRegistry grammar,
                                  @Nullable String endRegexSource,
                                  boolean allowA,
                                  boolean allowG) {
        return getCachedCompiledPatterns(grammar).compileAG(allowA, allowG);
    }

    //*************************************************************************

    private RegExpSourceList getCachedCompiledPatterns(IRuleRegistry grammar) {
        if (cachedCompiledPatterns == null) {
            cachedCompiledPatterns = new RegExpSourceList();
            collectPatterns(grammar, cachedCompiledPatterns);
        }
        return cachedCompiledPatterns;
    }
}
