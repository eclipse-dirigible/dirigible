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

    /**
     * A flow that PARKS until the record reaches a status - the shape the status axis invites, and the
     * one whose guard was never resolved.
     */
    private static final String WAITING = """
              - name: Dunning
                trigger: { onCreate: Fine }
                steps:
                  - { name: hold, kind: wait, args: { onTransition: Fine, when: "Status == IDENTIFIED", next: remind } }
                  - { name: remind, kind: end }
            """;

    /** A wait resumed by ANOTHER entity's transition, walked back through {@code via:}. */
    private static final String WAITING_VIA = """
              - name: Collection
                trigger: { onCreate: Fine }
                steps:
                  - { name: settle, kind: wait, args: { onTransition: Payment, via: fine, when: "Status == SETTLED", next: closed } }
                  - { name: closed, kind: end }
            """;

    /** The payment whose own nomenclature the {@code via:} wait's guard is read against. */
    private static final String PAYMENT = """
              - name: PaymentStatus
                kind: setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Payment
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal }
                relations:
                  - { name: fine, kind: manyToOne, to: Fine }
                  - { name: Status, kind: manyToOne, to: PaymentStatus, function: EntityStatus, init: 1 }
            """;

    /** The payment nomenclature's own seeds - deliberately numbered apart from the fine's. */
    private static final String PAYMENT_SEEDS = """
              - name: paymentStatuses
                entity: PaymentStatus
                rows:
                  - { id: 1, name: NEW }
                  - { id: 2, name: PENDING }
                  - { id: 3, name: SETTLED }
            """;

    /** The follow-up flow, started by the transition the first flow produces. */
    private static final String DUNNING = """
              - name: Dunning
                trigger: { onTransition: Fine, when: "Status == IDENTIFIED" }
                steps:
                  - { name: remind, kind: end }
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
     * The trigger keeps an at-most-once guard, but a PER-PROCESS one (the record's {@code ProcessIds}
     * stamps), so a later transition does not restart it - and does not block the OTHER flow either.
     */
    @Test
    void aProcessTriggerCanStartOnATransition() {
        IntentModel model = IntentParser.parse(YAML.replace("trigger: { onCreate: Fine }", "trigger: { onTransition: Fine }"));
        assertEquals("onTransition", TriggerSupport.triggerKind(model.getProcesses()
                                                                     .get(0)));
        assertEquals("-transitioned", EventBinding.topicSuffix(TriggerSupport.triggerKind(model.getProcesses()
                                                                                               .get(0))));
    }

    /**
     * The composition the axis invites, and the one that could not work: the create-triggered flow sets
     * the status, and the transition that status produces starts a SECOND flow on the same record.
     *
     * <p>
     * Both listeners are emitted, each on its own channel and carrying its own process name - which is
     * what the generated guard scopes its at-most-once check to (the record's {@code ProcessIds}
     * stamps). While that check read the record's single {@code ProcessId}, the second flow was skipped
     * for every record the first had already stamped, silently: no log line, no visible symptom.
     */
    @Test
    void twoProcessesCanTriggerOnTheSameRecordOnDifferentChannels() {
        IntentModel model = IntentParser.parse(YAML.replace("notifications:", DUNNING + "notifications:"));

        java.util.List<Map<String, Object>> triggers = GlueIntentGenerator.buildTriggersForTest(model);

        assertEquals(2, triggers.size(), "both the create-triggered and the transition-triggered flow must get a listener");
        Map<String, Object> identify = triggers.get(0);
        Map<String, Object> dunning = triggers.get(1);
        assertEquals("Identify", identify.get("process"));
        assertEquals("", identify.get("topicSuffix"), "the create channel is the unsuffixed base topic");
        assertEquals("Dunning", dunning.get("process"));
        assertEquals("-transitioned", dunning.get("topicSuffix"));
        assertEquals(identify.get("entity"), dunning.get("entity"), "both flows are about the same record");
        // ...and the status the second flow waits for resolves to its seed ID. A trigger's `when` was
        // the one guard in the DSL whose status NAME was never resolved, so it reached the generated
        // listener as a string compared against the integer FK: never true, and an onTransition trigger
        // - which is exactly the one that needs a status guard - could not start at all.
        assertEquals("java.util.Objects.equals(entity.Status, 2)", dunning.get("guardExpression"),
                "the trigger's status guard must compare against the seed id, not against the name as a string");
    }

    /**
     * A {@code wait} step's guard was the last {@code when} in the DSL whose status NAME survived
     * unresolved: the wait template emits it verbatim, so the generated listener compared the integer
     * status FK against the string {@code "IDENTIFIED"}. Always false - and since a wait that matches
     * nothing is a deliberate no-op, the parked instance simply never resumed, with nothing logged
     * anywhere (#6907).
     */
    @Test
    void aWaitStatusGuardResolvesToTheSeedId() {
        IntentModel model = IntentParser.parse(YAML.replace("notifications:", WAITING + "notifications:"));

        Map<String, Object> wait = only(GlueIntentGenerator.buildWaitsForTest(model));

        assertEquals("Fine", wait.get("eventEntity"));
        assertEquals("-transitioned", wait.get("topicSuffix"));
        assertEquals("java.util.Objects.equals(entity.Status, 2)", wait.get("guardExpression"),
                "the wait's status guard must compare against the seed id, not against the name as a string");
    }

    /**
     * And it resolves against the EVENT entity's nomenclature, not the trigger entity's - the two
     * differ exactly when the wait walks back through {@code via:}, and taking the id from the wrong
     * lifecycle would be the #6645 failure again one level down.
     */
    @Test
    void aViaWaitResolvesAgainstTheEventEntitysNomenclature() {
        IntentModel model = IntentParser.parse(YAML.replace("seeds:", PAYMENT + "seeds:")
                                                   .replace("processes:", PAYMENT_SEEDS + "processes:")
                                                   .replace("notifications:", WAITING_VIA + "notifications:"));

        Map<String, Object> wait = only(GlueIntentGenerator.buildWaitsForTest(model));

        assertEquals("Payment", wait.get("eventEntity"));
        assertEquals("Fine", wait.get("parentEntity"));
        assertEquals("java.util.Objects.equals(entity.Status, 3)", wait.get("guardExpression"),
                "the guard is over the payment's own status nomenclature, which numbers SETTLED apart from any fine status");
    }
}
