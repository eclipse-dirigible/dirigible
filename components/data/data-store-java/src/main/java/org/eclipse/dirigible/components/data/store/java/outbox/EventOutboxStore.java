/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.store.java.outbox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.eclipse.dirigible.database.sql.DataType;
import org.eclipse.dirigible.database.sql.SqlFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Persistence for the event outbox. Like the per-tenant configuration table, every operation runs
 * against the default datasource, which inside a tenant execution scope is routed to that tenant's
 * schema — so a distinct DIRIGIBLE_EVENT_OUTBOX table lives in every tenant schema and no tenant
 * column is needed. The table is created on first use (create-if-absent).
 *
 * <p>
 * The insert deliberately takes a caller-supplied {@link Connection}: it is the connection of the
 * Hibernate session that is writing the entity row, so the event and the row share one transaction.
 * Every other operation opens its own connection.
 */
@Component
class EventOutboxStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventOutboxStore.class);

    /** Unquoted table name — used for metadata existence checks and by the DML builders. */
    static final String TABLE_NAME = "DIRIGIBLE_EVENT_OUTBOX";

    static final String COLUMN_ID = "EVENT_ID";
    static final String COLUMN_TOPIC = "EVENT_TOPIC";
    static final String COLUMN_PAYLOAD = "EVENT_PAYLOAD";
    static final String COLUMN_CREATED_AT = "EVENT_CREATED_AT";
    static final String COLUMN_ATTEMPTS = "EVENT_ATTEMPTS";
    static final String COLUMN_NEXT_ATTEMPT_AT = "EVENT_NEXT_ATTEMPT_AT";
    static final String COLUMN_ERROR = "EVENT_ERROR";

    /** Quoted identifiers — used for DDL so the identifiers stay case-exact across dialects. */
    private static final String QUOTED_TABLE = quoted(TABLE_NAME);

    /**
     * Tenants whose outbox table is known to exist. The check is a database-metadata round trip, so it
     * is paid once per tenant per node rather than on every write.
     */
    private final Set<String> preparedTenants = ConcurrentHashMap.newKeySet();

    private final DataSourcesManager dataSourcesManager;

    private final TenantContext tenantContext;

    EventOutboxStore(DataSourcesManager dataSourcesManager, TenantContext tenantContext) {
        this.dataSourcesManager = dataSourcesManager;
        this.tenantContext = tenantContext;
    }

    /**
     * Makes sure the current tenant's outbox table exists, on a connection of its own so that no DDL
     * runs inside the business transaction the event will be recorded in.
     *
     * @throws SQLException if the table is missing and cannot be created
     */
    void prepare() throws SQLException {
        String tenant = currentTenantKey();
        if (preparedTenants.contains(tenant)) {
            return;
        }
        try (Connection connection = connection()) {
            createTableIfAbsent(connection);
        }
        preparedTenants.add(tenant);
    }

    /**
     * Records one event on the given connection — the connection of the transaction that is writing the
     * entity row.
     *
     * @param connection the transaction's connection
     * @param event the event to record
     * @param nextAttemptAt when the relay may first take the entry over; the in-process dispatch that
     *        follows the commit owns it until then
     * @throws SQLException if the insert fails, which must fail the enclosing transaction
     */
    void insert(Connection connection, PendingEvent event, Instant nextAttemptAt) throws SQLException {
        String sql = SqlFactory.getNative(connection)
                               .insert()
                               .into(TABLE_NAME)
                               .column(COLUMN_ID)
                               .column(COLUMN_TOPIC)
                               .column(COLUMN_PAYLOAD)
                               .column(COLUMN_CREATED_AT)
                               .column(COLUMN_ATTEMPTS)
                               .column(COLUMN_NEXT_ATTEMPT_AT)
                               .build();
        Timestamp now = Timestamp.from(Instant.now());
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, event.id());
            statement.setString(2, event.topic());
            statement.setString(3, event.payload());
            statement.setTimestamp(4, now);
            statement.setInt(5, event.attempts());
            statement.setTimestamp(6, Timestamp.from(nextAttemptAt));
            statement.executeUpdate();
        }
    }

    /**
     * Records one event on a connection of its own — the standalone variant for an announcement that is
     * deliberately decoupled from the write it is about (a deferred publish after a synchronous chain's
     * commit), where there is no enclosing transaction to join.
     *
     * @param event the event to record
     * @param nextAttemptAt when the relay may first take the entry over
     * @throws SQLException if the insert fails
     */
    void insert(PendingEvent event, Instant nextAttemptAt) throws SQLException {
        try (Connection connection = connection()) {
            insert(connection, event, nextAttemptAt);
        }
    }

    /**
     * @return true when the current tenant has an outbox table at all — a tenant that never wrote an
     *         entity has none, and the relay must not fail on it
     * @throws SQLException if the metadata lookup fails
     */
    boolean tableExists() throws SQLException {
        if (preparedTenants.contains(currentTenantKey())) {
            return true;
        }
        try (Connection connection = connection()) {
            return SqlFactory.getNative(connection)
                             .existsTable(connection, TABLE_NAME);
        }
    }

    /**
     * Reads the entries of the current tenant that are due for another delivery attempt, oldest first.
     *
     * @param cutoff entries whose next attempt is at or before this moment are due
     * @param limit the maximum number of entries to read
     * @return the due entries, never {@code null}
     * @throws SQLException if the read fails
     */
    List<PendingEvent> findDue(Instant cutoff, int limit) throws SQLException {
        try (Connection connection = connection()) {
            String sql = SqlFactory.getNative(connection)
                                   .select()
                                   .column("*")
                                   .from(TABLE_NAME)
                                   .where(COLUMN_NEXT_ATTEMPT_AT + " <= ?")
                                   .order(COLUMN_CREATED_AT)
                                   .limit(limit)
                                   .build();
            List<PendingEvent> due = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setTimestamp(1, Timestamp.from(cutoff));
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        due.add(new PendingEvent(resultSet.getString(COLUMN_ID), resultSet.getString(COLUMN_TOPIC),
                                resultSet.getString(COLUMN_PAYLOAD), resultSet.getInt(COLUMN_ATTEMPTS)));
                    }
                }
            }
            return due;
        }
    }

    /**
     * Claims an entry for a delivery attempt: bumps its attempt counter and pushes its next attempt
     * beyond the grace period. The attempt counter doubles as the optimistic lock, so two relays racing
     * over the same entry cannot both win it.
     *
     * @param event the entry to claim, carrying the attempt count it was read with
     * @param nextAttemptAt when the entry becomes due again if this attempt fails
     * @return true if this caller won the claim
     * @throws SQLException if the update fails
     */
    boolean claim(PendingEvent event, Instant nextAttemptAt) throws SQLException {
        try (Connection connection = connection()) {
            String sql = SqlFactory.getNative(connection)
                                   .update()
                                   .table(TABLE_NAME)
                                   .set(COLUMN_ATTEMPTS, "?")
                                   .set(COLUMN_NEXT_ATTEMPT_AT, "?")
                                   .where(COLUMN_ID + " = ?")
                                   .where(COLUMN_ATTEMPTS + " = ?")
                                   .build();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, event.attempts() + 1);
                statement.setTimestamp(2, Timestamp.from(nextAttemptAt));
                statement.setString(3, event.id());
                statement.setInt(4, event.attempts());
                return statement.executeUpdate() == 1;
            }
        }
    }

    /**
     * Drops a delivered entry.
     *
     * @param id the entry identifier
     * @throws SQLException if the delete fails
     */
    void delete(String id) throws SQLException {
        try (Connection connection = connection()) {
            String sql = SqlFactory.getNative(connection)
                                   .delete()
                                   .from(TABLE_NAME)
                                   .where(COLUMN_ID + " = ?")
                                   .build();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, id);
                statement.executeUpdate();
            }
        }
    }

    /**
     * Records why the last delivery attempt failed, so an operator reading the table can tell.
     *
     * @param id the entry identifier
     * @param error the failure description; truncated to the column width
     * @throws SQLException if the update fails
     */
    void recordError(String id, String error) throws SQLException {
        try (Connection connection = connection()) {
            String sql = SqlFactory.getNative(connection)
                                   .update()
                                   .table(TABLE_NAME)
                                   .set(COLUMN_ERROR, "?")
                                   .where(COLUMN_ID + " = ?")
                                   .build();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, error == null ? null : error.substring(0, Math.min(error.length(), 2000)));
                statement.setString(2, id);
                statement.executeUpdate();
            }
        }
    }

    private Connection connection() throws SQLException {
        DirigibleDataSource dataSource = dataSourcesManager.getDefaultDataSource();
        return dataSource.getConnection();
    }

    private String currentTenantKey() {
        return tenantContext.isNotInitialized() ? ""
                : tenantContext.getCurrentTenant()
                               .getId();
    }

    private void createTableIfAbsent(Connection connection) throws SQLException {
        if (SqlFactory.getNative(connection)
                      .existsTable(connection, TABLE_NAME)) {
            return;
        }
        String sql = SqlFactory.getNative(connection)
                               .create()
                               .table(QUOTED_TABLE)
                               .column(quoted(COLUMN_ID), DataType.VARCHAR, true, false, false, "(36)")
                               .column(quoted(COLUMN_TOPIC), DataType.VARCHAR, false, false, false, "(255)")
                               .column(quoted(COLUMN_PAYLOAD), DataType.CLOB, false, true, false)
                               .column(quoted(COLUMN_CREATED_AT), DataType.TIMESTAMP, false, false, false)
                               .column(quoted(COLUMN_ATTEMPTS), DataType.INTEGER, false, false, false)
                               .column(quoted(COLUMN_NEXT_ATTEMPT_AT), DataType.TIMESTAMP, false, false, false)
                               .column(quoted(COLUMN_ERROR), DataType.VARCHAR, false, true, false, "(2000)")
                               .build();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
            LOGGER.info("Created the event outbox table using sql [{}]", sql);
        } catch (SQLException ex) {
            // Another concurrent write may have created the table in the meantime; tolerate it.
            if (SqlFactory.getNative(connection)
                          .existsTable(connection, TABLE_NAME)) {
                LOGGER.debug("Event outbox table already exists after a concurrent creation.", ex);
                return;
            }
            throw ex;
        }
    }

    private static String quoted(String identifier) {
        return "\"" + identifier + "\"";
    }

}
