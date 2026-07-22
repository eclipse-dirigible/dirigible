/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.numbering;

import java.sql.SQLException;
import java.util.Map;

import org.eclipse.dirigible.components.engine.numbering.DocumentNumberService;
import org.eclipse.dirigible.sdk.component.Beans;

/**
 * Client SDK for first-class document numbering: allocate the next gap-free number for a series and
 * render it through the series' format. Backed by the platform's per-tenant counter store (the same
 * store the application shell's Document Numbering settings manage), so hand-written
 * {@code custom/} code and the generated stamping share one engine and one sequence.
 *
 * <p>
 * Example: {@code DocumentNumbers.next("SalesInvoice", "SI-{seq:07}", Map.of("year", "2026"))} →
 * {@code SI-0000001} (then {@code SI-0000002}, …). The scope map both partitions the counter and
 * feeds the format's {@code {year}} / {@code {<Field>}} tokens.
 */
public final class DocumentNumbers {

    private DocumentNumbers() {}

    /**
     * Allocate and format the next number for a series.
     *
     * @param series the series identity (documents sharing a sequence pass the same series)
     * @param format the format template ({@code {seq}} / {@code {seq:0N}} / {@code {series}} / scope
     *        tokens), or {@code null}/blank for the default {@code {series}-{seq:06}}
     * @param scope the resolved scope values partitioning the counter (empty for an unscoped series)
     * @return the formatted document number
     */
    public static String next(String series, String format, Map<String, String> scope) {
        try {
            return Beans.get(DocumentNumberService.class)
                        .next(series, format, scope);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to allocate a document number for series [" + series + "]", e);
        }
    }

    /**
     * Allocate and format the next number for an unscoped series.
     *
     * @param series the series identity
     * @param format the format template (see {@link #next(String, String, Map)})
     * @return the formatted document number
     */
    public static String next(String series, String format) {
        return next(series, format, Map.of());
    }
}
