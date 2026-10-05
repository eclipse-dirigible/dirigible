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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.base.readiness.PlatformReadiness;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.data.structures.synchronizer.table.TableAlterProcessor;
import org.eclipse.dirigible.components.engine.template.velocity.VelocityGenerationEngine;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.logging.LogsAsserter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ch.qos.logback.classic.Level;

/**
 * A re-publish never costs a table the data of a column its definition stops declaring (#7635): the
 * column is kept, logged and counted, until the definition lists it as {@code dropped}; and a
 * column declared {@code renamedFrom} another takes the old one's values with it - including
 * through the schema the model generation emits.
 */
class TableUndeclaredColumnIT extends IntegrationTest {

    private static final String PROJECT = "table-undeclared-column-it";

    private static final String TABLE_LOCATION = "/" + PROJECT + "/tables/note.table";

    private static final String TABLE_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + TABLE_LOCATION;

    private static final String SCHEMA_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/invoice.schema";

    private static final String TEMPLATE_LOCATION = "/META-INF/dirigible/template-application-schema/data/application.schema.template";

    private static final String NOTE_TABLE = "UNDECLARED_NOTE";

    private static final String INVOICE_TABLE = "UNDECLARED_INVOICE";

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private DataSourcesManager dataSourcesManager;

    @Autowired
    private VelocityGenerationEngine velocityGenerationEngine;

    private LogsAsserter alterLogs;

    @BeforeEach
    void attachLogs() {
        alterLogs = new LogsAsserter(TableAlterProcessor.class, Level.INFO);
    }

    @Test
    void anUndeclaredColumnIsKeptUntilTheTableListsItAsDropped() throws Exception {
        publish(TABLE_PATH, noteTable("""
                {
                    "type": "VARCHAR",
                    "length": 20,
                    "nullable": false,
                    "name": "NOTE_B"
                }"""));
        execute("INSERT INTO \"" + NOTE_TABLE + "\" (\"NOTE_ID\", \"NOTE_A\", \"NOTE_B\") VALUES (1, 'a', 'b')");

        publish(TABLE_PATH, noteTable(null));

        assertThat(value(NOTE_TABLE, "NOTE_B")).as("the undeclared column keeps its data")
                                               .isEqualTo("b");
        alterLogs.assertLoggedMessage(
                "Column [NOTE_B] of table [" + NOTE_TABLE + "] is not declared by [" + TABLE_LOCATION + "] and is kept with its data",
                Level.WARN);
        assertThat(PlatformReadiness.getInstance()
                                    .getOrphanColumns()).as("the kept column is counted in the artefacts health component")
                                                        .isPositive();
        // the application no longer writes the column, so it must no longer refuse the insert
        execute("INSERT INTO \"" + NOTE_TABLE + "\" (\"NOTE_ID\", \"NOTE_A\") VALUES (2, 'a')");

        publish(TABLE_PATH, noteTable(null).replace("\"type\": \"TABLE\",", "\"type\": \"TABLE\", \"dropped\": [\"NOTE_B\"],"));

        assertThat(hasColumn(NOTE_TABLE, "NOTE_B")).as("a column listed as dropped is dropped")
                                                   .isFalse();
    }

    @Test
    void theGeneratedSchemaRenamesAColumnWithItsDataAndDropsTheListedOne() throws Exception {
        publish(SCHEMA_PATH, """
                {
                    "schema": {
                        "structures": [
                            {
                                "name": "UNDECLARED_INVOICE",
                                "type": "TABLE",
                                "columns": [
                                    { "type": "INTEGER", "primaryKey": true, "name": "INVOICE_ID" },
                                    { "type": "VARCHAR", "length": 20, "nullable": true, "name": "INVOICE_INVOICE_DATE" },
                                    { "type": "VARCHAR", "length": 20, "nullable": true, "name": "INVOICE_OLD_NOTE" }
                                ]
                            }
                        ]
                    }
                }
                """);
        execute("INSERT INTO \"" + INVOICE_TABLE + "\" VALUES (1, '2026-10-05', 'obsolete')");

        // what the model generation emits for `renamedFrom: invoiceDate` and `dropped: [oldNote]`
        publish(SCHEMA_PATH, renderInvoiceSchema());

        assertThat(value(INVOICE_TABLE, "INVOICE_ISSUE_DATE")).as("the renamed column carries the old one's values")
                                                              .isEqualTo("2026-10-05");
        assertThat(hasColumn(INVOICE_TABLE, "INVOICE_INVOICE_DATE")).as("the old name is gone, not left behind")
                                                                    .isFalse();
        assertThat(hasColumn(INVOICE_TABLE, "INVOICE_OLD_NOTE")).as("the dropped column is dropped")
                                                                .isFalse();
    }

