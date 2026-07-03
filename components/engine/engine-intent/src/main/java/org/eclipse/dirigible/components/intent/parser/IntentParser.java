/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.dirigible.components.intent.model.DependsOnIntent;
import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.FormIntent;
import org.eclipse.dirigible.components.intent.model.InboundIntent;
import org.eclipse.dirigible.components.intent.model.IntegrationIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.NotificationIntent;
import org.eclipse.dirigible.components.intent.model.ProcessIntent;
import org.eclipse.dirigible.components.intent.model.RelationIntent;
import org.eclipse.dirigible.components.intent.model.ReportIntent;
import org.eclipse.dirigible.components.intent.model.RollupIntent;
import org.eclipse.dirigible.components.intent.model.ScheduleConditionIntent;
import org.eclipse.dirigible.components.intent.model.SettlementIntent;
import org.eclipse.dirigible.components.intent.model.ScheduleIntent;
import org.eclipse.dirigible.components.intent.model.SeedIntent;
import org.eclipse.dirigible.components.intent.model.StepIntent;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.ToNumberPolicy;

/**
 * Parses the YAML payload of a {@code .intent} file into an {@link IntentModel} tree. SnakeYAML
 * loads the document into a generic map; that map is then round-tripped through a plain Gson
 * instance (see {@link #GSON}) so the typed-POJO mapping stays in a single place.
 *
 * <p>
 * SafeConstructor blocks the {@code !!type} / {@code !!new} tags - YAML deserialisation of intents
 * authored by an LLM or pasted from the web must never become a code-execution surface.
 *
 * <p>
 * Structural validation runs after deserialisation: duplicate names, dangling relation targets,
 * unknown field / relation / step kinds, and dangling form-entity references are surfaced via
 * {@link IntentValidationException}. The set of {@link IntentValidationException#getIssues()
 * issues} carries every problem found in one pass rather than failing fast - a usable error message
 * lists everything the author needs to fix.
 */
public final class IntentParser {

    private static final Set<String> FIELD_TYPES =
            Set.of("string", "text", "integer", "int", "long", "decimal", "double", "boolean", "date", "timestamp", "uuid");
    /**
     * Primary keys must be an integer type - the Dirigible model convention is integer identifiers
     * (auto-increment), and a non-integer auto-increment column is invalid SQL on most databases.
     */
    private static final Set<String> INTEGER_PK_TYPES = Set.of("integer", "int", "long");
    /** Numeric field types a sum roll-up (its field / {@code of} / capacity / balance) may use. */
    private static final Set<String> NUMERIC_TYPES = Set.of("integer", "int", "long", "decimal", "double");
    private static final Set<String> RELATION_KINDS = Set.of("oneToMany", "manyToOne", "oneToOne", "manyToMany");
    private static final Set<String> STEP_KINDS = Set.of("userTask", "serviceTask", "decision", "script", "end");
    /** Entity lifecycle events a declarative-glue item (notification, reaction) can bind to. */
    private static final Set<String> EVENT_KINDS = Set.of("onCreate", "onUpdate", "onDelete");
    /** Notification delivery channels supported today. */
    private static final Set<String> NOTIFICATION_CHANNELS = Set.of("email");
    /** Comparison operators a schedule's {@code where} condition may use. */
    private static final Set<String> SCHEDULE_OPERATORS = Set.of("eq", "ne", "gt", "ge", "lt", "le", "like");
    /** HTTP methods an outbound integration may use. */
    private static final Set<String> HTTP_METHODS = Set.of("GET", "POST", "PUT", "PATCH", "DELETE");

    /**
     * Plain Gson for the YAML-Map -> JSON -> POJO round-trip. The platform's {@code JsonHelper} /
     * {@code GsonHelper} cannot be used here: they are configured with
     * {@code excludeFieldsWithoutExposeAnnotation()}, which silently maps every un-annotated model
     * field to null/empty - the parser then "succeeds" with an empty {@link IntentModel} and every
     * generator quietly skips its slice. {@code LONG_OR_DOUBLE} keeps YAML integers integral (seed row
     * {@code id: 1} must render as {@code 1} in the CSV, not {@code 1.0}).
     */
    private static final Gson GSON = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
                                                      .create();

    private IntentParser() {}

    /**
     * Parse and validate the given YAML source.
     *
     * @param yaml the raw YAML content of an {@code .intent} file (may be null or blank)
     * @return the typed model, never null - an empty model is returned for blank input
     * @throws IntentValidationException if structural problems are found in the model
     */
    public static IntentModel parse(String yaml) {
        if (yaml == null || yaml.isBlank()) {
            return new IntentModel();
        }
        Yaml loader = new Yaml(new SafeConstructor(new LoaderOptions()));
        Object tree = loader.load(yaml);
        if (tree == null) {
            return new IntentModel();
        }
        String json = GSON.toJson(tree);
        IntentModel model;
        try {
            model = GSON.fromJson(json, IntentModel.class);
        } catch (JsonSyntaxException ex) {
            // A scalar with the wrong shape (commonly a {..} YAML flow-mapping where a string is
            // expected - e.g. an unquoted brace recipient like `to: {member.email}`) fails the typed
            // mapping here, before validate() runs. Surface it as a normal validation issue so the
            // editor shows a helpful message in its problem list instead of a raw 500.
            throw new IntentValidationException(List.of("intent has a value of the wrong type: " + rootMessage(ex)
                    + " - note that brace interpolation ({...}) is only valid inside quoted subject/body strings;"
                    + " a recipient/path field must be a plain scalar (e.g. `to: member.email`, not `to: {member.email}`)"));
        }
        if (model == null) {
            return new IntentModel();
        }
        validate(model);
        return model;
    }

    /**
     * The deepest cause message - Gson wraps the informative "Expected ... path $...." in its cause.
     */
    private static String rootMessage(Throwable ex) {
        Throwable cause = ex;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() == null ? ex.toString() : cause.getMessage();
    }

    /**
     * Run all structural checks. Collects every issue before throwing so authors get one complete error
     * message rather than playing whack-a-mole.
     */
    private static void validate(IntentModel model) {
        List<String> issues = new ArrayList<>();
        Set<String> usesAliases = validateUses(model, issues);
        Set<String> entityNames = validateEntities(model, usesAliases, issues);
        validateProcesses(model, entityNames, issues);
        validateForms(model, entityNames, issues);
        validateReports(model, entityNames, issues);
        validateSeeds(model, entityNames, issues);
        validateLanguages(model, issues);
        validateNotifications(model, entityNames, issues);
        validateSchedules(model, entityNames, issues);
        validateIntegrations(model, entityNames, issues);
        validateInbound(model, entityNames, issues);
        validateRollups(model, issues);
        validateSettlements(model, issues);
        if (!issues.isEmpty()) {
            throw new IntentValidationException(issues);
        }
    }

