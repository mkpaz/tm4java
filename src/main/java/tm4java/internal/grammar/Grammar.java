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
import tm4java.grammar.*;
import tm4java.internal.grammar.matcher.Matcher;
import tm4java.internal.grammar.oniguruma.OnigString;
import tm4java.internal.grammar.raw.IRawGrammar;
import tm4java.internal.grammar.raw.IRawRepository;
import tm4java.internal.grammar.raw.IRawRule;
import tm4java.internal.grammar.raw.RawRule;
import tm4java.internal.grammar.rule.IRuleFactoryHelper;
import tm4java.internal.grammar.rule.Rule;
import tm4java.internal.grammar.rule.RuleFactory;
import tm4java.internal.grammar.rule.RuleId;
import tm4java.internal.registry.IGrammarRepository;
import tm4java.internal.registry.IThemeProvider;
import tm4java.internal.utils.ObjectCloner;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;

/**
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/grammar/grammar.ts#L98">
 * vscode-textmate/src/grammar/grammar.ts#L98</a>
 */
public final class Grammar implements IGrammar, IRuleFactoryHelper {

    private static final Logger LOGGER = System.getLogger(Grammar.class.getName());

    private final String rootScopeName;
    private final BasicScopeAttributesProvider basicScopeAttributesProvider;
    private final IGrammarRepository grammarRepository;
    private final IRawGrammar grammar;
    private final @Nullable BalancedBracketSelectors balancedBracketSelectors;

    private final Map<RuleId, @Nullable Rule> ruleId2desc = new HashMap<>();
    private final Map<String /*scopeName*/, IRawGrammar> includedGrammars = new HashMap<>();
    private final List<TokenTypeMatcher> tokenTypeMatchers = new ArrayList<>();

    private @Nullable RuleId rootId;
    private int lastRuleId = 0;
    private @Nullable List<Injection> injections;

    final IThemeProvider themeProvider;

    public Grammar(String rootScopeName,
                   IRawGrammar grammar,
                   int initialLanguage,
                   @Nullable Map<String, Integer> embeddedLanguages,
                   @Nullable Map<String, Integer> tokenTypes,
                   @Nullable BalancedBracketSelectors balancedBracketSelectors,
                   IGrammarRepository grammarRepository,
                   IThemeProvider themeProvider) {

        this.rootScopeName = rootScopeName;
        this.basicScopeAttributesProvider = new BasicScopeAttributesProvider(initialLanguage, embeddedLanguages);
        this.grammarRepository = grammarRepository;
        this.grammar = initGrammar(grammar, null);
        this.balancedBracketSelectors = balancedBracketSelectors;
        this.themeProvider = themeProvider;

        if (tokenTypes != null) {
            for (var entry : tokenTypes.entrySet()) {
                var selector = entry.getKey();
                int type = entry.getValue();
                for (var matcher : Matcher.createMatchers(selector)) {
                    tokenTypeMatchers.add(new TokenTypeMatcher(matcher.matcher(), type));
                }
            }
        }
    }

    @Override
    public String toString() {
        return "Grammar{"
            + "name=" + getName()
            + ", scopeName=" + getScopeName()
            + '}';
    }

    //*************************************************************************
    // IGrammar
    //*************************************************************************

    @Override
    public @Nullable String getName() {
        return grammar.getName();
    }

    @Override
    public String getScopeName() {
        return rootScopeName;
    }

    @Override
    public Collection<String> getFileTypes() {
        return grammar.getFileTypes();
    }

    @Override
    public @Nullable String getFoldingStartMarker() {
        return grammar.getFoldingStartMarker();
    }

    @Override
    public @Nullable String getFoldingEndMarker() {
        return grammar.getFoldingEndMarker();
    }

    @Override
    public ITokenizeLineResult<IToken[]> tokenizeLine(String lineText) {
        return tokenizeLine(lineText, null, null);
    }

