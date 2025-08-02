/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.grammar;

import org.jspecify.annotations.Nullable;
import tm4java.grammar.EncodedTokenAttributes;
import tm4java.internal.theme.FontStyle;
import tm4java.theme.StyleAttributes;
import tm4java.internal.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static tm4java.internal.utils.NullSafetyHelper.castNonNull;

/**
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/grammar/grammar.ts#L418">
 * vscode-textmate/src/grammar/grammar.ts#L418</a>
 */
final class AttributedScopeStack {

    record Frame(int encodedTokenAttributes, List<String> scopeNames) {
    }

    private final @Nullable AttributedScopeStack parent;
    private final ScopeStack scopePath;

    final int tokenAttributes;

    private AttributedScopeStack(@Nullable AttributedScopeStack parent,
                                 ScopeStack scopePath,
                                 int tokenAttributes) {
        this.parent = parent;
        this.scopePath = scopePath;
        this.tokenAttributes = tokenAttributes;
    }

    String scopeName() {
        return scopePath.scopeName();
    }

    List<String> getScopeNames() {
        return scopePath.getSegments();
    }

    AttributedScopeStack pushAttributed(@Nullable String scopePath, Grammar grammar) {
        if (scopePath == null) {
            return this;
        }

        if (scopePath.indexOf(' ') == -1) {
            // this is the common case and much faster
            return pushAttributed(this, scopePath, grammar);
        }

        var scopes = StringUtils.splitToArray(scopePath, ' ');
        var result = this;
        for (var scope : scopes) {
            result = pushAttributed(result, scope, grammar);
        }

        return result;
    }

    private AttributedScopeStack pushAttributed(AttributedScopeStack target,
                                                String scopeName,
                                                Grammar grammar) {
        var rawMetadata = grammar.getMetadataForScope(scopeName);

        var newPath = target.scopePath.push(scopeName);
        var scopeThemeMatchResult = grammar.themeProvider.themeMatch(newPath);
        var metadata = mergeAttributes(target.tokenAttributes, rawMetadata, scopeThemeMatchResult);

        return new AttributedScopeStack(target, newPath, metadata);
    }

    List<Frame> getExtensionIfDefined(@Nullable AttributedScopeStack base) {
        var result = new ArrayList<Frame>();
        var self = this;

        while (self != null && self != base) {
            var parent = self.parent;
            result.add(new AttributedScopeStack.Frame(
                self.tokenAttributes,
                self.scopePath.getExtensionIfDefined(parent != null ? parent.scopePath : null))
            );
            self = self.parent;
        }

        if (self == base) {
            Collections.reverse(result);
            return result;
        }

        return Collections.emptyList();
    }

    boolean equals(AttributedScopeStack other) {
        return areEqual(this, other);
    }

    @Override
    public String toString() {
        return String.join(" ", getScopeNames());
    }

    //*************************************************************************

    @SuppressWarnings("ConstantValue")
    static boolean areEqual(@Nullable AttributedScopeStack a, @Nullable AttributedScopeStack b) {
        do {
            if (a == b) {
                return true;
            }

            // end of list reached for both
            if (a == null && b == null) {
                return true;
            }

            // end of list reached only for one
            if (a == null || b == null) {
                return false;
            }

            if (a.tokenAttributes != b.tokenAttributes || !Objects.equals(a.scopeName(), b.scopeName())) {
                return false;
            }

            // go to previous pair
            a = a.parent;
            b = b.parent;
        } while (true);
    }

    static int mergeAttributes(int existingTokenAttributes,
                               BasicScopeAttributes basicScopeAttributes,
                               @Nullable StyleAttributes styleAttributes) {
        var fontStyle = FontStyle.NOT_SET;
        var foreground = 0;
        var background = 0;

        if (styleAttributes != null) {
            fontStyle = styleAttributes.fontStyle();
            foreground = styleAttributes.foregroundId();
            background = styleAttributes.backgroundId();
        }

        return EncodedTokenAttributes.set(
            existingTokenAttributes,
            basicScopeAttributes.languageId(),
            basicScopeAttributes.tokenType(),
            null,
            fontStyle,
            foreground,
            background
        );
    }

    static @Nullable AttributedScopeStack fromExtension(@Nullable AttributedScopeStack namesScopeList,
                                                        List<Frame> contentNameScopesList) {
        var current = namesScopeList;

        ScopeStack scopeNames = namesScopeList != null ? namesScopeList.scopePath : null;
        for (var frame : contentNameScopesList) {
            scopeNames = ScopeStack.push(scopeNames, frame.scopeNames);
            current = new AttributedScopeStack(current, castNonNull(scopeNames), frame.encodedTokenAttributes);
        }

        return current;
    }

    static AttributedScopeStack createRoot(String scopeName, int tokenAttributes) {
        return new AttributedScopeStack(null, new ScopeStack(null, scopeName), tokenAttributes);
    }

    static AttributedScopeStack createRootAndLookUpScopeName(String scopeName,
                                                             int encodedTokenAttributes,
                                                             Grammar grammar) {
        var rawRootMetadata = grammar.getMetadataForScope(scopeName);
        var scopePath = new ScopeStack(null, scopeName);
        var rootStyle = grammar.themeProvider.themeMatch(scopePath);

        var resolvedTokenAttributes = AttributedScopeStack.mergeAttributes(
            encodedTokenAttributes,
            rawRootMetadata,
            rootStyle
        );

        return new AttributedScopeStack(null, scopePath, resolvedTokenAttributes);
    }
}
