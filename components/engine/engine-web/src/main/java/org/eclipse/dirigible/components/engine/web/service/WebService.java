/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.web.service;

import jakarta.servlet.http.HttpServletRequest;
import org.eclipse.dirigible.commons.api.helpers.ContentTypeHelper;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.ResourcesCache;
import org.eclipse.dirigible.commons.config.ResourcesCache.Cache;
import org.eclipse.dirigible.components.engine.web.exposure.ExposeManager;
import org.eclipse.dirigible.components.registry.accessor.RegistryAccessor;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.repository.api.RepositoryNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;

/**
 * The Class WebService.
 */
@Service
@RequestScope
public class WebService {

    /** The Constant WEB_CACHE. */
    private static final Cache WEB_CACHE = ResourcesCache.getWebCache();

    /** The Constant INDEX_HTML. */
    private static final String INDEX_HTML = "index.html";

    /** The request. */
    @Autowired
    private HttpServletRequest request;

    /** The registry accessor. */
    @Autowired
    private RegistryAccessor registryAccessor;

    /**
     * Gets the resource.
     *
     * @param path the path
     * @return the resource
     */
    public ResponseEntity getResource(@PathVariable("path") String path) {
        if (ExposeManager.isPathExposed(path)) {
            if (path.trim()
                    .isEmpty()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Listing of web folders is forbidden.");
            } else if (path.trim()
                           .endsWith(IRepositoryStructure.SEPARATOR)) {
                return getResourceByPath(path + INDEX_HTML);
            }
            ResponseEntity resourceResponse = getResourceByPath(path);
            if (!Configuration.isProductiveIFrameEnabled()) {
                resourceResponse.getHeaders()
                                .add("X-Frame-Options", "Deny");
            }
            return resourceResponse;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Requested resource is not exposed.");
    }

    /**
     * Gets the resource by path.
     *
     * @param path the path
     * @return the resource by path
     */
    private ResponseEntity getResourceByPath(String path) {
        if (isCached(path)) {
            return sendResourceNotModified();
        }

        if (registryAccessor.existResource(path)) {
            IResource resource = registryAccessor.getResource(path);
            if (resource == null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Resource requested is not exposed.");
            }
            String contentType = resource.getContentType();
            return sendResource(path, resource.isBinary(), resource.getContent(), contentType);
        }

        String errorMessage = "Resource not found: " + path;
        try {
            byte[] content = registryAccessor.getRegistryContent(path);
            if (content != null) {
                String contentType = ContentTypeHelper.getContentType(ContentTypeHelper.getExtension(path));
                return sendResource(path, ContentTypeHelper.isBinary(contentType), content, contentType);
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Requested resource not found.");
            }
        } catch (RepositoryNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
        }
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
