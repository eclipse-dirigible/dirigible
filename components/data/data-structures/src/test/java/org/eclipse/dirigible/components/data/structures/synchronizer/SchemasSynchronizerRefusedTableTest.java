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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyWrapper;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizerCallback;
import org.eclipse.dirigible.components.database.DatabaseSystem;
import org.eclipse.dirigible.components.database.DirigibleConnection;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.data.structures.domain.Schema;
import org.eclipse.dirigible.components.data.structures.domain.Table;
import org.eclipse.dirigible.components.data.structures.domain.TableColumn;
import org.eclipse.dirigible.components.data.structures.service.SchemaService;
import org.eclipse.dirigible.components.data.structures.service.TableService;
import org.eclipse.dirigible.components.data.structures.service.ViewService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A table the database refuses in a way that is fatal for the connection - PostgreSQL's SQLSTATE
 * {@code 0A000}, after which the pool marks the connection broken - costs the schema that table,
 * not every table declared after it, and the schema is recorded FAILED and retried instead of
 * CREATED (#7766).
 */
class SchemasSynchronizerRefusedTableTest {

    /** The table whose CREATE TABLE the database refuses. */
    private static final String REFUSED_TABLE = "T7766_REFUSED";

    /** The tables of the schema, in declaration order, the refused one in the middle. */
    private static final List<String> TABLES = List.of("T7766_BEFORE", REFUSED_TABLE, "T7766_AFTER", "T7766_LAST");

    /** The in-memory database, kept open across connections. */
    private static final String URL = "jdbc:h2:mem:schema_refused_table;DB_CLOSE_DELAY=-1";

    /** The synchronizer under test. */
    private SchemasSynchronizer synchronizer;

    /** Whether the database still refuses the table. */
    private final AtomicBoolean refusing = new AtomicBoolean(true);

    /** The connections the data source handed out. */
    private final AtomicInteger acquired = new AtomicInteger();

    /** The real connections behind the handed-out ones, closed after each test. */
    private final List<Connection> opened = new ArrayList<>();

    /** Keeps the in-memory database alive while the test runs. */
    private Connection keeper;

    /**
     * Wires the synchronizer over a default data source whose connections break on the refused table,
     * and a callback that writes the registered state onto the artefact, as the synchronization
     * processor does.
     *
     * @throws SQLException if the database cannot be opened
     */
    @BeforeEach
    void setUp() throws SQLException {
        keeper = DriverManager.getConnection(URL, "sa", "");

        DirigibleDataSource dataSource = mock(DirigibleDataSource.class);
        when(dataSource.getConnection()).thenAnswer(invocation -> breakingOnRefusal(DriverManager.getConnection(URL, "sa", "")));
        DataSourcesManager dataSourcesManager = mock(DataSourcesManager.class);
        when(dataSourcesManager.getDefaultDataSource()).thenReturn(dataSource);

        SchemaService schemaService = mock(SchemaService.class);
        when(schemaService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        synchronizer =
                new SchemasSynchronizer(schemaService, dataSourcesManager, mock(TableService.class), mock(ViewService.class), "DefaultDB");

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
     * Drops the in-memory database and closes every connection the test opened.
     *
     * @throws SQLException if closing fails
     */
    @AfterEach
    void tearDown() throws SQLException {
        for (Connection connection : opened) {
            connection.close();
        }
        try (Connection database = keeper; Statement statement = database.createStatement()) {
            statement.execute("DROP ALL OBJECTS");
        }
    }

    /**
     * Every table declared after the refused one is still created - on a connection acquired anew,
     * since the one the refusal broke answers nothing - and the schema reads FAILED, naming the refused
     * table, instead of CREATED.
     *
     * @throws SQLException if the database cannot be read
     */
    @Test
    void aRefusedTableCostsOnlyItselfAndFailsTheSchema() throws SQLException {
        Schema schema = schema();
        TopologyWrapper<Schema> wrapper = new TopologyWrapper<>(schema, new HashMap<>(), synchronizer);

        assertFalse(synchronizer.completeImpl(wrapper, ArtefactPhase.CREATE), "A schema with a refused table is not completed");

        assertEquals(ArtefactLifecycle.FAILED, schema.getLifecycle());
        assertTrue(schema.getError()
                         .contains(REFUSED_TABLE),
                "The failure names the refused table: " + schema.getError());
        assertTrue(exists("T7766_BEFORE"));
        assertFalse(exists(REFUSED_TABLE));
        assertTrue(exists("T7766_AFTER"), "The table after the refused one is created on a fresh connection");
        assertTrue(exists("T7766_LAST"));
        assertEquals(2, acquired.get(), "One connection for the pass, one more after the refusal broke it");
    }

    /**
     * A FAILED schema is attempted again, and once the database accepts the refused table the schema
     * heals to CREATED.
     *
     * @throws SQLException if the database cannot be read
     */
    @Test
    void aFailedSchemaIsRetriedAndHeals() throws SQLException {
        Schema schema = schema();
        TopologyWrapper<Schema> wrapper = new TopologyWrapper<>(schema, new HashMap<>(), synchronizer);
        assertFalse(synchronizer.completeImpl(wrapper, ArtefactPhase.CREATE));
        assertEquals(ArtefactLifecycle.FAILED, schema.getLifecycle());

        refusing.set(false);

        assertTrue(synchronizer.completeImpl(wrapper, ArtefactPhase.CREATE), "The retry completes once nothing is refused");
        assertEquals(ArtefactLifecycle.CREATED, schema.getLifecycle());
        assertTrue(exists(REFUSED_TABLE), "The retry creates the table refused the first time");
    }

    private static Schema schema() {
        Schema schema = new Schema("/refused/refused.schema", "refused", "", Set.of());
        for (String name : TABLES) {
            Table table = new Table(name);
            table.setSchema(null);
            table.setSchemaReference(schema);
            new TableColumn("ID", "INTEGER", null, false, true, null, "0", "0", false, table);
            schema.getTables()
                  .add(table);
        }
        schema.setLifecycle(ArtefactLifecycle.NEW);
        return schema;
    }

    private boolean exists(String table) throws SQLException {
        try (ResultSet tables = keeper.getMetaData()
                                      .getTables(null, null, table, null)) {
            return tables.next();
        }
    }

    /**
     * Wraps a real connection so that preparing the refused table's DDL closes it and throws, as a
     * PostgreSQL {@code 0A000} refusal leaves a pooled connection: every later statement on it fails.
     *
     * @param real the real connection
     * @return the wrapping connection, answering as H2
     */
    private DirigibleConnection breakingOnRefusal(Connection real) {
        acquired.incrementAndGet();
        opened.add(real);
        return (DirigibleConnection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {DirigibleConnection.class},
                (proxy, method, args) -> {
                    if ("getDatabaseSystem".equals(method.getName())) {
                        return DatabaseSystem.H2;
                    }
                    if ("isOfType".equals(method.getName())) {
                        return args != null && DatabaseSystem.H2.equals(args[0]);
                    }
                    if (refusing.get() && "prepareStatement".equals(method.getName()) && args != null && args[0] instanceof String sql
                            && sql.contains(REFUSED_TABLE)) {
                        real.close();
                        throw new SQLException("cannot use column reference in DEFAULT expression", "0A000");
                    }
                    try {
                        return method.invoke(real, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }

    /**
     * Unwraps the artefact of a wrapper handed to the callback.
     *
     * @param wrapper the wrapper
     * @return the schema
     */
    private static Schema artefact(Object wrapper) {
        return ((TopologyWrapper<?>) wrapper).getArtefact() instanceof Schema schema ? schema : null;
    }
}
