/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.grammar.rule;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A list of raw regular expressions (sources) that can be compiled into
 * a single compiled rule.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/rule.ts#L744">
 * vscode-textmate/src/rule.ts#L744</a>
 */
public final class RegExpSourceList {

    private final List<RegExpSource> items = new ArrayList<>();

    // cache for compiled rules with \A or \G anchors
    private final @Nullable CompiledRule[][] anchorCache = new CompiledRule[2][2];

    // flag indicating whether any of the regexps have \A or \G anchors
    private boolean hasAnchors;

    private @Nullable CompiledRule cached;

    /**
     * Adds a regular expression to the list.
     */
    void add(RegExpSource item) {
        items.add(item);
        if (!hasAnchors) {
            hasAnchors = item.hasAnchor();
        }
    }

    /**
     * Removes a regular expression from the list.
     */
    void remove(RegExpSource item) {
        items.addFirst(item);
        if (!hasAnchors) {
            hasAnchors = item.hasAnchor();
        }
    }

    /**
     * Returns the number of regular expressions in the list.
     */
    int length() {
        return items.size();
    }

    /**
     * Sets the regular expression at a given index.
     *
     * @param index     the index of the regular expression
     * @param newSource the new regular expression
     */
    void setSource(int index, String newSource) {
        RegExpSource regexp = items.get(index);
        if (!Objects.equals(regexp.getSource(), newSource)) {
            disposeCache();
            regexp.setSource(newSource);
        }
    }

    /**
     * Compiles the regular expressions in this list into a single compiled rule.
     */
    CompiledRule compile() {
        if (cached == null) {
            List<String> regexps = items.stream().map(RegExpSource::getSource).toList();
            cached = new CompiledRule(regexps, items.stream()
                                                   .map(item -> item.ruleId)
                                                   .toArray(RuleId[]::new));
        }
        return cached;
    }

    /**
     * Compiles the regular expressions in this list into a single compiled rule.
     *
     * @param allowA whether to allow \A (the start of the input)
     * @param allowG whether to allow \G (the start of the input or the start of the last match)
     */
    CompiledRule compileAG(boolean allowA, boolean allowG) {
        if (!hasAnchors) {
            return compile();
        }

        var indexA = allowA ? 1 : 0;
        var indexG = allowG ? 1 : 0;

        var rule = anchorCache[indexA][indexG];
        if (rule == null) {
            rule = anchorCache[indexA][indexG] = resolveAnchors(allowA, allowG);
        }
        return rule;
    }

    //*************************************************************************

    private CompiledRule resolveAnchors(boolean allowA, boolean allowG) {
        List<String> regexps = items.stream()
                                   .map(item -> item.resolveAnchors(allowA, allowG))
                                   .toList();
        return new CompiledRule(
            regexps, items.stream().map(item -> item.ruleId).toArray(RuleId[]::new)
        );
    }

    private void disposeCache() {
        cached = null;
        anchorCache[0][0] = null;
        anchorCache[0][1] = null;
        anchorCache[1][0] = null;
        anchorCache[1][1] = null;
    }
}
