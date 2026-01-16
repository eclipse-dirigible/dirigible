/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.odata2.sql.clause;

import org.apache.olingo.odata2.api.commons.HttpStatusCodes;
import org.apache.olingo.odata2.api.edm.*;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.uri.KeyPredicate;
import org.apache.olingo.odata2.api.uri.NavigationPropertySegment;
import org.apache.olingo.odata2.api.uri.SelectItem;
import org.eclipse.dirigible.components.database.DatabaseSystem;
import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.dialects.SqlDialectFactory;
import org.eclipse.dirigible.engine.odata2.sql.api.OData2Exception;
import org.eclipse.dirigible.engine.odata2.sql.api.SQLStatementParam;
import org.eclipse.dirigible.engine.odata2.sql.binding.EdmTableBinding;
import org.eclipse.dirigible.engine.odata2.sql.builder.EdmUtils;
import org.eclipse.dirigible.engine.odata2.sql.builder.SQLContext;
import org.eclipse.dirigible.engine.odata2.sql.builder.SQLUtils;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.EMPTY_LIST;
import static org.eclipse.dirigible.engine.odata2.sql.builder.EdmUtils.evaluateDateTimeExpressions;
import static org.eclipse.dirigible.engine.odata2.sql.utils.OData2Utils.*;

/**
 * The Class SQLSelectClause.
 */
public final class SQLSelectClause {

    /** The Constant NOT_SET. */
    public static final int NOT_SET = -1;
    /** The Constant EMPTY_STRING. */
    private static final String EMPTY_STRING = "";
    /** The column mapping. */
    // Later we would like to bind the columns in the entity set to the correct
    // entities/fields
    private final Map<Integer, EdmTarget> columnMapping;
    /** The parameters. */
    private final Collection<EdmTarget> parameters;
    /** The query. */
    private final org.eclipse.dirigible.engine.odata2.sql.builder.SQLSelectBuilder query;
    /** The selects from target entity. */
    private final List<SelectItem> selectsFromTargetEntity;
    /** The expands. */
    private final List<ArrayList<NavigationPropertySegment>> expands;
    /** The statement params. */
    private final List<SQLStatementParam> statementParams;
    /** The at column. */
    private int atColumn = 0;
    /** The is count. */
    private boolean isCount;
    /** The top. */
    private int top;

    /** The skip. */
    private int skip;

    /** The target. */
    private EdmStructuralType target;
    /** The key predicates. */
    private List<KeyPredicate> keyPredicates;

    /**
     * Instantiates a new SQL select clause.
     *
     * @param parent the parent
     */
    @SuppressWarnings("unchecked")
    public SQLSelectClause(final org.eclipse.dirigible.engine.odata2.sql.builder.SQLSelectBuilder parent) {
        this(parent, EMPTY_LIST, EMPTY_LIST);
    }

    /**
     * Instantiates a new SQL select clause.
     *
     * @param query the query
     * @param selects the selects
     * @param expands the expands
     */
    public SQLSelectClause(final org.eclipse.dirigible.engine.odata2.sql.builder.SQLSelectBuilder query, final List<SelectItem> selects,
            final List<ArrayList<NavigationPropertySegment>> expands) {
        columnMapping = new TreeMap<>();
        parameters = new HashSet<>();
        statementParams = new ArrayList<>();
        this.query = query;
        selectsFromTargetEntity = selects;
        this.expands = expands;
        isCount = false;
        top = NOT_SET;
        skip = NOT_SET;
    }

    /**
     * The Enum EvaluationType.
     */
    public enum EvaluationType {

        /** The select column list. */
        SELECT_COLUMN_LIST,
        /** The from. */
        FROM,
        /** The join. */
        JOIN,
        /** The where. */
        WHERE,
        /** The select limit. */
        SELECT_LIMIT,
        /** The into. */
        INTO,
        /** The values. */
        VALUES,
        /** The keys. */
        KEYS,
        /** The select offset. */
        SELECT_OFFSET,
        /** The table. */
        TABLE
    }


    /**
     * The Class SQLSelectBuilder.
     */
    public static class SQLSelectBuilder {

        /** The select expression. */
        private final SQLSelectClause selectExpression;

        /**
         * Instantiates a new SQL select builder.
         *
         * @param select the select
         */
        public SQLSelectBuilder(final SQLSelectClause select) {
            selectExpression = select;
        }

