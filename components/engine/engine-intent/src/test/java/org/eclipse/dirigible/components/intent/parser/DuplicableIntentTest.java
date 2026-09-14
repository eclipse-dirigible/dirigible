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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.junit.jupiter.api.Test;

/**
 * The object form of {@code duplicable} (#7358): which fields a copy must NOT carry over from the
 * source. Each refusal below exists because the same authoring mistake, accepted, produces a
 * duplicate that is silently wrong (a copy dated last month) or one the server rejects every time.
 */
class DuplicableIntentTest {

    private static final String INVOICES = """
            name: invoices
            entities:
              - name: Customer
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: SalesInvoiceStatus
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: SalesInvoice
                duplicable: true
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string, number: { series: Sales Invoice } }
                  - { name: date, type: date, required: true }
                  - { name: due, type: date, calculatedActionOnCreate: custom.DueDate }
                  - { name: taxEventDate, type: date, calculatedActionOnCreate: custom.TaxEventDate }
                  - { name: note, type: string }
                  - { name: total, type: decimal, aggregate: true }
                  - { name: printedAt, type: date, readOnly: true }
                  - { name: period, type: month }
                relations:
                  - { name: customer, kind: manyToOne, to: Customer, required: true }
                  - { name: status, kind: manyToOne, to: SalesInvoiceStatus, function: EntityStatus }
              - name: SalesInvoiceItem
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: quantity, type: decimal }
                relations:
                  - { name: salesInvoice, kind: manyToOne, to: SalesInvoice, composition: true, required: true }
            """;

    private static final String OBJECT_FORM = """
                duplicable:
                  defaults: { date: now }
                  reset: [due, taxEventDate]
            """.stripTrailing();

    @Test
    void theBooleanShorthandStillParsesAndCarriesNoRules() {
        EntityIntent invoice = entity(IntentParser.parse(INVOICES), "SalesInvoice");

        assertTrue(invoice.isDuplicable(), "duplicable: true is still the shorthand for the Duplicate action");
        assertNotNull(invoice.getDuplicable(), "the shorthand normalizes to the empty object form");
        assertTrue(invoice.getDuplicable()
                          .getReset()
                          .isEmpty());
        assertTrue(invoice.getDuplicable()
                          .getDefaults()
                          .isEmpty(),
                "an entity that says nothing keeps today's copy-everything behaviour");
    }

    @Test
    void falseIsNotDuplicable() {
        EntityIntent invoice = entity(IntentParser.parse(INVOICES.replace("duplicable: true", "duplicable: false")), "SalesInvoice");

        assertFalse(invoice.isDuplicable());
    }

    @Test
    void theObjectFormParsesBothKeys() {
        EntityIntent invoice = entity(IntentParser.parse(objectForm()), "SalesInvoice");

        assertTrue(invoice.isDuplicable());
        assertEquals(List.of("due", "taxEventDate"), invoice.getDuplicable()
                                                            .getReset());
        assertEquals(Map.of("date", "now"), invoice.getDuplicable()
                                                   .getDefaults());
    }

    @Test
    void aNameThatIsNeitherFieldNorRelationIsRejected() {
        assertIssue(objectForm("reset: [dueDate]"), "names [dueDate] which is not a field or a to-one relation of entity [SalesInvoice]");
    }

    @Test
    void theDocumentNumberIsRejectedBecauseItIsAlreadyDropped() {
        assertIssue(objectForm("reset: [number]"), "the document number is minted by the server and never copied");
    }

    @Test
    void thePrimaryKeyIsRejectedBecauseItIsAlreadyDropped() {
        assertIssue(objectForm("reset: [id]"), "the record's identity is minted by the server and never copied");
    }

    @Test
    void anAggregateIsRejectedBecauseItIsAlreadyDropped() {
        assertIssue(objectForm("reset: [total]"), "an aggregate is derived from the lines and never copied");
    }

    @Test
    void aReadOnlyFieldIsRejectedBecauseItIsAlreadyDropped() {
        assertIssue(objectForm("reset: [printedAt]"), "a readOnly field is never copied");
    }

    @Test
    void theStatusRelationIsRejectedBecauseItIsAlreadyDropped() {
        assertIssue(objectForm("reset: [status]"), "the status of a copy is the lifecycle's initial one and never copied");
    }

    @Test
    void aNameInBothResetAndDefaultsIsRejectedAsAContradiction() {
        assertIssue(objectForm("defaults: { due: now }\n      reset: [due]"), "names [due] in both reset and defaults");
    }

    @Test
    void nowOnANonDateFieldIsRejected() {
        assertIssue(objectForm("defaults: { note: now }"), "now is today in the field's own shape");
    }

    @Test
    void nowOnAMonthFieldIsAccepted() {
        EntityIntent invoice = entity(IntentParser.parse(objectForm("defaults: { period: now }")), "SalesInvoice");

        assertEquals(Map.of("period", "now"), invoice.getDuplicable()
                                                     .getDefaults());
    }

    @Test
    void resettingARequiredFieldWithNoCreateTimeRuleIsRejected() {
        assertIssue(objectForm("reset: [date]"), "would make every duplicate fail");
    }

    @Test
    void resettingARequiredRelationIsAccepted() {
        // A relation carries no create-time rule to check, and a copy that deliberately asks for a new
        // counterparty is a legitimate thing to author.
        EntityIntent invoice = entity(IntentParser.parse(objectForm("reset: [customer]")), "SalesInvoice");

        assertEquals(List.of("customer"), invoice.getDuplicable()
                                                 .getReset());
    }

    @Test
    void aScalarThatIsNeitherBooleanNorMappingIsRejected() {
        assertIssue(INVOICES.replace("duplicable: true", "duplicable: reset"), "is neither true/false nor a mapping");
    }

    @Test
    void anUnknownKeyInsideTheObjectFormIsReported() {
        assertIssue(objectForm("resets: [due]"), "resets");
    }

    private static String objectForm() {
        return INVOICES.replace("    duplicable: true", OBJECT_FORM);
    }

    private static String objectForm(String body) {
        return INVOICES.replace("    duplicable: true", "    duplicable:\n      " + body);
    }

    private static void assertIssue(String yaml, String expected) {
        IntentValidationException exception = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));

        assertTrue(exception.getMessage()
                            .contains(expected),
                () -> "expected an issue containing [" + expected + "] but got: " + exception.getMessage());
    }

    private static EntityIntent entity(IntentModel model, String name) {
        return model.getEntities()
                    .stream()
                    .filter(entity -> name.equals(entity.getName()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("no entity [" + name + "]"));
    }
}
