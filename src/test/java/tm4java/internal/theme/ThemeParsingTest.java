/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.theme;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.*;

import java.util.List;

import static tm4java.internal.theme.FontStyle.*;

/**
 * @see <a href="https://github.com/microsoft/vscode-textmate/blob/v9.2.0/src/tests/themes.test.ts#L286">
 * vscode-textmate/tests/themes.test.ts</a>
 */
@NullMarked
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ThemeParsingTest extends AbstractThemeTest {

    @Test
    @Order(1)
    @DisplayName("Theme parsing can parse")
    public void testCanParse() {
        List<ParsedThemeRule> actual = parseTheme("""
            { "settings": [
            { "settings": { "foreground": "#F8F8F2", "background": "#272822" } },
            { "scope": "source, something", "settings": { "background": "#100000" } },
            { "scope": ["bar", "baz"], "settings": { "background": "#010000" } },
            { "scope": "source.css selector bar", "settings": { "fontStyle": "bold" } },
            { "scope": "constant", "settings": { "fontStyle": "italic", "foreground": "#ff0000" } },
            { "scope": "constant.numeric", "settings": { "foreground": "#00ff00" } },
            { "scope": "constant.numeric.hex", "settings": { "fontStyle": "bold" } },
            { "scope": "constant.numeric.oct", "settings": { "fontStyle": "bold italic underline" } },
            { "scope": "constant.numeric.bin", "settings": { "fontStyle": "bold strikethrough" } },
            { "scope": "constant.numeric.dec", "settings": { "fontStyle": "", "foreground": "#0000ff" } },
            { "scope": "foo", "settings": { "fontStyle": "", "foreground": "#CFA" } }
            ]}"""
        );

        var expected = List.of(
            new ParsedThemeRule("", null, 0, FontStyle.NOT_SET, "#F8F8F2", "#272822"),
            new ParsedThemeRule("source", null, 1, FontStyle.NOT_SET, null, "#100000"),
            new ParsedThemeRule("something", null, 1, FontStyle.NOT_SET, null, "#100000"),
            new ParsedThemeRule("bar", null, 2, FontStyle.NOT_SET, null, "#010000"),
            new ParsedThemeRule("baz", null, 2, FontStyle.NOT_SET, null, "#010000"),
            new ParsedThemeRule("bar", List.of("selector", "source.css"), 3, BOLD, null, null),
            new ParsedThemeRule("constant", null, 4, ITALIC, "#ff0000", null),
            new ParsedThemeRule("constant.numeric", null, 5, FontStyle.NOT_SET, "#00ff00", null),
            new ParsedThemeRule("constant.numeric.hex", null, 6, BOLD, null, null),
            new ParsedThemeRule("constant.numeric.oct", null, 7, BOLD | ITALIC | UNDERLINE, null, null),
            new ParsedThemeRule("constant.numeric.bin", null, 8, BOLD | STRIKETHROUGH, null, null),
            new ParsedThemeRule("constant.numeric.dec", null, 9, NONE, "#0000ff", null),
            new ParsedThemeRule("foo", null, 10, NONE, "#CFA", null)
        );

        assertThat(actual).isEqualTo(expected);
    }
}
