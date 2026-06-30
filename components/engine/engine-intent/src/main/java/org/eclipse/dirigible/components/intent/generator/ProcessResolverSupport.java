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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.FormIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.ProcessIntent;
import org.eclipse.dirigible.components.intent.model.RelationIntent;
import org.eclipse.dirigible.components.intent.model.StepIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads the {@code relation.field} references in a process's <b>decision conditions</b> and
 * <b>user-task forms</b> and turns each into a <b>resolver</b>: a step inserted just before the
 * first step that needs it, which loads the related (to-one) entity by its FK id and exposes the
 * wanted field as a process variable.
 * <p>
 * A decision authored as {@code if: "book.price > 500"} on a {@code LoanApproval} triggered by
 * {@code onCreate: Loan} resolves {@code book} (a to-one relation on {@code Loan}) to {@code Book},
 * reads {@code price}, and publishes it as the {@code book_price} variable; the condition is then
 * rewritten to {@code book_price > 500}. A user-task form that lists {@code book.price} among its
 * {@code fields} drives the same resolver, and the form control binds to the {@code book_price}
 * variable so the value is shown to the reviewer (this is why the form needs the resolver: the form
 * model is the process variables, which carry the {@code Book} FK id but not the book's own
 * fields).
 * <p>
 * A resolver is anchored at the <b>earliest</b> step that references it (the steps are scanned in
 * declaration order and each {@code relation.field} is recorded once per process). The variable it
 * sets persists for the rest of the process, so a single resolver before the first user-task form
 * also serves a later decision on the same path - which is the whole point: fetching it before the
 * form rather than only before the decision is what makes the form field non-empty.
 * <p>
 * Scope: one-hop to-one relations of the process's trigger entity (the entity whose fields seed the
 * process variables). Cross-process / multi-hop paths are out of scope and ignored with a warning.
 */
