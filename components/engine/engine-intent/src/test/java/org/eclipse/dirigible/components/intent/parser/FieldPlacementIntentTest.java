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

import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.junit.jupiter.api.Test;

/**
 * {@code placement: main | details | hidden} (dirigible #8369): where a field sits on the record
 * page. The rail shows values, so {@code details} needs a read-only field; a hidden field is never
 * an input and never a column, so nothing may ask for it as one.
 */
class FieldPlacementIntentTest {

    private static String shipment(String fields, String extra) {
        return """
                name: logistics
                entities:
                  - name: Shipment
                %s    fields:
                      - { name: id,        type: integer, primaryKey: true, generated: true }
                      - { name: reference, type: string, length: 40 }
                %s
                    relations:
                      - { name: order, kind: manyToOne, to: Order }
                  - name: Order
                    related:
                      - { entity: Shipment, show: [reference] }
                    fields:
                      - { name: id,     type: integer, primaryKey: true, generated: true }
                      - { name: number, type: string, length: 20 }
                """.formatted(extra, fields);
    }

    private static String field(String declaration) {
        return "      - " + declaration;
    }

    @Test
    void mainAndDetailsOnAReadOnlyFieldParse() {
        IntentModel model = IntentParser.parse(shipment(field("{ name: carrierNote, type: text, readOnly: true, placement: main }") + "\n"
                + field("{ name: dispatched, type: timestamp, readOnly: true, placement: details }") + "\n"
                + field("{ name: syncToken, type: string, length: 64, readOnly: true, placement: hidden }"), ""));
        assertEquals("main", fieldOf(model, "carrierNote").getPlacement());
        assertEquals("details", fieldOf(model, "dispatched").getPlacement());
        assertEquals("hidden", fieldOf(model, "syncToken").getPlacement());
    }

    @Test
    void mainOnAnEditableFieldIsAccepted() {
        IntentParser.parse(shipment(field("{ name: note, type: text, placement: main }"), ""));
    }

    @Test
    void detailsOnAnEditableFieldIsRefused() {
        assertIssue(shipment(field("{ name: note, type: text, placement: details }"), ""),
                "entity [Shipment] field [note] placement: details - the Details rail shows values, not inputs");
    }

    @Test
    void hiddenOnARequiredFieldWithoutASourceIsRefused() {
        assertIssue(shipment(field("{ name: syncToken, type: string, length: 64, required: true, placement: hidden }"), ""),
                "a hidden field is never an input, so nothing can supply the required [syncToken]");
        IntentParser.parse(shipment(
                field("{ name: syncToken, type: string, length: 64, required: true, defaultValue: 'none', placement: hidden }"), ""));
    }

    @Test
    void hiddenOnThePrimaryKeyIsRefused() {
        String yaml = shipment("", "").replace("{ name: id,        type: integer, primaryKey: true, generated: true }",
                "{ name: id,        type: integer, primaryKey: true, generated: true, placement: hidden }");
        assertIssue(yaml, "entity [Shipment] field [id] placement: hidden conflicts with primaryKey");
    }

    @Test
    void hiddenConflictsWithMajorListAndRelatedShow() {
        assertIssue(shipment(field("{ name: syncToken, type: string, length: 64, readOnly: true, major: true, placement: hidden }"), ""),
                "placement: hidden conflicts with major: true");
        assertIssue(shipment(
                field("{ name: syncToken, type: string, length: 64, readOnly: true, placement: hidden, visibleWhen: \"reference == X\" }"),
                ""), "placement: hidden conflicts with visibleWhen");
        assertIssue(shipment(field("{ name: syncToken, type: string, length: 64, readOnly: true, placement: hidden }"),
                "    list: [reference, syncToken]\n"), "placement: hidden conflicts with the entity's list:");
        String shown = shipment(field("{ name: syncToken, type: string, length: 64, readOnly: true, placement: hidden }"), "").replace(
                "show: [reference]", "show: [reference, syncToken]");
        assertIssue(shown, "placement: hidden conflicts with the related: register on [Order], whose show: names it");
    }

    @Test
    void anUnknownPlacementIsRefused() {
        assertIssue(shipment(field("{ name: note, type: text, readOnly: true, placement: sidebar }"), ""),
                "entity [Shipment] field [note] placement [sidebar] - placement must be main, details or hidden");
    }

    private static FieldIntent fieldOf(IntentModel model, String name) {
        return model.getEntities()
                    .stream()
                    .filter(e -> "Shipment".equals(e.getName()))
                    .findFirst()
                    .orElseThrow()
                    .getFields()
                    .stream()
                    .filter(f -> name.equals(f.getName()))
                    .findFirst()
                    .orElseThrow();
    }

    private static void assertIssue(String yaml, String fragment) {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains(fragment)),
                "expected an issue containing [" + fragment + "], got: " + ex.getIssues());
    }
}
