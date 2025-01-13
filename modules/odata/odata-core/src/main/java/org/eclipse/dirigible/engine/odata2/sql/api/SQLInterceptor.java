/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.odata2.sql.api;

import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.processor.ODataContext;
import org.apache.olingo.odata2.api.uri.UriInfo;
import org.apache.olingo.odata2.api.uri.info.DeleteUriInfo;
import org.apache.olingo.odata2.api.uri.info.PostUriInfo;
import org.apache.olingo.odata2.api.uri.info.PutMergePatchUriInfo;
import org.eclipse.dirigible.engine.odata2.sql.builder.SQLDeleteBuilder;
import org.eclipse.dirigible.engine.odata2.sql.builder.SQLInsertBuilder;
import org.eclipse.dirigible.engine.odata2.sql.builder.SQLSelectBuilder;
import org.eclipse.dirigible.engine.odata2.sql.builder.SQLUpdateBuilder;

/**
 * The Interface SQLInterceptor.
 */
public interface SQLInterceptor {

    /**
     * On create.
     *
     * @param query the query
     * @param uriInfo the uri info
     * @param context the context
     * @return the SQL insert builder
     * @throws ODataException the o data exception
     */
    default SQLInsertBuilder onCreate(SQLInsertBuilder query, PostUriInfo uriInfo, ODataContext context) throws ODataException {
        return query;
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
    default SQLSelectBuilder onRead(SQLSelectBuilder query, UriInfo uriInfo, ODataContext context) throws ODataException {
        return query;
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
    default SQLUpdateBuilder onUpdate(SQLUpdateBuilder query, PutMergePatchUriInfo uriInfo, ODataContext context) throws ODataException {
        return query;
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
    default SQLDeleteBuilder onDelete(SQLDeleteBuilder query, DeleteUriInfo uriInfo, ODataContext context) throws ODataException {
        return query;
    }
}
