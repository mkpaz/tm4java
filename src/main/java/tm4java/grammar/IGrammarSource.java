/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.grammar;

import org.jspecify.annotations.Nullable;
import tm4java.TMException;
import tm4java.internal.utils.Resources;
import tm4java.parser.ContentType;
import tm4java.parser.TMParser;
import tm4java.parser.TMParserNanoJson;
import tm4java.parser.TMParserPList;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Defines an abstraction for reading and parsing TextMate grammars from various sources
 * (such as files, classpath resources, or in-memory strings).
 *
 * <p>Convenience static factory methods:
 * <ul>
 * <li>{@link #fromFile(Path)}</li>
 * <li>{@link #fromResource(Class, String)}</li>
 * <li>{@link #fromString(ContentType, String)}</li>
 * </ul>
 */
public interface IGrammarSource {

    /**
     * Returns the unique URI identifying this grammar source.
     *
     * @return the URI location of the grammar
     */
    URI getURI();

    /**
     * Creates and returns a new {@link Reader} to access the raw grammar content.
     *
     * <p>Callers are responsible for closing the returned reader.
     *
     * @return a reader for the grammar content
     * @throws IOException if an I/O error occurs while opening the stream
     */
    Reader getReader() throws IOException;

    /**
     * Returns the last modified time of the grammar source resource.
     *
     * @return the last modification timestamp in milliseconds since Unix epoch,
     * or {@code 0} if the resource does not exist or the timestamp cannot be determined
     */
    long getLastModified();

    /**
     * Returns the content type (e.g., JSON or XML/Plist) of the grammar source.
     *
     * <p>The default implementation attempts to infer the content type from the URI path extension.
     *
     * @return the detected {@link ContentType}, or {@code null} if it cannot be inferred from the URI
     * @throws TMException if the content type is unsupported
     */
    default @Nullable ContentType getContentType() {
        return ContentType.getByExtension(getURI().getPath());
    }

    /**
     * Returns a parser instance capable of deserializing the grammar from this source.
     *
     * <p>The default implementation supports parsing JSON and XML (Plist) grammars based on
     * {@link #getContentType()}.
     *
     * @return the {@link TMParser} suited for this grammar source
     * @throws TMException if the content type is unknown or unsupported
     */
    default TMParser getParser() {
        var contentType = getContentType();
        if (contentType == ContentType.JSON) {
            return TMParserNanoJson.instance();
        }
        if (contentType == ContentType.XML) {
            return TMParserPList.instance();
        }

        throw new TMException("Unsupported grammar content type: " + getURI());
    }

    //*************************************************************************

    /**
     * Creates a grammar source from the specified file path using default UTF-8 encoding
     * and auto-detecting content type.
     *
     * @param file the path to the grammar file
     * @return a new {@link IGrammarSource} instance backing the given file
     * @see #fromFile(Path, ContentType, Charset)
     */
    static IGrammarSource fromFile(Path file) {
        return fromFile(file, null, null);
    }

    /**
     * Creates a grammar source for reading a TextMate grammar from a file.
     *
     * @param file        the path to the grammar file
     * @param contentType the explicit content type of the file, or {@code null} to auto-detect by file extension
     * @param charset     the character set used to read the file, or {@code null} for UTF-8
     * @return a new {@link IGrammarSource} instance backing the given file
     */
    static IGrammarSource fromFile(Path file,
                                   @Nullable ContentType contentType,
                                   @Nullable Charset charset) {
        return new IGrammarSource() {
            @Override
            public URI getURI() {
                return file.toUri();
            }

            @Override
            public Reader getReader() throws IOException {
                return Files.newBufferedReader(file, Objects.requireNonNullElse(charset, StandardCharsets.UTF_8));
            }

            @Override
            public @Nullable ContentType getContentType() {
                return contentType != null ? contentType : ContentType.getByExtension(file);
            }

            @Override
            public long getLastModified() {
                return file.toFile().lastModified();
            }
        };
    }

    /**
     * Creates a grammar source from a Java classpath resource using default UTF-8 encoding
     * and auto-detecting content type.
     *
     * @param anchor       the class to be used for resource resolution
     * @param resourceName the name/path of the classpath resource
     * @return a new {@link IGrammarSource} instance backing the classpath resource
     * @see #fromResource(Class, String, ContentType, Charset)
     */
    static IGrammarSource fromResource(Class<?> anchor, String resourceName) {
        return fromResource(anchor, resourceName, null, null);
    }

    /**
     * Creates a grammar source for reading a TextMate grammar from a Java classpath resource.
     *
     * @param anchor       the class to be used for resource resolution
     * @param resourceName the name/path of the classpath resource
     * @param contentType  the explicit content type of the resource, or {@code null} to auto-detect
     * @param charset      the character set used to read the resource, or {@code null} for UTF-8
     * @return a new {@link IGrammarSource} instance backing the classpath resource
     */
    static IGrammarSource fromResource(Class<?> anchor,
                                       String resourceName,
                                       @Nullable ContentType contentType,
                                       @Nullable Charset charset) {
        var uri = Resources.getResource(anchor, resourceName);

        return new IGrammarSource() {
            @Override
            public URI getURI() {
                return uri;
            }

            @Override
            public Reader getReader() throws IOException {
                return Resources.getReader(anchor, resourceName, charset);
            }

            @Override
            public @Nullable ContentType getContentType() {
                return contentType != null ? contentType : ContentType.getByExtension(resourceName);
            }

            @Override
            public long getLastModified() {
                try {
                    return Resources.getLastModified(anchor, resourceName);
                } catch (IOException e) {
                    return 0;
                }
            }
        };
    }

    /**
     * Creates an in-memory grammar source from an unparsed string payload.
     *
     * @param contentType the explicit content type (e.g. JSON or XML) of the raw string
     * @param content     the raw string content of the grammar
     * @return a new {@link IGrammarSource} instance wrapping the string
     */
    static IGrammarSource fromString(ContentType contentType, String content) {
        var uri = URI.create("data:"
            + contentType.getMimeType()
            + ";charset=UTF-8,"
            + URLEncoder.encode(content, StandardCharsets.UTF_8)
        );
        long modified = System.currentTimeMillis();

        return new IGrammarSource() {
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
            public long getLastModified() {
                return modified;
            }
        };
    }
}
