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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.InboundIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.RelationIntent;

/**
 * Mapping on <b>arrival</b> - the {@code accept:} gate and the {@code map:} projection of an
 * {@code inbound} entry, the mirror of {@link PayloadSupport}'s declared payload.
 *
 * <p>
 * Without a map an arrival deserializes straight into the entity, which works only when the
 * sender's JSON already <em>is</em> the entity, field for field. A real arrival contract is an
 * envelope:
 *
 * <pre>
 * accept: { type: user.assignment.requested, version: 1 }
 * map:
 *   messageId: messageId
 *   email:     email
 *   tenant:    { lookup: Tenant, by: tenantId, from: tenantId }
 * </pre>
 *
 * Three things that shape the design:
 *
 * <ul>
 * <li><b>A lookup resolves a business key to a relation.</b> The envelope carries a name and the
 * entity stores a foreign key, which on its own is what forced a hand-written consumer even for an
 * otherwise fully modelled arrival. The {@code by:} field must be UNIQUE (or the target's primary
 * key): a non-unique lookup silently picking one of several rows is worse than failing, so it is
 * refused when the intent is read rather than at run time.
 * <li><b>A lookup that matches nothing REJECTS the arrival</b> - it never stores a null relation,
 * and the generated handler logs the value it could not resolve.
 * <li><b>A non-matching {@code accept:} is acknowledged and ignored</b>, with a warning. A sender
 * rolling out a new version must not fill this receiver's error queue.
 * </ul>
 *
 * <p>
 * Everything is pre-rendered here - the gate as one Java boolean expression, each mapped value as a
 * conversion over an {@code Object} local - so the Velocity templates stay shape-only, as every
 * other glue collection does.
 *
 * <p>
 * One invariant those expressions must keep: the envelope reaches the generated code through <b>two
 * different readers</b>. A queue, topic or file payload is parsed by the SDK's Gson, which types
 * every JSON number as a {@code Double}; a webhook's body is BOUND by the platform's Jackson, which
 * types an integral one as an {@code Integer} or {@code Long}. So a number is compared through
 * {@code Number.doubleValue()} rather than cast, and converted through {@code BigDecimal} rather
 * than {@code Integer.parseInt} - which would reject the very {@code "1.0"} the Gson path produces.
 * Every expression here is correct under both readers; keep it that way.
 */
public final class ArrivalSupport {

    /**
     * The field types a {@code by:} may name. The lookup value arrives as JSON text or a number, so a
     * business key is a string or an integer; a temporal key would make the conversion partial (a
     * malformed date reads as null) for no case anybody has.
     */
    private static final Set<String> BY_TYPES = Set.of("string", "text", "uuid", "integer", "int", "long", "month", "week");

    /** The closed key vocabulary of a {@code map:} lookup value. */
    private static final Set<String> LOOKUP_KEYS = Set.of("lookup", "by", "from");

    private ArrivalSupport() {}

    /**
     * One mapped scalar: an entity property filled from an envelope key.
     *
     * @param property the entity's PascalCase property
     * @param from the envelope key, as authored
     * @param fromLiteral the envelope key as a Java string literal
     * @param expression the conversion of the {@code raw} local to the property's type
     */
    public record MapField(String property, String from, String fromLiteral, String expression) {
    }

    /**
     * One business-key lookup: a to-one relation filled from the target row a unique field of it
     * matches.
     *
     * @param property the entity's PascalCase foreign-key property
     * @param local the prefix of the locals the generated block declares
     * @param from the envelope key carrying the business key, as authored
     * @param fromLiteral the envelope key as a Java string literal
     * @param targetEntity the looked-up entity
     * @param targetPerspective the looked-up entity's resolved perspective (its Java package segment)
     * @param byProperty the target's PascalCase property the business key matches
     * @param byValueExpression the conversion of the envelope value to that property's type
     * @param targetKeyProperty the target's PascalCase primary-key property - the value stored
     */
    public record Lookup(String property, String local, String from, String fromLiteral, String targetEntity, String targetPerspective,
            String byProperty, String byValueExpression, String targetKeyProperty) {
    }

