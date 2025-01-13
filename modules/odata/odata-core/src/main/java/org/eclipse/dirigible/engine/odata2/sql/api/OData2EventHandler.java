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

import java.io.InputStream;
import java.util.Map;

import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.processor.ODataResponse;
import org.apache.olingo.odata2.api.uri.info.DeleteUriInfo;
import org.apache.olingo.odata2.api.uri.info.PostUriInfo;
import org.apache.olingo.odata2.api.uri.info.PutMergePatchUriInfo;

/**
 * The Interface OData2EventHandler.
 */
public interface OData2EventHandler {

    /** The Constant DIRIGIBLE_ODATA_EVENT_HANDLER_NAME. */
    public static final String DIRIGIBLE_ODATA_EVENT_HANDLER_NAME = "DIRIGIBLE_ODATA_EVENT_HANDLER_NAME";

    /** The Constant DEFAULT_ODATA_EVENT_HANDLER_NAME. */
    public static final String DEFAULT_ODATA_EVENT_HANDLER_NAME = "default";

    /**
     * Before create entity.
     *
     * @param uriInfo the uri info
     * @param requestContentType the request content type
     * @param contentType the content type
     * @param entry the entry
     * @param context the context
     * @return the o data response
     * @throws ODataException the o data exception
     */
    ODataResponse beforeCreateEntity(final PostUriInfo uriInfo, final String requestContentType, final String contentType, ODataEntry entry,
            Map<Object, Object> context) throws ODataException;

    /**
     * After create entity.
     *
     * @param uriInfo the uri info
     * @param requestContentType the request content type
     * @param contentType the content type
     * @param entry the entry
     * @param context the context
     * @return the o data response
     * @throws ODataException the o data exception
     */
    ODataResponse afterCreateEntity(final PostUriInfo uriInfo, final String requestContentType, final String contentType,
            final ODataEntry entry, Map<Object, Object> context) throws ODataException;

    /**
     * Checks if is using on create entity.
     *
     * @param uriInfo the uri info
     * @param requestContentType the request content type
     * @param contentType the content type
     * @return true, if is using on create entity
     */
    boolean isUsingOnCreateEntity(final PostUriInfo uriInfo, final String requestContentType, final String contentType);

    /**
     * Checks if is using after create entity.
     *
     * @param uriInfo the uri info
     * @param requestContentType the request content type
     * @param contentType the content type
     * @return true, if is using after create entity
     */
    boolean isUsingAfterCreateEntity(final PostUriInfo uriInfo, final String requestContentType, final String contentType);

    /**
     * On create entity.
     *
     * @param uriInfo the uri info
     * @param content the content
     * @param requestContentType the request content type
     * @param contentType the content type
     * @param context the context
     * @return the o data response
     * @throws ODataException the o data exception
     */
    ODataResponse onCreateEntity(final PostUriInfo uriInfo, final InputStream content, final String requestContentType,
            final String contentType, Map<Object, Object> context) throws ODataException;

    /**
     * Forbid create entity.
     *
     * @param uriInfo the uri info
     * @param requestContentType the request content type
     * @param contentType the content type
     * @return true, if successful
     */
    boolean forbidCreateEntity(final PostUriInfo uriInfo, final String requestContentType, final String contentType);

    /**
     * Before update entity.
     *
     * @param uriInfo the uri info
     * @param requestContentType the request content type
     * @param merge the merge
     * @param contentType the content type
     * @param entry the entry
     * @param context the context
     * @return the o data response
     * @throws ODataException the o data exception
     */
    ODataResponse beforeUpdateEntity(final PutMergePatchUriInfo uriInfo, final String requestContentType, final boolean merge,
            final String contentType, final ODataEntry entry, Map<Object, Object> context) throws ODataException;

    /**
     * After update entity.
     *
     * @param uriInfo the uri info
     * @param requestContentType the request content type
     * @param merge the merge
     * @param contentType the content type
     * @param entry the entry
     * @param context the context
     * @return the o data response
     * @throws ODataException the o data exception
     */
    ODataResponse afterUpdateEntity(final PutMergePatchUriInfo uriInfo, final String requestContentType, final boolean merge,
            final String contentType, final ODataEntry entry, Map<Object, Object> context) throws ODataException;

    /**
     * Checks if is using on update entity.
     *
     * @param uriInfo the uri info
     * @param requestContentType the request content type
     * @param merge the merge
     * @param contentType the content type
     * @return true, if is using on update entity
     */
    boolean isUsingOnUpdateEntity(final PutMergePatchUriInfo uriInfo, final String requestContentType, final boolean merge,
            final String contentType);


    /**
     * On update entity.
     *
     * @param uriInfo the uri info
     * @param content the content
     * @param requestContentType the request content type
     * @param merge the merge
     * @param contentType the content type
     * @param context the context
     * @return the o data response
     * @throws ODataException the o data exception
     */
    ODataResponse onUpdateEntity(final PutMergePatchUriInfo uriInfo, final InputStream content, final String requestContentType,
            final boolean merge, final String contentType, Map<Object, Object> context) throws ODataException;

    /**
     * Forbid update entity.
     *
     * @param uriInfo the uri info
     * @param requestContentType the request content type
     * @param merge the merge
     * @param contentType the content type
     * @return true, if successful
     */
    boolean forbidUpdateEntity(final PutMergePatchUriInfo uriInfo, final String requestContentType, final boolean merge,
            final String contentType);

    /**
     * Before delete entity.
     *
     * @param uriInfo the uri info
     * @param contentType the content type
     * @param context the context
     * @return the o data response
     * @throws ODataException the o data exception
     */
    ODataResponse beforeDeleteEntity(final DeleteUriInfo uriInfo, final String contentType, Map<Object, Object> context)
            throws ODataException;

    /**
     * After delete entity.
     *
     * @param uriInfo the uri info
     * @param contentType the content type
     * @param context the context
     * @return the o data response
     * @throws ODataException the o data exception
     */
    ODataResponse afterDeleteEntity(final DeleteUriInfo uriInfo, final String contentType, Map<Object, Object> context)
            throws ODataException;

    /**
     * Checks if is using on delete entity.
     *
     * @param uriInfo the uri info
     * @param contentType the content type
     * @return true, if is using on delete entity
     */
    boolean isUsingOnDeleteEntity(final DeleteUriInfo uriInfo, final String contentType);

    /**
     * On delete entity.
     *
     * @param uriInfo the uri info
     * @param contentType the content type
     * @param context the context
     * @return the o data response
     * @throws ODataException the o data exception
     */
    ODataResponse onDeleteEntity(final DeleteUriInfo uriInfo, final String contentType, Map<Object, Object> context) throws ODataException;

    /**
     * Forbid delete entity.
     *
     * @param uriInfo the uri info
     * @param contentType the content type
     * @return true, if successful
     */
    boolean forbidDeleteEntity(final DeleteUriInfo uriInfo, final String contentType);

    /**
     * Gets the name.
     *
     * @return the name
     */
    String getName();

}
