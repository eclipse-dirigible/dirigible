/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.management.format;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.ClassUtils;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.*;
import java.text.SimpleDateFormat;

/**
 * The ResultSet JSON Writer.
 */
public class ResultSetJsonWriter extends AbstractResultSetWriter<String> {

    private static final String ISO_8601_DATA_FORMAT = "YYYY-MM-DD";
    /** The object mapper. */
    private final ObjectMapper objectMapper = new ObjectMapper();
    /** The limited. */
    private boolean limited = true;
    /** The stringify. */
    private boolean stringify = true;

    /**
     * Checks if is stringified.
     *
     * @return true, if is stringified
     */
    public boolean isStringified() {
        return stringify;
    }

    /**
     * Sets the stringify.
     *
     * @param stringify the new stringify
     */
    public void setStringified(boolean stringify) {
        this.stringify = stringify;
    }

    @Override
    public void write(ResultSet resultSet, OutputStream output) throws Exception {
        write(resultSet, output, ISO_8601_DATA_FORMAT);
    }

    public void write(ResultSet resultSet, OutputStream output, String dateFormat) throws Exception {

        JsonGenerator jsonGenerator = objectMapper.getFactory()
                                                  .createGenerator(output);

        jsonGenerator.writeStartArray();

        int count = 0;
        while (resultSet.next()) {
            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();

            jsonGenerator.writeStartObject();

            for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {
                String name = resultSetMetaData.getColumnName(i);
                String label = resultSetMetaData.getColumnLabel(i);
                Object value = resultSet.getObject(name);
                if (value == null && stringify) {
                    value = "[NULL]";
                }
                if (value != null && ("org.bson.Document".equals(value.getClass()
                                                                      .getCanonicalName())
                        || "org.bson.types.ObjectId".equals(value.getClass()
                                                                 .getCanonicalName())
                        || "java.util.ArrayList".equals(value.getClass()
                                                             .getCanonicalName()))) {
                    if (stringify) {
                        value = value.toString();
                    }
                }
                if (value != null && !ClassUtils.isPrimitiveOrWrapper(value.getClass()) && value.getClass() != String.class
                        && !java.util.Date.class.isAssignableFrom(value.getClass())
                        && !java.math.BigInteger.class.isAssignableFrom(value.getClass())
                        && !java.math.BigDecimal.class.isAssignableFrom(value.getClass())) {
                    if (stringify) {
                        value = "[BINARY]";
                    }
                }

                jsonGenerator.writeFieldName(label != null ? label : name);
                if (value instanceof String) {
                    jsonGenerator.writeString((String) value);
                } else if (value instanceof Character) {
                    jsonGenerator.writeString(String.valueOf((char) value));
                } else if (value instanceof Float) {
                    jsonGenerator.writeNumber((Float) value);
                } else if (value instanceof Double) {
                    jsonGenerator.writeNumber((Double) value);
                } else if (value instanceof BigDecimal) {
                    jsonGenerator.writeNumber((BigDecimal) value);
                } else if (value instanceof Long) {
                    jsonGenerator.writeNumber((Long) value);
                } else if (value instanceof BigInteger) {
                    jsonGenerator.writeNumber((BigInteger) value);
                } else if (value instanceof Integer) {
                    jsonGenerator.writeNumber((Integer) value);
                } else if (value instanceof Byte) {
                    jsonGenerator.writeNumber((Byte) value);
                } else if (value instanceof Short) {
                    jsonGenerator.writeNumber((Short) value);
                } else if (value instanceof Boolean) {
                    jsonGenerator.writeBoolean((Boolean) value);
                } else if (value instanceof Blob blob) {
                    int[] intArray = readBlob(blob);
                    jsonGenerator.writeArray(intArray, 0, intArray.length - 1);
                } else if (value instanceof Clob clob) {
                    String clobValue = readClob(clob);
                    jsonGenerator.writeString(clobValue);
                }
                if (value instanceof Date) {
                    String formattedValue = serializeSqlDate((Date) value, dateFormat);
                    jsonGenerator.writeString(formattedValue);
                } else {
                    jsonGenerator.writeString(value == null ? null : value.toString());
                }
            }

            jsonGenerator.writeEndObject();

            if (this.isLimited() && (++count > getLimit())) {
                break;
            }
        }

        jsonGenerator.writeEndArray();
        jsonGenerator.flush();
    }

    private String serializeSqlDate(Date sqlDate, String pattern) {
        SimpleDateFormat formatter = new SimpleDateFormat(pattern);
        return formatter.format(sqlDate);
    }

    /**
     * Checks if is limited.
     *
     * @return true, if is limited
     */
    public boolean isLimited() {
        return limited;
    }

    /**
     * Sets the limited.
     *
     * @param limited the new limited
     */
    public void setLimited(boolean limited) {
        this.limited = limited;
    }

    /**
     * Read blob.
     *
     * @param blob the blob
     * @return the int[]
     * @throws SQLException the SQL exception
     */
    private int[] readBlob(Blob blob) throws SQLException {
        int blobLength = (int) blob.length();
        byte[] blobAsBytes = blob.getBytes(1, blobLength);
        blob.free();
        int[] intArray = new int[blobLength];
        for (int j = 0; j < blobAsBytes.length; intArray[j] = blobAsBytes[j++]);
        return intArray;
    }

    /**
     * Read clob.
     *
     * @param clob the clob
     * @return the string
     * @throws SQLException the SQL exception
     */
    private String readClob(Clob clob) throws SQLException {
        long clobLength = clob.length();
        if (clobLength <= Integer.MAX_VALUE) {
            return clob.getSubString(1, (int) clobLength);
        }
        return "The size of the CLOB is too big";
    }

}
