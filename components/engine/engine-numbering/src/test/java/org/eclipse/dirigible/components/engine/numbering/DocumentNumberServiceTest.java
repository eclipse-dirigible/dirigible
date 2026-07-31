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

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DocumentNumberServiceTest {

    @Test
    void rendersSeqPaddingSeriesAndScopeTokens() {
        Map<String, String> scope = new LinkedHashMap<>();
        scope.put("Company", "1");
        scope.put("year", "2026");

        assertEquals("SI-0000042", DocumentNumberService.render("SI-{seq:07}", "SalesInvoice", 42, scope));
        assertEquals("SalesInvoice/2026/42", DocumentNumberService.render("{series}/{year}/{seq}", "SalesInvoice", 42, scope));
        assertEquals("INV-1-2026-000042", DocumentNumberService.render("INV-{Company}-{year}-{seq:06}", "SalesInvoice", 42, scope));
        // An unknown token renders empty; the default format uses the series.
        assertEquals("SalesInvoice-000001",
                DocumentNumberService.render(DocumentNumberService.DEFAULT_FORMAT, "SalesInvoice", 1, Map.of()));
    }

    @Test
    void scopeKeyJoinsValuesAndIsEmptyWhenUnscoped() {
        Map<String, String> scope = new LinkedHashMap<>();
        scope.put("Company", "1");
        scope.put("year", "2026");
        assertEquals("1|2026", DocumentNumberService.scopeKey(scope));
        assertEquals("", DocumentNumberService.scopeKey(Map.of()));
    }

    /**
     * The tenant's prefix/size override REPLACES the authored format - it can only express a prefix
     * plus a padded sequence, so merging it into a richer template would be guesswork.
     */
    @Test
    void overrideReplacesTheAuthoredFormatWithPrefixAndWidth() {
        // The BG case: drop the "SI" prefix entirely, total width 10.
        assertEquals("{seq:010}", DocumentNumberService.effectiveFormat("SI{seq:08}", "", 10));
        assertEquals("0000000042", DocumentNumberService.render(DocumentNumberService.effectiveFormat("SI{seq:08}", "", 10),
                "Sales Invoice", 42, java.util.Map.of()));
        // A numeric prefix, same total width - the sequence shrinks to fit.
        assertEquals("00{seq:08}", DocumentNumberService.effectiveFormat("SI{seq:08}", "00", 10));
        assertEquals("0000000042", DocumentNumberService.render(DocumentNumberService.effectiveFormat("SI{seq:08}", "00", 10),
                "Sales Invoice", 42, java.util.Map.of()));
        assertEquals(10,
                DocumentNumberService.render(DocumentNumberService.effectiveFormat("SI{seq:08}", "00", 10), "Sales Invoice", 42,
                        java.util.Map.of())
                                     .length());
    }

    @Test
    void noOverrideKeepsTheAuthoredFormat() {
        assertEquals("SI{seq:08}", DocumentNumberService.effectiveFormat("SI{seq:08}", null, null));
        // A prefix without a width is not an override - a width is what makes it renderable.
        assertEquals("SI{seq:08}", DocumentNumberService.effectiveFormat("SI{seq:08}", "X", null));
    }

    @Test
    void anUnusableWidthFallsBackInsteadOfMangling() {
        // The width leaves no room for even one digit: keep the authored format rather than emit "PRE".
        assertEquals("PRE{seq:04}", DocumentNumberService.effectiveFormat("PRE{seq:04}", "PRE", 3));
    }

    /**
     * A format carrying scope tokens cannot be expressed as prefix + width, so the management surface
     * must not offer the override for it - it would silently drop the tokens.
     */
    @Test
    void onlySequenceOnlyFormatsAreOverridable() {
        assertTrue(DocumentNumberService.overridable("SI{seq:08}"));
        assertTrue(DocumentNumberService.overridable("{seq}"));
        assertTrue(DocumentNumberService.overridable("PLAIN"));
        assertTrue(!DocumentNumberService.overridable("INV-{year}-{seq:05}"));
        assertTrue(!DocumentNumberService.overridable("{series}-{seq:06}"));
    }
}
