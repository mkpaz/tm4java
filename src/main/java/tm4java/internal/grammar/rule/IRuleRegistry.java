/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.grammar.rule;

import java.util.function.Function;

/**
 * A registry for managing and retrieving rules.
 *
 * <p>Responsible for allocating unique {@link RuleId} instances and storing instantiated
 * {@link Rule} objects for access during grammar compilation and tokenization.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/rule.ts#L31">
 * vscode-textmate/src/rule.ts#L31</a>
 */
public interface IRuleRegistry {

    /**
     * Retrieves a rule by its id.
     *
     * @param ruleId the internal ID of the rule
     * @return the resolved {@link Rule} instance associated with the specified ID
     * @throws IndexOutOfBoundsException if no rule with the given id was found
     */
    Rule getRule(RuleId ruleId);

    /**
     * Registers a new rule with the registry.
     *
     * @param <T>     the type of the rule
     * @param factory a function that creates a new rule instance
     * @return the registered rule
     */
    <T extends Rule> T registerRule(Function<RuleId, T> factory);
}
