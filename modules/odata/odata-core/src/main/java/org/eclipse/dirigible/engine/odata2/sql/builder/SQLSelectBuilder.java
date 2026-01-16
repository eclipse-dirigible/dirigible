/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.odata2.sql.builder;

import org.apache.olingo.odata2.api.commons.HttpStatusCodes;
import org.apache.olingo.odata2.api.edm.*;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.exception.ODataNotImplementedException;
import org.apache.olingo.odata2.api.uri.NavigationPropertySegment;
import org.apache.olingo.odata2.api.uri.NavigationSegment;
import org.apache.olingo.odata2.api.uri.SelectItem;
import org.apache.olingo.odata2.api.uri.UriInfo;
import org.apache.olingo.odata2.api.uri.expression.FilterExpression;
import org.apache.olingo.odata2.api.uri.expression.OrderByExpression;
import org.apache.olingo.odata2.api.uri.expression.OrderExpression;
import org.eclipse.dirigible.components.database.DatabaseSystem;
import org.eclipse.dirigible.engine.odata2.sql.api.OData2Exception;
import org.eclipse.dirigible.engine.odata2.sql.api.SQLStatement;
import org.eclipse.dirigible.engine.odata2.sql.api.SQLStatementParam;
import org.eclipse.dirigible.engine.odata2.sql.binding.EdmTableBinding.ColumnInfo;
import org.eclipse.dirigible.engine.odata2.sql.binding.EdmTableBindingProvider;
import org.eclipse.dirigible.engine.odata2.sql.clause.*;
import org.eclipse.dirigible.engine.odata2.sql.clause.SQLSelectClause.EvaluationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.eclipse.dirigible.engine.odata2.sql.clause.SQLSelectClause.EvaluationType.*;
import static org.eclipse.dirigible.engine.odata2.sql.utils.OData2Utils.fqn;
import static org.eclipse.dirigible.engine.odata2.sql.utils.OData2Utils.isOrderByEntityInExpand;

/**
 * The Class SQLSelectBuilder.
 */
public class SQLSelectBuilder extends AbstractQueryBuilder {

    /** The Constant LOG. */
    private static final Logger LOG = LoggerFactory.getLogger(SQLSelectBuilder.class);

    /** The Constant SPACE. */
    private static final String SPACE = " ";

    /** The structural types in join. */
    private final Set<String> structuralTypesInJoin;

    /** The join expressions. */
    private final List<SQLJoinClause> joinExpressions = new ArrayList<>();

    /** The select expression. */
    private SQLSelectClause selectExpression;

    /** The order by clause. */
    private SQLOrderByClause orderByClause;

    /** The group by clause. */
    private SQLGroupByClause groupByClause;

    /** The serverside paging. */
    private boolean serversidePaging;

    /**
     * Instantiates a new SQL select builder.
     *
     * @param tableMappingProvider the table mapping provider
     */
    public SQLSelectBuilder(final EdmTableBindingProvider tableMappingProvider) {
        super(tableMappingProvider);
        selectExpression = null;
        orderByClause = null;
        groupByClause = null;
        structuralTypesInJoin = new HashSet<>();
    }

    /**
     * Select.
     *
     * @param selects the selects
     * @param expand the expand
     * @return the SQL select clause
     */
    public SQLSelectClause select(final List<SelectItem> selects, final List<ArrayList<NavigationPropertySegment>> expand) {
        selectExpression = new SQLSelectClause(this, selects, expand);
        return selectExpression;
    }

    /**
     * Select.
     *
     * @return the SQL select clause. SQL select builder
     */
    public SQLSelectClause.SQLSelectBuilder select() {
        // this is the count
        selectExpression = new SQLSelectClause(this);
        return new SQLSelectClause.SQLSelectBuilder(selectExpression);
    }

    /**
     * And.
     *
     * @param whereClause the where clause
     * @return the SQL select builder
     */
    public SQLSelectBuilder and(final SQLWhereClause whereClause) {
        getWhereClause().and(whereClause);
        return this;
    }

    /**
     * Or.
     *
     * @param whereClause the where clause
     * @return the SQL select builder
     */
    public SQLSelectBuilder or(final SQLWhereClause whereClause) {
        getWhereClause().or(whereClause);
        return this;
    }

    /**
     * Order by.
     *
     * @param orderBy the order by
     * @param entityType the entity type
     * @return the SQL select builder
     * @throws ODataNotImplementedException the o data not implemented exception
     */
    public SQLSelectBuilder orderBy(final OrderByExpression orderBy, final EdmEntityType entityType) throws ODataNotImplementedException {
        if (orderBy != null && orderBy.getOrders() != null) {
            for (OrderExpression order : orderBy.getOrders()) {
                switch (order.getExpression()
                             .getKind()) {
                    case PROPERTY:
                    case MEMBER:
                        continue;
                    default:
                        LOG.error("OrderBy with non property or member expressions are not implemented yet!");
                        throw new ODataNotImplementedException();
                }
            }
        }
        this.orderByClause = new SQLOrderByClause(this, entityType, orderBy);
        return this;
    }

