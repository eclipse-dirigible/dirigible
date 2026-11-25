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

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.eclipse.dirigible.components.ide.template.service.model.ModelValues.str;

/**
 * Renders the compute block of one roll-up aggregate, for the handler that maintains a parent's
 * totals when its children change.
 *
 * <p>
 * The emitted code references the {@code parent}, {@code derived} and {@code changed} names the
 * roll-up handler template provides, and the child rows under the variable it is handed: the
 * template's {@code rows} for the first filter of a coalesced handler, a query of its own for every
 * other one (see {@link #renderRows}). Every aggregate is wrapped in its own block so that locals
 * never collide when several are coalesced into one handler, and every one is guarded so it writes
 * the parent - and so raises the parent's update event - only when the value actually moves.
 * Without that guard the cascade a parent update triggers would not terminate.
 */
final class RollupAggregates {

    /**
     * Not instantiable.
     */
    private RollupAggregates() {}

    /**
     * Renders one aggregate.
     *
     * @param rollup the roll-up descriptor
     * @param rows the variable holding the child rows this roll-up's own filter selects
     * @return the Java source of the compute block
     */
    static String render(Map<String, Object> rollup, String rows) {
        String countField = str(rollup, "countField");
        return switch (strOrEmpty(rollup, "op")) {
            case "latest" -> renderLatest(rollup, countField, rows);
            case "sum" -> renderSum(rollup, countField, rows);
            default -> renderCount(countField, rows);
        };
    }

    /**
     * Declares a further row set of a coalesced handler: the child rows one more distinct filter
     * selects (#8330). Roll-ups sharing a child and a relation share one handler, but not necessarily
     * one {@code where:} - each must be computed over the rows its own filter selects.
     *
     * @param childEntity the child entity
     * @param rows the variable to declare
     * @param criteriaExpression the query selecting the rows
     * @return the Java source of the declaration
     */
    static String renderRows(String childEntity, String rows, String criteriaExpression) {
        return "        java.util.List<" + childEntity + "Entity> " + rows + " = new " + childEntity + "Repository().findAll("
                + criteriaExpression + ");\n";
    }

    /**
     * Copies the value of the child row with the greatest ordering date onto the parent. Tracking the
     * row rather than the value keeps this independent of the copied field's Java type; a parent left
     * with no rows resets to null.
     *
     * @param rollup the roll-up descriptor
     * @param countField the parent field to write
     * @param rows the variable holding the child rows
     * @return the Java source
     */
    private static String renderLatest(Map<String, Object> rollup, String countField, String rows) {
        String childEntity = str(rollup, "childEntity");
        String byField = str(rollup, "byField");
        String ofField = str(rollup, "ofField");
        StringBuilder out = new StringBuilder(512);
        out.append("        {\n");
        out.append("            ")
           .append(childEntity)
           .append("Entity latestRow = null;\n");
        out.append("            for (var row : ")
           .append(rows)
           .append(") {\n");
        out.append("                if (row.")
           .append(byField)
           .append(" != null && (latestRow == null || latestRow.")
           .append(byField)
           .append(" == null || row.")
           .append(byField)
           .append(".compareTo(latestRow.")
           .append(byField)
           .append(") > 0)) {\n");
        out.append("                    latestRow = row;\n");
        out.append("                }\n");
        out.append("            }\n");
        out.append("            var latestValue = latestRow == null ? null : latestRow.")
           .append(ofField)
           .append(";\n");
        out.append("            if (!java.util.Objects.equals(parent.")
           .append(countField)
           .append(", latestValue)) {\n");
        out.append("                parent.")
           .append(countField)
           .append(" = latestValue;\n");
        out.append("                derived.put(\"")
           .append(countField)
           .append("\", parent.")
           .append(countField)
           .append(");\n");
        out.append("                changed = true;\n");
        out.append("            }\n");
        out.append("        }\n");
        return out.toString();
    }

