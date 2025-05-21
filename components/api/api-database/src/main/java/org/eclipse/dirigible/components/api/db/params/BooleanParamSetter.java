package org.eclipse.dirigible.components.api.db.params;

import com.google.gson.JsonElement;
import org.eclipse.dirigible.components.database.NamedParameterStatement;
import org.eclipse.dirigible.database.sql.DataTypeUtils;

import java.sql.PreparedStatement;
import java.sql.SQLException;

class BooleanParamSetter extends BaseParamSetter {

    /**
     * Checks if is applicable.
     *
     * @param dataType the data type
     * @return true, if is applicable
     */
    @Override
    public boolean isApplicable(String dataType) {
        return DataTypeUtils.isBoolean(dataType) || DataTypeUtils.isBit(dataType);
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
                                                        .isBoolean()) {
            boolean value = sourceParam.getAsJsonPrimitive()
                                       .getAsBoolean();
            preparedStatement.setBoolean(paramIndex, value);
            return;
        }

        if (sourceParam.isJsonPrimitive() && sourceParam.getAsJsonPrimitive()
                                                        .isNumber()) {
            boolean value = sourceParam.getAsJsonPrimitive()
                                       .getAsBoolean();
            preparedStatement.setBoolean(paramIndex, value);
            return;
        }
        if (sourceParam.isJsonPrimitive() && sourceParam.getAsJsonPrimitive()
                                                        .isString()) {
            boolean value = Boolean.parseBoolean(sourceParam.getAsJsonPrimitive()
                                                            .getAsString());
            preparedStatement.setBoolean(paramIndex, value);
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
     * @param dataType the data type
     * @throws SQLException the SQL exception
     */
    @Override
    public void setParam(JsonElement sourceParam, String paramName, NamedParameterStatement preparedStatement, String dataType)
            throws SQLException {
        if (sourceParam.isJsonPrimitive() && sourceParam.getAsJsonPrimitive()
                                                        .isNumber()) {
            boolean value = sourceParam.getAsJsonPrimitive()
                                       .getAsBoolean();
            preparedStatement.setBoolean(paramName, value);
            return;
        }
        if (sourceParam.isJsonPrimitive() && sourceParam.getAsJsonPrimitive()
                                                        .isString()) {
            boolean value = Boolean.parseBoolean(sourceParam.getAsJsonPrimitive()
                                                            .getAsString());
            preparedStatement.setBoolean(paramName, value);
            return;
        }
        throwWrongValue(sourceParam, dataType);
    }
}
