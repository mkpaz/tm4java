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

import java.util.Collection;

/**
 * Represents a raw TextMate grammar rule.
 *
 * <p>Defines a single syntax matching unit which may be a simple match pattern, a bounded block
 * pattern (begin/end or begin/while), an inclusion reference, or a repository entry holder.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/rawGrammar.ts#L39">
 * vscode-textmate/src/rawGrammar.ts#L39</a>
 * @see <a href="https://macromates.com/manual/en/language_grammars">TextMate rule keys</a>
 */
public interface IRawRule {

    /**
     * Returns the value of the rule id.
     *
     * <p>This is not part of the specification and is used only internally.
     *
     * @return the internal {@link RuleId} assigned to this rule, or {@code null} if unassigned
     */
    @Nullable
    RuleId getId();

    /**
     * Sets the internal rule id.
     *
     * <p>This is not part of the specification and is used only internally.
     *
     * @param id the internal rule id to be set
     */
    void setId(RuleId id);

    /**
     * Returns the value of the {@code name} property of the rule.
     *
     * <p>This property is used for styling and scope-specific settings and actions,
     * and should generally be derived from standard names.
     *
     * @return the scope name assigned to matches of this rule
     * (e.g. {@code "comment.line.double-slash"}), or {@code null}
     */
    @Nullable String getName();

    /**
     * Returns the value of the {@code patterns} property of the rule.
     *
     * @return a {@link Collection} of child sub-rules, or {@code null} if no nested patterns exist
     * @see IRawGrammar#getPatterns()
     */
    @Nullable Collection<IRawRule> getPatterns();

    /**
     * Returns the value of the {@code contentName} property of the rule.
     *
     * <p>This property is similar to the name key but assigns the name only to the text matched
     * by the {@code begin} / {@code end} patterns. For example, to get the text between
     * {@code #if 0} and {@code #endif} marked up as a comment:
     *
     * <pre>{@code
     * {  begin = '#if 0(\\s.*)?$'; end = '#endif';
     *    contentName = 'comment.block.preprocessor';
     * };
     * }</pre>
     *
     * @return the scope name assigned exclusively to the inner enclosed content, or {@code null}
     */
    @Nullable String getContentName();

    /**
     * Returns the value of the {@code match} property of the rule.
     *
     * <p>This property contains a regex used to identify the portion of text
     * to which the name should be assigned. For example: {@code '\b(true|false)\b'}.
     *
     * @return the regex pattern string for a single-line match, or {@code null}
     */
    @Nullable String getMatch();

    /**
     * Returns the value of the {@code begin} property of the rule.
     *
     * <p>The {@code begin} and {@code end} properties allow matches that span several lines and
     * must be mutually exclusive with the {@code match} key. Each is a regex
     * pattern. {@code begin} is the pattern that starts the block, and {@code end} is the
     * pattern that ends the block. Captures from the {@code begin} pattern can be referenced
     * in the {@code end} pattern using normal regex back-references. This is
     * often used with here-docs.
     *
     * <pre>{@code
     * name  = 'string.unquoted.here-doc';
     * begin = '<<(\w+)'; // match here-doc token
     * end   = '^\1$';    // match end of here-doc
     * }</pre>
     *
     * @return the regex pattern string initiating a block rule, or {@code null}
     */
    @Nullable
    String getBegin();

    /**
     * Returns the value of the {@code while} property of the rule.
     *
     * <p>Defines a regex that must match on each subsequent line after the
     * {@code begin} match until either the {@code end} pattern is matched or the
     * {@code while} pattern fails. If the {@code while} pattern fails on a line, the
     * {@code end} pattern (if present) is then checked on that line.
     *
     * @return the regex pattern string that must continue matching each line, or {@code null}
     */
    @Nullable
    String getWhile();

    /**
     * Returns the value of the {@code end} property of the rule.
     *
     * @return the regex pattern string terminating a block rule, or {@code null}
     * @see #getBegin()
     */
    @Nullable
    String getEnd();

