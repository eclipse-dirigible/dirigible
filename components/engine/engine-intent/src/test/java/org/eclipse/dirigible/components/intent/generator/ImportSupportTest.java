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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.components.intent.parser.IntentValidationException;
import org.junit.jupiter.api.Test;

/**
 * The {@code imports:} construct (#7856): what the parser refuses, and the import class the glue
 * generator plans from what it accepts.
 */
class ImportSupportTest {

    private static final String ENTITIES = """
            name: bank
            entities:
              - name: StatementLine
                fields:
                  - { name: id,          type: integer, primaryKey: true, generated: true }
                  - { name: statementId, type: string,  required: true }
                  - { name: lineNumber,  type: integer, required: true }
                  - { name: bookedOn,    type: date }
                  - { name: amount,      type: decimal, precision: 18, scale: 2 }
                  - { name: details,     type: string,  length: 500 }
                  - { name: matched,     type: boolean, defaultValue: "false", required: true }
              - name: Statement
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
              - name: Payment
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal }
                relations:
                  - { name: statement, kind: manyToOne, to: Statement, required: true }
            """;

    private static final String VALID = ENTITIES + """
            imports:
              - name: bankCsv
                into: StatementLine
                format: csv
                encoding: windows-1251
                csv: { separator: ";", decimal: ",", grouping: ".", dateFormat: dd.MM.yyyy, skipLines: 1 }
                map:
                  statementId: _file
                  lineNumber:  _line
                  bookedOn:    Дата
                  amount:      Сума
                  details:     Основание
                duplicateKey: [statementId, lineNumber]
              - name: swift
                into: StatementLine
                format: mt940
                map: { statementId: statementId, lineNumber: lineNumber, bookedOn: valueDate, amount: amount, details: details }
                duplicateKey: [statementId, lineNumber]
              - name: iso
                into: StatementLine
                format: camt053
                map: { statementId: statementId, lineNumber: lineNumber, amount: amount, details: counterpartyName }
            """;

    @Test
    void a_csv_import_is_planned_with_its_locale_rules_reader_and_duplicate_key() {
        List<Map<String, Object>> imports = build(IntentParser.parse(VALID));

        assertEquals(3, imports.size());
        Map<String, Object> csv = imports.get(0);
        assertEquals("BankCsv", csv.get("className"));
        assertEquals("StatementLine", csv.get("entity"));
        assertEquals("BANK_STATEMENT_LINE", csv.get("table"));
        assertEquals("DelimitedText.read(ImportText.decode(content, \"windows-1251\"), ';', true, 1, java.util.List.of(\"Дата\", \"Сума\","
                + " \"Основание\"))", csv.get("readerExpression"));
        assertEquals("ImportValues.of(',', \".\", \"dd.MM.yyyy\", null)", csv.get("valuesExpression"));
        assertEquals("\"proj.StatementLine.StatementLineFullAccess\"", csv.get("roles"),
                "the upload is gated by the entity's own write role, the one its controller checks");
        assertEquals(List.of(field("StatementId", "fileName", "text"), field("LineNumber", "Integer.toString(index + 1)", "integer"),
                field("BookedOn", "record.get(\"Дата\")", "date"), field("Amount", "record.get(\"Сума\")", "decimal"),
                field("Details", "record.get(\"Основание\")", "text")), csv.get("mapFields"));
        assertEquals(Boolean.TRUE, csv.get("hasDuplicateKey"));
        assertEquals(
                List.of(Map.of("property", "StatementId", "label", "statementId"), Map.of("property", "LineNumber", "label", "lineNumber")),
                csv.get("duplicateKey"));
    }

    @Test
    void statement_formats_read_canonical_values_and_camt_decodes_itself() {
        List<Map<String, Object>> imports = build(IntentParser.parse(VALID));

        assertEquals("Mt940.read(ImportText.decode(content, \"UTF-8\"))", imports.get(1)
                                                                                 .get("readerExpression"));
        assertEquals("ImportValues.canonical()", imports.get(1)
                                                        .get("valuesExpression"));
        assertEquals("Camt053.read(content)", imports.get(2)
                                                     .get("readerExpression"));
        assertEquals(Boolean.FALSE, imports.get(2)
                                           .get("hasDuplicateKey"));
        assertEquals(Set.of("StatementLine"), ImportSupport.targets(IntentParser.parse(VALID)));
    }

