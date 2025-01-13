/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.csvim.processor;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.dirigible.commons.api.helpers.DateTimeUtils;
import org.eclipse.dirigible.components.data.csvim.domain.CsvFile;
import org.eclipse.dirigible.components.data.csvim.domain.CsvRecord;
import org.eclipse.dirigible.components.data.csvim.utils.CsvimUtils;
import org.eclipse.dirigible.components.data.management.domain.ColumnMetadata;
import org.eclipse.dirigible.components.data.management.domain.TableMetadata;
import org.eclipse.dirigible.database.persistence.PersistenceException;
import org.eclipse.dirigible.database.sql.DataTypeUtils;
import org.eclipse.dirigible.database.sql.SqlFactory;
import org.eclipse.dirigible.database.sql.builders.records.InsertBuilder;
import org.eclipse.dirigible.database.sql.builders.records.UpdateBuilder;
import org.eclipse.dirigible.database.sql.dialects.SqlDialectFactory;
import org.eclipse.dirigible.database.sql.dialects.postgres.PostgresSqlDialect;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The Class CsvProcessor.
 */
@Component
public class CsvProcessor {

    /**
     * The Constant MODULE.
     */
    private static final String MODULE = "dirigible-cms-csv";

    /**
     * The Constant ERROR_TYPE_PROCESSOR.
     */
    private static final String ERROR_TYPE_PROCESSOR = "PROCESSOR";

    /**
     * The Constant logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(CsvProcessor.class);

    /**
     * Insert.
     *
     * @param connection the connection
     * @param schema the schema
     * @param tableMetadata the table metadata
     * @param csvRecords the csv records
     * @param headerNames the header names
     * @param csvFile the csv file
     * @throws SQLException the SQL exception
     */
    public void insert(Connection connection, String schema, TableMetadata tableMetadata, List<CsvRecord> csvRecords,
            List<String> headerNames, CsvFile csvFile) throws SQLException {
        if (csvRecords.isEmpty()) {
            logger.info("Skipping import - CSV records are empty for csv file [{}].", csvFile);
            return;
        }

        if (tableMetadata == null) {
            logger.warn("Missing table metadata for file [{}] on insert", csvFile);
            return;
        }
        if (null != schema) {
            connection.setSchema(schema);
        }
        logger.info("Will insert data into table [{}] in schema [{}]", tableMetadata.getName(), schema);
        List<ColumnMetadata> availableTableColumns = tableMetadata.getColumns();
        InsertBuilder insertBuilder = new InsertBuilder(SqlFactory.deriveDialect(connection));
        insertBuilder.into(tableMetadata.getName());

        for (ColumnMetadata columnMetadata : availableTableColumns) {
            insertBuilder.column("\"" + columnMetadata.getName() + "\"")
                         .value("?");
        }
        try (PreparedStatement preparedStatement = connection.prepareStatement(insertBuilder.generate())) {
            for (CsvRecord next : csvRecords) {
                populateInsertPreparedStatementValues(next, availableTableColumns, preparedStatement);
                preparedStatement.addBatch();
            }
            logger.info(String.format("CSV records with Ids [%s] were successfully added in BATCH INSERT for table [%s].",
                    csvRecords.stream()
                              .map(e -> e.getCsvRecord()
                                         .get(0))
                              .collect(Collectors.toList()),
                    tableMetadata.getName()));
            preparedStatement.executeBatch();
        } catch (Throwable t) {
            String errorMessage = String.format(
                    "Error occurred while trying to BATCH INSERT CSV records [%s] into table [%s].", csvRecords.stream()
                                                                                                               .map(e -> e.getCsvRecord()
                                                                                                                          .get(0))
                                                                                                               .collect(
                                                                                                                       Collectors.toList()),
                    tableMetadata.getName());
            CsvimUtils.logProcessorErrors(errorMessage, ERROR_TYPE_PROCESSOR, csvFile.getFile(), CsvFile.ARTEFACT_TYPE, MODULE);
            logger.error(errorMessage, t);
        }
    }

