/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.grammar.oniguruma;

import static org.assertj.core.api.Assertions.assertThat;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

@NullMarked
public class OnigRegExpTest {

    @Test
    public void testOnigRegExp() {
        assertOnigRegExpSearch(
            "\\G(MAKEFILES|VPATH|SHELL|MAKESHELL|MAKE|MAKELEVEL|MAKEFLAGS|MAKECMDGOALS|CURDIR|SUFFIXES|\\.LIBPATTERNS)(?=\\s*\\))",
            "ifeq (version,$(firstword $(MAKECMDGOALS))\n",
            28,
            true,
            "MAKECMDGOALS",
            "MAKECMDGOALS");
    }

    /**
     * Tests that no caching is performed if the regexp contains a \G anchor
     */
    @Test
    public void testOnigRegExpCaching() {
        var regexp = new OnigRegExp(
            "\\G(MAKEFILES|VPATH|SHELL|MAKESHELL|MAKE|MAKELEVEL|MAKEFLAGS|MAKECMDGOALS|CURDIR|SUFFIXES|\\.LIBPATTERNS)(?=\\s*\\))");

        var line = "ifeq (version,$(firstword $(MAKECMDGOALS))\n";
        var onigLine = OnigString.of(line);

        var result = regexp.search(onigLine, 10);
        assertThat(result)
            .withFailMessage("Did not expect a match in input: \"%s\" starting at position %d", line, 10)
            .isNull();

        result = regexp.search(onigLine, 28);
        assertOnigRegExpSearch(line, result, 28, true, "MAKECMDGOALS", "MAKECMDGOALS");
    }

    @Test
    public void testNegativeLookBehinds() {
        // test of OnigRegExp.rewritePatternIfRequired (lookbehind1)
        assertOnigRegExpSearch("(?<!\\.\\s*)\\b(await)\\b", "await", 0, true, "await", "await");
        assertOnigRegExpSearch("(?<!\\.\\s*)\\b(await)\\b", "  await", 0, true, "  await", "await");
        assertOnigRegExpSearch("(?<!\\.\\s*)\\b(await)\\b", ".await", 0, false);
        assertOnigRegExpSearch("(?<!\\.\\s*)\\b(await)\\b", "  .await", 0, false);

        // test of OnigRegExp.rewritePatternIfRequired (lookbehind2)
        assertOnigRegExpSearch("(?<=^\\s*)\\\\fi", "\\fi", 0, true, "\\fi");
        assertOnigRegExpSearch("(?<=^\\s*)\\\\fi", "  \\fi", 0, true, "  \\fi");

        // test of OnigRegExp.rewritePatternIfRequired (lookbehind3)
        assertOnigRegExpSearch("(?<=\\s*\\.)\\w+", ".foo", 0, true, ".foo");
        assertOnigRegExpSearch("(?<=\\s*\\.)\\w+", "  .foo", 0, true, "  .foo");
    }

    //*************************************************************************

    @SuppressWarnings("DataFlowIssue")
    void assertOnigRegExpSearch(String input,
                                @Nullable OnigResult result,
                                int startPosition,
                                boolean shouldMatch,
                                String... expectedGroups) {
        if (shouldMatch) {
            assertThat(result)
                .withFailMessage(
                    "Expected a match in input: \"%s\" starting at position %d",
                    input, startPosition
                )
                .isNotNull();

            assertThat(result.count())
                .withFailMessage(
                    "Expected %d groups, but found %d in input: \"%s\"",
                    expectedGroups.length, result.count(), input
                )
                .isEqualTo(expectedGroups.length);

            for (int i = 0; i < expectedGroups.length; i++) {
                String expectedGroup = expectedGroups[i];
                int start = result.locationAt(i);
                int end = start + result.lengthAt(i);
                String actualGroup = input.substring(start, end);
                assertThat(actualGroup)
                    .withFailMessage(
                        "Expected group %d to be \"%s\" but found \"%s\" in input: \"%s\"",
                        i, expectedGroup, actualGroup, input
                    )
                    .isEqualTo(expectedGroup);
            }
        } else {
            assertThat(result)
                .withFailMessage(
                    "Did not expect a match in input: \"%s\" starting at position %d",
                    input, startPosition
                )
                .isNull();
        }
    }

    void assertOnigRegExpSearch(String pattern,
                                String input,
                                int startPosition,
                                boolean shouldMatch,
                                String... expectedGroups) {
        OnigRegExp regexp = new OnigRegExp(pattern);
        OnigResult result = regexp.search(OnigString.of(input), startPosition);
        assertOnigRegExpSearch(input, result, startPosition, shouldMatch, expectedGroups);
    }
}
