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
import tm4java.internal.grammar.dependencies.IncludeReference;
import tm4java.internal.grammar.raw.IRawCaptures;
import tm4java.internal.grammar.raw.IRawRepository;
import tm4java.internal.grammar.raw.IRawRule;
import tm4java.internal.grammar.raw.RawRule;

import java.lang.System.Logger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import tm4java.internal.utils.NullSafetyHelper;

import static java.lang.System.Logger.Level.*;
import static tm4java.internal.utils.NullSafetyHelper.castNonNull;

/**
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/rule.ts#L381">
 * vscode-textmate/src/rule.ts#L381</a>
 */
public final class RuleFactory {

    private static final Logger LOGGER = System.getLogger(RuleFactory.class.getName());

    private RuleFactory() {
        // utility class
    }

    public static RuleId getCompiledRuleId(IRawRule desc,
                                           IRuleFactoryHelper helper,
                                           IRawRepository repository) {
        if (desc.getId() == null) {
            helper.registerRule(ruleId -> {
                desc.setId(ruleId);

                var ruleMatch = desc.getMatch();
                if (ruleMatch != null) {
                    return new MatchRule(
                        ruleId,
                        desc.getName(),
                        ruleMatch,
                        compileCaptures(desc.getCaptures(), helper, repository)
                    );
                }

                var begin = desc.getBegin();
                if (begin == null) {
                    var repository_ = desc.getRepository() == null
                        ? repository
                        : IRawRepository.merge(repository, desc.getRepository());
                    var patterns = desc.getPatterns();
                    if (patterns == null && desc.getInclude() != null) {
                        patterns = List.of(new RawRule().setInclude(desc.getInclude()));
                    }
                    return new IncludeOnlyRule(
                        ruleId,
                        desc.getName(),
                        desc.getContentName(),
                        compilePatterns(patterns, helper, repository_)
                    );
                }

                String ruleWhile = desc.getWhile();
                if (ruleWhile != null) {
                    return new BeginWhileRule(
                        ruleId,
                        desc.getName(),
                        desc.getContentName(),
                        begin,
                        compileCaptures(
                            NullSafetyHelper.defaultIfNull(desc.getBeginCaptures(), desc.getCaptures()),
                            helper,
                            repository
                        ),
                        ruleWhile,
                        compileCaptures(
                            NullSafetyHelper.defaultIfNull(desc.getWhileCaptures(), desc.getCaptures()),
                            helper,
                            repository
                        ),
                        compilePatterns(desc.getPatterns(), helper, repository)
                    );
                }

                return new BeginEndRule(
                    ruleId,
                    desc.getName(),
                    desc.getContentName(),
                    begin,
                    compileCaptures(
                        NullSafetyHelper.defaultIfNull(desc.getBeginCaptures(), desc.getCaptures()),
                        helper,
                        repository
                    ),
                    desc.getEnd(),
                    compileCaptures(
                        NullSafetyHelper.defaultIfNull(desc.getEndCaptures(), desc.getCaptures()),
                        helper,
                        repository
                    ),
                    desc.isApplyEndPatternLast(),
                    compilePatterns(desc.getPatterns(), helper, repository)
                );
            });
        }

        return castNonNull(desc.getId());
    }

    //*************************************************************************

    private static CaptureRule createCaptureRule(IRuleFactoryHelper helper,
                                                 @Nullable String name,
                                                 @Nullable String contentName,
                                                 RuleId retokenizeCapturedWithRuleId) {
        return helper.registerRule(id -> new CaptureRule(id, name, contentName, retokenizeCapturedWithRuleId));
    }

    private static List<@Nullable CaptureRule> compileCaptures(@Nullable IRawCaptures captures,
                                                               IRuleFactoryHelper helper,
                                                               IRawRepository repository) {
        if (captures == null) {
            return Collections.emptyList();
        }

        // find the maximum capture id
        int maximumCaptureId = 0;
        for (String captureId : captures.getCaptureIds()) {
            int numericCaptureId = parseInt(captureId);
            if (numericCaptureId > maximumCaptureId) {
                maximumCaptureId = numericCaptureId;
            }
        }

        // initialize result
        var result = new ArrayList<@Nullable CaptureRule>(maximumCaptureId);
        for (int i = 0; i <= maximumCaptureId; i++) {
            result.add(null);
        }

        // fill out result
        captures.forEachCapture((captureId, rule) -> {
            int numericCaptureId = parseInt(captureId);
            RuleId retokenizeCapturedWithRuleId = rule.getPatterns() == null
                ? RuleId.NO_RULE
                : getCompiledRuleId(rule, helper, repository);
            result.set(
                numericCaptureId,
                createCaptureRule(helper, rule.getName(), rule.getContentName(), retokenizeCapturedWithRuleId)
            );
        });

        return result;
    }

