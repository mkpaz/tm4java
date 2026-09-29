/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.parser;

import org.jspecify.annotations.Nullable;

import java.io.Serial;

/**
 * An adapter interface that allows to store key-value properties.
 *
 * <p>Used during JSON/Plist parser navigation to dynamically mutate nested data structures
 * (lists or maps) using key path strings or numeric indices.
 *
 * @param <V> the type of the property value
 */
public interface PropertySettable<V> {

    /**
     * Sets a property with the given name and value.
     *
     * @param name  the name of the property
     * @param value the value of the property
     */
    void setProperty(String name, V value);

    //*************************************************************************

    /**
     * An {@link ArrayList} implementation that adapts indexed list insertions and updates
     * to the {@link PropertySettable} interface.
     *
     * @param <T> the type of elements in this list
     */
    class ArrayList<T> extends java.util.ArrayList<T> implements PropertySettable<T> {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * Sets an element at the parsed integer index, appending if the index equals current size.
         *
         * @param name  the string representation of the array index (e.g. {@code "0"})
         * @param value the element value to add or update
         * @throws NumberFormatException     if {@code name} cannot be parsed as an integer
         * @throws IndexOutOfBoundsException if the parsed index is out of bounds
         */
        @Override
        public void setProperty(String name, T value) {
            var index = Integer.parseInt(name);
            if (index == size()) {
                add(value);
            } else {
                set(index, value);
            }
        }
    }

    /**
     * A {@link HashMap} implementation that adapts string key-value mappings
     * to the {@link PropertySettable} interface.
     *
     * @param <T> the type of mapped values
     */
    class HashMap<T> extends java.util.HashMap<String, @Nullable T> implements PropertySettable<T> {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * Associates the specified value with the specified key in this map.
         *
         * @param name  the property key name
         * @param value the value to associate with the key, or {@code null}
         */
        @Override
        public void setProperty(String name, @Nullable T value) {
            put(name, value);
        }
    }
}
