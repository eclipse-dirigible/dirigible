/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.db;

import org.apache.commons.io.output.WriterOutputStream;
import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.components.base.logging.LoggingExecutor;
import org.eclipse.dirigible.components.data.management.helpers.DatabaseMetadataHelper;
import org.eclipse.dirigible.components.data.management.helpers.DatabaseResultSetHelper;
import org.eclipse.dirigible.components.data.management.helpers.ResultParameters;
import org.eclipse.dirigible.components.data.management.service.DatabaseDefinitionService;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.database.DatabaseParameters;
import org.eclipse.dirigible.components.database.DirigibleConnection;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.eclipse.dirigible.components.database.NamedParameterStatement;
import org.eclipse.dirigible.database.persistence.processors.identity.PersistenceNextValueIdentityProcessor;
import org.eclipse.dirigible.database.sql.SqlFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

import static java.text.MessageFormat.format;

/**
 * The Class DatabaseFacade.
 */
@Component
public class DatabaseFacade implements InitializingBean {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(DatabaseFacade.class);

    /** The database facade. */
    private static DatabaseFacade INSTANCE;

    /** The database definition service. */
    private final DatabaseDefinitionService databaseDefinitionService;

    /** The data sources manager. */
    private final DataSourcesManager dataSourcesManager;

    /**
     * Instantiates a new database facade.
     *
     * @param databaseDefinitionService the database definition service
     * @param dataSourcesManager the data sources manager
     */
    @Autowired
    private DatabaseFacade(DatabaseDefinitionService databaseDefinitionService, DataSourcesManager dataSourcesManager) {
        this.databaseDefinitionService = databaseDefinitionService;
        this.dataSourcesManager = dataSourcesManager;
    }

    /**
     * After properties set.
     */
    @Override
    public void afterPropertiesSet() {
        INSTANCE = this;
    }

    /**
     * Gets the data sources.
     *
     * @return the data sources
     */
    public static String getDataSources() {
        return GsonHelper.toJson(DatabaseFacade.get()
                                               .getDatabaseDefinitionService()
                                               .getDataSourcesNames());
    }

    /**
     * Gets the instance.
     *
     * @return the database facade
     */
    public static DatabaseFacade get() {
        return INSTANCE;
    }

    /**
     * Gets the database definition service.
     *
     * @return the database definition service
     */
    public DatabaseDefinitionService getDatabaseDefinitionService() {
        return databaseDefinitionService;
    }

    /**
     * Gets the default data source.
     *
     * @return the default data source
     */
    public static DirigibleDataSource getDefaultDataSource() {
        return DatabaseFacade.get()
                             .getDataSourcesManager()
                             .getDefaultDataSource();
    }

    /**
     * Gets the data sources manager.
     *
     * @return the data sources manager
     */
    public DataSourcesManager getDataSourcesManager() {
        return dataSourcesManager;
    }

    /**
     * Gets the metadata.
     *
     * @param datasourceName the datasource name
     * @return the metadata
     * @throws SQLException the SQL exception
     */
    public static String getMetadata(String datasourceName) throws Throwable {
        DataSource dataSource = getDataSource(datasourceName);
        if (dataSource == null) {
            String error = format("DataSource {0} not known.", datasourceName);
            throw new IllegalArgumentException(error);
        }
        return LoggingExecutor.executeWithException(dataSource, () -> DatabaseMetadataHelper.getMetadataAsJson(dataSource));
    }

    /**
     * Gets the data source.
     *
     * @param datasourceName the datasource name
     * @return the data source
     */
    private static DirigibleDataSource getDataSource(String datasourceName) {
        return datasourceName == null || datasourceName.trim()
                                                       .isEmpty()
                || "DefaultDB".equals(datasourceName)
                        ? DatabaseFacade.get()
                                        .getDataSourcesManager()
                                        .getDefaultDataSource()
                        : DatabaseFacade.get()
                                        .getDataSourcesManager()
                                        .getDataSource(datasourceName);
    }

