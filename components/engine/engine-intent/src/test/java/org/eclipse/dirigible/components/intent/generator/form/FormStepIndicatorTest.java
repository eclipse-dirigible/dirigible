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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * Verifies the document-status-flow step-indicator metadata the {@link FormIntentGenerator} emits
 * on a BPM task form: the ordered non-terminal status names ({@code steps}) taken from the bound
 * entity's {@code function: EntityStatus} relation seeds, plus the current-status variable
 * ({@code statusVar}). A terminal (cancel/void/…) status is excluded, translation seeds are
 * ignored, and a non-task form gets no steps.
 */
class FormStepIndicatorTest {

    private static final String YAML = """
            name: sales
            entities:
              - name: OrderStatus
                function: Setting
                multilingual: true
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
                  - { name: description, type: string }
              - name: SalesOrder
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                relations:
                  - { name: Status, kind: manyToOne, to: OrderStatus, function: EntityStatus, init: 1 }
            processes:
              - name: OrderApproval
                trigger: { onCreate: SalesOrder }
                steps:
                  - { name: confirm, kind: userTask, args: { assignee: manager, form: ConfirmOrder } }
                  - { name: end, kind: end }
            forms:
              - { name: ConfirmOrder, forEntity: SalesOrder, fields: [Status], actions: [confirm] }
              - { name: PlainOrder, forEntity: SalesOrder, fields: [Status] }
            seeds:
              - name: order-statuses
                entity: OrderStatus
                rows:
                  - { id: 1, name: DRAFT, description: Being prepared }
                  - { id: 2, name: APPROVED, description: Ready to ship }
                  - { id: 3, name: SHIPPED }
                  - { id: 4, name: CANCELLED, description: Called off }
              - name: order-statuses-bg
                entity: OrderStatus
                language: bg
                rows:
                  - { id: 1, name: "Чернова" }
            """;

    @SuppressWarnings("unchecked")
    @Test
    void emitsTheStatusFlowStepsOnTheTaskForm() {
        IntentModel model = IntentParser.parse(YAML);
        Map<String, Map<String, Object>> forms = FormIntentGenerator.buildFormsForTest(model);

        Map<String, Object> meta = (Map<String, Object>) forms.get("ConfirmOrder")
                                                              .get("metadata");
        assertEquals(Boolean.TRUE, meta.get("taskForm"));
        // CANCELLED is terminal (excluded); the bg translation seed is ignored; order is seed order.
        // Each step carries its label plus the optional `description` seed column (absent -> no key).
        assertEquals(List.of(Map.of("label", "DRAFT", "description", "Being prepared"),
                Map.of("label", "APPROVED", "description", "Ready to ship"), Map.of("label", "SHIPPED")), meta.get("steps"));
        // The current-status model variable is the EntityStatus relation name.
        assertEquals("Status", meta.get("statusVar"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void noStepsOnANonTaskForm() {
        IntentModel model = IntentParser.parse(YAML);
        Map<String, Map<String, Object>> forms = FormIntentGenerator.buildFormsForTest(model);

        Map<String, Object> meta = (Map<String, Object>) forms.get("PlainOrder")
                                                              .get("metadata");
        assertFalse(meta.containsKey("taskForm"));
        assertFalse(meta.containsKey("steps"));
        assertTrue(meta.get("statusVar") == null);
    }
}
