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
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import java.nio.charset.StandardCharsets;

import org.eclipse.dirigible.components.base.http.roles.Roles;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.framework.security.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.restassured.http.ContentType;

/**
 * End-to-end test of the dynamic CMS access control: an administrator grants a role access to a
 * path and it takes effect for the next request, with no job, no artefact and no waiting.
 * <p>
 * Each assertion here is a failure mode of the mechanism this replaces: propagation took up to a
 * minute through a Quartz job and two caches; a restriction on a folder never removed it from a
 * listing (the resolver it used matched an exact path only); and grants lived in a global table
 * while the content they guard is per tenant.
 */
class CmsAccessControlIT extends IntegrationTest {

    private static final String DOCUMENTS = "/services/documents";
    private static final String ACCESS = DOCUMENTS + "/access";

    /** The raw content path - the same tenant content, served by file path. */
    private static final String CMS = "/services/cms";

    private static final String FOLDER = "cms-access-it";
    private static final String FOLDER_PATH = "/" + FOLDER;
    private static final String FILE_NAME = "secret.txt";
    private static final String GRANTED_ROLE = "cms-access-it-role";

    private static final String ADMIN = "cms-access-it-admin";
    private static final String PLAIN = "cms-access-it-user";
    private static final String PASSWORD = "cms-access-it-password";

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SecurityUtil securityUtil;

    /**
     * The base class dirties the Spring context after every test method, so each test starts from a
     * clean database - the users are created per test and nothing needs tearing down.
     */
    @BeforeEach
    void createUsers() {
        securityUtil.createUserInDefaultTenant(ADMIN, PASSWORD, Roles.RoleNames.ADMINISTRATOR);
        securityUtil.createUserInDefaultTenant(PLAIN, PASSWORD);
    }

    @Test
    void a_read_grant_hides_the_folder_from_everybody_without_the_role_and_takes_effect_at_once() {
        restAssuredExecutor.execute(() -> createFolder(), ADMIN, PASSWORD);

        // before the grant the folder is visible to any authenticated user - the CMS is open by
        // default and a rule is what restricts it
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(DOCUMENTS)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("children.name", hasItem(FOLDER)),
                PLAIN, PASSWORD);

        restAssuredExecutor.execute(() -> grant(FOLDER_PATH, "READ", GRANTED_ROLE), ADMIN, PASSWORD);

        // NO waiting: the very next request sees it. The folder disappears from the listing AND is
        // unreachable by direct path - the old resolver did neither.
        restAssuredExecutor.execute(() -> {
            given().when()
                   .get(DOCUMENTS)
                   .then()
                   .statusCode(200)
                   .body("children.name", not(hasItem(FOLDER)));

            given().when()
                   .get(DOCUMENTS + "?path=" + FOLDER_PATH)
                   .then()
                   .statusCode(403);
        }, PLAIN, PASSWORD);

        // an administrator does not hold the granted role either, so the rule applies to them too -
        // access is by role, not by privilege level
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(DOCUMENTS + "?path=" + FOLDER_PATH)
                                                 .then()
                                                 .statusCode(403),
                ADMIN, PASSWORD);

        // revoking restores it, again on the next request
        restAssuredExecutor.execute(() -> revoke(FOLDER_PATH, "READ", GRANTED_ROLE), ADMIN, PASSWORD);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(DOCUMENTS + "?path=" + FOLDER_PATH)
                                                 .then()
                                                 .statusCode(200),
                PLAIN, PASSWORD);
    }

    @Test
    void the_effective_grants_report_what_a_path_declares_and_what_it_inherits() {
        restAssuredExecutor.execute(() -> {
            createFolder();
            grant(FOLDER_PATH, "READ", GRANTED_ROLE);

            given().when()
                   .get(ACCESS + "?path=" + FOLDER_PATH)
                   .then()
                   .statusCode(200)
                   .body("path", equalTo(FOLDER_PATH))
                   .body("own.role", hasItem(GRANTED_ROLE));

            // a child declares nothing of its own but inherits the parent's grant
            given().when()
                   .get(ACCESS + "?path=" + FOLDER_PATH + "/child")
                   .then()
                   .statusCode(200)
                   .body("own", equalTo(java.util.List.of()))
                   .body("inherited.role", hasItem(GRANTED_ROLE));
        }, ADMIN, PASSWORD);
    }

    @Test
    void managing_access_requires_an_administrative_role() {
        restAssuredExecutor.execute(() -> {
            given().when()
                   .get(ACCESS + "?path=/")
                   .then()
                   .statusCode(403);

            given().contentType(ContentType.JSON)
                   .body(grantBody("/", "READ", GRANTED_ROLE))
                   .when()
                   .put(ACCESS)
                   .then()
                   .statusCode(403);
        }, PLAIN, PASSWORD);
    }

    /**
     * A grant must govern every way the content can be read, not only the Documents API. The raw
     * content path serves the same tenant files by path, so if it ignored the grants, restricting a
     * folder would hide it from the user interface while leaving every file under it downloadable by
     * any authenticated caller who knows - or guesses - the path.
     */
    @Test
    void a_read_grant_also_blocks_the_raw_cms_content_path() {
        restAssuredExecutor.execute(() -> {
            createFolder();
            upload();
        }, ADMIN, PASSWORD);

        // open by default: both surfaces serve the file while no rule exists
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(CMS + FOLDER_PATH + "/" + FILE_NAME)
                                                 .then()
                                                 .statusCode(200),
                PLAIN, PASSWORD);

        restAssuredExecutor.execute(() -> grant(FOLDER_PATH, "READ", GRANTED_ROLE), ADMIN, PASSWORD);

        restAssuredExecutor.execute(() -> {
            given().when()
                   .get(DOCUMENTS + "/download?path=" + FOLDER_PATH + "/" + FILE_NAME)
                   .then()
                   .statusCode(403);

            given().when()
                   .get(CMS + FOLDER_PATH + "/" + FILE_NAME)
                   .then()
                   .statusCode(403);
        }, PLAIN, PASSWORD);
    }

    @Test
    void the_internal_folders_are_not_grantable() {
        restAssuredExecutor.execute(() -> given().contentType(ContentType.JSON)
                                                 .body(grantBody("/__internal", "READ", GRANTED_ROLE))
                                                 .when()
                                                 .put(ACCESS)
                                                 .then()
                                                 .statusCode(400),
                ADMIN, PASSWORD);
    }

    private static void createFolder() {
        given().contentType(ContentType.JSON)
               .body("{\"parentFolder\":\"/\",\"name\":\"" + FOLDER + "\"}")
               .when()
               .post(DOCUMENTS + "/folder");
    }

    private static void upload() {
        given().multiPart("file", FILE_NAME, "secret".getBytes(StandardCharsets.UTF_8))
               .when()
               .post(DOCUMENTS + "?path=" + FOLDER_PATH)
               .then()
               .statusCode(200);
    }

    private static void grant(String path, String method, String role) {
        given().contentType(ContentType.JSON)
               .body(grantBody(path, method, role))
               .when()
               .put(ACCESS)
               .then()
               .statusCode(200);
    }

    private static void revoke(String path, String method, String role) {
        given().contentType(ContentType.JSON)
               .body(grantBody(path, method, role))
               .when()
               .delete(ACCESS);
    }

    private static String grantBody(String path, String method, String role) {
        return "{\"path\":\"" + path + "\",\"method\":\"" + method + "\",\"role\":\"" + role + "\"}";
    }
}
