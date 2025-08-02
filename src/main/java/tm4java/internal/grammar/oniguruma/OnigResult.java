/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.grammar.oniguruma;

import org.joni.Region;

/**
 * Represents the result of an Oniguruma regular expression match.
 *
 * @see <a href="https://github.com/atom/node-oniguruma/blob/master/src/onig-result.cc">
 * node-oniguruma/src/onig-result.cc</a>
 */
public final class OnigResult {

    // the source Oniguruma search result region
    private final Region region;

    // the index of this search result in the scanner used to track the best match
    private int indexInScanner = -1;

    /**
     * Creates a new OnigResult instance.
     *
     * @param region the region of the match
     */
    OnigResult(Region region) {
        this.region = region;
    }

    /**
     * Returns the index of the match, for scanner.
     */
    public int getIndex() {
        return indexInScanner;
    }

    /**
     * Sets the index of the match, for scanner.
     */
    void setIndex(int index) {
        indexInScanner = index;
    }

    /**
     * Returns the start location of the specified capture group.
     *
     * @param index the index of the capture group
     */
    public int locationAt(int index) {
        int bytes = region.getBeg(index);
        return Math.max(bytes, 0);
    }

    /**
     * Returns the length of the specified capture group.
     *
     * @param index the index of the capture group
     */
    public int lengthAt(int index) {
        int bytes = region.getEnd(index) - region.getBeg(index);
        return Math.max(bytes, 0);
    }

    /**
     * Returns the number of capture groups.
     */
    public int count() {
        return region.getNumRegs();
    }

    @Override
    public String toString() {
        return "OnigResult [indexInScanner=" + indexInScanner + ", region=" + region + "]";
    }
}