    /**
     * Update.
     *
     * @param connection the connection
     * @param schema the schema
     * @param tableMetadata the table metadata
     * @param csvRecords the csv records
     * @param headerNames the header names
     * @param pkName the pk name
     * @param csvFile the csv file
     * @throws SQLException the SQL exception
     */
    public void update(Connection connection, String schema, TableMetadata tableMetadata, List<CsvRecord> csvRecords,
            List<String> headerNames, String pkName, CsvFile csvFile) throws SQLException {
        if (csvRecords.isEmpty()) {
            logger.info("Skipping update - CSV records are empty for csv file [{}].", csvFile);
            return;
        }
        if (tableMetadata == null) {
            logger.warn("Missing table metadata for file [{}] on update", csvFile);
            return;
        }
        if (null != schema) {
            connection.setSchema(schema);
        }
        logger.info("Will update data into table [{}] in schema [{}]", tableMetadata.getName(), schema);
        List<ColumnMetadata> availableTableColumns = tableMetadata.getColumns();
        UpdateBuilder updateBuilder = new UpdateBuilder(SqlFactory.deriveDialect(connection));
        updateBuilder.table(tableMetadata.getName());

        for (ColumnMetadata columnMetadata : availableTableColumns) {
            if (columnMetadata.getName()
                              .equals(pkName)) {
                continue;
            }

            updateBuilder.set("\"" + columnMetadata.getName() + "\"", "?");
        }

        if (pkName != null) {
            updateBuilder.where(String.format("%s = ?", pkName));
        } else {
            updateBuilder.where(String.format("%s = ?", availableTableColumns.get(0)
                                                                             .getName()));
        }

        try (PreparedStatement preparedStatement = connection.prepareStatement(updateBuilder.generate())) {
            for (CsvRecord next : csvRecords) {
                executeUpdatePreparedStatement(next, availableTableColumns, preparedStatement);
                preparedStatement.addBatch();
            }
            if (logger.isInfoEnabled()) {
                logger.info(String.format("CSV records with Ids [%s] were successfully added in BATCH UPDATED for table [%s].",
                        csvRecords.stream()
                                  .map(e -> e.getCsvRecord()
                                             .get(0))
                                  .collect(Collectors.toList()),
                        tableMetadata.getName()));
            }
            preparedStatement.executeBatch();
        } catch (Throwable t) {
            String errorMessage = String.format(
                    "Error occurred while trying to BATCH UPDATE CSV records [%s] into table [%s].", csvRecords.stream()
                                                                                                               .map(e -> e.getCsvRecord()
                                                                                                                          .get(0))
                                                                                                               .collect(
                                                                                                                       Collectors.toList()),
                    tableMetadata.getName());
            CsvimUtils.logProcessorErrors(errorMessage, ERROR_TYPE_PROCESSOR, csvFile.getFile(), CsvFile.ARTEFACT_TYPE, MODULE);
            if (logger.isErrorEnabled()) {
                logger.error(errorMessage, t);
            }
        }
    }

    /**
     * Populate insert prepared statement values.
     *
     * @param csvRecord the csv record
     * @param tableColumns the table columns
     * @param statement the statement
     * @throws SQLException the SQL exception
     */
    private void populateInsertPreparedStatementValues(CsvRecord csvRecord, List<ColumnMetadata> tableColumns, PreparedStatement statement)
            throws SQLException {
        if (csvRecord.getHeaderNames()
                     .size() > 0) {
            insertCsvWithHeader(csvRecord, tableColumns, statement);
        } else {
            insertCsvWithoutHeader(csvRecord, tableColumns, statement);
        }
    }

    /**
     * Execute update prepared statement.
     *
     * @param csvRecord the csv record
     * @param tableColumns the table columns
     * @param statement the statement
     * @throws SQLException the SQL exception
     */
    private void executeUpdatePreparedStatement(CsvRecord csvRecord, List<ColumnMetadata> tableColumns, PreparedStatement statement)
            throws SQLException {
        if (csvRecord.getHeaderNames()
                     .size() > 0) {
            updateCsvWithHeader(csvRecord, tableColumns, statement);
        } else {
            updateCsvWithoutHeader(csvRecord, tableColumns, statement);
        }

        statement.execute();
    }

    /**
     * Insert csv with header.
     *
     * @param csvRecord the csv record
     * @param tableColumns the table columns
     * @param statement the statement
     * @throws SQLException the SQL exception
     */
    private void insertCsvWithHeader(CsvRecord csvRecord, List<ColumnMetadata> tableColumns, PreparedStatement statement)
            throws SQLException {
        for (int i = 0; i < tableColumns.size(); i++) {
            String columnName = tableColumns.get(i)
                                            .getName();
            String columnType = tableColumns.get(i)
                                            .getType();
            String value = csvRecord.getCsvValueForColumn(columnName);
            setPreparedStatementValue(csvRecord.isDistinguishEmptyFromNull(), statement, i + 1, value, columnType);
        }
    }

