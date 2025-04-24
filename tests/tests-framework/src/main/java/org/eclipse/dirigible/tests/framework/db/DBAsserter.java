/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.framework.db;

import org.assertj.db.api.Assertions;
import org.assertj.db.type.AssertDbConnection;
import org.assertj.db.type.AssertDbConnectionFactory;
import org.assertj.db.type.Table;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
public class DBAsserter {
    private final DataSourcesManager dataSourcesManager;

    protected DBAsserter(DataSourcesManager dataSourcesManager) {
        this.dataSourcesManager = dataSourcesManager;
    }

    public void assertRowCount(String tableName, int expectedRowCount) {
        DataSource dataSource = dataSourcesManager.getDefaultDataSource();
        AssertDbConnection connection = AssertDbConnectionFactory.of(dataSource)
                                                                 .create();

        Table table = connection.table(tableName)
                                .build();

        Assertions.assertThat(table)
                  .hasNumberOfRows(expectedRowCount);
    }

    public void assertRowHasColumnWithValue(String tableName, int rowIndex, String columnName, Object expectedValue) {
        DataSource dataSource = dataSourcesManager.getDefaultDataSource();
        AssertDbConnection connection = AssertDbConnectionFactory.of(dataSource)
                                                                 .create();

        Table table = connection.table(tableName)
                                .build();

        Assertions.assertThat(table)
                  .row(rowIndex)
                  .value(columnName)
                  .isEqualTo(expectedValue);
    }

    public void assertTableHasColumn(String tableName, String columnName) {
        DataSource dataSource = dataSourcesManager.getDefaultDataSource();
        AssertDbConnection connection = AssertDbConnectionFactory.of(dataSource)
                                                                 .create();

        Table table = connection.table(tableName)
                                .build();

        Assertions.assertThat(table)
                  .column(columnName);
    }

}

