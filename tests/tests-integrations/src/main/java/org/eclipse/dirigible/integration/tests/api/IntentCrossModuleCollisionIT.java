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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.google.gson.Gson;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Cross-module collision coverage: two intent modules that author a SAME-NAMED entity
 * ({@code Recurrence}) and a SAME-NAMED reaction (the {@code cancelRecurrence} transition) must
 * deploy together on one instance with both modules fully live.
 *
 * <p>
 * This is exactly the shape that used to fail with the shared {@code gen.events} package and
 * simple-class-name keying: the two {@code CancelRecurrenceTransition} classes collided by FQN
 * (registry-wide client-Java compilation refused the duplicate), and the two
 * {@code RecurrenceController}/{@code RecurrenceRepository} beans collided by bean name (the
 * container keeps the first and drops the second silently), as did the Hibernate entity name in the
 * shared SessionFactory. The fix namespaces the events package per module
 * ({@code gen.events.<module>}) and module-qualifies the generated bean/entity names.
 *
 * <p>
 * Per the emission contract (kf-catalog §9a.7) the assertions target the OUTERMOST observable
 * layer: both entity controllers answer over REST and both generated transition endpoints flip the
 * status - not merely that the files were emitted.
 */
class IntentCrossModuleCollisionIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_A = "collide-a";
    private static final String PROJECT_B = "collide-b";

    /** The same application shape authored twice under different module names. */
    private static String intentYaml(String moduleName) {
        return """
                name: %s
                description: cross-module collision fixture - same-named entity and reaction in two modules

                entities:
                  - name: RecurrenceStatus
                    kind: setting
                    fields:
                      - { name: id,   type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string,  required: true, length: 100 }

                  - name: Recurrence
                    fields:
                      - { name: id,   type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string,  required: true, length: 100 }
                    relations:
                      - { name: Status, kind: manyToOne, to: RecurrenceStatus, function: EntityStatus, init: 1 }

                transitions:
                  - { name: cancelRecurrence, forEntity: Recurrence, from: [1], setStatus: 2 }

                seeds:
                  - name: statuses
                    entity: RecurrenceStatus
                    rows:
                      - { id: 1, name: Active }
                      - { id: 2, name: Cancelled }
                """.formatted(moduleName);
    }

    @Autowired
    private IRepository repository;
    @Autowired
    private RestAssuredExecutor restAssuredExecutor;
    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void two_modules_with_same_named_entity_and_reaction_deploy_together_without_collisions() {
        generateProject(PROJECT_A);
        generateProject(PROJECT_B);

        assertNamespacedEmission(PROJECT_A, "collide_a");
        assertNamespacedEmission(PROJECT_B, "collide_b");

        publishProject(PROJECT_A);
        publishProject(PROJECT_B);
        synchronizationProcessor.forceProcessSynchronizers();

        assertModuleLive(PROJECT_A, "collide_a");
        assertModuleLive(PROJECT_B, "collide_b");
    }

    /** Layer 1: the generated sources carry the module-scoped package and qualified names. */
    private void assertNamespacedEmission(String project, String module) {
        String transition = contentOf(project, "gen/events/" + module + "/CancelRecurrenceTransition.java");
        assertTrue(transition.contains("package gen.events." + module + ";"),
                "the transition handler must land in the module-scoped events package");
        assertTrue(transition.contains("@Component(\"" + module + "_CancelRecurrenceTransition\")"),
                "the transition handler bean name must be module-qualified");

        String entity = contentOf(project, "gen/" + module + "/data/recurrence/RecurrenceEntity.java");
        assertTrue(entity.contains("@Entity(name = \"" + module + "_RecurrenceEntity\")"),
                "the Hibernate entity name must be module-qualified (one shared SessionFactory)");

        String repositoryClass = contentOf(project, "gen/" + module + "/data/recurrence/RecurrenceRepository.java");
        assertTrue(repositoryClass.contains("@Component(\"" + module + "_RecurrenceRepository\")"),
                "the repository bean name must be module-qualified");

        String controller = contentOf(project, "gen/" + module + "/api/recurrence/RecurrenceController.java");
        assertTrue(controller.contains("@Component(\"" + module + "_RecurrenceController\")"),
                "the controller bean name must be module-qualified");
    }

    /**
     * Layer 2: the module actually WORKS on the running instance - the entity controller answers (bean
     * + Hibernate registration survived the co-deploy) and the generated transition endpoint flips the
     * status (the events handler class registered under its distinct FQN).
     */
    private void assertModuleLive(String project, String module) {
        String api = "/services/java/" + project + "/gen/" + module + "/api";
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body("{\"Name\":\"row of " + project + "\"}")
                                                        .when()
                                                        .post(api + "/recurrence/RecurrenceController")
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")),
                60);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(api + "/recurrence/RecurrenceController/" + id.get())
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Name", equalTo("row of " + project))
                                                 .body("Status", equalTo(1)));
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"id\":" + id.get() + "}")
                                                 .when()
                                                 .post("/services/java/" + project + "/gen/events/" + module
                                                         + "/CancelRecurrenceTransition/run")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Status", equalTo(2)));
    }

    /** Write the intent and drive model-to-code from the generate response's own plan. */
    private void generateProject(String project) {
        writeIntent(project, intentYaml(project));
        AtomicReference<List<Map<String, Object>>> plan = new AtomicReference<>();
        restAssuredExecutor.execute(() -> plan.set(given().when()
                                                          .post("/services/ide/intent/generate?workspace=" + WORKSPACE + "&project="
                                                                  + project + "&path=app.intent")
                                                          .then()
                                                          .statusCode(200)
                                                          .extract()
                                                          .jsonPath()
                                                          .getList("codeGenerations")));
        for (Map<String, Object> codeGeneration : plan.get()) {
            String template = String.valueOf(codeGeneration.get("templateId"));
            String modelPath = String.valueOf(codeGeneration.get("path"));
            String parameters = new Gson().toJson(codeGeneration.get("parameters"));
            String payload = "{\"template\":\"" + template + "\",\"parameters\":" + parameters + "}";
            restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                     .body(payload)
                                                     .when()
                                                     .post("/services/js/service-generate/generate.mjs/model/" + WORKSPACE + "/" + project
                                                             + "?path=" + modelPath)
                                                     .then()
                                                     .statusCode(201));
        }
    }

    private void publishProject(String project) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .post("/services/ide/publisher/" + WORKSPACE + "/" + project + "/")
                                                 .then()
                                                 .statusCode(200));
    }

    private void writeIntent(String project, String yaml) {
        String path = projectPath(project) + "/app.intent";
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(yaml.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, yaml.getBytes(StandardCharsets.UTF_8));
        }
    }

    private String contentOf(String project, String fileName) {
        return new String(repository.getResource(projectPath(project) + "/" + fileName)
                                    .getContent(),
                StandardCharsets.UTF_8);
    }

    private static String projectPath(String project) {
        return IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + project;
    }

    @AfterEach
    void cleanup() {
        for (String project : List.of(PROJECT_A, PROJECT_B)) {
            restAssuredExecutor.execute(() -> given().when()
                                                     .delete("/services/ide/publisher/" + WORKSPACE + "/" + project)
                                                     .then()
                                                     .statusCode(greaterThanOrEqualTo(200)));
            if (repository.hasCollection(projectPath(project))) {
                repository.removeCollection(projectPath(project));
            }
        }
    }
}