    /**
     * Returns the value of the {@code captures} property of the rule.
     *
     * <p>The {@code captures}, {@code beginCaptures}, and {@code endCaptures} keys allow
     * assigning scopes to the captures of the {@code match}, {@code begin}, or
     * {@code end} patterns. Using the {@code captures} key for a {@code begin}/
     * {@code end} rule is shorthand for providing both {@code beginCaptures} and
     * {@code endCaptures} with the same values.
     *
     * <p>The value of these keys is a dictionary where the key is the capture number and
     * the value is a dictionary of attributes to assign to the captured text. Currently,
     * the only supported attribute is (scope) name.
     *
     * <pre>{@code
     * match = '(@selector\$)(.*?)(\$)';
     * captures = {
     *      1 = { name = 'storage.type.objc'; };
     *      3 = { name = 'storage.type.objc'; };
     * };
     * }</pre>
     *
     * <p>In this example, we match text like {@code @selector(windowWillClose:)} but the
     * {@code storage.type.objc} name will only be assigned to {@code @selector(} and
     * {@code )}.
     *
     * @return the {@link IRawCaptures} mapping capture groups to rules, or {@code null}
     */
    @Nullable IRawCaptures getCaptures();

    /**
     * Returns the value of the {@code beginCaptures} property of the rule.
     *
     * @return the {@link IRawCaptures} mapping capture groups for the {@code begin} pattern, or {@code null}
     * @see #getCaptures()
     */
    @Nullable IRawCaptures getBeginCaptures();

    /**
     * Returns the value of the {@code whileCaptures} property of the rule.
     *
     * @return the {@link IRawCaptures} mapping capture groups for the {@code while} pattern, or {@code null}
     * @see #getCaptures()
     */
    @Nullable IRawCaptures getWhileCaptures();

    /**
     * Returns the value of the {@code endCaptures} property of the rule.
     *
     * @return the {@link IRawCaptures} mapping capture groups for the {@code end} pattern, or {@code null}
     * @see #getCaptures()
     */
    @Nullable IRawCaptures getEndCaptures();

    /**
     * Returns the value of the {@code repository} property of the rule.
     *
     * <p>This is a dictionary of rules that can be included from other places in the
     * grammar. The key is the name of the rule, and the value is the actual rule.
     *
     * @return the local rule {@link IRawRepository}, or {@code null} if no local repository is declared
     * @see IRawGrammar#getRepository()
     * @see #getInclude()
     */
    @Nullable IRawRepository getRepository();

    /**
     * Returns the value of the {@code includes} property of the rule.
     *
     * <p>This property allows referencing a different language, recursively referencing the
     * grammar itself, or a rule declared in this file's repository.
     *
     * <p>To reference another language, the scope name of that language is used:
     * <pre>{@code
     * begin = '<\?(php|=)?'; end = '\?>'; patterns = (
     *      { include = "source.php"; }
     * );
     * }</pre>
     *
     * <p>To reference the grammar itself, {@code $self} is used.
     * <pre>{@code
     * begin = '\('; end = '\)'; patterns = (
     *      { include = "$self"; }
     * );
     * }</pre>
     *
     * <p>To reference a rule from the current grammar's repository, the name is prefixed
     * with a pound sign (#).
     * <pre>{@code
     * patterns = (
     *     {  begin = '"'; end = '"'; patterns = (
     *            { include = "#escaped-char"; },
     *            { include = "#variable"; }
     *        );
     *     },
     * );
     * repository = {
     *     escaped-char = { match = '\\.'; };
     *     variable =     { match = '\$[a-zA-Z0-9_]+'; };
     * };
     * }</pre>
     *
     * @return the target scope selector, {@code $self}, {@code $base},
     * or repository reference key (e.g. {@code "#rule-name"}), or {@code null}
     */
    @Nullable String getInclude();

    /**
     * Returns the value of the {@code applyEndPatternLast} property for this rule.
     *
     * <p>This property determines whether the {@code end} pattern is applied after all other
     * rule patterns. When set to {@code true}, the end pattern is matched last within the
     * rule.
     *
     * @return {@code true} if the end pattern evaluation is deferred until after inner patterns;
     * {@code false} otherwise
     */
    boolean isApplyEndPatternLast();
}