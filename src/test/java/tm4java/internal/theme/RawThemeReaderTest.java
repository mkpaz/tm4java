/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.theme;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import tm4java.Resources;
import tm4java.TMException;
import tm4java.theme.IRawTheme;
import tm4java.theme.IThemeSource;

import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static tm4java.internal.utils.NullSafetyHelper.castNonNull;

@NullMarked
public class RawThemeReaderTest {

    @Test
    @SuppressWarnings("DataFlowIssue")
    public void testLoadingThemes() throws IOException {
        var count = new AtomicInteger();

        try (var files = Files.list(Resources.getFile("/tm4java/test-cases/themes"))) {
            files.forEach(file -> {
                var fileName = file.getFileName().toString();

                if (fileName.endsWith(".json")
                    && (fileName.contains("light") || fileName.contains("dark") || fileName.contains("black"))
                    || fileName.endsWith(".tmTheme")
                ) {
                    System.out.println("Parsing [" + file + "]...");

                    try {
                        IRawTheme rawTheme = RawTheme.read(IThemeSource.fromFile(file));
                        count.incrementAndGet();

                        assertThat(castNonNull(rawTheme.getName())).isNotEmpty();
                        assertThat(castNonNull(rawTheme.getSettings())).isNotEmpty();

                        for (var setting : rawTheme.getSettings()) {
                            assertThat(setting).isNotNull();
                            assertThat(setting.getSetting()).isNotNull();
                        }

                        var theme = Theme.createFromRawTheme(rawTheme, null);
                        assertThat(theme.getColorMap()).isNotEmpty();
                        assertThat(theme.getDefaults()).isNotNull();
                    } catch (Exception e) {
                        throw new TMException(e.getMessage(), e);
                    }
                }
            });
        }

        System.out.println("Successfully parsed " + count.intValue() + " themes.");
        assertThat(count.intValue())
            .withFailMessage("Only %d themes found, expected more than 10!", count.intValue())
            .isGreaterThan(10);
    }
}
