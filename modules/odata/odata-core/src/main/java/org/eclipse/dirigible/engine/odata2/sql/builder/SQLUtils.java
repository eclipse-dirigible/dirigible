/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.odata2.sql.builder;

import static org.eclipse.dirigible.engine.odata2.sql.builder.EdmUtils.evaluateDateTimeExpressions;
import static org.eclipse.dirigible.engine.odata2.sql.utils.OData2Utils.isPropertyParameter;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.olingo.odata2.api.edm.EdmEntityType;
import org.apache.olingo.odata2.api.edm.EdmException;
import org.apache.olingo.odata2.api.edm.EdmProperty;
import org.apache.olingo.odata2.api.edm.EdmSimpleType;
import org.apache.olingo.odata2.api.edm.EdmSimpleTypeKind;
import org.apache.olingo.odata2.api.edm.EdmStructuralType;
import org.apache.olingo.odata2.api.exception.ODataApplicationException;
import org.apache.olingo.odata2.api.uri.KeyPredicate;
import org.apache.olingo.odata2.api.uri.expression.ExceptionVisitExpression;
import org.apache.olingo.odata2.api.uri.expression.FilterExpression;
import org.eclipse.dirigible.engine.odata2.sql.api.SQLStatementParam;
import org.eclipse.dirigible.engine.odata2.sql.binding.EdmTableBinding.ColumnInfo;
import org.eclipse.dirigible.engine.odata2.sql.clause.SQLWhereClause;
import org.eclipse.dirigible.engine.odata2.sql.clause.SQLWhereClauseVisitor;

/**
 * The Class SQLUtils.
 */
public final class SQLUtils {

    /**
     * Instantiates a new SQL utils.
     */
    private SQLUtils() {}