    @Override
    public ITokenizeLineResult<IToken[]> tokenizeLine(String lineText,
                                                      @Nullable IStateStack prevState,
                                                      @Nullable Duration timeLimit) {
        return doTokenize(lineText, (StateStack) prevState, false, timeLimit);
    }

    @Override
    public ITokenizeLineResult<int[]> tokenizeLine2(String lineText) {
        return tokenizeLine2(lineText, null, null);
    }

    @Override
    public ITokenizeLineResult<int[]> tokenizeLine2(String lineText,
                                                    @Nullable IStateStack prevState,
                                                    @Nullable Duration timeLimit) {
        return doTokenize(lineText, (StateStack) prevState, true, timeLimit);
    }

    //*************************************************************************
    // IRuleFactoryHelper
    //*************************************************************************

    @Override
    public Rule getRule(RuleId ruleId) {
        var rule = ruleId2desc.get(ruleId);
        if (rule == null) {
            throw new IndexOutOfBoundsException(
                "No rule with index " + ruleId.id + " found. Possible values: 0.." + ruleId2desc.size()
            );
        }
        return rule;
    }

    @Override
    public <T extends Rule> T registerRule(Function<RuleId, T> factory) {
        var id = RuleId.of(++lastRuleId);
        T result = factory.apply(id);
        ruleId2desc.put(id, result);
        return result;
    }

    @Override
    public @Nullable IRawGrammar getExternalGrammar(String scopeName,
                                                    @Nullable IRawRepository repository) {
        if (includedGrammars.containsKey(scopeName)) {
            return includedGrammars.get(scopeName);
        }

        IRawGrammar rawIncludedGrammar = grammarRepository.lookup(scopeName);
        if (rawIncludedGrammar != null) {
            includedGrammars.put(
                scopeName,
                initGrammar(rawIncludedGrammar, repository != null ? repository.getBase() : null)
            );
            return includedGrammars.get(scopeName);
        }

        return null;
    }

    //*************************************************************************

    BasicScopeAttributes getMetadataForScope(String scope) {
        return basicScopeAttributesProvider.getBasicScopeAttributes(scope);
    }

    List<Injection> getInjections() {
        if (injections == null) {
            injections = this.doCollectInjections();

            if (LOGGER.isLoggable(Level.TRACE) && !injections.isEmpty()) {
                LOGGER.log(Level.TRACE, "Grammar " + rootScopeName + " contains the following injections:");
                for (var injection : injections) {
                    LOGGER.log(Level.TRACE, "  - " + injection.debugSelector);
                }
            }
        }
        return injections;
    }

    private void collectInjections(List<Injection> result,
                                   String selector,
                                   IRawRule rawRule,
                                   IRuleFactoryHelper ruleFactoryHelper,
                                   IRawGrammar rawGrammar) {
        var matchers = Matcher.createMatchers(selector);
        var ruleId = RuleFactory.getCompiledRuleId(rawRule, ruleFactoryHelper, grammar.getRepository());
        for (var matcher : matchers) {
            result.add(
                new Injection(selector, matcher.matcher(), ruleId, rawGrammar, matcher.priority())
            );
        }
    }

    private List<Injection> doCollectInjections() {
        var grammarRepository = new IGrammarRepository() {
            @Override
            public @Nullable IRawGrammar lookup(String scopeName) {
                if (Objects.equals(scopeName, Grammar.this.rootScopeName)) {
                    return Grammar.this.grammar;
                }
                return getExternalGrammar(scopeName, null);
            }

            @Override
            public @Nullable Collection<String> injections(String targetScope) {
                return Grammar.this.grammarRepository.injections(targetScope);
            }
        };

        var result = new ArrayList<Injection>();
        var scopeName = rootScopeName;
        var grammar = grammarRepository.lookup(scopeName);

        if (grammar != null) {
            // add injections from the current grammar
            var rawInjections = grammar.getInjections();
            if (rawInjections != null) {
                for (var e : rawInjections.entrySet()) {
                    collectInjections(result, e.getKey(), e.getValue(), this, grammar);
                }
            }

            // add injection grammars contributed for the current scope
            var injectionScopeNames = grammarRepository.injections(scopeName);
            if (injectionScopeNames != null) {
                injectionScopeNames.forEach(injectionScopeName -> {
                    var injectionGrammar = getExternalGrammar(injectionScopeName, null);
                    if (injectionGrammar != null) {
                        var selector = injectionGrammar.getInjectionSelector();
                        if (selector != null) {
                            collectInjections(
                                result, selector, injectionGrammar.toRawRule(), this, injectionGrammar
                            );
                        }
                    }
                });
            }
        }

        result.sort(Comparator.comparingInt(i -> i.priority)); // sort by priority

        return result;
    }

