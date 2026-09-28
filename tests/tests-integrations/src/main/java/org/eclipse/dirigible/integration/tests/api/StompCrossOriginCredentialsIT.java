/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

/**
 * With {@code DIRIGIBLE_CORS_ALLOW_CREDENTIALS} on, a STOMP session opened cross-origin from a
 * configured origin is authenticated by its handshake - the HTTP authentication here, the session
 * cookie of a browser - exactly as a same-origin one is; without any identity it is still refused
 * (#7445). The companion case, credentials off, is in {@code ExternalFrontendIT}.
 */
// one boot for the class: the configuration is static and every case reads only
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class StompCrossOriginCredentialsIT extends IntegrationTest {

    private static final String ORIGIN = "https://app.example.com";
    private static final String DESTINATION = "/user/queue/reply/credentials-it";
    private static final String SOCKJS_CONNECT = "[\"CONNECT\\naccept-version:1.2\\nheart-beat:0,0\\n\\n\\u0000\"]";
    private static final String SOCKJS_DISCONNECT = "[\"DISCONNECT\\n\\n\\u0000\"]";

    private static final String USERNAME = DirigibleConfig.BASIC_ADMIN_USERNAME.getFromBase64Value();
    private static final String PASSWORD = DirigibleConfig.BASIC_ADMIN_PASS.getFromBase64Value();

    private static ThreadPoolTaskScheduler stompScheduler;

    @LocalServerPort
    private int port;

    @Autowired
    private SimpUserRegistry simpUserRegistry;

    @BeforeAll
    static void allowTheOriginWithCredentials() {
        // cleared by IntegrationTest.reloadConfigurations() once the class is done
        Configuration.set(DirigibleConfig.CORS_ALLOWED_ORIGINS.getKey(), ORIGIN);
        Configuration.set(DirigibleConfig.CORS_ALLOW_CREDENTIALS.getKey(), Boolean.TRUE.toString());
        stompScheduler = new ThreadPoolTaskScheduler();
        stompScheduler.setThreadNamePrefix("stomp-credentials-it-");
        stompScheduler.initialize();
    }

    @AfterAll
    static void stopScheduler() {
        stompScheduler.shutdown();
    }

    @Test
    void aCrossOriginHandshakeIdentityConnectsWhenCredentialsAreAllowed() throws Exception {
        WebSocketHttpHeaders handshakeHeaders = new WebSocketHttpHeaders();
        handshakeHeaders.setOrigin(ORIGIN);
        handshakeHeaders.setBasicAuth(USERNAME, PASSWORD);
        RecordingSessionHandler handler = new RecordingSessionHandler();

        StompSession session = connect(handshakeHeaders, handler);
        try {
            session.subscribe(DESTINATION, new StompSessionHandlerAdapter() {});
            awaitSubscription(USERNAME, DESTINATION);
            assertTrue(handler.errors.isEmpty());
        } finally {
            session.disconnect();
        }
    }

    @Test
    void aSameOriginHandshakeIdentityStillConnects() throws Exception {
        WebSocketHttpHeaders handshakeHeaders = new WebSocketHttpHeaders();
        handshakeHeaders.setBasicAuth(USERNAME, PASSWORD);
        RecordingSessionHandler handler = new RecordingSessionHandler();

        StompSession session = connect(handshakeHeaders, handler);
        try {
            session.subscribe(DESTINATION, new StompSessionHandlerAdapter() {});
            awaitSubscription(USERNAME, DESTINATION);
            assertTrue(handler.errors.isEmpty());
        } finally {
            session.disconnect();
        }
    }

    @Test
    void aCrossOriginConnectWithoutAnyIdentityIsStillRefused() throws Exception {
        WebSocketHttpHeaders handshakeHeaders = new WebSocketHttpHeaders();
        handshakeHeaders.setOrigin(ORIGIN);
        RecordingSessionHandler handler = new RecordingSessionHandler();

        assertThrows(ExecutionException.class, () -> connect(handshakeHeaders, handler));
        assertEquals("Unauthorized", handler.errors.poll(10, TimeUnit.SECONDS));
    }

    @Test
    void aCrossOriginSockJsPollingSessionConnectsByItsHandshakeIdentity() {
        String transport = "/stomp/000/" + UUID.randomUUID() + "/";

        crossOrigin().when()
                     .post(transport + "xhr")
                     .then()
                     .statusCode(200)
                     .body(equalTo("o\n"));
        crossOrigin().contentType(ContentType.TEXT)
                     .body(SOCKJS_CONNECT)
                     .when()
                     .post(transport + "xhr_send")
                     .then()
                     .statusCode(204);
        crossOrigin().when()
                     .post(transport + "xhr")
                     .then()
                     .statusCode(200)
                     .body(containsString("CONNECTED"))
                     .body(containsString("user-name:" + USERNAME));
        crossOrigin().contentType(ContentType.TEXT)
                     .body(SOCKJS_DISCONNECT)
                     .when()
                     .post(transport + "xhr_send")
                     .then()
                     .statusCode(204);
    }

    private RequestSpecification crossOrigin() {
        return given().port(port)
                      .header("Origin", ORIGIN)
                      .auth()
                      .preemptive()
                      .basic(USERNAME, PASSWORD);
    }

    private StompSession connect(WebSocketHttpHeaders handshakeHeaders, RecordingSessionHandler handler) throws Exception {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new StringMessageConverter());
        client.setTaskScheduler(stompScheduler);
        return client.connectAsync("ws://localhost:" + port + "/stomp", handshakeHeaders, new StompHeaders(), handler)
                     .get(15, TimeUnit.SECONDS);
    }

    private void awaitSubscription(String user, String destination) {
        Awaitility.await()
                  .atMost(10, TimeUnit.SECONDS)
                  .until(() -> {
                      SimpUser simpUser = simpUserRegistry.getUser(user);
                      return simpUser != null && simpUser.getSessions()
                                                         .stream()
                                                         .flatMap(session -> session.getSubscriptions()
                                                                                    .stream())
                                                         .anyMatch(subscription -> destination.equals(subscription.getDestination()));
                  });
    }

    /** Records what the broker sends to the session itself - ERROR frames only. */
    private static final class RecordingSessionHandler extends StompSessionHandlerAdapter {

        final BlockingQueue<String> errors = new LinkedBlockingQueue<>();

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            errors.add(String.valueOf(headers.getFirst("message")));
        }
    }
}
