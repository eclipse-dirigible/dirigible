/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.filter;

import com.google.common.html.HtmlEscapers;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.base.http.access.UserRequestVerifier;
import org.eclipse.dirigible.components.security.domain.Access;
import org.eclipse.dirigible.components.security.verifier.AccessVerifier;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.Principal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The Security Filter.
 */
@Component
public class SecurityFilter implements Filter {

    /**
     * The Constant CONSTRAINT_SCOPE_HTTP.
     */
    public static final String CONSTRAINT_SCOPE_HTTP = "HTTP";
    /**
     * The Constant ROLE_PUBLIC.
     */
    public static final String ROLE_PUBLIC = "Public";
    /**
     * The Constant logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(SecurityFilter.class);
    /**
     * The Constant PATH_WEB_RESOURCES.
     */
    private static final String PATH_WEB_RESOURCES = "/web/resources";
    /**
     * The Constant SECURED_PREFIXES.
     */
    private static final Set<String> SECURED_PREFIXES = new HashSet<>();

    /** The security access verifier. */
    private final AccessVerifier securityAccessVerifier;

    /**
     * Instantiates a new security filter.
     *
     * @param securityAccessVerifier the security access verifier
     */
    @Autowired
    public SecurityFilter(AccessVerifier securityAccessVerifier) {
        this.securityAccessVerifier = securityAccessVerifier;
    }

    /**
     * Inits the security filter.
     *
     * @param filterConfig the filter config
     */
    @Override
    public void init(FilterConfig filterConfig) {
        SECURED_PREFIXES.add("/js");
        SECURED_PREFIXES.add("/public");
        SECURED_PREFIXES.add("/web");
        SECURED_PREFIXES.add("/wiki");
        SECURED_PREFIXES.add("/command");
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
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        String path =
                !"".equals(httpServletRequest.getServletPath()) ? httpServletRequest.getServletPath() : IRepositoryStructure.SEPARATOR;
        if (!path.startsWith(PATH_WEB_RESOURCES)) {
            for (String prefix : SECURED_PREFIXES) {
                if (path.startsWith(prefix)) {
                    path = path.substring(prefix.length());
                    break;
                }
            }
            String method = httpServletRequest.getMethod();

            boolean isInRole = false;
            Principal principal = httpServletRequest.getUserPrincipal();

            List<Access> securityAccesses = securityAccessVerifier.getMatchingSecurityAccesses(CONSTRAINT_SCOPE_HTTP, path, method);
            if (!securityAccesses.isEmpty()) {

                if (principal == null && !Configuration.isJwtModeEnabled()) {
                    // white list check
                    for (Access securityAccess : securityAccesses) {
                        if (ROLE_PUBLIC.equalsIgnoreCase(securityAccess.getRole())) {
                            isInRole = true;
                            break;
                        }
                    }

                    if (!isInRole) {
                        forbidden(path, "No logged in user", httpServletResponse);
                        return;
                    }
                } else {
                    for (Access securityAccess : securityAccesses) {
                        if (ROLE_PUBLIC.equalsIgnoreCase(securityAccess.getRole())
                                || UserRequestVerifier.isUserInRole(httpServletRequest, securityAccess.getRole())) {
                            isInRole = true;
                            break;
                        }
                    }
                    if (!isInRole) {
                        forbidden(path, "The logged in user does not have any of the required roles for the requested" + " URI",
                                httpServletResponse);
                        return;
                    }
                }
            } else {
                if (!Configuration.isAnonymousModeEnabled() && principal == null && !Configuration.isJwtModeEnabled()) {
                    forbidden(path, "No logged in user and no white list constraints", httpServletResponse);
                    return;
                }
            }
        }

        chain.doFilter(request, response);
    }

    /**
     * Forbidden.
     *
     * @param uri the uri
     * @param message the message
     * @param response the response
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private void forbidden(String uri, String message, HttpServletResponse response) throws IOException {
        String error = String.format("Requested URI [%s] is forbidden: %s", uri, message);
        if (logger.isWarnEnabled()) {
            logger.warn(error);
        }
        response.sendError(HttpServletResponse.SC_FORBIDDEN, HtmlEscapers.htmlEscaper()
                                                                         .escape(error));
    }

    /**
     * Destroy.
     */
    @Override
    public void destroy() {
        // Not Used
    }

}
