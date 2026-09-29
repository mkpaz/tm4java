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
 * Represents a simple TextMate single-line matching rule defined by a single regex.
 *
 * <p>Evaluates a regex pattern against text and applies scope tags or capture rules upon matching.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/rule.ts#L122">
 * vscode-textmate/src/rule.ts#L122</a>
 */
public final class MatchRule extends Rule {

    /** The regex source object for this matching pattern. */
    private final RegExpSource match;

    /** List of capture rules applied to matching groups in the regex. */
    public final List<@Nullable CaptureRule> captures;

    /** Cached collection of compiled regex sources. */
    private @Nullable RegExpSourceList cachedCompiledPatterns;

    /**
     * Constructs a new {@link MatchRule} instance.
     *
     * @param id       the unique internal rule identifier
     * @param name     the scope name assigned to matches of this rule, or {@code null}
     * @param match    the regex pattern string
     * @param captures the capture rules associated with groups in the match pattern
     */
    MatchRule(RuleId id, @Nullable String name, String match, List<@Nullable CaptureRule> captures) {
        super(id, name, null);
        this.match = new RegExpSource(match, id);
        this.captures = captures;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Adds the single matching regex pattern of this rule to the destination pattern list.
     *
     * @param grammar the rule registry for resolving references
     * @param out     the target list receiving the matching pattern source
     */
    @Override
    public void collectPatterns(IRuleRegistry grammar, RegExpSourceList out) {
        out.add(match);
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
     * Returns the list of regex patterns for this rule.
     *
     * @param grammar the rule registry used during pattern collection
     * @return the cached {@link RegExpSourceList} containing the compiled pattern
     */
    private RegExpSourceList getCachedCompiledPatterns(IRuleRegistry grammar) {
        if (cachedCompiledPatterns == null) {
            cachedCompiledPatterns = new RegExpSourceList();
            collectPatterns(grammar, cachedCompiledPatterns);
        }

        return cachedCompiledPatterns;
    }
}
