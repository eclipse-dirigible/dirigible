/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.odata.transformers;

import com.google.common.base.CaseFormat;
import org.eclipse.dirigible.components.base.spring.BeanProvider;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.data.structures.domain.Table;
import org.eclipse.dirigible.components.data.structures.domain.TableColumn;
import org.eclipse.dirigible.components.data.structures.domain.TableConstraintForeignKey;
import org.eclipse.dirigible.components.odata.api.ODataProperty;
import org.eclipse.dirigible.database.sql.ISqlKeywords;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * The Class DBMetadataUtil.
 */
public class ODataDatabaseMetadataUtil {

    /** The Constant DIRIGIBLE_GENERATE_PRETTY_NAMES. */
    public static final String DIRIGIBLE_GENERATE_PRETTY_NAMES = "DIRIGIBLE_GENERATE_PRETTY_NAMES";

    /** The Constant JDBC_COLUMN_NAME_PROPERTY. */
    public static final String JDBC_COLUMN_NAME_PROPERTY = "COLUMN_NAME";

    /** The Constant JDBC_COLUMN_TYPE_PROPERTY. */
    public static final String JDBC_COLUMN_TYPE_PROPERTY = "TYPE_NAME";

    /** The Constant JDBC_COLUMN_NULLABLE_PROPERTY. */
    public static final String JDBC_COLUMN_NULLABLE_PROPERTY = "NULLABLE";

    /** The Constant JDBC_COLUMN_SIZE_PROPERTY. */
    public static final String JDBC_COLUMN_SIZE_PROPERTY = "COLUMN_SIZE";

    /** The Constant JDBC_COLUMN_DECIMAL_DIGITS_PROPERTY. */
    public static final String JDBC_COLUMN_DECIMAL_DIGITS_PROPERTY = "DECIMAL_DIGITS";

    /** The Constant JDBC_FK_TABLE_NAME_PROPERTY. */
    public static final String JDBC_FK_TABLE_NAME_PROPERTY = "FKTABLE_NAME";

    /** The Constant JDBC_FK_NAME_PROPERTY. */
    public static final String JDBC_FK_NAME_PROPERTY = "FK_NAME";

    /** The Constant JDBC_PK_NAME_PROPERTY. */
    public static final String JDBC_PK_NAME_PROPERTY = "PK_NAME";

    /** The Constant JDBC_PK_TABLE_NAME_PROPERTY. */
    public static final String JDBC_PK_TABLE_NAME_PROPERTY = "PKTABLE_NAME";

    /** The Constant JDBC_PK_TABLE_NAME_PROPERTY. */
    public static final String JDBC_PK_SCHEMA_NAME_PROPERTY = "PKTABLE_SCHEM";

    /** The Constant JDBC_FK_COLUMN_NAME_PROPERTY. */
    public static final String JDBC_FK_COLUMN_NAME_PROPERTY = "FKCOLUMN_NAME";

    /** The Constant JDBC_PK_COLUMN_NAME_PROPERTY. */
    public static final String JDBC_PK_COLUMN_NAME_PROPERTY = "PKCOLUMN_NAME";

    /** The Constant SQL_TO_ODATA_EDM_TYPES. */
    private static final Map<String, String> SQL_TO_ODATA_EDM_TYPES = new HashMap<>();

