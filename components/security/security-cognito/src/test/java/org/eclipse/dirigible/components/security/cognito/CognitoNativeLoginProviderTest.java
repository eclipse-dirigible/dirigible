/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.cognito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.eclipse.dirigible.components.security.oauth2.login.NativeLoginChallenge;
import org.eclipse.dirigible.components.security.oauth2.login.NativeLoginChallengeAnswer;
import org.eclipse.dirigible.components.security.oauth2.login.NativeLoginCredentials;
import org.eclipse.dirigible.components.security.oauth2.login.NativeLoginException;
import org.eclipse.dirigible.components.security.oauth2.login.NativeLoginResult;
import org.eclipse.dirigible.components.security.oauth2.login.NativeLoginTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Unit tests for {@link CognitoNativeLoginProvider} - the SRP handshake orchestration, the
 * challenge round-trip and the normalization of Cognito error codes.
 */
@ExtendWith(MockitoExtension.class)
class CognitoNativeLoginProviderTest {

    private static final Gson GSON = new Gson();
    private static final String USER_ID_FOR_SRP = "3b3f18f4-1c3d-4b6e-9f9a-0e2f4c1a5d77";

    @Mock
    private CognitoIdpClient idpClient;

    private CognitoNativeLoginProvider provider;
    private ClientRegistration registration;

    @BeforeEach
    void setUp() {
        provider = new CognitoNativeLoginProvider(idpClient);
        registration = ClientRegistration.withRegistrationId("default-tenant")
                                         .clientId("client-id")
                                         .clientSecret("client-secret")
                                         .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                                         .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                                         .authorizationUri("https://cognito.example.org/oauth2/authorize")
                                         .tokenUri("https://cognito.example.org/oauth2/token")
                                         .issuerUri("https://cognito-idp.eu-central-1.amazonaws.com/eu-central-1_TestPool1")
                                         .userNameAttributeName("email")
                                         .build();
    }

    @Test
    void theDefaultRegistrationIsTheOneTheDefaultTenantClientIsRegisteredUnder() {
        assertEquals("cognito", provider.getDefaultRegistrationId());
    }

    @Test
    void aPasswordLoginRunsTheSrpHandshakeAndReturnsTheTokens() {
        when(idpClient.initiateAuth(any(), eq("client-id"), anyMap(), any())).thenReturn(passwordVerifierChallenge());
        when(idpClient.respondToAuthChallenge(any(), eq("client-id"), eq("PASSWORD_VERIFIER"), eq("session-1"), anyMap(),
                any())).thenReturn(authenticated());

        NativeLoginResult result =
                provider.authenticate(registration, new NativeLoginCredentials("jane.doe@example.org", "the-password", null));

        NativeLoginTokens tokens = assertInstanceOf(NativeLoginTokens.class, result);
        assertEquals("id-token", tokens.idToken());
        assertEquals("access-token", tokens.accessToken());
        assertEquals("refresh-token", tokens.refreshToken());
        assertEquals(3600L, tokens.expiresInSeconds());

        ArgumentCaptor<Map<String, String>> initiation = ArgumentCaptor.forClass(Map.class);
        verify(idpClient).initiateAuth(any(), eq("client-id"), initiation.capture(), any());
        assertEquals("jane.doe@example.org", initiation.getValue()
                                                       .get("USERNAME"));
        assertNotNull(initiation.getValue()
                                .get("SRP_A"));
        assertNotNull(initiation.getValue()
                                .get("SECRET_HASH"));

        ArgumentCaptor<Map<String, String>> verifier = ArgumentCaptor.forClass(Map.class);
        verify(idpClient).respondToAuthChallenge(any(), eq("client-id"), eq("PASSWORD_VERIFIER"), eq("session-1"), verifier.capture(),
                any());
        // the verifier answers for the canonical user Cognito resolved, not the typed alias
        assertEquals(USER_ID_FOR_SRP, verifier.getValue()
                                              .get("USERNAME"));
        assertEquals("c2VjcmV0LWJsb2Nr", verifier.getValue()
                                                 .get("PASSWORD_CLAIM_SECRET_BLOCK"));
        assertNotNull(verifier.getValue()
                              .get("PASSWORD_CLAIM_SIGNATURE"));
        assertNotNull(verifier.getValue()
                              .get("TIMESTAMP"));
        assertEquals(CognitoSrp.secretHash("client-id", "client-secret", USER_ID_FOR_SRP), verifier.getValue()
                                                                                                   .get("SECRET_HASH"));
    }

    @Test
    void anMfaChallengeIsSurfacedWithTheCanonicalUsername() {
        when(idpClient.initiateAuth(any(), anyString(), anyMap(), any())).thenReturn(passwordVerifierChallenge());
        when(idpClient.respondToAuthChallenge(any(), anyString(), eq("PASSWORD_VERIFIER"), anyString(), anyMap(), any())).thenReturn(
                GSON.fromJson("{\"ChallengeName\":\"SOFTWARE_TOKEN_MFA\",\"Session\":\"session-2\",\"ChallengeParameters\":{}}",
                        JsonObject.class));

        NativeLoginResult result =
                provider.authenticate(registration, new NativeLoginCredentials("jane.doe@example.org", "the-password", null));

        NativeLoginChallenge challenge = assertInstanceOf(NativeLoginChallenge.class, result);
        assertEquals("SOFTWARE_TOKEN_MFA", challenge.name());
        assertEquals("session-2", challenge.session());
        assertEquals(USER_ID_FOR_SRP, challenge.parameters()
                                               .get("USERNAME"));
    }

