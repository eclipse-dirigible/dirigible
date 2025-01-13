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

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The Class HttpContextFilterFilterConfig.
 */
@Configuration
public class HttpContextFilterConfig {

    /**
     * Security filter registration bean.
     *
     * @param httpContextFilter the security filter
     * @return the filter registration bean
     */
    @Bean
    public FilterRegistrationBean<HttpContextFilter> httpContextFilterRegistrationBean(HttpContextFilter httpContextFilter) {
        FilterRegistrationBean<HttpContextFilter> filterRegistrationBean = new FilterRegistrationBean<>(httpContextFilter);

        filterRegistrationBean.setFilter(httpContextFilter);
        filterRegistrationBean.addUrlPatterns("/services/*", "/public/*", "/odata/v2/*");

        return filterRegistrationBean;
    }
}
