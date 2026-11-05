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
import static org.junit.jupiter.api.Assertions.assertFalse;

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
                GlueRendering.transitionGuard(t));
        assertEquals("Paid == 0", t.get("guardText"));
    }

    @Test
    void noGuardRendersEmptyExpressions() {
        IntentModel model = IntentParser.parse(YAML.replace("    when: \"Paid == 0\"\n", ""));
        Map<String, Object> t = GlueIntentGenerator.buildTransitionsForTest(model)
                                                   .get(0);
        // Rendered as the empty guardExpr, so the template's #if($guardExpr != "") renders nothing.
        assertEquals("", GlueRendering.transitionGuard(t));
        assertEquals("", t.get("guardText"));
    }

    /**
     * A personal transition (#8406) carries what its personal run is scoped by: the owner relation's FK
     * and the identity entity the caller is matched against - the coordinates a trigger's personal
     * assignee is resolved through.
     */
    @Test
    void aPersonalTransitionCarriesItsOwnerRelation() {
        IntentModel model = IntentParser.parse("""
                name: vacations
                entities:
                  - name: Employee
                    identity: email
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: email, type: string, unique: true }
                  - name: VacationStatus
                    function: Setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: VacationRequest
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Employee, kind: manyToOne, to: Employee, required: true, personal: true }
                      - { name: Status, kind: manyToOne, to: VacationStatus, function: EntityStatus, init: 1 }
                transitions:
                  - { name: WithdrawVacationRequest, forEntity: VacationRequest, from: [2, 3], setStatus: 6, personal: true }
                  - { name: CancelVacationRequest, forEntity: VacationRequest, from: [1], setStatus: 7 }
                """);
        List<Map<String, Object>> transitions = GlueIntentGenerator.buildTransitionsForTest(model);

        Map<String, Object> personal = transitions.get(0);
        assertEquals(Boolean.TRUE, personal.get("personal"));
        assertEquals("Employee", personal.get("personalFkProperty"));
        assertEquals("Employee", personal.get("personalTargetEntity"));
        assertEquals("Email", personal.get("personalIdentityProperty"));
        assertEquals(Boolean.FALSE, personal.get("personalCrossModel"));

        // The power-only transition is the descriptor it always was.
        Map<String, Object> power = transitions.get(1);
        assertFalse(power.containsKey("personal"), power.toString());
        assertFalse(power.containsKey("personalFkProperty"), power.toString());
    }

    /**
     * A move declaring {@code stamp: number} (#8493) carries the coordinates of the number it issues -
     * the series and the partition, with the partition relation's {@code init:} fallback - exactly as
     * the NumberStamp delegate's descriptor does; a move without it carries none.
     */
    @Test
    void aStampingTransitionCarriesTheNumberItIssues() {
        IntentModel model = IntentParser.parse("""
                name: protocols
                entities:
                  - name: Company
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: ProtocolStatus
                    function: Setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: Protocol
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: number, type: string, number: { series: VAT Protocol, per: Company, stampOn: issue } }
                    relations:
                      - { name: Company, kind: manyToOne, to: Company, init: 1 }
                      - { name: Status, kind: manyToOne, to: ProtocolStatus, function: EntityStatus, init: 1 }
                transitions:
                  - { name: IssueProtocol, forEntity: Protocol, from: [1], setStatus: 2, stamp: number }
                  - { name: CancelProtocol, forEntity: Protocol, from: [1], setStatus: 3 }
                seeds:
                  - name: companies
                    entity: Company
                    rows:
                      - { id: 1, name: Acme }
                """);
        List<Map<String, Object>> transitions = GlueIntentGenerator.buildTransitionsForTest(model);

        assertEquals(List.of(Map.of("field", "Number", "series", "VAT Protocol", "per", "Company", "perDefault", "1")), transitions.get(0)
                                                                                                                                   .get("stampNumbers"));
        assertFalse(transitions.get(1)
                               .containsKey("stampNumbers"),
                "only the move that declares the stamp issues the number");
    }
}
