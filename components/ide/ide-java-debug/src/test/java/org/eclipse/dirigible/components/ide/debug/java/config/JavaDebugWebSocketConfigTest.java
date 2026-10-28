/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.debug.java.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.HashMap;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.ide.debug.java.JavaDebugManager;
import org.eclipse.dirigible.components.ide.debug.java.handler.JavaDebugWebSocketHandler;
import org.eclipse.dirigible.components.ide.lsp.java.process.JdtLsManager;
import org.eclipse.dirigible.engine.java.runtime.ClassPathIndex;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.socket.server.support.OriginHandshakeInterceptor;
import org.springframework.web.socket.server.support.WebSocketHttpRequestHandler;

/**
 * The Java debugger websocket takes the same handshake defaults as the other IDE websockets.
 */
class JavaDebugWebSocketConfigTest {

    private static final String PATH = BaseEndpoint.PREFIX_ENDPOINT_WEBSOCKETS + "ide/java-debug";

    private final WebApplicationContextRunner contextRunner =
            new WebApplicationContextRunner().withBean(ClassPathIndex.class, () -> mock(ClassPathIndex.class))
                                             .withUserConfiguration(JdtLsManager.class, JavaDebugManager.class,
                                                     JavaDebugWebSocketHandler.class, JavaDebugWebSocketConfig.class);

    @Test
    void acceptsAHandshakeFromItsOwnOrigin() {
        contextRunner.run(context -> {
            MockHttpServletRequest request = handshakeRequest("http://localhost");
            WebSocketHttpRequestHandler handler = handler(context, request);
            OriginHandshakeInterceptor origins = handler.getHandshakeInterceptors()
                                                        .stream()
                                                        .filter(OriginHandshakeInterceptor.class::isInstance)
                                                        .map(OriginHandshakeInterceptor.class::cast)
                                                        .findFirst()
                                                        .orElseThrow();

            assertThat(origins.beforeHandshake(new ServletServerHttpRequest(request),
                    new ServletServerHttpResponse(new MockHttpServletResponse()), handler.getWebSocketHandler(), new HashMap<>())).isTrue();
        });
    }

    @Test
    void refusesAHandshakeFromAnotherOrigin() {
        contextRunner.run(context -> {
            MockHttpServletRequest request = handshakeRequest("https://other.example");
            MockHttpServletResponse response = new MockHttpServletResponse();

            handler(context, request).handleRequest(request, response);

            assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        });
    }

    private static MockHttpServletRequest handshakeRequest(String origin) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", PATH);
        request.addHeader("Origin", origin);
        request.addHeader("Upgrade", "websocket");
        request.addHeader("Connection", "Upgrade");
        request.addHeader("Sec-WebSocket-Version", "13");
        request.addHeader("Sec-WebSocket-Key", "dGhlIHNhbXBsZSBub25jZQ==");
        return request;
    }

    private static WebSocketHttpRequestHandler handler(AssertableWebApplicationContext context, MockHttpServletRequest request)
            throws Exception {
        return (WebSocketHttpRequestHandler) context.getBean(HandlerMapping.class)
                                                    .getHandler(request)
                                                    .getHandler();
    }
}