    @SuppressWarnings("CallToPrintStackTrace")
    private static CompilePatternsResult compilePatterns(@Nullable Collection<IRawRule> patterns,
                                                         IRuleFactoryHelper helper,
                                                         IRawRepository repository) {
        if (patterns == null) {
            return new CompilePatternsResult(new RuleId[0], false);
        }

        var r = new ArrayList<RuleId>();
        for (IRawRule pattern : patterns) {
            RuleId ruleId = null;
            var patternInclude = pattern.getInclude();
            if (patternInclude != null) {
                var reference = IncludeReference.parseInclude(patternInclude);

                switch (reference.getKind()) {
                    case BASE:
                        ruleId = getCompiledRuleId(repository.getBase(), helper, repository);
                        break;
                    case SELF:
                        ruleId = getCompiledRuleId(repository.getSelf(), helper, repository);
                        break;
                    case RELATIVE_REFERENCE:
                        // local include found in repository
                        var localIncludedRule = repository.getRule(reference.getRuleName());
                        if (localIncludedRule != null) {
                            ruleId = getCompiledRuleId(localIncludedRule, helper, repository);
                        } else {
                            LOGGER.log(DEBUG, "WARN: CANNOT find rule for scopeName [{0}]. I am [{1}]",
                                patternInclude, repository.getBase().getName()
                            );
                        }
                        break;
                    case TOP_LEVEL_REFERENCE,
                         TOP_LEVEL_REPOSITORY_REFERENCE:
                        var externalGrammarName = reference.getScopeName();

                        // external include
                        var externalGrammar = helper.getExternalGrammar(externalGrammarName, repository);

                        if (externalGrammar != null) {
                            var externalGrammarRepo = externalGrammar.getRepository();
                            String externalGrammarInclude =
                                reference.getKind() == IncludeReference.Kind.TOP_LEVEL_REPOSITORY_REFERENCE
                                    ? reference.getRuleName()
                                    : null;
                            if (externalGrammarInclude != null) {
                                var externalIncludedRule = externalGrammarRepo.getRule(externalGrammarInclude);
                                if (externalIncludedRule != null) {
                                    ruleId = getCompiledRuleId(externalIncludedRule, helper, externalGrammarRepo);
                                } else {
                                    LOGGER.log(DEBUG, "WARN: CANNOT find rule for scopeName [{0}]. I am [{1}]",
                                        patternInclude, repository.getBase().getName()
                                    );
                                }
                            } else {
                                ruleId = getCompiledRuleId(externalGrammarRepo.getSelf(), helper, externalGrammarRepo);
                            }
                        } else {
                            LOGGER.log(DEBUG, "WARN: CANNOT find grammar for scopeName [{0}]. I am [{1}]",
                                patternInclude, repository.getBase().getName()
                            );
                        }
                        break;
                }
            } else {
                ruleId = getCompiledRuleId(pattern, helper, repository);
            }

            if (ruleId != null) {
                Rule rule;
                try {
                    rule = helper.getRule(ruleId);
                } catch (IndexOutOfBoundsException e) {
                    rule = null;
                    if (patternInclude != null) {
                        LOGGER.log(DEBUG, "WARN: Include rule '" + ruleId + "' references another not yet parsed rule");
                    } else {
                        // should never happen
                        e.printStackTrace();
                    }
                }

                boolean skipRule = false;
                if (rule instanceof IncludeOnlyRule ior) {
                    if (ior.hasMissingPatterns && ior.patterns.length == 0) {
                        skipRule = true;
                    }
                } else if (rule instanceof BeginEndRule ber) {
                    if (ber.hasMissingPatterns && ber.patterns.length == 0) {
                        skipRule = true;
                    }
                } else if (rule instanceof BeginWhileRule bwr) {
                    if (bwr.hasMissingPatterns && bwr.patterns.length == 0) {
                        skipRule = true;
                    }
                }

                if (skipRule) {
                    LOGGER.log(DEBUG, "WARN: REMOVING " + rule + " ENTIRELY DUE TO EMPTY PATTERNS THAT ARE MISSING");
                    continue;
                }

                r.add(ruleId);
            }
        }

        return new CompilePatternsResult(r.toArray(RuleId[]::new), patterns.size() != r.size());
    }

    private static int parseInt(String string) {
        try {
            return Integer.parseInt(string, 10);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
