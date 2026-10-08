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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * A {@code number: { stampOn: issue }} field holds a UUID placeholder until its document is issued
 * (issue #7722). A relation labelled by such a number - its picker, its lookup cells - and a
 * {@code show:} column of one are marked in the model, so the UI renders the draft marker instead
 * of the placeholder.
 */
class EdmDraftNumberLabelTest {

    private static final String BILLING = """
            name: billing
            entities:
              - name: SalesInvoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string, length: 100, number: { series: Sales Invoice, stampOn: issue } }
                  - { name: issuedOn, type: date }
              - name: Payment
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal }
                relations:
                  - { name: invoice, kind: manyToOne, to: SalesInvoice, show: [number, issuedOn] }
                  - { name: settles, kind: subset, to: SalesInvoice }
            """;

    @Test
    void aRelationLabelledByAnUnstampedNumberIsMarked() {
        assertEquals("Number", property(BILLING, "Payment", "Invoice").get("widgetDropDownValue"),
                "sanity: the invoice's first string field labels it");
        assertEquals("true", property(BILLING, "Payment", "Invoice").get("widgetDropDownDocumentNumber"));
        assertEquals("true", property(BILLING, "Payment", "Settles").get("widgetDropDownDocumentNumber"),
                "a subset over the same target resolves each key to the same label");
    }

    @Test
    void aShowColumnOfTheNumberIsMarkedAndTheOthersAreNot() {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> columns = (List<Map<String, Object>>) property(BILLING, "Payment", "Invoice").get("lookupColumns");
        assertEquals("true", columns.get(0)
                                    .get("documentNumber"),
                "the number column: " + columns);
        assertNull(columns.get(1)
                          .get("documentNumber"),
                "the date column is a date: " + columns);
    }

    @Test
    void aNumberStampedOnCreateIsNeverAPlaceholder() {
        String yaml = BILLING.replace("stampOn: issue", "stampOn: create");
        assertNull(property(yaml, "Payment", "Invoice").get("widgetDropDownDocumentNumber"));
    }

    @Test
    void aTargetLabelledByItsNameIsNotMarked() {
        String yaml = BILLING.replace("      - { name: issuedOn, type: date }\n",
                "      - { name: issuedOn, type: date }\n      - { name: name, type: string }\n");
        assertEquals("Name", property(yaml, "Payment", "Invoice").get("widgetDropDownValue"));
        assertNull(property(yaml, "Payment", "Invoice").get("widgetDropDownDocumentNumber"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> property(String yaml, String entityName, String propertyName) {
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "billing");
        List<Map<String, Object>> entities = (List<Map<String, Object>>) ((Map<String, Object>) model.get("model")).get("entities");
        Map<String, Object> entity = entities.stream()
                                             .filter(e -> entityName.equals(e.get("name")))
                                             .findFirst()
                                             .orElseThrow(() -> new AssertionError("no entity [" + entityName + "]"));
        return ((List<Map<String, Object>>) entity.get("properties")).stream()
                                                                     .filter(p -> propertyName.equals(p.get("name")))
                                                                     .findFirst()
                                                                     .orElseThrow(() -> new AssertionError(
                                                                             "no property [" + propertyName + "] on [" + entityName + "]"));
    }
}