    @Test
    void a_csv_without_a_header_reads_columns_by_position() {
        List<Map<String, Object>> imports = build(IntentParser.parse(ENTITIES + """
                imports:
                  - name: plain
                    into: StatementLine
                    format: csv
                    csv: { header: false, separator: "\\t" }
                    map: { statementId: 1, lineNumber: _line, amount: "3" }
                """));

        assertEquals("DelimitedText.read(ImportText.decode(content, \"UTF-8\"), '\\t', false, 0, java.util.List.of(\"1\", \"3\"))",
                imports.get(0)
                       .get("readerExpression"));
    }

    @Test
    void an_import_the_runtime_could_not_perform_is_refused_at_parse() {
        List<String> issues = issues("""
                imports:
                  - name: broken
                    into: StatementLine
                    format: mt940
                    encoding: no-such-charset
                    csv: { separator: ";" }
                    map: { lineNumber: lineNumber, amount: Сума, nope: amount, id: amount }
                    duplicateKey: [details, lineNumber, lineNumber]
                  - name: xml
                    into: StatementLine
                    format: camt053
                    encoding: windows-1251
                    map: { statementId: statementId, lineNumber: lineNumber }
                  - name: child
                    into: Payment
                    format: csv
                    map: { amount: Amount, statement: Statement }
                  - name: excel
                    into: Ghost
                    format: xlsx
                  - name: rules
                    into: StatementLine
                    format: csv
                    csv: { separator: ";;", decimal: ",", grouping: ",", dateFormat: "dd.MM.yyyy bbb", skipLines: -1 }
                    map: { statementId: A, lineNumber: B }
                  - name: positions
                    into: StatementLine
                    format: csv
                    csv: { header: false }
                    map: { statementId: Name, lineNumber: _line }
                """);

        assertIssue(issues, "import [broken] has unknown encoding [no-such-charset]");
        assertIssue(issues, "import [broken] declares csv rules, but reads format mt940");
        assertIssue(issues, "import [broken] map [amount] reads [Сума], which an mt940 statement line does not carry");
        assertIssue(issues, "import [broken] map [nope] is not a field of [StatementLine]");
        assertIssue(issues, "import [broken] map [id] fills the primary key");
        assertIssue(issues, "import [broken] does not map [statementId], a required field of [StatementLine]");
        assertIssue(issues, "import [broken] duplicateKey [details] is not a mapped field");
        assertIssue(issues, "import [broken] duplicateKey names [lineNumber] twice");
        assertIssue(issues, "import [xml] declares an encoding, but a camt053 file is XML and declares its own");
        assertIssue(issues, "import [child] imports into [Payment], whose relation [statement] is required");
        assertIssue(issues, "import [child] map [statement] is not a field of [Payment] - an import fills fields only, not relations");
        assertIssue(issues, "import [excel] imports into unknown entity [Ghost]");
        assertIssue(issues, "import [excel] has format [xlsx]");
        assertIssue(issues, "import [rules] csv separator [;;] must be one character");
        assertIssue(issues, "import [rules] csv grouping and decimal are both [,]");
        assertIssue(issues, "import [rules] csv dateFormat [dd.MM.yyyy bbb] is not a valid date-time pattern");
        assertIssue(issues, "import [rules] csv skipLines [-1] must not be negative");
        assertIssue(issues, "import [positions] map [statementId] reads [Name], but the file has no header");
        assertTrue(issues.stream()
                         .noneMatch(issue -> issue.contains("[matched]")),
                "a required field with a default needs no mapping: " + issues);
    }

