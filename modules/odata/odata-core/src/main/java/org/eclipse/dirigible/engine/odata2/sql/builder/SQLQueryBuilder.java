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

import org.apache.olingo.odata2.api.edm.*;
import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.processor.ODataContext;
import org.apache.olingo.odata2.api.uri.*;
import org.eclipse.dirigible.engine.odata2.sql.api.OData2Exception;
import org.eclipse.dirigible.engine.odata2.sql.api.SQLInterceptor;
import org.eclipse.dirigible.engine.odata2.sql.api.SQLStatementParam;
import org.eclipse.dirigible.engine.odata2.sql.binding.EdmTableBinding.ColumnInfo;
import org.eclipse.dirigible.engine.odata2.sql.binding.EdmTableBindingProvider;
import org.eclipse.dirigible.engine.odata2.sql.clause.SQLWhereClause;
import org.eclipse.dirigible.engine.odata2.sql.processor.SQLInterceptorChain;

import java.util.*;

import static org.apache.olingo.odata2.api.commons.HttpStatusCodes.REQUESTED_RANGE_NOT_SATISFIABLE;
import static org.eclipse.dirigible.engine.odata2.sql.builder.EdmUtils.evaluateDateTimeExpressions;

/**
 * The Class SQLQueryBuilder.
 */
public class SQLQueryBuilder {

    /**
     * Maximum number of rows returned (threshold for server-side paging).
     */
    public static final int DEFAULT_SERVER_PAGING_SIZE = 1000;

    /** The table binding. */
    private final EdmTableBindingProvider tableBinding;

    /** The chain. */
    private final SQLInterceptorChain chain = new SQLInterceptorChain();

    /**
     * Instantiates a new SQL query builder.
     *
     * @param tableBindnig the table bindnig
     */
    public SQLQueryBuilder(EdmTableBindingProvider tableBindnig) {
        this.tableBinding = tableBindnig;
    }

    /**
     * Adds the interceptor.
     *
     * @param interceptor the interceptor
     */
    public void addInterceptor(SQLInterceptor interceptor) {
        chain.addInterceptor(interceptor);
    }

    /**
     * Builds the select count query.
     *
     * @param uri the uri
     * @param context the context
     * @return the SQL select builder
     * @throws ODataException the o data exception
     */
    public SQLSelectBuilder buildSelectCountQuery(final UriInfo uri, ODataContext context) throws ODataException {
        EdmEntityType target = uri.getTargetEntitySet()
                                  .getEntityType();
        SQLSelectBuilder q = new SQLSelectBuilder(tableBinding);
        q.select()
         .count()
         .from(target, uri.getKeyPredicates())
         .join(uri.getStartEntitySet(), uri.getTargetEntitySet(), uri.getNavigationSegments())
         .with(uri.getKeyPredicates())
         .filter(uri.getTargetEntitySet(), uri.getFilter());
        return chain.onRead(q, uri, context);
    }

    /**
     * Builds the select entity query.
     *
     * @param uri the uri
     * @param context the context
     * @return the SQL select builder
     * @throws ODataException the o data exception
     */
    public SQLSelectBuilder buildSelectEntityQuery(final UriInfo uri, ODataContext context) throws ODataException {
        EdmEntityType target = uri.getTargetEntitySet()
                                  .getEntityType();
        SQLSelectBuilder q = new SQLSelectBuilder(tableBinding);
        q.select(uri.getSelect(), uri.getExpand())
         .from(target, uri.getKeyPredicates())
         .filter(uri.getTargetEntitySet(), uri.getFilter())
         .join(uri.getStartEntitySet(), uri.getTargetEntitySet(), uri.getNavigationSegments())
         .with(uri.getKeyPredicates());
        if (uri.getKeyPredicates() != uri.getTargetKeyPredicates()) {
            q.and(whereClauseFromKeyPredicates(q, uri.getTargetEntitySet()
                                                     .getEntityType(),
                    uri.getTargetKeyPredicates()));
        }
        return chain.onRead(q, uri, context);
    }

