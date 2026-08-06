/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/** Parsing the conditional {@code rule(by:..., cases:..., default:...)} posting item-cell form. */
class PostingRuleSelectorTest {

    @Test
    void parsesByCasesAndDefaultInAuthoringOrder() {
        Optional<PostingRuleSelector> parsed =
                PostingRuleSelector.parse("rule(by: PaymentMethod, cases: { 1: BankAccount, 2: CashAccount }, default: SuspenseAccount)");
        assertTrue(parsed.isPresent());
        PostingRuleSelector selector = parsed.get();
        assertEquals("PaymentMethod", selector.by());
        assertEquals("SuspenseAccount", selector.defaultColumn());
        assertEquals(List.of("1", "2"), List.copyOf(selector.cases()
                                                            .keySet()));
        assertEquals("BankAccount", selector.cases()
                                            .get("1"));
        assertEquals("CashAccount", selector.cases()
                                            .get("2"));
    }

    @Test
    void defaultIsOptional() {
        PostingRuleSelector selector = PostingRuleSelector.parse("rule(by: Method, cases: { 1: BankAccount })")
                                                          .orElseThrow();
        assertEquals(null, selector.defaultColumn());
        assertEquals(1, selector.cases()
                                .size());
    }

    @Test
    void plainRuleAndExpressionsAreNotTheConditionalForm() {
        assertFalse(PostingRuleSelector.parse("rule(receivableAccount)")
                                       .isPresent());
        assertFalse(PostingRuleSelector.parse("Net + Vat")
                                       .isPresent());
        assertFalse(PostingRuleSelector.parse(null)
                                       .isPresent());
    }

    /**
     * ReDoS guard (CodeQL): the cases-body match must be linear. The witness is a crafted prefix with a
     * long run of spaces and no closing brace - the old {@code \s*([^}]*?)\s*\}} backtracked
     * catastrophically on it; the current single greedy {@code [^}]*} returns effectively instantly.
     */
    @Test
    void doesNotBacktrackCatastrophicallyOnAMaliciousCasesBody() {
        String malicious = "rule(by:0,cases:{{" + " ".repeat(50_000);
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> assertFalse(PostingRuleSelector.parse(malicious)
                                                                                              .isPresent()));
    }
}
