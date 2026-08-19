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

import java.util.Map;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * The status axis of the glue event vocabulary: {@code event: { onTransition: <Entity> }}.
 *
 * <p>
 * {@code -transitioned} and {@code -updated} are fully disjoint channels - a workflow
 * {@code setRelationField}, a {@code transitions:} button and a {@code generates} completion hook
 * all publish the former and never the latter, deliberately, so a system write cannot re-fire the
 * onUpdate reactions meant for a person's edit. The consequence was that the whole {@code -updated}
 * half of the DSL was deaf to every status the system itself wrote: you could not declaratively
 * send an email when a workflow set a status, because there was no vocabulary to switch to. These
 * assert the topic each construct now subscribes to.
 */
class GlueTransitionAxisTest {

    /** A fine whose status a workflow step sets - the canonical shape the axis was missing. */
    private static final String YAML = """
            name: fines
            entities:
              - name: FineStatus
                kind: setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Driver
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: email, type: string }
              - name: Fine
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string }
                relations:
                  - { name: driver, kind: manyToOne, to: Driver }
                  - { name: Status, kind: manyToOne, to: FineStatus, function: EntityStatus, init: 1 }
            seeds:
              - name: fineStatuses
                entity: FineStatus
                rows:
                  - { id: 1, name: NEW }
                  - { id: 2, name: IDENTIFIED }
            processes:
              - name: Identify
                trigger: { onCreate: Fine }
                steps:
                  - { name: attribute, kind: serviceTask, args: { setRelationField: Status, value: IDENTIFIED } }
                  - { name: done, kind: end }
            notifications:
              - name: fineAttributed
                event: { onTransition: Fine }
                to: driver.email
                subject: "Fine {number} attributed"
                body: "Your fine has been attributed to you."
            integrations:
              - name: pushAttribution
                event: { onTransition: Fine }
                method: POST
                url: "@config:PARTNER_URL"
            outbound:
              - name: publishAttribution
                event: { onTransition: Fine }
                to: { topic: "codbex.fines" }
            """;

    private static Map<String, Object> only(java.util.List<Map<String, Object>> entries) {
        assertEquals(1, entries.size(), "expected exactly one entry, got: " + entries);
        return entries.get(0);
    }

    /**
     * The reported case verbatim: the status is set by a {@code setRelationField} step, which publishes
     * {@code -transitioned}; a notification bound to {@code onUpdate} listened on the other channel and
     * never fired, with nothing anywhere to say so.
     */
    @Test
    void aNotificationCanBindTheStatusChannel() {
        Map<String, Object> notification = only(GlueIntentGenerator.buildNotificationsForTest(IntentParser.parse(YAML)));
        assertEquals("Fine", notification.get("entity"));
        assertEquals("-transitioned", notification.get("topicSuffix"));
    }

    @Test
    void anIntegrationCanBindTheStatusChannel() {
        Map<String, Object> integration = only(GlueIntentGenerator.buildIntegrationsForTest(IntentParser.parse(YAML)));
        assertEquals("Fine", integration.get("entity"));
        assertEquals("-transitioned", integration.get("topicSuffix"));
    }

    /** The departure rides the same shared binding, so it comes along rather than drifting behind. */
    @Test
    void aDepartureCanBindTheStatusChannel() {
        Map<String, Object> departure = only(GlueIntentGenerator.buildOutboundForTest(IntentParser.parse(YAML)));
        assertEquals("Fine", departure.get("entity"));
        assertEquals("-transitioned", departure.get("topicSuffix"));
    }

    /**
     * A process may also START on a transition - "when the fine is identified, run the dunning flow".
     * The trigger's own at-most-once guard (a stamped ProcessId) is unchanged, so a later transition
     * does not restart it.
     */
    @Test
    void aProcessTriggerCanStartOnATransition() {
        IntentModel model = IntentParser.parse(YAML.replace("trigger: { onCreate: Fine }", "trigger: { onTransition: Fine }"));
        assertEquals("onTransition", TriggerSupport.triggerKind(model.getProcesses()
                                                                     .get(0)));
        assertEquals("-transitioned", EventBinding.topicSuffix(TriggerSupport.triggerKind(model.getProcesses()
                                                                                               .get(0))));
    }
}
