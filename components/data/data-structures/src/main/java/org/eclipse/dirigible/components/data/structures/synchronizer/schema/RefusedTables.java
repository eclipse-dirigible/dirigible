/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.structures.synchronizer.schema;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.dirigible.components.data.structures.domain.Schema;

/**
 * The tables of one schema pass that the database refused. A refused table is skipped so the rest
 * of the schema still lands, and the pass then reports the schema as failed instead of created, so
 * the synchronizer records it FAILED and retries it (#7766).
 */
final class RefusedTables {

    /** The refusal of each table, by table name, in declaration order. */
    private final Map<String, Exception> refusals = new LinkedHashMap<>();

    /**
     * Records a refused table.
     *
     * @param tableName the table name
     * @param cause the refusal
     */
    void add(String tableName, Exception cause) {
        refusals.put(tableName, cause);
    }

    /**
     * Fails the schema when any of its tables was refused, naming each one.
     *
     * @param schema the schema of the pass
     * @throws SQLException naming the refused tables, caused by the first refusal
     */
    void throwIfAny(Schema schema) throws SQLException {
        if (refusals.isEmpty()) {
            return;
        }
        Exception first = refusals.values()
                                  .iterator()
                                  .next();
        SQLException failure = new SQLException("Schema [" + schema.getName() + "]: the database refused " + refusals.size() + " table(s) "
                + refusals.keySet() + " - the first refusal: " + first.getMessage(), first);
        refusals.values()
                .stream()
                .skip(1)
                .forEach(failure::addSuppressed);
        throw failure;
    }
}