    /**
     * Gets the metadata.
     *
     * @return the metadata
     * @throws SQLException the SQL exception
     */
    public static String getMetadata() throws Throwable {
        DataSource dataSource = getDataSource(null);
        return LoggingExecutor.executeWithException(dataSource, () -> {
            if (dataSource == null) {
                String error = format("No default DataSource has been configured.");
                throw new IllegalArgumentException(error);
            }
            return DatabaseMetadataHelper.getMetadataAsJson(dataSource);
        });
    }

    /**
     * Gets the product name of the database.
     *
     * @param datasourceName the datasource name
     * @return the product name
     * @throws SQLException the SQL exception
     */
    public static String getProductName(String datasourceName) throws Throwable {
        DataSource dataSource = getDataSource(datasourceName);
        if (dataSource == null) {
            String error = format("DataSource {0} not known.", datasourceName);
            throw new IllegalArgumentException(error);
        }
        return LoggingExecutor.executeWithException(dataSource, () -> DatabaseMetadataHelper.getProductName(dataSource));
    }

    /**
     * Gets the product name of the database.
     *
     * @return the product name
     * @throws SQLException the SQL exception
     */
    public static String getProductName() throws Throwable {
        DataSource dataSource = getDataSource(null);
        return LoggingExecutor.executeWithException(dataSource, () -> {
            if (dataSource == null) {
                String error = format("No default DataSource has been configured.");
                throw new IllegalArgumentException(error);
            }
            return DatabaseMetadataHelper.getProductName(dataSource);
        });
    }

    /**
     * Executes SQL query.
     *
     * @param sql the sql
     * @param parameters the parameters
     * @return the result of the query as JSON
     * @throws Exception the exception
     */
    public static String query(String sql, String parameters) throws Throwable {
        return query(sql, parameters, null);
    }

    // ============ Query ===========

    /**
     * Executes SQL query.
     *
     * @param sql the sql
     * @param parameters the parameters
     * @param datasourceName the datasource name
     * @return the result of the query as JSON
     * @throws Exception the exception
     */
    public static String query(String sql, String parameters, String datasourceName) throws Throwable {
        return query(sql, parameters, datasourceName, null);
    }

