/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.theme;

import org.jspecify.annotations.Nullable;

import java.util.regex.Pattern;

/**
 * Represents an RGB color with red, green, and blue components.
 *
 * @param red   the red component (0-255)
 * @param green the green component (0-255)
 * @param blue  the blue component (0-255)
 */
public record RGB(int red, int green, int blue) {

    private static final Pattern RRGGBB = Pattern.compile("^#[0-9a-f]{6}", Pattern.CASE_INSENSITIVE);
    private static final Pattern RRGGBBAA = Pattern.compile("^#[0-9a-f]{8}", Pattern.CASE_INSENSITIVE);
    private static final Pattern RGB = Pattern.compile("^#[0-9a-f]{3}", Pattern.CASE_INSENSITIVE);
    private static final Pattern RGBA = Pattern.compile("^#[0-9a-f]{4}", Pattern.CASE_INSENSITIVE);

    /**
     * Creates an RGB color from a hexadecimal string.
     * The string may start with a '#' character.
     *
     * @param hex the hexadecimal color string (e.g., "#FF5733")
     */
    public static @Nullable RGB fromHex(@Nullable String hex) {
        if (hex == null || hex.isBlank()) {
            return null;
        }

        var offset = hex.startsWith("#") ? 1 : 0;

        return new RGB(
            Integer.parseInt(hex.substring(offset, offset + 2), 16),
            Integer.parseInt(hex.substring(offset + 2, offset + 4), 16),
            Integer.parseInt(hex.substring(offset + 4, offset + 6), 16)
        );
    }

    @Override
    public String toString() {
        return "rgb(" + red + "," + green + "," + blue + ")";
    }

    public static boolean isValidHexColor(CharSequence hex) {
        if (hex.isEmpty()) {
            return false;
        }
        if (RRGGBB.matcher(hex).matches()) {
            return true; // #rrggbb
        }
        if (RRGGBBAA.matcher(hex).matches()) {
            return true; // #rrggbbaa
        }
        if (RGB.matcher(hex).matches()) {
            return true; // #rgb
        }

        return RGBA.matcher(hex).matches(); // #rgba
    }
}
