package tm4java;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class Resources {

    public Resources() {
        // utility class
    }

    public static Path getDirectory(String path) {
        try {
            return Path.of(Objects.requireNonNull(Resources.class.getResource(path)).toURI());
        } catch (Exception e) {
            throw new TMException(e.getMessage(), e);
        }
    }

    public static Path getFile(String path) {
        try {
            return Paths.get(Objects.requireNonNull(Resources.class.getResource(path)).toURI());
        } catch (Exception e) {
            throw new TMException(e.getMessage(), e);
        }
    }

    public static BufferedReader getReader(String resourceName) throws FileNotFoundException {
        return getReader(Resources.class, resourceName, null);
    }

    public static BufferedReader getReader(Class<?> clazz,
                                           String resourceName,
                                           @Nullable Charset charset) throws FileNotFoundException {
        InputStream is = clazz.getResourceAsStream(resourceName);
        if (is == null) {
            throw new FileNotFoundException("Resource not found: " + resourceName);
        }
        return new BufferedReader(new InputStreamReader(is, charset == null ? StandardCharsets.UTF_8 : charset));
    }
}
