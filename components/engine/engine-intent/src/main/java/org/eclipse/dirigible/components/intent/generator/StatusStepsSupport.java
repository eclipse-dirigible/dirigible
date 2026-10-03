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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.LifecycleStages;
import org.eclipse.dirigible.components.intent.model.ProcessIntent;
import org.eclipse.dirigible.components.intent.model.RelationIntent;
import org.eclipse.dirigible.components.intent.model.StepIntent;

/**
 * Which statuses a status stepper shows, read off the model once for both steppers that render one:
 * a task form's (the flow the form belongs to, {@code FormIntentGenerator}) and a document page's
 * (every flow of the entity, {@code EdmIntentGenerator}). A stepper states "this flow goes 1, 2,
 * 3", so its steps are the statuses a flow actually WALKS - the ones its process steps write
 * ({@code setRelationField: <status relation>}) plus the one it enters at - in seed order, the
 * cancel/reject/void-style terminals dropped. The nomenclature alone is a list and cannot express a
 * branch: taken whole it shows settlement states (PARTIAL, PAID) as the next steps of an approval
 * (issue #7085).
 *
 * <p>
 * Everything here is decided by seed id and the base (untranslated) seed name, never by a rendered
 * label: a page that ordered its steps by the label it showed listed them alphabetically, in
 * whichever language the user had chosen (issue #7592).
 */
public final class StatusStepsSupport {

    /**
     * Terminal / negative status names excluded from a stepper - a cancel/reject/void is an off-path
     * outcome (it stays the status pill), not a forward step. Same heuristic the badge colouring uses
     * so the two stay consistent.
     */
    private static final Pattern TERMINAL_STATUS = Pattern.compile("(cancel|reject|declin|void|fail|overdue|insufficient|exhaust)");

    /**
     * A trigger {@code when} guard that pins one status: {@code <status relation> == <seed id>} (the
     * symbolic name is already resolved to the id by {@code StatusSymbolResolver}). Only {@code ==} -
     * that is the single comparison the generated listener's guard enforces
     * ({@code NotificationSupport.guard}), and an inequality names no entry status anyway.
     */
    private static final Pattern STATUS_EQUALITY = Pattern.compile("\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*==\\s*(-?\\d+)\\s*");

    private StatusStepsSupport() {}

    /**
     * Whether a seeded status name is a terminal / off-path outcome rather than a forward step.
     *
     * @param name the base seed name
     * @return true for a cancel/reject/void-style name
     */
    public static boolean isTerminal(String name) {
        return name != null && TERMINAL_STATUS.matcher(name.toLowerCase(Locale.ROOT))
                                              .find();
    }

    /**
     * The steps of a document page's stepper: the seed ids of every status some flow of the entity
     * walks ({@link #walkedStatusIds} over every process the entity triggers), in seed order, terminals
     * dropped. When no process of the entity writes a status the model says nothing about the walk, and
     * the whole non-terminal nomenclature is the best available reading - the same fallback the task
     * form takes.
     *
     * @param model the whole intent
     * @param entity the document entity
     * @param statusRel the entity's {@code function: EntityStatus} relation, to a status entity seeded
     *        in this model
     * @return the step seed ids in seed order, never {@code null}
     */
    public static List<Integer> lifecycleStepIds(IntentModel model, EntityIntent entity, RelationIntent statusRel) {
        Set<Integer> walked = walkedStatusIds(model, entity, statusRel, process -> true);
        List<Integer> steps = new ArrayList<>();
        for (Map.Entry<Integer, String> seeded : LifecycleStages.seededStatuses(model, statusRel.getTo())
                                                                .entrySet()) {
            String name = seeded.getValue();
            if (name == null || isTerminal(name) || (!walked.isEmpty() && !walked.contains(seeded.getKey()))) {
                continue;
            }
            steps.add(seeded.getKey());
        }
        return steps;
    }

