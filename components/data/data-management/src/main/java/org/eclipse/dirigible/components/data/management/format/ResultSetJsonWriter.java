/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.management.format;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import jakarta.annotation.PostConstruct;

import org.apache.commons.lang3.ClassUtils;
import org.eclipse.dirigible.commons.api.helpers.GsonHelper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * The ResultSet JSON Writer.
 */
public class ResultSetJsonWriter extends AbstractResultSetWriter<String> {

    /** The limited. */
    private boolean limited = true;

    /** The stringify. */
    private boolean stringify = true;

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

    /** The object mapper. */
    private ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Write.
     *
     * @param resultSet the result set
     * @param output the output
     * @throws Exception the exception
     */
    @Override
    public void write(ResultSet resultSet, OutputStream output) throws Exception {

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
                    jsonGenerator.writeString(new String(new char[] {(char) value}));
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

}
