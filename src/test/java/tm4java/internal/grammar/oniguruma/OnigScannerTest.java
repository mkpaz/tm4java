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

import java.util.Arrays;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
public class OnigScannerTest {

    @Test
    public void testOnigScanner() {
        var scanner = new OnigScanner(Arrays.asList("c", "a(b)?"));
        OnigScannerMatch result = scanner.findNextMatch(OnigString.of("abc"), 0);
        assert result != null;

        assertThat(result.index).isEqualTo(1);
        assertThat(result.getCaptureIndices()).containsExactly(
            new OnigCaptureIndex(0, 2),
            new OnigCaptureIndex(1, 2)
        );

        scanner = new OnigScanner(Arrays.asList("a([b-d])c"));
        result = scanner.findNextMatch(OnigString.of("!abcdef"), 0);
        assert result != null;
        assertThat(result.index).isEqualTo(0);
        assertThat(result.getCaptureIndices()).containsExactly(
            new OnigCaptureIndex(1, 4),
            new OnigCaptureIndex(2, 3)
        );
    }
}
