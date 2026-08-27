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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.generator.TestContexts;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.components.intent.parser.IntentValidationException;
import org.junit.jupiter.api.Test;

/**
 * A report's authored {@code parameters:} - the user-set inputs bound into its {@code WHERE}: the
 * emitted {@code parameters} declarations, the {@code conditions} rows that bind them (which must
 * keep round-tripping through the report editor's builder), and the parse-time rules that refuse a
 * parameter whose unset value would silently narrow the report.
 */
class ReportParametersTest {

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
                  - { name: createdAt, type: timestamp }
                  - { name: total, type: decimal }
                  - { name: note, type: string }
                relations:
                  - { name: Customer, kind: manyToOne, to: Customer }
            reports:
              - name: Revenue
                source: Invoice
                dimensions: [issuedOn, Customer.name]
                measures: ["sum(total)"]
                parameters:
                  - { name: fromDate, target: issuedOn, op: ge }
                  - { name: toDate, target: issuedOn, op: le }
                  - { name: minTotal, type: number, target: total, op: ge, initial: "0" }
                  - { name: customer, target: Customer.name, op: like }
            """;

    private static Map<String, Object> document(String intent, int index) {
        IntentModel model = IntentParser.parse(intent);
        return ReportIntentGenerator.buildForTest(TestContexts.context(model), model.getReports()
                                                                                    .get(index));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> rows(Map<String, Object> document, String key) {
        return (List<Map<String, Object>>) document.get(key);
    }

    private static String query(Map<String, Object> document) {
        return (String) document.get("query");
    }

    @Test
    void everyDeclaredParameterIsBoundAndDeclaredWithItsUnsetValue() {
        List<Map<String, Object>> parameters = rows(document(INTENT, 0), "parameters");

        assertEquals(List.of("fromDate", "toDate", "minTotal", "customer"), parameters.stream()
                                                                                      .map(parameter -> parameter.get("name"))
                                                                                      .toList());
        // The target field types the parameter - a timestamp binds as a DATE, see below.
        assertEquals(List.of("DATE", "DATE", "DECIMAL", "CHARACTER VARYING"), parameters.stream()
                                                                                        .map(parameter -> parameter.get("type"))
                                                                                        .toList());
        // A date window bound widens to all time and a like search to the pattern that matches every
        // value, so the report nobody has touched is the unfiltered one. A numeric bound has no neutral
        // value, so the author's own initial is what it binds.
        assertEquals(List.of("1900-01-01", "9999-12-31", "0", ""), parameters.stream()
                                                                             .map(parameter -> parameter.get("initial"))
                                                                             .toList());
    }

    @Test
    void aParameterBindsAsAStructuredConditionSoTheReportStaysEditable() {
        Map<String, Object> document = document(INTENT, 0);

        // The point of binding a plain comparison against a named marker: the term is a conditions row
        // the editor's builder owns, so declaring a parameter cannot park the report in free-style
        // mode. The WHERE must therefore be exactly what those rows re-emit.
        List<Map<String, Object>> conditions = rows(document, "conditions");
        assertEquals(4, conditions.size());
        // Every target here is nullable, so it reads through its empty value: a row with no date, no
        // total or no name must still be in the report before anyone filters it.
        assertEquals(Map.of("left", "COALESCE(Invoice.\"INVOICE_ISSUED_ON\", DATE '1900-01-01')", "operation", ">=", "right", ":fromDate"),
                conditions.get(0));
        assertEquals(Map.of("left", "COALESCE(Invoice.\"INVOICE_ISSUED_ON\", DATE '9999-12-31')", "operation", "<=", "right", ":toDate"),
                conditions.get(1));
        assertEquals(Map.of("left", "COALESCE(Invoice.\"INVOICE_TOTAL\", 0)", "operation", ">=", "right", ":minTotal"), conditions.get(2));
        // A like search matches anywhere in the value, which is also what makes its empty default match
        // every row.
        assertEquals(Map.of("left", "COALESCE(Customer.\"CUSTOMER_NAME\", '')", "operation", "LIKE", "right", "'%' || :customer || '%'"),
                conditions.get(3));

        assertTrue(query(document).contains("WHERE COALESCE(Invoice.\"INVOICE_ISSUED_ON\", DATE '1900-01-01') >= :fromDate"
                + " AND COALESCE(Invoice.\"INVOICE_ISSUED_ON\", DATE '9999-12-31') <= :toDate"
                + " AND COALESCE(Invoice.\"INVOICE_TOTAL\", 0) >= :minTotal"
                + " AND COALESCE(Customer.\"CUSTOMER_NAME\", '') LIKE '%' || :customer || '%'"), query(document));
    }

    @Test
    void aParameterOverARelationHopJoinsLikeADimension() {
        // The Customer.name parameter reaches through the same INNER JOIN a dimension would add - and
        // adds no second one when a dimension already reached that entity.
        String query = query(document(INTENT, 0));
        assertEquals(1, query.split("INNER JOIN", -1).length - 1, query);
    }

    @Test
    void aRequiredTargetKeepsThePlainComparison() {
        // Nothing to coalesce - the column cannot be null - so the comparison stays the index-friendly
        // one. Reading through the empty value is a correctness fix for a nullable target, not a
        // blanket rewrite.
        Map<String, Object> document =
                document(INTENT.replace("{ name: issuedOn, type: date }", "{ name: issuedOn, type: date, required: true }"), 0);

        assertEquals("Invoice.\"INVOICE_ISSUED_ON\"", rows(document, "conditions").get(0)
                                                                                  .get("left"));
    }

    @Test
    void aTimestampTargetIsComparedAsADate() {
        Map<String, Object> document = document(INTENT.replace("target: issuedOn, op: ge", "target: createdAt, op: ge")
                                                      .replace("target: issuedOn, op: le", "target: createdAt, op: le"),
                0);

        // The input is a date picker either way, so the instant is truncated to its day: comparing the
        // raw timestamp against midnight would drop the chosen day's own rows from the `le` bound.
        assertEquals("COALESCE(CAST(Invoice.\"INVOICE_CREATED_AT\" AS DATE), DATE '1900-01-01')", rows(document, "conditions").get(0)
                                                                                                                              .get("left"));
        assertEquals("DATE", rows(document, "parameters").get(0)
                                                         .get("type"));
    }

    @Test
    void parametersAreAppendedAfterTheAuthoredFilter() {
        Map<String, Object> document = document(INTENT.replace("    measures:", "    filter: \"total > 0\"\n    measures:"), 0);

        List<Map<String, Object>> conditions = rows(document, "conditions");
        assertEquals(5, conditions.size());
        assertEquals(":fromDate", conditions.get(1)
                                            .get("right"));
        assertTrue(
                query(document).contains(
                        "WHERE Invoice.\"INVOICE_TOTAL\" > 0 AND COALESCE(Invoice.\"INVOICE_ISSUED_ON\", DATE '1900-01-01') >= :fromDate"),
                query(document));
    }

    @Test
    void aFilterTheBuilderCannotRepresentStillGetsItsParameters() {
        // An OR-carrying filter is deliberately not decomposed into conditions (the editor opens the
        // report free-style), but the parameters must still bind - the alternative is an input the
        // report silently ignores.
        Map<String, Object> document =
                document(INTENT.replace("    measures:", "    filter: \"total > 0 OR note = 'x'\"\n    measures:"), 0);

        assertNull(document.get("conditions"));
        // ... and the filter is parenthesised, or ANDing a parameter onto a bare OR rebinds it - AND
        // binds tighter, so the report would silently answer a different question.
        assertTrue(query(document).contains("WHERE (Invoice.\"INVOICE_TOTAL\" > 0 OR Invoice.\"INVOICE_NOTE\" = 'x')"
                + " AND COALESCE(Invoice.\"INVOICE_ISSUED_ON\", DATE '1900-01-01') >= :fromDate"), query(document));
    }

    @Test
    void aReportWithoutParametersDeclaresNone() {
        Map<String, Object> document = document("""
                name: sales
                entities:
                  - name: Invoice
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: total, type: decimal }
                reports:
                  - name: Revenue
                    source: Invoice
                    measures: ["sum(total)"]
                """, 0);

        assertNull(document.get("parameters"));
        assertFalse(query(document).contains(":"), query(document));
    }

    @Test
    void aBalanceReportKeepsItsWindowAndTakesFurtherParameters() {
        Map<String, Object> document = document("""
                name: ledger
                entities:
                  - name: Entry
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: entryDate, type: date }
                      - { name: account, type: string }
                      - { name: debit, type: decimal }
                      - { name: credit, type: decimal }
                reports:
                  - name: TrialBalance
                    source: Entry
                    kind: balance
                    date: entryDate
                    debit: debit
                    credit: credit
                    dimensions: [account]
                    parameters:
                      - { name: account, target: account, op: eq, initial: "1000" }
                """, 0);

        assertEquals(List.of("fromDate", "toDate", "account"), rows(document, "parameters").stream()
                                                                                           .map(parameter -> parameter.get("name"))
                                                                                           .toList());
        assertTrue(query(document).contains("WHERE COALESCE(Entry.\"ENTRY_ACCOUNT\", '') = :account"), query(document));
    }

    @Test
    void aParameterCollidingWithTheBalanceWindowIsRejected() {
        assertThrows(IntentValidationException.class, () -> IntentParser.parse("""
                name: ledger
                entities:
                  - name: Entry
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: entryDate, type: date }
                      - { name: debit, type: decimal }
                      - { name: credit, type: decimal }
                reports:
                  - name: TrialBalance
                    source: Entry
                    kind: balance
                    date: entryDate
                    debit: debit
                    credit: credit
                    dimensions: [entryDate]
                    parameters:
                      - { name: fromDate, target: entryDate, op: ge }
                """));
    }

    /** A parameter whose declaration cannot be honoured is refused at parse, naming what is wrong. */
    @Test
    void malformedParametersAreRejected() {
        assertRejected("- { name: fromDate, target: issuedOn, op: between }", "unknown op");
        assertRejected("- { name: fromDate, target: issuedOn }", "has no op");
        assertRejected("- { target: issuedOn, op: ge }", "parameter with no name");
        assertRejected("- { name: from date, target: issuedOn, op: ge }", "plain identifier");
        assertRejected("- { name: language, target: issuedOn, op: ge }", "reserved name");
        // The generated report controller declares each parameter as a Java method parameter next to
        // its own locals, so a name that would shadow one of them - or is not a Java identifier at all
        // - is refused here instead of failing javac inside generated code.
        assertRejected("- { name: filter, target: issuedOn, op: ge }", "reserved name");
        assertRejected("- { name: limit, target: issuedOn, op: ge }", "reserved name");
        assertRejected("- { name: class, target: issuedOn, op: ge }", "not a Java keyword");
        assertRejected("- { name: fromDate, op: ge }", "has no target field");
        assertRejected("- { name: fromDate, target: nowhere, op: ge }", "is not a field of");
        assertRejected("- { name: fromDate, target: Customer, op: eq, initial: \"1\" }", "targets the relation");
        assertRejected("- { name: fromDate, target: Customer.name.first, op: eq, initial: \"a\" }", "ONE relation hop");
        assertRejected("- { name: fromDate, type: number, target: issuedOn, op: ge }", "declares type [number]");
        assertRejected("- { name: fromDate, type: money, target: issuedOn, op: ge }", "unknown type");
        assertRejected("- { name: search, target: total, op: like }", "op: like");
        assertRejected("- { name: flagged, target: paid, op: eq, initial: \"true\" }",
                "a parameter binds a date, timestamp, number or string");
        // No neutral "any value" exists for an equality or a numeric bound, so the author must say what
        // the report shows before the user sets it.
        assertRejected("- { name: minTotal, target: total, op: ge }", "needs an initial value");
        assertRejected("- { name: note, target: note, op: eq }", "needs an initial value");
        assertRejected("""
                - { name: fromDate, target: issuedOn, op: ge }
                      - { name: fromDate, target: issuedOn, op: le }""", "declared twice");
    }

    private static void assertRejected(String parameter, String expectedMessage) {
        String intent = """
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
                      - { name: total, type: decimal }
                      - { name: note, type: string }
                      - { name: paid, type: boolean }
                    relations:
                      - { name: Customer, kind: manyToOne, to: Customer }
                reports:
                  - name: Revenue
                    source: Invoice
                    dimensions: [issuedOn]
                    measures: ["sum(total)"]
                    parameters:
                      %s
                """.formatted(parameter);
        IntentValidationException failure = assertThrows(IntentValidationException.class, () -> IntentParser.parse(intent), parameter);
        assertTrue(failure.getMessage()
                          .contains(expectedMessage),
                failure.getMessage());
    }
}