    /**
     * Group by.
     *
     * @param entityType the entity type
     * @return the SQL select builder
     */
    public SQLSelectBuilder groupBy(final EdmEntityType entityType) {
        if (hasAggregationTypePresent(entityType)) {
            this.groupByClause = new SQLGroupByClause(this, entityType);
        }
        return this;
    }

    /**
     * Validate order by.
     *
     * @param uriInfo the uri info
     * @throws EdmException the edm exception
     * @throws ODataNotImplementedException the o data not implemented exception
     */
    public void validateOrderBy(final UriInfo uriInfo) throws EdmException, ODataNotImplementedException {
        boolean usingOrderBy = uriInfo.getOrderBy() != null && uriInfo.getOrderBy()
                                                                      .getOrders() != null;
        if (usingOrderBy) {
            for (OrderExpression orderExpression : uriInfo.getOrderBy()
                                                          .getOrders()) {
                switch (orderExpression.getExpression()
                                       .getKind()) {
                    case PROPERTY:
                        continue;
                    case MEMBER:
                        // For a member order by to work we need a corresponding expand (which also means a join).
                        // Otherwise the table in the member order by clause will not be part of the query
                        // E.g. SELECT T0.ID AS "ID_T0" FROM CARS AS T0 ORDER BY T1.FIRSTNAME DESC (we need a join to define
                        // T1)
                        if (!isOrderByEntityInExpand(orderExpression, uriInfo)) {
                            throw new OData2Exception("Missing $expand of the entity in the OrderBy clause", HttpStatusCodes.BAD_REQUEST); // no
                            // expand,
                            // but
                            // member
                            // order
                            // by
                            // is
                            // not
                            // allowed
                        }
                        continue;
                    default:
                        LOG.error("OrderBy is implmented only for properties or member properties!");
                        throw new ODataNotImplementedException();
                }
            }
        }
    }

    /**
     * Filter.
     *
     * @param filterTarget the filter target
     * @param filter the filter
     * @return the SQL select builder
     * @throws ODataException the o data exception
     */
    public SQLSelectBuilder filter(final EdmEntitySet filterTarget, final FilterExpression filter) throws ODataException {
        // TODO we do not search only filter target table. What if we filter on property that is complex
        // type and is field of the target entity?
        SQLWhereClause where = SQLUtils.buildSQLWhereClause(this, filterTarget.getEntityType(), filter);
        if (!where.isEmpty()) {
            getWhereClause().and(where);
        }
        return this;
    }

    /**
     * Filter.
     *
     * @param filterTarget the filter target
     * @param keyProperty the key property
     * @param idsOfLeadingEntities the ids of leading entities
     * @return the SQL select builder
     * @throws ODataException the o data exception
     */
    public SQLSelectBuilder filter(final EdmEntitySet filterTarget, final EdmProperty keyProperty, final List<String> idsOfLeadingEntities)
            throws ODataException {
        ColumnInfo column = getSQLTableColumnInfo(filterTarget.getEntityType(), keyProperty);
        List<SQLStatementParam> filterParams = new ArrayList<>();
        StringBuilder filterClause = new StringBuilder(column.getColumnName() + " IN (");
        for (String leadingEntityId : idsOfLeadingEntities) {
            filterParams.add(new SQLStatementParam(leadingEntityId, keyProperty, column));
            filterClause.append("?,");
        }
        // delete the last ,
        filterClause.deleteCharAt(filterClause.length() - 1);
        filterClause.append(")");
        SQLWhereClause where = new SQLWhereClause(filterClause.toString(), filterParams);
        if (!where.isEmpty()) {
            getWhereClause().and(where);
        }
        return this;
    }

    /**
     * Join.
     *
     * @param start the start
     * @param target the target
     * @param expands the expands
     * @return the SQL join clause
     * @throws ODataException the o data exception
     */
    public SQLJoinClause join(final EdmEntitySet start, final EdmEntitySet target, final List<NavigationSegment> expands)
            throws ODataException {
        return this.join(start.getEntityType(), target.getEntityType());
    }

    /**
     * Join.
     *
     * @param from the from
     * @param to the to
     * @return the SQL join clause
     * @throws EdmException the edm exception
     */
    public SQLJoinClause join(final EdmStructuralType from, final EdmStructuralType to) throws EdmException {
        SQLJoinClause join = new SQLJoinClause(this, from, to);
        if (!join.isEmpty()) {
            // adds the join if it does not exist
            if (!joinExpressions.contains(join)) {
                joinExpressions.add(join);
                structuralTypesInJoin.add(fqn(from));
            }
        }
        return join;
    }

    /**
     * Gets the join where clauses.
     *
     * @return the join where clauses
     */
    // Do NOT use this method; For Unit testing purposes ONLY
    Iterator<SQLJoinClause> getJoinWhereClauses() {
        return joinExpressions.iterator();
    }

    /**
     * Check if server side paging is enabled.
     *
     * @return true if it is enabled
     */
    public boolean isServersidePaging() {
        return serversidePaging;
    }

