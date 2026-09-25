/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.framework.keycloak;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * A real Keycloak in a container, so a browser test can sign in as a user whose token carries
 * tenant groups {@code <tenantId>.<appId>.<role>} - the only way to test the {@code TOKEN_GROUPS}
 * user interface end to end. One container serves every test class of a JVM.
 *
 * <p>
 * The realm is built through the admin REST API rather than imported: an import that declares
 * {@code clientScopes} replaces Keycloak's built-in set and breaks every login, while a realm
 * created empty keeps the defaults (including {@code offline_access}, which the platform requests).
 * A Group Membership mapper writes the plain group names into a claim of their own; the redirect
 * URI is set to the application's actual port once it is known, because Keycloak refuses a wildcard
 * port.
 */
public final class KeycloakTestContainer {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(KeycloakTestContainer.class);

    /** The image. */
    private static final DockerImageName IMAGE = DockerImageName.parse("quay.io/keycloak/keycloak:26.0");

    /** The realm the tests use. */
    public static final String REALM = "dirigible-it";

    /** The client the application signs in with. */
    public static final String CLIENT_ID = "dirigible-it";

    /** Its secret. */
    public static final String CLIENT_SECRET = "dirigible-it-secret";

    /** The claim the group names are written to. */
    public static final String GROUPS_CLAIM = "dirigible_groups";

    /** The shared instance. */
    private static KeycloakTestContainer shared;

    /** The container. */
    private final GenericContainer<?> container;

    /** The HTTP client. */
    private final HttpClient http = HttpClient.newBuilder()
                                              .connectTimeout(Duration.ofSeconds(10))
                                              .build();

    /** The id of the client, once created. */
    private String clientUuid;

    /**
     * Instantiates a container.
     */
    @SuppressWarnings("resource")
    private KeycloakTestContainer() {
        container = new GenericContainer<>(IMAGE).withExposedPorts(8080)
                                                 .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", "admin")
                                                 .withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", "admin")
                                                 .withCommand("start-dev")
                                                 .waitingFor(Wait.forHttp("/realms/master")
                                                                 .forPort(8080)
                                                                 .withStartupTimeout(Duration.ofMinutes(4)));
    }

    /**
     * The shared container, started with its realm on first use.
     *
     * @return the container
     */
    public static synchronized KeycloakTestContainer shared() {
        if (shared == null) {
            KeycloakTestContainer started = new KeycloakTestContainer();
            started.container.start();
            started.createRealm();
            shared = started;
            LOGGER.info("Keycloak test container started at [{}]", started.baseUrl());
        }
        return shared;
    }

    /**
     * The server's base URL as the host sees it.
     *
     * @return the base URL
     */
    public String baseUrl() {
        return "http://localhost:" + container.getMappedPort(8080);
    }

    /**
     * The issuer of the test realm - one string for the browser and the application, both on the host.
     *
     * @return the issuer
     */
    public String issuer() {
        return baseUrl() + "/realms/" + REALM;
    }

    /**
     * Allows the application at the given base URL to receive the login callback.
     *
     * @param applicationBaseUrl e.g. {@code http://localhost:43210}
     */
    public synchronized void allowRedirectsTo(String applicationBaseUrl) {
        String body = "{\"clientId\":\"" + CLIENT_ID + "\",\"redirectUris\":[\"" + applicationBaseUrl + "/*\"],\"webOrigins\":[\"+\"]}";
        send("PUT", "/admin/realms/" + REALM + "/clients/" + clientUuid, body, 204);
    }

    /**
     * Creates a group unless it exists.
     *
     * @param name the group name, e.g. {@code acme.library.Owner}
     */
    public synchronized void ensureGroup(String name) {
        int status = call("POST", "/admin/realms/" + REALM + "/groups", "{\"name\":\"" + name + "\"}").statusCode();
        if (status != 201 && status != 409) {
            throw new IllegalStateException("Keycloak refused to create the group [" + name + "]: " + status);
        }
    }

