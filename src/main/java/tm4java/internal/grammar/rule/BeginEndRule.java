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
import tm4java.internal.grammar.oniguruma.OnigCaptureIndex;

import java.util.List;
import java.util.Objects;

/**
 * Represents a TextMate block matching rule defined by a starting pattern ({@code begin})
 * and an ending pattern ({@code end}).
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/rule.ts#L209">
 * vscode-textmate//src/rule.ts#L209</a>
 */
public final class BeginEndRule extends Rule {

    private final RegExpSource begin;
    private final RegExpSource end;

    /** Controls whether the end pattern evaluation is deferred until after inner child patterns. */
    public final boolean applyEndPatternLast;

    /** List of capture rules applied to matching groups in the {@code begin} pattern. */
    public final List<@Nullable CaptureRule> beginCaptures;

    /** List of capture rules applied to matching groups in the {@code end} pattern. */
    public final List<@Nullable CaptureRule> endCaptures;

    /** Flag indicating whether the {@code end} pattern contains back-references to {@code begin} capture groups. */
    public final boolean endHasBackReferences;

    /** Flag indicating whether any nested sub-patterns failed to resolve during initialization. */
    public final boolean hasMissingPatterns;

    /** Array of rule IDs for nested sub-patterns embedded within this block. */
    public final RuleId[] patterns;

    private @Nullable RegExpSourceList cachedCompiledPatterns;

    /**
     * Constructs a new {@link BeginEndRule} instance.
     *
     * @param id                  the unique internal rule identifier
     * @param name                the scope name assigned to matches of this rule, or {@code null}
     * @param contentName         the scope name assigned exclusively to inner enclosed content, or {@code null}
     * @param begin               the regex string starting the block
     * @param beginCaptures       the capture rules for the {@code begin} pattern
     * @param end                 the regex string ending the block, or {@code null}
     * @param endCaptures         the capture rules for the {@code end} pattern
     * @param applyEndPatternLast whether to test the end pattern after inner child patterns
     * @param patterns            the result containing child rule IDs and missing pattern flags
     */
    BeginEndRule(RuleId id,
                 @Nullable String name,
                 @Nullable String contentName,
                 String begin,
                 List<@Nullable CaptureRule> beginCaptures,
                 @Nullable String end,
                 List<@Nullable CaptureRule> endCaptures,
                 boolean applyEndPatternLast,
                 CompilePatternsResult patterns) {
        super(id, name, contentName);

        this.begin = new RegExpSource(begin, id);
        this.beginCaptures = beginCaptures;
        this.end = new RegExpSource(Objects.requireNonNullElse(end, "\uFFFF"), RuleId.END_RULE);
        this.endHasBackReferences = this.end.hasBackReferences;
        this.endCaptures = endCaptures;
        this.applyEndPatternLast = applyEndPatternLast;
        this.patterns = patterns.patterns();
        this.hasMissingPatterns = patterns.hasMissingPatterns();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Adds the starting pattern ({@code begin}) of this rule to the destination pattern list.
     *
     * @param grammar the rule registry for resolving references
     * @param out     the target list receiving the starting pattern source
     */
    @Override
    public void collectPatterns(IRuleRegistry grammar, RegExpSourceList out) {
        out.add(begin);
    }

    /**
     * {@inheritDoc}
     *
     * @param grammar        the rule registry for resolving sub-rules
     * @param endRegexSource resolved regex string for dynamic end pattern, or {@code null}
     * @return the compiled regex matching rule
     */
    @Override
    public CompiledRule compile(IRuleRegistry grammar, @Nullable String endRegexSource) {
        return getCachedCompiledPatterns(grammar, endRegexSource).compile();
    }

    /**
     * {@inheritDoc}
     *
     * @param grammar        the rule registry for resolving sub-rules
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
        return getCachedCompiledPatterns(grammar, endRegexSource).compileAG(allowA, allowG);
    }

    /**
     * Resolves back-references in the {@code end} pattern using capture results from the {@code begin} match.
     *
     * @param lineText       the line text against which the match occurred
     * @param captureIndices array of capture indices from the initial {@code begin} match
     * @return the fully resolved {@code end} regex string
     */
    public String getEndWithResolvedBackReferences(CharSequence lineText, OnigCaptureIndex[] captureIndices) {
        return end.resolveBackReferences(lineText, captureIndices);
    }

    //*************************************************************************

    /**
     * Updates the combined list of regex patterns (nested rules and {@code end} pattern)
     * for block evaluation.
     *
     * @param grammar        the rule registry for resolving child patterns
     * @param endRegexSource resolved back-reference pattern override, or {@code null}
     * @return the cached or updated {@link RegExpSourceList}
     */
    private RegExpSourceList getCachedCompiledPatterns(IRuleRegistry grammar, @Nullable String endRegexSource) {
        if (cachedCompiledPatterns == null) {
            cachedCompiledPatterns = new RegExpSourceList();

            for (var pattern : patterns) {
                var rule = grammar.getRule(pattern);
                rule.collectPatterns(grammar, cachedCompiledPatterns);
            }

            if (applyEndPatternLast) {
                cachedCompiledPatterns.add(endHasBackReferences ? end.clone() : end);
            } else {
                cachedCompiledPatterns.remove(endHasBackReferences ? end.clone() : end);
            }
        }

        if (endHasBackReferences && endRegexSource != null) {
            if (applyEndPatternLast) {
                cachedCompiledPatterns.setSource(cachedCompiledPatterns.length() - 1, endRegexSource);
            } else {
                cachedCompiledPatterns.setSource(0, endRegexSource);
            }
        }

        return cachedCompiledPatterns;
    }
}