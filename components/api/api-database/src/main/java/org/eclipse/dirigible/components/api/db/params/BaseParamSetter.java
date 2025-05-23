package org.eclipse.dirigible.components.api.db.params;

import com.google.gson.JsonElement;
import org.eclipse.dirigible.components.database.NamedParameterStatement;

import java.sql.PreparedStatement;

abstract class BaseParamSetter implements ParamSetter {

    protected void throwWrongValue(JsonElement sourceParam, String paramName, NamedParameterStatement preparedStatement)
            throws IllegalArgumentException {
        throw new IllegalArgumentException(
                "Wrong value [" + sourceParam + "] for parameter with name [" + paramName + "] for statement: " + preparedStatement);
    }

    protected void throwWrongValue(JsonElement sourceParam, int paramIndex, PreparedStatement preparedStatement)
            throws IllegalArgumentException {
        throw new IllegalArgumentException(
                "Wrong value [" + sourceParam + "] at index [" + paramIndex + "] for statement: " + preparedStatement);
    }

}


