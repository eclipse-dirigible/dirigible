/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.snowflake;

import org.eclipse.dirigible.components.base.util.AuthoritiesUtil;
import org.eclipse.dirigible.components.tenants.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.Set;

@Profile("snowflake")
@Service
class SnowflakeUserDetailsService implements UserDetailsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SnowflakeUserDetailsService.class);

    private final SnowflakeUserManager userManager;

    SnowflakeUserDetailsService(SnowflakeUserManager userManager) {
        this.userManager = userManager;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        LOGGER.debug("Loading user with username [{}]...", username);
        User user = userManager.findUserByUsername(username)
                               .orElseGet(() -> userManager.createNewUser(username));

        LOGGER.debug("Logged in user with username [{}] in tenant [{}]", user.getUsername(), user.getTenant());
        Set<String> userRoles = userManager.getUserRoleNames(user);
        LOGGER.debug("User [{}] has assigned roles [{}]", user, userRoles);

        Set<GrantedAuthority> auths = AuthoritiesUtil.toAuthorities(userRoles);

        return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(), auths);
    }

}
