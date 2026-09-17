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

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.dirigible.components.ide.template.service.model.JavaLiterals;
import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.RelationIntent;

/**
 * The value vocabulary of a {@code posts:} {@code set:} entry - the forms the parser accepts and
 * the Java the generator renders, in one place so the two cannot drift.
 *
 * <p>
 * The vocabulary is closed on purpose. A value the renderer did not recognise used to be passed
 * through verbatim into the generated assignment, so an authored {@code Note: issued} - YAML having
 * stripped the quotes long before the renderer saw it - emitted {@code row.Note = issued;} and the
 * whole generated module stopped compiling (dirigible #7246). A plain constant is therefore
 * rendered as an escaped Java string literal, and a value that reads as an EXPRESSION the renderer
 * cannot compile is refused at parse time rather than turned into a string that would silently be
 * the wrong value.
 *
 * <p>
 * A constant is also rendered for the TYPE of the column it is written into (dirigible #7287).
 * Every intent numeric-with-scale type is a {@code java.math.BigDecimal} in the generated entity
 * and {@code long} is a {@code Long}, so a bare {@code -3.5} / {@code 2} emitted the un-compilable
 * {@code row.Quantity = -3.5;} / {@code row.Sequence = 2;} for exactly the same reason the text
 * case did. {@link #targetType} reads the column's type off the target entity and
 * {@link #expression} renders to it; a constant that column cannot hold at all ({@code issued} into
 * a decimal, a fraction into a long) is named by {@link #typeMismatch} and refused at parse.
 */
public final class PostSetSupport {

    /** A negated per-item copy: {@code -item.<Field>}. */
    private static final Pattern NEGATED_ITEM = Pattern.compile("^-\\s*item\\.(\\w+)$");

    /** A per-item copy: {@code item.<Field>}. */
    private static final Pattern ITEM = Pattern.compile("^item\\.(\\w+)$");

    /** A copy off the source record: {@code source.<Field>}. */
    private static final Pattern SOURCE = Pattern.compile("^source\\.(\\w+)$");

    /** A number - an integer (an FK id, a direction) or a decimal. */
    private static final Pattern NUMBER = Pattern.compile("^-?\\d+(\\.\\d+)?$");

    /**
     * A value the author quoted so explicitly that the double quotes survived YAML, to force the string
     * reading of an expression-like text.
     *
     * <p>
     * YAML strips one level of quoting before the parser ever sees the scalar, so the authored spelling
     * that arrives here is {@code '"Receipt.Store"'} - single quotes wrapping the literal double quotes
     * - and NOT {@code "Receipt.Store"}, which arrives as the bare dotted path and is refused. That is
     * the spelling {@link #quotedSpelling} prints and the refusal names.
     */
    private static final Pattern QUOTED = Pattern.compile("^\"[^\"]*\"$");

    /**
     * The Java type of the column a {@code set:} entry writes, as far as the rendering of a constant
     * cares: the intent types collapse onto the handful of Java types the generated entity declares
     * ({@code decimal} and {@code double} are both a {@code BigDecimal} column, a to-one relation is
     * its target's integer key).
     */
    public enum TargetType {

        /** A {@code String} column. */
        STRING,

        /** An {@code Integer} column - a plain {@code integer} field, or a to-one relation's FK. */
        INTEGER,

        /** A {@code Long} column. */
        LONG,

        /** A {@code java.math.BigDecimal} column - an intent {@code decimal} or {@code double}. */
        DECIMAL,

        /** A {@code Boolean} column. */
        BOOLEAN,

        /** A {@code LocalDate} / {@code Instant} column - no constant stands in for one here. */
        TEMPORAL,

        /** Not resolvable here (no such column on the target): rendered as it always was. */
        UNKNOWN
    }

    /**
     * Not instantiable.
     */
    private PostSetSupport() {}

