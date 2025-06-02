/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.cms.filter;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.dirigible.components.engine.cms.service.CmsService;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * The Client Side Routing Filter.
 */
@Component
public class CmsClientSideRoutingFilter implements Filter {

    /**
     * The Constant logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(CmsClientSideRoutingFilter.class);

    final String TEXT_HTML_WITH_UTF8_CHARSET = MediaType.TEXT_HTML_VALUE + ";charset=" + java.nio.charset.StandardCharsets.UTF_8;

    /**
     * The Constant VALID_PREFIXES.
     */
    private static final Set<String> VALID_PREFIXES = new HashSet<>();

    /** The CMS service. */
    private final CmsService cmsService;

    /**
     * Instantiates a new cms client-side routing filter.
     *
     * @param cmsService the cms service
     */
    @Autowired
    public CmsClientSideRoutingFilter(CmsService cmsService) {
        this.cmsService = cmsService;
    }

    /**
     * Inits the cms client-side routing filter.
     *
     * @param filterConfig the filter config
     */
    @Override
    public void init(FilterConfig filterConfig) {
        VALID_PREFIXES.add("/services/cms");
    }

    /**
     * Do filter.
     *
     * @param request the request
     * @param response the response
     * @param chain the chain
     * @throws ServletException the servlet exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {

        HttpServletRequest httpServletRequest = (HttpServletRequest) request;

        String path = (httpServletRequest.getServletPath() == null || httpServletRequest.getServletPath()
                                                                                        .isEmpty()) ? IRepositoryStructure.SEPARATOR
                                                                                                : httpServletRequest.getServletPath();

        for (String prefix : VALID_PREFIXES) {
            if (path.startsWith(prefix)) {
                path = path.substring(prefix.length());
                String method = httpServletRequest.getMethod();
                if ("GET".equalsIgnoreCase(method)) {
                    if (!cmsService.existDocument(path)) {
                        if (cmsService.existDocument(path + ".html")) {
                            response.setContentType(TEXT_HTML_WITH_UTF8_CHARSET);
                            final byte[] indexData = cmsService.getDocument(path + ".html");
                            response.setContentLength(indexData.length);
                            response.getOutputStream()
                                    .write(indexData);
                            response.getOutputStream()
                                    .flush();
                            return;
                        }
                    }
                }
            }
        }

        chain.doFilter(request, response);
    }

    /**
     * Destroy.
     */
    @Override
    public void destroy() {
        // Not Used
    }

}