    /**
     * Sums a child field into the parent, additionally maintaining the balance and the status of a
     * capacity-bearing parent.
     *
     * @param rollup the roll-up descriptor
     * @param countField the parent field to write
     * @param rows the variable holding the child rows
     * @return the Java source
     */
    private static String renderSum(Map<String, Object> rollup, String countField, String rows) {
        String sumField = str(rollup, "sumField");
        // Emptiness, not nullness: the intent glue writes "" for an unused optional field
        // (GlueIntentGenerator), and the JavaScript this was ported from tested truthiness - so a null
        // check let "" through and emitted `parent. == null ? ...`, which does not compile. That broke
        // the WHOLE client-Java batch (one javac task per generation), so every controller in the app
        // 404-ed, not just the roll-up.
        String capacityField = strOrEmpty(rollup, "capacityField");
        String balanceField = strOrEmpty(rollup, "balanceField");
        String statusField = strOrEmpty(rollup, "statusField");
        StringBuilder out = new StringBuilder(512);
        out.append("        {\n");
        out.append("            java.math.BigDecimal sum = java.math.BigDecimal.ZERO;\n");
        out.append("            for (var row : ")
           .append(rows)
           .append(") {\n");
        out.append("                if (row.")
           .append(sumField)
           .append(" != null) {\n");
        out.append("                    sum = sum.add(row.")
           .append(sumField)
           .append(");\n");
        out.append("                }\n");
        out.append("            }\n");
        out.append("            if (parent.")
           .append(countField)
           .append(" == null || parent.")
           .append(countField)
           .append(".compareTo(sum) != 0) {\n");
        out.append("                parent.")
           .append(countField)
           .append(" = sum;\n");
        out.append("                derived.put(\"")
           .append(countField)
           .append("\", parent.")
           .append(countField)
           .append(");\n");
        if (!capacityField.isEmpty()) {
            out.append("                java.math.BigDecimal capacity = parent.")
               .append(capacityField)
               .append(" == null ? java.math.BigDecimal.ZERO : parent.")
               .append(capacityField)
               .append(";\n");
            if (!balanceField.isEmpty()) {
                // An unenforced capacity is a measurement (#7904): a parent with none has no balance,
                // where an enforced one keeps reading a missing capacity as zero.
                out.append("                parent.")
                   .append(balanceField)
                   .append(" = ");
                if ("false".equals(str(rollup, "enforce"))) {
                    out.append("parent.")
                       .append(capacityField)
                       .append(" == null ? null : ");
                }
                out.append("capacity.subtract(sum);\n");
                out.append("                derived.put(\"")
                   .append(balanceField)
                   .append("\", parent.")
                   .append(balanceField)
                   .append(");\n");
            }
            if (!statusField.isEmpty()
                    && !(strOrEmpty(rollup, "statusWhenFull").isEmpty() && strOrEmpty(rollup, "statusWhenPartial").isEmpty())) {
                appendStatus(out, rollup, statusField);
            }
        }
        out.append("                changed = true;\n");
        out.append("            }\n");
        out.append("        }\n");
        return out.toString();
    }

    /**
     * Moves the parent's status with the sum, in BOTH directions. A positive sum sets
     * {@code statusWhenFull} / {@code statusWhenPartial}; the status that move displaces is remembered
     * in the parent's displaced-status column the first time the roll-up takes the status over, and a
     * sum back at zero restores it - so deleting the only allocation of a PAID invoice puts the invoice
     * back where the payment found it (CONFIRMED, or ISSUED if it was paid straight from there), not
     * left PAID with nothing paid (#7016). Only the two roll-up-owned statuses are ever relinquished: a
     * document somebody voided or cancelled while partially paid stays voided when its allocation goes.
     * A parent that was already in {@code statusWhenPartial} when the sum first moved displaced
     * nothing, so at a zero sum its FULL status falls back to the partial one (#8312).
     *
     * <p>
     * Either seed may be left out (#7941): the roll-up then owns only the band it names, and a sum in
     * the other band relinquishes the status exactly as a zero sum does - a fully depreciated asset
     * whose last depreciation is reversed goes back to the status it had, not left FULLY DEPRECIATED.
     * Splicing the missing seed in as an empty literal emitted Java that did not compile, which took
     * the whole client-Java batch down.
     *
     * <p>
     * The status is compared with {@code Objects.equals} because the seed ids are emitted as int
     * literals and the property is the boxed FK type.
     *
     * @param out the Java source being built
     * @param rollup the roll-up descriptor, naming at least one of the two seeds
     * @param statusField the parent status property
     */
    private static void appendStatus(StringBuilder out, Map<String, Object> rollup, String statusField) {
        String displacedField = str(rollup, "statusDisplacedField");
        String whenFull = strOrEmpty(rollup, "statusWhenFull");
        String whenPartial = strOrEmpty(rollup, "statusWhenPartial");
        String owned = Stream.of(whenFull, whenPartial)
                             .filter(seed -> !seed.isEmpty())
                             .map(seed -> "java.util.Objects.equals(parent." + statusField + ", " + seed + ")")
                             .collect(Collectors.joining(" || "));
        String band;
        String target;
        if (whenPartial.isEmpty()) {
            band = "sum.signum() > 0 && sum.compareTo(capacity) >= 0";
            target = whenFull;
        } else if (whenFull.isEmpty()) {
            band = "sum.signum() > 0 && sum.compareTo(capacity) < 0";
            target = whenPartial;
        } else {
            band = "sum.signum() > 0";
            target = "sum.compareTo(capacity) >= 0 ? " + whenFull + " : " + whenPartial;
        }
        out.append("                if (")
           .append(band)
           .append(") {\n");
        out.append("                    if (!(")
           .append(owned)
           .append(")) {\n");
        out.append("                        parent.")
           .append(displacedField)
           .append(" = parent.")
           .append(statusField)
           .append(";\n");
        out.append("                        derived.put(\"")
           .append(displacedField)
           .append("\", parent.")
           .append(displacedField)
           .append(");\n");
        out.append("                    }\n");
        out.append("                    parent.")
           .append(statusField)
           .append(" = ")
           .append(target)
           .append(";\n");
        out.append("                    derived.put(\"")
           .append(statusField)
           .append("\", parent.")
           .append(statusField)
           .append(");\n");
        out.append("                } else if (")
           .append(owned)
           .append(") {\n");
        out.append("                    if (parent.")
           .append(displacedField)
           .append(" == null) {\n");
        if (!whenFull.isEmpty() && !whenPartial.isEmpty()) {
            appendFullFallsBackToPartial(out, statusField, whenFull, whenPartial);
        } else {
            appendNoDisplacedStatusWarning(out, rollup, statusField);
        }
        out.append("                    } else {\n");
        out.append("                        parent.")
           .append(statusField)
           .append(" = parent.")
           .append(displacedField)
           .append(";\n");
        out.append("                        parent.")
           .append(displacedField)
           .append(" = null;\n");
        out.append("                        derived.put(\"")
           .append(statusField)
           .append("\", parent.")
           .append(statusField)
           .append(");\n");
        out.append("                        derived.put(\"")
           .append(displacedField)
           .append("\", null);\n");
        out.append("                    }\n");
        out.append("                }\n");
    }