    /**
     * Insert csv without header.
     *
     * @param csvRecord the csv record
     * @param tableColumns the table columns
     * @param statement the statement
     * @throws SQLException the SQL exception
     */
    private void insertCsvWithoutHeader(CsvRecord csvRecord, List<ColumnMetadata> tableColumns, PreparedStatement statement)
            throws SQLException {
        for (int i = 0; i < csvRecord.getCsvRecord()
                                     .size(); i++) {
            String value = csvRecord.getCsvRecord()
                                    .get(i);
            String columnType = tableColumns.get(i)
                                            .getType();

            setPreparedStatementValue(csvRecord.isDistinguishEmptyFromNull(), statement, i + 1, value, columnType);
        }
    }

    /**
     * Update csv with header.
     *
     * @param csvRecord the csv record
     * @param tableColumns the table columns
     * @param statement the statement
     * @throws SQLException the SQL exception
     */
    private void updateCsvWithHeader(CsvRecord csvRecord, List<ColumnMetadata> tableColumns, PreparedStatement statement)
            throws SQLException {
        CSVRecord existingCsvRecord = csvRecord.getCsvRecord();

        for (int i = 1; i < tableColumns.size(); i++) {
            String columnName = tableColumns.get(i)
                                            .getName();
            String value = csvRecord.getCsvValueForColumn(columnName);
            String columnType = tableColumns.get(i)
                                            .getType();

            setPreparedStatementValue(csvRecord.isDistinguishEmptyFromNull(), statement, i, value, columnType);
        }

        String pkColumnType = tableColumns.get(0)
                                          .getType();
        int lastStatementPlaceholderIndex = existingCsvRecord.size();

        setValue(statement, lastStatementPlaceholderIndex, pkColumnType, csvRecord.getCsvRecordPkValue());
    }

    /**
     * Update csv without header.
     *
     * @param csvRecord the csv record
     * @param tableColumns the table columns
     * @param statement the statement
     * @throws SQLException the SQL exception
     */
    private void updateCsvWithoutHeader(CsvRecord csvRecord, List<ColumnMetadata> tableColumns, PreparedStatement statement)
            throws SQLException {
        CSVRecord existingCsvRecord = csvRecord.getCsvRecord();
        for (int i = 1; i < existingCsvRecord.size(); i++) {
            String value = existingCsvRecord.get(i);
            String columnType = tableColumns.get(i)
                                            .getType();
            setPreparedStatementValue(csvRecord.isDistinguishEmptyFromNull(), statement, i, value, columnType);
        }

        String pkColumnType = tableColumns.get(0)
                                          .getType();
        int lastStatementPlaceholderIndex = existingCsvRecord.size();
        setValue(statement, lastStatementPlaceholderIndex, pkColumnType, existingCsvRecord.get(0));
    }

    /**
     * Sets the prepared statement value.
     *
     * @param distinguishEmptyFromNull the distinguish empty from null
     * @param statement the statement
     * @param i the i
     * @param value the value
     * @param columnType the column type
     * @throws SQLException the SQL exception
     */
    private void setPreparedStatementValue(Boolean distinguishEmptyFromNull, PreparedStatement statement, int i, String value,
            String columnType) throws SQLException {
        if (StringUtils.isEmpty(value)) {
            value = distinguishEmptyFromNull ? "" : null;
        }
        setValue(statement, i, columnType, value);
    }

