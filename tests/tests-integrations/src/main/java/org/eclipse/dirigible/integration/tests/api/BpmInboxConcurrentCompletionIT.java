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
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.framework.tenant.DirigibleTestTenant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

import io.restassured.http.ContentType;

/**
 * Listing the Inbox while its tasks are being completed never fails the list (issue #7575).
 *
 * <p>
 * The list is one task query followed by per-task reads, so a task another session completes in
 * between is "not found" by its read - and, being its process's only task here, its process
 * instance is gone too. That one vanished task used to fail the whole list with a 500 for whoever
 * loaded it at that moment. The race is a matter of timing, so this lists continuously while every
 * task is being completed and requires every list call to answer: with the fix it cannot fail,
 * without it it fails whenever the race is hit.
 *
 * <p>
 * The concurrent half uses a plain HTTP client with explicit credentials:
 * {@link RestAssuredExecutor} swaps REST Assured's static base URI and authentication around each
 * call, which two threads cannot share.
 */
class BpmInboxConcurrentCompletionIT extends IntegrationTest {

    private static final String PROJECT = "bpm-inbox-concurrent-completion-it";
    private static final String PROCESS_KEY = "bpm-inbox-concurrent-completion-it-process";
    private static final String BPMN_REGISTRY_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/process.bpmn";
    private static final String GROUP_TASKS = "/services/inbox/tasks?type=groups";
    private static final int INSTANCES = 40;
    private static final long ASSERTION_TIMEOUT_SECONDS = 60;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @LocalServerPort
    private int port;

    private final HttpClient http = HttpClient.newBuilder()
                                              .connectTimeout(Duration.ofSeconds(10))
                                              .build();

    @AfterEach
    void cleanup() {
        if (repository.hasResource(BPMN_REGISTRY_PATH)) {
            repository.removeResource(BPMN_REGISTRY_PATH);
            synchronizationProcessor.forceProcessSynchronizers();
        }
    }

    @Test
    void the_group_task_list_answers_while_its_tasks_are_completed() throws Exception {
        repository.createResource(BPMN_REGISTRY_PATH, BPMN.getBytes(StandardCharsets.UTF_8), false, "application/xml", true);
        synchronizationProcessor.forceProcessSynchronizers();
        for (int i = 0; i < INSTANCES; i++) {
            startProcess(i);
        }
        List<String> taskIds = new ArrayList<>();
        restAssuredExecutor.execute(() -> taskIds.addAll(given().when()
                                                                .get(GROUP_TASKS)
                                                                .then()
                                                                .statusCode(200)
                                                                .body("findAll { it.processDefinitionId.startsWith('" + PROCESS_KEY
                                                                        + ":') }", hasSize(INSTANCES))
                                                                .extract()
                                                                .<List<String>>path("findAll { it.processDefinitionId.startsWith('"
                                                                        + PROCESS_KEY + ":') }.id")),
                ASSERTION_TIMEOUT_SECONDS);

        AtomicBoolean completing = new AtomicBoolean(true);
        CompletableFuture<List<Integer>> listings = CompletableFuture.supplyAsync(() -> {
            List<Integer> statuses = new ArrayList<>();
            while (completing.get()) {
                statuses.add(send(HttpRequest.newBuilder(uri(GROUP_TASKS))
                                             .GET()));
            }
            return statuses;
        });
        // Completed from the END of the list: every list query still holds the tail, and the lister walks
        // towards it while it disappears. Completed in list order, the lister would only ever run ahead
        // of the completions and never meet a task that vanished behind its own query.
        List<String> completionOrder = new ArrayList<>(taskIds);
        Collections.reverse(completionOrder);
        List<Integer> completions = new ArrayList<>();
        try {
            for (String taskId : completionOrder) {
                completions.add(send(HttpRequest.newBuilder(uri("/services/inbox/tasks/" + taskId))
                                                .header("Content-Type", "application/json")
                                                .POST(HttpRequest.BodyPublishers.ofString("{\"action\":\"COMPLETE\"}"))));
            }
        } finally {
            completing.set(false);
        }
        List<Integer> listingStatuses = listings.get(ASSERTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        assertTrue(completions.stream()
                              .allMatch(status -> status == 200),
                "every task should have been completed: " + completions);
        assertTrue(!listingStatuses.isEmpty(), "the list should have been loaded while the tasks were being completed");
        assertEquals(List.of(), listingStatuses.stream()
                                               .filter(status -> status != 200)
                                               .toList(),
                "no list call may fail because a listed task was completed meanwhile (of " + listingStatuses.size() + " calls)");
    }

    private void startProcess(int index) {
        String body = "{\"processDefinitionKey\":\"" + PROCESS_KEY + "\",\"businessKey\":\"" + PROCESS_KEY + "-" + index
                + "\",\"parameters\":\"{}\"}";
        restAssuredExecutor.execute(() -> given().contentType(ContentType.JSON)
                                                 .body(body)
                                                 .when()
                                                 .post("/services/bpm/bpm-processes/instance")
                                                 .then()
                                                 .statusCode(200));
    }

    private int send(HttpRequest.Builder request) {
        DirigibleTestTenant tenant = DirigibleTestTenant.createDefaultTenant();
        String credentials = Base64.getEncoder()
                                   .encodeToString((tenant.getUsername() + ":" + tenant.getPassword()).getBytes(StandardCharsets.UTF_8));
        try {
            return http.send(request.header("Authorization", "Basic " + credentials)
                                    .timeout(Duration.ofSeconds(30))
                                    .build(),
                    HttpResponse.BodyHandlers.discarding())
                       .statusCode();
        } catch (InterruptedException ex) {
            Thread.currentThread()
                  .interrupt();
            throw new IllegalStateException("Interrupted while calling the inbox", ex);
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("Failed to call the inbox", ex);
        }
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    /** One candidate-group task, the process's only step: completing it ends the process. */
    private static final String BPMN = """
            <?xml version="1.0" encoding="UTF-8"?>
            <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                         xmlns:flowable="http://flowable.org/bpmn"
                         targetNamespace="http://www.flowable.org/processdef">
              <process id="%s" name="Concurrent Completion" isExecutable="true">
                <startEvent id="start"/>
                <sequenceFlow id="f1" sourceRef="start" targetRef="approve"/>
                <userTask id="approve" name="Approve" flowable:candidateGroups="ADMINISTRATOR"/>
                <sequenceFlow id="f2" sourceRef="approve" targetRef="end"/>
                <endEvent id="end"/>
              </process>
            </definitions>
            """.formatted(PROCESS_KEY);
}
