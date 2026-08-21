/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.tenant;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.commons.config.InvalidConfigException;

/**
 * How the current tenant is resolved for an incoming request.
 *
 * <p>
 * The strategy is a deployment-wide decision, configured through
 * {@link DirigibleConfig#TENANT_RESOLUTION_STRATEGY}. It lives in core-base because both the
 * resolution itself (core-tenants) and the identity providers that feed it (security-cognito,
 * security-keycloak) have to agree on it.
 */
public enum TenantResolutionStrategy {

    /**
     * The tenant is the subdomain of the request host, matched against
     * {@link DirigibleConfig#TENANT_SUBDOMAIN_REGEX}. Every tenant needs its own host.
     */
    SUBDOMAIN,

    /**
     * The tenant is the one the user selected, validated against the identity provider groups named
     * {@code <tenantId>.<appId>.<role>}. One host serves every tenant of the application.
     */
    TOKEN_GROUPS;

    /**
     * Resolves the configured strategy.
     *
     * @return the configured strategy, {@link #SUBDOMAIN} when nothing is configured
     * @throws InvalidConfigException if the configured value is not a known strategy
     */
    public static TenantResolutionStrategy fromConfiguration() {
        String configuredValue = DirigibleConfig.TENANT_RESOLUTION_STRATEGY.getStringValue();
        return parse(configuredValue);
    }

    /**
     * Parses a strategy name, tolerating surrounding whitespace and any letter case.
     *
     * @param value the configured value
     * @return the matching strategy, {@link #SUBDOMAIN} for a blank value
     * @throws InvalidConfigException if the value is not a known strategy
     */
    static TenantResolutionStrategy parse(String value) {
        if (value == null || value.isBlank()) {
            return SUBDOMAIN;
        }
        String normalizedValue = value.trim()
                                      .toUpperCase();
        for (TenantResolutionStrategy strategy : values()) {
            if (strategy.name()
                        .equals(normalizedValue)) {
                return strategy;
            }
        }
        throw new InvalidConfigException(
                "Unknown tenant resolution strategy [" + value + "] configured in [" + DirigibleConfig.TENANT_RESOLUTION_STRATEGY.getKey()
                        + "]. Supported values: [" + SUBDOMAIN + ", " + TOKEN_GROUPS + "]",
                DirigibleConfig.TENANT_RESOLUTION_STRATEGY.getKey());
    }
}
