/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.db;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.eclipse.dirigible.commons.api.helpers.BytesHelper;
import org.eclipse.dirigible.commons.api.helpers.DateTimeUtils;
import org.eclipse.dirigible.components.database.NamedParameterStatement;
import org.eclipse.dirigible.database.sql.DataTypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * The Class ParametersSetter.
 */
class ParametersSetter {

    /** The Constant paramSetters. */
    private static final Set<ParamSetter> paramSetters = Set.of(//
            new BooleanParamSetter(), //
            new TinyIntParamSetter(), //
            new IntegerParamSetter(), //
            new DoubleParamSetter(), //
            new TextParamSetter(), //
            new DateParamSetter(), //
            new TimestampParamSetter(), //
            new TimeParamSetter(), //
            new SmallIntParamSetter(), //
            new BigIntParamSetter(), //
            new RealParamSetter(), //
            new BlobParamSetter());
    private static final Logger LOGGER = LoggerFactory.getLogger(ParametersSetter.class);


    /**
     * The Interface ParamSetter.
     */
    private interface ParamSetter {

        /**
         * Checks if is applicable.
         *
         * @param dataType the data type
         * @return true, if is applicable
         */
        boolean isApplicable(String dataType);

        /**
         * Sets the param.
         *
         * @param sourceParam the source param
         * @param paramIndex the param index
         * @param preparedStatement the prepared statement
         * @param dataType the data type
         * @throws SQLException the SQL exception
         */
        void setParam(JsonElement sourceParam, int paramIndex, PreparedStatement preparedStatement, String dataType) throws SQLException;

        /**
         * Sets the param.
         *
         * @param sourceParam the source param
         * @param paramName the param name
         * @param preparedStatement the prepared statement
         * @param dataType the data type
         * @throws SQLException the SQL exception
         */
        void setParam(JsonElement sourceParam, String paramName, NamedParameterStatement preparedStatement, String dataType)
                throws SQLException;
    }


    /**
     * The Class BaseParamSetter.
     */
    private static abstract class BaseParamSetter implements ParamSetter {

        /**
         * Throw wrong value.
         *
         * @param sourceParam the source param
         * @param dataType the data type
         */
        protected void throwWrongValue(JsonElement sourceParam, String dataType) {
            throw new IllegalArgumentException("Wrong value [" + sourceParam + "] for parameter of type " + dataType);
        }
    }


    /**
     * The Class TextParamSetter.
     */
    private static class TextParamSetter extends BaseParamSetter {

        /**
         * Checks if is applicable.
         *
         * @param dataType the data type
         * @return true, if is applicable
         */
        @Override
        public boolean isApplicable(String dataType) {
            return DataTypeUtils.isVarchar(dataType) || DataTypeUtils.isText(dataType) || DataTypeUtils.isChar(dataType)
                    || DataTypeUtils.isNvarchar(dataType) || DataTypeUtils.isCharacterVarying(dataType);
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
            if (!sourceParam.isJsonPrimitive() || !sourceParam.getAsJsonPrimitive()
                                                              .isString()) {
                throwWrongValue(sourceParam, dataType);
            }
            String value = sourceParam.getAsJsonPrimitive()
                                      .getAsString();
            preparedStatement.setString(paramIndex, value);
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
            if (!sourceParam.isJsonPrimitive() || !sourceParam.getAsJsonPrimitive()
                                                              .isString()) {
                throwWrongValue(sourceParam, dataType);
            }
            String value = sourceParam.getAsJsonPrimitive()
                                      .getAsString();
            preparedStatement.setString(paramName, value);
        }
    }


    /**
     * The Class DateParamSetter.
     */
    private static class DateParamSetter extends BaseParamSetter {

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


    /**
     * The Class TimestampParamSetter.
     */
    private static class TimestampParamSetter extends BaseParamSetter {

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


