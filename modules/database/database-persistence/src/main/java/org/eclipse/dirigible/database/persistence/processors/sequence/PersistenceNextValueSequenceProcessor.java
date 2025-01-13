/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.persistence.processors.sequence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.eclipse.dirigible.database.persistence.IEntityManagerInterceptor;
import org.eclipse.dirigible.database.persistence.PersistenceException;
import org.eclipse.dirigible.database.persistence.model.PersistenceTableModel;
import org.eclipse.dirigible.database.persistence.parser.Serializer;
import org.eclipse.dirigible.database.persistence.processors.AbstractPersistenceProcessor;
import org.eclipse.dirigible.database.sql.ISqlKeywords;
import org.eclipse.dirigible.database.sql.SqlFactory;
import org.eclipse.dirigible.database.sql.builders.sequence.NextValueSequenceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Persistence Next Value Sequence Processor.
 */
public class PersistenceNextValueSequenceProcessor extends AbstractPersistenceProcessor {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(PersistenceNextValueSequenceProcessor.class);

    /**
     * Instantiates a new persistence next value sequence processor.
     *
     * @param entityManagerInterceptor the entity manager interceptor
     */
    public PersistenceNextValueSequenceProcessor(IEntityManagerInterceptor entityManagerInterceptor) {
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
        NextValueSequenceBuilder nextValueBuilder = SqlFactory.getNative(SqlFactory.deriveDialect(connection))
                                                              .nextval(tableModel.getTableName() + ISqlKeywords.UNDERSCROE
                                                                      + ISqlKeywords.KEYWORD_SEQUENCE);

        String sql = nextValueBuilder.toString();
        if (logger.isTraceEnabled()) {
            logger.trace(sql);
        }
        return sql;
    }

    /**
     * Nextval.
     *
     * @param connection the connection
     * @param tableModel the table model
     * @return the long
     * @throws PersistenceException the persistence exception
     */
    public long nextval(Connection connection, PersistenceTableModel tableModel) throws PersistenceException {
        if (logger.isTraceEnabled()) {
            logger.trace("nextval -> connection: " + connection.hashCode() + ", tableModel: " + Serializer.serializeTableModel(tableModel));
        }
        long result = -1;
        String sql = null;
        PreparedStatement preparedStatement = null;
        try {
            sql = generateScript(connection, tableModel);
            preparedStatement = openPreparedStatement(connection, sql);
            ResultSet resultSet = null;
            try {
                resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    result = resultSet.getLong(1);
                    return result;
                }
            } finally {
                if (resultSet != null) {
                    resultSet.close();
                }
            }
        } catch (Exception e) {
            throw new PersistenceException(sql, e);
        } finally {
            closePreparedStatement(preparedStatement);
        }
        return result;
    }

}
