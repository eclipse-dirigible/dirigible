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
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.NotificationIntent;
import org.eclipse.dirigible.components.intent.model.ProcessIntent;
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

        if (triggers.isEmpty() && resolvers.isEmpty() && notifications.isEmpty()) {
            // No process glue for this intent - any stale .glue is removed by the post-pass scrub.
            return;
        }

        Map<String, Object> glue = new LinkedHashMap<>();
        glue.put("triggers", triggers);
        glue.put("resolvers", resolvers);
        glue.put("notifications", notifications);
        context.writeModelFile(IntentNaming.baseName(context) + ".glue", JsonHelper.toJson(glue));
        LOGGER.debug("Wrote glue with [{}] trigger(s), [{}] resolver(s) and [{}] notification(s)", triggers.size(), resolvers.size(),
                notifications.size());
    }

    private static List<Map<String, Object>> buildTriggers(IntentModel model, Map<String, EntityIntent> byName,
            Map<String, String> compositionParents, IntentSettings settings) {
        List<Map<String, Object>> triggers = new ArrayList<>();
        for (ProcessIntent process : model.getProcesses()) {
            if (process.getName() == null || process.getName()
                                                    .isBlank()) {
                continue;
            }
            String entity = TriggerSupport.onCreateEntity(process);
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
            Object when = notification.getEvent()
                                      .get("when");
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", notification.getName());
            entry.put("className", IntentNaming.pascalCase(notification.getName()));
            entry.put("entity", entity);
            entry.put("perspective", IntentEntities.resolvePerspective(entity, compositionParents));
            entry.put("topicSuffix", NotificationSupport.topicSuffix(NotificationSupport.eventKind(notification)));
            entry.put("guardExpression", NotificationSupport.guard(when == null ? null : when.toString()));
            entry.put("toExpression", NotificationSupport.recipientExpression(notification.getTo()));
            entry.put("subjectExpression", NotificationSupport.interpolate(notification.getSubject()));
            entry.put("bodyExpression", NotificationSupport.interpolate(notification.getBody()));
            notifications.add(entry);
        }
        return notifications;
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
