/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.theme;


import static tm4java.internal.theme.FontStyle.BOLD;
import static tm4java.internal.theme.FontStyle.ITALIC;
import static tm4java.internal.theme.FontStyle.NONE;
import static tm4java.internal.theme.FontStyle.UNDERLINE;

import java.util.Arrays;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import tm4java.internal.grammar.ScopeStack;
import tm4java.internal.utils.Strings;
import tm4java.theme.StyleAttributes;

/**
 * @see <a href="https://github.com/microsoft/vscode-textmate/blob/v9.2.0/src/tests/themes.test.ts#L323">
 * vscode-textmate/tests/themes.test.ts</a>
 */
@NullMarked
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ThemeResolvingTest extends AbstractThemeTest {

    static final ThemeTrieElementRule NOT_SET_THEME_TRIE_ELEMENT_RULE =
        new ThemeTrieElementRule(0, null, FontStyle.NOT_SET, NOT_SET, NOT_SET);

    static final ThemeTrieElement NOT_SET_THEME_TRIE_ELEMENT =
        new ThemeTrieElement(NOT_SET_THEME_TRIE_ELEMENT_RULE);

    @Test
    @Order(1)
    @DisplayName("Theme resolving strcmp works")
    public void testStrCmpWorks() {
        var actual = Arrays.asList("bar", "z", "zu", "a", "ab", "");
        actual.sort(Strings::strCmp);

        var expected = List.of("", "a", "ab", "bar", "z", "zu");
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @Order(2)
    @DisplayName("Theme resolving strArrCmp works")
    public void testStrArrCmpWorks() {
        assertStrArrCmp("001", null, null, 0);
        assertStrArrCmp("002", null, List.of(), -1);
        assertStrArrCmp("003", null, List.of("a"), -1);
        assertStrArrCmp("004", List.of(), null, 1);
        assertStrArrCmp("005", List.of("a"), null, 1);
        assertStrArrCmp("006", List.of(), List.of(), 0);
        assertStrArrCmp("007", List.of(), List.of("a"), -1);
        assertStrArrCmp("008", List.of("a"), List.of(), 1);
        assertStrArrCmp("009", List.of("a"), List.of("a"), 0);
        assertStrArrCmp("010", List.of("a", "b"), List.of("a"), 1);
        assertStrArrCmp("011", List.of("a"), List.of("a", "b"), -1);
        assertStrArrCmp("012", List.of("a", "b"), List.of("a", "b"), 0);
        assertStrArrCmp("013", List.of("a", "b"), List.of("a", "c"), -1);
        assertStrArrCmp("014", List.of("a", "c"), List.of("a", "b"), 1);
    }

    @Test
    @Order(3)
    @DisplayName("Theme resolving always has defaults")
    public void testAlwaysHasDefaults() {
        var actual = createTheme();

        var colorMap = new ColorMap();
        int A = colorMap.getId("#000000");
        int B = colorMap.getId("#ffffff");
        var expected = new Theme(colorMap, StyleAttributes.of(NONE, A, B), NOT_SET_THEME_TRIE_ELEMENT);

        assertThemeEqual(actual, expected);
    }

    @Test
    @Order(4)
    @DisplayName("Theme resolving respects incoming defaults 1")
    public void testRespectsIncomingDefaults1() {
        var actual = createTheme(
            new ParsedThemeRule("", null, -1, FontStyle.NOT_SET, null, null)
        );

        var colorMap = new ColorMap();
        int A = colorMap.getId("#000000");
        int B = colorMap.getId("#ffffff");
        var expected = new Theme(colorMap, StyleAttributes.of(NONE, A, B), NOT_SET_THEME_TRIE_ELEMENT);

        assertThemeEqual(actual, expected);
    }

    @Test
    @Order(5)
    @DisplayName("Theme resolving respects incoming defaults 2")
    public void testRespectsIncomingDefaults2() {
        Theme actual = createTheme(
            new ParsedThemeRule("", null, -1, NONE, null, null)
        );

        var colorMap = new ColorMap();
        int A = colorMap.getId("#000000");
        int B = colorMap.getId("#ffffff");
        var expected = new Theme(colorMap, StyleAttributes.of(NONE, A, B), NOT_SET_THEME_TRIE_ELEMENT);

        assertThemeEqual(actual, expected);
    }

    @Test
    @Order(6)
    @DisplayName("Theme resolving respects incoming defaults 3")
    public void testRespectsIncomingDefaults3() {
        var actual = createTheme(
            new ParsedThemeRule("", null, -1, BOLD, null, null)
        );

        var colorMap = new ColorMap();
        int A = colorMap.getId("#000000");
        int B = colorMap.getId("#ffffff");
        var expected = new Theme(colorMap, StyleAttributes.of(BOLD, A, B), NOT_SET_THEME_TRIE_ELEMENT);

        assertThemeEqual(actual, expected);
    }

    @Test
    @Order(7)
    @DisplayName("Theme resolving respects incoming defaults 4")
    public void testRespectsIncomingDefaults4() {
        var actual = createTheme(
            new ParsedThemeRule("", null, -1, FontStyle.NOT_SET, "#ff0000", null)
        );

        var colorMap = new ColorMap();
        int A = colorMap.getId("#ff0000");
        int B = colorMap.getId("#ffffff");
        var expected = new Theme(colorMap, StyleAttributes.of(NONE, A, B), NOT_SET_THEME_TRIE_ELEMENT);

        assertThemeEqual(actual, expected);
    }

    @Test
    @Order(8)
    @DisplayName("Theme resolving respects incoming defaults 5")
    public void testRespectsIncomingDefaults5() {
        var actual = createTheme(
            new ParsedThemeRule("", null, -1, FontStyle.NOT_SET, null, "#ff0000")
        );

        var colorMap = new ColorMap();
        int A = colorMap.getId("#000000");
        int B = colorMap.getId("#ff0000");
        var expected = new Theme(colorMap, StyleAttributes.of(NONE, A, B), NOT_SET_THEME_TRIE_ELEMENT);

        assertThemeEqual(actual, expected);
    }

    @Test
    @Order(9)
    @DisplayName("Theme resolving can merge incoming defaults")
    public void testCanMergeIncomingDefaults() {
        var actual = createTheme(
            new ParsedThemeRule("", null, -1, FontStyle.NOT_SET, null, "#ff0000"),
            new ParsedThemeRule("", null, -1, FontStyle.NOT_SET, "#00ff00", null),
            new ParsedThemeRule("", null, -1, BOLD, null, null)
        );

        var colorMap = new ColorMap();
        int A = colorMap.getId("#00ff00");
        int B = colorMap.getId("#ff0000");
        var expected = new Theme(colorMap, StyleAttributes.of(BOLD, A, B), NOT_SET_THEME_TRIE_ELEMENT);

        assertThemeEqual(actual, expected);
    }

    @Test
    @Order(10)
    @DisplayName("Theme resolving defaults are inherited")
    public void testDefaultsAreInherited() {
        Theme actual = createTheme(
            new ParsedThemeRule("", null, -1, FontStyle.NOT_SET, "#F8F8F2", "#272822"),
            new ParsedThemeRule("var", null, -1, FontStyle.NOT_SET, "#ff0000", null)
        );

        var colorMap = new ColorMap();
        int A = colorMap.getId("#F8F8F2");
        int B = colorMap.getId("#272822");
        int C = colorMap.getId("#ff0000");
        var expected = new Theme(
            colorMap,
            StyleAttributes.of(NONE, A, B),
            new ThemeTrieElement(
                NOT_SET_THEME_TRIE_ELEMENT_RULE,
                List.of(),
                map("var", new ThemeTrieElement(
                    new ThemeTrieElementRule(1, null, FontStyle.NOT_SET, C, NOT_SET)
                ))
            )
        );

        assertThemeEqual(actual, expected);
    }

    @Test
    @Order(11)
    @DisplayName("Theme resolving same rules get merged")
    public void testSameRulesGetMerged() {
        var actual = createTheme(
            new ParsedThemeRule("", null, -1, FontStyle.NOT_SET, "#F8F8F2", "#272822"),
            new ParsedThemeRule("var", null, 1, BOLD, null, null),
            new ParsedThemeRule("var", null, 0, FontStyle.NOT_SET, "#ff0000", null)
        );

        var colorMap = new ColorMap();
        int A = colorMap.getId("#F8F8F2");
        int B = colorMap.getId("#272822");
        int C = colorMap.getId("#ff0000");
        var expected = new Theme(
            colorMap,
            StyleAttributes.of(NONE, A, B),
            new ThemeTrieElement(NOT_SET_THEME_TRIE_ELEMENT_RULE,
                List.of(),
                map("var", new ThemeTrieElement(
                    new ThemeTrieElementRule(1, null, BOLD, C, NOT_SET)
                ))
            )
        );

        assertThemeEqual(actual, expected);
    }

    @Test
    @Order(12)
    @DisplayName("Theme resolving rules are inherited 1")
    public void testRulesAreInherited1() {
        var actual = createTheme(
            new ParsedThemeRule("", null, -1, FontStyle.NOT_SET, "#F8F8F2", "#272822"),
            new ParsedThemeRule("var", null, -1, BOLD, "#ff0000", null),
            new ParsedThemeRule("var.identifier", null, -1, FontStyle.NOT_SET, "#00ff00", null)
        );

        var colorMap = new ColorMap();
        int A = colorMap.getId("#F8F8F2");
        int B = colorMap.getId("#272822");
        int C = colorMap.getId("#ff0000");
        int D = colorMap.getId("#00ff00");
        var expected = new Theme(colorMap,
            StyleAttributes.of(NONE, A, B),
            new ThemeTrieElement(
                NOT_SET_THEME_TRIE_ELEMENT_RULE,
                List.of(),
                map("var", new ThemeTrieElement(
                    new ThemeTrieElementRule(1, null, BOLD, C, NOT_SET),
                    List.of(),
                    map("identifier", new ThemeTrieElement(
                        new ThemeTrieElementRule(2, null, BOLD, D, NOT_SET)
                    ))
                ))
            )
        );

        assertThemeEqual(actual, expected);
    }

    @Test
    @Order(13)
    @DisplayName("Theme resolving rules are inherited 2")
    public void testRulesAreInherited2() {
        var actual = createTheme(
            new ParsedThemeRule("", null, -1, FontStyle.NOT_SET, "#F8F8F2", "#272822"),
            new ParsedThemeRule("var", null, -1, BOLD, "#ff0000", null),
            new ParsedThemeRule("var.identifier", null, -1, FontStyle.NOT_SET, "#00ff00", null),
            new ParsedThemeRule("constant", null, 4, ITALIC, "#100000", null),
            new ParsedThemeRule("constant.numeric", null, 5, FontStyle.NOT_SET, "#200000", null),
            new ParsedThemeRule("constant.numeric.hex", null, 6, BOLD, null, null),
            new ParsedThemeRule("constant.numeric.oct", null, 7, BOLD | ITALIC | UNDERLINE, null, null),
            new ParsedThemeRule("constant.numeric.dec", null, 8, NONE, "#300000", null)
        );

        var colorMap = new ColorMap();
        int A = colorMap.getId("#F8F8F2");
        int B = colorMap.getId("#272822");
        int C = colorMap.getId("#100000");
        int D = colorMap.getId("#200000");
        int E = colorMap.getId("#300000");
        int F = colorMap.getId("#ff0000");
        int G = colorMap.getId("#00ff00");
        var expected = new Theme(
            colorMap,
            StyleAttributes.of(NONE, A, B),
            new ThemeTrieElement(
                NOT_SET_THEME_TRIE_ELEMENT_RULE,
                List.of(),
                map("var", new ThemeTrieElement(
                        new ThemeTrieElementRule(1, null, BOLD, F, NOT_SET),
                        List.of(),
                        map("identifier", new ThemeTrieElement(
                            new ThemeTrieElementRule(2, null, BOLD, G, NOT_SET)
                        ))
                    ),
                    "constant", new ThemeTrieElement(
                        new ThemeTrieElementRule(1, null, ITALIC, C, NOT_SET),
                        List.of(),
                        map("numeric", new ThemeTrieElement(
                            new ThemeTrieElementRule(2, null, ITALIC, D, NOT_SET),
                            List.of(),
                            map("hex", new ThemeTrieElement(
                                    new ThemeTrieElementRule(3, null, BOLD, D, NOT_SET)),
                                "oct", new ThemeTrieElement(
                                    new ThemeTrieElementRule(3, null, BOLD | ITALIC | UNDERLINE, D, NOT_SET)),
                                "dec", new ThemeTrieElement(new ThemeTrieElementRule(3, null, NONE, E, NOT_SET))
                            ))
                        ))
                )
            )
        );

        assertThemeEqual(actual, expected);
    }

    @Test
    @Order(14)
    @DisplayName("Theme resolving rules with parent scopes")
    public void testRulesWithParentScopes() {
        var actual = createTheme(
            new ParsedThemeRule("", null, -1, FontStyle.NOT_SET, "#F8F8F2", "#272822"),
            new ParsedThemeRule("var", null, -1, BOLD, "#100000", null),
            new ParsedThemeRule("var.identifier", null, -1, FontStyle.NOT_SET, "#200000", null),
            new ParsedThemeRule("var", List.of("source.css"), 1, ITALIC, "#300000", null),
            new ParsedThemeRule("var", List.of("source.css"), 2, UNDERLINE, null, null)
        );

        var colorMap = new ColorMap();
        int A = colorMap.getId("#F8F8F2");
        int B = colorMap.getId("#272822");
        int C = colorMap.getId("#100000");
        int D = colorMap.getId("#300000");
        int E = colorMap.getId("#200000");
        var expected = new Theme(
            colorMap,
            StyleAttributes.of(NONE, A, B),
            new ThemeTrieElement(
                NOT_SET_THEME_TRIE_ELEMENT_RULE,
                List.of(),
                map("var", new ThemeTrieElement(
                    new ThemeTrieElementRule(1, null, BOLD, C, NOT_SET),
                    List.of(new ThemeTrieElementRule(1, List.of("source.css"), UNDERLINE, D, NOT_SET)),
                    map("identifier", new ThemeTrieElement(
                        new ThemeTrieElementRule(2, null, BOLD, E, NOT_SET),
                        List.of(new ThemeTrieElementRule(1, List.of("source.css"), UNDERLINE, D, NOT_SET))
                    ))
                ))
            )
        );

        assertThemeEqual(actual, expected);
    }

    @Test
    @Order(15)
    @DisplayName("Theme resolving a rule with child combinator")
    public void testRuleWithChildCombinator() {
        var theme = createTheme("""
            {"settings": [
            	{ "settings": { "foreground": "#100000" } },
            	{ "scope": "b a",       "settings": { "foreground": "#200000" } },
            	{ "scope": "b > a",     "settings": { "foreground": "#300000" } },
            	{ "scope": "c > b > a", "settings": { "foreground": "#400000" } },
            	{ "scope": "a",         "settings": { "foreground": "#500000" } },
            ]}"""
        );

        var colorMap = theme.getColorMap();
        StyleAttributesMatcher matcher = (String[] path) -> {
            var matched = theme.match(ScopeStack.from(path));
            if (matched == null || matched.foregroundId() == 0) {
                return null;
            }
            return colorMap.get(matched.foregroundId());
        };

        assertThat(matcher.match("b", "a")).as("b a").isEqualTo("#300000");
        assertThat(matcher.match("b", "c", "a")).as("b c a").isEqualTo("#200000");
        assertThat(matcher.match("c", "b", "a")).as("c b a").isEqualTo("#400000");
        assertThat(matcher.match("c", "b", "d", "a")).as("c b d a").isEqualTo("#200000");
    }

    @Test
    @Order(16)
    @DisplayName("Theme resolving should give deeper scopes higher specificity (#233)")
    public void testGiveDeeperScopesHigherSpecificity() {
        var theme = createTheme("""
            {"settings": [
            	{ "settings": { "foreground": "#100000" } },
            	{ "scope": "y.z a.b", "settings": { "foreground": "#200000" } },
            	{ "scope": "x y a.b", "settings": { "foreground": "#300000" } },
            ]}"""
        );

        var colorMap = theme.getColorMap();
        StyleAttributesMatcher matcher = (String[] path) -> {
            var result = theme.match(ScopeStack.from(path));
            if (result == null || result.foregroundId() == 0) {
                return null;
            }
            return colorMap.get(result.foregroundId());
        };

        assertThat(matcher.match("x", "a.b")).as("x a.b").isNull();
        assertThat(matcher.match("y", "a.b")).as("y a.b").isNull();
        assertThat(matcher.match("y.z", "a")).as("y.z a").isNull();
        assertThat(matcher.match("x", "y", "a.b")).as("x y a.b").isEqualTo("#300000");

        // Even though the "x y a.b" rule has more scopes in its path,
        // the "y.z a.b" rule has a deeper match, so it should take precedence.
        assertThat(matcher.match("x", "y.z", "a.b")).as("y.z a.b").isEqualTo("#200000");
    }

    @Test
    @Order(17)
    @DisplayName("Theme resolving issue #38: ignores rules with invalid colors")
    public void testIssue_38_ignores_rules_with_invalid_colors() {
        var actual = parseTheme("""
            { "settings": [
            	{
            		"settings": {
            			"background": "#222222",
            			"foreground": "#cccccc"
            		}
            	}, {
            		"name": "Variable",
            		"scope": "variable",
            		"settings": {
            			"fontStyle": ""
            		}
            	}, {
            		"name": "Function argument",
            		"scope": "variable.parameter",
            		"settings": {
            			"fontStyle": "italic",
            			"foreground": ""
            		}
            	}, {
            		"name": "Library variable",
            		"scope": "support.other.variable",
            		"settings": {
            			"fontStyle": ""
            		}
            	}, {
            		"name": "Function argument",
            		"scope": "variable.other",
            		"settings": {
            			"foreground": "",
            			"fontStyle": "normal"
            		}
            	}, {
            		"name": "Coffeescript Function argument",
            		"scope": "variable.parameter.function.coffee",
            		"settings": {
            			"foreground": "#F9D423",
            			"fontStyle": "italic"
            		}
            	}
            ]}"""
        );

        var expected = List.of(
            new ParsedThemeRule("", null, 0, FontStyle.NOT_SET, "#cccccc", "#222222"),
            new ParsedThemeRule("variable", null, 1, NONE, null, null),
            new ParsedThemeRule("variable.parameter", null, 2, ITALIC, null, null),
            new ParsedThemeRule("support.other.variable", null, 3, NONE, null, null),
            new ParsedThemeRule("variable.other", null, 4, NONE, null, null),
            new ParsedThemeRule("variable.parameter.function.coffee", null, 5, ITALIC, "#F9D423", null)
        );

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @Order(18)
    @DisplayName("Theme resolving issue #35: Trailing comma in a tmTheme scope selector")
    public void testIssue_35_Trailing_comma_in_a_tmTheme_scope_selector() {
        var actual = parseTheme("""
            { "settings": [{
                    "settings": {
                        "background": "#25292C",
                        "foreground": "#EFEFEF"
                    }
                }, {
                    "name": "CSS at-rule keyword control",
                    "scope": "meta.at-rule.return.scss, meta.at-rule.return.scss punctuation.definition, meta.at-rule.else.scss, meta.at-rule.else.scss punctuation.definition, meta.at-rule.if.scss, meta.at-rule.if.scss punctuation.definition",
                    "settings": {
                        "foreground": "#CC7832"
                    }
                }
            ]}"""
        );

        var expected = List.of(
            new ParsedThemeRule("", null, 0, FontStyle.NOT_SET, "#EFEFEF", "#25292C"),
            new ParsedThemeRule("meta.at-rule.return.scss", null, 1, FontStyle.NOT_SET, "#CC7832", null),
            new ParsedThemeRule("punctuation.definition", List.of("meta.at-rule.return.scss"), 1, FontStyle.NOT_SET,
                "#CC7832", null),
            new ParsedThemeRule("meta.at-rule.else.scss", null, 1, FontStyle.NOT_SET, "#CC7832", null),
            new ParsedThemeRule("punctuation.definition", List.of("meta.at-rule.else.scss"), 1, FontStyle.NOT_SET,
                "#CC7832", null),
            new ParsedThemeRule("meta.at-rule.if.scss", null, 1, FontStyle.NOT_SET, "#CC7832", null),
            new ParsedThemeRule("punctuation.definition", List.of("meta.at-rule.if.scss"), 1, FontStyle.NOT_SET,
                "#CC7832", null)
        );

        assertThat(actual).isEqualTo(expected);
    }

    //*************************************************************************

    interface StyleAttributesMatcher {
        @Nullable
        String match(String... path);
    }

    static void assertThemeEqual(Theme actual, Theme expected) {
        // if this fails, we get a nice visual representation of the difference:
        assertThat(THEME_GSON.toJson(actual)).isEqualTo(THEME_GSON.toJson(expected));

        // this ensures hashCode/equals are properly implemented:
        assertThat(actual).isEqualTo(expected);
    }

    static void assertStrArrCmp(String testCase,
                                @Nullable List<String> a,
                                @Nullable List<String> b,
                                int expected) {
        assertThat(Strings.strArrCmp(a, b)).as(testCase).isEqualTo(expected);
    }
}
