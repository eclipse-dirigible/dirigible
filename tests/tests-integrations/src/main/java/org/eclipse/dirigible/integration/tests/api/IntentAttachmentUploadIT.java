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
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
 * The generated {@code function: Attachment} surface at runtime, through the published application:
 * an upload stores a row that carries the metadata of the file it just wrote, that file downloads
 * through the generated endpoint, and a plain {@code POST} of the same entity still cannot forge
 * those columns.
 *
 * <p>
 * The five metadata columns ({@code FileName}, {@code ContentType}, {@code FileSize},
 * {@code StoragePath}, {@code Uuid}) are injected read-only, so they belong to the set a user
 * create must not supply (#7549). #7568 enforced that at the top of the generated repository's
 * {@code save()} - the create path of every system writer, the upload handler included - and every
 * upload then stored a row pointing at nothing: null metadata, an orphaned CMS object, a download
 * that failed on the null path (#7620). The drop now lives on the REST create verb, where the other
 * rules of its shape already are, and the repository keeps what a system writer assigns. The three
 * claims are asserted at the outermost layer, over HTTP against the generated controllers, because
 * a test that only reads the rendered template proved nothing about the row (#7568 shipped green).
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentAttachmentUploadIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "attach";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String PEOPLE = API + "/person/PersonController";
    private static final String PHOTOS = API + "/person/PersonPhotoController";
    private static final long TIMEOUT_SECONDS = 90;

    /** A master and the attachment child it owns - the shape the issue reproduces with. */
    private static final String INTENT_YAML = """
            name: attach
            description: attachment fixture - an upload keeps the metadata of the file it stored

            entities:
              - name: Person
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, length: 100 }

              # function: Attachment injects FileName / ContentType / FileSize / StoragePath / Uuid as
              # read-only columns next to the audit ones; Caption is the one ordinary column, so the
              # forged create below can show that an authored value is kept while the system-owned
              # ones are dropped.
              - name: PersonPhoto
                function: Attachment
                fields:
                  - { name: caption, type: string, length: 100 }
                relations:
                  - { name: Person, kind: manyToOne, to: Person, composition: true, required: true }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void an_upload_keeps_the_metadata_of_the_file_it_stored_and_a_post_cannot_forge_it() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int person = create(PEOPLE, "{\"Name\":\"Ada\"}");
        byte[] bytes = "not really a png, but every byte of it must come back".getBytes(StandardCharsets.UTF_8);

        // (1) The upload answers with the metadata of the file it just wrote to the tenant CMS...
        restAssuredExecutor.execute(() -> given().multiPart("file", "photo.png", bytes, "image/png")
                                                 .when()
                                                 .post(PHOTOS + "/upload?Person=" + person)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("", hasSize(1))
                                                 .body("[0].FileName", equalTo("photo.png"))
                                                 .body("[0].ContentType", equalTo("image/png"))
                                                 .body("[0].FileSize", equalTo(bytes.length))
                                                 .body("[0].StoragePath", startsWith("/Attachments/PersonPhoto/"))
                                                 .body("[0].Uuid", notNullValue())
                                                 .body("[0].Person", equalTo(person)),
                TIMEOUT_SECONDS);

        // ...and the STORED row carries it - the claim the issue is about. Read back through the
        // master-scoped list and then by id, never from the upload's own return value.
        int photo = onlyPhotoOf(person);
        restAssuredExecutor.execute(() -> {
            JsonPath stored = given().when()
                                     .get(PHOTOS + "/" + photo)
                                     .then()
                                     .statusCode(200)
                                     .body("FileName", equalTo("photo.png"))
                                     .body("ContentType", equalTo("image/png"))
                                     .body("FileSize", equalTo(bytes.length))
                                     .body("StoragePath", startsWith("/Attachments/PersonPhoto/"))
                                     .body("Person", equalTo(person))
                                     .extract()
                                     .jsonPath();
            // The uuid is the per-upload folder the SDK stored the file under, so the two columns must
            // name one CMS object - not merely both be present.
            String storagePath = stored.getString("StoragePath");
            assertTrue(storagePath.endsWith("/" + stored.getString("Uuid") + "/photo.png"),
                    "the stored Uuid must be the folder of the stored file, got: " + storagePath);
        });

        // (2) The file downloads through the generated endpoint, byte for byte, under its own name.
        restAssuredExecutor.execute(() -> {
            byte[] served = given().when()
                                   .get(PHOTOS + "/" + photo + "/download")
                                   .then()
                                   .statusCode(200)
                                   .header("Content-Type", startsWith("image/png"))
                                   .header("Content-Disposition", containsString("photo.png"))
                                   .extract()
                                   .asByteArray();
            assertArrayEquals(bytes, served, "the download must stream the uploaded bytes verbatim");
        });

        // (3) #7549 stays fixed: a plain create carrying the five columns stores null for every one of
        // them, while the authored column it carried is kept.
        int forged = create(PHOTOS,
                "{\"Person\":" + person + ",\"Caption\":\"forged\",\"FileName\":\"forged.png\","
                        + "\"ContentType\":\"image/png\",\"FileSize\":1,\"StoragePath\":\"/Attachments/PersonPhoto/forged.png\","
                        + "\"Uuid\":\"forged\"}");
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(PHOTOS + "/" + forged)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Caption", equalTo("forged"))
                                                 .body("FileName", nullValue())
                                                 .body("ContentType", nullValue())
                                                 .body("FileSize", nullValue())
                                                 .body("StoragePath", nullValue())
                                                 .body("Uuid", nullValue()));
    }

    /** The one photo row of a person, through the master-scoped list the details panel reads. */
    private int onlyPhotoOf(int person) {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(given().when()
                                                        .get(PHOTOS + "?Person=" + person)
                                                        .then()
                                                        .statusCode(200)
                                                        .body("", hasSize(1))
                                                        .extract()
                                                        .path("[0].Id")));
        return id.get();
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
        writeIntent();
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
