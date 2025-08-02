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
 * <p>
 * See <a href="https://github.com/microsoft/vscode-textmate/blob/v9.2.0/src/theme.ts#L385">
 * vscode-textmate/theme.ts#ColorMap#L385</a>.
 */
public final class ColorMap {

    final boolean isFrozen;
    int lastColorId = 0;
    final List<String> id2color = new ArrayList<>();
    final List<String> id2colorUnmodifiable = Collections.unmodifiableList(id2color);
    final Map<String /*color*/, @Nullable Integer /*color id*/> color2id = new HashMap<>();

    public ColorMap() {
        this(null);
    }

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
