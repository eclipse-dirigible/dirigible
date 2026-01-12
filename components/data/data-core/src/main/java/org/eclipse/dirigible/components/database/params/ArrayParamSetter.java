/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.database.params;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.dirigible.commons.api.helpers.BytesHelper;
import org.eclipse.dirigible.components.database.ParameterizedByIndex;
import org.eclipse.dirigible.components.database.ParameterizedByName;
import org.eclipse.dirigible.database.sql.DataTypeUtils;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

class ArrayParamSetter extends BaseParamSetter {

    /**
     * Checks if is applicable.
     *
     * @param dataType the data type
     * @return true, if is applicable
     */
    @Override
    public boolean isApplicable(String dataType) {
        return DataTypeUtils.isArray(dataType);
    }

    /**
     * Sets the param.
     *
     * @param sourceParam the source param
     * @param paramIndex the param index
     * @param preparedStatement the prepared statement
     * @throws SQLException the SQL exception
     */
    @Override
    public void setParam(JsonElement sourceParam, int paramIndex, ParameterizedByIndex preparedStatement) throws SQLException {
        if (sourceParam.isJsonArray()) {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<String>>() {}.getType();
            List<String> objectList = gson.fromJson(sourceParam, listType);
            preparedStatement.setArray(paramIndex, objectList, "VARCHAR");
            return;
        }

        throwWrongValue(sourceParam, paramIndex, preparedStatement);
    }

    /**
     * Sets the param.
     *
     * @param sourceParam the source param
     * @param paramName the param name
     * @param preparedStatement the prepared statement
     * @throws SQLException the SQL exception
     */
    @Override
    public void setParam(JsonElement sourceParam, String paramName, ParameterizedByName preparedStatement) throws SQLException {
        if (sourceParam.isJsonArray()) {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<String>>() {}.getType();
            List<String> objectList = gson.fromJson(sourceParam, listType);
            preparedStatement.setArray(paramName, objectList, "VARCHAR");
            return;
        }

        throwWrongValue(sourceParam, paramName, preparedStatement);
    }
}
