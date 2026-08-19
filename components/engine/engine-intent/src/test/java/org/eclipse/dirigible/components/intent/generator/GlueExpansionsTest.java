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
 * Verifies the {@code expansions} glue the {@link GlueIntentGenerator} emits: one descriptor per
 * master event, carrying the coordinates the handler needs to RECONCILE the child set rather than
 * rebuild it (dirigible #6817).
 *
 * <p>
 * The child primary key is what makes the reconciliation possible: a row whose period survives the
 * span change is addressed by id - re-spread in place - instead of being deleted and recreated. A
 * descriptor missing it would emit {@code child.} with no member and take the whole client-Java
 * registry down with it, so it is asserted here and not only end-to-end.
 */
class GlueExpansionsTest {

    private static final String YAML = """
            name: loans
            entities:
              - name: Loan
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: startDate, type: date }
                  - { name: endDate, type: date }
                  - { name: principal, type: decimal }
                  - { name: periods, type: integer }
              - name: LoanInstallment
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: dueDate, type: date }
                  - { name: amount, type: decimal }
                relations:
                  - { name: Loan, kind: manyToOne, to: Loan, composition: true, required: true }
            expansions:
              - name: installments
                from: Loan
                into: LoanInstallment
                unit: month
                between: { start: startDate, end: endDate }
                map: { dueDate: period }
                spread: { total: principal, into: amount, round: 2 }
                count: periods
            """;

    @Test
    void emitsOneHandlerPerMasterEventWithTheReconciliationCoordinates() {
        IntentModel model = IntentParser.parse(YAML);
        List<Map<String, Object>> expansions = GlueIntentGenerator.buildExpansionsForTest(model);

        assertEquals(2, expansions.size(), "one handler on the master's create event and one on its update event");
        assertEquals("InstallmentsExpansionOnCreate", expansions.get(0)
                                                                .get("className"));
        assertEquals("", expansions.get(0)
                                   .get("topicSuffix"));
        assertEquals("InstallmentsExpansionOnUpdate", expansions.get(1)
                                                                .get("className"));
        assertEquals("-updated", expansions.get(1)
                                           .get("topicSuffix"));

        Map<String, Object> onCreate = expansions.get(0);
        assertEquals("Loan", onCreate.get("masterEntity"));
        assertEquals("Id", onCreate.get("masterPk"));
        assertEquals("LoanInstallment", onCreate.get("childEntity"));
        assertEquals("Id", onCreate.get("childPk"), "the child key addresses a kept row for the in-place re-spread");
        assertEquals("Loan", onCreate.get("fkProperty"));
        assertEquals("DueDate", onCreate.get("mapProperty"));
        assertEquals("Principal", onCreate.get("spreadTotalProperty"));
        assertEquals("Amount", onCreate.get("spreadIntoProperty"));
        assertEquals("Periods", onCreate.get("countProperty"));
        assertEquals("Integer.valueOf(periods.size())", onCreate.get("countValue"));
    }
}