    /**
     * The Class TimeParamSetter.
     */
    private static class TimeParamSetter extends BaseParamSetter {

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
         * @param dataType the data type
         * @throws SQLException the SQL exception
         */
        @Override
        public void setParam(JsonElement sourceParam, int paramIndex, PreparedStatement preparedStatement, String dataType)
                throws SQLException {
            if (sourceParam.isJsonPrimitive() && sourceParam.getAsJsonPrimitive()
                                                            .isNumber()) {
                Time value = new Time(sourceParam.getAsJsonPrimitive()
                                                 .getAsLong());
                preparedStatement.setTime(paramIndex, value);
                return;
            }

            if (sourceParam.isJsonPrimitive() && sourceParam.getAsJsonPrimitive()
                                                            .isString()) {
                Time value;
                try {
                    value = new Time(Long.parseLong(sourceParam.getAsJsonPrimitive()
                                                               .getAsString()));
                } catch (NumberFormatException e) {
                    // assume XSDTime
                    value = new Time(jakarta.xml.bind.DatatypeConverter.parseTime(sourceParam.getAsJsonPrimitive()
                                                                                             .getAsString())
                                                                       .getTime()
                                                                       .getTime());
                }
                preparedStatement.setTime(paramIndex, value);
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
                Time value = new Time(sourceParam.getAsJsonPrimitive()
                                                 .getAsLong());
                preparedStatement.setTime(paramName, value);
                return;
            }

            if (sourceParam.isJsonPrimitive() && sourceParam.getAsJsonPrimitive()
                                                            .isString()) {
                Time value;
                try {
                    value = new Time(Long.parseLong(sourceParam.getAsJsonPrimitive()
                                                               .getAsString()));
                } catch (NumberFormatException e) {
                    // assume XSDTime
                    value = new Time(jakarta.xml.bind.DatatypeConverter.parseTime(sourceParam.getAsJsonPrimitive()
                                                                                             .getAsString())
                                                                       .getTime()
                                                                       .getTime());
                }
                preparedStatement.setTime(paramName, value);
                return;
            }

            throwWrongValue(sourceParam, dataType);
        }
    }


    /**
     * The Class IntegerParamSetter.
     */
    private static class IntegerParamSetter extends BaseParamSetter {

        /**
         * Checks if is applicable.
         *
         * @param dataType the data type
         * @return true, if is applicable
         */
        @Override
        public boolean isApplicable(String dataType) {
            return DataTypeUtils.isInteger(dataType);
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
                int value = sourceParam.getAsJsonPrimitive()
                                       .getAsInt();
                preparedStatement.setInt(paramIndex, value);
                return;
            }
            if (sourceParam.isJsonPrimitive() && sourceParam.getAsJsonPrimitive()
                                                            .isString()) {
                int value = Integer.parseInt(sourceParam.getAsJsonPrimitive()
                                                        .getAsString());
                preparedStatement.setInt(paramIndex, value);
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
                int value = sourceParam.getAsJsonPrimitive()
                                       .getAsInt();
                preparedStatement.setInt(paramName, value);
                return;
            }
            if (sourceParam.isJsonPrimitive() && sourceParam.getAsJsonPrimitive()
                                                            .isString()) {
                int value = Integer.parseInt(sourceParam.getAsJsonPrimitive()
                                                        .getAsString());
                preparedStatement.setInt(paramName, value);
                return;
            }
            throwWrongValue(sourceParam, dataType);
        }
    }


    /**
     * The Class TinyIntParamSetter.
     */
    private static class TinyIntParamSetter extends BaseParamSetter {

