/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.db.params;

import com.google.gson.JsonElement;
import org.eclipse.dirigible.components.database.NamedParameterStatement;
import org.eclipse.dirigible.database.sql.DataTypeUtils;

import java.sql.PreparedStatement;
import java.sql.SQLException;

class RealParamSetter extends BaseParamSetter {

    /**
     * Checks if is applicable.
     *
     * @param dataType the data type
     * @return true, if is applicable
     */
    @Override
    public boolean isApplicable(String dataType) {
        return DataTypeUtils.isReal(dataType);
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
    public void setParam(JsonElement sourceParam, int paramIndex, PreparedStatement preparedStatement) throws SQLException {
        if (sourceParam.isJsonPrimitive() && sourceParam.getAsJsonPrimitive()
                                                        .isNumber()) {
            float value = sourceParam.getAsJsonPrimitive()
                                     .getAsNumber()
                                     .floatValue();
            preparedStatement.setFloat(paramIndex, value);
            return;
        }
        if (sourceParam.isJsonPrimitive() && sourceParam.getAsJsonPrimitive()
                                                        .isString()) {
            float value = Float.parseFloat(sourceParam.getAsJsonPrimitive()
                                                      .getAsString());
            preparedStatement.setFloat(paramIndex, value);
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
    public void setParam(JsonElement sourceParam, String paramName, NamedParameterStatement preparedStatement) throws SQLException {
        if (sourceParam.isJsonPrimitive() && sourceParam.getAsJsonPrimitive()
                                                        .isNumber()) {
            float value = sourceParam.getAsJsonPrimitive()
                                     .getAsNumber()
                                     .floatValue();
            preparedStatement.setFloat(paramName, value);
            return;
        }
        if (sourceParam.isJsonPrimitive() && sourceParam.getAsJsonPrimitive()
                                                        .isString()) {
            float value = Float.parseFloat(sourceParam.getAsJsonPrimitive()
                                                      .getAsString());
            preparedStatement.setFloat(paramName, value);
            return;
        }
        throwWrongValue(sourceParam, paramName, preparedStatement);
    }
}
