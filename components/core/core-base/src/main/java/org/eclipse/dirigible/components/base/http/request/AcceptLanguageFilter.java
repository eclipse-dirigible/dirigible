/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.http.request;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Answers 400 to a request whose {@code Accept-Language} the container refuses to parse (#8709).
 *
 * <p>
 * Tomcat refuses a header listing more locales than its connector's {@code maxLocaleCount} (10 by
 * default) with an {@link IllegalArgumentException} from {@code getLocale()}. Spring's
 * {@code RequestContextFilter} asks for the locale on every request, so without this filter the
 * exception escaped every handler as an unhandled 500 logged at ERROR - on any path, anonymous ones
 * included, at the will of any client. The locales are parsed here first, ahead of that filter and
 * the security chain, and a refused header ends the request with a 400 written directly: an error
 * dispatch would run {@code RequestContextFilter} again.
 */
class AcceptLanguageFilter extends HttpFilter {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(AcceptLanguageFilter.class);

    static final String MESSAGE = "The Accept-Language header lists more languages than the server accepts";

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            request.getLocales();
        } catch (IllegalArgumentException e) {
            // DEBUG, not WARN: any client can send this header, and a log line per request is theirs to
            // trigger at will
            LOGGER.debug("Refused the Accept-Language header of [{} {}]", request.getMethod(), request.getRequestURI(), e);
            refuse(response);
            return;
        }
        chain.doFilter(request, response);
    }

    private static void refuse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter()
                .write("{\"status\":400,\"error\":\"Bad Request\",\"message\":\"" + MESSAGE + "\"}");
    }
}