    /**
     * Sets the value.
     *
     * @param preparedStatement the prepared statement
     * @param i the i
     * @param dataType the data type
     * @param value the value
     * @throws SQLException the SQL exception
     */
    protected void setValue(PreparedStatement preparedStatement, int i, String dataType, String value) throws SQLException {
        if (logger.isTraceEnabled()) {
            logger.trace("setValue -> i: " + i + ", dataType: " + dataType + ", value: " + value);
        }

        if (value == null) {
            preparedStatement.setNull(i, DataTypeUtils.getSqlTypeByDataType(dataType));
        } else if (Types.VARCHAR == DataTypeUtils.getSqlTypeByDataType(dataType)) {
            preparedStatement.setString(i, sanitize(value));
        } else if (Types.NVARCHAR == DataTypeUtils.getSqlTypeByDataType(dataType)) {
            preparedStatement.setString(i, sanitize(value));
        } else if (Types.CHAR == DataTypeUtils.getSqlTypeByDataType(dataType)) {
            preparedStatement.setString(i, sanitize(value));
        } else if (Types.DATE == DataTypeUtils.getSqlTypeByDataType(dataType)) {
            if (value.equals("")) {
                preparedStatement.setNull(i, DataTypeUtils.getSqlTypeByDataType(dataType));
            } else {
                preparedStatement.setDate(i, DateTimeUtils.parseDate(value));
            }
        } else if (Types.TIME == DataTypeUtils.getSqlTypeByDataType(dataType)) {
            if (value.equals("")) {
                preparedStatement.setNull(i, DataTypeUtils.getSqlTypeByDataType(dataType));
            } else {
                preparedStatement.setTime(i, DateTimeUtils.parseTime(value));
            }
        } else if (Types.TIMESTAMP == DataTypeUtils.getSqlTypeByDataType(dataType)) {
            if (value.equals("")) {
                preparedStatement.setNull(i, DataTypeUtils.getSqlTypeByDataType(dataType));
            } else {
                preparedStatement.setTimestamp(i, DateTimeUtils.parseDateTime(value));
            }
        } else if (Types.INTEGER == DataTypeUtils.getSqlTypeByDataType(dataType)) {
            value = numberize(value);
            preparedStatement.setInt(i, Integer.parseInt(value));
        } else if (Types.TINYINT == DataTypeUtils.getSqlTypeByDataType(dataType)) {
            value = numberize(value);
            preparedStatement.setByte(i, Byte.parseByte(value));
        } else if (Types.SMALLINT == DataTypeUtils.getSqlTypeByDataType(dataType)) {
            value = numberize(value);
            preparedStatement.setShort(i, Short.parseShort(value));
        } else if (Types.BIGINT == DataTypeUtils.getSqlTypeByDataType(dataType)) {
            value = numberize(value);
            preparedStatement.setLong(i, new BigInteger(value).longValueExact());
        } else if (Types.REAL == DataTypeUtils.getSqlTypeByDataType(dataType)) {
            value = numberize(value);
            preparedStatement.setFloat(i, Float.parseFloat(value));
        } else if (Types.DOUBLE == DataTypeUtils.getSqlTypeByDataType(dataType)) {
            value = numberize(value);
            preparedStatement.setDouble(i, Double.parseDouble(value));
        } else if (Types.BOOLEAN == DataTypeUtils.getSqlTypeByDataType(dataType)
                || Types.BIT == DataTypeUtils.getSqlTypeByDataType(dataType)) {
            preparedStatement.setBoolean(i, Boolean.parseBoolean(value));
        } else if (Types.DECIMAL == DataTypeUtils.getSqlTypeByDataType(dataType)
                || Types.NUMERIC == DataTypeUtils.getSqlTypeByDataType(dataType)) {
            value = numberize(value);
            preparedStatement.setBigDecimal(i, new BigDecimal(value));
        } else if (Types.NCLOB == DataTypeUtils.getSqlTypeByDataType(dataType)) {
            preparedStatement.setString(i, sanitize(value));
        } else if (Types.BLOB == DataTypeUtils.getSqlTypeByDataType(dataType)
                || Types.BINARY == DataTypeUtils.getSqlTypeByDataType(dataType)
                || Types.LONGVARBINARY == DataTypeUtils.getSqlTypeByDataType(dataType)) {
            byte[] bytes = Base64.getDecoder()
                                 .decode(value);
            preparedStatement.setBinaryStream(i, new ByteArrayInputStream(bytes), bytes.length);
        } else if (Types.CLOB == DataTypeUtils.getSqlTypeByDataType(dataType)
                || Types.LONGVARCHAR == DataTypeUtils.getSqlTypeByDataType(dataType)) {
            byte[] bytes = Base64.getDecoder()
                                 .decode(value);
            preparedStatement.setAsciiStream(i, new ByteArrayInputStream(bytes), bytes.length);
        } else if (Types.OTHER == DataTypeUtils.getSqlTypeByDataType(dataType)) {
            if (SqlDialectFactory.getDialect(preparedStatement.getConnection()) instanceof PostgresSqlDialect) {
                if (!value.trim()
                          .isEmpty()) {
                    PGobject pgobject = new PGobject();
                    pgobject.setType(dataType);
                    pgobject.setValue(value);
                    preparedStatement.setObject(i, pgobject);
                } else {
                    preparedStatement.setNull(i, DataTypeUtils.getSqlTypeByDataType(dataType));
                }
            }
        } else {
            throw new PersistenceException(String.format("Database type [%s] not supported", dataType));
        }
    }

    /**
     * Sanitize.
     *
     * @param value the value
     * @return the string
     */
    private String sanitize(String value) {
        if (value != null && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        if (value != null && value.startsWith("'") && value.endsWith("'")) {
            value = value.substring(1, value.length() - 1);
        }
        return value != null ? value.trim() : null;
    }

    /**
     * Numberize.
     *
     * @param value the value
     * @return the string
     */
    private String numberize(String value) {
        if (StringUtils.isEmpty(value)) {
            value = "0";
        }
        return value;
    }
}
