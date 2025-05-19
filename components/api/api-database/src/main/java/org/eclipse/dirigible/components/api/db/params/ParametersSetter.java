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
import java.util.Set;

/**
 * The Class ParametersSetter.
 */
public class ParametersSetter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParametersSetter.class);
    /** The Constant paramSetters. */
    private static final Set<ParamSetter> paramSetters = Set.of(//
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

    public static void setParameters(JsonElement parameters, NamedParameterStatement preparedStatement) throws SQLException {
        if (!parameters.isJsonArray()) {
            throw new IllegalArgumentException("Parameters must be provided as a JSON array, e.g. [1, 'John', 9876]");
        }

        for (JsonElement parameterElement : parameters.getAsJsonArray()) {
            setParameter(preparedStatement, parameterElement);
        }
    }

    private static void setParameter(NamedParameterStatement preparedStatement, JsonElement parameterElement)
            throws IllegalArgumentException, SQLException {

        if (!parameterElement.isJsonObject()) {
            throw new IllegalArgumentException("Parameters must contain objects only. Parameter element: " + parameterElement);
        }

        setJsonObjectParam(preparedStatement, parameterElement);
    }

    private static void setJsonObjectParam(NamedParameterStatement preparedStatement, JsonElement parameterElement) throws SQLException {
        ParamJsonObject paramJsonObject = ParamJsonObject.fromJsonElement(parameterElement);

        String name = paramJsonObject.getName();
        String dataType = paramJsonObject.getType();

        JsonElement valueElement = paramJsonObject.getValueElement();
        if (null == valueElement || valueElement.isJsonNull()) {
            Integer sqlType = DataTypeUtils.getSqlTypeByDataType(dataType);
            preparedStatement.setNull(name, sqlType);
            return;
        }

        ParamSetter paramSetter = getParamSetterForType(dataType);
        paramSetter.setParam(valueElement, name, preparedStatement, dataType);
    }

    private static ParamSetter getParamSetterForType(String dataType) {
        return paramSetters.stream()
                           .filter(ps -> ps.isApplicable(dataType))
                           .findFirst()
                           .orElseThrow(() -> new IllegalArgumentException(
                                   "Parameter 'type'[" + dataType + "] must be a string representing a valid database type name"));
    }

    public static void setManyParameters(JsonElement parametersElement, PreparedStatement preparedStatement)
            throws IllegalArgumentException, SQLException {
        JsonArray parametersArray = getParametersArray(parametersElement);

        for (int paramsIdx = 0; paramsIdx < parametersArray.size(); paramsIdx++) {
            JsonElement parameters = parametersArray.get(paramsIdx);
            setParameters(parameters, preparedStatement);
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

    public static void setParameters(JsonElement parameters, PreparedStatement preparedStatement) throws SQLException {
        if (!parameters.isJsonArray()) {
            throw new IllegalArgumentException("Parameters must be provided as a JSON array, e.g. [1, 'John', 9876]");
        }

        JsonArray paramsArray = parameters.getAsJsonArray();

        ParameterMetaData paramsMetaData = preparedStatement.getParameterMetaData();
        int sqlParametersCount = paramsMetaData.getParameterCount();

        int paramsCount = paramsArray.size();
        if (sqlParametersCount != paramsCount) {
            String errMsg = "Provided invalid parameters count of [" + paramsCount + "]. Expected parameters count: " + sqlParametersCount;
            throw new IllegalArgumentException(errMsg);
        }

        for (int idx = 0; idx < paramsCount; idx++) {
            int sqlParamIndex = idx + 1;
            JsonElement parameter = paramsArray.get(idx);

            setParameter(preparedStatement, sqlParamIndex, parameter);
        }
    }

    private static void setParameter(PreparedStatement preparedStatement, int sqlParamIndex, JsonElement parameterElement)
            throws IllegalArgumentException, SQLException {

        ParameterMetaData parameterMetaData = preparedStatement.getParameterMetaData();
        int sqlType = parameterMetaData.getParameterType(sqlParamIndex);

        String dirigibleSqlType = DataTypeUtils.getDatabaseTypeName(sqlType);
        ParamSetter paramSetter = getParamSetterForType(dirigibleSqlType);
        LOGGER.debug("Found param setter [{}] for sql type [{}] which is converted to dirigible type [{}]", paramSetter, sqlType,
                dirigibleSqlType);

        if (parameterElement.isJsonPrimitive()) {
            paramSetter.setParam(parameterElement, sqlParamIndex, preparedStatement, dirigibleSqlType);
            return;
        }

        if (parameterElement.isJsonObject()) {
            ParamJsonObject paramJsonObject = ParamJsonObject.fromJsonElement(parameterElement);

            JsonElement valueElement = paramJsonObject.getValueElement();
            if (null == valueElement || valueElement.isJsonNull()) {
                preparedStatement.setNull(sqlParamIndex, sqlType);
                return;
            }

            paramSetter.setParam(valueElement, sqlParamIndex, preparedStatement, dirigibleSqlType);
            return;
        }

        throw new IllegalArgumentException("Parameters must contain primitives or objects only. Parameter element: " + parameterElement);

    }

}
