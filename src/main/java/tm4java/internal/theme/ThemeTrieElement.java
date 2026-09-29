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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static tm4java.internal.utils.StringUtils.strArrCmp;

/**
 * Represents a node in the prefix tree (Trie) used for efficient scope matching and theme rule lookup.
 *
 * <p>Each node corresponds to a scope segment (e.g., {@code "source"} or {@code "java"}) and holds
 * styling rules applicable at that scope depth, as well as child nodes for deeper segments.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/theme.ts#L503">
 * vscode-textmate/src/theme.ts#ThemeTrieElement#L503</a>
 * @see <a href="https://github.com/microsoft/vscode/blob/1.102.2/src/vs/editor/common/languages/supports/tokenization.ts#L338">
 * vscode/src/tokenization.ts#ThemeTrieElement#L338</a>
 */
public final class ThemeTrieElement {

    /** The primary fallback rule applied when no parent scopes match. */
    final ThemeTrieElementRule mainRule;

    /** List of conditional styling rules that require parent scope matches. */
    final List<ThemeTrieElementRule> rulesWithParentScopes;

    /** Map of child trie elements keyed by scope segment name (e.g. {@code "keyword"}). */
    final Map<String /*segment*/, ThemeTrieElement> children;

    /**
     * Constructs a new root trie node with the specified main rule.
     *
     * @param mainRule the default theme rule for this scope node
     */
    public ThemeTrieElement(ThemeTrieElementRule mainRule) {
        this(mainRule, new ArrayList<>(), new HashMap<>());
    }

    /**
     * Constructs a trie node with a main rule and conditional parent scope rules.
     *
     * @param mainRule              the default theme rule for this scope node
     * @param rulesWithParentScopes list of rules that require parent scope matching
     */
    public ThemeTrieElement(ThemeTrieElementRule mainRule,
                            List<ThemeTrieElementRule> rulesWithParentScopes) {
        this(mainRule, rulesWithParentScopes, new HashMap<>());
    }

    /**
     * Constructs a trie node with full specifications including children.
     *
     * @param mainRule              the default theme rule for this scope node
     * @param rulesWithParentScopes list of rules that require parent scope matching
     * @param children              map of child nodes indexed by scope segment
     */
    public ThemeTrieElement(ThemeTrieElementRule mainRule,
                            List<ThemeTrieElementRule> rulesWithParentScopes,
                            Map<String/*segment*/, ThemeTrieElement> children) {
        this.mainRule = mainRule;
        this.rulesWithParentScopes = rulesWithParentScopes;
        this.children = children;
    }

    /**
     * Finds and returns all applicable rules for a given scope path, sorted by specificity.
     *
     * <p>Traverses down the trie following dot-separated segments in the {@code scope}.
     *
     * @param scope the target scope path segment string (e.g. {@code "keyword.control.java"})
     * @return a list of matching {@link ThemeTrieElementRule} objects ordered from most to least specific
     */
    public List<ThemeTrieElementRule> match(String scope) {
        if (!scope.isEmpty()) {
            int dotIndex = scope.indexOf('.');
            String head, tail;
            if (dotIndex == -1) {
                head = scope;
                tail = "";
            } else {
                head = scope.substring(0, dotIndex);
                tail = scope.substring(dotIndex + 1);
            }

            ThemeTrieElement child = children.get(head);
            if (child != null) {
                return child.match(tail);
            }
        }

        var rules = new ArrayList<>(rulesWithParentScopes);
        rules.add(mainRule);
        rules.sort(ThemeTrieElement::cmpBySpecificity);

        return rules;
    }

    /**
     * Inserts a theme rule into the trie hierarchy for the given scope and parent scopes.
     *
     * @param scopeDepth   the depth (number of segments) of the scope name
     * @param scope        the scope path string to index (e.g. {@code "comment.line"})
     * @param parentScopes optional list of parent scopes required for contextual matching
     * @param fontStyle    font style bitflags to apply
     * @param foreground   foreground color ID to apply
     * @param background   background color ID to apply
     */
    public void insert(int scopeDepth,
                       String scope,
                       @Nullable List<String> parentScopes,
                       int fontStyle,
                       int foreground,
                       int background) {
        if (scope.isEmpty()) {
            doInsertHere(scopeDepth, parentScopes, fontStyle, foreground, background);
            return;
        }

        int dotIndex = scope.indexOf('.');
        String head, tail;

        if (dotIndex == -1) {
            head = scope;
            tail = "";
        } else {
            head = scope.substring(0, dotIndex);
            tail = scope.substring(dotIndex + 1);
        }

        ThemeTrieElement child = children.computeIfAbsent(head, _ ->
            new ThemeTrieElement(mainRule.clone(), ThemeTrieElementRule.clone(rulesWithParentScopes))
        );

        child.insert(scopeDepth + 1, tail, parentScopes, fontStyle, foreground, background);
    }

