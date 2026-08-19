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

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * First-party sign-in endpoint. Authenticates the user server-side against the identity provider
 * through the active profile's {@link NativeLoginProvider} and establishes the standard platform
 * session, so an application can own its login UX instead of redirecting to the provider-hosted
 * page. On profiles without a provider (basic, github, ...) the endpoint answers 404 and nothing
 * changes.
 *
 * <p>
 * The contract is a challenge round-trip, not success/failure only: {@code POST /login/native}
 * takes the credentials and answers either {@code AUTHENTICATED} (session cookie set),
 * {@code CHALLENGE} (with the provider's challenge name, opaque session state and parameters - the
 * answer goes to {@code POST /login/native/challenge}), or a normalized failure outcome. Provider
 * raw messages never reach the client.
 *
 * <p>
 * Credential-handling discipline: the request bodies carry the raw password, so nothing here logs
 * them, puts them into exceptions or retains them after the provider call returns. Cross-site
 * request forgery is countered by accepting only {@code application/json} bodies - a cross-origin
 * browser cannot produce that content type without a CORS preflight, which this endpoint never
 * grants.
 */
@RestController
class NativeLoginEndpoint {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(NativeLoginEndpoint.class);

    private final ObjectProvider<NativeLoginProvider> loginProvider;
    private final ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository;
    private final NativeLoginSessionInitializer sessionInitializer;

    NativeLoginEndpoint(ObjectProvider<NativeLoginProvider> loginProvider,
            ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository, NativeLoginSessionInitializer sessionInitializer) {
        this.loginProvider = loginProvider;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.sessionInitializer = sessionInitializer;
    }

    /**
     * Authenticates the user with the identity provider and mints the platform session.
     *
     * @param body the credentials
     * @param request the request
     * @param response the response
     * @return the normalized outcome - {@code AUTHENTICATED} or a {@code CHALLENGE} to answer on
     *         {@link #challenge(ChallengeRequest, HttpServletRequest, HttpServletResponse)}
     */
    @PostMapping(path = "/login/native", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest body, HttpServletRequest request,
            HttpServletResponse response) {
        NativeLoginProvider provider = requireProvider();
        if (!StringUtils.hasText(body.username()) || !StringUtils.hasText(body.password())) {
            throw new NativeLoginException(NativeLoginException.Outcome.INVALID_REQUEST, "Missing username or password");
        }
        ClientRegistration registration = resolveRegistration(provider, body.registrationId());
        NativeLoginResult result =
                provider.authenticate(registration, new NativeLoginCredentials(body.username(), body.password(), body.userContextData()));
        return respond(registration, result, request, response);
    }

    /**
     * Answers a challenge returned by a previous step and mints the platform session when the provider
     * is satisfied.
     *
     * @param body the challenge answer
     * @param request the request
     * @param response the response
     * @return the normalized outcome - {@code AUTHENTICATED} or the next {@code CHALLENGE}
     */
    @PostMapping(path = "/login/native/challenge", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> challenge(@RequestBody ChallengeRequest body, HttpServletRequest request,
            HttpServletResponse response) {
        NativeLoginProvider provider = requireProvider();
        if (!StringUtils.hasText(body.challenge()) || !StringUtils.hasText(body.session()) || !StringUtils.hasText(body.username())) {
            throw new NativeLoginException(NativeLoginException.Outcome.INVALID_REQUEST, "Missing challenge, session or username");
        }
        ClientRegistration registration = resolveRegistration(provider, body.registrationId());
        Map<String, String> responses = body.responses() != null ? body.responses() : Map.of();
        NativeLoginResult result = provider.answerChallenge(registration,
                new NativeLoginChallengeAnswer(body.challenge(), body.session(), body.username(), responses, body.userContextData()));
        return respond(registration, result, request, response);
    }

    /**
     * Translates a failed login step into its normalized outcome - the response body never carries
     * provider-raw messages.
     *
     * @param exception the failure
     * @return the outcome response
     */
    @ExceptionHandler(NativeLoginException.class)
    public ResponseEntity<Map<String, Object>> onLoginFailure(NativeLoginException exception) {
        LOGGER.debug("Native login failed with outcome [{}]", exception.getOutcome(), exception);
        return ResponseEntity.status(statusOf(exception.getOutcome()))
                             .body(Map.of("outcome", exception.getOutcome()
                                                              .name()));
    }

    private NativeLoginProvider requireProvider() {
        NativeLoginProvider provider = loginProvider.getIfAvailable();
        if (provider == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Native login is not supported by the active security profile");
        }
        return provider;
    }

    private ClientRegistration resolveRegistration(NativeLoginProvider provider, String registrationId) {
        String effectiveId = StringUtils.hasText(registrationId) ? registrationId : provider.getDefaultRegistrationId();
        ClientRegistrationRepository repository = clientRegistrationRepository.getIfAvailable();
        ClientRegistration registration = repository != null ? repository.findByRegistrationId(effectiveId) : null;
        if (registration == null) {
            throw new NativeLoginException(NativeLoginException.Outcome.INVALID_REQUEST,
                    "Unknown client registration [" + effectiveId + "]");
        }
        return registration;
    }

    private ResponseEntity<Map<String, Object>> respond(ClientRegistration registration, NativeLoginResult result,
            HttpServletRequest request, HttpServletResponse response) {
        if (result instanceof NativeLoginTokens tokens) {
            sessionInitializer.establishSession(registration, tokens, request, response);
            return ResponseEntity.ok(Map.of("outcome", "AUTHENTICATED"));
        }
        NativeLoginChallenge challenge = (NativeLoginChallenge) result;
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("outcome", "CHALLENGE");
        payload.put("challenge", challenge.name());
        payload.put("session", challenge.session());
        payload.put("parameters", challenge.parameters() != null ? challenge.parameters() : Map.of());
        return ResponseEntity.ok(payload);
    }

    private static HttpStatus statusOf(NativeLoginException.Outcome outcome) {
        return switch (outcome) {
            case INVALID_REQUEST -> HttpStatus.BAD_REQUEST;
            case TOO_MANY_ATTEMPTS -> HttpStatus.TOO_MANY_REQUESTS;
            default -> HttpStatus.UNAUTHORIZED;
        };
    }

    /**
     * The sign-in request.
     *
     * @param registrationId the client registration to log in against; the provider's default when
     *        omitted
     * @param username the username
     * @param password the password
     * @param userContextData the optional provider-specific client-side collector blob
     */
    record LoginRequest(String registrationId, String username, String password, String userContextData) {

        /**
         * Keeps the password out of the record's generated string form.
         *
         * @return the string form without credential material
         */
        @Override
        public String toString() {
            return "LoginRequest[registrationId=" + registrationId + ", username=" + username + "]";
        }
    }

    /**
     * The challenge-answer request.
     *
     * @param registrationId the client registration to log in against; the provider's default when
     *        omitted
     * @param challenge the challenge name being answered
     * @param session the opaque provider session state from the challenge response
     * @param username the username the challenge round-trip runs for
     * @param responses the challenge response parameters
     * @param userContextData the optional provider-specific client-side collector blob
     */
    record ChallengeRequest(String registrationId, String challenge, String session, String username, Map<String, String> responses,
            String userContextData) {

        /**
         * Keeps the possibly credential-bearing responses out of the record's generated string form.
         *
         * @return the string form without credential material
         */
        @Override
        public String toString() {
            return "ChallengeRequest[registrationId=" + registrationId + ", challenge=" + challenge + ", username=" + username + "]";
        }
    }
}