    static {
        SQL_TO_ODATA_EDM_TYPES.put("TIME", "Edm.Time");
        SQL_TO_ODATA_EDM_TYPES.put("DATE", "Edm.DateTime");
        SQL_TO_ODATA_EDM_TYPES.put("SECONDDATE", "Edm.DateTime");
        SQL_TO_ODATA_EDM_TYPES.put("TIMESTAMP", "Edm.DateTime");
        SQL_TO_ODATA_EDM_TYPES.put("TINYINT", "Edm.Byte");
        SQL_TO_ODATA_EDM_TYPES.put("SMALLINT", "Edm.Int16");
        SQL_TO_ODATA_EDM_TYPES.put("INTEGER", "Edm.Int32");
        SQL_TO_ODATA_EDM_TYPES.put("INT", "Edm.Int32");
        SQL_TO_ODATA_EDM_TYPES.put("INT2", "Edm.Int32");
        SQL_TO_ODATA_EDM_TYPES.put("INT4", "Edm.Int32");
        SQL_TO_ODATA_EDM_TYPES.put("INT8", "Edm.Int64");
        SQL_TO_ODATA_EDM_TYPES.put("BIGINT", "Edm.Int64");
        SQL_TO_ODATA_EDM_TYPES.put("BIGSERIAL", "Edm.Int64");
        SQL_TO_ODATA_EDM_TYPES.put("SERIAL8", "Edm.Int64");
        SQL_TO_ODATA_EDM_TYPES.put("SMALLDECIMAL", "Edm.Decimal");
        SQL_TO_ODATA_EDM_TYPES.put("DECIMAL", "Edm.Decimal");
        SQL_TO_ODATA_EDM_TYPES.put("NUMERIC", "Edm.Decimal");
        SQL_TO_ODATA_EDM_TYPES.put("REAL", "Edm.Single");
        SQL_TO_ODATA_EDM_TYPES.put("FLOAT", "Edm.Single");
        SQL_TO_ODATA_EDM_TYPES.put("FLOAT4", "Edm.Single");
        SQL_TO_ODATA_EDM_TYPES.put("FLOAT8", "Edm.Double");
        SQL_TO_ODATA_EDM_TYPES.put("DOUBLE", "Edm.Double");
        SQL_TO_ODATA_EDM_TYPES.put("DOUBLE PRECISION", "Edm.Double");
        SQL_TO_ODATA_EDM_TYPES.put("VARCHAR", "Edm.String");
        SQL_TO_ODATA_EDM_TYPES.put("NVARCHAR", "Edm.String");
        SQL_TO_ODATA_EDM_TYPES.put("CHARACTER VARYING", "Edm.String");
        SQL_TO_ODATA_EDM_TYPES.put("CHARACTER", "Edm.String");
        SQL_TO_ODATA_EDM_TYPES.put("CHAR", "Edm.String");
        SQL_TO_ODATA_EDM_TYPES.put("NCHAR", "Edm.String");
        SQL_TO_ODATA_EDM_TYPES.put("BINARY", "Edm.Binary");
        SQL_TO_ODATA_EDM_TYPES.put("BINARY VARYING", "Edm.Binary");
        SQL_TO_ODATA_EDM_TYPES.put("BINARY LARGE OBJECT", "Edm.Binary");
        SQL_TO_ODATA_EDM_TYPES.put("BYTEA", "Edm.Binary");
        SQL_TO_ODATA_EDM_TYPES.put("VARBINARY", "Edm.Binary");
        SQL_TO_ODATA_EDM_TYPES.put("BOOLEAN", "Edm.Boolean");
        SQL_TO_ODATA_EDM_TYPES.put("BOOL", "Edm.Boolean");
        SQL_TO_ODATA_EDM_TYPES.put("BYTE", "Edm.Byte");
        SQL_TO_ODATA_EDM_TYPES.put("BIT", "Edm.Byte");
        SQL_TO_ODATA_EDM_TYPES.put("BLOB", "Edm.String");
        SQL_TO_ODATA_EDM_TYPES.put("NCLOB", "Edm.String");
        SQL_TO_ODATA_EDM_TYPES.put("CLOB", "Edm.String");
        SQL_TO_ODATA_EDM_TYPES.put("CHARACTER LARGE OBJECT", "Edm.String");
        SQL_TO_ODATA_EDM_TYPES.put("TEXT", "Edm.String");
        SQL_TO_ODATA_EDM_TYPES.put("BINTEXT", "Edm.Binary");
        SQL_TO_ODATA_EDM_TYPES.put("ALPHANUM", "Edm.String");
    }

    /**
     * Gets the data source.
     *
     * @return the data source
     */
    protected synchronized DataSource getDataSource() {
        DataSourcesManager dataSourcesManager = BeanProvider.getBean(DataSourcesManager.class);
        return dataSourcesManager.getDefaultDataSource();
    }

    /**
     * Gets the table metadata.
     *
     * @param tableName the table name
     * @return the table metadata
     * @throws SQLException the SQL exception
     */
    public Table getTableMetadata(String tableName) throws SQLException {
        return getTableMetadata(tableName, null);
    }

