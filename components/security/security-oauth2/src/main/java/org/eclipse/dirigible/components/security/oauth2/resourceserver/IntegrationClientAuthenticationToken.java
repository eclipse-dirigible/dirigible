/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.oauth2.resourceserver;

import java.util.Collection;
import java.util.List;

import org.eclipse.dirigible.components.base.tenant.clients.IntegrationClient;
import org.eclipse.dirigible.components.base.util.AuthoritiesUtil;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * The identity of a registered integration client. Its name is the client id - what a row it writes
 * records as its author. As authenticated it holds no role at all: nothing its token's scopes map
 * to, and its roles only once the request has entered the tenant the client belongs to (see
 * {@link #inItsTenant()}), which the tenant selection filter does for every request that names that
 * tenant and refuses for any other.
 */
public class IntegrationClientAuthenticationToken extends JwtAuthenticationToken {

    private static final long serialVersionUID = 1L;

    /** The client as the registry knows it. */
    private final IntegrationClient client;

    /**
     * Instantiates the identity of a client outside its tenant - without roles.
     *
     * @param jwt the access token
     * @param client the client as the registry knows it
     */
    public IntegrationClientAuthenticationToken(Jwt jwt, IntegrationClient client) {
        this(jwt, client, List.of());
    }

    private IntegrationClientAuthenticationToken(Jwt jwt, IntegrationClient client, Collection<? extends GrantedAuthority> authorities) {
        super(jwt, authorities, client.clientId());
        this.client = client;
    }

    /**
     * Gets the client.
     *
     * @return the client as the registry knows it
     */
    public IntegrationClient getClient() {
        return client;
    }

    /**
     * The identity of the client inside its tenant: the roles the registry gives it there, and nothing
     * else.
     *
     * @return the identity to run a request of the client's tenant as
     */
    public IntegrationClientAuthenticationToken inItsTenant() {
        IntegrationClientAuthenticationToken entered =
                new IntegrationClientAuthenticationToken(getToken(), client, AuthoritiesUtil.toAuthorities(client.roles()));
        entered.setDetails(getDetails());
        return entered;
    }
}
