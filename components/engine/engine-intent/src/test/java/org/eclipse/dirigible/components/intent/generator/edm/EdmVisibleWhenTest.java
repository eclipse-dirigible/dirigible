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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.components.intent.parser.IntentValidationException;
import org.junit.jupiter.api.Test;

/**
 * {@code visibleWhen} (dirigible #7502): the status gate on a field and on a composition child's
 * detail panel. Its names resolve to seed ids - a field's against the record's own nomenclature, a
 * panel's against its MASTER's - and it reaches the {@code .model} and the {@code .edm} as one
 * scalar condition in the model's property names, which the template layer renders.
 */
class EdmVisibleWhenTest {

    /** A document with an items child, a secondary child gated on the invoice, and a gated field. */
    private static String invoice(String paymentGate, String paidOnGate) {
        return """
                name: sales
                seeds:
                  - name: invoice-statuses
                    entity: InvoiceStatus
                    rows:
                      - { id: 1, name: DRAFT }
                      - { id: 2, name: ISSUED }
                      - { id: 3, name: PAID }
                entities:
                  - name: InvoiceStatus
                    kind: setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string, length: 50 }
                  - name: Invoice
                    function: Document
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: paidOn, type: date%s }
                    relations:
                      - { name: Status, kind: manyToOne, to: InvoiceStatus, function: EntityStatus, init: DRAFT }
                  - name: InvoiceItem
                    function: DocumentItem
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: amount, type: decimal }
                    relations:
                      - { name: Invoice, kind: manyToOne, to: Invoice, composition: true }
                  - name: InvoicePayment
                %s    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: amount, type: decimal }
                    relations:
                      - { name: Invoice, kind: manyToOne, to: Invoice, composition: true }
                """.formatted(paidOnGate, paymentGate);
    }

    private static final String GATED = invoice("    visibleWhen: \"Status != DRAFT\"\n", ", visibleWhen: \"Status == PAID\"");

    @Test
    void aFieldGateResolvesTheRecordsOwnStatusNameIntoTheModelScalar() {
        assertEquals("Status == 3", property(entity(GATED, "Invoice"), "PaidOn").get("visibleWhen"));
    }

    @Test
    void aPanelGateResolvesAgainstTheMastersNomenclature() {
        assertEquals("Status != 1", entity(GATED, "InvoicePayment").get("visibleWhen"),
                "the child has no status of its own - DRAFT is the invoice's");
    }

    @Test
    void theListFormIsAnAndOfItsTerms() {
        String yaml = invoice("    visibleWhen: [\"Status != DRAFT\", \"Status != PAID\"]\n", "");
        assertEquals("Status != 1 && Status != 3", entity(yaml, "InvoicePayment").get("visibleWhen"));
    }

    @Test
    void anUngatedModelCarriesNoAttribute() {
        String plain = invoice("", "");
        assertNull(entity(plain, "InvoicePayment").get("visibleWhen"));
        assertNull(property(entity(plain, "Invoice"), "PaidOn").get("visibleWhen"));
    }

    /**
     * A scalar, so the modeler's twin keeps it - an attribute the .edm drops is lost on the next save.
     */
    @Test
    void bothGatesRoundTripThroughTheEdmXml() {
        String xml = EdmIntentGenerator.buildEdmXmlForTest(IntentParser.parse(GATED), "sales");
        assertTrue(xml.contains("visibleWhen=\"Status == 3\""), xml);
        assertTrue(xml.contains("visibleWhen=\"Status != 1\""), xml);
    }

    /** A status name has no ordering - the same refusal every other status guard gives. */
    @Test
    void anOrderedComparisonAgainstAStatusNameIsRefused() {
        assertIssue(invoice("    visibleWhen: \"Status >= ISSUED\"\n", ""), "a status name has no ordering");
    }

    @Test
    void aNumericOrderedComparisonIsRefusedByTheGrammar() {
        assertIssue(invoice("    visibleWhen: \"Status >= 2\"\n", ""), "must be `<Property> ==|!= <literal>`");
    }

    @Test
    void aPanelGateMustBeOnACompositionChild() {
        String yaml = GATED.replace("  - name: Invoice\n    function: Document\n",
                "  - name: Invoice\n    function: Document\n    visibleWhen: \"Status != DRAFT\"\n");
        assertIssue(yaml, "is not a composition child");
    }

    @Test
    void theDocumentsLineItemsAreNotAPanel() {
        String yaml = GATED.replace("  - name: InvoiceItem\n    function: DocumentItem\n",
                "  - name: InvoiceItem\n    function: DocumentItem\n    visibleWhen: \"Status != 1\"\n");
        assertIssue(yaml, "is the line items of document [Invoice]");
    }

    @Test
    void aPanelTermMustNameAPropertyOfTheMaster() {
        assertIssue(invoice("    visibleWhen: \"amount != 0\"\n", ""), "which is not a field or to-one relation of [Invoice]");
    }

    @Test
    void aRequiredFieldCannotBeHidden() {
        assertIssue(invoice("", ", required: true, visibleWhen: \"Status == PAID\""), "declare a status-gated `checks: requiredWhen`");
    }

    @Test
    void anUnknownStatusNameIsRefused() {
        assertIssue(invoice("    visibleWhen: \"Status != VOIDED\"\n", ""),
                "names [VOIDED], which is not a seeded status of [InvoiceStatus]");
    }

    private static void assertIssue(String yaml, String fragment) {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains(fragment)),
                "expected an issue containing [" + fragment + "], got: " + ex.getIssues());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> entity(String yaml, String name) {
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "sales");
        List<Map<String, Object>> entities = (List<Map<String, Object>>) ((Map<String, Object>) model.get("model")).get("entities");
        return entities.stream()
                       .filter(entity -> name.equals(entity.get("name")))
                       .findFirst()
                       .orElseThrow(() -> new AssertionError("no entity [" + name + "]"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> property(Map<String, Object> entity, String name) {
        return ((List<Map<String, Object>>) entity.get("properties")).stream()
                                                                     .filter(property -> name.equals(property.get("name")))
                                                                     .findFirst()
                                                                     .orElseThrow(() -> new AssertionError("no property [" + name + "]"));
    }
}
