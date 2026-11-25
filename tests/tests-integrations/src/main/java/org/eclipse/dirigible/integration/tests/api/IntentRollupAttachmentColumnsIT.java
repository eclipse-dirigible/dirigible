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
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.restassured.path.json.JsonPath;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

/**
 * A roll-up reads the columns the generator injects into a {@code function: Attachment} child
 * (dirigible #7947): {@code op: latest} mirrors the newest upload's {@code StoragePath} by its
 * {@code CreatedAt} onto the parent, and a count filters on the upload's {@code ContentType}.
 *
 * <p>
 * The rows are written by the generated upload route, the one writer that fills those columns, and
 * the totals are read back from the parent over HTTP - so the generated handlers compiling and
 * running against the injected properties is what is proved.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentRollupAttachmentColumnsIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "rollupattachment";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String COMPANIES = API + "/company/CompanyController";
    private static final String LOGOS = API + "/company/CompanyLogoController";
    private static final long TIMEOUT_SECONDS = 90;
    /** The roll-up runs off the child's topics, after the commit. */
    private static final long ROLLUP_TIMEOUT_SECONDS = 30;

    private static final String INTENT_YAML = """
            name: rollupattachment
            description: roll-ups over the injected file and audit columns of an attachment child

            entities:
              - name: Company
                fields:
                  - { name: id,        type: integer, primaryKey: true, generated: true }
                  - { name: name,      type: string, length: 100 }
                  - { name: logo,      type: string, length: 1000, readOnly: true }
                  - { name: pngImages, type: integer, readOnly: true }
              - name: CompanyLogo
                function: Attachment
                relations:
                  - { name: Company, kind: manyToOne, to: Company, composition: true, required: true }

            rollups:
              - { name: latestLogo, entity: CompanyLogo, via: Company, field: logo, op: latest, of: StoragePath, by: CreatedAt }
              - name: pngImages
                entity: CompanyLogo
                via: Company
                field: pngImages
                where:
                  - { field: ContentType, op: eq, value: image/png }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void the_newest_upload_is_mirrored_onto_the_parent_and_a_count_filters_on_its_content_type() {
        writeIntent();
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int company = create(COMPANIES, "{\"Name\":\"Acme\"}");

        JsonPath png = upload(company, "logo.png", "image/png");
        awaitRollups(company, png.getString("[0].StoragePath"), 1);

        // A newer upload is the logo now, and a JPEG is not counted as a PNG.
        JsonPath jpeg = upload(company, "logo.jpg", "image/jpeg");
        awaitRollups(company, jpeg.getString("[0].StoragePath"), 1);

        // Deleting it brings the previous upload back.
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete(LOGOS + "/" + jpeg.getInt("[0].Id"))
                                                 .then()
                                                 .statusCode(200),
                TIMEOUT_SECONDS);
        awaitRollups(company, png.getString("[0].StoragePath"), 1);

        // A second PNG is both the newest upload and the second one counted.
        JsonPath secondPng = upload(company, "logo-2.png", "image/png");
        awaitRollups(company, secondPng.getString("[0].StoragePath"), 2);
    }

    private JsonPath upload(int company, String name, String contentType) {
        AtomicReference<JsonPath> stored = new AtomicReference<>();
        byte[] bytes = ("the bytes of " + name).getBytes(StandardCharsets.UTF_8);
        restAssuredExecutor.execute(() -> stored.set(given().multiPart("file", name, bytes, contentType)
                                                            .when()
                                                            .post(LOGOS + "/upload?Company=" + company)
                                                            .then()
                                                            .statusCode(200)
                                                            .extract()
                                                            .jsonPath()),
                TIMEOUT_SECONDS);
        return stored.get();
    }

    private void awaitRollups(int company, String logo, int pngImages) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(COMPANIES + "/" + company)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Logo", equalTo(logo))
                                                 .body("PngImages", equalTo(pngImages)),
                ROLLUP_TIMEOUT_SECONDS);
    }

    private int create(String controller, String body) {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body(body)
                                                        .when()
                                                        .post(controller)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")),
                TIMEOUT_SECONDS);
        return id.get();
    }

    private void generateProject() {
        AtomicReference<List<Map<String, Object>>> plan = new AtomicReference<>();
        restAssuredExecutor.execute(() -> plan.set(given().when()
                                                          .post("/services/ide/intent/generate?workspace=" + WORKSPACE + "&project="
                                                                  + PROJECT + "&path=app.intent")
                                                          .then()
                                                          .statusCode(200)
                                                          .extract()
                                                          .jsonPath()
                                                          .getList("codeGenerations")));
        for (Map<String, Object> codeGeneration : plan.get()) {
            assertEquals(Boolean.TRUE, codeGeneration.get("generated"),
                    "generating code from " + codeGeneration.get("path") + " failed: " + codeGeneration.get("error"));
        }
    }

    private void publishProject() {
        restAssuredExecutor.execute(() -> given().when()
                                                 .post("/services/ide/publisher/" + WORKSPACE + "/" + PROJECT + "/")
                                                 .then()
                                                 .statusCode(200));
    }

    private void writeIntent() {
        String path = PROJECT_PATH + "/app.intent";
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(INTENT_YAML.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, INTENT_YAML.getBytes(StandardCharsets.UTF_8));
        }
    }

    @AfterEach
    void cleanup() {
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete("/services/ide/publisher/" + WORKSPACE + "/" + PROJECT)
                                                 .then()
                                                 .statusCode(greaterThanOrEqualTo(200)));
        if (repository.hasCollection(PROJECT_PATH)) {
            repository.removeCollection(PROJECT_PATH);
        }
        synchronizationProcessor.forceProcessSynchronizers();
    }
}
