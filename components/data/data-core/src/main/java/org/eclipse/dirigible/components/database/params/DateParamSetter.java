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

import java.sql.Date;
import java.sql.SQLException;
import java.util.Optional;

import org.eclipse.dirigible.commons.api.helpers.DateTimeUtils;
import org.eclipse.dirigible.components.database.ParameterizedByIndex;
import org.eclipse.dirigible.components.database.ParameterizedByName;
import org.eclipse.dirigible.database.sql.DataTypeUtils;

import com.google.gson.JsonElement;

class DateParamSetter extends BaseParamSetter {

    /**
     * Checks if is applicable.
     *
     * @param dataType the data type
     * @return true, if is applicable
     */
    @Override
    public boolean isApplicable(String dataType) {
        return DataTypeUtils.isDate(dataType);
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
        if (sourceParam.isJsonPrimitive() && sourceParam.getAsJsonPrimitive()
                                                        .isNumber()) {
            Date value = new Date(sourceParam.getAsJsonPrimitive()
                                             .getAsLong());
            preparedStatement.setDate(paramIndex, value);
            return;
        }

        if (sourceParam.isJsonPrimitive() && sourceParam.getAsJsonPrimitive()
                                                        .isString()) {
            String paramStringValue = sourceParam.getAsString();
            Optional<Date> date = DateTimeUtils.optionallyParseDate(paramStringValue);
            if (date.isPresent()) {
                preparedStatement.setDate(paramIndex, date.get());
                return;
            }
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
        if (sourceParam.isJsonPrimitive() && sourceParam.getAsJsonPrimitive()
                                                        .isNumber()) {
            Date value = new Date(sourceParam.getAsJsonPrimitive()
                                             .getAsLong());
            preparedStatement.setDate(paramName, value);
            return;
        }

        if (sourceParam.isJsonPrimitive() && sourceParam.getAsJsonPrimitive()
                                                        .isString()) {
            String paramStringValue = sourceParam.getAsString();
            Optional<Date> date = DateTimeUtils.optionallyParseDate(paramStringValue);
            if (date.isPresent()) {
                preparedStatement.setDate(paramName, date.get());
                return;
            }
        }
        throwWrongValue(sourceParam, paramName, preparedStatement);
    }
}

