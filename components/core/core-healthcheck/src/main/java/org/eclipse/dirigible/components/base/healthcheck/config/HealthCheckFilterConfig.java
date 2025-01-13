/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.healthcheck.config;

import org.eclipse.dirigible.components.base.healthcheck.filter.HealthCheckFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The Class HealthCheckFilterConfig.
 */
@Configuration
public class HealthCheckFilterConfig {

    /**
     * Health check filter filter registration bean.
     *
     * @param healthCheckFilter the health check filter
     * @return the filter registration bean
     */
    @Bean
    public FilterRegistrationBean<HealthCheckFilter> healthCheckFilterRegistrationBean(HealthCheckFilter healthCheckFilter) {
        FilterRegistrationBean<HealthCheckFilter> filterRegistrationBean = new FilterRegistrationBean<>(healthCheckFilter);

        filterRegistrationBean.setFilter(healthCheckFilter);
        filterRegistrationBean.addUrlPatterns("/services/*", "/public/*");

        return filterRegistrationBean;
    }

}
