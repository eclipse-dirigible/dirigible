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

import org.apache.olingo.odata2.api.edm.EdmEntityType;
import org.apache.olingo.odata2.api.edm.EdmException;
import org.apache.olingo.odata2.api.edm.EdmProperty;
import org.eclipse.dirigible.engine.odata2.sql.api.SQLClause;
import org.eclipse.dirigible.engine.odata2.sql.builder.SQLContext;

/**
 * The Class SQLGroupByClause.
 */
public class SQLGroupByClause implements SQLClause {

    /** The query. */
    private final org.eclipse.dirigible.engine.odata2.sql.builder.SQLSelectBuilder query;

    /** The entity type. */
    private final EdmEntityType entityType;

    /**
     * Instantiates a new SQL group by clause.
     *
     * @param query the query
     * @param entityType the entity type
     */
    public SQLGroupByClause(final org.eclipse.dirigible.engine.odata2.sql.builder.SQLSelectBuilder query, final EdmEntityType entityType) {
        this.query = query;
        this.entityType = entityType;
    }

    /**
     * Evaluate.
     *
     * @param context the context
     * @return the string
     * @throws EdmException the edm exception
     */
    @Override
    public String evaluate(final SQLContext context) throws EdmException {
        return groupByClauseBuilder(context);
    }

    /**
     * Group by clause builder.
     *
     * @param context the context
     * @return the string
     * @throws EdmException the edm exception
     */
    public String groupByClauseBuilder(SQLContext context) throws EdmException {
        StringBuilder groupByClause = new StringBuilder();
        for (String propertyName : entityType.getPropertyNames()) {
            EdmProperty prop = (EdmProperty) entityType.getProperty(propertyName);
            String columnNameWithoutAlias = query.getPureSQLColumnName(entityType, prop);

            if (!query.isAggregationTypeExplicit(entityType)
                    && query.isColumnContainedInAggregationProp(entityType, columnNameWithoutAlias)) {
                continue;
            }

            if (!groupByClause.toString()
                              .isEmpty()) {
                groupByClause.append(", ");
            }

            if (context == null || context.getDatabaseSystem() != null) {
                groupByClause.append(query.getSQLTableColumn(entityType, prop));
            } else {
                groupByClause.append(query.getSQLTableColumnAlias(entityType, prop)); // this gives the correct "group by" column name for
                // Open SQL
            }
        }
        return groupByClause.toString();
    }

    /**
     * Checks if is empty.
     *
     * @return true, if is empty
     */
    @Override
    public boolean isEmpty() {
        return true;
    }
}
