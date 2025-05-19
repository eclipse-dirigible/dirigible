package org.eclipse.dirigible.components.api.db.params;

import com.google.gson.JsonElement;
import org.eclipse.dirigible.commons.api.helpers.DateTimeUtils;
import org.eclipse.dirigible.components.database.NamedParameterStatement;
import org.eclipse.dirigible.database.sql.DataTypeUtils;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

class DateParamSetter extends BaseParamSetter {

    /**
     * Checks if is applicable.
     *
     * @param dataType the data type
     * @return true, if is applicable
     */
    @Override
    public boolean isApplicable(String dataType) {
        return DataTypeUtils.isDate(dataType);
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
            Date value = new Date(sourceParam.getAsJsonPrimitive()
                                             .getAsLong());
            preparedStatement.setDate(paramIndex, value);
            return;
        }

        if (sourceParam.isJsonPrimitive() && sourceParam.getAsJsonPrimitive()
                                                        .isString()) {
            String paramStringValue = sourceParam.getAsString();
            Optional<Date> date = DateTimeUtils.optionallyParseDate(paramStringValue);
            if (date.isPresent()) {
                preparedStatement.setDate(paramIndex, date.get());
                return;
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
            Date value = new Date(sourceParam.getAsJsonPrimitive()
                                             .getAsLong());
            preparedStatement.setDate(paramName, value);
            return;
        }

        if (sourceParam.isJsonPrimitive() && sourceParam.getAsJsonPrimitive()
                                                        .isString()) {
            String paramStringValue = sourceParam.getAsString();
            Optional<Date> date = DateTimeUtils.optionallyParseDate(paramStringValue);
            if (date.isPresent()) {
                preparedStatement.setDate(paramName, date.get());
                return;
            }
        }
        throwWrongValue(sourceParam, dataType);
    }
}

