/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
public class StringsTest {

    @Test
    void testSplitToArray() {
        assertThat(Strings.splitToArray("", '.'))
            .containsExactly("");
        assertThat(Strings.splitToArray("abc", '.'))
            .containsExactly("abc");
        assertThat(Strings.splitToArray("abc.", '.'))
            .containsExactly("abc", "");
        assertThat(Strings.splitToArray(".abc.", '.'))
            .containsExactly("", "abc", "");
        assertThat(Strings.splitToArray(".", '.'))
            .containsExactly("", "");
        assertThat(Strings.splitToArray("...", '.'))
            .containsExactly("", "", "", "");
        assertThat(Strings.splitToArray("1.2.3.4.5.6.7.8", '.'))
            .containsExactly("1", "2", "3", "4", "5", "6", "7", "8");
    }

    @Test
    public void testSplitToList() {
        assertThat(Strings.splitToList("", '.'))
            .containsExactly("");
        assertThat(Strings.splitToList("abc", '.'))
            .containsExactly("abc");
        assertThat(Strings.splitToList("abc.", '.'))
            .containsExactly("abc", "");
        assertThat(Strings.splitToList(".abc.", '.'))
            .containsExactly("", "abc", "");
        assertThat(Strings.splitToList(".", '.'))
            .containsExactly("", "");
        assertThat(Strings.splitToList("...", '.'))
            .containsExactly("", "", "", "");
        assertThat(Strings.splitToList("1.2.3.4.5.6.7.8", '.'))
            .containsExactly("1", "2", "3", "4", "5", "6", "7", "8");
    }
}
