/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.parser;

import tm4java.parser.nanojson.JsonObject;
import tm4java.parser.nanojson.JsonParser;
import tm4java.parser.nanojson.JsonParserException;
import tm4java.TMException;
import tm4java.parser.PropertyPath.ListBasedPropertyPath;

import java.io.BufferedReader;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A NanoJson-based implementation of the {@link TMParser} for parsing JSON documents
 * into a PropertySettable hierarchical structure.
 */
public final class TMParserNanoJson implements TMParser {

    private static final TMParserNanoJson INSTANCE = new TMParserNanoJson();

    private TMParserNanoJson() {
        // singleton
    }

    @Override
    public <T extends PropertySettable<?>> T parse(Reader source, ObjectFactory<T> factory) {
        return jsonToPropertySettableRoot(readJson(source), factory);
    }

    public static TMParserNanoJson instance() {
        return INSTANCE;
    }

    //*************************************************************************

    private JsonObject readJson(Reader source) {
        var jsonString = removeTrailingCommas(
            new BufferedReader(source)
                .lines()
                .collect(Collectors.joining("\n"))
        );

        try {
            return Objects.requireNonNull(JsonParser.object().from(jsonString), "Unexpected null value");
        } catch (JsonParserException e) {
            throw new TMException("Invalid JSON", e);
        }
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
        // matches:
        // },
        //    }
        // as well as:
        // },
        //   // foo
        //   // bar
        //    }
        return jsonString.replaceAll("(,)(\\s*\\n(\\s*\\/\\/.*\\n)*\\s*[\\]}])", "$2");
    }
}
