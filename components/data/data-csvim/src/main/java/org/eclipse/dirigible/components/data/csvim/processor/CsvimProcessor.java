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

import static org.eclipse.dirigible.components.api.platform.RepositoryFacade.getResource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.data.csvim.domain.CsvFile;
import org.eclipse.dirigible.components.data.csvim.domain.CsvRecord;
import org.eclipse.dirigible.components.data.csvim.synchronizer.CsvimProcessingException;
import org.eclipse.dirigible.components.data.csvim.utils.CsvimUtils;
import org.eclipse.dirigible.components.data.sources.config.DefaultDataSourceName;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.database.DatabaseSystem;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.eclipse.dirigible.components.database.domain.ColumnMetadata;
import org.eclipse.dirigible.components.database.domain.TableMetadata;
import org.eclipse.dirigible.database.sql.SqlFactory;
import org.eclipse.dirigible.database.sql.builders.records.SelectBuilder;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.repository.api.RepositoryReadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The Class CsvimProcessor.
 */
@Component
public class CsvimProcessor {

    /**
     * The Constant logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(CsvimProcessor.class);

    /**
     * The Constant MODULE.
     */
    private static final String MODULE = "dirigible-cms-csvim";

    /**
     * The Constant ERROR_TYPE_PROCESSOR.
     */
    private static final String ERROR_TYPE_PROCESSOR = "PROCESSOR";

    /**
     * The Constant ERROR_MESSAGE_DIFFERENT_COLUMNS_SIZE.
     */
    private static final String ERROR_MESSAGE_DIFFERENT_COLUMNS_SIZE =
            "Error while trying to process CSV record from location [%s]. The number of CSV items should be equal to the number of columns of the database entity.";

    /**
     * The Constant ERROR_MESSAGE_INSERT_RECORD.
     */
    private static final String ERROR_MESSAGE_INSERT_RECORD =
            "Error occurred while trying to insert in table [%s] a CSV record [%s] from location [%s].";

    /**
     * The Constant PROBLEM_MESSAGE_DIFFERENT_COLUMNS_SIZE.
     */
    private static final String PROBLEM_MESSAGE_DIFFERENT_COLUMNS_SIZE =
            "Error while trying to process CSV record with Id [%s]. The number of CSV items should be equal to the number of columns of the database entity.";

    /**
     * The Constant PROBLEM_MESSAGE_INSERT_RECORD.
     */
    private static final String PROBLEM_MESSAGE_INSERT_RECORD = "Error occurred while trying to insert in table [%s] a CSV record [%s].";

    /** The Constant PROBLEM_WITH_TABLE_METADATA_OR_CSVPARSER. */
    private static final String PROBLEM_WITH_TABLE_METADATA_OR_CSVPARSER =
            "No table metadata found for table [%s] or CSVParser not created";

    /** The csv processor. */
    private final CsvProcessor csvProcessor;

    /** The datasources manager. */
    private final DataSourcesManager datasourcesManager;

    /** The default data source name. */
    private final String defaultDataSourceName;

    /** The strict mode. */
    private boolean strictMode;

    /**
     * Instantiates a new csvim processor.
     *
     * @param csvProcessor the csvprocessor service
     * @param datasourcesManager the datasources manager
     * @param defaultDataSourceName the default data source name
     */
    @Autowired
    public CsvimProcessor(CsvProcessor csvProcessor, DataSourcesManager datasourcesManager,
            @DefaultDataSourceName String defaultDataSourceName) {
        this.csvProcessor = csvProcessor;
        this.datasourcesManager = datasourcesManager;
        this.defaultDataSourceName = defaultDataSourceName;
        this.strictMode = Boolean.parseBoolean(Configuration.get("DIRIGIBLE_CSV_STRICT_MODE", "false"));
    }

    public void process(CsvFile csvFile, byte[] content, String dataSourceName) throws Exception {
        try (InputStream inStream = new ByteArrayInputStream(content)) {
            this.process(csvFile, inStream, dataSourceName);
        }
    }

