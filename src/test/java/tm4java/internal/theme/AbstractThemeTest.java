/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.theme;

import com.google.gson.*;
import org.assertj.core.api.Assertions;
import org.jspecify.annotations.NullMarked;
import tm4java.parser.ContentType;
import tm4java.theme.IThemeSource;
import tm4java.theme.StyleAttributes;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NullMarked
abstract class AbstractThemeTest extends Assertions {

    protected static final int NOT_SET = 0;

    //@formatter:off
    protected static final Gson THEME_GSON = new GsonBuilder()
        .registerTypeAdapter(Theme.class, new ThemeSerializer())
        .registerTypeAdapter(ColorMap.class, new ColorMapSerializer())
        .registerTypeAdapter(StyleAttributes.class, new StyleAttributesSerializer())
        .registerTypeAdapter(ThemeTrieElement.class, new ThemeTrieElementSerializer())
        .registerTypeAdapter(ThemeTrieElementRule.class, new ThemeTrieElementRuleSerializer())
        .setExclusionStrategies(new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes f) {
                // ignore the cache objects
                return f.getDeclaredClass() == Theme.class && f.getName().equals("_cachedMatchRoot");
            }

            @Override
            public boolean shouldSkipClass(Class<?> clazz) {
                return false;
            }
        })
        .setPrettyPrinting()
        .create();
    //@formatter:on

    @SuppressWarnings("unchecked")
    protected static <K, V> Map<K, V> map(K k, V v, Object... moreKVs) {
        var map = new HashMap<K, V>();
        map.put(k, v);

        if (moreKVs.length == 0) {
            return map;
        }

        boolean nextIsValue = false;
        K key = null;

        for (Object obj : moreKVs) {
            if (nextIsValue) {
                map.put(key, (V) obj);
                nextIsValue = false;
            } else {
                key = (K) obj;
                nextIsValue = true;
            }
        }

        return map;
    }

    protected static Theme createTheme(ParsedThemeRule... rules) {
        return Theme.createFromParsedTheme(List.of(rules), null);
    }

    protected static Theme createTheme(String themeAsJsonString) {
        var rawTheme = RawTheme.read(
            IThemeSource.fromString(ContentType.JSON, themeAsJsonString)
        );
        return Theme.createFromRawTheme(rawTheme, null);
    }

    protected static List<ParsedThemeRule> parseTheme(String themeAsJsonString) {
        var rawTheme = RawTheme.read(
            IThemeSource.fromString(ContentType.JSON, themeAsJsonString)
        );
        return Theme.parseTheme(rawTheme);
    }

    protected static class ThemeSerializer implements JsonSerializer<Theme> {
        @Override
        public JsonElement serialize(Theme theme, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.add("colorMap", context.serialize(theme.getColorMap()));
            jsonObject.add("defaults", context.serialize(theme.getDefaults()));
            jsonObject.add("root", context.serialize(theme.root()));
            jsonObject.add("editorColors", context.serialize(theme.getEditorColors()));
            return jsonObject;
        }
    }

    protected static class ColorMapSerializer implements JsonSerializer<ColorMap> {
        @Override
        public JsonElement serialize(ColorMap colorMap, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("isFrozen", colorMap.isFrozen);
            jsonObject.addProperty("lastColorId", colorMap.lastColorId);
            jsonObject.add("id2color", context.serialize(colorMap.id2color));
            jsonObject.add("id2colorUnmodifiable", context.serialize(colorMap.id2colorUnmodifiable));
            jsonObject.add("color2id", context.serialize(colorMap.color2id));
            return jsonObject;
        }
    }

    protected static class StyleAttributesSerializer implements JsonSerializer<StyleAttributes> {
        @Override
        public JsonElement serialize(StyleAttributes styleAttributes, Type typeOfSrc,
                                     JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("fontStyle", styleAttributes.fontStyle());
            jsonObject.addProperty("foregroundId", styleAttributes.foregroundId());
            jsonObject.addProperty("backgroundId", styleAttributes.backgroundId());
            return jsonObject;
        }
    }

    protected static class ThemeTrieElementSerializer implements JsonSerializer<ThemeTrieElement> {
        @Override
        public JsonElement serialize(ThemeTrieElement themeTrieElement, Type typeOfSrc,
                                     JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.add("mainRule", context.serialize(themeTrieElement.mainRule));
            jsonObject.add("rulesWithParentScopes", context.serialize(themeTrieElement.rulesWithParentScopes));
            jsonObject.add("children", context.serialize(themeTrieElement.children));
            return jsonObject;
        }
    }

    protected static class ThemeTrieElementRuleSerializer implements JsonSerializer<ThemeTrieElementRule> {
        @Override
        public JsonElement serialize(ThemeTrieElementRule rule, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("scopeDepth", rule.scopeDepth);
            jsonObject.add("parentScopes", context.serialize(rule.parentScopes));
            jsonObject.addProperty("fontStyle", rule.fontStyle);
            jsonObject.addProperty("foreground", rule.foreground);
            jsonObject.addProperty("background", rule.background);
            return jsonObject;
        }
    }
}
