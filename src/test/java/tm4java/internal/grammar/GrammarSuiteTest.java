/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.grammar;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import tm4java.Resources;
import tm4java.TMException;
import tm4java.grammar.IGrammar;
import tm4java.grammar.IStateStack;
import tm4java.grammar.IGrammarSource;
import tm4java.registry.IRegistryOptions;
import tm4java.registry.Registry;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.*;

import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @see <a href="https://github.com/microsoft/vscode-textmate/tree/main/test-cases">vscode-textmate/test-cases</a>
 */
@NullMarked
public class GrammarSuiteTest {

    static final File ROOT = Resources.getDirectory("/tm4java").toFile();

    // TODO: Fix tests
    // It seems that problem comes from with encoding. OnigString should support UTF-16 like
    // https://github.com/atom/node-oniguruma/blob/master/src/onig-string.cc
    static final List<String> IGNORE_TESTS = List.of("TEST #66");

    @TestFactory
    @DisplayName("Tokenization /first-mate/")
    public Collection<DynamicTest> firstMate() throws Exception {
        return createTestSuite(new File(ROOT, "test-cases/first-mate/tests.json"));
    }

    @TestFactory
    @DisplayName("Tokenization /suite1/tests.json")
    public Collection<DynamicTest> testsJSon() throws Exception {
        return createTestSuite(new File(ROOT, "test-cases/suite1/tests.json"));
    }

    @TestFactory
    @DisplayName("Tokenization /suite1/whileTests.json")
    public Collection<DynamicTest> whileTests() throws Exception {
        return createTestSuite(new File(ROOT, "test-cases/suite1/whileTests.json"));
    }

    //*************************************************************************

    List<DynamicTest> createTestSuite(File testLocation) throws Exception {
        try (var reader = new FileReader(testLocation)) {
            List<TestCase> tests = new GsonBuilder()
                .registerTypeAdapter(TestCase.class, (JsonDeserializer<Object>) TestCase::deserialize)
                .registerTypeAdapter(RawTestLine.class, (JsonDeserializer<Object>) RawTestLine::deserialize)
                .registerTypeAdapter(RawToken.class, (JsonDeserializer<Object>) RawToken::deserialize)
                .create()
                .fromJson(reader, TestCase.TEST_CASE_TYPE);

            var dynamicTests = new ArrayList<DynamicTest>();
            for (TestCase testCase : tests) {
                if (!IGNORE_TESTS.contains(testCase.desc)) {
                    testCase.testLocation = testLocation;
                    dynamicTests.add(DynamicTest.dynamicTest(testCase.desc, testCase::execute));
                }
            }

            return dynamicTests;
        }
    }

    static class RawToken {

        public @Nullable String value;
        public @Nullable List<String> scopes;

        RawToken() {
        }

        RawToken(String value, List<String> scopes) {
            this.value = value;
            this.scopes = scopes;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof RawToken other
                && Objects.equals(this.value, other.value)
                && Objects.equals(this.scopes, other.scopes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(scopes, value);
        }

        @Override
        public String toString() {
            return "RawToken{\n  value:" + value + "\n  scopes:" + scopes + "\n}";
        }

        static RawToken deserialize(JsonElement json, Type type, JsonDeserializationContext ctx) {
            JsonObject obj = json.getAsJsonObject();

            var rawToken = new RawToken();
            rawToken.value = obj.get("value").getAsString();

            JsonArray scopes = obj.getAsJsonArray("scopes");
            rawToken.scopes = ctx.deserialize(scopes, List.class);

            return rawToken;
        }
    }

    static final class RawTestLine {

        static final Type TOKEN_TYPE = new TypeToken<List<RawToken>>() {
        }.getType();

        public @Nullable String line;
        public @Nullable List<RawToken> tokens;

        static RawTestLine deserialize(JsonElement json, Type type, JsonDeserializationContext ctx) {
            JsonObject obj = json.getAsJsonObject();

            var line = new RawTestLine();
            line.line = obj.get("line").getAsString();
            line.tokens = ctx.deserialize(obj.get("tokens"), TOKEN_TYPE);

            return line;
        }
    }

    /**
     * @see <a href="https://github.com/microsoft/vscode-textmate/blob/v9.2.0/src/tests/tokenization.test.ts">
     * vscode-textmate/tests/tokenization.test.ts</a>
     */
    static class TestCase {

        static final Type LINE_TYPE = new TypeToken<List<RawTestLine>>() {
        }.getType();

