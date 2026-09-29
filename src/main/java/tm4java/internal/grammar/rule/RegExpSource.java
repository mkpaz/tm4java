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
import tm4java.internal.utils.RegexUtils;

import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a source regex string with support for anchor transformations,
 * and back-reference resolutions.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/rule.ts#L582">
 * vscode-textmate/src/rule.ts#L582</a>
 */
final class RegExpSource {

    /** Regex pattern used to detect numeric back-references (e.g. {@code \1}, {@code \2}). */
    private static final Pattern HAS_BACK_REFERENCES = Pattern.compile("\\\\(\\d+)");

    /** Regex pattern used for matching and replacing back-reference tokens in regex source strings. */
    private static final Pattern BACK_REFERENCING_END = Pattern.compile("\\\\(\\d+)");

    /** The processed regex pattern source string. */
    private String source;

    /** 2D lookup array caching variations of the regex pattern based on {@code \A} and {@code \G} anchors. */
    private String @Nullable [][] anchorCache;

    /** The ID of the rule associated with this regex source. */
    final RuleId ruleId;

    /** Whether this regex source has back references. */
    final boolean hasBackReferences;

    /**
     * See {@link #RegExpSource(String, RuleId, boolean)}.
     *
     * @param regExpSource the source regex string
     * @param ruleId       the rule ID associated with this pattern
     */
    RegExpSource(String regExpSource, RuleId ruleId) {
        this(regExpSource, ruleId, true);
    }

    /**
     * Creates a new regex source.
     *
     * @param regExpSource  the source of the regex
     * @param ruleId        the id of the rule associated with this regex source
     * @param handleAnchors whether to handle anchors
     */
    RegExpSource(String regExpSource, RuleId ruleId, boolean handleAnchors) {
        if (handleAnchors && !regExpSource.isEmpty()) {
            int len = regExpSource.length();
            int lastPushedPos = 0;
            var output = new StringBuilder();

            boolean hasAnchors = false;
            for (int pos = 0; pos < len; pos++) {
                char ch = regExpSource.charAt(pos);

                if (ch == '\\') {
                    if (pos + 1 < len) {
                        char nextCh = regExpSource.charAt(pos + 1);
                        if (nextCh == 'z') {
                            output.append(regExpSource, lastPushedPos, pos);
                            output.append("$(?!\\n)(?<!\\n)");
                            lastPushedPos = pos + 2;
                        } else if (nextCh == 'A' || nextCh == 'G') {
                            hasAnchors = true;
                        }
                        pos++;
                    }
                }
            }

            if (lastPushedPos == 0) {
                // No \z hit
                source = regExpSource;
            } else {
                output.append(regExpSource, lastPushedPos, len);
                source = output.toString();
            }
            if (hasAnchors) {
                anchorCache = buildAnchorCache();
            }
        } else {
            this.source = regExpSource;
        }

        this.ruleId = ruleId;
        this.hasBackReferences = HAS_BACK_REFERENCES.matcher(this.source).find();
    }

    /**
     * Returns the underlying regex source string.
     *
     * @return the current regex source string
     */
    String getSource() {
        return this.source;
    }

    /**
     * Updates the underlying regex source string and rebuilds the anchor cache if necessary.
     *
     * @param newSource the new regex source string
     */
    void setSource(String newSource) {
        if (Objects.equals(source, newSource)) {
            return;
        }
        this.source = newSource;

        if (hasAnchor()) {
            this.anchorCache = buildAnchorCache();
        }
    }

    /**
     * Checks if this regex source contains cached anchor variations.
     *
     * @return {@code true} if an anchor cache is populated; {@code false} otherwise
     */
    boolean hasAnchor() {
        return anchorCache != null;
    }

    /**
     * Resolves back-references (e.g., {@code \1}) in the regex using capture values from a prior match.
     *
     * @param lineText       the line text sequence where the match occurred
     * @param captureIndices array of capture indices from the initial match
     * @return the resolved regex source string
     */
    String resolveBackReferences(CharSequence lineText, OnigCaptureIndex[] captureIndices) {
        var capturedValues = new ArrayList<String>(captureIndices.length);
        for (var capture : captureIndices) {
            capturedValues.add(lineText.subSequence(capture.start(), capture.end()).toString());
        }

        return BACK_REFERENCING_END.matcher(this.source).replaceAll(match -> {
            try {
                int index = Integer.parseInt(match.group(1));
                if (index < captureIndices.length) {
                    var replacement = RegexUtils.escapeRegExpCharacters(capturedValues.get(index));
                    // see https://stackoverflow.com/a/70785772/5116073
                    return Matcher.quoteReplacement(replacement);
                }
            } catch (NumberFormatException e) {
                // ignore
            }
            return "";
        });
    }

    /**
     * Retrieves the appropriate regex string variant based on anchor flags.
     *
     * @param allowA whether anchor {@code \A} matching is permitted
     * @param allowG whether anchor {@code \G} matching is permitted
     * @return the resolved regex source string for the specified anchor permissions
     */
    String resolveAnchors(boolean allowA, boolean allowG) {
        var anchorCache = this.anchorCache;
        if (anchorCache == null) {
            return source;
        }

        return anchorCache[allowA ? 1 : 0][allowG ? 1 : 0];
    }

    /**
     * Creates a clone of this {@link RegExpSource} instance.
     *
     * @return a new {@link RegExpSource} instance initialized with the same source string and rule ID
     */
    @Override
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    protected RegExpSource clone() {
        return new RegExpSource(source, this.ruleId);
    }

    //*************************************************************************

    /**
     * Builds a 2D cache matrix containing transformed regex variants for combinations of
     * allowed/disallowed {@code \A} and {@code \G} anchors.
     *
     * @return a 2x2 matrix of source strings indexed by {@code [allowA ? 1 : 0][allowG ? 1 : 0]}
     */
    private String[][] buildAnchorCache() {
        var source = this.source;
        var sourceLen = source.length();

        var resultA0G0 = new StringBuilder(sourceLen);
        var resultA0G1 = new StringBuilder(sourceLen);
        var resultA1G0 = new StringBuilder(sourceLen);
        var resultA1G1 = new StringBuilder(sourceLen);

        for (int pos = 0; pos < sourceLen; pos++) {
            char ch = source.charAt(pos);
            resultA0G0.append(ch);
            resultA0G1.append(ch);
            resultA1G0.append(ch);
            resultA1G1.append(ch);

            if (ch == '\\' && pos + 1 < sourceLen) {
                char nextCh = source.charAt(pos + 1);
                if (nextCh == 'A') {
                    resultA0G0.append('\uFFFF');
                    resultA0G1.append('\uFFFF');
                    resultA1G0.append('A');
                    resultA1G1.append('A');
                } else if (nextCh == 'G') {
                    resultA0G0.append('\uFFFF');
                    resultA0G1.append('G');
                    resultA1G0.append('\uFFFF');
                    resultA1G1.append('G');
                } else {
                    resultA0G0.append(nextCh);
                    resultA0G1.append(nextCh);
                    resultA1G0.append(nextCh);
                    resultA1G1.append(nextCh);
                }
                pos++;
            }
        }

        return new String[][] {
            {resultA0G0.toString(), resultA0G1.toString()},
            {resultA1G0.toString(), resultA1G1.toString()}
        };
    }
}