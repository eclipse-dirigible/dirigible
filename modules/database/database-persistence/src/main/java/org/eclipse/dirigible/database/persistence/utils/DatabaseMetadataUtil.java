/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.persistence.utils;

import com.google.common.base.CaseFormat;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.eclipse.dirigible.database.persistence.model.PersistenceTableColumnModel;
import org.eclipse.dirigible.database.persistence.model.PersistenceTableIndexModel;
import org.eclipse.dirigible.database.persistence.model.PersistenceTableModel;
import org.eclipse.dirigible.database.persistence.model.PersistenceTableRelationModel;
import org.eclipse.dirigible.database.sql.ISqlKeywords;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The Class DatabaseMetadataUtil.
 */
public class DatabaseMetadataUtil {

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

    /** The Constant JDBC_FK_COLUMN_NAME_PROPERTY. */
    public static final String JDBC_FK_COLUMN_NAME_PROPERTY = "FKCOLUMN_NAME";

    /** The Constant JDBC_PK_COLUMN_NAME_PROPERTY. */
    public static final String JDBC_PK_COLUMN_NAME_PROPERTY = "PKCOLUMN_NAME";

    /**
     * Gets the table metadata.
     *
     * @param tableName the table name
     * @return the table metadata
     * @throws SQLException the SQL exception
     */
    public static PersistenceTableModel getTableMetadata(String tableName) throws SQLException {
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
    public static PersistenceTableModel getTableMetadata(String tableName, String schemaName) throws SQLException {
        return getTableMetadata(tableName, schemaName, null);
    }

    /**
     * Gets the table metadata.
     *
     * @param tableName the table name
     * @param schemaName the schema name
     * @param dataSource the data source
     * @return the table metadata
     * @throws SQLException the SQL exception
     */
    public static PersistenceTableModel getTableMetadata(String tableName, String schemaName, DataSource dataSource) throws SQLException {
        PersistenceTableModel tableMetadata = new PersistenceTableModel(tableName, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData databaseMetadata = connection.getMetaData();
            try (ResultSet rs = databaseMetadata.getTables(null, schemaName, tableName, null)) {
                if (rs.next()) {
                    addFields(databaseMetadata, connection, tableMetadata, schemaName);
                    addPrimaryKeys(databaseMetadata, connection, tableMetadata, schemaName);
                    addForeignKeys(databaseMetadata, connection, tableMetadata, schemaName);
                    addIndices(databaseMetadata, connection, tableMetadata, schemaName);
                    addTableType(databaseMetadata, connection, tableMetadata, schemaName);
                } else {
                    return null;
                }
            }
            tableMetadata.setSchemaName(schemaName);
        } catch (SQLException e) {
            throw e;
        }

        return tableMetadata;
    }

    /**
     * Adds the foreign keys.
     *
     * @param databaseMetadata the database metadata
     * @param connection the connection
     * @param tableMetadata the table metadata
     * @param schema the schema
     * @throws SQLException the SQL exception
     */
    public static void addForeignKeys(DatabaseMetaData databaseMetadata, Connection connection, PersistenceTableModel tableMetadata,
            String schema) throws SQLException {
        ResultSet foreignKeys =
                databaseMetadata.getImportedKeys(connection.getCatalog(), schema, normalizeTableName(tableMetadata.getTableName()));
        if (foreignKeys.next()) {
            iterateForeignKeys(tableMetadata, foreignKeys);
        }
    }

    /**
     * Iterate foreign keys.
     *
     * @param tableMetadata the table metadata
     * @param foreignKeys the foreign keys
     * @throws SQLException the SQL exception
     */
    private static void iterateForeignKeys(PersistenceTableModel tableMetadata, ResultSet foreignKeys) throws SQLException {
        do {
            PersistenceTableRelationModel relationMetadata = new PersistenceTableRelationModel(
                    foreignKeys.getString(JDBC_FK_TABLE_NAME_PROPERTY), foreignKeys.getString(JDBC_PK_TABLE_NAME_PROPERTY),
                    foreignKeys.getString(JDBC_FK_COLUMN_NAME_PROPERTY), foreignKeys.getString(JDBC_PK_COLUMN_NAME_PROPERTY),
                    foreignKeys.getString(JDBC_FK_NAME_PROPERTY), foreignKeys.getString(JDBC_PK_NAME_PROPERTY));
            tableMetadata.getRelations()
                         .add(relationMetadata);
        } while (foreignKeys.next());
    }

    /**
     * Adds the primary keys.
     *
     * @param databaseMetadata the database metadata
     * @param connection the connection
     * @param tableMetadata the table metadata
     * @param schema the schema
     * @throws SQLException the SQL exception
     */
    public static void addPrimaryKeys(DatabaseMetaData databaseMetadata, Connection connection, PersistenceTableModel tableMetadata,
            String schema) throws SQLException {
        ResultSet primaryKeys =
                databaseMetadata.getPrimaryKeys(connection.getCatalog(), schema, normalizeTableName(tableMetadata.getTableName()));
        if (primaryKeys.next()) {
            iteratePrimaryKeys(tableMetadata, primaryKeys);
        }

    }

    /**
     * Iterate primary keys.
     *
     * @param tableMetadata the table metadata
     * @param primaryKeys the primary keys
     * @throws SQLException the SQL exception
     */
    private static void iteratePrimaryKeys(PersistenceTableModel tableMetadata, ResultSet primaryKeys) throws SQLException {
        do {
            setColumnPrimaryKey(primaryKeys.getString(JDBC_COLUMN_NAME_PROPERTY), tableMetadata);
        } while (primaryKeys.next());
    }

    /**
     * Sets the column primary key.
     *
     * @param columnName the column name
     * @param tableModel the table model
     */
    public static void setColumnPrimaryKey(String columnName, PersistenceTableModel tableModel) {
        tableModel.getColumns()
                  .forEach(column -> {
                      if (column.getName()
                                .equals(columnName)) {
                          column.setPrimaryKey(true);
                      }
                  });
    }

    /**
     * Add indices.
     *
     * @param databaseMetadata the database metadata
     * @param connection the connection
     * @param tableMetadata the table metadata
     * @param schema the schema name
     * @throws SQLException the SQL exception
     */
    public static void addIndices(DatabaseMetaData databaseMetadata, Connection connection, PersistenceTableModel tableMetadata,
            String schema) throws SQLException {

        try (ResultSet indexes = databaseMetadata.getIndexInfo(connection.getCatalog(), schema,
                normalizeTableName(tableMetadata.getTableName()), false, true)) {
            String lastIndexName = "";

            PersistenceTableIndexModel index = null;

            while (indexes.next()) {
                String indexName = indexes.getString("INDEX_NAME");
                if (indexName == null) {
                    continue;
                }

                if (!indexName.equals(lastIndexName)) {
                    index = new PersistenceTableIndexModel();
                    index.setName(indexName);
                    index.setUnique(!indexes.getBoolean("NON_UNIQUE"));
                    tableMetadata.getIndices()
                                 .add(index);
                    lastIndexName = indexName;
                }
                if (index != null) {
                    String columnName = indexes.getString("COLUMN_NAME");
                    index.getColumns()
                         .add(columnName);
                }
            }
        }

    }

    /**
     * Adds the fields.
     *
     * @param databaseMetadata the database metadata
     * @param connection the connection
     * @param tableMetadata the table metadata
     * @param schemaPattern the schema pattern
     * @throws SQLException the SQL exception
     */
    public static void addFields(DatabaseMetaData databaseMetadata, Connection connection, PersistenceTableModel tableMetadata,
            String schemaPattern) throws SQLException {
        ResultSet columns =
                databaseMetadata.getColumns(connection.getCatalog(), schemaPattern, normalizeTableName(tableMetadata.getTableName()), null);
        if (columns.next()) {
            iterateFields(tableMetadata, columns);
        }

    }

    /**
     * Iterate fields.
     *
     * @param tableMetadata the table metadata
     * @param columns the columns
     * @throws SQLException the SQL exception
     */
    private static void iterateFields(PersistenceTableModel tableMetadata, ResultSet columns) throws SQLException {
        do {
            tableMetadata.getColumns()
                         .add(new PersistenceTableColumnModel(columns.getString(JDBC_COLUMN_NAME_PROPERTY),
                                 columns.getString(JDBC_COLUMN_TYPE_PROPERTY), columns.getBoolean(JDBC_COLUMN_NULLABLE_PROPERTY), false,
                                 columns.getInt(JDBC_COLUMN_SIZE_PROPERTY), columns.getInt(JDBC_COLUMN_DECIMAL_DIGITS_PROPERTY)));
        } while (columns.next());
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
     * Adds the table type.
     *
     * @param databaseMetadata the database metadata
     * @param connection the connection
     * @param tableMetadata the table metadata
     * @param schemaPattern the schema pattern
     * @throws SQLException the SQL exception
     */
    public static void addTableType(DatabaseMetaData databaseMetadata, Connection connection, PersistenceTableModel tableMetadata,
            String schemaPattern) throws SQLException {
        ResultSet tables =
                databaseMetadata.getTables(connection.getCatalog(), schemaPattern, normalizeTableName(tableMetadata.getTableName()), null);
        if (tables.next()) {
            iterateTables(tableMetadata, tables);
        }
    }

    /**
     * Iterate tables.
     *
     * @param tableMetadata the table metadata
     * @param tables the tables
     * @throws SQLException the SQL exception
     */
    private static void iterateTables(PersistenceTableModel tableMetadata, ResultSet tables) throws SQLException {
        do {
            tableMetadata.setTableType(tables.getString("TABLE_TYPE"));
        } while (tables.next());
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
     * Gets the table schema.
     *
     * @param dataSource the data source
     * @param tableName the table name
     * @return the table schema
     * @throws SQLException the SQL exception
     */
    public static String getTableSchema(DataSource dataSource, String tableName) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            ResultSet rs = databaseMetaData.getTables(connection.getCatalog(), null, tableName, new String[] {ISqlKeywords.KEYWORD_TABLE});
            if (rs.next()) {
                return rs.getString("TABLE_SCHEM");
            }
            return null;
        }
    }

    /**
     * Gets the tables in schema.
     *
     * @param dataSource the data source
     * @param schemaName the schema name
     * @return the tables in schema
     * @throws SQLException the SQL exception
     */
    public static List<String> getTablesInSchema(DataSource dataSource, String schemaName) throws SQLException {
        List<String> tableNames = new ArrayList<String>();
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            ResultSet schemas = databaseMetaData.getSchemas(null, schemaName);
            if (schemas.next()) {

                ResultSet rs =
                        databaseMetaData.getTables(connection.getCatalog(), schemaName, null, new String[] {ISqlKeywords.KEYWORD_TABLE});
                while (rs.next()) {
                    tableNames.add(rs.getString("TABLE_NAME"));
                }
                return tableNames;
            }
        }
        return null;
    }

