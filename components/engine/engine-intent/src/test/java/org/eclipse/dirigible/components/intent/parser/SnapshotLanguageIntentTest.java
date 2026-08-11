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
 * Coverage for the print render-language knob: {@code language:} / {@code languageFrom:} on a
 * {@code function: Snapshot} child (resolved on its document master) and on a notify block with
 * {@code attach: print} (resolved on the entity the message is about). The two are mutually
 * exclusive, the path is a one-hop {@code relation.field} to a string field, and the knob is
 * meaningless anywhere else - each misuse must fail at parse time, not mint wrong-language copies.
 */
class SnapshotLanguageIntentTest {

    /** A valid document + snapshot child; the placeholders let each test vary the knobs. */
    private static String yaml(String snapshotKnobs, String notifyKnobs) {
        return """
                name: billing
                entities:
                  - name: Partner
                    fields:
                      - { name: id,     type: integer, primaryKey: true, generated: true }
                      - { name: name,   type: string, required: true }
                      - { name: email,  type: string }
                      - { name: locale, type: string, length: 5 }
                      - { name: rating, type: integer }
                  - name: Invoice
                    fields:
                      - { name: id,   type: integer, primaryKey: true, generated: true }
                      - { name: note, type: string }
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
                    subject: "Invoice {id}"
                    body: "Attached."
                %s
                """.formatted(snapshotKnobs, notifyKnobs);
    }

    @Test
    void snapshotLanguageKnobsParse() {
        IntentModel model = IntentParser.parse(yaml("    languageFrom: partner.locale", "    attach: print"));
        assertEquals("partner.locale", model.getEntities()
                                            .get(3)
                                            .getLanguageFrom());
        IntentModel literal = IntentParser.parse(yaml("    language: bg", "    attach: print"));
        assertEquals("bg", literal.getEntities()
                                  .get(3)
                                  .getLanguage());
    }

    @Test
    void notifyLanguageFromParses() {
        IntentModel model = IntentParser.parse(yaml("", """
                    attach: print
                    languageFrom: partner.locale
                """));
        assertEquals("partner.locale", model.getNotifications()
                                            .get(0)
                                            .getLanguageFrom());
    }

    @Test
    void snapshotDeclaringBothKnobsIsRejected() {
        IntentValidationException ex = assertThrows(IntentValidationException.class,
                () -> IntentParser.parse(yaml("    language: bg\n    languageFrom: partner.locale", "    attach: print")));
        assertTrue(ex.getMessage()
                     .contains("mutually exclusive"),
                ex.getMessage());
    }

    @Test
    void languageKnobOnANonSnapshotEntityIsRejected() {
        String yaml = """
                name: billing
                entities:
                  - name: Invoice
                    language: bg
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getMessage()
                     .contains("function: Snapshot children only"),
                ex.getMessage());
    }

    @Test
    void snapshotLanguageFromMustBeAOneHopPath() {
        IntentValidationException ex = assertThrows(IntentValidationException.class,
                () -> IntentParser.parse(yaml("    languageFrom: locale", "    attach: print")));
        assertTrue(ex.getMessage()
                     .contains("one-hop relation.field"),
                ex.getMessage());
    }

    @Test
    void snapshotLanguageFromRejectsAnUnknownRelation() {
        IntentValidationException ex = assertThrows(IntentValidationException.class,
                () -> IntentParser.parse(yaml("    languageFrom: supplier.locale", "    attach: print")));
        assertTrue(ex.getMessage()
                     .contains("is not a to-one relation of [Invoice]"),
                ex.getMessage());
    }

    @Test
    void snapshotLanguageFromRejectsANonStringField() {
        IntentValidationException ex = assertThrows(IntentValidationException.class,
                () -> IntentParser.parse(yaml("    languageFrom: partner.rating", "    attach: print")));
        assertTrue(ex.getMessage()
                     .contains("must be a string field"),
                ex.getMessage());
    }

    @Test
    void notifyLanguageWithoutAttachIsRejected() {
        IntentValidationException ex =
                assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml("", "    language: bg")));
        assertTrue(ex.getMessage()
                     .contains("without attach: print"),
                ex.getMessage());
    }

    @Test
    void notifyDeclaringBothKnobsIsRejected() {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml("", """
                    attach: print
                    language: bg
                    languageFrom: partner.locale
                """)));
        assertTrue(ex.getMessage()
                     .contains("mutually exclusive"),
                ex.getMessage());
    }
}
