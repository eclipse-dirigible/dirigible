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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
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
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

/**
 * A bearer CONNECT authenticates the session in place and gives it the token's deadline, an expired
 * token refuses what the client sends, a DISCONNECT is never refused, and everything else is left
 * to the handshake - unless the session was opened cross-origin and credentials are not allowed, in
 * which case the handshake identity is refused.
 */
class BearerTokenStompInterceptorTest {

    private static final String TOKEN = "eyJhbGciOiJSUzI1NiJ9.token";
    private static final String ORIGIN = "https://app.example.com";

    private final ObjectProvider<BearerTokenAuthenticator> authenticatorProvider = mock(ObjectProvider.class);
    private final BearerTokenAuthenticator authenticator = mock(BearerTokenAuthenticator.class);
    private final ObjectProvider<BearerTokenStompSessionTerminator> terminatorProvider = mock(ObjectProvider.class);
    private final BearerTokenStompSessionTerminator terminator = mock(BearerTokenStompSessionTerminator.class);
    private final MessageChannel channel = mock(MessageChannel.class);
    private final Authentication jane = new TestingAuthenticationToken("jane", "n/a", "ROLE_DEVELOPER");
    private final Map<String, Object> sessionAttributes = new ConcurrentHashMap<>();

    private BearerTokenStompInterceptor interceptor;

    @BeforeEach
    void setUp() {
        when(terminatorProvider.getObject()).thenReturn(terminator);
        interceptor = new BearerTokenStompInterceptor(authenticatorProvider, terminatorProvider, false);
    }

    @Test
    void aConnectWithoutAuthorizationIsLeftAlone() {
        // a same-origin session - every page the platform serves - is authenticated by its handshake
        Message<byte[]> connect = frame(StompCommand.CONNECT, null, true);

        assertSame(connect, interceptor.preSend(connect, channel));
        assertNull(SimpMessageHeaderAccessor.getUser(connect.getHeaders()));
        verifyNoInteractions(authenticatorProvider);
    }

    @Test
    void aCrossOriginConnectWithoutAuthorizationIsRefusedUnlessCredentialsAreAllowed() {
        // the handshake carried the cookie, and a listed origin gets CORS, not the user's session
        sessionAttributes.put(CrossOriginHandshakeInterceptor.CROSS_ORIGIN_ATTRIBUTE, ORIGIN);
        Message<byte[]> connect = frame(StompCommand.CONNECT, null, true);

        InsufficientAuthenticationException refusal =
                assertThrows(InsufficientAuthenticationException.class, () -> interceptor.preSend(connect, channel));

        assertTrue(refusal.getMessage()
                          .contains(ORIGIN),
                "the log names the origin");
        assertTrue(refusal.getMessage()
                          .contains(DirigibleConfig.CORS_ALLOW_CREDENTIALS.getKey()),
                "and the key that decides");
        verifyNoInteractions(authenticatorProvider);
    }

    @Test
    void aCrossOriginStompFrameIsHeldToTheSameRule() {
        sessionAttributes.put(CrossOriginHandshakeInterceptor.CROSS_ORIGIN_ATTRIBUTE, ORIGIN);

        assertThrows(InsufficientAuthenticationException.class, () -> interceptor.preSend(frame(StompCommand.STOMP, null, true), channel));
    }

    @Test
    void aCrossOriginConnectIsLeftAloneWhenCredentialsAreAllowed() {
        BearerTokenStompInterceptor credentialed = new BearerTokenStompInterceptor(authenticatorProvider, terminatorProvider, true);
        sessionAttributes.put(CrossOriginHandshakeInterceptor.CROSS_ORIGIN_ATTRIBUTE, ORIGIN);
        Message<byte[]> connect = frame(StompCommand.CONNECT, null, true);

        assertSame(connect, credentialed.preSend(connect, channel));
        verifyNoInteractions(authenticatorProvider);
    }

    @Test
    void aCrossOriginBearerConnectAuthenticatesTheToken() {
        // the token is the identity such a session is meant to carry, whatever the handshake had
        when(authenticatorProvider.getIfAvailable()).thenReturn(authenticator);
        when(authenticator.authenticate(TOKEN)).thenReturn(new AuthenticatedBearerToken(jane, null));
        sessionAttributes.put(CrossOriginHandshakeInterceptor.CROSS_ORIGIN_ATTRIBUTE, ORIGIN);
        Message<byte[]> connect = frame(StompCommand.CONNECT, "Bearer " + TOKEN, true);

        assertSame(jane, SimpMessageHeaderAccessor.getUser(interceptor.preSend(connect, channel)
                                                                      .getHeaders()));
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
        verify(terminator).endAt("session-1", "jane", expiresAt);
    }

    @Test
    void anImmutableConnectIsRefusedRatherThanHalfAuthenticated() {
        // not the shape Spring's STOMP handler produces: a user set on a copy would reach this frame
        // only, and every later frame of the session would run anonymous
        Message<byte[]> connect = frame(StompCommand.CONNECT, "bearer " + TOKEN, false);

        assertThrows(IllegalStateException.class, () -> interceptor.preSend(connect, channel));
        verifyNoInteractions(authenticatorProvider);
    }

    @Test
    void theStompFrameIsTreatedLikeConnect() {
        when(authenticatorProvider.getIfAvailable()).thenReturn(authenticator);
        when(authenticator.authenticate(TOKEN)).thenReturn(new AuthenticatedBearerToken(jane, null));
        Message<byte[]> stomp = frame(StompCommand.STOMP, "Bearer " + TOKEN, true);

        assertSame(jane, SimpMessageHeaderAccessor.getUser(interceptor.preSend(stomp, channel)
                                                                      .getHeaders()));
        assertNull(sessionAttributes.get(BearerTokenStompInterceptor.EXPIRES_AT_ATTRIBUTE), "a token without expiry records none");
        verifyNoInteractions(terminator);
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
    void aDisconnectIsNeverRefusedAndEndsTheDeadline() {
        // Spring sends a DISCONNECT itself when the connection closes, so that the broker drops the
        // session's subscriptions - refused, the broker would keep them for good
        sessionAttributes.put(BearerTokenStompInterceptor.EXPIRES_AT_ATTRIBUTE, Instant.now()
                                                                                       .minusSeconds(1));
        Message<byte[]> disconnect = frame(StompCommand.DISCONNECT, null, true);

        assertSame(disconnect, interceptor.preSend(disconnect, channel));
        verify(terminator).sessionEnded("session-1");
    }

    @Test
    void aDisconnectOfASessionWithoutATokenIsLeftAlone() {
        Message<byte[]> disconnect = frame(StompCommand.DISCONNECT, null, true);

        assertSame(disconnect, interceptor.preSend(disconnect, channel));
        verify(terminator).sessionEnded("session-1");
        verifyNoInteractions(authenticatorProvider);
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
