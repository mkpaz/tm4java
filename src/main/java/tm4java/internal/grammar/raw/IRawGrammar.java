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

import java.util.Collection;
import java.util.Map;

/**
 * Represents a raw TextMate grammar root definition.
 *
 * <p>Encapsulates top-level metadata, syntax rules, repository definitions, scope injection settings,
 * and optional folding markers parsed from TextMate grammar files (JSON/Plist).
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/rawGrammar.ts#L8">
 * vscode-textmate/src/rawGrammar.ts#L8</a>
 * @see <a href="https://macromates.com/manual/en/language_grammars">TextMate grammars</a>
 */
public interface IRawGrammar {

    /**
     * Returns the value of the {@code name} property of the grammar.
     *
     * @return the human-readable display name of the language/grammar, or {@code null} if unspecified
     */
    @Nullable String getName();

    /**
     * Returns the value of the {@code scopeName} property of the grammar.
     *
     * <p>This unique, dot-separated name specifies the grammar, with each left-most part
     * specializing the name. Typically, it consists of two parts: the first is either
     * {@code text} or {@code source}, and the second is the language or document type,
     * e.g., {@code source.java}.
     *
     * @return the root scope identifier of this grammar (e.g. {@code "source.java"})
     */
    String getScopeName();

    /**
     * Returns the value of the {@code filetypes} property of the grammar.
     *
     * <p>This is an array of file type extensions that the grammar should (by default) be used with.
     *
     * @return a {@link Collection} of file extension strings (e.g. {@code ["java", "jav"]})
     */
    Collection<String> getFileTypes();

    /**
     * Returns the value of the {@code patterns} property of the grammar.
     *
     * <p>This is an array with the actual rules used to parse the document.
     *
     * <pre>{@code
     * patterns = (
     *     {  name = 'keyword.control.untitled';
     *        match = '\b(if|while|for|return)\b';
     *     },
     *     {  name = 'string.quoted.double.untitled';
     *        begin = '"';
     *        end = '"';
     *        patterns = (
     *            {  name = 'constant.character.escape.untitled';
     *               match = '\\.';
     *            }
     *        );
     *     },
     * );
     * }</pre>
     *
     * @return a collection of top-level {@link IRawRule} syntax patterns, or {@code null} if empty
     */
    @Nullable Collection<IRawRule> getPatterns(); // TODO: non-null in upstream project

    /**
     * Returns the value of the {@code injections} property of the grammar.
     *
     * <p>Injection grammars extend an existing grammar. An injection grammar is a
     * regular grammar injected into a specific scope within the existing grammar.
     *
     * @return a {@link Map} of injection scope selector keys to their corresponding
     * target {@link IRawRule} values, or {@code null}
     */
    @Nullable Map<String, IRawRule> getInjections();

    /**
     * Returns the value of the {@code injectionSelector} property of the grammar.
     *
     * <p>This scope selector specifies the scopes in which the injected grammar should
     * be applied.
     *
     * @return the TextMate scope selector string governing injection application, or {@code null}
     */
    @Nullable String getInjectionSelector();

    /**
     * Returns the value of the {@code firstLineMatch} property of the grammar.
     *
     * <p>This regex is matched against the first line of the document
     * when it is loaded. If it matches, the grammar is applied to the document,
     * unless overridden by the user.
     *
     * @return the regex pattern string for first-line auto-detection, or {@code null}
     */
    @Nullable String getFirstLineMatch();

    /**
     * Returns the value of the {@code repository} property of the grammar.
     *
     * <p>This is a dictionary of rules that can be included from other places in the
     * grammar. The key is the name of the rule, and the value is the actual rule.
     *
     * @return the {@link IRawRepository} container mapping rule names
     * to reusable {@link IRawRule} instances
     * @see IRawRule#getInclude()
     */
    IRawRepository getRepository();

    /**
     * Sets the specified repository as the grammar repository.
     *
     * @param repository the repository to be set as the grammar repository
     */
    void setRepository(IRawRepository repository);

    /**
     * Returns the value of the {@code foldingStartMarker} property of the grammar.
     *
     * <p>Folding markers are regular expressions that lines in the document are matched against.
     * If a line matches one of the patterns (but not both), it becomes a folding marker.
     *
     * @return the regex pattern string matching code block start boundaries, or {@code null}
     */
    @Nullable String getFoldingStartMarker(); // not in the upstream

    /**
     * Returns the value of the {@code foldingEndMarker} property of the grammar.
     *
     * @return the regex pattern string matching code block end boundaries, or {@code null}
     * @see #getFoldingStartMarker()
     */
    @Nullable String getFoldingEndMarker(); // not in the upstream

    /**
     * Converts the grammar to an {@link IRawRule}.
     *
     * @return a synthesized composite {@link IRawRule} representing this entire grammar definition
     */
    IRawRule toRawRule();
}
