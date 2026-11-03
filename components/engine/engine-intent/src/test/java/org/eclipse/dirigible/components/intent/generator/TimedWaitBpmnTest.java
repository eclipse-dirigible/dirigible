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
 * A timed {@code wait} ({@code after:}) renders as a timer intermediate catch event - the pause of
 * a polling loop the engine resumes by itself, so it carries no message - while an event wait
 * beside it keeps its message catch event.
 */
class TimedWaitBpmnTest {

    static final String YAML =
            """
                    name: archive
                    entities:
                      - name: ArchiveRequest
                        fields:
                          - { name: id, type: integer, primaryKey: true, generated: true }
                          - { name: approved, type: boolean }
                          - { name: errorMessage, type: string, length: 2000 }
                    processes:
                      - name: ExportArchive
                        trigger: { onCreate: ArchiveRequest }
                        vars:
                          - { name: exportState }
                        steps:
                          - { name: startExport, kind: serviceTask, args: { delegate: custom.StartExportTask, retry: { count: 3, every: PT30S }, onError: recordFailure, next: checkExport } }
                          - { name: checkExport, kind: serviceTask, args: { delegate: custom.CheckExportTask, produces: [exportState], retry: { count: 3, every: PT30S }, onError: recordFailure, next: exportDone } }
                          - { name: exportDone,  kind: decision,    args: { if: "exportState == 'DONE'", then: publish, else: pause } }
                          - { name: pause,       kind: wait,        args: { after: PT10S, next: checkExport } }
                          - { name: publish,     kind: serviceTask, args: { delegate: custom.PublishTask, next: done } }
                          - { name: recordFailure, kind: serviceTask, args: { setField: errorMessage, value: "{error}", next: done } }
                          - { name: done, kind: end }
                    """;

    /** The same flow with an approval event wait in front of the publish. */
    private static final String WITH_EVENT_WAIT = YAML.replace("then: publish, else: pause", "then: awaitApproval, else: pause")
                                                      .replace("      - { name: publish,",
                                                              "      - { name: awaitApproval, kind: wait, args: { onUpdate: ArchiveRequest, when: \"approved == true\", next: publish } }\n"
                                                                      + "      - { name: publish,");

    @Test
    void aTimedWaitIsATimerCatchEvent() {
        String bpmn = bpmn(YAML);

        assertTrue(bpmn.contains("""
                    <intermediateCatchEvent id="pause" name="Pause">
                      <timerEventDefinition><timeDuration>PT10S</timeDuration></timerEventDefinition>
                    </intermediateCatchEvent>
                """), "expected a timer catch event for the timed wait in:\n" + bpmn);
        assertFalse(bpmn.contains("<message "), "a timed wait has no message to subscribe to:\n" + bpmn);
        assertFalse(bpmn.contains("<messageEventDefinition"), "a timed wait has no message to subscribe to:\n" + bpmn);
    }

    @Test
    void thePollingLoopIsWired() {
        String bpmn = bpmn(YAML);

        assertFlow(bpmn, "exportDone", "pause");
        assertFlow(bpmn, "pause", "checkExport");
        assertFlow(bpmn, "exportDone", "publish");
    }

    @Test
    void anEventWaitBesideItKeepsItsMessage() {
        String bpmn = bpmn(WITH_EVENT_WAIT);

        assertTrue(bpmn.contains("  <message id=\"ExportArchiveAwaitApproval\" name=\"ExportArchiveAwaitApproval\"></message>\n"),
                "the event wait's message definition must be unchanged:\n" + bpmn);
        assertTrue(
                bpmn.contains(
                        """
                                    <intermediateCatchEvent id="awaitApproval" name="Await Approval">
                                      <extensionElements>
                                        <flowable:executionListener event="start" expression="${EarlyMessages.checkWhenArmed(execution)}"></flowable:executionListener>
                                        <flowable:executionListener event="end" expression="${EarlyMessages.consumed(execution)}"></flowable:executionListener>
                                      </extensionElements>
                                      <messageEventDefinition messageRef="ExportArchiveAwaitApproval"></messageEventDefinition>
                                    </intermediateCatchEvent>
                                """),
                "the event wait's catch event must subscribe to its message and consume one kept for it (#8106):\n" + bpmn);
        assertFalse(bpmn.contains("<intermediateCatchEvent id=\"pause\" name=\"Pause\">\n      <extensionElements>"),
                "a timed wait resumes on its timer - no message is ever kept for it:\n" + bpmn);
        assertFalse(bpmn.contains("ExportArchivePause"), "the timed wait must have no message:\n" + bpmn);
    }

    @Test
    void aUserTaskDiscardsWhatWasKeptForALaterWaitOnlyWhereTheProcessHasAnEventWait() {
        String userTask = """
                name: archive
                entities:
                  - name: ArchiveRequest
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                processes:
                  - name: ExportArchive
                    trigger: { onCreate: ArchiveRequest }
                    steps:
                      - { name: review, kind: userTask, args: { next: %s } }
                      - { name: awaitUpdate, kind: wait, args: { onUpdate: ArchiveRequest, next: done } }
                      - { name: pause, kind: wait, args: { after: PT10S, next: done } }
                      - { name: done, kind: end }
                """;
        String discard = "<flowable:executionListener event=\"end\" expression=\"${EarlyMessages.discardAll(execution)}\">";

        String withEventWait = bpmn(userTask.formatted("awaitUpdate"));
        String review = withEventWait.substring(withEventWait.indexOf("<userTask id=\"review\""), withEventWait.indexOf("</userTask>"));
        assertTrue(review.contains(discard),
                "only an event arriving after the person completed the task may resume a later wait (#8106):\n" + withEventWait);

        String timedOnly = bpmn(userTask.formatted("pause")
                                        .replace(
                                                "      - { name: awaitUpdate, kind: wait, args: { onUpdate: ArchiveRequest, next: done } }\n",
                                                ""));
        assertFalse(timedOnly.contains("EarlyMessages"), "a process without an event wait keeps no message:\n" + timedOnly);
    }

    private static void assertFlow(String bpmn, String source, String target) {
        assertTrue(bpmn.contains("sourceRef=\"" + source + "\" targetRef=\"" + target + "\""),
                "expected a flow " + source + " -> " + target + " in:\n" + bpmn);
    }

    /** The BPMN the generator writes for the {@code ExportArchive} process of the given intent. */
    static String bpmn(String yaml) {
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
                     .endsWith("/ExportArchive.bpmn")) {
                return new String(contents.getAllValues()
                                          .get(i),
                        StandardCharsets.UTF_8);
            }
        }
        throw new AssertionError("the process BPMN was not written; wrote " + paths.getAllValues());
    }
}
