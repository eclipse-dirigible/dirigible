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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Authorization-request resolver that passes an allowlisted identity-provider hint from
 * {@code /oauth2/authorization/{registrationId}} through onto the authorize redirect.
 *
 * <p>
 * {@link DefaultOAuth2AuthorizationRequestResolver} drops request parameters, so an application
 * cannot deep-link a federated login. With the hint forwarded ({@code identity_provider} /
 * {@code idp_identifier} for Cognito, {@code kc_idp_hint} for Keycloak), the provider redirects
 * straight to the corporate IdP and its own hosted page is never rendered, keeping SAML/social
 * federation working in a fully first-party UX. Only the allowlisted parameters are forwarded -
 * everything else on the request stays dropped, as before.
 */
public class IdpHintAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    /** The identity-provider hint parameters known to the supported providers. */
    private static final Set<String> ALLOWED_PARAMETERS = Set.of("identity_provider", "idp_identifier", "kc_idp_hint");

    private final OAuth2AuthorizationRequestResolver delegate;

    /**
     * Instantiates the resolver over the default one on the standard authorization base URI.
     *
     * @param clientRegistrationRepository the client registration repository
     */
    public IdpHintAuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository) {
        this(new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository,
                OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI));
    }

    IdpHintAuthorizationRequestResolver(OAuth2AuthorizationRequestResolver delegate) {
        this.delegate = delegate;
    }

    /**
     * Resolves the authorization request, forwarding any allowlisted hint parameters.
     *
     * @param request the request
     * @return the authorization request, or {@code null} when the request does not match
     */
    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        return withIdpHints(request, delegate.resolve(request));
    }

    /**
     * Resolves the authorization request for the given registration, forwarding any allowlisted hint
     * parameters.
     *
     * @param request the request
     * @param clientRegistrationId the client registration id
     * @return the authorization request, or {@code null} when the request does not match
     */
    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        return withIdpHints(request, delegate.resolve(request, clientRegistrationId));
    }

    private static OAuth2AuthorizationRequest withIdpHints(HttpServletRequest request, OAuth2AuthorizationRequest authorizationRequest) {
        if (authorizationRequest == null) {
            return null;
        }
        Map<String, Object> hints = new LinkedHashMap<>();
        for (String parameter : ALLOWED_PARAMETERS) {
            String value = request.getParameter(parameter);
            if (value != null && !value.isBlank()) {
                hints.put(parameter, value);
            }
        }
        if (hints.isEmpty()) {
            return authorizationRequest;
        }
        return OAuth2AuthorizationRequest.from(authorizationRequest)
                                         .additionalParameters(parameters -> parameters.putAll(hints))
                                         .build();
    }
}
