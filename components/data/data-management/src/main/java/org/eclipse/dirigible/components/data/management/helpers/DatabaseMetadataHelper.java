/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.management.helpers;

import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.components.data.management.domain.*;
import org.eclipse.dirigible.components.database.DatabaseNameNormalizer;
import org.eclipse.dirigible.components.database.DatabaseParameters;
import org.eclipse.dirigible.components.database.DatabaseSystem;
import org.eclipse.dirigible.components.database.DatabaseSystemDeterminer;
import org.eclipse.dirigible.database.sql.DatabaseType;
import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.ISqlKeywords;
import org.eclipse.dirigible.database.sql.SqlFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * The Database Metadata Helper.
 */
public class DatabaseMetadataHelper implements DatabaseParameters {

    /**
     * The Constant logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(DatabaseMetadataHelper.class);


    /**
     * The Interface Filter.
     *
     * @param <T> the generic type
     */
    public interface Filter<T> {

        /**
         * Accepts.
         *
         * @param t the t
         * @return true, if successful
         */
        boolean accepts(T t);
    }

    /**
     * Gets the dialect.
     *
     * @param connection the connection
     * @return the dialect
     */
    private static ISqlDialect getDialect(Connection connection) {
        return SqlFactory.deriveDialect(connection);
    }

    /**
     * List schemas.
     *
     * @param connection the connection
     * @param catalogName the catalog name
     * @param schemaNameFilter the schema name filter
     * @param nameFilter the name filter
     * @return the list
     * @throws SQLException the SQL exception
     */
    public static List<SchemaMetadata> listSchemas(Connection connection, String catalogName, Filter<String> schemaNameFilter,
            Filter<String> nameFilter) throws SQLException {

        ISqlDialect sqlDialect = getDialect(connection);

        List<SchemaMetadata> result = new ArrayList<SchemaMetadata>();
        if (sqlDialect.isSchemaFilterSupported()) {
            try {
                // low level filtering for schema
                try (Statement stmt = connection.createStatement()) {
                    try (ResultSet rs = stmt.executeQuery(sqlDialect.getSchemaFilterScript())) {
                        filterSchemas(connection, catalogName, schemaNameFilter, nameFilter, result, rs);
                    }
                }
            } catch (Exception e) {
                DatabaseMetaData dmd = connection.getMetaData();
                // backup in case of wrong product recognition
                try (ResultSet rs = dmd.getSchemas(catalogName, null)) {
                    filterSchemas(connection, catalogName, schemaNameFilter, nameFilter, result, rs);
                }
            }
        } else if (sqlDialect.isCatalogForSchema()) {
            DatabaseMetaData dmd = connection.getMetaData();
            try (ResultSet rs = dmd.getCatalogs()) {
                filterSchemas(connection, catalogName, schemaNameFilter, nameFilter, result, rs);
            }
        } else {
            DatabaseMetaData dmd = connection.getMetaData();
            try (ResultSet rs = dmd.getSchemas(catalogName, null)) {
                filterSchemas(connection, catalogName, schemaNameFilter, nameFilter, result, rs);
            }
        }

        if (sqlDialect.getDatabaseType(connection)
                      .equals(DatabaseType.NOSQL.getName())) {
            result.forEach(s -> s.setKind(DatabaseType.NOSQL.getName()
                                                            .toLowerCase()));
        }
        return result;
    }

    /**
     * Filter schemas.
     *
     * @param connection the connection
     * @param catalogName the catalog name
     * @param schemaNameFilter the schema name filter
     * @param nameFilter the name filter
     * @param result the result
     * @param rs the rs
     * @throws SQLException the SQL exception
     */
    private static void filterSchemas(Connection connection, String catalogName, Filter<String> schemaNameFilter, Filter<String> nameFilter,
            List<SchemaMetadata> result, ResultSet rs) throws SQLException {
        if (rs != null) {
            while (rs.next()) {
                String schemeName = rs.getString(1); // TABLE_SCHEM or TABLE_CAT
                // higher level filtering for schema if low level is not supported
                if ((schemaNameFilter != null) && !schemaNameFilter.accepts(schemeName)) {
                    continue;
                }
                result.add(new SchemaMetadata(schemeName, connection, catalogName, nameFilter));
            }
        }
    }

