/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator.edm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * A posting's back-reference is the system's to write (issue #7825): the posting finds "its" post
 * by it and rewrites a DRAFT one in place, so a user who could set it on a hand-written entry
 * handed that entry - and its lines - to the posting. The relation therefore carries the readOnly
 * flag without the author writing it, which drops it from a user create and keeps the stored value
 * on a user update, while the posting's own {@code save()} still writes it.
 */
class PostingBackReferenceIntentTest {

    private static final String MODEL = """
            name: ledger
            entities:
              - name: DocStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, length: 100 }
              - name: Doc
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Status, kind: manyToOne, to: DocStatus, function: EntityStatus, init: 1 }
              - name: Entry
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                relations:
                  - { name: Status, kind: manyToOne, to: DocStatus, function: EntityStatus, init: 1 }
                  - { name: Doc,    kind: manyToOne, to: Doc%s }
                  - { name: Other,  kind: manyToOne, to: Doc }
              - name: EntryLine
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: debit, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Entry, kind: manyToOne, to: Entry, composition: true, required: true }
            postings:
              - name: docPosting
                event: { onTransition: Doc, when: "Status == 2" }
                creates: Entry
                backReference: Doc
                items:
                  - { debit: "Amount" }
            """;

    @Test
    void thePostingsBackReferenceIsReadOnlyWithoutTheAuthorDeclaringIt() {
        Map<String, Object> doc = entryProperty("", "Doc");
        assertEquals("true", doc.get("isReadOnlyProperty"));
        assertEquals("DROPDOWN", doc.get("widgetType"));
        assertEquals("Doc", doc.get("relationshipEntityName"));
    }

    @Test
    void aRelationNoPostingReadsStaysUserWritable() {
        assertFalse(entryProperty("", "Other").containsKey("isReadOnlyProperty"));
    }

    /**
     * A required back-reference is left as written: readOnly beside NOT NULL would refuse every user
     * create of the target (#8751), so the author has to choose - drop required, or declare the target
     * systemWritten.
     */
    @Test
    void aRequiredBackReferenceIsLeftAsWritten() {
        assertFalse(entryProperty(", required: true", "Doc").containsKey("isReadOnlyProperty"));
    }

    @Test
    void thePostsIdempotencyBackReferenceIsReadOnlyToo() {
        String yaml = """
                name: stock
                entities:
                  - name: Status
                    kind: setting
                    fields:
                      - { name: id,   type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string, length: 100 }
                  - name: GoodsIssue
                    fields:
                      - { name: id,       type: integer, primaryKey: true, generated: true }
                      - { name: quantity, type: decimal }
                    relations:
                      - { name: Status, kind: manyToOne, to: Status, function: EntityStatus, init: 1 }
                  - name: StockMovement
                    fields:
                      - { name: id,       type: integer, primaryKey: true, generated: true }
                      - { name: quantity, type: decimal }
                    relations:
                      - { name: GoodsIssue, kind: manyToOne, to: GoodsIssue }
                posts:
                  - name: goodsIssueLedger
                    forEntity: GoodsIssue
                    event: 2
                    into: StockMovement
                    idempotentBy: GoodsIssue
                    set:
                      quantity: source.quantity
                """;
        Map<String, Object> back = propertyByName(
                entityNamed(EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "stock"), "StockMovement"), "GoodsIssue");
        assertEquals("true", back.get("isReadOnlyProperty"));
    }

    private static Map<String, Object> entryProperty(String backReferenceKeys, String name) {
        Map<String, Object> modelJson =
                EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(MODEL.formatted(backReferenceKeys)), "ledger");
        return propertyByName(entityNamed(modelJson, "Entry"), name);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> entityNamed(Map<String, Object> modelJson, String name) {
        List<Map<String, Object>> entities = (List<Map<String, Object>>) ((Map<String, Object>) modelJson.get("model")).get("entities");
        return entities.stream()
                       .filter(e -> name.equals(e.get("name")))
                       .findFirst()
                       .orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> propertyByName(Map<String, Object> entity, String name) {
        List<Map<String, Object>> properties = (List<Map<String, Object>>) entity.get("properties");
        Map<String, Object> property = properties.stream()
                                                 .filter(p -> name.equals(p.get("name")))
                                                 .findFirst()
                                                 .orElse(null);
        assertFalse(property == null, "property [" + name + "] not found");
        return property;
    }
}
