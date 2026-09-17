/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.structures.synchronizer.table;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.eclipse.dirigible.components.data.structures.domain.TableConstraintUnique;
import java.util.HashSet;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.dirigible.components.data.structures.domain.Table;
import org.eclipse.dirigible.components.data.structures.domain.TableColumn;
import org.eclipse.dirigible.components.database.DatabaseNameNormalizer;
import org.eclipse.dirigible.database.sql.DataType;
import org.eclipse.dirigible.database.sql.DataTypeUtils;
import org.eclipse.dirigible.database.sql.ISqlKeywords;
import org.eclipse.dirigible.database.sql.SqlException;
import org.eclipse.dirigible.database.sql.SqlFactory;
import org.eclipse.dirigible.database.sql.builders.table.AlterTableBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Table Alter Processor.
 */
public class TableAlterProcessor {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(TableAlterProcessor.class);

    /** The Constant INCOMPATIBLE_CHANGE_OF_TABLE. */
    private static final String INCOMPATIBLE_CHANGE_OF_TABLE = "Incompatible change of table [%s] by adding a column [%s] which is [%s]";
    // $NON-NLS-1$

    /**
     * Execute the corresponding statement.
     *
     * @param connection the connection
     * @param tableModel the table model
     * @throws SQLException the SQL exception
     */
    public static void execute(Connection connection, Table tableModel) throws SQLException {
        String tableName = "\"" + tableModel.getName() + "\"";

        logger.info("Processing Alter Table: " + tableName);

        Map<String, String> columnDefinitions = new HashMap<>();
        DatabaseMetaData dmd = connection.getMetaData();
        String schema = connection.getSchema();
        ResultSet rsColumns = dmd.getColumns(null, schema, DatabaseNameNormalizer.normalizeTableName(tableName), null);
        while (rsColumns.next()) {
            int columnType = rsColumns.getInt(5);
            String columnName = rsColumns.getString(4)
                                         .toUpperCase();
            try {
                String typeName = DataTypeUtils.getDatabaseTypeName(columnType);
                columnDefinitions.put(DatabaseNameNormalizer.normalizeColumnName(columnName), typeName);
            } catch (SqlException ex) {
                String errorMessage = "Missing type for column [" + columnName + "] and type [" + columnType + "]";
                throw new SqlException(errorMessage, ex);
            }
        }

        List<String> modelColumnNames = new ArrayList<>();

        // ADD iteration
        for (TableColumn columnModel : tableModel.getColumns()) {
            String name = DatabaseNameNormalizer.normalizeColumnName(columnModel.getName());

            DataType type = DataType.valueOfByName(columnModel.getType());
            String length = columnModel.getLength();
            boolean isNullable = columnModel.isNullable();
            boolean isPrimaryKey = columnModel.isPrimaryKey();
            boolean isUnique = columnModel.isUnique();
            String defaultValue = columnModel.getDefaultValue();
            String scale = columnModel.getScale();
            String args = "";
            if (length != null) {
                if (type.equals(DataType.VARCHAR) || type.equals(DataType.CHAR) || type.equals(DataType.NVARCHAR)
                        || type.equals(DataType.CHARACTER_VARYING) || type.equals(DataType.CHARACTER)) {
                    args = ISqlKeywords.OPEN + length + ISqlKeywords.CLOSE;
                }
                if (scale != null) {
                    if (type.equals(DataType.DECIMAL)) {
                        args = ISqlKeywords.OPEN + length + "," + scale + ISqlKeywords.CLOSE;
                    }
                }
            }
            if (defaultValue != null) {
                if ("".equals(defaultValue)) {
                    if (type.equals(DataType.VARCHAR) || type.equals(DataType.CHAR) || type.equals(DataType.NVARCHAR)
                            || type.equals(DataType.CHARACTER_VARYING) || type.equals(DataType.CHARACTER)) {
                        args += " DEFAULT '" + defaultValue + "' ";
                    }
                } else {
                    args += " DEFAULT " + defaultValue + " ";
                }

            }

            modelColumnNames.add(name.toUpperCase());

            String nameOriginalCanonical = name.toUpperCase();
            if (!columnDefinitions.containsKey(nameOriginalCanonical)) {

                AlterTableBuilder alterTableBuilder = SqlFactory.getNative(connection)
                                                                .alter()
                                                                .table(tableName);

                alterTableBuilder.add()
                                 .column("\"" + name + "\"", type, isPrimaryKey, isNullable, isUnique, args);

                if (!isNullable) {
                    logger.error("Column Definitions: {}", columnDefinitions);
                    throw new SQLException(String.format(INCOMPATIBLE_CHANGE_OF_TABLE, tableName, name, "NOT NULL"));
                }
                if (isPrimaryKey) {
                    logger.error("Column Definitions: {}", columnDefinitions);
                    throw new SQLException(String.format(INCOMPATIBLE_CHANGE_OF_TABLE, tableName, name, "PRIMARY KEY"));
                }

                executeAlterBuilder(connection, alterTableBuilder);

            } else {
                String typeFromMetadata = columnDefinitions.get(nameOriginalCanonical);
                String typeFromDefinition = type.toString();
                String unifiedTypeFromMetadata = DataTypeUtils.getUnifiedDatabaseType(typeFromMetadata);
                String unifiedTypeFromDefinition = DataTypeUtils.getUnifiedDatabaseType(typeFromDefinition);
                if (!unifiedTypeFromMetadata.equals(unifiedTypeFromDefinition)) {
                    if (!DataTypeUtils.isCharacterType(unifiedTypeFromMetadata)
                            || !DataTypeUtils.isCharacterType(unifiedTypeFromDefinition)) {
                        logger.error("Column Definitions: {}", columnDefinitions);
                        throw new SQLException(String.format(INCOMPATIBLE_CHANGE_OF_TABLE, tableName, name,
                                "of type " + typeFromMetadata + " to be changed to " + type));
                    }
                    // Both are character types, so the existing column can hold the same kind of value and
                    // is left as it is - the alternative, failing the whole table, drowns the real
                    // incompatible changes in noise on every re-publish over an existing database.
                    logger.warn(
                            "Column [{}] of table [{}] is [{}] in the database while it is defined as [{}]. The column is kept as it is.",
                            name, tableName, typeFromMetadata, typeFromDefinition);
                }
            }
        }

        // DROP iteration
        for (String columnName : columnDefinitions.keySet()) {
            if (!modelColumnNames.contains(columnName.toUpperCase())) {
                AlterTableBuilder alterTableBuilder = SqlFactory.getNative(connection)
                                                                .alter()
                                                                .table(tableName);
                alterTableBuilder.drop()
                                 .column("\"" + columnName + "\"", DataType.BOOLEAN);
                executeAlterBuilder(connection, alterTableBuilder);
            }
        }
        reconcileUniqueConstraints(connection, tableName, tableModel);
    }