    /**
     * Gets the table metadata.
     *
     * @param tableName the table name
     * @param schemaName the schema name
     * @return the table metadata
     * @throws SQLException the SQL exception
     */
    public Table getTableMetadata(String tableName, String schemaName) throws SQLException {
        if (tableName == null) {
            throw new SQLException("Table name cannot be null while querying for the table metadata");
        }
        Table tableMetadata = new Table();
        tableMetadata.setName(tableName);
        tableMetadata.setSchema(schemaName);
        try (Connection connection = getDataSource().getConnection()) {
            DatabaseMetaData databaseMetadata = connection.getMetaData();
            String artifactType = getArtifactType(databaseMetadata, connection, tableName, schemaName);
            if (null == artifactType) {
                return null;
            }
            getColumns(databaseMetadata, connection, tableMetadata, schemaName);
            getForeignKeys(databaseMetadata, connection, tableMetadata, schemaName);
            tableMetadata.setKind(artifactType);
        }

        return tableMetadata;
    }

    /**
     * Gets the foreign keys.
     *
     * @param databaseMetadata the database metadata
     * @param connection the connection
     * @param tableMetadata the artifact name
     * @param schemaName the schema name
     * @return the foreign keys
     * @throws SQLException the SQL exception
     */
    private void getForeignKeys(DatabaseMetaData databaseMetadata, Connection connection, Table tableMetadata, String schemaName)
            throws SQLException {
        ResultSet foreignKeys =
                databaseMetadata.getImportedKeys(connection.getCatalog(), schemaName, normalizeTableName(tableMetadata.getName()));

        while (foreignKeys.next()) {
            new TableConstraintForeignKey(foreignKeys.getString(JDBC_FK_NAME_PROPERTY), new String[] {},
                    new String[] {foreignKeys.getString(JDBC_FK_COLUMN_NAME_PROPERTY)}, foreignKeys.getString(JDBC_PK_TABLE_NAME_PROPERTY),
                    foreignKeys.getString(JDBC_PK_SCHEMA_NAME_PROPERTY), new String[] {foreignKeys.getString(JDBC_PK_COLUMN_NAME_PROPERTY)},
                    tableMetadata.getConstraints());

        }
    }

    /**
     * Convert sql type to odata edm type.
     *
     * @param sqlType the sql type
     * @return the string
     */
    public String convertSqlTypeToOdataEdmType(String sqlType) {
        if (sqlType == null) {
            throw new IllegalArgumentException("SQL type cannot be null while converting to EDM type");
        }
        String edmColumnType = SQL_TO_ODATA_EDM_TYPES.get(sqlType.toUpperCase());
        if (null != edmColumnType) {
            return edmColumnType;
        }

        throw new IllegalArgumentException("SQL Type [" + sqlType + "] is not supported.");
    }

    /**
     * Gets the primary keys.
     *
     * @param databaseMetadata the database metadata
     * @param connection the connection
     * @param tableMetadata the artifact name
     * @param schemaName the schema name
     * @return the primary keys
     * @throws SQLException the SQL exception
     */
    private List<String> getPrimaryKeys(DatabaseMetaData databaseMetadata, Connection connection, Table tableMetadata, String schemaName)
            throws SQLException {
        ResultSet primaryKeys =
                databaseMetadata.getPrimaryKeys(connection.getCatalog(), schemaName, normalizeTableName(tableMetadata.getName()));

        List<String> primaryKeyColumns = new ArrayList<>();
        while (primaryKeys.next()) {
            primaryKeyColumns.add(primaryKeys.getString(JDBC_COLUMN_NAME_PROPERTY));
        }

        return primaryKeyColumns;
    }

    /**
     * Gets the columns.
     *
     * @param databaseMetadata the database metadata
     * @param connection the connection
     * @param tableMetadata the artifact name
     * @param schemaPattern the schema pattern
     * @return the columns
     * @throws SQLException the SQL exception
     */
    private void getColumns(DatabaseMetaData databaseMetadata, Connection connection, Table tableMetadata, String schemaPattern)
            throws SQLException {
        ResultSet columns =
                databaseMetadata.getColumns(connection.getCatalog(), schemaPattern, normalizeTableName(tableMetadata.getName()), null);

        List<String> primaryKeys = getPrimaryKeys(databaseMetadata, connection, tableMetadata, schemaPattern);

        Set<String> uniqueColumns = new HashSet<>();
        while (columns.next()) {
            String columnName = columns.getString(JDBC_COLUMN_NAME_PROPERTY);
            if (!uniqueColumns.contains(columnName)) {
                uniqueColumns.add(columnName);
                String columnType = convertSqlTypeToOdataEdmType(columns.getString(JDBC_COLUMN_TYPE_PROPERTY));
                new TableColumn(columnName, columnType, columns.getInt(JDBC_COLUMN_SIZE_PROPERTY) + "",
                        columns.getBoolean(JDBC_COLUMN_NULLABLE_PROPERTY), primaryKeys.contains(columnName), null,
                        columns.getInt(JDBC_COLUMN_DECIMAL_DIGITS_PROPERTY) + "", false, tableMetadata);
            }
        }
    }