    @Test
    void aChallengeAnswerCarriesTheUsernameAndSecretHash() {
        when(idpClient.respondToAuthChallenge(any(), eq("client-id"), eq("SOFTWARE_TOKEN_MFA"), eq("session-2"), anyMap(),
                any())).thenReturn(authenticated());

        NativeLoginResult result = provider.answerChallenge(registration, new NativeLoginChallengeAnswer("SOFTWARE_TOKEN_MFA", "session-2",
                USER_ID_FOR_SRP, Map.of("SOFTWARE_TOKEN_MFA_CODE", "123456"), null));

        assertInstanceOf(NativeLoginTokens.class, result);
        ArgumentCaptor<Map<String, String>> responses = ArgumentCaptor.forClass(Map.class);
        verify(idpClient).respondToAuthChallenge(any(), eq("client-id"), eq("SOFTWARE_TOKEN_MFA"), eq("session-2"), responses.capture(),
                any());
        assertEquals("123456", responses.getValue()
                                        .get("SOFTWARE_TOKEN_MFA_CODE"));
        assertEquals(USER_ID_FOR_SRP, responses.getValue()
                                               .get("USERNAME"));
        assertNotNull(responses.getValue()
                               .get("SECRET_HASH"));
    }

    @Test
    void anUnknownUserNormalizesToInvalidCredentials() {
        when(idpClient.initiateAuth(any(), anyString(), anyMap(), any())).thenThrow(
                new CognitoIdpException("UserNotFoundException", "User does not exist."));

        NativeLoginException exception = assertThrows(NativeLoginException.class,
                () -> provider.authenticate(registration, new NativeLoginCredentials("who@example.org", "any", null)));

        assertEquals(NativeLoginException.Outcome.INVALID_CREDENTIALS, exception.getOutcome());
    }

    @Test
    void throttlingNormalizesToTooManyAttempts() {
        when(idpClient.initiateAuth(any(), anyString(), anyMap(), any())).thenThrow(
                new CognitoIdpException("TooManyRequestsException", "Rate exceeded"));

        NativeLoginException exception = assertThrows(NativeLoginException.class,
                () -> provider.authenticate(registration, new NativeLoginCredentials("jane.doe@example.org", "any", null)));

        assertEquals(NativeLoginException.Outcome.TOO_MANY_ATTEMPTS, exception.getOutcome());
    }

    @Test
    void aTransportFailureNormalizesToAuthenticationFailed() {
        when(idpClient.initiateAuth(any(), anyString(), anyMap(), any())).thenThrow(new CognitoIdpException(null, "unreachable"));

        NativeLoginException exception = assertThrows(NativeLoginException.class,
                () -> provider.authenticate(registration, new NativeLoginCredentials("jane.doe@example.org", "any", null)));

        assertEquals(NativeLoginException.Outcome.AUTHENTICATION_FAILED, exception.getOutcome());
    }

    @Test
    void aRegistrationWithoutClientSecretSendsNoSecretHash() {
        ClientRegistration withoutSecret = ClientRegistration.withRegistrationId("default-tenant")
                                                             .clientId("client-id")
                                                             .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                                                             .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                                                             .authorizationUri("https://cognito.example.org/oauth2/authorize")
                                                             .tokenUri("https://cognito.example.org/oauth2/token")
                                                             .issuerUri(
                                                                     "https://cognito-idp.eu-central-1.amazonaws.com/eu-central-1_TestPool1")
                                                             .userNameAttributeName("email")
                                                             .build();
        when(idpClient.initiateAuth(any(), anyString(), anyMap(), any())).thenReturn(passwordVerifierChallenge());
        when(idpClient.respondToAuthChallenge(any(), anyString(), anyString(), anyString(), anyMap(), any())).thenReturn(authenticated());

        provider.authenticate(withoutSecret, new NativeLoginCredentials("jane.doe@example.org", "the-password", null));

        ArgumentCaptor<Map<String, String>> initiation = ArgumentCaptor.forClass(Map.class);
        verify(idpClient).initiateAuth(any(), anyString(), initiation.capture(), any());
        assertNull(initiation.getValue()
                             .get("SECRET_HASH"));
    }

    private static JsonObject passwordVerifierChallenge() {
        CognitoSrp serverFacingSrp = new CognitoSrp();
        JsonObject response = new JsonObject();
        response.addProperty("ChallengeName", "PASSWORD_VERIFIER");
        response.addProperty("Session", "session-1");
        JsonObject parameters = new JsonObject();
        parameters.addProperty("USER_ID_FOR_SRP", USER_ID_FOR_SRP);
        parameters.addProperty("USERNAME", USER_ID_FOR_SRP);
        // any valid group element works for the handshake shape - the signature is not verified here
        parameters.addProperty("SRP_B", serverFacingSrp.srpA());
        parameters.addProperty("SALT", "aa00bb11");
        parameters.addProperty("SECRET_BLOCK", "c2VjcmV0LWJsb2Nr");
        response.add("ChallengeParameters", parameters);
        return response;
    }

    private static JsonObject authenticated() {
        return GSON.fromJson("{\"AuthenticationResult\":{\"IdToken\":\"id-token\",\"AccessToken\":\"access-token\","
                + "\"RefreshToken\":\"refresh-token\",\"ExpiresIn\":3600,\"TokenType\":\"Bearer\"}}", JsonObject.class);
    }
}
