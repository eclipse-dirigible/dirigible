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
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.awaitility.Awaitility;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.api.websockets.WebsocketsFacade;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import com.google.gson.Gson;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/**
 * A frontend hosted outside the platform - a single-page application, a mobile application, a
 * webview shell - calls the platform from its own origin with the tokens its identity provider
 * issued, over HTTP and over the STOMP broker (#7415).
 *
 * <p>
 * The platform runs the {@code keycloak} profile against an in-process OpenID Connect provider:
 * JWKS, authorization, token and userinfo endpoints on a local port, minting RS256 tokens exactly
 * as a realm would. Nothing here reaches the network. One boot serves every case.
 */
@ActiveProfiles("keycloak")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Import(ExternalFrontendITConfig.class)
class ExternalFrontendIT extends IntegrationTest {

    private static final String ORIGIN = "https://app.example.com";
    private static final String UNLISTED_ORIGIN = "https://evil.example.com";
    private static final String CLIENT_ID = "external-frontend-it";
    private static final String CLIENT_SECRET = "external-frontend-it-secret";
    private static final String PROJECT = "external-frontend-it";
    private static final String WHOAMI = "/services/js/" + PROJECT + "/whoami.mjs";
    private static final String WORKSPACES = "/services/ide/workspaces";
    static final List<String> DEVELOPER_GROUPS = List.of("DEVELOPER", "it-reader");
    private static final List<String> READER_GROUPS = List.of("it-reader");
    private static final String BROWSER_ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
    private static final String AXIOS_ACCEPT = "application/json, text/plain, */*";

    /**
     * Prints who the platform thinks is calling. {@code it-reader} and {@code it-writer} are roles no
     * platform role implies, so the pair tells the granted roles apart from the DEVELOPER shortcut.
     */
    private static final String WHOAMI_SOURCE = """
            import { user } from "@aerokit/sdk/security";
            import { response } from "@aerokit/sdk/http";

            response.setContentType("application/json");
            response.println(JSON.stringify({
                name: user.getName(),
                reader: user.isInRole("it-reader"),
                writer: user.isInRole("it-writer")
            }));
            """;

    /** Read by {@link ExternalFrontendITConfig}'s stub provider, which signs its tokens here. */
    static MockIdentityProvider identityProvider;
    private static ThreadPoolTaskScheduler stompScheduler;

    @LocalServerPort
    private int port;

    @Autowired
    private IRepository repository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private SimpUserRegistry simpUserRegistry;

    @BeforeAll
    static void startIdentityProviderAndAllowTheOrigin() throws Exception {
        identityProvider = MockIdentityProvider.start();
        Configuration.set(DirigibleConfig.CORS_ALLOWED_ORIGINS.getKey(), ORIGIN);
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        stompScheduler = new ThreadPoolTaskScheduler();
        stompScheduler.setThreadNamePrefix("external-frontend-it-stomp-");
        stompScheduler.initialize();
    }

    @AfterAll
    static void stopIdentityProvider() {
        stompScheduler.shutdown();
        identityProvider.stop();
    }

    @DynamicPropertySource
    static void keycloakProfile(DynamicPropertyRegistry registry) {
        registry.add("DIRIGIBLE_KEYCLOAK_AUTH_SERVER_URL", () -> identityProvider.issuer());
        registry.add("DIRIGIBLE_KEYCLOAK_CLIENT_ID", () -> CLIENT_ID);
        registry.add("DIRIGIBLE_KEYCLOAK_CLIENT_SECRET", () -> CLIENT_SECRET);
        // the callback template - Spring expands it to the base URL of the request that starts the login
        registry.add("DIRIGIBLE_HOST", () -> "{baseUrl}");
    }

    @BeforeEach
    void writeWhoami() {
        repository.createResource(IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/whoami.mjs",
                WHOAMI_SOURCE.getBytes(StandardCharsets.UTF_8), false, "application/javascript", true);
    }

    // --- CORS ----------------------------------------------------------------------------------

