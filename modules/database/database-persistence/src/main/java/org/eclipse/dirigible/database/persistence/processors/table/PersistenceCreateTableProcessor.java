/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.persistence.processors.table;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.eclipse.dirigible.database.persistence.IEntityManagerInterceptor;
import org.eclipse.dirigible.database.persistence.PersistenceException;
import org.eclipse.dirigible.database.persistence.model.PersistenceTableColumnModel;
import org.eclipse.dirigible.database.persistence.model.PersistenceTableModel;
import org.eclipse.dirigible.database.persistence.parser.Serializer;
import org.eclipse.dirigible.database.persistence.processors.AbstractPersistenceProcessor;
import org.eclipse.dirigible.database.sql.DataType;
import org.eclipse.dirigible.database.sql.SqlFactory;
import org.eclipse.dirigible.database.sql.builders.table.CreateTableBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Persistence Create Table Processor.
 */
public class PersistenceCreateTableProcessor extends AbstractPersistenceProcessor {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(PersistenceCreateTableProcessor.class);

    /**
     * Instantiates a new persistence create table processor.
     *
     * @param entityManagerInterceptor the entity manager interceptor
     */
    public PersistenceCreateTableProcessor(IEntityManagerInterceptor entityManagerInterceptor) {
        super(entityManagerInterceptor);
    }

