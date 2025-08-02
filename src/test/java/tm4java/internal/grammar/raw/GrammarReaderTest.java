/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.grammar.raw;

import static org.assertj.core.api.Assertions.assertThat;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import tm4java.Resources;
import tm4java.grammar.IGrammarSource;

@NullMarked
public class GrammarReaderTest {

    /**
     * Loads the same TextMate grammar in different formats and checks
     * loading them results in equal IRawGrammar objects.
     */
    @Test
    public void testLoadDifferentPlistFormats() throws Exception {
        var grammarFromXML = RawGrammar.from(
            IGrammarSource.fromResource(Resources.class, "JavaScript.tmLanguage")
        );
        assertThat(grammarFromXML).isNotNull();
        assertThat(grammarFromXML.getFileTypes()).isNotEmpty();

        var grammarFromJSON = RawGrammar.from(
            IGrammarSource.fromResource(Resources.class, "JavaScript.tmLanguage.json")
        );
        assertThat(grammarFromJSON).isEqualTo(grammarFromXML);
    }
}
