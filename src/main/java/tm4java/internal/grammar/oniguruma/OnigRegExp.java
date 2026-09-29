package tm4java.internal.grammar.oniguruma;

import org.jspecify.annotations.Nullable;
import tm4java.TMException;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.ref.Cleaner;
import java.lang.ref.Reference;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Represents an Oniguruma regex.
 *
 * @see <a href="https://github.com/atom/node-oniguruma/blob/master/src/onig-reg-exp.cc">
 * github.com/atom/node-oniguruma/src/onig-reg-exp.cc</a>
 */
public final class OnigRegExp implements AutoCloseable {

    private record Cache(OnigString src,
                         int start,
                         int @Nullable [] beg,
                         int @Nullable [] end) { }

    private static final Logger LOG = System.getLogger(OnigRegExp.class.getName());
    private static final Cleaner CLEANER = Cleaner.create();

    private final String pattern;
    private final MemorySegment regexPtr;

    private final Cleaner.@Nullable Cleanable cleanable;
    private volatile boolean closed;

    private final boolean cacheable; // \G is bound to the start position, caching is not allowed for such patterns
    private volatile @Nullable Cache cache;

    /**
     * See {@link #OnigRegExp(String, boolean)}.
     */
    public OnigRegExp(String pattern) {
        this(pattern, false);
    }

    /**
     * Creates a new OnigRegExp instance with case-insensitive matching.
     *
     * @param pattern    the pattern of the regex
     * @param ignoreCase whether to perform case-insensitive matching
     * @throws TMException if parsing fails
     */
    public OnigRegExp(String pattern, boolean ignoreCase) {
        this.pattern = pattern;
        this.cacheable = !pattern.contains("\\G");

        MemorySegment parsedRegex;
        try {
            parsedRegex = parsePattern(pattern, ignoreCase);
        } catch (Exception e) {
            // log a warning but do NOT throw an exception
            if (LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, "Skipping invalid regex pattern '%s': %s"
                    .formatted(pattern, e.getMessage()));
            }
            parsedRegex = MemorySegment.NULL;
        }

        this.regexPtr = parsedRegex;

