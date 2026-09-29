/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.parser;

import com.google.gson.Gson;
import tm4java.parser.PropertyPath.ListBasedPropertyPath;

import java.io.BufferedReader;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A Gson-based implementation of the {@link TMParser} for parsing JSON documents
 * into a {@link PropertySettable} hierarchical structure.
 */
public final class TMParserGson implements TMParser {

    private static final TMParserGson INSTANCE = new TMParserGson();
    private static final Gson GSON = new Gson();

    private TMParserGson() {
        // singleton
    }

    @Override
    public <T extends PropertySettable<?>> T parse(Reader source, ObjectFactory<T> factory) {
        return jsonToPropertySettableRoot(readJson(source), factory);
    }

    public static TMParserGson instance() {
        return INSTANCE;
    }

    //*************************************************************************

    @SuppressWarnings("unchecked")
    private Map<String, Object> readJson(Reader source) {
        // GSON does not support trailing commas so we have to manually remove them
        var jsonString = removeTrailingCommas(
            new BufferedReader(source)
                .lines()
                .collect(Collectors.joining("\n"))
        );

        return Objects.requireNonNull(GSON.fromJson(jsonString, Map.class), "Unexpected null value");
    }

    private <T extends PropertySettable<?>> T jsonToPropertySettableRoot(Map<String, Object> jsonRoot,
                                                                         ObjectFactory<T> factory) {
        var root = factory.createRoot();
        var propertyPath = new ListBasedPropertyPath();

        for (var entry : jsonRoot.entrySet()) {
            addChildRecursively(factory, propertyPath, root, entry.getKey(), entry.getValue());
        }

        return root;
    }

    private <T extends PropertySettable<?>> void addChildRecursively(ObjectFactory<T> factory,
                                                                     ListBasedPropertyPath propertyPath,
                                                                     PropertySettable<?> parent,
                                                                     Object propertyKey,
                                                                     Object jsonChild) {
        propertyPath.add(propertyKey);

        if (jsonChild instanceof Map<?, ?> map) {
            var child = factory.createChild(propertyPath, Map.class);
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                addChildRecursively(factory, propertyPath, child, entry.getKey(), entry.getValue());
            }
            setProperty(parent, propertyKey, child);
        } else if (jsonChild instanceof List<?> list) {
            var child = factory.createChild(propertyPath, List.class);
            for (int i = 0, l = list.size(); i < l; i++) {
                addChildRecursively(factory, propertyPath, child, i, list.get(i));
            }
            setProperty(parent, propertyKey, child);
        } else {
            setProperty(parent, propertyKey, jsonChild);
        }

        propertyPath.removeLast();
    }

    @SuppressWarnings("unchecked")
    private void setProperty(PropertySettable<?> settable, Object propertyId, Object value) {
        ((PropertySettable<Object>) settable).setProperty(propertyId.toString(), value);
    }

    @SuppressWarnings("RegExpRedundantEscape")
    private String removeTrailingCommas(String jsonString) {
        return jsonString.replaceAll("(,)(\\s*\\n(\\s*\\/\\/.*\\n)*\\s*[\\]}])", "$2");
    }
}
