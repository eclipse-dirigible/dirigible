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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.dirigible.components.intent.model.CheckIntent;
import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.RelationIntent;
import org.eclipse.dirigible.components.intent.model.TransitionIntent;

/**
 * The condition of a {@code checks: requiredWhen} entry - the grammar the parser refuses on and the
 * typed reading the generator emits, in one place so the two cannot drift.
 *
 * <p>
 * What this class produces is DATA, not code (issue #7405): a condition becomes typed terms and a
 * {@code compare} literal becomes its reading, and the model carries those. The Java is rendered
 * from them a layer out, by {@code JavaLiterals} - the same split a property default has always
 * had. The declarative glue lists carry the very same terms since issue #7425, so there is no Java
 * rendered anywhere in this class any more.
 *
 * <p>
 * The condition is a closed set of equality comparisons, ANDed, over the record's own properties or
 * a one-hop {@code Relation.property} (issue #7495). It is deliberately not an expression language:
 * a condition the generator cannot compile would leave the value required unconditionally, i.e. a
 * {@code required} nobody authored, and that failure is silent in exactly the way this module
 * refuses everywhere else.
 *
 * <p>
 * The comparison is typed against the property's DECLARED type rather than generically, because a
 * boxed comparison across types is silently always-false: {@code Objects.equals(Long, int)} never
 * holds, so a guard on a {@code long} column would switch the rule off and report nothing. That is
 * also why only the types with an exact equality are guardable at all - a decimal, a double or a
 * date is compared for equality by nobody who means it.
 */
public final class CheckSupport {

    /**
     * One comparison of a condition: a property of the record - or, for a {@code requiredWhen} /
     * {@code forbidWhen}, a one-hop {@code Relation.field}, so a child can test its parent and a
     * document its counterparty - against a literal: a number, a quoted string, a bare word (a status
     * name is already its seed id here, resolved before the typed mapping) or a boolean. The glue
     * {@code event.when} guard stays record-local and refuses a dotted property at term validation.
     */
    public static final Pattern TERM =
            Pattern.compile("\\s*(\\w+(?:\\.\\w+)?)\\s*(==|!=)\\s*('[^']*'|\"[^\"]*\"|-?\\d+|[A-Za-z_][A-Za-z0-9_\\-]*)\\s*");

    /** The field types a condition may compare - those with an exact, type-safe equality. */
    public static final Set<String> GUARD_TYPES = Set.of("string", "text", "integer", "int", "long", "boolean");

    /**
     * The guardable types whose values are whole numbers. They are guardable at any width, so a
     * comparison against one is rendered numerically rather than as a boxed equality (the term's
     * {@code numericKey} flag).
     */
    public static final Set<String> NUMERIC_GUARD_TYPES = Set.of("integer", "int", "long");

    /**
     * The type of a term that tests whether a value is there at all ({@code Payslip != null}, #7555) -
     * the one comparison meaningful for a value of ANY type, and the only way a condition can say "this
     * row is linked to something": no literal of the property's type stands for "unset".
     */
    public static final String NULL_TEST_TYPE = "null";

    /**
     * The types a condition over a document's LINES may compare besides {@link #GUARD_TYPES} (issue
     * #7560): an amount or a rate, compared BY VALUE - {@code vatRate == 0} is the rule the kind exists
     * for, and a boxed {@code equals} on a decimal column would never hold.
     */
    public static final Set<String> DECIMAL_TYPES = Set.of("decimal", "double");

    /** The local a line is read off in a generated items loop. */
    public static final String ITEM = "item";

    /** The field types a {@code compare} check orders, by the family they compare inside. */
    private static final Map<String, String> COMPARE_FAMILIES = Map.of("date", "date", "timestamp", "timestamp", "integer", "number", "int",
            "number", "long", "number", "decimal", "number", "double", "number");

