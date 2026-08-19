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

import java.time.Duration;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /** The field types a moment value may be compared against. */
    static final Map<String, Moment.Shape> TEMPORAL_TYPES = Map.of("date", Moment.Shape.DATE, "timestamp", Moment.Shape.TIMESTAMP);

    /**
     * A moment value: one of the now-tokens, optionally followed by a single signed ISO-8601 duration.
     * Anchored, so a trailing anything (a second offset, a stray word) simply is not a moment and the
     * caller reports it as one.
     */
    private static final Pattern MOMENT = Pattern.compile("(CURRENT_DATE|CURRENT_TIMESTAMP|NOW)(?:([+-])(\\S+))?");

    private ScheduleSupport() {}

    /**
     * A {@code where} value that names a moment: the current date or timestamp, optionally offset by an
     * ISO-8601 duration ({@code CURRENT_TIMESTAMP-PT30M} is thirty minutes ago,
     * {@code CURRENT_DATE+P7D} a week from today). Resolved against the clock of the run that fires,
     * not of the generation.
     *
     * <p>
     * This is a moment vocabulary, not an expression language: exactly one offset on one token, and the
     * offset must be an amount the token's own shape can carry - a date has no time component, so
     * {@code CURRENT_DATE-PT30M} is an authoring error rather than a comparison quietly rounded to a
     * day. {@link #offsetValid()} answers that question for the parser, which is why it is separate
     * from parsing the value at all.
     */
    public static final class Moment {

        /** Which temporal shape the comparison happens in. */
        public enum Shape {
            /** A calendar day - {@code java.time.LocalDate}. */
            DATE,
            /** A date and a time - {@code java.time.LocalDateTime}. */
            TIMESTAMP
        }

        private final Shape shape;
        private final boolean forward;
        private final String duration;

        private Moment(Shape shape, boolean forward, String duration) {
            this.shape = shape;
            this.forward = forward;
            this.duration = duration;
        }

        /**
         * @return the shape the token names
         */
        public Shape shape() {
            return shape;
        }

        /**
         * @return the authored offset ({@code PT30M}), or {@code null} when the value is a bare token
         */
        public String duration() {
            return duration;
        }

        /**
         * @return whether the offset is an ISO-8601 amount this shape can carry (always {@code true} for a
         *         bare token)
         */
        public boolean offsetValid() {
            if (duration == null) {
                return true;
            }
            try {
                if (timeBased()) {
                    // A time component is only meaningful on a timestamp; on a date it would be silently
                    // truncated, so the shape decides rather than the parse succeeding by accident.
                    return shape == Shape.TIMESTAMP && Duration.parse(duration) != null;
                }
                return Period.parse(duration) != null;
            } catch (DateTimeParseException ex) {
                return false;
            }
        }

        /**
         * @return the Java expression the generated job evaluates at each firing
         */
        public String javaExpression() {
            String now = shape == Shape.DATE ? "java.time.LocalDate.now()" : "java.time.LocalDateTime.now()";
            if (duration == null) {
                return now;
            }
            String amount = (timeBased() ? "java.time.Duration.parse(\"" : "java.time.Period.parse(\"") + duration + "\")";
            return now + (forward ? ".plus(" : ".minus(") + amount + ")";
        }

        /**
         * A duration carrying a time component is a {@code Duration}; a date-only one is a {@code Period}.
         */
        private boolean timeBased() {
            return duration.indexOf('T') >= 0 || duration.indexOf('t') >= 0;
        }
    }

    /**
     * Read a {@code where} value as a moment.
     *
     * @param value the authored value
     * @return the moment, or {@code null} when the value does not name one (an ordinary literal)
     */
    public static Moment moment(Object value) {
        if (!(value instanceof String text)) {
            return null;
        }
        Matcher matcher = MOMENT.matcher(text.trim());
        if (!matcher.matches()) {
            return null;
        }
        Moment.Shape shape = "CURRENT_DATE".equals(matcher.group(1)) ? Moment.Shape.DATE : Moment.Shape.TIMESTAMP;
        return new Moment(shape, "+".equals(matcher.group(2)), matcher.group(3));
    }

    /**
     * The shape a field of the given logical type is compared in.
     *
     * @param fieldType the authored field type, may be {@code null}
     * @return the shape, or {@code null} when the type is not temporal
     */
    public static Moment.Shape shapeOf(String fieldType) {
        return fieldType == null ? null : TEMPORAL_TYPES.get(fieldType);
    }

    /**
     * @param op the author-facing operator token
     * @return whether it maps to a supported {@code Criteria} comparison
     */
    public static boolean isSupportedOperator(String op) {
        return op != null && OPERATORS.containsKey(op);
    }

    /**
     * Build the Java {@code Criteria} expression for a schedule's {@code where} (field names are the
     * PascalCase entity property names; values are bound, with a {@link Moment} - a now-token and its
     * optional offset - evaluated against the clock of the run that fires).
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

    /** A condition value as a Java expression: a moment, a number/boolean, or a quoted string. */
    private static String valueToJava(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        Moment moment = moment(value);
        if (moment != null && moment.offsetValid()) {
            return moment.javaExpression();
        }
        String text = value.toString();
        return "\"" + text.replace("\\", "\\\\")
                          .replace("\"", "\\\"")
                + "\"";
    }
}