    @Override
    public int hashCode() {
        int result = 31 + children.hashCode();
        result = 31 * result + mainRule.hashCode();
        return 31 * result + rulesWithParentScopes.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof ThemeTrieElement other) {
            return children.equals(other.children)
                && mainRule.equals(other.mainRule)
                && rulesWithParentScopes.equals(other.rulesWithParentScopes);
        }

        return false;
    }

    //*************************************************************************

    /**
     * Comparator for sorting theme rules by TextMate scope specificity.
     *
     * <p>Evaluates rules according to:
     * <ol>
     * <li>Scope depth of the primary rule (deeper scope depth wins).</li>
     * <li>Length of matching parent scopes examined depth-first (longer length wins).</li>
     * <li>Total count of parent scopes (more parent scopes wins).</li>
     * </ol>
     *
     * @param a first rule to compare
     * @param b second rule to compare
     * @return negative integer if {@code a} is more specific than {@code b},
     * positive if less specific, zero if equal
     */
    private static int cmpBySpecificity(ThemeTrieElementRule a, ThemeTrieElementRule b) {
        // First, compare the scope depths of both rules. The “scope depth” of a rule is
        // the number of segments (delimited by dots) in the rule's deepest scope name
        // (i.e. the final scope name in the scope path delimited by spaces).
        if (a.scopeDepth != b.scopeDepth) {
            return b.scopeDepth - a.scopeDepth;
        }

        // Traverse the parent scopes depth-first, comparing the specificity of both
        // rules' parent scopes, which matches the behavior described by Ranking Matches
        // in TextMate 1.5's manual: https://macromates.com/manual/en/scope_selectors
        // Start at index 0 for both rules, since the parent scopes were reversed
        // beforehand (i.e. index 0 is the deepest parent scope).
        int aParentIndex = 0;
        int bParentIndex = 0;

        int aParentScopesSize = a.parentScopes.size();
        int bParentScopesSize = b.parentScopes.size();

        while (true) {
            // child combinators don't affect specificity.
            if (aParentScopesSize > aParentIndex && ">".equals(a.parentScopes.get(aParentIndex))) {
                aParentIndex++;
            }
            if (bParentScopesSize > bParentIndex && ">".equals(b.parentScopes.get(bParentIndex))) {
                bParentIndex++;
            }

            // This is a scope-by-scope comparison, so we need to stop once a rule runs out of parent scopes.
            if (aParentIndex >= aParentScopesSize || bParentIndex >= bParentScopesSize) {
                break;
            }

            // When sorting by scope name specificity, it's safe to treat a longer parent
            // scope as more specific. If both rules' parent scopes match a given scope
            // path, the longer parent scope will always be more specific.
            int parentScopeLengthDiff = b.parentScopes.get(bParentIndex).length()
                - a.parentScopes.get(aParentIndex).length();

            if (parentScopeLengthDiff != 0) {
                return parentScopeLengthDiff;
            }

            aParentIndex++;
            bParentIndex++;
        }

        // If a depth-first, scope-by-scope comparison resulted in a tie, the rule with
        // more parent scopes is considered more specific.
        return bParentScopesSize - aParentScopesSize;
    }

    /**
     * Inserts styling attributes into the current node, merging into existing rules or creating a new rule.
     *
     * @param scopeDepth   the depth of the scope being inserted
     * @param parentScopes list of parent scope names, or {@code null} if updating the main rule
     * @param fontStyle    font style bitflags
     * @param foreground   foreground color ID
     * @param background   background color ID
     */
    private void doInsertHere(int scopeDepth,
                              @Nullable List<String> parentScopes,
                              int fontStyle,
                              int foreground,
                              int background) {

        if (parentScopes == null) {
            // merge into the main rule
            mainRule.acceptOverwrite(scopeDepth, fontStyle, foreground, background);
            return;
        }

        // try to merge into existing rule
        for (ThemeTrieElementRule rule : rulesWithParentScopes) {
            if (strArrCmp(rule.parentScopes, parentScopes) == 0) {
                // bingo! => we get to merge this into an existing one
                rule.acceptOverwrite(scopeDepth, fontStyle, foreground, background);
                return;
            }
        }

        // Must add a new rule
        //*************************************************

        // Inherit from main rule
        if (fontStyle == FontStyle.NOT_SET) {
            fontStyle = mainRule.fontStyle;
        }
        if (foreground == 0) {
            foreground = mainRule.foreground;
        }
        if (background == 0) {
            background = mainRule.background;
        }

        rulesWithParentScopes.add(
            new ThemeTrieElementRule(scopeDepth, parentScopes, fontStyle, foreground, background)
        );
    }
}