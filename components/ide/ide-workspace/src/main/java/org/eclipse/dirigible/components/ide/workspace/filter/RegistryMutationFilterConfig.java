/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.workspace.filter;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the {@link RegistryMutationFilter} on the endpoints that publish to - or unpublish from
 * - the registry. Registering it explicitly also keeps Spring Boot from mapping it to every
 * request: a write through a generated application's REST controller is not a registry mutation.
 */
@Configuration
class RegistryMutationFilterConfig {

    /** The publisher and workspace endpoints - the client paths that write to the registry. */
    private static final String[] URL_PATTERNS = { //
            "/services/ide/publisher/*", //
            "/services/ide/workspace/*", //
            "/services/ide/workspaces/*"};

    @Bean
    FilterRegistrationBean<RegistryMutationFilter> registryMutationFilterRegistrationBean(RegistryMutationFilter filter) {
        FilterRegistrationBean<RegistryMutationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns(URL_PATTERNS);
        return registration;
    }

}
