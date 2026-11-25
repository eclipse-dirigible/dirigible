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

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * The trigger of a process whose task form lists the record's lines ({@code forms[].items}, issue
 * #7907) names the lines' entity and perspective, from which the events template seeds the lines'
 * controller URL - and the trigger of any other process is left exactly as it was.
 */
class GlueItemLinksTest {

    private static final String YAML =
            """
                    name: billing
                    entities:
                      - name: Invoice
                        fields:
                          - { name: id, type: integer, primaryKey: true, generated: true }
                          - { name: number, type: string }
                      - name: InvoiceItem
                        fields:
                          - { name: id, type: integer, primaryKey: true, generated: true }
                          - { name: name, type: string }
                        relations:
                          - { name: Invoice, kind: manyToOne, to: Invoice, composition: true }
                    processes:
                      - name: Approval
                        trigger: { onCreate: Invoice }
                        steps:
                          - { name: approve, kind: userTask, args: { assignee: approver, form: ApproveInvoice } }
                          - { name: done, kind: end }
                    forms:
                      - { name: ApproveInvoice, forEntity: Invoice, fields: [number], items: { entity: InvoiceItem, fields: [name] }, actions: [approve] }
                    """;

    @Test
    void theTriggerLinksTheLinesItsTaskFormLists() {
        Map<String, Object> trigger = GlueIntentGenerator.buildTriggersForTest(IntentParser.parse(YAML))
                                                         .get(0);
        assertEquals(List.of(Map.of("entity", "InvoiceItem", "perspective", "Invoice")), trigger.get("itemLinks"),
                "a composition child lives under its master's perspective");
    }

    @Test
    void aTriggerWhoseFormsListNoLinesCarriesNoLinks() {
        String yaml = YAML.replace(" items: { entity: InvoiceItem, fields: [name] },", "");
        Map<String, Object> trigger = GlueIntentGenerator.buildTriggersForTest(IntentParser.parse(yaml))
                                                         .get(0);
        assertFalse(trigger.containsKey("itemLinks"), "an application without lines tables must generate the .glue it always did");
    }
}
