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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;

import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.NumberIntent;
import org.eclipse.dirigible.components.intent.model.ProcessIntent;
import org.eclipse.dirigible.components.intent.model.RelationIntent;
import org.eclipse.dirigible.components.intent.model.StepIntent;

/**
 * Builds the {@code numbering} glue collection: one descriptor per {@code number: { stampOn: issue
 * }} field, driving the generated {@code gen/events/<Entity>NumberStamp.java} delegate. The
 * document is created with a UUID placeholder (the uuid auto-fill); this delegate, wired as a
 * {@code delegate:} service task at the issue step, replaces it with the real formatted number -
 * idempotently, so a re-issue after an amend keeps the number.
 *
 * <p>
 * The number is allocated + formatted via {@code sdk.numbering.DocumentNumbers} (the shared
 * per-tenant counter) and written with the targeted {@code updateProperty} (a workflow system
 * write). A partitioned series resolves its partition from the {@code per} FK, falling back to that
 * relation's {@code init:} default when the FK is null (#7101) - the default company is a partition
 * like any other.
 */
final class NumberingSupport {

    private NumberingSupport() {}

    /**
     * One numbering descriptor per {@code stampOn: issue} number field in the model.
     *
     * @param model the parsed intent model
     * @param compositionParents each entity's transitive composition parent (perspective resolution)
     * @return the {@code numbering} collection (possibly empty)
     */
    static List<Map<String, Object>> buildNumbering(IntentModel model, Map<String, String> compositionParents) {
        List<Map<String, Object>> numbering = new ArrayList<>();
        List<SetFieldSupport.Setter> setters = SetFieldSupport.setters(model);
        for (EntityIntent entity : model.getEntities()) {
            for (FieldIntent field : entity.getFields()) {
                NumberIntent number = field.getNumber();
                if (number == null || !"issue".equalsIgnoreCase(number.getStampOn())) {
                    continue; // stampOn:create is handled at insert by the DAO; only issue needs a step
                }
                Map<String, Object> descriptor = new LinkedHashMap<>();
                descriptor.put("entity", entity.getName());
                descriptor.put("perspective", IntentEntities.resolvePerspective(entity.getName(), compositionParents, model));
                descriptor.put("masterPk", IntentEntities.keyFieldName(entity));
                descriptor.put("field", IntentNaming.pascalCase(field.getName()));
                descriptor.put("series", number.getSeries() == null ? entity.getName() : number.getSeries());
                // The partition FK property the stamp reads off the entity ("" = tenant-wide series).
                String per = number.getPer() == null || number.getPer()
                                                              .isBlank() ? "" : IntentNaming.pascalCase(number.getPer());
                descriptor.put("per", per);
                // The partition the stamp falls back to when the FK is null on the loaded row: the
                // relation's `init:` default (#7101) - the same value the create-time allocator uses,
                // so both stamp paths resolve the default company to ITS partition, never the base row.
                descriptor.put("perDefault", partitionDefault(entity, per));
                // The status each step running this stamp precedes (#7577): the stamp asks the
                // repository whether that move will be accepted BEFORE it spends a series value.
                descriptor.put("gates", statusGates(model, entity, setters));
                numbering.add(descriptor);
            }
        }
        return numbering;
    }

