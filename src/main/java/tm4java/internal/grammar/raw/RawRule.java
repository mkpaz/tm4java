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
import tm4java.internal.grammar.rule.RuleId;
import tm4java.parser.PropertySettable;

import java.io.Serial;
import java.util.Collection;
import java.util.List;

/**
 * The default implementation of {@link IRawRule}.
 */
public class RawRule extends PropertySettable.HashMap<Object> implements IRawRule {

    @Serial
    private static final long serialVersionUID = 1L;

    // property keys
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

    /**
     * {@inheritDoc}
     *
     * @return the assigned internal {@link RuleId}, or {@code null} if unassigned
     */
    @Override
    public @Nullable RuleId getId() {
        return (RuleId) get(ID);
    }

    /**
     * {@inheritDoc}
     *
     * @param id the internal rule id to be set
     */
    @Override
    public void setId(RuleId id) {
        super.put(ID, id);
    }

    /**
     * {@inheritDoc}
     *
     * @return the scope name string assigned to matches of this rule, or {@code null}
     */
    @Override
    public @Nullable String getName() {
        return (String) get(NAME);
    }

    /**
     * Sets the scope name property for this rule.
     *
     * @param name the scope name string to set
     * @return this {@link RawRule} instance for method chaining
     */
    public RawRule setName(String name) {
        super.put(NAME, name);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @return a {@link Collection} of child sub-rules, or {@code null} if no nested patterns exist
     */
    @Override
    @SuppressWarnings("unchecked")
    public @Nullable Collection<IRawRule> getPatterns() {
        return (Collection<IRawRule>) get(PATTERNS);
    }

    /**
     * Sets the sub-pattern collection for this rule.
     *
     * @param patterns the collection of nested {@link IRawRule} objects to set, or {@code null}
     * @return this {@link RawRule} instance for method chaining
     */
    public RawRule setPatterns(@Nullable Collection<IRawRule> patterns) {
        super.put(PATTERNS, patterns);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @return the scope name assigned exclusively to inner content, or {@code null}
     */
    @Override
    public @Nullable String getContentName() {
        return (String) get(CONTENT_NAME);
    }

    /**
     * {@inheritDoc}
     *
     * @return the regex pattern string for a single-line match, or {@code null}
     */
    @Override
    public @Nullable String getMatch() {
        return (String) get(MATCH);
    }

    /**
     * {@inheritDoc}
     *
     * @return the regex pattern string initiating a block rule, or {@code null}
     */
    @Override
    public @Nullable String getBegin() {
        return (String) get(BEGIN);
    }

    /**
     * {@inheritDoc}
     *
     * @return the regex pattern string that must continue matching each line, or {@code null}
     */
    @Override
    public @Nullable String getWhile() {
        return (String) get(WHILE);
    }

    /**
     * {@inheritDoc}
     *
     * @return the regex pattern string terminating a block rule, or {@code null}
     */
    @Override
    public @Nullable String getEnd() {
        return (String) get(END);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Ensures capture structures stored as JSON arrays are converted to 1-indexed group mappings.
     *
     * @return the {@link IRawCaptures} mapping capture groups to rules, or {@code null}
     */
    @Override
    public @Nullable IRawCaptures getCaptures() {
        updateCaptures(CAPTURES);
        return (IRawCaptures) get(CAPTURES);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Ensures capture structures stored as JSON arrays are converted to 1-indexed group mappings.
     *
     * @return the {@link IRawCaptures} mapping capture groups for the {@code begin} pattern, or {@code null}
     */
    @Override
    public @Nullable IRawCaptures getBeginCaptures() {
        updateCaptures(BEGIN_CAPTURES);
        return (IRawCaptures) get(BEGIN_CAPTURES);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Ensures capture structures stored as JSON arrays are converted to 1-indexed group mappings.
     *
     * @return the {@link IRawCaptures} mapping capture groups for the {@code while} pattern, or {@code null}
     */
    @Override
    public @Nullable IRawCaptures getWhileCaptures() {
        updateCaptures(WHILE_CAPTURES);
        return (IRawCaptures) get(WHILE_CAPTURES);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Ensures capture structures stored as JSON arrays are converted to 1-indexed group mappings.
     *
     * @return the {@link IRawCaptures} mapping capture groups for the {@code end} pattern, or {@code null}
     */
    @Override
    public @Nullable IRawCaptures getEndCaptures() {
        updateCaptures(END_CAPTURES);
        return (IRawCaptures) get(END_CAPTURES);
    }

    /**
     * {@inheritDoc}
     *
     * @return the local rule {@link IRawRepository}, or {@code null} if absent
     */
    @Override
    public @Nullable IRawRepository getRepository() {
        return (IRawRepository) get(REPOSITORY);
    }

    /**
     * {@inheritDoc}
     *
     * @return the target scope selector, {@code $self}, {@code $base}, or repository reference, or {@code null}
     */
    @Override
    public @Nullable String getInclude() {
        return (String) get(INCLUDE);
    }

    /**
     * Sets the target scope or repository reference string for inclusion.
     *
     * @param include the inclusion reference path string, or {@code null}
     * @return this {@link RawRule} instance for method chaining
     */
    public RawRule setInclude(@Nullable String include) {
        super.put(INCLUDE, include);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Evaluates boolean values, integer status flags (where {@code 1} represents {@code true}),
     * or defaults to {@code false}.
     *
     * @return {@code true} if the end pattern evaluation is deferred until after
     * inner patterns; {@code false} otherwise
     */
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

    /**
     * Normalizes capture list values into capture group rules.
     *
     * <p>Converts JSON array-structured captures into a map indexed sequentially starting from 1.
     *
     * @param name the key name of the capture field to check and update
     */
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