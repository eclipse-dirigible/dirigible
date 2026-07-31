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

import org.eclipse.dirigible.components.engine.numbering.DocumentNumberService;
import org.eclipse.dirigible.sdk.component.Beans;

/**
 * Allocates gap-free document numbers from a named series.
 *
 * <p>
 * The number's SHAPE is not passed here and is deliberately not knowable from application code: a
 * series' prefix and total width are declared once in a module's {@code .numbers} artefact and are
 * configurable per tenant afterwards, so one application serves jurisdictions with different
 * numbering conventions without being forked or regenerated.
 *
 * <p>
 * A series may be PARTITIONED - typically per company, because two legal entities in one tenant
 * each owe their own sequential range. Pass the partition value (the {@code per} relation's id) and
 * that partition's own sequence is used.
 *
 * <p>
 * Example: {@code DocumentNumbers.next("Sales Invoice", String.valueOf(entity.Company))}.
 */
public final class DocumentNumbers {

    private DocumentNumbers() {}

    /**
     * Allocate the next number of an unpartitioned series.
     *
     * @param series the series identity
     * @return the allocated number
     */
    public static String next(String series) {
        return next(series, null);
    }

    /**
     * Allocate the next number of a series within a partition.
     *
     * @param series the series identity
     * @param partition the partition value (the {@code per} relation's id), or null when the series is
     *        not partitioned
     * @return the allocated number
     */
    public static String next(String series, String partition) {
        try {
            return Beans.get(DocumentNumberService.class)
                        .next(series, partition);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to allocate a document number for series [" + series + "]", e);
        }
    }
}
