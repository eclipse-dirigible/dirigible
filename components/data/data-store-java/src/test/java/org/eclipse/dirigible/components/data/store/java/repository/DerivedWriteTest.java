/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.store.java.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.eclipse.dirigible.sdk.utils.Json;
import org.junit.jupiter.api.Test;

/**
 * The derived-write marker (#7557): a roll-up's {@code "-updated"} names the columns it wrote, so a
 * listener reacting to authored changes can skip it - and nothing else is marked.
 */
class DerivedWriteTest {

    /** A row as a generated entity serializes it. */
    public static class Payment {
        public Integer Id = 88;
        public java.math.BigDecimal Amount = new java.math.BigDecimal("48");
        public java.math.BigDecimal Allocated = java.math.BigDecimal.ZERO;
    }

    private static final String ROW = Json.stringify(new Payment());

    @Test
    void outsideADerivedWriteThePayloadIsUntouched() {
        assertEquals(ROW, DerivedWrite.mark(ROW));
        assertNull(DerivedWrite.columns(ROW), "an authored write carries no marker");
        assertFalse(DerivedWrite.touchedNoneOf(ROW, "Amount"), "an authored write is never skipped");
    }

    @Test
    void aDerivedWriteNamesTheColumnsItWrote() {
        String marked = DerivedWrite.run(List.of("Allocated"), () -> DerivedWrite.mark(ROW));

        assertEquals(Set.of("Allocated"), DerivedWrite.columns(marked));
        assertTrue(DerivedWrite.touchedNoneOf(marked, "Amount", "Customer"),
                "the allocated roll-up moved neither the pot nor a match column");
        assertFalse(DerivedWrite.touchedNoneOf(marked, "Allocated"));
        Payment parsed = Json.parse(marked, Payment.class);
        assertEquals(88, parsed.Id, "the marked payload still deserializes into the entity");
    }

    @Test
    void aDerivedPotIsNotSkipped() {
        // A pot that is itself a roll-up (the payment's amount summed from its lines) moves through a
        // derived write, and that IS a reason to re-settle.
        String marked = DerivedWrite.run(List.of("Amount"), () -> DerivedWrite.mark(ROW));

        assertFalse(DerivedWrite.touchedNoneOf(marked, "Amount", "Customer"));
    }

    @Test
    void onlyTheFirstEventOfTheDerivedWriteIsMarked() {
        // A write the derived one causes in turn (a line resumming its master) is about another row.
        List<String> events = DerivedWrite.run(List.of("Allocated"), () -> List.of(DerivedWrite.mark(ROW), DerivedWrite.mark(ROW)));

        assertEquals(Set.of("Allocated"), DerivedWrite.columns(events.get(0)));
        assertNull(DerivedWrite.columns(events.get(1)));
    }

    @Test
    void theScopeEndsWithTheCall() {
        DerivedWrite.run(List.of("Allocated"), () -> "no event");

        assertNull(DerivedWrite.columns(DerivedWrite.mark(ROW)));
    }

    @Test
    void theScopeEndsWhenTheWriteFails() {
        try {
            DerivedWrite.run(List.of("Allocated"), () -> {
                throw new IllegalStateException("refused");
            });
        } catch (IllegalStateException expected) {
            // the write's own failure
        }

        assertNull(DerivedWrite.columns(DerivedWrite.mark(ROW)));
    }

    @Test
    void anUnparseablePayloadIsNotDerived() {
        assertNull(DerivedWrite.columns("not json"));
        assertNull(DerivedWrite.columns(null));
    }
}
