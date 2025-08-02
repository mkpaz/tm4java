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

import java.util.List;


/**
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/rule.ts#L122">
 * vscode-textmate/src/rule.ts#L122</a>
 */
public final class MatchRule extends Rule {

    private final RegExpSource match;

    public final List<@Nullable CaptureRule> captures;

    private @Nullable RegExpSourceList cachedCompiledPatterns;

    MatchRule(RuleId id, @Nullable String name, String match, List<@Nullable CaptureRule> captures) {
        super(id, name, null);
        this.match = new RegExpSource(match, id);
        this.captures = captures;
    }

    @Override
    public void collectPatterns(IRuleRegistry grammar, RegExpSourceList out) {
        out.add(match);
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