    /**
     * Process.
     *
     * @param csvFile the csv file
     * @param content the content
     * @param dataSourceName the dataSourceName, if a null is passed, the default DataSource will be
     *        used
     * @throws Exception the exception
     */
    public void process(CsvFile csvFile, InputStream content, String dataSourceName) throws Exception {
        boolean defaultDataSource = isDefaultDataSource(dataSourceName);
        DirigibleDataSource dataSource =
                defaultDataSource ? datasourcesManager.getDefaultDataSource() : datasourcesManager.getDataSource(dataSourceName);
        try (Connection connection = dataSource.getConnection()) {

            String fileSchema = csvFile.getSchema();
            String targetSchema = fileSchema;
            if (defaultDataSource) {
                if (("PUBLIC".equals(fileSchema) && dataSource.isOfType(DatabaseSystem.H2))//
                        || ("public".equals(fileSchema) && dataSource.isOfType(DatabaseSystem.POSTGRESQL))//
                        || (dataSourceName == null && "public".equalsIgnoreCase(fileSchema))) {
                    // 1. needed for the multitenancy logic - change the schema to the default db schema for this tenant
                    // 2. when datasource is not specified but the import is for default schema - import into defaultdb
                    // schema
                    logger.debug("Changing target schema from [{}] to default data source schema [{}]", targetSchema,
                            connection.getSchema());
                    targetSchema = connection.getSchema();
                }
            }

            if (null != targetSchema) {
                logger.debug("Updating connection schema to [{}]", targetSchema);
                connection.setSchema(targetSchema);
            }
            String tableName = csvFile.getTable();
            CSVParser csvParser = getCsvParser(csvFile, content);
            TableMetadata tableMetadata = CsvimUtils.getTableMetadata(tableName, targetSchema, connection);
            if (tableMetadata == null) {
                String errorMessage = "Table metadata was not found for table [" + tableName + "] in schema [" + targetSchema + "]";
                throw new CsvimProcessingException((errorMessage));
            }

            String pkName = getPkName(tableMetadata, csvParser.getHeaderNames());

            List<CSVRecord> recordsToInsert = new ArrayList<>();
            List<CSVRecord> recordsToUpdate = new ArrayList<>();

            String pkNameForCSVRecord = getPkNameForCSVRecord(connection, tableName, targetSchema, csvParser.getHeaderNames());

            List<ColumnMetadata> tableColumns = tableMetadata.getColumns();
            boolean skipComparing = isEmptyTable(targetSchema, tableName, connection);

            int countAll = 0;
            int countBatch = 0;
            int batchSize = getCsvDataBatchSize();
            for (CSVRecord csvRecord : csvParser) {
                countAll++;
                countBatch++;
                if (csvRecord.size() != tableColumns.size()) {
                    if (isStrictMode()) {
                        CsvimUtils.logProcessorErrors(String.format(PROBLEM_MESSAGE_DIFFERENT_COLUMNS_SIZE, csvFile.getFile()),
                                ERROR_TYPE_PROCESSOR, csvFile.getFile(), CsvFile.ARTEFACT_TYPE, MODULE);
                        throw new Exception(String.format(ERROR_MESSAGE_DIFFERENT_COLUMNS_SIZE, csvFile.getFile()));
                    }
                }
                if (skipComparing) {
                    recordsToInsert.add(csvRecord);
                } else {
                    String pkValueForCSVRecord = getPkValueForCSVRecord(csvRecord, tableMetadata, csvParser.getHeaderNames());

                    if (pkValueForCSVRecord == null) {
                        recordsToInsert.add(csvRecord);
                    } else {
                        if (!recordExists(targetSchema, tableName, pkNameForCSVRecord, pkValueForCSVRecord, connection)) {
                            recordsToInsert.add(csvRecord);
                        } else {
                            recordsToUpdate.add(csvRecord);
                        }
                    }
                }
                if (countBatch >= batchSize) {
                    countBatch = 0;
                    if (recordsToInsert.size() > 0) {
                        insertCsvRecords(connection, targetSchema, tableMetadata, recordsToInsert, csvParser.getHeaderNames(), csvFile);
                    }
                    if (Boolean.TRUE.equals(csvFile.getUpsert()) && recordsToUpdate.size() > 0) {
                        updateCsvRecords(connection, targetSchema, tableMetadata, recordsToUpdate, csvParser.getHeaderNames(), pkName,
                                csvFile);
                    }
                    recordsToInsert.clear();
                    recordsToUpdate.clear();
                }
            }

            if (recordsToInsert.size() > 0) {
                insertCsvRecords(connection, targetSchema, tableMetadata, recordsToInsert, csvParser.getHeaderNames(), csvFile);
            }
            if (Boolean.TRUE.equals(csvFile.getUpsert()) && recordsToUpdate.size() > 0) {
                updateCsvRecords(connection, targetSchema, tableMetadata, recordsToUpdate, csvParser.getHeaderNames(), pkName, csvFile);
            }
            if (countAll > 0 && csvFile.getSequence() != null) {
                int sequenceStart = countAll + 1;

                PreparedStatement preparedStatement = null;
                try {
                    String createSequenceSql = SqlFactory.getNative(connection)
                                                         .create()
                                                         .sequence(csvFile.getSequence())
                                                         .start(sequenceStart)
                                                         .build();
                    preparedStatement = connection.prepareStatement(createSequenceSql);
                    preparedStatement.executeUpdate();
                } catch (SQLException e) {
                    if (preparedStatement != null) {
                        preparedStatement.close();
                    }
                    try {
                        String alterSequenceSql = SqlFactory.getNative(connection)
                                                            .alter()
                                                            .sequence(csvFile.getSequence())
                                                            .restartWith(sequenceStart)
                                                            .build();
                        preparedStatement = connection.prepareStatement(alterSequenceSql);
                        preparedStatement.executeUpdate();
                    } catch (SQLException e1) {
                        logger.error("Failed to restart database sequence [" + csvFile.getSequence() + "]", e1);
                    } finally {
                        if (preparedStatement != null) {
                            preparedStatement.close();
                        }
                    }
                } finally {
                    if (preparedStatement != null) {
                        preparedStatement.close();
                    }
                }
            }
        }
    }

