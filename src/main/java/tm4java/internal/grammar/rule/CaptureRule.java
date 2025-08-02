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
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/rule.ts#L96">
 * vscode-textmate/src/rule.ts#L96</a>
 */
public final class CaptureRule extends Rule {

    public final RuleId retokenizeCapturedWithRuleId;

    CaptureRule(RuleId id,
                @Nullable String name,
                @Nullable String contentName,
                RuleId retokenizeCapturedWithRuleId) {
        super(id, name, contentName);
        this.retokenizeCapturedWithRuleId = retokenizeCapturedWithRuleId;
    }

    @Override
    public void collectPatterns(IRuleRegistry grammar, RegExpSourceList out) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompiledRule compile(IRuleRegistry grammar, @Nullable String endRegexSource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompiledRule compileAG(IRuleRegistry grammar,
                                  @Nullable String endRegexSource,
                                  boolean allowA,
                                  boolean allowG) {
        throw new UnsupportedOperationException();
    }
}
