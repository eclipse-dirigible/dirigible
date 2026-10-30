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

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.MultiPartConfig;
import io.restassured.config.MultipartMode;
import io.restassured.config.RestAssuredConfig;
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

    /**
     * A browser sends a multipart file name as UTF-8; RestAssured's strict default sends it as ASCII.
     */
    private static final RestAssuredConfig UTF8_MULTIPART = RestAssured.config()
                                                                       .httpClient(HttpClientConfig.httpClientConfig()
                                                                                                   .multipartMode(
                                                                                                           MultipartMode.BROWSER_COMPATIBLE))
                                                                       .multiPartConfig(MultiPartConfig.multiPartConfig()
                                                                                                       .defaultCharset(
                                                                                                               StandardCharsets.UTF_8));

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

        // (4) #8100: a required composition only checked that the master was named. A master that does
        // not exist is now refused - by the plain create and by the upload, which refuses before it
        // stores a file - and no photo row is left behind for it.
        int missing = 999999;
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Person\":" + missing + ",\"Caption\":\"orphan\"}")
                                                 .when()
                                                 .post(PHOTOS)
                                                 .then()
                                                 .statusCode(400)
                                                 .body(containsString("Person " + missing + " does not exist")));
        restAssuredExecutor.execute(() -> given().multiPart("file", "orphan.png", bytes, "image/png")
                                                 .when()
                                                 .post(PHOTOS + "/upload?Person=" + missing)
                                                 .then()
                                                 .statusCode(400)
                                                 .body(containsString("Person " + missing + " does not exist")));
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(PHOTOS + "?Person=" + missing)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("", hasSize(0)));

        // (5) #8221: a name outside ISO-8859-1 made the raw quoted header invalid, and the container
        // dropped the whole Content-Disposition, so the browser proposed "download" as the save name. The
        // name now goes out as an RFC 6266 / 5987 pair - an ASCII fallback plus the percent-encoded UTF-8
        // filename* - for both disposition types, so saving from the inline viewer proposes it as well.
        // The space checks that it is encoded as %20, never as the form-encoding '+'.
        String storedName = "\u0424\u0430\u043a\u0442\u0443\u0440\u0430 1.pdf";
        AtomicInteger invoice = new AtomicInteger();
        restAssuredExecutor.execute(() -> invoice.set(given().config(UTF8_MULTIPART)
                                                             .multiPart("file", storedName, bytes, "application/pdf")
                                                             .when()
                                                             .post(PHOTOS + "/upload?Person=" + person)
                                                             .then()
                                                             .statusCode(200)
                                                             .body("[0].FileName", equalTo(storedName))
                                                             .extract()
                                                             .path("[0].Id")));
        for (String type : List.of("attachment", "inline")) {
            String query = "inline".equals(type) ? "?disposition=inline" : "";
            restAssuredExecutor.execute(() -> {
                String disposition = given().when()
                                            .get(PHOTOS + "/" + invoice.get() + "/download" + query)
                                            .then()
                                            .statusCode(200)
                                            .extract()
                                            .header("Content-Disposition");
                assertEquals(type + "; filename=\"_______ 1.pdf\"; filename*=UTF-8''%D0%A4%D0%B0%D0%BA%D1%82%D1%83%D1%80%D0%B0%201.pdf",
                        disposition, "the " + type + " download must carry the RFC 6266 / 5987 pair");
                String encoded = disposition.substring(disposition.indexOf("filename*=UTF-8''") + "filename*=UTF-8''".length());
                assertEquals(storedName, URLDecoder.decode(encoded, StandardCharsets.UTF_8),
                        "the decoded " + type + " filename* must be the stored FileName, the browser's default save name");
            });
        }
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
