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

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.dirigible.components.tenants.tenant.TenantContextInitFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;

/**
 * Refuses to start on a security chain that selects the tenant of a request before the tenant scope
 * of the request is open.
 *
 * <p>
 * {@link TenantSelectionFilter} enters the tenant a request names and runs the rest of the chain in
 * it. That holds only while {@link TenantContextInitFilter} runs ahead of it in the same chain. Run
 * anywhere later - as the servlet filter Spring Boot registers for the component, which comes after
 * the whole security chain - it opens the scope again from the request alone, which is the default
 * tenant for every bearer request: an integration client, or a user's ID token, is then served from
 * the default tenant with the roles of the tenant it named (#8458). A chain built that way must not
 * serve traffic, so the context refresh fails, as {@code TenantResolutionConfigValidator} fails it
 * on a configuration that cannot resolve tenants.
 *
 * <p>
 * It runs once every singleton exists, so it reads the chains as they were built rather than
 * creating them early. A chain without the selection filter - every chain outside
 * {@code TOKEN_GROUPS} - is not its concern.
 */
@Component
public class TenantSelectionChainValidator implements SmartInitializingSingleton {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(TenantSelectionChainValidator.class);

    private final ObjectProvider<SecurityFilterChain> securityFilterChains;

    /**
     * Instantiates a new tenant selection chain validator.
     *
     * @param securityFilterChains the security chains of the deployment
     */
    public TenantSelectionChainValidator(ObjectProvider<SecurityFilterChain> securityFilterChains) {
        this.securityFilterChains = securityFilterChains;
    }

    /**
     * Validates every security chain of the deployment.
     *
     * @throws IllegalStateException if a chain selects a tenant before the tenant scope is open
     */
    @Override
    public void afterSingletonsInstantiated() {
        securityFilterChains.orderedStream()
                            .forEach(TenantSelectionChainValidator::validate);
    }

    /**
     * Validates one security chain.
     *
     * @param chain the chain
     * @throws IllegalStateException if the chain runs the tenant selection without the tenant context
     *         init filter ahead of it
     */
    static void validate(SecurityFilterChain chain) {
        List<Filter> filters = chain.getFilters();
        int selection = indexOf(filters, TenantSelectionFilter.class);
        if (selection < 0) {
            return;
        }
        int scope = indexOf(filters, TenantContextInitFilter.class);
        if (scope < 0 || scope > selection) {
            String message = "The security chain " + namesOf(filters) + " runs [" + TenantSelectionFilter.class.getSimpleName()
                    + "] without [" + TenantContextInitFilter.class.getSimpleName()
                    + "] ahead of it: the tenant scope would be opened again after the selection, and a bearer request naming its"
                    + " tenant would be served from the default tenant. Add the tenant context init filter to the chain ahead of"
                    + " its authentication filters.";
            LOGGER.error(message);
            throw new IllegalStateException(message);
        }
    }

    private static int indexOf(List<Filter> filters, Class<? extends Filter> type) {
        for (int i = 0; i < filters.size(); i++) {
            if (type.isInstance(filters.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private static String namesOf(List<Filter> filters) {
        return filters.stream()
                      .map(filter -> filter.getClass()
                                           .getSimpleName())
                      .collect(Collectors.joining(", ", "[", "]"));
    }
}
