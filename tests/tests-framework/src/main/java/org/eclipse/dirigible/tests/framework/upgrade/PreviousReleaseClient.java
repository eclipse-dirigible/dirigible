/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.framework.upgrade;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.awaitility.Awaitility;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Drives a released Dirigible over its public HTTP surface, as the IDE does: the in-process test
 * helpers ({@code ProjectDeployer}, the Selenide views) work on the application the test JVM runs,
 * not on a release in a container. Every call authenticates as the default administrator.
 */
public final class PreviousReleaseClient {

    private static final String WORKSPACE = "workspace";

    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(2);

    private static final Gson GSON = new Gson();

    private final String baseUrl;

    private final String authorization = "Basic " + Base64.getEncoder()
                                                          .encodeToString("admin:admin".getBytes(StandardCharsets.UTF_8));

    private final HttpClient http = HttpClient.newBuilder()
                                              .connectTimeout(Duration.ofSeconds(10))
                                              .build();

    PreviousReleaseClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * Creates a project in the administrator's workspace, and the workspace itself - a fresh instance
     * has none until the IDE first lists it.
     *
     * @param project the project
     */
    public void createProject(String project) {
        expect(send("POST", "/services/ide/workspaces/" + WORKSPACE, null, null), 201, 304);
        expect(send("POST", "/services/ide/workspaces/" + WORKSPACE + "/" + project, null, null), 201);
    }

    /**
     * Writes a new file into a workspace project.
     *
     * @param project the project
     * @param path the project-relative path
     * @param content the content
     */
    public void writeFile(String project, String path, String content) {
        expect(send("POST", "/services/ide/workspaces/" + WORKSPACE + "/" + project + "/" + path, "text/plain", content), 201);
    }

    /**
     * Runs the intent Generate - the model files and the code from them - and fails unless every code
     * generation succeeded.
     *
     * @param project the project
     * @param intentPath the project-relative path of the intent
     */
    public void generateFromIntent(String project, String intentPath) {
        HttpResponse<String> response = send("POST",
                "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + project + "&path=" + intentPath, null, null);
        expect(response, 200);
        Map<String, Object> result = GSON.fromJson(response.body(), new TypeToken<Map<String, Object>>() {}.getType());
        Object generations = result.get("codeGenerations");
        if (!(generations instanceof List<?> list) || list.isEmpty()) {
            throw new IllegalStateException("The previous release generated no code from [" + intentPath + "]: " + response.body());
        }
        for (Object generation : list) {
            if (!(generation instanceof Map<?, ?> entry) || !Boolean.TRUE.equals(entry.get("generated"))) {
                throw new IllegalStateException("The previous release failed a code generation: " + generation);
            }
        }
    }

    /**
     * Publishes a workspace project into the registry.
     *
     * @param project the project
     */
    public void publish(String project) {
        expect(send("POST", "/services/ide/publisher/" + WORKSPACE + "/" + project + "/", null, null), 200);
    }

    /**
     * Waits until a path answers 200 - a published controller, once the synchronizers compiled it.
     *
     * @param path the path
     * @param timeout how long to wait
     */
    public void awaitAvailable(String path, Duration timeout) {
        Awaitility.await()
                  .pollInterval(2, TimeUnit.SECONDS)
                  .atMost(timeout)
                  .ignoreExceptions()
                  .until(() -> send("GET", path, null, null).statusCode() == 200);
    }

    /**
     * Creates a record through a JSON endpoint.
     *
     * @param path the endpoint
     * @param record the record
     * @return the created record as the endpoint answered it
     */
    public Map<String, Object> create(String path, Map<String, Object> record) {
        HttpResponse<String> response = send("POST", path, "application/json", GSON.toJson(record));
        expect(response, 200);
        return GSON.fromJson(response.body(), new TypeToken<Map<String, Object>>() {}.getType());
    }

    /**
     * Reads a JSON list.
     *
     * @param path the endpoint
     * @return the list
     */
    public List<Map<String, Object>> list(String path) {
        HttpResponse<String> response = send("GET", path, null, null);
        expect(response, 200);
        return GSON.fromJson(response.body(), new TypeToken<List<Map<String, Object>>>() {}.getType());
    }

    /**
     * Waits until a JSON list satisfies a condition and returns it.
     *
     * @param path the endpoint
     * @param condition the condition
     * @param timeout how long to wait
     * @return the list that satisfied it
     */
    public List<Map<String, Object>> awaitList(String path, Predicate<List<Map<String, Object>>> condition, Duration timeout) {
        return Awaitility.await()
                         .pollInterval(2, TimeUnit.SECONDS)
                         .atMost(timeout)
                         .until(() -> list(path), condition);
    }

    private HttpResponse<String> send(String method, String path, String contentType, String body) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                                                 .timeout(REQUEST_TIMEOUT)
                                                 .header("Authorization", authorization)
                                                 .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                                                         : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        if (contentType != null) {
            request.header("Content-Type", contentType);
        }
        try {
            return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException ex) {
            throw new IllegalStateException("Could not call the previous release: " + method + " " + path, ex);
        } catch (InterruptedException ex) {
            Thread.currentThread()
                  .interrupt();
            throw new IllegalStateException("Interrupted calling the previous release: " + method + " " + path, ex);
        }
    }

    private static void expect(HttpResponse<String> response, int... statuses) {
        for (int status : statuses) {
            if (response.statusCode() == status) {
                return;
            }
        }
        throw new IllegalStateException(response.request()
                                                .method()
                + " " + response.uri() + " answered " + response.statusCode() + ": " + response.body());
    }
}
