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
import tm4java.grammar.IGrammarSource;
import tm4java.parser.ObjectFactory;
import tm4java.parser.PropertyPath;
import tm4java.parser.PropertySettable;

import java.io.Serial;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The default implementation of the {@link IRawGrammar}.
 *
 * <p>This class provides dynamic property storage and accessors for parsing, navigating,
 * and building TextMate grammar structures.
 */
public class RawGrammar extends PropertySettable.HashMap<Object> implements IRawGrammar {

    @Serial
    private static final long serialVersionUID = 1L;

    // property keys
    protected static final String FILE_TYPES = "fileTypes";
    protected static final String FIRST_LINE_MATCH = "firstLineMatch";
    protected static final String INJECTIONS = "injections";
    protected static final String INJECTION_SELECTOR = "injectionSelector";
    protected static final String NAME = "name";
    protected static final String PATTERNS = "patterns";
    protected static final String SCOPE_NAME = "scopeName";

    /** Cached list of sanitized file extensions associated with this grammar. */
    protected transient @Nullable List<String> fileTypes;

    /**
     * {@inheritDoc}
     *
     * <p>Lazy-loads and caches file extension strings, stripping leading dot ({@code .}) prefixes if present.
     *
     * @return a {@link Collection} of normalized file extension strings
     */
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

    /**
     * {@inheritDoc}
     *
     * @return the first-line matching regex string, or {@code null} if unspecified
     */
    @Override
    public @Nullable String getFirstLineMatch() {
        return (String) get(FIRST_LINE_MATCH);
    }

    /**
     * {@inheritDoc}
     *
     * @return the folding start marker regex string, or {@code null} if unspecified
     */
    @Override
    public @Nullable String getFoldingStartMarker() {
        return (String) get("foldingStartMarker");
    }

    /**
     * {@inheritDoc}
     *
     * @return the folding end marker regex string, or {@code null} if unspecified
     */
    @Override
    public @Nullable String getFoldingEndMarker() {
        return (String) get("foldingEndMarker");
    }

    /**
     * {@inheritDoc}
     *
     * @return a {@link Map} of injection scope selectors to target {@link IRawRule} instances, or {@code null}
     */
    @Override
    @SuppressWarnings("unchecked")
    public @Nullable Map<String, IRawRule> getInjections() {
        return (Map<String, IRawRule>) get(INJECTIONS);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Normalizes collection-based injection selectors into a single OR-joined ({@code |})
     * string selector if needed.
     *
     * @return the injection selector pattern string, or {@code null}
     */
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

    /**
     * {@inheritDoc}
     *
     * @return the display name string of the grammar, or {@code null}
     */
    @Override
    public @Nullable String getName() {
        return (String) get(NAME);
    }

    /**
     * {@inheritDoc}
     *
     * @return a {@link Collection} of top-level grammar rules, or {@code null}
     */
    @SuppressWarnings("unchecked")
    @Override
    public @Nullable Collection<IRawRule> getPatterns() {
        return (Collection<IRawRule>) get(PATTERNS);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Lazy-initializes and binds an empty {@link RawRepository} if none has been configured.
     *
     * @return the grammar's {@link IRawRepository}
     */
    @Override
    public IRawRepository getRepository() {
        var repo = (IRawRepository) get(RawRule.REPOSITORY);
        if (repo == null) {
            repo = new RawRepository();
            setRepository(repo);
        }
        return repo;
    }

    /**
     * {@inheritDoc}
     *
     * @return the root scope identifier string
     * @throws NoSuchElementException if the {@value #SCOPE_NAME} property is missing
     */
    @Override
    public String getScopeName() {
        return (String) getOrThrow(SCOPE_NAME);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Invalidates cached {@link #fileTypes} when updated.
     *
     * @param key   property key
     * @param value property value
     * @return previous value associated with key, or {@code null}
     */
    @Override
    public @Nullable Object put(String key, @Nullable Object value) {
        if (FILE_TYPES.equals(key)) {
            fileTypes = null;
        }
        return super.put(key, value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Invalidates cached {@link #fileTypes} if present in the source map.
     *
     * @param m map containing key-value pairs to copy
     */
    @Override
    public void putAll(Map<? extends String, ? extends @Nullable Object> m) {
        if (m.containsKey(FILE_TYPES)) {
            fileTypes = null;
        }
        super.putAll(m);
    }

    /**
     * {@inheritDoc}
     *
     * @param repository the repository to set
     */
    @Override
    public void setRepository(IRawRepository repository) {
        super.put(RawRule.REPOSITORY, repository);
    }

    /**
     * {@inheritDoc}
     *
     * @return a delegate {@link IRawRule} backed by this grammar instance
     */
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

    /**
     * Retrieves a mandatory property value or throws an exception if unmapped.
     *
     * @param key the property key to look up
     * @return the value associated with the key
     * @throws NoSuchElementException if no entry exists for the given key
     */
    protected Object getOrThrow(Object key) {
        var obj = get(key);
        if (obj == null) {
            throw new NoSuchElementException("Key '" + key + "' does not exit for grammar '" + getName() + '"');
        }
        return obj;
    }

    //*************************************************************************

    /**
     * An {@link ObjectFactory} for the {@link RawGrammar}.
     */
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

    /**
     * Constructs a {@link RawGrammar} instance by reading and parsing raw content
     * from an {@link IGrammarSource}.
     *
     * @param source the source provider supplying reader and parser instances
     * @return the parsed {@link RawGrammar} tree
     * @throws Exception if an IO or parsing error occurs
     */
    public static RawGrammar from(IGrammarSource source) throws Exception {
        try (var reader = source.getReader()) {
            return source.getParser().parse(reader, OBJECT_FACTORY);
        }
    }
}