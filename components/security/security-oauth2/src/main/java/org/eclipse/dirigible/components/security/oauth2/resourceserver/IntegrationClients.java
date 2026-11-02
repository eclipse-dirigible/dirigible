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

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.eclipse.dirigible.components.base.tenant.TenantResolutionStrategy;
import org.eclipse.dirigible.components.base.tenant.clients.IntegrationClient;
import org.eclipse.dirigible.components.base.tenant.clients.IntegrationClientRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;

/**
 * Tells the access token of a registered integration client - the machine client of a tenant's
 * external system - from every other access token.
 *
 * <p>
 * The deployment's own clients (the audiences it is configured with: its login client, the
 * provisioning system's machine client) are never looked up, so they behave exactly as before. Any
 * other client is asked of the {@link IntegrationClientRegistry} on every request, so a revoked
 * client is refused on its next call whatever its token's lifetime.
 *
 * <p>
 * Only under the {@code TOKEN_GROUPS} tenant resolution strategy: that is where the tenant
 * selection filter confines a client to its tenant. Under any other strategy no client is
 * registered, and an access token keeps the rules it always had.
 */
public final class IntegrationClients {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(IntegrationClients.class);

    /**
     * The claim an access token names its client in - Cognito's, and RFC 9068's for every JWT access
     * token.
     */
    static final String CLIENT_ID_CLAIM = "client_id";

    /** The claim Keycloak names the client a token was issued to in, when it carries no client id. */
    static final String AUTHORIZED_PARTY_CLAIM = "azp";

    /** No registry at all. */
    private static final IntegrationClients NONE = new IntegrationClients(() -> null, Set.of(), false);

    private final Supplier<IntegrationClientRegistry> registry;

    private final Set<String> deploymentClients;

    private final boolean enabled;

    /**
     * Instantiates the lookup.
     *
     * @param registry the registry, null from the supplier when the registry is not deployed
     * @param deploymentClients the deployment's own clients - never looked up
     * @param enabled whether clients are registered at all
     */
    IntegrationClients(Supplier<IntegrationClientRegistry> registry, Set<String> deploymentClients, boolean enabled) {
        this.registry = registry;
        this.deploymentClients = Set.copyOf(deploymentClients);
        this.enabled = enabled;
    }

    /**
     * The lookup of a deployment: enabled under the {@code TOKEN_GROUPS} strategy.
     *
     * @param registry the registry, null from the supplier when the registry is not deployed
     * @param settings the bearer token rules, whose audiences are the deployment's own clients
     * @return the lookup
     */
    static IntegrationClients of(Supplier<IntegrationClientRegistry> registry, ResourceServerJwtSettings settings) {
        boolean enabled = TenantResolutionStrategy.TOKEN_GROUPS == TenantResolutionStrategy.fromConfiguration();
        Set<String> deploymentClients = new HashSet<>(settings.idTokenAudiences());
        deploymentClients.addAll(settings.accessTokenAudiences());
        if (enabled) {
            LOGGER.info(
                    "Access tokens of clients other than the deployment's own [{}] are accepted when the integration client"
                            + " registry knows them, confined to the tenant it registers them for, with the roles it gives them there.",
                    deploymentClients.size());
        }
        return new IntegrationClients(registry, deploymentClients, enabled);
    }

    /**
     * No client is registered.
     *
     * @return the lookup that finds nothing
     */
    static IntegrationClients none() {
        return NONE;
    }

    /**
     * Whether the token's client is worth asking the registry about: clients are registered here, and
     * it is not one of the deployment's own.
     *
     * @param jwt the access token
     * @return true when the registry decides about the token
     */
    boolean isCandidate(Jwt jwt) {
        if (!enabled) {
            return false;
        }
        String clientId = clientIdOf(jwt);
        return clientId != null && !deploymentClients.contains(clientId);
    }

    /**
     * The registered, active client an access token was issued to.
     *
     * @param jwt the access token
     * @return the client, empty for one of the deployment's own clients, an unknown or a revoked one
     */
    Optional<IntegrationClient> registered(Jwt jwt) {
        if (!isCandidate(jwt)) {
            return Optional.empty();
        }
        IntegrationClientRegistry clients = registry.get();
        if (clients == null) {
            return Optional.empty();
        }
        return clients.findActive(clientIdOf(jwt));
    }

    /**
     * The client an access token names: {@code client_id}, else {@code azp}.
     *
     * @param jwt the access token
     * @return the client id, null when it names none
     */
    static String clientIdOf(Jwt jwt) {
        String clientId = jwt.getClaimAsString(CLIENT_ID_CLAIM);
        if (StringUtils.hasText(clientId)) {
            return clientId;
        }
        String authorizedParty = jwt.getClaimAsString(AUTHORIZED_PARTY_CLAIM);
        return StringUtils.hasText(authorizedParty) ? authorizedParty : null;
    }
}
