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

/**
 * Represents the index of a capture in a string.
 */
public record OnigCaptureIndex(int start, int end) {

    /**
     * An empty capture index.
     */
    static final OnigCaptureIndex EMPTY = new OnigCaptureIndex(0, 0);

    /**
     * Creates a new OnigCaptureIndex instance.
     *
     * @param start the start index of the capture
     * @param end   the end index of the capture
     */
    public OnigCaptureIndex(int start, int end) {
        this.start = Math.max(start, 0);
        this.end = Math.max(end, 0);
    }

    /**
     * Returns the length of the capture.
     */
    public int getLength() {
        return end - start;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof OnigCaptureIndex(int start_, int end_)) {
            return end == end_ && start == start_;
        }
        return false;
    }

    @Override
    public String toString() {
        return "OnigCaptureIndex{"
            + "start=" + start
            + ", end=" + end
            + ", length=" + getLength()
            + '}';
    }
}
