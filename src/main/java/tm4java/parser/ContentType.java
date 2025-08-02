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

import java.nio.file.Path;

public enum ContentType {

    JSON("application/json"),
    XML("application/xml"),
    YAML("application/x-yaml");

    private final String mimeType;

    ContentType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getMimeType() {
        return mimeType;
    }

    //*************************************************************************

    public static @Nullable ContentType getByExtension(Path file) {
        return getByExtension(file.toString());
    }

    public static @Nullable ContentType getByExtension(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1)
            .trim()
            .toLowerCase();

        return switch (extension) {
            case "json" -> ContentType.JSON;
            case "plist", "tmlanguage", "tmtheme", "xml" -> ContentType.XML;
            case "yaml", "yml", "yaml-tmlanguage", "yaml-tmtheme" -> ContentType.YAML;
            default -> null;
        };
    }
}
