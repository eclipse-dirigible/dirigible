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
import org.eclipse.dirigible.components.data.sources.domain.DataSource;
import org.eclipse.dirigible.components.data.sources.manager.DataSourceInitializer;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.eclipse.dirigible.database.persistence.utils.DatabaseMetadataUtil;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.awaitility.AwaitilityExecutor;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.history.HistoricProcessInstance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.notNullValue;

class SchemaExportImportIT extends IntegrationTest {

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private DataSourcesManager dataSourcesManager;

    @Autowired
    private ProcessEngine processEngine;

    @Autowired
    private DataSourceInitializer dataSourceInitializer;

    @Test
    void testSystemDBExportImport() throws SQLException {
        String exportProcessId = triggerSystemDBExportProcess();
        assertProcessExecutedSuccessfully(exportProcessId);

        createTargetDataSource();
        String importProcessId = triggerSystemDBImportProcess();
        assertProcessExecutedSuccessfully(importProcessId);

        DirigibleDataSource dataSource = dataSourcesManager.getDataSource("TARGETDS");
        List<String> createdTables = DatabaseMetadataUtil.getTablesInSchema(dataSource, "PUBLIC");
        assertThat(createdTables).hasSizeGreaterThan(0);
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

        String exportProcessId = restAssuredExecutor.executeWithResult(() -> given().contentType(ContentType.JSON)
                                                                                    .body(body)
                                                                                    .when()
                                                                                    .post("/services/data/schema/exportProcesses")
                                                                                    .then()
                                                                                    .statusCode(202)
                                                                                    .body("processId", notNullValue())
                                                                                    .extract()
                                                                                    .path("processId"));
        return exportProcessId;
    }

    private void assertProcessExecutedSuccessfully(String processInstanceId) {
        AwaitilityExecutor.execute("Process with id " + processInstanceId + " didn't completed for the expected time.",
                () -> await().atMost(10, TimeUnit.SECONDS)
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

    private void createTargetDataSource() {

        DataSource targetDataSource = new DataSource();
        targetDataSource.setName("TARGETDS");
        targetDataSource.setDriver("org.h2.Driver");
        targetDataSource.setUsername("sa");
        targetDataSource.setPassword("saPass");
        targetDataSource.setUrl("jdbc:h2:file:./target/dirigible/h2/TARGETDS");

        dataSourceInitializer.initialize(targetDataSource);
    }

    private String triggerSystemDBImportProcess() {
        // exclude Flowable tables which are related to the Flowable export process execution
        // if not excluded, import will fail due to import issues related to table constraints
        String body = """
                {
                    "dataSource": "TARGETDS",
                    "exportPath": "/systemdb-export-folder"
                }
                """;

        String exportProcessId = restAssuredExecutor.executeWithResult(() -> given().contentType(ContentType.JSON)
                                                                                    .body(body)
                                                                                    .when()
                                                                                    .post("/services/data/schema/importProcesses")
                                                                                    .then()
                                                                                    .statusCode(202)
                                                                                    .body("processId", notNullValue())
                                                                                    .extract()
                                                                                    .path("processId"));
        return exportProcessId;
    }
}
