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

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * First-class document numbering runtime: allocates the next value for a series (partitioned by
 * scope) and renders it through the authored {@code format} template. The gap-free per-tenant
 * counter lives in {@link DocumentNumberStore}; this service adds the scope-key derivation and the
 * format grammar ({@code {seq}} / {@code {seq:0N}} zero-pad, {@code {series}}, and scope tokens
 * {@code {<name>}} such as {@code {year}}).
 */
@Component
public class DocumentNumberService {

    /** Default format when the field declares none: the series then a 6-digit sequence. */
    static final String DEFAULT_FORMAT = "{series}-{seq:06}";

    private static final Pattern TOKEN = Pattern.compile("\\{([a-zA-Z][a-zA-Z0-9_]*)(?::0(\\d+))?\\}");

    private final DocumentNumberStore store;

    DocumentNumberService(DocumentNumberStore store) {
        this.store = store;
    }

    /**
     * Allocate and format the next number for a series. The scope map (insertion-ordered
     * {@code name -> value}) both partitions the counter and feeds the format's scope tokens.
     *
     * @param series the series identity (documents sharing a sequence pass the same series)
     * @param format the format template, or {@code null}/blank for {@link #DEFAULT_FORMAT}
     * @param scope the resolved scope values (e.g. {@code {Company=1, year=2026}}); empty for unscoped
     * @return the formatted document number
     * @throws SQLException if the allocation fails
     */
    public String next(String series, String format, Map<String, String> scope) throws SQLException {
        Map<String, String> safeScope = scope == null ? Map.of() : scope;
        long seq = store.allocate(series, scopeKey(safeScope));
        return render(format == null || format.isBlank() ? DEFAULT_FORMAT : format, series, seq, safeScope);
    }

    /** All counter rows of the current tenant (for the management surface). */
    public List<DocumentNumberStore.Counter> list() throws SQLException {
        return store.list();
    }

    /**
     * Set the <b>next</b> value a (series, scope) counter will allocate (e.g. start invoices at 1000).
     *
     * @param series the series identity
     * @param scope the scope key ({@code ""} for unscoped)
     * @param next the next value to allocate (stored as {@code next - 1})
     * @throws SQLException if the write fails
     */
    public void setNext(String series, String scope, long next) throws SQLException {
        store.setCounter(series, scope, Math.max(0, next - 1));
    }

    /** The counter partition key: the scope values joined by {@code |}; {@code ""} when unscoped. */
    static String scopeKey(Map<String, String> scope) {
        return String.join("|", scope.values());
    }

    /**
     * Render a format template. {@code {seq}} / {@code {seq:0N}} expand the sequence (zero-padded to
     * N); {@code {series}} the series; any other {@code {name}} the scope value for that name (empty
     * when absent).
     */
    static String render(String format, String series, long seq, Map<String, String> scope) {
        Map<String, String> tokens = new LinkedHashMap<>(scope);
        tokens.put("series", series);
        Matcher matcher = TOKEN.matcher(format);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String name = matcher.group(1);
            String pad = matcher.group(2);
            String value;
            if ("seq".equals(name)) {
                value = pad == null ? Long.toString(seq) : String.format("%0" + pad + "d", seq);
            } else {
                value = tokens.getOrDefault(name, "");
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(out);
        return out.toString();
    }
}
