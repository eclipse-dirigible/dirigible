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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.dirigible.components.base.http.access.AuthenticatedBearerToken;
import org.eclipse.dirigible.components.base.http.access.BearerTokenAuthenticator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

/**
 * A bearer CONNECT authenticates the session in place, an expired token ends it, and everything
 * else is left to the handshake.
 */
class BearerTokenStompInterceptorTest {

    private static final String TOKEN = "eyJhbGciOiJSUzI1NiJ9.token";

    private final ObjectProvider<BearerTokenAuthenticator> authenticatorProvider = mock(ObjectProvider.class);
    private final BearerTokenAuthenticator authenticator = mock(BearerTokenAuthenticator.class);
    private final MessageChannel channel = mock(MessageChannel.class);
    private final Authentication jane = new TestingAuthenticationToken("jane", "n/a", "ROLE_DEVELOPER");
    private final Map<String, Object> sessionAttributes = new ConcurrentHashMap<>();

    private BearerTokenStompInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new BearerTokenStompInterceptor(authenticatorProvider);
    }

    @Test
    void aConnectWithoutAuthorizationIsLeftAlone() {
        Message<byte[]> connect = frame(StompCommand.CONNECT, null, true);

        assertSame(connect, interceptor.preSend(connect, channel));
        assertNull(SimpMessageHeaderAccessor.getUser(connect.getHeaders()));
        verifyNoInteractions(authenticatorProvider);
    }

    @Test
    void aBearerConnectSetsTheUserInPlaceAndRecordsTheExpiry() {
        Instant expiresAt = Instant.now()
                                   .plusSeconds(300);
        when(authenticatorProvider.getIfAvailable()).thenReturn(authenticator);
        when(authenticator.authenticate(TOKEN)).thenReturn(new AuthenticatedBearerToken(jane, expiresAt));
        Message<byte[]> connect = frame(StompCommand.CONNECT, "Bearer " + TOKEN, true);

        Message<?> result = interceptor.preSend(connect, channel);

        assertSame(connect, result, "the CONNECT accessor is mutable, so the user goes on the frame itself");
        assertSame(jane, SimpMessageHeaderAccessor.getUser(result.getHeaders()));
        assertEquals(expiresAt, sessionAttributes.get(BearerTokenStompInterceptor.EXPIRES_AT_ATTRIBUTE));
    }

    @Test
    void anImmutableConnectGetsTheUserOnACopy() {
        when(authenticatorProvider.getIfAvailable()).thenReturn(authenticator);
        when(authenticator.authenticate(TOKEN)).thenReturn(new AuthenticatedBearerToken(jane, null));
        Message<byte[]> connect = frame(StompCommand.CONNECT, "bearer " + TOKEN, false);

        Message<?> result = interceptor.preSend(connect, channel);

        assertNotSame(connect, result);
        assertSame(jane, SimpMessageHeaderAccessor.getUser(result.getHeaders()));
        assertNull(sessionAttributes.get(BearerTokenStompInterceptor.EXPIRES_AT_ATTRIBUTE), "a token without expiry records none");
    }

    @Test
    void theStompFrameIsTreatedLikeConnect() {
        when(authenticatorProvider.getIfAvailable()).thenReturn(authenticator);
        when(authenticator.authenticate(TOKEN)).thenReturn(new AuthenticatedBearerToken(jane, null));
        Message<byte[]> stomp = frame(StompCommand.STOMP, "Bearer " + TOKEN, true);

        assertSame(jane, SimpMessageHeaderAccessor.getUser(interceptor.preSend(stomp, channel)
                                                                      .getHeaders()));
    }

    @Test
    void authorizationOnAnyOtherFrameIsIgnored() {
        Message<byte[]> send = frame(StompCommand.SEND, "Bearer " + TOKEN, true);

        assertSame(send, interceptor.preSend(send, channel));
        assertNull(SimpMessageHeaderAccessor.getUser(send.getHeaders()));
        verifyNoInteractions(authenticatorProvider);
    }

    @Test
    void anotherSchemeIsBadCredentials() {
        Message<byte[]> connect = frame(StompCommand.CONNECT, "Basic YWRtaW46YWRtaW4=", true);

        assertThrows(BadCredentialsException.class, () -> interceptor.preSend(connect, channel));
        verifyNoInteractions(authenticatorProvider);
    }

    @Test
    void anEmptyTokenIsBadCredentials() {
        Message<byte[]> connect = frame(StompCommand.CONNECT, "Bearer   ", true);

        assertThrows(BadCredentialsException.class, () -> interceptor.preSend(connect, channel));
    }

    @Test
    void withoutAnAuthenticatorABearerConnectIsRefused() {
        when(authenticatorProvider.getIfAvailable()).thenReturn(null);
        Message<byte[]> connect = frame(StompCommand.CONNECT, "Bearer " + TOKEN, true);

        assertThrows(AuthenticationServiceException.class, () -> interceptor.preSend(connect, channel));
    }

    @Test
    void aRefusedTokenPropagatesTheRefusal() {
        when(authenticatorProvider.getIfAvailable()).thenReturn(authenticator);
        when(authenticator.authenticate(TOKEN)).thenThrow(new BadCredentialsException("expired"));
        Message<byte[]> connect = frame(StompCommand.CONNECT, "Bearer " + TOKEN, true);

        assertThrows(BadCredentialsException.class, () -> interceptor.preSend(connect, channel));
    }

    @Test
    void framesOfASessionWhoseTokenExpiredAreRefused() {
        sessionAttributes.put(BearerTokenStompInterceptor.EXPIRES_AT_ATTRIBUTE, Instant.now()
                                                                                       .minusSeconds(1));

        assertThrows(CredentialsExpiredException.class, () -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, null, true), channel));
        assertThrows(CredentialsExpiredException.class, () -> interceptor.preSend(frame(StompCommand.SEND, null, true), channel));
    }

    @Test
    void framesOfASessionWhoseTokenIsValidPass() {
        sessionAttributes.put(BearerTokenStompInterceptor.EXPIRES_AT_ATTRIBUTE, Instant.now()
                                                                                       .plusSeconds(300));
        Message<byte[]> subscribe = frame(StompCommand.SUBSCRIBE, null, true);

        assertSame(subscribe, interceptor.preSend(subscribe, channel));
    }

    private Message<byte[]> frame(StompCommand command, String authorization, boolean mutable) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setSessionId("session-1");
        accessor.setSessionAttributes(sessionAttributes);
        if (authorization != null) {
            accessor.setNativeHeader("Authorization", authorization);
        }
        accessor.setLeaveMutable(mutable);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
