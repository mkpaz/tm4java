/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.theme;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Represents a parsed and compiled TextMate theme, capable of matching scope sequences
 * against theme rules and resolving color palette references.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/theme.ts#L339">
 * vscode-textmate/src/theme.ts#L339</a>
 */
public interface ITheme {

    /**
     * Returns the indexed palette of color HEX strings used by this theme.
     *
     * <p>The position of a color string in this list corresponds to its internal color ID
     * used in encoded token attributes. Index {@code 0} represents default/unspecified color.
     *
     * @return an immutable list of HEX color strings where indices correspond to color IDs
     */
    List<String> getColorMap();

    /**
     * Returns the default style attributes applied when no specific theme rule matches a token scope.
     *
     * @return the default {@link StyleAttributes}
     */
    StyleAttributes getDefaults();

    /**
     * Returns the workbench editor UI color definitions associated with this theme.
     *
     * @return a map of editor UI color key-value pairs
     */
    Map<String, String> getEditorColors();

    /**
     * Resolves and matches the styling attributes for a given TextMate scope path.
     *
     * @param scope a space-separated scope chain or single scope string
     *              (e.g. {@code "source.java entity.name.function"})
     * @return the resolved {@link StyleAttributes} for the best matching theme rule,
     * or {@code null} if no rule matches
     */
    @Nullable StyleAttributes match(String scope);
}
