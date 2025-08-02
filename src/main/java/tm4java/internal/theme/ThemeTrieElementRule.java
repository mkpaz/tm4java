/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.theme;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a rule in a theme trie.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/theme.ts#L430">
 * vscode-textmate/src/theme.ts#ThemeTrieElementRule#L430</a>
 * @see <a href="https://github.com/microsoft/vscode/blob/1.102.2/src/vs/editor/common/languages/supports/tokenization.ts#L276">
 * vscode/src/tokenization.ts#ThemeTrieElementRule#L276</a>
 */
public class ThemeTrieElementRule {

    public int scopeDepth;
    public final List<String> parentScopes;
    public int fontStyle;
    public int foreground;
    public int background;

    /**
     * Creates a new theme trie element rule.
     *
     * @param scopeDepth   the scope depth
     * @param parentScopes the list of parent scopes
     * @param fontStyle    the font style mask
     * @param foreground   the foreground color id
     * @param background   the background color id
     * @see FontStyle
     */
    public ThemeTrieElementRule(int scopeDepth,
                                @Nullable List<String> parentScopes,
                                int fontStyle,
                                int foreground,
                                int background) {
        this.scopeDepth = scopeDepth;
        this.parentScopes = parentScopes == null ? Collections.emptyList() : parentScopes;
        this.fontStyle = fontStyle;
        this.foreground = foreground;
        this.background = background;
    }

    /**
     * Overwrites the rule's properties if the new scope depth is not greater than the current one.
     */
    public void acceptOverwrite(int scopeDepth, int fontStyle, int foreground, int background) {
        if (this.scopeDepth > scopeDepth) {
            // TODO (upstream): How did this happen?
            System.err.printf(
                "my depth: '%d', overwriting depth: '%d'. How did this happen?%n", this.scopeDepth, scopeDepth
            );
        } else {
            this.scopeDepth = scopeDepth;
        }

        if (fontStyle != FontStyle.NOT_SET) {
            this.fontStyle = fontStyle;
        }
        if (foreground != 0) {
            this.foreground = foreground;
        }
        if (background != 0) {
            this.background = background;
        }
    }

    @Override
    public int hashCode() {
        int result = 31 + background;
        result = 31 * result + fontStyle;
        result = 31 * result + foreground;
        result = 31 * result + Objects.hashCode(parentScopes);
        return 31 * result + scopeDepth;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof ThemeTrieElementRule other) {
            return scopeDepth == other.scopeDepth
                && background == other.background
                && fontStyle == other.fontStyle
                && foreground == other.foreground
                && Objects.equals(parentScopes, other.parentScopes);
        }

        return false;
    }

    @Override
    public String toString() {
        return "ThemeTrieElementRule{"
            + "scopeDepth=" + scopeDepth
            + ", parentScopes=" + parentScopes
            + ", fontStyle=" + fontStyle
            + ", foreground=" + foreground
            + ", background=" + background
            + '}';
    }

    @Override
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public ThemeTrieElementRule clone() {
        return new ThemeTrieElementRule(scopeDepth, parentScopes, fontStyle, foreground, background);
    }

    //*************************************************************************

    public static List<ThemeTrieElementRule> clone(List<ThemeTrieElementRule> list) {
        var result = new ArrayList<ThemeTrieElementRule>(list.size());
        for (var item : list) {
            result.add(item.clone());
        }
        return result;
    }
}