        // register a Cleaner action to free the native regex once this instance becomes unreachable,
        // a NULL pointer means parsing failed, so there is nothing to free
        if (parsedRegex.address() != 0) {
            MemorySegment freePtr = parsedRegex;
            this.cleanable = CLEANER.register(this, () -> {
                try {
                    Oniguruma.ONIG_FREE.invokeExact(freePtr);
                } catch (Throwable _) {
                    // cleanup actions must never throw
                }
            });
        } else {
            this.cleanable = null;
        }
    }

    /**
     * Searches for a match in the given string.
     *
     * @param str           the string to search
     * @param startPosition the position to start searching from
     * @return the search result, or {@code null} if no match is found
     */
    public @Nullable OnigResult search(OnigString str, int startPosition) {
        return doSearch(str, startPosition, Integer.MAX_VALUE, -1);
    }

    /**
     * Searches for a match in the given string, logging debug information.
     *
     * @param str           the string to search
     * @param startPosition the position to start searching from
     * @param regExpIndex   the index of this regexp in the scanner, for debug output
     * @return the search result, or null if no match is found
     */
    public @Nullable OnigResult searchDebug(OnigString str, int startPosition, int regExpIndex) {
        return doSearch(str, startPosition, Integer.MAX_VALUE, regExpIndex);
    }

    /**
     * Returns the pattern of this regex.
     */
    public String pattern() {
        return pattern;
    }

    /**
     * Releases the native resources held by this expression.
     *
     * <p>Must NOT be called concurrently with a search on the same instance.
     */
    @Override
    public void close() {
        closed = true;
        if (cleanable != null) {
            cleanable.clean();
        }
    }

    @Override
    public String toString() {
        return "OnigRegExp{" + "pattern='" + pattern + '\'' + '}';
    }

    /**
     * Parses the given regex pattern into an Oniguruma Regex object.
     *
     * @param patternStr the pattern to parse
     * @param ignoreCase whether to perform case-insensitive matching
     * @return the pointer to the parsed regex, or {@code MemorySegment.NULL} on failure
     */
    private MemorySegment parsePattern(String patternStr, boolean ignoreCase) {
        byte[] patternBytes = patternStr.getBytes(StandardCharsets.UTF_8);

        try (var arena = Arena.ofConfined()) {
            MemorySegment patternSeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, patternBytes);
            MemorySegment patternEndSeg = patternSeg.asSlice(patternBytes.length);
            MemorySegment regexOutSeg = arena.allocate(ValueLayout.ADDRESS);

            int options = Oniguruma.ONIG_OPTION_CAPTURE_GROUP;
            if (ignoreCase) {
                options |= Oniguruma.ONIG_OPTION_IGNORE_CASE;
            }

            int status;
            try {
                status = (int) Oniguruma.ONIG_NEW.invokeExact(
                    regexOutSeg,
                    patternSeg,
                    patternEndSeg,
                    options,
                    Oniguruma.ENC_UTF8,
                    Oniguruma.SYNTAX_ONIGURUMA,
                    MemorySegment.NULL
                );
            } catch (Throwable e) {
                throw new IllegalStateException(
                    "onig_new invocation failed for pattern '%s'".formatted(patternStr), e
                );
            }

            if (status != 0) {
                String errorMsg = Oniguruma.getErrorMessage(status);
                throw new TMException(
                    "Failed to compile pattern '%s': %s (code %d)".formatted(patternStr, errorMsg, status)
                );
            }

            return regexOutSeg.get(ValueLayout.ADDRESS, 0);
        }
    }

    /**
     * Searches for a match in the given data.
     *
     * @param source     the source string
     * @param startPos   the start byte position
     * @param limitPos   the end byte position (exclusive)
     * @param regexIndex the regexp index for debug output, or -1 to disable debug logging
     * @return the search result, or null if no match is found
     */
    private @Nullable OnigResult doSearch(OnigString source, int startPos, int limitPos, int regexIndex) {
        // closed by the user, or pattern failed to compile — nothing to search with
        if (closed || regexPtr.address() == 0) {
            return null;
        }

        // start outside the string, or empty/invalid [start, limit] range
        if (startPos < 0 || startPos > source.bytesCount || limitPos <= startPos) {
            return null;
        }

        // reuse a previous match if it still covers the current start position
        if (cacheable) {
            Cache c = cache;
            if (c != null && c.src() == source && startPos >= c.start()) {
                if (c.beg() == null) {
                    return null; // no match was found from an earlier position
                }
                if (c.beg()[0] >= startPos) {
                    // beg and end are always set together, so end is non-null here
                    return c.beg()[0] >= limitPos
                        ? null
                        : new OnigResult(c.beg(), Objects.requireNonNull(c.end()));
                }
            }
        }

        MemorySegment strSeg = source.nativeSegment();
        MemorySegment strEndSeg = source.nativeEnd();
        MemorySegment startSeg = strSeg.asSlice(startPos);
        MemorySegment rangeSeg = limitPos >= source.bytesCount ? strEndSeg : strSeg.asSlice(limitPos);
        MemorySegment regionSeg = Oniguruma.acquireRegion();

        try {
            int status = (int) Oniguruma.ONIG_SEARCH.invokeExact(
                regexPtr,
                strSeg,
                strEndSeg,
                startSeg,
                rangeSeg,
                regionSeg,
                Oniguruma.ONIG_OPTION_NONE
            );

            if (status >= 0) {
                var result = new OnigResult(regionSeg);
                if (cacheable) {
                    cache = new Cache(source, startPos, result.begArray(), result.endArray());
                }

                // match starts at or after the limit — out of the allowed range, reject it
                if (status >= limitPos) {
                    return null;
                }

                // regexIndex < 0 means debug logging is disabled by the caller
                if (regexIndex >= 0 && LOG.isLoggable(Level.DEBUG)) {
                    LOG.log(Level.DEBUG, "[OnigRegExp #%d] Matched! status=%d, pattern='%s', beg=%s, end=%s"
                        .formatted(regexIndex, status, pattern, result.begAsString(), result.endAsString()));
                }
                return result;
            }

            // status < 0 but not a plain "no match" — a real Oniguruma error occurred
            if (status != Oniguruma.ONIG_MISMATCH && LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, "onig_search failed for '%s': %s"
                    .formatted(pattern, Oniguruma.getErrorMessage(status)));
            }

            // remember the miss only if the search covered the whole string
            if (status == Oniguruma.ONIG_MISMATCH && cacheable && limitPos >= source.bytesCount) {
                cache = new Cache(source, startPos, null, null);
            }
            return null;

        } catch (Throwable e) {
            throw new IllegalStateException(
                "Error during Oniguruma search for pattern '%s'".formatted(pattern), e
            );
        } finally {
            Oniguruma.releaseRegion(regionSeg);
            // prevent the Cleaner from freeing the regex during the search
            Reference.reachabilityFence(this);
        }
    }
}