    /**
     * List tables.
     *
     * @param connection the connection
     * @param catalogName the catalog name
     * @param schemeName the scheme name
     * @param tableNameFilter the table name filter
     * @return the list
     * @throws SQLException the SQL exception
     */
    public static List<TableMetadata> listTables(Connection connection, String catalogName, String schemeName,
            Filter<String> tableNameFilter) throws SQLException {

        DatabaseMetaData dmd = connection.getMetaData();

        ISqlDialect sqlDialect = getDialect(connection);

        List<TableMetadata> result = new ArrayList<TableMetadata>();

        ResultSet rs = null;
        try {
            if (sqlDialect.isCatalogForSchema()) {
                rs = dmd.getTables(schemeName, null, PRCNT, TABLE_TYPES);
            } else {
                rs = dmd.getTables(catalogName, schemeName, PRCNT, TABLE_TYPES);
            }

            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                String tableType = rs.getString("TABLE_TYPE");
                String tableRemarks = rs.getString("REMARKS");
                if ((tableNameFilter != null) && !tableNameFilter.accepts(tableName)) {
                    continue;
                }
                result.add(new TableMetadata(tableName, tableType, tableRemarks, connection, catalogName, schemeName, false));
            }
        } finally {
            if (rs != null) {
                rs.close();
            }
        }

