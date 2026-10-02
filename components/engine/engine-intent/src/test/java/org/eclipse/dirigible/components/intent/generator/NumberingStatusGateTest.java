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

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * The {@code stampOn: issue} numbering descriptor knows the status each step running the stamp
 * precedes (#7577): the stamp spends a value of a legal, gap-free series, so it asks the repository
 * whether that move will be accepted before it allocates. The status comes from the process - the
 * first {@code setRelationField} on the document's status that the stamp step's {@code next:} chain
 * of service tasks reaches.
 */
class NumberingStatusGateTest {

    private static final String INVOICING =
            """
                    name: invoicing
                    entities:
                      - name: InvoiceStatus
                        kind: setting
                        fields:
                          - { name: id, type: integer, primaryKey: true, generated: true }
                          - { name: name, type: string }
                      - name: SalesInvoice
                        fields:
                          - { name: id, type: integer, primaryKey: true, generated: true }
                          - { name: number, type: string, length: 100, number: { series: Sales Invoice, stampOn: issue } }
                        relations:
                          - { name: Status, kind: manyToOne, to: InvoiceStatus, function: EntityStatus, init: 1 }
                    processes:
                      - name: SalesInvoiceIssue
                        trigger: { onCreate: SalesInvoice }
                        steps:
                          - { name: issue,          kind: userTask,    args: { form: IssueSalesInvoice, next: generateNumber } }
                          - { name: generateNumber, kind: serviceTask, args: { delegate: gen.events.invoicing.SalesInvoiceNumberStamp, next: notify } }
                          - { name: notify,         kind: serviceTask, args: { delegate: gen.events.invoicing.SomethingElse, next: markIssued } }
                          - { name: markIssued,     kind: serviceTask, args: { setRelationField: Status, value: 3, next: done } }
                          - { name: done,           kind: end }
                    forms:
                      - { name: IssueSalesInvoice, forEntity: SalesInvoice, fields: [number] }
                    seeds:
                      - name: statuses
                        entity: InvoiceStatus
                        rows:
                          - { id: 1, name: Draft }
                          - { id: 3, name: Issued }
                    """;

    /** Through a service task in between, to the first status write of the chain. */
    @Test
    void theStampStepIsGatedOnTheStatusItsChainWrites() {
        assertEquals(List.of(Map.of("process", "SalesInvoiceIssue", "step", "generateNumber", "status", "3")), gatesOf(INVOICING));
    }

    /** A wait before any status write ends the search: what follows it is another transaction. */
    @Test
    void aUserTaskBeforeTheStatusWriteGatesNothing() {
        String waiting = INVOICING.replace(
                "{ name: notify,         kind: serviceTask, args: { delegate: gen.events.invoicing.SomethingElse, next: markIssued } }",
                "{ name: notify,         kind: userTask,    args: { form: IssueSalesInvoice, next: markIssued } }");
        assertEquals(List.of(), gatesOf(waiting));
    }

    /** No status relation, nothing to move - and nothing gated to ask about. */
    @Test
    void anEntityWithoutAStatusGatesNothing() {
        String statusless =
                INVOICING.replace(
                        "                relations:\n                  - { name: Status, kind: manyToOne, to: InvoiceStatus, function: EntityStatus, init: 1 }\n",
                        "")
                         .replace("{ name: markIssued,     kind: serviceTask, args: { setRelationField: Status, value: 3, next: done } }",
                                 "{ name: markIssued,     kind: serviceTask, args: { setField: note, value: X, next: done } }")
                         .replace("- { name: id, type: integer, primaryKey: true, generated: true }\n      - { name: number,",
                                 "- { name: id, type: integer, primaryKey: true, generated: true }\n      - { name: note, type: string, length: 100 }\n      - { name: number,");
        assertEquals(List.of(), gatesOf(statusless));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> gatesOf(String yaml) {
        IntentModel model = IntentParser.parse(yaml);
        List<Map<String, Object>> numbering = NumberingSupport.buildNumbering(model, IntentEntities.compositionParents(model));
        assertEquals(1, numbering.size(), "exactly one stampOn: issue number: " + numbering);
        return (List<Map<String, Object>>) numbering.get(0)
                                                    .get("gates");
    }
}
