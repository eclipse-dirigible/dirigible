package org.eclipse.dirigible.components.api.db.params;

import com.google.gson.JsonElement;
import org.eclipse.dirigible.commons.api.helpers.DateTimeUtils;
import org.eclipse.dirigible.components.database.NamedParameterStatement;
import org.eclipse.dirigible.database.sql.DataTypeUtils;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;

class TimestampParamSetter extends BaseParamSetter {

    /**
     * Checks if is applicable.
     *
     * @param dataType the data type
     * @return true, if is applicable
     */
    @Override
    public boolean isApplicable(String dataType) {
        return DataTypeUtils.isTimestamp(dataType) || DataTypeUtils.isDateTime(dataType);
    }

    /**
     * Sets the param.
     *
     * @param sourceParam the source param
     * @param paramIndex the param index
     * @param preparedStatement the prepared statement
     * @param dataType the data type
     * @throws SQLException the SQL exception
     */
    @Override
    public void setParam(JsonElement sourceParam, int paramIndex, PreparedStatement preparedStatement, String dataType)
            throws SQLException {

        if (sourceParam.isJsonPrimitive() && sourceParam.getAsJsonPrimitive()
                                                        .isNumber()) {
            Timestamp value = new Timestamp(sourceParam.getAsJsonPrimitive()
                                                       .getAsLong());
            preparedStatement.setTimestamp(paramIndex, value);
            return;
        }

        if (sourceParam.isJsonPrimitive() && sourceParam.getAsJsonPrimitive()
                                                        .isString()) {
            String paramStringValue = sourceParam.getAsString();

            Optional<Timestamp> timestamp = DateTimeUtils.optionallyParseDateTime(paramStringValue);
            if (timestamp.isPresent()) {
                preparedStatement.setTimestamp(paramIndex, timestamp.get());
            }
        }
        throwWrongValue(sourceParam, dataType);
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
            Timestamp value = new Timestamp(sourceParam.getAsJsonPrimitive()
                                                       .getAsLong());
            preparedStatement.setTimestamp(paramName, value);
            return;
        }

        if (sourceParam.isJsonPrimitive() && sourceParam.getAsJsonPrimitive()
                                                        .isString()) {
            String paramStringValue = sourceParam.getAsString();

            Optional<Timestamp> timestamp = DateTimeUtils.optionallyParseDateTime(paramStringValue);
            if (timestamp.isPresent()) {
                preparedStatement.setTimestamp(paramName, timestamp.get());
            }
        }
        throwWrongValue(sourceParam, dataType);
    }

}
