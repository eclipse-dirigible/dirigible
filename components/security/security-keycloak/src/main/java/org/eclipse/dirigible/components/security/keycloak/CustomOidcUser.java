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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * The Class CustomOidcUser.
 */
class CustomOidcUser extends DefaultOidcUser {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 6970756030913509875L;

    /** The additional authorities. */
    private final Set<GrantedAuthority> additionalAuthorities;

    /**
     * Instantiates a new custom oidc user.
     *
     * @param oidcUser the oidc user
     * @param additionalAuthorities the additional authorities
     */
    CustomOidcUser(OidcUser oidcUser, Set<GrantedAuthority> additionalAuthorities) {
        super(oidcUser.getAuthorities(), oidcUser.getIdToken(), oidcUser.getUserInfo());
        this.additionalAuthorities = additionalAuthorities;
    }

    /**
     * Instantiates a new custom oidc user.
     *
     * @param oidcUser the oidc user
     * @param userNameAttributeName the user name attribute name
     * @param additionalAuthorities the additional authorities
     */
    CustomOidcUser(OidcUser oidcUser, String userNameAttributeName, Set<GrantedAuthority> additionalAuthorities) {
        super(oidcUser.getAuthorities(), oidcUser.getIdToken(), oidcUser.getUserInfo(), userNameAttributeName);
        this.additionalAuthorities = additionalAuthorities;
    }

    /**
     * Gets the authorities.
     *
     * @return the authorities
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Collection<? extends GrantedAuthority> authorities = super.getAuthorities();

        List<GrantedAuthority> allAuthorities = new ArrayList<>(authorities);
        allAuthorities.addAll(additionalAuthorities);

        return Collections.unmodifiableCollection(allAuthorities);
    }

}
