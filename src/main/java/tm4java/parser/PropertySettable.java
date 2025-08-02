/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.parser;

import java.io.Serial;

/**
 * An adapter interface that allows to store key-value properties.
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

    class ArrayList<T> extends java.util.ArrayList<T> implements PropertySettable<T> {

        @Serial
        private static final long serialVersionUID = 1L;

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

    class HashMap<T> extends java.util.HashMap<String, T> implements PropertySettable<T> {

        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public void setProperty(String name, T value) {
            put(name, value);
        }
    }
}