    public static String query(String sql, String parameters, String datasourceName, String resultParametersJson) throws Throwable {
        ResultParameters resultParameters =
                null == resultParametersJson ? null : GsonHelper.fromJson(resultParametersJson, ResultParameters.class);

        DataSource dataSource = getDataSource(datasourceName);
        return LoggingExecutor.executeWithException(dataSource, () -> {
            if (dataSource == null) {
                String error = format("DataSource {0} not known.", datasourceName);
                throw new IllegalArgumentException(error);
            }
            try (Connection connection = dataSource.getConnection()) {
                try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                    if (parameters != null) {
                        IndexedOrNamedStatement statement = new IndexedOrNamedStatement(preparedStatement);
                        ParametersSetter.setParameters(parameters, statement);
                    }
                    ResultSet resultSet = preparedStatement.executeQuery();
                    StringWriter sw = new StringWriter();
                    OutputStream output;
                    try {
                        output = WriterOutputStream.builder()
                                                   .setWriter(sw)
                                                   .setCharset(StandardCharsets.UTF_8)
                                                   .get();
                    } catch (IOException e) {
                        throw new Exception(e);
                    }
                    DatabaseResultSetHelper.toJson(resultSet, false, false, output, Optional.ofNullable(resultParameters));
                    return sw.toString();
                }
            } catch (Exception ex) {
                logger.error("Failed to execute query statement [{}] in data source [{}].", sql, datasourceName, ex);
                throw ex;
            }
        });
    }

    /**
     * Executes SQL query.
     *
     * @param sql the sql
     * @return the result of the query as JSON
     * @throws Exception the exception
     */
    public static String query(String sql) throws Throwable {
        return query(sql, null, null);
    }

    /**
     * Executes named parameters SQL query.
     *
     * @param sql the sql
     * @param parameters the parameters
     * @return the result of the query as JSON
     * @throws Exception the exception
     */
    public static String queryNamed(String sql, String parameters) throws Throwable {
        return queryNamed(sql, parameters, null);
    }

    /**
     * Executes named parameters SQL query.
     *
     * @param sql the sql
     * @param parameters the parameters
     * @param datasourceName the datasource name
     * @return the result of the query as JSON
     * @throws Exception the exception
     */
    public static String queryNamed(String sql, String parameters, String datasourceName) throws Throwable {
        DataSource dataSource = getDataSource(datasourceName);
        if (dataSource == null) {
            String error = format("DataSource {0} not known.", datasourceName);
            throw new IllegalArgumentException(error);
        }
        return LoggingExecutor.executeWithException(dataSource, () -> {
            try (Connection connection = dataSource.getConnection()) {
                try (NamedParameterStatement preparedStatement = new NamedParameterStatement(connection, sql)) {
                    if (parameters != null) {
                        IndexedOrNamedStatement statement = new IndexedOrNamedStatement(preparedStatement);
                        ParametersSetter.setParameters(parameters, statement);
                    }
                    ResultSet resultSet = preparedStatement.executeQuery();
                    StringWriter sw = new StringWriter();
                    OutputStream output;
                    try {
                        output = WriterOutputStream.builder()
                                                   .setWriter(sw)
                                                   .setCharset(StandardCharsets.UTF_8)
                                                   .get();
                    } catch (IOException e) {
                        throw new Exception(e);
                    }
                    DatabaseResultSetHelper.toJson(resultSet, false, false, output);
                    return sw.toString();
                }
            } catch (Exception ex) {
                logger.error("Failed to execute query statement [{}] in data source [{}].", sql, datasourceName, ex);
                throw ex;
            }
        });
    }

    /**
     * Executes named parameters SQL query.
     *
     * @param sql the sql
     * @return the result of the query as JSON
     * @throws Exception the exception
     */
    public static String queryNamed(String sql) throws Throwable {
        return queryNamed(sql, null, null);
    }

    /**
     * Executes SQL insert.
     *
     * @param sql the insert statement to be executed
     * @param parameters statement parameters
     * @param datasourceName the datasource name
     * @return the generated IDs
     * @throws SQLException if an error occur
     * @throws IllegalArgumentException if the provided datasouce is not found
     * @throws RuntimeException if an error occur
     */
    public static List<Map<String, Object>> insert(String sql, String parameters, String datasourceName) throws Throwable {
        DataSource dataSource = getDataSource(datasourceName);
        if (dataSource == null) {
            throw new IllegalArgumentException("DataSource [" + datasourceName + "] not known.");
        }

        return LoggingExecutor.executeWithException(dataSource, () -> {

            try (Connection connection = dataSource.getConnection()) {
                try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                    if (parameters != null) {
                        IndexedOrNamedStatement statement = new IndexedOrNamedStatement(preparedStatement);
                        ParametersSetter.setParameters(parameters, statement);
                    }

                    int updatedRows = preparedStatement.executeUpdate();
                    List<Map<String, Object>> generatedKeysList = new ArrayList<>(updatedRows);

                    try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                        ResultSetMetaData metaData = generatedKeys.getMetaData();
                        int columnCount = metaData.getColumnCount();

                        while (generatedKeys.next()) {
                            Map<String, Object> keyRow = new LinkedHashMap<>();
                            for (int i = 1; i <= columnCount; i++) {
                                String columnName = metaData.getColumnLabel(i);
                                Object value = generatedKeys.getObject(i);
                                keyRow.put(columnName, value);
                            }
                            generatedKeysList.add(keyRow);
                        }

                        return generatedKeysList;
                    }
                } catch (SQLFeatureNotSupportedException ex) {
                    logger.debug("RETURN_GENERATED_KEYS not supported for connection [{}]. Will execute insert without this option.",
                            connection, ex);
                    insertWithoutResult(sql, parameters, connection);
                    return Collections.emptyList();
                }
            } catch (SQLException | RuntimeException ex) {
                logger.error("Failed to execute insert statement [{}] in data source [{}].", sql, datasourceName, ex);
                throw ex;
            }

        });
    }

    private static void insertWithoutResult(String sql, String parameters, Connection connection) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            if (parameters != null) {
                IndexedOrNamedStatement statement = new IndexedOrNamedStatement(preparedStatement);
                ParametersSetter.setParameters(parameters, statement);
            }
            preparedStatement.executeUpdate();
        }
    }

    // =========== Insert ===========

    /**
     * Executes named SQL insert.
     *
     * @param sql the insert statement to be executed
     * @param parameters statement parameters
     * @param datasourceName the datasource name
     * @return the generated IDs
     * @throws SQLException if an error occur
     * @throws IllegalArgumentException if the provided datasouce is not found
     * @throws RuntimeException if an error occur
     */
    public static List<Long> insertNamed(String sql, String parameters, String datasourceName) throws Throwable {
        DataSource dataSource = getDataSource(datasourceName);
        if (dataSource == null) {
            throw new IllegalArgumentException("DataSource [" + datasourceName + "] not known.");
        }
        return LoggingExecutor.executeWithException(dataSource, () -> {
            try (Connection connection = dataSource.getConnection();
                    NamedParameterStatement preparedStatement =
                            new NamedParameterStatement(connection, sql, Statement.RETURN_GENERATED_KEYS)) {

                if (parameters != null) {
                    IndexedOrNamedStatement statement = new IndexedOrNamedStatement(preparedStatement);
                    ParametersSetter.setParameters(parameters, statement);
                }
                int updatedRows = preparedStatement.executeUpdate();
                List<Long> generatedIds = new ArrayList<>(updatedRows);
                try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                    while (generatedKeys.next()) {
                        generatedIds.add(generatedKeys.getLong(1));
                    }
                    return generatedIds;
                }
            } catch (SQLException | RuntimeException ex) {
                logger.error("Failed to execute insert statement [{}] in data source [{}].", sql, datasourceName, ex);
                throw ex;
            }
        });

    }

    /**
     * Executes SQL update.
     *
     * @param sql the sql
     * @param parameters the parameters
     * @return the number of the rows that has been changed
     * @throws Exception the exception
     */
    public static int update(String sql, String parameters) throws Throwable {
        return update(sql, parameters, null);
    }

    // =========== Update ===========

    /**
     * Executes SQL update.
     *
     * @param sql the sql
     * @param parameters the parameters
     * @param datasourceName the datasource name
     * @return the number of the rows that has been changed
     * @throws Exception the exception
     */
    public static int update(String sql, String parameters, String datasourceName) throws Throwable {
        DataSource dataSource = getDataSource(datasourceName);
        if (dataSource == null) {
            String error = format("DataSource {0} not known.", datasourceName);
            throw new IllegalArgumentException(error);
        }

        return LoggingExecutor.executeWithException(dataSource, () -> {
            try (Connection connection = dataSource.getConnection()) {
                try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                    if (parameters != null) {
                        IndexedOrNamedStatement statement = new IndexedOrNamedStatement(preparedStatement);
                        ParametersSetter.setParameters(parameters, statement);
                    }
                    return preparedStatement.executeUpdate();
                }
            } catch (Exception ex) {
                logger.error("Failed to execute update statement [{}] in data source [{}].", sql, datasourceName, ex);
                throw ex;
            }
        });

    }

    /**
     * Executes SQL update.
     *
     * @param sql the sql
     * @return the number of the rows that has been changed
     * @throws Exception the exception
     */
    public static int update(String sql) throws Throwable {
        return update(sql, null, null);
    }

    /**
     * Executes named SQL update.
     *
     * @param sql the sql
     * @param parameters the parameters
     * @param datasourceName the datasource name
     * @return the number of the rows that has been changed
     * @throws Exception the exception
     */
    public static int updateNamed(String sql, String parameters, String datasourceName) throws Throwable {
        DataSource dataSource = getDataSource(datasourceName);
        if (dataSource == null) {
            String error = format("DataSource {0} not known.", datasourceName);
            throw new IllegalArgumentException(error);
        }

        return LoggingExecutor.executeWithException(dataSource, () -> {
            try (Connection connection = dataSource.getConnection()) {
                try (NamedParameterStatement preparedStatement = new NamedParameterStatement(connection, sql)) {
                    if (parameters != null) {
                        IndexedOrNamedStatement statement = new IndexedOrNamedStatement(preparedStatement);
                        ParametersSetter.setParameters(parameters, statement);
                    }
                    return preparedStatement.executeUpdate();
                }
            } catch (Exception ex) {
                logger.error("Failed to execute update statement [{}] in data source [{}].", sql, datasourceName, ex);
                throw ex;
            }
        });

    }

    /**
     * Executes named SQL update.
     *
     * @param sql the sql
     * @param parameters the parameters
     * @return the number of the rows that has been changed
     * @throws Exception the exception
     */
    public static int updateNamed(String sql, String parameters) throws Throwable {
        return update(sql, parameters, null);
    }

    /**
     * Executes named SQL update.
     *
     * @param sql the sql
     * @return the number of the rows that has been changed
     * @throws Exception the exception
     */
    public static int updateNamed(String sql) throws Throwable {
        return update(sql, null, null);
    }

    /**
     * Gets the connection.
     *
     * @return the connection
     * @throws SQLException the SQL exception
     */
    public static DirigibleConnection getConnection() throws Throwable {
        return getConnection(null);
    }

    /**
     * Gets the connection.
     *
     * @param datasourceName the datasource name
     * @return the connection
     * @throws SQLException the SQL exception
     */
    public static DirigibleConnection getConnection(String datasourceName) throws Throwable {
        DirigibleDataSource dataSource = getDataSource(datasourceName);
        if (dataSource == null) {
            String error = format("DataSource {0} not known.", datasourceName);
            throw new IllegalArgumentException(error);
        }
        return LoggingExecutor.executeWithException(dataSource, () -> {
            try {
                return dataSource.getConnection();
            } catch (RuntimeException | SQLException ex) {
                String errorMessage = "Failed to get connection from datasource: " + datasourceName;
                logger.error(errorMessage, ex); // log it here because the client may handle the exception and hide the details.
                throw new SQLException(errorMessage, ex);
            }
        });
    }

    /**
     * Nextval.
     *
     * @param sequence the sequence
     * @return the long
     * @throws SQLException the SQL exception
     */
    public static long nextval(String sequence) throws Throwable {
        return nextval(sequence, null, null);
    }

    // ========= Sequence ===========

    /**
     * Nextval.
     *
     * @param sequence the sequence
     * @param datasourceName the datasource name
     * @param tableName the table name
     * @return the nextval
     * @throws SQLException the SQL exception
     */
    public static long nextval(String sequence, String datasourceName, String tableName) throws Throwable {
        DataSource dataSource = getDataSource(datasourceName);
        if (dataSource == null) {
            String error = format("DataSource {0} not known.", datasourceName);
            throw new IllegalArgumentException(error);
        }
        return LoggingExecutor.executeWithException(dataSource, () -> {
            try (Connection connection = dataSource.getConnection()) {
                try {
                    return getNextVal(sequence, connection);
                } catch (SQLException e) {
                    // assuming the sequence does not exists first time, hence create it implicitly
                    logger.warn("Implicitly creating a Sequence [{}] due to: [{}]", sequence, e.getMessage());
                    createSequenceInternal(sequence, null, connection, tableName);
                    return getNextVal(sequence, connection);
                } catch (IllegalStateException e) {
                    // assuming the sequence objects are not supported by the underlying database
                    PersistenceNextValueIdentityProcessor persistenceNextValueIdentityProcessor =
                            new PersistenceNextValueIdentityProcessor(null);
                    return persistenceNextValueIdentityProcessor.nextval(connection, tableName);
                }
            }
        });

    }

    /**
     * Gets the next val.
     *
     * @param sequence the sequence
     * @param connection the connection
     * @return the next val
     * @throws SQLException the SQL exception
     */
    private static long getNextVal(String sequence, Connection connection) throws SQLException {
        String sql = SqlFactory.getNative(connection)
                               .nextval(sequence)
                               .build();
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getLong(1);
            }
            throw new SQLException("ResultSet is empty while getting next value of the Sequence: " + sequence);
        }

    }

    /**
     * Creates the sequence internal.
     *
     * @param sequence the sequence
     * @param seqStart the sequence start
     * @param connection the connection
     * @param tableName the table name
     * @throws SQLException the SQL exception
     */
    private static void createSequenceInternal(String sequence, final Integer seqStart, Connection connection, String tableName)
            throws SQLException {
        Integer sequenceStart = seqStart;
        Integer sequenceMaxStart = seqStart;
        Integer sequenceCountStart = seqStart;

        if (sequenceStart == null && tableName != null) {
            try {

                ResultSet primaryKeysResultSet = connection.getMetaData()
                                                           .getPrimaryKeys(connection.getCatalog(), null, tableName);
                if (primaryKeysResultSet.next()) {
                    String columnName = primaryKeysResultSet.getString(DatabaseParameters.JDBC_COLUMN_NAME_PROPERTY);
                    String maxSql = SqlFactory.getNative(connection)
                                              .select()
                                              .column("max(" + columnName + ")")
                                              .from(tableName)
                                              .build();
                    try (PreparedStatement countPreparedStatement = connection.prepareStatement(maxSql)) {
                        ResultSet rs = countPreparedStatement.executeQuery();
                        if (rs.next()) {
                            sequenceMaxStart = rs.getInt(1);
                            sequenceMaxStart++;
                        }
                    } catch (SQLException e) {
                        // Do nothing
                    }
                }
            } catch (SQLException e) {
                // Do nothing, fall back to the count approach
            }
            String countSql = SqlFactory.getNative(connection)
                                        .select()
                                        .column("count(*)")
                                        .from(tableName)
                                        .build();
            try (PreparedStatement countPreparedStatement = connection.prepareStatement(countSql)) {
                ResultSet rs = countPreparedStatement.executeQuery();
                if (rs.next()) {
                    sequenceCountStart = rs.getInt(1);
                    sequenceCountStart++;
                }
            } catch (SQLException e) {
                // Do nothing
            }
        }

        if (sequenceStart == null || sequenceMaxStart > sequenceStart) {
            sequenceStart = sequenceMaxStart;
        }
        if (sequenceStart == null || sequenceCountStart > sequenceStart) {
            sequenceStart = sequenceCountStart;
        }

        String sql = SqlFactory.getNative(connection)
                               .create()
                               .sequence(sequence)
                               .start(sequenceStart)
                               .build();
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.executeUpdate();
        }

    }

    /**
     * Nextval.
     *
     * @param sequence the sequence
     * @param datasourceName the datasource name
     * @return the long
     * @throws SQLException the SQL exception
     */
    public static long nextval(String sequence, String datasourceName) throws Throwable {
        return nextval(sequence, datasourceName, null);
    }

    /**
     * Creates the sequence.
     *
     * @param sequence the sequence
     * @param start the start
     * @throws SQLException the SQL exception
     */
    public static void createSequence(String sequence, Integer start) throws Throwable {
        createSequence(sequence, null, null);
    }

    /**
     * Creates the sequence.
     *
     * @param sequence the sequence
     * @param start the start
     * @param datasourceName the datasource name
     * @throws SQLException the SQL exception
     */
    public static void createSequence(String sequence, Integer start, String datasourceName) throws Throwable {
        DataSource dataSource = getDataSource(datasourceName);
        if (dataSource == null) {
            String error = format("DataSource {0} not known.", datasourceName);
            throw new IllegalArgumentException(error);
        }

        LoggingExecutor.executeNoResultWithException(dataSource, () -> {
            try (Connection connection = dataSource.getConnection()) {
                createSequenceInternal(sequence, start, connection, null);

            } catch (Exception ex) {
                logger.error("Failed to create sequence [{}] in data source [{}].", sequence, datasourceName, ex);
                throw ex;
            }
        });
    }

    /**
     * Creates the sequence.
     *
     * @param sequence the sequence
     * @throws SQLException the SQL exception
     */
    public static void createSequence(String sequence) throws Throwable {
        createSequence(sequence, null, null);
    }

    /**
     * Drop sequence.
     *
     * @param sequence the sequence
     * @throws SQLException the SQL exception
     */
    public static void dropSequence(String sequence) throws Throwable {
        dropSequence(sequence, null);
    }

    /**
     * Drop sequence.
     *
     * @param sequence the sequence
     * @param datasourceName the datasource name
     * @throws SQLException the SQL exception
     */
    public static void dropSequence(String sequence, String datasourceName) throws Throwable {
        DataSource dataSource = getDataSource(datasourceName);
        if (dataSource == null) {
            String error = format("DataSource {0} not known.", datasourceName);
            throw new IllegalArgumentException(error);
        }

        LoggingExecutor.executeNoResultWithException(dataSource, () -> {
            try (Connection connection = dataSource.getConnection()) {
                String sql = SqlFactory.getNative(connection)
                                       .drop()
                                       .sequence(sequence)
                                       .build();
                try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                    preparedStatement.executeUpdate();
                }
            } catch (Exception ex) {
                logger.error("Failed to drop sequence [{}] in data source [{}].", sequence, datasourceName, ex);
                throw ex;
            }
        });
    }

    // =========== SQL ===========

    /**
     * Gets the default SQL factory.
     *
     * @return the default SQL factory
     */
    public static SqlFactory getDefault() {
        return SqlFactory.getDefault();
    }

    /**
     * Gets a native SQL factory.
     *
     * @param connection the connection
     * @return a native SQL factory
     */
    public static SqlFactory getNative(Connection connection) {
        return SqlFactory.getNative(connection);
    }

    /**
     * Read blob value.
     *
     * @param resultSet the result set
     * @param index the index
     * @return the byte[]
     */
    public static byte[] readBlobValue(ResultSet resultSet, Integer index) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream input;
        try {
            input = resultSet.getBinaryStream(index);
            readByteStream(baos, input);
        } catch (Exception e) {
            logger.error("Failed to retreive a BLOB value of [{}].", index, e);
        }
        return baos.toByteArray();
    }

    /**
     * Read byte stream.
     *
     * @param baos the baos
     * @param input the input
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void readByteStream(ByteArrayOutputStream baos, InputStream input) throws IOException {
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) > 0) {
            baos.write(buffer, 0, bytesRead);
        }
    }

    /**
     * Read blob value.
     *
     * @param resultSet the result set
     * @param column the column
     * @return the byte[]
     */
    public static byte[] readBlobValue(ResultSet resultSet, String column) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream input;
        try {
            input = resultSet.getBinaryStream(column);
            readByteStream(baos, input);
        } catch (Exception e) {
            logger.error("Failed to retreive a BLOB value of [{}].", column, e);
        }
        return baos.toByteArray();
    }

}
