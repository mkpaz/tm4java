/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.theme;

import org.jspecify.annotations.Nullable;
import tm4java.TMException;
import tm4java.theme.StyleAttributes;

import java.util.*;

/**
 * The ColorMap contains an indexed list of all color variables parsed from the theme.
 * This works with {@link StyleAttributes}, which only refers to the color ID (index) in the map.
 *
 * <p>Color IDs are 1-based integers where {@code 0} indicates an undefined/null color.
 * The map can operate in dynamic mode (auto-registering new colors) or in frozen mode (read-only palette).
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/blob/v9.2.0/src/theme.ts#L405">
 * vscode-textmate/theme.ts#L405</a>.
 */
public final class ColorMap {

    /** Flag indicating whether the color map is immutable (frozen). */
    final boolean isFrozen;

    /** Counter tracking the last assigned color ID integer. */
    int lastColorId = 0;

    /** Internal list mapping color integer IDs to their normalized uppercase HEX color strings. */
    final List<String> id2color = new ArrayList<>();

    /** Unmodifiable view of {@link #id2color} exposed to consumers. */
    final List<String> id2colorUnmodifiable = Collections.unmodifiableList(id2color);

    /** Internal lookup map mapping uppercase HEX color strings to their integer color IDs. */
    final Map<String /*color*/, @Nullable Integer /*color id*/> color2id = new HashMap<>();

    /**
     * Constructs an empty, mutable (unfrozen) {@code ColorMap}.
     */
    public ColorMap() {
        this(null);
    }

    /**
     * Constructs a {@code ColorMap} initialized with a predefined list of HEX color strings.
     *
     * <p>If a list is provided, the color map becomes frozen (read-only) and throws an exception
     * if an unknown color is queried.
     *
     * @param colorMap optional initial list of HEX color strings, or {@code null} to create a mutable map
     */
    public ColorMap(@Nullable List<String> colorMap) {
        // the upstream implementation works with 1-based indexes
        id2color.add("");

        if (colorMap != null) {
            isFrozen = true;
            for (String color : colorMap) {
                String colorUpper = color.toUpperCase();
                color2id.put(colorUpper, id2color.size());
                id2color.add(colorUpper);
            }
        } else {
            isFrozen = false;
        }
    }

    /**
     * Returns the 1-based integer color ID for the given HEX color string.
     *
     * <p>If the color is {@code null}, returns {@code 0}. If the color is new and the map
     * is not frozen, registers the new color and returns its newly assigned ID.
     *
     * @param color the HEX color string (e.g. {@code "#FF0000"}) or {@code null}
     * @return the 1-based color ID, or {@code 0} if {@code color} is {@code null}
     * @throws TMException if the color is missing and this map is frozen
     */
    public int getId(@Nullable String color) {
        if (color == null) {
            return 0;
        }

        String colorUpper = color.toUpperCase();
        Integer value = color2id.get(colorUpper);
        if (value != null) {
            return value;
        }

        if (isFrozen) {
            throw new TMException("Missing color in frozen color map:" + colorUpper);
        }

        value = ++lastColorId;
        color2id.put(colorUpper, value);
        id2color.add(colorUpper);

        return value;
    }

    /**
     * Returns an unmodifiable list mapping color IDs to their HEX color string values.
     *
     * <p>Note that index {@code 0} contains an empty string as color IDs are 1-based.
     *
     * @return an unmodifiable list of color strings
     */
    public List<String> getColorMap() {
        return id2colorUnmodifiable;
    }

    @Override
    public int hashCode() {
        return 31 * (31 + lastColorId) + color2id.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof ColorMap other) {
            return lastColorId == other.lastColorId && color2id.equals(other.color2id);
        }

        return false;
    }
}
