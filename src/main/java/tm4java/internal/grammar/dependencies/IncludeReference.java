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

    public enum Kind {
        BASE,
        SELF,
        RELATIVE_REFERENCE,
        TOP_LEVEL_REFERENCE,
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

    public Kind getKind() {
        return kind;
    }

    public String getScopeName() {
        return scopeName;
    }

    public String getRuleName() {
        return ruleName;
    }

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
