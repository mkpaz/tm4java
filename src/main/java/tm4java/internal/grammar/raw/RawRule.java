/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.grammar.raw;

import org.jspecify.annotations.Nullable;
import tm4java.parser.PropertySettable;
import tm4java.internal.grammar.rule.RuleId;

import java.io.Serial;
import java.util.Collection;
import java.util.List;

/**
 * The default implementation of {@link IRawRule}.
 */
public class RawRule extends PropertySettable.HashMap<@Nullable Object> implements IRawRule {

    @Serial
    private static final long serialVersionUID = 1L;

    protected static final String APPLY_END_PATTERN_LAST = "applyEndPatternLast";
    protected static final String BEGIN = "begin";
    protected static final String BEGIN_CAPTURES = "beginCaptures";
    protected static final String CAPTURES = "captures";
    protected static final String CONTENT_NAME = "contentName";
    protected static final String END = "end";
    protected static final String END_CAPTURES = "endCaptures";
    protected static final String ID = "id";
    protected static final String INCLUDE = "include";
    protected static final String MATCH = "match";
    protected static final String NAME = "name";
    protected static final String PATTERNS = "patterns";
    protected static final String REPOSITORY = "repository";
    protected static final String WHILE = "while";
    protected static final String WHILE_CAPTURES = "whileCaptures";

    @Override
    public @Nullable RuleId getId() {
        return (RuleId) get(ID);
    }

    @Override
    public void setId(RuleId id) {
        super.put(ID, id);
    }

    @Override
    public @Nullable String getName() {
        return (String) get(NAME);
    }

    public RawRule setName(String name) {
        super.put(NAME, name);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable Collection<IRawRule> getPatterns() {
        return (Collection<IRawRule>) get(PATTERNS);
    }

    public RawRule setPatterns(@Nullable Collection<IRawRule> patterns) {
        super.put(PATTERNS, patterns);
        return this;
    }

    @Override
    public @Nullable String getContentName() {
        return (String) get(CONTENT_NAME);
    }

    @Override
    public @Nullable String getMatch() {
        return (String) get(MATCH);
    }

    @Override
    public @Nullable String getBegin() {
        return (String) get(BEGIN);
    }

    @Override
    public @Nullable String getWhile() {
        return (String) get(WHILE);
    }

    @Override
    public @Nullable String getEnd() {
        return (String) get(END);
    }

    @Override
    public @Nullable IRawCaptures getCaptures() {
        updateCaptures(CAPTURES);
        return (IRawCaptures) get(CAPTURES);
    }

    @Override
    public @Nullable IRawCaptures getBeginCaptures() {
        updateCaptures(BEGIN_CAPTURES);
        return (IRawCaptures) get(BEGIN_CAPTURES);
    }

    @Override
    public @Nullable IRawCaptures getWhileCaptures() {
        updateCaptures(WHILE_CAPTURES);
        return (IRawCaptures) get(WHILE_CAPTURES);
    }

    @Override
    public @Nullable IRawCaptures getEndCaptures() {
        updateCaptures(END_CAPTURES);
        return (IRawCaptures) get(END_CAPTURES);
    }

    @Override
    public @Nullable IRawRepository getRepository() {
        return (IRawRepository) get(REPOSITORY);
    }

    @Override
    public @Nullable String getInclude() {
        return (String) get(INCLUDE);
    }

    public RawRule setInclude(@Nullable String include) {
        super.put(INCLUDE, include);
        return this;
    }

    @Override
    public boolean isApplyEndPatternLast() {
        Object applyEndPatternLast = get(APPLY_END_PATTERN_LAST);
        return switch (applyEndPatternLast) {
            case Boolean asBool -> asBool;
            case Integer asInt -> asInt == 1;
            case null, default -> false;
        };
    }

    //*************************************************************************

    protected void updateCaptures(String name) {
        Object captures = get(name);
        if (captures instanceof List<?> capturesList) {
            var rawCaptures = new RawRule();
            int i = 0;
            for (var capture : capturesList) {
                i++;
                rawCaptures.put(Integer.toString(i), capture);
            }
            super.put(name, rawCaptures);
        }
    }
}
