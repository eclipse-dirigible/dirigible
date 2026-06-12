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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.dirigible.components.intent.generator.IntentGenerationContext;
import org.eclipse.dirigible.components.intent.generator.IntentTargetGenerator;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.ProcessIntent;
import org.eclipse.dirigible.components.intent.model.StepIntent;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Emits one {@code gen/<process>.bpmn} per {@link ProcessIntent} declared in the intent. The output
 * is a minimal Flowable-flavoured BPMN 2.0 document:
 * <ul>
 * <li>one {@code <startEvent>} and one {@code <endEvent>} per process</li>
 * <li>{@code userTask} ->
 * {@code <userTask flowable:candidateGroups="..." flowable:formKey="..."/>}</li>
 * <li>{@code serviceTask} / {@code script} ->
 * {@code <serviceTask flowable:delegateExpression="${JSTask}">} with the {@code handler} extension
 * element pointing at {@code args.call}</li>
 * <li>{@code decision} -> {@code <exclusiveGateway>} plus a conditioned outgoing flow to
 * {@code args.then} and a default flow to the next step</li>
 * <li>{@code end} -> the canonical end event (no separate element; the outgoing flow targets the
 * single {@code <endEvent>})</li>
 * </ul>
 *
 * <p>
 * <b>No BPMN diagram block ({@code bpmndi}) is emitted.</b> Flowable executes without it; the BPMN
 * editor in the IDE auto-lays out a missing diagram on first open. Skipping it keeps the generator
 * deterministic and trivially stable across re-runs (no spurious x/y churn).
 *
 * <p>
 * <b>Path-free references.</b> Per the CLAUDE.md "no template-engine paths" rule,
 * {@code flowable:formKey} carries the bare form name from {@code args.form}, not a rendered HTML
 * URL; the deployment-time form-key resolver maps it to a generated page. {@code args.call} is
 * passed through verbatim - it should reference a hand-authored handler under {@code custom/}, not
 * a template output.
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
        IRepository repository = context.getRepository();
        String genRoot = context.getGenRoot();
        Set<String> seenFiles = new HashSet<>();
        for (ProcessIntent process : model.getProcesses()) {
            if (process.getName() == null || process.getName()
                                                    .isBlank()) {
                LOGGER.warn("Skipping unnamed process in intent [{}]", context.getIntent()
                                                                              .getName());
                continue;
            }
            String fileName = process.getName() + ".bpmn";
            if (!seenFiles.add(fileName)) {
                LOGGER.warn("Duplicate process [{}] in intent [{}] - keeping the first occurrence", process.getName(), context.getIntent()
                                                                                                                              .getName());
                continue;
            }
            String content = render(process);
            writeResource(repository, genRoot + "/" + fileName, content);
        }
    }

    private static String render(ProcessIntent process) {
        List<StepIntent> steps = process.getSteps();
        List<String> effectiveSteps = buildEffectiveStepIds(steps);
        StringBuilder sb = new StringBuilder(2048);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"");
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        sb.append(" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"");
        sb.append(" xmlns:flowable=\"http://flowable.org/bpmn\"");
        sb.append(" typeLanguage=\"http://www.w3.org/2001/XMLSchema\"");
        sb.append(" expressionLanguage=\"http://www.w3.org/1999/XPath\"");
        sb.append(" targetNamespace=\"http://www.flowable.org/processdef\">\n");
        sb.append("  <process id=\"")
          .append(escapeXmlAttribute(process.getName()))
          .append("\" name=\"")
          .append(escapeXmlAttribute(process.getName()))
          .append("\" isExecutable=\"true\">\n");
        sb.append("    <startEvent id=\"")
          .append(START_ID)
          .append("\"></startEvent>\n");
        for (StepIntent step : steps) {
            if (step.getName() == null || step.getName()
                                              .isBlank()) {
                continue;
            }
            appendStepElement(sb, step);
        }
        sb.append("    <endEvent id=\"")
          .append(END_ID)
          .append("\"></endEvent>\n");
        appendSequenceFlows(sb, steps, effectiveSteps);
        sb.append("  </process>\n");
        sb.append("</definitions>\n");
        return sb.toString();
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

    private static void appendStepElement(StringBuilder sb, StepIntent step) {
        String kind = step.getKind() == null ? "userTask" : step.getKind();
        switch (kind) {
            case "userTask":
                appendUserTask(sb, step);
                break;
            case "serviceTask":
            case "script":
                appendServiceTask(sb, step);
                break;
            case "decision":
                appendExclusiveGateway(sb, step);
                break;
            case "end":
                break;
            default:
                LOGGER.warn("Unknown step kind [{}] for step [{}] - rendering as userTask", kind, step.getName());
                appendUserTask(sb, step);
                break;
        }
    }

    private static void appendUserTask(StringBuilder sb, StepIntent step) {
        String assignee = stringArg(step, "assignee");
        String form = stringArg(step, "form");
        sb.append("    <userTask id=\"")
          .append(escapeXmlAttribute(step.getName()))
          .append("\" name=\"")
          .append(escapeXmlAttribute(step.getName()))
          .append("\"");
        if (assignee != null && !assignee.isBlank()) {
            sb.append(" flowable:candidateGroups=\"")
              .append(escapeXmlAttribute(assignee))
              .append("\"");
        }
        if (form != null && !form.isBlank()) {
            sb.append(" flowable:formKey=\"")
              .append(escapeXmlAttribute(form))
              .append("\"");
        }
        sb.append("></userTask>\n");
    }

    private static void appendServiceTask(StringBuilder sb, StepIntent step) {
        String call = stringArg(step, "call");
        sb.append("    <serviceTask id=\"")
          .append(escapeXmlAttribute(step.getName()))
          .append("\" name=\"")
          .append(escapeXmlAttribute(step.getName()))
          .append("\" flowable:async=\"true\" flowable:delegateExpression=\"${JSTask}\">\n");
        if (call != null && !call.isBlank()) {
            sb.append("      <extensionElements>\n");
            sb.append("        <flowable:field name=\"handler\">\n");
            sb.append("          <flowable:string><![CDATA[")
              .append(call)
              .append("]]></flowable:string>\n");
            sb.append("        </flowable:field>\n");
            sb.append("      </extensionElements>\n");
        }
        sb.append("    </serviceTask>\n");
    }

    private static void appendExclusiveGateway(StringBuilder sb, StepIntent step) {
        sb.append("    <exclusiveGateway id=\"")
          .append(escapeXmlAttribute(step.getName()))
          .append("\" name=\"")
          .append(escapeXmlAttribute(step.getName()))
          .append("\" default=\"")
          .append("flow_")
          .append(step.getName())
          .append("_default\"></exclusiveGateway>\n");
    }

    /**
     * Emit the sequence flows. The default is a linear chain through {@link #buildEffectiveStepIds}.
     * Decision steps emit an extra conditioned flow to {@code args.then}; the default chained flow to
     * the next step in order remains and is marked as the gateway default by ID convention.
     */
    private static void appendSequenceFlows(StringBuilder sb, List<StepIntent> steps, List<String> effectiveIds) {
        for (int i = 0; i < effectiveIds.size() - 1; i++) {
            String source = effectiveIds.get(i);
            String target = effectiveIds.get(i + 1);
            String flowId;
            if (isDecisionId(source, steps)) {
                flowId = "flow_" + source + "_default";
            } else {
                flowId = "flow_" + source + "_" + target;
            }
            sb.append("    <sequenceFlow id=\"")
              .append(escapeXmlAttribute(flowId))
              .append("\" sourceRef=\"")
              .append(escapeXmlAttribute(source))
              .append("\" targetRef=\"")
              .append(escapeXmlAttribute(target))
              .append("\"></sequenceFlow>\n");
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
            sb.append("    <sequenceFlow id=\"flow_")
              .append(escapeXmlAttribute(step.getName()))
              .append("_then\" sourceRef=\"")
              .append(escapeXmlAttribute(step.getName()))
              .append("\" targetRef=\"")
              .append(escapeXmlAttribute(thenTarget))
              .append("\">\n");
            sb.append("      <conditionExpression xsi:type=\"tFormalExpression\"><![CDATA[${")
              .append(condition)
              .append("}]]></conditionExpression>\n");
            sb.append("    </sequenceFlow>\n");
        }
    }

    private static boolean isDecisionId(String stepId, List<StepIntent> steps) {
        for (StepIntent step : steps) {
            if (stepId.equals(step.getName()) && "decision".equalsIgnoreCase(step.getKind())) {
                return true;
            }
        }
        return false;
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

    private static void writeResource(IRepository repository, String path, String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(bytes);
        } else {
            repository.createResource(path, bytes);
        }
    }
}
