/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator;

import java.util.Map;

import org.eclipse.dirigible.components.intent.model.ScheduleConditionIntent;
import org.eclipse.dirigible.components.intent.model.ScheduleIntent;

/**
 * Translates a {@link ScheduleIntent}'s {@code where} filter into the typed {@code Criteria}
 * builder call the generated {@code @Scheduled} job runs against the entity repository. Pure (no
 * Spring/IO) so the operator mapping and date-token handling are unit-tested directly.
 */
public final class ScheduleSupport {

    /** Author operator -&gt; {@code Criteria} method. */
    static final Map<String, String> OPERATORS =
            Map.of("eq", "eq", "ne", "ne", "gt", "gt", "ge", "ge", "lt", "lt", "le", "le", "like", "like");

    private ScheduleSupport() {}

    /**
     * @param op the author-facing operator token
     * @return whether it maps to a supported {@code Criteria} comparison
     */
    public static boolean isSupportedOperator(String op) {
        return op != null && OPERATORS.containsKey(op);
    }

    /**
     * Build the Java {@code Criteria} expression for a schedule's {@code where} (field names are the
     * PascalCase entity property names; values are bound, with {@code CURRENT_DATE}/
     * {@code CURRENT_TIMESTAMP} evaluated to "now").
     *
     * @param schedule the schedule
     * @return e.g.
     *         {@code Criteria.create().lt("DueOn", java.time.LocalDate.now()).eq("Status", "ACTIVE")}
     */
    public static String criteriaExpression(ScheduleIntent schedule) {
        StringBuilder expr = new StringBuilder("Criteria.create()");
        for (ScheduleConditionIntent condition : schedule.getWhere()) {
            String method = OPERATORS.get(condition.getOp());
            if (method == null) {
                continue; // validated at parse time; defensively skip an unknown operator
            }
            expr.append('.')
                .append(method)
                .append("(\"")
                .append(IntentNaming.pascalCase(condition.getField()))
                .append("\", ")
                .append(valueToJava(condition.getValue()))
                .append(')');
        }
        return expr.toString();
    }

    /** A condition value as a Java expression: a now-token, a number/boolean, or a quoted string. */
    private static String valueToJava(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        String text = value.toString();
        if ("CURRENT_DATE".equals(text)) {
            return "java.time.LocalDate.now()";
        }
        if ("CURRENT_TIMESTAMP".equals(text) || "NOW".equals(text)) {
            return "java.time.LocalDateTime.now()";
        }
        return "\"" + text.replace("\\", "\\\\")
                          .replace("\"", "\\\"")
                + "\"";
    }
}