    /**
     * A resolved arrival mapping: the gate, the mapped scalars and the business-key lookups, each in
     * declaration order.
     *
     * @param acceptExpression the whole gate as one Java boolean expression, or {@code null} when the
     *        entry declares no {@code accept:}
     * @param acceptSummary the gate in words, for the ignored-message warning
     * @param fields the mapped scalars
     * @param lookups the business-key lookups
     */
    public record Plan(String acceptExpression, String acceptSummary, List<MapField> fields, List<Lookup> lookups) {
    }

    /**
     * Validate an arrival's {@code accept:} and {@code map:} against the entity it creates, collecting
     * every problem rather than stopping at the first.
     *
     * @param inbound the authored arrival
     * @param entity the entity it creates, or {@code null} when it could not be resolved
     * @param byName all LOCAL entities by name
     * @param subject how to name the entry in a message, e.g. {@code inbound [userAssignments]}
     * @param issues the collecting issue list
     */
    public static void validate(InboundIntent inbound, EntityIntent entity, Map<String, EntityIntent> byName, String subject,
            List<String> issues) {
        validateAccept(inbound.getAccept(), subject, issues);
        if (entity == null) {
            return; // the unknown-entity issue is already reported; nothing to check the map against
        }
        for (Map.Entry<String, Object> declared : inbound.getMap()
                                                         .entrySet()) {
            validateMapEntry(declared.getKey(), declared.getValue(), entity, byName, subject, issues);
        }
    }

    /**
     * Translate an arrival's {@code accept:} and {@code map:} into what the generated handler renders.
     *
     * @param inbound the authored arrival
     * @param entity the entity it creates
     * @param byName all LOCAL entities by name
     * @param compositionParents composition-parent map, to resolve a lookup target's perspective
     * @param model the model the arrival belongs to, for settings-aware perspective resolution
     * @return the plan, or {@code null} when the entry declares neither key - which is what keeps an
     *         arrival without them generating byte for byte as before
     * @throws IllegalArgumentException when a declared mapping cannot be resolved; the caller reports
     *         the drop with the reason instead of generating a handler that ingests something else
     */
    public static Plan plan(InboundIntent inbound, EntityIntent entity, Map<String, EntityIntent> byName,
            Map<String, String> compositionParents, IntentModel model) {
        Map<String, Object> accept = inbound.getAccept();
        Map<String, Object> map = inbound.getMap();
        if ((accept.isEmpty() && map.isEmpty()) || entity == null) {
            return null;
        }
        List<MapField> fields = new ArrayList<>();
        List<Lookup> lookups = new ArrayList<>();
        for (Map.Entry<String, Object> declared : map.entrySet()) {
            String name = declared.getKey();
            Object value = declared.getValue();
            if (value instanceof Map<?, ?> lookup) {
                lookups.add(lookup(name, lookup, entity, byName, compositionParents, model));
            } else {
                fields.add(field(name, value, entity, byName));
            }
        }
        return new Plan(accept.isEmpty() ? null : acceptExpression(accept), accept.isEmpty() ? null : acceptSummary(accept), fields,
                lookups);
    }

