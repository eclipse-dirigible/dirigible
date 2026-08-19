/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.oauth2.login;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for {@link NativeLoginEndpoint} - the request validation, the provider dispatch, the
 * challenge round-trip contract and the normalized outcome codes.
 */
@ExtendWith(MockitoExtension.class)
class NativeLoginEndpointTest {

    private static final String REGISTRATION_ID = "default-tenant";

    @Mock
    private ObjectProvider<NativeLoginProvider> loginProviderProvider;

    @Mock
    private ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider;

    @Mock
    private NativeLoginProvider loginProvider;

    @Mock
    private ClientRegistrationRepository clientRegistrationRepository;

    @Mock
    private NativeLoginSessionInitializer sessionInitializer;

    private NativeLoginEndpoint endpoint;
    private ClientRegistration registration;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        endpoint = new NativeLoginEndpoint(loginProviderProvider, clientRegistrationRepositoryProvider, sessionInitializer);
        registration = ClientRegistration.withRegistrationId(REGISTRATION_ID)
                                         .clientId("client-id")
                                         .clientSecret("client-secret")
                                         .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                                         .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                                         .authorizationUri("https://idp.example.org/oauth2/authorize")
                                         .tokenUri("https://idp.example.org/oauth2/token")
                                         .userNameAttributeName("email")
                                         .build();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    void aSuccessfulLoginMintsTheSessionAndAnswersAuthenticated() {
        stubProviderAndRegistration();
        NativeLoginTokens tokens = new NativeLoginTokens("id-token", "access-token", "refresh-token", 3600L);
        when(loginProvider.authenticate(eq(registration), any())).thenReturn(tokens);

        ResponseEntity<Map<String, Object>> result =
                endpoint.login(new NativeLoginEndpoint.LoginRequest(null, "jane.doe@example.org", "the-password", null), request, response);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("AUTHENTICATED", result.getBody()
                                            .get("outcome"));
        verify(sessionInitializer).establishSession(registration, tokens, request, response);
    }

    @Test
    void aChallengeIsSurfacedWithoutMintingASession() {
        stubProviderAndRegistration();
        when(loginProvider.authenticate(eq(registration), any())).thenReturn(
                new NativeLoginChallenge("SOFTWARE_TOKEN_MFA", "session-1", Map.of("USERNAME", "canonical-user")));

        ResponseEntity<Map<String, Object>> result =
                endpoint.login(new NativeLoginEndpoint.LoginRequest(null, "jane.doe@example.org", "the-password", null), request, response);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("CHALLENGE", result.getBody()
                                        .get("outcome"));
        assertEquals("SOFTWARE_TOKEN_MFA", result.getBody()
                                                 .get("challenge"));
        assertEquals("session-1", result.getBody()
                                        .get("session"));
        assertEquals(Map.of("USERNAME", "canonical-user"), result.getBody()
                                                                 .get("parameters"));
        verifyNoInteractions(sessionInitializer);
    }

    @Test
    void aChallengeAnswerIsForwardedNormalized() {
        stubProviderAndRegistration();
        NativeLoginTokens tokens = new NativeLoginTokens("id-token", "access-token", "refresh-token", 3600L);
        when(loginProvider.answerChallenge(eq(registration), any())).thenReturn(tokens);

        ResponseEntity<Map<String, Object>> result = endpoint.challenge(
                new NativeLoginEndpoint.ChallengeRequest(null, "SOFTWARE_TOKEN_MFA", "session-1", "canonical-user", null, null), request,
                response);

        assertEquals("AUTHENTICATED", result.getBody()
                                            .get("outcome"));
        ArgumentCaptor<NativeLoginChallengeAnswer> answer = ArgumentCaptor.forClass(NativeLoginChallengeAnswer.class);
        verify(loginProvider).answerChallenge(eq(registration), answer.capture());
        assertEquals("SOFTWARE_TOKEN_MFA", answer.getValue()
                                                 .challenge());
        assertEquals(Map.of(), answer.getValue()
                                     .responses());
        verify(sessionInitializer).establishSession(registration, tokens, request, response);
    }

    @Test
    void missingCredentialsAreRefusedAsInvalidRequest() {
        when(loginProviderProvider.getIfAvailable()).thenReturn(loginProvider);

        NativeLoginException exception = assertThrows(NativeLoginException.class,
                () -> endpoint.login(new NativeLoginEndpoint.LoginRequest(null, "jane.doe@example.org", "", null), request, response));

        assertEquals(NativeLoginException.Outcome.INVALID_REQUEST, exception.getOutcome());
    }

    @Test
    void anUnknownRegistrationIsRefusedAsInvalidRequest() {
        when(loginProviderProvider.getIfAvailable()).thenReturn(loginProvider);
        when(clientRegistrationRepositoryProvider.getIfAvailable()).thenReturn(clientRegistrationRepository);
        when(clientRegistrationRepository.findByRegistrationId("unknown")).thenReturn(null);

        NativeLoginException exception = assertThrows(NativeLoginException.class,
                () -> endpoint.login(new NativeLoginEndpoint.LoginRequest("unknown", "jane.doe@example.org", "the-password", null), request,
                        response));

        assertEquals(NativeLoginException.Outcome.INVALID_REQUEST, exception.getOutcome());
    }

    @Test
    void withoutAProviderTheEndpointAnswersNotFound() {
        when(loginProviderProvider.getIfAvailable()).thenReturn(null);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> endpoint.login(new NativeLoginEndpoint.LoginRequest(null, "jane.doe@example.org", "the-password", null), request,
                        response));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void failuresAreAnsweredAsNormalizedOutcomes() {
        assertOutcomeStatus(NativeLoginException.Outcome.INVALID_REQUEST, HttpStatus.BAD_REQUEST);
        assertOutcomeStatus(NativeLoginException.Outcome.INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED);
        assertOutcomeStatus(NativeLoginException.Outcome.PASSWORD_RESET_REQUIRED, HttpStatus.UNAUTHORIZED);
        assertOutcomeStatus(NativeLoginException.Outcome.TOO_MANY_ATTEMPTS, HttpStatus.TOO_MANY_REQUESTS);
    }

    private void assertOutcomeStatus(NativeLoginException.Outcome outcome, HttpStatus expectedStatus) {
        ResponseEntity<Map<String, Object>> result = endpoint.onLoginFailure(new NativeLoginException(outcome, "diagnostic"));
        assertEquals(expectedStatus, result.getStatusCode());
        assertEquals(outcome.name(), result.getBody()
                                           .get("outcome"));
    }

    private void stubProviderAndRegistration() {
        when(loginProviderProvider.getIfAvailable()).thenReturn(loginProvider);
        when(clientRegistrationRepositoryProvider.getIfAvailable()).thenReturn(clientRegistrationRepository);
        when(loginProvider.getDefaultRegistrationId()).thenReturn(REGISTRATION_ID);
        when(clientRegistrationRepository.findByRegistrationId(REGISTRATION_ID)).thenReturn(registration);
    }
}
