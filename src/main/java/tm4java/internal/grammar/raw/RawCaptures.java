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

import java.io.Serial;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.function.BiConsumer;

/**
 * The default implementation of the {@link IRawCaptures}.
 */
public class RawCaptures extends PropertySettable.HashMap<IRawRule> implements IRawCaptures {

    @Serial
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = System.getLogger(RawCaptures.class.getName());

    @Override
    public @Nullable IRawRule getCapture(String captureId) {
        try {
            return get(captureId);
        } catch (ClassCastException e) {
            LOGGER.log(Level.ERROR, "Unexpected ClassCastException in RawCaptures.getCapture('" + captureId + "')", e);
            throw e;
        }
    }

    @Override
    public Iterable<String> getCaptureIds() {
        return keySet();
    }

    @Override
    public void forEachCapture(BiConsumer<String, IRawRule> action) {
        forEach((String captureId, Object rule) -> {
            try {
                // to handle e.g.
                // https://github.com/microsoft/vscode/blob/1.100.0/extensions/yaml/syntaxes/yaml-1.1.tmLanguage.json#L139-L159
                if ("comment".equals(captureId) && rule instanceof String) {
                    return;
                }
                action.accept(captureId, (IRawRule) rule);
            } catch (ClassCastException e) {
                LOGGER.log(Level.ERROR,
                    "Unexpected ClassCastException in RawCaptures.getCapture('" + captureId + "')", e);
                throw e;
            }
        });
    }
}
