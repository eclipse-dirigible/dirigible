/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator.edm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * The document page's status stepper (issue #7592): the {@code widgetStatusSteps} attribute of the
 * {@code DOCUMENT_STATUS} property lists, by seed id and in seed order, the statuses the document's
 * flows walk - the same rule as the task form's stepper - so the page no longer derives its steps
 * from the status picker's label-sorted options.
 */
class EdmStatusStepsTest {

    /**
     * The sta shape: an approval flow walking DRAFT -> APPROVED -> ISSUED -> SENT -> CONFIRMED, beside
     * the settlement statuses PARTIAL / PAID no process writes (the paid roll-up does) and a terminal
     * CANCELLED. Alphabetically the statuses read APPROVED, CANCELLED, CONFIRMED, DRAFT, ... - the
     * order the page showed - and the Bulgarian translation seed sorts differently again.
     */
    private static final String YAML = """
            name: billing
            entities:
              - name: InvoiceStatus
                function: Setting
                multilingual: true
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Customer
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Invoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                relations:
                  - { name: Customer, kind: manyToOne, to: Customer }
                  - { name: Status, kind: manyToOne, to: InvoiceStatus, function: EntityStatus, init: 1 }
            processes:
              - name: InvoiceApproval
                trigger: { onCreate: Invoice }
                steps:
                  - { name: approve, kind: userTask, args: { assignee: approver, form: ApproveInvoice } }
                  - { name: approveDecision, kind: decision, args: { if: "action == 'approve'", then: activate, else: cancel } }
                  - { name: activate, kind: serviceTask, args: { setRelationField: Status, value: 2, next: issue } }
                  - { name: issue, kind: serviceTask, args: { setRelationField: Status, value: 3, next: send } }
                  - { name: send, kind: serviceTask, args: { setRelationField: Status, value: 4, next: confirm } }
                  - { name: confirm, kind: serviceTask, args: { setRelationField: Status, value: 5, next: end } }
                  - { name: cancel, kind: serviceTask, args: { setRelationField: Status, value: 8, next: end } }
                  - { name: end, kind: end }
            forms:
              - { name: ApproveInvoice, forEntity: Invoice, fields: [Status], actions: [approve, reject] }
            seeds:
              - name: invoice-statuses
                entity: InvoiceStatus
                rows:
                  - { id: 1, name: DRAFT }
                  - { id: 2, name: APPROVED }
                  - { id: 3, name: ISSUED }
                  - { id: 4, name: SENT }
                  - { id: 5, name: CONFIRMED }
                  - { id: 6, name: PARTIAL }
                  - { id: 7, name: PAID }
                  - { id: 8, name: CANCELLED }
              - name: invoice-statuses-bg
                entity: InvoiceStatus
                language: bg
                rows:
                  - { id: 1, name: "Чернова" }
                  - { id: 2, name: "Одобрена" }
                  - { id: 8, name: "Анулирана" }
            """;

    @Test
    void theStepsAreTheStatusesTheFlowsWalkInSeedOrder() {
        // DRAFT (the onCreate flow's entry) then every status the approval writes, in seed order;
        // PARTIAL/PAID are written by no flow, CANCELLED is terminal - all three stay the pill.
        assertEquals("1,2,3,4,5", property(YAML, "Status").get("widgetStatusSteps"));
    }

    @Test
    void withoutAFlowThatWritesTheStatusTheStepsAreTheNonTerminalNomenclature() {
        String noProcess = YAML.substring(0, YAML.indexOf("processes:")) + YAML.substring(YAML.indexOf("forms:"))
                                                                               .replace(
                                                                                       "  - { name: ApproveInvoice, forEntity: Invoice, fields: [Status], actions: [approve, reject] }\n",
                                                                                       "  - { name: InvoiceCard, forEntity: Invoice, fields: [Status] }\n");
        assertEquals("1,2,3,4,5,6,7", property(noProcess, "Status").get("widgetStatusSteps"));
    }

    @Test
    void aPlainRelationCarriesNoSteps() {
        Map<String, Object> customer = property(YAML, "Customer");
        assertEquals("DROPDOWN", customer.get("widgetType"));
        assertFalse(customer.containsKey("widgetStatusSteps"));
    }

    @Test
    void theEdmTwinCarriesTheStepsToo() {
        String edm = EdmIntentGenerator.buildEdmXmlForTest(IntentParser.parse(YAML), "billing");
        assertTrue(edm.contains("widgetStatusSteps=\"1,2,3,4,5\""), edm);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> property(String yaml, String name) {
        Map<String, Object> modelJson = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "billing");
        List<Map<String, Object>> entities = (List<Map<String, Object>>) ((Map<String, Object>) modelJson.get("model")).get("entities");
        Map<String, Object> invoice = entities.stream()
                                              .filter(e -> "Invoice".equals(e.get("name")))
                                              .findFirst()
                                              .orElseThrow();
        return ((List<Map<String, Object>>) invoice.get("properties")).stream()
                                                                      .filter(p -> name.equals(p.get("name")))
                                                                      .findFirst()
                                                                      .orElseThrow();
    }
}