        /**
         * Checks if is applicable.
         *
         * @param dataType the data type
         * @return true, if is applicable
         */
        @Override
        public boolean isApplicable(String dataType) {
            return DataTypeUtils.isTinyint(dataType);
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
                byte value = (byte) sourceParam.getAsJsonPrimitive()
                                               .getAsInt();
                preparedStatement.setByte(paramIndex, value);
                return;
            }
            if (sourceParam.isJsonPrimitive() && sourceParam.getAsJsonPrimitive()
                                                            .isString()) {
                byte value = (byte) Integer.parseInt(sourceParam.getAsJsonPrimitive()
                                                                .getAsString());
                preparedStatement.setByte(paramIndex, value);
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
                byte value = (byte) sourceParam.getAsJsonPrimitive()
                                               .getAsInt();
                preparedStatement.setByte(paramName, value);
                return;
            }
            if (sourceParam.isJsonPrimitive() && sourceParam.getAsJsonPrimitive()
                                                            .isString()) {
                byte value = (byte) Integer.parseInt(sourceParam.getAsJsonPrimitive()
                                                                .getAsString());
                preparedStatement.setByte(paramName, value);
                return;
            }
            throwWrongValue(sourceParam, dataType);
        }
    }


    /**
     * The Class SmallIntParamSetter.
     */
    private static class SmallIntParamSetter extends BaseParamSetter {

