/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.grammar.oniguruma;

import org.jspecify.annotations.Nullable;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.List;

/**
 * The scanner for the specified list of Oniguruma regexps.
 *
 * @see <a href="https://github.com/atom/node-oniguruma/blob/master/src/onig-scanner.cc">
 * /node-oniguruma/src/onig-scanner.cc</a>
 */
public final class OnigScanner {

    private static final Logger LOG = System.getLogger(OnigScanner.class.getName());

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
     * @param source   the source string to search
     * @param startPos the position to start the search
     * @return an {@code OnigScannerMatch} if a match is found, {@code null} otherwise
     */
    public @Nullable OnigScannerMatch findNextMatch(OnigString source, int startPos) {
        if (LOG.isLoggable(Level.TRACE)) {
            LOG.log(Level.TRACE, "[OnigScanner] findNextMatch charPos=%d (strLen=%d, bytesCount=%d) content='%s'"
                    .formatted(startPos, source.content.length(), source.bytesCount, source.content));
        }

        OnigResult bestResult = search(source, startPos);
        if (bestResult != null) {
            OnigScannerMatch match = new OnigScannerMatch(bestResult, source);
            if (LOG.isLoggable(Level.TRACE)) {
                LOG.log(Level.TRACE, "[OnigScanner] BEST MATCH found by regex #%d (pattern: %s): %s"
                        .formatted(bestResult.getIndex(), regExps.get(bestResult.getIndex()).pattern(), match));
            }
            return match;
        }

        if (LOG.isLoggable(Level.TRACE)) {
            LOG.log(Level.TRACE, "[OnigScanner] NO MATCH found");
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
        if (LOG.isLoggable(Level.TRACE)) {
            LOG.log(Level.TRACE,
                "[OnigScanner.search] charOffset=%d -> byteOffset=%d".formatted(charOffset, byteOffset)
            );
        }

        int bestLocation = Integer.MAX_VALUE;
        OnigResult bestResult = null;
        int index = 0;

        // pick the regexp whose match starts earliest in the source
        for (OnigRegExp regExp : regExps) {
            OnigResult result = regExp.searchDebug(source, byteOffset, index);
            if (result != null && result.count() > 0) {
                int location = result.locationAt(0);
                if (location >= 0 && location < bestLocation) {
                    bestLocation = location;
                    bestResult = result;
                    bestResult.setIndex(index);

                    if (LOG.isLoggable(Level.TRACE)) {
                        LOG.log(Level.TRACE, "[OnigScanner.search] regExp #%d matched at byteLoc=%d (best so far)"
                            .formatted(index, location));
                    }

                    // a match at the current offset cannot be beaten
                    if (location == byteOffset) {
                        if (LOG.isLoggable(Level.TRACE)) {
                            LOG.log(Level.TRACE, "[OnigScanner.search] match at current byteOffset, short-circuit break");
                        }
                        break;
                    }
                }
            }
            index++;
        }

        return bestResult;
    }
}
