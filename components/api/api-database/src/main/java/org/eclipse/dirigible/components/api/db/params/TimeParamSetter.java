package org.eclipse.dirigible.components.api.db.params;

import com.google.gson.JsonElement;
import org.eclipse.dirigible.commons.api.helpers.DateTimeUtils;
import org.eclipse.dirigible.components.database.NamedParameterStatement;
import org.eclipse.dirigible.database.sql.DataTypeUtils;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;
import java.util.Optional;

class TimeParamSetter extends BaseParamSetter {

    /**
     * Checks if is applicable.
     *
     * @param dataType the data type
     * @return true, if is applicable
     */
    @Override
    public boolean isApplicable(String dataType) {
        return DataTypeUtils.isTime(dataType);
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
            Time value = new Time(sourceParam.getAsJsonPrimitive()
                                             .getAsLong());
            preparedStatement.setTime(paramIndex, value);
            return;
        }

        if (sourceParam.isJsonPrimitive() && sourceParam.getAsJsonPrimitive()
                                                        .isString()) {
            String paramStringValue = sourceParam.getAsString();
            Optional<Time> time = DateTimeUtils.optionallyParseTime(paramStringValue);
            if (time.isPresent()) {
                preparedStatement.setTime(paramIndex, time.get());
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
    public void setParam(JsonElement sourceParam, String paramName, NamedParameterStatement preparedStatement) throws SQLException {
        if (sourceParam.isJsonPrimitive() && sourceParam.getAsJsonPrimitive()
                                                        .isNumber()) {
            Time value = new Time(sourceParam.getAsJsonPrimitive()
                                             .getAsLong());
            preparedStatement.setTime(paramName, value);
            return;
        }

        if (sourceParam.isJsonPrimitive() && sourceParam.getAsJsonPrimitive()
                                                        .isString()) {
            String paramStringValue = sourceParam.getAsString();
            Optional<Time> time = DateTimeUtils.optionallyParseTime(paramStringValue);
            if (time.isPresent()) {
                preparedStatement.setTime(paramName, time.get());
                return;
            }
        }

        throwWrongValue(sourceParam, paramName, preparedStatement);
    }
}
