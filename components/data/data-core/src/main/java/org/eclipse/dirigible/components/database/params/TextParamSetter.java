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

import java.sql.SQLException;

import org.eclipse.dirigible.components.database.NamedParameterStatement;
import org.eclipse.dirigible.components.database.Parameterized;
import org.eclipse.dirigible.database.sql.DataTypeUtils;

import com.google.gson.JsonElement;

class TextParamSetter extends BaseParamSetter {

    /**
     * Checks if is applicable.
     *
     * @param dataType the data type
     * @return true, if is applicable
     */
    @Override
    public boolean isApplicable(String dataType) {
        return DataTypeUtils.isVarchar(dataType) || DataTypeUtils.isText(dataType) || DataTypeUtils.isChar(dataType)
                || DataTypeUtils.isNvarchar(dataType) || DataTypeUtils.isCharacterVarying(dataType);
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
    public void setParam(JsonElement sourceParam, int paramIndex, Parameterized preparedStatement) throws SQLException {
        if (!sourceParam.isJsonPrimitive() || !sourceParam.getAsJsonPrimitive()
                                                          .isString()) {
            throwWrongValue(sourceParam, paramIndex, preparedStatement);
        }
        String value = sourceParam.getAsJsonPrimitive()
                                  .getAsString();
        preparedStatement.setString(paramIndex, value);
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
        if (!sourceParam.isJsonPrimitive() || !sourceParam.getAsJsonPrimitive()
                                                          .isString()) {
            throwWrongValue(sourceParam, paramName, preparedStatement);
        }
        String value = sourceParam.getAsJsonPrimitive()
                                  .getAsString();
        preparedStatement.setString(paramName, value);
    }
}
