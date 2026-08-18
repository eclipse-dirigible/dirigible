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

import java.math.BigInteger;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.eclipse.dirigible.components.security.oauth2.login.NativeLoginChallenge;
import org.eclipse.dirigible.components.security.oauth2.login.NativeLoginChallengeAnswer;
import org.eclipse.dirigible.components.security.oauth2.login.NativeLoginCredentials;
import org.eclipse.dirigible.components.security.oauth2.login.NativeLoginException;
import org.eclipse.dirigible.components.security.oauth2.login.NativeLoginProvider;
import org.eclipse.dirigible.components.security.oauth2.login.NativeLoginResult;
import org.eclipse.dirigible.components.security.oauth2.login.NativeLoginTokens;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import com.google.gson.JsonObject;

/**
 * First-party Cognito sign-in over the {@code USER_SRP_AUTH} flow - deliberately not
 * {@code USER_PASSWORD_AUTH}: with SRP the password is used only to compute a zero-knowledge proof,
 * so it never crosses the wire from the platform to AWS.
 *
 * <p>
 * Everything is derived from the client registration the login runs against (client id/secret, pool
 * coordinates from the issuer URI), so per-tenant registrations work without extra configuration.
 * Challenges beyond the password verifier ({@code NEW_PASSWORD_REQUIRED}, the MFA family,
 * {@code CUSTOM_CHALLENGE}) are surfaced to the caller as a round-trip rather than failure, and
 * provider error codes are normalized - raw messages stay server-side.
 */
@Profile("cognito")
@Component
class CognitoNativeLoginProvider implements NativeLoginProvider {

    private static final String PASSWORD_VERIFIER = "PASSWORD_VERIFIER";
    private static final String USERNAME = "USERNAME";

    /**
     * The registration id {@code DynamicClientRegistrationRepository} registers the default-tenant
     * Cognito client under (the registration's name, not its {@code client-name} label).
     */
    private static final String DEFAULT_REGISTRATION_ID = "cognito";

    private final CognitoIdpClient idpClient;

    CognitoNativeLoginProvider(CognitoIdpClient idpClient) {
        this.idpClient = idpClient;
    }

    /**
     * The registration used when a login request does not name one - the default tenant's.
     *
     * @return the default client registration id
     */
    @Override
    public String getDefaultRegistrationId() {
        return DEFAULT_REGISTRATION_ID;
    }

    /**
     * Runs the SRP handshake: {@code InitiateAuth} with the ephemeral public key, then the
     * {@code PASSWORD_VERIFIER} challenge with the password claim signature.
     *
     * @param registration the client registration the login runs against
     * @param credentials the first-party credentials
     * @return the tokens, or the next challenge Cognito requires
     */
    @Override
    public NativeLoginResult authenticate(ClientRegistration registration, NativeLoginCredentials credentials) {
        CognitoUserPool pool = pool(registration);
        CognitoSrp srp = new CognitoSrp();
        Map<String, String> authParameters = new LinkedHashMap<>();
        authParameters.put(USERNAME, credentials.username());
        authParameters.put("SRP_A", srp.srpA());
        addSecretHash(authParameters, registration, credentials.username());
        try {
            JsonObject response = idpClient.initiateAuth(pool, registration.getClientId(), authParameters, credentials.userContextData());
            if (!PASSWORD_VERIFIER.equals(asString(response, "ChallengeName"))) {
                return toResult(response, credentials.username());
            }
            return verifyPassword(pool, registration, srp, credentials, response);
        } catch (CognitoIdpException ex) {
            throw normalized(ex);
        }
    }

    /**
     * Answers a surfaced challenge ({@code NEW_PASSWORD_REQUIRED}, {@code SOFTWARE_TOKEN_MFA},
     * {@code SMS_MFA}, {@code EMAIL_OTP}, {@code CUSTOM_CHALLENGE}, ...).
     *
     * @param registration the client registration the login runs against
     * @param answer the challenge answer
     * @return the tokens, or the next challenge Cognito requires
     */
    @Override
    public NativeLoginResult answerChallenge(ClientRegistration registration, NativeLoginChallengeAnswer answer) {
        CognitoUserPool pool = pool(registration);
        Map<String, String> responses = new LinkedHashMap<>(answer.responses());
        responses.put(USERNAME, answer.username());
        addSecretHash(responses, registration, answer.username());
        try {
            JsonObject response = idpClient.respondToAuthChallenge(pool, registration.getClientId(), answer.challenge(), answer.session(),
                    responses, answer.userContextData());
            return toResult(response, answer.username());
        } catch (CognitoIdpException ex) {
            throw normalized(ex);
        }
    }

