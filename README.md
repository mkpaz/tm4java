## tm4java

TextMate (VSCode) grammars and themes support for Java.

This project is a fork of Eclipse [tm4e-core](https://github.com/eclipse-tm4e/tm4e)
which is a Java port of [vscode-textmate](https://github.com/microsoft/vscode-textmate).

### Intro

TL;DR:

- You feed the source code text to the tokenizer, which is powered by a corresponding grammar.
- The grammar is a file that defines a set of regex patterns based on Oniguruma syntax.
- The result is a list of tokens, with each token associated with one or more scopes (selectors).
- Scopes are hierarchical and sorted from least specific to most specific.
- You use the obtained scope(s) as a selector to query the theme rules that define what style (text color, font)
  should be used to display the text.
- For this reason, the scopes defined in the grammar shouldn't be random but should follow naming conventions,
  allowing one theme to cover multiple (preferably all) grammars.

Links:

- [VSCode Syntax Highlight Guide](https://code.visualstudio.com/api/language-extensions/syntax-highlight-guide)
- [Oniguruma syntax](https://rbuckton.github.io/regexp-features/engines/oniguruma.html)
- [TextMate grammars](https://macromates.com/manual/en/language_grammars)

You can find many grammars in [tests](src/test/resources/tm4java/language_pack) or in specialized repos,
such as [textmate-grammars-themes](https://github.com/shikijs/textmate-grammars-themes).

### Usage

Maven:

```xml

<dependency>
    <groupId>io.github.mkpaz</groupId>
    <artifactId>tm4java</artifactId>
    <version>1.0.0</version>
</dependency>
```

Gradle:

```groovy
repositories {
    mavenCentral()
}
dependencies {
    implementation 'io.github.mkpaz:tm4java:1.0.0'
}
```

#### Dependencies

The only dependency is [org.jruby.joni](https://github.com/jruby/joni) which is a port
of [Oniguruma](https://github.com/kkos/oniguruma) regexp library, because the TextMate grammars
are based on Oniguruma regular expressions.

#### Example

```java
// the registry is the main entry point; it's used to register grammars and set up a theme.
var registry = new Registry();

// add some grammars; the returned instance is then used to tokenize the text
IGrammar grammar = registry.addGrammar(IGrammarSource.fromFile(
    Resources.getFile("/tm4java/language_pack/java/java.tmLanguage.json")
));

// set a theme to query styles against the tokenization result
ITheme theme = registry.setTheme(IThemeSource.fromFile(
    Resources.getFile("/tm4java/test-cases/themes/light_vs.json")
));

String text = """
    public static void main(String[] args) {
        System.out.println("Hello World!");
    }
    """;

BiConsumer<String, IToken> printer = (line, token) -> {
    System.out.printf("Token:\t'%s'%n", line.substring(token.getStartIndex(), token.getEndIndex()));
    System.out.printf("Scopes:\t%s%n", token.getScopes());

    // scopes are sorted from least specific to the most specific, so we query them in reverse order
    for (int i = token.getScopes().size() - 1; i >= 0; i--) {
        String scope = token.getScopes().get(i);
        // query the theme for style attributes
        StyleAttributes style = theme.match(scope);
        if (style != null && !StyleAttributes.NO_STYLE.equals(style)) {
            System.out.println("Style:\t" + style);
        }
    }
};

// For multiline text, there are two options:
// - tokenize the whole text if it doesn't contain too many lines
// - tokenize it line-by-line; in this case, the state should be maintained
IStateStack state = null;
for (var line : text.split("\r?\n")) {
    ITokenizeLineResult<IToken[]> result = grammar.tokenizeLine(line, state, Duration.ofSeconds(1));
    state = result.ruleStack();
    Arrays.stream(result.tokens()).forEach(token -> printer.accept(line, token));
}
```

The result of this code is:

```text
Token:	'public'
Scopes:	[source.java, storage.modifier.java]
Style:	StyleAttributes{fontStyle=-1, foregroundId=5, backgroundId=0}
Token:	' '
Scopes:	[source.java]
Token:	'static'
Scopes:	[source.java, storage.modifier.java]
Style:	StyleAttributes{fontStyle=-1, foregroundId=5, backgroundId=0}
...
```

You can find the full version of this example in [UsageTest.java](src/test/java/tm4java/UsageTest.java).

### Benchmark

See [BENCHMARK.md](BENCHMARK.md).

### Why fork?

TM4E is not published in Maven Central, as Eclipse uses its own release ecosystem. It is not modularized
because Eclipse uses OSGi instead of Java modules, and depends on both Google Gson and SnakeYAML.

The fork embeds NanoJson instead of Gson, removes unnecessary dependencies, offers an API to use a custom parser,
eliminates Eclipse-specific code from the codebase, and provides a cleaner modular structure along with Javadoc.
