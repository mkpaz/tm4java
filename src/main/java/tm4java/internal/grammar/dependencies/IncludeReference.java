/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.grammar.dependencies;

import tm4java.internal.grammar.raw.RawRepository;

/**
 * Represents a parsed {@code include} rule reference in a TextMate grammar (e.g. {@code $self},
 * {@code $base}, {@code #ruleName}, {@code scope.name}, or {@code scope.name#ruleName}).
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/grammar/grammarDependencies.ts#L240">
 * vscode-textmate/src/grammar/grammarDependencies.ts#L240</a>
 */
public final class IncludeReference {

    private static final IncludeReference BASE = new IncludeReference(
        Kind.BASE, RawRepository.DOLLAR_BASE, ""
    );

    private static final IncludeReference SELF = new IncludeReference(
        Kind.SELF, RawRepository.DOLLAR_SELF, ""
    );

    /**
     * Identifies the category of the include reference.
     */
    public enum Kind {

        /** The {@code $base} reference, pointing to the outer root grammar. */
        BASE,

        /** The {@code $self} reference, pointing to the current grammar context. */
        SELF,

        /** A relative reference within the local repository (e.g. {@code #ruleName}). */
        RELATIVE_REFERENCE,

        /** A reference to the top-level rule of an external grammar (e.g. {@code source.js}). */
        TOP_LEVEL_REFERENCE,

        /** A reference to a repository rule in an external grammar (e.g. {@code source.js#expression}). */
        TOP_LEVEL_REPOSITORY_REFERENCE
    }

    private final Kind kind;
    private final String scopeName;
    private final String ruleName;

    private IncludeReference(Kind kind, String scopeName, String ruleName) {
        this.kind = kind;
        this.scopeName = scopeName;
        this.ruleName = ruleName;
    }

    /**
     * Returns the kind of this include reference.
     *
     * @return the reference kind
     */
    public Kind getKind() {
        return kind;
    }

    /**
     * Returns the target scope name associated with this reference, if applicable.
     *
     * @return the scope name, or empty string if not applicable
     */
    public String getScopeName() {
        return scopeName;
    }

    /**
     * Returns the target repository rule name associated with this reference, if applicable.
     *
     * @return the rule name, or empty string if not applicable
     */
    public String getRuleName() {
        return ruleName;
    }

    /**
     * Parses an include string into an {@link IncludeReference} instance.
     *
     * @param include the raw include string from grammar JSON/Plist
     *                (e.g., {@code "$self"}, {@code "#foo"}, {@code "source.js#bar"})
     * @return the parsed {@link IncludeReference}
     */
    public static IncludeReference parseInclude(String include) {
        return switch (include) {
            case RawRepository.DOLLAR_BASE -> BASE;
            case RawRepository.DOLLAR_SELF -> SELF;
            default -> parseSharp(include);
        };
    }

    private static IncludeReference parseSharp(String include) {
        var indexOfSharp = include.indexOf('#');
        return switch (indexOfSharp) {
            case -1 -> new IncludeReference(Kind.TOP_LEVEL_REFERENCE, include, "");
            case 0 -> new IncludeReference(Kind.RELATIVE_REFERENCE, "", include.substring(1));
            default -> {
                var scopeName = include.substring(0, indexOfSharp);
                var ruleName = include.substring(indexOfSharp + 1);
                yield new IncludeReference(Kind.TOP_LEVEL_REPOSITORY_REFERENCE, scopeName, ruleName);
            }
        };
    }
}