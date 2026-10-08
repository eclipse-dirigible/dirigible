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

import org.eclipse.dirigible.components.intent.model.GeneratesIntent;
import org.junit.jupiter.api.Test;

/**
 * A create-from that also writes the link row back to its source, and a prompt defaulting to a
 * source property (issue #7748): record a payment FROM the invoice it pays.
 */
class GeneratesLinkIntentTest {

    private static final String RECORD_PAYMENT = """
            name: sales
            entities:
              - name: PaymentMethod
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: CustomerPayment
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal, required: true }
                  - { name: reference, type: string }
                  - { name: date, type: date }
                relations:
                  - { name: Method, kind: manyToOne, to: PaymentMethod }
              - name: SalesInvoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string }
                  - { name: balance, type: decimal }
                relations:
                  - { name: Method, kind: manyToOne, to: PaymentMethod }
              - name: SalesInvoiceCustomerPayment
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal, required: true }
                  - { name: note, type: string }
                relations:
                  - { name: SalesInvoice, kind: manyToOne, to: SalesInvoice, composition: true, required: true }
                  - { name: CustomerPayment, kind: manyToOne, to: CustomerPayment, required: true, inlineCreate: false }
            generates:
              - name: record-payment
                from: SalesInvoice
                to: CustomerPayment
                label: Record payment
                defaults: { date: now }
                prompt:
                  - { field: amount, required: true, default: balance }
                  - { field: Method, default: Method }
                  - { field: reference }
                link:
                  entity: SalesInvoiceCustomerPayment
                  map: { amount: amount }
            """;

    @Test
    void parsesTheLinkAndThePromptDefault() {
        GeneratesIntent g = IntentParser.parse(RECORD_PAYMENT)
                                        .getGenerates()
                                        .get(0);
        assertTrue(g.hasLink());
        assertEquals("SalesInvoiceCustomerPayment", g.getLink()
                                                     .getEntity());
        assertEquals("amount", g.getLink()
                                .getMap()
                                .get("amount"));
        assertEquals("balance", g.getPrompt()
                                 .get(0)
                                 .getDefaultFrom());
    }

    @Test
    void anUnknownLinkEntityIsRefused() {
        assertIssue(RECORD_PAYMENT.replace("entity: SalesInvoiceCustomerPayment", "entity: Nowhere"),
                "link names unknown entity [Nowhere]");
    }

    @Test
    void theLinkMustBeACompositionChildOfTheSource() {
        assertIssue(RECORD_PAYMENT.replace("to: SalesInvoice, composition: true, required: true", "to: SalesInvoice, required: true"),
                "must declare a composition to-one relation to the source [SalesInvoice]");
    }

    @Test
    void theLinkMustPointAtTheTarget() {
        assertIssue(
                RECORD_PAYMENT.replace(
                        "- { name: CustomerPayment, kind: manyToOne, to: CustomerPayment, required: true, inlineCreate: false }",
                        "- { name: Method, kind: manyToOne, to: PaymentMethod }"),
                "declares no to-one relation to the target [CustomerPayment]");
    }

    @Test
    void twoRelationsToTheTargetNeedRelation() {
        String twice = RECORD_PAYMENT.replace("required: true, inlineCreate: false }",
                "required: true, inlineCreate: false }\n      - { name: Refund, kind: manyToOne, to: CustomerPayment }");
        assertIssue(twice, "declares several to-one relations to the target [CustomerPayment] - name the one");
        GeneratesIntent g = IntentParser
                                        .parse(twice.replace("entity: SalesInvoiceCustomerPayment",
                                                "entity: SalesInvoiceCustomerPayment\n      relation: CustomerPayment"))
                                        .getGenerates()
                                        .get(0);
        assertEquals("CustomerPayment", g.getLink()
                                         .getRelation());
    }

    @Test
    void anUnknownRelationIsRefused() {
        assertIssue(
                RECORD_PAYMENT.replace("entity: SalesInvoiceCustomerPayment",
                        "entity: SalesInvoiceCustomerPayment\n      relation: Refund"),
                "link relation [Refund] is not a to-one relation of [SalesInvoiceCustomerPayment] to the target [CustomerPayment]");
    }

    @Test
    void theLinkMapIsCheckedOnBothSides() {
        assertIssue(RECORD_PAYMENT.replace("map: { amount: amount }", "map: { paid: amount }"),
                "link map key [paid] is not a field of [SalesInvoiceCustomerPayment]");
        assertIssue(RECORD_PAYMENT.replace("map: { amount: amount }", "map: { amount: total }"),
                "link map value [total] is not a field of the target [CustomerPayment]");
        assertIssue(RECORD_PAYMENT.replace("map: { amount: amount }", "map: { amount: reference }"),
                "link map copies [reference] (string) into [amount] (decimal) - the two fields must have the same type");
    }

    @Test
    void aLinkFromACrossModelSourceIsRefused() {
        String yaml = """
                name: payments
                uses:
                  - { model: sales }
                entities:
                  - name: CustomerPayment
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                  - name: Allocation
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                generates:
                  - name: pay-invoice
                    from: SalesInvoice
                    fromUses: sales
                    forEntity: SalesInvoice
                    to: CustomerPayment
                    link: { entity: Allocation }
                """;
        assertIssue(yaml, "link requires a local source");
    }

    @Test
    void aPromptDefaultNamesASourcePropertyOfTheSameType() {
        assertIssue(RECORD_PAYMENT.replace("default: balance", "default: owed"),
                "prompt field [amount] defaults to [owed], which is not a field or to-one relation of the source [SalesInvoice]");
        assertIssue(RECORD_PAYMENT.replace("default: balance", "default: number"),
                "prompt field [amount] (decimal) defaults to [number] (string) - the two must have the same type");
        assertIssue(RECORD_PAYMENT.replace("{ field: Method, default: Method }", "{ field: Method, default: balance }"),
                "prompt relation [Method] defaults to [balance], which is not a to-one relation of the source to the same entity [PaymentMethod]");
    }

    private static void assertIssue(String yaml, String expected) {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains(expected)),
                "expected an issue containing [" + expected + "], got: " + ex.getIssues());
    }
}
