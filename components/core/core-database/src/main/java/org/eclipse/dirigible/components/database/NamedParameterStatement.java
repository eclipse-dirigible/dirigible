/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.database;

import java.io.InputStream;
import java.sql.*;
import java.util.*;

/**
 * This class wraps around a PreparedStatement and allows the programmer to set parameters by name
 * instead of by index. This eliminates any confusion as to which parameter index represents what.
 * This also means that rearranging the SQL statement or adding a parameter doesn't involve
 * renumbering your indices. Code such as this:
 *
 *
 * Connection con=getConnection(); String query="select * from my_table where name=? or address=?";
 * PreparedStatement p=con.prepareStatement(query); p.setString(1, "bob"); p.setString(2, "123
 * terrace ct"); ResultSet rs=p.executeQuery();
 *
 * can be replaced with:
 *
 * Connection con=getConnection(); String query="select * from my_table where name=:name or
 * address=:address"; NamedParameterStatement p=new NamedParameterStatement(con, query);
 * p.setString("name", "bob"); p.setString("address", "123 terrace ct"); ResultSet
 * rs=p.executeQuery();
 *
 * Sourced from JavaWorld Article @ http://www.javaworld.com/javaworld/jw-04-2007/jw-04-jdbc.html
 *
 * @author adam_crume
 */

public class NamedParameterStatement implements AutoCloseable, ParameterizedByName {

    /** The statement this object is wrapping. */
    private final PreparedStatement statement;

    /** Maps parameter names to arrays of ints which are the parameter indices. */
    private Map<String, int[]> indexMap;

    /**
     * Creates a NamedParameterStatement. Wraps a call to
     * c.Connection#prepareStatement(java.lang.String) prepareStatement.
     *
     * @param connection the database connection
     * @param query the parameterized query
     * @throws SQLException if the statement could not be created
     */
    public NamedParameterStatement(Connection connection, String query) throws SQLException {
        String parsedQuery = parse(query);
        statement = connection.prepareStatement(parsedQuery);
    }

    /**
     * Parses a query with named parameters. The parameter-index mappings are put into the map, and the
     * parsed query is returned. DO NOT CALL FROM CLIENT CODE. This method is non-private so JUnit code
     * can test it.
     *
     * @param query query to parse
     * @return the parsed query
     */
    final String parse(String query) {
        // I was originally using regular expressions, but they didn't work well for ignoring
        // parameter-like strings inside quotes.
        int length = query.length();
        StringBuffer parsedQuery = new StringBuffer(length);
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        int index = 1;
        HashMap<String, List<Integer>> indexes = new HashMap<String, List<Integer>>(10);

        for (int i = 0; i < length; i++) {
            char c = query.charAt(i);
            if (inSingleQuote) {
                if (c == '\'') {
                    inSingleQuote = false;
                }
            } else if (inDoubleQuote) {
                if (c == '"') {
                    inDoubleQuote = false;
                }
            } else {
                if (c == '\'') {
                    inSingleQuote = true;
                } else if (c == '"') {
                    inDoubleQuote = true;
                } else if (c == ':' && i + 1 < length && Character.isJavaIdentifierStart(query.charAt(i + 1))) {
                    int j = i + 2;
                    while (j < length && Character.isJavaIdentifierPart(query.charAt(j))) {
                        j++;
                    }
                    String name = query.substring(i + 1, j);
                    c = '?'; // replace the parameter with a question mark
                    i += name.length(); // skip past the end if the parameter

                    List<Integer> indexList = indexes.get(name);
                    if (indexList == null) {
                        indexList = new LinkedList<Integer>();
                        indexes.put(name, indexList);
                    }
                    indexList.add(Integer.valueOf(index));

                    index++;
                }
            }
            parsedQuery.append(c);
        }

        indexMap = new HashMap<String, int[]>(indexes.size());
        // replace the lists of Integer objects with arrays of ints
        for (Map.Entry<String, List<Integer>> entry : indexes.entrySet()) {
            List<Integer> list = entry.getValue();
            int[] intIndexes = new int[list.size()];
            int i = 0;
            for (Integer x : list) {
                intIndexes[i++] = x.intValue();
            }
            indexMap.put(entry.getKey(), intIndexes);
        }

        return parsedQuery.toString();
    }

    /**
     * Instantiates a new named parameter statement.
     *
     * @param connection the connection
     * @param query the query
     * @param returnGeneratedKeys the return generated keys
     * @throws SQLException the SQL exception
     */
    public NamedParameterStatement(Connection connection, String query, int returnGeneratedKeys) throws SQLException {
        String parsedQuery = parse(query);
        statement = connection.prepareStatement(parsedQuery, returnGeneratedKeys);
    }

    /**
     * Sets a parameter.
     *
     * @param name parameter name
     * @param value parameter value
     * @throws SQLException if an error occurred
     * @throws IllegalArgumentException if the parameter does not exist
     * @see PreparedStatement#setObject(int, java.lang.Object)
     */
    @Override
    public void setObject(String name, Object value) throws SQLException {
        int[] indexes = getIndexes(name);
        for (int i = 0; i < indexes.length; i++) {
            statement.setObject(indexes[i], value);
        }
    }

