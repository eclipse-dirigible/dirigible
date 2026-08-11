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

import static org.eclipse.dirigible.components.ide.template.service.model.ModelValues.str;

/**
 * Renders the compute block of one roll-up aggregate, for the handler that maintains a parent's
 * totals when its children change.
 *
 * <p>
 * The emitted code references the {@code rows}, {@code parent}, {@code derived} and {@code changed}
 * names the roll-up handler template provides. Every aggregate is wrapped in its own block so that
 * locals never collide when several are coalesced into one handler, and every one is guarded so it
 * writes the parent - and so raises the parent's update event - only when the value actually moves.
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
     * @return the Java source of the compute block
     */
    static String render(Map<String, Object> rollup) {
        String countField = str(rollup, "countField");
        return switch (strOrEmpty(rollup, "op")) {
            case "latest" -> renderLatest(rollup, countField);
            case "sum" -> renderSum(rollup, countField);
            default -> renderCount(countField);
        };
    }

    /**
     * Copies the value of the child row with the greatest ordering date onto the parent. Tracking the
     * row rather than the value keeps this independent of the copied field's Java type; a parent left
     * with no rows resets to null.
     *
     * @param rollup the roll-up descriptor
     * @param countField the parent field to write
     * @return the Java source
     */
    private static String renderLatest(Map<String, Object> rollup, String countField) {
        String childEntity = str(rollup, "childEntity");
        String byField = str(rollup, "byField");
        String ofField = str(rollup, "ofField");
        StringBuilder out = new StringBuilder(512);
        out.append("        {\n");
        out.append("            ")
           .append(childEntity)
           .append("Entity latestRow = null;\n");
        out.append("            for (var row : rows) {\n");
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
     * @return the Java source
     */
    private static String renderSum(Map<String, Object> rollup, String countField) {
        String sumField = str(rollup, "sumField");
        String capacityField = str(rollup, "capacityField");
        String balanceField = str(rollup, "balanceField");
        String statusField = str(rollup, "statusField");
        StringBuilder out = new StringBuilder(512);
        out.append("        {\n");
        out.append("            java.math.BigDecimal sum = java.math.BigDecimal.ZERO;\n");
        out.append("            for (var row : rows) {\n");
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
        if (capacityField != null) {
            out.append("                java.math.BigDecimal capacity = parent.")
               .append(capacityField)
               .append(" == null ? java.math.BigDecimal.ZERO : parent.")
               .append(capacityField)
               .append(";\n");
            if (balanceField != null) {
                out.append("                parent.")
                   .append(balanceField)
                   .append(" = capacity.subtract(sum);\n");
                out.append("                derived.put(\"")
                   .append(balanceField)
                   .append("\", parent.")
                   .append(balanceField)
                   .append(");\n");
            }
            if (statusField != null) {
                out.append("                if (sum.signum() > 0) {\n");
                out.append("                    parent.")
                   .append(statusField)
                   .append(" = sum.compareTo(capacity) >= 0 ? ")
                   .append(str(rollup, "statusWhenFull"))
                   .append(" : ")
                   .append(str(rollup, "statusWhenPartial"))
                   .append(";\n");
                out.append("                    derived.put(\"")
                   .append(statusField)
                   .append("\", parent.")
                   .append(statusField)
                   .append(");\n");
                out.append("                }\n");
            }
        }
        out.append("                changed = true;\n");
        out.append("            }\n");
        out.append("        }\n");
        return out.toString();
    }

    /**
     * Counts the child rows into the parent.
     *
     * @param countField the parent field to write
     * @return the Java source
     */
    private static String renderCount(String countField) {
        StringBuilder out = new StringBuilder(256);
        out.append("        {\n");
        out.append("            int count = rows.size();\n");
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
