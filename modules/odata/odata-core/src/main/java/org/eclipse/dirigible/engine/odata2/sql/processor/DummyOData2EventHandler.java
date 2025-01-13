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

import java.io.InputStream;
import java.util.Map;

import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.apache.olingo.odata2.api.processor.ODataResponse;
import org.apache.olingo.odata2.api.uri.info.DeleteUriInfo;
import org.apache.olingo.odata2.api.uri.info.PostUriInfo;
import org.apache.olingo.odata2.api.uri.info.PutMergePatchUriInfo;
import org.eclipse.dirigible.engine.odata2.sql.api.OData2EventHandler;

/**
 * The Class DummyOData2EventHandler.
 */
public class DummyOData2EventHandler implements OData2EventHandler {

    /** The Constant ODATA2_EVENT_HANDLER_NAME. */
    private static final String ODATA2_EVENT_HANDLER_NAME = "dummy";

    /**
     * Before create entity.
     *
     * @param uriInfo the uri info
     * @param requestContentType the request content type
     * @param contentType the content type
     * @param entry the entry
     * @param context the context
     * @return the o data response
     */
    @Override
    public ODataResponse beforeCreateEntity(PostUriInfo uriInfo, String requestContentType, String contentType, ODataEntry entry,
            Map<Object, Object> context) {
        return null;
    }

    /**
     * After create entity.
     *
     * @param uriInfo the uri info
     * @param requestContentType the request content type
     * @param contentType the content type
     * @param entry the entry
     * @param context the context
     * @return the o data response
     */
    @Override
    public ODataResponse afterCreateEntity(PostUriInfo uriInfo, String requestContentType, String contentType, ODataEntry entry,
            Map<Object, Object> context) {
        return null;
    }

    /**
     * Checks if is using on create entity.
     *
     * @param uriInfo the uri info
     * @param requestContentType the request content type
     * @param contentType the content type
     * @return true, if is using on create entity
     */
    @Override
    public boolean isUsingOnCreateEntity(PostUriInfo uriInfo, String requestContentType, String contentType) {
        return false;
    }

    /**
     * Checks if is using after create entity.
     *
     * @param uriInfo the uri info
     * @param requestContentType the request content type
     * @param contentType the content type
     * @return true, if is using after create entity
     */
    @Override
    public boolean isUsingAfterCreateEntity(PostUriInfo uriInfo, String requestContentType, String contentType) {
        return false;
    }

    /**
     * On create entity.
     *
     * @param uriInfo the uri info
     * @param content the content
     * @param requestContentType the request content type
     * @param contentType the content type
     * @param context the context
     * @return the o data response
     */
    @Override
    public ODataResponse onCreateEntity(PostUriInfo uriInfo, InputStream content, String requestContentType, String contentType,
            Map<Object, Object> context) {
        return null;
    }

    /**
     * Forbid create entity.
     *
     * @param uriInfo the uri info
     * @param requestContentType the request content type
     * @param contentType the content type
     * @return true, if successful
     */
    @Override
    public boolean forbidCreateEntity(PostUriInfo uriInfo, String requestContentType, String contentType) {
        return false;
    }

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
     */
    @Override
    public ODataResponse beforeUpdateEntity(PutMergePatchUriInfo uriInfo, String requestContentType, boolean merge, String contentType,
            ODataEntry entry, Map<Object, Object> context) {
        return null;
    }

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
     */
    @Override
    public ODataResponse afterUpdateEntity(PutMergePatchUriInfo uriInfo, String requestContentType, boolean merge, String contentType,
            ODataEntry entry, Map<Object, Object> context) {
        return null;
    }

    /**
     * Checks if is using on update entity.
     *
     * @param uriInfo the uri info
     * @param requestContentType the request content type
     * @param merge the merge
     * @param contentType the content type
     * @return true, if is using on update entity
     */
    @Override
    public boolean isUsingOnUpdateEntity(PutMergePatchUriInfo uriInfo, String requestContentType, boolean merge, String contentType) {
        return false;
    }

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
     */
    @Override
    public ODataResponse onUpdateEntity(PutMergePatchUriInfo uriInfo, InputStream content, String requestContentType, boolean merge,
            String contentType, Map<Object, Object> context) {
        return null;
    }

    /**
     * Forbid update entity.
     *
     * @param uriInfo the uri info
     * @param requestContentType the request content type
     * @param merge the merge
     * @param contentType the content type
     * @return true, if successful
     */
    @Override
    public boolean forbidUpdateEntity(PutMergePatchUriInfo uriInfo, String requestContentType, boolean merge, String contentType) {
        return false;
    }

    /**
     * Before delete entity.
     *
     * @param uriInfo the uri info
     * @param contentType the content type
     * @param context the context
     * @return the o data response
     */
    @Override
    public ODataResponse beforeDeleteEntity(DeleteUriInfo uriInfo, String contentType, Map<Object, Object> context) {
        return null;
    }

    /**
     * After delete entity.
     *
     * @param uriInfo the uri info
     * @param contentType the content type
     * @param context the context
     * @return the o data response
     */
    @Override
    public ODataResponse afterDeleteEntity(DeleteUriInfo uriInfo, String contentType, Map<Object, Object> context) {
        return null;
    }

    /**
     * Checks if is using on delete entity.
     *
     * @param uriInfo the uri info
     * @param contentType the content type
     * @return true, if is using on delete entity
     */
    @Override
    public boolean isUsingOnDeleteEntity(DeleteUriInfo uriInfo, String contentType) {
        return false;
    }

    /**
     * On delete entity.
     *
     * @param uriInfo the uri info
     * @param contentType the content type
     * @param context the context
     * @return the o data response
     */
    @Override
    public ODataResponse onDeleteEntity(DeleteUriInfo uriInfo, String contentType, Map<Object, Object> context) {
        return null;
    }

    /**
     * Forbid delete entity.
     *
     * @param uriInfo the uri info
     * @param contentType the content type
     * @return true, if successful
     */
    @Override
    public boolean forbidDeleteEntity(DeleteUriInfo uriInfo, String contentType) {
        return false;
    }

    /**
     * Gets the name.
     *
     * @return the name
     */
    @Override
    public String getName() {
        return ODATA2_EVENT_HANDLER_NAME;
    }

}
