/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
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
import org.eclipse.dirigible.components.database.domain.ColumnMetadata;
import org.eclipse.dirigible.components.database.domain.TableMetadata;
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
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
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
            logger.warn("Skipping import - CSV records are empty for csv file [{}].", csvFile);
            return;
        }

        if (tableMetadata == null) {
            logger.warn("Missing table metadata for file [{}] on insert", csvFile);
            return;
        }
        if (null != schema) {
            connection.setSchema(schema);
        }
        logger.info("Will insert [{}] data records into table [{}] in schema [{}]", csvRecords.size(), tableMetadata.getName(), schema);
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
            logger.debug("CSV records with Ids [{}] were successfully added in BATCH INSERT for table [{}].", csvRecords.stream()
                                                                                                                        .map(e -> e.getCsvRecord()
                                                                                                                                   .get(0))
                                                                                                                        .collect(
                                                                                                                                Collectors.toList()),
                    tableMetadata.getName());
            preparedStatement.executeBatch();
            logger.info("Successfully inserted [{}] data records into table [{}] in schema [{}]", csvRecords.size(),
                    tableMetadata.getName(), schema);
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
            // TODO: why not rethrowing the exception
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
            logger.debug("Skipping update - CSV records are empty for csv file [{}].", csvFile);
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
            logger.debug("CSV records with Ids [{}] were successfully added in BATCH UPDATED for table [{}].", csvRecords.stream()
                                                                                                                         .map(e -> e.getCsvRecord()
                                                                                                                                    .get(0))
                                                                                                                         .collect(
                                                                                                                                 Collectors.toList()),
                    tableMetadata.getName());
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
            logger.error(errorMessage, t);
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
            ColumnMetadata columnMetadata = tableColumns.get(i);
            String columnName = columnMetadata.getName();
            String columnType = columnMetadata.getType();
            String value = csvRecord.getCsvValueForColumn(columnName);
            int paramIdx = i + 1;
            setPreparedStatementValue(csvRecord.isDistinguishEmptyFromNull(), statement, paramIdx, value, columnType,
                    csvRecord.getLocale());
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

            setPreparedStatementValue(csvRecord.isDistinguishEmptyFromNull(), statement, i + 1, value, columnType, csvRecord.getLocale());
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

            setPreparedStatementValue(csvRecord.isDistinguishEmptyFromNull(), statement, i, value, columnType, csvRecord.getLocale());
        }

        String pkColumnType = tableColumns.get(0)
                                          .getType();
        int lastStatementPlaceholderIndex = existingCsvRecord.size();

        setValue(statement, lastStatementPlaceholderIndex, pkColumnType, csvRecord.getCsvRecordPkValue(), csvRecord.getLocale());
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
            setPreparedStatementValue(csvRecord.isDistinguishEmptyFromNull(), statement, i, value, columnType, csvRecord.getLocale());
        }

        String pkColumnType = tableColumns.get(0)
                                          .getType();
        int lastStatementPlaceholderIndex = existingCsvRecord.size();
        setValue(statement, lastStatementPlaceholderIndex, pkColumnType, existingCsvRecord.get(0), csvRecord.getLocale());
    }

    /**
     * Sets the prepared statement value.
     *
     * @param distinguishEmptyFromNull the distinguish empty from null
     * @param statement the statement
     * @param paramIdx the paramIdx
     * @param value the value
     * @param columnType the column type
     * @throws SQLException the SQL exception
     */
    private void setPreparedStatementValue(Boolean distinguishEmptyFromNull, PreparedStatement statement, int paramIdx, String value,
            String columnType, Optional<Locale> locale) throws SQLException {
        if (StringUtils.isEmpty(value)) {
            value = distinguishEmptyFromNull ? value : null;
        }
        setValue(statement, paramIdx, columnType, value, locale);
    }

    /**
     * Sets the value.
     *
     * @param preparedStatement the prepared statement
     * @param paramIdx the paramIdx
     * @param dataType the data type
     * @param value the value
     * @throws SQLException the SQL exception
     */
    protected void setValue(PreparedStatement preparedStatement, int paramIdx, String dataType, String value, Optional<Locale> locale)
            throws SQLException {
        logger.trace("setValue -> paramIdx: {}, dataType: {}, value: {}", paramIdx, dataType, value);

        // TODO consider to use org.eclipse.dirigible.components.api.db.params.ParametersSetter for reuse
        if (value == null) {
            preparedStatement.setNull(paramIdx, DataTypeUtils.getSqlTypeByDataType(dataType));
        } else if (Types.VARCHAR == DataTypeUtils.getSqlTypeByDataType(dataType)) {
            preparedStatement.setString(paramIdx, sanitize(value));
        } else if (Types.NVARCHAR == DataTypeUtils.getSqlTypeByDataType(dataType)) {
            preparedStatement.setString(paramIdx, sanitize(value));
        } else if (Types.CHAR == DataTypeUtils.getSqlTypeByDataType(dataType)) {
            preparedStatement.setString(paramIdx, sanitize(value));
        } else if (Types.DATE == DataTypeUtils.getSqlTypeByDataType(dataType)) {
            if (value.equals("")) {
                preparedStatement.setNull(paramIdx, DataTypeUtils.getSqlTypeByDataType(dataType));
            } else {
                preparedStatement.setDate(paramIdx, DateTimeUtils.parseDate(value));
            }
        } else if (Types.TIME == DataTypeUtils.getSqlTypeByDataType(dataType)) {
            if (value.equals("")) {
                preparedStatement.setNull(paramIdx, DataTypeUtils.getSqlTypeByDataType(dataType));
            } else {
                preparedStatement.setTime(paramIdx, DateTimeUtils.parseTime(value));
            }
        } else if (Types.TIMESTAMP == DataTypeUtils.getSqlTypeByDataType(dataType)) {
            if (value.equals("")) {
                preparedStatement.setNull(paramIdx, DataTypeUtils.getSqlTypeByDataType(dataType));
            } else {
                preparedStatement.setTimestamp(paramIdx, DateTimeUtils.parseDateTime(value));
            }
        } else if (Types.INTEGER == DataTypeUtils.getSqlTypeByDataType(dataType)) {
            value = numberize(value, locale);
            preparedStatement.setInt(paramIdx, parseInt(value));
        } else if (Types.TINYINT == DataTypeUtils.getSqlTypeByDataType(dataType)) {
            value = numberize(value, locale);
            preparedStatement.setByte(paramIdx, parseByte(value));
        } else if (Types.SMALLINT == DataTypeUtils.getSqlTypeByDataType(dataType)) {
            value = numberize(value, locale);
            preparedStatement.setShort(paramIdx, parseShort(value));
        } else if (Types.BIGINT == DataTypeUtils.getSqlTypeByDataType(dataType)) {
            value = numberize(value, locale);
            preparedStatement.setLong(paramIdx, createBigInteger(value).longValueExact());
        } else if (Types.REAL == DataTypeUtils.getSqlTypeByDataType(dataType)) {
            value = numberize(value, locale);
            preparedStatement.setFloat(paramIdx, parseFloat(value));
        } else if (Types.DOUBLE == DataTypeUtils.getSqlTypeByDataType(dataType)) {
            value = numberize(value, locale);
            preparedStatement.setDouble(paramIdx, parseDouble(value));
        } else if (Types.FLOAT == DataTypeUtils.getSqlTypeByDataType(dataType)) {
            value = numberize(value, locale);
            preparedStatement.setFloat(paramIdx, parseFloat(value));
        } else if (Types.BOOLEAN == DataTypeUtils.getSqlTypeByDataType(dataType)
                || Types.BIT == DataTypeUtils.getSqlTypeByDataType(dataType)) {
            preparedStatement.setBoolean(paramIdx, parseBoolean(value));
        } else if (Types.DECIMAL == DataTypeUtils.getSqlTypeByDataType(dataType)
                || Types.NUMERIC == DataTypeUtils.getSqlTypeByDataType(dataType)) {
            value = numberize(value, locale);
            preparedStatement.setBigDecimal(paramIdx, parseBigDecimal(value));
        } else if (Types.NCLOB == DataTypeUtils.getSqlTypeByDataType(dataType)) {
            preparedStatement.setString(paramIdx, sanitize(value));
        } else if (Types.BLOB == DataTypeUtils.getSqlTypeByDataType(dataType)
                || Types.BINARY == DataTypeUtils.getSqlTypeByDataType(dataType)
                || Types.LONGVARBINARY == DataTypeUtils.getSqlTypeByDataType(dataType)) {
            byte[] bytes = Base64.getDecoder()
                                 .decode(value);
            preparedStatement.setBinaryStream(paramIdx, new ByteArrayInputStream(bytes), bytes.length);
        } else if (Types.CLOB == DataTypeUtils.getSqlTypeByDataType(dataType)
                || Types.LONGVARCHAR == DataTypeUtils.getSqlTypeByDataType(dataType)) {
            byte[] bytes = Base64.getDecoder()
                                 .decode(value);
            preparedStatement.setAsciiStream(paramIdx, new ByteArrayInputStream(bytes), bytes.length);
        } else if (Types.OTHER == DataTypeUtils.getSqlTypeByDataType(dataType)) {
            if (SqlDialectFactory.getDialect(preparedStatement.getConnection()) instanceof PostgresSqlDialect) {
                if (!value.trim()
                          .isEmpty()) {
                    PGobject pgobject = new PGobject();
                    pgobject.setType(dataType);
                    pgobject.setValue(value);
                    preparedStatement.setObject(paramIdx, pgobject);
                } else {
                    preparedStatement.setNull(paramIdx, DataTypeUtils.getSqlTypeByDataType(dataType));
                }
            }
        } else {
            throw new PersistenceException(String.format("Database type [%s] not supported", dataType));
        }
    }

    private static double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException | NullPointerException ex) {
            throw new IllegalArgumentException("Failed to parse [" + value + "] to double", ex);
        }
    }

    private static float parseFloat(String value) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException | NullPointerException ex) {
            throw new IllegalArgumentException("Failed to parse [" + value + "] to float", ex);
        }
    }

    private static int parseInt(String value) {
        try {
            // use BigDecimal to support values in format like 30.000
            BigDecimal bd = new BigDecimal(value);
            return bd.intValueExact();
        } catch (ArithmeticException | IllegalArgumentException ex) {
            throw new IllegalArgumentException("Failed to parse [" + value + "] to integer", ex);
        }
    }

    private static byte parseByte(String value) {
        try {
            return Byte.parseByte(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Failed to parse [" + value + "] to byte", ex);
        }
    }

    private static short parseShort(String value) {
        try {
            return Short.parseShort(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Failed to parse [" + value + "] to short", ex);
        }
    }

    private static BigInteger createBigInteger(String value) {
        try {
            return new BigInteger(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Failed to create big integer from  [" + value + "]", ex);
        }
    }

    private static boolean parseBoolean(String value) {
        return Boolean.parseBoolean(value);
    }

    private static BigDecimal parseBigDecimal(String input) {
        try {
            return new BigDecimal(input);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Unable to parse [" + input + "] to big decimal", ex);
        }
    }

    private String formatLocalizedNumber(String number, Optional<Locale> locale) throws ParseException {
        NumberFormat nf = NumberFormat.getInstance(locale.get());
        Number parsed = nf.parse(number);
        return parsed.toString();
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

    private String numberize(String value, Optional<Locale> locale) {
        if (StringUtils.isEmpty(value)) {
            value = "0";
        }

        if (locale.isEmpty()) {
            return value;
        }
        try {
            return formatLocalizedNumber(value, locale);
        } catch (ParseException ex) {
            throw new IllegalArgumentException("Failed to format provided number [" + value + "] using locale [" + locale + "]", ex);
        }
    }
}
