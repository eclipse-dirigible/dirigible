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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.FormIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.ProcessIntent;
import org.eclipse.dirigible.components.intent.model.StepIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads the {@code editable} fields of a process's user-task forms and turns each user task into a
 * <b>writer</b>: a BPMN service task, inserted just after the user task, that writes the reviewer's
 * edits from the process variables back onto the trigger entity.
 * <p>
 * This is the write-side mirror of {@link ProcessResolverSupport} (which loads entity data
 * <em>into</em> the context) and the variable-valued sibling of {@link SetFieldSupport} (which
 * writes a literal): where a setter assigns a fixed value, a writer assigns the value the form
 * captured into the {@code <Property>} process variable when the task completed. It loads the
 * entity by its PK process variable, assigns each editable field, and persists via the repository's
 * {@code updateWithoutEvent} - a workflow-driven system write that must not re-fire
 * {@code onUpdate} reactions (the same rule the trigger and setters follow).
 * <p>
 * Editable fields may be any plain field of the trigger entity; each carries a
 * {@link WriteField#coercion() coercion} category so the generated Writer converts the form's
 * process variable to the entity's Java type ({@code LocalDate} / {@code Instant} / {@code Integer}
 * / {@code Long} / {@code BigDecimal} / {@code Double} / {@code Boolean} / {@code String}). The
 * form-builder sends date/timestamp values ISO-shaped; a relation.field is never editable.
 */
public final class WriterSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(WriterSupport.class);

    private WriterSupport() {}

    /**
     * One editable field a writer assigns: the PascalCase property (also the process-variable name) and
     * the {@code coercion} category the {@code Writer.java.template} uses to convert the variable to
     * the field's Java type ({@code string} / {@code integer} / {@code long} / {@code decimal} /
     * {@code double} / {@code boolean} / {@code date} / {@code timestamp}).
     */
    public record WriteField(String property, String coercion) {
    }

    /**
     * One writer to generate (one generated {@code JavaDelegate} per user task that has editable
     * fields).
     *
     * @param process the owning process
     * @param userTask the user-task step whose form's edits this writer persists
     * @param className the generated handler class simple name (e.g. {@code LoanApprovalReviewWrite})
     * @param entity the trigger entity written (e.g. {@code Loan})
     * @param perspective the entity's resolved perspective (its gen data subfolder)
     * @param keyProperty the process variable holding the entity's PK (e.g. {@code Id})
     * @param keyAccessor the {@link Number} accessor matching the PK type ({@code intValue} /
     *        {@code longValue})
     * @param fields the editable fields assigned back onto the entity
     */
    public record Writer(String process, String userTask, String className, String entity, String perspective, String keyProperty,
            String keyAccessor, List<WriteField> fields) {
    }

    /**
     * The generated writer class name for a user task: the process plus the step name, both PascalCase
     * ({@code LoanApproval} + {@code review} -> {@code LoanApprovalReviewWrite}), unique across tasks.
     */
    public static String className(String process, String userTask) {
        return IntentNaming.pascalCase(process) + IntentNaming.pascalCase(userTask) + "Write";
    }

    /** Every writer across every process in the model. */
    public static List<Writer> writers(IntentModel model) {
        List<Writer> writers = new ArrayList<>();
        Map<String, EntityIntent> byName = IntentEntities.byName(model);
        Map<String, String> compositionParents = IntentEntities.compositionParents(model);
        Map<String, FormIntent> formsByName = formsByName(model);
        for (ProcessIntent process : model.getProcesses()) {
            String triggerEntity = TriggerSupport.triggerEntity(process);
            EntityIntent owner = triggerEntity == null ? null : byName.get(triggerEntity);
            if (owner == null) {
                continue; // no trigger entity -> no entity instance to write back to
            }
            for (StepIntent step : process.getSteps()) {
                if (!"userTask".equals(step.getKind()) || step.getName() == null) {
                    continue;
                }
                FormIntent form = formsByName.get(stringArg(step, "form"));
                if (form == null || form.getEditable()
                                        .isEmpty()) {
                    continue;
                }
                List<WriteField> fields = editableFields(owner, form, process, step);
                if (fields.isEmpty()) {
                    continue;
                }
                writers.add(new Writer(process.getName(), step.getName(), className(process.getName(), step.getName()), triggerEntity,
                        IntentEntities.resolvePerspective(triggerEntity, compositionParents), IntentEntities.keyFieldName(owner),
                        idAccessor(IntentEntities.primaryKeyOf(owner)), fields));
            }
        }
        return writers;
    }

    private static List<WriteField> editableFields(EntityIntent owner, FormIntent form, ProcessIntent process, StepIntent step) {
        Set<WriteField> fields = new LinkedHashSet<>();
        for (String fieldName : form.getEditable()) {
            if (fieldName == null || fieldName.isBlank()) {
                continue;
            }
            FieldIntent field = fieldOf(owner, fieldName);
            if (field == null) {
                LOGGER.warn("Editable field [{}] on form [{}] (task [{}] of process [{}]) is not a field of [{}] - skipping write-back",
                        fieldName, form.getName(), step.getName(), process.getName(), owner.getName());
                continue;
            }
            fields.add(new WriteField(IntentNaming.pascalCase(fieldName), coercion(field)));
        }
        return new ArrayList<>(fields);
    }

    /**
     * The coercion category for an editable field's type - tells {@code Writer.java.template} how to
     * turn the process variable into the entity's Java field type. string/text/uuid flow straight
     * through; the rest parse from the variable.
     */
    private static String coercion(FieldIntent field) {
        String type = field.getType() == null ? "string"
                : field.getType()
                       .toLowerCase(java.util.Locale.ROOT);
        switch (type) {
            case "integer":
            case "int":
                return "integer";
            case "long":
                return "long";
            case "decimal":
                return "decimal";
            case "double":
                return "double";
            case "boolean":
                return "boolean";
            case "date":
                return "date";
            case "timestamp":
                return "timestamp";
            default:
                return "string"; // string / text / uuid
        }
    }

    private static Map<String, FormIntent> formsByName(IntentModel model) {
        Map<String, FormIntent> index = new HashMap<>();
        for (FormIntent form : model.getForms()) {
            if (form.getName() != null) {
                index.put(form.getName(), form);
            }
        }
        return index;
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
