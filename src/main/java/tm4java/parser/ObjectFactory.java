/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.parser;

import java.util.List;
import java.util.Map;

/**
 * A factory for creating {@link PropertySettable} objects during JSON or Plist parsing.
 *
 * @param <T> the type of the root {@link PropertySettable} object created by this factory
 * @see PropertySettable
 * @see PropertyPath
 */
public interface ObjectFactory<T extends PropertySettable<?>> {

    /**
     * Creates the root {@link PropertySettable} object to populate during parsing.
     *
     * @return the root {@code PropertySettable} instance
     */
    T createRoot();

    /**
     * Creates a child {@link PropertySettable} object for a nested data structure.
     *
     * @param path       the hierarchical path of the child property being instantiated
     * @param sourceType the source class type of the nested structure; typically {@link Map} for JSON objects
     *                   or {@link List} for JSON arrays/lists
     * @return a new {@code PropertySettable} instance capable of holding properties of the child element
     */
    PropertySettable<?> createChild(PropertyPath path, Class<?> sourceType);
}