/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.grammar;

import org.jspecify.annotations.Nullable;
import tm4java.internal.grammar.rule.BeginEndRule;
import tm4java.internal.grammar.rule.BeginWhileRule;
import tm4java.internal.grammar.rule.CaptureRule;
import tm4java.internal.grammar.rule.CompiledRule;
import tm4java.internal.grammar.rule.MatchRule;
import tm4java.internal.grammar.rule.Rule;
import tm4java.internal.grammar.rule.RuleId;
import tm4java.internal.grammar.oniguruma.OnigCaptureIndex;
import tm4java.internal.grammar.oniguruma.OnigScannerMatch;
import tm4java.internal.grammar.oniguruma.OnigString;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.time.Duration;
import java.util.*;

import static java.lang.System.Logger.Level.INFO;
import static java.lang.System.Logger.Level.TRACE;
import static tm4java.internal.utils.NullSafetyHelper.castNonNull;

/**
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/grammar/tokenizeString.ts#L31">
 * vscode-textmate/src/grammar/tokenizeString.ts#L31</a>
 */
final class LineTokenizer {

    private static final Logger LOGGER = System.getLogger(LineTokenizer.class.getName());

    record TokenizeStringResult(StateStack stack,
                                boolean stoppedEarly) {
    }

    private static class MatchResult {
        final OnigCaptureIndex[] captureIndices;
        final RuleId matchedRuleId;

        MatchResult(RuleId matchedRuleId,
                    OnigCaptureIndex[] captureIndices) {
            this.matchedRuleId = matchedRuleId;
            this.captureIndices = captureIndices;
        }
    }

    private static final class MatchInjectionsResult extends MatchResult {
        final boolean isPriorityMatch;

        MatchInjectionsResult(RuleId matchedRuleId,
                              OnigCaptureIndex[] captureIndices,
                              boolean isPriorityMatch) {
            super(matchedRuleId, captureIndices);
            this.isPriorityMatch = isPriorityMatch;
        }
    }

    private record LocalStackElement(AttributedScopeStack scopes,
                                     int endPos) {
    }

    private record WhileCheckResult(StateStack stack,
                                    int linePos,
                                    int anchorPosition,
                                    boolean isFirstLine) {
    }

    private record WhileStack(StateStack stack,
                              BeginWhileRule rule) {
    }

    //*************************************************************************

    private final Grammar grammar;
    private final OnigString lineText;
    private boolean isFirstLine;
    private int linePos;
    private StateStack stack;
    private final LineTokens lineTokens;
    private int anchorPosition = -1;
    private boolean stop;
    private final int lineLength;

    private LineTokenizer(Grammar grammar,
                          OnigString lineText,
                          boolean isFirstLine,
                          int linePos,
                          StateStack stack,
                          LineTokens lineTokens) {
        this.grammar = grammar;
        this.lineText = lineText;
        this.isFirstLine = isFirstLine;
        this.linePos = linePos;
        this.stack = stack;
        this.lineTokens = lineTokens;
        this.lineLength = lineText.content.length();
    }

    static TokenizeStringResult tokenizeString(Grammar grammar,
                                               OnigString lineText,
                                               boolean isFirstLine,
                                               int linePos,
                                               StateStack stack,
                                               LineTokens lineTokens,
                                               boolean checkWhileConditions,
                                               Duration timeLimit) {
        return new LineTokenizer(grammar, lineText, isFirstLine, linePos, stack, lineTokens)
            .scan(checkWhileConditions, timeLimit.toMillis());
    }

    public static String debugCompiledRuleToString(CompiledRule ruleScanner) {
        var r = new ArrayList<String>(ruleScanner.rules.length);
        for (int i = 0, l = ruleScanner.rules.length; i < l; i++) {
            r.add("   - " + ruleScanner.rules[i] + ": " + ruleScanner.debugRegExps.get(i));
        }
        return String.join(System.lineSeparator(), r);
    }

    //*************************************************************************

