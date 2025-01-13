/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.endpoint;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.eclipse.dirigible.components.base.context.ContextException;
import org.eclipse.dirigible.components.base.context.ThreadContextFacade;
import org.springframework.stereotype.Component;

/**
 * The HTTP Context Filter.
 */
@Component
public class HttpContextFilter implements Filter {

    /**
     * Inits the.
     *
     * @param filterConfig the filter config
     * @throws ServletException the servlet exception
     */
    /*
     * (non-Javadoc)
     *
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Not used
    }

    /**
     * Do filter.
     *
     * @param request the request
     * @param response the response
     * @param chain the chain
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws ServletException the servlet exception
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            ThreadContextFacade.setUp();
            try {
                ThreadContextFacade.set(HttpServletRequest.class.getCanonicalName(), request);
                ThreadContextFacade.set(HttpServletResponse.class.getCanonicalName(), response);

                chain.doFilter(request, response);
            } finally {
                ThreadContextFacade.tearDown();
            }
        } catch (ContextException e) {
            throw new ServletException(e);
        }
    }

    /**
     * Destroy.
     */
    @Override
    public void destroy() {
        // Not used
    }

}
