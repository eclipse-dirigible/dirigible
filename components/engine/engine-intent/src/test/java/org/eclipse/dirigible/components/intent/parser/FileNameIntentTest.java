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

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.junit.jupiter.api.Test;

/**
 * Coverage for the {@code fileName:} pattern that names a server-side render - a
 * {@code function: Snapshot} child's minted copies (resolved on its document master) and a notify
 * block's {@code attach: print} PDF (resolved on the entity the message is about).
 *
 * <p>
 * Every part of the pattern is validated at parse time rather than left to render time: a token
 * that resolved to nothing would produce a name indistinguishable from every other copy's, which is
 * the exact failure the knob exists to fix.
 */
class FileNameIntentTest {

    /** A valid document + snapshot child + notification; the placeholders vary the knobs. */
    private static String yaml(String snapshotKnobs, String notifyKnobs) {
        return """
                name: billing
                entities:
                  - name: Partner
                    fields:
                      - { name: id,        type: integer, primaryKey: true, generated: true }
                      - { name: name,      type: string, required: true }
                      - { name: shortName, type: string }
                      - { name: email,     type: string }
                  - name: Invoice
                    fields:
                      - { name: id,     type: integer, primaryKey: true, generated: true }
                      - { name: number, type: string }
                      - { name: issued, type: date }
                      - { name: note,   type: string }
                    relations:
                      - { name: partner, kind: manyToOne, to: Partner }
                  - name: InvoiceItem
                    fields:
                      - { name: id,     type: integer, primaryKey: true, generated: true }
                      - { name: amount, type: decimal }
                    relations:
                      - { name: invoice, kind: manyToOne, to: Invoice, composition: true }
                  - name: InvoiceCopy
                    function: Snapshot
                %s
                    relations:
                      - { name: invoice, kind: manyToOne, to: Invoice, composition: true }
                notifications:
                  - name: invoiceCreated
                    event: { onCreate: Invoice }
                    to: partner.email
                    subject: "Invoice {number}"
                    body: "Attached."
                %s
                """.formatted(snapshotKnobs, notifyKnobs);
    }

