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
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.annotation.DirtiesContext;

/**
 * End-to-end test for the export representation of {@code GET /services/core/repository}: a
 * collection downloads as a zip of its content including its subfolders, a resource downloads as
 * the file itself, and the plain listing representation stays untouched.
 */
// One Dirigible boot for the whole class: each method cleans up after itself (or creates only
// collision-free state), so the per-method context reset inherited from IntegrationTest would only
// add boot time per test.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RepositoryExportIT extends IntegrationTest {

    private static final String ENDPOINT = "/services/core/repository";

    private static final String FOLDER = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/repository-export-it";

    private static final String FOLDER_NAME = "repository-export-it";

    private static final String TOP_FILE = FOLDER + "/top.txt";

    private static final String NESTED_FILE = FOLDER + "/nested/deep.txt";

    private static final byte[] CONTENT = "exported content".getBytes(StandardCharsets.UTF_8);

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @BeforeEach
    void createFolderWithASubfolder() {
        repository.createResource(TOP_FILE, CONTENT, false, "text/plain", true);
        repository.createResource(NESTED_FILE, CONTENT, false, "text/plain", true);
    }

    @AfterEach
    void removeFolder() {
        if (repository.hasCollection(FOLDER)) {
            repository.removeCollection(FOLDER);
        }
    }

    @Test
    void exportsAFolderAsAZipOfItsContentWithItsSubfolders() {
        restAssuredExecutor.execute(() -> {
            byte[] zip = given().queryParam("export", "true")
                                .when()
                                .get(ENDPOINT + FOLDER)
                                .then()
                                .statusCode(200)
                                .header(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment"))
                                .header(HttpHeaders.CONTENT_DISPOSITION, containsString(FOLDER_NAME + "-"))
                                .header(HttpHeaders.CONTENT_DISPOSITION, containsString(".zip"))
                                .extract()
                                .asByteArray();

            assertThat(entryNames(zip)).contains(FOLDER_NAME + "/top.txt", FOLDER_NAME + "/nested/deep.txt");
        });
    }

    @Test
    void exportsAFileAsTheFileItself() {
        restAssuredExecutor.execute(() -> {
            byte[] content = given().queryParam("export", "true")
                                    .when()
                                    .get(ENDPOINT + TOP_FILE)
                                    .then()
                                    .statusCode(200)
                                    .header(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment"))
                                    .header(HttpHeaders.CONTENT_DISPOSITION, containsString("top.txt"))
                                    .extract()
                                    .asByteArray();

            assertThat(content).isEqualTo(CONTENT);
        });
    }

    @Test
    void answersNotFoundForAPathThatDoesNotExist() {
        restAssuredExecutor.execute(() -> given().queryParam("export", "true")
                                                 .when()
                                                 .get(ENDPOINT + FOLDER + "/there-is-no-such-file.txt")
                                                 .then()
                                                 .statusCode(404));
    }

    /** The export representation must not shadow the listing the Repository tree is built from. */
    @Test
    void stillListsACollectionWithoutTheExportParameter() {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(ENDPOINT + FOLDER)
                                                 .then()
                                                 .statusCode(200)
                                                 .contentType(containsString("application/json"))
                                                 .body(containsString("top.txt")));
    }

    private static List<String> entryNames(byte[] zip) {
        List<String> names = new ArrayList<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zip))) {
            for (ZipEntry entry = zipInputStream.getNextEntry(); entry != null; entry = zipInputStream.getNextEntry()) {
                names.add(entry.getName());
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read the exported archive", e);
        }
        return names;
    }
}
