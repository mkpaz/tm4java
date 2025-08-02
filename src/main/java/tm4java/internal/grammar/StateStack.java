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
import tm4java.grammar.IStateStack;
import tm4java.internal.grammar.rule.IRuleRegistry;
import tm4java.internal.grammar.rule.Rule;
import tm4java.internal.grammar.rule.RuleId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static tm4java.internal.utils.NullSafetyHelper.castNonNull;

/**
 * Represents a "pushed" state on the stack (as a linked list element).
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/grammar/grammar.ts#L592">
 * vscode-textmate/src/grammar/grammar.ts#L592</a>
 */
public final class StateStack implements IStateStack {

    public static final StateStack NULL = new StateStack(
        null, RuleId.NO_RULE, 0, 0, false, null, null, null
    );

    public record Frame(RuleId ruleId,
                        @Nullable Integer enterPos,
                        @Nullable Integer anchorPos,
                        boolean beginRuleCapturedEOL,
                        @Nullable String endRule,
                        List<AttributedScopeStack.Frame> nameScopesList,
                        // on top of nameScopesList
                        List<AttributedScopeStack.Frame> contentNameScopesList) {
    }

    /**
     * The position on the current line where this state was pushed.
     * This is relevant only while tokenizing a line, to detect endless loops.
     * Its value is meaningless across lines.
     */
    private int enterPos;

    /**
     * The captured anchor position when this stack element was pushed.
     * This is relevant only while tokenizing a line, to restore the anchor
     * position when popping. Its value is meaningless across lines.
     */
    private int anchorPos;

    /**
     * The depth of the stack.
     */
    final int depth;

    /**
     * The previous state on the stack (or null for the root state).
     */
    final @Nullable StateStack parent;

    /**
     * The state (rule) that this element represents.
     */
    final RuleId ruleId;

    /**
     * The state has entered and captured \n. This means that the next
     * line should have an anchorPosition of 0.
     */
    final boolean beginRuleCapturedEOL;

    /**
     * The "pop" (end) condition for this state in case that it was
     * dynamically generated through captured text.
     */
    final @Nullable String endRule;

    /**
     * The list of scopes containing the "name" for this state.
     */
    final @Nullable AttributedScopeStack nameScopesList;

    /**
     * The list of scopes containing the "contentName" (besides "name")
     * for this state. This list **must** contain as an element `scopeName`.
     */
    final @Nullable AttributedScopeStack contentNameScopesList;

    StateStack(@Nullable StateStack parent,
               RuleId ruleId,
               int enterPos,
               int anchorPos,
               boolean beginRuleCapturedEOL,
               @Nullable String endRule,
               @Nullable AttributedScopeStack nameScopesList,
               @Nullable AttributedScopeStack contentNameScopesList) {

        this.parent = parent;
        this.ruleId = ruleId;
        this.depth = parent != null ? parent.depth + 1 : 1;
        this.enterPos = enterPos;
        this.anchorPos = anchorPos;
        this.beginRuleCapturedEOL = beginRuleCapturedEOL;
        this.endRule = endRule;
        this.nameScopesList = nameScopesList;
        this.contentNameScopesList = contentNameScopesList;
    }

    @Override
    public int getDepth() {
        return depth;
    }

    StateStack push(RuleId ruleId,
                    int enterPos,
                    int anchorPos,
                    boolean beginRuleCapturedEOL,
                    @Nullable String endRule,
                    @Nullable AttributedScopeStack nameScopesList,
                    @Nullable AttributedScopeStack contentNameScopesList) {
        return new StateStack(
            this,
            ruleId,
            enterPos,
            anchorPos,
            beginRuleCapturedEOL,
            endRule,
            nameScopesList,
            contentNameScopesList
        );
    }

    @Nullable
    StateStack pop() {
        return parent;
    }

    StateStack safePop() {
        if (parent != null) {
            return parent;
        }
        return this;
    }

    void reset() {
        StateStack item = this;
        while (item != null) {
            item.enterPos = -1;
            item.anchorPos = -1;
            item = item.parent;
        }
    }

    int getEnterPos() {
        return enterPos;
    }

    int getAnchorPos() {
        return anchorPos;
    }