    /**
     * The glue keys an arrival mapping contributes, always present so a template can branch on them -
     * an undefined Velocity variable renders as its own literal name, so a template must never depend
     * on a key being absent.
     *
     * @param plan the resolved mapping, or {@code null} when the entry declares none
     * @return the {@code hasEnvelope} / {@code hasAccept} / {@code hasMap} keys and their payloads
     */
    public static Map<String, Object> arrivalFields(Plan plan) {
        boolean hasAccept = plan != null && plan.acceptExpression() != null;
        boolean hasMap = plan != null && !(plan.fields()
                                               .isEmpty()
                && plan.lookups()
                       .isEmpty());
        Map<String, Object> fields = new LinkedHashMap<>();
        // The envelope is parsed for either half, so one flag gates the parse and the imports.
        fields.put("hasEnvelope", hasAccept || hasMap);
        fields.put("hasAccept", hasAccept);
        fields.put("acceptExpression", hasAccept ? plan.acceptExpression() : "");
        fields.put("acceptSummary", hasAccept ? plan.acceptSummary() : "");
        fields.put("acceptSummaryLiteral", NotificationSupport.quote(hasAccept ? plan.acceptSummary() : ""));
        fields.put("hasMap", hasMap);
        List<Map<String, Object>> mapped = new ArrayList<>();
        List<Map<String, Object>> looked = new ArrayList<>();
        if (plan != null) {
            for (MapField field : plan.fields()) {
                Map<String, Object> rendered = new LinkedHashMap<>();
                rendered.put("property", field.property());
                rendered.put("from", field.from());
                rendered.put("fromLiteral", field.fromLiteral());
                rendered.put("expression", field.expression());
                mapped.add(rendered);
            }
            for (Lookup lookup : plan.lookups()) {
                Map<String, Object> rendered = new LinkedHashMap<>();
                rendered.put("property", lookup.property());
                rendered.put("local", lookup.local());
                rendered.put("from", lookup.from());
                rendered.put("fromLiteral", lookup.fromLiteral());
                rendered.put("targetEntity", lookup.targetEntity());
                rendered.put("targetPerspective", lookup.targetPerspective());
                rendered.put("byProperty", lookup.byProperty());
                rendered.put("byValueExpression", lookup.byValueExpression());
                rendered.put("targetKeyProperty", lookup.targetKeyProperty());
                looked.add(rendered);
            }
        }
        fields.put("mapFields", mapped);
        fields.put("lookups", looked);
        return fields;
    }

    private static void validateAccept(Map<String, Object> accept, String subject, List<String> issues) {
        for (Map.Entry<String, Object> declared : accept.entrySet()) {
            String key = declared.getKey();
            Object value = declared.getValue();
            if (value instanceof Map || value instanceof Iterable) {
                issues.add(subject + " accept [" + key + "] must be a scalar - a gate compares an envelope key with one value");
                continue;
            }
            if (value == null || (value instanceof String text && text.isBlank())) {
                issues.add(subject + " accept [" + key + "] has no value to gate on");
            }
        }
    }

    private static void validateMapEntry(String name, Object value, EntityIntent entity, Map<String, EntityIntent> byName, String subject,
            List<String> issues) {
        FieldIntent field = fieldOf(entity, name);
        RelationIntent relation = toOneRelation(entity, name);
        if (field == null && relation == null) {
            issues.add(subject + " map [" + name + "] is not a field or a to-one relation of [" + entity.getName() + "]");
            return;
        }
        if (field != null && field.isPrimaryKey()) {
            issues.add(subject + " map [" + name
                    + "] fills the primary key, which is generated on insert - map the arrival's own identifier onto a field of its own"
                    + " (a unique: true one, so a redelivery is refused)");
            return;
        }
        if (value instanceof Map<?, ?> lookup) {
            validateLookup(name, lookup, relation, byName, subject, issues);
            return;
        }
        if (value instanceof Iterable) {
            issues.add(subject + " map [" + name + "] must be an envelope key or a lookup { lookup, by, from }, not a list");
            return;
        }
        if (!(value instanceof String text)) {
            issues.add(subject + " map [" + name + "] must be an envelope key or a lookup { lookup, by, from }");
            return;
        }
        if (text.isBlank()) {
            issues.add(subject + " map [" + name + "] names no envelope key");
        }
    }

    private static void validateLookup(String name, Map<?, ?> lookup, RelationIntent relation, Map<String, EntityIntent> byName,
            String subject, List<String> issues) {
        String where = subject + " map [" + name + "] lookup";
        for (Object key : lookup.keySet()) {
            if (!LOOKUP_KEYS.contains(String.valueOf(key))) {
                issues.add(where + " declares unknown key [" + key + "] - a lookup names lookup, by and from");
            }
        }
        String target = text(lookup.get("lookup"));
        String by = text(lookup.get("by"));
        String from = text(lookup.get("from"));
        if (target == null) {
            issues.add(where + " names no entity to look up");
        }
        if (by == null) {
            issues.add(where + " has no by - the target field the business key matches");
        }
        if (from == null) {
            issues.add(where + " has no from - the envelope key carrying the business key");
        }
        if (relation == null) {
            issues.add(where + " fills [" + name + "], which is not a to-one relation - a lookup resolves a business key to a relation");
            return;
        }
        if (relation.isCrossModel()) {
            issues.add(where + " reads [" + relation.getTo()
                    + "], which must be an entity declared in this model (cross-model lookups are not supported yet)");
            return;
        }
        if (target == null || by == null) {
            return;
        }
        if (!target.equals(relation.getTo())) {
            issues.add(where + " looks up [" + target + "] but [" + name + "] relates to [" + relation.getTo()
                    + "] - a lookup reads the relation's own target");
            return;
        }
        EntityIntent looked = byName.get(target);
        if (looked == null) {
            issues.add(where + " reads unknown entity [" + target + "]");
            return;
        }
        validateBy(by, looked, where, issues);
    }

