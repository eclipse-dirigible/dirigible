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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.RelationIntent;

/**
 * The condition of a {@code checks: requiredWhen} entry - the grammar the parser refuses on and the
 * Java the generator renders, in one place so the two cannot drift.
 *
 * <p>
 * The condition is a closed set of equality comparisons over the record's own properties, ANDed. It
 * is deliberately not an expression language: a condition the generator cannot compile would leave
 * the value required unconditionally, i.e. a {@code required} nobody authored, and that failure is
 * silent in exactly the way this module refuses everywhere else.
 *
 * <p>
 * The comparison is rendered against the property's DECLARED type rather than generically, because
 * a boxed comparison across types is silently always-false: {@code Objects.equals(Long, int)} never
 * holds, so a guard on a {@code long} column would switch the rule off and report nothing. That is
 * also why only the types with an exact equality are guardable at all - a decimal, a double or a
 * date is compared for equality by nobody who means it.
 */
public final class CheckSupport {

    /**
     * One comparison of a condition: a property of the record against a literal - a number, a quoted
     * string, a bare word (a status name is already its seed id here, resolved before the typed
     * mapping) or a boolean.
     */
    public static final Pattern TERM =
            Pattern.compile("\\s*(\\w+)\\s*(==|!=)\\s*('[^']*'|\"[^\"]*\"|-?\\d+|[A-Za-z_][A-Za-z0-9_\\-]*)\\s*");

    /** The field types a condition may compare - those with an exact, type-safe equality. */
    public static final Set<String> GUARD_TYPES = Set.of("string", "text", "integer", "int", "long", "boolean");

    /**
     * The guardable types whose values are whole numbers. They are guardable at any width, so a
     * comparison against one is rendered numerically rather than as a boxed equality - see
     * {@link #numericComparison}.
     */
    public static final Set<String> NUMERIC_GUARD_TYPES = Set.of("integer", "int", "long");

    private CheckSupport() {}

    /**
     * The type a comparison is rendered against: the declared type normalised. A field without a
     * {@code type:} is a string, as it is everywhere else in the DSL, and a type is matched
     * case-insensitively, because {@code Integer} and {@code integer} are the same declaration to the
     * rest of the parser. Neither is an authoring mistake, and neither may reach a
     * {@link java.util.Set#of} lookup raw - {@code Set.of(...).contains(null)} throws.
     *
     * @param type the declared type, or {@code null}
     * @return the normalised type
     */
    public static String guardType(String type) {
        return type == null || type.isBlank() ? "string"
                : type.trim()
                      .toLowerCase(Locale.ROOT);
    }

    /**
     * One parsed comparison.
     *
     * @param property the record's property being compared
     * @param equal whether the comparison is {@code ==} (rather than {@code !=})
     * @param literal the authored literal, quotes included when it carried them
     */
    public record Comparison(String property, boolean equal, String literal) {
    }

    /**
     * The comparisons of a condition - one, or the list form (an implicit AND).
     *
     * @param when the authored condition
     * @return the authored comparison strings, in order
     */
    public static List<String> terms(Object when) {
        if (when == null) {
            return List.of();
        }
        List<String> terms = new ArrayList<>();
        if (when instanceof List<?> list) {
            for (Object term : list) {
                terms.add(term == null ? "" : String.valueOf(term));
            }
        } else {
            terms.add(String.valueOf(when));
        }
        return terms;
    }

    /**
     * Parses one comparison.
     *
     * @param term the authored comparison
     * @return the parsed comparison, or {@code null} when it does not have the shape
     */
    public static Comparison parse(String term) {
        if (term == null) {
            return null;
        }
        Matcher matcher = TERM.matcher(term);
        if (!matcher.matches()) {
            return null;
        }
        return new Comparison(matcher.group(1), "==".equals(matcher.group(2)), matcher.group(3));
    }

