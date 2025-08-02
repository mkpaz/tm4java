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
import org.junit.jupiter.api.Test;
import tm4java.Resources;
import tm4java.internal.grammar.ScopeStack;
import tm4java.parser.ContentType;
import tm4java.theme.IThemeSource;

import static org.assertj.core.api.Assertions.assertThat;
import static tm4java.internal.utils.NullSafetyHelper.castNonNull;

@NullMarked
public class ThemeTypeTest {

    @Test
    public void testTMPlistTheme() {
        var rawTheme = RawTheme.read(
            IThemeSource.fromFile(Resources.getFile("/tm4java/test-cases/themes/QuietLight.tmTheme"))
        );
        assertThat(rawTheme.getName()).isEqualTo("Quiet Light");

        var theme = Theme.createFromRawTheme(rawTheme, null);
        var colors = theme.getColorMap();

        var editorColors = rawTheme.getEditorColors();
        assertThat(editorColors.get("background")).isEqualTo("#F5F5F5");
        assertThat(editorColors.get("caret")).isEqualTo("#000000");
        assertThat(editorColors.get("foreground")).isEqualTo("#333333");
        assertThat(editorColors.get("invisibles")).isEqualTo("#AAAAAA");
        assertThat(editorColors.get("lineHighlight")).isEqualTo("#E4F6D4");
        assertThat(editorColors.get("selection")).isEqualTo("#C9D0D9");

        var attrs = castNonNull(theme.match(ScopeStack.from()));
        assertThat(colors.get(attrs.foregroundId())).isEqualTo("#333333");
        assertThat(colors.get(attrs.backgroundId())).isEqualTo("#F5F5F5");

        attrs = castNonNull(theme.match(ScopeStack.from("comment")));
        assertThat(colors.get(attrs.foregroundId())).isEqualTo("#AAAAAA");
        assertThat(attrs.fontStyle() & FontStyle.ITALIC).isEqualTo(FontStyle.ITALIC);

        attrs = castNonNull(theme.match(ScopeStack.from("punctuation.definition.comment")));
        assertThat(colors.get(attrs.foregroundId())).isEqualTo("#AAAAAA");
        assertThat(attrs.fontStyle() & FontStyle.ITALIC).isEqualTo(FontStyle.ITALIC);

        attrs = castNonNull(theme.match(ScopeStack.from("keyword")));
        assertThat(colors.get(attrs.foregroundId())).isEqualTo("#4B83CD");
        attrs = castNonNull(theme.match(ScopeStack.from("keyword.operator")));
        assertThat(colors.get(attrs.foregroundId())).isEqualTo("#777777");
    }

    @Test
    public void testTMJsonTheme() {
        var rawTheme = RawTheme.read(
            IThemeSource.fromFile(Resources.getFile("/tm4java/test-cases/themes/dark_vs.json"))
        );
        assertThat(rawTheme.getName()).isEqualTo("Dark Visual Studio");

        var theme = Theme.createFromRawTheme(rawTheme, null);
        var colors = theme.getColorMap();

        var editorColors = rawTheme.getEditorColors();
        assertThat(editorColors.get("foreground")).isEqualTo("#D4D4D4");
        assertThat(editorColors.get("background")).isEqualTo("#1E1E1E");

        var attrs = castNonNull(theme.match(ScopeStack.from()));
        assertThat(colors.get(attrs.foregroundId())).isEqualTo("#D4D4D4");
        assertThat(colors.get(attrs.backgroundId())).isEqualTo("#1E1E1E");

        attrs = castNonNull(theme.match(ScopeStack.from("comment")));
        assertThat(colors.get(attrs.foregroundId())).isEqualTo("#608B4E");
        assertThat(attrs.fontStyle() & FontStyle.ITALIC).isEqualTo(FontStyle.ITALIC);

        attrs = castNonNull(theme.match(ScopeStack.from("keyword")));
        assertThat(colors.get(attrs.foregroundId())).isEqualTo("#569CD6");
        attrs = castNonNull(theme.match(ScopeStack.from("keyword.operator")));
        assertThat(colors.get(attrs.foregroundId())).isEqualTo("#D4D4D4");
        attrs = castNonNull(theme.match(ScopeStack.from("keyword.operator.expression")));
        assertThat(colors.get(attrs.foregroundId())).isEqualTo("#569CD6");
    }

    @Test
    public void testVSCodeJsonTheme() {
        var rawTheme = RawTheme.read(
            IThemeSource.fromString(ContentType.JSON, """
                {
                  "name": "My theme",
                  "tokenColors": [
                    {
                      "settings": {
                        "foreground": "#ABCDEF",
                        "background": "#012345"
                      }
                    },
                    {
                      "name": "Comment",
                      "scope": "comment",
                      "settings": {
                        "fontStyle": "italic",
                        "foreground": "#FF0000"
                      }
                    },
                    {
                      "name": "Keyword",
                      "scope": "keyword",
                      "settings": {
                        "foreground": "#00FF00"
                      }
                    }
                  ],
                  "colors": {
                    "editor.foreground": "#FFFFFF",
                    "editor.background": "#000000",
                    "editor.selectionForeground": "#EEEEEE",
                    "editor.selectionBackground": "#333333",
                    "editor.lineHighlightBackground": "#999999"
                  },
                  "semanticHighlighting": true
                }
                """
            )
        );
        assertThat(rawTheme.getName()).isEqualTo("My theme");

        var theme = Theme.createFromRawTheme(rawTheme, null);
        var colors = theme.getColorMap();

        var editorColors = rawTheme.getEditorColors();
        assertThat(editorColors.get("editor.foreground")).isEqualTo("#FFFFFF");
        assertThat(editorColors.get("editor.background")).isEqualTo("#000000");

        var attrs = castNonNull(theme.match(ScopeStack.from()));
        assertThat(colors.get(attrs.foregroundId())).isEqualTo("#ABCDEF");
        assertThat(colors.get(attrs.backgroundId())).isEqualTo("#012345");

        attrs = castNonNull(theme.match(ScopeStack.from("comment")));
        assertThat(colors.get(attrs.foregroundId())).isEqualTo("#FF0000");
        assertThat(attrs.fontStyle() & FontStyle.ITALIC).isEqualTo(FontStyle.ITALIC);

        attrs = castNonNull(theme.match(ScopeStack.from("keyword.something")));
        assertThat(colors.get(attrs.foregroundId())).isEqualTo("#00FF00");
    }
}
