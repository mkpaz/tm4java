# tm4java

TextMate (VSCode) grammar and theme support for Java.

This project is a fork of Eclipse [tm4e-core](https://github.com/eclipse-tm4e/tm4e), which is a Java port
of [vscode-textmate](https://github.com/microsoft/vscode-textmate).

- Compiled with Java 25.
- No external dependencies. A single ~1 MB multi-platform JAR that includes the compiled Oniguruma native binaries.

## Intro

TL;DR:

- You feed source code text into the tokenizer, which is powered by the corresponding grammar.
- A grammar file defines a set of regex patterns using Oniguruma syntax.
- The output is a list of tokens, with each token assigned one or more scopes (selectors).
- Scopes are hierarchical, ordered from least specific to most specific.
- You use the resulting scope(s) as selectors to query theme rules that specify visual styles (text color, font
  attributes). Because of this, scope names should adhere to standard naming conventions so a single theme can support
  multiple grammars.

Links:

- [VSCode Syntax Highlight Guide](https://code.visualstudio.com/api/language-extensions/syntax-highlight-guide)
- [Oniguruma syntax](https://rbuckton.github.io/regexp-features/engines/oniguruma.html)
- [TextMate grammars](https://macromates.com/manual/en/language_grammars)

You can find many grammars in [tests](src/test/resources/tm4java/language_pack) or in specialized repos,
such as [textmate-grammars-themes](https://github.com/shikijs/textmate-grammars-themes).

## Usage

Maven:

```xml

<dependency>
    <groupId>io.github.mkpaz</groupId>
    <artifactId>tm4java</artifactId>
    <version>1.1.0</version>
</dependency>
```

Gradle:

```groovy
repositories {
    mavenCentral()
}
dependencies {
    implementation 'io.github.mkpaz:tm4java:1.1.0'
}
```

Example:

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

// For multiline text, you have two options:
// - Tokenize the entire text at once if it is relatively short.
// - Tokenize line by line, maintaining the grammar state between lines.
IStateStack state = null;
for (var line : text.split("\r?\n")) {
    ITokenizeLineResult<IToken[]> result = grammar.tokenizeLine(line, state, Duration.ofSeconds(1));
    state = result.ruleStack();
    Arrays.stream(result.tokens()).forEach(token -> printer.accept(line, token));
}
```

This produces output like the following:

```text
Token:	'public'
Scopes:	[source.java, storage.modifier.java]
Style:	StyleAttributes{fontStyle=-1, foregroundId=5, backgroundId=0}
Token:	' '
Scopes:	[source.java]
Token:	'static'
Scopes:	[source.java, storage.modifier.java]
Style:	StyleAttributes{fontStyle=-1, foregroundId=5, backgroundId=0}
// ... etc.
```

Use these style attributes to look up theme color values and apply them to your code editor component.

See the full example in [UsageTest.java](src/test/java/tm4java/UsageTest.java).

Refer to the accompanying [`tm4javafx`](https://github.com/mkpaz/tm4javafx) project for a more detailed example.

## Benchmark

The JMH [`GrammarBenchmark`](src/test/java/tm4java/benchmark/GrammarBenchmark.java) measures the performance and memory
allocations of tokenizing a source file line-by-line using `Grammar#tokenizeLine()`. The benchmark preloads
[`GrammarBenchmark.JavaFile.txt`](src/test/resources/tm4java/benchmark/GrammarBenchmark.JavaFile.txt) and processes
its entire contents during each iteration.

Current version (Oniguruma FFM with cache optimizations):

```txt
Benchmark                                             Mode  Cnt        Score       Error   Units
GrammarBenchmark.tokenizeGrammar                     thrpt   10       18,525 ±     0,567   ops/s
GrammarBenchmark.tokenizeGrammar:gc.alloc.rate       thrpt   10      113,760 ±     4,728  MB/sec
GrammarBenchmark.tokenizeGrammar:gc.alloc.rate.norm  thrpt   10  6439736,838 ± 71624,596    B/op
GrammarBenchmark.tokenizeGrammar:gc.count            thrpt   10       11,000              counts
GrammarBenchmark.tokenizeGrammar:gc.time             thrpt   10      121,000                  ms
```

Earlier version (Joni - an Oniguruma port to Java, used by TM4E):

```txt
Benchmark                                             Mode  Cnt        Score      Error   Units
GrammarBenchmark.tokenizeGrammar                     thrpt   10         8,375 ±   0,769   ops/s
GrammarBenchmark.tokenizeGrammar:gc.alloc.rate       thrpt   10       145,882 ±  13,378  MB/sec
GrammarBenchmark.tokenizeGrammar:gc.alloc.rate.norm  thrpt   10  18270605,978 ± 156,599    B/op
GrammarBenchmark.tokenizeGrammar:gc.count            thrpt   10        14,000            counts
GrammarBenchmark.tokenizeGrammar:gc.time             thrpt   10        54,000                ms
```

Key Takeaways:

- 2.2x throughput increase: Tokenization performance improved from ~8,375 ops/s to ~18,525 ops/s
  reducing the average execution time per full-file tokenization from ~119 ms down to ~54 ms.
- 65% memory reduction: Heap allocations per operation were reduced from ~18.27 MB/op down to ~6.44 MB/op.

To run the benchmark:

```txt
mvn test -DskipTests -P benchmark
```

## Why fork?

- Maven Central availability: TM4E is not published on Maven Central because Eclipse uses its own release
  ecosystem. `tm4java` removes Eclipse-specific code, providing a cleaner modular structure along with Javadoc.

- Modularization and dependencies: TM4E is not modularized—as Eclipse relies on OSGi rather than Java modules and
  has hard dependencies on both Google Gson and SnakeYAML. `tm4java` has no external dependencies; it embeds
  NanoJson parser and also provides an API to use a custom parser.

- Performance: TM4E uses Joni (a Java port of the Oniguruma regex library) to support TextMate grammar syntax.
  `tm4java` uses the native Oniguruma library via FFM, making it 2.2x faster while using 65% less memory.
