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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

/**
 * Thin client for the unauthenticated {@code cognito-idp} authentication APIs
 * ({@code InitiateAuth}, {@code RespondToAuthChallenge}) over their JSON wire protocol - the calls
 * need no request signing and no AWS SDK.
 *
 * <p>
 * Request bodies carry authentication material (SRP proofs, challenge answers), so nothing here
 * logs them, and failures surface only the service error type plus its message.
 */
@Profile("cognito")
@Component
class CognitoIdpClient {

    private static final String TARGET_PREFIX = "AWSCognitoIdentityProviderService.";
    private static final String CONTENT_TYPE = "application/x-amz-json-1.1";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private static final Gson GSON = new Gson();

    private final HttpClient httpClient = HttpClient.newBuilder()
                                                    .connectTimeout(CONNECT_TIMEOUT)
                                                    .build();

    /**
     * Calls {@code InitiateAuth} with the {@code USER_SRP_AUTH} flow.
     *
     * @param pool the user pool
     * @param clientId the app client id
     * @param authParameters the auth parameters ({@code USERNAME}, {@code SRP_A}, optional
     *        {@code SECRET_HASH})
     * @param userContextData the optional client-side collector blob for threat protection
     * @return the service response
     * @throws CognitoIdpException when the service refuses the call or is unreachable
     */
    JsonObject initiateAuth(CognitoUserPool pool, String clientId, Map<String, String> authParameters, String userContextData) {
        JsonObject request = new JsonObject();
        request.addProperty("AuthFlow", "USER_SRP_AUTH");
        request.addProperty("ClientId", clientId);
        request.add("AuthParameters", toJson(authParameters));
        addUserContextData(request, userContextData);
        return call(pool, "InitiateAuth", request);
    }

    /**
     * Calls {@code RespondToAuthChallenge}.
     *
     * @param pool the user pool
     * @param clientId the app client id
     * @param challengeName the challenge being answered
     * @param session the opaque session state from the previous step
     * @param challengeResponses the challenge responses
     * @param userContextData the optional client-side collector blob for threat protection
     * @return the service response
     * @throws CognitoIdpException when the service refuses the call or is unreachable
     */
    JsonObject respondToAuthChallenge(CognitoUserPool pool, String clientId, String challengeName, String session,
            Map<String, String> challengeResponses, String userContextData) {
        JsonObject request = new JsonObject();
        request.addProperty("ChallengeName", challengeName);
        request.addProperty("ClientId", clientId);
        if (StringUtils.hasText(session)) {
            request.addProperty("Session", session);
        }
        request.add("ChallengeResponses", toJson(challengeResponses));
        addUserContextData(request, userContextData);
        return call(pool, "RespondToAuthChallenge", request);
    }

    private JsonObject call(CognitoUserPool pool, String operation, JsonObject request) {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                                             .uri(URI.create(pool.endpoint()))
                                             .timeout(REQUEST_TIMEOUT)
                                             .header("Content-Type", CONTENT_TYPE)
                                             .header("X-Amz-Target", TARGET_PREFIX + operation)
                                             .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(request), StandardCharsets.UTF_8))
                                             .build();
        HttpResponse<String> response;
        try {
            response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new CognitoIdpException(null, "The [" + operation + "] call to [" + pool.endpoint() + "] failed", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread()
                  .interrupt();
            throw new CognitoIdpException(null, "The [" + operation + "] call to [" + pool.endpoint() + "] was interrupted", ex);
        }
        if (response.statusCode() == 200) {
            return GSON.fromJson(response.body(), JsonObject.class);
        }
        throw toServiceException(operation, response);
    }

    private static CognitoIdpException toServiceException(String operation, HttpResponse<String> response) {
        String errorType = null;
        String message = null;
        try {
            JsonObject error = GSON.fromJson(response.body(), JsonObject.class);
            if (error != null) {
                errorType = asString(error, "__type");
                message = asString(error, "message");
                if (message == null) {
                    message = asString(error, "Message");
                }
            }
        } catch (JsonSyntaxException ex) {
            // a non-JSON error page from an intermediary - the status code is all there is
        }
        if (errorType != null && errorType.contains("#")) {
            errorType = errorType.substring(errorType.indexOf('#') + 1);
        }
        return new CognitoIdpException(errorType, "The [" + operation + "] call failed with status [" + response.statusCode()
                + "], error type [" + errorType + "]: " + message);
    }

    private static String asString(JsonObject object, String member) {
        return object.has(member) && object.get(member)
                                           .isJsonPrimitive() ? object.get(member)
                                                                      .getAsString()
                                                   : null;
    }

    private static JsonObject toJson(Map<String, String> parameters) {
        JsonObject json = new JsonObject();
        parameters.forEach(json::addProperty);
        return json;
    }

    private static void addUserContextData(JsonObject request, String userContextData) {
        if (StringUtils.hasText(userContextData)) {
            JsonObject contextData = new JsonObject();
            contextData.addProperty("EncodedData", userContextData);
            request.add("UserContextData", contextData);
        }
    }
}