    /**
     * The type of the target column a {@code set:} key writes, or {@link TargetType#UNKNOWN} when the
     * target entity does not declare it.
     *
     * <p>
     * The key is authored in the target's presentation spelling ({@code Quantity}) while the field is
     * declared in the model's ({@code quantity}), so both sides are compared PascalCased - the same
     * spelling the generated assignment uses. A to-one relation is its target's key column: an
     * {@code Integer}, or a {@code Long} where that entity's key is a {@code long}.
     *
     * @param entity the target entity ({@code into}), or null
     * @param byName every entity of the model by name, to read a relation target's key type
     * @param key the authored {@code set:} key
     * @return the column's type, never null
     */
    public static TargetType targetType(EntityIntent entity, Map<String, EntityIntent> byName, String key) {
        if (entity == null || key == null) {
            return TargetType.UNKNOWN;
        }
        String property = IntentNaming.pascalCase(key);
        for (FieldIntent field : entity.getFields()) {
            if (field.getName() != null && property.equals(IntentNaming.pascalCase(field.getName()))) {
                return ofIntentType(field.getType());
            }
        }
        for (RelationIntent relation : entity.getRelations()) {
            boolean toOne = "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
            if (!toOne || relation.getName() == null || !property.equals(IntentNaming.pascalCase(relation.getName()))) {
                continue;
            }
            EntityIntent referenced = byName == null ? null : byName.get(relation.getTo());
            FieldIntent primaryKey = referenced == null ? null : IntentEntities.primaryKeyOf(referenced);
            TargetType keyType = primaryKey == null ? TargetType.INTEGER : ofIntentType(primaryKey.getType());
            return keyType == TargetType.LONG ? TargetType.LONG : TargetType.INTEGER;
        }
        return TargetType.UNKNOWN;
    }

    /** The intent type vocabulary, collapsed onto the Java type the generated entity declares. */
    private static TargetType ofIntentType(String type) {
        String value = type == null ? "string"
                : type.trim()
                      .toLowerCase(Locale.ROOT);
        return switch (value) {
            case "integer", "int", "relation" -> TargetType.INTEGER;
            case "long" -> TargetType.LONG;
            // `double` is a DECIMAL column like `decimal`, so both properties are BigDecimal.
            case "decimal", "double" -> TargetType.DECIMAL;
            case "boolean" -> TargetType.BOOLEAN;
            case "date", "time", "timestamp", "datetime" -> TargetType.TEMPORAL;
            case "string", "text" -> TargetType.STRING;
            default -> TargetType.UNKNOWN;
        };
    }

    /**
     * Whether a value reads as an expression this renderer cannot compile - a dotted path off anything
     * but {@code item} / {@code source}, or a negation of anything but a per-item copy.
     *
     * <p>
     * Such a value is refused rather than rendered: the author meant a value read from somewhere, so
     * emitting the text as a string constant would put a wrong value in the ledger silently, which is
     * the one outcome worse than a refusal.
     *
     * @param raw the authored value
     * @return true when the value must be refused
     */
    public static boolean isUnsupportedExpression(String raw) {
        if (raw == null) {
            return false;
        }
        String value = raw.trim();
        if (isCopy(value) || NUMBER.matcher(value)
                                   .matches()
                || QUOTED.matcher(value)
                         .matches()) {
            return false;
        }
        return value.indexOf('.') >= 0 || value.startsWith("-");
    }

    /**
     * The remedy an unsupported expression's refusal prints: the authored value spelled so that the
     * double quotes SURVIVE YAML and reach {@link #QUOTED} - single quotes wrapping double quotes.
     *
     * <p>
     * The refusal used to print {@code "Receipt.Store"}, which YAML unquotes back into the very dotted
     * path being refused, so an author who followed the remedy got the identical refusal (dirigible
     * #7287).
     *
     * @param raw the authored value
     * @return the spelling to print
     */
    public static String quotedSpelling(String raw) {
        return "'\"" + (raw == null ? "" : raw.trim()) + "\"'";
    }

    /**
     * Why the target column cannot hold this constant, phrased for a parse refusal, or null when it
     * can.
     *
     * <p>
     * A constant the column's Java type cannot take is not renderable in any spelling: a text into a
     * {@code BigDecimal}, a fraction into a {@code Long}, anything but {@code true} / {@code false}
     * into a {@code Boolean}, any constant at all into a date. Each one used to reach {@code javac} as
     * a bare literal or a string literal and fail the compile of the whole generated module, which is
     * the class of defect the closed vocabulary exists to end. A copy ({@code item.} / {@code source.}
     * / {@code null}) is not type-checked here - the two columns' types are the author's business, and
     * a mismatch there is a compile error this rule cannot see.
     *
     * @param raw the authored value
     * @param type the target column's type
     * @return the reason, or null when the constant is renderable
     */
    public static String typeMismatch(String raw, TargetType type) {
        if (raw == null || type == null || type == TargetType.UNKNOWN || type == TargetType.STRING) {
            return null;
        }
        String value = raw.trim();
        if (isCopy(value) || "null".equals(value)) {
            return null;
        }
        boolean number = NUMBER.matcher(value)
                               .matches();
        return switch (type) {
            case INTEGER, LONG -> {
                if (!number) {
                    yield "the column is a whole number, and [" + value + "] is not one";
                }
                yield value.indexOf('.') < 0 ? null : "the column is a whole number, and [" + value + "] has a fraction";
            }
            case DECIMAL -> number ? null : "the column is a decimal, and [" + value + "] is not a number";
            case BOOLEAN -> "true".equals(value) || "false".equals(value) ? null
                    : "the column is a boolean, and [" + value + "] is not true or false";
            case TEMPORAL -> "the column is a date/time, which no constant can be written to here"
                    + " - copy it from item.<Field> or source.<Field>";
            default -> null;
        };
    }