        static final Type TEST_CASE_TYPE = (new TypeToken<ArrayList<TestCase>>() {
        }).getType();

        public @Nullable String desc;
        public @Nullable List<String> grammars;
        public @Nullable String grammarPath;
        public @Nullable String grammarScopeName;
        public @Nullable List<String> grammarInjections;
        public @Nullable List<RawTestLine> lines;
        public transient @Nullable File testLocation;

        void execute() throws Exception {
            var options = new IRegistryOptions() {
                @Override
                public @Nullable Collection<String> getInjections(String scopeName) {
                    if (scopeName.equals(grammarScopeName)) {
                        return grammarInjections;
                    }
                    return null;
                }
            };

            Objects.requireNonNull(testLocation, "testLocation can not be null");

            var registry = new Registry(options);
            IGrammar grammar = getGrammar(registry, testLocation.getParentFile());

            if (grammarScopeName != null) {
                grammar = registry.grammarForScopeName(grammarScopeName);
            }

            if (grammar == null) {
                throw new TMException("I HAVE NO GRAMMAR FOR TEST");
            }

            IStateStack prevState = null;
            for (var testLine : Objects.requireNonNull(lines)) {
                prevState = assertLineTokenization(grammar, testLine, prevState);
            }
        }

        @Nullable
        IGrammar getGrammar(Registry registry, File dir) {
            IGrammar grammar = null;
            for (String path : Objects.requireNonNull(grammars)) {
                IGrammar tmpGrammar = registry.addGrammar(
                    IGrammarSource.fromFile(new File(dir, path).toPath())
                );
                if (path.equals(grammarPath)) {
                    grammar = tmpGrammar;
                }
            }
            return grammar;
        }

        static IStateStack assertLineTokenization(IGrammar grammar,
                                                  RawTestLine testCase,
                                                  @Nullable IStateStack prevState) {
            var line = testCase.line;

            var tokenizeLineResult = grammar.tokenizeLine(Objects.requireNonNull(line), prevState, null);

            var actualTokens = Arrays.stream(tokenizeLineResult.tokens())
                .map(token -> new RawToken(
                    line.substring(token.getStartIndex(), Math.min(token.getEndIndex(), line.length())),
                    token.getScopes()
                ))
                .toList();

            Objects.requireNonNull(testCase.tokens, "test tokens can not be null");

            if (!line.isEmpty()) {
                // remove empty tokens
                testCase.tokens = testCase.tokens.stream()
                    .filter(token -> !Objects.requireNonNull(token.value).isEmpty())
                    .toList();
            }

            deepEqual(actualTokens, testCase.tokens, "Tokenizing line '" + line + "'");

            return tokenizeLineResult.ruleStack();
        }

        static void deepEqual(List<RawToken> actualTokens, List<RawToken> expectedTokens, String message) {
            // compare collection size
            if (expectedTokens.size() != actualTokens.size()) {
                var actualTokensStr = actualTokens.stream()
                    .map(Object::toString)
                    .collect(joining("\n"));
                var expectedTokensStr = expectedTokens.stream()
                    .map(Object::toString)
                    .collect(joining("\n"));

                assertThat(actualTokensStr)
                    .as(message + " (collection size)")
                    .isEqualTo(expectedTokensStr);
            }

            // compare item
            for (int i = 0; i < expectedTokens.size(); i++) {
                var expected = expectedTokens.get(i);
                var actual = actualTokens.get(i);

                assertThat(actual.value)
                    .as(message + " (value of item '" + i + "' problem)")
                    .isEqualTo(expected.value);
                assertThat(actual.scopes)
                    .as(message + " (scopes of item '" + i + "' problem)")
                    .isEqualTo(expected.scopes);
            }
        }

        static TestCase deserialize(JsonElement json, Type type, JsonDeserializationContext ctx) {
            JsonObject obj = json.getAsJsonObject();

            var testCase = new TestCase();
            testCase.desc = obj.get("desc").getAsString();
            testCase.grammars = ctx.deserialize(obj.get("grammars"), List.class);
            if (obj.get("grammarPath") != null) {
                testCase.grammarPath = obj.get("grammarPath").getAsString();
            }
            if (obj.get("grammarScopeName") != null) {
                testCase.grammarScopeName = obj.get("grammarScopeName").getAsString();
            }
            testCase.grammarInjections = ctx.deserialize(obj.get("grammarInjections"), List.class);
            testCase.lines = ctx.deserialize(obj.get("lines"), LINE_TYPE);

            return testCase;
        }
    }
}
