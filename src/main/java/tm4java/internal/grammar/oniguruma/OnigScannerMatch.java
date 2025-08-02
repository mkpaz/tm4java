/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.grammar.oniguruma;

import java.util.Arrays;
import org.jspecify.annotations.Nullable;

/**
 * Represents a match result from an {@link OnigScanner}.
 *
 * @see <a href="https://github.com/atom/node-oniguruma/blob/master/src/onig-scanner.cc">
 * node-oniguruma/src/onig-scanner.cc</a>
 */
public final class OnigScannerMatch {

    private final OnigCaptureIndex[] captureIndices;

    /**
     * The index of the match in the source string.
     */
    public final int index;

    /**
     * Creates a new OnigScannerMatch instance.
     *
     * @param result the OnigResult object containing the match information
     * @param source the OnigString object representing the source string
     */
    OnigScannerMatch(OnigResult result, OnigString source) {
        this.index = result.getIndex();
        this.captureIndices = captureIndicesOfMatch(result, source);
    }

    /**
     * Returns the capture indices of the match.
     */
    public OnigCaptureIndex[] getCaptureIndices() {
        return captureIndices;
    }

    @Override
    public int hashCode() {
        return 31 * (31 + index) + Arrays.hashCode(captureIndices);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof OnigScannerMatch other) {
            return index == other.index && Arrays.equals(captureIndices, other.captureIndices);
        }
        return false;
    }

    @Override
    public String toString() {
        var result = new StringBuilder("{\n")
                         .append("  \"index\": ")
                         .append(index)
                         .append(",\n")
                         .append("  \"captureIndices\": [\n");

        int i = 0;
        for (OnigCaptureIndex captureIndex : captureIndices) {
            if (i > 0) {
                result.append(",\n");
            }
            result.append("    ");
            result.append(captureIndex);
            i++;
        }

        result.append("\n")
            .append("  ]\n")
            .append("}");

        return result.toString();
    }

    /**
     * Extracts the capture indices from the given OnigResult and OnigString.
     *
     * @param result the OnigResult object containing the match information
     * @param source the OnigString object representing the source string
     * @return an array of OnigCaptureIndex objects representing the capture indices
     */
    private OnigCaptureIndex[] captureIndicesOfMatch(OnigResult result, OnigString source) {
        int resultCount = result.count();
        var captures = new OnigCaptureIndex[resultCount];

        for (int i = 0; i < resultCount; i++) {
            int loc = result.locationAt(i);
            int captureStart = source.getCharIndexOfByte(loc);
            int captureEnd = source.getCharIndexOfByte(loc + result.lengthAt(i));
            captures[i] = captureStart == 0 && captureEnd == 0
                              ? OnigCaptureIndex.EMPTY
                              : new OnigCaptureIndex(captureStart, captureEnd);
        }

        return captures;
    }
}
