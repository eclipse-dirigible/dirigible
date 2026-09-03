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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.components.intent.parser.IntentValidationException;
import org.junit.jupiter.api.Test;

/**
 * The delivery axis (dirigible #7023): a notify block that could not send announces
 * {@code -notifyFailed}, and the glue consumers plus a process trigger can bind it. Until this axis
 * a failed delivery was a server log line, so "when the invoice mail bounces, chase it" had no
 * expression at all.
 */
class GlueNotifyFailedAxisTest {

    private static final String YAML = """
            name: billing
            entities:
              - name: InvoiceStatus
                function: Setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Customer
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: email, type: string }
              - name: Invoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string }
                  - { name: sendOutcome, type: string, length: 128 }
                relations:
                  - { name: customer, kind: manyToOne, to: Customer }
                  - { name: Status, kind: manyToOne, to: InvoiceStatus, function: EntityStatus, init: 1 }
            seeds:
              - name: invoiceStatuses
                entity: InvoiceStatus
                rows:
                  - { id: 1, name: DRAFT }
                  - { id: 2, name: SENT }
            transitions:
              - name: SendInvoice
                forEntity: Invoice
                from: [1]
                setStatus: 2
                notify:
                  to: customer.email
                  subject: "Invoice {number}"
                  body: "Attached."
                  outcome: sendOutcome
            notifications:
              - name: tellOpsAboutABounce
                event: { onNotifyFailed: Invoice }
                to: "ops@example.com"
                subject: "Invoice {number} could not be mailed"
                body: "The delivery said: {sendOutcome}"
            integrations:
              - name: pushBounceToCrm
                event: { onNotifyFailed: Invoice }
                method: POST
                url: "https://crm.example.com/bounces"
            """;

    @Test
    void everyGlueConsumerCanBindTheDeliveryFailure() {
        IntentModel model = IntentParser.parse(YAML);

        Map<String, Object> notification = GlueIntentGenerator.buildNotificationsForTest(model)
                                                              .get(0);
        assertEquals("-notifyFailed", notification.get("topicSuffix"),
                "a consumer bound to onNotifyFailed must listen on the topic the failed stamp publishes");

        List<Map<String, Object>> integrations = GlueIntentGenerator.buildIntegrationsForTest(model);
        assertEquals(1, integrations.size());
        assertEquals("-notifyFailed", integrations.get(0)
                                                  .get("topicSuffix"));
    }

    /** The suffix is one constant read by the sender and by every consumer, so they cannot drift. */
    @Test
    void theSuffixIsTheOneTheSenderPublishes() {
        assertEquals(EventBinding.NOTIFY_FAILED_SUFFIX, EventBinding.topicSuffix(EventBinding.ON_NOTIFY_FAILED));
        assertEquals("-notifyFailed", EventBinding.NOTIFY_FAILED_SUFFIX);
    }

    /**
     * A process may start on it, which is what makes "open a task when the mail bounces" expressible
     * without any Java - the reaction the issue asked for.
     */
    @Test
    void aProcessMayTriggerOnIt() {
        String yaml = YAML + """
                processes:
                  - name: ChaseDelivery
                    trigger: { onNotifyFailed: Invoice }
                    steps:
                      - { name: chase, kind: userTask, args: { assignee: billing, next: done } }
                      - { name: done, kind: end }
                """;
        IntentModel model = IntentParser.parse(yaml);
        Map<String, Object> trigger = GlueIntentGenerator.buildTriggersForTest(model)
                                                         .get(0);
        assertEquals("Invoice", trigger.get("entity"));
        assertEquals("-notifyFailed", trigger.get("topicSuffix"));
    }

    /** A trigger still binds at most one moment - the delivery axis is one more, not an exception. */
    @Test
    void aTriggerStillBindsOneMomentOnly() {
        String yaml = YAML + """
                processes:
                  - name: ChaseDelivery
                    trigger: { onNotifyFailed: Invoice, onCreate: Invoice }
                    steps:
                      - { name: done, kind: end }
                """;
        try {
            IntentParser.parse(yaml);
            throw new AssertionError("expected the intent to be refused");
        } catch (IntentValidationException expected) {
            assertTrue(String.join("; ", expected.getIssues())
                             .contains("at most one of onCreate/onUpdate/onDelete/onTransition/onNotifyFailed"),
                    String.join("; ", expected.getIssues()));
        }
    }
}