    private TokenizeStringResult scan(boolean checkWhileConditions, long timeLimit) {
        stop = false;

        if (checkWhileConditions) {
            var whileCheckResult = checkWhileConditions(
                grammar, lineText, isFirstLine, linePos, stack, lineTokens
            );
            stack = whileCheckResult.stack;
            linePos = whileCheckResult.linePos;
            isFirstLine = whileCheckResult.isFirstLine;
            anchorPosition = whileCheckResult.anchorPosition;
        }

        var startTime = System.currentTimeMillis();
        while (!stop) {
            if (timeLimit > 0) {
                var elapsedTime = System.currentTimeMillis() - startTime;
                if (elapsedTime > timeLimit) {
                    return new TokenizeStringResult(stack, true);
                }
            }
            scanNext(); // potentially modifies linePos && anchorPosition
        }

        return new TokenizeStringResult(stack, false);
    }

    private void scanNext() {
        LOGGER.log(TRACE, () -> "@@scanNext: |" + lineText.content.replace("\n", "\\n").substring(linePos) + '|');

        MatchResult r = matchRuleOrInjections(
            grammar, lineText, isFirstLine, linePos, stack, anchorPosition
        );

        if (r == null) {
            // no match
            LOGGER.log(TRACE, " no more matches.");
            lineTokens.produce(stack, lineLength);
            stop = true;
            return;
        }

        OnigCaptureIndex[] captureIndices = r.captureIndices;
        RuleId matchedRuleId = r.matchedRuleId;
        boolean hasAdvanced = captureIndices.length > 0 && captureIndices[0].end() > linePos;

        if (matchedRuleId.equals(RuleId.END_RULE)) {
            // we matched the `end` for this rule => pop it
            BeginEndRule poppedRule = (BeginEndRule) stack.getRule(grammar);

            lineTokens.produce(stack, captureIndices[0].start());
            stack = stack.withContentNameScopesList(stack.nameScopesList);
            handleCaptures(
                grammar, lineText, isFirstLine, stack, lineTokens, poppedRule.endCaptures, captureIndices
            );
            lineTokens.produce(stack, captureIndices[0].end());

            // pop
            var popped = stack;
            stack = Objects.requireNonNull(stack.pop(), "Unexpected null value");
            anchorPosition = popped.getAnchorPos();

            if (!hasAdvanced && popped.getEnterPos() == linePos) {
                // grammar pushed & popped a rule without advancing
                var msg = "[1] - Grammar is in an endless loop - Grammar pushed & popped a rule without advancing";
                LOGGER.log(INFO, msg);
                // See https://github.com/microsoft/vscode-textmate/issues/12
                // Let's assume this was a mistake by the grammar author and the intent was to continue in this state
                stack = popped;

                lineTokens.produce(stack, lineLength);
                stop = true;
                return;
            }
        } else if (captureIndices.length > 0) {
            // we matched a rule!
            Rule rule = grammar.getRule(matchedRuleId);
            lineTokens.produce(stack, captureIndices[0].start());
            StateStack beforePush = stack;

            // push it on the stack rule
            var scopeName = rule.getName(lineText.content, captureIndices);
            var nameScopesList = castNonNull(stack.contentNameScopesList).pushAttributed(scopeName, grammar);
            stack = stack.push(
                matchedRuleId,
                linePos,
                anchorPosition,
                captureIndices[0].end() == lineLength,
                null,
                nameScopesList,
                nameScopesList
            );

            if (rule instanceof BeginEndRule pushedRule) {
                handleCaptures(
                    grammar,
                    lineText,
                    isFirstLine,
                    stack,
                    lineTokens,
                    pushedRule.beginCaptures,
                    captureIndices
                );
                lineTokens.produce(stack, captureIndices[0].end());
                anchorPosition = captureIndices[0].end();

                var contentName = pushedRule.getContentName(lineText.content, captureIndices);
                var contentNameScopesList = nameScopesList.pushAttributed(contentName, grammar);
                stack = stack.withContentNameScopesList(contentNameScopesList);

                if (pushedRule.endHasBackReferences) {
                    stack = stack.withEndRule(
                        pushedRule.getEndWithResolvedBackReferences(lineText.content, captureIndices)
                    );
                }

                if (!hasAdvanced && beforePush.hasSameRuleAs(stack)) {
                    // grammar pushed the same rule without advancing
                    LOGGER.log(INFO,
                        "[2] - Grammar is in an endless loop - Grammar pushed the same rule without advancing");
                    stack = castNonNull(stack.pop());
                    lineTokens.produce(stack, lineLength);
                    stop = true;
                    return;
                }
            } else if (rule instanceof BeginWhileRule pushedRule) {
                handleCaptures(
                    grammar,
                    lineText,
                    isFirstLine,
                    stack,
                    lineTokens,
                    pushedRule.beginCaptures,
                    captureIndices
                );
                lineTokens.produce(stack, captureIndices[0].end());
                anchorPosition = captureIndices[0].end();
                var contentName = pushedRule.getContentName(lineText.content, captureIndices);
                var contentNameScopesList = nameScopesList.pushAttributed(contentName, grammar);
                stack = stack.withContentNameScopesList(contentNameScopesList);

                if (pushedRule.whileHasBackReferences) {
                    stack = stack.withEndRule(
                        pushedRule.getWhileWithResolvedBackReferences(lineText.content, captureIndices)
                    );
                }

                if (!hasAdvanced && beforePush.hasSameRuleAs(stack)) {
                    // Grammar pushed the same rule without advancing
                    LOGGER.log(INFO,
                        "[3] - Grammar is in an endless loop - Grammar pushed the same rule without advancing");
                    stack = castNonNull(stack.pop());
                    lineTokens.produce(stack, lineLength);
                    stop = true;
                    return;
                }
            } else {
                MatchRule matchingRule = (MatchRule) rule;

                handleCaptures(
                    grammar,
                    lineText,
                    isFirstLine,
                    stack,
                    lineTokens,
                    matchingRule.captures,
                    captureIndices
                );
                lineTokens.produce(stack, captureIndices[0].end());

                // pop rule immediately since it is a MatchRule
                stack = castNonNull(stack.pop());

                if (!hasAdvanced) {
                    // grammar is not advancing, nor is it pushing/popping
                    LOGGER.log(INFO,
                        "[4] - Grammar is in an endless loop - Grammar is not advancing, nor is it pushing/popping");
                    stack = stack.safePop();
                    lineTokens.produce(stack, lineLength);
                    stop = true;
                    return;
                }
            }
        }

        if (captureIndices.length > 0 && captureIndices[0].end() > linePos) {
            // advance stream
            linePos = captureIndices[0].end();
            isFirstLine = false;
        }
    }