    Rule getRule(IRuleRegistry grammar) {
        return grammar.getRule(ruleId);
    }

    StateStack withContentNameScopesList(@Nullable AttributedScopeStack contentNameScopesList) {
        if (Objects.equals(this.contentNameScopesList, contentNameScopesList)) {
            return this;
        }
        return castNonNull(parent).push(
            ruleId,
            enterPos,
            anchorPos,
            beginRuleCapturedEOL,
            endRule,
            nameScopesList,
            contentNameScopesList
        );
    }

    StateStack withEndRule(String endRule) {
        if (this.endRule != null && this.endRule.equals(endRule)) {
            return this;
        }
        return new StateStack(
            parent,
            ruleId,
            enterPos,
            anchorPos,
            beginRuleCapturedEOL,
            endRule,
            nameScopesList,
            contentNameScopesList
        );
    }

    @SuppressWarnings("unused")
    Frame toStateStackFrame() {
        var nameScopesList_ = nameScopesList;
        var contentNameScopesList_ = contentNameScopesList;
        var parent_ = parent;
        return new Frame(
            ruleId,
            null,
            null,
            beginRuleCapturedEOL,
            endRule,
            nameScopesList_ != null
                ? nameScopesList_.getExtensionIfDefined(parent_ != null ? parent_.nameScopesList : null)
                : Collections.emptyList(),
            contentNameScopesList_ != null
                ? contentNameScopesList_.getExtensionIfDefined(nameScopesList)
                : Collections.emptyList()
        );
    }

    /**
     * Used to warn of endless loops
     */
    boolean hasSameRuleAs(StateStack other) {
        var item = this;
        while (item != null && item.enterPos == other.enterPos) {
            if (item.ruleId == other.ruleId) {
                return true;
            }
            item = item.parent;
        }
        return false;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (other instanceof StateStack otherState) {
            return areEqual(this, otherState);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = 31 + Objects.hashCode(contentNameScopesList);
        result = 31 * result + Objects.hashCode(endRule);
        result = 31 * result + Objects.hashCode(parent);
        result = 31 * result + Objects.hashCode(ruleId);
        return 31 * result + depth;
    }

    @Override
    public String toString() {
        var r = new ArrayList<String>();
        writeString(r);
        return '[' + String.join(", ", r) + ']';
    }

    //*************************************************************************

    private void writeString(List<String> res) {
        if (parent != null) {
            parent.writeString(res);
        }
        res.add("(" + ruleId + ", " + nameScopesList + ", " + contentNameScopesList + ")");
    }

    //*************************************************************************

    @SuppressWarnings("unused")
    public static StateStack pushFrame(@Nullable StateStack self, Frame frame) {
        var namesScopeList = AttributedScopeStack.fromExtension(self == null ? null : self.nameScopesList,
            frame.nameScopesList);
        var enterPos = frame.enterPos;
        var anchorPos = frame.anchorPos;
        return new StateStack(
            self,
            frame.ruleId,
            enterPos == null ? -1 : enterPos,
            anchorPos == null ? -1 : anchorPos,
            frame.beginRuleCapturedEOL,
            frame.endRule,
            namesScopeList,
            AttributedScopeStack.fromExtension(namesScopeList, frame.contentNameScopesList)
        );
    }

    /**
     * A structural equals check. Does not take into account `scopes`.
     */
    @SuppressWarnings("ConstantValue")
    private static boolean structuralEquals(@Nullable StateStack a,
                                            @Nullable StateStack b) {
        do {
            if (a == b) {
                return true;
            }

            if (a == null && b == null) {
                // end of list reached for both
                return true;
            }

            if (a == null || b == null) {
                // end of list reached only for one
                return false;
            }

            if (a.depth != b.depth
                || !Objects.equals(a.ruleId, b.ruleId)
                || !Objects.equals(a.endRule, b.endRule)) {
                return false;
            }

            // go to previous pair
            a = a.parent;
            b = b.parent;
        } while (true);
    }

    private static boolean areEqual(StateStack a, StateStack b) {
        if (a == b) {
            return true;
        }
        if (!structuralEquals(a, b)) {
            return false;
        }
        return AttributedScopeStack.areEqual(a.contentNameScopesList, b.contentNameScopesList);
    }
}
