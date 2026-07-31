/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.numbering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * A number is {@code prefix + sequence zero-padded to size}, and nothing else - there is no token
 * grammar left to get wrong.
 */
class DocumentNumberServiceTest {

    @Test
    void rendersPrefixPlusSequencePaddedToTheTotalWidth() {
        assertEquals("SI00000042", DocumentNumberService.render("SI", 10, 42));
        assertEquals(10, DocumentNumberService.render("SI", 10, 42)
                                              .length());
        // No prefix: the whole width is sequence - the Bulgarian 10-digit continuous number.
        assertEquals("0000000042", DocumentNumberService.render("", 10, 42));
        // A numeric prefix is just a prefix; the sequence shrinks to keep the total width.
        assertEquals("0000000042", DocumentNumberService.render("00", 10, 42));
        // An annual restart is a prefix change plus a counter reset - no token, no hidden rule.
        assertEquals("2026-000042", DocumentNumberService.render("2026-", 11, 42));
    }

    @Test
    void aNullPrefixIsTreatedAsNone() {
        assertEquals("000042", DocumentNumberService.render(null, 6, 42));
    }

    /**
     * A sequence that outgrows its width renders in FULL rather than truncated: a truncated number is a
     * different number, and silently minting a duplicate is far worse than an over-wide one.
     */
    @Test
    void anOverflowingSequenceIsNeverTruncated() {
        assertEquals("SI1234567890", DocumentNumberService.render("SI", 6, 1234567890L));
    }

    @Test
    void aWidthThatLeavesNoRoomForASequenceIsRejected() {
        DocumentNumberService service = new DocumentNumberService(null);
        assertThrows(IllegalArgumentException.class, () -> service.setShape("Sales Invoice", "", "PRE", 3));
        assertThrows(IllegalArgumentException.class, () -> service.setShape("Sales Invoice", "", "", 0));
    }

    @Test
    void anAbsurdWidthIsRejected() {
        DocumentNumberService service = new DocumentNumberService(null);
        assertThrows(IllegalArgumentException.class, () -> service.setShape("Sales Invoice", "", "SI", DocumentNumberService.MAX_SIZE + 1));
    }
}
