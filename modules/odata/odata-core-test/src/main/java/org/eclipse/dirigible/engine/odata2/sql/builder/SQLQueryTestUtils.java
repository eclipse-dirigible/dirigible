/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.odata2.sql.builder;

import org.apache.olingo.odata2.api.edm.EdmStructuralType;

/**
 * SQLQueryTestUtils.
 */
public class SQLQueryTestUtils {

    /**
     * Instantiates a new SQL query test utils.
     */
    private SQLQueryTestUtils() {
        // Static class
    }

    /**
     * Grant table alias for structural type in query.
     *
     * @param sqlQuery the query
     * @param structuralType the type
     */
    public static void grantTableAliasForStructuralTypeInQuery(final SQLSelectBuilder sqlQuery, final EdmStructuralType structuralType) {
        sqlQuery.grantTableAliasForStructuralTypeInQuery(structuralType);
    }

}
