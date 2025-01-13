/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.odata2.sql.processor;

import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.processor.ODataContext;
import org.apache.olingo.odata2.api.uri.UriInfo;
import org.apache.olingo.odata2.api.uri.info.DeleteUriInfo;
import org.apache.olingo.odata2.api.uri.info.PostUriInfo;
import org.apache.olingo.odata2.api.uri.info.PutMergePatchUriInfo;
import org.eclipse.dirigible.engine.odata2.sql.api.SQLInterceptor;
import org.eclipse.dirigible.engine.odata2.sql.builder.SQLDeleteBuilder;
import org.eclipse.dirigible.engine.odata2.sql.builder.SQLInsertBuilder;
import org.eclipse.dirigible.engine.odata2.sql.builder.SQLSelectBuilder;
import org.eclipse.dirigible.engine.odata2.sql.builder.SQLUpdateBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The Class SQLInterceptorChain.
 */
public class SQLInterceptorChain implements SQLInterceptor {

    /** The interceptors. */
    private final List<SQLInterceptor> interceptors;

    /**
     * Instantiates a new SQL interceptor chain.
     *
     * @param interceptors the interceptors
     */
    public SQLInterceptorChain(SQLInterceptor... interceptors) {
        this.interceptors = new ArrayList<>();
        this.interceptors.addAll(Arrays.asList(interceptors));
    }

    /**
     * Adds the interceptor.
     *
     * @param interceptor the interceptor
     */
    public void addInterceptor(SQLInterceptor interceptor) {
        this.interceptors.add(interceptor);
    }

    /**
     * On create.
     *
     * @param query the query
     * @param uriInfo the uri info
     * @param context the context
     * @return the SQL insert builder
     * @throws ODataException the o data exception
     */
    @Override
    public SQLInsertBuilder onCreate(SQLInsertBuilder query, PostUriInfo uriInfo, ODataContext context) throws ODataException {
        SQLInsertBuilder resultQuery = query;
        for (SQLInterceptor interceptor : interceptors) {
            SQLInsertBuilder res = interceptor.onCreate(resultQuery, uriInfo, context);
            resultQuery = (res == null ? query : res);
        }
        return resultQuery;
    }

    /**
     * On read.
     *
     * @param query the query
     * @param uriInfo the uri info
     * @param context the context
     * @return the SQL select builder
     * @throws ODataException the o data exception
     */
    @Override
    public SQLSelectBuilder onRead(SQLSelectBuilder query, UriInfo uriInfo, ODataContext context) throws ODataException {
        SQLSelectBuilder resultQuery = query;
        for (SQLInterceptor interceptor : interceptors) {
            SQLSelectBuilder res = interceptor.onRead(resultQuery, uriInfo, context);
            resultQuery = (res == null ? query : res);
        }
        return resultQuery;
    }

    /**
     * On update.
     *
     * @param query the query
     * @param uriInfo the uri info
     * @param context the context
     * @return the SQL update builder
     * @throws ODataException the o data exception
     */
    @Override
    public SQLUpdateBuilder onUpdate(SQLUpdateBuilder query, PutMergePatchUriInfo uriInfo, ODataContext context) throws ODataException {
        SQLUpdateBuilder resultQuery = query;
        for (SQLInterceptor interceptor : interceptors) {
            SQLUpdateBuilder res = interceptor.onUpdate(resultQuery, uriInfo, context);
            resultQuery = (res == null ? query : res);
        }
        return resultQuery;
    }

    /**
     * On delete.
     *
     * @param query the query
     * @param uriInfo the uri info
     * @param context the context
     * @return the SQL delete builder
     * @throws ODataException the o data exception
     */
    @Override
    public SQLDeleteBuilder onDelete(SQLDeleteBuilder query, DeleteUriInfo uriInfo, ODataContext context) throws ODataException {
        SQLDeleteBuilder resultQuery = query;
        for (SQLInterceptor interceptor : interceptors) {
            SQLDeleteBuilder res = interceptor.onDelete(resultQuery, uriInfo, context);
            resultQuery = (res == null ? query : res);

        }
        return resultQuery;
    }

}
