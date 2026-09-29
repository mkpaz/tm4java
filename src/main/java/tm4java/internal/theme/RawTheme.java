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
 *
 * <p>This class serves a multipurpose role in representing unparsed raw theme structure maps
 * read from JSON or Plist files. Because TextMate themes contain hierarchical structures where
 * the root object, individual settings, and style dictionaries share key-value mapping behaviors,
 * this class implements {@link IRawTheme}, {@link IRawThemeSetting}, and {@link IThemeSetting}
 * simultaneously on top of a dynamic property map.
 */
public final class RawTheme extends PropertySettable.HashMap<Object>
    implements IRawTheme, IRawThemeSetting, IThemeSetting {

    @Serial
    private static final long serialVersionUID = 1L;

    //*************************************************************************
    // IRawTheme
    //*************************************************************************

    /**
     * {@inheritDoc}
     *
     * <p>Retrieves the optional display name of the theme (key {@code "name"}).
     */
    @Override
    public @Nullable String getName() {
        return (String) get("name");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Retrieves theme setting rules, supporting both VSCode style ({@code "tokenColors"})
     * and classic TextMate style ({@code "settings"}).
     */
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public @Nullable Collection<IRawThemeSetting> getSettings() {
        if (get("tokenColors") instanceof Collection settings) {
            return settings;
        }
        return (Collection<IRawThemeSetting>) get("settings");
    }

    //*****************************************************
    // Custom TM4E code, not from the upstream
    //*****************************************************

    /**
     * {@inheritDoc}
     *
     * <p>Resolves workbench editor UI color definitions. Checked first against top-level
     * {@code "colors"} (VSCode format), and falls back to searching for global settings entries
     * without a scope specification (classic TextMate format).
     */
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

    /**
     * {@inheritDoc}
     *
     * <p>Retrieves the scope or list of scopes targeted by this setting rule (key {@code "scope"}).
     */
    @Override
    public @Nullable Object getScope() {
        return get("scope");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Retrieves the inner style attribute dictionary for this setting rule (key {@code "settings"}).
     */
    @Override
    public @Nullable IThemeSetting getSetting() {
        return (IThemeSetting) get("settings");
    }

    //*************************************************************************
    // IThemeSetting
    //*************************************************************************

    /**
     * {@inheritDoc}
     *
     * <p>Retrieves the target font style definition string (key {@code "fontStyle"}).
     */
    @Override
    public @Nullable String getFontStyle() {
        return (String) get("fontStyle");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Retrieves the background color string (key {@code "background"}).
     */
    @Override
    public @Nullable String getBackground() {
        return (String) get("background");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Retrieves the foreground color string (key {@code "foreground"}).
     */
    @Override
    public @Nullable String getForeground() {
        return (String) get("foreground");
    }

    //*************************************************************************
    // Theme Reader/Parser
    //*************************************************************************

    /**
     * An {@link ObjectFactory} for the {@link RawTheme}.
     */
    public static final ObjectFactory<RawTheme> OBJECT_FACTORY = new ObjectFactory<>() {

        @Override
        public RawTheme createRoot() {
            return new RawTheme();
        }

        @Override
        public PropertySettable<?> createChild(PropertyPath path, Class<?> sourceType) {
            return List.class.isAssignableFrom(sourceType)
                ? new PropertySettable.ArrayList<>()
                : new RawTheme();
        }
    };

    /**
     * Reads and parses a raw theme from an {@link IThemeSource}.
     *
     * <p>Automatically selects between JSON and Plist parsers based on the content type of the source.
     *
     * @param source the input theme source containing raw theme data
     * @return the parsed {@link IRawTheme} instance
     * @throws TMException if an error occurs during reading or parsing
     */
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