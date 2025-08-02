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
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/theme.ts#L101">
 * vscode-textmate/src/theme.ts#L101</a>
 */
public record ScopeStack(@Nullable ScopeStack parent, String scopeName) {

    public static ScopeStack from(String first) {
        return new ScopeStack(null, first);
    }

    public static @Nullable ScopeStack from(String... segments) {
        ScopeStack result = null;
        for (String segment : segments) {
            result = new ScopeStack(result, segment);
        }
        return result;
    }

    public static @Nullable ScopeStack push(@Nullable ScopeStack path,
                                            List<String> scopeNames) {
        for (var name : scopeNames) {
            path = new ScopeStack(path, name);
        }
        return path;
    }

    //*************************************************************************

    public ScopeStack push(String scopeName) {
        return new ScopeStack(this, scopeName);
    }

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
