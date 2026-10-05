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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Validation of a reversal bound to the source's delete (dirigible #7634): {@code onDelete} binds
 * only a reversal, of the sibling's own source, and the original's back-reference must survive the
 * delete ({@code whenTargetDeleted: keep}) for the reversal to find it.
 */
class PostingsFollowSourceIntentTest {

    private static final String YAML = """
            name: ledger
            uses:
              - { model: pay }
            entities:
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
                relations:
                  - { name: Entry, kind: manyToOne, to: Entry, composition: true, required: true }
            postings:
              - name: paymentPosting
                event: { onCreate: Payment, model: pay }
                creates: Entry
                backReference: Payment
                items:
                  - { debit: "Amount" }
              - name: paymentStorno
                event: { onDelete: Payment, model: pay }
                reverses: paymentPosting
                storno: Storno
            """;

    @Test
    void aDeleteReversalOverAKeptBackReferenceParses() {
        assertDoesNotThrow(() -> IntentParser.parse(YAML));
    }

    /** A deleted source has nothing to post - only an entry posted for it to red-storno. */
    @Test
    void onDeleteWithoutReversesIsRejected() {
        assertIssue(
                YAML.replace("    reverses: paymentPosting\n    storno: Storno\n",
                        "    creates: Entry\n    backReference: Payment\n    items:\n      - { debit: \"Amount\" }\n"),
                "binds onDelete, which only a reversal (`reverses:`) may");
    }

    /**
     * The default restrict refuses the payment's delete while the entry references it, so the reversal
     * would never run - said at parse rather than discovered as a 409.
     */
    @Test
    void aRestrictedBackReferenceIsRejected() {
        assertIssue(YAML.replace(", whenTargetDeleted: keep", ""), "must be a cross-model relation declaring `whenTargetDeleted: keep`");
    }

    /** nullify clears the very link the reversal follows to the original. */
    @Test
    void aNullifiedBackReferenceIsRejected() {
        assertIssue(YAML.replace("whenTargetDeleted: keep", "whenTargetDeleted: nullify"),
                "must be a cross-model relation declaring `whenTargetDeleted: keep`");
    }

    /** Only the delete of the sibling's own source leaves one of its entries to reverse. */
    @Test
    void aDeleteOfAnotherEntityIsRejected() {
        assertIssue(YAML.replace("event: { onDelete: Payment, model: pay }", "event: { onDelete: Refund, model: pay }"),
                "onDelete [Refund] must name the source of [paymentPosting] (Payment)");
    }

    private static void assertIssue(String yaml, String expected) {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getMessage()
                     .contains(expected),
                "expected issue containing [" + expected + "] but got: " + ex.getMessage());
    }
}
