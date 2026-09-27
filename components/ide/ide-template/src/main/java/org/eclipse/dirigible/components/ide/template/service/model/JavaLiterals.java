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

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Java literals for values a model carries as text.
 *
 * <p>
 * A model value reaches a generated Java source through a template, which interpolates it verbatim.
 * A value holding a quote or a backslash therefore used to end the literal it was being written
 * into and break the compile of the whole generated module - an authored {@code defaultValue: '6"'}
 * rendered {@code entity.Size = "6"";} (dirigible #7154). Escaping the value keeps the worst case
 * at one mis-valued field.
 *
 * <p>
 * {@link #escape(String)} is THE escape for "an authored value inside a generated Java literal",
 * and it is public for that reason: the loop was copied into engine-intent twice before (dirigible
 * #7287), where a copy that drifts is a compile error in a generated module nobody sees until a
 * regen. Every hand-rolled copy across this package and engine-intent was replaced by a call to it
 * (dirigible #7295), and the copies differed - one dropped a carriage return, several escaped
 * neither the newline nor the control characters - so "the same loop" was never quite true.
 * Anything that renders a Java literal from a model value calls this; it does not grow a copy.
 */
public final class JavaLiterals {

    /** The current date, the anchor every period range is derived from. */
    private static final String TODAY = "java.time.LocalDate.now()";

    /**
     * Not instantiable.
     */
    private JavaLiterals() {}

    /**
     * Escapes a value for placement inside a Java string literal: the backslash and the double quote
     * that would otherwise end the literal, and the control characters that would end the line.
     *
     * @param value the raw value
     * @return the escaped value, ready to be placed between two double quotes
     */
    public static String escape(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 8);
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                default -> {
                    if (character < 0x20 || character == 0x7f) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }

    /**
     * The authored default of a property as a Java expression of the property's own type, or null when
     * the property has no default that a Java literal can stand in for.
     *
     * <p>
     * Every arm reads the authored text through {@link AuthoredDefaults}, so both authoring shapes -
     * bare and SQL-quoted - yield the value the column would hold. Reading it in only one arm is how
     * {@code defaultValue: "'20'"} on an integer column seeded {@code 20} in the item dialog and
     * emitted {@code Integer.valueOf("'20'")} in the repository, a {@code NumberFormatException} on
     * every create that relied on the default (dirigible #7293).
     *
     * <p>
     * A numeric default is parsed from its text rather than inlined as a numeric literal - the same
     * parse the generated expression performs, which is why it is run HERE: an unparsable numeric
     * default is refused while the author is generating, naming the property, instead of compiling into
     * an expression that throws on every create of that entity. A date/time or binary column has no
     * literal: its DEFAULT is emitted verbatim into the DDL and is typically a SQL expression
     * ({@code CURRENT_DATE}, {@code now()}).
     *
     * @param javaClass the property's Java class, as the parameter graph resolved it
     * @param defaultValue the authored default, as the model carries it
     * @param property the property the default is authored on, for the refusal message
     * @return the Java expression, or null when there is none
     * @throws IllegalArgumentException when a numeric property's default is not a value of its type
     */
    public static String defaultValueExpression(String javaClass, String defaultValue, String property) {
        if (javaClass == null || defaultValue == null || defaultValue.isEmpty()) {
            return null;
        }
        String value = AuthoredDefaults.unquote(defaultValue);
        return switch (javaClass) {
            case "java.math.BigDecimal" -> numericExpression("new java.math.BigDecimal", value, javaClass, property,
                    java.math.BigDecimal::new);
            case "Double" -> numericExpression("Double.valueOf", value, javaClass, property, Double::valueOf);
            case "Float" -> numericExpression("Float.valueOf", value, javaClass, property, Float::valueOf);
            case "Long" -> numericExpression("Long.valueOf", value, javaClass, property, Long::valueOf);
            case "Integer" -> numericExpression("Integer.valueOf", value, javaClass, property, Integer::valueOf);
            case "Short" -> numericExpression("Short.valueOf", value, javaClass, property, Short::valueOf);
            case "Boolean" -> AuthoredDefaults.readsAsTrue(defaultValue) ? "Boolean.TRUE" : "Boolean.FALSE";
            case "String" -> "\"" + escape(value) + "\"";
            default -> null;
        };
    }

    /**
     * A numeric default as the factory call the generated code applies it through, refusing a text the
     * very same factory cannot read.
     *
     * @param factory the factory the expression calls
     * @param value the authored default, unquoted
     * @param javaClass the property's Java class, for the refusal message
     * @param property the property the default is authored on, for the refusal message
     * @param parse the factory itself, run here on the authored text
     * @return the factory call
     */
    private static String numericExpression(String factory, String value, String javaClass, String property, Consumer<String> parse) {
        try {
            parse.accept(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Property [" + property + "] declares the default [" + value
                    + "], which is not a value of its type [" + javaClass + "] - every create applying it would fail.", ex);
        }
        return factory + "(\"" + escape(value) + "\")";
    }

    /**
     * The Java expression a {@code checks: compare} literal renders as, from the NEUTRAL reading the
     * model carries (issue #7405).
     *
     * <p>
     * The model describes what the author wrote - {@code {kind: number, text: "0"}}, a moment with an
     * optional signed ISO-8601 offset, or an ISO-8601 temporal - and the language appears only here,
     * the same split a property default has had all along through {@link #defaultValueExpression}. The
     * generator has already refused everything this cannot render: a number that is not one, an offset
     * a date cannot take, a moment of the wrong shape. A reading it still does not recognise yields
     * null rather than a guess, and the template reads the key's absence as "no literal" exactly as it
     * reads a missing default.
     *
     * <p>
     * A number compares by VALUE through {@code BigDecimal}, so a decimal and a long still compare
     * exactly; a temporal compares in the SHAPE the generated column carries ({@code LocalDate} for a
     * date, {@code Instant} for a timestamp), because a comparison across those two does not compile.
     *
     * @param reading the reading the model carries, as the check's {@code value}
     * @return the Java expression, or null when there is no reading to render
     */
    public static String compareLiteralExpression(Map<String, ?> reading) {
        if (reading == null) {
            return null;
        }
        String kind = text(reading, "kind");
        if ("number".equals(kind)) {
            String number = text(reading, "text");
            return number == null ? null : "new java.math.BigDecimal(\"" + escape(number) + "\")";
        }
        boolean date = "date".equals(text(reading, "shape"));
        if ("temporal".equals(kind)) {
            String value = text(reading, "text");
            return value == null ? null : (date ? "java.time.LocalDate.parse(\"" : "java.time.Instant.parse(\"") + escape(value) + "\")";
        }
        if (!"moment".equals(kind)) {
            return null;
        }
        String now = date ? "java.time.LocalDate.now()" : "java.time.Instant.now()";
        String offset = text(reading, "offset");
        if (offset == null) {
            return now;
        }
        String amount = (date ? "java.time.Period.parse(\"" : "java.time.Duration.parse(\"") + escape(offset) + "\")";
        return now + ("false".equals(text(reading, "forward")) ? ".minus(" : ".plus(") + amount + ")";
    }

    /**
     * The Java boolean a {@code requiredWhen} / {@code forbidWhen} condition renders as, from the
     * NEUTRAL terms the model carries (issue #7405) - the comparisons ANDed, each against the type the
     * generator resolved for it.
     *
     * <p>
     * A term whose value is a foreign key of an unknown width ({@code numericKey}) is compared by
     * VALUE, not as a boxed equality: the column is typed from the target's key, and
     * {@code Objects.equals(Long, Integer)} never holds, so the boxed form would switch the guard off
     * while looking authored (dirigible #7237). A term reading a loaded hop goes through that hop's
     * null guard, because the hop may not have resolved.
     *
     * @param terms the terms the model carries, as the check's {@code when}
     * @return the Java expression, or null when there are no terms to render
     */
    public static String conditionExpression(List<? extends Map<String, ?>> terms) {
        if (terms == null || terms.isEmpty()) {
            return null;
        }
        StringBuilder expression = new StringBuilder();
        for (Map<String, ?> term : terms) {
            String literal = guardLiteral(text(term, "type"), text(term, "value"));
            if (literal == null) {
                return null; // a term the generator did not type - never rendered as a weaker guard
            }
            String owner = text(term, "owner");
            String property = text(term, "property");
            String access = owner == null || "entity".equals(owner) ? "entity." + property
                    : "(" + owner + " == null ? null : " + owner + "." + property + ")";
            boolean equal = flag(term, "equal");
            String comparison = flag(term, "numericKey") ? "(" + access + " != null && " + access + ".longValue() == " + literal + ")"
                    : "java.util.Objects.equals(" + access + ", " + literal + ")";
            if (expression.length() > 0) {
                expression.append(" && ");
            }
            expression.append(equal ? comparison : "!" + comparison);
        }
        return expression.toString();
    }

    /**
     * The Java {@code Criteria} builder chain a neutral criteria list renders as (issue #7406) - the
     * clauses in declared order, each an operator, a property and a bound value.
     *
     * <p>
     * A {@code .glue} used to carry the chain itself, {@code Criteria.create().lt("Due",
     * java.time.LocalDate.now())} - the builder's package and {@code java.time} written into the
     * process description every template reads. It now carries the clauses as data and the language
     * appears only here, the same split {@code checks} took in #7405.
     *
     * @param terms the clauses the glue carries, may be null
     * @return the chain, e.g. {@code .eq("Status", 3).gt("TotalHours", 0)}, empty for no clauses
     */
    public static String criteriaChain(List<? extends Map<String, ?>> terms) {
        if (terms == null) {
            return "";
        }
        StringBuilder chain = new StringBuilder();
        for (Map<String, ?> term : terms) {
            String operator = text(term, "op");
            String property = text(term, "property");
            String value = criteriaValueExpression(term.get("value"));
            // A clause the generator did not write in full cannot be narrowed into a guess: the operator
            // set and the value vocabulary are both closed and validated while the author is generating,
            // so anything else here is a glue no generation of this build produced.
            if (operator == null || property == null || value == null) {
                continue;
            }
            chain.append('.')
                 .append(operator)
                 .append("(\"")
                 .append(escape(property))
                 .append("\", ")
                 .append(value)
                 .append(')');
        }
        return chain.toString();
    }

    /**
     * The same clauses as a whole criteria, for a caller that opens one rather than appending to one.
     *
     * @param terms the clauses the glue carries, may be null
     * @return e.g. {@code Criteria.create().eq("Active", true)}
     */
    public static String criteriaExpression(List<? extends Map<String, ?>> terms) {
        return "Criteria.create()" + criteriaChain(terms);
    }

    /**
     * One criteria clause's bound value as a Java expression, from the reading the glue carries.
     *
     * <p>
     * The value is BOUND by the criteria rather than compared in generated code, so a number renders as
     * the number it is - a {@code BigDecimal} wrapper would change the bind's type - while a moment
     * renders in the shape the queried COLUMN carries, which is what makes the comparison bind at all
     * (issue #7384).
     *
     * @param raw the reading, as the clause's {@code value}
     * @return the Java expression, or null when the reading is not one this renders
     */
    private static String criteriaValueExpression(Object raw) {
        if (!(raw instanceof Map<?, ?> reading)) {
            return null;
        }
        String kind = text(reading, "kind");
        if (kind == null) {
            return null;
        }
        String value = text(reading, "text");
        return switch (kind) {
            case "null" -> "null";
            case "number", "boolean" -> value;
            case "string" -> value == null ? null : "\"" + escape(value) + "\"";
            case "moment" -> momentExpression(reading);
            default -> null;
        };
    }

    /**
     * A now-token, optionally offset, as the Java expression the generated job evaluates at each
     * firing.
     *
     * <p>
     * A calendar amount has no fixed length in seconds, so on a timestamp it is applied on the calendar
     * of the run's own zone and handed back as the instant the column holds - the meaning a
     * {@code Duration} could not express at all. The offset's own spelling says which it is: an
     * ISO-8601 amount carrying a time component is a {@code Duration}, a date-only one a
     * {@code Period}.
     *
     * @param reading the moment reading
     * @return the expression
     */
    private static String momentExpression(Map<?, ?> reading) {
        boolean date = "date".equals(text(reading, "shape"));
        String offset = text(reading, "offset");
        if (offset == null) {
            return date ? "java.time.LocalDate.now()" : "java.time.Instant.now()";
        }
        String movement = "false".equals(text(reading, "forward")) ? ".minus(" : ".plus(";
        if (date) {
            return "java.time.LocalDate.now()" + movement + "java.time.Period.parse(\"" + escape(offset) + "\"))";
        }
        if (offset.indexOf('T') >= 0 || offset.indexOf('t') >= 0) {
            return "java.time.Instant.now()" + movement + "java.time.Duration.parse(\"" + escape(offset) + "\"))";
        }
        return "java.time.ZonedDateTime.now()" + movement + "java.time.Period.parse(\"" + escape(offset) + "\")).toInstant()";
    }

    /**
     * The first day of the current period, as a {@code java.time.LocalDate} expression (issue #7406).
     *
     * <p>
     * A scheduled generation's {@code run:} key term used to reach the glue as this arithmetic already
     * written out. What the author declared is a PERIOD - a month, a quarter - and that is what the
     * glue now carries; where the month begins is a rendering, and it belongs here with every other
     * one.
     *
     * @param period the declared period
     * @return the expression, or null for a period that has no range (a {@code day}, or an unknown one)
     */
    public static String periodLowerExpression(String period) {
        if (period == null) {
            return null;
        }
        return switch (period) {
            case "week" -> TODAY + ".with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))";
            case "month" -> TODAY + ".withDayOfMonth(1)";
            case "quarter" -> TODAY + ".with(java.time.temporal.IsoFields.DAY_OF_QUARTER, 1)";
            case "year" -> TODAY + ".withDayOfYear(1)";
            default -> null;
        };
    }

    /**
     * The last day of the current period, derived from its first - so the range the generated guard
     * queries is by construction the period the row is dated into.
     *
     * @param period the declared period
     * @return the expression, or null for a period that has no range
     */
    public static String periodUpperExpression(String period) {
        String lower = periodLowerExpression(period);
        if (lower == null) {
            return null;
        }
        return lower + switch (period) {
            case "week" -> ".plusDays(6)";
            case "month" -> ".plusMonths(1).minusDays(1)";
            case "quarter" -> ".plusMonths(3).minusDays(1)";
            default -> ".plusYears(1).minusDays(1)";
        };
    }

    /**
     * Today, the expression a {@code run: day} key term compares against - a single day needs no range,
     * and rendering it as one would make the generated guard say {@code between(today, today)} where
     * the author wrote {@code run: day}.
     *
     * @return the expression
     */
    public static String todayExpression() {
        return TODAY;
    }

    /**
     * The Java expression a posting's compared DEFAULT renders as, from the neutral reading the glue
     * carries (issue #7406) - the value the generated {@code save()} would have written into the column
     * had the intent not derived one, which is what makes a redelivery distinguishable from an
     * amendment (issue #7131).
     *
     * <p>
     * The reading is the column's own, taken at generation against the SQL type the property becomes,
     * so this renders it and does not re-decide it: a {@code number} compares by value through
     * {@code BigDecimal}, a {@code boolean} through the boxed constants the column holds, and a
     * {@code string} as the stored text - always quoted, even when it reads as a number, because a bare
     * {@code 0} would compare unequal to the stored {@code "0"}.
     *
     * @param raw the reading the glue carries, as the assignment's {@code derivedDefaultValue}
     * @return the Java expression, or null when there is no reading to render
     */
    public static String derivedDefaultExpression(Object raw) {
        if (!(raw instanceof Map<?, ?> reading)) {
            return null;
        }
        String kind = text(reading, "kind");
        String value = text(reading, "text");
        if (kind == null || value == null) {
            return null;
        }
        return switch (kind) {
            case "number" -> "new java.math.BigDecimal(\"" + escape(value) + "\")";
            case "boolean" -> "true".equals(value) ? "Boolean.TRUE" : "Boolean.FALSE";
            case "string" -> "\"" + escape(value) + "\"";
            default -> null;
        };
    }

    /**
     * A guard term's value as a Java literal of its type - the exact equality the term was typed
     * against, and null for a type that has none.
     *
     * <p>
     * {@code number} is the type of a term the generator could NOT type against a declared property -
     * the untyped guards of a process trigger, a wait and a register lookup (issue #7425), whose
     * value's own spelling is all there is to go on. It renders as the bare spelling, whole or
     * fractional, exactly as those guards always compared.
     *
     * @param type the type the generator resolved the term against
     * @param value the authored value, unquoted
     * @return the Java literal, or null
     */
    private static String guardLiteral(String type, String value) {
        if (type == null || value == null) {
            return null;
        }
        return switch (type) {
            case "string", "text" -> "\"" + escape(value) + "\"";
            case "integer", "int" -> value.matches("-?\\d+") ? value : null;
            case "long" -> value.matches("-?\\d+") ? value + "L" : null;
            case "number" -> value.matches("-?\\d+(\\.\\d+)?") ? value : null;
            case "boolean" -> "true".equals(value) || "false".equals(value) ? value : null;
            default -> null;
        };
    }

    /** A model value as text, or null when it is absent - a model carries everything as strings. */
    private static String text(Map<?, ?> holder, String key) {
        Object value = holder == null ? null : holder.get(key);
        return value == null ? null : String.valueOf(value);
    }

    /** A model flag, which reaches here as a boolean or as the text one serialised to. */
    private static boolean flag(Map<?, ?> holder, String key) {
        Object value = holder == null ? null : holder.get(key);
        return value instanceof Boolean bool ? bool : "true".equals(String.valueOf(value));
    }
}
