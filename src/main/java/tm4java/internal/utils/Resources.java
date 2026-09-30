/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.utils;

import org.jspecify.annotations.Nullable;
import tm4java.TMException;

import java.io.*;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.CodeSource;

/**
 * Utility class for accessing resources.
 */
public final class Resources {

    private Resources() {
        // utility class
    }

    /**
     * Returns a reader for the specified resource.
     *
     * @param anchor        the class to be used for resource lookup
     * @param resourceName the name of the resource
     * @return a {@link BufferedReader} for reading the resource
     * @throws FileNotFoundException if the resource is not found on the classpath
     */
    public static BufferedReader getReader(Class<?> anchor, String resourceName) throws FileNotFoundException {
        return getReader(anchor, resourceName, null);
    }

    /**
     * Retrieves a reader for the specified resource with an optional charset.
     *
     * @param anchor        the class to be used for resource lookup
     * @param resourceName the name of the resource
     * @param charset      the charset to use, or null for default (UTF-8)
     * @return a {@link BufferedReader} for reading the resource
     * @throws FileNotFoundException if the resource is not found on the classpath
     */
    public static BufferedReader getReader(Class<?> anchor,
                                           String resourceName,
                                           @Nullable Charset charset) throws FileNotFoundException {
        InputStream is = anchor.getResourceAsStream(resourceName);
        if (is == null) {
            throw new FileNotFoundException("Resource not found: " + resourceName);
        }
        return new BufferedReader(new InputStreamReader(is, charset == null ? StandardCharsets.UTF_8 : charset));
    }

    /**
     * Returns the last modified time (ms since epoch) for a classpath resource.
     *
     * @param anchor        the class to be used for resource lookup
     * @param resourceName the name of the resource
     * @return the last modified time in milliseconds since epoch
     * @throws IOException           on I/O errors
     * @throws FileNotFoundException if the resource is not found on the classpath
     */
    public static long getLastModified(Class<?> anchor, String resourceName) throws IOException {
        URL url = anchor.getResource(resourceName);
        if (url == null) {
            throw new FileNotFoundException("Resource not found: " + resourceName);
        }

        switch (url.getProtocol()) {
            case "file":
                try {
                    return new File(url.toURI()).lastModified();
                } catch (URISyntaxException e) {
                    return new File(url.getFile()).lastModified();
                }
            case "jar":
                var urlConnection = (JarURLConnection) url.openConnection();
                var entry = urlConnection.getJarEntry();
                if (entry != null) {
                    long time = entry.getTime();
                    return time > 0 ? time : 0L; // normalize "unknown" (-1) to 0
                }
                return urlConnection.getLastModified();
            default:
                return url.openConnection().getLastModified();
        }
    }

    /**
     * Returns a URI for the specified classpath resource.
     *
     * <p>If the resource exists, its URI is returned. Otherwise, a fallback URI is created
     * based on the class's code-source location.
     *
     * @param anchor        the class to be used for resource lookup
     * @param resourceName the name of the resource
     * @return the resolved {@link URI} for the resource
     * @throws IllegalArgumentException if the code source or its location cannot be determined
     */
    public static URI getResource(Class<?> anchor, String resourceName) {
        URL url = anchor.getResource(resourceName);
        if (url != null) {
            return URI.create(url.toString());
        }

        CodeSource codeSource = anchor.getProtectionDomain().getCodeSource();
        if (codeSource == null) {
            throw new IllegalArgumentException("Cannot determine code source for class: " + anchor.getName());
        }

        URL codeSourceLocation = codeSource.getLocation();
        if (codeSourceLocation == null) {
            throw new IllegalArgumentException("Cannot determine code-source URL for class: " + anchor.getName());
        }

        // normalize the resourceName (strip leading slash, if present)
        resourceName = resourceName.startsWith("/")
            ? resourceName.substring(1)
            : resourceName;

        String externalUrl = codeSourceLocation.toExternalForm();

        return switch (codeSourceLocation.getProtocol()) {
            case "file": {
                if (externalUrl.endsWith(".jar")) {
                    // build "jar:file:/.../myapp.jar!/normalized"
                    yield URI.create(normalizeUrl(externalUrl, "!/") + resourceName);
                }
                // build "file:" URI pointing to <location>/<normalized>
                yield URI.create(normalizeUrl(externalUrl, "/") + resourceName);
            }
            case "jar": {
                // build: "jar:file:/path/to/myapp.jar!/normalized"
                yield URI.create(normalizeUrl(externalUrl, "!/") + resourceName);
            }
            default: {
                yield URI.create(normalizeUrl(externalUrl, "/") + resourceName);
            }
        };
    }

    private static String normalizeUrl(String url, String tail) {
        return url.endsWith(tail) ? url : url + tail;
    }
}