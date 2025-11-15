package org.eclipse.dirigible.components.database;

import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;

public interface Parameterized {

    /**
     * Sets a parameter.
     *
     * @param index parameter index
     * @param value parameter value
     * @throws SQLException if an error occurred
     * @throws IllegalArgumentException if the parameter does not exist
     * @see PreparedStatement#setObject(int, java.lang.Object)
     */
    void setObject(int index, Object value) throws SQLException;

    /**
     * Sets a parameter.
     *
     * @param index parameter index
     * @param value parameter value
     * @throws SQLException if an error occurred
     * @throws IllegalArgumentException if the parameter does not exist
     * @see PreparedStatement#setString(int, java.lang.String)
     */
    void setString(int index, String value) throws SQLException;

    /**
     * Sets a parameter.
     *
     * @param index parameter index
     * @param value parameter value
     * @throws SQLException if an error occurred
     * @throws IllegalArgumentException if the parameter does not exist
     * @see PreparedStatement#setInt(int, int)
     */
    void setInt(int index, int value) throws SQLException;

    /**
     * Sets the byte.
     *
     * @param index parameter index
     * @param value the value
     * @throws SQLException the SQL exception
     */
    void setByte(int index, byte value) throws SQLException;

    /**
     * Sets the short.
     *
     * @param index parameter index
     * @param value the value
     * @throws SQLException the SQL exception
     */
    void setShort(int index, short value) throws SQLException;

    /**
     * Sets the float.
     *
     * @param index parameter index
     * @param value the value
     * @throws SQLException the SQL exception
     */
    void setFloat(int index, float value) throws SQLException;

    /**
     * Sets the double.
     *
     * @param index parameter index
     * @param value the value
     * @throws SQLException the SQL exception
     */
    void setDouble(int index, double value) throws SQLException;

    /**
     * Sets the big decimal.
     *
     * @param index parameter index
     * @param value the value
     * @throws SQLException the SQL exception
     */
    void setBigDecimal(int index, BigDecimal value) throws SQLException;

    /**
     * Sets the boolean.
     *
     * @param index parameter index
     * @param value the value
     * @throws SQLException the SQL exception
     */
    void setBoolean(int index, boolean value) throws SQLException;

    /**
     * Sets the binary stream.
     *
     * @param index parameter index
     * @param value the value
     * @param length the length
     * @throws SQLException the SQL exception
     */
    void setBinaryStream(int index, InputStream value, int length) throws SQLException;

    /**
     * Sets the binary stream.
     *
     * @param index parameter index
     * @param value the value
     * @param length the length
     * @throws SQLException the SQL exception
     */
    void setBinaryStream(int index, InputStream value, long length) throws SQLException;

    /**
     * Sets a parameter.
     *
     * @param index parameter index
     * @param value parameter value
     * @throws SQLException if an error occurred
     * @throws IllegalArgumentException if the parameter does not exist
     * @see PreparedStatement#setInt(int, int)
     */
    void setLong(int index, long value) throws SQLException;

    /**
     * Sets a parameter.
     *
     * @param index parameter index
     * @param value parameter value
     * @throws SQLException if an error occurred
     * @throws IllegalArgumentException if the parameter does not exist
     * @see PreparedStatement#setTimestamp(int, java.sql.Timestamp)
     */
    void setTimestamp(int index, Timestamp value) throws SQLException;

    /**
     * Sets a parameter.
     *
     * @param index parameter index
     * @param value parameter value
     * @throws SQLException if an error occurred
     * @throws IllegalArgumentException if the parameter does not exist
     * @see PreparedStatement#setDate(int, java.sql.Date)
     */
    void setDate(int index, java.sql.Date value) throws SQLException;

    /**
     * Sets a parameter.
     *
     * @param index parameter index
     * @param value parameter value
     * @param cal parameter cal
     * @throws SQLException if an error occurred
     * @throws IllegalArgumentException if the parameter does not exist
     * @see PreparedStatement#setDate(int, java.sql.Date, Calendar)
     */
    void setDate(int index, java.sql.Date value, Calendar cal) throws SQLException;

    /**
     * Sets the time.
     *
     * @param index parameter index
     * @param value the value
     * @throws SQLException the SQL exception
     */
    void setTime(int index, java.sql.Time value) throws SQLException;

    /**
     * Sets the time.
     *
     * @param index parameter index
     * @param value the value
     * @param cal the cal
     * @throws SQLException the SQL exception
     */
    void setTime(int index, java.sql.Time value, Calendar cal) throws SQLException;

    /**
     * Sets the null.
     *
     * @param index parameter index
     * @param sqlType the sql type
     * @throws SQLException the SQL exception
     */
    void setNull(int index, Integer sqlType) throws SQLException;

    void setObject(int index, Object value, int targetSqlType) throws SQLException;

    Connection getConnection() throws SQLException;

    void addBatch() throws SQLException;

    int getParameterCount() throws SQLException;

    int getParameterType(int sqlParamIndex) throws SQLException;

}
