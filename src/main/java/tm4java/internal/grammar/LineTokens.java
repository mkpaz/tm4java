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
import tm4java.grammar.IToken;
import tm4java.grammar.EncodedTokenAttributes;
import tm4java.internal.grammar.tokenattrs.OptionalStandardTokenType;
import tm4java.internal.theme.FontStyle;

import java.lang.System.Logger;
import java.util.*;

import static java.lang.System.Logger.Level.TRACE;
import static tm4java.internal.utils.CollectionUtils.getElementAt;

/**
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/grammar/grammar.ts#L945">
 * vscode-textmate/src/grammar/grammar.ts#L945</a>
 */
final class LineTokens {

    private static final Logger LOGGER = System.getLogger(LineTokens.class.getName());
    private static final Deque<Token> EMPTY_DEQUE = new ArrayDeque<>(0);

    private final boolean emitBinaryTokens;
    private final String lineText;            // defined only if LOGGER.isLoggable(TRACE)
    private final int origLineLength;
    private final List<TokenTypeMatcher> tokenTypeOverrides;
    private final @Nullable BalancedBracketSelectors balancedBracketSelectors;
    private final Deque<Token> tokens;        // used only if emitBinaryTokens is false
    private final List<Integer> binaryTokens; // used only if emitBinaryTokens is true.

    private int lastTokenEndIndex = 0;

    LineTokens(boolean emitBinaryTokens,
               String lineText,
               int origLineLength,
               List<TokenTypeMatcher> tokenTypeOverrides,
               @Nullable BalancedBracketSelectors balancedBracketSelectors) {

        this.emitBinaryTokens = emitBinaryTokens;
        this.tokenTypeOverrides = tokenTypeOverrides;
        this.lineText = LOGGER.isLoggable(TRACE) ? lineText : ""; // store line only if it's logged
        this.origLineLength = origLineLength;

        if (this.emitBinaryTokens) {
            this.tokens = EMPTY_DEQUE;
            this.binaryTokens = new ArrayList<>();
        } else {
            this.tokens = new ArrayDeque<>();
            this.binaryTokens = Collections.emptyList();
        }

        this.balancedBracketSelectors = balancedBracketSelectors;
    }

    void produce(StateStack stack, int endIndex) {
        produceFromScopes(stack.contentNameScopesList, endIndex);
    }

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

            if (!binaryTokens.isEmpty() && binaryTokens.getLast() == metadata) {
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

    private static final class Token implements IToken {

        private int startIndex;
        private final int endIndex;
        private final List<String> scopes;

        Token(int startIndex, int endIndex, List<String> scopes) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.scopes = scopes;
        }

        @Override
        public int getStartIndex() {
            return startIndex;
        }

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