    /**
     * Builds the select entity set query.
     *
     * @param uri the uri
     * @param context the context
     * @return the SQL select builder
     * @throws ODataException the o data exception
     */
    public SQLSelectBuilder buildSelectEntitySetQuery(final UriInfo uri, ODataContext context) throws ODataException {
        return buildSelectEntitySetQuery(uri, Collections.emptyList(), context);
    }

    /**
     * Builds the select entity set query.
     *
     * @param uri the uri
     * @param readIdsForExpand the read ids for expand
     * @param context the context
     * @return the SQL select builder
     * @throws ODataException the o data exception
     */
    public SQLSelectBuilder buildSelectEntitySetQuery(final UriInfo uri, List<String> readIdsForExpand, ODataContext context)
            throws ODataException {
        EdmEntityType target = uri.getTargetEntitySet()
                                  .getEntityType();

        SQLSelectBuilder q = new SQLSelectBuilder(tableBinding);
        final boolean needsServersidePaging = calculateNeedsServersidePaging(uri);
        Integer effectiveTop;
        if (needsServersidePaging) {
            effectiveTop = getEntityPagingSize(target);
            q.setServersidePaging(true);
        } else {
            effectiveTop = uri.getTop();
        }

        final Integer effectiveSkip = calculateEffectiveSkip(uri);

        if (readIdsForExpand == null || readIdsForExpand.isEmpty()) {
            // no expand, we filter as usual
            q.select(uri.getSelect(), uri.getExpand())
             .top(effectiveTop)
             .skip(effectiveSkip)
             .from(target, uri.getKeyPredicates());
            q.filter(uri.getTargetEntitySet(), uri.getFilter());
        } else {
            // we have the problem that top does not work for exapnd. Therefore we do 2 queries to select the
            // ids of the target entities (with applied filter),
            // and then we do filter on these IDS with the expand, with no top and skip
            // SELECT TOP XXX FROM TTTT AS M WHERE FILTER
            // SELECT XXX WHERE XXX.ID IN (...)
            q.select(uri.getSelect(), uri.getExpand())
             .from(target, uri.getKeyPredicates());
            q.filter(uri.getTargetEntitySet(), getKeyProperty(target), readIdsForExpand);
        }
        q.join(uri.getStartEntitySet(), uri.getTargetEntitySet(), uri.getNavigationSegments())
         .with(uri.getKeyPredicates());
        q.validateOrderBy(uri);
        q.groupBy(uri.getTargetEntitySet()
                     .getEntityType());
        q.orderBy(uri.getOrderBy(), uri.getTargetEntitySet()
                                       .getEntityType());

        return chain.onRead(q, uri, context);
    }

    /**
     * Builds the select entity set ids for top and expand query.
     *
     * @param uri the uri
     * @param context the context
     * @return the SQL select builder
     * @throws ODataException the o data exception
     */
    public SQLSelectBuilder buildSelectEntitySetIdsForTopAndExpandQuery(final UriInfo uri, ODataContext context) throws ODataException {
        SQLSelectBuilder q = new SQLSelectBuilder(tableBinding);
        EdmEntityType target = uri.getTargetEntitySet()
                                  .getEntityType();

        final boolean needsServersidePaging = calculateNeedsServersidePaging(uri);
        Integer effectiveTop;
        if (needsServersidePaging) {
            effectiveTop = getEntityPagingSize(target);
            q.setServersidePaging(true);
        } else {
            effectiveTop = uri.getTop();
        }

        final Integer effectiveSkip = calculateEffectiveSkip(uri);

        q.select(buildSelectItemsForPrimaryKey(target), null)
         .top(effectiveTop)
         .skip(effectiveSkip)
         .from(target, uri.getKeyPredicates());
        q.filter(uri.getTargetEntitySet(), uri.getFilter())
         .join(uri.getStartEntitySet(), uri.getTargetEntitySet(), uri.getNavigationSegments())
         .with(uri.getKeyPredicates());

        // adds additional joins on the navigation properties required for correct ordering of the result
        // set
        for (ArrayList<NavigationPropertySegment> segments : uri.getExpand()) {
            EdmEntitySet joinTarget = uri.getTargetEntitySet();
            for (NavigationPropertySegment nav : segments) {
                // when we have Owners/Addresses the addresses needs to be joined with the Owners.
                // The joinTarget would be Owner when nav.getTargetEntitySet is Address
                q.join(nav.getTargetEntitySet(), joinTarget, Collections.emptyList());
                joinTarget = nav.getTargetEntitySet();
            }
        }
        q.groupBy(uri.getTargetEntitySet()
                     .getEntityType());
        q.orderBy(uri.getOrderBy(), uri.getTargetEntitySet()
                                       .getEntityType());
        return chain.onRead(q, uri, context);
    }

