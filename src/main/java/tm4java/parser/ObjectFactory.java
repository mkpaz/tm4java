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
 * A factory for creating PropertySettable objects.
 */
public interface ObjectFactory<T extends PropertySettable<?>> {

    /**
     * Creates the root PropertySettable object.
     */
    T createRoot();

    /**
     * Creates a child PropertySettable object.
     *
     * @param path       the path of the child
     * @param sourceType the type of the source data; {@link Map} or {@link List}
     */
    PropertySettable<?> createChild(PropertyPath path, Class<?> sourceType);
}
