/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.grammar.dependencies;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import tm4java.TMException;
import tm4java.internal.grammar.dependencies.AbsoluteRuleReference.TopLevelRepositoryRuleReference;
import tm4java.internal.grammar.dependencies.AbsoluteRuleReference.TopLevelRuleReference;
import tm4java.internal.grammar.raw.IRawGrammar;
import tm4java.internal.grammar.raw.IRawRepository;
import tm4java.internal.grammar.raw.IRawRule;
import tm4java.internal.registry.IGrammarRepository;

/**
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/grammar/grammarDependencies.ts#L59">
 * vscode-textmate/src/grammar/grammarDependencies.ts#L59</a>
 */
public final class ScopeDependencyProcessor {

    private final IGrammarRepository repo;
    private final String initialScopeName;
    private final Set<String> seenFullScopeRequests = new HashSet</*scopeName*/>();
    private final Set<String> seenPartialScopeRequests = new HashSet</*scopeName*/>();

    private Deque<AbsoluteRuleReference> queue = new ArrayDeque<>();

    public ScopeDependencyProcessor(IGrammarRepository repo, String initialScopeName) {
        this.repo = repo;
        this.initialScopeName = initialScopeName;
        this.seenFullScopeRequests.add(initialScopeName);
        this.queue.add(new TopLevelRuleReference(initialScopeName));
    }

    public Deque<AbsoluteRuleReference> getQueue() {
        return queue;
    }

    public void processQueue() {
        var q = queue;
        queue = new ArrayDeque<>();

        var deps = new ExternalReferenceCollector();
        for (var dep : q) {
            collectReferencesOfReference(dep, initialScopeName, repo, deps);
        }

        for (var dep : deps.references) {
            if (dep instanceof TopLevelRuleReference) {
                if (seenFullScopeRequests.contains(dep.getScopeName())) {
                    continue; // already processed
                }
                seenFullScopeRequests.add(dep.getScopeName());
                queue.push(dep);
            } else {
                if (seenFullScopeRequests.contains(dep.getScopeName())) {
                    continue; // already processed in full
                }
                if (seenPartialScopeRequests.contains(dep.toKey())) {
                    continue; // already processed
                }
                seenPartialScopeRequests.add(dep.toKey());
                queue.push(dep);
            }
        }
    }

    //*************************************************************************

    private void collectReferencesOfReference(AbsoluteRuleReference reference,
                                              String baseGrammarScopeName,
                                              IGrammarRepository repo,
                                              ExternalReferenceCollector result) {
        var selfGrammar = repo.lookup(reference.getScopeName());
        if (selfGrammar == null) {
            if (reference.getScopeName().equals(baseGrammarScopeName)) {
                throw new TMException("No grammar provided for <" + initialScopeName + ">");
            }
            return;
        }

        var baseGrammar = Objects.requireNonNull(repo.lookup(baseGrammarScopeName));

        if (reference instanceof TopLevelRuleReference) {
            collectExternalReferencesInTopLevelRule(new Context(baseGrammar, selfGrammar), result);
        } else if (reference instanceof TopLevelRepositoryRuleReference ref) {
            collectExternalReferencesInTopLevelRepositoryRule(
                ref.getRuleName(),
                new ContextWithRepository(baseGrammar, selfGrammar, selfGrammar.getRepository()),
                result
            );
        }

        var injections = repo.injections(reference.getScopeName());
        if (injections != null) {
            for (var injection : injections) {
                result.add(new TopLevelRuleReference(injection));
            }
        }
    }


    private void collectExternalReferencesInTopLevelRepositoryRule(String ruleName,
                                                                   ContextWithRepository context,
                                                                   ExternalReferenceCollector result) {
        if (context.repository != null) {
            var rule = context.repository.getRule(ruleName);
            if (rule != null) {
                collectExternalReferencesInRules(List.of(rule), context, result);
            }
        }
    }

    private void collectExternalReferencesInTopLevelRule(Context context,
                                                         ExternalReferenceCollector result) {
        var patterns = context.selfGrammar.getPatterns();
        if (patterns != null) {
            collectExternalReferencesInRules(
                patterns,
                new ContextWithRepository(context, context.selfGrammar.getRepository()),
                result
            );
        }

        var injections = context.selfGrammar.getInjections();
        if (injections != null) {
            collectExternalReferencesInRules(
                injections.values(),
                new ContextWithRepository(context, context.selfGrammar.getRepository()),
                result
            );
        }
    }