    /**
     * Gets the table binding.
     *
     * @return the table binding
     */
    public EdmTableBindingProvider getTableBinding() {
        return tableBinding;
    }

    /**
     * Gets the entity paging size.
     *
     * @param targetType the target type
     * @return the entity paging size
     */
    public Integer getEntityPagingSize(EdmEntityType targetType) {
        return DEFAULT_SERVER_PAGING_SIZE;
    }

    /**
     * Gets the key property.
     *
     * @param type the type
     * @return the key property
     * @throws EdmException the edm exception
     */
    private EdmProperty getKeyProperty(EdmEntityType type) throws EdmException {
        List<String> keyProperties = type.getKeyPropertyNames();
        if (keyProperties.size() > 1) {
            throw new IllegalArgumentException(
                    "Complex key properties are not supported so far. Extend the default SQLSelectBuilder Builder with your own!");
        }
        return (EdmProperty) type.getProperty(keyProperties.get(0));
    }

    /**
     * Builds the select items for primary key.
     *
     * @param target the target
     * @return the list
     * @throws EdmException the edm exception
     */
    private List<SelectItem> buildSelectItemsForPrimaryKey(final EdmEntityType target) throws EdmException {
        List<SelectItem> result = new ArrayList<>();
        List<String> keyProperties = target.getKeyPropertyNames();
        for (final String prop : keyProperties) {
            SelectItem item = new SelectItem() {

                @Override
                public boolean isStar() {
                    return false;
                }

                @Override
                public EdmProperty getProperty() {
                    try {
                        return (EdmProperty) target.getProperty(prop);
                    } catch (EdmException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public List<NavigationPropertySegment> getNavigationPropertySegments() {
                    return null;
                }
            };

            result.add(item);
        }
        return result;
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
    private static SQLWhereClause whereClauseFromKeyPredicates(SQLSelectBuilder query, EdmStructuralType type,
            final List<KeyPredicate> keyPredicates) throws EdmException {
        StringBuilder whereClause = new StringBuilder();
        List<SQLStatementParam> SQLParams = new ArrayList<>();
        if (keyPredicates != null) {
            Iterator<KeyPredicate> it = keyPredicates.iterator();
            while (it.hasNext()) {
                KeyPredicate keyPredicate = it.next();
                Object literal = keyPredicate.getLiteral();

                EdmProperty property = keyPredicate.getProperty();
                if (property.isSimple()) {
                    EdmSimpleType edmSimpleType = (EdmSimpleType) keyPredicate.getProperty()
                                                                              .getType();
                    literal = evaluateDateTimeExpressions(literal, edmSimpleType);
                    ColumnInfo info = query.getSQLTableColumnInfo(type, property);
                    whereClause.append(info.getColumnName())
                               .append(" = ?");
                    SQLParams.add(SQLWhereClause.param(literal, edmSimpleType, info));
                } else {
                    // TODO what to do with complex properties?
                    throw new IllegalStateException();
                }
                if (it.hasNext()) {
                    whereClause.append(" AND ");
                }
            }
            SQLWhereClause where = new SQLWhereClause(whereClause.toString(), SQLParams.toArray(new SQLStatementParam[SQLParams.size()]));
            return where;
        } else {
            return new SQLWhereClause();
        }
    }

    /**
     * Calculate needs serverside paging.
     *
     * @param uri the uri
     * @return true, if successful
     * @throws EdmException the edm exception
     */
    private boolean calculateNeedsServersidePaging(UriInfo uri) throws EdmException {
        final Integer top = uri.getTop();
        return top == null || top > getEntityPagingSize(uri.getTargetEntitySet()
                                                           .getEntityType());
    }

    /**
     * Calculates the effective value for skip which is based on $skip and $skipToken.
     *
     * @param uri the uri
     * @return the integer
     */
    private static Integer calculateEffectiveSkip(final UriInfo uri) {
        String skipTokenString = uri.getSkipToken();
        Integer skipToken;
        if (skipTokenString != null) {
            try {
                skipToken = Integer.parseInt(skipTokenString);
                if (skipToken < 0) {
                    throw new OData2Exception("$skipToken must be a positive number equal or greater than zero",
                            REQUESTED_RANGE_NOT_SATISFIABLE);
                }
            } catch (NumberFormatException e) {
                throw new OData2Exception("$skipToken must be a number", REQUESTED_RANGE_NOT_SATISFIABLE, e);
            }
        } else {
            skipToken = null;
        }

        Integer skip = uri.getSkip();
        Integer effectiveSkip;
        if (skipToken != null && skip != null) {
            effectiveSkip = skipToken + skip;
        } else if (skipToken == null && skip != null) {
            effectiveSkip = skip;
        } else if (skipToken != null) {
            effectiveSkip = skipToken;
        } else {
            effectiveSkip = null;
        }
        return effectiveSkip;
    }

    /**
     * Builds the insert entity query.
     *
     * @param uri the uri
     * @param entry the entry
     * @param context the context
     * @return the SQL insert builder
     * @throws ODataException the o data exception
     */
    public SQLInsertBuilder buildInsertEntityQuery(final UriInfo uri, ODataEntry entry, ODataContext context) throws ODataException {
        EdmEntityType target = uri.getTargetEntitySet()
                                  .getEntityType();
        SQLInsertBuilder q = new SQLInsertBuilder(tableBinding);
        q.into(target, entry);
        return chain.onCreate(q, uri, context);
    }

    /**
     * Builds the delete entity query.
     *
     * @param uri the uri
     * @param keys the keys
     * @param context the context
     * @return the SQL delete builder
     * @throws ODataException the o data exception
     */
    public SQLDeleteBuilder buildDeleteEntityQuery(final UriInfo uri, Map<String, Object> keys, ODataContext context)
            throws ODataException {
        EdmEntityType target = uri.getStartEntitySet()
                                  .getEntityType();
        SQLDeleteBuilder q = new SQLDeleteBuilder(tableBinding);
        q.deleteFrom(target)
         .keys(keys);
        return chain.onDelete(q, uri, context);
    }

    /**
     * Builds the update entity query.
     *
     * @param uri the uri
     * @param entry the entry
     * @param uriKeys the uri keys
     * @param context the context
     * @return the SQL update builder
     * @throws ODataException the o data exception
     */
    public SQLUpdateBuilder buildUpdateEntityQuery(UriInfo uri, ODataEntry entry, Map<String, Object> uriKeys, ODataContext context)
            throws ODataException {
        EdmEntityType target = uri.getTargetEntitySet()
                                  .getEntityType();
        SQLUpdateBuilder q = new SQLUpdateBuilder(tableBinding, uriKeys);
        q.update(target, entry);
        return chain.onUpdate(q, uri, context);
    }

}
