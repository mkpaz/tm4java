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
 * TextMate grammar API. Language grammars assign names to document elements,
 * such as keywords, comments, and strings.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/main.ts#L200">vscode-textmate/src/main.ts#L200</a>
 */
public interface IGrammar {

    /**
     * Returns the name of the grammar as specified in the grammar file.
     * For example, "Java".
     */
    @Nullable
    String getName();

    /**
     * Returns the scope name of the grammar as specified in the grammar file.
     * <p>
     * The scope name is a unique, dot-separated identifier where each left-most part specializes the name.
     * For example, Java is "source.java".
     */
    String getScopeName();

    /**
     * Returns the supported file types as specified in the grammar file.
     */
    Collection<String> getFileTypes();

    /**
     * Returns the folding start marker regex.
     * <p>
     * Folding markers are regular expressions that lines in the document are matched against.
     * If a line matches one of the patterns (but not both), it becomes a folding marker.
     * <p>
     * Folding markers are defined in some grammars but are not supported in the upstream.
     */
    @Nullable
    String getFoldingStartMarker();

    /**
     * Returns the folding end marker regex.
     * <p>
     * Folding markers are defined in some grammars but are not supported in the upstream.
     */
    @Nullable
    String getFoldingEndMarker();

    /**
     * Tokenizes the given line of text.
     * <p>
     * Note that TextMate grammars work with exactly with lines, not a paragraphs or blocks.
     * The LF at the end of the line is not required.
     */
    ITokenizeLineResult<IToken[]> tokenizeLine(String lineText);

    /**
     * Tokenizes the given line of text using the previous line state {@code prevState}.
     *
     * @param lineText  the line of text to tokenize
     * @param prevState the previous line state
     * @param timeLimit the duration after which tokenization is aborted; in this case, the returned
     *                  result will have {@link ITokenizeLineResult#stoppedEarly()} set to true.
     */
    ITokenizeLineResult<IToken[]> tokenizeLine(String lineText,
                                               @Nullable IStateStack prevState,
                                               @Nullable Duration timeLimit);

    /**
     * Tokenizes the given line of text into binary-encoded tokens with resolved theme information.
     * <p>
     * Each token occupies two array indices. For token index {@code i}:
     * <ul>
     * <li>startIndex at offset <code>2*i</code></li>
     * <li>metadata at offset <code>2*i + 1</code></li>
     * </ul>
     * The metadata in binary format contains the following information:
     * <pre>
     * - language
     * - token type (regex, string, comment, other)
     * - font style
     * - foreground color
     * - background color
     * </pre>
     * <p>
     * Use {@link EncodedTokenAttributes} to obtain the encoded metadata information.
     */
    ITokenizeLineResult<int[]> tokenizeLine2(String lineText);

    /**
     * Tokenizes the given line of text into binary-encoded tokens with resolved theme information
     * using the previous line state {@code prevState}.
     * <p>
     * Also see {@link #tokenizeLine2(String)}.
     *
     * @param lineText  the line of text to tokenize
     * @param prevState the previous line state
     * @param timeLimit the duration after which tokenization is aborted; in this case, the returned
     *                  result will have {@link ITokenizeLineResult#stoppedEarly()} set to true.
     */
    ITokenizeLineResult<int[]> tokenizeLine2(String lineText,
                                             @Nullable IStateStack prevState,
                                             @Nullable Duration timeLimit);
}
