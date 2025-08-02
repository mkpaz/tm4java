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
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/rule.ts#L290">
 * vscode-textmate/src/rule.ts#L290</a>
 */
public final class BeginWhileRule extends Rule {

    private final RegExpSource beginRegExp;
    private final RegExpSource whileRegExp;

    public final List<@Nullable CaptureRule> beginCaptures;
    public final List<@Nullable CaptureRule> whileCaptures;
    public final boolean whileHasBackReferences;

    final boolean hasMissingPatterns;
    final RuleId[] patterns;

    private @Nullable RegExpSourceList cachedCompiledPatterns;
    private @Nullable RegExpSourceList cachedCompiledWhilePatterns;

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

    @Override
    public void collectPatterns(IRuleRegistry grammar, RegExpSourceList out) {
        out.add(beginRegExp);
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

    public CompiledRule compileWhile(@Nullable String endRegexSource) {
        return getCachedCompiledWhilePatterns(endRegexSource).compile();
    }

    public CompiledRule compileWhileAG(@Nullable String endRegexSource,
                                       boolean allowA,
                                       boolean allowG) {
        return getCachedCompiledWhilePatterns(endRegexSource).compileAG(allowA, allowG);
    }

    public String getWhileWithResolvedBackReferences(CharSequence lineText,
                                                     OnigCaptureIndex[] captureIndices) {
        return whileRegExp.resolveBackReferences(lineText, captureIndices);
    }

    //*************************************************************************

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
