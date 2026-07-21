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
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.ProcessIntent;
import org.eclipse.dirigible.components.intent.model.RelationIntent;

/**
 * {@code abortOn: { status: [ids] | id, then: <step> }} on a process cancels the in-flight instance
 * when the trigger entity transitions into one of the listed EntityStatus seed ids - closing the
 * orphaned-Inbox-task hole a {@code transitions:} void/cancel opens. Emitted as an <b>interrupting
 * message event subprocess</b> (a {@code triggeredByEvent} subprocess whose interrupting message
 * start event fires the {@code <Process>Abort} message and runs to a terminate end event), so
 * whatever is in flight - user tasks, parked waits, armed boundary timers - is killed at once,
 * without wrapping the main flow. The correlating glue is a {@code MessageHandler} on the entity's
 * {@code -transitioned} topic that matches the status list and correlates on the stamped
 * {@code ProcessId} (fail-soft: no parked instance is a no-op).
 * <p>
 * An optional {@code then} names a declared {@code serviceTask} cleanup (a {@code setField} /
 * {@code setRelationField}) run inside the abort handler before terminating; that step is
 * <b>abort-only</b> - excluded from the main flow.
 */
public final class ProcessAbortSupport {

    private ProcessAbortSupport() {}

    /**
     * One abort listener to generate (one per process declaring {@code abortOn}).
     *
     * @param process the owning process
     * @param entity the trigger entity whose transition aborts the flow
     * @param perspective the entity's resolved perspective (its gen data subfolder + the topic name)
     * @param messageName the BPMN message the event subprocess subscribes to and the listener fires
     * @param statusProperty the PascalCase EntityStatus FK property read to match the abort statuses
     * @param statuses the EntityStatus seed ids any of which aborts the flow
     */
    public record Abort(String process, String entity, String perspective, String messageName, String statusProperty,
            List<Integer> statuses) {
    }

    /** Every abort listener across every process in the model. */
    public static List<Abort> aborts(IntentModel model) {
        List<Abort> aborts = new ArrayList<>();
        Map<String, EntityIntent> byName = IntentEntities.byName(model);
        Map<String, String> compositionParents = IntentEntities.compositionParents(model);
        for (ProcessIntent process : model.getProcesses()) {
            List<Integer> statuses = statuses(process);
            String entity = TriggerSupport.triggerEntity(process);
            if (process.getName() == null || statuses.isEmpty() || entity == null || !byName.containsKey(entity)) {
                continue; // no abortOn, or parser-reported; skip so no dangling glue is emitted
            }
            String statusProperty = entityStatusProperty(byName.get(entity));
            if (statusProperty == null) {
                continue; // parser-reported: abortOn needs an EntityStatus relation to match on
            }
            aborts.add(new Abort(process.getName(), entity, IntentEntities.resolvePerspective(entity, compositionParents),
                    messageName(process.getName()), statusProperty, statuses));
        }
        return aborts;
    }

    /** The BPMN message an abort event subprocess subscribes to. */
    public static String messageName(String process) {
        return IntentNaming.pascalCase(process) + "Abort";
    }

    /** The abort statuses declared on a process ({@code status:} a list or a bare scalar), or empty. */
    public static List<Integer> statuses(ProcessIntent process) {
        List<Integer> statuses = new ArrayList<>();
        Object raw = process.getAbortOn() == null ? null
                : process.getAbortOn()
                         .get("status");
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                Integer id = asInt(item);
                if (id != null) {
                    statuses.add(id);
                }
            }
        } else {
            Integer id = asInt(raw);
            if (id != null) {
                statuses.add(id);
            }
        }
        return statuses;
    }

    /**
     * The abort-only cleanup step named by {@code then}, or null (omitted or the literal {@code end}).
     */
    public static String thenStep(ProcessIntent process) {
        Object raw = process.getAbortOn() == null ? null
                : process.getAbortOn()
                         .get("then");
        if (raw == null) {
            return null;
        }
        String then = raw.toString()
                         .trim();
        return then.isEmpty() || "end".equalsIgnoreCase(then) ? null : then;
    }

    /** The PascalCase FK property of the entity's {@code function: EntityStatus} relation, or null. */
    static String entityStatusProperty(EntityIntent entity) {
        if (entity == null || entity.getRelations() == null) {
            return null;
        }
        for (RelationIntent relation : entity.getRelations()) {
            if (relation.isEntityStatus() && relation.getName() != null) {
                return IntentNaming.pascalCase(relation.getName());
            }
        }
        return null;
    }

    private static Integer asInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null && value.toString()
                                  .trim()
                                  .matches("-?\\d+")) {
            return Integer.valueOf(value.toString()
                                        .trim());
        }
        return null;
    }
}