    private boolean isDefaultDataSource(String dataSourceName) {
        return null == dataSourceName || dataSourceName.equalsIgnoreCase(defaultDataSourceName);
    }

    /**
     * Checks if is strict mode.
     *
     * @return true, if is strict mode
     */
    public boolean isStrictMode() {
        return strictMode;
    }

    /**
     * Sets the strict mode.
     *
     * @param strictMode the new strict mode
     */
    public void setStrictMode(boolean strictMode) {
        this.strictMode = strictMode;
    }

    /**
     * Gets the csv parser.
     *
     * @param csvFile the csv file
     * @param contentAsInputStream the content as input stream
     * @return the csv parser
     * @throws Exception the exception
     */
    private CSVParser getCsvParser(CsvFile csvFile, InputStream contentAsInputStream) throws Exception {
        try {
            CSVFormat csvFormat = createCSVFormat(csvFile);
            return CSVParser.parse(contentAsInputStream, StandardCharsets.UTF_8, csvFormat);
        } catch (Exception ex) {
            String errorMessage = String.format("Error occurred while trying to parse data from CSV file [%s].", csvFile.getFile());
            CsvimUtils.logProcessorErrors(errorMessage, ERROR_TYPE_PROCESSOR, csvFile.getFile(), CsvFile.ARTEFACT_TYPE, MODULE);
            logger.error(errorMessage, ex);
            throw ex;
        }
    }

    /**
     * Creates the CSV format.
     *
     * @param csvFile the csv file
     * @return the CSV format
     * @throws Exception the exception
     */
    private CSVFormat createCSVFormat(CsvFile csvFile) throws Exception {
        if (csvFile.getDelimField() != null && (!csvFile.getDelimField()
                                                        .equals(",")
                && !csvFile.getDelimField()
                           .equals(";")
                && !csvFile.getDelimField()
                           .equals("\t"))) {
            String errorMessage = "Only ';', ',' or tab characters are supported as delimiters for CSV files.";
            CsvimUtils.logProcessorErrors(errorMessage, ERROR_TYPE_PROCESSOR, csvFile.getFile(), CsvFile.ARTEFACT_TYPE, MODULE);
            throw new Exception(errorMessage);
        }
        if (csvFile.getDelimEnclosing() != null && csvFile.getDelimEnclosing()
                                                          .length() > 1) {
            String errorMessage = "Delim enclosing should only contain one character.";
            CsvimUtils.logProcessorErrors(errorMessage, ERROR_TYPE_PROCESSOR, csvFile.getFile(), CsvFile.ARTEFACT_TYPE, MODULE);
            throw new Exception(errorMessage);
        }

        char delimiter = Objects.isNull(csvFile.getDelimField()) ? ','
                : csvFile.getDelimField()
                         .charAt(0);
        char quote = Objects.isNull(csvFile.getDelimEnclosing()) ? '"'
                : csvFile.getDelimEnclosing()
                         .charAt(0);

        // import csv format must be aligned with the used export csv format
        CSVFormat csvFormat = CSVFormat.newFormat(delimiter)
                                       .withIgnoreEmptyLines()
                                       .withQuoteMode(QuoteMode.ALL_NON_NULL)
                                       .withQuote(quote);

        boolean useHeader = !Objects.isNull(csvFile.getHeader()) && csvFile.getHeader();
        if (useHeader) {
            csvFormat = csvFormat.withFirstRecordAsHeader();
        }
        boolean trim = !Objects.isNull(csvFile.getTrim()) && csvFile.getTrim();
        if (trim) {
            csvFormat = csvFormat.withTrim(trim);
        }

        return csvFormat;
    }

