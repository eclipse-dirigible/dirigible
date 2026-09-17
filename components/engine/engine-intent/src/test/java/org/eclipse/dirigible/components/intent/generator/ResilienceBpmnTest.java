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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;

import org.eclipse.dirigible.components.intent.generator.bpmn.BpmnIntentGenerator;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IResource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Declarative step resilience in the emitted BPMN - dirigible #6762 and #7056: {@code retry:}
 * becomes a Flowable failed-job retry cycle on the service task, {@code onError:} an error boundary
 * event routed like a decision branch (catching the {@code INTENT_STEP_FAILED} error the runtime
 * conversion raises for the final failed attempt), and a var's {@code clearAfter} an
 * {@code event="end"} execution listener removing the value once its step completes. Both emission
 * paths are covered: the {@code delegate:} task's {@code flowable:class} element and the
 * {@code notify:} task's {@code ${JavaTask}} delegate-expression element, whose cycle has to ride
 * the same extensionElements block its {@code handler} field does. An intent without the keys must
 * emit none of it.
 */
class ResilienceBpmnTest {

    private static final String YAML =
            """
                    name: provisioning
                    entities:
                      - name: ProvisioningStatus
                        function: Setting
                        fields:
                          - { name: id, type: integer, primaryKey: true, generated: true }
                          - { name: name, type: string }
                      - name: TenantApplication
                        fields:
                          - { name: id, type: integer, primaryKey: true, generated: true }
                          - { name: failureMessage, type: string }
                        relations:
                          - { name: Status, kind: manyToOne, to: ProvisioningStatus, function: EntityStatus, init: 1 }
                    processes:
                      - name: TenantProvisioning
                        trigger: { onCreate: TenantApplication }
                        vars:
                          - { name: dbPassword, clearAfter: provisionApp }
                        steps:
                          - { name: createSchema, kind: serviceTask, args: { delegate: custom.SchemaProvisioner, produces: [dbPassword], retry: { count: 3, every: PT30S }, onError: recordFailure } }
                          - { name: provisionApp, kind: serviceTask, args: { delegate: custom.AppProvisioner, uses: [dbPassword], retry: { count: 5, every: PT1M }, onError: recordFailure, next: done } }
                          - { name: recordFailure, kind: serviceTask, args: { setField: failureMessage, value: "{error}", next: markFailed } }
                          - { name: markFailed, kind: serviceTask, args: { setRelationField: Status, value: 3, next: end } }
                          - { name: done, kind: end }
                    """;

    /**
     * The #7056 fixture: the same process with a non-fan-out {@code notify:} step declaring the keys.
     * Its work is the message, so a delivery failure fails the task - which is why the send is one of
     * the two shapes step resilience applies to.
     */
    private static final String SEND_YAML = YAML.replace("      - { name: recordFailure,",
            "      - { name: notifyOwner, kind: serviceTask, args: { notify: { to: owner@example.com, subject: \"Tenant provisioned\","
                    + " body: \"Ready.\" }, retry: { count: 1, every: PT5S }, onError: recordFailure, next: done } }\n"
                    + "      - { name: recordFailure,")
                                                // Rewire only provisionApp (its PT1M cycle is unique) to lead to the send; the send keeps
                                                // its own next: done, so the chain is provisionApp -> notifyOwner -> done, not a self-loop.
                                                .replace("every: PT1M }, onError: recordFailure, next: done",
                                                        "every: PT1M }, onError: recordFailure, next: notifyOwner");

    private static String bpmn(String yaml) {
        IntentModel model = IntentParser.parse(yaml);
        IRepository repository = mock(IRepository.class);
        IResource missing = mock(IResource.class);
        when(repository.getResource(anyString())).thenReturn(missing);
        when(missing.exists()).thenReturn(false);
        IntentGenerationContext context = new IntentGenerationContext(model, "/proj", "proj", "workspace", "app", repository);
        context.setSettings(IntentSettings.scaffold(model));

        new BpmnIntentGenerator().generate(context);

        ArgumentCaptor<String> paths = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<byte[]> contents = ArgumentCaptor.forClass(byte[].class);
        verify(repository, atLeastOnce()).createResource(paths.capture(), contents.capture());
        for (int i = 0; i < paths.getAllValues()
                                 .size(); i++) {
            if (paths.getAllValues()
                     .get(i)
                     .endsWith("/TenantProvisioning.bpmn")) {
                return new String(contents.getAllValues()
                                          .get(i),
                        StandardCharsets.UTF_8);
            }
        }
        throw new AssertionError("the process BPMN was not written; wrote " + paths.getAllValues());
    }

    private static void assertFlow(String bpmn, String source, String target) {
        assertTrue(bpmn.contains("sourceRef=\"" + source + "\" targetRef=\"" + target + "\""),
                "expected a flow " + source + " -> " + target + " in:\n" + bpmn);
    }

