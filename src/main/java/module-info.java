import org.jspecify.annotations.NullMarked;

@NullMarked
module tm4java {
    requires static org.jspecify;    // compile time only
    requires static com.google.gson; // optional

    requires org.jruby.joni; // textmate regex engine
    requires java.xml;       // plist format support

    exports tm4java;
    exports tm4java.grammar;
    exports tm4java.parser;
    exports tm4java.parser.nanojson;
    exports tm4java.registry;
    exports tm4java.theme;
}
