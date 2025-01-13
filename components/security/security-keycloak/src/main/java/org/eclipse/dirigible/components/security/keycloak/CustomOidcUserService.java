/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.keycloak;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.base.http.roles.Roles;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.base.util.AuthoritiesUtil;
import org.eclipse.dirigible.components.tenants.domain.User;
import org.eclipse.dirigible.components.tenants.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The Class CustomOidcUserService.
 */
@Profile("keycloak")
@Service
public class CustomOidcUserService extends OidcUserService {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomOidcUserService.class);

    /** The Constant USER_NOT_IN_TENANT_ERR. */
    private static final OAuth2Error USER_NOT_IN_TENANT_ERR = new OAuth2Error("user_not_registered_in_tenant");

    /** The user service. */
    private final UserService userService;

    /** The tenant context. */
    private final TenantContext tenantContext;

    /**
     * Instantiates a new custom oidc user service.
     *
     * @param userService the user service
     * @param tenantContext the tenant context
     */
    public CustomOidcUserService(UserService userService, TenantContext tenantContext) {
        this.userService = userService;
        this.tenantContext = tenantContext;
    }

    /**
     * Load user.
     *
     * @param userRequest the user request
     * @return the oidc user
     * @throws OAuth2AuthenticationException the o auth 2 authentication exception
     */
    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        User user = getTenantUser(oidcUser);
        Set<String> roleNames = getRoleNames(user);
        Set<GrantedAuthority> roleAuthorities = AuthoritiesUtil.toAuthorities(roleNames);

        return createOidcUser(userRequest, oidcUser, roleAuthorities);

    }

    /**
     * Gets the tenant user.
     *
     * @param oidcUser the oidc user
     * @return the tenant user
     */
    private User getTenantUser(OidcUser oidcUser) {
        if (isTrialEnabled()) {
            LOGGER.debug("Trial enabled - user [{}] will not be checked for the current tenant.", oidcUser.getName());
            return null;
        }
        String username = oidcUser.getName();
        String tenantId = tenantContext.getCurrentTenant()
                                       .getId();

        return userService.findUserByUsernameAndTenantId(username, tenantId)
                          .orElseThrow(() -> {
                              String message = "User [" + username + "] was not found in tenant [" + tenantId + "].";
                              return new OAuth2AuthenticationException(USER_NOT_IN_TENANT_ERR, message);
                          });
    }

    /**
     * Checks if is trial enabled.
     *
     * @return true, if is trial enabled
     */
    private boolean isTrialEnabled() {
        return DirigibleConfig.TRIAL_ENABLED.getBooleanValue();
    }

    /**
     * Gets the role names.
     *
     * @param user the user
     * @return the role names
     */
    private Set<String> getRoleNames(User user) {
        if (isTrialEnabled()) {
            LOGGER.debug("Trial enabled - returning all available system roles for the current user.");
            return Arrays.stream(Roles.values())
                         .map(Roles::getRoleName)
                         .collect(Collectors.toSet());
        }
        return userService.getUserRoleNames(user);
    }

    /**
     * Creates the oidc user.
     *
     * @param userRequest the user request
     * @param oidcUser the oidc user
     * @param additionalAuthorities the additional authorities
     * @return the oidc user
     */
    private OidcUser createOidcUser(OidcUserRequest userRequest, OidcUser oidcUser, Set<GrantedAuthority> additionalAuthorities) {
        String userNameAttributeName = userRequest.getClientRegistration()
                                                  .getProviderDetails()
                                                  .getUserInfoEndpoint()
                                                  .getUserNameAttributeName();
        return StringUtils.hasText(userNameAttributeName) ? new CustomOidcUser(oidcUser, userNameAttributeName, additionalAuthorities)
                : new CustomOidcUser(oidcUser, additionalAuthorities);
    }

}
