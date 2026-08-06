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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import java.nio.charset.StandardCharsets;

import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.restassured.http.ContentType;

/**
 * End-to-end test of the Documents REST contract - the surface both the Document Storage
 * perspective and the application shells' Documents section talk to.
 * <p>
 * It is written against {@link #BASE} so the identical assertions can be pointed at the JavaScript
 * predecessor ({@code /services/js/documents/api/documents.js}) and at the Java endpoint, which is
 * what makes "drop-in replacement" a verified claim rather than an intention. Load-bearing details
 * asserted here: the root is listed with NO {@code path} parameter (which is how both user
 * interfaces open), {@code __internal} never appears in a listing and is not addressable, and the
 * child payload carries the {@code type} / {@code path} / {@code readable} fields the interfaces
 * render.
 */
class DocumentsApiIT extends IntegrationTest {

    /**
     * The endpoint under test. Run with
     * {@code -Ddocuments.api.base=/services/js/documents/api/documents.js} to replay the same
     * assertions against the JavaScript predecessor.
     */
    private static final String BASE = System.getProperty("documents.api.base", "/services/documents");

    private static final String FOLDER = "documents-api-it";
    private static final String FOLDER_PATH = "/" + FOLDER;
    private static final String FILE_NAME = "note.txt";
    private static final String FILE_PATH = FOLDER_PATH + "/" + FILE_NAME;
    private static final String CONTENT = "documents api integration test";

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void documents_lifecycle_over_the_rest_contract() {
        restAssuredExecutor.execute(() -> {
            deleteQuietly(FOLDER_PATH);

            // the ROOT is listed with no path parameter at all
            given().when()
                   .get(BASE)
                   .then()
                   .statusCode(200)
                   .body("path", equalTo("/"))
                   .body("children.name", not(hasItem("__internal")));

            given().contentType(ContentType.JSON)
                   .body("{\"parentFolder\":\"/\",\"name\":\"" + FOLDER + "\"}")
                   .when()
                   .post(BASE + "/folder")
                   .then()
                   .statusCode(200)
                   .body("path", equalTo(FOLDER_PATH));

            given().when()
                   .get(BASE)
                   .then()
                   .statusCode(200)
                   .body("children.name", hasItem(FOLDER));

            given().multiPart("file", FILE_NAME, CONTENT.getBytes(StandardCharsets.UTF_8), "text/plain")
                   .when()
                   .post(BASE + "?path=" + FOLDER_PATH)
                   .then()
                   .statusCode(200);

            given().when()
                   .get(BASE + "?path=" + FOLDER_PATH)
                   .then()
                   .statusCode(200)
                   .body("path", equalTo(FOLDER_PATH))
                   .body("children.name", contains(FILE_NAME))
                   .body("children[0].type", equalTo("cmis:document"))
                   .body("children[0].path", equalTo(FILE_PATH))
                   .body("children[0].readable", equalTo(true));

            given().when()
                   .get(BASE + "/preview?path=" + FILE_PATH)
                   .then()
                   .statusCode(200)
                   .body(equalTo(CONTENT));

            given().when()
                   .get(BASE + "/download?path=" + FILE_PATH)
                   .then()
                   .statusCode(200)
                   .header("Content-Disposition", "attachment; filename=\"" + FILE_NAME + "\"");

            given().when()
                   .get(BASE + "/zip?path=" + FOLDER_PATH)
                   .then()
                   .statusCode(200);

            given().contentType(ContentType.JSON)
                   .body("{\"path\":\"" + FILE_PATH + "\",\"name\":\"renamed.txt\"}")
                   .when()
                   .put(BASE)
                   .then()
                   .statusCode(anyOf204Or200());

            given().when()
                   .get(BASE + "?path=" + FOLDER_PATH)
                   .then()
                   .statusCode(200)
                   .body("children.name", contains("renamed.txt"));

            deleteQuietly(FOLDER_PATH);

            given().when()
                   .get(BASE)
                   .then()
                   .statusCode(200)
                   .body("children.name", not(hasItem(FOLDER)));
        });
    }

    @Test
    void listing_the_root_by_explicit_slash_now_works() {
        // The JavaScript predecessor answered 400 here, which is why both user interfaces list the
        // root with NO path parameter at all. The Java endpoint treats "/" as the root, so the quirk
        // is gone; listing without a parameter keeps working either way (asserted above).
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(BASE + "?path=/")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("path", equalTo("/")));
    }

    @Test
    void the_internal_folder_is_not_addressable() {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(BASE + "?path=/__internal")
                                                 .then()
                                                 .statusCode(403));
    }

    /** Rename answers 204 from the Java endpoint and 200 from its JavaScript predecessor. */
    private static org.hamcrest.Matcher<Integer> anyOf204Or200() {
        return org.hamcrest.Matchers.anyOf(equalTo(200), equalTo(204));
    }

    private static void deleteQuietly(String path) {
        given().contentType(ContentType.JSON)
               .body("[\"" + path + "\"]")
               .when()
               .delete(BASE);
    }
}