    /**
     * The {@code by:} field must identify at most one row - a unique field, or the primary key - and
     * its type must be one a JSON business key can carry. Anything else is a lookup that would pick a
     * row.
     */
    private static void validateBy(String by, EntityIntent looked, String where, List<String> issues) {
        FieldIntent field = fieldOf(looked, by);
        if (field == null) {
            issues.add(where + " matches on [" + by + "], which is not a field of [" + looked.getName() + "]");
            return;
        }
        if (!field.isUnique() && !field.isPrimaryKey()) {
            issues.add(where + " matches on [" + by + "], which is not unique on [" + looked.getName()
                    + "] - declare unique: true on it, since a lookup that could match several rows would silently pick one");
        }
        if (!BY_TYPES.contains(normalized(field.getType()))) {
            issues.add(where + " matches on the [" + field.getType() + "] field [" + by
                    + "] - a business key is a string or an integer field");
        }
        // A business key that is also translated is a key on a moving target: the read overlay hands the
        // UI the translated value, saving the row writes it back into the base column, and from then on
        // the sender's key resolves nothing - the arrival is REJECTED rather than storing a null FK, so
        // the symptom is a queue of refused messages far from the model that caused it (#6545).
        if (looked.isMultilingual() && field.hasLanguageColumn()) {
            issues.add(where + " matches on [" + by + "], a translated property of the multilingual entity [" + looked.getName()
                    + "] - a business key must not be translated; declare `translatable: false` on it");
        }
    }

    private static MapField field(String name, Object value, EntityIntent entity, Map<String, EntityIntent> byName) {
        String from = text(value);
        if (from == null) {
            throw new IllegalArgumentException("map [" + name + "] names no envelope key");
        }
        FieldIntent field = fieldOf(entity, name);
        String type;
        if (field != null) {
            type = normalized(field.getType());
        } else {
            // A to-one mapped from a plain envelope key carries the target's raw key, so it converts to
            // that key's own type rather than to a string.
            RelationIntent relation = toOneRelation(entity, name);
            if (relation == null) {
                throw new IllegalArgumentException("map [" + name + "] is not a field or a to-one relation of [" + entity.getName() + "]");
            }
            type = keyType(byName.get(relation.getTo()));
        }
        return new MapField(IntentNaming.pascalCase(name), from, NotificationSupport.quote(from), conversion(type, "raw"));
    }

    private static Lookup lookup(String name, Map<?, ?> declared, EntityIntent entity, Map<String, EntityIntent> byName,
            Map<String, String> compositionParents, IntentModel model) {
        RelationIntent relation = toOneRelation(entity, name);
        if (relation == null) {
            throw new IllegalArgumentException("map [" + name + "] lookup fills [" + name + "], which is not a to-one relation");
        }
        String from = text(declared.get("from"));
        String by = text(declared.get("by"));
        EntityIntent looked = byName.get(relation.getTo());
        if (from == null || by == null || looked == null) {
            throw new IllegalArgumentException(
                    "map [" + name + "] lookup of [" + relation.getTo() + "] is incomplete - it names lookup, by and from");
        }
        FieldIntent byField = fieldOf(looked, by);
        if (byField == null) {
            throw new IllegalArgumentException(
                    "map [" + name + "] lookup matches on [" + by + "], which is not a field of [" + looked.getName() + "]");
        }
        String property = IntentNaming.pascalCase(name);
        String local = "lookup" + property;
        return new Lookup(property, local, from, NotificationSupport.quote(from), looked.getName(),
                IntentEntities.resolvePerspective(looked.getName(), compositionParents, model), IntentNaming.pascalCase(by),
                conversion(normalized(byField.getType()), local + "Key"), IntentEntities.keyFieldName(looked));
    }