    /**
     * Returns the indexes for a parameter.
     *
     * @param name parameter name
     * @return parameter indexes
     * @throws IllegalArgumentException if the parameter does not exist
     */
    private int[] getIndexes(String name) {
        int[] indexes = indexMap.get(name);
        if (indexes == null) {
            throw new IllegalArgumentException("Parameter not found: " + name);
        }
        return indexes;
    }

    /**
     * Sets a parameter.
     *
     * @param name parameter name
     * @param value parameter value
     * @throws SQLException if an error occurred
     * @throws IllegalArgumentException if the parameter does not exist
     * @see PreparedStatement#setString(int, java.lang.String)
     */
    @Override
    public void setString(String name, String value) throws SQLException {
        int[] indexes = getIndexes(name);
        for (int i = 0; i < indexes.length; i++) {
            statement.setString(indexes[i], value);
        }
    }

    /**
     * Sets a parameter.
     *
     * @param name parameter name
     * @param value parameter value
     * @throws SQLException if an error occurred
     * @throws IllegalArgumentException if the parameter does not exist
     * @see PreparedStatement#setInt(int, int)
     */
    @Override
    public void setInt(String name, int value) throws SQLException {
        int[] indexes = getIndexes(name);
        for (int i = 0; i < indexes.length; i++) {
            statement.setInt(indexes[i], value);
        }
    }

    /**
     * Sets the byte.
     *
     * @param name the name
     * @param value the value
     * @throws SQLException the SQL exception
     */
    @Override
    public void setByte(String name, byte value) throws SQLException {
        int[] indexes = getIndexes(name);
        for (int i = 0; i < indexes.length; i++) {
            statement.setByte(indexes[i], value);
        }
    }

    /**
     * Sets the short.
     *
     * @param name the name
     * @param value the value
     * @throws SQLException the SQL exception
     */
    @Override
    public void setShort(String name, short value) throws SQLException {
        int[] indexes = getIndexes(name);
        for (int i = 0; i < indexes.length; i++) {
            statement.setShort(indexes[i], value);
        }
    }

    /**
     * Sets the float.
     *
     * @param name the name
     * @param value the value
     * @throws SQLException the SQL exception
     */
    @Override
    public void setFloat(String name, float value) throws SQLException {
        int[] indexes = getIndexes(name);
        for (int i = 0; i < indexes.length; i++) {
            statement.setFloat(indexes[i], value);
        }
    }

    /**
     * Sets the double.
     *
     * @param name the name
     * @param value the value
     * @throws SQLException the SQL exception
     */
    @Override
    public void setDouble(String name, double value) throws SQLException {
        int[] indexes = getIndexes(name);
        for (int i = 0; i < indexes.length; i++) {
            statement.setDouble(indexes[i], value);
        }
    }

    /**
     * Sets the boolean.
     *
     * @param name the name
     * @param value the value
     * @throws SQLException the SQL exception
     */
    @Override
    public void setBoolean(String name, boolean value) throws SQLException {
        int[] indexes = getIndexes(name);
        for (int i = 0; i < indexes.length; i++) {
            statement.setBoolean(indexes[i], value);
        }
    }

    /**
     * Sets the binary stream.
     *
     * @param name the name
     * @param value the value
     * @param length the length
     * @throws SQLException the SQL exception
     */
    @Override
    public void setBinaryStream(String name, InputStream value, int length) throws SQLException {
        int[] indexes = getIndexes(name);
        for (int i = 0; i < indexes.length; i++) {
            statement.setBinaryStream(indexes[i], value, length);
        }
    }

    /**
     * Sets the binary stream.
     *
     * @param name the name
     * @param value the value
     * @param length the length
     * @throws SQLException the SQL exception
     */
    @Override
    public void setBinaryStream(String name, InputStream value, long length) throws SQLException {
        int[] indexes = getIndexes(name);
        for (int i = 0; i < indexes.length; i++) {
            statement.setBinaryStream(indexes[i], value, length);
        }
    }

    /**
     * Sets a parameter.
     *
     * @param name parameter name
     * @param value parameter value
     * @throws SQLException if an error occurred
     * @throws IllegalArgumentException if the parameter does not exist
     * @see PreparedStatement#setInt(int, int)
     */
    @Override
    public void setLong(String name, long value) throws SQLException {
        int[] indexes = getIndexes(name);
        for (int i = 0; i < indexes.length; i++) {
            statement.setLong(indexes[i], value);
        }
    }

    /**
     * Sets a parameter.
     *
     * @param name parameter name
     * @param value parameter value
     * @throws SQLException if an error occurred
     * @throws IllegalArgumentException if the parameter does not exist
     * @see PreparedStatement#setTimestamp(int, java.sql.Timestamp)
     */
    @Override
    public void setTimestamp(String name, Timestamp value) throws SQLException {
        int[] indexes = getIndexes(name);
        for (int i = 0; i < indexes.length; i++) {
            statement.setTimestamp(indexes[i], value);
        }
    }

