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

public interface ITheme {

    List<String> getColorMap();

    StyleAttributes getDefaults();

    Map<String, String> getEditorColors();

    @Nullable StyleAttributes match(String scope);
}
