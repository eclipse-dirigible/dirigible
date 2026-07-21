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
 * Verifies the {@code aborts} glue collection: the topic binding, the abort message name and the
 * status-match expression the generated {@code MessageHandler} tests before correlating.
 */
class GlueAbortsTest {

    private static final String YAML = """
            name: sales
            entities:
              - name: OrderStatus
                function: Setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: SalesOrder
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                relations:
                  - { name: Status, kind: manyToOne, to: OrderStatus, function: EntityStatus, init: 1 }
            processes:
              - name: OrderApproval
                trigger: { onCreate: SalesOrder }
                abortOn: { status: [3, 4] }
                steps:
                  - { name: confirm, kind: userTask, args: { assignee: manager, form: ConfirmOrder } }
                  - { name: end, kind: end }
            forms:
              - { name: ConfirmOrder, forEntity: SalesOrder, fields: [Status], actions: [confirm] }
            """;

    @Test
    void rendersTheAbortListener() {
        IntentModel model = IntentParser.parse(YAML);
        List<Map<String, Object>> aborts = GlueIntentGenerator.buildAbortsForTest(model);
        assertEquals(1, aborts.size());
        Map<String, Object> abort = aborts.get(0);

        assertEquals("OrderApproval", abort.get("process"));
        assertEquals("SalesOrder", abort.get("entity"));
        assertEquals("OrderApprovalAbort", abort.get("messageName"));
        assertEquals("entity.Status != null && entity.Status == 3 || entity.Status != null && entity.Status == 4",
                abort.get("statusMatchExpression"));
    }

    @Test
    void noAbortOnMeansNoAbortGlue() {
        IntentModel model = IntentParser.parse(YAML.replace("abortOn: { status: [3, 4] }", ""));
        assertEquals(0, GlueIntentGenerator.buildAbortsForTest(model)
                                           .size());
    }
}
