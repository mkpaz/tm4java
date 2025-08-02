/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.grammar.rule;

import java.util.List;
import tm4java.internal.grammar.oniguruma.OnigScanner;

/**
 * Represents a compiled rule.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/rule.ts#L858">
 * vscode-textmate/src/rule.ts#L858</a>
 */
public final class CompiledRule {

    /**
     * The list of regular expressions for debugging purposes.
     */
    public final List<String> debugRegExps;

    /**
     * The OnigScanner instance for matching the rule regular expressions.
     */
    public final OnigScanner scanner;

    /**
     * The array of rule ids associated with this compiled rule.
     */
    public final RuleId[] rules;

    CompiledRule(List<String> regExps, RuleId[] rules) {
        this.rules = rules;
        this.scanner = new OnigScanner(regExps);
        this.debugRegExps = regExps;
    }
}
