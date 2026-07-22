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
import org.eclipse.dirigible.components.intent.model.GenerateChildIntent;
import org.eclipse.dirigible.components.intent.model.GeneratesIntent;
import org.eclipse.dirigible.components.intent.model.GeneratesItemsIntent;
import org.eclipse.dirigible.components.intent.model.InboundIntent;
import org.eclipse.dirigible.components.intent.model.IntegrationIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.NotificationIntent;
import org.eclipse.dirigible.components.intent.model.ProcessIntent;
import org.eclipse.dirigible.components.intent.model.RelationIntent;
import org.eclipse.dirigible.components.intent.model.ExpansionIntent;
import org.eclipse.dirigible.components.intent.model.RollupIntent;
import org.eclipse.dirigible.components.intent.model.ScheduleIntent;
import org.eclipse.dirigible.components.intent.model.SettlementIntent;
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
        List<Map<String, Object>> timerLoaders = buildTimerLoaders(model, settings);
        List<Map<String, Object>> waits = buildWaits(model, settings);
        List<Map<String, Object>> aborts = buildAborts(model, settings);
        List<Map<String, Object>> writers = buildWriters(model, settings);
        List<Map<String, Object>> setters = buildSetters(model, settings);
        List<Map<String, Object>> notifications = buildNotifications(model, byName, compositionParents, settings);
        List<Map<String, Object>> schedules = buildSchedules(model, byName, compositionParents, settings, context);
        List<Map<String, Object>> integrations = buildIntegrations(model, byName, compositionParents, settings);
        List<Map<String, Object>> inbound = buildInbound(model, byName, compositionParents, settings);
        List<Map<String, Object>> rollups = buildRollups(model, byName, compositionParents, settings);
        List<Map<String, Object>> expansions = buildExpansions(model, byName, compositionParents, settings);
        List<Map<String, Object>> settlements = buildSettlements(model, byName, compositionParents, settings, context);
        List<Map<String, Object>> generates = buildGenerates(model, byName, compositionParents, settings, context);
        List<Map<String, Object>> transitions = buildTransitions(model, byName, compositionParents, settings);
        List<Map<String, Object>> postings = buildPostings(model, byName, compositionParents, settings, context);
        List<Map<String, Object>> printFeeders = PrintFeederSupport.buildPrintFeeders(model, byName, compositionParents, context);

        if (triggers.isEmpty() && resolvers.isEmpty() && fieldLoaders.isEmpty() && timerLoaders.isEmpty() && waits.isEmpty()
                && aborts.isEmpty() && writers.isEmpty() && setters.isEmpty() && notifications.isEmpty() && schedules.isEmpty()
                && integrations.isEmpty() && inbound.isEmpty() && rollups.isEmpty() && expansions.isEmpty() && settlements.isEmpty()
                && generates.isEmpty() && transitions.isEmpty() && printFeeders.isEmpty() && postings.isEmpty()) {
            // No process glue for this intent - any stale .glue is removed by the post-pass scrub.
            return;
        }

        Map<String, Object> glue = new LinkedHashMap<>();
        glue.put("triggers", triggers);
        glue.put("resolvers", resolvers);
        glue.put("fieldLoaders", fieldLoaders);
        glue.put("timerLoaders", timerLoaders);
        glue.put("waits", waits);
        glue.put("aborts", aborts);
        glue.put("writers", writers);
        glue.put("setters", setters);
        glue.put("notifications", notifications);
        glue.put("schedules", schedules);
        glue.put("integrations", integrations);
        glue.put("inbound", inbound);
        glue.put("rollups", rollups);
        glue.put("expansions", expansions);
        glue.put("settlements", settlements);
        glue.put("generates", generates);
        glue.put("transitions", transitions);
        glue.put("postings", postings);
        glue.put("printFeeders", printFeeders);
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
            putPersonalAssignee(trigger, byName.get(entity), model, byName, compositionParents, context);
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
    /**
     * The template-ready child blocks of a scheduled generation, up to two levels. A child target
     * resolves in the SAME model as the generation target (the {@code uses} alias, or locally); the
     * {@code forEach} collection entity is always LOCAL. The row variable is {@code r<depth>}; field
     * assignments are pre-rendered against it, defaults against literals.
     */
    private static List<Map<String, Object>> buildGenerateChildren(List<GenerateChildIntent> children, UsesIntent uses,
            Map<String, EntityIntent> byName, Map<String, String> compositionParents, IntentGenerationContext context, int depth) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (GenerateChildIntent child : children) {
            Map<String, Object> entry = new LinkedHashMap<>();
            boolean crossModel = uses != null;
            CrossModelSupport.TargetInfo target = crossModel ? CrossModelSupport.resolve(context, uses, child.getTo()) : null;
            entry.put("toEntity", child.getTo());
            entry.put("toCrossModel", crossModel);
            entry.put("toModel", crossModel ? uses.getModel() : "");
            entry.put("toPerspective",
                    target != null ? target.perspectiveName() : IntentEntities.resolvePerspective(child.getTo(), compositionParents));
            entry.put("toPk", target != null ? target.keyField() : IntentEntities.keyFieldName(byName.get(child.getTo())));
            entry.put("parentFkProperty", IntentNaming.pascalCase(child.getParent()));
            Object days = child.getForEach()
                               .get("days");
            String rowVar = "r" + depth;
            if (days != null) {
                entry.put("kind", "days");
                entry.put("dayField", IntentNaming.pascalCase(child.getDayField()));
                entry.put("fieldAssignments", childAssignments(java.util.Map.of(), child.getDefaults(), rowVar));
            } else {
                entry.put("kind", "entity");
                String collection = String.valueOf(child.getForEach()
                                                        .get("entity"));
                entry.put("forEachEntity", collection);
                entry.put("forEachPerspective", IntentEntities.resolvePerspective(collection, compositionParents));
                @SuppressWarnings("unchecked")
                Map<String, Object> match = (Map<String, Object>) child.getForEach()
                                                                       .get("match");
                Map.Entry<String, Object> condition = match.entrySet()
                                                           .iterator()
                                                           .next();
                entry.put("matchProperty", IntentNaming.pascalCase(condition.getKey()));
                entry.put("matchSourceExpression", "entity." + IntentNaming.pascalCase(String.valueOf(condition.getValue())));
                entry.put("fieldAssignments", childAssignments(toStringMap(child.getMap()), child.getDefaults(), rowVar));
            }
            entry.put("rowVar", rowVar);
            if (child.getChildren() != null && !child.getChildren()
                                                     .isEmpty()) {
                entry.put("children", buildGenerateChildren(child.getChildren(), uses, byName, compositionParents, context, depth + 1));
            }
            result.add(entry);
        }
        return result;
    }

    private static Map<String, String> toStringMap(Map<String, String> map) {
        return map == null ? java.util.Map.of() : map;
    }

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

    /**
     * When the trigger entity has a {@code personal: true} owner relation, the listener also seeds the
     * {@code __personalUser} process variable - the identity value (login username) of the record's
     * owner - so user tasks with {@code assignee: personal} land in exactly that person's Inbox. Emits
     * the FK property plus the identity target coordinates (same shapes as relationLinks; the template
     * engine assembles the import).
     */
    private static void putPersonalAssignee(Map<String, Object> trigger, EntityIntent owner, IntentModel model,
            Map<String, EntityIntent> byName, Map<String, String> compositionParents, IntentGenerationContext context) {
        if (owner == null || owner.getRelations() == null) {
            return;
        }
        for (RelationIntent relation : owner.getRelations()) {
            if (!relation.isPersonal()) {
                continue;
            }
            trigger.put("personalFkProperty", IntentNaming.pascalCase(relation.getName()));
            trigger.put("personalTargetEntity", relation.getTo());
            boolean crossModel = relation.getModel() != null && !relation.getModel()
                                                                         .isBlank();
            trigger.put("personalCrossModel", crossModel);
            if (crossModel) {
                UsesIntent uses = findUses(model, relation.getModel());
                CrossModelSupport.TargetInfo target = uses == null ? null : CrossModelSupport.resolve(context, uses, relation.getTo());
                trigger.put("personalTargetModel", relation.getModel());
                trigger.put("personalIdentityProperty",
                        target != null && target.identityProperty() != null ? target.identityProperty() : "Email");
                trigger.put("personalTargetPerspective", target != null ? target.perspectiveName() : relation.getTo());
            } else {
                EntityIntent target = byName.get(relation.getTo());
                trigger.put("personalTargetModel", "");
                trigger.put("personalIdentityProperty",
                        target != null && target.getIdentity() != null ? IntentNaming.pascalCase(target.getIdentity()) : "Email");
                trigger.put("personalTargetPerspective", target != null && target.isSetting() ? "Settings"
                        : IntentEntities.resolvePerspective(relation.getTo(), compositionParents));
            }
            return;
        }
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
            boolean latest = "latest".equals(op);
            if (sum && (rollup.getOf() == null || rollup.getOf()
                                                        .isBlank())) {
                LOGGER.warn("Sum roll-up [{}] has no 'of' field - skipping", rollup.getName());
                continue;
            }
            if (latest && (rollup.getOf() == null || rollup.getOf()
                                                           .isBlank()
                    || rollup.getBy() == null || rollup.getBy()
                                                       .isBlank())) {
                LOGGER.warn("Latest roll-up [{}] needs both 'of' and 'by' - skipping", rollup.getName());
                continue;
            }
            String fkProperty = IntentNaming.pascalCase(rollup.getVia());
            Map<String, Object> base = new LinkedHashMap<>();
            base.put("childEntity", rollup.getEntity());
            // A setting entity's generated code lives under the shared "Settings" perspective, not its
            // own name - so a roll-up whose child/parent is `kind: setting` must resolve there, like
            // the relation-link / personal-assignee builders do. Without this the generated handler
            // imports gen.<mod>.data.<entityname> (which does not exist) instead of ...data.settings
            // and the whole client-Java batch fails to compile (a setting-entity roll-up, e.g.
            // Currency <- CurrencyRate, is the case that exposed it).
            base.put("childPerspective",
                    child.isSetting() ? "Settings" : IntentEntities.resolvePerspective(rollup.getEntity(), compositionParents));
            base.put("parentEntity", via.getTo());
            base.put("parentPerspective",
                    parent.isSetting() ? "Settings" : IntentEntities.resolvePerspective(via.getTo(), compositionParents));
            base.put("fkProperty", fkProperty);
            base.put("countField", IntentNaming.pascalCase(rollup.getField()));
            base.put("op", op);
            base.put("sumField", sum ? IntentNaming.pascalCase(rollup.getOf()) : "");
            // latest: copy the `of` value of the child row with the greatest `by` onto the parent field.
            base.put("ofField", latest ? IntentNaming.pascalCase(rollup.getOf()) : "");
            base.put("byField", latest ? IntentNaming.pascalCase(rollup.getBy()) : "");
            // Optional (sum) capacity/balance/status: keep a `balance` field = capacity - sum, and set a
            // `status` relation to whenFull/whenPartial at the thresholds. Empty string / -1 = not set.
            boolean withCapacity = sum && rollup.getCapacity() != null && !rollup.getCapacity()
                                                                                 .isBlank();
            base.put("capacityField", withCapacity ? IntentNaming.pascalCase(rollup.getCapacity()) : "");
            base.put("balanceField", withCapacity && rollup.getBalance() != null && !rollup.getBalance()
                                                                                           .isBlank()
                                                                                                   ? IntentNaming.pascalCase(
                                                                                                           rollup.getBalance())
                                                                                                   : "");
            boolean withStatus = withCapacity && rollup.getStatus() != null && !rollup.getStatus()
                                                                                      .isBlank();
            base.put("statusField", withStatus ? IntentNaming.pascalCase(rollup.getStatus()) : "");
            base.put("statusWhenFull", withStatus && rollup.getStatusWhenFull() != null ? rollup.getStatusWhenFull()
                                                                                                .toString()
                    : "");
            base.put("statusWhenPartial", withStatus && rollup.getStatusWhenPartial() != null ? rollup.getStatusWhenPartial()
                                                                                                      .toString()
                    : "");
            // Recompute the value for the affected parent from the store on each child event.
            base.put("criteriaExpression", "Criteria.create().eq(\"" + fkProperty + "\", entity." + fkProperty + ")");
            // Handler name derives from the coalescing key (childEntity + parent-fk), NOT the roll-up name:
            // generateUtils groups every roll-up sharing (childEntity, fkProperty, event) into one handler, so
            // the name must be shared across the group. Two roll-ups on the same child+fk+event collapse into
            // this one class.
            String className = rollup.getEntity() + fkProperty;
            rollups.add(rollupEntry(base, className + "RollupOnCreate", ""));
            if (sum || latest) {
                // A line edit changes the sum (or which row is latest / its value), so sum AND latest
                // roll-ups must also recompute on update.
                rollups.add(rollupEntry(base, className + "RollupOnUpdate", "-updated"));
            }
            rollups.add(rollupEntry(base, className + "RollupOnDelete", "-deleted"));
        }
        return rollups;
    }

    /**
     * One glue entry per {@link SettlementIntent}: resolves the junction's two FK properties, the
     * invoice open-amount fields, and the cross-model payment's project/perspective/topic (via
     * {@link CrossModelSupport}) so the two settlement templates (onPayment listener + onInvoice
     * delegate) can be rendered. Java-package sanitization happens in the {@code settlements} case of
     * {@code generateUtils.js} (same as triggers), keeping this generator at logical names.
     */
    private static List<Map<String, Object>> buildSettlements(IntentModel model, Map<String, EntityIntent> byName,
            Map<String, String> compositionParents, IntentSettings settings, IntentGenerationContext context) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (SettlementIntent s : model.getSettlements()) {
            if (s.getName() == null || s.getName()
                                        .isBlank()) {
                continue;
            }
            EntityIntent junction = byName.get(s.getJunction());
            EntityIntent invoice = byName.get(s.getInvoice());
            if (junction == null || invoice == null) {
                continue; // parser already reported the bad reference
            }
            if (!settings.shouldGenerate("settlements", s.getName())) {
                LOGGER.info("Settings opt-out: keeping existing settlement [{}] (not generated)", s.getName());
                continue;
            }
            RelationIntent fkInvoice = relationTo(junction, s.getInvoice());
            RelationIntent fkPayment = relationTo(junction, s.getPayment());
            if (fkInvoice == null || fkPayment == null) {
                continue; // parser already reported the missing junction relation
            }
            boolean crossModel = fkPayment.getModel() != null && !fkPayment.getModel()
                                                                           .isBlank();
            UsesIntent uses = crossModel ? findUses(model, fkPayment.getModel()) : null;
            CrossModelSupport.TargetInfo payTarget = uses == null ? null : CrossModelSupport.resolve(context, uses, s.getPayment());
            String paymentProject = crossModel ? (uses == null ? fkPayment.getModel() : uses.resolveProject()) : context.getProjectName();
            String paymentPerspective = payTarget != null ? payTarget.perspectiveName() : s.getPayment();

            Map<String, Object> e = new LinkedHashMap<>();
            e.put("name", IntentNaming.pascalCase(s.getName()));
            e.put("match", pascalList(s.getMatch()));
            // invoice (this project)
            e.put("invoiceEntity", s.getInvoice());
            e.put("invoicePerspective", IntentEntities.resolvePerspective(s.getInvoice(), compositionParents));
            e.put("invoicePk", IntentEntities.keyFieldName(invoice));
            e.put("invoiceTotal", IntentNaming.pascalCase(s.getTotal()));
            e.put("invoicePaid", IntentNaming.pascalCase(s.getPaid()));
            e.put("order", IntentNaming.pascalCase(s.getOrder()));
            e.put("invoiceStatus", s.getStatus() == null ? "" : IntentNaming.pascalCase(s.getStatus()));
            e.put("payableCondition", payableCondition(s.getPayableStatuses()));
            // junction (this project)
            e.put("junctionEntity", s.getJunction());
            e.put("junctionPerspective", IntentEntities.resolvePerspective(s.getJunction(), compositionParents));
            e.put("junctionFkInvoice", IntentNaming.pascalCase(fkInvoice.getName()));
            e.put("junctionFkPayment", IntentNaming.pascalCase(fkPayment.getName()));
            e.put("junctionAmount", IntentNaming.pascalCase(s.getAmount()));
            // payment (possibly cross-model)
            e.put("crossModel", crossModel);
            e.put("paymentEntity", s.getPayment());
            e.put("paymentProject", paymentProject);
            e.put("paymentModel", crossModel ? fkPayment.getModel() : "");
            e.put("paymentPerspective", paymentPerspective);
            e.put("paymentPk", payTarget != null ? payTarget.keyField() : "Id");
            e.put("paymentPot", IntentNaming.pascalCase(s.getPot()));
            e.put("paymentTopic", paymentProject + "-" + paymentPerspective + "-" + s.getPayment());
            out.add(e);
        }
        return out;
    }

    /**
     * One glue entry per {@link GeneratesIntent}: resolves the source entity's perspective/genFolder
     * (in this project) and the target's - possibly cross-model, via {@link CrossModelSupport} - plus
     * the pre-rendered field assignment expressions (source-copy, {@code now}, or literal) for the
     * header and (optionally) the composition items. The {@code Generate.java.template} then renders a
     * REST {@code @Controller} that clones a source record into a fresh target record and saves it
     * through the <b>target's</b> generated repository so its create-time logic (numbering, status
     * init) fires. The matching client button is emitted separately by the
     * {@code GeneratesIntentGenerator}.
     */
    private static List<Map<String, Object>> buildGenerates(IntentModel model, Map<String, EntityIntent> byName,
            Map<String, String> compositionParents, IntentSettings settings, IntentGenerationContext context) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (GeneratesIntent g : model.getGenerates()) {
            if (g.getName() == null || g.getName()
                                        .isBlank()) {
                continue;
            }
            EntityIntent source = byName.get(g.getFrom());
            if (source == null) {
                continue; // parser already reported the bad reference
            }
            if (!settings.shouldGenerate("generates", g.getName())) {
                LOGGER.info("Settings opt-out: keeping existing controller for generates [{}] (not generated)", g.getName());
                continue;
            }
            boolean crossModel = g.getUses() != null && !g.getUses()
                                                          .isBlank();
            UsesIntent uses = crossModel ? findUses(model, g.getUses()) : null;
            CrossModelSupport.TargetInfo target = uses == null ? null : CrossModelSupport.resolve(context, uses, g.getTo());
            String toPerspective =
                    target != null ? target.perspectiveName() : IntentEntities.resolvePerspective(g.getTo(), compositionParents);
            String toPk = target != null ? target.keyField() : IntentEntities.keyFieldName(byName.get(g.getTo()));

            Map<String, Object> e = new LinkedHashMap<>();
            e.put("name", g.getName());
            e.put("className", IntentNaming.pascalIdentifier(g.getName()));
            e.put("crossModel", crossModel);
            e.put("fromEntity", g.getFrom());
            e.put("fromPerspective", IntentEntities.resolvePerspective(g.getFrom(), compositionParents));
            e.put("toEntity", g.getTo());
            e.put("toModel", crossModel ? g.getUses() : "");
            e.put("toPerspective", toPerspective);
            e.put("toPk", toPk);
            e.put("fieldAssignments", assignments(g.getMap(), g.getDefaults(), "source"));
            // Completion hook: the SOURCE's EntityStatus FK is set to this seed id after the target
            // is created (empty = no hook). Pre-resolved to the PascalCase FK property.
            String sourceStatusProperty = "";
            if (g.getSourceStatus() != null) {
                for (org.eclipse.dirigible.components.intent.model.RelationIntent relation : source.getRelations()) {
                    if (relation.isEntityStatus()) {
                        sourceStatusProperty = IntentNaming.pascalCase(relation.getName());
                    }
                }
            }
            e.put("sourceStatusProperty", sourceStatusProperty);
            e.put("sourceStatusValue",
                    g.getSourceStatus() == null || sourceStatusProperty.isEmpty() ? "" : String.valueOf(g.getSourceStatus()));

            GeneratesItemsIntent items = g.getItems();
            boolean hasItems = items != null && items.getFrom() != null && !items.getFrom()
                                                                                 .isBlank()
                    && items.getTo() != null && !items.getTo()
                                                      .isBlank();
            e.put("hasItems", hasItems);
            if (hasItems) {
                e.put("fromItemEntity", items.getFrom());
                e.put("toItemEntity", items.getTo());
                // The SOURCE item's own perspective: a composition child resolves to its master's
                // perspective (== fromPerspective, so this is a no-op for the common document-item
                // case), but a source item that is a SEPARATE primary entity referencing the source
                // by FK (e.g. an aggregate document whose per-line detail is its own entity) lives in
                // its OWN package - the template must qualify srcItem with this, not the source
                // document's perspective. (The TARGET item stays on toPerspective: a create-from
                // always writes into the target document's own composition-item table.)
                e.put("fromItemPerspective", IntentEntities.resolvePerspective(items.getFrom(), compositionParents));
                // A document child's FK back to its master is, by convention, the master entity's name.
                e.put("srcFkProperty", IntentNaming.pascalCase(g.getFrom()));
                e.put("toFkProperty", IntentNaming.pascalCase(g.getTo()));
                // childAssignments (not assignments) so a numeric item default renders as BigDecimal -
                // line-item columns (quantity/price/amount) are decimal, and a bare int literal does
                // not convert to the generated BigDecimal field.
                e.put("itemFieldAssignments", childAssignments(items.getMap(), items.getDefaults(), "srcItem"));
            } else {
                e.put("fromItemEntity", "");
                e.put("toItemEntity", "");
                e.put("fromItemPerspective", "");
                e.put("srcFkProperty", "");
                e.put("toFkProperty", "");
                e.put("itemFieldAssignments", new ArrayList<>());
            }
            out.add(e);
        }
        return out;
    }

    /**
     * Transitions: one entry per {@code transitions} declaration - the guarded on-demand status flip.
     * EVERYTHING is pre-rendered here so the Velocity template contains no expression logic: the
     * allowed-statuses check is a Java boolean expression over an {@code int currentStatus} local, and
     * the optional {@code when} guard is a full SDK {@code Calc} comparison over the loaded
     * {@code source} entity (Calc semantics: a null field reads as 0 - identical to calculated fields).
     */
    private static List<Map<String, Object>> buildTransitions(IntentModel model, Map<String, EntityIntent> byName,
            Map<String, String> compositionParents, IntentSettings settings) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (org.eclipse.dirigible.components.intent.model.TransitionIntent t : model.getTransitions()) {
            if (t.getName() == null || t.getName()
                                        .isBlank()
                    || t.getForEntity() == null || t.getSetStatus() == null || t.getFrom() == null || t.getFrom()
                                                                                                       .isEmpty()) {
                continue; // parser already reported the malformed declaration
            }
            EntityIntent entity = byName.get(t.getForEntity());
            if (entity == null) {
                continue; // parser already reported the bad reference
            }
            if (!settings.shouldGenerate("transitions", t.getName())) {
                LOGGER.info("Settings opt-out: keeping existing controller for transition [{}] (not generated)", t.getName());
                continue;
            }
            String statusProperty = "";
            for (org.eclipse.dirigible.components.intent.model.RelationIntent relation : entity.getRelations()) {
                if (relation.isEntityStatus()) {
                    statusProperty = IntentNaming.pascalCase(relation.getName());
                }
            }
            if (statusProperty.isEmpty()) {
                continue; // parser already reported the missing EntityStatus relation
            }
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("name", t.getName());
            e.put("className", IntentNaming.pascalIdentifier(t.getName()));
            e.put("entity", t.getForEntity());
            e.put("perspective", IntentEntities.resolvePerspective(t.getForEntity(), compositionParents));
            e.put("statusProperty", statusProperty);
            e.put("setStatus", String.valueOf(t.getSetStatus()));
            List<String> terms = new ArrayList<>();
            List<String> fromIds = new ArrayList<>();
            for (Integer from : t.getFrom()) {
                terms.add("currentStatus == " + from);
                fromIds.add(String.valueOf(from));
            }
            e.put("allowedExpr", String.join(" || ", terms));
            e.put("fromStatuses", String.join(", ", fromIds));
            String guardExpr = "";
            String guardText = "";
            if (t.getWhen() != null && !t.getWhen()
                                         .isBlank()) {
                java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\s*(\\w+)\\s*(==|!=)\\s*(-?\\d+(?:\\.\\d+)?)\\s*")
                                                                         .matcher(t.getWhen());
                if (matcher.matches()) {
                    // Calc reads the field with the calculated-field semantics (null -> 0); compareTo
                    // keeps the comparison exact for decimals.
                    guardExpr = "org.eclipse.dirigible.sdk.utils.Calc.eval(\"" + IntentNaming.pascalCase(matcher.group(1))
                            + "\", source, 6).compareTo(new java.math.BigDecimal(\"" + matcher.group(3) + "\")) "
                            + ("==".equals(matcher.group(2)) ? "==" : "!=") + " 0";
                    guardText = t.getWhen()
                                 .trim();
                }
            }
            e.put("guardExpr", guardExpr);
            e.put("guardText", guardText);
            out.add(e);
        }
        return out;
    }

    /**
     * One abort listener per process declaring {@code abortOn}: a {@code MessageHandler} on the trigger
     * entity's {@code -transitioned} topic that matches the abort statuses and correlates the
     * {@code <Process>Abort} message on the record's stamped {@code ProcessId} (fail-soft: no parked
     * instance is a no-op). The interrupting event subprocess the message fires is emitted by the BPMN
     * generator.
     */
    private static List<Map<String, Object>> buildAborts(IntentModel model, IntentSettings settings) {
        List<Map<String, Object>> aborts = new ArrayList<>();
        for (ProcessAbortSupport.Abort abort : ProcessAbortSupport.aborts(model)) {
            if (!settings.shouldGenerate("aborts", abort.process())) {
                LOGGER.info("Settings opt-out: keeping existing handler for abort [{}] (not generated)", abort.process());
                continue;
            }
            List<String> terms = new ArrayList<>();
            for (Integer status : abort.statuses()) {
                terms.add("entity." + abort.statusProperty() + " != null && entity." + abort.statusProperty() + " == " + status);
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("process", abort.process());
            entry.put("entity", abort.entity());
            entry.put("perspective", abort.perspective());
            entry.put("messageName", abort.messageName());
            entry.put("statusMatchExpression", String.join(" || ", terms));
            aborts.add(entry);
        }
        return aborts;
    }

    /** Test hook: build the {@code aborts} glue collection without a repository. */
    static List<Map<String, Object>> buildAbortsForTest(IntentModel model) {
        return buildAborts(model, IntentSettings.parse("{}"));
    }

    /** Test hook: build the {@code rollups} glue collection without a repository. */
    static List<Map<String, Object>> buildRollupsForTest(IntentModel model) {
        return buildRollups(model, IntentEntities.byName(model), IntentEntities.compositionParents(model), IntentSettings.parse("{}"));
    }

    /** Test hook: build the {@code waits} glue collection without a repository. */
    static List<Map<String, Object>> buildWaitsForTest(IntentModel model) {
        return buildWaits(model, IntentSettings.parse("{}"));
    }

    /** Test hook: build the {@code timerLoaders} glue collection without a repository. */
    static List<Map<String, Object>> buildTimerLoadersForTest(IntentModel model) {
        return buildTimerLoaders(model, IntentSettings.parse("{}"));
    }

    /** Test hook: build the {@code transitions} glue collection without a repository. */
    static List<Map<String, Object>> buildTransitionsForTest(IntentModel model) {
        return buildTransitions(model, IntentEntities.byName(model), IntentEntities.compositionParents(model), IntentSettings.parse("{}"));
    }

    /**
     * Test hook: build the {@code generates} glue collection without a repository. With a null context
     * a cross-model target falls back to {@link CrossModelSupport}'s naming-convention defaults
     * (perspective = entity name, key = {@code Id}), which is deterministic and enough to assert the
     * mapping shape.
     */
    static List<Map<String, Object>> buildGeneratesForTest(IntentModel model) {
        return buildGenerates(model, IntentEntities.byName(model), IntentEntities.compositionParents(model), IntentSettings.parse("{}"),
                null);
    }

    /**
     * Postings: one entry per {@code postings} declaration, with EVERYTHING pre-rendered for the
     * shape-only template - the source topic + re-load coordinates, the status guard, the at-most-once
     * back-reference, the rule lookup, and per-row Java assignment expressions (a
     * {@code rule(<column>)} reference reads the resolved rule row; anything else runs through the SDK
     * {@code Calc} evaluator against the re-loaded source; string headers support
     * {@code {sourceProperty}} interpolation).
     */
    private static List<Map<String, Object>> buildPostings(IntentModel model, Map<String, EntityIntent> byName,
            Map<String, String> compositionParents, IntentSettings settings, IntentGenerationContext context) {
        List<Map<String, Object>> out = new ArrayList<>();
        // A reversal posting's storno link doubles as the discriminator between the reversed
        // sibling's own documents (link empty) and reversals (link set) - the SIBLING's handler
        // must filter its idempotency lookup by it too, so map: base posting name -> storno.
        Map<String, String> stornoOfReversed = new LinkedHashMap<>();
        for (org.eclipse.dirigible.components.intent.model.PostingIntent posting : model.getPostings()) {
            if (posting.getReverses() != null && !posting.getReverses()
                                                         .isBlank()
                    && posting.getStorno() != null) {
                stornoOfReversed.put(posting.getReverses(), IntentNaming.pascalCase(posting.getStorno()));
            }
        }
        for (org.eclipse.dirigible.components.intent.model.PostingIntent posting : model.getPostings()) {
            boolean isReverse = posting.getReverses() != null && !posting.getReverses()
                                                                         .isBlank();
            // Reversal mode: creates/backReference/rule/map/items come from the reversed sibling;
            // the reversal contributes its own event plus the storno link, and every item amount
            // expression is negated (same sides - red storno).
            org.eclipse.dirigible.components.intent.model.PostingIntent effective = posting;
            if (isReverse) {
                for (org.eclipse.dirigible.components.intent.model.PostingIntent candidate : model.getPostings()) {
                    if (candidate != posting && posting.getReverses()
                                                       .equals(candidate.getName())) {
                        effective = candidate;
                    }
                }
                if (effective == posting || posting.getStorno() == null) {
                    continue; // parser already reported it
                }
            }
            if (posting.getName() == null || posting.getName()
                                                    .isBlank()
                    || posting.getEvent() == null || effective.getCreates() == null) {
                continue; // parser already reported it
            }
            if (!settings.shouldGenerate("postings", posting.getName())) {
                LOGGER.info("Settings opt-out: keeping existing handler for posting [{}] (not generated)", posting.getName());
                continue;
            }
            EntityIntent creates = byName.get(effective.getCreates());
            EntityIntent itemsEntity = creates == null ? null : compositionChild(creates, byName);
            if (creates == null || itemsEntity == null) {
                continue; // parser already reported it
            }
            String sourceEntity = String.valueOf(posting.getEvent()
                                                        .get("onTransition"));
            Object alias = posting.getEvent()
                                  .get("model");
            String sourceProject;
            String sourcePerspective;
            String sourceKeyField = "Id";
            String sourceGenFolder;
            if (alias != null) {
                UsesIntent uses = findUses(model, String.valueOf(alias));
                if (uses == null) {
                    continue; // parser already reported it
                }
                sourceProject = uses.getProject() == null || uses.getProject()
                                                                 .isBlank() ? uses.getModel() : uses.getProject();
                sourceGenFolder = uses.getModel();
                CrossModelSupport.TargetInfo info = CrossModelSupport.resolve(context, uses, sourceEntity);
                sourcePerspective = info.perspectiveName();
                sourceKeyField = info.keyField();
            } else {
                sourceProject = ""; // resolved to the own project name at template time
                sourceGenFolder = "";
                sourcePerspective = IntentEntities.resolvePerspective(sourceEntity, compositionParents);
                EntityIntent local = byName.get(sourceEntity);
                if (local != null) {
                    sourceKeyField = IntentEntities.keyFieldName(local);
                }
            }
            // The status guard: "<Property> == <seed id>", evaluated against the RE-LOADED source.
            java.util.regex.Matcher when = java.util.regex.Pattern.compile("\\s*(\\w+)\\s*==\\s*(\\d+)\\s*")
                                                                  .matcher(String.valueOf(posting.getEvent()
                                                                                                 .get("when")));
            if (!when.matches()) {
                continue; // parser already reported it
            }
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("name", posting.getName());
            e.put("className", IntentNaming.pascalIdentifier(posting.getName()));
            e.put("crossModel", alias != null);
            e.put("sourceProject", sourceProject);
            e.put("sourceGenFolder", sourceGenFolder);
            e.put("sourcePerspective", sourcePerspective);
            e.put("sourceEntity", sourceEntity);
            e.put("sourceKeyField", sourceKeyField);
            e.put("guardProperty", IntentNaming.pascalCase(when.group(1)));
            e.put("guardValue", when.group(2));
            e.put("targetEntity", creates.getName());
            e.put("targetPerspective", IntentEntities.resolvePerspective(creates.getName(), compositionParents));
            e.put("targetPk", IntentEntities.keyFieldName(creates));
            e.put("itemsEntity", itemsEntity.getName());
            e.put("itemsPerspective", IntentEntities.resolvePerspective(itemsEntity.getName(), compositionParents));
            e.put("itemsFk", IntentNaming.pascalCase(creates.getName()));
            e.put("backRefProperty", IntentNaming.pascalCase(effective.getBackReference()));
            // Reversal coordinates: the reversal handler locates the original through the empty
            // storno link and stamps it on its own creation; the reversed sibling's handler filters
            // reversals OUT of its idempotency lookup through the same property.
            e.put("stornoProperty", isReverse ? IntentNaming.pascalCase(posting.getStorno()) : "");
            e.put("stornoFilterProperty", isReverse ? "" : stornoOfReversed.getOrDefault(posting.getName(), ""));
            // Rule lookup: a single match selector, columns referenced from the items.
            boolean hasRule = effective.getRule() != null && effective.getRule()
                                                                      .get("entity") != null;
            e.put("hasRule", hasRule);
            java.util.Set<String> usedRuleColumns = new java.util.LinkedHashSet<>();
            if (hasRule) {
                String ruleEntityName = String.valueOf(effective.getRule()
                                                                .get("entity"));
                e.put("ruleEntity", ruleEntityName);
                // A setting rule entity (the normal case) lives under the global Settings perspective.
                EntityIntent ruleEntityIntent = byName.get(ruleEntityName);
                e.put("rulePerspective", ruleEntityIntent != null && ruleEntityIntent.isSetting() ? "Settings"
                        : IntentEntities.resolvePerspective(ruleEntityName, compositionParents));
                Map<?, ?> match = (Map<?, ?>) effective.getRule()
                                                       .get("match");
                Map.Entry<?, ?> selector = match.entrySet()
                                                .iterator()
                                                .next();
                e.put("ruleMatchProperty", IntentNaming.pascalCase(String.valueOf(selector.getKey())));
                e.put("ruleMatchValueJava", javaLiteral(selector.getValue()));
            }
            // Header assignments: copy / literal / {placeholder} template - pre-rendered Java.
            List<Map<String, Object>> headerAssignments = new ArrayList<>();
            if (effective.getMap() != null) {
                for (Map.Entry<String, String> entry : effective.getMap()
                                                                .entrySet()) {
                    headerAssignments.add(postingAssignment(entry.getKey(), entry.getValue()));
                }
            }
            e.put("headerAssignments", headerAssignments);
            // Item rows: rule(...) refs read the rule row; expressions run through Calc on the source.
            List<Map<String, Object>> itemRows = new ArrayList<>();
            for (Map<String, String> row : effective.getItems() == null ? List.<Map<String, String>>of() : effective.getItems()) {
                Map<String, Object> rendered = new LinkedHashMap<>();
                List<Map<String, Object>> assigns = new ArrayList<>();
                String rowGuard = "";
                for (Map.Entry<String, String> cell : row.entrySet()) {
                    String value = cell.getValue() == null ? ""
                            : cell.getValue()
                                  .trim();
                    if ("when".equals(cell.getKey())) {
                        java.util.regex.Matcher guard = java.util.regex.Pattern.compile("\\s*(\\w+)\\s*([!=]=)\\s*(\\d+(?:\\.\\d+)?)\\s*")
                                                                               .matcher(value);
                        if (guard.matches()) {
                            // Calc reads the (possibly null) source field as BigDecimal - null-safe.
                            rowGuard = "Calc.eval(\"" + IntentNaming.pascalCase(guard.group(1))
                                    + "\", source, 6).compareTo(new java.math.BigDecimal(\"" + guard.group(3) + "\")) "
                                    + ("==".equals(guard.group(2)) ? "==" : "!=") + " 0";
                        }
                        continue;
                    }
                    java.util.regex.Matcher ruleRef = java.util.regex.Pattern.compile("rule\\((\\w+)\\)")
                                                                             .matcher(value);
                    Map<String, Object> assign = new LinkedHashMap<>();
                    assign.put("targetProp", IntentNaming.pascalCase(cell.getKey()));
                    if (ruleRef.matches()) {
                        String column = IntentNaming.pascalCase(ruleRef.group(1));
                        usedRuleColumns.add(column);
                        assign.put("expr", "ruleRow." + column);
                    } else {
                        FieldIntent target = fieldOf(itemsEntity, cell.getKey());
                        int scale = target != null && target.getScale() != null ? target.getScale() : 2;
                        // Reversal: the SAME expression negated on the SAME side (red storno).
                        String expr = isReverse ? "-(" + value + ")" : value;
                        assign.put("expr", "Calc.eval(\"" + expr.replace("\"", "\\\"") + "\", source, " + scale + ")");
                    }
                    assigns.add(assign);
                }
                rendered.put("guard", rowGuard);
                rendered.put("assigns", assigns);
                itemRows.add(rendered);
            }
            e.put("itemRows", itemRows);
            e.put("usedRuleColumns", new ArrayList<>(usedRuleColumns));
            out.add(e);
        }
        return out;
    }

    /**
     * Test hook: build the postings glue without a repository (convention fallbacks, deterministic).
     */
    static List<Map<String, Object>> buildPostingsForTest(IntentModel model) {
        return buildPostings(model, IntentEntities.byName(model), IntentEntities.compositionParents(model), IntentSettings.parse("{}"),
                null);
    }

    /** The entity's composition child (first entity declaring a composition to-one back to it). */
    private static EntityIntent compositionChild(EntityIntent entity, Map<String, EntityIntent> byName) {
        for (EntityIntent candidate : byName.values()) {
            if (candidate.getRelations() == null) {
                continue;
            }
            for (RelationIntent relation : candidate.getRelations()) {
                if (relation.isComposition() && entity.getName()
                                                      .equals(relation.getTo())) {
                    return candidate;
                }
            }
        }
        return null;
    }

    /** The entity's field by authored name (case-insensitive), or null. */
    private static FieldIntent fieldOf(EntityIntent entity, String name) {
        if (entity.getFields() == null || name == null) {
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
     * A posting header assignment: a bare source property name copies it; a value containing
     * {@code {prop}} placeholders becomes a Java string concatenation; anything else is a literal.
     */
    private static Map<String, Object> postingAssignment(String targetProperty, String value) {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("targetProp", IntentNaming.pascalCase(targetProperty));
        String v = value == null ? "" : value.trim();
        if (v.matches("\\w+") && !v.matches("\\d+")) {
            a.put("expr", "source." + IntentNaming.pascalCase(v));
        } else if (v.contains("{")) {
            StringBuilder expr = new StringBuilder();
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\{(\\w+)\\}")
                                                               .matcher(v);
            int last = 0;
            while (m.find()) {
                if (m.start() > last) {
                    if (expr.length() > 0) {
                        expr.append(" + ");
                    }
                    expr.append('"')
                        .append(v.substring(last, m.start())
                                 .replace("\"", "\\\""))
                        .append('"');
                }
                if (expr.length() > 0) {
                    expr.append(" + ");
                }
                expr.append("source.")
                    .append(IntentNaming.pascalCase(m.group(1)));
                last = m.end();
            }
            if (last < v.length()) {
                if (expr.length() > 0) {
                    expr.append(" + ");
                }
                expr.append('"')
                    .append(v.substring(last)
                             .replace("\"", "\\\""))
                    .append('"');
            }
            a.put("expr", expr.toString());
        } else {
            a.put("expr", javaLiteral(v));
        }
        return a;
    }

    /** A YAML scalar as a Java literal: numbers bare, everything else a quoted string. */
    private static String javaLiteral(Object value) {
        String v = String.valueOf(value);
        if (v.matches("-?\\d+")) {
            return v;
        }
        return '"' + v.replace("\"", "\\\"") + '"';
    }

    /**
     * Pre-render the target field assignments for a generate mapping: a {@code map} entry copies a
     * source property ({@code <sourceVar>.<Prop>}); a {@code defaults} entry sets {@code now} (today's
     * date) or a literal. The expression is rendered here (in Java, testable) so the Velocity template
     * only emits {@code target.<prop> = <expr>;} - no expression logic in the template.
     */
    private static List<Map<String, Object>> assignments(Map<String, String> map, Map<String, String> defaults, String sourceVar) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (map != null) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                if (entry.getValue() == null || entry.getValue()
                                                     .isBlank()) {
                    continue;
                }
                list.add(assignment(entry.getKey(), sourceVar + "." + IntentNaming.pascalCase(entry.getValue())));
            }
        }
        if (defaults != null) {
            for (Map.Entry<String, String> entry : defaults.entrySet()) {
                if (entry.getValue() == null || entry.getValue()
                                                     .isBlank()) {
                    continue;
                }
                list.add(assignment(entry.getKey(), literalExpression(entry.getValue())));
            }
        }
        return list;
    }

    /**
     * Like {@code assignments} but a NUMERIC literal default renders as a {@code BigDecimal} - the
     * child shapes (hours, amounts) are decimal columns, and a bare int literal does not convert to the
     * generated {@code BigDecimal} field.
     */
    private static List<Map<String, Object>> childAssignments(Map<String, String> map, Map<String, String> defaults, String sourceVar) {
        Map<String, String> typedDefaults = new LinkedHashMap<>();
        if (defaults != null) {
            typedDefaults.putAll(defaults);
        }
        List<Map<String, Object>> list = assignments(map, java.util.Map.of(), sourceVar);
        for (Map.Entry<String, String> entry : typedDefaults.entrySet()) {
            if (entry.getValue() == null || entry.getValue()
                                                 .isBlank()) {
                continue;
            }
            String expression = literalExpression(entry.getValue());
            if (expression.matches("-?\\d+(\\.\\d+)?")) {
                expression = "new java.math.BigDecimal(\"" + expression + "\")";
            }
            list.add(assignment(entry.getKey(), expression));
        }
        return list;
    }

    private static Map<String, Object> assignment(String targetProperty, String expression) {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("targetProp", IntentNaming.pascalCase(targetProperty));
        a.put("expr", expression);
        return a;
    }

    /**
     * A Java expression for a {@code defaults} value: {@code now} -> today's {@code LocalDate}; an
     * integer / decimal / boolean literal -> its Java form; anything else -> a quoted Java string.
     */
    private static String literalExpression(String value) {
        String v = value.trim();
        if ("now".equals(v)) {
            return "java.time.LocalDate.now()";
        }
        if ("true".equals(v) || "false".equals(v)) {
            return v;
        }
        if (v.matches("-?\\d+")) {
            return v;
        }
        if (v.matches("-?\\d+\\.\\d+")) {
            return "new java.math.BigDecimal(\"" + v + "\")";
        }
        return "\"" + v.replace("\\", "\\\\")
                       .replace("\"", "\\\"")
                       .replace("\n", "\\n")
                       .replace("\r", "\\r")
                + "\"";
    }

    /** The junction's to-one relation whose target is the given entity, or null. */
    private static RelationIntent relationTo(EntityIntent junction, String targetEntity) {
        if (junction.getRelations() == null || targetEntity == null) {
            return null;
        }
        for (RelationIntent relation : junction.getRelations()) {
            boolean toOne = "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
            if (toOne && targetEntity.equals(relation.getTo())) {
                return relation;
            }
        }
        return null;
    }

    /** PascalCase each name in the list (the match relations become FK property names). */
    private static List<String> pascalList(List<String> names) {
        List<String> out = new ArrayList<>();
        if (names != null) {
            for (String n : names) {
                if (n != null && !n.isBlank()) {
                    out.add(IntentNaming.pascalCase(n));
                }
            }
        }
        return out;
    }

    /** A Java boolean expression testing {@code s} against the payable status ids, or {@code true}. */
    private static String payableCondition(List<Integer> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return "true";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < statuses.size(); i++) {
            if (i > 0) {
                sb.append(" || ");
            }
            sb.append("s == ")
              .append(statuses.get(i));
        }
        return sb.toString();
    }

    private static Map<String, Object> rollupEntry(Map<String, Object> base, String className, String topicSuffix) {
        Map<String, Object> entry = new LinkedHashMap<>(base);
        entry.put("className", className);
        entry.put("topicSuffix", topicSuffix);
        return entry;
    }

    /**
     * Period expansions: per expansion, two handlers - on the master's create and update events - that
     * (re)generate the child rows for the span. Everything type-dependent (the defaults literals, the
     * count write-back) is pre-rendered here as Java lines so the template stays shape-only; the child
     * rows go through the child repository, so their create/delete events fire and downstream
     * roll-ups/guards run exactly as for hand-entered rows.
     */
    private static List<Map<String, Object>> buildExpansions(IntentModel model, Map<String, EntityIntent> byName,
            Map<String, String> compositionParents, IntentSettings settings) {
        List<Map<String, Object>> expansions = new ArrayList<>();
        for (ExpansionIntent expansion : model.getExpansions()) {
            if (expansion.getName() == null || expansion.getName()
                                                        .isBlank()) {
                continue;
            }
            EntityIntent master = byName.get(expansion.getFrom());
            EntityIntent child = byName.get(expansion.getInto());
            if (master == null || child == null || expansion.getBetween() == null) {
                continue; // parser already reported the bad reference
            }
            if (!settings.shouldGenerate("expansions", expansion.getName())) {
                LOGGER.info("Settings opt-out: keeping existing handlers for expansion [{}] (not generated)", expansion.getName());
                continue;
            }
            RelationIntent back = null;
            for (RelationIntent relation : child.getRelations()) {
                boolean toOne = "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
                if (toOne && expansion.getFrom()
                                      .equals(relation.getTo())) {
                    back = relation;
                    break;
                }
            }
            if (back == null) {
                continue;
            }
            String mapProperty = null;
            for (Map.Entry<String, String> entry : expansion.getMap()
                                                            .entrySet()) {
                if ("period".equals(entry.getValue())) {
                    mapProperty = IntentNaming.pascalCase(entry.getKey());
                    break;
                }
            }
            if (mapProperty == null) {
                continue;
            }
            String fkProperty = IntentNaming.pascalCase(back.getName());
            Map<String, Object> base = new LinkedHashMap<>();
            base.put("masterEntity", expansion.getFrom());
            base.put("masterPerspective", IntentEntities.resolvePerspective(expansion.getFrom(), compositionParents));
            base.put("masterPk", IntentEntities.keyFieldName(master));
            base.put("childEntity", expansion.getInto());
            base.put("childPerspective", IntentEntities.resolvePerspective(expansion.getInto(), compositionParents));
            base.put("fkProperty", fkProperty);
            base.put("startProperty", IntentNaming.pascalCase(expansion.getBetween()
                                                                       .getStart()));
            base.put("endProperty", IntentNaming.pascalCase(expansion.getBetween()
                                                                     .getEnd()));
            base.put("mapProperty", mapProperty);
            String unit = expansion.getUnit() == null || expansion.getUnit()
                                                                  .isBlank() ? "day"
                                                                          : expansion.getUnit()
                                                                                     .trim()
                                                                                     .toLowerCase();
            base.put("unit", unit);
            base.put("skipDays", expansion.getSkipDays()
                                          .stream()
                                          .map(String::valueOf)
                                          .collect(java.util.stream.Collectors.joining(",")));
            base.put("defaultsBlock", expansionDefaultsBlock(child, expansion));
            ExpansionIntent.Spread spread = expansion.getSpread();
            base.put("spreadTotalProperty", spread == null ? "" : IntentNaming.pascalCase(spread.getTotal()));
            base.put("spreadIntoProperty", spread == null ? "" : IntentNaming.pascalCase(spread.getInto()));
            base.put("spreadRound", spread == null || spread.getRound() == null ? "2"
                    : spread.getRound()
                            .toString());
            base.put("countProperty", expansionCountProperty(master, expansion));
            base.put("countValue", expansionCountValue(master, expansion));
            base.put("criteriaExpression",
                    "Criteria.create().eq(\"" + fkProperty + "\", master." + IntentEntities.keyFieldName(master) + ")");
            String className = IntentNaming.pascalIdentifier(expansion.getName()) + "Expansion";
            expansions.add(rollupEntry(base, className + "OnCreate", ""));
            expansions.add(rollupEntry(base, className + "OnUpdate", "-updated"));
        }
        return expansions;
    }

    /** Pre-rendered Java assignment lines for the expansion's literal child defaults. */
    private static String expansionDefaultsBlock(EntityIntent child, ExpansionIntent expansion) {
        StringBuilder block = new StringBuilder();
        for (Map.Entry<String, Object> entry : expansion.getDefaults()
                                                        .entrySet()) {
            FieldIntent field = fieldNamed(child, entry.getKey());
            if (field == null) {
                continue;
            }
            String property = IntentNaming.pascalCase(entry.getKey());
            Object value = entry.getValue();
            String literal;
            String type = field.getType() == null ? "string" : field.getType();
            switch (type) {
                case "integer", "int" -> literal = "Integer.valueOf(" + integralLiteral(value) + ")";
                case "long" -> literal = "Long.valueOf(" + integralLiteral(value) + "L)";
                case "decimal", "double" -> literal = "new java.math.BigDecimal(\"" + value + "\")";
                case "boolean" -> literal = String.valueOf(Boolean.parseBoolean(String.valueOf(value)));
                default -> literal = "\"" + String.valueOf(value)
                                                  .replace("\"", "\\\"")
                        + "\"";
            }
            block.append("            child.")
                 .append(property)
                 .append(" = ")
                 .append(literal)
                 .append(";\n");
        }
        return block.toString();
    }

    /** An integral literal for a YAML number (Gson parses YAML integers as Long or Double). */
    private static String integralLiteral(Object value) {
        if (value instanceof Number number) {
            return String.valueOf(number.longValue());
        }
        return String.valueOf(value);
    }

    /** The PascalCase master property receiving the count write-back, or empty when not declared. */
    private static String expansionCountProperty(EntityIntent master, ExpansionIntent expansion) {
        if (expansion.getCount() == null || expansion.getCount()
                                                     .isBlank()) {
            return "";
        }
        if (fieldNamed(master, expansion.getCount()) == null) {
            return "";
        }
        return IntentNaming.pascalCase(expansion.getCount());
    }

    /**
     * The count write-back value expression, typed to the master field. Paired with
     * {@link #expansionCountProperty} so the template persists the count as a targeted single-column
     * {@code updateProperty} instead of a full-row merge.
     */
    private static String expansionCountValue(EntityIntent master, ExpansionIntent expansion) {
        if (expansion.getCount() == null || expansion.getCount()
                                                     .isBlank()) {
            return "";
        }
        FieldIntent field = fieldNamed(master, expansion.getCount());
        if (field == null) {
            return "";
        }
        String type = field.getType() == null ? "decimal" : field.getType();
        return switch (type) {
            case "integer", "int" -> "Integer.valueOf(periods.size())";
            case "long" -> "Long.valueOf(periods.size())";
            default -> "java.math.BigDecimal.valueOf(periods.size())";
        };
    }

    /** The child/master field with the given authored name, or null. */
    private static FieldIntent fieldNamed(EntityIntent entity, String name) {
        for (FieldIntent field : entity.getFields()) {
            if (field.getName() != null && field.getName()
                                                .equals(name)) {
                return field;
            }
        }
        return null;
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
            Map<String, String> compositionParents, IntentSettings settings, IntentGenerationContext context) {
        List<Map<String, Object>> schedules = new ArrayList<>();
        for (ScheduleIntent schedule : model.getSchedules()) {
            if (schedule.getName() == null || schedule.getName()
                                                      .isBlank()) {
                continue;
            }
            String entity = schedule.getEntity();
            if (entity == null || !byName.containsKey(entity)) {
                continue;
            }
            boolean generates = schedule.getGenerate() != null;
            if (!generates && schedule.getNotify() == null) {
                continue; // parser already reported "no notify/generate action"
            }
            if (!settings.shouldGenerate("schedules", schedule.getName())) {
                LOGGER.info("Settings opt-out: keeping existing job for schedule [{}] (not generated)", schedule.getName());
                continue;
            }

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", schedule.getName());
            // pascalIdentifier (not pascalCase) so a hyphenated schedule name yields a valid Java class.
            entry.put("className", IntentNaming.pascalIdentifier(schedule.getName()));
            entry.put("cron", schedule.getCron());
            entry.put("entity", entity);
            entry.put("perspective", IntentEntities.resolvePerspective(entity, compositionParents));
            entry.put("criteriaExpression", ScheduleSupport.criteriaExpression(schedule));

            if (generates) {
                // Scheduled record generation: the queried row is the source, so its create-from maps the
                // loop variable (named "entity" in the job template) onto a fresh target and saves through
                // the target's repository. Item cloning is out of scope here (see parser validation).
                GeneratesIntent g = schedule.getGenerate();
                g.setFrom(entity);
                boolean crossModel = g.getUses() != null && !g.getUses()
                                                              .isBlank();
                UsesIntent uses = crossModel ? findUses(model, g.getUses()) : null;
                CrossModelSupport.TargetInfo target = uses == null ? null : CrossModelSupport.resolve(context, uses, g.getTo());
                String toPerspective =
                        target != null ? target.perspectiveName() : IntentEntities.resolvePerspective(g.getTo(), compositionParents);
                String toPk = target != null ? target.keyField() : IntentEntities.keyFieldName(byName.get(g.getTo()));
                entry.put("action", "generate");
                entry.put("genCrossModel", crossModel);
                entry.put("genToEntity", g.getTo());
                entry.put("genToModel", crossModel ? g.getUses() : "");
                entry.put("genToPerspective", toPerspective);
                entry.put("genToPk", toPk);
                entry.put("genFieldAssignments", assignments(g.getMap(), g.getDefaults(), "entity"));
                if (g.getChildren() != null && !g.getChildren()
                                                 .isEmpty()) {
                    // Collection-driven children: one row per element of a source collection, saved
                    // under the just-generated parent. Everything is pre-rendered here (the
                    // expansions convention) - the job template stays shape-only.
                    entry.put("genChildren", buildGenerateChildren(g.getChildren(), uses, byName, compositionParents, context, 1));
                }
            } else {
                // The per-row action reuses the notification machinery against the queried row entity.
                NotificationSupport.Plan plan =
                        NotificationSupport.plan(schedule.getNotify(), byName.get(entity), byName, compositionParents);
                if (plan == null) {
                    LOGGER.warn("Schedule [{}] notify recipient [{}] is not a resolvable field or relation.field of [{}] - skipping",
                            schedule.getName(), schedule.getNotify()
                                                        .getTo(),
                            entity);
                    continue;
                }
                entry.put("action", "notify");
                entry.put("relationLoads", relationLoads(plan));
                entry.put("toExpression", plan.toExpression());
                entry.put("subjectExpression", plan.subjectExpression());
                entry.put("bodyExpression", plan.bodyExpression());
            }
            schedules.add(entry);
        }
        return schedules;
    }

    /**
     * Test hook: build the {@code schedules} glue collection without a repository (a null context makes
     * a cross-model generate target fall back to naming-convention defaults, enough to assert the
     * mapping and action shape).
     */
    static List<Map<String, Object>> buildSchedulesForTest(IntentModel model) {
        return buildSchedules(model, IntentEntities.byName(model), IntentEntities.compositionParents(model), IntentSettings.parse("{}"),
                null);
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

    /**
     * One expire date loader per user task with an {@code expire: { until: <date field> }} timer: a
     * {@code JavaDelegate} inserted before the task (by the BPMN generator) that re-reads the trigger
     * entity's date field at task entry and publishes the {@code java.util.Date} process variable the
     * boundary timer's {@code timeDate} binds to (see {@link ProcessTimerSupport}).
     */
    private static List<Map<String, Object>> buildTimerLoaders(IntentModel model, IntentSettings settings) {
        List<Map<String, Object>> loaders = new ArrayList<>();
        for (ProcessTimerSupport.TimerLoad load : ProcessTimerSupport.timerLoads(model)) {
            if (!settings.shouldGenerate("timerLoaders", load.handler())) {
                LOGGER.info("Settings opt-out: keeping existing handler for timer loader [{}] (not generated)", load.handler());
                continue;
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("process", load.process());
            entry.put("handler", load.handler());
            entry.put("ownerEntity", load.ownerEntity());
            entry.put("ownerPerspective", load.ownerPerspective());
            entry.put("ownerKeyProperty", load.ownerKeyProperty());
            entry.put("ownerKeyAccessor", load.ownerKeyAccessor());
            entry.put("variable", load.variable());
            entry.put("dueExpression", load.dueExpression());
            loaders.add(entry);
        }
        return loaders;
    }

    /**
     * One wait listener per {@code wait} step: a {@code MessageHandler} on the event entity's topic
     * that resolves the process-carrying record (through the {@code via:} back-reference, or the event
     * record itself), reads its stamped {@code ProcessId} and correlates the parked catch event's
     * message (see {@link ProcessWaitSupport}). Fail-soft: no parked instance is a no-op.
     */
    private static List<Map<String, Object>> buildWaits(IntentModel model, IntentSettings settings) {
        List<Map<String, Object>> waits = new ArrayList<>();
        for (ProcessWaitSupport.Wait wait : ProcessWaitSupport.waits(model)) {
            if (!settings.shouldGenerate("waits", wait.className())) {
                LOGGER.info("Settings opt-out: keeping existing handler for wait [{}] (not generated)", wait.className());
                continue;
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("process", wait.process());
            entry.put("className", wait.className());
            entry.put("messageName", wait.messageName());
            entry.put("eventEntity", wait.eventEntity());
            entry.put("eventPerspective", wait.eventPerspective());
            entry.put("eventKeyProperty", wait.eventKeyProperty());
            entry.put("topicSuffix", EventBinding.topicSuffix(wait.eventKind()));
            entry.put("guardExpression", NotificationSupport.guard(wait.when()));
            // Blank in the direct case (the event entity is the trigger entity itself, carrying its
            // own ProcessId); the template branches on it.
            entry.put("viaFkProperty", wait.viaFkProperty() == null ? "" : wait.viaFkProperty());
            entry.put("parentEntity", wait.parentEntity());
            entry.put("parentPerspective", wait.parentPerspective());
            waits.add(entry);
        }
        return waits;
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
                f.put("coercion", field.coercion());
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
            entry.put("relation", setter.relation() ? "true" : "false");
            setters.add(entry);
        }
        return setters;
    }
}
