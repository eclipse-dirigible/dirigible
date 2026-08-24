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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.eclipse.dirigible.components.intent.model.InboundSourceIntent;
import org.eclipse.dirigible.components.intent.model.IntegrationIntent;
import org.eclipse.dirigible.components.intent.model.OutboundIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.LifecycleStages;
import org.eclipse.dirigible.components.intent.model.NotificationIntent;
import org.eclipse.dirigible.components.intent.model.PostingRuleSelector;
import org.eclipse.dirigible.components.intent.model.ProcessIntent;
import org.eclipse.dirigible.components.intent.model.RelationIntent;
import org.eclipse.dirigible.components.intent.model.ExpansionIntent;
import org.eclipse.dirigible.components.intent.model.RollupIntent;
import org.eclipse.dirigible.components.intent.model.ScheduleConditionIntent;
import org.eclipse.dirigible.components.intent.model.ScheduleIntent;
import org.eclipse.dirigible.components.intent.model.SettlementIntent;
import org.eclipse.dirigible.components.intent.model.UsesIntent;
import org.eclipse.dirigible.components.intent.parser.IntentValidationException;
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
 * <li>{@code assignees} - one per user task whose {@code assignee} is a relation walk off the
 * trigger record; generates the {@code JavaDelegate} that walks it to the person the task belongs
 * to and publishes their login (see {@link ProcessAssigneeSupport}).</li>
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
        List<Map<String, Object>> assignees = buildAssignees(model, settings, context);
        List<Map<String, Object>> timerLoaders = buildTimerLoaders(model, settings);
        List<Map<String, Object>> waits = buildWaits(model, settings);
        List<Map<String, Object>> aborts = buildAborts(model, settings);
        List<Map<String, Object>> writers = buildWriters(model, settings);
        List<Map<String, Object>> setters = buildSetters(model, settings);
        List<Map<String, Object>> notifications = buildNotifications(model, byName, compositionParents, settings, context);
        List<Map<String, Object>> schedules = buildSchedules(model, byName, compositionParents, settings, context);
        List<Map<String, Object>> integrations = buildIntegrations(model, byName, compositionParents, settings, context);
        List<Map<String, Object>> inbound = buildInbound(model, byName, compositionParents, settings, context);
        List<Map<String, Object>> inboundMessages = buildInboundMessages(model, byName, compositionParents, settings, context);
        List<Map<String, Object>> inboundFiles = buildInboundFiles(model, byName, compositionParents, settings, context);
        List<Map<String, Object>> outbound = buildOutbound(model, byName, compositionParents, settings, context);
        List<Map<String, Object>> stepEvents = buildStepEvents(model, compositionParents, settings);
        List<Map<String, Object>> rollups = buildRollups(model, byName, compositionParents, settings, context);
        ExpansionHandlers expansionHandlers = buildExpansions(model, byName, compositionParents, settings);
        List<Map<String, Object>> expansions = expansionHandlers.reconciliations();
        List<Map<String, Object>> expansionCleanups = expansionHandlers.cleanups();
        List<Map<String, Object>> settlements = buildSettlements(model, byName, compositionParents, settings, context);
        List<Map<String, Object>> settlementListeners = buildSettlementListeners(settlements);
        List<Map<String, Object>> generates = buildGenerates(model, byName, compositionParents, settings, context);
        List<Map<String, Object>> transitions = buildTransitions(model, byName, compositionParents, settings, context);
        List<Map<String, Object>> sends = buildSends(model, byName, compositionParents, settings, context);
        List<Map<String, Object>> postings = buildPostings(model, byName, compositionParents, settings, context);
        List<Map<String, Object>> posts = buildPosts(model, byName, compositionParents);
        List<Map<String, Object>> aggregates = buildAggregates(model, byName, compositionParents);
        List<Map<String, Object>> resolves = buildResolves(model, byName, compositionParents, settings);
        List<Map<String, Object>> printFeeders = PrintFeederSupport.buildPrintFeeders(model, byName, compositionParents, context);
        List<Map<String, Object>> snapshots =
                SnapshotSupport.buildSnapshots(model, byName, compositionParents, crossModelLookup(model, context));
        List<Map<String, Object>> numbering = NumberingSupport.buildNumbering(model, compositionParents);

        if (triggers.isEmpty() && resolvers.isEmpty() && fieldLoaders.isEmpty() && assignees.isEmpty() && timerLoaders.isEmpty()
                && waits.isEmpty() && aborts.isEmpty() && writers.isEmpty() && setters.isEmpty() && notifications.isEmpty()
                && schedules.isEmpty() && integrations.isEmpty() && inbound.isEmpty() && inboundMessages.isEmpty() && inboundFiles.isEmpty()
                && outbound.isEmpty() && stepEvents.isEmpty() && rollups.isEmpty() && expansions.isEmpty() && settlements.isEmpty()
                && generates.isEmpty() && transitions.isEmpty() && printFeeders.isEmpty() && postings.isEmpty() && snapshots.isEmpty()
                && numbering.isEmpty() && posts.isEmpty() && aggregates.isEmpty() && sends.isEmpty() && resolves.isEmpty()) {
            // No process glue for this intent - any stale .glue is removed by the post-pass scrub.
            return;
        }

        Map<String, Object> glue = new LinkedHashMap<>();
        glue.put("triggers", triggers);
        glue.put("resolvers", resolvers);
        glue.put("fieldLoaders", fieldLoaders);
        glue.put("assignees", assignees);
        glue.put("timerLoaders", timerLoaders);
        glue.put("waits", waits);
        glue.put("aborts", aborts);
        glue.put("writers", writers);
        glue.put("setters", setters);
        glue.put("notifications", notifications);
        glue.put("schedules", schedules);
        glue.put("integrations", integrations);
        glue.put("inbound", inbound);
        glue.put("inboundMessages", inboundMessages);
        glue.put("inboundFiles", inboundFiles);
        glue.put("outbound", outbound);
        // One emitter per observed process-step moment, deduplicated across every notification and
        // integration bound to it: the delegate the BPMN generator inserts at that boundary publishes
        // the trigger entity on the step topic those consumers already bind to.
        glue.put("stepEvents", stepEvents);
        glue.put("rollups", rollups);
        glue.put("expansions", expansions);
        glue.put("expansionCleanups", expansionCleanups);
        glue.put("settlements", settlements);
        glue.put("settlementListeners", settlementListeners);
        glue.put("generates", generates);
        // The event-driven subset (issue #6711) - the SAME descriptors, filtered, so the listener and
        // the create-from it calls can never be built from divergent data. A create-from with no event
        // must contribute no listener, which is why this is a collection of its own rather than a flag
        // the listener template branches on (one file per entry is the collection contract).
        glue.put("generateEvents", generates.stream()
                                            .filter(entry -> Boolean.TRUE.equals(entry.get("hasEvent")))
                                            .toList());
        // The declared-reopen subset (issue #6868), filtered from the same descriptors for the same
        // reason: the listener that returns the source when its target is retired must agree with the
        // create-from's own guard about what "retired" means, and a create-from that declares no reopen
        // must contribute no listener at all.
        glue.put("generateReopens", generates.stream()
                                             .filter(entry -> Boolean.TRUE.equals(entry.get("hasReopen")))
                                             .toList());
        glue.put("transitions", transitions);
        glue.put("sends", sends);
        glue.put("postings", postings);
        glue.put("posts", posts);
        glue.put("aggregates", aggregates);
        glue.put("resolves", resolves);
        glue.put("printFeeders", printFeeders);
        glue.put("snapshots", snapshots);
        glue.put("numbering", numbering);
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
            trigger.put("perspective", IntentEntities.resolvePerspective(entity, compositionParents, model));
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
     * {@code forEach} collection entity is LOCAL by default, or cross-model when the child's
     * {@code forEach} carries a {@code model:} alias (resolved through {@link CrossModelSupport}). The
     * row variable is {@code r<depth>}; field assignments are pre-rendered against it, defaults against
     * literals.
     */
    private static List<Map<String, Object>> buildGenerateChildren(List<GenerateChildIntent> children, UsesIntent uses, IntentModel model,
            Map<String, EntityIntent> byName, Map<String, String> compositionParents, IntentGenerationContext context, int depth) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (GenerateChildIntent child : children) {
            Map<String, Object> entry = new LinkedHashMap<>();
            boolean crossModel = uses != null;
            CrossModelSupport.TargetInfo target = crossModel ? CrossModelSupport.resolve(context, uses, child.getTo()) : null;
            entry.put("toEntity", child.getTo());
            entry.put("toCrossModel", crossModel);
            entry.put("toModel", crossModel ? uses.getModel() : "");
            entry.put("toPerspective", target != null ? target.perspectiveName()
                    : IntentEntities.resolvePerspective(child.getTo(), compositionParents, model));
            entry.put("toPk", target != null ? target.keyField() : IntentEntities.keyFieldName(byName.get(child.getTo())));
            entry.put("parentFkProperty", IntentNaming.pascalCase(child.getParent()));
            Object days = child.getForEach()
                               .get("days");
            String rowVar = "r" + depth;
            if (days != null) {
                entry.put("kind", "days");
                entry.put("dayField", IntentNaming.pascalCase(child.getDayField()));
                entry.put("fieldAssignments",
                        childAssignments(java.util.Map.of(), child.getDefaults(), rowVar,
                                temporalKinds(crossModel ? null : byName.get(child.getTo()), target),
                                relationProperties(crossModel ? null : byName.get(child.getTo()), target)));
            } else {
                entry.put("kind", "entity");
                String collection = String.valueOf(child.getForEach()
                                                        .get("entity"));
                Object forEachModelObj = child.getForEach()
                                              .get("model");
                boolean forEachCrossModel = forEachModelObj != null && !String.valueOf(forEachModelObj)
                                                                              .isBlank();
                entry.put("forEachEntity", collection);
                entry.put("forEachCrossModel", forEachCrossModel);
                entry.put("forEachModel", forEachCrossModel ? String.valueOf(forEachModelObj) : "");
                if (forEachCrossModel) {
                    // The collection lives in another model; its perspective comes from the owner's
                    // .model (already validated resolvable in firstUnresolvableScheduleRef, so this
                    // resolve does not fail for a schedule that reached here).
                    UsesIntent collectionUses = findUses(model, String.valueOf(forEachModelObj));
                    CrossModelSupport.TargetInfo collectionTarget =
                            collectionUses == null ? null : CrossModelSupport.resolve(context, collectionUses, collection);
                    entry.put("forEachPerspective", collectionTarget != null ? collectionTarget.perspectiveName() : collection);
                } else {
                    entry.put("forEachPerspective", IntentEntities.resolvePerspective(collection, compositionParents, model));
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> match = (Map<String, Object>) child.getForEach()
                                                                       .get("match");
                Map.Entry<String, Object> condition = match.entrySet()
                                                           .iterator()
                                                           .next();
                entry.put("matchProperty", IntentNaming.pascalCase(condition.getKey()));
                entry.put("matchSourceExpression", "entity." + IntentNaming.pascalCase(String.valueOf(condition.getValue())));
                entry.put("fieldAssignments",
                        childAssignments(toStringMap(child.getMap()), child.getDefaults(), rowVar,
                                temporalKinds(crossModel ? null : byName.get(child.getTo()), target),
                                relationProperties(crossModel ? null : byName.get(child.getTo()), target)));
            }
            entry.put("rowVar", rowVar);
            if (child.getChildren() != null && !child.getChildren()
                                                     .isEmpty()) {
                entry.put("children",
                        buildGenerateChildren(child.getChildren(), uses, model, byName, compositionParents, context, depth + 1));
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
                // own name - the settings-aware resolvePerspective handles it, or the FK URL 404s.
                link.put("targetPerspective", IntentEntities.resolvePerspective(relation.getTo(), compositionParents, model));
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
                trigger.put("personalTargetPerspective", IntentEntities.resolvePerspective(relation.getTo(), compositionParents, model));
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
            Map<String, String> compositionParents, IntentSettings settings, IntentGenerationContext context) {
        List<Map<String, Object>> notifications = new ArrayList<>();
        NotificationSupport.CrossModelLookup lookup = crossModelLookup(model, context);
        for (NotificationIntent notification : model.getNotifications()) {
            if (notification.getName() == null || notification.getName()
                                                              .isBlank()) {
                continue;
            }
            // Either axis: the entity a lifecycle event names, or the trigger entity of the process a
            // step event names - the record the message is about, and what every path resolves against.
            String entity = StepEventSupport.eventEntity(model, notification.getEvent());
            if (entity == null || !byName.containsKey(entity)) {
                continue;
            }
            if (!settings.shouldGenerate("notifications", notification.getName())) {
                LOGGER.info("Settings opt-out: keeping existing listener for notification [{}] (not generated)", notification.getName());
                continue;
            }
            NotificationSupport.Plan plan = NotificationSupport.plan(notification, byName.get(entity), byName, compositionParents, lookup);
            if (plan == null) {
                reportDroppedGlue(context, "Notification [" + notification.getName() + "] recipient [" + notification.getTo()
                        + "] is not a resolvable field or relation.field of [" + entity + "] - the notification was NOT generated");
                continue;
            }
            NotifySupport.PrintAttachment attachment = printAttachment(notification, byName.get(entity), model, byName, compositionParents,
                    context, "Notification [" + notification.getName() + "]");
            if (attachment == null && NotifySupport.attachesPrint(notification)) {
                continue; // asked for the document but it cannot be rendered - reported above
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", notification.getName());
            entry.put("className", IntentNaming.pascalCase(notification.getName()));
            entry.put("entity", entity);
            entry.put("perspective", IntentEntities.resolvePerspective(entity, compositionParents, model));
            // The PK property an `attach: print` hands to the generated print feeder. Deliberately
            // NOT named keyProperty: that key marks a TRIGGER entry (its process variable), and the
            // engine IT keys "no trigger was generated" on trigger-only keys being absent.
            entry.put("attachKeyProperty", IntentEntities.keyFieldName(byName.get(entity)));
            entry.put("topicSuffix", StepEventSupport.topicSuffix(notification.getEvent()));
            entry.put("relationLoads", relationLoads(plan, attachment));
            entry.put("guardExpression", plan.guardExpression());
            entry.put("toExpression", plan.toExpression());
            entry.put("subjectExpression", plan.subjectExpression());
            entry.put("bodyExpression", plan.bodyExpression());
            entry.putAll(NotifySupport.attachmentFields(attachment));
            entry.putAll(NotifySupport.deepLinkFields(plan, byName.get(entity)));
            notifications.add(entry);
        }
        return notifications;
    }

    private static List<Map<String, Object>> buildRollups(IntentModel model, Map<String, EntityIntent> byName,
            Map<String, String> compositionParents, IntentSettings settings, IntentGenerationContext context) {
        List<Map<String, Object>> rollups = new ArrayList<>();
        for (RollupIntent rollup : model.getRollups()) {
            if (rollup.getName() == null || rollup.getName()
                                                  .isBlank()) {
                continue;
            }
            // A CROSS-MODEL CHILD: the counted rows are owned by another model, the total lands on a
            // LOCAL parent this roll-up names outright (a foreign child's relations are not in this
            // document, so `via` alone cannot point at one). The handler binds the OWNER project's topic
            // and reads the rows back through the owner's generated repository - the shape an n:m
            // allocation needs, whose link rows live with one side of the pairing while the other side's
            // total belongs here.
            boolean crossModelChild = rollup.isCrossModelChild();
            EntityIntent child = crossModelChild ? null : byName.get(rollup.getEntity());
            RelationIntent via = child == null ? null : toOneRelation(child, rollup.getVia());
            EntityIntent parent = via == null ? null : byName.get(via.getTo());
            // A CROSS-MODEL parent: the child is local (it owns the event this handler binds to), the
            // parent is owned by another model and reached through `uses`. Its coordinates therefore come
            // from the OWNER's .model, exactly as a cross-model relation target does - the local byName
            // has no entry for it, which is why such a roll-up used to be impossible to express.
            boolean crossModelParent = via != null && via.getModel() != null && !via.getModel()
                                                                                    .isBlank();
            CrossModelSupport.TargetInfo parentTarget = null;
            String childPerspective = null;
            String childProject = "";
            String parentEntity = null;
            String parentPerspective = null;
            if (crossModelChild) {
                UsesIntent uses = findUses(model, rollup.getModel());
                EntityIntent localParent = rollup.getParent() == null ? null : byName.get(rollup.getParent());
                if (uses == null || localParent == null) {
                    continue; // parser already reported the undeclared alias / non-local parent
                }
                CrossModelSupport.TargetInfo childTarget;
                try {
                    childTarget = CrossModelSupport.resolve(context, uses, rollup.getEntity());
                } catch (IntentValidationException ex) {
                    reportDroppedGlue(context, "Roll-up [" + rollup.getName() + "] child entity [" + rollup.getEntity() + "] in model ["
                            + rollup.getModel() + "] cannot be resolved: " + ex.getMessage() + " - not generated");
                    continue;
                }
                // The owner model WAS read: every property this roll-up reads off the foreign child must
                // be one of its own. The parser cannot check them (the entity is not local), so a miss is
                // reported here rather than emitted as a handler that fails the client-Java batch.
                String missing = firstUnresolvableChildProperty(rollup, childTarget);
                if (missing != null) {
                    reportDroppedGlue(context, "Roll-up [" + rollup.getName() + "] " + missing + " is not a property of cross-model child ["
                            + rollup.getModel() + ":" + rollup.getEntity() + "] - not generated");
                    continue;
                }
                // And `via` must reference THIS roll-up's parent. A property that exists but points at
                // something else would key the aggregate on foreign ids and look up parents by them -
                // wrong totals with nothing anywhere saying so, which is worse than not generating.
                String references = childTarget.propertyRelations() == null ? null
                        : childTarget.propertyRelations()
                                     .get(IntentNaming.pascalCase(rollup.getVia()));
                if (childTarget.resolved() && childTarget.propertyRelations() != null && !localParent.getName()
                                                                                                     .equals(references)) {
                    reportDroppedGlue(context,
                            "Roll-up [" + rollup.getName() + "] via [" + rollup.getVia() + "] of cross-model child [" + rollup.getModel()
                                    + ":" + rollup.getEntity() + "] references ["
                                    + (references == null ? "nothing - it is not a relation" : references) + "], not the parent ["
                                    + localParent.getName() + "] - not generated");
                    continue;
                }
                childPerspective = childTarget.perspectiveName();
                childProject = uses.resolveProject();
                parentEntity = localParent.getName();
                parentPerspective = IntentEntities.resolvePerspective(parentEntity, compositionParents, model);
            } else if (crossModelParent) {
                UsesIntent uses = findUses(model, via.getModel());
                if (uses == null) {
                    reportDroppedGlue(context, "Roll-up [" + rollup.getName() + "] reaches its parent through model [" + via.getModel()
                            + "], which is not declared in uses - not generated");
                    continue;
                }
                parentTarget = CrossModelSupport.resolve(context, uses, via.getTo());
                String counter = IntentNaming.pascalCase(rollup.getField());
                if (parentTarget.resolved() && parentTarget.propertyNames() != null && !parentTarget.propertyNames()
                                                                                                    .contains(counter)) {
                    // The owner model WAS read and carries no such property. The parser cannot catch this
                    // (the entity is not local), so surface it here rather than emit a handler that would
                    // fail the client-Java batch.
                    reportDroppedGlue(context, "Roll-up [" + rollup.getName() + "] field [" + rollup.getField()
                            + "] is not a property of cross-model parent [" + via.getModel() + ":" + via.getTo() + "] - not generated");
                    continue;
                }
            } else if (parent == null) {
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
            // Empty for a local child - the pipeline then uses this project's gen folder and name, so a
            // local roll-up renders exactly as before.
            base.put("childCrossModel", crossModelChild);
            base.put("childModel", crossModelChild ? rollup.getModel() : "");
            base.put("childProject", childProject);
            // A setting entity's generated code lives under the shared "Settings" perspective, not its
            // own name - so a roll-up whose child/parent is `kind: setting` must resolve there, like
            // the relation-link / personal-assignee builders do. Without this the generated handler
            // imports gen.<mod>.data.<entityname> (which does not exist) instead of ...data.settings
            // and the whole client-Java batch fails to compile (a setting-entity roll-up, e.g.
            // Currency <- CurrencyRate, is the case that exposed it).
            // A cross-model child's perspective comes from ITS owner's model (resolved above).
            base.put("childPerspective",
                    crossModelChild ? childPerspective : IntentEntities.resolvePerspective(rollup.getEntity(), compositionParents, model));
            base.put("parentEntity", crossModelChild ? parentEntity : via.getTo());
            // A cross-model parent's perspective comes from the owner's model (resolved above); a local
            // one from this model's own composition/setting layout.
            base.put("parentPerspective", crossModelParent ? parentTarget.perspectiveName()
                    : crossModelChild ? parentPerspective : IntentEntities.resolvePerspective(via.getTo(), compositionParents, model));
            // Empty for a local parent - the generation pipeline then falls back to this project's gen folder.
            base.put("parentModel", crossModelParent ? via.getModel() : "");
            base.put("parentCrossModel", crossModelParent);
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
            if (withCapacity && crossModelChild) {
                // The balance and the status ARE maintained (both are writes on the local parent), but the
                // capacity GUARD - the check that refuses a child row overdrawing the parent - is emitted
                // into the child's own DAO, which the owner model generates. Said out loud, because a
                // capacity that enforces nothing is exactly what must not pass for a limit.
                String warning = "Roll-up [" + rollup.getName() + "] measures the cross-model child [" + rollup.getModel() + ":"
                        + rollup.getEntity() + "] against capacity [" + rollup.getCapacity()
                        + "]: the balance and status are maintained, but the overdraw GUARD is not installed - it belongs to the child's"
                        + " own repository, which the [" + rollup.getModel() + "] model generates.";
                LOGGER.warn(warning);
                if (context != null) {
                    context.addIssue(warning);
                }
            }
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
            // The generation pipeline groups every roll-up sharing (childEntity, fkProperty, event) into one
            // handler, so
            // the name must be shared across the group. Two roll-ups on the same child+fk+event collapse into
            // this one class.
            // A foreign child is qualified by its model: a local and a foreign child of the SAME name
            // rolling up through the same relation are two different handlers, and one class name for
            // both would have the pipeline write one over the other.
            String className = (crossModelChild ? IntentNaming.pascalIdentifier(rollup.getModel()) : "") + rollup.getEntity() + fkProperty;
            rollups.add(rollupEntry(base, className + "RollupOnCreate", ""));
            // EVERY op recomputes on update, not just sum / latest: a line edit changes the sum (or which
            // row is latest, or its value), and an edit that RE-PARENTS a child - the ordinary way a child
            // moves between parents - changes the count of the parent it moved TO. The recompute is the
            // same query for every op and reads the child rows back from the store, so the update handler
            // is idempotent and never op-specific. (The parent the child moved AWAY from is repaired by
            // the RollupOnRekey handler below, off the "-rekeyed" event the DAO publishes for the move.)
            rollups.add(rollupEntry(base, className + "RollupOnUpdate", "-updated"));
            rollups.add(rollupEntry(base, className + "RollupOnDelete", "-deleted"));
            // Re-parenting: the child's create/update/delete events all name the parent it belongs to NOW,
            // so the parent it moved AWAY from is named by no event of theirs and kept the child's
            // contribution forever (#6819). The DAO publishes the row on "-rekeyed" whenever a grouping
            // column moves - the previous row from the full-row write, both the previous and the written
            // one from the targeted writes that publish no "-updated" at all. It is the same recompute
            // keyed on the payload's FK, so one handler repairs whichever side the payload names.
            rollups.add(rollupEntry(base, className + "RollupOnRekey", "-rekeyed"));
        }
        return rollups;
    }

    /**
     * The first property this roll-up reads off a cross-model child that the owner's model does not
     * declare - its {@code via} FK, and the {@code of} / {@code by} fields the aggregation reads.
     * Returns null when everything resolves, and also when the owner's model could not be read (the
     * convention fallback), which is the same rule {@code dependsOn} and the schedules use: never fail
     * a generation on a model that was not there to check against.
     *
     * @param rollup the roll-up
     * @param child the resolved cross-model child
     * @return a description of the first unresolvable property, or null
     */
    private static String firstUnresolvableChildProperty(RollupIntent rollup, CrossModelSupport.TargetInfo child) {
        if (!child.resolved() || child.propertyNames() == null) {
            return null;
        }
        java.util.Map<String, String> read = new LinkedHashMap<>();
        read.put("via", rollup.getVia());
        if ("sum".equals(rollup.getOp()) || "latest".equals(rollup.getOp())) {
            read.put("of", rollup.getOf());
        }
        if ("latest".equals(rollup.getOp())) {
            read.put("by", rollup.getBy());
        }
        for (Map.Entry<String, String> entry : read.entrySet()) {
            String property = entry.getValue();
            if (property != null && !property.isBlank() && !child.propertyNames()
                                                                 .contains(IntentNaming.pascalCase(property))) {
                return entry.getKey() + " [" + property + "]";
            }
        }
        return null;
    }

    /**
     * One glue entry per {@link SettlementIntent}: resolves the junction's two FK properties, the
     * invoice open-amount fields, and the cross-model payment's project/perspective/topic (via
     * {@link CrossModelSupport}) so the two settlement templates (onPayment listener + onInvoice
     * delegate) can be rendered. Java-package sanitization happens in the {@code settlements} case of
     * the generation pipeline (same as triggers), keeping this generator at logical names.
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
            e.put("invoicePerspective", IntentEntities.resolvePerspective(s.getInvoice(), compositionParents, model));
            e.put("invoicePk", IntentEntities.keyFieldName(invoice));
            e.put("invoiceTotal", IntentNaming.pascalCase(s.getTotal()));
            e.put("invoicePaid", IntentNaming.pascalCase(s.getPaid()));
            e.put("order", IntentNaming.pascalCase(s.getOrder()));
            e.put("invoiceStatus", s.getStatus() == null ? "" : IntentNaming.pascalCase(s.getStatus()));
            e.put("payableCondition", payableCondition(s.getPayableStatuses()));
            // junction (this project)
            e.put("junctionEntity", s.getJunction());
            e.put("junctionPerspective", IntentEntities.resolvePerspective(s.getJunction(), compositionParents, model));
            e.put("junctionPk", IntentEntities.keyFieldName(junction));
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
     * One payment-listener entry per settlement per event moment of the payment: its create AND its
     * update. The allocation is written as a recompute of the payment's unallocated balance, so binding
     * the correction event too is safe by construction - a re-delivery with nothing left to allocate
     * does nothing. Bound to create alone (#6818), a payment booked for the wrong amount and corrected
     * afterwards, or created in a draft state and completed later, was never (re-)allocated and the
     * invoice silently kept the original settled figure.
     *
     * @param settlements the settlement descriptors
     * @return one entry per settlement per bound event
     */
    private static List<Map<String, Object>> buildSettlementListeners(List<Map<String, Object>> settlements) {
        List<Map<String, Object>> listeners = new ArrayList<>();
        for (Map<String, Object> settlement : settlements) {
            String name = String.valueOf(settlement.get("name"));
            // The create handler keeps its established class name; the correction one is suffixed.
            listeners.add(rollupEntry(settlement, name + "OnPayment", ""));
            listeners.add(rollupEntry(settlement, name + "OnPaymentUpdated", "-updated"));
            if (!Boolean.TRUE.equals(settlement.get("crossModel"))) {
                // A corrected MATCH column re-targets the allocation wholesale: the payment's DAO
                // publishes "-rekeyed" for the move (the match columns are grouping keys), and this
                // handler releases everything and re-allocates from the STORE - which needs the
                // payment's repository, so it exists only for a local payment. A cross-model payment's
                // DAO belongs to the owner model, which knows nothing of this settlement.
                listeners.add(rollupEntry(settlement, name + "OnPaymentRekeyed", "-rekeyed"));
            }
        }
        return listeners;
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
     *
     * <p>
     * An entry declaring an {@code event} (issue #6711) additionally lands in the
     * {@code generateEvents} collection, whose template renders the listener that calls the same
     * create-from - see {@link #putGeneratesEvent}. One declaring a {@code sourceStatusOnRetire} (issue
     * #6868) lands in {@code generateReopens} as well, whose template renders the listener that returns
     * the source when the target it made is retired - see {@link #putSupersededTarget}.
     */
    private static List<Map<String, Object>> buildGenerates(IntentModel model, Map<String, EntityIntent> byName,
            Map<String, String> compositionParents, IntentSettings settings, IntentGenerationContext context) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (GeneratesIntent g : model.getGenerates()) {
            if (g.getName() == null || g.getName()
                                        .isBlank()) {
                continue;
            }
            // The SOURCE is normally a local entity; with `fromUses:` it is owned by another model and
            // resolved from that model's .model exactly as a cross-model TARGET is. Authoring the
            // create-from on the TARGET's module is what keeps the two modules' generated Java a DAG:
            // otherwise "A generates into B" puts a reference to B inside A while B already references
            // A, and neither can be compiled - or packaged as a jar - before the other.
            boolean crossModelSource = g.isCrossModelSource();
            UsesIntent fromUses = crossModelSource ? findUses(model, g.getFromUses()) : null;
            CrossModelSupport.TargetInfo sourceInfo = fromUses == null ? null : CrossModelSupport.resolve(context, fromUses, g.getFrom());
            EntityIntent source = crossModelSource ? null : byName.get(g.getFrom());
            if (source == null && !crossModelSource) {
                continue; // parser already reported the bad reference
            }
            // A one-hop map source needs the SOURCE's relations to know what to load. With `fromUses:`
            // they live in the owner's .model and nothing here can resolve them, so the hop is refused
            // instead of emitting a read that walks a foreign key as if it were an object.
            if (crossModelSource && firstHop(g.getMap()) != null) {
                reportDroppedGlue(context,
                        "generates [" + g.getName() + "] map [" + firstHop(g.getMap())
                                + "] is a relation.field of a cross-model source (fromUses [" + g.getFromUses()
                                + "]), whose relations are known only to that model - map a direct property of the source, or author the"
                                + " create-from in [" + g.getFromUses() + "] - the create-from was NOT generated");
                continue;
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
                    target != null ? target.perspectiveName() : IntentEntities.resolvePerspective(g.getTo(), compositionParents, model);
            String toPk = target != null ? target.keyField() : IntentEntities.keyFieldName(byName.get(g.getTo()));
            String fromPerspective = sourceInfo != null ? sourceInfo.perspectiveName()
                    : IntentEntities.resolvePerspective(g.getFrom(), compositionParents, model);
            String fromPk = sourceInfo != null ? sourceInfo.keyField() : IntentEntities.keyFieldName(source);

            Map<String, Object> e = new LinkedHashMap<>();
            e.put("name", g.getName());
            e.put("className", IntentNaming.pascalIdentifier(g.getName()));
            e.put("crossModel", crossModel);
            e.put("fromEntity", g.getFrom());
            e.put("fromPerspective", fromPerspective);
            e.put("fromPk", fromPk);
            // The NEGATIVE of "has a button" on purpose: the template gates the @Controller half on
            // !$eventOnly, so a .glue written before this key existed keeps rendering the endpoint it
            // always did (a regeneration from an older glue file must not silently drop the button).
            e.put("eventOnly", !g.hasButton());
            putGeneratesEvent(g, e, fromPk);
            putSupersededTarget(g, e, model, crossModel ? null : byName.get(g.getTo()), context);
            // Cross-model source: the gen folder and the project that owns the source's topics/views.
            e.put("crossModelSource", crossModelSource);
            e.put("fromModel", crossModelSource ? g.getFromUses() : "");
            e.put("fromProject", crossModelSource ? fromUses.resolveProject() : "");
            e.put("toEntity", g.getTo());
            e.put("toModel", crossModel ? g.getUses() : "");
            e.put("toPerspective", toPerspective);
            e.put("toPk", toPk);
            // A hop reads the related row, which the create-from must load first: the resolver renders the
            // null-guarded access and accumulates one load per distinct relation, and `relationLoads`
            // hands those to the template - the same pair a notification's relation.field recipient uses.
            NotificationSupport.Resolver hops = crossModelSource ? null
                    : NotificationSupport.resolver(source, byName, compositionParents, crossModelLookup(model, context));
            List<Map<String, Object>> fieldAssignments = assignments(g.getMap(), g.getDefaults(), "source",
                    temporalKinds(crossModel ? null : byName.get(g.getTo()), target), hops);
            if (fieldAssignments == null) {
                reportDroppedGlue(context, "generates [" + g.getName() + "] map [" + firstHop(g.getMap())
                        + "] is not a resolvable one-hop relation.field of [" + g.getFrom() + "] - the create-from was NOT generated");
                continue;
            }
            List<NotificationSupport.RelationLoad> hopLoads = hops == null ? List.of() : hops.loads();
            String collision = collidingLocal(hopLoads);
            if (collision != null) {
                reportDroppedGlue(context,
                        "generates [" + g.getName() + "] hops through the relation [" + collision + "] of [" + g.getFrom()
                                + "], whose name is one the generated create-from already uses for a local of its own"
                                + " - rename the relation, or map a direct property instead - the create-from was NOT generated");
                continue;
            }
            e.put("fieldAssignments", fieldAssignments);
            e.put("relationLoads", relationLoads(hopLoads));
            // Completion hook: the SOURCE's EntityStatus FK is set to this seed id after the target
            // is created (empty = no hook). Pre-resolved to the PascalCase FK property - locally off
            // the relation, cross-model off the owner .model's DOCUMENT_STATUS widget.
            String sourceStatusProperty = "";
            if (g.getSourceStatus() != null) {
                if (crossModelSource) {
                    // Never guess the property name: the status FK is author-named (Status / Stage /
                    // State), so a wrong guess would emit an updateProperty against a column that does
                    // not exist - a failure visible only at run time, on the button's first click.
                    if (sourceInfo == null || sourceInfo.statusProperty() == null) {
                        throw new org.eclipse.dirigible.components.intent.parser.IntentValidationException(List.of("generates ["
                                + g.getName()
                                + "] declares sourceStatus but the function: EntityStatus relation of its cross-model source ["
                                + g.getFrom() + "] (model [" + g.getFromUses() + "]) could not be read: either the owner entity declares "
                                + "none, or its model was not resolvable here. Generate the [" + g.getFromUses()
                                + "] model first, or publish its module so its .model is in the registry."));
                    }
                    sourceStatusProperty = sourceInfo.statusProperty();
                } else {
                    for (org.eclipse.dirigible.components.intent.model.RelationIntent relation : source.getRelations()) {
                        if (relation.isEntityStatus()) {
                            sourceStatusProperty = IntentNaming.pascalCase(relation.getName());
                        }
                    }
                }
            }
            e.put("sourceStatusProperty", sourceStatusProperty);
            e.put("sourceStatusValue",
                    g.getSourceStatus() == null || sourceStatusProperty.isEmpty() ? "" : String.valueOf(g.getSourceStatus()));
            if (sourceStatusProperty.isEmpty()) {
                // No status FK resolved on the source, so there is no completion hook and nothing for a
                // retired target to return the source to. Emitting the reopen listener anyway would give
                // it a column to write that does not exist - a failure visible only at run time. Cleared
                // whole, so the .glue never records a reopen that cannot be generated.
                e.put("hasReopen", false);
                e.put("reopenStatusValue", "");
                e.put("reopenRetiredCondition", "");
            }

            GeneratesItemsIntent items = g.getItems();
            boolean hasItems = items != null && items.getFrom() != null && !items.getFrom()
                                                                                 .isBlank()
                    && items.getTo() != null && !items.getTo()
                                                      .isBlank();
            List<Map<String, String>> lineRows = g.getItemLines();
            // Computed-line form (issue #6555). Mutually exclusive with the mirror form - the parser
            // rejects declaring both; here the mirror wins defensively if both slipped through.
            boolean hasItemLines = !hasItems && lineRows != null && !lineRows.isEmpty();
            e.put("hasItems", hasItems);
            e.put("hasItemLines", hasItemLines);
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
                // A cross-model source's items are owned by the same foreign model as the source.
                e.put("fromItemPerspective", crossModelSource ? CrossModelSupport.resolve(context, fromUses, items.getFrom())
                                                                                 .perspectiveName()
                        : IntentEntities.resolvePerspective(items.getFrom(), compositionParents, model));
                // A document child's FK back to its master is, by convention, the master entity's name.
                e.put("srcFkProperty", IntentNaming.pascalCase(g.getFrom()));
                e.put("toFkProperty", IntentNaming.pascalCase(g.getTo()));
                // childAssignments (not assignments) so a numeric item default renders as BigDecimal -
                // line-item columns (quantity/price/amount) are decimal, and a bare int literal does
                // not convert to the generated BigDecimal field. The item target lives in the SAME
                // model as the header target, so a cross-model header implies a resolvable item.
                // A default naming a TO-ONE RELATION of the item target (a required classifier the map
                // has no source for, e.g. a SalesInvoiceItem's TaxRate) is a foreign-key id, not an
                // amount - it must stay an integer literal, so the relation names are passed alongside.
                CrossModelSupport.TargetInfo itemTarget = uses == null ? null : CrossModelSupport.resolve(context, uses, items.getTo());
                e.put("itemFieldAssignments",
                        childAssignments(items.getMap(), items.getDefaults(), "srcItem",
                                temporalKinds(crossModel ? null : byName.get(items.getTo()), itemTarget),
                                relationProperties(crossModel ? null : byName.get(items.getTo()), itemTarget)));
                e.put("itemLines", new ArrayList<>());
            } else if (hasItemLines) {
                // The synthetic lines write into the TARGET document's composition line-items child,
                // resolved automatically (never named in the intent): same-model from this model,
                // cross-model from the owner .model. Its perspective == the header target's (a document
                // renders its items there), so toJavaPerspective/toGenFolder/toPk carry over unchanged;
                // only the item ENTITY name and its per-cell field TYPES are new. Cells are expressions
                // over the loaded `source` master (Calc arithmetic / {} string interpolation / FK copy),
                // the postings item-cell conventions applied to a create-from.
                String itemEntityName;
                Map<String, CellMeta> itemMetas;
                if (crossModel) {
                    CrossModelSupport.ItemsChildInfo child = CrossModelSupport.resolveItemsChild(context, uses, g.getTo());
                    if (!child.resolved()) {
                        throw new org.eclipse.dirigible.components.intent.parser.IntentValidationException(
                                List.of("generates [" + g.getName() + "] declares computed item lines but the cross-model target ["
                                        + g.getTo() + "] (model [" + g.getUses() + "]) has no composition line-items child"));
                    }
                    itemEntityName = child.childEntity();
                    itemMetas = crossModelCellMetas(child);
                } else {
                    EntityIntent itemEntity = compositionChild(byName.get(g.getTo()), byName);
                    if (itemEntity == null) {
                        throw new org.eclipse.dirigible.components.intent.parser.IntentValidationException(
                                List.of("generates [" + g.getName() + "] declares computed item lines but the target [" + g.getTo()
                                        + "] has no composition line-items child"));
                    }
                    itemEntityName = itemEntity.getName();
                    itemMetas = localCellMetas(itemEntity);
                }
                e.put("fromItemEntity", "");
                e.put("toItemEntity", itemEntityName);
                e.put("fromItemPerspective", "");
                e.put("srcFkProperty", "");
                e.put("toFkProperty", IntentNaming.pascalCase(g.getTo()));
                e.put("itemFieldAssignments", new ArrayList<>());
                // Cell expressions are written over the SOURCE record, so the known-property set comes
                // from wherever the source is defined - locally, or the owner .model for a cross-model
                // source (an unresolved owner yields an empty set, i.e. no local name check).
                java.util.Set<String> knownSourceProperties = crossModelSource
                        ? (sourceInfo != null && sourceInfo.propertyNames() != null ? sourceInfo.propertyNames() : java.util.Set.of())
                        : sourceProperties(source);
                e.put("itemLines", computedItemLines(lineRows, itemMetas, knownSourceProperties));
            } else {
                e.put("fromItemEntity", "");
                e.put("toItemEntity", "");
                e.put("fromItemPerspective", "");
                e.put("srcFkProperty", "");
                e.put("toFkProperty", "");
                e.put("itemFieldAssignments", new ArrayList<>());
                e.put("itemLines", new ArrayList<>());
            }
            e.put("hasPrompt", g.hasPrompt());
            e.put("promptFields", promptFields(g, crossModel ? null : byName.get(g.getTo())));
            out.add(e);
        }
        return out;
    }

    /**
     * The event half of a create-from (issues #6711, #6800), pre-rendered onto its glue entry: the
     * topic suffix the listener binds to (an {@code onCreate} the source's bare create topic, an
     * {@code onTransition} its {@code -transitioned} topic, a step binding the step-scoped topic the
     * generated emitter publishes to), the optional status guard as a property/value pair evaluated
     * against the RE-LOADED source, the cardinality, and the back-reference.
     *
     * <p>
     * The back-reference is DERIVED from the {@code map} entry that copies the source's primary key
     * rather than declared a second time: the mapping already says which target property points back at
     * the source, and two ways to say it could only drift. A missing one fails loudly here because
     * without it the create-from has no way to recognize its own output (under {@code mode: once} an
     * event redelivery would mint a duplicate document; under {@code mode: append} the appended row
     * would not say what it is about) - the parser catches the local case earlier, with the fix in the
     * message.
     *
     * @param g the create-from
     * @param e the glue entry being built
     * @param fromPk the source's primary-key property name
     */
    private static void putGeneratesEvent(GeneratesIntent g, Map<String, Object> e, String fromPk) {
        e.put("hasEvent", g.isEventDriven());
        if (!g.isEventDriven()) {
            e.put("isCreate", false);
            e.put("isStep", false);
            e.put("topicSuffix", "");
            e.put("appendMode", false);
            e.put("guardProperty", "");
            e.put("guardValue", "");
            e.put("backRefProperty", "");
            return;
        }
        boolean isCreate = g.getEvent()
                            .get("onCreate") != null;
        e.put("isCreate", isCreate);
        StepEventSupport.Binding step = StepEventSupport.binding(g.getEvent());
        e.put("isStep", step != null);
        e.put("stepProcess", step == null ? "" : step.process());
        e.put("stepName", step == null ? "" : step.step());
        e.put("topicSuffix",
                step != null ? StepEventSupport.topicSuffix(step.process(), step.step(), step.kind()) : isCreate ? "" : "-transitioned");
        // The cardinality (#6800): `append` drops the existing-target lookup in the create-from, so
        // every delivery of the event creates a row. It is the absence of a guard, not another guard.
        e.put("appendMode", g.isAppendMode());
        String guardProperty = "";
        String guardValue = "";
        Object whenValue = g.getEvent()
                            .get("when");
        if (whenValue != null) {
            java.util.regex.Matcher when = java.util.regex.Pattern.compile("\\s*(\\w+)\\s*==\\s*(\\d+)\\s*")
                                                                  .matcher(String.valueOf(whenValue));
            if (when.matches()) {
                guardProperty = IntentNaming.pascalCase(when.group(1));
                guardValue = when.group(2);
            }
        }
        e.put("guardProperty", guardProperty);
        e.put("guardValue", guardValue);
        String backReference = "";
        for (Map.Entry<String, String> mapping : g.getMap()
                                                  .entrySet()) {
            if (fromPk.equals(IntentNaming.pascalCase(mapping.getValue()))) {
                backReference = IntentNaming.pascalCase(mapping.getKey());
            }
        }
        if (backReference.isEmpty()) {
            throw new org.eclipse.dirigible.components.intent.parser.IntentValidationException(List.of("generates [" + g.getName()
                    + "] is event-driven but its map copies no source key onto a back-reference: add the target's to-one back to ["
                    + g.getFrom() + "] to the map (the source's [" + fromPk
                    + "] as its value) - it is the at-most-once guard against an event redelivery"));
        }
        e.put("backRefProperty", backReference);
    }

    /**
     * The state half of the at-most-once guard (issue #6814): which of the target's statuses mean the
     * document that already back-references the source no longer counts, so a replacement may be
     * minted.
     *
     * <p>
     * The guard as first shipped asked existence only - "is there a target for this source?" - and a
     * voided or cancelled target answers yes forever: it keeps existing and keeps back-referencing the
     * source, so the source's one-shot slot was consumed at the first creation and nothing that later
     * happened to the target released it. "Void and reissue" - an ordinary business flow - was
     * inexpressible for an event-driven create-from.
     *
     * <p>
     * What a status MEANS is already declared once, where the nomenclature is seeded: the
     * {@code stage:} classification (see {@link LifecycleStages}) the report scope resolves through. A
     * target whose status is classified {@code cancelled} or {@code void} is retired, and the guard
     * steps over it; {@code draft} and {@code live} ones still block, so a redelivered event keeps
     * finding the document it created. Reusing the classification rather than adding a second way to
     * say it is deliberate - two vocabularies for "this row no longer counts" could only drift.
     *
     * <p>
     * Nothing is emitted when the create-from appends (there is no guard to make state-aware), when the
     * target carries no lifecycle at all (there is no state to read), or for a cross-model target,
     * whose seeds live in its owner model so no classification is resolvable here. A target that DOES
     * carry a lifecycle whose nomenclature nobody classified gets the warning - that combination is the
     * silent one, where the guard looks state-aware and is not.
     *
     * <p>
     * The SAME resolution also drives the declared reopen (issue #6868), which reads the classification
     * from the other end: the guard asks whether the target that already exists is retired, the reopen
     * listener asks whether the transition it just saw is what retired it. Emitting both from one
     * resolution is what stops them disagreeing about what "retired" means - and the reason the reopen
     * introduces no vocabulary of its own to say it.
     *
     * @param g the create-from
     * @param e the glue entry being built
     * @param model the model being generated
     * @param target the local target entity, {@code null} for a cross-model target
     * @param context the generation context collecting the warnings, may be {@code null}
     */
    private static void putSupersededTarget(GeneratesIntent g, Map<String, Object> e, IntentModel model, EntityIntent target,
            IntentGenerationContext context) {
        e.put("hasRetiredStatus", false);
        e.put("retiredStatusProperty", "");
        e.put("retiredStatusCondition", "");
        e.put("hasReopen", false);
        e.put("reopenStatusValue", "");
        e.put("reopenRetiredCondition", "");
        // An appending create-from (issue #6800) keeps no guard at all, so there is nothing for a
        // retired target to release - and warning about an unclassified nomenclature there would be
        // noise about a guard that does not exist. A reopen is refused on that shape by the parser, so
        // there is nothing to emit for it here either.
        if (!g.isEventDriven() || g.isAppendMode()) {
            return;
        }
        RelationIntent status = LifecycleStages.statusRelation(target);
        if (status == null || status.getTo() == null) {
            return;
        }
        Map<String, List<Integer>> stages = status.isCrossModel() ? Map.of() : LifecycleStages.stagesOf(model, status.getTo());
        List<Integer> retired = new ArrayList<>(stages.getOrDefault(LifecycleStages.CANCELLED, List.of()));
        retired.addAll(stages.getOrDefault(LifecycleStages.VOID, List.of()));
        if (retired.isEmpty()) {
            String warning = "generates [" + g.getName() + "] is event-driven and its target [" + g.getTo()
                    + "] carries a lifecycle status [" + status.getName() + "], but no seed row of [" + status.getTo()
                    + "] is classified with `stage:` - the at-most-once guard can only ask whether a [" + g.getTo()
                    + "] exists, so a cancelled or voided one blocks its replacement forever. Classify the seed rows of [" + status.getTo()
                    + "] with `stage:` (draft/live/cancelled/void) so a retired target can be superseded.";
            LOGGER.warn(warning);
            if (context != null) {
                context.addIssue(warning);
            }
            return;
        }
        String property = IntentNaming.pascalCase(status.getName());
        e.put("hasRetiredStatus", true);
        e.put("retiredStatusProperty", property);
        // Rendered against the template's loop variable: a retired candidate is stepped over, the first
        // one that is not is this source's document.
        e.put("retiredStatusCondition", retiredCondition("candidate", property, retired));
        // The declared reopen (issue #6868) reads the SAME classification from the other end: the guard
        // asks "is the document that exists retired?", the reopen listener asks "did this transition
        // retire it?". One resolution, so the two can never disagree about what retired means - which
        // is the whole reason the reopen adds no vocabulary of its own for it.
        if (!g.hasReopen()) {
            return;
        }
        e.put("hasReopen", true);
        e.put("reopenStatusValue", String.valueOf(g.getSourceStatusOnRetire()));
        e.put("reopenRetiredCondition", retiredCondition("target", property, retired));
    }

    /**
     * The retiring-status test as a Java disjunction over a named local - {@code cancelled} and
     * {@code void} ids in seed order.
     *
     * @param local the Java local the status is read off
     * @param property the status FK property
     * @param retired the retiring seed ids
     * @return the rendered condition
     */
    private static String retiredCondition(String local, String property, List<Integer> retired) {
        StringBuilder condition = new StringBuilder();
        for (Integer id : retired) {
            if (condition.length() > 0) {
                condition.append(" || ");
            }
            condition.append(local)
                     .append('.')
                     .append(property)
                     .append(" == ")
                     .append(id);
        }
        return condition.toString();
    }

    /**
     * The {@code prompt:} inputs of a generates action (issue #6685), pre-rendered for the template
     * (the expansions convention - the template stays shape-only): per prompted TARGET property its
     * PascalCase name, the required flag, and the Java expression converting the posted JSON value (an
     * {@code Object raw} local - Gson delivers numbers as Double, everything else as String/Boolean) to
     * the generated entity field's Java type. A to-one relation is its integer FK. The parser has
     * already constrained prompts to a local target and rejected unsupported field types.
     */
    private static List<Map<String, Object>> promptFields(GeneratesIntent g, EntityIntent target) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (!g.hasPrompt() || target == null) {
            return out;
        }
        for (org.eclipse.dirigible.components.intent.model.PromptFieldIntent p : g.getPrompt()) {
            String field = p.getField();
            if (field == null || field.isBlank()) {
                continue; // parser already reported it
            }
            FieldIntent targetField = fieldNamed(target, field);
            if (targetField == null && toOneRelation(target, field) == null) {
                continue; // parser already reported it
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("prop", IntentNaming.pascalCase(field));
            entry.put("required", p.isRequired());
            entry.put("expr", promptConversion(targetField == null ? "relation" : targetField.getType()));
            out.add(entry);
        }
        return out;
    }

    /** The Java expression converting the posted {@code Object raw} to the target field's type. */
    private static String promptConversion(String type) {
        String normalized = type == null ? "string" : type;
        return switch (normalized) {
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
     * Transitions: one entry per {@code transitions} declaration - the guarded on-demand status flip.
     * EVERYTHING is pre-rendered here so the Velocity template contains no expression logic: the
     * allowed-statuses check is a Java boolean expression over an {@code int currentStatus} local, and
     * the optional {@code when} guard is a full SDK {@code Calc} comparison over the loaded
     * {@code source} entity (Calc semantics: a null field reads as 0 - identical to calculated fields).
     */
    private static List<Map<String, Object>> buildTransitions(IntentModel model, Map<String, EntityIntent> byName,
            Map<String, String> compositionParents, IntentSettings settings, IntentGenerationContext context) {
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
            e.put("perspective", IntentEntities.resolvePerspective(t.getForEntity(), compositionParents, model));
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
            // Optional outbound mail after the flip commits ("on Void, mail the counterparty"),
            // resolved against the transitioned record - the same notify block a schedule or a
            // process step authors. Fail-soft in the generated controller: the flip is the contract.
            e.putAll(notifyFields(t.getNotify(), entity, model, byName, compositionParents, context,
                    "Transition [" + t.getName() + "] notify"));
            out.add(e);
        }
        return out;
    }

    /**
     * The {@code notify*} keys of a descriptor whose call site may carry an embedded notify block (a
     * transition, a process step): the pre-rendered recipient / subject / body expressions, the
     * relation loads they need, and the attachment coordinates - or {@code notify: false} and empty
     * strings when no block was authored (or its recipient does not resolve, which is reported and
     * dropped rather than mailing garbage).
     */
    private static Map<String, Object> notifyFields(NotificationIntent notify, EntityIntent entity, IntentModel model,
            Map<String, EntityIntent> byName, Map<String, String> compositionParents, IntentGenerationContext context, String subject) {
        Map<String, Object> fields = new LinkedHashMap<>();
        // A fan-out sends one message PER ROW of a related entity, so every path - recipient,
        // placeholders, attached document - resolves against the ROW rather than the record the block
        // hangs on. An unresolvable forEach drops the whole block: mailing the record once instead of
        // each row would be a different message, quietly sent to the wrong party.
        NotifySupport.FanOut fanOut = NotifySupport.fanOut(notify, entity, byName, compositionParents);
        boolean fanOutRequested = notify != null && notify.getForEach() != null && !notify.getForEach()
                                                                                          .isBlank();
        if (fanOutRequested && fanOut == null) {
            reportDroppedGlue(context, subject + " forEach [" + notify.getForEach() + "] is not a declared entity with exactly one to-one"
                    + " relation back to [" + (entity == null ? "?" : entity.getName()) + "] - the mail was NOT generated");
        }
        EntityIntent about = fanOut == null ? entity : byName.get(fanOut.entity());
        // The record the rows hang off stays addressable inside a fan-out, but only through the explicit
        // `record.` placeholder scope - so which of the two entities a path reads is always authored.
        EntityIntent anchor = fanOut == null ? null : entity;
        boolean dropped = fanOutRequested && fanOut == null;
        NotificationSupport.Plan plan = notify == null || dropped ? null
                : NotificationSupport.plan(notify.getTo(), notify.getSubject(), notify.getBody(), null, about, anchor, byName,
                        compositionParents, crossModelLookup(model, context));
        if (notify != null && plan == null && !dropped) {
            reportDroppedGlue(context, subject + " recipient [" + notify.getTo() + "] is not a resolvable field or relation.field of ["
                    + (about == null ? "?" : about.getName()) + "] - the mail was NOT generated");
        }
        // `attach: recordPrint` attaches the ANCHOR record's document instead of the row's - one
        // document, many recipients - so it is resolved (and later rendered) against the record.
        EntityIntent document = NotifySupport.attachesRecordPrint(notify) && fanOut != null ? entity : about;
        NotifySupport.PrintAttachment attachment =
                plan == null ? null : printAttachment(notify, document, model, byName, compositionParents, context, subject);
        boolean send = plan != null && (attachment != null || !NotifySupport.attachesPrint(notify));
        fields.put("notify", String.valueOf(send));
        fields.put("notifyRelationLoads", send ? relationLoads(plan, attachment) : new ArrayList<>());
        fields.put("notifyToExpression", send ? plan.toExpression() : "null");
        fields.put("notifySubjectExpression", send ? plan.subjectExpression() : "\"\"");
        fields.put("notifyBodyExpression", send ? plan.bodyExpression() : "\"\"");
        // Whether a per-row message quotes the anchor record - the fan-out templates then hand the
        // loaded record to their send method, and only then (an argument nothing reads is noise).
        fields.put("notifyRecordScoped", String.valueOf(send && fanOut != null && NotifySupport.usesRecordScope(notify)));
        fields.putAll(NotifySupport.fanOutFields(send ? fanOut : null));
        fields.putAll(NotifySupport.attachmentFields(send ? attachment : null));
        // The key the print feeder is fed with: the ROW's for `attach: print` (the loop variable is
        // named `entity` in the templates for exactly this reason, so one expression set serves both
        // shapes), the ANCHOR record's for `attach: recordPrint`.
        fields.put("attachKeyProperty", send && attachment != null ? IntentEntities.keyFieldName(document) : "");
        fields.putAll(NotifySupport.deepLinkFields(send ? plan : null, about));
        return fields;
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

    /** Test hook: build the {@code triggers} glue collection without a repository. */
    static List<Map<String, Object>> buildTriggersForTest(IntentModel model) {
        IntentGenerationContext context =
                new IntentGenerationContext(model, "/" + model.getName(), model.getName(), "workspace", model.getName(), null);
        return buildTriggers(model, IntentEntities.byName(model), IntentEntities.compositionParents(model), IntentSettings.parse("{}"),
                context);
    }

    /** Test hook: build the {@code aborts} glue collection without a repository. */
    static List<Map<String, Object>> buildAbortsForTest(IntentModel model) {
        return buildAborts(model, IntentSettings.parse("{}"));
    }

    /** Test hook: build the {@code setters} glue collection without a repository. */
    static List<Map<String, Object>> buildSettersForTest(IntentModel model) {
        return buildSetters(model, IntentSettings.parse("{}"));
    }

    /** Test hook: build the {@code expansions} glue collection without a repository. */
    static List<Map<String, Object>> buildExpansionsForTest(IntentModel model) {
        return buildExpansions(model, IntentEntities.byName(model), IntentEntities.compositionParents(model),
                IntentSettings.parse("{}")).reconciliations();
    }

    /** Test hook: build the {@code rollups} glue collection without a repository. */
    static List<Map<String, Object>> buildRollupsForTest(IntentModel model) {
        return buildRollupsForTest(model, null);
    }

    /**
     * Test hook: build the {@code rollups} glue collection against a context, so a cross-model child
     * can be resolved against a REAL owner model rather than the naming-convention fallback.
     *
     * @param model the parsed model
     * @param context the generation context (may be null)
     * @return the glue entries
     */
    static List<Map<String, Object>> buildRollupsForTest(IntentModel model, IntentGenerationContext context) {
        return buildRollups(model, IntentEntities.byName(model), IntentEntities.compositionParents(model), IntentSettings.parse("{}"),
                context);
    }

    /** Test hook: build the {@code settlementListeners} glue collection without a repository. */
    static List<Map<String, Object>> buildSettlementListenersForTest(IntentModel model) {
        IntentGenerationContext context =
                new IntentGenerationContext(model, "/" + model.getName(), model.getName(), "workspace", model.getName(), null);
        return buildSettlementListeners(buildSettlements(model, IntentEntities.byName(model), IntentEntities.compositionParents(model),
                IntentSettings.parse("{}"), context));
    }

    /** Test hook: build the {@code waits} glue collection without a repository. */
    static List<Map<String, Object>> buildWaitsForTest(IntentModel model) {
        return buildWaits(model, IntentSettings.parse("{}"));
    }

    /** Test hook: build the {@code assignees} glue collection without a repository. */
    static List<Map<String, Object>> buildAssigneesForTest(IntentModel model) {
        return buildAssignees(model, IntentSettings.parse("{}"), null);
    }

    /** Test hook: build the {@code timerLoaders} glue collection without a repository. */
    static List<Map<String, Object>> buildTimerLoadersForTest(IntentModel model) {
        return buildTimerLoaders(model, IntentSettings.parse("{}"));
    }

    /** Test hook: build the {@code transitions} glue collection without a repository. */
    static List<Map<String, Object>> buildTransitionsForTest(IntentModel model) {
        return buildTransitions(model, IntentEntities.byName(model), IntentEntities.compositionParents(model), IntentSettings.parse("{}"),
                null);
    }

    /**
     * Sends: one entry per {@code serviceTask} that declares a {@code notify} block - the step whose
     * work IS to send a message about the process's trigger record, optionally with the record's
     * rendered document attached ({@code attach: print}). Everything is pre-rendered here (the
     * recipient / subject / body expressions, the relation loads, the attachment coordinates); the BPMN
     * generator binds the task to the generated {@code <Process><Step>Send} delegate this drives.
     */
    private static List<Map<String, Object>> buildSends(IntentModel model, Map<String, EntityIntent> byName,
            Map<String, String> compositionParents, IntentSettings settings, IntentGenerationContext context) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (NotifySupport.Sender sender : NotifySupport.senders(model)) {
            if (!settings.shouldGenerate("sends", sender.className())) {
                LOGGER.info("Settings opt-out: keeping existing delegate for send [{}] (not generated)", sender.className());
                continue;
            }
            EntityIntent entity = byName.get(sender.entity());
            Map<String, Object> fields = notifyFields(sender.block(), entity, model, byName, compositionParents, context,
                    "Process [" + sender.process() + "] step [" + sender.step() + "] notify");
            if (!"true".equals(fields.get("notify"))) {
                continue; // unresolvable recipient / unrenderable attachment - reported by notifyFields
            }
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("process", sender.process());
            e.put("step", sender.step());
            e.put("className", sender.className());
            e.put("entity", sender.entity());
            e.put("perspective", IntentEntities.resolvePerspective(sender.entity(), compositionParents, model));
            e.put("keyProperty", sender.keyProperty());
            e.put("keyAccessor", sender.keyAccessor());
            e.putAll(fields);
            out.add(e);
        }
        return out;
    }

    /** Test hook: build the {@code sends} glue collection without a repository. */
    static List<Map<String, Object>> buildSendsForTest(IntentModel model) {
        return buildSends(model, IntentEntities.byName(model), IntentEntities.compositionParents(model), IntentSettings.parse("{}"), null);
    }

    /** Test hook: build the {@code notifications} glue collection without a repository. */
    static List<Map<String, Object>> buildNotificationsForTest(IntentModel model) {
        return buildNotifications(model, IntentEntities.byName(model), IntentEntities.compositionParents(model), IntentSettings.parse("{}"),
                null);
    }

    /** Test hook: build the {@code integrations} glue collection without a repository. */
    static List<Map<String, Object>> buildIntegrationsForTest(IntentModel model) {
        return buildIntegrations(model, IntentEntities.byName(model), IntentEntities.compositionParents(model), IntentSettings.parse("{}"),
                null);
    }

    /** Test hook: build the {@code stepEvents} glue collection without a repository. */
    static List<Map<String, Object>> buildStepEventsForTest(IntentModel model) {
        return buildStepEvents(model, IntentEntities.compositionParents(model), IntentSettings.parse("{}"));
    }

    /** Test hook: build the {@code inboundMessages} glue collection without a repository. */
    static List<Map<String, Object>> buildInboundMessagesForTest(IntentModel model) {
        return buildInboundMessages(model, IntentEntities.byName(model), IntentEntities.compositionParents(model),
                IntentSettings.parse("{}"), null);
    }

    /** Test hook: build the {@code inboundFiles} glue collection without a repository. */
    static List<Map<String, Object>> buildInboundFilesForTest(IntentModel model) {
        return buildInboundFiles(model, IntentEntities.byName(model), IntentEntities.compositionParents(model), IntentSettings.parse("{}"),
                null);
    }

    /** Test hook: build the {@code outbound} glue collection without a repository. */
    static List<Map<String, Object>> buildOutboundForTest(IntentModel model) {
        return buildOutbound(model, IntentEntities.byName(model), IntentEntities.compositionParents(model), IntentSettings.parse("{}"),
                null);
    }

    /** Test hook: build the {@code inbound} (HTTP webhook) glue collection without a repository. */
    static List<Map<String, Object>> buildInboundForTest(IntentModel model) {
        return buildInbound(model, IntentEntities.byName(model), IntentEntities.compositionParents(model), IntentSettings.parse("{}"),
                null);
    }

    /**
     * Build the {@code posts} glue collection: one descriptor per {@code posts:} rule
     * ({@link org.eclipse.dirigible.components.intent.model.PostIntent}). Each descriptor drives a
     * generated event handler that, on the source document's {@code event:} event, emits mapped rows
     * into the {@code into:} target (per {@code forEach:} line item), idempotently by
     * {@code idempotentBy}. This is the structural glue; the handler template + BPMN/listener wiring is
     * the next stage.
     */
    private static List<Map<String, Object>> buildPosts(IntentModel model, Map<String, EntityIntent> byName,
            Map<String, String> compositionParents) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (org.eclipse.dirigible.components.intent.model.PostIntent p : model.getPosts()) {
            if (p.getName() == null || p.getName()
                                        .isBlank()
                    || p.getForEntity() == null || p.getInto() == null || p.getEvent() == null) {
                continue; // malformed: name/forEntity/into/event are required
            }
            EntityIntent source = byName.get(p.getForEntity());
            EntityIntent target = byName.get(p.getInto());
            if (source == null || target == null) {
                continue; // bad source/target reference (v1: same-model target)
            }
            // The trigger: `create` (fires on insert, no status guard) or a numeric status seed id
            // (fires on the -transitioned topic, guarded to that status - matches transitions/postings).
            boolean isCreate = "create".equalsIgnoreCase(p.getEvent()
                                                          .trim());
            Integer statusValue = null;
            String statusProperty = "";
            if (!isCreate) {
                try {
                    statusValue = Integer.valueOf(p.getEvent()
                                                   .trim());
                } catch (NumberFormatException nfe) {
                    LOGGER.warn("posts [{}]: event must be `create` or a numeric status seed id, was [{}] - skipped", p.getName(),
                            p.getEvent());
                    continue;
                }
                for (RelationIntent relation : source.getRelations()) {
                    if (relation.isEntityStatus()) {
                        statusProperty = IntentNaming.pascalCase(relation.getName());
                    }
                }
                if (statusProperty.isEmpty()) {
                    LOGGER.warn("posts [{}]: source [{}] has no function: EntityStatus relation for a status-triggered post - skipped",
                            p.getName(), p.getForEntity());
                    continue;
                }
            }
            // Per-item collection: forEach -> the composition child of the source (the entity whose
            // composition relation targets it). Absent -> one row from the source itself.
            String itemsEntity = "";
            String itemsFk = "";
            String itemsPerspective = "";
            boolean perItem = p.getForEach() != null && !p.getForEach()
                                                          .isBlank();
            if (perItem) {
                EntityIntent child = null;
                String fk = null;
                for (EntityIntent candidate : model.getEntities()) {
                    if (p.getForEntity()
                         .equals(compositionParents.get(candidate.getName()))) {
                        for (RelationIntent relation : candidate.getRelations()) {
                            if (relation.isComposition() && p.getForEntity()
                                                             .equals(relation.getTo())) {
                                child = candidate;
                                fk = IntentNaming.pascalCase(relation.getName());
                            }
                        }
                    }
                }
                if (child == null) {
                    LOGGER.warn("posts [{}]: forEach set but [{}] has no composition child - skipped", p.getName(), p.getForEntity());
                    continue;
                }
                itemsEntity = child.getName();
                itemsFk = fk;
                itemsPerspective = IntentEntities.resolvePerspective(child.getName(), compositionParents, model);
            }

            Map<String, Object> e = new LinkedHashMap<>();
            e.put("name", p.getName());
            e.put("className", IntentNaming.pascalIdentifier(p.getName()));
            e.put("entity", p.getForEntity());
            e.put("sourcePerspective", IntentEntities.resolvePerspective(p.getForEntity(), compositionParents, model));
            e.put("sourceKeyField", IntentEntities.keyFieldName(source));
            e.put("isCreate", isCreate);
            e.put("event", p.getEvent());
            e.put("statusProperty", statusProperty);
            e.put("statusValue", statusValue == null ? "" : String.valueOf(statusValue));
            e.put("perItem", perItem);
            e.put("itemsEntity", itemsEntity);
            e.put("itemsFk", itemsFk);
            e.put("itemsPerspective", itemsPerspective);
            e.put("into", p.getInto());
            e.put("targetPerspective", IntentEntities.resolvePerspective(p.getInto(), compositionParents, model));
            e.put("targetPk", IntentEntities.keyFieldName(target));
            e.put("backRef", p.getIdempotentBy() == null ? "" : IntentNaming.pascalCase(p.getIdempotentBy()));
            e.put("guard", p.getGuard() == null ? "" : p.getGuard());
            // Rendered per-row assignments: target field -> a Java expression over `source` / `item`.
            List<Map<String, String>> assigns = new ArrayList<>();
            for (Map.Entry<String, String> f : p.getSet()
                                                .entrySet()) {
                Map<String, String> pair = new LinkedHashMap<>();
                pair.put("field", IntentNaming.pascalCase(f.getKey()));
                pair.put("expr", postSetExpr(f.getValue()));
                assigns.add(pair);
            }
            e.put("assigns", assigns);
            out.add(e);
        }
        return out;
    }

    /**
     * Render a {@code posts:} {@code set:} value to a Java expression over the {@code source} entity
     * and the per-item {@code item} entity. Supported forms (the inventory ledger's needs):
     * {@code item.<Field>} (item copy), {@code -item.<Field>} (negated item copy, null-safe),
     * {@code source.<Field>} (source copy), an integer literal (a constant FK/int), a quoted string,
     * else pass-through (best effort). Fuller {@code Calc} expressions are a follow-up.
     */
    private static String postSetExpr(String raw) {
        if (raw == null) {
            return "null";
        }
        String v = raw.trim();
        java.util.regex.Matcher neg = java.util.regex.Pattern.compile("^-\\s*item\\.(\\w+)$")
                                                             .matcher(v);
        if (neg.matches()) {
            String f = "item." + IntentNaming.pascalCase(neg.group(1));
            return f + " == null ? null : " + f + ".negate()";
        }
        java.util.regex.Matcher item = java.util.regex.Pattern.compile("^item\\.(\\w+)$")
                                                              .matcher(v);
        if (item.matches()) {
            return "item." + IntentNaming.pascalCase(item.group(1));
        }
        java.util.regex.Matcher src = java.util.regex.Pattern.compile("^source\\.(\\w+)$")
                                                             .matcher(v);
        if (src.matches()) {
            return "source." + IntentNaming.pascalCase(src.group(1));
        }
        if (v.matches("-?\\d+")) {
            return v; // integer constant (e.g. a Direction FK id)
        }
        if (v.matches("\"[^\"]*\"")) {
            return v; // already-quoted string literal
        }
        return v; // pass-through (best effort); fuller Calc rendering is a follow-up
    }

    /** Test hook: build the {@code posts} glue collection without a repository. */
    static List<Map<String, Object>> buildPostsForTest(IntentModel model) {
        return buildPosts(model, IntentEntities.byName(model), IntentEntities.compositionParents(model));
    }

    /**
     * Build the {@code aggregates} glue collection: one descriptor per {@code aggregates:} rule
     * ({@link org.eclipse.dirigible.components.intent.model.AggregateIntent}). Each drives a generated
     * handler that maintains a running sum/count of a source entity's field, grouped by its to-one
     * relations, upserted into a separate target entity keyed by that group. Structural glue; the
     * keyed-upsert handler template is the next stage.
     */
    private static List<Map<String, Object>> buildAggregates(IntentModel model, Map<String, EntityIntent> byName,
            Map<String, String> compositionParents) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (org.eclipse.dirigible.components.intent.model.AggregateIntent a : model.getAggregates()) {
            if (a.getName() == null || a.getName()
                                        .isBlank()
                    || a.getOf() == null || a.getInto() == null || a.getField() == null || a.getBy()
                                                                                            .isEmpty()) {
                continue; // malformed: name/of/into/field/by are required
            }
            EntityIntent source = byName.get(a.getOf());
            EntityIntent target = byName.get(a.getInto());
            if (source == null || target == null) {
                continue; // bad source/target reference (v1: same-model)
            }
            String op = a.getOp() == null || a.getOp()
                                              .isBlank() ? "sum"
                                                      : a.getOp()
                                                         .trim()
                                                         .toLowerCase(java.util.Locale.ROOT);
            // The grouping keys must be to-one relations of BOTH the source and the target (the target
            // is keyed by the same FKs). Emit the pascal FK names paired for the key match.
            List<Map<String, String>> keys = new ArrayList<>();
            boolean keysOk = true;
            for (String key : a.getBy()) {
                String fk = IntentNaming.pascalCase(key);
                boolean onSource = source.getRelations()
                                         .stream()
                                         .anyMatch(r -> fk.equals(IntentNaming.pascalCase(r.getName())));
                boolean onTarget = target.getRelations()
                                         .stream()
                                         .anyMatch(r -> fk.equals(IntentNaming.pascalCase(r.getName())));
                if (!onSource || !onTarget) {
                    LOGGER.warn("aggregate [{}]: key [{}] must be a to-one relation of both source [{}] and target [{}] - skipped",
                            a.getName(), key, a.getOf(), a.getInto());
                    keysOk = false;
                    break;
                }
                Map<String, String> pair = new LinkedHashMap<>();
                pair.put("key", fk);
                keys.add(pair);
            }
            if (!keysOk) {
                continue;
            }
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("name", a.getName());
            e.put("className", IntentNaming.pascalIdentifier(a.getName()));
            e.put("op", op);
            e.put("sourceEntity", a.getOf());
            e.put("sourcePerspective", IntentEntities.resolvePerspective(a.getOf(), compositionParents, model));
            e.put("sourceKeyField", IntentEntities.keyFieldName(source));
            e.put("sumField", a.getSum() == null ? "" : IntentNaming.pascalCase(a.getSum()));
            e.put("keys", keys);
            e.put("targetEntity", a.getInto());
            e.put("targetPerspective", IntentEntities.resolvePerspective(a.getInto(), compositionParents, model));
            e.put("targetPk", IntentEntities.keyFieldName(target));
            e.put("targetField", IntentNaming.pascalCase(a.getField()));
            out.add(e);
        }
        return out;
    }

    /** Test hook: build the {@code aggregates} glue collection without a repository. */
    static List<Map<String, Object>> buildAggregatesForTest(IntentModel model) {
        return buildAggregates(model, IntentEntities.byName(model), IntentEntities.compositionParents(model));
    }

    /**
     * Build the {@code resolves} glue collection: one descriptor per effective-dated register lookup
     * ({@link org.eclipse.dirigible.components.intent.model.ResolveIntent}). Each drives a generated
     * handler that, on the record's create/update event, queries the register by the {@code match} keys
     * and keeps the rows whose validity period covers the record's date - then fills the to-one from
     * the single covering row, or leaves it unset and flags the record when there is none or more than
     * one.
     */
    private static List<Map<String, Object>> buildResolves(IntentModel model, Map<String, EntityIntent> byName,
            Map<String, String> compositionParents, IntentSettings settings) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (org.eclipse.dirigible.components.intent.model.ResolveIntent resolve : model.getResolves()) {
            if (resolve.getName() == null || resolve.getName()
                                                    .isBlank()) {
                continue; // malformed: the parser already reported it
            }
            if (!settings.shouldGenerate("resolves", resolve.getName())) {
                LOGGER.info("Settings opt-out: keeping existing handler for resolve [{}] (not generated)", resolve.getName());
                continue;
            }
            String kind = EventBinding.kind(resolve.getEvent());
            EntityIntent record = byName.get(EventBinding.entity(resolve.getEvent()));
            EntityIntent register = byName.get(resolve.getFrom());
            if (kind == null || record == null || register == null) {
                continue;
            }
            RelationIntent filled = toOneRelation(record, resolve.getSet());
            RelationIntent value = filled == null ? null : soleToOneTo(register, filled.getTo());
            if (filled == null || value == null) {
                continue;
            }
            List<Map<String, String>> matches = new ArrayList<>();
            for (Map.Entry<String, String> pair : resolve.getMatch()
                                                         .entrySet()) {
                Map<String, String> match = new LinkedHashMap<>();
                match.put("registerProperty", IntentNaming.pascalCase(pair.getKey()));
                match.put("recordProperty", IntentNaming.pascalCase(pair.getValue()));
                matches.add(match);
            }
            if (matches.isEmpty()) {
                continue;
            }
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("name", resolve.getName());
            e.put("className", IntentNaming.pascalIdentifier(resolve.getName()));
            e.put("entity", record.getName());
            e.put("perspective", IntentEntities.resolvePerspective(record.getName(), compositionParents, model));
            e.put("keyProperty", IntentEntities.keyFieldName(record));
            e.put("topicSuffix", EventBinding.topicSuffix(kind));
            e.put("guardExpression", NotificationSupport.guard(stringArg(resolve.getEvent(), "when")));
            e.put("setProperty", IntentNaming.pascalCase(filled.getName()));
            e.put("registerEntity", register.getName());
            e.put("registerPerspective", IntentEntities.resolvePerspective(register.getName(), compositionParents, model));
            e.put("registerValueProperty", IntentNaming.pascalCase(value.getName()));
            e.put("matches", matches);
            // The static register narrowing, pre-rendered as Java literals: the template only chains
            // them onto the Criteria, so nothing about a value's type has to be decided in Velocity.
            List<Map<String, String>> filters = new ArrayList<>();
            for (Map.Entry<String, Object> pair : resolve.getWhere()
                                                         .entrySet()) {
                Map<String, String> filter = new LinkedHashMap<>();
                filter.put("property", IntentNaming.pascalCase(pair.getKey()));
                filter.put("literal", javaLiteral(pair.getValue()));
                filters.add(filter);
            }
            e.put("filters", filters);
            e.put("filterSummary", filters.stream()
                                          .map(filter -> filter.get("property") + " = " + filter.get("literal"))
                                          .collect(java.util.stream.Collectors.joining(", ")));
            e.put("matchSummary", matches.stream()
                                         .map(match -> match.get("registerProperty") + " = " + match.get("recordProperty"))
                                         .collect(java.util.stream.Collectors.joining(", ")));
            e.put("startProperty", property(resolve.getBetween()
                                                   .get("start")));
            e.put("endProperty", property(resolve.getBetween()
                                                 .get("end")));
            e.put("valueProperty", property(resolve.getBetween()
                                                   .get("value")));
            e.put("outcomeProperty", property(resolve.getOutcome()));
            String statusProperty = entityStatusProperty(record);
            String foundStatus = status(resolve.getFound());
            String notFoundStatus = status(resolve.getNotFound());
            String ambiguousStatus = status(resolve.getAmbiguous());
            e.put("statusProperty", statusProperty);
            e.put("foundStatus", foundStatus);
            e.put("notFoundStatus", notFoundStatus);
            e.put("ambiguousStatus", ambiguousStatus);
            // Whether the handler routes by status at all - a lookup that never sets one gets no status
            // parameter and no status branch, rather than dead code carried through every generated app.
            e.put("writesStatus", String.valueOf(
                    !statusProperty.isEmpty() && !(foundStatus.isEmpty() && notFoundStatus.isEmpty() && ambiguousStatus.isEmpty())));
            out.add(e);
        }
        return out;
    }

    /**
     * The entity's ONLY to-one relation pointing at {@code target}, or {@code null} when there is none
     * or more than one - a lookup with a choice of columns to copy is refused, not guessed.
     */
    private static RelationIntent soleToOneTo(EntityIntent entity, String target) {
        RelationIntent found = null;
        for (RelationIntent relation : entity.getRelations() == null ? List.<RelationIntent>of() : entity.getRelations()) {
            if (!target.equals(relation.getTo()) || !("manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind()))) {
                continue;
            }
            if (found != null) {
                return null;
            }
            found = relation;
        }
        return found;
    }

    /** The {@code function: EntityStatus} relation's property of the entity, or {@code ""}. */
    private static String entityStatusProperty(EntityIntent entity) {
        for (RelationIntent relation : entity.getRelations() == null ? List.<RelationIntent>of() : entity.getRelations()) {
            if (relation.isEntityStatus()) {
                return IntentNaming.pascalCase(relation.getName());
            }
        }
        return "";
    }

    /** An authored property name as the generated Java field, or {@code ""} when absent. */
    private static String property(String authored) {
        return authored == null || authored.isBlank() ? "" : IntentNaming.pascalCase(authored);
    }

    /** An outcome block's {@code setStatus} seed id as a string, or {@code ""} when it sets none. */
    private static String status(Map<String, Object> outcome) {
        Object value = outcome == null ? null : outcome.get("setStatus");
        return value instanceof Number number ? String.valueOf(number.intValue()) : "";
    }

    /** A string argument of a free-form binding map, or {@code null}. */
    private static String stringArg(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        return value == null ? null : value.toString();
    }

    /**
     * The guard keys of an event binding: the Java condition, plus whether there is one at all - a
     * template that only needs the record in order to evaluate a guard must not parse it otherwise (a
     * departure with no payload forwards the message it received verbatim).
     *
     * @param event the {@code event:} binding map
     * @return the {@code guardExpression} / {@code hasGuard} keys
     */
    private static Map<String, Object> guardFields(Map<String, Object> event) {
        String guard = NotificationSupport.guard(stringArg(event, "when"));
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("guardExpression", guard);
        fields.put("hasGuard", !"true".equals(guard));
        return fields;
    }

    /** Test hook: build the {@code resolves} glue collection without a repository. */
    static List<Map<String, Object>> buildResolvesForTest(IntentModel model) {
        return buildResolves(model, IntentEntities.byName(model), IntentEntities.compositionParents(model), IntentSettings.parse("{}"));
    }

    /**
     * Test hook: build the {@code generates} glue collection without a repository. With a null context
     * a cross-model target falls back to {@link CrossModelSupport}'s naming-convention defaults
     * (perspective = entity name, key = {@code Id}), which is deterministic and enough to assert the
     * mapping shape.
     */
    static List<Map<String, Object>> buildGeneratesForTest(IntentModel model) {
        return buildGeneratesForTest(model, null);
    }

    /**
     * Test hook: build the {@code generates} glue collection against a context, so the warnings a
     * create-from collects (an event-driven one whose target lifecycle nobody classified) are readable.
     */
    static List<Map<String, Object>> buildGeneratesForTest(IntentModel model, IntentGenerationContext context) {
        return buildGenerates(model, IntentEntities.byName(model), IntentEntities.compositionParents(model), IntentSettings.parse("{}"),
                context);
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
            // The trigger: `onTransition` binds the -transitioned topic (status guard mandatory);
            // `onCreate` binds the source's CREATE topic (the bare entity topic - the platform
            // publishes creates unsuffixed) - the source with no status lifecycle (a booked
            // payment) whose only event is its insert. The guard stays optional for onCreate.
            Object onCreateSource = posting.getEvent()
                                           .get("onCreate");
            boolean isCreate = onCreateSource != null;
            String sourceEntity = String.valueOf(isCreate ? onCreateSource
                    : posting.getEvent()
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
                sourcePerspective = IntentEntities.resolvePerspective(sourceEntity, compositionParents, model);
                EntityIntent local = byName.get(sourceEntity);
                if (local != null) {
                    sourceKeyField = IntentEntities.keyFieldName(local);
                }
            }
            // The guard: "<Property> == <seed id>", evaluated against the RE-LOADED source.
            // Mandatory for onTransition (the status guard); optional for onCreate.
            String guardProperty = "";
            String guardValue = "";
            Object whenValue = posting.getEvent()
                                      .get("when");
            if (whenValue != null) {
                java.util.regex.Matcher when = java.util.regex.Pattern.compile("\\s*(\\w+)\\s*==\\s*(\\d+)\\s*")
                                                                      .matcher(String.valueOf(whenValue));
                if (!when.matches()) {
                    continue; // parser already reported it
                }
                guardProperty = IntentNaming.pascalCase(when.group(1));
                guardValue = when.group(2);
            } else if (!isCreate) {
                continue; // parser already reported it (onTransition requires the status guard)
            }
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("name", posting.getName());
            e.put("className", IntentNaming.pascalIdentifier(posting.getName()));
            e.put("isCreate", isCreate);
            e.put("crossModel", alias != null);
            e.put("sourceProject", sourceProject);
            e.put("sourceGenFolder", sourceGenFolder);
            e.put("sourcePerspective", sourcePerspective);
            e.put("sourceEntity", sourceEntity);
            e.put("sourceKeyField", sourceKeyField);
            e.put("guardProperty", guardProperty);
            e.put("guardValue", guardValue);
            e.put("targetEntity", creates.getName());
            e.put("targetPerspective", IntentEntities.resolvePerspective(creates.getName(), compositionParents, model));
            e.put("targetPk", IntentEntities.keyFieldName(creates));
            e.put("itemsEntity", itemsEntity.getName());
            e.put("itemsPerspective", IntentEntities.resolvePerspective(itemsEntity.getName(), compositionParents, model));
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
            // Conditional rule(by:...) cells (#6534): the selected column is a RUNTIME choice, so it
            // cannot join the static usedRuleColumns null-skip (that would skip whenever ANY case column
            // is null). Each collected expression is instead null-checked as a whole after the rule row
            // is resolved - an unmatched/undetermined account skips the posting fail-soft.
            List<String> conditionalRuleGuards = new ArrayList<>();
            if (hasRule) {
                String ruleEntityName = String.valueOf(effective.getRule()
                                                                .get("entity"));
                e.put("ruleEntity", ruleEntityName);
                // A setting rule entity (the normal case) lives under the global Settings perspective.
                e.put("rulePerspective", IntentEntities.resolvePerspective(ruleEntityName, compositionParents, model));
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
                    Map<String, Object> assign = new LinkedHashMap<>();
                    assign.put("targetProp", IntentNaming.pascalCase(cell.getKey()));
                    java.util.Optional<PostingRuleSelector> ruleSelector = PostingRuleSelector.parse(value);
                    java.util.regex.Matcher ruleRef = java.util.regex.Pattern.compile("rule\\((\\w+)\\)")
                                                                             .matcher(value);
                    if (ruleSelector.isPresent()) {
                        // Conditional rule column (#6534): a null-safe classifier ternary that reads the
                        // rule row's column chosen by the source's `by` value. Not a static usedRuleColumn
                        // (the choice is per-row at runtime); its resolved value is null-guarded below.
                        String ternary = conditionalRuleExpression(ruleSelector.get());
                        conditionalRuleGuards.add(ternary);
                        assign.put("expr", ternary);
                    } else if (ruleRef.matches()) {
                        String column = IntentNaming.pascalCase(ruleRef.group(1));
                        usedRuleColumns.add(column);
                        assign.put("expr", "ruleRow." + column);
                    } else if (toOneRelation(itemsEntity, cell.getKey()) != null) {
                        // Source-FK copy (issue #6533): the item cell's key is a to-one relation of the
                        // items entity, so the value names a source relation whose FK id is copied
                        // verbatim onto the line - the counterparty dimension (Customer/Supplier) that
                        // makes an auto-posted line show up in the subledger balances. The raw Long FK is
                        // copied (no re-resolution); a null source FK copies null (the line simply carries
                        // no dimension). NOT negated under reversal: a red-storno line must carry the SAME
                        // dimension as the original, or it would not net the counterparty's balance.
                        assign.put("expr", "source." + IntentNaming.pascalCase(value));
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
            e.put("conditionalRuleGuards", conditionalRuleGuards);
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
    /**
     * The Java expression for a conditional {@code rule(by: ..., cases: ..., default: ...)} account
     * reference (#6534): a null-safe classifier ternary that reads the resolved {@code ruleRow}'s
     * column chosen by the source's {@code by} value. The classifier is read through the SDK
     * {@code Calc} evaluator (null-safe, the same reader the {@code when} guard uses), each case key
     * compared as a {@code BigDecimal}; an unmatched value falls to the {@code default} column, or to
     * {@code null} (which the generated handler null-guards → the posting skips to the unposted
     * worklist).
     */
    private static String conditionalRuleExpression(PostingRuleSelector selector) {
        String classifier = IntentNaming.pascalCase(selector.by());
        String expr = selector.defaultColumn() != null ? "ruleRow." + IntentNaming.pascalCase(selector.defaultColumn()) : "null";
        List<Map.Entry<String, String>> entries = new ArrayList<>(selector.cases()
                                                                          .entrySet());
        for (int i = entries.size() - 1; i >= 0; i--) {
            Map.Entry<String, String> caseEntry = entries.get(i);
            String condition =
                    "Calc.eval(\"" + classifier + "\", source, 6).compareTo(new java.math.BigDecimal(\"" + caseEntry.getKey() + "\")) == 0";
            expr = condition + " ? ruleRow." + IntentNaming.pascalCase(caseEntry.getValue()) + " : " + expr;
        }
        return "(" + expr + ")";
    }

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

    /**
     * The local variable names the two create-from templates declare around a one-hop load.
     *
     * <p>
     * A hop's load is declared in a local named after the RELATION - that is what the shared resolver
     * embeds in the read it renders, and seven templates already render it that way. So a relation
     * named like one of these would emit a second declaration of a name already in scope: a Java
     * compile error on the generated file, loud but with nothing explaining it. Naming the collision
     * here turns that into a message about the model.
     *
     * <p>
     * The list is AUTHORED against {@code Generate.java.template} and {@code Job.java.template}, and
     * comparison is case-SENSITIVE because Java locals are: a relation called {@code Status} does not
     * collide with a local called {@code status}. Drift is safe in one direction only, which is the
     * direction it can drift: a name missing here behaves exactly as it does today (the compile error),
     * while nothing that compiles is ever refused.
     */
    private static final Set<String> CREATE_FROM_LOCALS = Set.of("source", "sourceId", "sourceRepository", "target", "saved", "savedTarget",
            "existing", "candidate", "item", "raw", "values", "req", "id", "entity", "rows", "day", "monthEnd", "recordUrl", "inboxUrl",
            "subject", "body", "document", "part", "parts", "from", "to");

    /**
     * The first one-hop load whose local would collide with a name the template already declares, or
     * {@code null} when none does.
     */
    private static String collidingLocal(List<NotificationSupport.RelationLoad> loads) {
        for (NotificationSupport.RelationLoad load : loads) {
            if (CREATE_FROM_LOCALS.contains(load.local())) {
                return load.local();
            }
        }
        return null;
    }

    /**
     * Whether a {@code map} value is a one-hop {@code relation.field} path rather than a direct
     * property of the source. A property name never carries a dot, so the dot alone decides.
     */
    private static boolean isHop(String value) {
        return value != null && value.indexOf('.') >= 0;
    }

    /**
     * The first {@code map} entry whose source is a one-hop path, rendered for a message, or
     * {@code null} when every source is a direct property. Used to refuse a hop where the source entity
     * is not resolvable here at all - a cross-model source, whose relations live in the owner's
     * {@code .model}.
     */
    private static String firstHop(Map<String, String> map) {
        if (map == null) {
            return null;
        }
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (isHop(entry.getValue())) {
                return entry.getKey() + " -> " + entry.getValue();
            }
        }
        return null;
    }

    /** A YAML scalar as a Java literal: numbers bare, everything else a quoted string. */
    private static String javaLiteral(Object value) {
        // A Boolean written as a String would filter a boolean column with the text "true" and match
        // nothing; the backslash is escaped before the quote so a value carrying either cannot close the
        // literal early. Statuses arrive already resolved to ids, so a lifecycle filter takes the bare
        // integer branch.
        if (value instanceof Boolean) {
            return String.valueOf(value);
        }
        String v = String.valueOf(value);
        if (v.matches("-?\\d+")) {
            return v;
        }
        return '"' + v.replace("\\", "\\\\")
                      .replace("\"", "\\\"")
                + '"';
    }

    /**
     * Pre-render the target field assignments for a generate mapping: a {@code map} entry copies a
     * source property ({@code <sourceVar>.<Prop>}); a {@code defaults} entry sets {@code now} (today's
     * date, in the target field's own shape - see {@code literalExpression}) or a literal. The
     * expression is rendered here (in Java, testable) so the Velocity template only emits
     * {@code target.<prop> = <expr>;} - no expression logic in the template.
     *
     * <p>
     * This overload takes no hop resolver, so a dotted {@code map} value keeps the reading it always
     * had. It serves the item / child mappings, whose source is the row being cloned - the parser
     * refuses a hop there, because one load per row is a different shape from the create-from's single
     * load.
     */
    private static List<Map<String, Object>> assignments(Map<String, String> map, Map<String, String> defaults, String sourceVar,
            java.util.function.Function<String, String> temporalKinds) {
        return assignments(map, defaults, sourceVar, temporalKinds, null);
    }

    /**
     * The same pre-rendering, with a resolver that lets a {@code map} value be a one-hop
     * {@code relation.field} of the source. A direct property is read off the source row the generated
     * code already holds; a hop is read off the RELATED row, through the null-guarded access the
     * notification resolver renders - and registering it with that resolver is what puts the
     * load-by-foreign-key in the template's {@code relationLoads}. Both call sites therefore bind
     * {@code relationLoads} from {@code hops.loads()} right after calling this.
     *
     * @return the assignments, or {@code null} when a hop cannot be resolved - the caller reports that
     *         and skips the entry rather than emitting a read that does not compile
     */
    private static List<Map<String, Object>> assignments(Map<String, String> map, Map<String, String> defaults, String sourceVar,
            java.util.function.Function<String, String> temporalKinds, NotificationSupport.Resolver hops) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (map != null) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                if (entry.getValue() == null || entry.getValue()
                                                     .isBlank()) {
                    continue;
                }
                String value = entry.getValue()
                                    .trim();
                String expression =
                        hops != null && isHop(value) ? hops.access(value, false) : sourceVar + "." + IntentNaming.pascalCase(value);
                if (expression == null) {
                    return null;
                }
                list.add(assignment(entry.getKey(), expression));
            }
        }
        if (defaults != null) {
            for (Map.Entry<String, String> entry : defaults.entrySet()) {
                if (entry.getValue() == null || entry.getValue()
                                                     .isBlank()) {
                    continue;
                }
                list.add(assignment(entry.getKey(),
                        literalExpression(entry.getValue(), temporalKinds.apply(IntentNaming.pascalCase(entry.getKey())))));
            }
        }
        return list;
    }

    /**
     * Like {@code assignments} but a NUMERIC literal default renders as a {@code BigDecimal} - the
     * child shapes (hours, amounts) are decimal columns, and a bare int literal does not convert to the
     * generated {@code BigDecimal} field.
     */
    private static List<Map<String, Object>> childAssignments(Map<String, String> map, Map<String, String> defaults, String sourceVar,
            java.util.function.Function<String, String> temporalKinds, java.util.Set<String> relationProperties) {
        Map<String, String> typedDefaults = new LinkedHashMap<>();
        if (defaults != null) {
            typedDefaults.putAll(defaults);
        }
        List<Map<String, Object>> list = assignments(map, java.util.Map.of(), sourceVar, temporalKinds);
        for (Map.Entry<String, String> entry : typedDefaults.entrySet()) {
            if (entry.getValue() == null || entry.getValue()
                                                 .isBlank()) {
                continue;
            }
            String expression = literalExpression(entry.getValue(), temporalKinds.apply(IntentNaming.pascalCase(entry.getKey())));
            // A to-one relation default is a foreign-key ID - the generated field is Integer, so the
            // decimal-column convenience wrap below would not even compile against it.
            boolean relation = relationProperties != null && relationProperties.contains(IntentNaming.pascalCase(entry.getKey()));
            if (!relation && expression.matches("-?\\d+(\\.\\d+)?")) {
                expression = "new java.math.BigDecimal(\"" + expression + "\")";
            }
            list.add(assignment(entry.getKey(), expression));
        }
        return list;
    }

    /**
     * The pascal-cased names of the item target's to-one relations - locally from its intent relations,
     * cross-model from the owner {@code .model}'s widget types (the edm generator gives every to-one FK
     * the DROPDOWN - or, for the EntityStatus one, DOCUMENT_STATUS - widget, so the owner model carries
     * the fact without a new attribute).
     */
    private static java.util.Set<String> relationProperties(EntityIntent local, CrossModelSupport.TargetInfo target) {
        java.util.Set<String> names = new java.util.LinkedHashSet<>();
        if (local != null && local.getRelations() != null) {
            for (RelationIntent relation : local.getRelations()) {
                boolean toOne = "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
                if (toOne && relation.getName() != null) {
                    names.add(IntentNaming.pascalCase(relation.getName()));
                }
            }
        }
        if (target != null && target.propertyWidgets() != null) {
            for (Map.Entry<String, String> widget : target.propertyWidgets()
                                                          .entrySet()) {
                // MULTISELECT is deliberately excluded: this set identifies FK relation properties, and
                // a subset relation's property is a plain VARCHAR value list, never an FK.
                if ("DROPDOWN".equals(widget.getValue()) || "DOCUMENT_STATUS".equals(widget.getValue())) {
                    names.add(widget.getKey());
                }
            }
        }
        return names;
    }

    /**
     * The rendering-relevant type of a target line-items cell: its logical {@code kind} ({@code string}
     * / {@code decimal} / {@code double} / {@code integer} / {@code long} / {@code boolean} /
     * {@code date} / {@code timestamp} / {@code month} / {@code week} / {@code unknown}), the decimal
     * {@code scale} (for the {@code Calc} rounding of a numeric cell) and whether the cell is a to-one
     * {@code relation} (a foreign-key copy, not an arithmetic value).
     */
    private record CellMeta(String kind, int scale, boolean relation) {
    }

    /**
     * The cell metas of a SAME-model target items child, read from its intent fields / to-one
     * relations.
     */
    private static Map<String, CellMeta> localCellMetas(EntityIntent itemEntity) {
        Map<String, CellMeta> metas = new LinkedHashMap<>();
        if (itemEntity.getRelations() != null) {
            for (RelationIntent relation : itemEntity.getRelations()) {
                boolean toOne = "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
                if (toOne && relation.getName() != null) {
                    metas.put(IntentNaming.pascalCase(relation.getName()), new CellMeta("relation", 0, true));
                }
            }
        }
        if (itemEntity.getFields() != null) {
            for (FieldIntent field : itemEntity.getFields()) {
                if (field.getName() == null || field.getType() == null) {
                    continue;
                }
                int scale = field.getScale() != null ? field.getScale() : 2;
                metas.put(IntentNaming.pascalCase(field.getName()), new CellMeta(kindOfIntentType(field.getType()), scale, false));
            }
        }
        return metas;
    }

    /** The cell metas of a CROSS-model target items child, read from the owner {@code .model}. */
    private static Map<String, CellMeta> crossModelCellMetas(CrossModelSupport.ItemsChildInfo child) {
        Map<String, CellMeta> metas = new LinkedHashMap<>();
        for (Map.Entry<String, String> property : child.propertyTypes()
                                                       .entrySet()) {
            String name = property.getKey(); // owner .model property names are already PascalCase
            if (child.relationProperties()
                     .contains(name)) {
                metas.put(name, new CellMeta("relation", 0, true));
                continue;
            }
            String widget = child.propertyWidgets()
                                 .get(name);
            String kind;
            if ("MONTH".equals(widget)) {
                kind = "month";
            } else if ("WEEK".equals(widget)) {
                kind = "week";
            } else {
                kind = kindOfJdbcType(property.getValue());
            }
            int scale = child.propertyScales()
                             .getOrDefault(name, 2);
            metas.put(name, new CellMeta(kind, scale, false));
        }
        return metas;
    }

    /** Logical cell kind for an intent field type (same-model target). */
    private static String kindOfIntentType(String type) {
        return switch (type.toLowerCase(java.util.Locale.ROOT)) {
            case "string", "text", "uuid" -> "string";
            case "integer", "int" -> "integer";
            case "long" -> "long";
            case "decimal" -> "decimal";
            case "double" -> "double";
            case "boolean" -> "boolean";
            case "date" -> "date";
            case "timestamp" -> "timestamp";
            case "month" -> "month";
            case "week" -> "week";
            default -> "unknown";
        };
    }

    /**
     * Logical cell kind for a JDBC {@code dataType} (cross-model target, read from the owner .model).
     */
    private static String kindOfJdbcType(String dataType) {
        return switch (dataType == null ? "" : dataType.toUpperCase(java.util.Locale.ROOT)) {
            case "VARCHAR", "CHAR", "CLOB", "LONGVARCHAR", "NVARCHAR" -> "string";
            case "DECIMAL", "NUMERIC" -> "decimal";
            case "DOUBLE", "REAL", "FLOAT" -> "double";
            case "INTEGER", "SMALLINT", "TINYINT" -> "integer";
            case "BIGINT" -> "long";
            case "BOOLEAN", "BIT" -> "boolean";
            case "DATE" -> "date";
            case "TIMESTAMP", "TIME" -> "timestamp";
            default -> "unknown";
        };
    }

    /**
     * PascalCase names of the source entity's fields + to-one relations (drives string-cell
     * copy/interpolation).
     */
    private static java.util.Set<String> sourceProperties(EntityIntent source) {
        java.util.Set<String> names = new java.util.LinkedHashSet<>();
        if (source == null) {
            return names;
        }
        if (source.getFields() != null) {
            for (FieldIntent field : source.getFields()) {
                if (field.getName() != null) {
                    names.add(IntentNaming.pascalCase(field.getName()));
                }
            }
        }
        if (source.getRelations() != null) {
            for (RelationIntent relation : source.getRelations()) {
                if (relation.getName() != null) {
                    names.add(IntentNaming.pascalCase(relation.getName()));
                }
            }
        }
        return names;
    }

    /**
     * Render the computed line-items ({@code itemLines}, issue #6555): one entry per synthetic line,
     * each {@code {guard, assigns:[{targetProp, expr}]}} - the same pre-rendered shape a posting item
     * row uses, so the template stays logic-free. Every {@code expr} runs over the loaded
     * {@code source} master. A {@code when} cell becomes the row guard.
     */
    private static List<Map<String, Object>> computedItemLines(List<Map<String, String>> rows, Map<String, CellMeta> metas,
            java.util.Set<String> sourceProps) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (rows == null) {
            return out;
        }
        for (Map<String, String> row : rows) {
            Map<String, Object> rendered = new LinkedHashMap<>();
            List<Map<String, Object>> assigns = new ArrayList<>();
            String guard = "";
            for (Map.Entry<String, String> cell : row.entrySet()) {
                String value = cell.getValue() == null ? ""
                        : cell.getValue()
                              .trim();
                if ("when".equalsIgnoreCase(cell.getKey())) {
                    guard = computedGuard(value);
                    continue;
                }
                CellMeta meta = metas.get(IntentNaming.pascalCase(cell.getKey()));
                assigns.add(assignment(cell.getKey(), computedCellExpression(value, meta, sourceProps)));
            }
            rendered.put("guard", guard);
            rendered.put("assigns", assigns);
            out.add(rendered);
        }
        return out;
    }

    /**
     * A single computed-line cell as a Java expression over {@code source}: a to-one relation copies
     * the source foreign key ({@code source.<Prop>}, issue #6533 parity); a numeric field is a
     * {@code Calc} arithmetic expression rounded to its scale (integer/long narrowed off the
     * {@code BigDecimal}); a string field is a {@code {field}}-interpolated concatenation, a bare
     * source-property copy, or a quoted literal; a {@code month}/{@code week}/date/boolean field takes
     * {@code now} / a literal.
     */
    private static String computedCellExpression(String value, CellMeta meta, java.util.Set<String> sourceProps) {
        String v = value == null ? "" : value.trim();
        if (meta != null && meta.relation()) {
            return "source." + IntentNaming.pascalCase(v);
        }
        String kind = meta == null ? "unknown" : meta.kind();
        int scale = meta == null ? 2 : meta.scale();
        switch (kind) {
            case "decimal":
                return calcExpression(v, scale);
            case "double":
                return calcExpression(v, scale) + ".doubleValue()";
            case "integer":
                return calcExpression(v, 0) + ".intValue()";
            case "long":
                return calcExpression(v, 0) + ".longValue()";
            case "boolean":
            case "date":
            case "timestamp":
            case "month":
            case "week": {
                // A bare source property copies it (e.g. a date carried over from the source); otherwise
                // `now` / a literal in the field's own shape (month -> YYYY-MM, week -> YYYY-Www, else
                // LocalDate / boolean / quoted string).
                String copy = bareSourceCopy(v, sourceProps);
                String temporalKind = "month".equals(kind) || "week".equals(kind) ? kind : null;
                return copy != null ? copy : literalExpression(v, temporalKind);
            }
            case "string":
                return stringCellExpression(v, sourceProps);
            default:
                // unknown: only when a cross-model item child is unresolved (null-context unit tests);
                // best effort - an arithmetic-looking value is numeric, otherwise a string.
                return v.matches("[\\w.]*[-+*/()][\\w.+\\-*/() ]*") ? calcExpression(v, scale) : stringCellExpression(v, sourceProps);
        }
    }

    /**
     * {@code Calc.eval("<expr>", source, <scale>)} - the calculated-field / posting-amount convention.
     */
    private static String calcExpression(String expr, int scale) {
        return "Calc.eval(\"" + expr.replace("\\", "\\\\")
                                    .replace("\"", "\\\"")
                + "\", source, " + scale + ")";
    }

    /**
     * A string cell: {@code {field}} placeholders become a Java concatenation over {@code source}; a
     * bare identifier that IS a source property copies it ({@code source.<Prop>}); anything else is a
     * quoted literal (so a plain caption like {@code "Consulting services"} is NOT read as a field).
     */
    private static String stringCellExpression(String v, java.util.Set<String> sourceProps) {
        if (v.contains("{")) {
            StringBuilder expr = new StringBuilder();
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\{(\\w+)\\}")
                                                               .matcher(v);
            int last = 0;
            while (m.find()) {
                if (m.start() > last) {
                    appendConcat(expr, '"' + v.substring(last, m.start())
                                              .replace("\"", "\\\"")
                            + '"');
                }
                appendConcat(expr, "String.valueOf(source." + IntentNaming.pascalCase(m.group(1)) + ")");
                last = m.end();
            }
            if (last < v.length()) {
                appendConcat(expr, '"' + v.substring(last)
                                          .replace("\"", "\\\"")
                        + '"');
            }
            return expr.length() == 0 ? "\"\"" : expr.toString();
        }
        String copy = bareSourceCopy(v, sourceProps);
        if (copy != null) {
            return copy;
        }
        return "\"" + v.replace("\\", "\\\\")
                       .replace("\"", "\\\"")
                       .replace("\n", "\\n")
                       .replace("\r", "\\r")
                + "\"";
    }

    /**
     * {@code source.<Prop>} when {@code v} is a bare identifier naming a source property, else null.
     */
    private static String bareSourceCopy(String v, java.util.Set<String> sourceProps) {
        if (v.matches("[A-Za-z_]\\w*") && sourceProps.contains(IntentNaming.pascalCase(v))) {
            return "source." + IntentNaming.pascalCase(v);
        }
        return null;
    }

    private static void appendConcat(StringBuilder expr, String term) {
        if (expr.length() > 0) {
            expr.append(" + ");
        }
        expr.append(term);
    }

    /**
     * A computed-line {@code when} guard as a null-safe {@code Calc} comparison over {@code source} -
     * the postings item-row guard convention ({@code <field> ==|!= <n>}); an unparseable guard yields
     * no guard (the line is always created).
     */
    private static String computedGuard(String value) {
        java.util.regex.Matcher guard = java.util.regex.Pattern.compile("\\s*(\\w+)\\s*([!=]=)\\s*(\\d+(?:\\.\\d+)?)\\s*")
                                                               .matcher(value);
        if (guard.matches()) {
            return "Calc.eval(\"" + IntentNaming.pascalCase(guard.group(1)) + "\", source, 6).compareTo(new java.math.BigDecimal(\""
                    + guard.group(3) + "\")) " + ("==".equals(guard.group(2)) ? "==" : "!=") + " 0";
        }
        return "";
    }

    /**
     * The logical temporal kind of the TARGET entity's fields, for the type-aware {@code now} default:
     * PascalCase property name -> {@code month} / {@code week}; anything else absent (null). A
     * same-model target reads its intent fields directly; a cross-model target reads the owner model's
     * widget types through {@link CrossModelSupport.TargetInfo#propertyWidgets()} - the {@code .model}
     * is the only cross-model carrier of the LOGICAL type, since month/week are plain VARCHAR at the
     * JDBC level. An unresolved target (unit test / convention fallback) keeps the untyped behavior.
     */
    private static java.util.function.Function<String, String> temporalKinds(EntityIntent local, CrossModelSupport.TargetInfo target) {
        Map<String, String> kinds = new LinkedHashMap<>();
        if (local != null && local.getFields() != null) {
            for (FieldIntent field : local.getFields()) {
                if (field.getName() == null || field.getType() == null) {
                    continue;
                }
                String type = field.getType()
                                   .toLowerCase(java.util.Locale.ROOT);
                if ("month".equals(type) || "week".equals(type)) {
                    kinds.put(IntentNaming.pascalCase(field.getName()), type);
                }
            }
        }
        if (target != null && target.propertyWidgets() != null) {
            for (Map.Entry<String, String> widget : target.propertyWidgets()
                                                          .entrySet()) {
                if ("MONTH".equals(widget.getValue())) {
                    kinds.put(widget.getKey(), "month");
                } else if ("WEEK".equals(widget.getValue())) {
                    kinds.put(widget.getKey(), "week");
                }
            }
        }
        return kinds::get;
    }

    private static Map<String, Object> assignment(String targetProperty, String expression) {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("targetProp", IntentNaming.pascalCase(targetProperty));
        a.put("expr", expression);
        return a;
    }

    /**
     * A Java expression for a {@code defaults} value: {@code now} -> today's value in the TARGET
     * field's own shape - a {@code month} field gets the {@code YYYY-MM} string, a {@code week} field
     * the {@code YYYY-Www} ISO-week string, anything else today's {@code LocalDate} (month/week are
     * plain {@code String} properties on the generated entity, so the untyped {@code LocalDate.now()}
     * would not even compile against them); an integer / decimal / boolean literal -> its Java form;
     * anything else -> a quoted Java string.
     */
    private static String literalExpression(String value, String temporalKind) {
        String v = value.trim();
        if ("now".equals(v)) {
            if ("month".equals(temporalKind)) {
                return "java.time.YearMonth.now().toString()";
            }
            if ("week".equals(temporalKind)) {
                return "String.format(\"%04d-W%02d\", java.time.LocalDate.now().get(java.time.temporal.IsoFields.WEEK_BASED_YEAR), "
                        + "java.time.LocalDate.now().get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR))";
            }
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

    /**
     * Copies a descriptor into one per-event handler entry - the shape shared by every recompute-style
     * listener (roll-ups, expansions, settlements): the same descriptor rendered once per bound event,
     * distinguished only by its class name and the topic suffix it binds.
     *
     * @param base the descriptor
     * @param className the generated handler class name
     * @param topicSuffix the bound event's topic suffix
     * @return the entry
     */
    private static Map<String, Object> rollupEntry(Map<String, Object> base, String className, String topicSuffix) {
        Map<String, Object> entry = new LinkedHashMap<>(base);
        entry.put("className", className);
        entry.put("topicSuffix", topicSuffix);
        return entry;
    }

    /**
     * Period expansions: per expansion, three handlers - on the master's create and update events, that
     * reconcile the child rows for the span as a diff (insert the missing periods, delete the ones that
     * fell out of it, keep the rest), and on its delete event, that removes them again. Everything
     * type-dependent (the defaults literals, the count write-back) is pre-rendered here as Java lines
     * so the template stays shape-only; the child rows go through the child repository, so their
     * create/delete events fire and downstream roll-ups/guards run exactly as for hand-entered rows.
     */
    private static ExpansionHandlers buildExpansions(IntentModel model, Map<String, EntityIntent> byName,
            Map<String, String> compositionParents, IntentSettings settings) {
        List<Map<String, Object>> expansions = new ArrayList<>();
        List<Map<String, Object>> cleanups = new ArrayList<>();
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
            base.put("masterPerspective", IntentEntities.resolvePerspective(expansion.getFrom(), compositionParents, model));
            base.put("masterPk", IntentEntities.keyFieldName(master));
            base.put("childEntity", expansion.getInto());
            base.put("childPerspective", IntentEntities.resolvePerspective(expansion.getInto(), compositionParents, model));
            // The child's key: the reconciliation keeps the rows whose period survives a span change and
            // re-spreads their share by id, so it needs the primary key property to address them.
            base.put("childPk", IntentEntities.keyFieldName(child));
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
            cleanups.add(rollupEntry(base, className + "OnDelete", "-deleted"));
        }
        return new ExpansionHandlers(expansions, cleanups);
    }

    /**
     * The handlers an intent's expansions contribute, split by the template that renders them: the
     * reconciliation pair per expansion, and the cleanup that removes the generated rows when their
     * master is deleted. They are two collections rather than one because a template source renders
     * once per collection entry, and the cleanup's body shares nothing with the reconciliation's.
     *
     * @param reconciliations the create/update handlers
     * @param cleanups the master-delete handlers
     */
    private record ExpansionHandlers(List<Map<String, Object>> reconciliations, List<Map<String, Object>> cleanups) {
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
            Map<String, String> compositionParents, IntentSettings settings, IntentGenerationContext context) {
        List<Map<String, Object>> inbound = new ArrayList<>();
        for (InboundIntent webhook : model.getInbound()) {
            if (webhook.getSource() != null) {
                continue; // a non-HTTP source is its own collection (its own generated handler shape)
            }
            Map<String, Object> entry = inboundEntry(webhook, model, byName, compositionParents, settings, "controller", context);
            if (entry == null) {
                continue;
            }
            entry.put("path", webhook.getPath());
            inbound.add(entry);
        }
        return inbound;
    }

    /**
     * The queue / topic ingests: one self-describing {@code MessageHandler} each, consuming the JSON
     * record off the declared destination and saving it exactly as the webhook does with a posted body.
     */
    private static List<Map<String, Object>> buildInboundMessages(IntentModel model, Map<String, EntityIntent> byName,
            Map<String, String> compositionParents, IntentSettings settings, IntentGenerationContext context) {
        List<Map<String, Object>> messages = new ArrayList<>();
        for (InboundIntent ingest : model.getInbound()) {
            InboundSourceIntent source = ingest.getSource();
            boolean queue = source != null && source.getQueue() != null && !source.getQueue()
                                                                                  .isBlank();
            boolean topic = source != null && source.getTopic() != null && !source.getTopic()
                                                                                  .isBlank();
            if (!queue && !topic) {
                continue;
            }
            Map<String, Object> entry = inboundEntry(ingest, model, byName, compositionParents, settings, "consumer", context);
            if (entry == null) {
                continue;
            }
            entry.put("destination", queue ? source.getQueue() : source.getTopic());
            entry.put("listenerKind", queue ? "QUEUE" : "TOPIC");
            messages.add(entry);
        }
        return messages;
    }

    /**
     * The drop-folder ingests: one {@code JobHandler} each, polling the folder on the declared cron and
     * saving every record of every file that arrived.
     */
    private static List<Map<String, Object>> buildInboundFiles(IntentModel model, Map<String, EntityIntent> byName,
            Map<String, String> compositionParents, IntentSettings settings, IntentGenerationContext context) {
        List<Map<String, Object>> files = new ArrayList<>();
        for (InboundIntent ingest : model.getInbound()) {
            InboundSourceIntent source = ingest.getSource();
            if (source == null || source.getFolder() == null || source.getFolder()
                                                                      .isBlank()) {
                continue;
            }
            Map<String, Object> entry = inboundEntry(ingest, model, byName, compositionParents, settings, "job", context);
            if (entry == null) {
                continue;
            }
            entry.put("folder", source.getFolder());
            entry.put("cron", source.getCron());
            files.add(entry);
        }
        return files;
    }

    /**
     * The facts every inbound ingest shares, whatever it arrives on - or {@code null} when the entry is
     * unusable (no name, an unknown entity) or the developer opted out of generating it.
     *
     * <p>
     * The declared {@code accept:} gate and {@code map:} projection are shared too: what an arrival's
     * payload looks like has nothing to do with what it travelled on, so all three handler shapes read
     * it from the same plan. A mapping that cannot be resolved drops the whole arrival with the reason
     * - ingesting the raw payload instead would silently store something else.
     */
    private static Map<String, Object> inboundEntry(InboundIntent ingest, IntentModel model, Map<String, EntityIntent> byName,
            Map<String, String> compositionParents, IntentSettings settings, String handlerNoun, IntentGenerationContext context) {
        if (ingest.getName() == null || ingest.getName()
                                              .isBlank()) {
            return null;
        }
        String entity = ingest.getCreate();
        if (entity == null || !byName.containsKey(entity)) {
            return null;
        }
        if (!settings.shouldGenerate("inbound", ingest.getName())) {
            LOGGER.info("Settings opt-out: keeping existing {} for inbound [{}] (not generated)", handlerNoun, ingest.getName());
            return null;
        }
        ArrivalSupport.Plan arrival;
        try {
            arrival = ArrivalSupport.plan(ingest, byName.get(entity), byName, compositionParents, model);
        } catch (IllegalArgumentException ex) {
            reportDroppedGlue(context, "Inbound [" + ingest.getName() + "]: " + ex.getMessage() + " - the arrival was NOT generated");
            return null;
        }
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("name", ingest.getName());
        entry.put("className", IntentNaming.pascalCase(ingest.getName()));
        entry.put("entity", entity);
        entry.put("perspective", IntentEntities.resolvePerspective(entity, compositionParents, model));
        entry.putAll(ArrivalSupport.arrivalFields(arrival));
        return entry;
    }

    /**
     * The process-step event emitters: the {@code JavaDelegate} the BPMN generator inserts at each
     * observed step boundary to publish the process's trigger entity on the step topic. Deduplicated
     * per (process, step, moment) by {@link StepEventSupport}, so ten notifications on the same moment
     * still publish once.
     */
    private static List<Map<String, Object>> buildStepEvents(IntentModel model, Map<String, String> compositionParents,
            IntentSettings settings) {
        List<Map<String, Object>> stepEvents = new ArrayList<>();
        for (StepEventSupport.Emitter emitter : StepEventSupport.emitters(model)) {
            if (!settings.shouldGenerate("stepEvents", emitter.className())) {
                LOGGER.info("Settings opt-out: keeping existing delegate for step event [{}] (not generated)", emitter.className());
                continue;
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", emitter.className());
            entry.put("className", emitter.className());
            entry.put("process", emitter.process());
            entry.put("step", emitter.step());
            entry.put("entity", emitter.entity());
            entry.put("perspective", IntentEntities.resolvePerspective(emitter.entity(), compositionParents, model));
            entry.put("keyProperty", emitter.keyProperty());
            entry.put("keyAccessor", emitter.keyAccessor());
            entry.put("topicSuffix", StepEventSupport.topicSuffix(emitter.process(), emitter.step(), emitter.kind()));
            stepEvents.add(entry);
        }
        return stepEvents;
    }

    private static List<Map<String, Object>> buildIntegrations(IntentModel model, Map<String, EntityIntent> byName,
            Map<String, String> compositionParents, IntentSettings settings, IntentGenerationContext context) {
        List<Map<String, Object>> integrations = new ArrayList<>();
        for (IntegrationIntent integration : model.getIntegrations()) {
            if (integration.getName() == null || integration.getName()
                                                            .isBlank()) {
                continue;
            }
            // Either axis - see the notification builder: a step event forwards the trigger entity.
            String entity = StepEventSupport.eventEntity(model, integration.getEvent());
            if (entity == null || !byName.containsKey(entity)) {
                continue;
            }
            if (!settings.shouldGenerate("integrations", integration.getName())) {
                LOGGER.info("Settings opt-out: keeping existing listener for integration [{}] (not generated)", integration.getName());
                continue;
            }
            // The declared envelope, when there is one. A value that cannot be resolved (a cross-model
            // relation.field the owner does not carry) drops the whole integration with the reason -
            // sending the record instead would silently substitute a different contract.
            PayloadSupport.Plan payload;
            try {
                payload = PayloadSupport.plan(integration.getPayload(), byName.get(entity), byName, compositionParents,
                        crossModelLookup(model, context));
            } catch (IllegalArgumentException ex) {
                reportDroppedGlue(context,
                        "Integration [" + integration.getName() + "]: " + ex.getMessage() + " - the integration was NOT generated");
                continue;
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", integration.getName());
            entry.put("className", IntentNaming.pascalCase(integration.getName()));
            entry.put("entity", entity);
            entry.put("perspective", IntentEntities.resolvePerspective(entity, compositionParents, model));
            entry.put("topicSuffix", StepEventSupport.topicSuffix(integration.getEvent()));
            entry.put("clientMethod", IntegrationSupport.clientMethod(integration.getMethod()));
            entry.put("hasBody", IntegrationSupport.hasBody(integration.getMethod()));
            entry.put("urlExpression", IntegrationSupport.urlExpression(integration.getUrl()));
            // The event axis carries a `when` guard and every other consumer of the axis honours it -
            // an integration that ignored it forwarded records the author had excluded.
            entry.putAll(guardFields(integration.getEvent()));
            entry.putAll(PayloadSupport.payloadFields(payload));
            entry.put("relationLoads", relationLoads(payload == null ? List.of() : payload.loads()));
            integrations.add(entry);
        }
        return integrations;
    }

    /**
     * The outbound departures: one self-describing {@code MessageHandler} each, subscribed to the event
     * topic the record is already published on and re-publishing it - as the record's JSON, or as the
     * declared envelope - on the queue or topic the entry names.
     *
     * <p>
     * The publisher is a subscriber, which is what gives the construct its stated semantics for free:
     * the write is already committed by the time the event message is delivered, so a failed departure
     * can never fail the write it reacts to.
     */
    private static List<Map<String, Object>> buildOutbound(IntentModel model, Map<String, EntityIntent> byName,
            Map<String, String> compositionParents, IntentSettings settings, IntentGenerationContext context) {
        List<Map<String, Object>> departures = new ArrayList<>();
        for (OutboundIntent outbound : model.getOutbound()) {
            if (outbound.getName() == null || outbound.getName()
                                                      .isBlank()) {
                continue;
            }
            // Either axis - see the notification builder: a step event carries the trigger entity.
            String entity = StepEventSupport.eventEntity(model, outbound.getEvent());
            if (entity == null || !byName.containsKey(entity)) {
                continue;
            }
            OutboundSupport.Target target = OutboundSupport.target(outbound.getTo());
            if (target == null) {
                continue; // parser already reported "exactly one of queue/topic"
            }
            if (!settings.shouldGenerate("outbound", outbound.getName())) {
                LOGGER.info("Settings opt-out: keeping existing publisher for outbound [{}] (not generated)", outbound.getName());
                continue;
            }
            // The declared envelope, when there is one. A value that cannot be resolved (a cross-model
            // relation.field the owner does not carry) drops the whole departure with the reason -
            // emitting the record instead would silently put a different contract on the wire.
            PayloadSupport.Plan payload;
            try {
                payload = PayloadSupport.plan(outbound.getPayload(), byName.get(entity), byName, compositionParents,
                        crossModelLookup(model, context));
            } catch (IllegalArgumentException ex) {
                reportDroppedGlue(context,
                        "Outbound [" + outbound.getName() + "]: " + ex.getMessage() + " - the departure was NOT generated");
                continue;
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", outbound.getName());
            entry.put("className", IntentNaming.pascalIdentifier(outbound.getName()));
            entry.put("entity", entity);
            entry.put("perspective", IntentEntities.resolvePerspective(entity, compositionParents, model));
            entry.put("topicSuffix", StepEventSupport.topicSuffix(outbound.getEvent()));
            entry.put("destination", target.destination());
            entry.put("channel", target.channel()
                                       .name());
            entry.put("producerMethod", target.producerMethod());
            entry.putAll(guardFields(outbound.getEvent()));
            entry.putAll(PayloadSupport.payloadFields(payload));
            entry.put("relationLoads", relationLoads(payload == null ? List.of() : payload.loads()));
            departures.add(entry);
        }
        return departures;
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
            // The source entity may live in another model (model: <uses alias>). It is resolved against
            // the owner's .model through CrossModelSupport (workspace first, registry fallback) - the
            // same two-tier, order-independent resolution relations / dependsOn / leafOnly use. A null
            // context (unit test) yields the naming-convention defaults so the shape can be asserted
            // without a repository.
            boolean sourceCrossModel = schedule.getModel() != null && !schedule.getModel()
                                                                               .isBlank();
            CrossModelSupport.TargetInfo sourceTarget = null;
            if (sourceCrossModel) {
                UsesIntent sourceUses = findUses(model, schedule.getModel());
                if (sourceUses == null) {
                    continue; // parser already reported the undeclared alias
                }
                try {
                    sourceTarget = CrossModelSupport.resolve(context, sourceUses, entity);
                } catch (IntentValidationException ex) {
                    reportDroppedGlue(context, "Schedule [" + schedule.getName() + "] source entity [" + entity + "] in model ["
                            + schedule.getModel() + "] cannot be resolved: " + ex.getMessage() + " - the schedule was NOT generated");
                    continue;
                }
            } else if (entity == null || !byName.containsKey(entity)) {
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
            // Never emit a job that cannot compile: for a cross-model source, validate every reference
            // that resolves against the source row (where fields, generate.map sources, child match
            // sources) - and, for a cross-model forEach collection, its own match/map references -
            // against the owner's properties, dropping the schedule loudly on a miss. Skipped when the
            // owner model was not resolved (convention fallback), the same rule dependsOn uses.
            if (sourceCrossModel) {
                String missingRef = firstUnresolvableScheduleRef(model, schedule, sourceTarget, context);
                if (missingRef != null) {
                    reportDroppedGlue(context, "Schedule [" + schedule.getName() + "] " + missingRef + " does not resolve against the ["
                            + schedule.getModel() + "] source - the schedule was NOT generated");
                    continue;
                }
            }

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", schedule.getName());
            // pascalIdentifier (not pascalCase) so a hyphenated schedule name yields a valid Java class.
            entry.put("className", IntentNaming.pascalIdentifier(schedule.getName()));
            entry.put("cron", schedule.getCron());
            entry.put("entity", entity);
            // A cross-model source's generated job imports the OWNER's gen classes (the leafOnly
            // precedent); sourceModel drives the sanitized gen folder in the template, and the
            // perspective / key come from the owner's .model.
            entry.put("sourceCrossModel", sourceCrossModel);
            entry.put("sourceModel", sourceCrossModel ? schedule.getModel() : "");
            entry.put("perspective", sourceCrossModel ? sourceTarget.perspectiveName()
                    : IntentEntities.resolvePerspective(entity, compositionParents, model));
            // The PK property an `attach: print` hands to the generated print feeder. Deliberately
            // NOT named keyProperty: that key marks a TRIGGER entry (its process variable), and the
            // engine IT keys "no trigger was generated" on trigger-only keys being absent.
            entry.put("attachKeyProperty", sourceCrossModel ? sourceTarget.keyField() : IntentEntities.keyFieldName(byName.get(entity)));
            entry.put("criteriaExpression", ScheduleSupport.criteriaExpression(schedule));
            // The attachment and deep-link keys are always present (empty for a generate schedule): an
            // undefined Velocity variable renders as its own name, so a template must never rely on
            // absence.
            entry.putAll(NotifySupport.attachmentFields(null));
            entry.putAll(NotifySupport.deepLinkFields(null, null));

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
                        target != null ? target.perspectiveName() : IntentEntities.resolvePerspective(g.getTo(), compositionParents, model);
                String toPk = target != null ? target.keyField() : IntentEntities.keyFieldName(byName.get(g.getTo()));
                entry.put("action", "generate");
                entry.put("genCrossModel", crossModel);
                entry.put("genToEntity", g.getTo());
                entry.put("genToModel", crossModel ? g.getUses() : "");
                entry.put("genToPerspective", toPerspective);
                entry.put("genToPk", toPk);
                // The row the job queried is the source, so a one-hop map source is loaded off it exactly
                // as the notify action loads a relation.field recipient - and `relationLoads` is what the
                // job template renders those loads from, for either action.
                EntityIntent rowEntity = sourceCrossModel ? null : byName.get(entity);
                if (rowEntity == null && firstHop(g.getMap()) != null) {
                    reportDroppedGlue(context,
                            "Schedule [" + schedule.getName() + "] generate map [" + firstHop(g.getMap())
                                    + "] is a relation.field of a source this model cannot resolve (model [" + schedule.getModel()
                                    + "]), whose relations are known only to its owner - map a direct property of the row"
                                    + " - the schedule was NOT generated");
                    continue;
                }
                NotificationSupport.Resolver hops = rowEntity == null ? null
                        : NotificationSupport.resolver(rowEntity, byName, compositionParents, crossModelLookup(model, context));
                List<Map<String, Object>> genFieldAssignments = assignments(g.getMap(), g.getDefaults(), "entity",
                        temporalKinds(crossModel ? null : byName.get(g.getTo()), target), hops);
                if (genFieldAssignments == null) {
                    reportDroppedGlue(context, "Schedule [" + schedule.getName() + "] generate map [" + firstHop(g.getMap())
                            + "] is not a resolvable one-hop relation.field of [" + entity + "] - the schedule was NOT generated");
                    continue;
                }
                List<NotificationSupport.RelationLoad> hopLoads = hops == null ? List.of() : hops.loads();
                String collision = collidingLocal(hopLoads);
                if (collision != null) {
                    reportDroppedGlue(context,
                            "Schedule [" + schedule.getName() + "] generate hops through the relation [" + collision + "] of [" + entity
                                    + "], whose name is one the generated job already uses for a local of its own"
                                    + " - rename the relation, or map a direct property instead - the schedule was NOT generated");
                    continue;
                }
                entry.put("genFieldAssignments", genFieldAssignments);
                entry.put("relationLoads", relationLoads(hopLoads));
                if (g.getChildren() != null && !g.getChildren()
                                                 .isEmpty()) {
                    // Collection-driven children: one row per element of a source collection, saved
                    // under the just-generated parent. Everything is pre-rendered here (the
                    // expansions convention) - the job template stays shape-only.
                    entry.put("genChildren", buildGenerateChildren(g.getChildren(), uses, model, byName, compositionParents, context, 1));
                }
            } else {
                // The per-row action reuses the notification machinery against the queried row entity.
                NotificationSupport.Plan plan = NotificationSupport.plan(schedule.getNotify(), byName.get(entity), byName,
                        compositionParents, crossModelLookup(model, context));
                if (plan == null) {
                    reportDroppedGlue(context, "Schedule [" + schedule.getName() + "] notify recipient [" + schedule.getNotify()
                                                                                                                    .getTo()
                            + "] is not a resolvable field or relation.field of [" + entity + "] - the schedule was NOT generated");
                    continue;
                }
                NotifySupport.PrintAttachment attachment = printAttachment(schedule.getNotify(), byName.get(entity), model, byName,
                        compositionParents, context, "Schedule [" + schedule.getName() + "] notify");
                if (attachment == null && NotifySupport.attachesPrint(schedule.getNotify())) {
                    continue; // asked for the document but it cannot be rendered - reported above
                }
                entry.put("action", "notify");
                entry.put("relationLoads", relationLoads(plan, attachment));
                entry.put("toExpression", plan.toExpression());
                entry.put("subjectExpression", plan.subjectExpression());
                entry.put("bodyExpression", plan.bodyExpression());
                entry.putAll(NotifySupport.attachmentFields(attachment));
                entry.putAll(NotifySupport.deepLinkFields(plan, byName.get(entity)));
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

    /**
     * The first cross-model schedule reference that does not resolve against its owner model, or
     * {@code null} when every reference resolves. Validates the references that read the source row
     * (each {@code where} field, each {@code generate.map} source, and recursively each child
     * {@code forEach.match} source) against the source's properties, and - for a cross-model
     * {@code forEach} collection - its own match-key + child-map references against the collection
     * owner's properties. Skips a source/collection whose owner model was not resolved
     * ({@code propertyNames() == null}, the convention fallback), the same tolerance {@code dependsOn}
     * uses.
     */
    private static String firstUnresolvableScheduleRef(IntentModel model, ScheduleIntent schedule,
            CrossModelSupport.TargetInfo sourceTarget, IntentGenerationContext context) {
        java.util.Set<String> sourceProps = sourceTarget == null ? null : sourceTarget.propertyNames();
        if (sourceProps != null) {
            for (ScheduleConditionIntent condition : schedule.getWhere()) {
                if (isMissing(sourceProps, condition.getField())) {
                    return "where field [" + condition.getField() + "]";
                }
            }
            for (Map.Entry<String, String> mapping : schedule.getGenerate()
                                                             .getMap()
                                                             .entrySet()) {
                if (isMissing(sourceProps, mapping.getValue())) {
                    return "generate map source [" + mapping.getValue() + "]";
                }
            }
        }
        return firstUnresolvableChildRef(model, schedule.getGenerate()
                                                        .getChildren(),
                sourceProps, context);
    }

    /**
     * Recursive helper for {@link #firstUnresolvableScheduleRef}: validates every child's
     * {@code forEach.match} source against the (cross-model) source row, and - for a cross-model
     * {@code forEach} collection - its match-key + this child's {@code map} sources against the
     * collection owner's properties.
     */
    private static String firstUnresolvableChildRef(IntentModel model, List<GenerateChildIntent> children,
            java.util.Set<String> sourceProps, IntentGenerationContext context) {
        if (children == null) {
            return null;
        }
        for (GenerateChildIntent child : children) {
            Object matchObject = child.getForEach()
                                      .get("match");
            if (matchObject instanceof Map) {
                Map<?, ?> match = (Map<?, ?>) matchObject;
                if (sourceProps != null) {
                    for (Map.Entry<?, ?> condition : match.entrySet()) {
                        if (isMissing(sourceProps, String.valueOf(condition.getValue()))) {
                            return "generate child forEach match source [" + condition.getValue() + "]";
                        }
                    }
                }
                Object forEachModel = child.getForEach()
                                           .get("model");
                if (forEachModel != null && !String.valueOf(forEachModel)
                                                   .isBlank()) {
                    String collection = String.valueOf(child.getForEach()
                                                            .get("entity"));
                    UsesIntent collectionUses = findUses(model, String.valueOf(forEachModel));
                    if (collectionUses != null) {
                        CrossModelSupport.TargetInfo collectionTarget;
                        try {
                            collectionTarget = CrossModelSupport.resolve(context, collectionUses, collection);
                        } catch (IntentValidationException ex) {
                            return "forEach collection [" + collection + "] in model [" + forEachModel + "] (" + ex.getMessage() + ")";
                        }
                        java.util.Set<String> collectionProps = collectionTarget.propertyNames();
                        if (collectionProps != null) {
                            for (Map.Entry<?, ?> condition : match.entrySet()) {
                                if (isMissing(collectionProps, String.valueOf(condition.getKey()))) {
                                    return "generate child forEach match field [" + condition.getKey() + "]";
                                }
                            }
                            for (String mapSource : child.getMap()
                                                         .values()) {
                                if (isMissing(collectionProps, mapSource)) {
                                    return "generate child map source [" + mapSource + "]";
                                }
                            }
                        }
                    }
                }
            }
            String nested = firstUnresolvableChildRef(model, child.getChildren(), sourceProps, context);
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }

    /**
     * Whether an authored field reference is absent from a set of (PascalCase) owner property names,
     * applying the same PascalCase mapping the criteria / assignment renderers use.
     */
    private static boolean isMissing(java.util.Set<String> properties, String authoredName) {
        return authoredName != null && !authoredName.isBlank() && !properties.contains(IntentNaming.pascalCase(authoredName));
    }

    /**
     * A cross-model relation resolver for the notify machinery: reads the owner model's facts
     * (perspective / project / property names) through {@link CrossModelSupport} so a
     * {@code relation.field} recipient or placeholder can point at an entity owned by another model.
     * Returns {@code null} for a relation whose {@code uses} alias is not declared; a declared-but-
     * unresolvable owner fails loudly through {@code CrossModelSupport.resolve} (the same "generate the
     * leaf first" contract as generate targets and relation links). A {@code null} context (unit test)
     * yields a no-op lookup so same-model resolution is unaffected.
     */
    /**
     * The same lookup shaped for {@link ProcessAssigneeSupport}, which needs the target's identity
     * property (which login a record maps to) rather than its property names.
     */
    private static ProcessAssigneeSupport.CrossModelLookup assigneeCrossModelLookup(IntentModel model, IntentGenerationContext context) {
        if (context == null) {
            return relation -> null;
        }
        return relation -> {
            UsesIntent uses = findUses(model, relation.getModel());
            if (uses == null) {
                return null;
            }
            CrossModelSupport.TargetInfo target = CrossModelSupport.resolve(context, uses, relation.getTo());
            return new ProcessAssigneeSupport.CrossModelTarget(target.perspectiveName(), uses.resolveProject(), uses.getModel(),
                    target.identityProperty());
        };
    }

    /**
     * One assignee resolver per user task whose {@code assignee} is a relation walk: a
     * {@code JavaDelegate} inserted before the task (by the BPMN generator) that walks the trigger
     * record's to-one relations to the person the task belongs to and publishes their login into the
     * variable the task's {@code flowable:assignee} binds to (see {@link ProcessAssigneeSupport}).
     */
    private static List<Map<String, Object>> buildAssignees(IntentModel model, IntentSettings settings, IntentGenerationContext context) {
        List<Map<String, Object>> assignees = new ArrayList<>();
        for (ProcessAssigneeSupport.Assignee assignee : ProcessAssigneeSupport.assignees(model, assigneeCrossModelLookup(model, context))) {
            if (!settings.shouldGenerate("assignees", assignee.handler())) {
                LOGGER.info("Settings opt-out: keeping existing handler for assignee resolver [{}] (not generated)", assignee.handler());
                continue;
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("process", assignee.process());
            entry.put("step", assignee.step());
            entry.put("handler", assignee.handler());
            entry.put("variable", assignee.variable());
            entry.put("path", assignee.path());
            entry.put("ownerEntity", assignee.ownerEntity());
            entry.put("ownerPerspective", assignee.ownerPerspective());
            entry.put("ownerKeyProperty", assignee.ownerKeyProperty());
            entry.put("ownerKeyAccessor", assignee.ownerKeyAccessor());
            entry.put("firstFkProperty", assignee.firstFkProperty());
            entry.put("identityLocal", assignee.identityLocal());
            entry.put("identityProperty", assignee.identityProperty());
            List<Map<String, Object>> hops = new ArrayList<>();
            for (ProcessAssigneeSupport.Hop hop : assignee.hops()) {
                Map<String, Object> entryHop = new LinkedHashMap<>();
                entryHop.put("local", hop.local());
                entryHop.put("entity", hop.entity());
                entryHop.put("perspective", hop.perspective());
                entryHop.put("nextFkProperty", hop.nextFkProperty());
                entryHop.put("crossModel", hop.crossModel());
                entryHop.put("targetModel", hop.targetModel());
                entryHop.put("targetProject", hop.targetProject());
                hops.add(entryHop);
            }
            entry.put("hops", hops);
            assignees.add(entry);
        }
        return assignees;
    }

    private static NotificationSupport.CrossModelLookup crossModelLookup(IntentModel model, IntentGenerationContext context) {
        if (context == null) {
            return relation -> null;
        }
        return relation -> {
            UsesIntent uses = findUses(model, relation.getModel());
            if (uses == null) {
                return null;
            }
            CrossModelSupport.TargetInfo target = CrossModelSupport.resolve(context, uses, relation.getTo());
            return new NotificationSupport.CrossModelTarget(target.perspectiveName(), uses.resolveProject(), uses.getModel(),
                    target.propertyNames());
        };
    }

    /**
     * Surface a piece of glue that could not be emitted (e.g. an unresolvable notify recipient) both in
     * the log AND as a generate-response issue, so the drop is not silent at the API level (dirigible
     * #6360). The generation itself still succeeds - the issue is a warning, not a 422.
     */
    private static void reportDroppedGlue(IntentGenerationContext context, String message) {
        LOGGER.warn(message);
        if (context != null) {
            context.addIssue(message);
        }
    }

    /**
     * Resolve a notify block's {@code attach: print} against the entity the message is about, reporting
     * the drop when the entity has no printable document shape. The parser rejects that combination up
     * front, so this is the generation-time backstop (the same belt-and-braces the notify recipient
     * gets): a mail must never go out claiming a document it could not render.
     *
     * @param notify the notify block, may be {@code null}
     * @param entity the entity the message is about
     * @param model the parsed model
     * @param context the generation context (to surface the drop as a response issue)
     * @param subject the call site, for the reported message
     * @return the attachment, or {@code null} when none was asked for or it cannot be rendered
     */
    private static NotifySupport.PrintAttachment printAttachment(NotificationIntent notify, EntityIntent entity, IntentModel model,
            Map<String, EntityIntent> byName, Map<String, String> compositionParents, IntentGenerationContext context, String subject) {
        NotifySupport.PrintAttachment attachment;
        try {
            attachment = NotifySupport.printAttachment(notify, entity, model, byName, compositionParents, crossModelLookup(model, context));
        } catch (IllegalArgumentException ex) {
            // A declared languageFrom that does not resolve - report the precise reason and drop.
            reportDroppedGlue(context, subject + " " + ex.getMessage() + " - the mail was NOT generated");
            return null;
        }
        if (attachment == null && NotifySupport.attachesPrint(notify)) {
            reportDroppedGlue(context, subject + " asks to attach the print of [" + (entity == null ? "?" : entity.getName())
                    + "], which is not a document (header + line-items child) and has no print template - NOT generated");
        }
        return attachment;
    }

    /**
     * The relation loads a notify-bearing handler must declare: the ones the message text needs, plus
     * the ones an authored {@code fileName:} pattern reads on top of them. Both sides name their local
     * after the relation, so a relation referenced by both is loaded ONCE - declaring it twice would
     * not compile.
     *
     * @param plan the translated notify block
     * @param attachment the resolved print attachment, or {@code null} for a plain-text message
     * @return the merged loads, message-text ones first
     */
    private static List<Map<String, Object>> relationLoads(NotificationSupport.Plan plan, NotifySupport.PrintAttachment attachment) {
        List<NotificationSupport.RelationLoad> merged = new ArrayList<>(plan.loads());
        if (attachment != null) {
            Set<String> declared = new LinkedHashSet<>();
            for (NotificationSupport.RelationLoad load : merged) {
                declared.add(load.local());
            }
            for (NotificationSupport.RelationLoad load : attachment.fileNameLoads()) {
                if (declared.add(load.local())) {
                    merged.add(load);
                }
            }
        }
        return relationLoads(merged);
    }

    private static List<Map<String, Object>> relationLoads(List<NotificationSupport.RelationLoad> resolved) {
        return NotificationSupport.loadFields(resolved);
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
            // The {error} token (whole-value, parser-enforced) reads the failure message the runtime
            // conversion published instead of assigning a literal. Emitted only when used, so every
            // other setter's descriptor stays byte-identical.
            if (!setter.relation() && ProcessResilienceSupport.ERROR_TOKEN.equals(setter.value()
                                                                                        .trim())) {
                entry.put("errorMessage", "true");
            }
            setters.add(entry);
        }
        return setters;
    }
}