    private static String message(String snapshotKnobs, String notifyKnobs) {
        return assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml(snapshotKnobs, notifyKnobs))).getMessage();
    }

    @Test
    void aFullPatternParsesOnBothSides() {
        IntentModel model =
                IntentParser.parse(yaml("    fileName: \"{number}_{issued:yyyyMMdd}_{partner.shortName|partner.name}-v{Version}\"", """
                            attach: print
                            fileName: "{number}_{issued:yyyy-MM-dd}_{partner.name}"
                        """));

        assertEquals("{number}_{issued:yyyyMMdd}_{partner.shortName|partner.name}-v{Version}", model.getEntities()
                                                                                                    .get(3)
                                                                                                    .getFileName());
        assertEquals("{number}_{issued:yyyy-MM-dd}_{partner.name}", model.getNotifications()
                                                                         .get(0)
                                                                         .getFileName());
    }

    @Test
    void aNotifyFileNameWithoutAttachIsRejected() {
        // The knob names the attached render; a plain-text message has no file to name, so a pattern
        // there is an authored promise nothing consumes.
        assertTrue(message("", "    fileName: \"{number}\"").contains("declares fileName without attach: print"),
                message("", "    fileName: \"{number}\""));
    }

    @Test
    void aFileNameOnANonSnapshotEntityIsRejected() {
        String yaml = yaml("", "    attach: print").replace("""
                  - name: Invoice
                    fields:
                """, """
                  - name: Invoice
                    fileName: "{number}"
                    fields:
                """);

        assertTrue(assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml)).getMessage()
                                                                                                .contains(
                                                                                                        "applies to function: Snapshot children only"),
                "a fileName on an ordinary entity names nothing");
    }

    @Test
    void aPatternThatInterpolatesNothingIsRejected() {
        // A constant name would call every copy the same thing - the very failure the knob replaces.
        assertTrue(message("    fileName: \"invoice\"", "    attach: print").contains("interpolates nothing"),
                message("    fileName: \"invoice\"", "    attach: print"));
    }

    @Test
    void unbalancedBracesAreRejected() {
        assertTrue(message("    fileName: \"{number\"", "    attach: print").contains("interpolates nothing"),
                "an unopened token has no closing brace at all");
        assertTrue(message("    fileName: \"{number}_{issued\"", "    attach: print").contains("unclosed { token"),
                message("    fileName: \"{number}_{issued\"", "    attach: print"));
        assertTrue(message("    fileName: \"{num{ber}}\"", "    attach: print").contains("unbalanced or nested braces"),
                message("    fileName: \"{num{ber}}\"", "    attach: print"));
    }

    @Test
    void anUnknownFieldIsRejected() {
        assertTrue(message("    fileName: \"{missing}\"", "    attach: print").contains("[missing] is not a field of [Invoice]"),
                message("    fileName: \"{missing}\"", "    attach: print"));
    }

    @Test
    void anUnknownRelationIsRejected() {
        assertTrue(
                message("    fileName: \"{supplier.name}\"", "    attach: print").contains(
                        "[supplier] is not a to-one relation of [Invoice]"),
                message("    fileName: \"{supplier.name}\"", "    attach: print"));
    }

    @Test
    void anUnknownFieldOnARelationTargetIsRejected() {
        assertTrue(message("    fileName: \"{partner.iban}\"", "    attach: print").contains("[iban] is not a field of [Partner]"),
                message("    fileName: \"{partner.iban}\"", "    attach: print"));
    }

    @Test
    void aMultiHopPathIsRejected() {
        assertTrue(
                message("    fileName: \"{invoice.partner.name}\"", "    attach: print").contains(
                        "is not a field or a one-hop relation.field path"),
                message("    fileName: \"{invoice.partner.name}\"", "    attach: print"));
    }

    @Test
    void eachAlternativeOperandIsValidatedOnItsOwn() {
        // A fallback is not a licence to leave the second branch unchecked - it would be the one that
        // renders exactly when the first is blank, i.e. on the records that need it.
        assertTrue(message("    fileName: \"{number|missing}\"", "    attach: print").contains("[missing] is not a field of [Invoice]"),
                message("    fileName: \"{number|missing}\"", "    attach: print"));
    }

    @Test
    void aDateFormatOnANonDateFieldIsRejected() {
        assertTrue(message("    fileName: \"{note:yyyyMMdd}\"", "    attach: print").contains("applies to a date or timestamp field"),
                message("    fileName: \"{note:yyyyMMdd}\"", "    attach: print"));
    }

    @Test
    void anInvalidDateFormatIsRejected() {
        assertTrue(message("    fileName: \"{issued:yyyyQQQQQQQ}\"", "    attach: print").contains("is not a valid date format"),
                message("    fileName: \"{issued:yyyyQQQQQQQ}\"", "    attach: print"));
    }

    @Test
    void anEmptyDateFormatIsRejected() {
        assertTrue(message("    fileName: \"{issued:}\"", "    attach: print").contains("empty date pattern after the colon"),
                message("    fileName: \"{issued:}\"", "    attach: print"));
    }

    @Test
    void theVersionTokenIsRejectedOnAMailedCopy() {
        // Only a snapshot has versions; a sent PDF has none, and a pattern using it there would read as
        // a working declaration.
        assertTrue(message("", """
                    attach: print
                    fileName: "{number}-v{Version}"
                """).contains("only a snapshot copy has"), message("", """
                    attach: print
                    fileName: "{number}-v{Version}"
                """));
    }

    /**
     * A fan-out whose ANCHOR record is the document ({@code attach: recordPrint}): the copy is rendered
     * ONCE, before the per-row loop, so only fields of the anchor itself are readable in its name.
     */
    private static String fanOutYaml(String fileName) {
        return """
                name: payroll
                entities:
                  - name: RunStatus
                    function: Setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: Employee
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                      - { name: email, type: string }
                  - name: PayrollRun
                    function: Document
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: month, type: string, documentTitle: true }
                    relations:
                      - { name: Status, kind: manyToOne, to: RunStatus, function: EntityStatus, init: 1 }
                  - name: Payslip
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: net, type: decimal }
                    relations:
                      - { name: PayrollRun, kind: manyToOne, to: PayrollRun, composition: true, required: true }
                      - { name: Employee, kind: manyToOne, to: Employee, required: true }
                  - name: PayslipItem
                    function: DocumentItem
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: amount, type: decimal }
                    relations:
                      - { name: Payslip, kind: manyToOne, to: Payslip, composition: true, required: true }

                processes:
                  - name: RunPosting
                    trigger: { onCreate: PayrollRun }
                    steps:
                      - { name: post, kind: userTask, args: { assignee: approver, next: mailPayslips } }
                      - name: mailPayslips
                        kind: serviceTask
                        args:
                          notify:
                            forEach: Payslip
                            to: Employee.email
                            subject: "Payslip {record.month}"
                            body: "Your payslip is attached."
                            attach: recordPrint
                            fileName: "%s"
                          next: end
                      - { name: end, kind: end }
                """.formatted(fileName);
    }

    @Test
    void aFieldOfTheAnchorNamesARecordPrintCopy() {
        IntentModel model = IntentParser.parse(fanOutYaml("{month}-run"));

        assertEquals(1, model.getProcesses()
                             .size());
    }

    @Test
    void aRelationHopIsRejectedOnARecordPrintFanOut() {
        // The anchor's document is rendered once, outside the per-row loop, where the block's relation
        // locals do not exist yet - so a hop would read a local nothing declares.
        assertTrue(assertThrows(IntentValidationException.class, () -> IntentParser.parse(fanOutYaml("{Status.name}"))).getMessage()
                                                                                                                       .contains(
                                                                                                                               "rendered once for the"),
                "a fan-out's anchor document cannot read a relation hop");
    }
}
