/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.oauth.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.security.oauth.client.CognitoDefaultTenantProperties;
import org.eclipse.dirigible.components.security.oauth.client.KeycloakDefaultTenantProperties;
import org.eclipse.dirigible.components.security.oauth.domain.ClientRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.stereotype.Component;

@Component
public class DynamicClientRegistrationRepository
        implements ClientRegistrationRepository, Iterable<org.springframework.security.oauth2.client.registration.ClientRegistration> {

    private static final Logger logger = LoggerFactory.getLogger(DynamicClientRegistrationRepository.class);
    private static final Map<String, org.springframework.security.oauth2.client.registration.ClientRegistration> REGISTRATIONS =
            new HashMap<>();

    private final ClientRegistrationService service;
    private final List<ClientRegistration> clientRegistrations = new ArrayList<>();;

    public DynamicClientRegistrationRepository(ClientRegistrationService service, CognitoDefaultTenantProperties cognitoClient,
            KeycloakDefaultTenantProperties keycloakClient) {
        this.service = service;
        registerCognitoClient(cognitoClient);
        registerKeycloakClient(keycloakClient);
        registerCustomClients();
    }

    private void registerCognitoClient(CognitoDefaultTenantProperties cognitoClient) {
        if (cognitoClient.hasAllProperties()) {
            ClientRegistration cognitoClientRegistration = new ClientRegistration(cognitoClient.getClientName(), "cognito", "",
                    cognitoClient.getClientId(), cognitoClient.getClientSecret(), cognitoClient.getRedirectUri(),
                    cognitoClient.getAuthorizationGrantType(), cognitoClient.getScope(), cognitoClient.getTokenUri(),
                    cognitoClient.getAuthorizationUri(), cognitoClient.getUserInfoUri(), cognitoClient.getIssuerUri(),
                    cognitoClient.getJwkSetUri(), cognitoClient.getUserNameAttribute());
            cognitoClientRegistration.setId("default-tenant");
            cognitoClientRegistration.updateKey();
            clientRegistrations.add(cognitoClientRegistration);
        }
    }

    private void registerKeycloakClient(KeycloakDefaultTenantProperties keycloakClient) {
        if (keycloakClient.hasAllProperties()) {
            ClientRegistration keycloakClientRegistration = new ClientRegistration(keycloakClient.getClientName(), "keycloak", "",
                    keycloakClient.getClientId(), keycloakClient.getClientSecret(), keycloakClient.getRedirectUri(),
                    keycloakClient.getAuthorizationGrantType(), keycloakClient.getScope(), keycloakClient.getTokenUri(),
                    keycloakClient.getAuthorizationUri(), keycloakClient.getUserInfoUri(), keycloakClient.getIssuerUri(),
                    keycloakClient.getJwkSetUri(), keycloakClient.getUserNameAttribute());
            keycloakClientRegistration.setId("default-tenant");
            keycloakClientRegistration.updateKey();
            clientRegistrations.add(keycloakClientRegistration);
        }
    }

    private void registerCustomClients() {
        String customOAuthClientsLists = Configuration.get("DIRIGIBLE_OAUTH_CUSTOM_CLIENTS");
        if ((customOAuthClientsLists != null) && !"".equals(customOAuthClientsLists)) {
            logger.trace("Custom OAuth clients list: [{}]", customOAuthClientsLists);
            StringTokenizer tokens = new StringTokenizer(customOAuthClientsLists, ",");
            while (tokens.hasMoreTokens()) {
                String name = tokens.nextToken();
                clientRegistrations.add(createOAuthClientRegistration(name));
            }
        }
    }

    @Override
    public Iterator<org.springframework.security.oauth2.client.registration.ClientRegistration> iterator() {
        clientRegistrations.forEach(next -> {
            logger.info("Initializing a custom OAuth client with name [{}]", next.getName());
            this.save(next);
        });
        service.getAll()
               .stream()
               .map((ClientRegistration registration) -> toClientRegistration(registration))
               .forEach(e -> REGISTRATIONS.put(e.getRegistrationId(), e));
        return REGISTRATIONS.values()
                            .iterator();
    }

    @Override
    public org.springframework.security.oauth2.client.registration.ClientRegistration findByRegistrationId(String registrationId) {
        return REGISTRATIONS.get(registrationId);
    }

    public Optional<ClientRegistration> findById(String id) {
        return service.findById(id);
    }

    public Optional<ClientRegistration> findByClientId(String clientId) {
        return service.findByClientId(clientId);
    }

    public List<ClientRegistration> getAll() {
        return service.getAll();
    }

    public ClientRegistration save(ClientRegistration registration) {
        ClientRegistration newRegistration = service.save(registration);
        org.springframework.security.oauth2.client.registration.ClientRegistration clientRegistration = toClientRegistration(registration);
        REGISTRATIONS.put(clientRegistration.getRegistrationId(), clientRegistration);
        return newRegistration;
    }

    public void delete(ClientRegistration registration) {
        service.delete(registration);
        org.springframework.security.oauth2.client.registration.ClientRegistration clientRegistration = toClientRegistration(registration);
        REGISTRATIONS.remove(clientRegistration.getRegistrationId());
    }

    private org.springframework.security.oauth2.client.registration.ClientRegistration toClientRegistration(
            ClientRegistration registration) {
        AuthorizationGrantType authorizationGrantType = AuthorizationGrantType.AUTHORIZATION_CODE;
        if (registration.getAuthorizationGrantType()
                        .equals("client_credentials")) {
            authorizationGrantType = AuthorizationGrantType.CLIENT_CREDENTIALS;
        } else if (registration.getAuthorizationGrantType()
                               .equals("refresh_token")) {
            authorizationGrantType = AuthorizationGrantType.REFRESH_TOKEN;
        }

        return org.springframework.security.oauth2.client.registration.ClientRegistration.withRegistrationId(registration.getName())
                                                                                         .clientId(registration.getClientId())
                                                                                         .clientSecret(registration.getClientSecret())
                                                                                         .tokenUri(registration.getTokenUri())
                                                                                         .authorizationUri(
                                                                                                 registration.getAuthorizationUri())
                                                                                         .userInfoUri(registration.getUserInfoUri())
                                                                                         .issuerUri(registration.getIssuerUri())
                                                                                         .jwkSetUri(registration.getJwkSetUri())
                                                                                         .redirectUri(registration.getRedirectUri())
                                                                                         .userNameAttributeName(
                                                                                                 registration.getUserNameAttributeName())
                                                                                         .scope(registration.getScope()
                                                                                                            .split(","))
                                                                                         .authorizationGrantType(authorizationGrantType)
                                                                                         .build();
    }

    private ClientRegistration createOAuthClientRegistration(String name) {
        String clientId = getRequiredParameter(name, "CLIENT_ID");
        String clientSecret = getRequiredParameter(name, "CLIENT_SECRET");
        String redirectUri = getRequiredParameter(name, "REDIRECT_URI");
        String grantType = getRequiredParameter(name, "GRANT_TYPE");
        String scope = getRequiredParameter(name, "SCOPE");
        String tokenUri = getRequiredParameter(name, "TOKEN_URI");
        String authorizationUri = getRequiredParameter(name, "AUTHORIZATION_URI");
        String userInfoUri = getRequiredParameter(name, "USER_INFO_URI");
        String issuerUri = getRequiredParameter(name, "ISSUER_URI");
        String jwkSetUri = getRequiredParameter(name, "JWK_SET_URI");
        String userNameAttribute = getRequiredParameter(name, "USER_NAME_ATTRIBUTE");

        ClientRegistration clientRegistration = new ClientRegistration(name, name, "", clientId, clientSecret, redirectUri, grantType,
                scope, tokenUri, authorizationUri, userInfoUri, issuerUri, jwkSetUri, userNameAttribute);
        clientRegistration.setId(name);
        clientRegistration.updateKey();

        return clientRegistration;
    }

    private String getRequiredParameter(String clientRegistrationName, String suffix) {
        String configName = createConfigName(clientRegistrationName, suffix);
        String value = Configuration.get(configName);
        if (null == value || value.trim()
                                  .isEmpty()) {
            throw new IllegalArgumentException("Missing required configuration parameter [" + configName + "] for data source ["
                    + clientRegistrationName + "]. The value is: " + value);
        }
        return value;
    }

    private String createConfigName(String clientRegistrationName, String suffix) {
        return clientRegistrationName + "_" + suffix;
    }
}
