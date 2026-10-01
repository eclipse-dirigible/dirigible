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
    /**
     * The id of a user, by email.
     *
     * @param email the email
     * @return the id
     */
    public synchronized String userIdByEmail(String email) {
        HttpResponse<String> found =
                call("GET", "/admin/realms/" + REALM + "/users?exact=true&email=" + URLEncoder.encode(email, StandardCharsets.UTF_8), "");
        return idIn(found, "user [" + email + "]");
    }

    /**
     * Adds an existing user to a group, creating the group first when it does not exist. Adding a
     * member again is no change.
     *
     * @param email the user's email
     * @param group the group name
     */
    public synchronized void addUserToGroup(String email, String group) {
        ensureGroup(group);
        send("PUT", "/admin/realms/" + REALM + "/users/" + userIdByEmail(email) + "/groups/" + groupId(group), "", 204);
    }

    /**
     * Removes a user from a group. A group that does not exist, or a user who is not in it, is no
     * change.
     *
     * @param email the user's email
     * @param group the group name
     */
    public synchronized void removeUserFromGroup(String email, String group) {
        HttpResponse<String> found = findGroup(group);
        if (found.statusCode() == 200 && found.body()
                                              .trim()
                                              .equals("[]")) {
            return;
        }
        send("DELETE", "/admin/realms/" + REALM + "/users/" + userIdByEmail(email) + "/groups/" + idIn(found, "group [" + group + "]"), "",
                204);
    }

    /**
     * The realm's access token lifespan.
     *
     * @return the lifespan in seconds
     */
    public synchronized int accessTokenLifespan() {
        HttpResponse<String> realm = call("GET", "/admin/realms/" + REALM, "");
        Matcher lifespan = Pattern.compile("\"accessTokenLifespan\"\\s*:\\s*(\\d+)")
                                  .matcher(realm.body());
        if (!lifespan.find()) {
            throw new IllegalStateException("The realm carries no access token lifespan: " + realm.body());
        }
        return Integer.parseInt(lifespan.group(1));
    }

    /**
     * Sets the realm's access token lifespan. The realm is shared by every test of the JVM, so a caller
     * restores the value it read with {@link #accessTokenLifespan()}.
     *
     * @param seconds the lifespan in seconds
     */
    public synchronized void setAccessTokenLifespan(int seconds) {
        send("PUT", "/admin/realms/" + REALM, "{\"accessTokenLifespan\":" + seconds + "}", 204);
    }

    private String groupId(String group) {
        return idIn(findGroup(group), "group [" + group + "]");
    }

    /** The top-level groups named exactly so - the tenant groups are all top-level. */
    private HttpResponse<String> findGroup(String group) {
        return call("GET", "/admin/realms/" + REALM + "/groups?exact=true&search=" + URLEncoder.encode(group, StandardCharsets.UTF_8), "");
    }

    private static String idIn(HttpResponse<String> response, String what) {
        Matcher id = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"")
                            .matcher(response.body());
        if (response.statusCode() != 200 || !id.find()) {
            throw new IllegalStateException("Keycloak has no " + what + ": " + response.statusCode() + " " + response.body());
        }
        return id.group(1);
    }

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
