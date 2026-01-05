/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api.java;

import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.database.helpers.DatabaseMetadataHelper;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.fail;

class DatabaseMetadataHelperTest extends IntegrationTest {

    @Autowired
    private DataSourcesManager datasourcesManager;

    /**
     * List schemas test.
     */
    @Test
    public void listSchemasTest() {
        try {
            try (Connection connection = datasourcesManager.getDefaultDataSource()
                                                           .getConnection()) {
                DatabaseMetadataHelper.listSchemas(connection, null, null, null);
            }
        } catch (SQLException e) {
            fail(e);
        }
    }

}