    @Test
    void retryBecomesAFailedJobRetryTimeCycleOnTheDelegateTask() {
        String bpmn = bpmn(YAML);

        // R<n> counts TOTAL attempts, so count: 3 further attempts = R4.
        assertTrue(bpmn.contains("<flowable:failedJobRetryTimeCycle>R4/PT30S</flowable:failedJobRetryTimeCycle>"),
                "count: 3 must emit an R4 cycle in:\n" + bpmn);
        assertTrue(bpmn.contains("<flowable:failedJobRetryTimeCycle>R6/PT1M</flowable:failedJobRetryTimeCycle>"),
                "count: 5 must emit an R6 cycle in:\n" + bpmn);
        int task = bpmn.indexOf("<serviceTask id=\"createSchema\"");
        int cycle = bpmn.indexOf("R4/PT30S");
        int nextTask = bpmn.indexOf("<serviceTask id=\"provisionApp\"");
        assertTrue(task >= 0 && task < cycle && cycle < nextTask, "the cycle must ride its own task's element in:\n" + bpmn);
    }

    @Test
    void onErrorEmitsTheErrorDefinitionAndACancellingBoundaryEvent() {
        String bpmn = bpmn(YAML);

        assertTrue(bpmn.contains("<error id=\"intentStepError\" name=\"Intent Step Error\" errorCode=\"INTENT_STEP_FAILED\"></error>"),
                "one error definition at the definitions level in:\n" + bpmn);
        assertTrue(bpmn.contains("<boundaryEvent id=\"createSchemaError\" attachedToRef=\"createSchema\" cancelActivity=\"true\">"),
                "the boundary must cancel the failed task in:\n" + bpmn);
        assertTrue(bpmn.contains("<errorEventDefinition errorRef=\"intentStepError\"></errorEventDefinition>"),
                "the boundary must catch the intent error in:\n" + bpmn);
        assertTrue(bpmn.contains("<boundaryEvent id=\"provisionAppError\""), "each onError step gets its own boundary in:\n" + bpmn);
    }

    @Test
    void theErrorRouteFlowsToTheOnErrorStepAndTheMainFlowBypassesIt() {
        String bpmn = bpmn(YAML);

        assertFlow(bpmn, "createSchemaError", "recordFailure");
        assertFlow(bpmn, "provisionAppError", "recordFailure");
        assertFlow(bpmn, "recordFailure", "markFailed");
        assertFlow(bpmn, "markFailed", "end");
        assertFalse(bpmn.contains("sourceRef=\"provisionApp\" targetRef=\"recordFailure\""),
                "the main flow must route around the error steps:\n" + bpmn);
        assertFlow(bpmn, "provisionApp", "end");
    }

    @Test
    void theDiagramPlacesTheBoundaryShapeAndItsEdge() {
        String bpmn = bpmn(YAML);

        assertTrue(bpmn.contains("BPMNShape_createSchemaError"), "the boundary needs a shape or the modeler opens broken:\n" + bpmn);
        assertTrue(bpmn.contains("BPMNEdge_flow_createSchemaError_then"), "the error route needs its edge:\n" + bpmn);
    }

    @Test
    void clearAfterEmitsAnEndListenerOnItsStep() {
        String bpmn = bpmn(YAML);

        String listener = "<flowable:executionListener event=\"end\" expression=\"${execution.removeVariable('dbPassword')}\">";
        int index = bpmn.indexOf(listener);
        assertTrue(index >= 0, "the clearAfter listener must be emitted in:\n" + bpmn);
        assertTrue(bpmn.indexOf("<serviceTask id=\"provisionApp\"") < index && index < bpmn.indexOf("<serviceTask id=\"recordFailure\""),
                "the listener must ride the clearAfter step's element in:\n" + bpmn);
        assertEquals(index, bpmn.lastIndexOf(listener), "one var clears once:\n" + bpmn);
    }

    @Test
    void clearAfterAUserTaskRidesTheTaskElement() {
        String yaml = YAML.replace("clearAfter: provisionApp", "clearAfter: hold")
                          .replace("onError: recordFailure, next: done", "onError: recordFailure, next: hold")
                          .replace("  - { name: done, kind: end }",
                                  "  - { name: hold, kind: userTask, args: { assignee: operator, next: done } }\n"
                                          + "      - { name: done, kind: end }");
        String bpmn = bpmn(yaml);

        int task = bpmn.indexOf("<userTask id=\"hold\"");
        int listener = bpmn.indexOf("${execution.removeVariable('dbPassword')}");
        assertTrue(task >= 0 && listener > task, "the listener must ride the user task's element in:\n" + bpmn);
        assertTrue(bpmn.contains("</userTask>"), "the user task must close as a container around its listeners:\n" + bpmn);
    }

