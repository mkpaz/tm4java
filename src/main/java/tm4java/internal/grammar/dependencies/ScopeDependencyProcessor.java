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
 * Traverses TextMate grammar dependency trees to resolve and queue external scope references.
 *
 * <p>This processor iteratively scans rule trees (patterns, repository items, and injections) for
 * rules that reference external grammars or specific repository entries (e.g., via {@code include}
 * directives such as {@code scope#rule}). Discovered dependencies are queued so they can be loaded
 * asynchronously or on-demand by the grammar registry prior to tokenization.
 *
 * @see AbsoluteRuleReference
 * @see IGrammarRepository
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/grammar/grammarDependencies.ts#L59">
 * vscode-textmate/src/grammar/grammarDependencies.ts#L59</a>
 */
public final class ScopeDependencyProcessor {

    /** The grammar repository used to resolve grammars and injection scopes. */
    private final IGrammarRepository repo;

    /** The root scope name from which the dependency processing originated. */
    private final String initialScopeName;

    /** Set of scope names for which all top-level rules have already been queued or processed. */
    private final Set<String> seenFullScopeRequests = new HashSet</*scopeName*/>();

    /**
     * Set of unique rule keys (e.g., {@code scope#rule}) for which partial repository rules
     * have already been queued or processed.
     */
    private final Set<String> seenPartialScopeRequests = new HashSet</*scopeName*/>();

    /** Work queue containing unresolved absolute rule references awaiting processing or loading. */
    private Deque<AbsoluteRuleReference> queue = new ArrayDeque<>();

    /**
     * Constructs a new {@code ScopeDependencyProcessor} for the specified root scope name.
     *
     * <p>Initializes the processor with a {@link TopLevelRuleReference} for the initial scope.
     *
     * @param repo             the grammar repository used to look up raw grammars and cross-grammar injections
     * @param initialScopeName the entry-point scope name to resolve dependencies for (e.g., {@code "source.java"})
     */
    public ScopeDependencyProcessor(IGrammarRepository repo, String initialScopeName) {
        this.repo = repo;
        this.initialScopeName = initialScopeName;
        this.seenFullScopeRequests.add(initialScopeName);
        this.queue.add(new TopLevelRuleReference(initialScopeName));
    }

    /**
     * Returns the current queue of pending rule references that need to be processed or loaded.
     *
     * <p>An empty queue indicates that all reachable grammar dependencies have been fully resolved.
     *
     * @return a mutable {@link Deque} of pending {@link AbsoluteRuleReference} instances
     */
    public Deque<AbsoluteRuleReference> getQueue() {
        return queue;
    }

    /**
     * Processes all current references in the queue, collecting their external dependencies
     * and injections, and populates the queue with newly discovered external scope references.
     *
     * <p>This method should be called in a loop along with fetching missing grammars into the
     * {@link IGrammarRepository} until {@link #getQueue()} becomes empty.
     *
     * @throws TMException if the initial grammar for {@code initialScopeName} cannot be found in the repository
     */
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

    /**
     * Collects external rule references and injections for a specific absolute rule reference.
     *
     * @param reference             the rule reference to process
     * @param baseGrammarScopeName  the scope name of the root grammar
     * @param repo                  the repository used to look up grammars
     * @param result                the collector accumulating discovered dependencies
     */
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

    /**
     * Traverses a named repository rule in the target grammar to collect external dependencies.
     */
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

    /**
     * Traverses the top-level patterns and injections of a grammar to collect external dependencies.
     */
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

    /**
     * Iteratively evaluates a collection of raw rules for {@code include} directives,
     * resolving local references and enqueuing external ones into the collector.
     */
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

    /**
     * Context holding references to the root base grammar and current target grammar being traversed.
     */
    private static class Context {

        /** The root grammar that initiated the dependency processing session. */
        final IRawGrammar baseGrammar;

        /** The grammar currently being inspected for rule references. */
        final IRawGrammar selfGrammar;

        Context(IRawGrammar baseGrammar, IRawGrammar selfGrammar) {
            this.baseGrammar = baseGrammar;
            this.selfGrammar = selfGrammar;
        }
    }

    /**
     * Accumulator that collects discovered external rule references while tracking visited rules to prevent cycles.
     */
    private static final class ExternalReferenceCollector {

        /** Queue of discovered external rule references. */
        final Deque<AbsoluteRuleReference> references = new ArrayDeque<>();

        /** Keys of references already added to the collector. */
        final Deque<String> seenReferenceKeys = new ArrayDeque<>();

        /** Set of raw rules visited during traversal to avoid infinite loops on circular includes. */
        final Set<IRawRule> visitedRule = new HashSet<>();

        /**
         * Adds an external rule reference to the queue if it has not been recorded yet.
         *
         * @param reference the absolute rule reference to add
         */
        void add(AbsoluteRuleReference reference) {
            var key = reference.toKey();
            if (seenReferenceKeys.contains(key)) {
                return;
            }

            seenReferenceKeys.push(key);
            references.push(reference);
        }
    }

    /**
     * Context extending {@link Context} to carry local repository rules accessible in the current pattern scope.
     */
    private static final class ContextWithRepository extends Context {

        /** The combined or local repository accessible to the current rule. */
        final @Nullable IRawRepository repository;

        ContextWithRepository(Context context, @Nullable IRawRepository repository) {
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