    /**
     * Sets the serverside paging.
     *
     * @param serversidePaging the serverside paging
     * @return the SQL select builder
     */
    public SQLSelectBuilder setServersidePaging(final boolean serversidePaging) {
        this.serversidePaging = serversidePaging;
        return this;
    }

    /**
     * Builds the.
     *
     * @param context the context
     * @return the SQL statement
     * @throws ODataException the o data exception
     */
    @Override
    public SQLStatement build(SQLContext context) throws ODataException {
        return new SQLStatement() {
            @Override
            public String sql() throws ODataException {
                return buildSelect(context);
            }

            @Override
            public List<SQLStatementParam> getStatementParams() {
                return SQLSelectBuilder.this.getStatementParams();
            }

            @Override
            public boolean isEmpty() {
                return false;
            }
        };
    }

    /**
     * Gets the statement params.
     *
     * @return the statement params
     */
    @Override
    public List<SQLStatementParam> getStatementParams() {
        List<SQLStatementParam> selectClauseStatementParams = getSelectExpression().getStatementParams();
        List<SQLStatementParam> whereClauseStatementParams = getWhereClause().getStatementParams();

        return Stream.concat(selectClauseStatementParams.stream(), whereClauseStatementParams.stream())
                     .collect(Collectors.toList());
    }

    /**
     * Gets the select expression.
     *
     * @return the select expression
     */
    public SQLSelectClause getSelectExpression() {
        return selectExpression;
    }

    /**
     * Build select clause.
     *
     * @param context the context
     * @return the clause
     * @throws ODataException in case of an error
     */
    public String buildSelect(final SQLContext context) throws ODataException {
        // TODO make immutable

        StringBuilder builder = new StringBuilder();
        if (selectExpression == null)
            throw new IllegalStateException("Please initialize the select clause!");
        builder.append("SELECT ");

        String limit = selectExpression.evaluate(context, SELECT_LIMIT);
        String offset = selectExpression.evaluate(context, SELECT_OFFSET);

        if (context.getDatabaseSystem()
                   .isOfType(DatabaseSystem.MSSQL)) {
            if (!limit.isBlank() && offset.isBlank()) {
                builder.append(" TOP " + limit + " ");
            }
        }

        builder.append(selectExpression.evaluate(context, SELECT_COLUMN_LIST));
        builder.append(" FROM ");
        builder.append(selectExpression.evaluate(context, FROM))
               .append(SPACE);
        builder.append(evaluateJoins(context));
        if (!getWhereClause().isEmpty()) {
            builder.append(" WHERE ");
            builder.append(getWhereClause().evaluate(context))
                   .append(SPACE);
        }

        SQLGroupByClause gb = getGroupByClause();
        if (gb != null) {
            String groupByExpression = gb.evaluate(context);
            if (!groupByExpression.isEmpty()) {
                builder.append("GROUP BY ")
                       .append(groupByExpression)
                       .append(SPACE);
            }
        }

        SQLOrderByClause ob = getOrderByClause();
        if (null != ob) {
            String orderByExpression = ob.evaluate(context);
            if (!orderByExpression.isEmpty()) {
                builder.append("ORDER BY ")
                       .append(orderByExpression)
                       .append(SPACE);
            }
        }

        if (context.getDatabaseSystem()
                   .isOfType(DatabaseSystem.MSSQL)) {
            if (!limit.isBlank() && !offset.isBlank()) {
                builder.append(" OFFSET " + offset + " ROWS FETCH NEXT " + limit + " ROWS ONLY ");
            }
        } else {
            addSelectOption(context, builder, SELECT_LIMIT);
            addSelectOption(context, builder, SELECT_OFFSET);
        }

        return SQLUtils.normalizeSQLExpression(builder);
    }

    /**
     * Gets the order by clause.
     *
     * @return the order by clause
     */
    public SQLOrderByClause getOrderByClause() {
        return orderByClause;
    }

    /**
     * Gets the group by clause.
     *
     * @return the group by clause
     */
    public SQLGroupByClause getGroupByClause() {
        return groupByClause;
    }

    /**
     * Evaluate joins.
     *
     * @param context the context
     * @return the string
     * @throws ODataException the o data exception
     */
    private String evaluateJoins(final SQLContext context) throws ODataException {
        StringBuilder builder = new StringBuilder();
        for (SQLJoinClause join : joinExpressions) {
            if (join.isEmpty()) {
                continue;
            }
            builder.append(join.evaluate(context))
                   .append(SPACE);
        }
        return builder.toString();
    }

    /**
     * Adds the select option.
     *
     * @param context the context
     * @param statementBuilder the statement builder
     * @param option the option
     * @throws EdmException the edm exception
     */
    private void addSelectOption(SQLContext context, StringBuilder statementBuilder, EvaluationType option) throws EdmException {
        String optionSupplement = selectExpression.evaluate(context, option);
        if (!optionSupplement.isEmpty()) {
            statementBuilder.append(optionSupplement)
                            .append(SPACE);
        }
    }

}
