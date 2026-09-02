/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.template.service.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Covers the emitted roll-up compute blocks, whose optional fields the intent layer writes as the
 * EMPTY STRING rather than leaving out.
 *
 * <p>
 * That distinction is the whole point of these tests. The JavaScript this was ported from tested
 * truthiness ({@code if (ru.capacityField)}), which is false for {@code ""}; a null check is not,
 * so an unused capacity emitted {@code parent. == null} - a member access with no member. It does
 * not compile, and because the client-Java runtime compiles the whole registry in one javac task,
 * it took every controller in the generated application down with it (a 404 on endpoints that had
 * nothing to do with roll-ups).
 */
class RollupAggregatesTest {

    /**
     * A roll-up whose optional capacity, balance and status are unset the way the intent writes them.
     */
    private static Map<String, Object> sumRollup(String capacityField, String balanceField, String statusField) {
        Map<String, Object> rollup = new LinkedHashMap<>();
        rollup.put("op", "sum");
        rollup.put("childEntity", "SalesOrderItem");
        rollup.put("countField", "Total");
        rollup.put("sumField", "Amount");
        rollup.put("capacityField", capacityField);
        rollup.put("balanceField", balanceField);
        rollup.put("statusField", statusField);
        rollup.put("statusWhenFull", "");
        rollup.put("statusWhenPartial", "");
        rollup.put("statusDisplacedField", statusField.isEmpty() ? "" : "Displaced" + statusField);
        rollup.put("fkProperty", "SalesOrder");
        return rollup;
    }

    @Test
    void emptyOptionalsEmitNoCapacityBlockAtAll() {
        String rendered = RollupAggregates.render(sumRollup("", "", ""));

        assertThat(rendered).as("the sum itself is always emitted")
                            .contains("parent.Total = sum;");
        assertThat(rendered).as("an unset capacity must not emit a capacity block")
                            .doesNotContain("capacity");
        assertThat(rendered).as("no member access may be left without its member")
                            .doesNotContain("parent. ")
                            .doesNotContain("parent.;")
                            .doesNotContain("parent.)");
    }

    @Test
    void populatedOptionalsEmitTheCapacityBalanceAndStatusBlock() {
        Map<String, Object> rollup = sumRollup("Capacity", "Available", "Status");
        rollup.put("statusWhenFull", "3");
        rollup.put("statusWhenPartial", "2");

        String rendered = RollupAggregates.render(rollup);

        assertThat(rendered).contains("parent.Capacity")
                            .contains("parent.Available = capacity.subtract(sum);")
                            .contains("parent.Status = sum.compareTo(capacity) >= 0 ? 3 : 2;");
    }

    /**
     * The status moves in BOTH directions (#7016): a sum that returns to zero must give back the status
     * the roll-up set, and give it back to what it displaced - not to a declared constant, which is
     * wrong for a parent that entered the roll-up's region from another status.
     */
    @Test
    void aZeroSumRestoresTheStatusTheRollupDisplaced() {
        Map<String, Object> rollup = sumRollup("Capacity", "Available", "Status");
        rollup.put("statusWhenFull", "3");
        rollup.put("statusWhenPartial", "2");

        String rendered = RollupAggregates.render(rollup);

        String owned = "java.util.Objects.equals(parent.Status, 3) || java.util.Objects.equals(parent.Status, 2)";
        assertThat(rendered).as("the first move INTO a roll-up-owned status snapshots the status it displaces")
                            .contains("if (!(" + owned + ")) {")
                            .contains("parent.DisplacedStatus = parent.Status;")
                            .contains("derived.put(\"DisplacedStatus\", parent.DisplacedStatus);");
        assertThat(rendered).as("a zero sum relinquishes only a status the roll-up itself set")
                            .contains("} else if (" + owned + ") {")
                            .contains("parent.Status = parent.DisplacedStatus;")
                            .contains("parent.DisplacedStatus = null;")
                            .contains("derived.put(\"Status\", parent.Status);")
                            .contains("derived.put(\"DisplacedStatus\", null);");
        assertThat(rendered).as("a roll-up-owned status with no recorded predecessor is reported, not guessed")
                            .contains("if (parent.DisplacedStatus == null) {")
                            .contains("LOG.warn(")
                            .contains("entity.SalesOrder, parent.Status);");
    }

    @Test
    void absentOptionalsBehaveLikeEmptyOnes() {
        Map<String, Object> rollup = new LinkedHashMap<>();
        rollup.put("op", "sum");
        rollup.put("childEntity", "SalesOrderItem");
        rollup.put("countField", "Total");
        rollup.put("sumField", "Amount");

        String rendered = RollupAggregates.render(rollup);

        assertThat(rendered).doesNotContain("capacity")
                            .doesNotContain("parent. ");
    }
}
