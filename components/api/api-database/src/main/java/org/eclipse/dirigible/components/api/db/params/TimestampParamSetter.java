package org.eclipse.dirigible.components.api.db.params;

import com.google.gson.JsonElement;
import org.eclipse.dirigible.components.database.NamedParameterStatement;
import org.eclipse.dirigible.database.sql.DataTypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

class TimestampParamSetter extends BaseParamSetter {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(TimestampParamSetter.class);

    /** The Constant SIMPLE_DATE_FORMAT_WITHOUT_ZONE. */
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT_WITHOUT_ZONE =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());

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
            Timestamp value;
            try {
                value = new Timestamp(Long.parseLong(sourceParam.getAsJsonPrimitive()
                                                                .getAsString()));
            } catch (NumberFormatException e) {
                String timestampString = sourceParam.getAsJsonPrimitive()
                                                    .getAsString();
                value = new Timestamp(getTime(timestampString));
            }
            preparedStatement.setTimestamp(paramIndex, value);
            return;
        }
        throwWrongValue(sourceParam, dataType);
    }

    /**
     * Gets the time.
     *
     * @param timestampString the timestamp string
     * @return the time
     */
    private long getTime(String timestampString) {
        try {
            // assume date string in ISO format e.g. 2018-05-22T21:00:00.000Z
            Calendar calendar = jakarta.xml.bind.DatatypeConverter.parseDateTime(timestampString);
            return calendar.getTime()
                           .getTime();
        } catch (IllegalArgumentException ex) {
            logger.debug("Failed to parse timestamp string [{}]", timestampString, ex);

            try {
                java.util.Date date = SIMPLE_DATE_FORMAT_WITHOUT_ZONE.parse(timestampString);
                return date.getTime();
            } catch (ParseException e) {
                throw new IllegalArgumentException("Cannot get time from timestamp string " + timestampString, e);
            }
        }
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
            Timestamp value;
            try {
                value = new Timestamp(Long.parseLong(sourceParam.getAsJsonPrimitive()
                                                                .getAsString()));
            } catch (NumberFormatException e) {
                String timestampString = sourceParam.getAsJsonPrimitive()
                                                    .getAsString();
                value = new Timestamp(getTime(timestampString));
            }
            preparedStatement.setTimestamp(paramName, value);
            return;
        }
        throwWrongValue(sourceParam, dataType);
    }
}
