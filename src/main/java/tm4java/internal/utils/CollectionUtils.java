/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.utils;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

/**
 * A utility class for performing common operations on collections.
 */
public final class CollectionUtils {

    private CollectionUtils() {
        // utility class
    }

    /**
     * Finds the first element in the list that matches the given predicate.
     *
     * @param list   the list to search
     * @param filter the predicate to test elements against
     */
    public static @Nullable <T> T findFirst(List<T> list, Predicate<T> filter) {
        for (T e : list) {
            if (filter.test(e)) {
                return e;
            }
        }
        return null;
    }

    /**
     * Retrieves the element at the specified index in the list.
     *
     * @param list  a non-empty list
     * @param index the element to get; negative index counts from end of list,
     *              e.g. -1 = last element.
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public static <T> T getElementAt(List<T> list, int index) {
        if (index < 0) {
            return list.get(list.size() + index);
        }
        return list.get(index);
    }
}
