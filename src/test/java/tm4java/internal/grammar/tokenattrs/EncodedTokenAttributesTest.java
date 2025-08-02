/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.grammar.tokenattrs;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.*;
import tm4java.grammar.EncodedTokenAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static tm4java.internal.theme.FontStyle.*;

/**
 * {@link EncodedTokenAttributes} tests same than vscode-textmate.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/tests/grammar.test.ts">
 * github.com/Microsoft/vscode-textmate/blob/master/src/tests/grammar.test.ts</a>
 */
@NullMarked
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EncodedTokenAttributesTest {

    @Test
    @Order(1)
    @DisplayName("StackElementMetadata works")
    public void testWorks() {
        int value = EncodedTokenAttributes.set(0, 1, OptionalStandardTokenType.REGEX, false, UNDERLINE | BOLD, 101, 102);
        assertEquals(value, 1, StandardTokenType.REGEX, false, UNDERLINE | BOLD, 101, 102);
    }

    @Test
    @Order(2)
    @DisplayName("StackElementMetadata can overwrite languageId")
    public void testCanOverwriteLanguageId() {
        int value =
            EncodedTokenAttributes.set(0, 1, OptionalStandardTokenType.REGEX, false, UNDERLINE | BOLD, 101, 102);
        assertEquals(value, 1, StandardTokenType.REGEX, false, UNDERLINE | BOLD, 101, 102);

        value = EncodedTokenAttributes.set(value, 2, OptionalStandardTokenType.NOT_SET, false, NOT_SET, 0, 0);
        assertEquals(value, 2, StandardTokenType.REGEX, false, UNDERLINE | BOLD, 101, 102);
    }

    @Test
    @Order(3)
    @DisplayName("StackElementMetadata can overwrite tokenType")
    public void testCanOverwriteTokenType() {
        int value =
            EncodedTokenAttributes.set(0, 1, OptionalStandardTokenType.REGEX, false, UNDERLINE | BOLD, 101, 102);
        assertEquals(value, 1, StandardTokenType.REGEX, false, UNDERLINE | BOLD, 101, 102);

        value = EncodedTokenAttributes.set(value, 0, OptionalStandardTokenType.COMMENT, false, NOT_SET, 0, 0);
        assertEquals(value, 1, StandardTokenType.COMMENT, false, UNDERLINE | BOLD, 101, 102);
    }

    @Test
    @Order(4)
    @DisplayName("StackElementMetadata can overwrite font style")
    public void testCanOverwriteFontStyle() {
        int value =
            EncodedTokenAttributes.set(0, 1, OptionalStandardTokenType.REGEX, false, UNDERLINE | BOLD, 101, 102);
        assertEquals(value, 1, StandardTokenType.REGEX, false, UNDERLINE | BOLD, 101, 102);

        value = EncodedTokenAttributes.set(value, 0, OptionalStandardTokenType.NOT_SET, false, NONE, 0, 0);
        assertEquals(value, 1, StandardTokenType.REGEX, false, NONE, 101, 102);
    }

    @Test
    @Order(5)
    @DisplayName("StackElementMetadata can overwrite font style with strikethrough")
    public void testCanOverwriteFontStyleWithStrikethrough() {
        int value = EncodedTokenAttributes.set(0, 1, OptionalStandardTokenType.REGEX, false, STRIKETHROUGH, 101, 102);
        assertEquals(value, 1, StandardTokenType.REGEX, false, STRIKETHROUGH, 101, 102);

        value = EncodedTokenAttributes.set(value, 0, OptionalStandardTokenType.NOT_SET, false, NONE, 0, 0);
        assertEquals(value, 1, StandardTokenType.REGEX, false, NONE, 101, 102);
    }

    @Test
    @Order(6)
    @DisplayName("StackElementMetadata can overwrite foreground")
    public void testCanOverwriteForeground() {
        int value =
            EncodedTokenAttributes.set(0, 1, OptionalStandardTokenType.REGEX, false, UNDERLINE | BOLD, 101, 102);
        assertEquals(value, 1, StandardTokenType.REGEX, false, UNDERLINE | BOLD, 101, 102);

        value = EncodedTokenAttributes.set(value, 0, OptionalStandardTokenType.NOT_SET, false, NOT_SET, 5, 0);
        assertEquals(value, 1, StandardTokenType.REGEX, false, UNDERLINE | BOLD, 5, 102);
    }

    @Test
    @Order(7)
    @DisplayName("StackElementMetadata can overwrite background")
    public void testCanOverwriteBackground() {
        int value =
            EncodedTokenAttributes.set(0, 1, OptionalStandardTokenType.REGEX, false, UNDERLINE | BOLD, 101, 102);
        assertEquals(value, 1, StandardTokenType.REGEX, false, UNDERLINE | BOLD, 101, 102);

        value = EncodedTokenAttributes.set(value, 0, OptionalStandardTokenType.NOT_SET, false, NOT_SET, 0, 7);
        assertEquals(value, 1, StandardTokenType.REGEX, false, UNDERLINE | BOLD, 101, 7);
    }

    @Test
    @Order(8)
    @DisplayName("StackElementMetadata can overwrite balanced bracket bit")
    public void testCanOverwriteBalancedBracketBit() {
        int value =
            EncodedTokenAttributes.set(0, 1, OptionalStandardTokenType.REGEX, false, UNDERLINE | BOLD, 101, 102);
        assertEquals(value, 1, StandardTokenType.REGEX, false, UNDERLINE | BOLD, 101, 102);

        value = EncodedTokenAttributes.set(value, 0, OptionalStandardTokenType.NOT_SET, true, NOT_SET, 0, 0);
        assertEquals(value, 1, StandardTokenType.REGEX, true, UNDERLINE | BOLD, 101, 102);

        value = EncodedTokenAttributes.set(value, 0, OptionalStandardTokenType.NOT_SET, false, NOT_SET, 0, 0);
        assertEquals(value, 1, StandardTokenType.REGEX, false, UNDERLINE | BOLD, 101, 102);
    }

    @Test
    @Order(9)
    @DisplayName("StackElementMetadata can work at max values")
    public void testCanWorkAtMaxValues() {
        int maxLangId = 255;
        int maxTokenType =
            StandardTokenType.COMMENT | StandardTokenType.OTHER | StandardTokenType.REGEX | StandardTokenType.STRING;
        int maxFontStyle = BOLD | ITALIC | UNDERLINE;
        int maxForeground = 511;
        int maxBackground = 254;

        int value =
            EncodedTokenAttributes.set(0, maxLangId, maxTokenType, true, maxFontStyle, maxForeground, maxBackground);
        assertEquals(value, maxLangId, maxTokenType, true, maxFontStyle, maxForeground, maxBackground);
    }

    //*************************************************************************

    static void assertEquals(int metadata,
                             int languageId,
                             int /*StandardTokenType*/ tokenType,
                             boolean containsBalancedBrackets,
                             int /*FontStyle*/ fontStyle,
                             int foreground,
                             int background) {
        var actual = "{\n"
                     + "languageId: " + EncodedTokenAttributes.getLanguageId(metadata) + ",\n"
                     + "tokenType: " + EncodedTokenAttributes.getTokenType(metadata) + ",\n"
                     + "containsBalancedBrackets: " + EncodedTokenAttributes.containsBalancedBrackets(metadata) + ",\n"
                     + "fontStyle: " + EncodedTokenAttributes.getFontStyle(metadata) + ",\n"
                     + "foreground: " + EncodedTokenAttributes.getForeground(metadata) + ",\n"
                     + "background: " + EncodedTokenAttributes.getBackground(metadata) + ",\n"
                     + "}";

        var expected = "{\n"
                       + "languageId: " + languageId + ",\n"
                       + "tokenType: " + tokenType + ",\n"
                       + "containsBalancedBrackets: " + containsBalancedBrackets + ",\n"
                       + "fontStyle: " + fontStyle + ",\n"
                       + "foreground: " + foreground + ",\n"
                       + "background: " + background + ",\n"
                       + "}";

        assertThat(actual)
            .as("equals for " + EncodedTokenAttributes.toBinaryStr(metadata))
            .isEqualTo(expected);
    }
}
