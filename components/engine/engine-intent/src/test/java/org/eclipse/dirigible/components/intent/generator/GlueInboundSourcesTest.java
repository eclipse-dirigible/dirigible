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
import org.junit.jupiter.api.Test;

/**
 * Verifies that an inbound ingest is routed to the collection matching how it arrives - HTTP
 * webhook, queue/topic listener or polled drop folder - and that each carries the coordinates its
 * generated handler needs.
 */
class GlueInboundSourcesTest {

    private static final String YAML = """
            name: crm
            entities:
              - name: Lead
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: name,  type: string }
            inbound:
              - { name: leadHook,  path: /webhooks/lead, create: Lead }
              - { name: leadQueue, source: { queue: leads.inbound }, create: Lead }
              - { name: leadFeed,  source: { topic: crm.leads }, create: Lead }
              - { name: leadDrop,  source: { folder: /data/inbox/leads, cron: "0 */5 * * * ?" }, create: Lead }
            """;

    @Test
    void eachArrivalLandsInItsOwnCollection() {
        IntentModel model = IntentParser.parse(YAML);

        List<Map<String, Object>> webhooks = GlueIntentGenerator.buildInboundForTest(model);
        assertEquals(1, webhooks.size(), "only the path-declaring ingest is an HTTP controller");
        assertEquals("leadHook", webhooks.get(0)
                                         .get("name"));
        assertEquals("/webhooks/lead", webhooks.get(0)
                                               .get("path"));

        List<Map<String, Object>> messages = GlueIntentGenerator.buildInboundMessagesForTest(model);
        assertEquals(2, messages.size());
        assertEquals("leads.inbound", messages.get(0)
                                              .get("destination"));
        assertEquals("QUEUE", messages.get(0)
                                      .get("listenerKind"));
        assertEquals("crm.leads", messages.get(1)
                                          .get("destination"));
        assertEquals("TOPIC", messages.get(1)
                                      .get("listenerKind"));

        List<Map<String, Object>> files = GlueIntentGenerator.buildInboundFilesForTest(model);
        assertEquals(1, files.size());
        assertEquals("/data/inbox/leads", files.get(0)
                                               .get("folder"));
        assertEquals("0 */5 * * * ?", files.get(0)
                                           .get("cron"));

        for (Map<String, Object> entry : List.of(webhooks.get(0), messages.get(0), files.get(0))) {
            assertEquals("Lead", entry.get("entity"));
            assertEquals("Lead", entry.get("perspective"));
            assertTrue(entry.get("className")
                            .toString()
                            .startsWith("Lead"),
                    "the handler class name is derived from the ingest name");
        }
    }
}
