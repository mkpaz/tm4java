/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java;

import org.jspecify.annotations.Nullable;

import java.io.Serial;

/**
 * TextMate exception.
 */
public class TMException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public TMException(@Nullable String message) {
        super(message);
    }

    public TMException(@Nullable String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
