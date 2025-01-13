/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.odata2.sql.clause;

import org.apache.olingo.odata2.api.edm.EdmSimpleType;
import org.eclipse.dirigible.engine.odata2.sql.api.SQLClause;
import org.eclipse.dirigible.engine.odata2.sql.api.SQLStatementParam;
import org.eclipse.dirigible.engine.odata2.sql.binding.EdmTableBinding;
import org.eclipse.dirigible.engine.odata2.sql.builder.SQLContext;

import java.util.*;

/**
 * The Class SQLWhereClause.
 */
public final class SQLWhereClause implements SQLClause {

    /** The Constant AND. */
    private static final String AND = " AND ";

    /** The Constant OR. */
    private static final String OR = " OR ";

    /** The Constant OPEN_BRACKET. */
    private static final char OPEN_BRACKET = '(';

    /** The Constant CLOSE_BRACKET. */
    private static final char CLOSE_BRACKET = ')';

    /** The statement params. */
    private final List<SQLStatementParam> statementParams;

    /** The where clause. */
    private final StringBuilder whereClause;


    /**
     * Instantiates a new SQL where clause.
     */
    public SQLWhereClause() {
        this("");
    }

    /**
     * Instantiates a new SQL where clause.
     *
     * @param sql the sql
     * @param statementParams the statement params
     */
    public SQLWhereClause(String sql, List<SQLStatementParam> statementParams) {
        this.whereClause = new StringBuilder(sql);
        this.statementParams = new ArrayList<>();
        if (statementParams != null) {
            this.statementParams.addAll(statementParams);
        }
    }

    /**
     * Instantiates a new SQL where clause.
     *
     * @param whereClause the where clause
     * @param statementParams the statement params
     */
    public SQLWhereClause(String whereClause, SQLStatementParam... statementParams) {
        this(whereClause, statementParams == null ? new ArrayList<>() : Arrays.asList(statementParams));
    }

    /**
     * Evaluate.
     *
     * @param context the context
     * @return the string
     */
    public String evaluate(SQLContext context) {
        return isEmpty() ? "" : getWhereClause();
    }

    /**
     * Checks if is empty.
     *
     * @return true, if is empty
     */
    public boolean isEmpty() {
        return whereClause == null || whereClause.length() == 0;
    }

    /**
     * And.
     *
     * @param where the where
     * @return the SQL where clause
     */
    public SQLWhereClause and(SQLWhereClause where) {
        return append(AND, where);
    }

    /**
     * Or.
     *
     * @param where the where
     * @return the SQL where clause
     */
    public SQLWhereClause or(SQLWhereClause where) {
        return append(OR, where);
    }

    /**
     * Gets the where clause.
     *
     * @return the where clause
     */
    public String getWhereClause() {
        return this.whereClause.toString();
    }

    /**
     * Gets the statement params.
     *
     * @return the statement params
     */
    public List<SQLStatementParam> getStatementParams() {
        return Collections.unmodifiableList(statementParams);
    }

    /**
     * Gets the param at.
     *
     * @param index the index
     * @return the param at
     */
    // For internal use (Unit Test purposes) only!
    SQLStatementParam getParamAt(int index) {
        return statementParams.get(index);
    }

    /**
     * Append.
     *
     * @param appendPredicate the append predicate
     * @param whereClauses the where clauses
     * @return the SQL where clause
     */
    private SQLWhereClause append(final String appendPredicate, final SQLWhereClause... whereClauses) {
        boolean useBraces = whereClauses.length > 1;
        Iterator<SQLWhereClause> it = Arrays.asList(whereClauses)
                                            .iterator();
        if (useBraces) {
            this.whereClause.append(appendPredicate);
            this.whereClause.append(OPEN_BRACKET);
        }
        while (it.hasNext()) {
            SQLWhereClause where = it.next();
            if (!where.isEmpty() && !endsWithOpenBracket() && !isEmpty()) {
                this.whereClause.append(AND);
            }
            if (!where.isEmpty()) {
                this.whereClause.append(where.whereClause);
                this.statementParams.addAll(where.statementParams);
            }
        }
        if (useBraces) {
            this.whereClause.append(CLOSE_BRACKET);
        }
        return this;
    }

    /**
     * Ends with open bracket.
     *
     * @return true, if successful
     */
    private boolean endsWithOpenBracket() {
        if (isEmpty()) {
            return false;
        } else {
            return OPEN_BRACKET == whereClause.charAt(whereClause.length() - 1);
        }
    }

    /**
     * Param.
     *
     * @param value the value
     * @param edmType the edm type
     * @param columnInfo the column info
     * @return the SQL statement param
     */
    public static SQLStatementParam param(Object value, final EdmSimpleType edmType, final EdmTableBinding.ColumnInfo columnInfo) {
        return new SQLStatementParam(value, edmType, columnInfo);
    }

}
