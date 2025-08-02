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
import tm4java.TMException;
import tm4java.parser.TMParser;
import tm4java.parser.TMParserNanoJson;
import tm4java.parser.TMParserPList;
import tm4java.internal.utils.Resources;
import tm4java.parser.ContentType;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Defines an interface for reading and parsing TextMate themes from various sources.
 * <p>
 * See helper static methods:
 * <li>{@link #fromFile(Path)}
 * <li>{@link #fromResource(Class, String)}
 * <li>{@link #fromString(ContentType, String)}
 */
public interface IThemeSource {

    /**
     * Returns the theme source URI.
     */
    URI getURI();

    /**
     * Returns the theme source reader.
     */
    Reader getReader() throws IOException;

    /**
     * Returns the theme resource modification date or 0, if resource does not exist
     * or modification date cannot be determined.
     */
    long lastModified() throws IOException;

    /**
     * Returns the content type of the theme source.
     * The default implementation attempts to infer the content type from the URI.
     *
     * @throws TMException if the content type is unsupported or cannot be determined
     */
    default @Nullable ContentType getContentType() {
        return ContentType.getByExtension(getURI().getPath());
    }

    /**
     * Returns the parser for deserializing a theme from the source resource.
     * The default implementation can parse themes from JSON or PList formats.
     */
    default TMParser getParser() {
        var contentType = getContentType();
        if (contentType == ContentType.JSON) {
            return TMParserNanoJson.instance();
        }
        if (contentType == ContentType.XML) {
            return TMParserPList.instance();
        }

        throw new TMException("Unsupported theme content type: " + getURI());
    }

    //*************************************************************************

    /**
     * See {@link #fromFile(Path, ContentType, Charset)}.
     */
    static IThemeSource fromFile(Path file) {
        return fromFile(file, null, null);
    }

    /**
     * Creates a source for reading a theme from the specified file path.
     *
     * @param file        the path to the theme file
     * @param contentType the content type of the file, or null if unknown
     * @param charset     The character set for reading the file, defaults to UTF-8
     * @throws TMException if the content type is unsupported or cannot be determined
     */
    static IThemeSource fromFile(Path file, @Nullable ContentType contentType, @Nullable Charset charset) {
        return new IThemeSource() {
            @Override
            public URI getURI() {
                return file.toUri();
            }

            @Override
            public Reader getReader() throws IOException {
                return Files.newBufferedReader(file, charset == null ? StandardCharsets.UTF_8 : charset);
            }

            @Override
            public @Nullable ContentType getContentType() {
                return contentType != null ? contentType : ContentType.getByExtension(file);
            }

            @Override
            public long lastModified() {
                return file.toFile().lastModified();
            }
        };
    }

    /**
     * See {@link #fromResource(Class, String, ContentType, Charset)}.
     */
    static IThemeSource fromResource(Class<?> clazz, String resourceName) {
        return fromResource(clazz, resourceName, null, null);
    }

    /**
     * Creates a source for reading a theme from the specified file path.
     *
     * @param clazz        the class to be used for resource lookup
     * @param resourceName the name of the resource
     * @param contentType  the content type of the resource, or null if unknown
     * @param charset      rhe character set for reading the resource, defaults to UTF-8
     * @throws TMException if the content type is unsupported or cannot be determined
     */
    static IThemeSource fromResource(Class<?> clazz, String resourceName,
                                     @Nullable ContentType contentType,
                                     @Nullable Charset charset) {
        var uri = Resources.getResource(clazz, resourceName);

        return new IThemeSource() {
            @Override
            public URI getURI() {
                return uri;
            }

            @Override
            public Reader getReader() throws IOException {
                return Resources.getReader(clazz, resourceName, charset);
            }

            @Override
            public @Nullable ContentType getContentType() {
                return contentType != null ? contentType : ContentType.getByExtension(resourceName);
            }

            @Override
            public long lastModified() throws IOException {
                return Resources.getLastModified(clazz, resourceName);
            }
        };
    }

    /**
     * Creates a source for reading a theme from the specified string.
     *
     * @param contentType the content type of the resource, or null if unknown
     * @param content     the source string containing a theme
     * @throws TMException if the content type is unsupported or cannot be determined
     */
    static IThemeSource fromString(ContentType contentType, String content) {
        var uri = URI.create("data:"
            + contentType.getMimeType()
            + ";charset=UTF-8,"
            + URLEncoder.encode(content, StandardCharsets.UTF_8)
        );
        long modified = System.currentTimeMillis();

        return new IThemeSource() {
            @Override
            public URI getURI() {
                return uri;
            }

            @Override
            public Reader getReader() {
                return new StringReader(content);
            }

            @Override
            public ContentType getContentType() {
                return contentType;
            }

            @Override
            public long lastModified() {
                return modified;
            }
        };
    }
}