    /** Without the resilience keys nothing new is emitted - existing intents stay byte-identical. */
    @Test
    void anIntentWithoutResilienceKeysEmitsNoneOfIt() {
        String yaml = YAML.replace(", produces: [dbPassword], retry: { count: 3, every: PT30S }, onError: recordFailure", "")
                          .replace(", uses: [dbPassword], retry: { count: 5, every: PT1M }, onError: recordFailure", "")
                          .replace("    vars:\n      - { name: dbPassword, clearAfter: provisionApp }\n", "")
                          .replace("value: \"{error}\"", "value: failed");
        String bpmn = bpmn(yaml);

        assertFalse(bpmn.contains("failedJobRetryTimeCycle"), "no retry cycle without retry:\n" + bpmn);
        assertFalse(bpmn.contains("<error "), "no error definition without onError:\n" + bpmn);
        assertFalse(bpmn.contains("errorEventDefinition"), "no boundary without onError:\n" + bpmn);
        assertFalse(bpmn.contains("executionListener"), "no clear listener without clearAfter:\n" + bpmn);
    }

    @Test
    void onErrorEndRoutesTheFailureToTheEndEvent() {
        String yaml = YAML.replace("value: \"{error}\"", "value: failed")
                          .replace("onError: recordFailure }", "onError: end }")
                          .replace("onError: recordFailure,", "onError: end,");
        String bpmn = bpmn(yaml);

        assertFlow(bpmn, "createSchemaError", "end");
    }

    /**
     * A send step is emitted on the {@code ${JavaTask}} delegate-expression path, so its cycle has to
     * be written into the extensionElements block the {@code handler} field already opens - next to it,
     * and inside the send's own element rather than the next task's.
     */
    @Test
    void aSendStepCarriesItsRetryCycleNextToItsHandlerField() {
        String bpmn = bpmn(SEND_YAML);

        int task = bpmn.indexOf("<serviceTask id=\"notifyOwner\"");
        int handler = bpmn.indexOf("TenantProvisioningNotifyOwnerSend");
        int cycle = bpmn.indexOf("R2/PT5S");
        int nextTask = bpmn.indexOf("<serviceTask id=\"recordFailure\"");
        assertTrue(task >= 0, "the send step must be emitted in:\n" + bpmn);
        assertTrue(bpmn.contains("<flowable:failedJobRetryTimeCycle>R2/PT5S</flowable:failedJobRetryTimeCycle>"),
                "count: 1 must emit an R2 cycle on the send in:\n" + bpmn);
        assertTrue(task < handler && handler < cycle && cycle < nextTask,
                "the cycle must ride the send's own element, after its handler field, in:\n" + bpmn);
        assertTrue(bpmn.contains("<serviceTask id=\"notifyOwner\" name=\"Notify Owner\" flowable:async=\"true\""),
                "the send keeps its async boundary - a retry cycle only re-runs an async job - in:\n" + bpmn);
        assertTrue(bpmn.contains("flowable:delegateExpression=\"${JavaTask}\""),
                "the send stays on the delegate-expression path in:\n" + bpmn);
    }

    /**
     * The boundary machinery is shape-agnostic: a send's onError routes exactly as a delegate's does.
     */
    @Test
    void aSendStepGetsItsOwnErrorBoundaryAndRoute() {
        String bpmn = bpmn(SEND_YAML);

        assertTrue(bpmn.contains("<boundaryEvent id=\"notifyOwnerError\" attachedToRef=\"notifyOwner\" cancelActivity=\"true\">"),
                "the send needs its own cancelling boundary in:\n" + bpmn);
        assertFlow(bpmn, "notifyOwnerError", "recordFailure");
        // The normal chain is provisionApp -> notifyOwner -> done (the end step, emitted as "end"): the
        // send advances, never loops to itself.
        assertFlow(bpmn, "provisionApp", "notifyOwner");
        assertFlow(bpmn, "notifyOwner", "end");
        assertTrue(bpmn.contains("BPMNShape_notifyOwnerError"), "the boundary needs a shape or the modeler opens broken:\n" + bpmn);
        assertTrue(bpmn.contains("BPMNEdge_flow_notifyOwnerError_then"), "the error route needs its edge:\n" + bpmn);
    }

    /** A send that declares nothing emits no cycle - the send path stays byte-identical too. */
    @Test
    void aSendWithoutResilienceKeysEmitsNoCycle() {
        String bpmn = bpmn(SEND_YAML.replace("retry: { count: 1, every: PT5S }, onError: recordFailure, ", ""));

        int task = bpmn.indexOf("<serviceTask id=\"notifyOwner\"");
        int nextTask = bpmn.indexOf("<serviceTask id=\"recordFailure\"");
        assertTrue(task >= 0 && nextTask > task, "both steps must still be emitted in:\n" + bpmn);
        assertFalse(bpmn.substring(task, nextTask)
                        .contains("failedJobRetryTimeCycle"),
                "no cycle on a send that declares none in:\n" + bpmn);
        assertFalse(bpmn.contains("<boundaryEvent id=\"notifyOwnerError\""), "no boundary on a send that declares none in:\n" + bpmn);
    }

    @Test
    void theWholeMechanismIsIdempotent() {
        assertEquals(bpmn(YAML), bpmn(YAML), "identical input must produce byte-identical output");
        assertEquals(bpmn(SEND_YAML), bpmn(SEND_YAML), "identical input must produce byte-identical output");
    }
}
