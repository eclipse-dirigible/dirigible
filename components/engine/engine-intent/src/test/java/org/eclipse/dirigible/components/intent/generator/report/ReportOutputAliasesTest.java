/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.generator.TestContexts;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.ReportIntent;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * {@link ReportIntentGenerator#outputAliases(ReportIntent)} names a report's columns as its own
 * generation names them - the contract a declared export (#7855) reads report rows by.
 */
class ReportOutputAliasesTest {

    private static final String INTENT = """
            name: sales
            entities:
              - name: Customer
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Invoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: issuedOn, type: date }
                  - { name: dueOn, type: date }
                  - { name: total, type: decimal }
                  - { name: taxBase, type: decimal }
                relations:
                  - { name: customer, kind: manyToOne, to: Customer }
            reports:
              - name: Everything
                source: Invoice
                dimensions: [customer.name, customer, "month(issuedOn)", "ageing(dueOn, [30, 60])"]
                measures: ["count(*)", "sum(taxBase)", "avg(total)", "max(customer.name)"]
            """;

    @Test
    void theAliasesAreTheColumnsTheGeneratedReportNames() {
        IntentModel model = IntentParser.parse(INTENT);
        ReportIntent report = model.getReports()
                                   .get(0);
        Map<String, Object> document = ReportIntentGenerator.buildForTest(TestContexts.context(model), report);

        List<String> generated = new ArrayList<>();
        for (Object column : (List<?>) document.get("columns")) {
            // `customer` also groups by the customer's key, which no row carries (#8102).
            if (!Boolean.FALSE.equals(((Map<?, ?>) column).get("select"))) {
                generated.add(String.valueOf(((Map<?, ?>) column).get("alias")));
            }
        }
        Map<String, String> aliases = ReportIntentGenerator.outputAliases(report);

        assertEquals(generated, new ArrayList<>(aliases.values()));
        assertEquals(aliases.get(ReportIntentGenerator.columnKey("sum(taxBase)")),
                aliases.get(ReportIntentGenerator.columnKey(" SUM( taxBase ) ")), "the key ignores whitespace and case");
    }

    @Test
    void aLedgerKindHasNoAuthoredColumns() {
        IntentModel model = IntentParser.parse("""
                name: books
                entities:
                  - name: Entry
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: account, type: string }
                      - { name: postedOn, type: date }
                      - { name: debit, type: decimal }
                      - { name: credit, type: decimal }
                reports:
                  - name: Balance
                    source: Entry
                    kind: balance
                    date: postedOn
                    debit: debit
                    credit: credit
                    dimensions: [account]
                """);

        assertTrue(ReportIntentGenerator.outputAliases(model.getReports()
                                                            .get(0))
                                        .isEmpty());
    }
}
