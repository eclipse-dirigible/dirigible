/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.oauth2.login;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.HtmlUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Shows the provider's sentence on Spring Security's generated login page in place of its fixed
 * {@code Invalid credentials}, which the page offers no way to replace: the page is rendered into a
 * buffer and its error alert is swapped for one carrying the escaped sentence. When there is no
 * sentence, or the page no longer renders that alert, the page goes out unchanged.
 *
 * <p>
 * Registered only on a chain without a custom login page ({@code DIRIGIBLE_SECURITY_LOGIN_PAGE}),
 * which reads the failure through {@link OAuth2LoginFailureEndpoint} instead.
 */
class GeneratedLoginPageFailureFilter extends OncePerRequestFilter {

    /** The error alert the generated page renders for every failure. */
    static final String GENERIC_ALERT = "<div class=\"alert alert-danger\" role=\"alert\">Invalid credentials</div>";

    private static final String FAILURE_PAGE = "/login";
    private static final String FAILURE_QUERY = "error";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"GET".equals(request.getMethod()) || !FAILURE_QUERY.equals(request.getQueryString())
                || !(request.getContextPath() + FAILURE_PAGE).equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Optional<String> message = OAuth2LoginFailure.take(request)
                                                     .map(OAuth2LoginFailure::description);
        if (message.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }
        ContentCachingResponseWrapper page = new ContentCachingResponseWrapper(response);
        filterChain.doFilter(request, page);
        String html = new String(page.getContentAsByteArray(), StandardCharsets.UTF_8);
        String alert = "<div class=\"alert alert-danger\" role=\"alert\">"
                + HtmlUtils.htmlEscape(message.get(), StandardCharsets.UTF_8.name()) + "</div>";
        byte[] body = html.replace(GENERIC_ALERT, alert)
                          .getBytes(StandardCharsets.UTF_8);
        response.setContentLength(body.length);
        response.getOutputStream()
                .write(body);
    }
}
