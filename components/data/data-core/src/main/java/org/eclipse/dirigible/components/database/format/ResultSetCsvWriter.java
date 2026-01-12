/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.database.format;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ClassUtils;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The ResultSet CSV Writer.
 */
public class ResultSetCsvWriter extends AbstractResultSetWriter<String> {

    /**
     * The Constant logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(ResultSetCsvWriter.class);

    /** The limited. */
    private boolean limited = true;

    /** The stringify. */
    private boolean stringify = true;

    /**
     * Checks if is stringified.
     *
     * @return true, if is stringified
     */
    @Override
    public boolean isStringified() {
        return stringify;
    }

    /**
     * Sets the stringified.
     *
     * @param stringify the new stringified
     */
    @Override
    public void setStringified(boolean stringify) {
        this.stringify = stringify;
    }

    /**
     * Write.
     *
     * @param resultSet the result set
     * @param output the output
     * @throws Exception the exception
     */
    @Override
    public void write(ResultSet resultSet, OutputStream output) throws Exception {
        OutputStreamWriter sw = new OutputStreamWriter(output);

        ResultSetMetaData resultSetMetaData = resultSet.getMetaData();

        int count = 0;
        List<String> names = new ArrayList<>();
        if (resultSetMetaData == null) {
            resultSetMetaData = resultSet.getMetaData(); // dynamic result set metadata
        }
        for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {
            String name = resultSetMetaData.getColumnName(i);
            names.add(name);
        }

        if (!resultSet.next()) {
            sw.write(names.stream()
                          .collect(Collectors.joining(",")));
            sw.flush();
            return;
        }

        try {
            // export csv format must be aligned with the used import csv format
            CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                                                   .setHeader(names.stream()
                                                                   .toArray(String[]::new))
                                                   .setQuoteMode(QuoteMode.ALL_NON_NULL)
                                                   .build();
            CSVPrinter printer = new CSVPrinter(sw, csvFormat);
            count = 0;
            do {
                List<Object> values = new ArrayList<>();
                for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {
                    Object value = null;
                    String name = resultSetMetaData.getColumnName(i);
                    int dbt = resultSetMetaData.getColumnType(i);
                    if (dbt == Types.BLOB || dbt == Types.BINARY || dbt == Types.LONGVARBINARY || dbt == Types.VARBINARY) {
                        InputStream is = resultSet.getBinaryStream(name);
                        if (is == null && stringify) {
                            value = "[NULL]";
                        } else if (is != null) {
                            byte[] ba = IOUtils.toByteArray(is);
                            if (stringify) {
                                value = "[BLOB]";
                            } else {
                                value = Base64.getEncoder()
                                              .encodeToString(ba);
                            }
                        }
                    } else if (dbt == Types.CLOB || dbt == Types.LONGVARCHAR) {
                        Clob clob = resultSet.getClob(name);
                        if (clob == null && stringify) {
                            value = "[NULL]";
                        } else if (clob != null) {
                            byte[] ba = IOUtils.toByteArray(clob.getAsciiStream());
                            if (stringify) {
                                value = "[CLOB]";
                            } else {
                                value = Base64.getEncoder()
                                              .encodeToString(ba);
                            }
                        }
                    } else if (dbt == Types.OTHER) {
                        Object dataObject = resultSet.getObject(name);
                        if (dataObject instanceof PGobject) {
                            value = ((PGobject) dataObject).getValue();
                        }
                        if (dataObject instanceof String valueString) {
                            value = valueString;
                        }
                    } else {
                        value = resultSet.getObject(name);
                        if (value == null && stringify) {
                            value = "[NULL]";
                        }
                        if (value != null && !ClassUtils.isPrimitiveOrWrapper(value.getClass()) && value.getClass() != String.class
                                && !java.util.Date.class.isAssignableFrom(value.getClass())
                                && !java.math.BigInteger.class.isAssignableFrom(value.getClass())
                                && !java.math.BigDecimal.class.isAssignableFrom(value.getClass())) {
                            if (stringify) {
                                value = "[BINARY]";
                            }
                        }
                    }
                    values.add(value);
                }
                try {
                    printer.printRecord(values);
                    printer.flush();
                } catch (Exception e) {
                    logger.error(e.getMessage());
                    printer.close();
                }

                if (this.isLimited() && (++count > getLimit())) {
                    break;
                }
            } while (resultSet.next());

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * Checks if is limited.
     *
     * @return true, if is limited
     */
    @Override
    public boolean isLimited() {
        return limited;
    }

    /**
     * Sets the limited.
     *
     * @param limited the new limited
     */
    @Override
    public void setLimited(boolean limited) {
        this.limited = limited;
    }

}
