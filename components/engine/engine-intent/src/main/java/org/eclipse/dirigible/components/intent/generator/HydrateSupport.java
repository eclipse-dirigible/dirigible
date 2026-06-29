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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.ProcessIntent;
import org.eclipse.dirigible.components.intent.model.StepIntent;

/**
 * Reads the user tasks of a process and turns each process into a <b>hydrator</b>: a BPMN service
 * task, inserted just before every user task, that re-loads the process's trigger entity by its
 * process-variable id and re-publishes <b>all of its own scalar fields</b> as process variables.
 * <p>
 * This is the read-side counterpart of {@link SetFieldSupport} (which writes one literal field) and
 * the generalisation of {@link ProcessResolverSupport} (which loads a single related field): where
 * a resolver freshens one {@code relation.field}, a hydrator freshens the trigger entity's
 * <em>own</em> fields. The process is started with a snapshot of the entity (the trigger passes the
 * entity JSON to {@code Process.start}); if the entity is edited between process start and the
 * moment a user task is created, that snapshot goes stale. The hydrator runs at exactly the moment
 * the task is created - which is the moment the user must decide on the data - so the task form
 * (whose model is the process variables) shows the current values (e.g. a freshly set
 * {@code DueOn}, a recomputed document {@code Total}) instead of the start-time snapshot.
 * <p>
 * FK ids and {@code relation.field} values are out of scope here: the FK columns stay in the
 * process context for the existing {@link ProcessResolverSupport} resolvers to read, which already
 * load the related entity live. A hydrator only refreshes the trigger entity's directly-persisted
 * fields.
 * <p>
 * Scope: one hydrator per process that has at least one {@code userTask} and a trigger entity. A
 * process with no user task (or no trigger entity to load by id) produces none.
 */
public final class HydrateSupport {

    private HydrateSupport() {}

    /**
     * One hydrator to generate (one generated {@code JavaDelegate} per process).
     *
     * @param process the owning process
     * @param className the generated handler class simple name (e.g. {@code LoanApprovalHydrate})
     * @param entity the trigger entity reloaded and published (e.g. {@code Loan})
     * @param perspective the entity's resolved perspective (its gen data subfolder)
     * @param keyProperty the process variable holding the entity's PK (e.g. {@code Id})
     * @param keyAccessor the {@link Number} accessor matching the PK type ({@code intValue} /
     *        {@code longValue})
     * @param fields the PascalCase names of the entity's own fields re-published as process variables
     */
    public record Hydrator(String process, String className, String entity, String perspective, String keyProperty, String keyAccessor,
            List<String> fields) {
    }

    /**
     * The generated hydrator class name for a process ({@code LoanApproval} ->
     * {@code LoanApprovalHydrate}).
     */
    public static String className(String process) {
        return IntentNaming.pascalCase(process) + "Hydrate";
    }

    /** Every hydrator across every process in the model. */
    public static List<Hydrator> hydrators(IntentModel model) {
        List<Hydrator> hydrators = new ArrayList<>();
        Map<String, EntityIntent> byName = IntentEntities.byName(model);
        Map<String, String> compositionParents = IntentEntities.compositionParents(model);
        for (ProcessIntent process : model.getProcesses()) {
            if (process.getName() == null || process.getName()
                                                    .isBlank()) {
                continue;
            }
            String triggerEntity = TriggerSupport.triggerEntity(process);
            EntityIntent owner = triggerEntity == null ? null : byName.get(triggerEntity);
            if (owner == null || !hasUserTask(process)) {
                continue; // nothing to load by id, or no task whose form would show the data
            }
            hydrators.add(new Hydrator(process.getName(), className(process.getName()), triggerEntity,
                    IntentEntities.resolvePerspective(triggerEntity, compositionParents), IntentEntities.keyFieldName(owner),
                    idAccessor(IntentEntities.primaryKeyOf(owner)), ownFieldProperties(owner)));
        }
        return hydrators;
    }

    private static boolean hasUserTask(ProcessIntent process) {
        for (StepIntent step : process.getSteps()) {
            if ("userTask".equals(step.getKind())) {
                return true;
            }
        }
        return false;
    }

    /** The PascalCase property names of the entity's own fields, deduped, in declaration order. */
    private static List<String> ownFieldProperties(EntityIntent owner) {
        Set<String> properties = new LinkedHashSet<>();
        for (FieldIntent field : owner.getFields()) {
            if (field.getName() != null && !field.getName()
                                                 .isBlank()) {
                properties.add(IntentNaming.pascalCase(field.getName()));
            }
        }
        return new ArrayList<>(properties);
    }

    private static String idAccessor(FieldIntent pk) {
        String type = pk == null || pk.getType() == null ? "integer" : pk.getType();
        return "long".equals(type) ? "longValue" : "intValue";
    }
}
