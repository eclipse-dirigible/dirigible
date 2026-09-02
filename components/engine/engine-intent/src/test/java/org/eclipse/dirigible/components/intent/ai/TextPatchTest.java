/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * The apply half of the patch-shaped proposal contract (dirigible #6958): a patch either splices
 * cleanly - leaving everything it did not anchor byte-identical - or is refused whole, with a
 * reason the model can act on.
 */
class TextPatchTest {

    /**
     * Comments, blank lines, alignment and key order - everything a parse-serialize pass would lose.
     */
    private static final String DOCUMENT = """
            name: lib          # the library application
            entities:

              - name: Member
                audit: true
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }

              - name: Loan
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
            """;

    @Test
    void anAnchoredInsertLeavesEverythingElseByteIdentical() {
        JsonArray edits = edits(edit("insertAfter", "      - { name: name, type: string }", "\n      - { name: notes, type: text }"));

        TextPatch.Result result = TextPatch.apply(DOCUMENT, edits);

        assertEquals(List.of(), result.issues());
        assertEquals(DOCUMENT.replace("      - { name: name, type: string }",
                "      - { name: name, type: string }\n      - { name: notes, type: text }"), result.document());
        assertTrue(result.document()
                         .contains("name: lib          # the library application"),
                "the comment and its alignment survive");
        assertTrue(result.document()
                         .contains("entities:\n\n  - name: Member"),
                "the blank line survives");
    }

    @Test
    void severalEditsApplyAgainstTheOriginalOffsets() {
        // Both anchors are located in the document as given - an earlier edit's insertion must not
        // shift a later edit's anchor, which is the classic way a multi-edit patch corrupts a file.
        JsonArray edits = edits(edit("replace", "  - name: Loan", "  - name: Loan\n    audit: true"),
                edit("insertAfter", "      - { name: name, type: string }", "\n      - { name: notes, type: text }"));

        TextPatch.Result result = TextPatch.apply(DOCUMENT, edits);

        assertEquals(List.of(), result.issues());
        assertTrue(result.document()
                         .contains("  - name: Loan\n    audit: true\n"));
        assertTrue(result.document()
                         .contains("      - { name: name, type: string }\n      - { name: notes, type: text }\n"));
    }

    @Test
    void aDeleteRemovesExactlyTheAnchor() {
        TextPatch.Result result = TextPatch.apply(DOCUMENT, edits(edit("delete", "    audit: true\n", null)));

        assertEquals(DOCUMENT.replace("    audit: true\n", ""), result.document());
    }

    @Test
    void anAnchorThatIsNotFoundRefusesTheWholePatch() {
        JsonArray edits = edits(edit("insertAfter", "      - { name: name, type: string }", "\n      - { name: notes, type: text }"),
                edit("replace", "  - name: Reservation", "  - name: Reservation\n    audit: true"));

        TextPatch.Result result = TextPatch.apply(DOCUMENT, edits);

        assertNull(result.document(), "nothing is applied when one edit cannot be");
        assertEquals(1, result.issues()
                              .size());
        assertTrue(result.issues()
                         .get(0)
                         .contains("edit #2 (replace)"));
        assertTrue(result.issues()
                         .get(0)
                         .contains("not found"));
        assertTrue(result.issues()
                         .get(0)
                         .contains("- name: Reservation"),
                "the issue names the anchor that missed");
    }

    @Test
    void anAmbiguousAnchorIsRefusedRatherThanTakingTheFirstMatch() {
        TextPatch.Result result = TextPatch.apply(DOCUMENT, edits(edit("replace", "    fields:", "    fields: # ?")));

        assertNull(result.document());
        assertTrue(result.issues()
                         .get(0)
                         .contains("not unique"));
    }

    @Test
    void overlappingEditsAreRefused() {
        JsonArray edits =
                edits(edit("replace", "  - name: Member\n    audit: true", "  - name: Member"), edit("delete", "    audit: true\n", null));

        TextPatch.Result result = TextPatch.apply(DOCUMENT, edits);

        assertNull(result.document());
        assertTrue(result.issues()
                         .get(0)
                         .contains("overlap"));
    }

    @Test
    void anUnknownOpAndAnEmptyAnchorAreBothReported() {
        JsonArray edits = edits(edit("rewrite", "  - name: Loan", "x"), edit("replace", "", "y"));

        TextPatch.Result result = TextPatch.apply(DOCUMENT, edits);

        assertNull(result.document());
        assertEquals(2, result.issues()
                              .size(),
                "every unusable edit is reported, not just the first");
        assertTrue(result.issues()
                         .get(0)
                         .contains("unknown op [rewrite]"));
        assertTrue(result.issues()
                         .get(1)
                         .contains("no anchor"));
    }

    @Test
    void anEmptyPatchAndAnEmptyDocumentAreBothRefused() {
        assertTrue(TextPatch.apply(DOCUMENT, new JsonArray())
                            .issues()
                            .get(0)
                            .contains("no edits"));
        assertTrue(TextPatch.apply("", edits(edit("replace", "a", "b")))
                            .issues()
                            .get(0)
                            .contains("no current document"),
                "a document that does not exist yet must be proposed whole, not patched");
    }

    @Test
    void aCrlfDocumentIsAnchoredByALfAnchorAndStaysCrlf() {
        // The document travels through a browser buffer that may carry CRLF; a model reproducing a
        // line writes LF. An anchor that can never match is a feature that never applies.
        String crlf = DOCUMENT.replace("\n", "\r\n");

        TextPatch.Result result = TextPatch.apply(crlf,
                edits(edit("insertAfter", "      - { name: name, type: string }", "\n      - { name: notes, type: text }")));

        assertNotNull(result.document(), () -> "refused: " + result.issues());
        assertEquals(crlf.replace("      - { name: name, type: string }\r\n",
                "      - { name: name, type: string }\r\n      - { name: notes, type: text }\r\n"), result.document());
    }

    @Test
    void theSchemaNamesTheDocumentAndTheOperations() {
        Map<String, Object> schema = TextPatch.editsSchema("app.intent");

        assertEquals("array", schema.get("type"));
        assertTrue(String.valueOf(schema.get("description"))
                         .contains("app.intent"));
        @SuppressWarnings("unchecked")
        Map<String, Object> items = (Map<String, Object>) schema.get("items");
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) items.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> op = (Map<String, Object>) properties.get("op");
        assertEquals(List.of("replace", "insertBefore", "insertAfter", "delete"), op.get("enum"));
        assertEquals(List.of("op", "anchor"), items.get("required"));
    }

    private static JsonArray edits(JsonObject... entries) {
        JsonArray array = new JsonArray();
        for (JsonObject entry : entries) {
            array.add(entry);
        }
        return array;
    }

    private static JsonObject edit(String op, String anchor, String content) {
        JsonObject edit = new JsonObject();
        edit.addProperty("op", op);
        edit.addProperty("anchor", anchor);
        if (content != null) {
            edit.addProperty("content", content);
        }
        return edit;
    }
}
