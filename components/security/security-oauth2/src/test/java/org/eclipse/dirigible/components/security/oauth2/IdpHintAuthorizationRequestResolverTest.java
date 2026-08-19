/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.oauth2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

/**
 * Unit tests for {@link IdpHintAuthorizationRequestResolver} - the allowlisted identity-provider
 * hint passthrough onto the authorize redirect.
 */
@ExtendWith(MockitoExtension.class)
class IdpHintAuthorizationRequestResolverTest {

    @Mock
    private OAuth2AuthorizationRequestResolver delegate;

    private IdpHintAuthorizationRequestResolver resolver;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        resolver = new IdpHintAuthorizationRequestResolver(delegate);
        request = new MockHttpServletRequest("GET", "/oauth2/authorization/test");
    }

    @Test
    void forwardsTheAllowlistedHints() {
        request.setParameter("identity_provider", "CorporateSaml");
        request.setParameter("kc_idp_hint", "corporate-oidc");
        when(delegate.resolve(request)).thenReturn(authorizationRequest());

        OAuth2AuthorizationRequest resolved = resolver.resolve(request);

        assertEquals("CorporateSaml", resolved.getAdditionalParameters()
                                              .get("identity_provider"));
        assertEquals("corporate-oidc", resolved.getAdditionalParameters()
                                               .get("kc_idp_hint"));
    }

    @Test
    void dropsEverythingOutsideTheAllowlist() {
        request.setParameter("identity_provider", "CorporateSaml");
        request.setParameter("prompt", "none");
        request.setParameter("redirect_uri", "https://evil.example.org/");
        when(delegate.resolve(request)).thenReturn(authorizationRequest());

        OAuth2AuthorizationRequest resolved = resolver.resolve(request);

        assertEquals("CorporateSaml", resolved.getAdditionalParameters()
                                              .get("identity_provider"));
        assertFalse(resolved.getAdditionalParameters()
                            .containsKey("prompt"));
        assertFalse(resolved.getAdditionalParameters()
                            .containsKey("redirect_uri"));
    }

    @Test
    void leavesARequestWithoutHintsUntouched() {
        OAuth2AuthorizationRequest original = authorizationRequest();
        when(delegate.resolve(request)).thenReturn(original);

        assertSame(original, resolver.resolve(request));
    }

    @Test
    void passesANonMatchingRequestThrough() {
        when(delegate.resolve(request)).thenReturn(null);

        assertNull(resolver.resolve(request));
    }

    @Test
    void forwardsTheHintsOnTheExplicitRegistrationOverloadToo() {
        request.setParameter("idp_identifier", "corp");
        when(delegate.resolve(request, "test")).thenReturn(authorizationRequest());

        OAuth2AuthorizationRequest resolved = resolver.resolve(request, "test");

        assertEquals("corp", resolved.getAdditionalParameters()
                                     .get("idp_identifier"));
    }

    private static OAuth2AuthorizationRequest authorizationRequest() {
        return OAuth2AuthorizationRequest.authorizationCode()
                                         .authorizationUri("https://idp.example.org/oauth2/authorize")
                                         .clientId("client-id")
                                         .redirectUri("https://app.example.org/login/oauth2/code/test")
                                         .state("state-1")
                                         .build();
    }
}