    @Test
    void aPreflightFromTheListedOriginIsGranted() {
        api().header("Origin", ORIGIN)
             .header("Access-Control-Request-Method", "GET")
             .header("Access-Control-Request-Headers", "authorization")
             .when()
             .options(WHOAMI)
             .then()
             .statusCode(200)
             .header("Access-Control-Allow-Origin", ORIGIN)
             .header("Access-Control-Allow-Methods", containsString("GET"))
             .header("Access-Control-Allow-Headers", containsStringIgnoringCase("authorization"))
             .header("Access-Control-Max-Age", "3600")
             .header("Access-Control-Allow-Credentials", nullValue());
    }

    @Test
    void aPreflightFromAnUnlistedOriginIsRefused() {
        api().header("Origin", UNLISTED_ORIGIN)
             .header("Access-Control-Request-Method", "GET")
             .when()
             .options(WHOAMI)
             .then()
             .statusCode(403);
    }

    // --- the answer to an unauthenticated call -------------------------------------------------

    @Test
    void aProgrammaticUnauthenticatedRequestGetsAJsonUnauthorized() {
        api().header("Sec-Fetch-Mode", "cors")
             .when()
             .get(WORKSPACES)
             .then()
             .statusCode(401)
             .header("WWW-Authenticate", "Bearer")
             .contentType(ContentType.JSON)
             .body("status", equalTo(401))
             .body("error", equalTo("Unauthorized"))
             .body("message", equalTo("Authentication required"))
             .body("path", equalTo(WORKSPACES));

        // what axios sends - no fetch metadata reaches a server from a non-browser runtime
        api().accept(AXIOS_ACCEPT)
             .when()
             .get(WORKSPACES)
             .then()
             .statusCode(401)
             .header("WWW-Authenticate", "Bearer");
    }

    @Test
    void aBrowserNavigationIsSentToTheLogin() {
        api().redirects()
             .follow(false)
             .accept(BROWSER_ACCEPT)
             .header("Sec-Fetch-Mode", "navigate")
             .when()
             .get(WORKSPACES)
             .then()
             .statusCode(302)
             .header("Location", containsString("/oauth2/authorization/keycloak"));
    }

    // --- bearer identities ---------------------------------------------------------------------

    @Test
    void anIdTokenActsAsTheUser() {
        String token = identityProvider.idToken("jane", DEVELOPER_GROUPS);

        api().auth()
             .oauth2(token)
             .header("Origin", ORIGIN)
             .when()
             .get(WORKSPACES)
             .then()
             .statusCode(200)
             .header("Set-Cookie", nullValue())
             .header("Access-Control-Allow-Origin", ORIGIN)
             .header("Access-Control-Expose-Headers", "Content-Disposition");

        api().auth()
             .oauth2(token)
             .when()
             .get(WHOAMI)
             .then()
             .statusCode(200)
             .body("name", equalTo("jane"))
             .body("reader", equalTo(true));
    }

    @Test
    void anIdTokenIsHeldToTheRolesOfItsGroups() {
        String token = identityProvider.idToken("bob", READER_GROUPS);

        api().auth()
             .oauth2(token)
             .when()
             .get(WORKSPACES)
             .then()
             .statusCode(403);

        api().auth()
             .oauth2(token)
             .when()
             .get(WHOAMI)
             .then()
             .statusCode(200)
             .body("name", equalTo("bob"))
             .body("reader", equalTo(true))
             .body("writer", equalTo(false));
    }

