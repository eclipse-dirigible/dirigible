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
import org.eclipse.dirigible.components.intent.generator.ProcessFieldLoadSupport.FieldLoad;
import org.eclipse.dirigible.components.intent.generator.ProcessResolverSupport.Resolver;
import org.eclipse.dirigible.components.intent.generator.SetFieldSupport.Setter;
import org.eclipse.dirigible.components.intent.generator.WriterSupport.WriteField;
import org.eclipse.dirigible.components.intent.generator.WriterSupport.Writer;
import org.eclipse.dirigible.components.intent.generator.edm.CrossModelSupport;
import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.InboundIntent;
import org.eclipse.dirigible.components.intent.model.IntegrationIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.NotificationIntent;
import org.eclipse.dirigible.components.intent.model.ProcessIntent;
import org.eclipse.dirigible.components.intent.model.RelationIntent;
import org.eclipse.dirigible.components.intent.model.RollupIntent;
import org.eclipse.dirigible.components.intent.model.ScheduleIntent;
import org.eclipse.dirigible.components.intent.model.UsesIntent;
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
        List<Map<String, Object>> triggers = buildTriggers(model, byName, compositionParents, settings, context);
        List<Map<String, Object>> resolvers = buildResolvers(model, settings);
        List<Map<String, Object>> fieldLoaders = buildFieldLoaders(model, settings);
        List<Map<String, Object>> writers = buildWriters(model, settings);
        List<Map<String, Object>> setters = buildSetters(model, settings);
        List<Map<String, Object>> notifications = buildNotifications(model, byName, compositionParents, settings);
        List<Map<String, Object>> schedules = buildSchedules(model, byName, compositionParents, settings);
        List<Map<String, Object>> integrations = buildIntegrations(model, byName, compositionParents, settings);
        List<Map<String, Object>> inbound = buildInbound(model, byName, compositionParents, settings);
        List<Map<String, Object>> rollups = buildRollups(model, byName, compositionParents, settings);

        if (triggers.isEmpty() && resolvers.isEmpty() && fieldLoaders.isEmpty() && writers.isEmpty() && setters.isEmpty()
                && notifications.isEmpty() && schedules.isEmpty() && integrations.isEmpty() && inbound.isEmpty() && rollups.isEmpty()) {
            // No process glue for this intent - any stale .glue is removed by the post-pass scrub.
            return;
        }

        Map<String, Object> glue = new LinkedHashMap<>();
        glue.put("triggers", triggers);
        glue.put("resolvers", resolvers);
        glue.put("fieldLoaders", fieldLoaders);
        glue.put("writers", writers);
        glue.put("setters", setters);
        glue.put("notifications", notifications);
        glue.put("schedules", schedules);
        glue.put("integrations", integrations);
        glue.put("inbound", inbound);
        glue.put("rollups", rollups);
        context.writeModelFile(IntentNaming.baseName(context) + ".glue", JsonHelper.toJson(glue));
        LOGGER.debug(
                "Wrote glue with [{}] trigger(s), [{}] resolver(s), [{}] writer(s), [{}] setter(s),"
                        + " [{}] notification(s), [{}] schedule(s), [{}] integration(s), [{}] inbound webhook(s) and [{}] rollup(s)",
                triggers.size(), resolvers.size(), writers.size(), setters.size(), notifications.size(), schedules.size(),
                integrations.size(), inbound.size(), rollups.size());
    }

    private static List<Map<String, Object>> buildTriggers(IntentModel model, Map<String, EntityIntent> byName,
            Map<String, String> compositionParents, IntentSettings settings, IntentGenerationContext context) {
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
            // Per to-one relation: enough to build the target controller URL so the task form can resolve
            // each FK to a display name (the form falls back to the raw id when a URL is missing).
            trigger.put("relationLinks", buildRelationLinks(byName.get(entity), model, byName, compositionParents, context));
            triggers.add(trigger);
        }
        return triggers;
    }

    /**
     * One link per to-one relation of the trigger entity: the FK property plus the logical names needed
     * to build the target's REST controller URL (project / model / perspective / entity) and its label
     * field. The events template assembles the URL (it knows the path layout); the task form fetches
     * the related record and shows its label, falling back to the raw FK id. Cross-model relations
     * carry the target project + model alias; same-model ones leave those blank so the template uses
     * the owner's.
     */
    private static List<Map<String, Object>> buildRelationLinks(EntityIntent owner, IntentModel model, Map<String, EntityIntent> byName,
            Map<String, String> compositionParents, IntentGenerationContext context) {
        List<Map<String, Object>> links = new ArrayList<>();
        if (owner == null) {
            return links;
        }
        for (RelationIntent relation : owner.getRelations()) {
            boolean toOne = "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
            if (!toOne || relation.getName() == null || relation.getTo() == null) {
                continue;
            }
            Map<String, Object> link = new LinkedHashMap<>();
            link.put("fkProperty", IntentNaming.pascalCase(relation.getName()));
            link.put("targetEntity", relation.getTo());
            boolean crossModel = relation.getModel() != null && !relation.getModel()
                                                                         .isBlank();
            link.put("crossModel", crossModel);
            if (crossModel) {
                UsesIntent uses = findUses(model, relation.getModel());
                CrossModelSupport.TargetInfo target = uses == null ? null : CrossModelSupport.resolve(context, uses, relation.getTo());
                link.put("targetProject", uses == null ? relation.getModel() : uses.resolveProject());
                link.put("targetModel", relation.getModel());
                link.put("targetPerspective", target != null ? target.perspectiveName() : relation.getTo());
                link.put("labelField", target != null ? target.labelField() : "Name");
            } else {
                EntityIntent target = byName.get(relation.getTo());
                link.put("targetProject", "");
                link.put("targetModel", "");
                // A setting entity's controller lives under the shared "Settings" perspective, not its
                // own name (the template routes SETTING entities there); resolvePerspective only handles
                // composition nesting, so special-case settings or the FK URL 404s.
                link.put("targetPerspective", target != null && target.isSetting() ? "Settings"
                        : IntentEntities.resolvePerspective(relation.getTo(), compositionParents));
                link.put("labelField", nameField(target));
            }
            links.add(link);
        }
        return links;
    }

    /** The to-one target's label property: its {@code name} field (PascalCased), else {@code Name}. */
    private static String nameField(EntityIntent target) {
        if (target != null) {
            for (FieldIntent field : target.getFields()) {
                if (field.getName() != null && "name".equalsIgnoreCase(field.getName())) {
                    return IntentNaming.pascalCase(field.getName());
                }
            }
        }
        return "Name";
    }

    /** The {@code uses:} entry for a model alias, or null if the intent declares none. */
    private static UsesIntent findUses(IntentModel model, String alias) {
        for (UsesIntent uses : model.getUses()) {
            if (alias.equals(uses.getModel())) {
                return uses;
            }
        }
        return null;
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
            String op = rollup.getOp() == null || rollup.getOp()
                                                        .isBlank() ? "count" : rollup.getOp();
            boolean sum = "sum".equals(op);
            if (sum && (rollup.getOf() == null || rollup.getOf()
                                                        .isBlank())) {
                LOGGER.warn("Sum roll-up [{}] has no 'of' field - skipping", rollup.getName());
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
            base.put("op", op);
            base.put("sumField", sum ? IntentNaming.pascalCase(rollup.getOf()) : "");
            // Recompute the value for the affected parent from the store on each child event.
            base.put("criteriaExpression", "Criteria.create().eq(\"" + fkProperty + "\", entity." + fkProperty + ")");
            String className = IntentNaming.pascalCase(rollup.getName());
            rollups.add(rollupEntry(base, className + "RollupOnCreate", ""));
            if (sum) {
                // A line edit changes the sum, so a sum roll-up must also recompute on update.
                rollups.add(rollupEntry(base, className + "RollupOnUpdate", "-updated"));
            }
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

    private static List<Map<String, Object>> buildFieldLoaders(IntentModel model, IntentSettings settings) {
        List<Map<String, Object>> loaders = new ArrayList<>();
        for (FieldLoad load : ProcessFieldLoadSupport.fieldLoads(model)) {
            if (!settings.shouldGenerate("fieldLoaders", load.handler())) {
                LOGGER.info("Settings opt-out: keeping existing handler for field loader [{}] (not generated)", load.handler());
                continue;
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("process", load.process());
            entry.put("handler", load.handler());
            entry.put("ownerEntity", load.ownerEntity());
            entry.put("ownerPerspective", load.ownerPerspective());
            entry.put("ownerKeyProperty", load.ownerKeyProperty());
            entry.put("ownerKeyAccessor", load.ownerKeyAccessor());
            entry.put("fields", new ArrayList<>(load.fields()));
            loaders.add(entry);
        }
        return loaders;
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
            // Owner = the trigger entity; the resolver loads it by its id (the only thing in the id-only
            // process context) to read the FK, then loads the target. See Resolver.java.template.
            entry.put("ownerEntity", resolver.ownerEntity());
            entry.put("ownerPerspective", resolver.ownerPerspective());
            entry.put("ownerKeyProperty", resolver.ownerKeyProperty());
            entry.put("ownerKeyAccessor", resolver.ownerKeyAccessor());
            resolvers.add(entry);
        }
        return resolvers;
    }

    private static List<Map<String, Object>> buildWriters(IntentModel model, IntentSettings settings) {
        List<Map<String, Object>> writers = new ArrayList<>();
        for (Writer writer : WriterSupport.writers(model)) {
            if (!settings.shouldGenerate("writers", writer.className())) {
                LOGGER.info("Settings opt-out: keeping existing handler for writer [{}] (not generated)", writer.className());
                continue;
            }
            List<Map<String, Object>> fields = new ArrayList<>();
            for (WriteField field : writer.fields()) {
                Map<String, Object> f = new LinkedHashMap<>();
                f.put("property", field.property());
                fields.add(f);
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("process", writer.process());
            entry.put("userTask", writer.userTask());
            entry.put("className", writer.className());
            entry.put("entity", writer.entity());
            entry.put("perspective", writer.perspective());
            entry.put("keyProperty", writer.keyProperty());
            entry.put("keyAccessor", writer.keyAccessor());
            entry.put("fields", fields);
            writers.add(entry);
        }
        return writers;
    }

    private static List<Map<String, Object>> buildSetters(IntentModel model, IntentSettings settings) {
        List<Map<String, Object>> setters = new ArrayList<>();
        for (Setter setter : SetFieldSupport.setters(model)) {
            if (!settings.shouldGenerate("setters", setter.className())) {
                LOGGER.info("Settings opt-out: keeping existing handler for setter [{}] (not generated)", setter.className());
                continue;
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("process", setter.process());
            entry.put("className", setter.className());
            entry.put("entity", setter.entity());
            entry.put("perspective", setter.perspective());
            entry.put("keyProperty", setter.keyProperty());
            entry.put("keyAccessor", setter.keyAccessor());
            entry.put("field", setter.field());
            entry.put("value", setter.value());
            setters.add(entry);
        }
        return setters;
    }
}
