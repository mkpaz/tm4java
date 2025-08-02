/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.utils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A utility class for handling null safety in a more controlled manner.
 */
public final class NullSafetyHelper {

    private NullSafetyHelper() {
        // utility class
    }

    /**
     * Casts a non-null value marked as {@link Nullable} to {@link NonNull}.
     * <p>
     * Only use if you are sure the value is non-null but annotation-based null
     * analysis was not able to determine it.
     *
     * @throws IllegalStateException if the given value is null
     */
    public static <T> @NonNull T castNonNull(@Nullable T value) {
        if (value == null) {
            throw new IllegalStateException("Unexpected null value present.");
        }
        return value;
    }

    /**
     * Returns the given object if it is non-null; otherwise, returns the default value.
     * <p>
     * Unlike {@code Objects.requireNonNullElse()} this does allow the default
     * value to return null.
     */
    public static <T> @Nullable T defaultIfNull(@Nullable T obj,
                                                @Nullable T defaultValue) {
        if (obj == null) {
            return defaultValue;
        }
        return obj;
    }
}
