/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.http.access;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.stereotype.Component;

/**
 * Enables CORS on whichever security chain the deployment runs, once origins are configured.
 *
 * <p>
 * Every chain applies the custom configurators before its URL matrix, so one bean reaches the
 * basic, snowflake and OAuth2 login chains alike without a per-profile edit. The chains that always
 * had CORS (basic, snowflake) get the same source their own bean returns; the OAuth2 chains gain
 * it. An unconfigured deployment changes nothing here: a CORS filter without a matching
 * configuration answers every preflight with 403, which is not what a chain without CORS did.
 *
 * <p>
 * Inside the chain the CORS filter runs ahead of authorization, so a preflight from a configured
 * origin is answered before the login redirect or the 401 would fire - and ahead of the servlet
 * filter enforcing the {@code .access} constraints, which never sees a preflight.
 */
@Component
class CorsSecurityConfigurator implements CustomSecurityConfigurator {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(CorsSecurityConfigurator.class);

    /**
     * Configure.
     *
     * @param http the http
     * @throws Exception the exception
     */
    @Override
    public void configure(HttpSecurity http) throws Exception {
        if (!CorsConfigurationSourceProvider.isConfigured()) {
            LOGGER.debug("No cross-origin origins are configured in [{}].", DirigibleConfig.CORS_ALLOWED_ORIGINS.getKey());
            return;
        }
        LOGGER.info("Cross-origin requests are accepted from {}.", CorsConfigurationSourceProvider.allowedOriginPatterns());
        http.cors(cors -> cors.configurationSource(CorsConfigurationSourceProvider.get()));
    }
}
