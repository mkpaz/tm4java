/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.parser;

import java.io.Reader;

/**
 * An interface for parsing input data into a hierarchical structure
 * of {@link PropertySettable} objects.
 */
public interface TMParser {

    /**
     * Parses the given source into a PropertySettable object.
     *
     * @param <T>     the type of the value
     * @param source  the source data to parse
     * @param factory the factory for creating PropertySettable objects
     * @throws Exception if parsing fails
     */
    <T extends PropertySettable<?>> T parse(Reader source, ObjectFactory<T> factory) throws Exception;
}
