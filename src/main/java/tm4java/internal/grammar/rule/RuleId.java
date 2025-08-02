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
import tm4java.TMException;

/**
 * A unique identifier for a rule.
 * This is not part of the specification and is used only internally.
 *
 * @see <a href=
 * "https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/rule.ts#L14">
 * github.com/microsoft/vscode-textmate/blob/main/src/rule.ts</a>
 */
public final class RuleId {

    /**
     * A special constant indicating no rule.
     */
    public static final RuleId NO_RULE = new RuleId(0);

    /**
     * A special constant indicating that the {@code end} regexp matched.
     */
    public static final RuleId END_RULE = new RuleId(-1);

    /**
     * A special constant indicating that the {@code while} regexp matched.
     */
    public static final RuleId WHILE_RULE = new RuleId(-2);

    public final int id;

    private RuleId(int id) {
        this.id = id;
    }

    public boolean equals(RuleId other) {
        return id == other.id;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof RuleId other) {
            return id == other.id;
        }
        return false;
    }

    public boolean notEquals(RuleId other) {
        return id != other.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        return Integer.toString(id);
    }

    //*************************************************************************

    public static RuleId of(int id) {
        if (id < 0) {
            throw new TMException("[id] must be > 0");
        }
        return new RuleId(id);
    }
}