    /**
     * The status each process step running the entity's stamp precedes (#7577): for every step whose
     * {@code delegate:} is the generated {@code <Entity>NumberStamp}, the first status write its
     * {@code next:} chain of service tasks reaches - the {@code setRelationField} on the entity's
     * {@code function: EntityStatus} relation the stamp is placed in front of so a posting sees the
     * real number. The stamp asks the repository whether that move will be accepted before it
     * allocates: the series is legal and gap-free, so a value it spends on a move that is then refused
     * is a gap nothing can repair. Keyed by process and step - the BPMN's process id and activity id -
     * since one stamp class serves every process that issues the entity. A chain that waits (a user
     * task, a decision) before any status write stops the search: what follows a wait is a separate
     * transaction, and the stamp's own write already committed.
     *
     * @param model the parsed intent model
     * @param entity the numbered entity
     * @param setters every field setter in the model, with resolved status ids
     * @return one {@code {process, step, status}} entry per stamp step that precedes a status write
     */
    private static List<Map<String, Object>> statusGates(IntentModel model, EntityIntent entity, List<SetFieldSupport.Setter> setters) {
        List<Map<String, Object>> gates = new ArrayList<>();
        RelationIntent status = IntentEntities.entityStatusRelation(entity);
        if (status == null) {
            return gates; // no status to move, so nothing gated to ask about
        }
        String statusProperty = IntentNaming.pascalCase(status.getName());
        String stampClass = entity.getName() + "NumberStamp";
        for (ProcessIntent process : model.getProcesses()) {
            if (!entity.getName()
                       .equals(TriggerSupport.triggerEntity(process))) {
                continue;
            }
            Map<String, StepIntent> stepsByName = new HashMap<>();
            for (StepIntent step : process.getSteps()) {
                if (step.getName() != null) {
                    stepsByName.put(step.getName(), step);
                }
            }
            Map<String, String> statusWrites = new HashMap<>();
            for (SetFieldSupport.Setter setter : setters) {
                if (process.getName()
                           .equals(setter.process())
                        && setter.relation() && statusProperty.equals(setter.field()) && !setter.value()
                                                                                                .isBlank()) {
                    statusWrites.put(setter.step(), setter.value()
                                                          .trim());
                }
            }
            for (StepIntent step : process.getSteps()) {
                if (step.getName() == null || !runsStamp(step, stampClass)) {
                    continue;
                }
                String written = statusWrittenAfter(step, stepsByName, statusWrites);
                if (written != null && written.matches("\\d+")) {
                    Map<String, Object> gate = new LinkedHashMap<>();
                    gate.put("process", process.getName());
                    gate.put("step", step.getName());
                    gate.put("status", written);
                    gates.add(gate);
                }
            }
        }
        return gates;
    }

    /** Whether a step is a service task whose {@code delegate:} is the given stamp class. */
    private static boolean runsStamp(StepIntent step, String stampClass) {
        Object delegate = step.getArgs() == null ? null
                : step.getArgs()
                      .get("delegate");
        if (!"serviceTask".equals(step.getKind()) || delegate == null) {
            return false;
        }
        String name = delegate.toString()
                              .trim();
        return name.equals(stampClass) || name.endsWith("." + stampClass);
    }

    /**
     * The status the {@code next:} chain after {@code from} writes first, walking service tasks only.
     */
    private static String statusWrittenAfter(StepIntent from, Map<String, StepIntent> stepsByName, Map<String, String> statusWrites) {
        Set<String> visited = new HashSet<>();
        StepIntent current = from;
        while (current != null && visited.add(current.getName())) {
            Object next = current.getArgs() == null ? null
                    : current.getArgs()
                             .get("next");
            StepIntent step = next == null ? null : stepsByName.get(next.toString());
            if (step == null) {
                return null;
            }
            String written = statusWrites.get(step.getName());
            if (written != null) {
                return written;
            }
            if (!"serviceTask".equals(step.getKind())) {
                return null; // a wait before any status write: what follows is another transaction
            }
            current = step;
        }
        return null;
    }

    /**
     * The {@code init:} of the entity's relation named {@code per} ("" when the relation carries none):
     * the value the partition FK WILL hold on a row that left it unset.
     *
     * <p>
     * Always a plain seed id, so it needs no unquoting the way the DAO template's
     * {@code #defaultLiteral} does for a field default: a partition is a KEY compared against the
     * values explicit rows allocate under, and a SQL-quoted {@code 'ACME'} beside {@code ACME} would be
     * two counters for one company - but {@code init:} never reaches here in that shape. The parser
     * resolves it against the target's own seeds and refuses anything that is neither a seeded name nor
     * a numeric id ({@code StatusSymbolResolver}), so the quoted authoring shape a field default
     * accepts is a parse error on a relation (#7147).
     *
     * @param entity the entity declaring the numbered field
     * @param per the pascal-cased partition relation name ("" when the series is tenant-wide)
     * @return the relation's init value, or "" when it declares none
     */
    private static String partitionDefault(EntityIntent entity, String per) {
        if (per.isEmpty()) {
            return "";
        }
        for (RelationIntent relation : entity.getRelations()) {
            if (relation.getName() != null && per.equals(IntentNaming.pascalCase(relation.getName()))) {
                return relation.getInit() == null ? ""
                        : relation.getInit()
                                  .trim();
            }
        }
        return "";
    }
}