    private @Nullable MatchResult matchRule(Grammar grammar,
                                            OnigString lineText,
                                            boolean isFirstLine,
                                            int linePos,
                                            StateStack stack,
                                            int anchorPosition) {
        var rule = stack.getRule(grammar);
        var ruleScanner = rule.compileAG(grammar, stack.endRule, isFirstLine, linePos == anchorPosition);

        OnigScannerMatch r = ruleScanner.scanner.findNextMatch(lineText, linePos);

        if (r != null) {
            return new MatchResult(ruleScanner.rules[r.index], r.getCaptureIndices());
        }
        return null;
    }

    private @Nullable MatchResult matchRuleOrInjections(Grammar grammar,
                                                        OnigString lineText,
                                                        boolean isFirstLine,
                                                        int linePos,
                                                        StateStack stack,
                                                        int anchorPosition) {
        // look for normal grammar rule
        MatchResult matchResult = matchRule(grammar, lineText, isFirstLine, linePos, stack, anchorPosition);

        // look for injected rules
        List<Injection> injections = grammar.getInjections();
        if (injections.isEmpty()) {
            // No injections whatsoever => early return
            return matchResult;
        }

        var injectionResult =
            matchInjections(injections, grammar, lineText, isFirstLine, linePos, stack, anchorPosition);
        if (injectionResult == null) {
            // no injections matched => early return
            return matchResult;
        }

        if (matchResult == null) {
            // only injections matched => early return
            return injectionResult;
        }

        // decide if `matchResult` or `injectionResult` should win
        int matchResultScore = matchResult.captureIndices[0].start();
        int injectionResultScore = injectionResult.captureIndices[0].start();

        if (injectionResultScore < matchResultScore
            || injectionResult.isPriorityMatch
            && injectionResultScore == matchResultScore) {
            // injection won!
            return injectionResult;
        }

        return matchResult;
    }