    public static Set<String> getTableDependencies(Set<String> tables, String schema, DirigibleDataSource dataSource) throws SQLException {
        Set<String> dependencies = new HashSet<>();
        for (String table : tables) {
            Set<String> tableDependencies = getTableDependencies(table, schema, dataSource);
            dependencies.addAll(tableDependencies);
        }
        return dependencies;
    }

    public static Set<String> getTableDependencies(String table, String schema, DirigibleDataSource dataSource) throws SQLException {
        Set<String> processedTables = new HashSet<>();
        return getTableDependencies(table, schema, dataSource, processedTables);
    }

    public static Set<String> getTableDependencies(String table, String schema, DirigibleDataSource dataSource, Set<String> processedTables)
            throws SQLException {
        Set<String> dependencies = new HashSet<>();
        PersistenceTableModel tableMetadata = DatabaseMetadataUtil.getTableMetadata(table, schema, dataSource);
        List<PersistenceTableRelationModel> relations = tableMetadata.getRelations();
        if (null != relations) {
            Set<String> tableDependencies = new HashSet<>(relations.stream()
                                                                   .map(m -> m.getToTableName())
                                                                   .collect(Collectors.toSet()));
            tableDependencies.remove(table);// sometimes current table is returned as well
            dependencies.addAll(tableDependencies);
        }

        // get dependencies of the dependencies
        Set<String> uncheckedDependencies = new HashSet<>(dependencies);
        uncheckedDependencies.removeAll(processedTables);

        for (String uncheckedDependency : uncheckedDependencies) {
            dependencies.addAll(getTableDependencies(uncheckedDependency, schema, dataSource, processedTables));
            processedTables.add(uncheckedDependency);
        }

        return dependencies;
    }

}
