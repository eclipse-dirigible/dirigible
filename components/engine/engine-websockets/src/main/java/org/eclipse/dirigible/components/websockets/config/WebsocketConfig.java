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

import java.util.List;

import org.eclipse.dirigible.components.base.http.access.BearerTokenAuthenticator;
import org.eclipse.dirigible.components.base.http.access.CorsConfigurationSourceProvider;
import org.eclipse.dirigible.components.websockets.service.WebsocketProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.messaging.access.intercept.AuthorizationChannelInterceptor;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;
import org.springframework.security.messaging.context.SecurityContextChannelInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * The STOMP broker of the platform: the {@code /stomp} endpoint (raw and SockJS), the simple broker
 * behind {@code /queue} and {@code /topic}, and the rules a client is held to.
 *
 * <p>
 * A client is either the browser session that opened the handshake or a bearer token presented in
 * the {@code Authorization} header of the CONNECT frame - the shape an external frontend uses,
 * since a native application has no cookie and a cross-origin page cannot set headers on a
 * WebSocket handshake. Every frame is then authorized: a CONNECT needs a principal, a client may
 * subscribe to its own {@code /user/queue} destinations only and send to the application
 * destinations under {@code /ws} only. Nothing else passes - in particular no anonymous client, no
 * subscription to a queue the broker addresses another user by, and no direct publish to a broker
 * destination.
 *
 * <p>
 * The handshake accepts the configured cross-origin origins; unconfigured it stays same-origin, as
 * it always was.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebsocketConfig implements WebSocketMessageBrokerConfigurer {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(WebsocketConfig.class);

    /** The processor. */
    private final WebsocketProcessor processor;

    private final ObjectProvider<BearerTokenAuthenticator> bearerTokenAuthenticator;

    /**
     * Instantiates a new websocket config.
     *
     * @param processor the processor
     * @param bearerTokenAuthenticator the authenticator of bearer tokens, present on the profiles that
     *        accept them
     */
    public WebsocketConfig(WebsocketProcessor processor, ObjectProvider<BearerTokenAuthenticator> bearerTokenAuthenticator) {
        this.processor = processor;
        this.bearerTokenAuthenticator = bearerTokenAuthenticator;
    }

    /**
     * Gets the processor.
     *
     * @return the processor
     */
    public WebsocketProcessor getProcessor() {
        return processor;
    }

    /**
     * Configure message broker.
     *
     * @param config the config
     */
    @Override
    public void configureMessageBroker(final MessageBrokerRegistry config) {
        config.enableSimpleBroker("/queue/", "/topic/");
        config.setApplicationDestinationPrefixes("/ws");
    }

    /**
     * Register stomp endpoints.
     *
     * @param registry the registry
     */
    @Override
    public void registerStompEndpoints(final StompEndpointRegistry registry) {
        registry.setErrorHandler(new BearerTokenStompErrorHandler());
        StompWebSocketEndpointRegistration endpoint = registry.addEndpoint("/stomp");
        StompWebSocketEndpointRegistration sockJsEndpoint = registry.addEndpoint("/stomp");
        if (CorsConfigurationSourceProvider.isConfigured()) {
            List<String> origins = CorsConfigurationSourceProvider.allowedOriginPatterns();
            LOGGER.info("The STOMP handshake accepts the origins {}.", origins);
            String[] originPatterns = origins.toArray(String[]::new);
            endpoint.setAllowedOriginPatterns(originPatterns);
            // the SockJS registration copies the patterns when it is created, so they go on first
            sockJsEndpoint.setAllowedOriginPatterns(originPatterns);
        }
        sockJsEndpoint.withSockJS();
    }

    /**
     * Puts the bearer authentication and the frame authorization in front of everything the broker and
     * the controllers do with a client frame.
     *
     * @param registration the inbound channel registration
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new BearerTokenStompInterceptor(bearerTokenAuthenticator), new SecurityContextChannelInterceptor(),
                new AuthorizationChannelInterceptor(inboundAuthorization()));
    }

    /**
     * The rules every client frame is held to.
     *
     * @return the authorization manager of the inbound channel
     */
    static AuthorizationManager<Message<?>> inboundAuthorization() {
        return MessageMatcherDelegatingAuthorizationManager.builder()
                                                           .nullDestMatcher()
                                                           .authenticated()
                                                           .simpSubscribeDestMatchers("/user/queue/**")
                                                           .authenticated()
                                                           .simpMessageDestMatchers("/ws/**")
                                                           .authenticated()
                                                           .simpTypeMatchers(SimpMessageType.MESSAGE, SimpMessageType.SUBSCRIBE)
                                                           .denyAll()
                                                           .anyMessage()
                                                           .denyAll()
                                                           .build();
    }

}