    private void collectExternalReferencesInRules(Collection<IRawRule> rules,
                                                  ContextWithRepository context,
                                                  ExternalReferenceCollector result) {

        for (var rule : rules) {
            if (result.visitedRule.contains(rule)) {
                continue;
            }
            result.visitedRule.add(rule);

            var patternRepository = rule.getRepository() == null
                                        ? context.repository
                                        : IRawRepository.merge(context.repository, rule.getRepository());

            var patternPatterns = rule.getPatterns();
            if (patternPatterns != null) {
                collectExternalReferencesInRules(
                    patternPatterns,
                    new ContextWithRepository(context, patternRepository),
                    result
                );
            }

            var include = rule.getInclude();
            if (include == null) {
                continue;
            }

            var reference = IncludeReference.parseInclude(include);

            switch (reference.getKind()) {
                case BASE:
                    collectExternalReferencesInTopLevelRule(
                        new Context(context.baseGrammar, context.baseGrammar),
                        result
                    );
                    break;
                case SELF:
                    collectExternalReferencesInTopLevelRule(context, result);
                    break;
                case RELATIVE_REFERENCE:
                    collectExternalReferencesInTopLevelRepositoryRule(
                        reference.getRuleName(),
                        new ContextWithRepository(context, patternRepository),
                        result
                    );
                    break;
                case TOP_LEVEL_REFERENCE:
                case TOP_LEVEL_REPOSITORY_REFERENCE:
                    IRawGrammar selfGrammar = reference.getScopeName().equals(context.selfGrammar.getScopeName())
                                                  ? context.selfGrammar
                                                  : reference.getScopeName().equals(context.baseGrammar.getScopeName())
                                                        ? context.baseGrammar
                                                        : null;

                    if (selfGrammar != null) {
                        var newContext = new ContextWithRepository(
                            context.baseGrammar, selfGrammar, patternRepository
                        );

                        if (reference.getKind() == IncludeReference.Kind.TOP_LEVEL_REPOSITORY_REFERENCE) {
                            collectExternalReferencesInTopLevelRepositoryRule(
                                reference.getRuleName(), newContext, result
                            );
                        } else {
                            collectExternalReferencesInTopLevelRule(newContext, result);
                        }
                    } else {
                        if (reference.getKind() == IncludeReference.Kind.TOP_LEVEL_REPOSITORY_REFERENCE) {
                            result.add(
                                new TopLevelRepositoryRuleReference(reference.getScopeName(), reference.getRuleName())
                            );
                        } else {
                            result.add(new TopLevelRuleReference(reference.getScopeName()));
                        }
                    }
                    break;
            }
        }
    }

    //*************************************************************************

    private static class Context {

        final IRawGrammar baseGrammar;
        final IRawGrammar selfGrammar;

        Context(IRawGrammar baseGrammar, IRawGrammar selfGrammar) {
            this.baseGrammar = baseGrammar;
            this.selfGrammar = selfGrammar;
        }
    }

    private static final class ExternalReferenceCollector {

        final Deque<AbsoluteRuleReference> references = new ArrayDeque<>();
        final Deque<String> seenReferenceKeys = new ArrayDeque<>();
        final Set<IRawRule> visitedRule = new HashSet<>();

        void add(AbsoluteRuleReference reference) {
            var key = reference.toKey();

            if (seenReferenceKeys.contains(key)) {
                return;
            }

            seenReferenceKeys.push(key);
            references.push(reference);
        }
    }

    private static final class ContextWithRepository extends Context {

        final @Nullable IRawRepository repository;

        ContextWithRepository(Context context,
                              @Nullable IRawRepository repository) {
            super(context.baseGrammar, context.selfGrammar);
            this.repository = repository;
        }

        ContextWithRepository(IRawGrammar baseGrammar,
                              IRawGrammar selfGrammar,
                              @Nullable IRawRepository repository) {
            super(baseGrammar, selfGrammar);
            this.repository = repository;
        }
    }
}
