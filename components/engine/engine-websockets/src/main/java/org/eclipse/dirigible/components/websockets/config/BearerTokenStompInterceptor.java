/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.websockets.config;

import java.time.Instant;
import java.util.Map;

import org.eclipse.dirigible.components.base.http.access.AuthenticatedBearerToken;
import org.eclipse.dirigible.components.base.http.access.BearerTokenAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;

/**
 * Authenticates a STOMP session by the bearer token of its CONNECT frame.
 *
 * <p>
 * A CONNECT (or STOMP) frame carrying an {@code Authorization: Bearer} header is authenticated by
 * the same rules the HTTP chain applies to the token, and the resulting identity becomes the user
 * of the session - set on the frame's own header accessor, which at this point is still mutable and
 * carries the callback that records the user for every later frame of the session. The token's
 * expiry is kept in the session attributes, and once it passes every further frame the client sends
 * is refused; {@link BearerTokenStompSessionTerminator} ends the session at that instant as well,
 * so a client that only listens is not kept beyond it either. The connection lives no longer than
 * the token that opened it.
 *
 * <p>
 * A DISCONNECT is never refused: Spring sends one itself when the connection closes, so that the
 * broker drops the session's subscriptions, and a refusal would leave it holding them. A frame
 * without the header is left alone: a browser session is authenticated by the handshake. An
 * {@code Authorization} header on any other frame is ignored. On a profile without a
 * {@link BearerTokenAuthenticator} a bearer CONNECT is refused. The header value is never logged.
 */
class BearerTokenStompInterceptor implements ChannelInterceptor {

    /** The session attribute holding the {@link Instant} the token of a bearer session expires. */
    static final String EXPIRES_AT_ATTRIBUTE = BearerTokenStompInterceptor.class.getName() + ".EXPIRES_AT";

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BearerTokenStompInterceptor.class);

    private static final String BEARER_PREFIX = "Bearer ";

    private final ObjectProvider<BearerTokenAuthenticator> authenticator;

    private final ObjectProvider<BearerTokenStompSessionTerminator> sessionTerminator;

    /**
     * Instantiates the interceptor.
     *
     * @param authenticator the authenticator of bearer tokens, present on the profiles that accept them
     * @param sessionTerminator ends a bearer session when its token expires - resolved on first use,
     *        because it depends on the broker beans that the configuration registering this interceptor
     *        contributes to
     */
    BearerTokenStompInterceptor(ObjectProvider<BearerTokenAuthenticator> authenticator,
            ObjectProvider<BearerTokenStompSessionTerminator> sessionTerminator) {
        this.authenticator = authenticator;
        this.sessionTerminator = sessionTerminator;
    }

    /**
     * Pre send.
     *
     * @param message the frame
     * @param channel the inbound channel
     * @return the frame, with the authenticated user set on a bearer CONNECT
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }
        StompCommand command = accessor.getCommand();
        if (command == StompCommand.CONNECT || command == StompCommand.STOMP) {
            String authorization = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
            if (authorization != null) {
                authenticate(accessor, authorization);
            }
            return message;
        }
        if (command == StompCommand.DISCONNECT) {
            sessionTerminator.getObject()
                             .sessionEnded(accessor.getSessionId());
            return message;
        }
        refuseWhenExpired(accessor.getSessionAttributes());
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor, String authorization) {
        if (!accessor.isMutable()) {
            // Spring's STOMP handler keeps the CONNECT accessor mutable until the immutable-headers
            // interceptor it appends last has run. A user set on a copy would reach this frame only
            // and every later frame of the session would run anonymous - so refuse to wire it up
            // half way rather than authenticate a single frame
            throw new IllegalStateException("The CONNECT frame of the STOMP session [" + accessor.getSessionId()
                    + "] carries immutable headers, so the bearer identity cannot be recorded for the session");
        }
        if (!authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            throw new BadCredentialsException("Unsupported authorization scheme");
        }
        String token = authorization.substring(BEARER_PREFIX.length())
                                    .trim();
        if (token.isEmpty()) {
            throw new BadCredentialsException("Empty bearer token");
        }
        BearerTokenAuthenticator bearerTokenAuthenticator = authenticator.getIfAvailable();
        if (bearerTokenAuthenticator == null) {
            throw new AuthenticationServiceException("Bearer tokens are not supported by the active security profile");
        }
        AuthenticatedBearerToken authenticated = bearerTokenAuthenticator.authenticate(token);
        String user = authenticated.authentication()
                                   .getName();
        Instant expiresAt = authenticated.expiresAt();
        if (expiresAt != null) {
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes != null) {
                sessionAttributes.put(EXPIRES_AT_ATTRIBUTE, expiresAt);
            }
            sessionTerminator.getObject()
                             .endAt(accessor.getSessionId(), user, expiresAt);
        }
        LOGGER.debug("Authenticated the STOMP session [{}] of user [{}] by a bearer token", accessor.getSessionId(), user);
        accessor.setUser(authenticated.authentication());
    }

    private static void refuseWhenExpired(Map<String, Object> sessionAttributes) {
        if (sessionAttributes != null && sessionAttributes.get(EXPIRES_AT_ATTRIBUTE) instanceof Instant expiresAt && !Instant.now()
                                                                                                                             .isBefore(
                                                                                                                                     expiresAt)) {
            throw new CredentialsExpiredException("The bearer token of the session expired");
        }
    }
}
