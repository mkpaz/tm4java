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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static tm4java.internal.utils.NullSafety.castNonNull;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import tm4java.internal.grammar.raw.RawRepository;
import tm4java.internal.grammar.raw.RawRule;

@NullMarked
public class ObjectClonerTest {

    @Test
    public void testDeepCloneRawRepository() {
        var rule1 = new RawRule();
        rule1.setName("Rule1");

        var rule2 = new RawRule();
        rule2.setName("Rule2");

        var repo = new RawRepository();
        repo.put("rule1", rule1);
        repo.put("rule1_1", rule1);
        repo.put("rule2", rule2);
        repo.put("rule2_2", rule2);

        var repoClone = ObjectCloner.deepClone(repo);
        assertThat(repoClone).isNotNull();
        assertThat(repoClone).isNotSameAs(repo);
        assertThat(repoClone).isEqualTo(repo);

        assertNotNull(repo.getRule("rule1"));
        assertNotNull(repo.getRule("rule1_1"));
        assertNotNull(repo.getRule("rule2"));
        assertNotNull(repo.getRule("rule2_2"));

        assertThat(castNonNull(repoClone.getRule("rule1"))).isNotSameAs(rule1);
        assertThat(castNonNull(repoClone.getRule("rule1_1"))).isNotSameAs(rule1);
        assertThat(castNonNull(repoClone.getRule("rule2"))).isNotSameAs(rule2);
        assertThat(castNonNull(repoClone.getRule("rule2_2"))).isNotSameAs(rule2);

        assertThat(castNonNull(repoClone.getRule("rule1"))).isEqualTo(rule1);
        assertThat(castNonNull(repoClone.getRule("rule1_1"))).isEqualTo(rule1);
        assertThat(castNonNull(repoClone.getRule("rule2"))).isEqualTo(rule2);
        assertThat(castNonNull(repoClone.getRule("rule2_2"))).isEqualTo(rule2);

        assertThat(castNonNull(repoClone.getRule("rule1"))).isSameAs(repoClone.getRule("rule1_1"));
        assertThat(castNonNull(repoClone.getRule("rule2"))).isSameAs(repoClone.getRule("rule2_2"));
    }

    @Test
    public void testDeepCloneEmptyArray() {
        var arr = new RawRule[0];
        var clone = ObjectCloner.deepClone(arr);
        assertThat(clone).isNotSameAs(arr);
        assertThat(clone).isEqualTo(arr);
    }

    @Test
    public void testDeepCloneArray() {
        var rule1 = new RawRule();
        rule1.setName("Rule1");

        var rule2 = new RawRule();
        rule2.setName("Rule2");

        var arr = new RawRule[] {rule1, rule1, rule2, rule2};
        var arrClone = ObjectCloner.deepClone(arr);

        assertThat(arrClone).isNotSameAs(arr);
        assertThat(arrClone).isEqualTo(arr);

        assertThat(arrClone[0]).isNotSameAs(rule1);
        assertThat(arrClone[1]).isNotSameAs(rule1);
        assertThat(arrClone[2]).isNotSameAs(rule2);
        assertThat(arrClone[3]).isNotSameAs(rule2);

        assertThat(arrClone[0]).isEqualTo(rule1);
        assertThat(arrClone[1]).isEqualTo(rule1);
        assertThat(arrClone[2]).isEqualTo(rule2);
        assertThat(arrClone[3]).isEqualTo(rule2);

        assertThat(arrClone[0]).isSameAs(arrClone[1]);
        assertThat(arrClone[2]).isSameAs(arrClone[3]);
    }
}
