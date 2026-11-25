/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator.form;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.components.intent.parser.IntentValidationException;
import org.junit.jupiter.api.Test;

/**
 * A task form's {@code items:} shows the record's lines (issue #7907): the approver of an invoice
 * saw number, customer and total, and approved an amount without seeing what is billed.
 */
class TaskFormItemsTest {

    private static final String YAML = """
            name: billing
            entities:
              - name: Invoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string }
                  - { name: total, type: decimal, scale: 2 }
              - name: InvoiceItem
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
                  - { name: quantity, type: decimal, scale: 3 }
                  - { name: deliveredOn, type: date }
                relations:
                  - { name: Invoice, kind: manyToOne, to: Invoice, composition: true }
            processes:
              - name: Approval
                trigger: { onCreate: Invoice }
                steps:
                  - { name: approve, kind: userTask, args: { assignee: approver, form: ApproveInvoice } }
                  - { name: done, kind: end }
            forms:
              - name: ApproveInvoice
                forEntity: Invoice
                fields: [number, total]
                items: { entity: InvoiceItem, fields: [name, quantity, deliveredOn] }
                actions: [approve]
            """;

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> controls(String yaml) {
        return (List<Map<String, Object>>) FormIntentGenerator.buildFormsForTest(IntentParser.parse(yaml))
                                                              .get("ApproveInvoice")
                                                              .get("form");
    }

    @Test
    void theLinesAreAReadOnlyTableUnderTheFields() {
        List<Map<String, Object>> controls = controls(YAML);
        Map<String, Object> table = controls.get(controls.size() - 2);
        assertEquals("table", table.get("controlId"), "the lines table sits between the fields and the action row: " + controls);
        assertEquals("Invoice Items", table.get("label"), "the heading the document page gives the same lines");
        assertEquals(
                List.of(Map.of("label", "Name", "value", "Name", "controlId", "input-textfield"),
                        Map.of("label", "Quantity", "value", "Quantity", "controlId", "input-number", "pattern", "#,##0.000"),
                        Map.of("label", "Delivered On", "value", "DeliveredOn", "controlId", "input-date")),
                table.get("headers"), "one column per listed field, typed and patterned like a header field");
    }

    /**
     * The rows are located the way the header is: through a process variable the trigger seeds, never a
     * path baked in here - plus the line's key to the record, a logical model name.
     */
    @Test
    void theTableNamesTheLocatorAndTheMasterKeyNotAPath() {
        Map<String, Object> table = controls(YAML).stream()
                                                  .filter(c -> "table".equals(c.get("controlId")))
                                                  .findFirst()
                                                  .orElseThrow();
        assertEquals("__InvoiceItemRowsUrl", table.get("source"));
        assertEquals("__InvoiceItemRows", table.get("model"), "__-prefixed, so a completion never posts the rows back");
        assertEquals("Invoice", table.get("masterProperty"));
        assertTrue(table.toString()
                        .indexOf("/services/") < 0,
                "no template-engine path may appear in the emitted control: " + table);
    }

    @Test
    void aFormWithoutItemsHasNoTable() {
        String yaml = YAML.replace("    items: { entity: InvoiceItem, fields: [name, quantity, deliveredOn] }\n", "");
        assertTrue(controls(yaml).stream()
                                 .noneMatch(c -> "table".equals(c.get("controlId"))));
    }

    @Test
    void theItemsEntityMustBeACompositionChildOfTheFormsEntity() {
        assertIssue(YAML.replace("composition: true", "composition: false"),
                "form [ApproveInvoice] items entity [InvoiceItem] is not a composition child of [Invoice]");
    }

    @Test
    void aColumnIsOneOfTheLinesOwnFields() {
        assertIssue(YAML.replace("fields: [name, quantity, deliveredOn]", "fields: [name, Invoice]"),
                "form [ApproveInvoice] items field [Invoice] is not a field of [InvoiceItem]");
    }

    /**
     * The lines are located through the trigger, so - as for an editable relation - only on its task
     * form.
     */
    @Test
    void theItemsNeedATaskFormOfTheTriggerEntity() {
        String yaml = YAML.replace("form: ApproveInvoice", "form: Other")
                          .replace("forms:\n", "forms:\n  - { name: Other, forEntity: Invoice, fields: [number], actions: [approve] }\n");
        assertIssue(yaml, "form [ApproveInvoice] items can only be shown on a TASK form");
    }

    private static void assertIssue(String yaml, String fragment) {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains(fragment)),
                "expected an issue containing [" + fragment + "], got: " + ex.getIssues());
    }
}