        /**
         * Checks if is applicable.
         *
         * @param dataType the data type
         * @return true, if is applicable
         */
        @Override
        public boolean isApplicable(String dataType) {
            return DataTypeUtils.isSmallint(dataType);
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
                short value = (short) sourceParam.getAsJsonPrimitive()
                                                 .getAsInt();
                preparedStatement.setShort(paramIndex, value);
                return;
            }
            if (sourceParam.isJsonPrimitive() && sourceParam.getAsJsonPrimitive()
                                                            .isString()) {
                short value = (short) Integer.parseInt(sourceParam.getAsJsonPrimitive()
                                                                  .getAsString());
                preparedStatement.setShort(paramIndex, value);
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
                short value = (short) sourceParam.getAsJsonPrimitive()
                                                 .getAsInt();
                preparedStatement.setShort(paramName, value);
                return;
            }
            if (sourceParam.isJsonPrimitive() && sourceParam.getAsJsonPrimitive()
                                                            .isString()) {
                short value = (short) Integer.parseInt(sourceParam.getAsJsonPrimitive()
                                                                  .getAsString());
                preparedStatement.setShort(paramName, value);
                return;
            }
            throwWrongValue(sourceParam, dataType);
        }
    }


    /**
     * The Class BigIntParamSetter.
     */
    private static class BigIntParamSetter extends BaseParamSetter {

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
         * @param dataType the data type
         * @throws SQLException the SQL exception
         */
        @Override
        public void setParam(JsonElement sourceParam, int paramIndex, PreparedStatement preparedStatement, String dataType)
                throws SQLException {
            if (sourceParam.isJsonPrimitive() && sourceParam.getAsJsonPrimitive()
                                                            .isNumber()) {
                long value = sourceParam.getAsJsonPrimitive()
                                        .getAsBigInteger()
                                        .longValue();
                preparedStatement.setLong(paramIndex, value);
                return;
            }
            if (sourceParam.isJsonPrimitive() && sourceParam.getAsJsonPrimitive()
                                                            .isString()) {
                long value = Long.parseLong(sourceParam.getAsJsonPrimitive()
                                                       .getAsString());
                preparedStatement.setLong(paramIndex, value);
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
            throwWrongValue(sourceParam, dataType);
        }
    }


    /**
     * The Class RealParamSetter.
     */
    private static class RealParamSetter extends BaseParamSetter {

        /**
         * Checks if is applicable.
         *
         * @param dataType the data type
         * @return true, if is applicable
         */
        @Override
        public boolean isApplicable(String dataType) {
            return DataTypeUtils.isReal(dataType);
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
                float value = sourceParam.getAsJsonPrimitive()
                                         .getAsNumber()
                                         .floatValue();
                preparedStatement.setFloat(paramIndex, value);
                return;
            }
            if (sourceParam.isJsonPrimitive() && sourceParam.getAsJsonPrimitive()
                                                            .isString()) {
                float value = Float.parseFloat(sourceParam.getAsJsonPrimitive()
                                                          .getAsString());
                preparedStatement.setFloat(paramIndex, value);
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
                float value = sourceParam.getAsJsonPrimitive()
                                         .getAsNumber()
                                         .floatValue();
                preparedStatement.setFloat(paramName, value);
                return;
            }
            if (sourceParam.isJsonPrimitive() && sourceParam.getAsJsonPrimitive()
                                                            .isString()) {
                float value = Float.parseFloat(sourceParam.getAsJsonPrimitive()
                                                          .getAsString());
                preparedStatement.setFloat(paramName, value);
                return;
            }
            throwWrongValue(sourceParam, dataType);
        }
    }


    /**
     * The Class DoubleParamSetter.
     */
    private static class DoubleParamSetter extends BaseParamSetter {

        /**
         * Checks if is applicable.
         *
         * @param dataType the data type
         * @return true, if is applicable
         */
        @Override
        public boolean isApplicable(String dataType) {
            return DataTypeUtils.isDouble(dataType) | DataTypeUtils.isDecimal(dataType);
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
                double value = sourceParam.getAsJsonPrimitive()
                                          .getAsNumber()
                                          .doubleValue();
                preparedStatement.setDouble(paramIndex, value);
                return;
            }
            if (sourceParam.isJsonPrimitive() && sourceParam.getAsJsonPrimitive()
                                                            .isString()) {
                double value = Double.parseDouble(sourceParam.getAsJsonPrimitive()
                                                             .getAsString());
                preparedStatement.setDouble(paramIndex, value);
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
                double value = sourceParam.getAsJsonPrimitive()
                                          .getAsNumber()
                                          .doubleValue();
                preparedStatement.setDouble(paramName, value);
                return;
            }
            if (sourceParam.isJsonPrimitive() && sourceParam.getAsJsonPrimitive()
                                                            .isString()) {
                double value = Double.parseDouble(sourceParam.getAsJsonPrimitive()
                                                             .getAsString());
                preparedStatement.setDouble(paramName, value);
                return;
            }
            throwWrongValue(sourceParam, dataType);
        }
    }


    /**
     * The Class BooleanParamSetter.
     */
    private static class BooleanParamSetter extends BaseParamSetter {

        /**
         * Checks if is applicable.
         *
         * @param dataType the data type
         * @return true, if is applicable
         */
        @Override
        public boolean isApplicable(String dataType) {
            return DataTypeUtils.isBoolean(dataType);
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


    /**
     * The Class BlobParamSetter.
     */
    private static class BlobParamSetter extends BaseParamSetter {

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
         * @param dataType the data type
         * @throws SQLException the SQL exception
         */
        @Override
        public void setParam(JsonElement sourceParam, int paramIndex, PreparedStatement preparedStatement, String dataType)
                throws SQLException {
            if (sourceParam.isJsonArray()) {
                byte[] bytes = BytesHelper.jsonToBytes(sourceParam.getAsJsonArray()
                                                                  .toString());
                preparedStatement.setBinaryStream(paramIndex, new ByteArrayInputStream(bytes), bytes.length);
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
            if (sourceParam.isJsonArray()) {
                byte[] bytes = BytesHelper.jsonToBytes(sourceParam.getAsJsonArray()
                                                                  .toString());
                preparedStatement.setBinaryStream(paramName, new ByteArrayInputStream(bytes), bytes.length);
                return;
            }

            throwWrongValue(sourceParam, dataType);
        }
    }

    static void setParameters(JsonElement parameters, NamedParameterStatement preparedStatement) throws SQLException {
        if (!(parameters instanceof JsonArray parametersArray)) {
            throw new IllegalArgumentException("Parameters must be provided as a JSON array, e.g. [1, 'John', 9876]");
        }

        for (JsonElement parameterElement : parametersArray) {
            setParameter(preparedStatement, parameterElement);
        }
    }

    private static void setParameter(NamedParameterStatement preparedStatement, JsonElement parameterElement)
            throws IllegalArgumentException, SQLException {

        if (parameterElement.isJsonObject()) {
            setJsonObjectParam(preparedStatement, parameterElement);
            return;
        }

        throw new IllegalArgumentException("Parameters must contain objects only. Parameter element: " + parameterElement);
    }

    private static void setJsonObjectParam(NamedParameterStatement preparedStatement, JsonElement parameterElement) throws SQLException {

        ParamJsonObject paramJsonObject = ParamJsonObject.fromJsonElement(parameterElement);

        String name = paramJsonObject.getName();
        String dataType = paramJsonObject.getType();

        ParamSetter paramSetter = getParamSetter(dataType);
        JsonElement valueElement = paramJsonObject.getValueElement();
        if (null == valueElement || valueElement.isJsonNull()) {
            Integer sqlType = DataTypeUtils.getSqlTypeByDataType(dataType);
            preparedStatement.setNull(name, sqlType);
            return;
        }

        paramSetter.setParam(valueElement, name, preparedStatement, dataType);
    }

    private static ParamSetter getParamSetter(String dataType) {
        return paramSetters.stream()
                           .filter(ps -> ps.isApplicable(dataType))
                           .findFirst()
                           .orElseThrow(() -> new IllegalArgumentException(
                                   "Parameter 'type'[" + dataType + "] must be a string representing a valid database type name"));
    }

    static void setParameters(JsonElement parameters, PreparedStatement preparedStatement) throws SQLException {
        if (!(parameters instanceof JsonArray parametersArray)) {
            throw new IllegalArgumentException("Parameters must be provided as a JSON array, e.g. [1, 'John', 9876]");
        }

        int paramIndex = 1;
        for (JsonElement parameterElement : parametersArray) {
            setParameter(preparedStatement, paramIndex, parameterElement);
            paramIndex++;
        }
    }

    private static void setParameter(PreparedStatement preparedStatement, int sqlParamIndex, JsonElement parameterElement)
            throws IllegalArgumentException, SQLException {
        if (parameterElement.isJsonPrimitive()) {
            setJsonPrimitiveParam(preparedStatement, sqlParamIndex, parameterElement);
            return;
        }

        throw new IllegalArgumentException("Parameters must contain primitives only. Parameter element: " + parameterElement);
    }

    /**
     * Sets the json primitive param.
     *
     * @param preparedStatement the prepared statement
     * @param paramIndex the param index
     * @param parameterElement the parameter element
     * @throws SQLException the SQL exception
     */
    private static void setJsonPrimitiveParam(PreparedStatement preparedStatement, int paramIndex, JsonElement parameterElement)
            throws SQLException {
        if (parameterElement.getAsJsonPrimitive()
                            .isBoolean()) {
            preparedStatement.setBoolean(paramIndex, parameterElement.getAsBoolean());
            return;
        }

        if (parameterElement.getAsJsonPrimitive()
                            .isString()) {
            String paramStringValue = parameterElement.getAsString();

            Optional<Timestamp> timestamp = DateTimeUtils.optionallyParseDateTime(paramStringValue);
            if (timestamp.isPresent()) {
                preparedStatement.setTimestamp(paramIndex, timestamp.get());
                return;
            }

            Optional<Date> date = DateTimeUtils.optionallyParseDate(paramStringValue);
            if (date.isPresent()) {
                preparedStatement.setDate(paramIndex, date.get());
                return;
            }

            Optional<Time> time = DateTimeUtils.optionallyParseTime(paramStringValue);
            if (time.isPresent()) {
                preparedStatement.setTime(paramIndex, time.get());
                return;
            }

            preparedStatement.setString(paramIndex, parameterElement.getAsString());
            return;
        }

        if (parameterElement.getAsJsonPrimitive()
                            .isNumber()) {
            setNumber(preparedStatement, paramIndex, parameterElement);
            return;
        }

        throw new IllegalArgumentException("The type of parameter [" + parameterElement + "] as index [" + paramIndex + "] is unknown");
    }

    /**
     * Sets the number.
     *
     * @param preparedStatement the prepared statement
     * @param paramIndex the param index
     * @param parameterElement the parameter element
     * @throws SQLException the SQL exception
     */
    private static void setNumber(PreparedStatement preparedStatement, int paramIndex, JsonElement parameterElement) throws SQLException {
        if (isFloatingPointValue(parameterElement)) {
            try {
                preparedStatement.setDouble(paramIndex, parameterElement.getAsDouble());
                return;
            } catch (SQLException | ClassCastException e) {
                LOGGER.debug("Failed to set value [{}] at index [{}] as double", parameterElement, paramIndex, e);
                // Do nothing
            }
        }

        try {
            preparedStatement.setInt(paramIndex, parameterElement.getAsInt());
            return;
        } catch (SQLException | ClassCastException e) {
            LOGGER.debug("Failed to set value [{}] at index [{}] as int", parameterElement, paramIndex, e);
            // Do nothing
        }

        try {
            preparedStatement.setShort(paramIndex, parameterElement.getAsShort());
            return;
        } catch (SQLException | ClassCastException e) {
            LOGGER.debug("Failed to set value [{}] at index [{}] as short", parameterElement, paramIndex, e);
            // Do nothing
        }

        try {
            preparedStatement.setLong(paramIndex, parameterElement.getAsLong());
            return;
        } catch (SQLException | ClassCastException e) {
            LOGGER.debug("Failed to set value [{}] at index [{}] as long", parameterElement, paramIndex, e);
            // Do nothing
        }

        try {
            preparedStatement.setBigDecimal(paramIndex, parameterElement.getAsBigDecimal());
            return;
        } catch (SQLException | ClassCastException e) {
            LOGGER.debug("Failed to set value [{}] at index [{}] as big decimal", parameterElement, paramIndex, e);
            // Do nothing
        }

        preparedStatement.setObject(paramIndex, parameterElement.getAsNumber()
                                                                .toString());
    }

    private static boolean isFloatingPointValue(JsonElement parameterElement) {
        String numberStr = Double.toString(parameterElement.getAsDouble());

        return numberStr.contains(".") || numberStr.contains("e") || numberStr.contains("E");
    }

    static void setManyParameters(JsonElement parametersElement, PreparedStatement preparedStatement,
            Optional<InsertParameters> insertParameters) throws IllegalArgumentException, SQLException {
        JsonArray parametersArray = getParametersArray(parametersElement);

        ParameterMetaData paramsMetaData = preparedStatement.getParameterMetaData();
        int sqlParametersCount = paramsMetaData.getParameterCount();

        for (int paramsIdx = 0; paramsIdx < parametersArray.size(); paramsIdx++) {
            JsonElement parameterElement = parametersArray.get(paramsIdx);

            if (!parameterElement.isJsonArray()) {
                throw new IllegalArgumentException(
                        "Parameters at index [" + paramsIdx + "] must be provided as a JSON array, e.g. [1,\"John\",9876]");
            }
            JsonArray elementParametersArray = parameterElement.getAsJsonArray();

            int paramsCount = elementParametersArray.size();
            if (sqlParametersCount != paramsCount) {
                String errMsg = "Provided invalid parameters count of [" + paramsCount + "] at index [" + paramsIdx
                        + "]. Expected parameters count: " + sqlParametersCount;
                throw new IllegalArgumentException(errMsg);
            }

            for (int paramIdx = 0; paramIdx < elementParametersArray.size(); paramIdx++) {
                JsonElement elementParameter = elementParametersArray.get(paramIdx);
                int sqlParamIndex = (paramIdx + 1);

                setParameter(preparedStatement, sqlParamIndex, elementParameter);
            }

            preparedStatement.addBatch();
        }
    }

    private static JsonArray getParametersArray(JsonElement parametersElement) {
        if (!parametersElement.isJsonArray()) {
            throw new IllegalArgumentException(
                    "Parameters must be provided as a JSON array of JSON arrays, e.g. [[1,\"John\",9876],[2,\"Mary\",1234]]");
        }
        return parametersElement.getAsJsonArray();
    }

}
