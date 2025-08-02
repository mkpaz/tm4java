/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.grammar.rule;

import java.util.Objects;
import org.jspecify.annotations.Nullable;
import tm4java.internal.grammar.oniguruma.OnigCaptureIndex;

import java.util.List;

/**
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/rule.ts#L209">
 * vscode-textmate//src/rule.ts#L209</a>
 */
public final class BeginEndRule extends Rule {

    private final RegExpSource begin;
    private final RegExpSource end;
    private final boolean applyEndPatternLast;

    public final List<@Nullable CaptureRule> beginCaptures;
    public final List<@Nullable CaptureRule> endCaptures;
    public final boolean endHasBackReferences;

    final boolean hasMissingPatterns;
    final RuleId[] patterns;

    private @Nullable RegExpSourceList cachedCompiledPatterns;

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

    @Override
    public void collectPatterns(IRuleRegistry grammar, RegExpSourceList out) {
        out.add(begin);
    }

    @Override
    public CompiledRule compile(IRuleRegistry grammar, @Nullable String endRegexSource) {
        return getCachedCompiledPatterns(grammar, endRegexSource).compile();
    }

    @Override
    public CompiledRule compileAG(IRuleRegistry grammar,
                                  @Nullable String endRegexSource,
                                  boolean allowA,
                                  boolean allowG) {
        return getCachedCompiledPatterns(grammar, endRegexSource).compileAG(allowA, allowG);
    }

    public String getEndWithResolvedBackReferences(CharSequence lineText, OnigCaptureIndex[] captureIndices) {
        return end.resolveBackReferences(lineText, captureIndices);
    }

    public String debugBeginRegExp() {
        return begin.getSource();
    }

    public String debugEndRegExp() {
        return end.getSource();
    }

    //*************************************************************************

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
