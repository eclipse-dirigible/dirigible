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

import org.eclipse.dirigible.components.intent.model.ExpansionIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.junit.jupiter.api.Test;

/**
 * Coverage for the {@code expansions:} block - a master's date span generated into child rows.
 */
class ExpansionIntentTest {

    private static final String LOAN = """
            name: loans
            entities:
              - name: Loan
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: startDate, type: date, required: true }
                  - { name: endDate, type: date, required: true }
                  - { name: principal, type: decimal, required: true }
                  - { name: periods, type: integer }
              - name: LoanInstallment
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: dueDate, type: date }
                  - { name: amount, type: decimal }
                  - { name: note, type: string, length: 100 }
                relations:
                  - { name: Loan, kind: manyToOne, to: Loan, composition: true, required: true }
            """;

    @Test
    void parsesTheFullShape() {
        String yaml = LOAN + """
                expansions:
                  - name: installments
                    from: Loan
                    into: LoanInstallment
                    unit: month
                    between: { start: startDate, end: endDate }
                    map: { dueDate: period }
                    defaults: { note: generated }
                    spread: { total: principal, into: amount, round: 2 }
                    count: periods
                """;
        IntentModel model = IntentParser.parse(yaml);
        assertEquals(1, model.getExpansions()
                             .size());
        ExpansionIntent expansion = model.getExpansions()
                                         .get(0);
        assertEquals("Loan", expansion.getFrom());
        assertEquals("LoanInstallment", expansion.getInto());
        assertEquals("month", expansion.getUnit());
        assertEquals("startDate", expansion.getBetween()
                                           .getStart());
        assertEquals("period", expansion.getMap()
                                        .get("dueDate"));
        assertEquals("principal", expansion.getSpread()
                                           .getTotal());
        assertEquals("periods", expansion.getCount());
    }

    @Test
    void skipDaysParseOnTheDayUnit() {
        String yaml = LOAN + """
                expansions:
                  - name: work-days
                    from: Loan
                    into: LoanInstallment
                    between: { start: startDate, end: endDate }
                    skipDays: [0, 6]
                    map: { dueDate: period }
                """;
        IntentModel model = IntentParser.parse(yaml);
        assertEquals(2, model.getExpansions()
                             .get(0)
                             .getSkipDays()
                             .size());
    }

    @Test
    void rejectsTheBrokenShapes() {
        assertIssue("""
                expansions:
                  - name: x
                    from: Nope
                    into: LoanInstallment
                    between: { start: startDate, end: endDate }
                    map: { dueDate: period }
                """, "expands unknown entity [Nope]");
        assertIssue("""
                expansions:
                  - name: x
                    from: Loan
                    into: Loan
                    between: { start: startDate, end: endDate }
                    map: { startDate: period }
                """, "requires a to-one relation from [Loan] back to [Loan]");
        assertIssue("""
                expansions:
                  - name: x
                    from: Loan
                    into: LoanInstallment
                    unit: fortnight
                    between: { start: startDate, end: endDate }
                    map: { dueDate: period }
                """, "unknown unit [fortnight]");
        assertIssue("""
                expansions:
                  - name: x
                    from: Loan
                    into: LoanInstallment
                    unit: month
                    between: { start: startDate, end: endDate }
                    skipDays: [0]
                    map: { dueDate: period }
                """, "skipDays applies to unit day only");
        assertIssue("""
                expansions:
                  - name: x
                    from: Loan
                    into: LoanInstallment
                    between: { start: principal, end: endDate }
                    map: { dueDate: period }
                """, "between.start [principal] is not a date field");
        assertIssue("""
                expansions:
                  - name: x
                    from: Loan
                    into: LoanInstallment
                    between: { start: startDate, end: endDate }
                    map: { dueDate: tomorrow }
                """, "map value [tomorrow] is not supported");
        assertIssue("""
                expansions:
                  - name: x
                    from: Loan
                    into: LoanInstallment
                    between: { start: startDate, end: endDate }
                    map: { dueDate: period }
                    spread: { total: startDate, into: amount }
                """, "spread.total [startDate] is not a numeric field");
        assertIssue("""
                expansions:
                  - name: x
                    from: Loan
                    into: LoanInstallment
                    between: { start: startDate, end: endDate }
                    map: { dueDate: period }
                    count: startDate
                """, "count [startDate] is not a numeric field");
    }

    private static void assertIssue(String expansionsYaml, String fragment) {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(LOAN + expansionsYaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains(fragment)),
                "expected an issue containing [" + fragment + "], got: " + ex.getIssues());
    }
}
