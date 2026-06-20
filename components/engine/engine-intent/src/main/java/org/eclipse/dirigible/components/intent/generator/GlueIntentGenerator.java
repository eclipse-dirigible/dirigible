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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.base.helpers.JsonHelper;
import org.eclipse.dirigible.components.intent.generator.ProcessResolverSupport.Resolver;
import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.InboundIntent;
import org.eclipse.dirigible.components.intent.model.IntegrationIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.NotificationIntent;
import org.eclipse.dirigible.components.intent.model.ProcessIntent;
import org.eclipse.dirigible.components.intent.model.RelationIntent;
import org.eclipse.dirigible.components.intent.model.RollupIntent;
import org.eclipse.dirigible.components.intent.model.ScheduleIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Emits the {@code <intent>.glue} file: the process glue-code bindings the language templates need
 * but that belong to neither the EDM nor the BPMN. The EDM describes entities; the BPMN describes
 * control flow; neither knows <i>who starts a process</i> or <i>how its context is populated</i>.
 * Those bindings live here, externalised the same way {@code .report} and {@code .form} were lifted
 * out of the EDM - a standalone artifact today (intent-generated only), with room for a form-based
 * editor later.
 * <p>
 * Two collections, both consumed by the {@code template-application-events-java} ("Application -
 * Glue Code - Java") template:
 * <ul>
 * <li>{@code triggers} - one per process started by {@code trigger: { onCreate: <Entity> }};
 * generates the {@code @Listener} that starts the process on the entity's create event.</li>
 * <li>{@code resolvers} - one per {@code relation.field} referenced in a decision; generates the
 * {@code JavaDelegate} that loads the related entity at the decision and sets the variable the
 * rewritten condition tests (see {@link ProcessResolverSupport}).</li>
 * </ul>
 * The matching BPMN nodes (the resolver service task, the rewritten condition) are emitted by the
 * BPMN generator; the {@code ProcessId} back-reference column stays in the EDM (it is a real
 * persisted field). Idempotent: identical input produces byte-identical output.
 */
@Component
@Order(350)
public class GlueIntentGenerator implements IntentTargetGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlueIntentGenerator.class);

    @Override
    public String name() {
        return "glue";
    }

    @Override
    public void generate(IntentGenerationContext context) {
        IntentModel model = context.getModel();
        Map<String, EntityIntent> byName = IntentEntities.byName(model);
        Map<String, String> compositionParents = IntentEntities.compositionParents(model);

        IntentSettings settings = context.getSettings();
        List<Map<String, Object>> triggers = buildTriggers(model, byName, compositionParents, settings);
        List<Map<String, Object>> resolvers = buildResolvers(model, settings);
        List<Map<String, Object>> notifications = buildNotifications(model, byName, compositionParents, settings);
        List<Map<String, Object>> schedules = buildSchedules(model, byName, compositionParents, settings);
        List<Map<String, Object>> integrations = buildIntegrations(model, byName, compositionParents, settings);
        List<Map<String, Object>> inbound = buildInbound(model, byName, compositionParents, settings);
        List<Map<String, Object>> rollups = buildRollups(model, byName, compositionParents, settings);

        if (triggers.isEmpty() && resolvers.isEmpty() && notifications.isEmpty() && schedules.isEmpty() && integrations.isEmpty()
                && inbound.isEmpty() && rollups.isEmpty()) {
            // No process glue for this intent - any stale .glue is removed by the post-pass scrub.
            return;
        }

        Map<String, Object> glue = new LinkedHashMap<>();
        glue.put("triggers", triggers);
        glue.put("resolvers", resolvers);
        glue.put("notifications", notifications);
        glue.put("schedules", schedules);
        glue.put("integrations", integrations);
        glue.put("inbound", inbound);
        glue.put("rollups", rollups);
        context.writeModelFile(IntentNaming.baseName(context) + ".glue", JsonHelper.toJson(glue));
        LOGGER.debug(
                "Wrote glue with [{}] trigger(s), [{}] resolver(s), [{}] notification(s), [{}] schedule(s), [{}] integration(s),"
                        + " [{}] inbound webhook(s) and [{}] rollup(s)",
                triggers.size(), resolvers.size(), notifications.size(), schedules.size(), integrations.size(), inbound.size(),
                rollups.size());
    }

    private static List<Map<String, Object>> buildTriggers(IntentModel model, Map<String, EntityIntent> byName,
            Map<String, String> compositionParents, IntentSettings settings) {
        List<Map<String, Object>> triggers = new ArrayList<>();
        for (ProcessIntent process : model.getProcesses()) {
            if (process.getName() == null || process.getName()
                                                    .isBlank()) {
                continue;
            }
            String entity = TriggerSupport.triggerEntity(process);
            if (entity == null || entity.isBlank() || !byName.containsKey(entity)) {
                continue;
            }
            if (!settings.shouldGenerate("triggers", process.getName())) {
                LOGGER.info("Settings opt-out: keeping existing listener for trigger [{}] (not generated)", process.getName());
                continue;
            }
            Map<String, Object> trigger = new LinkedHashMap<>();
            trigger.put("process", process.getName());
            trigger.put("entity", entity);
            trigger.put("perspective", IntentEntities.resolvePerspective(entity, compositionParents));
            trigger.put("keyProperty", IntentEntities.keyFieldName(byName.get(entity)));
            // The BPM business key: the flagged trigger field's property, or the primary key when none
            // is flagged (preserving the historical default). keyProperty stays the PK - the listener
            // still loads the entity by id via findById; only the business key may differ.
            String businessKey = TriggerSupport.triggerBusinessKey(process);
            boolean hasBusinessKey = businessKey != null && !businessKey.isBlank();
            trigger.put("businessKeyProperty",
                    hasBusinessKey ? IntentNaming.pascalCase(businessKey) : IntentEntities.keyFieldName(byName.get(entity)));
            // When a businessKeyStrategy is set, the listener mints the value into the flagged field if
            // it is blank (today: a yyyyMMddHHmmss timestamp) and persists it via the existing update.
            boolean generateBusinessKey = hasBusinessKey && "timestamp".equals(TriggerSupport.triggerBusinessKeyStrategy(process));
            trigger.put("generateBusinessKey", String.valueOf(generateBusinessKey));
            trigger.put("topicSuffix", EventBinding.topicSuffix(TriggerSupport.triggerKind(process)));
            trigger.put("guardExpression", NotificationSupport.guard(TriggerSupport.triggerWhen(process)));
            triggers.add(trigger);
        }
        return triggers;
    }

    private static List<Map<String, Object>> buildNotifications(IntentModel model, Map<String, EntityIntent> byName,
            Map<String, String> compositionParents, IntentSettings settings) {
        List<Map<String, Object>> notifications = new ArrayList<>();
        for (NotificationIntent notification : model.getNotifications()) {
            if (notification.getName() == null || notification.getName()
                                                              .isBlank()) {
                continue;
            }
            String entity = NotificationSupport.eventEntity(notification);
            if (entity == null || !byName.containsKey(entity)) {
                continue;
            }
            if (!settings.shouldGenerate("notifications", notification.getName())) {
                LOGGER.info("Settings opt-out: keeping existing listener for notification [{}] (not generated)", notification.getName());
                continue;
            }
            NotificationSupport.Plan plan = NotificationSupport.plan(notification, byName.get(entity), byName, compositionParents);
            if (plan == null) {
                LOGGER.warn("Notification [{}] recipient [{}] is not a resolvable field or relation.field of [{}] - skipping",
                        notification.getName(), notification.getTo(), entity);
                continue;
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", notification.getName());
            entry.put("className", IntentNaming.pascalCase(notification.getName()));
            entry.put("entity", entity);
            entry.put("perspective", IntentEntities.resolvePerspective(entity, compositionParents));
            entry.put("topicSuffix", NotificationSupport.topicSuffix(NotificationSupport.eventKind(notification)));
            entry.put("relationLoads", relationLoads(plan));
            entry.put("guardExpression", plan.guardExpression());
            entry.put("toExpression", plan.toExpression());
            entry.put("subjectExpression", plan.subjectExpression());
            entry.put("bodyExpression", plan.bodyExpression());
            notifications.add(entry);
        }
        return notifications;
    }

    private static List<Map<String, Object>> buildRollups(IntentModel model, Map<String, EntityIntent> byName,
            Map<String, String> compositionParents, IntentSettings settings) {
        List<Map<String, Object>> rollups = new ArrayList<>();
        for (RollupIntent rollup : model.getRollups()) {
            if (rollup.getName() == null || rollup.getName()
                                                  .isBlank()) {
                continue;
            }
            EntityIntent child = byName.get(rollup.getEntity());
            RelationIntent via = child == null ? null : toOneRelation(child, rollup.getVia());
            EntityIntent parent = via == null ? null : byName.get(via.getTo());
            if (parent == null) {
                continue; // parser already reported the bad reference
            }
            if (!settings.shouldGenerate("rollups", rollup.getName())) {
                LOGGER.info("Settings opt-out: keeping existing listeners for rollup [{}] (not generated)", rollup.getName());
                continue;
            }
            String fkProperty = IntentNaming.pascalCase(rollup.getVia());
            Map<String, Object> base = new LinkedHashMap<>();
            base.put("childEntity", rollup.getEntity());
            base.put("childPerspective", IntentEntities.resolvePerspective(rollup.getEntity(), compositionParents));
            base.put("parentEntity", via.getTo());
            base.put("parentPerspective", IntentEntities.resolvePerspective(via.getTo(), compositionParents));
            base.put("fkProperty", fkProperty);
            base.put("countField", IntentNaming.pascalCase(rollup.getField()));
            // Recompute the count for the affected parent from the store on each child event.
            base.put("criteriaExpression", "Criteria.create().eq(\"" + fkProperty + "\", entity." + fkProperty + ")");
            String className = IntentNaming.pascalCase(rollup.getName());
            // Two listeners: recompute when a child is created and when one is deleted.
            rollups.add(rollupEntry(base, className + "RollupOnCreate", ""));
            rollups.add(rollupEntry(base, className + "RollupOnDelete", "-deleted"));
        }
        return rollups;
    }

    private static Map<String, Object> rollupEntry(Map<String, Object> base, String className, String topicSuffix) {
        Map<String, Object> entry = new LinkedHashMap<>(base);
        entry.put("className", className);
        entry.put("topicSuffix", topicSuffix);
        return entry;
    }

    private static RelationIntent toOneRelation(EntityIntent owner, String name) {
        for (RelationIntent relation : owner.getRelations()) {
            boolean toOne = "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
            if (toOne && name != null && name.equals(relation.getName())) {
                return relation;
            }
        }
        return null;
    }

    private static List<Map<String, Object>> buildInbound(IntentModel model, Map<String, EntityIntent> byName,
            Map<String, String> compositionParents, IntentSettings settings) {
        List<Map<String, Object>> inbound = new ArrayList<>();
        for (InboundIntent webhook : model.getInbound()) {
            if (webhook.getName() == null || webhook.getName()
                                                    .isBlank()) {
                continue;
            }
            String entity = webhook.getCreate();
            if (entity == null || !byName.containsKey(entity)) {
                continue;
            }
            if (!settings.shouldGenerate("inbound", webhook.getName())) {
                LOGGER.info("Settings opt-out: keeping existing controller for inbound webhook [{}] (not generated)", webhook.getName());
                continue;
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", webhook.getName());
            entry.put("className", IntentNaming.pascalCase(webhook.getName()));
            entry.put("entity", entity);
            entry.put("perspective", IntentEntities.resolvePerspective(entity, compositionParents));
            entry.put("path", webhook.getPath());
            inbound.add(entry);
        }
        return inbound;
    }

    private static List<Map<String, Object>> buildIntegrations(IntentModel model, Map<String, EntityIntent> byName,
            Map<String, String> compositionParents, IntentSettings settings) {
        List<Map<String, Object>> integrations = new ArrayList<>();
        for (IntegrationIntent integration : model.getIntegrations()) {
            if (integration.getName() == null || integration.getName()
                                                            .isBlank()) {
                continue;
            }
            String entity = EventBinding.entity(integration.getEvent());
            if (entity == null || !byName.containsKey(entity)) {
                continue;
            }
            if (!settings.shouldGenerate("integrations", integration.getName())) {
                LOGGER.info("Settings opt-out: keeping existing listener for integration [{}] (not generated)", integration.getName());
                continue;
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", integration.getName());
            entry.put("className", IntentNaming.pascalCase(integration.getName()));
            entry.put("entity", entity);
            entry.put("perspective", IntentEntities.resolvePerspective(entity, compositionParents));
            entry.put("topicSuffix", EventBinding.topicSuffix(EventBinding.kind(integration.getEvent())));
            entry.put("clientMethod", IntegrationSupport.clientMethod(integration.getMethod()));
            entry.put("hasBody", IntegrationSupport.hasBody(integration.getMethod()));
            entry.put("urlExpression", IntegrationSupport.urlExpression(integration.getUrl()));
            integrations.add(entry);
        }
        return integrations;
    }

    private static List<Map<String, Object>> buildSchedules(IntentModel model, Map<String, EntityIntent> byName,
            Map<String, String> compositionParents, IntentSettings settings) {
        List<Map<String, Object>> schedules = new ArrayList<>();
        for (ScheduleIntent schedule : model.getSchedules()) {
            if (schedule.getName() == null || schedule.getName()
                                                      .isBlank()) {
                continue;
            }
            String entity = schedule.getEntity();
            if (entity == null || !byName.containsKey(entity) || schedule.getNotify() == null) {
                continue;
            }
            if (!settings.shouldGenerate("schedules", schedule.getName())) {
                LOGGER.info("Settings opt-out: keeping existing job for schedule [{}] (not generated)", schedule.getName());
                continue;
            }
            // The per-row action reuses the notification machinery against the queried row entity.
            NotificationSupport.Plan plan = NotificationSupport.plan(schedule.getNotify(), byName.get(entity), byName, compositionParents);
            if (plan == null) {
                LOGGER.warn("Schedule [{}] notify recipient [{}] is not a resolvable field or relation.field of [{}] - skipping",
                        schedule.getName(), schedule.getNotify()
                                                    .getTo(),
                        entity);
                continue;
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", schedule.getName());
            entry.put("className", IntentNaming.pascalCase(schedule.getName()));
            entry.put("cron", schedule.getCron());
            entry.put("entity", entity);
            entry.put("perspective", IntentEntities.resolvePerspective(entity, compositionParents));
            entry.put("criteriaExpression", ScheduleSupport.criteriaExpression(schedule));
            entry.put("relationLoads", relationLoads(plan));
            entry.put("toExpression", plan.toExpression());
            entry.put("subjectExpression", plan.subjectExpression());
            entry.put("bodyExpression", plan.bodyExpression());
            schedules.add(entry);
        }
        return schedules;
    }

    private static List<Map<String, Object>> relationLoads(NotificationSupport.Plan plan) {
        List<Map<String, Object>> loads = new ArrayList<>();
        for (NotificationSupport.RelationLoad load : plan.loads()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("local", load.local());
            entry.put("targetEntity", load.targetEntity());
            entry.put("targetPerspective", load.targetPerspective());
            entry.put("fkProperty", load.fkProperty());
            loads.add(entry);
        }
        return loads;
    }

    private static List<Map<String, Object>> buildResolvers(IntentModel model, IntentSettings settings) {
        List<Map<String, Object>> resolvers = new ArrayList<>();
        for (Resolver resolver : ProcessResolverSupport.resolvers(model)) {
            if (!settings.shouldGenerate("resolvers", resolver.handler())) {
                LOGGER.info("Settings opt-out: keeping existing handler for resolver [{}] (not generated)", resolver.handler());
                continue;
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("process", resolver.process());
            entry.put("handler", resolver.handler());
            entry.put("fkProperty", resolver.fkProperty());
            entry.put("targetEntity", resolver.targetEntity());
            entry.put("targetPerspective", resolver.targetPerspective());
            entry.put("targetField", resolver.targetField());
            entry.put("targetIdAccessor", resolver.targetIdAccessor());
            entry.put("variable", resolver.variable());
            resolvers.add(entry);
        }
        return resolvers;
    }
}
