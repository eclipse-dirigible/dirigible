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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Java expressions for the EXPRESSION readings a {@code .glue} carries (issue #7425).
 *
 * <p>
 * The glue is the process description every template reads, Java and JavaScript alike. Its literal
 * keys stopped carrying Java in #7405 / #7406 ({@link JavaLiterals}); what was left were the
 * expressions - a read off a loaded record, a {@code Calc} evaluation, a typed guard, a string
 * concatenation, the print SDK calls, a classifier ternary. The glue now carries each of those as a
 * small tree of readings, every node a map with a {@code kind} and the facts the author declared,
 * and the Java appears only here.
 *
 * <p>
 * The vocabulary is closed: a reading this does not recognise renders as {@code null}, which the
 * binder reads as "nothing to render" rather than as a guess - the generator refuses everything
 * unrenderable while the author is generating, so an unknown kind here is a glue no generation of
 * this build produced. Every number travels as TEXT, because the glue is read back through a JSON
 * parser that types every number as a {@code Double}.
 *
 * <table>
 * <caption>The kinds</caption>
 * <tr>
 * <th>kind</th>
 * <th>keys</th>
 * <th>renders as</th>
 * </tr>
 * <tr>
 * <td>{@code read}</td>
 * <td>{@code owner}, {@code property}</td>
 * <td>{@code source.Number}</td>
 * </tr>
 * <tr>
 * <td>{@code hop}</td>
 * <td>{@code owner}, {@code property}</td>
 * <td>{@code (Customer == null ? null : Customer.Name)}</td>
 * </tr>
 * <tr>
 * <td>{@code local}</td>
 * <td>{@code name}</td>
 * <td>{@code version}</td>
 * </tr>
 * <tr>
 * <td>{@code string}</td>
 * <td>{@code text}</td>
 * <td>{@code "..."}, escaped</td>
 * </tr>
 * <tr>
 * <td>{@code number}</td>
 * <td>{@code text}, optional {@code type} ({@code integer} / {@code long} / {@code decimal})</td>
 * <td>{@code 2}, {@code 2L}, {@code new java.math.BigDecimal("2")}</td>
 * </tr>
 * <tr>
 * <td>{@code boolean}</td>
 * <td>{@code text}</td>
 * <td>{@code true}</td>
 * </tr>
 * <tr>
 * <td>{@code null}</td>
 * <td></td>
 * <td>{@code null}</td>
 * </tr>
 * <tr>
 * <td>{@code now}</td>
 * <td>{@code shape} ({@code date} / {@code timestamp} / {@code month} / {@code week})</td>
 * <td>today in the target field's own shape</td>
 * </tr>
 * <tr>
 * <td>{@code calc}</td>
 * <td>{@code text}, {@code owner}, {@code scale}, optional {@code narrow}</td>
 * <td>{@code Calc.eval("Net - Discount", source, 2)}</td>
 * </tr>
 * <tr>
 * <td>{@code text}</td>
 * <td>{@code of}</td>
 * <td>{@code String.valueOf(...)}</td>
 * </tr>
 * <tr>
 * <td>{@code concat}</td>
 * <td>{@code parts}, optional {@code forceString}</td>
 * <td>the parts joined with {@code +}</td>
 * </tr>
 * <tr>
 * <td>{@code negate}</td>
 * <td>{@code owner}, {@code property}</td>
 * <td>{@code item.Qty == null ? null : item.Qty.negate()}</td>
 * </tr>
 * <tr>
 * <td>{@code convert}</td>
 * <td>{@code type}</td>
 * <td>the posted {@code Object raw} converted to that type</td>
 * </tr>
 * <tr>
 * <td>{@code calcCompare}</td>
 * <td>{@code owner}, {@code property}, {@code equal}, {@code text}</td>
 * <td>{@code Calc.eval("Vat", source, 6).compareTo(new java.math.BigDecimal("0")) != 0}</td>
 * </tr>
 * <tr>
 * <td>{@code ruleCase}</td>
 * <td>{@code by}, {@code owner}, {@code cases} ({@code value} / {@code column}),
 * {@code otherwise}</td>
 * <td>the null-safe classifier ternary over {@code ruleRow}</td>
 * </tr>
 * <tr>
 * <td>{@code defaultLanguage}</td>
 * <td></td>
 * <td>{@code org.eclipse.dirigible.sdk.print.Print.defaultLanguage()}</td>
 * </tr>
 * <tr>
 * <td>{@code languageFrom}</td>
 * <td>{@code owner}, {@code property}</td>
 * <td>the loaded record's language, the platform default when blank</td>
 * </tr>
 * <tr>
 * <td>{@code part}</td>
 * <td>{@code of}, optional {@code format}</td>
 * <td>{@code org.eclipse.dirigible.sdk.print.FileNames.part(...)}</td>
 * </tr>
 * <tr>
 * <td>{@code first}</td>
 * <td>{@code parts}</td>
 * <td>{@code org.eclipse.dirigible.sdk.print.FileNames.first(...)}</td>
 * </tr>
 * <tr>
 * <td>{@code numberOrId}</td>
 * <td>{@code owner}, {@code entity}, {@code key}, optional {@code number}</td>
 * <td>the document number, else the entity name plus the record id</td>
 * </tr>
 * <tr>
 * <td>{@code due}</td>
 * <td>{@code property}, {@code shape}</td>
 * <td>the {@code java.util.Date} a boundary timer binds to</td>
 * </tr>
 * </table>
 */
public final class JavaExpressions {

    /** The SDK arithmetic evaluator, as the events templates that import it name it. */
    private static final String CALC = "Calc";

    /** The same evaluator fully qualified, for a template that does not import it. */
    private static final String QUALIFIED_CALC = "org.eclipse.dirigible.sdk.utils.Calc";

    /** The SDK helper the generated code sanitizes and formats every file-name value with. */
    private static final String FILE_NAMES = "org.eclipse.dirigible.sdk.print.FileNames";

    /** The platform's application language, the fallback every render language resolves to. */
    private static final String DEFAULT_LANGUAGE = "org.eclipse.dirigible.sdk.print.Print.defaultLanguage()";

    /** The moment a task with no expiry date is due - never, in practice. */
    private static final String FAR_FUTURE = "java.util.Date.from(java.time.Instant.parse(\"9999-12-31T00:00:00Z\"))";

    /** The scale a guard's {@code Calc} comparison reads the compared field at. */
    private static final String GUARD_SCALE = "6";

    /**
     * Not instantiable.
     */
    private JavaExpressions() {}

    /**
     * The Java expression a reading renders as, for a template that imports the SDK {@code Calc}.
     *
     * @param reading the reading the glue carries
     * @return the expression, or null when the value is not a reading this renders
     */
    public static String expression(Object reading) {
        return render(reading, false);
    }

    /**
     * The same expression with the SDK {@code Calc} evaluator fully qualified, for a template that does
     * not import it.
     *
     * @param reading the reading the glue carries
     * @return the expression, or null when the value is not a reading this renders
     */
    public static String qualifiedExpression(Object reading) {
        return render(reading, true);
    }

    /**
     * Renders one reading.
     *
     * @param raw the reading
     * @param qualified whether {@code Calc} is spelled fully qualified
     * @return the expression, or null
     */
    private static String render(Object raw, boolean qualified) {
        if (!(raw instanceof Map<?, ?> reading)) {
            return null;
        }
        String kind = text(reading, "kind");
        if (kind == null) {
            return null;
        }
        return switch (kind) {
            case "read" -> access(reading);
            case "hop" -> {
                String owner = text(reading, "owner");
                yield "(" + owner + " == null ? null : " + access(reading) + ")";
            }
            case "local" -> text(reading, "name");
            case "string" -> literal(text(reading, "text"));
            case "number" -> number(text(reading, "text"), text(reading, "type"));
            case "boolean" -> "true".equals(text(reading, "text")) ? "true" : "false";
            case "null" -> "null";
            case "now" -> now(text(reading, "shape"));
            case "calc" -> calc(reading, qualified);
            case "text" -> {
                String of = render(reading.get("of"), qualified);
                yield of == null ? null : "String.valueOf(" + of + ")";
            }
            case "concat" -> concat(reading, qualified);
            case "negate" -> {
                String access = access(reading);
                yield access + " == null ? null : " + access + ".negate()";
            }
            case "convert" -> convert(text(reading, "type"));
            case "calcCompare" -> calcCompare(reading, qualified);
            case "ruleCase" -> ruleCase(reading, qualified);
            case "defaultLanguage" -> DEFAULT_LANGUAGE;
            case "languageFrom" -> {
                String owner = text(reading, "owner");
                String access = access(reading);
                yield owner + " == null || " + access + " == null || " + access + ".isBlank() ? " + DEFAULT_LANGUAGE + " : " + access
                        + ".trim()";
            }
            case "part" -> part(reading, qualified);
            case "first" -> {
                List<String> parts = parts(reading, qualified);
                yield parts == null ? null : FILE_NAMES + ".first(" + String.join(", ", parts) + ")";
            }
            case "numberOrId" -> numberOrId(reading);
            case "due" -> due(reading);
            default -> null;
        };
    }

    /**
     * A property read off its owner.
     *
     * @param reading the reading
     * @return {@code owner.Property}
     */
    private static String access(Map<?, ?> reading) {
        return text(reading, "owner") + "." + text(reading, "property");
    }

    /**
     * A string literal.
     *
     * @param text the text
     * @return the escaped literal, or null for no text
     */
    private static String literal(String text) {
        return text == null ? null : "\"" + JavaLiterals.escape(text) + "\"";
    }

    /**
     * A numeric constant as a literal of the column's Java type - a whole number stays bare (with the
     * {@code L} a long takes), a decimal column takes a {@code BigDecimal}, and a fraction into a
     * whole-number column is narrowed through one so the module still compiles (the parser refuses that
     * shape, so it is reachable only from a model built in code).
     *
     * @param text the number's authored spelling
     * @param type the column's type, or null for the bare spelling
     * @return the literal
     */
    private static String number(String text, String type) {
        if (text == null) {
            return null;
        }
        boolean whole = text.indexOf('.') < 0;
        String decimal = "new java.math.BigDecimal(\"" + JavaLiterals.escape(text) + "\")";
        return switch (type == null ? "" : type) {
            case "decimal" -> decimal;
            case "long" -> whole ? text + "L" : decimal + ".longValue()";
            case "integer" -> whole ? text : decimal + ".intValue()";
            default -> text;
        };
    }

    /**
     * Today, in the target field's own shape: a {@code month} field holds the {@code YYYY-MM} string, a
     * {@code week} the {@code YYYY-Www} ISO week, a {@code timestamp} the {@code Instant}, anything
     * else the {@code LocalDate}. The shape is not cosmetic - none of the four compiles against the
     * others' property type.
     *
     * @param shape the field's temporal kind
     * @return the expression
     */
    private static String now(String shape) {
        return switch (shape == null ? "" : shape) {
            case "month" -> "java.time.YearMonth.now().toString()";
            case "week" -> "String.format(\"%04d-W%02d\", java.time.LocalDate.now().get(java.time.temporal.IsoFields.WEEK_BASED_YEAR), "
                    + "java.time.LocalDate.now().get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR))";
            case "timestamp" -> "java.time.Instant.now()";
            default -> "java.time.LocalDate.now()";
        };
    }

    /**
     * A {@code Calc} evaluation of an authored arithmetic expression over a loaded record, rounded to
     * the target column's scale and narrowed to its Java type where that type is not a
     * {@code BigDecimal}.
     *
     * @param reading the reading
     * @param qualified whether {@code Calc} is spelled fully qualified
     * @return the expression, or null when the reading carries no expression
     */
    private static String calc(Map<?, ?> reading, boolean qualified) {
        String expression = text(reading, "text");
        if (expression == null) {
            return null;
        }
        String scale = text(reading, "scale");
        String narrow = text(reading, "narrow");
        return (qualified ? QUALIFIED_CALC : CALC) + ".eval(\"" + JavaLiterals.escape(expression) + "\", " + text(reading, "owner") + ", "
                + (scale == null ? "2" : scale) + ")" + (narrow == null ? "" : "." + narrow + "Value()");
    }

    /**
     * A string concatenation of the parts - forced to a {@code String} result when a single non-literal
     * part would otherwise decide the type, and the empty literal for no part at all.
     *
     * @param reading the reading
     * @param qualified whether {@code Calc} is spelled fully qualified
     * @return the expression, or null when a part does not render
     */
    private static String concat(Map<?, ?> reading, boolean qualified) {
        List<String> parts = parts(reading, qualified);
        if (parts == null) {
            return null;
        }
        if (parts.isEmpty()) {
            return "\"\"";
        }
        if (parts.size() == 1 && flag(reading, "forceString") && !parts.get(0)
                                                                       .startsWith("\"")) {
            return "\"\" + " + parts.get(0);
        }
        return String.join(" + ", parts);
    }

    /**
     * The rendered {@code parts} of a reading, in order.
     *
     * @param reading the reading
     * @param qualified whether {@code Calc} is spelled fully qualified
     * @return the parts, or null when one does not render
     */
    private static List<String> parts(Map<?, ?> reading, boolean qualified) {
        List<String> rendered = new ArrayList<>();
        if (reading.get("parts") instanceof List<?> parts) {
            for (Object part : parts) {
                String expression = render(part, qualified);
                if (expression == null) {
                    return null;
                }
                rendered.add(expression);
            }
        }
        return rendered;
    }

    /**
     * The conversion of a posted {@code Object raw} to a prompted field's type.
     *
     * @param type the field's intent type, or {@code relation} for a to-one's key
     * @return the expression
     */
    private static String convert(String type) {
        return switch (type == null ? "string" : type) {
            case "relation", "integer", "int" -> "Integer.valueOf(new java.math.BigDecimal(String.valueOf(raw)).intValue())";
            case "long" -> "Long.valueOf(new java.math.BigDecimal(String.valueOf(raw)).longValue())";
            case "decimal" -> "new java.math.BigDecimal(String.valueOf(raw))";
            case "double" -> "Double.valueOf(String.valueOf(raw))";
            case "boolean" -> "Boolean.valueOf(String.valueOf(raw))";
            case "date" -> "java.time.LocalDate.parse(String.valueOf(raw))";
            default -> "String.valueOf(raw)"; // string / text / uuid / month / week
        };
    }

    /**
     * A null-safe numeric guard: the field read through {@code Calc} (a null reads as zero, the
     * calculated-field semantics) and compared exactly, as a {@code BigDecimal}, with the authored
     * number.
     *
     * @param reading the reading
     * @param qualified whether {@code Calc} is spelled fully qualified
     * @return the boolean expression, or null when the reading carries no number
     */
    private static String calcCompare(Map<?, ?> reading, boolean qualified) {
        String property = text(reading, "property");
        String value = text(reading, "text");
        if (property == null || value == null) {
            return null;
        }
        return (qualified ? QUALIFIED_CALC : CALC) + ".eval(\"" + JavaLiterals.escape(property) + "\", " + text(reading, "owner") + ", "
                + GUARD_SCALE + ").compareTo(new java.math.BigDecimal(\"" + JavaLiterals.escape(value) + "\")) "
                + (flag(reading, "equal") ? "==" : "!=") + " 0";
    }

    /**
     * The classifier ternary of a conditional rule column: the rule row's column chosen by the source's
     * {@code by} value, each case compared through {@code Calc}, an unmatched value falling to the
     * {@code otherwise} column or to {@code null} - which the generated handler null-guards.
     *
     * @param reading the reading
     * @param qualified whether {@code Calc} is spelled fully qualified
     * @return the expression, or null when the reading names no classifier
     */
    private static String ruleCase(Map<?, ?> reading, boolean qualified) {
        String classifier = text(reading, "by");
        if (classifier == null) {
            return null;
        }
        String otherwise = text(reading, "otherwise");
        String expression = otherwise == null || otherwise.isEmpty() ? "null" : "ruleRow." + otherwise;
        List<Map<?, ?>> cases = new ArrayList<>();
        if (reading.get("cases") instanceof List<?> declared) {
            for (Object entry : declared) {
                if (entry instanceof Map<?, ?> map) {
                    cases.add(map);
                }
            }
        }
        for (int index = cases.size() - 1; index >= 0; index--) {
            Map<?, ?> entry = cases.get(index);
            String condition = (qualified ? QUALIFIED_CALC : CALC) + ".eval(\"" + JavaLiterals.escape(classifier) + "\", "
                    + text(reading, "owner") + ", " + GUARD_SCALE + ").compareTo(new java.math.BigDecimal(\""
                    + JavaLiterals.escape(text(entry, "value")) + "\")) == 0";
            expression = condition + " ? ruleRow." + text(entry, "column") + " : " + expression;
        }
        return "(" + expression + ")";
    }

    /**
     * One sanitized, optionally formatted, file-name value.
     *
     * @param reading the reading
     * @param qualified whether {@code Calc} is spelled fully qualified
     * @return the expression, or null when the value does not render
     */
    private static String part(Map<?, ?> reading, boolean qualified) {
        String of = render(reading.get("of"), qualified);
        if (of == null) {
            return null;
        }
        String format = text(reading, "format");
        return FILE_NAMES + ".part(" + of + (format == null || format.isEmpty() ? "" : ", " + literal(format)) + ")";
    }

    /**
     * The default name of a rendered document: its own number when the entity declares one (so the
     * customer receives {@code SI00000042.pdf}, not {@code SalesInvoice 42.pdf}), else the entity name
     * plus the record id.
     *
     * @param reading the reading
     * @return the expression
     */
    private static String numberOrId(Map<?, ?> reading) {
        String owner = text(reading, "owner");
        String plain = "\"" + JavaLiterals.escape(text(reading, "entity")) + " \" + " + owner + "." + text(reading, "key");
        String number = text(reading, "number");
        if (number == null || number.isEmpty()) {
            return plain;
        }
        String access = owner + "." + number;
        return "(" + access + " == null || " + access + ".isBlank() ? " + plain + " : " + access + ")";
    }

    /**
     * The {@code java.util.Date} a task's boundary timer binds to: the record's date field read at task
     * entry - the end of that day for a {@code date}, the instant itself for a {@code timestamp} - and
     * the far future when the field is empty.
     *
     * @param reading the reading
     * @return the expression, or null when the reading names no field
     */
    private static String due(Map<?, ?> reading) {
        String property = text(reading, "property");
        if (property == null) {
            return null;
        }
        String access = "entity." + property;
        String value = "timestamp".equals(text(reading, "shape")) ? "java.util.Date.from(" + access + ")"
                : "java.util.Date.from(" + access + ".plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())";
        return access + " == null ? " + FAR_FUTURE + " : " + value;
    }

    /** A reading value as text, or null when it is absent - a glue carries everything as strings. */
    private static String text(Map<?, ?> holder, String key) {
        Object value = holder == null ? null : holder.get(key);
        return value == null ? null : String.valueOf(value);
    }

    /** A reading flag, which reaches here as a boolean or as the text one serialised to. */
    private static boolean flag(Map<?, ?> holder, String key) {
        Object value = holder == null ? null : holder.get(key);
        return value instanceof Boolean bool ? bool : "true".equals(String.valueOf(value));
    }
}
