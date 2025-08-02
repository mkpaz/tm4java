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
 * A source regular expression.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/rule.ts#L582">
 * vscode-textmate/src/rule.ts#L582</a>
 */
final class RegExpSource {

    private static final Pattern HAS_BACK_REFERENCES = Pattern.compile("\\\\(\\d+)");
    private static final Pattern BACK_REFERENCING_END = Pattern.compile("\\\\(\\d+)");

    private String source;
    private String @Nullable [][] anchorCache;

    /**
     * The ID of the rule associated with this regular expression source.
     */
    final RuleId ruleId;

    /**
     * Whether this regular expression source has back references.
     */
    final boolean hasBackReferences;

    /**
     * See {@link #RegExpSource(String, RuleId, boolean)}.
     */
    RegExpSource(String regExpSource, RuleId ruleId) {
        this(regExpSource, ruleId, true);
    }

    /**
     * Creates a new regular expression source.
     *
     * @param regExpSource  the source of the regular expression
     * @param ruleId        the id of the rule associated with this regular expression source
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

    String getSource() {
        return this.source;
    }

    void setSource(String newSource) {
        if (Objects.equals(source, newSource)) {
            return;
        }
        this.source = newSource;

        if (hasAnchor()) {
            this.anchorCache = buildAnchorCache();
        }
    }

    boolean hasAnchor() {
        return anchorCache != null;
    }

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

    String resolveAnchors(boolean allowA, boolean allowG) {
        var anchorCache = this.anchorCache;
        if (anchorCache == null) {
            return source;
        }

        return anchorCache[allowA ? 1 : 0][allowG ? 1 : 0];
    }

    @Override
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    protected RegExpSource clone() {
        return new RegExpSource(source, this.ruleId);
    }

    //*************************************************************************

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

        return new String[][]{
            {resultA0G0.toString(), resultA0G1.toString()},
            {resultA1G0.toString(), resultA1G1.toString()}
        };
    }
}
