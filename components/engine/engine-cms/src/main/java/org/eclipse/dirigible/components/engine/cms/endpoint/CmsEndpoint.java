/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.cms.endpoint;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.commons.api.helpers.ContentTypeHelper;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.ResourcesCache;
import org.eclipse.dirigible.commons.config.ResourcesCache.Cache;
import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.base.http.roles.Roles;
import org.eclipse.dirigible.components.engine.cms.CmisDocument;
import org.eclipse.dirigible.components.engine.cms.CmisObject;
import org.eclipse.dirigible.components.engine.cms.CmisSessionFactory;
import org.eclipse.dirigible.components.engine.cms.ObjectType;
import org.eclipse.dirigible.components.engine.cms.documents.DocumentAccessEvaluator;
import org.eclipse.dirigible.components.engine.cms.service.CmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The Class CmsEndpoint.
 * <p>
 * Secured path only. The CMS holds tenant BUSINESS content - record attachments, generated document
 * snapshots and the database export dumps - so it must never be served from the unauthenticated
 * {@code /public/**} space, where a known or guessable path is readable with no credentials at all.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_SECURED + "cms")
public class CmsEndpoint extends BaseEndpoint {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(CmsEndpoint.class);

    /** The Constant INDEX_HTML. */
    private static final String INDEX_HTML = "index.html";

    /**
     * The CMS folder the asynchronous database export writes its dumps into. Mirrors
     * {@code DataAsyncExportService.EXPORTS_FOLDER_NAME}, which this module cannot depend on.
     */
    private static final String EXPORTS_FOLDER_NAME = "__EXPORTS";

    /** The cms service. */
    private final CmsService cmsService;

    /** Decides whether the caller may read the requested path. */
    private final DocumentAccessEvaluator accessEvaluator;

    /** The Constant WEB_CACHE. */
    private static final Cache WEB_CACHE = ResourcesCache.getWebCache();

    /** The request. */
    @Autowired
    private HttpServletRequest request;


    /**
     * Instantiates a new cms endpoint.
     *
     * @param cmsService the cms service
     * @param accessEvaluator the per-path access evaluator
     */
    public CmsEndpoint(CmsService cmsService, DocumentAccessEvaluator accessEvaluator) {
        this.cmsService = cmsService;
        this.accessEvaluator = accessEvaluator;
    }

    /**
     * Gets the page.
     *
     * @param path the file path
     * @return the response
     */
    @GetMapping("/{*path}")
    public ResponseEntity get(@PathVariable("path") String path) {
        if (path.trim()
                .isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Listing of web folders is forbidden.");
        }
        assertExportsAccess(path);
        assertReadable(path);
        if (path.trim()
                .endsWith("/")) {
            return getDocumentByPath(path + INDEX_HTML);
        }
        ResponseEntity resourceResponse = getDocumentByPath(path);
        if (!Configuration.isProductiveIFrameEnabled()) {
            resourceResponse.getHeaders()
                            .add("X-Frame-Options", "Deny");
        }
        return resourceResponse;
    }

    /**
     * A database export dump is readable only by the roles entitled to produce one: a full schema dump
     * is an administrative artefact and must not be reachable just because its file name is known. This
     * is a blanket rule, independent of the per-path grants applied by {@link #assertReadable}.
     *
     * @param path the requested CMS path
     */
    private void assertExportsAccess(String path) {
        String normalized = path.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (!normalized.equals(EXPORTS_FOLDER_NAME) && !normalized.startsWith(EXPORTS_FOLDER_NAME + "/")) {
            return; // the folder itself or anything under it - not merely a name starting with it
        }
        if (!request.isUserInRole(Roles.ADMINISTRATOR.getRoleName()) && !request.isUserInRole(Roles.OPERATOR.getRoleName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access to the database exports requires the "
                    + Roles.ADMINISTRATOR.getRoleName() + " or " + Roles.OPERATOR.getRoleName() + " role.");
        }
    }

    /**
     * The per-path role grants apply here exactly as they do to the Documents API.
     * <p>
     * This endpoint reads the same tenant content by path, so without this check a grant restricting a
     * folder would hide it from the Documents user interface while still serving every file under it to
     * any authenticated caller who knows - or guesses - the path.
     *
     * @param path the requested CMS path
     */
    private void assertReadable(String path) {
        if (!accessEvaluator.isReadable(path, request)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access to the requested path is not allowed.");
        }
    }

    /**
     * Gets the document by path.
     *
     * @param path the path
     * @return the document by path
     */
    public ResponseEntity getDocumentByPath(String path) {
        if (isCached(path)) {
            return sendResourceNotModified();
        }

        CmisObject cmisObject;
        try {
            cmisObject = this.cmsService.getObjectByPath(path);
        } catch (IOException e) {
            String errorMessage = "Document not found: " + path;
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
        }
        ObjectType type = cmisObject.getType();
        if (ObjectType.DOCUMENT.equals(type) && cmisObject instanceof CmisDocument) {
            String contentType = ContentTypeHelper.getContentType(ContentTypeHelper.getExtension(path));
            byte[] content;
            try {
                content = this.cmsService.getDocumentContent(cmisObject);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
                String errorMessage = "Document cannot be loaded: " + path;
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
            }
            if (content == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Requested document not found.");
            }
            return sendResource(path, ContentTypeHelper.isBinary(contentType), content, contentType);
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Requested document not found.");
    }

    /**
     * Send resource.
     *
     * @param path the path
     * @param isBinary the is binary
     * @param content the content
     * @param contentType the content type
     * @return the response
     */
    private ResponseEntity sendResource(String path, boolean isBinary, byte[] content, String contentType) {
        String tag = cacheResource(path);
        final HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.valueOf(contentType));
        httpHeaders.add("Cache-Control", "public, must-revalidate, max-age=0");
        httpHeaders.add("ETag", tag);
        if (isBinary) {
            return new ResponseEntity(content, httpHeaders, HttpStatus.OK);
        }
        return new ResponseEntity(new String(content, StandardCharsets.UTF_8), httpHeaders, HttpStatus.OK);
    }

    /**
     * Send resource not modified.
     *
     * @return the response
     */
    private ResponseEntity sendResourceNotModified() {
        final HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("ETag", getTag());
        return new ResponseEntity(httpHeaders, HttpStatus.NOT_MODIFIED);
    }

    /**
     * Cache resource.
     *
     * @param path the path
     * @return the string
     */
    private String cacheResource(String path) {
        String tag = WEB_CACHE.generateTag();
        WEB_CACHE.setTag(path, tag);
        return tag;
    }

    /**
     * Checks if is cached.
     *
     * @param path the path
     * @return true, if is cached
     */
    private boolean isCached(String path) {
        String tag = getTag();
        String cachedTag = WEB_CACHE.getTag(path);
        return tag != null && tag.equals(cachedTag);

    }

    /**
     * Gets the tag.
     *
     * @return the tag
     */
    private String getTag() {
        return request.getHeader("If-None-Match");
    }

}
