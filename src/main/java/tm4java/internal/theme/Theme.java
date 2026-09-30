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
import tm4java.internal.grammar.ScopeStack;
import tm4java.internal.utils.Strings;
import tm4java.theme.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static tm4java.internal.utils.Iterables.findFirst;
import static tm4java.internal.utils.Strings.strArrCmp;
import static tm4java.internal.utils.Strings.strCmp;

/**
 * Concrete implementation of {@link ITheme} representing a compiled TextMate theme.
 *
 * <p>This class processes theme rules into a trie data structure ({@link ThemeTrieElement})
 * for scope prefix matching, caches matched rules per scope name, and resolves styling attributes
 * against a shared color map.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/theme.ts#L7">
 * vscode-textmate/src/theme.ts#Theme#L7</a>
 * @see <a href="https://github.com/microsoft/vscode/blob/1.102.2/src/vs/editor/common/languages/supports/tokenization.ts#L192">
 * vscode/src/tokenization.ts#TokenTheme#L192</a>
 */
public final class Theme implements ITheme {

    /** Concurrent match cache mapping a leaf scope name to candidate theme rules retrieved from the trie. */
    private final transient ConcurrentMap<String/*scopeName*/, List<ThemeTrieElementRule>> cachedMatchRoot =
        new ConcurrentHashMap<>();

    private final ColorMap colorMap;
    private final StyleAttributes defaults;
    private final ThemeTrieElement root;
    private Map<String, String> editorColors = Collections.emptyMap(); // not from the upstream

    /**
     * Constructs a compiled {@code Theme} instance.
     *
     * @param colorMap the color palette mapping hex values to integer IDs
     * @param defaults default style attributes to fall back on when no rule matches
     * @param root     the root node of the compiled theme rule trie
     */
    public Theme(ColorMap colorMap, StyleAttributes defaults, ThemeTrieElement root) {
        this.colorMap = colorMap;
        this.defaults = defaults;
        this.root = root;
    }

    /**
     * Returns the root node of the theme trie structure.
     *
     * @return the root {@link ThemeTrieElement}
     */
    public ThemeTrieElement root() {
        return root;
    }

    /**
     * Resolves the effective {@link StyleAttributes} for a scope stack path.
     *
     * @param scopePath the {@link ScopeStack} context representing the current scope chain
     * @return the resolved {@link StyleAttributes}, {@link #getDefaults()} if {@code scopePath} is {@code null},
     * or {@code null} if no matching rule is found in the trie
     */
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

        return StyleAttributes.of(effectiveRule.fontStyle, effectiveRule.foreground, effectiveRule.background);
    }

    @Override
    public @Nullable StyleAttributes match(String scope) {
        return match(ScopeStack.from(scope));
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
    public Map<String, String> getEditorColors() { // not from the upstream
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
                    return false; // invalid use of child combinator
                }
                scopePattern = parentScopeNames.get(++index);
                scopeMustMatch = true;
            }

            while (scopePath != null) {
                if (matchesScope(scopePath.scopeName(), scopePattern)) {
                    break;
                }
                if (scopeMustMatch) {
                    return false; // if a child combinator was used, the parent scope must match
                }
                scopePath = scopePath.parent();
            }

            if (scopePath == null) {
                return false; // no more potential matches
            }
            scopePath = scopePath.parent();
        }

        return true; // all parent scopes were matched
    }

    private boolean matchesScope(String scopeName, String scopeNamePattern) {
        return scopeNamePattern.equals(scopeName)
            || scopeName.startsWith(scopeNamePattern)
            && scopeName.charAt(scopeNamePattern.length()) == '.';
    }

    //*************************************************************************

    /**
     * Parses a raw theme data model into a flat list of uncompiled theme rules.
     *
     * @param source the raw theme definition loaded from JSON/Plist, or {@code null}
     * @return a list of parsed {@link ParsedThemeRule} entries, or an empty list
     * if {@code source} is {@code null}
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
                scopes = Strings.splitToList(scope, ',');
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

                var segments = Strings.splitToArray(settingsFontStyle, ' ');
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
                var segments = Strings.splitToList(s.trim(), ' ');
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
     * Resolves rule inheritance, builds default attributes, populates the color palette map,
     * and compiles rules into a trie structure.
     *
     * @param parsedThemeRules the list of parsed rules to resolve
     * @param colorMap         an optional initial palette of hex color strings
     * @return a fully compiled {@link Theme} instance
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

    /**
     * Creates and compiles a {@link Theme} directly from a raw theme representation.
     *
     * @param source   the raw theme object
     * @param colorMap an optional initial color map
     * @return the compiled {@link Theme} instance
     */
    public static Theme createFromRawTheme(@Nullable IRawTheme source, @Nullable List<String> colorMap) {
        var theme = createFromParsedTheme(parseTheme(source), colorMap);

        // not from the upstream
        if (source != null) {
            theme.editorColors = source.getEditorColors();
        }

        return theme;
    }

    /**
     * Creates a {@link Theme} from an already parsed list of theme rules.
     *
     * @param source   the parsed rules
     * @param colorMap an optional initial color map
     * @return the compiled {@link Theme} instance
     */
    public static Theme createFromParsedTheme(List<ParsedThemeRule> source, @Nullable List<String> colorMap) {
        return resolveParsedThemeRules(source, colorMap);
    }
}
