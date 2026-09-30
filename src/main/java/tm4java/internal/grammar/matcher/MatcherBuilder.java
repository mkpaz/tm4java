/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.grammar.matcher;

import org.jspecify.annotations.Nullable;

import java.lang.System.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static java.lang.System.Logger.Level.WARNING;

/**
 * Recursive descent parser and AST builder for TextMate scope selector expressions.
 *
 * <p>Parses raw selector strings into a collection of prioritized matcher predicates. Supports logical
 * conjunction (space), disjunction (comma / pipe), negation (minus), grouping (parentheses), and
 * priority prefixes ({@code L:} for left/low and {@code R:} for right/high).
 *
 * @param <T> the candidate object type evaluated by the resulting matchers
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/matcher.ts#L14">
 * vscode-textmate/src/matcher.ts#L14</a>
 */
final class MatcherBuilder<T> {

    private static final Logger LOGGER = System.getLogger(MatcherBuilder.class.getName());

    /** Tokenizer stream producing tokens from the selector string. */
    private final Tokenizer tokenizer;

    /** Matcher used to check if a list of scope identifiers matches the target context. */
    private final NameMatcher<T> matchesName;

    /** Collected list of compiled matchers paired with their priority. */
    private final List<MatcherWithPriority<T>> results = new ArrayList<>();

    /** Current token lookahead string. */
    private @Nullable String token;

    /**
     * Constructs a {@code MatcherBuilder} and immediately parses the given selector expression.
     *
     * @param selector    the raw TextMate scope selector expression
     * @param matchesName the name matching predicate strategy
     */
    MatcherBuilder(CharSequence selector, NameMatcher<T> matchesName) {
        this.tokenizer = new Tokenizer(selector);
        this.matchesName = matchesName;

        token = tokenizer.next();
        while (token != null) {
            int priority = 0;
            if (token.length() == 2 && token.charAt(1) == ':') {
                switch (token.charAt(0)) {
                    case 'R':
                        priority = 1;
                        break;
                    case 'L':
                        priority = -1;
                        break;
                    default:
                        LOGGER.log(WARNING, "Unknown priority %s in scope selector %s", token, selector);
                }
                this.token = tokenizer.next();
            }

            Matcher<T> matcher = parseConjunction();
            results.add(new MatcherWithPriority<>(matcher, priority));
            if (!",".equals(this.token)) {
                break;
            }

            token = tokenizer.next();
        }
    }

    /**
     * Returns the compiled list of prioritized matchers resulting from the parsed selector.
     *
     * @return the list of {@link MatcherWithPriority} instances
     */
    List<MatcherWithPriority<T>> results() {
        return results;
    }

    /**
     * Parses atomic expressions: identifiers, negated expressions ({@code -op}), or parenthesized
     * expressions {@code (expr)}.
     *
     * @return a {@link Matcher} representing the operand, or {@code null} if no valid operand is found
     */
    private @Nullable Matcher<T> parseOperand() {
        if ("-".equals(token)) {
            token = tokenizer.next();
            var expressionToNegate = parseOperand();
            return matcherInput -> expressionToNegate != null && !expressionToNegate.matches(matcherInput);
        }

        if ("(".equals(token)) {
            token = tokenizer.next();
            var expressionInParents = parseInnerExpression();
            if (")".equals(token)) {
                token = tokenizer.next();
            }
            return expressionInParents;
        }

        if (token != null && isIdentifier(token)) {
            var identifiers = new ArrayList<String>();
            do {
                identifiers.add(token);
                token = tokenizer.next();
            } while (token != null && isIdentifier(token));

            return matcherInput -> matchesName.matches(identifiers, matcherInput);
        }
        return null;
    }

    /**
     * Parses a sequence of space-separated operands forming a logical AND (conjunction) expression.
     *
     * @return a {@link Matcher} evaluating {@code true} only if all contained operands match
     */
    private Matcher<T> parseConjunction() {
        var matchers = new ArrayList<Matcher<T>>();

        Matcher<T> matcher = parseOperand();
        while (matcher != null) {
            matchers.add(matcher);
            matcher = parseOperand();
        }

        // every (and)
        return matcherInput -> {
            // same as 'matchers.stream().allMatch(m -> m.test(matcherInput))' but more memory friendly
            for (Matcher<T> m : matchers) {
                if (!m.matches(matcherInput)) {
                    return false;
                }
            }
            return true;
        };
    }

    /**
     * Parses comma/pipe-separated conjunction expressions forming a logical OR (disjunction)
     * expression inside parentheses.
     *
     * @return a {@link Matcher} evaluating {@code true} if any contained conjunction matches
     */
    private Matcher<T> parseInnerExpression() {
        var matchers = new ArrayList<Matcher<T>>();
        Matcher<T> matcher = parseConjunction();

        while (true) {
            matchers.add(matcher);
            if ("|".equals(token) || ",".equals(token)) {
                do {
                    token = tokenizer.next();
                } while ("|".equals(token) || ",".equals(token)); // ignore subsequent commas
            } else {
                break;
            }

            matcher = parseConjunction();
        }

        // some (or)
        return matcherInput -> {
            // same as 'matchers.stream().anyMatch(m -> m.test(matcherInput))' but more memory friendly
            for (Matcher<T> m : matchers) {
                if (m.matches(matcherInput)) {
                    return true;
                }
            }
            return false;
        };
    }

    /**
     * Checks if a token string consists of valid identifier characters (letters, digits, dots,
     * colons, underscores).
     *
     * @param token the token string to test
     * @return {@code true} if the token is a valid identifier segment
     */
    private boolean isIdentifier(String token) {
        if (token.isEmpty()) {
            return false;
        }

        /* approximately 2-3 times faster than:
         * static final Pattern IDENTIFIER_REGEXP = Pattern.compile("[\\w\\.:]+");
         * IDENTIFIER_REGEXP.matcher(token).matches();
         *
         * approximately 10% faster than:
         * token.chars().allMatch(ch -> ... )
         */
        for (int i = 0; i < token.length(); i++) {
            char ch = token.charAt(i);
            if (ch == '.' || ch == ':' || ch == '_'
                || ch >= 'a' && ch <= 'z'
                || ch >= 'A' && ch <= 'Z'
                || ch >= '0' && ch <= '9') {
                continue;
            }
            return false;
        }

        return true;
    }

    //*************************************************************************

    /**
     * Lexical tokenizer for scope selector expression strings.
     *
     * @see <a href="https://github.com/microsoft/vscode-textmate/tree/v9.2.0/src/matcher.ts#L93">
     * vscode-textmate/src/matcher.ts</a>
     */
    @SuppressWarnings({"RegExpRedundantEscape", "ClassCanBeRecord"})
    private static final class Tokenizer {

        /** Regex pattern matching priority prefixes, scope identifiers, separators, and punctuation. */
        static final Pattern TOKEN_PATTERN =
            Pattern.compile("([LR]:|[\\w\\.:][\\w\\.:\\-]*|[\\,\\|\\-\\(\\)])");

        /** Matcher instance bound to the input sequence. */
        final java.util.regex.Matcher regex;

        /**
         * Constructs a {@code Tokenizer} for the given input character sequence.
         *
         * @param input the raw selector expression input
         */
        Tokenizer(CharSequence input) {
            regex = TOKEN_PATTERN.matcher(input);
        }

        /**
         * Advances to and returns the next lexed token string.
         *
         * @return the next token, or {@code null} if the end of input is reached
         */
        @Nullable
        String next() {
            if (!regex.find()) {
                return null;
            }
            return regex.group();
        }
    }
}