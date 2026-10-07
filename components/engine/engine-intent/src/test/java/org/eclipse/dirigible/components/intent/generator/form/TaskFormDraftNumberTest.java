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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * The approval task form of a document whose number is stamped on issue shows that number BEFORE
 * the issue - a UUID placeholder (issue #7722). Its control is marked, so the form renders the
 * draft marker instead; the same holds for a {@code relation.field} path onto such a number.
 */
class TaskFormDraftNumberTest {

    private static final String YAML = """
            name: billing
            entities:
              - name: SalesInvoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string, length: 100, number: { series: Sales Invoice, stampOn: issue } }
                  - { name: issuedOn, type: date }
              - name: Reminder
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string }
                relations:
                  - { name: invoice, kind: manyToOne, to: SalesInvoice }
            processes:
              - name: Approval
                trigger: { onCreate: SalesInvoice }
                steps:
                  - { name: approve, kind: userTask, args: { assignee: manager, form: ApproveInvoice } }
                  - { name: done, kind: end }
              - name: Remind
                trigger: { onCreate: Reminder }
                steps:
                  - { name: review, kind: userTask, args: { assignee: manager, form: ReviewReminder } }
                  - { name: done, kind: end }
            forms:
              - name: ApproveInvoice
                forEntity: SalesInvoice
                fields: [number, issuedOn]
                actions: [approve]
              - name: ReviewReminder
                forEntity: Reminder
                fields: [invoice.number, note]
                actions: [done]
            """;

    @Test
    void theOwnNumberControlIsMarked() {
        assertEquals(Boolean.TRUE, control("ApproveInvoice", "NumberId").get("documentNumber"));
        assertNull(control("ApproveInvoice", "IssuedOnId").get("documentNumber"), "a date is not a number placeholder");
    }

    @Test
    void aRelationPathOntoTheNumberIsMarked() {
        assertEquals(Boolean.TRUE, control("ReviewReminder", "InvoiceNumberId").get("documentNumber"));
        assertNull(control("ReviewReminder", "NoteId").get("documentNumber"));
    }

    private static Map<String, Object> control(String formName, String id) {
        Map<String, Object> form = FormIntentGenerator.buildFormsForTest(IntentParser.parse(YAML))
                                                      .get(formName);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> controls = (List<Map<String, Object>>) form.get("form");
        return controls.stream()
                       .filter(c -> id.equals(c.get("id")))
                       .findFirst()
                       .orElseThrow(() -> new AssertionError("no control [" + id + "] in " + controls));
    }
}