    private NativeLoginResult verifyPassword(CognitoUserPool pool, ClientRegistration registration, CognitoSrp srp,
            NativeLoginCredentials credentials, JsonObject initiation) {
        JsonObject parameters = initiation.getAsJsonObject("ChallengeParameters");
        String userIdForSrp = parameters != null ? asString(parameters, "USER_ID_FOR_SRP") : null;
        String secretBlock = parameters != null ? asString(parameters, "SECRET_BLOCK") : null;
        String srpB = parameters != null ? asString(parameters, "SRP_B") : null;
        String salt = parameters != null ? asString(parameters, "SALT") : null;
        if (userIdForSrp == null || secretBlock == null || srpB == null || salt == null) {
            throw new NativeLoginException(NativeLoginException.Outcome.AUTHENTICATION_FAILED,
                    "Cognito answered the SRP initiation without the expected challenge parameters");
        }
        byte[] key = srp.passwordAuthenticationKey(pool.poolName(), userIdForSrp, credentials.password(), new BigInteger(srpB, 16),
                new BigInteger(salt, 16));
        String timestamp = CognitoSrp.timestamp(Instant.now());
        Map<String, String> responses = new LinkedHashMap<>();
        responses.put(USERNAME, userIdForSrp);
        responses.put("PASSWORD_CLAIM_SECRET_BLOCK", secretBlock);
        responses.put("PASSWORD_CLAIM_SIGNATURE",
                CognitoSrp.passwordClaimSignature(key, pool.poolName(), userIdForSrp, secretBlock, timestamp));
        responses.put("TIMESTAMP", timestamp);
        addSecretHash(responses, registration, userIdForSrp);
        JsonObject response = idpClient.respondToAuthChallenge(pool, registration.getClientId(), PASSWORD_VERIFIER,
                asString(initiation, "Session"), responses, credentials.userContextData());
        return toResult(response, userIdForSrp);
    }

    private static NativeLoginResult toResult(JsonObject response, String canonicalUsername) {
        JsonObject authenticationResult = response.has("AuthenticationResult") && response.get("AuthenticationResult")
                                                                                          .isJsonObject()
                                                                                                  ? response.getAsJsonObject(
                                                                                                          "AuthenticationResult")
                                                                                                  : null;
        if (authenticationResult != null) {
            String idToken = asString(authenticationResult, "IdToken");
            String accessToken = asString(authenticationResult, "AccessToken");
            if (idToken == null || accessToken == null) {
                throw new NativeLoginException(NativeLoginException.Outcome.AUTHENTICATION_FAILED,
                        "Cognito answered the authentication without the expected tokens");
            }
            Long expiresIn = authenticationResult.has("ExpiresIn") && authenticationResult.get("ExpiresIn")
                                                                                          .isJsonPrimitive()
                                                                                                  ? authenticationResult.get("ExpiresIn")
                                                                                                                        .getAsLong()
                                                                                                  : null;
            return new NativeLoginTokens(idToken, accessToken, asString(authenticationResult, "RefreshToken"), expiresIn);
        }
        String challengeName = asString(response, "ChallengeName");
        if (challengeName == null) {
            throw new NativeLoginException(NativeLoginException.Outcome.AUTHENTICATION_FAILED,
                    "Cognito answered with neither tokens nor a challenge");
        }
        Map<String, String> parameters = new LinkedHashMap<>();
        JsonObject challengeParameters = response.has("ChallengeParameters") && response.get("ChallengeParameters")
                                                                                        .isJsonObject()
                                                                                                ? response.getAsJsonObject(
                                                                                                        "ChallengeParameters")
                                                                                                : null;
        if (challengeParameters != null) {
            challengeParameters.entrySet()
                               .forEach(entry -> parameters.put(entry.getKey(), entry.getValue()
                                                                                     .getAsString()));
        }
        // the follow-up must run for the canonical user Cognito resolved, not the typed alias
        parameters.putIfAbsent(USERNAME, canonicalUsername);
        return new NativeLoginChallenge(challengeName, asString(response, "Session"), parameters);
    }

    private static NativeLoginException normalized(CognitoIdpException exception) {
        String errorType = exception.getErrorType() != null ? exception.getErrorType() : "";
        NativeLoginException.Outcome outcome = switch (errorType) {
            // UserNotFoundException maps to the same outcome to avoid user enumeration
            case "NotAuthorizedException", "UserNotFoundException" -> NativeLoginException.Outcome.INVALID_CREDENTIALS;
            case "PasswordResetRequiredException" -> NativeLoginException.Outcome.PASSWORD_RESET_REQUIRED;
            case "UserNotConfirmedException" -> NativeLoginException.Outcome.USER_NOT_CONFIRMED;
            case "CodeMismatchException" -> NativeLoginException.Outcome.CODE_MISMATCH;
            case "ExpiredCodeException" -> NativeLoginException.Outcome.CODE_EXPIRED;
            case "InvalidPasswordException" -> NativeLoginException.Outcome.INVALID_PASSWORD;
            case "TooManyRequestsException", "TooManyFailedAttemptsException", "LimitExceededException" -> NativeLoginException.Outcome.TOO_MANY_ATTEMPTS;
            default -> NativeLoginException.Outcome.AUTHENTICATION_FAILED;
        };
        return new NativeLoginException(outcome, "Cognito refused the authentication with [" + errorType + "]", exception);
    }

    private static CognitoUserPool pool(ClientRegistration registration) {
        return CognitoUserPool.fromIssuer(registration.getProviderDetails()
                                                      .getIssuerUri());
    }

    private static void addSecretHash(Map<String, String> parameters, ClientRegistration registration, String username) {
        if (StringUtils.hasText(registration.getClientSecret())) {
            parameters.put("SECRET_HASH", CognitoSrp.secretHash(registration.getClientId(), registration.getClientSecret(), username));
        }
    }

    private static String asString(JsonObject object, String member) {
        return object.has(member) && object.get(member)
                                           .isJsonPrimitive() ? object.get(member)
                                                                      .getAsString()
                                                   : null;
    }
}
