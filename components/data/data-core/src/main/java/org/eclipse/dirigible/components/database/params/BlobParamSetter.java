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
import java.sql.SQLException;

import org.eclipse.dirigible.commons.api.helpers.BytesHelper;
import org.eclipse.dirigible.components.database.ParameterizedByIndex;
import org.eclipse.dirigible.components.database.ParameterizedByName;
import org.eclipse.dirigible.database.sql.DataTypeUtils;

import com.google.gson.JsonElement;

class BlobParamSetter extends BaseParamSetter {

    /**
     * Checks if is applicable.
     *
     * @param dataType the data type
     * @return true, if is applicable
     */
    @Override
    public boolean isApplicable(String dataType) {
        return DataTypeUtils.isBlob(dataType);
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
            byte[] bytes = BytesHelper.jsonToBytes(sourceParam.getAsJsonArray()
                                                              .toString());
            preparedStatement.setBinaryStream(paramIndex, new ByteArrayInputStream(bytes), bytes.length);
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
            byte[] bytes = BytesHelper.jsonToBytes(sourceParam.getAsJsonArray()
                                                              .toString());
            preparedStatement.setBinaryStream(paramName, new ByteArrayInputStream(bytes), bytes.length);
            return;
        }

        throwWrongValue(sourceParam, paramName, preparedStatement);
    }
}
