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
import java.util.NoSuchElementException;

/**
 * The default implementation of the {@link IRawRepository}.
 */
public class RawRepository extends PropertySettable.HashMap<IRawRule> implements IRawRepository {

    @Serial
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = System.getLogger(RawRepository.class.getName());

    public static final String DOLLAR_BASE = "$base";
    public static final String DOLLAR_SELF = "$self";

    private IRawRule getOrThrow(String key) {
        IRawRule obj = get(key);
        if (obj == null) {
            throw new NoSuchElementException("Key '" + key + "' does not exit found");
        }
        return obj;
    }

    @Override
    public @Nullable IRawRule getRule(String name) {
        try {
            return get(name);
        } catch (ClassCastException e) {
            LOGGER.log(Level.ERROR, "Unexpected ClassCastException in RawRepository.getRule('" + name + "')", e);
            throw e;
        }
    }

    @Override
    public IRawRule getSelf() {
        return getOrThrow(DOLLAR_SELF);
    }

    @Override
    public void setSelf(IRawRule self) {
        super.put(DOLLAR_SELF, self);
    }

    @Override
    public IRawRule getBase() {
        return getOrThrow(DOLLAR_BASE);
    }

    @Override
    public void setBase(IRawRule base) {
        super.put(DOLLAR_BASE, base);
    }

    @Override
    public void putEntries(PropertySettable<IRawRule> target) {
        for (var entry : entrySet()) {
            target.setProperty(entry.getKey(), entry.getValue());
        }
    }
}