    @Test
    void anAccessTokenActsAsTheClientAsBefore() {
        String token = identityProvider.accessToken("service-account-reports", "openid dirigible/it-reader");

        api().auth()
             .oauth2(token)
             .when()
             .get(WHOAMI)
             .then()
             .statusCode(200)
             .body("name", equalTo("service-account-reports"))
             .body("reader", equalTo(true))
             .body("writer", equalTo(false));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("refusedTokens")
    void aTokenThatFailsValidationIsRefused(String token) {
        api().auth()
             .oauth2(token)
             .when()
             .get(WHOAMI)
             .then()
             .statusCode(401)
             .header("WWW-Authenticate", containsString("invalid_token"))
             .contentType(ContentType.JSON)
             .body("status", equalTo(401))
             .body("message", not(equalTo("Authentication required")));
    }

    static Stream<Named<String>> refusedTokens() {
        return Stream.of(
                Named.of("issued for another client",
                        identityProvider.idToken("jane", DEVELOPER_GROUPS, claims -> claims.audience("another-client"))),
                Named.of("issued by another realm",
                        identityProvider.idToken("jane", DEVELOPER_GROUPS,
                                claims -> claims.issuer("https://elsewhere.example.org/realms/it"))),
                Named.of("expired", identityProvider.idToken("jane", DEVELOPER_GROUPS,
                        claims -> claims.expirationTime(Date.from(Instant.now()
                                                                         .minusSeconds(120))))),
                Named.of("without the user name",
                        identityProvider.idToken("jane", DEVELOPER_GROUPS, claims -> claims.claim("preferred_username", null))),
                Named.of("a refresh token", identityProvider.idToken("jane", DEVELOPER_GROUPS, claims -> claims.claim("typ", "Refresh"))),
                Named.of("not a token at all", "not-a-token"));
    }

    // --- the hosted login keeps working without a session per request -------------------------

    @Test
    void theHostedLoginStillEstablishesASession() throws Exception {
        Response start = api().redirects()
                              .follow(false)
                              .when()
                              .get("/oauth2/authorization/keycloak");
        start.then()
             .statusCode(302);
        String authorizeUrl = start.getHeader("Location");
        assertTrue(authorizeUrl.startsWith(identityProvider.issuer()), authorizeUrl);
        String preLoginSession = start.getCookie("JSESSIONID");
        assertNotNull(preLoginSession, "the authorization request is kept in the session");

        // the identity provider authenticates jane and sends the browser back with a code
        String callbackUrl = identityProvider.authorize(authorizeUrl);
        assertTrue(callbackUrl.contains("/login/oauth2/code/keycloak?"), callbackUrl);

        // the callback URL is already encoded by the provider - encoding it again would corrupt the state
        Response login = api().urlEncodingEnabled(false)
                              .redirects()
                              .follow(false)
                              .cookie("JSESSIONID", preLoginSession)
                              .when()
                              .get(callbackUrl);
        login.then()
             .statusCode(302);
        String landing = login.getHeader("Location");
        assertTrue(identityProvider.tokenRequests() > 0, "the platform must exchange the code at the token endpoint");
        assertTrue(identityProvider.userInfoRequests() > 0, "the platform must load the user from the userinfo endpoint");
        assertFalse(landing.contains("error"), "the login must succeed, but the browser was sent to " + landing);
        String session = login.getCookie("JSESSIONID") != null ? login.getCookie("JSESSIONID") : preLoginSession;

        api().cookie("JSESSIONID", session)
             .header("Sec-Fetch-Mode", "cors")
             .when()
             .get(WORKSPACES)
             .then()
             .statusCode(200);
        api().cookie("JSESSIONID", session)
             .when()
             .get(WHOAMI)
             .then()
             .statusCode(200)
             .body("name", equalTo("jane"))
             .body("reader", equalTo(true));
    }

    // --- STOMP ---------------------------------------------------------------------------------

    @Test
    void aBearerStompSessionReceivesTheMessagesOfItsUserOnly() throws Exception {
        RecordingSessionHandler janeHandler = new RecordingSessionHandler();
        RecordingSessionHandler bobHandler = new RecordingSessionHandler();
        StompSession jane = connect(identityProvider.idToken("jane", DEVELOPER_GROUPS), ORIGIN, janeHandler);
        StompSession bob = connect(identityProvider.idToken("bob", READER_GROUPS), ORIGIN, bobHandler);
        try {
            BlockingQueue<String> janeInbox = subscribe(jane, "/user/queue/reply/it");
            BlockingQueue<String> bobInbox = subscribe(bob, "/user/queue/reply/it");
            awaitSubscription("jane", "/user/queue/reply/it");
            awaitSubscription("bob", "/user/queue/reply/it");

            // the broker registers a subscription asynchronously and confirms nothing - send until it lands
            Awaitility.await()
                      .atMost(15, TimeUnit.SECONDS)
                      .pollInterval(500, TimeUnit.MILLISECONDS)
                      .untilAsserted(() -> {
                          messagingTemplate.convertAndSendToUser("jane", "/queue/reply/it", "hello jane");
                          assertEquals("hello jane", janeInbox.poll(500, TimeUnit.MILLISECONDS), "the bearer identity is the STOMP user");
                      });

            assertNull(bobInbox.poll(2, TimeUnit.SECONDS), "another user's queue must stay silent");
            assertTrue(janeHandler.errors.isEmpty() && bobHandler.errors.isEmpty());
        } finally {
            jane.disconnect();
            bob.disconnect();
        }
    }

    @Test
    void aCookieSessionConnectsWithoutABearerHeader() throws Exception {
        // the pages the platform serves - every shipped websocket page - authenticate their STOMP session
        // by the handshake cookie alone, and the CONNECT gate must keep that open
        String session = api().auth()
                              .oauth2(identityProvider.idToken("jane", DEVELOPER_GROUPS))
                              .when()
                              .post("/login/token")
                              .getCookie("JSESSIONID");
        assertNotNull(session, "the exchange answers with the session cookie");
        WebSocketHttpHeaders handshakeHeaders = new WebSocketHttpHeaders();
        handshakeHeaders.add(HttpHeaders.COOKIE, "JSESSIONID=" + session);
        RecordingSessionHandler handler = new RecordingSessionHandler();

        StompSession stomp = connect(handshakeHeaders, new StompHeaders(), handler);
        try {
            BlockingQueue<String> inbox = subscribe(stomp, "/user/queue/reply/it");
            awaitSubscription("jane", "/user/queue/reply/it");
            Awaitility.await()
                      .atMost(15, TimeUnit.SECONDS)
                      .pollInterval(500, TimeUnit.MILLISECONDS)
                      .untilAsserted(() -> {
                          messagingTemplate.convertAndSendToUser("jane", "/queue/reply/it", "hello cookie");
                          assertEquals("hello cookie", inbox.poll(500, TimeUnit.MILLISECONDS), "the handshake cookie is the STOMP user");
                      });
            assertTrue(handler.errors.isEmpty());
        } finally {
            stomp.disconnect();
        }
    }

    @Test
    void aBearerStompSessionEndsWhenItsTokenDoes() throws Exception {
        RecordingSessionHandler handler = new RecordingSessionHandler();
        String shortLived = identityProvider.idToken("jane", DEVELOPER_GROUPS, claims -> claims.expirationTime(Date.from(Instant.now()
                                                                                                                                .plusSeconds(
                                                                                                                                        10))));
        StompSession session = connect(shortLived, ORIGIN, handler);
        subscribe(session, "/user/queue/reply/it");
        awaitSubscription("jane", "/user/queue/reply/it");

        // the client sends nothing more - the expiry of its token alone ends the session, with the
        // answer every refused frame gets
        assertEquals("Unauthorized", handler.errors.poll(30, TimeUnit.SECONDS));
        Awaitility.await()
                  .atMost(10, TimeUnit.SECONDS)
                  .until(() -> !session.isConnected());
    }

    @Test
    void anAnonymousStompConnectIsRefused() throws Exception {
        RecordingSessionHandler handler = new RecordingSessionHandler();

        assertThrows(ExecutionException.class, () -> connect(null, ORIGIN, handler));
        assertEquals("Unauthorized", handler.errors.poll(10, TimeUnit.SECONDS));
    }

    @Test
    void aStompConnectWithAnInvalidTokenIsRefused() throws Exception {
        RecordingSessionHandler handler = new RecordingSessionHandler();
        String foreign = identityProvider.idToken("jane", DEVELOPER_GROUPS, claims -> claims.audience("another-client"));

        assertThrows(ExecutionException.class, () -> connect(foreign, ORIGIN, handler));
        // the frame says no more than that - the reason stays in the log
        assertEquals("Unauthorized", handler.errors.poll(10, TimeUnit.SECONDS));
    }

    @Test
    void aStompSubscriptionToATopicIsRefused() throws Exception {
        RecordingSessionHandler handler = new RecordingSessionHandler();
        StompSession session = connect(identityProvider.idToken("jane", DEVELOPER_GROUPS), ORIGIN, handler);

        session.subscribe("/topic/announcements", new InboxFrameHandler(new LinkedBlockingQueue<>()));

        assertEquals("Unauthorized", handler.errors.poll(10, TimeUnit.SECONDS));
        Awaitility.await()
                  .atMost(10, TimeUnit.SECONDS)
                  .until(() -> !session.isConnected());
    }

    @Test
    void aStompHandshakeFromAnUnlistedOriginIsRefused() {
        assertThrows(ExecutionException.class,
                () -> connect(identityProvider.idToken("jane", DEVELOPER_GROUPS), UNLISTED_ORIGIN, new RecordingSessionHandler()));
    }

    @Test
    void aSockJsTransportRequestFromTheListedOriginIsAnsweredWithCredentials() {
        // SockJS's XHR transports send credentials and need the answer to allow them - the STOMP endpoint
        // answers its own CORS, whatever the platform's configuration says about credentials
        api().header("Origin", ORIGIN)
             .when()
             .get("/stomp/info")
             .then()
             .statusCode(200)
             .header("Access-Control-Allow-Origin", ORIGIN)
             .header("Access-Control-Allow-Credentials", "true");
    }

    @Test
    void aSockJsTransportRequestFromAnUnlistedOriginIsRefused() {
        api().header("Origin", UNLISTED_ORIGIN)
             .when()
             .get("/stomp/info")
             .then()
             .statusCode(403);
    }

    @Test
    void thePlatformsOwnClientConnectsWithABearerToken() throws Exception {
        // the websockets API of a script reaches a Dirigible broker with the CONNECT headers it is given
        StompSession session = WebsocketsFacade.createWebsocket(stompUrl(), PROJECT + "/ws-handler",
                Map.of("Authorization", "Bearer " + identityProvider.idToken("jane", DEVELOPER_GROUPS)));
        try {
            assertTrue(session.isConnected());
            // the client subscribes to its user queue on connect - the broker knows it as jane
            awaitSubscription("jane", "/user/queue/reply");
        } finally {
            session.disconnect();
        }
    }

    @Test
    void thePlatformsOwnClientIsRefusedWithoutOne() {
        assertThrows(ExecutionException.class, () -> WebsocketsFacade.createWebsocket(stompUrl(), PROJECT + "/ws-handler"));
    }

    // --- POST /login/token ---------------------------------------------------------------------

    @Test
    void anIdTokenIsExchangedForASessionThatEndsWithTheToken() {
        String token = identityProvider.idToken("jane", DEVELOPER_GROUPS, claims -> claims.expirationTime(Date.from(Instant.now()
                                                                                                                           .plusSeconds(
                                                                                                                                   8))));

        Response exchanged = api().auth()
                                  .oauth2(token)
                                  .when()
                                  .post("/login/token");
        exchanged.then()
                 .statusCode(200)
                 .body("outcome", equalTo("AUTHENTICATED"))
                 .body("expiresAt", notNullValue());
        String session = exchanged.getCookie("JSESSIONID");
        assertNotNull(session, "the exchange answers with the session cookie");

        api().cookie("JSESSIONID", session)
             .header("Sec-Fetch-Mode", "cors")
             .when()
             .get(WORKSPACES)
             .then()
             .statusCode(200);
        api().cookie("JSESSIONID", session)
             .when()
             .get(WHOAMI)
             .then()
             .statusCode(200)
             .body("name", equalTo("jane"))
             .body("reader", equalTo(true));

        Awaitility.await()
                  .atMost(30, TimeUnit.SECONDS)
                  .pollInterval(1, TimeUnit.SECONDS)
                  .untilAsserted(() -> api().cookie("JSESSIONID", session)
                                            .header("Sec-Fetch-Mode", "cors")
                                            .when()
                                            .get(WORKSPACES)
                                            .then()
                                            .statusCode(401));
    }

    @Test
    void anAccessTokenIsNotExchangedForASession() {
        api().auth()
             .oauth2(identityProvider.accessToken("service-account-reports", "openid dirigible/it-reader"))
             .when()
             .post("/login/token")
             .then()
             .statusCode(403)
             .body("outcome", equalTo("ID_TOKEN_REQUIRED"))
             .header("Set-Cookie", nullValue());
    }

    @Test
    void anExchangeWithoutATokenIsUnauthorized() {
        api().when()
             .post("/login/token")
             .then()
             .statusCode(401)
             .header("WWW-Authenticate", "Bearer")
             .body("outcome", equalTo("UNAUTHENTICATED"))
             .header("Set-Cookie", nullValue());
    }

    // --- POST /login/native creates the session it needs --------------------------------------

    @Test
    void theNativeLoginMintsASessionOnDemand() {
        // the chain creates a session only when one is needed, so the login request arrives without
        // one - the login itself must create the session its cookie names
        Response login = api().contentType(ContentType.JSON)
                              .body("{\"username\":\"jane\",\"password\":\"correct horse\"}")
                              .when()
                              .post("/login/native");
        login.then()
             .statusCode(200)
             .body("outcome", equalTo("AUTHENTICATED"));
        String session = login.getCookie("JSESSIONID");
        assertNotNull(session, "the login answers with the session cookie");

        // the revalidation filter finds the authorized client the login registered and lets the
        // session through; the roles are the ones of the user's groups
        api().cookie("JSESSIONID", session)
             .header("Sec-Fetch-Mode", "cors")
             .when()
             .get(WORKSPACES)
             .then()
             .statusCode(200);
        api().cookie("JSESSIONID", session)
             .when()
             .get(WHOAMI)
             .then()
             .statusCode(200)
             .body("name", equalTo("jane"))
             .body("reader", equalTo(true));
    }

    @Test
    void theNativeLoginReplacesThePreLoginSessionId() {
        String preLoginSession = api().redirects()
                                      .follow(false)
                                      .when()
                                      .get("/oauth2/authorization/keycloak")
                                      .getCookie("JSESSIONID");
        assertNotNull(preLoginSession, "the authorization request is kept in a session");

        Response login = api().cookie("JSESSIONID", preLoginSession)
                              .contentType(ContentType.JSON)
                              .body("{\"username\":\"jane\",\"password\":\"correct horse\"}")
                              .when()
                              .post("/login/native");
        login.then()
             .statusCode(200)
             .body("outcome", equalTo("AUTHENTICATED"));

        String session = login.getCookie("JSESSIONID");
        assertNotNull(session, "a pre-login session must not keep the id it was known under");
        assertNotEquals(preLoginSession, session);
    }

    // --- helpers -------------------------------------------------------------------------------

    private RequestSpecification api() {
        return given().port(port);
    }

    private String stompUrl() {
        return "ws://localhost:" + port + "/stomp";
    }

    private StompSession connect(String token, String origin, RecordingSessionHandler handler) throws Exception {
        WebSocketHttpHeaders handshakeHeaders = new WebSocketHttpHeaders();
        handshakeHeaders.setOrigin(origin);
        StompHeaders connectHeaders = new StompHeaders();
        if (token != null) {
            connectHeaders.add("Authorization", "Bearer " + token);
        }
        return connect(handshakeHeaders, connectHeaders, handler);
    }

    private StompSession connect(WebSocketHttpHeaders handshakeHeaders, StompHeaders connectHeaders, RecordingSessionHandler handler)
            throws Exception {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new org.springframework.messaging.converter.StringMessageConverter());
        client.setTaskScheduler(stompScheduler);
        return client.connectAsync(stompUrl(), handshakeHeaders, connectHeaders, handler)
                     .get(15, TimeUnit.SECONDS);
    }

