/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.grammar.oniguruma;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The scanner for the specified list of Oniguruma regexps.
 *
 * @see <a href="https://github.com/atom/node-oniguruma/blob/master/src/onig-scanner.cc">
 * /node-oniguruma/src/onig-scanner.cc</a>
 */
public final class OnigScanner {

    private final List<OnigRegExp> regExps;

    /**
     * Constructs an {@code OnigScanner} with the specified regular expressions.
     *
     * @param regExps a list of regular expressions as strings
     */
    public OnigScanner(List<String> regExps) {
        this.regExps = regExps.stream()
                           .map(OnigRegExp::new)
                           .toList();
    }

    /**
     * Finds the match in the source string starting from the specified position.
     *
     * @param source        the source string to search
     * @param startPosition the position to start the search
     * @return an {@code OnigScannerMatch} if a match is found, {@code null} otherwise
     */
    public @Nullable OnigScannerMatch findNextMatch(OnigString source, int startPosition) {
        OnigResult bestResult = search(source, startPosition);
        if (bestResult != null) {
            return new OnigScannerMatch(bestResult, source);
        }
        return null;
    }

    /**
     * Searches for a match in the given source string.
     *
     * @param source     the source string
     * @param charOffset the character offset to start searching from
     * @return the search result, or null if no match is found
     */
    private @Nullable OnigResult search(OnigString source, int charOffset) {
        int byteOffset = source.getByteIndexOfChar(charOffset);

        int bestLocation = 0;
        OnigResult bestResult = null;
        int index = 0;

        for (OnigRegExp regExp : regExps) {
            OnigResult result = regExp.search(source, byteOffset);
            if (result != null && result.count() > 0) {
                int location = result.locationAt(0);

                if (bestResult == null || location < bestLocation) {
                    bestLocation = location;
                    bestResult = result;
                    bestResult.setIndex(index);
                }

                if (location == byteOffset) {
                    break;
                }
            }
            index++;
        }

        return bestResult;
    }
}