        return result;
    }

    /**
     * List procedures.
     *
     * @param connection the connection
     * @param catalogName the catalog name
     * @param schemeName the scheme name
     * @param procedureNameFilter the procedure name filter
     * @return the list
     * @throws SQLException the SQL exception
     */
    public static List<ProcedureMetadata> listProcedures(Connection connection, String catalogName, String schemeName,
            Filter<String> procedureNameFilter) throws SQLException {

        DatabaseMetaData dmd = connection.getMetaData();

        ISqlDialect sqlDialect = getDialect(connection);

        List<ProcedureMetadata> result = new ArrayList<ProcedureMetadata>();

        ResultSet rs = null;
        try {
            if (sqlDialect.isCatalogForSchema()) {
                rs = dmd.getProcedures(schemeName, null, PRCNT);
            } else {
                rs = dmd.getProcedures(catalogName, schemeName, PRCNT);
            }

            while (rs.next()) {
                String procedureName = rs.getString("PROCEDURE_NAME");
                String procedureType = rs.getString("PROCEDURE_TYPE");
                String procedureRemarks = rs.getString("REMARKS");
                if ((procedureNameFilter != null) && !procedureNameFilter.accepts(procedureName)) {
                    continue;
                }
                result.add(
                        new ProcedureMetadata(procedureName, procedureType, procedureRemarks, connection, catalogName, schemeName, false));
            }
        } finally {
            if (rs != null) {
                rs.close();
            }
        }

        return result;
    }

    /**
     * List functions.
     *
     * @param connection the connection
     * @param catalogName the catalog name
     * @param schemeName the scheme name
     * @param functionNameFilter the function name filter
     * @return the list
     * @throws SQLException the SQL exception
     */
    public static List<FunctionMetadata> listFunctions(Connection connection, String catalogName, String schemeName,
            Filter<String> functionNameFilter) throws SQLException {

        DatabaseMetaData dmd = connection.getMetaData();

        ISqlDialect sqlDialect = getDialect(connection);

        List<FunctionMetadata> result = new ArrayList<FunctionMetadata>();

        ResultSet rs = null;
        try {
            if (sqlDialect.isCatalogForSchema()) {
                rs = dmd.getFunctions(schemeName, null, PRCNT);
            } else {
                rs = dmd.getFunctions(catalogName, schemeName, PRCNT);
            }

            while (rs.next()) {
                String functionName = rs.getString("FUNCTION_NAME");
                String functionType = rs.getString("FUNCTION_TYPE");
                String functionRemarks = rs.getString("REMARKS");
                if ((functionNameFilter != null) && !functionNameFilter.accepts(functionName)) {
                    continue;
                }
                result.add(new FunctionMetadata(functionName, functionType, functionRemarks, connection, catalogName, schemeName, false));
            }
        } finally {
            if (rs != null) {
                rs.close();
            }
        }

        return result;
    }

    /**
     * List sequences.
     *
     * @param connection the connection
     * @param name the name
     * @return the list
     * @throws SQLException the SQL exception
     */
    public static List<SequenceMetadata> listSequences(Connection connection, String name) throws SQLException {

        DatabaseMetaData dmd = connection.getMetaData();

        List<SequenceMetadata> result = new ArrayList<SequenceMetadata>();

        String query = null;

        DatabaseSystem databaseSystem = DatabaseSystemDeterminer.determine(connection);
        if (databaseSystem.isMariaDB() || databaseSystem.isMySQL()) {
            query = String.format(
                    "SELECT column_name FROM information_schema.columns WHERE table_schema = '%s' AND extra = 'auto_increment'", name);
        } else if (databaseSystem.isSnowflake()) {
            query = "SHOW SEQUENCES";
        } else if (!databaseSystem.isMongoDB()) {
            query = "SELECT * FROM information_schema.sequences";
        }

        ResultSet rs = null;
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String sequenceName;
                    if (databaseSystem.isMariaDB() || databaseSystem.isMySQL()) {
                        sequenceName = resultSet.getString("column_name");
                    } else if (databaseSystem.isSnowflake()) {
                        sequenceName = resultSet.getString("name");
                    } else {
                        sequenceName = resultSet.getString("sequence_name");
                    }
                    result.add(new SequenceMetadata(sequenceName));
                }
            }
        } finally {
            if (rs != null) {
                rs.close();
            }
        }
        return result;
    }

    /**
     * Describe table.
     *
     * @param connection the connection
     * @param catalogName the catalog name
     * @param schemeName the scheme name
     * @param tableName the table name
     * @return the TableMetadata
     * @throws SQLException the SQL exception
     */
    public static TableMetadata describeTable(Connection connection, String catalogName, String schemeName, String tableName)
            throws SQLException {

        DatabaseMetaData dmd = connection.getMetaData();

        ISqlDialect sqlDialect = getDialect(connection);

        ResultSet rs = null;
        try {
            if (sqlDialect.isCatalogForSchema()) {
                rs = dmd.getTables(schemeName, null, DatabaseNameNormalizer.normalizeTableName(tableName), TABLE_TYPES);
            } else {
                rs = dmd.getTables(catalogName, schemeName, DatabaseNameNormalizer.normalizeTableName(tableName), TABLE_TYPES);
            }

            if (rs.next()) {
                String tableType = rs.getString("TABLE_TYPE");
                String tableRemarks = rs.getString("REMARKS");
                return new TableMetadata(tableName, tableType, tableRemarks, connection, catalogName, schemeName, true);
            }
        } finally {
            if (rs != null) {
                rs.close();
            }
        }
        return null;
    }

    /**
     * Describe procedure.
     *
     * @param connection the connection
     * @param catalogName the catalog name
     * @param schemeName the scheme name
     * @param procedureName the procedure name
     * @return the ProcedureMetadata
     * @throws SQLException the SQL exception
     */
    public static ProcedureMetadata describeProcedure(Connection connection, String catalogName, String schemeName, String procedureName)
            throws SQLException {

        DatabaseMetaData dmd = connection.getMetaData();

        ISqlDialect sqlDialect = getDialect(connection);

        ResultSet rs = null;
        try {
            if (sqlDialect.isCatalogForSchema()) {
                rs = dmd.getProcedures(schemeName, null, DatabaseNameNormalizer.normalizeTableName(procedureName));
            } else {
                rs = dmd.getProcedures(catalogName, schemeName, DatabaseNameNormalizer.normalizeTableName(procedureName));
            }

            if (rs.next()) {
                String procedureType = rs.getString("PROCEDURE_TYPE");
                String procedureRemarks = rs.getString("REMARKS");
                return new ProcedureMetadata(procedureName, procedureType, procedureRemarks, connection, catalogName, schemeName, true);
            }
        } finally {
            if (rs != null) {
                rs.close();
            }
        }
        return null;
    }

    /**
     * Describe function.
     *
     * @param connection the connection
     * @param catalogName the catalog name
     * @param schemeName the scheme name
     * @param functionName the function name
     * @return the FunctionMetadata
     * @throws SQLException the SQL exception
     */
    public static FunctionMetadata describeFunction(Connection connection, String catalogName, String schemeName, String functionName)
            throws SQLException {

        DatabaseMetaData dmd = connection.getMetaData();

        ISqlDialect sqlDialect = getDialect(connection);

        ResultSet rs = null;
        try {
            if (sqlDialect.isCatalogForSchema()) {
                rs = dmd.getFunctions(schemeName, null, DatabaseNameNormalizer.normalizeTableName(functionName));
            } else {
                rs = dmd.getFunctions(catalogName, schemeName, DatabaseNameNormalizer.normalizeTableName(functionName));
            }

            if (rs.next()) {
                String functionType = rs.getString("FUNCTION_TYPE");
                String functionRemarks = rs.getString("REMARKS");
                return new FunctionMetadata(functionName, functionType, functionRemarks, connection, catalogName, schemeName, true);
            }
        } finally {
            if (rs != null) {
                rs.close();
            }
        }
        return null;
    }

    /**
     * The Interface ColumnsIteratorCallback.
     */
    public interface ColumnsIteratorCallback {

        /**
         * On column.
         *
         * @param name the name
         * @param type the type
         * @param size the size
         * @param isNullable the is nullable
         * @param isKey the is key
         * @param scale the scale
         */
        void onColumn(String name, String type, String size, boolean isNullable, boolean isKey, int scale);
    }


    /**
     * The Interface ProceduresColumnsIteratorCallback.
     */
    public interface ProcedureColumnsIteratorCallback {

        /**
         * Procedure column callback.
         *
         * @param name name
         * @param kind kind
         * @param type type
         * @param precision precision
         * @param length length
         * @param scale scale
         * @param radix radix
         * @param nullable nullable
         * @param remarks remarks
         */
        void onProcedureColumn(String name, int kind, String type, int precision, int length, int scale, int radix, boolean nullable,
                String remarks);
    }


    /**
     * The Interface FunctionColumnsIteratorCallback.
     */
    public interface FunctionColumnsIteratorCallback {

        /**
         * Function column callback.
         *
         * @param name name
         * @param kind kind
         * @param type type
         * @param precision precision
         * @param length length
         * @param scale scale
         * @param radix radix
         * @param nullable nullable
         * @param remarks remarks
         */
        void onFunctionColumn(String name, int kind, String type, int precision, int length, int scale, int radix, boolean nullable,
                String remarks);
    }


    /**
     * The Interface IndicesIteratorCallback.
     */
    public interface IndicesIteratorCallback {

        /**
         * On index.
         *
         * @param indexName the index name
         * @param indexType the index type
         * @param columnName the column name
         * @param isNonUnique the is non unique
         * @param indexQualifier the index qualifier
         * @param ordinalPosition the ordinal position
         * @param sortOrder the sort order
         * @param cardinality the cardinality
         * @param pagesIndex the pages index
         * @param filterCondition the filter condition
         */
        void onIndex(String indexName, String indexType, String columnName, boolean isNonUnique, String indexQualifier,
                String ordinalPosition, String sortOrder, String cardinality, String pagesIndex, String filterCondition);
    }


    /**
     * The Interface IndicesIteratorCallback.
     */
    public interface ForeignKeysIteratorCallback {

        /**
         * On index.
         *
         * @param fkName the index name
         */
        void onIndex(String fkName);
    }

    /**
     * Iterate table definition.
     *
     * @param connection the connection
     * @param catalogName the catalog name
     * @param schemaName the schema name
     * @param tableName the table name
     * @param columnsIteratorCallback the columns iterator callback
     * @param indicesIteratorCallback the indices iterator callback
     * @param foreignKeysIteratorCallback the foreign keys iterator callback
     * @throws SQLException the SQL exception
     */
    public static void iterateTableDefinition(Connection connection, String catalogName, String schemaName, String tableName,
            ColumnsIteratorCallback columnsIteratorCallback, IndicesIteratorCallback indicesIteratorCallback,
            ForeignKeysIteratorCallback foreignKeysIteratorCallback) throws SQLException {

        DatabaseMetaData dmd = connection.getMetaData();

        ResultSet columns = dmd.getColumns(catalogName, schemaName, DatabaseNameNormalizer.normalizeTableName(tableName), null);
        if (columns == null) {
            throw new SQLException("DatabaseMetaData.getColumns returns null");
        }
        ResultSet pks = dmd.getPrimaryKeys(catalogName, schemaName, DatabaseNameNormalizer.normalizeTableName(tableName));
        if (pks == null) {
            throw new SQLException("DatabaseMetaData.getPrimaryKeys returns null");
        }
        ResultSet indexes = dmd.getIndexInfo(catalogName, schemaName, DatabaseNameNormalizer.normalizeTableName(tableName), false, false);
        if (indexes == null) {
            throw new SQLException("DatabaseMetaData.getIndexInfo returns null");
        }

        ResultSet foreignKeys = dmd.getImportedKeys(catalogName, schemaName, DatabaseNameNormalizer.normalizeTableName(tableName));
        if (foreignKeys == null) {
            throw new SQLException("DatabaseMetaData.getImportedKeys returns null");
        }

        try {

            List<String> pkList = new ArrayList<String>();
            while (pks.next()) {
                String pkName = pks.getString(COLUMN_NAME);
                pkList.add(pkName);
            }

            while (columns.next()) {
                if (columnsIteratorCallback != null) {
                    String cname = columns.getString(COLUMN_NAME);
                    columnsIteratorCallback.onColumn(cname, columns.getString(TYPE_NAME), columns.getInt(COLUMN_SIZE) + EMPTY,
                            columns.getBoolean(IS_NULLABLE), pkList.contains(cname), columns.getInt(DECIMAL_DIGITS));
                }
            }
            while (indexes.next()) {
                if (indicesIteratorCallback != null) {
                    indicesIteratorCallback.onIndex(indexes.getString(INDEX_NAME), indexes.getString(TYPE_INDEX),
                            indexes.getString(COLUMN_NAME), indexes.getBoolean(NON_UNIQUE), indexes.getString(INDEX_QUALIFIER),
                            indexes.getShort(ORDINAL_POSITION) + EMPTY, indexes.getString(ASC_OR_DESC), indexes.getInt(CARDINALITY) + EMPTY,
                            indexes.getInt(PAGES_INDEX) + EMPTY, indexes.getString(FILTER_CONDITION));
                }
            }
            while (foreignKeys.next()) {
                if (foreignKeysIteratorCallback != null) {
                    foreignKeysIteratorCallback.onIndex(foreignKeys.getString(FK_NAME));
                }
            }
        } finally {
            columns.close();
            indexes.close();
            foreignKeys.close();
            pks.close();
        }
    }

    /**
     * Iterate procedure definition.
     *
     * @param connection the connection
     * @param catalogName the catalog name
     * @param schemaName the schema name
     * @param procedureName the procedure name
     * @param procedureColumnsIteratorCallback the procedure columns iterator callback
     * @throws SQLException the SQL exception
     */
    public static void iterateProcedureDefinition(Connection connection, String catalogName, String schemaName, String procedureName,
            ProcedureColumnsIteratorCallback procedureColumnsIteratorCallback) throws SQLException {

        DatabaseMetaData dmd = connection.getMetaData();

        ResultSet columns =
                dmd.getProcedureColumns(catalogName, schemaName, DatabaseNameNormalizer.normalizeTableName(procedureName), null);
        if (columns == null) {
            throw new SQLException("DatabaseMetaData.getProcedureColumns returns null");
        }

        try {

            while (columns.next()) {
                if (procedureColumnsIteratorCallback != null) {
                    String cname = columns.getString(COLUMN_NAME);
                    procedureColumnsIteratorCallback.onProcedureColumn(cname, columns.getInt(COLUMN_TYPE), columns.getString(TYPE_NAME),
                            columns.getInt(PRECISION), columns.getInt(LENGTH), columns.getInt(SCALE), columns.getInt(RADIX),
                            columns.getBoolean(NULLABLE), columns.getString(REMARKS));
                }
            }

        } finally {
            columns.close();
        }
    }

    /**
     * Iterate function definition.
     *
     * @param connection the connection
     * @param catalogName the catalog name
     * @param schemaName the schema name
     * @param functionName the function name
     * @param functionColumnsIteratorCallback the function columns iterator callback
     * @throws SQLException the SQL exception
     */
    public static void iterateFunctionDefinition(Connection connection, String catalogName, String schemaName, String functionName,
            FunctionColumnsIteratorCallback functionColumnsIteratorCallback) throws SQLException {

        DatabaseMetaData dmd = connection.getMetaData();

        ResultSet columns = dmd.getFunctionColumns(catalogName, schemaName, DatabaseNameNormalizer.normalizeTableName(functionName), null);
        if (columns == null) {
            throw new SQLException("DatabaseMetaData.getFunctionColumns returns null");
        }

        try {

            while (columns.next()) {
                if (functionColumnsIteratorCallback != null) {
                    String cname = columns.getString(COLUMN_NAME);
                    functionColumnsIteratorCallback.onFunctionColumn(cname, columns.getInt(COLUMN_TYPE), columns.getString(TYPE_NAME),
                            columns.getInt(PRECISION), columns.getInt(LENGTH), columns.getInt(SCALE), columns.getInt(RADIX),
                            columns.getBoolean(NULLABLE), columns.getString(REMARKS));
                }
            }

        } finally {
            columns.close();
        }
    }

    /**
     * Gets the metadata as json.
     *
     * @param dataSource the data source
     * @return the metadata as json
     * @throws SQLException the SQL exception
     */
    public static String getMetadataAsJson(DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetadata database = new DatabaseMetadata(connection, null, null, null);
            String json = GsonHelper.toJson(database);
            return json;
        }
    }

    /**
     * Gets the schema metadata as json.
     *
     * @param dataSource the data source
     * @param schema the schema
     * @return the schema metadata as json
     * @throws SQLException the SQL exception
     */
    public static String getSchemaMetadataAsJson(DataSource dataSource, String schema) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            SchemaMetadata metadata = new SchemaMetadata(schema, connection, null, null);
            String json = GsonHelper.toJson(metadata);
            return json;
        }
    }

    /**
     * Gets the metadata as json.
     *
     * @param dataSource the data source
     * @param schema the schema name
     * @param table the table name
     * @return the metadata as json
     * @throws SQLException the SQL exception
     */
    public static String getTableMetadataAsJson(DataSource dataSource, String schema, String table) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            if (SqlFactory.deriveDialect(connection)
                          .getDatabaseType(connection)
                          .equals(DatabaseType.NOSQL.getName())) {
                NoSQLTableMetadata noSQLTableMetadata = describeNoSQL(connection, null, schema, table);
                String json = GsonHelper.toJson(noSQLTableMetadata);
                return json;
            } else {
                TableMetadata tableMetadata = describeTable(connection, null, schema, table);
                try {
                    if (tableMetadata == null) {
                        String tableName = new String(Base64.getDecoder()
                                                            .decode(table));
                        tableMetadata = describeTable(connection, null, schema, tableName);
                    }
                } catch (Exception e) {
                    if (logger.isWarnEnabled()) {
                        logger.warn("Table does not exist and it is not Base64 encoded name either", e);
                    }
                }
                String json = GsonHelper.toJson(tableMetadata);
                return json;
            }
        }
    }

    /**
     * Describe no SQL.
     *
     * @param connection the connection
     * @param catalogName the catalog name
     * @param schemeName the scheme name
     * @param tableName the table name
     * @return the no SQL table metadata
     * @throws SQLException the SQL exception
     */
    private static NoSQLTableMetadata describeNoSQL(Connection connection, String catalogName, String schemeName, String tableName)
            throws SQLException {
        DatabaseMetaData dmd = connection.getMetaData();

        ISqlDialect sqlDialect = getDialect(connection);

        ResultSet rs = null;
        try {
            if (sqlDialect.isCatalogForSchema()) {
                rs = dmd.getTables(schemeName, null, DatabaseNameNormalizer.normalizeTableName(tableName), TABLE_TYPES);
            } else {
                rs = dmd.getTables(catalogName, schemeName, DatabaseNameNormalizer.normalizeTableName(tableName), TABLE_TYPES);
            }

            if (rs.next()) {
                String tableType = rs.getString("TABLE_TYPE");
                String tableRemarks = rs.getString("REMARKS");
                return new NoSQLTableMetadata(tableName, tableType, tableRemarks, connection, catalogName, schemeName, true);
            }
        } finally {
            if (rs != null) {
                rs.close();
            }
        }
        return null;
    }

    /**
     * Gets the metadata as json.
     *
     * @param dataSource the data source
     * @param schema the schema name
     * @param procedure the procedure name
     * @return the metadata as json
     * @throws SQLException the SQL exception
     */
    public static String getProcedureMetadataAsJson(DataSource dataSource, String schema, String procedure) throws SQLException {
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            ProcedureMetadata procedureMetadata = describeProcedure(connection, null, schema, procedure);
            String json = GsonHelper.toJson(procedureMetadata);
            return json;
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    if (logger.isWarnEnabled()) {
                        logger.warn(e.getMessage(), e);
                    }
                }
            }
        }
    }

    /**
     * Gets the metadata as json.
     *
     * @param dataSource the data source
     * @param schema the schema name
     * @param function the function name
     * @return the metadata as json
     * @throws SQLException the SQL exception
     */
    public static String getFunctionMetadataAsJson(DataSource dataSource, String schema, String function) throws SQLException {
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            FunctionMetadata functionMetadata = describeFunction(connection, null, schema, function);
            String json = GsonHelper.toJson(functionMetadata);
            return json;
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    if (logger.isWarnEnabled()) {
                        logger.warn(e.getMessage(), e);
                    }
                }
            }
        }
    }

    /**
     * Gets the product name.
     *
     * @param dataSource the data source
     * @return the product name
     * @throws SQLException the SQL exception
     */
    public static String getProductName(DataSource dataSource) throws SQLException {
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            DatabaseMetadata database = new DatabaseMetadata(connection, null, null, null);
            return database.getDatabaseProductName();
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    if (logger.isWarnEnabled()) {
                        logger.warn(e.getMessage(), e);
                    }
                }
            }
        }
    }

    /**
     * Gets the table schema.
     *
     * @param connection the connection
     * @param tableName the table name
     * @return the table schema
     * @throws SQLException the SQL exception
     */
    public static String getTableSchema(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData databaseMetaData = connection.getMetaData();
        ResultSet rs = databaseMetaData.getTables(connection.getCatalog(), null, tableName, new String[] {ISqlKeywords.KEYWORD_TABLE});
        if (rs.next()) {
            return rs.getString("TABLE_SCHEM");
        }
        return null;
    }
}
