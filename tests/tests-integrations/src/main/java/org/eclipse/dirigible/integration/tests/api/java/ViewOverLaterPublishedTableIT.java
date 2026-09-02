/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api.java;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.data.structures.domain.View;
import org.eclipse.dirigible.components.data.structures.service.ViewService;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.database.sql.SqlFactory;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A view whose SELECT reads another module's tables must come alive on the pass that creates those
 * tables - the incremental-publish flow of #6942, where the report module is published before its
 * owner.
 *
 * <p>
 * The failed CREATE VIEW used to be recorded as CREATED-with-error, a state neither the CREATE nor
 * the UPDATE gate matches, so the view stayed missing until its file was edited. It is now recorded
 * FAILED, which the next pass retries.
 */
// One Dirigible boot for the class: the single scenario cleans up after itself.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ViewOverLaterPublishedTableIT extends IntegrationTest {

    /** The project publishing the view first. */
    private static final String REPORT_PROJECT = "view-later-table-it-report";

    /** The project publishing the table second. */
    private static final String OWNER_PROJECT = "view-later-table-it-owner";

    /** The registry-relative location of the view - how the synchronizer records it. */
    private static final String VIEW_LOCATION = "/" + REPORT_PROJECT + "/views/lines.view";

    /** The repository-absolute path of the view fixture. */
    private static final String VIEW_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + VIEW_LOCATION;

    /** The repository-absolute path of the table fixture. */
    private static final String TABLE_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + OWNER_PROJECT + "/tables/city.table";

    /** The table the view selects from. */
    private static final String TABLE_NAME = "VIEW_LATER_CITY";

    /** The view under test. */
    private static final String VIEW_NAME = "VIEW_LATER_LINES";

    /** The view fixture source. */
    private static final String VIEW_SOURCE = """
            {
                "name": "%s",
                "type": "VIEW",
                "query": "SELECT \\"CITY_ID\\", \\"CITY_NAME\\" FROM \\"%s\\""
            }
            """.formatted(VIEW_NAME, TABLE_NAME);

    /** The table fixture source. */
    private static final String TABLE_SOURCE = """
            {
                "name": "%s",
                "type": "TABLE",
                "columns": [
                    {
                        "type": "INTEGER",
                        "primaryKey": true,
                        "nullable": false,
                        "name": "CITY_ID"
                    },
                    {
                        "type": "VARCHAR",
                        "length": 40,
                        "nullable": false,
                        "name": "CITY_NAME"
                    }
                ]
            }
            """.formatted(TABLE_NAME);

    /** The data source manager. */
    @Autowired
    private DataSourcesManager dataSourceManager;

    /** The view service - the artefact state under assertion. */
    @Autowired
    private ViewService viewService;

    /** The repository. */
    @Autowired
    private IRepository repository;

    /** The synchronization processor. */
    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    /**
     * The first pass has nothing that can satisfy the view, so its in-pass retry is a dead end by
     * design; keep that dead end short instead of paying the production interval ten times.
     */
    @BeforeAll
    static void shortenCrossRetry() {
        Configuration.set("DIRIGIBLE_SYNCHRONIZER_CROSS_RETRY_COUNT", "2");
        Configuration.set("DIRIGIBLE_SYNCHRONIZER_CROSS_RETRY_INTERVAL_MILLIS", "100");
    }

    /**
     * Publishing the view before the table it reads parks it FAILED with the cause; publishing the
     * table heals it on that very pass.
     *
     * @throws Exception the exception
     */
    @Test
    void viewOverTablesPublishedLaterHealsOnThePassThatCreatesThem() throws Exception {
        write(VIEW_PATH, VIEW_SOURCE);
        synchronizationProcessor.forceProcessSynchronizers();

        View parked = view();
        assertEquals(ArtefactLifecycle.FAILED, parked.getLifecycle(), "A CREATE VIEW over a missing table must be recorded FAILED");
        assertTrue(parked.getError()
                         .contains(TABLE_NAME),
                "The recorded error must name the failing statement, got: " + parked.getError());
        assertFalse(viewExists(), "The view cannot exist before its table does");

        write(TABLE_PATH, TABLE_SOURCE);
        synchronizationProcessor.forceProcessSynchronizers();

        assertTrue(viewExists(), "The pass that creates the table must also create the view that was waiting for it");
        View healed = view();
        assertEquals(ArtefactLifecycle.CREATED, healed.getLifecycle(), "The healed view must read CREATED");
        assertTrue(healed.getError() == null || healed.getError()
                                                      .isBlank(),
                "The healed view must carry no stale error, got: " + healed.getError());
        assertEquals(0, rowCount(), "The view must be queryable");
    }

    /**
     * Loads the single view artefact recorded for the fixture.
     *
     * @return the view
     */
    private View view() {
        List<View> views = viewService.findByLocation(VIEW_LOCATION);
        assertEquals(1, views.size(), "Exactly one view artefact is expected at " + VIEW_LOCATION);
        return views.get(0);
    }

    /**
     * Whether the view exists in the database.
     *
     * @return true when the view exists
     * @throws Exception the exception
     */
    private boolean viewExists() throws Exception {
        try (Connection connection = dataSourceManager.getDefaultDataSource()
                                                      .getConnection()) {
            return SqlFactory.getNative(connection)
                             .existsTable(connection, VIEW_NAME);
        }
    }

    /**
     * Counts the rows the view returns.
     *
     * @return the row count
     * @throws Exception the exception
     */
    private int rowCount() throws Exception {
        try (Connection connection = dataSourceManager.getDefaultDataSource()
                                                      .getConnection();
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM \"" + VIEW_NAME + "\"")) {
            assertTrue(rs.next(), "COUNT(*) returned no row");
            return rs.getInt(1);
        }
    }

    /**
     * Writes a fixture resource.
     *
     * @param path the path
     * @param source the source
     */
    private void write(String path, String source) {
        repository.createResource(path, source.getBytes(StandardCharsets.UTF_8), false, "text/plain", true);
    }

    /**
     * Cleanup the fixture.
     *
     * @throws Exception the exception
     */
    @AfterEach
    void cleanup() throws Exception {
        for (String path : List.of(VIEW_PATH, TABLE_PATH)) {
            if (repository.hasResource(path)) {
                repository.removeResource(path);
            }
        }
        synchronizationProcessor.forceProcessSynchronizers();
        try (Connection connection = dataSourceManager.getDefaultDataSource()
                                                      .getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute("DROP VIEW IF EXISTS \"" + VIEW_NAME + "\"");
            statement.execute("DROP TABLE IF EXISTS \"" + TABLE_NAME + "\"");
        }
    }
}
