package org.eclipse.dirigible.components.api.db.params;

import com.google.gson.JsonElement;
import org.eclipse.dirigible.components.database.NamedParameterStatement;
import org.eclipse.dirigible.database.sql.DataTypeUtils;

import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

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
    public void setParam(JsonElement sourceParam, int paramIndex, PreparedStatement preparedStatement) throws SQLException {
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
    public void setParam(JsonElement sourceParam, String paramName, NamedParameterStatement preparedStatement) throws SQLException {
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