    /**
     * Gets the pk name for CSV record.
     *
     * @param connection the connection
     * @param tableName the table name
     * @param schema the schema
     * @param headerNames the header names
     * @return the pk name for CSV record
     */
    private String getPkNameForCSVRecord(Connection connection, String tableName, String schema, List<String> headerNames) {
        TableMetadata tableModel = CsvimUtils.getTableMetadata(tableName, schema, connection);
        if (tableModel != null) {
            List<ColumnMetadata> columnModels = tableModel.getColumns();
            if (headerNames.size() > 0) {
                ColumnMetadata found = columnModels.stream()
                                                   .filter(ColumnMetadata::isKey)
                                                   .findFirst()
                                                   .orElse(null);
                return found != null ? found.getName() : null;
            }

            for (ColumnMetadata columnModel : columnModels) {
                if (columnModel.isKey()) {
                    return columnModel.getName();
                }
            }
        }

        return null;
    }

    /**
     * Gets the csv data batch size.
     *
     * @return the csv data batch size
     */
    private int getCsvDataBatchSize() {
        return DirigibleConfig.CSV_DATA_BATCH_SIZE.getIntValue();
    }

    /**
     * Insert csv records.
     *
     * @param connection the connection
     * @param schema the schema
     * @param tableModel the table model
     * @param recordsToProcess the records to process
     * @param headerNames the header names
     * @param csvFile the csv file
     */
    private void insertCsvRecords(Connection connection, String schema, TableMetadata tableModel, List<CSVRecord> recordsToProcess,
            List<String> headerNames, CsvFile csvFile) {
        try {
            List<CsvRecord> csvRecords = recordsToProcess.stream()
                                                         .map(e -> new CsvRecord(e, tableModel, headerNames,
                                                                 csvFile.getDistinguishEmptyFromNull(), csvFile.getParsedLocale()))
                                                         .collect(Collectors.toList());
            csvProcessor.insert(connection, schema, tableModel, csvRecords, headerNames, csvFile);
        } catch (Exception e) {
            String csvRecordValue = e.getMessage();
            CsvimUtils.logProcessorErrors(String.format(PROBLEM_MESSAGE_INSERT_RECORD, tableModel.getName(), csvRecordValue),
                    ERROR_TYPE_PROCESSOR, csvFile.getFile(), CsvFile.ARTEFACT_TYPE, MODULE);
            if (logger.isErrorEnabled()) {
                logger.error(String.format(ERROR_MESSAGE_INSERT_RECORD, tableModel.getName(), csvRecordValue, csvFile.getFile()), e);
            }
        }
    }

    /**
     * Update csv records.
     *
     * @param connection the connection
     * @param schema the schema
     * @param tableModel the table model
     * @param recordsToProcess the records to process
     * @param headerNames the header names
     * @param pkName the pk name
     * @param csvFile the csv file
     */
    private void updateCsvRecords(Connection connection, String schema, TableMetadata tableModel, List<CSVRecord> recordsToProcess,
            List<String> headerNames, String pkName, CsvFile csvFile) {
        try {
            List<CsvRecord> csvRecords = recordsToProcess.stream()
                                                         .map(e -> new CsvRecord(e, tableModel, headerNames,
                                                                 csvFile.getDistinguishEmptyFromNull(), csvFile.getParsedLocale()))
                                                         .collect(Collectors.toList());
            csvProcessor.update(connection, schema, tableModel, csvRecords, headerNames, pkName, csvFile);
        } catch (SQLException e) {
            String csvRecordValue = e.getMessage();
            CsvimUtils.logProcessorErrors(String.format(PROBLEM_MESSAGE_INSERT_RECORD, tableModel.getName(), csvRecordValue),
                    ERROR_TYPE_PROCESSOR, csvFile.getFile(), CsvFile.ARTEFACT_TYPE, MODULE);
            if (logger.isErrorEnabled()) {
                logger.error(String.format(ERROR_MESSAGE_INSERT_RECORD, tableModel.getName(), csvRecordValue, csvFile.getFile()), e);
            }
        }
    }

