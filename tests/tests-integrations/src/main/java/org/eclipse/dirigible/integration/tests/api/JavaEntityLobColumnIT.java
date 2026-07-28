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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.database.sql.DataTypeUtils;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.base.ProjectUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A client-Java {@code @Entity} mapping a large-text column keeps that column large when the
 * property is annotated {@code @Lob}.
 *
 * <p>
 * Entity registration goes through Hibernate's {@code hbm2ddl.auto = update}, which resizes an
 * existing column to whatever the mapping claims. A plain {@code String} property claims
 * {@code @Column}'s length - 255 by default - so the {@code CLOB} the {@code .table} artefact
 * declares used to end up a {@code VARCHAR(255)} on every deploy. {@code @Lob} maps it past the
 * dialect's maximum {@code VARCHAR} instead, which resolves to the database's own large-text type.
 *
 * <p>
 * The fixture keeps an {@code Instant}-mapped {@code TIMESTAMP} column too: the database reports it
 * back as its with-time-zone variant, which is the JDBC type the schema comparison has to tolerate
 * in the very same publish.
 */
class JavaEntityLobColumnIT extends IntegrationTest {

    private static final String PROJECT = "JavaEntityLobColumnIT";

    private static final String TABLE_NAME = "SCHEMA_FIRST_NOTE";

    /** The length a String property is mapped with when it declares none. */
    private static final int MAPPING_DEFAULT_LENGTH = 255;

    @Autowired
    private IRepository repository;

    @Autowired
    private ProjectUtil projectUtil;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private DataSourcesManager dataSourcesManager;

    @Test
    void a_lob_property_keeps_its_column_large() throws Exception {
        ClientJavaProjectDeployer.deploy(repository, projectUtil, synchronizationProcessor, PROJECT, PROJECT);

        // H2 keeps the declared CLOB, PostgreSQL renders it as its unbounded TEXT - either way a
        // character column far wider than the mapping's default length.
        Column text = column("NOTE_TEXT");
        assertThat(DataTypeUtils.isCharacterType(text.typeName())).as("NOTE_TEXT is %s", text)
                                                                  .isTrue();
        assertThat(text.size()).as("NOTE_TEXT is %s", text)
                               .isGreaterThan(MAPPING_DEFAULT_LENGTH);
    }

    private Column column(String name) throws Exception {
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                ResultSet columns = connection.getMetaData()
                                              .getColumns(null, connection.getSchema(), TABLE_NAME, name)) {
            assertThat(columns.next()).as("missing column [%s] - the table was not created at all", name)
                                      .isTrue();
            return new Column(columns.getString("TYPE_NAME"), columns.getInt("COLUMN_SIZE"));
        }
    }

    /** A column as the database reports it back, rendered into every assertion message. */
    private record Column(String typeName, int size) {

        @Override
        public String toString() {
            return typeName + "(" + size + ")";
        }
    }

    /**
     * The fixture files go away with the Dirigible folder the base class wipes per test class; the
     * table itself would survive a local run against an unclean target and mask the assertions.
     */
    @AfterEach
    void dropTable() throws Exception {
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS \"" + TABLE_NAME + "\"");
        }
    }
}
