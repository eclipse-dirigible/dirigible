/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.provisioning.external;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.base.http.access.CustomSecurityConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Conditional;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.stereotype.Component;

/**
 * Claims {@code /services/tenant-provisioning/**} for the roles that may provision tenants.
 *
 * <p>
 * Registering the rule here rather than in the platform's URL matrix is what makes it apply to
 * every security chain a deployment may run - basic authentication, Keycloak, Cognito - from one
 * bean. Custom configurators are applied before that matrix, and the first matching rule wins, so
 * this one is authoritative for the prefix.
 */
@Component
@Conditional(TenantProvisioningApiEnabledCondition.class)
class TenantProvisioningSecurityConfigurator implements CustomSecurityConfigurator {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(TenantProvisioningSecurityConfigurator.class);

    /** The Constant PATTERN. */
    private static final String PATTERN = "/" + BaseEndpoint.PREFIX_ENDPOINT_TENANT_PROVISIONING + "**";

    /**
     * Configure.
     *
     * @param http the http
     * @throws Exception the exception
     */
    @Override
    public void configure(HttpSecurity http) throws Exception {
        LOGGER.info("The tenant provisioning API is enabled. Claiming [{}] for roles [{}, {}, {}].", PATTERN,
                TenantProvisioningRoles.TENANT_PROVISIONER, TenantProvisioningRoles.ADMINISTRATOR, TenantProvisioningRoles.OPERATOR);
        http.authorizeHttpRequests(authz -> authz.requestMatchers(PATTERN)
                                                 .hasAnyRole(TenantProvisioningRoles.TENANT_PROVISIONER,
                                                         TenantProvisioningRoles.ADMINISTRATOR, TenantProvisioningRoles.OPERATOR));
    }
}
