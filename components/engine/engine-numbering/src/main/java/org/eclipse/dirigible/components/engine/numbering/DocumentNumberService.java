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
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Document numbering runtime: allocates the next value of a series and renders it.
 *
 * <p>
 * A series is {@code prefix + sequence zero-padded to size}, and nothing else - no token grammar.
 * The sequence is CONTINUOUS and never reset: jurisdictions that require an annual restart get it
 * by an administrator setting the prefix and the next value in January, which is visible and
 * auditable, rather than by a hidden reset rule that could mint a number twice.
 *
 * <p>
 * A series may be PARTITIONED (intent {@code per: Company}): each partition value has its own row,
 * so its own sequence, prefix and width. Two legal entities in one tenant each owe their own
 * sequential range and must not share a counter. Identical numbers across partitions are correct -
 * a number must be unique within a company's book, not across companies.
 *
 * <p>
 * A series must be DECLARED (a {@code .numbers} artefact, synchronized per tenant) before it can be
 * allocated from. Allocating from an unknown series fails loudly: a document must never carry a
 * number in a shape nobody chose.
 */
@Component
public class DocumentNumberService {

    /** Widest renderable number; the stored column is VARCHAR(100) and no series needs more. */
    static final int MAX_SIZE = 40;

    private final DocumentNumberStore store;

    DocumentNumberService(DocumentNumberStore store) {
        this.store = store;
    }

    /**
     * Allocate and render the next number of an unpartitioned series.
     *
     * @param series the series identity
     * @return the rendered number
     * @throws SQLException if the allocation fails
     */
    public String next(String series) throws SQLException {
        return next(series, null);
    }

    /**
     * Allocate and render the next number of a series, within a partition.
     *
     * @param series the series identity
     * @param partition the value of the {@code per} relation, or null for an unpartitioned series
     * @return the rendered number
     * @throws SQLException if the allocation fails
     * @throws IllegalStateException if the series is not declared for this tenant
     */
    public String next(String series, String partition) throws SQLException {
        DocumentNumberStore.Allocation allocation = store.allocate(series, partition == null ? "" : partition);
        return render(allocation.prefix(), allocation.size(), allocation.value());
    }

    /**
     * Renders {@code prefix + value} zero-padded so the whole number is {@code size} characters. A
     * value that outgrows the width is NOT truncated - it renders in full, because a wrong number is
     * worse than a wide one, and the overflow is visible enough to be corrected.
     *
     * @param prefix the literal prefix (may be empty)
     * @param size the total width
     * @param value the allocated sequence value
     * @return the rendered number
     */
    static String render(String prefix, int size, long value) {
        String safePrefix = prefix == null ? "" : prefix;
        int digits = Math.max(1, size - safePrefix.length());
        return safePrefix + String.format("%0" + digits + "d", value);
    }

    /**
     * Every series row of the current tenant, for the management surface.
     *
     * @return the series rows
     * @throws SQLException if the read fails
     */
    public List<DocumentNumberStore.Series> list() throws SQLException {
        return store.list();
    }

    /**
     * Provisions a declared series for this tenant if it has none yet - the synchronizer's write. An
     * existing row is left untouched: its counter is live and its prefix/width may have been configured
     * by an administrator, and neither is the artefact's business.
     *
     * @param series the series identity
     * @param prefix the declared default prefix
     * @param size the declared default width
     * @throws SQLException if the write fails
     */
    public void provision(String series, String prefix, int size) throws SQLException {
        store.provision(series, "", prefix, size);
    }

    /**
     * Sets the <b>next</b> value a series will allocate (e.g. restart at 1 in January).
     *
     * @param series the series identity
     * @param partition the partition value ({@code ""} for unpartitioned)
     * @param next the next value to allocate
     * @throws SQLException if the write fails
     */
    public void setNext(String series, String partition, long next) throws SQLException {
        store.setCounter(series, partition == null ? "" : partition, Math.max(0, next - 1));
    }

    /**
     * Sets the tenant's prefix and width for a series.
     *
     * @param series the series identity
     * @param partition the partition value ({@code ""} for unpartitioned)
     * @param prefix the literal prefix (empty is meaningful - no prefix at all)
     * @param size the total width
     * @throws SQLException if the write fails
     * @throws IllegalArgumentException if the width cannot hold the prefix plus a digit
     */
    public void setShape(String series, String partition, String prefix, int size) throws SQLException {
        String safePrefix = prefix == null ? "" : prefix;
        if (size <= safePrefix.length()) {
            throw new IllegalArgumentException("Size [" + size + "] leaves no room for a sequence after the prefix [" + safePrefix + "]");
        }
        if (size > MAX_SIZE) {
            throw new IllegalArgumentException("Size [" + size + "] exceeds the maximum of " + MAX_SIZE);
        }
        store.setShape(series, partition == null ? "" : partition, safePrefix, size);
    }
}
