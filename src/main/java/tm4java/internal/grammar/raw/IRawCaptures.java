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

import java.util.function.BiConsumer;

/**
 * Provides methods to work with the capture properties of the {@link IRawRule}.
 *
 * <p>Maps capture group indices or names (as string IDs) to their corresponding nested
 * syntax {@link IRawRule} definitions.
 *
 * <pre>{@code
 * match = '(@selector\$)(.*?)(\$)';
 * captures = {
 *      1 = { name = 'storage.type.objc'; };
 *      3 = { name = 'storage.type.objc'; };
 * };
 * }</pre>
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/rawGrammar.ts#L62">
 * vscode-textmate/src/rawGrammar.ts#L62</a>
 * @see IRawRule
 */
public interface IRawCaptures {

    /**
     * Returns the capture rule by the specified capture id.
     *
     * @param captureId the string representation of the capture group index
     *                  (e.g. {@code "1"}) or group name
     * @return the corresponding {@link IRawRule} for the capture group,
     * or {@code null} if no rule is bound
     */
    @Nullable
    IRawRule getCapture(String captureId);

    /**
     * Returns all capture rule ids.
     *
     * @return an {@link Iterable} over all registered capture group IDs
     */
    Iterable<String> getCaptureIds();

    /**
     * Executes the specified action for each capture rule.
     *
     * @param action a {@link BiConsumer} accepting the capture ID and its associated {@link IRawRule}
     */
    void forEachCapture(BiConsumer<String, IRawRule> action);
}