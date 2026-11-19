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

import java.util.Map;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.webmvc.error.DefaultErrorAttributes;
import org.springframework.boot.webmvc.error.ErrorAttributes;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;

import jakarta.servlet.DispatcherType;

/**
 * Wires the request id (#7948): the filter, ahead of every other one including the security chain,
 * and the error attributes that put the id into Spring Boot's error body.
 */
@Configuration
class RequestIdConfig {

    /**
     * Registers the filter on every path and dispatch.
     *
     * @return the registration
     */
    @Bean
    FilterRegistrationBean<RequestIdFilter> requestIdFilterRegistrationBean() {
        FilterRegistrationBean<RequestIdFilter> registration = new FilterRegistrationBean<>(new RequestIdFilter());
        registration.addUrlPatterns("/*");
        registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    /**
     * Registers the {@code Accept-Language} guard right behind the request id, so its 400 carries the
     * id, and ahead of Spring's {@code RequestContextFilter} and the security chain, which read the
     * locale. Only the request dispatch: a later dispatch of the same request has its locales parsed.
     *
     * @return the registration
     */
    @Bean
    FilterRegistrationBean<AcceptLanguageFilter> acceptLanguageFilterRegistrationBean() {
        FilterRegistrationBean<AcceptLanguageFilter> registration = new FilterRegistrationBean<>(new AcceptLanguageFilter());
        registration.addUrlPatterns("/*");
        registration.setDispatcherTypes(DispatcherType.REQUEST);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return registration;
    }

    /**
     * Spring Boot's error attributes plus {@value RequestIds#MDC_KEY}. Declaring the bean replaces the
     * default one, which backs off on any {@link ErrorAttributes}.
     *
     * @return the error attributes
     */
    @Bean
    ErrorAttributes requestIdErrorAttributes() {
        return new DefaultErrorAttributes() {

            @Override
            public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
                Map<String, Object> attributes = super.getErrorAttributes(webRequest, options);
                if (webRequest.getAttribute(RequestIds.ATTRIBUTE, RequestAttributes.SCOPE_REQUEST) instanceof String requestId) {
                    attributes.put(RequestIds.MDC_KEY, requestId);
                }
                return attributes;
            }
        };
    }
}
