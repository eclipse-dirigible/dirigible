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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.base.http.access.BearerTokenAuthenticator;
import org.eclipse.dirigible.components.websockets.service.WebsocketProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.messaging.access.intercept.AuthorizationChannelInterceptor;
import org.springframework.security.messaging.context.SecurityContextChannelInterceptor;
import org.springframework.web.socket.config.annotation.SockJsServiceRegistration;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

/**
 * The configured origins reach both STOMP registrations before the SockJS one is created, and the
 * inbound channel authenticates before it authorizes.
 */
@SuppressWarnings("unchecked")
class WebsocketConfigTest {

    private final StompEndpointRegistry registry = mock(StompEndpointRegistry.class);
    private final StompWebSocketEndpointRegistration endpoint = mock(StompWebSocketEndpointRegistration.class);
    private final StompWebSocketEndpointRegistration sockJsEndpoint = mock(StompWebSocketEndpointRegistration.class);

    private WebsocketConfig config;

    @BeforeEach
    void setUp() {
        config = new WebsocketConfig(mock(WebsocketProcessor.class), mock(ObjectProvider.class));
        when(registry.addEndpoint("/stomp")).thenReturn(endpoint, sockJsEndpoint);
        when(sockJsEndpoint.withSockJS()).thenReturn(mock(SockJsServiceRegistration.class));
    }

    @AfterEach
    void clearConfiguration() {
        Configuration.remove(DirigibleConfig.CORS_ALLOWED_ORIGINS.getKey());
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
    void theBearerAuthenticatorIsResolvedLazily() {
        ObjectProvider<BearerTokenAuthenticator> provider = mock(ObjectProvider.class);

        new WebsocketConfig(mock(WebsocketProcessor.class), provider).configureClientInboundChannel(mock(ChannelRegistration.class));

        verify(provider, never()).getIfAvailable();
    }
}
