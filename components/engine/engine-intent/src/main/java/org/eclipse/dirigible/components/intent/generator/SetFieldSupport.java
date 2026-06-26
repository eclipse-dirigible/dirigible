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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads the {@code setField} service tasks of a process and turns each into a <b>field setter</b>:
 * a BPMN service task that sets a field of the process's trigger entity to a fixed value and
 * persists it - generated as a {@code JavaDelegate} glue class instead of a hand-written
 * {@code custom/} stub.
 * <p>
 * A step authored as {@code { kind: serviceTask, args: { setField: status, value: ACTIVE } }} on a
 * {@code MemberApproval} process triggered by {@code onCreate: Member} loads the {@code Member} by
 * its process-variable id, assigns {@code status = "ACTIVE"}, and saves it via the repository's
 * {@code updateWithoutEvent} (a system write that must not re-fire {@code onUpdate} reactions). Two
 * such steps on the two branches of an approve/reject decision are how "Approve -> ACTIVE, Reject
 * -> REJECTED" is expressed as glue, with no custom Java.
 * <p>
 * Scope: the value is a literal assigned to a {@code string}/{@code text} field of the trigger
 * entity (the entity whose fields seed the process variables). Non-string fields and
 * expression-valued sets are out of scope for now (validated by the parser).
 */
public final class SetFieldSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetFieldSupport.class);

    private SetFieldSupport() {}

    /**
     * One field setter to generate.
     *
     * @param process the owning process
     * @param step the {@code serviceTask} step that declared the {@code setField}
     * @param className the generated handler class simple name (e.g. {@code MemberApprovalActivate})
     * @param entity the trigger entity whose field is set (e.g. {@code Member})
     * @param perspective the entity's resolved perspective (its gen data subfolder)
     * @param keyProperty the process variable holding the entity's PK (e.g. {@code Id})
     * @param keyAccessor the {@link Number} accessor matching the PK type ({@code intValue} /
     *        {@code longValue})
     * @param field the PascalCase field assigned (e.g. {@code Status})
     * @param value the literal value assigned (e.g. {@code ACTIVE})
     */
    public record Setter(String process, String step, String className, String entity, String perspective, String keyProperty,
            String keyAccessor, String field, String value) {
    }

    /** Every field setter across every process in the model. */
    public static List<Setter> setters(IntentModel model) {
        List<Setter> setters = new ArrayList<>();
        Map<String, EntityIntent> byName = IntentEntities.byName(model);
        Map<String, String> compositionParents = IntentEntities.compositionParents(model);
        for (ProcessIntent process : model.getProcesses()) {
            String triggerEntity = TriggerSupport.triggerEntity(process);
            EntityIntent owner = triggerEntity == null ? null : byName.get(triggerEntity);
            if (owner == null) {
                continue; // no trigger entity -> no entity instance to set a field on
            }
            for (StepIntent step : process.getSteps()) {
                if (!"serviceTask".equals(step.getKind())) {
                    continue;
                }
                String field = stringArg(step, "setField");
                String value = stringArg(step, "value");
                if (field == null || field.isBlank()) {
                    continue;
                }
                if (fieldOf(owner, field) == null) {
                    LOGGER.warn("Step [{}] in process [{}] sets unknown field [{}] of [{}] - skipping", step.getName(), process.getName(),
                            field, triggerEntity);
                    continue;
                }
                setters.add(new Setter(process.getName(), step.getName(), className(process.getName(), step.getName()), triggerEntity,
                        IntentEntities.resolvePerspective(triggerEntity, compositionParents), IntentEntities.keyFieldName(owner),
                        idAccessor(IntentEntities.primaryKeyOf(owner)), IntentNaming.pascalCase(field), value == null ? "" : value));
            }
        }
        return setters;
    }

    /**
     * The generated handler class name for a {@code setField} step: the process name plus the step
     * name, both PascalCase ({@code MemberApproval} + {@code activate} ->
     * {@code MemberApprovalActivate} ), so it is unique across processes that reuse a step name.
     */
    public static String className(String process, String step) {
        return IntentNaming.pascalCase(process) + IntentNaming.pascalCase(step);
    }

    private static FieldIntent fieldOf(EntityIntent entity, String name) {
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

    private static String stringArg(StepIntent step, String key) {
        Map<String, Object> args = step.getArgs();
        if (args == null) {
            return null;
        }
        Object value = args.get(key);
        return value == null ? null : value.toString();
    }
}