    /**
     * Sets a parameter.
     *
     * @param name parameter name
     * @param value parameter value
     * @throws SQLException if an error occurred
     * @throws IllegalArgumentException if the parameter does not exist
     * @see PreparedStatement#setDate(int, java.sql.Date)
     */
    @Override
    public void setDate(String name, java.sql.Date value) throws SQLException {
        int[] indexes = getIndexes(name);
        for (int i = 0; i < indexes.length; i++) {
            statement.setDate(indexes[i], value);
        }
    }

    /**
     * Sets a parameter.
     *
     * @param name parameter name
     * @param value parameter value
     * @param cal parameter cal
     * @throws SQLException if an error occurred
     * @throws IllegalArgumentException if the parameter does not exist
     * @see PreparedStatement#setDate(int, java.sql.Date, Calendar)
     */
    @Override
    public void setDate(String name, java.sql.Date value, Calendar cal) throws SQLException {
        int[] indexes = getIndexes(name);
        for (int i = 0; i < indexes.length; i++) {
            statement.setDate(indexes[i], value, cal);
        }
    }

    /**
     * Sets the time.
     *
     * @param name the name
     * @param value the value
     * @throws SQLException the SQL exception
     */
    @Override
    public void setTime(String name, java.sql.Time value) throws SQLException {
        int[] indexes = getIndexes(name);
        for (int i = 0; i < indexes.length; i++) {
            statement.setTime(indexes[i], value);
        }
    }

    /**
     * Sets the time.
     *
     * @param name the name
     * @param value the value
     * @param cal the cal
     * @throws SQLException the SQL exception
     */
    @Override
    public void setTime(String name, java.sql.Time value, Calendar cal) throws SQLException {
        int[] indexes = getIndexes(name);
        for (int i = 0; i < indexes.length; i++) {
            statement.setTime(indexes[i], value, cal);
        }
    }

    /**
     * Sets the null.
     *
     * @param name the name
     * @param sqlType the sql type
     * @throws SQLException the SQL exception
     */
    @Override
    public void setNull(String name, Integer sqlType) throws SQLException {
        int[] indexes = getIndexes(name);
        for (int i = 0; i < indexes.length; i++) {
            statement.setNull(indexes[i], sqlType);
        }
    }

    /**
     * Sets the array.
     *
     * @param name the name
     * @param value the value
     * @param typeName the type name
     * @throws SQLException the SQL exception
     */
    @Override
    public void setArray(String name, List<?> value, String typeName) throws SQLException {
        Connection connection = getStatement().getConnection();
        Array array = connection.createArrayOf(typeName, value.toArray());
        int[] indexes = getIndexes(name);
        for (int i = 0; i < indexes.length; i++) {
            statement.setArray(indexes[i], array);
        }
    }

    /**
     * Returns the underlying statement.
     *
     * @return the statement
     */
    public PreparedStatement getStatement() {
        return statement;
    }

    /**
     * Executes the statement.
     *
     * @return true if the first result is a ResultSet
     * @throws SQLException if an error occurred
     * @see PreparedStatement#execute()
     */
    public boolean execute() throws SQLException {
        return statement.execute();
    }

    /**
     * Executes the statement, which must be a query.
     *
     * @return the query results
     * @throws SQLException if an error occurred
     * @see PreparedStatement#executeQuery()
     */
    public ResultSet executeQuery() throws SQLException {
        return statement.executeQuery();
    }

    /**
     * Executes the statement, which must be an SQL INSERT, UPDATE or DELETE statement; or an SQL
     * statement that returns nothing, such as a DDL statement.
     *
     * @return number of rows affected
     * @throws SQLException if an error occurred
     * @see PreparedStatement#executeUpdate()
     */
    public int executeUpdate() throws SQLException {
        return statement.executeUpdate();
    }

    /**
     * Closes the statement.
     *
     * @throws SQLException if an error occurred
     * @see Statement#close()
     */
    @Override
    public void close() throws SQLException {
        statement.close();
    }

    /**
     * Adds the current set of parameters as a batch entry.
     *
     * @throws SQLException if something went wrong
     */
    public void addBatch() throws SQLException {
        statement.addBatch();
    }

    /**
     * Executes all of the batched statements.
     *
     * See Statement#executeBatch() for details.
     *
     * @return update counts for each statement
     * @throws SQLException if something went wrong
     */
    public int[] executeBatch() throws SQLException {
        return statement.executeBatch();
    }

    /**
     * Gets the generated keys.
     *
     * @return the generated keys
     * @throws SQLException the SQL exception
     */
    public ResultSet getGeneratedKeys() throws SQLException {
        return statement.getGeneratedKeys();
    }

    @Override
    public String toString() {
        return "NamedParameterStatement{" + "statement=" + statement + ", indexMap=" + toString(indexMap) + '}';
    }

    private static String toString(Map<String, int[]> map) {
        if (null == map) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        Iterator<Map.Entry<String, int[]>> iterator = map.entrySet()
                                                         .iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, int[]> entry = iterator.next();
            sb.append(entry.getKey())
              .append("=")
              .append(Arrays.toString(entry.getValue()));
            if (iterator.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
