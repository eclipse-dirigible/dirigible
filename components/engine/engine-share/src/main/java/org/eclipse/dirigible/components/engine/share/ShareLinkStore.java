/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.share;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.database.sql.DataType;
import org.eclipse.dirigible.database.sql.SqlFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Per-tenant share-link store (issue #8613): {@code DIRIGIBLE_SHARE_LINKS} and
 * {@code DIRIGIBLE_SHARE_LINK_OPENS} in the tenant-routed default datasource, so a tenant's links
 * live in its own schema, leave with it and cannot be read from another tenant's.
 *
 * <p>
 * A link row is written three ways and each writer touches only its own columns: the create inserts
 * it, a revoke stamps {@code REVOKED_BY} / {@code REVOKED_AT} once (a revoked link is never
 * un-revoked), and an open bumps {@code OPEN_COUNT} / {@code LAST_OPENED_AT} and appends one row to
 * the opens table. Nothing here is cached: whether a link answers is read on every request, which
 * is what makes a revoke immediate on every node.
 *
 * <p>
 * The tables are created by the first WRITE (a create or a revoke); a read before that has nothing
 * to find and issues no DDL, so the public route never creates a table.
 */
@Component
class ShareLinkStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShareLinkStore.class);

    private static final String LINKS_TABLE = "DIRIGIBLE_SHARE_LINKS";
    private static final String OPENS_TABLE = "DIRIGIBLE_SHARE_LINK_OPENS";

    private static final String LINK_COLUMNS = "\"SHARE_ID\", \"SHARE_ENTITY\", \"SHARE_RECORD\", \"SHARE_PATH\", \"SHARE_FILE_NAME\","
            + " \"SHARE_CONTENT_TYPE\", \"SHARE_CREATED_BY\", \"SHARE_CREATED_AT\", \"SHARE_EXPIRES_AT\", \"SHARE_REVOKED_BY\","
            + " \"SHARE_REVOKED_AT\", \"SHARE_OPEN_COUNT\", \"SHARE_LAST_OPENED_AT\"";

    private static final String SELECT_LINK = "SELECT " + LINK_COLUMNS + " FROM \"" + LINKS_TABLE + "\"";

    private static final int MAX_AGENT_LENGTH = 512;
    private static final int MAX_ADDRESS_LENGTH = 64;

    private final DataSourcesManager dataSourcesManager;

    ShareLinkStore(DataSourcesManager dataSourcesManager) {
        this.dataSourcesManager = dataSourcesManager;
    }

    /**
     * Inserts a link of the current tenant.
     *
     * @param tokenHash the hash of the link's token
     * @param entity the shared record's entity
     * @param recordId the shared record's key
     * @param storagePath the stored file the link serves
     * @param fileName the file name the reader receives
     * @param contentType the stored file's content type
     * @param createdBy who creates it
     * @param createdAt when
     * @param expiresAt when it stops answering, or {@code null}
     * @return the stored link
     * @throws SQLException if the insert fails
     */
    ShareLink insert(String tokenHash, String entity, String recordId, String storagePath, String fileName, String contentType,
            String createdBy, Instant createdAt, Instant expiresAt) throws SQLException {
        String id = UUID.randomUUID()
                        .toString();
        String sql = "INSERT INTO \"" + LINKS_TABLE + "\" (\"SHARE_ID\", \"SHARE_TOKEN_HASH\", \"SHARE_ENTITY\", \"SHARE_RECORD\","
                + " \"SHARE_PATH\", \"SHARE_FILE_NAME\", \"SHARE_CONTENT_TYPE\", \"SHARE_CREATED_BY\", \"SHARE_CREATED_AT\","
                + " \"SHARE_EXPIRES_AT\", \"SHARE_OPEN_COUNT\") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)";
        try (Connection connection = connection()) {
            ensureTablesExist(connection);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, id);
                statement.setString(2, tokenHash);
                statement.setString(3, entity);
                statement.setString(4, recordId);
                statement.setString(5, storagePath);
                statement.setString(6, fileName);
                statement.setString(7, contentType);
                statement.setString(8, createdBy);
                statement.setTimestamp(9, Timestamp.from(createdAt));
                statement.setTimestamp(10, expiresAt == null ? null : Timestamp.from(expiresAt));
                statement.executeUpdate();
            }
        }
        return new ShareLink(id, entity, recordId, storagePath, fileName, contentType, createdBy, createdAt, expiresAt, null, null, 0,
                null);
    }

    /**
     * The link of the current tenant a token hash names.
     *
     * @param tokenHash the hash of the presented token
     * @return the link, empty when this tenant has none with that hash
     * @throws SQLException if the read fails
     */
    Optional<ShareLink> findByTokenHash(String tokenHash) throws SQLException {
        try (Connection connection = connection()) {
            if (!linksTableExists(connection)) {
                return Optional.empty();
            }
            try (PreparedStatement statement = connection.prepareStatement(SELECT_LINK + " WHERE \"SHARE_TOKEN_HASH\" = ?")) {
                statement.setString(1, tokenHash);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? Optional.of(read(resultSet)) : Optional.empty();
                }
            }
        }
    }

    /**
     * Every link of one record, the newest first.
     *
     * @param entity the record's entity
     * @param recordId the record's key
     * @return the links
     * @throws SQLException if the read fails
     */
    List<ShareLink> findByRecord(String entity, String recordId) throws SQLException {
        try (Connection connection = connection()) {
            if (!linksTableExists(connection)) {
                return List.of();
            }
            String sql = SELECT_LINK + " WHERE \"SHARE_ENTITY\" = ? AND \"SHARE_RECORD\" = ? ORDER BY \"SHARE_CREATED_AT\" DESC";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, entity);
                statement.setString(2, recordId);
                List<ShareLink> links = new ArrayList<>();
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        links.add(read(resultSet));
                    }
                }
                return links;
            }
        }
    }

    /**
     * Revokes the links of one record that are not revoked yet - one of them when {@code linkId} names
     * it, all of them otherwise. The record is part of the predicate either way, so a link id cannot
     * revoke a link of another record.
     *
     * @param entity the record's entity
     * @param recordId the record's key
     * @param linkId the one link to revoke, or {@code null} for every link of the record
     * @param revokedBy who revokes
     * @param revokedAt when
     * @return how many links were revoked
     * @throws SQLException if the update fails
     */
    int revoke(String entity, String recordId, String linkId, String revokedBy, Instant revokedAt) throws SQLException {
        try (Connection connection = connection()) {
            if (!linksTableExists(connection)) {
                return 0;
            }
            String sql = "UPDATE \"" + LINKS_TABLE + "\" SET \"SHARE_REVOKED_BY\" = ?, \"SHARE_REVOKED_AT\" = ?"
                    + " WHERE \"SHARE_ENTITY\" = ? AND \"SHARE_RECORD\" = ? AND \"SHARE_REVOKED_AT\" IS NULL"
                    + (linkId == null ? "" : " AND \"SHARE_ID\" = ?");
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, revokedBy);
                statement.setTimestamp(2, Timestamp.from(revokedAt));
                statement.setString(3, entity);
                statement.setString(4, recordId);
                if (linkId != null) {
                    statement.setString(5, linkId);
                }
                return statement.executeUpdate();
            }
        }
    }

    /**
     * Records one open of a link: the counter and last-opened stamp on the link, and a row of its own
     * with who asked.
     *
     * @param linkId the opened link
     * @param openedAt when
     * @param address the remote address
     * @param userAgent the reader's user agent, as sent
     * @throws SQLException if the write fails
     */
    void recordOpen(String linkId, Instant openedAt, String address, String userAgent) throws SQLException {
        String bump = "UPDATE \"" + LINKS_TABLE + "\" SET \"SHARE_OPEN_COUNT\" = \"SHARE_OPEN_COUNT\" + 1, \"SHARE_LAST_OPENED_AT\" = ?"
                + " WHERE \"SHARE_ID\" = ?";
        String append = "INSERT INTO \"" + OPENS_TABLE + "\" (\"OPEN_ID\", \"OPEN_SHARE_ID\", \"OPEN_AT\", \"OPEN_ADDRESS\","
                + " \"OPEN_USER_AGENT\") VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = connection()) {
            try (PreparedStatement statement = connection.prepareStatement(bump)) {
                statement.setTimestamp(1, Timestamp.from(openedAt));
                statement.setString(2, linkId);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(append)) {
                statement.setString(1, UUID.randomUUID()
                                           .toString());
                statement.setString(2, linkId);
                statement.setTimestamp(3, Timestamp.from(openedAt));
                statement.setString(4, truncate(address, MAX_ADDRESS_LENGTH));
                statement.setString(5, truncate(userAgent, MAX_AGENT_LENGTH));
                statement.executeUpdate();
            }
        }
    }

    private Connection connection() throws SQLException {
        return dataSourcesManager.getDefaultDataSource()
                                 .getConnection();
    }

    private static ShareLink read(ResultSet resultSet) throws SQLException {
        return new ShareLink(resultSet.getString("SHARE_ID"), resultSet.getString("SHARE_ENTITY"), resultSet.getString("SHARE_RECORD"),
                resultSet.getString("SHARE_PATH"), resultSet.getString("SHARE_FILE_NAME"), resultSet.getString("SHARE_CONTENT_TYPE"),
                resultSet.getString("SHARE_CREATED_BY"), instant(resultSet, "SHARE_CREATED_AT"), instant(resultSet, "SHARE_EXPIRES_AT"),
                resultSet.getString("SHARE_REVOKED_BY"), instant(resultSet, "SHARE_REVOKED_AT"), resultSet.getLong("SHARE_OPEN_COUNT"),
                instant(resultSet, "SHARE_LAST_OPENED_AT"));
    }

    private static Instant instant(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private static String truncate(String value, int length) {
        return value == null || value.length() <= length ? value : value.substring(0, length);
    }

    private static boolean linksTableExists(Connection connection) throws SQLException {
        return SqlFactory.getNative(connection)
                         .existsTable(connection, LINKS_TABLE);
    }

    private void ensureTablesExist(Connection connection) throws SQLException {
        if (!linksTableExists(connection)) {
            create(connection, LINKS_TABLE, SqlFactory.getNative(connection)
                                                      .create()
                                                      .table(quoted(LINKS_TABLE))
                                                      .column("\"SHARE_ID\"", DataType.VARCHAR, true, false, false, "(36)")
                                                      .column("\"SHARE_TOKEN_HASH\"", DataType.VARCHAR, false, false, true, "(64)")
                                                      .column("\"SHARE_ENTITY\"", DataType.VARCHAR, false, false, false, "(255)")
                                                      .column("\"SHARE_RECORD\"", DataType.VARCHAR, false, false, false, "(255)")
                                                      .column("\"SHARE_PATH\"", DataType.VARCHAR, false, false, false, "(1000)")
                                                      .column("\"SHARE_FILE_NAME\"", DataType.VARCHAR, false, true, false, "(255)")
                                                      .column("\"SHARE_CONTENT_TYPE\"", DataType.VARCHAR, false, true, false, "(255)")
                                                      .column("\"SHARE_CREATED_BY\"", DataType.VARCHAR, false, true, false, "(255)")
                                                      .column("\"SHARE_CREATED_AT\"", DataType.TIMESTAMP, false, false, false)
                                                      .column("\"SHARE_EXPIRES_AT\"", DataType.TIMESTAMP, false, true, false)
                                                      .column("\"SHARE_REVOKED_BY\"", DataType.VARCHAR, false, true, false, "(255)")
                                                      .column("\"SHARE_REVOKED_AT\"", DataType.TIMESTAMP, false, true, false)
                                                      .column("\"SHARE_OPEN_COUNT\"", DataType.BIGINT, false, false, false)
                                                      .column("\"SHARE_LAST_OPENED_AT\"", DataType.TIMESTAMP, false, true, false)
                                                      .build());
        }
        if (!SqlFactory.getNative(connection)
                       .existsTable(connection, OPENS_TABLE)) {
            create(connection, OPENS_TABLE, SqlFactory.getNative(connection)
                                                      .create()
                                                      .table(quoted(OPENS_TABLE))
                                                      .column("\"OPEN_ID\"", DataType.VARCHAR, true, false, false, "(36)")
                                                      .column("\"OPEN_SHARE_ID\"", DataType.VARCHAR, false, false, false, "(36)")
                                                      .column("\"OPEN_AT\"", DataType.TIMESTAMP, false, false, false)
                                                      .column("\"OPEN_ADDRESS\"", DataType.VARCHAR, false, true, false, "(64)")
                                                      .column("\"OPEN_USER_AGENT\"", DataType.VARCHAR, false, true, false, "(512)")
                                                      .build());
        }
    }

    private static void create(Connection connection, String table, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
            LOGGER.info("Created per-tenant share-link table using sql [{}]", sql);
        } catch (SQLException ex) {
            if (SqlFactory.getNative(connection)
                          .existsTable(connection, table)) {
                LOGGER.debug("Share-link table [{}] already created concurrently", table, ex);
            } else {
                throw ex;
            }
        }
    }

    private static String quoted(String name) {
        return "\"" + name + "\"";
    }
}
