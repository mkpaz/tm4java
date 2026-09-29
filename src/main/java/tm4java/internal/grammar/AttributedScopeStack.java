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
import tm4java.internal.utils.StringUtils;
import tm4java.theme.StyleAttributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static tm4java.internal.utils.NullSafetyHelper.castNonNull;

/**
 * Represents a stack of scope names associated with encoded token attributes.
 *
 * <p>Combines hierarchical scope path information (via {@link ScopeStack}) with calculated
 * visual styling and language token metadata (stored as packed bitmask integer attributes).
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/grammar/grammar.ts#L418">
 * vscode-textmate/src/grammar/grammar.ts#L418</a>
 */
final class AttributedScopeStack {

    /**
     * Represents a snapshot frame containing token attributes and scope names.
     *
     * @param encodedTokenAttributes packed integer containing encoded token styling/language attributes
     * @param scopeNames             list of scope name strings attached at this frame level
     */
    record Frame(int encodedTokenAttributes, List<String> scopeNames) { }

    /** Packed integer representing combined language, style, and token metadata attributes. */
    final int tokenAttributes;

    private final @Nullable AttributedScopeStack parent;
    private final ScopeStack scopePath;

    /**
     * Constructs a new {@link AttributedScopeStack} node.
     *
     * @param parent          the parent node in the stack, or {@code null} if this is the root node
     * @param scopePath       the scope path stack representation
     * @param tokenAttributes packed bit field containing resolved token style/language metadata
     */
    private AttributedScopeStack(@Nullable AttributedScopeStack parent,
                                 ScopeStack scopePath,
                                 int tokenAttributes) {
        this.parent = parent;
        this.scopePath = scopePath;
        this.tokenAttributes = tokenAttributes;
    }

    /**
     * Returns the scope name at the tip of this stack node.
     *
     * @return the leaf scope name
     */
    String scopeName() {
        return scopePath.scopeName();
    }

    /**
     * Returns the complete list of scope names in order from root to leaf.
     *
     * @return an ordered list of scope names
     */
    List<String> getScopeNames() {
        return scopePath.getSegments();
    }

    /**
     * Pushes a space-delimited scope path string or single scope name onto this attributed stack.
     *
     * @param scopePath space-separated list of scope names to push, or {@code null}
     * @param grammar   the grammar used to resolve scope metadata and theme matches
     * @return the updated attributed scope stack head
     */
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

    /**
     * Internal helper to push a single scope name onto a target attributed scope stack.
     *
     * @param target    the base stack node
     * @param scopeName single scope name string
     * @param grammar   grammar instance for theme and metadata lookup
     * @return a new attributed scope stack node reflecting the new scope and merged attributes
     */
    private AttributedScopeStack pushAttributed(AttributedScopeStack target,
                                                String scopeName,
                                                Grammar grammar) {
        var rawMetadata = grammar.getMetadataForScope(scopeName);

        var newPath = target.scopePath.push(scopeName);
        var scopeThemeMatchResult = grammar.themeProvider.themeMatch(newPath);
        var metadata = mergeAttributes(target.tokenAttributes, rawMetadata, scopeThemeMatchResult);

        return new AttributedScopeStack(target, newPath, metadata);
    }

    /**
     * Computes the list of frames appended relative to an ancestor base stack.
     *
     * @param base expected base parent attributed scope stack, or {@code null}
     * @return ordered list of extended frames from base up to {@code this}, or empty list if not extending base
     */
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

    @Override
    public String toString() {
        return String.join(" ", getScopeNames());
    }

    //*************************************************************************

    /**
     * Evaluates deep structural and attribute equality between two attributed scope stack chains.
     *
     * @param a first stack
     * @param b second stack
     * @return {@code true} if both stack chains match in scope names and attributes
     */
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

    /**
     * Merges basic scope metadata and matched theme style rules into existing encoded token attributes.
     *
     * @param existingTokenAttributes current packed bit integer attributes
     * @param basicScopeAttributes    language and token type metadata derived from scope name
     * @param styleAttributes         matched theme color and font styles, or {@code null}
     * @return an updated packed bit integer containing merged attributes
     */
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

    /**
     * Reconstructs an {@link AttributedScopeStack} by extending a base stack with a list of frames.
     *
     * @param namesScopeList        base parent attributed scope stack, or {@code null}
     * @param contentNameScopesList list of frames to append onto the base stack
     * @return updated extended attributed scope stack head
     */
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

    /**
     * Constructs a root {@link AttributedScopeStack} instance for an initial scope name and token attributes.
     *
     * @param scopeName       the root scope name
     * @param tokenAttributes initial packed token attribute integer
     * @return a new root attributed scope stack node
     */
    static AttributedScopeStack createRoot(String scopeName, int tokenAttributes) {
        return new AttributedScopeStack(null, new ScopeStack(null, scopeName), tokenAttributes);
    }

    /**
     * Constructs a root {@link AttributedScopeStack} and resolves theme styles and metadata for the root scope.
     *
     * @param scopeName              the root scope name
     * @param encodedTokenAttributes base packed token attribute integer
     * @param grammar                grammar instance for theme matching and scope metadata lookup
     * @return a new root attributed scope stack node initialized with resolved attributes
     */
    static AttributedScopeStack createRootAndLookupScopeName(String scopeName,
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