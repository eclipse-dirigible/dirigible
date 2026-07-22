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
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

import java.nio.charset.StandardCharsets;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * End-to-end test for record attachments (the {@code sdk.cms.Attachments} SDK backing the generated
 * {@code function: Attachment} controllers). Drops a small client-Java {@code @Controller} that
 * calls {@code Attachments.storeUploads/open/delete} straight into the registry, force-syncs it,
 * then drives a full HTTP round-trip: multipart upload -> download the stored bytes verbatim ->
 * delete -> the file is gone. Exercises the real Spring multipart servlet path (the reason
 * {@code storeUploads} reads the already-parsed {@code getParts()} rather than commons-fileupload)
 * and the real tenant CMS.
 */
class RecordAttachmentIT extends IntegrationTest {

    /** Project segment used for the controller source. */
    private static final String PROJECT = "attachment-it";

    /** Registry-relative source path. */
    private static final String SOURCE_LOCATION = "/" + PROJECT + "/api/AttachmentsTestController.java";

    /** Fully-qualified path under the registry root. */
    private static final String REGISTRY_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + SOURCE_LOCATION;

    /** Base URL of the controller (derived from project + package + class name). */
    private static final String BASE = "/services/java/" + PROJECT + "/api/AttachmentsTestController";

    private static final long ASSERTION_TIMEOUT_SECONDS = 30;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void upload_download_delete_roundTrip() {
        writeAndSync(controllerSource());
        String content = "hello attachment round-trip";
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

        restAssuredExecutor.execute(() -> {
            // Upload a single file -> one stored attachment with its metadata and a CMS storage path.
            String path = given().multiPart("file", "note.txt", bytes, "text/plain")
                                 .when()
                                 .post(BASE + "/upload")
                                 .then()
                                 .statusCode(200)
                                 .body("[0].fileName", equalTo("note.txt"))
                                 .body("[0].contentType", equalTo("text/plain"))
                                 .body("[0].size", equalTo(bytes.length))
                                 .body("[0].path", notNullValue())
                                 .body("[0].path", startsWith("/Attachments/TestDoc/"))
                                 .extract()
                                 .path("[0].path");

            // Download -> the stored bytes come back verbatim.
            given().queryParam("path", path)
                   .when()
                   .get(BASE + "/download")
                   .then()
                   .statusCode(200)
                   .body(equalTo(content));

            // Delete -> the CMS file is removed, so a subsequent open no longer succeeds.
            given().queryParam("path", path)
                   .when()
                   .delete(BASE + "/remove")
                   .then()
                   .statusCode(200);

            given().queryParam("path", path)
                   .when()
                   .get(BASE + "/download")
                   .then()
                   .statusCode(anyOf(equalTo(404), equalTo(500)));
        }, ASSERTION_TIMEOUT_SECONDS);
    }

    @AfterEach
    void removeArtefactFromRegistry() {
        if (repository.hasResource(REGISTRY_PATH)) {
            repository.removeResource(REGISTRY_PATH);
            synchronizationProcessor.forceProcessSynchronizers();
        }
    }

    private void writeAndSync(String source) {
        repository.createResource(REGISTRY_PATH, source.getBytes(StandardCharsets.UTF_8), false, "text/x-java", true);
        synchronizationProcessor.forceProcessSynchronizers();
    }

    private static String controllerSource() {
        return """
                package api;

                import java.io.InputStream;
                import java.io.OutputStream;
                import java.util.List;

                import org.eclipse.dirigible.sdk.cms.Attachments;
                import org.eclipse.dirigible.sdk.http.Controller;
                import org.eclipse.dirigible.sdk.http.Delete;
                import org.eclipse.dirigible.sdk.http.Get;
                import org.eclipse.dirigible.sdk.http.Post;
                import org.eclipse.dirigible.sdk.http.QueryParam;
                import org.eclipse.dirigible.sdk.http.Response;

                @Controller
                public class AttachmentsTestController {

                    @Post("/upload")
                    public List<Attachments.Attachment> upload() {
                        return Attachments.storeUploads("TestDoc");
                    }

                    @Get("/download")
                    public void download(@QueryParam("path") String path) {
                        Response.setHeader("Content-Disposition", "attachment");
                        try (InputStream in = Attachments.open(path); OutputStream out = Response.getOutputStream()) {
                            in.transferTo(out);
                        } catch (java.io.IOException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Delete("/remove")
                    public void remove(@QueryParam("path") String path) {
                        Attachments.delete(path);
                    }
                }
                """;
    }

}
