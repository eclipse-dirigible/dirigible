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
 * Verifies the {@code transitions} entries the {@link GlueIntentGenerator} emits: the pre-rendered
 * allowed-statuses expression, the resolved EntityStatus property, and the optional Calc-backed
 * {@code when} guard.
 */
class GlueTransitionsTest {

    private static final String YAML = """
            name: billing
            entities:
              - name: InvoiceStatus
                function: Setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Invoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string, documentTitle: true }
                  - { name: paid, type: decimal }
                relations:
                  - { name: Status, kind: manyToOne, to: InvoiceStatus, function: EntityStatus, init: 1 }
            transitions:
              - name: VoidInvoice
                forEntity: Invoice
                from: [3, 4]
                setStatus: 8
                when: "Paid == 0"
                label: Void
                icon: ban
            """;

    @Test
    void rendersTheGuardsAndStatusWrite() {
        IntentModel model = IntentParser.parse(YAML);
        List<Map<String, Object>> transitions = GlueIntentGenerator.buildTransitionsForTest(model);
        assertEquals(1, transitions.size());
        Map<String, Object> t = transitions.get(0);

        assertEquals("VoidInvoice", t.get("name"));
        assertEquals("VoidInvoice", t.get("className"));
        assertEquals("Invoice", t.get("entity"));
        assertEquals("Status", t.get("statusProperty"));
        assertEquals("8", t.get("setStatus"));
        assertEquals("currentStatus == 3 || currentStatus == 4", t.get("allowedExpr"));
        assertEquals("3, 4", t.get("fromStatuses"));
        assertEquals("org.eclipse.dirigible.sdk.utils.Calc.eval(\"Paid\", source, 6)" + ".compareTo(new java.math.BigDecimal(\"0\")) == 0",
                t.get("guardExpr"));
        assertEquals("Paid == 0", t.get("guardText"));
    }

    @Test
    void noGuardRendersEmptyExpressions() {
        IntentModel model = IntentParser.parse(YAML.replace("    when: \"Paid == 0\"\n", ""));
        Map<String, Object> t = GlueIntentGenerator.buildTransitionsForTest(model)
                                                   .get(0);
        // The template's #if($guardExpr != "") renders nothing.
        assertEquals("", t.get("guardExpr"));
        assertEquals("", t.get("guardText"));
    }
}