    private static String noteTable(String extraColumn) {
        return """
                {
                    "name": "UNDECLARED_NOTE",
                    "type": "TABLE",
                    "columns": [
                        { "type": "INTEGER", "primaryKey": true, "nullable": false, "name": "NOTE_ID" },
                        { "type": "VARCHAR", "length": 20, "nullable": true, "name": "NOTE_A" }%s
                    ]
                }
                """.formatted(extraColumn == null ? "" : ",\n" + extraColumn);
    }

    private String renderInvoiceSchema() throws Exception {
        String template;
        try (InputStream in = getClass().getResourceAsStream(TEMPLATE_LOCATION)) {
            assertThat(in).as("template resource %s", TEMPLATE_LOCATION)
                          .isNotNull();
            template = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("name", "Invoice");
        model.put("dataName", INVOICE_TABLE);
        model.put("type", "PRIMARY");
        model.put("dataDropped", "INVOICE_OLD_NOTE");
        Map<String, Object> id = property("Id", "INVOICE_ID", "INTEGER");
        id.put("dataPrimaryKey", Boolean.TRUE);
        id.put("dataNotNull", Boolean.TRUE);
        Map<String, Object> issueDate = property("IssueDate", "INVOICE_ISSUE_DATE", "VARCHAR");
        issueDate.put("dataLength", "20");
        issueDate.put("dataRenamedFrom", "INVOICE_INVOICE_DATE");
        model.put("properties", new ArrayList<>(List.of(id, issueDate)));
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("models", List.of(model));
        parameters.put("tablePrefix", "");
        parameters.put("dataSource", "DefaultDB");
        byte[] out = velocityGenerationEngine.generate(parameters, TEMPLATE_LOCATION, template.getBytes(StandardCharsets.UTF_8));
        return new String(out, StandardCharsets.UTF_8);
    }

    private static Map<String, Object> property(String name, String dataName, String dataType) {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("name", name);
        property.put("dataName", dataName);
        property.put("dataType", dataType);
        property.put("dataPrimaryKey", Boolean.FALSE);
        return property;
    }

    private void publish(String path, String content) {
        repository.createResource(path, content.getBytes(StandardCharsets.UTF_8), false, "text/plain", true);
        synchronizationProcessor.forceProcessSynchronizers();
    }

    private String value(String table, String column) throws Exception {
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery("SELECT \"" + column + "\" FROM \"" + table + "\" WHERE " + "\""
                        + (NOTE_TABLE.equals(table) ? "NOTE_ID" : "INVOICE_ID") + "\" = 1")) {
            assertThat(rs.next()).as("row 1 of %s", table)
                                 .isTrue();
            return rs.getString(1);
        }
    }

    private boolean hasColumn(String table, String column) throws Exception {
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                ResultSet columns = connection.getMetaData()
                                              .getColumns(null, connection.getSchema(), table, column)) {
            return columns.next();
        }
    }

    private void execute(String sql) throws Exception {
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    @AfterEach
    void cleanup() throws Exception {
        for (String path : List.of(TABLE_PATH, SCHEMA_PATH)) {
            if (repository.hasResource(path)) {
                repository.removeResource(path);
            }
        }
        synchronizationProcessor.forceProcessSynchronizers();
        // the synchronizer keeps the data of a removed table, so drop the tables for the next run
        execute("DROP TABLE IF EXISTS \"" + NOTE_TABLE + "\"");
        execute("DROP TABLE IF EXISTS \"" + INVOICE_TABLE + "\"");
    }
}
