/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.security;

import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.base.util.AuthoritiesUtil;
import org.eclipse.dirigible.components.tenants.domain.User;
import org.eclipse.dirigible.components.tenants.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * The Class CustomUserDetailsService.
 */
@ConditionalOnProperty(name = "basic.enabled", havingValue = "true")
@Service
public class CustomUserDetailsService implements UserDetailsService {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomUserDetailsService.class);

    /** The user service. */
    private final UserService userService;

    /** The tenant context. */
    private final TenantContext tenantContext;

    /**
     * Instantiates a new custom user details service.
     *
     * @param userService the user service
     * @param tenantContext the tenant context
     */
    public CustomUserDetailsService(UserService userService, TenantContext tenantContext) {
        this.userService = userService;
        this.tenantContext = tenantContext;
    }

    /**
     * Load user by username.
     *
     * @param username the username
     * @return the user details
     * @throws UsernameNotFoundException the username not found exception
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Tenant tenant = tenantContext.getCurrentTenant();

        LOGGER.debug("Loading user with username [{}] in tenant [{}]...", username, tenant);
        User user = userService.findUserByUsernameAndTenantId(username, tenant.getId())
                               .orElseThrow(() -> new UsernameNotFoundException(
                                       "Username [" + username + "] was not found in tenant [" + tenant + "]."));

        LOGGER.debug("Logged in user with username [{}] in tenant [{}]", username, tenant);
        Set<String> userRoles = userService.getUserRoleNames(user);
        LOGGER.debug("User [{}] has assigned roles [{}]", user, userRoles);

        Set<GrantedAuthority> auths = AuthoritiesUtil.toAuthorities(userRoles);

        return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(), auths);
    }

}