        /**
         * Top.
         *
         * @param top the top
         * @return the SQL select clause
         */
        public SQLSelectClause top(final int top) {
            selectExpression.top(top);
            return selectExpression;
        }

        /**
         * Skip.
         *
         * @param skip the skip
         * @return the SQL select clause
         */
        public SQLSelectClause skip(final int skip) {
            selectExpression.skip(skip);
            return selectExpression;
        }

        /**
         * Count.
         *
         * @return the SQL select clause
         */
        public SQLSelectClause count() {
            selectExpression.setUsingCount(true);
            return selectExpression;
        }
    }


    /**
     * The Class EdmTarget.
     */
    private class EdmTarget {

        /** The edm navigation property. */
        private final EdmNavigationProperty edmNavigationProperty;
        /** The edm target type. */
        private EdmStructuralType edmTargetType;
        /** The edm property. */
        private EdmProperty edmProperty;

        /**
         * Instantiates a new edm target.
         *
         * @param edmTargetType the edm target type
         * @param edmProperty the edm property
         */
        public EdmTarget(final EdmStructuralType edmTargetType, final EdmProperty edmProperty) {
            this.edmTargetType = edmTargetType;
            this.edmProperty = edmProperty;
            this.edmNavigationProperty = null;
        }

        /**
         * Instantiates a new edm target.
         *
         * @param edmTargetType the edm target type
         * @param edmNavigationProperty the edm navigation property
         * @param edmProperty the edm property
         */
        public EdmTarget(final EdmStructuralType edmTargetType, EdmNavigationProperty edmNavigationProperty,
                final EdmProperty edmProperty) {
            this.edmTargetType = edmTargetType;
            this.edmProperty = edmProperty;
            this.edmNavigationProperty = edmNavigationProperty;
        }

        /**
         * Gets the edm navigation property.
         *
         * @return the edm navigation property
         */
        public EdmNavigationProperty getEdmNavigationProperty() {
            return edmNavigationProperty;
        }

        /**
         * Gets the edm target type.
         *
         * @return the edm target type
         */
        public EdmStructuralType getEdmTargetType() {
            return edmTargetType;
        }

        /**
         * Sets the edm target type.
         *
         * @param edmTargetType the new edm target type
         */
        public void setEdmTargetType(final EdmStructuralType edmTargetType) {
            this.edmTargetType = edmTargetType;
        }

        /**
         * Gets the edm property.
         *
         * @return the edm property
         */
        public EdmProperty getEdmProperty() {
            return edmProperty;
        }

        /**
         * Sets the edm property.
         *
         * @param edmProperty the new edm property
         */
        public void setEdmProperty(final EdmProperty edmProperty) {
            this.edmProperty = edmProperty;
        }

        /**
         * To string.
         *
         * @return the string
         */
        @Override
        public String toString() {
            try {
                if (isInlineTarget()) {
                    return "EdmTarget [" + edmTargetType.getName() + "." + edmNavigationProperty.getName() + edmProperty.getName() + "]";
                } else {
                    return "EdmTarget [" + edmTargetType.getName() + "." + edmProperty.getName() + "]";
                }
            } catch (EdmException e) {
                throw new OData2Exception(HttpStatusCodes.INTERNAL_SERVER_ERROR);
            }
        }

        /**
         * Checks if is inline target.
         *
         * @return true, if is inline target
         */
        public boolean isInlineTarget() {
            return edmNavigationProperty != null;
        }
    }

