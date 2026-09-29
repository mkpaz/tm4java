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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An immutable singly-linked node representing a stack of scope names.
 *
 * <p>Forms a scope hierarchy chain used during TextMate tokenization to track active scopes
 * and match thematic styling rules.
 *
 * @param parent    the parent scope node in the stack hierarchy, or {@code null}
 *                  if this is the root scope
 * @param scopeName the TextMate scope name stored at this stack node
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/theme.ts#L101">
 * vscode-textmate/src/theme.ts#L101</a>
 */
public record ScopeStack(@Nullable ScopeStack parent, String scopeName) {

    /**
     * Creates a new single-element {@link ScopeStack} starting with a root scope name.
     *
     * @param first the initial scope name
     * @return a new {@code ScopeStack} instance
     */
    public static ScopeStack from(String first) {
        return new ScopeStack(null, first);
    }

    /**
     * Creates a {@link ScopeStack} from an array of scope segments processed from left to right.
     *
     * @param segments the sequence of scope names to form the stack
     * @return the head of the constructed {@code ScopeStack}, or {@code null} if no segments were provided
     */
    public static @Nullable ScopeStack from(String... segments) {
        ScopeStack result = null;
        for (String segment : segments) {
            result = new ScopeStack(result, segment);
        }
        return result;
    }

    /**
     * Pushes a list of scope names onto an existing scope stack.
     *
     * @param path       the existing base scope stack, or {@code null}
     * @param scopeNames the sequence of scope names to push onto the stack
     * @return the new head of the extended scope stack
     */
    public static @Nullable ScopeStack push(@Nullable ScopeStack path, List<String> scopeNames) {
        for (var name : scopeNames) {
            path = new ScopeStack(path, name);
        }
        return path;
    }

    //*************************************************************************

    /**
     * Pushes a single scope name onto this stack.
     *
     * @param scopeName the scope name to append
     * @return a new {@link ScopeStack} representing the extended stack
     */
    public ScopeStack push(String scopeName) {
        return new ScopeStack(this, scopeName);
    }

    /**
     * Traverses the stack from root to leaf and returns the complete list of scope names.
     *
     * @return an ordered list of scope names in top-down hierarchy order
     */
    public List<String> getSegments() {
        ScopeStack item = this;
        var result = new ArrayList<String>();

        while (item != null) {
            result.add(item.scopeName);
            item = item.parent;
        }

        Collections.reverse(result);
        return result;
    }

    /**
     * Returns the relative extension of scope names appended on top of a specified base stack.
     *
     * @param base the expected ancestor base scope stack
     * @return an ordered list of scope names appended relative to {@code base},
     * or an empty list if this stack does not extend {@code base}
     */
    public List<String> getExtensionIfDefined(@Nullable ScopeStack base) {
        ScopeStack item = this;
        var result = new ArrayList<String>();

        while (item != null && item != base) {
            result.add(item.scopeName);
            item = item.parent;
        }

        if (item == base) {
            Collections.reverse(result);
            return result;
        }

        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return String.join(" ", getSegments());
    }
}
