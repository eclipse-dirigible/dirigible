/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.database.params;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.eclipse.dirigible.components.database.DatabaseSystem;
import org.eclipse.dirigible.components.database.ParameterizedByIndex;
import org.eclipse.dirigible.components.database.ParameterizedByName;
import org.eclipse.dirigible.components.database.domain.ColumnMetadata;
import org.eclipse.dirigible.components.database.domain.TableMetadata;
import org.eclipse.dirigible.components.database.helpers.DatabaseMetadataHelper;
import org.eclipse.dirigible.components.database.sql.SqlParser;
import org.eclipse.dirigible.database.sql.DataTypeUtils;
import org.eclipse.dirigible.database.sql.dialects.SqlDialectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.insert.Insert;

/**
 * The Class ParametersSetter.
 */
public class ParametersSetter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParametersSetter.class);

    /** The Constant paramSetters. */
    private static final Set<ParamSetter> paramSetters = Set.of(//
            new ArrayParamSetter(), //
            new BigDecimalParamSetter(), //
            new BigIntParamSetter(), //
            new BlobParamSetter(), //
            new BooleanParamSetter(), //
            new DateParamSetter(), //
            new DoubleParamSetter(), //
            new IntegerParamSetter(), //
            new RealParamSetter(), //
            new SmallIntParamSetter(), //
            new TextParamSetter(), //
            new TimeParamSetter(), //
            new TimestampParamSetter(), //
            new TinyIntParamSetter());

    public static void setNamedParameters(JsonElement parameters, ParameterizedByName preparedStatement) throws SQLException {
        if (!parameters.isJsonArray()) {
            throw new IllegalArgumentException("Parameters must be provided as a JSON array, e.g. [1, 'John', 9876]. Parameters ["
                    + parameters + "]. Statement: " + preparedStatement);
        }

        for (JsonElement parameterElement : parameters.getAsJsonArray()) {
            setNamedParameter(preparedStatement, parameterElement);
        }
    }

    private static void setNamedParameter(ParameterizedByName preparedStatement, JsonElement parameterElement)
            throws IllegalArgumentException, SQLException {

        if (!parameterElement.isJsonObject()) {
            throw new IllegalArgumentException(
                    "Parameters must contain objects only. Parameter element [" + parameterElement + "]. Statement: " + preparedStatement);
        }

        setNamedJsonObjectParam(preparedStatement, parameterElement);
    }

    private static void setNamedJsonObjectParam(ParameterizedByName preparedStatement, JsonElement parameterElement) throws SQLException {
        try {
            NamedParamJsonObject namedParamJsonObject = NamedParamJsonObject.fromJsonElement(parameterElement);

            String name = namedParamJsonObject.getName();
            String dataType = namedParamJsonObject.getType();

            JsonElement valueElement = namedParamJsonObject.getValueElement();
            if (null == valueElement || valueElement.isJsonNull()) {
                Integer sqlType = DataTypeUtils.getSqlTypeByDataType(dataType);
                LOGGER.debug("Dirigible sql type [{}] is mapped to [{}] for element [{}]", dataType, sqlType, parameterElement);

                preparedStatement.setNull(name, sqlType);
                return;
            }

            setNamedParamUsingSetter(preparedStatement, parameterElement, dataType, valueElement, name);
        } catch (IllegalArgumentException ex) {
            String errMsg = "Failed to set named param for parameter [" + parameterElement + "] in statement: " + preparedStatement;
            throw new IllegalArgumentException(errMsg, ex);
        }
    }

    private static void setNamedParamUsingSetter(ParameterizedByName preparedStatement, JsonElement parameterElement, String dataType,
            JsonElement valueElement, String name) throws SQLException {
        try {
            ParamSetter paramSetter = findParamSetterForType(dataType);
            LOGGER.debug("Found param setter [{}] for dirigible type [{}] for element [{}]", paramSetter, dataType, parameterElement);

            paramSetter.setParam(valueElement, name, preparedStatement);
        } catch (IllegalArgumentException ex) {
            String errMsg = "Failed to set named param with name [" + name + "] and type [" + dataType + "] for element: [" + valueElement
                    + "]. Statement: " + preparedStatement;
            throw new IllegalArgumentException(errMsg, ex);
        }
    }

    private static ParamSetter findParamSetterForType(String dataType) {
        return paramSetters.stream()
                           .filter(ps -> ps.isApplicable(dataType))
                           .findFirst()
                           .orElseThrow(() -> new IllegalArgumentException("Missing param setter for 'type'[" + dataType + "]"));
    }

    /**
     * Workaround for https://github.com/snowflakedb/snowflake-jdbc/issues/2186 Remove this method and
     * its usage once the issue is resolved.
     *
     * @param insertSql insert SQL
     * @param parametersElement
     * @param preparedStatement
     * @throws SQLException
     */
    public static void setManyIndexedParametersForInsert(String insertSql, JsonElement parametersElement,
            ParameterizedByIndex preparedStatement) throws SQLException {
        LOGGER.debug("Setting many indexed parameters for Snowflake prepared statement [{}]. Sql [{}]", preparedStatement, insertSql);

        Insert insertStatement = SqlParser.parseInsert(insertSql);
        ExpressionList<Column> insertColumns = insertStatement.getColumns();
        boolean userSpecifiedColumns = null != insertColumns;

        TableMetadata tableMetadata = getTableMetadata(preparedStatement.getConnection(), insertStatement);

        char snowflakeEscapeSymbol = SqlDialectFactory.getDialect(DatabaseSystem.SNOWFLAKE)
                                                      .getEscapeSymbol();
        List<ColumnMetadata> columnsMetadata = tableMetadata.getColumns();

        JsonArray parametersArrays = getParametersArrays(parametersElement);

        for (JsonElement paramsArrayElement : parametersArrays) {
            JsonArray paramsArray = toParamsArray(paramsArrayElement, preparedStatement);

            if (userSpecifiedColumns) {
                for (int paramsIdx = 0; paramsIdx < insertColumns.size(); paramsIdx++) {
                    Column inserColumn = insertColumns.get(paramsIdx);
                    String insertColumnName = inserColumn.getColumnName();
                    String unescapedInsertColumnName = insertColumnName.replaceAll(String.valueOf(snowflakeEscapeSymbol), "");
                    ColumnMetadata columnMetadata = findColumnMetadataByName(columnsMetadata, unescapedInsertColumnName);

                    int sqlParamIndex = paramsIdx + 1;
                    JsonElement parameter = paramsArray.get(paramsIdx);

                    String dirigibleSqlType = columnMetadata.getType();
                    int sqlType = DataTypeUtils.getSqlTypeByDataType(dirigibleSqlType);

                    setIndexedParameter(preparedStatement, sqlParamIndex, parameter, sqlType);
                }
            } else {
                if (columnsMetadata.size() != paramsArray.size()) {
                    throw new IllegalArgumentException(
                            "Mismatch parameters count [" + paramsArray.size() + "]. Table columns [" + columnsMetadata.size() + "]");
                }

                for (int idx = 0; idx < columnsMetadata.size(); idx++) {
                    ColumnMetadata columnMetadata = columnsMetadata.get(idx);

                    int sqlParamIndex = idx + 1;
                    JsonElement parameter = paramsArray.get(idx);

                    String dirigibleSqlType = columnMetadata.getType();
                    int sqlType = DataTypeUtils.getSqlTypeByDataType(dirigibleSqlType);

                    setIndexedParameter(preparedStatement, sqlParamIndex, parameter, sqlType);
                }
            }
            preparedStatement.addBatch();
        }
    }

    private static ColumnMetadata findColumnMetadataByName(List<ColumnMetadata> columnsMetadata, String columnName) {
        return columnsMetadata.stream()
                              .filter(cm -> Objects.equals(columnName, cm.getName()))
                              .findFirst()
                              .orElseThrow(() -> new ParametersSetterException(
                                      "Failed to find column metadata for column [" + columnName + "] in columns: " + columnsMetadata));
    }

    private static TableMetadata getTableMetadata(Connection connection, Insert insertStatement) {
        Table table = insertStatement.getTable();
        String schema = table.getSchemaName();
        String tableName = table.getName();

        try {
            TableMetadata tableMetadata = DatabaseMetadataHelper.describeTable(connection, null, schema, tableName);
            if (null != tableMetadata) {
                return tableMetadata;
            }
            String message = "Missing table metadata for table [" + tableName + "] in schema [" + schema
                    + "]. Details extracted from insert statement: " + insertStatement;
            throw new ParametersSetterException(message);

        } catch (SQLException ex) {
            String message = "Failed to get metadata for table [" + tableName + "] in schema [" + schema
                    + "]. Details extracted from insert statement: " + insertStatement;
            throw new ParametersSetterException(message, ex);
        }
    }

    private static JsonArray getParametersArrays(JsonElement parametersElement) {
        if (!parametersElement.isJsonArray()) {
            throw new IllegalArgumentException(
                    "Parameters must be provided as a JSON array of JSON arrays, e.g. [[1,\"John\",9876],[2,\"Mary\",1234]]. Parameters: "
                            + parametersElement);
        }
        return parametersElement.getAsJsonArray();
    }

    private static void setIndexedParameter(ParameterizedByIndex preparedStatement, int sqlParamIndex, JsonElement parameterElement,
            int sqlType) throws IllegalArgumentException, SQLException {

        if (Types.NULL == sqlType || parameterElement.isJsonNull()) {
            preparedStatement.setNull(sqlParamIndex, sqlType);
            return;
        }

        try {
            String dirigibleSqlType = DataTypeUtils.getDatabaseTypeName(sqlType);
            ParamSetter paramSetter = findParamSetterForType(dirigibleSqlType);
            LOGGER.debug(
                    "Found param setter [{}] for sql type [{}] which is converted to dirigible type [{}] for element [{}] at index [{}]",
                    paramSetter, sqlType, dirigibleSqlType, parameterElement, sqlParamIndex);

            if (parameterElement.isJsonPrimitive()) {
                paramSetter.setParam(parameterElement, sqlParamIndex, preparedStatement);
                return;
            }

            if (parameterElement.isJsonArray()) {
                paramSetter.setParam(parameterElement, sqlParamIndex, preparedStatement);
                return;
            }

            if (parameterElement.isJsonObject()) {
                setIndexedJsonObjectParam(preparedStatement, sqlParamIndex, parameterElement, sqlType, paramSetter);
                return;
            }
        } catch (IllegalArgumentException ex) {
            String errMsg = "Failed to set indexed param with index [" + sqlParamIndex + "] and sql type [" + sqlType + "] for element ["
                    + parameterElement + "] Statement: " + preparedStatement;
            throw new IllegalArgumentException(errMsg, ex);
        }

        String errMsg = "Parameter with index [" + sqlParamIndex + "] must be primitive or object. Parameter element [" + parameterElement
                + "] Statement: " + preparedStatement;
        throw new IllegalArgumentException(errMsg);
    }

    private static void setIndexedJsonObjectParam(ParameterizedByIndex preparedStatement, int sqlParamIndex, JsonElement parameterElement,
            int sqlType, ParamSetter paramSetter) throws SQLException {
        IndexedParamJsonObject paramJsonObject = IndexedParamJsonObject.fromJsonElement(parameterElement);

        JsonElement valueElement = paramJsonObject.getValueElement();
        if (null == valueElement || valueElement.isJsonNull()) {
            preparedStatement.setNull(sqlParamIndex, sqlType);
            return;
        }

        paramSetter.setParam(valueElement, sqlParamIndex, preparedStatement);
    }

    private static JsonArray toParamsArray(JsonElement paramsArrayJsonElement, ParameterizedByIndex preparedStatement) {
        if (!paramsArrayJsonElement.isJsonArray()) {
            throw new IllegalArgumentException("Parameters must be provided as a JSON array, e.g. [1, 'John', 9876]. Parameters ["
                    + paramsArrayJsonElement + "]. Statement: " + preparedStatement);
        }

        return paramsArrayJsonElement.getAsJsonArray();
    }

    public static void setManyIndexedParameters(JsonElement parametersElement, ParameterizedByIndex preparedStatement)
            throws IllegalArgumentException, SQLException {
        JsonArray parametersArrays = getParametersArrays(parametersElement);

        for (JsonElement parameters : parametersArrays) {
            setIndexedParameters(parameters, preparedStatement);
            preparedStatement.addBatch();
        }
    }

    public static void setIndexedParameters(JsonElement parameters, ParameterizedByIndex preparedStatement) throws SQLException {
        JsonArray paramsArray = toParamsArray(parameters, preparedStatement);

        int sqlParametersCount = preparedStatement.getParameterCount();

        int paramsCount = paramsArray.size();
        if (sqlParametersCount != paramsCount) {
            String errMsg = "Provided invalid parameters count of [" + paramsCount + "]. Expected parameters count [" + sqlParametersCount
                    + "]. Statement: " + preparedStatement;
            throw new IllegalArgumentException(errMsg);
        }

        for (int idx = 0; idx < paramsCount; idx++) {
            int sqlParamIndex = idx + 1;
            JsonElement parameter = paramsArray.get(idx);

            setIndexedParameter(preparedStatement, sqlParamIndex, parameter);
        }
    }

    private static void setIndexedParameter(ParameterizedByIndex preparedStatement, int sqlParamIndex, JsonElement parameterElement)
            throws IllegalArgumentException, SQLException {

        int sqlType;
        if (parameterElement.isJsonArray()) {
            sqlType = Types.ARRAY;
        } else if (parameterElement.isJsonObject()) {
            JsonElement typeElement = parameterElement.getAsJsonObject()
                                                      .get("type");
            if (typeElement != null) {
                String providedTypeName = typeElement.getAsString();
                sqlType = DataTypeUtils.getSqlTypeByDataType(providedTypeName);
            } else {
                sqlType = preparedStatement.getParameterType(sqlParamIndex);
            }
        } else {
            sqlType = preparedStatement.getParameterType(sqlParamIndex);
        }

        setIndexedParameter(preparedStatement, sqlParamIndex, parameterElement, sqlType);
    }

}