    @Test
    void a_camt_import_per_transaction_filters_booked_records_and_can_key_on_the_checksum() {
        List<Map<String, Object>> imports = build(IntentParser.parse(ENTITIES + """
                imports:
                  - name: isoBatch
                    into: StatementLine
                    format: camt053
                    camt053: { records: transactions }
                    where: { status: BOOK, reversal: false, currency: [EUR, BGN] }
                    map: { statementId: _checksum, lineNumber: transaction, amount: amount, details: counterpartyName }
                    duplicateKey: [statementId, lineNumber]
                  - name: plainCsv
                    into: StatementLine
                    format: csv
                    where: { Status: Booked }
                    map: { statementId: Id, lineNumber: _line }
                """));

        Map<String, Object> camt = imports.get(0);
        assertEquals("Camt053.readTransactions(content)", camt.get("readerExpression"));
        assertEquals(Boolean.TRUE, camt.get("hasWhere"));
        assertEquals(
                "\"BOOK\".equals(record.get(\"status\")) && \"false\".equals(record.get(\"reversal\"))"
                        + " && (\"EUR\".equals(record.get(\"currency\")) || \"BGN\".equals(record.get(\"currency\")))",
                camt.get("whereExpression"));
        assertEquals(field("StatementId", "checksum", "text"), ((List<?>) camt.get("mapFields")).get(0));
        assertEquals(field("LineNumber", "record.get(\"transaction\")", "integer"), ((List<?>) camt.get("mapFields")).get(1));

        Map<String, Object> csv = imports.get(1);
        assertEquals("DelimitedText.read(ImportText.decode(content, \"UTF-8\"), ',', true, 0, java.util.List.of(\"Id\", \"Status\"))",
                csv.get("readerExpression"), "a filtered column is read like a mapped one");
        assertEquals(Boolean.FALSE, build(IntentParser.parse(VALID)).get(0)
                                                                    .get("hasWhere"));
    }

    @Test
    void a_transaction_key_needs_records_per_transaction_and_a_filter_needs_a_record_key() {
        List<String> issues = issues("""
                imports:
                  - name: perEntry
                    into: StatementLine
                    format: camt053
                    map: { statementId: statementId, lineNumber: transaction }
                  - name: wrongRecords
                    into: StatementLine
                    format: camt053
                    camt053: { records: payments }
                    where: { _file: x.xml, sts: BOOK, status: [] }
                    map: { statementId: statementId, lineNumber: lineNumber }
                  - name: swiftRules
                    into: StatementLine
                    format: mt940
                    camt053: { records: transactions }
                    where: { status: BOOK }
                    map: { statementId: statementId, lineNumber: lineNumber }
                """);

        assertIssue(issues, "import [perEntry] map [lineNumber] reads [transaction], which only a record per transaction carries");
        assertIssue(issues, "import [wrongRecords] camt053 records [payments]");
        assertIssue(issues, "import [wrongRecords] where [_file] filters on _file");
        assertIssue(issues, "import [wrongRecords] where [sts] reads [sts], which a camt053 entry does not carry");
        assertIssue(issues, "import [wrongRecords] where [status] names no value");
        assertIssue(issues, "import [swiftRules] declares camt053 rules, but reads format mt940");
        assertIssue(issues, "import [swiftRules] where [status] reads [status], which an mt940 statement line does not carry");
    }

    @Test
    void an_unknown_key_in_an_import_is_reported_by_name() {
        List<String> issues = issues("""
                imports:
                  - name: typo
                    into: StatementLine
                    format: csv
                    csv: { delimiter: ";" }
                    map: { statementId: A, lineNumber: B }
                """);

        assertTrue(issues.stream()
                         .anyMatch(issue -> issue.contains("unknown key [delimiter]")),
                String.join("\n", issues));
    }

    private static List<String> issues(String imports) {
        IntentValidationException refused = assertThrows(IntentValidationException.class, () -> IntentParser.parse(ENTITIES + imports));
        return refused.getIssues();
    }

    private static void assertIssue(List<String> issues, String expected) {
        assertTrue(issues.stream()
                         .anyMatch(issue -> issue.startsWith(expected)),
                "missing issue [" + expected + "] in:\n" + String.join("\n", issues));
    }

    private static Map<String, Object> field(String property, String rawExpression, String conversion) {
        return Map.of("property", property, "rawExpression", rawExpression, "conversion", conversion);
    }

    private static List<Map<String, Object>> build(IntentModel model) {
        IntentGenerationContext context = new IntentGenerationContext(model, "/proj", "proj", "workspace", "bank", null);
        return ImportSupport.buildImports(model, IntentEntities.byName(model), IntentEntities.compositionParents(model),
                new IntentSettings(), context);
    }
}
