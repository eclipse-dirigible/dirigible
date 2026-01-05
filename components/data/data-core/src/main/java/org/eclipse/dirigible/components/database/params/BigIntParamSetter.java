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

import java.math.BigInteger;
import java.sql.SQLException;
import java.sql.Types;

import org.eclipse.dirigible.components.database.ParameterizedByIndex;
import org.eclipse.dirigible.components.database.ParameterizedByName;
import org.eclipse.dirigible.database.sql.DataTypeUtils;

import com.google.gson.JsonElement;

class BigIntParamSetter extends BaseParamSetter {

    /**
     * Checks if is applicable.
     *
     * @param dataType the data type
     * @return true, if is applicable
     */
    @Override
    public boolean isApplicable(String dataType) {
        return DataTypeUtils.isBigint(dataType);
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
            BigInteger value = sourceParam.getAsJsonPrimitive()
                                          .getAsBigInteger();
            preparedStatement.setObject(paramIndex, value, Types.BIGINT);
            return;
        }
        if (sourceParam.isJsonPrimitive() && sourceParam.getAsJsonPrimitive()
                                                        .isString()) {
            String stringValue = sourceParam.getAsJsonPrimitive()
                                            .getAsString();
            BigInteger value = new BigInteger(stringValue);
            preparedStatement.setObject(paramIndex, value, Types.BIGINT);
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
        if (sourceParam.isJsonPrimitive() && sourceParam.getAsJsonPrimitive()
                                                        .isNumber()) {
            long value = sourceParam.getAsJsonPrimitive()
                                    .getAsBigInteger()
                                    .longValue();
            preparedStatement.setLong(paramName, value);
            return;
        }
        if (sourceParam.isJsonPrimitive() && sourceParam.getAsJsonPrimitive()
                                                        .isString()) {
            long value = Long.parseLong(sourceParam.getAsJsonPrimitive()
                                                   .getAsString());
            preparedStatement.setLong(paramName, value);
            return;
        }
        throwWrongValue(sourceParam, paramName, preparedStatement);
    }
}
