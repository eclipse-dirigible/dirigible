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
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.ProcessIntent;
import org.eclipse.dirigible.components.intent.model.StepIntent;

/**
 * Boundary timers on a user task: {@code timeout: { after: P3D, then: remind }} is a
 * <b>non-cancelling</b> reminder/escalation (the task stays), {@code expire: { until: validUntil,
 * then: markExpired }} is a <b>cancelling</b>, date-field-driven expiry (the task is withdrawn).
 * The BPMN generator emits the {@code <boundaryEvent>} + {@code timerEventDefinition}; this support
 * derives the <b>expire date loader</b> - a {@code JavaDelegate} inserted just before the user task
 * that re-reads the trigger entity's date field at task entry (so an edited {@code validUntil}
 * mid-flow is honored) and publishes it as the process variable the boundary timer's
 * {@code timeDate} binds to. The loaded value is a {@code java.util.Date} (Flowable arms the timer
 * from it directly, no string parsing): a {@code date} field expires at the start of the day
 * <i>after</i> it (the field names the last valid day), a {@code timestamp} at its instant, and a
 * {@code null} arms a far-future date so the timer never effectively fires.
 */
public final class ProcessTimerSupport {

    /** The far-future due date a {@code null} expiry field arms (the timer never fires in practice). */
    private static final String FAR_FUTURE = "java.util.Date.from(java.time.Instant.parse(\"9999-12-31T00:00:00Z\"))";

    private ProcessTimerSupport() {}

    /**
     * One expire date loader to generate (one per user task with an {@code expire: { until: ... }}).
     *
     * @param process the owning process
     * @param beforeStep the user task before which the loader is inserted
     * @param handler the generated handler class simple name (e.g. {@code LoadClaimConfirmExpire})
     * @param ownerEntity the trigger entity loaded by id
     * @param ownerPerspective the trigger entity's resolved perspective (its gen data subfolder)
     * @param ownerKeyProperty the process variable holding the entity's PK (e.g. {@code Id})
     * @param ownerKeyAccessor the {@link Number} accessor matching the PK type
     * @param variable the process variable the boundary timer's {@code timeDate} binds to
     * @param dueExpression the pre-rendered Java expression producing the {@code java.util.Date}
     */
    public record TimerLoad(String process, String beforeStep, String handler, String ownerEntity, String ownerPerspective,
            String ownerKeyProperty, String ownerKeyAccessor, String variable, String dueExpression) {
    }

    /** Every expire date loader across every process in the model. */
    public static List<TimerLoad> timerLoads(IntentModel model) {
        List<TimerLoad> loads = new ArrayList<>();
        Map<String, EntityIntent> byName = IntentEntities.byName(model);
        Map<String, String> compositionParents = IntentEntities.compositionParents(model);
        for (ProcessIntent process : model.getProcesses()) {
            String triggerEntity = TriggerSupport.triggerEntity(process);
            EntityIntent owner = triggerEntity == null ? null : byName.get(triggerEntity);
            if (process.getName() == null || owner == null) {
                continue; // no trigger entity -> no date field to load
            }
            for (StepIntent step : process.getSteps()) {
                if (!"userTask".equals(step.getKind()) || step.getName() == null) {
                    continue;
                }
                String until = timerAttribute(step, "expire", "until");
                FieldIntent field = until == null ? null : fieldByName(owner, until);
                if (field == null) {
                    continue; // no expire timer, or parser-reported unknown field
                }
                loads.add(new TimerLoad(process.getName(), step.getName(),
                        "Load" + IntentNaming.pascalCase(process.getName()) + IntentNaming.pascalCase(step.getName()) + "Expire",
                        triggerEntity, IntentEntities.resolvePerspective(triggerEntity, compositionParents),
                        IntentEntities.keyFieldName(owner), idAccessor(IntentEntities.primaryKeyOf(owner)), expireVariable(step.getName()),
                        dueExpression(IntentNaming.pascalCase(field.getName()), field.getType())));
            }
        }
        return loads;
    }

    /** The process variable an expire boundary timer's {@code timeDate} binds to. */
    public static String expireVariable(String taskName) {
        return "__" + taskName + "ExpireDate";
    }

    /**
     * A nested attribute of a user task's {@code timeout:}/{@code expire:} map arg, or {@code null}
     * when the timer (or the attribute) is not declared or the arg is not a map.
     */
    public static String timerAttribute(StepIntent step, String timer, String attribute) {
        Object raw = step.getArgs() == null ? null
                : step.getArgs()
                      .get(timer);
        if (!(raw instanceof Map<?, ?> map)) {
            return null;
        }
        Object value = map.get(attribute);
        return value == null ? null : value.toString();
    }

    /**
     * The pre-rendered Java expression turning the entity's date field into the timer's
     * {@code java.util.Date} due value (the expansions convention - the template stays shape-only).
     */
    private static String dueExpression(String property, String type) {
        String value = "timestamp".equals(type) ? "java.util.Date.from(entity." + property + ")"
                : "java.util.Date.from(entity." + property + ".plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())";
        return "entity." + property + " == null ? " + FAR_FUTURE + " : " + value;
    }

    private static FieldIntent fieldByName(EntityIntent entity, String name) {
        for (FieldIntent field : entity.getFields()) {
            if (name.equals(field.getName())) {
                return field;
            }
        }
        return null;
    }

    private static String idAccessor(FieldIntent pk) {
        String type = pk == null || pk.getType() == null ? "integer" : pk.getType();
        return "long".equals(type) ? "longValue" : "intValue";
    }
}
