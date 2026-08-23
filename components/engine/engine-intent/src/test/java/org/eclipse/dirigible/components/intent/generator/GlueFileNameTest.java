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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * The declarative {@code fileName:} pattern, on both server-side renders: the snapshot copy a
 * document mints on issue and the PDF a {@code notify} block attaches. Asserts the pre-rendered
 * expressions the templates consume - the tokens, the loads a relation hop needs, and the two
 * defaults, which used to disagree (the mint named the file after the numeric primary key while the
 * mail already used the document number, so one document reached the archive and the customer under
 * two different names).
 *
 * <p>
 * Tokens name the AUTHORED field and relation names, exactly as a notify subject does; the
 * PascalCase property of the generated entity is what the emitted expression reads.
 */
class GlueFileNameTest {

    private static final String SNAPSHOT_PATTERN = "{number}_{issued:yyyyMMdd}_{Company.shortName|Company.name}";
    private static final String NOTIFY_PATTERN = "{number}_{issued:yyyyMMdd}_{Customer.name}";

    private static final String NUMBER_FIELD = """
                  - name: number
                    type: string
                    documentTitle: true
                    number: { series: Invoice, stampOn: create }
            """;

    @Test
    void aSnapshotPatternBecomesASanitizingExpressionOverTheDocument() {
        String expression = String.valueOf(snapshot(yaml(SNAPSHOT_PATTERN, NOTIFY_PATTERN, true)).get("fileNameExpression"));

        // Every interpolated value goes through the SDK sanitizer; the literal separators are the
        // author's and are emitted verbatim.
        assertTrue(expression.contains("org.eclipse.dirigible.sdk.print.FileNames.part(document.Number)"),
                "a direct field must be read off the loaded document: " + expression);
        assertTrue(expression.contains("FileNames.part(document.Issued, \"yyyyMMdd\")"),
                "the :pattern modifier must reach the SDK formatter: " + expression);
        assertTrue(
                expression.contains("FileNames.first(") && expression.contains("Company.ShortName") && expression.contains("Company.Name"),
                "the |-alternatives must become a first-non-blank call: " + expression);
        assertTrue(expression.contains(" + \"_\" + "), "the authored separators must be emitted as literals: " + expression);
    }

    @Test
    void aSnapshotPatternWithoutTheVersionTokenGetsTheVersionSuffix() {
        String expression = String.valueOf(snapshot(yaml(SNAPSHOT_PATTERN, NOTIFY_PATTERN, true)).get("fileNameExpression"));

        // Two versions of a copy must never share a name, so a pattern that does not place the version
        // itself gets it appended.
        assertTrue(expression.endsWith("+ \"_v\" + version + \".pdf\""), "the version suffix and .pdf must close the name: " + expression);
    }

    @Test
    void aSnapshotPatternThatPlacesTheVersionItselfGetsNoSuffix() {
        String expression =
                String.valueOf(snapshot(yaml("{number}-v{Version}-{issued:yyyyMMdd}", NOTIFY_PATTERN, true)).get("fileNameExpression"));

        assertTrue(expression.contains("\"-v\" + version + \"-\""), "the authored version placement must be kept: " + expression);
        assertTrue(expression.endsWith("+ \".pdf\"") && !expression.endsWith("+ \"_v\" + version + \".pdf\""),
                "the author already placed the version, so no suffix may be appended: " + expression);
    }

    @Test
    void aSnapshotRelationHopIsDeclaredAsALoadOffTheDocument() {
        List<Map<String, Object>> loads = loads(snapshot(yaml(SNAPSHOT_PATTERN, NOTIFY_PATTERN, true)), "fileNameLoads");

        assertEquals(1, loads.size(), "one hop, one load: " + loads);
        assertEquals("Company", loads.get(0)
                                     .get("local"));
        assertEquals("Company", loads.get(0)
                                     .get("targetEntity"));
        assertEquals("Company", loads.get(0)
                                     .get("fkProperty"));
        assertEquals(false, loads.get(0)
                                 .get("crossModel"));
    }

    @Test
    void anAttachmentPatternBecomesASanitizingExpressionOverTheRecord() {
        String expression = String.valueOf(transition(yaml(SNAPSHOT_PATTERN, NOTIFY_PATTERN, true)).get("attachFileNameExpression"));

        assertTrue(expression.contains("FileNames.part(entity.Number)"), "a direct field must be read off the record: " + expression);
        assertTrue(expression.contains("FileNames.part(entity.Issued, \"yyyyMMdd\")"), "the date format must reach the SDK: " + expression);
        assertTrue(expression.contains("(Customer == null ? null : Customer.Name)"),
                "a relation hop must read the local the handler loads: " + expression);
        assertTrue(expression.endsWith("+ \".pdf\""), "a mailed copy has no version, only the extension: " + expression);
    }