    /**
     * Evaluate.
     *
     * @param context the context
     * @param type the type
     * @return the string
     * @throws EdmException the edm exception
     */
    public String evaluate(final SQLContext context, final EvaluationType type) throws EdmException {
        switch (type) {
            case SELECT_COLUMN_LIST:
                return buildColumnList();
            case FROM:
                return buildFrom(context);
            case SELECT_LIMIT:
                return buildLimit(context);
            case SELECT_OFFSET:
                return buildOffset(context);
            default:
                throw new OData2Exception("Unable to evaluate the SQLSelect to type " + type, HttpStatusCodes.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Builds the offset.
     *
     * @return the string
     */
    private String buildOffset(SQLContext context) {
        if (skip <= 0) {
            return EMPTY_STRING;
        }

        if (context.getDatabaseSystem()
                   .isMSSQL()) {
            return Integer.toString(skip);
        }

        return "OFFSET " + skip;
    }

    /**
     * Checks if is empty.
     *
     * @return true, if is empty
     */
    public boolean isEmpty() {
        if (isCount)
            return false;
        else
            return columnMapping.size() == 0;
    }

    /**
     * Top.
     *
     * @param top the top
     * @return the SQL select clause
     */
    public SQLSelectClause top(final Integer top) {
        if (top != null) {
            this.top = top;
        }
        return this;
    }

    /**
     * Skip.
     *
     * @param skip the skip
     * @return the SQL select clause
     */
    public SQLSelectClause skip(final Integer skip) {
        if (skip != null && skip > 0) {
            this.skip = skip;
        } else if (skip != null && skip == 0) {
            // The odata impl of SAP UI5 uses skip=0 by default
            // In this case we do not have any skip
            this.skip = NOT_SET;
        }
        return this;
    }

    /**
     * Sets the using count.
     *
     * @param count the new using count
     */
    void setUsingCount(final boolean count) {
        isCount = count;
    }

    /**
     * Gets the top.
     *
     * @return the top
     */
    public int getTop() {
        return top;
    }

    /**
     * Gets the target.
     *
     * @return the target
     */
    public EdmStructuralType getTarget() {
        return target;
    }

    /**
     * Gets the skip.
     *
     * @return the skip
     */
    public int getSkip() {
        return skip;
    }

    /**
     * Gets the column mapping.
     *
     * @return the column mapping
     */
    public Map<Integer, EdmTarget> getColumnMapping() {
        return columnMapping;
    }

    /**
     * From.
     *
     * @param target the target
     * @param keyPredicates the key predicates
     * @return the org.eclipse.dirigible.engine.odata 2 .sql.builder. SQL select builder
     * @throws ODataException the o data exception
     */
    @SuppressWarnings("unchecked")
    public org.eclipse.dirigible.engine.odata2.sql.builder.SQLSelectBuilder from(final EdmStructuralType target,
            List<KeyPredicate> keyPredicates) throws ODataException {
        query.grantTableAliasForStructuralTypeInQuery(target);

        this.target = target;
        this.keyPredicates = keyPredicates;

        Collection<EdmProperty> properties = EdmUtils.getSelectedProperties(selectsFromTargetEntity, target);
        List<String> sqlTableParameters = this.query.getSQLTableParameters(this.target);

        if (isCount) {
            for (EdmProperty property : properties) {
                if (sqlTableParameters.contains(property.getName())) {
                    parameters.add(new EdmTarget(target, property));
                }
            }
            columnMapping.put(atColumn++, new EdmTarget(target, null));
        } else {
            for (EdmProperty property : properties) {
                if (sqlTableParameters.contains(property.getName())) {
                    parameters.add(new EdmTarget(target, property));
                } else {
                    columnMapping.put(atColumn++, new EdmTarget(target, property));
                }
            }

            if (!parameters.isEmpty() && keyPredicates.isEmpty()) {
                throw new OData2Exception("Collection " + target.getName() + " is not directly accessible.", HttpStatusCodes.BAD_REQUEST);
            }

            if (hasExpand(expands)) {
                // Handle expands here
                for (ArrayList<NavigationPropertySegment> contentExpands : expands) {
                    EdmStructuralType toEntityType = target;
                    for (NavigationPropertySegment contentExpand : contentExpands) {
                        EdmEntityType expandEntityType = contentExpand.getTargetEntitySet()
                                                                      .getEntityType();
                        Collection<EdmProperty> expandProperties = EdmUtils.getSelectedProperties(EMPTY_LIST, expandEntityType);
                        for (EdmProperty expandProperty : expandProperties) {
                            columnMapping.put(atColumn++, new EdmTarget(expandEntityType, expandProperty));
                            query.join(expandEntityType, toEntityType);
                        }
                        toEntityType = expandEntityType;
                    }
                }
            }
        }
        return query;
    }

    /**
     * Gets the property name from column mapping.
     *
     * @param columnIndex the column index
     * @return the property name from column mapping
     * @throws EdmException the edm exception
     */
    private String getPropertyNameFromColumnMapping(final int columnIndex) throws EdmException {
        return getPropertyFromColumnMapping(columnIndex).getName();
    }

    /**
     * Gets the property from column mapping.
     *
     * @param columnIndex the column index
     * @return the property from column mapping
     */
    private EdmProperty getPropertyFromColumnMapping(final int columnIndex) {
        return columnMapping.get(columnIndex)
                            .getEdmProperty();
    }

    /**
     * Gets the target type from column mapping.
     *
     * @param columnIndex the column index
     * @return the target type from column mapping
     */
    private EdmStructuralType getTargetTypeFromColumnMapping(final int columnIndex) {
        return columnMapping.get(columnIndex)
                            .getEdmTargetType();
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
     * Builds the limit.
     *
     * @param context the context
     * @return the string
     */
    private String buildLimit(final SQLContext context) {
        if (isCount)
            return EMPTY_STRING;
        String selectPredicate = EMPTY_STRING;
        if (top > 0) {
            if (context.getDatabaseSystem()
                       .isDerby()) {
                // Derby: FETCH { FIRST | NEXT } [integer-literal] {ROW | ROWS} ONLY
                return String.format("FETCH FIRST %d ROWS ONLY", top);
            }
            if (context.getDatabaseSystem()
                       .isMSSQL()) {
                return Integer.toString(top);
            }
            // PostgreSQL: [LIMIT { number | ALL }] [OFFSET number]
            return String.format("LIMIT %d", top);
        }

        return selectPredicate;
    }

    /**
     * Builds the from.
     *
     * @param context the context
     * @return the string
     * @throws EdmException the edm exception
     */
    private String buildFrom(final SQLContext context) throws EdmException {
        EdmTableBinding.DataStructureType targetDataStructureType = this.query.getSQLTableDataStructureType(this.target);
        List<String> tables = new ArrayList<>();
        Iterator<String> it = query.getTablesAliasesForEntitiesInQuery();
        while (it.hasNext()) {
            String tableAlias = it.next();
            char escapeSymbol = getEscapeSymbol(context);

            String escapedTableAlias = (escapeSymbol + tableAlias + escapeSymbol);
            EdmStructuralType type = query.getEntityInQueryForAlias(tableAlias);
            if (isSelectTarget(type)) {
                boolean isView = EdmTableBinding.DataStructureType.VIEW == targetDataStructureType;
                boolean isCalculationView = EdmTableBinding.DataStructureType.CALC_VIEW == targetDataStructureType;
                boolean isHanaDatabase = context.getDatabaseSystem()
                                                .isHANA();
                if ((isView || (isCalculationView && isHanaDatabase)) && !this.parameters.isEmpty()) {
                    addInputParamsAsStatementParams(parameters);
                    tables.add(query.getSQLTableName(target, context) + buildTargetParameters() + " AS " + escapedTableAlias);
                } else {
                    tables.add(query.getSQLTableName(target, context) + " AS " + escapedTableAlias);
                }
            }
        }

        return SQLUtils.csv(tables);
    }

    private static char getEscapeSymbol(SQLContext context) {
        DatabaseSystem databaseSystem = context.getDatabaseSystem();
        ISqlDialect dialect = SqlDialectFactory.getDialect(databaseSystem);
        return dialect.getEscapeSymbol();
    }

    /**
     * Builds the target parameters.
     *
     * @return the string
     */
    private String buildTargetParameters() {
        return parameters.stream()
                         .map(this::createInputParameterPlaceholder)
                         .collect(Collectors.joining(", ", "(", ")"));
    }

    /**
     * Creates the input parameter placeholder.
     *
     * @param parameter the parameter
     * @return the string
     */
    private String createInputParameterPlaceholder(EdmTarget parameter) {
        try {
            String propertyName = parameter.getEdmProperty()
                                           .getName();
            return "placeholder.\"$$" + propertyName + "$$\"" + " => " + " ?";
        } catch (EdmException e) {
            throw new IllegalArgumentException("Failed to create input parameter placeholder", e);
        }
    }

    /**
     * Creates the sql statement param.
     *
     * @param parameter the parameter
     * @return the SQL statement param
     * @throws EdmException the edm exception
     */
    private SQLStatementParam createSqlStatementParam(EdmTarget parameter) throws EdmException {
        EdmStructuralType type = parameter.getEdmTargetType();
        EdmProperty prop = parameter.getEdmProperty();
        EdmSimpleType edmSimpleType = (EdmSimpleType) prop.getType();
        Object parameterValue = getKeyPredicateValueByPropertyName(prop.getName(), keyPredicates);
        parameterValue = evaluateDateTimeExpressions(parameterValue, edmSimpleType);
        EdmTableBinding.ColumnInfo info = query.getSQLTableColumnInfo(type, prop);
        return new SQLStatementParam(parameterValue, prop, info);
    }

    /**
     * Checks if is select target.
     *
     * @param target the target
     * @return true, if is select target
     */
    private boolean isSelectTarget(final EdmStructuralType target) {
        // Always select the entity target
        return fqn(query.getSelectExpression()
                        .getTarget()).equals(fqn(target));
    }

    /**
     * Builds the column list.
     *
     * @return the string
     * @throws EdmException the edm exception
     */
    private String buildColumnList() throws EdmException {
        if (isCount)
            return "COUNT(*)";
        else {

            StringBuilder select = new StringBuilder();

            Iterator<Integer> columnIterator = columnMapping.keySet()
                                                            .iterator();

            if (query.hasKeyGeneratedPresent(target) && columnIterator.hasNext()) {
                select.append("row_number() over() \"row_num\"");
                select.append(", ");
            }

            while (columnIterator.hasNext()) {
                Integer column = columnIterator.next();
                EdmStructuralType type = getTargetTypeFromColumnMapping(column);
                String propertyName = getPropertyNameFromColumnMapping(column);
                EdmTyped p = type.getProperty(propertyName);
                if (!(p instanceof EdmProperty prop))
                    throw new OData2Exception("You must map the column " + column + " to a EDM property! The current type of property "
                            + propertyName + " is " + p, HttpStatusCodes.INTERNAL_SERVER_ERROR);
                if (p.getType()
                     .getKind() == EdmTypeKind.SIMPLE) {
                    if (query.hasAggregationTypePresent(target) && !query.isAggregationTypeExplicit(target)
                            && query.isColumnContainedInAggregationProp(target, query.getPureSQLColumnName(target, prop))) {

                        select.append(query.getColumnAggregationType(target, query.getPureSQLColumnName(target, prop)))
                              .append("(")
                              .append(tableColumnForSelectWithoutAlias(type, prop))
                              .append(") AS \"")
                              .append(query.getSQLTableColumnAlias(type, prop))
                              .append("\"");
                    } else {
                        select.append(tableColumnForSelect(type, prop));
                    }
                } else {
                    if (p.getType()
                         .getKind() == EdmTypeKind.COMPLEX) {
                        EdmStructuralType st = (EdmStructuralType) p.getType();

                        query.join(st, type);
                        Iterator<String> prit = st.getPropertyNames()
                                                  .iterator();
                        while (prit.hasNext()) {
                            EdmProperty stProp = (EdmProperty) st.getProperty(prit.next());
                            select.append(tableColumnForSelect(st, stProp));
                            if (prit.hasNext()) {
                                select.append(", ");
                            }
                        }

                    } else
                        throw new IllegalStateException("Unable to handle property " + p);
                }
                if (columnIterator.hasNext()) {
                    select.append(", ");
                }
            }

            addInputParamsAsStatementParams(parameters);

            String inputParamsSelectColumns = parameters.stream()
                                                        .map(parameter -> "?" + " AS "
                                                                + query.getSQLTableColumnAlias(parameter.getEdmTargetType(),
                                                                        parameter.getEdmProperty()))
                                                        .collect(Collectors.joining(", "));

            if (!inputParamsSelectColumns.isEmpty() && !select.toString()
                                                              .isEmpty()) {
                select.append(", ");
                select.append(inputParamsSelectColumns);
            }

            return select.toString();
        }
    }

    /**
     * Adds the input params as statement params.
     *
     * @param parameters the parameters
     */
    private void addInputParamsAsStatementParams(Collection<EdmTarget> parameters) {
        parameters.stream()
                  .forEach(parameter -> {
                      try {
                          SQLStatementParam sqlStatementParam = createSqlStatementParam(parameter);
                          statementParams.add(sqlStatementParam);
                      } catch (EdmException e) {
                          throw new IllegalArgumentException("Failed to create SQL statement parameter", e);
                      }
                  });
    }

    /**
     * Table column for select.
     *
     * @param type the type
     * @param prop the prop
     * @return the object
     */
    private Object tableColumnForSelect(final EdmStructuralType type, final EdmProperty prop) {
        return query.getSQLTableColumn(type, prop) + " AS \"" + query.getSQLTableColumnAlias(type, prop) + "\"";
    }

    /**
     * Table column for select without alias.
     *
     * @param type the type
     * @param prop the prop
     * @return the object
     */
    private Object tableColumnForSelectWithoutAlias(final EdmStructuralType type, final EdmProperty prop) {
        return query.getSQLTableColumn(type, prop);
    }
}
