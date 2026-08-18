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

import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.IntegrationIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.NotificationIntent;
import org.eclipse.dirigible.components.intent.model.OutboundIntent;
import org.eclipse.dirigible.components.intent.model.ProcessIntent;
import org.eclipse.dirigible.components.intent.model.StepIntent;

/**
 * The <b>process-step</b> half of the glue event axis: {@code event: { onStepReached: { process:
 * &lt;Process&gt;, step: &lt;step&gt; } }} and its {@code onStepCompleted} twin, next to the entity
 * lifecycle bindings {@link EventBinding} reads.
 *
 * <p>
 * A step event is delivered as a normal <b>entity message</b>: the BPMN generator inserts a
 * generated {@code JavaDelegate} at the step boundary which loads the process's trigger entity by
 * the id in the (clear-D) process context and publishes its JSON to a step-scoped topic. That topic
 * is the entity's own topic plus {@link #topicSuffix(Map) a step suffix}, so every consumer the
 * lifecycle events already feed - notifications with their relation loads, guards and print
 * attachments, integrations, outbound departures - binds to it and reads the payload unchanged. The
 * action vocabulary is therefore reused literally, not re-implemented per event kind.
 *
 * <p>
 * The record a step event is about is the process's trigger entity: a process runs on one record,
 * and every glue action (a recipient path, a placeholder, a forwarded body) is written against it.
 * A process without a resolvable trigger has no such record, so it cannot carry step events - the
 * parser rejects that binding rather than generating a listener nothing ever publishes to.
 */
public final class StepEventSupport {

    /** The event kind naming a step the execution has just reached (fires before the step runs). */
    public static final String ON_STEP_REACHED = "onStepReached";
    /** The event kind naming a step the execution has just completed (fires after the step ran). */
    public static final String ON_STEP_COMPLETED = "onStepCompleted";

    /** The step kinds a step event may bind to - the ones that occupy a moment in the flow. */
    public static final Set<String> EVENTABLE_STEP_KINDS = Set.of("userTask", "serviceTask");

    private static final String PROCESS_KEY = "process";
    private static final String STEP_KEY = "step";

    private StepEventSupport() {}

    /**
     * A step event binding as authored: the kind plus the process and step it names.
     *
     * @param kind {@link #ON_STEP_REACHED} or {@link #ON_STEP_COMPLETED}
     * @param process the process name
     * @param step the step name
     */
    public record Binding(String kind, String process, String step) {

        /** @return whether the binding fires after the step ran */
        public boolean completed() {
            return ON_STEP_COMPLETED.equals(kind);
        }
    }

    /**
     * One generated emitter: the {@code JavaDelegate} the BPMN generator inserts at a step boundary to
     * publish the trigger entity on the step topic. Emitters are deduplicated per (process, step,
     * kind), so any number of notifications, integrations and departures bound to the same moment share
     * one.
     *
     * @param process the process the step belongs to
     * @param step the step whose boundary the emitter sits at
     * @param kind {@link #ON_STEP_REACHED} or {@link #ON_STEP_COMPLETED}
     * @param className the generated delegate's simple name
     * @param entity the process's trigger entity - the record the event is about
     * @param keyProperty the process variable holding that record's PK
     * @param keyAccessor the {@link Number} accessor matching the PK type
     */
    public record Emitter(String process, String step, String kind, String className, String entity, String keyProperty,
            String keyAccessor) {

        /** @return whether the emitter runs after the step */
        public boolean completed() {
            return ON_STEP_COMPLETED.equals(kind);
        }
    }

    /**
     * @param kind an event kind, may be {@code null}
     * @return whether it is one of the two step-event kinds
     */
    public static boolean isStepKind(String kind) {
        return ON_STEP_REACHED.equals(kind) || ON_STEP_COMPLETED.equals(kind);
    }

    /**
     * Read a glue event map as a step binding.
     *
     * @param event the {@code event:} map, may be {@code null}
     * @return the binding, or {@code null} when the map carries no (well-formed) step event
     */
    public static Binding binding(Map<String, Object> event) {
        if (event == null) {
            return null;
        }
        for (String kind : new String[] {ON_STEP_REACHED, ON_STEP_COMPLETED}) {
            Object target = event.get(kind);
            if (target instanceof Map<?, ?> named) {
                String process = text(named.get(PROCESS_KEY));
                String step = text(named.get(STEP_KEY));
                if (process != null && step != null) {
                    return new Binding(kind, process, step);
                }
            }
        }
        return null;
    }

    /**
     * The entity a glue event binding is about, whichever axis it uses: the entity a lifecycle binding
     * names, or the trigger entity of the process a step binding names.
     *
     * @param model the parsed model
     * @param event the {@code event:} map, may be {@code null}
     * @return the entity name, or {@code null} when the binding resolves to none
     */
    public static String eventEntity(IntentModel model, Map<String, Object> event) {
        Binding binding = binding(event);
        if (binding == null) {
            return EventBinding.entity(event);
        }
        ProcessIntent process = process(model, binding.process());
        return process == null ? null : TriggerSupport.triggerEntity(process);
    }

