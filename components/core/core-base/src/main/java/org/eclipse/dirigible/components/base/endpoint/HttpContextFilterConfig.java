/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.endpoint;

import org.eclipse.dirigible.components.base.http.uri.HostedEngineUris;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * The Class HttpContextFilterFilterConfig.
 */
@Configuration
public class HttpContextFilterConfig {

    /**
     * Security filter registration bean. The base patterns are extended with any URL patterns
     * contributed by hosted engines (e.g. the externalized OData engine) via {@link HostedEngineUris}.
     *
     * @param httpContextFilter the security filter
     * @param hostedEngineUris the hosted engine URI contributions
     * @return the filter registration bean
     */
    @Bean
    public FilterRegistrationBean<HttpContextFilter> httpContextFilterRegistrationBean(HttpContextFilter httpContextFilter,
            List<HostedEngineUris> hostedEngineUris) {
        FilterRegistrationBean<HttpContextFilter> filterRegistrationBean = new FilterRegistrationBean<>(httpContextFilter);

        filterRegistrationBean.setFilter(httpContextFilter);
        filterRegistrationBean.addUrlPatterns("/services/*", "/public/*");
        for (HostedEngineUris engineUris : hostedEngineUris) {
            engineUris.filterUrlPatterns()
                      .forEach(filterRegistrationBean::addUrlPatterns);
        }

        return filterRegistrationBean;
    }
}
