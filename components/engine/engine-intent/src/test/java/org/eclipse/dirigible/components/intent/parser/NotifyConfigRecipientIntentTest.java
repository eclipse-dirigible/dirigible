/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.junit.jupiter.api.Test;

/**
 * The parse-time half of a {@code @config:KEY} notify recipient (issue #7385): the operations
 * mailbox a schedule reports to differs per environment, so the model names the configuration key
 * instead of an address. The one shape refused is a key that is empty - it could resolve to nothing
 * on every environment, which reads exactly like a record with nobody to mail.
 */
class NotifyConfigRecipientIntentTest {

    private static final String ENTITIES = """
            name: orders
            entities:
              - name: Order
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: status, type: integer }
                  - { name: email,  type: string, length: 120 }
            """;

    @Test
    void aScheduleMayReportToAConfiguredMailbox() {
        IntentModel model = IntentParser.parse(ENTITIES + """
                schedules:
                  - name: stuckOrders
                    cron: "0 */5 * * * ?"
                    entity: Order
                    where: [ { field: status, op: eq, value: 2 } ]
                    notify:
                      to: "@config:OPS_EMAIL"
                      subject: "Order {id} has not moved"
                      body: "It may need an operator."
                """);

        assertEquals("@config:OPS_EMAIL", model.getSchedules()
                                               .get(0)
                                               .getNotify()
                                               .getTo());
    }

    @Test
    void aNotificationsEntryMayUseTheSameReference() {
        IntentParser.parse(ENTITIES + """
                notifications:
                  - name: orderRaised
                    event: { onCreate: Order }
                    to: "@config:OPS_EMAIL"
                    subject: "A new order"
                    body: "One arrived."
                """);
    }

    @Test
    void aDottedConfigurationKeyIsNotReadAsAMultiHopPath() {
        // The multi-hop refusal counts dots on a path; a configuration key is not one, and keys with
        // dots are ordinary (`mail.ops.address`).
        IntentParser.parse(ENTITIES + """
                schedules:
                  - name: stuckOrders
                    cron: "0 */5 * * * ?"
                    entity: Order
                    where: [ { field: status, op: eq, value: 2 } ]
                    notify: { to: "@config:mail.ops.address", subject: "Stuck", body: "Body." }
                """);
    }

    @Test
    void anEmptyConfigurationKeyFailsTheParse() {
        IntentValidationException failure = assertThrows(IntentValidationException.class, () -> IntentParser.parse(ENTITIES + """
                schedules:
                  - name: stuckOrders
                    cron: "0 */5 * * * ?"
                    entity: Order
                    where: [ { field: status, op: eq, value: 2 } ]
                    notify: { to: "@config:", subject: "Stuck", body: "Body." }
                """));

        assertTrue(failure.getMessage()
                          .contains("empty @config: key"),
                failure.getMessage());
    }
}
