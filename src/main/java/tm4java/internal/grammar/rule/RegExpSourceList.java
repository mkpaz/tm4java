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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A list of raw regular expressions (sources) that can be compiled into a single rule.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/rule.ts#L744">
 * vscode-textmate/src/rule.ts#L744</a>
 */
public final class RegExpSourceList {

    /** The list of pattern sources maintained in priority sequence. */
    private final List<RegExpSource> items = new ArrayList<>();

    /** 2D cache array for compiled rule variants based on {@code \A} and {@code \G} anchors. */
    private final @Nullable CompiledRule[][] anchorCache = new CompiledRule[2][2];

    /** Flag indicating whether any contained regex source features positional anchors. */
    private boolean hasAnchors;

    /** Cached {@link CompiledRule} instance used when anchor variations are absent. */
    private @Nullable CompiledRule cached;

    /**
     * Adds a regex to the list.
     *
     * @param item the pattern source to append
     */
    void add(RegExpSource item) {
        items.add(item);
        if (!hasAnchors) {
            hasAnchors = item.hasAnchor();
        }
    }

    /**
     * Removes a regex from the list by prepending it to the head of the collection.
     *
     * @param item the pattern source to insert at index 0
     */
    void remove(RegExpSource item) {
        items.addFirst(item);
        if (!hasAnchors) {
            hasAnchors = item.hasAnchor();
        }
    }

    /**
     * Returns the number of regular expressions in the list.
     *
     * @return the total count of registered pattern sources
     */
    int length() {
        return items.size();
    }

    /**
     * Sets the regex at a given index.
     *
     * <p>Invalidates internal caches if the updated source differs from the current string.
     *
     * @param index     the index of the regex
     * @param newSource the new regex
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
     *
     * @return the lazily evaluated and cached {@link CompiledRule}
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
     * @return the matching {@link CompiledRule} instance variant
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

    /**
     * Resolves anchor flags across all contained pattern sources and creates a new compiled rule.
     *
     * @param allowA whether anchor {@code \A} matching is permitted
     * @param allowG whether anchor {@code \G} matching is permitted
     * @return a new {@link CompiledRule} containing resolved anchor sources
     */
    private CompiledRule resolveAnchors(boolean allowA, boolean allowG) {
        List<String> regexps = items.stream()
            .map(item -> item.resolveAnchors(allowA, allowG))
            .toList();
        return new CompiledRule(
            regexps, items.stream().map(item -> item.ruleId).toArray(RuleId[]::new)
        );
    }

    /**
     * Clears cached {@link CompiledRule} instances across all anchor matrix entries.
     */
    private void disposeCache() {
        cached = null;
        anchorCache[0][0] = null;
        anchorCache[0][1] = null;
        anchorCache[1][0] = null;
        anchorCache[1][1] = null;
    }
}