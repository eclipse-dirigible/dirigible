/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.oauth2.tenant;

import org.eclipse.dirigible.components.base.http.access.CustomSecurityConfigurator;
import org.eclipse.dirigible.components.base.tenant.TenantResolutionStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.stereotype.Component;

/**
 * Puts the tenant selection into whichever security chain the deployment runs.
 *
 * <p>
 * The filter has to run <em>before</em> authorization: a user who has not selected a tenant yet has
 * no tenant roles, so authorization would answer 403 before they ever saw the picker. Registering
 * it here rather than in each profile's chain builder is what makes the placement deterministic -
 * the chains apply the custom configurators as their last step, so the filter lands after the
 * session revalidation the OIDC profiles install (the refreshed authorities have to exist before
 * they are repaired) and the same wiring serves every profile.
 *
 * <p>
 * The picker page is claimed as authenticated here too, ahead of the platform's own URL matrix: it
 * is for a user who is logged in but has no tenant, so it can be neither public nor role gated.
 */
@Component
public class TenantSelectionSecurityConfigurator implements CustomSecurityConfigurator {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(TenantSelectionSecurityConfigurator.class);

    private final TenantSelectionFilter tenantSelectionFilter;

    /**
     * Instantiates a new tenant selection security configurator.
     *
     * @param tenantSelectionFilter the tenant selection filter
     */
    public TenantSelectionSecurityConfigurator(TenantSelectionFilter tenantSelectionFilter) {
        this.tenantSelectionFilter = tenantSelectionFilter;
    }

    /**
     * Configure.
     *
     * @param http the http
     * @throws Exception the exception
     */
    @Override
    public void configure(HttpSecurity http) throws Exception {
        if (TenantResolutionStrategy.TOKEN_GROUPS != TenantResolutionStrategy.fromConfiguration()) {
            return;
        }
        LOGGER.info("Tenants are selected by the user. Registering the tenant selection filter and the picker page [{}].",
                TenantSelectionFilter.TENANT_SELECTION_PAGE);
        http.authorizeHttpRequests(authz -> authz.requestMatchers(TenantSelectionFilter.TENANT_SELECTION_PAGE)
                                                 .authenticated())
            .addFilterBefore(tenantSelectionFilter, AuthorizationFilter.class);
    }
}