    /**
     * Gets the property name from db column name.
     *
     * @param DbColumnName the db column name
     * @param oDataProperties the o data properties
     * @param prettyPrint the pretty print
     * @return the property name from db column name
     */
    public static String getPropertyNameFromDbColumnName(String DbColumnName, List<ODataProperty> oDataProperties, boolean prettyPrint) {
        for (ODataProperty next : oDataProperties) {
            if (DbColumnName.equals(next.getColumn())) {
                return next.getName();
            }
        }
        return prettyPrint ? addCorrectFormatting(DbColumnName) : DbColumnName;
    }

    /**
     * Checks if is prop column valid DB column.
     *
     * @param propColumn the prop column
     * @param dbColumns the db columns
     * @return true, if is prop column valid DB column
     */
    public static boolean isPropColumnValidDBColumn(String propColumn, List<TableColumn> dbColumns) {
        for (TableColumn next : dbColumns) {
            if (next.getName()
                    .equals(propColumn)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if is nullable.
     *
     * @param column the column
     * @param properties the properties
     * @return true, if is nullable
     */
    public static boolean isNullable(TableColumn column, List<ODataProperty> properties) {
        String columnName = column.getName();
        for (ODataProperty next : properties) {
            if (columnName.equals(next.getColumn())) {
                return next.isNullable();
            }
        }
        return column.isNullable();
    }

    /**
     * Gets the type.
     *
     * @param column the column
     * @param properties the properties
     * @return the type
     */
    public static String getType(TableColumn column, List<ODataProperty> properties) {
        String columnName = column.getName();
        for (ODataProperty next : properties) {
            if (next.getType() != null) {
                if (columnName.equals(next.getColumn())) {
                    return next.getType();
                }
            }
        }
        return column.getType();
    }

    /**
     * Adds the correct formatting.
     *
     * @param columnName the column name
     * @return the string
     */
    public static String addCorrectFormatting(String columnName) {
        return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, columnName);
    }

    /**
     * Normalize table name.
     *
     * @param table the table
     * @return the string
     */
    public static String normalizeTableName(String table) {
        if (table != null && table.startsWith("\"") && table.endsWith("\"")) {
            table = table.substring(1, table.length() - 1);
        }
        return table;
    }

    /**
     * Gets the artifact type.
     *
     * @param databaseMetadata the database metadata
     * @param connection the connection
     * @param artifactName the artifact name
     * @param schemaPattern the schema pattern
     * @return the artifact type
     * @throws SQLException the SQL exception
     */
    public String getArtifactType(DatabaseMetaData databaseMetadata, Connection connection, String artifactName, String schemaPattern)
            throws SQLException {
        ResultSet tables = databaseMetadata.getTables(connection.getCatalog(), schemaPattern, normalizeTableName(artifactName), null);

        return tables.next() ? tables.getString("TABLE_TYPE") : null;
    }

    /**
     * Find schema of a given artifact name. The searchable artifacts are TABLE, VIEW, CALC_VIEW
     *
     * @param artifactName name of the artifact
     * @return of a given artifact name
     * @throws SQLException SQLException
     */
    public String getOdataArtifactTypeSchema(String artifactName) throws SQLException {
        return getArtifactSchema(artifactName,
                new String[] {ISqlKeywords.METADATA_TABLE, ISqlKeywords.METADATA_VIEW, ISqlKeywords.METADATA_CALC_VIEW});
    }

    /**
     * Gets the artifact schema.
     *
     * @param artifactName the artifact name
     * @param types the types
     * @return the artifact schema
     * @throws SQLException the SQL exception
     */
    public String getArtifactSchema(String artifactName, String[] types) throws SQLException {
        try (Connection connection = getDataSource().getConnection()) {
            DatabaseMetaData databaseMetadata = connection.getMetaData();
            ResultSet rs = databaseMetadata.getTables(connection.getCatalog(), null, artifactName, types);
            if (rs.next()) {
                return rs.getString("TABLE_SCHEM");
            }
            return null;
        }
    }
}
