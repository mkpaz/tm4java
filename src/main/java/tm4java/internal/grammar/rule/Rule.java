/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.grammar.rule;


import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import tm4java.internal.grammar.oniguruma.OnigCaptureIndex;

/**
 * An abstract base class for TextMate rules.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/rule.ts#L43">
 * vscode-textmate/src/rule.ts#L43</a>
 */
public abstract class Rule {

    @SuppressWarnings("RegExpRedundantEscape")
    private static final Pattern CAPTURING_REGEX_SOURCE =
        Pattern.compile("\\$(\\d+)|\\$\\{(\\d+):\\/(downcase|upcase)}");

    protected final RuleId id;

    private final @Nullable String name;
    private final boolean nameIsCapturing;

    private final @Nullable String contentName;
    private final boolean contentNameIsCapturing;

    Rule(RuleId id, @Nullable String name, @Nullable String contentName) {
        this.id = id;
        this.name = name;
        this.nameIsCapturing = hasCaptures(name);
        this.contentName = contentName;
        this.contentNameIsCapturing = hasCaptures(contentName);
    }

    public @Nullable String getName(@Nullable CharSequence lineText, OnigCaptureIndex @Nullable [] captureIndices) {
        var name = this.name;
        if (!nameIsCapturing || name == null || lineText == null || captureIndices == null) {
            return name;
        }
        return replaceCaptures(name, lineText, captureIndices);
    }

    public @Nullable String getContentName(CharSequence lineText, OnigCaptureIndex[] captureIndices) {
        var contentName = this.contentName;
        if (!contentNameIsCapturing || contentName == null) {
            return contentName;
        }
        return replaceCaptures(contentName, lineText, captureIndices);
    }

    public abstract void collectPatterns(IRuleRegistry grammar,
                                         RegExpSourceList out);

    public abstract CompiledRule compile(IRuleRegistry grammar,
                                         @Nullable String endRegexSource);

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

    protected boolean hasCaptures(@Nullable CharSequence regexSource) {
        if (regexSource == null) {
            return false;
        }
        return CAPTURING_REGEX_SOURCE.matcher(regexSource).find();
    }

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
