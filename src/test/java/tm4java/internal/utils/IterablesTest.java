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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
public class IterablesTest {

    @Test
    public void testGetElementAt() {
        assertThat(Iterables.getElementAt(List.of(1, 2, 3), 0)).isEqualTo(1);
        assertThat(Iterables.getElementAt(List.of(1, 2, 3), -1)).isEqualTo(3);
        assertThat(Iterables.getElementAt(List.of(1, 2, 3), -2)).isEqualTo(2);
        assertThat(Iterables.getElementAt(List.of(1, 2, 3), -3)).isEqualTo(1);

        assertThatThrownBy(() -> Iterables.getElementAt(List.of(1, 2, 3), -4))
            .isInstanceOf(ArrayIndexOutOfBoundsException.class);
        assertThatThrownBy(() -> Iterables.getElementAt(List.of(1, 2, 3), 4))
            .isInstanceOf(ArrayIndexOutOfBoundsException.class);
    }
}