    /**
     * A zero sum with no displaced status recorded means the parent was already in
     * {@code statusWhenPartial} when the roll-up first summed something - the roll-up's own "not full"
     * status, so taking it over displaced nothing (#8312). A FULL status must not outlive the sum that
     * produced it, so it falls back to that partial status; a parent resting in the partial status at a
     * zero sum is left alone.
     *
     * @param out the Java source being built
     * @param statusField the parent status property
     * @param whenFull the full-band seed id
     * @param whenPartial the partial-band seed id
     */
    private static void appendFullFallsBackToPartial(StringBuilder out, String statusField, String whenFull, String whenPartial) {
        out.append("                        if (java.util.Objects.equals(parent.")
           .append(statusField)
           .append(", ")
           .append(whenFull)
           .append(")) {\n");
        out.append("                            parent.")
           .append(statusField)
           .append(" = ")
           .append(whenPartial)
           .append(";\n");
        out.append("                            derived.put(\"")
           .append(statusField)
           .append("\", parent.")
           .append(statusField)
           .append(");\n");
        out.append("                        }\n");
    }

    /**
     * A single-seed roll-up that holds its status outside its band without a displaced status has no
     * status of its own to fall back to, so it reports the state rather than guessing one.
     *
     * @param out the Java source being built
     * @param rollup the roll-up descriptor
     * @param statusField the parent status property
     */
    private static void appendNoDisplacedStatusWarning(StringBuilder out, Map<String, Object> rollup, String statusField) {
        out.append(
                "                        LOG.warn(\"Roll-up: [{}] holds the roll-up-owned status [{}] with nothing summed, but records no"
                        + " displaced status to restore - left as it is\", entity.")
           .append(str(rollup, "fkProperty"))
           .append(", parent.")
           .append(statusField)
           .append(");\n");
    }

    /**
     * Counts the child rows into the parent.
     *
     * @param countField the parent field to write
     * @param rows the variable holding the child rows
     * @return the Java source
     */
    private static String renderCount(String countField, String rows) {
        StringBuilder out = new StringBuilder(256);
        out.append("        {\n");
        out.append("            int count = ")
           .append(rows)
           .append(".size();\n");
        out.append("            if (parent.")
           .append(countField)
           .append(" == null || parent.")
           .append(countField)
           .append(".intValue() != count) {\n");
        out.append("                parent.")
           .append(countField)
           .append(" = count;\n");
        out.append("                derived.put(\"")
           .append(countField)
           .append("\", parent.")
           .append(countField)
           .append(");\n");
        out.append("                changed = true;\n");
        out.append("            }\n");
        out.append("        }\n");
        return out.toString();
    }

    /**
     * Reads a key as a string, never null, so it can be switched on.
     *
     * @param map the node
     * @param key the key
     * @return the value, or the empty string
     */
    private static String strOrEmpty(Map<String, Object> map, String key) {
        String value = str(map, key);
        return value == null ? "" : value;
    }

}
