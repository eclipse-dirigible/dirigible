/*
 * Copyright (c) 2010-2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.source.snowpark;

import org.eclipse.dirigible.components.database.ConnectionEnhancer;
import org.eclipse.dirigible.components.database.DatabaseSystem;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;

@Component
class SnowflakeConnectionEnhancer implements ConnectionEnhancer {
    @Override
    public boolean isApplicable(DatabaseSystem databaseSystem) {
        return databaseSystem.isSnowflake();
    }

    @Override
    public void apply(Connection connection) throws SQLException {
        connection.createStatement()
                  .executeQuery("ALTER SESSION SET JDBC_QUERY_RESULT_FORMAT='JSON'");
    }
}
