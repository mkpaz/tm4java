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
 * A utility class for performing operations on collections.
 */
public final class Iterables {

    private Iterables() {
        // utility class
    }

    /**
     * Finds the first element in the list that matches the given predicate.
     *
     * @param <T>    the type of elements in the list
     * @param list   the list to search
     * @param filter the predicate to test elements against
     * @return the first matching element, or {@code null} if no element matches
     */
    public static <T> @Nullable T findFirst(List<T> list, Predicate<T> filter) {
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
     * @param <T>   the type of elements in the list
     * @param list  the list from which to retrieve the element
     * @param index the index of the element to get; negative index counts from end of list,
     *              e.g. -1 = last element
     * @return the element at the specified index
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   ({@code index >= list.size()} or {@code index < -list.size()})
     */
    public static <T> T getElementAt(List<T> list, int index) {
        if (index < 0) {
            return list.get(list.size() + index);
        }
        return list.get(index);
    }
}