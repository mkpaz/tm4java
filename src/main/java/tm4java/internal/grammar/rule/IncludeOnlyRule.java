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
 * Represents a transparent container rule that groups and includes child patterns.
 *
 * <p>This rule delegates pattern collection directly to its nested rules without contributing
 * an independent regex matching layer.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/rule.ts#L166">
 * vscode-textmate/src/rule.ts#L166</a>
 */
final class IncludeOnlyRule extends Rule {

    /** Indicates whether any nested sub-patterns failed to resolve during initialization. */
    final boolean hasMissingPatterns;

    /** Array of rule IDs for the nested patterns contained in this inclusion rule. */
    final RuleId[] patterns;

    /** Cached collection of compiled regex sources. */
    private @Nullable RegExpSourceList cachedCompiledPatterns;

    /**
     * Constructs a new {@link IncludeOnlyRule} instance.
     *
     * @param id          the unique internal rule identifier
     * @param name        the scope name for matches, or {@code null}
     * @param contentName the scope name for inner content, or {@code null}
     * @param patterns    the result container containing nested rule IDs and missing pattern flags
     */
    IncludeOnlyRule(RuleId id,
                    @Nullable String name,
                    @Nullable String contentName,
                    CompilePatternsResult patterns) {
        super(id, name, contentName);
        this.patterns = patterns.patterns();
        this.hasMissingPatterns = patterns.hasMissingPatterns();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Iterates over all contained rule IDs and delegates pattern collection to each child rule.
     */
    @Override
    public void collectPatterns(IRuleRegistry grammar, RegExpSourceList out) {
        for (RuleId pattern : patterns) {
            Rule rule = grammar.getRule(pattern);
            rule.collectPatterns(grammar, out);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param grammar        the rule registry for resolving child rules
     * @param endRegexSource resolved regex string for dynamic end pattern, or {@code null}
     * @return the compiled regex matching rule
     */
    @Override
    public CompiledRule compile(IRuleRegistry grammar, @Nullable String endRegexSource) {
        return getCachedCompiledPatterns(grammar).compile();
    }

    /**
     * {@inheritDoc}
     *
     * @param grammar        the rule registry for resolving child rules
     * @param endRegexSource resolved regex string for dynamic end pattern, or {@code null}
     * @param allowA         whether to permit anchor {@code \A} matching
     * @param allowG         whether to permit anchor {@code \G} matching
     * @return the compiled regex matching rule with anchor options
     */
    @Override
    public CompiledRule compileAG(IRuleRegistry grammar,
                                  @Nullable String endRegexSource,
                                  boolean allowA,
                                  boolean allowG) {
        return getCachedCompiledPatterns(grammar).compileAG(allowA, allowG);
    }

    //*************************************************************************

    /**
     * Returns the list of regex patterns collected from child rules.
     *
     * @param grammar the rule registry used to resolve child rule instances
     * @return the cached {@link RegExpSourceList} containing all collected child patterns
     */
    private RegExpSourceList getCachedCompiledPatterns(IRuleRegistry grammar) {
        if (cachedCompiledPatterns == null) {
            cachedCompiledPatterns = new RegExpSourceList();
            collectPatterns(grammar, cachedCompiledPatterns);
        }
        return cachedCompiledPatterns;
    }
}