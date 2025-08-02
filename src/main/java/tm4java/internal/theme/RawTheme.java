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
import tm4java.parser.*;
import tm4java.theme.IRawTheme;
import tm4java.theme.IRawThemeSetting;
import tm4java.theme.IThemeSetting;
import tm4java.theme.IThemeSource;

import java.io.Serial;
import java.util.*;

/**
 * The default implementation of {@link IRawTheme}.
 */
public final class RawTheme extends PropertySettable.HashMap<@Nullable Object>
    implements IRawTheme, IRawThemeSetting, IThemeSetting {

    @Serial
    private static final long serialVersionUID = 1L;

    //*************************************************************************
    // IRawTheme
    //*************************************************************************

    @Override
    public @Nullable String getName() {
        return (String) get("name");
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public @Nullable Collection<IRawThemeSetting> getSettings() {
        if (get("tokenColors") instanceof Collection settings) {
            return settings;
        }
        return (Collection<IRawThemeSetting>) get("settings");
    }

    // custom tm4e code, not from upstream
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Map<String, String> getEditorColors() {
        // vscode themes only
        if (get("colors") instanceof Map colors) {
            return colors;
        }

        var settings = getSettings();
        return settings == null ? Collections.emptyMap() : settings.stream()
            .filter(s -> s.getScope() == null)
            .map(s -> ((Map<String, Map<String, String>>) s).get("settings"))
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(Collections.emptyMap());
    }

    //*************************************************************************
    // IRawThemeSetting
    //*************************************************************************

    @Override
    public @Nullable Object getScope() {
        return get("scope");
    }

    @Override
    public @Nullable IThemeSetting getSetting() {
        return (IThemeSetting) get("settings");
    }

    //*************************************************************************
    // IThemeSetting
    //*************************************************************************

    @Override
    public @Nullable String getFontStyle() {
        return (String) get("fontStyle");
    }

    @Override
    public @Nullable String getBackground() {
        return (String) get("background");
    }

    @Override
    public @Nullable String getForeground() {
        return (String) get("foreground");
    }

    //*************************************************************************
    // Theme Reader/Parser
    //*************************************************************************

    public static final ObjectFactory<RawTheme> OBJECT_FACTORY = new ObjectFactory<>() {
        @Override
        public RawTheme createRoot() {
            return new RawTheme();
        }

        @Override
        public PropertySettable<?> createChild(PropertyPath path,
                                               Class<?> sourceType) {
            return List.class.isAssignableFrom(sourceType)
                ? new PropertySettable.ArrayList<>()
                : new RawTheme();
        }
    };

    public static IRawTheme read(IThemeSource source) throws TMException {
        try (var reader = source.getReader()) {
            if (source.getContentType() == ContentType.JSON) {
                return TMParserNanoJson.instance().parse(reader, OBJECT_FACTORY);
            }
            return TMParserPList.instance().parse(reader, OBJECT_FACTORY);
        } catch (Exception e) {
            throw new TMException(e.getMessage(), e);
        }
    }
}
