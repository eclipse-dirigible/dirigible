/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.camel.config;

import org.apache.camel.CamelContext;
import org.apache.camel.component.platform.http.PlatformHttpComponent;
import org.apache.camel.component.platform.http.spi.PlatformHttpEngine;
import org.apache.camel.component.platform.http.springboot.CamelRequestHandlerMapping;
import org.apache.camel.spring.boot.SpringBootCamelContext;
import org.eclipse.dirigible.components.engine.camel.processor.CamelDirigibleRequestHandlerMapping;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * The Class CamelDirigibleConfiguration.
 */
@Configuration
class CamelDirigibleConfiguration {

    /**
     * Creates the camel request handler mapping.
     *
     * @param camelContext the camel context
     * @param httpEngine the http engine
     * @param camelRequestHandlerMapping the camel request handler mapping
     * @return the camel request handler mapping
     */
    @Bean
    @Primary
    public CamelRequestHandlerMapping createCamelRequestHandlerMapping(CamelContext camelContext, PlatformHttpEngine httpEngine,
            CamelRequestHandlerMapping camelRequestHandlerMapping) {
        var httpComponent = camelContext.getComponent("platform-http", PlatformHttpComponent.class);
        httpComponent.removePlatformHttpListener(camelRequestHandlerMapping);
        return new CamelDirigibleRequestHandlerMapping(httpComponent, httpEngine);
    }

    /**
     * Creates the spring boot camel context.
     *
     * @param applicationContext the application context
     * @return the spring boot camel context
     */
    @Bean
    SpringBootCamelContext createSpringBootCamelContext(ApplicationContext applicationContext) {
        return new SpringBootCamelContext(applicationContext, true);
    }

}
