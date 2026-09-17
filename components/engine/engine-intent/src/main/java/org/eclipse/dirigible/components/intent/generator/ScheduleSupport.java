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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.dirigible.components.intent.model.ScheduleConditionIntent;
import org.eclipse.dirigible.components.intent.model.ScheduleIntent;

/**
 * Reads a {@link ScheduleIntent}'s {@code where} filter as the typed clauses the generated
 * {@code @Scheduled} job queries the entity repository by. Pure (no Spring/IO) so the operator
 * mapping and date-token handling are unit-tested directly.
 *
 * <p>
 * What this class produces is DATA, not code (issue #7406): the glue carries the clauses, and the
 * {@code Criteria} builder chain is rendered from them a layer out, by {@code JavaLiterals} - the
 * split {@code checks} took in #7405, for the same reason. A {@code Criteria.create().lt("Due",
 * java.time.LocalDate.now())} in a {@code .glue} writes a runtime package and a Java date API into
 * the process description every template reads, Java and JavaScript alike.
 */
public final class ScheduleSupport {

    /** Author operator -&gt; {@code Criteria} method. */
    static final Map<String, String> OPERATORS =
            Map.of("eq", "eq", "ne", "ne", "gt", "gt", "ge", "ge", "lt", "lt", "le", "le", "like", "like");

    /** The field types a moment value may be compared against. */
    static final Map<String, Moment.Shape> TEMPORAL_TYPES = Map.of("date", Moment.Shape.DATE, "timestamp", Moment.Shape.TIMESTAMP);

    /**
     * The same two shapes, keyed by the JDBC type a {@code .model} carries - the only spelling a
     * CROSS-MODEL source's properties are ever known by (the owner's intent types are not readable from
     * here, only what its generation emitted).
     */
    private static final Map<String, Moment.Shape> TEMPORAL_COLUMN_TYPES =
            Map.of("DATE", Moment.Shape.DATE, "TIMESTAMP", Moment.Shape.TIMESTAMP);

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
            /** A date and a time - {@code java.time.Instant}, the shape a timestamp column carries. */
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
         * @return whether the offset moves FORWARD from the token ({@code CURRENT_DATE+P7D}), as opposed to
         *         back from it
         */
        public boolean forward() {
            return forward;
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
     * The shape a column of the given JDBC type is compared in - the cross-model counterpart of
     * {@link #shapeOf(String)}, which reads an authored intent type. Both answer the same question, and
     * a cross-model {@code where} must be held to the same rule as a same-model one (issue #7393): a
     * moment of the other shape fails the query's bind on every tick.
     *
     * @param columnType the owner {@code .model}'s {@code dataType}, may be {@code null}
     * @return the shape, or {@code null} when the column is not temporal
     */
    public static Moment.Shape shapeOfColumn(String columnType) {
        return columnType == null ? null : TEMPORAL_COLUMN_TYPES.get(columnType.toUpperCase(java.util.Locale.ROOT));
    }

    /**
     * @param op the author-facing operator token
     * @return whether it maps to a supported {@code Criteria} comparison
     */
    public static boolean isSupportedOperator(String op) {
        return op != null && OPERATORS.containsKey(op);
    }

    /**
     * A schedule's {@code where} as the NEUTRAL clause list the glue carries (issue #7406) - one entry
     * per condition, in declared order, each an operator, the PascalCase entity property it reads and
     * the reading of its value. The {@code Criteria} chain is rendered from these a layer out, by
     * {@code JavaLiterals}, the same split {@code checks} took in #7405.
     *
     * @param schedule the schedule
     * @return the clauses, empty when the schedule filters on nothing
     */
    public static List<Map<String, Object>> criteria(ScheduleIntent schedule) {
        return conditions(schedule.getWhere());
    }

    /**
     * The same reading for a caller that holds the conditions itself - a create-from's source-row rule
     * ({@code items: where:}, issue #7091), which narrows the very query that selects the source
     * document's item rows by their master foreign key.
     *
     * <p>
     * A clause whose operator is not one this vocabulary has is skipped rather than approximated; the
     * parser has already reported it, and a generation reached by another route narrows the query by
     * the clauses it does understand rather than by a guessed comparison.
     *
     * @param conditions the authored conditions, may be {@code null}
     * @return the clauses, empty for no conditions
     */
    public static List<Map<String, Object>> conditions(List<ScheduleConditionIntent> conditions) {
        List<Map<String, Object>> clauses = new ArrayList<>();
        if (conditions == null) {
            return clauses;
        }
        for (ScheduleConditionIntent condition : conditions) {
            String method = OPERATORS.get(condition.getOp());
            if (method == null) {
                continue; // validated at parse time; defensively skip an unknown operator
            }
            // Written in this order deliberately: the glue is a serialized artefact a regen rewrites in
            // place, so a clause's byte order has to be a property of the intent and nothing else (issue
            // #7130).
            Map<String, Object> clause = new LinkedHashMap<>();
            clause.put("op", method);
            clause.put("property", IntentNaming.pascalCase(condition.getField()));
            clause.put("value", valueReading(condition.getValue()));
            clauses.add(clause);
        }
        return clauses;
    }

    /**
     * A condition value as the reading the glue carries: a moment - a now-token and its optional
     * offset, resolved against the clock of the run that fires - a number, a boolean, or a string.
     *
     * <p>
     * A number and a boolean keep their TEXT rather than their parsed value, as every other neutral
     * reading in this generation does: the glue is JSON, and a value that round-trips through a JSON
     * number loses the spelling the author wrote and gains a float's rendering of it.
     *
     * @param value the authored value
     * @return the reading
     */
    private static Map<String, Object> valueReading(Object value) {
        Map<String, Object> reading = new LinkedHashMap<>();
        if (value == null) {
            reading.put("kind", "null");
            return reading;
        }
        if (value instanceof Number || value instanceof Boolean) {
            reading.put("kind", value instanceof Boolean ? "boolean" : "number");
            reading.put("text", value.toString());
            return reading;
        }
        Moment moment = moment(value);
        if (moment != null && moment.offsetValid()) {
            reading.put("kind", "moment");
            reading.put("shape", moment.shape() == Moment.Shape.DATE ? "date" : "timestamp");
            if (moment.duration() != null) {
                reading.put("offset", moment.duration());
                reading.put("forward", moment.forward());
            }
            return reading;
        }
        reading.put("kind", "string");
        reading.put("text", value.toString());
        return reading;
    }

}
