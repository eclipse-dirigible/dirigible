/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.db;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import org.eclipse.dirigible.components.api.db.DatabaseFacade;
import org.eclipse.dirigible.components.database.DirigibleConnection;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.eclipse.dirigible.database.sql.SqlFactory;

/**
 * Primary entry point for relational-database access — both the JSON-shaped helpers used by
 * scripted callers and a raw {@link Connection} for code that prefers PreparedStatements.
 *
 * <p>
 * Two style choices:
 * <ul>
 * <li><strong>Pass-through SQL</strong> ({@link #query(String, String)},
 * {@link #update(String, String)}) — the simplest form; results come back as a JSON string. Good
 * for read-mostly endpoints and ad-hoc admin tools.</li>
 * <li><strong>JDBC</strong> ({@link #getConnection()}) — full PreparedStatement / ResultSet
 * control, transactions, type-safe parameter binding. Use this from Java controllers and anywhere
 * correctness matters more than terseness.</li>
 * </ul>
 *
 * <p>
 * {@code datasourceName} is optional on every method — when omitted, the default data source
 * applies. Pass the logical name of any {@code .datasource} artefact present in the registry to
 * target a non-default DB.
 *
 * <p>
 * Sequences ({@code nextval}, {@code createSequence}, {@code dropSequence}) work across H2,
 * PostgreSQL, Oracle, and MS SQL — the platform translates the call into the appropriate dialect.
 *
 * <p>
 * Users and schemas ({@code createUser}, {@code setUserPassword}, {@code dropUser},
 * {@code existsUser}, {@code createSchema}, {@code dropSchema}, {@code existsSchema}) are
 * translated the same way, which is the point of having them here: an application that manages
 * database principals — setting up the schemas and users it expects, or giving an isolated part of
 * itself a database identity of its own — would otherwise hand-write the DDL of one database and
 * stop working on the next. What "a user" means is not uniform, and the dialect absorbs that: on
 * SQL Server one is a server login plus a database user, so creating it emits two statements and
 * dropping it removes both.
 *
 * <p>
 * {@code existsUser} has no portable answer and says so rather than guessing — SQL standardises
 * {@code information_schema} for schemata but not for principals. It is implemented for PostgreSQL,
 * H2 and SQL Server; elsewhere it throws {@link java.sql.SQLFeatureNotSupportedException}.
 */
public final class Database {

    private Database() {}

    public static DirigibleConnection getConnection() throws Throwable {
        return DatabaseFacade.getConnection();
    }

    public static DirigibleConnection getConnection(String datasourceName) throws Throwable {
        return DatabaseFacade.getConnection(datasourceName);
    }

    public static DirigibleDataSource getDefaultDataSource() {
        return DatabaseFacade.getDefaultDataSource();
    }

    public static DirigibleDataSource getDataSource(String datasourceName) {
        return DatabaseFacade.getDataSource(datasourceName);
    }

    public static String getDataSources() {
        return DatabaseFacade.getDataSources();
    }

    public static String getMetadata() throws Throwable {
        return DatabaseFacade.getMetadata();
    }

    public static String getMetadata(String datasourceName) throws Throwable {
        return DatabaseFacade.getMetadata(datasourceName);
    }

    public static String getProductName() throws Throwable {
        return DatabaseFacade.getProductName();
    }

    public static String getProductName(String datasourceName) throws Throwable {
        return DatabaseFacade.getProductName(datasourceName);
    }

    public static String query(String sql) throws Throwable {
        return DatabaseFacade.query(sql);
    }

    public static String query(String sql, String parametersJson) throws Throwable {
        return DatabaseFacade.query(sql, parametersJson);
    }

    public static String query(String sql, String parametersJson, String datasourceName) throws Throwable {
        return DatabaseFacade.query(sql, parametersJson, datasourceName);
    }

    public static String queryNamed(String sql, String parametersJson) throws Throwable {
        return DatabaseFacade.queryNamed(sql, parametersJson);
    }

    public static String queryNamed(String sql, String parametersJson, String datasourceName) throws Throwable {
        return DatabaseFacade.queryNamed(sql, parametersJson, datasourceName);
    }

    public static int update(String sql) throws Throwable {
        return DatabaseFacade.update(sql);
    }

    public static int update(String sql, String parametersJson) throws Throwable {
        return DatabaseFacade.update(sql, parametersJson);
    }

    public static int update(String sql, String parametersJson, String datasourceName) throws Throwable {
        return DatabaseFacade.update(sql, parametersJson, datasourceName);
    }

    public static int updateNamed(String sql) throws Throwable {
        return DatabaseFacade.updateNamed(sql);
    }

    public static int updateNamed(String sql, String parametersJson) throws Throwable {
        return DatabaseFacade.updateNamed(sql, parametersJson);
    }

    public static int updateNamed(String sql, String parametersJson, String datasourceName) throws Throwable {
        return DatabaseFacade.updateNamed(sql, parametersJson, datasourceName);
    }

    public static List<Map<String, Object>> insert(String sql, String parametersJson, String datasourceName) throws Throwable {
        return DatabaseFacade.insert(sql, parametersJson, datasourceName);
    }

    public static List<Map<String, Object>> insertMany(String sql, String parametersJson, String datasourceName) throws Throwable {
        return DatabaseFacade.insertMany(sql, parametersJson, datasourceName);
    }

    public static List<Long> insertNamed(String sql, String parametersJson, String datasourceName) throws Throwable {
        return DatabaseFacade.insertNamed(sql, parametersJson, datasourceName);
    }

    public static long nextval(String sequence) throws Throwable {
        return DatabaseFacade.nextval(sequence);
    }