    /**
     * The Java expression a {@code set:} value renders to, over the {@code source} record and - in a
     * {@code forEach} rule - the {@code item} row, for a target column whose type is not resolvable.
     *
     * @param raw the authored value
     * @return the Java expression
     */
    public static String expression(String raw) {
        return expression(raw, TargetType.UNKNOWN);
    }

    /**
     * The Java expression a {@code set:} value renders to, over the {@code source} record and - in a
     * {@code forEach} rule - the {@code item} row, TYPED to the target column.
     *
     * <p>
     * The recognised forms are {@code item.<Field>}, {@code -item.<Field>} (null-safe),
     * {@code source.<Field>}, a number, {@code true} / {@code false} / {@code null}, and a value the
     * author quoted explicitly. A constant renders as a literal of the column's own Java type -
     * {@code new java.math.BigDecimal("-3.5")}, {@code 2L}, {@code 2}, {@code "2"} - never as a bare
     * literal of the wrong type, and never as a bare identifier; neither compiles.
     *
     * @param raw the authored value
     * @param type the target column's type
     * @return the Java expression
     */
    public static String expression(String raw, TargetType type) {
        if (raw == null) {
            return "null";
        }
        TargetType target = type == null ? TargetType.UNKNOWN : type;
        String value = raw.trim();
        Matcher negated = NEGATED_ITEM.matcher(value);
        if (negated.matches()) {
            String access = "item." + IntentNaming.pascalCase(negated.group(1));
            return access + " == null ? null : " + access + ".negate()";
        }
        Matcher item = ITEM.matcher(value);
        if (item.matches()) {
            return "item." + IntentNaming.pascalCase(item.group(1));
        }
        Matcher source = SOURCE.matcher(value);
        if (source.matches()) {
            return "source." + IntentNaming.pascalCase(source.group(1));
        }
        if ("null".equals(value)) {
            return "null";
        }
        if (NUMBER.matcher(value)
                  .matches()) {
            return number(value, target);
        }
        if ("true".equals(value) || "false".equals(value)) {
            return target == TargetType.STRING ? literal(value) : value;
        }
        if (QUOTED.matcher(value)
                  .matches()) {
            return literal(value.substring(1, value.length() - 1));
        }
        return literal(value);
    }

    /**
     * A numeric constant as a literal of the target column's Java type.
     *
     * <p>
     * A fraction into a whole-number column, and a number into a boolean or a date column, are refused
     * at parse ({@link #typeMismatch}) - so those branches are unreachable through a parsed model. They
     * still render something that COMPILES rather than something that does not, because the generator
     * is also reachable from a model built in code, and one mis-valued column beats a generated module
     * that will not build.
     */
    private static String number(String value, TargetType type) {
        boolean whole = value.indexOf('.') < 0;
        return switch (type) {
            case DECIMAL -> "new java.math.BigDecimal(\"" + value + "\")";
            case LONG -> whole ? value + "L" : "new java.math.BigDecimal(\"" + value + "\").longValue()";
            case INTEGER -> whole ? value : "new java.math.BigDecimal(\"" + value + "\").intValue()";
            case STRING -> literal(value);
            // A bare number is what an unresolvable column (an FK the target declares by another name)
            // always got, and a boolean / temporal column is refused at parse before it reaches here.
            default -> value;
        };
    }

    /** A constant as an escaped Java string literal. */
    private static String literal(String value) {
        return '"' + JavaLiterals.escape(value) + '"';
    }

    /** Whether the value is a copy off the source record or the per-item row, not a constant. */
    private static boolean isCopy(String value) {
        return NEGATED_ITEM.matcher(value)
                           .matches()
                || ITEM.matcher(value)
                       .matches()
                || SOURCE.matcher(value)
                         .matches();
    }
}