    /**
     * The seed ids of the statuses the flows of an entity WALK: every status a step of a selected
     * process writes ({@code setRelationField: <status relation>} + {@code value:}), plus the status
     * each such process enters at ({@link #entryStatus}). Only processes whose trigger entity is this
     * entity are read - a write on another entity's status says nothing about this one - and a process
     * that writes no status contributes nothing, its entry included. Empty when nothing is known, which
     * the callers read as "no filter".
     *
     * @param model the whole intent - the processes are declared beside the entities
     * @param entity the entity whose status the flows walk
     * @param statusRel the entity's {@code function: EntityStatus} relation
     * @param selected which of the entity's processes count (a task form: the ones opening it)
     * @return the walked seed ids, or empty when no selected flow writes a status
     */
    public static Set<Integer> walkedStatusIds(IntentModel model, EntityIntent entity, RelationIntent statusRel,
            Predicate<ProcessIntent> selected) {
        if (entity.getName() == null) {
            return Set.of();
        }
        Set<Integer> walked = new HashSet<>();
        Set<Integer> entries = new HashSet<>();
        for (ProcessIntent process : model.getProcesses()) {
            if (!entity.getName()
                       .equals(TriggerSupport.triggerEntity(process))
                    || !selected.test(process)) {
                continue;
            }
            boolean writesStatus = false;
            for (StepIntent step : process.getSteps()) {
                Map<String, Object> args = step.getArgs();
                Object relation = args == null ? null : args.get("setRelationField");
                if (relation == null || !statusRel.getName()
                                                  .equalsIgnoreCase(relation.toString())) {
                    continue;
                }
                Integer target = statusId(args.get("value"));
                if (target != null) {
                    walked.add(target);
                    writesStatus = true;
                }
            }
            if (!writesStatus) {
                continue; // this process says nothing about the walk, so neither does where it starts
            }
            Integer entry = entryStatus(process, statusRel);
            if (entry != null) {
                entries.add(entry);
            }
        }
        if (walked.isEmpty()) {
            return Set.of(); // no flow writes a status - they say nothing about the walk
        }
        walked.addAll(entries);
        return walked;
    }

    /**
     * The status the flow STANDS AT when it starts - its first step, when that is knowable from the
     * process's own {@code trigger}:
     * <ul>
     * <li>a status the trigger's {@code when} guard pins with an equality ({@code Status == ISSUED},
     * already resolved to the seed id by the parser) - the record cannot enter the flow at any other
     * status, whichever lifecycle event carries it in;</li>
     * <li>otherwise the relation's {@code init:} for an {@code onCreate} trigger - the status a freshly
     * created record stands at;</li>
     * <li>otherwise {@code null}. A bare {@code onUpdate} / {@code onTransition} flow starts wherever
     * the record happens to be, and the honest stepper is the one that claims no entry step at all.
     * </li>
     * </ul>
     * Reading {@code init:} for every trigger was the {@code #7085} defect one notch smaller (issue
     * #7239): a settlement flow triggered {@code onUpdate: Invoice} {@code when: "Status == ISSUED"}
     * showed DRAFT as its step 1 - a status it never walks - and omitted ISSUED, the one it enters at.
     *
     * @param process the owning process
     * @param statusRel the entity's {@code function: EntityStatus} relation
     * @return the entry status id, or {@code null} when the trigger does not say
     */
    private static Integer entryStatus(ProcessIntent process, RelationIntent statusRel) {
        Integer guarded = guardedStatus(TriggerSupport.triggerWhen(process), statusRel);
        if (guarded != null) {
            return guarded;
        }
        return "onCreate".equals(TriggerSupport.triggerKind(process)) ? statusId(statusRel.getInit()) : null;
    }

    /**
     * The status a {@code when} guard pins to a single value: {@code <status relation> == <id>}. The
     * guard may be a list of comparisons - their implicit AND (dirigible #6957) - and any term may be
     * the status one. An inequality pins nothing (it names the statuses the flow does NOT enter at),
     * and neither does a comparison on any other field.
     *
     * @param when the trigger's {@code when} - a comparison string, a list of them, or {@code null}
     * @param statusRel the entity's {@code function: EntityStatus} relation
     * @return the status id the guard pins, or {@code null}
     */
    private static Integer guardedStatus(Object when, RelationIntent statusRel) {
        if (when instanceof List<?> terms) {
            for (Object term : terms) {
                Integer status = guardedStatus(term, statusRel);
                if (status != null) {
                    return status;
                }
            }
            return null;
        }
        if (when == null || statusRel.getName() == null) {
            return null;
        }
        Matcher matcher = STATUS_EQUALITY.matcher(String.valueOf(when));
        return matcher.matches() && statusRel.getName()
                                             .equalsIgnoreCase(matcher.group(1)) ? statusId(matcher.group(2)) : null;
    }

    /**
     * A status seed id as an int - a symbolic status name has already been resolved to its id by the
     * parser, so a token that is not a number is not an id (and is reported there).
     */
    private static Integer statusId(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = value == null ? null
                : String.valueOf(value)
                        .trim();
        if (text == null || !text.matches("-?\\d+")) {
            return null;
        }
        try {
            return Integer.valueOf(text);
        } catch (NumberFormatException ex) {
            return null; // more digits than an int holds - no seed id looks like that
        }
    }
}