    private IRawGrammar initGrammar(IRawGrammar grammar, @Nullable IRawRule base) {
        grammar = ObjectCloner.deepClone(grammar);

        var repo = grammar.getRepository();
        repo.setSelf(
            new RawRule()
                .setName(grammar.getScopeName())
                .setPatterns(grammar.getPatterns())
        );
        repo.setBase(base != null ? base : repo.getSelf());

        return grammar;
    }

    @SuppressWarnings("unchecked")
    private synchronized <T> TokenizeLineResult<T> doTokenize(String lineText,
                                                              @Nullable StateStack prevState,
                                                              boolean emitBinaryTokens,
                                                              @Nullable Duration timeLimit) {
        if (rootId == null) {
            rootId = RuleFactory.getCompiledRuleId(
                grammar.getRepository().getSelf(), this, grammar.getRepository()
            );

            // this ensures ids are deterministic, and thus equal in renderer and webworker.
            this.getInjections();
        }

        boolean isFirstLine;
        if (prevState == null || prevState == StateStack.NULL) {
            isFirstLine = true;
            var rawDefaultMetadata = basicScopeAttributesProvider.getDefaultAttributes();
            var defaultStyle = themeProvider.getDefaults();
            int defaultMetadata = EncodedTokenAttributes.set(
                0,
                rawDefaultMetadata.languageId(),
                rawDefaultMetadata.tokenType(),
                null,
                defaultStyle.fontStyle(),
                defaultStyle.foregroundId(),
                defaultStyle.backgroundId()
            );

            var rootScopeName = getRule(rootId).getName(null, null);

            AttributedScopeStack scopeList;
            if (rootScopeName != null) {
                scopeList = AttributedScopeStack.createRootAndLookUpScopeName(
                    rootScopeName, defaultMetadata, this
                );
            } else {
                scopeList = AttributedScopeStack.createRoot("unknown", defaultMetadata);
            }

            prevState = new StateStack(
                null,
                rootId,
                -1,
                -1,
                false,
                null,
                scopeList,
                scopeList
            );
        } else {
            isFirstLine = false;
            prevState.reset();
        }

        // only add LF! if the passed lineText didn't have it
        int origLength = lineText.length();
        if (lineText.isEmpty() || lineText.charAt(lineText.length() - 1) != '\n') {
            lineText += '\n';
        }

        var onigLineText = OnigString.of(lineText);
        int lineLength = onigLineText.content.length();
        var lineTokens = new LineTokens(
            emitBinaryTokens,
            lineText,
            origLength,
            tokenTypeMatchers,
            balancedBracketSelectors
        );
        var r = LineTokenizer.tokenizeString(
            this,
            onigLineText,
            isFirstLine,
            0,
            prevState,
            lineTokens,
            true,
            timeLimit == null ? Duration.ZERO : timeLimit
        );

        return (TokenizeLineResult<T>) new TokenizeLineResult<>(
            emitBinaryTokens
                ? lineTokens.getBinaryResult(r.stack(), lineLength)
                : lineTokens.getResult(r.stack(), lineLength),
            r.stack(),
            r.stoppedEarly()
        );
    }
}