    public static long nextval(String sequence, String datasourceName) throws Throwable {
        return DatabaseFacade.nextval(sequence, datasourceName);
    }

    public static long nextval(String sequence, String datasourceName, String tableName) throws Throwable {
        return DatabaseFacade.nextval(sequence, datasourceName, tableName);
    }

    public static void createSequence(String sequence) throws Throwable {
        DatabaseFacade.createSequence(sequence);
    }

    public static void createSequence(String sequence, Integer start) throws Throwable {
        DatabaseFacade.createSequence(sequence, start);
    }

    public static void createSequence(String sequence, Integer start, String datasourceName) throws Throwable {
        DatabaseFacade.createSequence(sequence, start, datasourceName);
    }

    public static void dropSequence(String sequence) throws Throwable {
        DatabaseFacade.dropSequence(sequence);
    }

    public static void dropSequence(String sequence, String datasourceName) throws Throwable {
        DatabaseFacade.dropSequence(sequence, datasourceName);
    }

    /**
     * Creates a database user in the default data source.
     *
     * @param userId the user id
     * @param password the password
     * @throws Throwable the throwable
     */
    public static void createUser(String userId, String password) throws Throwable {
        DatabaseFacade.createUser(userId, password);
    }

    /**
     * Creates a database user.
     *
     * @param userId the user id
     * @param password the password
     * @param datasourceName the datasource name
     * @throws Throwable the throwable
     */
    public static void createUser(String userId, String password, String datasourceName) throws Throwable {
        DatabaseFacade.createUser(userId, password, datasourceName);
    }

    /**
     * Sets the password of an existing database user in the default data source.
     *
     * @param userId the user id
     * @param password the new password
     * @throws Throwable the throwable
     */
    public static void setUserPassword(String userId, String password) throws Throwable {
        DatabaseFacade.setUserPassword(userId, password);
    }

    /**
     * Sets the password of an existing database user.
     *
     * @param userId the user id
     * @param password the new password
     * @param datasourceName the datasource name
     * @throws Throwable the throwable
     */
    public static void setUserPassword(String userId, String password, String datasourceName) throws Throwable {
        DatabaseFacade.setUserPassword(userId, password, datasourceName);
    }

    /**
     * Drops a database user from the default data source.
     *
     * @param userId the user id
     * @throws Throwable the throwable
     */
    public static void dropUser(String userId) throws Throwable {
        DatabaseFacade.dropUser(userId);
    }

    /**
     * Drops a database user.
     *
     * @param userId the user id
     * @param datasourceName the datasource name
     * @throws Throwable the throwable
     */
    public static void dropUser(String userId, String datasourceName) throws Throwable {
        DatabaseFacade.dropUser(userId, datasourceName);
    }

    /**
     * Whether a database user exists in the default data source.
     *
     * @param userId the user id
     * @return true, if the user exists
     * @throws Throwable the throwable
     */
    public static boolean existsUser(String userId) throws Throwable {
        return DatabaseFacade.existsUser(userId);
    }

    /**
     * Whether a database user exists.
     *
     * @param userId the user id
     * @param datasourceName the datasource name
     * @return true, if the user exists
     * @throws Throwable the throwable
     */
    public static boolean existsUser(String userId, String datasourceName) throws Throwable {
        return DatabaseFacade.existsUser(userId, datasourceName);
    }

    /**
     * Creates a schema in the default data source.
     *
     * @param schema the schema
     * @param owner the user to own the schema, or null to leave ownership to the database
     * @throws Throwable the throwable
     */
    public static void createSchema(String schema, String owner) throws Throwable {
        DatabaseFacade.createSchema(schema, owner);
    }

    /**
     * Creates a schema.
     *
     * @param schema the schema
     * @param owner the user to own the schema, or null to leave ownership to the database
     * @param datasourceName the datasource name
     * @throws Throwable the throwable
     */
    public static void createSchema(String schema, String owner, String datasourceName) throws Throwable {
        DatabaseFacade.createSchema(schema, owner, datasourceName);
    }

    /**
     * Drops a schema from the default data source.
     *
     * @param schema the schema
     * @param cascade whether to drop the objects the schema contains as well
     * @throws Throwable the throwable
     */
    public static void dropSchema(String schema, boolean cascade) throws Throwable {
        DatabaseFacade.dropSchema(schema, cascade);
    }

    /**
     * Drops a schema.
     *
     * @param schema the schema
     * @param cascade whether to drop the objects the schema contains as well
     * @param datasourceName the datasource name
     * @throws Throwable the throwable
     */
    public static void dropSchema(String schema, boolean cascade, String datasourceName) throws Throwable {
        DatabaseFacade.dropSchema(schema, cascade, datasourceName);
    }

    /**
     * Whether a schema exists in the default data source.
     *
     * @param schema the schema
     * @return true, if the schema exists
     * @throws Throwable the throwable
     */
    public static boolean existsSchema(String schema) throws Throwable {
        return DatabaseFacade.existsSchema(schema);
    }

    /**
     * Whether a schema exists.
     *
     * @param schema the schema
     * @param datasourceName the datasource name
     * @return true, if the schema exists
     * @throws Throwable the throwable
     */
    public static boolean existsSchema(String schema, String datasourceName) throws Throwable {
        return DatabaseFacade.existsSchema(schema, datasourceName);
    }

    public static SqlFactory getDefaultSqlFactory() {
        return DatabaseFacade.getDefault();
    }

    public static SqlFactory getNativeSqlFactory(Connection connection) {
        return DatabaseFacade.getNative(connection);
    }
}
