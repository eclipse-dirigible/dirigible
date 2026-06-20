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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
        Set<String> entityNames = validateEntities(model, issues);
        validateProcesses(model, entityNames, issues);
        validateForms(model, entityNames, issues);
        validateReports(model, entityNames, issues);
        validateSeeds(model, entityNames, issues);
        validateNotifications(model, entityNames, issues);
        validateSchedules(model, entityNames, issues);
        validateIntegrations(model, entityNames, issues);
        validateInbound(model, entityNames, issues);
        validateRollups(model, issues);
        if (!issues.isEmpty()) {
            throw new IntentValidationException(issues);
        }
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
            if (counter == null) {
                issues.add("rollup [" + name + "] field [" + rollup.getField() + "] is not a field of parent [" + via.getTo() + "]");
            } else if (!INTEGER_PK_TYPES.contains(counter.getType())) {
                issues.add("rollup [" + name + "] field [" + rollup.getField() + "] must be an integer type to hold a count");
            }
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

    private static Set<String> validateEntities(IntentModel model, List<String> issues) {
        Set<String> entityNames = new HashSet<>();
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
                if (relation.isComposition() && !"manyToOne".equals(relation.getKind()) && !"oneToOne".equals(relation.getKind())) {
                    issues.add("entity [" + entity.getName() + "] relation [" + relation.getName()
                            + "] is marked composition but only a manyToOne/oneToOne relation can be a composition");
                }
                if (relation.getTo() == null || relation.getTo()
                                                        .isBlank()) {
                    issues.add("entity [" + entity.getName() + "] relation [" + relation.getName() + "] has no target");
                } else if (!entityNames.contains(relation.getTo())) {
                    issues.add("entity [" + entity.getName() + "] relation [" + relation.getName() + "] points to unknown entity ["
                            + relation.getTo() + "]");
                }
            }
        }
        return entityNames;
    }

    private static void validateProcesses(IntentModel model, Set<String> entityNames, List<String> issues) {
        Set<String> processNames = new HashSet<>();
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
            for (String kind : EVENT_KINDS) {
                Object target = process.getTrigger()
                                       .get(kind);
                if (target != null) {
                    triggerEvents++;
                    if (!entityNames.contains(target.toString())) {
                        issues.add("process [" + process.getName() + "] trigger " + kind + " references unknown entity [" + target + "]");
                    }
                }
            }
            if (triggerEvents > 1) {
                issues.add("process [" + process.getName() + "] trigger must declare at most one of onCreate/onUpdate/onDelete");
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
        }
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
            if (form.getForEntity() != null && !form.getForEntity()
                                                    .isBlank()
                    && !entityNames.contains(form.getForEntity())) {
                issues.add("form [" + form.getName() + "] references unknown entity [" + form.getForEntity() + "]");
            }
        }
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
            if (seed.getRows()
                    .isEmpty()) {
                issues.add("seed [" + seed.getName() + "] has no rows");
            }
        }
    }
}
