/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Base64;

import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.restassured.http.ContentType;

/**
 * Regression test for dirigible#6680: the db API facades rejected {@code null} parameter values
 * with "Unsupported parameter format", so the Database perspective's Results view could not save an
 * edited row whenever any column carried NULL - even though the JDBC layer binds a JSON null as SQL
 * NULL. Drives the same CRUD endpoint the Results view uses.
 */
class DatabaseCrudNullValuesIT extends IntegrationTest {

    /**
     * Quoted in every statement so both H2 (upper-folding) and PostgreSQL (lower-folding) keep the
     * exact case.
     */
    private static final String TABLE_NAME = "DB_NULL_PARAMS_IT";

    @Autowired
    private DataSourcesManager dataSourcesManager;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @BeforeEach
    void createAndSeedTable() throws Exception {
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE \"" + TABLE_NAME
                    + "\" (\"ID\" INTEGER NOT NULL PRIMARY KEY, \"NAME\" VARCHAR(64), \"NOTE\" VARCHAR(64))");
            statement.executeUpdate("INSERT INTO \"" + TABLE_NAME + "\" (\"ID\", \"NAME\", \"NOTE\") VALUES (1, 'John', 'seed')");
        }
    }

    @AfterEach
    void dropTable() throws Exception {
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE \"" + TABLE_NAME + "\"");
        }
    }

    @Test
    void a_row_updates_to_null_through_the_results_view_endpoint() throws Exception {
        String datasourceName = dataSourcesManager.getDefaultDataSource()
                                                  .getName();
        String schemaName;
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection()) {
            schemaName = connection.getSchema();
        }
        // The Results view base64-encodes the table name in the path (see result.js).
        String encodedTableName = Base64.getEncoder()
                                        .encodeToString(TABLE_NAME.getBytes(StandardCharsets.UTF_8));
        String url = "/services/js/view-databases/js/databaseTable.js/" + datasourceName + "/" + schemaName + "/" + encodedTableName;

        // NAME: null used to make Update.execute throw before reaching the database.
        restAssuredExecutor.execute(() -> given().contentType(ContentType.JSON)
                                                 .body("{\"data\":{\"ID\":1,\"NAME\":null,\"NOTE\":\"kept\"},\"primaryKey\":[\"ID\"]}")
                                                 .when()
                                                 .put(url)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("success", equalTo(true)));

        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                PreparedStatement statement =
                        connection.prepareStatement("SELECT \"NAME\", \"NOTE\" FROM \"" + TABLE_NAME + "\" WHERE \"ID\" = 1");
                ResultSet resultSet = statement.executeQuery()) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getString("NAME")).isNull();
            assertThat(resultSet.getString("NOTE")).isEqualTo("kept");
        }
    }
}