    /**
     * Generate script.
     *
     * @param connection the connection
     * @param tableModel the table model
     * @return the string
     */
    @Override
    protected String generateScript(Connection connection, PersistenceTableModel tableModel) {
        CreateTableBuilder createTableBuilder = SqlFactory.getNative(SqlFactory.deriveDialect(connection))
                                                          .create()
                                                          .table(tableModel.getTableName());
        for (PersistenceTableColumnModel columnModel : tableModel.getColumns()) {
            DataType dataType;
            dataType = DataType.valueOfByName(columnModel.getType());
            switch (dataType) {
                case VARCHAR:
                    createTableBuilder.columnVarchar(columnModel.getName(), columnModel.getLength(), columnModel.isPrimaryKey(),
                            columnModel.isNullable(), columnModel.isUnique(), columnModel.isIdentity());
                    break;
                case CHARACTER_VARYING:
                    createTableBuilder.columnVarchar(columnModel.getName(), columnModel.getLength(), columnModel.isPrimaryKey(),
                            columnModel.isNullable(), columnModel.isUnique(), columnModel.isIdentity());
                    break;
                case NVARCHAR:
                    createTableBuilder.columnNvarchar(columnModel.getName(), columnModel.getLength(), columnModel.isPrimaryKey(),
                            columnModel.isNullable(), columnModel.isUnique(), columnModel.isIdentity());
                    break;
                case CHAR:
                    createTableBuilder.columnChar(columnModel.getName(), columnModel.getLength(), columnModel.isPrimaryKey(),
                            columnModel.isNullable(), columnModel.isUnique(), columnModel.isIdentity());
                    break;
                case CHARACTER:
                    createTableBuilder.columnChar(columnModel.getName(), columnModel.getLength(), columnModel.isPrimaryKey(),
                            columnModel.isNullable(), columnModel.isUnique(), columnModel.isIdentity());
                    break;
                case DATE:
                    createTableBuilder.columnDate(columnModel.getName(), columnModel.isPrimaryKey(), columnModel.isNullable(),
                            columnModel.isUnique());
                    break;
                case TIME:
                    createTableBuilder.columnTime(columnModel.getName(), columnModel.isPrimaryKey(), columnModel.isNullable(),
                            columnModel.isUnique());
                    break;
                case TIMESTAMP:
                    createTableBuilder.columnTimestamp(columnModel.getName(), columnModel.isPrimaryKey(), columnModel.isNullable(),
                            columnModel.isUnique());
                    break;
                case INTEGER:
                    createTableBuilder.columnInteger(columnModel.getName(), columnModel.isPrimaryKey(), columnModel.isNullable(),
                            columnModel.isUnique(), columnModel.isIdentity());
                    break;
                case TINYINT:
                    createTableBuilder.columnTinyint(columnModel.getName(), columnModel.isPrimaryKey(), columnModel.isNullable(),
                            columnModel.isUnique());
                    break;
                case BIGINT:
                    createTableBuilder.columnBigint(columnModel.getName(), columnModel.isPrimaryKey(), columnModel.isNullable(),
                            columnModel.isUnique(), columnModel.isIdentity());
                    break;
                case SMALLINT:
                    createTableBuilder.columnSmallint(columnModel.getName(), columnModel.isPrimaryKey(), columnModel.isNullable(),
                            columnModel.isUnique());
                    break;
                case REAL:
                    createTableBuilder.columnReal(columnModel.getName(), columnModel.isPrimaryKey(), columnModel.isNullable(),
                            columnModel.isUnique());
                    break;
                case FLOAT:
                    createTableBuilder.columnFloat(columnModel.getName(), columnModel.isPrimaryKey(), columnModel.isNullable(),
                            columnModel.isUnique());
                    break;
                case DOUBLE:
                    createTableBuilder.columnDouble(columnModel.getName(), columnModel.isPrimaryKey(), columnModel.isNullable(),
                            columnModel.isUnique());
                    break;
                case DOUBLE_PRECISION:
                    createTableBuilder.columnDouble(columnModel.getName(), columnModel.isPrimaryKey(), columnModel.isNullable(),
                            columnModel.isUnique());
                    break;
                case BOOLEAN:
                    createTableBuilder.columnBoolean(columnModel.getName(), columnModel.isPrimaryKey(), columnModel.isNullable(),
                            columnModel.isUnique());
                    break;
                case BLOB:
                    createTableBuilder.columnBlob(columnModel.getName(), columnModel.isNullable());
                    break;
                case CLOB:
                    createTableBuilder.columnBlob(columnModel.getName(), columnModel.isNullable());
                    break;
                case DECIMAL:
                    createTableBuilder.columnDecimal(columnModel.getName(), columnModel.getLength(), columnModel.getScale(),
                            columnModel.isPrimaryKey(), columnModel.isNullable(), columnModel.isUnique(), columnModel.isIdentity());
                    break;
                case BIT:
                    createTableBuilder.columnBit(columnModel.getName(), columnModel.isNullable());
                    break;
                case VARBINARY:
                    createTableBuilder.columnVarbinary(columnModel.getName(), columnModel.isNullable());
                    break;
                case BINARY_LARGE_OBJECT:
                    createTableBuilder.columnVarbinary(columnModel.getName(), columnModel.isNullable());
                    break;
                case CHARACTER_LARGE_OBJECT:
                    createTableBuilder.columnVarbinary(columnModel.getName(), columnModel.isNullable());
                    break;
                default:
                    throw new PersistenceException("Unknown data type: " + dataType);
            }
        }

        String sql = createTableBuilder.build();
        if (logger.isTraceEnabled()) {
            logger.trace(sql);
        }
        return sql;
    }

    /**
     * Creates the table.
     *
     * @param connection the connection
     * @param tableModel the table model
     * @return the int
     * @throws PersistenceException the persistence exception
     */
    public int create(Connection connection, PersistenceTableModel tableModel) throws PersistenceException {
        if (logger.isTraceEnabled()) {
            logger.trace("create -> connection: " + connection.hashCode() + ", tableModel: " + Serializer.serializeTableModel(tableModel));
        }
        int result = 0;
        String sql = null;
        PreparedStatement preparedStatement = null;
        try {
            sql = generateScript(connection, tableModel);
            preparedStatement = openPreparedStatement(connection, sql);
            result = preparedStatement.executeUpdate();
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error(sql);
            }
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
            throw new PersistenceException(sql, e);
        } finally {
            closePreparedStatement(preparedStatement);
        }
        return result;
    }

}
