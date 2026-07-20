/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator.bpmn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import java.util.regex.Pattern;

import org.eclipse.dirigible.components.intent.generator.IntentEntities;
import org.eclipse.dirigible.components.intent.generator.IntentGenerationContext;
import org.eclipse.dirigible.components.intent.generator.IntentNaming;
import org.eclipse.dirigible.components.intent.generator.IntentTargetGenerator;
import org.eclipse.dirigible.components.intent.generator.ProcessFieldLoadSupport;
import org.eclipse.dirigible.components.intent.generator.ProcessFieldLoadSupport.FieldLoad;
import org.eclipse.dirigible.components.intent.generator.ProcessResolverSupport;
import org.eclipse.dirigible.components.intent.generator.ProcessResolverSupport.Resolver;
import org.eclipse.dirigible.components.intent.generator.ProcessTimerSupport;
import org.eclipse.dirigible.components.intent.generator.ProcessTimerSupport.TimerLoad;
import org.eclipse.dirigible.components.intent.generator.ProcessWaitSupport;
import org.eclipse.dirigible.components.intent.generator.WriterSupport;
import org.eclipse.dirigible.components.intent.generator.WriterSupport.Writer;
import org.eclipse.dirigible.components.intent.generator.SetFieldSupport;
import org.eclipse.dirigible.components.intent.generator.TriggerSupport;
import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.PermissionIntent;
import org.eclipse.dirigible.components.intent.model.ProcessIntent;
import org.eclipse.dirigible.components.intent.model.StepIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Emits one {@code <process>.bpmn} (at the project root) per {@link ProcessIntent} declared in the
 * intent. The output is a minimal Flowable-flavoured BPMN 2.0 document:
 * <ul>
 * <li>one {@code <startEvent>} and one {@code <endEvent>} per process</li>
 * <li>{@code userTask} ->
 * {@code <userTask flowable:candidateGroups="..." flowable:formKey="..."/>}</li>
 * <li>{@code serviceTask} / {@code script} ->
 * {@code <serviceTask flowable:delegateExpression="${JSTask}">} with the {@code handler} extension
 * element pointing at {@code args.call}</li>
 * <li>{@code decision} -> {@code <exclusiveGateway>} with a conditioned outgoing flow to
 * {@code args.then} and a default flow to {@code args.else} (falling back to the next step in the
 * chain when {@code else} is omitted) - so "big orders need CFO review, small ones skip it" is
 * expressible</li>
 * <li>{@code end} -> the canonical end event (no separate element; the outgoing flow targets the
 * single {@code <endEvent>})</li>
 * </ul>
 *
 * <p>
 * The <b>{@code bpmndi:BPMNDiagram}</b> block IS emitted (see {@link #appendBpmnDiagram}): the
 * Flowable/Oryx modeler renders the canvas only from the diagram interchange, so a process with no
 * shapes opens empty. Nodes are laid out with a deterministic layered (Sugiyama-style) placement -
 * column by longest-path distance from the start event, lane by predecessor barycentre - with
 * orthogonally-routed edges, so branching processes read cleanly without manual reordering; the
 * output stays byte-stable and the modeler re-routes on first manual edit.
 *
 * <p>
 * <b>{@code flowable:formKey} is the form page URL.</b> The Inbox / Process perspective opens a
 * task's form by navigating straight to its {@code formKey} (plus
 * {@code ?taskId=&processInstanceId=}), so the key must be the served path of the generated form
 * page, not a bare name (a bare name resolves relative to the inbox and 404s). It is therefore
 * {@code /services/web/<project>/gen/<form>/forms/<form>/index.html} - the layout the form-builder
 * template emits ({@code gen/{genFolderName}/forms/{fileName}/index.html}) for the
 * {@code <form>.form} this intent produced. This is a deliberate, documented exception to the "no
 * template-engine paths" rule: a user task cannot reference its form without naming where that form
 * is rendered, and this is the canonical Dirigible form-key convention. A service task's
 * {@code args.call} (a project-relative JS handler path such as {@code custom/notify-member.ts}) is
 * likewise qualified with the project name, because the {@code ${JSTask}} delegate resolves it
 * relative to the registry root; it should reference a hand-authored handler under {@code custom/}.
 *
 * <p>
 * A {@code relation.field} of the trigger entity referenced by a {@code decision} condition (e.g.
 * {@code customer.creditLimit > 10000}) <b>or by a user-task form</b> (a {@code book.price} field
 * on an approval form) gets a {@code JavaTask} resolver service task inserted before the earliest
 * step that needs it; decision conditions are rewritten to test the resolved variable
 * ({@code customer_creditLimit}) and form controls bind to it. The resolver handler itself is
 * generated from the {@code .glue} file (see
 * {@link org.eclipse.dirigible.components.intent.generator.ProcessResolverSupport}). The trigger
 * {@code onCreate} block similarly drives the {@code .glue} file - the BPMN keeps a plain
 * none-start event; the generated {@code @Listener} starts the process on the entity's create
 * event.
 *
 * <p>
 * Idempotent: identical input always produces byte-identical output.
 */
@Component
@Order(300)
public class BpmnIntentGenerator implements IntentTargetGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(BpmnIntentGenerator.class);

    private static final String START_ID = "start";
    private static final String END_ID = "end";

    @Override
    public String name() {
        return "bpmn";
    }

    @Override
    public void generate(IntentGenerationContext context) {
        IntentModel model = context.getModel();
        if (model.getProcesses()
                 .isEmpty()) {
            return;
        }
        // A user task's assignee names a role (lower-camel in the intent, e.g. "librarian"); the
        // Process Inbox matches a task's flowable:candidateGroups against the user's role names
        // (TaskServiceImpl: taskCandidateGroupIn(UserFacade.getUserRoles())), which are the declared
        // role names ("Librarian"). Resolve the assignee to the actual role name so the casing lines
        // up - otherwise the task is started but never shows up for anyone.
        Map<String, String> rolesByLowerName = buildRoleIndex(model.getPermissions());
        // Decision resolvers: a relation.field reference (book.price) becomes a service task before the
        // decision that loads the related entity and a condition rewritten to test the resolved
        // variable (book_price). The handler itself is generated from the .glue file, not here.
        List<Resolver> allResolvers = ProcessResolverSupport.resolvers(model);
        // Field loaders: a decision on the trigger entity's own field gets a JavaDelegate inserted before
        // the gateway that loads the owner by id and publishes the referenced fields (clear-D id-only).
        List<FieldLoad> allFieldLoads = ProcessFieldLoadSupport.fieldLoads(model);
        // Expire date loaders: a user task with `expire: { until: <date field> }` gets a JavaDelegate
        // inserted before it that re-reads the trigger entity's date field at task entry and publishes
        // the process variable the boundary timer's timeDate binds to.
        List<TimerLoad> allTimerLoads = ProcessTimerSupport.timerLoads(model);
        // Writers: a user task with editable fields gets a JavaDelegate (gen.events.<Process><Task>Write)
        // inserted after it to persist the reviewer's edits. Index by process+task so render() can place
        // it right after the matching user task.
        Map<String, String> writerByProcessTask = new HashMap<>();
        for (Writer writer : WriterSupport.writers(model)) {
            writerByProcessTask.put(writer.process() + "/" + writer.userTask(), writer.className());
        }
        // Relation setters declared on a user task get a JavaDelegate (gen.events.<Process><Task>)
        // inserted right after the task (like the writer) to set the relation FK once the task completes;
        // a setRelationField on a serviceTask is bound directly by appendServiceTask instead, so only the
        // user-task setters go in this map. Built from the validated setter list so an unknown relation
        // (which SetFieldSupport skips with a warning) never gets a dangling BPMN reference.
        Set<String> userTaskKeys = new HashSet<>();
        for (ProcessIntent process : model.getProcesses()) {
            for (StepIntent step : process.getSteps()) {
                if ("userTask".equals(step.getKind()) && step.getName() != null) {
                    userTaskKeys.add(process.getName() + "/" + step.getName());
                }
            }
        }
        Map<String, String> setterByProcessTask = new HashMap<>();
        for (SetFieldSupport.Setter setter : SetFieldSupport.setters(model)) {
            String key = setter.process() + "/" + setter.step();
            if (setter.relation() && userTaskKeys.contains(key)) {
                setterByProcessTask.put(key, setter.className());
            }
        }
        Map<String, EntityIntent> byName = IntentEntities.byName(model);
        // Extra candidate groups from the .settings (defaults to ADMINISTRATOR) appended to every user
        // task, so an administrator can always claim a task in addition to the task's own role.
        String candidateGroupsExtra = String.join(",", context.getSettings()
                                                              .candidateGroupsExtra());
        Set<String> seenFiles = new HashSet<>();
        for (ProcessIntent process : model.getProcesses()) {
            if (process.getName() == null || process.getName()
                                                    .isBlank()) {
                LOGGER.warn("Skipping unnamed process in intent [{}]", IntentNaming.baseName(context));
                continue;
            }
            String fileName = process.getName() + ".bpmn";
            if (!seenFiles.add(fileName)) {
                LOGGER.warn("Duplicate process [{}] in intent [{}] - keeping the first occurrence", process.getName(),
                        IntentNaming.baseName(context));
                continue;
            }
            if (!process.getTrigger()
                        .isEmpty()) {
                LOGGER.info(
                        "Process [{}] declares a trigger; the BPMN keeps a none-start event - auto-start (listener/handler under gen/events) is generated separately, so for now start it explicitly",
                        process.getName());
            }
            List<Resolver> processResolvers = new ArrayList<>();
            for (Resolver resolver : allResolvers) {
                if (process.getName()
                           .equals(resolver.process())) {
                    processResolvers.add(resolver);
                }
            }
            List<FieldLoad> processFieldLoads = new ArrayList<>();
            for (FieldLoad load : allFieldLoads) {
                if (process.getName()
                           .equals(load.process())) {
                    processFieldLoads.add(load);
                }
            }
            List<TimerLoad> processTimerLoads = new ArrayList<>();
            for (TimerLoad load : allTimerLoads) {
                if (process.getName()
                           .equals(load.process())) {
                    processTimerLoads.add(load);
                }
            }
            String taskPrefix = process.getName() + "/";
            Map<String, String> writerByTask = stripProcessPrefix(writerByProcessTask, taskPrefix);
            Map<String, String> setterByTask = stripProcessPrefix(setterByProcessTask, taskPrefix);
            context.writeModelFile(fileName,
                    render(process, rolesByLowerName, context.getProjectName(), processResolvers, processFieldLoads, processTimerLoads,
                            ownFieldPascalCase(process, byName), candidateGroupsExtra, writerByTask, setterByTask));
        }
    }

    /** The {@code <process>/<task>}-keyed entries for one process, re-keyed by the bare task name. */
    private static Map<String, String> stripProcessPrefix(Map<String, String> byProcessTask, String processPrefix) {
        Map<String, String> byTask = new HashMap<>();
        for (Map.Entry<String, String> entry : byProcessTask.entrySet()) {
            if (entry.getKey()
                     .startsWith(processPrefix)) {
                byTask.put(entry.getKey()
                                .substring(processPrefix.length()),
                        entry.getValue());
            }
        }
        return byTask;
    }

    /**
     * The trigger entity's own field names mapped camelCase -> PascalCase (the process-variable names).
     */
    private static Map<String, String> ownFieldPascalCase(ProcessIntent process, Map<String, EntityIntent> byName) {
        Map<String, String> rewrites = new LinkedHashMap<>();
        String triggerEntity = TriggerSupport.triggerEntity(process);
        EntityIntent entity = triggerEntity == null ? null : byName.get(triggerEntity);
        if (entity != null) {
            for (FieldIntent field : entity.getFields()) {
                if (field.getName() != null) {
                    rewrites.put(field.getName(), IntentNaming.pascalCase(field.getName()));
                }
            }
        }
        return rewrites;
    }

    /** Index of declared role names by their lower-cased form, for resolving a user-task assignee. */
    private static Map<String, String> buildRoleIndex(List<PermissionIntent> permissions) {
        Map<String, String> index = new HashMap<>();
        for (PermissionIntent permission : permissions) {
            String role = permission.getRole();
            if (role != null && !role.isBlank()) {
                index.put(role.toLowerCase(Locale.ROOT), role);
            }
        }
        return index;
    }

    /**
     * The candidate group for a user task's {@code assignee}: the matching declared role name
     * (case-insensitive), or PascalCase of the assignee when no role is declared - so the group still
     * matches the role convention the {@code .roles} generator would produce.
     */
    private static String resolveCandidateGroup(String assignee, Map<String, String> rolesByLowerName) {
        String match = rolesByLowerName.get(assignee.toLowerCase(Locale.ROOT));
        return match != null ? match : IntentNaming.pascalCase(assignee);
    }

    /**
     * The served URL of a form's generated page, used as the user task's {@code flowable:formKey}. The
     * form-builder template renders {@code <form>.form} to {@code gen/<form>/forms/<form>/index.html},
     * served under {@code /services/web/<project>/}.
     */
    private static String formPageUrl(String projectName, String form) {
        return "/services/web/" + projectName + "/gen/" + form + "/forms/" + form + "/index.html";
    }

    private static String render(ProcessIntent process, Map<String, String> rolesByLowerName, String projectName, List<Resolver> resolvers,
            List<FieldLoad> fieldLoads, List<TimerLoad> timerLoads, Map<String, String> ownFieldPascalCase, String candidateGroupsExtra,
            Map<String, String> writerByTask, Map<String, String> setterByTask) {
        // Insert each resolver service task before its anchor step (the earliest decision or user-task
        // form that needs it) and rewrite the decision conditions - on a COPY of the step list, never
        // mutating the shared model (the glue generator runs after this one and must still see the
        // original relation.field condition). Also insert a field-loader service task before an
        // own-field decision, an expire-date-loader service task before a user task with an `expire:`
        // timer, and writer/setter service tasks after a user task with editable fields or a
        // setRelationField.
        List<StepIntent> steps =
                augmentWithResolvers(process.getSteps(), resolvers, fieldLoads, timerLoads, ownFieldPascalCase, writerByTask, setterByTask);
        List<String> effectiveSteps = buildEffectiveStepIds(steps);
        List<SequenceFlow> flows = buildSequenceFlows(steps, effectiveSteps);
        // Boundary timers on user tasks (timeout: non-cancelling reminder; expire: cancelling,
        // date-field-driven). Their outgoing flow to `then` joins the sequence flows; the branch steps
        // are declared steps the author routes around with `next`, like decision branches.
        List<BoundaryTimer> boundaryTimers = collectBoundaryTimers(steps);
        for (BoundaryTimer timer : boundaryTimers) {
            flows.add(new SequenceFlow("flow_" + timer.id() + "_then", timer.id(), effectiveTarget(timer.thenTarget(), steps), null));
        }
        String processId = process.getName();
        StringBuilder sb = new StringBuilder(4096);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"");
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        sb.append(" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"");
        sb.append(" xmlns:flowable=\"http://flowable.org/bpmn\"");
        sb.append(" xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\"");
        sb.append(" xmlns:omgdc=\"http://www.omg.org/spec/DD/20100524/DC\"");
        sb.append(" xmlns:omgdi=\"http://www.omg.org/spec/DD/20100524/DI\"");
        sb.append(" typeLanguage=\"http://www.w3.org/2001/XMLSchema\"");
        sb.append(" expressionLanguage=\"http://www.w3.org/1999/XPath\"");
        sb.append(" targetNamespace=\"http://www.flowable.org/processdef\">\n");
        // Message definitions live at the definitions level; each wait step's catch event references
        // one by id. The name is process + step (unique per file; correlation additionally scopes by
        // process-instance id, so the compound name is for readability, not disambiguation).
        for (StepIntent step : steps) {
            if ("wait".equals(step.getKind()) && step.getName() != null) {
                String message = ProcessWaitSupport.messageName(processId, step.getName());
                sb.append("  <message id=\"")
                  .append(escapeXmlAttribute(message))
                  .append("\" name=\"")
                  .append(escapeXmlAttribute(message))
                  .append("\"></message>\n");
            }
        }
        sb.append("  <process id=\"")
          .append(escapeXmlAttribute(processId))
          .append("\" name=\"")
          .append(escapeXmlAttribute(IntentNaming.humanize(processId)))
          .append("\" isExecutable=\"true\">\n");
        sb.append("    <startEvent id=\"")
          .append(START_ID)
          .append("\"></startEvent>\n");
        for (StepIntent step : steps) {
            if (step.getName() == null || step.getName()
                                              .isBlank()) {
                continue;
            }
            appendStepElement(sb, step, rolesByLowerName, projectName, processId, candidateGroupsExtra);
        }
        for (BoundaryTimer timer : boundaryTimers) {
            appendBoundaryTimer(sb, timer);
        }
        sb.append("    <endEvent id=\"")
          .append(END_ID)
          .append("\"></endEvent>\n");
        writeSequenceFlows(sb, flows);
        sb.append("  </process>\n");
        appendBpmnDiagram(sb, processId, effectiveSteps, flows, steps, boundaryTimers);
        sb.append("</definitions>\n");
        return sb.toString();
    }

    /**
     * A boundary timer attached to a user task: {@code timeout:} is non-cancelling (the task stays; a
     * reminder/escalation branch runs alongside) with a literal {@code timeDuration}; {@code expire:}
     * is cancelling (the task is withdrawn) with a {@code timeDate} bound to the process variable the
     * expire date loader publishes at task entry.
     */
    private record BoundaryTimer(String id, String attachedTo, boolean cancelActivity, String timerType, String timeExpression,
            String thenTarget) {
    }

    /** The boundary timers declared across the (augmented) step list, in declaration order. */
    private static List<BoundaryTimer> collectBoundaryTimers(List<StepIntent> steps) {
        List<BoundaryTimer> timers = new ArrayList<>();
        for (StepIntent step : steps) {
            if (!"userTask".equals(step.getKind()) || step.getName() == null) {
                continue;
            }
            String after = ProcessTimerSupport.timerAttribute(step, "timeout", "after");
            String timeoutThen = ProcessTimerSupport.timerAttribute(step, "timeout", "then");
            if (after != null && timeoutThen != null) {
                timers.add(new BoundaryTimer(step.getName() + "Timeout", step.getName(), false, "timeDuration", after, timeoutThen));
            }
            String until = ProcessTimerSupport.timerAttribute(step, "expire", "until");
            String expireThen = ProcessTimerSupport.timerAttribute(step, "expire", "then");
            if (until != null && expireThen != null) {
                timers.add(new BoundaryTimer(step.getName() + "Expire", step.getName(), true, "timeDate",
                        "${" + ProcessTimerSupport.expireVariable(step.getName()) + "}", expireThen));
            }
        }
        return timers;
    }

    private static void appendBoundaryTimer(StringBuilder sb, BoundaryTimer timer) {
        sb.append("    <boundaryEvent id=\"")
          .append(escapeXmlAttribute(timer.id()))
          .append("\" attachedToRef=\"")
          .append(escapeXmlAttribute(timer.attachedTo()))
          .append("\" cancelActivity=\"")
          .append(timer.cancelActivity())
          .append("\">\n");
        sb.append("      <timerEventDefinition>\n");
        sb.append("        <")
          .append(timer.timerType())
          .append(">")
          .append(escapeXmlAttribute(timer.timeExpression()))
          .append("</")
          .append(timer.timerType())
          .append(">\n");
        sb.append("      </timerEventDefinition>\n");
        sb.append("    </boundaryEvent>\n");
    }

    /**
     * Produce a render-only step list: before each step that anchors a {@code relation.field} resolver
     * (a decision condition or a user-task form referencing the path), the Java service task (the
     * resolver) is inserted; every decision is replaced by a copy whose condition is rewritten to test
     * the resolved variables. A resolver is inserted once - at its anchor step - and the variable it
     * sets persists, so a decision downstream of the inserting step still resolves correctly even
     * though its own condition is what is rewritten. Original steps otherwise pass through untouched.
     */
    private static List<StepIntent> augmentWithResolvers(List<StepIntent> steps, List<Resolver> resolvers, List<FieldLoad> fieldLoads,
            List<TimerLoad> timerLoads, Map<String, String> ownFieldPascalCase, Map<String, String> writerByTask,
            Map<String, String> setterByTask) {
        List<StepIntent> result = new ArrayList<>(steps.size());
        for (StepIntent step : steps) {
            for (Resolver resolver : resolvers) {
                if (step.getName() != null && step.getName()
                                                  .equals(resolver.beforeStep())) {
                    // The task id is the lower-camel form of the handler so it matches the casing of the
                    // authored step ids; the delegate still resolves the PascalCase handler class.
                    result.add(javaServiceTaskStep(IntentNaming.camelCase(resolver.handler()), "gen.events." + resolver.handler()));
                }
            }
            for (FieldLoad load : fieldLoads) {
                if (step.getName() != null && step.getName()
                                                  .equals(load.beforeStep())) {
                    // Load the trigger entity's own fields the decision tests, right before the gateway.
                    result.add(javaServiceTaskStep(IntentNaming.camelCase(load.handler()), "gen.events." + load.handler()));
                }
            }
            for (TimerLoad load : timerLoads) {
                if (step.getName() != null && step.getName()
                                                  .equals(load.beforeStep())) {
                    // Re-read the expire date field at task entry, so the boundary timer arms fresh.
                    result.add(javaServiceTaskStep(IntentNaming.camelCase(load.handler()), "gen.events." + load.handler()));
                }
            }
            boolean userTask = "userTask".equals(step.getKind()) && step.getName() != null;
            // Delegates that run right after a user task, in order: the writer (persist the reviewer's
            // edits) then the setter (set a relation FK). Each falls through linearly into the next, and
            // the original `next` is carried onto the LAST one so downstream routing can't be bypassed.
            List<String> afterTask = new ArrayList<>(2);
            if (userTask && writerByTask.get(step.getName()) != null) {
                afterTask.add(writerByTask.get(step.getName()));
            }
            if (userTask && setterByTask.get(step.getName()) != null) {
                afterTask.add(setterByTask.get(step.getName()));
            }
            if (!afterTask.isEmpty()) {
                String next = stringArg(step, "next");
                result.add(next == null ? step : withoutNext(step));
                for (int i = 0; i < afterTask.size(); i++) {
                    String handler = afterTask.get(i);
                    StepIntent delegateStep = javaServiceTaskStep(IntentNaming.camelCase(handler), "gen.events." + handler);
                    if (i == afterTask.size() - 1 && next != null && !next.isBlank()) {
                        delegateStep.getArgs()
                                    .put("next", next);
                    }
                    result.add(delegateStep);
                }
                continue;
            }
            // Rewrite a decision against ALL process resolvers (the variables are process-global), not
            // just those anchored at this step - the path may have been resolved by an earlier step.
            result.add("decision".equals(step.getKind()) ? rewriteDecision(step, resolvers, ownFieldPascalCase) : step);
        }
        return result;
    }

    /** A copy of a user-task step with its {@code next} routing removed (transferred to the writer). */
    private static StepIntent withoutNext(StepIntent step) {
        StepIntent copy = new StepIntent();
        copy.setName(step.getName());
        copy.setKind(step.getKind());
        Map<String, Object> args = new LinkedHashMap<>(step.getArgs());
        args.remove("next");
        copy.setArgs(args);
        return copy;
    }

    /**
     * A synthetic service-task step bound to the {@code JavaTask} delegate + a client-class handler
     * FQN.
     */
    private static StepIntent javaServiceTaskStep(String name, String handlerFqn) {
        StepIntent step = new StepIntent();
        step.setName(name);
        step.setKind("serviceTask");
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("javaHandler", handlerFqn);
        step.setArgs(args);
        return step;
    }

    /**
     * A copy of the decision step whose {@code if} condition is rewritten to the resolved variables.
     */
    private static StepIntent rewriteDecision(StepIntent decision, List<Resolver> resolvers, Map<String, String> ownFieldPascalCase) {
        StepIntent copy = new StepIntent();
        copy.setName(decision.getName());
        copy.setKind(decision.getKind());
        Map<String, Object> args = new LinkedHashMap<>(decision.getArgs());
        Object condition = args.get("if");
        if (condition != null) {
            args.put("if", rewriteCondition(condition.toString(), resolvers, ownFieldPascalCase));
        }
        copy.setArgs(args);
        return copy;
    }

    /**
     * Rewrite a decision condition for the BPMN: each {@code relation.field} token becomes its resolved
     * variable, then the trigger entity's own fields are upper-cased to the PascalCase process-variable
     * names (the entity JSON seeds the process with PascalCase keys). Word boundaries keep the
     * already-substituted {@code relation_field} variable from being touched by the field pass.
     */
    private static String rewriteCondition(String condition, List<Resolver> resolvers, Map<String, String> ownFieldPascalCase) {
        String result = condition;
        for (Resolver resolver : resolvers) {
            result = result.replace(resolver.token(), resolver.variable());
        }
        for (Map.Entry<String, String> field : ownFieldPascalCase.entrySet()) {
            if (!field.getKey()
                      .equals(field.getValue())) {
                result = result.replaceAll("\\b" + Pattern.quote(field.getKey()) + "\\b",
                        java.util.regex.Matcher.quoteReplacement(field.getValue()));
            }
        }
        return result;
    }

    /**
     * Build the list of step IDs that participate in the default linear flow. {@code decision} steps
     * participate (they are an exclusiveGateway with a default outgoing edge), but {@code end} steps
     * are translated to the canonical end event - their IDs are replaced by {@link #END_ID} in the
     * sequence.
     */
    private static List<String> buildEffectiveStepIds(List<StepIntent> steps) {
        List<String> ids = new ArrayList<>();
        ids.add(START_ID);
        for (StepIntent step : steps) {
            if (step.getName() == null || step.getName()
                                              .isBlank()) {
                continue;
            }
            if ("end".equalsIgnoreCase(step.getKind())) {
                ids.add(END_ID);
            } else {
                ids.add(step.getName());
            }
        }
        ids.add(END_ID);
        return dedupeConsecutive(ids);
    }

    /**
     * Collapse repeated IDs (an author-declared {@code end} step followed by the implicit end leaves a
     * duplicate {@link #END_ID} entry). One end target is enough.
     */
    private static List<String> dedupeConsecutive(List<String> ids) {
        List<String> out = new ArrayList<>(ids.size());
        String previous = null;
        for (String id : ids) {
            if (!Objects.equals(id, previous)) {
                out.add(id);
            }
            previous = id;
        }
        return out;
    }

    private static void appendStepElement(StringBuilder sb, StepIntent step, Map<String, String> rolesByLowerName, String projectName,
            String processName, String candidateGroupsExtra) {
        String kind = step.getKind() == null ? "userTask" : step.getKind();
        switch (kind) {
            case "userTask":
                appendUserTask(sb, step, rolesByLowerName, projectName, candidateGroupsExtra);
                break;
            case "serviceTask":
            case "script":
                appendServiceTask(sb, step, projectName, processName);
                break;
            case "decision":
                appendExclusiveGateway(sb, step);
                break;
            case "wait":
                appendWaitCatchEvent(sb, step, processName);
                break;
            case "end":
                break;
            default:
                LOGGER.warn("Unknown step kind [{}] for step [{}] - rendering as userTask", kind, step.getName());
                appendUserTask(sb, step, rolesByLowerName, projectName, candidateGroupsExtra);
                break;
        }
    }

    private static void appendUserTask(StringBuilder sb, StepIntent step, Map<String, String> rolesByLowerName, String projectName,
            String candidateGroupsExtra) {
        String assignee = stringArg(step, "assignee");
        String form = stringArg(step, "form");
        sb.append("    <userTask id=\"")
          .append(escapeXmlAttribute(step.getName()))
          .append("\" name=\"")
          .append(escapeXmlAttribute(IntentNaming.humanize(step.getName())))
          .append("\"");
        if ("personal".equals(assignee)) {
            // The record's owner, resolved at start time by the trigger listener (identity mapping)
            // into the __personalUser variable - a per-user assignment, not a claimable group.
            sb.append(" flowable:assignee=\"${__personalUser}\"");
        } else if (assignee != null && !assignee.isBlank()) {
            String candidateGroups = resolveCandidateGroup(assignee, rolesByLowerName);
            if (candidateGroupsExtra != null && !candidateGroupsExtra.isBlank()) {
                candidateGroups = candidateGroups + "," + candidateGroupsExtra;
            }
            sb.append(" flowable:candidateGroups=\"")
              .append(escapeXmlAttribute(candidateGroups))
              .append("\"");
        }
        if (form != null && !form.isBlank()) {
            sb.append(" flowable:formKey=\"")
              .append(escapeXmlAttribute(formPageUrl(projectName, form)))
              .append("\"");
        }
        sb.append("></userTask>\n");
    }

    private static void appendServiceTask(StringBuilder sb, StepIntent step, String projectName, String processName) {
        // Five service-task shapes:
        // - a generator-synthesized resolver carries a javaHandler (a client JavaDelegate FQN) -> JavaTask;
        // - an author-declared serviceTask with a `setField` -> JavaTask bound to the gen.events.<Handler>
        // JavaDelegate the glue generator emits (sets a field of the trigger entity to a literal value);
        // - an author-declared serviceTask with a `setRelationField` -> JavaTask bound to the same
        // gen.events.<Handler> JavaDelegate (sets a to-one relation's FK to a seed id);
        // - an author-declared serviceTask with a `call` (a TS handler path) -> JSTask, the path qualified
        // with the project name (the JSTask delegate resolves relative to the registry root);
        // - an author-declared serviceTask with none of the above -> JavaTask bound to a custom.<Step> Java
        // handler that ServiceTaskHandlerGenerator scaffolds once under custom/ for the developer.
        // - an author-declared serviceTask with a `delegate` -> a client JavaDelegate FQN bound via
        // flowable:class (NOT ${JavaTask}), so Flowable injects the author's `fields` as delegate fields;
        // this is how a reusable, parameterized delegate (e.g. a document number generator) is invoked.
        String delegate = stringArg(step, "delegate");
        if (delegate != null && !delegate.isBlank()) {
            appendDelegateServiceTask(sb, step, delegate.trim());
            return;
        }
        String javaHandler = stringArg(step, "javaHandler");
        String setField = stringArg(step, "setField");
        String setRelationField = stringArg(step, "setRelationField");
        String call = stringArg(step, "call");
        boolean java;
        String handlerValue;
        if (javaHandler != null && !javaHandler.isBlank()) {
            java = true;
            handlerValue = javaHandler;
        } else if (setField != null && !setField.isBlank() || setRelationField != null && !setRelationField.isBlank()) {
            java = true;
            handlerValue = "gen.events." + SetFieldSupport.className(processName, step.getName());
        } else if (call != null && !call.isBlank()) {
            java = false;
            handlerValue = qualifyHandlerPath(projectName, call);
        } else {
            java = true;
            handlerValue = "custom." + IntentNaming.pascalCase(step.getName());
        }
        sb.append("    <serviceTask id=\"")
          .append(escapeXmlAttribute(step.getName()))
          .append("\" name=\"")
          .append(escapeXmlAttribute(IntentNaming.humanize(step.getName())))
          .append("\" flowable:async=\"true\" flowable:delegateExpression=\"")
          .append(java ? "${JavaTask}" : "${JSTask}")
          .append("\">\n");
        if (handlerValue != null && !handlerValue.isBlank()) {
            sb.append("      <extensionElements>\n");
            sb.append("        <flowable:field name=\"handler\">\n");
            sb.append("          <flowable:string><![CDATA[")
              .append(handlerValue)
              .append("]]></flowable:string>\n");
            sb.append("        </flowable:field>\n");
            sb.append("      </extensionElements>\n");
        }
        sb.append("    </serviceTask>\n");
    }

    /**
     * Emit a service task bound to an author-named client
     * {@link org.flowable.engine.delegate.JavaDelegate} via {@code flowable:class} (resolved through
     * the client class loader by {@code BpmFlowableConfig}'s {@code ClientAwareClassLoader}). Unlike
     * the {@code ${JavaTask}} dispatcher - which only forwards the {@code handler} field and
     * instantiates the target with a no-arg constructor - {@code flowable:class} lets Flowable inject
     * the declared {@code fields} into the delegate, so a reusable, parameterized delegate can be
     * configured per step.
     */
    private static void appendDelegateServiceTask(StringBuilder sb, StepIntent step, String delegateClass) {
        sb.append("    <serviceTask id=\"")
          .append(escapeXmlAttribute(step.getName()))
          .append("\" name=\"")
          .append(escapeXmlAttribute(IntentNaming.humanize(step.getName())))
          .append("\" flowable:async=\"true\" flowable:class=\"")
          .append(escapeXmlAttribute(delegateClass))
          .append("\">\n");
        Map<String, String> fields = delegateFields(step);
        if (!fields.isEmpty()) {
            sb.append("      <extensionElements>\n");
            for (Map.Entry<String, String> field : fields.entrySet()) {
                sb.append("        <flowable:field name=\"")
                  .append(escapeXmlAttribute(field.getKey()))
                  .append("\">\n");
                sb.append("          <flowable:string><![CDATA[")
                  .append(field.getValue())
                  .append("]]></flowable:string>\n");
                sb.append("        </flowable:field>\n");
            }
            sb.append("      </extensionElements>\n");
        }
        sb.append("    </serviceTask>\n");
    }

    /**
     * The delegate {@code fields} map (name -> literal string value) an author declared on a
     * {@code delegate} service task, preserving declaration order. A non-map {@code fields} arg
     * (already rejected by the parser) yields an empty map.
     */
    private static Map<String, String> delegateFields(StepIntent step) {
        Map<String, String> fields = new LinkedHashMap<>();
        Object raw = step.getArgs() == null ? null
                : step.getArgs()
                      .get("fields");
        if (raw instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    fields.put(entry.getKey()
                                    .toString(),
                            entry.getValue()
                                 .toString());
                }
            }
        }
        return fields;
    }

    /**
     * Qualify a project-relative JS handler path (e.g. {@code custom/notify-member.ts}) with the
     * project name, since the {@code ${JSTask}} delegate resolves it relative to the registry root
     * ({@code /registry/public}). Paths that already start with the project segment are left as-is.
     */
    private static String qualifyHandlerPath(String projectName, String call) {
        if (call == null || call.isBlank() || projectName == null || projectName.isBlank()) {
            return call;
        }
        String trimmed = call.startsWith("/") ? call.substring(1) : call;
        if (trimmed.equals(projectName) || trimmed.startsWith(projectName + "/")) {
            return trimmed;
        }
        return projectName + "/" + trimmed;
    }

    /**
     * A {@code wait} step: a message intermediate catch event that parks the process until the
     * generated wait listener (see
     * {@link org.eclipse.dirigible.components.intent.generator.ProcessWaitSupport}) correlates the
     * message on the resuming entity event. The message definition itself is emitted at the definitions
     * level by {@code render}.
     */
    private static void appendWaitCatchEvent(StringBuilder sb, StepIntent step, String processName) {
        sb.append("    <intermediateCatchEvent id=\"")
          .append(escapeXmlAttribute(step.getName()))
          .append("\" name=\"")
          .append(escapeXmlAttribute(IntentNaming.humanize(step.getName())))
          .append("\">\n");
        sb.append("      <messageEventDefinition messageRef=\"")
          .append(escapeXmlAttribute(ProcessWaitSupport.messageName(processName, step.getName())))
          .append("\"></messageEventDefinition>\n");
        sb.append("    </intermediateCatchEvent>\n");
    }

    private static void appendExclusiveGateway(StringBuilder sb, StepIntent step) {
        sb.append("    <exclusiveGateway id=\"")
          .append(escapeXmlAttribute(step.getName()))
          .append("\" name=\"")
          .append(escapeXmlAttribute(IntentNaming.humanize(step.getName())))
          .append("\" default=\"")
          .append(escapeXmlAttribute("flow_" + step.getName() + "_default"))
          .append("\"></exclusiveGateway>\n");
    }

    /** A resolved sequence flow: stable id, source/target element ids, and an optional condition. */
    private record SequenceFlow(String id, String source, String target, String condition) {
    }

    /**
     * Build the sequence flows. The default is a linear chain through {@link #buildEffectiveStepIds}.
     * Decision steps emit a conditioned flow to {@code args.then} and route their gateway-default flow
     * to {@code args.else} when declared (so the conditioned branch can actually be skipped); without
     * an {@code else} the default falls through to the next step in the chain. A non-decision step with
     * an explicit {@code args.next} routes its outgoing flow to that step (or {@code end}) instead of
     * the next in the chain - this is how two branch steps of a decision converge on a common successor
     * (e.g. {@code activate} and {@code reject} both flowing to {@code done}) without the first
     * silently falling through into the second.
     */
    private static List<SequenceFlow> buildSequenceFlows(List<StepIntent> steps, List<String> effectiveIds) {
        List<SequenceFlow> flows = new ArrayList<>();
        for (int i = 0; i < effectiveIds.size() - 1; i++) {
            String source = effectiveIds.get(i);
            String target = effectiveIds.get(i + 1);
            String flowId;
            StepIntent decision = decisionOf(source, steps);
            if (decision != null) {
                flowId = "flow_" + source + "_default";
                String elseTarget = stringArg(decision, "else");
                if (elseTarget != null && !elseTarget.isBlank()) {
                    target = effectiveTarget(elseTarget, steps);
                }
            } else {
                StepIntent sourceStep = stepByName(source, steps);
                String next = sourceStep == null ? null : stringArg(sourceStep, "next");
                if (next != null && !next.isBlank()) {
                    target = effectiveTarget(next, steps);
                }
                flowId = "flow_" + source + "_" + target;
            }
            flows.add(new SequenceFlow(flowId, source, target, null));
        }
        for (StepIntent step : steps) {
            if (!"decision".equalsIgnoreCase(step.getKind())) {
                continue;
            }
            String condition = stringArg(step, "if");
            String thenTarget = stringArg(step, "then");
            if (condition == null || condition.isBlank() || thenTarget == null || thenTarget.isBlank()) {
                LOGGER.warn("Decision [{}] is missing `if` or `then` - skipping conditioned outgoing flow", step.getName());
                continue;
            }
            flows.add(new SequenceFlow("flow_" + step.getName() + "_then", step.getName(), effectiveTarget(thenTarget, steps), condition));
        }
        return flows;
    }

    private static void writeSequenceFlows(StringBuilder sb, List<SequenceFlow> flows) {
        for (SequenceFlow flow : flows) {
            sb.append("    <sequenceFlow id=\"")
              .append(escapeXmlAttribute(flow.id()))
              .append("\" sourceRef=\"")
              .append(escapeXmlAttribute(flow.source()))
              .append("\" targetRef=\"")
              .append(escapeXmlAttribute(flow.target()))
              .append("\">");
            if (flow.condition() != null) {
                sb.append("\n      <conditionExpression xsi:type=\"tFormalExpression\"><![CDATA[${")
                  .append(flow.condition())
                  .append("}]]></conditionExpression>\n    ");
            }
            sb.append("</sequenceFlow>\n");
        }
    }

    /**
     * Resolve a {@code then}/{@code else} target to its BPMN element id: the literal {@code end} or a
     * step of kind {@code end} maps to the canonical end event.
     */
    private static String effectiveTarget(String targetName, List<StepIntent> steps) {
        if (END_ID.equalsIgnoreCase(targetName)) {
            return END_ID;
        }
        for (StepIntent step : steps) {
            if (targetName.equals(step.getName()) && "end".equalsIgnoreCase(step.getKind())) {
                return END_ID;
            }
        }
        return targetName;
    }

    /** The decision step with the given id, or null when the id is not a decision. */
    private static StepIntent decisionOf(String stepId, List<StepIntent> steps) {
        for (StepIntent step : steps) {
            if (stepId.equals(step.getName()) && "decision".equalsIgnoreCase(step.getKind())) {
                return step;
            }
        }
        return null;
    }

    // ----- BPMN diagram interchange (bpmndi) ---------------------------------------------------

    /** X of the first (start) column's centre. */
    private static final int FIRST_NODE_CENTER_X = 100;
    /** Horizontal distance between the centres of adjacent rank columns (task width 100 + gap). */
    private static final int COLUMN_PITCH = 180;
    /** Y of the centre lane (lane offset 0); branches fan out above and below it. */
    private static final int CENTER_Y = 260;
    /** Vertical distance between adjacent lanes. */
    private static final int LANE_HEIGHT = 130;

    /**
     * Append the {@code bpmndi:BPMNDiagram} block. The Flowable/Oryx modeler renders the canvas
     * <b>only</b> from this diagram interchange (a process with no {@code BPMNShape}s opens empty), so
     * it is mandatory.
     *
     * <p>
     * The layout is a deterministic <b>layered (Sugiyama-style) placement</b> rather than a single
     * line: each node's <b>column</b> (X) is its longest-path distance from the start event, so
     * parallel branches of a gateway share a column and the flow always reads left-to-right; each
     * node's <b>lane</b> (Y) is assigned per column by the barycentre of its predecessors' lanes and
     * centred on {@link #CENTER_Y}, so branches fan out above and below the main line instead of piling
     * onto one of two fixed lanes. Edges are routed <b>orthogonally</b> (an L/Z of right-angle
     * segments) when the endpoints are on different lanes, and as a straight segment when they line up
     * - the same shape a modeler draws by hand. The whole computation is a pure function of the step
     * graph, so re-generation stays byte-stable; the modeler re-routes on first manual edit.
     */
    private static void appendBpmnDiagram(StringBuilder sb, String processId, List<String> effectiveIds, List<SequenceFlow> flows,
            List<StepIntent> steps, List<BoundaryTimer> boundaryTimers) {
        // A boundary event has no column of its own - it rides its host task's border. For the layered
        // layout its outgoing branch still needs a rank, so each boundary flow contributes a pseudo-flow
        // from the HOST task to the branch target (the real boundary flow is invisible to the layout:
        // its source is not a laid-out node). The real flow keeps its own edge, drawn from the boundary
        // shape added after the layout.
        List<SequenceFlow> layoutFlows = new ArrayList<>(flows);
        for (BoundaryTimer timer : boundaryTimers) {
            layoutFlows.add(new SequenceFlow("layout_" + timer.id(), timer.attachedTo(), effectiveTarget(timer.thenTarget(), steps), null));
        }
        Map<String, int[]> bounds = layout(effectiveIds, layoutFlows, steps);
        // Boundary shapes overlap the host task's bottom edge (the modeler convention), fanning left
        // from the bottom-right corner when a task carries both a timeout and an expire timer.
        Map<String, Integer> timersOnHost = new HashMap<>();
        for (BoundaryTimer timer : boundaryTimers) {
            int[] host = bounds.get(timer.attachedTo());
            if (host == null) {
                continue;
            }
            int index = timersOnHost.merge(timer.attachedTo(), 1, Integer::sum) - 1;
            bounds.put(timer.id(), new int[] {host[0] + host[2] - 15 - index * 40, host[1] + host[3] - 15, 31, 31});
        }

        sb.append("  <bpmndi:BPMNDiagram id=\"BPMNDiagram_")
          .append(escapeXmlAttribute(processId))
          .append("\">\n");
        sb.append("    <bpmndi:BPMNPlane bpmnElement=\"")
          .append(escapeXmlAttribute(processId))
          .append("\" id=\"BPMNPlane_")
          .append(escapeXmlAttribute(processId))
          .append("\">\n");
        for (Map.Entry<String, int[]> entry : bounds.entrySet()) {
            int[] b = entry.getValue();
            sb.append("      <bpmndi:BPMNShape bpmnElement=\"")
              .append(escapeXmlAttribute(entry.getKey()))
              .append("\" id=\"BPMNShape_")
              .append(escapeXmlAttribute(entry.getKey()))
              .append("\">\n        <omgdc:Bounds height=\"")
              .append(b[3])
              .append("\" width=\"")
              .append(b[2])
              .append("\" x=\"")
              .append(b[0])
              .append("\" y=\"")
              .append(b[1])
              .append("\"/>\n      </bpmndi:BPMNShape>\n");
        }
        for (SequenceFlow flow : flows) {
            int[] source = bounds.get(flow.source());
            int[] target = bounds.get(flow.target());
            if (source == null || target == null) {
                continue;
            }
            sb.append("      <bpmndi:BPMNEdge bpmnElement=\"")
              .append(escapeXmlAttribute(flow.id()))
              .append("\" id=\"BPMNEdge_")
              .append(escapeXmlAttribute(flow.id()))
              .append("\">\n");
            for (int[] point : edgeWaypoints(source, target)) {
                sb.append("        <omgdi:waypoint x=\"")
                  .append(point[0])
                  .append("\" y=\"")
                  .append(point[1])
                  .append("\"/>\n");
            }
            sb.append("      </bpmndi:BPMNEdge>\n");
        }
        sb.append("    </bpmndi:BPMNPlane>\n");
        sb.append("  </bpmndi:BPMNDiagram>\n");
    }

    /**
     * Compute the {@code [x, y, width, height]} bounds of every node with a layered layout: column by
     * longest-path rank from the start event, lane by predecessor-barycentre within the column. The
     * returned map preserves {@code effectiveIds} order so the emitted shapes are byte-stable.
     */
    private static Map<String, int[]> layout(List<String> effectiveIds, List<SequenceFlow> flows, List<StepIntent> steps) {
        // Forward adjacency and predecessor lists, restricted to nodes that actually have a shape.
        Set<String> nodes = new LinkedHashSet<>(effectiveIds);
        Map<String, List<String>> predecessors = new LinkedHashMap<>();
        for (String id : effectiveIds) {
            predecessors.put(id, new ArrayList<>());
        }
        for (SequenceFlow flow : flows) {
            if (nodes.contains(flow.source()) && nodes.contains(flow.target()) && !flow.source()
                                                                                       .equals(flow.target())) {
                predecessors.get(flow.target())
                            .add(flow.source());
            }
        }

        Map<String, Integer> rank = rankByLongestPath(effectiveIds, flows, nodes);
        // Group nodes by rank in a stable (effectiveIds) order, then compress ranks to contiguous
        // columns so an empty rank leaves no visual gap.
        Map<Integer, List<String>> byRank = new java.util.TreeMap<>();
        for (String id : effectiveIds) {
            byRank.computeIfAbsent(rank.get(id), k -> new ArrayList<>())
                  .add(id);
        }
        Map<Integer, Integer> columnOf = new HashMap<>();
        int column = 0;
        for (Integer r : byRank.keySet()) {
            columnOf.put(r, column++);
        }

        // Lane assignment, one column at a time in rank order: sort a column's nodes by the average
        // lane of their already-placed predecessors (barycentre - the classic crossing-reduction
        // heuristic), then spread them symmetrically around the centre lane (offset 0).
        Map<String, Double> laneOf = new HashMap<>();
        for (List<String> columnNodes : byRank.values()) {
            columnNodes.sort(java.util.Comparator.<String>comparingDouble(id -> barycentre(id, predecessors, laneOf))
                                                 .thenComparingInt(effectiveIds::indexOf));
            double first = -(columnNodes.size() - 1) / 2.0;
            for (int i = 0; i < columnNodes.size(); i++) {
                laneOf.put(columnNodes.get(i), first + i);
            }
        }

        Map<String, int[]> bounds = new LinkedHashMap<>();
        for (String id : effectiveIds) {
            if (bounds.containsKey(id)) {
                continue;
            }
            int[] size = nodeSize(id, steps);
            int centerX = FIRST_NODE_CENTER_X + columnOf.get(rank.get(id)) * COLUMN_PITCH;
            int centerY = CENTER_Y + (int) Math.round(laneOf.get(id) * LANE_HEIGHT);
            bounds.put(id, new int[] {centerX - size[0] / 2, centerY - size[1] / 2, size[0], size[1]});
        }
        return bounds;
    }

    /**
     * Longest-path rank of each node from {@link #START_ID}, computed by Bellman-Ford-style relaxation
     * (bounded to {@code |nodes|} passes so a stray back edge cannot loop forever). The end event is
     * pinned to the deepest rank so nothing sits to its right.
     */
    private static Map<String, Integer> rankByLongestPath(List<String> effectiveIds, List<SequenceFlow> flows, Set<String> nodes) {
        Map<String, Integer> rank = new HashMap<>();
        for (String id : effectiveIds) {
            rank.put(id, 0);
        }
        for (int pass = 0; pass < nodes.size(); pass++) {
            boolean changed = false;
            for (SequenceFlow flow : flows) {
                Integer source = rank.get(flow.source());
                Integer target = rank.get(flow.target());
                if (source == null || target == null || flow.source()
                                                            .equals(flow.target())) {
                    continue;
                }
                if (target < source + 1) {
                    rank.put(flow.target(), source + 1);
                    changed = true;
                }
            }
            if (!changed) {
                break;
            }
        }
        int deepest = rank.values()
                          .stream()
                          .mapToInt(Integer::intValue)
                          .max()
                          .orElse(0);
        rank.put(END_ID, deepest);
        return rank;
    }

    /** Average lane of a node's already-placed predecessors, or {@code 0} when none are placed yet. */
    private static double barycentre(String id, Map<String, List<String>> predecessors, Map<String, Double> laneOf) {
        double sum = 0;
        int count = 0;
        for (String predecessor : predecessors.getOrDefault(id, List.of())) {
            Double lane = laneOf.get(predecessor);
            if (lane != null) {
                sum += lane;
                count++;
            }
        }
        return count == 0 ? 0 : sum / count;
    }

    /**
     * Waypoints for an edge from the {@code source} bounds to the {@code target} bounds: a straight
     * right-to-left segment when the two shapes share a lane, otherwise an orthogonal L/Z stepping out
     * of the source's right side, across to the midpoint column, up or down to the target's lane, and
     * into the target's left side.
     */
    private static List<int[]> edgeWaypoints(int[] source, int[] target) {
        int x1 = source[0] + source[2];
        int y1 = source[1] + source[3] / 2;
        int x2 = target[0];
        int y2 = target[1] + target[3] / 2;
        if (y1 == y2) {
            return List.of(new int[] {x1, y1}, new int[] {x2, y2});
        }
        int midX = (x1 + x2) / 2;
        return List.of(new int[] {x1, y1}, new int[] {midX, y1}, new int[] {midX, y2}, new int[] {x2, y2});
    }

    /** Width/height of a node's shape by its element id / step kind. */
    private static int[] nodeSize(String id, List<StepIntent> steps) {
        if (START_ID.equals(id)) {
            return new int[] {30, 30};
        }
        if (END_ID.equals(id)) {
            return new int[] {28, 28};
        }
        StepIntent step = stepByName(id, steps);
        String kind = step == null ? "userTask" : step.getKind();
        if ("decision".equalsIgnoreCase(kind)) {
            return new int[] {40, 40};
        }
        if ("wait".equalsIgnoreCase(kind)) {
            return new int[] {31, 31};
        }
        return new int[] {100, 80};
    }

    private static StepIntent stepByName(String name, List<StepIntent> steps) {
        for (StepIntent step : steps) {
            if (name.equals(step.getName())) {
                return step;
            }
        }
        return null;
    }

    private static String stringArg(StepIntent step, String key) {
        Map<String, Object> args = step.getArgs();
        if (args == null) {
            return null;
        }
        Object value = args.get(key);
        return value == null ? null : value.toString();
    }

    private static String escapeXmlAttribute(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(raw.length() + 8);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&apos;");
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        return sb.toString();
    }
}
