/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.theme;

import static tm4java.internal.utils.CollectionUtils.findFirst;
import static tm4java.internal.utils.StringUtils.strArrCmp;
import static tm4java.internal.utils.StringUtils.strCmp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.jspecify.annotations.Nullable;
import tm4java.internal.grammar.ScopeStack;
import tm4java.theme.IRawTheme;
import tm4java.theme.IRawThemeSetting;
import tm4java.internal.utils.StringUtils;
import tm4java.theme.ITheme;
import tm4java.theme.RGB;
import tm4java.theme.StyleAttributes;

/**
 * Represents a TextMate theme.
 * <p>
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/theme.ts#L7">
 * vscode-textmate/src/theme.ts#Theme#L7</a>
 * @see <a href="https://github.com/microsoft/vscode/blob/1.102.2/src/vs/editor/common/languages/supports/tokenization.ts#L192">
 * vscode/src/tokenization.ts#TokenTheme#L192</a>
 */
public final class Theme implements ITheme {

    private final transient ConcurrentMap<String/*scopeName*/, List<ThemeTrieElementRule>> cachedMatchRoot =
        new ConcurrentHashMap<>();

    private final ColorMap colorMap;
    private final StyleAttributes defaults;
    private final ThemeTrieElement root;
    private Map<String, String> editorColors = Collections.emptyMap(); // not from upstream

    public Theme(ColorMap colorMap,
                 StyleAttributes defaults,
                 ThemeTrieElement root) {
        this.colorMap = colorMap;
        this.defaults = defaults;
        this.root = root;
    }

    @Override
    public @Nullable StyleAttributes match(String scope) {
        return match(ScopeStack.from(scope));
    }

    public @Nullable StyleAttributes match(@Nullable ScopeStack scopePath) {
        if (scopePath == null) {
            return defaults;
        }
        var scopeName = scopePath.scopeName();
        var matchingTrieElements = cachedMatchRoot.computeIfAbsent(scopeName, root::match);

        var effectiveRule = findFirst(
            matchingTrieElements,
            v -> scopePathMatchesParentScopes(scopePath.parent(), v.parentScopes)
        );
        if (effectiveRule == null) {
            return null;
        }

        return StyleAttributes.of(
            effectiveRule.fontStyle,
            effectiveRule.foreground,
            effectiveRule.background
        );
    }

    public ThemeTrieElement root() {
        return root;
    }

    @Override
    public List<String> getColorMap() {
        return colorMap.getColorMap();
    }

    @Override
    public StyleAttributes getDefaults() {
        return defaults;
    }

    @Override
    public Map<String, String> getEditorColors() { // not from upstream
        return editorColors;
    }

