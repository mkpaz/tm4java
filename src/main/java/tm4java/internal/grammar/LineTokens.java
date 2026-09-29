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
import tm4java.grammar.BalancedBracketSelectors;
import tm4java.grammar.EncodedTokenAttributes;
import tm4java.grammar.IToken;
import tm4java.internal.grammar.tokenattrs.OptionalStandardTokenType;
import tm4java.internal.theme.FontStyle;
import tm4java.internal.utils.StringUtils;

import java.lang.System.Logger;
import java.util.*;

import static java.lang.System.Logger.Level.TRACE;
import static tm4java.internal.utils.CollectionUtils.getElementAt;

/**
 * Accumulates and builds tokens (either human-readable or encoded binary tokens)
 * produced during the line tokenization process.
 *
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/grammar/grammar.ts#L945">
 * vscode-textmate/src/grammar/grammar.ts#L945</a>
 */
final class LineTokens {

    private static final Logger LOGGER = System.getLogger(LineTokens.class.getName());

    /** Reusable empty deque instance to avoid allocations when binary mode is active. */
    private static final Deque<Token> EMPTY_DEQUE = new ArrayDeque<>(0);

    /** Flag indicating whether tokens are emitted as encoded binary integers or object instances. */
    private final boolean emitBinaryTokens;

    /** The raw input line string, captured for TRACE logging purposes only. */
    private final String lineText; // defined only if LOGGER.isLoggable(TRACE)

    /** Original line length before any internal modifications (such as appended newline chars). */
    private final int origLineLength;

    /** Overrides applied to specific scope patterns to change their token type metadata. */
    private final List<TokenTypeMatcher> tokenTypeOverrides;

    /** Selectors used to determine if tokens contain balanced brackets. */
    private final @Nullable BalancedBracketSelectors balancedBracketSelectors;

    /** Indicates whether adjacent tokens sharing identical metadata should be merged into one. */
    private final boolean mergeConsecutiveTokensWithEqualMetadata;

    /** Storage for object-based tokens, active when {@link #emitBinaryTokens} is {@code false}. */
    private final Deque<Token> tokens; // used only if emitBinaryTokens is false

    /** Storage for encoded binary token pairs (start index, metadata), active when {@link #emitBinaryTokens} is {@code true}. */
    private final List<Integer> binaryTokens; // used only if emitBinaryTokens is true.

    /** End offset of the previously produced token within the line. */
    private int lastTokenEndIndex = 0;

    /**
     * Constructs a new {@code LineTokens} builder instance.
     *
     * @param emitBinaryTokens         {@code true} to produce compact binary-encoded metadata tokens
     * @param lineText                 the original input line string
     * @param origLineLength           the initial character length of the line
     * @param tokenTypeOverrides       matching rules for overriding default token types
     * @param balancedBracketSelectors matcher for determining balanced bracket token attributes
     */
    LineTokens(boolean emitBinaryTokens,
               String lineText,
               int origLineLength,
               List<TokenTypeMatcher> tokenTypeOverrides,
               @Nullable BalancedBracketSelectors balancedBracketSelectors) {

        this.emitBinaryTokens = emitBinaryTokens;
        this.tokenTypeOverrides = tokenTypeOverrides;
        this.lineText = LOGGER.isLoggable(TRACE) ? lineText : ""; // store line only if it's logged
        this.origLineLength = origLineLength;
        this.mergeConsecutiveTokensWithEqualMetadata = this.emitBinaryTokens && !StringUtils.containsRTL(lineText);

        if (this.emitBinaryTokens) {
            this.tokens = EMPTY_DEQUE;
            this.binaryTokens = new ArrayList<>();
        } else {
            this.tokens = new ArrayDeque<>();
            this.binaryTokens = Collections.emptyList();
        }

        this.balancedBracketSelectors = balancedBracketSelectors;
    }

