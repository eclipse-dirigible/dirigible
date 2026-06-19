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
import org.eclipse.dirigible.components.intent.generator.ProcessResolverSupport;
import org.eclipse.dirigible.components.intent.generator.ProcessResolverSupport.Resolver;
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
 * shapes opens empty. Nodes are laid out left-to-right on a fixed lane for deterministic,
 * byte-stable output; the modeler re-routes on first manual edit.
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
 * A {@code decision} whose condition references a {@code relation.field} of the trigger entity
 * (e.g. {@code customer.creditLimit > 10000}) gets a {@code JavaTask} resolver service task
 * inserted immediately before the gateway, and the condition is rewritten to test the resolved
 * variable ({@code customer_creditLimit}); the resolver handler itself is generated from the
 * {@code .glue} file (see
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
            context.writeModelFile(fileName, render(process, rolesByLowerName, context.getProjectName(), processResolvers,
                    ownFieldPascalCase(process, byName), candidateGroupsExtra));
        }
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
            Map<String, String> ownFieldPascalCase, String candidateGroupsExtra) {
        // Insert a resolver service task before each decision that needs one and rewrite that
        // decision's condition - on a COPY of the step list, never mutating the shared model (the glue
        // generator runs after this one and must still see the original relation.field condition).
        List<StepIntent> steps = augmentWithResolvers(process.getSteps(), resolvers, ownFieldPascalCase);
        List<String> effectiveSteps = buildEffectiveStepIds(steps);
        List<SequenceFlow> flows = buildSequenceFlows(steps, effectiveSteps);
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
        sb.append("  <process id=\"")
          .append(escapeXmlAttribute(processId))
          .append("\" name=\"")
          .append(escapeXmlAttribute(processId))
          .append("\" isExecutable=\"true\">\n");
        sb.append("    <startEvent id=\"")
          .append(START_ID)
          .append("\"></startEvent>\n");
        for (StepIntent step : steps) {
            if (step.getName() == null || step.getName()
                                              .isBlank()) {
                continue;
            }
            appendStepElement(sb, step, rolesByLowerName, projectName, candidateGroupsExtra);
        }
        sb.append("    <endEvent id=\"")
          .append(END_ID)
          .append("\"></endEvent>\n");
        writeSequenceFlows(sb, flows);
        sb.append("  </process>\n");
        appendBpmnDiagram(sb, processId, effectiveSteps, flows, steps);
        sb.append("</definitions>\n");
        return sb.toString();
    }

    /**
     * Produce a render-only step list: before each decision that references a {@code relation.field}, a
     * Java service task (the resolver) is inserted, and the decision is replaced by a copy whose
     * condition is rewritten to test the resolved variable. Original steps pass through untouched.
     */
    private static List<StepIntent> augmentWithResolvers(List<StepIntent> steps, List<Resolver> resolvers,
            Map<String, String> ownFieldPascalCase) {
        List<StepIntent> result = new ArrayList<>(steps.size());
        for (StepIntent step : steps) {
            if (!"decision".equals(step.getKind())) {
                result.add(step);
                continue;
            }
            List<Resolver> stepResolvers = new ArrayList<>();
            for (Resolver resolver : resolvers) {
                if (step.getName() != null && step.getName()
                                                  .equals(resolver.decisionStep())) {
                    stepResolvers.add(resolver);
                }
            }
            for (Resolver resolver : stepResolvers) {
                result.add(javaServiceTaskStep(resolver.handler(), "gen.events." + resolver.handler()));
            }
            result.add(rewriteDecision(step, stepResolvers, ownFieldPascalCase));
        }
        return result;
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
            String candidateGroupsExtra) {
        String kind = step.getKind() == null ? "userTask" : step.getKind();
        switch (kind) {
            case "userTask":
                appendUserTask(sb, step, rolesByLowerName, projectName, candidateGroupsExtra);
                break;
            case "serviceTask":
            case "script":
                appendServiceTask(sb, step, projectName);
                break;
            case "decision":
                appendExclusiveGateway(sb, step);
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
          .append(escapeXmlAttribute(step.getName()))
          .append("\"");
        if (assignee != null && !assignee.isBlank()) {
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

    private static void appendServiceTask(StringBuilder sb, StepIntent step, String projectName) {
        // Three service-task shapes:
        // - a generator-synthesized resolver carries a javaHandler (a client JavaDelegate FQN) -> JavaTask;
        // - an author-declared serviceTask with a `call` (a TS handler path) -> JSTask, the path qualified
        // with the project name (the JSTask delegate resolves relative to the registry root);
        // - an author-declared serviceTask with NO call -> JavaTask bound to a custom.<Step> Java handler
        // that ServiceTaskHandlerGenerator scaffolds once under custom/ for the developer to implement.
        String javaHandler = stringArg(step, "javaHandler");
        String call = stringArg(step, "call");
        boolean java;
        String handlerValue;
        if (javaHandler != null && !javaHandler.isBlank()) {
            java = true;
            handlerValue = javaHandler;
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
          .append(escapeXmlAttribute(step.getName()))
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

    private static void appendExclusiveGateway(StringBuilder sb, StepIntent step) {
        sb.append("    <exclusiveGateway id=\"")
          .append(escapeXmlAttribute(step.getName()))
          .append("\" name=\"")
          .append(escapeXmlAttribute(step.getName()))
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
     * an {@code else} the default falls through to the next step in the chain.
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

    private static final int LANE_Y = 140;
    private static final int NODE_SPACING = 160;
    private static final int FIRST_NODE_CENTER_X = 100;

    /**
     * Append the {@code bpmndi:BPMNDiagram} block. The Flowable/Oryx modeler renders the canvas
     * <b>only</b> from this diagram interchange (a process with no {@code BPMNShape}s opens empty), so
     * it is mandatory. Nodes are laid out left-to-right along the linear chain at a fixed lane; edges
     * connect the right edge of the source to the left edge of the target. The layout is deterministic
     * so re-generation is byte-stable; the modeler re-routes on first manual edit.
     */
    private static void appendBpmnDiagram(StringBuilder sb, String processId, List<String> effectiveIds, List<SequenceFlow> flows,
            List<StepIntent> steps) {
        Map<String, int[]> bounds = new java.util.LinkedHashMap<>();
        for (int i = 0; i < effectiveIds.size(); i++) {
            String id = effectiveIds.get(i);
            if (bounds.containsKey(id)) {
                continue;
            }
            int[] size = nodeSize(id, steps);
            int centerX = FIRST_NODE_CENTER_X + i * NODE_SPACING;
            int x = centerX - size[0] / 2;
            int y = LANE_Y - size[1] / 2;
            bounds.put(id, new int[] {x, y, size[0], size[1]});
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
            int x1 = source[0] + source[2];
            int y1 = source[1] + source[3] / 2;
            int x2 = target[0];
            int y2 = target[1] + target[3] / 2;
            sb.append("      <bpmndi:BPMNEdge bpmnElement=\"")
              .append(escapeXmlAttribute(flow.id()))
              .append("\" id=\"BPMNEdge_")
              .append(escapeXmlAttribute(flow.id()))
              .append("\">\n        <omgdi:waypoint x=\"")
              .append(x1)
              .append("\" y=\"")
              .append(y1)
              .append("\"/>\n        <omgdi:waypoint x=\"")
              .append(x2)
              .append("\" y=\"")
              .append(y2)
              .append("\"/>\n      </bpmndi:BPMNEdge>\n");
        }
        sb.append("    </bpmndi:BPMNPlane>\n");
        sb.append("  </bpmndi:BPMNDiagram>\n");
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