    /**
     * Gets the pk value for CSV record.
     *
     * @param csvRecord the csv record
     * @param tableModel the table model
     * @param headerNames the header names
     * @return the pk value for CSV record
     */
    private String getPkValueForCSVRecord(CSVRecord csvRecord, TableMetadata tableModel, List<String> headerNames) {
        if (tableModel != null) {
            List<ColumnMetadata> columnModels = tableModel.getColumns();
            if (headerNames.size() > 0) {
                ColumnMetadata found = columnModels.stream()
                                                   .filter(ColumnMetadata::isKey)
                                                   .findFirst()
                                                   .orElse(null);
                String pkColumnName = found != null ? found.getName() : null;
                if (pkColumnName != null) {
                    int csvRecordPkValueIndex = headerNames.indexOf(pkColumnName);
                    return csvRecordPkValueIndex >= 0 ? csvRecord.get(csvRecordPkValueIndex) : null;
                }
                return null;
            }

            for (int i = 0; i < csvRecord.size(); i++) {
                boolean isColumnPk = columnModels.get(i)
                                                 .isKey();
                if (isColumnPk && !StringUtils.isEmpty(csvRecord.get(i))) {
                    return csvRecord.get(i);
                }
            }
        }

        return null;
    }

    /**
     * Gets the pk value for CSV record.
     *
     * @param tableModel the table model
     * @param headerNames the header names
     * @return the pk name or null
     */
    private String getPkName(TableMetadata tableModel, List<String> headerNames) {
        if (tableModel != null) {
            List<ColumnMetadata> columnModels = tableModel.getColumns();
            if (headerNames.size() > 0) {
                ColumnMetadata found = columnModels.stream()
                                                   .filter(ColumnMetadata::isKey)
                                                   .findFirst()
                                                   .orElse(null);
                return found != null ? found.getName() : null;
            }
        }
        return null;
    }

    /**
     * Record exists.
     *
     * @param schema the schema name
     * @param tableName the table name
     * @param pkNameForCSVRecord the pk name for CSV record
     * @param pkValueForCSVRecord the pk value for CSV record
     * @param connection the connection
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    private boolean recordExists(String schema, String tableName, String pkNameForCSVRecord, String pkValueForCSVRecord,
            Connection connection) throws SQLException {
        boolean exists = false;
        SelectBuilder selectBuilder = new SelectBuilder(SqlFactory.deriveDialect(connection));
        String sql = selectBuilder.distinct()
                                  .column("1")
                                  .from(tableName)
                                  .schema(schema)
                                  .where(pkNameForCSVRecord + " = ?")
                                  .build();
        try (PreparedStatement pstmt = connection.prepareCall(sql)) {
            ResultSet rs;
            try {
                pstmt.setString(1, pkValueForCSVRecord);
                rs = pstmt.executeQuery();
            } catch (Throwable e) {
                pstmt.setInt(1, Integer.parseInt(pkValueForCSVRecord));
                rs = pstmt.executeQuery();
            }
            if (rs.next()) {
                try {
                    exists = "1".equals(rs.getString(1));
                } catch (Throwable e) {
                    exists = 1 == rs.getInt(1);
                }
            }
        }
        return exists;
    }

    /**
     * Checks if is empty table.
     *
     * @param schema the schema name
     * @param tableName the table name
     * @param connection the connection
     * @return true, if is empty table
     * @throws SQLException the SQL exception
     */
    private boolean isEmptyTable(String schema, String tableName, Connection connection) throws SQLException {
        boolean isEmpty = false;
        SelectBuilder selectBuilder = new SelectBuilder(SqlFactory.deriveDialect(connection));
        String sql = selectBuilder.column("COUNT(*)")
                                  .from(tableName)
                                  .schema(schema)
                                  .build();
        try (PreparedStatement pstmt = connection.prepareCall(sql)) {
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                isEmpty = rs.getInt(1) == 0;
            }
        }
        return isEmpty;
    }

    /**
     * Gets the csv resource.
     *
     * @param csvFile the csv file
     * @return the csv resource
     */
    public static IResource getCsvResource(CsvFile csvFile) {
        return getResource(convertToActualFileName(csvFile.getFile()));
    }

    /**
     * Convert to actual file name.
     *
     * @param fileNamePath the file name path
     * @return the string
     */
    private static String convertToActualFileName(String fileNamePath) {
        return IRepositoryStructure.PATH_REGISTRY_PUBLIC + IRepository.SEPARATOR + fileNamePath;
    }

    /**
     * Gets the csv content.
     *
     * @param resource the resource
     * @return the csv content
     * @throws RepositoryReadException the repository read exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public byte[] getCsvContent(IResource resource) throws RepositoryReadException, IOException {
        return resource.getContent();
    }

}