    /**
     * Emits a token using the scope attributes provided by the current state stack node.
     *
     * @param stack    the active state stack node
     * @param endIndex the exclusive end character index for the token
     */
    void produce(StateStack stack, int endIndex) {
        produceFromScopes(stack.contentNameScopesList, endIndex);
    }

    /**
     * Emits a token spanning from {@link #lastTokenEndIndex} up to {@code endIndex} with scopes
     * derived from the provided scope stack.
     *
     * @param scopesList the scope stack representing active TextMate scopes, or {@code null}
     * @param endIndex   the exclusive end character index for the token
     */
    void produceFromScopes(@Nullable AttributedScopeStack scopesList, int endIndex) {
        if (lastTokenEndIndex >= endIndex) {
            return;
        }

        if (emitBinaryTokens) {
            int metadata = scopesList != null ? scopesList.tokenAttributes : 0;
            var containsBalancedBrackets = false;
            var balancedBracketSelectors_ = balancedBracketSelectors;
            if (balancedBracketSelectors_ != null && balancedBracketSelectors_.matchesAlways()) {
                containsBalancedBrackets = true;
            }

            if (!tokenTypeOverrides.isEmpty()
                || balancedBracketSelectors_ != null
                && !balancedBracketSelectors_.matchesAlways()
                && !balancedBracketSelectors_.matchesNever()
            ) {
                // only generate scope array when required to improve performance
                List<String> scopes = scopesList != null ? scopesList.getScopeNames() : Collections.emptyList();
                for (var tokenType : tokenTypeOverrides) {
                    if (tokenType.matcher().matches(scopes)) {
                        metadata = EncodedTokenAttributes.set(
                            metadata,
                            0,
                            tokenType.type(), // toOptionalTokenType(tokenType.type),
                            null,
                            FontStyle.NOT_SET,
                            0,
                            0
                        );
                    }
                }
                if (balancedBracketSelectors != null) {
                    containsBalancedBrackets = balancedBracketSelectors.match(scopes);
                }
            }

            if (containsBalancedBrackets) {
                metadata = EncodedTokenAttributes.set(
                    metadata,
                    0,
                    OptionalStandardTokenType.NOT_SET,
                    true,
                    FontStyle.NOT_SET,
                    0,
                    0
                );
            }

            if (mergeConsecutiveTokensWithEqualMetadata && !this.binaryTokens.isEmpty()
                && binaryTokens.getLast() == metadata) {
                // no need to push a token with the same metadata
                lastTokenEndIndex = endIndex;
                return;
            }

            if (LOGGER.isLoggable(TRACE)) {
                List<String> scopes = scopesList != null ? scopesList.getScopeNames() : Collections.emptyList();
                LOGGER.log(TRACE, "  token: |" + lineText
                    .substring(Math.max(lastTokenEndIndex, 0), endIndex)
                    .replace("\n", "\\n")
                    + '|'
                );
                for (String scope : scopes) {
                    LOGGER.log(TRACE, "      * " + scope);
                }
            }

            binaryTokens.add(lastTokenEndIndex);
            binaryTokens.add(metadata);

            lastTokenEndIndex = endIndex;
            return;
        }

        List<String> scopes = scopesList != null ? scopesList.getScopeNames() : Collections.emptyList();

        if (LOGGER.isLoggable(TRACE)) {
            LOGGER.log(TRACE, "  token: |" + lineText
                .substring(Math.max(lastTokenEndIndex, 0), endIndex)
                .replace("\n", "\\n")
                + '|'
            );
            for (String scope : scopes) {
                LOGGER.log(TRACE, "      * " + scope);
            }
        }

        // This is the change from the upstream project. When handling the tokenization
        // result, we usually take a substring from the original string using the token start
        // index (inclusive) and token end index (exclusive) to use with the corresponding
        // set of style attributes. The difference between JS/TS (upstream) and Java (port)
        // is that in JS, "foo".substring(0, 42) is "foo", while in Java it's IIOBE.
        //
        // What upstream does first is add LF to every incoming string for an unknown reason
        // (see Grammar), thus increasing its length. Then, this increased length is returned
        // as the token end index, but the substring is taken from the original unmodified
        // string. JS doesn't care about the difference, but Java does.
        //
        // The length also can't be zero (some tests check this), so for an empty string,
        // the expected token range is (0, 1) with an exclusive upper bound.
        int tokenEndIndex = Math.min(endIndex, Math.max(origLineLength, 1));

        tokens.add(new Token(lastTokenEndIndex, tokenEndIndex, scopes));
        lastTokenEndIndex = endIndex;
    }

