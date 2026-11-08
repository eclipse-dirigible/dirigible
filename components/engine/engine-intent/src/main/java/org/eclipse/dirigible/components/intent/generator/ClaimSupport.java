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
 * Reads the {@code onClaim} / {@code onUnclaim} args of a {@code userTask} (#8461) and turns each
 * task declaring them into a <b>claim listener</b>: a Flowable {@code TaskListener} on the task's
 * {@code assignment} event that moves the process's trigger record into one status while the task
 * is claimed and back when it is unclaimed.
 * <p>
 * {@code { onClaim: { setRelationField: Status, value: SUBMITTED }, onUnclaim: { setRelationField:
 * Status, value: DRAFT } }} on a create-triggered approval puts the document in SUBMITTED the
 * moment the approver claims the task, so a {@code forbidWhen: "Order.Status != DRAFT"} on its
 * lines freezes them while the approver decides, and an unclaim hands the document back to its
 * author. The write is the same targeted {@code updateProperty} a {@code setRelationField} step
 * makes, so the record's {@code lifecycle:} judges it and a move the lifecycle refuses fails the
 * claim.
 * <p>
 * Scope: one to-one relation of the trigger record, written with a seed id (the parser enforces
 * it); both halves are declared together.
 */
public final class ClaimSupport {

    /** The user-task arg naming the write a claim makes. */
    public static final String ON_CLAIM = "onClaim";

    /** The user-task arg naming the write an unclaim makes. */
    public static final String ON_UNCLAIM = "onUnclaim";

    private ClaimSupport() {}

    /**
     * One claim listener to generate.
     *
     * @param process the owning process
     * @param step the {@code userTask} declaring {@code onClaim} / {@code onUnclaim}
     * @param className the generated listener class simple name ({@code <Process><Step>ClaimListener})
     * @param entity the trigger entity whose relation is written
     * @param perspective the entity's resolved perspective (its gen data subfolder)
     * @param keyProperty the process variable holding the entity's PK
     * @param keyAccessor the {@link Number} accessor matching the PK type
     * @param field the PascalCase to-one relation FK property written
     * @param claimValue the seed id written when the task is claimed
     * @param unclaimValue the seed id written when the task is unclaimed
     */
    public record Claim(String process, String step, String className, String entity, String perspective, String keyProperty,
            String keyAccessor, String field, String claimValue, String unclaimValue) {
    }

    /**
     * Every claim listener across every process in the model. A task whose declaration does not resolve
     * (no trigger entity, a relation the trigger does not declare, a half missing) is skipped - the
     * parser has already named it.
     *
     * @param model the intent
     * @return the claim listeners, in declaration order
     */
    public static List<Claim> claims(IntentModel model) {
        List<Claim> claims = new ArrayList<>();
        Map<String, EntityIntent> byName = IntentEntities.byName(model);
        Map<String, String> compositionParents = IntentEntities.compositionParents(model);
        for (ProcessIntent process : model.getProcesses()) {
            String triggerEntity = TriggerSupport.triggerEntity(process);
            EntityIntent owner = triggerEntity == null ? null : byName.get(triggerEntity);
            if (owner == null) {
                continue;
            }
            for (StepIntent step : process.getSteps()) {
                if (step.getName() == null || (step.getKind() != null && !"userTask".equals(step.getKind()))) {
                    continue;
                }
                Map<?, ?> onClaim = write(step, ON_CLAIM);
                Map<?, ?> onUnclaim = write(step, ON_UNCLAIM);
                if (onClaim == null || onUnclaim == null) {
                    continue;
                }
                String relation = text(onClaim, "setRelationField");
                if (relation == null || !relation.equals(text(onUnclaim, "setRelationField")) || !isToOneOf(owner, relation)) {
                    continue;
                }
                claims.add(new Claim(process.getName(), step.getName(), className(process.getName(), step.getName()), owner.getName(),
                        IntentEntities.resolvePerspective(triggerEntity, compositionParents, model), IntentEntities.keyFieldName(owner),
                        idAccessor(owner), IntentNaming.pascalCase(relation), text(onClaim, "value"), text(onUnclaim, "value")));
            }
        }
        return claims;
    }

    /**
     * Whether the step declares a claim write - what makes the task's completion require a claim.
     *
     * @param step the step
     * @return true when the step declares {@code onClaim}
     */
    public static boolean declaresClaim(StepIntent step) {
        return write(step, ON_CLAIM) != null;
    }

    /**
     * The generated listener class name: process plus step plus {@code ClaimListener}, all PascalCase,
     * so it never meets the {@code <Process><Step>} setter a {@code setRelationField} on the same task
     * generates.
     *
     * @param process the process name
     * @param step the step name
     * @return the class simple name
     */
    public static String className(String process, String step) {
        return IntentNaming.pascalCase(process) + IntentNaming.pascalCase(step) + "ClaimListener";
    }

    private static Map<?, ?> write(StepIntent step, String key) {
        Object value = step.getArgs() == null ? null
                : step.getArgs()
                      .get(key);
        return value instanceof Map<?, ?> map ? map : null;
    }

    private static boolean isToOneOf(EntityIntent entity, String relation) {
        return entity.getRelations()
                     .stream()
                     .anyMatch(candidate -> relation.equals(candidate.getName())
                             && ("manyToOne".equals(candidate.getKind()) || "oneToOne".equals(candidate.getKind())));
    }

    private static String idAccessor(EntityIntent entity) {
        FieldIntent pk = IntentEntities.primaryKeyOf(entity);
        return pk != null && "long".equals(pk.getType()) ? "longValue" : "intValue";
    }

    private static String text(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value == null ? null : value.toString();
    }
}
