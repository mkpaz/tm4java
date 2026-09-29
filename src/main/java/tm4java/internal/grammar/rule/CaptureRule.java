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
 * Represents a rule applied to a specific capture group within a TextMate pattern.
 *
 * <p>Capture rules are nested scope assignments or re-tokenization triggers for matching subgroups.
 * They do not directly participate in pattern collection or compilation.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/rule.ts#L96">
 * vscode-textmate/src/rule.ts#L96</a>
 */
public final class CaptureRule extends Rule {

    /** The ID of the rule used to re-tokenize text captured by this group, or {@link RuleId#NO_RULE}. */
    public final RuleId retokenizeCapturedWithRuleId;

    /**
     * Constructs a new {@link CaptureRule} instance.
     *
     * @param id                           the unique internal rule identifier
     * @param name                         the scope name assigned to this capture group, or {@code null}
     * @param contentName                  the scope name assigned to inner content, or {@code null}
     * @param retokenizeCapturedWithRuleId the rule ID for re-tokenizing captured content
     */
    CaptureRule(RuleId id,
                @Nullable String name,
                @Nullable String contentName,
                RuleId retokenizeCapturedWithRuleId) {
        super(id, name, contentName);
        this.retokenizeCapturedWithRuleId = retokenizeCapturedWithRuleId;
    }

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException capture rules cannot collect standalone regex patterns
     */
    @Override
    public void collectPatterns(IRuleRegistry grammar, RegExpSourceList out) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException capture rules cannot be compiled directly
     */
    @Override
    public CompiledRule compile(IRuleRegistry grammar, @Nullable String endRegexSource) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException capture rules cannot be compiled directly
     */
    @Override
    public CompiledRule compileAG(IRuleRegistry grammar,
                                  @Nullable String endRegexSource,
                                  boolean allowA,
                                  boolean allowG) {
        throw new UnsupportedOperationException();
    }
}
