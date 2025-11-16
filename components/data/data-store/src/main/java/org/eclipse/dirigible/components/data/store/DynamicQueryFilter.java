/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.store;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.annotations.Expose;

import jakarta.persistence.EntityManager;

/**
 * Defines the supported relational and logical operators for dynamic filtering.
 */
enum Operator {
    EQ("="), // Equals
    NE("<>"), // Not Equals
    GT(">"), // Greater Than
    LT("<"), // Less Than
    GE(">="), // Greater Than or Equals
    LE("<="), // Less Than or Equals
    LIKE("LIKE"), // SQL LIKE operator
    BETWEEN("BETWEEN"), // SQL BETWEEN operator (requires two values)
    IN("IN"); // SQL IN operator (requires a List or Array of values)

    private final String sqlEquivalent;

    Operator(String sqlEquivalent) {
        this.sqlEquivalent = sqlEquivalent;
    }

    public String getSqlEquivalent() {
        return sqlEquivalent;
    }
}


/**
 * Encapsulates a single filtering condition for a dynamic query.
 */
class QueryCondition {
    @Expose
    public final String propertyName;
    @Expose
    public final Operator operator;
    @Expose
    public final Object value; // Can be a single value, or an array/list for BETWEEN/IN

    public QueryCondition(String propertyName, Operator operator, Object value) {
        this.propertyName = propertyName;
        this.operator = operator;
        this.value = value;
    }

    /**
     * Factory method for simple single-value conditions (EQ, GT, LT, etc.)
     */
    public static QueryCondition of(String propertyName, Operator operator, Object value) {
        return new QueryCondition(propertyName, operator, value);
    }

    /**
     * Factory method for BETWEEN conditions, which take two bounds.
     */
    public static QueryCondition between(String propertyName, Object lowerBound, Object upperBound) {
        return new QueryCondition(propertyName, Operator.BETWEEN, new Object[] {lowerBound, upperBound});
    }

    /**
     * Factory method for IN conditions, which take a collection of values.
     */
    public static QueryCondition in(String propertyName, List<?> values) {
        return new QueryCondition(propertyName, Operator.IN, values);
    }
}


/**
 * Defines the supported sorting directions.
 */
enum SortDirection {
    ASC, DESC
}


/**
 * Encapsulates a single sorting condition for a dynamic query.
 */
class SortCondition {
    @Expose
    public final String propertyName;
    @Expose
    public final SortDirection direction;

    public SortCondition(String propertyName, SortDirection direction) {
        this.propertyName = propertyName;
        this.direction = direction;
    }

    public static SortCondition asc(String propertyName) {
        return new SortCondition(propertyName, SortDirection.ASC);
    }

    public static SortCondition desc(String propertyName) {
        return new SortCondition(propertyName, SortDirection.DESC);
    }
}


/**
 * Encapsulates all the parameters for a dynamic query.
 */
class QueryOptions {
    @Expose
    public List<QueryCondition> conditions;
    @Expose
    public List<SortCondition> sorts;
    @Expose
    public int limit;
    @Expose
    public int offset;
}


/**
 * Utility class to build and execute a dynamic HQL query based on a list of structured
 * QueryCondition objects against a Hibernate dynamic entity (Map).
 */
public class DynamicQueryFilter {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(DynamicQueryFilter.class);

    /**
     * Executes a dynamic filter query against an entity identified by its entity-name.
     *
     * @param session The Hibernate Session
     * @param entityName The Hibernate entity-name (e.g., "Order" or "OrderItem").
     * @param queryOptions All the objects defining the filter criteria, sort, limit and offset.
     * @return A list of result Maps matching the criteria.
     */
    public static List<Map> list(Session session, String entityName, QueryOptions queryOptions) {
        Query<Map> query = filterDynamic(session, entityName, queryOptions);
        return query.getResultList();
    }

    /**
     * Executes a count dynamic filter query against an entity identified by its entity-name.
     *
     * @param session The Hibernate Session
     * @param entityName The Hibernate entity-name (e.g., "Order" or "OrderItem").
     * @param queryOptions All the objects defining the filter criteria, sort, limit and offset.
     * @return The count of the list of result Maps matching the criteria.
     */
    public static long count(Session session, String entityName, QueryOptions queryOptions) {
        Query<Map> query = filterDynamic(session, entityName, queryOptions);
        return query.getResultCount();
    }


    /**
     * Filter dynamic.
     *
     * @param session the session
     * @param entityName the entity name
     * @param queryOptions the query options
     * @return the query
     */
    private static Query<Map> filterDynamic(Session session, String entityName, QueryOptions queryOptions) {

        StringBuilder hql = new StringBuilder("SELECT e FROM " + entityName + " e WHERE 1=1");

        Map<String, Object> params = new HashMap<>();
        // Used to ensure unique parameter names in case a property is filtered multiple times
        AtomicInteger paramIndex = new AtomicInteger(0);

        if (queryOptions.conditions != null && !queryOptions.conditions.isEmpty()) {
            for (QueryCondition condition : queryOptions.conditions) {
                String propertyName = condition.propertyName;
                Operator operator = condition.operator;
                Object value = condition.value;

                if (value == null || (value instanceof List && ((List) value).isEmpty())) {
                    continue;
                }

                int currentParamIndex = paramIndex.getAndIncrement();

                if (operator == Operator.BETWEEN) {
                    // BETWEEN requires two values (bounds)
                    if (value instanceof Object[] && ((Object[]) value).length == 2) {
                        Object[] bounds = (Object[]) value;
                        String paramName1 = propertyName + "B" + currentParamIndex + "a";
                        String paramName2 = propertyName + "B" + currentParamIndex + "b";

                        hql.append(" AND e.")
                           .append(propertyName)
                           .append(" BETWEEN :")
                           .append(paramName1)
                           .append(" AND :")
                           .append(paramName2);

                        params.put(paramName1, bounds[0]);
                        params.put(paramName2, bounds[1]);
                    } else {
                        logger.error("Warning: BETWEEN operator requires an array of two values.");
                    }
                } else if (operator == Operator.IN) {
                    // IN requires a collection of values
                    String paramName = propertyName + "I" + currentParamIndex;
                    hql.append(" AND e.")
                       .append(propertyName)
                       .append(" IN (:")
                       .append(paramName)
                       .append(")");
                    params.put(paramName, value);
                } else {
                    // All other single-value operators (=, <, >, LIKE, etc.)
                    String paramName = propertyName + "S" + currentParamIndex;
                    hql.append(" AND e.")
                       .append(propertyName)
                       .append(" ")
                       .append(operator.getSqlEquivalent())
                       .append(" :")
                       .append(paramName);
                    params.put(paramName, value);
                }
            }
        }

        if (queryOptions.sorts != null && !queryOptions.sorts.isEmpty()) {
            hql.append(" ORDER BY ");
            boolean first = true;
            for (SortCondition sort : queryOptions.sorts) {
                if (!first) {
                    hql.append(", ");
                }
                // Append the property name and direction (e.g., e.orderId DESC)
                hql.append("e.")
                   .append(sort.propertyName)
                   .append(" ")
                   .append(sort.direction.name());
                first = false;
            }
        }

        Query<Map> query = session.createQuery(hql.toString(), Map.class);

        for (Map.Entry<String, Object> param : params.entrySet()) {
            query.setParameter(param.getKey(), param.getValue());
        }

        if (queryOptions.limit > 0) {
            query.setMaxResults(queryOptions.limit);
        }
        if (queryOptions.offset >= 0) {
            query.setFirstResult(queryOptions.offset);
        }

        return query;
    }

}