    /**
     * The gate as one Java boolean expression over the {@code envelope} local. A number is compared
     * through {@code Number.doubleValue()} rather than cast, because the two readers of an envelope
     * disagree on its box: Gson makes every number a {@code Double}, Jackson an integral one an
     * {@code Integer}.
     */
    private static String acceptExpression(Map<String, Object> accept) {
        List<String> checks = new ArrayList<>();
        for (Map.Entry<String, Object> declared : accept.entrySet()) {
            String get = "envelope.get(" + NotificationSupport.quote(declared.getKey()) + ")";
            Object value = declared.getValue();
            if (value instanceof Boolean flag) {
                checks.add((flag ? "Boolean.TRUE" : "Boolean.FALSE") + ".equals(" + get + ")");
            } else if (value instanceof Number number) {
                checks.add("(" + get + " instanceof Number && ((Number) " + get + ").doubleValue() == " + doubleLiteral(number) + ")");
            } else {
                // Compared against the value itself, not its String.valueOf - an absent key must not
                // match by stringifying to "null".
                checks.add(NotificationSupport.quote(String.valueOf(value)) + ".equals(" + get + ")");
            }
        }
        return String.join(" && ", checks);
    }

    private static String acceptSummary(Map<String, Object> accept) {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, Object> declared : accept.entrySet()) {
            parts.add(declared.getKey() + "=" + declared.getValue());
        }
        return String.join(", ", parts);
    }

    /**
     * A gate literal is compared as a double, so an authored {@code 1} must not render as {@code 1L}.
     */
    private static String doubleLiteral(Number number) {
        return new BigDecimal(number.toString()).toPlainString();
    }

    /**
     * The Java expression converting an envelope value to a property's type. An integral property goes
     * through {@code BigDecimal} because the Gson-parsed envelope types every number as a
     * {@code Double}: {@code Integer.parseInt} would reject the very {@code "1.0"} that produces, while
     * the {@code BigDecimal} route reads a Jackson-bound {@code Integer} just as happily. A malformed
     * date or timestamp converts to null rather than throwing - a message is not worth killing over one
     * unreadable field, and the entity's own required-value validation still speaks up.
     */
    private static String conversion(String type, String raw) {
        return switch (type) {
            case "integer", "int" -> "Integer.valueOf(new java.math.BigDecimal(String.valueOf(" + raw + ")).intValue())";
            case "long" -> "Long.valueOf(new java.math.BigDecimal(String.valueOf(" + raw + ")).longValue())";
            // `double` is a DECIMAL column like `decimal`, so both properties are BigDecimal.
            case "decimal", "double" -> "new java.math.BigDecimal(String.valueOf(" + raw + "))";
            case "boolean" -> "Boolean.valueOf(String.valueOf(" + raw + "))";
            case "date" -> "org.eclipse.dirigible.sdk.utils.LenientJavaTime.parseLocalDate(String.valueOf(" + raw + "))";
            case "timestamp" -> "org.eclipse.dirigible.sdk.utils.LenientJavaTime.parseInstant(String.valueOf(" + raw + "))";
            default -> "String.valueOf(" + raw + ")"; // string / text / uuid / month / week
        };
    }

    /** The type of a target's primary key - what a raw foreign key from the envelope converts to. */
    private static String keyType(EntityIntent target) {
        FieldIntent key = IntentEntities.primaryKeyOf(target);
        return key == null ? "integer" : normalized(key.getType());
    }

    private static String normalized(String type) {
        return type == null ? "string" : type;
    }

    private static String text(Object value) {
        if (!(value instanceof String text) || text.isBlank()) {
            return null;
        }
        return text.trim();
    }

    private static FieldIntent fieldOf(EntityIntent entity, String name) {
        for (FieldIntent field : entity.getFields()) {
            if (name != null && name.equals(field.getName())) {
                return field;
            }
        }
        return null;
    }

    private static RelationIntent toOneRelation(EntityIntent entity, String name) {
        for (RelationIntent relation : entity.getRelations()) {
            boolean toOne = "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
            if (toOne && name != null && name.equals(relation.getName())) {
                return relation;
            }
        }
        return null;
    }
}
