package org.eclipse.dirigible.components.api.db.params;

import com.google.gson.JsonElement;

import java.sql.PreparedStatement;

abstract class BaseParamSetter implements ParamSetter {

    /**
     * Throw wrong value.
     *
     * @param sourceParam the source param
     * @param dataType the data type
     */
    protected void throwWrongValue(JsonElement sourceParam, String dataType) throws IllegalArgumentException {
        throw new IllegalArgumentException("Wrong value [" + sourceParam + "] for parameter of type " + dataType);
    }

    protected void throwWrongValue(JsonElement sourceParam, int paramIndex, PreparedStatement preparedStatement)
            throws IllegalArgumentException {
        throw new IllegalArgumentException(
                "Wrong value [" + sourceParam + "] at index [" + paramIndex + "] for statement: " + preparedStatement);
    }

}


