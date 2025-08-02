/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.grammar.raw;

import java.util.function.BiConsumer;
import org.jspecify.annotations.Nullable;

/**
 * Provides methods to work with the capture properties of the {@link IRawRule}.
 * <p>
 * See {@link IRawRule#getCaptures()}, {@link IRawRule#getBeginCaptures()},
 * {@link IRawRule#getWhileCaptures()}, and {@link IRawRule#getEndCaptures()}.
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
 */
public interface IRawCaptures {

    /**
     * Returns the capture rule by the specified capture id.
     *
     * @param captureId the id of the capture
     */
    @Nullable
    IRawRule getCapture(String captureId);

    /**
     * Returns all capture rule ids.
     */
    Iterable<String> getCaptureIds();

    /**
     * Executes the specified action for each capture rule.
     */
    void forEachCapture(BiConsumer<String, IRawRule> action);
}
