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

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * The enrichment axis (#6929) as the glue sees it: every consumer that can bind {@code onPhase}
 * renders the declared phase as its topic suffix, and every consumer that does not bind one renders
 * exactly what it always did.
 */
class GluePhaseAxisTest {

    private static final String YAML = """
            name: inventory
            entities:
              - name: Account
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string }
              - name: PostingRule
                kind: setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: documentType, type: string }
                relations:
                  - { name: CostOfSalesAccount, kind: manyToOne, to: Account }
                  - { name: InventoryAccount, kind: manyToOne, to: Account }
              - name: StockMovement
                phases: [costed]
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: quantity, type: decimal, precision: 18, scale: 2 }
                  - { name: costValue, type: decimal, precision: 18, scale: 2 }
              - name: JournalEntry
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: entryDate, type: date }
                relations:
                  - { name: StockMovement, kind: manyToOne, to: StockMovement }
              - name: JournalEntryItem
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: debit, type: decimal, precision: 18, scale: 2 }
                  - { name: credit, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: JournalEntry, kind: manyToOne, to: JournalEntry, composition: true, required: true }
                  - { name: Account, kind: manyToOne, to: Account, required: true }
            postings:
              - name: cogsPosting
                event: { onPhase: StockMovement, phase: costed }
                creates: JournalEntry
                backReference: StockMovement
                map: { entryDate: date }
                rule: { entity: PostingRule, match: { documentType: "Goods Issue" } }
                items:
                  - { Account: rule(costOfSalesAccount), debit: "CostValue" }
                  - { Account: rule(inventoryAccount), credit: "CostValue" }
            notifications:
              - name: costedMovement
                event: { onPhase: StockMovement, phase: costed }
                to: "ops@example.com"
                subject: "Movement {id} costed"
                body: "The movement has been costed."
            """;

    @Test
    void aPostingBoundToAPhaseListensOnThePhaseTopic() {
        Map<String, Object> posting = GlueIntentGenerator.buildPostingsForTest(IntentParser.parse(YAML))
                                                         .get(0);

        assertEquals("-costed", posting.get("topicSuffix"), "the posting must observe the ENRICHED row, not the insert");
        assertEquals("reaches the costed phase", posting.get("moment"),
                "the template's own sentence is pre-rendered so it cannot describe a channel other than the one it binds");
        assertEquals(false, posting.get("isCreate"));
    }

    @Test
    void aPostingOnAPhaseNeedsNoStatusGuardBecauseThePhaseIsTheMoment() {
        Map<String, Object> posting = GlueIntentGenerator.buildPostingsForTest(IntentParser.parse(YAML))
                                                         .get(0);

        assertEquals("", posting.get("guardProperty"));
        assertEquals("", posting.get("guardValue"));
    }

    @Test
    void aNotificationBoundToAPhaseListensOnThePhaseTopic() {
        Map<String, Object> notification = GlueIntentGenerator.buildNotificationsForTest(IntentParser.parse(YAML))
                                                              .get(0);

        assertEquals("-costed", notification.get("topicSuffix"));
        assertEquals("StockMovement", notification.get("entity"));
    }

    @Test
    void theLifecycleAxesAreUntouched() {
        String onPhase = "event: { onPhase: StockMovement, phase: costed }";
        // The posting is declared first, so the first occurrence is its binding and the second the
        // notification's - one YAML, both axes back on their pre-#6929 channels.
        String yaml = YAML
                          .replaceFirst(java.util.regex.Pattern.quote(onPhase),
                                  java.util.regex.Matcher.quoteReplacement(
                                          "event: { onTransition: StockMovement, when: \"Quantity == 1\" }"))
                          .replace(onPhase, "event: { onCreate: StockMovement }");

        List<Map<String, Object>> postings = GlueIntentGenerator.buildPostingsForTest(IntentParser.parse(yaml));
        assertEquals("-transitioned", postings.get(0)
                                              .get("topicSuffix"));
        assertEquals("", GlueIntentGenerator.buildNotificationsForTest(IntentParser.parse(yaml))
                                            .get(0)
                                            .get("topicSuffix"),
                "a create still binds the bare entity topic");
    }
}
