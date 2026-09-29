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

/**
 * Provides methods to work with the {@code repository} property of the {@link IRawRule}.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/rawGrammar.ts#L37">
 * vscode-textmate/src/rawGrammar.ts#L37</a>
 * @see IRawRule#getRepository()
 * @see IRawRule#getInclude()
 */
public interface IRawRepository {

    /**
     * Returns the repository rule by its name, if present.
     *
     * @param name the name of the rule in the repository
     * @return the corresponding {@link IRawRule}, or {@code null} if no rule exists with that name
     */
    @Nullable
    IRawRule getRule(String name);

    /**
     * Returns the repository self reference (grammar).
     *
     * <p>The self reference allows the grammar itself to be included using the {@code $self}
     * keyword in the {@code include} attribute.
     *
     * @return the {@link IRawRule} bound to the {@code $self} reference
     */
    IRawRule getSelf();

    /**
     * Sets the specified rule as the repository self reference.
     *
     * <p>The "rule" in this context is an object that contains the root grammar
     * {@code scope} and {@code patterns} property values, representing the grammar itself.
     *
     * @param raw the rule to be set as the repository self reference
     * @see IRawGrammar#toRawRule()
     */
    void setSelf(IRawRule raw);

    /**
     * Returns the repository base reference.
     *
     * <p>This is basically the same as "self". Find more information here:
     * <a href="https://github.com/microsoft/vscode-textmate/issues/117">vscode-textmate/issues/117</a>
     *
     * @return the {@link IRawRule} bound to the {@code $base} reference
     */
    IRawRule getBase();

    /**
     * Sets the specified rule as the repository base reference.
     *
     * @param base the rule to be set as the repository base reference
     */
    void setBase(IRawRule base);

    /**
     * Adds the given entries to the repository.
     *
     * @param target the {@link PropertySettable} container where repository entries will be populated
     */
    void putEntries(PropertySettable<IRawRule> target);

    //*************************************************************************

    /**
     * Merges multiple repositories into a single repository.
     *
     * <p>Entries from later repositories in the parameter list overwrite matching keys
     * from earlier repositories. {@code null} arguments are safely ignored.
     *
     * @param sources the repositories to be merged
     * @return a new {@link IRawRepository} containing all merged entries
     */
    static IRawRepository merge(@Nullable IRawRepository... sources) {
        var merged = new RawRepository();
        for (var source : sources) {
            if (source == null) {
                continue;
            }
            source.putEntries(merged);
        }
        return merged;
    }
}