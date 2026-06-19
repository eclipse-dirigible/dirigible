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
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.ProcessIntent;
import org.eclipse.dirigible.components.intent.model.RelationIntent;
import org.eclipse.dirigible.components.intent.model.StepIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads the {@code relation.field} references in a process's decision conditions and turns each
 * into a <b>resolver</b>: a just-before-the-decision step that loads the related (to-one) entity by
 * its FK id and exposes the wanted field as a process variable.
 * <p>
 * A decision authored as {@code if: "book.price > 500"} on a {@code LoanApproval} triggered by
 * {@code onCreate: Loan} resolves {@code book} (a to-one relation on {@code Loan}) to {@code Book},
 * reads {@code price}, and publishes it as the {@code book_price} variable; the condition is then
 * rewritten to {@code book_price > 500}. Fetching at the decision (rather than eagerly at process
 * start) keeps the value fresh for long-running approvals and keeps the process variables lean.
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
     * @param decisionStep the decision step whose condition referenced the path
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
    public record Resolver(String process, String decisionStep, String token, String variable, String handler, String fkProperty,
            String targetEntity, String targetField, String targetPerspective, String targetIdAccessor) {
    }

    /** Every resolver across every process in the model. */
    public static List<Resolver> resolvers(IntentModel model) {
        List<Resolver> resolvers = new ArrayList<>();
        Map<String, EntityIntent> byName = IntentEntities.byName(model);
        Map<String, String> compositionParents = IntentEntities.compositionParents(model);
        Set<String> seen = new LinkedHashSet<>();
        for (ProcessIntent process : model.getProcesses()) {
            String triggerEntity = TriggerSupport.onCreateEntity(process);
            EntityIntent owner = triggerEntity == null ? null : byName.get(triggerEntity);
            if (owner == null) {
                continue; // no trigger entity -> no process-variable context to resolve against
            }
            for (StepIntent step : process.getSteps()) {
                if (!"decision".equals(step.getKind())) {
                    continue;
                }
                String condition = stringArg(step, "if");
                if (condition == null) {
                    continue;
                }
                collect(model, byName, compositionParents, process, owner, step, condition, resolvers, seen);
            }
        }
        return resolvers;
    }

    private static void collect(IntentModel model, Map<String, EntityIntent> byName, Map<String, String> compositionParents,
            ProcessIntent process, EntityIntent owner, StepIntent step, String condition, List<Resolver> resolvers, Set<String> seen) {
        Matcher matcher = RELATION_FIELD.matcher(condition);
        while (matcher.find()) {
            String relationName = matcher.group(1);
            String fieldName = matcher.group(2);
            RelationIntent relation = toOneRelation(owner, relationName);
            if (relation == null) {
                continue; // not a to-one relation of the trigger entity - leave the token alone
            }
            EntityIntent target = byName.get(relation.getTo());
            if (target == null || fieldOf(target, fieldName) == null) {
                LOGGER.warn("Decision [{}] in process [{}] references [{}] but [{}] has no field [{}] - skipping resolver", step.getName(),
                        process.getName(), matcher.group(), relation.getTo(), fieldName);
                continue;
            }
            String handler = "Resolve" + IntentNaming.pascalCase(relationName) + IntentNaming.pascalCase(fieldName);
            if (!seen.add(process.getName() + "/" + handler)) {
                continue; // same resolution referenced twice in the process
            }
            resolvers.add(new Resolver(process.getName(), step.getName(), relationName + "." + fieldName, relationName + "_" + fieldName,
                    handler, IntentNaming.pascalCase(relationName), relation.getTo(), IntentNaming.pascalCase(fieldName),
                    IntentEntities.resolvePerspective(relation.getTo(), compositionParents),
                    idAccessor(IntentEntities.primaryKeyOf(target))));
        }
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
