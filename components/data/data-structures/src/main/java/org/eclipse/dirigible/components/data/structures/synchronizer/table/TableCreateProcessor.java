/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.structures.synchronizer.table;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.dirigible.components.data.structures.domain.Table;
import org.eclipse.dirigible.components.data.structures.domain.TableColumn;
import org.eclipse.dirigible.components.data.structures.domain.TableConstraintCheck;
import org.eclipse.dirigible.components.data.structures.domain.TableConstraintForeignKey;
import org.eclipse.dirigible.components.data.structures.domain.TableConstraintUnique;
import org.eclipse.dirigible.components.data.structures.domain.TableIndex;
import org.eclipse.dirigible.database.sql.DataType;
import org.eclipse.dirigible.database.sql.ISqlKeywords;
import org.eclipse.dirigible.database.sql.SqlFactory;
import org.eclipse.dirigible.database.sql.builders.table.CreateTableBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Table Create Processor.
 */
public class TableCreateProcessor {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(TableCreateProcessor.class);

    /**
     * Execute the corresponding statement.
     *
     * @param connection the connection
     * @param tableModel the table model
     * @throws SQLException the SQL exception
     */
    public static void execute(Connection connection, Table tableModel) throws SQLException {
        execute(connection, tableModel, false);
    }

    /**
     * Execute the corresponding statement.
     *
     * @param connection the connection
     * @param tableModel the table model
     * @param skipForeignKeys the skip foreign keys
     * @throws SQLException the SQL exception
     */
    public static void execute(Connection connection, Table tableModel, boolean skipForeignKeys) throws SQLException {
        String tableName = "\"" + tableModel.getName() + "\"";

        logger.debug("Processing Create Table: [{}]", tableName);
        CreateTableBuilder createTableBuilder = SqlFactory.getNative(connection)
                                                          .create()
                                                          .table(tableName);

        createTableBuilder.schema(tableModel.getSchema());

        List<TableColumn> columns = tableModel.getColumns();
        List<TableIndex> indexes = tableModel.getIndexes();
        for (TableColumn columnModel : columns) {
            String name = "\"" + columnModel.getName() + "\"";

            DataType type = DataType.valueOfByName(columnModel.getType());
            String length = columnModel.getLength();
            boolean isNullable = columnModel.isNullable();
            boolean isPrimaryKey = columnModel.isPrimaryKey();
            boolean isUnique = columnModel.isUnique();
            boolean autoincrement = columnModel.isAutoincrement();
            String defaultValue = columnModel.getDefaultValue();
            String scale = columnModel.getScale();
            String precision = columnModel.getPrecision();
            String args = "";
            if (type.equals(DataType.DECIMAL)) {
                if (precision == null && scale == null) {
                    // don't add brackets = use default DB values for precision and scale
                } else if (precision != null && scale != null) {
                    args = ISqlKeywords.OPEN + precision + "," + scale + ISqlKeywords.CLOSE;
                } else {
                    throw new IllegalArgumentException(
                            "Missing scale or precision for decimal column [" + name + "] in table " + tableModel.getName());
                }
            }
            if (type.equals(DataType.VARCHAR) || type.equals(DataType.CHAR) || type.equals(DataType.NVARCHAR)
                    || type.equals(DataType.CHARACTER_VARYING)) {
                if (length != null) {
                    args = ISqlKeywords.OPEN + length + ISqlKeywords.CLOSE;
                }
            }
            if (defaultValue != null) {
                if ("".equals(defaultValue)) {
                    if (type.equals(DataType.VARCHAR) || type.equals(DataType.CHAR) || type.equals(DataType.NVARCHAR)
                            || type.equals(DataType.CHARACTER_VARYING)) {
                        args += " DEFAULT '" + defaultValue + "' ";
                    }
                } else {
                    args += " DEFAULT " + defaultValue + " ";
                }
            }
            createTableBuilder.column(name, type, isPrimaryKey, isNullable, isUnique, autoincrement, false, false, args);
        }
        if (tableModel.getConstraints() != null) {
            if (tableModel.getConstraints()
                          .getPrimaryKey() != null) {
                String[] primaryKeyColumns = new String[tableModel.getConstraints()
                                                                  .getPrimaryKey()
                                                                  .getColumns().length];
                int i = 0;
                for (String column : tableModel.getConstraints()
                                               .getPrimaryKey()
                                               .getColumns()) {
                    primaryKeyColumns[i++] = "\"" + column + "\"";
                }

                createTableBuilder.primaryKey(primaryKeyColumns);
            }
            if (!skipForeignKeys) {
                if (tableModel.getConstraints()
                              .getForeignKeys() != null
                        && !tableModel.getConstraints()
                                      .getForeignKeys()
                                      .isEmpty()) {
                    for (TableConstraintForeignKey foreignKey : tableModel.getConstraints()
                                                                          .getForeignKeys()) {
                        String foreignKeyName = "\"" + foreignKey.getName() + "\"";

                        String[] foreignKeyColumns = new String[foreignKey.getColumns().length];
                        int i = 0;
                        for (String column : foreignKey.getColumns()) {
                            foreignKeyColumns[i++] = "\"" + column + "\"";
                        }
                        String foreignKeyReferencedTable = "\"" + foreignKey.getReferencedTable() + "\"";

                        String[] foreignKeyReferencedColumns = new String[foreignKey.getReferencedColumns().length];
                        i = 0;
                        for (String column : foreignKey.getReferencedColumns()) {
                            foreignKeyReferencedColumns[i++] = "\"" + column + "\"";
                        }

                        String foreignKeyReferencedSchema = foreignKey.getReferencedSchema();
                        createTableBuilder.foreignKey(foreignKeyName, foreignKeyColumns, foreignKeyReferencedTable,
                                foreignKeyReferencedSchema, foreignKeyReferencedColumns);
                    }
                }
            }
            if (tableModel.getConstraints()
                          .getUniqueIndexes() != null) {
                for (TableConstraintUnique uniqueIndex : tableModel.getConstraints()
                                                                   .getUniqueIndexes()) {
                    String uniqueIndexName = "\"" + uniqueIndex.getName() + "\"";

                    List<String> uniqueIndexColumns = new ArrayList<>();
                    for (String column : uniqueIndex.getColumns()) {
                        TableColumn definedColumn = tableModel.getColumn(column);
                        if (null != definedColumn && (definedColumn.isUnique() || definedColumn.isPrimaryKey())) {
                            logger.debug(
                                    "Skipping creating index for column [{}] since it is marked as unique or primary key. The index will be automatically created when creating the table.",
                                    column);
                            continue;
                        }

                        String columnValue = "\"" + column + "\"";
                        uniqueIndexColumns.add(columnValue);
                    }

                    if (!uniqueIndexColumns.isEmpty()) {
                        createTableBuilder.unique(uniqueIndexName, uniqueIndexColumns.toArray(new String[0]));
                    }
                }
            }
            if (tableModel.getConstraints()
                          .getChecks() != null) {
                for (TableConstraintCheck check : tableModel.getConstraints()
                                                            .getChecks()) {
                    String checkName = "\"" + check.getName() + "\"";

                    String expression = check.getExpression();
                    if (null == expression) {
                        logger.debug("Skipping check constraint [{}] since the expression is missing:[{}]", check.getName(), expression);
                        continue;
                    }
                    createTableBuilder.check(checkName, expression);
                }
            }
        }
        if (indexes != null) {
            for (TableIndex indexModel : indexes) {
                String name = indexModel.getName();
                String type = indexModel.getType();
                Boolean isUnique = indexModel.isUnique();
                String order = indexModel.getOrder();
                Set<String> indexColumns = Set.of(indexModel.getColumns());
                createTableBuilder.index(name, isUnique, order, type, indexColumns);
            }
        }

        final String sql = createTableBuilder.build();
        logger.info("Create SQL: [{}]", sql);

        String[] parts = sql.split(CreateTableBuilder.STATEMENT_DELIMITER);
        for (String part : parts) {
            PreparedStatement statement = connection.prepareStatement(part);
            try {
                statement.executeUpdate();
            } catch (SQLException e) {
                logger.error("Failed to execute [{}]", part, e);
                throw new SQLException(e.getMessage(), e);
            } finally {
                if (statement != null) {
                    statement.close();
                }
            }
        }
    }

}