    /** The owner a term reads off when it reads the record being written, rather than a loaded hop. */
    public static final String RECORD = "entity";

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
            case "decimal", "double" -> value.matches("-?\\d+(\\.\\d+)?") ? "new java.math.BigDecimal(\"" + value + "\")" : null;
            default -> null;
        };
    }

    /**
     * Reads a whole condition - one comparison or the ANDed list - into the NEUTRAL terms a model
     * carries (issue #7405), every comparison typed against its property's DECLARED type.
     *
     * <p>
     * This is the one reader of a typed guard - the model's {@code checks} and the declarative glue
     * lists' event guards (issue #7425) carry what it produces, so the grammar, the type rule and the
     * refusals cannot drift into two answers.
     *
     * <p>
     * A term names where it reads from ({@code owner}), what it reads ({@code property}), whether the
     * comparison is an equality, and the typed value - never the code that performs it. A to-one's
     * foreign key carries {@code numericKey}, because its Java width is not knowable here: the column
     * is typed from the TARGET's key, and a cross-model target's key lives in the owner's
     * {@code .model}, where {@code long} is as legal as {@code integer}. {@code Objects.equals(Long,
     * Integer)} never holds, so such a term is compared by value, not boxed - a boxed equality would
     * switch the guard off while looking authored (#7237).
     *
     * @param entity the entity the condition is read off
     * @param byName the local entities by name (a to-one's key type comes from its target)
     * @param when the authored condition - a comparison, a list of them, or {@code null}
     * @return the terms, or {@code null} when there is no condition or a comparison does not read (the
     *         parser reports it; a condition silently degraded to {@code true} is the failure both call
     *         sites exist to refuse)
     */
    public static List<Map<String, Object>> conditionTerms(EntityIntent entity, Map<String, EntityIntent> byName, Object when) {
        if (entity == null) {
            return null;
        }
        List<Map<String, Object>> terms = new ArrayList<>();
        for (String term : terms(when)) {
            Comparison comparison = parse(term);
            Map<String, Object> read = comparison == null ? null : recordTerm(entity, byName, comparison);
            if (read == null) {
                return null;
            }
            terms.add(read);
        }
        return terms.isEmpty() ? null : terms;
    }

    /**
     * Reads a condition whose comparisons may also name a one-hop {@code Relation.property} - the
     * {@code requiredWhen} guard over the record a to-one points at (issue #7495: "the customer's
     * registration number is required when the customer is a business") - and the {@code forbidWhen}
     * guard over a child's parent, which reads its condition through this same method (issue #7509), so
     * the two kinds cannot type one term two ways. A bare property reads exactly as
     * {@link #conditionTerms(EntityIntent, Map, Object)} reads it, so a condition that names none
     * produces the same terms; a path is resolved through the check's own walker, which is what loads
     * each hop once for the value and the condition together and null-guards it.
     *
     * <p>
     * A path ending on a to-one compares its foreign key, whose Java width follows the TARGET's key and
     * is therefore compared by value ({@code numericKey}) for the reason the record-local key is
     * (#7237). So is a whole-number literal against a cross-model terminal, whose declared type is not
     * known here: by value it reads right at any integer width, and against a non-numeric column it
     * fails the compile rather than switching the rule off.
     *
     * @param entity the entity the condition is read off
     * @param byName the local entities by name
     * @param walker the walker shared with the check's other paths, which accumulates the hops read
     * @param when the authored condition
     * @return the terms, or {@code null} when there is no condition or a comparison does not read
     */
    public static List<Map<String, Object>> conditionTerms(EntityIntent entity, Map<String, EntityIntent> byName,
            ResolvePathSupport.Walker walker, Object when) {
        if (entity == null) {
            return null;
        }
        List<Map<String, Object>> terms = new ArrayList<>();
        for (String term : terms(when)) {
            Comparison comparison = parse(term);
            if (comparison == null) {
                return null;
            }
            Map<String, Object> read =
                    ResolvePathSupport.isPath(comparison.property()) ? pathTerm(walker.resolve(comparison.property()), comparison)
                            : recordTerm(entity, byName, comparison);
            if (read == null) {
                return null;
            }
            terms.add(read);
        }
        return terms.isEmpty() ? null : terms;
    }

    /**
     * Reads a condition over the document's LINES (issue #7560): each comparison names a field or a
     * to-one of the ITEMS entity and is read off the {@link #ITEM} local of a generated loop. The
     * record-local grammar and type rule apply, widened by {@link #DECIMAL_TYPES} - the condition the
     * kind exists for is an amount or a rate against a literal, compared by value.
     *
     * @param items the document's items entity
     * @param byName the local entities by name
     * @param when the authored condition
     * @return the terms, or {@code null} when there is no condition or a comparison does not read
     */
    public static List<Map<String, Object>> itemConditionTerms(EntityIntent items, Map<String, EntityIntent> byName, Object when) {
        if (items == null) {
            return null;
        }
        List<Map<String, Object>> terms = new ArrayList<>();
        for (String term : terms(when)) {
            Comparison comparison = parse(term);
            Map<String, Object> read = comparison == null ? null : itemTerm(items, byName, comparison);
            if (read == null) {
                return null;
            }
            terms.add(read);
        }
        return terms.isEmpty() ? null : terms;
    }

    /** A comparison over a line's own field or to-one, or {@code null} when it does not read. */
    private static Map<String, Object> itemTerm(EntityIntent items, Map<String, EntityIntent> byName, Comparison comparison) {
        FieldIntent field = field(items, comparison.property());
        RelationIntent relation = field == null ? toOne(items, comparison.property()) : null;
        if (field == null && relation == null) {
            return null;
        }
        String property = IntentNaming.pascalCase(comparison.property());
        if (isNullTest(comparison)) {
            return nullTerm(ITEM, property, comparison);
        }
        String type = guardType(field != null ? field.getType() : relationKeyType(relation, byName));
        if (DECIMAL_TYPES.contains(type)) {
            return term(ITEM, property, comparison, "decimal", false);
        }
        boolean numericKey = field == null && NUMERIC_GUARD_TYPES.contains(type);
        return term(ITEM, property, comparison, numericKey ? "long" : type, numericKey);
    }

    /** A comparison over the record's own field or to-one, or {@code null} when it does not read. */
    private static Map<String, Object> recordTerm(EntityIntent entity, Map<String, EntityIntent> byName, Comparison comparison) {
        FieldIntent field = field(entity, comparison.property());
        RelationIntent relation = field == null ? toOne(entity, comparison.property()) : null;
        if (field == null && relation == null) {
            return null;
        }
        if (isNullTest(comparison)) {
            return nullTerm(RECORD, IntentNaming.pascalCase(comparison.property()), comparison);
        }
        String type = guardType(field != null ? field.getType() : relationKeyType(relation, byName));
        boolean numericKey = field == null && NUMERIC_GUARD_TYPES.contains(type);
        return term(RECORD, IntentNaming.pascalCase(comparison.property()), comparison, numericKey ? "long" : type, numericKey);
    }

    /** A comparison over a resolved one-hop path, or {@code null} when it does not read. */
    private static Map<String, Object> pathTerm(ResolvePathSupport.Path path, Comparison comparison) {
        if (!path.resolved()) {
            return null;
        }
        if (isNullTest(comparison)) {
            return nullTerm(path.owner(), path.property(), comparison);
        }
        String terminal = path.terminalType();
        boolean wholeNumber = unquote(comparison.literal()).matches("-?\\d+") && !isQuoted(comparison.literal());
        if (ResolvePathSupport.RELATION_TERMINAL.equals(terminal) || (terminal == null && wholeNumber)) {
            return term(path.owner(), path.property(), comparison, "long", true);
        }
        String type =
                terminal != null ? guardType(terminal) : isQuoted(comparison.literal()) ? "string" : literalType(comparison.literal());
        if (!GUARD_TYPES.contains(type)) {
            return null;
        }
        return term(path.owner(), path.property(), comparison, type, false);
    }

    /**
     * Whether the comparison tests for an absent value: its literal is the bare word {@code null}. A
     * QUOTED {@code 'null'} stays the four-letter text, so a string field can still be compared with
     * it.
     *
     * @param comparison the parsed comparison
     * @return whether it is a null test
     */
    public static boolean isNullTest(Comparison comparison) {
        return comparison != null && "null".equals(comparison.literal());
    }

    /** A null-test term - no value, typed {@link #NULL_TEST_TYPE}, never compared by key width. */
    private static Map<String, Object> nullTerm(String owner, String property, Comparison comparison) {
        Map<String, Object> term = new LinkedHashMap<>();
        term.put("owner", owner);
        term.put("property", property);
        term.put("equal", comparison.equal());
        term.put("type", NULL_TEST_TYPE);
        term.put("numericKey", false);
        return term;
    }

    private static boolean isQuoted(String literal) {
        return literal.startsWith("'") || literal.startsWith("\"");
    }

    /** The type an unquoted, non-numeric literal against an unknown terminal reads as. */
    private static String literalType(String literal) {
        return "true".equals(literal) || "false".equals(literal) ? "boolean" : "string";
    }

    /**
     * One neutral term - the reading of a comparison, or {@code null} when the authored literal cannot
     * be a value of that type, which is the refusal every caller propagates.
     *
     * @param owner where the value is read from - {@link #RECORD}, or the local a resolved hop loaded
     *        it into
     * @param property the PascalCased property read off that owner
     * @param comparison the parsed comparison
     * @param type the type the comparison is rendered against
     * @param numericKey whether the value is a foreign key of an unknown width, compared by value
     * @return the term, or {@code null}
     */
    public static Map<String, Object> term(String owner, String property, Comparison comparison, String type, boolean numericKey) {
        if (javaLiteral(type, comparison.literal()) == null) {
            return null; // the literal is not a value of that type - the parser reports it
        }
        Map<String, Object> term = new LinkedHashMap<>();
        term.put("owner", owner);
        term.put("property", property);
        term.put("equal", comparison.equal());
        term.put("type", type);
        term.put("value", unquote(comparison.literal()));
        term.put("numericKey", numericKey);
        return term;
    }

    /**
     * Both sides of an {@code agree} check, resolved (issue #7631).
     *
     * <p>
     * Normally a side is the path {@code <relation>.<onProperty>} - the shared value read off the
     * record the relation points at. But a record may carry the shared target DIRECTLY as its own
     * to-one: an {@code OpeningBalance} has {@code Year -> FiscalYear} (which has {@code Company}) and
     * its own {@code Company}, and "the opening balance opens the books of the company its fiscal year
     * belongs to" is {@code Year.Company == Company}. That side is then the record's own foreign key,
     * which the same walker resolves from the bare relation name.
     *
     * @param walker the walker both sides share, so a prefix they have in common loads once
     * @param entity the record the check is declared on
     * @param left the authored relation naming the first side
     * @param right the authored relation naming the second side
     * @param onProperty the shared property
     * @return the two resolved paths, in the authored order - each the hop, or the record's own to-one
     */
    public static ResolvePathSupport.Path[] agreeSides(ResolvePathSupport.Walker walker, EntityIntent entity, String left, String right,
            String onProperty) {
        String[] names = {left, right};
        ResolvePathSupport.Path[] sides = {walker.resolve(left + "." + onProperty), walker.resolve(right + "." + onProperty)};
        for (int i = 0; i < 2; i++) {
            // Only when the OTHER side found the shared property: a side that cannot reach it while
            // nothing else can either is an onProperty that names nothing, and that must keep saying so
            // rather than be read as two bare foreign keys.
            if (!sides[i].resolved() && sides[1 - i].resolved() && toOne(entity, names[i]) != null) {
                sides[i] = walker.resolve(names[i]);
            }
        }
        return sides;
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
     * The comparison family a field type lands in - {@code date}, {@code timestamp} or {@code number} -
     * or {@code null} when the type does not order at all (a string, a {@code month}, a {@code week}:
     * ordering those lexicographically is never what the author meant, so a {@code compare} over one is
     * refused rather than generated).
     *
     * <p>
     * A {@code compare} check's two operands must sit in ONE family, which is what the generated code
     * needs: two temporals compare through their own {@code compareTo} (a {@code LocalDate} does not
     * compare to an {@code Instant}), two numbers by value through {@code BigDecimal} so a
     * {@code decimal} against a {@code long} stays exact.
     *
     * @param type the authored field type, may be {@code null}
     * @return the family, or {@code null}
     */
    public static String compareFamily(String type) {
        return type == null ? null
                : COMPARE_FAMILIES.get(type.trim()
                                           .toLowerCase(Locale.ROOT));
    }

    /**
     * Reads a {@code checks: compare} right-hand LITERAL (issue #7338) against the type of the field it
     * is compared with - the one place the rule lives, so the parser refuses exactly what the generator
     * cannot render.
     *
     * <p>
     * The literal is typed by that field: a numeric field takes a number and compares by VALUE through
     * {@code BigDecimal} (the same exactness the two-field form has); a temporal field takes either a
     * MOMENT - {@code CURRENT_DATE} / {@code CURRENT_TIMESTAMP} / {@code NOW} with at most one signed
     * ISO-8601 offset, the vocabulary a schedule's {@code where:} already carries - or an ISO-8601
     * instant/date literal. The moment is resolved against the clock of the write, not of the
     * generation, and it is rendered in the SHAPE the generated entity column actually carries
     * ({@code LocalDate} for a {@code date}, {@code Instant} for a {@code timestamp}), because a
     * comparison across those two shapes does not compile.
     *
     * <p>
     * A temporal literal must be QUOTED in the YAML: an unquoted {@code 2026-01-01} is resolved by the
     * YAML loader into a date object long before this sees it, and would arrive here as a locale-shaped
     * string nobody authored.
     *
     * @param fieldType the declared type of the field on the left of the comparison
     * @param value the authored literal
     * @return the reading - either a Java expression or the reason it is refused, never both
     */
    public static CompareLiteral compareLiteral(String fieldType, Object value) {
        String family = compareFamily(fieldType);
        if (family == null) {
            return CompareLiteral.refused("field is a [" + fieldType + "] - only dates, timestamps and numbers compare");
        }
        if (value == null) {
            return CompareLiteral.refused("requires a `value`");
        }
        if ("number".equals(family)) {
            java.math.BigDecimal number = decimal(value);
            return number == null
                    ? CompareLiteral.refused("value [" + value + "] is not a number, and a [" + fieldType + "] compares" + " with numbers")
                    : CompareLiteral.of(reading("number", "text", number.toPlainString()));
        }
        boolean date = "date".equals(family);
        String shape = date ? "date" : "timestamp";
        ScheduleSupport.Moment moment = ScheduleSupport.moment(value);
        if (moment != null) {
            boolean momentIsDate = moment.shape() == ScheduleSupport.Moment.Shape.DATE;
            if (momentIsDate != date) {
                return CompareLiteral.refused(
                        "value [" + value + "] names a " + (momentIsDate ? "date" : "timestamp") + " moment, and" + " a [" + fieldType
                                + "] compares with " + (date ? "dates - use CURRENT_DATE" : "timestamps - use CURRENT_TIMESTAMP"));
            }
            Map<String, Object> reading = reading("moment", "shape", shape);
            String offset = moment.duration();
            if (offset == null) {
                return CompareLiteral.of(reading);
            }
            // The offset is PARSED here and carried as the text it parsed from: the grammar - and the
            // refusal of an offset the field's shape cannot take - stays in this class, while the
            // rendering that turns it into a Period or a Duration belongs to whoever emits code.
            if ((date ? period(offset) : duration(offset)) == null) {
                return CompareLiteral.refused("value [" + value + "] carries an offset a [" + fieldType + "] cannot"
                        + (date ? " - a date has no time component" : "") + ": [" + offset + "]");
            }
            reading.put("offset", offset);
            reading.put("forward", moment.forward() ? "true" : "false");
            return CompareLiteral.of(reading);
        }
        String text = String.valueOf(value)
                            .trim();
        try {
            if (date) {
                java.time.LocalDate.parse(text);
            } else {
                java.time.Instant.parse(text);
            }
            Map<String, Object> reading = reading("temporal", "shape", shape);
            reading.put("text", text);
            return CompareLiteral.of(reading);
        } catch (java.time.format.DateTimeParseException ex) {
            return CompareLiteral.refused("value [" + value + "] is neither a moment (CURRENT_DATE / CURRENT_TIMESTAMP / NOW, with at"
                    + " most one signed ISO-8601 offset) nor a quoted ISO-8601 "
                    + (date ? "date (\"2026-01-01\")" : "instant" + " (\"2026-01-01T00:00:00Z\")") + ", and a [" + fieldType
                    + "] compares with those");
        }
    }

    /** A reading, opened with its kind and one further key - the shape every arm above starts from. */
    private static Map<String, Object> reading(String kind, String key, String value) {
        Map<String, Object> reading = new LinkedHashMap<>();
        reading.put("kind", kind);
        reading.put(key, value);
        return reading;
    }

    /** The authored value as an exact decimal, or {@code null} when it does not read as a number. */
    private static java.math.BigDecimal decimal(Object value) {
        try {
            return new java.math.BigDecimal(String.valueOf(value)
                                                  .trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** The offset itself when it reads as a date-only period, or {@code null} when it does not. */
    private static String period(String offset) {
        try {
            java.time.Period.parse(offset);
            return offset;
        } catch (java.time.format.DateTimeParseException ex) {
            return null;
        }
    }

    /** The offset itself when it reads as an instant duration, or {@code null} when it does not. */
    private static String duration(String offset) {
        try {
            java.time.Duration.parse(offset);
            return offset;
        } catch (java.time.format.DateTimeParseException ex) {
            return null;
        }
    }

    /**
     * The reading of a {@code compare} literal: the NEUTRAL description of the value the generated
     * comparison evaluates, or the reason the literal is refused. Exactly one of the two is present - a
     * refused literal has no reading, and a read one has nothing to report.
     *
     * <p>
     * The reading is data, not code (issue #7405). It names what the author wrote, typed against the
     * field it is compared with, and leaves the rendering to whoever emits a language: a {@code {kind:
     * number, text: "0"}}, a {@code {kind: moment, shape: date, offset: "P1D", forward: "true"}}, or a
     * {@code {kind: temporal, shape: timestamp, text: "2026-01-01T00:00:00Z"}}. The model carries it
     * verbatim, and {@code JavaLiterals} in the template layer turns it into the Java the generated
     * check tests - the same split a property default has had all along.
     */
    public static final class CompareLiteral {

        private final Map<String, Object> reading;
        private final String problem;

        private CompareLiteral(Map<String, Object> reading, String problem) {
            this.reading = reading;
            this.problem = problem;
        }

        private static CompareLiteral of(Map<String, Object> reading) {
            return new CompareLiteral(reading, null);
        }

        private static CompareLiteral refused(String problem) {
            return new CompareLiteral(null, problem);
        }

        /**
         * @return whether the literal reads
         */
        public boolean valid() {
            return problem == null;
        }

        /**
         * @return the neutral reading the model carries, or {@code null} when the literal is refused
         */
        public Map<String, Object> reading() {
            return reading;
        }

        /**
         * @return why the literal is refused, as the tail of an author-facing issue, or {@code null}
         */
        public String problem() {
            return problem;
        }
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

    /**
     * The status seed ids a SYSTEM writer moves each entity's {@code function: EntityStatus} FK to,
     * keyed by entity name: a {@code transitions:} button's {@code setStatus} and a {@code processes:}
     * step's {@code setRelationField} on that FK (#7595). Neither comes through a REST controller - the
     * button and the step both write through the repository's targeted {@code updateProperties} - so a
     * check that only the controllers run never fires when either of them moves the record.
     *
     * @param model the whole intent
     * @param setters the model's validated field setters ({@link SetFieldSupport#setters})
     * @return the targeted seed ids per entity name; an entity with none is absent
     */
    public static Map<String, Set<Integer>> systemStatusTargets(IntentModel model, List<SetFieldSupport.Setter> setters) {
        Map<String, Set<Integer>> targets = new HashMap<>();
        for (TransitionIntent transition : model.getTransitions()) {
            if (transition.getForEntity() != null && transition.getSetStatus() != null) {
                targets.computeIfAbsent(transition.getForEntity(), entity -> new TreeSet<>())
                       .add(transition.getSetStatus());
            }
        }
        Map<String, EntityIntent> byName = IntentEntities.byName(model);
        for (SetFieldSupport.Setter setter : setters) {
            EntityIntent entity = byName.get(setter.entity());
            RelationIntent status = entity == null ? null : IntentEntities.entityStatusRelation(entity);
            if (!setter.relation() || status == null || !IntentNaming.pascalCase(status.getName())
                                                                     .equals(setter.field())
                    || setter.value() == null || !setter.value()
                                                        .trim()
                                                        .matches("-?\\d+")) {
                continue;
            }
            targets.computeIfAbsent(setter.entity(), name -> new TreeSet<>())
                   .add(Integer.valueOf(setter.value()
                                              .trim()));
        }
        return targets;
    }

    /**
     * The status a check is gated on: its authored {@code status:}, else - for an UNGATED
     * {@code requiredWhen} - the status its own condition names, when that status is one a system
     * writer moves the record to (#7595). {@code when: "Status == APPROVED"} on an entity a workflow
     * step approves is a rule about the approval; left ungated it is emitted into the controllers'
     * {@code validate()}, which the step never reaches (base-sales-invoices verified it live: an Issue
     * with no reason went through until the author added the gate by hand). Gated, the repository
     * enforces it on every writer, and the BPMN generator keeps the step that writes the status in the
     * completing transaction, so the refusal reaches the person who acted instead of dead-lettering.
     * Both generators read the gate HERE, so they cannot disagree about it.
     *
     * @param entity the entity the check sits on
     * @param check the check
     * @param systemTargets the status ids a system writer moves this entity to
     *        ({@link #systemStatusTargets})
     * @return the gate status, or {@code null} when the check is ungated
     */
    public static Integer effectiveGate(EntityIntent entity, CheckIntent check, Set<Integer> systemTargets) {
        if (check.getStatus() != null) {
            return check.getStatus();
        }
        if (!"requiredWhen".equals(check.getKind()) || systemTargets == null || systemTargets.isEmpty()) {
            return null;
        }
        RelationIntent status = IntentEntities.entityStatusRelation(entity);
        if (status == null) {
            return null;
        }
        String statusProperty = IntentNaming.pascalCase(status.getName());
        for (String term : terms(check.getWhen())) {
            Comparison comparison = parse(term);
            if (comparison == null || !comparison.equal() || ResolvePathSupport.isPath(comparison.property())
                    || !statusProperty.equals(IntentNaming.pascalCase(comparison.property()))) {
                continue;
            }
            String literal = comparison.literal()
                                       .trim();
            if (literal.matches("-?\\d+") && systemTargets.contains(Integer.valueOf(literal))) {
                return Integer.valueOf(literal);
            }
        }
        return null;
    }
}
