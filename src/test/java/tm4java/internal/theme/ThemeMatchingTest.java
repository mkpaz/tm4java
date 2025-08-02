/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.theme;

import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import tm4java.internal.grammar.ScopeStack;
import tm4java.theme.StyleAttributes;

/**
 * @see <a href="https://github.com/microsoft/vscode-textmate/blob/v9.2.0/src/tests/themes.test.ts#L126">
 * vscode-textmate/tests/themes.test.ts</a>
 */
@NullMarked
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ThemeMatchingTest extends AbstractThemeTest {

    @Test
    @Order(1)
    @DisplayName("Theme matching gives higher priority to deeper matches")
    public void testGivesHigherPriorityToDeeperMatches() {
        Theme theme = createTheme("""
            {"settings": [
            	{ "settings": { "foreground": "#100000", "background": "#200000" } },
            	{ "scope": "punctuation.definition.string.begin.html", "settings": { "foreground": "#300000" } },
            	{ "scope": "meta.tag punctuation.definition.string", "settings": { "foreground": "#400000" } }
            ]}"""
        );

        StyleAttributes matched = theme.match(
            ScopeStack.from("punctuation.definition.string.begin.html")
        );

        assertThat(matched).isNotNull();
        assertThat(theme.getColorMap().get(matched.foregroundId())).isEqualTo("#300000");
    }

    @Test
    @Order(2)
    @DisplayName("Theme matching gives higher priority to parent matches 1")
    public void testGivesHigherPriorityToParentMatches1() {
        Theme theme = createTheme("""
            {"settings": [
            	{ "settings": { "foreground": "#100000", "background": "#200000" } },
            	{ "scope": "c a", "settings": { "foreground": "#300000" } },
            	{ "scope": "d a.b", "settings": { "foreground": "#400000" } },
            	{ "scope": "a", "settings": { "foreground": "#500000" } }
            ]}"""
        );

        List<String> colorMap = theme.getColorMap();
        StyleAttributes matched = theme.match(ScopeStack.from("d", "a.b"));

        assertThat(matched).isNotNull();
        assertThat(colorMap.get(matched.foregroundId())).isEqualTo("#400000");
    }

    @Test
    @Order(3)
    @DisplayName("Theme matching gives higher priority to parent matches 2")
    public void testGivesHigherPriorityToParentMatches2() {
        Theme theme = createTheme("""
            {"settings": [
            	{ "settings": { "foreground": "#100000", "background": "#200000" } },
            	{ "scope": "meta.tag entity", "settings": { "foreground": "#300000" } },
            	{ "scope": "meta.selector.css entity.name.tag", "settings": { "foreground": "#400000" } },
            	{ "scope": "entity", "settings": { "foreground": "#500000" } }
            ]}"""
        );

        List<String> colorMap = theme.getColorMap();
        StyleAttributes matched = theme.match(
            ScopeStack.from(
                "text.html.cshtml",
                "meta.tag.structure.any.html",
                "entity.name.tag.structure.any.html"
            )
        );

        assertThat(matched).isNotNull();
        assertThat(colorMap.get(matched.foregroundId())).isEqualTo("#300000");
    }

    @Test
    @Order(4)
    @DisplayName("Theme matching can match")
    public void testCanMatch() {
        Theme theme = createTheme("""
            {"settings": [
            	{ "settings": { "foreground": "#F8F8F2", "background": "#272822" } },
            	{ "scope": "source, something", "settings": { "background": "#100000" } },
            	{ "scope": ["bar", "baz"], "settings": { "background": "#200000" } },
            	{ "scope": "source.css selector bar", "settings": { "fontStyle": "bold" } },
            	{ "scope": "constant", "settings": { "fontStyle": "italic", "foreground": "#300000" } },
            	{ "scope": "constant.numeric", "settings": { "foreground": "#400000" } },
            	{ "scope": "constant.numeric.hex", "settings": { "fontStyle": "bold" } },
            	{ "scope": "constant.numeric.oct", "settings": { "fontStyle": "bold italic underline" } },
            	{ "scope": "constant.numeric.dec", "settings": { "fontStyle": "", "foreground": "#500000" } },
            	{ "scope": "storage.object.bar", "settings": { "fontStyle": "", "foreground": "#600000" } }
            ]}"""
        );

        // simpleMatch1..25
        assertThat(match(theme, "source"))
            .isEqualTo(map("background", "#100000", "fontStyle", "not set"));
        assertThat(match(theme, "source"))
            .isEqualTo(map("background", "#100000", "fontStyle", "not set"));
        assertThat(match(theme, "source.ts"))
            .isEqualTo(map("background", "#100000", "fontStyle", "not set"));
        assertThat(match(theme, "source.tss"))
            .isEqualTo(map("background", "#100000", "fontStyle", "not set"));
        assertThat(match(theme, "something"))
            .isEqualTo(map("background", "#100000", "fontStyle", "not set"));
        assertThat(match(theme, "something.ts"))
            .isEqualTo(map("background", "#100000", "fontStyle", "not set"));
        assertThat(match(theme, "something.tss"))
            .isEqualTo(map("background", "#100000", "fontStyle", "not set"));
        assertThat(match(theme, "baz"))
            .isEqualTo(map("background", "#200000", "fontStyle", "not set"));
        assertThat(match(theme, "baz.ts"))
            .isEqualTo(map("background", "#200000", "fontStyle", "not set"));
        assertThat(match(theme, "baz.tss"))
            .isEqualTo(map("background", "#200000", "fontStyle", "not set"));
        assertThat(match(theme, "constant"))
            .isEqualTo(map("foreground", "#300000", "fontStyle", "italic"));
        assertThat(match(theme, "constant.string"))
            .isEqualTo(map("foreground", "#300000", "fontStyle", "italic"));
        assertThat(match(theme, "constant.hex"))
            .isEqualTo(map("foreground", "#300000", "fontStyle", "italic"));
        assertThat(match(theme, "constant.numeric"))
            .isEqualTo(map("foreground", "#400000", "fontStyle", "italic"));
        assertThat(match(theme, "constant.numeric.baz"))
            .isEqualTo(map("foreground", "#400000", "fontStyle", "italic"));
        assertThat(match(theme, "constant.numeric.hex"))
            .isEqualTo(map("foreground", "#400000", "fontStyle", "bold"));
        assertThat(match(theme, "constant.numeric.hex.baz"))
            .isEqualTo(map("foreground", "#400000", "fontStyle", "bold"));
        assertThat(match(theme, "constant.numeric.oct"))
            .isEqualTo(map("foreground", "#400000", "fontStyle", "italic bold underline"));
        assertThat(match(theme, "constant.numeric.oct.baz"))
            .isEqualTo(map("foreground", "#400000", "fontStyle", "italic bold underline"));
        assertThat(match(theme, "constant.numeric.dec"))
            .isEqualTo(map("foreground", "#500000", "fontStyle", "none"));
        assertThat(match(theme, "constant.numeric.dec.baz"))
            .isEqualTo(map("foreground", "#500000", "fontStyle", "none"));
        assertThat(match(theme, "storage.object.bar"))
            .isEqualTo(map("foreground", "#600000", "fontStyle", "none"));
        assertThat(match(theme, "storage.object.bar.baz"))
            .isEqualTo(map("foreground", "#600000", "fontStyle", "none"));
        assertThat(match(theme, "storage.object.bart"))
            .isEqualTo(map("fontStyle", "not set"));
        assertThat(match(theme, "storage.object"))
            .isEqualTo(map("fontStyle", "not set"));
        assertThat(match(theme, "storage"))
            .isEqualTo(map("fontStyle", "not set"));

        // defaultMatch1..3
        assertThat(match(theme, ""))
            .isEqualTo(map("fontStyle", "not set"));
        assertThat(match(theme, "bazz"))
            .isEqualTo(map("fontStyle", "not set"));
        assertThat(match(theme, "asdfg"))
            .isEqualTo(map("fontStyle", "not set"));

        // multiMatch1..2
        assertThat(match(theme, "bar"))
            .isEqualTo(map("background", "#200000", "fontStyle", "not set"));
        assertThat(match(theme, "source.css", "selector", "bar"))
            .isEqualTo(map("background", "#200000", "fontStyle", "bold"));
    }

    @Test
    @Order(5)
    @DisplayName("Theme matching Microsoft/vscode#23460")
    public void testMicrosoft_vscode_23460() {
        Theme theme = createTheme("""
            {"settings": [
            	{
            		"settings": {
            			"foreground": "#aec2e0",
            			"background": "#14191f"
            		}
            	}, {
            		"name": "JSON String",
            		"scope": "meta.structure.dictionary.json string.quoted.double.json",
            		"settings": {
            			"foreground": "#FF410D"
            		}
            	}, {
            		"scope": "meta.structure.dictionary.json string.quoted.double.json",
            		"settings": {
            			"foreground": "#ffffff"
            		}
            	}, {
            		"scope": "meta.structure.dictionary.value.json string.quoted.double.json",
            		"settings": {
            			"foreground": "#FF410D"
            		}
            	}
            ]}"""
        );

        StyleAttributes matched = theme.match(
            ScopeStack.from(
                "source.json",
                "meta.structure.dictionary.json",
                "meta.structure.dictionary.value.json",
                "string.quoted.double.json"
            )
        );

        assertThat(matched).isNotNull();
        assertThat(theme.getColorMap().get(matched.foregroundId())).isEqualTo("#FF410D");
    }

    //*************************************************************************

    @Nullable
    Map<String, String> match(Theme theme, String... path) {
        var map = theme.getColorMap();

        var result = theme.match(ScopeStack.from(path));
        if (result == null) {
            return null;
        }

        var obj = map("fontStyle", fontStyleToString(result.fontStyle()));
        if (result.foregroundId() != 0) {
            obj.put("foreground", map.get(result.foregroundId()));
        }
        if (result.backgroundId() != 0) {
            obj.put("background", map.get(result.backgroundId()));
        }

        return obj;
    }

    static String fontStyleToString(int fontStyle) {
        if (fontStyle == FontStyle.NOT_SET) {
            return "not set";
        }
        if (fontStyle == FontStyle.NONE) {
            return "none";
        }

        var sb = new StringBuilder();
        if (FontStyle.isItalic(fontStyle)) {
            sb.append("italic ");
        }
        if (FontStyle.isBold(fontStyle)) {
            sb.append("bold ");
        }
        if (FontStyle.isUnderline(fontStyle)) {
            sb.append("underline ");
        }
        if (FontStyle.isStrikethrough(fontStyle)) {
            sb.append("strikethrough ");
        }

        if (sb.isEmpty()) {
            return "none";
        }

        // remove trailing space
        sb.setLength(sb.length() - 1);

        return sb.toString();
    }
}
