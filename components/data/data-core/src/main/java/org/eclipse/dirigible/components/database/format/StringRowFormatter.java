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

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;

/**
 * The String Row Formatter.
 */
public class StringRowFormatter implements RowFormatter {

    /** The Constant VALUE_NULL. */
    private static final String VALUE_NULL = "[NULL]";

    /** The Constant VALUE_BINARY. */
    private static final String VALUE_BINARY = "[BINARY]";

    /** The Constant BINARY_TYPES. */
    static final int[] BINARY_TYPES = new int[] {java.sql.Types.ARRAY, java.sql.Types.BINARY, java.sql.Types.BIT, java.sql.Types.BIT,
            java.sql.Types.BLOB, java.sql.Types.CLOB, java.sql.Types.DATALINK, java.sql.Types.DISTINCT, java.sql.Types.JAVA_OBJECT,
            java.sql.Types.LONGVARBINARY, java.sql.Types.NCLOB, java.sql.Types.NULL, java.sql.Types.OTHER, java.sql.Types.REF,
            java.sql.Types.SQLXML, java.sql.Types.STRUCT, java.sql.Types.VARBINARY};

    /**
     * Write.
     *
     * @param columnDescriptors the column descriptors
     * @param resultSetMetaData the result set meta data
     * @param resultSet the result set
     * @return the string
     * @throws SQLException the SQL exception
     */
    @Override
    public String write(List<ColumnDescriptor> columnDescriptors, ResultSetMetaData resultSetMetaData, ResultSet resultSet)
            throws SQLException {
        StringBuilder buff = new StringBuilder();
        buff.append(ResultSetMonospacedWriter.DELIMITER);
        for (ColumnDescriptor columnDescriptor : columnDescriptors) {

            String value = null;

            // For the schemaless NoSQL DBs it's perfectly legal for records to miss some key-value tuples.
            if (columnDescriptor.getSqlType() == Integer.MIN_VALUE) {
                value = "";
            } else {
                if (this.isBinaryType(columnDescriptor.getSqlType())) {
                    value = VALUE_BINARY;
                } else {
                    value = resultSet.getString(columnDescriptor.getLabel());
                }

                if (value == null) {
                    value = VALUE_NULL;
                }

                if (!VALUE_BINARY.equals(value) || !VALUE_NULL.equals(value)) {

                    int delta = value.length() - columnDescriptor.getDisplaySize();
                    if (delta > 0) {
                        value = value.substring(0, columnDescriptor.getDisplaySize());
                        if (value.length() > 3) {
                            value = value.substring(0, value.length() - 3) + "...";
                        }
                    } else if (delta < 0) {
                        value = String.format("%-" + columnDescriptor.getDisplaySize() + "s", value);
                    }

                }
            }

            buff.append(value);
            buff.append(ResultSetMonospacedWriter.DELIMITER);

        }
        buff.append(ResultSetMonospacedWriter.NEWLINE_CHARACTER);
        return buff.toString();
    }

    /**
     * Checks if is binary type.
     *
     * @param columnType the column type
     * @return true, if is binary type
     */
    boolean isBinaryType(int columnType) {
        for (int c : BINARY_TYPES) {
            if (columnType == c) {
                return true;
            }
        }
        return false;
    }

}