    private static BlockingQueue<String> subscribe(StompSession session, String destination) {
        BlockingQueue<String> inbox = new LinkedBlockingQueue<>();
        session.subscribe(destination, new InboxFrameHandler(inbox));
        return inbox;
    }

    /**
     * Waits until the broker knows the user - by the name the bearer token yields - as subscribed to
     * the destination.
     */
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

    /**
     * Records what the broker sends to the session itself - which is ERROR frames only, every other
     * frame goes to a subscription.
     */
    private static final class RecordingSessionHandler extends StompSessionHandlerAdapter {

        final BlockingQueue<String> errors = new LinkedBlockingQueue<>();

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            errors.add(String.valueOf(headers.getFirst("message")));
        }

        @Override
        public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
            errors.add("exception: " + exception.getMessage());
        }
    }

    private record InboxFrameHandler(BlockingQueue<String> inbox) implements StompFrameHandler {

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return String.class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            inbox.add(String.valueOf(payload));
        }
    }

    /**
     * The identity provider: a realm at {@code http://localhost:port/realms/it} issuing RS256 tokens,
     * with the four endpoints the platform talks to.
     */
    static final class MockIdentityProvider {

        private static final String KEY_ID = "external-frontend-it";
        private static final String REALM_PATH = "/realms/it";
        private static final Gson GSON = new Gson();

        private final HttpServer server;
        private final RSAKey key;
        private final String issuer;
        private final Map<String, String> nonceByCode = new ConcurrentHashMap<>();
        private final AtomicInteger tokenRequests = new AtomicInteger();
        private final AtomicInteger userInfoRequests = new AtomicInteger();

        private MockIdentityProvider(HttpServer server, RSAKey key) {
            this.server = server;
            this.key = key;
            this.issuer = "http://localhost:" + server.getAddress()
                                                      .getPort()
                    + REALM_PATH;
        }

        static MockIdentityProvider start() throws Exception {
            RSAKey key = new RSAKeyGenerator(2048).keyID(KEY_ID)
                                                  .algorithm(JWSAlgorithm.RS256)
                                                  .generate();
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            MockIdentityProvider provider = new MockIdentityProvider(server, key);
            byte[] jwks = new JWKSet(key.toPublicJWK()).toString()
                                                       .getBytes(StandardCharsets.UTF_8);
            server.createContext(REALM_PATH + "/protocol/openid-connect/certs", exchange -> respond(exchange, 200, jwks));
            server.createContext(REALM_PATH + "/protocol/openid-connect/auth", provider::authorizationEndpoint);
            server.createContext(REALM_PATH + "/protocol/openid-connect/token", provider::tokenEndpoint);
            server.createContext(REALM_PATH + "/protocol/openid-connect/userinfo", provider::userInfoEndpoint);
            server.start();
            return provider;
        }

        void stop() {
            server.stop(0);
        }

        String issuer() {
            return issuer;
        }

        int tokenRequests() {
            return tokenRequests.get();
        }

        int userInfoRequests() {
            return userInfoRequests.get();
        }

        /**
         * Plays the browser hop through the authorization endpoint: the user signs in and the provider
         * sends them back to the platform with a code.
         *
         * @param authorizeUrl the URL the platform redirected the browser to
         * @return the callback URL the provider redirects to
         */
        String authorize(String authorizeUrl) throws IOException, InterruptedException {
            HttpResponse<Void> response = HttpClient.newBuilder()
                                                    .followRedirects(HttpClient.Redirect.NEVER)
                                                    .build()
                                                    .send(HttpRequest.newBuilder(URI.create(authorizeUrl))
                                                                     .GET()
                                                                     .build(),
                                                            HttpResponse.BodyHandlers.discarding());
            assertEquals(302, response.statusCode());
            return response.headers()
                           .firstValue("Location")
                           .orElseThrow();
        }

        String idToken(String username, List<String> groups) {
            return idToken(username, groups, claims -> {
            });
        }

        String idToken(String username, List<String> groups, Consumer<JWTClaimsSet.Builder> customizer) {
            Instant now = Instant.now();
            JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder().issuer(issuer)
                                                                    .subject("sub-" + username)
                                                                    .audience(CLIENT_ID)
                                                                    .claim("azp", CLIENT_ID)
                                                                    .claim("typ", "ID")
                                                                    .claim("preferred_username", username)
                                                                    .claim("email", username + "@example.org")
                                                                    .claim("email_verified", true)
                                                                    .claim("groups", groups)
                                                                    .issueTime(Date.from(now))
                                                                    .expirationTime(Date.from(now.plusSeconds(300)));
            customizer.accept(claims);
            return sign(claims.build());
        }

        String accessToken(String subject, String scope) {
            Instant now = Instant.now();
            return sign(new JWTClaimsSet.Builder().issuer(issuer)
                                                  .subject(subject)
                                                  .audience("account")
                                                  .claim("azp", CLIENT_ID)
                                                  .claim("typ", "Bearer")
                                                  .claim("scope", scope)
                                                  .issueTime(Date.from(now))
                                                  .expirationTime(Date.from(now.plusSeconds(300)))
                                                  .build());
        }

        private String sign(JWTClaimsSet claims) {
            try {
                SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KEY_ID)
                                                                                       .build(),
                        claims);
                jwt.sign(new RSASSASigner(key));
                return jwt.serialize();
            } catch (Exception ex) {
                throw new IllegalStateException("Cannot sign a token", ex);
            }
        }

        private void authorizationEndpoint(HttpExchange exchange) throws IOException {
            Map<String, String> query = parameters(exchange.getRequestURI()
                                                           .getRawQuery());
            String code = UUID.randomUUID()
                              .toString();
            nonceByCode.put(code, query.getOrDefault("nonce", ""));
            String location = query.get("redirect_uri") + "?code=" + code + "&state="
                    + URLEncoder.encode(query.getOrDefault("state", ""), StandardCharsets.UTF_8);
            exchange.getResponseHeaders()
                    .add("Location", location);
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        }

        private void tokenEndpoint(HttpExchange exchange) throws IOException {
            tokenRequests.incrementAndGet();
            String body = new String(exchange.getRequestBody()
                                             .readAllBytes(),
                    StandardCharsets.UTF_8);
            String code = parameters(body).get("code");
            String nonce = code != null ? nonceByCode.remove(code) : null;
            if (nonce == null) {
                respond(exchange, 400, GSON.toJson(Map.of("error", "invalid_grant"))
                                           .getBytes(StandardCharsets.UTF_8));
                return;
            }
            Map<String, Object> tokens = new HashMap<>();
            tokens.put("access_token", accessToken("sub-jane", "openid profile email"));
            tokens.put("token_type", "Bearer");
            tokens.put("expires_in", 300);
            tokens.put("refresh_token", "refresh-" + code);
            tokens.put("id_token", idToken("jane", DEVELOPER_GROUPS, claims -> claims.claim("nonce", nonce)));
            tokens.put("scope", "openid profile email");
            respond(exchange, 200, GSON.toJson(tokens)
                                       .getBytes(StandardCharsets.UTF_8));
        }

        private void userInfoEndpoint(HttpExchange exchange) throws IOException {
            userInfoRequests.incrementAndGet();
            respond(exchange, 200,
                    GSON.toJson(
                            Map.of("sub", "sub-jane", "preferred_username", "jane", "email", "jane@example.org", "email_verified", true))
                        .getBytes(StandardCharsets.UTF_8));
        }

        private static void respond(HttpExchange exchange, int status, byte[] body) throws IOException {
            exchange.getResponseHeaders()
                    .add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        }

        private static Map<String, String> parameters(String encoded) {
            Map<String, String> parameters = new HashMap<>();
            if (encoded == null || encoded.isEmpty()) {
                return parameters;
            }
            for (String pair : encoded.split("&")) {
                int separator = pair.indexOf('=');
                String name = URLDecoder.decode(separator < 0 ? pair : pair.substring(0, separator), StandardCharsets.UTF_8);
                String value = separator < 0 ? "" : URLDecoder.decode(pair.substring(separator + 1), StandardCharsets.UTF_8);
                parameters.put(name, value);
            }
            return parameters;
        }
    }
}