    @Override
    public int hashCode() {
        int result = 31 + colorMap.hashCode();
        result = 31 * result + defaults.hashCode();
        return 31 * result + root.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Theme other) {
            return Objects.equals(colorMap, other.colorMap)
                   && Objects.equals(defaults, other.defaults)
                   && Objects.equals(root, other.root);
        }
        return false;
    }

    //*************************************************************************

    private boolean scopePathMatchesParentScopes(@Nullable ScopeStack scopePath,
                                                 List<String> parentScopeNames) {
        if (parentScopeNames.isEmpty()) {
            return true;
        }

        // starting with the deepest parent scope, look for a match in the scope path
        var parentScopeNamesLen = parentScopeNames.size();
        for (int index = 0; index < parentScopeNamesLen; index++) {
            var scopePattern = parentScopeNames.get(index);
            boolean scopeMustMatch = false;

            // check for a child combinator (a parent-child relationship)
            if (">".equals(scopePattern)) {
                if (index == parentScopeNamesLen - 1) {
                    // invalid use of child combinator
                    return false;
                }
                scopePattern = parentScopeNames.get(++index);
                scopeMustMatch = true;
            }

            while (scopePath != null) {
                if (matchesScope(scopePath.scopeName(), scopePattern)) {
                    break;
                }
                if (scopeMustMatch) {
                    // if a child combinator was used, the parent scope must match
                    return false;
                }
                scopePath = scopePath.parent();
            }

            if (scopePath == null) {
                // no more potential matches
                return false;
            }
            scopePath = scopePath.parent();
        }

        // all parent scopes were matched
        return true;
    }

    private boolean matchesScope(String scopeName, String scopeNamePattern) {
        return scopeNamePattern.equals(scopeName)
               || scopeName.startsWith(scopeNamePattern)
                  && scopeName.charAt(scopeNamePattern.length()) == '.';
    }

    //*************************************************************************

    /**
     * Parse a raw theme into rules.
     */
    public static List<ParsedThemeRule> parseTheme(@Nullable IRawTheme source) {
        if (source == null) {
            return Collections.emptyList();
        }

        var settings = source.getSettings();
        if (settings == null) {
            return Collections.emptyList();
        }

        var result = new ArrayList<ParsedThemeRule>();
        int i = -1;
        for (IRawThemeSetting entry : settings) {
            var entrySetting = entry.getSetting();
            if (entrySetting == null) {
                continue;
            }

            i++;

            Object settingScope = entry.getScope();
            List<String> scopes;
            if (settingScope instanceof String scope) {
                scope = scope.replaceAll("^,+", ""); // remove leading commas
                scope = scope.replaceAll(",+$", ""); // remove trailing commas
                scopes = StringUtils.splitToList(scope, ',');
            } else if (settingScope instanceof List) {
                @SuppressWarnings("unchecked")
                var settingScopes = (List<String>) settingScope;
                scopes = settingScopes;
            } else {
                scopes = List.of("");
            }

            int fontStyle = FontStyle.NOT_SET;
            String settingsFontStyle = entrySetting.getFontStyle();
            if (settingsFontStyle != null) {
                fontStyle = FontStyle.NONE;

                var segments = StringUtils.splitToArray(settingsFontStyle, ' ');
                for (var segment : segments) {
                    fontStyle = switch (segment) {
                        case "italic" -> fontStyle | FontStyle.ITALIC;
                        case "bold" -> fontStyle | FontStyle.BOLD;
                        case "underline" -> fontStyle | FontStyle.UNDERLINE;
                        case "strikethrough" -> fontStyle | FontStyle.STRIKETHROUGH;
                        default -> fontStyle;
                    };
                }
            }

            String foreground = null;
            if (entrySetting.getForeground() instanceof String fg && RGB.isValidHexColor(fg)) {
                foreground = fg;
            }

            String background = null;
            if (entrySetting.getBackground() instanceof String bg && RGB.isValidHexColor(bg)) {
                background = bg;
            }

            for (String s : scopes) {
                var segments = StringUtils.splitToList(s.trim(), ' ');
                var scope = segments.getLast();

                List<String> parentScopes = null;
                if (segments.size() > 1) {
                    parentScopes = new ArrayList<>(segments.subList(0, segments.size() - 1));
                    Collections.reverse(parentScopes);
                }

                result.add(new ParsedThemeRule(
                    scope,
                    parentScopes,
                    i,
                    fontStyle,
                    foreground,
                    background
                ));
            }
        }

        return result;
    }

    /**
     * Resolve rules (i.e. inheritance).
     */
    public static Theme resolveParsedThemeRules(List<ParsedThemeRule> parsedThemeRules,
                                                @Nullable List<String> colorMap) {
        // copy the list since we cannot be sure the given list is mutable
        var parsedThemeRules_ = new ArrayList<>(parsedThemeRules);

        // sort rules lexicographically, and then by index if necessary
        parsedThemeRules_.sort((a, b) -> {
            int r = strCmp(a.scope(), b.scope());
            if (r != 0) {
                return r;
            }
            r = strArrCmp(a.parentScopes(), b.parentScopes());
            if (r != 0) {
                return r;
            }
            return a.index() - b.index();
        });

        // determine defaults
        int defaultFontStyle = FontStyle.NONE;
        String defaultForeground = "#000000";
        String defaultBackground = "#ffffff";
        while (!parsedThemeRules_.isEmpty() && parsedThemeRules_.getFirst().scope().isEmpty()) {
            var incomingDefaults = parsedThemeRules_.removeFirst();
            if (incomingDefaults.fontStyle() != FontStyle.NOT_SET) {
                defaultFontStyle = incomingDefaults.fontStyle();
            }
            if (incomingDefaults.foreground() != null) {
                defaultForeground = incomingDefaults.foreground();
            }
            if (incomingDefaults.background() != null) {
                defaultBackground = incomingDefaults.background();
            }
        }
        var colorMap_ = new ColorMap(colorMap);
        var defaults = StyleAttributes.of(
            defaultFontStyle,
            colorMap_.getId(defaultForeground),
            colorMap_.getId(defaultBackground)
        );

        var root = new ThemeTrieElement(
            new ThemeTrieElementRule(0, null, FontStyle.NOT_SET, 0, 0), Collections.emptyList()
        );

        for (ParsedThemeRule rule : parsedThemeRules_) {
            root.insert(
                0,
                rule.scope(),
                rule.parentScopes(),
                rule.fontStyle(),
                colorMap_.getId(rule.foreground()),
                colorMap_.getId(rule.background())
            );
        }

        return new Theme(colorMap_, defaults, root);
    }

    public static Theme createFromRawTheme(@Nullable IRawTheme source,
                                           @Nullable List<String> colorMap) {
        var theme = createFromParsedTheme(parseTheme(source), colorMap);

        // not from upstream
        if (source != null) {
            theme.editorColors = source.getEditorColors();
        }

        return theme;
    }

    public static Theme createFromParsedTheme(List<ParsedThemeRule> source, @Nullable List<String> colorMap) {
        return resolveParsedThemeRules(source, colorMap);
    }
}
