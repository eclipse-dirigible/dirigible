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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.base.http.access.BearerTokenAuthenticator;
import org.eclipse.dirigible.components.websockets.service.WebsocketProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.messaging.access.intercept.AuthorizationChannelInterceptor;
import org.springframework.security.messaging.context.SecurityContextChannelInterceptor;
import org.springframework.web.socket.config.annotation.SockJsServiceRegistration;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

/**
 * The cross-origin marker and the configured origins that name a host reach both STOMP
 * registrations before the SockJS one is created - a wildcard never does - and the inbound channel
 * authenticates before it authorizes, holding a cross-origin CONNECT to the credentials setting.
 */
@SuppressWarnings("unchecked")
class WebsocketConfigTest {

    private final StompEndpointRegistry registry = mock(StompEndpointRegistry.class);
    private final StompWebSocketEndpointRegistration endpoint = mock(StompWebSocketEndpointRegistration.class);
    private final StompWebSocketEndpointRegistration sockJsEndpoint = mock(StompWebSocketEndpointRegistration.class);

    private WebsocketConfig config;

    @BeforeEach
    void setUp() {
        config = new WebsocketConfig(mock(WebsocketProcessor.class), mock(ObjectProvider.class), mock(ObjectProvider.class));
        when(registry.addEndpoint("/stomp")).thenReturn(endpoint, sockJsEndpoint);
        when(sockJsEndpoint.withSockJS()).thenReturn(mock(SockJsServiceRegistration.class));
    }

    @AfterEach
    void clearConfiguration() {
        Configuration.remove(DirigibleConfig.CORS_ALLOWED_ORIGINS.getKey());
        Configuration.remove(DirigibleConfig.CORS_ALLOW_CREDENTIALS.getKey());
    }

    @Test
    void unconfiguredKeepsTheSameOriginHandshake() {
        config.registerStompEndpoints(registry);

        verify(endpoint, never()).setAllowedOriginPatterns(any(String[].class));
        verify(sockJsEndpoint, never()).setAllowedOriginPatterns(any(String[].class));
        verify(sockJsEndpoint).withSockJS();
        verify(registry).setErrorHandler(any(BearerTokenStompErrorHandler.class));
    }

    @Test
    void theCrossOriginMarkerReachesBothRegistrationsBeforeSockJsIsCreated() {
        // the SockJS registration copies the interceptors when it is created, like the patterns
        config.registerStompEndpoints(registry);

        verify(endpoint).addInterceptors(any(CrossOriginHandshakeInterceptor.class));
        InOrder inOrder = inOrder(sockJsEndpoint);
        inOrder.verify(sockJsEndpoint)
               .addInterceptors(any(CrossOriginHandshakeInterceptor.class));
        inOrder.verify(sockJsEndpoint)
               .withSockJS();
    }

    @Test
    void configuredOriginsReachBothRegistrationsBeforeSockJsIsCreated() {
        DirigibleConfig.CORS_ALLOWED_ORIGINS.setStringValue("https://app.example.com, capacitor://localhost");

        config.registerStompEndpoints(registry);

        verify(endpoint).setAllowedOriginPatterns("https://app.example.com", "capacitor://localhost");
        InOrder inOrder = inOrder(sockJsEndpoint);
        inOrder.verify(sockJsEndpoint)
               .setAllowedOriginPatterns("https://app.example.com", "capacitor://localhost");
        inOrder.verify(sockJsEndpoint)
               .withSockJS();
    }

    @ParameterizedTest
    @ValueSource(strings = {"*", "https://*", "https://**", "https://*.*"})
    void anEveryOriginPatternKeepsTheSameOriginHandshake(String origins) {
        // a wildcard serves bearer clients over HTTP; a WebSocket handshake carries the session cookie,
        // so applying it to the STOMP endpoint would let any page open a session as a logged in user -
        // however the wildcard is spelled
        DirigibleConfig.CORS_ALLOWED_ORIGINS.setStringValue(origins);

        config.registerStompEndpoints(registry);

        verify(endpoint, never()).setAllowedOriginPatterns(any(String[].class));
        verify(sockJsEndpoint, never()).setAllowedOriginPatterns(any(String[].class));
        verify(sockJsEndpoint).withSockJS();
    }

    @Test
    void onlyTheOriginsNamingAHostReachBothRegistrations() {
        DirigibleConfig.CORS_ALLOWED_ORIGINS.setStringValue("https://*, https://app.example.com, h*");

        config.registerStompEndpoints(registry);

        verify(endpoint).setAllowedOriginPatterns("https://app.example.com");
        InOrder inOrder = inOrder(sockJsEndpoint);
        inOrder.verify(sockJsEndpoint)
               .setAllowedOriginPatterns("https://app.example.com");
        inOrder.verify(sockJsEndpoint)
               .withSockJS();
    }

    @Test
    void theInboundChannelAuthenticatesThenAuthorizes() {
        ChannelRegistration registration = mock(ChannelRegistration.class);
        ArgumentCaptor<ChannelInterceptor[]> interceptors = ArgumentCaptor.forClass(ChannelInterceptor[].class);

        config.configureClientInboundChannel(registration);

        verify(registration).interceptors(interceptors.capture());
        ChannelInterceptor[] chain = interceptors.getValue();
        assertEquals(3, chain.length);
        assertInstanceOf(BearerTokenStompInterceptor.class, chain[0]);
        assertInstanceOf(SecurityContextChannelInterceptor.class, chain[1]);
        assertInstanceOf(AuthorizationChannelInterceptor.class, chain[2]);
    }

    @Test
    void theConnectGateReadsTheCredentialsSettingWhenTheChannelIsConfigured() {
        DirigibleConfig.CORS_ALLOWED_ORIGINS.setStringValue("https://app.example.com");
        Message<byte[]> connect = crossOriginConnect();

        assertThrows(InsufficientAuthenticationException.class, () -> connectGate().preSend(connect, mock(MessageChannel.class)),
                "credentials off: a listed origin gets CORS, not the handshake identity");

        DirigibleConfig.CORS_ALLOW_CREDENTIALS.setBooleanValue(true);
        assertSame(connect, connectGate().preSend(connect, mock(MessageChannel.class)), "credentials on: the handshake identity applies");
    }

    private ChannelInterceptor connectGate() {
        ChannelRegistration registration = mock(ChannelRegistration.class);
        ArgumentCaptor<ChannelInterceptor[]> interceptors = ArgumentCaptor.forClass(ChannelInterceptor[].class);
        config.configureClientInboundChannel(registration);
        verify(registration).interceptors(interceptors.capture());
        return interceptors.getValue()[0];
    }

    private static Message<byte[]> crossOriginConnect() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionId("session-1");
        accessor.setSessionAttributes(Map.of(CrossOriginHandshakeInterceptor.CROSS_ORIGIN_ATTRIBUTE, "https://app.example.com"));
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    void theBearerAuthenticatorIsResolvedLazily() {
        ObjectProvider<BearerTokenAuthenticator> provider = mock(ObjectProvider.class);

        ObjectProvider<BearerTokenStompSessionTerminator> terminator = mock(ObjectProvider.class);

        new WebsocketConfig(mock(WebsocketProcessor.class), provider, terminator).configureClientInboundChannel(
                mock(ChannelRegistration.class));

        verify(provider, never()).getIfAvailable();
        verify(terminator, never()).getObject();
    }
}
