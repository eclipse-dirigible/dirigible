/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.tenant;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.tenants.service.TenantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Class TenantContextInitFilter.
 */
@Component
public class TenantContextInitFilter extends OncePerRequestFilter {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(TenantContextInitFilter.class);

    private static final List<String> HOST_HEADERS = List.of("host", "x-forwarded-host");

    /** The Constant TENANT_SUBDOMAIN_PATTERN. */
    private static final Pattern TENANT_SUBDOMAIN_PATTERN = Pattern.compile(DirigibleConfig.TENANT_SUBDOMAIN_REGEX.getStringValue());

    /** The Constant tenantCache. */
    private static final Cache<String, Optional<Tenant>> tenantCache = Caffeine.newBuilder()
                                                                               .expireAfterWrite(10, TimeUnit.MINUTES)
                                                                               .maximumSize(100)
                                                                               .build();

    /** The tenant service. */
    private final TenantService tenantService;

    /** The tenant context. */
    private final TenantContext tenantContext;
    private final boolean multitenantModeEnabled;
    private final Gson gson;

    /**
     * Instantiates a new tenant context init filter.
     *
     * @param tenantService the tenant service
     * @param tenantContext the tenant context
     */
    public TenantContextInitFilter(TenantService tenantService, TenantContext tenantContext) {
        this.tenantService = tenantService;
        this.tenantContext = tenantContext;
        this.multitenantModeEnabled = DirigibleConfig.MULTI_TENANT_MODE_ENABLED.getBooleanValue();
        this.gson = new GsonBuilder().serializeNulls()
                                     .create();
    }

    /**
     * Do filter internal.
     *
     * @param request the request
     * @param response the response
     * @param chain the chain
     * @throws ServletException the servlet exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Optional<Tenant> currentTenant = determineTenantSubdomain(request);
        if (currentTenant.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "There is no registered tenant for the current host");
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Tried to reach unregistered tenant. Headers: [{}]", getHeaders(request));
            }
            return;
        }

        try {
            tenantContext.execute(currentTenant.get(), () -> {
                chain.doFilter(request, response);
                return null;
            });

        } catch (ServletException | IOException | RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServletException(ex.getMessage(), ex);
        }
    }

    /**
     * Determine current tenant.
     *
     * @param request the request
     * @return the optional
     */
    private Optional<Tenant> determineTenantSubdomain(HttpServletRequest request) {
        if (!multitenantModeEnabled) {
            LOGGER.debug("The app is in single tenant mode. Will return the default tenant.");
            return Optional.of(TenantImpl.getDefaultTenant());
        }
        List<Optional<String>> subdomains = HOST_HEADERS.stream()
                                                        .map(hostHeader -> determineTenantSubdomain(request, hostHeader))
                                                        .toList();
        if (areEmpty(subdomains)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Tenant subdomain cannot be extracted from the current request. Will return the default tenant. Headers: [{}]",
                        getHeaders(request));
            }
            return Optional.of(TenantImpl.getDefaultTenant());
        }

        for (Optional<String> subdomain : subdomains) {
            Optional<Tenant> tenant = tenantBySubdomain(subdomain);
            if (tenant.isPresent()) {
                LOGGER.debug("Found registered tenant [{}] for subdomain [{}].", tenant.get(), subdomain.get());
                return tenant;
            }
        }
        return Optional.empty();
    }

    private String getHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        HOST_HEADERS.forEach(h -> headers.put(h, request.getHeader(h)));
        return gson.toJson(headers);
    }

    private boolean areEmpty(List<Optional<String>> optionals) {
        for (Optional<?> optional : optionals) {
            if (optional.isPresent()) {
                return false;
            }
        }
        return true;
    }

    private Optional<String> determineTenantSubdomain(HttpServletRequest request, String hostHeader) {
        String headerValue = request.getHeader(hostHeader);
        if (null == headerValue) {
            LOGGER.debug("Cannot extract subdomain from header [{}] with null value.", hostHeader);
            return Optional.empty();
        }
        Matcher matcher = TENANT_SUBDOMAIN_PATTERN.matcher(headerValue);
        if (matcher.find()) {
            String tenantSubdomain = matcher.group(1);
            LOGGER.debug("Host [{}] from header [{}] MATCHES tenant subdomain pattern [{}]. Extracted subdomain: [{}]", headerValue,
                    hostHeader, TENANT_SUBDOMAIN_PATTERN.pattern(), tenantSubdomain);

            return Optional.of(tenantSubdomain);
        }
        LOGGER.debug("Host [{}] from header [{}] does NOT match tenant subdomain pattern [{}].", headerValue, hostHeader,
                TENANT_SUBDOMAIN_PATTERN.pattern());
        return Optional.empty();
    }

    private Optional<Tenant> tenantBySubdomain(Optional<String> subdomainOpt) {
        if (subdomainOpt.isEmpty()) {
            return Optional.empty();
        }
        String subdomain = subdomainOpt.get();
        return tenantCache.get(subdomain, k -> {
            LOGGER.debug("Searching for tenant with subdomain [{}] from database", subdomain);
            return tenantService.findBySubdomain(subdomain)
                                .map(TenantImpl::createFromEntity);
        });
    }

    /**
     * Should not filter.
     *
     * @param request the request
     * @return true, if successful
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI()
                      .startsWith("/webjars/")
                || request.getRequestURI()
                          .startsWith("/css/")
                || request.getRequestURI()
                          .startsWith("/js/")
                || request.getRequestURI()
                          .endsWith(".ico");
    }

}
