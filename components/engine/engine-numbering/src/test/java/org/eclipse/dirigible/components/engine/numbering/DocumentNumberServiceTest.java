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
}
