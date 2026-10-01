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

import java.util.List;
import java.util.Map;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * A trigger records the type of its entity's primary key (#7599): the restart path receives the
 * record id as text and must convert it to what the repository expects.
 */
class GlueTriggerKeyTypeTest {

    private static final String DOCUMENT = """
            name: tickets
            entities:
              - name: Ticket
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: title, type: string }
              - name: Archive
                fields:
                  - { name: id, type: long, primaryKey: true, generated: true }
                  - { name: title, type: string }
            processes:
              - name: TicketApproval
                trigger: { onCreate: Ticket }
                steps:
                  - { name: approve, kind: userTask, args: { assignee: Manager, next: done } }
                  - { name: done, kind: end }
              - name: ArchiveReview
                trigger: { onCreate: Archive }
                steps:
                  - { name: review, kind: userTask, args: { assignee: Manager, next: done } }
                  - { name: done, kind: end }
            """;

    @Test
    void theTriggerCarriesTheKeyTypeOfItsEntity() {
        List<Map<String, Object>> triggers = GlueIntentGenerator.buildTriggersForTest(IntentParser.parse(DOCUMENT));

        assertEquals(2, triggers.size());
        assertEquals("integer", triggers.get(0)
                                        .get("keyType"));
        assertEquals("long", triggers.get(1)
                                     .get("keyType"));
    }
}
