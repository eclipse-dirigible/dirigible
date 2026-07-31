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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
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

    /** Widest renderable number - the stored column is VARCHAR(100), and no series needs more. */
    static final int MAX_SIZE = 40;

    private static final Pattern TOKEN = Pattern.compile("\\{([a-zA-Z][a-zA-Z0-9_]*)(?::0(\\d+))?\\}");

    /**
     * The series the running application declared, series -> authored format. Application-wide (it
     * comes from the generated code, which is the same for every tenant) and rebuilt on every boot, so
     * it is held in memory rather than written per tenant at start-up - where there is no tenant scope
     * to write into. Counters and format overrides stay per-tenant in the store.
     */
    private final Map<String, String> declared = new ConcurrentSkipListMap<>();

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
        DocumentNumberStore.Allocation allocation = store.allocate(series, scopeKey(safeScope));
        String authored = format == null || format.isBlank() ? DEFAULT_FORMAT : format;
        return render(effectiveFormat(authored, allocation.prefix(), allocation.size()), series, allocation.seq(), safeScope);
    }

    /**
     * Declares that a series exists and records its authored format, allocating nothing. The generated
     * declaring components call this on every boot so a series is configurable BEFORE its first
     * document.
     *
     * @param series the series identity
     * @param format the authored format template
     */
    public void declare(String series, String format) {
        if (series == null || series.isBlank()) {
            return;
        }
        declared.put(series, format == null || format.isBlank() ? DEFAULT_FORMAT : format);
    }

    /**
     * Every series the tenant can configure: the declared ones (application-wide) merged with the rows
     * that actually have a counter (per-tenant). A declared series with no row yet reports counter 0,
     * so the management surface can seed its next value and its format BEFORE the first document
     * exists.
     *
     * @return the series, declared-first then any counter-only leftovers
     * @throws SQLException if the read fails
     */
    public List<DocumentNumberStore.Counter> listAll() throws SQLException {
        Map<String, DocumentNumberStore.Counter> byKey = new LinkedHashMap<>();
        for (DocumentNumberStore.Counter counter : store.list()) {
            byKey.put(counter.series() + '|' + counter.scope(), counter);
        }
        List<DocumentNumberStore.Counter> result = new ArrayList<>();
        declared.forEach((series, format) -> {
            DocumentNumberStore.Counter row = byKey.remove(series + '|');
            result.add(row == null ? new DocumentNumberStore.Counter(series, "", 0L, format, null, null)
                    // The declared format is authoritative - the application, not the stored row, owns it.
                    : new DocumentNumberStore.Counter(row.series(), row.scope(), row.counter(), format, row.prefix(), row.size()));
        });
        result.addAll(byKey.values());
        return result;
    }

    /**
     * Sets or clears the tenant's format override for a series. Pass {@code null} prefix AND size to
     * drop the override and fall back to the authored format.
     *
     * @param series the series identity
     * @param scope the scope key ({@code ""} for unscoped)
     * @param prefix the literal prefix (may be empty - that is a MEANINGFUL value: no prefix at all)
     * @param size the total rendered width
     * @throws SQLException if the write fails
     * @throws IllegalArgumentException if the width cannot hold the prefix plus at least one digit
     */
    public void setOverride(String series, String scope, String prefix, Integer size) throws SQLException {
        if (prefix == null && size == null) {
            store.setOverride(series, scope, null, null);
            return;
        }
        String safePrefix = prefix == null ? "" : prefix;
        if (size == null) {
            throw new IllegalArgumentException("A prefix override also needs a total size");
        }
        if (size <= safePrefix.length()) {
            throw new IllegalArgumentException("Size [" + size + "] leaves no room for a sequence after the prefix [" + safePrefix + "]");
        }
        if (size > MAX_SIZE) {
            throw new IllegalArgumentException("Size [" + size + "] exceeds the maximum of " + MAX_SIZE);
        }
        store.setOverride(series, scope, safePrefix, size);
    }

    /**
     * The format actually used: the tenant's {@code prefix + zero-padded sequence} when overridden,
     * else the authored template. An override is expressible only as a prefix and a width, so it
     * deliberately REPLACES the authored format rather than merging into it.
     *
     * @param authored the authored template
     * @param prefix the override prefix, or null
     * @param size the override width, or null
     * @return the effective template
     */
    static String effectiveFormat(String authored, String prefix, Integer size) {
        if (size == null) {
            return authored;
        }
        String safePrefix = prefix == null ? "" : prefix;
        int digits = size - safePrefix.length();
        if (digits < 1) {
            return authored; // an unusable override never silently mangles the number
        }
        return safePrefix + "{seq:0" + digits + "}";
    }

    /**
     * Whether a format can be expressed as a prefix plus a padded sequence. A format carrying scope
     * tokens ({@code {year}}, {@code {series}}) cannot, so the management surface offers no prefix/size
     * override for it instead of silently dropping the tokens.
     *
     * @param format the authored template
     * @return true when only the sequence token appears
     */
    static boolean overridable(String format) {
        if (format == null || format.isBlank()) {
            return true;
        }
        Matcher matcher = TOKEN.matcher(format);
        while (matcher.find()) {
            if (!"seq".equals(matcher.group(1))) {
                return false;
            }
        }
        return true;
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