public final class ProcessResolverSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessResolverSupport.class);

    /** A {@code relation.field} token: a relation name, a dot, a field name. */
    private static final Pattern RELATION_FIELD = Pattern.compile("\\b([A-Za-z_][A-Za-z0-9_]*)\\.([A-Za-z_][A-Za-z0-9_]*)\\b");

    private ProcessResolverSupport() {}

    /**
     * One resolver to generate.
     *
     * @param process the owning process
     * @param beforeStep the step before which the resolver task is inserted (the earliest decision or
     *        user-task that referenced the path)
     * @param token the authored {@code relation.field} text (e.g. {@code book.price})
     * @param variable the process variable the resolver sets (e.g. {@code book_price})
     * @param handler the generated handler class simple name (e.g. {@code ResolveBookPrice})
     * @param fkProperty the process variable holding the FK id (e.g. {@code Book})
     * @param targetEntity the related entity (e.g. {@code Book})
     * @param targetField the PascalCase field read off it (e.g. {@code Price})
     * @param targetPerspective the related entity's resolved perspective (its gen data subfolder)
     * @param targetIdAccessor the {@link Number} accessor matching the target PK type ({@code intValue}
     *        / {@code longValue})
     */
    public record Resolver(String process, String beforeStep, String token, String variable, String handler, String fkProperty,
            String targetEntity, String targetField, String targetPerspective, String targetIdAccessor, String ownerEntity,
            String ownerPerspective, String ownerKeyProperty, String ownerKeyAccessor) {
    }

    /** Every resolver across every process in the model. */
    public static List<Resolver> resolvers(IntentModel model) {
        List<Resolver> resolvers = new ArrayList<>();
        Map<String, EntityIntent> byName = IntentEntities.byName(model);
        Map<String, String> compositionParents = IntentEntities.compositionParents(model);
        Map<String, FormIntent> formsByName = formsByName(model);
        Set<String> seen = new LinkedHashSet<>();
        for (ProcessIntent process : model.getProcesses()) {
            String triggerEntity = TriggerSupport.triggerEntity(process);
            EntityIntent owner = triggerEntity == null ? null : byName.get(triggerEntity);
            if (owner == null) {
                continue; // no trigger entity -> no process-variable context to resolve against
            }
            // Scan steps in declaration order so each relation.field is anchored at the FIRST step that
            // needs it (the `seen` set keeps the earliest); decision conditions and user-task forms both
            // contribute, and the variable persists across the process so one resolver serves both.
            for (StepIntent step : process.getSteps()) {
                if ("decision".equals(step.getKind())) {
                    String condition = stringArg(step, "if");
                    if (condition != null) {
                        collectFromCondition(byName, compositionParents, process, owner, step, condition, resolvers, seen);
                    }
                } else if ("userTask".equals(step.getKind())) {
                    FormIntent form = formsByName.get(stringArg(step, "form"));
                    if (form != null) {
                        collectFromForm(byName, compositionParents, process, owner, step, form, resolvers, seen);
                    }
                }
            }
        }
        return resolvers;
    }

    private static Map<String, FormIntent> formsByName(IntentModel model) {
        Map<String, FormIntent> index = new java.util.HashMap<>();
        for (FormIntent form : model.getForms()) {
            if (form.getName() != null) {
                index.put(form.getName(), form);
            }
        }
        return index;
    }

    private static void collectFromCondition(Map<String, EntityIntent> byName, Map<String, String> compositionParents,
            ProcessIntent process, EntityIntent owner, StepIntent step, String condition, List<Resolver> resolvers, Set<String> seen) {
        Matcher matcher = RELATION_FIELD.matcher(condition);
        while (matcher.find()) {
            addResolver(byName, compositionParents, process, owner, step, matcher.group(1), matcher.group(2), "decision", resolvers, seen);
        }
    }

    private static void collectFromForm(Map<String, EntityIntent> byName, Map<String, String> compositionParents, ProcessIntent process,
            EntityIntent owner, StepIntent step, FormIntent form, List<Resolver> resolvers, Set<String> seen) {
        for (String field : form.getFields()) {
            if (field == null) {
                continue;
            }
            int dot = field.indexOf('.');
            if (dot <= 0 || dot >= field.length() - 1) {
                continue; // a plain field of the bound entity - no resolver needed
            }
            addResolver(byName, compositionParents, process, owner, step, field.substring(0, dot), field.substring(dot + 1),
                    "user-task form", resolvers, seen);
        }
    }

    private static void addResolver(Map<String, EntityIntent> byName, Map<String, String> compositionParents, ProcessIntent process,
            EntityIntent owner, StepIntent step, String relationName, String fieldName, String origin, List<Resolver> resolvers,
            Set<String> seen) {
        RelationIntent relation = toOneRelation(owner, relationName);
        if (relation == null) {
            return; // not a to-one relation of the trigger entity - leave the token alone
        }
        EntityIntent target = byName.get(relation.getTo());
        if (target == null || fieldOf(target, fieldName) == null) {
            LOGGER.warn("{} [{}] in process [{}] references [{}.{}] but [{}] has no field [{}] - skipping resolver", origin, step.getName(),
                    process.getName(), relationName, fieldName, relation.getTo(), fieldName);
            return;
        }
        String handler = "Resolve" + IntentNaming.pascalCase(relationName) + IntentNaming.pascalCase(fieldName);
        if (!seen.add(process.getName() + "/" + handler)) {
            return; // same resolution already anchored at an earlier step in this process
        }
        resolvers.add(new Resolver(process.getName(), step.getName(), relationName + "." + fieldName, relationName + "_" + fieldName,
                handler, IntentNaming.pascalCase(relationName), relation.getTo(), IntentNaming.pascalCase(fieldName),
                IntentEntities.resolvePerspective(relation.getTo(), compositionParents), idAccessor(IntentEntities.primaryKeyOf(target)),
                owner.getName(), IntentEntities.resolvePerspective(owner.getName(), compositionParents), IntentEntities.keyFieldName(owner),
                idAccessor(IntentEntities.primaryKeyOf(owner))));
    }

    private static RelationIntent toOneRelation(EntityIntent owner, String name) {
        for (RelationIntent relation : owner.getRelations()) {
            boolean toOne = "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
            if (toOne && name.equals(relation.getName())) {
                return relation;
            }
        }
        return null;
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
        Object value = step.getArgs()
                           .get(key);
        return value == null ? null : value.toString();
    }
}