    /**
     * The Java literal a comparison against a property of this type is rendered with.
     *
     * @param type the property's declared type ({@code integer}, {@code string}, ...)
     * @param literal the authored literal
     * @return the Java literal, or {@code null} when the authored literal cannot be one of that type
     */
    public static String javaLiteral(String type, String literal) {
        if (type == null || literal == null) {
            return null;
        }
        String value = unquote(literal);
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "string", "text" -> NotificationSupport.quote(value);
            case "integer", "int" -> value.matches("-?\\d+") ? value : null;
            case "long" -> value.matches("-?\\d+") ? value + "L" : null;
            case "boolean" -> "true".equals(value) || "false".equals(value) ? value : null;
            default -> null;
        };
    }

    /**
     * Renders one comparison as a Java boolean expression.
     *
     * @param access the Java expression reading the property
     * @param equal whether the comparison is {@code ==}
     * @param javaLiteral the Java literal from {@link #javaLiteral}
     * @return the expression
     */
    public static String comparison(String access, boolean equal, String javaLiteral) {
        String equals = "java.util.Objects.equals(" + access + ", " + javaLiteral + ")";
        return equal ? equals : "!" + equals;
    }

    /**
     * Renders one comparison of a whole-number property as a NUMERIC comparison rather than a boxed
     * equality - the form a comparison whose Java width is not known here must take.
     *
     * <p>
     * A to-one's guard is such a property: the foreign-key column is typed from the TARGET's key, and
     * when the target belongs to another model that key is only readable from the owner's
     * {@code .model} - where a {@code long} is as legal as an {@code integer}. An
     * {@code Objects.equals(Long, Integer)} never holds, so the boxed form would switch the rule off
     * while looking authored, which is the failure this whole check exists to refuse.
     *
     * @param access the Java expression reading the property
     * @param equal whether the comparison is {@code ==}
     * @param javaLiteral the Java literal from {@link #javaLiteral}, rendered as a {@code long}
     * @return the expression
     */
    public static String numericComparison(String access, boolean equal, String javaLiteral) {
        String equals = "(" + access + " != null && " + access + ".longValue() == " + javaLiteral + ")";
        return equal ? equals : "!" + equals;
    }

    /**
     * Compiles a whole condition - one comparison or the ANDed list - into the Java boolean a generated
     * reader tests, every comparison rendered against its property's DECLARED type.
     *
     * <p>
     * This is the one renderer of a typed guard: the {@code requiredWhen} check it was written for and
     * the {@code event.when} of the declarative glue lists (issue #7289) share it, so the grammar the
     * parser refuses on, the type rule and the Java emitted for it cannot drift into three answers.
     *
     * <p>
     * A to-one's foreign key is a whole number of a width this class cannot know - the column is typed
     * from the TARGET's key, and a cross-model target's key lives in the owner's {@code .model}, where
     * {@code long} is as legal as {@code integer} - so such a comparison is rendered numerically:
     * {@code Objects.equals(Long, Integer)} never holds, and a boxed equality would switch the guard
     * off while looking authored (#7237). A field's own width is declared, so it keeps the exact boxed
     * equality.
     *
     * @param entity the entity the condition is read off
     * @param byName the local entities by name (a to-one's key type comes from its target)
     * @param when the authored condition - a comparison, a list of them, or {@code null}
     * @return the Java expression, or {@code null} when there is no condition or a comparison does not
     *         compile (the parser reports it; a condition silently degraded to {@code true} is the
     *         failure both call sites exist to refuse)
     */
    public static String condition(EntityIntent entity, Map<String, EntityIntent> byName, Object when) {
        if (entity == null) {
            return null;
        }
        List<String> conditions = new ArrayList<>();
        for (String term : terms(when)) {
            Comparison comparison = parse(term);
            if (comparison == null) {
                return null;
            }
            FieldIntent field = field(entity, comparison.property());
            RelationIntent relation = field == null ? toOne(entity, comparison.property()) : null;
            if (field == null && relation == null) {
                return null;
            }
            String type = guardType(field != null ? field.getType() : relationKeyType(relation, byName));
            boolean numericKey = field == null && NUMERIC_GUARD_TYPES.contains(type);
            String literal = javaLiteral(numericKey ? "long" : type, comparison.literal());
            if (literal == null) {
                return null;
            }
            String access = "entity." + IntentNaming.pascalCase(comparison.property());
            conditions.add(
                    numericKey ? numericComparison(access, comparison.equal(), literal) : comparison(access, comparison.equal(), literal));
        }
        return conditions.isEmpty() ? null : String.join(" && ", conditions);
    }

    /**
     * The entity's field of that name, matched case-insensitively - the guard renders the property
     * through {@link IntentNaming#pascalCase}, so the case an author wrote it in never reaches the
     * generated code and must not decide whether the guard is understood at all.
     *
     * @param entity the entity the condition is read off
     * @param name the authored property name
     * @return the field, or {@code null}
     */
    public static FieldIntent field(EntityIntent entity, String name) {
        if (entity == null || entity.getFields() == null || name == null) {
            return null;
        }
        for (FieldIntent field : entity.getFields()) {
            if (name.equalsIgnoreCase(field.getName())) {
                return field;
            }
        }
        return null;
    }

    /**
     * The entity's to-one relation of that name, matched case-insensitively - its foreign key is a
     * property of the record exactly as a field is, and the status guard is the reason a condition may
     * name one at all.
     *
     * @param entity the entity the condition is read off
     * @param name the authored property name
     * @return the relation, or {@code null}
     */
    public static RelationIntent toOne(EntityIntent entity, String name) {
        if (entity == null || entity.getRelations() == null || name == null) {
            return null;
        }
        for (RelationIntent relation : entity.getRelations()) {
            boolean toOne = "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
            if (toOne && name.equalsIgnoreCase(relation.getName())) {
                return relation;
            }
        }
        return null;
    }

    /**
     * The declared type of a to-one relation's foreign key - the target's primary-key type, falling
     * back to the whole number intent keys always are when the target is owned by another model.
     *
     * @param relation the to-one relation
     * @param byName the local entities by name
     * @return the declared type of the foreign key
     */
    public static String relationKeyType(RelationIntent relation, Map<String, EntityIntent> byName) {
        EntityIntent target = relation == null || relation.getTo() == null || byName == null ? null : byName.get(relation.getTo());
        if (target != null && target.getFields() != null) {
            for (FieldIntent field : target.getFields()) {
                if (field.isPrimaryKey() && field.getType() != null) {
                    return field.getType();
                }
            }
        }
        return "integer";
    }

    /**
     * The authored literal without its quotes.
     *
     * @param literal the authored literal
     * @return the value it carries
     */
    public static String unquote(String literal) {
        if (literal.length() >= 2
                && (literal.startsWith("'") && literal.endsWith("'") || literal.startsWith("\"") && literal.endsWith("\""))) {
            return literal.substring(1, literal.length() - 1);
        }
        return literal;
    }
}
