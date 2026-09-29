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
 *
 * <p>Implementations of this interface (such as JSON or Plist parsers) read text
 * streams and construct nested object/array trees using the provided {@link ObjectFactory}.
 *
 * @see PropertySettable
 * @see ObjectFactory
 */
public interface TMParser {

    /**
     * Parses the given source into a PropertySettable object.
     *
     * @param <T>     the root type of the resulting {@link PropertySettable} structure
     * @param source  the {@link Reader} stream providing the raw input content (e.g. JSON or Plist XML)
     * @param factory the {@link ObjectFactory} responsible for instantiating root and child nodes
     * @return the populated root {@code PropertySettable} instance
     * @throws Exception if an I/O error occurs or the input format is invalid
     */
    <T extends PropertySettable<?>> T parse(Reader source, ObjectFactory<T> factory) throws Exception;
}
