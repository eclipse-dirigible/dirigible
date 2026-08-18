/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.oauth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.List;
import org.eclipse.dirigible.components.security.oauth.client.CognitoDefaultTenantProperties;
import org.eclipse.dirigible.components.security.oauth.client.KeycloakDefaultTenantProperties;
import org.eclipse.dirigible.components.security.oauth.domain.ClientRegistration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link DynamicClientRegistrationRepository} - most importantly that a direct
 * lookup initializes the lazily populated store. Historically only the generated login page
 * iterated the repository, so with a custom login page (or a native login as the first request)
 * every {@code findByRegistrationId} answered {@code null} and both the authorization redirect and
 * the native sign-in failed.
 */
@ExtendWith(MockitoExtension.class)
class DynamicClientRegistrationRepositoryTest {

    @Mock
    private ClientRegistrationService service;

    @Mock
    private CognitoDefaultTenantProperties cognitoProperties;

    @Mock
    private KeycloakDefaultTenantProperties keycloakProperties;

    @Test
    void aDirectLookupInitializesTheStore() {
        ClientRegistration stored = new ClientRegistration("test-tenant", "lazy-lookup-client", "", "client-id", "client-secret",
                "{baseUrl}/login/oauth2/code/{registrationId}", "authorization_code", "openid", "https://idp.example.org/oauth2/token",
                "https://idp.example.org/oauth2/authorize", "https://idp.example.org/oauth2/userInfo", "https://idp.example.org",
                "https://idp.example.org/jwks", "email");
        when(service.getAll()).thenReturn(List.of(stored));
        DynamicClientRegistrationRepository repository =
                new DynamicClientRegistrationRepository(service, cognitoProperties, keycloakProperties);

        org.springframework.security.oauth2.client.registration.ClientRegistration found =
                repository.findByRegistrationId("lazy-lookup-client");

        assertNotNull(found);
        assertEquals("lazy-lookup-client", found.getRegistrationId());
        assertEquals("client-id", found.getClientId());
    }

    @Test
    void anUnknownRegistrationStaysUnknown() {
        when(service.getAll()).thenReturn(List.of());
        DynamicClientRegistrationRepository repository =
                new DynamicClientRegistrationRepository(service, cognitoProperties, keycloakProperties);

        assertNull(repository.findByRegistrationId("never-registered"));
    }
}
