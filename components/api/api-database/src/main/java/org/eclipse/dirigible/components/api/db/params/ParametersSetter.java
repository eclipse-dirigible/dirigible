/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.db.params;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.eclipse.dirigible.commons.api.helpers.DateTimeUtils;
import org.eclipse.dirigible.components.api.db.InsertParameters;
import org.eclipse.dirigible.components.api.db.ParamJsonObject;
import org.eclipse.dirigible.components.database.NamedParameterStatement;
import org.eclipse.dirigible.database.sql.DataTypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Optional;
import java.util.Set;

/**
 * The Class ParametersSetter.
 */
public class ParametersSetter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParametersSetter.class);

    /** The Constant paramSetters. */
    private static final Set<ParamSetter> paramSetters = Set.of(//
            new BooleanParamSetter(), //
            new TinyIntParamSetter(), //
            new IntegerParamSetter(), //
            new DoubleParamSetter(), //
            new TextParamSetter(), //
            new DateParamSetter(), //
            new TimestampParamSetter(), //
            new TimeParamSetter(), //
            new SmallIntParamSetter(), //
            new BigIntParamSetter(), //
            new RealParamSetter(), //
            new BlobParamSetter());

    public static void setParameters(JsonElement parameters, NamedParameterStatement preparedStatement) throws SQLException {
        if (!(parameters instanceof JsonArray parametersArray)) {
            throw new IllegalArgumentException("Parameters must be provided as a JSON array, e.g. [1, 'John', 9876]");
        }

        for (JsonElement parameterElement : parametersArray) {
            setParameter(preparedStatement, parameterElement);
        }
    }

    private static void setParameter(NamedParameterStatement preparedStatement, JsonElement parameterElement)
            throws IllegalArgumentException, SQLException {

        if (parameterElement.isJsonObject()) {
            setJsonObjectParam(preparedStatement, parameterElement);
            return;
        }

        throw new IllegalArgumentException("Parameters must contain objects only. Parameter element: " + parameterElement);
    }

    private static void setJsonObjectParam(NamedParameterStatement preparedStatement, JsonElement parameterElement) throws SQLException {

        ParamJsonObject paramJsonObject = ParamJsonObject.fromJsonElement(parameterElement);

        String name = paramJsonObject.getName();
        String dataType = paramJsonObject.getType();

        ParamSetter paramSetter = getParamSetter(dataType);
        JsonElement valueElement = paramJsonObject.getValueElement();
        if (null == valueElement || valueElement.isJsonNull()) {
            Integer sqlType = DataTypeUtils.getSqlTypeByDataType(dataType);
            preparedStatement.setNull(name, sqlType);
            return;
        }

        paramSetter.setParam(valueElement, name, preparedStatement, dataType);
    }

    private static ParamSetter getParamSetter(String dataType) {
        return paramSetters.stream()
                           .filter(ps -> ps.isApplicable(dataType))
                           .findFirst()
                           .orElseThrow(() -> new IllegalArgumentException(
                                   "Parameter 'type'[" + dataType + "] must be a string representing a valid database type name"));
    }

    public static void setParameters(JsonElement parameters, PreparedStatement preparedStatement) throws SQLException {
        if (!(parameters instanceof JsonArray parametersArray)) {
            throw new IllegalArgumentException("Parameters must be provided as a JSON array, e.g. [1, 'John', 9876]");
        }

        int paramIndex = 1;
        for (JsonElement parameterElement : parametersArray) {
            setParameter(preparedStatement, paramIndex, parameterElement);
            paramIndex++;
        }
    }

    private static void setParameter(PreparedStatement preparedStatement, int sqlParamIndex, JsonElement parameterElement)
            throws IllegalArgumentException, SQLException {
        if (parameterElement.isJsonPrimitive()) {
            setJsonPrimitiveParam(preparedStatement, sqlParamIndex, parameterElement);
            return;
        }

        throw new IllegalArgumentException("Parameters must contain primitives only. Parameter element: " + parameterElement);
    }

    /**
     * Sets the json primitive param.
     *
     * @param preparedStatement the prepared statement
     * @param paramIndex the param index
     * @param parameterElement the parameter element
     * @throws SQLException the SQL exception
     */
    private static void setJsonPrimitiveParam(PreparedStatement preparedStatement, int paramIndex, JsonElement parameterElement)
            throws SQLException {
        if (parameterElement.getAsJsonPrimitive()
                            .isBoolean()) {
            preparedStatement.setBoolean(paramIndex, parameterElement.getAsBoolean());
            return;
        }

        if (parameterElement.getAsJsonPrimitive()
                            .isString()) {
            String paramStringValue = parameterElement.getAsString();

            Optional<Timestamp> timestamp = DateTimeUtils.optionallyParseDateTime(paramStringValue);
            if (timestamp.isPresent()) {
                preparedStatement.setTimestamp(paramIndex, timestamp.get());
                return;
            }

            Optional<Date> date = DateTimeUtils.optionallyParseDate(paramStringValue);
            if (date.isPresent()) {
                preparedStatement.setDate(paramIndex, date.get());
                return;
            }

            Optional<Time> time = DateTimeUtils.optionallyParseTime(paramStringValue);
            if (time.isPresent()) {
                preparedStatement.setTime(paramIndex, time.get());
                return;
            }

            preparedStatement.setString(paramIndex, parameterElement.getAsString());
            return;
        }

        if (parameterElement.getAsJsonPrimitive()
                            .isNumber()) {
            setNumber(preparedStatement, paramIndex, parameterElement);
            return;
        }

        throw new IllegalArgumentException("The type of parameter [" + parameterElement + "] as index [" + paramIndex + "] is unknown");
    }

    /**
     * Sets the number.
     *
     * @param preparedStatement the prepared statement
     * @param paramIndex the param index
     * @param parameterElement the parameter element
     * @throws SQLException the SQL exception
     */
    private static void setNumber(PreparedStatement preparedStatement, int paramIndex, JsonElement parameterElement) throws SQLException {
        if (isFloatingPointValue(parameterElement)) {
            try {
                preparedStatement.setDouble(paramIndex, parameterElement.getAsDouble());
                return;
            } catch (SQLException | ClassCastException e) {
                LOGGER.debug("Failed to set value [{}] at index [{}] as double", parameterElement, paramIndex, e);
                // Do nothing
            }
        }

        try {
            preparedStatement.setInt(paramIndex, parameterElement.getAsInt());
            return;
        } catch (SQLException | ClassCastException e) {
            LOGGER.debug("Failed to set value [{}] at index [{}] as int", parameterElement, paramIndex, e);
            // Do nothing
        }

        try {
            preparedStatement.setShort(paramIndex, parameterElement.getAsShort());
            return;
        } catch (SQLException | ClassCastException e) {
            LOGGER.debug("Failed to set value [{}] at index [{}] as short", parameterElement, paramIndex, e);
            // Do nothing
        }

        try {
            preparedStatement.setLong(paramIndex, parameterElement.getAsLong());
            return;
        } catch (SQLException | ClassCastException e) {
            LOGGER.debug("Failed to set value [{}] at index [{}] as long", parameterElement, paramIndex, e);
            // Do nothing
        }

        try {
            preparedStatement.setBigDecimal(paramIndex, parameterElement.getAsBigDecimal());
            return;
        } catch (SQLException | ClassCastException e) {
            LOGGER.debug("Failed to set value [{}] at index [{}] as big decimal", parameterElement, paramIndex, e);
            // Do nothing
        }

        preparedStatement.setObject(paramIndex, parameterElement.getAsNumber()
                                                                .toString());
    }

    private static boolean isFloatingPointValue(JsonElement parameterElement) {
        String numberStr = Double.toString(parameterElement.getAsDouble());

        return numberStr.contains(".") || numberStr.contains("e") || numberStr.contains("E");
    }

    public static void setManyParameters(JsonElement parametersElement, PreparedStatement preparedStatement,
            Optional<InsertParameters> insertParameters) throws IllegalArgumentException, SQLException {
        JsonArray parametersArray = getParametersArray(parametersElement);

        ParameterMetaData paramsMetaData = preparedStatement.getParameterMetaData();
        int sqlParametersCount = paramsMetaData.getParameterCount();

        for (int paramsIdx = 0; paramsIdx < parametersArray.size(); paramsIdx++) {
            JsonElement parameterElement = parametersArray.get(paramsIdx);

            if (!parameterElement.isJsonArray()) {
                throw new IllegalArgumentException(
                        "Parameters at index [" + paramsIdx + "] must be provided as a JSON array, e.g. [1,\"John\",9876]");
            }
            JsonArray elementParametersArray = parameterElement.getAsJsonArray();

            int paramsCount = elementParametersArray.size();
            if (sqlParametersCount != paramsCount) {
                String errMsg = "Provided invalid parameters count of [" + paramsCount + "] at index [" + paramsIdx
                        + "]. Expected parameters count: " + sqlParametersCount;
                throw new IllegalArgumentException(errMsg);
            }

            for (int paramIdx = 0; paramIdx < elementParametersArray.size(); paramIdx++) {
                JsonElement elementParameter = elementParametersArray.get(paramIdx);
                int sqlParamIndex = (paramIdx + 1);

                setParameter(preparedStatement, sqlParamIndex, elementParameter);
            }

            preparedStatement.addBatch();
        }
    }

    private static JsonArray getParametersArray(JsonElement parametersElement) {
        if (!parametersElement.isJsonArray()) {
            throw new IllegalArgumentException(
                    "Parameters must be provided as a JSON array of JSON arrays, e.g. [[1,\"John\",9876],[2,\"Mary\",1234]]");
        }
        return parametersElement.getAsJsonArray();
    }

}