    /**
     * Brings the table's UNIQUE constraints in line with the model - the half of schema evolution the
     * column pass above never covered (#7019). A key the model declares (a {@code unique} column or a
     * composite {@code uniqueIndexes} entry) that the database lacks is ADDED; a UNIQUE the database
     * enforces that the model no longer declares is DROPPED - the same policy the column pass applies
     * to undeclared columns. Keys are compared as column SETS, so a differently named but equal key is
     * left alone and a second run issues nothing. PRIMARY KEY and FOREIGN KEY constraints are never
     * touched.
     *
     * <p>
     * Fails soft: a dialect without a catalog of unique constraints skips the step (no change from
     * before), and a statement the database refuses - duplicate rows already present, a foreign key
     * that depends on the unique - is logged with the table and key, without failing the column
     * reconciliation or the publish.
     *
     * @param connection the connection
     * @param quotedTableName the table name as the alter builder wants it (quoted)
     * @param tableModel the model
     */
    static void reconcileUniqueConstraints(Connection connection, String quotedTableName, Table tableModel) {
        String tableName = DatabaseNameNormalizer.normalizeTableName(quotedTableName);
        Map<String, List<String>> existing;
        try {
            existing = SqlFactory.getNative(connection)
                                 .uniqueConstraints(connection, tableName);
        } catch (SQLException e) {
            logger.warn("Unique constraints of table [{}] are not reconciled - the catalog read failed: {}", tableName, e.getMessage());
            return;
        }
        if (existing == null) {
            logger.info("Unique constraints of table [{}] are not reconciled - this database exposes no catalog of them", tableName);
            return;
        }
        existing.replaceAll((name, columns) -> columns.stream()
                                                      .map(String::toUpperCase)
                                                      .toList());
        Map<Set<String>, DesiredUnique> desired = new LinkedHashMap<>();
        for (TableColumn column : tableModel.getColumns()) {
            if (column.isUnique() && !column.isPrimaryKey()) {
                String columnName = DatabaseNameNormalizer.normalizeColumnName(column.getName())
                                                          .toUpperCase();
                desired.putIfAbsent(Set.of(columnName), new DesiredUnique(tableName + "_" + columnName + "_UNIQUE", List.of(columnName)));
            }
        }
        if (tableModel.getConstraints() != null && tableModel.getConstraints()
                                                             .getUniqueIndexes() != null) {
            for (TableConstraintUnique unique : tableModel.getConstraints()
                                                          .getUniqueIndexes()) {
                if (unique.getColumns() == null || unique.getColumns().length == 0) {
                    continue;
                }
                List<String> columns = new ArrayList<>(unique.getColumns().length);
                for (String column : unique.getColumns()) {
                    columns.add(DatabaseNameNormalizer.normalizeColumnName(column)
                                                      .toUpperCase());
                }
                String name = unique.getName() != null && !unique.getName()
                                                                 .isBlank() ? unique.getName()
                                                                         : tableName + "_" + String.join("_", columns) + "_UNIQUE";
                desired.putIfAbsent(new HashSet<>(columns), new DesiredUnique(name, columns));
            }
        }
        Set<Set<String>> present = new HashSet<>();
        for (Map.Entry<String, List<String>> constraint : existing.entrySet()) {
            Set<String> columns = new HashSet<>(constraint.getValue());
            if (desired.containsKey(columns)) {
                present.add(columns);
                continue;
            }
            logger.warn("Dropping unique constraint [{}] on table [{}] over {} - the model no longer declares it", constraint.getKey(),
                    tableName, constraint.getValue());
            AlterTableBuilder drop = SqlFactory.getNative(connection)
                                               .alter()
                                               .table(quotedTableName);
            drop.drop()
                .unique(constraint.getKey(), constraint.getValue()
                                                       .toArray(new String[0]));
            executeConstraintChange(connection, drop, tableName, constraint.getKey());
        }
        for (Map.Entry<Set<String>, DesiredUnique> key : desired.entrySet()) {
            if (present.contains(key.getKey())) {
                continue;
            }
            DesiredUnique unique = key.getValue();
            logger.info("Adding unique constraint [{}] on table [{}] over {}", unique.name(), tableName, unique.columns());
            AlterTableBuilder add = SqlFactory.getNative(connection)
                                              .alter()
                                              .table(quotedTableName);
            add.add()
               .unique(unique.name(), unique.columns()
                                            .toArray(new String[0]));
            executeConstraintChange(connection, add, tableName, unique.name());
        }
    }

