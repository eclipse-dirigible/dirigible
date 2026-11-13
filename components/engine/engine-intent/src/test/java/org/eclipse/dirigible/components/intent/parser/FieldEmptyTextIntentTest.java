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
 * {@code emptyText:} (dirigible #8605): the text every generated surface prints in place of an
 * empty value. Accepted on a field that can be empty and is shown as a value; refused where it
 * could never show or would show in place of something else.
 */
class FieldEmptyTextIntentTest {

    private static String member(String fields) {
        return """
                name: lending
                entities:
                  - name: Member
                    fields:
                      - { name: id,   type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string, length: 80, required: true }
                %s
                """.formatted(fields);
    }

    private static String field(String declaration) {
        return "      - " + declaration;
    }

    @Test
    void aFieldCarriesItsEmptyText() {
        IntentModel model = IntentParser.parse(member(String.join("\n", //
                field("{ name: maxLoans,  type: integer, readOnly: true, emptyText: No limit }"), //
                field("{ name: expiresOn, type: date, emptyText: Never }"), //
                field("{ name: nickname,  type: string, length: 40, emptyText: Not set }"), //
                field("{ name: notes,     type: text, emptyText: '  Nothing yet  ' }"), //
                field("{ name: lastSeen,  type: timestamp, readOnly: true, emptyText: Not yet }"))));
        assertEquals("No limit", fieldOf(model, "maxLoans").getEmptyText());
        assertEquals("Never", fieldOf(model, "expiresOn").getEmptyText());
        assertEquals("Not set", fieldOf(model, "nickname").getEmptyText());
        assertEquals("  Nothing yet  ", fieldOf(model, "notes").getEmptyText(), "the parser keeps it as authored; the generator trims");
        assertEquals("Not yet", fieldOf(model, "lastSeen").getEmptyText());
    }

    @Test
    void anEditableAndAReadOnlyFieldBothAcceptIt() {
        IntentParser.parse(member(field("{ name: limit, type: decimal, emptyText: Unlimited }") + "\n"
                + field("{ name: stamp, type: month, readOnly: true, emptyText: Open }")));
    }

    @Test
    void aBlankEmptyTextIsRefused() {
        assertIssue(member(field("{ name: nickname, type: string, emptyText: '   ' }")),
                "entity [Member] field [nickname] emptyText is blank - remove it to keep the em dash");
    }

    @Test
    void emptyTextOnABooleanIsRefused() {
        assertIssue(member(field("{ name: active, type: boolean, emptyText: Unknown }")),
                "entity [Member] field [active] emptyText is not allowed on a boolean");
    }

    @Test
    void emptyTextOnAUuidIsRefused() {
        assertIssue(member(field("{ name: ref, type: uuid, emptyText: None }")),
                "entity [Member] field [ref] emptyText is not allowed on a uuid");
    }

    @Test
    void emptyTextOnThePrimaryKeyIsRefused() {
        String yaml = member("").replace("{ name: id,   type: integer, primaryKey: true, generated: true }",
                "{ name: id,   type: integer, primaryKey: true, generated: true, emptyText: New }");
        assertIssue(yaml, "entity [Member] field [id] emptyText is not allowed on the primary key");
    }

    @Test
    void emptyTextOnARequiredFieldIsRefused() {
        String yaml = member("").replace("{ name: name, type: string, length: 80, required: true }",
                "{ name: name, type: string, length: 80, required: true, emptyText: Anonymous }");
        assertIssue(yaml, "entity [Member] field [name] emptyText is not allowed on a required field");
    }

    @Test
    void emptyTextOnADocumentNumberIsRefused() {
        assertIssue(member(field("{ name: number, type: string, length: 20, number: { series: Member Card }, emptyText: Pending }")),
                "entity [Member] field [number] emptyText is not allowed on a document number");
    }

    @Test
    void emptyTextWithPlacementHiddenIsRefused() {
        assertIssue(member(field("{ name: token, type: string, length: 64, readOnly: true, placement: hidden, emptyText: None }")),
                "entity [Member] field [token] emptyText is not allowed with placement: hidden");
    }

    @Test
    void emptyTextOnAnAggregateIsRefused() {
        assertIssue(member(field("{ name: total, type: decimal, aggregate: true, emptyText: Nothing }")),
                "entity [Member] field [total] emptyText is not allowed on an aggregate");
    }

    @Test
    void emptyTextOnADocumentTitleOrMessageBodyIsRefused() {
        assertIssue(member(field("{ name: title, type: string, length: 40, documentTitle: true, emptyText: Untitled }")),
                "entity [Member] field [title] emptyText is not allowed on the document title");
        assertIssue(member(field("{ name: title, type: string, length: 40, function: DocumentTitle, emptyText: Untitled }")),
                "entity [Member] field [title] emptyText is not allowed on the document title");
        assertIssue(member(field("{ name: body, type: text, messageBody: true, emptyText: Empty }")),
                "entity [Member] field [body] emptyText is not allowed on a chat message body");
    }

    @Test
    void emptyTextOnARelationIsAnUnknownKey() {
        String yaml = member("") + """
                    relations:
                      - { name: branch, kind: manyToOne, to: Branch, emptyText: Unassigned }
                  - name: Branch
                    fields:
                      - { name: id,   type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string, length: 40 }
                """;
        assertIssue(yaml, "emptyText");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("emptyText") && i.toLowerCase()
                                                                .contains("unknown")),
                "expected the relation's emptyText reported as an unknown key, got: " + ex.getIssues());
    }

    private static FieldIntent fieldOf(IntentModel model, String name) {
        return model.getEntities()
                    .stream()
                    .filter(e -> "Member".equals(e.getName()))
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
