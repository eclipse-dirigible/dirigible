/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api.java.db;

import io.restassured.http.ContentType;
import org.assertj.db.api.Assertions;
import org.assertj.db.type.Table;
import org.eclipse.dirigible.components.data.sources.domain.DataSource;
import org.eclipse.dirigible.components.data.sources.manager.DataSourceInitializer;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.database.DatabaseSystem;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.eclipse.dirigible.database.persistence.utils.DatabaseMetadataUtil;
import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.dialects.SqlDialectFactory;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.awaitility.AwaitilityExecutor;
import org.eclipse.dirigible.tests.framework.db.DBAsserter;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.framework.util.ResourceUtil;
import org.eclipse.dirigible.tests.framework.util.TestConditionsChecker;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.history.HistoricProcessInstance;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class SchemaExportImportIT extends IntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaExportImportIT.class);

    private static final String SOURCE_DATA_SOURCE_NAME = "SOURCEDS";
    private static final String TARGET_DATA_SOURCE_NAME = "TARGETDS";

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private DataSourcesManager dataSourcesManager;

    @Autowired
    private ProcessEngine processEngine;

    @Autowired
    private DataSourceInitializer dataSourceInitializer;

    @Autowired
    private DBAsserter dbAssserter;

    @Autowired
    private TestConditionsChecker testConditionsChecker;

    @Test
    void testExportImportOfTestingTables() throws SQLException {
        assumeTrue(testConditionsChecker.isH2OrPostgresDefaultDB(), "Skipping the test since the DefaultDB DB type is not supported");

        createH2DataSource(SOURCE_DATA_SOURCE_NAME);
        createTestingTables(SOURCE_DATA_SOURCE_NAME);

        String exportProcessId = triggerSourceDSExportProcess();
        assertProcessExecutedSuccessfully(exportProcessId);

        prepareTargetSchema();

        String importProcessId = triggerSourceDSImportProcess();
        assertProcessExecutedSuccessfully(importProcessId);

        assertImportedTestingTables();
    }

    private void createH2DataSource(String name) {
        DataSource targetDataSource = new DataSource();
        targetDataSource.setName(name);
        targetDataSource.setDriver("org.h2.Driver");
        targetDataSource.setUsername("sa");
        targetDataSource.setPassword("saPass");
        targetDataSource.setUrl("jdbc:h2:file:./target/dirigible/h2/" + name);

        dataSourceInitializer.initialize(targetDataSource);
    }

    private void createTestingTables(String dataSourceName) {
        DirigibleDataSource dataSource = dataSourcesManager.getDataSource(dataSourceName);
        String[] sqls = ResourceUtil.loadResource("/SchemaExportImportIT/test-tables.sql")
                                    .split(";");

        try (Connection connection = dataSource.getConnection()) {
            for (String sql : sqls) {
                LOGGER.debug("Will execute sql [{}]", sql);
                try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                    preparedStatement.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to create testing tables using sqls: " + Arrays.toString(sqls), ex);
        }
    }

    private String triggerSourceDSExportProcess() {
        String body = """
                {
                    "dataSource": "SOURCEDS",
                    "schema": "PUBLIC",
                    "exportPath": "/export-folder",
                    "includedTables": [],
                    "excludedTables": []
                }
                """;
        return triggerExportProcess(body);
    }

    private String triggerExportProcess(String body) {
        return restAssuredExecutor.executeWithResult(() -> given().contentType(ContentType.JSON)
                                                                  .body(body)
                                                                  .when()
                                                                  .post("/services/data/schema/exportProcesses")
                                                                  .then()
                                                                  .statusCode(202)
                                                                  .body("processId", notNullValue())
                                                                  .extract()
                                                                  .path("processId"));
    }

    private void assertProcessExecutedSuccessfully(String processInstanceId) {
        AwaitilityExecutor.execute("Process with id " + processInstanceId + " didn't completed for the expected time.",
                () -> await().atMost(60, TimeUnit.SECONDS)
                             .pollInterval(1, TimeUnit.SECONDS)
                             .until(() -> isProcessCompletedSuccessfully(processInstanceId)));
    }

    private boolean isProcessCompletedSuccessfully(String processInstanceId) {
        HistoricProcessInstance historicProcessInstance = processEngine.getHistoryService()
                                                                       .createHistoricProcessInstanceQuery()
                                                                       .processInstanceId(processInstanceId)
                                                                       .singleResult();
        return historicProcessInstance.getEndTime() != null;
    }

    private void prepareTargetSchema() throws SQLException {
        DirigibleDataSource defaultDataSource = dataSourcesManager.getDefaultDataSource();
        if (defaultDataSource.isOfType(DatabaseSystem.POSTGRESQL)) {
            ISqlDialect dialect = SqlDialectFactory.getDialect(defaultDataSource);
            String createTargetSchemaSql = dialect.create()
                                                  .schema("PUBLIC")
                                                  .build();
            LOGGER.debug("Creating target schema using sql [{}]", createTargetSchemaSql);
            try (Connection connection = defaultDataSource.getConnection();
                    PreparedStatement ps = connection.prepareStatement(createTargetSchemaSql)) {
                ps.executeUpdate();
            }
        } else {
            LOGGER.info("Missing logic for target schema preparation for default datasource {}", defaultDataSource);
        }
    }

    private String triggerSourceDSImportProcess() {
        String body = """
                {
                    "dataSource": "DefaultDB",
                    "exportPath": "/export-folder"
                }
                """;
        return triggerImportProcess(body);
    }

    private String triggerImportProcess(String body) {
        return restAssuredExecutor.executeWithResult(() -> given().contentType(ContentType.JSON)
                                                                  .body(body)
                                                                  .when()
                                                                  .post("/services/data/schema/importProcesses")
                                                                  .then()
                                                                  .statusCode(202)
                                                                  .body("processId", notNullValue())
                                                                  .extract()
                                                                  .path("processId"));
    }

    private void assertImportedTestingTables() throws SQLException {
        String targetSchema = "PUBLIC";
        assertTablesCount("DefaultDB", targetSchema, 5);

        Table users = dbAssserter.getDefaultDbTable(targetSchema, "USERS");
        // @formatter:off
        Assertions.assertThat(users).hasNumberOfRows(12)
                  .row(0).value("USERNAME").isEqualTo("ALICE").value("EMAIL").isEqualTo("alice@example.com").value("AGE").isEqualTo(18)
                  .row(1).value("USERNAME").isEqualTo("BOB").value("EMAIL").isEqualTo("bob@example.com").value("AGE").isEqualTo(19)
                  .row(2).value("USERNAME").isEqualTo("CHARLIE").value("EMAIL").isEqualTo("charlie@example.com").value("AGE").isEqualTo(20)
                  .row(3).value("USERNAME").isEqualTo("DIANA").value("EMAIL").isEqualTo("diana@example.com").value("AGE").isEqualTo(21)
                  .row(4).value("USERNAME").isEqualTo("ERIC").value("EMAIL").isEqualTo("eric@example.com").value("AGE").isEqualTo(22)
                  .row(5).value("USERNAME").isEqualTo("FIONA").value("EMAIL").isEqualTo("fiona@example.com").value("AGE").isEqualTo(23)
                  .row(6).value("USERNAME").isEqualTo("GEORGE").value("EMAIL").isEqualTo("george@example.com").value("AGE").isEqualTo(24)
                  .row(7).value("USERNAME").isEqualTo("HANNAH").value("EMAIL").isEqualTo("hannah@example.com").value("AGE").isEqualTo(25)
                  .row(8).value("USERNAME").isEqualTo("IVAN").value("EMAIL").isEqualTo("ivan@example.com").value("AGE").isEqualTo(26)
                  .row(9).value("USERNAME").isEqualTo("JULIA").value("EMAIL").isEqualTo("julia@example.com").value("AGE").isEqualTo(27)
                  .row(10).value("USERNAME").isEqualTo("KEVIN").value("EMAIL").isEqualTo("kevin@example.com").value("AGE").isEqualTo(28)
                  .row(11).value("USERNAME").isEqualTo("LINDA").value("EMAIL").isEqualTo("linda@example.com").value("AGE").isEqualTo(29);
        // @formatter:on

        Table categories = dbAssserter.getDefaultDbTable(targetSchema, "CATEGORIES");
        // @formatter:off
        Assertions.assertThat(categories).hasNumberOfRows(5)
                  .row(0).value("CATEGORY_ID").isEqualTo(1).value("NAME").isEqualTo("ELECTRONICS").value("DESCRIPTION").isEqualTo("Devices and gadgets")
                  .row(1).value("CATEGORY_ID").isEqualTo(2).value("NAME").isEqualTo("BOOKS").value("DESCRIPTION").isEqualTo("Printed and digital books")
                  .row(2).value("CATEGORY_ID").isEqualTo(3).value("NAME").isEqualTo("CLOTHING").value("DESCRIPTION").isEqualTo("Men and Women clothing")
                  .row(3).value("CATEGORY_ID").isEqualTo(4).value("NAME").isEqualTo("SPORTS").value("DESCRIPTION").isEqualTo("Sporting goods")
                  .row(4).value("CATEGORY_ID").isEqualTo(5).value("NAME").isEqualTo("HOME").value("DESCRIPTION").isEqualTo("Home and kitchen products");
        // @formatter:on

        Table products = dbAssserter.getDefaultDbTable(targetSchema, "PRODUCTS");
        // @formatter:off
        Assertions.assertThat(products).hasNumberOfRows(15)
                  .row(0).value("NAME").isEqualTo("LAPTOP").value("PRICE").isEqualTo(1200.15).value("CATEGORY_ID").isEqualTo(1)
                  .row(1).value("NAME").isEqualTo("SMARTPHONE").value("PRICE").isEqualTo(800.00).value("CATEGORY_ID").isEqualTo(1)
                  .row(2).value("NAME").isEqualTo("HEADPHONES").value("PRICE").isEqualTo(150.00).value("CATEGORY_ID").isEqualTo(1)
                  .row(3).value("NAME").isEqualTo("NOVEL BOOK").value("PRICE").isEqualTo(20.00).value("CATEGORY_ID").isEqualTo(2)
                  .row(4).value("NAME").isEqualTo("COOKBOOK").value("PRICE").isEqualTo(25.00).value("CATEGORY_ID").isEqualTo(2)
                  .row(5).value("NAME").isEqualTo("T-SHIRT").value("PRICE").isEqualTo(15.00).value("CATEGORY_ID").isEqualTo(3)
                  .row(6).value("NAME").isEqualTo("JEANS").value("PRICE").isEqualTo(50.00).value("CATEGORY_ID").isEqualTo(3)
                  .row(7).value("NAME").isEqualTo("SNEAKERS").value("PRICE").isEqualTo(75.00).value("CATEGORY_ID").isEqualTo(3)
                  .row(8).value("NAME").isEqualTo("FOOTBALL").value("PRICE").isEqualTo(30.00).value("CATEGORY_ID").isEqualTo(4)
                  .row(9).value("NAME").isEqualTo("TENNIS RACKET").value("PRICE").isEqualTo(120.00).value("CATEGORY_ID").isEqualTo(4)
                  .row(10).value("NAME").isEqualTo("BLENDER").value("PRICE").isEqualTo(60.00).value("CATEGORY_ID").isEqualTo(5)
                  .row(11).value("NAME").isEqualTo("VACUUM CLEANER").value("PRICE").isEqualTo(150.00).value("CATEGORY_ID").isEqualTo(5)
                  .row(12).value("NAME").isEqualTo("DESK LAMP").value("PRICE").isEqualTo(40.00).value("CATEGORY_ID").isEqualTo(5)
                  .row(13).value("NAME").isEqualTo("BACKPACK").value("PRICE").isEqualTo(55.00).value("CATEGORY_ID").isEqualTo(3)
                  .row(14).value("NAME").isEqualTo("E-READER").value("PRICE").isEqualTo(110.00).value("CATEGORY_ID").isEqualTo(1);
        // @formatter:on

        Table orders = dbAssserter.getDefaultDbTable(targetSchema, "ORDERS");
        // @formatter:off
        Assertions.assertThat(orders).hasNumberOfRows(12)
                  .row(0).value("USER_ID").isEqualTo(1).value("TOTAL_AMOUNT").isEqualTo(1250.15)
                  .row(1).value("USER_ID").isEqualTo(2).value("TOTAL_AMOUNT").isEqualTo(875.00)
                  .row(2).value("USER_ID").isEqualTo(3).value("TOTAL_AMOUNT").isEqualTo(75.00)
                  .row(3).value("USER_ID").isEqualTo(4).value("TOTAL_AMOUNT").isEqualTo(40.00)
                  .row(4).value("USER_ID").isEqualTo(5).value("TOTAL_AMOUNT").isEqualTo(200.00)
                  .row(5).value("USER_ID").isEqualTo(6).value("TOTAL_AMOUNT").isEqualTo(175.00)
                  .row(6).value("USER_ID").isEqualTo(7).value("TOTAL_AMOUNT").isEqualTo(110.00)
                  .row(7).value("USER_ID").isEqualTo(8).value("TOTAL_AMOUNT").isEqualTo(1500.00)
                  .row(8).value("USER_ID").isEqualTo(9).value("TOTAL_AMOUNT").isEqualTo(95.00)
                  .row(9).value("USER_ID").isEqualTo(10).value("TOTAL_AMOUNT").isEqualTo(130.00)
                  .row(10).value("USER_ID").isEqualTo(11).value("TOTAL_AMOUNT").isEqualTo(300.00)
                  .row(11).value("USER_ID").isEqualTo(12).value("TOTAL_AMOUNT").isEqualTo(60.00);
        // @formatter:on

        Table orderItems = dbAssserter.getDefaultDbTable(targetSchema, "ORDER_ITEMS");
        // @formatter:off
        Assertions.assertThat(orderItems).hasNumberOfRows(21)
                  .row(0).value("ORDER_ID").isEqualTo(1).value("PRODUCT_ID").isEqualTo(1).value("QUANTITY").isEqualTo(1).value("PRICE").isEqualTo(1200.15)
                  .row(1).value("ORDER_ID").isEqualTo(1).value("PRODUCT_ID").isEqualTo(3).value("QUANTITY").isEqualTo(1).value("PRICE").isEqualTo(50.00)
                  .row(2).value("ORDER_ID").isEqualTo(2).value("PRODUCT_ID").isEqualTo(2).value("QUANTITY").isEqualTo(1).value("PRICE").isEqualTo(800.00)
                  .row(3).value("ORDER_ID").isEqualTo(2).value("PRODUCT_ID").isEqualTo(6).value("QUANTITY").isEqualTo(5).value("PRICE").isEqualTo(75.00)
                  .row(4).value("ORDER_ID").isEqualTo(3).value("PRODUCT_ID").isEqualTo(9).value("QUANTITY").isEqualTo(2).value("PRICE").isEqualTo(60.00)
                  .row(5).value("ORDER_ID").isEqualTo(3).value("PRODUCT_ID").isEqualTo(6).value("QUANTITY").isEqualTo(1).value("PRICE").isEqualTo(15.00)
                  .row(6).value("ORDER_ID").isEqualTo(4).value("PRODUCT_ID").isEqualTo(4).value("QUANTITY").isEqualTo(2).value("PRICE").isEqualTo(40.00)
                  .row(7).value("ORDER_ID").isEqualTo(5).value("PRODUCT_ID").isEqualTo(11).value("QUANTITY").isEqualTo(2).value("PRICE").isEqualTo(120.00)
                  .row(8).value("ORDER_ID").isEqualTo(5).value("PRODUCT_ID").isEqualTo(6).value("QUANTITY").isEqualTo(4).value("PRICE").isEqualTo(60.00)
                  .row(9).value("ORDER_ID").isEqualTo(6).value("PRODUCT_ID").isEqualTo(7).value("QUANTITY").isEqualTo(3).value("PRICE").isEqualTo(150.00)
                  .row(10).value("ORDER_ID").isEqualTo(6).value("PRODUCT_ID").isEqualTo(14).value("QUANTITY").isEqualTo(1).value("PRICE").isEqualTo(25.00)
                  .row(11).value("ORDER_ID").isEqualTo(7).value("PRODUCT_ID").isEqualTo(15).value("QUANTITY").isEqualTo(1).value("PRICE").isEqualTo(110.00)
                  .row(12).value("ORDER_ID").isEqualTo(8).value("PRODUCT_ID").isEqualTo(1).value("QUANTITY").isEqualTo(1).value("PRICE").isEqualTo(1200.00)
                  .row(13).value("ORDER_ID").isEqualTo(8).value("PRODUCT_ID").isEqualTo(2).value("QUANTITY").isEqualTo(1).value("PRICE").isEqualTo(800.00)
                  .row(14).value("ORDER_ID").isEqualTo(9).value("PRODUCT_ID").isEqualTo(8).value("QUANTITY").isEqualTo(1).value("PRICE").isEqualTo(75.00)
                  .row(15).value("ORDER_ID").isEqualTo(9).value("PRODUCT_ID").isEqualTo(6).value("QUANTITY").isEqualTo(1).value("PRICE").isEqualTo(15.00)
                  .row(16).value("ORDER_ID").isEqualTo(10).value("PRODUCT_ID").isEqualTo(13).value("QUANTITY").isEqualTo(2).value("PRICE").isEqualTo(80.00)
                  .row(17).value("ORDER_ID").isEqualTo(10).value("PRODUCT_ID").isEqualTo(6).value("QUANTITY").isEqualTo(1).value("PRICE").isEqualTo(15.00)
                  .row(18).value("ORDER_ID").isEqualTo(11).value("PRODUCT_ID").isEqualTo(12).value("QUANTITY").isEqualTo(2).value("PRICE").isEqualTo(300.00)
                  .row(19).value("ORDER_ID").isEqualTo(12).value("PRODUCT_ID").isEqualTo(5).value("QUANTITY").isEqualTo(2).value("PRICE").isEqualTo(50.00)
                  .row(20).value("ORDER_ID").isEqualTo(12).value("PRODUCT_ID").isEqualTo(4).value("QUANTITY").isEqualTo(1).value("PRICE").isEqualTo(20.00);
        // @formatter:on
    }

    private void assertTablesCount(String dataSourceName, String schema, int expectedTablesCount) throws SQLException {
        DirigibleDataSource dataSource = dataSourcesManager.getDataSource(dataSourceName);
        List<String> createdTables = DatabaseMetadataUtil.getTablesInSchema(dataSource, schema);

        assertThat(createdTables).hasSize(expectedTablesCount);
    }

    @Test
    void testSystemDBExportImport() throws SQLException {
        String exportProcessId = triggerSystemDBExportProcess();
        assertProcessExecutedSuccessfully(exportProcessId);

        createH2DataSource(TARGET_DATA_SOURCE_NAME);

        assertTablesCount(TARGET_DATA_SOURCE_NAME, "PUBLIC", 0);

        String importProcessId = triggerSystemDBImportProcess();
        assertProcessExecutedSuccessfully(importProcessId);

        DirigibleDataSource dataSource = dataSourcesManager.getDataSource(TARGET_DATA_SOURCE_NAME);
        List<String> createdTables = DatabaseMetadataUtil.getTablesInSchema(dataSource, "PUBLIC");
        assertThat(createdTables).hasSizeGreaterThan(0);
    }

    private String triggerSystemDBImportProcess() {
        String body = """
                {
                    "dataSource": "TARGETDS",
                    "exportPath": "/systemdb-export-folder"
                }
                """;
        return triggerImportProcess(body);
    }

    private String triggerSystemDBExportProcess() {
        // exclude Flowable tables which are related to the Flowable export process execution
        // if not excluded, import will fail due to import issues related to table constraints
        String body = """
                {
                    "dataSource": "SystemDB",
                    "schema": "PUBLIC",
                    "exportPath": "/systemdb-export-folder",
                    "includedTables": [],
                    "excludedTables": ["ACT_RU_VARIABLE", "ACT_RU_JOB"]
                }
                """;
        return triggerExportProcess(body);
    }
}