    /**
     * Finalizes and returns the collected tokens as an array of {@link IToken} objects.
     *
     * @param stack      the state stack at the end of the line
     * @param lineLength total character length of the line
     * @return an array of produced {@link IToken} objects
     */
    IToken[] getResult(StateStack stack, int lineLength) {
        if (!tokens.isEmpty() && tokens.getLast().getStartIndex() == lineLength - 1) {
            // pop produced token for newline
            tokens.removeLast();
        }

        if (tokens.isEmpty()) {
            lastTokenEndIndex = -1;
            produce(stack, lineLength);
            tokens.getLast().setStartIndex(0);
        }

        return tokens.toArray(IToken[]::new);
    }

    /**
     * Finalizes and returns the collected binary tokens as a flattened integer array.
     *
     * <p>The returned array consists of pairs: {@code [startIndex_0, metadata_0, startIndex_1, metadata_1, ...]}.
     *
     * @param stack      the state stack at the end of the line
     * @param lineLength total character length of the line
     * @return an integer array containing encoded binary token ranges and metadata
     */
    int[] getBinaryResult(StateStack stack, int lineLength) {
        if (!binaryTokens.isEmpty() && getElementAt(binaryTokens, -2) == lineLength - 1) {
            // pop produced token for newline
            binaryTokens.removeLast();
            binaryTokens.removeLast();
        }

        if (binaryTokens.isEmpty()) {
            lastTokenEndIndex = -1;
            produce(stack, lineLength);
            binaryTokens.set(binaryTokens.size() - 2, 0);
        }

        return binaryTokens.stream().mapToInt(Integer::intValue).toArray();
    }

    //*************************************************************************

    /**
     * Default implementation of {@link IToken} used when binary token emission is disabled.
     */
    private static final class Token implements IToken {

        /** The starting character index (inclusive) of the token within the line. */
        private int startIndex;

        /** The ending character index (exclusive) of the token within the line. */
        private final int endIndex;

        /** The list of active TextMate scope names applied to this token. */
        private final List<String> scopes;

        /**
         * Constructs a new token.
         *
         * @param startIndex starting character offset (inclusive)
         * @param endIndex   ending character offset (exclusive)
         * @param scopes     list of active scope names
         */
        Token(int startIndex, int endIndex, List<String> scopes) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.scopes = scopes;
        }

        @Override
        public int getStartIndex() {
            return startIndex;
        }

        /**
         * Updates the starting character index of this token.
         *
         * @param startIndex new starting character offset
         */
        void setStartIndex(int startIndex) {
            this.startIndex = startIndex;
        }

        @Override
        public int getEndIndex() {
            return endIndex;
        }

        @Override
        public List<String> getScopes() {
            return scopes;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof Token other) {
                return startIndex == other.startIndex //
                    && endIndex == other.endIndex //
                    && scopes.equals(other.scopes);
            }
            return false;
        }

        @Override
        public int hashCode() {
            int prime = 31;
            int result = 1;
            result = prime * result + endIndex;
            result = prime * result + scopes.hashCode();
            result = prime * result + startIndex;
            return result;
        }

        @Override
        public String toString() {
            return "{"
                + "startIndex: " + startIndex
                + ", endIndex: " + endIndex
                + ", scopes: " + scopes
                + "}";
        }
    }
}
