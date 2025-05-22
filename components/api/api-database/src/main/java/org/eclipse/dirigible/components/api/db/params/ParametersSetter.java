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
import org.eclipse.dirigible.components.database.NamedParameterStatement;
import org.eclipse.dirigible.database.sql.DataTypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The Class ParametersSetter.
 */
public class ParametersSetter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParametersSetter.class);

    /** The Constant paramSetters. */
    private static final Set<ParamSetter> paramSetters = Set.of(//
            new BigDecimalParamSetter(), //
            new BigIntParamSetter(), //
            new BlobParamSetter(), //
            new BooleanParamSetter(), //
            new DateParamSetter(), //
            new DoubleParamSetter(), //
            new IntegerParamSetter(), //
            new RealParamSetter(), //
            new SmallIntParamSetter(), //
            new TextParamSetter(), //
            new TimeParamSetter(), //
            new TimestampParamSetter(), //
            new TinyIntParamSetter());

    public static void setNamedParameters(JsonElement parameters, NamedParameterStatement preparedStatement) throws SQLException {
        if (!parameters.isJsonArray()) {
            throw new IllegalArgumentException("Parameters must be provided as a JSON array, e.g. [1, 'John', 9876]. Parameters ["
                    + parameters + "]. Statement: " + preparedStatement);
        }

        for (JsonElement parameterElement : parameters.getAsJsonArray()) {
            setNamedParameter(preparedStatement, parameterElement);
        }
    }

    private static void setNamedParameter(NamedParameterStatement preparedStatement, JsonElement parameterElement)
            throws IllegalArgumentException, SQLException {

        if (!parameterElement.isJsonObject()) {
            throw new IllegalArgumentException(
                    "Parameters must contain objects only. Parameter element [" + parameterElement + "]. Statement: " + preparedStatement);
        }

        setNamedJsonObjectParam(preparedStatement, parameterElement);
    }

    private static void setNamedJsonObjectParam(NamedParameterStatement preparedStatement, JsonElement parameterElement)
            throws SQLException {
        try {
            NamedParamJsonObject namedParamJsonObject = NamedParamJsonObject.fromJsonElement(parameterElement);

            String name = namedParamJsonObject.getName();
            String dataType = namedParamJsonObject.getType();

            JsonElement valueElement = namedParamJsonObject.getValueElement();
            if (null == valueElement || valueElement.isJsonNull()) {
                Integer sqlType = DataTypeUtils.getSqlTypeByDataType(dataType);
                LOGGER.debug("Dirigible sql type [{}] is mapped to [{}] for element [{}]", dataType, sqlType, parameterElement);

                preparedStatement.setNull(name, sqlType);
                return;
            }

            setNamedParamUsingSetter(preparedStatement, parameterElement, dataType, valueElement, name);
        } catch (IllegalArgumentException ex) {
            String errMsg = "Failed to set named param for parameter [" + parameterElement + "] in statement: " + preparedStatement;
            throw new IllegalArgumentException(errMsg, ex);
        }
    }

    private static void setNamedParamUsingSetter(NamedParameterStatement preparedStatement, JsonElement parameterElement, String dataType,
            JsonElement valueElement, String name) throws SQLException {
        try {
            ParamSetter paramSetter = findParamSetterForType(dataType);
            LOGGER.debug("Found param setter [{}] for dirigible type [{}] for element [{}]", paramSetter, dataType, parameterElement);

            paramSetter.setParam(valueElement, name, preparedStatement);
        } catch (IllegalArgumentException ex) {
            String errMsg = "Failed to set named param with name [" + name + "] and type [" + dataType + "] for element: [" + valueElement
                    + "]. Statement: " + preparedStatement;
            throw new IllegalArgumentException(errMsg, ex);
        }
    }

    private static ParamSetter findParamSetterForType(String dataType) {
        return paramSetters.stream()
                           .filter(ps -> ps.isApplicable(dataType))
                           .findFirst()
                           .orElseThrow(() -> new IllegalArgumentException("Missing param setter for 'type'[" + dataType + "]"));
    }

    public static void setManyIndexedParameters(JsonElement parametersElement, PreparedStatement preparedStatement)
            throws IllegalArgumentException, SQLException {
        JsonArray parametersArray = getParametersArray(parametersElement);

        for (int paramsIdx = 0; paramsIdx < parametersArray.size(); paramsIdx++) {
            JsonElement parameters = parametersArray.get(paramsIdx);
            setIndexedParameters(parameters, preparedStatement);
            preparedStatement.addBatch();
        }
    }

    private static JsonArray getParametersArray(JsonElement parametersElement) {
        if (!parametersElement.isJsonArray()) {
            throw new IllegalArgumentException(
                    "Parameters must be provided as a JSON array of JSON arrays, e.g. [[1,\"John\",9876],[2,\"Mary\",1234]]. Parameters: "
                            + parametersElement);
        }
        return parametersElement.getAsJsonArray();
    }

    public static void setIndexedParameters(JsonElement parameters, PreparedStatement preparedStatement) throws SQLException {
        if (!parameters.isJsonArray()) {
            throw new IllegalArgumentException("Parameters must be provided as a JSON array, e.g. [1, 'John', 9876]. Parameters ["
                    + parameters + "]. Statement: " + preparedStatement);
        }

        JsonArray paramsArray = parameters.getAsJsonArray();

        ParameterMetaData paramsMetaData = preparedStatement.getParameterMetaData();
        int sqlParametersCount = paramsMetaData.getParameterCount();

        int paramsCount = paramsArray.size();
        if (sqlParametersCount != paramsCount) {
            String errMsg = "Provided invalid parameters count of [" + paramsCount + "]. Expected parameters count [" + sqlParametersCount
                    + "]. Statement: " + preparedStatement;
            throw new IllegalArgumentException(errMsg);
        }
        Map<Integer, Integer> paramTypes = new HashMap<>();
        for (int idx = 1; idx <= sqlParametersCount; idx++) {
            paramTypes.put(idx, paramsMetaData.getParameterType(idx));
        }

        LOGGER.info("!!! Prepared statement [{}] Parameters sql types by the driver: {}", preparedStatement, paramTypes);

        for (int idx = 0; idx < paramsCount; idx++) {
            int sqlParamIndex = idx + 1;
            JsonElement parameter = paramsArray.get(idx);

            setIndexedParameter(preparedStatement, sqlParamIndex, parameter);
        }
    }

    private static void setIndexedParameter(PreparedStatement preparedStatement, int sqlParamIndex, JsonElement parameterElement)
            throws IllegalArgumentException, SQLException {

        ParameterMetaData parameterMetaData = preparedStatement.getParameterMetaData();
        int sqlType = parameterMetaData.getParameterType(sqlParamIndex);

        if (Types.NULL == sqlType || parameterElement.isJsonNull()) {
            preparedStatement.setNull(sqlParamIndex, sqlType);
            return;
        }

        try {
            String dirigibleSqlType = DataTypeUtils.getDatabaseTypeName(sqlType);
            ParamSetter paramSetter = findParamSetterForType(dirigibleSqlType);
            LOGGER.debug(
                    "Found param setter [{}] for sql type [{}] which is converted to dirigible type [{}] for element [{}] at index [{}]",
                    paramSetter, sqlType, dirigibleSqlType, parameterElement, sqlParamIndex);

            if (parameterElement.isJsonPrimitive()) {
                paramSetter.setParam(parameterElement, sqlParamIndex, preparedStatement);
                return;
            }

            if (parameterElement.isJsonObject()) {
                setIndexedJsonObjectParam(preparedStatement, sqlParamIndex, parameterElement, sqlType, paramSetter);
                return;
            }
        } catch (IllegalArgumentException ex) {
            String errMsg = "Failed to set indexed param with index [" + sqlParamIndex + "] and sql type [" + sqlType + "] for element ["
                    + parameterElement + "] Statement: " + preparedStatement;
            throw new IllegalArgumentException(errMsg, ex);
        }

        String errMsg = "Parameter with index [" + sqlParamIndex + "] must be primitive or object. Parameter element [" + parameterElement
                + "] Statement: " + preparedStatement;
        throw new IllegalArgumentException(errMsg);

    }

    private static void setIndexedJsonObjectParam(PreparedStatement preparedStatement, int sqlParamIndex, JsonElement parameterElement,
            int sqlType, ParamSetter paramSetter) throws SQLException {
        IndexedParamJsonObject paramJsonObject = IndexedParamJsonObject.fromJsonElement(parameterElement);

        JsonElement valueElement = paramJsonObject.getValueElement();
        if (null == valueElement || valueElement.isJsonNull()) {
            preparedStatement.setNull(sqlParamIndex, sqlType);
            return;
        }

        paramSetter.setParam(valueElement, sqlParamIndex, preparedStatement);
    }

}
