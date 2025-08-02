/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.grammar.raw;

import org.jspecify.annotations.Nullable;
import tm4java.parser.ObjectFactory;
import tm4java.parser.PropertyPath;
import tm4java.parser.PropertySettable;
import tm4java.grammar.IGrammarSource;

import java.io.Serial;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The default implementation of the {@link IRawGrammar}.
 */
public class RawGrammar extends PropertySettable.HashMap<@Nullable Object> implements IRawGrammar {

    @Serial
    private static final long serialVersionUID = 1L;

    protected static final String FILE_TYPES = "fileTypes";
    protected static final String FIRST_LINE_MATCH = "firstLineMatch";
    protected static final String INJECTIONS = "injections";
    protected static final String INJECTION_SELECTOR = "injectionSelector";
    protected static final String NAME = "name";
    protected static final String PATTERNS = "patterns";
    protected static final String SCOPE_NAME = "scopeName";

    protected transient @Nullable List<String> fileTypes;

    @Override
    public Collection<String> getFileTypes() {
        List<String> result = fileTypes;
        if (result == null) {
            result = new ArrayList<>();
            Collection<?> unparsedFileTypes = (Collection<?>) get(FILE_TYPES);
            if (unparsedFileTypes != null) {
                for (Object o : unparsedFileTypes) {
                    String str = Objects.toString(o);
                    if (str.startsWith(".")) { // #202
                        str = str.substring(1);
                    }
                    result.add(str);
                }
            }
            fileTypes = result;
        }
        return result;
    }

    @Override
    public @Nullable String getFirstLineMatch() {
        return (String) get(FIRST_LINE_MATCH);
    }

    @Override
    public @Nullable String getFoldingStartMarker() {
        return (String) get("foldingStartMarker");
    }

    @Override
    public @Nullable String getFoldingEndMarker() {
        return (String) get("foldingEndMarker");
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable Map<String, IRawRule> getInjections() {
        return (Map<String, IRawRule>) get(INJECTIONS);
    }

    @Override
    public @Nullable String getInjectionSelector() {
        var value = get(INJECTION_SELECTOR);

        if (value instanceof Collection<?> coll) {
            // some grammars incorrectly provide a list of selectors,
            // so we join them into a single 'or' condition string
            return coll.stream()
                .map(Object::toString)
                .collect(Collectors.joining(" | "));
        }
        return (String) get(INJECTION_SELECTOR);
    }

    @Override
    public @Nullable String getName() {
        return (String) get(NAME);
    }

    @SuppressWarnings("unchecked")
    @Override
    public @Nullable Collection<IRawRule> getPatterns() {
        return (Collection<IRawRule>) get(PATTERNS);
    }

    @Override
    public IRawRepository getRepository() {
        var repo = (IRawRepository) get(RawRule.REPOSITORY);
        if (repo == null) {
            repo = new RawRepository();
            setRepository(repo);
        }
        return repo;
    }

    @Override
    public String getScopeName() {
        return (String) getOrThrow(SCOPE_NAME);
    }

    @Override
    public @Nullable Object put(String key, @Nullable Object value) {
        if (FILE_TYPES.equals(key)) {
            fileTypes = null;
        }
        return super.put(key, value);
    }

    @Override
    public void putAll(Map<? extends String, ? extends @Nullable Object> m) {
        if (m.containsKey(FILE_TYPES)) {
            fileTypes = null;
        }
        super.putAll(m);
    }

    @Override
    public void setRepository(IRawRepository repository) {
        super.put(RawRule.REPOSITORY, repository);
    }

    @Override
    public IRawRule toRawRule() {
        return new RawRule() {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            public @Nullable String getName() {
                return RawGrammar.this.getName();
            }

            @Override
            public @Nullable Collection<IRawRule> getPatterns() {
                return RawGrammar.this.getPatterns();
            }

            @Override
            public IRawRepository getRepository() {
                return RawGrammar.this.getRepository();
            }
        };
    }

    //*************************************************************************

    protected Object getOrThrow(Object key) {
        var obj = get(key);
        if (obj == null) {
            throw new NoSuchElementException("Key '" + key + "' does not exit for grammar '" + getName() + '"');
        }
        return obj;
    }

    //*************************************************************************

    public static final ObjectFactory<RawGrammar> OBJECT_FACTORY = new ObjectFactory<>() {

        @Override
        public RawGrammar createRoot() {
            return new RawGrammar();
        }

        @Override
        public PropertySettable<?> createChild(PropertyPath path,
                                               Class<?> sourceType) {
            return switch (path.last().toString()) {
                case RawRule.REPOSITORY -> new RawRepository();
                case RawRule.BEGIN_CAPTURES,
                     RawRule.CAPTURES,
                     RawRule.END_CAPTURES,
                     RawRule.WHILE_CAPTURES -> new RawCaptures();
                default -> List.class.isAssignableFrom(sourceType)
                    ? new PropertySettable.ArrayList<>()
                    : new RawRule();
            };
        }
    };

    public static RawGrammar from(IGrammarSource source) throws Exception {
        try (var reader = source.getReader()) {
            return source.getParser().parse(reader, OBJECT_FACTORY);
        }
    }
}