    private @Nullable MatchInjectionsResult matchInjections(List<Injection> injections,
                                                            Grammar grammar,
                                                            OnigString lineText,
                                                            boolean isFirstLine,
                                                            int linePos,
                                                            StateStack stack,
                                                            int anchorPosition) {
        // the lower, the better
        var bestMatchRating = Integer.MAX_VALUE;
        OnigCaptureIndex[] bestMatchCaptureIndices = null;
        var bestMatchRuleId = RuleId.END_RULE;
        var bestMatchResultPriority = 0;

        List<String> scopes = stack.contentNameScopesList != null
            ? stack.contentNameScopesList.getScopeNames()
            : Collections.emptyList();

        for (Injection injection : injections) {
            if (!injection.matches(scopes)) {
                // injection selector doesn't match stack
                continue;
            }

            var rule = grammar.getRule(injection.ruleId);
            var ruleScanner = rule.compileAG(
                grammar, null, isFirstLine, linePos == anchorPosition
            );
            var matchResult = ruleScanner.scanner.findNextMatch(lineText, linePos);
            if (matchResult == null) {
                continue;
            }

            if (LOGGER.isLoggable(Level.TRACE)) {
                LOGGER.log(Level.TRACE, "  matched injection: " + injection.debugSelector);
                LOGGER.log(Level.TRACE, debugCompiledRuleToString(ruleScanner));
            }

            int matchRating = matchResult.getCaptureIndices()[0].start();
            if (matchRating > bestMatchRating) {
                // injections are sorted by priority, so the previous injection had a better or equal priority
                continue;
            }

            bestMatchRating = matchRating;
            bestMatchCaptureIndices = matchResult.getCaptureIndices();
            bestMatchRuleId = ruleScanner.rules[matchResult.index];
            bestMatchResultPriority = injection.priority;

            if (bestMatchRating == linePos) {
                // no more need to look at the rest of the injections
                break;
            }
        }

        if (bestMatchCaptureIndices != null) {
            return new MatchInjectionsResult(
                bestMatchRuleId,
                bestMatchCaptureIndices,
                bestMatchResultPriority == -1
            );
        }

        return null;
    }

    private void handleCaptures(Grammar grammar,
                                OnigString lineText,
                                boolean isFirstLine,
                                StateStack stack,
                                LineTokens lineTokens,
                                List<@Nullable CaptureRule> captures,
                                OnigCaptureIndex[] captureIndices) {
        if (captures.isEmpty()) {
            return;
        }

        var lineTextContent = lineText.content;

        int len = Math.min(captures.size(), captureIndices.length);
        var localStack = new ArrayDeque<LocalStackElement>();
        int maxEnd = captureIndices[0].end();

        for (int i = 0; i < len; i++) {
            var captureRule = captures.get(i);
            if (captureRule == null) {
                // not interested
                continue;
            }

            var captureIndex = captureIndices[i];

            if (captureIndex.getLength() == 0) {
                // nothing really captured
                continue;
            }

            if (captureIndex.start() > maxEnd) {
                // capture going beyond consumed string
                break;
            }

            // pop captures while needed
            while (!localStack.isEmpty() && localStack.getLast().endPos <= captureIndex.start()) {
                // pop!
                var lastElem = localStack.removeLast();
                lineTokens.produceFromScopes(lastElem.scopes, lastElem.endPos);
            }

            if (!localStack.isEmpty()) {
                lineTokens.produceFromScopes(localStack.getLast().scopes, captureIndex.start());
            } else {
                lineTokens.produce(stack, captureIndex.start());
            }

            var retokenizeCapturedWithRuleId = captureRule.retokenizeCapturedWithRuleId;
            if (retokenizeCapturedWithRuleId.notEquals(RuleId.NO_RULE)) {
                // the capture requires additional matching
                var scopeName = captureRule.getName(lineTextContent, captureIndices);
                var nameScopesList = castNonNull(stack.contentNameScopesList).pushAttributed(scopeName, grammar);
                var contentName = captureRule.getContentName(lineTextContent, captureIndices);
                var contentNameScopesList = nameScopesList.pushAttributed(contentName, grammar);

                // the capture requires additional matching
                var stackClone =
                    stack.push(
                        retokenizeCapturedWithRuleId,
                        captureIndex.start(),
                        -1,
                        false,
                        null,
                        nameScopesList,
                        contentNameScopesList
                    );
                var onigSubStr = OnigString.of(lineTextContent.substring(0, captureIndex.end()));
                tokenizeString(
                    grammar,
                    onigSubStr,
                    isFirstLine && captureIndex.start() == 0,
                    captureIndex.start(),
                    stackClone,
                    lineTokens,
                    false,
                    Duration.ZERO // no time limit
                );
                continue;
            }

            var captureRuleScopeName = captureRule.getName(lineTextContent, captureIndices);
            if (captureRuleScopeName != null) {
                // push
                var base = localStack.isEmpty() ? stack.contentNameScopesList : localStack.getLast().scopes;
                var captureRuleScopesList = castNonNull(base).pushAttributed(captureRuleScopeName, grammar);
                localStack.add(new LocalStackElement(captureRuleScopesList, captureIndex.end()));
            }
        }

        while (!localStack.isEmpty()) {
            // pop!
            var lastElem = localStack.removeLast();
            lineTokens.produceFromScopes(lastElem.scopes, lastElem.endPos);
        }
    }

