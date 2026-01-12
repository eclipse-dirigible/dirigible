/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
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
import org.eclipse.dirigible.components.engine.cms.CmisDocument;
import org.eclipse.dirigible.components.engine.cms.CmisObject;
import org.eclipse.dirigible.components.engine.cms.CmisSessionFactory;
import org.eclipse.dirigible.components.engine.cms.ObjectType;
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
 */
@RestController
@RequestMapping({BaseEndpoint.PREFIX_ENDPOINT_SECURED + "cms", BaseEndpoint.PREFIX_ENDPOINT_PUBLIC + "cms"})
public class CmsEndpoint extends BaseEndpoint {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(CmsEndpoint.class);

    /** The Constant INDEX_HTML. */
    private static final String INDEX_HTML = "index.html";

    /** The cms service. */
    private final CmsService cmsService;

    /** The Constant WEB_CACHE. */
    private static final Cache WEB_CACHE = ResourcesCache.getWebCache();

    /** The request. */
    @Autowired
    private HttpServletRequest request;


    /**
     * Instantiates a new cms endpoint.
     *
     * @param cmsService the cms service
     */
    @Autowired
    public CmsEndpoint(CmsService cmsService) {
        this.cmsService = cmsService;
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
        } else if (path.trim()
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