    @Test
    void anAttachmentPatternsRelationSharesTheMessagesOwnLoad() {
        List<Map<String, Object>> loads = loads(transition(yaml(SNAPSHOT_PATTERN, NOTIFY_PATTERN, true)), "notifyRelationLoads");

        // The recipient (Customer.email) and the file name (Customer.name) name the same relation, and
        // both read the local named after it - declaring it twice would not compile.
        assertEquals(1, loads.size(), "the shared relation must be loaded exactly once: " + loads);
        assertEquals("Customer", loads.get(0)
                                      .get("local"));
    }

    @Test
    void aPatternMayAddARelationTheMessageTextNeverMentions() {
        List<Map<String, Object>> loads = loads(transition(yaml(SNAPSHOT_PATTERN, "{number}_{Company.name}", true)), "notifyRelationLoads");

        assertEquals(2, loads.size(), "the pattern's own relation must be added to the message's loads: " + loads);
        assertEquals("Customer", loads.get(0)
                                      .get("local"),
                "the message text's loads come first");
        assertEquals("Company", loads.get(1)
                                     .get("local"));
    }

    @Test
    void bothDefaultsAreTheDocumentNumberAndAgreeWithEachOther() {
        String withoutPatterns = yaml(null, null, true);
        String mint = String.valueOf(snapshot(withoutPatterns).get("fileNameExpression"));
        String mail = String.valueOf(transition(withoutPatterns).get("attachFileNameExpression"));

        // The document's own number, falling back to the entity name plus the id - the SAME expression
        // on both sides now. The mint adds the version, because a copy has one and a sent PDF has not.
        assertEquals("(document.Number == null || document.Number.isBlank() ? \"Invoice \" + document.Id : document.Number)"
                + " + \"_v\" + version + \".pdf\"", mint);
        assertEquals("(entity.Number == null || entity.Number.isBlank() ? \"Invoice \" + entity.Id : entity.Number) + \".pdf\"", mail);
    }

    @Test
    void aDocumentWithoutANumberFallsBackToItsEntityNameAndId() {
        String withoutNumber = yaml(null, null, false);

        assertEquals("\"Invoice \" + document.Id + \"_v\" + version + \".pdf\"", snapshot(withoutNumber).get("fileNameExpression"));
        assertEquals("\"Invoice \" + entity.Id + \".pdf\"", transition(withoutNumber).get("attachFileNameExpression"));
    }

    /**
     * The fixture: an invoice document with a snapshot child and a sending transition.
     *
     * @param snapshotFileName the snapshot's pattern, or {@code null} to leave it undeclared
     * @param notifyFileName the notify block's pattern, or {@code null} to leave it undeclared
     * @param numbered whether the document declares a {@code number:} field
     */
    private static String yaml(String snapshotFileName, String notifyFileName, boolean numbered) {
        return """
                name: billing
                entities:
                  - name: InvoiceStatus
                    function: Setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: Company
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                      - { name: shortName, type: string }
                  - name: Customer
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                      - { name: email, type: string }
                  - name: Invoice
                    function: Document
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                """ + (numbered ? NUMBER_FIELD : "") + """
                      - { name: issued, type: date }
                    relations:
                      - { name: Company,  kind: manyToOne, to: Company }
                      - { name: Customer, kind: manyToOne, to: Customer }
                      - { name: Status,   kind: manyToOne, to: InvoiceStatus, function: EntityStatus, init: 1 }
                  - name: InvoiceItem
                    function: DocumentItem
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: amount, type: decimal }
                    relations:
                      - { name: Invoice, kind: manyToOne, to: Invoice, composition: true, required: true }
                  - name: InvoiceCopy
                    function: Snapshot
                """ + line(4, "fileName", snapshotFileName) + """
                    relations:
                      - { name: Invoice, kind: manyToOne, to: Invoice, composition: true }

                transitions:
                  - name: SendInvoice
                    forEntity: Invoice
                    from: [1]
                    setStatus: 2
                    label: Send
                    icon: mail
                    notify:
                      to: Customer.email
                      subject: "Invoice {number}"
                      body: "Your invoice is attached."
                      attach: print
                """ + line(6, "fileName", notifyFileName);
    }

    /** One optional YAML scalar line at the given indentation, or nothing when the value is absent. */
    private static String line(int indent, String key, String value) {
        return value == null ? "" : " ".repeat(indent) + key + ": \"" + value + "\"\n";
    }

    private static Map<String, Object> snapshot(String yaml) {
        IntentModel model = IntentParser.parse(yaml);
        return SnapshotSupport.buildSnapshots(model, IntentEntities.byName(model), IntentEntities.compositionParents(model), null)
                              .get(0);
    }

    private static Map<String, Object> transition(String yaml) {
        return GlueIntentGenerator.buildTransitionsForTest(IntentParser.parse(yaml))
                                  .get(0);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> loads(Map<String, Object> entry, String key) {
        return (List<Map<String, Object>>) entry.get(key);
    }
}