    /**
     * Creates a user with a password and the given groups; an existing user is left as it is.
     *
     * @param email the email, also the username
     * @param password the password
     * @param groups the group names
     */
    public synchronized void ensureUser(String email, String password, String... groups) {
        for (String group : groups) {
            ensureGroup(group);
        }
        StringBuilder paths = new StringBuilder();
        for (String group : groups) {
            paths.append(paths.length() == 0 ? "" : ",")
                 .append("\"/")
                 .append(group)
                 .append("\"");
        }
        String local = email.substring(0, email.indexOf('@'));
        String body = "{\"username\":\"" + email + "\",\"email\":\"" + email + "\",\"emailVerified\":true,\"enabled\":true,"
                + "\"firstName\":\"" + local + "\",\"lastName\":\"Test\",\"credentials\":[{\"type\":\"password\",\"value\":\"" + password
                + "\",\"temporary\":false}],\"groups\":[" + paths + "]}";
        int status = call("POST", "/admin/realms/" + REALM + "/users", body).statusCode();
        if (status != 201 && status != 409) {
            throw new IllegalStateException("Keycloak refused to create the user [" + email + "]: " + status);
        }
    }

    /**
     * Creates the realm and its client.
     */
    private void createRealm() {
        send("POST", "/admin/realms", "{\"realm\":\"" + REALM + "\",\"enabled\":true,\"sslRequired\":\"none\"}", 201);
        String client = "{\"clientId\":\"" + CLIENT_ID + "\",\"secret\":\"" + CLIENT_SECRET + "\",\"publicClient\":false,"
                + "\"standardFlowEnabled\":true,\"directAccessGrantsEnabled\":true,\"redirectUris\":[\"http://localhost/*\"],"
                + "\"protocolMappers\":[{\"name\":\"tenant groups\",\"protocol\":\"openid-connect\",\"protocolMapper\":\"oidc-group-membership-mapper\","
                + "\"config\":{\"claim.name\":\"" + GROUPS_CLAIM + "\",\"full.path\":\"false\",\"id.token.claim\":\"true\","
                + "\"access.token.claim\":\"true\",\"userinfo.token.claim\":\"true\"}}]}";
        HttpResponse<String> created = call("POST", "/admin/realms/" + REALM + "/clients", client);
        if (created.statusCode() != 201) {
            throw new IllegalStateException("Keycloak refused to create the client: " + created.statusCode() + " " + created.body());
        }
        String location = created.headers()
                                 .firstValue("Location")
                                 .orElseThrow();
        clientUuid = location.substring(location.lastIndexOf('/') + 1);
    }

    /**
     * Sends an admin request that must answer the given status.
     *
     * @param method the method
     * @param path the path
     * @param body the JSON body
     * @param expected the expected status
     */
    private void send(String method, String path, String body, int expected) {
        HttpResponse<String> response = call(method, path, body);
        if (response.statusCode() != expected) {
            throw new IllegalStateException(method + " " + path + " answered " + response.statusCode() + ": " + response.body());
        }
    }

    /**
     * Sends an admin request.
     *
     * @param method the method
     * @param path the path
     * @param body the JSON body
     * @return the response
     */
    private HttpResponse<String> call(String method, String path, String body) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl() + path))
                                             .header("Authorization", "Bearer " + adminToken())
                                             .header("Content-Type", "application/json")
                                             .method(method, HttpRequest.BodyPublishers.ofString(body))
                                             .build();
            return http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new IllegalStateException("Could not call Keycloak: " + method + " " + path, e);
        } catch (InterruptedException e) {
            Thread.currentThread()
                  .interrupt();
            throw new IllegalStateException("Interrupted calling Keycloak", e);
        }
    }

    /**
     * An admin access token from the master realm.
     *
     * @return the token
     */
    private String adminToken() throws IOException, InterruptedException {
        String form =
                "grant_type=password&client_id=admin-cli&username=admin&password=" + URLEncoder.encode("admin", StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl() + "/realms/master/protocol/openid-connect/token"))
                                         .header("Content-Type", "application/x-www-form-urlencoded")
                                         .POST(HttpRequest.BodyPublishers.ofString(form))
                                         .build();
        String body = http.send(request, HttpResponse.BodyHandlers.ofString())
                          .body();
        Matcher token = Pattern.compile("\"access_token\"\\s*:\\s*\"([^\"]+)\"")
                               .matcher(body);
        if (!token.find()) {
            throw new IllegalStateException("Keycloak issued no admin token: " + body);
        }
        return token.group(1);
    }
}
