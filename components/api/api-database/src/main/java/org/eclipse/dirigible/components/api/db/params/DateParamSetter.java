package org.eclipse.dirigible.components.api.db.params;

import com.google.gson.JsonElement;
import org.eclipse.dirigible.components.database.NamedParameterStatement;
import org.eclipse.dirigible.database.sql.DataTypeUtils;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;

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
            Date value;
            try {
                value = new Date(Long.parseLong(sourceParam.getAsJsonPrimitive()
                                                           .getAsString()));
            } catch (NumberFormatException e) {
                // assume date string in ISO format e.g. 2018-05-22T21:00:00.000Z
                value = new Date(jakarta.xml.bind.DatatypeConverter.parseDateTime(sourceParam.getAsJsonPrimitive()
                                                                                             .getAsString())
                                                                   .getTime()
                                                                   .getTime());
            }
            preparedStatement.setDate(paramIndex, value);
            return;
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
            Date value;
            try {
                value = new Date(Long.parseLong(sourceParam.getAsJsonPrimitive()
                                                           .getAsString()));
            } catch (NumberFormatException e) {
                // assume date string in ISO format e.g. 2018-05-22T21:00:00.000Z
                value = new Date(jakarta.xml.bind.DatatypeConverter.parseDateTime(sourceParam.getAsJsonPrimitive()
                                                                                             .getAsString())
                                                                   .getTime()
                                                                   .getTime());
            }
            preparedStatement.setDate(paramName, value);
            return;
        }
        throwWrongValue(sourceParam, dataType);
    }
}

