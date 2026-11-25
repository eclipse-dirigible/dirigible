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
        String rendered = RollupAggregates.render(sumRollup("", "", ""), "rows");

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

        String rendered = RollupAggregates.render(rollup, "rows");

        assertThat(rendered).contains("parent.Capacity")
                            .contains("parent.Available = capacity.subtract(sum);")
                            .contains("parent.Status = sum.compareTo(capacity) >= 0 ? 3 : 2;");
    }

    /**
     * An unenforced capacity (#7904) is a measurement: no capacity, no balance - not the negated sum.
     */
    @Test
    void anUnenforcedCapacityLeavesNoBalanceWithoutACapacity() {
        Map<String, Object> rollup = sumRollup("Budget", "Remaining", "");
        rollup.put("enforce", false);

        assertThat(RollupAggregates.render(rollup, "rows")).contains(
                "parent.Remaining = parent.Budget == null ? null : capacity.subtract(sum);");
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

        String rendered = RollupAggregates.render(rollup, "rows");

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

        String rendered = RollupAggregates.render(rollup, "rows");

        assertThat(rendered).doesNotContain("capacity")
                            .doesNotContain("parent. ");
    }

    /**
     * A roll-up that owns only the FULL band (#7941): no status is spliced in for the missing partial
     * one - the emitted {@code Objects.equals(parent.Status, )} / {@code ? 3 : ;} did not compile and
     * took the whole client-Java batch down. Below the capacity the roll-up owns nothing, so a parent
     * that drops out of the full band gets back the status it displaced, as at a zero sum.
     */
    @Test
    void aFullOnlyStatusSetsTheFullBandAndRelinquishesBelowIt() {
        Map<String, Object> rollup = sumRollup("Capacity", "Available", "Status");
        rollup.put("statusWhenFull", "3");

        String rendered = RollupAggregates.render(rollup, "rows");

        assertThat(rendered).as("no seed id may be left empty")
                            .doesNotContain(", )")
                            .doesNotContain(": ;")
                            .doesNotContain("? ;");
        assertThat(rendered).contains("if (sum.signum() > 0 && sum.compareTo(capacity) >= 0) {")
                            .contains("parent.Status = 3;")
                            .contains("} else if (java.util.Objects.equals(parent.Status, 3)) {")
                            .contains("parent.Status = parent.DisplacedStatus;");
    }

    /**
     * The mirror of the full-only roll-up (#7941): only the partial band is owned, so reaching the
     * capacity relinquishes the status the same way a zero sum does.
     */
    @Test
    void aPartialOnlyStatusSetsThePartialBandAndRelinquishesOutsideIt() {
        Map<String, Object> rollup = sumRollup("Capacity", "", "Status");
        rollup.put("statusWhenPartial", "2");

        String rendered = RollupAggregates.render(rollup, "rows");

        assertThat(rendered).doesNotContain(", )")
                            .doesNotContain(": ;")
                            .doesNotContain("? ;");
        assertThat(rendered).contains("if (sum.signum() > 0 && sum.compareTo(capacity) < 0) {")
                            .contains("parent.Status = 2;")
                            .contains("} else if (java.util.Objects.equals(parent.Status, 2)) {");
    }

    /**
     * A status relation with no seed at all is refused by the parser; the emitter, handed one anyway,
     * leaves the status alone rather than emitting a block that cannot compile.
     */
    @Test
    void aStatusWithNoSeedEmitsNoStatusBlock() {
        String rendered = RollupAggregates.render(sumRollup("Capacity", "Available", "Status"), "rows");

        assertThat(rendered).contains("parent.Available = capacity.subtract(sum);")
                            .doesNotContain("parent.Status")
                            .doesNotContain(", )");
    }
}
