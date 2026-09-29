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
 * Represents a TextMate rule with {@code begin} and {@code while} patterns.
 *
 * <p>This rule matches an opening pattern to push a scope onto the stack and continues matching
 * while a condition (the {@code while} pattern) holds true on subsequent lines.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/rule.ts#L290">
 * vscode-textmate/src/rule.ts#L290</a>
 */
public final class BeginWhileRule extends Rule {

    /** The regex pattern source for the opening {@code begin} match. */
    private final RegExpSource beginRegExp;

    /** The regex pattern source for the continuing {@code while} match. */
    private final RegExpSource whileRegExp;

    /** The list of capture rules associated with groups in the {@code begin} pattern. */
    public final List<@Nullable CaptureRule> beginCaptures;

    /** The list of capture rules associated with groups in the {@code while} pattern. */
    public final List<@Nullable CaptureRule> whileCaptures;

    /** Indicates whether the {@code while} pattern contains back-references to {@code begin} capture groups. */
    public final boolean whileHasBackReferences;

    /** Indicates whether any child patterns failed to resolve during compilation setup. */
    final boolean hasMissingPatterns;

    /** The array of child rule IDs contained within this rule block. */
    final RuleId[] patterns;

    /** Cached compilation pattern list for matching child rules. */
    private @Nullable RegExpSourceList cachedCompiledPatterns;

    /** Cached compilation pattern list for matching the {@code while} condition. */
    private @Nullable RegExpSourceList cachedCompiledWhilePatterns;

    /**
     * Constructs a new {@link BeginWhileRule} instance.
     *
     * @param id            the unique internal rule identifier
     * @param name          the base scope name for matches, or {@code null}
     * @param contentName   the scope name for inner content, or {@code null}
     * @param beginRegExp   the regex pattern string for the {@code begin} rule
     * @param beginCaptures the capture rules for the {@code begin} pattern
     * @param whileRegExp   the regex pattern string for the {@code while} rule
     * @param whileCaptures the capture rules for the {@code while} pattern
     * @param patterns      the result container containing child rule IDs and missing pattern status
     */
    BeginWhileRule(RuleId id,
                   @Nullable String name,
                   @Nullable String contentName,
                   String beginRegExp,
                   List<@Nullable CaptureRule> beginCaptures,
                   String whileRegExp,
                   List<@Nullable CaptureRule> whileCaptures,
                   CompilePatternsResult patterns) {
        super(id, name, contentName);

        this.beginRegExp = new RegExpSource(beginRegExp, id);
        this.beginCaptures = beginCaptures;
        this.whileCaptures = whileCaptures;
        this.whileRegExp = new RegExpSource(whileRegExp, RuleId.WHILE_RULE);
        this.whileHasBackReferences = this.whileRegExp.hasBackReferences;
        this.patterns = patterns.patterns();
        this.hasMissingPatterns = patterns.hasMissingPatterns();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Adds the {@code begin} regex to the target source list.
     */
    @Override
    public void collectPatterns(IRuleRegistry grammar, RegExpSourceList out) {
        out.add(beginRegExp);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Compiles child patterns into an executable matcher.
     */
    @Override
    public CompiledRule compile(IRuleRegistry grammar, @Nullable String endRegexSource) {
        return getCachedCompiledPatterns(grammar).compile();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Compiles child patterns into an executable matcher with anchor restrictions.
     */
    @Override
    public CompiledRule compileAG(IRuleRegistry grammar,
                                  @Nullable String endRegexSource,
                                  boolean allowA,
                                  boolean allowG) {
        return getCachedCompiledPatterns(grammar).compileAG(allowA, allowG);
    }

    /**
     * Compiles the {@code while} pattern condition into an executable matcher.
     *
     * @param endRegexSource the dynamically resolved regex for back-references, or {@code null}
     * @return the compiled rule for evaluating the {@code while} pattern condition
     */
    public CompiledRule compileWhile(@Nullable String endRegexSource) {
        return getCachedCompiledWhilePatterns(endRegexSource).compile();
    }

    /**
     * Compiles the {@code while} pattern condition with anchor restrictions ({@code \A} and {@code \G}).
     *
     * @param endRegexSource the dynamically resolved regex for back-references, or {@code null}
     * @param allowA         whether to permit anchor {@code \A} matching
     * @param allowG         whether to permit anchor {@code \G} matching
     * @return the compiled rule for evaluating the {@code while} pattern condition
     */
    public CompiledRule compileWhileAG(@Nullable String endRegexSource,
                                       boolean allowA,
                                       boolean allowG) {
        return getCachedCompiledWhilePatterns(endRegexSource).compileAG(allowA, allowG);
    }

    /**
     * Resolves back-references in the {@code while} pattern using capture offsets from a prior {@code begin} match.
     *
     * @param lineText       the line text against which the {@code begin} match occurred
     * @param captureIndices the capture group offsets from the {@code begin} match
     * @return the resolved {@code while} regex string
     */
    public String getWhileWithResolvedBackReferences(CharSequence lineText,
                                                     OnigCaptureIndex[] captureIndices) {
        return whileRegExp.resolveBackReferences(lineText, captureIndices);
    }

    //*************************************************************************

    /**
     * Retrieves or initializes the cached pattern list for child rules.
     *
     * @param grammar the rule registry used to look up child rules
     * @return the cached pattern list of child rules
     */
    private RegExpSourceList getCachedCompiledPatterns(IRuleRegistry grammar) {
        if (cachedCompiledPatterns == null) {
            cachedCompiledPatterns = new RegExpSourceList();

            for (var pattern : patterns) {
                var rule = grammar.getRule(pattern);
                rule.collectPatterns(grammar, cachedCompiledPatterns);
            }
        }
        return cachedCompiledPatterns;
    }

    /**
     * Retrieves or initializes the cached pattern list for the {@code while} pattern condition.
     *
     * @param endRegexSource the resolved regex for back-references, or {@code null}
     * @return the cached pattern list for the {@code while} pattern
     */
    private RegExpSourceList getCachedCompiledWhilePatterns(@Nullable String endRegexSource) {
        if (cachedCompiledWhilePatterns == null) {
            cachedCompiledWhilePatterns = new RegExpSourceList();
            cachedCompiledWhilePatterns.add(whileHasBackReferences ? whileRegExp.clone() : whileRegExp);
        }

        if (whileHasBackReferences) {
            cachedCompiledWhilePatterns.setSource(0, Objects.requireNonNullElse(endRegexSource, "\uFFFF"));
        }

        return cachedCompiledWhilePatterns;
    }
}