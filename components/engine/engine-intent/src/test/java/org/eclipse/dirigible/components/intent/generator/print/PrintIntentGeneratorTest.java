/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator.print;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.parsers.document.DocumentNode;
import org.eclipse.dirigible.parsers.document.parser.DocumentParser;
import org.junit.jupiter.api.Test;

class PrintIntentGeneratorTest {

    private static final String INTENT = """
            name: sales
            entities:
              - name: SalesInvoiceStatus
                kind: setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: SalesInvoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string, documentTitle: true }
                  - { name: date, type: date, required: true }
                  - { name: net, type: decimal, aggregate: true }
                  - { name: total, type: decimal, aggregate: true }
                relations:
                  - { name: Status, kind: manyToOne, to: SalesInvoiceStatus }
              - name: SalesInvoiceItem
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
                  - { name: quantity, type: decimal, required: true }
                  - { name: amount, type: decimal }
                relations:
                  - { name: SalesInvoice, kind: manyToOne, to: SalesInvoice, composition: true, required: true }
              - name: Customer
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
            """;

    private final IntentModel model = IntentParser.parse(INTENT);

    @Test
    void detectsOnlyDocumentMasters() {
        Map<EntityIntent, EntityIntent> masters = PrintIntentGenerator.documentMasters(model);
        assertEquals(1, masters.size());
        EntityIntent master = masters.keySet()
                                     .iterator()
                                     .next();
        assertEquals("SalesInvoice", master.getName());
        assertEquals("SalesInvoiceItem", masters.get(master)
                                                .getName());
    }

    @Test
    void buildsAParseableStandardTemplate() {
        Map<EntityIntent, EntityIntent> masters = PrintIntentGenerator.documentMasters(model);
        EntityIntent master = masters.keySet()
                                     .iterator()
                                     .next();
        String template = PrintIntentGenerator.buildTemplate(master, masters.get(master));

        // must parse cleanly with the document-template DSL parser
        DocumentNode document = new DocumentParser().parseDocument(template);
        assertEquals("sales-invoice-print", document.attributes()
                                                    .get("id"));

        // title + number subtitle in the header, bound to the document data contract
        assertTrue(template.contains(">Sales Invoice</text>"));
        assertTrue(template.contains("{{document.Number}}"));
        // header fields: date + the Status relation; never the PK or aggregates
        assertTrue(template.contains("<field label=\"Date\">{{document.Date}}</field>"));
        assertTrue(template.contains("<field label=\"Status\">{{document.Status}}</field>"));
        assertFalse(template.contains("{{document.Id}}</field>"));
        // items table bound to the items list, first column wide, numeric right-aligned
        assertTrue(template.contains("<table source=\"items\">"));
        assertTrue(template.contains("<column width=\"3*\" label=\"Name\">{{Name}}</column>"));
        assertTrue(template.contains("<column width=\"*\" align=\"right\" label=\"Quantity\">{{Quantity}}</column>"));
        // totals: net as a field, total emphasized
        assertTrue(template.contains("<field label=\"Net\">{{document.Net}}</field>"));
        assertTrue(template.contains("<total align=\"right\">{{document.Total}}</total>"));
    }

    @Test
    void generationIsDeterministic() {
        Map<EntityIntent, EntityIntent> masters = PrintIntentGenerator.documentMasters(model);
        EntityIntent master = masters.keySet()
                                     .iterator()
                                     .next();
        String first = PrintIntentGenerator.buildTemplate(master, masters.get(master));
        String second = PrintIntentGenerator.buildTemplate(master, masters.get(master));
        assertEquals(first, second);
    }
}
