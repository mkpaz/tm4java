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
 * Unchecked exception thrown to indicate errors during TextMate grammar parsing,
 * tokenization, or registry operations.
 */
public class TMException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code TMException} with the specified detail message.
     *
     * @param message the detail message explaining the cause of the exception
     */
    public TMException(@Nullable String message) {
        super(message);
    }

    /**
     * Constructs a new {@code TMException} with the specified detail message and cause.
     *
     * @param message the detail message explaining the cause of the exception
     * @param cause   the underlying cause of the exception
     */
    public TMException(@Nullable String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
