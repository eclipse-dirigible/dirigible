/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.processes.tenant.export;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * The tables an export takes are the ones the applications declare - a tenant's schema holds the
 * platform's tables too, and they are never the tenant's to take away.
 */
class TenantExportTablesTest {

    @Test
    void onlyDeclaredTablesOfTheSchemaAreTaken() {
        Set<String> tables = TenantExportTables.select(List.of("LIBRARY_BOOK", "LIBRARY_AUTHOR", "SOMETHING_ELSE"),
                List.of("LIBRARY_BOOK", "LIBRARY_AUTHOR", "LIBRARY_NOT_CREATED_HERE"));

        assertEquals(Set.of("LIBRARY_BOOK", "LIBRARY_AUTHOR"), tables);
    }

    @Test
    void platformTablesAreNeverTaken() {
        // the platform keeps DIRIGIBLE_CONFIGURATIONS in a tenant's schema, and a declared name does not
        // change that
        Set<String> tables = TenantExportTables.select(List.of("DIRIGIBLE_CONFIGURATIONS", "dirigible_other", "LIBRARY_BOOK"),
                List.of("DIRIGIBLE_CONFIGURATIONS", "DIRIGIBLE_OTHER", "LIBRARY_BOOK"));

        assertEquals(Set.of("LIBRARY_BOOK"), tables);
    }

    @Test
    void namesMatchIgnoringCaseAndAreAnsweredAsTheSchemaSpellsThem() {
        Set<String> tables = TenantExportTables.select(List.of("library_book"), List.of("LIBRARY_BOOK"));

        assertEquals(Set.of("library_book"), tables);
    }

    @Test
    void aSchemaWithoutTablesGivesNone() {
        assertEquals(Set.of(), TenantExportTables.select(null, List.of("LIBRARY_BOOK")));
        assertEquals(Set.of(), TenantExportTables.select(List.of(), List.of("LIBRARY_BOOK")));
    }
}