    /** A key the model wants: its constraint name and its columns in the declared order. */
    private record DesiredUnique(String name, List<String> columns) {
    }

    /**
     * One ADD/DROP CONSTRAINT statement, fail-soft (see {@link #reconcileUniqueConstraints}).
     *
     * @param connection the connection
     * @param builder the built statement
     * @param tableName the table, for the log
     * @param constraint the constraint, for the log
     */
    private static void executeConstraintChange(Connection connection, AlterTableBuilder builder, String tableName, String constraint) {
        String sql = builder.build();
        if (logger.isInfoEnabled()) {
            logger.info(sql);
        }
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        } catch (SQLException e) {
            logger.error("Unique constraint [{}] on table [{}] could not be reconciled - the table's other changes stand. Statement: [{}]",
                    constraint, tableName, sql, e);
        }
    }

    /**
     * Execute alter builder.
     *
     * @param connection the connection
     * @param alterTableBuilder the alter table builder
     * @throws SQLException the SQL exception
     */
    private static void executeAlterBuilder(Connection connection, AlterTableBuilder alterTableBuilder) throws SQLException {
        final String sql = alterTableBuilder.build();
        if (logger.isInfoEnabled()) {
            logger.info(sql);
        }
        PreparedStatement statement = connection.prepareStatement(sql);
        try {
            statement.executeUpdate();
        } catch (SQLException e) {
            if (logger.isErrorEnabled()) {
                logger.error(sql);
            }
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
            throw new SQLException(e.getMessage(), e);
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

}
