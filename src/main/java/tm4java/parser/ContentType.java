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

/**
 * Enumeration representing supported content types and MIME types for TextMate files
 * (grammars and themes).
 *
 * <p>Provides helper utilities to detect the format based on file extensions.
 */
public enum ContentType {

    /** JSON format. */
    JSON("application/json"),

    /** XML / Apple Property List (Plist) format. */
    XML("application/xml"),

    /** YAML format. */
    YAML("application/x-yaml");

    /** The standard MIME type string associated with this content type. */
    private final String mimeType;

    /**
     * Constructs a {@code ContentType} enum instance with its associated MIME type.
     *
     * @param mimeType the standard MIME type string
     */
    ContentType(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * Returns the MIME type associated with this content type.
     *
     * @return the MIME type string (e.g. {@code "application/json"})
     */
    public String getMimeType() {
        return mimeType;
    }

    //*************************************************************************

    /**
     * Resolves the {@link ContentType} based on the file extension extracted from a {@link Path}.
     *
     * @param file the path of the file to inspect
     * @return the resolved {@link ContentType}, or {@code null} if the extension is unknown
     */
    public static @Nullable ContentType getByExtension(Path file) {
        return getByExtension(file.toString());
    }

    /**
     * Resolves the {@link ContentType} based on the file extension extracted from a file name or path string.
     *
     * <p>Supports standard extensions as well as TextMate-specific extensions:
     * <ul>
     * <li><b>JSON:</b> {@code json}</li>
     * <li><b>XML/Plist:</b> {@code plist}, {@code tmlanguage}, {@code tmtheme}, {@code xml}</li>
     * <li><b>YAML:</b> {@code yaml}, {@code yml}, {@code yaml-tmlanguage}, {@code yaml-tmtheme}</li>
     * </ul>
     *
     * @param fileName the name or path string of the file to inspect
     * @return the resolved {@link ContentType}, or {@code null} if the extension is unknown
     */
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