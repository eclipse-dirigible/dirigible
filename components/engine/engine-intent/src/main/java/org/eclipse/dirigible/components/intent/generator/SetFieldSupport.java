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
import org.eclipse.dirigible.components.intent.model.RelationIntent;
import org.eclipse.dirigible.components.intent.model.StepIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads the {@code setField} / {@code setRelationField} steps of a process and turns each into a
 * <b>field setter</b>: a BPMN service task that sets a field of the process's trigger entity to a
 * fixed value and persists it - generated as a {@code JavaDelegate} glue class instead of a
 * hand-written {@code custom/} stub.
 * <p>
 * A step authored as {@code { kind: serviceTask, args: { setField: status, value: ACTIVE } }} on a
 * {@code MemberApproval} process triggered by {@code onCreate: Member} loads the {@code Member} by
 * its process-variable id, assigns {@code status = "ACTIVE"}, and saves it via the repository's
 * {@code updateWithoutEvent} (a system write that must not re-fire {@code onUpdate} reactions). Two
 * such steps on the two branches of an approve/reject decision are how "Approve -> ACTIVE, Reject
 * -> REJECTED" is expressed as glue, with no custom Java.
 * <p>
 * {@code setRelationField: Status, value: 2} is the generic counterpart for a status modelled as a
 * to-one relation (an FK to a settings/nomenclature entity): it assigns the relation's FK property
 * to the integer seed id, unquoted. It is allowed on a {@code serviceTask} (bound directly) or a
 * {@code userTask} (the BPMN runs the setter right after the task completes, like the writer).
 * <p>
 * Scope: {@code setField} assigns a literal to a {@code string}/{@code text} field; an expression
 * value is out of scope. {@code setRelationField} assigns an integer seed id to a
 * {@code manyToOne}/ {@code oneToOne} relation's FK (validated by the parser).
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
     * @param field the PascalCase property assigned (a string field for {@code setField}, or a to-one
     *        relation's FK property for {@code setRelationField})
     * @param value the value assigned (a string literal, or a seed id for a relation FK)
     * @param relation {@code true} for a {@code setRelationField} (assign the FK to the integer
     *        {@code value}, unquoted); {@code false} for a {@code setField} (assign the quoted literal)
     */
    public record Setter(String process, String step, String className, String entity, String perspective, String keyProperty,
            String keyAccessor, String field, String value, boolean relation) {
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
            String perspective = IntentEntities.resolvePerspective(triggerEntity, compositionParents);
            String keyProp = IntentEntities.keyFieldName(owner);
            String keyAcc = idAccessor(IntentEntities.primaryKeyOf(owner));
            for (StepIntent step : process.getSteps()) {
                if (step.getName() == null) {
                    continue;
                }
                // setRelationField: set a to-one relation's FK to a seed id (an integer). Allowed on a
                // serviceTask or a userTask (the BPMN runs the setter right after the user task completes).
                String relField = stringArg(step, "setRelationField");
                if (relField != null && !relField.isBlank()) {
                    if (relationOf(owner, relField) == null) {
                        LOGGER.warn("Step [{}] in process [{}] sets unknown to-one relation [{}] of [{}] - skipping", step.getName(),
                                process.getName(), relField, triggerEntity);
                        continue;
                    }
                    String value = stringArg(step, "value");
                    setters.add(new Setter(process.getName(), step.getName(), className(process.getName(), step.getName()), triggerEntity,
                            perspective, keyProp, keyAcc, IntentNaming.pascalCase(relField), value == null ? "" : value, true));
                    continue;
                }
                // setField: set a string/text field to a literal value (serviceTask only).
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
                        perspective, keyProp, keyAcc, IntentNaming.pascalCase(field), value == null ? "" : value, false));
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

    /** The named to-one relation of the entity (the FK whose id a {@code setRelationField} assigns). */
    private static RelationIntent relationOf(EntityIntent entity, String name) {
        for (RelationIntent rel : entity.getRelations()) {
            if (name.equals(rel.getName()) && ("manyToOne".equals(rel.getKind()) || "oneToOne".equals(rel.getKind()))) {
                return rel;
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
