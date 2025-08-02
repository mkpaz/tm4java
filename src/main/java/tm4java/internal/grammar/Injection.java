/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.grammar;

import tm4java.internal.grammar.raw.IRawGrammar;
import tm4java.internal.grammar.matcher.Matcher;
import tm4java.internal.grammar.rule.RuleId;

import java.util.List;

/**
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/grammar/grammar.ts#L49">
 * vscode-textmate/src/grammar/grammar.ts#L49</a>
 */
final class Injection {

    private final Matcher<List<String>> matcher;

    final String debugSelector;
    final int priority; // -1 = 'L', 0 = default, 1 = 'R'
    final RuleId ruleId;
    final IRawGrammar grammar;

    Injection(String debugSelector,
              Matcher<List<String>> matcher,
              RuleId ruleId,
              IRawGrammar grammar,
              int priority) {
        this.debugSelector = debugSelector;
        this.matcher = matcher;
        this.ruleId = ruleId;
        this.grammar = grammar;
        this.priority = priority;
    }

    boolean matches(List<String> states) {
        return matcher.matches(states);
    }
}
