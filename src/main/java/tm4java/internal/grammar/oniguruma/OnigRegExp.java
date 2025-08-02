/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.grammar.oniguruma;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import org.jcodings.specific.NonStrictUTF8Encoding;
import org.joni.Matcher;
import org.joni.Option;
import org.joni.Regex;
import org.joni.Region;
import org.joni.Syntax;
import org.joni.WarnCallback;
import org.joni.exception.SyntaxException;
import org.jspecify.annotations.Nullable;
import tm4java.TMException;

/**
 * Represents an Oniguruma regular expression.
 *
 * @see <a href="https://github.com/atom/node-oniguruma/blob/master/src/onig-reg-exp.cc">
 * github.com/atom/node-oniguruma/src/onig-reg-exp.cc</a>
 */
public final class OnigRegExp {

    private static final Logger LOGGER = System.getLogger(OnigRegExp.class.getName());
    private static final WarnCallback LOGGER_WARN_CALLBACK = // DEBUG, because WARNING produce too many noise
        message -> LOGGER.log(Level.DEBUG, message);

    private final String pattern;
    private final Regex regex;
    private final boolean hasGAnchor;

    private @Nullable OnigString lastSearchString;
    private int lastSearchPosition = -1;
    private @Nullable OnigResult lastSearchResult;

    /**
     * See {@link #OnigRegExp(String, boolean)}.
     */
    public OnigRegExp(String pattern) {
        this(pattern, false);
    }

    /**
     * Creates a new OnigRegExp instance with case-insensitive matching.
     *
     * @param pattern    the pattern of the regular expression
     * @param ignoreCase whether to perform case-insensitive matching
     * @throws TMException if parsing fails
     */
    public OnigRegExp(String pattern, boolean ignoreCase) {
        hasGAnchor = pattern.contains("\\G");

        Regex regex;
        try {
            regex = parsePattern(pattern, ignoreCase);
        } catch (SyntaxException e) {
            try {
                regex = parsePattern(rewritePatternIfRequired(pattern), ignoreCase);
            } catch (SyntaxException unused) {
                throw new TMException("Parsing regex pattern \"" + pattern + "\" failed with " + e, e);
            }
        }

        this.pattern = pattern;
        this.regex = regex;
    }

    /**
     * Searches for a match in the given string.
     *
     * @param str           the string to search
     * @param startPosition the position to start searching from
     * @return the search result, or null if no match is found
     */
    public @Nullable OnigResult search(OnigString str, int startPosition) {
        if (hasGAnchor) {
            // Should not use caching, because the regular expression
            // targets the current search position (\G)
            return doSearch(str.bytesUTF8(), startPosition, str.bytesCount);
        }

        var lastSearchResult0 = this.lastSearchResult;
        if (lastSearchString == str
            && lastSearchPosition <= startPosition
            && (lastSearchResult0 == null || lastSearchResult0.locationAt(0) >= startPosition)) {
            return lastSearchResult0;
        }

        lastSearchString = str;
        lastSearchPosition = startPosition;
        lastSearchResult = doSearch(str.bytesUTF8(), startPosition, str.bytesCount);
        return lastSearchResult;
    }

    /**
     * Returns the pattern of this regular expression.
     */
    public String pattern() {
        return pattern;
    }

    @Override
    public String toString() {
        return "OnigRegExp{"
            + "pattern='" + pattern + '\''
            + '}';
    }

    //*************************************************************************

    /**
     * Parses the given regex pattern into an Oniguruma Regex object.
     *
     * @param pattern    the pattern to parse
     * @param ignoreCase whether to perform case-insensitive matching
     * @throws SyntaxException if parsing fails
     */
    private Regex parsePattern(String pattern, boolean ignoreCase) throws SyntaxException {
        int options = Option.CAPTURE_GROUP;
        if (ignoreCase) {
            options |= Option.IGNORECASE;
        }

        byte[] patternBytes = pattern.getBytes(StandardCharsets.UTF_8);
        return new Regex(
            patternBytes,
            0,
            patternBytes.length,
            options,
            NonStrictUTF8Encoding.INSTANCE,
            Syntax.RUBY,
            LOGGER.isLoggable(Level.WARNING) ? LOGGER_WARN_CALLBACK : WarnCallback.NONE
        );
    }

    /**
     * Rewrites the given pattern to workaround limitations of the joni library which
     * for example does not support negative variable-length look-behinds
     *
     * @see <a href="https://github.com/eclipse-tm4e/tm4e/issues/677">github.com/eclipse-tm4e/tm4e/issue/677</a>
     */
    private String rewritePatternIfRequired(String pattern) {
        // e.g. used in csharp.tmLanguage.json
        var lookbehind1 = "(?<!\\.\\s*)";
        if (pattern.startsWith(lookbehind1)) {
            return "(?<!\\.)\\s*" + pattern.substring(lookbehind1.length());
        }

        // e.g. used in markdown.math.block.tmLanguage.json and tex.tmLanguage.json
        var lookbehind2 = "(?<=^\\s*)";
        if (pattern.startsWith(lookbehind2)) {
            return "(?<=^)\\s*" + pattern.substring(lookbehind2.length());
        }

        // e.g. used in carbon.tmLanguage.json
        var lookbehind3 = "(?<=\\s*\\.)";
        if (pattern.startsWith(lookbehind3)) {
            return "\\s*\\." + pattern.substring(lookbehind3.length());
        }

        // e.g. used in julia.tmLanguage.json
        var lookbehind4 = "(?<=\\S\\s+)";
        if (pattern.startsWith(lookbehind4)) {
            return "\\S\\s+" + pattern.substring(lookbehind4.length());
        }

        return pattern;
    }

    /**
     * Searches for a match in the given data.
     */
    private @Nullable OnigResult doSearch(byte[] data, int startPosition, int end) {
        Matcher matcher = regex.matcher(data);
        int status = matcher.search(startPosition, end, Option.DEFAULT);

        if (status != Matcher.FAILED) {
            Region region = matcher.getEagerRegion();
            return new OnigResult(region);
        }

        return null;
    }
}