    /**
     * Each settlement must reference declared junction / invoice / payment entities; the junction must
     * have a to-one relation to each of them; the named amount / total / paid / pot / order fields must
     * exist; and each {@code match} must be a to-one relation of both the invoice and the payment.
     */
    private static void validateSettlements(IntentModel model, List<String> issues) {
        Map<String, EntityIntent> byName = new HashMap<>();
        for (EntityIntent entity : model.getEntities()) {
            if (entity.getName() != null) {
                byName.put(entity.getName(), entity);
            }
        }
        Set<String> names = new HashSet<>();
        for (SettlementIntent s : model.getSettlements()) {
            String label = s.getName() == null ? "<unnamed>" : s.getName();
            if (s.getName() == null || s.getName()
                                        .isBlank()) {
                issues.add("settlement has no name");
                continue;
            }
            if (!names.add(s.getName())) {
                issues.add("duplicate settlement [" + s.getName() + "]");
            }
            EntityIntent junction = byName.get(s.getJunction());
            EntityIntent invoice = byName.get(s.getInvoice());
            if (junction == null) {
                issues.add("settlement [" + label + "] references unknown junction entity [" + s.getJunction() + "]");
            }
            if (invoice == null) {
                issues.add("settlement [" + label + "] references unknown invoice entity [" + s.getInvoice() + "]");
            }
            if (s.getPayment() == null || s.getPayment()
                                           .isBlank()) {
                issues.add("settlement [" + label + "] must name a payment entity");
            }
            if (junction != null) {
                if (toOneRelationTo(junction, s.getInvoice()) == null) {
                    issues.add("settlement [" + label + "] junction [" + s.getJunction() + "] has no to-one relation to [" + s.getInvoice()
                            + "]");
                }
                if (toOneRelationTo(junction, s.getPayment()) == null) {
                    issues.add("settlement [" + label + "] junction [" + s.getJunction() + "] has no to-one relation to [" + s.getPayment()
                            + "]");
                }
                if (s.getAmount() == null || fieldByName(junction, s.getAmount()) == null) {
                    issues.add("settlement [" + label + "] amount [" + s.getAmount() + "] is not a field of the junction ["
                            + s.getJunction() + "]");
                }
            }
            if (invoice != null) {
                requireField(invoice, s.getTotal(), label, "total", issues);
                requireField(invoice, s.getPaid(), label, "paid", issues);
                requireField(invoice, s.getOrder(), label, "order", issues);
                if (s.getStatus() != null && !s.getStatus()
                                               .isBlank()
                        && toOneRelationByName(invoice, s.getStatus()) == null) {
                    issues.add("settlement [" + label + "] status [" + s.getStatus() + "] is not a to-one relation of [" + s.getInvoice()
                            + "]");
                }
                for (String m : s.getMatch()) {
                    if (toOneRelationByName(invoice, m) == null) {
                        issues.add("settlement [" + label + "] match [" + m + "] is not a to-one relation of the invoice [" + s.getInvoice()
                                + "]");
                    }
                }
            }
        }
    }

    private static void requireField(EntityIntent entity, String field, String label, String role, List<String> issues) {
        if (field == null || fieldByName(entity, field) == null) {
            issues.add("settlement [" + label + "] " + role + " [" + field + "] is not a field of [" + entity.getName() + "]");
        }
    }

