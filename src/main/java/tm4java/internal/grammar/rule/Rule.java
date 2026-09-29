/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.grammar.rule;


import org.jspecify.annotations.Nullable;
import tm4java.internal.grammar.oniguruma.OnigCaptureIndex;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An abstract base class for TextMate rules.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/rule.ts#L43">
 * vscode-textmate/src/rule.ts#L43</a>
 */
public abstract class Rule {

    /** Regex pattern used to detect and match dynamic capture references in scope names. */
    @SuppressWarnings("RegExpRedundantEscape")
    private static final Pattern CAPTURING_REGEX_SOURCE =
        Pattern.compile("\\$(\\d+)|\\$\\{(\\d+):\\/(downcase|upcase)}");

    /** Unique rule identifier within the grammar rule registry. */
    protected final RuleId id;

    /** Base scope name for matches of this rule, or {@code null} if untagged. */
    private final @Nullable String name;

    /** Flag indicating whether {@link #name} contains dynamic capture variables. */
    private final boolean nameIsCapturing;

    /** Scope name applied exclusively to inner content, or {@code null}. */
    private final @Nullable String contentName;

    /** Flag indicating whether {@link #contentName} contains dynamic capture variables. */
    private final boolean contentNameIsCapturing;

    /**
     * Constructs a new {@link Rule} base instance.
     *
     * @param id          the internal rule identifier
     * @param name        the scope name for matches, or {@code null}
     * @param contentName the scope name for inner content, or {@code null}
     */
    Rule(RuleId id, @Nullable String name, @Nullable String contentName) {
        this.id = id;
        this.name = name;
        this.nameIsCapturing = hasCaptures(name);
        this.contentName = contentName;
        this.contentNameIsCapturing = hasCaptures(contentName);
    }

    /**
     * Returns the resolved scope name for this rule, evaluating dynamic capture variables if present.
     *
     * @param lineText       the matched line sequence, or {@code null}
     * @param captureIndices the capture group offsets from the match result, or {@code null}
     * @return the resolved scope name string, or {@code null}
     */
    public @Nullable String getName(@Nullable CharSequence lineText, OnigCaptureIndex @Nullable [] captureIndices) {
        var name = this.name;
        if (!nameIsCapturing || name == null || lineText == null || captureIndices == null) {
            return name;
        }
        return replaceCaptures(name, lineText, captureIndices);
    }

    /**
     * Returns the resolved inner content scope name, evaluating dynamic capture variables if present.
     *
     * @param lineText       the matched line sequence
     * @param captureIndices the capture group offsets from the match result
     * @return the resolved content scope name string, or {@code null}
     */
    public @Nullable String getContentName(CharSequence lineText, OnigCaptureIndex[] captureIndices) {
        var contentName = this.contentName;
        if (!contentNameIsCapturing || contentName == null) {
            return contentName;
        }
        return replaceCaptures(contentName, lineText, captureIndices);
    }

    /**
     * Collects all regex sources associated with this rule into the target list.
     *
     * @param grammar the rule registry for resolving references
     * @param out     the target pattern list receiving compiled sources
     */
    public abstract void collectPatterns(IRuleRegistry grammar, RegExpSourceList out);

    /**
     * Compiles this rule into a executable regex matching rule.
     *
     * @param grammar        the rule registry for resolving child rules
     * @param endRegexSource resolved regex string for dynamic end pattern, or {@code null}
     * @return the compiled regex matching rule
     */
    public abstract CompiledRule compile(IRuleRegistry grammar, @Nullable String endRegexSource);

    /**
     * Compiles this rule with specific anchor matching flags ({@code \A} and {@code \G}).
     *
     * @param grammar        the rule registry for resolving child rules
     * @param endRegexSource resolved regex string for dynamic end pattern, or {@code null}
     * @param allowA         whether to permit anchor {@code \A} matching
     * @param allowG         whether to permit anchor {@code \G} matching
     * @return the compiled regex matching rule with anchor options
     */
    public abstract CompiledRule compileAG(IRuleRegistry grammar,
                                           @Nullable String endRegexSource,
                                           boolean allowA,
                                           boolean allowG);

    @Override
    public String toString() {
        return "Rule{"
            + "id=" + id
            + ", name='" + name + '\''
            + '}';
    }

    //*************************************************************************

    /**
     * Checks if the given regex source string contains capture group references.
     *
     * @param regexSource the string pattern to evaluate, or {@code null}
     * @return {@code true} if dynamic capture variables are found; {@code false} otherwise
     */
    protected boolean hasCaptures(@Nullable CharSequence regexSource) {
        if (regexSource == null) {
            return false;
        }
        return CAPTURING_REGEX_SOURCE.matcher(regexSource).find();
    }

    /**
     * Replaces capture group references in a scope name with extracted line substrings.
     *
     * @param regexSource    the scope template string containing capture placeholders
     * @param captureSource  the line text containing the source text
     * @param captureIndices array of match offsets for capture groups
     * @return the interpolated scope name string
     */
    protected String replaceCaptures(CharSequence regexSource,
                                     CharSequence captureSource,
                                     OnigCaptureIndex[] captureIndices) {
        Matcher m = CAPTURING_REGEX_SOURCE.matcher(regexSource);
        var sb = new StringBuilder();

        while (m.find()) {
            String match = m.group();
            String replacement = getReplacement(match, captureSource, captureIndices);
            m.appendReplacement(sb, replacement);
        }
        m.appendTail(sb);

        return sb.toString();
    }

    /**
     * Resolves a single capture variable expression (such as {@code $1} or {@code ${1:/downcase}})
     * to its dynamic string value.
     *
     * @param match          the matched variable expression token
     * @param captureSource  the line text containing matched content
     * @param captureIndices array of match offsets for capture groups
     * @return the evaluated replacement string, or the raw match string if capture index is missing
     */
    protected String getReplacement(String match,
                                    CharSequence captureSource,
                                    OnigCaptureIndex[] captureIndices) {
        int index;
        String command;
        int doublePointIndex = match.indexOf(':');

        if (doublePointIndex != -1) {
            index = Integer.parseInt(match.substring(2, doublePointIndex));
            command = match.substring(doublePointIndex + 2, match.length() - 1);
        } else {
            index = Integer.parseInt(match.substring(1));
            command = null;
        }

        OnigCaptureIndex capture = captureIndices.length > index ? captureIndices[index] : null;

        if (capture != null) {
            var result = captureSource.subSequence(capture.start(), capture.end());

            // remove leading dots that would make the selector invalid
            while (!result.isEmpty() && result.charAt(0) == '.') {
                result = result.subSequence(1, result.length());
            }

            if ("downcase".equals(command)) {
                return result.toString().toLowerCase();
            }
            if ("upcase".equals(command)) {
                return result.toString().toUpperCase();
            }

            return result.toString();
        }

        return match;
    }
}