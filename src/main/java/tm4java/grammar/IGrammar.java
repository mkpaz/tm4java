/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.grammar;

import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.Collection;

/**
 * TextMate grammar interface for tokenizing text lines into scope-annotated or binary-encoded tokens.
 *
 * <p>Language grammars assign scope names to document elements (such as keywords, comments, and strings)
 * and maintain state stack information across line boundaries.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/blob/v9.2.0/src/main.ts#L200">
 * vscode-textmate/src/main.ts#L200</a>
 */
public interface IGrammar {

    /**
     * Returns the human-readable display name of the grammar as specified in the grammar definition file.
     *
     * @return the grammar name (e.g., {@code "Java"}), or {@code null} if not defined in the grammar
     */
    @Nullable
    String getName();

    /**
     * Returns the root scope name of the grammar.
     *
     * <p>The scope name is a unique, dot-separated identifier where each segment narrows down
     * the scope specialization. For example, the Java scope name is {@code "source.java"}.
     *
     * @return the unique root scope name of this grammar
     */
    String getScopeName();

    /**
     * Returns the list of file extensions or file types associated with this grammar.
     *
     * @return an unmodifiable collection of file type extensions supported by this grammar
     */
    Collection<String> getFileTypes();

    /**
     * Returns the regex pattern used to detect the start of a folding region.
     *
     * <p>Folding markers are optional patterns defined in some legacy TextMate grammars.
     * Note that folding markers are maintained for compatibility and are not supported
     * in upstream VSCode TextMate.
     *
     * @return the folding start marker regex, or {@code null} if not defined
     */
    @Nullable
    String getFoldingStartMarker();

    /**
     * Returns the regex pattern used to detect the end of a folding region.
     *
     * <p>Folding markers are optional patterns defined in some legacy TextMate grammars.
     * Note that folding markers are maintained for compatibility and are not supported
     * in upstream VSCode TextMate.
     *
     * @return the folding end marker regex, or {@code null} if not defined
     */
    @Nullable
    String getFoldingEndMarker();

    /**
     * Tokenizes the given line of text starting from an initial/empty state.
     *
     * <p>This is a convenience overload equivalent to calling {@link #tokenizeLine(String, IStateStack, Duration)}
     * with {@code prevState = null} and {@code timeLimit = null}.
     *
     * @param lineText the line of text to tokenize
     * @return the result containing parsed tokens and the resulting state stack
     */
    ITokenizeLineResult<IToken[]> tokenizeLine(String lineText);

    /**
     * Tokenizes the given line of text using the state stack from the end of the previous line.
     *
     * <p>TextMate grammars process text strictly line-by-line. The line continuation state is preserved
     * via the returned {@link IStateStack} in {@link ITokenizeLineResult}.
     *
     * @param lineText  the line of text to tokenize
     * @param prevState the state stack from the end of the previous line, or {@code null} for the first line
     * @param timeLimit the duration after which tokenization should be aborted if incomplete
     * @return the result containing array of tokens, state stack, and an execution status flag
     */
    ITokenizeLineResult<IToken[]> tokenizeLine(String lineText,
                                               @Nullable IStateStack prevState,
                                               @Nullable Duration timeLimit);

    /**
     * Tokenizes the given line of text into binary-encoded tokens with resolved theme information,
     * starting from an initial/empty state.
     *
     * <p>This is a convenience overload equivalent to calling {@link #tokenizeLine2(String, IStateStack, Duration)}
     * with {@code prevState = null} and {@code timeLimit = null}.
     *
     * @param lineText the line of text to tokenize
     * @return the result containing flat binary-encoded metadata array and the resulting state stack
     * @see #tokenizeLine2(String, IStateStack, Duration)
     */
    ITokenizeLineResult<int[]> tokenizeLine2(String lineText);

    /**
     * Tokenizes the given line of text into binary-encoded tokens with resolved theme information
     * using the state stack from the previous line.
     *
     * <p>Each token occupies two consecutive array indices in the returned {@code int[]} payload:
     * <ul>
     * <li><code>Index {@code 2 * i}:</code> Token start character offset (0-based)</li>
     * <li><code>Index {@code 2 * i + 1}:</code> Bit-packed metadata (language ID, token type, font styles, colors)</li>
     * </ul>
     *
     * <p>Bitfields in the metadata integer can be extracted using methods in {@link EncodedTokenAttributes}.
     *
     * @param lineText  the line of text to tokenize
     * @param prevState the state stack from the end of the previous line, or {@code null} for the first line
     * @param timeLimit the duration after which tokenization should be aborted if incomplete
     * @return the result containing flat binary token representation and updated line state
     */
    ITokenizeLineResult<int[]> tokenizeLine2(String lineText,
                                             @Nullable IStateStack prevState,
                                             @Nullable Duration timeLimit);
}