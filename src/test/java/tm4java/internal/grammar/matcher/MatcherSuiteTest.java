/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.grammar.matcher;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.Executable;

/**
 * @see <a href="https://github.com/microsoft/vscode-textmate/blob/v9.2.0/src/tests/matcher.test.ts">
 * vscode-textmate/tests/matcher.test.ts</a>
 */
@NullMarked
public class MatcherSuiteTest {

    @TestFactory
    @DisplayName("MatcherTest")
    public List<DynamicTest> matcherTests() {
        var jsonTests = """
            [
            	{ "expression": "foo", "input": ["foo"], "result": true },
            	{ "expression": "foo", "input": ["bar"], "result": false },
            	{ "expression": "- foo", "input": ["foo"], "result": false },
            	{ "expression": "- foo", "input": ["bar"], "result": true },
            	{ "expression": "- - foo", "input": ["bar"], "result": false },
            	{ "expression": "bar foo", "input": ["foo"], "result": false },
            	{ "expression": "bar foo", "input": ["bar"], "result": false },
            	{ "expression": "bar foo", "input": ["bar", "foo"], "result": true },
            	{ "expression": "bar - foo", "input": ["bar"], "result": true },
            	{ "expression": "bar - foo", "input": ["foo", "bar"], "result": false },
            	{ "expression": "bar - foo", "input": ["foo"], "result": false },
            	{ "expression": "bar, foo", "input": ["foo"], "result": true },
            	{ "expression": "bar, foo", "input": ["bar"], "result": true },
            	{ "expression": "bar, foo", "input": ["bar", "foo"], "result": true },
            	{ "expression": "bar, -foo", "input": ["bar", "foo"], "result": true },
            	{ "expression": "bar, -foo", "input": ["yo"], "result": true },
            	{ "expression": "bar, -foo", "input": ["foo"], "result": false },
            	{ "expression": "(foo)", "input": ["foo"], "result": true },
            	{ "expression": "(foo - bar)", "input": ["foo"], "result": true },
            	{ "expression": "(foo - bar)", "input": ["foo", "bar"], "result": false },
            	{ "expression": "foo bar - (yo man)", "input": ["foo", "bar"], "result": true },
            	{ "expression": "foo bar - (yo man)", "input": ["foo", "bar", "yo"], "result": true },
            	{ "expression": "foo bar - (yo man)", "input": ["foo", "bar", "yo", "man"], "result": false },
            	{ "expression": "foo bar - (yo | man)", "input": ["foo", "bar", "yo", "man"], "result": false },
            	{ "expression": "foo bar - (yo | man)", "input": ["foo", "bar", "yo"], "result": false }
            ]
            """;

        List<TestCase> tests = new GsonBuilder()
                                   .registerTypeAdapter(TestCase.class,
                                       (JsonDeserializer<Object>) TestCase::deserialize)
                                   .create()
                                   .fromJson(jsonTests, TestCase.TEST_CASE_TYPE);

        var dynamicTests = new ArrayList<DynamicTest>();
        for (int i = 0; i < tests.size(); i++) {
            dynamicTests.add(DynamicTest.dynamicTest("Test #" + (i + 1), tests.get(i)));
        }

        return dynamicTests;
    }

    //*************************************************************************

    static class TestCase implements Executable {

        static final Type TEST_CASE_TYPE = new TypeToken<List<TestCase>>() {
        }.getType();

        public @Nullable String expression;
        public @Nullable List<String> input;
        public boolean result;

        @Override
        public void execute() {
            Objects.requireNonNull(expression, "expression field is not specified");
            Objects.requireNonNull(input, "input field is not specified");

            var actual = Matcher.createMatchers(expression)
                             .stream()
                             .anyMatch(mp -> mp.matcher().matches(input));
            assertThat(actual).isEqualTo(result);
        }

        static TestCase deserialize(JsonElement json, Type type, JsonDeserializationContext ctx) {
            var obj = json.getAsJsonObject();

            var testCase = new TestCase();
            testCase.expression = obj.get("expression").getAsString();
            testCase.input = ctx.deserialize(obj.get("input"), List.class);
            testCase.result = obj.get("result").getAsBoolean();

            return testCase;
        }
    }
}