    /**
     * The topic suffix a glue event binding appends to its entity's topic: the lifecycle suffix, or the
     * step-scoped suffix the generated emitter publishes to.
     *
     * @param event the {@code event:} map, may be {@code null}
     * @return the suffix ({@code ""} for an entity create event)
     */
    public static String topicSuffix(Map<String, Object> event) {
        Binding binding = binding(event);
        return binding == null ? EventBinding.topicSuffix(EventBinding.kind(event))
                : topicSuffix(binding.process(), binding.step(), binding.kind());
    }

    /**
     * The step-scoped topic suffix - appended to the entity's own topic, so a step event travels the
     * same channel shape as a lifecycle event and every existing consumer binds to it unchanged.
     *
     * @param process the process name
     * @param step the step name
     * @param kind the step event kind
     * @return the suffix, e.g. {@code -step-OrderApproval-managerReview-reached}
     */
    public static String topicSuffix(String process, String step, String kind) {
        return "-step-" + process + "-" + step + "-" + (ON_STEP_COMPLETED.equals(kind) ? "completed" : "reached");
    }

    /**
     * The generated emitter's class name - the process and step names, PascalCase, suffixed by the
     * moment, so it cannot collide with the setter / sender / writer classes of the same step.
     *
     * @param process the process name
     * @param step the step name
     * @param kind the step event kind
     * @return the simple class name
     */
    public static String className(String process, String step, String kind) {
        return IntentNaming.pascalCase(process) + IntentNaming.pascalCase(step)
                + (ON_STEP_COMPLETED.equals(kind) ? "Completed" : "Reached");
    }

    /**
     * Every emitter the model needs, in declaration order and deduplicated per (process, step, kind). A
     * binding whose process, step or trigger entity does not resolve contributes none - the parser has
     * already reported it.
     *
     * @param model the parsed intent model
     * @return the emitters (possibly empty)
     */
    public static List<Emitter> emitters(IntentModel model) {
        List<Emitter> emitters = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        Map<String, EntityIntent> byName = IntentEntities.byName(model);
        for (Map<String, Object> event : boundEvents(model)) {
            Binding binding = binding(event);
            if (binding == null || !seen.add(binding.kind() + "/" + binding.process() + "/" + binding.step())) {
                continue;
            }
            ProcessIntent process = process(model, binding.process());
            String entity = process == null ? null : TriggerSupport.triggerEntity(process);
            EntityIntent owner = entity == null ? null : byName.get(entity);
            if (owner == null || step(process, binding.step()) == null) {
                continue;
            }
            emitters.add(new Emitter(binding.process(), binding.step(), binding.kind(),
                    className(binding.process(), binding.step(), binding.kind()), entity, IntentEntities.keyFieldName(owner),
                    keyAccessor(owner)));
        }
        return emitters;
    }

    /**
     * @param process a process, may be {@code null}
     * @param stepName the step to find
     * @return the named step of that process, or {@code null}
     */
    public static StepIntent step(ProcessIntent process, String stepName) {
        if (process == null || stepName == null) {
            return null;
        }
        for (StepIntent step : process.getSteps()) {
            if (stepName.equals(step.getName())) {
                return step;
            }
        }
        return null;
    }

    /**
     * @param model the parsed model
     * @param processName the process to find
     * @return the named process, or {@code null}
     */
    public static ProcessIntent process(IntentModel model, String processName) {
        if (model == null || processName == null) {
            return null;
        }
        for (ProcessIntent process : model.getProcesses()) {
            if (processName.equals(process.getName())) {
                return process;
            }
        }
        return null;
    }

    /** The event maps of every glue entry that binds to the event axis, in declaration order. */
    private static List<Map<String, Object>> boundEvents(IntentModel model) {
        List<Map<String, Object>> events = new ArrayList<>();
        for (NotificationIntent notification : model.getNotifications()) {
            events.add(notification.getEvent());
        }
        for (IntegrationIntent integration : model.getIntegrations()) {
            events.add(integration.getEvent());
        }
        for (OutboundIntent outbound : model.getOutbound()) {
            events.add(outbound.getEvent());
        }
        return events;
    }

    private static String text(Object value) {
        String text = value == null ? null
                : value.toString()
                       .trim();
        return text == null || text.isEmpty() ? null : text;
    }

    private static String keyAccessor(EntityIntent owner) {
        FieldIntent pk = IntentEntities.primaryKeyOf(owner);
        String type = pk == null || pk.getType() == null ? "integer" : pk.getType();
        return "long".equals(type) ? "longValue" : "intValue";
    }
}
