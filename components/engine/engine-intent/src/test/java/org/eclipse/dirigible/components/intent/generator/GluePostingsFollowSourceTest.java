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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * The glue of a posting that follows its source (dirigible #7634): an {@code onCreate} posting gets
 * a twin on the source's {@code -updated} topic that only ever rewrites an existing post, and a
 * reversal bound to {@code onDelete} reads the deleted row off its payload and mirrors the
 * original's stored lines, every amount negated.
 */
class GluePostingsFollowSourceTest {

    private static final String YAML = """
            name: ledger
            uses:
              - { model: pay }
            entities:
              - name: Account
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
              - name: PostingRule
                kind: setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: documentType, type: string }
                relations:
                  - { name: CashAccount, kind: manyToOne, to: Account }
              - name: Entry
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                relations:
                  - { name: Payment, kind: manyToOne, to: Payment, model: pay, whenTargetDeleted: keep }
                  - { name: Storno, kind: manyToOne, to: Entry }
              - name: EntryLine
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: debit, type: decimal, precision: 18, scale: 2 }
                  - { name: credit, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Entry, kind: manyToOne, to: Entry, composition: true, required: true }
                  - { name: Account, kind: manyToOne, to: Account }
            postings:
              - name: paymentPosting
                event: { onCreate: Payment, model: pay }
                creates: Entry
                backReference: Payment
                rule: { entity: PostingRule, match: { documentType: "Payment" } }
                items:
                  - { Account: rule(cashAccount), debit: "Amount" }
                  - { credit: "Amount" }
              - name: paymentStorno
                event: { onDelete: Payment, model: pay }
                reverses: paymentPosting
                storno: Storno
            """;

    @Test
    void anOnCreatePostingGetsAFollowerOnTheSourcesEdit() {
        List<Map<String, Object>> postings = GlueIntentGenerator.buildPostingsForTest(IntentParser.parse(YAML));
        assertEquals(List.of("PaymentPosting", "PaymentPostingOnUpdate", "PaymentStorno"), postings.stream()
                                                                                                   .map(p -> p.get("className"))
                                                                                                   .toList());
        Map<String, Object> create = postings.get(0);
        Map<String, Object> follower = postings.get(1);
        assertEquals("", create.get("topicSuffix"));
        assertNull(create.get("followsSource"), "the create handler writes the first post");
        assertEquals("-updated", follower.get("topicSuffix"));
        assertEquals(Boolean.TRUE, follower.get("followsSource"));
        assertEquals("is edited", follower.get("moment"));
        // Everything else is the create handler's: the same derivation, the same rewrite bound, the
        // same filter of the reversal's rows out of its idempotency set.
        assertEquals(create.get("itemRows"), follower.get("itemRows"));
        assertEquals(create.get("amendableGuard"), follower.get("amendableGuard"));
        assertEquals("Storno", follower.get("stornoFilterProperty"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void aDeleteReversalMirrorsTheOriginalsStoredLines() {
        Map<String, Object> storno = GlueIntentGenerator.buildPostingsForTest(IntentParser.parse(YAML))
                                                        .get(2);
        assertEquals("-deleted", storno.get("topicSuffix"));
        assertEquals(Boolean.TRUE, storno.get("fromPayload"));
        assertEquals(Boolean.TRUE, storno.get("mirrorsOriginal"));
        assertNull(storno.get("followsSource"), "a reversal has no follower");
        List<Map<String, Object>> rows = (List<Map<String, Object>>) storno.get("mirrorRows");
        assertEquals(1, rows.size());
        List<Map<String, Object>> assigns = (List<Map<String, Object>>) rows.get(0)
                                                                            .get("assigns");
        // Each assigned cell once, off the stored line: the account copied, every amount negated.
        assertEquals(List.of("Account", "Debit", "Credit"), assigns.stream()
                                                                   .map(a -> a.get("targetProp"))
                                                                   .toList());
        assertEquals("originalItem.Account", GlueRendering.expr(assigns.get(0)));
        assertEquals("originalItem.Debit == null ? null : originalItem.Debit.negate()", GlueRendering.expr(assigns.get(1)));
        assertEquals("originalItem.Credit == null ? null : originalItem.Credit.negate()", GlueRendering.expr(assigns.get(2)));
    }

    @Test
    void aTransitionPostingHasNoFollower() {
        String yaml = YAML
                          .replace("event: { onCreate: Payment, model: pay }",
                                  "event: { onTransition: Payment, model: pay, when: \"Status == 2\" }")
                          .replace("event: { onDelete: Payment, model: pay }",
                                  "event: { onTransition: Payment, model: pay, when: \"Status == 3\" }");
        List<Map<String, Object>> postings = GlueIntentGenerator.buildPostingsForTest(IntentParser.parse(yaml));
        assertEquals(2, postings.size());
        assertTrue(postings.stream()
                           .noneMatch(p -> Boolean.TRUE.equals(p.get("followsSource")) || Boolean.TRUE.equals(p.get("mirrorsOriginal"))));
        assertNotEquals(Boolean.TRUE, postings.get(1)
                                              .get("fromPayload"));
    }
}