    /**
     * Walk the stack from bottom to top, and check each while condition in this order.
     * If any fails, cut off the entire stack above the failed while condition.
     * While conditions may also advance the linePosition.
     */
    private WhileCheckResult checkWhileConditions(Grammar grammar,
                                                  OnigString lineText,
                                                  boolean isFirstLine,
                                                  int linePos,
                                                  StateStack stack,
                                                  LineTokens lineTokens) {
        int anchorPosition = stack.beginRuleCapturedEOL ? 0 : -1;

        var whileRules = new ArrayList<WhileStack>();
        for (StateStack node = stack; node != null; node = node.pop()) {
            Rule nodeRule = node.getRule(grammar);
            if (nodeRule instanceof BeginWhileRule beginWhileRule) {
                whileRules.add(new WhileStack(node, beginWhileRule));
            }
        }

        for (int i = whileRules.size() - 1; i >= 0; i--) {
            var whileRule = whileRules.get(i);

            var ruleScanner = whileRule.rule.compileWhileAG(
                whileRule.stack.endRule, isFirstLine, anchorPosition == linePos
            );
            var r = ruleScanner.scanner.findNextMatch(lineText, linePos);
            if (LOGGER.isLoggable(TRACE)) {
                LOGGER.log(TRACE, "  scanning for while rule");
                LOGGER.log(TRACE, debugCompiledRuleToString(ruleScanner));
            }

            if (r != null) {
                RuleId matchedRuleId = ruleScanner.rules[r.index];
                if (RuleId.WHILE_RULE.notEquals(matchedRuleId)) {
                    // we shouldn't end up here
                    stack = castNonNull(whileRule.stack.pop());
                    break;
                }
                if (r.getCaptureIndices().length > 0) {
                    lineTokens.produce(whileRule.stack, r.getCaptureIndices()[0].start());
                    handleCaptures(grammar,
                        lineText,
                        isFirstLine,
                        whileRule.stack,
                        lineTokens,
                        whileRule.rule.whileCaptures,
                        r.getCaptureIndices()
                    );
                    lineTokens.produce(whileRule.stack, r.getCaptureIndices()[0].end());
                    anchorPosition = r.getCaptureIndices()[0].end();
                    if (r.getCaptureIndices()[0].end() > linePos) {
                        linePos = r.getCaptureIndices()[0].end();
                        isFirstLine = false;
                    }
                }
            } else {
                stack = castNonNull(whileRule.stack.pop());
                break;
            }
        }

        return new WhileCheckResult(stack, linePos, anchorPosition, isFirstLine);
    }
}
