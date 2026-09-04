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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Keys of a multilingual entity: {@code translatable: false} and the two sites that must not match
 * on a value the platform translates.
 *
 * <p>
 * On a {@code multilingual} entity every character-typed property is translatable, which is right
 * for a label and wrong for a KEY - a code a determination rule matches on, a business key an
 * arrival resolves a relation by. Translating a key breaks the match with no symptom at all: the
 * read overlay hands the UI the translated value, saving the row writes it back into the BASE
 * column, and from then on nothing matches the literal the model was authored with. The posting
 * simply stops firing; the arrival's lookup resolves nothing (dirigible #6545).
 */
class MultilingualKeyTest {

    /** A posting whose determination rule is keyed by a code - the reported shape. */
    private static final String POSTING = """
            name: ledger
            entities:
              - name: PostingRule
                kind: setting
                multilingual: true
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
                  - { name: documentType, type: string }
                  - { name: receivableAccount, type: string }
              - name: OrderStatus
                kind: setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Order
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: net, type: decimal }
                relations:
                  - { name: Status, kind: manyToOne, to: OrderStatus, function: EntityStatus, init: 1 }
              - name: JournalEntry
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: reason, type: string }
                relations:
                  - { name: Order, kind: manyToOne, to: Order }
              - name: JournalEntryItem
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: account, type: string }
                  - { name: debit, type: decimal }
                relations:
                  - { name: JournalEntry, kind: manyToOne, to: JournalEntry, composition: true, required: true }
            seeds:
              - name: orderStatuses
                entity: OrderStatus
                rows:
                  - { id: 1, name: DRAFT }
                  - { id: 2, name: ISSUED }
            postings:
              - name: orderPosting
                event: { onTransition: Order, when: "Status == ISSUED" }
                creates: JournalEntry
                backReference: Order
                map: { reason: "Order {id}" }
                rule: { entity: PostingRule, match: { documentType: "Order" } }
                items:
                  - { account: rule(receivableAccount), debit: "net" }
            """;

    /** An arrival resolving a relation by a business key of a multilingual nomenclature. */
    private static final String ARRIVAL = """
            name: intake
            entities:
              - name: Country
                kind: setting
                multilingual: true
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
                  - { name: code, type: string, unique: true }
              - name: Partner
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
                relations:
                  - { name: Country, kind: manyToOne, to: Country }
            inbound:
              - name: partnerFeed
                path: /partners
                create: Partner
                map:
                  name: partnerName
                  Country: { lookup: Country, by: code, from: countryCode }
            """;

    private static IntentValidationException refused(String yaml) {
        return assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
    }

    private static void assertIssue(IntentValidationException ex, String fragment) {
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(issue -> issue.contains(fragment)),
                "expected an issue containing [" + fragment + "], got: " + ex.getIssues());
    }

    /**
     * The reported case: the rule row's classifier is translated, so the literal the posting was
     * authored with stops matching it the moment a translation is saved. Refused at Generate, with the
     * marker that fixes it named in the message.
     */
    @Test
    void aPostingRuleCannotMatchOnATranslatedColumn() {
        assertIssue(refused(POSTING), "declare `translatable: false`");
    }

    /** Marked as the key it is, the same model generates. */
    @Test
    void markingTheRuleColumnAsAKeyMakesThePostingValid() {
        String yaml = POSTING.replace("{ name: documentType, type: string }", "{ name: documentType, type: string, translatable: false }");
        assertDoesNotThrow(() -> IntentParser.parse(yaml));
    }

    /** The same rule one axis over: a business key an arrival resolves a relation by. */
    @Test
    void anArrivalCannotLookUpByATranslatedBusinessKey() {
        assertIssue(refused(ARRIVAL), "a business key must not be translated");
    }

    @Test
    void markingTheBusinessKeyMakesTheArrivalValid() {
        String yaml = ARRIVAL.replace("{ name: code, type: string, unique: true }",
                "{ name: code, type: string, unique: true, translatable: false }");
        assertDoesNotThrow(() -> IntentParser.parse(yaml));
    }

    /**
     * A translation seed cannot carry a marked field: it has no column in the language table, so the
     * seeded CSV would name a column the schema never emitted and the import would fail at run time for
     * something the model already states.
     */
    @Test
    void aTranslationSeedCannotSetANonTranslatableField() {
        String yaml = """
                name: uoms
                languages: [en, bg]
                entities:
                  - name: UoM
                    kind: setting
                    multilingual: true
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                      - { name: iso, type: string, translatable: false }
                seeds:
                  - name: uoms-bg
                    entity: UoM
                    language: bg
                    rows:
                      - { id: 1, name: "Килограм", iso: "КГ" }
                """;
        assertIssue(refused(yaml), "not the id or a translatable (string/text) field");
    }

    /**
     * And the marker itself must be able to mean something: on an entity that keeps no per-language
     * values there is no language table to be left out of, and on a non-character field there is no
     * column in it either. Both are the authored-but-silently-ignored class, so both are refused.
     */
    @Test
    void theMarkerIsRefusedWhereItCannotMeanAnything() {
        String yaml = """
                name: shop
                entities:
                  - name: Product
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: code, type: string, translatable: false }
                """;
        assertIssue(refused(yaml), "is not multilingual - there is no language table");

        String numeric = """
                name: shop
                entities:
                  - name: Product
                    multilingual: true
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                      - { name: weight, type: decimal, translatable: false }
                """;
        assertIssue(refused(numeric), "only a string/text property is ever translated");
    }
}
