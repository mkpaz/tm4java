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
 *
 * <p>Acts as a dictionary storing named, reusable rule definitions and maintaining references
 * to special scope identifiers like {@code $self} and {@code $base}.
 */
public class RawRepository extends PropertySettable.HashMap<IRawRule> implements IRawRepository {

    private static final Logger LOGGER = System.getLogger(RawRepository.class.getName());

    @Serial
    private static final long serialVersionUID = 1L;

    // property keys
    public static final String DOLLAR_BASE = "$base";
    public static final String DOLLAR_SELF = "$self";

    /**
     * Retrieves a rule for the specified key or throws an exception if not found.
     *
     * @param key the lookup key in the repository
     * @return the associated {@link IRawRule}
     * @throws NoSuchElementException if no entry exists for the given key
     */
    private IRawRule getOrThrow(String key) {
        IRawRule obj = get(key);
        if (obj == null) {
            throw new NoSuchElementException("Key '" + key + "' does not exit found");
        }
        return obj;
    }

    /**
     * {@inheritDoc}
     *
     * @param name the repository rule identifier name
     * @return the matching {@link IRawRule}, or {@code null} if no rule exists with that name
     * @throws ClassCastException if the stored map value cannot be cast to {@link IRawRule}
     */
    @Override
    public @Nullable IRawRule getRule(String name) {
        try {
            return get(name);
        } catch (ClassCastException e) {
            LOGGER.log(Level.ERROR, "Unexpected ClassCastException in RawRepository.getRule('" + name + "')", e);
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link IRawRule} registered under {@value #DOLLAR_SELF}
     * @throws NoSuchElementException if the {@value #DOLLAR_SELF} entry is unmapped
     */
    @Override
    public IRawRule getSelf() {
        return getOrThrow(DOLLAR_SELF);
    }

    /**
     * {@inheritDoc}
     *
     * @param self the {@link IRawRule} to store as the {@value #DOLLAR_SELF} reference
     */
    @Override
    public void setSelf(IRawRule self) {
        super.put(DOLLAR_SELF, self);
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link IRawRule} registered under {@value #DOLLAR_BASE}
     * @throws NoSuchElementException if the {@value #DOLLAR_BASE} entry is unmapped
     */
    @Override
    public IRawRule getBase() {
        return getOrThrow(DOLLAR_BASE);
    }

    /**
     * {@inheritDoc}
     *
     * @param base the {@link IRawRule} to store as the {@value #DOLLAR_BASE} reference
     */
    @Override
    public void setBase(IRawRule base) {
        super.put(DOLLAR_BASE, base);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Copies all key-value entries present in this repository into the specified target container.
     *
     * @param target the {@link PropertySettable} receiver instance to populate
     */
    @Override
    public void putEntries(PropertySettable<IRawRule> target) {
        for (var entry : entrySet()) {
            target.setProperty(entry.getKey(), entry.getValue());
        }
    }
}