    /** The entity's to-one relation whose target is {@code targetEntity}, or null. */
    private static RelationIntent toOneRelationTo(EntityIntent entity, String targetEntity) {
        if (entity.getRelations() == null || targetEntity == null) {
            return null;
        }
        for (RelationIntent relation : entity.getRelations()) {
            if (targetEntity.equals(relation.getTo())
                    && ("manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind()))) {
                return relation;
            }
        }
        return null;
    }

    /**
     * Each schedule must have a unique name, a cron expression, an entity to query, supported
     * {@code where} operators, and a notify action with a valid recipient.
     */
    private static void validateSchedules(IntentModel model, Set<String> entityNames, List<String> issues) {
        Set<String> names = new HashSet<>();
        for (ScheduleIntent schedule : model.getSchedules()) {
            String name = schedule.getName();
            if (name == null || name.isBlank()) {
                issues.add("schedule has no name");
                continue;
            }
            if (!names.add(name)) {
                issues.add("duplicate schedule [" + name + "]");
            }
            if (schedule.getCron() == null || schedule.getCron()
                                                      .isBlank()) {
                issues.add("schedule [" + name + "] has no cron expression");
            }
            if (schedule.getEntity() == null || !entityNames.contains(schedule.getEntity())) {
                issues.add("schedule [" + name + "] queries unknown entity [" + schedule.getEntity() + "]");
            }
            for (ScheduleConditionIntent condition : schedule.getWhere()) {
                if (condition.getField() == null || condition.getField()
                                                             .isBlank()) {
                    issues.add("schedule [" + name + "] has a where-condition with no field");
                }
                if (!SCHEDULE_OPERATORS.contains(condition.getOp())) {
                    issues.add("schedule [" + name + "] where-condition uses unsupported operator [" + condition.getOp()
                            + "] (supported: eq/ne/gt/ge/lt/le/like)");
                }
            }
            if (schedule.getNotify() == null) {
                issues.add("schedule [" + name + "] has no notify action");
            } else {
                String to = schedule.getNotify()
                                    .getTo();
                if (to == null || to.isBlank()) {
                    issues.add("schedule [" + name + "] notify has no recipient (to)");
                } else if (!to.contains("@") && to.chars()
                                                  .filter(c -> c == '.')
                                                  .count() >= 2) {
                    issues.add("schedule [" + name + "] notify recipient [" + to + "] uses a multi-hop path, which is not supported");
                }
            }
        }
    }

    /**
     * Each roll-up must have a unique name, a child entity, a {@code via} to-one relation of that child
     * pointing at a parent, and an integer {@code field} on the parent to maintain.
     */
    private static void validateRollups(IntentModel model, List<String> issues) {
        java.util.Map<String, EntityIntent> byName = new java.util.HashMap<>();
        for (EntityIntent entity : model.getEntities()) {
            if (entity.getName() != null) {
                byName.put(entity.getName(), entity);
            }
        }
        Set<String> names = new HashSet<>();
        for (RollupIntent rollup : model.getRollups()) {
            String name = rollup.getName();
            if (name == null || name.isBlank()) {
                issues.add("rollup has no name");
                continue;
            }
            if (!names.add(name)) {
                issues.add("duplicate rollup [" + name + "]");
            }
            EntityIntent child = byName.get(rollup.getEntity());
            if (child == null) {
                issues.add("rollup [" + name + "] counts unknown entity [" + rollup.getEntity() + "]");
                continue;
            }
            RelationIntent via = null;
            for (RelationIntent relation : child.getRelations()) {
                boolean toOne = "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
                if (toOne && relation.getName() != null && relation.getName()
                                                                   .equals(rollup.getVia())) {
                    via = relation;
                }
            }
            if (via == null) {
                issues.add("rollup [" + name + "] via [" + rollup.getVia() + "] is not a to-one relation of [" + rollup.getEntity() + "]");
                continue;
            }
            EntityIntent parent = byName.get(via.getTo());
            FieldIntent counter = parent == null ? null : fieldByName(parent, rollup.getField());
            boolean sum = "sum".equals(rollup.getOp());
            if (counter == null) {
                issues.add("rollup [" + name + "] field [" + rollup.getField() + "] is not a field of parent [" + via.getTo() + "]");
            } else if (sum && !NUMERIC_TYPES.contains(counter.getType())) {
                issues.add("rollup [" + name + "] field [" + rollup.getField() + "] must be a numeric type to hold a sum");
            } else if (!sum && !INTEGER_PK_TYPES.contains(counter.getType())) {
                issues.add("rollup [" + name + "] field [" + rollup.getField() + "] must be an integer type to hold a count");
            }
            if (sum) {
                // sum needs a numeric child field to add up; capacity / balance (optional) are numeric parent
                // fields and status (optional) a to-one relation of the parent - see the balance/status roll-up.
                FieldIntent of = fieldByName(child, rollup.getOf());
                if (rollup.getOf() == null || rollup.getOf()
                                                    .isBlank()) {
                    issues.add("rollup [" + name + "] with op sum must declare `of` (the child field to sum)");
                } else if (of == null) {
                    issues.add("rollup [" + name + "] of [" + rollup.getOf() + "] is not a field of [" + rollup.getEntity() + "]");
                } else if (!NUMERIC_TYPES.contains(of.getType())) {
                    issues.add("rollup [" + name + "] of [" + rollup.getOf() + "] must be a numeric field to sum");
                }
                requireNumericParentField(parent, rollup.getCapacity(), name, "capacity", via.getTo(), issues);
                requireNumericParentField(parent, rollup.getBalance(), name, "balance", via.getTo(), issues);
                if (rollup.getStatus() != null && !rollup.getStatus()
                                                         .isBlank()
                        && (parent == null || toOneRelationByName(parent, rollup.getStatus()) == null)) {
                    issues.add(
                            "rollup [" + name + "] status [" + rollup.getStatus() + "] is not a to-one relation of [" + via.getTo() + "]");
                }
            }
        }
    }

    /** Validate an optional numeric parent field named on a roll-up (capacity / balance). */
    private static void requireNumericParentField(EntityIntent parent, String field, String rollup, String role, String parentName,
            List<String> issues) {
        if (field == null || field.isBlank()) {
            return;
        }
        FieldIntent f = parent == null ? null : fieldByName(parent, field);
        if (f == null || !NUMERIC_TYPES.contains(f.getType())) {
            issues.add("rollup [" + rollup + "] " + role + " [" + field + "] must be a numeric field of [" + parentName + "]");
        }
    }

    private static FieldIntent fieldByName(EntityIntent entity, String name) {
        for (FieldIntent field : entity.getFields()) {
            if (name != null && name.equals(field.getName())) {
                return field;
            }
        }
        return null;
    }

    /**
     * Each inbound webhook must have a unique name, a path, and a declared entity to create from the
     * posted payload.
     */
    private static void validateInbound(IntentModel model, Set<String> entityNames, List<String> issues) {
        Set<String> names = new HashSet<>();
        for (InboundIntent inbound : model.getInbound()) {
            String name = inbound.getName();
            if (name == null || name.isBlank()) {
                issues.add("inbound webhook has no name");
                continue;
            }
            if (!names.add(name)) {
                issues.add("duplicate inbound webhook [" + name + "]");
            }
            if (inbound.getPath() == null || inbound.getPath()
                                                    .isBlank()) {
                issues.add("inbound webhook [" + name + "] has no path");
            }
            if (inbound.getCreate() == null || !entityNames.contains(inbound.getCreate())) {
                issues.add("inbound webhook [" + name + "] creates unknown entity [" + inbound.getCreate() + "]");
            }
        }
    }

    /**
     * Each integration must have a unique name, bind to exactly one entity lifecycle event of a
     * declared entity, use a supported HTTP method, and name a target URL.
     */
    private static void validateIntegrations(IntentModel model, Set<String> entityNames, List<String> issues) {
        Set<String> names = new HashSet<>();
        for (IntegrationIntent integration : model.getIntegrations()) {
            String name = integration.getName();
            if (name == null || name.isBlank()) {
                issues.add("integration has no name");
                continue;
            }
            if (!names.add(name)) {
                issues.add("duplicate integration [" + name + "]");
            }
            int eventCount = 0;
            for (String kind : EVENT_KINDS) {
                Object target = integration.getEvent()
                                           .get(kind);
                if (target != null) {
                    eventCount++;
                    if (!entityNames.contains(target.toString())) {
                        issues.add("integration [" + name + "] " + kind + " references unknown entity [" + target + "]");
                    }
                }
            }
            if (eventCount != 1) {
                issues.add("integration [" + name + "] must declare exactly one of onCreate/onUpdate/onDelete");
            }
            String method = integration.getMethod();
            if (method != null && !method.isBlank() && !HTTP_METHODS.contains(method.trim()
                                                                                    .toUpperCase(Locale.ROOT))) {
                issues.add("integration [" + name + "] has unsupported HTTP method [" + method + "]");
            }
            if (integration.getUrl() == null || integration.getUrl()
                                                           .isBlank()) {
                issues.add("integration [" + name + "] has no url");
            }
        }
    }

    /**
     * Each notification must have a unique name, bind to exactly one entity lifecycle event
     * ({@code onCreate}/{@code onUpdate}/{@code onDelete}) of a declared entity, use a supported
     * channel, and name a recipient. The {@code when} guard and the {@code to} resolver path are
     * carried through to the generator (a later increment), which validates the path against the entity
     * at generation time.
     */
    private static void validateNotifications(IntentModel model, Set<String> entityNames, List<String> issues) {
        Set<String> names = new HashSet<>();
        for (NotificationIntent notification : model.getNotifications()) {
            String name = notification.getName();
            if (name == null || name.isBlank()) {
                issues.add("notification has no name");
                continue;
            }
            if (!names.add(name)) {
                issues.add("duplicate notification [" + name + "]");
            }
            int eventCount = 0;
            for (String kind : EVENT_KINDS) {
                Object target = notification.getEvent()
                                            .get(kind);
                if (target != null) {
                    eventCount++;
                    if (!entityNames.contains(target.toString())) {
                        issues.add("notification [" + name + "] " + kind + " references unknown entity [" + target + "]");
                    }
                }
            }
            if (eventCount != 1) {
                issues.add("notification [" + name + "] must declare exactly one of onCreate/onUpdate/onDelete");
            }
            String channel = notification.getChannel();
            if (channel != null && !channel.isBlank() && !NOTIFICATION_CHANNELS.contains(channel)) {
                issues.add("notification [" + name + "] has unsupported channel [" + channel + "] (supported: email)");
            }
            String to = notification.getTo();
            if (to == null || to.isBlank()) {
                issues.add("notification [" + name + "] has no recipient (to)");
            } else if (!to.contains("@") && to.chars()
                                              .filter(c -> c == '.')
                                              .count() >= 2) {
                // A recipient is a literal address, a direct field, or a one-hop relation.field; multi-hop
                // paths are not supported (the generator resolves a single to-one relation by FK id).
                issues.add("notification [" + name + "] recipient [" + to
                        + "] uses a multi-hop path, which is not supported - use a direct field, a one-hop relation.field, or a literal address");
            }
        }
    }

    /**
     * Each {@code uses[]} entry must name a non-blank, unique model alias. Returns the set of declared
     * aliases so {@link #validateEntities} can resolve cross-model relation targets against it.
     */
    private static Set<String> validateUses(IntentModel model, List<String> issues) {
        Set<String> aliases = new HashSet<>();
        for (org.eclipse.dirigible.components.intent.model.UsesIntent uses : model.getUses()) {
            String alias = uses.getModel();
            if (alias == null || alias.isBlank()) {
                issues.add("uses entry has no model");
                continue;
            }
            if (!aliases.add(alias)) {
                issues.add("duplicate uses model [" + alias + "]");
            }
        }
        return aliases;
    }

    private static Set<String> validateEntities(IntentModel model, Set<String> usesAliases, List<String> issues) {
        Set<String> entityNames = new HashSet<>();
        java.util.Map<String, EntityIntent> byName = new java.util.HashMap<>();
        for (EntityIntent entity : model.getEntities()) {
            if (entity.getName() != null) {
                byName.put(entity.getName(), entity);
            }
        }
        for (EntityIntent entity : model.getEntities()) {
            String name = entity.getName();
            if (name == null || name.isBlank()) {
                issues.add("entity has no name");
                continue;
            }
            if (!entityNames.add(name)) {
                issues.add("duplicate entity [" + name + "]");
            }
            Set<String> fieldNames = new HashSet<>();
            int idCount = 0;
            for (FieldIntent field : entity.getFields()) {
                if (field.getName() == null || field.getName()
                                                    .isBlank()) {
                    issues.add("entity [" + name + "] has a field with no name");
                    continue;
                }
                if (!fieldNames.add(field.getName())) {
                    issues.add("entity [" + name + "] declares field [" + field.getName() + "] twice");
                }
                if (field.getType() != null && !FIELD_TYPES.contains(field.getType()
                                                                          .toLowerCase(Locale.ROOT))) {
                    issues.add("entity [" + name + "] field [" + field.getName() + "] has unknown type [" + field.getType() + "]");
                }
                if (field.isPrimaryKey()) {
                    idCount++;
                    String type = field.getType() == null ? null
                            : field.getType()
                                   .toLowerCase(Locale.ROOT);
                    if (!INTEGER_PK_TYPES.contains(type)) {
                        issues.add("entity [" + name + "] primary-key field [" + field.getName()
                                + "] must be an integer type (integer/int/long) - identifiers are integer by convention"
                                + (type == null ? "" : ", got [" + field.getType() + "]"));
                    }
                }
                if (field.getSize() != null && (field.getSize() < 1 || field.getSize() > 12)) {
                    issues.add("entity [" + name + "] field [" + field.getName() + "] size [" + field.getSize()
                            + "] must be a 12-column grid span between 1 and 12 (typically 3/4/6/12)");
                }
                if (field.getDependsOn() != null) {
                    String subject = "entity [" + name + "] field [" + field.getName() + "]";
                    if (field.isPrimaryKey()) {
                        issues.add(subject + " is a primary key so it cannot declare dependsOn");
                    } else {
                        validateDependsOn(entity, subject, field.getDependsOn(), null, byName, issues);
                    }
                }
            }
            if (idCount > 1) {
                issues.add("entity [" + name + "] declares " + idCount + " primary-key fields - exactly one is allowed");
            }
        }
        for (EntityIntent entity : model.getEntities()) {
            if (entity.getName() == null) {
                continue;
            }
            for (RelationIntent relation : entity.getRelations()) {
                if (relation.getName() == null || relation.getName()
                                                          .isBlank()) {
                    issues.add("entity [" + entity.getName() + "] has a relation with no name");
                    continue;
                }
                if (relation.getKind() != null && !RELATION_KINDS.contains(relation.getKind())) {
                    issues.add("entity [" + entity.getName() + "] relation [" + relation.getName() + "] has unknown kind ["
                            + relation.getKind() + "]");
                }
                if (relation.getSize() != null && (relation.getSize() < 1 || relation.getSize() > 12)) {
                    issues.add("entity [" + entity.getName() + "] relation [" + relation.getName() + "] size [" + relation.getSize()
                            + "] must be a 12-column grid span between 1 and 12 (typically 3/4/6/12)");
                }
                if (relation.isComposition() && !"manyToOne".equals(relation.getKind()) && !"oneToOne".equals(relation.getKind())) {
                    issues.add("entity [" + entity.getName() + "] relation [" + relation.getName()
                            + "] is marked composition but only a manyToOne/oneToOne relation can be a composition");
                }
                boolean crossModel = relation.isCrossModel();
                if (crossModel) {
                    // A cross-model relation references an entity owned by another intent model declared in
                    // uses:. It can only be a to-one association (the FK + dropdown live on this side); it
                    // cannot compose a detail that lives in another model, and its target is validated
                    // against the referenced .model at generation time, not here.
                    if (!usesAliases.contains(relation.getModel())) {
                        issues.add("entity [" + entity.getName() + "] relation [" + relation.getName() + "] references undeclared model ["
                                + relation.getModel() + "] - add it to uses:");
                    }
                    if (!"manyToOne".equals(relation.getKind()) && !"oneToOne".equals(relation.getKind())) {
                        issues.add("entity [" + entity.getName() + "] relation [" + relation.getName() + "] is cross-model (model: "
                                + relation.getModel() + ") so it must be a manyToOne/oneToOne association");
                    }
                    if (relation.isComposition()) {
                        issues.add("entity [" + entity.getName() + "] relation [" + relation.getName()
                                + "] is cross-model so it cannot be a composition - a detail cannot be owned across models");
                    }
                    if (relation.getTo() == null || relation.getTo()
                                                            .isBlank()) {
                        issues.add("entity [" + entity.getName() + "] relation [" + relation.getName() + "] has no target");
                    }
                } else if (relation.getTo() == null || relation.getTo()
                                                               .isBlank()) {
                    issues.add("entity [" + entity.getName() + "] relation [" + relation.getName() + "] has no target");
                } else if (!entityNames.contains(relation.getTo())) {
                    issues.add("entity [" + entity.getName() + "] relation [" + relation.getName() + "] points to unknown entity ["
                            + relation.getTo() + "]");
                }
                if (relation.getDependsOn() != null) {
                    String subject = "entity [" + entity.getName() + "] relation [" + relation.getName() + "]";
                    boolean toOne = "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
                    if (!toOne) {
                        issues.add(subject + " declares dependsOn but only a manyToOne/oneToOne relation can depend on another");
                    } else if (relation.isDocumentStatus()) {
                        issues.add(subject + " is a documentStatus (a read-only pill) so it cannot declare dependsOn");
                    } else {
                        validateDependsOn(entity, subject, relation.getDependsOn(), relation, byName, issues);
                    }
                }
            }
        }
        return entityNames;
    }

    /**
     * A {@code dependsOn} declaration (on a field or a to-one relation) must name a sibling to-one
     * relation as its trigger, and its {@code valueFrom}/{@code filterBy} must resolve to properties of
     * the trigger's / the owning relation's target entity. Cross-model targets are validated against
     * the referenced {@code .model} at generation time, not here (same contract as the relation target
     * itself); a same-model target is checked immediately so a typo fails at parse time.
     */
    private static void validateDependsOn(EntityIntent entity, String subject, DependsOnIntent dependsOn, RelationIntent ownRelation,
            java.util.Map<String, EntityIntent> byName, List<String> issues) {
        String triggerName = dependsOn.getRelation();
        if (triggerName == null || triggerName.isBlank()) {
            issues.add(subject + " dependsOn requires `relation`: the sibling to-one relation that triggers it");
            return;
        }
        if (ownRelation != null && triggerName.equals(ownRelation.getName())) {
            issues.add(subject + " dependsOn cannot reference itself as the trigger");
            return;
        }
        RelationIntent trigger = toOneRelationByName(entity, triggerName);
        if (trigger == null) {
            issues.add(subject + " dependsOn relation [" + triggerName + "] is not a to-one relation of [" + entity.getName() + "]");
            return;
        }
        if (trigger.isDocumentStatus()) {
            issues.add(subject + " dependsOn relation [" + triggerName + "] is a documentStatus (a read-only pill) so it cannot trigger");
        }
        if (ownRelation == null) {
            // A scalar field is auto-populated - it needs the source property and has no option list.
            if (dependsOn.getValueFrom() == null || dependsOn.getValueFrom()
                                                             .isBlank()) {
                issues.add(subject + " dependsOn requires `valueFrom`: the trigger target's property to copy the value from");
            }
            if (dependsOn.getFilterBy() != null && !dependsOn.getFilterBy()
                                                             .isBlank()) {
                issues.add(subject + " dependsOn `filterBy` applies only to a relation (a dropdown) - a field has no option list");
            }
        } else if (isBlank(dependsOn.getValueFrom()) && isBlank(dependsOn.getFilterBy())) {
            issues.add(subject + " dependsOn requires `valueFrom` and/or `filterBy` - with neither, the filter would compare the target's"
                    + " primary key against the trigger's primary key");
        }
        // valueFrom lives on the TRIGGER's target entity; filterBy on the OWNING relation's target.
        validateDependsOnProperty(subject, "valueFrom", dependsOn.getValueFrom(), trigger, byName, issues);
        if (ownRelation != null) {
            validateDependsOnProperty(subject, "filterBy", dependsOn.getFilterBy(), ownRelation, byName, issues);
        }
    }

    private static void validateDependsOnProperty(String subject, String attribute, String property, RelationIntent targetRelation,
            java.util.Map<String, EntityIntent> byName, List<String> issues) {
        if (property == null || property.isBlank() || targetRelation.isCrossModel()) {
            return;
        }
        EntityIntent target = byName.get(targetRelation.getTo());
        if (target == null) {
            return; // the dangling relation target is reported separately
        }
        if (fieldByName(target, property) == null && toOneRelationByName(target, property) == null) {
            issues.add(subject + " dependsOn " + attribute + " [" + property + "] is not a field or to-one relation of ["
                    + targetRelation.getTo() + "]");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static void validateProcesses(IntentModel model, Set<String> entityNames, List<String> issues) {
        Set<String> processNames = new HashSet<>();
        java.util.Map<String, EntityIntent> byName = new java.util.HashMap<>();
        for (EntityIntent entity : model.getEntities()) {
            if (entity.getName() != null) {
                byName.put(entity.getName(), entity);
            }
        }
        for (ProcessIntent process : model.getProcesses()) {
            if (process.getName() == null || process.getName()
                                                    .isBlank()) {
                issues.add("process has no name");
                continue;
            }
            if (!processNames.add(process.getName())) {
                issues.add("duplicate process [" + process.getName() + "]");
            }
            int triggerEvents = 0;
            String triggerEntity = null;
            for (String kind : EVENT_KINDS) {
                Object target = process.getTrigger()
                                       .get(kind);
                if (target != null) {
                    triggerEvents++;
                    triggerEntity = target.toString();
                    if (!entityNames.contains(target.toString())) {
                        issues.add("process [" + process.getName() + "] trigger " + kind + " references unknown entity [" + target + "]");
                    }
                }
            }
            if (triggerEvents > 1) {
                issues.add("process [" + process.getName() + "] trigger must declare at most one of onCreate/onUpdate/onDelete");
            }
            // An optional businessKey flags which trigger-entity field becomes the started process
            // instance's BPM business key; it must be a field of the triggered entity.
            Object businessKey = process.getTrigger()
                                        .get("businessKey");
            FieldIntent businessKeyField = null;
            if (businessKey != null) {
                if (triggerEntity == null) {
                    issues.add("process [" + process.getName()
                            + "] trigger declares businessKey but no onCreate/onUpdate/onDelete event to start on");
                } else {
                    EntityIntent triggered = byName.get(triggerEntity);
                    businessKeyField = triggered == null ? null : fieldByName(triggered, businessKey.toString());
                    if (triggered != null && businessKeyField == null) {
                        issues.add("process [" + process.getName() + "] trigger businessKey [" + businessKey + "] is not a field of ["
                                + triggerEntity + "]");
                    }
                }
            }
            // An optional businessKeyStrategy mints the businessKey field's value when blank. Only
            // "timestamp" (a yyyyMMddHHmmss string) is supported today, and it needs a string field.
            Object strategy = process.getTrigger()
                                     .get("businessKeyStrategy");
            if (strategy != null) {
                if (!"timestamp".equals(strategy.toString())) {
                    issues.add("process [" + process.getName() + "] trigger businessKeyStrategy [" + strategy
                            + "] is not supported (supported: timestamp)");
                } else if (businessKey == null) {
                    issues.add("process [" + process.getName() + "] trigger businessKeyStrategy needs a businessKey field to populate");
                } else if (businessKeyField != null && businessKeyField.getType() != null && !"string".equals(businessKeyField.getType())
                        && !"text".equals(businessKeyField.getType())) {
                    issues.add("process [" + process.getName() + "] trigger businessKey field [" + businessKey
                            + "] must be a string/text field to hold a generated timestamp");
                }
            }
            Set<String> stepNames = new HashSet<>();
            for (StepIntent step : process.getSteps()) {
                if (step.getName() == null || step.getName()
                                                  .isBlank()) {
                    issues.add("process [" + process.getName() + "] has a step with no name");
                    continue;
                }
                if (!stepNames.add(step.getName())) {
                    issues.add("process [" + process.getName() + "] declares step [" + step.getName() + "] twice");
                }
                if (step.getKind() != null && !STEP_KINDS.contains(step.getKind())) {
                    issues.add(
                            "process [" + process.getName() + "] step [" + step.getName() + "] has unknown kind [" + step.getKind() + "]");
                }
            }
            validateDecisionTargets(process, issues);
            validateSetFieldSteps(process, triggerEntity, byName, issues);
            validateTaskFormActions(process, model, issues);
        }
    }

    /**
     * A user task is a <b>decision point</b> exactly when its form offers more than one completing
     * action (e.g. Approve / Reject - the auto-added {@code close} button never completes the task). In
     * that case the task must be <b>immediately followed by a decision</b> that branches on the chosen
     * {@code action}, or the extra buttons would all funnel into the same linear successor and do
     * nothing different - almost always an authoring mistake. A single-action task (e.g. {@code issue})
     * needs no decision: it flows on linearly (typically to a status {@code setField} and the next user
     * task). Enforced so the author sees, at parse time, what the chosen actions actually do.
     */
    private static void validateTaskFormActions(ProcessIntent process, IntentModel model, List<String> issues) {
        Map<String, FormIntent> formsByName = new HashMap<>();
        for (FormIntent form : model.getForms()) {
            if (form.getName() != null) {
                formsByName.put(form.getName(), form);
            }
        }
        List<StepIntent> steps = process.getSteps();
        for (int i = 0; i < steps.size(); i++) {
            StepIntent step = steps.get(i);
            if (!"userTask".equals(step.getKind()) || step.getArgs() == null) {
                continue;
            }
            Object formArg = step.getArgs()
                                 .get("form");
            FormIntent form = formArg == null ? null : formsByName.get(formArg.toString());
            if (form == null) {
                continue;
            }
            List<String> completing = new ArrayList<>();
            for (String action : form.getActions()) {
                if (action != null && !action.isBlank() && !"close".equalsIgnoreCase(action)) {
                    completing.add(action);
                }
            }
            if (completing.size() <= 1) {
                continue; // single (or no) completing action -> linear flow, no decision required
            }
            StepIntent successor = successorStep(step, steps, i);
            if (successor == null || !"decision".equals(successor.getKind())) {
                issues.add("user task [" + step.getName() + "] in process [" + process.getName() + "] uses form [" + form.getName()
                        + "] with multiple actions " + completing + " but is not immediately followed by a decision - a multi-option"
                        + " task must branch on the chosen action via a decision (e.g. `kind: decision, args: { if: \"action == '"
                        + completing.get(0) + "'\", then: ..., else: ... }`), or reduce the form to a single action");
            }
        }
    }

    /**
     * The step a user task flows to: its {@code next} arg when set, otherwise the next declared step.
     */
    private static StepIntent successorStep(StepIntent step, List<StepIntent> steps, int index) {
        Object next = step.getArgs() == null ? null
                : step.getArgs()
                      .get("next");
        if (next != null && !next.toString()
                                 .isBlank()) {
            for (StepIntent candidate : steps) {
                if (next.toString()
                        .equals(candidate.getName())) {
                    return candidate;
                }
            }
            return null; // next names `end` or an unknown step (the latter is reported elsewhere)
        }
        return index + 1 < steps.size() ? steps.get(index + 1) : null;
    }

    /**
     * A {@code serviceTask} declaring {@code setField} must name a {@code string}/{@code text} field of
     * the process's trigger entity and carry a {@code value} (the literal to assign). Any step may
     * carry a {@code next} that routes its outgoing flow to a declared step or {@code end} (used to
     * make two decision branches converge). Without these checks a typo would surface only at runtime.
     */
    private static void validateSetFieldSteps(ProcessIntent process, String triggerEntity, Map<String, EntityIntent> byName,
            List<String> issues) {
        Set<String> stepNames = new HashSet<>();
        for (StepIntent step : process.getSteps()) {
            if (step.getName() != null) {
                stepNames.add(step.getName());
            }
        }
        EntityIntent trigger = triggerEntity == null ? null : byName.get(triggerEntity);
        for (StepIntent step : process.getSteps()) {
            if (step.getName() == null) {
                continue;
            }
            String setField = stepArg(step, "setField");
            if (setField != null && !setField.isBlank()) {
                if (!"serviceTask".equals(step.getKind())) {
                    issues.add("process [" + process.getName() + "] step [" + step.getName() + "] uses setField but is not a serviceTask");
                } else if (trigger == null) {
                    issues.add("process [" + process.getName() + "] step [" + step.getName()
                            + "] uses setField but the process has no trigger entity to set it on");
                } else {
                    FieldIntent field = fieldByName(trigger, setField);
                    if (field == null) {
                        issues.add("process [" + process.getName() + "] step [" + step.getName() + "] setField [" + setField
                                + "] is not a field of [" + triggerEntity + "]");
                    } else if (field.getType() != null && !"string".equals(field.getType()) && !"text".equals(field.getType())) {
                        issues.add("process [" + process.getName() + "] step [" + step.getName() + "] setField [" + setField
                                + "] must be a string/text field (only literal string values are supported)");
                    }
                    if (stepArg(step, "value") == null || stepArg(step, "value").isBlank()) {
                        issues.add("process [" + process.getName() + "] step [" + step.getName() + "] setField [" + setField
                                + "] must declare a value");
                    }
                }
            }
            String setRelationField = stepArg(step, "setRelationField");
            if (setRelationField != null && !setRelationField.isBlank()) {
                if (!"serviceTask".equals(step.getKind()) && !"userTask".equals(step.getKind())) {
                    issues.add("process [" + process.getName() + "] step [" + step.getName()
                            + "] uses setRelationField but is not a serviceTask or userTask");
                } else if (trigger == null) {
                    issues.add("process [" + process.getName() + "] step [" + step.getName()
                            + "] uses setRelationField but the process has no trigger entity to set it on");
                } else {
                    RelationIntent relation = toOneRelationByName(trigger, setRelationField);
                    if (relation == null) {
                        issues.add("process [" + process.getName() + "] step [" + step.getName() + "] setRelationField [" + setRelationField
                                + "] is not a manyToOne/oneToOne relation of [" + triggerEntity + "]");
                    }
                    String value = stepArg(step, "value");
                    if (value == null || value.isBlank()) {
                        issues.add("process [" + process.getName() + "] step [" + step.getName() + "] setRelationField [" + setRelationField
                                + "] must declare a value (the related record id)");
                    } else if (!value.matches("-?\\d+")) {
                        issues.add("process [" + process.getName() + "] step [" + step.getName() + "] setRelationField [" + setRelationField
                                + "] value [" + value + "] must be an integer record id");
                    }
                }
            }
            String delegate = stepArg(step, "delegate");
            if (delegate != null && !delegate.isBlank()) {
                if (!"serviceTask".equals(step.getKind())) {
                    issues.add("process [" + process.getName() + "] step [" + step.getName() + "] uses delegate but is not a serviceTask");
                }
                boolean hasCall = stepArg(step, "call") != null && !stepArg(step, "call").isBlank();
                if ((setField != null && !setField.isBlank()) || (setRelationField != null && !setRelationField.isBlank()) || hasCall) {
                    issues.add("process [" + process.getName() + "] step [" + step.getName()
                            + "] delegate cannot be combined with setField/setRelationField/call");
                }
                Object fields = step.getArgs() == null ? null
                        : step.getArgs()
                              .get("fields");
                if (fields != null && !(fields instanceof Map)) {
                    issues.add("process [" + process.getName() + "] step [" + step.getName()
                            + "] delegate `fields` must be a map of name: value pairs");
                } else if (fields instanceof Map<?, ?> map) {
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        if (entry.getValue() instanceof Map || entry.getValue() instanceof List) {
                            issues.add("process [" + process.getName() + "] step [" + step.getName() + "] delegate field [" + entry.getKey()
                                    + "] must be a scalar value");
                        }
                    }
                }
            }
            String next = stepArg(step, "next");
            if (next != null && !next.isBlank() && !"end".equalsIgnoreCase(next) && !stepNames.contains(next)) {
                issues.add(
                        "process [" + process.getName() + "] step [" + step.getName() + "] `next` references unknown step [" + next + "]");
            }
        }
    }

    /**
     * The to-one ({@code manyToOne}/{@code oneToOne}) relation of the entity with the given name, or
     * null.
     */
    private static RelationIntent toOneRelationByName(EntityIntent entity, String name) {
        if (entity.getRelations() == null) {
            return null;
        }
        for (RelationIntent relation : entity.getRelations()) {
            if (name.equals(relation.getName()) && ("manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind()))) {
                return relation;
            }
        }
        return null;
    }

    /**
     * Decision steps must declare {@code if} and {@code then}; {@code then} and the optional
     * {@code else} must reference a declared step of the same process (or the literal {@code end}).
     * Without this check a typo silently produces BPMN that Flowable rejects on the next
     * synchronization cycle.
     */
    private static void validateDecisionTargets(ProcessIntent process, List<String> issues) {
        Set<String> stepNames = new HashSet<>();
        for (StepIntent step : process.getSteps()) {
            if (step.getName() != null) {
                stepNames.add(step.getName());
            }
        }
        for (StepIntent step : process.getSteps()) {
            if (!"decision".equals(step.getKind()) || step.getName() == null) {
                continue;
            }
            String condition = stepArg(step, "if");
            String thenTarget = stepArg(step, "then");
            if (condition == null || condition.isBlank() || thenTarget == null || thenTarget.isBlank()) {
                issues.add("process [" + process.getName() + "] decision [" + step.getName() + "] must declare both `if` and `then`");
                continue;
            }
            checkDecisionTarget(process, step, "then", thenTarget, stepNames, issues);
            String elseTarget = stepArg(step, "else");
            if (elseTarget != null && !elseTarget.isBlank()) {
                checkDecisionTarget(process, step, "else", elseTarget, stepNames, issues);
            }
        }
    }

    private static void checkDecisionTarget(ProcessIntent process, StepIntent step, String arg, String target, Set<String> stepNames,
            List<String> issues) {
        if (!"end".equalsIgnoreCase(target) && !stepNames.contains(target)) {
            issues.add("process [" + process.getName() + "] decision [" + step.getName() + "] `" + arg + "` references unknown step ["
                    + target + "]");
        }
    }

    private static String stepArg(StepIntent step, String key) {
        Object value = step.getArgs() == null ? null
                : step.getArgs()
                      .get(key);
        return value == null ? null : value.toString();
    }

    private static void validateForms(IntentModel model, Set<String> entityNames, List<String> issues) {
        Map<String, EntityIntent> byName = new HashMap<>();
        for (EntityIntent entity : model.getEntities()) {
            if (entity.getName() != null) {
                byName.put(entity.getName(), entity);
            }
        }
        Set<String> formNames = new HashSet<>();
        for (FormIntent form : model.getForms()) {
            if (form.getName() == null || form.getName()
                                              .isBlank()) {
                issues.add("form has no name");
                continue;
            }
            if (!formNames.add(form.getName())) {
                issues.add("duplicate form [" + form.getName() + "]");
            }
            EntityIntent bound = null;
            if (form.getForEntity() != null && !form.getForEntity()
                                                    .isBlank()) {
                if (!entityNames.contains(form.getForEntity())) {
                    issues.add("form [" + form.getName() + "] references unknown entity [" + form.getForEntity() + "]");
                } else {
                    bound = byName.get(form.getForEntity());
                }
            }
            validateFormRelationFields(form, bound, byName, issues);
            validateFormEditable(form, bound, issues);
        }
    }

    /**
     * An {@code editable} field (the per-field opt-out of a BPM task form's read-only default) must be
     * a plain, displayed field of the bound entity. Any field type is allowed: the generated Writer
     * coerces the form's process variable to the field's Java type ({@code date}/{@code timestamp}/
     * {@code number}/{@code boolean}/{@code string}). A {@code relation.field} can never be editable
     * (editing it would not write back).
     */
    private static void validateFormEditable(FormIntent form, EntityIntent bound, List<String> issues) {
        Set<String> displayed = new HashSet<>(form.getFields());
        for (String field : form.getEditable()) {
            if (field == null || field.isBlank()) {
                continue;
            }
            if (field.indexOf('.') >= 0) {
                issues.add("form [" + form.getName() + "] editable [" + field + "] is a relation.field, which cannot be edited");
                continue;
            }
            if (!displayed.contains(field)) {
                issues.add("form [" + form.getName() + "] editable [" + field + "] is not in the form's fields - only a displayed field"
                        + " can be made editable");
                continue;
            }
            if (bound == null) {
                continue; // the unknown-forEntity issue is already reported above
            }
            FieldIntent bf = fieldByName(bound, field);
            if (bf == null) {
                issues.add("form [" + form.getName() + "] editable [" + field + "] is not a field of [" + form.getForEntity() + "]");
                continue;
            }
            // Any plain entity field type is editable: the generated Writer coerces the form's process
            // variable to the field's Java type (LocalDate / Instant / Integer / Long / BigDecimal /
            // Double / Boolean / String). Only a relation.field (handled above) can never be written back.
        }
    }

    /**
     * A {@code relation.field} form field must be a one-hop to-one relation of the form's bound entity
     * with the field present on the target - so it can be resolved into a process variable at runtime
     * (the same one-hop scope as decision conditions). Multi-hop paths are not supported.
     */
    private static void validateFormRelationFields(FormIntent form, EntityIntent bound, Map<String, EntityIntent> byName,
            List<String> issues) {
        for (String field : form.getFields()) {
            if (field == null || field.indexOf('.') < 0) {
                continue;
            }
            if (bound == null) {
                issues.add("form [" + form.getName() + "] field [" + field
                        + "] uses a relation.field path but the form has no (valid) forEntity to resolve it against");
                continue;
            }
            int dot = field.indexOf('.');
            String relationName = field.substring(0, dot);
            String fieldName = field.substring(dot + 1);
            if (fieldName.indexOf('.') >= 0) {
                issues.add("form [" + form.getName() + "] field [" + field
                        + "] uses a multi-hop path, which is not supported - use a direct field or a one-hop relation.field");
                continue;
            }
            RelationIntent relation = toOneRelation(bound, relationName);
            if (relation == null) {
                issues.add("form [" + form.getName() + "] field [" + field + "] is not a to-one relation.field of [" + form.getForEntity()
                        + "]");
                continue;
            }
            EntityIntent target = byName.get(relation.getTo());
            if (target == null || fieldByName(target, fieldName) == null) {
                issues.add("form [" + form.getName() + "] field [" + field + "] references unknown field [" + fieldName + "] on ["
                        + relation.getTo() + "]");
            }
        }
    }

    private static RelationIntent toOneRelation(EntityIntent owner, String name) {
        for (RelationIntent relation : owner.getRelations()) {
            boolean toOne = "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
            if (toOne && name.equals(relation.getName())) {
                return relation;
            }
        }
        return null;
    }

    private static void validateReports(IntentModel model, Set<String> entityNames, List<String> issues) {
        Set<String> reportNames = new HashSet<>();
        for (ReportIntent report : model.getReports()) {
            if (report.getName() == null || report.getName()
                                                  .isBlank()) {
                issues.add("report has no name");
                continue;
            }
            if (!reportNames.add(report.getName())) {
                issues.add("duplicate report [" + report.getName() + "]");
            }
            if (report.getSource() == null || report.getSource()
                                                    .isBlank()) {
                issues.add("report [" + report.getName() + "] has no source");
            } else if (!entityNames.contains(report.getSource())) {
                issues.add("report [" + report.getName() + "] sources from unknown entity [" + report.getSource() + "]");
            }
        }
    }

    private static void validateSeeds(IntentModel model, Set<String> entityNames, List<String> issues) {
        java.util.Map<String, EntityIntent> byName = new java.util.HashMap<>();
        for (EntityIntent entity : model.getEntities()) {
            if (entity.getName() != null) {
                byName.put(entity.getName(), entity);
            }
        }
        Set<String> seedNames = new HashSet<>();
        for (SeedIntent seed : model.getSeeds()) {
            if (seed.getName() == null || seed.getName()
                                              .isBlank()) {
                issues.add("seed has no name");
                continue;
            }
            if (!seedNames.add(seed.getName())) {
                issues.add("duplicate seed [" + seed.getName() + "]");
            }
            if (seed.getEntity() == null || seed.getEntity()
                                                .isBlank()) {
                issues.add("seed [" + seed.getName() + "] has no entity");
            } else if (!entityNames.contains(seed.getEntity())) {
                issues.add("seed [" + seed.getName() + "] targets unknown entity [" + seed.getEntity() + "]");
            }
            if (seed.isFileSeed()) {
                // The seed data lives in an authored CSV: inline rows are mutually exclusive, and the
                // file must sit in a subfolder - root-level .csv files are owned and scrubbed by the
                // intent regeneration, which would delete the authored data.
                if (!seed.getRows()
                         .isEmpty()) {
                    issues.add("seed [" + seed.getName() + "] declares both `file` and inline `rows` - use exactly one");
                }
                String file = seed.getFile()
                                  .trim();
                if (file.startsWith("/") || file.contains("..")) {
                    issues.add("seed [" + seed.getName() + "] file [" + file + "] must be a project-relative path");
                } else if (!file.contains("/")) {
                    issues.add("seed [" + seed.getName() + "] file [" + file + "] must live in a subfolder (e.g. data/" + file
                            + ") - root-level .csv files are owned and scrubbed by the intent regeneration");
                }
            } else if (seed.getRows()
                           .isEmpty()) {
                issues.add("seed [" + seed.getName() + "] has no rows");
            }
            if (seed.isLanguageSeed()) {
                validateLanguageSeed(seed, byName.get(seed.getEntity()), issues);
            }
        }
    }

    /**
     * A translation seed ({@code language: bg}) targets a multilingual entity's language table: the
     * code is a short lowercase language code, and its rows carry only the base row's {@code id} plus
     * translatable (string/text, non-PK) fields of the entity.
     */
    private static void validateLanguageSeed(SeedIntent seed, EntityIntent entity, List<String> issues) {
        if (!seed.getLanguage()
                 .matches("[a-z]{2,3}")) {
            issues.add("seed [" + seed.getName() + "] language [" + seed.getLanguage()
                    + "] must be a short lowercase language code (e.g. bg)");
        }
        if (entity == null) {
            return; // the unknown entity is reported separately
        }
        if (!entity.isMultilingual()) {
            issues.add("seed [" + seed.getName() + "] carries translations but entity [" + entity.getName()
                    + "] is not multilingual - add `multilingual: true` to the entity");
            return;
        }
        Set<String> allowed = new HashSet<>();
        for (FieldIntent field : entity.getFields()) {
            if (field.getName() == null) {
                continue;
            }
            String type = field.getType() == null ? "string"
                    : field.getType()
                           .toLowerCase(Locale.ROOT);
            if (field.isPrimaryKey() || "string".equals(type) || "text".equals(type)) {
                allowed.add(field.getName());
            }
        }
        for (java.util.Map<String, Object> row : seed.getRows()) {
            for (String key : row.keySet()) {
                if (!allowed.contains(key)) {
                    issues.add("seed [" + seed.getName() + "] row references [" + key
                            + "] which is not the id or a translatable (string/text) field of [" + entity.getName() + "]");
                }
            }
        }
    }

    private static void validateLanguages(IntentModel model, List<String> issues) {
        for (String language : model.getLanguages()) {
            if (language == null || !language.matches("[a-z]{2,3}")) {
                issues.add("languages entry [" + language + "] must be a short lowercase language code (e.g. en, bg)");
            }
        }
    }
}
