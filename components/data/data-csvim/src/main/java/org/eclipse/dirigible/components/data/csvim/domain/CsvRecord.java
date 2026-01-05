/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.csvim.domain;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.apache.commons.csv.CSVRecord;
import org.eclipse.dirigible.components.database.domain.ColumnMetadata;
import org.eclipse.dirigible.components.database.domain.TableMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class CsvRecord.
 */
public class CsvRecord {

    private static final Logger LOGGER = LoggerFactory.getLogger(CsvRecord.class);

    /**
     * The csv record.
     */
    private final CSVRecord csvRecord;

    /**
     * The table metadata model.
     */
    private final TableMetadata table;

    /**
     * The header names.
     */
    private final List<String> headerNames;

    /**
     * The distinguish empty from null.
     */
    private final boolean distinguishEmptyFromNull;
    private final Optional<Locale> locale;
    /**
     * The pk column name.
     */
    private String pkColumnName;
    /**
     * The csv record pk value.
     */
    private String csvRecordPkValue;

    /**
     * Instantiates a new csv record definition.
     *
     * @param csvRecord the csv record
     * @param table the table metadata model
     * @param headerNames the header names
     * @param distinguishEmptyFromNull the distinguish empty from null
     */
    public CsvRecord(CSVRecord csvRecord, TableMetadata table, List<String> headerNames, boolean distinguishEmptyFromNull) {
        this(csvRecord, table, headerNames, distinguishEmptyFromNull, Optional.empty());
    }

    public CsvRecord(CSVRecord csvRecord, TableMetadata table, List<String> headerNames, boolean distinguishEmptyFromNull,
            Optional<Locale> locale) {
        this.csvRecord = csvRecord;
        this.table = table;
        this.headerNames = headerNames;
        this.distinguishEmptyFromNull = distinguishEmptyFromNull;
        this.locale = locale;
    }

    public Optional<Locale> getLocale() {
        return locale;
    }

    /**
     * Gets the csv record pk value.
     *
     * @return the csv record pk value
     */
    public String getCsvRecordPkValue() {
        if (csvRecordPkValue == null && headerNames.size() > 0) {
            csvRecordPkValue = getCsvValueForColumn(getPkColumnName());
        } else if (csvRecordPkValue == null) {
            csvRecordPkValue = csvRecord.get(0);
        }

        return csvRecordPkValue;
    }

    /**
     * Gets the csv value for column.
     *
     * @param columnName the column name
     * @return the csv value for column
     */
    public String getCsvValueForColumn(String columnName) {
        if (headerNames.size() > 0) {
            int csvValueIndex = headerNames.indexOf(columnName);
            if (csvValueIndex == -1) {
                return null;
            }

            if (!csvRecord.isSet(csvValueIndex)) {
                LOGGER.debug("Missing value with index [{}] for column [{}] in csv record {}. Will return null.", csvValueIndex, columnName,
                        csvRecord);
                return null;
            }

            return csvRecord.get(csvValueIndex);
        }

        return null;
    }

    /**
     * Gets the pk column name.
     *
     * @return the pk column name
     */
    public String getPkColumnName() {
        if (pkColumnName == null) {
            ColumnMetadata found = table.getColumns()
                                        .stream()
                                        .filter(c -> c.isKey())
                                        .findFirst()
                                        .orElse(null);
            pkColumnName = found != null ? found.getName() : null;
        }

        return pkColumnName;
    }

    /**
     * Gets the csv record.
     *
     * @return the csv record
     */
    public CSVRecord getCsvRecord() {
        return csvRecord;
    }

    /**
     * Gets the table metadata model.
     *
     * @return the table metadata model
     */
    public TableMetadata getTableMetadataModel() {
        return table;
    }

    /**
     * Gets the header names.
     *
     * @return the header names
     */
    public List<String> getHeaderNames() {
        return headerNames;
    }

    /**
     * Checks if is distinguish empty from null.
     *
     * @return true, if is distinguish empty from null
     */
    public boolean isDistinguishEmptyFromNull() {
        return distinguishEmptyFromNull;
    }

}
