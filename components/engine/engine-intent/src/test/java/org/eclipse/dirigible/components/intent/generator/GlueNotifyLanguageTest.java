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

import java.util.Map;

import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * The language a notify block is delivered in (#8787): a plain notice declares it like an
 * attachment does, and its subject and body are resolved in it.
 */
class GlueNotifyLanguageTest {

    private static final String YAML = """
            name: billing
            entities:
              - name: Language
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: code, type: string, length: 2 }
              - name: InvoiceStatus
                function: Setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Invoice
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string }
                  - { name: email,  type: string }
                relations:
                  - { name: Language, kind: manyToOne, to: Language }
                  - { name: Status, kind: manyToOne, to: InvoiceStatus, function: EntityStatus, init: 1 }
            transitions:
              - name: VoidInvoice
                forEntity: Invoice
                from: [1]
                setStatus: 2
                notify:
                  to: email
                  subject: "Invoice {number} was voided"
                  body: "Invoice {number} carries no amount due."
                  languageFrom: Language.code
            notifications:
              - name: invoiceCreated
                event: { onCreate: Invoice }
                to: email
                subject: "Invoice {number}"
                body: "Created."
                language: bg
            """;

    @Test
    void aPlainNoticeReadsItsLanguageOffTheRecord() {
        Map<String, Object> transition = GlueIntentGenerator.buildTransitionsForTest(IntentParser.parse(YAML))
                                                            .get(0);
        assertEquals("", transition.get("attach"));
        assertEquals("Language", transition.get("attachLanguageFkProperty"));
        assertEquals("Language", transition.get("attachLanguageTargetEntity"));
        assertEquals(
                "attachLanguageSource == null || attachLanguageSource.Code == null || attachLanguageSource.Code.isBlank()"
                        + " ? org.eclipse.dirigible.sdk.print.Print.defaultLanguage() : attachLanguageSource.Code.trim()",
                GlueRendering.attachLanguage(transition));
        assertEquals(GlueRendering.message("billing:billing-model.mail.transitions.VoidInvoice.body",
                "Invoice {number} carries no amount due.", "number", "entity.Number"), transition.get("notifyBodyExpression"));
    }

    @Test
    void aPlainNoticeTakesAFixedLanguage() {
        Map<String, Object> notification = GlueIntentGenerator.buildNotificationsForTest(IntentParser.parse(YAML))
                                                              .get(0);
        assertEquals("\"bg\"", GlueRendering.attachLanguage(notification));
        assertEquals("", notification.get("attachLanguageFkProperty"));
    }
}
