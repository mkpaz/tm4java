/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.grammar;

import tm4java.internal.grammar.matcher.Matcher;
import tm4java.internal.grammar.raw.IRawGrammar;
import tm4java.internal.grammar.rule.RuleId;

import java.util.List;

/**
 * Represents a syntax injection rule evaluated against scope stacks during TextMate tokenization.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/grammar/grammar.ts#L49">
 * vscode-textmate/src/grammar/grammar.ts#L49</a>
 */
final class Injection {

    private final Matcher<List<String>> matcher;

    /** The selector string used for debugging and identification. */
    final String debugSelector;

    /** The injection priority level (-1 for 'L' / left, 0 for default, 1 for 'R' / right). */
    final int priority;

    /** The ID of the rule associated with this injection. */
    final RuleId ruleId;

    /** The raw grammar containing the injected rule definitions. */
    final IRawGrammar grammar;

    /**
     * Constructs a new {@link Injection} instance.
     *
     * @param debugSelector the string representation of the scope selector
     * @param matcher       the predicate matcher for scope chains
     * @param ruleId        the ID of the rule to inject
     * @param grammar       the grammar defining the injection
     * @param priority      the injection priority order
     */
    Injection(String debugSelector,
              Matcher<List<String>> matcher,
              RuleId ruleId,
              IRawGrammar grammar,
              int priority
    ) {
        this.debugSelector = debugSelector;
        this.matcher = matcher;
        this.ruleId = ruleId;
        this.grammar = grammar;
        this.priority = priority;
    }

    /**
     * Evaluates whether this injection matches the specified list of scope names.
     *
     * @param states the list of scope names to test against
     * @return {@code true} if the scope chain matches the injection selector, {@code false} otherwise
     */
    boolean matches(List<String> states) {
        return matcher.matches(states);
    }
}
