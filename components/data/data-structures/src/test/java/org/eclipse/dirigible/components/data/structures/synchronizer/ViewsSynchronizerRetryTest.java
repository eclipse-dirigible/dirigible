/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.structures.synchronizer;

import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyWrapper;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizerCallback;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.data.structures.domain.View;
import org.eclipse.dirigible.components.data.structures.service.ViewService;
import org.eclipse.dirigible.components.database.DatabaseSystem;
import org.eclipse.dirigible.components.database.DirigibleConnection;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.eclipse.dirigible.database.sql.SqlFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A CREATE VIEW that fails because its table does not exist yet is recorded FAILED and retried on a
 * later pass - not parked as CREATED-with-error (#6942).
 */
class ViewsSynchronizerRetryTest {

    /** The in-memory database of the test. */
    private static final String JDBC_URL = "jdbc:h2:mem:views_synchronizer_retry;DB_CLOSE_DELAY=-1";

    /** The table the view selects from. */
    private static final String TABLE_NAME = "VIEW_RETRY_CITY";

    /** The view under test. */
    private static final String VIEW_NAME = "VIEW_RETRY_LINES";

    /** The synchronizer under test. */
    private ViewsSynchronizer synchronizer;

    /**
     * Wires the synchronizer over an in-memory database with a callback that persists the state the way
     * the synchronization processor does.
     *
     * @throws SQLException the SQL exception
     */
    @BeforeEach
    void setUp() throws SQLException {
        DirigibleDataSource dataSource = mock(DirigibleDataSource.class);
        when(dataSource.getConnection()).thenAnswer(invocation -> dirigibleConnection());
        DataSourcesManager dataSourcesManager = mock(DataSourcesManager.class);
        when(dataSourcesManager.getDefaultDataSource()).thenReturn(dataSource);
        ViewService viewService = mock(ViewService.class);
        when(viewService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        synchronizer = new ViewsSynchronizer(viewService, dataSourcesManager);
        SynchronizerCallback callback = mock(SynchronizerCallback.class);
        doAnswer(invocation -> {
            synchronizer.setStatus(artefact(invocation.getArgument(1)), invocation.getArgument(2), "");
            return null;
        }).when(callback)
          .registerState(any(), any(TopologyWrapper.class), any(ArtefactLifecycle.class));
        doAnswer(invocation -> {
            Throwable cause = invocation.getArgument(3);
            synchronizer.setStatus(artefact(invocation.getArgument(1)), invocation.getArgument(2), cause.getMessage());
            return null;
        }).when(callback)
          .registerState(any(), any(TopologyWrapper.class), any(ArtefactLifecycle.class), any(Throwable.class));
        synchronizer.setCallback(callback);
    }

    /**
     * Drops what the test created.
     *
     * @throws SQLException the SQL exception
     */
    @AfterEach
    void tearDown() throws SQLException {
        execute("DROP VIEW IF EXISTS \"" + VIEW_NAME + "\"");
        execute("DROP TABLE IF EXISTS \"" + TABLE_NAME + "\"");
    }

    /**
     * The failed create leaves the view FAILED with its cause and not completed, so the pass retries
     * it; once the table exists the same artefact is created and reads CREATED.
     *
     * @throws SQLException the SQL exception
     */
    @Test
    void failedCreateIsRecordedFailedAndRetriedOnceTheTableExists() throws SQLException {
        View view = new View("/report/lines.view", VIEW_NAME, "", null, "VIEW", null, "SELECT \"CITY_ID\" FROM \"" + TABLE_NAME + "\"");
        view.setLifecycle(ArtefactLifecycle.NEW);
        TopologyWrapper<View> wrapper = new TopologyWrapper<>(view, new HashMap<>(), synchronizer);

        assertFalse(synchronizer.completeImpl(wrapper, ArtefactPhase.CREATE), "A failed create is not completed - the pass retries it");
        assertEquals(ArtefactLifecycle.FAILED, view.getLifecycle(), "A failed create must read FAILED, not CREATED");
        assertTrue(view.getError()
                       .contains(TABLE_NAME),
                "The error must name the failing statement, got: " + view.getError());
        assertFalse(viewExists(), "No view can exist before its table does");

        execute("CREATE TABLE \"" + TABLE_NAME + "\" (\"CITY_ID\" INT PRIMARY KEY)");

        assertTrue(synchronizer.completeImpl(wrapper, ArtefactPhase.CREATE), "The retry over an existing table completes");
        assertEquals(ArtefactLifecycle.CREATED, view.getLifecycle(), "The healed view must read CREATED");
        assertEquals("", view.getError(), "The healed view must carry no stale error");
        assertTrue(viewExists(), "The retry must create the view");
    }

    /**
     * Opens a fresh connection to the in-memory database wearing the platform's connection type, which
     * the SQL dialect lookup reads the database system from.
     *
     * @return the connection
     * @throws SQLException the SQL exception
     */
    private static DirigibleConnection dirigibleConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(JDBC_URL, "sa", "");
        DirigibleConnection dirigibleConnection = mock(DirigibleConnection.class, delegatesTo(connection));
        doReturn(DatabaseSystem.H2).when(dirigibleConnection)
                                   .getDatabaseSystem();
        return dirigibleConnection;
    }

    /**
     * Whether the view exists in the database.
     *
     * @return true when it exists
     * @throws SQLException the SQL exception
     */
    private boolean viewExists() throws SQLException {
        try (Connection connection = DriverManager.getConnection(JDBC_URL, "sa", "")) {
            return SqlFactory.getNative(connection)
                             .existsTable(connection, VIEW_NAME);
        }
    }

    /**
     * Executes a statement against the in-memory database.
     *
     * @param sql the statement
     * @throws SQLException the SQL exception
     */
    private static void execute(String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection(JDBC_URL, "sa", ""); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    /**
     * Unwraps the artefact of a wrapper handed to the callback.
     *
     * @param wrapper the wrapper
     * @return the view
     */
    private static View artefact(Object wrapper) {
        return ((TopologyWrapper<?>) wrapper).getArtefact() instanceof View view ? view : null;
    }
}
