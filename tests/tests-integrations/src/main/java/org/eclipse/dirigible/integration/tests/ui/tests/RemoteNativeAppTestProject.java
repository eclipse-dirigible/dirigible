/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.BaseTestProject;
import org.eclipse.dirigible.tests.base.ProjectUtil;
import org.eclipse.dirigible.tests.framework.ide.EdmView;
import org.eclipse.dirigible.tests.framework.ide.IDE;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

/**
 * Drives the remote-native-app proxy end-to-end without touching any external network. The fixture
 * lives under {@code src/main/resources/RemoteNativeAppIT/} and is published through the IDE; an
 * in-JVM {@link HttpServer} started in {@link #configure()} stands in for the upstream service and
 * the {@code __UPSTREAM_URL__} placeholder in every {@code .native-app} file is rewritten to the
 * server's loopback URL at copy time.
 *
 * <p>
 * {@link #verify()} exercises the four contract paths of {@code security.exposedPaths} plus the
 * delete lifecycle:
 * <ol>
 * <li>whitelisted hit forwards to the upstream (200);</li>
 * <li>whitelist miss returns 404;</li>
 * <li>{@code basePath = ""} acts as a catch-all when no named app matches;</li>
 * <li>HTTP Basic credentials declared on the artefact are injected into the outbound request;</li>
 * <li>removing the artefact file from the registry unregisters the proxy (404 on subsequent
 * calls).</li>
 * </ol>
 */
@Lazy
@Component
class RemoteNativeAppTestProject extends BaseTestProject {

    private static final String UPSTREAM_URL_PLACEHOLDER = "__UPSTREAM_URL__";

    /** Expected outbound auth header for the basic-auth fixture: base64("alice:wonderland"). */
    private static final String EXPECTED_BASIC_AUTH_HEADER = "Basic YWxpY2U6d29uZGVybGFuZA==";

    private final ProjectUtil projectUtil;
    private final RestAssuredExecutor restAssuredExecutor;
    private final IRepository repository;
    private final SynchronizationProcessor synchronizationProcessor;

    private HttpServer upstream;
    private int upstreamPort;
    private volatile String lastAuthHeader;

    RemoteNativeAppTestProject(IDE ide, ProjectUtil projectUtil, EdmView edmView, RestAssuredExecutor restAssuredExecutor,
            IRepository repository, SynchronizationProcessor synchronizationProcessor) {
        super("RemoteNativeAppIT", ide, projectUtil, edmView);
        this.projectUtil = projectUtil;
        this.restAssuredExecutor = restAssuredExecutor;
        this.repository = repository;
        this.synchronizationProcessor = synchronizationProcessor;
    }

    @Override
    public void configure() {
        startUpstream();
        Map<String, String> placeholders = Map.of(UPSTREAM_URL_PLACEHOLDER, "http://127.0.0.1:" + upstreamPort);
        projectUtil.copyResourceProjectToDefaultUserWorkspace(getProjectResourcesFolder(), placeholders);
        publish();
        getIde().close();
    }

    @Override
    public void verify() {
        restAssuredExecutor.execute(this::runAssertions);
    }

    @Override
    public void cleanup() {
        if (upstream != null) {
            upstream.stop(0);
            upstream = null;
        }
    }

    private void runAssertions() {
        whitelistedPathForwardsToUpstream();
        nonWhitelistedPathReturns404();
        emptyBasePathActsAsCatchAll();
        basicAuthHeaderIsInjected();
        deleteUnregistersNativeApp();
    }

    private void whitelistedPathForwardsToUpstream() {
        given().when()
               .get("/services/native-apps-proxy/v1/http-bin-app/get")
               .then()
               .statusCode(200)
               .body(containsString("\"path\":\"/get\""));
    }

    private void nonWhitelistedPathReturns404() {
        given().when()
               .get("/services/native-apps-proxy/v1/http-bin-app/anything")
               .then()
               .statusCode(404);
    }

    private void emptyBasePathActsAsCatchAll() {
        // The first segment 'anything' does not match any named app, so the lookup filter falls
        // back to the empty-base catch-all (http-bin-app-root) and the entire path is forwarded.
        given().when()
               .get("/services/native-apps-proxy/v1/anything")
               .then()
               .statusCode(200)
               .body(containsString("\"path\":\"/anything\""));
    }

    private void basicAuthHeaderIsInjected() {
        lastAuthHeader = null;
        given().when()
               .get("/services/native-apps-proxy/v1/http-bin-app-auth/get")
               .then()
               .statusCode(200);
        String observed = lastAuthHeader;
        if (!EXPECTED_BASIC_AUTH_HEADER.equals(observed)) {
            throw new AssertionError(
                    "Expected outbound Authorization=[" + EXPECTED_BASIC_AUTH_HEADER + "] but upstream saw [" + observed + "]");
        }
    }

    private void deleteUnregistersNativeApp() {
        // Pre-condition — the delete fixture is still in the registry and reachable.
        given().when()
               .get("/services/native-apps-proxy/v1/http-bin-app-delete/get")
               .then()
               .statusCode(200);

        String registryPath = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/RemoteNativeAppIT/http-bin-app-delete.native-app";
        repository.removeResource(registryPath);
        synchronizationProcessor.forceProcessSynchronizers();

        // After DELETE the named lookup misses. The empty-base catch-all (http-bin-app-root) then
        // catches the request, so the upstream sees the *entire* original path including the now-
        // removed app's segment. Asserting on that path proves the named registration is gone —
        // had it survived, the lookup filter would have stripped 'http-bin-app-delete/' and the
        // upstream would have seen just '/get'.
        given().when()
               .get("/services/native-apps-proxy/v1/http-bin-app-delete/get")
               .then()
               .statusCode(200)
               .body(containsString("\"path\":\"/http-bin-app-delete/get\""));
    }

    private void startUpstream() {
        try {
            upstreamPort = allocateFreePort();
            upstream = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), upstreamPort), 0);
            upstream.createContext("/", this::handle);
            upstream.start();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to start upstream HTTP server for RemoteNativeAppIT", ex);
        }
    }

    private void handle(HttpExchange exchange) throws IOException {
        lastAuthHeader = exchange.getRequestHeaders()
                                 .getFirst("Authorization");
        String path = exchange.getRequestURI()
                              .getPath();
        String method = exchange.getRequestMethod();
        String body = "{\"method\":\"" + method + "\",\"path\":\"" + path + "\"}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders()
                .set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody()
                .write(bytes);
        exchange.close();
    }

    private static int allocateFreePort() throws IOException {
        try (ServerSocket s = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) {
            return s.getLocalPort();
        }
    }
}