    /**
     * Sets the params on statement.
     *
     * @param preparedStatement the prepared statement
     * @param params the params
     * @throws SQLException the SQL exception
     */
    public static void setParamsOnStatement(final PreparedStatement preparedStatement, List<SQLStatementParam> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            SQLStatementParam param = params.get(i);
            EdmSimpleTypeKind edmSimpleTypeKind = param.getEdmSimpleKind();
            Object value = param.getValue();

            if (null == value) {
                preparedStatement.setObject(i + 1, null);
                continue;
            }

            switch (edmSimpleTypeKind) {
                case Time:
                    preparedStatement.setTime(i + 1, asTime(param.getValue()));
                    break;
                case DateTime:
                    preparedStatement.setTimestamp(i + 1, asTimeStamp(param.getValue()));
                    break;
                case DateTimeOffset:
                    preparedStatement.setDate(i + 1, asSQLDate(param.getValue()));
                    break;
                case Byte:
                case Int16:
                    preparedStatement.setShort(i + 1, Short.parseShort(value.toString()));
                    break;
                case Int32:
                    preparedStatement.setInt(i + 1, Integer.parseInt(value.toString()));
                    break;
                case Int64:
                    preparedStatement.setLong(i + 1, Long.parseLong(value.toString()));
                    break;
                case Double:
                    preparedStatement.setDouble(i + 1, Double.parseDouble(value.toString()));
                    break;
                case String:
                    preparedStatement.setString(i + 1, String.valueOf(value));
                    break;
                case Boolean:
                    preparedStatement.setBoolean(i + 1, Boolean.parseBoolean(value.toString()));
                    break;
                case Decimal:
                    preparedStatement.setBigDecimal(i + 1, (BigDecimal) value);
                    break;
                case Null:
                    preparedStatement.setObject(i + 1, null);
                    break;
                case Guid:
                    preparedStatement.setObject(i + 1, value);
                    break;
                case SByte:
                    preparedStatement.setByte(i + 1, Byte.parseByte(value.toString()));
                    break;
                case Single:
                    preparedStatement.setFloat(i + 1, Float.parseFloat(value.toString()));
                    break;
                default:
                    throw new IllegalStateException("Unexpected EdmType - " + edmSimpleTypeKind);
            }
        }
    }

    /**
     * As time stamp.
     *
     * @param calendar the calendar
     * @return the timestamp
     */
    public static Timestamp asTimeStamp(final Calendar calendar) {
        return new Timestamp(calendar.getTime()
                                     .getTime());
    }

    /**
     * As time.
     *
     * @param calendar the calendar
     * @return the time
     */
    public static Time asTime(final Calendar calendar) {
        return new Time(calendar.getTime()
                                .getTime());
    }

    /**
     * As SQL date.
     *
     * @param calendar the calendar
     * @return the date
     */
    public static Date asSQLDate(final Calendar calendar) {
        return new Date(calendar.getTime()
                                .getTime());
    }

    /**
     * Normalize SQL expression.
     *
     * @param expression the expression
     * @return the string
     */
    public static String normalizeSQLExpression(final StringBuilder expression) {
        return normalizeSQLExpression(expression.toString());
    }

    /**
     * Normalize SQL expression.
     *
     * @param expression the expression
     * @return the string
     */
    public static String normalizeSQLExpression(final String expression) {
        return expression.replaceAll("  ", " ")
                         .trim();
    }

    /**
     * Assert parameters count.
     *
     * @param sql the sql
     * @param params the params
     * @return the string
     */
    public static String assertParametersCount(String sql, List<SQLStatementParam> params) {
        long count = sql.chars()
                        .filter(ch -> ch == '?')
                        .count();
        if (count != params.size()) {
            throw new IllegalStateException("The count of the ? symbols in the generated SQL " + sql
                    + " does not match the existing params " + params + ". Make sure that the count is the same");
        }
        return sql;
    }

    /**
     * Builds the SQL where clause.
     *
     * @param query the query
     * @param targetEntityType the target entity type
     * @param expression the expression
     * @return the SQL where clause
     * @throws ExceptionVisitExpression the exception visit expression
     * @throws ODataApplicationException the o data application exception
     */
    // TODO handle also Grouping Operators () for example /Products?$filter=(Price sub 5) gt 10
    public static SQLWhereClause buildSQLWhereClause(SQLSelectBuilder query, EdmStructuralType targetEntityType,
            final FilterExpression expression) throws ExceptionVisitExpression, ODataApplicationException {
        // Better make use of a visitor pattern
        // TODO the filter might have join tables. Table alias must be removed. We must know in what context
        // we are.
        SQLWhereClauseVisitor visitor = new SQLWhereClauseVisitor(query, targetEntityType);
        return expression == null ? new SQLWhereClause() : (SQLWhereClause) expression.accept(visitor);
    }

    /**
     * Where clause from key predicates.
     *
     * @param query the query
     * @param type the type
     * @param keyPredicates the key predicates
     * @return the SQL where clause
     * @throws EdmException the edm exception
     */
    public static SQLWhereClause whereClauseFromKeyPredicates(SQLSelectBuilder query, EdmStructuralType type,
            final List<KeyPredicate> keyPredicates) throws EdmException {
        StringBuilder whereClause = new StringBuilder();
        List<SQLStatementParam> params = new ArrayList<>();

        if (keyPredicates != null) {
            Iterator<KeyPredicate> keyPredicateIterator = keyPredicates.iterator();
            while (keyPredicateIterator.hasNext()) {
                KeyPredicate keyPredicate = keyPredicateIterator.next();

                EdmProperty property = keyPredicate.getProperty();

                if (!isPropertyParameter(property, query, type)) {
                    Object literal = keyPredicate.getLiteral();

                    if (property.isSimple()) {
                        EdmSimpleType edmSimpleType = (EdmSimpleType) keyPredicate.getProperty()
                                                                                  .getType();
                        literal = evaluateDateTimeExpressions(literal, edmSimpleType);
                        ColumnInfo info = query.getSQLTableColumnInfo(type, property);
                        params.add(SQLWhereClause.param(literal, edmSimpleType, info));
                    } else {
                        // TODO what to do with complex properties?
                        throw new IllegalStateException();
                    }
                }
            }

            Iterator<SQLStatementParam> paramIterator = params.iterator();

            while (paramIterator.hasNext()) {
                SQLStatementParam param = paramIterator.next();
                whereClause.append(param.getSqlColumnName())
                           .append(" = ?");

                if (paramIterator.hasNext()) {
                    whereClause.append(" AND ");
                }
            }

            return new SQLWhereClause(whereClause.toString(), params.toArray(new SQLStatementParam[params.size()]));
        } else {
            return new SQLWhereClause();
        }
    }


    /**
     * Csv in brackets.
     *
     * @param columnValues the column values
     * @return the string
     */
    public static String csvInBrackets(List<String> columnValues) {
        return columnValues.stream()
                           .collect(Collectors.joining(",", "(", ")"));
    }

    /**
     * Csv.
     *
     * @param values the values
     * @return the string
     */
    public static String csv(List<String> values) {
        return join(values, ", ");
    }

    /**
     * Join.
     *
     * @param values the values
     * @param delimiter the delimiter
     * @return the string
     */
    public static String join(List<String> values, String delimiter) {
        return String.join(delimiter, values);
    }


    /**
     * Basic validity check for the values. Prevents that someone deletes an entity with an invalid
     * request In short, a composite key makes sense if all elements are not-null (otherwise the
     * non-null element suffices)
     *
     * @param keyValue the value of the key
     * @return if the key value is valid
     */
    public static boolean isValidKeyValue(Object keyValue) {
        return keyValue != null;
    }

    /**
     * Checks if is key property.
     *
     * @param target the target
     * @param prop the prop
     * @return true, if is key property
     * @throws EdmException the edm exception
     */
    public static boolean isKeyProperty(EdmEntityType target, EdmProperty prop) throws EdmException {
        for (String name : target.getKeyPropertyNames()) {
            if (name.equals(prop.getName())) {
                return true;
            }
        }
        return false;
    }

}
